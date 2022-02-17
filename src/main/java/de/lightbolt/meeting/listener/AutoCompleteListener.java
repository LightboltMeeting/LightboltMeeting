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
import java.util.List;
import java.util.stream.Collectors;

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
			case "edit", "add-participant", "remove-participant" -> this.replyMeetings(event, this.getUserAndAdminMeetings(event));
			case "start", "discard" -> this.replyMeetings(event, this.getScheduledUserMeetings(event));
			case "end" -> this.replyMeetings(event, this.getOngoingUserMeetings(event));
			case "remove-admin", "add-admin" -> this.replyMeetings(event, this.getUserMeetings(event));
			default -> throw new IllegalStateException("Unknown Subcommand: " + event.getSubcommandName());
		};
	}

	private AutoCompleteCallbackAction replyMeetings(CommandAutoCompleteInteractionEvent event, List<Meeting> meetings) {
		ArrayList<Command.Choice> choices = new ArrayList<>();
		for (Meeting meeting : meetings) {
			String format;
			if (meeting.getCreatedBy() == event.getUser().getIdLong()) {
				format = "%s* — Meeting: \"%s\"";
			} else {
				format = "%s — Meeting: \"%s\"";
			}
			choices.add(new Command.Choice(String.format(format, meeting.getId(), meeting.getTitle()), meeting.getId()));
		}
		return event.replyChoices(choices);
	}

	private List<Meeting> getUserMeetings(CommandAutoCompleteInteractionEvent event) {
		try (var con = Bot.dataSource.getConnection()) {
			var repo = new MeetingRepository(con);
			return repo.getByUserId(event.getUser().getIdLong());
		} catch (SQLException e) {
			log.error("Could not retrieve Meetings from User: " + event.getUser().getAsTag(), e);
			return List.of();
		}
	}

	private List<Meeting> getUserAndAdminMeetings(CommandAutoCompleteInteractionEvent event) {
		try (var con = Bot.dataSource.getConnection()) {
			var repo = new MeetingRepository(con);
			var userId = event.getUser().getIdLong();
			return repo.getActive().stream().filter(
					m -> Arrays.stream(m.getAdmins()).anyMatch(l -> l == userId) || m.getCreatedBy() == userId).toList();
		} catch (SQLException e) {
			log.error("Could not retrieve Meetings from User: " + event.getUser().getAsTag(), e);
			return List.of();
		}
	}

	private List<Meeting> getOngoingUserMeetings(CommandAutoCompleteInteractionEvent event) {
		return getUserAndAdminMeetings(event).stream().filter(Meeting::isOngoing).collect(Collectors.toList());
	}

	private List<Meeting> getScheduledUserMeetings(CommandAutoCompleteInteractionEvent event) {
		return getUserAndAdminMeetings(event).stream().filter(m -> !m.isOngoing()).collect(Collectors.toList());
	}
}
