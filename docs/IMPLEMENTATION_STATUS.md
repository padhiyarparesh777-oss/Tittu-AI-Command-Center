# Implementation status

## Foundation committed

- [x] Android Gradle project
- [x] Compose navigation shell: Chat, Terminal, Projects, Settings
- [x] OpenCode HTTP client: health, create session, send prompt
- [x] Termux RUN_COMMAND launch bridge
- [x] Termux / Ubuntu environment selection
- [x] Explicit confirmation dialog before manual terminal command dispatch
- [x] Debug APK GitHub Actions workflow

## Not yet implemented / must be verified

- [ ] Build verified on CI
- [ ] OpenCode event streaming in chat UI
- [ ] Interactive terminal with PTY input/output
- [ ] Termux command result callback
- [ ] Automatic connection supervisor and process recovery
- [ ] Persistent chat history
- [ ] Project picker and Git operations
- [ ] Fine-grained risky command classification
- [ ] End-to-end tests on the user's Android phone

Do not describe unchecked items as complete.
