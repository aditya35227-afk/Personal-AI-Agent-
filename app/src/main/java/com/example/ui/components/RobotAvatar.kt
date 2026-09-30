package com.example.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.domain.model.ExecutionState
import com.example.ui.theme.RobotError
import com.example.ui.theme.RobotExecuting
import com.example.ui.theme.RobotListening
import com.example.ui.theme.RobotSuccess
import com.example.ui.theme.RobotThinking

@Composable
fun RobotAvatar(
    state: ExecutionState,
    currentQuery: String,
    statusMessage: String,
    onDismiss: () -> Unit,
    onActivateMic: () -> Unit,
    modifier: Modifier = Modifier,
    animationEnabled: Boolean = true
) {
    val infiniteTransition = rememberInfiniteTransition(label = "robot_anim")

    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val eyeGlow by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1.0f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "eye_glow"
    )

    val stateColor = when (state) {
        ExecutionState.LISTENING -> RobotListening
        ExecutionState.THINKING -> RobotThinking
        ExecutionState.EXECUTING -> RobotExecuting
        ExecutionState.SUCCESS -> RobotSuccess
        ExecutionState.ERROR -> RobotError
        ExecutionState.PERMISSION_REQUIRED -> Color(0xFFF97316)
        ExecutionState.CONFIRMATION_REQUIRED -> Color(0xFFEAB308)
        ExecutionState.IDLE -> MaterialTheme.colorScheme.primary
    }

    val stateLabel = when (state) {
        ExecutionState.LISTENING -> "Listening..."
        ExecutionState.THINKING -> "Thinking..."
        ExecutionState.EXECUTING -> "Taking action..."
        ExecutionState.SUCCESS -> "Done!"
        ExecutionState.ERROR -> "Notice"
        ExecutionState.PERMISSION_REQUIRED -> "Permission Needed"
        ExecutionState.CONFIRMATION_REQUIRED -> "Confirm Action"
        ExecutionState.IDLE -> "Ready"
    }

    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(16.dp)
            .testTag("robot_avatar_card"),
        shape = RoundedCornerShape(28.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f),
        tonalElevation = 6.dp,
        shadowElevation = 8.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Top action bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                // State Badge
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier
                        .clip(RoundedCornerShape(12.dp))
                        .background(stateColor.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 5.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(stateColor)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = stateLabel,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = stateColor
                    )
                }

                IconButton(
                    onClick = onDismiss,
                    modifier = Modifier.size(32.dp).testTag("close_robot_button")
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Minimize Robot",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Animated Robot Head Canvas
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier.size(130.dp)
            ) {
                Canvas(
                    modifier = Modifier
                        .size(if (animationEnabled && state != ExecutionState.IDLE) 120.dp * pulseScale else 120.dp)
                ) {
                    val w = size.width
                    val h = size.height

                    // Antenna stem
                    drawLine(
                        color = stateColor,
                        start = Offset(w * 0.5f, h * 0.20f),
                        end = Offset(w * 0.5f, h * 0.08f),
                        strokeWidth = 6f,
                        cap = StrokeCap.Round
                    )

                    // Antenna ball
                    drawCircle(
                        color = stateColor,
                        radius = 10f,
                        center = Offset(w * 0.5f, h * 0.08f)
                    )

                    // Head outer body
                    drawRoundRect(
                        brush = Brush.linearGradient(
                            listOf(
                                Color(0xFF1E293B),
                                Color(0xFF0F172A)
                            )
                        ),
                        topLeft = Offset(w * 0.15f, h * 0.22f),
                        size = Size(w * 0.70f, h * 0.58f),
                        cornerRadius = CornerRadius(34f, 34f)
                    )

                    // Glowing visor frame
                    drawRoundRect(
                        color = stateColor,
                        topLeft = Offset(w * 0.15f, h * 0.22f),
                        size = Size(w * 0.70f, h * 0.58f),
                        cornerRadius = CornerRadius(34f, 34f),
                        style = Stroke(width = 4f)
                    )

                    // Visor screen
                    drawRoundRect(
                        color = Color(0xFF030712),
                        topLeft = Offset(w * 0.23f, h * 0.32f),
                        size = Size(w * 0.54f, h * 0.32f),
                        cornerRadius = CornerRadius(20f, 20f)
                    )

                    // Eyes
                    val eyeAlpha = if (animationEnabled) eyeGlow else 0.9f
                    val eyeColor = stateColor.copy(alpha = eyeAlpha)

                    // Left Eye
                    drawCircle(
                        color = eyeColor,
                        radius = 11f,
                        center = Offset(w * 0.38f, h * 0.48f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = eyeAlpha),
                        radius = 4f,
                        center = Offset(w * 0.36f, h * 0.46f)
                    )

                    // Right Eye
                    drawCircle(
                        color = eyeColor,
                        radius = 11f,
                        center = Offset(w * 0.62f, h * 0.48f)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = eyeAlpha),
                        radius = 4f,
                        center = Offset(w * 0.60f, h * 0.46f)
                    )

                    // Subtle smile / audio line
                    if (state == ExecutionState.LISTENING || state == ExecutionState.SUCCESS) {
                        drawLine(
                            color = stateColor.copy(alpha = 0.8f),
                            start = Offset(w * 0.42f, h * 0.70f),
                            end = Offset(w * 0.58f, h * 0.70f),
                            strokeWidth = 4f,
                            cap = StrokeCap.Round
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Query or status display
            if (currentQuery.isNotBlank()) {
                Text(
                    text = "\"$currentQuery\"",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    maxLines = 2
                )
                Spacer(modifier = Modifier.height(6.dp))
            }

            Text(
                text = statusMessage.ifBlank { "How can I help with your studies today?" },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                lineHeight = 18.sp
            )

            if (state == ExecutionState.IDLE) {
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(20.dp))
                        .background(MaterialTheme.colorScheme.primaryContainer)
                        .clickable(onClick = onActivateMic)
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .testTag("tap_to_speak_badge"),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Mic,
                        contentDescription = "Tap to speak",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Tap to speak",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }
    }
}
