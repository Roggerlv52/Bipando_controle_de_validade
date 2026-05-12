package com.rogger.bp.data.database;

public class FirestoreSchema {


    /**
     * Constantes que espelham o schema do Firestore.
     * <p>
     * Estrutura:
     * users/
     * └── {username}/
     * ├── uid, nome, email, username
     * ├── categorias/
     * │    └── categoria_{id}/  →  id, nome
     * └── produtos/
     * └── produto_{id}/    →  id, nome, codigoBarras,
     * categoriaId, timestamp,
     * anotacoes, imagem,
     * deleted, deletedAt
     */

    // Construtor privado — classe utilitária, não instanciável
    private FirestoreSchema() {
    }

    // ── Coleções raiz ───────────────────────────────────────────────────────
    public static final class Collections {
        private Collections() {
        }

        public static final String USERS = "users";
        public static final String PRODUTOS = "produtos";
        public static final String CATEGORIAS = "categorias";
    }

    // ── Campos do documento de Usuário ─────────────────────────────────────
    public static final class User {
        private User() {
        }

        public static final String UID = "uid";
        public static final String NOME = "nome";
        public static final String EMAIL = "email";
        public static final String USERNAME = "username";
    }

    // ── Campos do documento de Produto ─────────────────────────────────────
    public static final class Produto {
        private Produto() {
        }

        public static final String ID = "id";
        public static final String NOME = "nome";
        public static final String CODIGO = "codigoBarras";
        public static final String CATEGORIA_ID = "categoriaId";
        public static final String TIMESTAMP = "timestamp";
        public static final String ANOTACOES = "anotacoes";
        public static final String IMAGEM = "imagem";
        public static final String DELETED = "deleted";
        public static final String DELETED_AT = "deletedAt";
    }

    // ── Campos do documento de Categoria ───────────────────────────────────
    public static final class Categoria {
        private Categoria() {
        }

        public static final String ID = "id";
        public static final String NOME = "nome";
    }
}

