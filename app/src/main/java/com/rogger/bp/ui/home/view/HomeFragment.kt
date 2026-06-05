package com.rogger.bp.ui.home.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.rogger.bp.R
import com.rogger.bp.data.model.PostCategory
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.databinding.FragmentHomeBinding
import com.rogger.bp.notification.NotificationPrefs
import com.rogger.bp.ui.base.CategoriaDialogUtil
import com.rogger.bp.ui.base.Utils
import com.rogger.bp.ui.commun.DependencyInjector
import com.rogger.bp.ui.home.ContractHome
import com.rogger.bp.ui.home.CustomProgressBar
import com.rogger.bp.ui.home.OnItemClickListener
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


    override lateinit var presenter: ContractHome.Presenter


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
        val category = DependencyInjector.registerCategoryRepository(requireContext())
        val homeRepository = DependencyInjector.registerHomeRepository(requireContext())
        presenter = HomePresenter(this, homeRepository, category)

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
        if (::presenter.isInitialized) presenter.onDestroy()
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
                    val bundle = Bundle().apply {
                        putString("uuid", produto.uuid)
                        putParcelable("product_bundle", produto)
                    }
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

                    override fun onCategoriaSelecionada(categoriaId: String) {
                        val nome = currentCategories
                            .firstOrNull { it.firestoreId == categoriaId }?.name ?: ""
                        Log.e("Home_fragment", " categoria id ${categoriaId} name:${nome}")
                        navegarParaScanner(categoriaId, nome)

                    }

                    override fun onAdicionarCategoria() {
                        Utils.dialogCategory(
                            requireContext(),
                            "Nova Categoria"
                        ) { nomeCategoria ->
                            navegarParaScanner(categoriaId = "", nomeCategoria)
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
                        if (products != null) {
                            adapterHome.setDados(products)
                            adapterHome.ordenarPorDiferencaDeDias()
                            showEmpty(products.isEmpty())
                        }
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


    private fun navegarParaScanner(categoriaId: String, categoriaNome: String) {
        val bundle = Bundle().apply {
            putString("categoria_id", categoriaId)
            putString("categoria_nome", categoriaNome)
        }
        findNavController().navigate(
            R.id.action_nav_home_to_nav_barcode_scan_fragment,
            bundle
        )
    }

    override fun showProgress(enable: Boolean) {
        if (enable) CustomProgressBar.showLoadingDialog(requireContext())
        else CustomProgressBar.hideLoadingDialog()
    }

    override fun showEmpty(isEmpty: Boolean) {
        val target = if (isEmpty) 1 else 0
        if (binding.viewFlipper.displayedChild != target) {
            binding.viewFlipper.displayedChild = target
            binding.txtHomeFlipper.visibility = View.VISIBLE
            binding.txtHomeFlipper.setText(" Click em mais para adicionar item")
        } else {
            binding.txtHomeFlipper.visibility = View.GONE
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
        onSuccess("\"${product.name}\" eliminado")
    }

    override fun onProductRestored(product: PostProduct) {
        onSuccess("\"${product.name}\" restaurado")
    }
}