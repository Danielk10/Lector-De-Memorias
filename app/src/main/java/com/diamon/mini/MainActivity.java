package com.diamon.mini;

import android.app.AlertDialog;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;

import com.diamon.mini.core.MiniproExecutor;
import com.diamon.mini.core.UsbController;
import com.diamon.mini.utils.AssetHelper;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private static final String TAG = "MiniproApp";
    private static final String PREFS = "minipro_prefs";
    private static final String KEY_EXPORT_URI = "export_uri";
    private static final String KEY_LAST_READ_FILE = "last_read_file";
    private static final String KEY_LAST_VERSION = "last_version_code";

    // Used to load the 'mini' library on application startup.
    static {
        System.loadLibrary("mini");
    }

    private UsbController usbController;
    private MiniproExecutor miniproExecutor;

    // UI
    private LinearLayout layoutLoading, layoutMainUI;
    private ScrollView scrollLog;
    private TextView tvStatus, tvLog, tvLoadingText, tvOperationStatus;
    private Spinner spinnerDevices;
    private Button btnConnect, btnProbe, btnRead, btnWrite, btnImport, btnExport;
    private Button btnRunCustomCommand, btnClearLogs, btnQuickClear, btnEraseChip, btnAbort, btnVerify;
    private EditText etCustomCommand, etChipModel;

    // Log buffering
    private final StringBuilder logBuffer = new StringBuilder();
    private final Handler logHandler = new Handler(Looper.getMainLooper());
    private boolean isLogUpdatePending = false;
    private final Runnable logUpdater = () -> {
        String newLogs;
        synchronized (logBuffer) {
            newLogs = logBuffer.toString();
            logBuffer.setLength(0);
            isLogUpdatePending = false;
        }
        if (!newLogs.isEmpty()) {
            tvLog.append(newLogs);
            scrollLog.post(() -> scrollLog.fullScroll(ScrollView.FOCUS_DOWN));
        }
    };

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean hasReadData = false;
    private volatile String lastReadFile = "rom.bin";

    private static final String[] SUPPORTED_DEVICES = {
            "TL866II+", "TL866A", "TL866CS", "T48", "T56", "T76"
    };

    // Activity Result Launchers
    private final ActivityResultLauncher<Intent> fileOpenLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        importFile(uri);
                    }
                }
            });

    private final ActivityResultLauncher<Intent> fileSaveLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        try {
                            getContentResolver().takePersistableUriPermission(uri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                        } catch (Exception ignored) {}
                        exportFileToUri(uri);
                    }
                }
            });

    private final ActivityResultLauncher<Intent> directoryPickerLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri treeUri = result.getData().getData();
                    if (treeUri != null) {
                        try {
                            getContentResolver().takePersistableUriPermission(treeUri,
                                    Intent.FLAG_GRANT_READ_URI_PERMISSION | Intent.FLAG_GRANT_WRITE_URI_PERMISSION);
                            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                                    .putString(KEY_EXPORT_URI, treeUri.toString()).apply();
                            log("Directorio de exportación configurado y guardado.");
                        } catch (Exception e) {
                            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                                    .putString(KEY_EXPORT_URI, treeUri.toString()).apply();
                            log("Directorio configurado (sin persistencia extendida).");
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find views
        layoutLoading = findViewById(R.id.layoutLoading);
        layoutMainUI = findViewById(R.id.layoutMainUI);
        tvLoadingText = findViewById(R.id.tvLoadingText);
        tvStatus = findViewById(R.id.tvStatus);
        tvLog = findViewById(R.id.tvLog);
        scrollLog = findViewById(R.id.scrollLog);
        tvOperationStatus = findViewById(R.id.tvOperationStatus);
        spinnerDevices = findViewById(R.id.spinnerDevices);

        btnConnect = findViewById(R.id.btnConnect);
        btnProbe = findViewById(R.id.btnProbe);
        btnVerify = findViewById(R.id.btnVerify);
        btnRead = findViewById(R.id.btnRead);
        btnWrite = findViewById(R.id.btnWrite);
        btnImport = findViewById(R.id.btnImport);
        btnExport = findViewById(R.id.btnExport);
        btnQuickClear = findViewById(R.id.btnQuickClear);
        btnEraseChip = findViewById(R.id.btnEraseChip);
        btnRunCustomCommand = findViewById(R.id.btnRunCustomCommand);
        btnClearLogs = findViewById(R.id.btnClearLogs);
        btnAbort = findViewById(R.id.btnAbort);
        etCustomCommand = findViewById(R.id.etCustomCommand);
        etChipModel = findViewById(R.id.etChipModel);

        // Setup device spinner
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.custom_spinner_item, SUPPORTED_DEVICES);
        adapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        spinnerDevices.setAdapter(adapter);

        // Initialize USB controller
        usbController = new UsbController(this, new UsbController.Callback() {
            @Override
            public void log(String message) {
                MainActivity.this.log(message);
            }

            @Override
            public void onDeviceConnected(String deviceName, int fd, String vidPid, boolean isRecognized) {
                runOnUiThread(() -> {
                    tvStatus.setText(getString(R.string.str_status_usb_connected, deviceName));
                    MainActivity.this.log("¡Permiso otorgado! Token USB: " + fd);
                    MainActivity.this.log("Conectado a USB VID:PID " + vidPid);

                    // Pasar FD al entorno nativo
                    setUsbFd(fd);

                    if (isRecognized) {
                        String autoName = UsbController.USB_AUTO_MAP.get(vidPid);
                        MainActivity.this.log("[OK] Dispositivo reconocido: " + autoName);
                        // Seleccionar en spinner si es posible
                        for (int i = 0; i < SUPPORTED_DEVICES.length; i++) {
                            if (SUPPORTED_DEVICES[i].equals(autoName)) {
                                spinnerDevices.setSelection(i);
                                break;
                            }
                        }
                    } else {
                        MainActivity.this.log("════════════════════════════════════════");
                        MainActivity.this.log("[AVISO] Dispositivo NO reconocido como programador minipro.");
                        MainActivity.this.log("Puedes intentar con los botones o la consola.");
                        MainActivity.this.log("════════════════════════════════════════");
                    }

                    btnProbe.setEnabled(true);
                    btnVerify.setEnabled(true);
                    btnRead.setEnabled(true);
                    btnWrite.setEnabled(true);
                    btnEraseChip.setEnabled(true);
                });
            }

            @Override
            public void onDeviceConnectionFailed(String deviceName) {
                MainActivity.this.log(deviceName + " falló en enlazarse a la app.");
            }

            @Override
            public void onDeviceDisconnected() {
                runOnUiThread(() -> {
                    tvStatus.setText(getString(R.string.str_estado_usb_desc));
                    clearUsbFd();
                    btnProbe.setEnabled(false);
                    btnVerify.setEnabled(false);
                    btnRead.setEnabled(false);
                    btnWrite.setEnabled(false);
                    btnEraseChip.setEnabled(false);
                    MainActivity.this.log("Dispositivo USB desconectado.");
                });
            }
        });

        // Initialize MiniPro executor
        miniproExecutor = new MiniproExecutor(this, new MiniproExecutor.Callback() {
            @Override
            public void log(String message) {
                MainActivity.this.log(message);
            }

            @Override
            public void onProcessStarted() {
                runOnUiThread(() -> {
                    if (btnAbort != null) btnAbort.setVisibility(View.VISIBLE);
                });
            }

            @Override
            public void onProcessFinished(int exitCode, String[] args) {
                runOnUiThread(() -> {
                    if (btnAbort != null) btnAbort.setVisibility(View.GONE);
                });

                if (exitCode == 0) {
                    for (int i = 0; i < args.length; i++) {
                        if ("-r".equals(args[i]) && i + 1 < args.length) {
                            hasReadData = true;
                            lastReadFile = args[i + 1];
                            SharedPreferences.Editor editor = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
                            editor.putString("bios_source", "Leído del chip (" + getSelectedDevice() + ")");
                            editor.putString(KEY_LAST_READ_FILE, lastReadFile);
                            editor.apply();
                            break;
                        }
                    }
                }
            }
        });

        // Setup copy on long click for terminal
        setupLogCopySupport();

        log("--- Aplicación Iniciada ---");

        // Asset extraction logic
        int currentVersion = getVersionCode();
        int lastVersion = getSharedPreferences(PREFS, MODE_PRIVATE).getInt(KEY_LAST_VERSION, -1);
        boolean assetsReady = AssetHelper.areAssetsExtracted(getApplicationContext());
        boolean skipLoading = assetsReady && (currentVersion == lastVersion);

        if (skipLoading) {
            layoutMainUI.setVisibility(View.VISIBLE);
            layoutLoading.setVisibility(View.GONE);
            log("Sistema minipro y assets listos.");

            executor.execute(() -> {
                File buggedDir = new File(getFilesDir(), "usr/usr");
                if (buggedDir.exists()) deleteRecursively(buggedDir);
                AssetHelper.ensureRuntimeReady(getApplicationContext());
            });
        } else {
            layoutMainUI.setVisibility(View.GONE);
            layoutLoading.setVisibility(View.VISIBLE);

            executor.execute(() -> {
                File buggedDir = new File(getFilesDir(), "usr/usr");
                if (buggedDir.exists()) deleteRecursively(buggedDir);

                boolean wasExtracted = AssetHelper.areAssetsExtracted(getApplicationContext());
                if (!wasExtracted) {
                    runOnUiThread(() -> tvLoadingText.setText(R.string.str_extracting_libs));
                } else {
                    runOnUiThread(() -> tvLoadingText.setText(R.string.str_verifying_dependencies));
                }

                boolean runtimeReady = AssetHelper.ensureRuntimeReady(getApplicationContext());

                runOnUiThread(() -> {
                    layoutLoading.setVisibility(View.GONE);
                    layoutMainUI.setVisibility(View.VISIBLE);

                    if (!wasExtracted) {
                        log(getString(R.string.str_log_new_install));
                        log(getString(R.string.str_log_preparing_resources));
                    } else {
                        log(getString(R.string.str_log_update_detected, lastVersion, currentVersion));
                        log(getString(R.string.str_log_verifying_resources));
                    }

                    logRuntimeInfo();

                    if (!runtimeReady) {
                        log(getString(R.string.str_log_warn_dependencies_failed));
                    } else {
                        log(wasExtracted ?
                                getString(R.string.str_log_resources_verified) :
                                getString(R.string.str_log_assets_copied));
                        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                                .putInt(KEY_LAST_VERSION, currentVersion).apply();
                    }

                    logHandler.removeCallbacks(logUpdater);
                    logUpdater.run();
                });
            });
        }

        // Register USB receiver
        usbController.registerReceiver();

        // Button listeners
        btnConnect.setOnClickListener(v -> usbController.searchAndRequestDevice());

        btnProbe.setOnClickListener(v -> {
            String chip = etChipModel.getText().toString().trim();
            executeMinipro("-p", chip);
        });

        btnVerify.setOnClickListener(v -> {
            String chip = etChipModel.getText().toString().trim();
            executeMinipro("-p", chip, "-m", "rom.bin");
        });

        btnRead.setOnClickListener(v -> {
            String chip = etChipModel.getText().toString().trim();
            executeMinipro("-p", chip, "-r", "rom.bin");
        });

        btnWrite.setOnClickListener(v -> {
            String chip = etChipModel.getText().toString().trim();
            executeMinipro("-p", chip, "-w", "rom.bin");
        });

        btnImport.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            fileOpenLauncher.launch(intent);
        });

        btnExport.setOnClickListener(v -> {
            if (!hasReadData) {
                log("Error: No hay datos leídos del chip aún.");
                log("Usa 'Leer Chip' primero para leer el contenido.");
                return;
            }
            File sourceFile = new File(getFilesDir(), lastReadFile);
            if (!sourceFile.exists()) {
                log("Error: El archivo '" + lastReadFile + "' no existe.");
                return;
            }
            String exportName = lastReadFile.equals("rom.bin") ? "dump_backup.bin" : lastReadFile;
            Intent intent = new Intent(Intent.ACTION_CREATE_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("application/octet-stream");
            intent.putExtra(Intent.EXTRA_TITLE, exportName);
            fileSaveLauncher.launch(intent);
        });

        btnEraseChip.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.str_confirm_erase_title)
                    .setMessage(R.string.str_confirm_erase_msg)
                    .setPositiveButton(R.string.str_yes_erase, (dialog, which) -> {
                        String chip = etChipModel.getText().toString().trim();
                        executeMinipro("-p", chip, "-E");
                    })
                    .setNegativeButton(R.string.str_cancelar, null)
                    .show();
        });

        btnQuickClear.setOnClickListener(v -> clearTransientRomState(true));

        btnRunCustomCommand.setOnClickListener(v -> {
            String rawCommand = etCustomCommand.getText() == null ? "" :
                    etCustomCommand.getText().toString().trim();
            if (rawCommand.isEmpty()) {
                log(getString(R.string.str_log_write_command_help));
                return;
            }
            executeCustomCommand(rawCommand);
        });

        btnClearLogs.setOnClickListener(v -> {
            synchronized (logBuffer) {
                logBuffer.setLength(0);
            }
            tvLog.setText("");
        });

        btnAbort.setOnClickListener(v -> miniproExecutor.abort());
    }

    // ========== Menu ==========

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_hex_viewer) {
            startActivity(new Intent(this, HexViewerActivity.class));
            return true;
        } else if (id == R.id.action_select_dir) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            directoryPickerLauncher.launch(intent);
            return true;
        } else if (id == R.id.action_about) {
            new AlertDialog.Builder(this)
                    .setTitle(R.string.str_acerca_de_titulo)
                    .setMessage(R.string.str_acerca_de_msg)
                    .setPositiveButton(R.string.str_cerrar, null)
                    .show();
            return true;
        } else if (id == R.id.action_policy) {
            startActivity(new Intent(this, PolicyActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ========== Helpers ==========

    private void log(String msg) {
        String line = msg + "\n";
        synchronized (logBuffer) {
            logBuffer.append(line);
            if (!isLogUpdatePending) {
                isLogUpdatePending = true;
                logHandler.postDelayed(logUpdater, 100);
            }
        }
    }

    private String getSelectedDevice() {
        Object selected = spinnerDevices.getSelectedItem();
        return selected != null ? selected.toString() : "TL866II+";
    }

    private void executeMinipro(String... args) {
        miniproExecutor.executeCommand(args, usbController.getCurrentFd());
    }

    private void executeCustomCommand(String raw) {
        // Parsear comando: puede ser "minipro -p TL866II+ -r rom.bin" o simplemente "-p TL866II+ -r rom.bin"
        String command = raw.trim();
        if (command.startsWith("minipro ")) {
            command = command.substring(8).trim();
        } else if (command.equals("minipro")) {
            command = "";
        }

        if (command.isEmpty()) {
            executeMinipro(); // sin args, muestra ayuda
            return;
        }

        // Parseo simple de argumentos respetando comillas
        List<String> args = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;
        for (char c : command.toCharArray()) {
            if (c == '"' || c == '\'') {
                inQuotes = !inQuotes;
            } else if (c == ' ' && !inQuotes) {
                if (current.length() > 0) {
                    args.add(current.toString());
                    current.setLength(0);
                }
            } else {
                current.append(c);
            }
        }
        if (current.length() > 0) {
            args.add(current.toString());
        }

        miniproExecutor.executeCommand(args.toArray(new String[0]), usbController.getCurrentFd());
    }

    private void clearTransientRomState(boolean notify) {
        hasReadData = false;
        lastReadFile = "rom.bin";
        File dumpFile = new File(getFilesDir(), "rom.bin");
        boolean exists = dumpFile.exists();
        boolean deleted = false;
        if (exists) {
            deleted = dumpFile.delete();
        }
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .remove("bios_source")
                .remove(KEY_LAST_READ_FILE)
                .apply();
        if (notify) {
            if (exists) {
                if (deleted) {
                    log("Archivo de datos (rom.bin) eliminado.");
                } else {
                    log("Error al intentar eliminar el archivo de datos.");
                }
            } else {
                log("No se eliminó ningún archivo porque no había datos.");
            }
        }
    }

    private int getVersionCode() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                return (int) getPackageManager().getPackageInfo(getPackageName(), 0).getLongVersionCode();
            } else {
                return getPackageManager().getPackageInfo(getPackageName(), 0).versionCode;
            }
        } catch (Exception e) {
            return 1;
        }
    }

    private void logRuntimeInfo() {
        log("Arch: " + Build.SUPPORTED_ABIS[0] + " | SDK: " + Build.VERSION.SDK_INT);
        log("Dispositivo: " + Build.MANUFACTURER + " " + Build.MODEL);

        File nativeLibDir = new File(getApplicationInfo().nativeLibraryDir);
        File miniproBin = new File(nativeLibDir, "libminipro_bin.so");
        log("minipro: " + (miniproBin.exists() ? "OK (" + miniproBin.length() / 1024 + " KB)" : "NO ENCONTRADO"));

        File libusb = new File(nativeLibDir, "libusb_1_0.so");
        log("libusb: " + (libusb.exists() ? "OK (" + libusb.length() / 1024 + " KB)" : "NO ENCONTRADO"));

        // Check native bridge
        log("Native bridge: " + stringFromJNI());
    }

    private void importFile(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) {
                throw new IllegalStateException("No se pudo abrir el archivo.");
            }

            String fileName = "archivo";
            long fileSize = -1;
            try {
                android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
                if (cursor != null && cursor.moveToFirst()) {
                    int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (idx >= 0) fileName = cursor.getString(idx);
                    int sizeIdx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                    if (sizeIdx >= 0) fileSize = cursor.getLong(sizeIdx);
                    cursor.close();
                }
            } catch (Exception ignored) {}

            if (fileSize > 128L * 1024 * 1024) {
                log("Error: Archivo demasiado grande (" + (fileSize / 1024 / 1024) + " MB). Máximo: 128 MB.");
                return;
            }

            boolean isIntelHex = fileName.toLowerCase().endsWith(".hex");
            File outFile = new File(getFilesDir(), "rom.bin");
            clearTransientRomState(false);
            long totalWritten = 0;

            try (OutputStream out = new FileOutputStream(outFile)) {
                if (isIntelHex) {
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    byte[] buffer = new byte[8192];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        baos.write(buffer, 0, read);
                    }
                    byte[] data = baos.toByteArray();
                    // Simplemente copiar como binario
                    out.write(data);
                    totalWritten = data.length;
                } else {
                    byte[] buffer = new byte[65536];
                    int read;
                    while ((read = in.read(buffer)) != -1) {
                        out.write(buffer, 0, read);
                        totalWritten += read;
                    }
                }
            }

            if (totalWritten == 0) {
                log("Error: El archivo seleccionado está vacío.");
                return;
            }

            String sizeStr;
            if (totalWritten >= 1024 * 1024) {
                sizeStr = String.format(Locale.US, "%.2f MB", totalWritten / (1024.0 * 1024.0));
            } else {
                sizeStr = String.format(Locale.US, "%.1f KB", totalWritten / 1024.0);
            }

            log("ROM importada: '" + fileName + "' (" + sizeStr + ")");
            log("Archivo guardado como 'rom.bin' — listo para Flashear o Verificar.");
            hasReadData = true;
            lastReadFile = "rom.bin";

            SharedPreferences.Editor editor = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
            editor.putString("bios_source", "Importado: " + fileName + " (" + sizeStr + ")");
            editor.putString(KEY_LAST_READ_FILE, "rom.bin");
            editor.apply();
        } catch (Exception e) {
            log("Error importando archivo: " + e.getMessage());
        }
    }

    private void exportFileToUri(Uri uri) {
        File sourceFile = new File(getFilesDir(), lastReadFile);
        try (InputStream in = new java.io.FileInputStream(sourceFile);
             OutputStream out = getContentResolver().openOutputStream(uri)) {

            if (out == null) throw new Exception("No se pudo acceder al archivo destino.");

            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            log("Éxito: '" + lastReadFile + "' exportado correctamente.");
        } catch (Exception e) {
            log("Error exportando archivo: " + e.getMessage());
        }
    }

    private void setupLogCopySupport() {
        tvLog.setOnLongClickListener(v -> {
            CharSequence text = tvLog.getText();
            if (text != null && text.length() > 0) {
                ClipboardManager clipboard = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
                ClipData clip = ClipData.newPlainText("Terminal Log", text);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(this, R.string.str_copiado, Toast.LENGTH_SHORT).show();
            }
            return true;
        });
    }

    private void deleteRecursively(File file) {
        if (file.isDirectory()) {
            File[] children = file.listFiles();
            if (children != null) {
                for (File child : children) {
                    deleteRecursively(child);
                }
            }
        }
        file.delete();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        usbController.unregisterReceiver();
        executor.shutdownNow();
    }

    // ========== Native methods ==========

    /**
     * A native method that is implemented by the 'mini' native library,
     * which is packaged with this application.
     */
    public native String stringFromJNI();
    public native void setUsbFd(int fd);
    public native void clearUsbFd();
}