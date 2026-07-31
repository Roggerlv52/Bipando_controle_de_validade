package com.rogger.bp.ui.add.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rogger.bp.domain.model.Category
import com.rogger.bp.domain.model.Product
import com.rogger.bp.domain.usecase.GetCategoriesUseCase
import com.rogger.bp.domain.usecase.SaveProductUseCase
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

data class AddProductState(
    val name: String = "",
    val barcode: String = "",
    val category: Category? = null,
    val expirationDate: Long = System.currentTimeMillis(),
    val note: String = "",
    val imageUri: Uri? = null,
    val categories: List<Category> = emptyList(),
    val isLoading: Boolean = false,
    val isSaved: Boolean = false,
    val errorMessage: String? = null
)

class AddProductViewModel(
    private val saveProductUseCase: SaveProductUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddProductState())
    val uiState: StateFlow<AddProductState> = _uiState.asStateFlow()

    init {
        loadCategories()
    }

    private fun loadCategories() {
        viewModelScope.launch {
            getCategoriesUseCase().collect { list ->
                _uiState.update { it.copy(categories = list) }
            }
        }
    }

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name) }
    fun onBarcodeChange(barcode: String) = _uiState.update { it.copy(barcode = barcode) }
    fun onCategoryChange(category: Category) = _uiState.update { it.copy(category = category) }
    fun onDateChange(date: Long) = _uiState.update { it.copy(expirationDate = date) }
    fun onNoteChange(note: String) = _uiState.update { it.copy(note = note) }
    fun onImageChange(uri: Uri?) = _uiState.update { it.copy(imageUri = uri) }

    fun saveProduct() {
        val state = _uiState.value
        if (state.name.isBlank()) {
            _uiState.update { it.copy(errorMessage = "Nome do produto é obrigatório") }
            return
        }
        if (state.category == null) {
            _uiState.update { it.copy(errorMessage = "Selecione uma categoria") }
            return
        }

        _uiState.update { it.copy(isLoading = true) }
        
        viewModelScope.launch {
            try {
                val product = Product(
                    uuid = UUID.randomUUID().toString(),
                    name = state.name,
                    barcode = state.barcode,
                    categoryId = state.category.id,
                    categoryName = state.category.name,
                    imageUri = state.imageUri?.toString() ?: "",
                    timestamp = state.expirationDate,
                    note = state.note,
                    deleted = false
                )
                saveProductUseCase(product)
                _uiState.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }
}
