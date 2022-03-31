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
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionType;
import net.dv8tion.jda.api.interactions.commands.build.SubcommandData;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import java.sql.SQLException;
import java.util.Arrays;
import java.util.Optional;

/**
 * <p>/meeting manage remove-admin</p>
 * Command that allows the Meeting Owner to remove Administrators from their meeting.
 */
public class RemoveAdminSubcommand extends Subcommand implements ISlashCommand {

	public RemoveAdminSubcommand() {
		this.setSubcommandData(new SubcommandData("remove-admin", "Remove Admins.")
				.addOption(OptionType.INTEGER, "meeting-id", "The Meeting's ID.", true, true)
				.addOption(OptionType.USER, "user", "The User you want to remove.", true, false));
	}

	@Override
	public void handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		try {
			LocaleConfig locale = LocalizationUtils.getLocale(Language.fromLocale(event.getUserLocale()));
			MeetingRepository repo = new MeetingRepository(Bot.dataSource.getConnection());

			var idOption = event.getOption("meeting-id");
			var userOption = event.getOption("user");
			if (userOption == null || idOption == null) {
				Responses.error(event, locale.getCommand().getMISSING_ARGUMENTS()).queue();
			}
			var id = (int) idOption.getAsLong();
			var user = userOption.getAsUser();
			var com = locale.getMeeting().getCommand();
			var meetings = repo.getByUserId(event.getUser().getIdLong());
			Optional<Meeting> meetingOptional = meetings.stream().filter(m -> m.getId() == id).findFirst();
			if (meetingOptional.isEmpty()) {
				Responses.error(event, String.format(com.getMEETING_NOT_FOUND(), id)).queue();
			}
			var meeting = meetingOptional.get();
			var admins = meeting.getAdmins();
			if (Arrays.stream(admins).anyMatch(x -> x == user.getIdLong())) {
				new MeetingManager(event.getJDA(), meeting).removeAdmin(user);
				Responses.success(event, com.getADMINS_REMOVE_SUCCESS_TITLE(),
						String.format(com.getADMINS_REMOVE_SUCCESS_DESCRIPTION(), user.getAsMention(), meeting.getTitle())).queue();
			} else {
				Responses.error(event, String.format(com.getMEETING_ADMIN_NOT_FOUND(), user.getAsMention())).queue();
			}
		} catch (SQLException e) {
			ResponseException.error("An error occurred while the bot was trying to execute a Meeting subcommand.", e);
		}
	}
}
