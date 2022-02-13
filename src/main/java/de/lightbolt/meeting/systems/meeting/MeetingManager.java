package de.lightbolt.meeting.systems.meeting;

import de.lightbolt.meeting.systems.meeting.model.Meeting;
import de.lightbolt.meeting.utils.localization.Language;
import de.lightbolt.meeting.utils.localization.LocaleConfig;
import de.lightbolt.meeting.utils.localization.LocalizationUtils;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.JDA;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.entities.VoiceChannel;

import java.util.Collections;

/**
 * Schedules and handles {@link de.lightbolt.meeting.systems.meeting.model.Meeting}s.
 */
@RequiredArgsConstructor
public class MeetingManager {
	private final JDA jda;
	private final Meeting meeting;

	public static final String MEETING_VOICE_NAME = "%s — %s";

	public static MessageEmbed buildMeetingEmbed(Meeting meeting, User createdBy, LocaleConfig locale) {
		return new EmbedBuilder()
				.setAuthor(createdBy.getAsTag(), null, createdBy.getEffectiveAvatarUrl())
				.setTitle(meeting.getTitle())
				.setDescription(meeting.getDescription())
				.setFooter(locale.getMeeting().getMEETING_EMBED_FOOTER())
				.setTimestamp(meeting.getDueAt().toLocalDateTime())
				.build();
	}

	public TextChannel getLogChannel() {
		return jda.getGuildById(meeting.getGuildId()).getTextChannelById(meeting.getLogChannelId());
	}

	public VoiceChannel getVoiceChannel() {
		return jda.getGuildById(meeting.getGuildId()).getVoiceChannelById(meeting.getVoiceChannelId());
	}

	public void updateMeeting(User updatedBy, LocaleConfig locale) {
		this.getVoiceChannel().getManager().setName(String.format(MEETING_VOICE_NAME, meeting.getId(), meeting.getTitle())).queue();
		jda.retrieveUserById(meeting.getCreatedBy()).queue(
				user -> {
					this.getLogChannel()
							.sendMessageFormat(locale.getMeeting().getLog().getLOG_MEETING_UPDATED(), updatedBy.getAsMention())
							.setEmbeds(buildMeetingEmbed(meeting, user, LocalizationUtils.getLocale(Language.valueOf(meeting.getLanguage()))))
							.queue();
				}
		);
	}

	public void addParticipant(User user) {
		var meetingLocale = LocalizationUtils.getLocale(Language.valueOf(meeting.getLanguage())).getMeeting().getLog();
		var text = this.getLogChannel();
		text.sendMessageFormat(meetingLocale.getLOG_PARTICIPANT_ADDED(), user.getAsMention()).queue();
		text.getManager().putMemberPermissionOverride(user.getIdLong(),
				Permission.getRaw(Permission.VIEW_CHANNEL, Permission.MESSAGE_SEND), 0
		).queue();
		var voice = this.getVoiceChannel();
		voice.getManager().putMemberPermissionOverride(user.getIdLong(),
				Collections.singleton(Permission.VIEW_CHANNEL),
				Collections.singleton(Permission.VOICE_CONNECT)
		).queue();
	}

	public void removeParticipant(User user) {
		var meetingLocale = LocalizationUtils.getLocale(Language.valueOf(meeting.getLanguage())).getMeeting().getLog();
		var text = this.getLogChannel();
		text.sendMessageFormat(meetingLocale.getLOG_PARTICIPANT_REMOVED(), user.getAsMention()).queue();
		text.getManager().putMemberPermissionOverride(user.getIdLong(), 0, Permission.ALL_PERMISSIONS).queue();
		var voice = this.getVoiceChannel();
		voice.getManager().putMemberPermissionOverride(user.getIdLong(), 0, Permission.ALL_PERMISSIONS).queue();
	}

	public void addAdmin(User user) {
		var meetingLocale = LocalizationUtils.getLocale(Language.valueOf(meeting.getLanguage())).getMeeting().getLog();
		var text = this.getLogChannel();
		text.sendMessageFormat(meetingLocale.getLOG_ADMIN_ADDED(), user.getAsMention()).queue();
	}

	public void removeAdmin(User user) {
		var meetingLocale = LocalizationUtils.getLocale(Language.valueOf(meeting.getLanguage())).getMeeting().getLog();
		var text = this.getLogChannel();
		text.sendMessageFormat(meetingLocale.getLOG_ADMIN_REMOVED(), user.getAsMention()).queue();
	}
}
