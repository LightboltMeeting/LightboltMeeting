package de.lightbolt.meeting.data.config.guild;

import de.lightbolt.meeting.data.config.GuildConfigItem;
import lombok.Data;
import lombok.EqualsAndHashCode;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;

/**
 * Configuration for the guild's moderation system.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ModerationConfig extends GuildConfigItem {
	private long staffRoleId;

	public Role getStaffRole() {
		return this.getGuild().getRoleById(this.staffRoleId);
	}
}
