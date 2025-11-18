package ru.goman.checkers.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.goman.checkers.BaseActivity;
import ru.goman.checkers.LevelSelectActivity;
import ru.goman.checkers.MainActivity;
import ru.goman.checkers.R;
import ru.goman.checkers.engine.AiEngine;
import ru.goman.checkers.logic.GameLogic;
import ru.goman.checkers.model.BoardState;
import ru.goman.checkers.model.Move;
import ru.goman.checkers.model.PieceType;
import ru.goman.checkers.model.Player;

/**
 * Экран игры в русские шашки.
 * Вся логика в GameLogic/BoardState, здесь только UI + режимы.
 */
public class GameActivity extends BaseActivity {

    // Задержка "раздумья" ИИ (мс)
    private static final long AI_THINK_MIN_MS = 500L;
    private static final long AI_THINK_MAX_MS = 800L;

    public static final String EXTRA_VS_AI = "EXTRA_VS_AI";
    public static final String EXTRA_DIFFICULTY = "EXTRA_DIFFICULTY";

    // SharedPreferences (статистика)
    private static final String PREFS_STATS       = "checkers_stats";
    private static final String KEY_GAMES_PREFIX  = "games_level_";
    private static final String KEY_WINS_PREFIX   = "wins_level_";
    private static final String KEY_LOSSES_PREFIX = "losses_level_";

    // SharedPreferences (настройки)
    private static final String PREFS_SETTINGS   = "checkers_settings";
    private static final String KEY_HUMAN_COLOR  = "human_color_vs_ai";   // "WHITE" / "BLACK"
    private static final String COLOR_WHITE      = "WHITE";
    private static final String COLOR_BLACK      = "BLACK";
    private static final String KEY_MOVE_HINT    = "move_hint";
    private static final String KEY_MUST_CAPTURE = "must_capture";

    // Жёсткая логика цветов: человек = WHITE, ИИ = BLACK
    private static final Player HUMAN_LOGICAL_COLOR = Player.WHITE;
    private static final Player AI_LOGICAL_COLOR    = Player.BLACK;

    // Логика игры
    private GameLogic gameLogic;
    private BoardState boardState;

    // Движок ИИ
    private AiEngine aiEngine;

    // Режим сложности (0 = easy, 1 = medium, 2 = hard, 3 = expert, 4 = grandmaster)
    private int difficultyLevel = 0;

    // Настройки
    private boolean moveHintEnabled        = true;
    private boolean mustCaptureRuleEnabled = true;

    // История состояний и ходов для Undo (по одному ходу)
    /** Снимки состояния И ДО хода. На каждый ход ровно один снапшот. */
    private final List<GameLogic.GameSnapshot> history = new ArrayList<>();
    /** Последовательность действительно выполненных ходов (в том же порядке). */
    private final List<Move> moveHistory = new ArrayList<>();

    // UI-состояние выбора
    private int selectedRow = -1;
    private int selectedCol = -1;
    private List<Move> highlightedMoves = Collections.emptyList();

    // Режимы игры
    private boolean vsAi = false;
    /** За какого логического цвета играет ИИ (жёстко BLACK). */
    private Player aiPlaysFor = AI_LOGICAL_COLOR;
    /** Кто ходит первым: true — человек, false — ИИ. */
    private boolean humanStartsFirst = true;
    /** Логический игрок, который начал партию (для текста "Белые / Чёрные"). */
    private Player startingPlayer = HUMAN_LOGICAL_COLOR;

    /**
     * Визуальный цвет игрока:
     * true  — игрок рисуется "как белый" (светлые фишки снизу),
     * false — игрок рисуется "как чёрный" (тёмные фишки снизу).
     * Логически он ВСЕГДА HUMAN_LOGICAL_COLOR (WHITE).
     */
    private boolean humanVisualIsWhite = true;

    // UI
    private int               levelNameResId;
    private TextView          tvScore;
    private TextView          tvTurn;
    private CheckersBoardView boardView;
    private ImageButton       btnUndo;

    private boolean isFirstAIMove = true;
    private boolean isUndoInProgress = false;

