package com.rogger.bp.ui.search

import android.annotation.SuppressLint
import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.rogger.bp.R
import com.rogger.bp.data.model.Produto
import com.rogger.bp.ui.base.Utils
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SearchAdapter(
    private val context: Context,
    private val dayYellow: Int,
    private val onItemClick: ((Produto) -> Unit)? = null
) : RecyclerView.Adapter<SearchAdapter.ViewHolder>() {

    private var dados: List<Produto> = emptyList()

    @SuppressLint("NotifyDataSetChanged")
    fun setDados(novos: List<Produto>) {
        dados = novos
        notifyDataSetChanged()
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_list, parent, false)
        return ViewHolder(view)
    }

    override fun getItemCount() = dados.size

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        val produto = dados[position]

        // Nome
        holder.txtTitle.text = produto.nome ?: ""

        // Categoria
        holder.txtSubTitle.text = produto.nomeCategoria ?: ""

        // Código de barras
        holder.txtBarcode.text = produto.codigoBarras ?: ""

        // Data de validade formatada
        if (produto.timestamp > 0) {
            val sdf = SimpleDateFormat("dd/MM/yyyy", Locale.getDefault())
            holder.txtDate.text = sdf.format(Date(produto.timestamp))
        } else {
            holder.txtDate.text = ""
        }

        // Indicador de cor (dias restantes)
        val dias = Utils.calcDifferencInDays(produto.timestamp)

        val corCirculo = when {
            dias <= 0 -> context.getColor(R.color.red)
            dias <= dayYellow -> context.getColor(R.color.yellow)
            else -> context.getColor(R.color.green)
        }

        if (dias < 1) {
            holder.txtContDay.text = if (dias < 0) "VENCIDO" else "VENCE HOJE"
        } else if (dias <= dayYellow) {
            holder.txtContDay.text = dias.toString() + if (dias.toInt() == 1) " Dia" else " Dias"
        } else {
            holder.txtContDay.text = "$dias dias"
        }
         holder.imgCircle.setColorFilter(corCirculo)

        // Imagem via Glide
        Glide.with(context)
            .asBitmap()
            .load(produto.imagem)
            .override(200, 200)
            .format(com.bumptech.glide.load.DecodeFormat.PREFER_RGB_565) // Economiza 50% de memória
            .placeholder(R.drawable.no_picture)
            .error(R.drawable.no_picture)
            .centerCrop()
            .into(holder.imgProduto)

        // Click no item
        holder.itemView.setOnClickListener { onItemClick?.invoke(produto) }
    }

    inner class ViewHolder(view: View) : RecyclerView.ViewHolder(view) {
        val txtTitle: TextView = view.findViewById(R.id.home_title)
        val txtSubTitle: TextView = view.findViewById(R.id.home_subTitle)
        val txtBarcode: TextView = view.findViewById(R.id.home_barcode)
        val txtDate: TextView = view.findViewById(R.id.txt_home_right)
        val txtContDay: TextView = view.findViewById(R.id.txt_home_left)
        val imgCircle: ImageView = view.findViewById(R.id.image_home_circle)
        val imgProduto: ImageView = view.findViewById<ImageView>(R.id.imageview_home).apply {
            // 🔥 Correção para Android 11: Desativa aceleração de hardware para evitar erro de Canvas muito grande
            setLayerType(View.LAYER_TYPE_SOFTWARE, null)
        }
    }
}