package ru.goman.checkers.logic;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import ru.goman.checkers.model.BoardState;
import ru.goman.checkers.model.Move;
import ru.goman.checkers.model.PieceType;
import ru.goman.checkers.model.Player;

/**
 * Логика русских шашек (без Android).
 * Хранит:
 * - BoardState (доску);
 * - текущего игрока;
 * - состояние цепочки взятий (если есть незавершённый бой);
 * - флаг правила обязательного взятия (из настроек);
 * - признак обязательного взятия в текущем положении.
 * Позволяет:
 * - получить все допустимые ходы текущего игрока;
 * - получить ходы для конкретной клетки;
 * - применить ход (с продолжением цепочки боёв, если нужно);
 * - проверить конец игры;
 * - делать снапшоты состояния (для Undo/ИИ).
 * Важно: предполагается, что доска изменяется только через этот класс.
 */
public final class GameLogic {

    // ----------------------------------------------------------------------
    // Вспомогательные типы
    // ----------------------------------------------------------------------

    /**
     * Результат применения хода.
     */
    public static final class MoveResult {

        /** Продолжается ли цепочка взятий (тот же игрок обязан ходить той же шашкой). */
        private final boolean captureChainContinues;

        /**
         * Победитель (если игра закончена).
         * null — игра не окончена.
         * Если not null:
         *   - winner = WHITE или BLACK;
         *   - проигравший — opposite().
         *   - currentPlayer в этот момент указывает на проигравшего
         *     (аналогично тому, как было в GameActivity).
         */
        @Nullable
        private final Player winner;

        private MoveResult(boolean captureChainContinues, @Nullable Player winner) {
            this.captureChainContinues = captureChainContinues;
            this.winner = winner;
        }

        /** Есть ли продолжение цепочки взятий тем же игроком. */
        public boolean isCaptureChainContinues() {
            return captureChainContinues;
        }

        /** Игра окончена? */
        public boolean isGameOver() {
            return winner != null;
        }

        /** Победитель, если игра окончена; иначе null. */
        @Nullable
        public Player getWinner() {
            return winner;
        }

        @Override
        @NonNull
        public String toString() {
            return "MoveResult{" +
                    "captureChainContinues=" + captureChainContinues +
                    ", winner=" + winner +
                    '}';
        }
    }

    /**
     * Снапшот состояния игры (для Undo или ИИ).
     * Содержит:
     * - копию доски;
     * - текущего игрока;
     * - флаги обязательного взятия и цепочки;
     * - координаты шашки, продолжающей бой (если есть).
     */
    public static final class GameSnapshot {
        @NonNull
        private final BoardState boardCopy;
        @NonNull
        private final Player currentPlayer;
        private final boolean mustCapture;
        private final boolean captureChainInProgress;
        private final int chainRow;
        private final int chainCol;

        private GameSnapshot(@NonNull BoardState boardCopy,
                             @NonNull Player currentPlayer,
                             boolean mustCapture,
                             boolean captureChainInProgress,
                             int chainRow,
                             int chainCol) {
            this.boardCopy = boardCopy;
            this.currentPlayer = currentPlayer;
            this.mustCapture = mustCapture;
            this.captureChainInProgress = captureChainInProgress;
            this.chainRow = chainRow;
            this.chainCol = chainCol;
        }

        @NonNull
        public BoardState getBoardCopy() {
            return boardCopy;
        }

        @NonNull
        public Player getCurrentPlayer() {
            return currentPlayer;
        }

        public boolean isMustCapture() {
            return mustCapture;
        }

        public boolean isCaptureChainInProgress() {
            return captureChainInProgress;
        }

        public int getChainRow() {
            return chainRow;
        }

        public int getChainCol() {
            return chainCol;
        }
    }

    // ----------------------------------------------------------------------
    // Поля состояния
    // ----------------------------------------------------------------------

    @NonNull
    private final BoardState board;

    @NonNull
    private Player currentPlayer;

