package com.rogger.bp.ui.deleteitem.view

import android.app.AlertDialog
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Toast
import androidx.fragment.app.Fragment
import androidx.recyclerview.widget.LinearLayoutManager
import com.rogger.bp.R
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.databinding.FragmentItemDeletedBinding
import com.rogger.bp.ui.commun.DependencyInjector
import com.rogger.bp.ui.deleteitem.DeletedItem
import com.rogger.bp.ui.deleteitem.presenation.DeletedItemPresenter
import com.rogger.bp.util.ImagePikerUtil
import java.io.File

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 17/05/2026
 * Hora: 16:38
 */
class ItemDeletedFragment : Fragment(R.layout.fragment_item_deleted), DeletedItem.View {

    override lateinit var presenter: DeletedItem.Presenter
    private var _binding: FragmentItemDeletedBinding? = null
    private val binding get() = _binding!!
    private lateinit var adapter: ItemDeletedAdapter

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentItemDeletedBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        val repository = DependencyInjector.itemDeletedRepository(requireContext())
        presenter = DeletedItemPresenter(this, repository)


        setupRecyclerView()
        presenter.loadDeletedItems() // ✅ CORREÇÃO: busca os dados ao abrir a tela
    }

    private fun setupRecyclerView() {
        adapter = ItemDeletedAdapter(requireContext()) { item ->
            mostrarDialogo(item)
        }
        binding.recyclerDeleted.layoutManager = LinearLayoutManager(requireContext())
        binding.recyclerDeleted.adapter = adapter
    }

    private fun mostrarDialogo(item: PostProduct) {
        AlertDialog.Builder(requireContext())
            .setTitle(requireContext().getString(
                R.string.dialog_title_delete_item
            ))
            .setMessage(requireContext().getString(
                R.string.dialog_title_option
            ))
            .setNegativeButton(requireContext().getString(
                R.string.dialog_button_cancel
            ), null)
            .setNeutralButton(requireContext().getString(
                R.string.dialog_button_restore
            )) { _, _ ->
                presenter.restoreItem(item)
            }
            .setPositiveButton(requireContext().getString(
                R.string.dialog_button_delete_permanently
            )) { _, _ ->
                presenter.deletePermanently(item)
                if (!item.imageUri.isNullOrEmpty()) {
                    ImagePikerUtil.cleanUpTempFiles(File(item.imageUri))
                }
            }
            .show()
    }

    override fun showDeletedItems(items: List<PostProduct>?) {
        adapter.submitList(items)
        binding.viewFlipper.displayedChild = 0
    }

    override fun showEmptyState() {
        adapter.submitList(emptyList())
        binding.viewFlipper.displayedChild = 1
        binding.imgDeleted.setImageResource(R.drawable.stop_item)
    }

    override fun showSuccessMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }

    override fun showErrorMessage(message: String) {
        Toast.makeText(requireContext(), message, Toast.LENGTH_LONG).show()
    }

    override fun showLoading(show: Boolean) {
        // Implementar progress bar se necessário
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }

    override fun onDestroy() {
        presenter.onDestroy()
        super.onDestroy()
    }
}