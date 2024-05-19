package com.masss.smartwatchapp.presentation

import android.os.Bundle
import android.view.View
import android.widget.LinearLayout
import android.widget.Switch
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.masss.smartwatchapp.R

class PermissionsActivity : AppCompatActivity() {

    private var LOG_TAG: String = "HanDomotic"
    private var deniedPermissions: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_permissions)

        deniedPermissions = intent.getStringExtra("denied_permissions")

        setupPermissionsPanel()
    }

    private fun setupPermissionsPanel() {
        val permissionPanel = findViewById<LinearLayout>(R.id.permissionPanel)
        val permissionList = findViewById<LinearLayout>(R.id.permissionList)

        var locationSwitchAlreadyDisplayed = false
        deniedPermissions?.split(",")?.forEach { permission ->
            if (permission == android.Manifest.permission.BODY_SENSORS) {
                addPermissionSwitch(R.string.accept_movement_data, permission, permissionList)
            }

            if ((permission == android.Manifest.permission.ACCESS_COARSE_LOCATION || permission == android.Manifest.permission.ACCESS_FINE_LOCATION)
                && !locationSwitchAlreadyDisplayed) {
                locationSwitchAlreadyDisplayed = true
                addPermissionSwitch(R.string.accept_location_data, permission, permissionList)
            }
        }

        permissionPanel.visibility = if (!deniedPermissions.isNullOrEmpty()) View.VISIBLE else View.GONE

        if (allDeniedPermissionsGranted()) {
            setResult(RESULT_OK)
            finish()
        }
    }

    private fun allDeniedPermissionsGranted(): Boolean {
        return deniedPermissions.isNullOrEmpty()
    }

    private fun requestPermission(permission: String) {
        requestPermissionsLauncher.launch(arrayOf(permission))
    }

    private val requestPermissionsLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val deniedPermissions = permissions.filter { !it.value }.keys
        if (deniedPermissions.isEmpty())

        else {
            setupPermissionsPanel()         // Update permission panel after requesting permissions
        }
    }

    private fun addPermissionSwitch(switchTextResourceId: Int, permission: String, permissionList: LinearLayout) {
        val permissionSwitch = Switch(this)
        permissionSwitch.text = getString(switchTextResourceId)
        permissionSwitch.isChecked = false
        permissionList.addView(permissionSwitch)

        permissionSwitch.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                requestPermission(permission)
            }
        }
    }
}