package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

/** R31-B07-C06#A1-#A4: typed failure attribution, short-window detection with expiry, strict single-position K3. */
public final class R31ResearchHypothesisRegimeTest {
    static final String PARENT = StatsR27.sha256("parent-policy");
    static ResearchHypothesisR31.Failure failure(ResearchHypothesisR31.Mechanism m, String component, String role, long through, long support) {
        return new ResearchHypothesisR31.Failure(PARENT, "run-" + m, m, m.name(), component, role, 1000, through, support, 0.12);
    }

    @Test public void sixControlledFailuresTriggerTheirOwnSourcedActions() {
        ResearchHypothesisR31.Registry registry = new ResearchHypothesisR31.Registry();
        Set<String> actions = new HashSet<>(), families = new HashSet<>();
        for (ResearchHypothesisR31.Mechanism m : ResearchHypothesisR31.Mechanism.values()) {
            ResearchHypothesisR31.Hypothesis h = registry.register(ResearchHypothesisR31.fromFailure(failure(m, "freq(1,128)", "SELECT", 999, 500)));
            assertEquals("run-" + m, h.sourceRun); assertEquals(PARENT, h.parentHash); assertEquals(m, h.mechanism);
            assertEquals(false, h.json().get("formalEvidenceInherited"));
            if (m == ResearchHypothesisR31.Mechanism.RESOURCE_FAILURE) {
                assertFalse(h.supported()); assertEquals("UNSUPPORTED:RESOURCE_FAILURE_IS_NOT_A_STRATEGY_HYPOTHESIS", registry.dispatch(h)); continue;
            }
            assertTrue(m.name(), h.supported());
            assertFalse(h.prediction.isEmpty()); assertFalse(h.control.isEmpty()); assertFalse(h.domain.isEmpty()); assertTrue(h.budgetUnits > 0);
            actions.add(registry.dispatch(h)); families.add(h.family);
        }
        assertEquals("five mechanisms, five different actions (no shared random mutation)", 5, actions.size());
        assertTrue("several families take part", families.size() >= 4);
        assertEquals("UNSUPPORTED", ResearchHypothesisR31.fromFailure(failure(ResearchHypothesisR31.Mechanism.COMPONENT_NEGATIVE, "", "SELECT", 999, 500)).status);
        assertEquals("INSUFFICIENT_SUPPORT:20<200", ResearchHypothesisR31.fromFailure(failure(ResearchHypothesisR31.Mechanism.CALIBRATION, "", "SELECT", 999, 20)).unsupportedReason);
        Map<String, ResearchHypothesisR31.Mechanism> reasons = new LinkedHashMap<>();
        reasons.put("OBSERVED_HIT_RATE_LT_035", ResearchHypothesisR31.Mechanism.CROSS_POSITION_SUPPORT); reasons.put("COVERAGE_LT_0.15", ResearchHypothesisR31.Mechanism.COVERAGE);
        reasons.put("BLOCK_BELOW_FLOOR", ResearchHypothesisR31.Mechanism.SHORT_WINDOW_DRIFT); reasons.put("PAIRED_LCB_BELOW_PREREGISTERED_MINUS_001", ResearchHypothesisR31.Mechanism.COMPONENT_NEGATIVE);
        reasons.put("HELD_LATER_ECE_GT_004", ResearchHypothesisR31.Mechanism.CALIBRATION);
        for (Map.Entry<String, ResearchHypothesisR31.Mechanism> e : reasons.entrySet()) assertEquals(e.getKey(), e.getValue(), ResearchHypothesisR31.mechanismOf(e.getKey()));
    }

