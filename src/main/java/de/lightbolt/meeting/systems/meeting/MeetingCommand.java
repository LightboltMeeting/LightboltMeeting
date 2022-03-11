package de.lightbolt.meeting.systems.meeting;

import com.dynxsty.dih4jda.commands.interactions.slash_command.dao.GlobalSlashCommand;
import de.lightbolt.meeting.systems.meeting.subcommands.CreateMeetingSubcommand;
import de.lightbolt.meeting.systems.meeting.subcommands.EndMeetingSubcommand;
import de.lightbolt.meeting.systems.meeting.subcommands.ListMeetingsSubcommand;
import de.lightbolt.meeting.systems.meeting.subcommands.StartMeetingSubcommand;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

import java.util.Map;

/**
 * Handler class for all Meeting Subcommands.
 */
public class MeetingCommand extends GlobalSlashCommand {

	public MeetingCommand() {
		this.setCommandData(Commands.slash("meeting", "Commands that allows users create and list Meetings."));
		this.setSubcommands(CreateMeetingSubcommand.class, EndMeetingSubcommand.class, ListMeetingsSubcommand.class, StartMeetingSubcommand.class);
	}
}