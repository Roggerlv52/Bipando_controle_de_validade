package com.rogger.bp.ui.home.view

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.google.android.material.snackbar.Snackbar
import com.rogger.bp.R
import com.rogger.bp.data.model.PostProduct
import com.rogger.bp.ui.home.ContractHome
import com.rogger.bp.ui.home.CustomProgressBar
import com.rogger.bp.ui.home.data.HomeDataSource
import com.rogger.bp.ui.home.data.HomeRepository
import com.rogger.bp.ui.home.presentation.HomePresenter

/*
 * Desenvolvido por Roger de Oliveira
 * Data: 13/05/2026
 * Hora: 22:36
 */
class HomeFragment : Fragment(), ContractHome.View {

    // ── MVP ───────────────────────────────────────────────────────────────

    override val presenter: ContractHome.Presenter by lazy {
        HomePresenter(
            view = this,
            repository = HomeRepository(HomeDataSource())
        )
    }

    // ── Lifecycle ─────────────────────────────────────────────────────────

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? = inflater.inflate(R.layout.fragment_home, container, false)

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        presenter.fetchProducts()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        presenter.onDestroy()
    }

    // ── ContractHome.View ─────────────────────────────────────────────────

    override fun showProgress(enable: Boolean) {
        if (enable) {
            CustomProgressBar.showLoadingDialog(requireContext())
        } else {
            CustomProgressBar.hideLoadingDialog()
        }
    }

    override fun showProducts(products: List<PostProduct>) {
        // A lista é gerida pelo AdapterHome via DataViewModel.
        // Este método pode ser usado para actualizar um adapter dedicado MVP
        // ou notificar o fragment pai, consoante a estratégia de migração.
    }

    override fun showEmpty(isEmpty: Boolean) {
        // Alterna visibilidade do estado vazio (ViewFlipper).
        // Implementação específica depende do binding usado (ver HomeFragment.java).
    }

    override fun onSuccess(message: String) {
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_SHORT).show()
        }
    }

    override fun onError(message: String) {
        view?.let {
            Snackbar.make(it, message, Snackbar.LENGTH_LONG).show()
        }
    }
}