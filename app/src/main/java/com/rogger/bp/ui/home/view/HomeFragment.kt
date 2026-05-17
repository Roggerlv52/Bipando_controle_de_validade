package com.rogger.bp.ui.home.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
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

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 13/05/2026
 * Hora: 22:36
 */
class HomeFragment : Fragment(), ContractHome.View {

    private var _binding: FragmentHomeBinding? = null
    private val binding get() = _binding!!

    override val presenter: ContractHome.Presenter by lazy {
        HomePresenter(
            view               = this,
            repository         = HomeRepository(HomeDataSource()),
            categoryRepository = CategoryRepository(CategoryDataSource())
        )
    }

    private lateinit var adapterHome: AdapterHome

    private var listaCategorias: List<PostCategory> = emptyList()

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

    private fun setupRecyclerView() {
        val diasAmarelo = NotificationPrefs.getDays(requireContext())

        adapterHome = AdapterHome(
            requireContext(),
            diasAmarelo,
            object : OnItemClickListener {

                override fun onItemClick(
                    position: Int,
                    data: List<PostProduct>
                ) {
                    val produto = data.getOrNull(position) ?: return
                    val bundle  = Bundle().apply { putString("uuid", produto.uuid) }
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
        binding.rcViewListHome.adapter       = adapterHome
    }

    private fun setupFab() {
        binding.fab.setOnClickListener {

            CategoriaDialogUtil.mostrarDialogo(
                requireContext(),
                listaCategorias,
                object : CategoriaDialogUtil.CategoriaCallback {

                    override fun onCategoriaSelecionada(categoriaId: Int) {
                        val postCat = listaCategorias.firstOrNull { it.id == categoriaId }
                        navegarParaScanner(
                            categoriaId   = categoriaId,
                            categoriaNome = postCat?.name ?: ""
                        )
                    }

                    override fun onAdicionarCategoria() {
                        Utils.dialogCategory(
                            requireContext(),
                            "Nova Categoria"
                        ) { nomeCategoria ->

                            val nova = PostCategory(name = nomeCategoria)
                            presenter.fetchCategories()
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

    private fun observeBarcodeResult() {
        findNavController()
            .currentBackStackEntry
            ?.savedStateHandle
            ?.getLiveData<String>(BarcodeScanFragment.KEY_SCANNED_BARCODE)
            ?.observe(viewLifecycleOwner) { barcode ->
                if (!barcode.isNullOrEmpty()) {
                    findNavController()
                        .currentBackStackEntry
                        ?.savedStateHandle
                        ?.set(BarcodeScanFragment.KEY_SCANNED_BARCODE, "")
                }
            }
    }

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
    override fun showProgress(enable: Boolean) {
        if (enable) CustomProgressBar.showLoadingDialog(requireContext())
        else        CustomProgressBar.hideLoadingDialog()
    }

    override fun showProducts(products: List<PostProduct>) {
        showEmpty(products.isEmpty())
        if (products.isNotEmpty()) {
            adapterHome.setDados(products)
            adapterHome.ordenarPorDiferencaDeDias()
        }
    }

    override fun showCategories(categories: List<PostCategory>) {
        listaCategorias = categories
    }

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
}
