package com.smartparking.mobile.ui.screens.admin

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import com.smartparking.mobile.data.model.MqttCredentials
import com.smartparking.mobile.data.model.ParkingSpace
import com.smartparking.mobile.ui.theme.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AdminScreen(
    onNavigateBack: () -> Unit,
    viewModel: AdminViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsState()

    // Show snackbar for success messages
    LaunchedEffect(uiState.successMessage) {
        if (uiState.successMessage != null) {
            // Auto-clear after showing
            kotlinx.coroutines.delay(2000)
            viewModel.clearSuccessMessage()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "ADMINISTRATION",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Admin Panel",
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
                    IconButton(onClick = { viewModel.loadParkingSpaces() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                }
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.showCreateWizard() },
                containerColor = Indigo600
            ) {
                Icon(
                    Icons.Default.Add,
                    contentDescription = "Add Parking Space",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Stats Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(16.dp),
                colors = CardDefaults.cardColors(containerColor = Indigo50)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Total Parking Spaces",
                            style = MaterialTheme.typography.labelMedium,
                            color = Indigo700
                        )
                        Text(
                            text = "${uiState.parkingSpaces.size}",
                            style = MaterialTheme.typography.headlineLarge,
                            fontWeight = FontWeight.Bold,
                            color = Indigo700
                        )
                    }
                    Icon(
                        Icons.Default.Business,
                        contentDescription = null,
                        modifier = Modifier.size(48.dp),
                        tint = Indigo600
                    )
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Parking Spaces",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Content
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
                uiState.error != null && uiState.parkingSpaces.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Error,
                                contentDescription = null,
                                modifier = Modifier.size(48.dp),
                                tint = Rose500
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = uiState.error ?: "Error loading data",
                                color = Rose600
                            )
                        }
                    }
                }
                uiState.parkingSpaces.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Icon(
                                Icons.Default.Business,
                                contentDescription = null,
                                modifier = Modifier.size(64.dp),
                                tint = Slate300
                            )
                            Spacer(modifier = Modifier.height(16.dp))
                            Text(
                                text = "No Parking Spaces",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.SemiBold
                            )
                            Text(
                                text = "Tap + to create your first parking space",
                                style = MaterialTheme.typography.bodyMedium,
                                color = Slate500
                            )
                        }
                    }
                }
                else -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        items(uiState.parkingSpaces) { space ->
                            ParkingSpaceItem(
                                space = space,
                                onEdit = { viewModel.showEditDialog(space) },
                                onDelete = { viewModel.showDeleteConfirm(space) }
                            )
                        }
                    }
                }
            }

            // Success Message
            if (uiState.successMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Emerald100),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Emerald600,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = uiState.successMessage ?: "",
                            color = Emerald700,
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }

    // Create Wizard Dialog
    if (uiState.showCreateWizard) {
        CreateWizardDialog(
            uiState = uiState,
            onDismiss = { viewModel.hideCreateWizard() },
            onNameChange = viewModel::updateParkingSpaceName,
            onLocationChange = viewModel::updateParkingSpaceLocation,
            onMcCodeChange = viewModel::updateMicrocontrollerCode,
            onMcNameChange = viewModel::updateMicrocontrollerName,
            onNext = { viewModel.nextStep() },
            onBack = { viewModel.previousStep() },
            onComplete = { viewModel.completeWizard() }
        )
    }

    // Edit Dialog
    if (uiState.showEditDialog && uiState.editingSpace != null) {
        EditDialog(
            space = uiState.editingSpace!!,
            name = uiState.editName,
            location = uiState.editLocation,
            isSubmitting = uiState.isSubmitting,
            onDismiss = { viewModel.hideEditDialog() },
            onNameChange = viewModel::updateEditName,
            onLocationChange = viewModel::updateEditLocation,
            onSave = { viewModel.saveEdit() }
        )
    }

    // Delete Confirmation Dialog
    if (uiState.showDeleteConfirm && uiState.deletingSpace != null) {
        DeleteConfirmDialog(
            space = uiState.deletingSpace!!,
            isSubmitting = uiState.isSubmitting,
            onDismiss = { viewModel.hideDeleteConfirm() },
            onConfirm = { viewModel.confirmDelete() }
        )
    }

    // MQTT Credentials Dialog
    if (uiState.showMqttCredentials && uiState.mqttCredentials != null) {
        MqttCredentialsDialog(
            credentials = uiState.mqttCredentials!!,
            onDismiss = { viewModel.hideMqttCredentials() }
        )
    }
}

