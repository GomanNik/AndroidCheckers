package ru.goman.checkers;

import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

/**
 * Базовая Activity:
 * - Следит за сменой языка и пересоздаёт экран при изменении.
 * - Централизованно читает настройку звука и даёт удобный API для клика с озвучкой.
 */
public abstract class BaseActivity extends AppCompatActivity {

    private static final String PREFS_SETTINGS = "checkers_settings";
    private static final String KEY_LANGUAGE   = "language";
    private static final String KEY_SOUND      = "sound_enabled";

    private static final String  DEFAULT_LANG        = "ru";
    private static final boolean DEFAULT_SOUND_STATE = true;

    /** Язык, с которым эта Activity была создана. */
    private String currentLang;

    /** Включен ли звук (клики) согласно настройкам. */
    private boolean soundEnabled;

    @Override
    protected void attachBaseContext(@NonNull Context newBase) {
        // Подменяем локаль до вызова super, чтобы все ресурсы были уже с нужным языком
        Context wrapped = LocaleManager.wrap(newBase);
        super.attachBaseContext(wrapped);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentLang = readLanguageFromPrefs(this);
        soundEnabled = readSoundEnabledFromPrefs(this);
    }

    @Override
    protected void onResume() {
        super.onResume();

        // 1. Проверяем изменение языка
        String langFromPrefs = readLanguageFromPrefs(this);
        if (!langFromPrefs.equals(currentLang)) {
            currentLang = langFromPrefs;
            recreate();
            return;
        }

        // 2. Обновляем состояние звука (если юзер выключил/включил в настройках)
        soundEnabled = readSoundEnabledFromPrefs(this);
    }

    // ------------------------------------------------------------------------
    // Чтение настроек
    // ------------------------------------------------------------------------

    private static String readLanguageFromPrefs(@NonNull Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE);
        return prefs.getString(KEY_LANGUAGE, DEFAULT_LANG);
    }

    private static boolean readSoundEnabledFromPrefs(@NonNull Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS_SETTINGS, Context.MODE_PRIVATE);
        return prefs.getBoolean(KEY_SOUND, DEFAULT_SOUND_STATE);
    }

    // ------------------------------------------------------------------------
    // Публичные/защищённые хелперы для звука
    // ------------------------------------------------------------------------

    /**
     * Текущее состояние звука из настроек.
     */
    protected boolean isSoundEnabled() {
        return soundEnabled;
    }

    /**
     * Проиграть звук клика, если звук включён в настройках.
     */
    protected void playClickSound() {
        if (!soundEnabled) {
            return;
        }
        SoundManager.getInstance(this).playClick();
    }

    /**
     * Обёртка для OnClickListener с автоматическим звуком клика.
     * Пример:
     * <pre>
     *     backButton.setOnClickListener(
     *         withClickSound(v -> onBackPressed())
     *     );
     * </pre>
     */
    @NonNull
    protected View.OnClickListener withClickSound(@NonNull View.OnClickListener delegate) {
        return v -> {
            playClickSound();
            delegate.onClick(v);
        };
    }
}
