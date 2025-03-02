package com.bojie.weatherbo.weather

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.Date

/**
 * Data class representing a weather forecast for an hour
 */
@Parcelize
class Hour(
    var time: Long = 0,
    var summary: String = "",
    var temperature: Double = 0.0,
    var icon: String = "",
    var timezone: String = ""
) : Parcelable {
    
    val iconId: Int
        get() = Forecast.getIconId(icon)
    
    val hour: String
        get() {
            val formatter = SimpleDateFormat("h a")
            val date = Date(time * 1000)
            return formatter.format(date)
        }
} 