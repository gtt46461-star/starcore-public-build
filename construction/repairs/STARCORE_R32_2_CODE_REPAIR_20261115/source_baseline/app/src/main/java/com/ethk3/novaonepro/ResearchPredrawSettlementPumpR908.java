package com.ethk3.novaonepro;

import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;

/**
 * R908 low-priority Research-only label pump.
 *
 * <p>Production settlement owns the exact business closure and never waits for this class.  This
 * pump only finds pre-draw Research rows that were frozen before the target draw, loads the exact
 * persisted draw, and invokes the idempotent Research sidecar settlement.  It owns no BET, no
 * Production settlement row and no model call after draw.</p>
 */
public final class ResearchPredrawSettlementPumpR908 {
    private ResearchPredrawSettlementPumpR908() {}

    public static Result drainBatch(DbStore store, int issueLimit, long budgetMs) {
        if (store == null) return new Result(0, 0, "STORE_NULL");
        if (SettlementExecutionGateR819.isActive()) return new Result(0, 0, "YIELD_PRODUCTION_SETTLEMENT");
        long started = TimeSourceR811.monoNowMs();
        long deadline = started + Math.max(80L, Math.min(1600L, budgetMs));
        int limit = Math.max(1, Math.min(32, issueLimit));
        // R950: settle already-valid frozen rows before legacy proof repair. The pending query is
        // proof-gated, so an unaudited old head cannot contaminate training; it simply waits for
        // the dedicated continuity maintenance lane instead of blocking fresh future labels.
        List<String> issues = pendingIssues(store, limit);
        int visited = 0, rows = 0;
        String lastVisited="";
        try {
        for (int i = 0; i < issues.size() && TimeSourceR811.monoNowMs() < deadline; i++) {
            ResearchWorkBudgetR927.checkpoint();
            if (RealtimePressureGateR811.hasActiveCriticalWork() || SettlementExecutionGateR819.isActive()) break;
            String issue = issues.get(i);
            lastVisited=issue;
            Models.Draw draw = store.getDrawByIssue(issue);
            if (draw == null || !draw.isValid()) continue;
            visited++;
            try { rows += AlgorithmInventionLabR908.settleDraw(store, draw); }
            catch (ResearchWorkBudgetR927.Yield yielded) { throw yielded; }
            catch (Throwable algorithmFailure) {
                recordFailure(store, "algorithm", issue, algorithmFailure);
            }
            try { rows += AssetGenesisEngineR915.settleDraw(store, draw); }
            catch (ResearchWorkBudgetR927.Yield yielded) { throw yielded; }
            catch (Throwable assetFailure) {
                recordFailure(store, "asset", issue, assetFailure);
            }
            rows += ResearchShadowChallengerRuntimeR817.settle(store, draw);
        }
        } finally {
            // Bounded circular scanning: an invalid or partially served old issue
            // cannot monopolize the first page forever. No outcome is acknowledged
            // here; still-pending rows remain in the next scan, including after restart.
            if(!lastVisited.isEmpty()) {
                final String cursor=lastVisited;
                ResearchWorkBudgetR927.finishLeaseReceiptR66(() -> {
                    store.setState("r72_research_settlement_scan_issue",cursor);return Boolean.TRUE;
                });
            }
        }
        int synced = 0;
        if (rows > 0 && !RealtimePressureGateR811.hasActiveCriticalWork()) {
            try { synced = ResearchEvaluationSchedulerR906.syncSettledCoverage(store); }
            catch (Throwable ignore) { synced = -1; }
        }
        String status = "DONE|issues=" + visited + "|rows=" + rows + "|coverage=" + synced
                + "|costMs=" + Math.max(0L, TimeSourceR811.monoNowMs() - started);
        try {
            store.setState("r908_research_settlement_status", status);
            store.setState("r908_research_settlement_at", String.valueOf(System.currentTimeMillis()));
        } catch (Throwable ignore) {}
        return new Result(visited, rows, status);
    }

    private static void recordFailure(DbStore store, String lane, String issue, Throwable failure) {
        try {
            store.setState("r919_research_settlement_" + lane + "_failure",
                    issue + ':' + failure.getClass().getSimpleName() + ':'
                            + safe(failure.getMessage()));
        } catch (Throwable ignored) {}
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace('|', '_').trim();
    }

    private static List<String> pendingIssues(DbStore store, int limit) {
        ArrayList<String> out = new ArrayList<String>();
        String after=store.getState("r72_research_settlement_scan_issue","");
        if(!TargetIssueResolver.isValid(after))after="";
        Cursor c = null;
        try {
            // R912: an invented-algorithm row is independently valid Research evidence.  R908
            // scanned only the shared challenger table; if that companion row was absent/already
            // settled, the algorithm's own state/update program never received its outcome.
            c = store.readable().rawQuery(
                    "SELECT target_issue FROM ("
                            + "SELECT r.target_issue FROM " + DbHelper.TABLE_R817_RESEARCH_PREDRAW + " r"
                            + " WHERE r.hit_label=-1 AND (r.candidate_id NOT LIKE 'R908-ALG-%' OR ("
                            + AlgorithmProofRecoveryR948.settlementEligibleR79("r.candidate_id","r.target_issue","r.record_hash") + ")) UNION "
                            + "SELECT a.target_issue FROM " + DbHelper.TABLE_R908_ALGORITHM_PREDRAW + " a"
                            + " WHERE a.hit_label=-1 AND (" + AlgorithmProofRecoveryR948.settlementEligibleR79("a.genome_id","a.target_issue","a.record_hash") + ") UNION "
                            + "SELECT target_issue FROM " + AssetGenesisEngineR915.TABLE_ASSET_FREEZE
                            + " WHERE hit_label=-1) pending "
                            + "WHERE EXISTS(SELECT 1 FROM " + DrawDependencyInputCP12.SOURCE
                            + " d WHERE d.issue=pending.target_issue AND d.dirty=0) "
                            + "GROUP BY target_issue ORDER BY CASE WHEN target_issue>? THEN 0 ELSE 1 END,target_issue ASC LIMIT ?",
                    new String[] { after,String.valueOf(limit) });
            while (c.moveToNext()) {
            ResearchWorkBudgetR927.checkpoint();
                String issue = c.getString(0);
                if (TargetIssueResolver.isValid(issue)) out.add(issue);
            }
        } finally { if (c != null) c.close(); }
        return out;
    }

    public static final class Result {
        public final int issues, rows;
        public final String status;
        Result(int issues, int rows, String status) {
            this.issues = issues;
            this.rows = rows;
            this.status = status == null ? "" : status;
        }
    }
}
