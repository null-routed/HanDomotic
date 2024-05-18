package com.masss.handomotic

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.kontakt.sdk.android.ble.configuration.ScanMode
import com.kontakt.sdk.android.ble.configuration.ScanPeriod
import com.kontakt.sdk.android.ble.manager.ProximityManager
import com.kontakt.sdk.android.ble.manager.ProximityManagerFactory
import com.kontakt.sdk.android.ble.manager.listeners.IBeaconListener
import com.kontakt.sdk.android.ble.rssi.RssiCalculators
import com.kontakt.sdk.android.common.KontaktSDK
import com.kontakt.sdk.android.common.profile.IBeaconDevice
import com.kontakt.sdk.android.common.profile.IBeaconRegion
import java.util.concurrent.TimeUnit

class BTBeaconManager (private val context: Context) {
    private val tag = "BTBeaconManager"
    private var proximityManager: ProximityManager? = null
    private val beaconsList = mutableListOf<Beacon>()

    init {
        KontaktSDK.initialize("123")
        setupProximityManager()
    }

    /**
     * @return the list of beacons sorted by rssi.
     */
    fun getBeacons(): List<Beacon> {
        return beaconsList.sortedBy { it.rssi }
    }

    /**
     * Adds a [beacon] to the list.
     */
    private fun addBeacon(beacon: IBeaconDevice) {
        val newBeacon = Beacon(beacon.uniqueId, beacon.uniqueId, beacon.rssi.toDouble())
        beaconsList.add(newBeacon)
    }

    private fun setupProximityManager() {
        proximityManager = ProximityManagerFactory.create(context)

        //Configure proximity manager basic options
        proximityManager?.configuration()
            ?.scanMode(ScanMode.BALANCED)
            ?.scanPeriod(ScanPeriod.RANGING)
            //?.activityCheckConfiguration(ActivityCheckConfiguration.DISABLED)
            //?.forceScanConfiguration(ForceScanConfiguration.DISABLED)
            ?.deviceUpdateCallbackInterval(TimeUnit.SECONDS.toMillis(5))
            ?.rssiCalculator(RssiCalculators.DEFAULT)
            //?.cacheFileName("Example")
            ?.resolveShuffledInterval(3)
            ?.monitoringEnabled(true)
            ?.monitoringSyncInterval(10)
        //?.secureProfilePayloadResolvers(Collections.emptyList())
        //?.kontaktScanFilters(KontaktScanFilter.DEFAULT_FILTERS_LIST)

        //Setting up iBeacon and Eddystone listeners
        proximityManager?.setIBeaconListener(createIBeaconListener())
    }

    private fun createIBeaconListener(): IBeaconListener {
        return object : IBeaconListener {
            override fun onIBeaconDiscovered(iBeacon: IBeaconDevice, region: IBeaconRegion) {
                Log.i(tag, "onIBeaconDiscovered: $iBeacon")
            }

            override fun onIBeaconsUpdated(iBeacons: List<IBeaconDevice>, region: IBeaconRegion) {
                Log.i(tag, "onIBeaconsUpdated: " + iBeacons.size)
            }

            override fun onIBeaconLost(iBeacon: IBeaconDevice, region: IBeaconRegion) {
                Log.e(tag, "onIBeaconLost: $iBeacon")
            }
        }
    }
}