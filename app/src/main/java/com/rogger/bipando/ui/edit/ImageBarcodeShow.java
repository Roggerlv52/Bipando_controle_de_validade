package com.rogger.bipando.ui.edit;

import android.graphics.Bitmap;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.common.BitMatrix;
import com.rogger.bipando.R;

public class ImageBarcodeShow extends Fragment {

    private ImageView imgBarcode;
    private TextView txtBarcode;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        return inflater.inflate(R.layout.image_barcode_show, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        imgBarcode = view.findViewById(R.id.img_barcode);
        txtBarcode = view.findViewById(R.id.txt_barcode_number);

        if (getArguments() != null) {
            String barcode = getArguments().getString("barcode");

            if (barcode != null && !barcode.isEmpty()) {
                txtBarcode.setText(barcode);
                gerarCodigoDeBarras(barcode);
            }
        }
    }

    private void gerarCodigoDeBarras(String barcode) {
        try {
            if (!barcode.matches("\\d+")) {
                throw new IllegalArgumentException("Barcode deve conter apenas números");
            }

            BarcodeFormat format;

            int length = barcode.length();

            if (length == 8) {
                format = BarcodeFormat.EAN_8;
            } else if (length == 12 || length == 13) {
                format = BarcodeFormat.EAN_13;
            } else {
                // 🔥 Qualquer outra sequência numérica
                format = BarcodeFormat.CODE_128;
            }

            BitMatrix bitMatrix = new MultiFormatWriter().encode(
                    barcode,
                    format,
                    1000,
                    400,
                    null
            );

            Bitmap bitmap = Bitmap.createBitmap(
                    bitMatrix.getWidth(),
                    bitMatrix.getHeight(),
                    Bitmap.Config.ARGB_8888
            );

            for (int x = 0; x < bitMatrix.getWidth(); x++) {
                for (int y = 0; y < bitMatrix.getHeight(); y++) {
                    bitmap.setPixel(
                            x,
                            y,
                            bitMatrix.get(x, y)
                                    ? Color.BLACK
                                    : Color.WHITE
                    );
                }
            }

            imgBarcode.setImageBitmap(bitmap);

        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(
                    requireContext(),
                    "Código inválido",
                    Toast.LENGTH_SHORT
            ).show();
        }
    }

}
