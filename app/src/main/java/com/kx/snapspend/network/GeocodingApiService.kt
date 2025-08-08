package com.kx.snapspend.network // Use your actual package name

import com.google.gson.annotations.SerializedName
import retrofit2.Response
import retrofit2.http.GET
import retrofit2.http.Query

// Data classes to hold the important parts of the JSON response from Google
data class GeocodingResponse(val results: List<GeocodingResult>)
data class GeocodingResult(val types: List<String>)

// Defines the API endpoint we want to call
interface GeocodingApiService {
    @GET("maps/api/geocode/json")
    suspend fun reverseGeocode(
        @Query("latlng") latlng: String,
        @Query("key") apiKey: String
    ): Response<GeocodingResponse>
}