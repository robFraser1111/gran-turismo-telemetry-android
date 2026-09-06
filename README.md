# SlickDash (Android)

Native Kotlin + Jetpack Compose companion for **Gran Turismo 7** live telemetry.

Not affiliated with Sony Interactive Entertainment or Polyphony Digital.

## Screenshots

Samsung Galaxy A17 (SM-A175F). IDLE session — UI layout only.

Images are listed one-per-line (not in a table) so Android Studio’s README preview can load them.

### Portrait — Simple

![Simple portrait](docs/screenshots/simple.png)

### Portrait — Driving

![Driving portrait](docs/screenshots/driving.png)

### Portrait — Pit wall

![Pit wall portrait](docs/screenshots/pit-wall.png)

### Landscape — Simple

![Simple landscape](docs/screenshots/simple-landscape.png)

### Landscape — Driving

![Driving landscape](docs/screenshots/driving-landscape.png)

### Landscape — Pit wall

![Pit wall landscape](docs/screenshots/pit-wall-landscape.png)

## Run

Open in Android Studio, sync Gradle, run on a device on the same LAN as the PS5.

Find PS5 on launch. Simple is the default. Settings cog for manual IP.

Requires Android 8+ (API 26). LAN UDP to GT7 (heartbeat 33739, receive 33740).

## CI

GitHub Actions builds a debug APK and runs unit tests on every PR. No signing secrets or Sentry auth tokens are used.
