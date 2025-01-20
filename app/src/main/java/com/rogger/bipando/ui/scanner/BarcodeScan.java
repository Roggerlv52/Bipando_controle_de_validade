package com.rogger.bipando.ui.scanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.text.InputType;
import android.view.Gravity;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.TextView;
import androidx.appcompat.widget.Toolbar;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeView;
import com.journeyapps.barcodescanner.DecoratedBarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;
import com.rogger.bipando.MainActivity;
import com.rogger.bipando.R;
import com.rogger.bipando.ui.base.BaseActivity;
import com.rogger.bipando.ui.commun.SharedPreferencesManager;
import com.rogger.bipando.ui.dd.Add_item;

import java.util.Arrays;
import java.util.Collection;

public class BarcodeScan extends BaseActivity {
    private BarcodeView barcodeView;
    private MediaPlayer mediaPlayer;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.barcode_scan);

        barcodeView = findViewById(R.id.barcode_scanner);
        Toolbar toolbar = findViewById(R.id.toolbar_main);
        toolbar.setBackgroundColor(Color.parseColor("#88000000"));
        TextView txtTb = toolbar.findViewById(R.id.tb_txt_title);
        ImageButton btBack = toolbar.findViewById(R.id.tb_btn_back);
        btBack.setOnClickListener(this::trocarFragmento);
        txtTb.setText(R.string.digitaliz_c_digo_de_barras);

        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.CAMERA}, 1);
        }

        TextView btnAddWithoutBarcode = findViewById(R.id.btn_add_without_barcode);
        Window window = getWindow();
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(ContextCompat.getColor(this, R.color.black));
        // Inicializar o MediaPlayer
        mediaPlayer = MediaPlayer.create(this, R.raw.scan_beep);
        btnAddWithoutBarcode.setOnClickListener(this::dialogScanner);

        Collection<BarcodeFormat> formats = Arrays.asList(BarcodeFormat.EAN_13,
                BarcodeFormat.EAN_8, BarcodeFormat.QR_CODE);
        barcodeView.setDecoderFactory(new DefaultDecoderFactory(formats));
        barcodeView.decodeContinuous(result -> {
            if (result.getText() != null) {
                boolean beep = SharedPreferencesManager.getBeepState(this, "beep");
                if (beep) {
                    playBeepSound();
                }
                String barcode = result.getText();
                gotoAdd_item(barcode);
            }
        });
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    private void dialogScanner(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        final EditText input = new EditText(this);

        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setBackground(getDrawable(R.drawable.edtx_custom));
        input.setTextAlignment(View.TEXT_ALIGNMENT_CENTER); // Centraliza o texto
        input.setGravity(Gravity.CENTER); // Garante que o texte fique no centro verticalmente
        builder.setView(input);
        barcodeView.pause();
        builder.setMessage("Insira numeos do codigo de barras.")

                .setPositiveButton("OK", (dialog, id) -> {
                    // Ação quando o usuário clica em OK
                    String barcode = input.getText().toString();
                    if (!barcode.isEmpty()) {
                        gotoAdd_item(barcode);
                    }
                }).setNegativeButton("Cancelar", (dialog, id) -> {
                    // Ação quando o usuário clica em Cancelar
                    dialog.cancel();
                    barcodeView.resume();
                });
        AlertDialog dialog = builder.create();
        dialog.show();

    }

    private void gotoAdd_item(String barcode) {
        Intent intent = new Intent(BarcodeScan.this, Add_item.class);
        intent.putExtra("key_barcode", barcode);
        intent.putExtra("key_true", true);
        startActivity(intent);
        finish();
    }

    private void playBeepSound() {
        if (mediaPlayer != null) {
            mediaPlayer.start();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        barcodeView.resume();
    }

    @Override
    protected void onPause() {
        super.onPause();
        barcodeView.pause();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Liberar recursos do MidiaPlayer
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
    private void trocarFragmento(View v) {
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

}