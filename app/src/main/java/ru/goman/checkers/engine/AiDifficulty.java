package ru.goman.checkers.engine;

import androidx.annotation.NonNull;

/**
 * Уровень сложности ИИ.
 * levelIndex — целочисленное значение (0..4), которое удобно хранить во внешнем коде.
 * searchDepth — глубина поиска в полуходах для "глубоких" уровней.
 * allowRandomness — можно ли использовать рандом между равноценными ходами.
 */
public enum AiDifficulty {

    /**
     * EASY:
     *  - без поиска вперёд, просто случайный допустимый ход.
     */
    EASY(0, 0, true),

    /**
     * MEDIUM:
     *  - один ход вперёд, смотрим материал;
     *  - между равноценными ходами добавляем немного случайности.
     */
    MEDIUM(1, 1, true),

    /**
     * HARD:
     *  - минимакс с альфа-бета, умеренная глубина.
     */
    HARD(2, 3, false),

    /**
     * EXPERT:
     *  - минимакс с альфа-бета, повышенная глубина.
     */
    EXPERT(3, 5, false),

    /**
     * GRANDMASTER:
     *  - максимальная глубина поиска, разумная для мобильного устройства.
     *  При необходимости глубину можно будет подкрутить.
     */
    GRANDMASTER(4, 6, false);

    private final int levelIndex;
    private final int searchDepth;
    private final boolean allowRandomness;

    AiDifficulty(int levelIndex, int searchDepth, boolean allowRandomness) {
        this.levelIndex = levelIndex;
        this.searchDepth = searchDepth;
        this.allowRandomness = allowRandomness;
    }

    /**
     * Целочисленный индекс сложности (0..4),
     * который удобно хранить/передавать снаружи (например, через Intent).
     */
    public int getLevelIndex() {
        return levelIndex;
    }

    /**
     * Рекомендуемая глубина поиска (в полуходах).
     * 0 — без поиска вперёд (чистый рандом).
     */
    public int getSearchDepth() {
        return searchDepth;
    }

    /**
     * Можно ли использовать случайность между равноценными ходами.
     */
    public boolean isRandomnessAllowed() {
        return allowRandomness;
    }

    /**
     * Безопасное преобразование int → AiDifficulty.
     * Меньше минимума → EASY, больше максимума → GRANDMASTER.
     */
    @NonNull
    public static AiDifficulty fromLevelIndex(int levelIndex) {
        for (AiDifficulty d : values()) {
            if (d.levelIndex == levelIndex) {
                return d;
            }
        }

        if (levelIndex <= EASY.levelIndex) {
            return EASY;
        }
        if (levelIndex >= GRANDMASTER.levelIndex) {
            return GRANDMASTER;
        }

        // На всякий случай, если попадём "между" известными индексами.
        return MEDIUM;
    }
}
