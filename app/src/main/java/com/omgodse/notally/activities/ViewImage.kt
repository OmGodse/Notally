package com.omgodse.notally.activities

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.omgodse.notally.R
import com.omgodse.notally.databinding.ActivityViewImageBinding
import com.omgodse.notally.miscellaneous.add

class ViewImage : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val binding = ActivityViewImageBinding.inflate(layoutInflater)
        setContentView(binding.root)

        val menu = binding.Toolbar.menu
        menu.add(R.string.share, R.drawable.share) {}
        menu.add(R.string.save_to_device, R.drawable.save) {}
        menu.add(R.string.delete, R.drawable.delete) {}

        binding.Toolbar.setNavigationOnClickListener {
            finish()
        }
    }
}