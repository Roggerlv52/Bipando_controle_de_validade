package com.rogger.bp.ui.add.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.Toast
import androidx.activity.result.ActivityResultLauncher
import androidx.fragment.app.Fragment
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.rogger.bp.R
import com.rogger.bp.data.model.PostCategory
import com.rogger.bp.data.model.PostImage
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.data.model.UserAuth
import com.rogger.bp.databinding.FragmentAddBinding
import com.rogger.bp.ui.add.RegisterAdd
import com.rogger.bp.ui.add.presentation.AddItemPresenter
import com.rogger.bp.ui.animation.ToastCustom
import com.rogger.bp.ui.base.MenuUtil
import com.rogger.bp.ui.base.Utils
import com.rogger.bp.ui.commun.DependencyInjector
import com.rogger.bp.ui.commun.ShowSelectDialog
import com.rogger.bp.ui.gallery.CameraCallback
import com.rogger.bp.ui.gallery.ImagePikerUtil
import com.rogger.bp.ui.gallery.ImageUtils
import com.rogger.bp.ui.home.CustomProgressBar
import com.rogger.bp.ui.scanner.ImageBarcode
import java.io.File

class AddItemFragment : Fragment(R.layout.fragment_add), RegisterAdd.View {

    companion object {
        const val KEY_BARCODE   = "key_barcode"
        const val KEY_CATEGORIA = "categoria_id"
    }
    override lateinit var presenter: RegisterAdd.Presenter

    private var _binding: FragmentAddBinding? = null
    private val binding get() = _binding!!

    private var listaCategorias: List<PostCategory> = emptyList()

    private var produto: PostProduct = PostProduct()

    private var spinnerPronto = false
    private var datePickerOpened = false
    private var barcode: String = ""
    private var timestamp: Long = 0L
    private var photoFile: File? = null
    private var remoteImageUri: String? = null
    private var confirmed: Boolean = false

    private val cameraUtil = ImagePikerUtil()
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>

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
        MenuUtil.clearMenu(this);

        val repository         = DependencyInjector.registerProductRepository(requireContext())
        val categoryRepository = DependencyInjector.registerCategoryRepository(requireContext())
        presenter = AddItemPresenter(this, repository, categoryRepository)

        val dataAtual = Utils.getCurrentDateFormatted()
        binding.datePickerBtnAdd.text = dataAtual
        binding.txtAddData.text       = dataAtual
        timestamp = Utils.getCurrentTimestamp()

        setupCamera()
        setupGalleryResult()
        setupSpinner()
        setupListeners()

        readArguments()

        presenter.fetchCategories()

