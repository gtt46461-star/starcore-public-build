package com.ethk3.novaonepro;

import java.util.List;

/**
 * R923 real-time adapter for the locked B06/B07 bridge.
 *
 * <p>Numerical support/risk/confidence are produced by the original bridge unchanged.  Expensive
 * database work is moved out of it: plugin health is a one-query thread snapshot and dynamic
 * freeze is a one-query materialized lookup.  The adapter never creates a Decision, Trade or BET.</p>
 */
public final class V51TianshuEvidenceBridgeR923 {
    public static final String VERSION = "R923_V51_B06_B07_HOT_PATH_V1";

    public V51TianshuEvidenceBridge.Result evaluate(
            DbStore store,
            List<Models.Draw> history,
            V51Models.PredictionContext context,
            V51Models.PositionAction action,
            V51Models.StateTrajectorySnapshot state,
            double[] numberP) {
        long begin = System.nanoTime();
        V51TianshuEvidenceBridge.Result out;
        StarcoreEvidenceKernel.HealthReadScope scope =
                StarcoreEvidenceKernel.openHealthReadScopeR923(store);
        try {
            // The original bridge uses DbStore only for diagnostic health/freeze reads.  Passing
            // null leaves all B06/B07 numerical algorithms intact and prevents a full matrix scan.
            out = new V51TianshuEvidenceBridge()
                    .evaluate(null, history, context, action, state, numberP);
        } finally {
            scope.close();
        }
        if (out == null) {
            out = new V51TianshuEvidenceBridge.Result();
            out.hardRisk = 1;
            out.summary = "R923_LOCKED_BRIDGE_NULL";
        }

        long freezeBegin = System.nanoTime();
        Models.ComboCandidate candidate = toCandidate(context, action, state);
        DynamicFreezeMatrixEngine.FreezeDecision freeze =
                DynamicFreezeRealtimeSnapshotR923.check(
                        store, candidate, context == null ? "" : context.targetIssue);
        long freezeUs = Math.max(0L, (System.nanoTime() - freezeBegin) / 1000L);
        if (freeze != null && freeze.frozen == 1) out.hardRisk = 1;

        String freezeAudit = freeze == null
                ? "NULL"
                : (freeze.frozen == 1
                        ? safe(freeze.freezeLevel) + ':' + safe(freeze.freezeKey)
                        : safe(freeze.freezeReason));
        out.fingerprint = V51ChecksumEngine.sha256(
                safe(out.fingerprint) + '|' + VERSION + "|freeze=" + freezeAudit);
        out.summary = safe(out.summary)
                + "|r923HotPath=PLUGIN_HEALTH_1Q+FREEZE_1Q"
                + "|r923Freeze=" + freezeAudit;
        out.timing = append(out.timing,
                "R923_FREEZE_LOOKUP=" + freezeUs + "us,R923_TOTAL="
                        + Math.max(0L, (System.nanoTime() - begin) / 1000L) + "us");
        return out;
    }

    private static Models.ComboCandidate toCandidate(
            V51Models.PredictionContext context,
            V51Models.PositionAction action,
            V51Models.StateTrajectorySnapshot state) {
        Models.ComboCandidate c = new Models.ComboCandidate();
        if (context == null || action == null) return c;
        c.baseIssue = context.baseIssue;
        c.targetIssue = context.targetIssue;
        c.position = action.position;
        c.n1 = action.n1;
        c.n2 = action.n2;
        c.n3 = action.n3;
        c.k3Mask = AppConfig.buildK3Mask(action.n1, action.n2, action.n3);
        c.rank = action.rank;
        c.modelP = action.rawActionP;
        c.rawP = action.rawActionP;
        c.finalP = action.rawActionP;
        c.calibratedP = action.rawActionP;
        c.championGap = action.rankMargin;
        c.comboGap = action.thirdMargin;
        c.stateMode = state == null ? AppConfig.MODE_CHAOS : state.lifecycle.name();
        c.stateKey = state == null ? "" : state.prototypeId;
        c.shapeKey = c.stateKey;
        c.effectiveSample = state == null ? 0 : state.runLength;
        c.driftRisk = state == null ? 1.0d : state.changePointRisk;
        c.trendLife = state == null ? 0.0d : Math.min(100.0d, state.remainingLife);
        c.recoveryScore = state == null ? 0.0d : state.confidence * 100.0d;
        return c;
    }

    private static String append(String first, String second) {
        String a = safe(first), b = safe(second);
        if (a.length() == 0) return b;
        if (b.length() == 0) return a;
        return a + ',' + b;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }
}
