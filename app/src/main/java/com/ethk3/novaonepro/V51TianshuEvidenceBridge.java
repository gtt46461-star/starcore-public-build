package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.List;

/**
 * B01-B07 repair bridge: connects mature B06/B07 evidence into V51 without creating a second owner.
 * Evidence-only: never writes finalP, lane, action, decision or BET. DecisionGate remains the only
 * BET owner.
 */
public final class V51TianshuEvidenceBridge {
    public static final String VERSION = "V57.7.3-R31-B06-B07-V51-EVIDENCE-BRIDGE";

    public static final class Result {
        public double support, risk, confidence;
        public int hardRisk, readyModules;
        public long costMs;
        /** Diagnostic-only stage timing; excluded from strategy fingerprint and aggregation. */
        public String fingerprint = "", summary = "", timing = "";
    }

    public Result evaluate(
            DbStore store,
            List<Models.Draw> history,
            V51Models.PredictionContext context,
            V51Models.PositionAction action,
            V51Models.StateTrajectorySnapshot state,
            double[] numberP) {
        long begin = System.currentTimeMillis();
        Result out = new Result();
        if (action == null || context == null) {
            out.hardRisk = 1;
            out.summary = "BRIDGE_INVALID_INPUT";
            return out;
        }
        final List<Models.Draw> bounded240 = boundHistory(history, 240);
        final List<PatternStateEngine.DrawRecord> pattern240 = patternRows(bounded240);
        final List<NumberBehaviorEngine.DrawRecord> number240 = numberRows(bounded240);
        final StringBuilder timing = new StringBuilder(384);
        long stageBegin = System.nanoTime();
        Models.ComboCandidate c = toCandidate(context, action, state, numberP);
        double sw = 0.0d, rw = 0.0d, cw = 0.0d, w = 0.0d;
        int ready = 0;
        StringBuilder fp = new StringBuilder(2048);
        try {
            PatternStateEngine.PatternStateResult pr =
                    new PatternStateEngine().analyze(pattern240, context.baseIssue);
            PatternStateEngine.B06PatternEvidenceBundle e =
                    new PatternStateEngine().buildB06Evidence(pr);
            double q = e.confidence;
            sw += e.support * q;
            rw += e.risk * q;
            cw += e.confidence * q;
            w += q;
            if (e.evidenceReady == 1) ready++;
            fp.append(e.fingerprint);
        } catch (Throwable x) {
            fp.append("PERR");
        }
        markTiming(timing, "B06_PATTERN", stageBegin);
        if (over(begin)) {
            out.timing = timing.toString();
            return finish(
                    store, out, sw, rw, cw, w, ready, fp, c, null, "B06_PATTERN_BUDGET", begin);
        }
        stageBegin = System.nanoTime();
        try {
            ShapeStateEngine.B06ShapeStateEvidence e =
                    new ShapeStateEngine()
                            .buildB06Evidence(toShape(context, action, state, numberP));
            double q = e.confidence;
            sw += e.support * q;
            rw += e.risk * q;
            cw += e.confidence * q;
            w += q;
            if (e.evidenceReady == 1) ready++;
            fp.append(e.fingerprint);
        } catch (Throwable x) {
            fp.append("SERR");
        }
        markTiming(timing, "B06_SHAPE", stageBegin);
        stageBegin = System.nanoTime();
        try {
            TransitionRelationEngine.TransitionRelationResult e =
                    new TransitionRelationEngine().analyzeCandidate(bounded240, c);
            if (e != null) {
                double q = e.b06Confidence;
                sw += e.b06Support * q;
                rw += e.b06Risk * q;
                cw += q * q;
                w += q;
                if (e.b06EvidenceReady == 1) ready++;
                fp.append(e.b06Fingerprint);
            }
        } catch (Throwable x) {
            fp.append("TERR");
        }
        markTiming(timing, "B06_TRANSITION", stageBegin);
        if (over(begin)) {
            out.timing = timing.toString();
            return finish(
                    store, out, sw, rw, cw, w, ready, fp, c, null, "B06_RELATION_BUDGET", begin);
        }
        stageBegin = System.nanoTime();
        try {
            SimilarShapeEngine.B06SimilarEvidence e =
                    new SimilarShapeEngine().evaluateCandidateB06(bounded240, c);
            double q = e.confidence;
            sw += e.support * q;
            rw += e.risk * q;
            cw += q * q;
            w += q;
            if (e.evidenceReady == 1) ready++;
            fp.append(e.fingerprint);
        } catch (Throwable x) {
            fp.append("SIERR");
        }
        markTiming(timing, "B06_SIMILAR", stageBegin);
        stageBegin = System.nanoTime();
        try {
            ShapeMemoryEngine.MemorySignal e =
                    new ShapeMemoryEngine().evaluateCandidate(c, bounded240);
            double q = e.b06Confidence;
            sw += e.b06Support * q;
            rw += e.b06Risk * q;
            cw += q * q;
            w += q;
            if (e.b06EvidenceReady == 1) ready++;
            fp.append(e.b06Fingerprint);
        } catch (Throwable x) {
            fp.append("MERR");
        }
        markTiming(timing, "B06_MEMORY", stageBegin);
        if (over(begin)) {
            out.timing = timing.toString();
            return finish(
                    store, out, sw, rw, cw, w, ready, fp, c, null, "B06_MEMORY_BUDGET", begin);
        }
        stageBegin = System.nanoTime();
        try {
            NumberBehaviorEngine.NumberBehaviorResult nr =
                    new NumberBehaviorEngine().analyze(number240, context.baseIssue);
            NumberBehaviorEngine.B06NumberEvidenceBundle e =
                    new NumberBehaviorEngine().buildB06Evidence(nr);
            double q = e.confidence;
            sw += e.support * q;
            rw += e.risk * q;
            cw += q * q;
            w += q;
            if (e.evidenceReady == 1) ready++;
            fp.append(e.fingerprint);
        } catch (Throwable x) {
            fp.append("NERR");
        }
        markTiming(timing, "B06_NUMBER", stageBegin);
        StarcoreEvidenceKernel.EvidenceBundle kb = null;
        stageBegin = System.nanoTime();
        try {
            StrategyEvidenceContract.Snapshot contract = StrategyEvidenceContract.capture(c);
            markTiming(timing, "B07_CONTRACT", stageBegin);
            stageBegin = System.nanoTime();
            StrategyEvidenceContract.ContractAudit audit =
                    StrategyEvidenceContract.auditCandidateR3(c);
            EvidenceGroupAggregatorEngine.Snapshot groups =
                    EvidenceGroupAggregatorEngine.evaluate(c, null, null, null, null);
            markTiming(timing, "B07_GROUPS", stageBegin);
            stageBegin = System.nanoTime();
            EvidenceHealthSnapshotEngine.Snapshot health =
                    new EvidenceHealthSnapshotEngine().loadReadOnlyR36(store);
            markTiming(timing, "B07_HEALTH", stageBegin);
            stageBegin = System.nanoTime();
            DynamicFreezeMatrixEngine.FreezeDecision freeze =
                    new DynamicFreezeMatrixEngine().checkCandidate(store, c);
            markTiming(timing, "B07_FREEZE", stageBegin);
            stageBegin = System.nanoTime();
            kb = new StarcoreEvidenceKernel().evaluateReadOnlyR36(store, c, "V51_PRE_AUTH");
            new StarcoreEvidenceKernel().applyEvidenceOnly(c, kb);
            markTiming(timing, "B07_KERNEL", stageBegin);
            double q = Math.max(0.05d, kb.confidence);
            sw += kb.support * q;
            rw += kb.risk * q;
            cw += kb.confidence * q;
            w += q;
            if ("READY".equals(kb.healthState) || "STRONG".equals(kb.healthState)) ready++;
            out.hardRisk =
                    (kb.hardRisk == 1 || (freeze != null && freeze.frozen == 1) || audit == null
                            ? 1
                            : 0);
            fp.append(contract == null ? "" : contract.fingerprint)
                    .append('|')
                    .append(audit == null ? "" : audit.fingerprint)
                    .append('|')
                    .append(groups == null ? "" : groups.summary())
                    .append('|')
                    .append(health == null ? "" : health.summary)
                    .append('|')
                    .append(kb.fingerprint);
        } catch (Throwable x) {
            out.hardRisk = 1;
            fp.append("B07ERR");
            markTiming(timing, "B07_ERROR", stageBegin);
        }
        out.timing = timing.toString();
        return finish(store, out, sw, rw, cw, w, ready, fp, c, kb, "COMPLETE", begin);
    }