        if (!datePickerOpened) {
            datePickerOpened = true
            binding.datePickerBtnAdd.performClick()
        }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (!confirmed && photoFile != null && remoteImageUri == null) {
            ImagePikerUtil.cleanUpTempFiles(photoFile)
        }
        _binding = null
    }

    override fun onDestroy() {
        if (::presenter.isInitialized) presenter.onDestroy()
        super.onDestroy()
    }

    private fun setupCamera() {
        cameraLauncher = cameraUtil.register(this, object : CameraCallback {
            override fun onImageCaptured(imageUri: Uri, imageFile: File) {
                try {
                    photoFile      = ImageUtils.processImage(requireContext(), imageUri, imageFile)
                    remoteImageUri = null
                    showLocalImage(photoFile!!)
                } catch (e: Exception) {
                    onFailure(e.message ?: "Erro ao processar imagem")
                }
            }
            override fun onCancel() {}
            override fun onError(e: Exception) { onFailure(e.message ?: "Erro na câmera") }
        })
    }

    private fun setupGalleryResult() {
        parentFragmentManager.setFragmentResultListener("gallery_result", viewLifecycleOwner) { _, bundle ->
            try {
                val img  = Uri.parse(bundle.getString("imageUri"))
                val file = ImagePikerUtil.createImageFile(requireContext())
                photoFile      = ImageUtils.processImage(requireContext(), img, file)
                remoteImageUri = null
                showLocalImage(photoFile!!)
            } catch (e: Exception) {
                onFailure(e.message ?: "Erro ao processar imagem da galeria")
            }
        }
    }

    private fun setupSpinner() {
        binding.spinnerAdd.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>?, view: View?, position: Int, id: Long
            ) {
                if (!spinnerPronto) return
                // posição 0 = placeholder "Selecione uma categoria"
                val cat = listaCategorias.getOrNull(position - 1)
                produto = if (cat != null) {
                    produto.copy(categoryId = cat.firestoreId, categoryName = cat.name)
                } else {
                    produto.copy(categoryId = "", categoryName = "")
                }
                Log.d("AddItemFragment", "Spinner selecionou: id=${produto.categoryId} name=${produto.categoryName}")
            }
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }

    // ── Listeners gerais ──────────────────────────────────────────────────

    private fun setupListeners() {
        binding.btnFgmSaveAdd.setOnClickListener { saveProduct() }

        binding.datePickerBtnAdd.setOnClickListener {
            Utils.showDatePicker(requireContext()) { ts, dataFormatada ->
                binding.datePickerBtnAdd.text = dataFormatada
                binding.txtAddData.text       = dataFormatada
                timestamp = ts
            }
        }

        binding.fragmentImgAdd.setOnClickListener {
            if (remoteImageUri != null) return@setOnClickListener
            openMediaPicker()
        }
    }

    private fun readArguments() {
        val args = arguments ?: return

        barcode = args.getString(KEY_BARCODE).orEmpty()

        val argCategoryId   = args.getString(KEY_CATEGORIA).orEmpty()
        val argCategoryName = args.getString("nameCategory").orEmpty()
        if (argCategoryId.isNotEmpty()) {
            produto = produto.copy(categoryId = argCategoryId, categoryName = argCategoryName)
            Log.d("AddItemFragment", "Categoria recebida por arg: id=$argCategoryId name=$argCategoryName")
        }

        if (barcode.isNotEmpty()) {
            binding.txtAddBarcode.text = barcode
            presenter.checkOrCreateImage(barcode)
        }
    }

    private fun showLocalImage(file: File) {
        Glide.with(requireContext())
            .load(file)
            .override(500, 500)
            .centerCrop()
            .into(binding.fragmentImgAdd)
    }

    private fun showRemoteImage(url: String) {
        Glide.with(requireContext())
            .load(url)
            .override(500, 500)
            .centerCrop()
            .into(binding.fragmentImgAdd)
        binding.fragmentImgAdd.isClickable = false
    }

    private fun openMediaPicker() {
        ShowSelectDialog.show(requireContext(), object : ShowSelectDialog.selectedCallback {
            override fun openGallery() { this@AddItemFragment.openGallery() }
            override fun openCamera()  { this@AddItemFragment.openCamera() }
        })
    }

    private fun saveProduct() {
        val nome = binding.edtNameFragmentAdd.text.toString().trim()
        if (!Utils.validEditText(binding.edtNameFragmentAdd)) return

        // 👉 Permite salvar sem foto: se photoFile e remoteImageUri forem nulos, a URI será vazia ("")
        val imageUri: Uri = when {
            remoteImageUri != null -> Uri.parse(remoteImageUri)
            photoFile != null      -> Uri.fromFile(photoFile)
            else                   -> Uri.EMPTY
        }

        val productToSave = produto.copy(
            imageUri  = imageUri.toString(), // Ficará salvo como "" (vazio) caso o usuário não use imagem
            name      = nome,
            note      = binding.editFragmentNoteAdd.text.toString(),
            barcode   = barcode,
            deleted   = false,
            timestamp = timestamp,
            publisher = UserAuth("", "", "", "", null)
        )

        confirmed = true
        presenter.saveProduct(productToSave,requireContext())
    }

    override fun showProgress(enable: Boolean) {
        binding.progressUploadAdd.visibility = if (enable) View.VISIBLE else View.INVISIBLE
        binding.btnFgmSaveAdd.isEnabled
        if (enable){
            binding.btnFgmSaveAdd.isEnabled = false
        }else{
            binding.btnFgmSaveAdd.isEnabled = true
        }
    }

    override fun onFailure(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun imageAlreadyExists(postImage: PostImage) {
        remoteImageUri = postImage.uri
        showRemoteImage(postImage.uri)
        if (postImage.name.isNotEmpty()) {
            binding.edtNameFragmentAdd.setText(postImage.name)
            binding.edtNameFragmentAdd.setSelection(binding.edtNameFragmentAdd.text.length)
        }
    }

    override fun onImageNotFound() {
        binding.progressUploadAdd.visibility =  View.INVISIBLE
    }

    override fun openCamera() {
        photoFile?.delete()
        cameraUtil.openCamera(requireContext(), cameraLauncher)
    }

    override fun openGallery() {
        findNavController().navigate(R.id.action_addFragment_to_nav_gallery_fragment)
    }

    override fun goToHome() {
        CustomProgressBar.showLoadingDialog(requireContext())
        Handler(Looper.getMainLooper()).postDelayed({
            CustomProgressBar.hideLoadingDialog()
            findNavController().popBackStack()
        }, 1000)

    }

    override fun showCategories(categories: List<PostCategory>) {
        listaCategorias = categories

        val nomes = mutableListOf("Selecione uma categoria")
        nomes.addAll(categories.map { it.name })

        val adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            nomes
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        spinnerPronto = false
        binding.spinnerAdd.adapter = adapter

        val indexNaLista = categories.indexOfFirst { it.firestoreId == produto.categoryId }
        if (indexNaLista >= 0) {
            binding.spinnerAdd.setSelection(indexNaLista + 1, false)
        }

        spinnerPronto = true
    }
}