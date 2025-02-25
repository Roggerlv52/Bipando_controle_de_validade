package com.rogger.bipando.ui.base;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.Toast;

import com.rogger.bipando.MainActivity;
import com.rogger.bipando.R;

import java.io.File;

public class SelectDialog {
    static final int REQUEST_IMAGE_CAPTURE = 1;
    static File photoFile = null;
    private static OnPhotoCapturedListener openCamera;

    public SelectDialog(OnPhotoCapturedListener listener) {
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

    public static void showDialogSave(Context context) {
        new AlertDialog.Builder(context).setTitle("Deseja salvar alteração?")
                .setPositiveButton("Salvar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface arg0, int arg1) {

                        Intent intent = new Intent(context, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
                        context.startActivity(intent);

                        arg0.dismiss();
                    }

                }).setNeutralButton("Cancelar", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        // Ação a ser executada ao clicar em Cancelar
                        dialog.dismiss();
                    }
                }).create().show();
    }
    public static void msg(Context context, String str) {
        Toast.makeText(context, str, Toast.LENGTH_SHORT).show();
    }

}