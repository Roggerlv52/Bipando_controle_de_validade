package com.rogger.bp.ui.deleteitem.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.data.repository.toData
import com.rogger.bp.domain.model.Product
import com.rogger.bp.ui.deleteitem.data.DeleteItemCallback
import com.rogger.bp.ui.deleteitem.data.DeleteItemRepository
import com.rogger.bp.domain.usecase.GetDeletedProductsUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class TrashState(
    val items: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val errorMessage: String? = null
)

class TrashViewModel(
    private val getDeletedProductsUseCase: GetDeletedProductsUseCase,
    private val deleteItemRepository: DeleteItemRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrashState())
    val uiState: StateFlow<TrashState> = _uiState.asStateFlow()

    init {
        loadTrash()
    }

    private fun loadTrash() {
        _uiState.update { it.copy(isLoading = true) }
        
        // 1. Sincroniza do Firebase (opcional, mas bom para consistência)
        deleteItemRepository.fetchItemDeleted(object : DeleteItemCallback {
            override fun onSuccess(items: List<PostProduct>?) {
                // O repository/datasource poderia atualizar o cache aqui se necessário
            }
            override fun onFailure(message: String) {
                _uiState.update { it.copy(errorMessage = message) }
            }
            override fun onComplete() {
                _uiState.update { it.copy(isLoading = false) }
            }
        })

        // 2. Observa o cache local
        viewModelScope.launch {
            getDeletedProductsUseCase()
                .catch { error ->
                    _uiState.update { it.copy(isLoading = false, errorMessage = error.message) }
                }
                .collect { list ->
                    _uiState.update { it.copy(items = list) }
                }
        }
    }

    fun restoreItem(product: Product) {
        deleteItemRepository.restore(product.toData(), object : DeleteItemCallback {
            override fun onSuccess(items: List<PostProduct>?) {}
            override fun onFailure(message: String) {
                _uiState.update { it.copy(errorMessage = message) }
            }
            override fun onComplete() {}
        })
    }

    fun deletePermanently(product: Product) {
        deleteItemRepository.deletePermanently(product.toData(), object : DeleteItemCallback {
            override fun onSuccess(items: List<PostProduct>?) {}
            override fun onFailure(message: String) {
                _uiState.update { it.copy(errorMessage = message) }
            }
            override fun onComplete() {}
        })
    }
}
