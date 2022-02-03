package de.lightbolt.meeting.systems.commands;

import de.lightbolt.meeting.Bot;
import de.lightbolt.meeting.command.interfaces.ISlashCommand;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.requests.restaction.interactions.ReplyCallbackAction;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.concurrent.TimeUnit;

public class UptimeCommand implements ISlashCommand {

	/**
	 * Calculates the Uptimes and returns a formatted String.
	 *
	 * @return The current Uptime as a String.
	 */
	public static String getUptime() {
		RuntimeMXBean rb = ManagementFactory.getRuntimeMXBean();
		long uptimeMs = rb.getUptime();
		long uptimeDays = TimeUnit.MILLISECONDS.toDays(uptimeMs);
		uptimeMs -= TimeUnit.DAYS.toMillis(uptimeDays);
		long uptimeHours = TimeUnit.MILLISECONDS.toHours(uptimeMs);
		uptimeMs -= TimeUnit.HOURS.toMillis(uptimeHours);
		long uptimeMin = TimeUnit.MILLISECONDS.toMinutes(uptimeMs);
		uptimeMs -= TimeUnit.MINUTES.toMillis(uptimeMin);
		long uptimeSec = TimeUnit.MILLISECONDS.toSeconds(uptimeMs);
		return String.format("%sd %sh %smin %ss",
				uptimeDays, uptimeHours, uptimeMin, uptimeSec);
	}

	@Override
	public ReplyCallbackAction handleSlashCommandInteraction(SlashCommandInteractionEvent event) {
		var e = new EmbedBuilder()
				.setColor(Bot.config.get(event.getGuild()).getSlashCommand().getDefaultColor())
				.setAuthor(getUptime(), null, event.getJDA().getSelfUser().getEffectiveAvatarUrl());
		return event.replyEmbeds(e.build());
	}
}
