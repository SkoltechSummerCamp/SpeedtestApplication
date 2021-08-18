#include "jni.h"
#include <fcntl.h>
#include <unistd.h>
#include <cstdio>
#include <sys/stat.h>
#include <sys/wait.h>
#include <cstdlib>

int main(int argc, char **argv);

static pid_t processPid;

extern "C" JNIEXPORT void JNICALL
Java_ru_scoltech_openran_speedtest_IperfRunner_mkfifo(JNIEnv* env, jobject, jstring jPipePath)
{
    const char* pipePath = env->GetStringUTFChars(jPipePath, nullptr);
    mkfifo(pipePath, 0777);
    env->ReleaseStringUTFChars(jPipePath, pipePath);
}

extern "C" JNIEXPORT void JNICALL
Java_ru_scoltech_openran_speedtest_IperfRunner_waitForProcess(JNIEnv* env, jobject)
{
    waitpid(processPid, nullptr, 0);
}

extern "C" JNIEXPORT void JNICALL
Java_ru_scoltech_openran_speedtest_IperfRunner_sendSigInt(JNIEnv* env, jobject)
{
    kill(processPid, SIGINT);
}

extern "C" JNIEXPORT void JNICALL
Java_ru_scoltech_openran_speedtest_IperfRunner_sendSigKill(JNIEnv* env, jobject)
{
    kill(processPid, SIGKILL);
}

int redirectFileToPipe(JNIEnv* env, jstring jPipePath, FILE* file)
{
    const char* pipePath = env->GetStringUTFChars(jPipePath, nullptr);
    const int pipeFd = open(pipePath, O_WRONLY);
    env->ReleaseStringUTFChars(jPipePath, pipePath);

    dup2(pipeFd, fileno(file));
    setbuf(file, nullptr);
    fflush(file);
    return pipeFd;
}

extern "C" JNIEXPORT int JNICALL
Java_ru_scoltech_openran_speedtest_IperfRunner_start(JNIEnv* env, jobject, jstring jStdoutPipePath, jstring jStderrPipePath, jobjectArray args)
{
    processPid = fork();
    if (processPid == -1) {
        return -1;
    } else if (processPid == 0) {
        int stdoutPipeFd = redirectFileToPipe(env, jStdoutPipePath, stdout);
        int stderrPipeFd = redirectFileToPipe(env, jStderrPipePath, stderr);

        int argc = env->GetArrayLength(args) + 1;
        char** argv = new char *[argc];
        argv[0] = "iperf";
        for (int i = 0; i < argc - 1; i++) {
            auto jArg = (jstring) (env->GetObjectArrayElement(args, i));
            argv[i + 1] = (char*) env->GetStringUTFChars(jArg, nullptr);
        }

        main(argc, argv);

        for (int i = 0; i < argc - 1; i++) {
            auto jArg = (jstring) (env->GetObjectArrayElement(args, i));
            env->ReleaseStringUTFChars(jArg, argv[i + 1]);
        }

        close(stderrPipeFd);
        close(stdoutPipeFd);
        exit(0);
    }
    return 0;
}
