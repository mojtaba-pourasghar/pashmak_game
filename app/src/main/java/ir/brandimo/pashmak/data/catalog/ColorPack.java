package ir.brandimo.pashmak.data.catalog;

import java.util.Collections;
import java.util.List;

/** A themed set of coloring pages, unlocked as the previous one is finished. */
public final class ColorPack {

    public final String id;
    public final String name;
    public final List<ColoringPage> pages;

    ColorPack(String id, String name, List<ColoringPage> pages) {
        this.id = id;
        this.name = name;
        this.pages = Collections.unmodifiableList(pages);
    }

    public int size() {
        return pages.size();
    }

    public ColoringPage page(int index) {
        return pages.get(Math.max(0, Math.min(index, pages.size() - 1)));
    }
}
