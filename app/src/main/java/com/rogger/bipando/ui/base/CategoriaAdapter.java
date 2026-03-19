package com.rogger.bipando.ui.base;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rogger.bipando.data.model.Categoria;

import java.util.List;

public class CategoriaAdapter extends RecyclerView.Adapter<CategoriaAdapter.ViewHolder> {

    public interface OnItemClick {
        void onClick(Categoria categoria);
    }

    private List<Categoria> lista;
    private OnItemClick listener;

    public CategoriaAdapter(List<Categoria> lista, OnItemClick listener) {
        this.lista = lista;
        this.listener = listener;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        TextView tv = new TextView(parent.getContext());
        tv.setPadding(30, 30, 30, 5);
        tv.setTextSize(17);

        return new ViewHolder(tv);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Categoria categoria = lista.get(position);
        holder.textView.setText(categoria.getNome());

        holder.textView.setOnClickListener(v -> {
            if (listener != null) listener.onClick(categoria);
        });
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            textView = (TextView) itemView;
        }
    }
}
