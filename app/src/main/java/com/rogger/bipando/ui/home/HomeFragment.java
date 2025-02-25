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
import com.rogger.bipando.database.DatabaseTest;
import com.rogger.bipando.database.Registro;
import com.rogger.bipando.databinding.FragmentHomeBinding;
import com.rogger.bipando.ui.scanner.BarcodeScan;

import java.util.List;

public class HomeFragment extends Fragment implements OnItemClickListener {
    private ViewFlipper viewFlipper;
    private ImageView imageView;
    private ProgressBar progressBar;
    private FragmentHomeBinding binding;

    public View onCreateView(@NonNull LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        HomeViewModel homeViewModel =
                new ViewModelProvider(this).get(HomeViewModel.class);
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        View root = binding.getRoot();

        RecyclerView recyclerView = binding.rcViewListHome;
        viewFlipper = binding.viewFlipper;
        imageView = binding.imgHomeFragment;
        progressBar = binding.profileProgress;
        AdapterHome adapte = new AdapterHome(requireContext(), 3, 10);

        List<Registro> dados= DatabaseTest.gerarDadosFicticios(62);
        recyclerView.setAdapter(adapte);
        adapte.setDados(dados);
        adapte.ordenarPorDiferencaDeDias();
        adapte.setOnItemClickListener(this);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        atualizarView(dados);
        return root;
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        FloatingActionButton fab = binding.fab;
        fab.setOnClickListener(view1 -> {
            Intent intent = new Intent(getActivity(), BarcodeScan.class);
            startActivity(intent);
        });
        super.onViewCreated(view, savedInstanceState);
    }

    private void atualizarView(List<Registro> dados) {
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

    @Override
    public void onItemClick(int position, List<Registro> data) {
        Registro registro = data.get(position);
        NavController navController = Navigation.findNavController(getActivity(), R.id.nav_host_fragment_content_main);
        Bundle bundle = new Bundle();
        bundle.putString("imageUrl",registro.setUri);
        bundle.putString("productName",registro.setname);
        bundle.putString("barcode",registro.setbarcod);
        bundle.putString("data",registro.setdate);
        bundle.putString("note",registro.setnote);
        navController.navigate(R.id.action_nav_home_to_nav_edit_fragment,bundle);
    }
}