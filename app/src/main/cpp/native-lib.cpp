#include <jni.h>
#include <string>
#include <cstdlib>
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