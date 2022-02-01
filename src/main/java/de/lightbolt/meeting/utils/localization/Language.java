package de.lightbolt.meeting.utils.localization;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Enum class, that represents all available and supported languages.
 */
@Getter
public enum Language {
	DE("localization/de-DE.json"),
	EN("localization/en-US.json");

	private final String path;

	Language(@NotNull String path) {
		this.path = path;
	}

	public static Language fromLocale(Locale locale) {
		return switch (locale.getLanguage()) {
			case "de" -> Language.DE;
			case "en" -> Language.EN;
			default -> Language.DE;
		};
	}
}