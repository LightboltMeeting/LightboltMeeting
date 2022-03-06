package de.lightbolt.meeting.command.data.slash_commands;

import de.lightbolt.meeting.data.config.BotConfig;
import de.lightbolt.meeting.data.config.UnknownPropertyException;
import lombok.Data;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Member;
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
			Member member = guild.getMemberById(id);
			if (member == null) throw new IllegalArgumentException("Member could not be found for id " + id);
			return new CommandPrivilege(CommandPrivilege.Type.USER, this.enabled, member.getIdLong());
		} else if (this.type.equalsIgnoreCase(CommandPrivilege.Type.ROLE.name())) {
			Long roleId;
			try {
				roleId = (Long) botConfig.get(guild).resolve(this.id);
			} catch (UnknownPropertyException e) {
				log.error("Unknown property while resolving role id. Owner was set as the enabled user.", e);
				return CommandPrivilege.enableUser(guild.getOwnerIdLong());
			}
			if (roleId == null) {
				log.warn("Missing role id. Owner was set as the enabled user.");
				return CommandPrivilege.enableUser(guild.getOwnerIdLong());
			}
			Role role = guild.getRoleById(roleId);
			if (role == null) {
				log.warn("Role could not be found. Owner was set as the enabled user.");
				return CommandPrivilege.enableUser(guild.getOwnerIdLong());
			}
			return new CommandPrivilege(CommandPrivilege.Type.ROLE, this.enabled, role.getIdLong());
		}
		throw new IllegalArgumentException("Invalid type.");
	}
}
