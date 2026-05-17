package com.rogger.bp.ui.edit.view

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.ArrayAdapter
import androidx.activity.result.ActivityResultLauncher
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DecodeFormat
import com.google.android.material.snackbar.Snackbar
import com.rogger.bp.R
import com.rogger.bp.data.model.PostCategory
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.databinding.FragmentEditBinding
import com.rogger.bp.ui.base.DialogUtil
import com.rogger.bp.ui.base.Utils
import com.rogger.bp.ui.category.data.CategoryDataSource
import com.rogger.bp.ui.category.data.CategoryRepository
import com.rogger.bp.ui.commun.ShowSelectDialog
import com.rogger.bp.ui.edit.ContractEdit
import com.rogger.bp.ui.edit.data.EditDataSource
import com.rogger.bp.ui.edit.data.EditRepository
import com.rogger.bp.ui.edit.presentation.EditPresenter
import com.rogger.bp.ui.gallery.CameraCallback
import com.rogger.bp.ui.gallery.ImagePikerUtil
import com.rogger.bp.ui.gallery.ImageUtils
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.collections.map

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 14/05/2026
 * Hora: 20:06
 */
class EditFragment : Fragment(), ContractEdit.View {

    override lateinit var presenter: ContractEdit.Presenter
    private var _binding: FragmentEditBinding? = null
    private val binding get() = _binding!!
    private var produto: PostProduct? = null

    private var timestamp: Long = 0L

    private var listaCategorias: List<PostCategory> = emptyList()

    private var spinnerPronto = false

    private var novaImagemFile: File? = null

    private val imagePickerUtil = ImagePikerUtil()
    private lateinit var cameraLauncher: ActivityResultLauncher<Intent>

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentEditBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        // ── Injecção MVP ──────────────────────────────────────────────────
        presenter = EditPresenter(
            view = this,
            repository = EditRepository(EditDataSource()),
            categoryRepository = CategoryRepository(CategoryDataSource())
        )

        binding.imageEdit.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        setupCamera()
        setupGalleryResult()
        setupSpinnerListener()
        setupClicks()
        setupMenu()

        presenter.fetchCategories()

