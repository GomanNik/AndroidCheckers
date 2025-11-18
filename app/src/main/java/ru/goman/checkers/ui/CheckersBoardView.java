package ru.goman.checkers.ui;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;

import android.view.View;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.util.List;

import ru.goman.checkers.R;
import ru.goman.checkers.model.BoardState;
import ru.goman.checkers.model.Move;
import ru.goman.checkers.model.PieceType;

public class CheckersBoardView extends View {

    private boolean humanIsWhite = true;


    // Размер доски берём из модели
    public static final int BOARD_SIZE = BoardState.BOARD_SIZE;

    @SuppressWarnings("unused")
    public enum PieceColor {
        NONE,
        WHITE,
        BLACK
    }

    // Клетки и рамка
    private final Paint paintLightSquare = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintDarkSquare = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintBoardBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintBoardOuter = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintCoordText = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Базовые заливки шашек
    private final Paint paintWhitePiece = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintBlackPiece = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintBlackPieceInner = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Контуры/тени/индикаторы
    private final Paint paintPieceStroke = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintPieceShadow = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintKingInner = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintMoveHint = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Обод и «канавки»
    private final Paint paintRimWhite = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintRimBlack = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintGrooveWhite = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintGrooveBlack = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintGradient = new Paint(Paint.ANTI_ALIAS_FLAG);

    // Прямоугольник внутренней доски и размер клетки
    private final RectF boardRect = new RectF();
    private final RectF outerRect = new RectF();
    private Shader darkSquareShader;
    private final android.graphics.Matrix darkSquareMatrix = new android.graphics.Matrix();
    private float shaderCellSize = -1f;

    private float cellSize = 0f;

    // Визуальная модель доски
    private final PieceType[][] board = new PieceType[BOARD_SIZE][BOARD_SIZE];

    // Выделение шашки
    private int selectedRow = -1;
    private int selectedCol = -1;
    private float selectionScale = 1f;
    private ValueAnimator selectionAnimator;

    // Анимация хода одной шашки
    private boolean moveAnimating = false;
    private int animFromRow = -1;
    private int animFromCol = -1;
    private float animFromCx, animFromCy;
    private float animToCx, animToCy;
    private float animProgress = 0f;
    private PieceType animPiece = PieceType.EMPTY;
    private ValueAnimator moveAnimator;

    // Подсказки ходов
    private final boolean[][] moveHints = new boolean[BOARD_SIZE][BOARD_SIZE];

    // Листенер клика по клетке
    public interface OnCellClickListener {
        void onCellClick(int row, int col);
    }

    @Nullable
    private OnCellClickListener cellClickListener;

    public CheckersBoardView(Context context) {
        this(context, null);
    }

