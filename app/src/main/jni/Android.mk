LOCAL_PATH := $(call my-dir)
include $(CLEAR_VARS)
LOCAL_MODULE := iperf2
LOCAL_MODULE_TAGS := dev
LOCAL_CFLAGS := -DHAVE_CONFIG_H -UAF_INET6 -w -Wno-error=format-security
LOCAL_LDFLAGS := -fPIE -pie
LOCAL_MODULE_PATH := $(TARGET_OUT_OPTIONAL_EXECUTABLES)
LOCAL_SRC_FILES := \
        ../cpp/compat/delay.c \
        ../cpp/compat/error.c \
        ../cpp/compat/gettimeofday.c \
        ../cpp/compat/inet_ntop.c \
        ../cpp/compat/inet_pton.c \
        ../cpp/compat/signal.c \
        ../cpp/compat/snprintf.c \
        ../cpp/compat/string.c \
        ../cpp/compat/Thread.c \
        ../cpp/src/Extractor.c \
        ../cpp/src/gnu_getopt_long.c \
        ../cpp/src/gnu_getopt.c \
        ../cpp/src/histogram.c \
        ../cpp/src/Locale.c \
        ../cpp/src/pdfs.c \
        ../cpp/src/ReportCSV.c \
        ../cpp/src/ReportDefault.c \
        ../cpp/src/Reporter.c \
        ../cpp/src/service.c \
        ../cpp/src/SocketAddr.c \
        ../cpp/src/sockets.c \
        ../cpp/src/stdio.c \
        ../cpp/src/tcp_window_size.c \
        ../cpp/src/Client.cpp \
        ../cpp/src/isochronous.cpp \
        ../cpp/src/Launch.cpp \
        ../cpp/src/List.cpp \
        ../cpp/src/PerfSocket.cpp \
        ../cpp/src/Settings.cpp \
        ../cpp/src/main.cpp \
        ../cpp/src/Listener.cpp \
        ../cpp/src/Server.cpp

LOCAL_C_INCLUDES += \
        $(LOCAL_PATH)/../cpp \
        $(LOCAL_PATH)/../cpp/include

LOCAL_DISABLE_FORMAT_STRING_CHECKS := true
include $(BUILD_SHARED_LIBRARY)
