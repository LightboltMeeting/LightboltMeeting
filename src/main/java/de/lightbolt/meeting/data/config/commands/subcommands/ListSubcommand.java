package de.lightbolt.meeting.data.config.commands.subcommands;

import de.lightbolt.meeting.command.interfaces.ISlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import java.io.File;

/**
 * Shows a list of all known configuration properties, their type, and their
 * current value.
 */
public class ListSubcommand implements ISlashCommand {
	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		return event.deferReply()
				.addFile(new File(String.format("config/%s.json", event.getGuild().getId())));
	}
}
