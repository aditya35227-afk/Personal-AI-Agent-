package com.example.ui.agent

import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Book
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.FlashlightOn
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.PhoneAndroid
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Quiz
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SmartToy
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.domain.model.ExecutionState
import com.example.ui.components.ActionConfirmationDialog
import com.example.ui.components.PermissionPromptDialog
import com.example.ui.components.RobotAvatar

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AgentScreen(
    viewModel: AgentViewModel,
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val agentState by viewModel.agentState.collectAsStateWithLifecycle()
    val currentQuery by viewModel.currentQuery.collectAsStateWithLifecycle()
    val lastResult by viewModel.lastResult.collectAsStateWithLifecycle()
    val notes by viewModel.notes.collectAsStateWithLifecycle()
    val routines by viewModel.routines.collectAsStateWithLifecycle()
    val context = LocalContext.current

    val micPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (granted) {
            viewModel.dismissPermissionPrompt()
            viewModel.startListening()
        } else {
            Toast.makeText(context, "Microphone permission is needed for voice actions", Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.SmartToy,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                        Spacer(modifier = Modifier.width(10.dp))
                        Column {
                            Text(
                                text = "Astra AI Voice Assistant",
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Local First • Fast & Private",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.startListening() },
                        modifier = Modifier.testTag("top_bar_mic_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Mic,
                            contentDescription = "Voice Assistant",
                            tint = if (uiState.isListening) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { viewModel.startListening() },
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
                modifier = Modifier.testTag("fab_voice_assistant")
            ) {
                Icon(
                    imageVector = Icons.Default.Mic,
                    contentDescription = "Voice Command",
                    modifier = Modifier.size(28.dp)
                )
            }
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            // Central Robot Avatar (Smoothly appears on activation, minimizes when done)
            AnimatedVisibility(
                visible = uiState.isAvatarVisible,
                enter = fadeIn() + expandVertically(),
                exit = fadeOut() + shrinkVertically()
            ) {
                RobotAvatar(
                    state = agentState,
                    currentQuery = currentQuery,
                    statusMessage = lastResult?.message ?: "",
                    onDismiss = { viewModel.dismissAvatar() },
                    onActivateMic = { viewModel.startListening() },
                    modifier = Modifier.fillMaxWidth()
                )
            }

            // Quick Assistant Activation Banner when minimized
            if (!uiState.isAvatarVisible) {
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    shape = RoundedCornerShape(18.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                        .clickable { viewModel.startListening() }
                        .testTag("activate_agent_banner")
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .clip(CircleShape)
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Mic,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                            }
                            Spacer(modifier = Modifier.width(12.dp))
                            Column {
                                Text(
                                    text = "Tap to speak with Astra",
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.SemiBold
                                )
                                Text(
                                    text = "Try: \"Torch on\", \"Set alarm\", \"Study timer\"",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.PlayArrow,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Category Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                AgentSubCategory.values().forEach { cat ->
                    FilterChip(
                        selected = uiState.selectedSubCategory == cat,
                        onClick = { viewModel.selectCategory(cat) },
                        label = { Text(cat.title) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primary,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimary
                        ),
                        modifier = Modifier.testTag("cat_chip_${cat.name.lowercase()}")
                    )
                }
            }

            // Section views
            when (uiState.selectedSubCategory) {
                AgentSubCategory.OVERVIEW -> {
                    AgentOverviewSection(
                        onRunCommand = { viewModel.runCommand(it) }
                    )
                }
                AgentSubCategory.STUDY_AGENT -> {
                    StudyAgentView(
                        isTimerActive = uiState.isTimerActive,
                        secondsRemaining = uiState.activeTimerSecondsRemaining,
                        notes = notes,
                        onStartStudyTimer = { viewModel.startStudyTimer(it) },
                        onStopTimer = { viewModel.stopStudyTimer() },
                        onSaveNote = { title, content, tag -> viewModel.saveQuickNote(title, content, tag) },
                        onDeleteNote = { viewModel.deleteNote(it) },
                        onRunCommand = { viewModel.runCommand(it) }
                    )
                }
                AgentSubCategory.PHONE_AGENT -> {
                    PhoneAgentView(
                        onRunCommand = { viewModel.runCommand(it) }
                    )
                }
                AgentSubCategory.SOCIAL_MEDIA -> {
                    SocialAssistantView(
                        onRunCommand = { viewModel.runCommand(it) }
                    )
                }
                AgentSubCategory.AUTOMATIONS -> {
                    AutomationsView(
                        routines = routines,
                        onAddRoutine = { t, p, a, pm -> viewModel.addRoutine(t, p, a, pm) },
                        onDeleteRoutine = { viewModel.deleteRoutine(it) },
                        onToggleRoutine = { id, enabled -> viewModel.toggleRoutine(id, enabled) },
                        onRunCommand = { viewModel.runCommand(it) }
                    )
                }
            }
        }
    }

    // Confirmation dialog for sensitive operations
    if (uiState.pendingConfirmationIntent != null) {
        ActionConfirmationDialog(
            intent = uiState.pendingConfirmationIntent!!,
            onConfirm = { viewModel.confirmPendingAction() },
            onDismiss = { viewModel.cancelPendingAction() }
        )
    }

    // Permission prompt dialog
    if (uiState.requiredPermissionToPrompt != null) {
        PermissionPromptDialog(
            permission = uiState.requiredPermissionToPrompt!!,
            onRequestPermission = {
                uiState.requiredPermissionToPrompt?.androidPermission?.let {
                    micPermissionLauncher.launch(it)
                }
            },
            onDismiss = { viewModel.dismissPermissionPrompt() }
        )
    }
}

@Composable
fun AgentOverviewSection(
    onRunCommand: (String) -> Unit
) {
    val sampleCommands = listOf(
        "torch on" to "Toggle Flashlight",
        "set alarm for 7 AM" to "Alarms & Wakeup",
        "open YouTube" to "Launch Apps",
        "start a 40 minute study timer" to "Study Pomodoro",
        "explain photosynthesis" to "Doubt Solving",
        "create an Instagram caption" to "Social Posts",
        "call Rahul" to "Sensitive Call (Confirmed)",
        "send Rahul a message saying I will be late" to "Sensitive SMS (Confirmed)"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Voice Command Test Bench",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Text(
                text = "Tap any command below to test the local router, Gemini intent engine, and permission confirmation flow:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(sampleCommands) { (command, label) ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRunCommand(command) }
                    .testTag("test_cmd_${command.take(12)}")
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "\"$command\"",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    Icon(
                        imageVector = Icons.Default.PlayArrow,
                        contentDescription = "Run",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun StudyAgentView(
    isTimerActive: Boolean,
    secondsRemaining: Int,
    notes: List<com.example.data.local.entity.NoteEntity>,
    onStartStudyTimer: (Int) -> Unit,
    onStopTimer: () -> Unit,
    onSaveNote: (String, String, String) -> Unit,
    onDeleteNote: (Long) -> Unit,
    onRunCommand: (String) -> Unit
) {
    var newNoteTitle by remember { mutableStateOf("") }
    var newNoteContent by remember { mutableStateOf("") }
    var showNoteInput by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // Study Timer Card
        item {
            Card(
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
                shape = RoundedCornerShape(20.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Focus & Study Timer",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Icon(
                            imageVector = Icons.Default.HourglassBottom,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.height(10.dp))

                    if (isTimerActive) {
                        val minutes = secondsRemaining / 60
                        val seconds = secondsRemaining % 60
                        Text(
                            text = "%02d:%02d".format(minutes, seconds),
                            style = MaterialTheme.typography.displayMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = onStopTimer,
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                        ) {
                            Text("Stop Timer")
                        }
                    } else {
                        Text(
                            text = "Select a study session duration:",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            listOf(15, 25, 45, 60).forEach { mins ->
                                Button(
                                    onClick = { onStartStudyTimer(mins) },
                                    modifier = Modifier.weight(1f).testTag("timer_btn_${mins}m")
                                ) {
                                    Text("${mins}m")
                                }
                            }
                        }
                    }
                }
            }
        }

        // Quick Academic Actions
        item {
            Text("Academic Tools", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.height(6.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onRunCommand("create a 3-question Science Quiz") }
                        .padding(vertical = 4.dp)
                        .testTag("tool_quiz")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Icon(imageVector = Icons.Default.Quiz, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Create Quiz", fontWeight = FontWeight.SemiBold)
                        Text("Practice MCQs", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier
                        .weight(1f)
                        .clickable { onRunCommand("explain photosynthesis step by step") }
                        .padding(vertical = 4.dp)
                        .testTag("tool_doubt")
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Icon(imageVector = Icons.Default.AutoAwesome, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Doubt Solver", fontWeight = FontWeight.SemiBold)
                        Text("Step-by-step help", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
        }

        // Quick Notes
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Study Notes & Formulas", style = MaterialTheme.typography.titleSmall, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                Text(
                    text = if (showNoteInput) "Cancel" else "+ Add Note",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.clickable { showNoteInput = !showNoteInput }
                )
            }
        }

        if (showNoteInput) {
            item {
                Surface(
                    shape = RoundedCornerShape(14.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        OutlinedTextField(
                            value = newNoteTitle,
                            onValueChange = { newNoteTitle = it },
                            label = { Text("Note Title / Concept") },
                            modifier = Modifier.fillMaxWidth().testTag("note_title_input")
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        OutlinedTextField(
                            value = newNoteContent,
                            onValueChange = { newNoteContent = it },
                            label = { Text("Summary or Formula") },
                            modifier = Modifier.fillMaxWidth().testTag("note_content_input")
                        )
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            onClick = {
                                if (newNoteTitle.isNotBlank()) {
                                    onSaveNote(newNoteTitle, newNoteContent, "STUDY")
                                    newNoteTitle = ""
                                    newNoteContent = ""
                                    showNoteInput = false
                                }
                            },
                            modifier = Modifier.fillMaxWidth().testTag("save_note_button")
                        ) {
                            Text("Save Study Note")
                        }
                    }
                }
            }
        }

        if (notes.isEmpty() && !showNoteInput) {
            item {
                Text(
                    text = "No saved study notes. Ask Astra or tap '+ Add Note'.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(notes) { note ->
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.surfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(note.title, fontWeight = FontWeight.SemiBold)
                            if (note.content.isNotBlank()) {
                                Text(note.content, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                        IconButton(onClick = { onDeleteNote(note.id) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete note", tint = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
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
fun PhoneAgentView(
    onRunCommand: (String) -> Unit
) {
    val phoneActions = listOf(
        Triple("Toggle Torch", "torch on", Icons.Default.FlashlightOn),
        Triple("Set 7 AM Alarm", "set alarm for 7 AM", Icons.Default.Alarm),
        Triple("10 Min Timer", "start a 10 minute timer", Icons.Default.Timer),
        Triple("Volume Up", "volume up", Icons.Default.VolumeUp),
        Triple("Open YouTube", "open YouTube", Icons.Default.PlayArrow),
        Triple("Open Settings", "open settings", Icons.Default.PhoneAndroid)
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                text = "Phone & System Controls",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Local device controls are handled immediately on-device without internet latency.",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(phoneActions) { (title, cmd, icon) ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRunCommand(cmd) }
                    .testTag("phone_action_${title.take(8)}")
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primaryContainer),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(imageVector = icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(20.dp))
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(title, fontWeight = FontWeight.SemiBold)
                            Text("\"$cmd\"", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                    Button(onClick = { onRunCommand(cmd) }) {
                        Text("Run")
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
fun SocialAssistantView(
    onRunCommand: (String) -> Unit
) {
    val prompts = listOf(
        "create an Instagram study caption for late night coding",
        "generate 5 trending hashtags for engineering student project",
        "draft a LinkedIn update about completing my AI coursework",
        "generate ideas for a study reel about exam preparation"
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                text = "Student Social Media & Content Creator",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(
                text = "Generate polished project updates, captions, and hashtag bundles for your student achievements:",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }

        items(prompts) { p ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRunCommand(p) }
            ) {
                Row(
                    modifier = Modifier.padding(16.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(p, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Medium)
                    }
                    Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }
}

@Composable
fun AutomationsView(
    routines: List<com.example.data.local.entity.RoutineEntity>,
    onAddRoutine: (String, String, String, String) -> Unit,
    onDeleteRoutine: (Long) -> Unit,
    onToggleRoutine: (Long, Boolean) -> Unit,
    onRunCommand: (String) -> Unit
) {
    var showAddDialog by remember { mutableStateOf(false) }
    var routineTitle by remember { mutableStateOf("") }
    var routinePhrase by remember { mutableStateOf("") }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Automations & Voice Routines", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                    Text("Execute multiple actions with a single shortcut phrase", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }

        item {
            Button(
                onClick = { showAddDialog = true },
                modifier = Modifier.fillMaxWidth().testTag("add_routine_button")
            ) {
                Text("+ New Custom Routine")
            }
        }

        // Built-in presets
        item {
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onRunCommand("start a 45 minute study timer") }
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("Routine: \"Study Mode\"", fontWeight = FontWeight.Bold)
                        Text("Trigger: \"Study time\" • Starts 45m timer & focus", style = MaterialTheme.typography.bodySmall)
                    }
                    Button(onClick = { onRunCommand("start a 45 minute study timer") }) {
                        Text("Trigger")
                    }
                }
            }
        }

        items(routines) { r ->
            Surface(
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(r.title, fontWeight = FontWeight.Bold)
                        Text("Trigger: \"${r.triggerPhrase}\"", style = MaterialTheme.typography.bodySmall)
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Switch(
                            checked = r.isEnabled,
                            onCheckedChange = { onToggleRoutine(r.id, it) }
                        )
                        IconButton(onClick = { onDeleteRoutine(r.id) }) {
                            Icon(imageVector = Icons.Default.Delete, contentDescription = "Delete")
                        }
                    }
                }
            }
        }

        item {
            Spacer(modifier = Modifier.height(30.dp))
        }
    }

    if (showAddDialog) {
        AlertDialog(
            onDismissRequest = { showAddDialog = false },
            title = { Text("Add Custom Routine") },
            text = {
                Column {
                    OutlinedTextField(
                        value = routineTitle,
                        onValueChange = { routineTitle = it },
                        label = { Text("Routine Name (e.g. Night Study)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = routinePhrase,
                        onValueChange = { routinePhrase = it },
                        label = { Text("Trigger Voice Phrase (e.g. night owl)") },
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (routineTitle.isNotBlank()) {
                            onAddRoutine(routineTitle, routinePhrase, "CUSTOM", "")
                            routineTitle = ""
                            routinePhrase = ""
                            showAddDialog = false
                        }
                    }
                ) {
                    Text("Save Routine")
                }
            },
            dismissButton = {
                OutlinedButton(onClick = { showAddDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}
