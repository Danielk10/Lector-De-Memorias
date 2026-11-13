package com.diamon.mini.ads;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import com.diamon.mini.ConsentManager;
import com.google.android.gms.ads.AdError;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.FullScreenContentCallback;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.gms.ads.rewarded.RewardedAd;
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback;

/**
 * Loads and presents the real rewarded ad unit used by the 15-minute Pro pass.
 *
 * <p>The ad is only requested after UMP consent allows it. If the user taps the
 * pass button before the ad is ready, the show request is queued and executed
 * as soon as the load finishes.</p>
 */
public class RewardedAdManager {

    private static final String TAG = "RewardedAdManager";

    /** Real rewarded unit configured in AdMob (not a Google test ID). */
    public static final String REWARDED_AD_UNIT_ID = "ca-app-pub-5141499161332805/3489519872";

    public interface RewardCallback {
        /** Called when the user finished the video and earned the reward. */
        void onRewarded();

        /** Called when the ad was closed without a reward or failed to show. */
        void onDismissed();
    }

    private final Context context;
    @Nullable
    private RewardedAd rewardedAd;
    private boolean loading = false;
    private boolean destroyed = false;
    @Nullable
    private RewardCallback pendingCallback;
    @Nullable
    private Activity pendingActivity;

    public RewardedAdManager(@NonNull Context context) {
        this.context = context.getApplicationContext();
    }

    public boolean canRequestAds() {
        return ConsentManager.canRequestAds();
    }

    public boolean isLoaded() {
        return rewardedAd != null;
    }

    public boolean isLoading() {
        return loading;
    }

    /** Preloads the rewarded video. Safe to call repeatedly. */
    public void load() {
        if (destroyed || loading || rewardedAd != null) return;
        if (!ConsentManager.canRequestAds()) {
            Log.d(TAG, "Rewarded ad not requested: consent not available.");
            notifyPendingDismissed();
            return;
        }

        loading = true;
        MobileAds.initialize(context, status -> {
            if (destroyed) {
                loading = false;
                return;
            }
            AdRequest request = new AdRequest.Builder().build();
            RewardedAd.load(context, REWARDED_AD_UNIT_ID, request,
                    new RewardedAdLoadCallback() {
                        @Override
                        public void onAdLoaded(@NonNull RewardedAd ad) {
                            loading = false;
                            if (destroyed) return;
                            rewardedAd = ad;
                            Log.d(TAG, "Rewarded ad loaded.");
                            RewardCallback pending = pendingCallback;
                            Activity activity = pendingActivity;
                            if (pending != null && activity != null
                                    && !activity.isFinishing() && !activity.isDestroyed()) {
                                // Show deferred request as soon as the ad is ready.
                                present(activity, pending);
                            } else {
                                notifyPendingDismissed();
                            }
                        }

                        @Override
                        public void onAdFailedToLoad(@NonNull LoadAdError error) {
                            loading = false;
                            rewardedAd = null;
                            Log.w(TAG, "Rewarded ad failed to load: " + error.getMessage());
                            notifyPendingDismissed();
                        }
                    });
        });
    }

    /**
     * Shows the rewarded ad. If it is not loaded yet the request is queued and
     * {@link RewardCallback#onDismissed()} is invoked if loading fails.
     */
    public void show(@NonNull Activity activity, @NonNull RewardCallback callback) {
        if (destroyed) {
            callback.onDismissed();
            return;
        }
        if (!ConsentManager.canRequestAds()) {
            callback.onDismissed();
            return;
        }
        if (rewardedAd != null) {
            pendingCallback = null;
            pendingActivity = null;
            present(activity, callback);
            return;
        }
        pendingCallback = callback;
        pendingActivity = activity;
        load();
    }

    private void present(@NonNull Activity activity, @NonNull RewardCallback callback) {
        final RewardedAd ad = rewardedAd;
        if (ad == null) {
            callback.onDismissed();
            return;
        }
        rewardedAd = null;
        pendingCallback = null;
        pendingActivity = null;

        final boolean[] rewarded = {false};
        ad.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                if (!rewarded[0]) callback.onDismissed();
                if (!destroyed) load();
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.w(TAG, "Rewarded ad failed to show: " + adError.getMessage());
                if (!rewarded[0]) callback.onDismissed();
                if (!destroyed) load();
            }
        });

        ad.show(activity, rewardItem -> {
            rewarded[0] = true;
            callback.onRewarded();
        });
    }

    private void notifyPendingDismissed() {
        RewardCallback pending = pendingCallback;
        pendingCallback = null;
        pendingActivity = null;
        if (pending != null) pending.onDismissed();
    }

    public void destroy() {
        destroyed = true;
        pendingCallback = null;
        pendingActivity = null;
        rewardedAd = null;
    }
}
