package com.rogger.bp.ui.edit.presentation

import com.rogger.bp.data.model.PostProduct
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
    private val repository: EditRepository
) : ContractEdit.Presenter {

    companion object {
        private const val NOME_MIN = 2
        private const val NOME_MAX = 80
    }

    // ── 1. Carregar produto ───────────────────────────────────────────────

    override fun loadProduct(productId: Int) {
        if (productId <= 0) {
            view?.onError("ID de produto inválido")
            return
        }

        view?.showProgress(true)

        repository.fetch(productId, object : EditCallback {

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

    // ── 2. Guardar produto (update) ───────────────────────────────────────

    override fun saveProduct(produto: PostProduct) {

        // Validação do nome
        val nome = produto.name?.trim() ?: ""
        when {
            nome.isEmpty()       -> { view?.onError("Informe o nome do produto"); return }
            nome.length < NOME_MIN -> { view?.onError("Nome deve ter pelo menos $NOME_MIN caracteres"); return }
            nome.length > NOME_MAX -> { view?.onError("Nome deve ter no máximo $NOME_MAX caracteres"); return }
        }

        // Validação da data
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

    // ── 3. Eliminar produto (soft-delete) ─────────────────────────────────

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

    // ── Lifecycle ─────────────────────────────────────────────────────────

    override fun onDestroy() {
        view = null
    }
}