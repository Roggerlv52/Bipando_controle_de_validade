package com.rogger.bp.ui.naviation

import com.rogger.bp.ui.groups.presentation.GroupsViewModel
import com.rogger.bp.ui.groups.view.GroupsScreen
import com.rogger.bp.ui.profile.view.ProfileScreen
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.google.firebase.auth.FirebaseAuth
import com.rogger.bp.data.database.BpDatabase
import com.rogger.bp.data.repository.AuthRepositoryImpl
import com.rogger.bp.data.repository.ProductRepositoryImpl
import com.rogger.bp.domain.usecase.LoginUseCase
import com.rogger.bp.ui.login.presentation.LoginViewModel
import com.rogger.bp.ui.login.view.LoginScreen
import com.rogger.bp.ui.home.presentation.HomeViewModel
import com.rogger.bp.ui.home.view.HomeScreen
import com.rogger.bp.ui.category.presentation.CategoryViewModel
import com.rogger.bp.ui.category.view.CategoryScreen
import com.rogger.bp.ui.scanner.ScannerScreen
import com.rogger.bp.ui.profile.presentation.ProfileViewModel
import com.rogger.bp.ui.edit.presentation.EditProductViewModel
import com.rogger.bp.ui.edit.view.EditProductScreen
import com.rogger.bp.ui.deleteitem.presentation.TrashViewModel
import com.rogger.bp.ui.deleteitem.view.TrashScreen
import com.rogger.bp.domain.usecase.*
import androidx.compose.ui.platform.LocalContext

import com.rogger.bp.domain.usecase.SaveProductUseCase
import com.rogger.bp.ui.add.presentation.AddProductViewModel
import com.rogger.bp.ui.add.view.AddProductScreen
import com.rogger.bp.ui.home.view.ImagePreviewScreen
import androidx.navigation.navArgument
import androidx.navigation.NavType

import com.rogger.bp.ui.payment.presentation.PaymentViewModel
import com.rogger.bp.ui.payment.view.PaymentScreen

import com.rogger.bp.ui.commun.DependencyInjector
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import com.rogger.bp.ui.theme.BipandoTheme
import com.rogger.bp.ui.theme.BipandoThemeType
import com.rogger.bp.ui.commun.SharedPreferencesManager
import com.rogger.bp.ui.tour.OnboardingScreen

