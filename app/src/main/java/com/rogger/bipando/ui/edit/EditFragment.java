package com.rogger.bipando.ui.edit;

import static android.app.Activity.RESULT_OK;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.rogger.bipando.R;
import com.rogger.bipando.ui.base.OnPhotoCapturedListener;
import com.rogger.bipando.ui.base.SelectDialog;
import com.rogger.bipando.ui.base.Utils;
import com.rogger.bipando.ui.scanner.ImageBarcode;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;


public class EditFragment extends Fragment implements OnPhotoCapturedListener {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private ImageView imageView;
    private File photoFile;
    private Uri photoUri;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_edit, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        imageView = view.findViewById(R.id.image_edit);
        TextView txtBarcode = view.findViewById(R.id.txt_edit_bacode);
        imageView = view.findViewById(R.id.image_edit);
        EditText edtName = view.findViewById(R.id.edt_name_fragment);
        Button btnData = view.findViewById(R.id.datePickerButton);
        EditText edtNote = view.findViewById(R.id.edit_fragment_note);
        /*
        *
         new SelectDialog(this);
         new Utils(this);
         Chamando o construtor pra escutar o evento de listener para ativar a interface.
         *
         *
         */
        new SelectDialog(this);
        new Utils(this);

        if (getArguments() != null) {
            String imageUrl = getArguments().getString("imageUrl");
            String name = getArguments().getString("productName");
            String barcode = getArguments().getString("barcode");
            String data = getArguments().getString("data");
            String note = getArguments().getString("note");

            Picasso.get().load(imageUrl).into(imageView);
            txtBarcode.setText(barcode);
            edtName.setText(name);
            btnData.setText(data);
            edtNote.setText(note);

            txtBarcode.setOnClickListener(view1 -> {
                startImageBarcode(barcode);
            });
            imageView.setOnClickListener(this::startAlerta);
        }
        super.onViewCreated(view, savedInstanceState);
    }

    private void startAlerta(View v) {
        SelectDialog.showCustomAlertDialog(this.requireContext());
    }

    private void startImageBarcode(String barcode) {
        Intent intent = new Intent(getContext(), ImageBarcode.class);
        intent.putExtra("kayBarcode", barcode);
        startActivity(intent);
    }

    @Override
    public void onOpenCamera() {
        try {
            photoFile = createImageFile();
            Intent cameraIntent = Utils.CameraIntent(requireContext(), photoFile);
            startActivityForResult(cameraIntent, REQUEST_IMAGE_CAPTURE);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public void onPassUri(Uri uri) {
        photoUri = uri;
    }

    private File createImageFile() throws IOException {
        //Cria o nome do arquivo
        @SuppressLint("SimpleDateFormat") String timeStamp = new SimpleDateFormat("yyyyMMdd").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = requireContext().getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName,/* prefixo*/
                ".jpg",/*sufixo*/
                storageDir/*Diretório*/);
        //Guarda o caminho do arquivo para usá-lo depois
        String currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            imageView.setImageURI(photoUri);
        }
    }
}
