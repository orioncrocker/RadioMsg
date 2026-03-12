# RadioMSG

Android app for group situational awareness messaging over radio. Supports text, pictures, and GPS locations, with bi-directional bridging to SMS and email recipients. Includes secured IoT data reads and commands via Home Assistant. Compatible with the Java Pskmail server for PCs.

## Install

Download the APK from the Releases section and install via a file manager. You may need to enable "Install unknown apps" in device Security settings. See the Quick Start Guide in the Downloads section for details.

## Build

Requires JDK 21 and Android SDK with `ANDROID_HOME` set.

```bash
./gradlew assembleDebug
```

APK output: `RadioMSG/build/outputs/apk/debug/RadioMSG-debug.apk`

For more details, see [CLAUDE.md](CLAUDE.md).
