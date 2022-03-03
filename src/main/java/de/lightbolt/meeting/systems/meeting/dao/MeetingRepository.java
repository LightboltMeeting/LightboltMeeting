package de.lightbolt.meeting.systems.meeting.dao;

import de.lightbolt.meeting.systems.meeting.MeetingStatus;
import de.lightbolt.meeting.systems.meeting.model.Meeting;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.sql.*;
import java.util.ArrayList;
import java.util.Arrays;
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
		var s = con.prepareStatement("INSERT INTO meetings (guild_id, created_by, participants, admins, created_at, due_at, timezone, title, description, language, category_id, log_channel_id, voice_channel_id, status) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)",
				Statement.RETURN_GENERATED_KEYS
		);
		s.setLong(1, meeting.getGuildId());
		s.setLong(2, meeting.getCreatedBy());
		s.setArray(3, con.createArrayOf("BIGINT", Arrays.stream(meeting.getParticipants()).mapToObj(o -> (Object) o).toArray()));
		s.setArray(4, con.createArrayOf("BIGINT", Arrays.stream(meeting.getAdmins()).mapToObj(o -> (Object) o).toArray()));
		s.setTimestamp(5, meeting.getCreatedAt());
		s.setTimestamp(6, meeting.getDueAt());
		s.setString(7, meeting.getTimeZoneRaw());
		s.setString(8, meeting.getTitle());
		s.setString(9, meeting.getDescription());
		s.setString(10, meeting.getLanguage());
		s.setLong(11, meeting.getCategoryId());
		s.setLong(12, meeting.getLogChannelId());
		s.setLong(13, meeting.getVoiceChannelId());
		s.setString(14, "SCHEDULED");
		int rows = s.executeUpdate();
		if (rows == 0) throw new SQLException("Meeting was not inserted.");
		ResultSet rs = s.getGeneratedKeys();
		if (rs.next()) {
			meeting.setId(rs.getInt(1));
		}
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

	public Optional<Meeting> getById(int id) throws SQLException {
		Meeting meeting = null;
		try (var s = con.prepareStatement("SELECT * FROM meetings WHERE id = ? AND status != 'INACTIVE'", Statement.RETURN_GENERATED_KEYS)) {
			s.setInt(1, id);
			var rs = s.executeQuery();
			if (rs.next()) {
				meeting = this.read(rs);
			}
			rs.close();
		}
		return Optional.ofNullable(meeting);
	}

	public List<Meeting> getByUserId(long userId) throws SQLException {
		List<Meeting> meetings = new ArrayList<>();
		try (var s = con.prepareStatement("SELECT * FROM meetings WHERE created_by = ? AND status != 'INACTIVE'", Statement.RETURN_GENERATED_KEYS)) {
			s.setLong(1, userId);
			var rs = s.executeQuery();
			while (rs.next()) {
				meetings.add(this.read(rs));
			}
			rs.close();
			return meetings;
		}
	}

	public List<Meeting> getActive() throws SQLException {
		List<Meeting> meetings = new ArrayList<>();
		try (PreparedStatement s = con.prepareStatement("SELECT * FROM meetings WHERE status != 'INACTIVE'", Statement.RETURN_GENERATED_KEYS)) {
			ResultSet rs = s.executeQuery();
			while (rs.next()) {
				meetings.add(this.read(rs));
			}
			rs.close();
			return meetings;
		}
	}

	public void update(int id, Meeting update) throws SQLException {
		var s = con.prepareStatement("UPDATE meetings " +
						"SET guild_id = ?, created_by = ?, participants = ?, admins = ?, created_at = ?, due_at = ?, timezone = ?, title = ?, description = ?, language = ?, category_id = ?, log_channel_id = ?, voice_channel_id = ?, status = ?" +
						"WHERE id = ?", Statement.RETURN_GENERATED_KEYS);
		s.setLong(1, update.getGuildId());
		s.setLong(2, update.getCreatedBy());
		s.setArray(3, con.createArrayOf("BIGINT", Arrays.stream(update.getParticipants()).mapToObj(o -> (Object) o).toArray()));
		s.setArray(4, con.createArrayOf("BIGINT", Arrays.stream(update.getAdmins()).mapToObj(o -> (Object) o).toArray()));
		s.setTimestamp(5, update.getCreatedAt());
		s.setTimestamp(6, update.getDueAt());
		s.setString(7, update.getTimeZoneRaw());
		s.setString(8, update.getTitle());
		s.setString(9, update.getDescription());
		s.setString(10, update.getLanguage());
		s.setLong(11, update.getCategoryId());
		s.setLong(12, update.getLogChannelId());
		s.setLong(13, update.getVoiceChannelId());
		s.setString(14, update.getStatusRaw());
		s.setInt(15, id);
		int rows = s.executeUpdate();
		if (rows == 0) throw new SQLException("Could not update Meeting participants. Meeting: " + update);
	}

	private Meeting read(ResultSet rs) throws SQLException {
		Meeting meeting = new Meeting();
		meeting.setId(rs.getInt("id"));
		meeting.setGuildId(rs.getLong("guild_id"));
		meeting.setCreatedBy(rs.getLong("created_by"));
		meeting.setParticipants(this.convertArrayToLongArray(rs.getArray("participants")));
		meeting.setAdmins(this.convertArrayToLongArray(rs.getArray("admins")));
		meeting.setCreatedAt(rs.getTimestamp("created_at"));
		meeting.setDueAt(rs.getTimestamp("due_at"));
		meeting.setTimeZoneRaw(rs.getString("timezone"));
		meeting.setTitle(rs.getString("title"));
		meeting.setDescription(rs.getString("description"));
		meeting.setLanguage(rs.getString("language"));
		meeting.setCategoryId(rs.getLong("category_id"));
		meeting.setLogChannelId(rs.getLong("log_channel_id"));
		meeting.setVoiceChannelId(rs.getLong("voice_channel_id"));
		meeting.setStatusRaw(rs.getString("status"));
		return meeting;
	}

	private long[] convertArrayToLongArray(Array array) throws SQLException {
		Object[] tmp = (Object[]) array.getArray();
		long[] longArray = new long[tmp.length];
		for (int i = 0; i < tmp.length; i++) {
			longArray[i] = (Long) tmp[i];
		}
		return longArray;
	}
}
