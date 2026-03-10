# Wammer - Android

[![Build](https://github.com/ncmud/Wamdroid/actions/workflows/ci.yml/badge.svg)](https://github.com/ncmud/Wamdroid/actions/workflows/ci.yml)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.3-blue)](https://kotlinlang.org)
[![API](https://img.shields.io/badge/API-31%2B-brightgreen)](https://developer.android.com/about/versions/12)
[![License](https://img.shields.io/badge/license-MIT-green)](LICENSE)
![It's dangerous!](https://img.shields.io/badge/You_are_likely_to_be_eaten_by_a-grue-red.svg)
[![Take this.](https://img.shields.io/badge/get-lamp-yellow.svg)](http://getlamp.com)

A MUD client for Android with Lua scripting, plugin support, and multi-connection capability. This is a fork of [BlowTorch](https://github.com/petter-syn/bloern) by Dan Block, modernized to build with current tools and target Android 16.

See also [Wammer - iOS](https://github.com/ncmud/Wammer), the sister project for iPhone, iPad, and macOS.

## About MUDs

[MUDs (Multi-User Dungeons)](https://en.wikipedia.org/wiki/MUD) are online multiplayer text-based games. Thousands of players today are on hundreds of MUDs in all manner of worlds: fantasy, absurdist, sci-fi, horror, and more. Many MUDs have been continuously online for decades.

## Features

- **Multi-connection** — connect to multiple MUDs simultaneously and switch between them
- **Lua scripting** — LuaJIT-powered plugin system with access to triggers, aliases, timers, and the UI
- **Triggers and responders** — pattern-matched triggers with actions: colorize, gag, replace, script execution, notifications, and more
- **Configurable button bars** — customizable button layouts per connection
- **MCCP** — Mud Client Compression Protocol v2 (zlib) for compressed data transfer
- **ANSI color** — full color rendering
- **Per-connection settings** — independent configuration for each MUD connection

## Getting Started

### Prerequisites

- Android SDK (set `ANDROID_SDK_ROOT`)
- Android NDK (set `NDK_HOME`)
- Java 17+

### Build native libraries

The app uses LuaJIT and native extensions (luajava, lsqlite3, marshal, luabins) that must be compiled for each target ABI.

```bash
./build_ndk_libraries_modern.sh
```

This builds for `arm64-v8a` and `x86_64` and copies `.so` files to `BTLib/src/main/jniLibs/`.

### Build the app

```bash
./gradlew :BT_Free:assembleDebug
```

Output: `BT_Free/build/outputs/apk/debug/`

### Install on a connected device

```bash
./gradlew :BT_Free:installDebug
```

### Release builds

Release builds require a signing keystore at `BTLib/key/bt_privatekey.keystore` and the `BT_RELEASE_PASS` environment variable:

```bash
export BT_RELEASE_PASS=your_keystore_password
./gradlew :BT_Free:assembleRelease
```

## Architecture

The app is split into two modules:

| Module | Purpose |
|--------|---------|
| **BTLib** | Shared library containing all core logic (`com.offsetnull.bt`) |
| **BT_Free** | Thin app wrapper with the launcher activity |

### Key components

- **StellarService** — background service managing all connections, runs in a separate process (`:stellar`)
- **Connection** — per-connection handler orchestrating data flow, triggers, and plugins
- **DataPumper** — network I/O thread with socket read/write management
- **Processor** — telnet protocol parser with option negotiation and MCCP decompression
- **Plugin** — Lua plugin system exposing Java API to Lua scripts
- **MainWindow** — main activity with terminal UI, input handling, and button bars

### Data flow

```
MUD Server -> Socket -> DataPumper -> Processor (telnet/MCCP) -> Connection (triggers/plugins) -> MainWindow (render)
User Input -> MainWindow -> Service -> Connection -> DataPumper -> Socket -> MUD Server
```

### IPC

AIDL-based communication between `MainWindow` (foreground) and `StellarService` (background process).

## Contributing

Pull requests, feature requests, and issues are welcome.

## Credits

This project is a fork of [BlowTorch](https://github.com/petter-syn/bloern) by Dan Block. BlowTorch was an impressive piece of work — a full-featured Android MUD client with Lua scripting and plugin support that served the MUD community for years. We are grateful for Dan's contribution to the community and for open-sourcing the project.

## License

Available under the MIT License. See [LICENSE](LICENSE) for details.
