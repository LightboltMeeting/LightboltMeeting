package de.lightbolt.meeting.utils.localization;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

/**
 * Enum class, that represents all available and supported languages.
 */
@Getter
public enum Language {
	DE("de-DE.json"),
	EN("en-EN.json");

	private final String path;

	Language(@NotNull String path) {
		this.path = path;
	}
}