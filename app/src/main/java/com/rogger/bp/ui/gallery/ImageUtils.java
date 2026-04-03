package com.rogger.bp.ui.gallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;
import android.util.Log;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {

    private static final String TAG = "ImageUtils";

    // Dimensão máxima do lado maior da imagem (px)
    private static final int MAX_DIMENSION = 800;

    // Tamanho máximo do arquivo final em bytes (100 KB)
    private static final int MAX_FILE_SIZE_BYTES = 100 * 1024;

    // Qualidade inicial e mínima permitida
    private static final int QUALIDADE_INICIAL = 85;
    private static final int QUALIDADE_MINIMA  = 30;
    // Passo de redução a cada iteração
    private static final int PASSO_QUALIDADE   = 10;

    /**
     * Processa a imagem vinda da galeria ou da câmera:
     *  1. Redimensiona para no máximo MAX_DIMENSION no lado maior
     *  2. Corrige rotação EXIF
     *  3. Comprime de forma adaptativa até atingir menos de MAX_FILE_SIZE_BYTES
     *
     * Funciona para galeria (Uri de conteúdo) e câmera (Uri de arquivo).
     */
    public static File processImage(Context context, Uri uri, File outputFile) throws Exception {

        // 1. Decodifica já redimensionando (evita OutOfMemory em fotos de câmera)
        Bitmap bitmap = decodeSampledBitmap(context, uri, MAX_DIMENSION, MAX_DIMENSION);

        // 2. Redimensiona proporcionalmente se ainda ultrapassar MAX_DIMENSION
        bitmap = redimensionarSeNecessario(bitmap, MAX_DIMENSION);

        // 3. Corrige rotação EXIF
        bitmap = rotateBitmapIfRequired(context, bitmap, uri);

        // 4. Compressão adaptativa — reduz qualidade até caber em MAX_FILE_SIZE_BYTES
        comprimirAdaptativamente(bitmap, outputFile);

        Log.d(TAG, "Imagem processada: " + outputFile.length() + " bytes"
                + " | " + bitmap.getWidth() + "x" + bitmap.getHeight() + "px");

        return outputFile;
    }

    // ======================== COMPRESSÃO ADAPTATIVA ========================

    /**
     * Comprime o bitmap reduzindo a qualidade JPEG progressivamente
     * até o arquivo ficar abaixo de MAX_FILE_SIZE_BYTES ou atingir
     * a qualidade mínima.
     *
     * Estratégia:
     *   - Começa em QUALIDADE_INICIAL (85)
     *   - Reduz PASSO_QUALIDADE (10) a cada iteração
     *   - Para quando o arquivo couber em 100 KB ou qualidade chegar em 30
     *   - Usa ByteArrayOutputStream em memória para medir antes de gravar
     */
    private static void comprimirAdaptativamente(Bitmap bitmap, File outputFile)
            throws IOException {

        int qualidade = QUALIDADE_INICIAL;
        ByteArrayOutputStream bos = new ByteArrayOutputStream();

        do {
            bos.reset();
            bitmap.compress(Bitmap.CompressFormat.JPEG, qualidade, bos);

            Log.d(TAG, "Compressão qualidade=" + qualidade
                    + " → " + bos.size() + " bytes");

            if (bos.size() <= MAX_FILE_SIZE_BYTES) break;

            qualidade -= PASSO_QUALIDADE;

        } while (qualidade >= QUALIDADE_MINIMA);

        // Grava o resultado final no arquivo de saída
        FileOutputStream fos = new FileOutputStream(outputFile);
        fos.write(bos.toByteArray());
        fos.flush();
        fos.close();
    }

    // ======================== REDIMENSIONAMENTO ========================

    /**
     * Redimensiona o bitmap proporcionalmente para que o lado maior
     * não ultrapasse maxDimension. Se já estiver dentro do limite, retorna o original.
     */
    private static Bitmap redimensionarSeNecessario(Bitmap bitmap, int maxDimension) {
        int largura = bitmap.getWidth();
        int altura  = bitmap.getHeight();

        if (largura <= maxDimension && altura <= maxDimension) {
            return bitmap; // já está dentro do limite
        }

        float escala;
        if (largura >= altura) {
            escala = (float) maxDimension / largura;
        } else {
            escala = (float) maxDimension / altura;
        }

        int novaLargura = Math.round(largura * escala);
        int novaAltura  = Math.round(altura * escala);

        return Bitmap.createScaledBitmap(bitmap, novaLargura, novaAltura, true);
    }

    // ======================== DECODE AMOSTRADO ========================

    /**
     * Decodifica a imagem já subamostrada para evitar OutOfMemory
     * em fotos de câmera de alta resolução (ex: 12MP, 48MP).
     */
    private static Bitmap decodeSampledBitmap(Context context, Uri uri,
                                              int reqWidth, int reqHeight) throws Exception {
        // Primeira passagem — só lê dimensões
        InputStream input = context.getContentResolver().openInputStream(uri);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(input, null, options);
        input.close();

        options.inSampleSize    = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;

        // Segunda passagem — decodifica com subamostragem
        input = context.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
        input.close();

        return bitmap;
    }

    private static int calculateInSampleSize(BitmapFactory.Options options,
                                             int reqWidth, int reqHeight) {
        int height     = options.outHeight;
        int width      = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth  = width  / 2;
            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth  / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    // ======================== CORREÇÃO DE ROTAÇÃO EXIF ========================

    private static Bitmap rotateBitmapIfRequired(Context context, Bitmap bitmap,
                                                 Uri uri) throws IOException {
        InputStream input = context.getContentResolver().openInputStream(uri);
        ExifInterface exif = new ExifInterface(input);

        int orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL);
        input.close();

        Matrix matrix = new Matrix();

        switch (orientation) {
            case ExifInterface.ORIENTATION_ROTATE_90:
                matrix.postRotate(90);
                break;
            case ExifInterface.ORIENTATION_ROTATE_180:
                matrix.postRotate(180);
                break;
            case ExifInterface.ORIENTATION_ROTATE_270:
                matrix.postRotate(270);
                break;
            default:
                return bitmap; // já está correta
        }

        return Bitmap.createBitmap(
                bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
}