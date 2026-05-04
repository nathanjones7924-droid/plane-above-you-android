package com.nathanjones.planeaboveyou.service

sealed class FlightError : Exception() {
    object InvalidURL : FlightError()
    object InvalidResponse : FlightError()
    object RateLimited : FlightError()
    data class HttpError(val code: Int) : FlightError()
    object NoData : FlightError()

    override val message: String
        get() = when (this) {
            is InvalidURL -> "Invalid API URL"
            is InvalidResponse -> "Invalid server response"
            is RateLimited -> "Too many requests — please wait a moment"
            is HttpError -> "Server error ($code)"
            is NoData -> "No flight data available"
        }
}
