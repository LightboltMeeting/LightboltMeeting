package de.lightbolt.meeting.systems.commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import de.lightbolt.meeting.Bot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

public class PingCommand extends SlashCommand {

	public PingCommand() {
		setSlashCommandData(Commands.slash("ping", "Checks the bot's Gateway Ping."));
	}

	@Override
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		MessageEmbed embed = new EmbedBuilder()
				.setAuthor(event.getJDA().getGatewayPing() + "ms", null, event.getJDA().getSelfUser().getEffectiveAvatarUrl())
				.setColor(Bot.config.getSystems().getSlashCommandConfig().getDefaultColor())
				.build();
		event.replyEmbeds(embed).queue();
	}
}
