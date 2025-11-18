// PieceType.java
package ru.goman.checkers.model;

import androidx.annotation.NonNull;

/**
 * Типы фигур на доске.
 * code — числовое значение для хранения в int[][].
 * Соответствие сделано совместимым с текущим GameActivity:
 * 0 = EMPTY
 * 1 = WHITE_MAN
 * 2 = WHITE_KING
 * 3 = BLACK_MAN
 * 4 = BLACK_KING
 */
public enum PieceType {
    EMPTY(0),
    WHITE_MAN(1),
    WHITE_KING(2),
    BLACK_MAN(3),
    BLACK_KING(4);

    private final int code;

    PieceType(int code) {
        this.code = code;
    }

    /**
     * Целочисленный код для хранения в массиве доски.
     */
    public int getCode() {
        return code;
    }

    /**
     * Строгий разбор int-кода.
     * Если код неизвестен — бросает IllegalArgumentException.
     */
    @NonNull
    public static PieceType fromCode(int code) {
        for (PieceType t : values()) {
            if (t.code == code) {
                return t;
            }
        }
        throw new IllegalArgumentException("Unknown piece code: " + code);
    }

    /**
     * Нестрогий разбор int-кода.
     * Если код неизвестен — возвращает EMPTY.
     * Удобно для защитного чтения, если данные могут быть повреждены.
     */
    @NonNull
    public static PieceType fromCodeOrEmpty(int code) {
        for (PieceType t : values()) {
            if (t.code == code) {
                return t;
            }
        }
        return EMPTY;
    }

    public boolean isEmpty() {
        return this == EMPTY;
    }

    public boolean isWhite() {
        return this == WHITE_MAN || this == WHITE_KING;
    }

    public boolean isBlack() {
        return this == BLACK_MAN || this == BLACK_KING;
    }

    public boolean isKing() {
        return this == WHITE_KING || this == BLACK_KING;
    }

    public boolean isMan() {
        return this == WHITE_MAN || this == BLACK_MAN;
    }

    /**
     * Принадлежит ли фигура указанному игроку.
     */
    public boolean belongsTo(@NonNull Player player) {
        if (isEmpty()) return false;
        return (player == Player.WHITE && isWhite())
                || (player == Player.BLACK && isBlack());
    }
}
