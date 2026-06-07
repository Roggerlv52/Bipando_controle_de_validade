package com.rogger.bp.ui.payment

import android.animation.ValueAnimator
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
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
        //enableEdgeToEdge()
        // Configuração do clique no botão Fechar (Voltar)
        binding.btnClosePay.setOnClickListener {
            parentFragmentManager.popBackStack()
        }
    }

    /**
     * Força o sistema a desenhar de ponta a ponta (Edge-to-Edge),
     * tornando a Barra de Status (topo) e Barra de Navegação (rodapé) 100% transparentes.
     */
    private fun enableEdgeToEdge() {
        val window = requireActivity().window

        WindowCompat.setDecorFitsSystemWindows(window, false)

        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.hide(WindowInsetsCompat.Type.systemBars())
        controller.systemBarsBehavior =
            WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
    }

    /**
     * Restaura as barras do sistema ao estado original do aplicativo para não afetar as outras telas.
     */
    private fun restoreSystemBars() {
        val window = requireActivity().window

        WindowCompat.setDecorFitsSystemWindows(window, true)

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller.show(WindowInsetsCompat.Type.systemBars())

        window.statusBarColor = Color.parseColor("#1a272f")
        window.navigationBarColor = Color.parseColor("#1a272f")
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