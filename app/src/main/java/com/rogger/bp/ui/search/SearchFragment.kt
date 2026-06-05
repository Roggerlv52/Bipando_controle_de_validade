package com.rogger.bp.ui.search

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.lifecycleScope
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rogger.bp.R
import com.rogger.bp.data.database.BpDatabase
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.notification.NotificationPrefs
import com.rogger.bp.ui.scanner.BarcodeScanFragment
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch


class SearchFragment : Fragment() {

    private var adapter: SearchAdapter? = null
    private var recyclerSearch: RecyclerView? = null
    private var layoutEmpty: LinearLayout? = null
    private var txtHint: TextView? = null

    // 👉 Job para gerenciar e cancelar buscas antigas (evita concorrência de digitação rápida)
    private var searchJob: Job? = null

    private val mainToolbar: Toolbar?
        get() = requireActivity().findViewById(R.id.toolbar)

    override fun onResume() {
        super.onResume()
        mainToolbar?.visibility = View.GONE
    }

    override fun onPause() {
        super.onPause()
        mainToolbar?.visibility = View.VISIBLE
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        val view = inflater.inflate(R.layout.fragment_search, container, false)

        recyclerSearch = view.findViewById(R.id.recycler_search)
        layoutEmpty = view.findViewById(R.id.layout_search_empty)
        txtHint = view.findViewById(R.id.txt_search_hint)
        val marquee = view.findViewById<TextView>(R.id.txt_marquee_hint)

        val diasAmarelo = NotificationPrefs.getDays(requireContext())

        // ── ✅ CORREÇÃO 1: Inicializando o adapter corretamente ──
        adapter = SearchAdapter(requireContext(), diasAmarelo) { produto ->
            // Ao clicar no item buscado, navega para a tela de Edição/Visualização
            val bundle = Bundle().apply {
                putString("uuid", produto.uuid)
                putParcelable("product_bundle", produto)
            }
            findNavController().navigate(R.id.action_nav_search_to_nav_edit_fragment, bundle)
        }

        recyclerSearch?.layoutManager = LinearLayoutManager(requireContext())
        recyclerSearch?.adapter = adapter

        // Seta de voltar
        view.findViewById<ImageButton>(R.id.btn_back)?.setOnClickListener {
            findNavController().popBackStack()
        }

        val searchView = view.findViewById<SearchView>(R.id.search_view)
        searchView?.queryHint = "Nome ou codigo de barras"
        searchView?.setOnQueryTextFocusChangeListener { _, hasFocus ->
            marquee.visibility = if (hasFocus) View.GONE else View.VISIBLE
        }

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean {
                query?.trim()?.takeIf { it.isNotEmpty() }?.let { despacharBusca(it) }
                return true
            }

            override fun onQueryTextChange(newText: String?): Boolean {
                marquee.visibility = if (newText.isNullOrEmpty()) View.VISIBLE else View.GONE
                val q = newText?.trim() ?: ""
                when {
                    q.length >= 2 -> despacharBusca(q)
                    q.isEmpty() -> mostrarEstadoVazio("Digite o nome ou o código de barras")
                }
                return true
            }
        })

        // Botão câmera → abre scanner no modo "search"
        view.findViewById<ImageButton>(R.id.btn_scan_barcode)?.setOnClickListener {
            val bundle = Bundle().apply {
                putString("caller", BarcodeScanFragment.CALLER_SEARCH)
            }
            findNavController().navigate(R.id.action_search_to_barcode_scanner, bundle)
        }

        // Resultado do scanner via SavedStateHandle
        findNavController()
            .currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<String>(BarcodeScanFragment.KEY_SCANNED_BARCODE)
            ?.observe(viewLifecycleOwner) { barcode ->
                if (!barcode.isNullOrEmpty()) {
                    searchView?.setQuery(barcode, true) // Dispara a busca
                    findNavController()
                        .currentBackStackEntry
                        ?.savedStateHandle
                        ?.set(BarcodeScanFragment.KEY_SCANNED_BARCODE, "")
                }
            }

        return view
    }

    override fun onDestroyView() {
        super.onDestroyView()
        searchJob?.cancel()
        recyclerSearch = null
        layoutEmpty = null
        txtHint = null
        adapter = null
    }

    private fun despacharBusca(query: String) {
        when {
            query.all { it.isDigit() } -> {
                buscarPorCodigoBarras(query)
            }

            else -> {
                buscarPorNome(query)
            }
        }
    }

    // ── ✅ CORREÇÃO 2: Implementando as buscas de forma offline-first e reativa via Room ──

    private fun buscarPorNome(query: String) {
        txtHint?.text = "Buscando produto: $query"
        val database = BpDatabase.getDatabase(requireContext())
        val flow = database.productDao().searchProductsByName(query)
        coletarResultados(flow)
    }

    private fun buscarPorCodigoBarras(barcode: String) {
        txtHint?.text = "🔢 Código de barras: $barcode"
        val database = BpDatabase.getDatabase(requireContext())
        val flow = database.productDao().searchProductsByBarcode(barcode)
        coletarResultados(flow)
    }

    private fun coletarResultados(flow: Flow<List<PostProduct>>) {
        searchJob?.cancel() // Cancela a escuta da busca anterior para evitar conflito
        searchJob = viewLifecycleOwner.lifecycleScope.launch {
            flow.collectLatest { produtos ->
                mostrarResultados(produtos)
            }
        }
    }

    private fun mostrarResultados(produtos: List<PostProduct>?) {
        if (produtos.isNullOrEmpty()) {
            mostrarEstadoVazio("Nenhum produto encontrado")
        } else {
            layoutEmpty?.visibility = View.GONE
            recyclerSearch?.visibility = View.VISIBLE
            adapter?.setDados(produtos)
        }
    }

    private fun mostrarEstadoVazio(mensagem: String) {
        txtHint?.text = mensagem
        layoutEmpty?.visibility = View.VISIBLE
        recyclerSearch?.visibility = View.GONE
    }
}