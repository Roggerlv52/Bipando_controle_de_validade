package com.rogger.bp.ui.home.view

import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.ViewCompat
import androidx.core.view.WindowInsetsCompat
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.rogger.bp.R
import com.rogger.bp.data.database.BpDatabase
import com.rogger.bp.data.model.PostCategory
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.databinding.FragmentHomeBinding
import com.rogger.bp.notification.NotificationPrefs
import com.rogger.bp.ui.base.CategoriaDialogUtil
import com.rogger.bp.ui.base.DialogUtil
import com.rogger.bp.ui.commun.DependencyInjector
import com.rogger.bp.ui.commun.SharedPreferencesManager
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
    private var totalDeProdutosDoUtilizador = 0

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

        binding?.let { b ->
            ViewCompat.setOnApplyWindowInsetsListener(b.fab) { v, insets ->
                val bars = insets.getInsets(WindowInsetsCompat.Type.navigationBars())
                val params = v.layoutParams as? ViewGroup.MarginLayoutParams
                params?.let {
                    // Define a margem inferior baseada na altura da barra de navegação + 16dp de respiro
                    it.bottomMargin = bars.bottom + 64
                    v.layoutParams = it
                }
                insets
            }
        }
        _binding?.let { b ->
            b.layoutEmptyStateAdd.setOnClickListener {
                // Simula o clique do botão FAB principal para abrir o diálogo de categorias
                b.fab.performClick()
            }
        }
        setupRecyclerView()
        // ✅ ATUALIZAÇÃO: Escuta dinamicamente a contagem total de produtos no banco de dados local
        val database = BpDatabase.getDatabase(requireContext())
        database.productDao().getTotalProductsCountLiveData().observe(viewLifecycleOwner) { count ->
            totalDeProdutosDoUtilizador = count ?: 0
        }
        setupFab()
        observePresenterFlows()
        // ── ✅ CORREÇÃO: Lê os argumentos para aplicar filtro de categoria se houver ──
        val categoryId = arguments?.getString("categoria_id").orEmpty()
        val categoryNome = arguments?.getString("categoria_nome").orEmpty()

        if (categoryId.isNotEmpty()) {
            // Atualiza o título da Toolbar para mostrar que a categoria está ativa
            (activity as? AppCompatActivity)?.supportActionBar?.title = "${
                requireContext().getString(R.string.category)
            }: $categoryNome"
            presenter.fetchProductsByCategory(categoryId)
        } else {
            // Comportamento normal caso venha do menu lateral direto
            presenter.fetchProducts()
        }

        presenter.fetchCategories()
        // Se não há categoria, fetchProducts() já carregou tudo normalmente
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
                override fun onHeaderDeleteClick(productsToDelete: List<PostProduct>, groupTitle: String) {
                    val total = productsToDelete.size
                    val mensagem = if (total == 1) {
                        getString(R.string.delete_confirm_single, productsToDelete.first().name)
                    } else {
                        getString(R.string.delete_confirm_multiple, total, groupTitle)
                    }

                    DialogUtil.showDeleteDialog(requireContext(), mensagem) {
                        CustomProgressBar.showLoadingDialog(requireContext())

                        // Executa a deleção sequencialmente para cada item do lote
                        productsToDelete.forEach { product ->
                            presenter.deleteProduct(product)
                        }

                        // Temporizador simples para ocultar o progresso
                        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                            CustomProgressBar.hideLoadingDialog()
                            // O fluxo reativo de dados atualizará a UI automaticamente
                        }, 1000)
                    }
                }
            }
        )

        binding.rcViewListHome.layoutManager = LinearLayoutManager(requireContext())
        binding.rcViewListHome.adapter = adapterHome
    }

    private fun setupFab() {
        binding.fab.setOnClickListener {
            val isPremium = SharedPreferencesManager.isPremium(requireContext())

            // ✅ ATUALIZAÇÃO: Bloqueia com base no total absoluto (ativos + lixeira) do banco de dados
            if (!isPremium && totalDeProdutosDoUtilizador >= 50) {
                mostrarDialogoLimitePremium()
                return@setOnClickListener
            }

            // Fluxo normal de abertura do diálogo de categorias
            CategoriaDialogUtil.mostrarDialogo(
                requireContext(),
                currentCategories,
                object : CategoriaDialogUtil.CategoriaCallback {

                    override fun onCategoriaSelecionada(categoriaId: String) {
                        val nome = currentCategories
                            .firstOrNull { it.firestoreId == categoriaId }?.name ?: ""
                        navegarParaScanner(categoriaId, nome)
                    }

                    override fun onAdicionarCategoria() {
                        AlertDialog.Builder(requireContext())
                            .setTitle(getString(R.string.dialog_title_no_category))
                            .setMessage(getString(R.string.dialog_msg_start_managing))
                            .setPositiveButton(getString(R.string.dialog_add_category_title)) { dialog, _ ->
                                dialog.dismiss()
                                findNavController().navigate(R.id.nav_category)
                            }
                            .setNegativeButton(getString(
                                R.string.dialog_button_not_now
                            )) { dialog, _ ->
                                dialog.dismiss()
                            }
                            .show()
                    }
                }
            )
        }
    }

    private fun observePresenterFlows() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch {
                    (presenter as HomePresenter).products.collect { products ->
                        if (products != null) {
                            adapterHome.setDados(products)
                            showEmpty(products.isEmpty())
                        }
                    }
                }
                launch {
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
    }

    override fun showEmpty(isEmpty: Boolean) {
        val target = if (isEmpty) 1 else 0
        if (binding.viewFlipper.displayedChild != target) {
            binding.viewFlipper.displayedChild = target
        }
    }

    private fun mostrarDialogoLimitePremium() {
        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.premium_limit_title))
            .setMessage(getString(R.string.premium_limit_message))
            .setPositiveButton(getString(R.string.premium_limit_positive)) { dialog, _ ->
                dialog.dismiss()
                findNavController().navigate(R.id.nav_payment)
            }
            .setNegativeButton(getString(R.string.premium_limit_negative)) { dialog, _ ->
                dialog.dismiss()
            }
            .show()
    }

    override fun onSuccess(name: String) {
        val b = _binding ?: return
        Snackbar.make(b.root,
            getString(R.string.product_deleted_success, name),
            Snackbar.LENGTH_SHORT).show()
    }

    override fun onError(message: String) {
        val b = _binding ?: return
        Snackbar.make(b.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onProductDeleted(product: PostProduct) {
        onSuccess(getString(R.string.product_deleted_success, product.name))
    }

    override fun onProductRestored(product: PostProduct) {
        onSuccess(getString(R.string.product_deleted_restored, product.name))
    }
}