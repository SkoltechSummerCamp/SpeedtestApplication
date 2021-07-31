#include <jni.h>
#include <iostream>
#include <stdio.h>
#include <string.h>
#include <fcntl.h>
#include <sys/stat.h>
#include <sys/types.h>
#include <unistd.h>
#include <chrono>
#include <thread>

using namespace std;

extern "C" JNIEXPORT void JNICALL
Java_com_company_test_MainActivity_mkfifo(JNIEnv* env, jobject, jstring jPipePath)
{
    const char* pipePath = env->GetStringUTFChars(jPipePath, nullptr);
    mkfifo(pipePath, 0777);
    env->ReleaseStringUTFChars(jPipePath, pipePath);
}

extern "C" JNIEXPORT int JNICALL
Java_com_company_test_MainActivity_mainJni(JNIEnv* env, jobject, jstring jPipePath)
{
    const char* pipePath = env->GetStringUTFChars(jPipePath, nullptr);
    const int pipeFd = open(pipePath, O_WRONLY);
    env->ReleaseStringUTFChars(jPipePath, pipePath);
    dup2(pipeFd, STDOUT_FILENO);
    setbuf(stdout, nullptr);

    for (int i = 1; i <= 10; i++) {
        printf("Hello, World! (%d)\n", i);
        std::this_thread::sleep_for(std::chrono::seconds(1));
    }

    close(pipeFd);
    return 0;
}
