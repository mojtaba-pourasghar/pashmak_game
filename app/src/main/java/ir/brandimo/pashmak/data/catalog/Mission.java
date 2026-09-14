package ir.brandimo.pashmak.data.catalog;

import java.util.Collections;
import java.util.List;

/** One themed collection the child builds by drawing its items on paper. */
public final class Mission {

    public final int index;
    public final String title;
    public final List<String> items;

    Mission(int index, String title, List<String> items) {
        this.index = index;
        this.title = title;
        this.items = Collections.unmodifiableList(items);
    }

    public int size() {
        return items.size();
    }

    public String itemAt(int slot) {
        if (items.isEmpty()) {
            return "";
        }
        int clamped = Math.max(0, Math.min(slot, items.size() - 1));
        return items.get(clamped);
    }

    /** The item the child should draw next, given how many are already captured. */
    public String currentItem(int captured) {
        return itemAt(captured);
    }

    public boolean isComplete(int captured) {
        return captured >= items.size();
    }

    public String itemsLine() {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < items.size(); i++) {
            if (i > 0) {
                sb.append(" · ");
            }
            sb.append(items.get(i));
        }
        return sb.toString();
    }
}
