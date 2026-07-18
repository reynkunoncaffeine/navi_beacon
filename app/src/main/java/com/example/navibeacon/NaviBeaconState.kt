package com.example.navibeacon

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

object NaviBeaconState {
    private val _detectedBeacons = MutableStateFlow<List<String>>(emptyList())
    val detectedBeacons = _detectedBeacons.asStateFlow()

    fun addBeaconDetection(beaconName: String) {
        val current = _detectedBeacons.value
        if (beaconName !in current) {
            _detectedBeacons.value = current + beaconName
        }
    }
}
