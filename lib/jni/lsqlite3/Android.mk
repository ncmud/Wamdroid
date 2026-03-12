LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := liblsqlite3
LOCAL_MODULE_FILENAME := liblsqlite3
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../sqlite3 $(LOCAL_PATH)/../luajava
LOCAL_SRC_FILES := ./lsqlite3.c
LOCAL_SHARED_LIBRARIES := sqlite3 lua
LOCAL_CFLAGS := -O3 -std=c99
include $(BUILD_SHARED_LIBRARY)
