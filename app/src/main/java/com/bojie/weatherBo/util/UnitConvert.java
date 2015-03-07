package com.bojie.weatherbo.util;

/**
 * Created by bojiejiang on 3/6/15.
 */
public
class UnitConvert {

    public static double fahrenheitToCelsius(double temperature) {
        temperature = ((temperature - 32) * 5.0) / 9.0;
        return temperature;
    }

    public static double celsiusToFahrenheit(double temperature) {
        temperature = (9.0 / 5.0) * temperature + 32;
        return temperature;
    }
}
