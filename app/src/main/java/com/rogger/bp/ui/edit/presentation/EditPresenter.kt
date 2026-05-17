package com.rogger.bp.ui.edit.presentation

import com.rogger.bp.data.model.PostCategory
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.ui.category.data.CategoryRepository
import com.rogger.bp.ui.category.data.FetchCategoriesCallback
import com.rogger.bp.ui.edit.ContractEdit
import com.rogger.bp.ui.edit.data.EditCallback
import com.rogger.bp.ui.edit.data.EditRepository

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 14/05/2026
 * Hora: 20:02
 */
class EditPresenter(
    private var view: ContractEdit.View?,
    private val repository: EditRepository,
    private val categoryRepository: CategoryRepository
) : ContractEdit.Presenter {

    companion object {
        private const val NOME_MIN = 2
        private const val NOME_MAX = 80
    }

    // ── 1. Carregar produto ───────────────────────────────────────────────

    override fun loadProduct(productUuid: String) {
        if (productUuid.isBlank()) {
            view?.onError("Identificador de produto inválido")
            return
        }

        view?.showProgress(true)

        // ✅ Alteração: Chamando o repositório com o UUID
        repository.fetchByUuid(productUuid, object : EditCallback {
            override fun onSuccess(produto: PostProduct) {
                view?.bindProduct(produto)
            }
            override fun onFailure(message: String) {
                view?.onError(message)
            }
            override fun onComplete() {
                view?.showProgress(false)
            }
        })
    }

    // ── 2. Guardar produto ────────────────────────────────────────────────

    override fun saveProduct(produto: PostProduct) {
        val nome = produto.name.trim()
        when {
            nome.isEmpty()         -> { view?.onError("Informe o nome do produto"); return }
            nome.length < NOME_MIN -> { view?.onError("Nome deve ter pelo menos $NOME_MIN caracteres"); return }
            nome.length > NOME_MAX -> { view?.onError("Nome deve ter no máximo $NOME_MAX caracteres"); return }
        }

        if (produto.timestamp <= 0L) {
            view?.onError("Selecione uma data de validade")
            return
        }

        view?.showProgress(true)

        repository.update(produto, object : EditCallback {
            override fun onSuccess(p: PostProduct) {
                view?.onSuccess("\"${p.name}\" actualizado com sucesso")
                view?.navigateBack()
            }
            override fun onFailure(message: String) {
                view?.onError(message)
            }
            override fun onComplete() {
                view?.showProgress(false)
            }
        })
    }

    // ── 3. Soft-delete ────────────────────────────────────────────────────

    override fun deleteProduct(produto: PostProduct) {
        view?.showProgress(true)

        repository.delete(produto, object : EditCallback {
            override fun onSuccess(p: PostProduct) {
                view?.onSuccess("\"${p.name}\" movido para a lixeira")
                view?.navigateBack()
            }
            override fun onFailure(message: String) {
                view?.onError(message)
            }
            override fun onComplete() {
                view?.showProgress(false)
            }
        })
    }

    // ── 4. Buscar categorias para o Spinner ───────────────────────────────

    override fun fetchCategories() {
        categoryRepository.fetchAll(object : FetchCategoriesCallback {
            override fun onSuccess(categories: List<PostCategory>) {
                view?.showCategories(categories)
            }
            override fun onFailure(message: String) {
                // Entrega lista vazia — Spinner mostrará só o placeholder
                view?.showCategories(emptyList())
            }
            override fun onComplete() { /* silencioso */ }
        })
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    override fun onDestroy() {
        view = null
    }
}