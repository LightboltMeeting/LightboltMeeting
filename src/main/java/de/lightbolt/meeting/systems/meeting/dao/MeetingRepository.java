package de.lightbolt.meeting.systems.meeting.dao;

import de.lightbolt.meeting.systems.meeting.model.Meeting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * Dao that represents the MEETINGS SQL Table.
 */
@Slf4j
@RequiredArgsConstructor
public class MeetingRepository {
	private final Connection con;

	public Meeting insert(Meeting meeting) throws SQLException {
		var s = con.prepareStatement("INSERT INTO meetings (guild_id, created_by, participants, created_at, due_at, title, description, language, log_channel_id, voice_channel_id) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS
		);
		s.setLong(1, meeting.getGuildId());
		s.setLong(2, meeting.getCreatedBy());
		s.setArray(3, meeting.getParticipants());
		s.setTimestamp(4, meeting.getCreatedAt());
		s.setTimestamp(5, meeting.getDueAt());
		s.setString(6, meeting.getTitle());
		s.setString(7, meeting.getDescription());
		s.setString(8, meeting.getLanguage());
		s.setLong(9, meeting.getLogChannelId());
		s.setLong(10, meeting.getVoiceChannelId());
		int rows = s.executeUpdate();
		if (rows == 0) throw new SQLException("Meeting was not inserted.");
		ResultSet rs = s.getGeneratedKeys();
		if (rs.next()) {
			meeting.setId(rs.getInt(1));
		}
		s.close();
		log.info("Inserted new Meeting: {}", meeting);
		return meeting;
	}

	public boolean delete(int id) throws SQLException {
		try (var s = con.prepareStatement("DELETE FROM meetings WHERE id = ?", Statement.RETURN_GENERATED_KEYS)) {
			s.setInt(1, id);
			int rows = s.executeUpdate();
			return rows != 0;
		}
	}

	private Optional<Meeting> findById(int id) throws SQLException {
		Meeting meeting = null;
		try (var s = con.prepareStatement("SELECT * FROM meetings WHERE id = ?", Statement.RETURN_GENERATED_KEYS)) {
			s.setInt(1, id);
			var rs = s.executeQuery();
			if (rs.next()) {
				meeting = this.read(rs);
			}
			rs.close();
		}
		return Optional.ofNullable(meeting);
	}

	private List<Meeting> getByUserId(long userId) throws SQLException {
		List<Meeting> meetings = new ArrayList<>();
		try (var s = con.prepareStatement("SELECT * FROM meetings WHERE created_by = ?", Statement.RETURN_GENERATED_KEYS)) {
			s.setLong(1, userId);
			var rs = s.executeQuery();
			while (rs.next()) {
				meetings.add(this.read(rs));
			}
			rs.close();
			return meetings;
		}
	}

	public Meeting updateParticipants(Meeting old, Array participants) throws SQLException {
		var s = con.prepareStatement("UPDATE meetings SET participants = ? WHERE id = ?", Statement.RETURN_GENERATED_KEYS);
		s.setArray(1, participants);
		s.setInt(2, old.getId());
		int rows = s.executeUpdate();
		if (rows == 0) throw new SQLException("Meeting Participants was not updated. Meeting: " + old);
		old.setParticipants(participants);
		return old;
	}

	private Meeting read(ResultSet rs) throws SQLException {
		Meeting meeting = new Meeting();
		meeting.setId(rs.getInt("id"));
		meeting.setGuildId(rs.getLong("guild_id"));
		meeting.setCreatedBy(rs.getLong("created_by"));
		meeting.setParticipants(rs.getArray("participants"));
		meeting.setCreatedAt(rs.getTimestamp("created_at"));
		meeting.setDueAt(rs.getTimestamp("due_at"));
		meeting.setTitle(rs.getString("title"));
		meeting.setDescription(rs.getString("description"));
		meeting.setLanguage(rs.getString("language"));
		meeting.setLogChannelId(rs.getLong("log_channel_id"));
		meeting.setVoiceChannelId(rs.getLong("voice_channel_id"));
		meeting.setActive(rs.getBoolean("active"));
		return meeting;
	}
}
