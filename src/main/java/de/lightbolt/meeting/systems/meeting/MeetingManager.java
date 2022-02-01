package de.lightbolt.meeting.systems.meeting;

import de.lightbolt.meeting.utils.localization.LocaleConfig;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;

/**
 * Schedules and handles {@link de.lightbolt.meeting.systems.meeting.model.Meeting}s.
 */
public class MeetingManager {

	public static MessageEmbed buildMeetingEmbed(int step, LocaleConfig.MeetingCreationConfig config, String description) {
		return new EmbedBuilder()
				.setTitle(String.format(config.getCREATION_DM_DEFAULT_EMBED_TITLE(), step, 5))
				.setDescription(description)
				.setFooter(String.format(config.getCREATION_DM_DEFAULT_EMBED_FOOTER(),
						MeetingCreationManager.TIMEOUT_INT,
						MeetingCreationManager.TIMEOUT_UNIT.name()))
				.build();
	}
}
