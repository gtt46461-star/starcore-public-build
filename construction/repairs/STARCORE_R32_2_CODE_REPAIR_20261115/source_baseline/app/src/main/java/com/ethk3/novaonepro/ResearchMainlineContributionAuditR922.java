package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/**
 * Per-issue proof of whether an invented asset/algorithm/formula actually changed Production
 * ranking.  A contribution row never grants REAL authority; it only explains the ranked action
 * that subsequently entered the sole DecisionGate.
 */
public final class ResearchMainlineContributionAuditR922 {
    public static final String VERSION = "R922_RESEARCH_MAINLINE_CONTRIBUTION_V1";
    public static final String TABLE = "v57_research_mainline_contribution_r922";

    private ResearchMainlineContributionAuditR922() {}

    public static void ensureSchema(SQLiteDatabase db) {
        if (db == null) throw new IllegalArgumentException("R922_CONTRIBUTION_DB_NULL");
        db.execSQL(
                "CREATE TABLE IF NOT EXISTS " + TABLE + "("
                        + "target_issue TEXT PRIMARY KEY,runtime_version TEXT NOT NULL,lane TEXT NOT NULL,"
                        + "candidate_id TEXT NOT NULL,candidate_name TEXT NOT NULL,status TEXT NOT NULL,"
                        + "applied INTEGER NOT NULL,influence_weight REAL NOT NULL,future_n INTEGER NOT NULL,"
                        + "hit_rate REAL NOT NULL,wilson_lower REAL NOT NULL,brier REAL NOT NULL,ece REAL NOT NULL,"
                        + "coverage REAL NOT NULL,paired_n INTEGER NOT NULL,paired_delta REAL NOT NULL,fdr_q REAL NOT NULL,"
                        + "evidence_hash TEXT NOT NULL,rank1_hash TEXT NOT NULL,"
                        + "controlled_influence_eligible INTEGER NOT NULL,direct_real_authority INTEGER NOT NULL CHECK(direct_real_authority=0),"
                        + "record_hash TEXT NOT NULL UNIQUE,created_at INTEGER NOT NULL)" );
        db.execSQL(
                "CREATE INDEX IF NOT EXISTS idx_r922_contribution_lane ON " + TABLE
                        + "(runtime_version,lane,applied,created_at DESC)");
    }

    static void validateExactWithinTransaction(SQLiteDatabase db,Contribution row,V51CompiledActionContract compiled){
        if(db==null||!db.inTransaction())throw new IllegalStateException("F05_CONTRIBUTION_TX");
        if(compiled==null||compiled.certifiedPrediction==null)return;
        CertifiedPredictionR974.Proof p=compiled.certifiedPrediction;
        if(row==null||!row.applied||!row.controlledInfluenceEligible
                ||!p.target.equals(row.targetIssue)||!p.candidate.equals(row.candidateId)
                ||!p.evidence.equals(row.evidenceHash)||!p.actionHash.equals(row.rankOneHash)
                ||!p.matches(compiled.selected))throw new IllegalStateException("F05_CONTRIBUTION_EXACT_IDENTITY");
    }

    static void insertWithinTransaction(SQLiteDatabase db, Contribution row) {
        if (db == null || !db.inTransaction())
            throw new IllegalStateException("R922_CONTRIBUTION_CANONICAL_TRANSACTION_REQUIRED");
        if (row == null || !row.valid())
            throw new IllegalArgumentException("R922_CONTRIBUTION_INVALID");
        Cursor existing = null;
        try {
            existing = BudgetedQueryR927.query(db,
                    "SELECT record_hash FROM " + TABLE + " WHERE target_issue=? LIMIT 1",
                    new String[] {row.targetIssue});
            if (existing.moveToFirst()) {
                if (!row.recordHash.equals(safe(existing.getString(0))))
                    throw new IllegalStateException("R922_CONTRIBUTION_FIRST_WRITE_CONFLICT");
                return;
            }
        } finally {
            if (existing != null) existing.close();
        }
        ContentValues v = new ContentValues();
        v.put("target_issue", row.targetIssue);
        v.put("runtime_version", row.runtimeVersion);
        v.put("lane", row.lane);
        v.put("candidate_id", row.candidateId);
        v.put("candidate_name", row.candidateName);
        v.put("status", row.status);
        v.put("applied", row.applied ? 1 : 0);
        v.put("influence_weight", row.influenceWeight);
        v.put("future_n", row.futureN);
        v.put("hit_rate", row.hitRate);
        v.put("wilson_lower", row.wilsonLower);
        v.put("brier", row.brier);
        v.put("ece", row.ece);
        v.put("coverage", row.coverage);
        v.put("paired_n", row.pairedN);
        v.put("paired_delta", row.pairedDelta);
        v.put("fdr_q", row.fdrQ);
        v.put("evidence_hash", row.evidenceHash);
        v.put("rank1_hash", row.rankOneHash);
        v.put("controlled_influence_eligible", row.controlledInfluenceEligible ? 1 : 0);
        v.put("direct_real_authority", 0);
        v.put("record_hash", row.recordHash);
        v.put("created_at", row.createdAt);
        db.insertOrThrow(TABLE, null, v);
    }

