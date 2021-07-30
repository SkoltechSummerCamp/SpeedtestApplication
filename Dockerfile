FROM ubuntu:16.04

RUN apt-get update -qq && \
    apt-get -y upgrade -qq
RUN apt-get install -y make bash git unzip wget curl openjdk-8-jdk build-essential autoconf nano tree

ENV ANDROID_SDK_VERSION 4333796
ENV ANDROID_SDK_HOME /opt/android-sdk
ENV ANDROID_SDK_FILENAME sdk-tools-linux-${ANDROID_SDK_VERSION}
ENV ANDROID_SDK_URL https://dl.google.com/android/repository/${ANDROID_SDK_FILENAME}.zip

RUN wget --no-check-certificate -q ${ANDROID_SDK_URL} && \
    mkdir -p ${ANDROID_SDK_HOME} && \
    unzip -q ${ANDROID_SDK_FILENAME}.zip -d ${ANDROID_SDK_HOME} && \
    rm -f ${ANDROID_SDK_FILENAME}.zip

ENV PATH=${PATH}:${ANDROID_SDK_HOME}/tools:${ANDROID_SDK_HOME}/tools/bin:${ANDROID_SDK_HOME}/platform-tools

RUN yes | sdkmanager --licenses
RUN yes | sdkmanager "platforms;android-24"

ENV ANDROID_NDK_VERSION r19
ENV ANDROID_NDK_HOME /opt/android-ndk
ENV ANDROID_NDK_FILENAME android-ndk-${ANDROID_NDK_VERSION}-linux-x86_64
ENV ANDROID_NDK_URL https://dl.google.com/android/repository/${ANDROID_NDK_FILENAME}.zip

RUN wget --no-check-certificate -q ${ANDROID_NDK_URL} && \
    mkdir -p ${ANDROID_NDK_HOME} && \
    unzip -q ${ANDROID_NDK_FILENAME}.zip && \
    mv ./android-ndk-${ANDROID_NDK_VERSION}/* ${ANDROID_NDK_HOME} && \
    rm -f ${ANDROID_NDK_FILENAME}.zip

ENV PATH=${PATH}:${ANDROID_NDK_HOME}

ENV NDK_PROJECT_PATH=/tmp

RUN apt-get clean

RUN apt-get install g++

# Config files

RUN mkdir -p /tmp/jni
COPY jni/Android.mk /tmp/jni
COPY jni/Application.mk /tmp/jni

# Sources

RUN mkdir -p /tmp/src
COPY src/ /tmp/src

# Compile

RUN ndk-build clean

RUN ndk-build NDK_APPLICATION_MK=/tmp/jni/Application.mk APP_BUILD_SCRIPT=/tmp/jni/Android.mk

RUN tree /tmp/libs
