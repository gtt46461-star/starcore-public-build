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

    private final Map<Long, Set<String>> controlLooks = new HashMap<>();
    /** R27.3: a look by a negative control (I00 random baseline, shuffled-label champion). Controls are never promoted, so
     * their looks are recorded but do not spend the block's {@link #MAX_DISTINCT_LOOKS} budget, which belongs to the main
     * line (champion and its challengers). Device 10.29: regenerated controls spent the budget in ~16 generations. */
    public synchronized boolean lookControl(long block, String policyHash) {
        return controlLooks.computeIfAbsent(block, k -> new LinkedHashSet<>()).add(policyHash);
    }
    public synchronized Map<Long, Set<String>> controlSnapshot() {
        Map<Long, Set<String>> out = new HashMap<>();
        for (Map.Entry<Long, Set<String>> e : controlLooks.entrySet()) out.put(e.getKey(), new LinkedHashSet<>(e.getValue()));
        return out;
    }
    public synchronized void restoreControl(long block, String policyHash) { lookControl(block, policyHash); }
    public synchronized int controlExposures(long block) { Set<String> s = controlLooks.get(block); return s == null ? 0 : s.size(); }

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

    // ---------------------------------------------------------------- R30-B03-03 confirm-once gate (M19)
    /** One H_CONFIRM read: (policy_hash, dataset_hash, range, read_at). */
    public static final class ConfirmRead {
        public final String policyHash, datasetHash; public final long block; public final int from, to; public final long readAt; public final boolean control;
        ConfirmRead(String p, String d, long b, int f, int t, long at, boolean control) { policyHash = p; datasetHash = d; block = b; from = f; to = t; readAt = at; this.control = control; }
    }
    public static final String CONFIRM_REUSED = "R30_CONFIRM_REUSED";
    /** Must return only after the unique reservation has committed, before any confirmation samples are read. */
    public interface ConfirmPersistence { void reserve(ConfirmRead read); }
    private final Map<String, ConfirmRead> confirmReads = new HashMap<>();
    private final java.util.List<ConfirmRead> unsavedConfirmReads = new java.util.ArrayList<>();
    private final Set<String> pendingConfirmReads = new HashSet<>(), legacyConfirmReads = new HashSet<>();
    private volatile ConfirmPersistence confirmPersistence;
    private long confirmReuseRejected;
    static String confirmKey(String policy, String dataset, long block) { return policy + "|" + dataset + "|" + block; }
    static String legacyConfirmKey(String policy, long block) { return policy + "|" + block; }
    public void setConfirmPersistence(ConfirmPersistence persistence) { confirmPersistence = persistence; }
    /** The only gate to H_CONFIRM: records the read; a second read of the same candidate on the same dataset hash and
     * block throws R30_CONFIRM_REUSED (and is counted for M19). */
    public ConfirmRead confirm(String policyHash, String datasetHash, long block, int from, int to) {
        return confirm(policyHash, datasetHash, block, from, to, false);
    }
    public ConfirmRead confirm(String policyHash, String datasetHash, long block, int from, int to, boolean control) {
        if (policyHash == null || policyHash.isEmpty() || datasetHash == null || datasetHash.isEmpty() || block < 0 || from < 0 || to <= from)
            throw new IllegalArgumentException("R30_CONFIRM_INVALID_IDENTITY_OR_RANGE");
        String k = confirmKey(policyHash, datasetHash, block);
        synchronized (this) {
            if (confirmReads.containsKey(k) || pendingConfirmReads.contains(k) || legacyConfirmReads.contains(legacyConfirmKey(policyHash, block))) {
                confirmReuseRejected++; throw new IllegalStateException(CONFIRM_REUSED + ":" + policyHash + "@" + block);
            }
            pendingConfirmReads.add(k);
        }
        ConfirmRead r = new ConfirmRead(policyHash, datasetHash, block, from, to, System.currentTimeMillis(), control);
        ConfirmPersistence persistence = confirmPersistence;
        try {
            // Never hold the ledger monitor while waiting for the writer or its pressure guard.
            if (persistence != null) persistence.reserve(r);
        } catch (RuntimeException | Error failure) {
            synchronized (this) { pendingConfirmReads.remove(k); }
            throw failure;
        }
        synchronized (this) {
            pendingConfirmReads.remove(k); confirmReads.put(k, r);
            if (persistence == null) unsavedConfirmReads.add(r);
            if (control) lookControl(block, policyHash); else look(block, policyHash);
        }
        return r;
    }
    public synchronized boolean confirmed(String policyHash, String datasetHash, long block) { return confirmReads.containsKey(confirmKey(policyHash, datasetHash, block)); }
    /** Restores a persisted read (process restart must not reopen H_CONFIRM). */
    public synchronized void restoreConfirm(String policyHash, String datasetHash, long block, int from, int to, long readAt) {
        restoreConfirm(policyHash, datasetHash, block, from, to, readAt, false);
    }
    public synchronized void restoreConfirm(String policyHash, String datasetHash, long block, int from, int to, long readAt, boolean control) {
        confirmReads.put(confirmKey(policyHash, datasetHash, block), new ConfirmRead(policyHash, datasetHash, block, from, to, readAt, control));
        if (control) lookControl(block, policyHash); else look(block, policyHash);
    }
    /** Older exposure rows have no dataset identity; do not reuse the same candidate/block on an unprovable assumption. */
    public synchronized void restoreLegacyExposure(String policyHash, long block) { legacyConfirmReads.add(legacyConfirmKey(policyHash, block)); }
    /** Reads not yet persisted (caller writes them to r30_confirm_read in its own batch). */
    public synchronized java.util.List<ConfirmRead> drainUnsavedConfirmReads() { java.util.List<ConfirmRead> out = new java.util.ArrayList<>(unsavedConfirmReads); unsavedConfirmReads.clear(); return out; }
    public synchronized long confirmReuseRejected() { return confirmReuseRejected; }
    public static final String CONFIRM_DDL = "CREATE TABLE IF NOT EXISTS r30_confirm_read(policy_hash TEXT NOT NULL,dataset_hash TEXT NOT NULL,block_id INTEGER NOT NULL,range_from INTEGER NOT NULL,range_to INTEGER NOT NULL,read_at INTEGER NOT NULL,control INTEGER NOT NULL DEFAULT 0,PRIMARY KEY(policy_hash,dataset_hash,block_id))";
}
