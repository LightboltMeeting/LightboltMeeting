package de.lightbolt.meeting.data.config.guild;

import lombok.Data;
import lombok.EqualsAndHashCode;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.entities.TextChannel;
import de.lightbolt.meeting.data.config.GuildConfigItem;

/**
 * Configuration for the guild's moderation system.
 */
@Data
@EqualsAndHashCode(callSuper = true)
public class ModerationConfig extends GuildConfigItem {
	private long logChannelId;
	private long staffRoleId;

	public TextChannel getLogChannel() {
		return this.getGuild().getTextChannelById(this.logChannelId);
	}

	public Role getStaffRole() {
		return this.getGuild().getRoleById(this.staffRoleId);
	}
}
