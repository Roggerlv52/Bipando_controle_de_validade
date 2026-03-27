package com.rogger.bp.ui.gallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Matrix;
import android.media.ExifInterface;
import android.net.Uri;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;

public class ImageUtils {

    public static File processImage(Context context, Uri uri, File outputFile) throws Exception {

        Bitmap original = decodeSampledBitmap(context, uri, 1024, 1024);

        // 🔄 CORRIGE ROTAÇÃO
        original = rotateBitmapIfRequired(context, original, uri);

        saveBitmapToFile(original, outputFile, 90);
        return outputFile;
    }

    private static void saveBitmapToFile(Bitmap original, File outputFile, int quality) throws IOException {
        FileOutputStream out = new FileOutputStream(outputFile);
        original.compress(Bitmap.CompressFormat.JPEG, quality, out);
        out.flush();
        out.close();
    }

    private static Bitmap decodeSampledBitmap(Context context, Uri uri, int reqWidth, int reqHeight) throws Exception {

        InputStream input = context.getContentResolver().openInputStream(uri);
        BitmapFactory.Options options = new BitmapFactory.Options();
        options.inJustDecodeBounds = true;
        BitmapFactory.decodeStream(input, null, options);
        input.close();

        options.inSampleSize = calculateInSampleSize(options, reqWidth, reqHeight);
        options.inJustDecodeBounds = false;

        input = context.getContentResolver().openInputStream(uri);
        Bitmap bitmap = BitmapFactory.decodeStream(input, null, options);
        input.close();

        return bitmap;
    }

    private static int calculateInSampleSize(BitmapFactory.Options options, int reqWidth, int reqHeight) {

        int height = options.outHeight;
        int width = options.outWidth;
        int inSampleSize = 1;

        if (height > reqHeight || width > reqWidth) {
            int halfHeight = height / 2;
            int halfWidth = width / 2;

            while ((halfHeight / inSampleSize) >= reqHeight
                    && (halfWidth / inSampleSize) >= reqWidth) {
                inSampleSize *= 2;
            }
        }
        return inSampleSize;
    }

    // ==========================
    // 🔄 CORREÇÃO DE ROTAÇÃO EXIF
    // ==========================
    private static Bitmap rotateBitmapIfRequired(Context context, Bitmap bitmap, Uri uri) throws IOException {

        InputStream input = context.getContentResolver().openInputStream(uri);
        ExifInterface exif = new ExifInterface(input);

        int orientation = exif.getAttributeInt(
                ExifInterface.TAG_ORIENTATION,
                ExifInterface.ORIENTATION_NORMAL
        );
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

        return Bitmap.createBitmap(bitmap, 0, 0, bitmap.getWidth(), bitmap.getHeight(), matrix, true);
    }
}

