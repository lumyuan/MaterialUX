package io.github.lumyuan.ux.ui.fragments

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import androidx.fragment.app.Fragment
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AccelerateDecelerateInterpolator
import android.widget.Toast
import androidx.cardview.widget.CardView
import io.github.lumyuan.ux.KTBasicActivity
import io.github.lumyuan.ux.R
import io.github.lumyuan.ux.core.animation.setOnFeedbackListener
import io.github.lumyuan.ux.core.animation.setOnTouchAnimationToRotation
import io.github.lumyuan.ux.core.common.dip2px
import io.github.lumyuan.ux.databinding.FragmentBlankBinding
import java.util.Random

// TODO: Rename parameter arguments, choose names that match
// the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
private const val ARG_PARAM1 = "param1"
private const val ARG_PARAM2 = "param2"

/**
 * A simple [Fragment] subclass.
 * Use the [BlankFragment.newInstance] factory method to
 * create an instance of this fragment.
 */
class BlankFragment : Fragment() {
    // TODO: Rename and change types of parameters
    private var param1: String? = null
    private var param2: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        arguments?.let {
            param1 = it.getString(ARG_PARAM1)
            param2 = it.getString(ARG_PARAM2)
        }
    }

    private lateinit var binding: FragmentBlankBinding
    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = FragmentBlankBinding.inflate(layoutInflater)
        //触摸动画
        binding.blurCard.setOnTouchAnimationToRotation(20f)
        //点击反馈事件
        binding.circularFlow.setOnFeedbackListener {
            Toast.makeText(it.context, "${binding.circularFlow.getProgress()}", Toast.LENGTH_SHORT).show()
        }

        val rt = object : Runnable {
            override fun run() {
                val random = Random()
                ObjectAnimator.ofFloat(binding.moveCard, "translationX", random.nextInt(400).toFloat() * (random.nextInt(3) - 1)).apply{
                    this.duration = d
                    interpolator = AccelerateDecelerateInterpolator()
                }.start()
                ObjectAnimator.ofFloat(binding.moveCard, "translationY", random.nextInt(400).toFloat() * (random.nextInt(3) - 1)).apply {
                    duration = d
                    interpolator = AccelerateDecelerateInterpolator()
                }.start()
                binding.circularFlow.setProgress(random.nextInt(101).toFloat())
                handler.postDelayed(this, d)
            }

        }
        handler.removeCallbacks(rt)
        handler.post(rt)
        binding.startButton.setOnClickListener {
            activity?.apply {
                startActivity(Intent(this, KTBasicActivity::class.java))
            }
        }
        return binding.root
    }

    companion object {
        /**
         * Use this factory method to create a new instance of
         * this fragment using the provided parameters.
         *
         * @param param1 Parameter 1.
         * @param param2 Parameter 2.
         * @return A new instance of fragment BlankFragment.
         */
        // TODO: Rename and change types and number of parameters
        @JvmStatic
        fun newInstance(param1: String, param2: String) =
            BlankFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_PARAM1, param1)
                    putString(ARG_PARAM2, param2)
                }
            }
    }

    private val d = 2000L

    private val handler = Handler(Looper.getMainLooper())

}