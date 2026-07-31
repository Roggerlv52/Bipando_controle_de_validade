package com.rogger.bp.ui.home.view

import android.content.Intent
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.tooling.preview.Preview
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import coil3.size.Precision
import com.rogger.bp.R
import com.rogger.bp.domain.model.Product
import com.rogger.bp.ui.home.presentation.HomeViewModel
import com.rogger.bp.ui.home.presentation.HomeState
import com.rogger.bp.util.CategorySelectionDialog
import com.rogger.bp.util.DeleteConfirmationDialog
import com.rogger.bp.data.model.PostCategory
import com.rogger.bp.ui.theme.BipandoTheme
import com.rogger.bp.util.TimeFormatter
import kotlinx.coroutines.launch

@Composable
fun HomeScreen(
    viewModel: HomeViewModel,
    onProductClick: (Product) -> Unit,
    onImageClick: (String) -> Unit,
    onScannerNavigate: (String, String) -> Unit,
    onProfileClick: () -> Unit,
    onCategoryClick: () -> Unit,
    onTrashClick: () -> Unit,
    onPaymentClick: () -> Unit,
    onLogout: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    LaunchedEffect(Unit) {
        viewModel.syncAndFetchProducts(context)
        viewModel.fetchCategories()
    }

    ModalNavigationDrawer(
        drawerState = drawerState,
        drawerContent = {
            ModalDrawerSheet {
                DrawerHeader(state)
                Spacer(modifier = Modifier.height(8.dp))
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
            onDeleteProducts = viewModel::deleteProducts,
            onLogout = { viewModel.logout(context, onLogout) },
            onExportPdf = { viewModel.exportPdf(context) },
            onExportExcel = { viewModel.exportExcel(context) }
        )
    }
}

@Composable
fun DrawerHeader(state: HomeState) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .background(MaterialTheme.colorScheme.primary)
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
                    .background(Color.White),
                contentScale = ContentScale.Crop
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = state.userName,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
            )
            Text(
                text = state.userEmail,
                color = Color.White.copy(alpha = 0.8f),
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
    context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share_app_title)))
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
    onDeleteProducts: (List<Product>) -> Unit,
    onLogout: () -> Unit,
    onExportPdf: () -> Unit,
    onExportExcel: () -> Unit
) {
    var showCategoryDialog by remember { mutableStateOf(false) }
    var deleteDialogProducts by remember { mutableStateOf<Pair<List<Product>, String>?>(null) }

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
            onConfirm = { onDeleteProducts(products) }
        )
    }

    Scaffold(
        topBar = {
            if (state.isSearchActive) {
                SearchTopAppBar(
                    query = state.searchQuery,
                    onQueryChange = onSearchQueryChange,
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
                .background(Color.White)
        ) {
            if (state.products.isEmpty() && !state.isLoading) {
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
                    contentPadding = PaddingValues(bottom = 80.dp)
                ) {
                    groupedProducts.forEach { (days, items) ->
                        item {
                            ProductGroupHeader(
                                daysRemaining = days,
                                yellowWarningLimit = state.yellowWarningDays,
                                onRemoveGroup = { statusText ->
                                    deleteDialogProducts = items.map { it.first } to statusText
                                }
                            )
                        }
                        
                        items(
                            items = items,
                            key = { it.first.uuid },
                            contentType = { "product" }
                        ) { (product, _) ->
                            ProductItem(
                                product = product, 
                                onClick = { onProductClick(product) },
                                onImageClick = { 
                                    if (product.imageUri.isNotEmpty()) {
                                        onImageClick(product.imageUri)
                                    }
                                }
                            )
                            HorizontalDivider(
                                modifier = Modifier.padding(horizontal = 16.dp),
                                thickness = 0.5.dp,
                                color = Color.LightGray.copy(alpha = 0.5f)
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

@Composable
fun ProductGroupHeader(
    daysRemaining: Long,
    yellowWarningLimit: Int,
    onRemoveGroup: (String) -> Unit
) {
    val (backgroundColor, dotColor, textColor, statusText) = remember(daysRemaining, yellowWarningLimit) {
        val bg: Color
        val dot: Color
        val txt: Color
        val label: String

        when {
            daysRemaining < 1 -> {
                // 🔴 Caso A: Vencido / Expired
                bg = Color(0xFFFDF2F2)
                dot = Color(0xFFEF4444)
                txt = Color(0xFF991B1B)
                label = if (daysRemaining == 0L) "Hoje" else "Vencido"
            }
            daysRemaining <= yellowWarningLimit -> {
                // 🟠 Caso B: Próximo do vencimento (Laranja/Amarelo)
                bg = Color(0xFFFFFBEB)
                dot = Color(0xFFF59E0B)
                txt = Color(0xFF92400E)
                label = if (daysRemaining == 1L) "Amanhã" else "$daysRemaining dias restantes"
            }
            else -> {
                // 🟢 Caso C: Seguro (Verde)
                bg = Color(0xFFF0FDF4)
                dot = Color(0xFF10B981)
                txt = Color(0xFF065F46)
                label = "$daysRemaining dias restantes"
            }
        }
        listOf(bg, dot, txt, label)
    }

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
                tint = Color(0xFF9CA3AF),
                modifier = Modifier.size(20.dp)
            )
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
                        leadingIcon = { Icon(Icons.Default.PictureAsPdf, contentDescription = null) }
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
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Logout, contentDescription = null) }
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
    onCloseClick: () -> Unit
) {
    TopAppBar(
        title = {
            TextField(
                value = query,
                onValueChange = onQueryChange,
                modifier = Modifier.fillMaxWidth(),
                placeholder = { Text("Pesquisar produtos...", color = Color.White.copy(alpha = 0.7f)) },
                singleLine = true,
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    cursorColor = Color.White,
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent
                )
            )
        },
        navigationIcon = {
            IconButton(onClick = onCloseClick) {
                Icon(Icons.Default.Close, contentDescription = "Fechar Pesquisa", tint = Color.White)
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
fun ProductItem(product: Product, onClick: () -> Unit, onImageClick: () -> Unit) {
    val formattedDate = remember(product.timestamp) {
        TimeFormatter.formatTimestamp(product.timestamp)
    }

    val context = LocalContext.current
    val imageRequest = remember(product.imageUri, product.uuid) {
        ImageRequest.Builder(context)
            .data(product.imageUri.ifEmpty { R.drawable.ic_shopping })
            .crossfade(true)
            .size(180, 180) 
            .precision(Precision.EXACT)
            .build()
    }

    Surface(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        color = Color.White
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
                    .clip(RoundedCornerShape(4.dp))
                    .background(Color(0xFFF0F0F0))
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
                    fontWeight = FontWeight.Medium,
                    color = Color.DarkGray,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(modifier = Modifier.height(4.dp))

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.CalendarToday,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = Color.Gray
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = formattedDate,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray
                    )
                }
                
                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = product.barcode,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.LightGray,
                    letterSpacing = 1.sp
                )

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        text = product.categoryName,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.Gray,
                        textAlign = TextAlign.End
                    )
                }
            }
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
                    Product(name = "Tirolez Mussarela U lactose 150g", categoryName = "Queijos", barcode = "7896030520198", timestamp = System.currentTimeMillis() - 86400000),
                    Product(name = "Verde Campo Lacfree Cottage", categoryName = "Queijos", barcode = "7898205920239", timestamp = System.currentTimeMillis() + 86400000 * 13),
                    Product(name = "Mussarela Búfala Bom Destino 550g", categoryName = "Queijos", barcode = "7898130990468", timestamp = System.currentTimeMillis() + 86400000 * 15),
                    Product(name = "Seara Bacon Double Smoked 180g", categoryName = "Embutidos", barcode = "7894904097296", timestamp = System.currentTimeMillis() + 86400000 * 15)
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
            onLogout = {},
            onExportPdf = {},
            onExportExcel = {}
        )
    }
}
