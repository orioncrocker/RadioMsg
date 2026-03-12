# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

RadioMSG is an Android app for situational awareness messaging over radio. It supports text/picture/GPS message exchange, bi-directional SMS/email bridging via radio, and IoT integration with Home Assistant. It is compatible with the Java Pskmail server for PCs.

## Build Commands

Requires JDK 21 and `ANDROID_HOME` pointing to an Android SDK with platform-34, build-tools-34, and NDK 28.2 installed.

All commands use the Gradle wrapper from the project root:

```bash
./gradlew assembleDebug      # Build debug APK
./gradlew assembleRelease    # Build release APK
./gradlew build              # Full build (both variants)
./gradlew installDebug       # Build and install debug APK on connected device
./gradlew lint               # Run lint checks
```

**Build system:** Gradle 8.4, AGP 8.1.0

**Build variants:**
- Debug: `jniDebuggable=true`, NDK debug symbols enabled
- Release: ProGuard configured but minification disabled

## Architecture

The app has three layers:

### 1. Java Application Layer (`RadioMSG/src/main/java/com/RadioMSG/`)
- **RadioMSG.java** — Main Activity; handles UI, GPS, Bluetooth, USB device management, gesture navigation, SMS/email UI
- **RMsgProcessor.java** — Background `Service`; handles email (IMAP/SMTP), SMS, server control, and Home Assistant IoT integration
- **Modem.java** — Manages audio I/O (8000 Hz PCM float via `AudioRecord`), USB serial port, and dispatches to the native modem engine
- **myPreferences.java** — Settings Activity
- `CCIR476.java`, `TxCCIR476.java`, `RxCCIR476.java`, `TxCCIR493.java`, `RxCCIR493.java` — CCIR 476/493 radio teletype protocol implementations

### 2. JNI Bridge (`RadioMSG/src/main/jni/`)
- **AndFlmsg_Fldigi_Interface.cpp** — JNI bridge connecting Java `Modem.java` to the Fldigi modem engine; handles encoding/decoding dispatch across modem types
- **Android.mk** / **Application.mk** — NDK build; produces `RadioMSG_Modem_Interface`; targets armeabi-v7a, arm64-v8a, x86, x86_64 with C++17

### 3. Native Modem Engine (`RadioMSG/src/main/jni/fldigi/`)
Derived from the Fldigi project. Implements multiple digital radio modem protocols:
- PSK, MFSK, MT63, THOR, Olivia, DominoEX, RSID
- DSP filters, FFT (fft/ and kissfft/), Viterbi decoding
- `libsamplerate/` — sample rate conversion

**Audio backend:** Google Oboe (`jni/oboe/`), using AAudio on Android 8.0+ and falling back to OpenSL ES on older devices.

### Data Flow
```
Radio/USB → Modem.java → JNI bridge → Fldigi engine → decoded message
                                      ↓
                              RMsgProcessor.java → email/SMS/HA relay
```

### Message Model
- `RMsgObject.java` — message data model
- `RMsgDisplayList.java` / `RMsgTxList.java` — receive and transmit queues
- `RMsgCheckSum.java` — message integrity

## Key Configuration

- **App version:** 2.1.0.18A (`AndroidManifest.xml`)
- **SDK:** minSdk 21, targetSdk 28, compileSdk 34
- **NDK:** 28.2.13676358 — required for 16KB page alignment (Android 15+)
- **NDK C++ standard:** C++17, using `c++_shared` STL
- **Permissions:** Location, Audio, Bluetooth, SMS, Camera, USB, Vibration
- USB device filter: `res/xml/device_filter.xml`
- Lint suppressions: `RadioMSG/lint.xml`
- `android.nonFinalResIds=false` in `gradle.properties` — keeps R fields as constants so existing `switch` statements on resource IDs compile under AGP 8
- `android.suppressUnsupportedCompileSdk=34` — suppresses AGP 8.1 warning about compileSdk 34 (cosmetic only)

## Notable Dependencies

- `com.github.mik3y:usb-serial-for-android` — USB serial (for radio TNCs)
- `com.sun.mail:android-mail` — JavaMail for IMAP/SMTP
- `org.jsoup:jsoup` — HTML parsing
- `io.michaelrocks:libphonenumber-android:9.0.24` — Phone number parsing
- `acra-4.5.0.jar` (local, `libs/`) — Crash reporting via `RadioMSGDebug.java`
