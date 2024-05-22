package com.masss.handomotic.utils

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.masss.handomotic.ScanActivity

class AuthPermission {
    companion object{
        fun checkPermissions(activity: Activity, context: Context) {
            val requiredPermissions = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S
            ) arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
            else arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            if (isAnyOfPermissionsNotGranted(context, requiredPermissions)) {
                ActivityCompat.requestPermissions(
                    activity,
                    requiredPermissions,
                    ScanActivity.REQUEST_CODE_PERMISSIONS
                )
            }
        }
        private fun isAnyOfPermissionsNotGranted(context: Context, requiredPermissions: Array<String>): Boolean {
            for (permission in requiredPermissions) {
                val checkSelfPermissionResult = ContextCompat.checkSelfPermission(context, permission)
                if (PackageManager.PERMISSION_GRANTED != checkSelfPermissionResult) {
                    return true
                }
            }
            return false
        }

        private fun socketBTPermissions() : Array<String>{
            val requiredPermissions = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S
            ) arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)

            else arrayOf(
                Manifest.permission.BLUETOOTH_SCAN,
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.ACCESS_FINE_LOCATION
            )
            return requiredPermissions
        }
    }
}