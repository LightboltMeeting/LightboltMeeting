package de.lightbolt.meeting.listener;

import de.lightbolt.meeting.Bot;
import de.lightbolt.meeting.utils.localization.Language;
import de.lightbolt.meeting.utils.localization.LocaleConfig;
import de.lightbolt.meeting.utils.localization.LocalizationUtils;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.guild.GuildJoinEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class GuildJoinListener extends ListenerAdapter {
	@Override
	public void onGuildJoin(@NotNull GuildJoinEvent event) {
		for (Guild guild : event.getJDA().getGuilds()) {
			Bot.interactionHandler.registerCommands(guild);
		}
//		LocaleConfig locale = LocalizationUtils.getLocale(Language.fromLocale(event.getGuild().getLocale()));
//		TextChannel channel = event.getGuild().getDefaultChannel();
//		if (channel == null) return;
//		channel.sendMessageFormat(locale.getGUILD_JOIN_MESSAGE()).queue();
	}
}
