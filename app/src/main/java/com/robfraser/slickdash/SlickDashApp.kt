package com.robfraser.slickdash

import android.app.Application
import io.sentry.Sentry
import io.sentry.android.core.SentryAndroid

/**
 * Crash reporting only. The DSN is a public client key (event submit), same pattern as Windows.
 * Override with SENTRY_DSN at build/runtime if needed. Never put org auth tokens in the repo.
 */
class SlickDashApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SentryAndroid.init(this) { options ->
            options.dsn = System.getenv("SENTRY_DSN")?.takeIf { it.isNotBlank() } ?: DefaultDsn
            options.isSendDefaultPii = false
            options.tracesSampleRate = 0.2
            options.release = "slickdash-android@${BuildConfig.VERSION_NAME}"
            options.environment = if (BuildConfig.DEBUG) "development" else "production"
            options.isDebug = BuildConfig.DEBUG
            options.setBeforeSend { event, _ ->
                // Drop machine hostname; keep stack traces.
                event.serverName = null
                event
            }
        }
    }

    companion object {
        // Public client DSN for project gran-telemetry-android (org robert-fraser).
        const val DefaultDsn =
            "https://b6c4b4ee2d96f51514f83fbda82bd117@o4511995844231168.ingest.us.sentry.io/4511997557997569"

        /** Debug-only helper to confirm the crash path. */
        fun captureTestError() {
            Sentry.captureException(
                IllegalStateException("SlickDash Android Sentry test " + System.currentTimeMillis())
            )
        }
    }
}
