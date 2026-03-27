package com.rogger.bp.data.repository;

import android.app.Application;
import android.util.Log;

import androidx.lifecycle.LiveData;

import com.google.firebase.auth.FirebaseAuth;
import com.rogger.bp.data.dao.CategoriaDao;
import com.rogger.bp.data.database.BpdDatabase;
import com.rogger.bp.data.database.FirebaseDataSource;
import com.rogger.bp.data.database.LocalCache;
import com.rogger.bp.data.model.Categoria;

import java.util.List;

/**
 * CategoriaRepository
 *
 * Orquestrador entre Room (local) e Firestore (nuvem) para categorias.
 * Mesma estratégia do ProdutoRepository:
 *  - Room é a fonte de verdade para a UI
 *  - Firestore é o backup/sync em nuvem
 *  - LocalCache evita buscas repetidas ao navegar
 */
public class CategoriaRepository {

    private static final String TAG = "CategoriaRepository";

    private final CategoriaDao       categoriaDao;
    private final FirebaseDataSource firebaseDataSource;
    private final LocalCache localCache;
    private final String             userId;

    private final LiveData<List<Categoria>> categorias;

    public CategoriaRepository(Application app) {
        BpdDatabase db = BpdDatabase.getDatabase(app);
        categoriaDao       = db.categoriaDao();
        firebaseDataSource = FirebaseDataSource.getInstance();
        localCache         = LocalCache.getInstance();
        userId             = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : "";

        categorias = categoriaDao.listarCategorias(userId);

        // Sincroniza do Firestore na inicialização
        sincronizarDoFirestore();
    }

    // ======================== LEITURA ========================

    public LiveData<List<Categoria>> getCategorias() {
        return categorias;
    }

    /**
     * Sincroniza categorias do Firestore → Room.
     * Usa cache para evitar chamadas desnecessárias.
     */
    public void sincronizarDoFirestore() {
        List<Categoria> cached = localCache.getCategorias();
        if (cached != null) {
            Log.d(TAG, "Cache de categorias válido. Expira em " +
                    (localCache.getCategoriasTtlRestante() / 1000) + "s");
            return;
        }

        Log.d(TAG, "Buscando categorias no Firestore...");
        firebaseDataSource.buscarCategorias(new FirebaseDataSource.FirestoreCallback<List<Categoria>>() {
            @Override
            public void onSuccess(List<Categoria> lista) {
                Log.d(TAG, "Sincronizado " + lista.size() + " categorias do Firestore");
                localCache.setCategorias(lista);
                BpdDatabase.databaseWriteExecutor.execute(() -> {
                    for (Categoria c : lista) {
                        c.setUserId(userId);
                        categoriaDao.inserirCategoria(c);
                    }
                });
            }

            @Override
            public void onFailure(Exception e) {
                Log.e(TAG, "Erro ao sincronizar categorias: " + e.getMessage());
            }
        });
    }

    // ======================== INSERIR ========================

    public void inserir(Categoria categoria) {
        categoria.setUserId(userId);
        BpdDatabase.databaseWriteExecutor.execute(() -> {
            long newId = categoriaDao.inserirCategoria(categoria);
            categoria.setId((int) newId);
            localCache.invalidarCategorias();

            firebaseDataSource.salvarCategoria(categoria, new FirebaseDataSource.FirestoreCallback<String>() {
                @Override
                public void onSuccess(String docId) {
                    Log.d(TAG, "Categoria salva no Firestore: " + docId);
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Falha ao salvar categoria no Firestore: " + e.getMessage());
                }
            });
        });
    }

    // ======================== ATUALIZAR ========================

    public void atualizar(Categoria categoria) {
        BpdDatabase.databaseWriteExecutor.execute(() -> {
            categoriaDao.atualizarCategoria(categoria);
            Log.d(TAG, "Categoria atualizada localmente: " + categoria.getNome());
            localCache.invalidarCategorias();

            firebaseDataSource.atualizarCategoria(categoria, new FirebaseDataSource.FirestoreCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d(TAG, "Categoria atualizada no Firestore: " + categoria.getNome());
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Falha ao atualizar categoria no Firestore: " + e.getMessage());
                }
            });
        });
    }

    // ======================== REMOVER ========================

    public void remover(Categoria categoria) {
        BpdDatabase.databaseWriteExecutor.execute(() -> {
            categoriaDao.removerCategoria(categoria);
            localCache.invalidarCategorias();

            firebaseDataSource.excluirCategoria(categoria.getId(), new FirebaseDataSource.FirestoreCallback<Void>() {
                @Override
                public void onSuccess(Void result) {
                    Log.d(TAG, "Categoria excluída do Firestore: " + categoria.getId());
                }

                @Override
                public void onFailure(Exception e) {
                    Log.e(TAG, "Falha ao excluir categoria do Firestore: " + e.getMessage());
                }
            });
        });
    }
}