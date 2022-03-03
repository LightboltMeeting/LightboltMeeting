package de.lightbolt.meeting.systems.meeting.subcommands.manage;

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

/**
 * <p>/meeting manage remove-participant</p>
 * Command that allows Meeting Administrators to remove participants from their meeting.
 */
public class RemoveParticipantSubcommand extends MeetingSubcommand {
	@Override
	protected ReplyCallbackAction handleMeetingCommand(SlashCommandInteractionEvent event, LocaleConfig locale, MeetingConfig config, MeetingRepository repo) throws SQLException {
		var idOption = event.getOption("meeting-id");
		var userOption = event.getOption("user");
		if (userOption == null || idOption == null) {
			return Responses.error(event, locale.getCommand().getMISSING_ARGUMENTS());
		}
		var id = (int) idOption.getAsLong();
		var user = userOption.getAsUser();
		var com = locale.getMeeting().getCommand();
		Optional<Meeting> meetingOptional = repo.getById(id);
		if (meetingOptional.isEmpty()) {
			return Responses.error(event, String.format(com.getMEETING_NOT_FOUND(), id));
		}
		var meeting = meetingOptional.get();
		if (!MeetingManager.canEditMeeting(meeting, event.getUser().getIdLong())) {
			return Responses.error(event, locale.getMeeting().getMEETING_NO_PERMISSION());
		}
		var participants = meeting.getParticipants();
		var admins = meeting.getAdmins();
		if (Arrays.stream(participants).anyMatch(x -> x == user.getIdLong())) {
			if (Arrays.stream(admins).anyMatch(x -> x == user.getIdLong())) {
				meeting.setAdmins(ArrayUtils.removeElement(admins, user.getIdLong()));
				repo.update(meeting.getId(), meeting);
			}
			new MeetingManager(event.getJDA(), meeting).removeParticipant(user);
			return Responses.success(event, com.getPARTICIPANTS_REMOVE_SUCCESS_TITLE(),
					String.format(com.getPARTICIPANTS_REMOVE_SUCCESS_DESCRIPTION(), user.getAsMention(), meeting.getTitle()));
		} else {
			return Responses.error(event, String.format(com.getMEETING_PARTICIPANT_NOT_FOUND(), user.getAsMention()));
		}
	}
}
