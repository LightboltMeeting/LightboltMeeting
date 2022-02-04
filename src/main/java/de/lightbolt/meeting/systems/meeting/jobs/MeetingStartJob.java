package de.lightbolt.meeting.systems.meeting.jobs;

import de.lightbolt.meeting.Bot;
import de.lightbolt.meeting.systems.meeting.dao.MeetingRepository;
import de.lightbolt.meeting.systems.meeting.model.Meeting;
import de.lightbolt.meeting.utils.localization.Language;
import de.lightbolt.meeting.utils.localization.LocaleConfig;
import de.lightbolt.meeting.utils.localization.LocalizationUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import org.quartz.Job;
import org.quartz.JobExecutionContext;

import java.sql.SQLException;
import java.util.Locale;

public class MeetingStartJob implements Job {
	private LocaleConfig locale;
	private LocaleConfig.MeetingConfig.MeetingLogConfig logLocale;

	@Override
	public void execute(JobExecutionContext context) {
		try {
			String[] jobDetail = context.getJobDetail().getKey().getName().split("-");
			Meeting meeting = new MeetingRepository(Bot.dataSource.getConnection()).findById(Integer.parseInt(jobDetail[0])).get();
			locale = LocalizationUtils.getLocale(Language.fromLocale(Locale.forLanguageTag(meeting.getLanguage())));
			logLocale = locale.getMeeting().getLog();
			StringBuilder participants = new StringBuilder();
			TextChannel logChannel = Bot.jda.getTextChannelById(meeting.getLogChannelId());

			for (long participantId: meeting.getParticipants()) {
				participants.append(Bot.jda.getUserById(participantId).getAsMention());
			}

			logChannel.sendMessage(participants).queue();
			logChannel.sendMessageEmbeds(buildEmbed()).queue();

		} catch (SQLException e) {
			e.printStackTrace();
		}
	}

	private MessageEmbed buildEmbed() {

		return new EmbedBuilder()
				.setTitle(logLocale.getLOG_START_TITLE())
				.setDescription(logLocale.getLOG_START_DESCRIPTION())
				.build();
	}
}
