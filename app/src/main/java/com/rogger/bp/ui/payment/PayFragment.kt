package com.rogger.bp.ui.payment

import android.graphics.Color
import android.os.Build
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
import com.rogger.bp.ui.base.BaseActivity
import com.rogger.bp.ui.commun.SharedPreferencesManager

@Suppress("DEPRECATION")
class PayFragment : Fragment() {

    private var billingManager: BillingManager? = null

    private var _binding: FragmentPayBinding? = null
    private val binding get() = _binding!!

    // Rastreia o plano selecionado pelo usuário na UI
    private var selectedPlanId: String? = null

    // Rastreia o plano atual da assinatura ativa (se houver)
    private var activePlanId: String? = null

    // ✅ Correção: texto de trial guardado SEPARADO por plano. Antes existia um único
    // texto genérico exibido independente do plano selecionado, o que podia mostrar
    // "30 dias grátis" mesmo quando o usuário tinha escolhido um plano sem trial.
    private var mensalTrialText: String? = null
    private var semestralTrialText: String? = null

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
                topMargin = 50
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

        // ✅ Correção: o banner de trial depende do plano selecionado, então precisa
        // ser recalculado sempre que a seleção muda (Mensal pode ter trial, Semestral não, ou vice-versa)
        updateTrialBanner()
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
                binding.btnSubscribe.text = getString(R.string.select_plan)
                binding.layoutPlanChangeInfo.visibility = View.GONE
            }

            planId == activePlanId -> {
                // Plano já ativo: botão desabilitado
                binding.btnSubscribe.isEnabled = false
                binding.btnSubscribe.text = getString(R.string.current_plan)
                binding.layoutPlanChangeInfo.visibility = View.GONE
            }

            activePlanId != null -> {
                // Tem assinatura ativa, mas quer outro plano
                val newPlanName = if (planId == billingManager?.productMensalId)
                    getString(R.string.plan_name_mensal) else getString(R.string.plan_name_semestral)
                binding.btnSubscribe.isEnabled = true
                binding.btnSubscribe.text = getString(R.string.change_plan_text, newPlanName)
                showPlanChangeInfo(newPlanName)
            }

            else -> {
                // Novo assinante
                binding.btnSubscribe.isEnabled = true
                binding.btnSubscribe.text = getString(R.string.btn_subscribe_now)
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
        binding.txtPlanChangeInfo.text = getString(R.string.plan_change_info_text, newPlanName)
    }

    /**
     * Diálogo de confirmação de troca de plano — exigido pelas diretrizes do Google Play
     * para garantir consentimento explícito do usuário antes de alterar uma assinatura ativa.
     */
    private fun showPlanChangeDialog(newPlanId: String) {
        val currentPlanName = if (activePlanId == billingManager?.productMensalId)
            getString(R.string.plan_name_mensal)else getString(R.string.plan_name_semestral)
        val newPlanName = if (newPlanId == billingManager?.productMensalId)
            getString(R.string.plan_name_mensal) else getString(R.string.plan_name_semestral)

        AlertDialog.Builder(requireContext())
            .setTitle(getString(R.string.change_subscription))
            .setMessage(getString(R.string.plan_change_dialog_message, currentPlanName, newPlanName))
            .setPositiveButton(getString(R.string.dialog_confirm_exchange)) { _, _ ->
                billingManager?.purchaseSubscription(newPlanId)
            }
            .setNegativeButton(getString(R.string.dialog_button_cancel), null)
            .show()
    }

    /**
     * ✅ Correção central: decide o que mostrar no banner de trial/plano ativo
     * com base no ESTADO ATUAL completo (plano ativo + plano selecionado + trial de cada plano),
     * em vez de cada callback assíncrono escrever direto na view (o que causava uma
     * condição de corrida: dependendo da ordem de resposta da rede, o banner de trial
     * podia sobrescrever a mensagem de "plano já ativo", ou vice-versa).
     *
     * Regras:
     * 1) Se o usuário JÁ tem assinatura ativa, isso tem prioridade — mostramos o status
     *    do plano ativo, nunca uma oferta de trial (ele não é mais um novo usuário).
     * 2) Caso contrário, mostramos o trial correspondente ESPECIFICAMENTE ao plano que
     *    está selecionado na tela (Mensal ou Semestral). Se o plano selecionado não tiver
     *    trial configurado no Google Play, o banner é ocultado — nunca mostramos a oferta
     *    de um plano diferente do que o usuário escolheu.
     */
    private fun updateTrialBanner() {
        val activeId = activePlanId

        if (activeId != null) {
            val activePlanName = if (activeId == billingManager?.productMensalId) {
                getString(R.string.plan_name_mensal)
            } else {
                getString(R.string.plan_name_semestral)
            }
            binding.txtTrialDuration.text = getString(R.string.plan_active_info_text, activePlanName)
            binding.txtTrialDuration.visibility = View.VISIBLE
            return
        }

        val trialForSelectedPlan = when (selectedPlanId) {
            billingManager?.productMensalId -> mensalTrialText
            billingManager?.productSemestralId -> semestralTrialText
            else -> null
        }

        if (trialForSelectedPlan != null) {
            binding.txtTrialDuration.text = trialForSelectedPlan
            binding.txtTrialDuration.visibility = View.VISIBLE
        } else {
            binding.txtTrialDuration.visibility = View.GONE
        }
    }

    // ── BillingManager ────────────────────────────────────────────────────────

    private fun initBillingManager() {
        billingManager = BillingManager(
            context = requireContext(),
            activity = requireActivity(),
            onPricesLoaded = { mensalPrice, semestralPrice, mensalTrial, semestralTrial ->
                binding.txtPriceMensal.text = mensalPrice
                binding.txtPriceSemestral.text = semestralPrice

                // ✅ Apenas guarda o estado; quem decide o que mostrar é updateTrialBanner(),
                // evitando que esta resposta assíncrona sobrescreva, por ordem de chegada,
                // uma mensagem de "plano ativo" já exibida por onSubscriptionStatusLoaded.
                mensalTrialText = mensalTrial
                semestralTrialText = semestralTrial
                updateTrialBanner()
            },
            onSubscriptionStatusLoaded = { activeProductId ->
                activePlanId = activeProductId

                // Grava o estado Premium com base na presença de uma assinatura ativa
                val premiumAtivo = activeProductId != null
                SharedPreferencesManager.setPremiumState(requireContext(), premiumAtivo)

                if (activeProductId != null) {
                    // ✅ Apenas pré-seleciona se o utilizador ainda não tiver feito uma escolha manual
                    if (selectedPlanId == null) {
                        selectPlan(activeProductId as String)
                    } else {
                        updateSubscribeButton()
                    }
                } else {
                    updateSubscribeButton()
                }

                // ✅ Sempre recalcula o banner com o estado atual completo (independente
                // de qual callback chegou primeiro — corrige a condição de corrida)
                updateTrialBanner()
            }
        )
    }

    // ── Sistema (Edge-to-Edge, barras, toolbar) ───────────────────────────────
    private fun enableEdgeToEdge() {
        val window = requireActivity().window
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // ✅ CORREÇÃO SÉNIOR: Ignora chamadas descontinuadas no Android 15+ (API 35+)
        if (Build.VERSION.SDK_INT < 35) {
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        }
    }

    private fun restoreSystemBars() {
        val window = requireActivity().window
        WindowCompat.setDecorFitsSystemWindows(window, true)

        // ✅ CORREÇÃO SÉNIOR: Ignora chamadas descontinuadas no Android 15+ (API 35+)
        if (Build.VERSION.SDK_INT < 35) {
            window.statusBarColor = getThemeColor(R.attr._color_theme_status)
            window.navigationBarColor = getThemeColor(R.attr._color_theme_navigation)
        }

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller?.show(WindowInsetsCompat.Type.systemBars())
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