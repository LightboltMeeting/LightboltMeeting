package de.lightbolt.meeting.systems.meeting;

import com.dynxsty.dih4jda.commands.interactions.slash_command.dao.GlobalSlashCommand;
import de.lightbolt.meeting.systems.meeting.subcommands.manage.*;
import net.dv8tion.jda.api.interactions.commands.build.Commands;

/**
 * Handler class for all Meeting Management Subcommands.
 */
public class MeetingManageCommand extends GlobalSlashCommand {

	public MeetingManageCommand() {
		this.setCommandData(Commands.slash("manage-meeting", "Commands for managing Meetings."));
		this.setSubcommands(AddAdminSubcommand.class, AddParticipantSubcommand.class, DiscardMeetingSubcommand.class, EditMeetingSubcommand.class, RemoveAdminSubcommand.class, RemoveParticipantSubcommand.class);
	}
}