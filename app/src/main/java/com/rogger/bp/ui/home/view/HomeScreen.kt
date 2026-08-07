package com.rogger.bp.ui.home.view

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.NavController
import androidx.navigation.compose.rememberNavController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CalendarToday
import androidx.compose.material.icons.filled.Category
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.QrCode
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DrawerValue
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalDrawerSheet
import androidx.compose.material3.ModalNavigationDrawer
import androidx.compose.material3.NavigationDrawerItem
import androidx.compose.material3.NavigationDrawerItemDefaults
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import com.rogger.bp.R
import com.rogger.bp.data.model.PostCategory
import com.rogger.bp.domain.model.Product
import com.rogger.bp.ui.home.presentation.HomeState
import com.rogger.bp.ui.home.presentation.HomeViewModel
import com.rogger.bp.ui.theme.BipandoTheme
import com.rogger.bp.util.CategorySelectionDialog
import com.rogger.bp.util.DeleteConfirmationDialog
import com.rogger.bp.util.TimeFormatter
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    navController: NavController,
    initialCategoryId: String? = null,
    initialCategoryName: String? = null,
    onProductClick: (Product) -> Unit,
    onImageClick: (String) -> Unit,
    onScannerNavigate: (String, String) -> Unit,
    onProfileClick: () -> Unit,
    onCategoryClick: () -> Unit,
    onTrashClick: () -> Unit,
    onPaymentClick: () -> Unit,
    onScannerSearch: () -> Unit,
    onLogout: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    val imagePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        uri?.let { viewModel.uploadProfileImage(context, it) }
    }

    var showEditNameDialog by remember { mutableStateOf(false) }
    var editedName by remember { mutableStateOf("") }

    if (showEditNameDialog) {
        AlertDialog(
            onDismissRequest = { showEditNameDialog = false },
            title = { Text("Editar Nome") },
            text = {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Seu Nome") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.updateUserName(context, editedName)
                        showEditNameDialog = false
                    }
                ) {
                    Text("Salvar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showEditNameDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }

    // Observa o resultado do scanner para pesquisa vindo do savedStateHandle
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    LaunchedEffect(navBackStackEntry) {
        navBackStackEntry?.savedStateHandle?.get<String>("search_barcode")?.let { barcode ->
            viewModel.toggleSearch(true)
            viewModel.onSearchQueryChange(barcode)
            navBackStackEntry?.savedStateHandle?.remove<String>("search_barcode")
        }
    }

    LaunchedEffect(Unit) {
        viewModel.syncAndFetchProducts(context)
        viewModel.fetchCategories()
        if (initialCategoryId != null) {
            viewModel.fetchProducts(initialCategoryId, initialCategoryName)
        }
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        scrimColor = Color.Black.copy(alpha = 0.8f), // Adiciona uma sombra mais intensa ao fundo
        drawerContent = {
            ModalDrawerSheet(
                modifier = Modifier.width(300.dp), // Define uma largura fixa menor para aumentar a margem à direita
                windowInsets = WindowInsets(0, 0, 0, 0) // Remove insets para o header encostar no topo
            ) {
                DrawerHeader(
                    state = state,
                    onNameClick = {
                        editedName = state.userName
                        showEditNameDialog = true
                    },
                    onImageClick = {
                        imagePickerLauncher.launch("image/*")
                    }
                )
                Spacer(modifier = Modifier.height(8.dp))
                DrawerItem(
                    label = "Home",
                    icon = Icons.Default.Home,
                    count = state.activeCount,
                    onClick = {
                        scope.launch { drawerState.close() }
                        // Se estiver filtrado por categoria ou se houver uma busca/filtro ativo,
                        // navegamos para a rota base "home" sem argumentos, resetando o estado da navegação.
                        if (state.categoryFilterName != null || state.isSearchActive) {
                            navController.navigate("home") {
                                popUpTo("home") { inclusive = true }
                            }
                        }
                    }
                )
                DrawerItem(
                    label = "Categorias",
                    icon = Icons.Default.Category,
                    count = state.categoryCount,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onCategoryClick()
                    }
                )
                DrawerItem(
                    label = "Lixeira",
                    icon = Icons.Default.Delete,
                    count = state.deletedCount,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onTrashClick()
                    }
                )
                DrawerItem(
                    label = "Perfil",
                    icon = Icons.Default.Person,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onProfileClick()
                    }
                )
                DrawerItem(
                    label = "Premium",
                    icon = Icons.Default.Star,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onPaymentClick()
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                DrawerItem(
                    label = "Compartilhar App",
                    icon = Icons.Default.Share,
                    onClick = {
                        scope.launch { drawerState.close() }
                        compartilharApp(context)
                    }
                )
            }
        }
    ) {
        HomeScreenContent(
            state = state,
            categories = categories,
            onProductClick = onProductClick,
            onImageClick = onImageClick,
            onScannerNavigate = onScannerNavigate,
            onCategoryClick = onCategoryClick,
            onMenuClick = { scope.launch { drawerState.open() } },
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onToggleSearch = viewModel::toggleSearch,
            onScannerSearch = onScannerSearch,
            onDeleteProducts = viewModel::deleteProducts,
            onLogout = { viewModel.logout(context, onLogout) },
            onExportPdf = { viewModel.exportPdf(context) },
            onExportExcel = { viewModel.exportExcel(context) }
        )
    }
}

