package com.diamon.mini.vault;

import android.content.Context;

import com.diamon.mini.binary.IntegrityManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Locale;

/**
 * Antibrick safety vault: atomic pre-operation snapshots of the loaded dump.
 *
 * <p>Snapshots are stored in {@code files/vault} as
 * {@code backup_<yyyyMMdd_HHmmss>_<chip>.bin} with a sidecar {@code .meta}
 * that records the chip, timestamp, size and SHA-256 fingerprint. The oldest
 * snapshots are pruned automatically.</p>
 */
public class SafetyVaultManager {

    public static final String VAULT_DIR_NAME = "vault";
    public static final int DEFAULT_MAX_BACKUPS_TO_KEEP = 8;

    private static SafetyVaultManager instance;

    private final File vaultDir;

    public static final class VaultBackup implements Comparable<VaultBackup> {
        private final File file;
        private final File metaFile;
        private final String chipName;
        private final long timestamp;
        private final long sizeBytes;
        private final String sha256;

        public VaultBackup(File file, File metaFile, String chipName, long timestamp,
                           long sizeBytes, String sha256) {
            this.file = file;
            this.metaFile = metaFile;
            this.chipName = chipName != null ? chipName : "EEPROM";
            this.timestamp = timestamp;
            this.sizeBytes = sizeBytes;
            this.sha256 = sha256 != null ? sha256 : "";
        }

        public File getFile() {
            return file;
        }

        public File getMetaFile() {
            return metaFile;
        }

        public String getChipName() {
            return chipName;
        }

        public long getTimestamp() {
            return timestamp;
        }

        public long getSizeBytes() {
            return sizeBytes;
        }

        public String getSha256() {
            return sha256;
        }

        public String getShortSha256() {
            return sha256.isEmpty() ? "N/A"
                    : sha256.substring(0, Math.min(12, sha256.length())) + "…";
        }

        public String getFormattedDate() {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
                    .format(new Date(timestamp));
        }

        @Override
        public int compareTo(VaultBackup other) {
            return Long.compare(other.timestamp, this.timestamp);
        }

        @Override
        public String toString() {
            return "VaultBackup{file=" + file.getName()
                    + ", chip='" + chipName + '\''
                    + ", date=" + getFormattedDate()
                    + ", size=" + sizeBytes + '}';
        }
    }

    public SafetyVaultManager(Context context) {
        this(new File(context.getFilesDir(), VAULT_DIR_NAME));
    }

    public SafetyVaultManager(File vaultDir) {
        this.vaultDir = vaultDir;
        if (!this.vaultDir.exists()) this.vaultDir.mkdirs();
    }

    public static synchronized SafetyVaultManager getInstance(Context context) {
        if (instance == null) {
            instance = new SafetyVaultManager(context.getApplicationContext());
        }
        return instance;
    }

    public File getVaultDir() {
        return vaultDir;
    }

    /**
     * Creates a snapshot only when the source differs from the newest stored
     * snapshots. Returns null when the content is already archived.
     */
    public VaultBackup createSnapshotIfChanged(File sourceDump, String chipName)
            throws IOException {
        if (sourceDump == null || !sourceDump.exists() || !sourceDump.isFile()) {
            return null;
        }
        String sourceHash = IntegrityManager.computeHashes(sourceDump).getSha256();
        List<VaultBackup> existing = listBackups();
        int checked = 0;
        for (VaultBackup backup : existing) {
            if (checked++ >= 3) break;
            if (backup.getSizeBytes() == sourceDump.length()
                    && sourceHash.equalsIgnoreCase(backup.getSha256())) {
                return null;
            }
        }
        return createPreFlashSnapshot(sourceDump, chipName);
    }

    public VaultBackup createPreFlashSnapshot(File sourceDump, String chipName)
            throws IOException {
        if (sourceDump == null || !sourceDump.exists() || !sourceDump.isFile()) {
            throw new IOException("Invalid source file for snapshot: " + sourceDump);
        }

        String safeChip = sanitizeChipName(chipName);
        long now = System.currentTimeMillis();
        String date = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(new Date(now));
        String baseName = String.format(Locale.US, "backup_%s_%s", date, safeChip);

        File target = new File(vaultDir, baseName + ".bin");
        File temp = new File(vaultDir, baseName + ".tmp");
        File meta = new File(vaultDir, baseName + ".meta");

        copyFile(sourceDump, temp);
        if (!temp.renameTo(target)) {
            copyFile(temp, target);
            temp.delete();
        }

        String sha256 = IntegrityManager.computeHashes(target).getSha256();
        writeMetadata(meta, safeChip, now, target.length(), sha256);
        pruneOldBackups(DEFAULT_MAX_BACKUPS_TO_KEEP);

        return new VaultBackup(target, meta, safeChip, now, target.length(), sha256);
    }

