package de.lightbolt.meeting.systems.meeting;

import de.lightbolt.meeting.command.eventwaiter.EventWaiter;
import de.lightbolt.meeting.utils.localization.LocaleConfig;
import lombok.RequiredArgsConstructor;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.PrivateChannel;
import net.dv8tion.jda.api.entities.User;

import java.util.List;

@RequiredArgsConstructor
public class MeetingCreationManager {
	private final User user;
	private final PrivateChannel channel;
	private final LocaleConfig.MeetingConfig config;
	private final EventWaiter waiter;

	// TODO: add Meeting Flow
	public void startMeetingFlow() {
		var mutualGuilds = user.getMutualGuilds();
		Guild guild = null;
		if (mutualGuilds.size() > 1) {
			sendMutualGuildsEmbed(this.config, this.channel, mutualGuilds);
		} else {
			guild = user.getMutualGuilds().get(0);
		}
		sendLanguageEmbed(config, this.channel, guild);
	}

	private void sendMutualGuildsEmbed(LocaleConfig.MeetingConfig locale, PrivateChannel channel, List<Guild> mutualGuilds) {
		channel.sendMessageEmbeds(MeetingManager.buildMeetingEmbed(
				0, locale,
				String.format(locale.getCREATION_DM_STEP_0_DESCRIPTION(), channel.getUser().getAsMention(), mutualGuilds.size()))
		).queue();
	}

	private void sendLanguageEmbed(LocaleConfig.MeetingConfig locale, PrivateChannel channel, Guild guild) {
		channel.sendMessageEmbeds(MeetingManager.buildMeetingEmbed(
				1, locale,
				String.format(locale.getCREATION_DM_STEP_1_DESCRIPTION(), guild.getName()))
		).queue();
	}
}
