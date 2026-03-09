#!/bin/bash
# Build LuaJIT 2.1 and JNI extensions for modern Android (arm64-v8a, x86_64)
# Requires: NDK r26+ installed via Homebrew or Android SDK Manager
set -euo pipefail

# Find NDK
if [ -n "${NDK_HOME:-}" ]; then
    NDK="$NDK_HOME"
elif [ -f /opt/homebrew/bin/ndk-build ]; then
    # Homebrew cask install
    NDK_EXEC=$(cat /opt/homebrew/bin/ndk-build | grep "readonly executable" | cut -d'"' -f2)
    NDK=$(dirname "$NDK_EXEC")
else
    echo "ERROR: Cannot find Android NDK. Set NDK_HOME or install via: brew install --cask android-ndk"
    exit 1
fi

echo "Using NDK at: $NDK"

LUAJIT_DIR="LuaJIT-2.1"
NDKABI=31  # Match our minSdk

if [ ! -d "$LUAJIT_DIR" ]; then
    echo "ERROR: $LUAJIT_DIR directory not found. Clone it with:"
    echo "  git clone https://github.com/LuaJIT/LuaJIT.git LuaJIT-2.1"
    exit 1
fi

TOOLCHAIN="$NDK/toolchains/llvm/prebuilt/darwin-x86_64"
if [ ! -d "$TOOLCHAIN" ]; then
    # Try linux
    TOOLCHAIN="$NDK/toolchains/llvm/prebuilt/linux-x86_64"
fi
if [ ! -d "$TOOLCHAIN" ]; then
    echo "ERROR: Cannot find NDK toolchain at $TOOLCHAIN"
    exit 1
fi

echo "Using toolchain: $TOOLCHAIN"

# Required by LuaJIT 2.1 Makefile on macOS
export MACOSX_DEPLOYMENT_TARGET=14.0

# Clean prior builds
echo "**********************************************"
echo "********* Cleaning prior builds. *************"
echo "**********************************************"
cd "$LUAJIT_DIR"
make clean || true
cd ..

# Clean old JNI artifacts
rm -f BTLib/jni/luajava/libluajit-*.a BTLib/jni/luajava/libluajit-*.so

build_luajit() {
    local ARCH=$1
    local CROSS_PREFIX=$2
    local TARGET_FLAGS=$3
    local OUTPUT_SUFFIX=$4

    echo ""
    echo "**********************************************"
    echo "** Building LuaJIT for $ARCH"
    echo "**********************************************"

    cd "$LUAJIT_DIR"
    make clean

    # For cross-compilation, HOST_CC must be a native compiler.
    # LuaJIT needs to build minilua/buildvm as host tools.
    make -j$(sysctl -n hw.ncpu) \
        CROSS="$CROSS_PREFIX" \
        STATIC_CC="${CROSS_PREFIX}clang" \
        DYNAMIC_CC="${CROSS_PREFIX}clang -fPIC" \
        TARGET_LD="${CROSS_PREFIX}clang" \
        TARGET_AR="${TOOLCHAIN}/bin/llvm-ar rcus" \
        TARGET_STRIP="${TOOLCHAIN}/bin/llvm-strip" \
        TARGET_FLAGS="$TARGET_FLAGS" \
        TARGET_SYS=Linux \
        BUILDMODE=static \
        HOST_CC="clang" \
        TARGET_CFLAGS="-fPIC" \
        XCFLAGS="-DLUAJIT_ENABLE_LUA52COMPAT"

    cp src/libluajit.a "src/libluajit-${OUTPUT_SUFFIX}.a"
    cd ..
}

# arm64-v8a
CROSS_ARM64="${TOOLCHAIN}/bin/aarch64-linux-android${NDKABI}-"
build_luajit "arm64-v8a" "$CROSS_ARM64" "" "arm64-v8a"

# x86_64 (for emulators)
CROSS_X86_64="${TOOLCHAIN}/bin/x86_64-linux-android${NDKABI}-"
build_luajit "x86_64" "$CROSS_X86_64" "" "x86_64"

# Copy output to JNI project
echo ""
echo "**********************************************"
echo "** Copying LuaJIT output to BTLib/jni/luajava"
echo "**********************************************"
cp "$LUAJIT_DIR/src/libluajit-arm64-v8a.a" BTLib/jni/luajava/
cp "$LUAJIT_DIR/src/libluajit-x86_64.a" BTLib/jni/luajava/

# Copy headers
echo "Copying LuaJIT headers..."
cp "$LUAJIT_DIR/src/lauxlib.h" BTLib/jni/luajava/
cp "$LUAJIT_DIR/src/lua.h" BTLib/jni/luajava/
cp "$LUAJIT_DIR/src/luaconf.h" BTLib/jni/luajava/
cp "$LUAJIT_DIR/src/luajit.h" BTLib/jni/luajava/
cp "$LUAJIT_DIR/src/luajit_rolling.h" BTLib/jni/luajava/
cp "$LUAJIT_DIR/src/lualib.h" BTLib/jni/luajava/

# Build JNI modules with ndk-build
echo ""
echo "**********************************************"
echo "** Building JNI modules (luajava, sqlite3, etc.)"
echo "**********************************************"
cd BTLib
"$NDK/ndk-build" NDK_PROJECT_PATH=. APP_BUILD_SCRIPT=jni/Android.mk NDK_APPLICATION_MK=jni/Application.mk

echo ""
echo "**********************************************"
echo "** Build complete! Libraries in BTLib/libs/"
echo "**********************************************"
ls -la libs/*/
