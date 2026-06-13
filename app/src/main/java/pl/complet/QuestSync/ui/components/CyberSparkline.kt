package pl.complet.QuestSync.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import pl.complet.QuestSync.ui.theme.RipperGold

@Composable
fun CyberSparkline(
    data: List<Float>,
    modifier: Modifier = Modifier.fillMaxWidth().height(40.dp)
) {
    Canvas(modifier = modifier) {
        if (data.size < 2) {
            drawLine(
                color = RipperGold.copy(alpha = 0.2f),
                start = Offset(0f, size.height / 2),
                end = Offset(size.width, size.height / 2),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f),
                strokeWidth = 1.dp.toPx()
            )
            return@Canvas
        }

        val minData = data.minOrNull() ?: 0f
        val maxData = data.maxOrNull() ?: 1f
        val range = (maxData - minData).coerceAtLeast(1f)
        
        val widthStep = size.width / (data.size - 1)
        
        val path = Path().apply {
            data.forEachIndexed { index, value ->
                val x = index * widthStep
                val y = size.height - ((value - minData) / range * size.height)
                if (index == 0) moveTo(x, y) else lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = RipperGold.copy(alpha = 0.1f),
            style = Stroke(width = 6.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )
        
        drawPath(
            path = path,
            color = RipperGold,
            style = Stroke(width = 1.5.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
        )

        data.forEachIndexed { index, value ->
            val x = index * widthStep
            val y = size.height - ((value - minData) / range * size.height)
            drawCircle(
                color = RipperGold,
                radius = 2.dp.toPx(),
                center = Offset(x, y)
            )
        }
    }
}