    public CheckersBoardView(Context context, @Nullable AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CheckersBoardView(Context context,
                             @Nullable AttributeSet attrs,
                             int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // фон самой вьюхи — прозрачный, чтобы углы показывали фон экрана
        setBackgroundColor(Color.TRANSPARENT);

        // Светлые клетки — чисто белые
        paintLightSquare.setColor(0xFFFFFFFF);
        // Базовый цвет тёмной клетки (если вдруг без градиента)
        paintDarkSquare.setColor(0xFF777777);

        // Внешняя чёрная рамка
        paintBoardOuter.setStyle(Paint.Style.FILL);
        paintBoardOuter.setColor(0xFF000000);

        // Тонкая светлая обводка внутреннего поля
        paintBoardBorder.setStyle(Paint.Style.STROKE);
        paintBoardBorder.setStrokeWidth(2f);
        paintBoardBorder.setColor(0xFFFFFFFF);

        // Подписи координат
        paintCoordText.setStyle(Paint.Style.FILL);
        paintCoordText.setColor(0xFFFFFFFF);
        paintCoordText.setTextAlign(Paint.Align.CENTER);

        // Цвета шашек
        paintWhitePiece.setColor(0xFFE4E7F0);          // пока белую не трогаем
        paintBlackPiece.setColor(0xFF141821);          // основной диск тёмной шашки

        paintBlackPieceInner.setStyle(Paint.Style.FILL);
        paintBlackPieceInner.setColor(0xFF39445A);     // центральная зона тёмной шашки

        // Обводка шашек
        paintPieceStroke.setStyle(Paint.Style.STROKE);
        paintPieceStroke.setStrokeWidth(2.5f);
        paintPieceStroke.setColor(0xCC000000);

        // Тень под шашкой
        paintPieceShadow.setColor(0x44000000);
        paintPieceShadow.setStyle(Paint.Style.FILL);

        // Обод
        paintRimWhite.setStyle(Paint.Style.STROKE);
        paintRimBlack.setStyle(Paint.Style.STROKE);
        paintRimWhite.setColor(0xFFBFC4D4);
        paintRimBlack.setColor(0xFF1E2533);           // внешнее кольцо тёмной шашки

        // «Канавки»
        paintGrooveWhite.setStyle(Paint.Style.STROKE);
        paintGrooveBlack.setStyle(Paint.Style.STROKE);
        paintGrooveWhite.setColor(0xFFB0B4C4);
        paintGrooveBlack.setColor(0xFF2A3244);        // второе кольцо тёмной шашки

        // Золото для дамки
        paintKingInner.setStyle(Paint.Style.STROKE);
        paintKingInner.setStrokeWidth(4f);
        paintKingInner.setColor(0xFFFFD54F);

        // Подсказки ходов
        paintMoveHint.setStyle(Paint.Style.FILL);
        paintMoveHint.setColor(0xFF202437);

        // Кисть под градиенты
        paintGradient.setStyle(Paint.Style.FILL);

        clearBoardModel();
        resetBoardPosition();
    }

    private void clearBoardModel() {
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                board[r][c] = PieceType.EMPTY;
            }
        }
    }

    public void setBoardState(@NonNull BoardState state) {
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                board[r][c] = state.getPiece(r, c);
            }
        }
        invalidate();
    }

    public void resetBoardPosition() {
        BoardState state = new BoardState();
        state.setupInitialPosition();
        setBoardState(state);
        clearSelection();
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        if (width <= 0 || height <= 0) {
            return;
        }

        float viewSize = Math.min(width, height);

        // без отступов по краям
        float outerPadding = 0f;

        float boardSize = viewSize - 2f * outerPadding;
        if (boardSize <= 0f) {
            return;
        }

        float boardLeft = (width - boardSize) / 2f;
        float boardTop = (height - boardSize) / 2f;
        float boardRight = boardLeft + boardSize;
        float boardBottom = boardTop + boardSize;

        // внешняя доска со скруглёнными краями
        outerRect.set(boardLeft, boardTop, boardRight, boardBottom);
        float cornerRadius = boardSize * 0.04f;
        canvas.drawRoundRect(outerRect, cornerRadius, cornerRadius, paintBoardOuter);

        // внутренний квадрат под координаты и 8×8 клетки
        float framePadding = 0f;
        float innerLeft = outerRect.left + framePadding;
        float innerTop = outerRect.top + framePadding;
        float innerRight = outerRect.right - framePadding;
        float innerBottom = outerRect.bottom - framePadding;

        float innerSize = Math.min(innerRight - innerLeft, innerBottom - innerTop);
        float innerCx = (innerLeft + innerRight) / 2f;
        float innerCy = (innerTop + innerBottom) / 2f;
        innerLeft = innerCx - innerSize / 2f;
        innerTop = innerCy - innerSize / 2f;
        innerRight = innerCx + innerSize / 2f;
        innerBottom = innerCy + innerSize / 2f;

        // толщина полосы под подписи (в долях размера клетки)
        final float labelFactor = 0.4f;

        float totalUnits = BOARD_SIZE + 2f * labelFactor;
        cellSize = innerSize / totalUnits;
        float labelMargin = cellSize * labelFactor;

        boardRect.set(
                innerLeft + labelMargin,
                innerTop + labelMargin,
                innerRight - labelMargin,
                innerBottom - labelMargin
        );

        // подготовим шейдер для тёмных клеток (один на весь draw)
        if (darkSquareShader == null || shaderCellSize != cellSize) {
            darkSquareShader = new LinearGradient(
                    0f, 0f,
                    cellSize, cellSize,
                    0xFFB3B3B3, // светлый угол
                    0xFF505050, // тёмный угол
                    Shader.TileMode.CLAMP
            );
            shaderCellSize = cellSize;
        }

        // клетки 8×8
        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                boolean dark = ((row + col) & 1) == 1;

                float x1 = boardRect.left + col * cellSize;
                float y1 = boardRect.top + row * cellSize;
                float x2 = x1 + cellSize;
                float y2 = y1 + cellSize;

                if (dark) {
                    darkSquareMatrix.reset();
                    darkSquareMatrix.setTranslate(x1, y1);
                    darkSquareShader.setLocalMatrix(darkSquareMatrix);

                    paintDarkSquare.setShader(darkSquareShader);
                    canvas.drawRect(x1, y1, x2, y2, paintDarkSquare);
                } else {
                    paintLightSquare.setShader(null);
                    canvas.drawRect(x1, y1, x2, y2, paintLightSquare);
                }
            }
        }

        // подсказки ходов
        drawMoveHints(canvas);

        // подписи координат
        drawCoordinates(canvas, labelMargin);

        // шашки
        drawPieces(canvas);
    }

    private void drawCoordinates(@NonNull Canvas canvas, float labelMargin) {
        if (cellSize <= 0f) return;

        float textSize = cellSize * 0.35f;
        paintCoordText.setTextSize(textSize);

        float textHeight = paintCoordText.descent() - paintCoordText.ascent();
        float textOffset = textHeight / 2f - paintCoordText.descent();

        // Буквы по горизонтали (A–H снизу, H–A сверху)
        for (int col = 0; col < BOARD_SIZE; col++) {
            char bottomChar = (char) ('A' + col);
            char topChar = (char) ('A' + (BOARD_SIZE - 1 - col));

            float cx = boardRect.left + (col + 0.5f) * cellSize;

            float bottomY = boardRect.bottom + labelMargin / 2f + textOffset;
            float topY = boardRect.top - labelMargin / 2f + textOffset;

            canvas.drawText(String.valueOf(bottomChar), cx, bottomY, paintCoordText);
            canvas.drawText(String.valueOf(topChar), cx, topY, paintCoordText);
        }

        // Цифры по вертикали: слева 8→1, справа 1→8
        for (int row = 0; row < BOARD_SIZE; row++) {
            int leftRank = BOARD_SIZE - row; // 8..1 сверху вниз
            int rightRank = row + 1;        // 1..8 сверху вниз

            float cy = boardRect.top + (row + 0.5f) * cellSize;
            float y = cy + textOffset;

            float leftX = boardRect.left - labelMargin / 2f;
            float rightX = boardRect.right + labelMargin / 2f;

            canvas.drawText(String.valueOf(leftRank), leftX, y, paintCoordText);
            canvas.drawText(String.valueOf(rightRank), rightX, y, paintCoordText);
        }
    }

    private void drawPieces(@NonNull Canvas canvas) {
        if (cellSize <= 0f) return;

        float baseRadius = cellSize * 0.38f;
        float shadowOffset = cellSize * 0.06f;

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                PieceType piece = board[row][col];
                if (piece == null || piece.isEmpty()) continue;

                // во время анимации не рисуем шашку на исходной клетке —
                // её рисует отдельный анимированный слой
                if (moveAnimating
                        && row == animFromRow
                        && col == animFromCol
                        && piece == animPiece) {
                    continue;
                }

                float cx = boardRect.left + col * cellSize + cellSize / 2f;
                float cy = boardRect.top + row * cellSize + cellSize / 2f;

                float scale = 1f;
                if (row == selectedRow && col == selectedCol) {
                    scale = selectionScale;
                }

                float radius = baseRadius * scale;

                // Тень
                canvas.drawCircle(cx + shadowOffset, cy + shadowOffset,
                        radius * 0.95f, paintPieceShadow);

                // Стилизованная фишка
                drawStyledPiece(canvas, cx, cy, radius, piece.isWhite(), piece.isKing());
            }
        }

        // Отдельно рисуем "летящую" шашку
        if (moveAnimating && animPiece != null && !animPiece.isEmpty()) {
            float cx = animFromCx + (animToCx - animFromCx) * animProgress;
            float cy = animFromCy + (animToCy - animFromCy) * animProgress;

            canvas.drawCircle(cx + shadowOffset, cy + shadowOffset,
                    baseRadius * 0.95f, paintPieceShadow);

            drawStyledPiece(canvas, cx, cy, baseRadius,
                    animPiece.isWhite(), animPiece.isKing());
        }
    }
    public void animatePieceMove(int fromRow,
                                 int fromCol,
                                 int toRow,
                                 int toCol,
                                 @NonNull PieceType piece,
                                 @Nullable Runnable onAnimationEnd) {

        // если ещё не успели посчитать размеры — просто без анимации
        if (cellSize <= 0f) {
            if (onAnimationEnd != null) {
                onAnimationEnd.run();
            }
            return;
        }

        // координаты центров клеток
        animFromCx = boardRect.left + fromCol * cellSize + cellSize / 2f;
        animFromCy = boardRect.top + fromRow * cellSize + cellSize / 2f;
        animToCx   = boardRect.left + toCol   * cellSize + cellSize / 2f;
        animToCy   = boardRect.top + toRow   * cellSize + cellSize / 2f;

        animFromRow = fromRow;
        animFromCol = fromCol;
        animPiece   = piece;

        // если анимация уже шла — отменяем
        if (moveAnimator != null) {
            moveAnimator.cancel();
            moveAnimator = null;
        }

        moveAnimating  = true;
        animProgress   = 0f;

        moveAnimator = ValueAnimator.ofFloat(0f, 1f);
        // чуть дольше и плавнее
        moveAnimator.setDuration(260); // мс
        moveAnimator.setInterpolator(
                new android.view.animation.AccelerateDecelerateInterpolator()
        );

        moveAnimator.addUpdateListener(a -> {
            animProgress = (float) a.getAnimatedValue();
            invalidate();
        });
        moveAnimator.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                moveAnimating = false;
                animPiece = PieceType.EMPTY;
                moveAnimator = null;
                invalidate();

                if (onAnimationEnd != null) {
                    onAnimationEnd.run();
                }
            }

            @Override
            public void onAnimationCancel(Animator animation) {
                moveAnimating = false;
                animPiece = PieceType.EMPTY;
                moveAnimator = null;
                invalidate();
            }
        });

        moveAnimator.start();
    }

    private void drawMoveHints(@NonNull Canvas canvas) {
        if (cellSize <= 0f) return;

        float hintRadius = cellSize * 0.18f;

        for (int row = 0; row < BOARD_SIZE; row++) {
            for (int col = 0; col < BOARD_SIZE; col++) {
                if (!moveHints[row][col]) continue;

                float cx = boardRect.left + col * cellSize + cellSize / 2f;
                float cy = boardRect.top + row * cellSize + cellSize / 2f;

                canvas.drawCircle(cx, cy, hintRadius, paintMoveHint);
            }
        }
    }

    private void drawStyledPiece(@NonNull Canvas canvas,
                                 float cx,
                                 float cy,
                                 float radius,
                                 boolean logicalWhite,
                                 boolean isKing) {

        // Преобразуем логический цвет (из модели) в визуальный цвет для игрока
        // человек "как белый" — логика == визуал
        boolean drawAsWhite = humanIsWhite == logicalWhite;    // человек "как чёрный" — инвертируем цвета

        // 1. Тень под шашкой
        float shadowOffset = radius * 0.18f;
        canvas.drawOval(
                cx - radius + shadowOffset,
                cy - radius + shadowOffset,
                cx + radius + shadowOffset,
                cy + radius + shadowOffset,
                paintPieceShadow
        );

        // 2. Основной диск шашки
        Paint fill = drawAsWhite ? paintWhitePiece : paintBlackPiece;
        fill.setStyle(Paint.Style.FILL);
        canvas.drawCircle(cx, cy, radius, fill);

        // 3. Толстый внешний контур (общий для белых и чёрных)
        paintPieceStroke.setStyle(Paint.Style.STROKE);
        paintPieceStroke.setColor(0xFF060708);                // почти чёрный графит
        paintPieceStroke.setStrokeWidth(radius * 0.16f);
        canvas.drawCircle(
                cx,
                cy,
                radius - paintPieceStroke.getStrokeWidth() * 0.5f,
                paintPieceStroke
        );

        // 4. Внутренние кольца
        if (drawAsWhite) {
            // визуально "светлая" шашка
            paintRimWhite.setStrokeWidth(radius * 0.09f);
            canvas.drawCircle(cx, cy, radius * 0.72f, paintRimWhite);

            paintGrooveWhite.setStrokeWidth(radius * 0.06f);
            canvas.drawCircle(cx, cy, radius * 0.52f, paintGrooveWhite);
        } else {
            // визуально "тёмная" шашка
            paintRimBlack.setStrokeWidth(radius * 0.09f);
            canvas.drawCircle(cx, cy, radius * 0.72f, paintRimBlack);

            paintGrooveBlack.setStrokeWidth(radius * 0.06f);
            canvas.drawCircle(cx, cy, radius * 0.52f, paintGrooveBlack);

            // центральная зона
            canvas.drawCircle(cx, cy, radius * 0.38f, paintBlackPieceInner);
        }

        // 5. Корона для дамки — готовая иконка
        if (isKing) {
            android.graphics.drawable.Drawable crown =
                    androidx.appcompat.content.res.AppCompatResources.getDrawable(
                            getContext(), R.drawable.ic_crown
                    );

            if (crown != null) {
                int size = (int) (radius * 1.2f);
                int left  = (int) (cx - size / 2f);
                int top   = (int) (cy - size / 2f);
                int right = left + size;
                int bottom = top + size;

                crown.setBounds(left, top, right, bottom);

                int crownTint = drawAsWhite ? 0xFF000000 : 0xFFFFFFFF;
                crown.setTint(crownTint);

                crown.draw(canvas);
            }
        }
    }

    @SuppressWarnings("unused")
    public PieceColor getPieceColor(int row, int col) {
        if (!isInside(row, col)) return PieceColor.NONE;
        PieceType p = board[row][col];
        if (p == null || p.isEmpty()) return PieceColor.NONE;
        if (p.isWhite()) return PieceColor.WHITE;
        if (p.isBlack()) return PieceColor.BLACK;
        return PieceColor.NONE;
    }

    private boolean isInside(int row, int col) {
        return row >= 0 && row < BOARD_SIZE
                && col >= 0 && col < BOARD_SIZE;
    }

    public void selectPiece(int row, int col, boolean animate) {
        if (!isInside(row, col) || board[row][col] == null || board[row][col].isEmpty()) {
            clearSelection();
            return;
        }

        selectedRow = row;
        selectedCol = col;

        if (selectionAnimator != null) {
            selectionAnimator.cancel();
        }

        if (animate) {
            selectionAnimator = ValueAnimator.ofFloat(1f, 1.12f);
            selectionAnimator.setDuration(150);
            selectionAnimator.setRepeatMode(ValueAnimator.REVERSE);
            selectionAnimator.setRepeatCount(1);
            selectionAnimator.addUpdateListener(animator -> {
                selectionScale = (float) animator.getAnimatedValue();
                invalidate();
            });
            selectionAnimator.start();
        } else {
            selectionScale = 1f;
            invalidate();
        }
    }

    public void clearSelection() {
        selectedRow = -1;
        selectedCol = -1;
        selectionScale = 1f;
        if (selectionAnimator != null) {
            selectionAnimator.cancel();
            selectionAnimator = null;
        }
        clearMoveHintsInternal();
        invalidate();
    }

    public void showMoveHints(@NonNull List<Move> moves, boolean enabled) {
        clearMoveHintsInternal();

        if (!enabled) {
            invalidate();
            return;
        }

        for (Move m : moves) {
            int r = m.getToRow();
            int c = m.getToCol();
            if (isInside(r, c)) {
                moveHints[r][c] = true;
            }
        }
        invalidate();
    }


    public void clearMoveHints() {
        clearMoveHintsInternal();
        invalidate();
    }

    private void clearMoveHintsInternal() {
        for (int r = 0; r < BOARD_SIZE; r++) {
            for (int c = 0; c < BOARD_SIZE; c++) {
                moveHints[r][c] = false;
            }
        }
    }

    public void setOnCellClickListener(@Nullable OnCellClickListener listener) {
        this.cellClickListener = listener;
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public boolean onTouchEvent(@NonNull MotionEvent event) {
        if (event.getAction() == MotionEvent.ACTION_DOWN) {
            float x = event.getX();
            float y = event.getY();

            if (!boardRect.contains(x, y) || cellSize <= 0f) {
                return false;
            }

            int col = (int) ((x - boardRect.left) / cellSize);
            int row = (int) ((y - boardRect.top) / cellSize);

            if (!isInside(row, col)) {
                return false;
            }

            if (cellClickListener != null) {
                cellClickListener.onCellClick(row, col);
            }

            return true;
        }

        return super.onTouchEvent(event);
    }

    public void setHumanIsWhite(boolean humanIsWhite) {
        this.humanIsWhite = humanIsWhite;
        invalidate();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (selectionAnimator != null) {
            selectionAnimator.cancel();
            selectionAnimator = null;
        }
    }
}
