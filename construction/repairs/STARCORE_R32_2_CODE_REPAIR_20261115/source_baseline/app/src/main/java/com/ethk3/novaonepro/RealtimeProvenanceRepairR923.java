package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.LinkedHashMap;

/**
 * Narrow repair for REAL rows whose provenance flags were lost by the pre-R923 serializer.
 *
 * <p>A row is repaired only when six immutable records from the same canonical transaction agree:
 * selected action, authorization, canonical observation, R922 opportunity, analysis request and
 * first draw receipt.  Ambiguous legacy rows are left untouched.  This does not create a BET.</p>
 */
public final class RealtimeProvenanceRepairR923 {
    public static final String VERSION = "R923_REALTIME_PROVENANCE_REPAIR_V1";

    private RealtimeProvenanceRepairR923() {}

    public static Result repair(final DbStore store) {
        if(store==null)return new Result(false,0,0,0,"STORE_NULL");
        ArrayList<Proof> candidates=load(store.readable());
        int eligible=0,skipped=0;
        for(Proof proof:candidates){if(proof.valid())eligible++;else skipped++;}
        // Eligibility is an audit result, never permission to overwrite an existing settlement.
        return new Result(true,candidates.size(),0,skipped,"CP4_READ_ONLY_PROVENANCE_AUDIT_ELIGIBLE="+eligible);
    }

    private static ArrayList<Proof> load(SQLiteDatabase db) {
        ArrayList<Proof> out = new ArrayList<Proof>();
        Cursor c = null;
        try {
            c = db.rawQuery(
                    "SELECT t._id,t.target_issue,t.position,t.n1,t.n2,t.n3,t.authorization_id,"
                            + "t.contract_valid,t.contract_status,t.lane_candidate,t.raw_p,"
                            + "t.calibrated_p,t.final_p,t.authorization_probability,t.created_at,t.bet_time,"
                            + "s.authorization_hash,s.contract_hash,s.lane,s.decision_action,s.position,s.n1,s.n2,s.n3,"
                            + "a.authorization_hash,a.contract_hash,a.created_at,"
                            + "o.authorization_hash,o.contract_hash,o.lane,o.decision_action,o.position,o.n1,o.n2,o.n3,"
                            + "o.pre_draw_frozen,o.commit_at,o.request_at,o.settled,o.actual_number,o.hit_label,"
                            + "p.authorized_action,p.gate_eligible,p.position,p.n1,p.n2,p.n3,p.created_at,"
                            + "r.status,r.request_at,r.finished_at,d.first_seen_at,"
                            + "CASE o.position WHEN 1 THEN d.n1 WHEN 2 THEN d.n2 WHEN 3 THEN d.n3"
                            + " WHEN 4 THEN d.n4 WHEN 5 THEN d.n5 WHEN 6 THEN d.n6 WHEN 7 THEN d.n7"
                            + " WHEN 8 THEN d.n8 WHEN 9 THEN d.n9 WHEN 10 THEN d.n10 ELSE 0 END,"
                            + "t.hit,t.result,t.k3_mask,t.authorization_valid,t.authorized_mode,t.stat_scope,t.action,"
                            + "COALESCE(t.source_type,''),COALESCE(t.is_realtime_real,0) FROM "
                            + DbHelper.TABLE_TRADES + " t JOIN " + V51Repository.SELECTED
                            + " s ON s.target_issue=t.target_issue JOIN " + V51Repository.AUTH
                            + " a ON a.decision_id=s.decision_id AND a.target_issue=s.target_issue JOIN "
                            + DbHelper.TABLE_R35_SHADOW_OBSERVATION
                            + " o ON o.target_issue=t.target_issue JOIN "
                            + ControlledCandidateOpportunityAuditR922.TABLE
                            + " p ON p.target_issue=t.target_issue JOIN "
                            + DbHelper.TABLE_R36_ANALYSIS_REQUEST
                            + " r ON r.target_issue=t.target_issue JOIN " + DbHelper.TABLE_DRAWS
                            + " d ON d.issue=t.target_issue WHERE t.settled=1 AND t.action='BET'"
                            + " AND t.stat_scope='REAL_TRADE' AND (COALESCE(t.source_type,'')"
                            + " IN ('','UNKNOWN') OR (t.source_type='REALTIME' AND COALESCE(t.is_realtime_real,0)<>1))"
                            + " ORDER BY t.target_issue ASC,t._id ASC",
                    null);
            while (c.moveToNext()) out.add(Proof.from(c));
        } catch (Throwable unavailable) {
            // A schema/query failure is not equivalent to "zero candidates".  Propagate it to
            // the coordinator so the caller publishes WRITE_FAILED instead of a false COMPLETE.
            throw new IllegalStateException("R923_PROVENANCE_SCAN_FAILED", unavailable);
        } finally {
            if (c != null) c.close();
        }
        return out;
    }

