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

import com.diamon.mini.ConsentManager;
import com.diamon.mini.R;
import com.diamon.mini.ads.RewardedAdManager;
import com.diamon.mini.billing.BillingManager;

/**
 * Modal used by every Pro-gated feature.
 *
 * <p>Offers the two access paths: the real rewarded AdMob video for a
 * 15-minute pass, or the permanent one-time Google Play purchase
 * ({@code eeprom_flasher_pro}, $4.99 USD).</p>
 */
public class ProAccessDialog {

    private static final int COLOR_BG = 0xFF181824;
    private static final int COLOR_CARD = 0xFF222234;
    private static final int COLOR_ACCENT_CYAN = 0xFF00B0FF;
    private static final int COLOR_ACCENT_GOLD = 0xFFFFB300;
    private static final int COLOR_TEXT_PRIMARY = 0xFFFFFFFF;
    private static final int COLOR_TEXT_SECONDARY = 0xFFB0B0C4;
    private static final int COLOR_CHECK = 0xFF00E676;

    public interface OnProUnlockedListener {
        void onProUnlocked();
    }

    public static void show(@NonNull Activity activity,
                            @Nullable BillingManager billingManager,
                            @NonNull ProPassManager proPassManager,
                            @Nullable OnProUnlockedListener onProUnlocked) {
        show(activity, billingManager, proPassManager, onProUnlocked, null);
    }

