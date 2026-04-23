package com.rogger.bp.data.model;

import androidx.room.Embedded;

public class CategoriaWithCount {
    @Embedded
    public Categoria categoria;
    
    public int count;

    public Categoria getCategoria() {
        return categoria;
    }

}
