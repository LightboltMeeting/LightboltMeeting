package de.lightbolt.meeting.systems.meeting;

import de.lightbolt.meeting.systems.meeting.model.Meeting;
import de.lightbolt.meeting.utils.localization.Language;
import de.lightbolt.meeting.utils.localization.LocaleConfig;
import de.lightbolt.meeting.utils.localization.LocalizationUtils;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.Permission;
import net.dv8tion.jda.api.entities.*;

import java.util.Collections;

/**
 * Schedules and handles {@link de.lightbolt.meeting.systems.meeting.model.Meeting}s.
 */
@RequiredArgsConstructor
public class MeetingManager {
	private final Guild guild;
	private final Meeting meeting;

	public static MessageEmbed buildMeetingCreationEmbed(int step, LocaleConfig.MeetingConfig.MeetingCreationConfig config, String description) {
		return new EmbedBuilder()
				.setTitle(String.format(config.getCREATION_DM_DEFAULT_EMBED_TITLE(), step, 5))
				.setDescription(description)
				.setFooter(String.format(config.getCREATION_DM_DEFAULT_EMBED_FOOTER(),
						MeetingCreationManager.TIMEOUT_INT,
						MeetingCreationManager.TIMEOUT_UNIT.name()))
				.build();
	}

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
		return guild.getTextChannelById(meeting.getLogChannelId());
	}

	public VoiceChannel getVoiceChannel() {
		return guild.getVoiceChannelById(meeting.getVoiceChannelId());
	}

	public void addParticipant(User user) {
		var meetingLocale = LocalizationUtils.getLocale(Language.valueOf(meeting.getLanguage())).getMeeting().getLog();
		var text = this.getLogChannel();
		text.sendMessage(String.format(meetingLocale.getLOG_PARTICIPANT_ADDED(), user.getAsMention())).queue();
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
		text.sendMessage(String.format(meetingLocale.getLOG_PARTICIPANT_REMOVED(), user.getAsMention())).queue();
		text.getManager().putMemberPermissionOverride(user.getIdLong(), 0, Permission.ALL_PERMISSIONS).queue();
		var voice = this.getVoiceChannel();
		voice.getManager().putMemberPermissionOverride(user.getIdLong(), 0, Permission.ALL_PERMISSIONS).queue();
	}

}
