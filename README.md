# Dual Audio

Plays Android media **to USB and Bluetooth (A2DP) at the same time**, e.g. a USB headset and
Bluetooth earbuds. It applies to audio from any app (music, video, games).

No root required: it uses [Shizuku](https://shizuku.rikka.app/) (or adb) to run with the
permissions of the Android shell.

## How it works

Android 12 introduced
[combined audio device routing](https://source.android.com/docs/core/audio/combined-audio-routing):
`setPreferredDevicesForStrategy()` accepts **multiple** devices for one strategy. Dual Audio gives
the media strategy `[USB, BLUETOOTH_A2DP]`, and `AudioPolicyManager` switches to a duplicated
output (`DuplicatingThread`) that copies the mix to both devices.

The API is `@SystemApi` and requires `MODIFY_AUDIO_ROUTING`, which a regular app can't get. The
Android **shell user (uid 2000)** does have it, so the routing code runs as shell, in one of two
ways:

| Version | Runs as shell via | Needs a PC? |
|---|---|---|
| **App** (recommended) | [Shizuku](https://shizuku.rikka.app/) | No, only wireless debugging |
| **adb CLI** | `adb shell app_process` | Yes, every time you apply it |

## Requirements

- **App:** Android 14 or later. **adb CLI:** Android 12 or later.
- A USB audio device and a Bluetooth A2DP headset, connected at the same time.
- Developer options enabled (wireless debugging for the app, USB or wireless debugging for adb).

## Installing the app

1. Download `DualAudio-vX.Y.Z.apk` from the **[Releases](../../releases)** page and open it on the
   phone. Allow "install unknown apps" for your browser or file manager when Android asks.
   You can verify the download against the `SHA256SUMS.txt` in the same release.
2. Install **Shizuku** from the
   [Play Store](https://play.google.com/store/apps/details?id=moe.shizuku.privileged.api) or
   [GitHub](https://github.com/RikkaApps/Shizuku/releases).

### Starting Shizuku (after every reboot)

1. Connect the phone to Wi-Fi. Wireless debugging only works on a Wi-Fi network.
2. *Developer options → Wireless debugging*: turn it on.
3. Open Shizuku → **Start via Wireless debugging**. The first time, tap **Pairing** and enter the
   code from *Pair device with pairing code* into Shizuku's notification.

Shizuku keeps running until the next reboot. With a PC connected, you can start it instead with:

```bash
adb shell "$(adb shell pm path moe.shizuku.privileged.api | sed 's/package://;s#/base.apk##' | tr -d '\r')/lib/arm64/libshizuku.so"
```

### Usage

1. Open **Dual Audio** → **Grant Shizuku access** → *Allow all the time*.
2. With both devices connected, tap **Enable USB + Bluetooth**.
3. **Disable (normal routing)** turns it off.

While enabled, a small background service (started through Shizuku) checks every 3 seconds and
**reapplies the routing automatically** when both devices are connected but the routing was
reset, for example after the earbuds reconnect. It keeps working with the app closed, until you
tap Disable, Shizuku stops, or the phone reboots.

## adb CLI

For a quick test from a computer, without installing the app. It needs the Android SDK
(`platforms`, `build-tools`, `platform-tools`) and JDK 17+:

```bash
cd tools/audio-dup
./run.sh status   # connected outputs and current routing
./run.sh set      # route media to USB + Bluetooth
./run.sh clear    # back to normal routing
```

The script finds the SDK through `$ANDROID_HOME`, or in `~/Library/Android/sdk` on macOS.
Unlike the app, nothing reapplies the routing: after a device disconnects or the phone reboots,
run `set` again.

## Verifying it works

```bash
adb shell dumpsys media.audio_policy | grep -A1 -E 'Device role per product|STRATEGY_MEDIA'
```

With routing enabled, `STRATEGY_MEDIA` lists both `AUDIO_DEVICE_OUT_BLUETOOTH_A2DP` and
`AUDIO_DEVICE_OUT_USB_HEADSET` as the selected device, and *Outputs* contains a duplicated output
with both devices and `Global active count: 1` while something is playing.

## Known limitations

- **Out of sync:** Bluetooth plays about 150–300 ms behind USB. Android doesn't compensate for
  this on a duplicated output.
- **Resampling:** the mix runs in the Bluetooth output's format and is converted to the USB
  device's format. With high-resolution Bluetooth modes (e.g. 24-bit/96 kHz), this adds a
  resampling step; it's all uncompressed PCM. Setting
  *Developer options → Bluetooth audio sample rate* to 48 kHz avoids it. Which output is primary
  is decided by Android when Bluetooth connects; this API can't change it.
- **Volume:** each device has its own volume curve, so the volume keys may affect them
  differently.
- **Vendor audio effects** (e.g. Samsung Dolby Atmos, SoundAlive, 360 Audio) may behave
  differently on the duplicated output.
- **Unofficial:** this relies on hidden Android APIs and may break with Android or vendor updates.
  Tested on Samsung One UI with Android 16.

## Building from source

```bash
./gradlew :app:installDebug
```

- A debug build and a release APK are signed with different keys, so Android won't install one
  over the other. Uninstall first when switching, then grant Shizuku access again.
- **When changing the routing service, bump `versionCode`** in `app/build.gradle.kts`. The
  service is a Shizuku daemon, and Shizuku only replaces it when the version changes.
- GitHub Actions builds every push (`.github/workflows/build.yml`) and publishes signed APKs for
  `v*` tags (`.github/workflows/release.yml`).

### Project layout

```
app/src/main/
  aidl/com/byxnd/dualaudio/routing/IAudioRoutingService.aidl   app ↔ shell service contract
  java/com/byxnd/dualaudio/
    MainActivity.kt                         UI; connects to Shizuku and the routing service
    routing/AudioRouter.java                routing logic (shared with the CLI)
    routing/AudioRoutingUserService.java    Shizuku service (runs as shell, auto-reapply)
tools/audio-dup/
  src/Poc.java                              CLI: status | set | clear
  run.sh                                    builds with javac/d8 and runs via app_process
```

## License

Copyright (C) 2026 xTudoS

Licensed under the **GNU General Public License v3.0 or later** (`GPL-3.0-or-later`); see
[`LICENSE`](LICENSE). In short:

- You may use, study, fork, and modify the code.
- If you **distribute** it or a modified version (an APK, a fork on a store, etc.), you must do
  so under the same license and make the corresponding source code available.
- There is no warranty.

Third-party libraries bundled in the app (MIT and Apache 2.0, both GPL-3.0 compatible) are listed
in [`THIRD_PARTY_NOTICES.md`](THIRD_PARTY_NOTICES.md). The app shows both files under
**Open-source licenses**.
