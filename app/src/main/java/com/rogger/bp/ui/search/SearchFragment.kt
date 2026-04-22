package com.rogger.bp.ui.search

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageButton
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.widget.SearchView
import androidx.appcompat.widget.Toolbar
import androidx.fragment.app.Fragment
import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.rogger.bp.R
import com.rogger.bp.data.model.Produto
import com.rogger.bp.ui.scanner.BarcodeScanFragment
import com.rogger.bp.ui.viewmodel.DataViewModel


class  SearchFragment : Fragment() {

    private var dataViewModel: DataViewModel? = null
    private var adapter: SearchAdapter? = null
    private var recyclerSearch: RecyclerView? = null
    private var layoutEmpty: LinearLayout? = null
    private var txtHint: TextView? = null

    private var activeObserver: Observer<List<Produto>>? = null
    private var activeLiveData: LiveData<List<Produto>>? = null

    // Referência à toolbar global do MainActivity
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

        dataViewModel = ViewModelProvider(this).get(DataViewModel::class.java)

        recyclerSearch = view.findViewById(R.id.recycler_search)
        layoutEmpty    = view.findViewById(R.id.layout_search_empty)
        txtHint        = view.findViewById(R.id.txt_search_hint)
        val marquee = view.findViewById<TextView>(R.id.txt_marquee_hint)

        adapter = SearchAdapter(requireContext()) { produto ->
            val bundle = Bundle().apply { putInt("id", produto.id) }
            findNavController().navigate(R.id.action_nav_search_to_nav_edit_fragment, bundle)
        }

        recyclerSearch?.layoutManager = LinearLayoutManager(requireContext())
        recyclerSearch?.adapter = adapter

        // Seta de voltar
        view.findViewById<ImageButton>(R.id.btn_back)?.setOnClickListener {
            findNavController().popBackStack()
        }

        // SearchView
        val searchView = view.findViewById<SearchView>(R.id.search_view)

        // ──────────────────────────────────────────────────────────────
        // Atualiza o hint para indicar ao usuário o atalho de categoria
        // ──────────────────────────────────────────────────────────────
        searchView?.queryHint = "Nome do produto ou @categoria…"
        searchView.setOnQueryTextFocusChangeListener { _, hasFocus ->
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
                    q.isEmpty()   -> mostrarEstadoVazio(getString(R.string.search_hint_default))
                }
                return true
            }
        })
        // Quando começa a digitar → esconde

        // ── Filtro inicial via argumento de navegação (Categoria) ──────
        val categoriaNome = arguments?.getString("categoria_nome")
        if (!categoriaNome.isNullOrEmpty()) {
            // Preenche a SearchView com "@NomeCategoria" para deixar claro o modo
            val queryInicial = "@$categoriaNome"
            searchView?.setQuery(queryInicial, false)
            buscarPorCategoria(categoriaNome)
            txtHint?.text = "📂 Categoria: $categoriaNome"
        }

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
                    buscarPorCodigoBarras(barcode)
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
        activeObserver?.let { activeLiveData?.removeObserver(it) }
        recyclerSearch = null
        layoutEmpty    = null
        txtHint        = null
        adapter        = null
    }

    // ── Despachador principal ─────────────────────────────────────────
    //
    // Regra de roteamento:
    //   • Query começa com "@"  →  busca por CATEGORIA (remove o "@")
    //   • Caso contrário        →  busca por NOME do produto (comportamento original)
    //
    // Exemplos:
    //   "@laticinios"  → lista todos os produtos da categoria "laticinios"
    //   "@beb"         → lista produtos de categorias que contêm "beb" (bebidas, etc.)
    //   "leite"        → lista produtos cujo nome contém "leite"
    // ─────────────────────────────────────────────────────────────────
    private fun despacharBusca(query: String) {
        if (query.startsWith("@")) {
            val nomeCategoria = query.removePrefix("@").trim()
            if (nomeCategoria.length >= 1) {
                buscarPorCategoria(nomeCategoria)
            } else {
                mostrarEstadoVazio("Digite o nome da categoria após @")
            }
        } else {
            buscarPorNome(query)
        }
    }

    // ── Métodos de busca ──────────────────────────────────────────────

    private fun buscarPorNome(query: String) {
        txtHint?.text = "Buscando produto: $query"
        substituirObserver(dataViewModel!!.buscarPorNome(query))
    }

    /**
     * 🆕 Busca todos os produtos que pertencem a categorias cujo nome
     * contenha [query]. A correspondência é parcial e case-insensitive
     * (implementada no SQL via LOWER + LIKE).
     */
    private fun buscarPorCategoria(query: String) {
        txtHint?.text = "📂 Categoria: $query"
        substituirObserver(dataViewModel!!.buscarPorNomeCategoria(query))
    }

    private fun buscarPorCodigoBarras(barcode: String) {
        txtHint?.text = "Buscando por código: $barcode"
        substituirObserver(dataViewModel!!.buscarPorCodigoBarras(barcode))
    }

    private fun substituirObserver(novoLiveData: LiveData<List<Produto>>) {
        activeObserver?.let { activeLiveData?.removeObserver(it) }
        val observer = Observer<List<Produto>> { produtos -> mostrarResultados(produtos) }
        activeLiveData = novoLiveData
        activeObserver = observer
        novoLiveData.observe(viewLifecycleOwner, observer)
    }

    // ── UI ────────────────────────────────────────────────────────────

    private fun mostrarResultados(produtos: List<Produto>?) {
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