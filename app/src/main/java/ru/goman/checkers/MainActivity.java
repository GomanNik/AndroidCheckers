package ru.goman.checkers;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Button btnExit = findViewById(R.id.btn_exit);
        Button btnShare = findViewById(R.id.btn_share);
        Button btnPlay = findViewById(R.id.btn_play);
        Button btnRules = findViewById(R.id.btn_rules);
        Button btnTwoPlayers = findViewById(R.id.btn_two_players);
        Button btnStats = findViewById(R.id.btn_stats);
        Button btnSettings = findViewById(R.id.btn_settings);

        btnExit.setOnClickListener(v -> finish());

        btnShare.setOnClickListener(v -> {
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("text/plain");
            String text = "Игра «Шашки» (Android). Пока ссылки на Google Play нет :)";
            sendIntent.putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(sendIntent, "Поделиться игрой"));
        });

        btnPlay.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, LevelSelectActivity.class))
        );

        btnRules.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, RulesActivity.class))
        );

        btnTwoPlayers.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, TwoPlayersActivity.class))
        );

        btnStats.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, StatsActivity.class))
        );

        btnSettings.setOnClickListener(v ->
                startActivity(new Intent(MainActivity.this, SettingsActivity.class))
        );
    }
}
