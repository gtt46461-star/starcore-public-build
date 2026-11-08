package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.util.*;
import org.junit.Test;

/** R31-B08-C03#A1-#A4: component registry, separate MASK_FROZEN / RETRAIN_REMOVED protocols, descriptive intervals. */
public final class R31PairedStudyProtocolTest {
    static final class Owned implements AutoCloseable {
        final FeatureCacheR27 cache; final EvalContextR27 ctx;
        Owned() { ReplayDatasetR27 d = ReplayDatasetR27.synthetic(3000, 30, new int[0]); cache = new FeatureCacheR27(d); ctx = new EvalContextR27(cache, SplitR27.of(d, null), null); }
        @Override public void close() { try { ctx.close(); } finally { cache.close(); } }
    }
    static PolicySpecR27 parent() { return RepairPairedResearchR30Test.parent(); }
    static PairedReplayRunnerR30R1 runner(Owned o, PairedReplayRunnerR30R1.Protocol protocol) { PolicySpecR27 p = parent(); return new PairedReplayRunnerR30R1(RepairPairedResearchR30Test.contract(o.ctx, p), p, "root/0", o.ctx, 1, 1, 64, protocol); }

    @Test public void familyLeafCandidatesAreStudiedOrExplicitlyUnsupportedAndEveryParentIsReached() {
        PolicySpecR27 plain = parent();
        PolicySpecR27 family = new PolicySpecR27(PolicyAstR27.op("add", PolicyAstR27.op("zscore", PolicyAstR27.leaf("family", 8)), PolicyAstR27.op("zscore", PolicyAstR27.leaf("freq", 1, 32))), 1, PolicySpecR27.Rule.ALWAYS, 0, 1, 1, true, .03, .01, IdeaFamilyR27.meta("I08", "TEST", 1, 3, "family fixture"));
        List<ResearchStudyTaskR30R1.ComponentR31> a = ResearchStudyTaskR30R1.componentsR31(plain, 1), b = ResearchStudyTaskR30R1.componentsR31(family, 1);
        assertTrue(a.get(0).supported()); assertEquals("root/0", a.get(0).path);
        for (ResearchStudyTaskR30R1.ComponentR31 c : b) { assertFalse(c.path, c.supported()); assertEquals("UNSUPPORTED_PAIR", c.status); assertFalse("every unsupported component names why", c.reason.isEmpty()); }
        Set<String> kinds = new HashSet<>(); for (ResearchStudyTaskR30R1.ComponentR31 c : b) kinds.add(c.kind);
        assertEquals(new HashSet<>(Arrays.asList("AST_SUBTREE", "FAMILY_PARAMETER_MODULE", "SELECTOR", "CALIBRATOR")), kinds);
        List<EvolutionLoopR27.Member> population = new ArrayList<>();
        for (int i = 0; i < 21; i++) { EvolutionLoopR27.Member m = new EvolutionLoopR27.Member(); m.spec = plain.withUpdater(.001 * (i + 1), .01, null); population.add(m); }
        Set<String> reached = new HashSet<>();
        for (int g = 0; g < 3; g++) { List<PolicySpecR27> window = ResearchStudyTaskR30R1.rotationR31(population, g, 8); assertEquals(8, window.size()); for (PolicySpecR27 p : window) reached.add(p.policyHash()); }
        assertEquals("rotation reaches every parent, not only the first eight", 21, reached.size());
    }

