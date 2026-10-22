package com.ethk3.novaonepro;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Legacy Batch-3 certificate V2 reference. Current CP4 authority is CertificateBundleServiceR929
 * with ResearchGatePlanR967.installed(); this ladder must never certify current CP4 REAL.
 *
 * <p>Certificate authority is deliberately limited to ranking influence.  It can never create a
 * REAL/BET decision; DecisionGateEngine remains the sole decision owner.</p>
 */
public final class CertificateServiceV2R929 {
    public static final String REVISION = "R929_CERTIFICATE_V2";
    public static final int L5_NEW_FUTURE_N = 120;
    public static final int L6_ADDITIONAL_FUTURE_N = 240;
    public static final int L6_CUMULATIVE_FUTURE_N = L5_NEW_FUTURE_N + L6_ADDITIONAL_FUTURE_N;
    public static final int WEIGHT_STEP_1_N = 600;
    public static final int WEIGHT_STEP_2_N = 840;
    public static final int WEIGHT_STEP_3_N = 1200;
    private CertificateServiceV2R929() {}

    public enum Namespace { ALGORITHM, GRAPH_ASSET, GENESIS_ASSET, MODEL_BUNDLE }
    public enum Level { L0, L1, L2, L3, L4, L5, L6, L7 }
    public enum State { ACTIVE, PAUSED, REVOKED, LEGACY_REVIEW }
    public enum Consumer { NONE, SHADOW_REORDER, CONTROLLED_RANKING }

    public static final class Identity {
        public final Namespace namespace;
        public final String candidateId, familyId, modelId, bundleHash, schemaHash;
        public final String selectionPolicyHash, domainSchemaHash, statisticsProtocol, anchorIssue;
        public final String hash;

        public Identity(Namespace namespace, String candidateId, String familyId, String modelId,
                        String bundleHash, String schemaHash, String selectionPolicyHash,
                        String domainSchemaHash, String statisticsProtocol, String anchorIssue) {
            this.namespace = namespace == null ? Namespace.ALGORITHM : namespace;
            this.candidateId = required("candidate", candidateId);
            this.familyId = required("family", familyId);
            this.modelId = required("model", modelId);
            this.bundleHash = requireHash("bundle", bundleHash);
            this.schemaHash = requireHash("schema", schemaHash);
            this.selectionPolicyHash = requireHash("selection", selectionPolicyHash);
            this.domainSchemaHash = requireHash("domain", domainSchemaHash);
            this.statisticsProtocol = required("statistics", statisticsProtocol);
            this.anchorIssue = required("anchor", anchorIssue);
            this.hash = V51ChecksumEngine.sha256(REVISION + '|' + this.namespace.name() + '|'
                    + this.candidateId + '|' + this.familyId + '|' + this.modelId + '|'
                    + this.bundleHash + '|' + this.schemaHash + '|' + this.selectionPolicyHash + '|'
                    + this.domainSchemaHash + '|' + this.statisticsProtocol + '|' + this.anchorIssue);
        }
    }

    public static final class Gate {
        public final String name;
        public final double observed, required;
        public final boolean pass;
        public final String source;

        Gate(String name, double observed, double required, boolean pass, String source) {
            this.name = required("gate", name);
            this.observed = observed;
            this.required = required;
            this.pass = pass;
            this.source = required("source", source);
        }

        String canonical() {
            return name + "=" + fmt(observed) + '/' + fmt(required) + ':' + (pass ? "PASS" : "FAIL")
                    + "@" + source;
        }
    }

    public static final class Evidence {
        public final int futureN, pairedN, hits, reliabilityBinMinN, stableFolds;
        public final double hitRate, wilsonLower, brier, ece, coverage, pairedDelta;
        public final double adjustedP, fdrQ;
        public final boolean pitPass, hostParityPass, androidParityPass, deterministicPass;
        public final boolean resourcePass, selectionCalibrationBound, domainSupported;
        public final String windowId, evidenceHash, inputHash, domainId;

        public Evidence(int futureN, int pairedN, int hits, int reliabilityBinMinN, int stableFolds,
                        double hitRate, double wilsonLower, double brier, double ece, double coverage,
                        double pairedDelta, double adjustedP, double fdrQ, boolean pitPass,
                        boolean hostParityPass, boolean androidParityPass, boolean deterministicPass,
                        boolean resourcePass, boolean selectionCalibrationBound, boolean domainSupported,
                        String windowId, String evidenceHash, String inputHash, String domainId) {
            this.futureN = Math.max(0, futureN);
            this.pairedN = Math.max(0, pairedN);
            this.hits = Math.max(0, hits);
            this.reliabilityBinMinN = Math.max(0, reliabilityBinMinN);
            this.stableFolds = Math.max(0, stableFolds);
            this.hitRate = boundedOrNaN(hitRate);
            this.wilsonLower = boundedOrNaN(wilsonLower);
            this.brier = nonNegativeOrNaN(brier);
            this.ece = boundedOrNaN(ece);
            this.coverage = boundedOrNaN(coverage);
            this.pairedDelta = finiteOrNaN(pairedDelta);
            this.adjustedP = boundedOrNaN(adjustedP);
            this.fdrQ = boundedOrNaN(fdrQ);
            this.pitPass = pitPass;
            this.hostParityPass = hostParityPass;
            this.androidParityPass = androidParityPass;
            this.deterministicPass = deterministicPass;
            this.resourcePass = resourcePass;
            this.selectionCalibrationBound = selectionCalibrationBound;
            this.domainSupported = domainSupported;
            this.windowId = required("window", windowId);
            this.evidenceHash = requireHash("evidence", evidenceHash);
            this.inputHash = requireHash("input", inputHash);
            this.domainId = required("domain", domainId);
            if (this.hits > this.futureN) throw new IllegalArgumentException("R929_CERT_HITS_GT_N");
        }
    }

