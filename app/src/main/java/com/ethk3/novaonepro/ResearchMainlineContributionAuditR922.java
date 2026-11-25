package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

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

    /** Resolve original rank, canonical authorization and real settlement identities. */
    public static List<Map<String,Object>> exactRealAttributionR34(SQLiteDatabase db,int limit){
        List<Map<String,Object>> results=new ArrayList<>();
        String sql="SELECT e.target_issue,e.candidate_id,e.model_hash,e.certificate_hash,e.proof_hash,e.authorization_hash,"
            +"t._id,s.record_hash,t.settled,t.actual_number,t.hit,t.pnl,t.result,s.settlement_hash,q.draw_hash,q.settlement_hash,"
            +"s.commit_at,d.open_time,d.first_seen_at,t.settled_at,t.settle_time"
            +" FROM r974_exact_real_receipt e JOIN "+DbHelper.TABLE_TRADES+" t"
            +" ON t._id=e.trade_id AND t.target_issue=e.target_issue AND t.authorization_id=e.authorization_hash"
            +" JOIN "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" s ON s.target_issue=e.target_issue AND s.authorization_hash=e.authorization_hash"
            +" JOIN "+V51Repository.AUTH+" a ON a.target_issue=e.target_issue AND a.authorization_hash=e.authorization_hash"
            +" AND a.bundle_hash=s.bundle_hash AND a.contract_hash=s.contract_hash"
            +" JOIN "+TABLE+" r ON r.target_issue=e.target_issue AND r.candidate_id=e.candidate_id AND r.rank1_hash=e.action_hash"
            +" LEFT JOIN "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" q ON q.target_issue=e.target_issue AND q.status='COMPLETED'"
            +" LEFT JOIN draws d ON d.issue=e.target_issue AND d.dirty=0"
            +" WHERE s.game_code=? AND s.pre_draw_frozen=1 AND s.decision_action='BET' AND t.action='BET' AND t.stat_scope='REAL_TRADE'"
            +" AND t.position=s.position AND t.n1=s.n1 AND t.n2=s.n2 AND t.n3=s.n3 AND r.applied=1"
            +" ORDER BY e.target_issue DESC LIMIT ?";
        try(Cursor c=BudgetedQueryR927.query(db,sql,new String[]{V51Repository.GAME_CODE,Integer.toString(Math.max(1,Math.min(100,limit)))})){
            while(c.moveToNext()){
                Map<String,Object> row=new LinkedHashMap<>();
                row.put("targetIssue",c.getString(0));row.put("policy",c.getString(1));row.put("modelHash",c.getString(2));
                row.put("certificateHash",c.getString(3));row.put("predictionProof",c.getString(4));
                row.put("authorizationHash",c.getString(5));row.put("tradeId",c.getLong(6));row.put("canonicalHash",c.getString(7));
                long committed=c.getLong(16),drawOpen=c.getLong(17),firstSeen=c.getLong(18);
                long settledAt=Math.max(c.getLong(19),c.getLong(20));
                boolean confirmedClock=exactPredrawClockR342(committed,drawOpen,firstSeen);
                boolean settled=c.getInt(8)==1&&confirmedClock&&settledAt>=firstSeen
                    &&matchesTerminal(c.getLong(6),c.getInt(9),c.getInt(10),
                    c.getString(12),c.getString(13),c.getString(14),c.getString(15));
                row.put("state",settled?"SETTLED_EXACT":c.getInt(8)==1?"SETTLEMENT_IDENTITY_UNKNOWN":"BET_AWAITING_SETTLEMENT");
                row.put("predrawClock",confirmedClock?"VERIFIED":drawOpen<=0?"EXACT_DRAW_CLOCK_UNAVAILABLE":"UNVERIFIED_ORDER");
                row.put("frozenCommitWallMs",committed>0?committed:null);
                row.put("drawFirstSeenWallMs",firstSeen>0?firstSeen:null);
                row.put("tradeSettledWallMs",settledAt>0?settledAt:null);
                if(settled){row.put("hit",c.getInt(10));row.put("pnl",c.getDouble(11));row.put("settlementHash",c.getString(15));}
                results.add(row);
            }
        }
        return results;
    }

    static boolean exactPredrawClockR342(long committed,long open,long firstSeen) {
        return committed>0L&&open>committed&&firstSeen>committed;
    }

    static boolean matchesTerminal(long trade,int actual,int hit,String result,String label,String draw,String terminal){
        if(trade<1||actual<1||actual>10||hit<0||hit>1||!Batch2ContractCoreR926.isSha256(label)
                ||!Batch2ContractCoreR926.isSha256(draw)||!Batch2ContractCoreR926.isSha256(terminal))return false;
        String prefix="R36_SETTLEMENT|"+draw+'|'+label+'|';
        String real=V51ChecksumEngine.sha256("R36_REAL|"+trade+'|'+draw+'|'+actual+'|'+hit);
        if(terminal.equals(V51ChecksumEngine.sha256(prefix+real+"|COMPLETED")))return true;
        String replay=V51ChecksumEngine.sha256("R36_REAL_REPLAY|"+trade+'|'+result+'|'+actual);
        return terminal.equals(V51ChecksumEngine.sha256(prefix+replay+"|COMPLETED"));
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
    
        /** R33.5 exact prepared-payload hydration; invoked only by the closed codec.
         * It restores frozen fields without consulting mutable current model state. */
        Contribution(PreparedCanonicalCodecR335.Decoded decodedR335) {
            this.targetIssue = (String) decodedR335.value(0);
            this.runtimeVersion = (String) decodedR335.value(1);
            this.lane = (String) decodedR335.value(2);
            this.candidateId = (String) decodedR335.value(3);
            this.candidateName = (String) decodedR335.value(4);
            this.status = (String) decodedR335.value(5);
            this.evidenceHash = (String) decodedR335.value(6);
            this.rankOneHash = (String) decodedR335.value(7);
            this.recordHash = (String) decodedR335.value(8);
            this.applied = ((Boolean) decodedR335.value(9)).booleanValue();
            this.controlledInfluenceEligible = ((Boolean) decodedR335.value(10)).booleanValue();
            this.influenceWeight = ((Double) decodedR335.value(11)).doubleValue();
            this.hitRate = ((Double) decodedR335.value(12)).doubleValue();
            this.wilsonLower = ((Double) decodedR335.value(13)).doubleValue();
            this.brier = ((Double) decodedR335.value(14)).doubleValue();
            this.ece = ((Double) decodedR335.value(15)).doubleValue();
            this.coverage = ((Double) decodedR335.value(16)).doubleValue();
            this.futureN = ((Integer) decodedR335.value(17)).intValue();
            this.pairedN = ((Integer) decodedR335.value(18)).intValue();
            this.pairedDelta = ((Double) decodedR335.value(19)).doubleValue();
            this.fdrQ = ((Double) decodedR335.value(20)).doubleValue();
            this.createdAt = ((Long) decodedR335.value(21)).longValue();
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