    public VaultBackup createPreFlashSnapshot(byte[] data, String chipName)
            throws IOException {
        if (data == null) throw new IOException("Null dump data");

        String safeChip = sanitizeChipName(chipName);
        long now = System.currentTimeMillis();
        String date = new SimpleDateFormat("yyyyMMdd_HHmmss", Locale.US)
                .format(new Date(now));
        String baseName = String.format(Locale.US, "backup_%s_%s", date, safeChip);

        File target = new File(vaultDir, baseName + ".bin");
        File temp = new File(vaultDir, baseName + ".tmp");
        File meta = new File(vaultDir, baseName + ".meta");

        try (FileOutputStream out = new FileOutputStream(temp)) {
            out.write(data);
            out.flush();
            out.getFD().sync();
        }
        if (!temp.renameTo(target)) {
            copyFile(temp, target);
            temp.delete();
        }

        String sha256 = IntegrityManager.computeHashes(target).getSha256();
        writeMetadata(meta, safeChip, now, target.length(), sha256);
        pruneOldBackups(DEFAULT_MAX_BACKUPS_TO_KEEP);

        return new VaultBackup(target, meta, safeChip, now, target.length(), sha256);
    }

    public List<VaultBackup> listBackups() {
        List<VaultBackup> list = new ArrayList<>();
        if (!vaultDir.exists() || !vaultDir.isDirectory()) return list;

        File[] files = vaultDir.listFiles(
                (dir, name) -> name.startsWith("backup_") && name.endsWith(".bin"));
        if (files == null) return list;

        for (File file : files) {
            String base = file.getName().substring(0, file.getName().length() - 4);
            File meta = new File(vaultDir, base + ".meta");
            VaultBackup backup = loadBackup(file, meta);
            if (backup != null) list.add(backup);
        }
        Collections.sort(list);
        return list;
    }

    public VaultBackup getLatestBackup() {
        List<VaultBackup> backups = listBackups();
        return backups.isEmpty() ? null : backups.get(0);
    }

    public VaultBackup getLatestBackupForChip(String chipName) {
        String safeChip = sanitizeChipName(chipName);
        for (VaultBackup backup : listBackups()) {
            if (backup.getChipName().equalsIgnoreCase(safeChip)) return backup;
        }
        return null;
    }

    /** Restores a snapshot into the destination file. */
    public boolean restoreBackup(VaultBackup backup, File destinationFile) {
        if (backup == null || backup.getFile() == null || !backup.getFile().exists()) {
            return false;
        }
        try {
            copyFile(backup.getFile(), destinationFile);
            return true;
        } catch (IOException e) {
            return false;
        }
    }

    public boolean deleteBackup(VaultBackup backup) {
        if (backup == null) return false;
        boolean deleted = false;
        if (backup.getFile() != null && backup.getFile().exists()) {
            deleted = backup.getFile().delete();
        }
        if (backup.getMetaFile() != null && backup.getMetaFile().exists()) {
            backup.getMetaFile().delete();
        }
        return deleted;
    }

    public int pruneOldBackups(int maxKeepCount) {
        if (maxKeepCount <= 0) return 0;
        List<VaultBackup> backups = listBackups();
        int deleted = 0;
        if (backups.size() > maxKeepCount) {
            for (int i = maxKeepCount; i < backups.size(); i++) {
                if (deleteBackup(backups.get(i))) deleted++;
            }
        }
        return deleted;
    }

    public long getTotalVaultSizeBytes() {
        long total = 0L;
        for (VaultBackup backup : listBackups()) total += backup.getSizeBytes();
        return total;
    }

    private VaultBackup loadBackup(File binFile, File metaFile) {
        String chip = "EEPROM";
        long timestamp = binFile.lastModified();
        long size = binFile.length();
        String sha256 = "";

        if (metaFile.exists()) {
            try (BufferedReader reader = new BufferedReader(new FileReader(metaFile))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    int eq = line.indexOf('=');
                    if (eq > 0) {
                        String key = line.substring(0, eq).trim();
                        String value = line.substring(eq + 1).trim();
                        if ("chip".equalsIgnoreCase(key)) chip = value;
                        else if ("timestamp".equalsIgnoreCase(key)) timestamp = Long.parseLong(value);
                        else if ("sha256".equalsIgnoreCase(key)) sha256 = value;
                    }
                }
            } catch (Exception ignored) {
            }
        } else {
            String name = binFile.getName();
            String[] parts = name.split("_");
            if (parts.length >= 4) chip = parts[3].replace(".bin", "");
        }
        return new VaultBackup(binFile, metaFile, chip, timestamp, size, sha256);
    }

    private void writeMetadata(File metaFile, String chip, long timestamp,
                               long size, String sha256) throws IOException {
        String content = "chip=" + chip + "\n"
                + "timestamp=" + timestamp + "\n"
                + "size=" + size + "\n"
                + "sha256=" + sha256 + "\n";
        try (FileOutputStream out = new FileOutputStream(metaFile)) {
            out.write(content.getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
    }

    private static String sanitizeChipName(String chipName) {
        if (chipName == null || chipName.trim().isEmpty()) return "UNKNOWN_CHIP";
        return chipName.replaceAll("[^a-zA-Z0-9_.-]", "_");
    }

    private static void copyFile(File source, File destination) throws IOException {
        try (InputStream in = new FileInputStream(source);
             OutputStream out = new FileOutputStream(destination)) {
            byte[] buffer = new byte[IntegrityManager.BUFFER_SIZE];
            int read;
            while ((read = in.read(buffer)) > 0) {
                out.write(buffer, 0, read);
            }
            out.flush();
        }
    }

    public static String formatFileSize(long bytes) {
        if (bytes < 1024) return bytes + " B";
        int exp = (int) (Math.log(bytes) / Math.log(1024));
        String prefix = "KMGTPE".charAt(exp - 1) + "";
        return String.format(Locale.US, "%.1f %sB", bytes / Math.pow(1024, exp), prefix);
    }
}
