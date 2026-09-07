package com.rogger.bp.ui.add.presentation

import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.rogger.bp.data.model.PostImage
import com.rogger.bp.ui.add.data.RegisterItemRepository
import com.rogger.bp.ui.add.data.SaveImageCallback
import com.rogger.bp.domain.model.Category
import com.rogger.bp.domain.model.Product
import com.rogger.bp.domain.usecase.GetCategoriesUseCase
import com.rogger.bp.domain.usecase.SaveProductUseCase
import com.rogger.bp.data.remote.OpenFoodFactsRepository
import com.rogger.bp.ui.commun.AnalyticsManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.UUID

import com.rogger.bp.ui.category.data.CategoryRepository
import com.rogger.bp.ui.category.data.FetchCategoriesCallback
import com.rogger.bp.data.model.PostCategory
import com.rogger.bp.ui.commun.SharedPreferencesManager
import android.content.Context
import android.util.Log

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
    private val context: Context,
    private val saveProductUseCase: SaveProductUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val registerRepository: RegisterItemRepository,
    private val categoryRepository: CategoryRepository,
    private val openFoodFactsRepository: OpenFoodFactsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AddProductState())
    val uiState: StateFlow<AddProductState> = _uiState.asStateFlow()

    init {
        loadCategories()
        syncCategories()
    }

    private fun syncCategories() {
        val workMode = SharedPreferencesManager.getWorkMode(context)
        categoryRepository.fetchAll(object : FetchCategoriesCallback {
            override fun onSuccess(categories: List<PostCategory>) {}
            override fun onFailure(message: String) {}
            override fun onComplete() {}
        }, workMode = workMode)
    }

    private fun loadCategories() {
        viewModelScope.launch {
            getCategoriesUseCase().collect { list ->
                _uiState.update { it.copy(categories = list) }
            }
        }
    }

    fun onNameChange(name: String) = _uiState.update { it.copy(name = name) }
    
    fun onBarcodeChange(barcode: String) {
        val trimmedBarcode = barcode.trim()
        _uiState.update { it.copy(barcode = trimmedBarcode) }
        if (trimmedBarcode.isNotEmpty()) {
            checkIfBarcodeExists(trimmedBarcode)
        }
    }

    private fun checkIfBarcodeExists(barcode: String) {
        Log.d("Add",barcode)
        viewModelScope.launch {
            // 1. Tenta buscar no cache local primeiro
            val localProduct = registerRepository.getLocalProductByBarcode(barcode)
            
            // Verifica se ainda estamos interessados neste barcode
            if (_uiState.value.barcode != barcode) return@launch

            if (localProduct != null) {
                _uiState.update { state ->
                    state.copy(
                        name = if (state.name.isBlank()) localProduct.name else state.name,
                        imageUri = if (state.imageUri == null && localProduct.imageUri.isNotEmpty()) Uri.parse(localProduct.imageUri) else state.imageUri,
                        category = if (state.category == null) state.categories.find { it.id == localProduct.categoryId } else state.category
                    )
                }
            }

            // 2. Busca no Firebase (Coleção global imageProdutos)
            val imageToResolve = PostImage(barcode = barcode)
            registerRepository.createImage(imageToResolve, object : SaveImageCallback {
                override fun onSuccess(image: PostImage) {}

                override fun onAlreadyExists(image: PostImage) {
                    _uiState.update { state ->
                        // CRÍTICO: Verifica se o barcode no estado ainda é o mesmo que iniciou esta busca
                        if (state.barcode != barcode) return@update state

                        state.copy(
                            // Preenche o nome se estiver vazio
                            name = if (state.name.isBlank()) image.name else state.name,
                            // Preenche a imagem se estiver vazia
                            imageUri = if (state.imageUri == null && image.uri.isNotEmpty()) Uri.parse(image.uri) else state.imageUri
                        )
                    }
                }

                override fun onFailure(message: String) {
                    android.util.Log.e("AddProductViewModel", "Erro ao resolver barcode $barcode remotamente: $message")
                    // 3. Se não existir no Firebase -> consulta Open Food Facts
                    fetchFromOpenFoodFacts(barcode)
                }

                override fun onComplete() {}
            })
        }
    }

    private fun fetchFromOpenFoodFacts(barcode: String) {
        viewModelScope.launch {
            val product = openFoodFactsRepository.fetchProduct(barcode)
            
            // Verifica se ainda estamos interessados neste barcode
            if (_uiState.value.barcode != barcode) return@launch

            if (product != null) {
                _uiState.update { state ->
                    state.copy(
                        // Preenche o nome se estiver vazio
                        name = if (state.name.isBlank()) product.name else state.name,
                        // Preenche a imagem se estiver vazia
                        imageUri = if (state.imageUri == null && product.uri.isNotEmpty()) Uri.parse(product.uri) else state.imageUri
                    )
                }
            }
        }
    }

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
                AnalyticsManager.logProductAdded(if (state.barcode.isNotEmpty()) "barcode" else "manual")
                _uiState.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }
}
