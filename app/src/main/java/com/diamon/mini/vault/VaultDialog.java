package com.diamon.mini.vault;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.diamon.mini.R;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

/** Lists, creates, restores and deletes Antibrick vault snapshots. */
public class VaultDialog {

    public interface Listener {
        void onRomRestored(@NonNull SafetyVaultManager.VaultBackup backup);

        void onSnapshotCreated(@NonNull SafetyVaultManager.VaultBackup backup);
    }

    public static void show(@NonNull Activity activity,
                            @NonNull SafetyVaultManager vault,
                            @NonNull File romFile,
                            @NonNull String chipName,
                            @Nullable Listener listener) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        float density = activity.getResources().getDisplayMetrics().density;
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(16, density), dp(12, density), dp(16, density), dp(8, density));
        root.setBackgroundColor(0xFF181824);

        TextView summary = new TextView(activity);
        summary.setTextSize(12);
        summary.setTextColor(0xFFB0B0C4);
        summary.setPadding(0, 0, 0, dp(8, density));
        root.addView(summary);

        ListView listView = new ListView(activity);
        listView.setDividerHeight(0);
        listView.setBackgroundColor(0xFF1E1E2C);
        root.addView(listView, new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dp(300, density)));

        final List<SafetyVaultManager.VaultBackup> items = new ArrayList<>();
        final ArrayAdapter<SafetyVaultManager.VaultBackup> adapter =
                new ArrayAdapter<SafetyVaultManager.VaultBackup>(activity, 0, items) {
                    @NonNull
                    @Override
                    public View getView(int position, @Nullable View convertView,
                                        @NonNull ViewGroup parent) {
                        LinearLayout row = new LinearLayout(activity);
                        row.setOrientation(LinearLayout.VERTICAL);
                        row.setPadding(dp(10, density), dp(8, density),
                                dp(10, density), dp(8, density));

                        SafetyVaultManager.VaultBackup backup = getItem(position);
                        TextView title = new TextView(activity);
                        title.setText(backup != null
                                ? backup.getChipName() + "  ·  " + backup.getFormattedDate()
                                : "");
                        title.setTextSize(13);
                        title.setTypeface(null, Typeface.BOLD);
                        title.setTextColor(0xFFECEFF1);

                        TextView subtitle = new TextView(activity);
                        subtitle.setText(backup != null
                                ? SafetyVaultManager.formatFileSize(backup.getSizeBytes())
                                + "  ·  SHA-256 " + backup.getShortSha256()
                                : "");
                        subtitle.setTextSize(11);
                        subtitle.setTextColor(0xFF90A4AE);

                        row.addView(title);
                        row.addView(subtitle);
                        return row;
                    }
                };
        listView.setAdapter(adapter);

        final Runnable refresh = () -> {
            items.clear();
            items.addAll(vault.listBackups());
            adapter.notifyDataSetChanged();
            long total = vault.getTotalVaultSizeBytes();
            summary.setText(items.isEmpty()
                    ? activity.getString(R.string.vault_empty)
                    : activity.getString(R.string.vault_summary,
                    items.size(), SafetyVaultManager.formatFileSize(total)));
        };
        refresh.run();

        Button create = new Button(activity);
        create.setText(activity.getString(R.string.vault_create_now));
        create.setTextColor(Color.WHITE);
        create.setBackgroundColor(0xFF00897B);
        create.setOnClickListener(v -> {
            if (!romFile.exists() || romFile.length() == 0L) {
                Toast.makeText(activity, activity.getString(R.string.vault_no_dump),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            create.setEnabled(false);
            create.setText(activity.getString(R.string.vault_creating));
            new Thread(() -> {
                SafetyVaultManager.VaultBackup created = null;
                Exception error = null;
                try {
                    created = vault.createSnapshotIfChanged(romFile, chipName);
                } catch (Exception e) {
                    error = e;
                }
                final SafetyVaultManager.VaultBackup result = created;
                final Exception failure = error;
                activity.runOnUiThread(() -> {
                    if (activity.isFinishing() || activity.isDestroyed()) return;
                    create.setEnabled(true);
                    create.setText(activity.getString(R.string.vault_create_now));
                    if (failure != null) {
                        Toast.makeText(activity,
                                activity.getString(R.string.vault_create_failed,
                                        failure.getMessage()),
                                Toast.LENGTH_LONG).show();
                    } else if (result == null) {
                        Toast.makeText(activity, activity.getString(R.string.vault_no_changes),
                                Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(activity,
                                activity.getString(R.string.vault_created,
                                        result.getFile().getName()),
                                Toast.LENGTH_LONG).show();
                        refresh.run();
                        if (listener != null) listener.onSnapshotCreated(result);
                    }
                });
            }, "VaultSnapshot").start();
        });
        root.addView(create);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setTitle(R.string.vault_title)
                .setView(root)
                .setNegativeButton(R.string.str_cerrar, null)
                .create();

        listView.setOnItemClickListener((parent, view, position, id) -> {
            if (position < 0 || position >= items.size()) return;
            SafetyVaultManager.VaultBackup backup = items.get(position);

            new AlertDialog.Builder(activity)
                    .setTitle(backup.getChipName())
                    .setMessage(activity.getString(R.string.vault_details,
                            backup.getFormattedDate(),
                            SafetyVaultManager.formatFileSize(backup.getSizeBytes()),
                            backup.getSha256()))
                    .setPositiveButton(R.string.vault_restore, (d, w) ->
                            confirmRestore(activity, vault, romFile, backup, dialog, listener))
                    .setNeutralButton(R.string.vault_delete, (d, w) ->
                            confirmDelete(activity, vault, backup, refresh))
                    .setNegativeButton(R.string.str_cancelar, null)
                    .show();
        });

        dialog.show();
    }

    private static void confirmRestore(Activity activity, SafetyVaultManager vault,
                                       File romFile,
                                       SafetyVaultManager.VaultBackup backup,
                                       AlertDialog parentDialog,
                                       @Nullable Listener listener) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.vault_restore_confirm_title)
                .setMessage(activity.getString(R.string.vault_restore_confirm_msg,
                        backup.getFile().getName()))
                .setPositiveButton(R.string.vault_restore, (d, w) -> {
                    new Thread(() -> {
                        // Preserve the current dump before replacing it.
                        try {
                            vault.createSnapshotIfChanged(romFile, "pre-restore");
                        } catch (Exception ignored) {
                        }
                        boolean ok = vault.restoreBackup(backup, romFile);
                        activity.runOnUiThread(() -> {
                            if (activity.isFinishing() || activity.isDestroyed()) return;
                            if (ok) {
                                Toast.makeText(activity,
                                        activity.getString(R.string.vault_restored,
                                                backup.getFile().getName()),
                                        Toast.LENGTH_LONG).show();
                                parentDialog.dismiss();
                                if (listener != null) listener.onRomRestored(backup);
                            } else {
                                Toast.makeText(activity,
                                        activity.getString(R.string.vault_restore_failed),
                                        Toast.LENGTH_LONG).show();
                            }
                        });
                    }, "VaultRestore").start();
                })
                .setNegativeButton(R.string.str_cancelar, null)
                .show();
    }

    private static void confirmDelete(Activity activity, SafetyVaultManager vault,
                                      SafetyVaultManager.VaultBackup backup,
                                      Runnable refresh) {
        new AlertDialog.Builder(activity)
                .setTitle(R.string.vault_delete_confirm_title)
                .setMessage(activity.getString(R.string.vault_delete_confirm_msg,
                        backup.getFile().getName()))
                .setPositiveButton(R.string.vault_delete, (d, w) -> {
                    boolean deleted = vault.deleteBackup(backup);
                    Toast.makeText(activity,
                            activity.getString(deleted
                                    ? R.string.vault_deleted
                                    : R.string.vault_delete_failed),
                            Toast.LENGTH_SHORT).show();
                    refresh.run();
                })
                .setNegativeButton(R.string.str_cancelar, null)
                .show();
    }

    private static int dp(int value, float density) {
        return Math.round(value * density);
    }
}
