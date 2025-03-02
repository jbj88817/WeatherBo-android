package com.bojie.weatherbo.weather

import android.os.Parcel
import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone

/**
 * Data class representing a weather forecast for a day
 */
@Parcelize
class Day(
    var time: Long = 0,
    var summary: String = "",
    var temperatureMax: Double = 0.0,
    var icon: String = "",
    var timezone: String = "",
    var cityName: String = ""
) : Parcelable {
    
    val iconId: Int
        get() = Forecast.getIconId(icon)
    
    val dayOfTheWeek: String
        get() {
            val formatter = SimpleDateFormat("EEEE")
            formatter.timeZone = TimeZone.getTimeZone(timezone)
            val dateTime = Date(time * 1000)
            return formatter.format(dateTime)
        }
} 