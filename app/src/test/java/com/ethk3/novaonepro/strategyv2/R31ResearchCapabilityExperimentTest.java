package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

/** R31-B07-C05#A1-#A4: registered whole-pipeline null/power trials through the production stages. */
public final class R31ResearchCapabilityExperimentTest {
    static long[] seeds(long base, int n) { long[] s = new long[n]; for (int i = 0; i < n; i++) s[i] = base + i; return s; }
    static ResearchCapabilityExperimentR31.Protocol protocol() {
        Map<ResearchCapabilityExperimentR31.Arm, long[]> arms = new EnumMap<>(ResearchCapabilityExperimentR31.Arm.class);
        arms.put(ResearchCapabilityExperimentR31.Arm.NULL_UNIFORM, seeds(1000, 24));
        arms.put(ResearchCapabilityExperimentR31.Arm.NULL_TIME_SHUFFLED, seeds(2000, 24));
        arms.put(ResearchCapabilityExperimentR31.Arm.SIGNAL, seeds(3000, 24));
        arms.put(ResearchCapabilityExperimentR31.Arm.SIGNAL_DISABLED, seeds(3000, 24)); // paired with SIGNAL seeds
        // registered before any trial: rows, segments, target position, strength, FAR upper-95 budget, power lower-95 floor
        return ResearchCapabilityExperimentR31.register("R31_K06_CAPABILITY", arms, 900, 400, 600, 3, 0.30, 0.15, 0.70);
    }
    static List<ResearchCapabilityExperimentR31.TrialRecord> all;
    static synchronized List<ResearchCapabilityExperimentR31.TrialRecord> records() { if (all == null) all = ResearchCapabilityExperimentR31.runAll(protocol(), ResearchCapabilityExperimentR31.PRODUCTION_STAGES); return all; }

    @Test public void everyTrialRecordsConfigSeedStatusCandidatesEvidenceAndGate() {
        ResearchCapabilityExperimentR31.Protocol p = protocol();
        List<ResearchCapabilityExperimentR31.TrialRecord> r = records();
        assertEquals(p.planned(), r.size());
        for (ResearchCapabilityExperimentR31.TrialRecord t : r) {
            assertEquals(p.hash(), t.protocolHash);
            assertEquals(t.arm + ":" + t.seed + ":" + t.faultReason, false, t.status == ResearchCapabilityExperimentR31.Status.FAILED);
            assertEquals("every candidate is counted, not only the winner", ResearchCapabilityExperimentR31.candidates(3).size(), t.candidates);
            assertTrue(t.evidenceHash.matches("[0-9a-f]{64}")); assertTrue(t.selectedHash.matches("[0-9a-f]{64}"));
            assertFalse(t.gateState.isEmpty());
            for (String k : new String[]{"protocolHash", "arm", "seed", "status", "candidates", "contractRejected", "evidenceHash", "gate"}) assertTrue(k, t.json().containsKey(k));
        }
        try { ResearchCapabilityExperimentR31.runPipeline(p, ResearchCapabilityExperimentR31.Arm.SIGNAL, 99, ResearchCapabilityExperimentR31.PRODUCTION_STAGES); fail("unregistered trial ran"); }
        catch (IllegalArgumentException expected) { assertEquals("R31_CAPABILITY_TRIAL_NOT_REGISTERED:SIGNAL:99", expected.getMessage()); }
    }

