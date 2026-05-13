package com.rogger.bp.ui.category.presentation

import com.rogger.bp.data.model.PostCategory
import com.rogger.bp.ui.category.ContractCategory
import com.rogger.bp.ui.category.data.CategoryRepository

class CategoryPresenter(
    private var view: ContractCategory.View?,
    private val repository: CategoryRepository,
) : ContractCategory.Presenter {

    override fun create(postCategory: PostCategory) {

    }

    override fun upload(postCategory: PostCategory) {

    }

    override fun delete(postCategory: PostCategory) {

    }

    override fun onDestroy() {

    }

}