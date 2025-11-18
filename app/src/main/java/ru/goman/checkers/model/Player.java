// Player.java
package ru.goman.checkers.model;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Игрок (цвет фигур).
 * Никаких привязок к Android UI — чистая модель.
 */
public enum Player {
    WHITE,
    BLACK;

    /**
     * Получить противоположного игрока.
     */
    @NonNull
    public Player opposite() {
        return this == WHITE ? BLACK : WHITE;
    }


    /**
     * Разбор строки вида "WHITE"/"BLACK" (регистр не важен).
     * Если строка некорректна/нулевая — вернёт defaultValue.
     */
    @NonNull
    public static Player fromColorString(
            @Nullable String value,
            @NonNull Player defaultValue
    ) {
        if (value == null) return defaultValue;

        String v = value.trim();
        if (v.equalsIgnoreCase("WHITE") || v.equalsIgnoreCase("W")) {
            return WHITE;
        }
        if (v.equalsIgnoreCase("BLACK") || v.equalsIgnoreCase("B")) {
            return BLACK;
        }
        return defaultValue;
    }
}
