package com.diamon.mini.ui;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;

import com.diamon.mini.R;
import com.diamon.mini.binary.IntegrityManager;

import java.io.File;
import java.util.Locale;

/** Shows the MD5, SHA-1, SHA-256 and CRC32 fingerprints of a dump. */
public final class IntegrityDialog {

    private IntegrityDialog() {
    }

    public static void show(@NonNull Activity activity, @NonNull String label,
                            @NonNull File file) {
        String clipboard = IntegrityManager.getClipboardText(activity);
        new Thread(() -> {
            IntegrityManager.HashResult hashes = null;
            String error = null;
            try {
                hashes = IntegrityManager.computeHashes(file);
            } catch (Exception e) {
                error = e.getMessage();
            }
            final IntegrityManager.HashResult result = hashes;
            final String failure = error;
            activity.runOnUiThread(() -> {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                if (result == null) {
                    new AlertDialog.Builder(activity)
                            .setTitle(R.string.integrity_title)
                            .setMessage(activity.getString(R.string.integrity_error, failure))
                            .setPositiveButton(R.string.str_cerrar, null)
                            .show();
                } else {
                    present(activity, label, result, clipboard);
                }
            });
        }, "IntegrityHash").start();
    }

    public static void show(@NonNull Activity activity, @NonNull String label,
                            @NonNull byte[] data) {
        String clipboard = IntegrityManager.getClipboardText(activity);
        new Thread(() -> {
            IntegrityManager.HashResult result = null;
            String error = null;
            try {
                result = IntegrityManager.computeHashes(data);
            } catch (Exception e) {
                error = e.getMessage();
            }
            final IntegrityManager.HashResult hashes = result;
            final String failure = error;
            activity.runOnUiThread(() -> {
                if (activity.isFinishing() || activity.isDestroyed()) return;
                if (hashes == null) {
                    new AlertDialog.Builder(activity)
                            .setTitle(R.string.integrity_title)
                            .setMessage(activity.getString(R.string.integrity_error, failure))
                            .setPositiveButton(R.string.str_cerrar, null)
                            .show();
                } else {
                    present(activity, label, hashes, clipboard);
                }
            });
        }, "IntegrityHash").start();
    }

    private static void present(Activity activity, String label,
                                IntegrityManager.HashResult hashes, String clipboard) {
        IntegrityManager.ClipboardMatchResult match =
                IntegrityManager.checkClipboardMatch(clipboard, hashes);

        StringBuilder text = new StringBuilder();
        text.append(activity.getString(R.string.integrity_file, label)).append('\n');
        text.append(activity.getString(R.string.integrity_size, hashes.getByteCount()))
                .append("\n\n");
        text.append(activity.getString(R.string.integrity_hashes)).append('\n');
        text.append("------------------------------------------\n");
        text.append("MD5:     ").append(hashes.getMd5()).append('\n');
        text.append("SHA-1:   ").append(hashes.getSha1()).append('\n');
        text.append("SHA-256: ").append(hashes.getSha256()).append('\n');
        text.append("CRC32:   ").append(hashes.getCrc32()).append("\n\n");

        if (match.isMatch()) {
            text.append(activity.getString(R.string.integrity_clipboard_match,
                    match.getAlgorithm()));
        } else if (match.hasClipboardHash()) {
            text.append(activity.getString(R.string.integrity_clipboard_mismatch,
                    match.getAlgorithm()));
        } else {
            text.append(activity.getString(R.string.integrity_clipboard_none));
        }
        text.append('\n');

        TextView content = new TextView(activity);
        content.setText(text.toString());
        content.setTextSize(12.5f);
        content.setTextColor(0xFFECEFF1);
        content.setTypeface(android.graphics.Typeface.MONOSPACE);
        content.setTextIsSelectable(true);
        int padding = Math.round(16 * activity.getResources().getDisplayMetrics().density);
        content.setPadding(padding, padding, padding, padding);

        ScrollView scrollView = new ScrollView(activity);
        scrollView.addView(content);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.integrity_title)
                .setView(scrollView)
                .setPositiveButton(R.string.integrity_copy, (d, w) -> {
                    ClipboardManager clipboardManager =
                            (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboardManager.setPrimaryClip(
                            ClipData.newPlainText("EEPROM Flasher integrity", text.toString()));
                    Toast.makeText(activity, activity.getString(R.string.integrity_copied),
                            Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton(R.string.str_cerrar, null)
                .create();
        dialog.show();

        // Keep the dialog readable on small screens.
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
