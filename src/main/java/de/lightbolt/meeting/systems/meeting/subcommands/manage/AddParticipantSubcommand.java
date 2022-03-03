package de.lightbolt.meeting.systems.meeting.subcommands.manage;

import de.lightbolt.meeting.command.Responses;
import de.lightbolt.meeting.data.config.guild.MeetingConfig;
import de.lightbolt.meeting.systems.meeting.MeetingManager;
import de.lightbolt.meeting.systems.meeting.MeetingSubcommand;
import de.lightbolt.meeting.systems.meeting.dao.MeetingRepository;
import de.lightbolt.meeting.systems.meeting.model.Meeting;
import de.lightbolt.meeting.utils.localization.LocaleConfig;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>/meeting manage add-participant</p>
 * Command that allows Meeting Administrators to add participants to their meeting.
 */
public class AddParticipantSubcommand extends MeetingSubcommand {
	private final int MEMBER_OPTIONS = 9;

	@Override
	protected ReplyCallbackAction handleMeetingCommand(SlashCommandInteractionEvent event, LocaleConfig locale, MeetingConfig config, MeetingRepository repo) throws SQLException {
		var idOption = event.getOption("meeting-id");
		if (idOption == null) {
			return Responses.error(event, locale.getCommand().getMISSING_ARGUMENTS());
		}
		List<User> users = new ArrayList<>();
		for (int i = 1; i < MEMBER_OPTIONS; i++) {
			var userOption = event.getOption("user-" + i);
			if (userOption == null) continue;
			users.add(userOption.getAsUser());
		}
		var id = (int) idOption.getAsLong();
		var com = locale.getMeeting().getCommand();
		Optional<Meeting> meetingOptional = repo.getById(id);
		if (meetingOptional.isEmpty()) {
			return Responses.error(event, String.format(com.getMEETING_NOT_FOUND(), id));
		}
		var meeting = meetingOptional.get();
		if (!MeetingManager.canEditMeeting(meeting, event.getUser().getIdLong())) {
			return Responses.error(event, locale.getMeeting().getMEETING_NO_PERMISSION());
		}
		long[] participants = meeting.getParticipants();
		for (User user : users) {
			if (Arrays.stream(participants).anyMatch(x -> x == user.getIdLong())) {
				return Responses.error(event, String.format(com.getMEETING_PARTICIPANT_ALREADY_ADDED(), user.getAsMention()));
			}
		}
		MeetingManager manager = new MeetingManager(event.getJDA(), meeting);
		manager.addParticipants(users.stream().mapToLong(User::getIdLong).toArray());
		return Responses.success(event, com.getPARTICIPANTS_ADD_SUCCESS_TITLE(), String.format(com.getPARTICIPANTS_ADD_SUCCESS_DESCRIPTION(), users.stream().map(User::getAsMention).collect(Collectors.joining(", ")), meeting.getTitle()));
	}
}