    /**
     * Флаг «правило обязательного взятия включено» (из настроек).
     * Если false — игрок может делать тихие ходы, даже если есть взятия.
     */
    private final boolean mustCaptureRuleEnabled;

    /** Есть ли обязательное взятие для текущего игрока (в рамках его хода, когда цепочки нет). */
    private boolean mustCapture;

    /** Находится ли партия в режиме продолжения цепочки взятий. */
    private boolean captureChainInProgress;
    /** Координаты шашки, которая обязана продолжать бой (если captureChainInProgress = true). */
    private int chainRow;
    private int chainCol;

    /**
     * Кэш текущих ходов (после вызова getAllMovesForCurrentPlayer()).
     * Сбрасывается при каждом изменении позиции (applyMove(), restoreFromSnapshot(), resetPosition()).
     */
    @NonNull
    private List<Move> currentMovesCache = Collections.emptyList();

    // ----------------------------------------------------------------------
    // Конструкторы
    // ----------------------------------------------------------------------

    /**
     * Полный конструктор с передачей флага mustCaptureRuleEnabled.
     */
    public GameLogic(@NonNull BoardState board,
                     @NonNull Player startingPlayer,
                     boolean mustCaptureRuleEnabled) {
        this.board = board;
        this.currentPlayer = startingPlayer;
        this.mustCaptureRuleEnabled = mustCaptureRuleEnabled;
        this.mustCapture = false;
        this.captureChainInProgress = false;
        this.chainRow = -1;
        this.chainCol = -1;
        recomputeCurrentMoves();
    }

    @NonNull
    public static GameLogic newGame(@NonNull Player startingPlayer,
                                    boolean mustCaptureRuleEnabled) {
        BoardState board = new BoardState();
        board.setupInitialPosition();
        return new GameLogic(board, startingPlayer, mustCaptureRuleEnabled);
    }

    // ----------------------------------------------------------------------
    // Публичный API (геттеры состояния)
    // ----------------------------------------------------------------------

    /** Текущая доска (живой объект, НЕ копия). */
    @NonNull
    public BoardState getBoard() {
        return board;
    }

    /** Текущий игрок, который должен ходить. */
    @NonNull
    public Player getCurrentPlayer() {
        return currentPlayer;
    }

    /** Есть ли обязательное взятие в рамках текущего хода (когда нет цепочки). */
    public boolean isMustCapture() {
        return mustCapture;
    }

    /** Сейчас идёт продолжение цепочки взятий? */
    public boolean isCaptureChainInProgress() {
        return captureChainInProgress;
    }

    /** Строка шашки, которая обязана продолжать бой (или -1, если цепочки нет). */
    public int getChainRow() {
        return chainRow;
    }

    /** Столбец шашки, которая обязана продолжать бой (или -1, если цепочки нет). */
    public int getChainCol() {
        return chainCol;
    }

    // ----------------------------------------------------------------------
    // Ходы
    // ----------------------------------------------------------------------

    /**
     * Получить все допустимые ходы для текущего игрока
     * с учётом:
     * - обязательного взятия (если включено mustCaptureRuleEnabled);
     * - возможной цепочки взятий (если она в процессе).
     * Список неизменяемый; при следующем изменении позиции будет пересчитан.
     */
    @NonNull
    public List<Move> getAllMovesForCurrentPlayer() {
        // всегда пересчитываем — логика относительно дешёвая, а ошибок меньше
        recomputeCurrentMoves();
        return currentMovesCache;
    }

    /**
     * Получить список ходов для конкретной клетки (строка/столбец)
     * в рамках текущего состояния (учитывая обязательное взятие и цепочку).
     * Если сейчас идёт цепочка взятий — ходы вернутся только для
     * клетки chainRow/chainCol; для остальных клеток список будет пустым.
     */
    @NonNull
    public List<Move> getMovesForCell(int row, int col) {
        if (!board.isInside(row, col)) {
            return Collections.emptyList();
        }

        if (captureChainInProgress &&
                (row != chainRow || col != chainCol)) {
            // во время цепочки можно ходить только той же шашкой
            return Collections.emptyList();
        }

        // Убедимся, что кэш актуален.
        recomputeCurrentMoves();

        if (currentMovesCache.isEmpty()) {
            return Collections.emptyList();
        }

        List<Move> result = new ArrayList<>();
        for (Move m : currentMovesCache) {
            if (m.getFromRow() == row && m.getFromCol() == col) {
                result.add(m);
            }
        }
        return Collections.unmodifiableList(result);
    }

