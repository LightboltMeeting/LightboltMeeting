package de.lightbolt.meeting.systems.meeting.subcommands.manage;

import com.dynxsty.dih4jda.commands.interactions.slash_command.ISlashCommand;
import com.dynxsty.dih4jda.commands.interactions.slash_command.dao.Subcommand;
import de.lightbolt.meeting.Bot;
import de.lightbolt.meeting.utils.ResponseException;
import de.lightbolt.meeting.utils.Responses;
import de.lightbolt.meeting.data.config.SystemsConfig;
import de.lightbolt.meeting.systems.meeting.MeetingManager;
import de.lightbolt.meeting.systems.meeting.dao.MeetingRepository;
import de.lightbolt.meeting.systems.meeting.model.Meeting;
import de.lightbolt.meeting.utils.localization.Language;
import de.lightbolt.meeting.utils.localization.LocaleConfig;
import de.lightbolt.meeting.utils.localization.LocalizationUtils;
import net.dv8tion.jda.api.entities.User;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * <p>/meeting manage add-participant</p>
 * Command that allows Meeting Administrators to add participants to their meeting.
 */
public class AddParticipantSubcommand extends Subcommand implements ISlashCommand {
	private final int MEMBER_OPTIONS = 9;

	public AddParticipantSubcommand() {
		this.setSubcommandData(new SubcommandData("add-participant", "Add participants to your Meeting.")
				.addOption(OptionType.INTEGER, "meeting-id", "The Meeting's ID.", true, true)
				.addOption(OptionType.USER, "user-1", "The User you want to add.", true, false)
				.addOption(OptionType.USER, "user-2", "The User you want to add.", false, false)
				.addOption(OptionType.USER, "user-3", "The User you want to add.", false, false)
				.addOption(OptionType.USER, "user-4", "The User you want to add.", false, false)
				.addOption(OptionType.USER, "user-5", "The User you want to add.", false, false)
				.addOption(OptionType.USER, "user-6", "The User you want to add.", false, false)
				.addOption(OptionType.USER, "user-7", "The User you want to add.", false, false)
				.addOption(OptionType.USER, "user-8", "The User you want to add.", false, false)
				.addOption(OptionType.USER, "user-9", "The User you want to add.", false, false));
	}

	@Override
	public void handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		try {
			LocaleConfig locale = LocalizationUtils.getLocale(Language.fromLocale(event.getUserLocale()));
			MeetingRepository repo = new MeetingRepository(Bot.dataSource.getConnection());

			var idOption = event.getOption("meeting-id");
			if (idOption == null) {
				Responses.error(event, locale.getCommand().getMISSING_ARGUMENTS()).queue();
			}
			List<User> users = new ArrayList<>();
			for (int i = 1; i < MEMBER_OPTIONS + 1 ; i++) {
				var userOption = event.getOption("user-" + i);
				if (userOption == null) continue;
				users.add(userOption.getAsUser());
			}
			var id = (int) idOption.getAsLong();
			var com = locale.getMeeting().getCommand();
			Optional<Meeting> meetingOptional = repo.getById(id);
			if (meetingOptional.isEmpty()) {
				Responses.error(event, String.format(com.getMEETING_NOT_FOUND(), id)).queue();
			}
			var meeting = meetingOptional.get();
			if (!MeetingManager.canEditMeeting(meeting, event.getUser().getIdLong())) {
				Responses.error(event, locale.getMeeting().getMEETING_NO_PERMISSION()).queue();
			}
			long[] participants = meeting.getParticipants();
			for (User user : users) {
				if (Arrays.stream(participants).anyMatch(x -> x == user.getIdLong())) {
					Responses.error(event, String.format(com.getMEETING_PARTICIPANT_ALREADY_ADDED(), user.getAsMention())).queue();
				}
			}
			MeetingManager manager = new MeetingManager(event.getJDA(), meeting);
			manager.addParticipants(users.stream().mapToLong(User::getIdLong).toArray());
			Responses.success(event, com.getPARTICIPANTS_ADD_SUCCESS_TITLE(), String.format(com.getPARTICIPANTS_ADD_SUCCESS_DESCRIPTION(), users.stream().map(User::getAsMention).collect(Collectors.joining(", ")), meeting.getTitle())).queue();
		} catch (SQLException e) {
			ResponseException.error("An error occurred while the bot was trying to execute a Meeting subcommand.", e);
		}
	}
}
