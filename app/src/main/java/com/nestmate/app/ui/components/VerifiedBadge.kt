package com.nestmate.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

@Composable
fun VerifiedBadge(
    isProvider: Boolean = false,
    showText: Boolean = false,
    modifier: Modifier = Modifier
) {
    val color = if (isProvider) Color(0xFF1976D2) else Color(0xFF388E3C)
    val textLabel = if (isProvider) "Verified Provider" else "Verified Student"

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        if (isProvider) {
            Icon(
                imageVector = Icons.Default.Shield,
                contentDescription = textLabel,
                tint = color,
                modifier = Modifier.size(16.dp)
            )
        } else {
            Box(
                modifier = Modifier
                    .size(16.dp)
                    .clip(CircleShape)
                    .background(color),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = textLabel,
                    tint = Color.White,
                    modifier = Modifier.size(12.dp)
                )
            }
        }
        
        if (showText) {
            Spacer(modifier = Modifier.width(4.dp))
            Text(
                text = textLabel,
                style = MaterialTheme.typography.labelSmall,
                color = color
            )
        }
    }
}
