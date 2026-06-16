package com.rogger.bp.ui.home.view;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.rogger.bp.R;
import com.rogger.bp.data.model.PostProduct;
import com.rogger.bp.ui.base.Utils;
import com.rogger.bp.ui.home.OnItemClickListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;

public class AdapterHome extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // ── ESTRUTURA INTERNA DE ITENS ──
    public static class ListItem {
        public static final int TYPE_HEADER = 0;
        public static final int TYPE_PRODUCT = 1;

        public final int type;
        public final String headerTitle;
        public final List<PostProduct> groupedProducts; // Apenas para cabeçalho
        public final PostProduct product; // Apenas para produto
        public final int headerColor;

        public ListItem(String headerTitle, List<PostProduct> groupedProducts, int headerColor) {
            this.type = TYPE_HEADER;
            this.headerTitle = headerTitle;
            this.groupedProducts = groupedProducts;
            this.product = null;
            this.headerColor = headerColor;
        }

        public ListItem(PostProduct product) {
            this.type = TYPE_PRODUCT;
            this.headerTitle = null;
            this.groupedProducts = null;
            this.product = product;
            this.headerColor = 0;
        }
    }

    private final OnItemClickListener mListener;
    private final List<ListItem> displayItems = new ArrayList<>();
    private List<PostProduct> rawProducts = new ArrayList<>();
    private final Context context;
    private final int diasLimiteAmarelo;

    public AdapterHome(Context context, int diasLimiteAmarelo, OnItemClickListener listener) {
        this.mListener = listener;
        this.context = context;
        this.diasLimiteAmarelo = diasLimiteAmarelo > 0 ? diasLimiteAmarelo : 10;
    }

    public void clear() {
        rawProducts.clear();
        displayItems.clear();
        notifyDataSetChanged();
    }

    @SuppressLint("NotifyDataSetChanged")
    public void setDados(List<PostProduct> dados) {
        this.rawProducts = dados != null ? dados : new ArrayList<>();
        agruparEPrepararItens();
    }

    // ── LÓGICA SÉNIOR DE AGRUPAMENTO ──
    private void agruparEPrepararItens() {
        displayItems.clear();
        if (rawProducts.isEmpty()) {
            notifyDataSetChanged();
            return;
        }

        // 1. Ordena os produtos por dias restantes (Vencidos primeiro)
        rawProducts.sort(Comparator.comparingLong(item -> Utils.calcDifferencInDays(item.getTimestamp())));

        // 2. Agrupa os produtos com base na proximidade de datas
        LinkedHashMap<String, List<PostProduct>> groups = new LinkedHashMap<>();
        HashMap<String, Integer> groupColors = new HashMap<>();

        for (PostProduct product : rawProducts) {
            long diffDays = Utils.calcDifferencInDays(product.getTimestamp());
            String title;
            int color;

            if (diffDays < 0) {
                title = "Vencido";
                color = Color.parseColor("#FDF2F2"); // Vermelho suave
            } else if (diffDays <= 1) {
                title = "Vence Hoje";
                color = Color.parseColor("#FDF2F2"); // Laranja/Amarelo suave
            } else {
                title = diffDays + (diffDays > 1 ? " dia restante" : " faltam poucos dias");
                // ✅ Define o fundo do card dinamicamente com base no limite de aviso
                if (diffDays <= diasLimiteAmarelo) {
                    color = Color.parseColor("#FFFBEB"); // Laranja suave (Atenção)
                } else {
                    title =  (diffDays + " dias");
                    color = Color.parseColor("#F0FDF4"); // Verde suave (Seguro)
                }
            }

            if (!groups.containsKey(title)) {
                groups.put(title, new ArrayList<>());
                groupColors.put(title, color);
            }
            groups.get(title).add(product);
        }

        // 3. Monta a lista sequencial para o RecyclerView
        for (java.util.Map.Entry<String, List<PostProduct>> entry : groups.entrySet()) {
            String title = entry.getKey();
            List<PostProduct> list = entry.getValue();
            int color = groupColors.get(title);

            displayItems.add(new ListItem(title, list, color)); // Cabeçalho
            for (PostProduct p : list) {
                displayItems.add(new ListItem(p)); // Produto
            }
        }
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return displayItems.get(position).type;
    }

    @Override
    public int getItemCount() {
        return displayItems.size();
    }

    private int dpToPx(int dp) {
        return (int) (dp * context.getResources().getDisplayMetrics().density);
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == ListItem.TYPE_HEADER) {
            // Criação programática do Cabeçalho estruturado
            LinearLayout mainLayout = new LinearLayout(parent.getContext());
            ViewGroup.MarginLayoutParams marginParams = new ViewGroup.MarginLayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT
            );
            marginParams.setMargins(dpToPx(0), dpToPx(0), dpToPx(0), dpToPx(0));
            mainLayout.setLayoutParams(marginParams);
            mainLayout.setOrientation(LinearLayout.HORIZONTAL);
            mainLayout.setGravity(Gravity.CENTER_VERTICAL);
            mainLayout.setPadding(dpToPx(12), dpToPx(10), dpToPx(12), dpToPx(10));

            // Ponto indicador de estado
            View dot = new View(parent.getContext());
            LinearLayout.LayoutParams dotParams = new LinearLayout.LayoutParams(dpToPx(8), dpToPx(8));
            dotParams.setMargins(0, 0, dpToPx(10), 0);
            dot.setLayoutParams(dotParams);
            mainLayout.addView(dot);

            // Título do Grupo
            TextView title = new TextView(parent.getContext());
            LinearLayout.LayoutParams titleParams = new LinearLayout.LayoutParams(
                    0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f
            );
            title.setLayoutParams(titleParams);
            title.setTextSize(14);
            title.setTypeface(null, android.graphics.Typeface.BOLD);
            mainLayout.addView(title);

            // Botão "X" de exclusão em massa
            ImageView imgDelete = new ImageView(parent.getContext());
            LinearLayout.LayoutParams delParams = new LinearLayout.LayoutParams(dpToPx(20), dpToPx(24));
            imgDelete.setLayoutParams(delParams);
            imgDelete.setImageResource(android.R.drawable.ic_menu_close_clear_cancel);
            imgDelete.setColorFilter(Color.parseColor("#9CA3AF")); // Cinzento
            mainLayout.addView(imgDelete);

            return new HeaderViewHolder(mainLayout, dot, title, imgDelete);
        } else {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list, parent, false);
            return new ProductViewHolder(view, mListener, displayItems);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ListItem item = displayItems.get(position);

        if (holder instanceof HeaderViewHolder) {
            HeaderViewHolder hHolder = (HeaderViewHolder) holder;
            hHolder.title.setText(item.headerTitle);

            // ✅ Recupera a diferença de dias com base no primeiro produto pertencente a este grupo
            long diffDays = 0;
            if (item.groupedProducts != null && !item.groupedProducts.isEmpty()) {
                diffDays = Utils.calcDifferencInDays(item.groupedProducts.get(0).getTimestamp());
            }

            GradientDrawable bg = new GradientDrawable();
            GradientDrawable dotBg = new GradientDrawable();
            dotBg.setShape(GradientDrawable.OVAL);

            // ✅ CONFIGURAÇÃO DINÂMICA DE CORES
            if (diffDays < 0) {
                // 🔴 Caso A: Vencido / Expired
                bg.setColor(Color.parseColor("#FDF2F2"));
                dotBg.setColor(Color.parseColor("#EF4444")); // Ponto vermelho
                hHolder.title.setTextColor(Color.parseColor("#991B1B"));
            } else if (diffDays <= diasLimiteAmarelo) {
                // 🟠 Caso B: Próximo do vencimento / Dentro do limite de aviso (Laranja)
                bg.setColor(Color.parseColor("#FFFBEB"));
                dotBg.setColor(Color.parseColor("#F59E0B")); // Ponto laranja
                hHolder.title.setTextColor(Color.parseColor("#92400E")); // Texto laranja escuro
            } else {
                // 🟢 Caso C: Seguro / Afastado do limite de aviso (Verde)
                bg.setColor(Color.parseColor("#F0FDF4"));
                dotBg.setColor(Color.parseColor("#10B981")); // Ponto verde
                hHolder.title.setTextColor(Color.parseColor("#065F46")); // Texto verde escuro
            }

            bg.setCornerRadius(dpToPx(10));
            hHolder.itemView.setBackground(bg);
            hHolder.dot.setBackground(dotBg);

            hHolder.imgDelete.setOnClickListener(v -> {
                if (mListener != null) {
                    mListener.onHeaderDeleteClick(item.groupedProducts, item.headerTitle);
                }
            });

        } else if (holder instanceof ProductViewHolder) {
            ProductViewHolder pHolder = (ProductViewHolder) holder;
            PostProduct modelo = item.product;

            String dataFormatada = "";
            if (modelo.getTimestamp() > 0) {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", new Locale("pt", "BR"));
                dataFormatada = sdf.format(new Date(modelo.getTimestamp()));
            }
            pHolder.txtRight.setText(dataFormatada);

            pHolder.txtTitle.setText(modelo.getName() != null ? modelo.getName() : "");

            String nomeCat = modelo.getCategoryName();
            if (nomeCat == null) nomeCat = "";
            pHolder.txtSubTitle.setText(nomeCat);

            pHolder.txtBarcode.setText(modelo.getBarcode() != null ? modelo.getBarcode() : "");

            Glide.with(context)
                    .asBitmap()
                    .load(modelo.getImageUri())
                    .override(200, 200)
                    .format(com.bumptech.glide.load.DecodeFormat.PREFER_RGB_565)
                    .centerCrop()
                    .error(android.R.drawable.ic_menu_gallery)
                    .into(pHolder.imageView);
        }
    }

    // ── VIEWHOLDERS ──
    public static class HeaderViewHolder extends RecyclerView.ViewHolder {
        View dot;
        TextView title;
        ImageView imgDelete;

        public HeaderViewHolder(View itemView, View dot, TextView title, ImageView imgDelete) {
            super(itemView);
            this.dot = dot;
            this.title = title;
            this.imgDelete = imgDelete;
        }
    }

    public static class ProductViewHolder extends RecyclerView.ViewHolder {
        private final ImageView  imageView;
        private final TextView txtRight, txtLight, txtTitle, txtSubTitle, txtBarcode;

        public ProductViewHolder(View itemView, OnItemClickListener mListener, List<ListItem> displayItems) {
            super(itemView);
            imageView = itemView.findViewById(R.id.imageview_home);
            if (imageView != null) imageView.setLayerType(View.LAYER_TYPE_SOFTWARE, null);
           // imageCircle = itemView.findViewById(R.id.image_home_circle);
            txtLight = itemView.findViewById(R.id.txt_home_left);
            txtRight = itemView.findViewById(R.id.txt_home_right);
            txtTitle = itemView.findViewById(R.id.home_title);
            txtSubTitle = itemView.findViewById(R.id.home_subTitle);
            txtBarcode = itemView.findViewById(R.id.home_barcode);

            itemView.setOnClickListener(v -> {
                if (mListener != null) {
                    int pos = getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        // Calcula o índice real na lista flat de produtos
                        List<PostProduct> realList = new ArrayList<>();
                        for (ListItem item : displayItems) {
                            if (item.type == ListItem.TYPE_PRODUCT) {
                                realList.add(item.product);
                            }
                        }
                        ListItem targetItem = displayItems.get(pos);
                        if (targetItem.type == ListItem.TYPE_PRODUCT) {
                            int targetIndex = realList.indexOf(targetItem.product);
                            mListener.onItemClick(targetIndex, realList);
                        }
                    }
                }
            });

            if (imageView != null) {
                imageView.setOnClickListener(v -> {
                    if (mListener != null) {
                        int pos = getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            ListItem item = displayItems.get(pos);
                            if (item.type == ListItem.TYPE_PRODUCT && item.product.getImageUri() != null && !item.product.getImageUri().isEmpty()) {
                                mListener.onImageClick(item.product.getImageUri());
                            }
                        }
                    }
                });
            }
        }
    }
}