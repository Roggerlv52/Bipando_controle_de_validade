package com.rogger.bp.ui.deleteitem;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ViewFlipper;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rogger.bp.R;
import com.rogger.bp.data.model.Produto;
import com.rogger.bp.ui.gallery.ImagePikerUtil;

import java.io.File;
import java.util.List;

public class ItemDeletedFragment extends Fragment {
    private ViewFlipper viewFlipper;
    private ItemDeletedViewModel viewModel;
    private ItemDeletedAdapter adapter;
    private ImageView imageView;

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
        viewFlipper = view.findViewById(R.id.view_flipper);
        imageView = view.findViewById(R.id.img_deleted);
        // 2️⃣ RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.recycler_deleted);
        recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));

        // 3️⃣ Adapter
        adapter = new ItemDeletedAdapter(this::mostrarDialogo);
        recyclerView.setAdapter(adapter);

        // 4️⃣ Observer
        viewModel.getProdutosDeletados()
                .observe(getViewLifecycleOwner(), deleted -> {
                    adapter.submitList(deleted);
                    if (deleted != null && !deleted.isEmpty()) {
                        atualizarView(deleted);
                    } else {
                        atualizarView(null);
                    }
                });
    }
    private void atualizarView(List<Produto> dados) {
        if (dados == null || dados.isEmpty()) {
            viewFlipper.setDisplayedChild(1); // Mostra a imagem
            imageView.setImageResource(R.drawable.stop_item);
            //progressBar.setVisibility(View.GONE);
        } else {
            viewFlipper.setDisplayedChild(0); // Mostra o RecyclerView
           // progressBar.setVisibility(View.GONE);
        }
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
