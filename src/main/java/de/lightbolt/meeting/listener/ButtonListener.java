package de.lightbolt.meeting.listener;

import de.lightbolt.meeting.data.h2db.DbHelper;
import de.lightbolt.meeting.systems.meeting.MeetingManager;
import de.lightbolt.meeting.systems.meeting.dao.MeetingRepository;
import net.dv8tion.jda.api.events.interaction.component.ButtonInteractionEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;
import org.jetbrains.annotations.NotNull;

public class ButtonListener extends ListenerAdapter {
	@Override
	public void onButtonInteraction(@NotNull ButtonInteractionEvent event) {
		String[] id = event.getComponentId().split(":");
		switch (id[0]) {
			case "meeting-faq" -> handleMeetingFAQ(event);
		}
	}

	private void handleMeetingFAQ(ButtonInteractionEvent event) {
		event.deferReply(true).queue();
		DbHelper.doDaoAction(MeetingRepository::new, dao -> {
			var meetingOptional = dao.getActive().stream().filter(m -> m.getLogChannelId() == event.getChannel().getIdLong()).findFirst();
			if (meetingOptional.isEmpty()) {
				return;
			}
			event.getHook().sendMessageEmbeds(new MeetingManager(event.getJDA(), meetingOptional.get()).buildMeetingFAQEmbed())
					.setEphemeral(true)
					.queue();
		});
	}
}
