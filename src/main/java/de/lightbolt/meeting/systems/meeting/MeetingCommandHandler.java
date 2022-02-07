package de.lightbolt.meeting.systems.meeting;

import de.lightbolt.meeting.command.DelegatingCommandHandler;
import de.lightbolt.meeting.systems.meeting.subcommands.*;

import java.util.Map;

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
		this.addSubcommandGroup(
				"manage", new DelegatingCommandHandler(Map.of(
						"discard", new DiscardMeetingSubcommand(),
						"add-participant", new AddParticipantSubcommand(),
						"remove-participant", new RemoveParticipantSubcommand(),
						"add-admin", new AddAdminSubcommand(),
						"remove-admin", new RemoveAdminSubcommand()
				)));
	}
}
