package de.lightbolt.meeting.utils.localization;

import com.google.gson.Gson;
import lombok.extern.slf4j.Slf4j;

import java.io.File;
import java.nio.file.Files;

/**
 * Utility class regarding the bot's localization.
 */
@Slf4j
public class LocalizationUtils {

	/**
	 * Gets the {@link LocaleConfig} based on the given {@link Language}.
	 *
	 * @param language The config's language, represented as the {@link Language} enum.
	 * @return The corresponding {@link LocaleConfig} object.
	 */
	public static LocaleConfig getLocale(Language language) {
		LocaleConfig localeConfig = null;
		try {
			var languageFile = new File(language.getPath());
			var reader = Files.newBufferedReader(languageFile.toPath());
			localeConfig = new Gson().fromJson(reader, LocaleConfig.class);
		} catch (Exception exception) {
			log.error("Could not load file: " + language.getPath(), exception);
		}
		return localeConfig;
	}

	public static LocaleConfig getLocale(String language) {
		if (Language.isValidLanguage(language)) {
			return getLocale(Language.valueOf(language));
		} else {
			throw new IllegalArgumentException("Invalid Language!");
		}
	}
}