package com.robfraser.granturismotelemetry

import android.app.Application
import com.robfraser.granturismotelemetry.models.AppSettings
import com.robfraser.granturismotelemetry.telemetry.TelemetryService

class GranTurismoApplication : Application() {
    lateinit var settings: AppSettings
        private set
    lateinit var telemetry: TelemetryService
        private set

    override fun onCreate() {
        super.onCreate()
        settings = AppSettings(this)
        telemetry = TelemetryService()
        telemetry.start(settings)
    }
}
