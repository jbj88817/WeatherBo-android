package com.bojie.weatherbo.ui

import android.content.Intent
import android.os.Bundle
import android.os.Parcelable
import android.view.View
import android.widget.AdapterView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import com.bojie.weatherbo.R
import com.bojie.weatherbo.adapter.DayAdapter
import com.bojie.weatherbo.databinding.ActivityDailyForecastBinding
import com.bojie.weatherbo.util.UnitConvert
import com.bojie.weatherbo.weather.Day
import java.text.DecimalFormat
import java.util.Arrays

class DailyForecastActivity : AppCompatActivity() {

    private lateinit var mDays: Array<Day>
    private lateinit var binding: ActivityDailyForecastBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityDailyForecastBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        val intent = intent
        val parcelables = intent.getParcelableArrayExtra(MainActivity.DAILY_FORECAST)
        mDays = Arrays.copyOf(parcelables, parcelables!!.size, Array<Day>::class.java)
        
        val adapter = DayAdapter(this, mDays)
        binding.list.adapter = adapter
        binding.list.emptyView = binding.empty
        binding.list.onItemClickListener = AdapterView.OnItemClickListener { _, _, position, _ ->
            val dayOfTheWeek = mDays[position].dayOfTheWeek
            val conditions = mDays[position].summary
            
            val tempInF = mDays[position].temperatureMax
            val tempInC = UnitConvert.fahrenheitToCelsius(tempInF)
            val df = DecimalFormat().apply {
                maximumFractionDigits = 2
            }
            
            val highTemp = when (MainActivity.mButtonUnitConvert.text) {
                "F" -> df.format(tempInF)
                "C" -> df.format(tempInC)
                else -> df.format(tempInF)
            }
            
            val message = String.format(
                "On %s the high will be %s and it will be %s",
                dayOfTheWeek,
                highTemp,
                conditions
            )
            
            Toast.makeText(applicationContext, message, Toast.LENGTH_LONG).show()
        }
        
        val cityName = intent.getStringExtra(MainActivity.CITY_NAME)
        binding.tvLocationLabel.text = cityName
    }
} 