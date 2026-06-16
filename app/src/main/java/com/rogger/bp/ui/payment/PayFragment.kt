package com.rogger.bp.ui.payment

import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import com.rogger.bp.R
import com.rogger.bp.databinding.FragmentPayBinding
import com.rogger.bp.ui.commun.SharedPreferencesManager

class PayFragment : Fragment() {

    private var billingManager: BillingManager? = null

    private var _binding: FragmentPayBinding? = null
    private val binding get() = _binding!!

    // Rastreia o plano selecionado pelo usuário na UI
    private var selectedPlanId: String? = null

    // Rastreia o plano atual da assinatura ativa (se houver)
    private var activePlanId: String? = null

    private val mainToolbar: Toolbar?
        get() = activity?.findViewById(R.id.toolbar)

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View {
        _binding = FragmentPayBinding.inflate(inflater, container, false)
        return binding.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        setupCloseButton()
        setupPlanSelectionClicks()
        setupSubscribeButton()
        initBillingManager()
    }

    // ── Botão Fechar ──────────────────────────────────────────────────────────

    private fun setupCloseButton() {
        binding.btnClosePay.setOnClickListener {
            parentFragmentManager.popBackStack()
        }

        ViewCompat.setOnApplyWindowInsetsListener(binding.btnClosePay) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = systemBars.top + 40
            }
            insets
        }
    }

    // ── Seleção de Planos (Mensal / Semestral) ────────────────────────────────

    private fun setupPlanSelectionClicks() {
        binding.btnMensal.setOnClickListener {
            selectPlan(billingManager?.productMensalId ?: "bipando_premium_mensal")
        }

        binding.btnSemestral.setOnClickListener {
            selectPlan(billingManager?.productSemestralId ?: "bipando_premium_semestral")
        }
    }

    /**
     * Aplica a seleção visual de um plano:
     * - Marca o checkbox do plano escolhido como ON
     * - Desmarca o outro como OFF
     * - Atualiza o texto e estado do botão de assinatura
     */
    private fun selectPlan(productId: String) {
        selectedPlanId = productId

        val isMensal = productId == (billingManager?.productMensalId ?: "bipando_premium_mensal")

        // Atualiza os drawables de checkbox
        binding.txtMSelect.setCompoundDrawablesRelativeWithIntrinsicBounds(
            0, 0,
            if (isMensal) android.R.drawable.checkbox_on_background
            else android.R.drawable.checkbox_off_background,
            0
        )
        binding.txtSSelect.setCompoundDrawablesRelativeWithIntrinsicBounds(
            0, 0,
            if (!isMensal) android.R.drawable.checkbox_on_background
            else android.R.drawable.checkbox_off_background,
            0
        )

        // Atualiza aparência visual dos cards de plano
        binding.btnMensal.alpha = if (isMensal) 1.0f else 0.6f
        binding.btnSemestral.alpha = if (!isMensal) 1.0f else 0.6f

        // Atualiza o botão de ação conforme estado da assinatura
        updateSubscribeButton()
    }

    // ── Botão de Assinatura ───────────────────────────────────────────────────

    private fun setupSubscribeButton() {
        binding.btnSubscribe.setOnClickListener {
            val planId = selectedPlanId ?: return@setOnClickListener

            // Se já tem assinatura ativa e quer trocar de plano → exibe diálogo informativo
            if (activePlanId != null && activePlanId != planId) {
                showPlanChangeDialog(planId)
            } else {
                // Nova assinatura: inicia o fluxo direto
                billingManager?.purchaseSubscription(planId)
            }
        }
    }

    /**
     * Atualiza o texto e estado do botão de ação de acordo com:
     * - Nenhum plano selecionado
     * - Usuário já tem o plano selecionado ativo
     * - Usuário quer trocar de plano
     * - Nova assinatura
     */
    private fun updateSubscribeButton() {
        val planId = selectedPlanId

        when {
            planId == null -> {
                binding.btnSubscribe.isEnabled = false
                binding.btnSubscribe.text = "Selecione um plano"
                binding.layoutPlanChangeInfo.visibility = View.GONE
            }

            planId == activePlanId -> {
                // Plano já ativo: botão desabilitado
                binding.btnSubscribe.isEnabled = false
                binding.btnSubscribe.text = "✓ Plano atual"
                binding.layoutPlanChangeInfo.visibility = View.GONE
            }

            activePlanId != null -> {
                // Tem assinatura ativa, mas quer outro plano
                val newPlanName = if (planId == billingManager?.productMensalId) "Mensal" else "Semestral"
                binding.btnSubscribe.isEnabled = true
                binding.btnSubscribe.text = "Mudar para o plano $newPlanName"
                showPlanChangeInfo(newPlanName)
            }

            else -> {
                // Novo assinante
                binding.btnSubscribe.isEnabled = true
                binding.btnSubscribe.text = "Assinar agora"
                binding.layoutPlanChangeInfo.visibility = View.GONE
            }
        }
    }

    /**
     * Exibe o banner informativo de troca de plano conforme as diretrizes do Google Play:
     * - Informar claramente que a mudança ocorre no próximo ciclo de cobrança
     * - Não cobrar o usuário imediatamente
     * - Deixar claro o novo plano e as condições
     */
    private fun showPlanChangeInfo(newPlanName: String) {
        binding.layoutPlanChangeInfo.visibility = View.VISIBLE
        binding.txtPlanChangeInfo.text =
            "⚠️ Ao confirmar, seu plano será alterado para $newPlanName. " +
                    "A mudança entrará em vigor no próximo ciclo de cobrança. " +
                    "Você não será cobrado agora. Gerencie sua assinatura a qualquer momento " +
                    "na Google Play Store."
    }

    /**
     * Diálogo de confirmação de troca de plano — exigido pelas diretrizes do Google Play
     * para garantir consentimento explícito do usuário antes de alterar uma assinatura ativa.
     */
    private fun showPlanChangeDialog(newPlanId: String) {
        val currentPlanName = if (activePlanId == billingManager?.productMensalId) "Mensal" else "Semestral"
        val newPlanName = if (newPlanId == billingManager?.productMensalId) "Mensal" else "Semestral"

        AlertDialog.Builder(requireContext())
            .setTitle("Alterar plano de assinatura")
            .setMessage(
                "Você está trocando do plano $currentPlanName para o plano $newPlanName.\n\n" +
                        "• A mudança será aplicada no próximo ciclo de cobrança.\n" +
                        "• Você não será cobrado agora pelo novo plano.\n" +
                        "• Seu acesso premium continua ativo até o fim do período atual.\n\n" +
                        "Deseja continuar?"
            )
            .setPositiveButton("Confirmar troca") { _, _ ->
                billingManager?.purchaseSubscription(newPlanId)
            }
            .setNegativeButton("Cancelar", null)
            .show()
    }

    // ── BillingManager ────────────────────────────────────────────────────────

    private fun initBillingManager() {
        billingManager = BillingManager(
            context = requireContext(),
            activity = requireActivity(),
            onPricesLoaded = { mensalPrice, semestralPrice ->
                binding.txtPriceMensal.text = mensalPrice
                binding.txtPriceSemestral.text = semestralPrice
                binding.txtTrialDuration.text = "30 dias grátis, depois $mensalPrice/mês"
            },
            onSubscriptionStatusLoaded = { activeProductId ->
                activePlanId = activeProductId

                // ✅ ATUALIZAÇÃO: Grava o estado Premium com base na presença de uma assinatura ativa
                val premiumAtivo = activeProductId != null
                SharedPreferencesManager.setPremiumState(requireContext(), premiumAtivo)

                if (activeProductId != null) {
                    selectPlan(activeProductId as String)
                } else {
                    selectedPlanId = null
                    updateSubscribeButton()
                }
            }
        )
    }

    // ── Sistema (Edge-to-Edge, barras, toolbar) ───────────────────────────────

    private fun enableEdgeToEdge() {
        val window = requireActivity().window
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
    }

    private fun restoreSystemBars() {
        val window = requireActivity().window
        WindowCompat.setDecorFitsSystemWindows(window, true)
        window.statusBarColor = getThemeColor(R.attr._color_theme_status)
        window.navigationBarColor = getThemeColor(R.attr._color_theme_navigation)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.show(WindowInsetsCompat.Type.systemBars())
    }

    private fun getThemeColor(@AttrRes attrColor: Int): Int {
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(attrColor, typedValue, true)
        return typedValue.data
    }

    override fun onResume() {
        super.onResume()
        enableEdgeToEdge()
        mainToolbar?.visibility = View.GONE
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }

    override fun onPause() {
        super.onPause()
        restoreSystemBars()
        mainToolbar?.visibility = View.VISIBLE
        (activity as? AppCompatActivity)?.supportActionBar?.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        billingManager?.destroy()
        _binding = null
    }
}