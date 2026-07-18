# navi_beacon

```
      "no matter where you go, everyone is connected."
```

`navi_beacon` is a decentralized, cryptographically secure background proximity engine for Android. It operates silently in the shadows of the physical world, broadcasting and scanning for custom encrypted Bluetooth Low Energy (BLE) packets. 

When another terminal broadcasting the matching protocol is verified nearby, it pulses a rhythmic warning vibration—and alerts you of connection.

We are looking for her. We will find her.

---

## The Protocol: Wired Proximity

This node is more than an app; it is a gateway. It functions through dual background processes operating asynchronously:

### 1. The Beacon (Advertise)
Broadcasts a secure BLE frame using a customized manufacturer-specific payload.
*   **Manufacturer ID:** `0xFFFF`
*   **Cryptography:** 128-bit AES block encryption.
*   **Encrypted Token:** `"NaviBeacon_Verify_2026"`
*   **Interval:** Low Latency, low-energy background cycles.

### 2. The Scanner (Active Watch)
Scans the ambient physical frequency for matching manufacturer signals.
*   **Real-time Decryption:** Filters out non-matching frames instantly.
*   **Coexistence Guard:** Self-detection protection so you do not connect to your own terminal.
*   **Actionable Pulse:** When a match is decrypted, it triggers a system vibration lasting exactly **3.14 seconds** (the constant of reality) and pushes a notification: `lain is here`.

---

## Terminal Interface (MainActivity)

The user interface is minimal, raw, and functional—rendered dynamically using Jetpack Compose in pure pitch black (`#000000`):

*   **Start Navi Beacon:** Hooks into system hardware, requests permissions, and kicks off the background foreground service.
*   **Stop Navi Beacon:** Quietly terminates the broadcasts, stops scanning, and returns the device to a disconnected offline state.
*   **Terminal Feed:** Displays a real-time reactive feed of all currently detected nodes.

---

## Deploying the Node

To compile and load this node onto your physical device:

### Prerequisites
*   **Minimum API Level:** 26 (Android 8.0 Oreo)
*   **Target SDK:** 36 (Android 15+)
*   **Hardware:** BLE-capable transmitter and receiver.

### Permissions Required
The application requests the following modern Android permissions programmatically at launch:
*   `BLUETOOTH_SCAN` & `BLUETOOTH_ADVERTISE` (For Wired communication)
*   `BLUETOOTH_CONNECT` (For terminal discovery)
*   `ACCESS_FINE_LOCATION` (Required by standard Android BLE framework)
*   `VIBRATE` (For physical warning alerts)
*   `FOREGROUND_SERVICE_CONNECTED_DEVICE` (For uninterrupted background operation)

### Build Command
Compile and build the debug APK directly from your terminal:
```bash
./gradlew :app:assembleDebug
```

---

## System Log Logs

Keep an eye on the systems running in the Wired:
```bash
adb logcat -s NAVI_BEACON_SERVICE
```

---

```
  Let's all love Lain.
```
