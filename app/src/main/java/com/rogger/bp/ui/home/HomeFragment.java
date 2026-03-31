package com.rogger.bp.ui.home;

import static com.rogger.bp.ui.home.CustomProgressBarKt.hideLoadingDialog;
import static com.rogger.bp.ui.home.CustomProgressBarKt.showLoadingDialog;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.navigation.NavController;
import androidx.navigation.Navigation;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.rogger.bp.R;
import com.rogger.bp.data.model.Categoria;
import com.rogger.bp.data.model.Produto;
import com.rogger.bp.databinding.FragmentHomeBinding;
import com.rogger.bp.notification.NotificationPrefs;
import com.rogger.bp.ui.base.CategoriaDialogUtil;
import com.rogger.bp.ui.base.Utils;
import com.rogger.bp.ui.viewmodel.CategoriaViewModel;
import com.rogger.bp.ui.viewmodel.DataViewModel;

import java.util.List;

public class HomeFragment extends Fragment implements OnItemClickListener {
    private ViewFlipper viewFlipper;
    private ImageView imageView;
    private FragmentHomeBinding binding;
    private DataViewModel dataViewModel;
    private CategoriaViewModel categoriaViewModel;
    private AdapterHome adapte;
    private NavController navController;
    private Bundle bundle;

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {


        binding = FragmentHomeBinding.inflate(inflater, container, false);

        RecyclerView recyclerView = binding.rcViewListHome;
        viewFlipper = binding.viewFlipper;
        imageView = binding.imgHomeFragment;

        // Recuperar o valor do slider de dias amarelo do SharedPreferences
        int diasAmarelo = NotificationPrefs.getDays(requireContext());

        // Instanciar o AdapterHome com o valor dinâmico do slider
        adapte = new AdapterHome(requireContext(), diasAmarelo, this);

        recyclerView.setAdapter(adapte);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Inicializar DataViewModel
        dataViewModel = new ViewModelProvider(this).get(DataViewModel.class);

        // ✅ Observar o estado de carregamento (Firebase Sync)
        dataViewModel.getIsLoading().observe(getViewLifecycleOwner(), isLoading -> {
            if (Boolean.TRUE.equals(isLoading)) {
                showLoadingDialog(requireContext());
            } else {
                hideLoadingDialog();
            }
        });

        // Observar os dados do banco de dados
        dataViewModel.getProdutos().observe(getViewLifecycleOwner(), produtos -> {
            if (produtos != null && !produtos.isEmpty()) {
                adapte.setDados(produtos);
                adapte.ordenarPorDiferencaDeDias();
                atualizarView(produtos);
            } else {
                atualizarView(null);
            }
        });

        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        // ✅ ViewModel inicializado uma única vez aqui, não dentro de mostrarDialogoSelecaoCategoria()
        categoriaViewModel = new ViewModelProvider(this).get(CategoriaViewModel.class);

        FloatingActionButton fab = binding.fab;
        fab.setOnClickListener(view1 -> {
            mostrarDialogoSelecaoCategoria();
        });
        super.onViewCreated(view, savedInstanceState);
    }

    /**
     * Exibe um diálogo para o utilizador selecionar uma categoria antes de ir para o scanner.
     * Usa observeOnce para garantir que o diálogo abre apenas uma vez por clique,
     * sem acumular observers a cada chamada.
     */
    private void mostrarDialogoSelecaoCategoria() {
        observeOnce(categoriaViewModel.getCategories(), categorias -> {
            if (categorias == null || categorias.isEmpty()) {
                mostrarDialogoAdicionarCategoria();
            } else {
                mostrarDialogoComSpinner(categorias);
            }
        });
    }

    /**
     * Observa um LiveData apenas uma vez e remove o observer automaticamente.
     */
    private <T> void observeOnce(androidx.lifecycle.LiveData<T> liveData, androidx.lifecycle.Observer<T> observer) {
        liveData.observe(getViewLifecycleOwner(), new androidx.lifecycle.Observer<T>() {
            @Override
            public void onChanged(T t) {
                liveData.removeObserver(this);
                observer.onChanged(t);
            }
        });
    }

    /**
     * Exibe um diálogo com um Spinner listando as categorias disponíveis
     */
    private void mostrarDialogoComSpinner(List<Categoria> categorias) {
        CategoriaDialogUtil.mostrarDialogo(
                requireContext(),
                categorias,
                new CategoriaDialogUtil.CategoriaCallback() {
                    @Override
                    public void onCategoriaSelecionada(int categoriaId) {
                        irParaScanner(categoriaId);
                    }

                    @Override
                    public void onAdicionarCategoria() {
                        mostrarDialogoAdicionarCategoria();
                    }
                }
        );
    }

    /**
     * Exibe um diálogo para o utilizador criar uma nova categoria
     */
    private void mostrarDialogoAdicionarCategoria() {
        Utils.dialogCategory(
                requireContext(),
                "Criar nova categoria",
                nomeCat -> {
                    Categoria novaCategoria = new Categoria();
                    novaCategoria.setNome(nomeCat);
                    categoriaViewModel.insert(novaCategoria);

                    // ✅ observeOnce evita múltiplas navegações se o LiveData emitir mais de uma vez
                    observeOnce(categoriaViewModel.getCategories(), categorias -> {
                        if (categorias != null) {
                            for (Categoria c : categorias) {
                                if (c.getNome().equals(nomeCat)) {
                                    irParaScanner(c.getId());
                                    break;
                                }
                            }
                        }
                    });
                }
        );
    }

    /**
     * Navega para o BarcodeScanFragment passando o ID da categoria
     */
    private void irParaScanner(int categoriaId) {
        Bundle bundle = new Bundle();
        bundle.putInt("categoria_id", categoriaId);
        if (getView() != null) {
            Navigation.findNavController(requireView()).navigate(R.id.nav_barcode_scan_fragment, bundle);
        }
    }

    private void atualizarView(List<Produto> dados) {
        if (dados == null || dados.isEmpty()) {
            viewFlipper.setDisplayedChild(1); // Mostra a imagem de lista vazia
            imageView.setImageResource(R.drawable.lista_vazia);
            // ✅ Não escondemos o progressBar aqui, deixamos o observer de isLoading cuidar disso
        } else {
            viewFlipper.setDisplayedChild(0); // Mostra o RecyclerView
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onItemClick(int position, List<Produto> data) {
        if (data == null || position >= data.size()) {
            Toast.makeText(getContext(), "Erro ao carregar produto", Toast.LENGTH_SHORT).show();
            return;
        }

        Produto produto = data.get(position);

        // Verificações de null
        int id = produto.getId() != 0 ? produto.getId() : 0;
        bundle = new Bundle();
        navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_content_main);
        bundle.putInt("id", id);
        navController.navigate(R.id.action_nav_home_to_nav_edit_fragment, bundle);
    }

    @Override
    public void onImageClick(String uri) {
        bundle = new Bundle();
        navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_content_main);
        bundle.putString("imageUri", uri);
        navController.navigate(R.id.action_nav_home_to_nav_show_fragment, bundle);
    }
}