package de.lightbolt.meeting.data.config.commands;

import de.lightbolt.meeting.command.DelegatingCommandHandler;
import de.lightbolt.meeting.data.config.commands.subcommands.GetSubcommand;
import de.lightbolt.meeting.data.config.commands.subcommands.ListSubcommand;
import de.lightbolt.meeting.data.config.commands.subcommands.SetSubcommand;

/**
 * The main command for interacting with the bot's configuration at runtime via
 * slash commands.
 */
public class ConfigCommandHandler extends DelegatingCommandHandler {
	/**
	 * Adds all subcommands {@link DelegatingCommandHandler#addSubcommand}.
	 */
	public ConfigCommandHandler() {
		addSubcommand("list", new ListSubcommand());
		addSubcommand("get", new GetSubcommand());
		addSubcommand("set", new SetSubcommand());
	}
}
