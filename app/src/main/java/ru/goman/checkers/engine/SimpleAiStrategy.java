package ru.goman.checkers.engine;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Random;

import ru.goman.checkers.logic.GameLogic;
import ru.goman.checkers.model.BoardState;
import ru.goman.checkers.model.Move;
import ru.goman.checkers.model.PieceType;
import ru.goman.checkers.model.Player;

/**
 * Базовая стратегия ИИ.
 * Поведение по уровням сложности:
 *  - EASY:
 *      * выбор случайного хода (все правила обязательного взятия уже внутри GameLogic);
 *  - MEDIUM:
 *      * один ход вперёд — считаем материал после хода;
 *      * при равенстве оценки между лучшими ходами:
 *          - если allowRandomness == true → выбираем случайный,
 *          - иначе берём первый лучший;
 *  - HARD / EXPERT / GRANDMASTER:
 *      * минимакс с альфа-бета-отсечениями и глубиной searchDepth;
 *      * на "листе" вместо голой оценки используется квази-поиск (quiescence):
 *          - если есть рубки, продолжаем их просчитывать до спокойной позиции;
 *          - это важно для шашек, где длинные обязательные цепочки бьющих ходов.
 * Важно: стратегия никогда не оставляет изменённое состояние —
 * все симуляции делаются через GameSnapshot + restoreFromSnapshot().
 */
public final class SimpleAiStrategy implements AiStrategy {

    private static final int WIN_SCORE   = 100_000;
    private static final int LOSS_SCORE  = -100_000;
    private static final int DRAW_SCORE  = 0;

    private final Random random = new Random();

    @Override
    @Nullable
    public Move chooseMove(@NonNull GameLogic logic,
                           @NonNull Player aiPlayer,
                           @NonNull AiDifficulty difficulty) {

        Objects.requireNonNull(logic, "logic");
        Objects.requireNonNull(aiPlayer, "aiPlayer");
        Objects.requireNonNull(difficulty, "difficulty");

        List<Move> allMoves = logic.getAllMovesForCurrentPlayer();
        if (allMoves.isEmpty()) {
            return null;
        }

        int depth = difficulty.getSearchDepth();

        // EASY — вообще без поиска вперёд.
        if (depth <= 0) {
            return chooseForEasy(allMoves);
        }

        // MEDIUM — один ход вперёд.
        if (depth == 1) {
            return chooseForMedium(logic, aiPlayer, allMoves, difficulty);
        }

        // Всё, что выше — минимакс с альфа-бета и квази-поиском.
        return chooseWithMinimax(logic, aiPlayer, allMoves, difficulty);
    }

    // ---------------------------------------------------------------------
    // EASY
    // ---------------------------------------------------------------------

    /**
     * EASY: просто случайный ход из списка допустимых.
     */
    @Nullable
    private Move chooseForEasy(@NonNull List<Move> moves) {
        if (moves.isEmpty()) {
            return null;
        }
        int index = random.nextInt(moves.size());
        return moves.get(index);
    }

    // ---------------------------------------------------------------------
    // MEDIUM
    // ---------------------------------------------------------------------

    /**
     * MEDIUM: один шаг вперёд — смотрим материал после хода.
     * При равенстве оценок учитываем allowRandomness у difficulty.
     */
    @Nullable
    private Move chooseForMedium(@NonNull GameLogic logic,
                                 @NonNull Player aiPlayer,
                                 @NonNull List<Move> moves,
                                 @NonNull AiDifficulty difficulty) {

        int bestScore = Integer.MIN_VALUE;
        List<Move> bestMoves = new ArrayList<>();

        for (Move move : moves) {
            GameLogic.GameSnapshot snapshot = logic.createSnapshot();
            int score;
            try {
                GameLogic.MoveResult result = logic.applyMove(move);

                if (result.isGameOver()) {
                    Player winner = result.getWinner();
                    if (winner == null) {
                        score = DRAW_SCORE;
                    } else if (winner == aiPlayer) {
                        score = WIN_SCORE;
                    } else {
                        score = LOSS_SCORE;
                    }
                } else {
                    // Оцениваем материал и позицию после одного хода.
                    score = evaluatePosition(logic.getBoard(), aiPlayer);
                }
            } finally {
                logic.restoreFromSnapshot(snapshot);
            }

            if (score > bestScore) {
                bestScore = score;
                bestMoves.clear();
                bestMoves.add(move);
            } else if (score == bestScore) {
                bestMoves.add(move);
            }
        }

        if (bestMoves.isEmpty()) {
            return chooseForEasy(moves);
        }

        // Если для уровня случайность запрещена — берём первый лучший ход.
        if (!difficulty.isRandomnessAllowed() || bestMoves.size() == 1) {
            return bestMoves.get(0);
        }

        int idx = random.nextInt(bestMoves.size());
        return bestMoves.get(idx);
    }

