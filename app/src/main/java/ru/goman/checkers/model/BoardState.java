// BoardState.java
package ru.goman.checkers.model;

import androidx.annotation.NonNull;

/**
 * Модель состояния доски 8x8 для русских шашек.
 * Хранит только данные (int-коды фигур), без логики ходов и без Android.
 * Используется и UI, и игровой логикой, и ИИ.
 */
public class BoardState {

    public static final int BOARD_SIZE = 8;
    public static final int INITIAL_PIECES_PER_SIDE = 12;

    // Плотный массив [row][col] с кодами PieceType.getCode()
    private final int[][] cells = new int[BOARD_SIZE][BOARD_SIZE];

    /**
     * Создаёт пустую доску (все клетки EMPTY).
     */
    public BoardState() {
        clear();
    }

    /**
     * Глубокая копия другой доски.
     */
    public BoardState(@NonNull BoardState other) {
        for (int r = 0; r < BOARD_SIZE; r++) {
            System.arraycopy(other.cells[r], 0, this.cells[r], 0, BOARD_SIZE);
        }
    }

    /**
     * Создать доску из сырых данных int[8][8].
     * Делает глубокую копию и валидирует размер и значения.
     *
     * @throws IllegalArgumentException если размер массива не 8x8
     *                                  или содержит некорректные коды фигур.
     */
    @NonNull
    @SuppressWarnings("unused")
    public static BoardState fromRaw(@NonNull int[][] raw) {
        if (raw.length != BOARD_SIZE) {
            throw new IllegalArgumentException("raw board must have " + BOARD_SIZE + " rows");
        }
        BoardState state = new BoardState();
        for (int r = 0; r < BOARD_SIZE; r++) {
            if (raw[r] == null || raw[r].length != BOARD_SIZE) {
                throw new IllegalArgumentException("raw board row " + r +
                        " must have length " + BOARD_SIZE);
            }
            for (int c = 0; c < BOARD_SIZE; c++) {
                int code = raw[r][c];
                PieceType piece = PieceType.fromCode(code);
                state.cells[r][c] = piece.getCode();
            }

        }
        return state;
    }

    /**
     * Очистить доску — все клетки EMPTY.
     */
    public void clear() {
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                cells[r][c] = PieceType.EMPTY.getCode();
            }
        }
    }

    /**
     * Стандартная начальная расстановка русских шашек.
     * Чёрные сверху, белые снизу.
     */
    public void setupInitialPosition() {
        clear();

        // чёрные – верхние 3 ряда (0..2) на тёмных клетках
        for (int row = 0; row <= 2; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (isDarkCell(row, col)) {
                    cells[row][col] = PieceType.BLACK_MAN.getCode();
                }
            }
        }

        // белые – нижние 3 ряда (5..7) на тёмных клетках
        for (int row = 5; row <= 7; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (isDarkCell(row, col)) {
                    cells[row][col] = PieceType.WHITE_MAN.getCode();
                }
            }
        }
    }

    // ----------------------------------------------------------------
    // Доступ к клеткам
    // ----------------------------------------------------------------

    /**
     * Проверка, что координаты в пределах доски.
     */
    public boolean isInside(int row, int col) {
        return row >= 0 && row < BOARD_SIZE
                && col >= 0 && col < BOARD_SIZE;
    }

    /**
     * Темная ли клетка (игровая) — по правилу (r + c) % 2 == 1.
     */
    public boolean isDarkCell(int row, int col) {
        return ((row + col) & 1) == 1;
    }

    /**
     * Получить int-код фигуры в клетке (см. PieceType.getCode()).
     *
     * @throws IllegalArgumentException если координаты вне доски.
     */
    public int getCode(int row, int col) {
        assertInside(row, col);
        return cells[row][col];
    }

    /**
     * Получить тип фигуры в клетке.
     */
    @NonNull
    public PieceType getPiece(int row, int col) {
        return PieceType.fromCodeOrEmpty(getCode(row, col));
    }

    /**
     * Установить int-код фигуры в клетку (с валидацией кода).
     *
     * @throws IllegalArgumentException если координаты вне доски
     *                                  или код фигуры некорректен.
     */
    public void setCode(int row, int col, int code) {
        assertInside(row, col);
        PieceType piece = PieceType.fromCode(code);
        cells[row][col] = piece.getCode();
    }


    /**
     * Установить тип фигуры в клетку.
     */
    public void setPiece(int row, int col, @NonNull PieceType type) {
        setCode(row, col, type.getCode());
    }

    private void assertInside(int row, int col) {
        if (!isInside(row, col)) {
            throw new IllegalArgumentException(
                    "Cell (" + row + "," + col + ") is outside board " + BOARD_SIZE + "x" + BOARD_SIZE
            );
        }
    }

    // ----------------------------------------------------------------
    // Вспомогательные методы для статистики / ИИ
    // ----------------------------------------------------------------

    /**
     * Посчитать количество фигур указанного игрока (и дамок, и простых).
     */
    public int countPieces(@NonNull Player player) {
        int count = 0;
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                PieceType piece = PieceType.fromCodeOrEmpty(cells[r][c]);
                if (piece.belongsTo(player)) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Посчитать количество простых шашек (манов) у игрока.
     */
    public int countMen(@NonNull Player player) {
        int count = 0;
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                PieceType piece = PieceType.fromCodeOrEmpty(cells[r][c]);
                if (piece.isMan() && piece.belongsTo(player)) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Посчитать количество дамок у игрока.
     */
    public int countKings(@NonNull Player player) {
        int count = 0;
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                PieceType piece = PieceType.fromCodeOrEmpty(cells[r][c]);
                if (piece.isKing() && piece.belongsTo(player)) {
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Получить глубокую копию массива int[8][8].
     * Можно отдавать в ИИ, не боясь, что он испортит текущую доску.
     */
    @NonNull
    public int[][] copyRaw() {
        int[][] copy = new int[BOARD_SIZE][BOARD_SIZE];
        for (int r = 0; r < BOARD_SIZE; r++) {
            System.arraycopy(cells[r], 0, copy[r], 0, BOARD_SIZE);
        }
        return copy;
    }

    /**
     * Глубокая копия BoardState.
     */
    @NonNull
    public BoardState deepCopy() {
        return new BoardState(this);
    }

    // ----------------------------------------------------------------
    // equals / hashCode — полезно для тестов, отладки, кешей
    // ----------------------------------------------------------------

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BoardState)) return false;

        BoardState that = (BoardState) o;

        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                if (this.cells[r][c] != that.cells[r][c]) {
                    return false;
                }
            }
        }
        return true;
    }

    @Override
    public int hashCode() {
        int result = 1;
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                result = 31 * result + cells[r][c];
            }
        }
        return result;
    }
}
