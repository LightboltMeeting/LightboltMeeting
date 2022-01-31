package de.lightbolt.meeting.systems.commands;

import de.lightbolt.meeting.Bot;
import de.lightbolt.meeting.command.interfaces.ISlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.time.Instant;
import java.time.temporal.ChronoUnit;

public class UptimeCommand implements ISlashCommand {

	/**
	 * Calculates the Uptimes and returns a formatted String.
	 *
	 * @return The current Uptime as a String.
	 */
	public String getUptime() {
		RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
		var startDate = Instant.now().minus(rb.getUptime(), ChronoUnit.MILLIS);
		return String.format("<t:%s:R>", startDate);
	}

	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		String botImage = event.getJDA().getSelfUser().getAvatarUrl();
		var e = new EmbedBuilder()
				.setColor(Bot.config.get(event.getGuild()).getSlashCommand().getDefaultColor())
				.setAuthor(getUptime(), null, botImage);

		return event.replyEmbeds(e.build());
	}
}
