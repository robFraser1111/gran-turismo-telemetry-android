package com.robfraser.granturismotelemetry

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.windowsizeclass.ExperimentalMaterial3WindowSizeClassApi
import androidx.compose.material3.windowsizeclass.calculateWindowSizeClass
import androidx.compose.ui.graphics.toArgb
import com.robfraser.granturismotelemetry.theme.GTColors
import com.robfraser.granturismotelemetry.theme.GTTheme
import com.robfraser.granturismotelemetry.ui.RootView

class MainActivity : ComponentActivity() {
    @OptIn(ExperimentalMaterial3WindowSizeClassApi::class)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val page = GTColors.page.toArgb()
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(page),
            navigationBarStyle = SystemBarStyle.dark(page),
        )
        val app = application as GranTurismoApplication
        setContent {
            val windowSizeClass = calculateWindowSizeClass(this)
            GTTheme(settings = app.settings, telemetry = app.telemetry) {
                RootView(windowSizeClass = windowSizeClass)
            }
        }
    }
}
