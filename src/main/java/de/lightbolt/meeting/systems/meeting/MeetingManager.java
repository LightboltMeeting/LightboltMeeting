package de.lightbolt.meeting.systems.meeting;

import de.lightbolt.meeting.Bot;
import de.lightbolt.meeting.annotations.MissingLocale;
import de.lightbolt.meeting.data.config.guild.MeetingConfig;
import de.lightbolt.meeting.data.h2db.DbHelper;
import de.lightbolt.meeting.systems.meeting.dao.MeetingRepository;
import de.lightbolt.meeting.systems.meeting.model.Meeting;
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

import java.time.ZoneOffset;
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
				List<Meeting> activeMeetings = dao.getActive().stream().filter(p -> p.getGuildId() == guild.getIdLong()).toList();
				for (Meeting m : activeMeetings) {
					MeetingManager manager = new MeetingManager(jda, m);
					Category category = guild.getCategoryById(m.getCategoryId());
					if (category == null) {
						manager.createMeetingChannels(guild, jda.getUserById(m.getCreatedBy()), m.getLocaleConfig(), config);
					} else {
						List<Long> channelIds = category.getChannels().stream().map(Channel::getIdLong).toList();
						if (!channelIds.contains(m.getLogChannelId())) {
							manager.createLogChannel(category, jda.getUserById(m.getCreatedBy()), m.getLocaleConfig(), config);
						}
						if (!channelIds.contains(m.getVoiceChannelId())) {
							manager.createVoiceChannel(category, config);
						}
					}
				}
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

	// FIXME: 27.02.2022 Voice Channel Name defaults back to the Planned State, even though the Meeting is started.
	@MissingLocale
	public void startMeeting() {
		var text = this.getLogChannel();
		var logLocale = meeting.getLocaleConfig().getMeeting().getLog();
		var config = Bot.config.get(getJDA().getGuildById(meeting.getGuildId())).getMeeting();
		text.sendMessageFormat(logLocale.getLOG_MEETING_STARTED(), Arrays.stream(meeting.getParticipants()).mapToObj(m -> String.format("<@%s>", m)).collect(Collectors.joining(", "))).queue();
		this.getVoiceChannel()
				.getManager()
				.setName(String.format(config.getMeetingVoiceTemplate(), config.getMeetingOngoingEmoji(), "Ongoing Meeting"))
				.queue(s -> this.updateVoiceChannelPermissions(this.getVoiceChannel(), meeting.getParticipants(), true));
		DbHelper.doDaoAction(MeetingRepository::new, dao -> dao.setStatus(meeting.getId(), MeetingStatus.ONGOING));
	}

	public void endMeeting() {
		this.getLogChannel().delete().queue();
		this.getVoiceChannel().delete().queue();
		this.getCategory().delete().queue();
		Bot.meetingStateManager.cancelMeetingSchedule(meeting);
		DbHelper.doDaoAction(MeetingRepository::new, dao -> dao.setStatus(meeting.getId(), MeetingStatus.INACTIVE));
	}

	public void discardMeeting() {
		this.getLogChannel().delete().queue();
		this.getVoiceChannel().delete().queue();
		this.getCategory().delete().queue();
		Bot.meetingStateManager.cancelMeetingSchedule(meeting);
		DbHelper.doDaoAction(MeetingRepository::new, dao -> dao.setStatus(meeting.getId(), MeetingStatus.INACTIVE));
	}

	/**
	 * Creates all Meeting Channels.
	 */
	public void createMeetingChannels(Guild guild, User createdBy, LocaleConfig locale, MeetingConfig config) {
		guild.createCategory(String.format(config.getMeetingCategoryTemplate(), meeting.getTitle())).queue(
				category -> {
					this.createLogChannel(category, createdBy, locale, config);
					this.createVoiceChannel(category, config);
					DbHelper.doDaoAction(MeetingRepository::new, dao -> dao.updateCategory(meeting, category.getIdLong()));
				}
		);
	}

	/**
	 * Gets the Meeting's category.
	 *
	 * @return The meetings log channel as a {@link TextChannel}.
	 */
	public Category getCategory() {
		return getJDA().getGuildById(meeting.getGuildId()).getCategoryById(meeting.getCategoryId());
	}

	/**
	 * Creates a new Log Channel for this Meeting.
	 *
	 * @param category  The Meeting Category that's specified in the config file.
	 * @param createdBy The Meeting's owner.
	 * @param locale    The Meeting's locale.
	 */
	public void createLogChannel(@NotNull Category category, User createdBy, LocaleConfig locale, MeetingConfig config) {
		category.createTextChannel(String.format(config.getMeetingLogTemplate(), meeting.getId())).queue(
				channel -> {
					this.updateLogChannelPermissions(channel, meeting.getParticipants());
					channel.sendMessageEmbeds(buildMeetingEmbed(meeting, createdBy, locale))
							.setActionRow(Button.secondary("meeting-faq", locale.getMeeting().getFaq().getFAQ_BUTTON_LABEL()))
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
		return getJDA().getGuildById(meeting.getGuildId()).getTextChannelById(meeting.getLogChannelId());
	}

	/**
	 * Creates a new Voice Channel for this Meeting.
	 *
	 * @param category  The Meeting Category that's specified in the config file.
	 */
	public void createVoiceChannel(@NotNull Category category, MeetingConfig config) {
		category.createVoiceChannel(String.format(config.getMeetingVoiceTemplate(), config.getMeetingPlannedEmoji(), meeting.getDueAtFormatted())).queue(
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
		return getJDA().getGuildById(meeting.getGuildId()).getVoiceChannelById(meeting.getVoiceChannelId());
	}

	/**
	 * Updates a single Meeting.
	 *
	 * @param updatedBy The user that initiated this process.
	 * @param locale    The user's locale.
	 */
	public void updateMeeting(User updatedBy, LocaleConfig locale) {
		getJDA().retrieveUserById(meeting.getCreatedBy()).queue(
				user -> this.getLogChannel()
						.sendMessageFormat(locale.getMeeting().getLog().getLOG_MEETING_UPDATED(), updatedBy.getAsMention())
						.setEmbeds(buildMeetingEmbed(meeting, user, LocalizationUtils.getLocale(meeting.getLanguage())))
						.queue()
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
		this.updateVoiceChannelPermissions(channel, participants, meeting.getStatus() == MeetingStatus.ONGOING);
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

	public MessageEmbed buildMeetingFAQEmbed() {
		var faqLocale = meeting.getLocaleConfig().getMeeting().getFaq();
		return new EmbedBuilder()
				.setTitle(faqLocale.getFAQ_EMBED_TITLE())
				.addField(faqLocale.getFAQ_MEETING_START_FIELD_HEADER(), String.format(faqLocale.getFAQ_MEETING_START_FIELD_DESCRIPTION(), meeting.getDueAt().toLocalDateTime().toEpochSecond(ZoneOffset.UTC)), false)
				.addField(faqLocale.getFAQ_MEETING_EDIT_FIELD_HEADER(), faqLocale.getFAQ_MEETING_EDIT_FIELD_DESCRIPTION(), false)
				.addField(faqLocale.getFAQ_MEETING_ADMIN_FIELD_HEADER(), faqLocale.getFAQ_MEETING_ADMIN_FIELD_DESCRIPTION(), false)
				.build();
	}

	public JDA getJDA() {
		return this.jda;
	}
}