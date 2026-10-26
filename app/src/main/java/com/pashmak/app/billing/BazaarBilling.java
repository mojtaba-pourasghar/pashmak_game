package com.pashmak.app.billing;

import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Handler;
import android.os.Looper;

import androidx.activity.ComponentActivity;
import androidx.annotation.MainThread;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;

import java.util.ArrayList;
import java.util.List;

import com.pashmak.app.BuildConfig;
import com.pashmak.app.data.prefs.GamePrefs;

import ir.cafebazaar.poolakey.Connection;
import ir.cafebazaar.poolakey.Payment;
import ir.cafebazaar.poolakey.config.PaymentConfiguration;
import ir.cafebazaar.poolakey.config.SecurityCheck;
import ir.cafebazaar.poolakey.entity.PurchaseInfo;
import ir.cafebazaar.poolakey.entity.PurchaseState;
import ir.cafebazaar.poolakey.request.PurchaseRequest;
import kotlin.Unit;

/**
 * The full version, bought once through Cafe Bazaar with Poolakey.
 *
 * <p>Whether it is owned is remembered in {@link GamePrefs}, so the app opens
 * unlocked with no network and no Bazaar. Bazaar is asked again on every launch and
 * its answer replaces what was remembered: a refund locks the app again, and a
 * reinstall finds the purchase without anyone pressing "restore".
 *
 * <p>Each trip to Bazaar opens its own connection and closes it when the answer is
 * in. Poolakey shuts its worker thread down on disconnect, so a {@link Payment} is
 * never reused, and an idle app holds no binding to Bazaar's service.
 *
 * <p>Poolakey may call back from its own thread; every callback here is posted to
 * the main thread before it touches any state.
 */
public final class BazaarBilling {

    /** The product defined in Bazaar's developer panel: in-app, not consumable. */
    public static final String SKU_FULL = "pashmak_full";

    private static final String BAZAAR_APP = "com.farsitel.bazaar";

    public enum Result { PURCHASED, CANCELED, NO_BAZAAR, FAILED }

    public interface PurchaseListener {
        void onPurchaseResult(@NonNull Result result);
    }

    public interface RefreshListener {
        /**
         * @param checked whether Bazaar actually answered
         * @param owned   what it said, or what was remembered when it did not answer
         */
        void onRefreshed(boolean checked, boolean owned);
    }

    private interface Ready {
        void run(@NonNull Payment payment);
    }

    /** One trip to Bazaar: it reports exactly once and always lets go of the connection. */
    private final class Trip {
        @Nullable
        private Connection connection;
        private boolean over;

        /** True for the first caller only; that caller reports the outcome. */
        boolean close() {
            if (over) {
                return false;
            }
            over = true;
            final Connection open = connection;
            connection = null;
            if (open != null) {
                // Never from inside Poolakey's own callback: disconnecting tears down
                // the thread that callback may have come from.
                main.post(() -> {
                    try {
                        open.disconnect();
                    } catch (RuntimeException ignored) {
                        // Already gone; nothing to release.
                    }
                });
            }
            return true;
        }
    }

    private static volatile BazaarBilling instance;

    private final Context appContext;
    private final GamePrefs prefs;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final List<RefreshListener> waiting = new ArrayList<>();

    private boolean refreshing;
    @Nullable
    private Trip purchaseTrip;
    /**
     * Bazaar's payment screen is open and its answer has not come back. If the
     * screen that started it was rebuilt in the meantime the answer is lost with it,
     * so the next screen to resume asks Bazaar instead.
     */
    private boolean awaitingPaymentScreen;

    private BazaarBilling(@NonNull Context context) {
        appContext = context.getApplicationContext();
        prefs = GamePrefs.get(appContext);
    }

    @NonNull
    public static BazaarBilling get(@NonNull Context context) {
        BazaarBilling local = instance;
        if (local == null) {
            synchronized (BazaarBilling.class) {
                if (instance == null) {
                    instance = new BazaarBilling(context);
                }
                local = instance;
            }
        }
        return local;
    }

    @NonNull
    public LiveData<Boolean> ownedLive() {
        return prefs.fullUnlockedLive();
    }

    public boolean owned() {
        return prefs.isFullUnlocked();
    }

