package com.rogger.bp.ui.category.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rogger.bp.data.model.PostCategory
import com.rogger.bp.domain.model.Category
import com.rogger.bp.ui.category.data.CategoryCallback
import com.rogger.bp.ui.category.data.CategoryRepository
import com.rogger.bp.ui.category.data.FetchCategoriesCallback
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class CategoryState(
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null
)

class CategoryViewModel(
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(CategoryState())
    val uiState: StateFlow<CategoryState> = _uiState.asStateFlow()

    init {
        syncAndFetchCategories()
    }

    private fun syncAndFetchCategories() {
        // Só mostra loading se a lista estiver vazia E ainda não estiver sincronizando
        if (_uiState.value.categories.isEmpty() && !categoryRepository.isSyncing()) {
            _uiState.update { it.copy(isLoading = true) }
        }
        
        // 1. Inicia sincronização do Firebase para o Room
        categoryRepository.fetchAll(object : FetchCategoriesCallback {
            override fun onSuccess(categories: List<PostCategory>) {
                // Repository atualiza o Room, que observamos abaixo
            }

            override fun onFailure(message: String) {
                _uiState.update { it.copy(isLoading = false, errorMessage = message) }
            }

            override fun onComplete() {
                _uiState.update { it.copy(isLoading = false) }
            }
        })

        // 2. Observa o Room para ter os dados atualizados (com contagens se necessário)
        viewModelScope.launch {
            categoryRepository.getCachedCategoriesWithCountsFlow()
                .map { list -> 
                    list.map { Category(id = it.firestoreId, name = it.name, itemCount = it.itemCount) }
                }
                .collect { categoryList ->
                    _uiState.update { it.copy(categories = categoryList) }
                }
        }
    }

    fun saveCategory(name: String) {
        if (name.isBlank()) return

        val postCategory = PostCategory(name = name)

        _uiState.update { it.copy(isLoading = true) }
        categoryRepository.create(postCategory, object : CategoryCallback {
            override fun onSuccess(category: PostCategory) {
                _uiState.update { it.copy(isLoading = false, successMessage = "Categoria salva!") }
            }
            override fun onAlreadyExists(category: PostCategory) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Categoria já existe") }
            }
            override fun onFailure(message: String) {
                _uiState.update { it.copy(isLoading = false, errorMessage = message) }
            }
            override fun onComplete() {
                _uiState.update { it.copy(isLoading = false) }
            }
        })
    }

    fun deleteCategory(category: Category) {
        val postCategory = PostCategory(firestoreId = category.id, name = category.name)
        categoryRepository.delete(postCategory, object : CategoryCallback {
            override fun onSuccess(category: PostCategory) {}
            override fun onAlreadyExists(category: PostCategory) {}
            override fun onFailure(message: String) {
                _uiState.update { it.copy(errorMessage = message) }
            }
            override fun onComplete() {}
        })
    }

    fun updateCategory(category: Category, newName: String) {
        if (newName.isBlank()) return
        
        val postCategory = PostCategory(
            firestoreId = category.id,
            name = newName
        )
        
        _uiState.update { it.copy(isLoading = true) }
        categoryRepository.update(postCategory, object : CategoryCallback {
            override fun onSuccess(category: PostCategory) {
                _uiState.update { it.copy(isLoading = false, successMessage = "Categoria atualizada!") }
            }
            override fun onAlreadyExists(category: PostCategory) {
                _uiState.update { it.copy(isLoading = false, errorMessage = "Nome de categoria já existe") }
            }
            override fun onFailure(message: String) {
                _uiState.update { it.copy(isLoading = false, errorMessage = message) }
            }
            override fun onComplete() {
                _uiState.update { it.copy(isLoading = false) }
            }
        })
    }
    
    private fun CategoryState.updateLoadingState(): CategoryState {
        // Como o onComplete do create pode ser chamado após onSuccess ou onFailure,
        // garantimos que o loading pare.
        return copy(isLoading = false)
    }

    override fun onCleared() {
        super.onCleared()
        categoryRepository.stopListeningForCategories()
    }
}
