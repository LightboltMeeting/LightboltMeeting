package de.lightbolt.meeting.systems.meeting.subcommands;

import de.lightbolt.meeting.command.Responses;
import de.lightbolt.meeting.data.config.guild.MeetingConfig;
import de.lightbolt.meeting.systems.meeting.MeetingManager;
import de.lightbolt.meeting.systems.meeting.MeetingStatus;
import de.lightbolt.meeting.systems.meeting.MeetingSubcommand;
import de.lightbolt.meeting.systems.meeting.dao.MeetingRepository;
import de.lightbolt.meeting.systems.meeting.model.Meeting;
import de.lightbolt.meeting.utils.localization.LocaleConfig;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import java.sql.SQLException;
import java.util.Optional;

public class EndMeetingSubcommand extends MeetingSubcommand {
	@Override
	protected ReplyCallbackAction handleMeetingCommand(SlashCommandInteractionEvent event, LocaleConfig locale, MeetingConfig config, MeetingRepository repo) throws SQLException {
		OptionMapping idOption = event.getOption("meeting-id");
		if (idOption == null) {
			return Responses.error(event, locale.getCommand().getMISSING_ARGUMENTS());
		}
		int id = (int) idOption.getAsLong();
		Optional<Meeting> meetingOptional = repo.getById(id);
		if (meetingOptional.isEmpty()) {
			return Responses.error(event, String.format(locale.getMeeting().getCommand().getMEETING_NOT_FOUND(), id));
		}
		Meeting meeting = meetingOptional.get();
		if (!MeetingManager.canEditMeeting(meeting, event.getUser().getIdLong())) {
			return Responses.error(event, locale.getMeeting().getMEETING_NO_PERMISSION());
		}
		MeetingManager manager = new MeetingManager(event.getJDA(), meeting);
		var com = locale.getMeeting().getCommand();
		if (meeting.getStatus() != MeetingStatus.ONGOING) {
			return Responses.error(event, com.getMEETING_END_FAILED_DESCRIPTION());
		}
		manager.endMeeting();
		return Responses.success(event, com.getMEETING_END_SUCCESS_TITLE(), String.format(com.getMEETING_END_SUCCESS_DESCRIPTION(), id));
	}
}
