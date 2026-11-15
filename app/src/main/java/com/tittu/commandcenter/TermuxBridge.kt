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
        } else command

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

    /**
     * Install OpenCode on first launch when missing, then start its local API.
     * The installer is the official OpenCode install script; logs are kept in
     * ~/.opencode-server.log so setup failures can be inspected in Termux.
     */
    fun startOpenCode(context: Context, useUbuntu: Boolean): Result<Unit> =
        runShellCommand(
            context,
            "export PATH=\"\$HOME/.opencode/bin:\$HOME/.local/bin:\$PATH\"; " +
                "if ! command -v opencode >/dev/null 2>&1; then " +
                "command -v curl >/dev/null 2>&1 || { echo 'curl is required to install OpenCode'; exit 127; }; " +
                "curl -fsSL https://opencode.ai/install | bash || exit 1; " +
                "export PATH=\"\$HOME/.opencode/bin:\$HOME/.local/bin:\$PATH\"; fi; " +
                "command -v opencode || exit 127; " +
                "if ! pgrep -f '[o]pencode serve.*4096' >/dev/null 2>&1; then " +
                "nohup opencode serve --hostname 0.0.0.0 --port 4096 > \"\$HOME/.opencode-server.log\" 2>&1 < /dev/null & fi",
            useUbuntu,
            background = true
        )

    fun setupUbuntuDesktop(context: Context, useUbuntu: Boolean): Result<Unit> =
        runShellCommand(
            context,
            "apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y xfce4 xfce4-terminal tigervnc-standalone-server dbus-x11",
            useUbuntu,
            background = true
        )

    fun startUbuntuDesktop(context: Context, useUbuntu: Boolean): Result<Unit> =
        runShellCommand(
            context,
            "mkdir -p \$HOME/.vnc && printf '#!/bin/sh\\nstartxfce4 &\\n' > \$HOME/.vnc/xstartup && chmod +x \$HOME/.vnc/xstartup && (vncserver -kill :1 >/dev/null 2>&1 || true) && vncserver :1 -localhost yes -geometry 1280x800 -depth 24",
            useUbuntu,
            background = true
        )

    fun stopUbuntuDesktop(context: Context, useUbuntu: Boolean): Result<Unit> =
        runShellCommand(context, "vncserver -kill :1", useUbuntu, background = true)

    fun installDeveloperTools(context: Context, useUbuntu: Boolean): Result<Unit> =
        runShellCommand(
            context,
            "apt-get update && DEBIAN_FRONTEND=noninteractive apt-get install -y git curl wget nano vim build-essential python3 python3-pip nodejs npm openssh-server",
            useUbuntu,
            background = true
        )

    private fun shellQuote(value: String): String =
        "'" + value.replace("'", "'\\''") + "'"
}
