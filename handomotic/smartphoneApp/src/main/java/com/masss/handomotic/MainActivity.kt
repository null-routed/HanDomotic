package com.masss.handomotic

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.databinding.DataBindingUtil
import com.masss.handomotic.ui.theme.HanDomoticTheme
import com.kontakt.sdk.android.ble.configuration.ScanMode
import com.kontakt.sdk.android.ble.configuration.ScanPeriod
import com.kontakt.sdk.android.ble.connection.OnServiceReadyListener
import com.kontakt.sdk.android.ble.manager.ProximityManager
import com.kontakt.sdk.android.ble.manager.ProximityManagerFactory
import com.kontakt.sdk.android.ble.manager.listeners.IBeaconListener
import com.kontakt.sdk.android.ble.rssi.RssiCalculators
import com.kontakt.sdk.android.common.KontaktSDK
import com.kontakt.sdk.android.common.profile.IBeaconDevice
import com.kontakt.sdk.android.common.profile.IBeaconRegion
import com.masss.handomotic.databinding.HomeBinding

class MainActivity : ComponentActivity() {
    private lateinit var binding: HomeBinding

    companion object {
        const val REQUEST_CODE_PERMISSIONS: Int = 100
        const val TAG = "MainActivity"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        binding = DataBindingUtil.setContentView(this, R.layout.home)
        binding.greeting.text = "Wake up, samurai!"
        checkPermissions()
    }

    private fun checkPermissions() {
        val requiredPermissions = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S
        ) arrayOf(Manifest.permission.ACCESS_FINE_LOCATION)
        else arrayOf(
            Manifest.permission.BLUETOOTH_SCAN,
            Manifest.permission.BLUETOOTH_CONNECT,
            Manifest.permission.ACCESS_FINE_LOCATION
        )
        if (isAnyOfPermissionsNotGranted(requiredPermissions)) {
            ActivityCompat.requestPermissions(
                this,
                requiredPermissions,
                MainActivity.REQUEST_CODE_PERMISSIONS
            )
        }
    }

    private fun isAnyOfPermissionsNotGranted(requiredPermissions: Array<String>): Boolean {
        for (permission in requiredPermissions) {
            val checkSelfPermissionResult = ContextCompat.checkSelfPermission(this, permission)
            if (PackageManager.PERMISSION_GRANTED != checkSelfPermissionResult) {
                return true
            }
        }
        return false
    }
}