package com.diamon.mini;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.diamon.mini.billing.BillingManager;
import com.diamon.mini.binary.DiffAnalyzer;
import com.diamon.mini.pro.ProAccessDialog;
import com.diamon.mini.pro.ProPassManager;
import com.diamon.mini.ui.ReportDialog;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class HexDiffActivity extends AppCompatActivity {

    private TextView tvDiffSummary, tvDiffStats;
    private RecyclerView recyclerDiff;
    private Button btnLoadFile1, btnLoadFile2;

    private byte[] dataA = null;
    private byte[] dataB = null;
    private String nameA = "";
    private String nameB = "";

    private BillingManager billingManager;
    private ProPassManager proPassManager;
    private String pendingReportText;

    private final ActivityResultLauncher<String> reportSaveLauncher = registerForActivityResult(
            new ActivityResultContracts.CreateDocument("text/plain"),
            uri -> {
                if (uri != null) writePendingReport(uri);
            });

    private final ActivityResultLauncher<Intent> file1Launcher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        try {
                            dataA = readUriToBytes(uri);
                            nameA = getFileName(uri);
                            if (nameA.toLowerCase().endsWith(".hex")) {
                                dataA = parseIntelHexToBinary(dataA);
                            }
                            btnLoadFile1.setText(getString(R.string.str_file_a, nameA));
                            tryCompare();
                        } catch (Exception e) {
                            tvDiffSummary.setText(getString(R.string.str_err_load_file_a, e.getMessage()));
                        }
                    }
                }
            });

    private final ActivityResultLauncher<Intent> file2Launcher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                    Uri uri = result.getData().getData();
                    if (uri != null) {
                        try {
                            dataB = readUriToBytes(uri);
                            nameB = getFileName(uri);
                            if (nameB.toLowerCase().endsWith(".hex")) {
                                dataB = parseIntelHexToBinary(dataB);
                            }
                            btnLoadFile2.setText(getString(R.string.str_file_b, nameB));
                            tryCompare();
                        } catch (Exception e) {
                            tvDiffSummary.setText(getString(R.string.str_err_load_file_b, e.getMessage()));
                        }
                    }
                }
            });

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_hex_diff);

        nameA = getString(R.string.str_archivo_a);
        nameB = getString(R.string.str_archivo_b);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(R.string.str_comparar_hex);
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        proPassManager = new ProPassManager(this);
        billingManager = new BillingManager(this, new BillingManager.BillingListener() {
            @Override
            public void onProStatusChanged(boolean isProPurchased) {
                runOnUiThread(() -> invalidateOptionsMenu());
            }

            @Override
            public void onPurchaseSuccess(String productId) {
                runOnUiThread(() -> invalidateOptionsMenu());
            }
        });

        tvDiffSummary = findViewById(R.id.tvDiffSummary);
        tvDiffStats = findViewById(R.id.tvDiffStats);
        recyclerDiff = findViewById(R.id.recyclerDiff);
        recyclerDiff.setLayoutManager(new LinearLayoutManager(this));
        btnLoadFile1 = findViewById(R.id.btnLoadFile1);
        btnLoadFile2 = findViewById(R.id.btnLoadFile2);

        // Pre-cargar rom.bin automáticamente si existe
        String lastRead = getSharedPreferences("minipro_prefs", MODE_PRIVATE)
                .getString("last_read_file", "rom.bin");
        File romFile = new File(getFilesDir(), lastRead);
        if (!romFile.exists()) {
            romFile = new File(getFilesDir(), "rom.bin");
        }
        if (romFile.exists()) {
            try {
                dataA = readFileToBytes(romFile);
                nameA = romFile.getName() + " — " + getString(R.string.str_current_memory);
                if (romFile.getName().toLowerCase().endsWith(".hex")) {
                    dataA = parseIntelHexToBinary(dataA);
                }
                btnLoadFile1.setText(getString(R.string.str_file_a, nameA));
            } catch (Exception ignored) {}
        }

        btnLoadFile1.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            file1Launcher.launch(intent);
        });

        btnLoadFile2.setOnClickListener(v -> {
            Intent intent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType("*/*");
            file2Launcher.launch(intent);
        });

        if (dataA != null) {
            tryCompare();
        }
    }

    private boolean isProActive() {
        return proPassManager != null && proPassManager.isProActive(billingManager);
    }

    private void requireProThen(Runnable action) {
        if (isProActive()) {
            action.run();
        } else {
            ProAccessDialog.show(this, billingManager, proPassManager, () -> {
                invalidateOptionsMenu();
                action.run();
            });
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (billingManager != null) billingManager.destroy();
    }

    // ────────── Menu Pro ─────────────────────────────────────────────────────

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.hex_diff_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_diff_analysis) {
            requireProThen(this::showSectorAnalysis);
            return true;
        }
        if (id == R.id.action_diff_report) {
            requireProThen(this::exportComparisonReport);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void showSectorAnalysis() {
        if (dataA == null || dataB == null) {
            Toast.makeText(this, R.string.diff_analysis_no_data, Toast.LENGTH_LONG).show();
            return;
        }

        final byte[] a = dataA;
        final byte[] b = dataB;
        new Thread(() -> {
            DiffAnalyzer.Result result = DiffAnalyzer.analyze(a, b);
            String body = buildAnalysisText(result);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                ReportDialog.show(this, getString(R.string.diff_analysis_title), body, null);
            });
        }, "DiffAnalysis").start();
    }

    private String buildAnalysisText(DiffAnalyzer.Result result) {
        StringBuilder sb = new StringBuilder();
        sb.append(getString(R.string.diff_analysis_header)).append("\n\n");
        sb.append(getString(R.string.str_compare_summary,
                nameA, dataA.length, nameB, dataB.length)).append("\n\n");

        if (result.isIdentical()) {
            sb.append(getString(R.string.diff_analysis_identical)).append('\n');
            return sb.toString();
        }

        sb.append(getString(R.string.diff_analysis_stats,
                result.getDiffCount(), result.getMaxLength(), result.getPercent()))
                .append('\n');
        sb.append(getString(R.string.diff_analysis_first_last,
                String.format(Locale.US, "0x%08X", result.getFirstDiff()),
                String.format(Locale.US, "0x%08X", result.getLastDiff())))
                .append("\n\n");

        sb.append(getString(R.string.diff_analysis_sectors_header,
                DiffAnalyzer.SECTOR_SIZE / 1024,
                result.getChangedSectorCount(),
                result.getTotalSectors())).append('\n');

        int shown = 0;
        for (DiffAnalyzer.SectorDiff sector : result.getChangedSectors()) {
            if (shown >= 60) {
                sb.append(getString(R.string.diff_analysis_truncated,
                        result.getChangedSectors().size() - shown)).append('\n');
                break;
            }
            double pct = (sector.getDiffCount() * 100.0)
                    / Math.max(1L, sector.getEnd() - sector.getStart());
            sb.append(getString(R.string.diff_analysis_sector_line,
                    sector.getIndex(),
                    String.format(Locale.US, "0x%08X", sector.getStart()),
                    String.format(Locale.US, "0x%08X", sector.getEnd() - 1),
                    sector.getDiffCount(), pct)).append('\n');
            shown++;
        }
        return sb.toString();
    }

    private void exportComparisonReport() {
        if (dataA == null || dataB == null) {
            Toast.makeText(this, R.string.diff_analysis_no_data, Toast.LENGTH_LONG).show();
            return;
        }

        Toast.makeText(this, R.string.diff_report_preparing, Toast.LENGTH_SHORT).show();
        final byte[] a = dataA;
        final byte[] b = dataB;

        new Thread(() -> {
            DiffAnalyzer.Result result = DiffAnalyzer.analyze(a, b);
            String report = DiffAnalyzer.buildReport(
                    getApplicationContext(), nameA, a, nameB, b, result);
            runOnUiThread(() -> {
                if (isFinishing() || isDestroyed()) return;
                final String suggested = buildReportFileName();
                ReportDialog.show(this, getString(R.string.diff_analysis_title), report,
                        () -> {
                            pendingReportText = report;
                            reportSaveLauncher.launch(suggested);
                        });
            });
        }, "DiffReport").start();
    }

    private String buildReportFileName() {
        String base = nameB != null && !nameB.isEmpty() ? nameB : "diff";
        int dot = base.lastIndexOf('.');
        if (dot > 0) base = base.substring(0, dot);
        base = base.replaceAll("[^a-zA-Z0-9_.-]", "_");
        String date = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US).format(new Date());
        return "eeprom_diff_" + base + "_" + date + ".txt";
    }

    private void writePendingReport(Uri uri) {
        if (pendingReportText == null) return;
        try (OutputStream out = getContentResolver().openOutputStream(uri)) {
            if (out == null) throw new IllegalStateException(getString(R.string.str_err_open_file));
            out.write(pendingReportText.getBytes(StandardCharsets.UTF_8));
            out.flush();
            Toast.makeText(this,
                    getString(R.string.diff_report_saved, uri.getLastPathSegment()),
                    Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, getString(R.string.diff_report_save_error, e.getMessage()),
                    Toast.LENGTH_LONG).show();
        } finally {
            pendingReportText = null;
        }
    }

    private void tryCompare() {
        if (dataA == null || dataB == null) {
            tvDiffSummary.setText(R.string.str_load_both_files);
            return;
        }

        int maxLen = Math.max(dataA.length, dataB.length);
        int diffCount = 0;

        for (int i = 0; i < maxLen; i++) {
            byte a = i < dataA.length ? dataA[i] : (byte) 0xFF;
            byte b = i < dataB.length ? dataB[i] : (byte) 0xFF;
            if (a != b) diffCount++;
        }

        tvDiffSummary.setText(getString(R.string.str_compare_summary, nameA, dataA.length, nameB, dataB.length));

        tvDiffStats.setVisibility(View.VISIBLE);
        if (diffCount == 0) {
            tvDiffStats.setText(R.string.str_files_identical);
            tvDiffStats.setTextColor(0xFF4CAF50); // Verde
        } else {
            double pct = (diffCount * 100.0) / maxLen;
            tvDiffStats.setText(getString(R.string.str_files_different, diffCount, pct, maxLen));
            tvDiffStats.setTextColor(0xFFFF5252); // Rojo
        }

        recyclerDiff.setAdapter(new DiffAdapter(dataA, dataB));
    }

    private byte[] readUriToBytes(Uri uri) throws Exception {
        try (InputStream is = getContentResolver().openInputStream(uri)) {
            if (is == null) {
                throw new IllegalStateException(getString(R.string.str_err_open_file));
            }

            android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null && cursor.moveToFirst()) {
                int sizeIdx = cursor.getColumnIndex(android.provider.OpenableColumns.SIZE);
                if (sizeIdx >= 0) {
                    long size = cursor.getLong(sizeIdx);
                    if (size > 16 * 1024 * 1024) {
                        cursor.close();
                        throw new IllegalArgumentException(getString(R.string.str_file_too_large, 16));
                    }
                }
                cursor.close();
            }

            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
            byte[] buf = new byte[16384];
            int nRead;
            long totalRead = 0;
            while ((nRead = is.read(buf)) != -1) {
                totalRead += nRead;
                if (totalRead > 16 * 1024 * 1024) {
                    throw new IllegalArgumentException(getString(R.string.str_file_too_large, 16));
                }
                buffer.write(buf, 0, nRead);
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
        } catch (Exception ignored) {}
        return name;
    }

    private byte[] parseIntelHexToBinary(byte[] hexData) {
        try {
            String content = new String(hexData, StandardCharsets.UTF_8);
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

            if (minAddr == Long.MAX_VALUE || maxAddr <= minAddr) {
                return hexData;
            }

            long bufferSize = maxAddr - minAddr;
            if (bufferSize > 32L * 1024 * 1024) {
                return hexData;
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
            return binBuffer;
        } catch (Exception e) {
            return hexData;
        }
    }

    @Override
    public boolean onSupportNavigateUp() {
        getOnBackPressedDispatcher().onBackPressed();
        return true;
    }

    // Adapter que muestra filas HEX con diferencias resaltadas
    static class DiffAdapter extends RecyclerView.Adapter<DiffAdapter.DiffViewHolder> {
        private final byte[] dataA;
        private final byte[] dataB;
        private final int maxLen;

        DiffAdapter(byte[] dataA, byte[] dataB) {
            this.dataA = dataA;
            this.dataB = dataB;
            this.maxLen = Math.max(dataA.length, dataB.length);
        }

        @NonNull
        @Override
        public DiffViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_hex_row, parent, false);
            return new DiffViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull DiffViewHolder holder, int position) {
            int rowStart = position * 16;
            int length = Math.min(16, maxLen - rowStart);

            StringBuilder hexBuilder = new StringBuilder();
            StringBuilder asciiBuilder = new StringBuilder();
            boolean rowHasDiff = false;

            for (int i = 0; i < 16; i++) {
                if (i < length) {
                    int idx = rowStart + i;
                    byte a = idx < dataA.length ? dataA[idx] : (byte) 0xFF;
                    byte b = idx < dataB.length ? dataB[idx] : (byte) 0xFF;
                    boolean isDiff = (a != b);
                    if (isDiff) rowHasDiff = true;

                    if (isDiff) {
                        hexBuilder.append(String.format(Locale.US, "%02X→%02X ", a & 0xFF, b & 0xFF));
                    } else {
                        hexBuilder.append(String.format(Locale.US, "%02X     ", a & 0xFF));
                    }

                    if (a >= 32 && a <= 126) {
                        asciiBuilder.append((char) a);
                    } else {
                        asciiBuilder.append(".");
                    }
                } else {
                    hexBuilder.append("       ");
                    asciiBuilder.append(" ");
                }
            }

            holder.tvAddress.setText(String.format(Locale.US, "%08X", rowStart));
            holder.tvHex.setText(hexBuilder.toString());
            holder.tvAscii.setText(asciiBuilder.toString());

            // Resaltar diferencias
            if (rowHasDiff) {
                holder.tvHex.setTextColor(0xFFFF5252); // Rojo
                holder.tvAddress.setTextColor(0xFFFF9800); // Naranja
            } else {
                holder.tvHex.setTextColor(0xFFE0E0E0); // Blanco
                holder.tvAddress.setTextColor(0xFF2196F3); // Azul normal
            }
        }

        @Override
        public int getItemCount() {
            return (int) Math.ceil((double) maxLen / 16.0);
        }

        static class DiffViewHolder extends RecyclerView.ViewHolder {
            TextView tvAddress, tvHex, tvAscii;

            DiffViewHolder(View itemView) {
                super(itemView);
                tvAddress = itemView.findViewById(R.id.tvAddress);
                tvHex = itemView.findViewById(R.id.tvHex);
                tvAscii = itemView.findViewById(R.id.tvAscii);
            }
        }
    }
}
