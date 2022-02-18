package de.lightbolt.meeting.systems.meeting;

import de.lightbolt.meeting.Bot;
import de.lightbolt.meeting.annotations.MissingLocale;
import de.lightbolt.meeting.data.h2db.DbHelper;
import de.lightbolt.meeting.systems.meeting.dao.MeetingRepository;
import de.lightbolt.meeting.systems.meeting.model.Meeting;
import de.lightbolt.meeting.utils.localization.Language;
import de.lightbolt.meeting.utils.localization.LocaleConfig;
import de.lightbolt.meeting.utils.localization.LocalizationUtils;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;
import net.dv8tion.jda.api.interactions.components.buttons.Button;
import org.apache.commons.lang3.ArrayUtils;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Schedules and handles {@link de.lightbolt.meeting.systems.meeting.model.Meeting}s.
 */
@Slf4j
@RequiredArgsConstructor
public class MeetingManager {
	public static final String MEETING_LOG_NAME = "meeting-%s-log";
	public static final String MEETING_VOICE_NAME = "%s — %s";
	private final JDA jda;
	private final Meeting meeting;

	/**
	 * Builds an embed that represents a single meeting with all its necessary information.
	 *
	 * @param meeting   The meeting to display.
	 * @param createdBy The user that created the meeting.
	 * @param locale    The user's locale.
	 * @return A {@link MessageEmbed}.
	 */
	public static @NotNull MessageEmbed buildMeetingEmbed(@NotNull Meeting meeting, @NotNull User createdBy, @NotNull LocaleConfig locale) {
		return new EmbedBuilder()
				.setAuthor(createdBy.getAsTag(), null, createdBy.getEffectiveAvatarUrl())
				.setTitle(meeting.getTitle())
				.setDescription(meeting.getDescription())
				.setFooter(locale.getMeeting().getMEETING_EMBED_FOOTER())
				.setTimestamp(meeting.getDueAt().toLocalDateTime())
				.build();
	}

	/**
	 * Checks for missing or unknown Log & Voice channels and creates/deletes them.
	 * @param jda The JDA instance
	 */
	public static void checkActiveMeetings(@NotNull JDA jda) {
		for (var guild : jda.getGuilds()) {
			var config = Bot.config.get(guild).getMeeting();
			DbHelper.doDaoAction(MeetingRepository::new, dao -> {
				List<Meeting> activeMeetings = dao.getActive();
				List<Long> activeLogs = activeMeetings.stream().map(Meeting::getLogChannelId).toList();
				List<Long> activeVoices = activeMeetings.stream().map(Meeting::getVoiceChannelId).toList();
				Category category = config.getMeetingCategory();
				Category ongoingCategory = config.getOngoingMeetingCategory();
				List<Long> channels = category.getChannels().stream().map(GuildChannel::getIdLong).toList();
				ongoingCategory.getChannels().forEach(c -> channels.add(c.getIdLong()));
				channels.forEach(c -> {
					if (!activeLogs.contains(c) && !activeVoices.contains(c)) {
						var channel = guild.getGuildChannelById(c);
						log.info("Removing Unknown Meeting Channel: " + channel.getName());
						channel.delete().queue();
					}
				});
				activeMeetings.forEach(meeting -> {
					Category destination;
					if (meeting.isOngoing()) destination = config.getOngoingMeetingCategory();
					else destination = config.getMeetingCategory();
					jda.retrieveUserById(meeting.getCreatedBy()).queue(owner -> {
								if (!channels.contains(meeting.getLogChannelId())) {
									new MeetingManager(jda, meeting).createLogChannel(destination, owner,
											LocalizationUtils.getLocale(Language.valueOf(meeting.getLanguage())));
									log.info("Created missing Log Channel for Meeting: " + meeting);
								}
								if (!channels.contains(meeting.getVoiceChannelId())) {
									new MeetingManager(jda, meeting).createVoiceChannel(destination);
									log.info("Created missing Voice Channel for Meeting: " + meeting);
								}
							}
					);
				});
			});
		}
	}

	/**
	 * Checks if the given user can edit (or add/remove participants) the given meeting.
	 *
	 * @param meeting The meeting.
	 * @param userId  The user whose checked.
	 * @return Whether the user is permitted to edit the meeting.
	 */
	public static boolean canEditMeeting(@NotNull Meeting meeting, long userId) {
		return Arrays.stream(meeting.getAdmins()).anyMatch(l -> l == userId) || meeting.getCreatedBy() == userId;
	}

	public void startMeeting(@NotNull User startedBy) {
		var logLocale = meeting.getLocaleConfig().getMeeting().getLog();
		this.getLogChannel().sendMessageFormat(logLocale.getLOG_MEETING_MANUALLY_STARTED(), startedBy.getAsMention()).queue();
		this.startMeeting();
	}

