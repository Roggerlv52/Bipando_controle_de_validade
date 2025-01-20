package com.rogger.bipando.ui.scanner;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Bundle;
import android.view.OrientationEventListener;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.rogger.bipando.R;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.squareup.picasso.Picasso;

public class ImageBarcode extends AppCompatActivity {

	private ImageView imageView, imageBack;
	private TextView txtCode;
	private Configuration newConfig;
	private OrientationEventListener orientationEventListener;

	@Override
	protected void onCreate(Bundle arg0) {
		super.onCreate(arg0);
		setContentView(R.layout.image_layout);

		imageView = findViewById(R.id.imagelayout);
		txtCode = findViewById(R.id.txt_code_layout);
		imageBack = findViewById(R.id.image_back);
		Window window = getWindow();

		// Verificar a versão do Android
        // Configurar a cor da StatusBar para preto
        window.setStatusBarColor(getResources().getColor(android.R.color.black)); // Substitua android.R.color.black pela cor desejada
        // Dentro do método onCreate() ou onResume() da segunda atividade
		Intent intent = getIntent();
		String code = intent.getStringExtra("kayBarcode");
	
		// Agora você tem os dados recebidos e pode usá-los conforme necessário
		if (code != null) {
			ImageBarcod(code);
		} else {
			String ids = intent.getStringExtra("ids");
			String uri = intent.getStringExtra("uri");
			if (ids != null && uri != null) {
				int id = Integer.parseInt(ids);
				imageVewUri(id,uri);
			}
		}

		imageBack.setOnClickListener(v -> {
			finish();
		});
	}

	private void ImageBarcod(String code) {
		
		try {
			BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
			Bitmap bitmap = barcodeEncoder.encodeBitmap(code, BarcodeFormat.CODE_128, 400, 200);
			imageView.setImageBitmap(bitmap);

		} catch (Exception e) {
			Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
		}
		txtCode.setText(code);
	}

    private void imageVewUri(int id,String uri) {
        /*
        Registro modelo = SqlHelper.getInstance(this).buscarId(id);
        if (modelo.byteArray != null) {
        	imageView.setImageBitmap(ConvertTo.byteArrayToBitmap(modelo.byteArray));
        }
         */
		Picasso.get().load(uri).into(imageView);
    }
}