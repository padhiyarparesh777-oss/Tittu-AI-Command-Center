package com.tittu.commandcenter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChatBubbleOutline
import androidx.compose.material.icons.filled.FolderOpen
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Terminal
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

private val Ink = Color(0xFF090812)
private val Panel = Color(0xFF171426)
private val Panel2 = Color(0xFF211B35)
private val Violet = Color(0xFF9B7BFF)
private val Lavender = Color(0xFFC5B4FF)
private val Muted = Color(0xFFAAA3BC)
private val Good = Color(0xFF63D6A4)

private val CommandColors = darkColorScheme(
    primary = Violet,
    onPrimary = Color.White,
    secondary = Lavender,
    background = Ink,
    onBackground = Color(0xFFF5F1FF),
    surface = Panel,
    onSurface = Color(0xFFF5F1FF),
    surfaceVariant = Panel2,
    onSurfaceVariant = Muted,
    outline = Color(0xFF51466B)
)

class MainActivity : ComponentActivity() {
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(9, 8, 18)
        window.navigationBarColor = android.graphics.Color.rgb(9, 8, 18)
        window.decorView.systemUiVisibility = 0
        setContent {
            MaterialTheme(colorScheme = CommandColors) {
                CommandCenterApp {
                    permissionLauncher.launch(TermuxBridge.RUN_COMMAND_PERMISSION)
                }
            }
        }
    }
}

private enum class AppTab(val title: String) {
    CHAT("Chat"), TERMINAL("Terminal"), PROJECTS("Projects"), SETTINGS("Settings")
}

