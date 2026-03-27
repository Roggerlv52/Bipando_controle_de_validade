package com.rogger.bp.ui.home;

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
    private ProgressBar progressBar;
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

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        RecyclerView recyclerView = binding.rcViewListHome;
        viewFlipper = binding.viewFlipper;
        imageView = binding.imgHomeFragment;
        progressBar = binding.profileProgress;

        // Recuperar o valor do slider de dias amarelo do SharedPreferences
        int diasAmarelo = NotificationPrefs.getDays(requireContext());

        // Instanciar o AdapterHome com o valor dinâmico do slider
        adapte = new AdapterHome(requireContext(), diasAmarelo);

        recyclerView.setAdapter(adapte);
        adapte.setOnItemClickListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // Inicializar DataViewModel
        dataViewModel = new ViewModelProvider(this).get(DataViewModel.class);

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

        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        FloatingActionButton fab = binding.fab;
        fab.setOnClickListener(view1 -> {
            mostrarDialogoSelecaoCategoria();
        });
        super.onViewCreated(view, savedInstanceState);
    }

    /**
     * Exibe um diálogo para o utilizador selecionar uma categoria antes de ir para o scanner
     */
    private void mostrarDialogoSelecaoCategoria() {
        // Inicializar o CategoriaViewModel
        categoriaViewModel = new ViewModelProvider(this).get(CategoriaViewModel.class);

        // Observar as categorias apenas uma vez para evitar múltiplas aberturas de diálogo
        categoriaViewModel.getCategories().observe(getViewLifecycleOwner(), new androidx.lifecycle.Observer<List<Categoria>>() {
            @Override
            public void onChanged(List<Categoria> categorias) {
                // Remove o observer imediatamente para que o diálogo não abra novamente se a lista mudar
                categoriaViewModel.getCategories().removeObserver(this);

                if (categorias == null || categorias.isEmpty()) {
                    // Sem categorias, mostrar opção de criar
                    mostrarDialogoAdicionarCategoria();
                } else {
                    // Mostrar diálogo com spinner de categorias
                    mostrarDialogoComSpinner(categorias);
                }
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
                    // Criar a categoria
                    Categoria novaCategoria = new Categoria();
                    novaCategoria.setNome(nomeCat);
                    categoriaViewModel.insert(novaCategoria);

                    // Aguardar a inserção e navegar para o scanner com a nova categoria
                    categoriaViewModel.getCategories().observe(getViewLifecycleOwner(), new androidx.lifecycle.Observer<List<Categoria>>() {
                        @Override
                        public void onChanged(List<Categoria> categorias) {
                            if (categorias != null && !categorias.isEmpty()) {
                                // Procurar a categoria criada pelo nome
                                for (Categoria c : categorias) {
                                    if (c.getNome().equals(nomeCat)) {
                                        // Remove o observer antes de navegar
                                        categoriaViewModel.getCategories().removeObserver(this);
                                        irParaScanner(c.getId());
                                        break;
                                    }
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
            viewFlipper.setDisplayedChild(1); // Mostra a imagem
            imageView.setImageResource(R.drawable.empty_list);
            progressBar.setVisibility(View.GONE);
        } else {
            viewFlipper.setDisplayedChild(0); // Mostra o RecyclerView
            progressBar.setVisibility(View.GONE);
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
        int id = produto.getId() !=  0 ? produto.getId() :0;
        bundle = new Bundle();
        navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_content_main);
        bundle.putInt("id",id);
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
