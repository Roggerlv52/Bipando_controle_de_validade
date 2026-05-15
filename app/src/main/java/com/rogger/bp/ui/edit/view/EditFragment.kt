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
import android.widget.AdapterView
import android.widget.ArrayAdapter
import androidx.activity.result.ActivityResultLauncher
import androidx.core.view.MenuProvider
import androidx.fragment.app.Fragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.fragment.findNavController
import com.bumptech.glide.Glide
import com.google.android.material.snackbar.Snackbar
import com.rogger.bp.R
import com.rogger.bp.data.database.FirestoreSchema.Categoria
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.databinding.FragmentEditBinding
import com.rogger.bp.ui.base.DialogUtil
import com.rogger.bp.ui.base.Utils
import com.rogger.bp.ui.commun.DependencyInjector
import com.rogger.bp.ui.commun.ShowSelectDialog
import com.rogger.bp.ui.edit.ContractEdit
import com.rogger.bp.ui.edit.presentation.EditPresenter
import com.rogger.bp.ui.gallery.CameraCallback
import com.rogger.bp.ui.gallery.ImagePikerUtil
import com.rogger.bp.ui.gallery.ImageUtils
import com.rogger.bp.ui.viewmodel.CategoriaViewModel
import com.rogger.bp.ui.viewmodel.DataViewModel
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 14/05/2026
 * Hora: 20:06
 */
class EditFragment : Fragment(), ContractEdit.View {
    override lateinit var presenter: ContractEdit.Presenter

    private var _binding: FragmentEditBinding? = null
    private val binding get() = _binding!!
    private lateinit var categoriaViewModel: CategoriaViewModel
    private lateinit var dataViewModel: DataViewModel
    private var produto: PostProduct? = null
    private var timestamp: Long = 0L
    private var carregandoSpinner = true
    private val listaCategorias = mutableListOf<Categoria>()
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

        // ── Injecção de dependências MVP ──────────────────────────────────
        val repository = DependencyInjector.registerEditRepository()
        presenter = EditPresenter(this, repository)

            //ViewModelProvider(requireActivity()).get(CategoriaViewModel::class.java)
        dataViewModel = ViewModelProvider(requireActivity()).get(DataViewModel::class.java)

        binding.imageEdit.setLayerType(View.LAYER_TYPE_SOFTWARE, null)

        setupCamera()
        setupGalleryResult()
        setupSpinner()
        setupClicks()
        setupMenu()
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
                    val processed = ImageUtils.processImage(requireContext(), imageUri, imageFile)