@Composable
private fun ParkingSpaceItem(
    space: ParkingSpace,
    onEdit: () -> Unit,
    onDelete: () -> Unit
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
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.weight(1f)
            ) {
                Surface(
                    shape = RoundedCornerShape(8.dp),
                    color = Indigo50
                ) {
                    Icon(
                        Icons.Default.Business,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(8.dp)
                            .size(24.dp),
                        tint = Indigo600
                    )
                }

                Column {
                    Text(
                        text = space.name,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = space.location,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (space.owner != null) {
                        Text(
                            text = "Owner: ${space.owner}",
                            style = MaterialTheme.typography.labelSmall,
                            color = Slate500
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                IconButton(
                    onClick = onEdit,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Edit,
                        contentDescription = "Edit",
                        tint = Indigo600,
                        modifier = Modifier.size(20.dp)
                    )
                }
                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(36.dp)
                ) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = Rose600,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun CreateWizardDialog(
    uiState: AdminUiState,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onMcCodeChange: (String) -> Unit,
    onMcNameChange: (String) -> Unit,
    onNext: () -> Boolean,
    onBack: () -> Unit,
    onComplete: () -> Unit
) {
    Dialog(onDismissRequest = { if (!uiState.isSubmitting) onDismiss() }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                // Header
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Create Parking Space",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    IconButton(
                        onClick = onDismiss,
                        enabled = !uiState.isSubmitting
                    ) {
                        Icon(Icons.Default.Close, contentDescription = "Close")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Step Indicator
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    for (i in 1..3) {
                        StepIndicator(
                            step = i,
                            currentStep = uiState.wizardStep,
                            label = when (i) {
                                1 -> "Details"
                                2 -> "Controller"
                                else -> "Review"
                            }
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Step Content
                when (uiState.wizardStep) {
                    1 -> Step1Content(
                        name = uiState.parkingSpaceName,
                        location = uiState.parkingSpaceLocation,
                        onNameChange = onNameChange,
                        onLocationChange = onLocationChange
                    )
                    2 -> Step2Content(
                        mcCode = uiState.microcontrollerCode,
                        mcName = uiState.microcontrollerName,
                        onMcCodeChange = onMcCodeChange,
                        onMcNameChange = onMcNameChange
                    )
                    3 -> Step3Content(
                        name = uiState.parkingSpaceName,
                        location = uiState.parkingSpaceLocation,
                        mcCode = uiState.microcontrollerCode,
                        mcName = uiState.microcontrollerName
                    )
                }

                // Error
                if (uiState.error != null) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = uiState.error,
                        color = Rose600,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (uiState.wizardStep > 1) {
                        OutlinedButton(
                            onClick = onBack,
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isSubmitting
                        ) {
                            Text("Back")
                        }
                    }

                    Button(
                        onClick = {
                            if (uiState.wizardStep < 3) {
                                onNext()
                            } else {
                                onComplete()
                            }
                        },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isSubmitting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.wizardStep == 3) Emerald600 else Indigo600
                        )
                    ) {
                        if (uiState.isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text(if (uiState.wizardStep < 3) "Next" else "Create")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun StepIndicator(step: Int, currentStep: Int, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            shape = RoundedCornerShape(50),
            color = if (step <= currentStep) Indigo600 else Slate200
        ) {
            Box(
                modifier = Modifier.size(32.dp),
                contentAlignment = Alignment.Center
            ) {
                if (step < currentStep) {
                    Icon(
                        Icons.Default.Check,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onPrimary,
                        modifier = Modifier.size(16.dp)
                    )
                } else {
                    Text(
                        text = step.toString(),
                        color = if (step <= currentStep) 
                            MaterialTheme.colorScheme.onPrimary 
                        else 
                            Slate600,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = if (step <= currentStep) Indigo600 else Slate500
        )
    }
}

@Composable
private fun Step1Content(
    name: String,
    location: String,
    onNameChange: (String) -> Unit,
    onLocationChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Step 1: Parking Space Details",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        OutlinedTextField(
            value = name,
            onValueChange = onNameChange,
            label = { Text("Name *") },
            placeholder = { Text("e.g., Downtown Parking") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = location,
            onValueChange = onLocationChange,
            label = { Text("Location *") },
            placeholder = { Text("e.g., 123 Main Street") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
    }
}

@Composable
private fun Step2Content(
    mcCode: String,
    mcName: String,
    onMcCodeChange: (String) -> Unit,
    onMcNameChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Step 2: Microcontroller Setup",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        OutlinedTextField(
            value = mcCode,
            onValueChange = onMcCodeChange,
            label = { Text("Microcontroller Code *") },
            placeholder = { Text("e.g., MC-001") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )

        OutlinedTextField(
            value = mcName,
            onValueChange = onMcNameChange,
            label = { Text("Controller Name *") },
            placeholder = { Text("e.g., Main Controller") },
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(12.dp),
            singleLine = true
        )
    }
}

@Composable
private fun Step3Content(
    name: String,
    location: String,
    mcCode: String,
    mcName: String
) {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        Text(
            text = "Step 3: Review",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Slate50),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Parking Space",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate500
                )
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = location,
                    style = MaterialTheme.typography.bodySmall,
                    color = Slate600
                )

                Spacer(modifier = Modifier.height(12.dp))
                Divider()
                Spacer(modifier = Modifier.height(12.dp))

                Text(
                    text = "Microcontroller",
                    style = MaterialTheme.typography.labelSmall,
                    color = Slate500
                )
                Text(
                    text = "$mcCode - $mcName",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

@Composable
private fun EditDialog(
    space: ParkingSpace,
    name: String,
    location: String,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onNameChange: (String) -> Unit,
    onLocationChange: (String) -> Unit,
    onSave: () -> Unit
) {
    Dialog(onDismissRequest = { if (!isSubmitting) onDismiss() }) {
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(modifier = Modifier.padding(24.dp)) {
                Text(
                    text = "Edit Parking Space",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )

                Spacer(modifier = Modifier.height(20.dp))

                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Name *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(12.dp))

                OutlinedTextField(
                    value = location,
                    onValueChange = onLocationChange,
                    label = { Text("Location *") },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp),
                    singleLine = true
                )

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        enabled = !isSubmitting
                    ) {
                        Text("Cancel")
                    }
                    Button(
                        onClick = onSave,
                        modifier = Modifier.weight(1f),
                        enabled = !isSubmitting
                    ) {
                        if (isSubmitting) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(20.dp),
                                color = MaterialTheme.colorScheme.onPrimary
                            )
                        } else {
                            Text("Save")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DeleteConfirmDialog(
    space: ParkingSpace,
    isSubmitting: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!isSubmitting) onDismiss() },
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = Rose600,
                modifier = Modifier.size(32.dp)
            )
        },
        title = { Text("Delete Parking Space?") },
        text = {
            Text(
                text = "Are you sure you want to delete \"${space.name}\"?\n\nThis action cannot be undone.",
                textAlign = TextAlign.Center
            )
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                enabled = !isSubmitting,
                colors = ButtonDefaults.buttonColors(containerColor = Rose600)
            ) {
                if (isSubmitting) {
                    CircularProgressIndicator(
                        modifier = Modifier.size(20.dp),
                        color = MaterialTheme.colorScheme.onPrimary
                    )
                } else {
                    Text("Delete")
                }
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                enabled = !isSubmitting
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
private fun MqttCredentialsDialog(
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
        // Reset after 2 seconds
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
                        CredentialRowWithCopy("Host", credentials.mqttHost, copiedField == "Host") {
                            copyToClipboard(credentials.mqttHost, "Host")
                        }
                        CredentialRowWithCopy("Port", credentials.mqttPort.toString(), copiedField == "Port") {
                            copyToClipboard(credentials.mqttPort.toString(), "Port")
                        }
                        CredentialRowWithCopy("Username", credentials.mqttUsername, copiedField == "Username") {
                            copyToClipboard(credentials.mqttUsername, "Username")
                        }
                        CredentialRowWithCopy("Password", credentials.mqttPassword, copiedField == "Password") {
                            copyToClipboard(credentials.mqttPassword, "Password")
                        }
                        CredentialRowWithCopy("Base Topic", credentials.baseTopic, copiedField == "Base Topic") {
                            copyToClipboard(credentials.baseTopic, "Base Topic")
                        }
                        CredentialRowWithCopy("MC Code", credentials.mcCode, copiedField == "MC Code") {
                            copyToClipboard(credentials.mcCode, "MC Code")
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
private fun CredentialRowWithCopy(
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
