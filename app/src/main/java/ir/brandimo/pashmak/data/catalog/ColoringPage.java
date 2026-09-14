package ir.brandimo.pashmak.data.catalog;

import java.util.Collections;
import java.util.List;

/** A pre-drawn picture the child colors region by region. */
public final class ColoringPage {

    public final String name;
    public final List<ColorRegion> regions;

    ColoringPage(String name, List<ColorRegion> regions) {
        this.name = name;
        this.regions = Collections.unmodifiableList(regions);
    }

    public int size() {
        return regions.size();
    }

    public ColorRegion byId(String id) {
        for (int i = 0; i < regions.size(); i++) {
            if (regions.get(i).id.equals(id)) {
                return regions.get(i);
            }
        }
        return null;
    }
}
