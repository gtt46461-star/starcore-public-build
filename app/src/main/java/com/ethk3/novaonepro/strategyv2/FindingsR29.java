package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** R29-040: diagnostic findings from measured state. Every finding names the evidence, its strength, what would
 * refute it, the code location that owns the behaviour, the construction card and the verification that confirms a
 * fix. A value that was not measured yields an UNKNOWN finding with the collection step, never a silent PASS. Pure. */
public final class FindingsR29 {
    /** Same value as QualificationInboxR29.BLOCK_ACT_PENDING_SETTLE; copied because this file is compiled as pure Java
     *  (verify_strategy_su1 deployed_java_compile) and QualificationInboxR29 depends on android.database. */
    static final String BLOCK_ACT_PENDING_SETTLE = "ACT_PENDING_SETTLE";

    private FindingsR29() {}

    static Map<String, Object> finding(String id, String severity, String evidence, String strength, String refutedBy, String code, String card, String verify) {
        Map<String, Object> f = new LinkedHashMap<>();
        String level = strength.startsWith("DIRECT_") ? "OBSERVED" : "NONE".equals(strength) ? "UNKNOWN" : strength;
        f.put("id", id); f.put("severity", severity); f.put("evidence", evidence); f.put("strength", level); f.put("refutedBy", refutedBy);
        f.put("code", code); f.put("card", card); f.put("verification", verify); return f;
    }
    static Long n(Object o) { return o instanceof Number ? ((Number) o).longValue() : null; }

    /** @param q QualificationInboxR29.diagnostics(db) */
    public static List<Map<String, Object>> fromQualification(Map<String, Object> q) {
        List<Map<String, Object>> out = new ArrayList<>();
        Long receipts = q == null ? null : n(q.get("receipts"));
        if (receipts == null) {
            out.add(finding("R29F-QUAL-UNKNOWN", "UNKNOWN", "r29_qual_* tables not readable in this snapshot", "NONE", "a snapshot taken after SchemaR27.ensure",
                    "SchemaR27.ensure / QualificationInboxR29.DDL", "R29-035", "start the app once on this build, then export the report again"));
            return out;
        }
        Long mismatch = n(q.get("receiptStateMismatch")), conflicts = n(q.get("conflictsOpen")), minus2 = n(q.get("legacyHitMinus2Remaining")),
                pending = n(q.get("pendingDrawOpen")), pendingAct = n(q.get("pendingDrawOpenActRows")), oldest = n(q.get("pendingDrawOldestOrdinal"));
        if (mismatch != null && mismatch > 0)
            out.add(finding("R29F-QUAL-RECEIPT-MISMATCH", "BLOCKER", mismatch + " experiments whose e-process n differs from their receipt count", "DIRECT_COUNT",
                    "receipt count equals n for every experiment", "QualificationInboxR29.step / needsRebuild", "R29-035", "tools/r29/run_qual_host.sh Q2/Q6; device kill-during-settle receipt"));
        if (conflicts != null && conflicts > 0)
            out.add(finding("R29F-QUAL-CONFLICT", "HIGH", conflicts + " isolated result conflicts (stored hit disagrees with the draw); certificates blocked", "DIRECT_COUNT",
                    "draw source shows the stored value; conflict rows resolved with audit", "QualificationInboxR29.plan / DrawCanonicalizerR811", "R29-012", "inspect r29_qual_conflict.detail; host Q4"));
        if (minus2 != null && minus2 > 0)
            out.add(finding("R29F-LEGACY-MINUS2", "MEDIUM", minus2 + " legacy hit=-2 rows still unsettled (draw not yet backfilled)", "DIRECT_COUNT",
                    "the draws exist and the next settle pass settles them", "LiveShadowsR27.settleBatch SETTLEABLE / ApiClient backfill", "R29-010", "host Q3; count reaches 0 after backfill"));
        if (pending != null && pending > 0)
            out.add(finding("R29F-PENDING-DRAW", pendingAct != null && pendingAct > 0 ? "HIGH" : "MEDIUM",
                    pending + " frozen periods wait for their draw (" + pendingAct + " ACT rows block contiguous qualification), oldest ordinal " + oldest, "DIRECT_COUNT",
                    "draw backfill resolves them (r29_pending_draw.resolved_at)", "ApiClient.fetchTodayBackfill/fetchYesterdayBackfill → LiveShadowsR27.settle", "R29-009",
                    "network restore + backfill; pendingDrawOpen returns to 0 without new hit=-2"));
        Object blocked = q.get("cursorByBlockReason");
        Long gaps = n(q.get("missingOpportunityExperiments"));
        if (gaps != null && gaps > 0)
            out.add(finding("R29F-MISSING-OPPORTUNITY", "BLOCKER", gaps + " experiments stopped at a calendar period with no original frozen record", "OBSERVED",
                    "an original immutable predraw record is recovered; never a postdraw replacement", "QualificationInboxR29.plan / RuntimeR27.LiveTask", "R29-035",
                    "host Q9; inspect blocked_ordinal, restore original record or close the evidence segment with audit"));
        if (blocked instanceof Map && ((Map<?, ?>) blocked).containsKey(BLOCK_ACT_PENDING_SETTLE))
            out.add(finding("R29F-SETTLE-LAG", "MEDIUM", "experiments blocked on an ACT period whose draw exists but is not settled yet", "DIRECT_STATE",
                    "next LIVE_SETTLE pass clears it", "RuntimeR27.LiveTask.step ordering settle → qualify", "R29-012", "M04 settlement latency probe"));
        Long v2 = n(q.get("gateV2Evaluated")), v2ok = n(q.get("gateV2WouldIssue"));
        if (v2 == null || v2 == 0)
            out.add(finding("R29F-GATEV2-NOT-RUN", "UNKNOWN", "no R29_GATE_V2 evaluation recorded (no ACTIVE live shadow or certify not run)", "NONE",
                    "r29_gate_v2 rows after the next certify pass", "LiveShadowsR27.certify → GateV2R29.record", "R29-036", "export after one live period with an ACTIVE shadow"));
        else if (v2ok != null && v2ok == 0)
            out.add(finding("R29F-NO-QUALIFIED", "INFO", v2 + " live experiments evaluated by R29_GATE_V2, none meets every gate; see gateV2Nearest for the specific gaps", "DIRECT_STATE",
                    "an experiment with would_issue=1", "GateV2R29.evaluate", "R29-049", "natural FUTURE samples; never by quota or window selection"));
        return out;
    }
}
