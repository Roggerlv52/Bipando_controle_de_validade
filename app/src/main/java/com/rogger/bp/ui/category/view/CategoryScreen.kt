package com.rogger.bp.ui.category.view

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Category
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.rogger.bp.R
import com.rogger.bp.domain.model.Category
import com.rogger.bp.ui.category.presentation.CategoryViewModel
import androidx.annotation.StringRes
import com.rogger.bp.ui.category.view.componentes.CategoryImageSelectionDialog
import com.rogger.bp.ui.category.view.componentes.CategoryItem
import com.rogger.bp.ui.category.view.componentes.EmptyCategoriesState
import com.rogger.bp.ui.commun.SharedPreferencesManager
import com.rogger.bp.ui.componentes.BipandoTextField
import com.rogger.bp.ui.componentes.SwipeToDeleteContainer

data class CategoryImageOption(
    @param:StringRes val nameRes: Int,
    val resId: Int
)

val categoryImageOptions = listOf(
    CategoryImageOption(R.string.cat_img_carne, R.drawable.carne),
    CategoryImageOption(R.string.cat_img_congelado, R.drawable.congelado),
    CategoryImageOption(R.string.cat_img_embutidos, R.drawable.embotidos),
    CategoryImageOption(R.string.cat_img_frutos_mar, R.drawable.frutos_mar),
    CategoryImageOption(R.string.cat_img_laticinios, R.drawable.laticineos),
    CategoryImageOption(R.string.cat_img_leite, R.drawable.leite),
    CategoryImageOption(R.string.cat_img_mercearia, R.drawable.mercearia),
    CategoryImageOption(R.string.cat_img_hortifruti, R.drawable.ortifrute),
    CategoryImageOption(R.string.cat_img_padaria, R.drawable.padaria),
    CategoryImageOption(R.string.cat_img_queijo, R.drawable.queijo),
    CategoryImageOption(R.string.cat_img_salames, R.drawable.salames),
    CategoryImageOption(R.string.cat_img_sorvete, R.drawable.sorvete),
    CategoryImageOption(R.string.cat_img_suco, R.drawable.suco)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CategoryScreen(
    viewModel: CategoryViewModel,
    onBackClick: () -> Unit,
    onCategoryClick: (Category) -> Unit,
) {
    val context = LocalContext.current
    val state by viewModel.uiState.collectAsState()
    var showAddDialog by remember { mutableStateOf(false) }
    var newCategoryName by remember { mutableStateOf("") }
    var showEditDialog by remember { mutableStateOf(false) }
    var categoryToEdit by remember { mutableStateOf<Category?>(null) }
    var editCategoryName by remember { mutableStateOf("") }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var categoryToDelete by remember { mutableStateOf<Category?>(null) }

    // Estado para o Dialog de Seleção de Imagem da Categoria
    var categoryForIconPicker by remember { mutableStateOf<Category?>(null) }
    var iconRefreshTrigger by remember { mutableIntStateOf(0) }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text(stringResource(R.string.category_dialog_new_title)) },
            text = {
                BipandoTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = stringResource(R.string.category_name_field_label),
                    leadingIcon = Icons.Default.Category
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.saveCategory(newCategoryName)
                        newCategoryName = ""
                        showAddDialog = false
                    }
                ) {
                    Text(stringResource(R.string.save_changes_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddDialog = false }) {
                    Text(stringResource(R.string.dialog_button_cancel))
                }
            }
        )
    }

    if (showEditDialog && categoryToEdit != null) {
        AlertDialog(
            onDismissRequest = { showEditDialog = false },
            title = { Text(stringResource(R.string.category_dialog_edit_title)) },
            text = {
                BipandoTextField(
                    value = editCategoryName,
                    onValueChange = { editCategoryName = it },
                    label = stringResource(R.string.category_name_field_label),
                    leadingIcon = Icons.Default.Category
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        categoryToEdit?.let {
                            viewModel.updateCategory(it, editCategoryName)
                        }
                        showEditDialog = false
                    }
                ) {
                    Text(stringResource(R.string.save_changes_button))
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditDialog = false }) {
                    Text(stringResource(R.string.dialog_button_cancel))
                }
            }
        )
    }

    if (showDeleteDialog && categoryToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text(stringResource(R.string.category_dialog_delete_title)) },
            text = { Text(stringResource(R.string.category_dialog_delete_confirm, categoryToDelete?.name ?: "")) },
            confirmButton = {
                Button(
                    onClick = {
                        categoryToDelete?.let { viewModel.deleteCategory(it) }
                        showDeleteDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    Text(stringResource(R.string.category_btn_delete))
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text(stringResource(R.string.dialog_button_cancel))
                }
            }
        )
    }

    if (categoryForIconPicker != null) {
        CategoryImageSelectionDialog(
            onDismiss = { categoryForIconPicker = null },
            onImageSelected = { selectedResId ->
                categoryForIconPicker?.let { cat ->
                    SharedPreferencesManager.setCategoryIcon(context, cat.id, selectedResId)
                    iconRefreshTrigger++
                }
                categoryForIconPicker = null
            }
        )
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        contentWindowInsets = WindowInsets(0, 0, 0, 0),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.category), fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = stringResource(R.string.cd_back))
                    }
                },
                actions = {
                    IconButton(onClick = { showAddDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = stringResource(R.string.category_dialog_new_title),
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = padding.calculateTopPadding())
        ) {
            AsyncImage(
                model = R.drawable.fundo_vector,
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop,
            )

            // Overlay para garantir legibilidade das categorias
            Box(modifier = Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.3f)))

            if (state.categories.isEmpty() && !state.isLoading) {
                EmptyCategoriesState()
            } else {
                val navBarBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        top = 16.dp,
                        end = 16.dp,
                        bottom = 16.dp + navBarBottomPadding
                    ),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(
                        items = state.categories,
                        key = { it.id }
                    ) { category ->
                        SwipeToDeleteContainer(
                            item = category,
                            onDelete = {
                                categoryToDelete = it
                                showDeleteDialog = true
                            },
                            onEdit = {
                                categoryToEdit = it
                                editCategoryName = it.name
                                showEditDialog = true
                            }
                        ) {
                            CategoryItem(
                                category = category,
                                refreshTrigger = iconRefreshTrigger,
                                onClick = { onCategoryClick(category) },
                                onIconClick = { categoryForIconPicker = category }
                            )
                        }
                    }
                }
            }

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}
