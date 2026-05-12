package com.rogger.bp.ui.add.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.rogger.bp.R
import com.rogger.bp.databinding.FragmentAddBinding
import com.rogger.bp.ui.add.RegisterAdd
import com.rogger.bp.ui.add.presentation.AddItemPresenter
import com.rogger.bp.ui.commun.DependencyInjector

class AddItemFragment : Fragment(R.layout.fragment_add), RegisterAdd.View {
    private var binding: FragmentAddBinding? = null


    override lateinit var presenter: RegisterAdd.Presenter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return super.onCreateView(inflater, container, savedInstanceState)
        binding = view?.let { FragmentAddBinding.bind(it) }

        val repositer = DependencyInjector.registerProductRepository()
        presenter = AddItemPresenter(this, repositer)

    }

    override fun showProgress(enable: Boolean) {
        if (enable) {
            binding?.progressUploadAdd?.visibility = View.VISIBLE
        } else {
            binding?.progressUploadAdd?.visibility = View.GONE
        }
    }

    override fun onFailure(message: String) {
    }

    override fun onSave() {
        presenter.create()
    }

    override fun goToGallery() {

    }

    override fun openCamera() {
    }

    override fun onDestroy() {
        binding = null
        presenter.onDestroy()
        super.onDestroy()
    }
}



