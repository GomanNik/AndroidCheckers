package ru.goman.checkers;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class StatsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        Button btnHome = findViewById(R.id.btn_stats_home);
        Button btnShare = findViewById(R.id.btn_stats_share);

        btnHome.setOnClickListener(v -> finish());

        btnShare.setOnClickListener(v -> {
            // Пока шарим заглушку
            String text = "Статистика по уровням:\n" +
                    "Лёгкий: сыграно 0, побед 0, поражений 0\n" +
                    "Средний: сыграно 0, побед 0, поражений 0\n" +
                    "Сложный: сыграно 0, побед 0, поражений 0\n" +
                    "Эксперт: сыграно 0, побед 0, поражений 0\n" +
                    "Гроссмейстер: сыграно 0, побед 0, поражений 0";

            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("text/plain");
            sendIntent.putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(sendIntent, "Поделиться статистикой"));
        });
    }
}
