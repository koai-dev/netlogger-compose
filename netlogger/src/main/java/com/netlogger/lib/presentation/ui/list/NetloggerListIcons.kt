package com.netlogger.lib.presentation.ui.list

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp

@Composable
internal fun TerminalIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(40.dp)) {
        val stroke = Stroke(width = 4.dp.toPx(), cap = StrokeCap.Square)
        drawRect(NetloggerListColors.Ink, style = stroke)
        drawLine(NetloggerListColors.Ink, Offset(9.dp.toPx(), 10.dp.toPx()), Offset(18.dp.toPx(), 20.dp.toPx()), stroke.width)
        drawLine(NetloggerListColors.Ink, Offset(18.dp.toPx(), 20.dp.toPx()), Offset(9.dp.toPx(), 30.dp.toPx()), stroke.width)
        drawLine(NetloggerListColors.Ink, Offset(21.dp.toPx(), 28.dp.toPx()), Offset(32.dp.toPx(), 28.dp.toPx()), stroke.width)
    }
}

@Composable
internal fun TrashIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(34.dp)) {
        val color = NetloggerListColors.Red
        val stroke = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Square)
        drawLine(color, Offset(5.dp.toPx(), 7.dp.toPx()), Offset(29.dp.toPx(), 7.dp.toPx()), stroke.width)
        drawLine(color, Offset(12.dp.toPx(), 3.dp.toPx()), Offset(22.dp.toPx(), 3.dp.toPx()), stroke.width)
        drawLine(color, Offset(9.dp.toPx(), 9.dp.toPx()), Offset(10.dp.toPx(), 31.dp.toPx()), stroke.width)
        drawLine(color, Offset(25.dp.toPx(), 9.dp.toPx()), Offset(24.dp.toPx(), 31.dp.toPx()), stroke.width)
        drawLine(color, Offset(10.dp.toPx(), 31.dp.toPx()), Offset(24.dp.toPx(), 31.dp.toPx()), stroke.width)
        drawLine(color, Offset(15.dp.toPx(), 13.dp.toPx()), Offset(15.dp.toPx(), 27.dp.toPx()), stroke.width)
        drawLine(color, Offset(20.dp.toPx(), 13.dp.toPx()), Offset(20.dp.toPx(), 27.dp.toPx()), stroke.width)
    }
}

@Composable
internal fun SearchIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(40.dp)) {
        val color = NetloggerListColors.Muted
        drawCircle(color, radius = 13.dp.toPx(), center = Offset(17.dp.toPx(), 17.dp.toPx()), style = Stroke(4.dp.toPx()))
        drawLine(color, Offset(27.dp.toPx(), 27.dp.toPx()), Offset(37.dp.toPx(), 37.dp.toPx()), 4.dp.toPx(), StrokeCap.Square)
    }
}

@Composable
internal fun ClearIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(34.dp)) {
        val color = NetloggerListColors.Muted
        drawLine(color, Offset(6.dp.toPx(), 6.dp.toPx()), Offset(28.dp.toPx(), 28.dp.toPx()), 4.dp.toPx(), StrokeCap.Square)
        drawLine(color, Offset(28.dp.toPx(), 6.dp.toPx()), Offset(6.dp.toPx(), 28.dp.toPx()), 4.dp.toPx(), StrokeCap.Square)
    }
}

@Composable
internal fun FilterIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(38.dp)) {
        val color = NetloggerListColors.Muted
        drawLine(color, Offset(4.dp.toPx(), 9.dp.toPx()), Offset(34.dp.toPx(), 9.dp.toPx()), 4.dp.toPx(), StrokeCap.Square)
        drawLine(color, Offset(10.dp.toPx(), 18.dp.toPx()), Offset(28.dp.toPx(), 18.dp.toPx()), 4.dp.toPx(), StrokeCap.Square)
        drawLine(color, Offset(17.dp.toPx(), 27.dp.toPx()), Offset(22.dp.toPx(), 27.dp.toPx()), 4.dp.toPx(), StrokeCap.Square)
    }
}

@Composable
internal fun GearIcon(modifier: Modifier = Modifier) {
    Canvas(modifier = modifier.size(44.dp)) {
        val c = center
        val path = Path()
        val outer = 21.dp.toPx()
        val inner = 16.dp.toPx()
        repeat(16) { index ->
            val angle = Math.toRadians((index * 22.5 - 90).toDouble())
            val radius = if (index % 2 == 0) outer else inner
            val point = Offset(
                x = c.x + kotlin.math.cos(angle).toFloat() * radius,
                y = c.y + kotlin.math.sin(angle).toFloat() * radius
            )
            if (index == 0) path.moveTo(point.x, point.y) else path.lineTo(point.x, point.y)
        }
        path.close()
        drawPath(path, NetloggerListColors.Gear)
        drawCircle(Color.White, radius = 7.dp.toPx(), center = c)
    }
}

@Preview(showBackground = true)
@Composable
private fun NetloggerIconsPreview() {
    androidx.compose.foundation.layout.Row {
        TerminalIcon()
        TrashIcon()
        SearchIcon()
        ClearIcon()
        FilterIcon()
        GearIcon()
    }
}
