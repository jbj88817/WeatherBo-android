package com.bojie.weatherbo.adapter

import android.content.Context
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import android.widget.Toast
import androidx.recyclerview.widget.RecyclerView
import com.bojie.weatherbo.R
import com.bojie.weatherbo.ui.MainActivity
import com.bojie.weatherbo.util.UnitConvert
import com.bojie.weatherbo.weather.Hour
import java.text.DecimalFormat
import kotlin.math.round

class HourAdapter(private val mContext: Context, private val mHours: Array<Hour>) : 
    RecyclerView.Adapter<HourAdapter.HourViewHolder>() {
    
    private var mTransferredTemp: Double = 0.0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): HourViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.hourly_list_item, parent, false)
        return HourViewHolder(view)
    }

    override fun onBindViewHolder(holder: HourViewHolder, position: Int) {
        holder.bindHour(mHours[position])
    }

    override fun getItemCount(): Int {
        return mHours.size
    }

    inner class HourViewHolder(itemView: View) : 
        RecyclerView.ViewHolder(itemView), View.OnClickListener {
        
        val timeLabel: TextView = itemView.findViewById(R.id.tv_hourlyTimeLabel)
        val summaryLabel: TextView = itemView.findViewById(R.id.tv_hourlySummaryLabel)
        val temperatureLabel: TextView = itemView.findViewById(R.id.tv_hourlyTemperatureLabel)
        val iconImageView: ImageView = itemView.findViewById(R.id.hourlyIconImageView)

        init {
            itemView.setOnClickListener(this)
        }

        fun bindHour(hour: Hour) {
            timeLabel.text = hour.hour
            summaryLabel.text = hour.summary
            
            // Convert unit
            val tempInF = hour.temperature
            val tempInC = UnitConvert.fahrenheitToCelsius(tempInF)
            
            mTransferredTemp = tempInF
            
            when (MainActivity.mButtonUnitConvert.text) {
                "F" -> {
                    mTransferredTemp = tempInF
                    temperatureLabel.text = round(tempInF).toInt().toString()
                }
                "C" -> {
                    mTransferredTemp = tempInC
                    temperatureLabel.text = round(tempInC).toInt().toString()
                }
                else -> {
                    temperatureLabel.text = round(tempInF).toInt().toString()
                }
            }
            
            iconImageView.setImageResource(hour.iconId)
        }

        override fun onClick(v: View) {
            val time = timeLabel.text.toString()
            
            // Format decimal
            val df = DecimalFormat().apply {
                maximumFractionDigits = 2
            }

            val temperature = df.format(mTransferredTemp)
            Log.d("temp", temperature)
            val summary = summaryLabel.text.toString()
            val message = String.format("At %s it will be %s and %s",
                time,
                temperature,
                summary)

            Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show()
        }
    }
} 