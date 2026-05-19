package com.rogger.bp.ui.category

import androidx.lifecycle.LiveData
import com.rogger.bp.data.model.PostCategory
import com.rogger.bp.ui.base.BasePresenter
import com.rogger.bp.ui.base.BaseView


/**
 * Contrato MVP do fluxo de categorias.
 * ── Presenter ────────────────────────────────────────────────────────────────
 *  [create]          → valida nome e cria; notifica se nome já existe.
 *  [update]          → valida e atualiza nome de categoria existente.
 *  [delete]          → remove uma categoria.
 *  [deleteMultiple]  → remove lista selecionada (ActionMode).
 *  [fetchCategories] → carrega lista do Firestore e entrega à View.
 *
 * ── View ─────────────────────────────────────────────────────────────────────
 *  [showProgress]     → exibe/oculta ProgressBar.
 *  [showCategories]   → popula RecyclerView com a lista recebida.
 *  [showEmpty]        → alterna entre lista e estado vazio.
 *  [onSuccess]        → feedback positivo com mensagem contextual.
 *  [onCategoryExists] → avisa que o nome já existe.
 *  [onError]          → feedback de erro.
 */
interface ContractCategory {
    interface Presenter : BasePresenter {
        fun create(category: PostCategory)
        fun update(category: PostCategory)
        fun delete(category: PostCategory)
        fun deleteMultiple(categories: List<PostCategory>)
        fun fetchCategories()
        fun getCountCategorias()
    }

    interface View : BaseView<Presenter> {
        fun showProgress(enable: Boolean)
        // fun showCategories(categories: List<PostCategory>) // Removido, a View observará o Flow diretamente
        fun showEmpty(isEmpty: Boolean)
        fun onSuccess(message: String)
        fun onError(message: String)
        fun onCategoryExists(category: PostCategory)
        fun onCategoryCreated(category: PostCategory)
        fun onCategoryUpdated(category: PostCategory)
        fun onCategoryDeleted(category: PostCategory)
    }
}