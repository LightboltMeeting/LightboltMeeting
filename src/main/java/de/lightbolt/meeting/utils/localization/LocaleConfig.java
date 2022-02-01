package de.lightbolt.meeting.utils.localization;

import lombok.Data;
import lombok.Getter;

/**
 * Simple Data class, that represents the bot's localization config.
 */
@Getter
public class LocaleConfig {
	private MeetingCreationConfig meetingCreation = new MeetingCreationConfig();

	@Data
	public static class MeetingCreationConfig {
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
}
