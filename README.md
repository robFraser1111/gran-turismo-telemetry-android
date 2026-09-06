# SlickDash (Android)

Native Kotlin + Jetpack Compose companion for **Gran Turismo 7** live telemetry.

Not affiliated with Sony Interactive Entertainment or Polyphony Digital.

## Screenshots

Samsung Galaxy A17 (SM-A175F). IDLE session — UI layout only.

### Portrait — Simple

<img src="docs/screenshots/simple.png" alt="Simple portrait" width="280" />

### Portrait — Driving

<img src="docs/screenshots/driving.png" alt="Driving portrait" width="280" />

### Portrait — Pit wall

<img src="docs/screenshots/pit-wall.png" alt="Pit wall portrait" width="280" />

### Landscape — Simple

<img src="docs/screenshots/simple-landscape.png" alt="Simple landscape" width="480" />

### Landscape — Driving

<img src="docs/screenshots/driving-landscape.png" alt="Driving landscape" width="480" />

### Landscape — Pit wall

<img src="docs/screenshots/pit-wall-landscape.png" alt="Pit wall landscape" width="480" />

## Run

Open in Android Studio, sync Gradle, run on a device on the same LAN as the PS5.

Find PS5 on launch. Simple is the default. Settings cog for manual IP.

Requires Android 8+ (API 26). LAN UDP to GT7 (heartbeat 33739, receive 33740).

## CI

GitHub Actions builds a debug APK and runs unit tests on every PR. No signing secrets or Sentry auth tokens are used.
