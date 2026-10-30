package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.InputStream;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/** R27-011/012 semantic continuity of the R26 legacy runtime across builds (docs/08). The fingerprint is the
 * trajectory of the legacy kernels' complete encoded state (hash every 64 rows) when a fresh runtime with the same
 * roster is replayed over the 512-period golden set: same numerics ⇒ same trajectory, whatever the source hash.
 * One fingerprint is stored per build ABI; after an upgrade the stored previous-build fingerprint is compared with
 * the current one and the decision (EQUIVALENT / CHANGED) is recorded in r27_abi_continuity for LegacyReplayR26. */
public final class LegacyContinuityR27 {
    private LegacyContinuityR27() {}
    public static final String KEY = "R26_LEGACY_RUNTIME", EQUIVALENT = SemanticAbiR27.EQUIVALENT, CHANGED = SemanticAbiR27.CHANGED;

    public static String decision(SQLiteDatabase db, String oldAbi, String newAbi) {
        db=ResearchRouteR29.reader(db);
        try (Cursor c = db.rawQuery("SELECT result FROM r27_abi_continuity WHERE policy_hash=? AND old_build=? AND new_build=?", new String[] {KEY, oldAbi, newAbi})) {
            return c.moveToFirst() ? c.getString(0) : null;
        } catch (RuntimeException schemaPending) { return null; }
    }
    public static void recordMintedRoster(SQLiteDatabase db, String checkpoint, String roster) {
        db.execSQL("INSERT OR IGNORE INTO r27_legacy_roster_alias VALUES(?,?,?)", new Object[] {checkpoint, roster, System.currentTimeMillis()});
    }
    public static String mintedRoster(DbStore store, String checkpoint, String fallback) {
        try (Cursor c = LegacyResearchScopeR29.observationReader(store).rawQuery("SELECT minted_roster FROM r27_legacy_roster_alias WHERE checkpoint_hash=?", new String[] {checkpoint})) { return c.moveToFirst() ? c.getString(0) : fallback; }
        catch (RuntimeException schemaPending) { return fallback; }
    }

