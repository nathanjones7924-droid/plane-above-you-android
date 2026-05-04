package com.nathanjones.planeaboveyou.viewmodel

import android.app.Application
import android.location.Location
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.nathanjones.planeaboveyou.model.CountryBounds
import com.nathanjones.planeaboveyou.model.Flight
import com.nathanjones.planeaboveyou.model.USStateBounds
import com.nathanjones.planeaboveyou.service.AppLocationManager
import com.nathanjones.planeaboveyou.service.FlightError
import com.nathanjones.planeaboveyou.service.FlightService
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.math.*

class FlightViewModel(application: Application) : AndroidViewModel(application) {

    data class FlightSnapshot(val latitude: Double, val longitude: Double, val timestamp: Long)

    private val flightService = FlightService()
    private val locationManager = AppLocationManager.getInstance(application)

    private val _allFlights = MutableStateFlow<List<Flight>>(emptyList())
    val allFlights: StateFlow<List<Flight>> = _allFlights.asStateFlow()

    private val _mapFlights = MutableStateFlow<List<Flight>>(emptyList())
    val mapFlights: StateFlow<List<Flight>> = _mapFlights.asStateFlow()

    private val _planesAbove = MutableStateFlow<List<Flight>>(emptyList())
    val planesAbove: StateFlow<List<Flight>> = _planesAbove.asStateFlow()

    private val _selectedFlight = MutableStateFlow<Flight?>(null)
    val selectedFlight: StateFlow<Flight?> = _selectedFlight.asStateFlow()

    private val _showingDetail = MutableStateFlow(false)
    val showingDetail: StateFlow<Boolean> = _showingDetail.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    private val _mapCameraPosition = MutableStateFlow(
        CameraPosition.builder().target(LatLng(39.8283, -98.5795)).zoom(3f).build()
    )
    val mapCameraPosition: StateFlow<CameraPosition> = _mapCameraPosition.asStateFlow()

    private val _countryBounds = MutableStateFlow<CountryBounds?>(null)

    val hasPlaneAbove: Boolean get() = _planesAbove.value.isNotEmpty()
    val planesAboveIDs: Set<String> get() = _planesAbove.value.map { it.id }.toSet()

    private val _fovMiles = MutableStateFlow(5.0)
    val fovRadiusMiles: StateFlow<Double> = _fovMiles.asStateFlow()
    val fovRadiusMeters: Double get() = _fovMiles.value * 1609.34

    private var refreshJob: Job? = null
    private var autoRefreshJob: Job? = null
    private var isRefreshing = false
    private var hasStarted = false
    private var previousSnapshots = mutableMapOf<String, FlightSnapshot>()
    private var displayHeadings = mutableMapOf<String, Double>()
    private var previouslyAboveIDs = emptySet<String>()

    val location = locationManager.location
    val heading = locationManager.heading
    val isAuthorized = locationManager.isAuthorized
    val city = locationManager.city
    val countryCode = locationManager.countryCode
    val stateCode = locationManager.stateCode

    init {
        viewModelScope.launch {
            location.collect { loc ->
                loc?.let { refreshIfNeededForLocation() }
            }
        }
    }

    fun start() {
        if (hasStarted) return
        hasStarted = true
        locationManager.startTracking()
        startAutoRefresh()
    }

    fun stop() {
        hasStarted = false
        autoRefreshJob?.cancel()
        locationManager.stopTracking()
    }

    fun refresh() {
        if (isRefreshing) return
        viewModelScope.launch { doRefresh() }
    }

    private suspend fun refreshIfNeededForLocation() {
        delay(500)
        if (hasStarted) doRefresh()
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        autoRefreshJob = viewModelScope.launch {
            doRefresh()
            var fastTrack = true
            var count = 0
            while (isActive) {
                delay(if (fastTrack && count < 3) 1000L else 20000L)
                count++
                if (count >= 3) fastTrack = false
                doRefresh()
            }
        }
    }

    private suspend fun doRefresh() {
        if (!isAuthorized.value) return
        if (isRefreshing) return
        isRefreshing = true
        _isLoading.value = true
        _errorMessage.value = null

        try {
            val loc = location.value
            val cc = countryCode.value
            val bounds: CountryBounds = if (cc.equals("US", true) && stateCode.value.isNotEmpty()) {
                USStateBounds.boundsForStateAndNeighbors(stateCode.value)
                    ?: CountryBounds.forCountryCode(cc)
                    ?: CountryBounds(-90.0, 90.0, -180.0, 180.0)
            } else {
                CountryBounds.forCountryCode(cc)
                    ?: CountryBounds(-90.0, 90.0, -180.0, 180.0)
            }
            _countryBounds.value = bounds

            val flights = flightService.fetchFlights(bounds)
            updateDisplayHeadings(flights)
            _allFlights.value = flights
            _mapFlights.value = if (flights.size > 100 && loc != null) {
                flights.sortedBy { it.distanceFrom(loc) }.take(100)
            } else flights

            loc?.let { userLoc ->
                val nearby = flights.filter { it.isAbove(userLoc, _fovMiles.value) }
                _planesAbove.value = nearby
                previouslyAboveIDs = nearby.map { it.id }.toSet()
            }

            _isLoading.value = false
        } catch (e: CancellationException) {
            _isLoading.value = false
        } catch (e: Exception) {
            _isLoading.value = false
            _errorMessage.value = when (e) {
                is FlightError -> e.message
                else -> e.localizedMessage ?: "An error occurred"
            }
        } finally {
            isRefreshing = false
        }
    }