    public static Result skippedForBudget(
            DbStore store, String reason, long elapsedBeforeOptionalMs) {
        Result o = new Result();
        o.support = 0.50d;
        o.risk = 0.0d;
        o.confidence = 1.0d;
        o.hardRisk = 0;
        o.readyModules = 0;
        o.costMs = 0L;
        o.fingerprint =
                V51ChecksumEngine.sha256(
                        VERSION
                                + "|SKIPPED_FOR_BUDGET|"
                                + (reason == null ? "" : reason)
                                + "|"
                                + elapsedBeforeOptionalMs);
        o.summary =
                "stage=SKIPPED_FOR_BUDGET|reason="
                        + (reason == null ? "" : reason)
                        + "|elapsedBeforeOptionalMs="
                        + elapsedBeforeOptionalMs
                        + "|support=0.5|risk=0|confidence=1|ready=0|hard=0|hash="
                        + o.fingerprint;
        try {
            if (store != null) {
                store.setState("v51_b06_b07_bridge", o.summary);
                store.setState("v51_optional_evidence_status", "SKIPPED_SOFT_BUDGET");
                store.setState(
                        "v51_optional_evidence_skip_elapsed_ms",
                        String.valueOf(elapsedBeforeOptionalMs));
            }
        } catch (Throwable ignore) {
        }
        return o;
    }

