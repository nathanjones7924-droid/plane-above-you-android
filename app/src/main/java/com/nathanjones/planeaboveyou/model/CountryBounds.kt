package com.nathanjones.planeaboveyou.model

import com.google.android.gms.maps.model.LatLng
import android.location.Location

data class CountryBounds(
    val minLat: Double,
    val maxLat: Double,
    val minLon: Double,
    val maxLon: Double
) {
    companion object {
        fun forCountryCode(code: String): CountryBounds? {
            return when (code.uppercase()) {
                "US" -> CountryBounds(24.396308, 49.384358, -125.0, -66.93457)
                "GB" -> CountryBounds(49.9, 58.7, -8.2, 1.8)
                "DE" -> CountryBounds(47.3, 55.1, 5.9, 15.0)
                "FR" -> CountryBounds(41.3, 51.1, -5.1, 9.6)
                "CA" -> CountryBounds(41.7, 83.1, -141.0, -52.6)
                "AU" -> CountryBounds(-43.6, -10.7, 113.2, 153.6)
                "JP" -> CountryBounds(24.0, 46.0, 123.0, 146.0)
                "IN" -> CountryBounds(6.7, 35.5, 68.2, 97.4)
                "BR" -> CountryBounds(-33.7, 5.3, -73.9, -34.8)
                "MX" -> CountryBounds(14.5, 32.7, -118.4, -86.7)
                "IT" -> CountryBounds(36.6, 47.1, 6.6, 18.5)
                "ES" -> CountryBounds(36.0, 43.8, -9.3, 3.3)
                "NL" -> CountryBounds(50.8, 53.5, 3.4, 7.1)
                "SE" -> CountryBounds(55.3, 69.1, 11.1, 24.2)
                "NO" -> CountryBounds(58.0, 71.2, 4.6, 31.1)
                "KR" -> CountryBounds(33.1, 38.6, 125.1, 131.9)
                "AE" -> CountryBounds(22.6, 26.1, 51.6, 56.4)
                else -> null
            }
        }
    }
}
