package com.tittu.commandcenter

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    private val permissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme {
                CommandCenterApp(
                    onRequestTermuxPermission = {
                        permissionLauncher.launch(TermuxBridge.RUN_COMMAND_PERMISSION)
                    }
                )
            }
        }
    }
}

private enum class AppTab(val title: String) {
    CHAT("Chat"), TERMINAL("Terminal"), PROJECTS("Projects"), SETTINGS("Settings")
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CommandCenterApp(onRequestTermuxPermission: () -> Unit) {
    val context = LocalContext.current
    var selectedTab by remember { mutableStateOf(AppTab.CHAT) }
    var status by remember { mutableStateOf("Not connected") }
    var output by remember { mutableStateOf("Welcome to Tittu AI Command Center.") }
    var prompt by remember { mutableStateOf("") }
    var command by remember { mutableStateOf("") }
    var sessionId by remember { mutableStateOf("") }
    var useUbuntu by remember { mutableStateOf(true) }
    var pendingCommand by remember { mutableStateOf<String?>(null) }
    var busy by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val client = remember { OpenCodeClient() }

    if (pendingCommand != null) {
        AlertDialog(
            onDismissRequest = { pendingCommand = null },
            title = { Text("Confirm terminal command") },
            text = { Text("Run this command in ${if (useUbuntu) "Ubuntu" else "Termux"}?\n\n$pendingCommand") },
            confirmButton = {
                TextButton(onClick = {
                    val cmd = pendingCommand ?: return@TextButton
                    pendingCommand = null
                    val result = TermuxBridge.runShellCommand(context, cmd, useUbuntu)
                    result.onSuccess {
                        output = "Command dispatched to ${if (useUbuntu) "Ubuntu" else "Termux"}. Check the Termux session/notification for output."
                    }.onFailure {
                        output = "Command failed to start: ${it.message}"
                    }
                }) { Text("Run command") }
            },
            dismissButton = {
                TextButton(onClick = { pendingCommand = null }) { Text("Cancel") }
            }
        )
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text("TITTU AI COMMAND CENTER")
                        Text(status, style = MaterialTheme.typography.labelSmall)
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        icon = {},
                        label = { Text(tab.title) }
                    )
                }
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            when (selectedTab) {
                AppTab.CHAT -> {
                    Text("AI Chat", style = MaterialTheme.typography.headlineSmall)
                    Text("OpenCode local API · 127.0.0.1:4096")
                    Card(Modifier.weight(1f).fillMaxWidth()) {
                        Text(
                            output,
                            modifier = Modifier.padding(14.dp).verticalScroll(rememberScrollState())
                        )
                    }
                    OutlinedTextField(
                        value = prompt,
                        onValueChange = { prompt = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Ask OpenCode to work on your project") }
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(
                            enabled = !busy && prompt.isNotBlank() && sessionId.isNotBlank(),
                            onClick = {
                                val text = prompt
                                prompt = ""
                                busy = true
                                scope.launch {
                                    client.sendPrompt(sessionId, text)
                                        .onSuccess { output = it.ifBlank { "Prompt submitted." } }
                                        .onFailure { output = "Error: ${it.message}" }
                                    busy = false
                                }
                            }
                        ) { Text(if (busy) "Working…" else "Send prompt") }
                        Button(
                            enabled = !busy,
                            onClick = {
                                busy = true
                                scope.launch {
                                    client.createSession()
                                        .onSuccess { raw ->
                                            sessionId = Regex("\"id\"\\s*:\\s*\"([^\"]+)\"")
                                                .find(raw)?.groupValues?.get(1).orEmpty()
                                            output = if (sessionId.isNotBlank()) "New OpenCode session: $sessionId" else raw
                                        }
                                        .onFailure { output = "Session error: ${it.message}" }
                                    busy = false
                                }
                            }
                        ) { Text("New session") }
                    }
                }
                AppTab.TERMINAL -> {
                    Text("Termux Terminal Bridge", style = MaterialTheme.typography.headlineSmall)
                    Text("Commands are sent to your local Termux installation.")
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Button(onClick = { useUbuntu = false }) { Text(if (!useUbuntu) "✓ Termux" else "Termux") }
                        Button(onClick = { useUbuntu = true }) { Text(if (useUbuntu) "✓ Ubuntu" else "Ubuntu") }
                    }
                    OutlinedTextField(
                        value = command,
                        onValueChange = { command = it },
                        modifier = Modifier.fillMaxWidth(),
                        label = { Text("Command to run") }
                    )
                    Button(
                        onClick = {
                            if (!TermuxBridge.isTermuxInstalled(context)) {
                                output = "Termux is not installed."
                            } else if (!TermuxBridge.hasPermission(context)) {
                                onRequestTermuxPermission()
                            } else if (CommandRiskPolicy.classify(command) == CommandRisk.SAFE_READ_ONLY) {
                                val result = TermuxBridge.runShellCommand(context, command, useUbuntu)
                                result.onSuccess {
                                    output = "Read-only command dispatched. Check the Termux session/notification for output."
                                }.onFailure {
                                    output = "Command failed to start: ${it.message}"
                                }
                            } else {
                                pendingCommand = command
                            }
                        },
                        enabled = command.isNotBlank()
                    ) { Text("Review & run") }
                    Card(Modifier.fillMaxWidth()) {
                        Text(output, modifier = Modifier.padding(12.dp))
                    }
                }
                AppTab.PROJECTS -> {
                    Text("Projects", style = MaterialTheme.typography.headlineSmall)
                    Text("Project directory selection and Git integration are planned for the next milestone.")
                    Card(Modifier.fillMaxWidth()) {
                        Text("OpenCode sessions can work in the project directory selected by the server. A native project picker is not implemented yet.", modifier = Modifier.padding(12.dp))
                    }
                }
                AppTab.SETTINGS -> {
                    Text("Connections & Settings", style = MaterialTheme.typography.headlineSmall)
                    Button(onClick = {
                        scope.launch {
                            status = "Checking OpenCode…"
                            client.health()
                                .onSuccess { status = "OpenCode connected"; output = it }
                                .onFailure { status = "OpenCode offline"; output = it.message ?: "Connection failed" }
                        }
                    }) { Text("Check OpenCode connection") }
                    Button(onClick = onRequestTermuxPermission) { Text("Grant Termux command permission") }
                    Button(onClick = {
                        val result = TermuxBridge.startOpenCode(context, useUbuntu)
                        result.onSuccess { status = "OpenCode start requested" }
                            .onFailure { status = "Start failed"; output = it.message ?: "Unknown error" }
                    }) { Text("Start OpenCode server") }
                    Text("Environment: ${if (useUbuntu) "Ubuntu (proot-distro)" else "Termux"}")
                    Text("Server: http://127.0.0.1:4096")
                    Text("OpenCode server is configured for localhost only.")
                }
            }
        }
    }
}
