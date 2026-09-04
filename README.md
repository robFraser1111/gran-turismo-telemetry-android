# SlickDash (Android)

Native Kotlin + Jetpack Compose companion for **Gran Turismo 7** live telemetry.

Not affiliated with Sony Interactive Entertainment or Polyphony Digital.

Open in Android Studio, sync Gradle, run on a device on the same LAN as the PS5.

Find PS5 on launch. Simple is the default. Settings cog for manual IP.

Requires Android 8+ (API 26). LAN UDP to GT7 (heartbeat 33739, receive 33740).

## Crash reporting (Sentry)

Errors go to the `gran-telemetry-android` project in org `robert-fraser`. The client DSN is baked in (public event-submit key only). Override with env `SENTRY_DSN` if needed. Do **not** commit Sentry org auth tokens, `sentry.properties` with tokens, or mapping-upload credentials.
