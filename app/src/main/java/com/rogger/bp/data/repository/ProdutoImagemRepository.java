package com.rogger.bp.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.rogger.bp.data.database.ProdutoImagemDataSource;
import com.rogger.bp.data.model.ProdutoImagem;

import java.util.HashMap;
import java.util.Map;

public class ProdutoImagemRepository {

    private static final String TAG = "ProdutoImagemRepo";
    private static final long TTL_MS = 10 * 60 * 1000L; // 10 minutos

    private final Map<String, CacheEntry> cache = new HashMap<>();
    private final ProdutoImagemDataSource dataSource;
    private static volatile ProdutoImagemRepository INSTANCE;

    public static ProdutoImagemRepository getInstance() {
        if (INSTANCE == null) {
            synchronized (ProdutoImagemRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new ProdutoImagemRepository();
                }
            }
        }
        return INSTANCE;
    }

    private ProdutoImagemRepository() {
        dataSource = ProdutoImagemDataSource.getInstance();
    }

    public void buscarPorCodigoBarras(@NonNull String codigoBarras,
                                      @NonNull ProdutoImagemDataSource.BuscaCallback callback) {

        // 1. Verifica cache primeiro
        ProdutoImagem cached = getCacheValido(codigoBarras);
        if (cached != null) {
            Log.d(TAG, "Cache hit para barcode: " + codigoBarras);
            callback.onEncontrado(cached);
            return;
        }

        // 2. Consulta Firestore
        Log.d(TAG, "Cache miss. Consultando Firestore para: " + codigoBarras);
        dataSource.buscarPorCodigoBarras(codigoBarras,
                new ProdutoImagemDataSource.BuscaCallback() {

                    @Override
                    public void onEncontrado(@NonNull ProdutoImagem produtoImagem) {
                        // Armazena no cache para próximas consultas
                        putCache(codigoBarras, produtoImagem);
                        callback.onEncontrado(produtoImagem);
                    }

                    @Override
                    public void onNaoEncontrado() {
                        // Não armazena no cache — barcode pode ser cadastrado em seguida
                        callback.onNaoEncontrado();
                    }

                    @Override
                    public void onErro(@NonNull Exception e) {
                        callback.onErro(e);
                    }
                });
    }

    private void putCache(@NonNull String codigoBarras, @NonNull ProdutoImagem imagem) {
        cache.put(codigoBarras, new CacheEntry(imagem, System.currentTimeMillis()));
    }

    private ProdutoImagem getCacheValido(@NonNull String codigoBarras) {
        CacheEntry entry = cache.get(codigoBarras);
        if (entry == null) return null;
        boolean valido = (System.currentTimeMillis() - entry.cachedAt) < TTL_MS;
        return valido ? entry.imagem : null;
    }

    private static class CacheEntry {
        final ProdutoImagem imagem;
        final long cachedAt;

        CacheEntry(ProdutoImagem imagem, long cachedAt) {
            this.imagem = imagem;
            this.cachedAt = cachedAt;
        }
    }
}