    /**
     * Проверка, является ли ход допустимым для текущего игрока.
     */
    public boolean isMoveLegal(@NonNull Move move) {
        for (Move legal : getAllMovesForCurrentPlayer()) {
            if (legal.equals(move)) {
                return true;
            }
        }
        return false;
    }

    // ----------------------------------------------------------------------
    // Применение хода
    // ----------------------------------------------------------------------

    /**
     * Применить ход к текущей позиции.
     * Ожидается, что ход взят из getAllMovesForCurrentPlayer() или getMovesForCell().
     * Если ход некорректен (не из списка допустимых) — будет IllegalArgumentException.
     *
     * @param move ход
     * @return MoveResult:
     *  - isCaptureChainContinues() == true → тот же игрок обязан продолжать бой;
     *  - isGameOver() == true → у следующего игрока нет ходов, в getWinner() победитель.
     */
    @NonNull
    public MoveResult applyMove(@NonNull Move move) {
        int fromRow = move.getFromRow();
        int fromCol = move.getFromCol();
        int toRow = move.getToRow();
        int toCol = move.getToCol();

        if (!board.isInside(fromRow, fromCol) || !board.isInside(toRow, toCol)) {
            throw new IllegalArgumentException("Move is outside board: " + move);
        }

        PieceType piece = board.getPiece(fromRow, fromCol);
        if (piece.isEmpty()) {
            throw new IllegalStateException("No piece at from-cell for move: " + move);
        }
        if (!piece.belongsTo(currentPlayer)) {
            throw new IllegalStateException("Piece does not belong to current player: " + move);
        }

        // Если идёт цепочка — ходить можно только той же шашкой.
        if (captureChainInProgress &&
                (fromRow != chainRow || fromCol != chainCol)) {
            throw new IllegalStateException(
                    "Capture chain in progress, move must start from (" +
                            chainRow + "," + chainCol + "), got " + move
            );
        }

        // Проверим, что ход легален (содержится в списке возможных).
        // Это защитная проверка от багов UI/ИИ.
        boolean found = false;
        for (Move legal : getAllMovesForCurrentPlayer()) {
            if (legal.equals(move)) {
                found = true;
                break;
            }
        }
        if (!found) {
            throw new IllegalArgumentException("Illegal move for current player: " + move);
        }

        // ---- Само применение хода ----

        // 1. убираем фигуру с исходной клетки
        board.setPiece(fromRow, fromCol, PieceType.EMPTY);

        // 2. если был захват — снимаем побитую фигуру
        if (move.isCapture()) {
            int cr = move.getCapturedRow();
            int cc = move.getCapturedCol();

            if (!board.isInside(cr, cc)) {
                throw new IllegalStateException(
                        "Captured cell is outside board for move: " + move
                );
            }
            PieceType captured = board.getPiece(cr, cc);
            if (captured.isEmpty() || captured.belongsTo(currentPlayer)) {
                // что-то пошло не так: побита пустая/своя фигура
                throw new IllegalStateException(
                        "Invalid captured piece for move: " + move +
                                ", captured=" + captured
                );
            }
            board.setPiece(cr, cc, PieceType.EMPTY);
        }

        // 3. ставим фигуру на новое место с учётом превращения в дамку
        PieceType newPiece = promoteIfNeeded(toRow, piece);
        board.setPiece(toRow, toCol, newPiece);

        // После изменения позиции кэш ходов устарел
        invalidateMovesCache();

        // 4. проверяем, может ли эта же фигура продолжать бой
        if (move.isCapture()) {
            List<Move> furtherMoves = getValidMovesForPiece(toRow, toCol, currentPlayer);
            List<Move> furtherCaptures = extractCaptures(furtherMoves);

            if (!furtherCaptures.isEmpty() && newPiece.belongsTo(currentPlayer)) {
                // продолжается цепочка взятий той же шашкой
                captureChainInProgress = true;
                chainRow = toRow;
                chainCol = toCol;
                mustCapture = true; // в рамках цепочки есть обязательное взятие

                // Обновим кэш ходов только на эти взятия
                currentMovesCache = Collections.unmodifiableList(furtherCaptures);

                return new MoveResult(true, null);
            }
        }

        // 5. Цепочка закончилась / был обычный ход — передаём ход сопернику
        captureChainInProgress = false;
        chainRow = -1;
        chainCol = -1;

        // Смена игрока
        currentPlayer = currentPlayer.opposite();

        // Пересчёт ходов для нового игрока
        recomputeCurrentMoves();

        // Если у нового игрока ходов нет — он проиграл, выигрывает тот, кто только что ходил
        if (currentMovesCache.isEmpty()) {
            Player winner = currentPlayer.opposite();
            return new MoveResult(false, winner);
        }

        return new MoveResult(false, null);
    }

