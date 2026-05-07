package com.nathanjones.planeaboveyou.ui.screens

import android.annotation.SuppressLint
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.CameraUpdateFactory
import com.google.android.gms.maps.model.BitmapDescriptorFactory
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.nathanjones.planeaboveyou.model.Flight
import com.nathanjones.planeaboveyou.viewmodel.FlightViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@SuppressLint("MissingPermission")
@Composable
fun MainScreen(viewModel: FlightViewModel, onShowOnboardingAgain: () -> Unit = {}) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val cameraPositionState = rememberCameraPositionState()

    val mapFlights by viewModel.mapFlights.collectAsState()
    val planesAbove by viewModel.planesAbove.collectAsState()
    val hasPlaneAbove by remember { derivedStateOf { planesAbove.isNotEmpty() } }
    val isLoading by viewModel.isLoading.collectAsState()
    val cameraPosition by viewModel.mapCameraPosition.collectAsState()
    val selectedFlight by viewModel.selectedFlight.collectAsState()
    val showingDetail by viewModel.showingDetail.collectAsState()
    val fovMiles by viewModel.fovRadiusMiles.collectAsState()
    val city by viewModel.city.collectAsState()
    val heading by viewModel.heading.collectAsState()
    val location by viewModel.location.collectAsState()

    var showSettings by remember { mutableStateOf(false) }

    LaunchedEffect(cameraPosition) {
        cameraPositionState.animate(
            CameraUpdateFactory.newCameraPosition(cameraPosition),
            500
        )
    }

    Box(modifier = Modifier.fillMaxSize()) {
        // Map
        GoogleMap(
            modifier = Modifier.fillMaxSize(),
            cameraPositionState = cameraPositionState,
            properties = MapProperties(
                isMyLocationEnabled = true,
                mapType = MapType.NORMAL
            ),
            uiSettings = MapUiSettings(
                zoomControlsEnabled = false,
                myLocationButtonEnabled = false,
                compassEnabled = true
            )
        ) {
            // FOV Circle
            location?.let { loc ->
                Circle(
                    center = LatLng(loc.latitude, loc.longitude),
                    radius = fovMiles * 1609.34,
                    fillColor = Color.Blue.copy(alpha = 0.08f),
                    strokeColor = Color.Blue.copy(alpha = 0.35f),
                    strokeWidth = 1.5f
                )
            }

            // Flight markers
            mapFlights.forEach { flight ->
                val isAbove = planesAbove.any { it.id == flight.id }
                val displayHeading = viewModel.displayHeading(flight)
                Marker(
                    state = rememberMarkerState(
                        position = LatLng(flight.latitude, flight.longitude)
                    ),
                    title = flight.formattedCallsign,
                    icon = BitmapDescriptorFactory.defaultMarker(
                        if (isAbove) BitmapDescriptorFactory.HUE_RED else BitmapDescriptorFactory.HUE_BLUE
                    ),
                    rotation = displayHeading.toFloat(),
                    onClick = {
                        viewModel.focusAndSelectFlight(flight)
                        true
                    }
                )
            }
        }

        // Top overlay
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 32.dp)
                .align(Alignment.TopCenter)
        ) {
            // Header
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            "Plane Above You",
                            fontSize = 20.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isLoading) Color(0xFFFFA500) else Color(0xFF22C55E))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                if (isLoading) "Scanning skies..." else "${mapFlights.size} planes in your area",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    IconButton(onClick = { showSettings = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    IconButton(onClick = { viewModel.focusOnCountry() }) {
                        Icon(Icons.Default.Map, contentDescription = "Country view")
                    }
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh",
                            modifier = Modifier.rotate(if (isLoading) 360f else 0f)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Overhead banner
            AnimatedVisibility(
                visible = hasPlaneAbove,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut()
            ) {
                OverheadBanner(
                    planes = planesAbove,
                    onTapPlane = { viewModel.focusAndSelectFlight(it) }
                )
            }
        }

        // Bottom bar
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 12.dp)
                .align(Alignment.BottomCenter),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.92f)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (hasPlaneAbove) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.AirplanemodeActive,
                            contentDescription = null,
                            tint = Color.Red,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "${planesAbove.size} in FOV",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.Red
                        )
                    }
                } else {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.VisibilityOff,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            "No planes in your FOV",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.weight(1f))

                if (city.isNotEmpty()) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.LocationOn,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(city, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                }

                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(14.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        "FOV ${String.format("%.1f", fovMiles)} mi",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.primary.copy(alpha = 0.7f)
                    )
                }

                heading?.let { h ->
                    Spacer(modifier = Modifier.width(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            Icons.Default.KeyboardArrowUp,
                            contentDescription = null,
                            modifier = Modifier.size(14.dp),
                            tint = Color(0xFF66B2FF)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("${h.toInt()}°", fontSize = 12.sp, color = Color(0xFF66B2FF))
                    }
                }
            }
        }
    }

    // Detail sheet
    AnimatedVisibility(
        visible = showingDetail,
        enter = slideInVertically { it },
        exit = slideOutVertically { it }
    ) {
        selectedFlight?.let { flight ->
            FlightDetailSheet(
                flight = flight,
                userLocation = location,
                onDismiss = { viewModel.dismissDetail() }
            )
        }
    }

        // Settings sheet
    AnimatedVisibility(
        visible = showSettings,
        enter = fadeIn(),
        exit = fadeOut()
    ) {
        SettingsScreen(
            currentFOV = fovMiles,
            onFOVChanged = { viewModel.setFOVMiles(it) },
            onShowOnboardingAgain = onShowOnboardingAgain,
            onDismiss = { showSettings = false }
        )
    }
}
