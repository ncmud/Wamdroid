LOCAL_PATH := $(call my-dir)

include $(CLEAR_VARS)
LOCAL_MODULE := libmarshal
LOCAL_MODULE_FILENAME := libmarshal
LOCAL_C_INCLUDES += $(LOCAL_PATH)/../luajava
LOCAL_SRC_FILES := ./lmarshal.c
LOCAL_SHARED_LIBRARIES := lua
LOCAL_CFLAGS := -O3 -std=c99
include $(BUILD_SHARED_LIBRARY)
