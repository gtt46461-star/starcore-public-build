package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.util.List;

/** R27-011/012 semantic continuity of the R26 legacy runtime across builds (docs/08). The fingerprint is the
 * trajectory of the legacy kernels' complete encoded state (hash every 64 rows) when a fresh runtime with the same
 * roster is replayed over the 512-period golden set: same numerics ⇒ same trajectory, whatever the source hash.
 * One fingerprint is stored per build ABI; after an upgrade the stored previous-build fingerprint is compared with
 * the current one and the decision (EQUIVALENT / CHANGED) is recorded in r27_abi_continuity for LegacyReplayR26. */
public final class LegacyContinuityR27 {
    private LegacyContinuityR27() {}
    public static final String KEY = "R26_LEGACY_RUNTIME", EQUIVALENT = SemanticAbiR27.EQUIVALENT, CHANGED = SemanticAbiR27.CHANGED;

    public static String decision(SQLiteDatabase db, String oldAbi, String newAbi) {
        try (Cursor c = db.rawQuery("SELECT result FROM r27_abi_continuity WHERE policy_hash=? AND old_build=? AND new_build=?", new String[] {KEY, oldAbi, newAbi})) {
            return c.moveToFirst() ? c.getString(0) : null;
        } catch (RuntimeException schemaPending) { return null; }
    }
    public static void recordMintedRoster(SQLiteDatabase db, String checkpoint, String roster) {
        db.execSQL("INSERT OR IGNORE INTO r27_legacy_roster_alias VALUES(?,?,?)", new Object[] {checkpoint, roster, System.currentTimeMillis()});
    }
    public static String mintedRoster(DbStore store, String checkpoint, String fallback) {
        try (Cursor c = store.readable().rawQuery("SELECT minted_roster FROM r27_legacy_roster_alias WHERE checkpoint_hash=?", new String[] {checkpoint})) { return c.moveToFirst() ? c.getString(0) : fallback; }
        catch (RuntimeException schemaPending) { return fallback; }
    }

    /** Golden-set trajectory fingerprint of a fresh legacy runtime with this roster under the running code. */
    static String[] fingerprint(List<String> canonical, ReplayDatasetR27 golden) {
        StringBuilder all = new StringBuilder("R27_LEGACY_CODE_FP_V1"); String firstErr = null;
        try {
            LegacyReplayStateR26 state = new LegacyReplayStateR26(canonical, 0);
            long base = 1;
            for (int t = 0; t < golden.size(); t++) {
                long ord = base + (golden.ordinals[t] - golden.ordinals[0]);
                state.advance(golden.draws[t], ord);
                if ((t + 1) % 64 == 0) all.append('|').append(t).append(':').append(StatsR27.sha256(state.encode()));
            }
        } catch (Exception e) { firstErr = e.getClass().getSimpleName() + ":" + e.getMessage(); all.append("|ERROR:").append(firstErr); }
        return new String[] {StatsR27.sha256(all.toString()), firstErr};
    }

    /** One r27-abi slice for the legacy runtime. Returns progress units. */
    public static long step(DbStore store, SQLiteDatabase ro, ReplayDatasetR27 golden, String operatorAbi, ResearchWriteQueueR27 writes) throws Exception {
        String current = ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256, stored = null, rosterAbi = null; byte[] stateBlob = null; String stateHash = null;
        try (Cursor c = ro.rawQuery("SELECT numerical_abi,state,state_hash FROM r26_legacy_runtime WHERE singleton=1", null)) { if (c.moveToFirst()) { stored = c.getString(0); stateBlob = c.getBlob(1); stateHash = c.getString(2); } }
        if (stored == null) return 0;
        List<String> canonical = CheckpointArtifactsR26.forStore(store).restoreLegacy(stateBlob, stateHash).canonical;
        String rosterKey = KEY + ":" + StatsR27.sha256(String.join("\n", canonical));
        boolean haveCurrent; try (Cursor c = ro.rawQuery("SELECT 1 FROM r27_semantic_fingerprint WHERE policy_hash=? AND build=?", new String[] {rosterKey, current})) { haveCurrent = c.moveToFirst(); }
        boolean needDecision = !stored.equals(current) && decision(ro, stored, current) == null;
        if (haveCurrent && !needDecision) return 0;
        String[] fp = fingerprint(canonical, golden);
        String prior = null; try (Cursor c = ro.rawQuery("SELECT fingerprint FROM r27_semantic_fingerprint WHERE policy_hash=? AND build=?", new String[] {rosterKey, stored})) { if (c.moveToFirst()) prior = c.getString(0); }
        String result = prior != null && prior.equals(fp[0]) && fp[1] == null ? EQUIVALENT : CHANGED;
        String field = prior == null ? "NO_PRIOR_FINGERPRINT(previous build had no R27 semantic record)" : fp[1] != null ? "ERROR:" + fp[1] : result.equals(CHANGED) ? "state_trajectory" : null;
        ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch(); long now = System.currentTimeMillis();
        b.add("INSERT OR REPLACE INTO r27_semantic_fingerprint(policy_hash,build,fingerprint,operator_abi,rows_blob,computed_at) VALUES(?,?,?,?,NULL,?)", rosterKey, current, fp[0], operatorAbi, now);
        if (needDecision) b.add("INSERT OR IGNORE INTO r27_abi_continuity(policy_hash,old_build,new_build,result,first_diff_issue,first_diff_field,checked_at) VALUES(?,?,?,?,?,?,?)", KEY, stored, current, result, null, field, now);
        while (!writes.submit("LEGACY_ABI_CONTINUITY", b, () -> false)) Thread.sleep(50);
        RuntimeCoordinatorR26.wake();
        return 1;
    }
}
