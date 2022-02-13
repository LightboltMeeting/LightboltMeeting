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
		this.addSubcommand("edit", new EditMeetingSubcommand());
		this.addSubcommand("discard", new DiscardMeetingSubcommand());
		this.addSubcommand("list", new ListMeetingsSubcommand());
		this.addSubcommandGroup(
				"manage", new DelegatingCommandHandler(Map.of(
						"add-participant", new AddParticipantSubcommand(),
						"remove-participant", new RemoveParticipantSubcommand(),
						"add-admin", new AddAdminSubcommand(),
						"remove-admin", new RemoveAdminSubcommand()
				)));
	}
}
