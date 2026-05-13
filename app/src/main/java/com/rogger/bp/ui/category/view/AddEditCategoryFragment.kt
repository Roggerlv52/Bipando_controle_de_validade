package com.rogger.bp.ui.category.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.rogger.bp.R
import com.rogger.bp.databinding.FragmentAddEditCategoryBinding
import com.rogger.bp.ui.category.ContractCategory
import com.rogger.bp.ui.category.presentation.CategoryPresenter
import com.rogger.bp.ui.commun.DependencyInjector

class AddEditCategoryFragment : Fragment(R.layout.fragment_add_edit_category),
    ContractCategory.View {
    private var _binding: FragmentAddEditCategoryBinding? = null
    private val binding get() = _binding!!

    override lateinit var presenter: ContractCategory.Presenter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        _binding = FragmentAddEditCategoryBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repository = DependencyInjector.registerCategoryRepository()
        presenter = CategoryPresenter(this,repository)

    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun showProgress(enable: Boolean) {

    }

    override fun onSuccess() {

    }

    override fun onFailure(message: String) {

    }

    override fun categoryExists(message: String) {

    }
}