@Composable
fun DrawerHeader(state: HomeState, onNameClick: () -> Unit, onImageClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.primary)
            .statusBarsPadding()
            .height(180.dp)
            .padding(16.dp),
        contentAlignment = Alignment.BottomStart
    ) {
        Column {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current)
                    .data(state.userPhoto.ifEmpty { R.drawable.ic_person_24 })
                    .crossfade(true)
                    .build(),
                contentDescription = null,
                modifier = Modifier
                    .size(64.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.surface)
                    .clickable(onClick = onImageClick),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.userName,
                color = MaterialTheme.colorScheme.onPrimary,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                modifier = Modifier.clickable(onClick = onNameClick)
            )
            Text(
                text = state.userEmail,
                color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f),
                fontSize = 14.sp
            )
        }
    }
}

@Composable
fun DrawerItem(
    label: String,
    icon: ImageVector,
    count: Int = 0,
    onClick: () -> Unit
) {
    NavigationDrawerItem(
        label = {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(text = label)
                if (count > 0) {
                    Badge(containerColor = MaterialTheme.colorScheme.secondaryContainer) {
                        Text(text = if (count > 99) "99+" else count.toString())
                    }
                }
            }
        },
        selected = false,
        onClick = onClick,
        icon = { Icon(icon, contentDescription = null) },
        modifier = Modifier.padding(NavigationDrawerItemDefaults.ItemPadding)
    )
}

