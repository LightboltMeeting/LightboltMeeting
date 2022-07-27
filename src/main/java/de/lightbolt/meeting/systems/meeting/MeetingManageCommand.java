package de.lightbolt.meeting.systems.meeting;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import de.lightbolt.meeting.systems.meeting.subcommands.manage.*;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

/**
 * Handler class for all Meeting Management Subcommands.
 */
public class MeetingManageCommand extends SlashCommand {

	public MeetingManageCommand() {
		setSlashCommandData(Commands.slash("manage-meeting", "Commands for managing Meetings."));
		addSubcommands(new AddAdminSubcommand(), new AddParticipantSubcommand(), new DiscardMeetingSubcommand(), new EditMeetingSubcommand(), new RemoveAdminSubcommand(), new RemoveParticipantSubcommand());
	}
}