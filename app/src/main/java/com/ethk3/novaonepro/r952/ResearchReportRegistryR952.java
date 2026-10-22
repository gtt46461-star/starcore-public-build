package com.ethk3.novaonepro.r952;

import com.ethk3.novaonepro.DbStore;
import java.util.LinkedHashMap;
import java.util.Map;

public final class ResearchReportRegistryR952 {
    private ResearchReportRegistryR952() {}
    public static Map<String, String> snapshot(DbStore store) {
        LinkedHashMap<String, String> out = new LinkedHashMap<String, String>();
        out.put("fullblood", FullbloodDiagnosticsProviderR952.render(store));
        out.put("capability_trace", CapabilityTracePageR952.render(store));
        return out;
    }
}
