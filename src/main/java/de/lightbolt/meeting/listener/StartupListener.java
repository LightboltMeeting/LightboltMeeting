package de.lightbolt.meeting.listener;

import de.lightbolt.meeting.Bot;
import de.lightbolt.meeting.systems.meeting.MeetingManager;
import de.lightbolt.meeting.systems.meeting.MeetingStateManager;
import net.dv8tion.jda.api.events.ReadyEvent;
import net.dv8tion.jda.api.hooks.ListenerAdapter;

import java.util.concurrent.TimeUnit;

public class StartupListener extends ListenerAdapter {
	@Override
	public void onReady(ReadyEvent event) {
		Bot.config.flush();
		Bot.meetingStateManager = new MeetingStateManager();
		Bot.asyncPool.scheduleWithFixedDelay(() -> MeetingManager.checkActiveMeetings(event.getJDA()), 10, 300, TimeUnit.SECONDS);
	}
}
