package com.ethk3.novaonepro;

import java.util.LinkedHashMap;
import java.util.Map;

/** Small deterministic cache for immutable current-target state trajectories. */
public final class V50StateHistoryCache {
    private static final int CAPACITY = 48;
    private final LinkedHashMap<String, V50Models.StateTrajectorySnapshot[]> cache =
            new LinkedHashMap<String, V50Models.StateTrajectorySnapshot[]>(CAPACITY, 0.75f, true) {
                protected boolean removeEldestEntry(
                        Map.Entry<String, V50Models.StateTrajectorySnapshot[]> eldest) {
                    return size() > CAPACITY;
                }
            };

    public synchronized V50Models.StateTrajectorySnapshot[] get(String key) {
        V50Models.StateTrajectorySnapshot[] value = cache.get(key);
        return value == null ? null : (V50Models.StateTrajectorySnapshot[]) value.clone();
    }

    public synchronized void put(String key, V50Models.StateTrajectorySnapshot[] value) {
        if (key == null || value == null) return;
        cache.put(key, (V50Models.StateTrajectorySnapshot[]) value.clone());
    }
}
