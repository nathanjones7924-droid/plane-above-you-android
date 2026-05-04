package com.nathanjones.planeaboveyou.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@Composable
fun OnboardingScreen(onFinish: () -> Unit) {
    var page by remember { mutableIntStateOf(0) }
    val totalPages = 4
    val coroutineScope = rememberCoroutineScope()
    var planeOffset by remember { mutableFloatStateOf(-400f) }
    var showContent by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        showContent = true
        coroutineScope.launch {
            while (true) {
                planeOffset = -400f
                delay(100)
                planeOffset = 400f
                delay(4000)
            }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Background gradient shifts per page
        AnimatedContent(
            targetState = page,
            transitionSpec = { fadeIn(animationSpec = tween(600)) togetherWith fadeOut(animationSpec = tween(600)) }
        ) { targetPage ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.linearGradient(
                            colors = gradientColors(targetPage)
                        )
                    )
            )
        }

        // Floating particles (simple dots)
        FloatingDots()

        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Skip button
            Box(modifier = Modifier.fillMaxWidth().padding(top = 16.dp, end = 24.dp)) {
                if (page < totalPages - 1) {
                    TextButton(
                        onClick = { page = totalPages - 1 },
                        modifier = Modifier.align(Alignment.TopEnd)
                    ) {
                        Text("Skip", color = Color.White.copy(alpha = 0.7f))
                    }
                }
            }

            Spacer(modifier = Modifier.weight(0.5f))

            // Page content
            Crossfade(targetState = page, animationSpec = tween(600)) { targetPage ->
                when (targetPage) {
                    0 -> WelcomePage(planeOffset = planeOffset, showContent = showContent)
                    1 -> FOVPage()
                    2 -> LocationPage()
                    3 -> SleepPage(onKeepAwakeChanged = { /* handled in page */ })
                }
            }

            Spacer(modifier = Modifier.weight(0.3f))

            // Page indicators
            Row(
                modifier = Modifier.padding(bottom = 24.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                repeat(totalPages) { i ->
                    Box(
                        modifier = Modifier
                            .width(if (i == page) 24.dp else 8.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(if (i == page) Color.White else Color.White.copy(alpha = 0.35f))
                    )
                }
            }

            // Action button
            Button(
                onClick = {
                    if (page < totalPages - 1) {
                        page++
                    } else {
                        onFinish()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 32.dp)
                    .height(56.dp),
                shape = RoundedCornerShape(16.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color.White)
            ) {
                Text(
                    if (page < totalPages - 1) "Continue" else "Get Started",
                    color = buttonTextColor(page),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

@Composable
private fun WelcomePage(planeOffset: Float, showContent: Boolean) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            // Radar rings
            repeat(3) { ring ->
                Box(
                    modifier = Modifier
                        .size((100 + ring * 60).dp)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(CircleShape)
                            .background(Color.White.copy(alpha = 0.08f + ring * 0.04f))
                    )
                }
            }

            // Animated plane
            Icon(
                imageVector = Icons.Default.AirplanemodeActive,
                contentDescription = null,
                modifier = Modifier
                    .size(28.dp)
                    .rotate(-45f)
                    .offset(x = planeOffset.dp, y = 0.dp),
                tint = Color.White.copy(alpha = 0.25f)
            )

            // Center icon
            Icon(
                imageVector = Icons.Default.AirplanemodeActive,
                contentDescription = null,
                modifier = Modifier.size(80.dp),
                tint = Color.White
            )
        }

        Text(
            "Plane Above You",
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "See every flight overhead\nin real time.",
            fontSize = 18.sp,
            color = Color.White.copy(alpha = 0.8f),
            lineHeight = 24.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun FOVPage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.15f))
            )

            Box(
                modifier = Modifier
                    .size(180.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color.White.copy(alpha = 0.2f), Color.Transparent)
                        )
                    )
            )

            // Dotted planes
            val fovPlanes = listOf(
                Triple(-55f, -40f, -30f),
                Triple(60f, -25f, 45f),
                Triple(-30f, 50f, -60f),
                Triple(45f, 55f, 120f),
                Triple(-70f, 10f, 90f)
            )
            fovPlanes.forEach { (x, y, rotation) ->
                Icon(
                    imageVector = Icons.Default.AirplanemodeActive,
                    contentDescription = null,
                    modifier = Modifier
                        .size(14.dp)
                        .rotate(rotation)
                        .offset(x.dp, y.dp),
                    tint = Color.White.copy(alpha = 0.7f)
                )
            }

            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color.White)
            )

            Icon(
                imageVector = Icons.Default.Visibility,
                contentDescription = null,
                modifier = Modifier
                    .size(48.dp)
                    .offset(y = (-120).dp),
                tint = Color.White.copy(alpha = 0.9f)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Your Field of View",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Customize how much sky you scan.\nAdjust your FOV radius from 1 to 50 miles in Settings.",
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.75f),
            lineHeight = 22.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun LocationPage() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(200.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(Color.White.copy(alpha = 0.08f))
            )

            Icon(
                imageVector = Icons.Default.Add,
                contentDescription = null,
                modifier = Modifier.size(40.dp),
                tint = Color.White.copy(alpha = 0.2f)
            )

            Icon(
                imageVector = Icons.Default.MyLocation,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = Color.White
            )

            repeat(2) { i ->
                Box(
                    modifier = Modifier
                        .size((120 + i * 50).dp)
                        .clip(CircleShape)
                        .background(Color.Transparent)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.White.copy(alpha = 0.12f))
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Location Access",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Allow location access so we can\nfind planes flying above you.",
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.75f),
            lineHeight = 22.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
    }
}

