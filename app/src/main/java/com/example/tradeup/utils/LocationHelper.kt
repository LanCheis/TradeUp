// app/src/main/java/com/example/tradeup/utils/LocationHelper.kt

package com.example.tradeup.utils

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Address
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import androidx.core.app.ActivityCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import java.util.*
import kotlin.math.*

object LocationHelper {

    data class LocationData(
        val latitude: Double,
        val longitude: Double,
        val address: String
    )

    fun getCurrentLocation(
        context: Context,
        onSuccess: (LocationData) -> Unit,
        onFailure: (String) -> Unit
    ) {
        if (!hasLocationPermission(context)) {
            onFailure("Location permission not granted")
            return
        }

        val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location: Location? ->
                    if (location != null) {
                        getAddressFromLocation(context, location.latitude, location.longitude) { address ->
                            val locationData = LocationData(
                                latitude = location.latitude,
                                longitude = location.longitude,
                                address = address
                            )
                            onSuccess(locationData)
                        }
                    } else {
                        onFailure("Unable to get current location")
                    }
                }
                .addOnFailureListener { exception ->
                    onFailure("Location error: ${exception.message}")
                }
        } catch (e: SecurityException) {
            onFailure("Location permission denied")
        }
    }

    fun getAddressFromLocation(
        context: Context,
        latitude: Double,
        longitude: Double,
        onResult: (String) -> Unit
    ) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses: List<Address>? = geocoder.getFromLocation(latitude, longitude, 1)

            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                val addressText = buildString {
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
                }
                onResult(addressText.ifEmpty { "Unknown location" })
            } else {
                onResult("Ho Chi Minh City, Vietnam") // Default location
            }
        } catch (e: Exception) {
            onResult("Ho Chi Minh City, Vietnam") // Default location
        }
    }

    fun calculateDistance(
        lat1: Double, lon1: Double,
        lat2: Double, lon2: Double
    ): Double {
        val earthRadius = 6371.0 // Earth's radius in kilometers

        val dLat = Math.toRadians(lat2 - lat1)
        val dLon = Math.toRadians(lon2 - lon1)

        val a = sin(dLat / 2) * sin(dLat / 2) +
                cos(Math.toRadians(lat1)) * cos(Math.toRadians(lat2)) *
                sin(dLon / 2) * sin(dLon / 2)

        val c = 2 * atan2(sqrt(a), sqrt(1 - a))

        return earthRadius * c
    }

    fun hasLocationPermission(context: Context): Boolean {
        return ActivityCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) == PackageManager.PERMISSION_GRANTED
    }

    fun isLocationEnabled(context: Context): Boolean {
        val locationManager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        return locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER) ||
                locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    // Convert location string to coordinates (for existing listings)
    fun getCoordinatesFromAddress(
        context: Context,
        address: String,
        onResult: (Double?, Double?) -> Unit
    ) {
        try {
            val geocoder = Geocoder(context, Locale.getDefault())
            val addresses = geocoder.getFromLocationName(address, 1)

            if (!addresses.isNullOrEmpty()) {
                val location = addresses[0]
                onResult(location.latitude, location.longitude)
            } else {
                // Default to Ho Chi Minh City coordinates
                onResult(10.8231, 106.6297)
            }
        } catch (e: Exception) {
            // Default to Ho Chi Minh City coordinates
            onResult(10.8231, 106.6297)
        }
    }
}