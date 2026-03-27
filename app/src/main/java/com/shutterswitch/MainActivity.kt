package com.shutterswitch

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

class MainActivity : ComponentActivity() {

    // Permission launcher for POST_NOTIFICATIONS (Android 13+)
    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* Notification will show if granted; silently ignored if denied */ }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Request notification permission on Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            ShutterSwitchTheme {
                ShutterSwitchScreen(
                    onSwitchOn = { startWakeLockService() },
                    onSwitchOff = { stopWakeLockService() }
                )
            }
        }
    }

    private fun startWakeLockService() {
        val intent = Intent(this, WakeLockService::class.java)
        ContextCompat.startForegroundService(this, intent)
    }

    private fun stopWakeLockService() {
        val intent = Intent(this, WakeLockService::class.java)
        stopService(intent)
    }

    override fun onDestroy() {
        super.onDestroy()
        // Stop the service when activity is destroyed to avoid orphaned wake locks
        stopWakeLockService()
    }
}

// ─────────────────────────────────────────────
// Compose UI
// ─────────────────────────────────────────────

@Composable
fun ShutterSwitchScreen(
    onSwitchOn: () -> Unit,
    onSwitchOff: () -> Unit
) {
    var isOn by remember { mutableStateOf(false) }

    // Animate background gradient
    val bgColorTop by animateColorAsState(
        targetValue = if (isOn) Color(0xFF0A1628) else Color(0xFF1A1A2E),
        animationSpec = tween(600), label = "bgTop"
    )
    val bgColorBottom by animateColorAsState(
        targetValue = if (isOn) Color(0xFF001F4D) else Color(0xFF16213E),
        animationSpec = tween(600), label = "bgBottom"
    )

    // Glow scale animation
    val glowScale by animateFloatAsState(
        targetValue = if (isOn) 1.4f else 0.6f,
        animationSpec = spring(stiffness = Spring.StiffnessLow), label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(bgColorTop, bgColorBottom))),
        contentAlignment = Alignment.Center
    ) {
        // Ambient glow blob behind the switch
        Box(
            modifier = Modifier
                .size(320.dp)
                .scale(glowScale)
                .blur(80.dp)
                .background(
                    color = if (isOn) Color(0x6600CFFF) else Color(0x22334455),
                    shape = CircleShape
                )
        )

        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            // App title
            Text(
                text = "SHUTTER\nSWITCH",
                fontSize = 36.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 8.sp,
                lineHeight = 42.sp,
                color = Color(0xFFE0F4FF),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Status subtitle
            val statusText by remember(isOn) {
                derivedStateOf { if (isOn) "WAKE LOCK ACTIVE" else "DEVICE MAY SLEEP" }
            }
            val statusColor by animateColorAsState(
                targetValue = if (isOn) Color(0xFF00CFFF) else Color(0xFF667788),
                animationSpec = tween(400), label = "statusColor"
            )
            Text(
                text = statusText,
                fontSize = 13.sp,
                fontWeight = FontWeight.Medium,
                letterSpacing = 3.sp,
                color = statusColor
            )

            Spacer(modifier = Modifier.height(72.dp))

            // The large Shutter Switch toggle
            LargeShutterToggle(
                isOn = isOn,
                onToggle = { newState ->
                    isOn = newState
                    if (newState) onSwitchOn() else onSwitchOff()
                }
            )

            Spacer(modifier = Modifier.height(56.dp))

            // Info card
            InfoCard(isOn = isOn)
        }
    }
}

@Composable
fun LargeShutterToggle(
    isOn: Boolean,
    onToggle: (Boolean) -> Unit
) {
    val trackColor by animateColorAsState(
        targetValue = if (isOn) Color(0xFF005F7A) else Color(0xFF1E2A38),
        animationSpec = tween(400), label = "track"
    )
    val thumbColor by animateColorAsState(
        targetValue = if (isOn) Color(0xFF00CFFF) else Color(0xFF445566),
        animationSpec = tween(400), label = "thumb"
    )
    val thumbOffsetDp by animateDpAsState(
        targetValue = if (isOn) 68.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "thumbOffset"
    )

    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        // Track
        Box(
            modifier = Modifier
                .width(160.dp)
                .height(88.dp)
                .clip(RoundedCornerShape(44.dp))
                .background(trackColor)
                .padding(8.dp),
            contentAlignment = Alignment.CenterStart
        ) {
            // Thumb
            Box(
                modifier = Modifier
                    .offset(x = thumbOffsetDp)
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(thumbColor),
                contentAlignment = Alignment.Center
            ) {
                // Power icon indicator
                Text(
                    text = if (isOn) "⏻" else "⏼",
                    fontSize = 28.sp,
                    color = if (isOn) Color(0xFF001A22) else Color(0xFF223344)
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // ON / OFF tap button
        Button(
            onClick = { onToggle(!isOn) },
            modifier = Modifier
                .width(160.dp)
                .height(52.dp),
            shape = RoundedCornerShape(26.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = if (isOn) Color(0xFF00CFFF) else Color(0xFF2A3A4A),
                contentColor = if (isOn) Color(0xFF001A22) else Color(0xFF99BBCC)
            ),
            elevation = ButtonDefaults.buttonElevation(
                defaultElevation = if (isOn) 8.dp else 2.dp
            )
        ) {
            Text(
                text = if (isOn) "TURN OFF" else "TURN ON",
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                letterSpacing = 2.sp
            )
        }
    }
}

@Composable
fun InfoCard(isOn: Boolean) {
    val cardBg by animateColorAsState(
        targetValue = if (isOn) Color(0x2200CFFF) else Color(0x111E2A38),
        animationSpec = tween(500), label = "cardBg"
    )
    val borderColor by animateColorAsState(
        targetValue = if (isOn) Color(0x6600CFFF) else Color(0x221E2A38),
        animationSpec = tween(500), label = "border"
    )

    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp)),
        color = cardBg,
        tonalElevation = 0.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = if (isOn)
                    "🔒  Wake lock acquired\nThe CPU will not sleep while this is active."
                else
                    "💤  No wake lock held\nThe device may sleep normally.",
                fontSize = 14.sp,
                color = if (isOn) Color(0xFFAAEEFF) else Color(0xFF667788),
                textAlign = TextAlign.Center,
                lineHeight = 22.sp
            )
        }
    }
}

// ─────────────────────────────────────────────
// Theme
// ─────────────────────────────────────────────

@Composable
fun ShutterSwitchTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = Color(0xFF00CFFF),
            background = Color(0xFF0A1628),
            surface = Color(0xFF16213E)
        ),
        content = content
    )
}