    public static void observeSettlement(
            DbStore store, Models.ComboCandidate c, Models.Draw actual) {
        if (store == null || c == null || actual == null || !actual.isValid()) return;
        try {
            Models.AttributionResult a = new AttributionEngine().analyzeCandidate(c, actual);
            if (a != null) {
                store.setState(
                        "v51_b06_attribution_last",
                        a.mainReasonCode
                                + "|"
                                + a.affectedModule
                                + "|"
                                + a.confidence
                                + "|hit="
                                + a.hit);
            }
        } catch (Throwable ignore) {
        }
        try {
            StarcoreEvidenceKernel.observeReplay(
                    store, c, actual.getNumberAtPosition(c.position), "V51_SETTLEMENT");
        } catch (Throwable ignore) {
        }
    }

    private Result finish(
            DbStore store,
            Result o,
            double sw,
            double rw,
            double cw,
            double w,
            int ready,
            StringBuilder fp,
            Models.ComboCandidate c,
            StarcoreEvidenceKernel.EvidenceBundle kb,
            String stage,
            long begin) {
        o.support = w <= 0 ? 0 : clamp(sw / w);
        o.risk = w <= 0 ? 1 : clamp(rw / w);
        o.confidence = w <= 0 ? 0 : clamp(cw / w);
        o.readyModules = ready;
        o.costMs = Math.max(0L, System.currentTimeMillis() - begin);
        o.fingerprint = V51ChecksumEngine.sha256(VERSION + '|' + fp.toString() + '|' + stage);
        o.summary =
                "stage="
                        + stage
                        + "|support="
                        + o.support
                        + "|risk="
                        + o.risk
                        + "|confidence="
                        + o.confidence
                        + "|ready="
                        + ready
                        + "|hard="
                        + o.hardRisk
                        + "|hash="
                        + o.fingerprint;
        return o;
    }

    /** Active R3.6 evidence is never selected or truncated by wall-clock time. */
    private boolean over(long begin) {
        return false;
    }

