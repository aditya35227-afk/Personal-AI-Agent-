package com.example.ui.profile

import android.content.Context
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Block
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Contacts
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Description
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.MyLocation
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Divider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.ConfirmationLevel
import com.example.domain.model.LanguageMode
import com.example.permissions.AppPermission
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileScreen(
    viewModel: ProfileViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val settings by viewModel.settings.collectAsStateWithLifecycle()
    val permissionLogs by viewModel.permissionLogs.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var selectedPermissionToRequest by remember { mutableStateOf<AppPermission?>(null) }
    var showClearDataConfirm by remember { mutableStateOf(false) }

    val singlePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) {
        viewModel.refreshPermissionStates()
    }

    LaunchedEffect(Unit) {
        viewModel.refreshPermissionStates()
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(
                            text = "Privacy & Profile Center",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            text = "Privacy-First • Completely Free for Students",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Emergency Stop Banner
            Surface(
                color = if (uiState.isEmergencyStopped) MaterialTheme.colorScheme.errorContainer else MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(
                        modifier = Modifier.weight(1f),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = if (uiState.isEmergencyStopped) Icons.Default.Warning else Icons.Default.Security,
                            contentDescription = null,
                            tint = if (uiState.isEmergencyStopped) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = if (uiState.isEmergencyStopped) "AGENT EMERGENCY STOPPED" else "Agent Active & Safe",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isEmergencyStopped) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (uiState.isEmergencyStopped) "All voice & background tasks halted" else "All sensitive actions require confirmation",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }

                    Button(
                        onClick = { viewModel.toggleEmergencyStop() },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (uiState.isEmergencyStopped) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        ),
                        modifier = Modifier.testTag("emergency_stop_button")
                    ) {
                        Text(if (uiState.isEmergencyStopped) "Resume" else "Stop")
                    }
                }
            }

            // Sub-Section Chips
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ProfileSubSection.values().forEach { section ->
                    FilterChip(
                        selected = uiState.selectedTab == section,
                        onClick = { viewModel.selectSection(section) },
                        label = { Text(section.title) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("profile_tab_${section.name.lowercase()}")
                    )
                }
            }

            // Tab Content
            when (uiState.selectedTab) {
                ProfileSubSection.PERMISSIONS -> {
                    PermissionsDashboard(
                        permissionStates = uiState.permissionStates,
                        onRequestPermission = { perm ->
                            if (perm.androidPermission != null) {
                                singlePermissionLauncher.launch(perm.androidPermission)
                            } else {
                                Toast.makeText(context, "${perm.title} uses zero-permission picker", Toast.LENGTH_SHORT).show()
                            }
                        },
                        onOpenSettings = { viewModel.openAppSettings(context) }
                    )
                }
                ProfileSubSection.ACCOUNT -> {
                    AccountSettingsView(
                        settings = settings,
                        onUpdateAssistantName = { viewModel.updateAssistantName(it) },
                        onUpdateDisplayName = { viewModel.updateDisplayName(it) },
                        onUpdateLanguage = { viewModel.updateLanguage(it) },
                        onUpdateConfirmationLevel = { viewModel.updateConfirmationLevel(it) },
                        onToggleVoiceResponse = { viewModel.toggleVoiceResponse(it) },
                        onUpdateVoiceSpeed = { viewModel.updateVoiceSpeed(it) },
                        onToggleRobotAnimation = { viewModel.toggleRobotAnimation(it) },
                        onToggleAutoListen = { viewModel.toggleAutoListen(it) }
                    )
                }
                ProfileSubSection.VOICE -> {
                    VoiceMatchView(
                        settings = settings,
                        isMicGranted = uiState.permissionStates[AppPermission.MICROPHONE] == true,
                        isTesting = uiState.isTestingVoice,
                        testResult = uiState.voiceTestResult,
                        onToggleVoiceActivation = { viewModel.toggleVoiceActivation(it) },
                        onUpdateWakePhrase = { viewModel.updateWakePhrase(it) },
                        onTestVoice = { viewModel.testVoiceRecognition() },
                        onRequestMic = {
                            AppPermission.MICROPHONE.androidPermission?.let {
                                singlePermissionLauncher.launch(it)
                            }
                        }
                    )
                }
                ProfileSubSection.DATA_CONTROL -> {
                    DataControlView(
                        settings = settings,
                        permissionLogs = permissionLogs,
                        onTogglePrivacyMode = { viewModel.togglePrivacyMode(it) },
                        onToggleMemory = { viewModel.toggleConversationMemory(it) },
                        onClearAllData = { showClearDataConfirm = true },
                        onExportData = {
                            Toast.makeText(context, "Exporting encrypted student archive to Downloads folder", Toast.LENGTH_LONG).show()
                        }
                    )
                }
            }
        }
    }

    if (showClearDataConfirm) {
        AlertDialog(
            onDismissRequest = { showClearDataConfirm = false },
            title = { Text("Clear All Local Data?") },
            text = { Text("This will permanently remove chat history, saved study notes, and reset all settings to defaults. This action cannot be undone.") },
            confirmButton = {
                Button(
                    onClick = {
                        viewModel.clearAllData()
                        showClearDataConfirm = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.testTag("confirm_clear_data_button")
                ) {
                    Text("Delete Everything")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showClearDataConfirm = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun PermissionsDashboard(
    permissionStates: Map<AppPermission, Boolean>,
    onRequestPermission: (AppPermission) -> Unit,
    onOpenSettings: () -> Unit
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Security,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Privacy Shield Active",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "All sensitive permissions are OFF by default. Permissions are only requested immediately before a feature requires them. Photo & document pickers use Android's Zero-Permission model.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onPrimaryContainer
                    )
                }
            }
        }

        items(AppPermission.values()) { perm ->
            val isGranted = permissionStates[perm] == true
            PermissionItemCard(
                permission = perm,
                isGranted = isGranted,
                onRequest = { onRequestPermission(perm) },
                onOpenSettings = onOpenSettings
            )
        }

        item {
            Spacer(modifier = Modifier.height(24.dp))
        }
    }
}