                    showImageView(processed.absolutePath)
                } catch (e: Exception) {
                    onError(e.message ?: "Erro ao processar imagem")
                }
            }

            override fun onCancel() { /* nada */
            }

            override fun onError(e: Exception) {
                onError(e.message ?: "Erro na câmera")
            }
        })
    }

    /** Recebe resultado da galeria (GalleryFragment via FragmentResult API). */
    private fun setupGalleryResult() {
        parentFragmentManager.setFragmentResultListener(
            "gallery_result",
            viewLifecycleOwner
        ) { _, bundle ->
            try {
                val uri = Uri.parse(bundle.getString("imageUri"))
                val out = ImagePikerUtil.createImageFile(requireContext())
                val processed = ImageUtils.processImage(requireContext(), uri, out)

                showImageView(processed.absolutePath)
            } catch (e: Exception) {
                onError(e.message ?: "Erro ao processar imagem da galeria")
            }
        }
    }

    private fun setupSpinner() {
        categoriaViewModel.categories.observe(viewLifecycleOwner) { cats ->
            listaCategorias.clear()

            // Placeholder

           // if (cats != null) listaCategorias.addAll(cats)

            val adapter = ArrayAdapter(
                requireContext(),
                android.R.layout.simple_spinner_item,
                listaCategorias
            ).also { it.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item) }

            binding.spinnerEdit.adapter = adapter

            // Selecciona a categoria do produto após o adapter estar pronto
            produto?.let { selecionarCategoria(it.categoryId) }
            carregandoSpinner = false
        }

        binding.spinnerEdit.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(
                parent: AdapterView<*>, view: View?, position: Int, id: Long
            ) {
                if (carregandoSpinner || produto == null) return
                val cat = listaCategorias[position]
                // produto!!.categoryId = if (cat.id == -1) 0 else cat.id
            }

            override fun onNothingSelected(parent: AdapterView<*>) { /* nada */
            }
        }
    }

    /** Liga todos os clicks do layout. */
    private fun setupClicks() {

        // ImageView → abre picker câmera/galeria
        binding.imageEdit.setOnClickListener {
            ShowSelectDialog.show(requireContext(), object : ShowSelectDialog.selectedCallback {
                override fun openGallery() {

                    findNavController().navigate(R.id.action_editFragment_to_galerryFragment)
                }

                override fun openCamera() {
                    //editStateVM.prepareForNewImage()
                    imagePickerUtil.openCamera(requireContext(), cameraLauncher)
                }
            })
        }

        binding.datePickerButton.setOnClickListener {
            Utils.showDatePicker(requireContext()) { ts, dataFormatada ->
                timestamp = ts
                binding.datePickerButton.text = dataFormatada
            }
        }

        binding.btnFgmSave.setOnClickListener {
            saveProduct()
        }

        // TextView barcode → abre ecrã de imagem do barcode
        binding.txtEditBacode.setOnClickListener {
            val barcode = binding.txtEditBacode.text.toString()
            if (barcode.isNotBlank()) openImageBarcode(barcode)
        }
    }

    /** Cria o menu de acções (ícone delete na toolbar). */
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
        val id = arguments?.getInt("id", -1) ?: -1
        if (id == -1) {
            onError("Produto inválido")
            return
        }
        presenter.loadProduct(id)
    }

    private fun saveProduct() {
        if (!Utils.validEditText(binding.edtNameFragment)) return

        val p = produto ?: run { onError("Produto não carregado"); return }

        // Guarda URL da imagem antiga antes de aplicar a nova
        //val urlAntiga = editStateVM.oldImagePath

        // Aplica imagem nova ao objeto Produto (local → caminho do ficheiro)
        p.name = binding.edtNameFragment.text.toString().trim()
        p.note = binding.editFragmentNote.text.toString()
        p.timestamp = timestamp
        p.barcode = binding.txtEditBacode.text.toString()

        presenter.saveProduct(p)
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
            // if (cat.id == categoryId) {
            //     binding.spinnerEdit.setSelection(index)
            //    return
            // }
        }
    }

    private fun openImageBarcode(barcode: String) {
        val intent = Intent(requireContext(), ImageBarcodeShow::class.java)
        intent.putExtra("barcode", barcode)
        startActivity(intent)
    }

    private fun showImageView(path: String) {
        binding.imageEdit.setImageResource(R.drawable.carregando)
        Glide.with(requireContext())
            .asBitmap()
            .load(path)
            .override(512, 512)
            .format(com.bumptech.glide.load.DecodeFormat.PREFER_RGB_565)
            .centerCrop()
            .placeholder(R.drawable.carregando)
            .error(R.drawable.up_picture)
            .into(binding.imageEdit)
    }

    override fun showProgress(enable: Boolean) {
        binding.btnFgmSave.isEnabled = !enable
    }

    override fun bindProduct(produto: PostProduct) {

        binding.edtNameFragment.setText(produto.name)
        binding.editFragmentNote.setText(produto.note ?: "")
        binding.txtEditBacode.text = produto.barcode ?: ""

        timestamp = produto.timestamp
        binding.datePickerButton.text = timestampParaData(timestamp)

        //editStateVM.setOldImagePath(produto.imageUri ?: "")

       // if (!editStateVM.hasNewImage()) {
        //    showImageView(produto.imageUri ?: "")
       // }

        if (!carregandoSpinner) selecionarCategoria(produto.categoryId)
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

    private fun timestampParaData(ts: Long): String =
        SimpleDateFormat("dd/MM/yyyy", Locale("pt", "BR")).format(Date(ts))
}