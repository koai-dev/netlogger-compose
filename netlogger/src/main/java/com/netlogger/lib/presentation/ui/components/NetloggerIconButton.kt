package com.netlogger.lib.presentation.ui.components

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp

@Composable
fun NetloggerIconButton(
    modifier: Modifier = Modifier,
    icon: Int,
    colorFilter: Color = Color.Unspecified,
    onClick: (() -> Unit)? = null
) {
    Image(
        painter = painterResource(icon),
        contentDescription = null,
        modifier = modifier
            .size(40.dp)
            .padding(8.dp)
            .clickable(enabled = onClick != null, onClick = { onClick?.invoke() }),
        colorFilter = ColorFilter.tint(colorFilter)
    )
}