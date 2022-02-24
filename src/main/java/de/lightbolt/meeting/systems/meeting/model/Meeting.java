package de.lightbolt.meeting.systems.meeting.model;

import de.lightbolt.meeting.systems.meeting.MeetingStatus;
import de.lightbolt.meeting.utils.localization.Language;
import de.lightbolt.meeting.utils.localization.LocaleConfig;
import de.lightbolt.meeting.utils.localization.LocalizationUtils;
import lombok.Data;

import java.sql.Timestamp;
import java.time.format.DateTimeFormatter;

/**
 * Data class representing a single Meeting.
 */
@Data
public class Meeting {
	private int id;
	private long guildId;
	private long createdBy;
	private long[] participants;
	private long[] admins;
	private Timestamp createdAt;
	private Timestamp dueAt;
	private String title;
	private String description;
	private String language;
	private long categoryId;
	private long logChannelId;
	private long voiceChannelId;
	private String statusRaw;

	public LocaleConfig getLocaleConfig() {
		return LocalizationUtils.getLocale(Language.valueOf(this.language));
	}

	public String getDueAtFormatted() { return this.dueAt.toLocalDateTime().format(DateTimeFormatter.ofPattern("dd.MM.yyyy HH:mm")); }

	public MeetingStatus getStatus() { return MeetingStatus.valueOf(this.statusRaw); }

	public void setStatus(MeetingStatus status) { this.statusRaw = status.name(); }
}
