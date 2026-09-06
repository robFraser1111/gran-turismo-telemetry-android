# Play Store release (Android)

Upload a signed **AAB** with Play App Signing. Keep the upload keystore offline — never commit `*.jks`, `*.keystore`, or `keystore.properties`.

## One-time: create an upload keystore

On your PC (Java 17 on `PATH` / `JAVA_HOME`):

```powershell
cd C:\dev\gran-turismo-telemetry-android
keytool -genkey -v -keystore upload-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload
```

Use a strong password. Store the `.jks` and passwords in a password manager — losing the upload key is painful even with Play App Signing.

Copy the example props file:

```powershell
copy keystore.properties.example keystore.properties
```

Edit `keystore.properties` so `storeFile`, passwords, and `keyAlias` match. `storeFile` is relative to the **repo root**.

## Build the Play bundle

```powershell
cd C:\dev\gran-turismo-telemetry-android
.\gradlew bundleRelease
```

Output: `app\build\outputs\bundle\release\app-release.aab`

Without `keystore.properties` (or the env vars below), `bundleRelease` still builds but the AAB is **unsigned** and Play will reject it.

### Optional env vars (CI later)

Same values as the properties file:

- `PLAY_STORE_FILE`
- `PLAY_STORE_PASSWORD`
- `PLAY_KEY_ALIAS`
- `PLAY_KEY_PASSWORD`

## Play Console

1. Create the app (`com.robfraser.slickdash`).
2. Enable **Play App Signing** (default for new apps).
3. Production / closed testing → create release → upload `app-release.aab`.
4. Complete Data safety, privacy policy URL, screenshots, and closed testing (12 testers / 14 days before production).

Privacy: https://robfraser1111.github.io/gran-turismo-telemetry-windows/privacy.html

Bump `versionCode` (and usually `versionName`) in `app/build.gradle.kts` for every new upload.
