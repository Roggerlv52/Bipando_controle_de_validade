package com.rogger.bipando.ui.commun;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.AsyncTask;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

/** @noinspection deprecation*/
public class ImageDownloader {

	public static void downloadImage(String imageUrl, ImageCallback callback) {
		new AsyncTask<String, Void, Bitmap>() {
			@Override
			protected Bitmap doInBackground(String... urls) {
				String imageUrl = urls[0];
				try {
					URL url = new URL(imageUrl);
					HttpURLConnection connection = (HttpURLConnection) url.openConnection();
					connection.connect();
					InputStream inputStream = connection.getInputStream();
					return BitmapFactory.decodeStream(inputStream);
				} catch (Exception e) {
					e.printStackTrace();
					return null;
				}
			}

			@Override
			protected void onPostExecute(Bitmap bitmap) {
				if (bitmap != null) {
					// Chame o callback com a imagem baixada
					callback.onImageDownloaded(bitmap);
				}
			}
		}.execute(imageUrl);
	}
}
