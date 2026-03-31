package com.rogger.bp.data.repository;

import android.util.Log;

import androidx.annotation.NonNull;

import com.rogger.bp.data.database.ProdutoImagemDataSource;
import com.rogger.bp.data.model.ProdutoImagem;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

/**
 * ProdutoImagemRepository
 *
 * Orquestra o cache em memória e o ProdutoImagemDataSource.
 *
 * Estratégia de cache:
 *   - Buscas por barcode são cacheadas por TTL de 10 minutos
 *   - Imagens mudam raramente (nunca após criadas), logo TTL longo é seguro
 *   - Cache separado do LocalCache principal para não misturar responsabilidades
 *   - Cache invalida automaticamente ao salvar novo documento
 *
 * Uso no AddFragment:
 *   1. buscarPorCodigoBarras() → verifica cache → consulta Firestore se necessário
 *   2. salvarNovaImagem()      → só chamado se barcode não existia
 */
public class ProdutoImagemRepository {

    private static final String TAG    = "ProdutoImagemRepo";
    private static final long   TTL_MS = 10 * 60 * 1000L; // 10 minutos

    // Cache em memória: codigoBarras → ProdutoImagem
    private final Map<String, CacheEntry> cache = new HashMap<>();

    private final ProdutoImagemDataSource dataSource;

    // ======================== SINGLETON ========================

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

    // ======================== BUSCA ========================

    /**
     * Busca a imagem global pelo código de barras.
     *
     * Prioridade:
     *   1. Cache em memória (zero tráfego de dados)
     *   2. Firestore (caso cache ausente ou expirado)
     *
     * @param codigoBarras Código de barras escaneado
     * @param callback     Resultado: encontrado, não encontrado ou erro
     */
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

    // ======================== SALVAR ========================

    /**
     * Salva uma nova imagem global para o código de barras.
     *
     * Após sucesso, armazena no cache automaticamente para
     * que consultas posteriores na mesma sessão não batam no Firestore.
     *
     * @param codigoBarras  Código de barras do produto
     * @param nomeProduto   Nome digitado pelo usuário
     * @param arquivoLocal  Arquivo de imagem capturado/selecionado
     * @param callback      Resultado da operação
     */
    public void salvarNovaImagem(@NonNull String codigoBarras,
                                 @NonNull String nomeProduto,
                                 @NonNull File arquivoLocal,
                                 @NonNull ProdutoImagemDataSource.SalvarCallback callback) {

        dataSource.salvarNovaImagem(codigoBarras, nomeProduto, arquivoLocal,
                new ProdutoImagemDataSource.SalvarCallback() {

                    @Override
                    public void onProgresso(int porcentagem) {
                        callback.onProgresso(porcentagem);
                    }

                    @Override
                    public void onSucesso(@NonNull ProdutoImagem produtoImagem) {
                        // Armazena no cache imediatamente após criar
                        putCache(codigoBarras, produtoImagem);
                        Log.d(TAG, "Nova imagem salva e cacheada: " + codigoBarras);
                        callback.onSucesso(produtoImagem);
                    }

                    @Override
                    public void onJaExiste(@NonNull ProdutoImagem produtoImagemExistente) {
                        // Outro usuário cadastrou antes — cacheia e repassa
                        putCache(codigoBarras, produtoImagemExistente);
                        Log.d(TAG, "Barcode já existia (race condition): " + codigoBarras);
                        callback.onJaExiste(produtoImagemExistente);
                    }

                    @Override
                    public void onErro(@NonNull Exception e) {
                        callback.onErro(e);
                    }
                });
    }

    // ======================== CACHE INTERNO ========================

    private void putCache(@NonNull String codigoBarras, @NonNull ProdutoImagem imagem) {
        cache.put(codigoBarras, new CacheEntry(imagem, System.currentTimeMillis()));
    }

    private ProdutoImagem getCacheValido(@NonNull String codigoBarras) {
        CacheEntry entry = cache.get(codigoBarras);
        if (entry == null) return null;
        boolean valido = (System.currentTimeMillis() - entry.cachedAt) < TTL_MS;
        return valido ? entry.imagem : null;
    }

    /**
     * Limpa todo o cache. Chamar junto com LocalCache.clearAll() no logout.
     */
    public void clearCache() {
        cache.clear();
        Log.d(TAG, "Cache de imagens limpo.");
    }

    // ======================== ENTRY INTERNA ========================

    private static class CacheEntry {
        final ProdutoImagem imagem;
        final long          cachedAt;

        CacheEntry(ProdutoImagem imagem, long cachedAt) {
            this.imagem    = imagem;
            this.cachedAt  = cachedAt;
        }
    }
}
