package com.rogger.bp.ui.home.view

import android.Manifest
import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.navigation.NavController
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
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Group
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PictureAsPdf
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TableChart
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
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
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDrawerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.DisposableEffect
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
import com.rogger.bp.R.drawable
import com.rogger.bp.data.model.PostCategory
import com.rogger.bp.domain.model.Product
import com.rogger.bp.ui.home.presentation.HomeState
import com.rogger.bp.ui.home.presentation.HomeViewModel
import com.rogger.bp.ui.commun.AnalyticsManager
import com.rogger.bp.ui.naviation.Routes
import com.rogger.bp.ui.theme.BipandoTheme
import com.rogger.bp.ui.componentes.LoadingDialog
import com.rogger.bp.ui.home.view.componentes.DrawerHeader
import com.rogger.bp.ui.home.view.componentes.DrawerItem
import com.rogger.bp.ui.home.view.componentes.EmptyState
import com.rogger.bp.ui.home.view.componentes.HomeTopAppBar
import com.rogger.bp.ui.home.view.componentes.ProductGroupHeader
import com.rogger.bp.ui.home.view.componentes.ProductItem
import com.rogger.bp.ui.home.view.componentes.SearchTopAppBar
import com.rogger.bp.util.CategorySelectionDialog
import com.rogger.bp.util.DeleteConfirmationDialog
import com.rogger.bp.util.ShareUtil
import com.rogger.bp.util.TimeFormatter
import com.rogger.bp.util.VoiceSearchDialog
import kotlinx.coroutines.launch
import okhttp3.internal.notify

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
    onPaymentClick: () -> Unit,
    onScannerSearch: () -> Unit,
    onLogout: () -> Unit
) {
    val state by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val drawerState = rememberDrawerState(initialValue = DrawerValue.Closed)
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    var showVoiceSearchDialog by remember { mutableStateOf(false) }

    val recordAudioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            showVoiceSearchDialog = true
        }
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

    // Refresh ao carregar ou voltar para a tela (Resume)
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) {
                viewModel.updateWorkMode(context)
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    LaunchedEffect(Unit) {
        // Carrega as informações do usuário. O sync de produtos ocorrerá após carregar o perfil ou no init.
        viewModel.loadUserInfo(context)
        viewModel.syncAndFetchProducts(context)
        
        if (initialCategoryId != null) {
            viewModel.fetchProducts(initialCategoryId, initialCategoryName)
        }
    }

    LaunchedEffect(state.errorMessage) {
        state.errorMessage?.let {
            android.widget.Toast.makeText(context, it, android.widget.Toast.LENGTH_LONG).show()
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
                DrawerHeader(state = state)
                Spacer(modifier = Modifier.height(8.dp))
                DrawerItem(
                    label = stringResource(R.string.menu_home),
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
                    label = stringResource(R.string.category),
                    icon = Icons.Default.Category,
                    count = state.categoryCount,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onCategoryClick()
                    }
                )
                DrawerItem(
                    label = stringResource(R.string.menu_profile),
                    icon = Icons.Default.Person,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onProfileClick()
                    }
                )
                DrawerItem(
                    label = stringResource(R.string.menu_premium),
                    icon = Icons.Default.Star,
                    onClick = {
                        scope.launch { drawerState.close() }
                        onPaymentClick()
                    }
                )
                DrawerItem(
                    label = stringResource(R.string.menu_groups),
                    icon = Icons.Default.Group,
                    onClick = {
                        scope.launch { drawerState.close() }
                        navController.navigate(Routes.GROUPS)
                    }
                )
                HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                DrawerItem(
                    label = stringResource(R.string.menu_share_app),
                    icon = Icons.Default.Share,
                    onClick = {
                        scope.launch { drawerState.close() }
                        ShareUtil.shareApp(context)
                    }
                )
                DrawerItem(
                    label = stringResource(R.string.menu_support),
                    icon = Icons.Default.Email,
                    onClick = {
                        scope.launch { drawerState.close() }
                        abrirSuporteEmail(context)
                    }
                )
            }
        }
    ) {
        HomeScreenContent(
            state = state,
            categories = categories,
            userRole = state.userRole,
            onProductClick = onProductClick,
            onImageClick = onImageClick,
            onScannerNavigate = onScannerNavigate,
            onCategoryClick = onCategoryClick,
            onMenuClick = { scope.launch { drawerState.open() } },
            onSearchQueryChange = viewModel::onSearchQueryChange,
            onToggleSearch = { active ->
                if (active) AnalyticsManager.logProductSearch("text")
                viewModel.toggleSearch(active)
            },
            onScannerSearch = {
                AnalyticsManager.logProductSearch("barcode")
                onScannerSearch()
            },
            onVoiceSearchClick = {
                recordAudioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
            },
            onDeleteProducts = viewModel::deleteProducts,
            onLogout = { viewModel.logout(context, onLogout) },
            onExportPdf = { viewModel.exportPdf(context) },
            onExportExcel = { viewModel.exportExcel(context) }
        )



        if (showVoiceSearchDialog) {
            VoiceSearchDialog(
                onDismiss = { showVoiceSearchDialog = false },
                onResult = { result ->
                    AnalyticsManager.logVoiceSearch()
                    AnalyticsManager.logProductSearch("voice")
                    viewModel.onSearchQueryChange(result)
                }
            )
        }
    }
}

