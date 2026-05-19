package com.rogger.bp.ui.commun

import android.content.Context
import com.rogger.bp.data.database.BpdDatabase
import com.rogger.bp.data.database.RoomProductCache
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
    fun registerProductRepository(): RegisterItemRepository {
        return RegisterRepository(FireRegisterDataSource())
    }

    fun registerCategoryRepository(context: Context): CategoryRepository {
        val database = BpdDatabase.getDatabase(context)
        val categoryDao = database.categoryDao()
        val roomCategoryCache = RoomCategoryCache(categoryDao)
        val categoryDataSource = CategoryDataSource()
        return CategoryRepository(categoryDataSource, roomCategoryCache)
    }

    fun registerHomeRepository(context: Context): HomeRepository {
        val database = BpdDatabase.getDatabase(context)
        val productDao = database.productDao()
        val roomProductCache = RoomProductCache(productDao)
        val homeDataSource = HomeDataSource()
        return HomeRepository(homeDataSource, roomProductCache)
    }

    fun registerEditRepository(): EditRepository {
        return EditRepository(EditDataSource())
    }

    fun loginRepository() : LoginRepository{
        return LoginRepository(FireDataSource())
    }

    fun itemDeletedRepository(): DeleteItemRepository{
        return DeleteItemRepository(DeleteItemDataSource())
    }
}