private fun compartilharApp(context: android.content.Context) {
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "text/plain"
        val message = context.getString(R.string.share_app_message, context.packageName)
        putExtra(Intent.EXTRA_TEXT, message)
    }
    context.startActivity(
        Intent.createChooser(
            shareIntent,
            context.getString(R.string.share_app_title)
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    state: HomeState,
    categories: List<PostCategory>,
    onProductClick: (Product) -> Unit,
    onImageClick: (String) -> Unit,
    onScannerNavigate: (String, String) -> Unit,
    onCategoryClick: () -> Unit,
    onMenuClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: (Boolean) -> Unit,
    onScannerSearch: () -> Unit,
    onDeleteProducts: (List<Product>) -> Unit,
    onLogout: () -> Unit,
    onExportPdf: () -> Unit,
    onExportExcel: () -> Unit
) {
    val scope = rememberCoroutineScope()
    var showCategoryDialog by remember { mutableStateOf(false) }
    var deleteDialogProducts by remember { mutableStateOf<Pair<List<Product>, String>?>(null) }
    val removingProductUuids = remember { mutableStateListOf<String>() }

    // Estado derivado para evitar que o "EmptyState" apareça enquanto itens ainda estão sendo removidos visualmente
    val isListVisuallyEmpty by remember(state.products, removingProductUuids) {
        derivedStateOf {
            state.products.isEmpty() || state.products.all { removingProductUuids.contains(it.uuid) }
        }
    }

    if (showCategoryDialog) {
        CategorySelectionDialog(
            categories = categories,
            onDismiss = { showCategoryDialog = false },
            onCategorySelected = { id, name -> onScannerNavigate(id, name) },
            onAddCategory = { onCategoryClick() }
        )
    }

    deleteDialogProducts?.let { (products, statusText) ->
        val total = products.size
        val message = if (total == 1) {
            stringResource(id = R.string.delete_confirm_single, products.first().name)
        } else {
            stringResource(id = R.string.delete_confirm_multiple, total, statusText)
        }

        DeleteConfirmationDialog(
            message = message,
            onDismiss = { deleteDialogProducts = null },
            onConfirm = {
                val uuids = products.map { it.uuid }
                removingProductUuids.addAll(uuids)
                scope.launch {
                    // Tempo da animação (600ms) + um pequeno buffer para garantir suavidade
                    kotlinx.coroutines.delay(650) 
                    onDeleteProducts(products)
                    
                    // Aguardamos o processamento do DB e a emissão do novo Flow de produtos
                    // Isso evita que o item 'pisque' de volta caso a lista local ainda contenha o item
                    kotlinx.coroutines.delay(400)
                    removingProductUuids.removeAll(uuids)
                    deleteDialogProducts = null
                }
            }
        )
    }

    Scaffold(
        topBar = {
            if (state.isSearchActive) {
                SearchTopAppBar(
                    query = state.searchQuery,
                    onQueryChange = onSearchQueryChange,
                    onBarcodeClick = onScannerSearch,
                    onCloseClick = { onToggleSearch(false) }
                )
            } else {
                HomeTopAppBar(
                    title = state.categoryFilterName ?: "",
                    onSearchClick = { onToggleSearch(true) },
                    onMenuClick = onMenuClick,
                    onLogout = onLogout,
                    onExportPdf = onExportPdf,
                    onExportExcel = onExportExcel
                )
            }
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCategoryDialog = true },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Adicionar Produto")
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            if (state.isFirstLoad || (state.isLoading && state.products.isEmpty())) {
                // Não mostra nada ou apenas o progresso durante a primeira carga
                // Isso evita que o "EmptyState" (imagem de lista vazia) apareça
                // enquanto o Room ainda está lendo os dados.
            } else if (isListVisuallyEmpty) {
                EmptyState(
                    isSearch = state.searchQuery.isNotEmpty(),
                    onAddClick = { showCategoryDialog = true }
                )
            } else {
                val groupedProducts = remember(state.products) {
                    state.products
                        .map { it to TimeFormatter.getDaysRemaining(it.timestamp) }
                        .sortedBy { it.second }
                        .groupBy { it.second }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 0.5.dp, bottom = 80.dp)
                ) {
                    groupedProducts.forEach { (days, items) ->
                        val isGroupRemoving =
                            items.all { removingProductUuids.contains(it.first.uuid) }

                        item {
                            ProductGroupHeader(
                                daysRemaining = days,
                                isGroupRemoving,
                                yellowWarningLimit = state.yellowWarningDays,
                                onRemoveGroup = { statusText ->
                                    deleteDialogProducts = items.map { it.first } to statusText
                                }
                            )
                        }

                        itemsIndexed(
                            items = items,
                            key = { _, item -> item.first.uuid },
                            contentType = { _, _ -> "product" }
                        ) { index, (product, _) ->
                            val isItemVisible = !removingProductUuids.contains(product.uuid)
                            val itemOnClick = remember(product.uuid) { { onProductClick(product) } }
                            val itemOnImageClick = remember(product.uuid) { 
                                { if (product.imageUri.isNotEmpty()) onImageClick(product.imageUri) } 
                            }

                            ProductItem(
                                product = product,
                                isItemVisible = isItemVisible,
                                showDivider = index < items.lastIndex,
                                onClick = itemOnClick,
                                onImageClick = itemOnImageClick
                            )
                        }
                    }
                }
            }

            if (state.isFirstLoad) {
                CircularProgressIndicator(modifier = Modifier.align(Alignment.Center))
            }
        }
    }
}

