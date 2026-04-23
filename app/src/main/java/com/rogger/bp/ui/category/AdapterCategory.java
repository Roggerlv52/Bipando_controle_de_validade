package com.rogger.bp.ui.category;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.rogger.bp.R;
import com.rogger.bp.data.model.Categoria;

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

        void onMenuEditClick(Categoria categoria); // 🔑 Novo método para o clique nos três pontos

        void onSelectionChanged(int totalSelecionados);
    }

    public AdapterCategory(OnCategoriaListener listener) {
        this.listener = listener;
    }

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
            // ✅ Exibe o nome concatenado com a quantidade de produtos
            String textoExibicao = categoria.getNome();
            if (categoria.getCount() > 0) {
                textoExibicao += " (" + categoria.getCount() + ")";
            } else {
                textoExibicao += " (0)";
            }
            nome.setText(textoExibicao);

            checkBox.setVisibility(modoSelecao ? View.VISIBLE : View.GONE);
            checkBox.setChecked(selecionadas.contains(categoria));

            // 🔑 Clique no item inteiro -> Listar produtos desta categoria
            itemView.setOnClickListener(v -> {
                if (modoSelecao) {
                    toggleSelecionado(categoria);
                } else {
                    listener.onClick(categoria);
                }
            });

            // 🔑 Clique no menu (três pontos) -> Editar nome da categoria
            imgMenu.setOnClickListener(v -> {
                listener.onMenuEditClick(categoria);
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
