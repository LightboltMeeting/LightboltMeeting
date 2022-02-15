package de.lightbolt.meeting.utils.localization;

import lombok.Data;
import lombok.Getter;

/**
 * Simple Data class, that represents the bot's localization config.
 */
@Getter
public class LocaleConfig {
	private final MeetingConfig meeting = new MeetingConfig();
	private final CommandConfig command = new CommandConfig();

	@Data
	public static class CommandConfig {
		private String MISSING_ARGUMENTS;
	}

	@Data
	public static class MeetingConfig {
		private String MEETING_EMBED_FOOTER;
		private String MEETING_NO_PERMISSION;

		private MeetingCreationConfig creation = new MeetingCreationConfig();
		private MeetingEditConfig edit = new MeetingEditConfig();
		private MeetingCommandConfig command = new MeetingCommandConfig();
		private MeetingLogConfig log = new MeetingLogConfig();

		@Data
		public static class MeetingCreationConfig {
			private String CREATION_TOO_MANY_MEETINGS_DESCRIPTION;
			private String CREATION_NOT_PERMITTED_DESCRIPTION;

			private String CREATION_DATE_PLACEHOLDER;
			private String CREATION_LANGUAGE_PLACEHOLDER;

			private String CREATION_MODAL_HEADER;
			private String CREATION_NAME_LABEL;
			private String CREATION_DESCRIPTION_LABEL;
			private String CREATION_DATE_LABEL;
			private String CREATION_LANGUAGE_LABEL;

			private String CREATION_INVALID_DATE;
			private String CREATION_INVALID_LANGUAGE;

			private String CREATION_SUCCESS_TITLE;
			private String CREATION_SUCCESS_DESCRIPTION;
			private String CREATION_FAILED;
		}

		@Data
		public static class MeetingCommandConfig {
			private String MEETING_NOT_FOUND;
			private String MEETING_PARTICIPANT_ALREADY_ADDED;
			private String MEETING_PARTICIPANT_NOT_FOUND;
			private String MEETING_ADMIN_ALREADY_ADDED;
			private String MEETING_ADMIN_NOT_FOUND;
			private String MEETING_ADMIN_NOT_A_PARTICIPANT;

			private String LIST_REPLY_TEXT;
			private String LIST_PARTICIPANTS;

			private String CANCEL_MEETING_TITLE;
			private String CANCEL_MEETING_DESCRIPTION;

			private String PARTICIPANTS_ADD_SUCCESS_TITLE;
			private String PARTICIPANTS_ADD_SUCCESS_DESCRIPTION;
			private String PARTICIPANTS_REMOVE_SUCCESS_TITLE;
			private String PARTICIPANTS_REMOVE_SUCCESS_DESCRIPTION;

			private String ADMINS_ADD_SUCCESS_TITLE;
			private String ADMINS_ADD_SUCCESS_DESCRIPTION;
			private String ADMINS_REMOVE_SUCCESS_TITLE;
			private String ADMINS_REMOVE_SUCCESS_DESCRIPTION;
		}

		@Data
		public static class MeetingLogConfig {
			private String LOG_PARTICIPANT_ADDED;
			private String LOG_PARTICIPANT_REMOVED;

			private String LOG_ADMIN_ADDED;
			private String LOG_ADMIN_REMOVED;

			private String LOG_REMINDER_TITLE;
			private String LOG_REMINDER_DESCRIPTION;

			private String LOG_START_TITLE;
			private String LOG_START_DESCRIPTION;

			private String LOG_TIMEUNIT_MINUTES;
			private String LOG_TIMEUNIT_HOURS;

			private String LOG_MEETING_UPDATED;
		}

		@Data
		public static class MeetingEditConfig {
			private String EDIT_DATE_PLACEHOLDER;
			private String EDIT_LANGUAGE_PLACEHOLDER;

			private String EDIT_MODAL_HEADER;
			private String EDIT_NAME_LABEL;
			private String EDIT_DESCRIPTION_LABEL;
			private String EDIT_DATE_LABEL;
			private String EDIT_LANGUAGE_LABEL;

			private String EDIT_INVALID_DATE;
			private String EDIT_INVALID_LANGUAGE;

			private String EDIT_SUCCESS_TITLE;
			private String EDIT_SUCCESS_DESCRIPTION;
			private String EDIT_FAILED;
		}
	}
}
