package com.diamon.mini.binary;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.CRC32;

/**
 * Streaming multi-algorithm integrity engine for EEPROM/Flash dumps.
 *
 * <p>Computes MD5, SHA-1, SHA-256 and CRC32 in a single pass without loading
 * the whole file into memory, and can compare the result with hashes found in
 * the clipboard (for example a manufacturer's published SHA-256).</p>
 */
public final class IntegrityManager {

    public static final int BUFFER_SIZE = 256 * 1024;

    private static final Pattern HEX_64 = Pattern.compile("\\b([a-fA-F0-9]{64})\\b");
    private static final Pattern HEX_40 = Pattern.compile("\\b([a-fA-F0-9]{40})\\b");
    private static final Pattern HEX_32 = Pattern.compile("\\b([a-fA-F0-9]{32})\\b");
    private static final Pattern HEX_8 = Pattern.compile("\\b([a-fA-F0-9]{8})\\b");

    private IntegrityManager() {
    }

    public interface HashProgressListener {
        void onProgress(long bytesProcessed, long totalBytes);
    }

    public static final class HashResult {
        private final String md5;
        private final String sha1;
        private final String sha256;
        private final String crc32;
        private final long byteCount;

        public HashResult(String md5, String sha1, String sha256, String crc32, long byteCount) {
            this.md5 = md5 != null ? md5.toLowerCase(Locale.US) : "";
            this.sha1 = sha1 != null ? sha1.toLowerCase(Locale.US) : "";
            this.sha256 = sha256 != null ? sha256.toLowerCase(Locale.US) : "";
            this.crc32 = crc32 != null ? crc32.toLowerCase(Locale.US) : "";
            this.byteCount = byteCount;
        }

        public String getMd5() {
            return md5;
        }

        public String getSha1() {
            return sha1;
        }

        public String getSha256() {
            return sha256;
        }

        public String getCrc32() {
            return crc32;
        }

        public long getByteCount() {
            return byteCount;
        }

        public Map<String, String> asMap() {
            Map<String, String> map = new LinkedHashMap<>();
            map.put("MD5", md5);
            map.put("SHA-1", sha1);
            map.put("SHA-256", sha256);
            map.put("CRC32", crc32);
            return Collections.unmodifiableMap(map);
        }

        public String formatSummary() {
            StringBuilder sb = new StringBuilder();
            sb.append("Size:    ").append(byteCount).append(" bytes\n");
            sb.append("MD5:     ").append(md5).append("\n");
            sb.append("SHA-1:   ").append(sha1).append("\n");
            sb.append("SHA-256: ").append(sha256).append("\n");
            sb.append("CRC32:   ").append(crc32);
            return sb.toString();
        }
    }

    public static final class ClipboardMatchResult {
        private final boolean match;
        private final String algorithm;
        private final String matchedHash;
        private final String clipboardText;
        private final String message;

        public ClipboardMatchResult(boolean match, String algorithm, String matchedHash,
                                    String clipboardText, String message) {
            this.match = match;
            this.algorithm = algorithm;
            this.matchedHash = matchedHash;
            this.clipboardText = clipboardText;
            this.message = message;
        }

        public boolean isMatch() {
            return match;
        }

        public boolean isMatched() {
            return match;
        }

        public String getAlgorithm() {
            return algorithm;
        }

        public String getMatchedAlgorithm() {
            return algorithm != null ? algorithm : "";
        }

        public boolean hasClipboardHash() {
            return algorithm != null && !algorithm.isEmpty();
        }

        public String getMatchedHash() {
            return matchedHash;
        }

        public String getClipboardText() {
            return clipboardText;
        }