    // ----------------------------------------------------------------------
    // Снапшоты (для Undo/ИИ)
    // ----------------------------------------------------------------------

    /**
     * Создать снапшот текущего состояния игры.
     * BoardState внутри снапшота — ГЛУБОКАЯ копия.
     */
    @NonNull
    public GameSnapshot createSnapshot() {
        BoardState copy = board.deepCopy();
        return new GameSnapshot(
                copy,
                currentPlayer,
                mustCapture,
                captureChainInProgress,
                chainRow,
                chainCol
        );
    }

    /**
     * Восстановить состояние игры из снапшота.
     * Текущая доска будет перезаписана содержимым boardCopy из снапшота.
     */
    public void restoreFromSnapshot(@NonNull GameSnapshot snapshot) {
        BoardState src = snapshot.getBoardCopy();
        int size = BoardState.BOARD_SIZE;

        // Копируем сырые коды во внутреннюю доску.
        int[][] raw = src.copyRaw();
        for (int r = 0; r < size; r++) {
            for (int c = 0; c < size; c++) {
                board.setCode(r, c, raw[r][c]);
            }
        }

        this.currentPlayer = snapshot.getCurrentPlayer();
        this.mustCapture = snapshot.isMustCapture();
        this.captureChainInProgress = snapshot.isCaptureChainInProgress();
        this.chainRow = snapshot.getChainRow();
        this.chainCol = snapshot.getChainCol();

        // кэш ходов устарел
        invalidateMovesCache();
        recomputeCurrentMoves();
    }

    // ----------------------------------------------------------------------
    // Внутренняя генерация ходов
    // ----------------------------------------------------------------------

