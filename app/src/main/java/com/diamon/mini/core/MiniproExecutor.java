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
    private volatile Process currentProcess;

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

                ProcessBuilder pb = new ProcessBuilder(command);
                pb.directory(filesDir);
                pb.redirectErrorStream(true);

                int inheritableFd = -1;
                try {
                    // Configurar entorno
                    Map<String, String> env = pb.environment();
                    String ldPath = nativeLibDir.getAbsolutePath();
                    File usrLib = new File(filesDir, "usr/lib");
                    if (usrLib.exists()) {
                        ldPath = nativeLibDir.getAbsolutePath() + ":" + usrLib.getAbsolutePath();
                    }
                    env.put("LD_LIBRARY_PATH", ldPath);
                    env.put("HOME", filesDir.getAbsolutePath());
                    env.put("TMPDIR", context.getCacheDir().getAbsolutePath());

                    String path = new File(filesDir, "usr/bin").getAbsolutePath()
                            + ":" + nativeLibDir.getAbsolutePath()
                            + ":" + System.getenv("PATH");
                    env.put("PATH", path);

                    // Configurar FD USB para libusb parcheada
                    if (usbFd >= 0) {
                        inheritableFd = dupFdForChild(usbFd);
                        if (inheritableFd >= 0) {
                            env.put("ANDROID_USB_FD", String.valueOf(inheritableFd));
                            Log.i(TAG, "USB FD duplicado: " + usbFd + " -> " + inheritableFd + " (heredable)");
                        } else {
                            env.put("ANDROID_USB_FD", String.valueOf(usbFd));
                            Log.w(TAG, "dup() falló, usando FD original: " + usbFd);
                        }
                    } else {
                        env.remove("ANDROID_USB_FD");
                    }

                    // Configurar ruta de datos de minipro
                    if (shareDir.exists()) {
                        env.put("MINIPRO_DATA", shareDir.getAbsolutePath());
                    }

                    callback.onProcessStarted();
                    currentProcess = pb.start();

                    // Leer salida línea por línea
                    try (BufferedReader reader = new BufferedReader(
                            new InputStreamReader(currentProcess.getInputStream()))) {
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

                    int exitCode = currentProcess.waitFor();
                    currentProcess = null;

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
                currentProcess = null;
            }
        });
    }

    /**
     * Aborta el proceso actual de minipro si está en ejecución.
     */
    public void abort() {
        Process p = currentProcess;
        if (p != null) {
            try {
                p.destroy();
                callback.log("[ABORT] Proceso minipro detenido por el usuario.");
            } catch (Exception e) {
                callback.log("[ERROR] No se pudo detener el proceso: " + e.getMessage());
            }
            currentProcess = null;
        } else {
            callback.log("No hay proceso activo para detener.");
        }
    }

    public boolean isRunning() {
        return currentProcess != null;
    }
}
