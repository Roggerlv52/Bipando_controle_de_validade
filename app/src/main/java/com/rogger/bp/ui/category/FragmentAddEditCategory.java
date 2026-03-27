package com.rogger.bp.ui.category;

import android.os.Bundle;
import android.view.ActionMode;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.rogger.bp.R;
import com.rogger.bp.data.model.Categoria;
import com.rogger.bp.ui.base.Utils;
import com.rogger.bp.ui.viewmodel.CategoriaViewModel;

import java.util.List;

public class FragmentAddEditCategory extends Fragment {

    private CategoriaViewModel categoriaViewModel;
    private AdapterCategory adapter;
    private ActionMode actionMode;

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(
                R.layout.fragment_add_edit_category, container, false);
        // ViewModel
        categoriaViewModel =
                new ViewModelProvider(requireActivity())
                        .get(CategoriaViewModel.class);

        // RecyclerView
        RecyclerView recyclerView = view.findViewById(R.id.recycler_view_category);
        recyclerView.setLayoutManager(
                new LinearLayoutManager(requireContext()));

        // Adapter
        adapter = new AdapterCategory(new AdapterCategory.OnCategoriaListener() {

            @Override
            public void onClick(Categoria categoria) {
                // Clique normal (opcional)
                Utils.dialogCategory(
                        requireContext(),
                        "Editar categoria",
                        nome -> {
                            Categoria c = new Categoria();
                            c.setId(categoria.getId());
                            c.setUserId(categoria.getUserId());
                            c.setNome(nome);
                            categoriaViewModel.update(c);
                        });
            }

            @Override
            public void onSelectionChanged(int total) {

                if (total > 0 && actionMode == null) {
                    actionMode = requireActivity()
                            .startActionMode(actionModeCallback);
                }

                if (actionMode != null) {
                    actionMode.setTitle(total + " selecionada(s)");
                    if (total == 0) actionMode.finish();
                }
            }
        });

        recyclerView.setAdapter(adapter);

        // Observa categorias
        categoriaViewModel.getCategories()
                .observe(getViewLifecycleOwner(), categorias -> {
                    adapter.setItems(categorias);
                });
        return view;
    }

    // 🔥 ActionMode
    private final ActionMode.Callback actionModeCallback =
            new ActionMode.Callback() {

                @Override
                public boolean onCreateActionMode(ActionMode mode, Menu menu) {
                    mode.getMenuInflater()
                            .inflate(R.menu.menu_categoria_action, menu);
                    adapter.setModoSelecao(true);
                    return true;
                }

                @Override
                public boolean onPrepareActionMode(ActionMode mode, Menu menu) {
                    return false;
                }

                @Override
                public boolean onActionItemClicked(
                        ActionMode mode, MenuItem item) {

                    if (item.getItemId() == R.id.action_delete) {
                        DialogUtil.showConfirmDialog(requireActivity(),
                                "Tem certeza que deseja excluir?", () -> {
                                    //adapter.getSelecionadas()
                                    //       .forEach(categoriaViewModel::remover);
                                    // mode.finish();
                                    List<Categoria> selecionadas =
                                            adapter.getSelecionadas();
                                    for (Categoria c : selecionadas) {
                                        categoriaViewModel.remover(c);
                                    }
                                    mode.finish();
                                });

                        return true;
                    }
                    return false;
                }

                @Override
                public void onDestroyActionMode(ActionMode mode) {
                    adapter.setModoSelecao(false);
                    actionMode = null;
                }
            };

    // Menu normal (adicionar categoria)
    @Override
    public void onViewCreated(
            @NonNull View view,
            @Nullable Bundle savedInstanceState) {

        requireActivity().addMenuProvider(
                new MenuProvider() {
                    @Override
                    public void onCreateMenu(
                            @NonNull Menu menu,
                            @NonNull MenuInflater menuInflater) {

                        menu.clear();
                        menuInflater.inflate(R.menu.category, menu);
                    }

                    @Override
                    public boolean onMenuItemSelected(
                            @NonNull MenuItem menuItem) {

                        if (menuItem.getItemId() == R.id.add_category) {

                            Utils.dialogCategory(
                                    requireContext(),
                                    "Criar categoria",
                                    nome -> {
                                        Categoria c = new Categoria();
                                        c.setNome(nome);
                                        categoriaViewModel.insert(c);
                                    });
                            return true;
                        }
                        return false;
                    }
                },
                getViewLifecycleOwner(),
                Lifecycle.State.RESUMED
        );
    }
}
