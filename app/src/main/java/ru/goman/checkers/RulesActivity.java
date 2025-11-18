package ru.goman.checkers;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.ImageButton;

public class RulesActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rules);

        ImageButton btnHome = findViewById(R.id.btn_rules_home);
        WebView webView     = findViewById(R.id.web_rules);

        // Отключаем системный звук, чтобы не было дубля с кастомным
        btnHome.setSoundEffectsEnabled(false);

        // Кнопка "Выход" = назад в MainActivity
        btnHome.setOnClickListener(withClickSound(v -> finish()));

        // Настройка WebView и загрузка локального HTML с правилами
        webView.setWebViewClient(new WebViewClient());
        webView.getSettings().setJavaScriptEnabled(false); // JS не нужен

        SharedPreferences prefs =
                getSharedPreferences("checkers_settings", MODE_PRIVATE);
        String lang = prefs.getString("language", "ru");

        String file = lang.equals("en") ? "rules_en.html" : "rules_ru.html";
        webView.loadUrl("file:///android_asset/" + file);
    }
}
