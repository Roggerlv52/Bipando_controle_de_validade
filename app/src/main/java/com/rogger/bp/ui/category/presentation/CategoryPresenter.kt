package com.rogger.bp.ui.category.presentation

import com.rogger.bp.data.model.PostCategory
import com.rogger.bp.ui.category.ContractCategory
import com.rogger.bp.ui.category.data.CategoryCallback
import com.rogger.bp.ui.category.data.CategoryRepository
import com.rogger.bp.ui.category.data.FetchCategoriesCallback

class CategoryPresenter(
    private var view: ContractCategory.View?,
    private val repository: CategoryRepository
) : ContractCategory.Presenter {

    companion object {
        private const val NAME_MIN = 3
        private const val NAME_MAX = 40
    }

    override fun create(category: PostCategory) {
        val validatedName = validateName(category.name) ?: return

        view?.showProgress(true)
        repository.create(category.copy(name = validatedName), object : CategoryCallback {
            override fun onSuccess(cat: PostCategory) {
                view?.onSuccess("Categoria \"${cat.name}\" criada")
            }

            override fun onAlreadyExists(cat: PostCategory) {
                view?.onCategoryExists(cat)
            }

            override fun onFailure(message: String) {
                view?.onError(message)
            }

            override fun onComplete() {
                view?.showProgress(false)
                fetchCategories()
            }
        })
    }

    override fun update(category: PostCategory) {
        if (category.id == 0) {
            view?.onError("Categoria inválida para edição")
            return
        }
        val validatedName = validateName(category.name) ?: return

        view?.showProgress(true)
        repository.update(category.copy(name = validatedName), object : CategoryCallback {
            override fun onSuccess(cat: PostCategory) {
                view?.onSuccess("Categoria atualizada para \"${cat.name}\"")
            }

            override fun onAlreadyExists(cat: PostCategory) {
                view?.onCategoryExists(cat)
            }

            override fun onFailure(message: String) {
                view?.onError(message)
            }

            override fun onComplete() {
                view?.showProgress(false)
                fetchCategories()
            }
        })
    }

    override fun delete(category: PostCategory) {
        view?.showProgress(true)
        repository.delete(category, object : CategoryCallback {
            override fun onSuccess(cat: PostCategory) {
                view?.onSuccess("Categoria \"${cat.name}\" excluída")
            }

            override fun onAlreadyExists(cat: PostCategory) { /* não aplicável */
            }

            override fun onFailure(message: String) {
                view?.onError(message)
            }

            override fun onComplete() {
                view?.showProgress(false)
                fetchCategories()
            }
        })
    }

    override fun deleteMultiple(categories: List<PostCategory>) {
        if (categories.isEmpty()) {
            view?.onError("Nenhuma categoria selecionada")
            return
        }

        view?.showProgress(true)
        val total = categories.size
        var completed = 0
        var errors = 0

        categories.forEach { category ->
            repository.delete(category, object : CategoryCallback {
                override fun onSuccess(cat: PostCategory) { /* contado em onComplete */
                }

                override fun onAlreadyExists(cat: PostCategory) { /* não aplicável */
                }

                override fun onFailure(message: String) {
                    errors++
                }

                override fun onComplete() {
                    completed++
                    if (completed == total) {
                        view?.showProgress(false)
                        if (errors == 0) {
                            val msg = if (total == 1)
                                "Categoria \"${categories.first().name}\" excluída"
                            else "$total categorias excluídas"
                            view?.onSuccess(msg)
                        } else {
                            view?.onError("$errors de $total categorias falharam ao excluir")
                        }
                        fetchCategories()
                    }
                }
            })
        }
    }

    override fun fetchCategories() {
        view?.showProgress(true)
        repository.fetchAll(object : FetchCategoriesCallback {
            override fun onSuccess(categories: List<PostCategory>) {
                view?.showCategories(categories)
                view?.showEmpty(categories.isEmpty())
            }

            override fun onFailure(message: String) {
                view?.onError(message)
                view?.showEmpty(true)
            }

            override fun onComplete() {
                view?.showProgress(false)
            }
        })
    }

    override fun getCountCategorias() {
    }

    private fun validateName(name: String): String? {
        val trimmed = name.trim()
        return when {
            trimmed.isEmpty() -> {
                view?.onError("Informe um nome para a categoria"); null
            }

            trimmed.length < NAME_MIN -> {
                view?.onError("Nome deve ter pelo menos $NAME_MIN caracteres"); null
            }

            trimmed.length > NAME_MAX -> {
                view?.onError("Nome deve ter no máximo $NAME_MAX caracteres"); null
            }

            else -> trimmed
        }
    }

    override fun onDestroy() {
        view = null
    }
}