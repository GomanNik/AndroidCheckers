// Move.java
package ru.goman.checkers.model;

import androidx.annotation.NonNull;

/**
 * Один элементарный ход (один прыжок или одно перемещение).
 * Координаты — индексы клеток 0..7 (строка/столбец).
 * Для рубки capturedRow/capturedCol указывают побитую фигуру,
 * либо -1/-1, если ход без взятия.
 */
public final class Move {

    public static final int NO_CAPTURE = -1;

    private final int fromRow;
    private final int fromCol;
    private final int toRow;
    private final int toCol;
    private final int capturedRow;
    private final int capturedCol;

    /**
     * Обычный ход без взятия.
     */
    public Move(int fromRow, int fromCol, int toRow, int toCol) {
        this(fromRow, fromCol, toRow, toCol, NO_CAPTURE, NO_CAPTURE);
    }

    /**
     * Ход c возможным взятием.
     *
     * @param capturedRow индекс строки побитой фигуры (или -1)
     * @param capturedCol индекс столбца побитой фигуры (или -1)
     */
    public Move(int fromRow,
                int fromCol,
                int toRow,
                int toCol,
                int capturedRow,
                int capturedCol) {

        // Базовая валидация (нулевой индекс и т.п. ловим сразу).
        checkIndex(fromRow, "fromRow");
        checkIndex(fromCol, "fromCol");
        checkIndex(toRow, "toRow");
        checkIndex(toCol, "toCol");

        if (!isNoCapture(capturedRow, capturedCol)) {
            checkIndex(capturedRow, "capturedRow");
            checkIndex(capturedCol, "capturedCol");
        }

        this.fromRow = fromRow;
        this.fromCol = fromCol;
        this.toRow = toRow;
        this.toCol = toCol;
        this.capturedRow = capturedRow;
        this.capturedCol = capturedCol;
    }

    private static void checkIndex(int idx, @NonNull String name) {
        if (idx < 0) {
            throw new IllegalArgumentException("Index " + name + " must be >= 0, got " + idx);
        }
        // Верхнюю границу (<=7) не проверяем, чтобы Move оставался независим
        // от конкретного размера доски. Это проверит логика/BoardState.
    }

    private static boolean isNoCapture(int row, int col) {
        return row == NO_CAPTURE && col == NO_CAPTURE;
    }

    public int getFromRow() {
        return fromRow;
    }

    public int getFromCol() {
        return fromCol;
    }

    public int getToRow() {
        return toRow;
    }

    public int getToCol() {
        return toCol;
    }

    public int getCapturedRow() {
        return capturedRow;
    }

    public int getCapturedCol() {
        return capturedCol;
    }

    public boolean isCapture() {
        return capturedRow >= 0 && capturedCol >= 0;
    }

    @NonNull
    @Override
    public String toString() {
        return "Move{" +
                "from=(" + fromRow + "," + fromCol + ")" +
                ", to=(" + toRow + "," + toCol + ")" +
                (isCapture()
                        ? ", capture=(" + capturedRow + "," + capturedCol + ")"
                        : "") +
                '}';
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof Move)) return false;

        Move move = (Move) o;

        if (fromRow != move.fromRow) return false;
        if (fromCol != move.fromCol) return false;
        if (toRow != move.toRow) return false;
        if (toCol != move.toCol) return false;
        if (capturedRow != move.capturedRow) return false;
        return capturedCol == move.capturedCol;
    }

    @Override
    public int hashCode() {
        int result = fromRow;
        result = 31 * result + fromCol;
        result = 31 * result + toRow;
        result = 31 * result + toCol;
        result = 31 * result + capturedRow;
        result = 31 * result + capturedCol;
        return result;
    }
}
