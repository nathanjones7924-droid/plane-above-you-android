package com.nathanjones.planeaboveyou.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.viewmodel.compose.viewModel
import com.nathanjones.planeaboveyou.ui.screens.*
import com.nathanjones.planeaboveyou.viewmodel.FlightViewModel
import kotlinx.coroutines.launch

@Composable
fun PlaneAboveYouApp(viewModel: FlightViewModel = viewModel()) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var showOnboarding by remember {
        mutableStateOf(
            !context.getSharedPreferences("plane_prefs", 0).getBoolean("has_seen_onboarding", false)
        )
    }

    var permissionGranted by remember {
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }

    val launcher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perms ->
        permissionGranted = perms.values.any { it }
        if (permissionGranted) viewModel.start()
    }

    LaunchedEffect(Unit) {
        if (!permissionGranted) {
            launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
        } else {
            viewModel.start()
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        if (showOnboarding) {
            OnboardingScreen(
                onFinish = {
                    showOnboarding = false
                    context.getSharedPreferences("plane_prefs", 0).edit().putBoolean("has_seen_onboarding", true).apply()
                    if (!permissionGranted) {
                        launcher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION))
                    }
                }
            )
        } else {
            MainScreen(viewModel = viewModel)
        }
    }
}
