package de.lightbolt.meeting.systems.meeting.subcommands;

import de.lightbolt.meeting.command.Responses;
import de.lightbolt.meeting.systems.meeting.MeetingCreationManager;
import de.lightbolt.meeting.systems.meeting.MeetingSubcommand;
import de.lightbolt.meeting.systems.meeting.dao.MeetingRepository;
import de.lightbolt.meeting.utils.localization.LocaleConfig;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

public class CreateMeetingSubcommand extends MeetingSubcommand {
	@Override
	protected ReplyCallbackAction handleMeetingCommand(SlashCommandInteractionEvent event, LocaleConfig.MeetingCreationConfig locale, MeetingRepository repo) {
		event.getUser().openPrivateChannel().queue(channel -> {
			Responses.info(event.getHook(), locale.getCREATION_START_RESPONSE_TITLE(), locale.getCREATION_START_RESPONSE_DESCRIPTION()).queue();
			new MeetingCreationManager(event.getJDA(), channel.getUser(), channel, locale).startMeetingFlow();
		}, error -> Responses.error(event.getHook(), locale.getCREATION_START_OPEN_PRIVATE_FAILED()).queue());
		return event.deferReply(true);
	}
}