private fun abrirSuporteEmail(context: android.content.Context) {
    val emailIntent = Intent(Intent.ACTION_SENDTO).apply {
        data = Uri.parse("mailto:bipandosuporte@gmail.com")
        putExtra(Intent.EXTRA_SUBJECT, context.getString(R.string.support_email_subject))
    }
    try {
        context.startActivity(Intent.createChooser(emailIntent, context.getString(R.string.support_email_chooser)))
    } catch (_: Exception) {
        // Tratar caso não tenha app de email
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreenContent(
    state: HomeState,
    categories: List<PostCategory>,
    userRole: String = "Admin",
    onProductClick: (Product) -> Unit,
    onImageClick: (String) -> Unit,
    onScannerNavigate: (String, String) -> Unit,
    onCategoryClick: () -> Unit,
    onMenuClick: () -> Unit,
    onSearchQueryChange: (String) -> Unit,
    onToggleSearch: (Boolean) -> Unit,
    onScannerSearch: () -> Unit,
    onVoiceSearchClick: () -> Unit,
    onDeleteProducts: (List<Product>) -> Unit,
    onLogout: () -> Unit,
    onExportPdf: () -> Unit,
    onExportExcel: () -> Unit
) {
    val context = LocalContext.current
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
                    onVoiceSearchClick = onVoiceSearchClick,
                    onCloseClick = { onToggleSearch(false) }
                )
            } else {
                HomeTopAppBar(
                    title =  state.currentGroupName,
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
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_product))
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .background(MaterialTheme.colorScheme.background)
        ) {
            // Se estiver carregando pela primeira vez, mostramos apenas o loader centralizado
            if (state.isLoading && state.products.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    CircularProgressIndicator()
                }
            } else if (isListVisuallyEmpty) {
                // Estado vazio real (após o carregamento terminar e não houver itens)
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
                                isGroupRemoving = isGroupRemoving,
                                yellowWarningLimit = state.yellowWarningDays,
                                userRole = userRole,
                                onRemoveGroup = { statusText ->
                                    if (userRole.equals("Reader", ignoreCase = true)) {
                                        android.widget.Toast.makeText(context, "Apenas Administradores ou Editores podem remover itens.", android.widget.Toast.LENGTH_SHORT).show()
                                    } else {
                                        deleteDialogProducts = items.map { it.first } to statusText
                                    }
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
        }
    }
}

