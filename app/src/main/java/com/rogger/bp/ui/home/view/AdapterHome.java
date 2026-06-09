package com.rogger.bp.ui.home.view;

/*
/*@Roger de oliveira 
*/

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
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
import com.rogger.bp.data.model.PostProduct;
import com.rogger.bp.ui.base.Utils;
import com.rogger.bp.ui.home.OnItemClickListener;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdapterHome extends RecyclerView.Adapter<AdapterHome.ViewHolder> {

    private OnItemClickListener mListener;
    private List<PostProduct> dados;
    private final Context context;
    private int diasLimiteAmarelo;

    public void clear() {
        if (dados != null) {
            dados.clear();
            notifyDataSetChanged();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void ordenarPorDiferencaDeDias() {
        if (dados != null) {
            dados.sort(Comparator.comparingLong(item -> Utils.calcDifferencInDays(item.getTimestamp())));
            notifyDataSetChanged();
        }
    }

    public AdapterHome(Context context, int diasLimiteAmarelo, OnItemClickListener listener) {
        this.mListener        = listener;
        this.context          = context;
        this.diasLimiteAmarelo = diasLimiteAmarelo > 0 ? diasLimiteAmarelo : 10;
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setDados(List<PostProduct> dados) {
        this.dados = dados;
        notifyDataSetChanged();
    }

    public List<PostProduct> getDados() {
        return dados;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (dados == null || position >= dados.size()) return;

        PostProduct modelo = dados.get(position);

        // 1. Cálculo de dias
        long diasRestantes = Utils.calcDifferencInDays(modelo.getTimestamp());

        // 2. Formatação da data de vencimento
        String dataFormatada = "";
        if (modelo.getTimestamp() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"));
            dataFormatada = sdf.format(new Date(modelo.getTimestamp()));
        }
        holder.txtRight.setText(dataFormatada);

        // 3. Indicador de cor/dias
        if (diasRestantes < 1) {
            holder.imageCircle.setImageResource(R.drawable.red_circle);
            holder.txtLight.setTextColor(Color.RED);
            holder.txtRight.setTextColor(Color.RED);
            holder.txtLight.setText(diasRestantes < 0 ? "VENCIDO" : "VENCE HOJE");
        } else if (diasRestantes <= diasLimiteAmarelo) {
            holder.imageCircle.setImageResource(R.drawable.yellow_circle);
            holder.txtRight.setTextColor(Color.parseColor("#FFD700"));
            holder.txtLight.setText(diasRestantes + (diasRestantes == 1 ? " Dia" : " Dias"));
        } else {
            holder.imageCircle.setImageResource(R.drawable.circle);
            holder.txtLight.setText(diasRestantes + " dias");
        }

        // 4. Nome do produto
        holder.txtTitle.setText(modelo.getName() != null ? modelo.getName() : "");

        // FIX: categoryName já vem preenchido no PostProduct desde o momento
        // do cadastro (AddItemFragment salva produto.copy(categoryName=...)).
        // Basta exibir diretamente — não há mais necessidade de um Map local.
        String nomeCat = modelo.getCategoryName();
        if (nomeCat == null) nomeCat = "";
        holder.txtSubTitle.setText(nomeCat);
        Log.d("AdapterHome", "pos=" + position + " name=" + modelo.getName() + " cat=" + nomeCat);

        // 5. Código de barras
        holder.txtBarcode.setText(modelo.getBarcode() != null ? modelo.getBarcode() : "");

        // 6. Imagem
        Glide.with(context)
                .asBitmap()
                .load(modelo.getImageUri())
                .override(200, 200)
                .format(com.bumptech.glide.load.DecodeFormat.PREFER_RGB_565)
                .centerCrop()
                .error(R.drawable.imagem_error)
                .into(holder.imageView);
    }

    @Override
    public int getItemCount() {
        return dados != null ? dados.size() : 0;
    }

    // ── ViewHolder ────────────────────────────────────────────────────────

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageCircle, imageView;
        private final TextView  txtRight, txtLight, txtTitle, txtSubTitle, txtBarcode;

        public ViewHolder(View itemView) {
            super(itemView);
            imageView   = itemView.findViewById(R.id.imageview_home);
            imageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            imageCircle = itemView.findViewById(R.id.image_home_circle);
            txtLight    = itemView.findViewById(R.id.txt_home_left);
            txtRight    = itemView.findViewById(R.id.txt_home_right);
            txtTitle    = itemView.findViewById(R.id.home_title);
            txtSubTitle = itemView.findViewById(R.id.home_subTitle);
            txtBarcode  = itemView.findViewById(R.id.home_barcode);

            itemView.setOnClickListener(v -> {
                if (mListener != null) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        mListener.onItemClick(pos, dados);
                    }
                }
            });

            imageView.setOnClickListener(v -> {
                if (mListener != null && dados != null) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        PostProduct item = dados.get(pos);
                        if (item.getImageUri() != null && !item.getImageUri().isEmpty()) {
                            mListener.onImageClick(item.getImageUri());
                        }
                    }
                }
            });
        }
    }
}