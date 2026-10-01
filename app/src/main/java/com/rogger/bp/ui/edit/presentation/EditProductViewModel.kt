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
import com.rogger.bp.ui.groups.data.GroupRepository
import com.rogger.bp.ui.category.data.CategoryRepository
import com.rogger.bp.ui.category.data.FetchCategoriesCallback
import com.rogger.bp.data.model.PostCategory
import com.rogger.bp.ui.commun.AnalyticsManager
import com.rogger.bp.ui.commun.SharedPreferencesManager
import android.content.Context
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
    val userRole: String = "Admin",
    val errorMessage: String? = null
)

class EditProductViewModel(
    private val context: Context,
    private val getProductByUuidUseCase: GetProductByUuidUseCase,
    private val getCategoriesUseCase: GetCategoriesUseCase,
    private val saveProductUseCase: SaveProductUseCase,
    private val deleteProductUseCase: DeleteProductUseCase,
    private val groupRepository: GroupRepository,
    private val categoryRepository: CategoryRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(EditProductState())
    val uiState: StateFlow<EditProductState> = _uiState.asStateFlow()

    init {
        observeUserRole()
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

    private fun observeUserRole() {
        viewModelScope.launch {
            groupRepository.getLocalGroupFlow().collect { group ->
                if (group != null) {
                    val members = groupRepository.fetchMembers(group.groupId).getOrNull()
                    val currentUser = com.google.firebase.auth.FirebaseAuth.getInstance().currentUser
                    val role = members?.find { it.userId == currentUser?.uid }?.role ?: "Admin"
                    _uiState.update { it.copy(userRole = role) }
                } else {
                    _uiState.update { it.copy(userRole = "Admin") }
                }
            }
        }
    }

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
                AnalyticsManager.logProductEdited()
                _uiState.update { it.copy(isLoading = false, isSaved = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun deleteProduct() {
        val state = _uiState.value
        // ✅ Correção: Apenas Admins e Editores podem apagar.
        if (state.userRole != "Admin" && state.userRole != "Editor") {
            _uiState.update { it.copy(errorMessage = "Apenas Administradores ou Editores podem mover produtos para a lixeira.") }
            return
        }
        
        val product = state.product
        if (product == null) {
            _uiState.update { it.copy(errorMessage = "Erro: Produto não carregado") }
            return
        }

        _uiState.update { it.copy(isLoading = true, errorMessage = null) }
        
        viewModelScope.launch {
            try {
                deleteProductUseCase(product)
                AnalyticsManager.logProductDeleted()
                _uiState.update { it.copy(isLoading = false, isDeleted = true) }
            } catch (e: Exception) {
                _uiState.update { it.copy(isLoading = false, errorMessage = e.message) }
            }
        }
    }

    fun clearError() = _uiState.update { it.copy(errorMessage = null) }
}
