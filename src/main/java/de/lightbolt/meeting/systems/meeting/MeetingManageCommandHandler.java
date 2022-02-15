package de.lightbolt.meeting.systems.meeting;

import de.lightbolt.meeting.command.DelegatingCommandHandler;
import de.lightbolt.meeting.systems.meeting.subcommands.*;

/**
 * Handler class for all Meeting Management Subcommands.
 */
public class MeetingManageCommandHandler extends DelegatingCommandHandler {
	/**
	 * Adds all subcommands.
	 * @see DelegatingCommandHandler
	 */
	public MeetingManageCommandHandler() {
		this.addSubcommand("edit", new EditMeetingSubcommand());
		this.addSubcommand("discard", new DiscardMeetingSubcommand());
		this.addSubcommand("add-participant", new AddParticipantSubcommand());
		this.addSubcommand("remove-participant", new RemoveParticipantSubcommand());
		this.addSubcommand("add-admin", new AddAdminSubcommand());
		this.addSubcommand("remove-admin", new RemoveAdminSubcommand());
	}
}