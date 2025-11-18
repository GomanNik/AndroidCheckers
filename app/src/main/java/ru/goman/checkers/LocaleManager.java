package ru.goman.checkers;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.content.res.Resources;

import java.util.Locale;

public final class LocaleManager {

    private static final String PREFS_SETTINGS = "checkers_settings";
    private static final String KEY_LANGUAGE   = "language";
    private static final String DEFAULT_LANG   = "ru";

    private LocaleManager() {
        // утилитный класс
    }

    /**
     * Оборачиваем контекст с локалью из SharedPreferences.
     * Вызывается из BaseActivity.attachBaseContext().
     */
    public static Context wrap(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE);

        String lang = prefs.getString(KEY_LANGUAGE, DEFAULT_LANG);
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);

        Resources res = context.getResources();
        Configuration config = new Configuration(res.getConfiguration());
        config.setLocale(locale);

        return context.createConfigurationContext(config);
    }
}
