package com.diamon.mini.core;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MiniproExecutor {
    private static final String TAG = "MiniproExecutor";

    // JNI: duplica el FD USB sin O_CLOEXEC para que sea heredable por procesos hijos
    private static native int dupFdForChild(int fd);
    // JNI: cierra el FD duplicado tras finalizar minipro
    private static native void closeDupedFd(int fd);

    // Native process control to bypass ProcessBuilder FD closure
    private static native int[] startNativeProcess(String executable, String[] args, int usbFd, String ldLibraryPath, String miniproData);
    private static native int waitForNativeProcess(int pid);
    private static native void terminateNativeProcess(int pid);

    static {
        System.loadLibrary("mini");
    }

    public interface Callback {
        void log(String message);
        void onProcessStarted();
        void onProcessFinished(int exitCode, String[] args);
    }

    private final Context context;
    private final Callback callback;
    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile int currentPid = -1;

    public MiniproExecutor(Context context, Callback callback) {
        this.context = context;
        this.callback = callback;
    }

    /**
     * Ejecuta minipro con los argumentos dados.
     * @param args argumentos para minipro (ej: "-p", "TL866II+", "-r", "rom.bin")
     * @param usbFd file descriptor del dispositivo USB, o -1 si no hay USB
     */
    public void executeCommand(String[] args, int usbFd) {
        executor.execute(() -> {
            try {
                File nativeLibDir = new File(context.getApplicationInfo().nativeLibraryDir);
                File filesDir = context.getFilesDir();

                // Resolver ruta de minipro
                File miniproBin = new File(nativeLibDir, "libminipro_bin.so");
                if (!miniproBin.exists()) {
                    miniproBin = new File(filesDir, "usr/bin/minipro");
                }
                if (!miniproBin.exists()) {
                    callback.log("[ERROR] No se encontró el binario minipro.");
                    callback.onProcessFinished(-1, args);
                    return;
                }

                File shareDir = new File(filesDir, "usr/share/minipro");

                // Construir comando
                List<String> command = new ArrayList<>();
                command.add(miniproBin.getAbsolutePath());

                // Asegurar paso de base de datos XML de manera dinámica mediante argumentos
                File infoicXml = new File(shareDir, "infoic.xml");
                File logicicXml = new File(shareDir, "logicic.xml");

                boolean hasInfoic = false;
                boolean hasLogicic = false;
                for (String arg : args) {
                    if ("--infoic".equals(arg)) hasInfoic = true;
                    if ("--logicic".equals(arg)) hasLogicic = true;
                }

                if (!hasInfoic && infoicXml.exists()) {
                    command.add("--infoic");
                    command.add(infoicXml.getAbsolutePath());
                }
                if (!hasLogicic && logicicXml.exists()) {
                    command.add("--logicic");
                    command.add(logicicXml.getAbsolutePath());
                }

                command.addAll(Arrays.asList(args));

                // Construct command line arguments (argv array without the executable itself)
                String[] commandArgs = command.subList(1, command.size()).toArray(new String[0]);

                int inheritableFd = -1;
                int childPid = -1;
                int readFd = -1;
                try {
                    String ldPath = nativeLibDir.getAbsolutePath();
                    File usrLib = new File(filesDir, "usr/lib");
                    if (usrLib.exists()) {
                        ldPath = nativeLibDir.getAbsolutePath() + ":" + usrLib.getAbsolutePath();
                    }

                    String miniproDataPath = shareDir.exists() ? shareDir.getAbsolutePath() : "";

                    // Configurar FD USB para libusb parcheada
                    if (usbFd >= 0) {
                        inheritableFd = dupFdForChild(usbFd);
                    }

                    int fdToPass = inheritableFd >= 0 ? inheritableFd : usbFd;

                    callback.onProcessStarted();

                    // Start process natively to prevent ProcessBuilder from closing the file descriptor
                    int[] processInfo = startNativeProcess(miniproBin.getAbsolutePath(), commandArgs, fdToPass, ldPath, miniproDataPath);
                    if (processInfo == null || processInfo[0] <= 0) {
                        callback.log("[ERROR] Error al iniciar el proceso nativo.");
                        callback.onProcessFinished(-1, args);
                        return;
                    }

                    childPid = processInfo[0];
                    readFd = processInfo[1];
                    currentPid = childPid; // Store current pid to allow aborting

                    // Leer la salida del pipe nativo usando ParcelFileDescriptor
                    try (android.os.ParcelFileDescriptor pfd = android.os.ParcelFileDescriptor.adoptFd(readFd);
                         java.io.FileInputStream fis = new java.io.FileInputStream(pfd.getFileDescriptor());
                         BufferedReader reader = new BufferedReader(new InputStreamReader(fis))) {
                        String line;
                        while ((line = reader.readLine()) != null) {
                            if (line.contains("Using overridden database file")
                                    || line.contains("Share dir:")
                                    || line.contains("com.diamon.mini")) {
                                continue;
                            }
                            callback.log(line);
                        }
                    }

                    int exitCode = waitForNativeProcess(childPid);
                    currentPid = -1;

                    callback.onProcessFinished(exitCode, args);
                } finally {
                    if (inheritableFd >= 0) {
                        closeDupedFd(inheritableFd);
                        Log.i(TAG, "FD duplicado " + inheritableFd + " cerrado");
                    }
                }

            } catch (Exception e) {
                Log.e(TAG, "Error ejecutando minipro: " + e.getMessage(), e);
                callback.log("[EXCEPCIÓN] " + e.getMessage());
                callback.onProcessFinished(-1, args);
                currentPid = -1;
            }
        });
    }

    /**
     * Aborta el proceso actual de minipro si está en ejecución.
     */
    public void abort() {
        int pid = currentPid;
        if (pid > 0) {
            try {
                terminateNativeProcess(pid);
                callback.log("[ABORT] Proceso nativo minipro detenido por el usuario.");
            } catch (Exception e) {
                callback.log("[ERROR] No se pudo detener el proceso nativo: " + e.getMessage());
            }
            currentPid = -1;
        } else {
            callback.log("No hay proceso activo para detener.");
        }
    }

    public boolean isRunning() {
        return currentPid > 0;
    }
}
