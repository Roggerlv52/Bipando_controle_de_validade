package com.rogger.bp.ui.edit.view

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Notes
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.rogger.bp.R
import com.rogger.bp.ui.edit.presentation.EditProductViewModel
import com.rogger.bp.ui.componentes.BipandoButton
import com.rogger.bp.ui.componentes.BipandoTextField
import com.rogger.bp.util.TimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditProductScreen(
    viewModel: EditProductViewModel,
    productUuid: String,
    onBackClick: () -> Unit,
    onDeleteSuccess: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val snackbarHostState = remember { SnackbarHostState() }

    var showDatePicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }

    val pickMedia = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.PickVisualMedia()
    ) { uri ->
        uri?.let { viewModel.onImageChange(it.toString()) }
    }

    LaunchedEffect(productUuid) {
        viewModel.loadProduct(productUuid)
    }

    LaunchedEffect(state.isSaved, state.isDeleted) {
        if (state.isSaved || state.isDeleted) {
            if (state.isDeleted) onDeleteSuccess() else onBackClick()
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearError()
        }
    }

    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Excluir Produto") },
            text = { Text("Deseja realmente excluir \"${state.name}\"?") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.deleteProduct()
                        showDeleteDialog = false
                    },
                    enabled = !state.isLoading,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) {
                    if (state.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.White)
                    } else {
                        Text("Excluir")
                    }
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }, enabled = !state.isLoading) { Text("Cancelar") }
            }
        )
    }

    if (showDatePicker) {
        val datePickerState = rememberDatePickerState(
            initialSelectedDateMillis = state.expirationDate
        )
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(onClick = {
                    datePickerState.selectedDateMillis?.let { viewModel.onDateChange(it) }
                    showDatePicker = false
                }) { Text("Confirmar") }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) { Text("Cancelar") }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Editar Produto", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Delete, 
                            contentDescription = "Excluir", 
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
                )
            )
        }
    ) { padding ->
        if (state.isLoading && state.product == null) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(scrollState)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Imagem do Produto
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFFF0F0F0))
                        .clickable {
                            pickMedia.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly))
                        },
                    contentAlignment = Alignment.Center
                ) {
                    AsyncImage(
                        model = state.imageUri.ifEmpty { R.drawable.ic_shopping },
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop,
                        error = painterResource(R.drawable.ic_shopping)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                BipandoTextField(
                    value = state.name,
                    onValueChange = viewModel::onNameChange,
                    label = "Nome do Produto",
                    leadingIcon = Icons.Default.ShoppingBasket
                )

                Spacer(modifier = Modifier.height(16.dp))

                BipandoTextField(
                    value = state.barcode,
                    onValueChange = viewModel::onBarcodeChange,
                    label = "Código de Barras",
                    leadingIcon = Icons.Default.QrCode
                )

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedCard(
                    onClick = { showCategoryPicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.Category, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = state.category?.name ?: "Selecionar Categoria",
                            modifier = Modifier.weight(1f)
                        )
                        Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                OutlinedCard(
                    onClick = { showDatePicker = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Default.CalendarToday, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text("Data de Vencimento", style = MaterialTheme.typography.labelSmall, color = Color.Gray)
                            Text(
                                text = TimeFormatter.formatTimestamp(state.expirationDate),
                                style = MaterialTheme.typography.bodyLarge
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                BipandoTextField(
                    value = state.note,
                    onValueChange = viewModel::onNoteChange,
                    label = "Notas",
                    leadingIcon = Icons.AutoMirrored.Filled.Notes
                )

                Spacer(modifier = Modifier.height(32.dp))

                BipandoButton(
                    text = "Salvar Alterações",
                    onClick = viewModel::saveProduct,
                    isLoading = state.isLoading
                )
            }
        }
    }

    if (showCategoryPicker) {
        ModalBottomSheet(onDismissRequest = { showCategoryPicker = false }) {
            Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
                Text("Selecione uma Categoria", style = MaterialTheme.typography.titleLarge, modifier = Modifier.padding(bottom = 16.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(state.categories) { category ->
                        ListItem(
                            headlineContent = { Text(category.name) },
                            modifier = Modifier.clickable {
                                viewModel.onCategoryChange(category)
                                showCategoryPicker = false
                            }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
