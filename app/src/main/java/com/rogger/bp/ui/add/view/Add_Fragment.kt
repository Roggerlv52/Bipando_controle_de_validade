package com.rogger.bp.ui.add.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.bumptech.glide.Glide
import com.rogger.bp.R
import com.rogger.bp.data.model.PostImage
import com.rogger.bp.databinding.FragmentAddBinding
import com.rogger.bp.ui.add.RegisterAdd
import com.rogger.bp.ui.add.presentation.AddItemPresenter
import com.rogger.bp.ui.commun.DependencyInjector

class AddItemFragment : Fragment(R.layout.fragment_add), RegisterAdd.View {
    private var barcode: String = ""
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

        val args = arguments

        if (args != null) {

            barcode = args.getString("key_barcode").toString()
            var categoriaId = args.getInt("categoria_id", -1)

            if (barcode.isNotEmpty()) {
                binding.txtAddBarcode.text = barcode
                val image = PostImage(barcode = barcode)
                presenter.createImage(image)
            }

        }
        setupListeners()
    }

    private fun setupListeners() {
        binding.btnFgmSaveAdd.setOnClickListener {
            onSave()
        }

        // Adicione outros listeners conforme necessário (câmera, galeria, etc)
    }

    override fun showProgress(enable: Boolean) {
        binding.progressUploadAdd.visibility = if (enable) View.VISIBLE else View.GONE
        binding.btnFgmSaveAdd.isEnabled = !enable
    }

    override fun imageExit(postImage: PostImage) {
        Glide.with(requireContext())
            .load(postImage.uri)
            .override(500, 500)
            .placeholder(R.drawable.carregando)
            .error(R.drawable.imagem_error)
            .centerCrop()
            .into(binding.fragmentImgAdd)
    }

    override fun onFailure(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun onSave() {
        // Exemplo de como chamar o presenter (ajuste com os dados reais dos inputs)
        // val product = PostProduct(...)
        // presenter.create(product)

        // Se estiver salvando imagem separadamente para verificar existência:
        val image = PostImage(barcode = barcode)
        presenter.createImage(image)
    }

    override fun goToHome() {
        // Lógica para navegar para Home
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
