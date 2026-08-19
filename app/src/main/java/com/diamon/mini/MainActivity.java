package com.diamon.mini;

import android.util.Log;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

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
import android.text.Editable;
import android.text.Html;
import android.text.TextWatcher;
import android.text.method.LinkMovementMethod;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.diamon.mini.core.MiniproExecutor;
import com.diamon.mini.core.UsbController;
import com.diamon.mini.ui.views.LogScrollView;
import com.diamon.mini.ui.views.PinoutView;
import com.diamon.mini.utils.AssetHelper;
import com.diamon.mini.utils.ChipDatabase;
import com.diamon.mini.utils.FileManager;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Arrays;
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
    private static final String KEY_SELECTED_CHIP = "selected_chip_model";
    private static final String KEY_SELECTED_DEVICE = "selected_device_model";

    // Used to load the 'mini' native library on application startup.
    static {
        System.loadLibrary("mini");
    }

    private UsbController usbController;
    private MiniproExecutor miniproExecutor;

    // UI
    private LinearLayout layoutLoading, layoutMainUI;
    private LogScrollView scrollLog;
    private TextView tvStatus, tvLog, tvLoadingText, tvOperationStatus;
    private Spinner spinnerDevices;
    private Button btnConnect, btnRead, btnWrite, btnImport, btnExport;
    private Button btnRunCustomCommand, btnClearLogs, btnQuickClear, btnEraseChip, btnAbort, btnVerify;
    private Button btnSearchChip, btnAutodetectChip;
    private EditText etCustomCommand, etChipModel;

    // Terminal Log Buffering with Carriage Return (\r) Overwrite Handling
    private final List<StringBuilder> consoleLines = new ArrayList<>();
    private int currentLineIndex = -1;
    private boolean cursorAtStartOfLine = false;
    private final Handler logHandler = new Handler(Looper.getMainLooper());
    private boolean isLogUpdatePending = false;

    private boolean isScrollAtBottom() {
        if (scrollLog == null || tvLog == null) return true;
        int scrollY = scrollLog.getScrollY();
        int scrollHeight = scrollLog.getHeight();
        int contentHeight = tvLog.getHeight();
        if (contentHeight == 0) return true;
        return (scrollY + scrollHeight) >= (contentHeight - 100);
    }

    private final Runnable logUpdater = new Runnable() {
        @Override
        public void run() {
            if (isFinishing() || isDestroyed()) {
                isLogUpdatePending = false;
                return;
            }
            String fullLogs;
            synchronized (consoleLines) {
                StringBuilder sb = new StringBuilder();
                for (int i = 0; i < consoleLines.size(); i++) {
                    if (i > 0) sb.append("\n");
                    sb.append(consoleLines.get(i).toString());
                }
                fullLogs = sb.toString();
                isLogUpdatePending = false;
            }

            final boolean wasAtBottom = isScrollAtBottom();
            final int scrollY = scrollLog != null ? scrollLog.getScrollY() : 0;

            tvLog.setText(fullLogs);

            boolean shouldScrollToBottom = wasAtBottom;
            if (miniproExecutor != null && miniproExecutor.isRunning()) {
                shouldScrollToBottom = true;
            }

            if (scrollLog != null) {
                if (shouldScrollToBottom) {
                    scrollLog.post(() -> scrollLog.fullScroll(ScrollView.FOCUS_DOWN));
                } else {
                    scrollLog.post(() -> scrollLog.setScrollY(scrollY));
                }
            }
        }
    };

    private final ExecutorService executor = Executors.newSingleThreadExecutor();
    private volatile boolean hasReadData = false;
    private volatile String lastReadFile = "rom.bin";
    private final List<String> currentOutputLines = new ArrayList<>();

    private static final String[] SUPPORTED_DEVICES = {
            "TL866II+", "TL866A", "TL866CS", "T48", "T56", "T76", "Logic"
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
                            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                                    .putString(KEY_EXPORT_URI, uri.toString()).apply();
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
                            log(getString(R.string.str_export_dir_saved));
                        } catch (Exception e) {
                            getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                                    .putString(KEY_EXPORT_URI, treeUri.toString()).apply();
                            log(getString(R.string.str_export_dir_no_persist));
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Find Views
        layoutLoading = findViewById(R.id.layoutLoading);
        layoutMainUI = findViewById(R.id.layoutMainUI);
        tvLoadingText = findViewById(R.id.tvLoadingText);
        tvStatus = findViewById(R.id.tvStatus);
        tvLog = findViewById(R.id.tvLog);
        scrollLog = findViewById(R.id.scrollLog);
        tvOperationStatus = findViewById(R.id.tvOperationStatus);
        spinnerDevices = findViewById(R.id.spinnerDevices);

        btnSearchChip = findViewById(R.id.btnSearchChip);
        btnAutodetectChip = findViewById(R.id.btnAutodetectChip);
        btnConnect = findViewById(R.id.btnConnect);
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

        // Restore saved chip model
        String savedChip = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_SELECTED_CHIP, "W25Q128FV");
        etChipModel.setText(savedChip);

        // Setup Device Spinner with Custom Item Layouts
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this,
                R.layout.custom_spinner_item, SUPPORTED_DEVICES);
        adapter.setDropDownViewResource(R.layout.custom_spinner_dropdown_item);
        spinnerDevices.setAdapter(adapter);

        // Restore saved programmer model
        String savedDevice = getSharedPreferences(PREFS, MODE_PRIVATE).getString(KEY_SELECTED_DEVICE, SUPPORTED_DEVICES[0]);
        for (int i = 0; i < SUPPORTED_DEVICES.length; i++) {
            if (SUPPORTED_DEVICES[i].equalsIgnoreCase(savedDevice)) {
                spinnerDevices.setSelection(i);
                break;
            }
        }

        spinnerDevices.setOnItemSelectedListener(new android.widget.AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(android.widget.AdapterView<?> parent, View view, int position, long id) {
                if (position >= 0 && position < SUPPORTED_DEVICES.length) {
                    saveSelectedDevice(SUPPORTED_DEVICES[position]);
                }
            }

            @Override
            public void onNothingSelected(android.widget.AdapterView<?> parent) {}
        });

        // Initialize USB Controller
        usbController = new UsbController(this, new UsbController.Callback() {
            @Override
            public void log(String message) {
                MainActivity.this.log(message);
            }

            @Override
            public void onDeviceConnected(String deviceName, int fd, String vidPid, boolean isRecognized, String autoProgrammer) {
                runOnUiThread(() -> {
                    tvStatus.setText(getString(R.string.str_status_usb_connected, deviceName));
                    MainActivity.this.log(getString(R.string.str_usb_permission_granted, fd));
                    MainActivity.this.log(getString(R.string.str_connected_usb_vid_pid, vidPid));

                    // Pasar FD al entorno nativo de libusb
                    setUsbFd(fd);

                    if (isRecognized && autoProgrammer != null) {
                        MainActivity.this.log(getString(R.string.str_device_recognized, autoProgrammer));
                        for (int i = 0; i < SUPPORTED_DEVICES.length; i++) {
                            if (SUPPORTED_DEVICES[i].equalsIgnoreCase(autoProgrammer)) {
                                spinnerDevices.setSelection(i);
                                saveSelectedDevice(SUPPORTED_DEVICES[i]);
                                break;
                            }
                        }
                    } else {
                        MainActivity.this.log("════════════════════════════════════════");
                        if (UsbController.UNSUPPORTED_USB_MAP.containsKey(vidPid)) {
                            MainActivity.this.log(getString(R.string.str_warn_unsupported_programmer, UsbController.UNSUPPORTED_USB_MAP.get(vidPid)));
                        } else {
                            MainActivity.this.log(getString(R.string.str_warn_unrecognized_usb));
                        }
                        MainActivity.this.log("════════════════════════════════════════");
                    }

                    btnAutodetectChip.setEnabled(true);
                    btnVerify.setEnabled(true);
                    btnRead.setEnabled(true);
                    btnWrite.setEnabled(true);
                    btnEraseChip.setEnabled(true);
                });
            }

            @Override
            public void onDeviceConnectionFailed(String deviceName) {
                MainActivity.this.log(getString(R.string.str_usb_connection_failed, deviceName));
            }

            @Override
            public void onDeviceDisconnected() {
                runOnUiThread(() -> {
                    tvStatus.setText(getString(R.string.str_estado_usb_desc));
                    clearUsbFd();
                    btnAutodetectChip.setEnabled(false);
                    btnVerify.setEnabled(false);
                    btnRead.setEnabled(false);
                    btnWrite.setEnabled(false);
                    btnEraseChip.setEnabled(false);
                    MainActivity.this.log(getString(R.string.str_usb_disconnected));
                });
            }
        });

        // Initialize MiniPro Executor
        miniproExecutor = new MiniproExecutor(this, new MiniproExecutor.Callback() {
            @Override
            public void log(String message) {
                MainActivity.this.log(message);
                synchronized (currentOutputLines) {
                    currentOutputLines.add(message);
                }
            }

            @Override
            public void onProcessStarted() {
                synchronized (currentOutputLines) {
                    currentOutputLines.clear();
                }
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
                            editor.putString("bios_source", getString(R.string.str_read_from_chip, getSelectedChip()));
                            editor.putString(KEY_LAST_READ_FILE, lastReadFile);
                            editor.apply();
                            break;
                        }
                    }

                    // Autodetección (-a)
                    boolean isAutodetect = false;
                    for (String arg : args) {
                        if ("-a".equals(arg) || "-d".equals(arg)) {
                            isAutodetect = true;
                            break;
                        }
                    }
                    if (isAutodetect) {
                        String foundChip = null;
                        boolean foundAutodetectLine = false;
                        synchronized (currentOutputLines) {
                            for (String line : currentOutputLines) {
                                if (line.contains("Autodetecting device") || line.contains("Found")) {
                                    foundAutodetectLine = true;
                                    continue;
                                }
                                if (foundAutodetectLine) {
                                    String trimmed = line.trim();
                                    if (!trimmed.isEmpty() && !trimmed.contains("device(s) found") && !trimmed.contains("Error")) {
                                        int atIdx = trimmed.indexOf('@');
                                        foundChip = atIdx > 0 ? trimmed.substring(0, atIdx) : trimmed;
                                        break;
                                    }
                                }
                            }
                        }
                        if (foundChip != null) {
                            final String chipToSet = foundChip;
                            runOnUiThread(() -> {
                                etChipModel.setText(chipToSet);
                                ChipDatabase.addRecentChip(MainActivity.this, chipToSet);
                                saveSelectedChip(chipToSet);
                                MainActivity.this.log(getString(R.string.str_chip_detected_configured, chipToSet));
                            });
                        }
                    }
                }
            }
        });

        setupLogCopySupport();

        log(getString(R.string.str_app_started));

        // Runtime & Asset Initialization
        int currentVersion = getVersionCode();
        int lastVersion = getSharedPreferences(PREFS, MODE_PRIVATE).getInt(KEY_LAST_VERSION, -1);
        boolean assetsReady = AssetHelper.areAssetsExtracted(getApplicationContext());
        boolean skipLoading = assetsReady && (currentVersion == lastVersion);

        if (skipLoading) {
            layoutMainUI.setVisibility(View.VISIBLE);
            layoutLoading.setVisibility(View.GONE);
            log(getString(R.string.str_system_ready));

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

        // ────────── Button Listeners ─────────────────────────────────────────

        btnConnect.setOnClickListener(v -> usbController.searchAndRequestDevice());

        btnSearchChip.setOnClickListener(v -> showChipSelectorDialog());

        btnAutodetectChip.setOnClickListener(v -> {
            log(getString(R.string.str_starting_autodetect));
            executeMinipro("-a", "8");
        });

        btnVerify.setOnClickListener(v -> {
            String chip = getSelectedChip();
            if (chip.isEmpty()) {
                log(getString(R.string.str_err_specify_chip));
                return;
            }
            File f = new File(getFilesDir(), "rom.bin");
            if (!f.exists() || f.length() == 0) {
                log(getString(R.string.str_err_no_rom_loaded));
                return;
            }
            executeMinipro("-p", chip, "-m", "rom.bin");
        });

        btnRead.setOnClickListener(v -> {
            String chip = getSelectedChip();
            if (chip.isEmpty()) {
                log(getString(R.string.str_err_specify_chip));
                return;
            }
            executeMinipro("-p", chip, "-r", "rom.bin");
        });

        btnWrite.setOnClickListener(v -> {
            String chip = getSelectedChip();
            if (chip.isEmpty()) {
                log(getString(R.string.str_err_specify_chip));
                return;
            }
            File f = new File(getFilesDir(), "rom.bin");
            if (!f.exists() || f.length() == 0) {
                log(getString(R.string.str_err_no_rom_loaded));
                return;
            }
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
                log(getString(R.string.str_err_no_data_read));
                log(getString(R.string.str_use_read_chip_first));
                return;
            }
            File sourceFile = new File(getFilesDir(), lastReadFile);
            if (!sourceFile.exists()) {
                log(getString(R.string.str_file_not_found, lastReadFile));
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
                        String chip = getSelectedChip();
                        if (chip.isEmpty()) {
                            log(getString(R.string.str_err_specify_chip));
                            return;
                        }
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
            synchronized (consoleLines) {
                consoleLines.clear();
                currentLineIndex = -1;
                cursorAtStartOfLine = false;
            }
            tvLog.setText("");
        });

        btnAbort.setOnClickListener(v -> miniproExecutor.abort());
    }

    // ────────── Menu ─────────────────────────────────────────────────────────

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
        } else if (id == R.id.action_hex_diff) {
            startActivity(new Intent(this, HexDiffActivity.class));
            return true;
        } else if (id == R.id.action_pinouts) {
            showPinoutsDialog();
            return true;
        } else if (id == R.id.action_select_dir) {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT_TREE);
            directoryPickerLauncher.launch(intent);
            return true;
        } else if (id == R.id.action_about) {
            showAboutDialog();
            return true;
        } else if (id == R.id.action_policy) {
            startActivity(new Intent(this, PolicyActivity.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    // ────────── Chip Selection Dialog & Search ───────────────────────────────

    private void showChipSelectorDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        View dialogView = LayoutInflater.from(this).inflate(R.layout.custom_spinner_dropdown_item, null);

        // Crear vista personalizada programática limpia
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int pad = (int) (12 * getResources().getDisplayMetrics().density);
        layout.setPadding(pad, pad, pad, pad);
        layout.setBackgroundColor(0xFF12141D);

        // Barra de búsqueda
        EditText searchBox = new EditText(this);
        searchBox.setHint(R.string.str_buscar_chip_hint);
        searchBox.setHintTextColor(0xFF757575);
        searchBox.setTextColor(0xFFFFFFFF);
        searchBox.setBackgroundResource(R.drawable.bg_input_field);
        searchBox.setPadding(pad, pad / 2, pad, pad / 2);
        searchBox.setTextSize(14f);
        layout.addView(searchBox);

        // Botón Agregar Chip Manual
        Button btnAddManual = new Button(this);
        btnAddManual.setText(R.string.str_agregar_chip_manual);
        btnAddManual.setTextSize(12f);
        btnAddManual.setBackgroundColor(0xFF1565C0);
        btnAddManual.setTextColor(0xFFFFFFFF);
        LinearLayout.LayoutParams lpBtn = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                (int) (38 * getResources().getDisplayMetrics().density));
        lpBtn.setMargins(0, pad / 2, 0, pad / 2);
        layout.addView(btnAddManual, lpBtn);

        // Lista de chips
        ListView listView = new ListView(this);
        List<String> allChips = new ArrayList<>();
        List<String> recentChips = ChipDatabase.getRecentChips(this);
        if (!recentChips.isEmpty()) {
            allChips.addAll(recentChips);
        }
        List<String> customChips = ChipDatabase.getCustomChips(this);
        for (String c : customChips) {
            if (!allChips.contains(c)) allChips.add(c);
        }
        for (String c : ChipDatabase.getAllPredefinedChips()) {
            if (!allChips.contains(c)) allChips.add(c);
        }

        final List<String> currentFiltered = new ArrayList<>(allChips);
        ArrayAdapter<String> chipAdapter = new ArrayAdapter<String>(this,
                android.R.layout.simple_list_item_1, currentFiltered) {
            @Override
            public View getView(int position, View convertView, android.view.ViewGroup parent) {
                TextView tv = (TextView) super.getView(position, convertView, parent);
                tv.setTextColor(0xFFECEFF1);
                tv.setTextSize(14f);
                tv.setBackgroundColor(0xFF1C2234);
                int p = (int) (10 * getResources().getDisplayMetrics().density);
                tv.setPadding(p, p, p, p);
                return tv;
            }
        };
        listView.setAdapter(chipAdapter);
        layout.addView(listView, new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 600));

        AlertDialog dialog = builder.setTitle(R.string.str_seleccionar_chip_dialog_title)
                .setView(layout)
                .setNegativeButton(R.string.str_cerrar, null)
                .create();

        // Filtro en tiempo real
        searchBox.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                String query = s.toString().trim().toUpperCase();
                currentFiltered.clear();
                if (query.isEmpty()) {
                    currentFiltered.addAll(allChips);
                } else {
                    for (String chip : allChips) {
                        if (chip.toUpperCase().contains(query)) {
                            currentFiltered.add(chip);
                        }
                    }
                }
                chipAdapter.notifyDataSetChanged();
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // Selección de chip
        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position >= 0 && position < currentFiltered.size()) {
                String selected = currentFiltered.get(position);
                etChipModel.setText(selected);
                ChipDatabase.addRecentChip(this, selected);
                saveSelectedChip(selected);
                log(getString(R.string.str_chip_configured, selected));
                dialog.dismiss();
            }
        });

        // Botón Agregar Manual
        btnAddManual.setOnClickListener(v -> {
            dialog.dismiss();
            showAddManualChipDialog();
        });

        dialog.show();
    }

    private void showAddManualChipDialog() {
        EditText input = new EditText(this);
        input.setHint(R.string.str_chip_manual_hint);
        input.setHintTextColor(0xFF757575);
        input.setTextColor(0xFFFFFFFF);
        input.setBackgroundResource(R.drawable.bg_input_field);
        int pad = (int) (12 * getResources().getDisplayMetrics().density);
        input.setPadding(pad, pad, pad, pad);

        new AlertDialog.Builder(this)
                .setTitle(R.string.str_chip_manual_title)
                .setView(input)
                .setPositiveButton(R.string.str_guardar, (dialog, which) -> {
                    String chip = input.getText().toString().trim().toUpperCase();
                    if (!chip.isEmpty()) {
                        ChipDatabase.addCustomChip(this, chip);
                        etChipModel.setText(chip);
                        saveSelectedChip(chip);
                        log(getString(R.string.str_custom_chip_added, chip));
                    }
                })
                .setNegativeButton(R.string.str_cancelar, null)
                .show();
    }

    // ────────── Pinouts Dialog ───────────────────────────────────────────────

    private void showPinoutsDialog() {
        String[] options = {
                getString(R.string.str_pinout_opt_zif40),
                getString(R.string.str_pinout_opt_spi25),
                getString(R.string.str_pinout_opt_i2c24),
                getString(R.string.str_pinout_opt_mw93),
                getString(R.string.str_pinout_opt_parallel),
                getString(R.string.str_pinout_opt_icsp),
                getString(R.string.str_pinout_opt_avrisp),
                getString(R.string.str_pinout_opt_plcc32)
        };

        new AlertDialog.Builder(this)
                .setTitle(R.string.str_pinouts_de_hard)
                .setItems(options, (dialog, which) -> {
                    ImageView iv = new ImageView(this);
                    iv.setBackgroundColor(0xFF12141D);
                    int pad = (int) (8 * getResources().getDisplayMetrics().density);
                    iv.setPadding(pad, pad, pad, pad);
                    String title = options[which];

                    switch (which) {
                        case 0:
                            PinoutView.dibujarZIF40(this, iv);
                            break;
                        case 1:
                            PinoutView.dibujarSPI25(this, iv);
                            break;
                        case 2:
                            PinoutView.dibujarI2C24(this, iv);
                            break;
                        case 3:
                            PinoutView.dibujarMicrowire93(this, iv);
                            break;
                        case 4:
                            PinoutView.dibujarParallelDIP(this, iv);
                            break;
                        case 5:
                            PinoutView.dibujarICSP(this, iv);
                            break;
                        case 6:
                            PinoutView.dibujarAVRISP(this, iv);
                            break;
                        case 7:
                        default:
                            PinoutView.dibujarPLCC32(this, iv);
                            break;
                    }

                    ScrollView scroll = new ScrollView(this);
                    scroll.addView(iv);
                    new AlertDialog.Builder(this)
                            .setTitle(title)
                            .setView(scroll)
                            .setPositiveButton(R.string.str_cerrar, null)
                            .show();
                })
                .setNegativeButton(R.string.str_cancelar, null)
                .show();
    }

    // ────────── About & Licenses Dialog ──────────────────────────────────────

    private void showAboutDialog() {
        ScrollView scrollView = new ScrollView(this);
        scrollView.setBackgroundColor(0xFF12141D);
        TextView aboutText = new TextView(this);
        int padding = (int) (18 * getResources().getDisplayMetrics().density);
        aboutText.setPadding(padding, padding, padding, padding / 2);
        aboutText.setMovementMethod(LinkMovementMethod.getInstance());
        aboutText.setTextColor(0xFFECEFF1);
        aboutText.setLinkTextColor(0xFF00BFA5);
        aboutText.setTextSize(14f);

        String aboutHtml = getString(R.string.str_about_html);

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            aboutText.setText(Html.fromHtml(aboutHtml, Html.FROM_HTML_MODE_COMPACT));
        } else {
            @SuppressWarnings("deprecation")
            CharSequence text = Html.fromHtml(aboutHtml);
            aboutText.setText(text);
        }

        scrollView.addView(aboutText);

        new AlertDialog.Builder(this)
                .setTitle(R.string.str_acerca_de_titulo)
                .setView(scrollView)
                .setPositiveButton(R.string.str_cerrar, null)
                .show();
    }

    // ────────── Helpers ──────────────────────────────────────────────────────

    private String getSelectedChip() {
        return etChipModel.getText() != null ? etChipModel.getText().toString().trim() : "";
    }

    private void saveSelectedChip(String chip) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(KEY_SELECTED_CHIP, chip).apply();
    }

    private String getSelectedDevice() {
        if (spinnerDevices != null && spinnerDevices.getSelectedItem() != null) {
            return spinnerDevices.getSelectedItem().toString();
        }
        return SUPPORTED_DEVICES[0];
    }

    private void saveSelectedDevice(String device) {
        getSharedPreferences(PREFS, MODE_PRIVATE).edit()
                .putString(KEY_SELECTED_DEVICE, device).apply();
    }

    private void log(String msg) {
        appendRawLogOnUi(msg + "\n");
    }

    private void appendRawLogOnUi(String text) {
        synchronized (consoleLines) {
            if (consoleLines.isEmpty()) {
                consoleLines.add(new StringBuilder());
                currentLineIndex = 0;
            }

            for (int i = 0; i < text.length(); i++) {
                char c = text.charAt(i);
                if (c == '\n') {
                    consoleLines.add(new StringBuilder());
                    currentLineIndex = consoleLines.size() - 1;
                    cursorAtStartOfLine = false;
                } else if (c == '\r') {
                    cursorAtStartOfLine = true;
                } else if (c == '\b') {
                    StringBuilder currentLine = consoleLines.get(currentLineIndex);
                    if (currentLine.length() > 0) {
                        currentLine.setLength(currentLine.length() - 1);
                    }
                } else {
                    StringBuilder currentLine = consoleLines.get(currentLineIndex);
                    if (cursorAtStartOfLine) {
                        currentLine.setLength(0);
                        cursorAtStartOfLine = false;
                    }
                    currentLine.append(c);
                }
            }

            while (consoleLines.size() > 1200) {
                consoleLines.remove(0);
                currentLineIndex--;
            }
            if (currentLineIndex < 0) currentLineIndex = 0;
        }

        if (!isLogUpdatePending) {
            isLogUpdatePending = true;
            logHandler.postDelayed(logUpdater, 80);
        }
    }

    private void executeMinipro(String... args) {
        miniproExecutor.executeCommand(args, usbController.getCurrentFd());
    }

    private void executeCustomCommand(String raw) {
        String command = raw.trim();
        if (command.startsWith("minipro ")) {
            command = command.substring(8).trim();
        } else if (command.equals("minipro")) {
            command = "";
        }

        if (command.isEmpty()) {
            executeMinipro();
            return;
        }

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
                    log(getString(R.string.str_rom_deleted));
                } else {
                    log(getString(R.string.str_rom_delete_error));
                }
            } else {
                log(getString(R.string.str_no_file_deleted));
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
        log("Architecture: " + Build.SUPPORTED_ABIS[0] + " | SDK: " + Build.VERSION.SDK_INT);
        log("Device: " + Build.MANUFACTURER + " " + Build.MODEL);

        File nativeLibDir = new File(getApplicationInfo().nativeLibraryDir);
        File miniproBin = new File(nativeLibDir, "libminipro_bin.so");
        log("minipro binary: " + (miniproBin.exists() ? "OK - " + miniproBin.length() / 1024 + " KB" : "NOT FOUND"));

        File libusb = new File(nativeLibDir, "libusb_1_0.so");
        log("patched libusb: " + (libusb.exists() ? "OK - " + libusb.length() / 1024 + " KB" : "NOT FOUND"));

        log("Native JNI Bridge: " + stringFromJNI());
    }

    private void importFile(Uri uri) {
        try (InputStream in = getContentResolver().openInputStream(uri)) {
            if (in == null) {
                throw new IllegalStateException(getString(R.string.str_err_open_file));
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
                log(getString(R.string.str_file_too_large, (int) (fileSize / 1024 / 1024)));
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
                log(getString(R.string.str_file_empty));
                return;
            }

            String sizeStr;
            if (totalWritten >= 1024 * 1024) {
                sizeStr = String.format(Locale.US, "%.2f MB", totalWritten / (1024.0 * 1024.0));
            } else {
                sizeStr = String.format(Locale.US, "%.1f KB", totalWritten / 1024.0);
            }

            log(getString(R.string.str_rom_imported, fileName, sizeStr));
            hasReadData = false;

            SharedPreferences.Editor editor = getSharedPreferences(PREFS, MODE_PRIVATE).edit();
            editor.putString("bios_source", getString(R.string.str_imported_label, fileName, sizeStr));
            editor.apply();
        } catch (Exception e) {
            log(getString(R.string.str_error) + ": " + e.getMessage());
        }
    }

    private void exportFileToUri(Uri uri) {
        File sourceFile = new File(getFilesDir(), lastReadFile);
        try (InputStream in = new java.io.FileInputStream(sourceFile);
             OutputStream out = getContentResolver().openOutputStream(uri)) {

            if (out == null) throw new Exception(getString(R.string.str_err_open_file));

            byte[] buffer = new byte[8192];
            int read;
            while ((read = in.read(buffer)) != -1) {
                out.write(buffer, 0, read);
            }
            log(getString(R.string.str_export_success, lastReadFile));
        } catch (Exception e) {
            log(getString(R.string.str_error) + ": " + e.getMessage());
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

    // ────────── Native methods ───────────────────────────────────────────────

    public native String stringFromJNI();
    public native void setUsbFd(int fd);
    public native void clearUsbFd();
}