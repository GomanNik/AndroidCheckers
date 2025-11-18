package ru.goman.checkers;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.NonNull;

public class SettingsActivity extends BaseActivity {

    // Отдельные prefs под настройки (не статистика)
    private static final String PREFS_SETTINGS      = "checkers_settings";

    private static final String KEY_HUMAN_COLOR     = "human_color_vs_ai";   // "WHITE" / "BLACK"
    private static final String KEY_MUST_CAPTURE    = "must_capture";        // boolean
    private static final String KEY_MOVE_HINT       = "move_hint";           // boolean
    private static final String KEY_LANGUAGE        = "language";            // "ru" / "en"

    private static final String COLOR_WHITE         = "WHITE";
    private static final String COLOR_BLACK         = "BLACK";

    private static final String LANG_RU             = "ru";
    private static final String LANG_EN             = "en";

    private SharedPreferences prefs;

    // Кнопки, чтобы удобно обновлять состояние
    private Button btnPlayWhite;
    private Button btnPlayBlack;

    private Button btnMustCaptureYes;
    private Button btnMustCaptureNo;

    private Button btnHintYes;
    private Button btnHintNo;

    private Button btnLangRu;
    private Button btnLangEn;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        prefs = getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE);

        ImageButton btnHome  = findViewById(R.id.btn_settings_home);

        btnPlayWhite       = findViewById(R.id.btn_play_white);
        btnPlayBlack       = findViewById(R.id.btn_play_black);
        btnMustCaptureYes  = findViewById(R.id.btn_must_capture_yes);
        btnMustCaptureNo   = findViewById(R.id.btn_must_capture_no);
        btnHintYes         = findViewById(R.id.btn_hint_yes);
        btnHintNo          = findViewById(R.id.btn_hint_no);
        btnLangRu          = findViewById(R.id.btn_lang_ru);
        btnLangEn          = findViewById(R.id.btn_lang_en);

        // Отключаем системный звук, чтобы не было дубля с кастомным
        btnHome.setSoundEffectsEnabled(false);
        btnPlayWhite.setSoundEffectsEnabled(false);
        btnPlayBlack.setSoundEffectsEnabled(false);
        btnMustCaptureYes.setSoundEffectsEnabled(false);
        btnMustCaptureNo.setSoundEffectsEnabled(false);
        btnHintYes.setSoundEffectsEnabled(false);
        btnHintNo.setSoundEffectsEnabled(false);
        btnLangRu.setSoundEffectsEnabled(false);
        btnLangEn.setSoundEffectsEnabled(false);

        // Домой
        btnHome.setOnClickListener(withClickSound(v -> finish()));

        // --- Загрузка текущих значений из prefs и первичная инициализация UI ---

        String humanColor = prefs.getString(KEY_HUMAN_COLOR, COLOR_WHITE);
        applyPlayColorSelection(COLOR_WHITE.equals(humanColor));

        boolean mustCapture = prefs.getBoolean(KEY_MUST_CAPTURE, true);
        applyMustCaptureSelection(mustCapture);

        boolean moveHint = prefs.getBoolean(KEY_MOVE_HINT, true);
        applyMoveHintSelection(moveHint);

        String language = prefs.getString(KEY_LANGUAGE, LANG_RU);
        applyLanguageSelection(LANG_RU.equals(language));

        // --- Обработчики кликов ---

        // Играть за: Белые / Чёрные
        btnPlayWhite.setOnClickListener(withClickSound(v -> {
            saveHumanColor(COLOR_WHITE);
            applyPlayColorSelection(true);
        }));

        btnPlayBlack.setOnClickListener(withClickSound(v -> {
            saveHumanColor(COLOR_BLACK);
            applyPlayColorSelection(false);
        }));

        // Бить обязательно: Да / Нет
        btnMustCaptureYes.setOnClickListener(withClickSound(v -> {
            saveBoolean(KEY_MUST_CAPTURE, true);
            applyMustCaptureSelection(true);
        }));

        btnMustCaptureNo.setOnClickListener(withClickSound(v -> {
            saveBoolean(KEY_MUST_CAPTURE, false);
            applyMustCaptureSelection(false);
        }));

        // Подсказка хода: Да / Нет
        btnHintYes.setOnClickListener(withClickSound(v -> {
            saveBoolean(KEY_MOVE_HINT, true);
            applyMoveHintSelection(true);
        }));

        btnHintNo.setOnClickListener(withClickSound(v -> {
            saveBoolean(KEY_MOVE_HINT, false);
            applyMoveHintSelection(false);
        }));

        // Язык: Русский / Английский
        btnLangRu.setOnClickListener(withClickSound(v -> changeLanguage(LANG_RU)));

        btnLangEn.setOnClickListener(withClickSound(v -> changeLanguage(LANG_EN)));
    }

    // ------------------------------------------------------------------------
    // Смена языка
    // ------------------------------------------------------------------------

    private void changeLanguage(@NonNull String lang) {
        saveString(KEY_LANGUAGE, lang);

        // Обновляем визуальное выделение
        applyLanguageSelection(LANG_RU.equals(lang));

        // Пересоздаём только экран настроек.
        // Остальные активити обновятся благодаря логике в BaseActivity.onResume().
        recreate();
    }

    // ------------------------------------------------------------------------
    // Сохранение в SharedPreferences
    // ------------------------------------------------------------------------

    private void saveHumanColor(@NonNull String color) {
        saveString(KEY_HUMAN_COLOR, color);
    }

    private void saveString(@NonNull String key, @NonNull String value) {
        prefs.edit().putString(key, value).apply();
    }

    private void saveBoolean(@NonNull String key, boolean value) {
        prefs.edit().putBoolean(key, value).apply();
    }

    // ------------------------------------------------------------------------
    // Обновление UI для групп кнопок
    // ------------------------------------------------------------------------

    /** true = человек играет белыми, false = чёрными */
    private void applyPlayColorSelection(boolean humanIsWhite) {
        if (btnPlayWhite == null || btnPlayBlack == null) return;

        btnPlayWhite.setEnabled(true);
        btnPlayBlack.setEnabled(true);

        btnPlayWhite.setSelected(humanIsWhite);
        btnPlayBlack.setSelected(!humanIsWhite);
    }

    /** true = бить обязательно, false = можно не бить */
    private void applyMustCaptureSelection(boolean mustCapture) {
        if (btnMustCaptureYes == null || btnMustCaptureNo == null) return;

        btnMustCaptureYes.setEnabled(true);
        btnMustCaptureNo.setEnabled(true);

        btnMustCaptureYes.setSelected(mustCapture);
        btnMustCaptureNo.setSelected(!mustCapture);
    }

    /** true = подсказка ходов включена */
    private void applyMoveHintSelection(boolean moveHint) {
        if (btnHintYes == null || btnHintNo == null) return;

        btnHintYes.setEnabled(true);
        btnHintNo.setEnabled(true);

        btnHintYes.setSelected(moveHint);
        btnHintNo.setSelected(!moveHint);
    }

    /** true = русский, false = английский */
    private void applyLanguageSelection(boolean russian) {
        if (btnLangRu == null || btnLangEn == null) return;

        btnLangRu.setEnabled(true);
        btnLangEn.setEnabled(true);

        btnLangRu.setSelected(russian);
        btnLangEn.setSelected(!russian);
    }
}
