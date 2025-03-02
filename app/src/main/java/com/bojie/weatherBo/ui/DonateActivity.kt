package com.bojie.weatherbo.ui

import android.os.Bundle
import android.view.View
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bojie.weatherbo.R
import com.bojie.weatherbo.databinding.ActivityDonateBinding

class DonateActivity : AppCompatActivity() {

    companion object {
        var isPaid = false
    }

    private lateinit var binding: ActivityDonateBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDonateBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setupDonateButton()
    }
    
    private fun setupDonateButton() {
        binding.donateInAppButton.setOnClickListener {
            // In a real app, this would handle in-app purchases
            // For now, we'll just simulate a successful donation
            isPaid = true
            Toast.makeText(this, R.string.donate_thank_you, Toast.LENGTH_LONG).show()
            finish()
        }
    }
} 