    public static String recentDetail(DbStore store, int limit) {
        if (store == null) return "READ_ERROR:STORE_NULL";
        Cursor c = null;
        StringBuilder b = new StringBuilder(2048);
        try {
            c = BudgetedQueryR927.query(store.readable(),
                    "SELECT target_issue,lane,candidate_id,candidate_name,status,applied,"
                            + "influence_weight,future_n,hit_rate,wilson_lower,brier,ece,coverage,"
                            + "paired_n,paired_delta,fdr_q,controlled_influence_eligible,evidence_hash "
                            + "FROM " + TABLE + " WHERE runtime_version=?"
                            + " ORDER BY created_at DESC,target_issue DESC LIMIT ?",
                    new String[] {AppConfig.R36_RUNTIME_REPAIR_VERSION,
                            String.valueOf(Math.max(1, Math.min(50, limit)))});
            int n = 0;
            while (c.moveToNext()) {
                n++;
                b.append(n).append("｜期=").append(safe(c.getString(0)))
                        .append("｜lane=").append(safe(c.getString(1)))
                        .append("｜candidate=").append(safe(c.getString(2)))
                        .append('/').append(safe(c.getString(3)))
                        .append("｜status=").append(safe(c.getString(4)))
                        .append("｜applied=").append(c.getInt(5))
                        .append("｜weight=").append(c.getDouble(6))
                        .append("｜N/HR/Wilson=").append(c.getInt(7)).append('/')
                        .append(c.getDouble(8)).append('/').append(c.getDouble(9))
                        .append("｜Brier/ECE/Coverage=").append(c.getDouble(10)).append('/')
                        .append(c.getDouble(11)).append('/').append(c.getDouble(12))
                        .append("｜Paired=").append(c.getInt(13)).append(':').append(c.getDouble(14))
                        .append("｜FDR=").append(c.getDouble(15))
                        .append("｜controlledEligible=").append(c.getInt(16))
                        .append("｜evidence=").append(shortHash(c.getString(17))).append('\n');
            }
            if (n == 0) return "NO_R922_CONTRIBUTION_SAMPLE|fallback=100%";
            return b.toString();
        } catch (Throwable failure) {
            return "READ_ERROR:" + failure.getClass().getSimpleName() + ':' + safe(failure.getMessage());
        } finally {
            if (c != null) c.close();
        }
    }

