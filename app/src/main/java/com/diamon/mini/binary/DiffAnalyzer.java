package com.diamon.mini.binary;

import android.content.Context;

import com.diamon.mini.R;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Sector-aware comparison engine used by the advanced HEX diff.
 *
 * <p>The basic difference counter remains available to every user. This class
 * adds the Pro analysis: first/last difference, 4 KB sector map and an
 * exportable technical report with SHA-256 fingerprints.</p>
 */
public final class DiffAnalyzer {

    public static final int SECTOR_SIZE = 4096;
    private static final int MAX_REPORTED_OFFSETS = 200;
    private static final int MAX_REPORTED_SECTORS = 200;

    private DiffAnalyzer() {
    }

    public static final class SectorDiff {
        private final int index;
        private final int diffCount;
        private final long start;
        private final long end;

        SectorDiff(int index, int diffCount, long start, long end) {
            this.index = index;
            this.diffCount = diffCount;
            this.start = start;
            this.end = end;
        }

        public int getIndex() {
            return index;
        }

        public int getDiffCount() {
            return diffCount;
        }

        public long getStart() {
            return start;
        }

        public long getEnd() {
            return end;
        }
    }

    public static final class Result {
        private final long maxLength;
        private final long diffCount;
        private final long firstDiff;
        private final long lastDiff;
        private final int totalSectors;
        private final List<SectorDiff> changedSectors;

        Result(long maxLength, long diffCount, long firstDiff, long lastDiff,
               int totalSectors, List<SectorDiff> changedSectors) {
            this.maxLength = maxLength;
            this.diffCount = diffCount;
            this.firstDiff = firstDiff;
            this.lastDiff = lastDiff;
            this.totalSectors = totalSectors;
            this.changedSectors = changedSectors;
        }

        public long getMaxLength() {
            return maxLength;
        }

        public long getDiffCount() {
            return diffCount;
        }

        public long getFirstDiff() {
            return firstDiff;
        }

        public long getLastDiff() {
            return lastDiff;
        }

        public int getTotalSectors() {
            return totalSectors;
        }

        public int getChangedSectorCount() {
            return changedSectors.size();
        }

        public List<SectorDiff> getChangedSectors() {
            return changedSectors;
        }

        public double getPercent() {
            return maxLength > 0 ? (diffCount * 100.0) / maxLength : 0.0;
        }

        public boolean isIdentical() {
            return diffCount == 0L;
        }
    }

    public static Result analyze(byte[] dataA, byte[] dataB) {
        byte[] a = dataA != null ? dataA : new byte[0];
        byte[] b = dataB != null ? dataB : new byte[0];
        long maxLength = Math.max(a.length, b.length);
        int totalSectors = (int) ((maxLength + SECTOR_SIZE - 1) / SECTOR_SIZE);
        int[] sectorCounts = new int[totalSectors];

        long diffCount = 0L;
        long firstDiff = -1L;
        long lastDiff = -1L;

        for (long i = 0; i < maxLength; i++) {
            byte va = i < a.length ? a[(int) i] : (byte) 0xFF;
            byte vb = i < b.length ? b[(int) i] : (byte) 0xFF;
            if (va != vb) {
                diffCount++;
                if (firstDiff < 0L) firstDiff = i;
                lastDiff = i;
                sectorCounts[(int) (i / SECTOR_SIZE)]++;
            }
        }

        List<SectorDiff> sectors = new ArrayList<>();
        for (int i = 0; i < sectorCounts.length; i++) {
            if (sectorCounts[i] > 0) {
                long start = (long) i * SECTOR_SIZE;
                long end = Math.min(maxLength, start + SECTOR_SIZE);
                sectors.add(new SectorDiff(i, sectorCounts[i], start, end));
            }
        }

        return new Result(maxLength, diffCount, firstDiff, lastDiff, totalSectors, sectors);
    }

    /** Builds a plain-text report suitable for sharing or archiving. */
    public static String buildReport(Context context, String nameA, byte[] dataA,
                                     String nameB, byte[] dataB, Result result) {
        StringBuilder sb = new StringBuilder();
        String generated = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                .format(new Date());

        sb.append(context.getString(R.string.diff_report_header)).append('\n');
        sb.append(context.getString(R.string.diff_report_generated, generated)).append('\n');
        sb.append("============================================================\n\n");

        sb.append(context.getString(R.string.diff_report_file,
                "A", nameA, dataA != null ? dataA.length : 0)).append('\n');
        sb.append(context.getString(R.string.diff_report_sha,
                IntegrityManager.computeHashes(dataA).getSha256())).append('\n');
        sb.append(context.getString(R.string.diff_report_file,
                "B", nameB, dataB != null ? dataB.length : 0)).append('\n');
        sb.append(context.getString(R.string.diff_report_sha,
                IntegrityManager.computeHashes(dataB).getSha256())).append("\n\n");

        if (result.isIdentical()) {
            sb.append(context.getString(R.string.diff_report_identical)).append('\n');
        } else {
            sb.append(context.getString(R.string.diff_report_stats,
                    result.getDiffCount(), result.getPercent(), result.getMaxLength()))
                    .append('\n');
            sb.append(context.getString(R.string.diff_report_first_last,
                    String.format(Locale.US, "0x%08X", result.getFirstDiff()),
                    String.format(Locale.US, "0x%08X", result.getLastDiff())))
                    .append("\n\n");
        }

        sb.append(context.getString(R.string.diff_report_sectors,
                result.getChangedSectorCount(), result.getTotalSectors(), SECTOR_SIZE / 1024))
                .append('\n');

        int shownSectors = 0;
        for (SectorDiff sector : result.getChangedSectors()) {
            if (shownSectors >= MAX_REPORTED_SECTORS) {
                sb.append(context.getString(R.string.diff_report_truncated,
                        result.getChangedSectors().size() - shownSectors)).append('\n');
                break;
            }
            double pct = (sector.getDiffCount() * 100.0) / Math.max(1L, sector.getEnd() - sector.getStart());
            sb.append(context.getString(R.string.diff_report_sector_line,
                    sector.getIndex(),
                    String.format(Locale.US, "0x%08X", sector.getStart()),
                    String.format(Locale.US, "0x%08X", sector.getEnd() - 1),
                    sector.getDiffCount(), pct)).append('\n');
            shownSectors++;
        }

        if (!result.isIdentical()) {
            sb.append('\n').append(context.getString(R.string.diff_report_offsets_header))
                    .append('\n');
            int shown = 0;
            long max = Math.max(dataA != null ? dataA.length : 0,
                    dataB != null ? dataB.length : 0);
            for (long i = 0; i < max && shown < MAX_REPORTED_OFFSETS; i++) {
                byte va = i < (dataA != null ? dataA.length : 0) ? dataA[(int) i] : (byte) 0xFF;
                byte vb = i < (dataB != null ? dataB.length : 0) ? dataB[(int) i] : (byte) 0xFF;
                if (va != vb) {
                    sb.append(context.getString(R.string.diff_report_offset_line,
                            String.format(Locale.US, "0x%08X", i),
                            String.format(Locale.US, "%02X", va & 0xFF),
                            String.format(Locale.US, "%02X", vb & 0xFF))).append('\n');
                    shown++;
                }
            }
            if (shown >= MAX_REPORTED_OFFSETS && result.getDiffCount() > shown) {
                sb.append(context.getString(R.string.diff_report_truncated_offsets,
                        result.getDiffCount() - shown)).append('\n');
            }
        }

        sb.append('\n').append(context.getString(R.string.diff_report_footer)).append('\n');
        return sb.toString();
    }
}
