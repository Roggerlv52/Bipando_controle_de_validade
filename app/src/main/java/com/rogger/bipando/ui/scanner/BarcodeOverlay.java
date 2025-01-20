package com.rogger.bipando.ui.scanner;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.NonNull;

public class BarcodeOverlay extends View {
    private Paint paint;
    public BarcodeOverlay(Context context) {
        super(context);
        init();
    }
    public BarcodeOverlay(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }
    public BarcodeOverlay(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }
    private void init() {
        paint = new Paint();
        paint.setColor(Color.argb(150, 0, 0, 0)); // Cor preta translúcida
        paint.setStyle(Paint.Style.FILL);
    }

    @Override
    protected void onDraw(@NonNull Canvas canvas) {
        super.onDraw(canvas);
        int width = getWidth();
        int height = getHeight();
        // Defina as dimensões do retângulo central
        float rectWidth = width * 0.8f;
        float rectHeight = height * 0.3f;
        float left = (width - rectWidth) / 2;
        float top = (height - rectHeight) / 2;
        float right = left + rectWidth;
        float bottom = top + rectHeight;

        // Desenhe o fundo escuro
        canvas.drawRect(0, 0, width, top, paint);
        canvas.drawRect(0, bottom, width, height, paint);
        canvas.drawRect(0, top, left, bottom, paint);
        canvas.drawRect(right, top, width, bottom, paint);

        // Mudar a cor para branco e desenhar as bordas do retângulo central
        paint.setColor(Color.WHITE);
        paint.setStyle(Paint.Style.STROKE);
        paint.setStrokeWidth(5f);
        canvas.drawRect(left, top, right, bottom, paint);
    }
}
