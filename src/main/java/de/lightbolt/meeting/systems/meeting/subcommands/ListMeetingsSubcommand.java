package de.lightbolt.meeting.systems.meeting.subcommands;

import com.dynxsty.dih4jda.commands.interactions.slash_command.ISlashCommand;
import com.dynxsty.dih4jda.commands.interactions.slash_command.dao.Subcommand;
import de.lightbolt.meeting.Bot;
import de.lightbolt.meeting.systems.meeting.MeetingManager;
import de.lightbolt.meeting.systems.meeting.dao.MeetingRepository;
import de.lightbolt.meeting.systems.meeting.model.Meeting;
import de.lightbolt.meeting.utils.ResponseException;
import de.lightbolt.meeting.utils.localization.Language;
import de.lightbolt.meeting.utils.localization.LocaleConfig;
import de.lightbolt.meeting.utils.localization.LocalizationUtils;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;


import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;

/**
 * <p>/meeting list</p>
 * Command that allows every user to list their active Meetings.
 */
public class ListMeetingsSubcommand extends Subcommand implements ISlashCommand {

	public ListMeetingsSubcommand() {
		this.setSubcommandData(new SubcommandData("list", "List all your Meeting."));
	}

	@Override
	public void handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		try {
			LocaleConfig locale = LocalizationUtils.getLocale(Language.fromLocale(event.getUserLocale()));
			MeetingRepository repo = new MeetingRepository(Bot.dataSource.getConnection());

			var meetings = repo.getByUserId(event.getUser().getIdLong());
			event.replyFormat(locale.getMeeting().getCommand().getLIST_REPLY_TEXT(), meetings.size())
					.addEmbeds(buildMeetingListEmbed(event.getUser(), locale, meetings)).setEphemeral(true).queue();
		} catch(SQLException e) {
			ResponseException.error("An error occurred while the bot was trying to execute a Meeting subcommand.", e);
		}
	}

	private List<MessageEmbed> buildMeetingListEmbed(User user, LocaleConfig locale, List<Meeting> meetings) {
		List<MessageEmbed> embeds = new ArrayList<>();
		meetings.forEach(m -> embeds.add(MeetingManager.buildMeetingEmbed(m, user, locale)));
		return embeds;
	}
}
