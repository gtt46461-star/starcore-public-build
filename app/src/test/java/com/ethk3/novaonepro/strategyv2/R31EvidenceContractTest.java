package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

/** R31-B10-C02#A1-#A4 and R31-B10-C03#A1/#A2: streak-gate DP, V1 preserved, validated contracts, no alpha reset, frequency. */
public final class R31EvidenceContractTest {
    @Test public void streakDpMatchesAnalyticValues() {
        for (double p : new double[]{0, .3, .34, .4, 1}) assertEquals("n=0", 1, CertificateGateR27.streakGatePassProbabilityR31(0, p, 6), 0);
        for (int n = 0; n <= 6; n++) assertEquals("n<=6 can never exceed 6", 1, CertificateGateR27.streakGatePassProbabilityR31(n, 0, 6), 1e-15);
        assertEquals("p=0, n=7 always loses 7 in a row", 0, CertificateGateR27.streakGatePassProbabilityR31(7, 0, 6), 0);
        assertEquals("p=1 never loses", 1, CertificateGateR27.streakGatePassProbabilityR31(500, 1, 6), 1e-12);
        for (double p : new double[]{.2, .34, .4}) assertEquals("n=7: only all-miss fails", 1 - Math.pow(1 - p, 7), CertificateGateR27.streakGatePassProbabilityR31(7, p, 6), 1e-12);
        // n=8, k=1 by hand: sequences of length 8 without two consecutive misses (p=.5): F(10)/2^8 = 55/256
        assertEquals(55.0 / 256, CertificateGateR27.streakGatePassProbabilityR31(8, .5, 1), 1e-12);
        Map<String, Object> s = CertificateGateR27.streakGateSensitivityR31(250, new double[]{.34, .40});
        assertEquals("IID_SINGLE_STREAK_GATE_SENSITIVITY_NOT_A_LIVE_ESTIMATE", s.get("scope"));
        double at34 = (Double) ((Map<?, ?>) s.get("passProbabilityByHitRate")).get("0.34"), at40 = (Double) ((Map<?, ?>) s.get("passProbabilityByHitRate")).get("0.4");
        assertTrue("the single 6-streak gate alone rarely passes at 34-40%: " + at34 + "/" + at40, at34 < at40 && at40 < .5);
    }

