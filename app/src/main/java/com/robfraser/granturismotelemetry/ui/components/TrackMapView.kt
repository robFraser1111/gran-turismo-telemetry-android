package com.robfraser.granturismotelemetry.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robfraser.granturismotelemetry.models.Formatters
import com.robfraser.granturismotelemetry.models.SectorCompare
import com.robfraser.granturismotelemetry.theme.GTColors
import kotlin.math.cos
import kotlin.math.sin

@Composable
fun TrackMapView(
    progress: Double,
    locked: Boolean = false,
    showSectors: Boolean = false,
    sectors: List<SectorCompare> = emptyList(),
    title: String = "TRACK MAP",
    footer: String? = null,
    modifier: Modifier = Modifier,
) {
    Box(modifier = modifier) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .clip(CardShape)
                .background(GTColors.card)
                .then(if (locked) Modifier.blur(2.dp) else Modifier)
                .padding(16.dp),
        ) {
            CapsLabel(title)
            Box(modifier = Modifier.weight(1f).padding(top = 8.dp)) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    val pts = samples(size.width, size.height, 200)
                    val stroke = if (showSectors) 6f else 3f
                    val loop = Path()
                    pts.forEachIndexed { i, p ->
                        if (i == 0) loop.moveTo(p.x, p.y) else loop.lineTo(p.x, p.y)
                    }
                    loop.close()
                    drawPath(
                        loop,
                        color = GTColors.track,
                        style = Stroke(width = stroke, join = StrokeJoin.Round),
                    )
                    if (!locked) {
                        val n = maxOf(2, (pts.size * progress.coerceIn(0.0, 1.0)).toInt())
                        val lit = Path()
                        pts.take(n).forEachIndexed { i, p ->
                            if (i == 0) lit.moveTo(p.x, p.y) else lit.lineTo(p.x, p.y)
                        }
                        drawPath(
                            lit,
                            color = GTColors.cyan,
                            style = Stroke(width = stroke, join = StrokeJoin.Round, cap = androidx.compose.ui.graphics.StrokeCap.Round),
                        )
                        val idx = (progress.coerceIn(0.0, 1.0) * (pts.size - 1)).toInt().coerceIn(0, pts.lastIndex)
                        val p = pts[idx]
                        val r = if (showSectors) 10f else 6f
                        drawCircle(GTColors.cyan, radius = r, center = p)
                    }
                }
                if (showSectors && sectors.isNotEmpty() && !locked) {
                    Column(
                        modifier = Modifier
                            .align(Alignment.CenterEnd)
                            .padding(end = 12.dp),
                    ) {
                        sectors.forEach { s ->
                            val c = GTColors.deltaColor(s.deltaSeconds).let {
                                if (it == GTColors.muted) GTColors.cyan else it
                            }
                            Text(
                                text = "S${s.number}  ${Formatters.sector(s.timeSeconds)}",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = c,
                            )
                        }
                    }
                }
            }
            if (footer != null) {
                Text(
                    text = footer,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Medium,
                    letterSpacing = 1.5.sp,
                    color = GTColors.muted,
                    modifier = Modifier.padding(top = 8.dp),
                )
            }
        }
        if (locked) {
            LockedOverlay("Live track map is Pro", modifier = Modifier.fillMaxSize())
        }
    }
}

private fun samples(width: Float, height: Float, count: Int): List<Offset> {
    return List(count) { i ->
        val t = i.toDouble() / count.toDouble() * 2 * Math.PI
        val x = 0.52 + 0.34 * cos(t) + 0.04 * cos(2 * t)
        val y = 0.52 + 0.30 * sin(t) - 0.06 * sin(2 * t)
        Offset((x * width).toFloat(), (y * height).toFloat())
    }
}
