package com.diamon.mini.ui;

import android.app.Activity;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Typeface;
import android.view.ViewGroup;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.diamon.mini.R;

/** Read-only report viewer with copy and save actions. */
public final class ReportDialog {

    private ReportDialog() {
    }

    public static void show(@NonNull Activity activity,
                            @NonNull String title,
                            @NonNull String body,
                            @Nullable Runnable onSave) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        TextView content = new TextView(activity);
        content.setText(body);
        content.setTextSize(11.5f);
        content.setTextColor(0xFFECEFF1);
        content.setTypeface(Typeface.MONOSPACE);
        content.setTextIsSelectable(true);
        int padding = Math.round(14 * activity.getResources().getDisplayMetrics().density);
        content.setPadding(padding, padding, padding, padding);

        ScrollView scrollView = new ScrollView(activity);
        scrollView.addView(content);

        AlertDialog.Builder builder = new AlertDialog.Builder(activity)
                .setTitle(title)
                .setView(scrollView)
                .setPositiveButton(R.string.report_close, null)
                .setNeutralButton(R.string.report_copy, (d, w) -> {
                    ClipboardManager clipboard =
                            (ClipboardManager) activity.getSystemService(Context.CLIPBOARD_SERVICE);
                    clipboard.setPrimaryClip(ClipData.newPlainText(title, body));
                    Toast.makeText(activity, activity.getString(R.string.report_copied),
                            Toast.LENGTH_SHORT).show();
                });

        if (onSave != null) {
            builder.setNegativeButton(R.string.report_save, (d, w) -> onSave.run());
        }

        AlertDialog dialog = builder.create();
        dialog.show();
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT);
        }
    }
}
