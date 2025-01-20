package com.rogger.bipando.ui.base;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.Intent;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import com.rogger.bipando.R;
import java.io.File;

public class SelectDialog {
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static File photoFile = null;
    private static OnPhotoCapturedListener openCamera;

    public SelectDialog( OnPhotoCapturedListener listener) {
        openCamera = listener;
    }

    public static void showCustomAlertDialog(final Context context) {
        // Inflar o layout personalizado
        View customView = LayoutInflater.from(context).inflate(R.layout.dialog_custom, null);
        // Configurar elementos de layout e lógica aqui (ex: botões, textos, etc.)
        AlertDialog.Builder builder = new AlertDialog.Builder(context)
                .setPositiveButton("", (dialogInterface, i) -> {

                }).setNegativeButton("Galeria", (dialogInterface, i) -> {
                    abrirGaleria(context);
                });
        builder.setView(customView).setPositiveButton("Camera", (dialogInterface, i) -> {
            openCamera.onOpenCamera();

        });
        AlertDialog alertDialog = builder.create();
        alertDialog.show();
    }

    private static void abrirGaleria(Context context) {
        //Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI)
        Intent galeriaIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        ((Activity) context).startActivityForResult(galeriaIntent, REQUEST_IMAGE_CAPTURE);
    }
}