        loadProductFromArgs()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        if (::presenter.isInitialized) presenter.onDestroy()
        super.onDestroy()
    }

    private fun setupCamera() {
        cameraLauncher = imagePickerUtil.register(this, object : CameraCallback {
            override fun onImageCaptured(imageUri: Uri, imageFile: File) {
                try {
                    novaImagemFile = ImageUtils.processImage(requireContext(), imageUri, imageFile)
                    showImageView(novaImagemFile!!.absolutePath)
                } catch (e: Exception) {
                    onError(e.message ?: "Erro ao processar imagem")
                }
            }

            override fun onCancel() {}
            override fun onError(e: Exception) {
                onError(e.message ?: "Erro na câmera")
            }
        })
    }

    private fun setupGalleryResult() {
        parentFragmentManager.setFragmentResultListener(
            "gallery_result",
            viewLifecycleOwner
        ) { _, bundle ->
            try {
                val uri = Uri.parse(bundle.getString("imageUri"))
                val out = ImagePikerUtil.createImageFile(requireContext())
                novaImagemFile = ImageUtils.processImage(requireContext(), uri, out)
                showImageView(novaImagemFile!!.absolutePath)
            } catch (e: Exception) {
                onError(e.message ?: "Erro ao processar imagem da galeria")
            }
        }
    }

    private fun setupSpinnerListener() {
        binding.spinnerEdit.onItemSelectedListener =
            object : android.widget.AdapterView.OnItemSelectedListener {
                override fun onItemSelected(
                    parent: android.widget.AdapterView<*>,
                    view: View?,
                    position: Int,
                    id: Long
                ) {
                    if (!spinnerPronto || produto == null) return
                    // Posição 0 = placeholder "Selecione…" (id = -1)
                    val cat = listaCategorias.getOrNull(position - 1)
                    produto = produto!!.copy(categoryId = cat?.id ?: 0)
                }

                override fun onNothingSelected(parent: android.widget.AdapterView<*>) {}
            }
    }

    private fun setupClicks() {
        // Imagem → picker câmera/galeria
        binding.imageEdit.setOnClickListener {
            ShowSelectDialog.show(requireContext(), object : ShowSelectDialog.selectedCallback {
                override fun openGallery() {
                    findNavController().navigate(R.id.action_editFragment_to_galerryFragment)
                }

                override fun openCamera() {
                    imagePickerUtil.openCamera(requireContext(), cameraLauncher)
                }
            })
        }

        // Data
        binding.datePickerButton.setOnClickListener {
            Utils.showDatePicker(requireContext()) { ts, dataFormatada ->
                timestamp = ts
                binding.datePickerButton.text = dataFormatada
            }
        }

        // Salvar
        binding.btnFgmSave.setOnClickListener { salvarProduto() }

        // Barcode → tela de imagem do barcode
        binding.txtEditBacode.setOnClickListener {
            val barcode = binding.txtEditBacode.text.toString()
            if (barcode.isNotBlank()) abrirImagemBarcode(barcode)
        }
    }

    private fun setupMenu() {
        requireActivity().addMenuProvider(object : MenuProvider {
            override fun onCreateMenu(menu: Menu, menuInflater: MenuInflater) {
                menu.clear()
                menuInflater.inflate(R.menu.menu_edit, menu)
            }

            override fun onMenuItemSelected(menuItem: MenuItem): Boolean {
                if (menuItem.itemId == R.id.action_delete) {
                    confirmarDelete()
                    return true
                }
                return false
            }
        }, viewLifecycleOwner, Lifecycle.State.RESUMED)
    }

    private fun loadProductFromArgs() {
        val id = arguments?.getString("uuid")
        if (id == null) {
            onError("Produto inválido")
            return
        }
        presenter.loadProduct(id)
    }

    private fun salvarProduto() {
        if (!Utils.validEditText(binding.edtNameFragment)) return

        val p = produto ?: run { onError("Produto não carregado"); return }

        val atualizado = p.copy(
            name = binding.edtNameFragment.text.toString().trim(),
            note = binding.editFragmentNote.text.toString(),
            timestamp = timestamp,
            barcode = binding.txtEditBacode.text.toString(),
            // Se o utilizador escolheu uma nova imagem, usa o caminho local
            imageUri = novaImagemFile?.absolutePath ?: p.imageUri
        )

        presenter.saveProduct(atualizado)
    }

    private fun confirmarDelete() {
        val p = produto ?: return
        DialogUtil.showDeleteDialog(
            requireContext(),
            "Deseja realmente excluir \"${p.name}\"?"
        ) {
            presenter.deleteProduct(p)
        }
    }

    private fun selecionarCategoria(categoryId: Int) {
        listaCategorias.forEachIndexed { index, cat ->
            if (cat.id == categoryId) {
                binding.spinnerEdit.setSelection(index + 1) // +1 pelo placeholder
                return
            }
        }
    }

    private fun abrirImagemBarcode(barcode: String) {
        val intent = Intent(requireContext(), ImageBarcodeShow::class.java)
        intent.putExtra("barcode", barcode)
        startActivity(intent)
    }

    private fun showImageView(path: String) {
        Glide.with(requireContext())
            .asBitmap()
            .load(path)
            .override(512, 512)
            .format(DecodeFormat.PREFER_RGB_565)
            .centerCrop()
            .placeholder(R.drawable.carregando)
            .error(R.drawable.up_picture)
            .into(binding.imageEdit)
    }

    private fun timestampParaData(ts: Long): String =
        SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date(ts))

    override fun showProgress(enable: Boolean) {
        binding.btnFgmSave.isEnabled = !enable
    }

    override fun bindProduct(produto: PostProduct) {
        this.produto = produto
        this.timestamp = produto.timestamp

        binding.edtNameFragment.setText(produto.name)
        binding.editFragmentNote.setText(produto.note)
        binding.txtEditBacode.text = produto.barcode
        binding.datePickerButton.text = timestampParaData(produto.timestamp)

        // Carrega imagem do Storage/URL
        if (produto.imageUri.isNotEmpty()) showImageView(produto.imageUri)

        // Se as categorias já chegaram, selecciona de imediato.
        // Se ainda estão a ser carregadas, [showCategories] chamará
        // selecionarCategoria depois do adapter estar pronto.
        if (spinnerPronto) selecionarCategoria(produto.categoryId)
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

        binding.spinnerEdit.adapter = adapter
        spinnerPronto = true

        // Produto já carregado antes das categorias? Selecciona agora.
        produto?.let { selecionarCategoria(it.categoryId) }
    }

    override fun onSuccess(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_SHORT).show()
    }

    override fun onError(message: String) {
        Snackbar.make(binding.root, message, Snackbar.LENGTH_LONG).show()
    }

    override fun navigateBack() {
        findNavController().popBackStack()
    }
}