    public boolean isBazaarInstalled() {
        try {
            appContext.getPackageManager().getPackageInfo(BAZAAR_APP, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Asks Bazaar what this account owns and remembers the answer. Calls that arrive
     * while one is already on its way share its answer.
     */
    @MainThread
    public void refresh(@Nullable RefreshListener listener) {
        if (listener != null) {
            waiting.add(listener);
        }
        if (refreshing) {
            return;
        }
        refreshing = true;
        final Trip trip = new Trip();
        connect(trip, payment -> payment.getPurchasedProducts(query -> {
            query.querySucceed(purchases -> {
                main.post(() -> {
                    if (trip.close()) {
                        boolean owned = ownsFull(purchases);
                        prefs.setFullUnlocked(owned);
                        finishRefresh(true, owned);
                    }
                });
                return Unit.INSTANCE;
            });
            query.queryFailed(error -> {
                main.post(() -> {
                    if (trip.close()) {
                        finishRefresh(false, owned());
                    }
                });
                return Unit.INSTANCE;
            });
            return Unit.INSTANCE;
        }), () -> {
            if (trip.close()) {
                finishRefresh(false, owned());
            }
        });
    }

    private void finishRefresh(boolean checked, boolean owned) {
        refreshing = false;
        List<RefreshListener> listeners = new ArrayList<>(waiting);
        waiting.clear();
        for (RefreshListener listener : listeners) {
            listener.onRefreshed(checked, owned);
        }
    }

    /**
     * Opens Bazaar's payment screen for the full version. Only one purchase runs at
     * a time; a second tap while Bazaar is already up is ignored.
     */
    @MainThread
    public void purchase(@NonNull ComponentActivity activity,
                         @NonNull PurchaseListener listener) {
        if (purchaseTrip != null) {
            return;
        }
        final Trip trip = new Trip();
        purchaseTrip = trip;
        connect(trip, payment -> payment.purchaseProduct(
                activity.getActivityResultRegistry(),
                new PurchaseRequest(SKU_FULL, null, null),
                purchase -> {
                    purchase.purchaseFlowBegan(() -> {
                        main.post(() -> {
                            if (!trip.over) {
                                awaitingPaymentScreen = true;
                            }
                        });
                        return Unit.INSTANCE;
                    });
                    purchase.failedToBeginFlow(error -> {
                        // Bazaar also refuses to start a purchase of something the
                        // account already owns; asking what it owns settles that.
                        main.post(() -> endPurchase(trip, Result.FAILED, listener, true));
                        return Unit.INSTANCE;
                    });
                    purchase.purchaseSucceed(info -> {
                        main.post(() -> {
                            // Poolakey has already checked the signature, so this
                            // counts even if the trip was given up on meanwhile.
                            boolean bought = isFull(info);
                            if (bought) {
                                prefs.setFullUnlocked(true);
                            }
                            endPurchase(trip, bought ? Result.PURCHASED : Result.FAILED,
                                    listener, !bought);
                        });
                        return Unit.INSTANCE;
                    });
                    purchase.purchaseCanceled(() -> {
                        main.post(() -> endPurchase(trip, Result.CANCELED, listener, false));
                        return Unit.INSTANCE;
                    });
                    purchase.purchaseFailed(error -> {
                        main.post(() -> endPurchase(trip, Result.FAILED, listener, true));
                        return Unit.INSTANCE;
                    });
                    return Unit.INSTANCE;
                }), () -> {
            // If Bazaar's screen was already up, the purchase may have gone through
            // with its answer lost along with the connection: ask again.
            boolean wasPaying = purchaseTrip == trip && awaitingPaymentScreen;
            endPurchase(trip, isBazaarInstalled() ? Result.FAILED : Result.NO_BAZAAR,
                    listener, wasPaying);
        });
    }

    /** Reports a purchase's outcome once, however many of Poolakey's callbacks fire. */
    private void endPurchase(@NonNull Trip trip, @NonNull Result result,
                             @NonNull PurchaseListener listener, boolean recheck) {
        if (!trip.close()) {
            return;
        }
        if (purchaseTrip == trip) {
            purchaseTrip = null;
            awaitingPaymentScreen = false;
        }
        listener.onPurchaseResult(result);
        if (recheck) {
            refresh(null);
        }
    }

    /**
     * Every screen calls this when it resumes. Normally it does nothing; it only
     * acts when Bazaar's payment screen was left and its answer never reached the
     * screen that asked — because that screen was rebuilt while Bazaar was up.
     */
    @MainThread
    public void onScreenResumed() {
        if (!awaitingPaymentScreen) {
            return;
        }
        // Give a live answer a moment to land first; it normally arrives just
        // before the screen underneath resumes.
        main.postDelayed(() -> {
            if (!awaitingPaymentScreen) {
                return;
            }
            awaitingPaymentScreen = false;
            Trip stale = purchaseTrip;
            purchaseTrip = null;
            if (stale != null) {
                stale.close();
            }
            refresh(null);
        }, 1500L);
    }

    /**
     * Connects and hands over the {@link Payment} once Bazaar is ready. A failure to
     * connect, or the connection dropping before the trip is over, runs
     * {@code failed}; callers make that safe to run late through {@link Trip#close}.
     */
    private void connect(@NonNull Trip trip, @NonNull Ready ready, @NonNull Runnable failed) {
        try {
            final Payment payment = new Payment(appContext, configuration());
            trip.connection = payment.connect(callback -> {
                callback.connectionSucceed(() -> {
                    main.post(() -> {
                        if (!trip.over) {
                            ready.run(payment);
                        }
                    });
                    return Unit.INSTANCE;
                });
                callback.connectionFailed(error -> {
                    main.post(failed);
                    return Unit.INSTANCE;
                });
                callback.disconnected(() -> {
                    main.post(failed);
                    return Unit.INSTANCE;
                });
                return Unit.INSTANCE;
            });
        } catch (RuntimeException e) {
            main.post(failed);
        }
    }

    /**
     * Purchases are signature-checked on the device against the RSA key from Bazaar's
     * panel. The build refuses to make a release without one; a debug build without
     * it skips the check so the flow can still be tried out.
     */
    @NonNull
    private static PaymentConfiguration configuration() {
        String key = BuildConfig.BAZAAR_RSA_KEY;
        SecurityCheck check = key.isEmpty()
                ? SecurityCheck.Disable.INSTANCE
                : new SecurityCheck.Enable(key);
        // Nothing is sold as a subscription, so an older Bazaar without them is fine.
        return new PaymentConfiguration(check, false);
    }

    /** Kotlin's List<PurchaseInfo> reaches Java as a wildcard type. */
    private static boolean ownsFull(@NonNull List<? extends PurchaseInfo> purchases) {
        for (PurchaseInfo info : purchases) {
            if (isFull(info)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isFull(@NonNull PurchaseInfo info) {
        return SKU_FULL.equals(info.getProductId())
                && info.getPurchaseState() == PurchaseState.PURCHASED;
    }
}