    private static Models.ComboCandidate toCandidate(
            V51Models.PredictionContext ctx,
            V51Models.PositionAction a,
            V51Models.StateTrajectorySnapshot st,
            double[] p) {
        Models.ComboCandidate c = new Models.ComboCandidate();
        c.baseIssue = ctx.baseIssue;
        c.targetIssue = ctx.targetIssue;
        c.position = a.position;
        c.n1 = a.n1;
        c.n2 = a.n2;
        c.n3 = a.n3;
        c.k3Mask = AppConfig.buildK3Mask(a.n1, a.n2, a.n3);
        c.rank = a.rank;
        c.modelP = a.rawActionP;
        c.rawP = a.rawActionP;
        c.finalP = a.rawActionP;
        c.calibratedP = a.rawActionP;
        c.championGap = a.rankMargin;
        c.comboGap = a.thirdMargin;
        c.stateMode = st == null ? AppConfig.MODE_CHAOS : st.lifecycle.name();
        c.stateKey = st == null ? "" : st.prototypeId;
        c.shapeKey = c.stateKey;
        c.effectiveSample = st == null ? 0 : st.runLength;
        c.driftRisk = st == null ? 1.0d : st.changePointRisk;
        c.trendLife = st == null ? 0.0d : Math.min(100.0d, st.remainingLife);
        c.recoveryScore = st == null ? 0.0d : st.confidence * 100.0d;
        return c;
    }

    private static Models.ShapeVector toShape(
            V51Models.PredictionContext ctx,
            V51Models.PositionAction a,
            V51Models.StateTrajectorySnapshot st,
            double[] p) {
        Models.ShapeVector v = new Models.ShapeVector();
        v.baseIssue = ctx.baseIssue;
        v.targetIssue = ctx.targetIssue;
        v.position = a.position;
        v.createdAt = ctx.createdAt;
        v.stateMode = st == null ? AppConfig.MODE_CHAOS : st.lifecycle.name();
        v.stateKey = st == null ? "" : st.prototypeId;
        v.shapeKey = v.stateKey;
        v.trendLife = st == null ? 0 : Math.min(100, st.remainingLife);
        v.driftRisk = st == null ? 100 : st.changePointRisk * 100.0d;
        v.recoveryScore = st == null ? 0 : st.confidence * 100.0d;
        v.championGap = a.rankMargin;
        v.comboGap = a.thirdMargin;
        v.entropy32 = a.entropy;
        v.entropy64 = a.entropy;
        v.entropy128 = a.entropy;
        v.top3Mass32 = a.rawActionP;
        v.top3Mass128 = a.rawActionP;
        v.top3Mass240 = a.rawActionP;
        v.top6Mass240 = Math.min(1.0d, a.rawActionP + 0.30d);
        v.numberProbs = new double[11];
        if (p != null) for (int i = 0; i < Math.min(10, p.length); i++) v.numberProbs[i + 1] = p[i];
        return v;
    }

    private static List<Models.Draw> boundHistory(List<Models.Draw> h, int n) {
        ArrayList<Models.Draw> x = new ArrayList<Models.Draw>();
        if (h == null) return x;
        int st = Math.max(0, h.size() - n);
        for (int i = st; i < h.size(); i++)
            if (h.get(i) != null && h.get(i).isValid()) x.add(h.get(i));
        return x;
    }

    private static List<PatternStateEngine.DrawRecord> patternRows(List<Models.Draw> h) {
        ArrayList<PatternStateEngine.DrawRecord> x = new ArrayList<PatternStateEngine.DrawRecord>();
        if (h == null) return x;
        for (Models.Draw d : h)
            x.add(new PatternStateEngine.DrawRecord(d.issue, d.toOpenCodeText(), d.openTime));
        return x;
    }

    private static List<NumberBehaviorEngine.DrawRecord> numberRows(List<Models.Draw> h) {
        ArrayList<NumberBehaviorEngine.DrawRecord> x =
                new ArrayList<NumberBehaviorEngine.DrawRecord>();
        if (h == null) return x;
        for (Models.Draw d : h)
            x.add(new NumberBehaviorEngine.DrawRecord(d.issue, d.toOpenCodeText(), d.openTime));
        return x;
    }

    private static void markTiming(StringBuilder out, String stage, long beginNanos) {
        if (out.length() > 0) out.append('|');
        out.append(stage)
                .append('=')
                .append(Math.max(0L, System.nanoTime() - beginNanos) / 1000000L)
                .append("ms");
    }

    private static double clamp(double x) {
        if (Double.isNaN(x) || Double.isInfinite(x)) return 0;
        if (x < 0) return 0;
        if (x > 1) return 1;
        return x;
    }
}
