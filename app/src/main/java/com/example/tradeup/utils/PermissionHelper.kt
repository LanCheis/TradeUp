package com.example.tradeup.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.material.dialog.MaterialAlertDialogBuilder

object PermissionHelper {

    const val LOCATION_PERMISSION_REQUEST_CODE = 1001
    const val CAMERA_PERMISSION_REQUEST_CODE = 1002
    const val STORAGE_PERMISSION_REQUEST_CODE = 1003

    /**
     * Check if all permissions are granted
     */
    fun hasPermissions(context: Context, permissions: Array<String>): Boolean {
        return permissions.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }

    /**
     * Request location permissions
     */
    fun requestLocationPermissions(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            LocationHelper.getLocationPermissions(),
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }

    /**
     * Show rationale dialog for location permissions
     */
    fun showLocationPermissionRationale(
        activity: Activity,
        onPositive: () -> Unit,
        onNegative: () -> Unit = {}
    ) {
        MaterialAlertDialogBuilder(activity)
            .setTitle("Location Permission Required")
            .setMessage("TradeUp needs location access to show you nearby listings and help buyers find your items.")
            .setPositiveButton("Grant Permission") { _, _ -> onPositive() }
            .setNegativeButton("Cancel") { _, _ -> onNegative() }
            .show()
    }

    /**
     * Show settings dialog when permission is permanently denied
     */
    fun showPermissionSettingsDialog(activity: Activity, permissionName: String) {
        MaterialAlertDialogBuilder(activity)
            .setTitle("Permission Required")
            .setMessage("$permissionName permission is required for this feature. Please enable it in app settings.")
            .setPositiveButton("Open Settings") { _, _ ->
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                    data = Uri.fromParts("package", activity.packageName, null)
                }
                activity.startActivity(intent)
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Handle permission request result
     */
    fun handleLocationPermissionResult(
        requestCode: Int,
        permissions: Array<String>,
        grantResults: IntArray,
        onGranted: () -> Unit,
        onDenied: () -> Unit,
        onPermanentlyDenied: () -> Unit
    ) {
        if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            when {
                grantResults.isNotEmpty() && grantResults.all { it == PackageManager.PERMISSION_GRANTED } -> {
                    onGranted()
                }
                permissions.any { perm ->
                    ActivityCompat.shouldShowRequestPermissionRationale(
                        perm as Activity, perm
                    )
                } -> {
                    onDenied()
                }
                else -> {
                    onPermanentlyDenied()
                }
            }
        }
    }
}