package com.rogger.bp.data.model;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "categorias")
public class Categoria {

    @PrimaryKey(autoGenerate = true)
    private int id;
    private String nome;
    private String userId;

    public int getId() {
        return id;
    }
    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }

    public void setId(int id) {
        this.id = id;
    }

    public String getNome() {
        return nome;
    }

    public void setNome(String nome) {
        this.nome = nome;
    }
    @NonNull
    @Override
    public String toString() {
        return nome;
    }
}

