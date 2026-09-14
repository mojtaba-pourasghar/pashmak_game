package ir.brandimo.pashmak.data.repo;

import android.os.Handler;
import android.os.Looper;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

/** One background thread for disk work, plus a main-thread poster. */
public final class AppExecutors {

    private static final Executor DISK = Executors.newSingleThreadExecutor();
    private static final Executor IMAGING = Executors.newSingleThreadExecutor();
    private static final Handler MAIN = new Handler(Looper.getMainLooper());

    private AppExecutors() {
    }

    public static Executor disk() {
        return DISK;
    }

    /** Kept separate so a long extraction never blocks a database write. */
    public static Executor imaging() {
        return IMAGING;
    }

    public static void main(Runnable task) {
        MAIN.post(task);
    }

    public static void mainDelayed(Runnable task, long delayMs) {
        MAIN.postDelayed(task, delayMs);
    }

    public static void cancelMain(Runnable task) {
        MAIN.removeCallbacks(task);
    }
}
