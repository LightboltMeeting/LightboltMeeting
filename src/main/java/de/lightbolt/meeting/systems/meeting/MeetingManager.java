package de.lightbolt.meeting.systems.meeting;

import de.lightbolt.meeting.systems.meeting.model.Meeting;
import de.lightbolt.meeting.utils.localization.LocaleConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;

/**
 * Schedules and handles {@link de.lightbolt.meeting.systems.meeting.model.Meeting}s.
 */
public class MeetingManager {

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

}
