package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/**
 * R31-B07-C06: typed research hypotheses derived from a measured failure.
 *
 * <p>Each failure mechanism dispatches to its own family and action with a falsifiable prediction, a fixed control,
 * an applicability domain, a minimum support and a paired protocol; a mechanism with no research remedy (a resource
 * failure) is UNSUPPORTED instead of being routed to a random mutation. Inputs may only come from TRAIN/SELECT (or
 * an already revealed failed look, as research input only) and only through the decision's previous period (t-1). Outcomes update development credit; formal confirmation or
 * future evidence is never inherited by a child.</p>
 */
public final class ResearchHypothesisR31 {
    private ResearchHypothesisR31() {}
    public static final String VERSION = "R31_RESEARCH_HYPOTHESIS_V1";
    public enum Mechanism { CALIBRATION, COVERAGE, SHORT_WINDOW_DRIFT, COMPONENT_NEGATIVE, CROSS_POSITION_SUPPORT, RESOURCE_FAILURE }

    /** One measured failure of a parent policy. */
    public static final class Failure {
        public final String parentHash, sourceRun, component, inputRole, reason; public final Mechanism mechanism;
        public final long decisionOrdinal, inputAvailableThrough, support; public final double measured;
        public Failure(String parentHash, String sourceRun, Mechanism mechanism, String reason, String component, String inputRole, long decisionOrdinal, long inputAvailableThrough, long support, double measured) {
            this.parentHash = parentHash; this.sourceRun = sourceRun; this.mechanism = mechanism; this.reason = reason == null ? "" : reason; this.component = component == null ? "" : component;
            this.inputRole = inputRole; this.decisionOrdinal = decisionOrdinal; this.inputAvailableThrough = inputAvailableThrough; this.support = support; this.measured = measured;
        }
    }
    public static final class Hypothesis {
        public final String id, parentHash, sourceRun, component, family, action, domain, prediction, control, pairing, status, unsupportedReason;
        public final Mechanism mechanism; public final long minimumSupport, budgetUnits;
        Hypothesis(Failure f, String family, String action, String domain, String prediction, String control, long minimumSupport, long budgetUnits, String status, String unsupportedReason) {
            this.parentHash = f.parentHash; this.sourceRun = f.sourceRun; this.component = f.component; this.mechanism = f.mechanism; this.family = family; this.action = action; this.domain = domain;
            this.prediction = prediction; this.control = control; this.pairing = "SAME_OPPORTUNITY_PARENT_CHILD_PAIRED_V1"; this.minimumSupport = minimumSupport; this.budgetUnits = budgetUnits;
            this.status = status; this.unsupportedReason = unsupportedReason == null ? "" : unsupportedReason;
            this.id = StatsR27.sha256(VERSION + "|" + f.parentHash + "|" + f.sourceRun + "|" + f.mechanism + "|" + f.component + "|" + action);
        }
        public boolean supported() { return "REGISTERED".equals(status); }
        public Map<String, Object> json() {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("version", VERSION); m.put("id", id); m.put("parentHash", parentHash); m.put("sourceRun", sourceRun); m.put("mechanism", mechanism.name()); m.put("component", component);
            m.put("family", family); m.put("action", action); m.put("domain", domain); m.put("falsifiablePrediction", prediction); m.put("control", control); m.put("pairing", pairing);
            m.put("minimumSupport", minimumSupport); m.put("budgetUnits", budgetUnits); m.put("status", status); m.put("unsupportedReason", unsupportedReason);
            m.put("formalEvidenceInherited", false);
            return m;
        }
    }

    /** Mechanism of a qualification failure reason recorded by the live qualification (R26 reasons). */
    public static Mechanism mechanismOf(String qualificationReason) {
        switch (qualificationReason) {
            case "COVERAGE_LT_0.15": return Mechanism.COVERAGE;
            case "BLOCK_BELOW_FLOOR": return Mechanism.SHORT_WINDOW_DRIFT;
            case "PAIRED_LCB_BELOW_PREREGISTERED_MINUS_001": return Mechanism.COMPONENT_NEGATIVE;
            case "OBSERVED_HIT_RATE_LT_035": return Mechanism.CROSS_POSITION_SUPPORT;
            case "HELD_LATER_ECE_GT_004": return Mechanism.CALIBRATION;
            default: throw new IllegalArgumentException("R31_HYPOTHESIS_UNKNOWN_FAILURE_REASON:" + qualificationReason);
        }
    }

