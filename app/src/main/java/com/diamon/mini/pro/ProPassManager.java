package com.diamon.mini.pro;

import android.content.Context;
import android.content.SharedPreferences;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.diamon.mini.billing.BillingManager;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.concurrent.TimeUnit;

/**
 * Manages the temporary Pro pass unlocked by watching a rewarded AdMob video.
 *
 * <p>The pass lasts 15 minutes, is stored locally with an HMAC-style SHA-256
 * signature and is mirrored to a small signed file so that clearing the app's
 * preferences does not immediately destroy an active pass. No cloud service is
 * used.</p>
 */
public class ProPassManager {

    private static final String TAG = "ProPassManager";
    public static final String PREFS_NAME = "pro_pass_prefs";
    public static final String KEY_EXPIRE_TIME = "pro_pass_expire_time";
    public static final String KEY_SIGNATURE = "pro_pass_signature";

    private static final String BACKUP_FILENAME = ".eeprom_flasher_pro_pass.dat";
    private static final String SALT = "EEPROM_FLASHER_PRO_PASS_SALT_V1";

    /** Duration of the rewarded pass: 15 minutes. */
    public static final long DURACION_PASE_MS = 15 * 60 * 1000L;

    public interface OnPassRestoredListener {
        void onPassRestored(long expireTime);
    }

    private final Context context;
    private final SharedPreferences prefs;
    @Nullable
    private String cachedDeviceId;
    @Nullable
    private OnPassRestoredListener onPassRestoredListener;

    public ProPassManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        if (!hasActiveProPass()) {
            intentarRestaurarRespaldo();
        }
    }

    @VisibleForTesting
    public ProPassManager(@NonNull SharedPreferences prefs) {
        this.context = null;
        this.prefs = prefs;
    }

    public void setOnPassRestoredListener(@Nullable OnPassRestoredListener listener) {
        this.onPassRestoredListener = listener;
    }

    /** Activates a fresh 15-minute pass from the current moment. */
    public void activarPase() {
        long expire = System.currentTimeMillis() + DURACION_PASE_MS;
        guardarExpiracion(expire);
        guardarRespaldoPersistente(expire);
        Log.i(TAG, "Pro pass activated until " + expire);
    }

    /** Alias kept for clarity at call sites. */
    public void activarPase15Minutos() {
        activarPase();
    }

    public boolean hasActiveProPass() {
        return System.currentTimeMillis() < getExpireTime();
    }

    public boolean isPaseActivo() {
        return hasActiveProPass();
    }

    public long getExpireTime() {
        long expire = prefs.getLong(KEY_EXPIRE_TIME, 0L);
        if (expire <= 0L) return 0L;
        String stored = prefs.getString(KEY_SIGNATURE, null);
        String expected = calcularHash(getDeviceId(), expire);
        if (stored == null || !stored.equals(expected)) {
            // Preferences were edited manually or belong to another device.
            return 0L;
        }
        return expire;
    }

    public long getExpirationTimestamp() {
        return getExpireTime();
    }

    public long getTiempoRestanteMs() {
        return Math.max(0L, getExpireTime() - System.currentTimeMillis());
    }

    /**
     * Returns a compact countdown: mm:ss, or h:mm:ss if a future change
     * ever extends the pass beyond one hour.
     */
    @NonNull
    public String getTiempoRestanteFormateado() {
        long remaining = getTiempoRestanteMs();
        if (remaining <= 0L) {
            return context != null ? context.getString(com.diamon.mini.R.string.pro_pass_expired) : "00:00";
        }
        long totalSeconds = TimeUnit.MILLISECONDS.toSeconds(remaining);
        long hours = totalSeconds / 3600L;
        long minutes = (totalSeconds % 3600L) / 60L;
        long seconds = totalSeconds % 60L;
        if (hours > 0L) {
            return String.format(java.util.Locale.US, "%d:%02d:%02d", hours, minutes, seconds);
        }
        return String.format(java.util.Locale.US, "%02d:%02d", minutes, seconds);
    }

    public String formatearTiempoRestante() {
        return getTiempoRestanteFormateado();
    }

    /** True when Pro is owned permanently or the temporary pass is active. */
    public boolean isProActive(@Nullable BillingManager billingManager) {
        return (billingManager != null && billingManager.isProPurchased()) || hasActiveProPass();
    }

    /** Static variant for activities that do not open a billing connection. */
    public boolean isProActive(@NonNull Context context) {
        return BillingManager.isProPurchased(context) || hasActiveProPass();
    }

    private void guardarExpiracion(long expireTime) {
        prefs.edit()
                .putLong(KEY_EXPIRE_TIME, expireTime)
                .putString(KEY_SIGNATURE, calcularHash(getDeviceId(), expireTime))
                .apply();
    }

    private String getDeviceId() {
        if (context == null) return "test_device";
        if (cachedDeviceId != null) return cachedDeviceId;
        try {
            String id = Settings.Secure.getString(
                    context.getContentResolver(), Settings.Secure.ANDROID_ID);
            cachedDeviceId = id != null ? id : "unknown_device";
        } catch (Exception e) {
            cachedDeviceId = "unknown_device";
        }
        return cachedDeviceId;
    }

    private String calcularHash(String deviceId, long expireTime) {
        try {
            String payload = deviceId + ":" + expireTime + ":" + SALT;
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] digest = md.digest(payload.getBytes(StandardCharsets.UTF_8));
            StringBuilder sb = new StringBuilder(digest.length * 2);
            for (byte b : digest) {
                sb.append(String.format(java.util.Locale.US, "%02x", b));
            }
            return sb.toString();
        } catch (Exception e) {
            return "";
        }
    }

    private void guardarRespaldoPersistente(long expireTime) {
        if (context == null) return;
        new Thread(() -> {
            try {
                File dir = context.getFilesDir();
                if (dir != null && !dir.exists()) dir.mkdirs();
                File backup = new File(dir, BACKUP_FILENAME);
                String hash = calcularHash(getDeviceId(), expireTime);
                try (FileWriter writer = new FileWriter(backup)) {
                    writer.write(expireTime + ":" + hash);
                    writer.flush();
                }
            } catch (Exception e) {
                Log.w(TAG, "Could not write pass backup", e);
            }
        }, "ProPassBackup").start();
    }

    private void intentarRestaurarRespaldo() {
        if (context == null) return;
        try {
            File backup = new File(context.getFilesDir(), BACKUP_FILENAME);
            if (!backup.exists() || !backup.canRead()) return;

            String line;
            try (BufferedReader reader = new BufferedReader(new FileReader(backup))) {
                line = reader.readLine();
            }
            if (line == null || !line.contains(":")) return;

            String[] parts = line.split(":");
            if (parts.length != 2) return;
            long expire = Long.parseLong(parts[0]);
            String expected = calcularHash(getDeviceId(), expire);
            if (!expected.equals(parts[1])) return;
            if (System.currentTimeMillis() >= expire) return;

            guardarExpiracion(expire);
            Log.i(TAG, "Pro pass restored from signed backup.");
            if (onPassRestoredListener != null) {
                onPassRestoredListener.onPassRestored(expire);
            }
        } catch (Exception e) {
            Log.w(TAG, "Could not restore pass backup", e);
        }
    }

    @VisibleForTesting
    public void setExpireTime(long expireTime) {
        guardarExpiracion(expireTime);
    }

    @VisibleForTesting
    public void limpiarPase() {
        prefs.edit()
                .remove(KEY_EXPIRE_TIME)
                .remove(KEY_SIGNATURE)
                .apply();
    }
}