    @Test public void bothProtocolsResumeExactlyAndTheRemovedArmNeverTouchesTheParentState() {
        try (Owned o = new Owned()) {
            Map<PairedReplayRunnerR30R1.Protocol, PairedReplayRunnerR30R1> done = new EnumMap<>(PairedReplayRunnerR30R1.Protocol.class);
            for (PairedReplayRunnerR30R1.Protocol protocol : PairedReplayRunnerR30R1.Protocol.values()) {
                PairedReplayRunnerR30R1 straight = runner(o, protocol), resumed = runner(o, protocol);
                while (!straight.complete()) straight.advance(o.ctx, 256);
                while (!resumed.complete()) { resumed.advance(o.ctx, 97); resumed = PairedReplayRunnerR30R1.restore(resumed.snapshot(), o.ctx); }
                assertEquals(protocol, resumed.protocol);
                assertEquals(protocol + ": interrupted and uninterrupted runs agree", straight.snapshot(), resumed.snapshot());
                done.put(protocol, straight);
            }
            PairedReplayRunnerR30R1 retrain = done.get(PairedReplayRunnerR30R1.Protocol.RETRAIN_REMOVED), mask = done.get(PairedReplayRunnerR30R1.Protocol.MASK_FROZEN);
            assertArrayEquals("removing a component in either protocol leaves the parent arm's learning state identical", retrain.biasR31(true), mask.biasR31(true), 0);
            assertArrayEquals("MASK_FROZEN never trains a separate removed-arm state", new double[10], mask.biasR31(false), 0);
            assertFalse("RETRAIN_REMOVED trains its own removed-arm state", Arrays.equals(new double[10], retrain.biasR31(false)));
            assertFalse("old RETRAIN checkpoints keep their bytes (no protocol field)", retrain.snapshot().contains("\"protocol\""));
            assertTrue(mask.snapshot().contains("\"protocol\":\"MASK_FROZEN\""));
            assertNotEquals("the two protocols measure different things", JsonR27.write(retrain.gain().blocks()), JsonR27.write(mask.gain().blocks()));
        }
    }

    @Test public void oneProtocolCannotClaimTheOthersEffectAndIntervalsStayDescriptive() {
        try (Owned o = new Owned()) {
            PairedReplayRunnerR30R1 mask = runner(o, PairedReplayRunnerR30R1.Protocol.MASK_FROZEN);
            assertEquals("MASKED_CONTRIBUTION_UNDER_SHARED_PARENT_STATE", mask.effectClaimR31(PairedReplayRunnerR30R1.Protocol.MASK_FROZEN));
            try { mask.effectClaimR31(PairedReplayRunnerR30R1.Protocol.RETRAIN_REMOVED); fail("masked result claimed a retrained causal effect"); }
            catch (IllegalArgumentException expected) { assertEquals("R31_PAIRED_PROTOCOL_MISMATCH:MASK_FROZEN!=RETRAIN_REMOVED", expected.getMessage()); }
            while (!mask.complete()) mask.advance(o.ctx, 256);
            ResearchGainR30R1.Estimate e = mask.gain().estimate();
            assertEquals("DESCRIPTIVE_RESEARCH_ONLY", ResearchGainR30R1.labelR31(e, "DESCRIPTIVE"));
            for (String formal : new String[]{"CERTIFIED", "QUALIFIED", "VERIFIED"})
                try { ResearchGainR30R1.labelR31(e, formal); fail(formal + " accepted for a development interval"); }
                catch (IllegalStateException expected) { assertEquals("R31_DESCRIPTIVE_RESEARCH_NOT_CERTIFIABLE:" + formal, expected.getMessage()); }
        }
    }

    static final class MapStore implements ResearchStudyCheckpointR31.Store {
        final Map<String, String> m = new TreeMap<>();
        public String get(String k) { return m.get(k); } public void put(String k, String v) { m.put(k, v); } public Set<String> keys() { return new TreeSet<>(m.keySet()); } public void remove(String k) { m.remove(k); }
    }
    @Test public void checkpointsMigrateBeforeTheLegacyCacheMayBeCleared() {
        MapStore legacy = new MapStore(), typed = new MapStore(); ResearchStudyCheckpointR31 c = new ResearchStudyCheckpointR31(legacy, typed);
        c.put("study-a", "{\"cursor\":12}"); c.put("study-b", "{\"cursor\":40}");
        try { c.clearLegacy(); fail("cache cleared before migration"); } catch (IllegalStateException expected) { assertEquals("R31_STUDY_CHECKPOINT_CACHE_BEFORE_MIGRATION", expected.getMessage()); }
        assertEquals(Collections.emptyList(), c.migrate());
        assertEquals(ResearchStudyCheckpointR31.Authority.TYPED_STORE, c.authority());
        c.put("study-a", "{\"cursor\":13}"); c.clearLegacy(); assertTrue(legacy.m.isEmpty());
        assertEquals("the typed store is the authority after the cache is gone", "{\"cursor\":13}", c.get("study-a"));
        c.rebuildLegacy(); assertEquals("{\"cursor\":40}", legacy.get("study-b"));
        typed.put("study-b", typed.get("study-b").replace("40", "41"));
        try { c.get("study-b"); fail("tampered typed state accepted"); } catch (IllegalStateException expected) { assertEquals("R31_STUDY_CHECKPOINT_INTEGRITY:study-b", expected.getMessage()); }
    }
}
