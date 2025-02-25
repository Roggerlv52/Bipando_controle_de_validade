package com.rogger.bipando.ui.deleteitem;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rogger.bipando.R;
import com.rogger.bipando.data.model.Produto;
import com.rogger.bipando.ui.gallery.ImagePikerUtil;

import java.io.File;

public class ItemDeletedFragment extends Fragment {

    private ItemDeletedViewModel viewModel;
    private ItemDeletedAdapter adapter;

    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_item_deleted, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        // 1️⃣ ViewModel
        viewModel = new ViewModelProvider(this)
                .get(ItemDeletedViewModel.class);

        // 2️⃣ RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.recycler_deleted);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // 3️⃣ Adapter
        adapter = new ItemDeletedAdapter(this::mostrarDialogo);
        recyclerView.setAdapter(adapter);

        // 4️⃣ Observer
        viewModel.getProdutosDeletados()
                .observe(getViewLifecycleOwner(), deleted -> {
                    Log.d("TAG", "Produtos deletados: " + deleted.toString());
                    adapter.submitList(deleted);
                });
    }

    private void mostrarDialogo(Produto produto) {
        new AlertDialog.Builder(requireContext())
                .setTitle("Item deletado")
                .setMessage("O que deseja fazer?")
                .setNegativeButton("Cancelar", null)
                .setNeutralButton("Restaurar", (d, w) ->
                        viewModel.restaurar(produto.getId()))
                .setPositiveButton("Excluir definitivamente", (d, w) -> {
                    viewModel.remover(produto.getId());
                    ImagePikerUtil.cleanUpTempFiles(new File(produto.getImagem()));
                })
                .show();
    }
}
