package com.nestmate.app.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun NestMateCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val shape = RoundedCornerShape(20.dp)
    val colors = CardDefaults.cardColors(
        containerColor = MaterialTheme.colorScheme.surfaceContainerHigh
    )
    val elevation = CardDefaults.cardElevation(
        defaultElevation = 2.dp,
        pressedElevation = 4.dp
    )
    val border = BorderStroke(
        0.5.dp,
        MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)
    )

    if (onClick != null) {
        Card(
            onClick = onClick,
            modifier = modifier.padding(vertical = 6.dp),
            shape = shape,
            elevation = elevation,
            colors = colors,
            border = border
        ) {
            content()
        }
    } else {
        Card(
            modifier = modifier.padding(vertical = 6.dp),
            shape = shape,
            elevation = elevation,
            colors = colors,
            border = border
        ) {
            content()
        }
    }
}