	public void startMeeting() {
		var text = this.getLogChannel();
		var voice = this.getVoiceChannel();
		Category ongoingMeetingsCategory = Bot.config.get(jda.getGuildById(meeting.getGuildId())).getMeeting().getOngoingMeetingCategory();
		text.getManager().setParent(ongoingMeetingsCategory).queue();
		voice.getManager().setParent(ongoingMeetingsCategory).queue();
		var logLocale = meeting.getLocaleConfig().getMeeting().getLog();
		text.sendMessageFormat(logLocale.getLOG_MEETING_STARTED(), Arrays.stream(meeting.getParticipants()).mapToObj(m -> String.format("<@%s>", m)).collect(Collectors.joining(", ")))
				.queue();
		DbHelper.doDaoAction(MeetingRepository::new, dao -> dao.markOngoing(meeting.getId()));
		this.updateVoiceChannelPermissions(this.getVoiceChannel(), meeting.getParticipants(), true);
	}

	public void endMeeting() {
		this.getLogChannel().delete().queue();
		this.getVoiceChannel().delete().queue();
		Bot.meetingStateManager.cancelMeetingSchedule(meeting);
		DbHelper.doDaoAction(MeetingRepository::new, dao -> dao.markInactive(meeting.getId()));
	}

	public void discardMeeting() {
		this.getLogChannel().delete().queue();
		this.getVoiceChannel().delete().queue();
		Bot.meetingStateManager.cancelMeetingSchedule(meeting);
		DbHelper.doDaoAction(MeetingRepository::new, dao -> dao.markInactive(meeting.getId()));
	}

	/**
	 * Creates a new Log Channel for this Meeting.
	 *
	 * @param category  The Meeting Category that's specified in the config file.
	 * @param createdBy The Meeting's owner.
	 * @param locale    The Meeting's locale.
	 */
	public void createLogChannel(@NotNull Category category, User createdBy, LocaleConfig locale) {
		category.createTextChannel(String.format(MeetingManager.MEETING_LOG_NAME, meeting.getId())).queue(
				channel -> {
					this.updateLogChannelPermissions(channel, meeting.getParticipants());
					channel.sendMessageEmbeds(buildMeetingEmbed(meeting, createdBy, locale))
							.setActionRow(Button.secondary("meeting-faq", "FAQ"))
							.queue();
					DbHelper.doDaoAction(MeetingRepository::new, dao -> dao.updateLogChannel(meeting, channel.getIdLong()));
				}, e -> log.error("Could not create Log Channel for Meeting: " + meeting, e));
	}

	/**
	 * Gets the Meeting's log channel.
	 *
	 * @return The meetings log channel as a {@link TextChannel}.
	 */
	public TextChannel getLogChannel() {
		return jda.getGuildById(meeting.getGuildId()).getTextChannelById(meeting.getLogChannelId());
	}

	/**
	 * Creates a new Voice Channel for this Meeting.
	 *
	 * @param category  The Meeting Category that's specified in the config file.
	 */
	public void createVoiceChannel(@NotNull Category category) {
		category.createVoiceChannel(String.format(MeetingManager.MEETING_VOICE_NAME, meeting.getId(), meeting.getTitle())).queue(
				channel -> {
					this.updateVoiceChannelPermissions(channel, meeting.getParticipants());
					DbHelper.doDaoAction(MeetingRepository::new, dao -> dao.updateVoiceChannel(meeting, channel.getIdLong()));
				}, e -> log.error("Could not create Voice Channel for Meeting: " + meeting, e));
	}

	/**
	 * Gets the Meeting's voice channel.
	 *
	 * @return The meetings voice channel as a {@link VoiceChannel}.
	 */
	public VoiceChannel getVoiceChannel() {
		return jda.getGuildById(meeting.getGuildId()).getVoiceChannelById(meeting.getVoiceChannelId());
	}

	/**
	 * Updates a single Meeting.
	 *
	 * @param updatedBy The user that initiated this process.
	 * @param locale    The user's locale.
	 */
	public void updateMeeting(User updatedBy, LocaleConfig locale) {
		this.getVoiceChannel().getManager().setName(String.format(MEETING_VOICE_NAME, meeting.getId(), meeting.getTitle())).queue();
		jda.retrieveUserById(meeting.getCreatedBy()).queue(
				user -> {
					this.getLogChannel()
							.sendMessageFormat(locale.getMeeting().getLog().getLOG_MEETING_UPDATED(), updatedBy.getAsMention())
							.setEmbeds(buildMeetingEmbed(meeting, user, LocalizationUtils.getLocale(meeting.getLanguage())))
							.queue();
				}
		);
	}

