package ru.goman.checkers;

import android.content.Intent;
import android.os.Bundle;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class SettingsActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_settings);

        Button btnHome = findViewById(R.id.btn_settings_home);
        Button btnShare = findViewById(R.id.btn_settings_share);

        btnHome.setOnClickListener(v -> finish());

        btnShare.setOnClickListener(v -> {
            String text = "Настройки игры «Шашки» (черновик, без сохранения параметров).";
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("text/plain");
            sendIntent.putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(sendIntent, "Поделиться"));
        });

        // Логику выбора (Белые/Чёрные, Да/Нет и т.д.) сделаем позже через SharedPreferences
    }
}