    /**
     * Полный пересчёт списка ходов для currentPlayer
     * с учётом обязательного взятия и возможной цепочки.
     */
    private void recomputeCurrentMoves() {
        List<Move> moves;

        if (captureChainInProgress) {
            // Во время цепочки считаем ходы только для одной фигуры
            if (!board.isInside(chainRow, chainCol)) {
                currentMovesCache = Collections.emptyList();
                mustCapture = false;
                return;
            }

            List<Move> pieceMoves = getValidMovesForPiece(chainRow, chainCol, currentPlayer);
            List<Move> captures = extractCaptures(pieceMoves);

            // В цепочке всегда речь про взятия, но UI глобальное сообщение
            // показывает только когда !captureChainInProgress, поэтому здесь
            // это значение ни на что не влияет снаружи.
            mustCapture = !captures.isEmpty();
            moves = captures;

        } else {
            // Обычная ситуация — ищем ходы по всей доске
            List<Move> captureMoves = new ArrayList<>();
            List<Move> quietMoves = new ArrayList<>();

            int size = BoardState.BOARD_SIZE;
            for (int row = 0; row < size; row++) {
                for (int col = 0; col < size; col++) {
                    PieceType piece = board.getPiece(row, col);
                    if (!piece.belongsTo(currentPlayer)) {
                        continue;
                    }

                    List<Move> pieceMoves = getValidMovesForPiece(row, col, currentPlayer);
                    for (Move m : pieceMoves) {
                        if (m.isCapture()) {
                            captureMoves.add(m);
                        } else {
                            quietMoves.add(m);
                        }
                    }
                }
            }

            if (!captureMoves.isEmpty() && mustCaptureRuleEnabled) {
                // Правило включено: если есть хотя бы одно взятие — ходить обязаны только взятиями
                mustCapture = true;
                moves = captureMoves;
            } else {
                // Правило отключено или взятий нет:
                //  - mustCapture для UI = false;
                //  - если взятия есть, но правило выключено — разрешаем и взятия, и тихие
                mustCapture = false;

                if (!captureMoves.isEmpty()) {
                    List<Move> combined = new ArrayList<>(captureMoves.size() + quietMoves.size());
                    combined.addAll(captureMoves);
                    combined.addAll(quietMoves);
                    moves = combined;
                } else {
                    moves = quietMoves;
                }
            }
        }

        currentMovesCache = Collections.unmodifiableList(moves);
    }

    /**
     * Все допустимые ходы для конкретной фигуры (без учёта глобального обязательного взятия).
     * Здесь возвращаются и тихие, и ударные ходы; фильтрация по правилу делается выше.
     */
    @NonNull
    private List<Move> getValidMovesForPiece(int row, int col, @NonNull Player owner) {
        if (!board.isInside(row, col)) {
            return Collections.emptyList();
        }

        PieceType piece = board.getPiece(row, col);
        if (piece.isEmpty() || !piece.belongsTo(owner)) {
            return Collections.emptyList();
        }

        List<Move> captures = new ArrayList<>();
        List<Move> quiet = new ArrayList<>();

        if (piece.isKing()) {
            collectKingMoves(row, col, owner, captures, quiet);
        } else {
            collectManMoves(row, col, piece, owner, captures, quiet);
        }

        if (captures.isEmpty() && quiet.isEmpty()) {
            return Collections.emptyList();
        }

        if (quiet.isEmpty()) {
            return Collections.unmodifiableList(captures);
        }
        if (captures.isEmpty()) {
            return Collections.unmodifiableList(quiet);
        }

        List<Move> all = new ArrayList<>(captures.size() + quiet.size());
        all.addAll(captures);
        all.addAll(quiet);
        return Collections.unmodifiableList(all);
    }

    /**
     * Сбор ходов для обычной шашки (ман).
     * В русских шашках:
     * - обычный ход только вперёд;
     * - взятие возможно во всех диагональных направлениях.
     * Эта функция всегда заполняет и captures, и quiet (если такие ходы есть),
     * а не принимает решение, что из этого разрешено.
     */
    private void collectManMoves(int row,
                                 int col,
                                 @NonNull PieceType piece,
                                 @NonNull Player owner,
                                 @NonNull List<Move> captures,
                                 @NonNull List<Move> quiet) {

        // Защитная проверка: сюда должны попадать только свои "маны"
        if (!piece.isMan() || !piece.belongsTo(owner)) {
            throw new IllegalArgumentException(
                    "collectManMoves called with invalid piece: " + piece +
                            ", owner=" + owner + " at (" + row + "," + col + ")"
            );
        }

        int[] dirs = {-1, 1};

        // --- сначала ищем все возможные взятия во всех диагоналях ---
        for (int dr : dirs) {
            for (int dc : dirs) {
                int midRow = row + dr;
                int midCol = col + dc;
                int landingRow = row + 2 * dr;
                int landingCol = col + 2 * dc;

                if (!board.isInside(midRow, midCol) || !board.isInside(landingRow, landingCol)) {
                    continue;
                }

                PieceType midPiece = board.getPiece(midRow, midCol);
                if (!isOpponentPiece(midPiece, owner)) {
                    continue;
                }
                if (!board.getPiece(landingRow, landingCol).isEmpty()) {
                    continue;
                }

                captures.add(new Move(row, col, landingRow, landingCol, midRow, midCol));
            }
        }

        // --- тихие ходы: только вперёд по диагонали ---
        int forwardDir = (owner == Player.WHITE) ? -1 : 1;
        for (int dc : dirs) {
            int nr = row + forwardDir;
            int nc = col + dc;
            if (!board.isInside(nr, nc)) continue;
            if (board.getPiece(nr, nc).isEmpty()) {
                quiet.add(new Move(row, col, nr, nc));
            }
        }
    }

