package ir.brandimo.pashmak.data.catalog;

import android.content.Context;
import android.content.res.Resources;
import android.content.res.TypedArray;

import androidx.annotation.NonNull;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import ir.brandimo.pashmak.R;

/**
 * The 20 missions are immutable content, so they live in resources and are read
 * once — only the child's progress goes to the database.
 */
public final class MissionCatalog {

    private static volatile List<Mission> cache;

    private MissionCatalog() {
    }

    @NonNull
    public static List<Mission> all(@NonNull Context context) {
        List<Mission> local = cache;
        if (local != null) {
            return local;
        }
        synchronized (MissionCatalog.class) {
            if (cache == null) {
                cache = load(context.getApplicationContext().getResources());
            }
            return cache;
        }
    }

    @NonNull
    public static Mission get(@NonNull Context context, int index) {
        List<Mission> missions = all(context);
        if (missions.isEmpty()) {
            return new Mission(0, "", new ArrayList<String>());
        }
        int clamped = Math.max(0, Math.min(index, missions.size() - 1));
        return missions.get(clamped);
    }

    public static int count(@NonNull Context context) {
        return all(context).size();
    }

    private static List<Mission> load(Resources res) {
        String[] titles = res.getStringArray(R.array.mission_titles);
        TypedArray itemArrays = res.obtainTypedArray(R.array.mission_items);
        List<Mission> missions = new ArrayList<>(titles.length);
        try {
            for (int i = 0; i < titles.length; i++) {
                int arrayId = itemArrays.getResourceId(i, 0);
                String[] items = arrayId == 0 ? new String[0] : res.getStringArray(arrayId);
                missions.add(new Mission(i, titles[i], new ArrayList<>(Arrays.asList(items))));
            }
        } finally {
            itemArrays.recycle();
        }
        return Collections.unmodifiableList(missions);
    }
}