    /** Typed hypothesis for one failure; formal inputs or inputs at/after the decision are refused. */
    public static Hypothesis fromFailure(Failure f) {
        if (f == null || f.parentHash == null || !f.parentHash.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("R31_HYPOTHESIS_PARENT");
        if (f.sourceRun == null || f.sourceRun.isEmpty()) throw new IllegalArgumentException("R31_HYPOTHESIS_SOURCE_REQUIRED");
        // A revealed failed qualification look is research input only (R26); unrevealed CONFIRM/FUTURE never is.
        if (!"TRAIN".equals(f.inputRole) && !"SELECT".equals(f.inputRole) && !"REVEALED_FAILED_LOOK".equals(f.inputRole)) throw new IllegalArgumentException("R31_HYPOTHESIS_FORMAL_INPUT:" + f.inputRole);
        if (f.inputAvailableThrough > f.decisionOrdinal - 1) throw new IllegalArgumentException("R31_HYPOTHESIS_FUTURE_INPUT:" + f.inputAvailableThrough + ">=" + f.decisionOrdinal);
        if (!Double.isFinite(f.measured)) throw new IllegalArgumentException("R31_HYPOTHESIS_MEASURE");
        switch (f.mechanism) {
            case CALIBRATION: return registered(f, "I10", "READOUT_TEMPERATURE_REFIT_ON_TRAIN", "SAME_SCORING_SAME_POSITION",
                    "child expected calibration error < parent on the same SELECT opportunities", "PARENT_READOUT_UNCHANGED", 200);
            case COVERAGE: return registered(f, "I13", "SELECTOR_RATE_REDESIGN_FIXED_SCORING", "SAME_SCORING_SAME_POSITION",
                    "child act frequency rises to the registered rate without a lower paired utility", "PARENT_SELECTOR_UNCHANGED", 200);
            case SHORT_WINDOW_DRIFT: return registered(f, "I06", "REGIME_WINDOW_AND_EXPIRY_F10_ROUTER", "PAST_STATE_ONLY_LAG_GE_1",
                    "child worst-block utility > parent worst block; no advantage on stationary control blocks", "STATIONARY_SHUFFLED_BLOCK_CONTROL", 300);
            case COMPONENT_NEGATIVE:
                if (f.component.isEmpty()) return unsupported(f, "I10", "COMPONENT_NOT_IDENTIFIED");
                return registered(f, "I10", "PRUNE_OR_REPLACE_MEASURED_COMPONENT", "SAME_PARENT_WITHOUT_COMPONENT",
                    "child without the component has paired utility >= parent", "PARENT_WITH_COMPONENT", 200);
            case CROSS_POSITION_SUPPORT: return registered(f, "I04", "CROSS_POSITION_STRICT_INTERSECTION_K3", "LAG_GE_1_SINGLE_TARGET_POSITION",
                    "intersection support predicts the target position above base on the same opportunities; abstains when |C|<3", "PARENT_SINGLE_POSITION_SCORING", 300);
            case RESOURCE_FAILURE: return unsupported(f, "", "RESOURCE_FAILURE_IS_NOT_A_STRATEGY_HYPOTHESIS");
            default: throw new IllegalStateException("R31_HYPOTHESIS_MECHANISM");
        }
    }
    private static Hypothesis registered(Failure f, String family, String action, String domain, String prediction, String control, long minimumSupport) {
        if (f.support < minimumSupport) return new Hypothesis(f, family, action, domain, prediction, control, minimumSupport, 0, "UNSUPPORTED", "INSUFFICIENT_SUPPORT:" + f.support + "<" + minimumSupport);
        return new Hypothesis(f, family, action, domain, prediction, control, minimumSupport, 4L * minimumSupport, "REGISTERED", "");
    }
    private static Hypothesis unsupported(Failure f, String family, String reason) { return new Hypothesis(f, family, "NONE", "", "", "", 0, 0, "UNSUPPORTED", reason); }

    /** Registry of hypotheses and their development outcomes. */
    public static final class Registry {
        private final Map<String, Hypothesis> hypotheses = new LinkedHashMap<>();
        private final Map<String, double[]> developmentCredit = new TreeMap<>(); // family -> {sum paired gain, n}
        private final Set<String> recorded = new HashSet<>();
        public Hypothesis register(Hypothesis h) { Hypothesis prior = hypotheses.putIfAbsent(h.id, h); return prior == null ? h : prior; }
        public List<Hypothesis> all() { return Collections.unmodifiableList(new ArrayList<>(hypotheses.values())); }
        /** Action for the hypothesis; unsupported hypotheses dispatch to nothing. */
        public String dispatch(Hypothesis h) { if (!hypotheses.containsKey(h.id)) throw new IllegalStateException("R31_HYPOTHESIS_NOT_REGISTERED"); return h.supported() ? h.family + ":" + h.action : "UNSUPPORTED:" + h.unsupportedReason; }
        /** Development (SELECT) outcome of one child; confirmation/future roles are refused. Returns whether it counted. */
        public boolean recordOutcome(Hypothesis h, String childHash, String role, double pairedGain) {
            if (!h.supported()) throw new IllegalStateException("R31_HYPOTHESIS_UNSUPPORTED_OUTCOME");
            if (!"SELECT".equals(role)) throw new IllegalArgumentException("R31_HYPOTHESIS_FORMAL_OUTCOME:" + role);
            if (!Double.isFinite(pairedGain)) throw new IllegalArgumentException("R31_HYPOTHESIS_GAIN");
            if (!recorded.add(h.id + "|" + childHash)) return false;
            double[] c = developmentCredit.computeIfAbsent(h.family, k -> new double[2]); c[0] += pairedGain; c[1]++;
            return true;
        }
        public double developmentCredit(String family) { double[] c = developmentCredit.get(family); return c == null || c[1] == 0 ? 0 : c[0] / c[1]; }
    }
}