    private static void putState(SQLiteDatabase db, String key, String value, long at) {
        ContentValues v = new ContentValues();
        v.put("state_key", key);
        v.put("state_value", value);
        v.put("updated_at", at);
        db.insertWithOnConflict(DbHelper.TABLE_APP_STATE, null, v, SQLiteDatabase.CONFLICT_REPLACE);
    }

    private static boolean k3(int a, int b, int c) {
        return DecisionGateEngine.b12K3(a, b, c);
    }

    private static boolean zero(double value) {
        return Double.isFinite(value) && Math.abs(value) <= 1.0e-12d;
    }

    private static boolean hash(String value) {
        return safe(value).matches("[0-9a-f]{64}");
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class Proof {
        long id, tradeAt, betAt, authAt, commitAt, canonicalRequestAt, opportunityAt,
                requestAt, finishedAt, firstSeenAt;
        String target, authorizationId, contractStatus, tradeLane, selectedAuth, selectedContract,
                selectedLane, selectedAction, authHash, authContract, canonicalAuth,
                canonicalContract, canonicalLane, canonicalAction, opportunityAction, requestStatus,
                tradeResult, authorizedMode, statScope, tradeAction, sourceType;
        int position, n1, n2, n3, selectedPosition, selectedN1, selectedN2, selectedN3,
                canonicalPosition, canonicalN1, canonicalN2, canonicalN3, preDrawFrozen,
                canonicalSettled, canonicalActual, canonicalHit, gateEligible, opportunityPosition,
                opportunityN1, opportunityN2, opportunityN3, drawActual, tradeHit, mask,
                authorizationValid, contractValid, realtimeFlag;
        double rawP, calibratedP, finalP, authorizationP;

        static Proof from(Cursor c) {
            Proof p = new Proof();
            int i = 0;
            p.id=c.getLong(i++);p.target=safe(c.getString(i++));p.position=c.getInt(i++);
            p.n1=c.getInt(i++);p.n2=c.getInt(i++);p.n3=c.getInt(i++);
            p.authorizationId=safe(c.getString(i++));p.contractValid=c.getInt(i++);
            p.contractStatus=safe(c.getString(i++));p.tradeLane=safe(c.getString(i++));
            p.rawP=c.getDouble(i++);p.calibratedP=c.getDouble(i++);p.finalP=c.getDouble(i++);
            p.authorizationP=c.getDouble(i++);p.tradeAt=c.getLong(i++);p.betAt=c.getLong(i++);
            p.selectedAuth=safe(c.getString(i++));p.selectedContract=safe(c.getString(i++));
            p.selectedLane=safe(c.getString(i++));p.selectedAction=safe(c.getString(i++));
            p.selectedPosition=c.getInt(i++);p.selectedN1=c.getInt(i++);p.selectedN2=c.getInt(i++);
            p.selectedN3=c.getInt(i++);p.authHash=safe(c.getString(i++));
            p.authContract=safe(c.getString(i++));p.authAt=c.getLong(i++);
            p.canonicalAuth=safe(c.getString(i++));p.canonicalContract=safe(c.getString(i++));
            p.canonicalLane=safe(c.getString(i++));p.canonicalAction=safe(c.getString(i++));
            p.canonicalPosition=c.getInt(i++);p.canonicalN1=c.getInt(i++);
            p.canonicalN2=c.getInt(i++);p.canonicalN3=c.getInt(i++);
            p.preDrawFrozen=c.getInt(i++);p.commitAt=c.getLong(i++);
            p.canonicalRequestAt=c.getLong(i++);p.canonicalSettled=c.getInt(i++);
            p.canonicalActual=c.getInt(i++);p.canonicalHit=c.getInt(i++);
            p.opportunityAction=safe(c.getString(i++));p.gateEligible=c.getInt(i++);
            p.opportunityPosition=c.getInt(i++);p.opportunityN1=c.getInt(i++);
            p.opportunityN2=c.getInt(i++);p.opportunityN3=c.getInt(i++);
            p.opportunityAt=c.getLong(i++);p.requestStatus=safe(c.getString(i++));
            p.requestAt=c.getLong(i++);p.finishedAt=c.getLong(i++);p.firstSeenAt=c.getLong(i++);
            p.drawActual=c.getInt(i++);p.tradeHit=c.getInt(i++);
            p.tradeResult=safe(c.getString(i++));p.mask=c.getInt(i++);
            p.authorizationValid=c.getInt(i++);p.authorizedMode=safe(c.getString(i++));
            p.statScope=safe(c.getString(i++));p.tradeAction=safe(c.getString(i++));
            p.sourceType=safe(c.getString(i++));p.realtimeFlag=c.getInt(i++);
            return p;
        }

        boolean valid() {
            if (!TargetIssueResolver.isValid(target) || id <= 0L) return false;
            if (!"BET".equals(tradeAction) || !AppConfig.STAT_SCOPE_REAL_TRADE.equals(statScope))
                return false;
            if (!(sourceType.length() == 0 || Models.SOURCE_TYPE_UNKNOWN.equals(sourceType)
                    || Models.SOURCE_TYPE_REALTIME.equals(sourceType))) return false;
            if (Models.SOURCE_TYPE_REALTIME.equals(sourceType) && realtimeFlag == 1) return false;
            if (authorizationValid != 1 || !"BET".equals(authorizedMode)
                    || contractValid != 1
                    || !ControlledRealContractAuditR922.EXACT_CONTRACT_STATUS.equals(contractStatus)
                    || !V51Enums.Lane.CONTROLLED_PROBE.name().equals(tradeLane)) return false;
            if (!(hash(authorizationId) || V51AuthorizationCore.isBetEnvelope(authorizationId))
                    || !authorizationId.equals(selectedAuth)
                    || !authorizationId.equals(authHash) || !authorizationId.equals(canonicalAuth))
                return false;
            if (!hash(selectedContract) || !selectedContract.equals(authContract)
                    || !selectedContract.equals(canonicalContract)) return false;
            if (!V51Enums.Lane.CONTROLLED_PROBE.name().equals(selectedLane)
                    || !selectedLane.equals(canonicalLane) || !"BET".equals(selectedAction)
                    || !selectedAction.equals(canonicalAction) || !"BET".equals(opportunityAction)
                    || gateEligible != 1) return false;
            if (position<1 || position>10 || !k3(n1,n2,n3) || selectedPosition!=position || canonicalPosition!=position
                    || opportunityPosition!=position || selectedN1!=n1 || selectedN2!=n2
                    || selectedN3!=n3 || canonicalN1!=n1 || canonicalN2!=n2 || canonicalN3!=n3
                    || opportunityN1!=n1 || opportunityN2!=n2 || opportunityN3!=n3) return false;
            if (mask != 0 && mask != AppConfig.buildK3Mask(n1,n2,n3)) return false;
            if (!(rawP >= 0.0d && rawP <= 1.0d) || !zero(calibratedP) || !zero(finalP)
                    || !zero(authorizationP)) return false;
            if (preDrawFrozen != 1 || canonicalSettled != 1 || canonicalHit != tradeHit
                    || canonicalActual != drawActual || drawActual < 1 || drawActual > 10) return false;
            int expectedHit = drawActual==n1 || drawActual==n2 || drawActual==n3 ? 1 : 0;
            if (tradeHit != expectedHit) return false;
            String normalized = AppConfig.normalizeResult(tradeResult);
            if (expectedHit==1 && !AppConfig.RESULT_WIN.equals(normalized)) return false;
            if (expectedHit==0 && !AppConfig.RESULT_MISS.equals(normalized)) return false;
            if (!AnalysisRequestStoreR36.CANONICAL_COMMITTED.equals(requestStatus)) return false;
            return requestAt > 0L && canonicalRequestAt == requestAt && commitAt > requestAt
                    && finishedAt >= commitAt && tradeAt == commitAt && betAt == commitAt
                    && authAt == commitAt && opportunityAt > 0L && opportunityAt <= commitAt
                    && firstSeenAt > commitAt && firstSeenAt > finishedAt;
        }
    }

    public static final class Result {
        public final boolean success;
        public final int candidates, repaired, skipped;
        public final String status;
        Result(boolean ok, int candidateCount, int repairedCount, int skippedCount, String state) {
            success=ok;candidates=Math.max(0,candidateCount);repaired=Math.max(0,repairedCount);
            skipped=Math.max(0,skippedCount);status=safe(state);
        }
        public String canonical() {
            return VERSION+"|ok="+(success?1:0)+"|candidates="+candidates
                    +"|repaired="+repaired+"|skipped="+skipped+"|status="+status;
        }
    }
}
