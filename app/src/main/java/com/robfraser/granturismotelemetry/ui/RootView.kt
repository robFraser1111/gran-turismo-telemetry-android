package com.robfraser.granturismotelemetry.ui

import android.app.Activity
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.windowsizeclass.WindowHeightSizeClass
import androidx.compose.material3.windowsizeclass.WindowSizeClass
import androidx.compose.material3.windowsizeclass.WindowWidthSizeClass
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import androidx.core.view.WindowInsetsCompat
import androidx.core.view.WindowInsetsControllerCompat
import com.robfraser.granturismotelemetry.theme.GTColors

/**
 * Picks one of the four approved layouts from window size class + orientation.
 * Desktop 1440 pit wall is web-only and is never shown here.
 */
@Composable
fun RootView(windowSizeClass: WindowSizeClass) {
    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(GTColors.page),
    ) {
        val landscape = maxWidth > maxHeight
        val sizeClassTablet =
            windowSizeClass.widthSizeClass != WindowWidthSizeClass.Compact &&
                windowSizeClass.heightSizeClass != WindowHeightSizeClass.Compact
        val shortest = minOf(maxWidth, maxHeight)
        val isTablet = sizeClassTablet || shortest >= 600.dp

        val immersive = landscape
        HideSystemBars(hide = immersive)

        when {
            isTablet && landscape -> TabletLandscapeView()
            isTablet && !landscape -> TabletPortraitView()
            landscape -> PhoneLandscapeView()
            else -> PhonePortraitView()
        }
    }
}

@Composable
private fun HideSystemBars(hide: Boolean) {
    val view = LocalView.current
    DisposableEffect(hide) {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            val controller = WindowCompat.getInsetsController(window, view)
            if (hide) {
                controller.hide(WindowInsetsCompat.Type.systemBars())
                controller.systemBarsBehavior =
                    WindowInsetsControllerCompat.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE
            } else {
                controller.show(WindowInsetsCompat.Type.systemBars())
            }
        }
        onDispose {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view)
                    .show(WindowInsetsCompat.Type.systemBars())
            }
        }
    }
}