@Composable
fun ProductGroupHeader(
    daysRemaining: Long,
    isGroupRemoving : Boolean,
    yellowWarningLimit: Int,
    onRemoveGroup: (String) -> Unit
) {
    val isDark =
        MaterialTheme.colorScheme.surface.run { (red * 0.299 + green * 0.587 + blue * 0.114) < 0.5 }

    val (backgroundColor, dotColor, textColor, statusText) = remember(
        daysRemaining,
        yellowWarningLimit,
        isDark
    ) {
        val bg: Color
        val dot: Color
        val txt: Color
        val label: String

        when {
            daysRemaining < 1 -> {
                // 🔴 Caso A: Vencido / Expired
                bg = if (isDark) Color(0xFF450A0A) else Color(0xFFFDF2F2)
                dot = Color(0xFFEF4444)
                txt = if (isDark) Color(0xFFFECACA) else Color(0xFF991B1B)
                label = if (daysRemaining == 0L) "Hoje" else "Vencido"
            }

            daysRemaining <= yellowWarningLimit -> {
                // 🟠 Caso B: Próximo do vencimento (Laranja/Amarelo)
                bg = if (isDark) Color(0xFF451A03) else Color(0xFFFFFBEB)
                dot = Color(0xFFF59E0B)
                txt = if (isDark) Color(0xFFFED7AA) else Color(0xFF92400E)
                label = if (daysRemaining == 1L) "Amanhã" else "$daysRemaining dias restantes"
            }

            else -> {
                // 🟢 Caso C: Seguro (Verde)
                bg = if (isDark) Color(0xFF064E3B) else Color(0xFFF0FDF4)
                dot = Color(0xFF10B981)
                txt = if (isDark) Color(0xFFD1FAE5) else Color(0xFF065F46)
                label = "$daysRemaining dias restantes"
            }
        }
        listOf(bg, dot, txt, label)
    }
    AnimatedVisibility(
        visible = !isGroupRemoving,
        exit = shrinkVertically(
            animationSpec = tween(600),
            shrinkTowards = Alignment.Top,
        ) + fadeOut()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(backgroundColor as Color)
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(8.dp)
                        .clip(CircleShape)
                        .background(dotColor as Color)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Text(
                    text = statusText as String,
                    color = textColor as Color,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }

            IconButton(
                onClick = { onRemoveGroup(statusText as String) },
                modifier = Modifier.size(20.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Remover Grupo",
                    tint = (textColor as Color).copy(alpha = 0.6f),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeTopAppBar(
    title: String,
    onSearchClick: () -> Unit,
    onMenuClick: () -> Unit,
    onLogout: () -> Unit,
    onExportPdf: () -> Unit,
    onExportExcel: () -> Unit
) {
    var showMenu by remember { mutableStateOf(false) }

    CenterAlignedTopAppBar(
        title = {
            Text(text = title, fontWeight = FontWeight.Bold)
        },
        navigationIcon = {
            IconButton(onClick = onMenuClick) {
                Icon(Icons.Default.Menu, contentDescription = "Menu")
            }
        },
        actions = {
            IconButton(onClick = onSearchClick) {
                Icon(Icons.Default.Search, contentDescription = "Pesquisar")
            }
            Box {
                IconButton(onClick = { showMenu = true }) {
                    Icon(Icons.Default.MoreVert, contentDescription = "Mais opções")
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Exportar PDF") },
                        onClick = {
                            showMenu = false
                            onExportPdf()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.Default.PictureAsPdf,
                                contentDescription = null
                            )
                        }
                    )
                    DropdownMenuItem(
                        text = { Text("Exportar Excel") },
                        onClick = {
                            showMenu = false
                            onExportExcel()
                        },
                        leadingIcon = { Icon(Icons.Default.TableChart, contentDescription = null) }
                    )
                    HorizontalDivider()
                    DropdownMenuItem(
                        text = { Text("Sair") },
                        onClick = {
                            showMenu = false
                            onLogout()
                        },
                        leadingIcon = {
                            Icon(
                                Icons.AutoMirrored.Filled.Logout,
                                contentDescription = null
                            )
                        }
                    )
                }
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary,
            titleContentColor = MaterialTheme.colorScheme.onPrimary,
            actionIconContentColor = MaterialTheme.colorScheme.onPrimary,
            navigationIconContentColor = MaterialTheme.colorScheme.onPrimary
        )


    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SearchTopAppBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onBarcodeClick: () -> Unit,
    onCloseClick: () -> Unit
) {
    TopAppBar(
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        "Pesquisar produtos...",
                        color = MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.7f)
                    )
                },
                singleLine = true,
                trailingIcon = {
                    IconButton(onClick = onBarcodeClick) {
                        Icon(
                            imageVector = Icons.Default.QrCode,
                            contentDescription = "Escanear Código",
                            tint = MaterialTheme.colorScheme.onPrimary
                        )
                    }
                },
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.onPrimary,
                    focusedTextColor = MaterialTheme.colorScheme.onPrimary,
                    unfocusedTextColor = MaterialTheme.colorScheme.onPrimary,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        },
        navigationIcon = {
            IconButton(onClick = onCloseClick) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Fechar Pesquisa",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = MaterialTheme.colorScheme.primary
        )
    )
}

