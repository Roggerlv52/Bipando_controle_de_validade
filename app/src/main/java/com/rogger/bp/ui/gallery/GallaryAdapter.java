package com.rogger.bp.ui.gallery;

import android.content.Context;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Size;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.collection.LruCache;
import androidx.recyclerview.widget.RecyclerView;

import com.rogger.bp.R;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;


public class GallaryAdapter extends RecyclerView.Adapter<GallaryAdapter.ViewHolder> {
    private Context context;
    private List<ImageData> images;
    private OnImgSelectedListener listener;
    private LruCache<String, Bitmap> cache;

    public GallaryAdapter(Context context, List<ImageData> images, OnImgSelectedListener listener,
                          LruCache<String, Bitmap> cache) {
        this.context = context;
        this.images = images;
        this.listener = listener;
        this.cache = cache;
    }

    @NonNull
    @Override
    public GallaryAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull GallaryAdapter.ViewHolder holder, int position) {
        ImageData img = images.get(position);
        Bitmap bitmap = cache.get(img.uri.toString());
        if (bitmap != null) {
            holder.imgThumb.setImageBitmap(bitmap);
        }else {
            loadThumbnailAsync(img.uri, holder.imgThumb);
        }
        holder.itemView.setOnClickListener(v -> listener.onImgSelected(img.uri));
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imgThumb;
        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            imgThumb = itemView.findViewById(R.id.imgThumb);
        }
    }
    private void loadThumbnailAsync(Uri uri, ImageView imageView) {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());

        executor.execute(() -> {
            try {
                Bitmap thumb;
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    thumb = context.getContentResolver().loadThumbnail(uri, new Size(250, 250), null);
                } else {
                    thumb = null;
                }

                cache.put(uri.toString(), thumb);

                handler.post(() -> imageView.setImageBitmap(thumb));
            } catch (Exception ignored) {
            }
        });
    }
}
