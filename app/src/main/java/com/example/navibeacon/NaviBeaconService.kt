package com.example.navibeacon

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.bluetooth.le.*
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.util.Log
import androidx.core.app.NotificationCompat

class NaviBeaconService : Service() {

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var bleAdvertiser: BluetoothLeAdvertiser? = null
    private var bleScanner: BluetoothLeScanner? = null

    private lateinit var beaconVibrator: NaviBeaconVibrator

    private val notifiedBeacons = mutableSetOf<String>()

    companion object {
        private const val CHANNEL_ID = "navi_beacon_channel"
        private const val TAG = "NAVI_BEACON_SERVICE"
        private const val LOST_TIMEOUT_MS = 3000L
    }

    private var isVibrating = false
    private val handler = Handler(Looper.getMainLooper())

    private val lostRunnable = Runnable {
        Log.d(TAG, "Beacon signal lost, stopping vibration.")
        beaconVibrator.stopVibration()
        isVibrating = false
    }

    private val advertiseCallback = object : AdvertiseCallback() {
        override fun onStartSuccess(settingsInEffect: AdvertiseSettings) {
            super.onStartSuccess(settingsInEffect)
            Log.d(TAG, "Navi Beacon broadcast started successfully.")
        }

        override fun onStartFailure(errorCode: Int) {
            super.onStartFailure(errorCode)
            Log.e(TAG, "Navi Beacon broadcast failed: $errorCode")
        }
    }

    private val scanCallback = object : ScanCallback() {
        override fun onScanResult(callbackType: Int, result: ScanResult) {
            super.onScanResult(callbackType, result)

            val scanRecord = result.scanRecord
            val manufacturerData = scanRecord?.getManufacturerSpecificData(NaviBeaconSecurity.MANUFACTURER_ID)

            if (NaviBeaconSecurity.verifyBeacon(manufacturerData)) {
                val beaconName = try {
                    result.device.name ?: result.device.address
                } catch (e: SecurityException) {
                    result.device.address
                }

                if (!isVibrating) {
                    beaconVibrator.triggerVibration()
                    isVibrating = true
                }

                handler.removeCallbacks(lostRunnable)
                handler.postDelayed(lostRunnable, LOST_TIMEOUT_MS)

                if (!notifiedBeacons.contains(beaconName)) {
                    showBeaconNotification(beaconName)
                    notifiedBeacons.add(beaconName)
                }

                NaviBeaconState.addBeaconDetection(beaconName)
            }
        }

        override fun onBatchScanResults(results: MutableList<ScanResult>?) {
            super.onBatchScanResults(results)
            results?.forEach { onScanResult(ScanSettings.CALLBACK_TYPE_ALL_MATCHES, it) }
        }

        override fun onScanFailed(errorCode: Int) {
            super.onScanFailed(errorCode)
            Log.e(TAG, "Beacon scan failed: $errorCode")
        }
    }

    private fun showBeaconNotification(beaconName: String) {
        val notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("lain is here")
            .setContentText("$beaconName detected")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        val manager = getSystemService(NotificationManager::class.java)
        manager?.notify(2, notification)
    }

    override fun onCreate() {
        super.onCreate()
        beaconVibrator = NaviBeaconVibrator(this)

        val bluetoothManager = getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        bleAdvertiser = bluetoothAdapter?.bluetoothLeAdvertiser
        bleScanner = bluetoothAdapter?.bluetoothLeScanner
    }

    @SuppressLint("MissingPermission")
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        createNotificationChannel()

        val notification: Notification = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("looking for lain")
            .setContentText("we will find lain, someday")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setOngoing(true)
            .build()

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                startForeground(1, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
            } else {
                startForeground(1, notification)
            }
        } catch (e: Exception) {
            Log.e(TAG, "startForeground failed", e)
            stopSelf()
            return START_NOT_STICKY
        }

        startNaviBeaconSystem()
        return START_STICKY
    }

    @androidx.annotation.RequiresPermission(allOf = [
        android.Manifest.permission.BLUETOOTH_SCAN,
        android.Manifest.permission.BLUETOOTH_ADVERTISE
    ])
    private fun startNaviBeaconSystem() {
        try {
            val advertiseSettings = AdvertiseSettings.Builder()
                .setAdvertiseMode(AdvertiseSettings.ADVERTISE_MODE_LOW_LATENCY)
                .setTxPowerLevel(AdvertiseSettings.ADVERTISE_TX_POWER_MEDIUM)
                .setConnectable(false)
                .build()

            val advertiseData = AdvertiseData.Builder()
                .addManufacturerData(NaviBeaconSecurity.MANUFACTURER_ID, NaviBeaconSecurity.getBeaconPayload())
                .setIncludeDeviceName(true)
                .build()

            try {
                bleAdvertiser?.startAdvertising(advertiseSettings, advertiseData, advertiseCallback)
            } catch (e: Exception) {
                Log.e(TAG, "Advertising failed: ${e.message}", e)
            }

            val scanFilter = ScanFilter.Builder()
                .setManufacturerData(NaviBeaconSecurity.MANUFACTURER_ID, NaviBeaconSecurity.getBeaconPayload())
                .build()

            val scanSettings = ScanSettings.Builder()
                .setScanMode(ScanSettings.SCAN_MODE_LOW_LATENCY)
                .build()

            try {
                bleScanner?.startScan(listOf(scanFilter), scanSettings, scanCallback)
            } catch (e: Exception) {
                Log.e(TAG, "Scanning failed: ${e.message}", e)
            }
        } catch (e: Exception) {
            Log.e(TAG, "startNaviBeaconSystem failed: ${e.message}", e)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        handler.removeCallbacks(lostRunnable)
        beaconVibrator.stopVibration()
        try {
            bleAdvertiser?.stopAdvertising(advertiseCallback)
            bleScanner?.stopScan(scanCallback)
        } catch (e: SecurityException) {
            Log.e(TAG, "Bluetooth permission missing during stop", e)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Navi Beacon Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
        }
    }
}
