package de.lightbolt.meeting.utils.localization;

import lombok.Data;
import lombok.Getter;

/**
 * Simple Data class, that represents the bot's localization config.
 */
@Getter
public class LocaleConfig {
	private final MeetingConfig meeting = new MeetingConfig();

	@Data
	public static class MeetingConfig {
		private String MEETING_EMBED_FOOTER;

		private MeetingCreationConfig creation = new MeetingCreationConfig();
		private MeetingCommandConfig command = new MeetingCommandConfig();

		@Data
		public static class MeetingCreationConfig {
			private String CREATION_START_TOO_MANY_MEETING_DESCRIPTION;
			private String CREATION_START_NOT_PERMITTED_DESCRIPTION;
			private String CREATION_START_RESPONSE_TITLE;
			private String CREATION_START_RESPONSE_DESCRIPTION;
			private String CREATION_START_OPEN_PRIVATE_FAILED;

			private String CREATION_DM_DEFAULT_EMBED_TITLE;
			private String CREATION_DM_DEFAULT_EMBED_FOOTER;
			private String CREATION_DM_TIMED_OUT_TITLE;
			private String CREATION_DM_TIMED_OUT_DESCRIPTION;
			private String CREATION_DM_NO_TRIES_LEFT_TITLE;
			private String CREATION_DM_NO_TRIES_LEFT_DESCRIPTION;

			private String CREATION_DM_STEP_1_DESCRIPTION;
			private String CREATION_DM_STEP_1_SELECTION_MENU_PLACEHOLDER;
			private String CREATION_DM_STEP_1_SELECTION_MENU_DESCRIPTION;
			private String CREATION_DM_STEP_1_SUCCESS_EPHEMERAL;

			private String CREATION_DM_STEP_2_DESCRIPTION;
			private String CREATION_DM_STEP_2_SELECTION_MENU_PLACEHOLDER;
			private String CREATION_DM_STEP_2_SELECTION_MENU_DESCRIPTION;
			private String CREATION_DM_STEP_2_SUCCESS_EPHEMERAL;

			private String CREATION_DM_STEP_3_DESCRIPTION;
			private String CREATION_DM_STEP_3_INVALID_DATE;

			private String CREATION_DM_STEP_4_DESCRIPTION;
			private String CREATION_DM_STEP_4_INVALID_TITLE;

			private String CREATION_DM_STEP_5_DESCRIPTION;
			private String CREATION_DM_STEP_5_INVALID_DESCRIPTION;

			private String CREATION_DM_STEP_6_DESCRIPTION;
			private String CREATION_DM_STEP_6_FOOTER;
			private String CREATION_DM_STEP_6_BUTTON_SAVE_MEETING;
			private String CREATION_DM_STEP_6_BUTTON_CANCEL_MEETING;
			private String CREATION_DM_STEP_6_PROCESS_CANCELED;
			private String CREATION_DM_STEP_6_MEETING_SAVED;
		}

		@Data
		public static class MeetingCommandConfig {
			private String MEETING_NOT_FOUND;
			private String MEETING_PARTICIPANT_ALREADY_ADDED;
			private String MEETING_PARTICIPANT_NOT_FOUND;

			private String LIST_REPLY_TEXT;
			private String LIST_PARTICIPANTS;

			private String CANCEL_MEETING_TITLE;
			private String CANCEL_MEETING_DESCRIPTION;

			private String PARTICIPANTS_ADD_SUCCESS_TITLE;
			private String PARTICIPANTS_ADD_SUCCESS_DESCRIPTION;

			private String PARTICIPANTS_REMOVE_SUCCESS_TITLE;
			private String PARTICIPANTS_REMOVE_SUCCESS_DESCRIPTION;
		}
	}
}