@Composable
private fun SleepPage(onKeepAwakeChanged: (Boolean) -> Unit) {
    val context = LocalContext.current
    var keepScreenAwake by remember {
        mutableStateOf(
            context.getSharedPreferences("plane_prefs", 0)
                .getBoolean("keep_screen_awake", false)
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 32.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(220.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(Color.White.copy(alpha = 0.1f))
            )

            Box(
                modifier = Modifier
                    .size(140.dp)
                    .clip(RoundedCornerShape(18.dp))
                    .background(Color.White.copy(alpha = 0.14f))
            )

            Icon(
                imageVector = if (keepScreenAwake) Icons.Default.LockOpen else Icons.Default.Lock,
                contentDescription = null,
                modifier = Modifier.size(52.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Sleep Prevention",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "Keep your screen awake while using the app.\nYou can change this later in Settings.",
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.75f),
            lineHeight = 22.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        // Toggle
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(Color.White.copy(alpha = 0.15f))
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    "Keep Screen Awake",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
                Text(
                    if (keepScreenAwake) "Enabled" else "Disabled",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
            Switch(
                checked = keepScreenAwake,
                onCheckedChange = { checked ->
                    keepScreenAwake = checked
                    context.getSharedPreferences("plane_prefs", 0)
                        .edit()
                        .putBoolean("keep_screen_awake", checked)
                        .apply()
                    onKeepAwakeChanged(checked)
                },
                colors = SwitchDefaults.colors(
                    checkedThumbColor = Color(0xFF4CAF50),
                    checkedTrackColor = Color(0xFF4CAF50).copy(alpha = 0.5f)
                )
            )
        }
    }
}

@Composable
private fun FloatingDots() {
    Box(modifier = Modifier.fillMaxSize()) {
        repeat(15) { i ->
            val x = (i * 73) % 100 / 100f
            val y = (i * 37) % 100 / 100f
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(
                        start = (x * 400).dp,
                        top = (y * 800).dp
                    )
            ) {
                Box(
                    modifier = Modifier
                        .size((2 + i % 3).dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.15f + (i % 3) * 0.1f))
                )
            }
        }
    }
}

private fun gradientColors(page: Int): List<Color> {
    return when (page) {
        0 -> listOf(Color(0xFF1A1A5C), Color(0xFF0D386B))
        1 -> listOf(Color(0xFF14335C), Color(0xFF1E5999))
        2 -> listOf(Color(0xFF0F2666), Color(0xFF264D8C))
        else -> listOf(Color(0xFF1E2E5C), Color(0xFF2E4266))
    }
}

private fun buttonTextColor(page: Int): Color {
    return when (page) {
        0 -> Color(0xFF1A1A5C)
        1 -> Color(0xFF14335C)
        2 -> Color(0xFF0F2666)
        else -> Color(0xFF1E2E5C)
    }
}
