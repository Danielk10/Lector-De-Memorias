package com.diamon.mini.report;

import android.content.Context;

import com.diamon.mini.R;
import com.diamon.mini.binary.IntegrityManager;
import com.diamon.mini.vault.SafetyVaultManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Arrays;
import java.util.Comparator;
import java.util.Date;
import java.util.Locale;

/**
 * Generates a plain-text technical report for the currently loaded dump.
 *
 * <p>Includes cryptographic hashes, blank-check status, non-0xFF statistics,
 * a byte histogram and a 4 KB sector occupancy summary. The report can be
 * copied or saved as a .txt file through the Storage Access Framework.</p>
 */
public final class TechnicalReport {

    private static final int SECTOR_SIZE = 4096;
    private static final int HISTOGRAM_TOP = 8;

    private TechnicalReport() {
    }

    public static String generate(Context context, File dump, String chip, String source)
            throws IOException {
        if (dump == null || !dump.exists() || !dump.isFile()) {
            throw new IOException("Dump file does not exist: " + dump);
        }

        IntegrityManager.HashResult hashes = IntegrityManager.computeHashes(dump);

        long[] histogram = new long[256];
        long nonFF = 0L;
        long firstNonFF = -1L;
        long lastNonFF = -1L;
        long size = 0L;
        int nonBlankSectors = 0;
        int currentSectorNonFF = 0;

        try (InputStream in = new FileInputStream(dump)) {
            byte[] buffer = new byte[IntegrityManager.BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) != -1) {
                for (int i = 0; i < read; i++) {
                    int value = buffer[i] & 0xFF;
                    histogram[value]++;
                    long offset = size + i;
                    if (value != 0xFF) {
                        nonFF++;
                        if (firstNonFF < 0L) firstNonFF = offset;
                        lastNonFF = offset;
                        currentSectorNonFF++;
                    }
                    if (((offset + 1) % SECTOR_SIZE) == 0) {
                        if (currentSectorNonFF > 0) nonBlankSectors++;
                        currentSectorNonFF = 0;
                    }
                }
                size += read;
            }
        }
        if (size % SECTOR_SIZE != 0 && currentSectorNonFF > 0) {
            nonBlankSectors++;
        }

        int totalSectors = (int) ((size + SECTOR_SIZE - 1) / SECTOR_SIZE);
        boolean blank = nonFF == 0L;

        StringBuilder sb = new StringBuilder();
        String generated = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                .format(new Date());

        sb.append(context.getString(R.string.report_header)).append('\n');
        sb.append(context.getString(R.string.report_generated, generated)).append('\n');
        sb.append("============================================================\n\n");

        sb.append(context.getString(R.string.report_file, dump.getName())).append('\n');
        sb.append(context.getString(R.string.report_size,
                size, SafetyVaultManager.formatFileSize(size))).append('\n');
        sb.append(context.getString(R.string.report_chip,
                chip != null && !chip.isEmpty() ? chip : "—")).append('\n');
        sb.append(context.getString(R.string.report_source,
                source != null && !source.isEmpty() ? source : "—")).append("\n\n");

        sb.append(context.getString(R.string.report_hashes)).append('\n');
        sb.append("------------------------------------------------------------\n");
        sb.append("MD5:     ").append(hashes.getMd5()).append('\n');
        sb.append("SHA-1:   ").append(hashes.getSha1()).append('\n');
        sb.append("SHA-256: ").append(hashes.getSha256()).append('\n');
        sb.append("CRC32:   ").append(hashes.getCrc32()).append("\n\n");

        sb.append(context.getString(R.string.report_integrity)).append('\n');
        sb.append("------------------------------------------------------------\n");
        sb.append(context.getString(R.string.report_blank,
                context.getString(blank ? R.string.report_yes : R.string.report_no)))
                .append('\n');
        sb.append(context.getString(R.string.report_non_ff,
                nonFF, size > 0 ? (nonFF * 100.0) / size : 0.0)).append('\n');
        sb.append(context.getString(R.string.report_first_last,
                firstNonFF >= 0 ? String.format(Locale.US, "0x%08X", firstNonFF) : "—",
                lastNonFF >= 0 ? String.format(Locale.US, "0x%08X", lastNonFF) : "—"))
                .append('\n');
        sb.append(context.getString(R.string.report_sectors,
                nonBlankSectors, totalSectors, SECTOR_SIZE / 1024)).append("\n\n");

        sb.append(context.getString(R.string.report_histogram)).append('\n');
        sb.append("------------------------------------------------------------\n");

        Integer[] order = new Integer[256];
        for (int i = 0; i < order.length; i++) order[i] = i;
        Arrays.sort(order, Comparator.comparingLong(index -> -histogram[index]));

        for (int i = 0; i < HISTOGRAM_TOP && i < order.length; i++) {
            int value = order[i];
            if (histogram[value] == 0L) break;
            double pct = size > 0 ? (histogram[value] * 100.0) / size : 0.0;
            sb.append(context.getString(R.string.report_histogram_line,
                    String.format(Locale.US, "%02X", value),
                    histogram[value], pct)).append('\n');
        }

        sb.append('\n').append(context.getString(R.string.report_footer)).append('\n');
        return sb.toString();
    }
}
