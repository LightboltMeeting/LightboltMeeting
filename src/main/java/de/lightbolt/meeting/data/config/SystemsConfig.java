package de.lightbolt.meeting.data.config;

import de.lightbolt.meeting.utils.Resolvable;
import lombok.Data;
import lombok.EqualsAndHashCode;

import javax.annotation.Nullable;
import java.awt.*;
import java.util.List;

/**
 * Contains configuration settings for various systems which the bot uses, such
 * as databases or dependencies that have runtime properties.
 */
@EqualsAndHashCode(callSuper = true)
@Data
public class SystemsConfig extends Resolvable {
	/**
	 * The token used to create the JDA Discord bot instance.
	 */
	private String jdaBotToken = "";

	/**
	 * The number of threads to allocate to the bot's general purpose async
	 * thread pool.
	 */
	private int asyncPoolSize = 4;

	/**
	 * Configuration for the Hikari connection pool that's used for the bot's
	 * SQL data source.
	 */
	private HikariConfig hikariConfig = new HikariConfig();

	private SlashCommandConfig slashCommandConfig = new SlashCommandConfig();

	private MeetingConfig meetingConfig = new MeetingConfig();

	/**
	 * Configuration settings for the Hikari connection pool.
	 */
	@Data
	public static class HikariConfig {
		private String jdbcUrl = "jdbc:h2:tcp://localhost:9125/./meeting_bot";
		private int maximumPoolSize = 5;
	}

	@Data
	public static class SlashCommandConfig {
		private String defaultColorHex = "#2F3136";
		private String warningColorHex = "#EBA434";
		private String errorColorHex = "#EB3434";
		private String infoColorHex = "#34A2EB";
		private String successColorHex = "#49DE62";

		public Color getDefaultColor() {
			return Color.decode(this.defaultColorHex);
		}

		public Color getWarningColor() {
			return Color.decode(this.warningColorHex);
		}

		public Color getErrorColor() {
			return Color.decode(this.errorColorHex);
		}

		public Color getInfoColor() {
			return Color.decode(this.infoColorHex);
		}

		public Color getSuccessColor() {
			return Color.decode(this.successColorHex);
		}
	}

	@Data
	public static class MeetingConfig {
		private String meetingCategoryTemplate = "%s";
		private String meetingLogTemplate = "meeting-%s-log";
		private String meetingVoiceTemplate = "%s %s";
		private String meetingPlannedEmoji = "\uD83D\uDCC5";
		private String meetingStartingSoonEmoji = "\uD83D\uDFE1";
		private String meetingOngoingEmoji = "\uD83D\uDFE2";

		private int maxMeetingsPerUser = 2;
		private List<Integer> meetingReminders = List.of(10, 60, 360, 1440);
	}
}
