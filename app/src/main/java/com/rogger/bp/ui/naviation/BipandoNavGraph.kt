package com.rogger.bp.ui.naviation

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.google.firebase.auth.FirebaseAuth
import com.rogger.bp.data.database.BpDatabase
import com.rogger.bp.data.repository.AuthRepositoryImpl
import com.rogger.bp.data.repository.ProductRepositoryImpl
import com.rogger.bp.domain.usecase.*
import com.rogger.bp.ui.add.presentation.AddProductViewModel
import com.rogger.bp.ui.add.view.AddProductScreen
import com.rogger.bp.ui.category.presentation.CategoryViewModel
import com.rogger.bp.ui.category.view.CategoryScreen
import com.rogger.bp.ui.commun.DependencyInjector
import com.rogger.bp.ui.commun.SharedPreferencesManager
import com.rogger.bp.ui.deleteitem.presentation.TrashViewModel
import com.rogger.bp.ui.deleteitem.view.TrashScreen
import com.rogger.bp.ui.edit.presentation.EditProductViewModel
import com.rogger.bp.ui.edit.view.EditProductScreen
import com.rogger.bp.ui.groups.presentation.GroupsViewModel
import com.rogger.bp.ui.groups.view.GroupsScreen
import com.rogger.bp.ui.home.presentation.HomeViewModel
import com.rogger.bp.ui.home.view.HomeScreen
import com.rogger.bp.ui.home.view.ImagePreviewScreen
import com.rogger.bp.ui.login.presentation.LoginViewModel
import com.rogger.bp.ui.login.view.LoginScreen
import com.rogger.bp.ui.payment.presentation.PaymentViewModel
import com.rogger.bp.ui.payment.view.PaymentScreen
import com.rogger.bp.ui.profile.presentation.ProfileViewModel
import com.rogger.bp.ui.profile.view.ProfileScreen
import com.rogger.bp.ui.scanner.ScannerScreen
import com.rogger.bp.ui.splash.presentation.SplashViewModel
import com.rogger.bp.ui.splash.view.SplashScreen
import com.rogger.bp.ui.theme.BipandoTheme
import com.rogger.bp.ui.theme.BipandoThemeType
import com.rogger.bp.ui.tour.OnboardingScreen

