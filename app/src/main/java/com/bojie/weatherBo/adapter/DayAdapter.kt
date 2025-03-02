package com.bojie.weatherbo.adapter

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.BaseAdapter
import android.widget.ImageView
import android.widget.TextView
import com.bojie.weatherbo.R
import com.bojie.weatherbo.ui.MainActivity
import com.bojie.weatherbo.util.UnitConvert
import com.bojie.weatherbo.weather.Day
import kotlin.math.round

class DayAdapter(private val mContext: Context, private val mDays: Array<Day>) : BaseAdapter() {

    override fun getCount(): Int {
        return mDays.size
    }

    override fun getItem(position: Int): Any {
        return mDays[position]
    }

    override fun getItemId(position: Int): Long {
        return 0
    }

    override fun getView(position: Int, convertView: View?, parent: ViewGroup): View {
        val holder: ViewHolder
        val view: View
        
        if (convertView == null) {
            view = LayoutInflater.from(mContext).inflate(R.layout.daily_list_item, null)
            holder = ViewHolder()
            holder.iconImageView = view.findViewById(R.id.iconImageView)
            holder.temperatureLabel = view.findViewById(R.id.tv_temperatureLabel)
            holder.dayLabel = view.findViewById(R.id.tv_dayName)
            
            view.tag = holder
        } else {
            view = convertView
            holder = convertView.tag as ViewHolder
        }

        val day = mDays[position]
        holder.iconImageView.setImageResource(day.iconId)
        
        val tempInF = day.temperatureMax
        val tempInC = UnitConvert.fahrenheitToCelsius(tempInF)
        
        when (MainActivity.mButtonUnitConvert.text) {
            "F" -> holder.temperatureLabel.text = round(tempInF).toInt().toString()
            "C" -> holder.temperatureLabel.text = round(tempInC).toInt().toString()
            else -> holder.temperatureLabel.text = round(tempInF).toInt().toString()
        }

        holder.dayLabel.text = if (position == 0) "Today" else day.dayOfTheWeek

        return view
    }

    private class ViewHolder {
        lateinit var iconImageView: ImageView
        lateinit var temperatureLabel: TextView
        lateinit var dayLabel: TextView
    }
} 