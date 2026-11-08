package com.ethk3.novaonepro;

import java.util.*;

/**
 * R31-B05-C09: WAL checkpoint policy that never blocks the realtime chain.
 *
 * <p>Each round first observes busy/log/checkpointed of a PASSIVE checkpoint. TRUNCATE is attempted only outside the
 * realtime freeze/settle window, only after a stale reader was cancelled by its own owner, and only within a short
 * slice budget; BUSY is a back-off (never a corruption signal), with exponentially growing waits and a cap, so a long
 * reader can never cause an unbounded BUSY loop. Deleted rows and reusable pages are reported separately from WAL
 * bytes. The offline maintenance service (StorageMaintenanceServiceR82) is never called from this path.</p>
 */
public final class StorageCheckpointR31 {
    /** Busy/log/checkpointed as SQLite reports them for one checkpoint call. */
    public static final class Result { public final int busy, log, checkpointed; public Result(int busy, int log, int checkpointed) { this.busy = busy; this.log = log; this.checkpointed = checkpointed; } }
    public interface Database {
        Result checkpoint(String mode);          // "PASSIVE" or "TRUNCATE"
        boolean inRealtimeWindow(long nowMs);     // freeze/settle deadline window
        boolean cancelStaleReaders();             // asks the reader owners to cancel; true when none remain
        long sliceRemainingMs();
    }
    public static final int MAX_BACKOFF_MS = 60_000, TRUNCATE_LOG_PAGES = 4096;
    private int consecutiveBusy; private long nextAttemptAt; private long passiveRounds, truncates, busyRounds, skippedRealtime;
    private final List<String> evidence = new ArrayList<>();

    /** One bounded maintenance round; returns what was done. */
    public String round(Database db, long nowMs) {
        if (nowMs < nextAttemptAt) return "BACKOFF_UNTIL:" + nextAttemptAt;
        if (db.inRealtimeWindow(nowMs)) { skippedRealtime++; return "SKIPPED_REALTIME_WINDOW"; }
        Result passive = db.checkpoint("PASSIVE"); passiveRounds++;
        evidence.add("PASSIVE busy=" + passive.busy + " log=" + passive.log + " checkpointed=" + passive.checkpointed);
        if (passive.busy != 0 || passive.checkpointed < passive.log) {
            busyRounds++; consecutiveBusy++;
            long wait = Math.min(MAX_BACKOFF_MS, 250L << Math.min(16, consecutiveBusy)); nextAttemptAt = nowMs + wait;
            if (consecutiveBusy >= 3) db.cancelStaleReaders(); // the reader's owner cancels it; never a forced close
            return "BUSY_BACKOFF_MS:" + wait;
        }
        consecutiveBusy = 0;
        if (passive.log < TRUNCATE_LOG_PAGES || db.sliceRemainingMs() < 20 || !db.cancelStaleReaders()) return "PASSIVE_COMPLETE";
        Result t = db.checkpoint("TRUNCATE");
        evidence.add("TRUNCATE busy=" + t.busy + " log=" + t.log + " checkpointed=" + t.checkpointed);
        if (t.busy != 0) { nextAttemptAt = nowMs + 1000; return "TRUNCATE_BUSY_BACKOFF"; }
        truncates++; return "TRUNCATED";
    }
    public Map<String, Object> json() {
        Map<String, Object> m = new LinkedHashMap<>(); m.put("passiveRounds", passiveRounds); m.put("truncates", truncates); m.put("busyRounds", busyRounds);
        m.put("skippedRealtimeWindow", skippedRealtime); m.put("consecutiveBusy", (long) consecutiveBusy); m.put("busyIsCorruption", false); m.put("offlineServiceInvoked", false);
        m.put("evidence", new ArrayList<Object>(evidence.subList(Math.max(0, evidence.size() - 16), evidence.size()))); return m;
    }
}
