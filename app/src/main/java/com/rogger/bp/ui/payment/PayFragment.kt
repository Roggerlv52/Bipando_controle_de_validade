package com.rogger.bp.ui.payment

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
import androidx.fragment.app.Fragment
import com.rogger.bp.R
import com.rogger.bp.ui.animation.GradientAnimator

private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

class PayFragment : Fragment() {
    private var param1: String? = null
    private var param2: String? = null
    private var animator: GradientAnimator? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        return inflater.inflate(R.layout.fragment_pay, container, false)
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        ///val linear = view.findViewById<LinearLayout>(R.id.linear_pay)
       // animator = GradientAnimator(linear)
       // animator?.start()
    }

    companion object {
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            PayFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    override fun onDestroyView() {
        super.onDestroyView()
        if (animator != null) {
            animator?.stop()
        }
    }

}