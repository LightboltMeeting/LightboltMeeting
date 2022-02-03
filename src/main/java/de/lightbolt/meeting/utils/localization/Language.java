package de.lightbolt.meeting.utils.localization;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Locale;

/**
 * Enum class, that represents all available and supported languages.
 */
@Getter
public enum Language {
	DE("localization/de-DE.json", "Deutsch"),
	EN("localization/en-US.json", "English");

	private final String path;
	private final String name;

	Language(@NotNull String path, @NotNull String name) {
		this.path = path;
		this.name = name;
	}

	public static Language fromLocale(Locale locale) {
		return switch (locale.getLanguage()) {
			case "de" -> Language.DE;
			case "en" -> Language.EN;
			default -> Language.DE;
		};
	}
}