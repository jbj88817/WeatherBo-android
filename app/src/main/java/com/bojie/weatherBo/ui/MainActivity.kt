package com.bojie.weatherbo.ui

import android.Manifest
import android.app.AlertDialog
import android.content.Context
import android.content.DialogInterface
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.LocationManager
import android.net.ConnectivityManager
import android.net.NetworkInfo
import android.os.Bundle
import android.util.Log
import android.view.View
import android.widget.Button
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout
import com.bojie.weatherbo.R
import com.bojie.weatherbo.databinding.ActivityMainBinding
import com.bojie.weatherbo.util.UnitConvert
import com.bojie.weatherbo.weather.Current
import com.bojie.weatherbo.weather.Day
import com.bojie.weatherbo.weather.Forecast
import com.bojie.weatherbo.weather.Hour
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdView
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationCallback
import com.google.android.gms.location.LocationRequest
import com.google.android.gms.location.LocationResult
import com.google.android.gms.location.LocationServices
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.util.Locale
import kotlin.math.round
import java.util.TimeZone

class MainActivity : AppCompatActivity(), SwipeRefreshLayout.OnRefreshListener {

    companion object {
        const val TAG = "MainActivity"
        const val DAILY_FORECAST = "DAILY_FORECAST"
        const val HOURLY_FORECAST = "HOURLY_FORECAST"
        const val CITY_NAME = "CITYNAME"
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        const val PREFS_NAME = "weather_prefs"
        const val KEY_IS_FAHRENHEIT = "is_fahrenheit"
        
        lateinit var mButtonUnitConvert: Button
    }

    private var mLongitude: Double = 0.0
    private var mLatitude: Double = 0.0

    private var mCityName: String = ""
    private var mCurrentTempInF: Double = 0.0
    private var mCurrentTempInC: Double = 0.0
    private var isFahrenheit: Boolean = true

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private lateinit var mForecast: Forecast
    private lateinit var binding: ActivityMainBinding
    private lateinit var mAdView: AdView



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        setContentView(binding.root)
        
        mButtonUnitConvert = findViewById(R.id.btn_unit)

        // Load preference
        val settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        isFahrenheit = settings.getBoolean(KEY_IS_FAHRENHEIT, true)
        updateUnitButtonText()
        
        binding.progressBar.visibility = View.INVISIBLE
        
        mForecast = Forecast()
        
