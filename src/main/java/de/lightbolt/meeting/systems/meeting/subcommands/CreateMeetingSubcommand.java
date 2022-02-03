package de.lightbolt.meeting.systems.meeting.subcommands;

import de.lightbolt.meeting.command.Responses;
import de.lightbolt.meeting.data.config.guild.MeetingConfig;
import de.lightbolt.meeting.systems.meeting.MeetingCreationManager;
import de.lightbolt.meeting.systems.meeting.MeetingSubcommand;
import de.lightbolt.meeting.systems.meeting.dao.MeetingRepository;
import de.lightbolt.meeting.utils.localization.LocaleConfig;
import net.dv8tion.jda.api.entities.Member;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import java.sql.SQLException;

public class CreateMeetingSubcommand extends MeetingSubcommand {
	@Override
	protected ReplyCallbackAction handleMeetingCommand(SlashCommandInteractionEvent event, LocaleConfig locale, MeetingConfig config, MeetingRepository repo) throws SQLException {
		var meetingLocale = locale.getMeeting().getCreation();
		if (repo.getByUserId(event.getUser().getIdLong()).size() > config.getMaxMeetingsPerUser()) {
			return Responses.error(event, meetingLocale.getCREATION_START_TOO_MANY_MEETING_DESCRIPTION());
		}
		if (!canCreateMeetings(event.getMember())) {
			return Responses.error(event, meetingLocale.getCREATION_START_NOT_PERMITTED_DESCRIPTION());
		}
		event.getUser().openPrivateChannel().queue(channel -> {
			Responses.info(event.getHook(), meetingLocale.getCREATION_START_RESPONSE_TITLE(), meetingLocale.getCREATION_START_RESPONSE_DESCRIPTION()).queue();
			new MeetingCreationManager(event.getJDA(), channel.getUser(), channel, locale).startMeetingFlow();
		}, error -> Responses.error(event.getHook(), meetingLocale.getCREATION_START_OPEN_PRIVATE_FAILED()).queue());
		return event.deferReply(true);
	}

	private boolean canCreateMeetings(Member member) {
		return !member.getUser().isSystem() && !member.getUser().isBot() && !member.isPending() && !member.isTimedOut();
	}
}
