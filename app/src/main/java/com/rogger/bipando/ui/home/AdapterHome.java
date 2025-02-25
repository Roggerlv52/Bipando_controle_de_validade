package com.rogger.bipando.ui.home;
/*
/*@Roger de oliveira 
*/

import android.annotation.SuppressLint;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.rogger.bipando.R;
import com.rogger.bipando.data.model.Produto;
import com.rogger.bipando.ui.commun.SharedPreferencesManager;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class AdapterHome extends ListAdapter<Produto, AdapterHome.ViewHolder> {
    private final List<Produto> fullList = new ArrayList<>();
    private final OnItemClickListener listener;
    private int n1;

    public AdapterHome(OnItemClickListener listener, int y) {
        super(DIFF_CALLBACK);
        this.listener = listener;
        n1 = y;
    }

    public interface OnItemClickListener {
        void onItemClick(Produto produto);
         void onImageClick(Produto produto);
    }

    private static final DiffUtil.ItemCallback<Produto> DIFF_CALLBACK = new DiffUtil.ItemCallback<Produto>() {
        @Override
        public boolean areItemsTheSame(@NonNull Produto oldItem, @NonNull Produto newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @SuppressLint("DiffUtilEquals")
        @Override
        public boolean areContentsTheSame(@NonNull Produto oldItem, @NonNull Produto newItem) {
            return oldItem.getTimestamp() == newItem.getTimestamp()
                    && Objects.equals(oldItem.getNome(), newItem.getNome())
                    && Objects.equals(oldItem.getCodigoBarras(), newItem.getCodigoBarras())
                    && Objects.equals(oldItem.getImagem(), newItem.getImagem());
        }
    };

    @SuppressLint("NotifyDataSetChanged")
    public void setItems(@NonNull List<Produto> list) {
        fullList.clear();
        fullList.addAll(list);
        super.submitList(new ArrayList<>(fullList));
    }
    /**
     * Filtra usando a fullList (original).
     */
    public void filter(@Nullable String query) {
        String q = (query == null) ? "" : query.trim().toLowerCase(Locale.getDefault());
        if (q.isEmpty()) {
            // mostra tudo
            super.submitList(new ArrayList<>(fullList));
            return;
        }

        List<Produto> filtered = new ArrayList<>();
        for (Produto item : fullList) {
            CharSequence cs = item.getNome(); // pode ser CharSequence
            String title = (cs == null) ? "" : cs.toString().toLowerCase(Locale.getDefault());
            if (title.contains(q)) {
                filtered.add(item);
            }
        }
        super.submitList(filtered);
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int arg1) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {

        Produto items = getItem(position); // ✅ CORRETO
        Long timestampValidade = items.getTimestamp();
        //boolean semData = false;
        if (timestampValidade == null) {
           // semData = true;
            holder.txtRight.setText("--");
            holder.txtLight.setText("Sem data");
            holder.imageCircle.setImageResource(R.drawable.circle);
        } else {
            long agora = System.currentTimeMillis();
            long diffMillis = timestampValidade - agora;
            double dias = Math.ceil(diffMillis / (1000.0 * 60 * 60 * 24));

            SimpleDateFormat sdf =
                    new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"));
            holder.txtRight.setText(sdf.format(new Date(timestampValidade)));

            if (dias < 1) {
                holder.imageCircle.setImageResource(R.drawable.red_circle);
                holder.txtLight.setText("VENCIDO");
            } else if (dias <= n1) {
                holder.imageCircle.setImageResource(R.drawable.yellow_circle);
                holder.txtLight.setText((int) dias + " dias restantes");
            } else {
                holder.imageCircle.setImageResource(R.drawable.circle);
                holder.txtLight.setText((int) dias+ " dias");
            }
        }

        holder.txtTitle.setText(items.getNome());
        holder.txtBarcode.setText(items.getCodigoBarras());

        String uri = items.getImagem();
        if (uri != null) {
            holder.imageView.setImageURI(Uri.parse(uri));
        } else {
            holder.imageView.setImageResource(R.drawable.no_picture);
        }
        // ✅ CLICK SEMPRE NO FINAL
        holder.itemLayout.setOnClickListener(v -> {
            if (listener != null) {
                listener.onItemClick(items);
            }
        });
        holder.imageView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onImageClick(items);
            }
        });
    }


    public static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageCircle;
        private final ImageView imageView;
        private  LinearLayout itemLayout;
        private final TextView txtRight, txtLight, txtTitle, txtBarcode;

        public ViewHolder(View itemview) {
            super(itemview);
            imageView = itemview.findViewById(R.id.imageview_home);
            imageCircle = itemview.findViewById(R.id.image_home_circle);
            txtLight = itemview.findViewById(R.id.txt_home_left);
            txtRight = itemview.findViewById(R.id.txt_home_right);
            txtTitle = itemview.findViewById(R.id.home_title);
            txtBarcode = itemview.findViewById(R.id.home_barcode);
            itemLayout = itemview.findViewById(R.id.item_layout);
        }
    }
}

