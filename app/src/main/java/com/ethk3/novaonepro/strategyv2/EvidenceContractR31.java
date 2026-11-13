package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/**
 * R31-B10-C02/C03: a versioned evidence contract for new experiments, beside the unchanged R27_GATE_V1.
 *
 * <p>A contract records the primary objective, risk definition, evaluation window, opportunity denominator, stop /
 * drawdown rule, calibration rule, minimum support, error budget, power target, target act rate with its tolerance
 * and the applicable devices. It has no issuing authority until its whole search pipeline was validated (null false
 * alarm upper bound within the error budget and power at or above the target, from a COMPLETE registered
 * experiment); otherwise new experiments stay OBSERVE/WAIT. Activation never touches V1 certificates, rejections or
 * the V1 mainline choice: a V2/R31 "would issue" result cannot change REAL selection.</p>
 */
public final class EvidenceContractR31 {
    private EvidenceContractR31() {}
    public static final String VERSION = "R31_EVIDENCE_CONTRACT_V1";
    public enum State { REGISTERED, VALIDATED, ACTIVE, OBSERVE_ONLY }

    public static final class Contract {
        public final String id, primaryObjective, riskDefinition, evaluationWindow, opportunityDenominator, stopRule, calibrationRule, applicableDevices;
        public final long minSupport; public final double errorBudget, powerTarget, targetActRate, actRateTolerance; public final String hash;
        Contract(String id, String objective, String risk, String window, String denominator, String stop, String calibration, long minSupport, double errorBudget, double powerTarget, double targetActRate, double tolerance, String devices) {
            this.id = id; primaryObjective = objective; riskDefinition = risk; evaluationWindow = window; opportunityDenominator = denominator; stopRule = stop; calibrationRule = calibration;
            this.minSupport = minSupport; this.errorBudget = errorBudget; this.powerTarget = powerTarget; this.targetActRate = targetActRate; actRateTolerance = tolerance; applicableDevices = devices;
            hash = StatsR27.sha256(VERSION + "|" + id + "|" + objective + "|" + risk + "|" + window + "|" + denominator + "|" + stop + "|" + calibration + "|" + minSupport + "|" + errorBudget + "|" + powerTarget + "|" + targetActRate + "|" + tolerance + "|" + devices);
        }
        public Map<String,Object> json() {
            Map<String,Object> m=new LinkedHashMap<>();m.put("version",VERSION);m.put("id",id);m.put("hash",hash);
            m.put("primaryObjective",primaryObjective);m.put("riskDefinition",riskDefinition);m.put("evaluationWindow",evaluationWindow);
            m.put("opportunityDenominator",opportunityDenominator);m.put("stopRule",stopRule);m.put("calibrationRule",calibrationRule);
            m.put("minSupport",minSupport);m.put("errorBudget",errorBudget);m.put("powerTarget",powerTarget);
            m.put("targetActRate",targetActRate);m.put("actRateTolerance",actRateTolerance);m.put("applicableDevices",applicableDevices);return m;
        }
    }
    /** Registration requires every field; thresholds are fixed here, before any result is seen. */
    public static Contract register(String id, String objective, String risk, String window, String denominator, String stop, String calibration, long minSupport, double errorBudget, double powerTarget, double targetActRate, double tolerance, String devices) {
        for (String f : new String[]{id, objective, risk, window, denominator, stop, calibration, devices}) if (f == null || f.isEmpty()) throw new IllegalArgumentException("R31_CONTRACT_FIELD_REQUIRED");
        if (id.equals(CertificateGateR27.GATE_VERSION)) throw new IllegalArgumentException("R31_CONTRACT_CANNOT_REUSE_V1_IDENTITY");
        if (minSupport < 1 || !(errorBudget > 0 && errorBudget < 1) || !(powerTarget > 0 && powerTarget < 1) || !(targetActRate > 0 && targetActRate <= 1) || !(tolerance >= 0 && tolerance < targetActRate))
            throw new IllegalArgumentException("R31_CONTRACT_THRESHOLDS");
        return new Contract(id, objective, risk, window, denominator, stop, calibration, minSupport, errorBudget, powerTarget, targetActRate, tolerance, devices);
    }

