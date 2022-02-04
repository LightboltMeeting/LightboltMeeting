package de.lightbolt.meeting.listener;

import de.lightbolt.meeting.Bot;
import de.lightbolt.meeting.systems.meeting.dao.MeetingRepository;
import de.lightbolt.meeting.systems.meeting.model.Meeting;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.events.interaction.command.CommandAutoCompleteInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import net.dv8tion.jda.api.interactions.commands.Command;
import net.dv8tion.jda.api.requests.restaction.interactions.AutoCompleteCallbackAction;

import java.sql.SQLException;
import java.util.ArrayList;

@Slf4j
public class AutoCompleteListener extends ListenerAdapter {
	@Override
	public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
		switch (event.getName()) {
			case "meeting" -> handleMeetingCommand(event).queue();
			default -> throw new IllegalStateException("Unknown Command: " + event.getName());
		}
	}

	private AutoCompleteCallbackAction handleMeetingCommand(CommandAutoCompleteInteractionEvent event) {
		return switch (event.getSubcommandName()) {
			case "discard" -> getUserMeetings(event);
			case "add-participant" -> getUserMeetings(event);
			case "remove-participant" -> getUserMeetings(event);
			case "add-admin" -> getUserMeetings(event);
			case "remove-admin" -> getUserMeetings(event);
			default -> throw new IllegalStateException("Unknown Subcommand: " + event.getSubcommandName());
		};
	}

	private AutoCompleteCallbackAction getUserMeetings(CommandAutoCompleteInteractionEvent event) {
		try (var con = Bot.dataSource.getConnection()) {
			var repo = new MeetingRepository(con);
			var meetings = repo.getByUserId(event.getUser().getIdLong());
			ArrayList<Command.Choice> choices = new ArrayList<>();
			for (Meeting meeting : meetings) {
				choices.add(new Command.Choice(String.format("%s — Meeting: \"%s\"", meeting.getId(), meeting.getTitle()), meeting.getId()));
			}
			return event.replyChoices(choices);
		} catch (SQLException e) {
			log.error("Could not retrieve Meetings from User: " + event.getUser().getAsTag(), e);
			return null;
		}
	}
}
