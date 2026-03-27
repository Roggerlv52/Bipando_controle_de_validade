package com.rogger.bp.ui.gallery;

import android.net.Uri;

import androidx.annotation.NonNull;

import java.io.File;

public interface CameraCallback {
    void onImageCaptured(@NonNull Uri imageUri, @NonNull File imageFile);
    void onCancel();
    void onError(@NonNull Exception e);
}
