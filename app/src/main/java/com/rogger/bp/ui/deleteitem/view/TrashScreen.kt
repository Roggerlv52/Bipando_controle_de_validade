package com.rogger.bp.ui.deleteitem.view

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.DeleteForever
import androidx.compose.material.icons.filled.Restore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import com.rogger.bp.R
import com.rogger.bp.domain.model.Product
import com.rogger.bp.ui.deleteitem.presentation.TrashViewModel
import com.rogger.bp.util.TimeFormatter

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TrashScreen(
    viewModel: TrashViewModel,
    onBackClick: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Lixeira", fontWeight = FontWeight.Bold) },
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
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(Color(0xFFF5F5F5))
        ) {
            if (state.items.isEmpty() && !state.isLoading) {
                EmptyTrashState()
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(
                        items = state.items,
                        key = { it.uuid }
                    ) { product ->
                        TrashItem(
                            product = product,
                            onRestore = { viewModel.restoreItem(product) },
                            onDeletePermanently = { viewModel.deletePermanently(product) }
                        )
                    }
                }
            }

            if (state.isLoading) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun TrashItem(
    product: Product,
    onRestore: () -> Unit,
    onDeletePermanently: () -> Unit
) {
    var showDeleteConfirm by remember { mutableStateOf(false) }
    var showRestoreConfirm by remember { mutableStateOf(false) }
    var isRemoving by remember { mutableStateOf(false) }

    if (showDeleteConfirm) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirm = false },
            title = { Text("Excluir Definitivamente") },
            text = { Text("Esta ação não pode ser desfeita. Deseja excluir \"${product.name}\" para sempre?") },
            confirmButton = {
                Button(
                    onClick = {
                        isRemoving = true
                        onDeletePermanently()
                        showDeleteConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                ) { Text("Excluir") }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirm = false }) { Text("Cancelar") }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }

    if (showRestoreConfirm) {
        AlertDialog(
            onDismissRequest = { showRestoreConfirm = false },
            title = { Text("Restaurar Produto") },
            text = { Text("Deseja devolver \"${product.name}\" para a sua lista de produtos?") },
            confirmButton = {
                Button(
                    onClick = {
                        isRemoving = true
                        onRestore()
                        showRestoreConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF388E3C))
                ) { Text("Restaurar") }
            },
            dismissButton = {
                TextButton(onClick = { showRestoreConfirm = false }) { Text("Cancelar") }
            },
            shape = RoundedCornerShape(16.dp),
            containerColor = Color.White
        )
    }

    AnimatedVisibility(
        visible = !isRemoving,
        exit = shrinkVertically(animationSpec = tween(600)) + fadeOut()
    ) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Row(
                modifier = Modifier
                    .padding(12.dp)
                    .fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                AsyncImage(
                    model = product.imageUri.ifEmpty { R.drawable.ic_shopping },
                    contentDescription = null,
                    modifier = Modifier
                        .size(60.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFFF0F0F0))
                )

                Spacer(modifier = Modifier.width(12.dp))

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = product.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Vencimento: ${TimeFormatter.formatTimestamp(product.timestamp)}",
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }

                Row {
                    IconButton(onClick = { showRestoreConfirm = true }) {
                        Icon(Icons.Default.Restore, contentDescription = "Restaurar", tint = Color(0xFF388E3C))
                    }
                    IconButton(onClick = { showDeleteConfirm = true }) {
                        Icon(Icons.Default.DeleteForever, contentDescription = "Excluir", tint = Color(0xFFD32F2F))
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyTrashState() {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_deleted_products),
            contentDescription = null,
            modifier = Modifier.size(100.dp),
            tint = Color.LightGray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Sua lixeira está vazia",
            fontSize = 18.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
    }
}
