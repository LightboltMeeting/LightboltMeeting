package de.lightbolt.meeting.systems.meeting;

import de.lightbolt.meeting.Bot;
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

import java.util.Arrays;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;

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
	public static MessageEmbed buildMeetingEmbed(Meeting meeting, User createdBy, LocaleConfig locale) {
		return new EmbedBuilder()
				.setAuthor(createdBy.getAsTag(), null, createdBy.getEffectiveAvatarUrl())
				.setTitle(meeting.getTitle())
				.setDescription(meeting.getDescription())
				.setFooter(locale.getMeeting().getMEETING_EMBED_FOOTER())
				.setTimestamp(meeting.getDueAt().toLocalDateTime())
				.build();
	}

	public static void checkActiveMeetings(JDA jda) {
		for (var guild : jda.getGuilds()) {
			DbHelper.doDaoAction(MeetingRepository::new, dao -> {
				List<Meeting> activeMeetings = dao.getActive();
				var activeLogs = activeMeetings.stream().map(Meeting::getLogChannelId).toList();
				var activeVoices = activeMeetings.stream().map(Meeting::getVoiceChannelId).toList();
				Category category = Bot.config.get(guild).getMeeting().getMeetingCategory();
				category.getChannels().forEach(c -> {
					if (!activeLogs.contains(c.getIdLong()) && !activeVoices.contains(c.getIdLong())) {
						log.info("Removing Unknown Meeting Channel: " + c.getName());
						c.delete().queue();
					}
				});
				var categoryChannelIds = category.getChannels().stream().map(GuildChannel::getIdLong).toList();
				activeMeetings.forEach(meeting -> jda.retrieveUserById(meeting.getCreatedBy()).queue(owner -> {
							if (!categoryChannelIds.contains(meeting.getLogChannelId())) {
								new MeetingManager(jda, meeting).createLogChannel(category, owner,
										LocalizationUtils.getLocale(Language.valueOf(meeting.getLanguage())));
								log.info("Created missing Log Channel for Meeting: " + meeting);
							}
							if (!categoryChannelIds.contains(meeting.getVoiceChannelId())) {
								new MeetingManager(jda, meeting).createVoiceChannel(category);
								log.info("Created missing Voice Channel for Meeting: " + meeting);
							}
						}
				));
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
	public static boolean canEditMeeting(Meeting meeting, long userId) {
		return Arrays.stream(meeting.getAdmins()).anyMatch(l -> l == userId) || meeting.getCreatedBy() == userId;
	}

	/**
	 * Creates a new Log Channel for this Meeting.
	 *
	 * @param category  The Meeting Category that's specified in the config file.
	 * @param createdBy The Meeting's owner.
	 * @param locale    The Meeting's locale.
	 */
	public void createLogChannel(Category category, User createdBy, LocaleConfig locale) {
		category.createTextChannel(String.format(MeetingManager.MEETING_LOG_NAME, meeting.getId())).queue(
				channel -> {
					this.setLogChannelPermissions(channel, meeting.getParticipants());
					channel.sendMessageEmbeds(buildMeetingEmbed(meeting, createdBy, locale)).queue();
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
	public void createVoiceChannel(Category category) {
		category.createVoiceChannel(String.format(MeetingManager.MEETING_VOICE_NAME, meeting.getId(), meeting.getTitle())).queue(
				channel -> {
					this.setVoiceChannelPermissions(channel, meeting.getParticipants());
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
	public void addParticipant(User user) {
		var meetingLocale = LocalizationUtils.getLocale(meeting.getLanguage()).getMeeting().getLog();
		var text = this.getLogChannel();
		text.sendMessageFormat(meetingLocale.getLOG_PARTICIPANT_ADDED(), user.getAsMention()).queue();
		this.setLogChannelPermissions(text, meeting.getParticipants());
		var voice = this.getVoiceChannel();
		voice.getManager().putMemberPermissionOverride(user.getIdLong(),
				Collections.singleton(Permission.VIEW_CHANNEL),
				Collections.singleton(Permission.VOICE_CONNECT)
		).queue();
	}

	/**
	 * Removes a single participant from the meeting.
	 *
	 * @param user The user that should be removed.
	 */
	public void removeParticipant(User user) {
		var meetingLocale = LocalizationUtils.getLocale(meeting.getLanguage()).getMeeting().getLog();
		var text = this.getLogChannel();
		text.sendMessageFormat(meetingLocale.getLOG_PARTICIPANT_REMOVED(), user.getAsMention()).queue();
		text.getManager().putMemberPermissionOverride(user.getIdLong(), 0, Permission.ALL_PERMISSIONS).queue();
		var voice = this.getVoiceChannel();
		voice.getManager().putMemberPermissionOverride(user.getIdLong(), 0, Permission.ALL_PERMISSIONS).queue();
	}

	/**
	 * Adds a single admin to the meeting.
	 *
	 * @param user The user that should be added as an admin.
	 */
	public void addAdmin(User user) {
		var meetingLocale = LocalizationUtils.getLocale(meeting.getLanguage()).getMeeting().getLog();
		var text = this.getLogChannel();
		text.sendMessageFormat(meetingLocale.getLOG_ADMIN_ADDED(), user.getAsMention()).queue();
	}

	/**
	 * Removes a single admin from the meeting.
	 *
	 * @param user The user that should be removed.
	 */
	public void removeAdmin(User user) {
		var meetingLocale = LocalizationUtils.getLocale(meeting.getLanguage()).getMeeting().getLog();
		var text = this.getLogChannel();
		text.sendMessageFormat(meetingLocale.getLOG_ADMIN_REMOVED(), user.getAsMention()).queue();
	}

	private void setLogChannelPermissions(TextChannel channel, long[] userId) {
		var manager = channel.getManager();
		manager.putRolePermissionOverride(channel.getGuild().getIdLong(), 0, Permission.ALL_PERMISSIONS);
		for (long id : userId) {
			manager.putMemberPermissionOverride(id,
					Permission.getPermissions(Permission.ALL_TEXT_PERMISSIONS + Permission.getRaw(Permission.VIEW_CHANNEL)), Collections.emptySet());
		}
		manager.queue();
	}

	private void setVoiceChannelPermissions(VoiceChannel channel, long[] userId) {
		var manager = channel.getManager();
		manager.putRolePermissionOverride(channel.getGuild().getIdLong(), 0, Permission.ALL_PERMISSIONS);
		for (long id : userId) {
			manager.putMemberPermissionOverride(id, Collections.singleton(Permission.VIEW_CHANNEL), Collections.singleton(Permission.VOICE_CONNECT));
		}
		manager.queue();
	}
}