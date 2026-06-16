package com.rogger.bp.ui.gallery;

import android.Manifest;
import android.content.ContentUris;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.MediaStore;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.RequiresApi;
import androidx.collection.LruCache;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rogger.bp.R;
import com.rogger.bp.ui.base.MenuUtil;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class GalleryFragment extends Fragment {
    private LruCache<String, Bitmap> cache;
    private RecyclerView rc;

    // ✅ ATUALIZAÇÃO: Contrato para permissões múltiplas (suporta acesso parcial do Android 14+)
    private final ActivityResultLauncher<String[]> permissionLauncher =
            registerForActivityResult(
                    new ActivityResultContracts.RequestMultiplePermissions(),
                    result -> {
                        boolean acessoTotalConcedido = false;

                        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                            Boolean imagensGranted = result.get(Manifest.permission.READ_MEDIA_IMAGES);
                            acessoTotalConcedido = imagensGranted != null && imagensGranted;
                        } else {
                            Boolean storageGranted = result.get(Manifest.permission.READ_EXTERNAL_STORAGE);
                            acessoTotalConcedido = storageGranted != null && storageGranted;
                        }

                        if (acessoTotalConcedido) {
                            // Acesso total concedido: Carrega a biblioteca inteira
                            loadImagesAsync();
                        } else {
                            // Caso estejamos no Android 14+ e o utilizador deu acesso limitado (parcial):
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                                Boolean limitadoGranted = result.get(Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED);
                                if (limitadoGranted != null && limitadoGranted) {
                                    // Acesso limitado concedido: Carrega apenas os itens selecionados,
                                    // mas na próxima abertura o app voltará a pedir acesso total.
                                    loadImagesAsync();
                                    return;
                                }
                            }

                            // Caso o acesso tenha sido totalmente negado
                            android.widget.Toast.makeText(
                                    requireContext(),
                                    "O acesso à galeria é necessário para selecionar imagens.",
                                    android.widget.Toast.LENGTH_LONG
                            ).show();
                        }
                    }
            );


    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_gallery, container, false);
    }

    @RequiresApi(api = Build.VERSION_CODES.TIRAMISU)
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        MenuUtil.clearMenu(this);
        rc = view.findViewById(R.id.rc_galerry);
        rc.setLayoutManager(new GridLayoutManager(getContext(), 3));
        initCache();

        checkPermissionAndLoadImages();

    }

    private void checkPermissionAndLoadImages() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) { // Android 14+ (API 34)
            // Verifica se já temos acesso TOTAL à galeria
            boolean temAcessoTotal = ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_MEDIA_IMAGES
            ) == PackageManager.PERMISSION_GRANTED;

            if (!temAcessoTotal) {
                // Se não temos acesso total (mesmo que tenhamos acesso parcial), solicitamos AMBAS as permissões.
                // Isso força o Android 14+ a abrir o diálogo perguntando se deseja "Selecionar mais fotos" ou "Permitir tudo".
                permissionLauncher.launch(new String[]{
                        Manifest.permission.READ_MEDIA_IMAGES,
                        Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED
                });
            } else {
                loadImagesAsync();
            }

        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) { // Android 13 (API 33)
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_MEDIA_IMAGES
            ) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(new String[]{Manifest.permission.READ_MEDIA_IMAGES});
            } else {
                loadImagesAsync();
            }

        } else { // Android 12 e inferiores
            if (ContextCompat.checkSelfPermission(
                    requireContext(),
                    Manifest.permission.READ_EXTERNAL_STORAGE
            ) != PackageManager.PERMISSION_GRANTED) {
                permissionLauncher.launch(new String[]{Manifest.permission.READ_EXTERNAL_STORAGE});
            } else {
                loadImagesAsync();
            }
        }
    }

    private void initCache() {
        int cacheSize = (int) (Runtime.getRuntime().maxMemory() / 1024) / 8;
        cache = new LruCache<>(cacheSize);
    }

    private void loadImagesAsync() {
        ExecutorService executor = Executors.newSingleThreadExecutor();
        Handler handler = new Handler(Looper.getMainLooper());
        executor.execute(() -> {
            List<ImageData> imageData = loadImagesFromDevice();
            handler.post(() -> {
                GallaryAdapter adapter = new GallaryAdapter(getContext(), imageData, this::onImageSelected, cache);
                rc.setAdapter(adapter);
            });

        });
    }

    private void onImageSelected(Uri uri) {

        Bundle result = new Bundle();
        result.putString("imageUri", uri.toString());

        getParentFragmentManager()
                .setFragmentResult("gallery_result", result);

        Navigation.findNavController(requireView()).popBackStack();
    }

    private List<ImageData> loadImagesFromDevice() {
        List<ImageData> list = new ArrayList<>();

        String[] projection = {MediaStore.Images.Media._ID, MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.SIZE};

        Cursor cursor = requireContext().getContentResolver().query(MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                projection, null, null, MediaStore.Images.Media.DATE_ADDED + " DESC");

        if (cursor != null) {
            int idColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID);
            int nameColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME);
            int sizeColumn = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE);

            while (cursor.moveToNext()) {
                long id = cursor.getLong(idColumn);
                String name = cursor.getString(nameColumn);
                long size = cursor.getLong(sizeColumn);

                Uri uri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, id);

                list.add(new ImageData(name, uri, size));
            }

            cursor.close();
        }

        return list;
    }
}
