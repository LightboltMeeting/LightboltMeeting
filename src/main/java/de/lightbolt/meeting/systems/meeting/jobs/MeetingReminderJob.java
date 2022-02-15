package de.lightbolt.meeting.systems.meeting.jobs;

import de.lightbolt.meeting.Bot;
import de.lightbolt.meeting.data.h2db.DbHelper;
import de.lightbolt.meeting.systems.meeting.MeetingManager;
import de.lightbolt.meeting.systems.meeting.dao.MeetingRepository;
import de.lightbolt.meeting.systems.meeting.model.Meeting;
import de.lightbolt.meeting.utils.localization.LocaleConfig;
import lombok.extern.slf4j.Slf4j;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.util.Arrays;
import java.util.Optional;
import java.util.stream.Collectors;

@Slf4j
public class MeetingReminderJob implements Job {
	@Override
	public void execute(JobExecutionContext context) {
		String[] jobDetail = context.getJobDetail().getKey().getName().split("-");
		DbHelper.doDaoAction(MeetingRepository::new, dao -> {
			Optional<Meeting> meetingOptional = new MeetingRepository(Bot.dataSource.getConnection()).findById(Integer.parseInt(jobDetail[0]));
			if (meetingOptional.isEmpty()) {
				log.warn("Meeting doesn't exist, cannot execute reminder job.");
				return;
			}
			Meeting meeting = meetingOptional.get();
			MeetingManager manager = new MeetingManager(Bot.jda, meeting);
			manager.getLogChannel()
					.sendMessageFormat(Arrays.stream(meeting.getParticipants()).mapToObj(l -> String.format("<@%s>", l)).collect(Collectors.joining(", ")))
					.setEmbeds(buildReminderEmbed(meeting.getLocaleConfig().getMeeting().getLog(), jobDetail[2]))
					.queue();
		});
	}

	private MessageEmbed buildReminderEmbed(LocaleConfig.MeetingConfig.MeetingLogConfig logLocale, String reminder) {
		String reminderTimeUnit;
		if (Integer.parseInt(reminder) > 60) {
			reminderTimeUnit = logLocale.getLOG_TIMEUNIT_HOURS();
			reminder = String.valueOf((Integer.parseInt(reminder) / 60));
		} else {
			reminderTimeUnit = logLocale.getLOG_TIMEUNIT_MINUTES();
		}
		return new EmbedBuilder()
				.setTitle(logLocale.getLOG_REMINDER_TITLE())
				.setDescription(String.format(logLocale.getLOG_REMINDER_DESCRIPTION(), reminder, reminderTimeUnit))
				.build();
	}
}
