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

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;

/**
 * <p>/meeting manage add-admin</p>
 * Command that allows the Meeting Owner to add Administrators to their meeting.
 */
public class AddAdminSubcommand extends MeetingSubcommand {
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
		var meetings = repo.getByUserId(event.getUser().getIdLong());
		Optional<Meeting> meetingOptional = meetings.stream().filter(m -> m.getId() == id).findFirst();
		if (meetingOptional.isEmpty()) {
			return Responses.error(event, String.format(com.getMEETING_NOT_FOUND(), id));
		}
		var meeting = meetingOptional.get();
		var participants = meeting.getParticipants();
		var admins = meeting.getAdmins();
		if (!Arrays.stream(participants).anyMatch(x -> x == user.getIdLong())) {
			return Responses.error(event, String.format(com.getMEETING_ADMIN_NOT_A_PARTICIPANT(), user.getAsMention()));
		}
		if (Arrays.stream(admins).anyMatch(x -> x == user.getIdLong())) {
			return Responses.error(event, String.format(com.getMEETING_ADMIN_ALREADY_ADDED(), user.getAsMention()));
		}
		new MeetingManager(event.getJDA(), meeting).addAdmin(user);
		return Responses.success(event, com.getADMINS_ADD_SUCCESS_TITLE(), String.format(com.getADMINS_ADD_SUCCESS_DESCRIPTION(), user.getAsMention(), meeting.getTitle()));
	}
}