    fun setFOVMiles(miles: Double) {
        _fovMiles.value = miles.coerceIn(1.0, 50.0)
    }

    fun focusOnCountry() {
        val bounds = _countryBounds.value ?: return
        val center = LatLng((bounds.minLat + bounds.maxLat) / 2, (bounds.minLon + bounds.maxLon) / 2)
        val latSpan = (bounds.maxLat - bounds.minLat).coerceAtLeast(0.01)
        val lonSpan = (bounds.maxLon - bounds.minLon).coerceAtLeast(0.01)
        val maxSpan = max(latSpan, lonSpan)
        val zoom = when {
            maxSpan > 40 -> 3f
            maxSpan > 20 -> 4f
            maxSpan > 10 -> 5f
            maxSpan > 5 -> 6f
            maxSpan > 2 -> 7f
            maxSpan > 1 -> 8f
            else -> 9f
        }
        _mapCameraPosition.value = CameraPosition.builder().target(center).zoom(zoom).build()
    }

    fun focusOnPlane(flight: Flight) {
        _mapCameraPosition.value = CameraPosition.builder()
            .target(LatLng(flight.latitude, flight.longitude))
            .zoom(12f)
            .build()
    }

    fun focusAndSelectFlight(flight: Flight) {
        focusOnPlane(flight)
        viewModelScope.launch {
            delay(200)
            _selectedFlight.value = flight
            _showingDetail.value = true
        }
    }

    fun selectFlight(flight: Flight) {
        _selectedFlight.value = flight
        _showingDetail.value = true
    }

    fun dismissDetail() {
        _showingDetail.value = false
    }

    fun displayHeading(flight: Flight): Double {
        return displayHeadings[flight.id] ?: flight.heading
    }

    private fun updateDisplayHeadings(flights: List<Flight>) {
        val nextSnapshots = mutableMapOf<String, FlightSnapshot>()
        val nextDisplay = mutableMapOf<String, Double>()
        for (flight in flights) {
            val current = FlightSnapshot(flight.latitude, flight.longitude, flight.timestamp)
            nextSnapshots[flight.id] = current
            val previous = previousSnapshots[flight.id]
            if (previous == null) {
                nextDisplay[flight.id] = normalizedDegrees(flight.heading)
                continue
            }
            val from = Location("").apply { latitude = previous.latitude; longitude = previous.longitude }
            val to = Location("").apply { latitude = current.latitude; longitude = current.longitude }
            val distanceMeters = from.distanceTo(to).toDouble()
            val deltaTime = (current.timestamp - previous.timestamp).coerceAtLeast(0)
            if (distanceMeters >= 120 && deltaTime > 0 && deltaTime <= 300) {
                val movementBearing = bearingDegrees(previous.latitude, previous.longitude, current.latitude, current.longitude)
                val weight = (distanceMeters / 2000.0).coerceIn(0.25, 0.9)
                val blended = blendedAngleDegrees(normalizedDegrees(flight.heading), normalizedDegrees(movementBearing), weight)
                nextDisplay[flight.id] = blended
            } else {
                nextDisplay[flight.id] = normalizedDegrees(flight.heading)
            }
        }
        previousSnapshots = nextSnapshots
        displayHeadings = nextDisplay
    }

    private fun normalizedDegrees(value: Double): Double {
        var d = value % 360.0
        if (d < 0) d += 360.0
        return d
    }

    private fun blendedAngleDegrees(a: Double, b: Double, bWeight: Double): Double {
        val wB = bWeight.coerceIn(0.0, 1.0)
        val wA = 1.0 - wB
        val aRad = Math.toRadians(a)
        val bRad = Math.toRadians(b)
        val x = cos(aRad) * wA + cos(bRad) * wB
        val y = sin(aRad) * wA + sin(bRad) * wB
        return normalizedDegrees(Math.toDegrees(atan2(y, x)))
    }

    private fun bearingDegrees(fromLat: Double, fromLon: Double, toLat: Double, toLon: Double): Double {
        val lat1 = Math.toRadians(fromLat)
        val lat2 = Math.toRadians(toLat)
        val dLon = Math.toRadians(toLon - fromLon)
        val y = sin(dLon) * cos(lat2)
        val x = cos(lat1) * sin(lat2) - sin(lat1) * cos(lat2) * cos(dLon)
        return normalizedDegrees(Math.toDegrees(atan2(y, x)))
    }

    override fun onCleared() {
        super.onCleared()
        stop()
    }
}