    static CertificateGateR27.Evidence evidence(long acts, long hits, double ece, double worst, int streak, double i00, double nul) {
        CertificateGateR27.Evidence e = new CertificateGateR27.Evidence(); e.policyHash = StatsR27.sha256("p" + acts + hits + streak); e.sequentialState = SequentialQualificationR27.QUALIFIED;
        e.acts = acts; e.hits = hits; e.ece = ece; e.worstBlock = worst; e.maxLosingStreak = streak; e.abiEquivalent = true; e.pairedLcbVsI00 = i00; e.pairedLcbVsNull = nul; e.logE = 4; e.calibrationQuality = .5; return e;
    }
    @Test public void v1ConstantsAndVerdictsReplayExactlyAndAnUnvalidatedContractCannotIssue() {
        assertEquals("R27_GATE_V1", CertificateGateR27.GATE_VERSION);
        assertEquals(.34, CertificateGateR27.MIN_HR, 0); assertEquals(250, CertificateGateR27.MIN_ACTS); assertEquals(.04, CertificateGateR27.MAX_ECE, 0); assertEquals(.26, CertificateGateR27.MIN_WORST_BLOCK, 0); assertEquals(6, CertificateGateR27.MAX_LOSS_STREAK);
        Object[][] replay = {
            {evidence(250, 85, .03, .27, 6, .01, .01), Collections.emptyList()},
            {evidence(250, 84, .03, .27, 6, .01, .01), Collections.singletonList("hitRate>=0.34")},
            {evidence(249, 85, .03, .27, 6, .01, .01), Collections.singletonList("futureActs>=250")},
            {evidence(250, 85, .05, .27, 7, .01, .01), Arrays.asList("ece<=0.04", "losingStreak<=6")},
            {evidence(250, 85, .03, .25, 6, -.01, .01), Arrays.asList("worst720Block>=0.26", "betterThanI00")},
        };
        for (Object[] r : replay) {
            CertificateGateR27.Verdict v = CertificateGateR27.evaluate((CertificateGateR27.Evidence) r[0]);
            assertEquals(r[1], v.failing); assertEquals(((List<?>) r[1]).isEmpty(), v.active);
            Map<String, Object> report = CertificateGateR27.rejectionReportR31(v);
            assertEquals("COMPLETE", report.get("computation")); assertEquals(v.active ? "ISSUED" : "REJECTED_BY_GATES", report.get("result"));
        }
        EvidenceContractR31.Contract c = contract(.10, .70);
        EvidenceContractR31.Active unvalidated = EvidenceContractR31.activate(c, null);
        assertEquals(EvidenceContractR31.State.OBSERVE_ONLY, unvalidated.state); assertFalse(unvalidated.mayIssueFor(c.hash));
        assertEquals("V1 remains the authority", CertificateGateR27.GATE_VERSION, EvidenceContractR31.authorityFor(unvalidated, c.hash));
        try { EvidenceContractR31.register(CertificateGateR27.GATE_VERSION, "o", "r", "w", "d", "s", "c", 250, .05, .8, .15, .05, "x"); fail("V1 identity reused"); }
        catch (IllegalArgumentException expected) { assertEquals("R31_CONTRACT_CANNOT_REUSE_V1_IDENTITY", expected.getMessage()); }
    }
    static EvidenceContractR31.Contract contract(double errorBudget, double power) {
        return EvidenceContractR31.register("R31_RISK_CONTRACT_TEST", "PAIRED_UTILITY_PER_OPPORTUNITY", "MAX_DRAWDOWN_AND_CALIBRATION", "720_PERIOD_FUTURE_WINDOW", "ELIGIBLE_OPPORTUNITIES_INCLUDING_ABSTAIN",
                "STOP_ON_DRAWDOWN_OR_CALIBRATION_BREACH", "ECE_STRATIFIED", 250, errorBudget, power, .15, .05, "HOST_JVM_ONLY");
    }
    static ResearchCapabilityExperimentR31.Summary pipeline(double strength) {
        Map<ResearchCapabilityExperimentR31.Arm, long[]> arms = new EnumMap<>(ResearchCapabilityExperimentR31.Arm.class);
        arms.put(ResearchCapabilityExperimentR31.Arm.NULL_UNIFORM, R31ResearchCapabilityExperimentTest.seeds(7000, 40));
        arms.put(ResearchCapabilityExperimentR31.Arm.SIGNAL, R31ResearchCapabilityExperimentTest.seeds(8000, 16));
        ResearchCapabilityExperimentR31.Protocol p = ResearchCapabilityExperimentR31.register("R31_K08_CONTRACT_" + strength, arms, 900, 400, 600, 3, strength, .15, .70);
        return ResearchCapabilityExperimentR31.summarize(p, ResearchCapabilityExperimentR31.runAll(p, ResearchCapabilityExperimentR31.PRODUCTION_STAGES));
    }
    @Test public void aContractIsActivatedOnlyWhenTheWholePipelineMeetsItsRegisteredBudgets() {
        EvidenceContractR31.Contract c = contract(.15, .70);
        ResearchCapabilityExperimentR31.Summary strong = pipeline(.30), weak = pipeline(.02);
        EvidenceContractR31.Validation ok = EvidenceContractR31.validateDesign(c, strong), notOk = EvidenceContractR31.validateDesign(c, weak);
        assertTrue(ok.reasons.toString(), ok.passed);
        assertFalse("a weak signal does not reach the registered power", notOk.passed);
        assertTrue(notOk.reasons.toString(), notOk.reasons.toString().contains("POWER_LOWER95_BELOW_TARGET"));
        assertEquals(EvidenceContractR31.State.ACTIVE, EvidenceContractR31.activate(c, ok).state);
        assertEquals("not validated -> OBSERVE", EvidenceContractR31.State.OBSERVE_ONLY, EvidenceContractR31.activate(c, notOk).state);
        EvidenceContractR31.Contract other = contract(.05, .70);
        assertEquals("a validation belongs to one exact contract", EvidenceContractR31.State.OBSERVE_ONLY, EvidenceContractR31.activate(other, ok).state);
        EvidenceContractR31.Active active = EvidenceContractR31.activate(c, ok);
        assertEquals("R31_RISK_CONTRACT_TEST", EvidenceContractR31.authorityFor(active, c.hash));
        assertEquals("old V1 experiments are never re-judged by the new contract", CertificateGateR27.GATE_VERSION, EvidenceContractR31.authorityFor(active, "V1_EXPERIMENT"));
    }

