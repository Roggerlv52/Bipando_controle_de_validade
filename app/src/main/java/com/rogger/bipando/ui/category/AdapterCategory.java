package com.rogger.bipando.ui.category;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rogger.bipando.R;
import com.rogger.bipando.data.model.Categoria;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

public class AdapterCategory extends RecyclerView.Adapter<AdapterCategory.ViewHolder> {

    private final List<Categoria> categorias = new ArrayList<>();
    private final Set<Categoria> selecionadas = new HashSet<>();

    private boolean modoSelecao = false;

    private final OnCategoriaListener listener;

    public interface OnCategoriaListener {
        void onClick(Categoria categoria);

        void onSelectionChanged(int totalSelecionados);
    }

    public AdapterCategory(OnCategoriaListener listener) {
        this.listener = listener;
    }

    // 🔹 Atualiza lista
    public void setItems(List<Categoria> list) {
        categorias.clear();
        categorias.addAll(list);
        selecionadas.clear();
        notifyDataSetChanged();
    }

    // 🔹 Ativa / desativa modo seleção
    public void setModoSelecao(boolean ativo) {
        modoSelecao = ativo;
        if (!ativo) selecionadas.clear();
        notifyDataSetChanged();
    }

    public boolean isModoSelecao() {
        return modoSelecao;
    }

    // 🔹 Retorna selecionadas
    public List<Categoria> getSelecionadas() {
        return new ArrayList<>(selecionadas);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(
            @NonNull ViewGroup parent,
            int viewType
    ) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_category, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Categoria categoria = categorias.get(position);
        holder.bind(categoria);
    }

    @Override
    public int getItemCount() {
        return categorias.size();
    }

    class ViewHolder extends RecyclerView.ViewHolder {

        TextView nome;
        CheckBox checkBox;
        ImageView imgMenu;

        ViewHolder(View itemView) {
            super(itemView);
            nome = itemView.findViewById(R.id.txt_categoria_nome);
            checkBox = itemView.findViewById(R.id.check_categoria);
            imgMenu = itemView.findViewById(R.id.img_menu_edit);
        }

        void bind(Categoria categoria) {
            nome.setText(categoria.getNome());

            checkBox.setVisibility(modoSelecao ? View.VISIBLE : View.GONE);
            checkBox.setChecked(selecionadas.contains(categoria));

            itemView.setOnClickListener(v -> {
                if (modoSelecao) {
                    toggleSelecionado(categoria);
                } else {
                    listener.onClick(categoria);
                }
            });
            imgMenu.setOnClickListener(v -> {
                listener.onClick(categoria);
            });

            itemView.setOnLongClickListener(v -> {
                if (!modoSelecao) {
                    setModoSelecao(true);
                    toggleSelecionado(categoria);
                    listener.onSelectionChanged(selecionadas.size());
                }
                return true;
            });

            checkBox.setOnClickListener(v -> {
                toggleSelecionado(categoria);
            });
        }

        private void toggleSelecionado(Categoria categoria) {
            if (selecionadas.contains(categoria)) {
                selecionadas.remove(categoria);
            } else {
                selecionadas.add(categoria);
            }
            notifyItemChanged(getAdapterPosition());
            listener.onSelectionChanged(selecionadas.size());
        }
    }
}

