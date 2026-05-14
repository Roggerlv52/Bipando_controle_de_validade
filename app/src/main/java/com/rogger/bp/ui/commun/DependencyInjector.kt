package com.rogger.bp.ui.commun

import com.rogger.bp.ui.add.data.FireRegisterDataSource
import com.rogger.bp.ui.add.data.RegisterItemRepository
import com.rogger.bp.ui.add.data.RegisterRepository
import com.rogger.bp.ui.category.data.CategoryDataSource
import com.rogger.bp.ui.category.data.CategoryRepository
import com.rogger.bp.ui.edit.data.EditDataSource
import com.rogger.bp.ui.edit.data.EditRepository
import com.rogger.bp.ui.login.data.FireDataSource
import com.rogger.bp.ui.login.data.LoginRepository

object DependencyInjector {
    fun registerProductRepository(): RegisterItemRepository {
        return RegisterRepository(FireRegisterDataSource())
    }
    fun registerCategoryRepository(): CategoryRepository {
        return CategoryRepository(CategoryDataSource())
    }
    fun registerEditRepository(): EditRepository {
        return EditRepository(EditDataSource())
    }
    fun loginRepository() : LoginRepository{
        return LoginRepository(FireDataSource())
    }
}