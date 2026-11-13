package com.diamon.mini.billing;

import android.app.Activity;
import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.ApplicationInfo;
import android.content.pm.InstallSourceInfo;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.SystemClock;
import android.provider.Settings;
import android.util.Log;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.annotation.VisibleForTesting;

import com.android.billingclient.api.AcknowledgePurchaseParams;
import com.android.billingclient.api.BillingClient;
import com.android.billingclient.api.BillingClientStateListener;
import com.android.billingclient.api.BillingFlowParams;
import com.android.billingclient.api.BillingResult;
import com.android.billingclient.api.PendingPurchasesParams;
import com.android.billingclient.api.ProductDetails;
import com.android.billingclient.api.Purchase;
import com.android.billingclient.api.PurchasesUpdatedListener;
import com.android.billingclient.api.QueryProductDetailsParams;
import com.android.billingclient.api.QueryPurchasesParams;
import com.diamon.mini.R;

import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Google Play Billing 8.3.0 wrapper for the permanent EEPROM Flasher Pro product.
 *
 * <p>The product is a one-time, non-consumable purchase. It is acknowledged after
 * purchase so it remains available forever and can be restored on any device
 * signed into the same Google account.</p>
 */
public class BillingManager implements PurchasesUpdatedListener {

    private static final String TAG = "BillingManager";

    /** Permanent Pro product configured in Play Console. */
    public static final String PRODUCT_ID_PRO = "eeprom_flasher_pro";

    public static final String PREFS_NAME = "billing_prefs";
    public static final String KEY_PRO_USER = "is_pro_user";

    private static final long LAUNCH_DEBOUNCE_MS = 2000L;
    private static final String PLAY_STORE_PACKAGE = "com.android.vending";

    public interface BillingListener {
        default void onProStatusChanged(boolean isProPurchased) {}
        default void onPriceLoaded(String formattedPrice) {}
        default void onPurchaseSuccess(String productId) { onProStatusChanged(true); }
        default void onBillingError(String errorMessage) {}
    }

    private final Context context;
    private final Handler mainHandler = new Handler(Looper.getMainLooper());
    private final BillingClient billingClient;
    private final List<BillingListener> listeners = new CopyOnWriteArrayList<>();
    @Nullable
    private ProductDetails proDetails;
    private final Map<String, ProductDetails> availableProducts = new HashMap<>();
    private boolean serviceConnected = false;
    private boolean proCached = false;
    private long lastLaunchTimeMs = 0L;
    private volatile boolean destroyed = false;
    @Nullable
    private Boolean playInstallOverride;
    @Nullable
    private Boolean playInstallAvailableCache;

    public BillingManager(@NonNull Context context, @Nullable BillingListener listener) {
        this.context = context.getApplicationContext();
        if (listener != null) {
            this.listeners.add(listener);
        }
        this.billingClient = BillingClient.newBuilder(this.context)
                .setListener(this)
                .enablePendingPurchases(
                        PendingPurchasesParams.newBuilder()
                                .enableOneTimeProducts()
                                .build())
                .build();
        this.proCached = isProPurchased();
        startConnection();
    }

    /** Replaces the main billing listener. */
    public void setBillingListener(@Nullable BillingListener listener) {
        listeners.clear();
        if (listener != null) listeners.add(listener);
    }

    /** Adds an extra listener, e.g. an open dialog watching for a restored purchase. */
    public void addListener(@NonNull BillingListener listener) {
        if (!listeners.contains(listener)) listeners.add(listener);
    }

    public void removeListener(@Nullable BillingListener listener) {
        if (listener != null) listeners.remove(listener);
    }

    private void notifyProChanged(boolean isPro) {
        mainHandler.post(() -> {
            if (destroyed) return;
            for (BillingListener current : listeners) {
                try {
                    current.onProStatusChanged(isPro);
                } catch (Exception e) {
                    Log.w(TAG, "Billing listener failed", e);
                }
            }
        });
    }

