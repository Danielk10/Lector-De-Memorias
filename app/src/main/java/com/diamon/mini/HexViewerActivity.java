package com.diamon.mini;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.InputFilter;
import android.text.InputType;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.style.ForegroundColorSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.OnBackPressedCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.diamon.mini.billing.BillingManager;
import com.diamon.mini.binary.PatchTransaction;
import com.diamon.mini.pro.ProAccessDialog;
import com.diamon.mini.pro.ProPassManager;
import com.diamon.mini.ui.IntegrityDialog;
import com.diamon.mini.vault.SafetyVaultManager;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Locale;

public class HexViewerActivity extends AppCompatActivity {

    public interface HexDataSource extends java.io.Closeable {
        long size();

        int read(long offset, byte[] dest, int destOffset, int length);
    }

    public static class MappedFileSource implements HexDataSource {
        private final java.io.RandomAccessFile raf;
        private final java.nio.channels.FileChannel channel;
        private final java.nio.MappedByteBuffer mappedBuffer;
        private final long size;

        public MappedFileSource(File file) throws Exception {
            this.raf = new java.io.RandomAccessFile(file, "r");
            this.channel = raf.getChannel();
            this.size = channel.size();
            this.mappedBuffer = channel.map(java.nio.channels.FileChannel.MapMode.READ_ONLY, 0, size);
        }

        @Override
        public long size() {
            return size;
        }

        @Override
        public int read(long offset, byte[] dest, int destOffset, int length) {
            if (offset >= size) return 0;
            int toRead = (int) Math.min((long) length, size - offset);
            synchronized (mappedBuffer) {
                mappedBuffer.position((int) offset);
                mappedBuffer.get(dest, destOffset, toRead);
            }
            return toRead;
        }

        @Override
        public void close() {
            try {
                channel.close();
            } catch (Exception ignored) {
            }
            try {
                raf.close();
            } catch (Exception ignored) {
            }
        }
    }

    public static class ByteArraySource implements HexDataSource {
        private final byte[] data;

        public ByteArraySource(byte[] data) {
            this.data = data != null ? data : new byte[0];
        }

        @Override
        public long size() {
            return data.length;
        }

        @Override
        public int read(long offset, byte[] dest, int destOffset, int length) {
            if (offset >= data.length) return 0;
            int toRead = (int) Math.min((long) length, data.length - offset);
            System.arraycopy(data, (int) offset, dest, destOffset, toRead);
            return toRead;
        }

        @Override
        public void close() {
        }
    }

    private static final String TAG = "HexViewerActivity";
    private static final String INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-5141499161332805/6545581216";
    private static final int MAX_EDIT_SIZE = 64 * 1024 * 1024;

    private TextView tvHexSummary;
    private RecyclerView recyclerHex;
    private HexAdapter hexAdapter;
    private InterstitialAd mInterstitialAd;
    private HexDataSource currentDataSource;

    private final PatchTransaction patchTransaction = new PatchTransaction();
    private BillingManager billingManager;
    private ProPassManager proPassManager;

    private String currentDisplayName = "rom.bin";
    private long baseAddress = 0L;
    private byte[] pendingSaveBytes;

