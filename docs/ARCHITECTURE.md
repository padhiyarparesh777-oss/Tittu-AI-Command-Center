# Architecture

## Components

- Android UI: Kotlin + Jetpack Compose.
- OpenCode client: local HTTP API at 127.0.0.1:4096.
- Termux bridge: Android RUN_COMMAND service.
- Ubuntu: optional proot-distro environment launched through Termux.

## Trust boundaries

The Android app is a local control surface. OpenCode executes agent tools in its configured project context. Termux RUN_COMMAND starts commands in the Termux environment. Ubuntu commands are wrapped with `proot-distro login ubuntu`.

The first milestone uses localhost-only OpenCode networking. Remote access is intentionally not enabled.

## Planned next milestones

1. Stream OpenCode events and render structured assistant messages.
2. Use OpenCode PTY endpoints for interactive terminal sessions where supported by the installed OpenCode version.
3. Capture Termux command results through a PendingIntent callback.
4. Add project directory selection and session-to-project mapping.
5. Add command risk classification and approval policies.
6. Add foreground-service based connection supervision with explicit Android notification.
