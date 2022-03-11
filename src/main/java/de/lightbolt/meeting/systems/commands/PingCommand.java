package de.lightbolt.meeting.systems.commands;

import com.dynxsty.dih4jda.commands.interactions.slash_command.ISlashCommand;
import com.dynxsty.dih4jda.commands.interactions.slash_command.dao.GlobalSlashCommand;
import de.lightbolt.meeting.Bot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

public class PingCommand extends GlobalSlashCommand implements ISlashCommand {

	public PingCommand() {
		this.setCommandData(Commands.slash("ping", "Checks the Bot's Gateway Ping."));
	}

	@Override
	public void handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		var e = new EmbedBuilder()
				.setAuthor(event.getJDA().getGatewayPing() + "ms", null, event.getJDA().getSelfUser().getEffectiveAvatarUrl())
				.setColor(Bot.config.getSystems().getSlashCommandConfig().getDefaultColor())
				.build();
		event.replyEmbeds(e).queue();
	}
}
