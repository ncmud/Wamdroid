LOCAL_PATH := $(call my-dir)

ifeq ($(TARGET_ARCH_ABI),arm64-v8a)

include $(CLEAR_VARS)
LOCAL_MODULE := luajit
LOCAL_SRC_FILES := libluajit-arm64-v8a.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := lua
LOCAL_STATIC_LIBRARIES := luajit
LOCAL_CFLAGS := -O3 -std=c99 -DLUA_USE_DLOPEN
LOCAL_SRC_FILES := luajava.c
include $(BUILD_SHARED_LIBRARY)

endif

ifeq ($(TARGET_ARCH_ABI),x86_64)

include $(CLEAR_VARS)
LOCAL_MODULE := luajit
LOCAL_SRC_FILES := libluajit-x86_64.a
include $(PREBUILT_STATIC_LIBRARY)

include $(CLEAR_VARS)
LOCAL_MODULE := lua
LOCAL_STATIC_LIBRARIES := luajit
LOCAL_CFLAGS := -O3 -std=c99 -DLUA_USE_DLOPEN
LOCAL_SRC_FILES := luajava.c
include $(BUILD_SHARED_LIBRARY)

endif
