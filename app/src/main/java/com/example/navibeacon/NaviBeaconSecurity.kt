package com.example.navibeacon

import java.nio.charset.StandardCharsets
import javax.crypto.Cipher
import javax.crypto.spec.SecretKeySpec

object NaviBeaconSecurity {
    private const val AES_KEY = "NaviBeaconSecKey" // 16 chars
    private const val BEACON_TOKEN = "NaviBeacon_Verify_2026"

    const val MANUFACTURER_ID = 0xFFFF

    fun getBeaconPayload(): ByteArray {
        val secretKey = SecretKeySpec(AES_KEY.toByteArray(StandardCharsets.UTF_8), "AES")
        val cipher = Cipher.getInstance("AES/ECB/PKCS5Padding")
        cipher.init(Cipher.ENCRYPT_MODE, secretKey)
        return cipher.doFinal(BEACON_TOKEN.toByteArray(StandardCharsets.UTF_8)).take(8).toByteArray()
    }

    fun verifyBeacon(receivedData: ByteArray?): Boolean {
        if (receivedData == null) return false
        val expected = getBeaconPayload()
        return receivedData.contentEquals(expected)
    }
}
