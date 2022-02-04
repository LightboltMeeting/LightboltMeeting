package de.lightbolt.meeting.systems.meeting.subcommands;

import de.lightbolt.meeting.command.Responses;
import de.lightbolt.meeting.data.config.guild.MeetingConfig;
import de.lightbolt.meeting.systems.meeting.MeetingManager;
import de.lightbolt.meeting.systems.meeting.MeetingSubcommand;
import de.lightbolt.meeting.systems.meeting.dao.MeetingRepository;
import de.lightbolt.meeting.systems.meeting.model.Meeting;
import de.lightbolt.meeting.utils.localization.LocaleConfig;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.apache.commons.lang3.ArrayUtils;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;

public class RemoveParticipantSubcommand extends MeetingSubcommand {
	@Override
	protected ReplyCallbackAction handleMeetingCommand(SlashCommandInteractionEvent event, LocaleConfig locale, MeetingConfig config, MeetingRepository repo) throws SQLException {
		var idOption = event.getOption("meeting-id");
		var userOption = event.getOption("user");
		if (userOption == null || idOption == null) {
			return Responses.error(event, "Missing required arguments");
		}
		var id = (int) idOption.getAsLong();
		var user = userOption.getAsUser();
		var com = locale.getMeeting().getCommand();
		var meetings = repo.getByUserId(event.getUser().getIdLong());
		Optional<Meeting> meetingOptional = meetings.stream().filter(m -> m.getId() == id).findFirst();
		if (meetingOptional.isPresent()) {
			var meeting = meetingOptional.get();
			var participants = meeting.getParticipants();
			var admins = meeting.getAdmins();
			if (Arrays.stream(participants).anyMatch(x -> x == user.getIdLong())) {
				if (Arrays.stream(admins).anyMatch(x -> x == user.getIdLong())) {
					var newAdmins = ArrayUtils.removeElement(admins, user.getIdLong());
					repo.updateAdmins(meeting, newAdmins);
				}
				var newParticipants = ArrayUtils.removeElement(participants, user.getIdLong());
				repo.updateParticipants(meeting, newParticipants);
				new MeetingManager(event.getJDA(), meeting).removeParticipant(user);
				return Responses.success(event, com.getPARTICIPANTS_REMOVE_SUCCESS_TITLE(),
						String.format(com.getPARTICIPANTS_REMOVE_SUCCESS_DESCRIPTION(), user.getAsMention(), meeting.getTitle()));
			} else {
				return Responses.error(event, String.format(com.getMEETING_PARTICIPANT_NOT_FOUND(), user.getAsMention()));
			}
		} else {
			return Responses.error(event, String.format(com.getMEETING_NOT_FOUND(), id));
		}
	}
}