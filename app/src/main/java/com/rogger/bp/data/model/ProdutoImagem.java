package com.rogger.bp.data.model;

/**
 * ProdutoImagem
 * <p>
 * Representa um documento da coleção global "imagens_produtos" no Firestore.
 * <p>
 * Estrutura no Firestore:
 * imagens_produtos/{codigoBarras}
 * └── nomeProduto : "Leite Integral"
 * └── imagemUrl   : "https://firebasestorage..."
 * <p>
 * Regras:
 * - Um documento por código de barras (chave única global)
 * - Qualquer usuário autenticado pode LER
 * - Só é CRIADO se o codigoBarras ainda NÃO existe no banco
 * - Nunca é atualizado nem deletado por usuários
 * <p>
 * NÃO é uma entidade Room — não é salvo localmente.
 * O cache é gerenciado pelo LocalCache em memória.
 */
public class ProdutoImagem {

    private String codigoBarras; // ID do documento no Firestore
    private String nomeProduto;
    private String imagemUrl;

    // ======================== CONSTRUTORES ========================

    public ProdutoImagem() {
        // Necessário para desserialização
    }

    public ProdutoImagem(String codigoBarras, String nomeProduto, String imagemUrl) {
        this.codigoBarras = codigoBarras;
        this.nomeProduto = nomeProduto;
        this.imagemUrl = imagemUrl;
    }

    // ======================== GETTERS / SETTERS ========================

    public String getCodigoBarras() {
        return codigoBarras;
    }

    public void setCodigoBarras(String codigoBarras) {
        this.codigoBarras = codigoBarras;
    }

    public String getNomeProduto() {
        return nomeProduto;
    }

    public void setNomeProduto(String nomeProduto) {
        this.nomeProduto = nomeProduto;
    }

    public String getImagemUrl() {
        return imagemUrl;
    }

    public void setImagemUrl(String imagemUrl) {
        this.imagemUrl = imagemUrl;
    }

    // ======================== HELPERS ========================

    /**
     * Verifica se este objeto tem imagem válida para exibição.
     * Útil no AddFragment para decidir se preenche automaticamente.
     */
    public boolean temImagem() {
        return imagemUrl != null && !imagemUrl.isEmpty();
    }

    /**
     * Verifica se este objeto tem nome sugerido válido.
     */
    public boolean temNome() {
        return nomeProduto != null && !nomeProduto.isEmpty();
    }
}

