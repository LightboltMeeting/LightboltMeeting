package de.lightbolt.meeting.systems.meeting;

import de.lightbolt.meeting.command.DelegatingCommandHandler;
import de.lightbolt.meeting.systems.meeting.subcommands.CreateMeetingSubcommand;

/**
 * Handler class for all Meeting Subcommands.
 */
public class MeetingCommandHandler extends DelegatingCommandHandler {
	/**
	 * Adds all subcommands. {@link DelegatingCommandHandler}
	 */
	public MeetingCommandHandler() {
		this.addSubcommand("create", new CreateMeetingSubcommand());
	}
}
