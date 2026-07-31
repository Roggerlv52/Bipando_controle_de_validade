package com.rogger.bp.ui.edit.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rogger.bp.domain.model.Category
import com.rogger.bp.domain.model.Product
import com.rogger.bp.domain.usecase.GetCategoriesUseCase
import com.rogger.bp.domain.usecase.GetProductByUuidUseCase
import com.rogger.bp.domain.usecase.SaveProductUseCase
import com.rogger.bp.domain.usecase.DeleteProductUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

data class EditProductState(
    val product: Product? = null,
    val name: String = "",
    val barcode: String = "",
    val category: Category? = null,
    val expirationDate: Long = System.currentTimeMillis(),
    val note: String = "",
    val imageUri: String = "",
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val isDeleted: Boolean = false,
    val errorMessage: String? = null
)

class EditProductViewModel(
    private val getProductByUuidUseCase: GetProductByUuidUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val saveProductUseCase: SaveProductUseCase,
    private val deleteProductUseCase: DeleteProductUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProductState())
    val uiState: StateFlow<EditProductState> = _uiState.asStateFlow()

    fun loadProduct(uuid: String) {
        _uiState.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            // Load Categories first
            val categories = getCategoriesUseCase().first()
            _uiState.update { it.copy(categories = categories) }

            // Load Product
            getProductByUuidUseCase(uuid).collect { product ->
                if (product != null) {
                    _uiState.update { state ->
                        state.copy(
                            product = product,
                            name = product.name,
                            barcode = product.barcode,
                            category = categories.find { it.id == product.categoryId },
                            expirationDate = product.timestamp,
                            note = product.note,
                            imageUri = product.imageUri,
                            isLoading = false
                        )
                    }
                } else {
                    _uiState.update { it.copy(isLoading = false, errorMessage = "Produto não encontrado") }
                }
            }
        }
    }

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name) }
    fun onBarcodeChange(barcode: String) = _uiState.update { it.copy(barcode = barcode) }
    fun onCategoryChange(category: Category) = _uiState.update { it.copy(category = category) }
    fun onDateChange(date: Long) = _uiState.update { it.copy(expirationDate = date) }
    fun onNoteChange(note: String) = _uiState.update { it.copy(note = note) }
    fun onImageChange(uri: String) = _uiState.update { it.copy(imageUri = uri) }

    fun saveProduct() {
        val state = _uiState.value
        val originalProduct = state.product ?: return

        if (state.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Nome do produto é obrigatório") }
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            try {
                val updatedProduct = originalProduct.copy(
                    name = state.name,
                    barcode = state.barcode,
                    categoryId = state.category?.id ?: originalProduct.categoryId,
                    categoryName = state.category?.name ?: originalProduct.categoryName,
                    imageUri = state.imageUri,
                    timestamp = state.expirationDate,
                    note = state.note
                )
                saveProductUseCase(updatedProduct)
                _uiState.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun deleteProduct() {
        val product = _uiState.value.product
        if (product == null) {
            _uiState.update { it.copy(errorMessage = "Erro: Produto não carregado") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        
        viewModelScope.launch {
            try {
                deleteProductUseCase(product)
                _uiState.update { it.copy(isLoading = false, isDeleted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
}
