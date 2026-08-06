#include <jni.h>
#include <string>
#include <cstdlib>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <android/log.h>

#define LOG_TAG "MiniNative"
#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, LOG_TAG, __VA_ARGS__)
#define LOGE(...) __android_log_print(ANDROID_LOG_ERROR, LOG_TAG, __VA_ARGS__)

extern "C" JNIEXPORT jstring JNICALL
Java_com_diamon_mini_MainActivity_stringFromJNI(
        JNIEnv* env,
        jobject /* this */) {
    std::string hello = "MiniPro Native Bridge Ready";
    return env->NewStringUTF(hello.c_str());
}

extern "C" JNIEXPORT void JNICALL
Java_com_diamon_mini_MainActivity_setUsbFd(
        JNIEnv* env,
        jobject /* this */,
        jint fd) {
    char fdStr[16];
    snprintf(fdStr, sizeof(fdStr), "%d", fd);
    setenv("ANDROID_USB_FD", fdStr, 1);
    LOGI("ANDROID_USB_FD set to %d", fd);
}

extern "C" JNIEXPORT void JNICALL
Java_com_diamon_mini_MainActivity_clearUsbFd(
        JNIEnv* env,
        jobject /* this */) {
    unsetenv("ANDROID_USB_FD");
    LOGI("ANDROID_USB_FD cleared");
}

extern "C" JNIEXPORT jint JNICALL
Java_com_diamon_mini_core_MiniproExecutor_dupFdForChild(JNIEnv *env, jclass clazz, jint fd) {
    if (fd < 0) {
        LOGE("dupFdForChild: FD inválido (%d)", (int) fd);
        return -1;
    }

    int newFd = dup((int) fd);
    if (newFd < 0) {
        LOGE("dupFdForChild: dup(%d) falló, errno=%d", (int) fd, errno);
        return -1;
    }

    int flags = fcntl(newFd, F_GETFD);
    if (flags >= 0 && (flags & FD_CLOEXEC)) {
        fcntl(newFd, F_SETFD, flags & ~FD_CLOEXEC);
        LOGI("dupFdForChild: O_CLOEXEC removido explícitamente del FD %d", newFd);
    }

    LOGI("dupFdForChild: FD %d -> %d (heredable)", (int) fd, newFd);
    return (jint) newFd;
}

extern "C" JNIEXPORT void JNICALL
Java_com_diamon_mini_core_MiniproExecutor_closeDupedFd(JNIEnv *env, jclass clazz, jint fd) {
    if (fd >= 0) {
        LOGI("closeDupedFd: cerrando FD %d", (int) fd);
        close((int) fd);
    }
}