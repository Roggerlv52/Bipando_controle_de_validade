package com.rogger.bp.ui.scanner;

import android.Manifest;
import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.content.pm.PackageManager;
import android.media.MediaPlayer;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.InputType;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.navigation.NavBackStackEntry;
import androidx.navigation.NavController;
import androidx.navigation.fragment.NavHostFragment;

import com.google.zxing.BarcodeFormat;
import com.journeyapps.barcodescanner.BarcodeView;
import com.journeyapps.barcodescanner.DefaultDecoderFactory;
import com.rogger.bp.R;
import com.rogger.bp.ui.commun.SharedPreferencesManager;

import java.util.Arrays;
import java.util.Collection;

public class BarcodeScanFragment extends Fragment {

    /** Valor do argumento "caller" quando aberto pelo SearchFragment */
    public static final String CALLER_SEARCH = "search";
    /** Chave usada no SavedStateHandle para devolver o barcode ao SearchFragment */
    public static final String KEY_SCANNED_BARCODE = "scanned_barcode";

    private BarcodeView barcodeView;
    private MediaPlayer mediaPlayer;
    private boolean beep;
    private int categoryId;
    /** Quem chamou este fragment: "add" (padrão) ou "search" */
    private String caller;
    /** Impede que múltiplas leituras disparem navegação repetida */
    private boolean resultHandled = false;

    public BarcodeScanFragment() {
        // Required empty public constructor
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View view = inflater.inflate(R.layout.barcode_scan, container, false);

        barcodeView = view.findViewById(R.id.barcode_scanner);

        setupArguments();
        setupPermissions();
        setupScanner();
        setupButtons(view);
        beep = SharedPreferencesManager.getBeepState(requireContext(), "beep");
        mediaPlayer = MediaPlayer.create(requireContext(), R.raw.scan_bipando);

        return view;
    }

    // -------------------- SETUP --------------------

    private void setupArguments() {
        if (getArguments() != null) {
            categoryId = getArguments().getInt("categoria_id", 0);
            caller     = getArguments().getString("caller", "add");
        } else {
            caller = "add";
        }
    }

    private void setupPermissions() {
        if (ContextCompat.checkSelfPermission(
                requireContext(),
                Manifest.permission.CAMERA
        ) != PackageManager.PERMISSION_GRANTED) {
            ActivityCompat.requestPermissions(
                    requireActivity(),
                    new String[]{Manifest.permission.CAMERA},
                    1
            );
        }
    }

    private void setupButtons(View view) {
        TextView btnAddWithoutBarcode = view.findViewById(R.id.btn_add_without_barcode);
        if (CALLER_SEARCH.equals(caller)) {
            // Modo busca: oculta o botão de inserção manual
            btnAddWithoutBarcode.setVisibility(View.GONE);
        } else {
            // Modo adição: comportamento original
            btnAddWithoutBarcode.setOnClickListener(this::dialogScanner);
        }
    }

    private void setupScanner() {
        Collection<BarcodeFormat> formats = Arrays.asList(
                BarcodeFormat.EAN_13,
                BarcodeFormat.EAN_8,
                BarcodeFormat.QR_CODE
        );
        barcodeView.setDecoderFactory(new DefaultDecoderFactory(formats));
        barcodeView.decodeContinuous(result -> {
            if (result.getText() != null && !resultHandled) {
                resultHandled = true;
                if (beep) playBeepSound();
                if (CALLER_SEARCH.equals(caller)) {
                    returnBarcodeToSearch(result.getText());
                } else {
                    gotoAddItem(result.getText());
                }
            }
        });
    }

    // -------------------- DIALOG (somente modo "add") --------------------

    @SuppressLint("UseCompatLoadingForDrawables")
    private void dialogScanner(View v) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        final EditText input = new EditText(requireContext());
        input.setInputType(InputType.TYPE_CLASS_NUMBER);
        input.setGravity(Gravity.CENTER);
        builder.setView(input);
        barcodeView.pause();
        builder.setMessage("Insira números do código de barras.")
                .setPositiveButton("OK", (dialog, id) -> {
                    String barcode = input.getText().toString();
                    if (!barcode.isEmpty()) {
                        new Handler(Looper.getMainLooper()).postDelayed(() ->
                                gotoAddItem(barcode), 1500);
                    }
                })
                .setNegativeButton("Cancelar", (dialog, id) -> {
                    dialog.dismiss();
                    barcodeView.resume();
                });
        builder.create().show();
    }

    // -------------------- NAVIGATION --------------------

    /**
     * Modo "add": navega para AddFragment passando o barcode.
     */
    private void gotoAddItem(String barcode) {
        Bundle bundle = new Bundle();
        bundle.putString("key_barcode", barcode);
        bundle.putInt("categoria_id", categoryId);
        NavController navController = NavHostFragment.findNavController(this);
        navController.navigate(R.id.nav_add_fragment, bundle);
    }

    /**
     * Modo "search": devolve o barcode para o SearchFragment via SavedStateHandle
     * e faz popBackStack para retornar sem abrir nenhum dialog.
     */
    private void returnBarcodeToSearch(String barcode) {
        NavController navController = NavHostFragment.findNavController(this);
        NavBackStackEntry previousEntry = navController.getPreviousBackStackEntry();
        if (previousEntry != null) {
            previousEntry.getSavedStateHandle().set(KEY_SCANNED_BARCODE, barcode);
        }
        navController.popBackStack();
    }

    // -------------------- MEDIA --------------------

    private void playBeepSound() {
        MediaPlayer mp = MediaPlayer.create(requireContext(), R.raw.scan_bipando);
        mp.start();
    }

    // -------------------- LIFECYCLE --------------------

    @Override
    public void onResume() {
        super.onResume();
        resultHandled = false;
        barcodeView.resume();
    }

    @Override
    public void onPause() {
        super.onPause();
        barcodeView.pause();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (mediaPlayer != null) {
            mediaPlayer.release();
            mediaPlayer = null;
        }
    }
}