	/**
	 * Adds a single participant to the meeting.
	 *
	 * @param user The user that should be added as a participant.
	 */
	public void addParticipant(@NotNull User user) {
		TextChannel text = this.getLogChannel();
		long[] newParticipants = ArrayUtils.add(meeting.getParticipants(), user.getIdLong());
		DbHelper.doDaoAction(MeetingRepository::new, dao -> {
			var updated = dao.updateParticipants(meeting, newParticipants);
			text.sendMessageFormat(meeting.getLocaleConfig().getMeeting().getLog().getLOG_PARTICIPANT_ADDED(), user.getAsMention()).queue();
			this.updateLogChannelPermissions(text, updated.getParticipants());
			this.updateVoiceChannelPermissions(this.getVoiceChannel(), updated.getParticipants());
		});
	}

	/**
	 * Removes a single participant from the meeting.
	 *
	 * @param user The user that should be removed.
	 */
	public void removeParticipant(@NotNull User user) {
		var meetingLocale = meeting.getLocaleConfig().getMeeting().getLog();
		var text = this.getLogChannel();
		var voice = this.getVoiceChannel();
		text.sendMessageFormat(meetingLocale.getLOG_PARTICIPANT_REMOVED(), user.getAsMention()).queue();
		var newParticipants = ArrayUtils.removeElement(meeting.getParticipants(), user.getIdLong());
		DbHelper.doDaoAction(MeetingRepository::new, dao -> {
			dao.updateParticipants(meeting, newParticipants);
			text.getManager().putMemberPermissionOverride(user.getIdLong(), 0, Permission.ALL_PERMISSIONS).queue();
			voice.getManager().putMemberPermissionOverride(user.getIdLong(), 0, Permission.ALL_PERMISSIONS).queue();
			text.sendMessageFormat(meetingLocale.getLOG_ADMIN_REMOVED(), user.getAsMention()).queue();
		});
	}

	/**
	 * Adds a single admin to the meeting.
	 *
	 * @param user The user that should be added as an admin.
	 */
	public void addAdmin(@NotNull User user) {
		TextChannel text = this.getLogChannel();
		long[] newAdmins = ArrayUtils.add(meeting.getAdmins(), user.getIdLong());
		DbHelper.doDaoAction(MeetingRepository::new, dao -> {
			var updated = dao.updateAdmins(meeting, newAdmins);
			text.sendMessageFormat(meeting.getLocaleConfig().getMeeting().getLog().getLOG_ADMIN_ADDED(), user.getAsMention()).queue();
			this.updateLogChannelPermissions(text, updated.getParticipants());
			this.updateVoiceChannelPermissions(this.getVoiceChannel(), updated.getParticipants());
		});
	}

	/**
	 * Removes a single admin from the meeting.
	 *
	 * @param user The user that should be removed.
	 */
	public void removeAdmin(@NotNull User user) {
		var meetingLocale = meeting.getLocaleConfig().getMeeting().getLog();
		var text = this.getLogChannel();
		var newAdmins = ArrayUtils.removeElement(meeting.getAdmins(), user.getIdLong());
		DbHelper.doDaoAction(MeetingRepository::new, dao -> {
			var updated = dao.updateAdmins(meeting, newAdmins);
			text.sendMessageFormat(meetingLocale.getLOG_ADMIN_REMOVED(), user.getAsMention()).queue();
			this.updateLogChannelPermissions(text, updated.getParticipants());
			this.updateVoiceChannelPermissions(this.getVoiceChannel(), updated.getParticipants());
		});
	}

	private void updateLogChannelPermissions(@NotNull TextChannel channel, long[] userId) {
		var manager = channel.getManager();
		manager.putRolePermissionOverride(channel.getGuild().getIdLong(), 0, Permission.ALL_PERMISSIONS);
		for (long id : userId) {
			manager.putMemberPermissionOverride(id,
					Permission.getPermissions(Permission.ALL_TEXT_PERMISSIONS + Permission.getRaw(Permission.VIEW_CHANNEL)), Collections.emptySet());
		}
		manager.queue();
	}

	private void updateVoiceChannelPermissions(@NotNull VoiceChannel channel, long[] participants) {
		this.updateVoiceChannelPermissions(channel, participants, meeting.isOngoing());
	}

	private void updateVoiceChannelPermissions(@NotNull VoiceChannel channel, long[] participants, boolean ongoing) {
		var manager = channel.getManager();
		manager.putRolePermissionOverride(channel.getGuild().getIdLong(), 0, Permission.ALL_PERMISSIONS);
		for (long id : participants) {
			if (ongoing) manager.putMemberPermissionOverride(id, Permission.getPermissions(Permission.ALL_VOICE_PERMISSIONS + Permission.getRaw(Permission.VIEW_CHANNEL)), Collections.emptySet());
			else manager.putMemberPermissionOverride(id, Collections.singleton(Permission.VIEW_CHANNEL), Collections.singleton(Permission.VOICE_CONNECT));
		}
		manager.queue(s -> {}, e -> log.error("Could not update Channel Permissions for Channel: " + channel.getName(), e));
	}

}