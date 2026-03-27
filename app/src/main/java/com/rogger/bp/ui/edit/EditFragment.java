package com.rogger.bp.ui.edit;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.fragment.NavHostFragment;

import com.bumptech.glide.Glide;
import com.rogger.bp.R;
import com.rogger.bp.data.database.FirebaseStorageDataSource;
import com.rogger.bp.data.model.Categoria;
import com.rogger.bp.data.model.Produto;
import com.rogger.bp.ui.base.DialogUtil;
import com.rogger.bp.ui.base.Utils;
import com.rogger.bp.ui.commun.ShowSelectDialog;
import com.rogger.bp.ui.gallery.CameraCallback;
import com.rogger.bp.ui.gallery.ImagePikerUtil;
import com.rogger.bp.ui.gallery.ImageUtils;
import com.rogger.bp.ui.viewmodel.CategoriaViewModel;
import com.rogger.bp.ui.viewmodel.DataViewModel;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EditFragment extends Fragment {

    private TextView txtBarcode;
    private EditText edtName, edtNote;
    private Button btnData, btnSave;
    private ImageView imgUpload;
    private Spinner spinner;

    private long timestamp;
    private Produto produto;

    private DataViewModel dataViewModel;
    private CategoriaViewModel categoriaViewModel;
    private EditPresenterViewModel editVM;

    private ArrayAdapter<Categoria> categoriaAdapter;
    private final List<Categoria> listaCategorias = new ArrayList<>();
    private boolean carregandoSpinner = true;

    private ImagePikerUtil imagePickerUtil;
    private ActivityResultLauncher<Intent> cameraLauncher;

    // -------------------- LIFECYCLE --------------------

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        View v = inflater.inflate(R.layout.fragment_edit, container, false);

        txtBarcode = v.findViewById(R.id.txt_edit_bacode);
        edtName = v.findViewById(R.id.edt_name_fragment);
        edtNote = v.findViewById(R.id.edit_fragment_note);
        btnData = v.findViewById(R.id.datePickerButton);
        btnSave = v.findViewById(R.id.btn_fgm_save);
        imgUpload = v.findViewById(R.id.image_edit);
        spinner = v.findViewById(R.id.spinner_edit);

        dataViewModel = new ViewModelProvider(requireActivity())
                .get(DataViewModel.class);

        categoriaViewModel = new ViewModelProvider(requireActivity())
                .get(CategoriaViewModel.class);

        editVM = new ViewModelProvider(this)
                .get(EditPresenterViewModel.class);

        imagePickerUtil = new ImagePikerUtil();
        loadProduto();
        return v;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        setupCamera();
        setupGalleryResult();
        setupSpinner();
        setupClicks();
        setupMenu();
        loadProduto();
    }

    // -------------------- CAMERA / GALERIA --------------------

    private void setupCamera() {
        cameraLauncher = imagePickerUtil.register(this, new CameraCallback() {
            @Override
            public void onImageCaptured(@NonNull Uri uri, @NonNull File file) {
                try {
                    File processed =
                            ImageUtils.processImage(requireContext(), uri, file);
                    editVM.onNewImage(processed);
                    setImageView( processed.getAbsolutePath());
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onCancel() {
            }

            @Override
            public void onError(@NonNull Exception e) {
            }
        });
    }

    private void setupGalleryResult() {
        getParentFragmentManager().setFragmentResultListener(
                "gallery_result",
                getViewLifecycleOwner(),
                (key, bundle) -> {
                    try {
                        Uri uri = Uri.parse(bundle.getString("imageUri"));
                        File out = ImagePikerUtil.createImageFile(requireContext());
                        File processed =
                                ImageUtils.processImage(requireContext(), uri, out);

                        editVM.onNewImage(processed);
                        setImageView(processed.getAbsolutePath());
                    } catch (Exception e) {
                        e.printStackTrace();
                    }

                }
        );
    }

    // -------------------- UI --------------------

    private void setupClicks() {

        imgUpload.setOnClickListener(v ->
                ShowSelectDialog.show(requireContext(), new ShowSelectDialog.selectedCallback() {
                    @Override
                    public void openGallery() {
                        editVM.prepareForNewImage();
                        NavHostFragment.findNavController(EditFragment.this)
                                .navigate(R.id.action_editFragment_to_galerryFragment);
                    }

                    @Override
                    public void openCamera() {
                        editVM.prepareForNewImage();
                        imagePickerUtil.openCamera(requireContext(), cameraLauncher);
                    }
                })
        );

        btnData.setOnClickListener(v ->
                Utils.showDatePicker(requireContext(), (ts, data) -> {
                    timestamp = ts;
                    btnData.setText(data);
                })
        );

        btnSave.setOnClickListener(v -> {
            if (!Utils.validEditText(edtName)) return;

            // Pega a URL antiga ANTES de aplicar a nova imagem local
            String urlAntiga = editVM.getOldImagePath();

            collectInputs();

            // Agora chama o update com callback para garantir o upload para o Storage
            dataViewModel.update(produto, urlAntiga, new FirebaseStorageDataSource.UploadCallback() {
                @Override
                public void onProgresso(int porcentagem) {
                    Log.d("EditFragment", "Upload progresso: " + porcentagem + "%");
                }

                @Override
                public void onSucesso(String urlDownload) {
                    Log.d("EditFragment", "Upload concluído: " + urlDownload);
                }

                @Override
                public void onErro(Exception e) {
                    Log.e("EditFragment", "Falha no upload da imagem: " + e.getMessage());
                }
            });

            Toast.makeText(getContext(),
                    "Produto salvo com sucesso",
                    Toast.LENGTH_SHORT).show();

            NavHostFragment.findNavController(this).popBackStack();
        });

        txtBarcode.setOnClickListener(v -> {
            Bundle b = new Bundle();
            b.putString("barcode", produto.getCodigoBarras());
            NavHostFragment.findNavController(this)
                    .navigate(R.id.nav_barcode_show, b);
        });
    }

    // -------------------- SPINNER --------------------

    private void setupSpinner() {
        categoriaViewModel.getCategories().observe(
                getViewLifecycleOwner(),
                cats -> {
                    listaCategorias.clear();

                    Categoria placeholder = new Categoria();
                    placeholder.setId(-1);
                    placeholder.setNome("Selecione uma categoria");
                    listaCategorias.add(placeholder);

                    if (cats != null) listaCategorias.addAll(cats);

                    categoriaAdapter = new ArrayAdapter<>(
                            requireContext(),
                            android.R.layout.simple_spinner_item,
                            listaCategorias
                    );
                    categoriaAdapter.setDropDownViewResource(
                            android.R.layout.simple_spinner_dropdown_item);

                    spinner.setAdapter(categoriaAdapter);

                    if (produto != null) selecionarCategoria(produto.getCategoryId());
                    carregandoSpinner = false;
                }
        );

        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(
                    AdapterView<?> parent, View view, int position, long id
            ) {
                if (carregandoSpinner || produto == null) return;

                Categoria c = listaCategorias.get(position);
                produto.setCategoryId(c.getId() == -1 ? 0 : c.getId());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    // -------------------- DADOS --------------------

    private void loadProduto() {
        Bundle args = getArguments();
        if (args == null) return;

        int id = args.getInt("id", -1);
        if (id == -1) return;
        dataViewModel.getProdutoByIdInMemory(id)
                .observe(getViewLifecycleOwner(), this::bindProduto);
    }

    private void bindProduto(Produto p) {
        this.produto = p;

        edtName.setText(p.getNome());
        edtNote.setText(p.getAnotacoes());
        txtBarcode.setText(p.getCodigoBarras());

        timestamp = p.getTimestamp();
        btnData.setText(timestampParaData(timestamp));
        editVM.setOldImagePath(produto.getImagem());
        if (!editVM.hasNewImage()) {
           setImageView(p.getImagem());
        }
        if (!carregandoSpinner) selecionarCategoria(p.getCategoryId());
    }

    private void collectInputs() {
        produto.setNome(edtName.getText().toString().trim());
        produto.setAnotacoes(edtNote.getText().toString());
        produto.setTimestamp(timestamp);
        produto.setCodigoBarras(txtBarcode.getText().toString());
        editVM.applyImageToProduto(produto);
    }

    private void selecionarCategoria(int id) {
        for (int i = 0; i < listaCategorias.size(); i++) {
            if (listaCategorias.get(i).getId() == id) {
                spinner.setSelection(i);
                break;
            }
        }
    }

    // -------------------- MENU --------------------

    private void setupMenu() {
        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater inflater) {
                menu.clear();
                inflater.inflate(R.menu.menu_edit, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem item) {
                if (item.getItemId() == R.id.action_delete) {
                    DialogUtil.showDeleteDialog(
                            requireContext(),
                            "Deseja realmente excluir este produto?",
                            () -> {
                                dataViewModel.moverParaLixeira(produto.getId());
                                NavHostFragment.findNavController(EditFragment.this)
                                        .popBackStack();
                            }
                    );
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner());
    }

    // -------------------- UTILS --------------------
    private void setImageView(String imgUri) {
        Glide.with(requireContext())
                .load(imgUri)
                .override(350, 350)
                .placeholder(R.drawable.gradient_one)
                .error(R.drawable.up_picture)
                .into(imgUpload);
    }

    private static String timestampParaData(long ts) {
        return new SimpleDateFormat(
                "dd/MM/yyyy", new Locale("pt", "BR")
        ).format(new Date(ts));
    }
}
