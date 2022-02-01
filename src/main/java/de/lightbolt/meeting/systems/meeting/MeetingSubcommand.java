package de.lightbolt.meeting.systems.meeting;

import de.lightbolt.meeting.Bot;
import de.lightbolt.meeting.command.ResponseException;
import de.lightbolt.meeting.command.interfaces.ISlashCommand;
import de.lightbolt.meeting.systems.meeting.dao.MeetingRepository;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import java.sql.SQLException;

/**
 * Abstract parent class for all Meeting Subcommands.
 */
public abstract class MeetingSubcommand implements ISlashCommand {
	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) throws ResponseException {
		try (var con = Bot.dataSource.getConnection()) {
			con.setAutoCommit(true);
			var reply = this.handleMeetingCommand(event, new MeetingRepository(con));
			con.commit();
			return reply;
		} catch (SQLException e) {
			throw ResponseException.error("An error occurred while the bot was trying to execute a Meeting subcommand.", e);
		}
	}

	protected abstract ReplyCallbackAction handleMeetingCommand(SlashCommandInteractionEvent event, MeetingRepository repo) throws SQLException;
}
