package ru.goman.checkers;

import android.content.Intent;
import android.os.Bundle;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;

import androidx.appcompat.app.AppCompatActivity;

public class RulesActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_rules);

        Button btnHome = findViewById(R.id.btn_rules_home);
        Button btnShare = findViewById(R.id.btn_rules_share);
        WebView webView = findViewById(R.id.web_rules);

        // Кнопка "Выход" = назад
        btnHome.setOnClickListener(v -> finish());

        // Поделиться — просто линкуем текст
        btnShare.setOnClickListener(v -> {
            String text = "Правила игры в шашки (офлайн в приложении «Шашки»).";
            Intent sendIntent = new Intent(Intent.ACTION_SEND);
            sendIntent.setType("text/plain");
            sendIntent.putExtra(Intent.EXTRA_TEXT, text);
            startActivity(Intent.createChooser(sendIntent, "Поделиться правилами"));
        });

        // Настройка WebView и загрузка локального HTML
        webView.setWebViewClient(new WebViewClient());
        webView.getSettings().setJavaScriptEnabled(false); // нам JS не нужен
        webView.loadUrl("file:///android_asset/rules_ru.html");
    }
}
