package com.rogger.bp.ui.deleteitem.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import android.content.Context
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.data.repository.toData
import com.rogger.bp.domain.model.Product
import com.rogger.bp.ui.commun.SharedPreferencesManager
import com.rogger.bp.ui.deleteitem.data.DeleteItemCallback
import com.rogger.bp.ui.deleteitem.data.DeleteItemRepository
import com.rogger.bp.domain.usecase.GetDeletedProductsUseCase
import com.rogger.bp.ui.groups.data.GroupRepository
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

data class TrashState(
    val items: List<Product> = emptyList(),
    val isLoading: Boolean = false,
    val userRole: String = "Admin",
    val errorMessage: String? = null
)

class TrashViewModel(
    private val getDeletedProductsUseCase: GetDeletedProductsUseCase,
    private val deleteItemRepository: DeleteItemRepository,
    private val groupRepository: GroupRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(TrashState())
    val uiState: StateFlow<TrashState> = _uiState.asStateFlow()

    init {
        loadTrash()
        observeUserRole()
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
    
    fun checkAndCleanOldItems(context: Context) {
        val isPremium = SharedPreferencesManager.isPremium(context)
        if (isPremium) return

        val thirtyDaysAgo = System.currentTimeMillis() - TimeUnit.DAYS.toMillis(30)
        
        _uiState.value.items.forEach { product ->
            val deletedAt = product.deletedAt ?: 0L
            if (deletedAt > 0 && deletedAt < thirtyDaysAgo) {
                deletePermanently(product)
            }
        }
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
        if (_uiState.value.userRole != "Admin" && _uiState.value.userRole != "Editor") {
            _uiState.update { it.copy(errorMessage = "Apenas Administradores ou Editores podem restaurar produtos.") }
            return
        }

        deleteItemRepository.restore(product.toData(), object : DeleteItemCallback {
            override fun onSuccess(items: List<PostProduct>?) {}
            override fun onFailure(message: String) {
                _uiState.update { it.copy(errorMessage = message) }
            }
            override fun onComplete() {}
        })
    }

    fun deletePermanently(product: Product) {
        if (_uiState.value.userRole != "Admin" && _uiState.value.userRole != "Editor") {
            _uiState.update { it.copy(errorMessage = "Apenas Administradores ou Editores podem excluir permanentemente.") }
            return
        }

        _uiState.update { it.copy(errorMessage = null) }
        deleteItemRepository.deletePermanently(product.toData(), object : DeleteItemCallback {
            override fun onSuccess(items: List<PostProduct>?) {}
            override fun onFailure(message: String) {
                _uiState.update { it.copy(errorMessage = message) }
            }
            override fun onComplete() {}
        })
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }
}
