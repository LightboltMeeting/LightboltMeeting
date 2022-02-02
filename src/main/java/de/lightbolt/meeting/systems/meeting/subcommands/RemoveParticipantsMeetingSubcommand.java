package de.lightbolt.meeting.systems.meeting.subcommands;

import de.lightbolt.meeting.command.Responses;
import de.lightbolt.meeting.data.config.guild.MeetingConfig;
import de.lightbolt.meeting.systems.meeting.MeetingManager;
import de.lightbolt.meeting.systems.meeting.MeetingSubcommand;
import de.lightbolt.meeting.systems.meeting.dao.MeetingRepository;
import de.lightbolt.meeting.utils.localization.Language;
import de.lightbolt.meeting.utils.localization.LocaleConfig;
import de.lightbolt.meeting.utils.localization.LocalizationUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;
import org.apache.commons.lang3.ArrayUtils;

import java.sql.SQLException;
import java.util.Arrays;

public class RemoveParticipantsMeetingSubcommand extends MeetingSubcommand {
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
		var meetingOptional = repo.findById(id);
		if (meetingOptional.isPresent()) {
			var meeting = meetingOptional.get();
			var participants = meeting.getParticipants();
			if (Arrays.stream(participants).anyMatch(x -> x == user.getIdLong())) {
				var newParticipants = ArrayUtils.removeElement(participants, user.getIdLong());
				repo.updateParticipants(meeting, newParticipants);
				new MeetingManager(event.getGuild(), meeting).removeParticipant(user);
				return Responses.success(event, com.getPARTICIPANTS_REMOVE_SUCCESS_TITLE(),
						String.format(com.getPARTICIPANTS_REMOVE_SUCCESS_DESCRIPTION(), user.getAsMention(), meeting.getTitle()));
			}
			return Responses.error(event, String.format(com.getMEETING_PARTICIPANT_NOT_FOUND(), user.getAsMention()));
		} else {
			return Responses.error(event, String.format(com.getMEETING_NOT_FOUND(), id));
		}
	}
}
