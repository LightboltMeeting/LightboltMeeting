package de.lightbolt.meeting.data.config.commands.subcommands;

import de.lightbolt.meeting.Bot;
import de.lightbolt.meeting.command.Responses;
import de.lightbolt.meeting.command.interfaces.ISlashCommand;
import de.lightbolt.meeting.data.config.UnknownPropertyException;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

/**
 * Subcommand that allows staff-members to edit the bot's configuration.
 */
public class SetSubcommand implements ISlashCommand {
	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		var propertyOption = event.getOption("property");
		var valueOption = event.getOption("value");
		if (propertyOption == null || valueOption == null) {
			return Responses.warning(event, "Missing required arguments.");
		}
		String property = propertyOption.getAsString().trim();
		String valueString = valueOption.getAsString().trim();
		try {
			Bot.config.get(event.getGuild()).set(property, valueString);
			return Responses.success(event, "Configuration Updated", String.format("The property `%s` has been set to `%s`.", property, valueString));
		} catch (UnknownPropertyException e) {
			return Responses.warning(event, "Unknown Property", "The property `" + property + "` could not be found.");
		}
	}
}
