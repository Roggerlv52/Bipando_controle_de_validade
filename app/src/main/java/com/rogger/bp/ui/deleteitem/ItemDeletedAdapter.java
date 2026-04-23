package com.rogger.bp.ui.deleteitem;

import android.content.Context;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.rogger.bp.R;
import com.rogger.bp.data.model.Produto;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class ItemDeletedAdapter extends RecyclerView.Adapter<ItemDeletedAdapter.ViewHolder> {
    private Context context;
    private List<Produto> lista = new ArrayList<>();
    private OnItemDeletedClick listener;

    public ItemDeletedAdapter(Context c, OnItemDeletedClick listener) {
        this.context = c;
        this.listener = listener;
    }

    public interface OnItemDeletedClick {
        void onClick(Produto produto);
    }

    public void submitList(List<Produto> produtos) {
        lista = produtos;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_deleted, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ItemDeletedAdapter.ViewHolder holder, int position) {
        Produto produto = lista.get(position);
        Log.d("ADAPTER", "Produto: " + produto.getNome() + "");
        SimpleDateFormat sdf =
                new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"));
        holder.txt_left.setText(sdf.format(new Date(produto.getTimestamp())));
        holder.txt_rigth.setText(produto.getNomeCategoria());
        holder.txt_home.setText(produto.getNome());
        holder.txt_bcd.setText(produto.getCodigoBarras());

        Glide.with(context)
                .load(produto.getImagem())
                .override(200, 200)
                .centerCrop()
                .placeholder(R.drawable.carregando)
                .error(R.drawable.imagem_error)
                .into(holder.img_home);


        holder.itemView.setOnClickListener(v -> listener.onClick(produto));
    }

    @Override
    public int getItemCount() {
        return lista.size();
    }

    // ViewHolder padrão
    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView txt_left, txt_rigth, txt_home, txt_bcd;
        ImageView img_home;


        public ViewHolder(View itemView) {
            super(itemView);
            txt_left = itemView.findViewById(R.id.txt_home_left);
            txt_rigth = itemView.findViewById(R.id.txt_home_right);
            txt_home = itemView.findViewById(R.id.home_title);
            txt_bcd = itemView.findViewById(R.id.txt_item_bcd);
            img_home = itemView.findViewById(R.id.imageview_home);
        }
    }
}
