package de.lightbolt.meeting.listener;

import de.lightbolt.meeting.data.h2db.DbHelper;
import de.lightbolt.meeting.systems.meeting.MeetingManager;
import de.lightbolt.meeting.systems.meeting.dao.MeetingRepository;
import de.lightbolt.meeting.utils.Responses;
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
			case "meeting-faq" -> handleMeetingFAQ(event);
			default -> Responses.error(event.getHook(), locale.getCommand().getUNKNOWN_INTERACTION()).queue();
		}
	}

	private void handleMeetingFAQ(ButtonInteractionEvent event) {
		event.deferReply(true).queue();
		DbHelper.doDaoAction(MeetingRepository::new, dao -> {
			var meetingOptional = dao.getActive().stream().filter(m -> m.getLogChannelId() == event.getChannel().getIdLong()).findFirst();
			if (meetingOptional.isEmpty()) {
				event.getHook().sendMessage("Could not find corresponding Meeting").queue();
				return;
			}
			event.getHook()
					.sendMessageEmbeds(new MeetingManager(event.getJDA(), meetingOptional.get()).buildMeetingFAQEmbed())
					.queue();
		});
	}
}
