package com.rogger.bp.ui.category.presentation

import com.rogger.bp.data.model.PostCategory
import com.rogger.bp.ui.category.ContractCategory
import com.rogger.bp.ui.category.data.CategoryCallback
import com.rogger.bp.ui.category.data.CategoryRepository
import com.rogger.bp.ui.category.data.FetchCategoriesCallback
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

class CategoryPresenter(
    private var view: ContractCategory.View?,
    private val repository: CategoryRepository
) : ContractCategory.Presenter {

    private val _categories = MutableStateFlow<List<PostCategory>>(emptyList())
    val categories: StateFlow<List<PostCategory>> = _categories.asStateFlow()

    private val presenterScope = CoroutineScope(Dispatchers.Main + Job())
    private var categoriesCollectionJob: Job? = null

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
                // Não precisa chamar fetchCategories() aqui, o listener do Firestore e o Flow do Room já farão a atualização
            }
        })
    }

    override fun update(category: PostCategory) {
        if (category.userId.isBlank()) {
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
                // Não precisa chamar fetchCategories() aqui, o listener do Firestore e o Flow do Room já farão a atualização
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
                // Não precisa chamar fetchCategories() aqui, o listener do Firestore e o Flow do Room já farão a atualização
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
                        // Não precisa chamar fetchCategories() aqui, o listener do Firestore e o Flow do Room já farão a atualização
                    }
                }
            })
        }
    }

    override fun fetchCategories() {
        view?.showProgress(true)

        // Inicia o listener do Firestore e atualiza o cache local
        repository.fetchAll(object : FetchCategoriesCallback {
            override fun onSuccess(categories: List<PostCategory>) {
                view?.showProgress(false)
            }

            override fun onFailure(message: String) {
                view?.onError(message)
                view?.showProgress(false)
            }

            override fun onComplete() {
                // onComplete não é chamado para listeners contínuos
            }
        })

        // Coleta as categorias do cache local e as expõe para a View
        categoriesCollectionJob?.cancel() // Cancela o job anterior se houver
        categoriesCollectionJob = presenterScope.launch {
            repository.getCachedCategoriesFlow().collectLatest {
                _categories.value = it
                view?.showEmpty(it.isEmpty()) // Atualiza o estado de vazio
                view?.showProgress(false) // Garante que o progresso seja ocultado após o primeiro carregamento do cache
            }
        }
    }

    override fun getCountCategorias() {
        // Este método pode ser removido ou adaptado para usar o Flow de categorias
        // Por exemplo, _categories.value.size
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
        repository.stopListeningForCategories()
        categoriesCollectionJob?.cancel()
        presenterScope.cancel()
        view = null
    }
}