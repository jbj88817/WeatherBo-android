package com.bojie.weatherbo.ui

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.bojie.weatherbo.R
import com.bojie.weatherbo.adapter.HourAdapter
import com.bojie.weatherbo.databinding.ActivityHourlyForecastBinding
import com.bojie.weatherbo.weather.Hour
import java.util.Arrays

class HourlyForecastActivity : AppCompatActivity() {

    private lateinit var mHours: Array<Hour>
    private lateinit var binding: ActivityHourlyForecastBinding
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityHourlyForecastBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        val intent = intent
        val parcelables = intent.getParcelableArrayExtra(MainActivity.HOURLY_FORECAST)
        mHours = Arrays.copyOf(parcelables, parcelables!!.size, Array<Hour>::class.java)
        
        val adapter = HourAdapter(this, mHours)
        binding.recyclerView.adapter = adapter
        
        val layoutManager = LinearLayoutManager(this)
        binding.recyclerView.layoutManager = layoutManager
        
        binding.recyclerView.setHasFixedSize(true)
    }
} 