package com.rogger.bipando.data.database;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.rogger.bipando.data.model.Categoria;
import com.rogger.bipando.data.model.Produto;

import java.util.Collections;
import java.util.List;

/**
 * LocalCache
 *
 * Cache em memória (RAM) com TTL (Time To Live) para evitar buscas
 * repetidas no Firestore ao navegar entre telas.
 *
 * Funciona como uma camada entre o Repository e o FirebaseDataSource:
 *
 *   Repository → LocalCache (válido?) → retorna cache
 *                     ↓ expirado ou vazio
 *              FirebaseDataSource → Firestore → atualiza cache
 *
 * TTL padrão: 5 minutos. Configurável por instância.
 *
 * O cache é limpo automaticamente no logout (clearAll).
 */
public class LocalCache {

    private static final long TTL_MS_DEFAULT = 5 * 60 * 1000L; // 5 minutos

    // ======================== SINGLETON ========================

    private static volatile LocalCache INSTANCE;

    public static LocalCache getInstance() {
        if (INSTANCE == null) {
            synchronized (LocalCache.class) {
                if (INSTANCE == null) {
                    INSTANCE = new LocalCache();
                }
            }
        }
        return INSTANCE;
    }

    private LocalCache() {}

    // ======================== CACHE DE PRODUTOS ========================

    private List<Produto> cachedProdutosAtivos;
    private List<Produto> cachedProdutosDeletados;
    private long produtosAtivosCachedAt    = 0L;
    private long produtosDeletadosCachedAt = 0L;
    private long ttlProdutos = TTL_MS_DEFAULT;

    /** Define o TTL dos produtos em milissegundos */
    public void setTtlProdutos(long ttlMs) {
        this.ttlProdutos = ttlMs;
    }

    /** Armazena a lista de produtos ativos no cache */
    public void setProdutosAtivos(@NonNull List<Produto> lista) {
        cachedProdutosAtivos       = lista;
        produtosAtivosCachedAt     = System.currentTimeMillis();
    }

    /** Armazena a lista de produtos deletados no cache */
    public void setProdutosDeletados(@NonNull List<Produto> lista) {
        cachedProdutosDeletados    = lista;
        produtosDeletadosCachedAt  = System.currentTimeMillis();
    }

    /**
     * Retorna produtos ativos se o cache ainda for válido (dentro do TTL).
     * Retorna null se o cache estiver expirado ou vazio.
     */
    @Nullable
    public List<Produto> getProdutosAtivos() {
        if (isCacheValido(produtosAtivosCachedAt, ttlProdutos)) {
            return Collections.unmodifiableList(cachedProdutosAtivos);
        }
        return null;
    }

    /**
     * Retorna produtos deletados se o cache ainda for válido.
     */
    @Nullable
    public List<Produto> getProdutosDeletados() {
        if (isCacheValido(produtosDeletadosCachedAt, ttlProdutos)) {
            return Collections.unmodifiableList(cachedProdutosDeletados);
        }
        return null;
    }

    /** Invalida o cache de produtos (força nova busca no próximo acesso) */
    public void invalidarProdutos() {
        cachedProdutosAtivos       = null;
        cachedProdutosDeletados    = null;
        produtosAtivosCachedAt     = 0L;
        produtosDeletadosCachedAt  = 0L;
    }

    // ======================== CACHE DE CATEGORIAS ========================

    private List<Categoria> cachedCategorias;
    private long categoriasCachedAt = 0L;
    private long ttlCategorias = TTL_MS_DEFAULT;

    /** Define o TTL das categorias em milissegundos */
    public void setTtlCategorias(long ttlMs) {
        this.ttlCategorias = ttlMs;
    }

    /** Armazena a lista de categorias no cache */
    public void setCategorias(@NonNull List<Categoria> lista) {
        cachedCategorias    = lista;
        categoriasCachedAt  = System.currentTimeMillis();
    }

    /**
     * Retorna categorias se o cache ainda for válido.
     * Retorna null se expirado.
     */
    @Nullable
    public List<Categoria> getCategorias() {
        if (isCacheValido(categoriasCachedAt, ttlCategorias)) {
            return Collections.unmodifiableList(cachedCategorias);
        }
        return null;
    }

    /** Invalida o cache de categorias */
    public void invalidarCategorias() {
        cachedCategorias   = null;
        categoriasCachedAt = 0L;
    }

    // ======================== CONTROLE GERAL ========================

    /**
     * Limpa TODO o cache. Deve ser chamado no logout do usuário
     * para evitar que dados de um usuário vazem para outro.
     */
    public void clearAll() {
        invalidarProdutos();
        invalidarCategorias();
    }

    /**
     * Verifica se o timestamp de cache ainda está dentro do TTL.
     */
    private boolean isCacheValido(long cachedAt, long ttlMs) {
        if (cachedAt == 0L) return false;
        return (System.currentTimeMillis() - cachedAt) < ttlMs;
    }

    /**
     * Retorna quantos ms faltam para o cache de produtos expirar.
     * Valor negativo significa que já expirou.
     */
    public long getProdutosTtlRestante() {
        if (produtosAtivosCachedAt == 0L) return -1L;
        return ttlProdutos - (System.currentTimeMillis() - produtosAtivosCachedAt);
    }

    /**
     * Retorna quantos ms faltam para o cache de categorias expirar.
     */
    public long getCategoriasTtlRestante() {
        if (categoriasCachedAt == 0L) return -1L;
        return ttlCategorias - (System.currentTimeMillis() - categoriasCachedAt);
    }
}
