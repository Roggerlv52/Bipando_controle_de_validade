package com.rogger.bp.ui.payment

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.util.TypedValue
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.annotation.AttrRes
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.ViewCompat
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.updateLayoutParams
import androidx.fragment.app.Fragment
import com.rogger.bp.R
import com.rogger.bp.databinding.FragmentPayBinding

/*
 private fun iniciarAnimacaoVibracaoInfinita() {
        shakeAnimator?.cancel()

        shakeAnimator = ValueAnimator.ofInt(0,20,-18,18,-15,15,-6,6,0).apply {
            duration = 1200

            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()
            addUpdateListener { valueAnimator ->
                val deslocamento = valueAnimator.animatedValue as Int
                binding.btnStartFreeTrial.translationX = deslocamento.toFloat()
            }
            start()
        }
    }
 */

class PayFragment : Fragment() {

    private var _binding: FragmentPayBinding? = null
    private val binding get() = _binding!!

    private var shakeAnimator: ValueAnimator? = null

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

        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(
            R.attr.ic_color_theme,
            typedValue,
            true
        )
        binding.btnClosePay.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
        // Aplica padding dinâmico no botão fechar para respeitar a statusbar real
        ViewCompat.setOnApplyWindowInsetsListener(binding.btnClosePay) { v, insets ->
            val systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars())
            v.updateLayoutParams<ViewGroup.MarginLayoutParams> {
                topMargin = systemBars.top + 40  // 8dp de folga visual
            }
            insets
        }
    }

    /**
     * Força o sistema a desenhar de ponta a ponta (Edge-to-Edge),
     * tornando a Barra de Status (topo) e Barra de Navegação (rodapé) 100% transparentes.
     */
    private fun enableEdgeToEdge() {
        val window = requireActivity().window

        // Diz ao sistema que o app vai gerenciar os insets manualmente
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Torna as barras transparentes (não esconde — apenas deixa ver por baixo)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
    }

    /**
     * Restaura as barras do sistema ao estado original do aplicativo para não afetar as outras telas.
     */
    private fun restoreSystemBars() {
        val window = requireActivity().window

        // Volta a encaixar a UI dentro das barras
        WindowCompat.setDecorFitsSystemWindows(window, true)

        // Restaura as cores originais do app
        window.statusBarColor = getThemeColor(R.attr._color_theme_status)
        window.navigationBarColor = getThemeColor(R.attr._color_theme_navigation)
        // Garante que as barras estejam visíveis (caso hide() tenha sido chamado antes)
        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.show(WindowInsetsCompat.Type.systemBars())
    }
    private fun getThemeColor(@AttrRes attrColor: Int): Int {
        val typedValue = TypedValue()
        requireContext().theme.resolveAttribute(attrColor, typedValue, true)
        return typedValue.data
    }

    private fun iniciarAnimacaoVibracaoInfinita() {
        shakeAnimator?.cancel()

        shakeAnimator = ValueAnimator.ofInt(0, 20, -20, 20, -20, 20, -20, 0).apply {
            duration = 1200
            repeatCount = ValueAnimator.INFINITE
            repeatMode = ValueAnimator.RESTART
            interpolator = android.view.animation.AccelerateDecelerateInterpolator()

            addUpdateListener { valueAnimator ->
                val deslocamento = valueAnimator.animatedValue as Int
                binding.btnStartFreeTrial.translationX = deslocamento.toFloat()
            }
            start()
        }
    }

    override fun onResume() {
        super.onResume()
        // 👉 Ativa o modo de imersão de ponta a ponta ao entrar
        enableEdgeToEdge()
        iniciarAnimacaoVibracaoInfinita()

        mainToolbar?.visibility = View.GONE
        (activity as? AppCompatActivity)?.supportActionBar?.hide()
    }

    override fun onPause() {
        super.onPause()
        shakeAnimator?.cancel()
        shakeAnimator = null

        // 👉 Restaura a formatação padrão das barras para as outras telas do app
        restoreSystemBars()

        mainToolbar?.visibility = View.VISIBLE
        (activity as? AppCompatActivity)?.supportActionBar?.show()
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}