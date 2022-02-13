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

import java.sql.SQLException;

/**
 * <p>/meeting discard</p>
 * Command that allows the Meeting Owner to discard their meeting.
 */
public class DiscardMeetingSubcommand extends MeetingSubcommand {
	@Override
	protected ReplyCallbackAction handleMeetingCommand(SlashCommandInteractionEvent event, LocaleConfig locale, MeetingConfig config, MeetingRepository repo) throws SQLException {
		var idOption = event.getOption("meeting-id");
		if (idOption == null) {
			return Responses.error(event, locale.getCommand().getMISSING_ARGUMENTS());
		}
		var id = (int) idOption.getAsLong();
		var meetings = repo.getByUserId(event.getUser().getIdLong());
		var com = locale.getMeeting().getCommand();
		if (meetings.stream().map(Meeting::getId).anyMatch(p -> p == id) && repo.markInactive(id)) {
			var optionalMeeting = meetings.stream().filter(p -> p.getId() == id).findFirst();
			optionalMeeting.ifPresent(m -> {
				var manager = new MeetingManager(event.getJDA(), m);
				manager.getLogChannel().delete().queue();
				manager.getVoiceChannel().delete().queue();
			});
			return Responses.success(event, com.getCANCEL_MEETING_TITLE(),
					String.format(com.getCANCEL_MEETING_DESCRIPTION(), id));
		} else {
			return Responses.error(event, String.format(com.getMEETING_NOT_FOUND(), id));
		}
	}
}
