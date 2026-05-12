package com.rogger.bp.ui.commun

import com.rogger.bp.ui.add.data.FireRegisterDataSource
import com.rogger.bp.ui.add.data.RegisterItemRepository.RegisterRepository

object DependencyInjector {
    fun registerProductRepository(): RegisterRepository {
        return RegisterRepository(FireRegisterDataSource())
    }
}