    /**
     * Рекурсивно вычисляет максимальное количество дополнительных взятий
     * для дамки owner, стоящей в (row, col) на доске b.
     * Возвращает число побитых шашек, не считая текущего хода.
     */
    private int maxAdditionalKingCaptures(@NonNull BoardState b,
                                          int row,
                                          int col,
                                          @NonNull Player owner) {

        int[] dirs = {-1, 1};
        final PieceType kingPiece =
                (owner == Player.WHITE) ? PieceType.WHITE_KING : PieceType.BLACK_KING;

        int best = 0;

        for (int dr : dirs) {
            for (int dc : dirs) {
                boolean opponentFound = false;
                int opponentRow = -1;
                int opponentCol = -1;

                int step = 1;
                while (true) {
                    int r = row + dr * step;
                    int c = col + dc * step;
                    if (!b.isInside(r, c)) {
                        break;
                    }

                    PieceType cellPiece = b.getPiece(r, c);

                    if (!opponentFound) {
                        if (cellPiece.isEmpty()) {
                            step++;
                            continue;
                        }
                        if (cellPiece.belongsTo(owner)) {
                            // своя фигура — в этом направлении удара нет
                            break;
                        }
                        // первая встреченная чужая шашка
                        opponentFound = true;
                        opponentRow = r;
                        opponentCol = c;
                        step++;
                        continue;
                    }

                    // после побитой шашки — возможные клетки приземления
                    if (!cellPiece.isEmpty()) {
                        // упёрлись во вторую фигуру/край — дальше в этом направлении нельзя
                        break;
                    }

                    // r, c — кандидат для приземления
                    BoardState tmp = b.deepCopy();
                    tmp.setPiece(row, col, PieceType.EMPTY);
                    tmp.setPiece(opponentRow, opponentCol, PieceType.EMPTY);
                    tmp.setPiece(r, c, kingPiece);

                    int further = maxAdditionalKingCaptures(tmp, r, c, owner);
                    int total = 1 + further; // побитая сейчас + дальше по цепочке

                    if (total > best) {
                        best = total;
                    }

                    step++;
                }
            }
        }

        return best;
    }