    public static final class Certificate {
        public final Identity identity;
        public final Evidence evidence;
        public final Level level;
        public final State state;
        public final Consumer consumer;
        public final double maxInfluenceWeight;
        public final Map<String, Gate> gates;
        public final String reason, certificateHash;
        public final boolean canCreateReal = false;

        Certificate(Identity identity, Evidence evidence, Level level, State state, Consumer consumer,
                    double maxInfluenceWeight, Map<String, Gate> gates, String reason) {
            this.identity = identity;
            this.evidence = evidence;
            this.level = level;
            this.state = state;
            this.consumer = consumer;
            this.maxInfluenceWeight = maxInfluenceWeight;
            this.gates = Collections.unmodifiableMap(new LinkedHashMap<String, Gate>(gates));
            this.reason = required("reason", reason);
            StringBuilder b = new StringBuilder(1024);
            b.append(REVISION).append('|').append(identity.hash).append('|')
                    .append(evidence.evidenceHash).append('|').append(level.name()).append('|')
                    .append(state.name()).append('|').append(consumer.name()).append('|')
                    .append(fmt(maxInfluenceWeight)).append('|').append(reason);
            for (Gate gate : this.gates.values()) b.append('|').append(gate.canonical());
            certificateHash = V51ChecksumEngine.sha256(b.toString());
        }

        public boolean permitsRankingInfluence() {
            return state == State.ACTIVE && consumer == Consumer.CONTROLLED_RANKING
                    && level.ordinal() >= Level.L6.ordinal() && maxInfluenceWeight > 0.0d;
        }
    }

