package de.lightbolt.meeting.systems.meeting;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import de.lightbolt.meeting.systems.meeting.subcommands.CreateMeetingSubcommand;
import de.lightbolt.meeting.systems.meeting.subcommands.EndMeetingSubcommand;
import de.lightbolt.meeting.systems.meeting.subcommands.ListMeetingsSubcommand;
import de.lightbolt.meeting.systems.meeting.subcommands.StartMeetingSubcommand;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

/**
 * Handler class for all Meeting Subcommands.
 */
public class MeetingCommand extends SlashCommand {

	public MeetingCommand() {
		setSlashCommandData(Commands.slash("meeting", "Commands that allows users create and list Meetings."));
		addSubcommands(new CreateMeetingSubcommand(), new EndMeetingSubcommand(), new ListMeetingsSubcommand(), new StartMeetingSubcommand());
	}
}