package com.rogger.bp.ui.add.view

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.rogger.bp.R
import com.rogger.bp.R.drawable
import com.rogger.bp.domain.model.Category
import com.rogger.bp.ui.add.presentation.AddProductViewModel
import com.rogger.bp.ui.componentes.BipandoButton
import com.rogger.bp.ui.componentes.BipandoTextField
import com.rogger.bp.util.ImagePickerBottomSheet
import com.rogger.bp.util.ImagePikerUtil
import com.rogger.bp.util.TimeFormatter
import java.io.File

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddProductScreen(
    viewModel: AddProductViewModel,
    barcode: String? = null,
    initialCategoryId: String? = null,
    onBackClick: () -> Unit,
    onSaveSuccess: () -> Unit,
    onBarcodeClick: (String) -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val scrollState = rememberScrollState()
    val context = LocalContext.current

    var showDatePicker by remember { mutableStateOf(false) }
    var showCategoryPicker by remember { mutableStateOf(false) }
    var showImagePicker by remember { mutableStateOf(false) }

    var cameraImageFile by remember { mutableStateOf<File?>(null) }

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { viewModel.onImageChange(it) }
    }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicture()
    ) { success ->
        if (success && cameraImageFile != null) {
            viewModel.onImageChange(Uri.fromFile(cameraImageFile))
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            val file = ImagePikerUtil.createImageFile(context)
            cameraImageFile = file
            val uri = ImagePikerUtil.getUriForFile(context, file)
            cameraLauncher.launch(uri)
        }
    }

    LaunchedEffect(barcode) {
        barcode?.let { viewModel.onBarcodeChange(it) }
    }

    LaunchedEffect(initialCategoryId, state.categories) {
        if (initialCategoryId != null && state.categories.isNotEmpty()) {
            state.categories.find { it.id == initialCategoryId }?.let {
                viewModel.onCategoryChange(it)
            }
        }
    }

    LaunchedEffect(state.isSaved) {
        if (state.isSaved) {
            onSaveSuccess()
        }
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
        topBar = {
            TopAppBar(
                title = { Text("Novo Produto", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Voltar")
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
                        showImagePicker = true
                    },
                contentAlignment = Alignment.Center
            ) {
                if (state.imageUri != null) {
                    AsyncImage(
                        model = state.imageUri,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                } else {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(Icons.Default.AddAPhoto, contentDescription = null, tint = Color.Gray, modifier = Modifier.size(40.dp))
                        Text("Adicionar Foto", color = Color.Gray, fontSize = 12.sp)
                    }
                }
            }

            if (state.barcode.isNotEmpty()) {
                Surface(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .clickable { onBarcodeClick(state.barcode) },
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model =   drawable.ic_barcode_scanner_24,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = state.barcode,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(32.dp))

            BipandoTextField(
                value = state.name,
                onValueChange = viewModel::onNameChange,
                label = "Nome do Produto",
                leadingIcon = Icons.Default.ShoppingBasket
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Seletor de Categoria
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
                        modifier = Modifier.weight(1f),
                        color = if (state.category == null) Color.Gray else Color.Unspecified
                    )
                    Icon(Icons.Default.ArrowDropDown, contentDescription = null)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Data de Vencimento
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
                label = "Notas (Opcional)",
                leadingIcon = Icons.Default.Notes
            )

            Spacer(modifier = Modifier.height(32.dp))

            BipandoButton(
                text = "Salvar Produto",
                onClick = viewModel::saveProduct,
                isLoading = state.isLoading
            )

            if (state.errorMessage != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Text(state.errorMessage!!, color = MaterialTheme.colorScheme.error)
            }
        }
    }

    // Modal para Categoria
    if (showCategoryPicker) {
        ModalBottomSheet(
            onDismissRequest = { showCategoryPicker = false }
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp)
            ) {
                Text("Selecione uma Categoria", style = MaterialTheme.typography.titleLarge,
                    modifier = Modifier.padding(bottom = 16.dp))
                LazyColumn(modifier = Modifier.heightIn(max = 400.dp)) {
                    items(state.categories) { category ->
                        ListItem(
                            headlineContent = { Text(category.name) },
                            modifier = Modifier.clickable {
                                viewModel.onCategoryChange(category)
                                showCategoryPicker = false
                            },
                            colors = ListItemDefaults.colors(containerColor = Color.Transparent)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }

    if (showImagePicker) {
        ImagePickerBottomSheet(
            onDismiss = { showImagePicker = false },
            onCameraClick = {
                cameraPermissionLauncher.launch(android.Manifest.permission.CAMERA)
            },
            onGalleryClick = {
                imagePickerLauncher.launch("image/*")
            }
        )
    }
}
