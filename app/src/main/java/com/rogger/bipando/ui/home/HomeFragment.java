package com.rogger.bipando.ui.home;

import android.content.Intent;
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
import com.rogger.bipando.R;
import com.rogger.bipando.data.model.Produto;
import com.rogger.bipando.databinding.FragmentHomeBinding;
import com.rogger.bipando.ui.viewmodel.DataViewModel;

import java.util.List;

public class HomeFragment extends Fragment implements OnItemClickListener {
    private ViewFlipper viewFlipper;
    private ImageView imageView;
    private ProgressBar progressBar;
    private FragmentHomeBinding binding;
    private DataViewModel dataViewModel;
    private AdapterHome adapte;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        RecyclerView recyclerView = binding.rcViewListHome;
        viewFlipper = binding.viewFlipper;
        imageView = binding.imgHomeFragment;
        progressBar = binding.profileProgress;
        adapte = new AdapterHome(requireContext(), 10);//passando valor fixo

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
            Navigation.findNavController(view).navigate(R.id.nav_barcode_scan_fragment);
        });
        super.onViewCreated(view, savedInstanceState);
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