    /**
     * Same as {@link #show(Activity, BillingManager, ProPassManager, OnProUnlockedListener)}
     * plus an optional action to open the Pro tools hub while the pass is active.
     */
    public static void show(@NonNull Activity activity,
                            @Nullable BillingManager billingManager,
                            @NonNull ProPassManager proPassManager,
                            @Nullable OnProUnlockedListener onProUnlocked,
                            @Nullable Runnable onOpenProTools) {
        if (activity.isFinishing() || activity.isDestroyed()) return;

        boolean permanent = billingManager != null && billingManager.isProPurchased();
        if (permanent) {
            new AlertDialog.Builder(activity)
                    .setTitle(R.string.pro_permanent_active_title)
                    .setMessage(R.string.pro_permanent_active_desc)
                    .setPositiveButton(R.string.str_cerrar, null)
                    .show();
            if (onProUnlocked != null) onProUnlocked.onProUnlocked();
            return;
        }

        // With a temporary pass the dialog still offers the rewarded video;
        // only the permanent purchase hides the rewarded option.
        boolean passActive = proPassManager.hasActiveProPass();

        if (billingManager != null && !billingManager.isProductAvailable()) {
            billingManager.queryProducts();
        }

        RewardedAdManager rewardedAdManager = new RewardedAdManager(activity);
        ConsentManager.requestConsent(activity, canRequestAds -> {
            if (canRequestAds) rewardedAdManager.load();
        });

        AlertDialog.Builder builder = new AlertDialog.Builder(activity);
        float density = activity.getResources().getDisplayMetrics().density;

        ScrollView scrollView = new ScrollView(activity);
        LinearLayout root = new LinearLayout(activity);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20, density), dp(20, density), dp(20, density), dp(16, density));
        root.setBackgroundColor(COLOR_BG);

        String appName = activity.getString(R.string.app_name);

        TextView title = new TextView(activity);
        title.setText(activity.getString(R.string.pro_access_title, appName));
        title.setTextSize(22);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(COLOR_ACCENT_GOLD);
        title.setGravity(Gravity.CENTER);
        root.addView(title);

        TextView subtitle = new TextView(activity);
        subtitle.setText(activity.getString(R.string.pro_access_subtitle, appName));
        subtitle.setTextSize(13);
        subtitle.setTextColor(COLOR_TEXT_SECONDARY);
        subtitle.setGravity(Gravity.CENTER);
        subtitle.setPadding(0, dp(6, density), 0, dp(16, density));
        root.addView(subtitle);

        if (passActive) {
            TextView passStatus = new TextView(activity);
            passStatus.setText(activity.getString(R.string.pro_pass_active_desc,
                    proPassManager.getTiempoRestanteFormateado()));
            passStatus.setTextSize(13);
            passStatus.setTypeface(null, Typeface.BOLD);
            passStatus.setTextColor(COLOR_CHECK);
            passStatus.setGravity(Gravity.CENTER);
            passStatus.setPadding(0, 0, 0, dp(12, density));
            root.addView(passStatus);
        }

        LinearLayout card = new LinearLayout(activity);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(16, density), dp(16, density), dp(16, density), dp(16, density));
        GradientDrawable cardBg = new GradientDrawable();
        cardBg.setColor(COLOR_CARD);
        cardBg.setCornerRadius(dp(12, density));
        cardBg.setStroke(dp(1, density), 0xFF35354D);
        card.setBackground(cardBg);

        String[] features = new String[]{
                activity.getString(R.string.pro_feature_antibrick_vault),
                activity.getString(R.string.pro_feature_crypto_checker),
                activity.getString(R.string.pro_feature_live_hex_editor),
                activity.getString(R.string.pro_feature_advanced_diff),
                activity.getString(R.string.pro_feature_technical_report),
                activity.getString(R.string.pro_feature_zero_ads)
        };

        for (String feature : features) {
            LinearLayout row = new LinearLayout(activity);
            row.setOrientation(LinearLayout.HORIZONTAL);
            row.setPadding(0, dp(4, density), 0, dp(4, density));

            TextView check = new TextView(activity);
            check.setText("✓ ");
            check.setTextSize(14);
            check.setTypeface(null, Typeface.BOLD);
            check.setTextColor(COLOR_CHECK);

            TextView text = new TextView(activity);
            text.setText(feature);
            text.setTextSize(12.5f);
            text.setTextColor(COLOR_TEXT_PRIMARY);

            row.addView(check);
            row.addView(text);
            card.addView(row);
        }
        root.addView(card);

        View spacer = new View(activity);
        spacer.setMinimumHeight(dp(18, density));
        root.addView(spacer);

        AlertDialog dialog = builder.setView(scrollView).create();
        final BillingManager.BillingListener proWatcher = new BillingManager.BillingListener() {
            @Override
            public void onProStatusChanged(boolean isProPurchased) {
                if (isProPurchased && dialog.isShowing()
                        && !activity.isFinishing() && !activity.isDestroyed()) {
                    // Pro was just restored or purchased: never keep offering videos.
                    dialog.dismiss();
                    if (onProUnlocked != null) onProUnlocked.onProUnlocked();
                }
            }
        };
        if (billingManager != null) billingManager.addListener(proWatcher);
        dialog.setOnDismissListener(d -> {
            if (billingManager != null) billingManager.removeListener(proWatcher);
            rewardedAdManager.destroy();
        });

        final boolean[] rewarded = {false};

        Button btnRewarded = new Button(activity);
        btnRewarded.setText(activity.getString(R.string.pro_btn_watch_rewarded_video));
        btnRewarded.setTextSize(14);
        btnRewarded.setTypeface(null, Typeface.BOLD);
        btnRewarded.setTextColor(Color.WHITE);
        GradientDrawable rewardBg = new GradientDrawable();
        rewardBg.setColor(COLOR_ACCENT_CYAN);
        rewardBg.setCornerRadius(dp(8, density));
        btnRewarded.setBackground(rewardBg);
        btnRewarded.setPadding(dp(12, density), dp(12, density),
                dp(12, density), dp(12, density));

        btnRewarded.setOnClickListener(v -> {
            if (billingManager != null && billingManager.isProPurchased()) {
                // Permanent Pro was restored while this dialog was open.
                dialog.dismiss();
                if (onProUnlocked != null) onProUnlocked.onProUnlocked();
                return;
            }
            if (!rewardedAdManager.canRequestAds()) {
                Toast.makeText(activity, activity.getString(R.string.pro_ads_consent_required),
                        Toast.LENGTH_LONG).show();
                return;
            }
            btnRewarded.setEnabled(false);
            if (!rewardedAdManager.isLoaded()) {
                btnRewarded.setText(activity.getString(R.string.pro_loading_rewarded_video));
            }

            rewardedAdManager.show(activity, new RewardedAdManager.RewardCallback() {
                @Override
                public void onRewarded() {
                    rewarded[0] = true;
                    proPassManager.activarPase();
                    Toast.makeText(activity,
                            activity.getString(R.string.pro_pass_activated_toast, appName),
                            Toast.LENGTH_LONG).show();
                    if (onProUnlocked != null) onProUnlocked.onProUnlocked();
                    dialog.dismiss();
                }

                @Override
                public void onDismissed() {
                    if (rewarded[0]) return;
                    btnRewarded.setEnabled(true);
                    btnRewarded.setText(activity.getString(R.string.pro_btn_watch_rewarded_video));
                }
            });
        });
        root.addView(btnRewarded);

        View buttonSpacer = new View(activity);
        buttonSpacer.setMinimumHeight(dp(10, density));
        root.addView(buttonSpacer);

        Button btnBuy = new Button(activity);
        String price = billingManager != null
                ? billingManager.getFormattedPrice("$4.99")
                : "$4.99";
        btnBuy.setText(activity.getString(R.string.pro_btn_buy_permanent, appName,
                " — " + price));
        btnBuy.setTextSize(14);
        btnBuy.setTypeface(null, Typeface.BOLD);
        btnBuy.setTextColor(0xFF1E1E2E);
        GradientDrawable buyBg = new GradientDrawable();
        buyBg.setColor(COLOR_ACCENT_GOLD);
        buyBg.setCornerRadius(dp(8, density));
        btnBuy.setBackground(buyBg);
        btnBuy.setPadding(dp(12, density), dp(12, density),
                dp(12, density), dp(12, density));

        btnBuy.setOnClickListener(v -> {
            if (billingManager == null) {
                Toast.makeText(activity, activity.getString(R.string.pro_billing_not_available),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            if (!billingManager.isProductAvailable()) {
                billingManager.queryProducts();
                Toast.makeText(activity, activity.getString(R.string.pro_billing_not_available),
                        Toast.LENGTH_SHORT).show();
                return;
            }
            boolean launched = billingManager.launchPurchaseFlow(activity);
            if (launched) {
                dialog.dismiss();
            } else {
                int message = billingManager.isPlayInstallAvailable()
                        ? R.string.pro_billing_not_available
                        : R.string.str_billing_not_play_install;
                Toast.makeText(activity, activity.getString(message), Toast.LENGTH_LONG).show();
            }
        });
        root.addView(btnBuy);

        if (passActive && onOpenProTools != null) {
            View toolsSpacer = new View(activity);
            toolsSpacer.setMinimumHeight(dp(10, density));
            root.addView(toolsSpacer);

            Button btnTools = new Button(activity);
            btnTools.setText(activity.getString(R.string.pro_open_tools));
            btnTools.setTextSize(14);
            btnTools.setTypeface(null, Typeface.BOLD);
            btnTools.setTextColor(Color.WHITE);
            GradientDrawable toolsBg = new GradientDrawable();
            toolsBg.setColor(0xFF00897B);
            toolsBg.setCornerRadius(dp(8, density));
            btnTools.setBackground(toolsBg);
            btnTools.setPadding(dp(12, density), dp(12, density),
                    dp(12, density), dp(12, density));
            btnTools.setOnClickListener(v -> {
                dialog.dismiss();
                onOpenProTools.run();
            });
            root.addView(btnTools);
        }

        TextView restoreNote = new TextView(activity);
        restoreNote.setText(activity.getString(R.string.pro_restore_note));
        restoreNote.setTextSize(11);
        restoreNote.setTextColor(COLOR_TEXT_SECONDARY);
        restoreNote.setGravity(Gravity.CENTER);
        restoreNote.setPadding(0, dp(10, density), 0, 0);
        root.addView(restoreNote);

        Button btnCancel = new Button(activity);
        btnCancel.setText(activity.getString(R.string.str_cancelar));
        btnCancel.setTextSize(13);
        btnCancel.setTextColor(COLOR_TEXT_SECONDARY);
        btnCancel.setBackgroundColor(Color.TRANSPARENT);
        btnCancel.setPadding(0, dp(10, density), 0, dp(4, density));
        btnCancel.setOnClickListener(v -> dialog.dismiss());
        root.addView(btnCancel);

        scrollView.addView(root);
        dialog.show();
    }

    private static int dp(int value, float density) {
        return Math.round(value * density);
    }
}
