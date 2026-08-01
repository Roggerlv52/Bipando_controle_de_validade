package com.rogger.bp.ui.naviation

import androidx.compose.runtime.Composable
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
import com.rogger.bp.ui.profile.view.ProfileScreen
import com.rogger.bp.ui.edit.presentation.EditProductViewModel
import com.rogger.bp.ui.edit.view.EditProductScreen
import com.rogger.bp.ui.deleteitem.presentation.TrashViewModel
import com.rogger.bp.ui.deleteitem.view.TrashScreen
import com.rogger.bp.domain.usecase.*
import androidx.compose.ui.platform.LocalContext

import com.rogger.bp.domain.usecase.SaveCategoryUseCase

import com.rogger.bp.domain.usecase.DeleteCategoryUseCase

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

@Composable
fun BipandoNavGraph(navController: NavHostController) {
    val context = LocalContext.current
    val database = BpDatabase.getDatabase(context)
    val auth = FirebaseAuth.getInstance()
    
    // Persistência de login: Se o usuário já está logado no Firebase, vai direto para HOME
    val startDestination = if (auth.currentUser != null) Routes.HOME else Routes.LOGIN

    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable(Routes.LOGIN) {
            val authRepository = AuthRepositoryImpl(FirebaseAuth.getInstance())
            val loginUseCase = LoginUseCase(authRepository)
            
            val viewModel: LoginViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return LoginViewModel(loginUseCase) as T
                    }
                }
            )

            LoginScreen(
                viewModel = viewModel,
                onLoginSuccess = {
                    navController.navigate(Routes.HOME) {
                        popUpTo(Routes.LOGIN) { inclusive = true }
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
            val categoryId = backStackEntry.arguments?.getString("categoryId")
            val categoryName = backStackEntry.arguments?.getString("categoryName")

            val authRepository = AuthRepositoryImpl(FirebaseAuth.getInstance())
            val homeRepository = DependencyInjector.registerHomeRepository(context)
            val categoryRepository = DependencyInjector.registerCategoryRepository(context)
            
            val viewModel: HomeViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return HomeViewModel(homeRepository, authRepository, categoryRepository) as T
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
                    initialCategoryId = categoryId,
                    initialCategoryName = categoryName,
                    onProductClick = { product ->
                        navController.navigate("edit_product/${product.uuid}")
                    },
                    onImageClick = { uri ->
                        val encodedUri = java.net.URLEncoder.encode(uri, "UTF-8")
                        navController.navigate("image_preview?uri=$encodedUri")
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
                    onTrashClick = {
                        navController.navigate(Routes.TRASH)
                    },
                    onPaymentClick = {
                        navController.navigate(Routes.PAYMENT)
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
                        return CategoryViewModel(categoryRepository) as T
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
                        val encodedName = java.net.URLEncoder.encode(category.name, "UTF-8")
                        navController.navigate("${Routes.HOME}?categoryId=${category.id}&categoryName=$encodedName") {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
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

            BipandoTheme(themeType = themeType) {
                ScannerScreen(
                    onBarcodeScanned = { barcode ->
                        navController.navigate("add_product/$barcode/$categoryId") {
                            popUpTo(Routes.SCANNER) { inclusive = true }
                        }
                    },
                    onBackClick = { navController.popBackStack() }
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
            
            val productRepository = ProductRepositoryImpl(database)
            val saveProductUseCase = SaveProductUseCase(productRepository)
            val getCategoriesUseCase = GetCategoriesUseCase(productRepository)

            val viewModel: AddProductViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return AddProductViewModel(saveProductUseCase, getCategoriesUseCase) as T
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
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(Routes.PROFILE) {
            val viewModel: ProfileViewModel = viewModel()
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
                    onPaymentClick = {
                        navController.navigate(Routes.PAYMENT)
                    }
                )
            }
        }

        composable(Routes.TRASH) {
            val productRepository = ProductRepositoryImpl(database)
            val getDeletedProductsUseCase = GetDeletedProductsUseCase(productRepository)
            val deleteItemRepository = DependencyInjector.itemDeletedRepository(context)

            val viewModel: TrashViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return TrashViewModel(
                            getDeletedProductsUseCase,
                            deleteItemRepository
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

        composable(Routes.PAYMENT) {
            val viewModel: PaymentViewModel = viewModel()
            PaymentScreen(
                viewModel = viewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable(
            route = Routes.EDIT_PRODUCT,
            arguments = listOf(navArgument("uuid") { type = NavType.StringType })
        ) { backStackEntry ->
            val uuid = backStackEntry.arguments?.getString("uuid") ?: ""
            
            val productRepository = ProductRepositoryImpl(database)
            val getProductByUuidUseCase = GetProductByUuidUseCase(productRepository)
            val getCategoriesUseCase = GetCategoriesUseCase(productRepository)
            val saveProductUseCase = SaveProductUseCase(productRepository)
            val deleteProductUseCase = DeleteProductUseCase(productRepository)

            val viewModel: EditProductViewModel = viewModel(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        return EditProductViewModel(
                            getProductByUuidUseCase,
                            getCategoriesUseCase,
                            saveProductUseCase,
                            deleteProductUseCase
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
                        navController.navigate(Routes.HOME) {
                            popUpTo(Routes.HOME) { inclusive = true }
                        }
                    }
                )
            }
        }

        composable(
            route = Routes.IMAGE_PREVIEW,
            arguments = listOf(navArgument("uri") { 
                type = NavType.StringType
                defaultValue = ""
            })
        ) { backStackEntry ->
            val uri = backStackEntry.arguments?.getString("uri") ?: ""
            // Navigation already decodes query parameters, so we don't need manual decode here.
            // If the URI was double-encoded, we might need one decode. 
            // But usually one encode for the route is enough.
            val decodedUri = uri
            
            val themeNumber = SharedPreferencesManager.getThemeNumber(context, "chave")
            val themeType = when (themeNumber) {
                2 -> BipandoThemeType.GREEN
                3 -> BipandoThemeType.RED
                4 -> BipandoThemeType.DARK
                else -> BipandoThemeType.CLASSIC
            }

            BipandoTheme(themeType = themeType) {
                ImagePreviewScreen(
                    imageUri = decodedUri,
                    onBackClick = { navController.popBackStack() }
                )
            }
        }

        composable(Routes.REGISTER) { }
    }
}
