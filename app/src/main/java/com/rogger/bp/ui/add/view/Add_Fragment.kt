package com.rogger.bp.ui.add.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.rogger.bp.R
import com.rogger.bp.databinding.FragmentAddBinding
import com.rogger.bp.ui.add.RegisterAdd
import com.rogger.bp.ui.add.presentation.AddItemPresenter
import com.rogger.bp.ui.commun.DependencyInjector

class AddItemFragment : Fragment(R.layout.fragment_add), RegisterAdd.View {
    
    private var _binding: FragmentAddBinding? = null
    private val binding get() = _binding!!

    override lateinit var presenter: RegisterAdd.Presenter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentAddBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        
        val repository = DependencyInjector.registerProductRepository()
        presenter = AddItemPresenter(this, repository)
        
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnSaveAdd.setOnClickListener {
            onSave()
        }
        
        // Adicione outros listeners conforme necessário (câmera, galeria, etc)
    }

    override fun showProgress(enable: Boolean) {
        binding.progressUploadAdd.visibility = if (enable) View.VISIBLE else View.GONE
        binding.btnSaveAdd.isEnabled = !enable
    }

    override fun onFailure(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onSave() {
        // Exemplo de como chamar o presenter (ajuste com os dados reais dos inputs)
        // val product = PostProduct(...)
        // presenter.create(product)
        
        // Se estiver salvando imagem separadamente para verificar existência:
        // val image = PostImage(barcode = binding.editBarcodeAdd.text.toString(), ...)
        // presenter.createImage(image)
    }

    override fun goToGallery() {
        // Lógica para abrir galeria
    }

    override fun openCamera() {
        // Lógica para abrir câmera
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        if (::presenter.isInitialized) {
            presenter.onDestroy()
        }
        super.onDestroy()
    }
}