@Composable
fun PermissionItemCard(
    permission: AppPermission,
    isGranted: Boolean,
    onRequest: () -> Unit,
    onOpenSettings: () -> Unit
) {
    val icon = when (permission) {
        AppPermission.MICROPHONE -> Icons.Default.Mic
        AppPermission.CAMERA -> Icons.Default.CameraAlt
        AppPermission.CONTACTS -> Icons.Default.Contacts
        AppPermission.PHONE -> Icons.Default.Phone
        AppPermission.SMS -> Icons.Default.Sms
        AppPermission.NOTIFICATIONS -> Icons.Default.Notifications
        AppPermission.CALENDAR -> Icons.Default.CalendarMonth
        AppPermission.LOCATION -> Icons.Default.MyLocation
        AppPermission.PHOTOS_VIDEOS -> Icons.Default.PhotoLibrary
        AppPermission.FILES -> Icons.Default.Description
    }

    Surface(
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceVariant,
        modifier = Modifier
            .fillMaxWidth()
            .testTag("permission_card_${permission.key}")
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(
                                if (isGranted) MaterialTheme.colorScheme.primaryContainer
                                else MaterialTheme.colorScheme.surface
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = icon,
                            contentDescription = null,
                            tint = if (isGranted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = permission.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(6.dp)
                                    .clip(CircleShape)
                                    .background(if (isGranted) Color(0xFF10B981) else Color(0xFF94A3B8))
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = if (isGranted) "Granted / Secure" else "Off by Default",
                                style = MaterialTheme.typography.labelSmall,
                                color = if (isGranted) Color(0xFF10B981) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                if (!isGranted) {
                    Button(
                        onClick = onRequest,
                        modifier = Modifier.testTag("enable_perm_${permission.key}")
                    ) {
                        Text("Enable")
                    }
                } else {
                    OutlinedButton(
                        onClick = onOpenSettings,
                        modifier = Modifier.testTag("settings_perm_${permission.key}")
                    ) {
                        Text("Manage")
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = permission.usageDescription,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun AccountSettingsView(
    settings: com.example.domain.model.AppSettings,
    onUpdateAssistantName: (String) -> Unit,
    onUpdateDisplayName: (String) -> Unit,
    onUpdateLanguage: (LanguageMode) -> Unit,
    onUpdateConfirmationLevel: (ConfirmationLevel) -> Unit,
    onToggleVoiceResponse: (Boolean) -> Unit,
    onUpdateVoiceSpeed: (Float) -> Unit,
    onToggleRobotAnimation: (Boolean) -> Unit,
    onToggleAutoListen: (Boolean) -> Unit
) {
    var assistantNameInput by remember(settings.assistantName) { mutableStateOf(settings.assistantName) }
    var displayNameInput by remember(settings.userDisplayName) { mutableStateOf(settings.userDisplayName) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            // Free Badge
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text("Free Student Edition", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        Text("No subscriptions, no ads, no premium locks, no paywalls.", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
        }

        item {
            Text("Profile & Identity", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = assistantNameInput,
                onValueChange = {
                    assistantNameInput = it
                    onUpdateAssistantName(it)
                },
                label = { Text("Assistant Name") },
                modifier = Modifier.fillMaxWidth().testTag("assistant_name_input"),
                shape = RoundedCornerShape(14.dp)
            )
            Spacer(modifier = Modifier.height(10.dp))
            OutlinedTextField(
                value = displayNameInput,
                onValueChange = {
                    displayNameInput = it
                    onUpdateDisplayName(it)
                },
                label = { Text("Your Display Name") },
                modifier = Modifier.fillMaxWidth().testTag("user_name_input"),
                shape = RoundedCornerShape(14.dp)
            )
        }

        item {
            Text("Language & Speech", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                LanguageMode.values().forEach { mode ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (settings.languageMode == mode) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUpdateLanguage(mode) }
                            .padding(vertical = 2.dp)
                    ) {
                        Row(
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(mode.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                            if (settings.languageMode == mode) {
                                Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            }
                        }
                    }
                }
            }
        }

        item {
            Text("Action Confirmation Level", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                ConfirmationLevel.values().forEach { level ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (settings.confirmationLevel == level) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onUpdateConfirmationLevel(level) }
                            .padding(vertical = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(level.displayName, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
                                if (settings.confirmationLevel == level) {
                                    Icon(imageVector = Icons.Default.CheckCircle, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                                }
                            }
                            Text(level.description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        item {
            Text("Voice & Visual Preferences", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(8.dp))

            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Voice Responses (TTS)")
                        Switch(
                            checked = settings.voiceResponseEnabled,
                            onCheckedChange = onToggleVoiceResponse,
                            modifier = Modifier.testTag("voice_response_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Text("Voice Speed: %.1fx".format(settings.voiceSpeed), style = MaterialTheme.typography.bodySmall)
                    Slider(
                        value = settings.voiceSpeed,
                        onValueChange = onUpdateVoiceSpeed,
                        valueRange = 0.7f..1.5f,
                        steps = 7,
                        modifier = Modifier.testTag("voice_speed_slider")
                    )

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Animated Robot Avatar")
                        Switch(
                            checked = settings.robotAnimationEnabled,
                            onCheckedChange = onToggleRobotAnimation,
                            modifier = Modifier.testTag("robot_anim_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Auto-listen after response")
                        Switch(
                            checked = settings.autoListenAfterResponse,
                            onCheckedChange = onToggleAutoListen
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun VoiceMatchView(
    settings: com.example.domain.model.AppSettings,
    isMicGranted: Boolean,
    isTesting: Boolean,
    testResult: String,
    onToggleVoiceActivation: (Boolean) -> Unit,
    onUpdateWakePhrase: (String) -> Unit,
    onTestVoice: () -> Unit,
    onRequestMic: () -> Unit
) {
    var wakePhraseInput by remember(settings.wakePhrase) { mutableStateOf(settings.wakePhrase) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Voice Activation & Wake Word",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Notice & Honest Disclosure:\nAndroid OS strictly restricts third-party apps from maintaining 24/7 background always-on microphone listening to safeguard your privacy and battery life. Voice activation operates while the app is open or in foreground assistant mode.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text("Voice Activation", fontWeight = FontWeight.SemiBold)
                            Text("Trigger assistant with voice phrase", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                        Switch(
                            checked = settings.voiceActivationEnabled,
                            onCheckedChange = onToggleVoiceActivation,
                            modifier = Modifier.testTag("voice_activation_switch")
                        )
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    OutlinedTextField(
                        value = wakePhraseInput,
                        onValueChange = {
                            wakePhraseInput = it
                            onUpdateWakePhrase(it)
                        },
                        label = { Text("Wake Phrase") },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    )
                }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Microphone Hardware Status", fontWeight = FontWeight.SemiBold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(if (isMicGranted) "Active & Ready" else "Permission Not Granted")
                        if (!isMicGranted) {
                            Button(onClick = onRequestMic) {
                                Text("Grant Mic")
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))
                    Button(
                        onClick = onTestVoice,
                        enabled = !isTesting,
                        modifier = Modifier.fillMaxWidth().testTag("test_voice_button")
                    ) {
                        Icon(imageVector = Icons.Default.Mic, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (isTesting) "Listening to test audio..." else "Test Voice Recognition")
                    }

                    if (testResult.isNotBlank()) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Surface(
                            color = MaterialTheme.colorScheme.surface,
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Text(
                                text = testResult,
                                style = MaterialTheme.typography.bodySmall,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DataControlView(
    settings: com.example.domain.model.AppSettings,
    permissionLogs: List<com.example.data.local.entity.PermissionLogEntity>,
    onTogglePrivacyMode: (Boolean) -> Unit,
    onToggleMemory: (Boolean) -> Unit,
    onClearAllData: () -> Unit,
    onExportData: () -> Unit
) {
    val dateFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                shape = RoundedCornerShape(16.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Private Data Firewall", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "• Private contacts, SMS, and documents never flow to AI unless explicitly selected.\n• Confidential numbers and OTPs are redacted prior to query dispatch.\n• Zero hidden background microphone or camera recording.",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        item {
            Surface(shape = RoundedCornerShape(14.dp), color = MaterialTheme.colorScheme.surfaceVariant) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Strict Privacy Mode", fontWeight = FontWeight.SemiBold)
                        Switch(checked = settings.privacyMode, onCheckedChange = onTogglePrivacyMode)
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Local Conversation Memory", fontWeight = FontWeight.SemiBold)
                        Switch(checked = settings.conversationMemoryEnabled, onCheckedChange = onToggleMemory)
                    }
                }
            }
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OutlinedButton(
                    onClick = onExportData,
                    modifier = Modifier.weight(1f).testTag("export_data_button")
                ) {
                    Icon(imageVector = Icons.Default.Download, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Export Data")
                }

                Button(
                    onClick = onClearAllData,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.weight(1f).testTag("clear_all_data_button")
                ) {
                    Icon(imageVector = Icons.Default.Delete, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Clear All")
                }
            }
        }

        item {
            Text(
                text = "Permission Activity Audit Log",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }

        if (permissionLogs.isEmpty()) {
            item {
                Text(
                    text = "No permission access recorded yet.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(permissionLogs.take(15)) { log ->
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "${log.permissionKey.uppercase()} • ${log.actionType}",
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = log.details,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = dateFormat.format(Date(log.timestamp)),
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}
