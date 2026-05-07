package com.nathanjones.planeaboveyou.ui.screens

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun SettingsScreen(currentFOV: Double, onFOVChanged: (Double) -> Unit, onDismiss: () -> Unit, onShowOnboardingAgain: () -> Unit = {}) {
    val context = LocalContext.current
    var fovValue by remember { mutableFloatStateOf(currentFOV.toFloat()) }
    var keepScreenAwake by remember {
        mutableStateOf(
            context.getSharedPreferences("plane_prefs", 0)
                .getBoolean("keep_screen_awake", false)
        )
    }
    var showSleepDialog by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Settings,
                    contentDescription = null,
                    modifier = Modifier.size(24.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Settings", fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column(modifier = Modifier.fillMaxWidth()) {
                // Field of View
                Text(
                    "Field of View",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.padding(bottom = 4.dp)
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Radius", fontSize = 14.sp)
                    Text(
                        String.format("%.1f miles", fovValue),
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Slider(
                    value = fovValue,
                    onValueChange = { fovValue = it },
                    onValueChangeFinished = { onFOVChanged(fovValue.toDouble()) },
                    valueRange = 1f..50f,
                    steps = 97,
                    modifier = Modifier.padding(vertical = 8.dp)
                )

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Keep Screen Awake
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Keep Screen Awake",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            if (keepScreenAwake) "Enabled" else "Disabled",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    Switch(
                        checked = keepScreenAwake,
                        onCheckedChange = { checked ->
                            if (checked) {
                                showSleepDialog = true
                            } else {
                                keepScreenAwake = false
                                context.getSharedPreferences("plane_prefs", 0)
                                    .edit()
                                    .putBoolean("keep_screen_awake", false)
                                    .apply()
                            }
                        }
                    )
                }

                Text(
                    "Used for plane detection and notifications.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp)
                )

                Divider(modifier = Modifier.padding(vertical = 12.dp))

                // Show Onboarding
                Button(
                    onClick = {
                        context.getSharedPreferences("plane_prefs", 0)
                            .edit()
                            .putBoolean("has_seen_onboarding", false)
                            .apply()
                        onShowOnboardingAgain()
                        onDismiss()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text("Show Onboarding Again")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Done")
            }
        }
    )

    // Sleep prevention confirmation dialog
    if (showSleepDialog) {
        AlertDialog(
            onDismissRequest = { showSleepDialog = false },
            title = { Text("Enable Sleep Prevention?") },
            text = {
                Text("When enabled, your screen will stay awake while the app is open. This may use more battery.")
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        keepScreenAwake = true
                        context.getSharedPreferences("plane_prefs", 0)
                            .edit()
                            .putBoolean("keep_screen_awake", true)
                            .apply()
                        showSleepDialog = false
                    }
                ) {
                    Text("Enable")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSleepDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
