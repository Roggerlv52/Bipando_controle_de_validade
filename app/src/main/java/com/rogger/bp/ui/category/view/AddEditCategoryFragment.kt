package com.rogger.bp.ui.category.view

import android.os.Bundle
import android.view.ActionMode
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.fragment.findNavController
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.snackbar.Snackbar
import com.rogger.bp.R
import com.rogger.bp.data.model.PostCategory
import com.rogger.bp.databinding.FragmentAddEditCategoryBinding
import com.rogger.bp.ui.base.DialogUtil
import com.rogger.bp.ui.base.Utils
import com.rogger.bp.ui.category.ContractCategory
import com.rogger.bp.ui.category.presentation.CategoryPresenter
import com.rogger.bp.ui.commun.DependencyInjector
import kotlinx.coroutines.launch

class AddEditCategoryFragment : Fragment(), ContractCategory.View {

    override lateinit var presenter: ContractCategory.Presenter

    private var _binding: FragmentAddEditCategoryBinding? = null
    private val binding get() = _binding!!

    private lateinit var adapter: AdapterCategory
    private var actionMode: ActionMode? = null

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddEditCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val categoryRepository = DependencyInjector.registerCategoryRepository(requireContext())
        presenter = CategoryPresenter(this, categoryRepository)

        binding.recyclerViewCategory.layoutManager = LinearLayoutManager(requireContext())

        setupAdapter()
        binding.recyclerViewCategory.adapter = adapter

        setupListeners()
        observePresenterFlows()

        presenter.fetchCategories()
    }

    private fun setupAdapter() {
        adapter = AdapterCategory(object : AdapterCategory.OnCategoriaListener {

            override fun onClick(categoria: PostCategory) {
                // 👉 Modificado: Navega direto para a Home aplicando o filtro
                navigateToHomeFiltered(categoria.firestoreId, categoria.name)
            }

            override fun onMenuEditClick(categoria: PostCategory) {
                openEditDialog(categoria)
            }

            override fun onSelectionChanged(total: Int) {
                when {
                    total > 0 && actionMode == null ->
                        actionMode = requireActivity().startActionMode(actionModeCallback)

                    total == 0 -> actionMode?.finish()
                }
                actionMode?.title = resources.getQuantityString(R.plurals.categories_selected_count, total, total)
            }
        })
    }

    private fun setupListeners() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear()
                menuInflater.inflate(R.menu.category, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == R.id.add_category) {
                    openCreateDialog()
                    return true
                }
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private val actionModeCallback = object : ActionMode.Callback {
        override fun onCreateActionMode(mode: ActionMode, menu: Menu): Boolean {
            mode.menuInflater.inflate(R.menu.menu_categoria_action, menu)
            adapter.setModoSelecao(true)
            return true
        }

        override fun onPrepareActionMode(mode: ActionMode, menu: Menu) = false
        override fun onActionItemClicked(mode: ActionMode, item: MenuItem): Boolean {
            if (item.itemId == R.id.action_delete) {
                confirmDeleteSelected(mode)
                return true
            }
            return false
        }

        override fun onDestroyActionMode(mode: ActionMode) {
            adapter.setModoSelecao(false)
            actionMode = null
        }
    }

    private fun openCreateDialog() {
        Utils.dialogCategory(
            requireContext(),
            requireContext().getString(R.string.dialog_add_category_title)
        ) { nome ->
            presenter.create(PostCategory(name = nome))
        }
    }

    private fun openEditDialog(category: PostCategory) {
        Utils.dialogCategory(
            requireContext(),
            requireContext().getString(R.string.edit) + "\"${category.name}\""
        ) { novoNome ->
            presenter.update(category.copy(name = novoNome))
        }
    }

    private fun confirmDeleteSelected(mode: ActionMode) {
        val selecionadas = adapter.selecionadas
        val mensagem = if (selecionadas.size == 1)
            "${
                requireContext().getString(
                    R.string.dialog_title_delete
                )
            } \"${selecionadas.first().name}\"?"
        else
            "${
                requireContext().getString(
                    R.string.dialog_title_delete
                )
            } ${selecionadas.size} ${
                requireContext().getString(
                    R.string.category
                )
            }?"

        DialogUtil.showDeleteDialog(requireContext(), mensagem) {
            presenter.deleteMultiple(selecionadas)
            mode.finish()
        }
    }

    private fun navigateToHomeFiltered(categoriaId: String, categoriaNome: String) {
        val bundle = Bundle().apply {
            putString("categoria_id", categoriaId)
            putString("categoria_nome", categoriaNome)
        }
        findNavController().navigate(R.id.nav_home, bundle)
    }

    private fun observePresenterFlows() {
        viewLifecycleOwner.lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                launch { // Observa as categorias
                    (presenter as CategoryPresenter).categories.collect { categories ->
                        if (categories != null) {
                            adapter.setItems(categories)
                            showEmpty(categories.isEmpty())
                        }
                    }
                }
            }
        }
    }

    override fun showProgress(enable: Boolean) {
        binding.progressCategory.visibility = if (enable) View.VISIBLE else View.GONE
    }

    override fun showEmpty(isEmpty: Boolean) {
        binding.recyclerViewCategory.visibility = if (isEmpty) View.GONE else View.VISIBLE
        binding.layoutEmptyCategory.visibility = if (isEmpty) View.VISIBLE else View.GONE
    }

    override fun onSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onCategoryExists(category: PostCategory) {
        Snackbar.make(
            binding.root,
            "\"${category.name}\" ${
                requireContext().getString(R.string.category_txt_exists)
            }",
            Snackbar.LENGTH_LONG
        ).show()
    }

    override fun onError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun onCategoryCreated(category: PostCategory) {
        onSuccess(
            "${
                requireContext().getString(R.string.category_2)
            } \"${category.name}\" ${
                requireContext().getString(R.string.toast_txt_created)
            }"
        )
    }

    override fun onCategoryUpdated(category: PostCategory) {
        onSuccess(
            "${
                requireContext().getString(R.string.category_txt_update)
            } \"${category.name}\""
        )
    }

    override fun onCategoryDeleted(category: PostCategory) {
        onSuccess(
            "${
                requireContext().getString(R.string.category_2)
            } \"${category.name}\" ${
                requireContext().getString(R.string.toast_txt_excluded)
            }"
        )
    }

    override fun onDestroyView() {
        actionMode?.finish()
        if (::presenter.isInitialized) presenter.onDestroy() // ← mover para cá
        super.onDestroyView()
        _binding = null
    }
}