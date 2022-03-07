package de.lightbolt.meeting.systems.meeting.subcommands;

import de.lightbolt.meeting.data.config.SystemsConfig;
import de.lightbolt.meeting.systems.meeting.MeetingManager;
import de.lightbolt.meeting.systems.meeting.MeetingSubcommand;
import de.lightbolt.meeting.systems.meeting.dao.MeetingRepository;
import de.lightbolt.meeting.systems.meeting.model.Meeting;
import de.lightbolt.meeting.utils.localization.LocaleConfig;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>/meeting list</p>
 * Command that allows every user to list their active Meetings.
 */
public class ListMeetingsSubcommand extends MeetingSubcommand {
	@Override
	protected ReplyCallbackAction handleMeetingCommand(SlashCommandInteractionEvent event, LocaleConfig locale, SystemsConfig.MeetingConfig config, MeetingRepository repo) throws SQLException {
		var meetings = repo.getByUserId(event.getUser().getIdLong());
		return event.replyFormat(locale.getMeeting().getCommand().getLIST_REPLY_TEXT(), meetings.size())
				.addEmbeds(buildMeetingListEmbed(event.getUser(), locale, meetings));
	}

	private List<MessageEmbed> buildMeetingListEmbed(User user, LocaleConfig locale, List<Meeting> meetings) {
		List<MessageEmbed> embeds = new ArrayList<>();
		meetings.forEach(m -> embeds.add(MeetingManager.buildMeetingEmbed(m, user, locale)));
		return embeds;
	}
}
