package com.masss.handomotic

import android.content.Context
import android.util.Log
import android.widget.Toast
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
import com.masss.handomotic.models.Beacon
import java.util.concurrent.TimeUnit

class BTBeaconManager(
    private val context: Context,
    private val knownBeacons : MutableMap<String, Beacon>
) {

    private val tag = "BTBeaconManager"
    private var proximityManager: ProximityManager? = null
    private val beaconsMap = mutableMapOf<String, Beacon>()

    constructor(
        context: Context,
        knownBeacons: List<Beacon>
    ) : this(context, knownBeacons.associateBy { it.address }.toMutableMap())

    init {
        KontaktSDK.initialize("123")
        setupProximityManager()
    }

    fun addKnownBeacon(beacon: Beacon) {
        knownBeacons[beacon.address] = beacon
    }

    /**
     * @return the list of beacons sorted by rssi.
     */
    fun getBeacons(testing: Boolean = false): MutableMap<String, Beacon> {
        //return beaconsList.sortedBy { it.rssi * -1 }
        return beaconsMap
    }

    fun getKnownBeacons(): MutableMap<String, Beacon> {
        return knownBeacons
    }

    fun getUnknownBeacons(): MutableMap<String, Beacon> {
        return beaconsMap.filter { !knownBeacons.containsKey(it.key) }.toMutableMap()
    }

    /**
     * Adds a [beacon] to the list.
     */
    private fun addBeacon(beacon: IBeaconDevice) {
        val newBeacon = Beacon(beacon.uniqueId, beacon.address, null, beacon.rssi.toDouble())
        beaconsMap[newBeacon.address] = newBeacon
    }

    private fun removeBeacon(beacon: IBeaconDevice) {
        beaconsMap[beacon.address]?.let {
            beaconsMap.remove(beacon.address)
        }
    }

    // TODO: può essere migliorato evitando di scansionare
    private fun setRssi(beacon: IBeaconDevice) {
        beaconsMap[beacon.address]?.rssi = beacon.rssi.toDouble()
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
                addBeacon(iBeacon)
            }

            override fun onIBeaconsUpdated(iBeacons: List<IBeaconDevice>, region: IBeaconRegion) {
                Log.i(tag, "onIBeaconsUpdated: " + iBeacons.size)
                iBeacons.forEach {
                    setRssi(it)
                }
                //Log.i(tag, "Nearest is: " + getBeacons().values.sortedBy { it.rssi }[0])
            }

            override fun onIBeaconLost(iBeacon: IBeaconDevice, region: IBeaconRegion) {
                Log.e(tag, "onIBeaconLost: $iBeacon")
                removeBeacon(iBeacon)
            }
        }
    }

    fun startScanning() {
        // Empty the list of beacons
        beaconsMap.clear()

        //Connect to scanning service and start scanning when ready
        proximityManager!!.connect(OnServiceReadyListener {
            //Check if proximity manager is already scanning
            if (proximityManager!!.isScanning) {
                Toast.makeText(
                    context,
                    "Already scanning",
                    Toast.LENGTH_SHORT
                ).show()
                return@OnServiceReadyListener
            }
            proximityManager!!.startScanning()
        })
    }

    fun isScanning(): Boolean {
        return proximityManager!!.isScanning
    }

    fun stopScanning() {
        if (proximityManager!!.isScanning) {
            proximityManager!!.stopScanning()
            Toast.makeText(context, "Scanning stopped", Toast.LENGTH_SHORT).show()
        }
    }

    fun destroy() {
        proximityManager!!.disconnect()
        proximityManager = null
    }

}