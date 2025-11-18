package ru.goman.checkers;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.annotation.NonNull;

import ru.goman.checkers.ui.GameActivity;

public class LevelSelectActivity extends BaseActivity {

    public static final String EXTRA_LEVEL_NAME_RES_ID =
            "ru.goman.checkers.EXTRA_LEVEL_NAME_RES_ID";

    /**
     * Флаг: играем против ИИ или 2 игрока.
     * Берём из Intent и сохраняем, чтобы не потерять при пересоздании активности
     * (например, при смене языка / конфигурации).
     */
    private boolean vsAi;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level_select);

        // Восстанавливаем vsAi:
        //  - при первом запуске — из Intent ("vs_ai" от MainActivity)
        //  - при пересоздании (смена языка и т.п.) — из savedInstanceState
        if (savedInstanceState != null) {
            vsAi = savedInstanceState.getBoolean(
                    "vs_ai",
                    getIntent().getBooleanExtra("vs_ai", false)
            );
        } else {
            vsAi = getIntent().getBooleanExtra("vs_ai", false);
        }

        Button btnEasy        = findViewById(R.id.btn_level_easy);
        Button btnMedium      = findViewById(R.id.btn_level_medium);
        Button btnHard        = findViewById(R.id.btn_level_hard);
        Button btnExpert      = findViewById(R.id.btn_level_expert);
        Button btnGrandmaster = findViewById(R.id.btn_level_grandmaster);
        ImageButton btnHome   = findViewById(R.id.btn_level_home);

        // Отключаем системный звук, чтобы не было дубля с кастомным
        btnEasy.setSoundEffectsEnabled(false);
        btnMedium.setSoundEffectsEnabled(false);
        btnHard.setSoundEffectsEnabled(false);
        btnExpert.setSoundEffectsEnabled(false);
        btnGrandmaster.setSoundEffectsEnabled(false);
        btnHome.setSoundEffectsEnabled(false);

        btnEasy.setOnClickListener(withClickSound(v ->
                startGameWithLevel(R.string.level_easy, 0)));

        btnMedium.setOnClickListener(withClickSound(v ->
                startGameWithLevel(R.string.level_medium, 1)));

        btnHard.setOnClickListener(withClickSound(v ->
                startGameWithLevel(R.string.level_hard, 2)));

        btnExpert.setOnClickListener(withClickSound(v ->
                startGameWithLevel(R.string.level_expert, 3)));

        btnGrandmaster.setOnClickListener(withClickSound(v ->
                startGameWithLevel(R.string.level_grandmaster, 4)));

        btnHome.setOnClickListener(withClickSound(v -> {
            Intent i = new Intent(LevelSelectActivity.this, MainActivity.class);
            i.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
            finish();
        }));
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        // сохраняем vsAi, чтобы после смены языка / поворота экрана он не сбросился
        outState.putBoolean("vs_ai", vsAi);
    }

    private void startGameWithLevel(int levelNameResId, int difficultyLevel) {
        Intent intent = new Intent(LevelSelectActivity.this, GameActivity.class);

        // Передаём ИМЕННО ID строки уровня, а не сам текст — GameActivity
        // потом вызовет getString(levelNameResId) уже в текущей локали.
        intent.putExtra(EXTRA_LEVEL_NAME_RES_ID, levelNameResId);

        // Режим игры (vs AI / 2 игрока)
        intent.putExtra(GameActivity.EXTRA_VS_AI, vsAi);

        // передаём сложность ИИ
        intent.putExtra(GameActivity.EXTRA_DIFFICULTY, difficultyLevel);

        startActivity(intent);
    }
}