    private final ActivityResultLauncher<String> saveCopyLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("application/octet-stream"),
            uri -> {
                if (uri != null) writePendingDump(uri);
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hex_viewer);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.str_visor_hexadecim);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        tvHexSummary = findViewById(R.id.tvHexSummary);
        recyclerHex = findViewById(R.id.recyclerHex);
        recyclerHex.setLayoutManager(new LinearLayoutManager(this));

        proPassManager = new ProPassManager(this);
        billingManager = new BillingManager(this, new BillingManager.BillingListener() {
            @Override
            public void onProStatusChanged(boolean isProPurchased) {
                runOnUiThread(() -> onProStateChanged());
            }

            @Override
            public void onPurchaseSuccess(String productId) {
                runOnUiThread(() -> onProStateChanged());
            }
        });

        patchTransaction.addListener(tx -> runOnUiThread(() -> {
            if (hexAdapter != null) hexAdapter.notifyDataSetChanged();
            invalidateOptionsMenu();
        }));

        initInterstitialAd();

        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override
            public void handleOnBackPressed() {
                handleExit();
            }
        });

        loadDataFromIntent();
    }

    private boolean isProActive() {
        return proPassManager != null && proPassManager.isProActive(billingManager);
    }

    private void onProStateChanged() {
        if (isProActive()) {
            mInterstitialAd = null;
        }
        invalidateOptionsMenu();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (billingManager != null) billingManager.destroy();
        if (currentDataSource != null) {
            try {
                currentDataSource.close();
            } catch (Exception ignored) {
            }
            currentDataSource = null;
        }
    }

    private void initInterstitialAd() {
        if (isProActive()) return;
        ConsentManager.requestConsent(this, canRequestAds -> {
            if (isFinishing() || isDestroyed() || !canRequestAds || isProActive()) {
                return;
            }
            MobileAds.initialize(this, initializationStatus -> runOnUiThread(this::loadInterstitialAd));
        });
    }

    private void loadInterstitialAd() {
        if (!ConsentManager.canRequestAds() || isProActive()) {
            return;
        }
        AdRequest adRequest = new AdRequest.Builder().build();
        InterstitialAd.load(
                this,
                INTERSTITIAL_AD_UNIT_ID,
                adRequest,
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        mInterstitialAd = interstitialAd;
                        Log.d(TAG, "Interstitial loaded.");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        mInterstitialAd = null;
                        Log.e(TAG, "Interstitial failed: " + loadAdError.getMessage());
                    }
                });
    }

    private void handleExit() {
        if (isProActive() || mInterstitialAd == null || !ConsentManager.canRequestAds()) {
            finish();
            return;
        }
        mInterstitialAd.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                mInterstitialAd = null;
                finish();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                mInterstitialAd = null;
                finish();
            }
        });
        mInterstitialAd.show(this);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == android.R.id.home) {
            handleExit();
            return true;
        }
        if (id == R.id.action_hex_undo) {
            if (patchTransaction.undo()) {
                Toast.makeText(this, R.string.hex_undo_done, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.hex_nothing_to_undo, Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        if (id == R.id.action_hex_redo) {
            if (patchTransaction.redo()) {
                Toast.makeText(this, R.string.hex_redo_done, Toast.LENGTH_SHORT).show();
            } else {
                Toast.makeText(this, R.string.hex_nothing_to_redo, Toast.LENGTH_SHORT).show();
            }
            return true;
        }
        if (id == R.id.action_hex_save) {
            startSaveCopy();
            return true;
        }
        if (id == R.id.action_hex_apply_rom) {
            confirmApplyToRom();
            return true;
        }
        if (id == R.id.action_hex_integrity) {
            if (!isProActive()) {
                ProAccessDialog.show(this, billingManager, proPassManager, () -> {
                    onProStateChanged();
                    if (isProActive()) showDumpIntegrity();
                });
                return true;
            }
            showDumpIntegrity();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showDumpIntegrity() {
        if (currentDataSource == null) {
            Toast.makeText(this, R.string.str_err_no_data_visualize,
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (currentDataSource.size() > MAX_EDIT_SIZE) {
            Toast.makeText(this, R.string.hex_too_large, Toast.LENGTH_LONG).show();
            return;
        }
        final String label = currentDisplayName;
        new Thread(() -> {
            byte[] dump = readAllWithPatches();
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (dump == null || dump.length == 0) {
                    Toast.makeText(this, R.string.str_err_no_data_visualize,
                            Toast.LENGTH_LONG).show();
                    return;
                }
                IntegrityDialog.show(this, label, dump);
            });
        }, "HexIntegrity").start();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.hex_viewer_menu, menu);
        return true;
    }

    @Override
    public boolean onPrepareOptionsMenu(Menu menu) {
        MenuItem undo = menu.findItem(R.id.action_hex_undo);
        if (undo != null) undo.setEnabled(patchTransaction.canUndo());
        MenuItem redo = menu.findItem(R.id.action_hex_redo);
        if (redo != null) redo.setEnabled(patchTransaction.canRedo());

        MenuItem save = menu.findItem(R.id.action_hex_save);
        if (save != null) {
            int modified = patchTransaction.getModifiedCount();
            save.setTitle(modified > 0
                    ? getString(R.string.hex_menu_save_count, modified)
                    : getString(R.string.hex_menu_save));
            save.setEnabled(currentDataSource != null);
        }
        MenuItem apply = menu.findItem(R.id.action_hex_apply_rom);
        if (apply != null) {
            apply.setEnabled(currentDataSource != null
                    && patchTransaction.getModifiedCount() > 0);
        }
        MenuItem integrity = menu.findItem(R.id.action_hex_integrity);
        if (integrity != null) integrity.setEnabled(currentDataSource != null);
        return super.onPrepareOptionsMenu(menu);
    }

    private void loadDataFromIntent() {
        Uri fileUri = getIntent().getData();
        if (fileUri != null) {
            loadFromUri(fileUri);
        } else {
            loadDefaultRom();
        }
    }

    private void loadDefaultRom() {
        String biosSource = getSharedPreferences("minipro_prefs", MODE_PRIVATE)
                .getString("bios_source", null);

        String trackedFile = getSharedPreferences("minipro_prefs", MODE_PRIVATE)
                .getString("last_read_file", "rom.bin");
        File dataFile = new File(getFilesDir(), trackedFile);
        if (!dataFile.exists()) {
            dataFile = new File(getFilesDir(), "rom.bin");
        }
        if (!dataFile.exists()) {
            tvHexSummary.setText(R.string.str_err_no_data_visualize);
            return;
        }

        try {
            currentDisplayName = dataFile.getName();
            String fileName = dataFile.getName();
            if (fileName.toLowerCase().endsWith(".hex")) {
                byte[] data = readFileToBytes(dataFile);
                parseIntelHex(data, biosSource);
            } else {
                MappedFileSource source = new MappedFileSource(dataFile);
                displayDataSource(source, fileName, biosSource);
            }
        } catch (Exception e) {
            tvHexSummary.setText(getString(R.string.str_err_load_data, e.getMessage()));
        }
    }

    private void loadFromUri(Uri uri) {
        try {
            byte[] data = readUriToBytes(uri);
            String fileName = getFileName(uri);
            currentDisplayName = fileName;
            String source = fileName;

            if (fileName.toLowerCase().endsWith(".hex")) {
                parseIntelHex(data, source);
            } else {
                displayDataSource(new ByteArraySource(data), fileName, source);
            }
        } catch (Exception e) {
            tvHexSummary.setText(getString(R.string.str_err_load_data, e.getMessage()));
        }
    }

    private byte[] readUriToBytes(Uri uri) throws Exception {
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) throw new IllegalStateException(getString(R.string.str_err_open_file));
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            int nRead;
            byte[] data = new byte[16384];
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            return buffer.toByteArray();
        }
    }

    private byte[] readFileToBytes(File file) throws Exception {
        try (InputStream is = new java.io.FileInputStream(file)) {
            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] data = new byte[16384];
            int nRead;
            while ((nRead = is.read(data, 0, data.length)) != -1) {
                buffer.write(data, 0, nRead);
            }
            return buffer.toByteArray();
        }
    }

    private String getFileName(Uri uri) {
        String name = "archivo";
        try {
            android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int idx = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                if (idx >= 0) name = cursor.getString(idx);
                cursor.close();
            }
        } catch (Exception ignored) {
        }
        return name;
    }

    private void displayDataSource(HexDataSource source, String name, String biosSource) {
        if (currentDataSource != null && currentDataSource != source) {
            try {
                currentDataSource.close();
            } catch (Exception ignored) {
            }
        }
        currentDataSource = source;
        patchTransaction.clear();
        baseAddress = 0L;
        currentDisplayName = name;

        String summary = getString(R.string.str_binary_summary, name, source.size());
        if (biosSource != null) {
            summary += getString(R.string.str_source_label, biosSource);
        }
        tvHexSummary.setText(summary);
        hexAdapter = new HexAdapter(source, 0, patchTransaction, this::onByteClicked);
        recyclerHex.setAdapter(hexAdapter);
        invalidateOptionsMenu();
    }

    private void parseIntelHex(byte[] hexData, String biosSource) {
        try {
            String content = new String(hexData);
            String[] lines = content.split("\\r?\\n");

            long minAddr = Long.MAX_VALUE;
            long maxAddr = 0;
            int upperAddress = 0;

            for (String line : lines) {
                line = line.trim();
                if (!line.startsWith(":") || line.length() < 11) continue;
                int byteCount = Integer.parseInt(line.substring(1, 3), 16);
                int address = Integer.parseInt(line.substring(3, 7), 16);
                int type = Integer.parseInt(line.substring(7, 9), 16);
                if (type == 0x00) {
                    long absolute = (long) upperAddress + address;
                    minAddr = Math.min(minAddr, absolute);
                    maxAddr = Math.max(maxAddr, absolute + byteCount);
                } else if (type == 0x04 && line.length() >= 15) {
                    upperAddress = Integer.parseInt(line.substring(9, 13), 16) << 16;
                } else if (type == 0x02 && line.length() >= 15) {
                    upperAddress = Integer.parseInt(line.substring(9, 13), 16) << 4;
                } else if (type == 0x01) {
                    break;
                }
            }

            if (minAddr == Long.MAX_VALUE) {
                tvHexSummary.setText(R.string.str_err_hex_no_records);
                return;
            }

            long bufferSize = maxAddr - minAddr;
            if (bufferSize > 32L * 1024 * 1024) {
                tvHexSummary.setText(getString(R.string.str_err_hex_too_large,
                        String.valueOf(bufferSize / 1024 / 1024)));
                return;
            }

            byte[] binBuffer = new byte[(int) bufferSize];
            java.util.Arrays.fill(binBuffer, (byte) 0xFF);

            upperAddress = 0;
            for (String line : lines) {
                line = line.trim();
                if (!line.startsWith(":") || line.length() < 11) continue;
                int byteCount = Integer.parseInt(line.substring(1, 3), 16);
                int address = Integer.parseInt(line.substring(3, 7), 16);
                int type = Integer.parseInt(line.substring(7, 9), 16);
                if (type == 0x00) {
                    long absolute = (long) upperAddress + address;
                    int offset = (int) (absolute - minAddr);
                    for (int i = 0; i < byteCount; i++) {
                        if (offset + i < binBuffer.length) {
                            binBuffer[offset + i] = (byte) Integer.parseInt(
                                    line.substring(9 + i * 2, 11 + i * 2), 16);
                        }
                    }
                } else if (type == 0x04 && line.length() >= 15) {
                    upperAddress = Integer.parseInt(line.substring(9, 13), 16) << 16;
                } else if (type == 0x02 && line.length() >= 15) {
                    upperAddress = Integer.parseInt(line.substring(9, 13), 16) << 4;
                } else if (type == 0x01) {
                    break;
                }
            }

            String summary = getString(R.string.str_hex_detected,
                    (int) minAddr, (int) (maxAddr - 1));
            if (biosSource != null) {
                summary += getString(R.string.str_source_label, biosSource);
            }
            tvHexSummary.setText(summary);

            if (currentDataSource != null) {
                try {
                    currentDataSource.close();
                } catch (Exception ignored) {
                }
            }
            ByteArraySource source = new ByteArraySource(binBuffer);
            currentDataSource = source;
            patchTransaction.clear();
            baseAddress = minAddr;
            hexAdapter = new HexAdapter(source, (int) minAddr, patchTransaction,
                    this::onByteClicked);
            recyclerHex.setAdapter(hexAdapter);
            invalidateOptionsMenu();
        } catch (Exception e) {
            tvHexSummary.setText(getString(R.string.str_err_parse_hex, e.getMessage()));
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    // =========================================================================
    // Pro writable editor
    // =========================================================================

    private void onByteClicked(long relativeOffset) {
        if (!isProActive()) {
            ProAccessDialog.show(this, billingManager, proPassManager, () -> {
                onProStateChanged();
                if (isProActive()) {
                    if (currentDataSource != null && currentDataSource.size() > MAX_EDIT_SIZE) {
                        Toast.makeText(this, R.string.hex_too_large, Toast.LENGTH_LONG).show();
                    } else {
                        showByteEditor(relativeOffset);
                    }
                }
            });
            return;
        }
        if (currentDataSource != null && currentDataSource.size() > MAX_EDIT_SIZE) {
            Toast.makeText(this, R.string.hex_too_large, Toast.LENGTH_LONG).show();
            return;
        }
        showByteEditor(relativeOffset);
    }

    private byte readByte(long relativeOffset) {
        if (currentDataSource == null) return (byte) 0xFF;
        byte[] single = new byte[1];
        int read = currentDataSource.read(relativeOffset, single, 0, 1);
        return read > 0 ? single[0] : (byte) 0xFF;
    }

    private void showByteEditor(long relativeOffset) {
        byte original = readByte(relativeOffset);
        byte currentValue = patchTransaction.getByte(relativeOffset, original);
        long absolute = baseAddress + relativeOffset;

        float density = getResources().getDisplayMetrics().density;
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        int padding = Math.round(18 * density);
        layout.setPadding(padding, padding / 2, padding, 0);

        TextView info = new TextView(this);
        info.setText(getString(R.string.hex_edit_current,
                original & 0xFF, original & 0xFF, currentValue & 0xFF));
        info.setTextSize(13);
        info.setTextColor(0xFFB0BEC5);
        info.setPadding(0, 0, 0, Math.round(10 * density));
        layout.addView(info);

        EditText input = new EditText(this);
        input.setText(String.format(Locale.US, "%02X", currentValue & 0xFF));
        input.setHint(R.string.hex_edit_hint);
        input.setHintTextColor(0xFF757575);
        input.setTextColor(0xFFFFFFFF);
        input.setBackgroundResource(R.drawable.bg_input_field);
        input.setInputType(InputType.TYPE_CLASS_TEXT
                | InputType.TYPE_TEXT_FLAG_CAP_CHARACTERS);
        input.setFilters(new InputFilter[]{new InputFilter.LengthFilter(2)});
        input.setSelectAllOnFocus(true);
        input.setPadding(padding / 2, padding / 3, padding / 2, padding / 3);
        layout.addView(input);

        ScrollView scrollView = new ScrollView(this);
        scrollView.addView(layout);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(getString(R.string.hex_edit_title, absolute))
                .setView(scrollView)
                .setPositiveButton(R.string.str_guardar, null)
                .setNegativeButton(R.string.str_cancelar, null)
                .create();

        dialog.setOnShowListener(d -> dialog.getButton(AlertDialog.BUTTON_POSITIVE)
                .setOnClickListener(v -> {
                    String raw = input.getText() == null
                            ? "" : input.getText().toString().trim();
                    raw = raw.replace("0x", "").replace("0X", "").replace(" ", "");
                    if (!raw.matches("[0-9a-fA-F]{1,2}")) {
                        Toast.makeText(this, R.string.hex_edit_invalid,
                                Toast.LENGTH_SHORT).show();
                        return;
                    }
                    int value = Integer.parseInt(raw, 16);
                    boolean changed = patchTransaction.modifyByte(
                            relativeOffset, original, (byte) value, "HEX edit");
                    if (changed) {
                        Toast.makeText(this,
                                getString(R.string.hex_edit_changed, absolute, value),
                                Toast.LENGTH_SHORT).show();
                    }
                    dialog.dismiss();
                }));
        dialog.show();
    }

    private byte[] readAllWithPatches() {
        HexDataSource source = currentDataSource;
        if (source == null) return null;
        long size = source.size();
        if (size > MAX_EDIT_SIZE) return null;
        try {
            byte[] dump = new byte[(int) size];
            int total = 0;
            while (total < size) {
                int read = source.read(total, dump, total, (int) (size - total));
                if (read <= 0) break;
                total += read;
            }
            if (total == 0) return dump;
            return patchTransaction.applyTo(dump);
        } catch (Exception e) {
            return null;
        }
    }

    private String suggestedSaveName() {
        String base = currentDisplayName != null ? currentDisplayName : "rom.bin";
        int dot = base.lastIndexOf('.');
        if (dot > 0) base = base.substring(0, dot);
        return base + "_edited.bin";
    }

    private void startSaveCopy() {
        if (currentDataSource == null) {
            Toast.makeText(this, R.string.str_err_no_data_visualize,
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (currentDataSource.size() > MAX_EDIT_SIZE) {
            Toast.makeText(this, R.string.hex_too_large, Toast.LENGTH_LONG).show();
            return;
        }
        new Thread(() -> {
            byte[] dump = readAllWithPatches();
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (dump == null || dump.length == 0) {
                    Toast.makeText(this, R.string.str_err_no_data_visualize,
                            Toast.LENGTH_LONG).show();
                    return;
                }
                pendingSaveBytes = dump;
                saveCopyLauncher.launch(suggestedSaveName());
            });
        }, "HexSaveCopy").start();
    }

    private void writePendingDump(Uri uri) {
        if (pendingSaveBytes == null) return;
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (out == null) throw new IllegalStateException(getString(R.string.str_err_open_file));
            out.write(pendingSaveBytes);
            out.flush();
            Toast.makeText(this,
                    getString(R.string.hex_saved, uri.getLastPathSegment()),
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.hex_save_error, e.getMessage()),
                    Toast.LENGTH_LONG).show();
        } finally {
            pendingSaveBytes = null;
        }
    }

    private void confirmApplyToRom() {
        int modified = patchTransaction.getModifiedCount();
        if (modified <= 0) {
            Toast.makeText(this, R.string.hex_no_changes, Toast.LENGTH_SHORT).show();
            return;
        }
        new AlertDialog.Builder(this)
                .setTitle(R.string.hex_apply_confirm_title)
                .setMessage(getString(R.string.hex_apply_confirm_msg, modified))
                .setPositiveButton(R.string.hex_menu_apply_rom, (d, w) -> applyToRom())
                .setNegativeButton(R.string.str_cancelar, null)
                .show();
    }

    private void applyToRom() {
        if (currentDataSource == null) {
            Toast.makeText(this, R.string.str_err_no_data_visualize,
                    Toast.LENGTH_LONG).show();
            return;
        }
        if (currentDataSource.size() > MAX_EDIT_SIZE) {
            Toast.makeText(this, R.string.hex_too_large, Toast.LENGTH_LONG).show();
            return;
        }

        new Thread(() -> {
            byte[] dump = readAllWithPatches();
            if (dump == null || dump.length == 0) {
                runOnUiThread(() -> {
                    if (isFinishing() || isDestroyed()) return;
                    Toast.makeText(this, R.string.str_err_no_data_visualize,
                            Toast.LENGTH_LONG).show();
                });
                return;
            }

            Exception error = null;
            try {
                File dir = getFilesDir();
                File rom = new File(dir, "rom.bin");
                File temp = new File(dir, "rom_hex_edit.tmp");
                File old = new File(dir, "rom_hex_old.tmp");

                if (isProActive()) {
                    try {
                        SafetyVaultManager.getInstance(getApplicationContext())
                                .createSnapshotIfChanged(rom, "pre-hex-apply");
                    } catch (Exception ignored) {
                    }
                }

                try (FileOutputStream out = new FileOutputStream(temp)) {
                    out.write(dump);
                    out.flush();
                    out.getFD().sync();
                }

                if (old.exists() && !old.delete()) {
                    throw new java.io.IOException("Could not clear old temporary file");
                }
                boolean hadOld = rom.exists();
                if (hadOld && !rom.renameTo(old)) {
                    throw new java.io.IOException("Could not move the previous rom.bin");
                }
                if (!temp.renameTo(rom)) {
                    if (hadOld) old.renameTo(rom);
                    throw new java.io.IOException("Could not replace rom.bin");
                }
                if (old.exists()) old.delete();
            } catch (Exception e) {
                error = e;
            }

            final Exception failure = error;
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                if (failure != null) {
                    Toast.makeText(this,
                            getString(R.string.hex_apply_error, failure.getMessage()),
                            Toast.LENGTH_LONG).show();
                    return;
                }
                getSharedPreferences("minipro_prefs", MODE_PRIVATE).edit()
                        .putString("last_read_file", "rom.bin")
                        .putString("bios_source", getString(R.string.hex_source_edited))
                        .apply();
                Toast.makeText(this, R.string.hex_applied, Toast.LENGTH_LONG).show();
                loadDefaultRom();
            });
        }, "HexApplyToRom").start();
    }

    // =========================================================================
    // Hex adapter
    // =========================================================================

    interface OnByteClickListener {
        void onByteClick(long relativeOffset);
    }

    static class HexAdapter extends RecyclerView.Adapter<HexAdapter.HexViewHolder> {
        private final HexDataSource dataSource;
        private final int startAddress;
        private final PatchTransaction patchTransaction;
        private final OnByteClickListener byteClickListener;

        HexAdapter(HexDataSource dataSource, int startAddress,
                   PatchTransaction patchTransaction,
                   OnByteClickListener byteClickListener) {
            this.dataSource = dataSource;
            this.startAddress = startAddress;
            this.patchTransaction = patchTransaction;
            this.byteClickListener = byteClickListener;
        }

        @NonNull
        @Override
        public HexViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_hex_row, parent, false);
            return new HexViewHolder(view, byteClickListener);
        }

        @Override
        public void onBindViewHolder(@NonNull HexViewHolder holder, int position) {
            long rowStart = (long) position * 16;
            long currentAddr = (long) startAddress + rowStart;
            byte[] rowBuf = new byte[16];
            int length = dataSource.read(rowStart, rowBuf, 0, 16);

            holder.currentRowStart = rowStart;
            holder.currentLength = length;

            SpannableStringBuilder hexBuilder = new SpannableStringBuilder();
            SpannableStringBuilder asciiBuilder = new SpannableStringBuilder();
            boolean rowModified = false;

            for (int i = 0; i < 16; i++) {
                if (i < length) {
                    byte original = rowBuf[i];
                    byte value = patchTransaction.getByte(rowStart + i, original);
                    boolean modified = value != original;
                    if (modified) rowModified = true;

                    int start = hexBuilder.length();
                    hexBuilder.append(String.format(Locale.US, "%02X ", value));
                    hexBuilder.setSpan(
                            new ForegroundColorSpan(modified ? 0xFFFF5252 : 0xFFE0E0E0),
                            start, hexBuilder.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

                    int asciiStart = asciiBuilder.length();
                    asciiBuilder.append(value >= 32 && value <= 126 ? (char) value : '.');
                    asciiBuilder.setSpan(
                            new ForegroundColorSpan(modified ? 0xFFFF5252 : 0xFF4CAF50),
                            asciiStart, asciiBuilder.length(),
                            Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                } else {
                    hexBuilder.append("   ");
                    asciiBuilder.append(" ");
                }
            }

            holder.tvAddress.setText(String.format(Locale.US, "%08X", currentAddr));
            holder.tvAddress.setTextColor(rowModified ? 0xFFFF9800 : 0xFF2196F3);
            holder.tvHex.setText(hexBuilder);
            holder.tvAscii.setText(asciiBuilder);
        }

        @Override
        public int getItemCount() {
            return (int) Math.ceil((double) dataSource.size() / 16.0);
        }

        static class HexViewHolder extends RecyclerView.ViewHolder {
            TextView tvAddress, tvHex, tvAscii;
            long currentRowStart = 0L;
            int currentLength = 0;

            HexViewHolder(View itemView, OnByteClickListener listener) {
                super(itemView);
                tvAddress = itemView.findViewById(R.id.tvAddress);
                tvHex = itemView.findViewById(R.id.tvHex);
                tvAscii = itemView.findViewById(R.id.tvAscii);

                tvHex.setOnTouchListener((v, event) -> {
                    if (event.getAction() != MotionEvent.ACTION_UP) return true;
                    int charPos = tvHex.getOffsetForPosition(event.getX(), event.getY());
                    int index = Math.min(15, Math.max(0, charPos / 3));
                    if (index < currentLength && listener != null) {
                        listener.onByteClick(currentRowStart + index);
                    }
                    return true;
                });

                tvAscii.setOnTouchListener((v, event) -> {
                    if (event.getAction() != MotionEvent.ACTION_UP) return true;
                    int charPos = tvAscii.getOffsetForPosition(event.getX(), event.getY());
                    if (charPos >= 0 && charPos < currentLength && listener != null) {
                        listener.onByteClick(currentRowStart + charPos);
                    }
                    return true;
                });
            }
        }
    }
}
