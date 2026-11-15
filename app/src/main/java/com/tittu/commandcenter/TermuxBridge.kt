package com.tittu.commandcenter

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat

object TermuxBridge {
    private const val TERMUX_PACKAGE = "com.termux"
    const val RUN_COMMAND_PERMISSION = "com.termux.permission.RUN_COMMAND"
    private const val RUN_COMMAND_ACTION = "com.termux.RUN_COMMAND"
    private const val RUN_COMMAND_SERVICE = "com.termux.app.RunCommandService"
    private const val TERMUX_BASH = "/data/data/com.termux/files/usr/bin/bash"

    fun hasPermission(context: Context): Boolean =
        ContextCompat.checkSelfPermission(context, RUN_COMMAND_PERMISSION) ==
            PackageManager.PERMISSION_GRANTED

    fun isTermuxInstalled(context: Context): Boolean = try {
        context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
        true
    } catch (_: Exception) {
        false
    }

    fun runShellCommand(
        context: Context,
        command: String,
        useUbuntu: Boolean,
        background: Boolean = false
    ): Result<Unit> = runCatching {
        check(isTermuxInstalled(context)) { "Termux is not installed." }
        check(hasPermission(context)) { "Grant Termux RUN_COMMAND permission first." }

        val actualCommand = if (useUbuntu) {
            "proot-distro login ubuntu -- bash -lc " + shellQuote(command)
        } else {
            command
        }

        val intent = Intent(RUN_COMMAND_ACTION).apply {
            setClassName(TERMUX_PACKAGE, RUN_COMMAND_SERVICE)
            putExtra("com.termux.RUN_COMMAND_PATH", TERMUX_BASH)
            putExtra("com.termux.RUN_COMMAND_ARGUMENTS", arrayOf("-lc", actualCommand))
            putExtra("com.termux.RUN_COMMAND_WORKDIR", "/data/data/com.termux/files/home")
            putExtra("com.termux.RUN_COMMAND_BACKGROUND", background)
        }
        context.startService(intent)
        Unit
    }

    fun startOpenCode(context: Context, useUbuntu: Boolean): Result<Unit> =
        runShellCommand(
            context,
            "if ! pgrep -f 'opencode serve.*4096' >/dev/null 2>&1; then nohup opencode serve --hostname 127.0.0.1 --port 4096 > \"\$HOME/.opencode-server.log\" 2>&1 < /dev/null & fi",
            useUbuntu,
            background = true
        )

    private fun shellQuote(value: String): String =
        "'" + value.replace("'", "'\\''") + "'"
}
