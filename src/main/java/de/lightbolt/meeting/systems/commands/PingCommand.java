package de.lightbolt.meeting.systems.commands;

import de.lightbolt.meeting.Bot;
import de.lightbolt.meeting.command.interfaces.ISlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

public class PingCommand implements ISlashCommand {
	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		var e = new EmbedBuilder()
				.setAuthor(event.getJDA().getGatewayPing() + "ms", null, event.getJDA().getSelfUser().getEffectiveAvatarUrl())
				.setColor(Bot.config.getSystems().getSlashCommandConfig().getDefaultColor())
				.build();
		return event.replyEmbeds(e);
	}
}
