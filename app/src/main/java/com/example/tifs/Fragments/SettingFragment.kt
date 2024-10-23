package com.example.tifs.Fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.animation.AnimationUtils
import android.widget.Toast
import androidx.fragment.app.Fragment
import com.example.tifs.R
import com.example.tifs.databinding.FragmentCommentBinding
import com.example.tifs.databinding.FragmentSettingBinding

class SettingFragment : Fragment() {


    private var _binding: FragmentSettingBinding? = null
    private val binding get() = _binding!!

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        // Inflate the layout for this fragment
        _binding = FragmentSettingBinding.inflate(inflater, container, false)

        // Function to handle click with blink and toast
        fun handleClick(view: View) {
            val blinkAnimation = AnimationUtils.loadAnimation(requireContext(), R.anim.blink)
            view.startAnimation(blinkAnimation)
            Toast.makeText(requireContext(), "Coming soon", Toast.LENGTH_SHORT).show()
        }

        // Set click listeners on the layouts
        binding.languageLayout.setOnClickListener { handleClick(it) }
        binding.accountInformation.setOnClickListener { handleClick(it) }
        binding.securityLayout.setOnClickListener { handleClick(it) }
        binding.privacyPolicy.setOnClickListener { handleClick(it) }
        binding.termsLayout.setOnClickListener { handleClick(it) }
        binding.aboutusLayout.setOnClickListener { handleClick(it) }

        return binding.root
    }

    override fun onDestroyView() {
        super.onDestroyView()
        _binding = null
    }
}