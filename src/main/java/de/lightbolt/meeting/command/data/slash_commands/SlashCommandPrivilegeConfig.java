package de.lightbolt.meeting.command.data.slash_commands;

import de.lightbolt.meeting.data.config.BotConfig;
import de.lightbolt.meeting.data.config.UnknownPropertyException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Role;
import net.dv8tion.jda.api.interactions.commands.privileges.CommandPrivilege;

/**
 * Simple DTO representing slash command privileges.
 */
@Data
@Slf4j
public class SlashCommandPrivilegeConfig {
	private String type;
	private boolean enabled = true;
	private String id;

	/**
	 * Converts the current {@link SlashCommandPrivilegeConfig} into a {@link CommandPrivilege} object.
	 *
	 * @param guild     The current guild.
	 * @param botConfig The bot's config.
	 * @return The {@link CommandPrivilege} object.
	 */
	public CommandPrivilege toData(Guild guild, BotConfig botConfig) {
		if (this.type.equalsIgnoreCase(CommandPrivilege.Type.USER.name())) {
			Long userId = null;
			try {
				userId = (Long) botConfig.getSystems().resolve(this.id);
			} catch (UnknownPropertyException e) {
				log.error("Unknown property while resolving role id.", e);
			}
			if (userId == null) throw new IllegalArgumentException("Missing user id.");
			return new CommandPrivilege(CommandPrivilege.Type.USER, this.enabled, userId);
		} else if (this.type.equalsIgnoreCase(CommandPrivilege.Type.ROLE.name())) {
			Long roleId = null;
			try {
				roleId = (Long) botConfig.get(guild).resolve(this.id);
			} catch (UnknownPropertyException e) {
				log.error("Unknown property while resolving role id.", e);
			}
			if (roleId == null) throw new IllegalArgumentException("Missing role id.");
			Role role = guild.getRoleById(roleId);
			if (role == null) throw new IllegalArgumentException("Role could not be found for id " + roleId);
			return new CommandPrivilege(CommandPrivilege.Type.ROLE, this.enabled, role.getIdLong());
		}
		throw new IllegalArgumentException("Invalid type.");
	}
}