        public String getMessage() {
            return message;
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Hash computation
    // ─────────────────────────────────────────────────────────────────────────

    public static HashResult computeHashes(File file, HashProgressListener listener)
            throws IOException {
        if (file == null || !file.exists() || !file.isFile()) {
            throw new IOException("File does not exist or is invalid: " + file);
        }
        long total = file.length();
        try (InputStream in = new FileInputStream(file)) {
            return computeHashes(in, total, listener);
        }
    }

    public static HashResult computeHashes(File file) throws IOException {
        return computeHashes(file, null);
    }

    public static HashResult computeHashes(InputStream in, long totalBytes,
                                           HashProgressListener listener) throws IOException {
        MessageDigest md5;
        MessageDigest sha1;
        MessageDigest sha256;
        try {
            md5 = MessageDigest.getInstance("MD5");
            sha1 = MessageDigest.getInstance("SHA-1");
            sha256 = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hash algorithm unavailable", e);
        }
        CRC32 crc32 = new CRC32();

        byte[] buffer = new byte[BUFFER_SIZE];
        long processed = 0L;
        int read;
        while ((read = in.read(buffer)) != -1) {
            if (read > 0) {
                md5.update(buffer, 0, read);
                sha1.update(buffer, 0, read);
                sha256.update(buffer, 0, read);
                crc32.update(buffer, 0, read);
                processed += read;
                if (listener != null) listener.onProgress(processed, totalBytes);
            }
        }

        return new HashResult(
                bytesToHex(md5.digest()),
                bytesToHex(sha1.digest()),
                bytesToHex(sha256.digest()),
                String.format(Locale.US, "%08x", crc32.getValue()),
                processed);
    }

    public static HashResult computeHashes(byte[] data) {
        if (data == null) data = new byte[0];
        try {
            MessageDigest md5 = MessageDigest.getInstance("MD5");
            MessageDigest sha1 = MessageDigest.getInstance("SHA-1");
            MessageDigest sha256 = MessageDigest.getInstance("SHA-256");
            CRC32 crc32 = new CRC32();

            md5.update(data);
            sha1.update(data);
            sha256.update(data);
            crc32.update(data);

            return new HashResult(
                    bytesToHex(md5.digest()),
                    bytesToHex(sha1.digest()),
                    bytesToHex(sha256.digest()),
                    String.format(Locale.US, "%08x", crc32.getValue()),
                    data.length);
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Hash algorithm unavailable", e);
        }
    }

    // ─────────────────────────────────────────────────────────────────────────
    // Clipboard matching
    // ─────────────────────────────────────────────────────────────────────────

    public static ClipboardMatchResult checkClipboardMatch(Context context, File file)
            throws IOException {
        return checkClipboardMatch(getClipboardText(context), computeHashes(file));
    }

    public static ClipboardMatchResult checkClipboardMatch(String clipText, HashResult hashes) {
        if (hashes == null) {
            return new ClipboardMatchResult(false, null, null, clipText,
                    "File hashes are not available.");
        }
        return checkClipboardMatch(clipText, hashes.asMap());
    }

    public static ClipboardMatchResult checkClipboardMatch(String clipText,
                                                           Map<String, String> hashes) {
        if (clipText == null || clipText.trim().isEmpty()) {
            return new ClipboardMatchResult(false, null, null, "",
                    "Clipboard is empty.");
        }

        String raw = clipText.trim();
        Map<String, String> normalized = new LinkedHashMap<>();
        if (hashes != null) {
            for (Map.Entry<String, String> entry : hashes.entrySet()) {
                if (entry.getValue() != null) {
                    normalized.put(entry.getKey().toUpperCase(Locale.US),
                            entry.getValue().trim().toLowerCase(Locale.US));
                }
            }
        }

        Matcher m64 = HEX_64.matcher(raw);
        while (m64.find()) {
            String token = m64.group(1).toLowerCase(Locale.US);
            String expected = normalized.get("SHA-256");
            if (expected != null) {
                return new ClipboardMatchResult(expected.equalsIgnoreCase(token),
                        "SHA-256", token, raw,
                        expected.equalsIgnoreCase(token)
                                ? "SHA-256 matches the clipboard."
                                : "SHA-256 does not match the clipboard.");
            }
        }

        Matcher m40 = HEX_40.matcher(raw);
        while (m40.find()) {
            String token = m40.group(1).toLowerCase(Locale.US);
            String expected = normalized.get("SHA-1");
            if (expected != null) {
                return new ClipboardMatchResult(expected.equalsIgnoreCase(token),
                        "SHA-1", token, raw,
                        expected.equalsIgnoreCase(token)
                                ? "SHA-1 matches the clipboard."
                                : "SHA-1 does not match the clipboard.");
            }
        }

        Matcher m32 = HEX_32.matcher(raw);
        while (m32.find()) {
            String token = m32.group(1).toLowerCase(Locale.US);
            String expected = normalized.get("MD5");
            if (expected != null) {
                return new ClipboardMatchResult(expected.equalsIgnoreCase(token),
                        "MD5", token, raw,
                        expected.equalsIgnoreCase(token)
                                ? "MD5 matches the clipboard."
                                : "MD5 does not match the clipboard.");
            }
        }

        Matcher m8 = HEX_8.matcher(raw);
        while (m8.find()) {
            String token = m8.group(1).toLowerCase(Locale.US);
            String expected = normalized.get("CRC32");
            if (expected != null) {
                return new ClipboardMatchResult(expected.equalsIgnoreCase(token),
                        "CRC32", token, raw,
                        expected.equalsIgnoreCase(token)
                                ? "CRC32 matches the clipboard."
                                : "CRC32 does not match the clipboard.");
            }
        }

        return new ClipboardMatchResult(false, null, null, raw,
                "No known hash was found in the clipboard.");
    }

    public static String getClipboardText(Context context) {
        if (context == null) return null;
        try {
            ClipboardManager clipboard =
                    (ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null && clipboard.hasPrimaryClip()) {
                ClipData clip = clipboard.getPrimaryClip();
                if (clip != null && clip.getItemCount() > 0) {
                    CharSequence text = clip.getItemAt(0).getText();
                    return text != null ? text.toString() : null;
                }
            }
        } catch (Exception ignored) {
        }
        return null;
    }

    public static String bytesToHex(byte[] bytes) {
        if (bytes == null) return "";
        StringBuilder sb = new StringBuilder(bytes.length * 2);
        for (byte b : bytes) {
            int value = b & 0xFF;
            if (value < 16) sb.append('0');
            sb.append(Integer.toHexString(value));
        }
        return sb.toString();
    }
}
