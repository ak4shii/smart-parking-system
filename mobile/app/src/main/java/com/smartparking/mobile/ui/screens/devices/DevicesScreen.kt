package com.smartparking.mobile.ui.screens.devices

import android.content.Context
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartparking.mobile.data.model.Microcontroller
import com.smartparking.mobile.data.model.MqttCredentials
import com.smartparking.mobile.data.model.Sensor
import com.smartparking.mobile.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import java.util.concurrent.TimeUnit

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DevicesScreen(
    onNavigateBack: () -> Unit,
    viewModel: DevicesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()
    var showParkingSpaceMenu by remember { mutableStateOf(false) }
    var selectedTab by remember { mutableStateOf(0) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "IOT MANAGEMENT",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Devices & Sensors",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.refresh() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Parking Space Selector
            if (uiState.parkingSpaces.isNotEmpty()) {
                ExposedDropdownMenuBox(
                    expanded = showParkingSpaceMenu,
                    onExpandedChange = { showParkingSpaceMenu = it }
                ) {
                    OutlinedTextField(
                        value = uiState.selectedParkingSpace?.let { "${it.name} - ${it.location}" } ?: "Select Parking",
                        onValueChange = {},
                        readOnly = true,
                        label = { Text("Parking Space") },
                        trailingIcon = {
                            ExposedDropdownMenuDefaults.TrailingIcon(expanded = showParkingSpaceMenu)
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .menuAnchor(),
                        shape = RoundedCornerShape(12.dp)
                    )
                    ExposedDropdownMenu(
                        expanded = showParkingSpaceMenu,
                        onDismissRequest = { showParkingSpaceMenu = false }
                    ) {
                        uiState.parkingSpaces.forEach { space ->
                            DropdownMenuItem(
                                text = { Text("${space.name} - ${space.location}") },
                                onClick = {
                                    viewModel.selectParkingSpace(space.id)
                                    showParkingSpaceMenu = false
                                }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Stats Cards
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Total",
                    value = uiState.totalSensors.toString(),
                    icon = Icons.Default.Sensors,
                    containerColor = Indigo50,
                    contentColor = Indigo700
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Active",
                    value = uiState.activeSensors.toString(),
                    icon = Icons.Default.CheckCircle,
                    containerColor = Emerald50,
                    contentColor = Emerald700
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Ultrasonic",
                    value = uiState.ultrasonicCount.toString(),
                    icon = Icons.Default.Sensors,
                    containerColor = Blue50,
                    contentColor = Blue700
                )
                StatCard(
                    modifier = Modifier.weight(1f),
                    title = "Infrared",
                    value = uiState.infraredCount.toString(),
                    icon = Icons.Default.Sensors,
                    containerColor = Purple50,
                    contentColor = Purple700
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tab Row
            TabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = { Text("Sensors") }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = { Text("Controllers") }
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null -> {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = uiState.error ?: "",
                            modifier = Modifier.padding(16.dp),
                            color = MaterialTheme.colorScheme.onErrorContainer
                        )
                    }
                }
                else -> {
                    when (selectedTab) {
                        0 -> SensorsList(
                            sensors = uiState.filteredSensors,
                            searchQuery = uiState.searchQuery,
                            onSearchQueryChange = viewModel::updateSearchQuery,
                            getSlotLabel = viewModel::getSlotLabel,
                            getMcName = viewModel::getMicrocontrollerName,
                            isMcOnline = viewModel::isMicrocontrollerOnline
                        )
                        1 -> MicrocontrollersList(
                            microcontrollers = uiState.filteredMicrocontrollers,
                            isRegenerating = uiState.isRegenerating,
                            regeneratingMcId = uiState.regeneratingMcId,
                            onRegenerateKey = viewModel::regenerateMqttCredentials
                        )
                    }
                }
            }
        }
    }

    // MQTT Credentials Dialog
    if (uiState.showMqttCredentials && uiState.mqttCredentials != null) {
        MqttCredentialsDialogDevices(
            credentials = uiState.mqttCredentials!!,
            onDismiss = { viewModel.hideMqttCredentials() }
        )
    }
}

@Composable
private fun StatCard(
    modifier: Modifier = Modifier,
    title: String,
    value: String,
    icon: ImageVector,
    containerColor: androidx.compose.ui.graphics.Color,
    contentColor: androidx.compose.ui.graphics.Color
) {
    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = value,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = contentColor
            )
            Text(
                text = title,
                style = MaterialTheme.typography.labelSmall,
                color = contentColor.copy(alpha = 0.8f)
            )
        }
    }
}

