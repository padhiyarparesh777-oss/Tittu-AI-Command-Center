# TITTU AI COMMAND CENTER

Android companion for OpenCode + Termux + Ubuntu. The Android app is the control surface; OpenCode remains the AI coding agent and Termux/Ubuntu performs local execution.

## Initial implementation

- Kotlin + Jetpack Compose Android application
- OpenCode local API client: health check, session creation and prompt submission
- Termux RUN_COMMAND bridge for starting OpenCode in Termux or proot-distro Ubuntu
- Conservative command confirmation in the first terminal screen
- GitHub Actions workflow for debug APK builds

This is an initial foundation, not a claim that every planned feature is complete.

## Requirements

- Android device with Termux installed
- OpenCode installed in the selected Termux or Ubuntu environment
- Termux external-app command permission granted to this app
- OpenCode server bound to localhost on port 4096

Start manually in Termux:
```bash
opencode serve --hostname 127.0.0.1 --port 4096
```

Or, if OpenCode is installed inside a proot-distro named ubuntu:
```bash
proot-distro login ubuntu -- bash -lc 'opencode serve --hostname 127.0.0.1 --port 4096'
```

Keep the server bound to 127.0.0.1. Do not expose it to a LAN or public internet.

## Build

Open in Android Studio or run the GitHub Actions workflow. The CI workflow builds a debug APK and uploads it as an artifact.

## Security

- Initial API client targets localhost only.
- Do not store API keys in source code.
- Terminal commands require an explicit confirmation in the initial UI.
- Android may stop background processes due to battery or memory management; auto-start is best-effort.
