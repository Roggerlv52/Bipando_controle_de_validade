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
import android.widget.Spinner;
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

    private static final String TAG = "AddFragment";
    private FragmentAddBinding binding;
    // ── Estado ───────────────────────────────────────────────────
    private long timestamp;
    private Produto produto;
    private File photoFile;
    private boolean confirmed = false;
    private boolean carregandoSpinner = true;
    private int categoriaId = -1;

    /**
     * URL da imagem vinda do banco global (imagens_produtos).
     * Se preenchida, significa que a imagem já existe e NÃO será feito upload.
     */
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
    public View onCreateView(@NonNull LayoutInflater inflater,ViewGroup container,Bundle savedInstanceState) {
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

    // ============================================================
    // NOVO FLUXO — CONSULTA DE IMAGEM GLOBAL
    // ============================================================

    /**
     * Consulta a coleção global "imagens_produtos" pelo código de barras.
     * <p>
     * Resultado A — barcode JÁ existe:
     * → Preenche imgUpload com a imagem via Glide (sem download manual)
     * → Preenche edtName com o nome sugerido (editável pelo usuário)
     * → Bloqueia o botão de imagem (não precisa capturar nova foto)
     * → Ao salvar, NÃO faz upload — usa a imagemUrlGlobal diretamente
     * <p>
     * Resultado B — barcode NÃO existe:
     * → Formulário permanece em branco (comportamento original)
     * → Usuário captura imagem normalmente
     * → Ao salvar, faz upload global em imagens_produtos/{barcode}/imagem.jpg
     * e depois grava o documento no Firestore
     */
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
                            // Formulário permanece em branco — usuário cadastra normalmente
                            Log.d(TAG, "Barcode novo. Usuário deve capturar imagem.");
                        });
                    }

                    @Override
                    public void onErro(@NonNull Exception e) {
                        if (!isAdded()) return;
                        requireActivity().runOnUiThread(() -> {
                            mostrarProgressoConsulta(false);
                            Log.e(TAG, "Erro ao consultar imagem global: " + e.getMessage());
                            // Falha silenciosa — formulário permanece em branco,
                            // usuário pode continuar normalmente
                        });
                    }
                });
    }

    /**
     * Preenche o formulário com os dados da imagem já existente no banco global.
     * Chamado apenas quando o barcode já foi cadastrado por algum usuário.
     */
    private void preencherComImagemExistente(@NonNull ProdutoImagem produtoImagem) {
        imagemUrlGlobal = produtoImagem.getImagemUrl();
        if (produtoImagem.temImagem()) {
            Glide.with(this)
                    .load(imagemUrlGlobal)
                    .placeholder(R.drawable.carregando)
                    .error(R.drawable.imagem_error)
                    .centerCrop()
                    .into(binding.fragmentImgAdd);

            // Bloqueia o botão de imagem — não faz sentido trocar uma imagem global
            binding.fragmentImgAdd.setClickable(false);
            binding.fragmentImgAdd.setAlpha(0.85f);
        }

        // Preenche o nome sugerido se disponível (editável — usuário pode ajustar)
        if (produtoImagem.temNome()) {
            binding.edtNameFragmentAdd.setText(produtoImagem.getNomeProduto());
            binding.edtNameFragmentAdd.setSelection(
                    binding.edtNameFragmentAdd.getText().length());
        }

        Log.d(TAG, "Formulário preenchido com imagem global: " + produtoImagem.getCodigoBarras());
    }

    /**
     * Salva o produto no banco.
     * <p>
     * Caso A — imagemUrlGlobal preenchida (barcode já existia):
     * → Salva produto com a URL global diretamente. Zero upload.
     * <p>
     * Caso B — imagemUrlGlobal nula + photoFile disponível (barcode novo):
     * → Faz upload global em imagens_produtos/{barcode}/imagem.jpg
     * → Grava documento em imagens_produtos/{barcode}
     * → Salva produto com a URL retornada
     * <p>
     * Caso C — sem imagem nenhuma:
     * → Salva produto normalmente sem imagem
     */
    private void saveData() {
        produto.setNome(binding.edtNameFragmentAdd.getText().toString().trim());
        produto.setAnotacoes(binding.editFragmentNoteAdd.getText().toString());
        produto.setTimestamp(timestamp);
        produto.setCodigoBarras(binding.txtAddBarcode.getText().toString().trim());

        // Caso A: imagem global já existia — usa URL direto, sem upload
        if (imagemUrlGlobal != null && !imagemUrlGlobal.isEmpty()) {
            produto.setImagem(imagemUrlGlobal);
            dataViewModel.insert(produto, null);
            Log.d(TAG, "Produto salvo com imagem global existente. Sem upload.");
            return;
        }

        // Caso B: barcode novo com imagem local capturada — faz upload global
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
                        Log.d(TAG, "Upload global concluído: " + urlDownload);
                    });
                }

                @Override
                public void onErro(Exception e) {
                    if (!isAdded()) return;
                    requireActivity().runOnUiThread(() -> {
                        mostrarProgressoUpload(false);
                        Log.e(TAG, "Falha no upload global: " + e.getMessage());
                    });
                }
            });
            return;
        }

        // Caso C: sem imagem — salva produto normalmente
        dataViewModel.insert(produto, null);
        Log.d(TAG, "Produto salvo sem imagem.");
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
                        binding.fragmentImgAdd.setImageURI(Uri.fromFile(photoFile));
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
                    // Imagem local capturada — descarta qualquer URL global anterior
                    imagemUrlGlobal = null;
                    binding.fragmentImgAdd.setImageURI(Uri.fromFile(photoFile));
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
            // Só permite trocar imagem se NÃO veio do banco global
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
        // Limpa arquivo temporário local apenas se o usuário cancelou
        // e a imagem não veio do banco global
        if (!confirmed && photoFile != null && imagemUrlGlobal == null) {
            ImagePikerUtil.cleanUpTempFiles(photoFile);
        }
    }
}