package com.bojie.weatherbo.weather

import com.bojie.weatherbo.R

/**
 * Class representing the complete weather forecast
 */
class Forecast {
    var current: Current? = null
    var hourlyForecast: Array<Hour>? = null
    var dailyForecast: Array<Day>? = null
    
    companion object {
        @JvmStatic
        fun getIconId(iconString: String): Int {
            // clear-day, clear-night, rain, snow, sleet, wind, fog, cloudy, partly-cloudy-day, or partly-cloudy-night.
            return when (iconString) {
                "clear-day" -> R.drawable.clear_day
                "clear-night" -> R.drawable.clear_night
                "rain" -> R.drawable.rain
                "snow" -> R.drawable.snow
                "sleet" -> R.drawable.sleet
                "wind" -> R.drawable.wind
                "fog" -> R.drawable.fog
                "cloudy" -> R.drawable.cloudy
                "partly-cloudy-day" -> R.drawable.partly_cloudy
                "partly-cloudy-night" -> R.drawable.cloudy_night
                else -> R.drawable.clear_day
            }
        }
    }
} 