    static boolean[] stream(StatsR27.SplitMix r, double[][] segments) { List<Boolean> out = new ArrayList<>(); for (double[] s : segments) for (int i = 0; i < s[0]; i++) out.add(r.nextDouble() < s[1]); boolean[] b = new boolean[out.size()]; for (int i = 0; i < b.length; i++) b[i] = out.get(i); return b; }
    @Test public void shortSignalAndDriftAreDetectedWithinTheRegisteredRangeAndNullsStayWithinBudget() {
        RegimeHypothesisR31.Protocol protocol = RegimeHypothesisR31.Protocol.registered();
        int signalStreams = 40, detected = 0, expired = 0; List<Long> delays = new ArrayList<>();
        for (int s = 0; s < signalStreams; s++) {
            boolean[] x = stream(new StatsR27.SplitMix(5000 + s), new double[][]{{300, .30}, {300, .55}, {400, .30}});
            RegimeHypothesisR31.Detector d = new RegimeHypothesisR31.Detector(protocol);
            for (int t = 0; t < x.length; t++) d.observe(t, x[t]);
            Long onset = null; for (long o : d.onsets()) if (o >= 300 && o < 600) { onset = o; break; }
            if (onset != null) { detected++; delays.add(onset - 300); }
            for (long e : d.expiries()) if (e >= 600 && e < 900) { expired++; break; }
        }
        Collections.sort(delays);
        assertTrue("power lower95 >= 0.85: " + detected + "/" + signalStreams, ResearchCapabilityExperimentR31.clopperPearsonLower(detected, signalStreams, .05) >= 0.85);
        assertTrue("median detection delay <= 100: " + delays, delays.get(delays.size() / 2) <= 100);
        assertTrue("drift expiry after the signal ends: " + expired, expired >= 0.85 * signalStreams);
        int nullStreams = 60, falseAlarms = 0;
        for (int s = 0; s < nullStreams; s++) {
            boolean[] x = stream(new StatsR27.SplitMix(9000 + s), new double[][]{{1000, .30}});
            RegimeHypothesisR31.Detector d = new RegimeHypothesisR31.Detector(protocol); for (int t = 0; t < x.length; t++) d.observe(t, x[t]);
            if (!d.onsets().isEmpty()) falseAlarms++;
        }
        assertTrue("null false alarms upper95 <= 0.10: " + falseAlarms + "/" + nullStreams, ResearchCapabilityExperimentR31.clopperPearsonUpper(falseAlarms, nullStreams, .05) <= 0.10);
        // nested time validation: the choice for every outer fold uses only earlier folds
        List<int[]> calls = new ArrayList<>();
        RegimeHypothesisR31.WindowSelection sel = RegimeHypothesisR31.proposeWindow(new int[]{16, 64, 256}, (w, from, to) -> { calls.add(new int[]{from, to}); return w == 64 ? 1 : 0; }, 0, 1000, 5);
        assertEquals(64, sel.window); assertEquals(4, sel.foldChoices.size());
        int callIndex = 0;
        for (int k = 1; k < 5; k++) for (int w = 0; w < 3; w++) for (int j = 0; j < k; j++) assertTrue("fold " + k + " reads only data before " + sel.foldStarts.get(k - 1), calls.get(callIndex++)[1] <= sel.foldStarts.get(k - 1));
    }

    static List<Set<Integer>> supportsFor(int size) {
        Set<Integer> a = new TreeSet<>(Arrays.asList(1, 2, 3, 4, 5, 6)), b = new TreeSet<>();
        for (int n = 1; n <= size; n++) b.add(n); b.add(9); b.add(10);
        return Arrays.asList(a, b);
    }
    @Test public void strictIntersectionAbstainsBelowThreeAndUsesTheFixedTieBreak() {
        for (int size = 0; size <= 4; size++) {
            RegimeHypothesisR31.K3 k = RegimeHypothesisR31.toSinglePositionK3(4, 1000, supportsFor(size), new long[]{999, 998}, null);
            assertEquals(size, k.candidateSize); assertEquals(4, k.position);
            double sum = 0; for (double p : k.probabilities) { assertTrue(Double.isFinite(p) && p >= 0); sum += p; } assertEquals(1, sum, 1e-12);
            if (size < 3) { assertTrue("abstains at |C|=" + size, k.abstain); assertNull("never filled with random numbers", k.numbers); }
            else { assertFalse(k.abstain); assertEquals(3, k.numbers.length); assertTrue(k.numbers[0] < k.numbers[1] && k.numbers[1] < k.numbers[2]); }
        }
        assertArrayEquals("|C|=3 is the set", new int[]{1, 2, 3}, RegimeHypothesisR31.toSinglePositionK3(4, 1000, supportsFor(3), new long[]{999, 998}, null).numbers);
        assertArrayEquals("equal scores: smaller numbers first", new int[]{1, 2, 3}, RegimeHypothesisR31.toSinglePositionK3(4, 1000, supportsFor(4), new long[]{999, 998}, null).numbers);
        double[] scores = new double[10]; scores[3] = .9; scores[1] = .5; scores[0] = .5; scores[2] = .1;
        assertArrayEquals("score descending, then number ascending", new int[]{1, 2, 4}, RegimeHypothesisR31.toSinglePositionK3(4, 1000, supportsFor(4), new long[]{999, 998}, scores).numbers);
    }

