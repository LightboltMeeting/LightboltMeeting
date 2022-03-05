package de.lightbolt.meeting.data.config.commands.subcommands;

import de.lightbolt.meeting.Bot;
import de.lightbolt.meeting.command.Responses;
import de.lightbolt.meeting.command.interfaces.ISlashCommand;
import de.lightbolt.meeting.data.config.UnknownPropertyException;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

/**
 * Subcommand that allows staff-members to get a single property variable from the guild config.
 */
public class GetSubcommand implements ISlashCommand {
	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		var propertyOption = event.getOption("property");
		if (propertyOption == null) {
			return Responses.warning(event, "Missing required property argument.");
		}
		String property = propertyOption.getAsString().trim();
		try {
			Object value = Bot.config.get(event.getGuild()).resolve(property);
			return Responses.info(event, "Configuration Property", String.format("The value of the property `%s` is:\n```\n%s\n```", property, value));
		} catch (UnknownPropertyException e) {
			return Responses.warning(event, "Unknown Property", "The property `" + property + "` could not be found.");
		}
	}
}
