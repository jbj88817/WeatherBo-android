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
        
        lateinit var mButtonUnitConvert: Button
    }

    private var mLongitude: Double = 0.0
    private var mLatitude: Double = 0.0
    private var mCityName: String = ""
    private var mCurrentTempInF: Double = 0.0
    private var mCurrentTempInC: Double = 0.0

    private lateinit var fusedLocationClient: FusedLocationProviderClient
    private lateinit var locationRequest: LocationRequest
    private lateinit var locationCallback: LocationCallback

    private lateinit var mForecast: Forecast
    private lateinit var binding: ActivityMainBinding
    private lateinit var mAdView: AdView

    private var apiKeyIndex = 0 // Track which API key we're using

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        
        mButtonUnitConvert = findViewById(R.id.btn_unit)
        
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
        // Using OpenWeatherMap API instead of forecast.io (Dark Sky) which is deprecated
        // Try multiple API keys in case one is not working
        val apiKeys = arrayOf(
            "8c99ceea4e74b9b23a44b929973ad718",
        )
        val apiKey = apiKeys[apiKeyIndex] // Use the currently selected key
        
        val forecastUrl = "https://api.openweathermap.org/data/2.5/onecall?lat=$latitude&lon=$longitude&exclude=minutely&units=imperial&appid=$apiKey"
        
        Log.d(TAG, "API URL: $forecastUrl (using API key index: $apiKeyIndex)")
        
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
                                    mForecast = parseOpenWeatherMapData(jsonData)
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
                                
                                // If we get a 401 unauthorized error, try with a different API key
                                if (response.code == 401) {
                                    val currentKeyIndex = apiKeyIndex
                                    if (currentKeyIndex < apiKeys.size - 1) {
                                        // Try the next API key
                                        apiKeyIndex++
                                        Log.d(TAG, "Trying with next API key (index: $apiKeyIndex)")
                                        Toast.makeText(
                                            this@MainActivity,
                                            "API key unauthorized, trying another key...",
                                            Toast.LENGTH_SHORT
                                        ).show()
                                        fetchWeatherData(latitude, longitude)
                                        return@runOnUiThread
                                    } else {
                                        // We've tried all API keys, use fallback data
                                        Log.d(TAG, "All API keys failed, using fallback weather data")
                                        Toast.makeText(
                                            this@MainActivity,
                                            "Using demo weather data (API key issue)",
                                            Toast.LENGTH_LONG
                                        ).show()
                                        mForecast = createFallbackWeatherData()
                                        updateDisplay()
                                    }
                                } else {
                                    alertUserAboutError()
                                }
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
        }
    }
    
    private fun parseOpenWeatherMapData(jsonData: String): Forecast {
        val forecast = Forecast()
        try {
            val rootObject = JSONObject(jsonData)
            Log.d(TAG, "Parsing OpenWeatherMap JSON data")
            
            // Parse timezone
            val timezone = rootObject.getString("timezone")
            
            // Parse current weather
            val currentObject = rootObject.getJSONObject("current")
            val current = Current()
            current.humidity = currentObject.getDouble("humidity") / 100.0
            current.time = currentObject.getLong("dt")
            current.temperature = currentObject.getDouble("temp")
            current.timeZone = timezone
            current.precipChance = if (currentObject.has("pop")) currentObject.getDouble("pop") else 0.0
            
            // Get weather description and icon
            val weatherArray = currentObject.getJSONArray("weather")
            if (weatherArray.length() > 0) {
                val weatherObject = weatherArray.getJSONObject(0)
                current.summary = weatherObject.getString("description")
                // Map OpenWeatherMap icons to our custom icons
                current.icon = mapOpenWeatherIconToOurFormat(weatherObject.getString("icon"))
            }
            
            current.cityName = mCityName
            forecast.current = current
            
            // Parse hourly forecast
            val hourlyArray = rootObject.getJSONArray("hourly")
            val hours = Array(24) { i ->
                val hourObject = hourlyArray.getJSONObject(i)
                val hour = Hour()
                hour.time = hourObject.getLong("dt")
                hour.temperature = hourObject.getDouble("temp")
                hour.timezone = timezone
                
                val precipProbability = if (hourObject.has("pop")) hourObject.getDouble("pop") else 0.0
                
                val weatherData = hourObject.getJSONArray("weather").getJSONObject(0)
                hour.summary = weatherData.getString("description")
                hour.icon = mapOpenWeatherIconToOurFormat(weatherData.getString("icon"))
                
                hour
            }
            forecast.hourlyForecast = hours
            
            // Parse daily forecast
            val dailyArray = rootObject.getJSONArray("daily")
            val days = Array(Math.min(dailyArray.length(), 7)) { i ->
                val dayObject = dailyArray.getJSONObject(i)
                val day = Day()
                day.time = dayObject.getLong("dt")
                
                val tempObject = dayObject.getJSONObject("temp")
                day.temperatureMax = tempObject.getDouble("max")
                
                val weatherData = dayObject.getJSONArray("weather").getJSONObject(0)
                day.summary = weatherData.getString("description")
                day.icon = mapOpenWeatherIconToOurFormat(weatherData.getString("icon"))
                
                day.timezone = timezone
                day.cityName = mCityName
                
                day
            }
            forecast.dailyForecast = days
            
        } catch (e: JSONException) {
            Log.e(TAG, "Error parsing OpenWeatherMap JSON: ${e.message}", e)
        }
        
        return forecast
    }
    
    private fun mapOpenWeatherIconToOurFormat(openWeatherIcon: String): String {
        return when {
            openWeatherIcon.contains("01") -> "clear-day" // clear sky
            openWeatherIcon.contains("02") -> "partly-cloudy-day" // few clouds
            openWeatherIcon.contains("03") -> "partly-cloudy-day" // scattered clouds
            openWeatherIcon.contains("04") -> "cloudy" // broken clouds
            openWeatherIcon.contains("09") -> "rain" // shower rain
            openWeatherIcon.contains("10") -> "rain" // rain
            openWeatherIcon.contains("11") -> "thunderstorm" // thunderstorm
            openWeatherIcon.contains("13") -> "snow" // snow
            openWeatherIcon.contains("50") -> "fog" // mist
            else -> "clear-day" // default
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
        if (mButtonUnitConvert.text == "F") {
            mButtonUnitConvert.text = "C"
            binding.temperatureLabel.text = round(mCurrentTempInC).toInt().toString()
        } else {
            mButtonUnitConvert.text = "F"
            binding.temperatureLabel.text = round(mCurrentTempInF).toInt().toString()
        }
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