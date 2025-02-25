package com.rogger.bipando.ui.gallery;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.provider.MediaStore;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.core.content.FileProvider;
import androidx.fragment.app.Fragment;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class ImagePikerUtil {
    private Uri photoUri;
    private File photoFile;
    private CameraCallback callback;
    private static final String TAG = "ImagePickerUtil";
    /**
     * Registra o launcher (chamar no onCreate ou onViewCreated)
     */
    public ActivityResultLauncher<Intent> register(
            @NonNull Fragment fragment,
            @NonNull CameraCallback callback
    ) {
        this.callback = callback;

        return fragment.registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK) {
                        callback.onImageCaptured(photoUri, photoFile);
                    } else {
                        callback.onCancel();
                        cleanUpTempFiles(photoFile);
                    }
                }
        );
    }

    /**
     * Abre a câmera
     */
    public void openCamera(@NonNull Context context,
                           @NonNull ActivityResultLauncher<Intent> launcher) {
        try {
            photoFile = createImageFile(context);
            photoUri = FileProvider.getUriForFile(context, context.getPackageName() + ".provider", photoFile
            );

            Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            intent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
            intent.addFlags(Intent.FLAG_GRANT_WRITE_URI_PERMISSION);

            launcher.launch(intent);

        } catch (Exception e) {
            callback.onError(e);
        }
    }
    /**
     * Cria arquivo temporário
     */
    public static File createImageFile(Context context) throws IOException {
        String timeStamp =
                new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault())
                        .format(new Date());

        File storageDir =
                context.getExternalFilesDir(Environment.DIRECTORY_PICTURES);

        return File.createTempFile(
                "IMG_" + timeStamp + "_",
                ".jpg",
                storageDir
        );
    }
    /**
     * Limpa todos os arquivos temporários criados pelo ImagePickerUtil.
     * Deve ser chamado no onDestroy() do Fragment/Activity para garantir a limpeza.
     */
    public static void cleanUpTempFiles(File file) {
        if (file != null && file.exists()) {
            file.delete();
        }
    }
}

