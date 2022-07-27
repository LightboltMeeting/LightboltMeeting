package de.lightbolt.meeting.utils.localization;

import lombok.Getter;
import net.dv8tion.jda.api.interactions.DiscordLocale;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;

import java.util.Arrays;

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

	@Contract(pure = true)
	public static Language fromLocale(@NotNull DiscordLocale locale) {
		return switch (locale.getLocale()) {
			case "de" -> Language.DE;
			case "en" -> Language.EN;
			default -> Language.DE;
		};
	}

	public static boolean isValidLanguage(String s) {
		return Arrays.stream(Language.values()).anyMatch(l -> l.toString().equals(s));
	}
}