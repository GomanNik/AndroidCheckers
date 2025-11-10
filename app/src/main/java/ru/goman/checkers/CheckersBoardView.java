package ru.goman.checkers;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

public class CheckersBoardView extends View {

    private final Paint paintLight = new Paint();
    private final Paint paintDark = new Paint();

    public CheckersBoardView(Context context) {
        this(context, null);
    }

    public CheckersBoardView(Context context, AttributeSet attrs) {
        this(context, attrs, 0);
    }

    public CheckersBoardView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        // Цвета клеток (можно потом вынести в ресурсы)
        paintLight.setColor(0xFFEED0A4); // светлые клетки
        paintDark.setColor(0xFFB07C4F);  // тёмные клетки
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);

        int width = getWidth();
        int height = getHeight();

        int size = Math.min(width, height);
        float cellSize = size / 8f;

        float left = (width - size) / 2f;
        float top = (height - size) / 2f;

        for (int row = 0; row < 8; row++) {
            for (int col = 0; col < 8; col++) {
                boolean dark = (row + col) % 2 == 1;
                float x1 = left + col * cellSize;
                float y1 = top + row * cellSize;
                float x2 = x1 + cellSize;
                float y2 = y1 + cellSize;

                canvas.drawRect(x1, y1, x2, y2, dark ? paintDark : paintLight);
            }
        }
    }
}
