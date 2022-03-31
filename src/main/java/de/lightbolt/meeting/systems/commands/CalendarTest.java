package de.lightbolt.meeting.systems.commands;

import de.lightbolt.meeting.Bot;
import de.lightbolt.meeting.command.ResponseException;
import de.lightbolt.meeting.command.interfaces.ISlashCommand;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.OptionMapping;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

public class CalendarTest implements ISlashCommand {
	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) throws ResponseException {
		boolean multiSelect = event.getOption("multi-select", false, OptionMapping::getAsBoolean);
		return event.reply("test").addActionRows(Bot.calender.renderDayCalender(event.getUser(), multiSelect));
	}
}
