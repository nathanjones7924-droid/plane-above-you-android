package com.nathanjones.planeaboveyou.service

import android.annotation.SuppressLint
import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.os.Looper
import com.google.android.gms.location.*
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.Locale
import kotlin.math.*

class AppLocationManager private constructor(private val context: Context) {

    companion object {
        @Volatile
        private var INSTANCE: AppLocationManager? = null
        fun getInstance(context: Context): AppLocationManager {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: AppLocationManager(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val fusedClient = LocationServices.getFusedLocationProviderClient(context)

    private val _location = MutableStateFlow<Location?>(null)
    val location: StateFlow<Location?> = _location.asStateFlow()

    private val _heading = MutableStateFlow<Double?>(null)
    val heading: StateFlow<Double?> = _heading.asStateFlow()

    private val _isAuthorized = MutableStateFlow(false)
    val isAuthorized: StateFlow<Boolean> = _isAuthorized.asStateFlow()

    private val _countryCode = MutableStateFlow("US")
    val countryCode: StateFlow<String> = _countryCode.asStateFlow()

    private val _stateCode = MutableStateFlow("")
    val stateCode: StateFlow<String> = _stateCode.asStateFlow()

    private val _city = MutableStateFlow("")
    val city: StateFlow<String> = _city.asStateFlow()

    private val _gpsAltitude = MutableStateFlow(0.0)
    val gpsAltitude: StateFlow<Double> = _gpsAltitude.asStateFlow()

    private val _cityElevation = MutableStateFlow(0.0)
    val cityElevation: StateFlow<Double> = _cityElevation.asStateFlow()

    private var callback: LocationCallback? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private var headingJob: Job? = null

    fun hasPermission(): Boolean {
        val fine = android.content.pm.PackageManager.PERMISSION_GRANTED ==
            context.checkSelfPermission(android.Manifest.permission.ACCESS_FINE_LOCATION)
        val coarse = android.content.pm.PackageManager.PERMISSION_GRANTED ==
            context.checkSelfPermission(android.Manifest.permission.ACCESS_COARSE_LOCATION)
        return fine || coarse
    }

    @SuppressLint("MissingPermission")
    fun startTracking() {
        if (!hasPermission()) return
        _isAuthorized.value = true

        val request = LocationRequest.Builder(
            Priority.PRIORITY_BALANCED_POWER_ACCURACY, 10000L
        ).setMinUpdateIntervalMillis(5000L).build()

        val cb = object : LocationCallback() {
            override fun onLocationResult(result: LocationResult) {
                result.lastLocation?.let { loc ->
                    _location.value = loc
                    _gpsAltitude.value = loc.altitude
                    reverseGeocode(loc)
                }
            }
        }
        callback = cb
        fusedClient.requestLocationUpdates(request, cb, Looper.getMainLooper())
        startHeadingSimulation()
    }

    fun stopTracking() {
        callback?.let { fusedClient.removeLocationUpdates(it) }
        callback = null
        headingJob?.cancel()
    }

    private fun reverseGeocode(loc: Location) {
        scope.launch(Dispatchers.IO) {
            try {
                val gc = Geocoder(context, Locale.getDefault())
                val addresses = gc.getFromLocation(loc.latitude, loc.longitude, 1)
                addresses?.firstOrNull()?.let { addr ->
                    _city.value = addr.locality ?: addr.subAdminArea ?: ""
                    _countryCode.value = addr.countryCode?.uppercase() ?: "US"
                    val admin = addr.adminArea ?: ""
                    _stateCode.value = stateAbbreviation(admin)
                    if (_city.value.isNotEmpty()) {
                        fetchCityElevation()
                    }
                }
            } catch (_: Exception) { }
        }
    }

    private fun stateAbbreviation(adminArea: String): String {
        val map = mapOf(
            "Alabama" to "AL", "Alaska" to "AK", "Arizona" to "AZ", "Arkansas" to "AR",
            "California" to "CA", "Colorado" to "CO", "Connecticut" to "CT", "Delaware" to "DE",
            "Florida" to "FL", "Georgia" to "GA", "Hawaii" to "HI", "Idaho" to "ID",
            "Illinois" to "IL", "Indiana" to "IN", "Iowa" to "IA", "Kansas" to "KS",
            "Kentucky" to "KY", "Louisiana" to "LA", "Maine" to "ME", "Maryland" to "MD",
            "Massachusetts" to "MA", "Michigan" to "MI", "Minnesota" to "MN", "Mississippi" to "MS",
            "Missouri" to "MO", "Montana" to "MT", "Nebraska" to "NE", "Nevada" to "NV",
            "New Hampshire" to "NH", "New Jersey" to "NJ", "New Mexico" to "NM", "New York" to "NY",
            "North Carolina" to "NC", "North Dakota" to "ND", "Ohio" to "OH", "Oklahoma" to "OK",
            "Oregon" to "OR", "Pennsylvania" to "PA", "Rhode Island" to "RI", "South Carolina" to "SC",
            "South Dakota" to "SD", "Tennessee" to "TN", "Texas" to "TX", "Utah" to "UT",
            "Vermont" to "VT", "Virginia" to "VA", "Washington" to "WA", "West Virginia" to "WV",
            "Wisconsin" to "WI", "Wyoming" to "WY"
        )
        return map[adminArea] ?: ""
    }

    private fun fetchCityElevation() {
        scope.launch(Dispatchers.IO) {
            try {
                val lat = _location.value?.latitude ?: return@launch
                val lon = _location.value?.longitude ?: return@launch
                val url = "https://api.open-elevation.com/api/v1/lookup?locations=$lat,$lon"
                val req = Request.Builder().url(url).build()
                val resp = OkHttpClient().newCall(req).execute()
                val json = JSONObject(resp.body?.string() ?: "")
                val results = json.getJSONArray("results")
                if (results.length() > 0) {
                    _cityElevation.value = results.getJSONObject(0).getDouble("elevation")
                }
            } catch (_: Exception) { }
        }
    }

    private fun startHeadingSimulation() {
        headingJob?.cancel()
        headingJob = scope.launch {
            var currentHeading = 0.0
            while (isActive) {
                delay(100)
                currentHeading = (currentHeading + 0.5) % 360.0
                _heading.value = currentHeading
            }
        }
    }
}
