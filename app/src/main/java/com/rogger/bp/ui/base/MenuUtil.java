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


    public static void hideItems(Fragment fragment, @IdRes int... itemIds) {
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
            public void onMenuClosed(@NonNull Menu menu) {
                MenuProvider.super.onMenuClosed(menu);
            }

            @Override
            public void onPrepareMenu(@NonNull Menu menu) {
                for (int id : itemIds) {
                    MenuItem item = menu.findItem(id);
                    if (item != null) item.setVisible(true);
                }
            }
        }, fragment.getViewLifecycleOwner(), Lifecycle.State.RESUMED);
    }
}
