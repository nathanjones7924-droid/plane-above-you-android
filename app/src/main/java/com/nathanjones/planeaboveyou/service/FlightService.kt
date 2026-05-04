package com.nathanjones.planeaboveyou.service

import com.nathanjones.planeaboveyou.model.CountryBounds
import com.nathanjones.planeaboveyou.model.Flight
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class FlightService {
    private val feedURL = "https://data-cloud.flightradar24.com/zones/fcgi/feed.js"
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()
    private val defaultLimit = 1500

    suspend fun fetchFlights(bounds: CountryBounds, limit: Int = defaultLimit): List<Flight> =
        withContext(Dispatchers.IO) { fetchFlightsInternal(bounds, limit) }

    suspend fun fetchFlightsTiled(
        bounds: CountryBounds,
        tileRows: Int = 2,
        tileCols: Int = 2,
        limitPerTile: Int = 1500
    ): List<Flight> = withContext(Dispatchers.IO) {
        val rows = tileRows.coerceAtLeast(1)
        val cols = tileCols.coerceAtLeast(1)
        val tiles = splitBounds(bounds, rows, cols)
        val deferred = tiles.map { tile ->
            async { fetchFlightsInternal(tile, limitPerTile) }
        }
        val all = deferred.awaitAll().flatten()
        all.distinctBy { it.id }
    }

    private suspend fun fetchFlightsInternal(bounds: CountryBounds, limit: Int): List<Flight> {
        val boundsParam = "${bounds.maxLat},${bounds.minLat},${bounds.minLon},${bounds.maxLon}"
        val clampedLimit = limit.coerceIn(100, 3000)
        val url = "$feedURL?faa=1&satellite=1&mlat=1&flarm=1&adsb=1&gnd=0&air=1&vehicles=0&estimated=1&gliders=0&stats=0&limit=$clampedLimit&bounds=$boundsParam"

        val request = Request.Builder()
            .url(url)
            .addHeader("User-Agent", "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/120.0.0.0 Safari/537.36")
            .addHeader("Origin", "https://www.flightradar24.com")
            .addHeader("Referer", "https://www.flightradar24.com")
            .addHeader("Accept", "application/json")
            .build()

        val response = client.newCall(request).execute()
        if (!response.isSuccessful) {
            if (response.code == 429) throw FlightError.RateLimited
            throw FlightError.HttpError(response.code)
        }
        val body = response.body?.string() ?: throw FlightError.InvalidResponse
        return parseFR24Feed(body)
    }

    private fun parseFR24Feed(jsonString: String): List<Flight> {
        val json = JSONObject(jsonString)
        val metaKeys = setOf("full_count", "version", "stats")
        val flights = mutableListOf<Flight>()
        json.keys().forEach { key ->
            if (metaKeys.contains(key)) return@forEach
            val arr = json.optJSONArray(key) ?: return@forEach
            if (arr.length() < 17) return@forEach
            val icao24 = arr.optString(0, "")
            val lat = arr.optDouble(1, 0.0)
            val lon = arr.optDouble(2, 0.0)
            val heading = arr.optDouble(3, 0.0)
            val altitude = arr.optInt(4, 0)
            val speed = arr.optInt(5, 0)
            val squawk = arr.optString(6, "")
            val aircraftType = arr.optString(8, "")
            val registration = arr.optString(9, "")
            val timestamp = arr.optDouble(10, 0.0).toLong()
            val origin = arr.optString(11, "")
            val destination = arr.optString(12, "")
            val callsign = arr.optString(13, "")
            val verticalRate = arr.optInt(15, 0)
            val airlineICAO = if (arr.length() > 16) arr.optString(16, "") else ""
            if (lat == 0.0 && lon == 0.0) return@forEach
            val onGround = altitude == 0
            if (onGround) return@forEach
            flights.add(
                Flight(
                    id = key,
                    icao24 = icao24,
                    callsign = callsign,
                    latitude = lat,
                    longitude = lon,
                    heading = heading,
                    altitude = altitude,
                    groundSpeed = speed,
                    squawk = squawk,
                    aircraftType = aircraftType,
                    registration = registration,
                    originAirport = origin,
                    destinationAirport = destination,
                    airlineICAO = airlineICAO,
                    onGround = onGround,
                    verticalRate = verticalRate,
                    timestamp = timestamp
                )
            )
        }
        return flights
    }

    private fun splitBounds(bounds: CountryBounds, rows: Int, cols: Int): List<CountryBounds> {
        val latStep = (bounds.maxLat - bounds.minLat) / rows
        val lonStep = (bounds.maxLon - bounds.minLon) / cols
        val result = mutableListOf<CountryBounds>()
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                val minLat = bounds.minLat + row * latStep
                val maxLat = if (row == rows - 1) bounds.maxLat else minLat + latStep
                val minLon = bounds.minLon + col * lonStep
                val maxLon = if (col == cols - 1) bounds.maxLon else minLon + lonStep
                result.add(CountryBounds(minLat, maxLat, minLon, maxLon))
            }
        }
        return result
    }
}
