package com.nathanjones.planeaboveyou.ui.screens

import android.location.Location
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.nathanjones.planeaboveyou.model.Flight

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FlightDetailSheet(flight: Flight, userLocation: Location?, onDismiss: () -> Unit) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = false)

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() }
    ) {
        LazyColumn(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp)
                .padding(bottom = 32.dp)
        ) {
            item {
                // Header
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(
                                Brush.linearGradient(
                                    listOf(Color(0xFF1976D2), Color(0xFF4FC3F7))
                                )
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.AirplanemodeActive,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier
                                .size(36.dp)
                                .rotate((flight.heading - 45).toFloat())
                        )
                    }
                    Spacer(modifier = Modifier.height(12.dp))
                    Text(
                        flight.formattedCallsign,
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        flight.airlineName,
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Chip(flight.formattedAircraftType, Color(0xFF1976D2))
                        if (flight.originAirport.isNotEmpty() || flight.destinationAirport.isNotEmpty()) {
                            Chip(flight.formattedRoute, Color(0xFF388E3C))
                        }
                    }
                }
            }

            item {
                // Mini map
                val cameraPositionState = rememberCameraPositionState {
                    position = CameraPosition.fromLatLngZoom(
                        LatLng(flight.latitude, flight.longitude), 10f
                    )
                }
                GoogleMap(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                        .clip(RoundedCornerShape(12.dp)),
                    cameraPositionState = cameraPositionState,
                    properties = MapProperties(mapType = MapType.NORMAL),
                    uiSettings = MapUiSettings(zoomControlsEnabled = false, scrollGesturesEnabled = false)
                ) {
                    Marker(
                        state = rememberMarkerState(position = LatLng(flight.latitude, flight.longitude)),
                        title = flight.formattedCallsign
                    )
                    userLocation?.let {
                        Marker(
                            state = rememberMarkerState(position = LatLng(it.latitude, it.longitude)),
                            title = "You",
                            icon = com.google.android.gms.maps.model.BitmapDescriptorFactory.defaultMarker(
                                com.google.android.gms.maps.model.BitmapDescriptorFactory.HUE_BLUE
                            )
                        )
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            item {
                InfoSection("Flight Info", listOf(
                    Triple("Flight", flight.formattedCallsign, Icons.Default.Tag),
                    Triple("Airline", flight.airlineName, Icons.Default.Business),
                    Triple("Airline ICAO", flight.airlineICAO.uppercase(), Icons.Default.Numbers)
                ).filter { (_, value, _) -> value.isNotEmpty() && value != "Unknown Airline" })
            }

            item {
                InfoSection("Aircraft", listOf(
                    Triple("Type", flight.formattedAircraftType, Icons.Default.AirplanemodeActive),
                    Triple("ICAO Type", flight.aircraftType.uppercase(), Icons.Default.Numbers),
                    Triple("Registration", flight.registration.uppercase(), Icons.Default.Search)
                ).filter { (_, value, _) -> value.isNotEmpty() && value != "Unknown" })
            }

            item {
                InfoSection("Route", listOf(
                    Triple("Route", flight.formattedRoute, Icons.Default.SwapHoriz),
                    Triple("Origin", flight.originAirport, Icons.Default.FlightTakeoff),
                    Triple("Destination", flight.destinationAirport, Icons.Default.FlightLand)
                ).filter { (_, value, _) -> value.isNotEmpty() })
            }

            item {
                InfoSection("Position", listOf(
                    Triple("Latitude", String.format("%.4f°", flight.latitude), Icons.Default.LocationOn),
                    Triple("Longitude", String.format("%.4f°", flight.longitude), Icons.Default.LocationOn),
                    Triple("Altitude", "${flight.altitudeFeet} ft", Icons.Default.TrendingUp)
                ))
            }

            item {
    val movementItems = mutableListOf(
        Triple("Ground Speed", "${flight.speedMph} mph", Icons.Default.Speed),
        Triple("Speed (knots)", "${flight.speedKnots} kts", Icons.Default.Timer),
        Triple("Heading", "${flight.heading.toInt()}°", Icons.Default.Explore)
    )
    if (flight.verticalRate != 0) {
        val icon = if (flight.verticalRate > 0) Icons.Default.TrendingUp else Icons.Default.TrendingDown
        movementItems.add(Triple("Vertical Rate", "${flight.verticalRate} fpm", icon))
    }
    InfoSection("Movement", movementItems)
            }

            if (userLocation != null) {
                item {
                    val distance = flight.distanceFrom(userLocation)
                    val isAbove = flight.isAbove(userLocation, 3.0)
                    InfoSection("Relative to You", listOf(
                        Triple("Distance", String.format("%.1f mi", distance), Icons.Default.Straighten),
                        Triple("Above You", if (isAbove) "Yes ✈️" else "No", if (isAbove) Icons.Default.CheckCircle else Icons.Default.Cancel)
                    ))
                }
            }
        }
    }
}

@Composable
private fun Chip(text: String, color: Color) {
    Text(
        text = text,
        fontSize = 12.sp,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.1f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 6.dp)
    )
}

@Composable
private fun InfoSection(title: String, items: List<Triple<String, String, androidx.compose.ui.graphics.vector.ImageVector>>) {
    Column(modifier = Modifier.padding(bottom = 16.dp)) {
        Text(
            text = title,
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(bottom = 8.dp)
        )
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(10.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column {
                items.forEachIndexed { index, item ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            item.third,
                            contentDescription = null,
                            modifier = Modifier.size(20.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            item.first,
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            item.second,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (index < items.size - 1) {
                        Divider(modifier = Modifier.padding(start = 48.dp))
                    }
                }
            }
        }
    }
}