@Composable
fun EmptyState(isSearch: Boolean, onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Image(
            painter = painterResource(id = if (isSearch) R.drawable.ic_search_24 else R.drawable.lista_vazia),
            contentDescription = null,
            modifier = Modifier.size(200.dp)
        )
        Spacer(modifier = Modifier.height(24.dp))
        Text(
            text = if (isSearch) "Nenhum produto encontrado para sua busca." else stringResource(id = R.string.txt_empty_list),
            textAlign = TextAlign.Center,
            fontSize = 18.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (!isSearch) {
            Spacer(modifier = Modifier.height(24.dp))
            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(12.dp)
            ) {
                Text("Adicionar Meu Primeiro Produto")
            }
        }
    }
}

@Composable
fun ProductItem(
    product: Product,
    isItemVisible: Boolean,
    showDivider: Boolean,
    onClick: () -> Unit,
    onImageClick: () -> Unit
) {
    val formattedDate = remember(product.timestamp) {
        TimeFormatter.formatTimestamp(product.timestamp)
    }

    val context = LocalContext.current
    val imageRequest = remember(product.imageUri) {
        ImageRequest.Builder(context)
            .data(product.imageUri.ifEmpty { R.drawable.ic_shopping })
            .size(200, 200) // Tamanho otimizado para thumbnail
            .precision(Precision.INEXACT) // Maior performance no cache
            .crossfade(false) // Sem animação para scroll ultra-suave
            .build()
    }

    // Só aplica AnimatedVisibility se o item estiver em processo de remoção
    // Isso reduz a profundidade da árvore de UI durante o scroll normal
    if (isItemVisible) {
        ProductItemContent(product, formattedDate, imageRequest, showDivider, onClick, onImageClick)
    } else {
        AnimatedVisibility(
            visible = false,
            exit = shrinkVertically(
                animationSpec = tween(500),
                shrinkTowards = Alignment.Top
            ) + fadeOut()
        ) {
            ProductItemContent(product, formattedDate, imageRequest, showDivider, onClick, onImageClick)
        }
    }
}

@Composable
private fun ProductItemContent(
    product: Product,
    formattedDate: String,
    imageRequest: ImageRequest,
    showDivider: Boolean,
    onClick: () -> Unit,
    onImageClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .clickable(onClick = onClick)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.Top
        ) {
            AsyncImage(
                model = imageRequest,
                contentDescription = product.name,
                modifier = Modifier
                    .size(70.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant)
                    .then(
                        if (product.imageUri.isNotEmpty()) {
                            Modifier.clickable { onImageClick() }
                        } else {
                            Modifier
                        }
                    ),
                contentScale = ContentScale.Crop,
                error = painterResource(R.drawable.ic_shopping),
                placeholder = painterResource(R.drawable.ic_shopping)
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = product.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(12.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = product.barcode,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f),
                    letterSpacing = 1.sp
                )

                Text(
                    text = product.categoryName,
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.8f),
                    textAlign = TextAlign.End,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        if (showDivider) {
            HorizontalDivider(
                modifier = Modifier.padding(horizontal = 16.dp),
                thickness = 0.5.dp,
                color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun HomeScreenPreview() {
    BipandoTheme {
        HomeScreenContent(
            state = HomeState(
                products = listOf(
                    Product(
                        name = "Tirolez Mussarela U lactose 150g",
                        categoryName = "Queijos",
                        barcode = "7896030520198",
                        timestamp = System.currentTimeMillis() - 86400000
                    ),
                    Product(
                        name = "Verde Campo Lacfree Cottage",
                        categoryName = "Queijos",
                        barcode = "7898205920239",
                        timestamp = System.currentTimeMillis() + 86400000 * 13
                    ),
                    Product(
                        name = "Mussarela Búfala Bom Destino 550g",
                        categoryName = "Queijos",
                        barcode = "7898130990468",
                        timestamp = System.currentTimeMillis() + 86400000 * 15
                    ),
                    Product(
                        name = "Seara Bacon Double Smoked 180g",
                        categoryName = "Embutidos",
                        barcode = "7894904097296",
                        timestamp = System.currentTimeMillis() + 86400000 * 15
                    )
                ),
                isLoading = false
            ),
            categories = emptyList(),
            onProductClick = {},
            onImageClick = {},
            onScannerNavigate = { _, _ -> },
            onCategoryClick = {},
            onMenuClick = {},
            onSearchQueryChange = {},
            onToggleSearch = {},
            onDeleteProducts = {},
            onScannerSearch = {},
            onLogout = {},
            onExportPdf = {},
            onExportExcel = {}
        )
    }
}
