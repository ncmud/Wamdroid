# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## What This Is

MUDWammer (Wamdroid) — an Android MUD client with Lua scripting, plugin support, and multi-connection capability.

## Build Commands

### Prerequisites
- Android SDK (set `ANDROID_SDK_ROOT`)
- Android NDK (set `NDK_HOME`)

### Native Libraries (LuaJIT + Extensions)
```bash
./build_ndk_libraries_modern.sh
```
Builds LuaJIT 2.1 for arm64-v8a and x86_64. Then builds JNI modules (luajava, lsqlite3, sqlite3, bit, marshal, luabins) and copies `.so` files to `lib/src/main/jniLibs/<abi>/`.

### Android App
```bash
./gradlew :app:assembleDebug     # Debug APK
./gradlew :app:assembleRelease   # Release (needs signing cert + BT_RELEASE_PASS env var)
```
Output: `app/build/outputs/apk/`

Release signing cert: `lib/key/bt_privatekey.keystore`.

### No Test Infrastructure
No unit or integration tests exist.

## Architecture

### Module Structure
- **lib** — Shared library (all core logic). Package: `org.ncmud.mudwammer`
- **app** — Thin app wrapper (1 Java file: `FreeLauncher`). Package: `org.ncmud.mudwammer.app`

### Key Classes (all in lib `org.ncmud.mudwammer`)

| Class | Role |
|-------|------|
| `service.StellarService` | Background service managing all connections. Runs in separate process (`:stellar`). |
| `service.Connection` | Per-connection handler — largest class (~137KB). Orchestrates data flow, triggers, plugins. |
| `service.DataPumper` | Network I/O thread. Reads from socket, hands to Processor. Contains `OutputWriterThread`. |
| `service.Processor` | Parses telnet protocol, handles option negotiation, MCCP decompression. |
| `service.plugin.Plugin` | Lua plugin system (~93KB). Loads/runs Lua scripts, exposes Java API to Lua. |
| `window.MainWindow` | Main activity (~111KB). Terminal UI, input handling, button bars. |
| `window.TextTree` | Efficient line-based terminal buffer storage. |
| `launcher.Launcher` | Connection management UI (add/edit/launch MUD connections). |

### IPC Model
AIDL-based communication between MainWindow (foreground) and StellarService (background process):
- `IConnectionBinder` — Service exposes to Window
- `IWindowCallback` — Window registers callbacks with Service
- `ILauncherCallback` — Launcher registers with Service

### Data Flow
```
MUD Server → Socket → DataPumper → Processor (telnet/MCCP) → Connection (triggers/plugins/Lua) → MainWindow (render)
User Input → MainWindow → Service → Connection → DataPumper → Socket → MUD Server
```

### Threading
- StellarService — Handler thread managing connections
- Connection — Handler-based message dispatch (40+ MESSAGE_* constants)
- DataPumper — Socket I/O thread + child OutputWriterThread
- Lua plugin execution on service handler thread

### Plugin/Scripting System
Lua scripts (LuaJIT 2.0.5) with Java bridge via `org.keplerproject.luajava`. Plugins are XML-configured with Lua code. Available Lua extensions: sqlite3, bitwise ops, binary serialization (marshal, luabins).

### Trigger/Responder System
Pattern-matched triggers with 7 action types in `org.ncmud.mudwammer.responder`:
- `ack/` — Acknowledgment
- `color/` — Colorize matched text
- `gag/` — Suppress output
- `notification/` — Android notifications
- `replace/` — Text replacement
- `script/` — Execute Lua
- `toast/` — Toast messages

### Configuration
XML-based per-connection settings. Plugin manifests loaded at runtime. Hot-reload via MESSAGE_RELOADSETTINGS.

## Git Notes
- Main branch: `trunk` (not `master`)
