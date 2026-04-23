package com.rogger.bp.ui.add;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.Navigation;

import com.bumptech.glide.Glide;
import com.rogger.bp.R;
import com.rogger.bp.data.database.FirebaseStorageDataSource;
import com.rogger.bp.data.database.ProdutoImagemDataSource;
import com.rogger.bp.data.model.Categoria;
import com.rogger.bp.data.model.Produto;
import com.rogger.bp.data.model.ProdutoImagem;
import com.rogger.bp.data.repository.ProdutoImagemRepository;
import com.rogger.bp.databinding.FragmentAddBinding;
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
    private FragmentAddBinding binding;
    private long timestamp;
    private Produto produto;
    private File photoFile;
    private boolean confirmed = false;
    private boolean carregandoSpinner = true;
    private int categoriaId = -1;
    private String imagemUrlGlobal = null;
    private ArrayAdapter<Categoria> categoriaAdapter;
    private List<Categoria> listaCategorias = new ArrayList<>();
    private ImagePikerUtil cameraUtil;
    private ActivityResultLauncher<Intent> cameraLauncher;
    private CategoriaViewModel categoriaViewModel;
    private DataViewModel dataViewModel;
    private ProdutoImagemRepository produtoImagemRepository;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentAddBinding.inflate(inflater, container, false);

        categoriaViewModel = new ViewModelProvider(requireActivity()).get(CategoriaViewModel.class);
        dataViewModel = new ViewModelProvider(requireActivity()).get(DataViewModel.class);
        produtoImagemRepository = ProdutoImagemRepository.getInstance();
        produto = new Produto();
        cameraUtil = new ImagePikerUtil();

        String dataAtual = Utils.getCurrentDateFormatted();
        binding.datePickerBtnAdd.setText(dataAtual);
        binding.txtAddData.setText(dataAtual);
        timestamp = Utils.getCurrentTimestamp();

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        configurarSpinner();
        configurarGalleryResult();
        configurarCamera();
        configurarListeners();
        configurarMenu();

        // Recebe o código de barras vindo do scanner
        Bundle args = getArguments();
        if (args != null) {
            String barcode = args.getString("key_barcode");
            categoriaId = args.getInt("categoria_id", -1);

            if (barcode != null && !barcode.isEmpty()) {
                binding.txtAddBarcode.setText(barcode);
                // ─── NOVO FLUXO: consulta imagem global antes de exibir formulário
                consultarImagemGlobal(barcode);
            }
        }
    }

    private void consultarImagemGlobal(@NonNull String barcode) {
        mostrarProgressoConsulta(true);

        produtoImagemRepository.buscarPorCodigoBarras(barcode,
                new ProdutoImagemDataSource.BuscaCallback() {

                    @Override
                    public void onEncontrado(@NonNull ProdutoImagem produtoImagem) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            mostrarProgressoConsulta(false);
                            preencherComImagemExistente(produtoImagem);
                        });
                    }

                    @Override
                    public void onNaoEncontrado() {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            mostrarProgressoConsulta(false);
                        });
                    }

                    @Override
                    public void onErro(@NonNull Exception e) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            mostrarProgressoConsulta(false);
                        });
                    }
                });
    }

    private void preencherComImagemExistente(@NonNull ProdutoImagem produtoImagem) {
        imagemUrlGlobal = produtoImagem.getImagemUrl();
        if (produtoImagem.temImagem()) {
            Glide.with(this)
                    .load(imagemUrlGlobal)
                    .override(500, 500)
                    .placeholder(R.drawable.carregando)
                    .error(R.drawable.imagem_error)
                    .centerCrop()
                    .into(binding.fragmentImgAdd);
            binding.fragmentImgAdd.setClickable(false);
        }
        if (produtoImagem.temNome()) {
            binding.edtNameFragmentAdd.setText(produtoImagem.getNomeProduto());
            binding.edtNameFragmentAdd.setSelection(
                    binding.edtNameFragmentAdd.getText().length());
        }
    }

    private void saveData() {
        produto.setNome(binding.edtNameFragmentAdd.getText().toString().trim());
        produto.setAnotacoes(binding.editFragmentNoteAdd.getText().toString());
        produto.setTimestamp(timestamp);
        produto.setCodigoBarras(binding.txtAddBarcode.getText().toString().trim());
        if (imagemUrlGlobal != null && !imagemUrlGlobal.isEmpty()) {
            produto.setImagem(imagemUrlGlobal);
            dataViewModel.insert(produto, null);
            return;
        }

        if (photoFile != null && !produto.getCodigoBarras().isEmpty()) {
            produto.setImagem(photoFile.getAbsolutePath());
            mostrarProgressoUpload(true);

            dataViewModel.insert(produto, new FirebaseStorageDataSource.UploadCallback() {

                @Override
                public void onProgresso(int porcentagem) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() ->
                            binding.progressUploadAdd.setProgress(porcentagem));
                }

                @Override
                public void onSucesso(String urlDownload) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        mostrarProgressoUpload(false);
                    });
                }

                @Override
                public void onErro(Exception e) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        mostrarProgressoUpload(false);
                    });
                }
            });
            return;
        }
        dataViewModel.insert(produto, null);
    }

    private void configurarSpinner() {
        categoriaViewModel.getCategories().observe(getViewLifecycleOwner(), categories -> {
            listaCategorias.clear();
            if (categories != null) listaCategorias.addAll(categories);

            categoriaAdapter = new ArrayAdapter<>(
                    requireContext(),
                    android.R.layout.simple_spinner_item,
                    listaCategorias);
            categoriaAdapter.setDropDownViewResource(
                    android.R.layout.simple_spinner_dropdown_item);
            binding.spinnerAdd.setAdapter(categoriaAdapter);

            if (categoriaId != -1) selecionarCategoriaPorId(categoriaId);
            carregandoSpinner = false;
        });

        binding.spinnerAdd.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view,
                                       int position, long id) {
                if (carregandoSpinner) return;
                produto.setCategoryId(listaCategorias.get(position).getId());
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });
    }

    private void configurarGalleryResult() {
        getParentFragmentManager().setFragmentResultListener(
                "gallery_result",
                getViewLifecycleOwner(),
                (key, bundle) -> {
                    try {
                        Uri img = Uri.parse(bundle.getString("imageUri"));
                        File file = ImagePikerUtil.createImageFile(requireContext());
                        photoFile = ImageUtils.processImage(requireContext(), img, file);
                        // Imagem local selecionada — descarta qualquer URL global anterior
                        imagemUrlGlobal = null;
                        Glide.with(this).load(photoFile).override(500, 500).centerCrop().into(binding.fragmentImgAdd);
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                });
    }

    private void configurarCamera() {
        cameraLauncher = cameraUtil.register(this, new CameraCallback() {
            @Override
            public void onImageCaptured(@NonNull Uri imageUri, @NonNull File imageFile) {
                try {
                    photoFile = ImageUtils.processImage(requireContext(), imageUri, imageFile);
                    imagemUrlGlobal = null;
                    Glide.with(this).load(photoFile).override(500, 500).centerCrop().into(binding.fragmentImgAdd);
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }

            @Override
            public void onCancel() {
            }

            @Override
            public void onError(@NonNull Exception e) {
                Toast.makeText(getContext(), e.getMessage(), Toast.LENGTH_LONG).show();
            }
        });
    }

    private void configurarListeners() {
        binding.datePickerBtnAdd.setOnClickListener(v ->
                Utils.showDatePicker(requireContext(), (ts, dataFormatada) -> {
                    binding.datePickerBtnAdd.setText(dataFormatada);
                    binding.txtAddData.setText(dataFormatada);
                    timestamp = ts;
                }));

        binding.txtAddBarcode.setOnClickListener(v ->
                startImageBarcode(binding.txtAddBarcode.getText().toString()));

        binding.fragmentImgAdd.setOnClickListener(v -> {
            if (imagemUrlGlobal != null) return;
            confirmed = false;
            ShowSelectDialog.show(getContext(), new ShowSelectDialog.selectedCallback() {
                @Override
                public void openGallery() {
                    Navigation.findNavController(requireView())
                            .navigate(R.id.action_addFragment_to_nav_gallery_fragment);
                }

                @Override
                public void openCamera() {
                    if (photoFile != null) photoFile.delete();
                    cameraUtil.openCamera(requireContext(), cameraLauncher);
                }
            });
        });

        binding.btnFgmSaveAdd.setOnClickListener(v -> {
            if (Utils.validEditText(binding.edtNameFragmentAdd)) {
                saveData();
                confirmed = true;
                Toast.makeText(getContext(), "Produto salvo com sucesso", Toast.LENGTH_SHORT).show();
                Navigation.findNavController(requireView()).popBackStack();
            }
        });
    }

    private void configurarMenu() {
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
                                ImagePikerUtil.cleanUpTempFiles(photoFile);
                                Navigation.findNavController(requireView()).popBackStack();
                            });
                    return true;
                }
                return false;
            }
        }, getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    private void mostrarProgressoConsulta(boolean mostrar) {
        binding.progressUploadAdd.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        binding.btnFgmSaveAdd.setEnabled(!mostrar);
    }

    private void mostrarProgressoUpload(boolean mostrar) {
        binding.progressUploadAdd.setVisibility(mostrar ? View.VISIBLE : View.GONE);
        binding.progressUploadAdd.setIndeterminate(!mostrar);
        binding.btnFgmSaveAdd.setEnabled(!mostrar);
    }

    private void startImageBarcode(String barcode) {
        Intent intent = new Intent(getContext(), ImageBarcode.class);
        intent.putExtra("keyBarcode", barcode);
        startActivity(intent);
    }

    private void selecionarCategoriaPorId(int id) {
        for (int i = 0; i < listaCategorias.size(); i++) {
            if (listaCategorias.get(i).getId() == id) {
                binding.spinnerAdd.setSelection(i);
                produto.setCategoryId(id);
                return;
            }
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (!confirmed && photoFile != null && imagemUrlGlobal == null) {
            ImagePikerUtil.cleanUpTempFiles(photoFile);
        }
    }
}