    // ------------------------------------------------------------------------
    // ЖИЗНЕННЫЙ ЦИКЛ
    // ------------------------------------------------------------------------

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_game);

        tvScore   = findViewById(R.id.tv_score_value);
        TextView tvLevel = findViewById(R.id.tv_game_level);
        tvTurn    = findViewById(R.id.tv_game_turn);
        boardView = findViewById(R.id.board_view);

        ImageButton btnHome    = findViewById(R.id.btn_game_home);
        ImageButton btnRestart = findViewById(R.id.btn_game_restart);
        btnUndo                = findViewById(R.id.btn_game_undo);
        ImageButton btnSound   = findViewById(R.id.btn_game_sound);

        btnHome.setSoundEffectsEnabled(false);
        btnRestart.setSoundEffectsEnabled(false);
        btnUndo.setSoundEffectsEnabled(false);
        btnSound.setSoundEffectsEnabled(false);

        // Режим игры (vs AI / 2 игрока)
        vsAi = getIntent().getBooleanExtra(EXTRA_VS_AI, false);

        if (vsAi) {
            levelNameResId = getIntent().getIntExtra(
                    LevelSelectActivity.EXTRA_LEVEL_NAME_RES_ID,
                    R.string.level_easy
            );
            String levelName = getString(levelNameResId);

            difficultyLevel = getIntent().getIntExtra(EXTRA_DIFFICULTY, 0);

            tvLevel.setVisibility(View.VISIBLE);
            tvLevel.setText(getString(R.string.game_level_format, levelName));
        } else {
            difficultyLevel = 0;
            levelNameResId = 0;
            tvLevel.setVisibility(View.GONE);
        }

        SharedPreferences settings = getSharedPreferences(PREFS_SETTINGS, MODE_PRIVATE);
        moveHintEnabled        = settings.getBoolean(KEY_MOVE_HINT, true);
        mustCaptureRuleEnabled = settings.getBoolean(KEY_MUST_CAPTURE, true);

        String humanColor = settings.getString(KEY_HUMAN_COLOR, COLOR_WHITE);

        if (vsAi) {
            aiPlaysFor = AI_LOGICAL_COLOR;

            boolean humanChoosesBlack = COLOR_BLACK.equals(humanColor);

            humanVisualIsWhite = !humanChoosesBlack;
            boardView.setHumanIsWhite(humanVisualIsWhite);

            humanStartsFirst = !humanChoosesBlack;

            aiEngine = new AiEngine(aiPlaysFor);
        } else {
            aiPlaysFor = AI_LOGICAL_COLOR; // не используется, но пусть будет консистентно
            boardView.setHumanIsWhite(true);
            humanStartsFirst = true;
            humanVisualIsWhite = true;
            aiEngine = null;
        }

        tvScore.setText("0 : 0");

        btnHome.setOnClickListener(withClickSound(v -> {
            startActivity(new Intent(GameActivity.this, MainActivity.class));
            finish();
        }));
        btnRestart.setOnClickListener(withClickSound(v -> resetGame()));
        btnUndo.setOnClickListener(withClickSound(v -> handleUndoClick()));
        btnSound.setOnClickListener(withClickSound(v ->
                Toast.makeText(
                        this,
                        getString(R.string.game_sound_not_implemented),
                        Toast.LENGTH_SHORT
                ).show()
        ));

        boardView.setOnCellClickListener(this::onCellClicked);

        startGame();
    }

    // ------------------------------------------------------------------------
    // СТАРТ / СБРОС ИГРЫ
    // ------------------------------------------------------------------------

    /** Полный старт новой партии. */
    private void startGame() {
        history.clear();
        moveHistory.clear();          // <--- ВАЖНО: чистим историю ходов
        selectedRow = selectedCol = -1;
        highlightedMoves = Collections.emptyList();
        isFirstAIMove = true;
        isUndoInProgress = false;

        startingPlayer = humanStartsFirst ? HUMAN_LOGICAL_COLOR : AI_LOGICAL_COLOR;
        gameLogic = GameLogic.newGame(startingPlayer, mustCaptureRuleEnabled);

        refreshBoardViewFromLogic();
        boardView.clearSelection();

        updateTurnText();
        updateScoreFromBoard();
        updateUndoButtonState();

        maybeMakeAIMoveIfNeeded();
    }

    private void resetGame() {
        startGame();
    }

    /** Обновить boardState и UI-доску из логики. */
    private void refreshBoardViewFromLogic() {
        if (gameLogic == null || boardView == null) return;
        boardState = gameLogic.getBoard();
        boardView.setBoardState(boardState);
    }

    /**
     * Текст «Ход: Белые / Чёрные».
     */
    private void updateTurnText() {
        if (tvTurn == null || gameLogic == null) return;

        Player current = gameLogic.getCurrentPlayer();
        boolean showWhite = (current == startingPlayer);

        tvTurn.setText(showWhite
                ? getString(R.string.game_turn_white)
                : getString(R.string.game_turn_black));
    }

    // ------------------------------------------------------------------------
    // ОБРАБОТКА КЛИКОВ ПО ДОСКЕ
    // ------------------------------------------------------------------------

    private void onCellClicked(int row, int col) {
        if (boardState == null || gameLogic == null) return;

        if (isUndoInProgress) {
            boardView.clearMoveHints();
            return;
        }

        if (!boardState.isInside(row, col)) {
            boardView.clearMoveHints();
            return;
        }

        // во время хода ИИ клики игнорируем
        if (vsAi && gameLogic.getCurrentPlayer() == aiPlaysFor) {
            boardView.clearMoveHints();
            return;
        }

        if (gameLogic.isCaptureChainInProgress()) {
            int cr = gameLogic.getChainRow();
            int cc = gameLogic.getChainCol();

            boolean isPieceCell = (row == cr && col == cc);
            boolean isMoveCell = false;
            for (Move m : highlightedMoves) {
                if (m.getToRow() == row && m.getToCol() == col) {
                    isMoveCell = true;
                    break;
                }
            }

            if (!isPieceCell && !isMoveCell) {
                boardView.clearMoveHints();
                return;
            }
        }

        boolean clickedOwnPiece = boardState.getPiece(row, col)
                .belongsTo(gameLogic.getCurrentPlayer());

        if (clickedOwnPiece) {
            handleSelectPiece(row, col);
        } else if (selectedRow >= 0 && selectedCol >= 0) {
            handleAttemptMoveTo(row, col);
        } else {
            boardView.clearMoveHints();
        }
    }

    private void handleSelectPiece(int row, int col) {
        if (boardState == null || gameLogic == null) return;

        if (!boardState.getPiece(row, col).belongsTo(gameLogic.getCurrentPlayer())) {
            return;
        }

        List<Move> movesForThisPiece = gameLogic.getMovesForCell(row, col);

        if (movesForThisPiece.isEmpty()) {
            if (gameLogic.isMustCapture() && !gameLogic.isCaptureChainInProgress()) {
                Toast.makeText(
                        this,
                        getString(R.string.game_must_capture_hint),
                        Toast.LENGTH_SHORT
                ).show();
            }
            return;
        }

        if (gameLogic.isCaptureChainInProgress()) {
            int chainRow = gameLogic.getChainRow();
            int chainCol = gameLogic.getChainCol();
            if (row != chainRow || col != chainCol) {
                return;
            }
        }

        selectedRow = row;
        selectedCol = col;
        highlightedMoves = movesForThisPiece;

        boardView.selectPiece(row, col, true);
        showMoveHintsForCurrentSelection();
    }

    private void handleAttemptMoveTo(int targetRow, int targetCol) {
        if (boardState == null || gameLogic == null) return;
        if (!boardState.isInside(targetRow, targetCol)) return;
        if (highlightedMoves.isEmpty()) return;

        Move chosen = findMoveTo(targetRow, targetCol, highlightedMoves);
        if (chosen == null) {
            boardView.clearMoveHints();
            return;
        }

        makeMove(chosen);
    }

    // ------------------------------------------------------------------------
    // ВЫПОЛНЕНИЕ ХОДА
    // ------------------------------------------------------------------------

    private void makeMove(@NonNull Move move) {
        if (gameLogic == null || boardState == null || boardView == null) return;

        if (!gameLogic.isMoveLegal(move)) {
            return;
        }

        // Сохраняем снапшот ДО выполнения хода
        history.add(gameLogic.createSnapshot());
        moveHistory.add(move);

        PieceType piece = boardState.getPiece(move.getFromRow(), move.getFromCol());
        if (piece.isEmpty()) {
            applyMoveInternal(move);
            return;
        }

        boardView.animatePieceMove(
                move.getFromRow(),
                move.getFromCol(),
                move.getToRow(),
                move.getToCol(),
                piece,
                () -> applyMoveInternal(move)
        );
    }

    private void applyMoveInternal(@NonNull Move move) {
        if (gameLogic == null) return;

        if (!gameLogic.isMoveLegal(move)) {
            return;
        }

        GameLogic.MoveResult result = gameLogic.applyMove(move);

        refreshBoardViewFromLogic();

        selectedRow = selectedCol = -1;
        highlightedMoves = Collections.emptyList();
        boardView.clearSelection();

        updateScoreFromBoard();

        if (result.isCaptureChainContinues()) {
            if (vsAi && gameLogic.getCurrentPlayer() == aiPlaysFor) {
                maybeMakeAIMoveIfNeeded();
            } else {
                int chainRow = gameLogic.getChainRow();
                int chainCol = gameLogic.getChainCol();

                selectedRow = chainRow;
                selectedCol = chainCol;

                highlightedMoves = gameLogic.getMovesForCell(chainRow, chainCol);

                boardView.selectPiece(chainRow, chainCol, true);
                showMoveHintsForCurrentSelection();
            }
            updateUndoButtonState();
            return;
        }

        updateTurnText();

        if (result.isGameOver()) {
            Player winner = result.getWinner();
            if (winner == null) {
                Player loser = gameLogic.getCurrentPlayer();
                winner = loser.opposite();
            }
            handleGameOver(winner);
            return;
        }

        maybeMakeAIMoveIfNeeded();
        updateUndoButtonState();
    }

    // ------------------------------------------------------------------------
    // GAME OVER
    // ------------------------------------------------------------------------

    private void handleGameOver(@NonNull Player logicalWinner) {
        String winnerName;

        if (vsAi) {
            boolean humanWon = (logicalWinner == HUMAN_LOGICAL_COLOR);

            boolean winnerVisualIsWhite;
            if (humanWon) {
                winnerVisualIsWhite = humanVisualIsWhite;
            } else {
                winnerVisualIsWhite = !humanVisualIsWhite;
            }

            winnerName = winnerVisualIsWhite
                    ? getString(R.string.game_winner_white)
                    : getString(R.string.game_winner_black);

            updateStatsAfterGame(logicalWinner);
        } else {
            boolean winnerIsStartingSide = (logicalWinner == startingPlayer);
            winnerName = winnerIsStartingSide
                    ? getString(R.string.game_winner_white)
                    : getString(R.string.game_winner_black);
        }

        showGameOverDialog(winnerName);
    }

    private void showGameOverDialog(@NonNull String winnerName) {
        android.view.View dialogView = android.view.LayoutInflater.from(this)
                .inflate(R.layout.dialog_game_over, null, false);

        TextView tvWinner  = dialogView.findViewById(R.id.tv_game_over_winner);
        android.widget.Button btnRestart = dialogView.findViewById(R.id.btn_game_over_restart);
        android.widget.Button btnMenu    = dialogView.findViewById(R.id.btn_game_over_menu);

        btnRestart.setSoundEffectsEnabled(false);
        btnMenu.setSoundEffectsEnabled(false);

        tvWinner.setText(getString(R.string.game_over_winner_format, winnerName));

        androidx.appcompat.app.AlertDialog dialog =
                new androidx.appcompat.app.AlertDialog.Builder(this, R.style.GameAlertDialog)
                        .setView(dialogView)
                        .setCancelable(false)
                        .create();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawable(
                    new android.graphics.drawable.ColorDrawable(
                            android.graphics.Color.TRANSPARENT
                    )
            );
        }

        btnRestart.setOnClickListener(withClickSound(v -> {
            dialog.dismiss();
            resetGame();
        }));

        btnMenu.setOnClickListener(withClickSound(v -> {
            dialog.dismiss();
            startActivity(new Intent(GameActivity.this, MainActivity.class));
            finish();
        }));

        dialog.show();
    }

    // ------------------------------------------------------------------------
    // UNDO ПО ОДНОМУ ХОДУ / ПОЛНОЙ ПАРЕ (ИГРОК + ИИ)
    // ------------------------------------------------------------------------

    private void handleUndoClick() {
        if (gameLogic == null) return;

        if (history.isEmpty() || moveHistory.isEmpty()) {
            Toast.makeText(
                    this,
                    getString(R.string.game_no_undo_moves),
                    Toast.LENGTH_SHORT
            ).show();
            updateUndoButtonState();
            return;
        }

        if (isUndoInProgress) {
            return;
        }

        isUndoInProgress = true;
        updateUndoButtonState();

        // Режим "2 игрока" — откатываем один последний ход
        if (!vsAi) {
            undoLastMoveInternal(() -> {
                isUndoInProgress = false;
                updateUndoButtonState();
            });
            return;
        }

        // Режим против ИИ — откатываем полный "раунд":
        // все ходы ИИ после последнего хода человека + сам этот ход.
        if (gameLogic.getCurrentPlayer() != HUMAN_LOGICAL_COLOR) {
            // На всякий случай, сюда попадать не должны (кнопка отключена)
            isUndoInProgress = false;
            updateUndoButtonState();
            return;
        }

        int lastIndex = history.size() - 1;
        int targetIndex = -1;

        // Ищем последний ход человека (по снапшоту ДО хода)
        for (int i = lastIndex; i >= 0; i--) {
            GameLogic.GameSnapshot snapshot = history.get(i);
            if (snapshot.getCurrentPlayer() == HUMAN_LOGICAL_COLOR) {
                targetIndex = i;
                break;
            }
        }

        if (targetIndex == -1) {
            // Человек ещё не ходил (например, ИИ сделал первый ход)
            Toast.makeText(
                    this,
                    getString(R.string.game_no_undo_moves),
                    Toast.LENGTH_SHORT
            ).show();
            isUndoInProgress = false;
            updateUndoButtonState();
            return;
        }

        final int movesToUndo = lastIndex - targetIndex + 1;

        undoMultipleMoves(movesToUndo, () -> {
            isUndoInProgress = false;
            updateUndoButtonState();
        });
    }

    /**
     * Откатывает несколько последних ходов подряд с анимацией.
     */
    private void undoMultipleMoves(final int movesToUndo, final Runnable onFinished) {
        if (movesToUndo <= 0) {
            if (onFinished != null) {
                onFinished.run();
            }
            return;
        }

        undoLastMoveInternal(() -> undoMultipleMoves(movesToUndo - 1, onFinished));
    }

    /**
     * Откатывает один последний ход (без различия режимов).
     * Вся логика сохранена, как была, только обёрнута в колбэк.
     */
    private void undoLastMoveInternal(final Runnable onFinished) {
        if (gameLogic == null) {
            if (onFinished != null) {
                onFinished.run();
            }
            return;
        }

        if (history.isEmpty() || moveHistory.isEmpty()) {
            if (onFinished != null) {
                onFinished.run();
            }
            return;
        }

        // Снимаем последний ход и соответствующий снапшот ДО этого хода
        Move lastMove = moveHistory.remove(moveHistory.size() - 1);
        GameLogic.GameSnapshot snapshot = history.remove(history.size() - 1);

        // Берём тип шашки на конечной клетке (текущее состояние ещё "после хода")
        PieceType piece = boardState.getPiece(lastMove.getToRow(), lastMove.getToCol());

        Runnable applyAndFinish = () -> {
            applyUndoSnapshot(snapshot);
            if (onFinished != null) {
                onFinished.run();
            }
        };

        // Если почему-то фигуры нет (не успели отрисовать и т.п.) — просто откатываем снапшот
        if (piece.isEmpty() || boardView == null) {
            applyAndFinish.run();
            return;
        }

        // Анимируем возврат шашки обратно, ПОСЛЕ чего восстанавливаем снапшот
        boardView.animatePieceMove(
                lastMove.getToRow(),
                lastMove.getToCol(),
                lastMove.getFromRow(),
                lastMove.getFromCol(),
                piece,
                applyAndFinish
        );
    }

    /** Применяет снапшот после UNDO и синхронизирует UI. */
    private void applyUndoSnapshot(@NonNull GameLogic.GameSnapshot snapshot) {
        gameLogic.restoreFromSnapshot(snapshot);
        refreshBoardViewFromLogic();

        selectedRow = -1;
        selectedCol = -1;
        highlightedMoves = Collections.emptyList();

        boardView.clearSelection();
        updateTurnText();
        updateScoreFromBoard();
        updateUndoButtonState();
    }

    // ------------------------------------------------------------------------
    // СТАТИСТИКА
    // ------------------------------------------------------------------------

    private void updateStatsAfterGame(@NonNull Player winnerPlayer) {
        boolean humanWon = (winnerPlayer == HUMAN_LOGICAL_COLOR);

        SharedPreferences prefs = getSharedPreferences(PREFS_STATS, MODE_PRIVATE);

        String suffix = String.valueOf(levelNameResId);

        String keyGames  = KEY_GAMES_PREFIX  + suffix;
        String keyWins   = KEY_WINS_PREFIX   + suffix;
        String keyLosses = KEY_LOSSES_PREFIX + suffix;

        int games  = prefs.getInt(keyGames, 0);
        int wins   = prefs.getInt(keyWins, 0);
        int losses = prefs.getInt(keyLosses, 0);

        games++;
        if (humanWon) {
            wins++;
        } else {
            losses++;
        }

        prefs.edit()
                .putInt(keyGames, games)
                .putInt(keyWins, wins)
                .putInt(keyLosses, losses)
                .apply();
    }

    // ------------------------------------------------------------------------
    // ВСПОМОГАТЕЛЬНЫЕ МЕТОДЫ ДЛЯ UI
    // ------------------------------------------------------------------------

    private void showMoveHintsForCurrentSelection() {
        if (boardView == null) return;

        List<Move> moves = (highlightedMoves != null)
                ? highlightedMoves
                : Collections.emptyList();

        boardView.showMoveHints(moves, moveHintEnabled);
    }

    private void updateScoreFromBoard() {
        if (boardState == null || tvScore == null) return;

        int whiteCount = boardState.countPieces(Player.WHITE);
        int blackCount = boardState.countPieces(Player.BLACK);

        int whiteCaptured = BoardState.INITIAL_PIECES_PER_SIDE - whiteCount;
        int blackCaptured = BoardState.INITIAL_PIECES_PER_SIDE - blackCount;

        tvScore.setText(getString(R.string.score_format, whiteCaptured, blackCaptured));
    }

    /**
     * Включаем/выключаем кнопку Undo в зависимости от режима и состояния.
     */
    private void updateUndoButtonState() {
        if (btnUndo == null) {
            return;
        }

        boolean enabled;

        if (gameLogic == null || history.isEmpty() || moveHistory.isEmpty()) {
            enabled = false;
        } else if (!vsAi) {
            // 2 игрока — можно отменять, если есть ходы и сейчас не идёт отмена
            enabled = !isUndoInProgress;
        } else {
            // Против ИИ — отмена доступна только на ходу человека
            if (!isUndoInProgress && gameLogic.getCurrentPlayer() == HUMAN_LOGICAL_COLOR) {
                boolean hasHumanMove = false;
                for (int i = history.size() - 1; i >= 0; i--) {
                    if (history.get(i).getCurrentPlayer() == HUMAN_LOGICAL_COLOR) {
                        hasHumanMove = true;
                        break;
                    }
                }
                enabled = hasHumanMove;
            } else {
                enabled = false;
            }
        }

        btnUndo.setEnabled(enabled);
        btnUndo.setAlpha(enabled ? 1f : 0.4f);
    }

    private Move findMoveTo(int targetRow, int targetCol, List<Move> moves) {
        for (Move m : moves) {
            if (m.getToRow() == targetRow && m.getToCol() == targetCol) {
                return m;
            }
        }
        return null;
    }

    // ------------------------------------------------------------------------
    // ЛОГИКА ХОДА ИИ
    // ------------------------------------------------------------------------

    /** Случайная задержка для хода ИИ в диапазоне [AI_THINK_MIN_MS; AI_THINK_MAX_MS]. */
    private long getRandomAiDelayMs() {
        long range = AI_THINK_MAX_MS - AI_THINK_MIN_MS;
        return AI_THINK_MIN_MS + (long) (Math.random() * (range + 1L));
    }

    private void maybeMakeAIMoveIfNeeded() {
        if (!vsAi || gameLogic == null || boardState == null) return;
        if (aiEngine == null) return;
        if (gameLogic.getCurrentPlayer() != aiPlaysFor) return;

        if (isFirstAIMove) {
            isFirstAIMove = false;
        }

        long delay = getRandomAiDelayMs();

        boardView.postDelayed(() -> {
            if (!vsAi || gameLogic == null || aiEngine == null) return;
            if (gameLogic.getCurrentPlayer() != aiPlaysFor) return;

            Move aiMove = computeAIMove(difficultyLevel);
            if (aiMove != null) {
                makeMove(aiMove);
            }
        }, delay);
    }

    private Move computeAIMove(int difficultyLevel) {
        if (!vsAi || gameLogic == null || aiEngine == null) {
            return null;
        }
        return aiEngine.chooseMove(gameLogic, difficultyLevel);
    }
}
