package ru.goman.checkers;

import android.os.Bundle;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;

public class GameActivity extends AppCompatActivity {

    private int levelNameResId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        // Получаем уровень из интента
        levelNameResId = getIntent().getIntExtra(
                LevelSelectActivity.EXTRA_LEVEL_NAME_RES_ID,
                R.string.level_easy
        );
        String levelName = getString(levelNameResId);

        TextView tvScore = findViewById(R.id.tv_score_value);
        TextView tvLevel = findViewById(R.id.tv_game_level);
        TextView tvTurn = findViewById(R.id.tv_game_turn);

        // Пока счёт всегда 0 : 0
        tvScore.setText("0 : 0");
        tvLevel.setText(getString(R.string.game_level_format, levelName));
        tvTurn.setText(getString(R.string.game_turn_white));

        Button btnHome = findViewById(R.id.btn_game_home);
        Button btnRestart = findViewById(R.id.btn_game_restart);
        Button btnUndo = findViewById(R.id.btn_game_undo);
        Button btnSound = findViewById(R.id.btn_game_sound);

        btnHome.setOnClickListener(v -> finish());

        // Пока заглушки — логику игры будем добавлять позже
        btnRestart.setOnClickListener(v -> {
            // TODO: сбросить партию и начать заново
        });

        btnUndo.setOnClickListener(v -> {
            // TODO: отмена последнего хода
        });

        btnSound.setOnClickListener(v -> {
            // TODO: включить/выключить звук
        });
    }
}
