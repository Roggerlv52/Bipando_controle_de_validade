package com.rogger.bp.ui.add.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
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
import com.rogger.bp.ui.base.Utils
import com.rogger.bp.ui.commun.DependencyInjector
import com.rogger.bp.ui.commun.ShowSelectDialog
import com.rogger.bp.ui.gallery.CameraCallback
import com.rogger.bp.ui.gallery.ImagePikerUtil
import com.rogger.bp.ui.gallery.ImageUtils
import com.rogger.bp.ui.scanner.ImageBarcode
import java.io.File

class AddItemFragment : Fragment(R.layout.fragment_add),RegisterAdd.View {
    companion object {
        const val KEY_BARCODE     = "key_barcode"
        const val KEY_CATEGORIA   = "categoria_id"
    }
    // ── MVP ───────────────────────────────────────────────────────────────
    override lateinit var presenter: RegisterAdd.Presenter

    private var _binding: FragmentAddBinding? = null
    private val binding get() = _binding!!
    private var listaCategorias: List<PostCategory> = emptyList()
    private var spinnerPronto = false

    private var barcode: String = ""
    private var timestamp: Long = 0L
    private var photoFile: File? = null
    private var remoteImageUri: String? = null   // URI da imagem já existente no Storage
    private var confirmed: Boolean = false        // evita apagar foto se o usuário salvou

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

        val repository = DependencyInjector.registerProductRepository()
        val categoryRepository = DependencyInjector.registerCategoryRepository(requireContext())
        presenter = AddItemPresenter(this, repository, categoryRepository)

        val dataAtual = Utils.getCurrentDateFormatted()
        binding.datePickerBtnAdd.text = dataAtual
        binding.txtAddData.text = dataAtual
        timestamp = Utils.getCurrentTimestamp()

