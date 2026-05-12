package com.rogger.bp.data.model;

public class UserModel {
    private String uid;
    private String nome;
    private String email;
    private String username;

    // Construtor vazio obrigatório para desserialização do Firestore
    public UserModel() {}

    public UserModel(String uid, String nome, String email, String username) {
        this.uid      = uid;
        this.nome     = nome;
        this.email    = email;
        this.username = username;
    }

    // ===================== GETTERS / SETTERS =====================

    public String getUid() { return uid; }
    public void setUid(String uid) { this.uid = uid; }

    public String getNome() { return nome; }
    public void setNome(String nome) { this.nome = nome; }

    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }

    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
}
