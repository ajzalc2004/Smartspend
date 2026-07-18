package com.smartspends.app.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import java.util.Locale

@Composable
fun DonutChart(
    data: Map<String, Double>,
    colors: List<Color>,
    modifier: Modifier = Modifier
) {
    val total = data.values.sum()
    if (total == 0.0) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("No data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(data) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val textMeasurer = rememberTextMeasurer()
    val sortedData = data.toList().sortedByDescending { it.second }

    val centerLabel = if (selectedIndex != null) {
        sortedData[selectedIndex!!].first
    } else {
        "Total Expense"
    }

    val centerValue = if (selectedIndex != null) {
        String.format(Locale.getDefault(), "₹%,.2f", sortedData[selectedIndex!!].second)
    } else {
        String.format(Locale.getDefault(), "₹%,.2f", total)
    }

    val textStyleLabel = TextStyle(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 11.sp,
        fontWeight = FontWeight.Normal
    )
    val textStyleVal = TextStyle(
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 16.sp,
        fontWeight = FontWeight.Bold
    )

    Canvas(
        modifier = modifier.pointerInput(data) {
            detectTapGestures { offset ->
                val center = Offset(size.width / 2f, size.height / 2f)
                val dx = offset.x - center.x
                val dy = offset.y - center.y
                val distance = Math.sqrt((dx * dx + dy * dy).toDouble()).toFloat()
                val radius = java.lang.Math.min(size.width, size.height).toFloat() / 2.5f
                val strokeWidth = 32.dp.toPx()

                if (distance >= (radius - strokeWidth / 2) && distance <= (radius + strokeWidth / 2)) {
                    var angle = Math.toDegrees(Math.atan2(dy.toDouble(), dx.toDouble())).toFloat()
                    angle = (angle + 90f + 360f) % 360f

                    var currentAngle = 0f
                    var foundIndex = -1
                    for (idx in sortedData.indices) {
                        val sweepAngle = ((sortedData[idx].second / total) * 360.0).toFloat()
                        if (angle >= currentAngle && angle <= currentAngle + sweepAngle) {
                            foundIndex = idx
                            break
                        }
                        currentAngle += sweepAngle
                    }
                    selectedIndex = if (foundIndex == selectedIndex) null else foundIndex
                } else {
                    selectedIndex = null
                }
            }
        }
    ) {
        val width = size.width
        val height = size.height
        val radius = size.minDimension / 2.5f
        val center = Offset(width / 2, height / 2)
        val strokeWidth = 32.dp.toPx()

        var startAngle = -90f

        sortedData.forEachIndexed { index, pair ->
            val slicePercentage = (pair.second / total).toFloat()
            val sweepAngle = slicePercentage * 360f * animationProgress.value
            val color = colors.getOrElse(index) { Color.Gray }
            val isSelected = selectedIndex == index

            drawArc(
                color = color,
                startAngle = startAngle,
                sweepAngle = sweepAngle,
                useCenter = false,
                topLeft = Offset(center.x - radius, center.y - radius),
                size = Size(radius * 2, radius * 2),
                style = Stroke(
                    width = if (isSelected) strokeWidth + 6.dp.toPx() else strokeWidth,
                    cap = StrokeCap.Round
                )
            )
            startAngle += sweepAngle
        }

        // Draw center labels
        val textLayoutResultLabel = textMeasurer.measure(centerLabel, style = textStyleLabel)
        val textLayoutResultVal = textMeasurer.measure(centerValue, style = textStyleVal)

        drawText(
            textLayoutResult = textLayoutResultLabel,
            topLeft = Offset(
                center.x - textLayoutResultLabel.size.width / 2,
                center.y - textLayoutResultLabel.size.height - 2
            )
        )
        drawText(
            textLayoutResult = textLayoutResultVal,
            topLeft = Offset(
                center.x - textLayoutResultVal.size.width / 2,
                center.y + 2
            )
        )
    }
}

@Composable
fun DoubleBarChart(
    labels: List<String>,
    valuesIncome: List<Double>,
    valuesExpense: List<Double>,
    incomeColor: Color,
    expenseColor: Color,
    modifier: Modifier = Modifier
) {
    if (labels.isEmpty()) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("No data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val maxVal = maxOf(
        (valuesIncome.maxOrNull() ?: 0.0),
        (valuesExpense.maxOrNull() ?: 0.0),
        1000.0 // Default min roof to avoid division by zero
    )

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(labels) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 10.sp,
        fontWeight = FontWeight.Medium
    )

    Canvas(
        modifier = modifier.pointerInput(labels) {
            detectTapGestures { offset ->
                val numBars = labels.size
                val barSpacing = size.width / (numBars + 0.5f)
                val graphHeight = size.height - 40.dp.toPx()

                if (offset.y <= graphHeight) {
                    var foundIdx = -1
                    for (i in 0 until numBars) {
                        val centerIndexX = barSpacing * (i + 0.75f)
                        val leftX = centerIndexX - 16.dp.toPx()
                        val rightX = centerIndexX + 16.dp.toPx()
                        if (offset.x >= leftX && offset.x <= rightX) {
                            foundIdx = i
                            break
                        }
                    }
                    selectedIndex = if (foundIdx == selectedIndex) null else foundIdx
                } else {
                    selectedIndex = null
                }
            }
        }
    ) {
        val width = size.width
        val height = size.height
        val graphHeight = height - 40.dp.toPx()
        val numBars = labels.size
        val barSpacing = width / (numBars + 0.5f)
        val barWidth = 12.dp.toPx()

        // Background lines
        val numGridLines = 4
        for (i in 0..numGridLines) {
            val y = (graphHeight / numGridLines) * i
            drawLine(
                color = Color.Gray.copy(alpha = 0.15f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
            )
        }

        // Draw Bars
        for (i in 0 until numBars) {
            val centerIndexX = barSpacing * (i + 0.75f)
            val isSelected = selectedIndex == i

            // Highlight selected column background
            if (isSelected) {
                drawRoundRect(
                    color = Color.Gray.copy(alpha = 0.08f),
                    topLeft = Offset(centerIndexX - 20.dp.toPx(), 0f),
                    size = Size(40.dp.toPx(), graphHeight),
                    cornerRadius = CornerRadius(8.dp.toPx(), 8.dp.toPx())
                )
            }

            // Income Bar (Left of Index center)
            val incVal = valuesIncome.getOrElse(i) { 0.0 }
            val incBarHeight = (incVal / maxVal).toFloat() * graphHeight * animationProgress.value
            val incTopLeftX = centerIndexX - barWidth - 2.dp.toPx()
            val incTopLeftY = graphHeight - incBarHeight

            drawRoundRect(
                color = if (isSelected) incomeColor else incomeColor.copy(alpha = 0.8f),
                topLeft = Offset(incTopLeftX, incTopLeftY),
                size = Size(barWidth, incBarHeight),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            // Expense Bar (Right of Index center)
            val expVal = valuesExpense.getOrElse(i) { 0.0 }
            val expBarHeight = (expVal / maxVal).toFloat() * graphHeight * animationProgress.value
            val expTopLeftX = centerIndexX + 2.dp.toPx()
            val expTopLeftY = graphHeight - expBarHeight

            drawRoundRect(
                color = if (isSelected) expenseColor else expenseColor.copy(alpha = 0.8f),
                topLeft = Offset(expTopLeftX, expTopLeftY),
                size = Size(barWidth, expBarHeight),
                cornerRadius = CornerRadius(4.dp.toPx(), 4.dp.toPx())
            )

            // Draw X Axis label
            val textLayoutResult = textMeasurer.measure(labels[i], style = labelStyle)
            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(
                    x = centerIndexX - textLayoutResult.size.width / 2,
                    y = graphHeight + 10.dp.toPx()
                )
            )
        }

        // Render Tooltip at the top center
        selectedIndex?.let { index ->
            val inc = valuesIncome.getOrNull(index) ?: 0.0
            val exp = valuesExpense.getOrNull(index) ?: 0.0
            val label = labels.getOrNull(index) ?: ""
            val tooltipText = "$label - Inc: ₹${String.format(Locale.getDefault(), "%,.0f", inc)} | Exp: ₹${String.format(Locale.getDefault(), "%,.0f", exp)}"
            val tooltipResult = textMeasurer.measure(
                tooltipText,
                style = TextStyle(
                    color = Color.White,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            val tooltipWidth = tooltipResult.size.width + 16.dp.toPx()
            val tooltipHeight = tooltipResult.size.height + 8.dp.toPx()

            drawRoundRect(
                color = Color.Black.copy(alpha = 0.85f),
                topLeft = Offset((width - tooltipWidth) / 2f, 4.dp.toPx()),
                size = Size(tooltipWidth, tooltipHeight),
                cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
            )
            drawText(
                textLayoutResult = tooltipResult,
                topLeft = Offset((width - tooltipResult.size.width) / 2f, 8.dp.toPx())
            )
        }
    }
}

@Composable
fun BezierLineChart(
    labels: List<String>,
    values: List<Double>,
    lineColor: Color,
    gradientColor: Color,
    modifier: Modifier = Modifier
) {
    if (labels.isEmpty() || values.isEmpty()) {
        Box(
            modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f), RoundedCornerShape(16.dp)),
            contentAlignment = Alignment.Center
        ) {
            Text("No data available", color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        return
    }

    val maxVal = values.maxOrNull() ?: 1.0
    val minVal = values.minOrNull() ?: 0.0
    val range = if (maxVal - minVal == 0.0) 1.0 else maxVal - minVal

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(labels) {
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 1000)
        )
    }

    var selectedIndex by remember { mutableStateOf<Int?>(null) }
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = TextStyle(
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        fontSize = 9.sp,
        fontWeight = FontWeight.Medium
    )

    Canvas(
        modifier = modifier.pointerInput(labels) {
            detectTapGestures { offset ->
                val pointsCount = values.size
                val xSpacing = size.width / (pointsCount - 1).coerceAtLeast(1)
                val graphHeight = size.height - 30.dp.toPx()

                if (offset.y <= graphHeight) {
                    val rawIndex = (offset.x / xSpacing).toInt()
                    val remainder = offset.x % xSpacing
                    val closestIndex = if (remainder > xSpacing / 2f) rawIndex + 1 else rawIndex
                    val foundIdx = closestIndex.coerceIn(0, pointsCount - 1)
                    selectedIndex = if (foundIdx == selectedIndex) null else foundIdx
                } else {
                    selectedIndex = null
                }
            }
        }
    ) {
        val width = size.width
        val height = size.height
        val graphHeight = height - 30.dp.toPx()
        val pointsCount = values.size
        val xSpacing = width / (pointsCount - 1).coerceAtLeast(1)

        val points = values.mapIndexed { index, value ->
            val x = index * xSpacing
            val ratio = ((value - minVal) / range).toFloat()
            val y = graphHeight - (ratio * graphHeight * 0.8f * animationProgress.value)
            Offset(x, y)
        }

        // Draw subtle background grid
        val numGridLines = 3
        for (i in 0..numGridLines) {
            val y = (graphHeight / numGridLines) * i
            drawLine(
                color = Color.Gray.copy(alpha = 0.10f),
                start = Offset(0f, y),
                end = Offset(width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        // Create bezier path
        val path = Path()
        val fillPath = Path()

        if (points.isNotEmpty()) {
            path.moveTo(points[0].x, points[0].y)
            fillPath.moveTo(points[0].x, points[0].y)

            for (i in 0 until points.size - 1) {
                val p0 = points[i]
                val p1 = points[i + 1]
                val controlPoint1 = Offset(p0.x + (p1.x - p0.x) / 2f, p0.y)
                val controlPoint2 = Offset(p0.x + (p1.x - p0.x) / 2f, p1.y)

                path.cubicTo(
                    controlPoint1.x, controlPoint1.y,
                    controlPoint2.x, controlPoint2.y,
                    p1.x, p1.y
                )
                fillPath.cubicTo(
                    controlPoint1.x, controlPoint1.y,
                    controlPoint2.x, controlPoint2.y,
                    p1.x, p1.y
                )
            }

            // Close the path for gradient fill
            fillPath.lineTo(points.last().x, graphHeight)
            fillPath.lineTo(points.first().x, graphHeight)
            fillPath.close()

            // Draw gradient fill
            drawPath(
                path = fillPath,
                brush = Brush.verticalGradient(
                    colors = listOf(gradientColor.copy(alpha = 0.35f), Color.Transparent),
                    startY = 0f,
                    endY = graphHeight
                )
            )

            // Draw line
            drawPath(
                path = path,
                color = lineColor,
                style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
            )

            // Draw connection dots
            val step = (points.size / 7).coerceAtLeast(1)
            for (i in points.indices step step) {
                val p = points[i]
                val isSelected = selectedIndex == i
                drawCircle(
                    color = lineColor,
                    radius = if (isSelected) 6.dp.toPx() else 4.dp.toPx(),
                    center = p
                )
                drawCircle(
                    color = Color.White,
                    radius = if (isSelected) 3.dp.toPx() else 2.dp.toPx(),
                    center = p
                )

                // Draw X Label below the dot
                val label = labels.getOrNull(i) ?: ""
                val textLayoutResult = textMeasurer.measure(label, style = labelStyle)
                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(
                        x = (p.x - textLayoutResult.size.width / 2).coerceIn(0f, width - textLayoutResult.size.width),
                        y = graphHeight + 6.dp.toPx()
                    )
                )
            }

            // Draw interactive tooltip when tapped
            selectedIndex?.let { index ->
                val p = points.getOrNull(index)
                if (p != null) {
                    // Draw vertical dotted guide line
                    drawLine(
                        color = lineColor.copy(alpha = 0.5f),
                        start = Offset(p.x, 0f),
                        end = Offset(p.x, graphHeight),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )

                    // Draw highlight circles
                    drawCircle(
                        color = lineColor,
                        radius = 8.dp.toPx(),
                        center = p
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4.dp.toPx(),
                        center = p
                    )

                    val valText = "₹${String.format(Locale.getDefault(), "%,.0f", values[index])}"
                    val dateLabel = labels.getOrNull(index) ?: ""
                    val tooltipText = "$dateLabel: $valText"
                    val tooltipResult = textMeasurer.measure(
                        tooltipText,
                        style = TextStyle(
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    val tooltipWidth = tooltipResult.size.width + 16.dp.toPx()
                    val tooltipHeight = tooltipResult.size.height + 8.dp.toPx()

                    var tooltipX = p.x - tooltipWidth / 2f
                    if (tooltipX < 4.dp.toPx()) tooltipX = 4.dp.toPx()
                    if (tooltipX + tooltipWidth > width - 4.dp.toPx()) tooltipX = width - tooltipWidth - 4.dp.toPx()

                    var tooltipY = p.y - tooltipHeight - 8.dp.toPx()
                    if (tooltipY < 4.dp.toPx()) tooltipY = p.y + 8.dp.toPx()

                    drawRoundRect(
                        color = Color.Black.copy(alpha = 0.85f),
                        topLeft = Offset(tooltipX, tooltipY),
                        size = Size(tooltipWidth, tooltipHeight),
                        cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                    )
                    drawText(
                        textLayoutResult = tooltipResult,
                        topLeft = Offset(tooltipX + 8.dp.toPx(), tooltipY + 4.dp.toPx())
                    )
                }
            }
        }
    }
}

@Composable
fun ChartLegendItem(
    color: Color,
    label: String,
    modifier: Modifier = Modifier
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(color)
        )
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