    @Test public void restartsRenamesChildrenAndRepeatedEvidenceNeverResetAlphaOrExposure() {
        double previous = Double.POSITIVE_INFINITY, sum = 0;
        for (long i = 1; i <= 100000; i++) { double a = SequentialQualificationR27.alpha(i); assertTrue(a < previous); previous = a; sum += a; }
        assertTrue("alpha over every index ever issued stays inside the root budget: " + sum, sum < SequentialQualificationR27.ALPHA_ROOT);
        SequentialQualificationR27.State s = new SequentialQualificationR27.State(StatsR27.sha256("p"), 7, 1);
        for (int k = 0; k < 100; k++) s.observe(1000, true, true);
        assertEquals("one experiment fed the same period 100 times holds one future observation", 1, s.n);
        s.observe(999, true, true); assertEquals("an older period never re-enters", 1, s.n);
        SequentialQualificationR27.State renamed = new SequentialQualificationR27.State(StatsR27.sha256("p-renamed"), 8, 1);
        assertTrue("a renamed/child policy gets a new, smaller alpha, never the old budget again", renamed.alpha < s.alpha);
        assertEquals("PLANNING_ESTIMATE_AT_CURRENT_EFFECT_ESTIMATE_NOT_A_COMPLETION_PROMISE", s.json().get("expectedRemainingScope"));
        assertFalse("R29 V2 is report-only", GateV2R29.ENABLED);
        List<CertificateGateR27.Evidence> active = Arrays.asList(evidence(250, 90, .02, .3, 5, .01, .01), evidence(260, 95, .01, .3, 4, .01, .01));
        String choice = CertificateGateR27.choose(active);
        for (CertificateGateR27.Evidence e : active) { GateV2R29.Evidence v2 = new GateV2R29.Evidence(); v2.policyHash = e.policyHash; GateV2R29.evaluate(v2); }
        assertEquals("a V2 would-issue verdict cannot change the REAL choice", choice, CertificateGateR27.choose(active));
    }

    @Test public void frequencyIsReportedWithItsDenominatorsAndNeverFabricated() {
        EvidenceContractR31.Contract c = contract(.10, .70);
        Map<String, Object> low = EvidenceContractR31.frequencyReport(c, new EvidenceContractR31.Opportunities(1000, 60, 940, 12, 3, 1), false, "QUANTILE_TOP_6_PERCENT");
        assertEquals("BELOW_TARGET", low.get("frequencyBand")); assertEquals(.06, (Double) low.get("actRate"), 1e-12); assertEquals(false, low.get("achieved"));
        assertEquals("QUANTILE_TOP_6_PERCENT", low.get("selectorSupportDomain")); assertEquals(1016L, low.get("scheduled")); assertEquals(false, low.get("actsFabricated"));
        Map<String, Object> inRangeButBad = EvidenceContractR31.frequencyReport(c, new EvidenceContractR31.Opportunities(1000, 150, 850, 0, 0, 0), false, "x");
        assertEquals("IN_TARGET_RANGE", inRangeButBad.get("frequencyBand")); assertEquals("frequency alone is not success", false, inRangeButBad.get("achieved"));
        assertEquals(true, EvidenceContractR31.frequencyReport(c, new EvidenceContractR31.Opportunities(1000, 150, 850, 0, 0, 0), true, "x").get("achieved"));
        try { new EvidenceContractR31.Opportunities(10, 8, 5, 0, 0, 0); fail("act+abstain beyond eligible"); } catch (IllegalArgumentException expected) { assertEquals("R31_OPPORTUNITY_COUNTS", expected.getMessage()); }
    }
}
