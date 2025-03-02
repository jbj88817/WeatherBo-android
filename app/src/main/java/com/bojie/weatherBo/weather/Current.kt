package com.bojie.weatherbo.weather

import java.text.SimpleDateFormat
import java.util.Date
import java.util.TimeZone
import kotlin.math.round

class Current {
    var icon: String = ""
    var time: Long = 0
    var temperature: Double = 0.0
    var humidity: Double = 0.0
    var precipChance: Double = 0.0
    var summary: String = ""
    var timeZone: String = ""
    var cityName: String = ""
    
    val iconId: Int
        get() = Forecast.getIconId(icon)
    
    val formattedTime: String
        get() {
            val formatter = SimpleDateFormat("h:mm a")
            formatter.timeZone = TimeZone.getTimeZone(timeZone)
            val dateTime = Date(time * 1000)
            return formatter.format(dateTime)
        }
    
    val temperatureInt: Int
        get() = round(temperature).toInt()
        
    val precipChanceInt: Int
        get() {
            val precipPercentage = precipChance * 100
            return round(precipPercentage).toInt()
        }
} 