package de.lightbolt.meeting.data.config.guild;

import de.lightbolt.meeting.data.config.GuildConfigItem;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.dv8tion.jda.api.entities.Category;

/**
 * Configuration for the guild's moderation system.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class MeetingConfig extends GuildConfigItem {
	private long meetingCategoryId;
	private int maxMeetingsPerUser = 2;

	public Category getMeetingCategory() {
		return this.getGuild().getCategoryById(this.meetingCategoryId);
	}
}