package com.rogger.bp.ui.home;

import android.annotation.SuppressLint;
import android.graphics.Matrix;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.ScaleGestureDetector;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.bumptech.glide.Glide;
import com.rogger.bp.R;

public class ShowFragment extends Fragment {

    private static final String ARG_IMAGE_URI = "imageUri";

    private ImageView imageView;
    private Matrix matrix = new Matrix();
    private float scale = 1f;

    private ScaleGestureDetector scaleDetector;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.fragment_show, container, false);
    }

    @SuppressLint("ClickableViewAccessibility")
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imageView = view.findViewById(R.id.imgPreview);
        // 🔥 Correção para Android 11: Desativa aceleração de hardware para evitar erro de Canvas muito grande
        imageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
        imageView.setImageMatrix(matrix);

        String uriString = getArguments() != null
                ? getArguments().getString(ARG_IMAGE_URI)
                : null;

        Glide.with(requireContext())
                .asBitmap()
                .load(uriString)
                .override(1024, 1024)
                .format(com.bumptech.glide.load.DecodeFormat.PREFER_RGB_565) // Economiza 50% de memória
                .fitCenter()
                .placeholder(R.drawable.carregando)
                .error(R.drawable.imagem_error)
                .into(imageView);
        scaleDetector = new ScaleGestureDetector(requireContext(), new ScaleGestureDetector.SimpleOnScaleGestureListener() {
            @Override
            public boolean onScale(ScaleGestureDetector detector) {
                scale *= detector.getScaleFactor();
                scale = Math.max(1f, Math.min(scale, 5f));

                matrix.setScale(
                        scale,
                        scale,
                        detector.getFocusX(),
                        detector.getFocusY()
                );
                imageView.setImageMatrix(matrix);
                return true;
            }
        });

        imageView.setOnTouchListener(new View.OnTouchListener() {
            float lastX, lastY;

            @Override
            public boolean onTouch(View v, MotionEvent event) {
                scaleDetector.onTouchEvent(event);

                if (!scaleDetector.isInProgress()) {
                    switch (event.getAction()) {
                        case MotionEvent.ACTION_DOWN:
                            lastX = event.getX();
                            lastY = event.getY();
                            break;

                        case MotionEvent.ACTION_MOVE:
                            float dx = event.getX() - lastX;
                            float dy = event.getY() - lastY;
                            matrix.postTranslate(dx, dy);
                            imageView.setImageMatrix(matrix);
                            lastX = event.getX();
                            lastY = event.getY();
                            break;
                    }
                }
                return true;
            }
        });
    }
}
