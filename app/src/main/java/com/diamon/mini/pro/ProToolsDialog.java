package com.diamon.mini.pro;

import android.app.Activity;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;

import com.diamon.mini.R;
import com.diamon.mini.billing.BillingManager;

/** Hub for the Pro-only tools available when the pass or purchase is active. */
public class ProToolsDialog {

    public interface Actions {
        void onOpenVault();

        void onOpenIntegrity();

        void onOpenReport();
    }

    public static void show(@NonNull Activity activity,
                            @Nullable BillingManager billingManager,
                            @NonNull ProPassManager proPassManager,
                            @NonNull Actions actions,
                            @Nullable Runnable onProStateChanged) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        float density = activity.getResources().getDisplayMetrics().density;
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20, density), dp(18, density), dp(20, density), dp(16, density));
        root.setBackgroundColor(0xFF181824);

        ScrollView scrollView = new ScrollView(activity);
        scrollView.addView(root);

        AlertDialog dialog = new AlertDialog.Builder(activity)
                .setView(scrollView)
                .setNegativeButton(R.string.str_cerrar, null)
                .create();

        TextView status = new TextView(activity);
        boolean permanent = billingManager != null && billingManager.isProPurchased();
        status.setText(permanent
                ? activity.getString(R.string.pro_tools_status_permanent)
                : activity.getString(R.string.pro_tools_status_pass,
                proPassManager.getTiempoRestanteFormateado()));
        status.setTextSize(13);
        status.setTypeface(null, Typeface.BOLD);
        status.setTextColor(0xFF00E676);
        status.setGravity(Gravity.CENTER);
        status.setPadding(0, 0, 0, dp(14, density));
        root.addView(status);

        if (!permanent) {
            root.addView(createButton(activity, density,
                    activity.getString(R.string.pro_btn_watch_another_video),
                    "", 0xFF00B0FF, () -> {
                        dialog.dismiss();
                        ProAccessDialog.show(activity, billingManager, proPassManager, () -> {
                            if (onProStateChanged != null) onProStateChanged.run();
                        });
                    }));
            root.addView(createSpacer(activity, density));
        }

        if (!permanent && billingManager != null) {
            Button buy = new Button(activity);
            String price = billingManager.getFormattedPrice("$4.99");
            buy.setText(activity.getString(R.string.pro_btn_buy_permanent,
                    activity.getString(R.string.app_name), " — " + price));
            buy.setTextSize(13);
            buy.setTypeface(null, Typeface.BOLD);
            buy.setTextColor(0xFF1E1E2E);
            GradientDrawable buyBg = new GradientDrawable();
            buyBg.setColor(0xFFFFB300);
            buyBg.setCornerRadius(dp(10, density));
            buy.setBackground(buyBg);
            buy.setPadding(dp(12, density), dp(10, density),
                    dp(12, density), dp(10, density));
            buy.setOnClickListener(v -> {
                if (!billingManager.isProductAvailable()) {
                    billingManager.queryProducts();
                    Toast.makeText(activity,
                            activity.getString(R.string.pro_billing_not_available),
                            Toast.LENGTH_SHORT).show();
                    return;
                }
                boolean launched = billingManager.launchPurchaseFlow(activity);
                if (!launched) {
                    int message = billingManager.isPlayInstallAvailable()
                            ? R.string.pro_billing_not_available
                            : R.string.str_billing_not_play_install;
                    Toast.makeText(activity, activity.getString(message),
                            Toast.LENGTH_LONG).show();
                }
            });
            root.addView(buy);
            root.addView(createSpacer(activity, density));
        }

        root.addView(createButton(activity, density,
                activity.getString(R.string.pro_tools_vault),
                "🛡️", 0xFF00897B, actions::onOpenVault));

        root.addView(createSpacer(activity, density));
        root.addView(createButton(activity, density,
                activity.getString(R.string.pro_tools_integrity),
                "🔐", 0xFF1565C0, actions::onOpenIntegrity));

        root.addView(createSpacer(activity, density));
        root.addView(createButton(activity, density,
                activity.getString(R.string.pro_tools_report),
                "📄", 0xFF6A1B9A, actions::onOpenReport));

        TextView hint = new TextView(activity);
        hint.setText(activity.getString(R.string.pro_tools_hex_hint));
        hint.setTextSize(11.5f);
        hint.setTextColor(0xFFB0B0C4);
        hint.setPadding(0, dp(16, density), 0, 0);
        root.addView(hint);

        dialog.show();
    }

    private static Button createButton(Activity activity, float density, String text,
                                       String icon, int color, Runnable action) {
        Button button = new Button(activity);
        button.setText(icon.isEmpty() ? text : icon + "  " + text);
        button.setTextSize(14);
        button.setTypeface(null, Typeface.BOLD);
        button.setTextColor(Color.WHITE);
        button.setGravity(Gravity.START | Gravity.CENTER_VERTICAL);
        GradientDrawable background = new GradientDrawable();
        background.setColor(color);
        background.setCornerRadius(dp(10, density));
        button.setBackground(background);
        button.setPadding(dp(16, density), dp(12, density),
                dp(16, density), dp(12, density));
        button.setOnClickListener(v -> action.run());
        return button;
    }

    private static View createSpacer(Activity activity, float density) {
        View spacer = new View(activity);
        spacer.setMinimumHeight(dp(8, density));
        return spacer;
    }

    private static int dp(int value, float density) {
        return Math.round(value * density);
    }
}
