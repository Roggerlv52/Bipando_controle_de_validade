package com.rogger.bipando.ui.base;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.provider.MediaStore;

import androidx.core.content.FileProvider;

import java.io.File;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.TimeUnit;

public class Utils {
    private static OnPhotoCapturedListener openCamera;

    public Utils(OnPhotoCapturedListener listener){
        openCamera = listener;
    }

    @SuppressLint("QueryPermissionsNeeded")
    public static Intent CameraIntent(Context context, File photoFile) throws Exception {
        // Verifica se há suporte para câmera
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(context.getPackageManager()) != null) {
            // Exclui o arquivo existente, se necessário
            if (photoFile != null && photoFile.exists()) {
                photoFile.delete();
            }
            // Cria o arquivo para a imagem
            if (photoFile == null) {
                throw new Exception("Falha ao criar arquivo de imagem.");
            }
            Uri photoUri = FileProvider.getUriForFile(
                    context,
                    context.getPackageName() + ".fileprovider",
                    photoFile
            );
            openCamera.onPassUri(photoUri);
            takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            return takePictureIntent;
        } else {
            throw new Exception("Câmera não suportada no dispositivo.");
        }

    }

    public static long calcDifferencInDays(String dateItem) {
        @SuppressLint("SimpleDateFormat") SimpleDateFormat format = new SimpleDateFormat("yyyy-MM-dd"); // Use o formato apropriado
        try {
            Date dataItem = format.parse(dateItem);
            Date dataAtual = new Date(); // Obtém a data atual
            long diferencaEmMillis = dataItem.getTime() - dataAtual.getTime();
            return TimeUnit.HOURS.convert(diferencaEmMillis, TimeUnit.MILLISECONDS);
        } catch (ParseException e) {
            e.printStackTrace();
            return -1; // Retorna um valor negativo se houver um erro na conversão da data
        }
    }

    public static String dataFomat(String originalDateString) {
        // Formato original da data
        @SuppressLint("SimpleDateFormat") SimpleDateFormat originalFormat = new SimpleDateFormat("yyyy-MM-dd");
        try {
            // Converte a string original para um objeto Date
            Date originalDate = originalFormat.parse(originalDateString);
            SimpleDateFormat desiredFormat = new SimpleDateFormat("dd/MM/yyyy");
            // Converte a data para a string no novo formato
            return desiredFormat.format(originalDate);

        } catch (ParseException e) {
            e.printStackTrace();
            return null;
        }
    }

}