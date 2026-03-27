package com.rogger.bp.ui.add;

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
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

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
import com.rogger.bp.ui.scanner.ImageBarcode;
import com.rogger.bp.ui.viewmodel.CategoriaViewModel;
import com.rogger.bp.ui.viewmodel.DataViewModel;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

public class AddFragment extends Fragment {
    private static final int REQUEST_IMAGE_CAPTURE = 1;
    private TextView txtBarcode, txtData;
    private EditText edtName, edtNote;
    private long timestamp;
    private Button btnData, btnSave;
    private ImageView imgUpload;
    private Uri newImageUri;
    private CategoriaViewModel categoriaViewModel;
    private DataViewModel dataViewModel;
    private Produto produto;
    private Spinner spinner;
    private ArrayAdapter<Categoria> categoriaAdapter;
    private List<Categoria> listaCategorias = new ArrayList<>();
    private ActivityResultLauncher<Intent> cameraLauncher;
    private boolean carregandoSpinner = true;
    private ImagePikerUtil cameraUtil;
    private boolean confirmed;
    private File photoFile;
    private int categoriaId = -1;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_add, container, false);
        categoriaViewModel = new ViewModelProvider(requireActivity()).get(CategoriaViewModel.class);
        dataViewModel = new ViewModelProvider(requireActivity()).get(DataViewModel.class);

        imgUpload = view.findViewById(R.id.fragment_img_add);
        txtBarcode = view.findViewById(R.id.txt_add_barcode);
        txtData = view.findViewById(R.id.txt_add_data);
        edtName = view.findViewById(R.id.edt_name_fragment_add);
        btnData = view.findViewById(R.id.datePickerBtn_add);
        edtNote = view.findViewById(R.id.edit_fragment_note_add);
        spinner = view.findViewById(R.id.spinner_add);
        btnSave = view.findViewById(R.id.btn_fgm_save_add);
        produto = new Produto();
        cameraUtil = new ImagePikerUtil();
        btnData.setText(Utils.getCurrentDateFormatted());
        txtData.setText(Utils.getCurrentDateFormatted());
        return view;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        Bundle args = getArguments();
        if (args != null) {
            String barcode = args.getString("key_barcode");
            categoriaId = args.getInt("categoria_id", -1);

            txtBarcode.setText(barcode);
        }
        categoriaViewModel.getCategories().observe(getViewLifecycleOwner(), categories -> {

            listaCategorias.clear();
            // (Opcional) placeholder
            if (categories != null) {
                listaCategorias.addAll(categories);
            }
            categoriaAdapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    listaCategorias
            );
            categoriaAdapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item
            );
            spinner.setAdapter(categoriaAdapter);
            // 🔥 AGORA SIM pode selecionar
            if (categoriaId != -1) {
                selecionarCategoriaPorId(categoriaId);
            }
            carregandoSpinner = false;
        });

        // Recupera o argumento
        getParentFragmentManager().setFragmentResultListener(
                "gallery_result",
                getViewLifecycleOwner(),
                (key, bundle) -> {

                    try {
                        Uri img = Uri.parse(bundle.getString("imageUri"));
                        File file = ImagePikerUtil.createImageFile(requireContext());
                        photoFile = ImageUtils.processImage(
                                requireContext(),
                                img,
                                file
                        );
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    imgUpload.setImageURI(Uri.fromFile(photoFile));
                }
        );
//
        cameraLauncher = cameraUtil.register(this, new CameraCallback() {

            @Override
            public void onImageCaptured(@NonNull Uri imageUri, @NonNull File imageFile) {
                try {
                    photoFile = ImageUtils.processImage(requireContext(), imageUri, imageFile);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
                imgUpload.setImageURI(Uri.fromFile(photoFile));
            }

            @Override
            public void onCancel() {
            }

            @Override
            public void onError(@NonNull Exception e) {
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
                Log.e("CameraError", e.getMessage());
            }
        });
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(
                    AdapterView<?> parent,
                    View view,
                    int position,
                    long id) {

                if (carregandoSpinner) return;

                Categoria selecionada = listaCategorias.get(position);

                // ✅ Salva no produto
                produto.setCategoryId(selecionada.getId());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });


        btnData.setOnClickListener(v -> {
            Utils.showDatePicker(requireContext(), new Utils.OnDateSelectedListener() {
                @Override
                public void onDateSelected(long timestamp, String dataFormatada) {
                    btnData.setText(dataFormatada);
                    txtData.setText(dataFormatada);
                    AddFragment.this.timestamp = timestamp;
                }
            });
        });
        txtBarcode.setOnClickListener(view1 -> {
            startImageBarcode(txtBarcode.getText().toString());
        });
        imgUpload.setOnClickListener(v -> {
            confirmed = false;
            ShowSelectDialog.show(getContext(), new ShowSelectDialog.selectedCallback() {

                @Override
                public void openGallery() {
                    Navigation.findNavController(requireView()).navigate(R.id.action_addFragment_to_nav_gallery_fragment);
                }

                @Override
                public void openCamera() {
                    cameraUtil.openCamera(requireContext(), cameraLauncher);
                    if (photoFile != null) {
                        photoFile.delete();
                    }
                }
            });
        });
        btnSave.setOnClickListener(v -> {
            if (Utils.validEditText(edtName)) {
                saveData();
                confirmed = true;
                Toast.makeText(getContext(), "Produto salvo com sucesso", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).popBackStack();
            }
        });

        requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear();
                menuInflater.inflate(R.menu.menu_edit, menu);
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                if (menuItem.getItemId() == R.id.action_delete) {
                    DialogUtil.showDeleteDialog(
                            requireContext(),
                            "Deseja realmente excluir este produto?",
                            () -> {
                                //dataViewModel.moverParaLixeira(produto.getId());
                                ImagePikerUtil.cleanUpTempFiles(photoFile);
                                Navigation.findNavController(requireView()).popBackStack();
                            }
                    );
                    return true;
                }
                return false;
            }

        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    private void saveData() {
        produto.setNome(edtName.getText().toString().trim());
        produto.setAnotacoes(edtNote.getText().toString());
        produto.setTimestamp(timestamp);
        produto.setCodigoBarras(txtBarcode.getText().toString());
        if (photoFile != null) {
            produto.setImagem(photoFile.getAbsolutePath());
        }
        // ✅ Só salva imagem se o usuário selecionou, passando o callback para upload no Storage
        dataViewModel.insert(produto, new FirebaseStorageDataSource.UploadCallback() {
            @Override
            public void onProgresso(int porcentagem) {
                Log.d("AddFragment", "Upload progresso: " + porcentagem + "%");
            }

            @Override
            public void onSucesso(String urlDownload) {
                Log.d("AddFragment", "Upload concluído: " + urlDownload);
            }

            @Override
            public void onErro(Exception e) {
                Log.e("AddFragment", "Falha no upload da imagem: " + e.getMessage());
            }
        });
    }

    private void startImageBarcode(String barcode) {
        Intent intent = new Intent(getContext(), ImageBarcode.class);
        intent.putExtra("keyBarcode", barcode);
        startActivity(intent);
    }

    private void selecionarCategoriaPorId(int id) {
        for (int i = 0; i < listaCategorias.size(); i++) {
            if (listaCategorias.get(i).getId() == id) {
                spinner.setSelection(i);
                produto.setCategoryId(id);
                return;
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (!confirmed && photoFile != null) {
            ImagePikerUtil.cleanUpTempFiles(photoFile);
        }
    }
}