    // ---------------------------------------------------------------------
    // HARD / EXPERT / GRANDMASTER — минимакс с альфа-бета
    // ---------------------------------------------------------------------

    @Nullable
    private Move chooseWithMinimax(@NonNull GameLogic logic,
                                   @NonNull Player aiPlayer,
                                   @NonNull List<Move> moves,
                                   @NonNull AiDifficulty difficulty) {

        int depth = Math.max(1, difficulty.getSearchDepth());

        int bestScore = Integer.MIN_VALUE;
        List<Move> bestMoves = new ArrayList<>();

        for (Move move : moves) {
            GameLogic.GameSnapshot snapshot = logic.createSnapshot();
            int score;
            try {
                GameLogic.MoveResult result = logic.applyMove(move);

                if (result.isGameOver()) {
                    Player winner = result.getWinner();
                    if (winner == null) {
                        score = DRAW_SCORE;
                    } else if (winner == aiPlayer) {
                        // Чем раньше победа, тем лучше — учитываем depth.
                        score = WIN_SCORE + depth;
                    } else {
                        score = LOSS_SCORE - depth;
                    }
                } else {
                    // После первого хода продолжаем поиск глубиной (depth - 1).
                    score = minimax(logic, aiPlayer, depth - 1,
                            Integer.MIN_VALUE, Integer.MAX_VALUE);
                }
            } finally {
                logic.restoreFromSnapshot(snapshot);
            }

            if (score > bestScore) {
                bestScore = score;
                bestMoves.clear();
                bestMoves.add(move);
            } else if (score == bestScore) {
                bestMoves.add(move);
            }
        }

        if (bestMoves.isEmpty()) {
            return chooseForEasy(moves);
        }

        // На "серьёзных" уровнях обычно выключаем рандом, но при желании
        // его можно включить через allowRandomness.
        if (!difficulty.isRandomnessAllowed() || bestMoves.size() == 1) {
            return bestMoves.get(0);
        }

        int idx = random.nextInt(bestMoves.size());
        return bestMoves.get(idx);
    }

    /**
     * Минимакс с альфа-бета-отсечениями.
     * depth — оставшаяся глубина в полуходах.
     */
    private int minimax(@NonNull GameLogic logic,
                        @NonNull Player aiPlayer,
                        int depth,
                        int alpha,
                        int beta) {

        if (depth <= 0) {
            // Вместо "глухой" оценки запускаем квази-поиск:
            // продолжаем просчитывать только рубки, пока позиция не станет "тихой".
            return quiescence(logic, aiPlayer, alpha, beta);
        }

        List<Move> moves = logic.getAllMovesForCurrentPlayer();
        if (moves.isEmpty()) {
            // У текущего игрока нет ходов → он проиграл.
            Player current = logic.getCurrentPlayer();
            return (current == aiPlayer) ? LOSS_SCORE - depth : WIN_SCORE + depth;
        }

        boolean maximizing = (logic.getCurrentPlayer() == aiPlayer);
        int best = maximizing ? Integer.MIN_VALUE : Integer.MAX_VALUE;

        for (Move move : moves) {
            GameLogic.GameSnapshot snapshot = logic.createSnapshot();
            int score;
            try {
                GameLogic.MoveResult result = logic.applyMove(move);

                if (result.isGameOver()) {
                    Player winner = result.getWinner();
                    if (winner == null) {
                        score = DRAW_SCORE;
                    } else if (winner == aiPlayer) {
                        score = WIN_SCORE + depth;
                    } else {
                        score = LOSS_SCORE - depth;
                    }
                } else {
                    score = minimax(logic, aiPlayer, depth - 1, alpha, beta);
                }
            } finally {
                logic.restoreFromSnapshot(snapshot);
            }

            // отсечение
            if (maximizing) {
                if (score > best) {
                    best = score;
                }
                if (score > alpha) {
                    alpha = score;
                }
            } else {
                if (score < best) {
                    best = score;
                }
                if (score < beta) {
                    beta = score;
                }
            }
            if (beta <= alpha) {
                break; // отсечение
            }
        }

        return best;
    }

    // ---------------------------------------------------------------------
    // Квази-поиск (quiescence search)
    // ---------------------------------------------------------------------

