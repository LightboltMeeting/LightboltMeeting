package de.lightbolt.meeting.data.config.guild;

import de.lightbolt.meeting.data.config.GuildConfigItem;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.dv8tion.jda.api.entities.Category;

import java.util.List;

/**
 * Configuration for the guild's moderation system.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MeetingConfig extends GuildConfigItem {
	private String meetingCategoryTemplate = "%s";
	private String meetingLogTemplate = "meeting-%s-log";
	private String meetingVoiceTemplate = "\uD83D\uDCC5 %s";

	private int maxMeetingsPerUser = 2;
	private List<Integer> meetingReminders = List.of(10, 60, 360, 1440);
}
