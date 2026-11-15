# Termux setup

1. Install Termux from a trusted source and open it once.
2. Install OpenCode in Termux, or install it in a proot-distro Ubuntu.
3. Open Termux settings and enable external applications / RUN_COMMAND.
4. If configuring manually, edit `$PREFIX/etc/termux.properties` and set:

```properties
allow-external-apps = true
```

5. Restart Termux after changing the setting.
6. Open Tittu AI Command Center and grant its Termux RUN_COMMAND permission.
7. In the app, choose Termux or Ubuntu and tap **Start OpenCode server**.
8. Tap **Check OpenCode connection**.

## Manual diagnostics

Termux:
```bash
command -v opencode
opencode --version
opencode serve --hostname 127.0.0.1 --port 4096
```

Ubuntu:
```bash
proot-distro login ubuntu
command -v opencode
opencode --version
opencode serve --hostname 127.0.0.1 --port 4096
```

The OpenCode server must stay on 127.0.0.1. Do not change the host to 0.0.0.0.