    /**
     * Сбор ходов для дамки.
     * Внутри одной ветки (одна и та же первая побитая шашка) оставляем
     * только те приземления, которые дают максимальное количество взятий
     * по этой ветке. Между разными ветками выбора не ограничиваем.
     */
    private void collectKingMoves(int row,
                                  int col,
                                  @NonNull Player owner,
                                  @NonNull List<Move> captures,
                                  @NonNull List<Move> quiet) {

        int[] dirs = {-1, 1};
        final PieceType kingPiece =
                (owner == Player.WHITE) ? PieceType.WHITE_KING : PieceType.BLACK_KING;

        // --- сначала собираем ударные ходы ---
        for (int dr : dirs) {
            for (int dc : dirs) {

                boolean opponentFound = false;
                int opponentRow = -1;
                int opponentCol = -1;

                int step = 1;

                // для этой диагонали (одной "первой побитой шашки")
                // запоминаем только лучшие по длине варианты
                int bestCaptureCountForDirection = 0;
                List<Move> bestMovesForDirection = new ArrayList<>();

                while (true) {
                    int r = row + dr * step;
                    int c = col + dc * step;
                    if (!board.isInside(r, c)) {
                        break;
                    }

                    PieceType cellPiece = board.getPiece(r, c);

                    if (!opponentFound) {
                        if (cellPiece.isEmpty()) {
                            step++;
                            continue;
                        }
                        if (cellPiece.belongsTo(owner)) {
                            // своя фигура — в этом направлении удара нет
                            break;
                        }
                        // первая встреченная чужая шашка
                        opponentFound = true;
                        opponentRow = r;
                        opponentCol = c;
                        step++;
                        continue;
                    }

                    // после побитой шашки — возможные клетки приземления
                    if (!cellPiece.isEmpty()) {
                        // упёрлись во вторую фигуру/край — дальше в этом направлении
                        // приземляться нельзя
                        break;
                    }

                    // r, c — пустая клетка-кандидат для приземления
                    BoardState tmp = board.deepCopy();
                    tmp.setPiece(row, col, PieceType.EMPTY);
                    tmp.setPiece(opponentRow, opponentCol, PieceType.EMPTY);
                    tmp.setPiece(r, c, kingPiece);

                    int furtherCaptures = maxAdditionalKingCaptures(tmp, r, c, owner);
                    int totalCaptures = 1 + furtherCaptures;

                    if (totalCaptures > bestCaptureCountForDirection) {
                        bestCaptureCountForDirection = totalCaptures;
                        bestMovesForDirection.clear();
                        bestMovesForDirection.add(
                                new Move(row, col, r, c, opponentRow, opponentCol)
                        );
                    } else if (totalCaptures == bestCaptureCountForDirection) {
                        bestMovesForDirection.add(
                                new Move(row, col, r, c, opponentRow, opponentCol)
                        );
                    }

                    step++;
                }

                // добавляем лучшие ходы по этой диагонали (если они есть)
                if (bestCaptureCountForDirection > 0) {
                    captures.addAll(bestMovesForDirection);
                }
            }
        }

        // --- тихие ходы дамки ---
        // Они нужны, чтобы при отсутствии взятий логика выше могла
        // вернуть пустой список captures, а quiet всё равно были доступны.
        for (int dr : dirs) {
            for (int dc : dirs) {
                int step = 1;
                while (true) {
                    int r = row + dr * step;
                    int c = col + dc * step;
                    if (!board.isInside(r, c)) {
                        break;
                    }
                    if (!board.getPiece(r, c).isEmpty()) {
                        break;
                    }
                    quiet.add(new Move(row, col, r, c));
                    step++;
                }
            }
        }
    }

    // ----------------------------------------------------------------------
    // Вспомогательные методы
    // ----------------------------------------------------------------------

    /** Сбросить кэш ходов. */
    private void invalidateMovesCache() {
        currentMovesCache = Collections.emptyList();
        mustCapture = false;
    }

    /** Отфильтровать только ударные ходы. */
    @NonNull
    private static List<Move> extractCaptures(@NonNull List<Move> moves) {
        if (moves.isEmpty()) return Collections.emptyList();
        List<Move> result = new ArrayList<>();
        for (Move m : moves) {
            if (m.isCapture()) {
                result.add(m);
            }
        }
        if (result.isEmpty()) {
            return Collections.emptyList();
        }
        return result;
    }

    /** Является ли фигура фигурой соперника данного игрока. */
    private static boolean isOpponentPiece(@NonNull PieceType piece, @NonNull Player player) {
        if (piece.isEmpty()) return false;
        return (player == Player.WHITE && piece.isBlack())
                || (player == Player.BLACK && piece.isWhite());
    }

    /**
     * Превращение шашки в дамку (в русских шашках — при достижении последней горизонтали).
     */
    @NonNull
    private PieceType promoteIfNeeded(int row, @NonNull PieceType piece) {
        if (piece == PieceType.WHITE_MAN && row == 0) {
            return PieceType.WHITE_KING;
        }
        if (piece == PieceType.BLACK_MAN && row == BoardState.BOARD_SIZE - 1) {
            return PieceType.BLACK_KING;
        }
        return piece;
    }

}
