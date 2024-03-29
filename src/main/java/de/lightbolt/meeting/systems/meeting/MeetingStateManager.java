package de.lightbolt.meeting.systems.meeting;

import de.lightbolt.meeting.Bot;
import de.lightbolt.meeting.data.config.SystemsConfig;
import de.lightbolt.meeting.systems.meeting.dao.MeetingRepository;
import de.lightbolt.meeting.systems.meeting.jobs.MeetingReminderJob;
import de.lightbolt.meeting.systems.meeting.jobs.MeetingStartJob;
import de.lightbolt.meeting.systems.meeting.model.Meeting;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.entities.Guild;
import org.quartz.*;
import org.quartz.impl.StdSchedulerFactory;

import java.util.Date;
import java.util.List;

import static org.quartz.JobBuilder.newJob;
import static org.quartz.TriggerBuilder.newTrigger;

@Slf4j
public class MeetingStateManager {
	public List<Meeting> activeMeetings;
	public Scheduler scheduler;

	public MeetingStateManager() {
		try {
			scheduler = new StdSchedulerFactory().getScheduler();
			scheduler.start();
			this.activeMeetings = new MeetingRepository(Bot.dataSource.getConnection()).getActive();
			for (Meeting meeting : activeMeetings) {
				scheduleMeeting(meeting);
			}
			log.info("Scheduled {} Meetings", activeMeetings.size());
		} catch (Exception e) {
			e.printStackTrace();
		}
	}

	public void scheduleMeeting(Meeting meeting) {
		Guild guild = Bot.jda.getGuildById(meeting.getGuildId());
		if (guild == null) {
			log.error("Unknown Guild of Meeting: " + meeting);
			return;
		}
		SystemsConfig.MeetingConfig meetingConfig = Bot.config.getSystems().getMeetingConfig();
		try {
			//Schedule Job for Meeting Start
			JobDetail job = newJob(MeetingStartJob.class)
					.withIdentity(meeting.getId() + "-start")
					.build();
			Date runTime = new Date(meeting.getDueAt().getTime());
			Trigger trigger = newTrigger()
					.withIdentity(meeting.getId() + "-starttrigger")
					.startAt(runTime)
					.build();
			scheduler.scheduleJob(job, trigger);

			//Schedule Jobs for Meeting Reminders
			for (Integer reminder : meetingConfig.getMeetingReminders()) {
				if (new Date().after(new Date(meeting.getDueAt().getTime() - (reminder * 60000)))) return;
				JobDetail reminderJob = newJob(MeetingReminderJob.class)
						.withIdentity(meeting.getId() + "-reminder-" + reminder)
						.build();
				Date reminderRunTime = new Date(meeting.getDueAt().getTime() - (reminder * 60000));
				Trigger reminderTrigger = newTrigger()
						.withIdentity(meeting.getId() + "-remindertrigger-" + reminder)
						.startAt(reminderRunTime)
						.build();
				scheduler.scheduleJob(reminderJob, reminderTrigger);
			}
		} catch (SchedulerException e) {
			e.printStackTrace();
		}
	}

	public void updateMeetingSchedule(Meeting meeting) {
		SystemsConfig.MeetingConfig meetingConfig = Bot.config.getSystems().getMeetingConfig();
		try {
			//Update Start Trigger
			Date runTime = new Date(meeting.getDueAt().getTime());
			Trigger oldStartTrigger = scheduler.getTrigger(TriggerKey.triggerKey(meeting.getId() + "-starttrigger"));
			TriggerBuilder startTriggerBuilder = oldStartTrigger.getTriggerBuilder();
			Trigger startTrigger = startTriggerBuilder
					.withIdentity(meeting.getId() + "-starttrigger")
					.startAt(runTime)
					.build();
			scheduler.rescheduleJob(oldStartTrigger.getKey(), startTrigger);

			//Update Triggers for Meeting Reminders
			for (Integer reminder : meetingConfig.getMeetingReminders()) {
				if (new Date().after(new Date(meeting.getDueAt().getTime() - (reminder * 60000)))) return;
				Date reminderRunTime = new Date(meeting.getDueAt().getTime() - (reminder * 60000));

				Trigger oldReminderTrigger = scheduler.getTrigger(TriggerKey.triggerKey(meeting.getId() + "-remindertrigger-" + reminder));
				if(oldReminderTrigger != null){
					TriggerBuilder reminderTriggerBuilder = oldReminderTrigger.getTriggerBuilder();
					Trigger reminderTrigger = reminderTriggerBuilder
							.withIdentity(meeting.getId() + "-remindertrigger-" + reminder)
							.startAt(reminderRunTime)
							.build();
					scheduler.rescheduleJob(oldReminderTrigger.getKey(), reminderTrigger);
				}
			}
		} catch (SchedulerException e) {
			e.printStackTrace();
		}
	}

	public void cancelMeetingSchedule(Meeting meeting) {
		SystemsConfig.MeetingConfig meetingConfig = Bot.config.getSystems().getMeetingConfig();
		try {
			scheduler.deleteJob(JobKey.jobKey(meeting.getId() + "-start"));
			for (int reminder : meetingConfig.getMeetingReminders()) {
				if (scheduler.getTrigger(TriggerKey.triggerKey(meeting.getId() + "-remindertrigger-" + reminder)) != null){
					scheduler.deleteJob(JobKey.jobKey(meeting.getId() + "-reminder-" + reminder));
				}
			}
		} catch (SchedulerException e) {
			e.printStackTrace();
		}
	}
}
