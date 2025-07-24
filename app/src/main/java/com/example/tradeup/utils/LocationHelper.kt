package com.example.tradeup.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.*
import kotlin.math.*

object LocationHelper {

    private const val TAG = "LocationHelper"

    data class LocationData(
        val latitude: Double,
        val longitude: Double,
        val address: String
    )




    /**
     * Get current location using FusedLocationProviderClient
     */
    fun getCurrentLocation(
        context: Context,
        onSuccess: (LocationData) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (!hasLocationPermission(context)) {
            onFailure("Location permission not granted")
            return
        }

        if (!isLocationEnabled(context)) {
            onFailure("Please enable location services")
            return
        }

        val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        Log.d(TAG, "Got location: ${location.latitude}, ${location.longitude}")
                        getAddressFromLocation(context, location.latitude, location.longitude) { address ->
                            val locationData = LocationData(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                address = address
                            )
                            onSuccess(locationData)
                        }
                    } else {
                        Log.w(TAG, "Location is null")
                        onFailure("Unable to get current location. Please try again.")
                    }
                }
                .addOnFailureListener { exception ->
                    Log.e(TAG, "Location error: ${exception.message}")
                    onFailure("Location error: ${exception.message}")
                }
        } catch (e: SecurityException) {
            Log.e(TAG, "Security exception: ${e.message}")
            onFailure("Location permission denied")
        }
    }

    /**
     * Convert coordinates to address string
     */
    fun getAddressFromLocation(
        context: Context,
        latitude: Double,
        longitude: Double,
        onResult: (String) -> Unit
    ) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())

            // For API 33+ use the new geocoder API
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocation(latitude, longitude, 1) { addresses ->
                    if (addresses.isNotEmpty()) {
                        val address = addresses[0]
                        val addressText = buildAddressString(address)
                        onResult(addressText)
                    } else {
                        onResult("Ho Chi Minh City, Vietnam")
                    }
                }
            } else {
                // For older API versions
                @Suppress("DEPRECATION")
                val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)

                if (!addresses.isNullOrEmpty()) {
                    val address = addresses[0]
                    val addressText = buildAddressString(address)
                    onResult(addressText)
                } else {
                    onResult("Ho Chi Minh City, Vietnam")
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Geocoding error: ${e.message}")
            onResult("Ho Chi Minh City, Vietnam")
        }
    }

    /**
     * Convert address string to coordinates
     */
    fun getCoordinatesFromAddress(
        context: Context,
        address: String,
        onResult: (Double?, Double?) -> Unit
    ) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())

            // For API 33+ use the new geocoder API
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
                geocoder.getFromLocationName(address, 1) { addresses ->
                    if (addresses.isNotEmpty()) {
                        val location = addresses[0]
                        onResult(location.latitude, location.longitude)
                    } else {
                        onResult(null, null)
                    }
                }
            } else {
                // For older API versions
                @Suppress("DEPRECATION")
                val addresses = geocoder.getFromLocationName(address, 1)

                if (!addresses.isNullOrEmpty()) {
                    val location = addresses[0]
                    onResult(location.latitude, location.longitude)
                } else {
                    onResult(null, null)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Address geocoding error: ${e.message}")
            onResult(null, null)
        }
    }

    /**
     * Build a readable address string from Address object
     */
    private fun buildAddressString(address: Address): String {
        return buildString {
            if (address.thoroughfare != null) append(address.thoroughfare)
            if (address.subAdminArea != null) {
                if (isNotEmpty()) append(", ")
                append(address.subAdminArea)
            }
            if (address.adminArea != null) {
                if (isNotEmpty()) append(", ")
                append(address.adminArea)
            }
            if (address.countryName != null) {
                if (isNotEmpty()) append(", ")
                append(address.countryName)
            }
        }.ifEmpty { "Ho Chi Minh City, Vietnam" }
    }

    /**
     * Calculate distance between two points using Haversine formula
     */
    fun calculateDistance(lat1: Double, lon1: Double, lat2: Double, lon2: Double): Double {
        val earthRadius = 6371.0 // Earth's radius in kilometers

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    /**
     * Calculate distance and return formatted string
     */
    fun getDistanceString(lat1: Double, lon1: Double, lat2: Double, lon2: Double): String {
        val distance = calculateDistance(lat1, lon1, lat2, lon2)
        return when {
            distance < 1 -> "${(distance * 1000).toInt()}m away"
            distance < 10 -> String.format("%.1f km away", distance)
            else -> "${distance.toInt()} km away"
        }
    }

    /**
     * Check if location permissions are granted
     */
    fun hasLocationPermission(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(
            context, Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context, Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    /**
     * Check if location services are enabled
     */
    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * Check if GPS provider specifically is enabled
     */
    fun isGpsEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)
    }

    /**
     * Check if network provider is enabled
     */
    fun isNetworkEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    /**
     * Get required location permissions array
     */
    fun getLocationPermissions(): Array<String> {
        return arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
    }

    /**
     * Default location for Ho Chi Minh City, Vietnam
     */
    fun getDefaultLocation(): LocationData {
        return LocationData(
            latitude = 10.8231,
            longitude = 106.6297,
            address = "Ho Chi Minh City, Vietnam"
        )
    }

    /**
     * Check if coordinates are within Vietnam bounds (rough approximation)
     */
    fun isWithinVietnam(latitude: Double, longitude: Double): Boolean {
        // Rough bounds for Vietnam
        return latitude in 8.5..23.5 && longitude in 102.0..110.0
    }

    /**
     * Check if two locations are within a certain radius (in km)
     */
    fun isWithinRadius(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double,
        radiusKm: Double
    ): Boolean {
        return calculateDistance(lat1, lon1, lat2, lon2) <= radiusKm
    }
}