    @Test public void aMissingTrialIsNotCompleteAndFaultsAreCountedApartFromNonDetections() {
        ResearchCapabilityExperimentR31.Protocol p = protocol();
        List<ResearchCapabilityExperimentR31.TrialRecord> r = new ArrayList<>(records()); r.remove(5);
        ResearchCapabilityExperimentR31.Summary s = ResearchCapabilityExperimentR31.summarize(p, r);
        assertEquals("INCOMPLETE", s.status); assertEquals(1, s.missing.size());
        assertFalse(s.farWithinBudget || s.powerWithinRegistered);
        long faultSeed = p.seeds.get(ResearchCapabilityExperimentR31.Arm.NULL_UNIFORM)[0];
        ResearchCapabilityExperimentR31.Pipeline faulty = (pr, arm, seed, draws) -> { if (arm == ResearchCapabilityExperimentR31.Arm.NULL_UNIFORM && seed == faultSeed) throw new IllegalStateException("INJECTED_FAULT"); return null; };
        ResearchCapabilityExperimentR31.TrialRecord failed = ResearchCapabilityExperimentR31.runPipeline(p, ResearchCapabilityExperimentR31.Arm.NULL_UNIFORM, faultSeed, faulty);
        assertEquals(ResearchCapabilityExperimentR31.Status.FAILED, failed.status); assertEquals("IllegalStateException:INJECTED_FAULT", failed.faultReason);
        List<ResearchCapabilityExperimentR31.TrialRecord> withFault = new ArrayList<>();
        for (ResearchCapabilityExperimentR31.TrialRecord t : records()) withFault.add(t.arm == failed.arm && t.seed == failed.seed ? failed : t);
        ResearchCapabilityExperimentR31.Summary f = ResearchCapabilityExperimentR31.summarize(p, withFault);
        assertEquals("FAULTED", f.status);
        long[] c = f.counts.get(ResearchCapabilityExperimentR31.Arm.NULL_UNIFORM);
        assertEquals(1, c[4]); assertEquals(c[0], c[1]); assertEquals(c[1], c[2] + c[3] + c[4]);
        assertFalse("a faulted experiment gives no verdict", f.farWithinBudget || f.powerWithinRegistered);
    }

    @Test public void signalOnOffPairsMeetTheRegisteredPowerAndNullFalseAlarmBudget() {
        ResearchCapabilityExperimentR31.Protocol p = protocol();
        ResearchCapabilityExperimentR31.Summary s = ResearchCapabilityExperimentR31.summarize(p, records());
        assertEquals("COMPLETE", s.status);
        assertEquals(72, s.nullTrials); assertEquals(24, s.signalTrials);
        assertTrue("false alarms " + s.falseAlarms + "/" + s.nullTrials + " upper95=" + s.farUpper95, s.farWithinBudget);
        assertTrue("power " + s.detections + "/" + s.signalTrials + " lower95=" + s.powerLower95, s.powerWithinRegistered);
        assertTrue("planting the signal is what is detected", s.counts.get(ResearchCapabilityExperimentR31.Arm.SIGNAL)[2] > s.counts.get(ResearchCapabilityExperimentR31.Arm.SIGNAL_DISABLED)[2] + 12);
        assertTrue("zero events still have a nonzero upper bound", ResearchCapabilityExperimentR31.clopperPearsonUpper(0, 72, .05) > 0.04);
        assertEquals(1 - Math.pow(.05, 1.0 / 72), ResearchCapabilityExperimentR31.clopperPearsonUpper(0, 72, .05), 1e-9);
    }

    @Test public void syntheticCandidatesHaveNoRealPermission() {
        for (ResearchCapabilityExperimentR31.TrialRecord t : records()) {
            assertFalse(t.realPermission); assertEquals(ResearchCapabilityExperimentR31.NAMESPACE, t.namespace);
            try { ResearchCapabilityExperimentR31.requireNaturalFutureNamespace(t, ResearchCapabilityExperimentR31.NATURAL_FUTURE_NAMESPACE); fail("synthetic record entered natural future evidence"); }
            catch (IllegalStateException expected) { assertEquals("R31_SYNTHETIC_HAS_NO_REAL_PERMISSION", expected.getMessage()); }
        }
        for (PolicySpecR27 s : ResearchCapabilityExperimentR31.candidates(3)) assertTrue(s.meta.reason.contains("no REAL permission"));
    }
}
