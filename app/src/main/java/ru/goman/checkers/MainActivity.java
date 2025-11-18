package ru.goman.checkers;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import androidx.appcompat.app.AlertDialog;

public class MainActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        ImageButton btnExit   = findViewById(R.id.btn_exit);
        Button btnPlay        = findViewById(R.id.btn_play);
        Button btnRules       = findViewById(R.id.btn_rules);
        Button btnTwoPlayers  = findViewById(R.id.btn_two_players);
        Button btnStats       = findViewById(R.id.btn_stats);
        Button btnSettings    = findViewById(R.id.btn_settings);

        // Отключаем системный звук, чтобы не было дубля с кастомным
        btnExit.setSoundEffectsEnabled(false);
        btnPlay.setSoundEffectsEnabled(false);
        btnRules.setSoundEffectsEnabled(false);
        btnTwoPlayers.setSoundEffectsEnabled(false);
        btnStats.setSoundEffectsEnabled(false);
        btnSettings.setSoundEffectsEnabled(false);

        btnExit.setOnClickListener(withClickSound(v -> showExitDialog()));

        btnPlay.setOnClickListener(withClickSound(v -> {
            Intent i = new Intent(MainActivity.this, LevelSelectActivity.class);
            i.putExtra("vs_ai", true);
            startActivity(i);
        }));

        btnTwoPlayers.setOnClickListener(withClickSound(v -> {
            Intent i = new Intent(MainActivity.this, ru.goman.checkers.ui.GameActivity.class);
            startActivity(i);
        }));

        btnRules.setOnClickListener(withClickSound(v ->
                startActivity(new Intent(MainActivity.this, RulesActivity.class))
        ));

        btnStats.setOnClickListener(withClickSound(v ->
                startActivity(new Intent(MainActivity.this, StatsActivity.class))
        ));

        btnSettings.setOnClickListener(withClickSound(v ->
                startActivity(new Intent(MainActivity.this, SettingsActivity.class))
        ));
    }

    private void showExitDialog() {
        View dialogView = LayoutInflater.from(this)
                .inflate(R.layout.dialog_exit_confirm, null, false);

        Button btnNo  = dialogView.findViewById(R.id.btn_exit_no);
        Button btnYes = dialogView.findViewById(R.id.btn_exit_yes);

        // Отключаем системный звук, чтобы не было дубля с кастомным
        btnNo.setSoundEffectsEnabled(false);
        btnYes.setSoundEffectsEnabled(false);

        AlertDialog dialog = new AlertDialog.Builder(this, R.style.GameAlertDialog)
                .setView(dialogView)
                .setCancelable(true)
                .create();

        // Прозрачный фон, чтобы были видны скруглённые углы bg_dialog_round
        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(
                            android.graphics.Color.TRANSPARENT
                    )
            );
        }

        btnNo.setOnClickListener(withClickSound(v -> dialog.dismiss()));
        btnYes.setOnClickListener(withClickSound(v -> {
            dialog.dismiss();
            finishAffinity();
        }));

        dialog.show();
    }
}
