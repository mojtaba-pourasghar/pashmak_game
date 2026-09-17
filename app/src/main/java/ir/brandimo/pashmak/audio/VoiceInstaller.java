package ir.brandimo.pashmak.audio;

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * Gets a Persian voice onto the device.
 *
 * <p>Pointing a parent at Android's speech settings is no use when the device has
 * nothing Persian to select there — the list is the problem, not the setting. What
 * is needed is an engine, and the shortest path to one is the store the family
 * already uses. In Iran that is usually Bazaar rather than Google Play, so Bazaar is
 * tried first, then Play, then the plain web page. None of them needs an account
 * with us and none of it touches the app's own offline promise.
 *
 * <p>The engine offered is eSpeak NG. It is not a beautiful voice — it is a
 * synthesiser, and it sounds like one — but it genuinely speaks Persian, it is free,
 * it is small, and it works with no network afterwards. A plain voice reading the
 * story is worth more to a three-year-old than a handsome silence.
 */
public final class VoiceInstaller {

    /** eSpeak NG: free, offline, and one of the few engines that speaks Persian. */
    public static final String ESPEAK = "com.reecedunn.espeak";
    /** RHVoice, the other one worth having; kinder on the ear where it has a voice. */
    public static final String RHVOICE = "com.github.olga_yakovleva.rhvoice.android";

    private static final String BAZAAR_APP = "com.farsitel.bazaar";

    private VoiceInstaller() {
    }

    /** True once an engine we know about is on the device. */
    public static boolean isInstalled(@NonNull Context context, @NonNull String pkg) {
        try {
            context.getPackageManager().getPackageInfo(pkg, 0);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * Opens the store on the engine's page. Returns the store it managed to open, or
     * null if it could open nothing — the caller says so rather than leaving a button
     * that appears to do nothing.
     */
    @Nullable
    public static String open(@NonNull Context context, @NonNull String pkg) {
        if (isInstalled(context, BAZAAR_APP)) {
            Intent bazaar = new Intent(Intent.ACTION_VIEW,
                    Uri.parse("bazaar://details?id=" + pkg));
            bazaar.setPackage(BAZAAR_APP);
            if (start(context, bazaar)) {
                return BAZAAR_APP;
            }
        }
        Intent play = new Intent(Intent.ACTION_VIEW,
                Uri.parse("market://details?id=" + pkg));
        if (start(context, play)) {
            return "play";
        }
        Intent web = new Intent(Intent.ACTION_VIEW, Uri.parse(
                "https://play.google.com/store/apps/details?id=" + pkg));
        if (start(context, web)) {
            return "web";
        }
        return null;
    }

    /**
     * Launches an engine that is already installed, so the parent lands on its own
     * screen — which is where its Persian voice data is downloaded — instead of
     * having to find it in the launcher.
     */
    public static boolean launch(@NonNull Context context, @NonNull String pkg) {
        Intent open = context.getPackageManager().getLaunchIntentForPackage(pkg);
        if (open == null) {
            return false;
        }
        open.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        return start(context, open);
    }

    private static boolean start(@NonNull Context context, @NonNull Intent intent) {
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        try {
            context.startActivity(intent);
            return true;
        } catch (ActivityNotFoundException | SecurityException e) {
            return false;
        }
    }
}
