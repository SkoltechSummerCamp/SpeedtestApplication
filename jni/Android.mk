LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := hello_world
LOCAL_SRC_FILES := ../src/hello_world.cpp

include $(BUILD_EXECUTABLE)