@Composable
fun BipandoNavGraph(navController: NavHostController) {
    val context = LocalContext.current
    val database = BpDatabase.getDatabase(context)
    val auth = FirebaseAuth.getInstance()
    
    val tourCompleted = SharedPreferencesManager.isTourCompleted(context)

    // Persistência de login e verificação de Onboarding
    val startDestination = if (auth.currentUser != null) {
        if (!tourCompleted) Routes.ONBOARDING else Routes.HOME
    } else {
        Routes.LOGIN
    }

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
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
            val categoryId = backStackEntry.savedStateHandle.remove<String>("selected_category_id")
                ?: backStackEntry.arguments?.getString("categoryId")
            val rawCategoryName = backStackEntry.savedStateHandle.remove<String>("selected_category_name")
                ?: backStackEntry.arguments?.getString("categoryName")
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

            val themeNumber = SharedPreferencesManager.getThemeNumber(context, "chave")
            val themeType = when (themeNumber) {
                2 -> BipandoThemeType.GREEN
                3 -> BipandoThemeType.RED
                4 -> BipandoThemeType.DARK
                else -> BipandoThemeType.CLASSIC
            }

            BipandoTheme(themeType = themeType) {
                HomeScreen(
                    viewModel = viewModel,
                    navController = navController,
                    initialCategoryId = categoryId,
                    initialCategoryName = categoryName,
                    onProductClick = { product ->
                        navController.navigate("edit_product/${product.uuid}")
                    },
                    onImageClick = { uri ->
                        val encodedUri = java.net.URLEncoder.encode(uri, "UTF-8")
                        navController.navigate("image_preview?uri=$encodedUri")
                    },
                    onScannerSearch = {
                        navController.navigate("scanner/SEARCH")
                    },
                    onScannerNavigate = { categoryId, categoryName ->
                        navController.navigate("scanner/$categoryId")
                    },
                    onProfileClick = {
                        navController.navigate(Routes.PROFILE)
                    },
                    onCategoryClick = {
                        navController.navigate(Routes.CATEGORY)
                    },
                    onPaymentClick = { limitReached ->
                        navController.navigate("payment?limitReached=$limitReached")
                    },
                    onLogout = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    }
                )
            }
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

            val themeNumber = SharedPreferencesManager.getThemeNumber(context, "chave")
            val themeType = when (themeNumber) {
                2 -> BipandoThemeType.GREEN
                3 -> BipandoThemeType.RED
                4 -> BipandoThemeType.DARK
                else -> BipandoThemeType.CLASSIC
            }

            BipandoTheme(themeType = themeType) {
                CategoryScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onCategoryClick = { category ->
                        navController.previousBackStackEntry?.savedStateHandle?.set("selected_category_id", category.id)
                        navController.previousBackStackEntry?.savedStateHandle?.set("selected_category_name", category.name)
                        navController.popBackStack()
                    }
                )
            }
        }

        composable(Routes.SCANNER, arguments = listOf(navArgument("categoryId") { type = NavType.StringType })) { backStackEntry ->
            val categoryId = backStackEntry.arguments?.getString("categoryId")
            
            val themeNumber = SharedPreferencesManager.getThemeNumber(context, "chave")
            val themeType = when (themeNumber) {
                2 -> BipandoThemeType.GREEN
                3 -> BipandoThemeType.RED
                4 -> BipandoThemeType.DARK
                else -> BipandoThemeType.CLASSIC
            }

            // Verifica limite global para o scanner (apenas se for para ADD)
            val isPremium = SharedPreferencesManager.isPremium(context)
            val (isLimitReached, setIsLimitReached) = remember { mutableStateOf(false) }
            
            LaunchedEffect(Unit) {
                if (!isPremium) {
                    val count = database.productDao().getGlobalTotalProductsCount()
                    setIsLimitReached(count >= 100)
                }
            }

            BipandoTheme(themeType = themeType) {
                ScannerScreen(
                    onBarcodeScanned = { barcode ->
                        val encodedBarcode = java.net.URLEncoder.encode(barcode, "UTF-8")
                        if (categoryId == "SEARCH") {
                            // Envia o barcode de volta através do savedStateHandle
                            navController.previousBackStackEntry?.savedStateHandle?.set("search_barcode", barcode)
                            navController.popBackStack()
                        } else {
                            navController.navigate("add_product/$encodedBarcode/$categoryId") {
                                popUpTo(Routes.SCANNER) { inclusive = true }
                            }
                        }
                    },
                    onBackClick = { navController.popBackStack() },
                    isLimitReached = isLimitReached,
                    isSearchMode = categoryId == "SEARCH"
                )
            }
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

            val themeNumber = SharedPreferencesManager.getThemeNumber(context, "chave")
            val themeType = when (themeNumber) {
                2 -> BipandoThemeType.GREEN
                3 -> BipandoThemeType.RED
                4 -> BipandoThemeType.DARK
                else -> BipandoThemeType.CLASSIC
            }

            BipandoTheme(themeType = themeType) {
                AddProductScreen(
                    viewModel = viewModel,
                    barcode = barcode,
                    initialCategoryId = categoryId,
                    onBackClick = { navController.popBackStack() },
                    onSaveSuccess = {
                        navController.popBackStack()
                    },
                    onBarcodeClick = { code ->
                        val encodedCode = java.net.URLEncoder.encode(code, "UTF-8")
                        navController.navigate("image_preview?barcode=$encodedCode")
                    }
                )
            }
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

            val themeNumber = SharedPreferencesManager.getThemeNumber(context, "chave")
            val themeType = when (themeNumber) {
                2 -> BipandoThemeType.GREEN
                3 -> BipandoThemeType.RED
                4 -> BipandoThemeType.DARK
                else -> BipandoThemeType.CLASSIC
            }

            BipandoTheme(themeType = themeType) {
                GroupsScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
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

            BipandoTheme(themeType = state.themeType) {
                ProfileScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() },
                    onLogoutSuccess = {
                        navController.navigate(Routes.LOGIN) {
                            popUpTo(0) { inclusive = true }
                        }
                    },
                    onTrashClick = {
                        navController.navigate(Routes.TRASH)
                    },
                    onPaymentClick = { limitReached ->
                        navController.navigate("payment?limitReached=$limitReached")
                    }
                )
            }
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

            val themeNumber = SharedPreferencesManager.getThemeNumber(context, "chave")
            val themeType = when (themeNumber) {
                2 -> BipandoThemeType.GREEN
                3 -> BipandoThemeType.RED
                4 -> BipandoThemeType.DARK
                else -> BipandoThemeType.CLASSIC
            }

            BipandoTheme(themeType = themeType) {
                TrashScreen(
                    viewModel = viewModel,
                    onBackClick = { navController.popBackStack() }
                )
            }
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
                onBackClick = { navController.popBackStack() },
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

            val themeNumber = SharedPreferencesManager.getThemeNumber(context, "chave")
            val themeType = when (themeNumber) {
                2 -> BipandoThemeType.GREEN
                3 -> BipandoThemeType.RED
                4 -> BipandoThemeType.DARK
                else -> BipandoThemeType.CLASSIC
            }

            BipandoTheme(themeType = themeType) {
                EditProductScreen(
                    viewModel = viewModel,
                    productUuid = uuid,
                    onBackClick = { navController.popBackStack() },
                    onDeleteSuccess = {
                        navController.popBackStack()
                    },
                    onBarcodeClick = { barcode ->
                        val encodedBarcode = java.net.URLEncoder.encode(barcode, "UTF-8")
                        navController.navigate("image_preview?barcode=$encodedBarcode")
                    }
                )
            }
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
            
            val themeNumber = SharedPreferencesManager.getThemeNumber(context, "chave")
            val themeType = when (themeNumber) {
                2 -> BipandoThemeType.GREEN
                3 -> BipandoThemeType.RED
                4 -> BipandoThemeType.DARK
                else -> BipandoThemeType.CLASSIC
            }

            BipandoTheme(themeType = themeType) {
                ImagePreviewScreen(
                    imageUri = uri,
                    barcode = barcode,
                    onBackClick = { navController.popBackStack() }
                )
            }
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
