package com.rogger.bipando.ui.commun;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;

import com.rogger.bipando.R;

public class ShowSelectDialog {
    private static LinearLayout btnGallery, btnCamera;
    public interface selectedCallback{
            void openGallery();
            void openCamera();
    }
    public static void show(Context context, selectedCallback callback){
        LayoutInflater inflater = LayoutInflater.from(context);
        View view = inflater.inflate(R.layout.dialog_select, null);
        AlertDialog dialog = new AlertDialog.Builder(context).setView(view).create();

         btnGallery = view.findViewById(R.id.btnGallery);
         btnCamera = view.findViewById(R.id.btnCamera);

         btnGallery.setOnClickListener( v -> {
             callback.openGallery();
             dialog.dismiss();
         });
         btnCamera.setOnClickListener( v -> {
             callback.openCamera();
             dialog.dismiss();
         });
        dialog.show();
    }
}
