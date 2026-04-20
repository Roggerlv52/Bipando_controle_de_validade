package com.rogger.bp.data.model;

import androidx.room.Embedded;

/**
 * POJO para representar o resultado do JOIN entre Produto e Categoria.
 */
public class ProdutoWithCategory {
    @Embedded
    public Produto produto;

    public String nomeCategoria;

    public Produto getProduto() {
        return produto;
    }

    public String getNomeCategoria() {
        return nomeCategoria != null ? nomeCategoria : "";
    }
}
