package com.omgodse.notally.fragments

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.Fragment
import com.omgodse.notally.databinding.FragmentSupportDevelopmentBinding
import com.omgodse.notally.miscellaneous.openLink

class SupportDevelopment : Fragment() {

    private var binding: FragmentSupportDevelopmentBinding? = null

    override fun onDestroyView() {
        super.onDestroyView()
        binding = null
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        binding?.Patreon?.setOnClickListener {
            openLink(Patreon)
        }
    }

    override fun onCreateView(inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?): View? {
        binding = FragmentSupportDevelopmentBinding.inflate(inflater)
        return binding?.root
    }

    companion object {
        private const val Patreon = "https://www.patreon.com/omgodse"
    }
}