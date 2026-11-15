package com.ethk3.novaonepro.r952;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class ResearchPageRegistryR952 {
    private ResearchPageRegistryR952() {}
    public static List<String> pageIds() {
        ArrayList<String> pages = new ArrayList<String>();
        pages.add("r952_fullblood"); pages.add("r952_capability_trace");
        pages.add("deep");pages.add("evolution");pages.add("creation");pages.add("atoms");
        return Collections.unmodifiableList(pages);
    }
}
