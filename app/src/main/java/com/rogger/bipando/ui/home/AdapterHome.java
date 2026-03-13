package com.rogger.bipando.ui.home;

/*
/*@Roger de oliveira 
*/

import static java.lang.Math.ceil;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.rogger.bipando.R;
import com.rogger.bipando.data.model.Produto;
import com.rogger.bipando.ui.base.Utils;
import com.rogger.bipando.ui.scanner.ImageBarcode;
import com.squareup.picasso.Picasso;

import java.text.SimpleDateFormat;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class AdapterHome extends RecyclerView.Adapter<AdapterHome.ViewHolder> {
    private AdapterView.OnItemClickListener mListener;
    private List<Produto> dados;
    private final Context context;
    private int n1;
    private int n2;

    public void setOnItemClickListener(AdapterView.OnItemClickListener listener) {
        this.mListener = listener;
    }

    public void clear() {
        if (dados != null) {
            dados.clear();

            notifyDataSetChanged();
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    public void ordenarPorDiferencaDeDias() {
        dados.sort(new Comparator<Produto>() {
            @Override
            public int compare(Produto item1, Produto item2) {
                long diferencaDiasItem1 = Utils.calcDifferencInDays(item1.getTimestamp());
                long diferencaDiasItem2 = Utils.calcDifferencInDays(item2.getTimestamp());

                // Ordena em ordem crescente
                return Long.compare(diferencaDiasItem1, diferencaDiasItem2);
            }
        });
        notifyDataSetChanged(); // Notifica o Adapter sobre a mudança na ordem dos dados
    }

    public AdapterHome(Context context, int n1, int n2) {
        this.context = context;
        this.n1 = n1;
        this.n2 = n2;
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
    public ViewHolder onCreateViewHolder(ViewGroup parent, int arg1) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_list, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Produto modelo = dados.get(position);
        long horas = Utils.calcDifferencInDays(modelo.getTimestamp());
        double dias = ceil(horas / 24.0f);

        String Agora = "";
        try {
            SimpleDateFormat dataFormatada = new SimpleDateFormat("yyyy-MM-dd", new Locale("pt", "BR"));
            Date datasalva = dataFormatada.parse(modelog);
            SimpleDateFormat minhadata = new SimpleDateFormat("dd-MM-yyyy", new Locale("pt", "BR"));
            assert datasalva != null;
            Agora = minhadata.format(datasalva);
        } catch (Exception e) {

        }
        if (n1 == 0 && n2 == 0) {
            n1 = 3;
            n2 = 10;
        }
        holder.txtRight.setText(Agora);
        int ds = (int) dias;
		/*
		Se data for maior que  10 dias cinal verde 
		Se data for menor ou igual a 10 dias cinal amarelo 
		Se data for menor que 3 dias cinal vermelho.
		*/
        if (dias <= n1) {
            holder.imageCircle.setImageResource(R.drawable.red_circle);
            holder.txtLight.setText(ds + "Dias restante");
            if (dias < 1) {
                holder.imageCircle.setImageResource(R.drawable.red_circle);
                holder.txtLight.setText(" VENCIDO ");
            }
        }
        if (dias <= n2 & dias > n1) {

            holder.imageCircle.setImageResource(R.drawable.yellow_circle);
            holder.txtLight.setText(ds + "Dias");
            //NotificationReceiver.showNotification(context);
        }
        if (dias > n2) {
            holder.imageCircle.setImageResource(R.drawable.circle);
            holder.txtLight.setText(String.valueOf(ds));
        }
        holder.txtTitle.setText(modelo.getNome());
        holder.txtSubTitle.setText(modelo.getAnotacoes());
        holder.txtBarcode.setText(modelo.getCodigoBarras());
        String uri = modelo.getImagem();
        if (uri != null) {
            Picasso.get().load(uri).into(holder.imageView);
        } else {
            holder.imageView.setImageResource(R.drawable.no_picture);
        }
        holder.id = modelo.getId();
        holder.uri = modelo.getImagem();
    }

    public int getItemCount() {
        return dados != null ? dados.size() : 0;

    }

    public class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView imageCircle;
        private final ImageView imageView;
        private final TextView txtRight;
        private final TextView txtLight;
        private final TextView txtTitle;
        private final TextView txtSubTitle;
        private final TextView txtBarcode;
        private String uri;
        private int id;

        public ViewHolder(View itemview) {
            super(itemview);
            imageView = itemview.findViewById(R.id.imageview_home);
            CardView cardHomeItem = itemview.findViewById(R.id.card_layout_home);
            imageCircle = itemview.findViewById(R.id.image_home_circle);
            txtLight = itemview.findViewById(R.id.txt_home_left);
            txtRight = itemview.findViewById(R.id.txt_home_right);
            txtTitle = itemview.findViewById(R.id.home_title);
            txtSubTitle = itemview.findViewById(R.id.home_subTitle);
            txtBarcode = itemview.findViewById(R.id.home_barcode);

            cardHomeItem.setOnClickListener(new View.OnClickListener() {
                @SuppressLint("SuspiciousIndentation")
                @Override
                public void onClick(View v) {
                    if (mListener != null) {
                        int position = getAdapterPosition();
                        if (position != RecyclerView.NO_POSITION) {
                            mListener.onItemClick(position, dados);
                        }
                    }
                }

            });
            imageView.setOnClickListener(v -> {
                if (uri != null) {
                    String ids = String.valueOf(id);
                    Intent intent = new Intent(context, ImageBarcode.class);
                    intent.putExtra("ids", ids);
                    intent.putExtra("uri", uri);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                    context.startActivity(intent);
                }
            });

        }

    }

}