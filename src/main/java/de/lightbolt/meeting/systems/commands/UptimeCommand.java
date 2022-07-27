package de.lightbolt.meeting.systems.commands;

import com.dynxsty.dih4jda.interactions.commands.SlashCommand;
import de.lightbolt.meeting.Bot;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.events.interaction.command.SlashCommandInteractionEvent;
import net.dv8tion.jda.api.interactions.commands.build.Commands;
import org.jetbrains.annotations.NotNull;

import java.lang.management.ManagementFactory;
import java.lang.management.RuntimeMXBean;
import java.util.concurrent.TimeUnit;

public class UptimeCommand extends SlashCommand {

	public UptimeCommand() {
		setSlashCommandData(Commands.slash("uptime", "Checks the Bot's Uptime."));
	}

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
	public void execute(@NotNull SlashCommandInteractionEvent event) {
		MessageEmbed embed = new EmbedBuilder()
				.setColor(Bot.config.getSystems().getSlashCommandConfig().getDefaultColor())
				.setAuthor(getUptime(), null, event.getJDA().getSelfUser().getEffectiveAvatarUrl())
				.build();
		event.replyEmbeds(embed).queue();
	}
}
