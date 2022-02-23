package de.lightbolt.meeting.systems.meeting;

import de.lightbolt.meeting.command.DelegatingCommandHandler;
import de.lightbolt.meeting.systems.meeting.subcommands.*;

import java.util.Map;

/**
 * Handler class for all Meeting Subcommands.
 */
public class MeetingCommandHandler extends DelegatingCommandHandler {
	/**
	 * Adds all subcommands.
	 * @see DelegatingCommandHandler
	 */
	public MeetingCommandHandler() {
		this.addSubcommand("create", new CreateMeetingSubcommand());
		this.addSubcommand("list", new ListMeetingsSubcommand());
		this.addSubcommand("start", new StartMeetingSubcommand());
		this.addSubcommand("end", new EndMeetingSubcommand());
	}
}