    /** Golden-set trajectory fingerprint of a fresh legacy runtime with this roster under the running code. */
    static String[] fingerprint(List<String> canonical, ReplayDatasetR27 golden) {
        StringBuilder all = new StringBuilder("R27_LEGACY_CODE_FP_V1"); String firstErr = null;
        try {
            LegacyReplayStateR26 state = new LegacyReplayStateR26(canonical, 0);
            long base = 1;
            for (int t = 0; t < golden.size(); t++) {CooperativeContinuationR26.poll();
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
        while (!writes.submit("LEGACY_ABI_CONTINUITY", b, () -> Thread.currentThread().isInterrupted())) {CooperativeContinuationR26.poll();Thread.sleep(50);}
        RuntimeCoordinatorR26.wake();
        return 1;
    }

    // ------------------------------------------------------------------ R29-002 requirement / file-card preservation
    public static final String PRESERVATION_ASSET = "r29/legacy_preservation.json";
    public static final int EXPECTED_REQUIREMENTS = 1832, EXPECTED_FILE_CARDS = 299;
    /** Closure values that would claim completion; none may appear without a regression receipt. */
    static final Set<String> COMPLETION_CLAIMS = new HashSet<>(java.util.Arrays.asList("VERIFIED", "DEVICE_VERIFIED", "FUTURE_QUALIFIED", "DONE", "PASS"));

    /** Verifies the preserved ledger: exact counts, unique ids, a status/owner/evidence path for every requirement,
     * every file card's requirement ids resolvable, and no completion claim without a receipt. Pure (host-testable).
     * {@code receipts} maps requirement id to a regression receipt reference; R29 carries none yet, so every
     * requirement stays NOT_VERIFIED and is counted, never dropped. */
    @SuppressWarnings("unchecked")
    public static Map<String, Object> preservation(String assetJson, Map<String, String> receipts) {
        Map<String, Object> out = new LinkedHashMap<>(); Map<String, Object> root = (Map<String, Object>) JsonR27.parse(assetJson);
        List<Object> reqs = (List<Object>) root.get("requirements"), cards = (List<Object>) root.get("fileCards");
        Set<String> ids = new HashSet<>(); long duplicates = 0, missingFields = 0, unfoundedClaims = 0, receipted = 0;
        Map<String, Long> byStatus = new LinkedHashMap<>(), byClosure = new LinkedHashMap<>();
        for (Object o : reqs) {
            Map<String, Object> r = (Map<String, Object>) o; String id = (String) r.get("id");
            if (id == null || !ids.add(id)) duplicates++;
            String status = (String) r.get("originalStatus"), owner = (String) r.get("owner"), target = (String) r.get("target"), text = (String) r.get("textSha256"), closure = (String) r.get("closure");
            if (empty(status) || empty(owner) || empty(target) || text == null || text.length() != 64 || empty(closure)) missingFields++;
            boolean receipt = receipts != null && id != null && receipts.containsKey(id); if (receipt) receipted++;
            if (closure != null && COMPLETION_CLAIMS.contains(closure) && !receipt) unfoundedClaims++;
            byStatus.merge(String.valueOf(status), 1L, Long::sum); byClosure.merge(String.valueOf(closure), 1L, Long::sum);
        }
        Set<String> cardIds = new HashSet<>(); long cardDuplicates = 0, unresolvedLinks = 0, links = 0;
        for (Object o : cards) {
            Map<String, Object> c = (Map<String, Object>) o; if (!cardIds.add(String.valueOf(c.get("id")))) cardDuplicates++;
            for (Object rid : (List<Object>) c.get("requirements")) { links++; if (!ids.contains(rid)) unresolvedLinks++; }
        }
        boolean complete = reqs.size() == EXPECTED_REQUIREMENTS && cards.size() == EXPECTED_FILE_CARDS && duplicates == 0 && cardDuplicates == 0 && missingFields == 0;
        out.put("schema", root.get("schema")); out.put("requirementsRegistrySha256", root.get("requirementsRegistrySha256")); out.put("cardsRegistrySha256", root.get("cardsRegistrySha256"));
        out.put("requirements", (long) reqs.size()); out.put("expectedRequirements", (long) EXPECTED_REQUIREMENTS); out.put("uniqueRequirementIds", (long) ids.size());
        out.put("fileCards", (long) cards.size()); out.put("expectedFileCards", (long) EXPECTED_FILE_CARDS); out.put("duplicateIds", duplicates + cardDuplicates);
        out.put("missingStatusOwnerOrEvidencePath", missingFields); out.put("cardRequirementLinks", links); out.put("unresolvedCardLinks", unresolvedLinks);
        out.put("completionClaimsWithoutReceipt", unfoundedClaims); out.put("regressionReceipts", receipted); out.put("notYetReverified", (long) reqs.size() - receipted);
        out.put("byOriginalStatus", byStatus); out.put("byClosure", byClosure);
        out.put("ledgerComplete", complete);
        // Recording coverage can be complete; re-verification cannot be inferred from records (docs/00 §2).
        out.put("status", !complete || unfoundedClaims > 0 ? "FAIL" : "RECORDED_NOT_REVERIFIED");
        return out;
    }
    static boolean empty(String s) { return s == null || s.isEmpty(); }

    /** Android: verify the packaged ledger. Errors are reported, never swallowed into a pass. */
    public static Map<String, Object> preservation(android.content.Context context) {
        try (InputStream in = context.getAssets().open(PRESERVATION_ASSET)) {
            java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream(); byte[] chunk = new byte[65536]; int k; while ((k = in.read(chunk)) > 0) buf.write(chunk, 0, k);
            byte[] bytes = buf.toByteArray(); Map<String, Object> m = preservation(new String(bytes, "UTF-8"), new LinkedHashMap<String, String>());
            m.put("assetSha256", StatsR27.sha256(bytes)); return m;
        } catch (Exception e) {
            Map<String, Object> m = new LinkedHashMap<>(); m.put("status", "FAIL"); m.put("reason", "asset " + PRESERVATION_ASSET + " unreadable: " + e.getClass().getSimpleName()); return m;
        }
    }
}