    /** Connects to Play Billing and refreshes product and purchase state. */
    public void startConnection() {
        if (destroyed) return;
        if (billingClient.isReady()) {
            serviceConnected = true;
            queryActivePurchases();
            queryProducts();
            return;
        }
        billingClient.startConnection(new BillingClientStateListener() {
            @Override
            public void onBillingSetupFinished(@NonNull BillingResult billingResult) {
                if (destroyed) {
                    billingClient.endConnection();
                    return;
                }
                if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                    serviceConnected = true;
                    Log.d(TAG, "Billing connected.");
                    queryActivePurchases();
                    queryProducts();
                } else {
                    serviceConnected = false;
                    Log.w(TAG, "Billing setup failed: " + billingResult.getDebugMessage()
                            + " (code " + billingResult.getResponseCode() + ")");
                }
            }

            @Override
            public void onBillingServiceDisconnected() {
                serviceConnected = false;
                Log.w(TAG, "Billing service disconnected.");
            }
        });
    }

    /** Loads the Pro product details and formatted localized price. */
    public void queryProducts() {
        if (destroyed) return;
        QueryProductDetailsParams.Product product =
                QueryProductDetailsParams.Product.newBuilder()
                        .setProductId(PRODUCT_ID_PRO)
                        .setProductType(BillingClient.ProductType.INAPP)
                        .build();
        QueryProductDetailsParams params = QueryProductDetailsParams.newBuilder()
                .setProductList(Collections.singletonList(product))
                .build();

        billingClient.queryProductDetailsAsync(params, (billingResult, queryResult) -> {
            if (destroyed) return;
            if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK
                    || queryResult == null
                    || queryResult.getProductDetailsList() == null
                    || queryResult.getProductDetailsList().isEmpty()) {
                Log.w(TAG, "Could not load Pro product: " + billingResult.getDebugMessage()
                        + " (code " + billingResult.getResponseCode() + ")");
                return;
            }
            for (ProductDetails details : queryResult.getProductDetailsList()) {
                availableProducts.put(details.getProductId(), details);
                if (PRODUCT_ID_PRO.equals(details.getProductId())) {
                    proDetails = details;
                }
            }
            if (proDetails != null) {
                ProductDetails.OneTimePurchaseOfferDetails offer =
                        proDetails.getOneTimePurchaseOfferDetails();
                if (offer != null) {
                    String price = offer.getFormattedPrice();
                    mainHandler.post(() -> {
                        if (destroyed) return;
                        for (BillingListener current : listeners) {
                            try {
                                current.onPriceLoaded(price);
                            } catch (Exception e) {
                                Log.w(TAG, "Billing listener failed", e);
                            }
                        }
                    });
                }
            }
        });
    }

    /** Returns the localized Play price, or the supplied fallback if not loaded yet. */
    @NonNull
    public String getFormattedPrice(@NonNull String fallbackPrice) {
        if (proDetails != null && proDetails.getOneTimePurchaseOfferDetails() != null) {
            String price = proDetails.getOneTimePurchaseOfferDetails().getFormattedPrice();
            if (price != null && !price.isEmpty()) return price;
        }
        return fallbackPrice;
    }

    /**
     * Google Play is the only installer able to complete the billing flow.
     * Sideloaded builds receive a null PendingIntent from Play and billing 8.x
     * throws before the app can recover, so purchases are blocked there.
     */
    public boolean isPlayInstallAvailable() {
        if (playInstallOverride != null) return playInstallOverride;
        if (playInstallAvailableCache == null) {
            playInstallAvailableCache = detectPlayInstallAvailable();
        }
        return playInstallAvailableCache;
    }

    @SuppressWarnings("deprecation")
    private boolean detectPlayInstallAvailable() {
        try {
            PackageManager pm = context.getPackageManager();
            if (pm == null) return false;

            String installer = null;
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                InstallSourceInfo info = pm.getInstallSourceInfo(context.getPackageName());
                if (info != null) {
                    installer = info.getInstallingPackageName();
                    if (installer == null) installer = info.getInitiatingPackageName();
                }
            } else {
                installer = pm.getInstallerPackageName(context.getPackageName());
            }

            if (!PLAY_STORE_PACKAGE.equals(installer)) {
                Log.w(TAG, "Billing blocked: installer is " + installer);
                return false;
            }
            ApplicationInfo playStore = pm.getApplicationInfo(PLAY_STORE_PACKAGE, 0);
            return playStore != null && playStore.enabled;
        } catch (Exception e) {
            Log.w(TAG, "Billing blocked: unable to verify installer", e);
            return false;
        }
    }

    @VisibleForTesting
    public void setPlayInstallOverride(@Nullable Boolean override) {
        this.playInstallOverride = override;
        this.playInstallAvailableCache = override;
    }

    /** Launches the Play purchase dialog for the permanent Pro product. */
    public boolean launchPurchaseFlow(@NonNull Activity activity) {
        if (destroyed || activity.isFinishing() || activity.isDestroyed()) return false;

        if (isRunningInTestLab(activity)) {
            notifyError("In-app billing is disabled in automated test environments.");
            return false;
        }
        if (!isPlayInstallAvailable()) {
            notifyError(context.getString(R.string.str_billing_not_play_install));
            return false;
        }
        if (!billingClient.isReady()) {
            startConnection();
            notifyError(context.getString(R.string.str_billing_connecting));
            return false;
        }
        if (proDetails == null) {
            queryProducts();
            notifyError(context.getString(R.string.str_billing_not_ready));
            return false;
        }

        long now = SystemClock.elapsedRealtime();
        if (lastLaunchTimeMs > 0L && (now - lastLaunchTimeMs) < LAUNCH_DEBOUNCE_MS) {
            return false;
        }
        lastLaunchTimeMs = now;

        BillingFlowParams.ProductDetailsParams detailsParams =
                BillingFlowParams.ProductDetailsParams.newBuilder()
                        .setProductDetails(proDetails)
                        .build();
        BillingFlowParams flowParams = BillingFlowParams.newBuilder()
                .setProductDetailsParamsList(Collections.singletonList(detailsParams))
                .build();

        final BillingResult result;
        try {
            result = billingClient.launchBillingFlow(activity, flowParams);
        } catch (RuntimeException e) {
            lastLaunchTimeMs = 0L;
            Log.e(TAG, "Failed to launch billing flow", e);
            notifyError("Failed to launch billing flow: " + e.getMessage());
            return false;
        }

        boolean success = result != null
                && result.getResponseCode() == BillingClient.BillingResponseCode.OK;
        if (!success) {
            lastLaunchTimeMs = 0L;
            notifyError(result != null ? result.getDebugMessage() : "Null BillingResult");
        }
        return success;
    }

    private boolean isRunningInTestLab(@NonNull Context ctx) {
        try {
            String value = Settings.System.getString(ctx.getContentResolver(), "firebase.test.lab");
            return "true".equalsIgnoreCase(value);
        } catch (Exception e) {
            return false;
        }
    }

    private void notifyError(String message) {
        Log.w(TAG, message);
        mainHandler.post(() -> {
            if (destroyed) return;
            for (BillingListener current : listeners) {
                try {
                    current.onBillingError(message);
                } catch (Exception e) {
                    Log.w(TAG, "Billing listener failed", e);
                }
            }
        });
    }

    @Override
    public void onPurchasesUpdated(@NonNull BillingResult billingResult,
                                   @Nullable List<Purchase> purchases) {
        if (destroyed) return;
        lastLaunchTimeMs = 0L;
        int code = billingResult.getResponseCode();

        if (code == BillingClient.BillingResponseCode.OK && purchases != null) {
            for (Purchase purchase : purchases) handlePurchase(purchase);
        } else if (code == BillingClient.BillingResponseCode.USER_CANCELED) {
            Log.d(TAG, "Purchase canceled by user.");
        } else if (code == BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED) {
            setProPurchased(true);
            notifyProChanged(true);
        } else {
            notifyError("Purchase failed (" + code + "): " + billingResult.getDebugMessage());
        }
    }

    /** Validates, acknowledges and persists a purchase. */
    public void handlePurchase(@NonNull Purchase purchase) {
        if (!purchase.getProducts().contains(PRODUCT_ID_PRO)) return;
        if (purchase.getPurchaseState() == Purchase.PurchaseState.PENDING) {
            Log.d(TAG, "Purchase pending confirmation.");
            return;
        }
        if (purchase.getPurchaseState() != Purchase.PurchaseState.PURCHASED) return;

        if (!purchase.isAcknowledged()) {
            acknowledgePurchase(purchase);
        } else {
            setProPurchased(true);
            notifyPurchaseSuccess();
        }
    }

    private void notifyPurchaseSuccess() {
        mainHandler.post(() -> {
            if (destroyed) return;
            for (BillingListener current : listeners) {
                try {
                    current.onPurchaseSuccess(PRODUCT_ID_PRO);
                } catch (Exception e) {
                    Log.w(TAG, "Billing listener failed", e);
                }
            }
        });
    }

    private void acknowledgePurchase(@NonNull Purchase purchase) {
        AcknowledgePurchaseParams params = AcknowledgePurchaseParams.newBuilder()
                .setPurchaseToken(purchase.getPurchaseToken())
                .build();
        billingClient.acknowledgePurchase(params, billingResult -> {
            if (billingResult.getResponseCode() == BillingClient.BillingResponseCode.OK) {
                Log.d(TAG, "Purchase acknowledged.");
                setProPurchased(true);
                notifyPurchaseSuccess();
            } else {
                notifyError("Failed to acknowledge purchase: " + billingResult.getDebugMessage());
            }
        });
    }

    /** Restores a previous Pro purchase silently at startup. */
    public void queryActivePurchases() {
        if (destroyed) return;
        QueryPurchasesParams params = QueryPurchasesParams.newBuilder()
                .setProductType(BillingClient.ProductType.INAPP)
                .build();
        billingClient.queryPurchasesAsync(params, (billingResult, purchases) -> {
            if (destroyed) return;
            if (billingResult.getResponseCode() != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "Could not query purchases: " + billingResult.getDebugMessage());
                return;
            }
            if (purchases == null) return;
            boolean found = false;
            for (Purchase purchase : purchases) {
                if (purchase.getProducts().contains(PRODUCT_ID_PRO)
                        && purchase.getPurchaseState() == Purchase.PurchaseState.PURCHASED) {
                    found = true;
                    handlePurchase(purchase);
                    break;
                }
            }
            // Play is authoritative: a successful query without the product means
            // it was refunded, revoked or bought with another account.
            if (!found && isProPurchased()) {
                setProPurchased(false);
            }
        });
    }

    public void setProPurchased(boolean value) {
        boolean previous = isProPurchased();
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .edit()
                .putBoolean(KEY_PRO_USER, value)
                .apply();
        proCached = value;
        if (value != previous) notifyProChanged(value);
    }

    /** True when the permanent Pro purchase is owned or cached locally. */
    public boolean isProPurchased() {
        if (proCached) return true;
        boolean value = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_PRO_USER, false);
        if (value) proCached = true;
        return value;
    }

    /** Static check that does not open a billing connection. */
    public static boolean isProPurchased(@NonNull Context context) {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                .getBoolean(KEY_PRO_USER, false);
    }

    public boolean isProductAvailable() {
        return proDetails != null;
    }

    public boolean isServiceConnected() {
        return serviceConnected;
    }

    public void destroy() {
        destroyed = true;
        mainHandler.removeCallbacksAndMessages(null);
        if (billingClient.isReady()) {
            billingClient.endConnection();
        }
        serviceConnected = false;
        listeners.clear();
    }

    public void onDestroy() {
        destroy();
    }
}
