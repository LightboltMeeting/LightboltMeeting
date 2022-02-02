package de.lightbolt.meeting.systems.meeting;

import de.lightbolt.meeting.command.DelegatingCommandHandler;
import de.lightbolt.meeting.systems.meeting.subcommands.*;

/**
 * Handler class for all Meeting Subcommands.
 */
public class MeetingCommandHandler extends DelegatingCommandHandler {
	/**
	 * Adds all subcommands. {@link DelegatingCommandHandler}
	 */
	public MeetingCommandHandler() {
		this.addSubcommand("create", new CreateMeetingSubcommand());
		this.addSubcommand("list", new ListMeetingsSubcommand());
		this.addSubcommand("cancel", new CancelMeetingSubcommand());
		this.addSubcommand("add-participants", new AddParticipantsMeetingSubcommand());
		this.addSubcommand("remove-participants", new RemoveParticipantsMeetingSubcommand());
	}
}
