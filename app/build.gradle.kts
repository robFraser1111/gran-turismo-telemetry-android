import java.util.Properties

plugins {
    id("com.android.application")
    id("org.jetbrains.kotlin.android")
    id("org.jetbrains.kotlin.plugin.compose")
}

// Play upload signing — local keystore.properties (gitignored) or env vars.
// Never commit *.jks / *.keystore / passwords.
val keystorePropertiesFile = rootProject.file("keystore.properties")
val keystoreProperties = Properties()
if (keystorePropertiesFile.exists()) {
    keystorePropertiesFile.inputStream().use { keystoreProperties.load(it) }
}

fun propOrEnv(prop: String, env: String): String? =
    keystoreProperties.getProperty(prop)?.takeIf { it.isNotBlank() }
        ?: System.getenv(env)?.takeIf { it.isNotBlank() }

val releaseStoreFile = propOrEnv("storeFile", "PLAY_STORE_FILE")
val releaseStorePassword = propOrEnv("storePassword", "PLAY_STORE_PASSWORD")
val releaseKeyAlias = propOrEnv("keyAlias", "PLAY_KEY_ALIAS")
val releaseKeyPassword = propOrEnv("keyPassword", "PLAY_KEY_PASSWORD")
val hasReleaseSigning =
    !releaseStoreFile.isNullOrBlank() &&
        !releaseStorePassword.isNullOrBlank() &&
        !releaseKeyAlias.isNullOrBlank() &&
        !releaseKeyPassword.isNullOrBlank() &&
        rootProject.file(releaseStoreFile!!).isFile

android {
    namespace = "com.robfraser.slickdash"
    compileSdk = 36
    defaultConfig {
        applicationId = "com.robfraser.slickdash"
        minSdk = 26
        targetSdk = 36
        versionCode = 2
        versionName = "1.0.0"
    }
    signingConfigs {
        if (hasReleaseSigning) {
            create("release") {
                storeFile = rootProject.file(releaseStoreFile!!)
                storePassword = releaseStorePassword
                keyAlias = releaseKeyAlias
                keyPassword = releaseKeyPassword
            }
        }
    }
    buildTypes {
        getByName("release") {
            isMinifyEnabled = false
            if (hasReleaseSigning) {
                signingConfig = signingConfigs.getByName("release")
            }
        }
    }
    compileOptions {
        sourceCompatibility = JavaVersion.VERSION_17
        targetCompatibility = JavaVersion.VERSION_17
    }
    kotlinOptions { jvmTarget = "17" }
    buildFeatures {
        compose = true
        buildConfig = true
    }
}
dependencies {
    implementation(platform("androidx.compose:compose-bom:2024.10.01"))
    implementation("androidx.compose.ui:ui")
    implementation("androidx.compose.material3:material3")
    implementation("androidx.compose.ui:ui-tooling-preview")
    implementation("androidx.activity:activity-compose:1.9.3")
    implementation("androidx.lifecycle:lifecycle-runtime-ktx:2.8.6")
    implementation("androidx.lifecycle:lifecycle-viewmodel-compose:2.8.6")
    implementation("androidx.datastore:datastore-preferences:1.1.1")
    testImplementation("junit:junit:4.13.2")
    implementation("io.sentry:sentry-android:8.33.0")
}
