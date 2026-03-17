package com.rogger.bipando.ui.home;

import android.app.AlertDialog;
import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Spinner;
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
import com.rogger.bipando.R;
import com.rogger.bipando.data.model.Categoria;
import com.rogger.bipando.data.model.Produto;
import com.rogger.bipando.databinding.FragmentHomeBinding;
import com.rogger.bipando.notification.NotificationPrefs;
import com.rogger.bipando.ui.base.Utils;
import com.rogger.bipando.ui.scanner.BarcodeScan;
import com.rogger.bipando.ui.viewmodel.CategoriaViewModel;
import com.rogger.bipando.ui.viewmodel.DataViewModel;

import java.util.ArrayList;
import java.util.List;

public class HomeFragment extends Fragment implements OnItemClickListener {
    private ViewFlipper viewFlipper;
    private ImageView imageView;
    private ProgressBar progressBar;
    private FragmentHomeBinding binding;
    private DataViewModel dataViewModel;
    private CategoriaViewModel categoriaViewModel;
    private AdapterHome adapte;

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
        adapte = new AdapterHome(requireContext(), 3, diasAmarelo);

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

        // Observar as categorias
        categoriaViewModel.getCategories().observe(getViewLifecycleOwner(), categorias -> {
            if (categorias == null || categorias.isEmpty()) {
                // Sem categorias, mostrar opção de criar
                mostrarDialogoAdicionarCategoria();
            } else {
                // Mostrar diálogo com spinner de categorias
                mostrarDialogoComSpinner(categorias);
            }
        });
    }

    /**
     * Exibe um diálogo com um Spinner listando as categorias disponíveis
     */
    private void mostrarDialogoComSpinner(List<Categoria> categorias) {
        AlertDialog.Builder builder = new AlertDialog.Builder(requireContext());
        builder.setTitle("Selecione uma Categoria");

        // Criar lista com opção "Adicionar categoria" no início
        List<Categoria> listaComOpcao = new ArrayList<>();
        Categoria opcaoAdicionar = new Categoria();
        opcaoAdicionar.setId(-1);
        opcaoAdicionar.setNome("+ Adicionar uma categoria");
        listaComOpcao.add(opcaoAdicionar);
        listaComOpcao.addAll(categorias);

        // Criar adapter para o spinner
        ArrayAdapter<Categoria> adapter = new ArrayAdapter<>(
                requireContext(),
                android.R.layout.simple_spinner_item,
                listaComOpcao
        );
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        // Criar e configurar o spinner
        Spinner spinner = new Spinner(requireContext());
        spinner.setAdapter(adapter);

        builder.setView(spinner);

        // Botões do diálogo
        builder.setPositiveButton("OK", (dialog, which) -> {
            Categoria selecionada = (Categoria) spinner.getSelectedItem();
            if (selecionada.getId() == -1) {
                // Opção "Adicionar categoria" foi selecionada
                mostrarDialogoAdicionarCategoria();
            } else {
                // Categoria válida selecionada, ir para o scanner
                irParaScanner(selecionada.getId());
            }
        });

        builder.setNegativeButton("Cancelar", (dialog, which) -> dialog.dismiss());

        builder.show();
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

                    // Aguardar um pouco para a categoria ser inserida e depois ir para o scanner
                    // Observar novamente as categorias para obter o ID da categoria criada
                    categoriaViewModel.getCategories().observe(getViewLifecycleOwner(), categorias -> {
                        if (categorias != null && !categorias.isEmpty()) {
                            // Procurar a categoria criada (última da lista)
                            Categoria categoriaCriada = categorias.get(categorias.size() - 1);
                            if (categoriaCriada.getNome().equals(nomeCat)) {
                                irParaScanner(categoriaCriada.getId());
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
        Navigation.findNavController(requireView()).navigate(R.id.nav_barcode_scan_fragment, bundle);
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
        String imageUrl = produto.getImagem() != null ? produto.getImagem() : "";
        String productName = produto.getNome() != null ? produto.getNome() : "";
        String barcode = produto.getCodigoBarras() != null ? produto.getCodigoBarras() : "";
        String dataFormatada = produto.getTimestamp() > 0 ? String.valueOf(produto.getTimestamp()) : "";
        String note = produto.getAnotacoes() != null ? produto.getAnotacoes() : "";

        NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_content_main);
        Bundle bundle = new Bundle();
        bundle.putString("imageUrl", imageUrl);
        bundle.putString("productName", productName);
        bundle.putString("barcode", barcode);
        bundle.putString("data", dataFormatada);
        bundle.putString("note", note);
        navController.navigate(R.id.action_nav_home_to_nav_edit_fragment, bundle);
    }
}
