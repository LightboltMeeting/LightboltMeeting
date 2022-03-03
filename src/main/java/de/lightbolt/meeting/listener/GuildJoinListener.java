package de.lightbolt.meeting.listener;

import de.lightbolt.meeting.Bot;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class GuildJoinListener extends ListenerAdapter {
	@Override
	public void onGuildJoin(@NotNull GuildJoinEvent event) {
		for (Guild guild : event.getJDA().getGuilds()) {
			Bot.interactionHandler.registerCommands(guild);
		}
	}
}
