package com.example

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

@Composable
fun AnimatedHeaderIcon(
    icon: ImageVector,
    backgroundColor: Color = Color.Gray,
    iconColor: Color = Color.White,
    tint: Color = Color.White,
    shape: Shape = CircleShape,
    useSurface: Boolean = false,
    iconSize: Dp = 24.dp,
    boxSize: Dp = 40.dp,
    padding: Dp = 0.dp
) {
    val actualTint = if (tint != Color.White) tint else iconColor
    if (useSurface) {
        Surface(
            shape = shape,
            color = backgroundColor,
            modifier = Modifier.size(boxSize).padding(padding)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = actualTint,
                    modifier = Modifier.size(iconSize)
                )
            }
        }
    } else {
        Box(
            modifier = Modifier
                .size(boxSize)
                .padding(padding)
                .background(backgroundColor, shape),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = actualTint,
                modifier = Modifier.size(iconSize)
            )
        }
    }
}