@Composable
private fun SensorsList(
    sensors: List<Sensor>,
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    getSlotLabel: (Long) -> String,
    getMcName: (Long) -> String,
    isMcOnline: (Long) -> Boolean
) {
    Column {
        // Search Field
        OutlinedTextField(
            value = searchQuery,
            onValueChange = onSearchQueryChange,
            label = { Text("Search sensors...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(12.dp))

        if (sensors.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.Sensors,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = if (searchQuery.isBlank()) "No sensors" else "No sensors found",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(sensors) { sensor ->
                    SensorItem(
                        sensor = sensor,
                        slotLabel = getSlotLabel(sensor.slotId),
                        mcName = getMcName(sensor.microcontrollerId),
                        isOnline = isMcOnline(sensor.microcontrollerId)
                    )
                }
            }
        }
    }
}

@Composable
private fun SensorItem(
    sensor: Sensor,
    slotLabel: String,
    mcName: String,
    isOnline: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Sensor Icon
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = if (sensor.type == "ultrasonic") Blue50 else Purple50
                ) {
                    Icon(
                        Icons.Default.Sensors,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp),
                        tint = if (sensor.type == "ultrasonic") Blue700 else Purple700
                    )
                }

                Column {
                    Text(
                        text = sensor.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "$slotLabel • $mcName",
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Type Badge
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (sensor.type == "ultrasonic") Blue100 else Purple100
                ) {
                    Text(
                        text = sensor.type,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (sensor.type == "ultrasonic") Blue700 else Purple700
                    )
                }

                // Online Status
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (isOnline) Emerald100 else Slate100
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (isOnline) Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (isOnline) Emerald700 else Slate600
                        )
                        Text(
                            text = if (isOnline) "Online" else "Offline",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (isOnline) Emerald700 else Slate600
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun MicrocontrollersList(
    microcontrollers: List<Microcontroller>,
    isRegenerating: Boolean,
    regeneratingMcId: Long?,
    onRegenerateKey: (Long) -> Unit
) {
    if (microcontrollers.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Icon(
                    Icons.Default.DeveloperBoard,
                    contentDescription = null,
                    modifier = Modifier.size(48.dp),
                    tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "No controllers found",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            contentPadding = PaddingValues(vertical = 8.dp)
        ) {
            items(microcontrollers) { mc ->
                MicrocontrollerItem(
                    mc = mc,
                    isRegenerating = isRegenerating && regeneratingMcId == mc.id,
                    onRegenerateKey = { onRegenerateKey(mc.id) }
                )
            }
        }
    }
}

@Composable
private fun MicrocontrollerItem(
    mc: Microcontroller,
    isRegenerating: Boolean,
    onRegenerateKey: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // MC Icon
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Indigo50
                ) {
                    Icon(
                        Icons.Default.DeveloperBoard,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(20.dp),
                        tint = Indigo700
                    )
                }

                Column {
                    Text(
                        text = mc.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = mc.mcCode,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "•",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = formatUptime(mc.uptimeSec),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            // Actions
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Regenerate Key Button
                IconButton(
                    onClick = onRegenerateKey,
                    enabled = !isRegenerating,
                    modifier = Modifier.size(36.dp)
                ) {
                    if (isRegenerating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            Icons.Default.Key,
                            contentDescription = "Regenerate Key",
                            tint = Amber600,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Online Status
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = if (mc.online) Emerald100 else Slate100
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (mc.online) Icons.Default.Wifi else Icons.Default.WifiOff,
                            contentDescription = null,
                            modifier = Modifier.size(12.dp),
                            tint = if (mc.online) Emerald700 else Slate600
                        )
                        Text(
                            text = if (mc.online) "Online" else "Offline",
                            style = MaterialTheme.typography.labelSmall,
                            color = if (mc.online) Emerald700 else Slate600
                        )
                    }
                }
            }
        }
    }
}

private fun formatUptime(seconds: Long): String {
    if (seconds <= 0) return "—"
    val days = TimeUnit.SECONDS.toDays(seconds)
    val hours = TimeUnit.SECONDS.toHours(seconds) % 24
    val minutes = TimeUnit.SECONDS.toMinutes(seconds) % 60
    return when {
        days > 0 -> "${days}d ${hours}h"
        hours > 0 -> "${hours}h ${minutes}m"
        else -> "${minutes}m"
    }
}

@Composable
private fun MqttCredentialsDialogDevices(
    credentials: MqttCredentials,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    val clipboardManager = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
    var copiedField by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    fun copyToClipboard(value: String, field: String) {
        val clip = android.content.ClipData.newPlainText(field, value)
        clipboardManager.setPrimaryClip(clip)
        copiedField = field
        scope.launch {
            delay(2000)
            copiedField = null
        }
    }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "MQTT Credentials",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(onClick = onDismiss) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Amber50),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Warning,
                            contentDescription = null,
                            tint = Amber600,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Save these credentials! They won't be shown again.",
                            style = MaterialTheme.typography.bodySmall,
                            color = Amber700
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Card(
                    colors = CardDefaults.cardColors(containerColor = Slate50),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        CredentialRowDevices("Host", credentials.mqttHost, copiedField == "Host") {
                            copyToClipboard(credentials.mqttHost, "Host")
                        }
                        CredentialRowDevices("Port", credentials.mqttPort.toString(), copiedField == "Port") {
                            copyToClipboard(credentials.mqttPort.toString(), "Port")
                        }
                        CredentialRowDevices("Username", credentials.mqttUsername, copiedField == "Username") {
                            copyToClipboard(credentials.mqttUsername, "Username")
                        }
                        CredentialRowDevices("Password", credentials.mqttPassword, copiedField == "Password") {
                            copyToClipboard(credentials.mqttPassword, "Password")
                        }
                        CredentialRowDevices("Base Topic", credentials.baseTopic, copiedField == "Base Topic") {
                            copyToClipboard(credentials.baseTopic, "Base Topic")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))

                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Done")
                }
            }
        }
    }
}

@Composable
private fun CredentialRowDevices(
    label: String,
    value: String,
    isCopied: Boolean,
    onCopy: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                color = Slate500
            )
            Text(
                text = value,
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = FontWeight.Medium
            )
        }
        IconButton(
            onClick = onCopy,
            modifier = Modifier.size(32.dp)
        ) {
            Icon(
                if (isCopied) Icons.Default.Check else Icons.Default.ContentCopy,
                contentDescription = if (isCopied) "Copied" else "Copy",
                tint = if (isCopied) Emerald600 else Slate500,
                modifier = Modifier.size(18.dp)
            )
        }
    }
}
