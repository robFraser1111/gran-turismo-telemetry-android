# SlickDash (Android)

Native Kotlin + Jetpack Compose companion for **Gran Turismo 7** live telemetry.

Not affiliated with Sony Interactive Entertainment or Polyphony Digital.

## Screenshots

| Simple | Driving | Pit wall |
| --- | --- | --- |
| ![Simple](docs/screenshots/simple.png) | ![Driving](docs/screenshots/driving.png) | ![Pit wall](docs/screenshots/pit-wall.png) |

Interim shots from the Windows companion (same modes). Replace with A17 captures after layout confirm.

## Run

Open in Android Studio, sync Gradle, run on a device on the same LAN as the PS5.

Find PS5 on launch. Simple is the default. Settings cog for manual IP.

Requires Android 8+ (API 26). LAN UDP to GT7 (heartbeat 33739, receive 33740).

## CI

GitHub Actions builds a debug APK and runs unit tests on every PR. No signing secrets or Sentry auth tokens are used.
