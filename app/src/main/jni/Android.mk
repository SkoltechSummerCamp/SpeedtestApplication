LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := hello_world
LOCAL_SRC_FILES := ../cpp/hello_world.cpp

include $(BUILD_SHARED_LIBRARY)
