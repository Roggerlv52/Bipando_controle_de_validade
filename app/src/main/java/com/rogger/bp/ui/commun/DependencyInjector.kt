package com.rogger.bp.ui.commun

import com.rogger.bp.ui.add.data.FireRegisterDataSource
import com.rogger.bp.ui.add.data.RegisterItemRepository
import com.rogger.bp.ui.add.data.RegisterRepository
import com.rogger.bp.ui.category.data.CategoryDataSource
import com.rogger.bp.ui.category.data.CategoryRepository

object DependencyInjector {
    fun registerProductRepository(): RegisterItemRepository {
        return RegisterRepository(FireRegisterDataSource())
    }
    fun registerCategoryRepository(): CategoryRepository {
        return CategoryRepository(CategoryDataSource())
    }
}