package ru.goman.checkers;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class LevelSelectActivity extends AppCompatActivity {

    public static final String EXTRA_LEVEL_NAME_RES_ID =
            "ru.goman.checkers.EXTRA_LEVEL_NAME_RES_ID";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_level_select);

        Button btnEasy = findViewById(R.id.btn_level_easy);
        Button btnMedium = findViewById(R.id.btn_level_medium);
        Button btnHard = findViewById(R.id.btn_level_hard);
        Button btnExpert = findViewById(R.id.btn_level_expert);
        Button btnGrandmaster = findViewById(R.id.btn_level_grandmaster);

        btnEasy.setOnClickListener(v ->
                startGameWithLevel(R.string.level_easy)
        );
        btnMedium.setOnClickListener(v ->
                startGameWithLevel(R.string.level_medium)
        );
        btnHard.setOnClickListener(v ->
                startGameWithLevel(R.string.level_hard)
        );
        btnExpert.setOnClickListener(v ->
                startGameWithLevel(R.string.level_expert)
        );
        btnGrandmaster.setOnClickListener(v ->
                startGameWithLevel(R.string.level_grandmaster)
        );
    }

    private void startGameWithLevel(int levelNameResId) {
        Intent intent = new Intent(LevelSelectActivity.this, GameActivity.class);
        intent.putExtra(EXTRA_LEVEL_NAME_RES_ID, levelNameResId);
        startActivity(intent);
    }
}
