package com.rogger.bp.ui.commun

import android.content.Context
import com.rogger.bp.data.database.BpDatabase
import com.rogger.bp.data.database.RoomProductCache
import com.rogger.bp.data.image.datasource.GlobalImageDataSource
import com.rogger.bp.data.image.datasource.UserImageDataSource
import com.rogger.bp.data.image.repository.ImageResolutionRepository
import com.rogger.bp.ui.add.data.FireRegisterDataSource
import com.rogger.bp.ui.add.data.RegisterItemRepository
import com.rogger.bp.ui.add.data.RegisterRepository
import com.rogger.bp.ui.category.data.CategoryDataSource
import com.rogger.bp.ui.category.data.CategoryRepository
import com.rogger.bp.ui.category.data.RoomCategoryCache
import com.rogger.bp.ui.deleteitem.data.DeleteItemDataSource
import com.rogger.bp.ui.deleteitem.data.DeleteItemRepository
import com.rogger.bp.ui.edit.data.EditDataSource
import com.rogger.bp.ui.edit.data.EditRepository
import com.rogger.bp.ui.home.data.HomeDataSource
import com.rogger.bp.ui.home.data.HomeRepository
import com.rogger.bp.ui.login.data.FireDataSource
import com.rogger.bp.ui.login.data.LoginRepository

object DependencyInjector {

    // ── Repositórios existentes (sem alteração de interface) ──────────────

    fun registerProductRepository(context: Context): RegisterItemRepository {
        val database = BpDatabase.getDatabase(context)
        val productDao = database.productDao()
        val roomProductCache = RoomProductCache(productDao)
        return RegisterItemRepository(FireRegisterDataSource(), roomProductCache)
    }

    fun registerCategoryRepository(context: Context): CategoryRepository {
        val database = BpDatabase.getDatabase(context)
        val categoryDao = database.categoryDao()
        val productDao = database.productDao() // 👉 Adicionado para buscar contagem
        val roomCategoryCache = RoomCategoryCache(categoryDao)
        val categoryDataSource = CategoryDataSource()
        return CategoryRepository(categoryDataSource, roomCategoryCache,productDao)
    }

    fun registerHomeRepository(context: Context): HomeRepository {
        val database = BpDatabase.getDatabase(context)
        val productDao = database.productDao()
        val roomProductCache = RoomProductCache(productDao)
        val homeDataSource = HomeDataSource()
        return HomeRepository(homeDataSource, roomProductCache)
    }

    fun registerEditRepository(context: Context): EditRepository {
        val database = BpDatabase.getDatabase(context)
        val productDao = database.productDao()
        val roomProductCache = RoomProductCache(productDao)
        return EditRepository(EditDataSource(), roomProductCache)
    }

    fun loginRepository(): LoginRepository {
        return LoginRepository(FireDataSource())
    }

    fun itemDeletedRepository(context: Context): DeleteItemRepository {

        val database = BpDatabase.getDatabase(context)
        val productDao = database.productDao()
        val roomProductCache = RoomProductCache(productDao)
        return DeleteItemRepository(DeleteItemDataSource(), roomProductCache)

    }

    // ── Novos repositórios de imagem ──────────────────────────────────────

    /**
     * Repositório central de resolução de imagem.
     *
     * Encapsula a lógica de fallback:
     *  personalizada → global → sem imagem
     *
     * Use quando precisar de controlo manual sobre o fluxo de imagem,
     * por exemplo numa futura tela de "gerenciar imagens".
     */
    fun imageResolutionRepository(): ImageResolutionRepository {
        return ImageResolutionRepository(
            userImageDataSource   = UserImageDataSource(),
            globalImageDataSource = GlobalImageDataSource()
        )
    }

    /**
     * DataSource para imagens personalizadas do utilizador.
     * Use diretamente apenas se precisar de operações isoladas (ex: remover imagem personalizada).
     */
    fun userImageDataSource(): UserImageDataSource {
        return UserImageDataSource()
    }

    /**
     * DataSource para imagens globais de produto.
     * Use diretamente apenas se precisar de leitura isolada da imagem global.
     */
    fun globalImageDataSource(): GlobalImageDataSource {
        return GlobalImageDataSource()
    }
}