        // Check if has GPS
        val locationManager = getSystemService(Context.LOCATION_SERVICE) as LocationManager
        if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
            buildAlertMessageNoGps()
        }
        
        // Setup location services
        setupLocationServices()
        
        // Refresh button click listener
        binding.refreshImageView.setOnClickListener {
            getForecast(mLatitude, mLongitude)
        }
        
        // SwipeRefresh
        binding.swipeRefreshLayout.setOnRefreshListener(this)
        binding.swipeRefreshLayout.setColorSchemeResources(
            R.color.swipeRefresh1,
            R.color.swipeRefresh2,
            R.color.swipeRefresh3,
            R.color.swipeRefresh4
        )
        
        // Setup ads
        mAdView = binding.adView
        mAdView.visibility = View.GONE
        
        // Setup button click listeners
        setupButtonClickListeners()
    }
    
    private fun setupLocationServices() {
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        
        locationRequest = LocationRequest.create().apply {
            priority = LocationRequest.PRIORITY_HIGH_ACCURACY
            interval = 10000 // 10 seconds
            fastestInterval = 1000 // 1 second
        }
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                locationResult.lastLocation?.let { location ->
                    Log.d(TAG, "New location received: ${location.latitude}, ${location.longitude}")
                    mLatitude = location.latitude
                    mLongitude = location.longitude
                    getForecast(mLatitude, mLongitude)
                    getCityName()
                } ?: run {
                    Log.e(TAG, "Location result was null")
                }
            }
        }
        
        // Check for location permission
        if (ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(Manifest.permission.ACCESS_FINE_LOCATION),
                LOCATION_PERMISSION_REQUEST_CODE
            )
        } else {
            // Get last known location first
            getLastKnownLocation()
            // Then start location updates
            startLocationUpdates()
        }
    }
    
    private fun getLastKnownLocation() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                if (location != null) {
                    Log.d(TAG, "Last known location: ${location.latitude}, ${location.longitude}")
                    mLatitude = location.latitude
                    mLongitude = location.longitude
                    getForecast(mLatitude, mLongitude)
                    getCityName()
                } else {
                    Log.d(TAG, "Last known location is null, waiting for location updates")
                }
            }.addOnFailureListener { e ->
                Log.e(TAG, "Error getting last location: ${e.message}", e)
            }
        }
    }
    
    private fun startLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(
                this,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        ) {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
        }
    }
    
    private fun getCityName() {
        val gcd = Geocoder(this, Locale.getDefault())
        try {
            val addresses = gcd.getFromLocation(mLatitude, mLongitude, 1)
            if (addresses != null && addresses.isNotEmpty()) {
                mCityName = addresses[0].locality ?: addresses[0].adminArea ?: ""
                Log.d("$TAG!!!!!!!!!!!", mCityName)
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
    }
    
    override fun onResume() {
        super.onResume()
        if (mLongitude != 0.0 && mLatitude != 0.0) {
            getForecast(mLatitude, mLongitude)
            getCityName()
        }
        
        if (DonateActivity.isPaid) {
            mAdView.isEnabled = false
            mAdView.visibility = View.GONE
        } else {
            mAdView.isEnabled = true
            mAdView.visibility = View.VISIBLE
            // Comment out AdMob initialization to avoid crashes
            /*
            val adRequest = AdRequest.Builder().build()
            mAdView.loadAd(adRequest)
            */
        }
    }
    
    override fun onPause() {
        super.onPause()
        fusedLocationClient.removeLocationUpdates(locationCallback)
    }
    
    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.isNotEmpty() && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                Log.d(TAG, "Location permission granted")
                // Get last known location first
                getLastKnownLocation()
                // Then start location updates
                startLocationUpdates()
            } else {
                Log.e(TAG, "Location permission denied")
                Toast.makeText(
                    this,
                    "Location permission is required for this app to work properly",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }
    
    private fun buildAlertMessageNoGps() {
        val builder = AlertDialog.Builder(this)
        builder.setMessage("Your GPS seems to be disabled, do you want to enable it?")
            .setCancelable(false)
            .setPositiveButton("Yes") { _, _ ->
                startActivity(Intent(android.provider.Settings.ACTION_LOCATION_SOURCE_SETTINGS))
            }
            .setNegativeButton("No") { dialog, _ ->
                dialog.cancel()
                Toast.makeText(this, "Location is needed for this app", Toast.LENGTH_SHORT).show()
            }
        val alert = builder.create()
        alert.show()
    }
    
    private fun getForecast(latitude: Double, longitude: Double) {
        // Check if we have valid coordinates
        if (latitude == 0.0 && longitude == 0.0) {
            Log.e(TAG, "Invalid location coordinates: $latitude, $longitude")
            Toast.makeText(
                this,
                "Unable to get your location. Please check your GPS settings.",
                Toast.LENGTH_LONG
            ).show()
            // Use default coordinates for New York City
            val defaultLat = 40.7128
            val defaultLon = -74.0060
            Log.d(TAG, "Using default location: New York City")
            Toast.makeText(
                this,
                "Using default location: New York City",
                Toast.LENGTH_LONG
            ).show()
            fetchWeatherData(defaultLat, defaultLon)
            return
        }
        
        Log.d(TAG, "Getting forecast for location: $latitude, $longitude, City: $mCityName")
        fetchWeatherData(latitude, longitude)
    }
    
    private fun fetchWeatherData(latitude: Double, longitude: Double) {
        val forecastUrl = "https://api.open-meteo.com/v1/forecast?latitude=$latitude&longitude=$longitude&current=temperature_2m,relative_humidity_2m,weather_code,precipitation_probability&hourly=temperature_2m,weather_code,precipitation_probability&daily=weather_code,temperature_2m_max,temperature_2m_min,precipitation_probability_max&temperature_unit=fahrenheit&wind_speed_unit=mph&precipitation_unit=inch&timezone=auto&timeformat=unixtime"
        
        Log.d(TAG, "API URL: $forecastUrl")
        
        if (isNetworkAvailable()) {
            toggleRefresh()
            
            val client = OkHttpClient()
            val request = Request.Builder()
                .url(forecastUrl)
                .build()
            
            client.newCall(request).enqueue(object : Callback {
                override fun onFailure(call: Call, e: IOException) {
                    runOnUiThread {
                        toggleRefresh()
                        Log.e(TAG, "Failed to fetch data: ${e.message}", e)
                        Toast.makeText(
                            this@MainActivity,
                            "Network error: ${e.message}",
                            Toast.LENGTH_LONG
                        ).show()
                        alertUserAboutError()
                    }
                }
                
                override fun onResponse(call: Call, response: Response) {
                    runOnUiThread { toggleRefresh() }
                    
                    try {
                        val jsonData = response.body?.string()
                        Log.v(TAG, "API Response: ${jsonData ?: "Empty response"}")
                        
                        if (response.isSuccessful) {
                            if (jsonData.isNullOrEmpty()) {
                                runOnUiThread {
                                    Log.e(TAG, "API returned empty response")
                                    Toast.makeText(
                                        this@MainActivity,
                                        "Empty response from server",
                                        Toast.LENGTH_LONG
                                    ).show()
                                    alertUserAboutError()
                                }
                            } else {
                                try {
                                    mForecast = parseOpenMeteoData(jsonData)
                                    runOnUiThread { updateDisplay() }
                                } catch (e: Exception) {
                                    runOnUiThread {
                                        Log.e(TAG, "JSON parsing error: ${e.message}", e)
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Data parsing error: ${e.message}",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        alertUserAboutError()
                                    }
                                }
                            }
                        } else {
                            runOnUiThread {
                                Log.e(TAG, "API Error: ${response.code} - ${response.message}")
                                alertUserAboutError()
                            }
                        }
                    } catch (e: IOException) {
                        runOnUiThread {
                            Log.e(TAG, "IO Exception: ${e.message}", e)
                            Toast.makeText(
                                this@MainActivity,
                                "IO Error: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            alertUserAboutError()
                        }
                    } catch (e: JSONException) {
                        runOnUiThread {
                            Log.e(TAG, "JSON Exception: ${e.message}", e)
                            Toast.makeText(
                                this@MainActivity,
                                "JSON Error: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            alertUserAboutError()
                        }
                    } catch (e: Exception) {
                        runOnUiThread {
                            Log.e(TAG, "Unexpected error: ${e.message}", e)
                            Toast.makeText(
                                this@MainActivity,
                                "Unexpected error: ${e.message}",
                                Toast.LENGTH_LONG
                            ).show()
                            alertUserAboutError()
                        }
                    }
                }
            })
        } else {
            Toast.makeText(this, "Network is unavailable!", Toast.LENGTH_LONG).show()
        }
    }
    
    private fun toggleRefresh() {
        if (binding.progressBar.visibility == View.INVISIBLE) {
            binding.progressBar.visibility = View.VISIBLE
            binding.refreshImageView.visibility = View.INVISIBLE
        } else {
            binding.progressBar.visibility = View.INVISIBLE
            binding.refreshImageView.visibility = View.VISIBLE
        }
    }
    
    private fun updateDisplay() {
        mForecast.current?.let { current ->
            binding.temperatureLabel.text = current.temperatureInt.toString()
            binding.timeLabel.text = "At ${current.formattedTime} it will be"
            binding.humidityValue.text = "${(current.humidity * 100).toInt()}%"
            binding.precipValue.text = "${current.precipChanceInt}%"
            binding.summaryLabel.text = current.summary
            binding.locationLabel.text = mCityName
            binding.iconImageView.setImageResource(current.iconId)
            
            mCurrentTempInF = current.temperature
            mCurrentTempInC = UnitConvert.fahrenheitToCelsius(mCurrentTempInF)

            updateTemperatureDisplay()
        }
    }

    private fun updateTemperatureDisplay() {
        if (isFahrenheit) {
            binding.temperatureLabel.text = round(mCurrentTempInF).toInt().toString()
        } else {
            binding.temperatureLabel.text = round(mCurrentTempInC).toInt().toString()
        }
    }

    private fun updateUnitButtonText() {
        if (isFahrenheit) {
            mButtonUnitConvert.text = "F"
        } else {
            mButtonUnitConvert.text = "C"
        }
    }
    
    private fun parseOpenMeteoData(jsonData: String): Forecast {
        val forecast = Forecast()
        try {
            val rootObject = JSONObject(jsonData)
            Log.d(TAG, "Parsing Open-Meteo JSON data")

            // Parse timezone
            val timezone = rootObject.getString("timezone")

            // Parse current weather
            val currentObject = rootObject.getJSONObject("current")
            val current = Current()
            current.humidity = currentObject.getDouble("relative_humidity_2m") / 100.0
            current.time = currentObject.getLong("time")
            current.temperature = currentObject.getDouble("temperature_2m")
            current.timeZone = timezone
            current.precipChance = currentObject.getDouble("precipitation_probability") / 100.0

            val weatherCode = currentObject.getInt("weather_code")
            current.summary = getWeatherDescription(weatherCode)
            current.icon = mapWmoIconToOurFormat(weatherCode)

            current.cityName = mCityName
            forecast.current = current

            // Parse hourly forecast
            val hourlyObject = rootObject.getJSONObject("hourly")
            val timeArray = hourlyObject.getJSONArray("time")
            val tempArray = hourlyObject.getJSONArray("temperature_2m")
            val codeArray = hourlyObject.getJSONArray("weather_code")
            
            val hours = Array(Math.min(timeArray.length(), 24)) { i ->
                val hour = Hour()
                hour.time = timeArray.getLong(i)
                hour.temperature = tempArray.getDouble(i)
                hour.timezone = timezone
                
                val code = codeArray.getInt(i)
                hour.summary = getWeatherDescription(code)
                hour.icon = mapWmoIconToOurFormat(code)
                
                hour
            }
            forecast.hourlyForecast = hours

            // Parse daily forecast
            val dailyObject = rootObject.getJSONObject("daily")
            val dailyTimeArray = dailyObject.getJSONArray("time")
            val dailyTempMaxArray = dailyObject.getJSONArray("temperature_2m_max")
            val dailyCodeArray = dailyObject.getJSONArray("weather_code")
            
            val days = Array(Math.min(dailyTimeArray.length(), 7)) { i ->
                val day = Day()
                day.time = dailyTimeArray.getLong(i)
                day.temperatureMax = dailyTempMaxArray.getDouble(i)
                
                val code = dailyCodeArray.getInt(i)
                day.summary = getWeatherDescription(code)
                day.icon = mapWmoIconToOurFormat(code)
                
                day.timezone = timezone
                day.cityName = mCityName
                
                day
            }
            forecast.dailyForecast = days

        } catch (e: JSONException) {
            Log.e(TAG, "Error parsing Open-Meteo JSON: ${e.message}", e)
        }

        return forecast
    }
    
    private fun mapWmoIconToOurFormat(weatherCode: Int): String {
        return when (weatherCode) {
            0 -> "clear-day"
            1, 2, 3 -> "partly-cloudy-day"
            45, 48 -> "fog"
            51, 53, 55 -> "rain"
            61, 63, 65 -> "rain"
            66, 67 -> "rain" // Freezing rain
            71, 73, 75 -> "snow"
            77 -> "snow"
            80, 81, 82 -> "rain"
            85, 86 -> "snow"
            95 -> "thunderstorm"
            96, 99 -> "thunderstorm"
            else -> "clear-day"
        }
    }
    
    private fun getWeatherDescription(weatherCode: Int): String {
        return when (weatherCode) {
            0 -> "Clear sky"
            1 -> "Mainly clear"
            2 -> "Partly cloudy"
            3 -> "Overcast"
            45 -> "Fog"
            48 -> "Depositing rime fog"
            51 -> "Light drizzle"
            53 -> "Moderate drizzle"
            55 -> "Dense drizzle"
            61 -> "Slight rain"
            63 -> "Moderate rain"
            65 -> "Heavy rain"
            66 -> "Light freezing rain"
            67 -> "Heavy freezing rain"
            71 -> "Slight snow fall"
            73 -> "Moderate snow fall"
            75 -> "Heavy snow fall"
            77 -> "Snow grains"
            80 -> "Slight rain showers"
            81 -> "Moderate rain showers"
            82 -> "Violent rain showers"
            85 -> "Slight snow showers"
            86 -> "Heavy snow showers"
            95 -> "Thunderstorm"
            96 -> "Thunderstorm with slight hail"
            99 -> "Thunderstorm with heavy hail"
            else -> "Unknown"
        }
    }
    
    private fun isNetworkAvailable(): Boolean {
        val manager = getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val networkInfo = manager.activeNetworkInfo
        return networkInfo != null && networkInfo.isConnected
    }
    
    private fun alertUserAboutError() {
        val dialog = AlertDialogFragment()
        dialog.show(supportFragmentManager, "error_dialog")
    }
    
    override fun onRefresh() {
        getForecast(mLatitude, mLongitude)
        binding.swipeRefreshLayout.isRefreshing = false
    }
    
    private fun setupButtonClickListeners() {
        binding.btnDaily.setOnClickListener { startDailyActivity() }
        binding.btnHourly.setOnClickListener { startHourlyActivity() }
        binding.btnDonate.setOnClickListener { startDonateActivity() }
        binding.btnUnit.setOnClickListener { toggleTemperatureUnit() }
    }
    
    private fun startDailyActivity() {
        val intent = Intent(this, DailyForecastActivity::class.java)
        intent.putExtra(DAILY_FORECAST, mForecast.dailyForecast)
        intent.putExtra(CITY_NAME, mCityName)
        startActivity(intent)
    }
    
    private fun startHourlyActivity() {
        val intent = Intent(this, HourlyForecastActivity::class.java)
        intent.putExtra(HOURLY_FORECAST, mForecast.hourlyForecast)
        startActivity(intent)
    }
    
    private fun startDonateActivity() {
        val intent = Intent(this, DonateActivity::class.java)
        startActivity(intent)
    }
    
    private fun toggleTemperatureUnit() {
        isFahrenheit = !isFahrenheit
        
        // Save preference
        val settings = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = settings.edit()
        editor.putBoolean(KEY_IS_FAHRENHEIT, isFahrenheit)
        editor.apply()

        updateUnitButtonText()
        updateTemperatureDisplay()
    }
    
    private fun createFallbackWeatherData(): Forecast {
        val forecast = Forecast()
        val currentTime = System.currentTimeMillis() / 1000 // Current time in seconds
        
        // Create mock current weather
        val current = Current().apply {
            timeZone = TimeZone.getDefault().id
            time = currentTime
            icon = "partly-cloudy-day"
            temperature = 72.0 // Fahrenheit
            humidity = 0.65 // 65%
            precipChance = 0.15 // 15%
            summary = "Partly cloudy"
            cityName = if (mCityName.isNotEmpty()) mCityName else "Demo City"
        }
        forecast.current = current
        
        // Create mock hourly forecast
        val hours = Array(24) { i ->
            Hour(
                time = currentTime + (i * 3600), // Add i hours
                summary = when (i % 4) {
                    0 -> "Partly cloudy"
                    1 -> "Mostly sunny"
                    2 -> "Clear"
                    else -> "Light rain"
                },
                temperature = 72.0 + (-5 + (i % 10)),
                icon = when (i % 5) {
                    0 -> "partly-cloudy-day"
                    1 -> "clear-day"
                    2 -> "rain"
                    3 -> "cloudy"
                    else -> "partly-cloudy-night"
                },
                timezone = TimeZone.getDefault().id
            )
        }
        forecast.hourlyForecast = hours
        
        // Create mock daily forecast
        val days = Array(7) { i ->
            Day(
                time = currentTime + (i * 86400), // Add i days
                summary = when (i % 4) {
                    0 -> "Partly cloudy throughout the day"
                    1 -> "Mostly sunny"
                    2 -> "Scattered showers"
                    else -> "Clear skies"
                },
                temperatureMax = 75.0 + (-3 + (i % 7)),
                icon = when (i % 5) {
                    0 -> "partly-cloudy-day"
                    1 -> "clear-day"
                    2 -> "rain"
                    3 -> "cloudy"
                    else -> "partly-cloudy-day"
                },
                timezone = TimeZone.getDefault().id,
                cityName = if (mCityName.isNotEmpty()) mCityName else "Demo City"
            )
        }
        forecast.dailyForecast = days
        
        return forecast
    }
} 