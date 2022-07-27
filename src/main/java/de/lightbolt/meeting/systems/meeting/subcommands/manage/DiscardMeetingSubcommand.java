package de.lightbolt.meeting.systems.meeting.subcommands.manage;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import de.lightbolt.meeting.Bot;
import de.lightbolt.meeting.systems.meeting.MeetingManager;
import de.lightbolt.meeting.systems.meeting.MeetingStatus;
import de.lightbolt.meeting.systems.meeting.dao.MeetingRepository;
import de.lightbolt.meeting.systems.meeting.model.Meeting;
import de.lightbolt.meeting.utils.ResponseException;
import de.lightbolt.meeting.utils.Responses;
import de.lightbolt.meeting.utils.localization.Language;
import de.lightbolt.meeting.utils.localization.LocaleConfig;
import de.lightbolt.meeting.utils.localization.LocalizationUtils;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import org.jetbrains.annotations.NotNull;

import java.sql.SQLException;
import java.util.Optional;

/**
 * <p>/meeting discard</p>
 * Command that allows the Meeting Owner to discard their meeting.
 */
public class DiscardMeetingSubcommand extends SlashCommand.Subcommand {

	public DiscardMeetingSubcommand() {
		setSubcommandData(new SubcommandData("discard", "Discards a single Meeting.")
				.addOption(OptionType.INTEGER, "meeting-id", "The Meeting's ID.", true, true));
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
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
			MeetingManager manager = new MeetingManager(event.getJDA(), meeting);
			var com = locale.getMeeting().getCommand();
			if (meeting.getStatus() == MeetingStatus.ONGOING) {
				Responses.error(event, com.getMEETING_DISCARD_FAILED_DESCRIPTION()).queue();
			}
			manager.endMeeting();
			Responses.success(event, com.getCANCEL_MEETING_TITLE(), String.format(com.getCANCEL_MEETING_DESCRIPTION(), id)).queue();
		} catch (SQLException e) {
			ResponseException.error("An error occurred while the bot was trying to execute a Meeting subcommand.", e);
		}
	}
}
