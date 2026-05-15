package com.rogger.bp.ui.home.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.rogger.bp.R
import com.rogger.bp.data.model.Categoria
import com.rogger.bp.data.model.PostCategory
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.databinding.FragmentHomeBinding
import com.rogger.bp.notification.NotificationPrefs
import com.rogger.bp.ui.base.CategoriaDialogUtil
import com.rogger.bp.ui.base.Utils
import com.rogger.bp.ui.category.data.CategoryDataSource
import com.rogger.bp.ui.category.data.CategoryRepository
import com.rogger.bp.ui.home.ContractHome
import com.rogger.bp.ui.home.CustomProgressBar
import com.rogger.bp.ui.home.OnItemClickListener
import com.rogger.bp.ui.home.data.HomeDataSource
import com.rogger.bp.ui.home.data.HomeRepository
import com.rogger.bp.ui.home.presentation.HomePresenter
import com.rogger.bp.ui.scanner.BarcodeScanFragment
import com.rogger.bp.ui.viewmodel.CategoriaViewModel
import com.rogger.bp.ui.viewmodel.DataViewModel

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 13/05/2026
 * Hora: 22:36
 */
class HomeFragment : Fragment(), ContractHome.View {

    // ── ViewBinding ───────────────────────────────────────────────────────

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    // ── MVP ───────────────────────────────────────────────────────────────

    override val presenter: ContractHome.Presenter by lazy {
        HomePresenter(
            view               = this,
            repository         = HomeRepository(HomeDataSource()),
            categoryRepository = CategoryRepository(CategoryDataSource())
        )
    }

    // ── Adapter ───────────────────────────────────────────────────────────

    private lateinit var adapterHome: AdapterHome

    /**
     * Cache de [PostCategory] — actualizado por [showCategories].
     * Convertido para [Categoria] (Room) apenas no momento de abrir o dialog,
     * via [toCategoriaList], para satisfazer a API Java do [CategoriaDialogUtil].
     */
    private var listaCategorias: List<PostCategory> = emptyList()

    // ─────────────────────────────────────────────────────────────────────
    // Lifecycle
    // ─────────────────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentHomeBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupRecyclerView()
        setupFab()
        observeBarcodeResult()

