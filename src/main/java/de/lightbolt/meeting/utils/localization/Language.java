package de.lightbolt.meeting.utils.localization;

import lombok.Getter;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;
import java.util.Locale;

/**
 * Enum class, that represents all available and supported languages.
 */
@Getter
public enum Language {
	DE("Deutsch", "localization/de-DE.json", "Europe/Berlin"),
	EN("English", "localization/en-US.json", "Europe/London");

	private final String path;
	private final String name;
	private final String timezone;

	Language(@NotNull String name, @NotNull String path, @NotNull String timezone) {
		this.path = path;
		this.name = name;
		this.timezone = timezone;
	}

	public static Language fromLocale(Locale locale) {
		return switch (locale.getLanguage()) {
			case "de" -> Language.DE;
			case "en" -> Language.EN;
			default -> Language.DE;
		};
	}

	public static boolean isValidLanguage(String s) {
		return Arrays.stream(Language.values()).anyMatch(l -> l.toString().equals(s));
	}
}