    @Test public void futureLabelsAndFormalInputsAreRefusedAndRestoreMatchesUninterrupted() {
        try { RegimeHypothesisR31.toSinglePositionK3(4, 1000, supportsFor(4), new long[]{999, 1000}, null); fail("same-period support accepted"); }
        catch (IllegalArgumentException expected) { assertEquals("R31_REGIME_FUTURE_INPUT:1000>=1000", expected.getMessage()); }
        RegimeHypothesisR31.Detector d = new RegimeHypothesisR31.Detector(RegimeHypothesisR31.Protocol.registered());
        d.observe(10, true);
        try { d.activeFor(10); fail("state built with the decision period's own label"); } catch (IllegalStateException expected) { assertEquals("R31_REGIME_FUTURE_INPUT:10>=10", expected.getMessage()); }
        try { d.observe(9, true); fail("older outcome accepted"); } catch (IllegalStateException expected) { assertEquals("R31_REGIME_ORDINAL_ORDER:9", expected.getMessage()); }
        for (String role : new String[]{"CONFIRM", "FUTURE"})
            try { ResearchHypothesisR31.fromFailure(failure(ResearchHypothesisR31.Mechanism.CALIBRATION, "", role, 999, 500)); fail(role); }
            catch (IllegalArgumentException expected) { assertEquals("R31_HYPOTHESIS_FORMAL_INPUT:" + role, expected.getMessage()); }
        try { ResearchHypothesisR31.fromFailure(failure(ResearchHypothesisR31.Mechanism.CALIBRATION, "", "SELECT", 1000, 500)); fail("input at the decision"); }
        catch (IllegalArgumentException expected) { assertEquals("R31_HYPOTHESIS_FUTURE_INPUT:1000>=1000", expected.getMessage()); }
        ResearchHypothesisR31.Registry registry = new ResearchHypothesisR31.Registry();
        ResearchHypothesisR31.Hypothesis h = registry.register(ResearchHypothesisR31.fromFailure(failure(ResearchHypothesisR31.Mechanism.COVERAGE, "", "SELECT", 999, 500)));
        try { registry.recordOutcome(h, "child-A", "CONFIRM", .1); fail("formal outcome fed back"); } catch (IllegalArgumentException expected) { assertEquals("R31_HYPOTHESIS_FORMAL_OUTCOME:CONFIRM", expected.getMessage()); }
        assertTrue(registry.recordOutcome(h, "child-A", "SELECT", .02)); assertFalse("the measured child is booked once", registry.recordOutcome(h, "child-A", "SELECT", .5));
        assertEquals(.02, registry.developmentCredit(h.family), 0);
        // restore in the middle of a stream continues exactly like the uninterrupted detector
        boolean[] x = stream(new StatsR27.SplitMix(77), new double[][]{{200, .30}, {200, .60}, {200, .30}});
        RegimeHypothesisR31.Detector straight = new RegimeHypothesisR31.Detector(RegimeHypothesisR31.Protocol.registered()), resumed = new RegimeHypothesisR31.Detector(RegimeHypothesisR31.Protocol.registered());
        List<String> a = new ArrayList<>(), b = new ArrayList<>();
        for (int t = 0; t < x.length; t++) {
            a.add(RegimeHypothesisR31.routePastState(straight, t, "F10_REGIME", "BASE")); straight.observe(t, x[t]);
            b.add(RegimeHypothesisR31.routePastState(resumed, t, "F10_REGIME", "BASE")); resumed.observe(t, x[t]);
            if (t % 37 == 0) resumed = RegimeHypothesisR31.Detector.decode(RegimeHypothesisR31.Protocol.registered(), resumed.encode());
        }
        assertEquals(a, b); assertEquals(straight.encode(), resumed.encode()); assertTrue(a.contains("F10_REGIME"));
        try { RegimeHypothesisR31.Detector.decode(new RegimeHypothesisR31.Protocol(.3, .6, 10), straight.encode()); fail("state restored under another protocol"); }
        catch (IllegalArgumentException expected) { assertEquals("R31_REGIME_STATE_PROTOCOL", expected.getMessage()); }
    }
}
