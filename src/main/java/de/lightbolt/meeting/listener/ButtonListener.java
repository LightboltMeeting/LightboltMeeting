package de.lightbolt.meeting.listener;

import de.lightbolt.meeting.systems.meeting.MeetingFaqEmbed;
import de.lightbolt.meeting.utils.localization.Language;
import de.lightbolt.meeting.utils.localization.LocaleConfig;
import de.lightbolt.meeting.utils.localization.LocalizationUtils;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class ButtonListener extends ListenerAdapter {
	@Override
	public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
		String[] id = event.getComponentId().split(":");
		LocaleConfig locale = LocalizationUtils.getLocale(Language.fromLocale(event.getUserLocale()));
		switch (id[0]) {
			case "meeting-faq" -> event.replyEmbeds(new MeetingFaqEmbed(locale).build()).setEphemeral(true).queue();
		}
	}
}