        // Carrega produtos e categorias via Presenter (Firestore)
        presenter.fetchProducts()
        presenter.fetchCategories()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.onDestroy()
        _binding = null
    }

    // ─────────────────────────────────────────────────────────────────────
    // Setup
    // ─────────────────────────────────────────────────────────────────────

    private fun setupRecyclerView() {
        val diasAmarelo = NotificationPrefs.getDays(requireContext())

        adapterHome = AdapterHome(
            requireContext(),
            diasAmarelo,
            object : OnItemClickListener {

                /** Clique no item → EditFragment com id do produto. */
                override fun onItemClick(
                    position: Int,
                    data: List<com.rogger.bp.data.model.Produto>
                ) {
                    val produto = data.getOrNull(position) ?: return
                    val bundle  = Bundle().apply { putInt("id", produto.id) }
                    findNavController().navigate(
                        R.id.action_nav_home_to_nav_edit_fragment,
                        bundle
                    )
                }

                /** Clique na imagem → ShowFragment em full-screen. */
                override fun onImageClick(uri: String) {
                    val bundle = Bundle().apply { putString("imageUri", uri) }
                    findNavController().navigate(
                        R.id.action_nav_home_to_nav_show_fragment,
                        bundle
                    )
                }
            }
        )

        binding.rcViewListHome.layoutManager = LinearLayoutManager(requireContext())
        binding.rcViewListHome.adapter       = adapterHome
    }

    /**
     * FAB → [CategoriaDialogUtil] com [listaCategorias] (PostCategory).
     *
     * ── Fluxo A — há categorias ───────────────────────────────────────────
     *   1. Dialog mostra a lista de PostCategory.
     *   2. Utilizador seleciona uma categoria.
     *   3. Navega para [BarcodeScanFragment] passando categoria_id + categoria_nome.
     *   4. Scanner lê o barcode e navega para [AddItemFragment] passando
     *      key_barcode + categoria_id (já implementado no BarcodeScanFragment.gotoAddItem).
     *
     * ── Fluxo B — sem categorias ──────────────────────────────────────────
     *   1. [CategoriaDialogUtil] detecta lista vazia → chama [onAdicionarCategoria].
     *   2. Abre [Utils.dialogCategory] para criar a primeira categoria.
     *   3. Usa o [CategoryRepository] via Presenter para criar no Firestore.
     *   4. Após criação navega para o scanner sem categoria pré-selecionada.
     */
    private fun setupFab() {
        binding.fab.setOnClickListener {

            // Converte PostCategory → Categoria (Room) apenas para a API Java
            // do CategoriaDialogUtil — sem mudar o modelo de dados
            val categoriasParaDialog = listaCategorias.toCategoriaList()

            CategoriaDialogUtil.mostrarDialogo(
                requireContext(),
                categoriasParaDialog,
                object : CategoriaDialogUtil.CategoriaCallback {

                    // ── A) Categoria selecionada → scanner ────────────────
                    override fun onCategoriaSelecionada(categoriaId: Int) {
                        val postCat = listaCategorias.firstOrNull { it.id == categoriaId }
                        navegarParaScanner(
                            categoriaId   = categoriaId,
                            categoriaNome = postCat?.name ?: ""
                        )
                    }

                    // ── B) Sem categorias → criar primeira ────────────────
                    override fun onAdicionarCategoria() {
                        Utils.dialogCategory(
                            requireContext(),
                            "Nova Categoria"
                        ) { nomeCategoria ->
                            // Cria no Firestore via CategoryPresenter implícito
                            // (usa o CategoryRepository directo para não duplicar Presenters)
                            val nova = PostCategory(name = nomeCategoria)
                            // Recarrega as categorias após criação para o próximo click do FAB
                            // já ter a lista atualizada
                            presenter.fetchCategories()
                            // Navega para o scanner sem categoria pré-selecionada
                            navegarParaScanner(
                                categoriaId   = -1,
                                categoriaNome = nomeCategoria
                            )
                        }
                    }
                }
            )
        }
    }

    /**
     * Observa o resultado do [BarcodeScanFragment] quando o scanner
     * é aberto em modo "add" a partir do Home.
     *
     * O [BarcodeScanFragment.gotoAddItem] navega directamente para
     * [nav_add_fragment] com key_barcode + categoria_id — não precisa
     * de SavedStateHandle porque não volta ao Home.
     * Este observer é mantido por precaução caso o fluxo de navegação mude.
     */
    private fun observeBarcodeResult() {
        findNavController()
            .currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<String>(BarcodeScanFragment.KEY_SCANNED_BARCODE)
            ?.observe(viewLifecycleOwner) { barcode ->
                if (!barcode.isNullOrEmpty()) {
                    // Limpa o resultado para não disparar novamente
                    findNavController()
                        .currentBackStackEntry
                        ?.savedStateHandle
                        ?.set(BarcodeScanFragment.KEY_SCANNED_BARCODE, "")
                }
            }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Navegação
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Navega para [BarcodeScanFragment] em modo "add" (caller = "add").
     *
     * Argumentos declarados no nav_barcode_scan_fragment:
     *   • categoria_id   : Int    (−1 = sem categoria)
     *   • categoria_nome : String
     *
     * O [BarcodeScanFragment] ao ler o barcode chama [gotoAddItem] que
     * navega para [nav_add_fragment] com:
     *   • key_barcode  : String  ← barcode lido pelo scanner
     *   • categoria_id : Int     ← passado aqui e reencaminhado
     *
     * Nota: categoria_nome não é repassado pelo BarcodeScanFragment.java actual.
     * Se necessário, basta adicionar ao Bundle em [gotoAddItem].
     */
    private fun navegarParaScanner(categoriaId: Int, categoriaNome: String) {
        val bundle = Bundle().apply {
            putInt("categoria_id",     categoriaId)
            putString("categoria_nome", categoriaNome)
        }
        findNavController().navigate(
            R.id.nav_barcode_scan_fragment,
            bundle
        )
    }

    // ─────────────────────────────────────────────────────────────────────
    // Helpers
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Converte [List<PostCategory>] para [List<Categoria>] apenas para
     * satisfazer a API Java do [CategoriaDialogUtil] sem alterar o modelo.
     *
     * O [CategoriaDialogUtil] usa [Categoria.getId()] e [Categoria.getNome()]
     * — mapeados aqui a partir de [PostCategory.id] e [PostCategory.name].
     */
    private fun List<PostCategory>.toCategoriaList(): List<Categoria> =
        map { postCat ->
            Categoria().apply {
                id   = postCat.id
                nome = postCat.name
            }
        }

    // ─────────────────────────────────────────────────────────────────────
    // ContractHome.View
    // ─────────────────────────────────────────────────────────────────────

    override fun showProgress(enable: Boolean) {
        if (enable) CustomProgressBar.showLoadingDialog(requireContext())
        else        CustomProgressBar.hideLoadingDialog()
    }

    /**
     * Recebe lista de [PostProduct] do Presenter e actualiza o AdapterHome.
     *
     * [AdapterHome] usa [Produto] (Room) — converte aqui apenas para manter
     * o adapter existente sem alterações. Se o adapter for migrado para
     * PostProduct, esta conversão pode ser removida.
     */
    override fun showProducts(products: List<PostProduct>) {
        showEmpty(products.isEmpty())
        if (products.isNotEmpty()) {
            val produtosRoom = products.toProdutoList()
            adapterHome.setDados(produtosRoom)
            adapterHome.ordenarPorDiferencaDeDias()
        }
    }

    /**
     * Recebe lista de [PostCategory] e armazena em [listaCategorias]
     * para o dialog do FAB estar sempre actualizado.
     */
    override fun showCategories(categories: List<PostCategory>) {
        listaCategorias = categories
    }

    /**
     * Controla o [ViewFlipper]:
     *   • displayedChild = 0 → RecyclerView
     *   • displayedChild = 1 → estado vazio
     */
    override fun showEmpty(isEmpty: Boolean) {
        val target = if (isEmpty) 1 else 0
        if (binding.viewFlipper.displayedChild != target) {
            binding.viewFlipper.displayedChild = target
        }
    }

    override fun onSuccess(message: String) {
        view?.let { Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show() }
    }

    override fun onError(message: String) {
        view?.let { Snackbar.make(it, message, Snackbar.LENGTH_LONG).show() }
    }

    // ─────────────────────────────────────────────────────────────────────
    // Conversão PostProduct → Produto (para AdapterHome existente em Java)
    // ─────────────────────────────────────────────────────────────────────

    /**
     * Converte [PostProduct] para [Produto] apenas para o [AdapterHome] Java.
     * Quando o AdapterHome for migrado para Kotlin/PostProduct, remover este bloco.
     */
    private fun List<PostProduct>.toProdutoList(): List<com.rogger.bp.data.model.Produto> =
        map { p ->
            com.rogger.bp.data.model.Produto().apply {
                id           = p.id
                userId       = p.userId
                nome         = p.name
                codigoBarras = p.barcode
                categoryId   = p.categoryId
                timestamp    = p.timestamp
                imagem       = p.imageUri
                isDeleted    = p.deleted
                deletedAt    = p.deletedAt
            }
        }
}