package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import org.junit.Test;

/** R30-B04-02 宿主测试。复制到 app/src/test/java/com/ethk3/novaonepro/strategyv2/。 */
public class R30BehaviorSignatureTest {
    static PolicySpecR27 spec(PolicyAstR27.Node ast) {
        return new PolicySpecR27(ast, 3, PolicySpecR27.Rule.ALWAYS, 0, 0, 1, false, 0.01, 0.001,
                IdeaFamilyR27.meta("I01", "TEST", 0, 1, "behavior equivalence fixture"));
    }
    static EvalContextR27 context() {
        ReplayDatasetR27 d = ReplayDatasetR27.synthetic(3000, 42, new int[0]);
        return new EvalContextR27(new FeatureCacheR27(d), SplitR27.of(d, null), null);
    }
    static EvolutionLoopR27.Member member(PolicySpecR27 spec, String island) {
        EvolutionLoopR27.Member m = new EvolutionLoopR27.Member(); m.spec = spec; m.tau = 1; m.island = island; return m;
    }
    @Test public void behaviorSignatureIgnoresNonActDetails() {
        boolean[] act = {true, false, true}; int[] pos = {1, 7, 2}, pos2 = {1, 3, 2};
        int[] m = {BehaviorSignatureR30.mask(1, 2, 3), 0, BehaviorSignatureR30.mask(4, 5, 10)};
        int[] m2 = {m[0], BehaviorSignatureR30.mask(6, 7, 8), m[2]};
        assertEquals(BehaviorSignatureR30.signature("ds1", act, pos, m), BehaviorSignatureR30.signature("ds1", act, pos2, m2));
        assertNotEquals(BehaviorSignatureR30.signature("ds1", act, pos, m), BehaviorSignatureR30.signature("ds2", act, pos, m));
        int[] m3 = {m[0], 0, BehaviorSignatureR30.mask(4, 5, 9)};
        assertNotEquals(BehaviorSignatureR30.signature("ds1", act, pos, m), BehaviorSignatureR30.signature("ds1", act, pos, m3));
    }

    @Test public void probesAndFingerprintReadOnlyTrainOutputs() {
        EvalContextR27 ctx = context();
        BehaviorSignatureR30.Probes probes = BehaviorSignatureR30.probes(ctx.data, ctx.split);
        assertTrue(probes.ready()); assertEquals(256, probes.rows.length);
        for (int t : probes.rows) assertTrue(t >= ctx.split.trainFrom && t < ctx.split.trainTo);
        PolicySpecR27 s = spec(PolicyAstR27.leaf("freq", 3, 32));
        ReplayEvaluatorR27.Trace tr = ctx.selectTrace(s, 1);
        String before = BehaviorSignatureR30.fromTrace(probes, tr);
        for (int t = ctx.split.trainTo; t < tr.act.length; t++) { tr.act[t] = !tr.act[t]; tr.skipped[t] = !tr.skipped[t]; tr.k3[t*3] = 99; }
        assertEquals(before, BehaviorSignatureR30.fromTrace(probes, tr));
        assertEquals(probes.hash, BehaviorSignatureR30.probes(ctx.data, ctx.split).hash);
        ReplayDatasetR27 shortData = ReplayDatasetR27.synthetic(100, 1, new int[0]);
        assertFalse(BehaviorSignatureR30.probes(shortData, SplitR27.of(shortData, null)).ready());
    }

    @Test public void syntaxTwinsCannotTakeSeparateArchiveCells() {
        EvalContextR27 ctx = context();
        BehaviorSignatureR30.Probes probes = BehaviorSignatureR30.probes(ctx.data, ctx.split);
        PolicySpecR27 a = spec(PolicyAstR27.leaf("freq", 3, 32));
        PolicySpecR27 b = spec(PolicyAstR27.op("add", PolicyAstR27.leaf("freq", 3, 32), PolicyAstR27.constant(0)));
        assertNotEquals(a.policyHash(), b.policyHash());
        String sa = BehaviorSignatureR30.fromTrace(probes, ctx.selectTrace(a, 1));
        String sb = BehaviorSignatureR30.fromTrace(probes, ctx.selectTrace(b, 1));
        assertEquals(sa, sb);
        EliteArchiveR27 archive = new EliteArchiveR27(); java.util.List<String[]> rejected = new java.util.ArrayList<>();
        BehaviorSignatureR30.Rejected receipt = (signature, kept, dropped) -> rejected.add(new String[] {signature, kept, dropped});
        archive.bindBehavior(java.util.Collections.emptyMap());
        assertTrue(archive.offer(0, a, 0.1, 1, sa, receipt));
        assertFalse(archive.offer(1, b, 100, 1, sb, receipt));
        assertEquals(1, archive.filled()); assertTrue(archive.occupies(a.policyHash())); assertFalse(archive.occupies(b.policyHash()));
        assertEquals(a.policyHash(), rejected.get(0)[1]); assertEquals(b.policyHash(), rejected.get(0)[2]);
    }

    @Test public void receivingIslandRejectsAnEquivalentMigrant() {
        PolicySpecR27 a = spec(PolicyAstR27.leaf("freq", 3, 32));
        PolicySpecR27 b = spec(PolicyAstR27.op("add", PolicyAstR27.leaf("freq", 3, 32), PolicyAstR27.constant(0)));
        java.util.List<EvolutionLoopR27.Member> members = java.util.Arrays.asList(member(a, "I02"), member(b, "I01"));
        java.util.Map<String,String> fingerprints = new java.util.HashMap<>(); fingerprints.put(a.policyHash(), "same"); fingerprints.put(b.policyHash(), "same");
        java.util.List<String[]> rejected = new java.util.ArrayList<>();
        java.util.List<String[]> moves = IslandModelR27.migrations(members, fingerprints, (s,k,d) -> rejected.add(new String[] {s,k,d}));
        assertTrue(moves.stream().noneMatch(m -> m[0].equals(b.policyHash()) && m[1].equals("I02")));
        assertEquals(1, rejected.size()); assertEquals(a.policyHash(), rejected.get(0)[1]);
    }

    @Test public void productionEvolutionEmitsArchiveAndMigrationReceipts() {
        EvalContextR27 ctx = context(); R27BirthGateTest.Recorder sink = new R27BirthGateTest.Recorder();
        EvolutionLoopR27.Config cfg = new EvolutionLoopR27.Config(); cfg.births = 0; cfg.nullControl = false; cfg.i00PerGeneration = 0;
        EvolutionLoopR27 loop = new EvolutionLoopR27(cfg, ctx.cache, sink, () -> 0L); loop.generation = 5; loop.stepDataset();
        loop.population.add(member(spec(PolicyAstR27.leaf("freq", 3, 32)), "I02"));
        loop.population.add(member(spec(PolicyAstR27.op("add", PolicyAstR27.leaf("freq", 3, 32), PolicyAstR27.constant(0))), "I01"));
        loop.stepHalving(); loop.stepMigrateAblate();
        assertTrue(sink.events.stream().anyMatch(e -> e.startsWith("D|") && e.endsWith("|ARCHIVE")));
        assertTrue(sink.events.stream().anyMatch(e -> e.startsWith("D|") && e.endsWith("|MIGRATION")));
    }
}