    private static String shortHash(String value) {
        String v = safe(value);
        return v.length() <= 12 ? v : v.substring(0, 12);
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    public static final class Contribution {
        public final String targetIssue, runtimeVersion, lane, candidateId, candidateName;
        public final String status, evidenceHash, rankOneHash, recordHash;
        public final boolean applied, controlledInfluenceEligible;
        public final double influenceWeight, hitRate, wilsonLower, brier, ece, coverage;
        public final int futureN, pairedN;
        public final double pairedDelta, fdrQ;
        public final long createdAt;

        public Contribution(
                String targetIssue, String lane, String candidateId, String candidateName,
                String status, boolean applied, double influenceWeight, int futureN,
                double hitRate, double wilsonLower, double brier, double ece, double coverage,
                int pairedN, double pairedDelta, double fdrQ, String evidenceHash,
                String rankOneHash, boolean controlledInfluenceEligible, long createdAt) {
            this(targetIssue, AppConfig.R36_RUNTIME_REPAIR_VERSION, lane, candidateId,
                    candidateName, status, applied, influenceWeight, futureN, hitRate,
                    wilsonLower, brier, ece, coverage, pairedN, pairedDelta, fdrQ,
                    evidenceHash, rankOneHash, controlledInfluenceEligible, createdAt, "");
        }

        private Contribution(
                String targetIssue, String runtimeVersion, String lane, String candidateId,
                String candidateName, String status, boolean applied, double influenceWeight,
                int futureN, double hitRate, double wilsonLower, double brier, double ece,
                double coverage, int pairedN, double pairedDelta, double fdrQ,
                String evidenceHash, String rankOneHash, boolean controlledInfluenceEligible,
                long createdAt, String recordHash) {
            this.targetIssue = safe(targetIssue);
            this.runtimeVersion = safe(runtimeVersion);
            this.lane = safe(lane);
            this.candidateId = safe(candidateId);
            this.candidateName = safe(candidateName);
            this.status = safe(status);
            this.applied = applied;
            this.influenceWeight = finite(influenceWeight);
            this.futureN = Math.max(0, futureN);
            this.hitRate = finite(hitRate);
            this.wilsonLower = finite(wilsonLower);
            this.brier = finite(brier);
            this.ece = finite(ece);
            this.coverage = finite(coverage);
            this.pairedN = Math.max(0, pairedN);
            this.pairedDelta = finite(pairedDelta);
            this.fdrQ = finite(fdrQ);
            this.evidenceHash = safe(evidenceHash);
            this.rankOneHash = safe(rankOneHash);
            this.controlledInfluenceEligible = controlledInfluenceEligible;
            this.createdAt = Math.max(1L, createdAt);
            String canonical = canonicalWithoutHash();
            this.recordHash = safe(recordHash).length() == 64
                    ? safe(recordHash) : V51ChecksumEngine.sha256(canonical);
        }

        private String canonicalWithoutHash() {
            return VERSION + '|' + targetIssue + '|' + runtimeVersion + '|' + lane + '|'
                    + candidateId + '|' + candidateName + '|' + status + '|' + (applied ? 1 : 0)
                    + '|' + influenceWeight + '|' + futureN + '|' + hitRate + '|' + wilsonLower
                    + '|' + brier + '|' + ece + '|' + coverage + '|' + pairedN + '|'
                    + pairedDelta + '|' + fdrQ + '|' + evidenceHash + '|' + rankOneHash + '|'
                    + (controlledInfluenceEligible ? 1 : 0) + "|directREAL=0|" + createdAt;
        }

        public boolean valid() {
            return TargetIssueResolver.isValid(targetIssue) && runtimeVersion.length() > 0
                    && lane.length() > 0 && status.length() > 0 && rankOneHash.length() == 64
                    && recordHash.length() == 64 && createdAt > 0L;
        }

        private static double finite(double value) {
            return Double.isNaN(value) || Double.isInfinite(value) ? -1.0d : value;
        }
    }

public static String recentDetailSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, int limit) {
        if (store == null) return "READ_ERROR:STORE_NULL";
        Cursor c = null;
        com.ethk3.novaonepro.ReportTextCP12 b = new com.ethk3.novaonepro.ReportTextCP12(report, 2048);
        try {
            c = BudgetedQueryR927.querySnapshotCP4(report, store.readable(),
                    "SELECT target_issue,lane,candidate_id,candidate_name,status,applied,"
                            + "influence_weight,future_n,hit_rate,wilson_lower,brier,ece,coverage,"
                            + "paired_n,paired_delta,fdr_q,controlled_influence_eligible,evidence_hash "
                            + "FROM " + TABLE + " WHERE runtime_version=?"
                            + " ORDER BY created_at DESC,target_issue DESC LIMIT ?",
                    new String[] {AppConfig.R36_RUNTIME_REPAIR_VERSION,
                            String.valueOf(Math.max(1, Math.min(50, limit)))});
            int n = 0;
            while (c.moveToNext()) {
                n++;
                b.append(n).append("｜期=").append(safeSnapshotCP4(report, c.getString(0)))
                        .append("｜lane=").append(safeSnapshotCP4(report, c.getString(1)))
                        .append("｜candidate=").append(safeSnapshotCP4(report, c.getString(2)))
                        .append('/').append(safeSnapshotCP4(report, c.getString(3)))
                        .append("｜status=").append(safeSnapshotCP4(report, c.getString(4)))
                        .append("｜applied=").append(c.getInt(5))
                        .append("｜weight=").append(c.getDouble(6))
                        .append("｜N/HR/Wilson=").append(c.getInt(7)).append('/')
                        .append(c.getDouble(8)).append('/').append(c.getDouble(9))
                        .append("｜Brier/ECE/Coverage=").append(c.getDouble(10)).append('/')
                        .append(c.getDouble(11)).append('/').append(c.getDouble(12))
                        .append("｜Paired=").append(c.getInt(13)).append(':').append(c.getDouble(14))
                        .append("｜FDR=").append(c.getDouble(15))
                        .append("｜controlledEligible=").append(c.getInt(16))
                        .append("｜evidence=").append(shortHashSnapshotCP4(report, c.getString(17))).append('\n');
            }
            if (n == 0) return "NO_R922_CONTRIBUTION_SAMPLE|fallback=100%";
            return b.toString();
        } catch (Throwable failure) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",failure);} finally {
            if (c != null) c.close();
        }
    }

public static String safeSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String value) {
        return value == null ? "" : value.trim();
    }

public static String shortHashSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String value) {
        String v = safeSnapshotCP4(report, value);
        return v.length() <= 12 ? v : v.substring(0, 12);
    }
}
