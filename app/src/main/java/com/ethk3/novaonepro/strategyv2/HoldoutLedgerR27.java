package com.ethk3.novaonepro.strategyv2;

import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

/** R27-016 holdout exposure ledger (docs/03 3.6). Every distinct (block, policy) look is recorded; a block seen by
 * more than {@link #MAX_DISTINCT_LOOKS} distinct policies is degraded into H_SELECT and can no longer verify anything. */
public final class HoldoutLedgerR27 {
    public static final int MAX_DISTINCT_LOOKS = 64;
    private final Map<Long, Set<String>> looks = new HashMap<>();

    /** Records a look; returns true when the look is new. */
    public synchronized boolean look(long block, String policyHash) {
        return looks.computeIfAbsent(block, k -> new LinkedHashSet<>()).add(policyHash);
    }
    public synchronized boolean seen(long block, String policyHash) { Set<String> s = looks.get(block); return s != null && s.contains(policyHash); }
    public synchronized int exposures(long block) { Set<String> s = looks.get(block); return s == null ? 0 : s.size(); }
    public synchronized boolean degraded(long block) { return exposures(block) > MAX_DISTINCT_LOOKS; }
    public synchronized Set<Long> degradedBlocks() {
        Set<Long> out = new HashSet<>();
        for (Map.Entry<Long, Set<String>> e : looks.entrySet()) if (e.getValue().size() > MAX_DISTINCT_LOOKS) out.add(e.getKey());
        return out;
    }
    /** Remaining distinct looks before the block degrades. */
    public synchronized int remaining(long block) { return Math.max(0, MAX_DISTINCT_LOOKS - exposures(block)); }
    public synchronized Map<Long, Set<String>> snapshot() {
        Map<Long, Set<String>> out = new HashMap<>();
        for (Map.Entry<Long, Set<String>> e : looks.entrySet()) out.put(e.getKey(), new LinkedHashSet<>(e.getValue()));
        return out;
    }
    public synchronized void restore(long block, String policyHash) { look(block, policyHash); }
}
