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
import com.google.android.gms.ads.interstitial.InterstitialAd;
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback;

/**
 * Intersticial compartido por {@code MainActivity} y {@code HexViewerActivity}.
 *
 * <p>Antes cada {@code HexViewerActivity} creaba su propio intersticial en
 * {@code onCreate()}: el consentimiento, la inicialización del SDK y la descarga
 * del anuncio ocurrían mientras el usuario abría el visor HEX, así que al
 * cerrarlo en pocos segundos el anuncio todavía no existía y nunca se mostraba
 * (show rate del 44% en AdMob).</p>
 *
 * <p>Ahora el anuncio vive en este gestor estático: se pide al abrir el visor
 * (igual que hace C-PIC) y se conserva en memoria al cambiar de Activity. Si el
 * usuario cierra el visor antes de que llegue, la descarga continúa y el
 * anuncio queda listo para la siguiente vez, en lugar de perderse.</p>
 *
 * <p>Todos los métodos deben llamarse desde el hilo principal.</p>
 */
public final class InterstitialAdManager {

    private static final String TAG = "InterstitialAds";

    /** Bloque "Intestinal de EEPROM Flasher" en AdMob. */
    private static final String AD_UNIT_ID = "ca-app-pub-5141499161332805/6545581216";

    /** Un anuncio cargado se descarta después de una hora para no mostrarlo caducado. */
    private static final long TTL_MS = 60 * 60 * 1000L;

    /** Tiempo mínimo entre dos intersticiales para no castigar al usuario. */
    private static final long COOLDOWN_MS = 3 * 60 * 1000L;

    private static InterstitialAd ad;
    private static long loadedAt;
    private static long lastShownAt;
    private static boolean loading;
    private static boolean sdkInitialized;

    private InterstitialAdManager() {
    }

    /**
     * Pide un intersticial si no hay uno fresco y no hay otra petición en curso.
     * No hace nada si UMP todavía no permite anuncios; el modo Pro lo comprueba
     * el llamador antes de invocar este método.
     */
    public static void preload(@NonNull Context context) {
        if (!ConsentManager.canRequestAds() || isFresh() || loading) {
            return;
        }
        final Context appContext = context.getApplicationContext();
        if (sdkInitialized) {
            load(appContext);
            return;
        }
        sdkInitialized = true;
        MobileAds.initialize(appContext, status -> load(appContext));
    }

    private static void load(@NonNull Context context) {
        if (loading || isFresh() || !ConsentManager.canRequestAds()) {
            return;
        }
        loading = true;
        Log.d(TAG, "Solicitando intersticial...");
        InterstitialAd.load(
                context,
                AD_UNIT_ID,
                new AdRequest.Builder().build(),
                new InterstitialAdLoadCallback() {
                    @Override
                    public void onAdLoaded(@NonNull InterstitialAd interstitialAd) {
                        ad = interstitialAd;
                        loadedAt = System.currentTimeMillis();
                        loading = false;
                        Log.d(TAG, "Intersticial listo.");
                    }

                    @Override
                    public void onAdFailedToLoad(@NonNull LoadAdError loadAdError) {
                        ad = null;
                        loadedAt = 0;
                        loading = false;
                        Log.w(TAG, "No se pudo cargar el intersticial: "
                                + loadAdError.getCode() + " - " + loadAdError.getMessage());
                    }
                });
    }

    private static boolean isFresh() {
        return ad != null
                && loadedAt > 0
                && System.currentTimeMillis() - loadedAt < TTL_MS;
    }

    /** true si hay un anuncio cargado, no caducado y ya pasó el cooldown. */
    public static boolean isReadyToShow() {
        return ConsentManager.canRequestAds()
                && isFresh()
                && System.currentTimeMillis() - lastShownAt >= COOLDOWN_MS;
    }

    /**
     * Muestra el intersticial si está listo y ejecuta {@code onDismissed} al
     * cerrarse (o si el anuncio falla al mostrarse). Devuelve false sin tocar
     * nada cuando no se puede mostrar, para que el llamador continúe su flujo.
     */
    public static boolean showIfAvailable(
            @NonNull Activity activity, @Nullable Runnable onDismissed) {
        if (activity.isFinishing() || activity.isDestroyed() || !isReadyToShow()) {
            return false;
        }

        final InterstitialAd current = ad;
        ad = null;
        loadedAt = 0;
        lastShownAt = System.currentTimeMillis();

        current.setFullScreenContentCallback(new FullScreenContentCallback() {
            @Override
            public void onAdDismissedFullScreenContent() {
                if (onDismissed != null) {
                    onDismissed.run();
                }
            }

            @Override
            public void onAdFailedToShowFullScreenContent(@NonNull AdError adError) {
                Log.w(TAG, "Fallo al mostrar el intersticial: " + adError.getMessage());
                if (onDismissed != null) {
                    onDismissed.run();
                }
            }
        });

        try {
            current.show(activity);
        } catch (RuntimeException e) {
            Log.e(TAG, "Excepción mostrando el intersticial", e);
            if (onDismissed != null) {
                onDismissed.run();
            }
            return false;
        }
        return true;
    }

    /** Descarta el anuncio cargado (por ejemplo al activar el modo Pro). */
    public static void clear() {
        ad = null;
        loadedAt = 0;
    }
}
