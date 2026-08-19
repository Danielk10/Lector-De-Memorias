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

#include <sys/wait.h>
#include <sys/types.h>
#include <signal.h>
#include <vector>

extern "C" JNIEXPORT jintArray JNICALL
Java_com_diamon_mini_core_MiniproExecutor_startNativeProcess(
        JNIEnv *env, jclass clazz,
        jstring jExecutable,
        jobjectArray jArgs,
        jint usbFd,
        jstring jLdLibraryPath,
        jstring jMiniproData) {

    const char *execPath = env->GetStringUTFChars(jExecutable, nullptr);
    const char *ldLibPath = env->GetStringUTFChars(jLdLibraryPath, nullptr);
    const char *miniproData = env->GetStringUTFChars(jMiniproData, nullptr);

    // Preparar argumentos (argv)
    int argc = env->GetArrayLength(jArgs);
    std::vector<char*> argv;
    std::vector<const char*> stringsToRelease;
    argv.push_back(const_cast<char*>(execPath)); // argv[0] debe ser el nombre del ejecutable
    for (int i = 0; i < argc; i++) {
        jstring argStr = (jstring) env->GetObjectArrayElement(jArgs, i);
        const char *arg = env->GetStringUTFChars(argStr, nullptr);
        argv.push_back(const_cast<char*>(arg));
        stringsToRelease.push_back(arg);
    }
    argv.push_back(nullptr);

    // Crear pipe para capturar stdout y stderr
    int pipefds[2];
    if (pipe(pipefds) < 0) {
        LOGE("startNativeProcess: pipe falló, errno=%d", errno);
        env->ReleaseStringUTFChars(jExecutable, execPath);
        env->ReleaseStringUTFChars(jLdLibraryPath, ldLibPath);
        env->ReleaseStringUTFChars(jMiniproData, miniproData);
        for (int i = 0; i < argc; i++) {
            jstring argStr = (jstring) env->GetObjectArrayElement(jArgs, i);
            env->ReleaseStringUTFChars(argStr, stringsToRelease[i]);
        }
        return nullptr;
    }

    pid_t pid = fork();
    if (pid == 0) {
        // --- PROCESO HIJO ---
        // Redirigir stdout y stderr al write-end del pipe
        dup2(pipefds[1], STDOUT_FILENO);
        dup2(pipefds[1], STDERR_FILENO);
        close(pipefds[0]);
        close(pipefds[1]);

        // Configurar variables de entorno
        char fdStr[16];
        snprintf(fdStr, sizeof(fdStr), "%d", usbFd);
        if (usbFd >= 0) {
            setenv("ANDROID_USB_FD", fdStr, 1);
        } else {
            unsetenv("ANDROID_USB_FD");
        }
        setenv("LD_LIBRARY_PATH", ldLibPath, 1);
        setenv("MINIPRO_DATA", miniproData, 1);

        // Remover O_CLOEXEC del FD de USB por si acaso
        if (usbFd >= 0) {
            int flags = fcntl(usbFd, F_GETFD);
            if (flags >= 0 && (flags & FD_CLOEXEC)) {
                fcntl(usbFd, F_SETFD, flags & ~FD_CLOEXEC);
            }
        }

        // Ejecutar el binario
        execv(execPath, argv.data());
        exit(127);
    }

    // --- PROCESO PADRE ---
    // Cerrar el write-end del pipe en el padre
    close(pipefds[1]);

    // Liberar memoria JNI
    env->ReleaseStringUTFChars(jExecutable, execPath);
    env->ReleaseStringUTFChars(jLdLibraryPath, ldLibPath);
    env->ReleaseStringUTFChars(jMiniproData, miniproData);
    for (int i = 0; i < argc; i++) {
        jstring argStr = (jstring) env->GetObjectArrayElement(jArgs, i);
        env->ReleaseStringUTFChars(argStr, stringsToRelease[i]);
    }

    jintArray result = env->NewIntArray(2);
    if (result != nullptr) {
        jint arr[2] = { (jint) pid, (jint) pipefds[0] };
        env->SetIntArrayRegion(result, 0, 2, arr);
    }
    return result;
}

extern "C" JNIEXPORT jint JNICALL
Java_com_diamon_mini_core_MiniproExecutor_waitForNativeProcess(
        JNIEnv *env, jclass clazz, jint pid) {
    int status;
    if (waitpid((pid_t) pid, &status, 0) < 0) {
        return -1;
    }
    if (WIFEXITED(status)) {
        return WEXITSTATUS(status);
    }
    return -1;
}

extern "C" JNIEXPORT void JNICALL
Java_com_diamon_mini_core_MiniproExecutor_terminateNativeProcess(
        JNIEnv *env, jclass clazz, jint pid) {
    if (pid > 0) {
        kill((pid_t) pid, SIGKILL);
    }
}