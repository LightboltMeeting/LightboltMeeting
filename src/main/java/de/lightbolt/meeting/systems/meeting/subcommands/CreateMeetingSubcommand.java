package de.lightbolt.meeting.systems.meeting.subcommands;

import de.lightbolt.meeting.command.Responses;
import de.lightbolt.meeting.command.eventwaiter.EventWaiter;
import de.lightbolt.meeting.systems.meeting.MeetingCreationManager;
import de.lightbolt.meeting.systems.meeting.MeetingSubcommand;
import de.lightbolt.meeting.systems.meeting.dao.MeetingRepository;
import de.lightbolt.meeting.utils.localization.LocaleConfig;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import java.sql.SQLException;

public class CreateMeetingSubcommand extends MeetingSubcommand {
	@Override
	protected ReplyCallbackAction handleMeetingCommand(SlashCommandInteractionEvent event, LocaleConfig.MeetingConfig locale, MeetingRepository repo) throws SQLException {
		event.getUser().openPrivateChannel().queue(channel -> {
		new MeetingCreationManager(channel.getUser(), channel, locale, new EventWaiter()).startMeetingFlow();
		Responses.info(event.getHook(), locale.getCREATION_START_RESPONSE_TITLE(), locale.getCREATION_START_RESPONSE_DESCRIPTION()).queue();
		}, error -> Responses.error(event.getHook(), locale.getCREATION_START_OPEN_PRIVATE_FAILED()).queue());
		return event.deferReply(true);
	}
}
