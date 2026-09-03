package com.robfraser.granturismotelemetry.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.robfraser.granturismotelemetry.models.Formatters
import com.robfraser.granturismotelemetry.models.SectorCompare
import com.robfraser.granturismotelemetry.theme.GTColors
import com.robfraser.granturismotelemetry.theme.GTType

@Composable
fun TraceLine(
    values: List<Double>,
    color: Color,
    modifier: Modifier = Modifier,
    lineWidth: Float = 2f,
    range: ClosedFloatingPointRange<Double>? = null,
) {
    val pts = if (values.size > 1) values else listOf(0.0, 0.0)
    Canvas(modifier = modifier.fillMaxWidth()) {
        val lo = range?.start ?: 0.0
        val hi = range?.endInclusive ?: 1.0
        val span = maxOf(0.0001, hi - lo)
        val path = Path()
        pts.forEachIndexed { i, v ->
            val x = size.width * i / (pts.size - 1).coerceAtLeast(1)
            val n = ((v - lo) / span).coerceIn(0.0, 1.0)
            val y = size.height * (1f - n.toFloat())
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(
            path,
            color = color,
            style = Stroke(width = lineWidth, cap = StrokeCap.Round, join = StrokeJoin.Round),
        )
    }
}

@Composable
fun ZeroLine(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.fillMaxSize()) {
        drawLine(
            color = GTColors.track,
            start = Offset(0f, size.height / 2f),
            end = Offset(size.width, size.height / 2f),
            strokeWidth = 1f,
        )
    }
}

@Composable
fun ThrottleBrakeCard(
    throttle: List<Double>,
    brake: List<Double>,
    compact: Boolean = false,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .clip(CardShape)
            .background(GTColors.card)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(if (compact) 6.dp else 10.dp),
    ) {
        CapsLabel("THROTTLE", color = GTColors.cyan)
        TraceLine(throttle, GTColors.cyan, modifier = Modifier.height(if (compact) 36.dp else 48.dp))
        CapsLabel("BRAKE", color = GTColors.red)
        TraceLine(brake, GTColors.red, modifier = Modifier.height(if (compact) 36.dp else 48.dp))
    }
}

@Composable
fun DeltaSparklineCard(
    delta: Double,
    values: List<Double>,
    caption: String = "Rolling 5-lap trend vs best",
    locked: Boolean = false,
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
            CapsLabel("DELTA SPARKLINE")
            Text(
                text = Formatters.delta(delta, showPlus = false),
                style = GTType.numeric(40.sp),
                color = GTColors.deltaColor(delta),
            )
            Box(modifier = Modifier.weight(1f).fillMaxWidth().padding(top = 8.dp)) {
                ZeroLine()
                TraceLine(
                    values = normalized(values),
                    color = GTColors.deltaColor(delta),
                    lineWidth = 3f,
                    range = 0.0..1.0,
                    modifier = Modifier.fillMaxSize(),
                )
            }
            Text(
                text = caption,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = GTColors.muted,
            )
        }
        if (locked) {
            LockedOverlay("Lap delta is Pro", modifier = Modifier.fillMaxSize())
        }
    }
}

private fun normalized(values: List<Double>): List<Double> {
    if (values.isEmpty()) return listOf(0.5, 0.5)
    val maxAbs = maxOf(0.2, values.maxOf { kotlin.math.abs(it) })
    return values.map { 0.5 - (it / (maxAbs * 2)) }
}

@Composable
fun SectorBar(sector: SectorCompare, modifier: Modifier = Modifier) {
    val fill = GTColors.deltaColor(sector.deltaSeconds)
    val widthFraction = when {
        kotlin.math.abs(sector.deltaSeconds) < 0.0005 -> 0.48f
        sector.deltaSeconds < 0 -> 0.62f
        else -> {
            val extra = minOf(0.4, kotlin.math.abs(sector.deltaSeconds) * 0.8).toFloat()
            minOf(0.92f, 0.48f + extra + 0.3f)
        }
    }
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        CapsLabel("SECTOR ${sector.number}")
        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
            Text(
                text = Formatters.sector(sector.timeSeconds),
                style = GTType.numeric(20.sp),
                color = GTColors.text,
            )
            Spacer(Modifier.weight(1f))
            Text(
                text = Formatters.delta(sector.deltaSeconds),
                style = GTType.numeric(16.sp),
                color = fill,
            )
        }
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(12.dp)
                .clip(RoundedCornerShape(50))
                .background(GTColors.inset),
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(widthFraction)
                    .fillMaxHeight()
                    .clip(RoundedCornerShape(50))
                    .background(if (fill == GTColors.muted) GTColors.muted else fill),
            )
        }
    }
}
