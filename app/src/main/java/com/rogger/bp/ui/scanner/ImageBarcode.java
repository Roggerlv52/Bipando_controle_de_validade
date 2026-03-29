package com.rogger.bp.ui.scanner;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.view.OrientationEventListener;
import android.view.Window;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.bumptech.glide.Glide;
import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeEncoder;
import com.rogger.bp.R;

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
		String code = intent.getStringExtra("keyBarcode");
	
		// Agora você tem os dados recebidos e pode usá-los conforme necessário
		if (code != null) {
			ImageBarcode(code);
		} else {
			String ids = intent.getStringExtra("ids");
			String uri = intent.getStringExtra("uri");
			if (ids != null && uri != null) {
				int id = Integer.parseInt(ids);
				imageViewUri(uri);
			}
		}

		imageBack.setOnClickListener(v -> {
			finish();
		});
	}

	private void ImageBarcode(String code) {
		
		try {
			BarcodeEncoder barcodeEncoder = new BarcodeEncoder();
			Bitmap bitmap = barcodeEncoder.encodeBitmap(code, BarcodeFormat.CODE_128, 400, 200);
			imageView.setImageBitmap(bitmap);

		} catch (Exception e) {
			Toast.makeText(this, e.toString(), Toast.LENGTH_SHORT).show();
		}
		txtCode.setText(code);
	}

    private void imageViewUri(String uri) {
		Glide.with(this)
				.load(uri)
				.error(R.drawable.imagem_error)
				.into(imageView);
    }
}