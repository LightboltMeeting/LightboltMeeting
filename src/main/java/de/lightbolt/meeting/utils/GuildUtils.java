package de.lightbolt.meeting.utils;

import de.lightbolt.meeting.Bot;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.MessageChannel;

public class GuildUtils {

	private GuildUtils() {
	}

	public static MessageChannel getLogChannel(Guild guild) {
		return Bot.config.get(guild).getModeration().getLogChannel();
	}

}
