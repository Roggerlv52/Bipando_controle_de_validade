package com.rogger.bipando.ui.dd;


import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.content.res.Resources;
import androidx.annotation.Nullable;
import androidx.appcompat.widget.Toolbar;

import com.rogger.bipando.MainActivity;
import com.rogger.bipando.R;
import com.rogger.bipando.database.Registro;
import com.rogger.bipando.ui.base.BaseActivity;
import com.rogger.bipando.ui.base.OnPhotoCapturedListener;
import com.rogger.bipando.ui.base.SelectDialog;
import com.rogger.bipando.ui.base.SpinerData;
import com.rogger.bipando.ui.base.Utils;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class Add_item extends BaseActivity implements OnPhotoCapturedListener {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private static final int REQUEST_CAMERA_PERMISSION = 100;
    private File photoFile;
    private Uri photoUri;
    private TextView txtData;
    private ImageButton btnScanner, btnScannerEdit, tb_back, tb_more;
    private Registro dados;
    private ImageView imgView;
    private Button btnDataPicker;
    private EditText editName, editNote;
    private String name;
    private String dataValid;
    private String barcod;
    private String note;

    @Override
    protected void onCreate(Bundle arg0) {
        super.onCreate(arg0);
        setContentView(R.layout.fragment_add);

        Toolbar toolbar = findViewById(R.id.toolbar_main);

        imgView = findViewById(R.id.img_fragmemt_add);
        editName = findViewById(R.id.edt_name_fragment_add);
        btnDataPicker = findViewById(R.id.datePickerBtn_add);
        Button btnSalva = findViewById(R.id.btn_fragment_salve_add);
        editNote = findViewById(R.id.edit_fragment_note_add);
        txtData = findViewById(R.id.txt_add_data);
        TextView txtBarcode = findViewById(R.id.txt_add_barcode);
        tb_back = toolbar.findViewById(R.id.tb_btn_back);
        //	tb_more = toolbar.findViewById(R.id.tb_btn_more);
        //	TextView tb_txt = toolbar.findViewById(R.id.tb_txt_title);
        //	tb_txt.setText("Adicionar item");
        new SelectDialog( this);
        new Utils(this);
        tb_back.setOnClickListener(this::trocarFragmento);

        btnDataPicker.setText(R.string.selct_date);

        Intent intentBarcode = getIntent();
        barcod = intentBarcode.getStringExtra("key_barcode");
        txtBarcode.setText(barcod);
        boolean b = intentBarcode.getBooleanExtra("key_true", true);
        if (b) {
            // Corrigir fututamente para que nao inicialize o
            // alertafialog toda vez que rotacionsr o dispositivo
            View vew = null;
            initDatePicker(vew);
        }
        imgView.setOnClickListener(v -> {
            SelectDialog.showCustomAlertDialog(this);
        });
        btnDataPicker.setOnClickListener(this::initDatePicker);
        btnSalva.setOnClickListener(v -> {
            name = editName.getText().toString();
            note = editNote.getText().toString();
            if (!name.isEmpty()) {
                String Agora = "";

                //long c = SqlHelper.getInstance(this).additem(name, note, barcod, Agora, byteArray);
                trocarFragmento(v);

            } else {
                SpinerData sp = new SpinerData();
                Resources resources = this.getResources();
                SelectDialog.msg(this, resources.getString(R.string.name_barcod_valid_));
            }
        });
    }

    private void initDatePicker(View v) {
        SpinerData sd = new SpinerData();
        DatePickerDialog.OnDateSetListener dateSetListener = (datePicker, year, month, day) -> {
            String dd = sd.makeDateString(day, month + 1, year);
            btnDataPicker.setText(dd);
            txtData.setText(dd);
        };

        int[] resultado = sd.retornaVetor("");
        int day = resultado[0];
        int month = resultado[1];
        int year = resultado[2];

        int style = AlertDialog.THEME_HOLO_LIGHT;

        DatePickerDialog datePickerDialog = new DatePickerDialog(this, style, dateSetListener, year, month, day);
        datePickerDialog.setTitle("Selecione a data de vencento.");
        datePickerDialog.show();
    }
    @Override
    public void onOpenCamera() {
        try {
          Intent cameraIntent =  Utils.CameraIntent(this,createImageFile());
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
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(imageFileName,/* prefixo*/
                ".jpg",/*sufixo*/
                storageDir/*Diretório*/);
        //Guarda o caminho do arquivo para usá-lo depois
        String currentPhotoPath = image.getAbsolutePath();
        return image;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK){
            imgView.setImageURI(photoUri);
        }
    }

    private void trocarFragmento(View v) {
        showCustomToast();
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }

    @Override
    public void onPointerCaptureChanged(boolean hasCapture) {
        super.onPointerCaptureChanged(hasCapture);
    }
}
/*
SimpleDateFormat dataFormatada = new SimpleDateFormat("yyyy-MM-dd", new Locale("pt", "BR"));
                    Date dataSalva = dataFormatada.parse(dataValid);
                    SimpleDateFormat minhadata = new SimpleDateFormat("yyyy-MM-dd", new Locale("pt", "BR"));
                    Agora = minhadata.format(dataSalva);
 */