    public static final class Validation {
        public final String contractHash, protocolHash; public final boolean passed; public final List<String> reasons; public final double farUpper95, powerLower95;
        Validation(String contract, String protocol, boolean passed, List<String> reasons, double far, double power) { contractHash = contract; protocolHash = protocol; this.passed = passed; this.reasons = Collections.unmodifiableList(reasons); farUpper95 = far; powerLower95 = power; }
    }
    /** Whole-pipeline validation from a registered capability experiment (null + signal arms). */
    public static Validation validateDesign(Contract c, ResearchCapabilityExperimentR31.Summary s) {
        List<String> reasons = new ArrayList<>();
        if (!"COMPLETE".equals(s.status)) reasons.add("EXPERIMENT_" + s.status);
        if (!(s.farUpper95 <= c.errorBudget)) reasons.add("FALSE_ALARM_UPPER95_ABOVE_BUDGET:" + s.farUpper95 + ">" + c.errorBudget);
        if (!(s.powerLower95 >= c.powerTarget)) reasons.add("POWER_LOWER95_BELOW_TARGET:" + s.powerLower95 + "<" + c.powerTarget);
        if (s.nullTrials < 1 || s.signalTrials < 1) reasons.add("MISSING_NULL_OR_SIGNAL_ARM");
        return new Validation(c.hash, s.protocolHash, reasons.isEmpty(), reasons, s.farUpper95, s.powerLower95);
    }
    public static final class Active {
        public final Contract contract; public final Validation validation; public final State state;
        Active(Contract c, Validation v, State s) { contract = c; validation = v; state = s; }
        /** Issuing authority for a NEW experiment registered under this contract; V1 experiments are never re-judged. */
        public boolean mayIssueFor(String experimentContract) { return state == State.ACTIVE && contract.hash.equals(experimentContract); }
    }
    /** ACTIVE only with a passed validation for this exact contract; otherwise OBSERVE_ONLY (no certificate). */
    public static Active activate(Contract c, Validation v) {
        if (v == null || !v.passed || !c.hash.equals(v.contractHash)) return new Active(c, v, State.OBSERVE_ONLY);
        return new Active(c, v, State.ACTIVE);
    }
    /** The authority in force: V1 unless a validated R31 contract is active for that experiment. */
    public static String authorityFor(Active active, String experimentContract) {
        return active != null && active.mayIssueFor(experimentContract) ? active.contract.id : CertificateGateR27.GATE_VERSION;
    }

    /** R31-B10-C03: canonical opportunity denominators of one analysis window. */
    public static final class Opportunities {
        public final long eligible, act, abstain, missedInput, missedDeadline, unknown;
        public Opportunities(long eligible, long act, long abstain, long missedInput, long missedDeadline, long unknown) {
            if (eligible < 0 || act < 0 || abstain < 0 || missedInput < 0 || missedDeadline < 0 || unknown < 0 || act + abstain > eligible) throw new IllegalArgumentException("R31_OPPORTUNITY_COUNTS");
            this.eligible = eligible; this.act = act; this.abstain = abstain; this.missedInput = missedInput; this.missedDeadline = missedDeadline; this.unknown = unknown;
        }
        public long scheduled() { return eligible + missedInput + missedDeadline + unknown; }
    }
    /** Act frequency against the registered target: missed and unknown periods stay in the report, never silently
     * removed; an act rate in range without a passed quality gate is not "achieved"; nothing is filled to reach a rate. */
    public static Map<String, Object> frequencyReport(Contract c, Opportunities o, boolean qualityPassed, String selectorSupportDomain) {
        Map<String, Object> m = new LinkedHashMap<>();
        double rate = o.eligible == 0 ? Double.NaN : o.act / (double) o.eligible;
        String band = !Double.isFinite(rate) ? "NO_ELIGIBLE_OPPORTUNITY" : rate < c.targetActRate - c.actRateTolerance ? "BELOW_TARGET" : rate > c.targetActRate + c.actRateTolerance ? "ABOVE_TARGET" : "IN_TARGET_RANGE";
        m.put("contract", c.id); m.put("targetActRate", c.targetActRate); m.put("tolerance", c.actRateTolerance);
        m.put("eligible", o.eligible); m.put("act", o.act); m.put("abstain", o.abstain); m.put("missedInput", o.missedInput); m.put("missedDeadline", o.missedDeadline); m.put("unknown", o.unknown); m.put("scheduled", o.scheduled());
        m.put("actRate", Double.isFinite(rate) ? (Object) rate : null); m.put("frequencyBand", band);
        if ("BELOW_TARGET".equals(band)) { m.put("insufficientReason", "SELECTOR_SUPPORT_DOMAIN_TOO_NARROW_OR_NO_QUALIFIED_CANDIDATE"); m.put("selectorSupportDomain", selectorSupportDomain); }
        m.put("qualityPassed", qualityPassed);
        m.put("achieved", "IN_TARGET_RANGE".equals(band) && qualityPassed);
        m.put("abstentionIsLawful", true); m.put("actsFabricated", false);
        return m;
    }
}
