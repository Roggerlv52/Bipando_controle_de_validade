package com.rogger.bp.ui.base;

import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rogger.bp.data.model.Categoria;
import com.rogger.bp.data.model.PostCategory;

import java.util.List;

public class CategoriaAdapter extends RecyclerView.Adapter<CategoriaAdapter.ViewHolder> {

    public interface OnItemClick {
        void onClick(PostCategory categoria);
    }

    private List<PostCategory> lista;
    private OnItemClick listener;

    public CategoriaAdapter(List<PostCategory> lista, OnItemClick listener) {
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
        PostCategory categoria = lista.get(position);
        holder.textView.setText(categoria.getName());

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
