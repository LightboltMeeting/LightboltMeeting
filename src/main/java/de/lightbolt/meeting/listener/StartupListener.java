package de.lightbolt.meeting.listener;

import de.lightbolt.meeting.Bot;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

public class StartupListener extends ListenerAdapter {
	@Override
	public void onReady(ReadyEvent event) {
		for (var guild : event.getJDA().getGuilds()) {
			Bot.interactionHandler.registerCommands(guild);
		}
	}
}