    public static Certificate evaluate(Identity id, Evidence e) {
        if (id == null || e == null) throw new IllegalArgumentException("R929_CERT_INPUT_NULL");
        LinkedHashMap<String, Gate> gates = new LinkedHashMap<String, Gate>();
        put(gates, new Gate("PIT", e.pitPass ? 1 : 0, 1, e.pitPass, "FeatureDagContractR929"));
        put(gates, new Gate("HOST_PARITY", e.hostParityPass ? 1 : 0, 1, e.hostParityPass, "host parity"));
        put(gates, new Gate("ANDROID_PARITY", e.androidParityPass ? 1 : 0, 1, e.androidParityPass, "device parity"));
        put(gates, new Gate("DETERMINISTIC", e.deterministicPass ? 1 : 0, 1, e.deterministicPass, "replay hash"));
        put(gates, new Gate("RESOURCE", e.resourcePass ? 1 : 0, 1, e.resourcePass, "same-device budget"));
        put(gates, new Gate("CAL_BINDING", e.selectionCalibrationBound ? 1 : 0, 1,
                e.selectionCalibrationBound, "selection policy hash"));
        put(gates, new Gate("DOMAIN_SUPPORT", e.domainSupported ? 1 : 0, 1,
                e.domainSupported, "predraw domain certificate"));
        put(gates, new Gate("FUTURE_N_L5", e.futureN, L5_NEW_FUTURE_N,
                e.futureN >= L5_NEW_FUTURE_N, e.windowId));
        put(gates, new Gate("FUTURE_N_L6", e.futureN, L6_CUMULATIVE_FUTURE_N,
                e.futureN >= L6_CUMULATIVE_FUTURE_N, e.windowId));
        put(gates, new Gate("STABLE_FOLDS", e.stableFolds, 5, e.stableFolds >= 5, "nested OOS"));
        put(gates, new Gate("ECE", e.ece, 0.04d, finite(e.ece) && e.ece <= 0.04d, "sealed/future calibration"));
        put(gates, new Gate("RELIABILITY_BIN_MIN_N", e.reliabilityBinMinN, 20,
                e.reliabilityBinMinN >= 20, "calibration bins"));
        put(gates, new Gate("MULTI_COMPARE", bestAdjusted(e), 0.05d,
                finite(bestAdjusted(e)) && bestAdjusted(e) <= 0.05d,
                "pre-registered conservative/FDR protocol"));

        boolean hard = e.pitPass && e.hostParityPass && e.deterministicPass && e.resourcePass;
        if (!hard) return new Certificate(id, e, Level.L2, State.PAUSED, Consumer.NONE, 0.0d,
                gates, "HARD_ENGINEERING_GATE_FAILED");
        if (!e.androidParityPass)
            return new Certificate(id, e, Level.L3, State.PAUSED, Consumer.NONE, 0.0d,
                    gates, "ANDROID_PARITY_REQUIRED");
        if (!e.selectionCalibrationBound)
            return new Certificate(id, e, Level.L4, State.PAUSED, Consumer.NONE, 0.0d,
                    gates, "SELECTION_CALIBRATION_NOT_BOUND");
        if (e.futureN < L5_NEW_FUTURE_N)
            return new Certificate(id, e, Level.L4, State.ACTIVE, Consumer.NONE, 0.0d,
                    gates, "WAIT_FUTURE_L5");
        if (!finite(e.ece) || e.ece > 0.04d || e.reliabilityBinMinN < 20)
            return new Certificate(id, e, Level.L4, State.PAUSED, Consumer.NONE, 0.0d,
                    gates, "CALIBRATION_GATE_FAILED");
        if (e.futureN < L6_CUMULATIVE_FUTURE_N)
            return new Certificate(id, e, Level.L5, State.ACTIVE, Consumer.SHADOW_REORDER, 0.0d,
                    gates, "L5_SHADOW_REORDER_ONLY");
        if (!e.domainSupported)
            return new Certificate(id, e, Level.L5, State.PAUSED, Consumer.SHADOW_REORDER, 0.0d,
                    gates, "DOMAIN_UNSUPPORTED_FALLBACK_GLOBAL");
        double p = bestAdjusted(e);
        if (!finite(p) || p > 0.05d || e.stableFolds < 5)
            return new Certificate(id, e, Level.L5, State.ACTIVE, Consumer.SHADOW_REORDER, 0.0d,
                    gates, "STATISTICAL_GATE_NOT_MET");

        double weight = 0.05d;
        Level level = Level.L6;
        String reason = "L6_CONTROLLED_RANKING_5_PERCENT";
        if (e.futureN >= WEIGHT_STEP_3_N) { weight = 0.08d; level = Level.L7; reason = "L7_1200_FUTURE_WEIGHT_CAP_8_PERCENT"; }
        else if (e.futureN >= WEIGHT_STEP_2_N) { weight = 0.07d; level = Level.L7; reason = "L7_840_FUTURE_WEIGHT_CAP_7_PERCENT"; }
        else if (e.futureN >= WEIGHT_STEP_1_N) { weight = 0.06d; level = Level.L7; reason = "L7_600_FUTURE_WEIGHT_CAP_6_PERCENT"; }
        return new Certificate(id, e, level, State.ACTIVE, Consumer.CONTROLLED_RANKING, weight,
                gates, reason);
    }

    public static Certificate legacyReview(Identity id, String legacyEvidenceHash) {
        Evidence e = new Evidence(0,0,0,0,0,Double.NaN,Double.NaN,Double.NaN,Double.NaN,
                Double.NaN,Double.NaN,Double.NaN,Double.NaN,false,false,false,false,false,false,false,
                "LEGACY_NO_NEW_WINDOW", requireHash("legacy", legacyEvidenceHash),
                id.schemaHash,"GLOBAL");
        return new Certificate(id,e,Level.L0,State.LEGACY_REVIEW,Consumer.NONE,0.0d,
                Collections.<String,Gate>emptyMap(),"LEGACY_REVIEW_ZERO_WEIGHT");
    }

    private static double bestAdjusted(Evidence e) {
        if (finite(e.adjustedP) && finite(e.fdrQ)) return Math.max(e.adjustedP, e.fdrQ);
        if (finite(e.adjustedP)) return e.adjustedP;
        if (finite(e.fdrQ)) return e.fdrQ;
        return Double.NaN;
    }
    private static void put(Map<String,Gate> m, Gate g) { m.put(g.name,g); }
    private static double boundedOrNaN(double v) { return finite(v) && v >= 0 && v <= 1 ? v : Double.NaN; }
    private static double nonNegativeOrNaN(double v) { return finite(v) && v >= 0 ? v : Double.NaN; }
    private static double finiteOrNaN(double v) { return finite(v) ? v : Double.NaN; }
    private static boolean finite(double v) { return !Double.isNaN(v) && !Double.isInfinite(v); }
    private static String required(String field,String value) {
        String x=value==null?"":value.trim(); if(x.length()==0)throw new IllegalArgumentException("R929_CERT_"+field+"_REQUIRED"); return x;
    }
    private static String requireHash(String field,String value) {
        String x=required(field,value).toLowerCase(Locale.US); if(!x.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("R929_CERT_"+field+"_HASH"); return x;
    }
    private static String fmt(double x) { return finite(x)?String.format(Locale.US,"%.8f",x):"N/A"; }
}
