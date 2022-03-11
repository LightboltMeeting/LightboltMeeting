package de.lightbolt.meeting.systems.meeting.subcommands;

import com.dynxsty.dih4jda.commands.interactions.slash_command.ISlashCommand;
import com.dynxsty.dih4jda.commands.interactions.slash_command.dao.Subcommand;
import de.lightbolt.meeting.Bot;
import de.lightbolt.meeting.utils.ResponseException;
import de.lightbolt.meeting.utils.Responses;
import de.lightbolt.meeting.systems.meeting.MeetingManager;
import de.lightbolt.meeting.systems.meeting.dao.MeetingRepository;
import de.lightbolt.meeting.systems.meeting.model.Meeting;
import de.lightbolt.meeting.utils.localization.Language;
import de.lightbolt.meeting.utils.localization.LocaleConfig;
import de.lightbolt.meeting.utils.localization.LocalizationUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;

import java.sql.SQLException;
import java.util.Optional;

public class StartMeetingSubcommand extends Subcommand implements ISlashCommand {

	public StartMeetingSubcommand() {
		this.setSubcommandData(new SubcommandData("start", "Start a Meeting manually.")
				.addOption(OptionType.INTEGER, "meeting-id", "The Meeting's ID.", true, true));
	}

	@Override
	public void handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		try {
			LocaleConfig locale = LocalizationUtils.getLocale(Language.fromLocale(event.getUserLocale()));
			MeetingRepository repo = new MeetingRepository(Bot.dataSource.getConnection());

			OptionMapping idOption = event.getOption("meeting-id");
			if (idOption == null) {
				Responses.error(event, locale.getCommand().getMISSING_ARGUMENTS()).queue();
			}
			int id = (int) idOption.getAsLong();
			Optional<Meeting> meetingOptional = repo.getById(id);
			if (meetingOptional.isEmpty()) {
				Responses.error(event, String.format(locale.getMeeting().getCommand().getMEETING_NOT_FOUND(), id)).queue();
			}
			Meeting meeting = meetingOptional.get();
			if (!MeetingManager.canEditMeeting(meeting, event.getUser().getIdLong())) {
				Responses.error(event, locale.getMeeting().getMEETING_NO_PERMISSION()).queue();
			}
			Bot.meetingStateManager.cancelMeetingSchedule(meeting);
			new MeetingManager(event.getJDA(), meeting).startMeeting(event.getUser());
			var com = locale.getMeeting().getCommand();
			Responses.success(event, com.getMEETING_START_SUCCESS_TITLE(), String.format(com.getMEETING_START_SUCCESS_DESCRIPTION(), meeting.getTitle())).queue();
		} catch(SQLException e) {
			ResponseException.error("An error occurred while the bot was trying to execute a Meeting subcommand.", e);
		}

	}
}
