package com.rogger.bipando.ui.home;

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
import androidx.navigation.fragment.NavHostFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.rogger.bipando.R;
import com.rogger.bipando.data.model.Categoria;
import com.rogger.bipando.data.model.Produto;
import com.rogger.bipando.databinding.FragmentHomeBinding;
import com.rogger.bipando.notification.NotificationPrefs;
import com.rogger.bipando.ui.base.DialogUtil;
import com.rogger.bipando.ui.commun.SharedPreferencesManager;
import com.rogger.bipando.ui.viewmodel.CategoriaViewModel;
import com.rogger.bipando.ui.viewmodel.DataViewModel;

import java.util.List;

public class HomeFragment extends Fragment {
    private ViewFlipper viewFlipper;
    private ImageView imageView;
    private ProgressBar progressBar;
    private FragmentHomeBinding binding;
    private DataViewModel dataViewModel;
    private CategoriaViewModel categoryViewModel;
    private AdapterHome adapter;

    public static HomeFragment newInstance() {
        return new HomeFragment();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        RecyclerView recyclerView = binding.rcViewListHome;
        viewFlipper = binding.viewFlipper;
        imageView = binding.imgHomeFragment;
        progressBar = binding.profileProgress;
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        adapter = new AdapterHome(new AdapterHome.OnItemClickListener() {
            final Bundle bundle = new Bundle();

            @Override
            public void onItemClick(Produto produto) {
                bundle.putInt("id", produto.getId());
                NavHostFragment
                        .findNavController(HomeFragment.this).navigate(R.id.nav_edit_fragment, bundle);
            }

            @Override
            public void onImageClick(Produto produto) {
                bundle.putString("imageUri", produto.getImagem());
                NavHostFragment
                        .findNavController(HomeFragment.this).navigate(R.id.nav_show_fragment, bundle);
            }
        }, NotificationPrefs.getDays(requireContext())
        );
        recyclerView.setAdapter(adapter);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        dataViewModel = new ViewModelProvider(requireActivity()).get(DataViewModel.class);

        FloatingActionButton fab = binding.fab;
        categoryViewModel = new ViewModelProvider(requireActivity()).get(CategoriaViewModel.class);
        categoryViewModel.getCategories().observe(getViewLifecycleOwner(), categories -> {

            fab.setOnClickListener(view1 -> {
                List<Categoria> categorias =
                        categoryViewModel.getCategories().getValue();

                // 🔴 Não há categorias
                if (categorias == null || categorias.isEmpty()) {
                    Toast.makeText(
                            requireContext(),
                            "Crie uma categoria antes de adicionar produtos",
                            Toast.LENGTH_SHORT
                    ).show();
                    return;
                }

                // ✅ Há categorias → mostra dialog
                DialogUtil.show(
                        requireContext(),
                        categorias,
                        categoriaSelecionada -> {

                            Bundle bundle = new Bundle();
                            bundle.putString("categoria_nome", categoriaSelecionada.getNome());
                            bundle.putInt("categoria_id", categoriaSelecionada.getId());
                            NavHostFragment
                                    .findNavController(this)
                                    .navigate(R.id.nav_barcode_scan_fragment, bundle);
                        }
                );
            });
        });

        dataViewModel.getProdutos().observe(getViewLifecycleOwner(), produtos -> {
            if (produtos != null) {
                adapter.setItems(produtos);
                atualizarView(produtos);
            }
        });

    }

    private void atualizarView(List<Produto> dados) {
        if (dados.isEmpty()) {
            viewFlipper.setDisplayedChild(1); // Mostra a imagem
            imageView.setImageResource(R.drawable.empty_list);
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
}