private data class ChatLine(val text: String, val fromUser: Boolean)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommandCenterApp(onRequestTermuxPermission: () -> Unit) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(AppTab.CHAT) }
    var status by remember { mutableStateOf("Connecting…") }
    var prompt by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("") }
    var sessionId by remember { mutableStateOf("") }
    var useUbuntu by remember { mutableStateOf(true) }
    var pendingCommand by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    var chatLines by remember { mutableStateOf(listOf(ChatLine("Welcome back, Tittu. Your AI workspace is ready.", false))) }
    var output by remember { mutableStateOf("No command executed yet.") }
    val scope = rememberCoroutineScope()
    val client = remember { OpenCodeClient() }

    LaunchedEffect(Unit) {
        client.health().onSuccess { status = "OpenCode online" }
            .onFailure { status = "OpenCode offline" }
    }

    if (pendingCommand != null) {
        AlertDialog(
            onDismissRequest = { pendingCommand = null },
            title = { Text("Confirm terminal command") },
            text = { Text("Run this command in ${if (useUbuntu) "Ubuntu" else "Termux"}?\n\n$pendingCommand") },
            confirmButton = {
                TextButton(onClick = {
                    val cmd = pendingCommand ?: return@TextButton
                    pendingCommand = null
                    TermuxBridge.runShellCommand(context, cmd, useUbuntu)
                        .onSuccess { output = "Command dispatched. Check the Termux session for output." }
                        .onFailure { output = "Command failed: ${it.message}" }
                }) { Text("Run command") }
            },
            dismissButton = { TextButton(onClick = { pendingCommand = null }) { Text("Cancel") } }
        )
    }

    Scaffold(
        containerColor = Ink,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Ink),
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Box(Modifier.size(38.dp).background(Brush.linearGradient(listOf(Violet, Color(0xFF5740A0))), RoundedCornerShape(13.dp)), contentAlignment = Alignment.Center) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null, tint = Color.White)
                        }
                        Column {
                            Text("TITTU AI", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                            Text("COMMAND CENTER", style = MaterialTheme.typography.labelSmall, color = Muted)
                        }
                    }
                },
                actions = {
                    Surface(shape = CircleShape, color = Panel2, modifier = Modifier.padding(end = 14.dp)) {
                        Row(Modifier.padding(horizontal = 10.dp, vertical = 7.dp), verticalAlignment = Alignment.CenterVertically) {
                            Box(Modifier.size(7.dp).background(if (status.contains("online")) Good else Violet, CircleShape))
                            Spacer(Modifier.width(6.dp))
                            Text(status, style = MaterialTheme.typography.labelSmall, color = Muted)
                        }
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar(containerColor = Panel, contentColor = Muted) {
                AppTab.entries.forEach { tab ->
                    val icon = when (tab) {
                        AppTab.CHAT -> Icons.Default.ChatBubbleOutline
                        AppTab.TERMINAL -> Icons.Default.Terminal
                        AppTab.PROJECTS -> Icons.Default.FolderOpen
                        AppTab.SETTINGS -> Icons.Default.Settings
                    }
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = { Icon(icon, contentDescription = tab.title) },
                        label = { Text(tab.title) },
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = Lavender,
                            selectedTextColor = Lavender,
                            indicatorColor = Panel2,
                            unselectedIconColor = Muted,
                            unselectedTextColor = Muted
                        )
                    )
                }
            }
        }
    ) { padding ->
        Column(
            Modifier.fillMaxSize().padding(padding).background(Brush.verticalGradient(listOf(Ink, Color(0xFF100D1D))))
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            when (selectedTab) {
                AppTab.CHAT -> {
                    Text("Your AI workspace", style = MaterialTheme.typography.labelLarge, color = Lavender)
                    Text("What are we building today?", style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(12.dp))
                    if (sessionId.isBlank()) {
                        GlassPanel(Modifier.fillMaxWidth()) {
                            Text("Start a session to begin", color = Color.White, fontWeight = FontWeight.SemiBold)
                            Text("Connect to your local OpenCode assistant and work on your project.", color = Muted, style = MaterialTheme.typography.bodySmall)
                            Button(onClick = {
                                busy = true
                                scope.launch {
                                    client.createSession().onSuccess { raw ->
                                        sessionId = client.extractSessionId(raw)
                                        chatLines = chatLines + ChatLine(if (sessionId.isNotBlank()) "Session ready. Tell me what you want to build." else "Could not read the new session ID.", false)
                                    }.onFailure { chatLines = chatLines + ChatLine("Session error: ${it.message}", false) }
                                    busy = false
                                }
                            }, enabled = !busy) { Text(if (busy) "Starting…" else "Start new session") }
                        }
                    }
                    LazyColumn(
                        modifier = Modifier.weight(1f).fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(vertical = 8.dp)
                    ) {
                        items(chatLines) { line ->
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = if (line.fromUser) Arrangement.End else Arrangement.Start) {
                                Surface(
                                    color = if (line.fromUser) Color(0xFF493779) else Panel,
                                    shape = RoundedCornerShape(18.dp),
                                    modifier = Modifier.widthIn(max = 320.dp)
                                ) {
                                    Text(line.text, Modifier.padding(horizontal = 14.dp, vertical = 11.dp), color = Color(0xFFF5F1FF), style = MaterialTheme.typography.bodyMedium)
                                }
                            }
                        }
                        if (busy) item {
                            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                CircularProgressIndicator(Modifier.size(16.dp), strokeWidth = 2.dp, color = Lavender)
                                Text("OpenCode is working…", color = Muted, style = MaterialTheme.typography.labelMedium)
                            }
                        }
                    }
                    Surface(color = Panel, shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
                        Column(Modifier.padding(10.dp)) {
                            OutlinedTextField(
                                value = prompt,
                                onValueChange = { prompt = it },
                                modifier = Modifier.fillMaxWidth(),
                                placeholder = { Text("Message your AI assistant…", color = Muted) },
                                minLines = 2,
                                maxLines = 5,
                                shape = RoundedCornerShape(16.dp),
                                colors = OutlinedTextFieldDefaults.colors(
                                    focusedBorderColor = Violet,
                                    unfocusedBorderColor = Color.Transparent,
                                    focusedTextColor = Color.White,
                                    unfocusedTextColor = Color.White,
                                    cursorColor = Lavender
                                )
                            )
                            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
                                Text(if (sessionId.isBlank()) "Start a session first" else "OpenCode · Local", color = Muted, style = MaterialTheme.typography.labelSmall, modifier = Modifier.weight(1f))
                                FilledIconButton(
                                    onClick = {
                                        val message = prompt.trim()
                                        if (message.isNotEmpty() && sessionId.isNotBlank()) {
                                            prompt = ""
                                            chatLines = chatLines + ChatLine(message, true)
                                            busy = true
                                            scope.launch {
                                                client.sendPrompt(sessionId, message)
                                                    .onSuccess { answer ->
                                                        chatLines = chatLines + ChatLine(answer.ifBlank { "Task submitted." }, false)
                                                    }
                                                    .onFailure { chatLines = chatLines + ChatLine("Error: ${it.message}", false) }
                                                busy = false
                                            }
                                        }
                                    },
                                    enabled = !busy && prompt.isNotBlank() && sessionId.isNotBlank()
                                ) { Icon(Icons.Default.AutoAwesome, contentDescription = "Send prompt") }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                        TextButton(onClick = {
                            busy = true
                            scope.launch {
                                client.createSession().onSuccess { raw ->
                                    sessionId = client.extractSessionId(raw)
                                    chatLines = listOf(ChatLine("New session ready. What should we work on?", false))
                                }.onFailure { chatLines = chatLines + ChatLine("Session error: ${it.message}", false) }
                                busy = false
                            }
                        }, enabled = !busy) { Text("＋ New session", color = Lavender) }
                    }
                }
                AppTab.TERMINAL -> {
                    SectionHeading("Terminal", "Run commands in your Termux or Ubuntu environment.")
                    GlassPanel(Modifier.fillMaxWidth()) {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = !useUbuntu, onClick = { useUbuntu = false }, label = { Text("Termux") })
                            FilterChip(selected = useUbuntu, onClick = { useUbuntu = true }, label = { Text("Ubuntu") })
                        }
                        OutlinedTextField(value = command, onValueChange = { command = it }, modifier = Modifier.fillMaxWidth(), label = { Text("Command") }, minLines = 2)
                        Button(onClick = {
                            if (!TermuxBridge.isTermuxInstalled(context)) output = "Termux is not installed."
                            else if (!TermuxBridge.hasPermission(context)) onRequestTermuxPermission()
                            else if (CommandRiskPolicy.classify(command) == CommandRisk.SAFE_READ_ONLY) {
                                TermuxBridge.runShellCommand(context, command, useUbuntu)
                                    .onSuccess { output = "Command dispatched. Check Termux for output." }
                                    .onFailure { output = "Command failed: ${it.message}" }
                            } else pendingCommand = command
                        }, enabled = command.isNotBlank()) { Text("Review & run") }
                    }
                    GlassPanel(Modifier.fillMaxWidth()) { Text("Execution status", color = Lavender, fontWeight = FontWeight.SemiBold); Text(output, color = Muted) }
                }
                AppTab.PROJECTS -> {
                    SectionHeading("Projects", "Your development workspace")
                    GlassPanel(Modifier.fillMaxWidth()) {
                        Icon(Icons.Default.FolderOpen, contentDescription = null, tint = Lavender, modifier = Modifier.size(30.dp))
                        Text("Project browser", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                        Text("Native project picker and repository browser are not implemented yet. OpenCode currently works in the server's configured working directory.", color = Muted)
                    }
                    GlassPanel(Modifier.fillMaxWidth()) {
                        Text("Coming next", color = Lavender, fontWeight = FontWeight.SemiBold)
                        Text("Project selection · File explorer · Git status · Recent work", color = Muted)
                    }
                }
                AppTab.SETTINGS -> {
                    SectionHeading("Settings", "Connections and environment")
                    GlassPanel(Modifier.fillMaxWidth()) {
                        Text("OpenCode connection", fontWeight = FontWeight.SemiBold)
                        Text("http://127.0.0.1:4096", color = Muted)
                        Button(onClick = {
                            scope.launch {
                                status = "Checking…"
                                client.health().onSuccess { status = "OpenCode online" }
                                    .onFailure { status = "OpenCode offline" }
                            }
                        }) { Text("Check connection") }
                        OutlinedButton(onClick = onRequestTermuxPermission, modifier = Modifier.fillMaxWidth()) { Text("Grant Termux command permission") }
                        OutlinedButton(onClick = {
                            TermuxBridge.startOpenCode(context, useUbuntu)
                                .onSuccess { status = "Start requested" }
                                .onFailure { status = "Start failed: ${it.message}" }
                        }, modifier = Modifier.fillMaxWidth()) { Text("Start OpenCode server") }
                    }
                    GlassPanel(Modifier.fillMaxWidth()) {
                        Text("Environment", fontWeight = FontWeight.SemiBold)
                        Text(if (useUbuntu) "Ubuntu (proot-distro)" else "Termux", color = Muted)
                        Text("Theme: Dark glass · Accent: Lavender", color = Muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun SectionHeading(title: String, subtitle: String) {
    Text(title, style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold)
    Text(subtitle, color = Muted, style = MaterialTheme.typography.bodySmall)
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun GlassPanel(modifier: Modifier = Modifier, content: @Composable ColumnScope.() -> Unit) {
    Surface(modifier = modifier, color = Panel.copy(alpha = 0.94f), shape = RoundedCornerShape(22.dp), tonalElevation = 3.dp) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp), content = content)
    }
}
