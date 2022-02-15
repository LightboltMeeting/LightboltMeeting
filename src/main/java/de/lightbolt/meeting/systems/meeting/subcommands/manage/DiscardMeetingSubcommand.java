package de.lightbolt.meeting.systems.meeting.subcommands.manage;

import de.lightbolt.meeting.annotations.MissingLocale;
import de.lightbolt.meeting.command.Responses;
import de.lightbolt.meeting.data.config.guild.MeetingConfig;
import de.lightbolt.meeting.systems.meeting.MeetingManager;
import de.lightbolt.meeting.systems.meeting.MeetingSubcommand;
import de.lightbolt.meeting.systems.meeting.dao.MeetingRepository;
import de.lightbolt.meeting.systems.meeting.model.Meeting;
import de.lightbolt.meeting.utils.localization.LocaleConfig;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import java.sql.SQLException;
import java.util.Optional;

/**
 * <p>/meeting discard</p>
 * Command that allows the Meeting Owner to discard their meeting.
 */
@MissingLocale
public class DiscardMeetingSubcommand extends MeetingSubcommand {
	@Override
	protected ReplyCallbackAction handleMeetingCommand(SlashCommandInteractionEvent event, LocaleConfig locale, MeetingConfig config, MeetingRepository repo) throws SQLException {
		OptionMapping idOption = event.getOption("meeting-id");
		if (idOption == null) {
			return Responses.error(event, locale.getCommand().getMISSING_ARGUMENTS());
		}
		int id = (int) idOption.getAsLong();
		Optional<Meeting> meetingOptional = repo.findById(id);
		if (meetingOptional.isEmpty()) {
			return Responses.error(event, String.format(locale.getMeeting().getCommand().getMEETING_NOT_FOUND(), id));
		}
		Meeting meeting = meetingOptional.get();
		if (!MeetingManager.canEditMeeting(meeting, event.getUser().getIdLong())) {
			return Responses.error(event, locale.getMeeting().getMEETING_NO_PERMISSION());
		}
		MeetingManager manager = new MeetingManager(event.getJDA(), meeting);
		if (meeting.isOngoing()) {
			return Responses.error(event, "Could not discard meeting. Use `/meeting end` instead.");
		}
		manager.discardMeeting();
		var com = locale.getMeeting().getCommand();
		return Responses.success(event, com.getCANCEL_MEETING_TITLE(), String.format(com.getCANCEL_MEETING_DESCRIPTION(), id));
	}
}
