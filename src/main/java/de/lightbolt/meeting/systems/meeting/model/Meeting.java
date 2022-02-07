package de.lightbolt.meeting.systems.meeting.model;

import lombok.Data;

import java.sql.Timestamp;

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
	private long logChannelId;
	private long voiceChannelId;
	private boolean active;
}
