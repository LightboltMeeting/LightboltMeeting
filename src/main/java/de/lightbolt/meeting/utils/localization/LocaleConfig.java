package de.lightbolt.meeting.utils.localization;

import lombok.Data;
import lombok.Getter;

/**
 * Simple Data class, that represents the bot's localization config.
 */
@Getter
public class LocaleConfig {
	private MeetingConfig meeting = new MeetingConfig();

	@Data
	public static class MeetingConfig {
		private String CREATION_START_RESPONSE_TITLE;
		private String CREATION_START_RESPONSE_DESCRIPTION;
		private String CREATION_START_OPEN_PRIVATE_FAILED;

		private String CREATION_DM_DEFAULT_EMBED_TITLE;
		private String CREATION_DM_STEP_0_DESCRIPTION;
		private String CREATION_DM_STEP_1_DESCRIPTION;
	}
}
