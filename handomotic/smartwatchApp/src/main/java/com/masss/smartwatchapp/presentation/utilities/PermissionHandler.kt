package com.masss.smartwatchapp.presentation.utilities

import android.app.Activity
import android.content.pm.PackageManager
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat


class PermissionHandler(private val activity: Activity) {

    private val PERMISSION_REQUEST_CODE = 1

    fun requestPermissionsAndCheck(permissions: Array<String>): Boolean {
        var allPermissionsGranted = true

        val permissionsToRequest = mutableListOf<String>()
        for (permission in permissions) {
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED)
                permissionsToRequest.add(permission)
        }

        if (permissionsToRequest.isNotEmpty()) {
            ActivityCompat.requestPermissions(activity, permissionsToRequest.toTypedArray(), PERMISSION_REQUEST_CODE)
            allPermissionsGranted = false
        }

        return allPermissionsGranted
    }

    fun arePermissionsGranted(permissions: Array<String>): Boolean {
        for (permission in permissions)
            if (ContextCompat.checkSelfPermission(activity, permission) != PackageManager.PERMISSION_GRANTED) {
                return false
        }
        return true
    }

    fun onRequestPermissionsResult(requestCode: Int, permissions: Array<out String>, grantResults: IntArray): Boolean {
        if (requestCode == PERMISSION_REQUEST_CODE) {
            for (result in grantResults) {
                if (result != PackageManager.PERMISSION_GRANTED)
                    return false
            }
            return true
        }

        return false
    }
}