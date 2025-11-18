package ru.goman.checkers;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.StringRes;

public class StatsActivity extends BaseActivity {

    private static final String PREFS_STATS       = "checkers_stats";
    private static final String KEY_GAMES_PREFIX  = "games_level_";
    private static final String KEY_WINS_PREFIX   = "wins_level_";
    private static final String KEY_LOSSES_PREFIX = "losses_level_";

    private static class GameStats {
        final int games;
        final int wins;
        final int losses;

        GameStats(int games, int wins, int losses) {
            this.games = games;
            this.wins = wins;
            this.losses = losses;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_stats);

        ImageButton btnHome = findViewById(R.id.btn_stats_home);

        // Отключаем системный звук, чтобы не было дубля с кастомным
        btnHome.setSoundEffectsEnabled(false);

        // Домой: просто закрываем экран статистики (возврат в MainActivity)
        btnHome.setOnClickListener(withClickSound(v -> finish()));

        // Заполняем значения "Сыграно / Побед / Поражений"
        populateStatsOnScreen();
    }

    // ------------------------------------------------------------------------
    // ЧТЕНИЕ СТАТИСТИКИ ИЗ PREFS
    // ------------------------------------------------------------------------

    private GameStats loadStatsForLevel(@StringRes int levelNameResId) {
        SharedPreferences prefs = getSharedPreferences(PREFS_STATS, MODE_PRIVATE);

        String suffix = String.valueOf(levelNameResId);

        String keyGames  = KEY_GAMES_PREFIX  + suffix;
        String keyWins   = KEY_WINS_PREFIX   + suffix;
        String keyLosses = KEY_LOSSES_PREFIX + suffix;

        int games  = prefs.getInt(keyGames, 0);
        int wins   = prefs.getInt(keyWins, 0);
        int losses = prefs.getInt(keyLosses, 0);

        return new GameStats(games, wins, losses);
    }

    // ------------------------------------------------------------------------
    // ЗАПОЛНЯЕМ ЭКРАН
    // ------------------------------------------------------------------------

    private void populateStatsOnScreen() {
        // грузим данные
        GameStats easy        = loadStatsForLevel(R.string.level_easy);
        GameStats medium      = loadStatsForLevel(R.string.level_medium);
        GameStats hard        = loadStatsForLevel(R.string.level_hard);
        GameStats expert      = loadStatsForLevel(R.string.level_expert);
        GameStats grandmaster = loadStatsForLevel(R.string.level_grandmaster);

        // Лёгкий
        TextView tvEasyPlayed  = findViewById(R.id.tv_easy_played);
        TextView tvEasyWon     = findViewById(R.id.tv_easy_won);
        TextView tvEasyLost    = findViewById(R.id.tv_easy_lost);

        // Средний
        TextView tvMediumPlayed = findViewById(R.id.tv_medium_played);
        TextView tvMediumWon    = findViewById(R.id.tv_medium_won);
        TextView tvMediumLost   = findViewById(R.id.tv_medium_lost);

        // Сложный
        TextView tvHardPlayed  = findViewById(R.id.tv_hard_played);
        TextView tvHardWon     = findViewById(R.id.tv_hard_won);
        TextView tvHardLost    = findViewById(R.id.tv_hard_lost);

        // Эксперт
        TextView tvExpertPlayed = findViewById(R.id.tv_expert_played);
        TextView tvExpertWon    = findViewById(R.id.tv_expert_won);
        TextView tvExpertLost   = findViewById(R.id.tv_expert_lost);

        // Гроссмейстер
        TextView tvGrandPlayed = findViewById(R.id.tv_grandmaster_played);
        TextView tvGrandWon    = findViewById(R.id.tv_grandmaster_won);
        TextView tvGrandLost   = findViewById(R.id.tv_grandmaster_lost);

        // Ставим текст через getString(..., value), чтобы убрать варнинги

        if (tvEasyPlayed != null)
            tvEasyPlayed.setText(getString(R.string.stats_played_format, easy.games));
        if (tvEasyWon != null)
            tvEasyWon.setText(getString(R.string.stats_won_format, easy.wins));
        if (tvEasyLost != null)
            tvEasyLost.setText(getString(R.string.stats_lost_format, easy.losses));

        if (tvMediumPlayed != null)
            tvMediumPlayed.setText(getString(R.string.stats_played_format, medium.games));
        if (tvMediumWon != null)
            tvMediumWon.setText(getString(R.string.stats_won_format, medium.wins));
        if (tvMediumLost != null)
            tvMediumLost.setText(getString(R.string.stats_lost_format, medium.losses));

        if (tvHardPlayed != null)
            tvHardPlayed.setText(getString(R.string.stats_played_format, hard.games));
        if (tvHardWon != null)
            tvHardWon.setText(getString(R.string.stats_won_format, hard.wins));
        if (tvHardLost != null)
            tvHardLost.setText(getString(R.string.stats_lost_format, hard.losses));

        if (tvExpertPlayed != null)
            tvExpertPlayed.setText(getString(R.string.stats_played_format, expert.games));
        if (tvExpertWon != null)
            tvExpertWon.setText(getString(R.string.stats_won_format, expert.wins));
        if (tvExpertLost != null)
            tvExpertLost.setText(getString(R.string.stats_lost_format, expert.losses));

        if (tvGrandPlayed != null)
            tvGrandPlayed.setText(getString(R.string.stats_played_format, grandmaster.games));
        if (tvGrandWon != null)
            tvGrandWon.setText(getString(R.string.stats_won_format, grandmaster.wins));
        if (tvGrandLost != null)
            tvGrandLost.setText(getString(R.string.stats_lost_format, grandmaster.losses));
    }
}
