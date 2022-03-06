package de.lightbolt.meeting.systems.meeting.jobs;

import de.lightbolt.meeting.Bot;
import de.lightbolt.meeting.data.h2db.DbHelper;
import de.lightbolt.meeting.systems.meeting.MeetingManager;
import de.lightbolt.meeting.systems.meeting.MeetingStatus;
import de.lightbolt.meeting.systems.meeting.dao.MeetingRepository;
import de.lightbolt.meeting.systems.meeting.model.Meeting;
import lombok.extern.slf4j.Slf4j;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.util.Optional;

@Slf4j
public class MeetingStartJob implements Job {
	@Override
	public void execute(JobExecutionContext context) {
		String[] jobDetail = context.getJobDetail().getKey().getName().split("-");
		DbHelper.doDaoAction(MeetingRepository::new, dao -> {
			Optional<Meeting> meetingOptional = dao.getById(Integer.parseInt(jobDetail[0]));
			if (meetingOptional.isEmpty()) {
				log.warn("Meeting doesn't exist, cannot execute start job.");
				return;
			}
			Meeting meeting = meetingOptional.get();
			if (meeting.getStatus() != MeetingStatus.ONGOING) {
				var manager = new MeetingManager(Bot.jda, meeting);
				manager.startMeeting();
			}
		});

	}
}
