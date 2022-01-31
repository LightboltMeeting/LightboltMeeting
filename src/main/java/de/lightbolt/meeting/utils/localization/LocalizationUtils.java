package de.lightbolt.meeting.utils.localization;

import com.google.gson.Gson;

import java.nio.file.Files;
import java.nio.file.Paths;

public class LocalizationUtils {

    public static LocaleConfig getLocale(Language language) {
        LocaleConfig localeConfig = null;
        try {
            var languageFile = Paths.get(LocalizationUtils.class.getClassLoader().getResource(language.getPath()).toURI());
            var reader = Files.newBufferedReader(languageFile);
            localeConfig = new Gson().fromJson(reader, LocaleConfig.class);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
        return localeConfig;
    }
}