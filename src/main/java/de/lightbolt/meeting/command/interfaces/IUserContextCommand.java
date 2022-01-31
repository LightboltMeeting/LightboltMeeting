package de.lightbolt.meeting.command.interfaces;

import de.lightbolt.meeting.command.ResponseException;
import net.dv8tion.jda.api.events.interaction.command.UserContextInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

/**
 * Interface that handles Discord's User Context Commands.
 */
public interface IUserContextCommand {
	ReplyCallbackAction handleUserContextCommandInteraction(UserContextInteractionEvent event) throws ResponseException;
}
