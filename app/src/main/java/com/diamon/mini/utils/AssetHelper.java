package com.diamon.mini.utils;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.res.AssetManager;
import android.system.Os;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class AssetHelper {
    private static final String TAG = "AssetHelper";
    private static final String PREFS_NAME = "AssetHelperPrefs";
    private static final String KEY_EXTRACTED = "assets_extracted_v2";
    private static final int BUFFER_SIZE = 8192;
    private static volatile String cachedAssetRuntimeRoot;
    private static final String COMPILED_RUNTIME_ROOT = "data/data/com.diamon.mini/files/usr";

    /**
     * Prepara runtime de assets una sola vez. Si ya existen, no vuelve a recorrer
     * todo el árbol; solamente repara archivos críticos faltantes.
     */
    public static synchronized boolean ensureRuntimeReady(Context context) {
        long start = System.currentTimeMillis();
        String runtimeRoot = resolveAssetRuntimeRoot(context);
        if (runtimeRoot == null) {
            Log.e(TAG, "No se encontró ruta runtime en assets (data/data/*/files/usr).");
            return false;
        }

        File usrDir = new File(context.getFilesDir(), "usr");
        boolean alreadyExtracted = areAssetsExtracted(context);

        if (!alreadyExtracted) {
            if (!extractAssets(context, runtimeRoot, usrDir)) {
                return false;
            }
            SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
            prefs.edit().putBoolean(KEY_EXTRACTED, true).apply();

            boolean linked = ensureNativeToolLinks(context);
            long duration = System.currentTimeMillis() - start;
            Log.i(TAG, "ensureRuntimeReady (extracción) completado en " + duration + "ms. Resultado: " + linked);
            return linked;
        }

        boolean ok = ensureNativeToolLinks(context);
        long duration = System.currentTimeMillis() - start;
        Log.i(TAG, "ensureRuntimeReady (reparación) completado en " + duration + "ms. Resultado: " + ok);
        return ok;
    }

    public static String getResolvedRuntimeRoot(Context context) {
        return resolveAssetRuntimeRoot(context);
    }

    private static String resolveAssetRuntimeRoot(Context context) {
        if (cachedAssetRuntimeRoot != null) {
            return cachedAssetRuntimeRoot;
        }

        AssetManager assetManager = context.getAssets();
        try {
            // Prioridad absoluta: ruta exacta de compilación documentada.
            String[] exactChildren = assetManager.list(COMPILED_RUNTIME_ROOT);
            if (exactChildren != null && exactChildren.length > 0) {
                cachedAssetRuntimeRoot = COMPILED_RUNTIME_ROOT;
                Log.i(TAG, "Runtime root de assets detectado (exacto): " + cachedAssetRuntimeRoot);
                return cachedAssetRuntimeRoot;
            }

            String[] pkgCandidates = assetManager.list("data/data");
            if (pkgCandidates == null || pkgCandidates.length == 0) {
                return null;
            }

            String expectedPkg = context.getPackageName();
            // Priorizamos el package real de la app
            for (String pkg : pkgCandidates) {
                if (pkg == null || pkg.isEmpty()) continue;
                String candidateRoot = "data/data/" + pkg + "/files/usr";
                String[] children = assetManager.list(candidateRoot);
                if (children != null && children.length > 0) {
                    cachedAssetRuntimeRoot = candidateRoot;
                    Log.i(TAG, "Runtime root de assets detectado: " + candidateRoot);
                    return cachedAssetRuntimeRoot;
                }
            }
        } catch (IOException e) {
            Log.e(TAG, "No se pudo resolver runtime root de assets: " + e.getMessage());
        }
        return null;
    }

    public static boolean extractAssets(Context context, String assetPath, File destDir) {
        AssetManager assetManager = context.getAssets();

        try {
            String[] files = assetManager.list(assetPath);

            if (files == null || files.length == 0) {
                // Es un archivo, copiarlo
                return copyAssetFile(assetManager, assetPath, destDir);
            } else {
                // Es un directorio, crearlo y procesar recursivamente
                if (!destDir.exists() && !destDir.mkdirs()) {
                    Log.e(TAG, "No se pudo crear directorio: " + destDir.getAbsolutePath());
                    return false;
                }

                for (String fileName : files) {
                    if (fileName == null || fileName.isEmpty()) continue;

                    String childAssetPath = assetPath + "/" + fileName;
                    File childDestDir = new File(destDir, fileName);

                    String[] subFiles = assetManager.list(childAssetPath);
                    if (subFiles != null && subFiles.length > 0) {
                        if (!extractAssets(context, childAssetPath, childDestDir)) {
                            return false;
                        }
                    } else {
                        if (!copyAssetFile(assetManager, childAssetPath, destDir)) {
                            return false;
                        }
                    }
                }
                return true;
            }
        } catch (IOException e) {
            Log.e(TAG, "Error extrayendo assets: " + e.getMessage());
            return false;
        }
    }

    private static boolean copyAssetFile(AssetManager assetManager, String assetPath, File destDir) {
        String fileName = assetPath.substring(assetPath.lastIndexOf('/') + 1);
        File destFile = new File(destDir, fileName);

        if (destFile.exists()) {
            return true;
        }

        if (!destDir.exists()) {
            if (!destDir.mkdirs() && !destDir.exists()) {
                Log.e(TAG, "No se pudo crear directorio: " + destDir.getAbsolutePath());
                return false;
            }
        }

        try (InputStream in = assetManager.open(assetPath);
             OutputStream out = new FileOutputStream(destFile)) {

            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
            if (assetPath.contains("/bin/") || assetPath.contains("/sbin/")) {
                destFile.setExecutable(true, true);
            }
            Log.d(TAG, "Copiado: " + destFile.getAbsolutePath());
            return true;

        } catch (IOException e) {
            Log.e(TAG, "Error copiando " + assetPath + ": " + e.getMessage());
            return false;
        }
    }

    public static boolean areAssetsExtracted(Context context) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean flagged = prefs.getBoolean(KEY_EXTRACTED, false);

        File shareDir = new File(context.getFilesDir(), "usr/share");
        boolean shareExists = shareDir.exists() && shareDir.isDirectory();

        if (flagged && shareExists) {
            String[] list = shareDir.list();
            if (list != null && list.length > 0) {
                return true;
            }
        }

        Log.w(TAG, "areAssetsExtracted: inconsistencia de caché. Forzando re-extracción.");
        return false;
    }

    private static boolean ensureNativeToolLinks(Context context) {
        File filesDir = context.getFilesDir();
        File nativeLibDir = new File(context.getApplicationInfo().nativeLibraryDir);
        File usrBin = new File(filesDir, "usr/bin");
        File usrLib = new File(filesDir, "usr/lib");

        if (!usrBin.exists()) usrBin.mkdirs();
        if (!usrLib.exists()) usrLib.mkdirs();

        boolean ok = true;
        // minipro binary: nativeLibDir/libminipro_bin.so -> usr/bin/minipro
        ok &= linkTool(new File(usrBin, "minipro"), new File(nativeLibDir, "libminipro_bin.so"));
        // libusb: nativeLibDir/libusb-1.0.so -> usr/lib/libusb-1.0.so
        ok &= linkTool(new File(usrLib, "libusb-1.0.so"), new File(nativeLibDir, "libusb-1.0.so"));

        return ok;
    }

    private static boolean linkTool(File linkPath, File target) {
        if (!target.exists()) {
            Log.e(TAG, "Target faltante. Imposible enlazar: " + target.getAbsolutePath());
            return false;
        }

        try {
            if (linkPath.exists() && linkPath.getCanonicalPath().equals(target.getCanonicalPath())) {
                return true;
            }
            linkPath.delete();
            Os.symlink(target.getAbsolutePath(), linkPath.getAbsolutePath());
            return true;
        } catch (Exception e) {
            Log.w(TAG, "Symlink falló para " + linkPath.getName() + " -> " + target.getName()
                    + ": " + e.getMessage() + ". Intentando copia...");
            return copyFile(target, linkPath);
        }
    }

    private static boolean copyFile(File source, File dest) {
        File parent = dest.getParentFile();
        if (parent != null && !parent.exists() && !parent.mkdirs()) {
            return false;
        }
        try (InputStream in = new FileInputStream(source);
             OutputStream out = new FileOutputStream(dest)) {
            byte[] buffer = new byte[BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            out.flush();
            File binParent = dest.getParentFile();
            String parentName = binParent != null ? binParent.getName() : "";
            if ("bin".equals(parentName) || "sbin".equals(parentName)) {
                dest.setExecutable(true, true);
            }
            return true;
        } catch (IOException e) {
            Log.e(TAG, "Error copiando archivo de fallback: " + e.getMessage());
            return false;
        }
    }
}
