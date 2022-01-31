package de.lightbolt.meeting.utils.localization;

import com.google.gson.Gson;

import java.nio.file.Files;
import java.nio.file.Paths;

public class LocalizationUtils {

    private static LocaleConfig localeConfig;

    public static LocaleConfig getLocale(Language language) {
        switch (language) {
            case DE -> loadDE();
            case EN -> loadEN();
        }
        return localeConfig;
    }

    private static void loadDE() {
        try {
            var languageFile = Paths.get(LocalizationUtils.class.getClassLoader().getResource("de-DE.json").toURI());
            var reader = Files.newBufferedReader(languageFile);
            localeConfig = new Gson().fromJson(reader, LocaleConfig.class);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }

    private static void loadEN() {
        try {
            var languageFile = Paths.get(LocalizationUtils.class.getClassLoader().getResource("en-EN.json").toURI());
            var reader = Files.newBufferedReader(languageFile);
            localeConfig = new Gson().fromJson(reader, LocaleConfig.class);
        } catch (Exception exception) {
            exception.printStackTrace();
        }
    }
}