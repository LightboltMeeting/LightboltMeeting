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
import java.util.Arrays;

@Slf4j
public class AutoCompleteListener extends ListenerAdapter {
	@Override
	public void onCommandAutoCompleteInteraction(CommandAutoCompleteInteractionEvent event) {
		switch (event.getName()) {
			case "meeting", "manage-meeting" -> handleMeetingCommand(event).queue();
			default -> throw new IllegalStateException("Unknown Command: " + event.getName());
		}
	}

	private AutoCompleteCallbackAction handleMeetingCommand(CommandAutoCompleteInteractionEvent event) {
		return switch (event.getSubcommandName()) {
			case "discard", "remove-admin", "add-admin" -> getUserMeetings(event);
			case "start", "end", "add-participant", "remove-participant", "edit" -> getUserAndAdminMeetings(event);
			default -> throw new IllegalStateException("Unknown Subcommand: " + event.getSubcommandName());
		};
	}

	private AutoCompleteCallbackAction getUserMeetings(CommandAutoCompleteInteractionEvent event) {
		try (var con = Bot.dataSource.getConnection()) {
			var repo = new MeetingRepository(con);
			var meetings = repo.getByUserId(event.getUser().getIdLong());
			ArrayList<Command.Choice> choices = new ArrayList<>();
			for (Meeting meeting : meetings) {
				choices.add(new Command.Choice(String.format("%s* — Meeting: \"%s\"", meeting.getId(), meeting.getTitle()), meeting.getId()));
			}
			return event.replyChoices(choices);
		} catch (SQLException e) {
			log.error("Could not retrieve Meetings from User: " + event.getUser().getAsTag(), e);
			return null;
		}
	}

	private AutoCompleteCallbackAction getUserAndAdminMeetings(CommandAutoCompleteInteractionEvent event) {
		try (var con = Bot.dataSource.getConnection()) {
			var repo = new MeetingRepository(con);
			var userId = event.getUser().getIdLong();
			var meetings = repo.getActive().stream().filter(
					m -> Arrays.stream(m.getAdmins()).anyMatch(l -> l == userId) ||
							m.getCreatedBy() == userId
			).toList();
			ArrayList<Command.Choice> choices = new ArrayList<>();
			for (Meeting meeting : meetings) {
				String format;
				if (meeting.getCreatedBy() == userId) {
					format = "%s* — Meeting: \"%s\"";
				} else {
					format = "%s — Meeting: \"%s\"";
				}
				choices.add(new Command.Choice(String.format(format, meeting.getId(), meeting.getTitle()), meeting.getId()));
			}
			return event.replyChoices(choices);
		} catch (SQLException e) {
			log.error("Could not retrieve Meetings from User: " + event.getUser().getAsTag(), e);
			return null;
		}
	}
}
