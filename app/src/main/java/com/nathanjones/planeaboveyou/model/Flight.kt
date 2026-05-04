package com.nathanjones.planeaboveyou.model

import android.location.Location
import com.google.android.gms.maps.model.LatLng

data class Flight(
    val id: String,
    val icao24: String,
    val callsign: String,
    val latitude: Double,
    val longitude: Double,
    val heading: Double,
    val altitude: Int,
    val groundSpeed: Int,
    val squawk: String,
    val aircraftType: String,
    val registration: String,
    val originAirport: String,
    val destinationAirport: String,
    val airlineICAO: String,
    val onGround: Boolean,
    val verticalRate: Int,
    val timestamp: Long
) {
    val coordinate: LatLng get() = LatLng(latitude, longitude)
    val altitudeFeet: Int get() = altitude
    val speedKnots: Int get() = groundSpeed
    val speedMph: Int get() = (groundSpeed * 1.15078).toInt()
    val trueTrack: Double? get() = heading
    val formattedCallsign: String get() = callsign.trim().ifEmpty { "Unknown" }
    val formattedAircraftType: String get() = if (aircraftType.isEmpty()) "Unknown" else AircraftTypes.nameFor(aircraftType)
    val formattedRoute: String get() {
        val from = originAirport.ifEmpty { "???" }
        val to = destinationAirport.ifEmpty { "???" }
        return if (from == "???" && to == "???") "Unknown Route" else "$from → $to"
    }
    val airlineName: String get() = if (airlineICAO.isEmpty()) "Unknown Airline" else Airlines.nameFor(airlineICAO)

    fun distanceFrom(location: Location): Double {
        val planeLocation = Location("").apply {
            latitude = this@Flight.latitude
            longitude = this@Flight.longitude
        }
        return planeLocation.distanceTo(location) / 1609.34
    }

    fun isAbove(location: Location, radiusMiles: Double = 2.0): Boolean {
        return !onGround && distanceFrom(location) <= radiusMiles
    }
}
