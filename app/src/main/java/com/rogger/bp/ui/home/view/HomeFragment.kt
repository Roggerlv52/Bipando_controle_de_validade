package com.rogger.bp.ui.home.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.rogger.bp.R
import com.rogger.bp.data.database.BpDatabase
import com.rogger.bp.data.database.RoomProductCache
import com.rogger.bp.data.model.PostCategory
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.databinding.FragmentHomeBinding
import com.rogger.bp.notification.NotificationPrefs
import com.rogger.bp.ui.base.CategoriaDialogUtil
import com.rogger.bp.ui.base.Utils
import com.rogger.bp.ui.category.data.CategoryDataSource
import com.rogger.bp.ui.category.data.CategoryRepository
import com.rogger.bp.ui.category.data.RoomCategoryCache
import com.rogger.bp.ui.home.ContractHome
import com.rogger.bp.ui.home.CustomProgressBar
import com.rogger.bp.ui.home.OnItemClickListener
import com.rogger.bp.ui.home.data.HomeDataSource
import com.rogger.bp.ui.home.data.HomeRepository
import com.rogger.bp.ui.home.presentation.HomePresenter
import kotlinx.coroutines.launch

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 13/05/2026
 * Hora: 22:36
 */
class HomeFragment : Fragment(), ContractHome.View {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    private var _presenter: HomePresenter? = null
    override val presenter: ContractHome.Presenter
        get() = _presenter!!

    private lateinit var adapterHome: AdapterHome
    private var currentCategories: List<PostCategory> = emptyList() // Para o dialog do FAB

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

        // Inicialização do Room e DAOs
        val database = BpDatabase.getDatabase(requireContext())
        val productDao = database.productDao()
        val categoryDao = database.categoryDao()

        // Inicialização dos Caches
        val roomProductCache = RoomProductCache(productDao)
        val roomCategoryCache = RoomCategoryCache(categoryDao)

        // Inicialização dos DataSources
        val homeDataSource = HomeDataSource()
        val categoryDataSource = CategoryDataSource()

        // Inicialização dos Repositórios
        val homeRepository = HomeRepository(homeDataSource, roomProductCache)
        val categoryRepository = CategoryRepository(categoryDataSource, roomCategoryCache)

        _presenter = HomePresenter(
            view = this,
            repository = homeRepository,
            categoryRepository = categoryRepository
        )

        setupRecyclerView()
        setupFab()
        observePresenterFlows()

        // Inicia o carregamento e o listener do Firestore
        presenter.fetchProducts()
        presenter.fetchCategories()
    }

    override fun onResume() {
        super.onResume()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        _presenter?.onDestroy()
        _presenter = null
        super.onDestroy()
    }

    private fun setupRecyclerView() {
        val diasAmarelo = NotificationPrefs.getDays(requireContext())

        adapterHome = AdapterHome(
            requireContext(),
            diasAmarelo,
            object : OnItemClickListener {

                override fun onItemClick(position: Int, data: List<PostProduct>) {
                    val produto = data.getOrNull(position) ?: return
                    val bundle = Bundle().apply { putString("uuid", produto.uuid) }
                    findNavController().navigate(
                        R.id.action_nav_home_to_nav_edit_fragment,
                        bundle
                    )
                }

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
        binding.rcViewListHome.adapter = adapterHome
    }

    private fun setupFab() {
        binding.fab.setOnClickListener {
            CategoriaDialogUtil.mostrarDialogo(
                requireContext(),
                currentCategories, // Usa a lista de categorias observada
                object : CategoriaDialogUtil.CategoriaCallback {

                    override fun onCategoriaSelecionada(categoriaId: Int) {
                        val nome = currentCategories
                            .firstOrNull { it.id == categoriaId }?.name ?: ""
                        navegarParaScanner(categoriaId, nome)
                    }

                    override fun onAdicionarCategoria() {
                        Utils.dialogCategory(
                            requireContext(),
                            "Nova Categoria"
                        ) { nomeCategoria ->
                            // A criação de categoria deve ser feita via Presenter/Repository
                            // e o listener do Firestore se encarregará de atualizar o cache e, consequentemente, o Flow.
                            // Por enquanto, apenas navegamos para o scanner com a nova categoria.
                            navegarParaScanner(-1, nomeCategoria)
                        }
                    }
                }
            )
        }
    }

    private fun observePresenterFlows() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { // Observa os produtos
                    // Adicione o "as HomePresenter" aqui
                    (presenter as HomePresenter).products.collect { products ->
                        adapterHome.setDados(products)
                        adapterHome.ordenarPorDiferencaDeDias()
                        showEmpty(products.isEmpty())
                    }
                }
                launch { // Observa as categorias
                    // Adicione o "as HomePresenter" aqui também
                    (presenter as HomePresenter).categories.collect { categories ->
                        currentCategories = categories
                    }
                }
            }
        }
    }


    private fun navegarParaScanner(categoriaId: Int, categoriaNome: String) {
        val bundle = Bundle().apply {
            putInt("categoria_id", categoriaId)
            putString("categoria_nome", categoriaNome)
        }
        findNavController().navigate(
            R.id.action_nav_home_to_nav_barcode_scan_fragment,
            bundle
        )
    }

    override fun showProgress(enable: Boolean) {
        val b = _binding ?: return
        if (enable) CustomProgressBar.showLoadingDialog(requireContext())
        else CustomProgressBar.hideLoadingDialog()
    }

    override fun showEmpty(isEmpty: Boolean) {
        val b = _binding ?: return
        val target = if (isEmpty) 1 else 0
        if (b.viewFlipper.displayedChild != target) {
            b.viewFlipper.displayedChild = target
        }
    }

    override fun onSuccess(message: String) {
        val b = _binding ?: return
        Snackbar.make(b.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onError(message: String) {
        val b = _binding ?: return
        Snackbar.make(b.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onProductDeleted(product: PostProduct) {
        // A lista já será atualizada via Flow, apenas mostra a mensagem
        onSuccess("\"${product.name}\" eliminado")
    }

    override fun onProductRestored(product: PostProduct) {
        // A lista já será atualizada via Flow, apenas mostra a mensagem
        onSuccess("\"${product.name}\" restaurado")
    }
}