    /**
     * Квази-поиск:
     *  - считаем статическую оценку позиции;
     *  - если есть рубки — продолжаем просчитывать только рубящие ходы,
     *    пока позиция не станет "тихой" (без боёв).
     * Это критично для шашек: длинные обязательные цепочки рубок
     * могут сильно искажать оценку, если их обрубить на глубине.
     */
    private int quiescence(@NonNull GameLogic logic,
                           @NonNull Player aiPlayer,
                           int alpha,
                           int beta) {

        // 1. Проверяем, не окончена ли игра.
        List<Move> allMoves = logic.getAllMovesForCurrentPlayer();
        if (allMoves.isEmpty()) {
            Player current = logic.getCurrentPlayer();
            return (current == aiPlayer) ? LOSS_SCORE : WIN_SCORE;
        }

        // 2. Статическая оценка "как есть".
        int standPat = evaluatePosition(logic.getBoard(), aiPlayer);
        boolean maximizing = (logic.getCurrentPlayer() == aiPlayer);

        if (maximizing) {
            if (standPat >= beta) {
                return standPat;
            }
            if (standPat > alpha) {
                alpha = standPat;
            }
        } else {
            if (standPat <= alpha) {
                return standPat;
            }
            if (standPat < beta) {
                beta = standPat;
            }
        }

        // 3. Собираем только ударные ходы.
        List<Move> captureMoves = new ArrayList<>();
        for (Move m : allMoves) {
            if (m.isCapture()) {
                captureMoves.add(m);
            }
        }

        // Нет боёв → позиция достаточно "тихая".
        if (captureMoves.isEmpty()) {
            return standPat;
        }

        // 4. Продолжаем поиск только по рубкам.
        for (Move move : captureMoves) {
            GameLogic.GameSnapshot snapshot = logic.createSnapshot();
            int score;
            try {
                GameLogic.MoveResult result = logic.applyMove(move);

                if (result.isGameOver()) {
                    Player winner = result.getWinner();
                    if (winner == null) {
                        score = DRAW_SCORE;
                    } else if (winner == aiPlayer) {
                        score = WIN_SCORE;
                    } else {
                        score = LOSS_SCORE;
                    }
                } else {
                    score = quiescence(logic, aiPlayer, alpha, beta);
                }
            } finally {
                logic.restoreFromSnapshot(snapshot);
            }

            // отсечение
            if (maximizing) {
                if (score > alpha) {
                    alpha = score;
                }
            } else {
                if (score < beta) {
                    beta = score;
                }
            }
            if (alpha >= beta) {
                break; // отсечение
            }
        }

        return maximizing ? alpha : beta;
    }

    // ---------------------------------------------------------------------
    // Оценка позиции
    // ---------------------------------------------------------------------

    /**
     * Статическая оценка позиции:
     *  - материальный баланс (маны/дамки);
     *  - продвижение мана к дамке;
     *  - близость к центру доски.
     * Всё считается с точки зрения aiPlayer:
     *  > 0 — хорошо для ИИ, < 0 — плохо.
     */
    private int evaluatePosition(@NonNull BoardState board,
                                 @NonNull Player aiPlayer) {

        int score = 0;

        for (int r = 0; r < BoardState.BOARD_SIZE; r++) {
            for (int c = 0; c < BoardState.BOARD_SIZE; c++) {
                PieceType piece = board.getPiece(r, c);
                if (piece.isEmpty()) {
                    continue;
                }

                boolean isAiPiece = piece.belongsTo(aiPlayer);
                int sign = isAiPiece ? 1 : -1;

                if (piece.isMan()) {
                    int base = 100;
                    int advance = advancementBonusForMan(piece, r);
                    int center = centerBonus(r, c);
                    score += sign * (base + advance + center);
                } else if (piece.isKing()) {
                    int base = 180;
                    int center = centerBonusForKing(r, c);
                    score += sign * (base + center);
                }
            }
        }

        return score;
    }

    /**
     * Бонус за продвижение мана к дамке.
     * Для белых — чем ближе к верхней линии (row = 0), тем лучше.
     * Для чёрных — чем ближе к нижней линии (row = 7), тем лучше.
     */
    private int advancementBonusForMan(@NonNull PieceType piece, int row) {
        // Предполагаем классическую раскладку:
        // WHITE движется "вверх" (к row = 0), BLACK — "вниз" (к row = 7).
        int distance;
        if (piece.isWhite()) {
            distance = BoardState.BOARD_SIZE - 1 - row; // 7 - row
        } else {
            distance = row; // 0..7
        }
        // Коэффициент небольшой, чтобы не перебивать материал.
        return distance * 4;
    }

    /**
     * Бонус за центральное положение (для мана).
     * Чем ближе к центру доски, тем больше бонус.
     */
    private int centerBonus(int row, int col) {
        int rowCenter = Math.min(row, BoardState.BOARD_SIZE - 1 - row); // 0..3
        int colCenter = Math.min(col, BoardState.BOARD_SIZE - 1 - col); // 0..3
        int sum = rowCenter + colCenter; // 0..6
        return sum * 3;
    }

    /**
     * Бонус за положение дамки.
     * Для дамки чуть сильнее ценим центр, так как она ходит далеко.
     */
    private int centerBonusForKing(int row, int col) {
        int rowCenter = Math.min(row, BoardState.BOARD_SIZE - 1 - row); // 0..3
        int colCenter = Math.min(col, BoardState.BOARD_SIZE - 1 - col); // 0..3
        int sum = rowCenter + colCenter; // 0..6
        return sum * 4;
    }
}
