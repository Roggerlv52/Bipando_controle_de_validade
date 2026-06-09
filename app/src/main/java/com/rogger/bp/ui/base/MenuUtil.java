package com.rogger.bp.ui.base;

import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;

import androidx.annotation.IdRes;
import androidx.annotation.NonNull;
import androidx.core.view.MenuProvider;
import androidx.fragment.app.Fragment;
import androidx.lifecycle.Lifecycle;

public final class MenuUtil {

    private MenuUtil() { /* utilitário estático, não instanciável */ }

    /**
     * ✅ NOVA FUNÇÃO: Limpa completamente qualquer item de menu da Toolbar
     * (esconde a pesquisa, os três pontinhos, etc.) enquanto este fragmento estiver ativo.
     * O menu da atividade é restaurado automaticamente ao voltar.
     */
    public static void clearMenu(@NonNull Fragment fragment) {
        fragment.requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                menu.clear(); // Limpa todos os botões inflados da Toolbar
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                return false;
            }
        }, fragment.getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }

    /**
     * 🛠️ FUNÇÃO CORRIGIDA: Oculta apenas itens específicos da Toolbar pelo ID
     * enquanto este fragmento estiver visível.
     */
    public static void hideSpecificItems(@NonNull Fragment fragment, @IdRes int... itemIds) {
        fragment.requireActivity().addMenuProvider(new MenuProvider() {
            @Override
            public void onCreateMenu(@NonNull Menu menu, @NonNull MenuInflater menuInflater) {
                // Não infla nenhum menu extra
            }

            @Override
            public boolean onMenuItemSelected(@NonNull MenuItem menuItem) {
                return false;
            }

            @Override
            public void onPrepareMenu(@NonNull Menu menu) {
                for (int id : itemIds) {
                    MenuItem item = menu.findItem(id);
                    // 👉 Correção: Alterado de setVisible(true) para setVisible(false) para ocultar de fato
                    if (item != null) item.setVisible(false);
                }
            }
        }, fragment.getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }
}
