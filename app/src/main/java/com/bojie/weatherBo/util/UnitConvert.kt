package com.bojie.weatherbo.util

/**
 * Utility class for temperature unit conversion
 */
object UnitConvert {
    
    fun fahrenheitToCelsius(temperature: Double): Double {
        return ((temperature - 32) * 5.0) / 9.0
    }
    
    fun celsiusToFahrenheit(temperature: Double): Double {
        return (9.0 / 5.0) * temperature + 32
    }
} 