        setupCamera()
        setupGalleryResult()
        setupListeners()
        readArguments()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        // Limpa arquivo temporário se o usuário cancelou sem salvar
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
                    photoFile = ImageUtils.processImage(requireContext(), imageUri, imageFile)
                    remoteImageUri = null    // foto local substitui qualquer URI remota
                    showLocalImage(photoFile!!)
                    // Upload é feito apenas ao clicar em "Salvar" (saveProduct).
                } catch (e: Exception) {
                    onFailure(e.message ?: "Erro ao processar imagem")
                }
            }

            override fun onCancel() { /* nenhuma ação necessária */ }
            override fun onError(e: Exception) {
                onFailure(e.message ?: "Erro na câmera")
            }
        })
    }

    private fun setupGalleryResult() {
        parentFragmentManager.setFragmentResultListener(
            "gallery_result",
            viewLifecycleOwner
        ) { _, bundle ->
            try {
                val img = Uri.parse(bundle.getString("imageUri"))
                val file = ImagePikerUtil.createImageFile(requireContext())
                photoFile = ImageUtils.processImage(requireContext(), img, file)
                remoteImageUri = null
                showLocalImage(photoFile!!)
                // Upload é feito apenas ao clicar em "Salvar" (saveProduct),
                // nunca antes de o produto existir no banco.
            } catch (e: Exception) {
                onFailure(e.message ?: "Erro ao processar imagem da galeria")
            }
        }
    }

    private fun setupListeners() {
        binding.btnFgmSaveAdd.setOnClickListener {
            saveProduct()
        }
        binding.datePickerBtnAdd.setOnClickListener {
            Utils.showDatePicker(requireContext()) { ts, dataFormatada ->
                binding.datePickerBtnAdd.text = dataFormatada
                binding.txtAddData.text = dataFormatada
                timestamp = ts
            }
        }

        binding.txtAddBarcode.setOnClickListener {
            openBarcodeImageScreen(binding.txtAddBarcode.text.toString())
        }

        binding.fragmentImgAdd.setOnClickListener {
            if (remoteImageUri != null) return@setOnClickListener   // bloqueado
            openMediaPicker()
        }
    }

    private fun readArguments() {
        val args = arguments ?: return

        barcode = args.getString(KEY_BARCODE).orEmpty()

        if (barcode.isNotEmpty()) {
            binding.txtAddBarcode.text = barcode
            // Verifica se já existe imagem para este barcode no catálogo global
            presenter.checkOrCreateImage(barcode)
        }
    }

    /** Exibe arquivo local no ImageView. */
    private fun showLocalImage(file: File) {
        Glide.with(requireContext())
            .load(file)
            .override(500, 500)
            .centerCrop()
            .into(binding.fragmentImgAdd)
    }

    /** Exibe URL remota no ImageView. */
    private fun showRemoteImage(url: String) {
        Glide.with(requireContext())
            .load(url)
            .override(500, 500)
            .placeholder(R.drawable.carregando)
            .error(R.drawable.imagem_error)
            .centerCrop()
            .into(binding.fragmentImgAdd)
        binding.fragmentImgAdd.isClickable = false
    }

    private fun openMediaPicker() {
        ShowSelectDialog.show(requireContext(), object : ShowSelectDialog.selectedCallback {
            override fun openGallery() {
                this@AddItemFragment.openGallery()
            }
            override fun openCamera() {
                this@AddItemFragment.openCamera()
            }
        })
    }

    private fun saveProduct() {
        val nome = binding.edtNameFragmentAdd.text.toString().trim()
        if (!Utils.validEditText(binding.edtNameFragmentAdd)) return

        val imageUri: Uri = when {
            remoteImageUri != null -> Uri.parse(remoteImageUri)
            photoFile != null      -> Uri.fromFile(photoFile)
            else                   -> Uri.EMPTY
        }

        val product = PostProduct(
            imageUri   = imageUri.toString(),
            name       = nome,
            note       = binding.editFragmentNoteAdd.text.toString(),
            barcode    = barcode,
            deleted    = false,
            timestamp  = timestamp,
            publisher  = UserAuth("", "", "", "", null)  // preenchido no DataSource com Auth atual
        )

        confirmed = true
        presenter.saveProduct(product)
    }

    private fun openBarcodeImageScreen(barcodeValue: String) {
        val intent = Intent(requireContext(), ImageBarcode::class.java)
        intent.putExtra("keyBarcode", barcodeValue)
        startActivity(intent)
    }
    override fun showProgress(enable: Boolean) {
        binding.progressUploadAdd.visibility = if (enable) View.VISIBLE else View.GONE
        binding.btnFgmSaveAdd.isEnabled = !enable
    }
    override fun onFailure(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
    override fun imageAlreadyExists(postImage: PostImage) {
        remoteImageUri = postImage.uri
        showRemoteImage(postImage.uri)
        if (postImage.name.isNotEmpty()) {
            binding.edtNameFragmentAdd.setText(postImage.name)
            binding.edtNameFragmentAdd.setSelection(
                binding.edtNameFragmentAdd.text.length
            )
        }
    }
    override fun onImageNotFound() {
        binding.fragmentImgAdd.isClickable = true
    }
    override fun openCamera() {
        photoFile?.delete()
        cameraUtil.openCamera(requireContext(), cameraLauncher)
    }
    override fun openGallery() {
        findNavController().navigate(R.id.action_addFragment_to_nav_gallery_fragment)
    }
    override fun goToHome() {
        ToastCustom.showCustomToast(requireContext(),"")

        findNavController().popBackStack()
    }

    override fun showCategories(categories: List<PostCategory>) {
        listaCategorias = categories

        val nomes = mutableListOf("Selecione uma categoria")
        nomes.addAll(categories.map { it.name })

        binding.spinnerAdd.adapter = ArrayAdapter(
            requireContext(),
            android.R.layout.simple_spinner_item,
            nomes
        ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

        spinnerPronto = true

        listaCategorias.forEachIndexed { index, cat ->
            if (cat.firestoreId.isBlank()) {
                binding.spinnerAdd.setSelection(index + 1)
                return
            }
        }
    }
}