@Composable
fun BipandoNavGraph(navController: NavHostController) {
    val context = LocalContext.current
    val database = BpDatabase.getDatabase(context)
    val auth = FirebaseAuth.getInstance()

    // Log de diagnóstico do Back Stack
    LaunchedEffect(navController) {
        navController.currentBackStackEntryFlow.collect { entry ->
            Log.d("NAV", "atual=${entry.destination.route}")
        }
    }

    // A Splash Screen é a rota inicial do app
    val startDestination = Routes.SPLASH

    // Tema reativo aplicado uma única vez
    var themeType by remember {
        val initialNumber = SharedPreferencesManager.getThemeNumber(context, "chave")
        mutableStateOf(
            when (initialNumber) {
                2 -> BipandoThemeType.GREEN
                3 -> BipandoThemeType.RED
                4 -> BipandoThemeType.DARK
                else -> BipandoThemeType.CLASSIC
            }
        )
    }

    DisposableEffect(context) {
        val updateTheme = {
            val number = SharedPreferencesManager.getThemeNumber(context, "chave")
            themeType = when (number) {
                2 -> BipandoThemeType.GREEN
                3 -> BipandoThemeType.RED
                4 -> BipandoThemeType.DARK
                else -> BipandoThemeType.CLASSIC
            }
        }
        updateTheme()
        onDispose { }
    }

    BipandoTheme(themeType = themeType) {
        NavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
        ) {
            composable(Routes.SPLASH) {
                val viewModel: SplashViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return SplashViewModel(context) as T
                        }
                    }
                )

                SplashScreen(
                    viewModel = viewModel,
                    onNavigateToHome = {
                        val isTourDone = SharedPreferencesManager.isTourCompleted(context)
                        if (!isTourDone) {
                            navController.navigate(Routes.ONBOARDING) {
                                popUpTo(Routes.SPLASH) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.SPLASH) { inclusive = true }
                            }
                        }
                    },
                    onNavigateToLogin = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(Routes.SPLASH) { inclusive = true }
                        }
                    }
                )
            }
            composable(Routes.LOGIN) {
                val authRepository = AuthRepositoryImpl(FirebaseAuth.getInstance())
                val loginUseCase = LoginUseCase(authRepository)
                val homeRepository = DependencyInjector.registerHomeRepository(context)
                val categoryRepository = DependencyInjector.registerCategoryRepository(context)
                val groupRepository = DependencyInjector.registerGroupRepository(context)
                
                val viewModel: LoginViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return LoginViewModel(
                                loginUseCase, 
                                homeRepository, 
                                categoryRepository, 
                                groupRepository
                            ) as T
                        }
                    }
                )

                LoginScreen(
                    viewModel = viewModel,
                    onLoginSuccess = {
                        val isTourDone = SharedPreferencesManager.isTourCompleted(context)
                        if (!isTourDone) {
                            navController.navigate(Routes.ONBOARDING) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        } else {
                            navController.navigate(Routes.HOME) {
                                popUpTo(Routes.LOGIN) { inclusive = true }
                            }
                        }
                    }
                )
            }
            
            composable(
                route = "${Routes.HOME}?categoryId={categoryId}&categoryName={categoryName}",
                arguments = listOf(
                    navArgument("categoryId") { type = NavType.StringType; nullable = true; defaultValue = null },
                    navArgument("categoryName") { type = NavType.StringType; nullable = true; defaultValue = null }
                )
            ) { backStackEntry ->
                val categoryId = remember(backStackEntry) {
                    backStackEntry.savedStateHandle.remove<String>("selected_category_id")
                        ?: backStackEntry.arguments?.getString("categoryId")
                }
                val rawCategoryName = remember(backStackEntry) {
                    backStackEntry.savedStateHandle.remove<String>("selected_category_name")
                        ?: backStackEntry.arguments?.getString("categoryName")
                }
                val categoryName = remember(rawCategoryName) {
                    rawCategoryName?.let {
                        try {
                            java.net.URLDecoder.decode(it, "UTF-8")
                        } catch (_: Exception) {
                            it
                        }
                    }
                }

                val authRepository = AuthRepositoryImpl(FirebaseAuth.getInstance())
                val homeRepository = DependencyInjector.registerHomeRepository(context)
                val categoryRepository = DependencyInjector.registerCategoryRepository(context)
                val profileRepository = DependencyInjector.profileRepository()
                val groupRepository = DependencyInjector.registerGroupRepository(context)
                
                val viewModel: HomeViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return HomeViewModel(
                                context,
                                homeRepository, 
                                authRepository, 
                                categoryRepository, 
                                profileRepository,
                                groupRepository
                            ) as T
                        }
                    }
                )

                HomeScreen(
                    viewModel = viewModel,
                    navController = navController,
                    initialCategoryId = categoryId,
                    initialCategoryName = categoryName,
                    onProductClick = { product ->
                        navController.navigate("edit_product/${product.uuid}") { launchSingleTop = true }
                    },
                    onImageClick = { uri ->
                        val encodedUri = java.net.URLEncoder.encode(uri, "UTF-8")
                        navController.navigate("image_preview?uri=$encodedUri") { launchSingleTop = true }
                    },
                    onScannerSearch = {
                        navController.navigate("scanner/SEARCH") { launchSingleTop = true }
                    },
                    onScannerNavigate = { catId, catName ->
                        navController.navigate("scanner/$catId") { launchSingleTop = true }
                    },
                    onProfileClick = {
                        navController.navigate(Routes.PROFILE) { launchSingleTop = true }
                    },
                    onCategoryClick = {
                        navController.navigate(Routes.CATEGORY) { launchSingleTop = true }
                    },
                    onPaymentClick = { limitReached ->
                        navController.navigate("payment?limitReached=$limitReached") { launchSingleTop = true }
                    },
                    onLogout = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.CATEGORY) {
                val categoryRepository = DependencyInjector.registerCategoryRepository(context)

                val viewModel: CategoryViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return CategoryViewModel(context, categoryRepository) as T
                        }
                    }
                )

                CategoryScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.safePopBackStack() },
                    onCategoryClick = { category ->
                        navController.previousBackStackEntry?.savedStateHandle?.set("selected_category_id", category.id)
                        navController.previousBackStackEntry?.savedStateHandle?.set("selected_category_name", category.name)
                        navController.safePopBackStack()
                    }
                )
            }

            composable(Routes.SCANNER, arguments = listOf(navArgument("categoryId") { type = NavType.StringType })) { backStackEntry ->
                val categoryId = backStackEntry.arguments?.getString("categoryId")
                var handled by remember { mutableStateOf(false) }

                val isPremium = SharedPreferencesManager.isPremium(context)
                val (isLimitReached, setIsLimitReached) = remember { mutableStateOf(false) }
                
                LaunchedEffect(Unit) {
                    if (!isPremium) {
                        val count = database.productDao().getGlobalTotalProductsCount()
                        setIsLimitReached(count >= 100)
                    }
                }

                ScannerScreen(
                    onBarcodeScanned = { barcode ->
                        if (handled) return@ScannerScreen
                        handled = true
                        val encodedBarcode = java.net.URLEncoder.encode(barcode, "UTF-8")
                        if (categoryId == "SEARCH") {
                            navController.previousBackStackEntry?.savedStateHandle?.set("search_barcode", barcode)
                            navController.safePopBackStack()
                        } else {
                            navController.navigate("add_product/$encodedBarcode/$categoryId") {
                                popUpTo(Routes.SCANNER) { inclusive = true }
                            }
                        }
                    },
                    onBackClick = { navController.safePopBackStack() },
                    isLimitReached = isLimitReached,
                    isSearchMode = categoryId == "SEARCH"
                )
            }

            composable(
                route = Routes.ADD_PRODUCT,
                arguments = listOf(
                    navArgument("barcode") { type = NavType.StringType; nullable = true },
                    navArgument("categoryId") { type = NavType.StringType; nullable = true }
                )
            ) { backStackEntry ->
                val barcode = backStackEntry.arguments?.getString("barcode")
                val categoryId = backStackEntry.arguments?.getString("categoryId")
                
                val productRepository = ProductRepositoryImpl(
                    context,
                    database,
                    DependencyInjector.imageResolutionRepository()
                )
                val saveProductUseCase = SaveProductUseCase(productRepository)
                val getCategoriesUseCase = GetCategoriesUseCase(productRepository)
                val registerItemRepository = DependencyInjector.registerProductRepository(context)
                val categoryRepository = DependencyInjector.registerCategoryRepository(context)

                val viewModel: AddProductViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return AddProductViewModel(
                                context,
                                saveProductUseCase, 
                                getCategoriesUseCase,
                                registerItemRepository,
                                categoryRepository,
                                DependencyInjector.openFoodFactsRepository()
                            ) as T
                        }
                    }
                )

                AddProductScreen(
                    viewModel = viewModel,
                    barcode = barcode,
                    initialCategoryId = categoryId,
                    onBackClick = { navController.safePopBackStack() },
                    onSaveSuccess = {
                        navController.safePopBackStack()
                    },
                    onBarcodeClick = { code ->
                        val encodedCode = java.net.URLEncoder.encode(code, "UTF-8")
                        navController.navigate("image_preview?barcode=$encodedCode") { launchSingleTop = true }
                    }
                )
            }

            composable(Routes.GROUPS) {
                val authRepository = AuthRepositoryImpl(FirebaseAuth.getInstance())
                val groupRepository = DependencyInjector.registerGroupRepository(context)
                val productDao = database.productDao()
                val viewModel: GroupsViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return GroupsViewModel(authRepository, groupRepository, productDao) as T
                        }
                    }
                )

                GroupsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.safePopBackStack() }
                )
            }

            composable(Routes.PROFILE) {
                val profileRepository = DependencyInjector.profileRepository()
                val productDao = database.productDao()
                val homeRepository = DependencyInjector.registerHomeRepository(context)
                val categoryRepository = DependencyInjector.registerCategoryRepository(context)
                val authRepository = AuthRepositoryImpl(auth)
                val groupRepository = DependencyInjector.registerGroupRepository(context)

                val viewModel: ProfileViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return ProfileViewModel(
                                profileRepository, 
                                productDao, 
                                homeRepository,
                                categoryRepository,
                                authRepository,
                                groupRepository
                            ) as T
                        }
                    }
                )
                val state by viewModel.uiState.collectAsState()

                // Atualiza o tema reativo caso mude nas configurações do perfil
                LaunchedEffect(state.themeType) {
                    themeType = state.themeType
                }

                ProfileScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.safePopBackStack() },
                    onLogoutSuccess = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onTrashClick = {
                        navController.navigate(Routes.TRASH) { launchSingleTop = true }
                    },
                    onPaymentClick = { limitReached ->
                        navController.navigate("payment?limitReached=$limitReached") { launchSingleTop = true }
                    }
                )
            }

            composable(Routes.TRASH) {
                val productRepository = ProductRepositoryImpl(
                    context,
                    database,
                    DependencyInjector.imageResolutionRepository()
                )
                val getDeletedProductsUseCase = GetDeletedProductsUseCase(productRepository)
                val deleteItemRepository = DependencyInjector.itemDeletedRepository(context)
                val groupRepository = DependencyInjector.registerGroupRepository(context)

                val viewModel: TrashViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return TrashViewModel(
                                getDeletedProductsUseCase,
                                deleteItemRepository,
                                groupRepository
                            ) as T
                        }
                    }
                )

                TrashScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.safePopBackStack() }
                )
            }

            composable(
                route = Routes.PAYMENT,
                arguments = listOf(
                    navArgument("limitReached") {
                        type = NavType.BoolType
                        defaultValue = false
                    }
                )
            ) { backStackEntry ->
                val limitReached = backStackEntry.arguments?.getBoolean("limitReached") ?: false
                val profileRepository = DependencyInjector.profileRepository()
                val productDao = database.productDao()
                
                val viewModel: PaymentViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return PaymentViewModel(profileRepository, productDao) as T
                        }
                    }
                )
                PaymentScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.safePopBackStack() },
                    isLimitReached = limitReached
                )
            }

            composable(
                route = Routes.EDIT_PRODUCT,
                arguments = listOf(navArgument("uuid") { type = NavType.StringType })
            ) { backStackEntry ->
                val uuid = backStackEntry.arguments?.getString("uuid") ?: ""
                
                val productRepository = ProductRepositoryImpl(
                    context,
                    database,
                    DependencyInjector.imageResolutionRepository()
                )
                val getProductByUuidUseCase = GetProductByUuidUseCase(productRepository)
                val getCategoriesUseCase = GetCategoriesUseCase(productRepository)
                val saveProductUseCase = SaveProductUseCase(productRepository)
                val deleteProductUseCase = DeleteProductUseCase(productRepository)
                val groupRepository = DependencyInjector.registerGroupRepository(context)
                val categoryRepository = DependencyInjector.registerCategoryRepository(context)

                val viewModel: EditProductViewModel = viewModel(
                    factory = object : ViewModelProvider.Factory {
                        override fun <T : ViewModel> create(modelClass: Class<T>): T {
                            return EditProductViewModel(
                                context,
                                getProductByUuidUseCase,
                                getCategoriesUseCase,
                                saveProductUseCase,
                                deleteProductUseCase,
                                groupRepository,
                                categoryRepository
                            ) as T
                        }
                    }
                )

                EditProductScreen(
                    viewModel = viewModel,
                    productUuid = uuid,
                    onBackClick = { navController.safePopBackStack() },
                    onDeleteSuccess = {
                        navController.safePopBackStack()
                    },
                    onBarcodeClick = { barcode ->
                        val encodedBarcode = java.net.URLEncoder.encode(barcode, "UTF-8")
                        navController.navigate("image_preview?barcode=$encodedBarcode") { launchSingleTop = true }
                    }
                )
            }

            composable(
                route = Routes.IMAGE_PREVIEW,
                arguments = listOf(
                    navArgument("uri") { 
                        type = NavType.StringType
                        defaultValue = ""
                    },
                    navArgument("barcode") {
                        type = NavType.StringType
                        defaultValue = ""
                    }
                )
            ) { backStackEntry ->
                val uri = backStackEntry.arguments?.getString("uri") ?: ""
                val barcode = backStackEntry.arguments?.getString("barcode") ?: ""

                ImagePreviewScreen(
                    imageUri = uri,
                    barcode = barcode,
                    onBackClick = { navController.safePopBackStack() }
                )
            }

            composable(Routes.ONBOARDING) {
                OnboardingScreen(
                    onFinish = {
                        SharedPreferencesManager.setTourCompleted(context, true)
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.ONBOARDING) { inclusive = true }
                        }
                    }
                )
            }

            composable(Routes.REGISTER) { }
        }
    }
}
