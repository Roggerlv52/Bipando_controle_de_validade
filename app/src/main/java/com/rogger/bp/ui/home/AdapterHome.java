package com.rogger.bp.ui.home;

/*
/*@Roger de oliveira 
*/

import android.annotation.SuppressLint;
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
import com.rogger.bp.data.dao.CategoriaDao;
import com.rogger.bp.data.database.BpdDatabase;
import com.rogger.bp.data.model.Categoria;
import com.rogger.bp.data.model.Produto;
import com.rogger.bp.ui.base.Utils;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdapterHome extends RecyclerView.Adapter<AdapterHome.ViewHolder> {
    private OnItemClickListener mListener;
    private List<Produto> dados;
    private final Context context;
    private int diasLimiteAmarelo;
    
    // Cache de categorias para evitar consultas repetitivas ao banco
    private final Map<Integer, String> categoriaMap = new HashMap<>();
    private final CategoriaDao categoriaDao;

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
        this.mListener = listener;
        this.context = context;
        this.diasLimiteAmarelo = diasLimiteAmarelo > 0 ? diasLimiteAmarelo : 10;
        this.categoriaDao = BpdDatabase.getDatabase(context).categoriaDao();
        carregarCategorias();
    }

    /**
     * Carrega as categorias do banco para o mapa de cache
     */
    private void carregarCategorias() {
        BpdDatabase.databaseWriteExecutor.execute(() -> {
            List<Categoria> categorias = categoriaDao.listarCategoriasSync(""); // Passando vazio para pegar o que estiver local
            if (categorias != null) {
                for (Categoria c : categorias) {
                    categoriaMap.put(c.getId(), c.getNome());

                }
            }
        });
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setDados(List<Produto> dados) {
        this.dados = dados;
        notifyDataSetChanged();
    }

    public List<Produto> getDados() {
        return dados;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        if (dados == null || position >= dados.size()) return;

        Produto modelo = dados.get(position);

        // 1. Cálculo de dias usando a nova lógica de Utils
        long diasRestantes = Utils.calcDifferencInDays(modelo.getTimestamp());

        // 2. Formatação da data de vencimento
        String dataFormatada = "";
        if (modelo.getTimestamp() > 0) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"));
            dataFormatada = sdf.format(new Date(modelo.getTimestamp()));
        }
        holder.txtRight.setText(dataFormatada);

        if (diasRestantes < 1) {
            holder.imageCircle.setImageResource(R.drawable.red_circle);
            holder.txtLight.setText(diasRestantes < 0 ? "VENCIDO" : "VENCE HOJE");
        } else if (diasRestantes <= diasLimiteAmarelo) {
            holder.imageCircle.setImageResource(R.drawable.yellow_circle);
            holder.txtLight.setText(diasRestantes + (diasRestantes == 1 ? " Dia" : " Dias"));
        } else {
            holder.imageCircle.setImageResource(R.drawable.circle);
            holder.txtLight.setText(diasRestantes + " dias");
        }

        // 4. Preenchimento de textos
        holder.txtTitle.setText(modelo.getNome() != null ? modelo.getNome() : "");
        
        // 🔑 NOME DA CATEGORIA: Tenta pegar do modelo, se vazio tenta do cache local
        String nomeCat = modelo.getNomeCategoria();
        if ((nomeCat == null || nomeCat.isEmpty()) && modelo.getCategoryId() != 0) {
            nomeCat = categoriaMap.get(modelo.getCategoryId());
            Log.d("AdpterHome","categoria "+ modelo.getNomeCategoria());
        }
        holder.txtSubTitle.setText(nomeCat != null ? nomeCat : "");
        
        holder.txtBarcode.setText(modelo.getCodigoBarras() != null ? modelo.getCodigoBarras() : "");

        // 5. Carregamento da imagem
        Glide.with(context)
                .asBitmap()
                .load(modelo.getImagem())
                .override(200, 200)
                .format(com.bumptech.glide.load.DecodeFormat.PREFER_RGB_565) // Economiza 50% de memória
                .centerCrop()
                .placeholder(R.drawable.carregando)
                .error(R.drawable.imagem_error)
                .into(holder.imageView);
        
        // 6. Clique na imagem
        holder.imageView.setOnClickListener(v -> {
            if (mListener != null && modelo.getImagem() != null) {
                mListener.onImageClick(modelo.getImagem());
            }
        });
    }

    @Override
    public int getItemCount() {
        return dados != null ? dados.size() : 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageCircle, imageView;
        private final TextView txtRight, txtLight, txtTitle, txtSubTitle, txtBarcode;

        public ViewHolder(View itemView) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageview_home);
            // 🔥 Correção para Android 11: Desativa aceleração de hardware para evitar erro de Canvas muito grande
            imageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
            imageCircle = itemView.findViewById(R.id.image_home_circle);
            txtLight = itemView.findViewById(R.id.txt_home_left);
            txtRight = itemView.findViewById(R.id.txt_home_right);
            txtTitle = itemView.findViewById(R.id.home_title);
            txtSubTitle = itemView.findViewById(R.id.home_subTitle);
            txtBarcode = itemView.findViewById(R.id.home_barcode);

            itemView.setOnClickListener(v -> {
                if (mListener != null) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        mListener.onItemClick(pos, dados);
                    }
                }
            });
        }
    }
}
