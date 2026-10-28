package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;

/** H04 (every birth-rejection reason, all inside the enumeration) and H05 (NO_OP is detected and not registered),
 * plus R27-021 (lossless canonical identity) and R27-023 (every idea family produces executable births). */
public class R27BirthGateTest {
    static PolicySpecR27 spec(PolicyAstR27.Node ast) { return new PolicySpecR27(ast, 1, PolicySpecR27.Rule.QUANTILE, 0, 0.2, 1.0, false, 0.01, 0.01, null); }
    static PolicyAstR27.Node z(PolicyAstR27.Node n) { return PolicyAstR27.op("zscore", n); }

    @Test public void everyRejectionReasonIsProducedAndEnumerated() throws Exception {
        ReplayDatasetR27 d = R27TestSupport.csv(1200); FeatureCacheR27 f = new FeatureCacheR27(d); BirthGateR27 g = new BirthGateR27(f, 1000);
        Map<BirthGateR27.Reason, String> seen = new EnumMap<>(BirthGateR27.Reason.class);
        PolicyAstR27.Node deep = z(PolicyAstR27.leaf("freq", 1, 32)); for (int i = 0; i < 7; i++) deep = PolicyAstR27.op("neg", deep);
        check(g, spec(deep), BirthGateR27.Reason.TOO_COMPLEX, seen);
        check(g, spec(PolicyAstR27.op("add", PolicyAstR27.leaf("freq", 1, 32), PolicyAstR27.leaf("gap", 1))), BirthGateR27.Reason.TYPE, seen);
        check(g, spec(z(PolicyAstR27.leaf("freq", 1, 7))), BirthGateR27.Reason.RANGE, seen);
        check(g, spec(z(PolicyAstR27.leaf("cross", 2, 0))), BirthGateR27.Reason.CAUSAL, seen);
        // OUTPUT: a leaf that is never available.
        FeatureCacheR27 nanCache = new FeatureCacheR27(d); nanCache.override("bigsmall", (fc, tp, a, b) -> { double[] v = new double[fc.data.size() * 10]; Arrays.fill(v, Double.NaN); return v; });
        check(new BirthGateR27(nanCache, 1000), spec(z(PolicyAstR27.leaf("bigsmall", 1, 32))), BirthGateR27.Reason.OUTPUT, seen);
        // COST: a deliberately slow leaf against the per-period cost limit.
        ReplayDatasetR27 tiny = ReplayDatasetR27.synthetic(40, 1, null); FeatureCacheR27 slow = new FeatureCacheR27(tiny);
        slow.override("oddeven", (fc, tp, a, b) -> { try { Thread.sleep(400); } catch (InterruptedException e) { } return new double[fc.data.size() * 10]; });
        check(new BirthGateR27(slow, 40), spec(z(PolicyAstR27.leaf("oddeven", 1, 32))), BirthGateR27.Reason.COST, seen);
        // DUPLICATE: identical behavior under a different identity (adding a constant 0 does not change z-scores).
        PolicySpecR27 base = spec(z(PolicyAstR27.leaf("freq", 1, 128))); BirthGateR27.Verdict v = g.check(base); assertTrue(v.ok); g.register(base.policyHash(), v.signature);
        PolicySpecR27 twin = spec(PolicyAstR27.op("add", z(PolicyAstR27.leaf("freq", 1, 128)), PolicyAstR27.constant(0)));
        assertNotEquals(base.policyHash(), twin.policyHash());
        BirthGateR27.Verdict tv = g.check(twin); assertTrue(tv.ok); assertEquals(base.policyHash(), g.duplicateOf(tv.signature)); seen.put(BirthGateR27.Reason.DUPLICATE, "JACCARD>=0.95");
        assertEquals(EnumSet.allOf(BirthGateR27.Reason.class), seen.keySet());
        R27TestSupport.log("H04_PASS reasons=" + seen);
    }
    static void check(BirthGateR27 g, PolicySpecR27 s, BirthGateR27.Reason want, Map<BirthGateR27.Reason, String> seen) {
        BirthGateR27.Verdict v = g.check(s); assertFalse(s.ast.toString(), v.ok); assertEquals(s.ast.toString() + " " + v.detail, want, v.reason); seen.put(want, v.detail);
    }

    static class Recorder implements EvolutionLoopR27.Sink {
        final List<String> events = new ArrayList<>();
        public void policy(PolicySpecR27 s, String st, String r, int g, String d) { events.add("P|" + s.policyHash() + "|" + st); }
        public void lineage(String c, String p, String r, String o, int g) { events.add("L|" + c + "|" + p + "|" + r); }
        public void score(String h, String d, ReplayScoreR27 s) { events.add("S|" + h + "|" + s.split); }
        public void generation(Map<String, Object> r) { Map<String, Object> x = new LinkedHashMap<>(r); for (String k : new String[] {"startedAt", "finishedAt", "replayMs", "candidateReplayP95Ms"}) x.remove(k); events.add("G|" + JsonR27.write(x)); }
        public void promote(PolicySpecR27 s, double t, int g, String r) { events.add("PROMOTE|" + s.policyHash()); }
        public void demote(String h, String r) { events.add("DEMOTE|" + h); }
        public void expert(ExpertProposalsR27.Proposal p, int g) { events.add("E|" + p.expert + "|" + g); }
        public void ablation(int g, String c, List<Object[]> rows) { events.add("A|" + g + "|" + rows.size()); }
        public void holdoutLook(long b, String h) { events.add("H|" + b + "|" + h); }
    }

    @Test public void noOpChildIsDetectedAndNotRegistered() {
        ReplayDatasetR27 d = ReplayDatasetR27.synthetic(3000, 21, null);
        Recorder rec = new Recorder(); EvolutionLoopR27.Config c = new EvolutionLoopR27.Config(); c.births = 0; c.nullControl = false;
        EvolutionLoopR27 loop = new EvolutionLoopR27(c, new FeatureCacheR27(d), rec, () -> 0L);
        PolicySpecR27 parent = spec(PolicyAstR27.op("mul", PolicyAstR27.constant(1.0), z(PolicyAstR27.leaf("freq", 1, 32))));
        BirthGateR27.Verdict pv = loop.context() == null ? null : new BirthGateR27(loop.context().cache, loop.context().split.trainTo).check(parent);
        EvolutionLoopR27.Member m = new EvolutionLoopR27.Member(); m.spec = parent; m.tau = 1; m.island = "I01"; m.sig = pv.signature; loop.population.add(m);
        loop.gateRegister(parent.policyHash(), pv.signature);
        // M_CONST that only rescales a z-scored subtree: the behavior signature cannot change.
        PolicySpecR27 child = parent.withAst(PolicyAstR27.op("mul", PolicyAstR27.constant(1.25), z(PolicyAstR27.leaf("freq", 1, 32))),
                new PolicySpecR27.Meta("I01", "M_CONST", Collections.singletonList(parent.policyHash()), 0, 1, "test"));
        loop.step(); // DATASET
        loop.addPendingForTest(child);
        loop.step(); // BIRTH (0 configured births besides the family minimum; the test child is appended)
        loop.step(); // GATE
        assertEquals("NO_OP", loop.pendingState(child.policyHash()));
        assertFalse(rec.events.contains("P|" + child.policyHash() + "|BORN"));
        R27TestSupport.log("H05_PASS noOp=" + loop.pendingState(child.policyHash()));
    }

    @Test public void canonicalIdentityIsLosslessAndStable() {
        PolicySpecR27 s = new PolicySpecR27(PolicyAstR27.op("mix", 0.1 + 0.2, z(PolicyAstR27.leaf("freq", 2, 512)), z(PolicyAstR27.leaf("gap", 4))), 2, PolicySpecR27.Rule.MARGIN, 0.07000000000000001, 0, 1.3, true, 0.0123456789012345, 0.001, null);
        PolicySpecR27 back = PolicySpecR27.parse(s.fullJson());
        assertEquals(s.policyHash(), back.policyHash()); assertEquals(s.canonical(), back.canonical());
        assertTrue(s.canonical().contains("0.30000000000000004")); assertTrue(s.canonical().contains("0.0123456789012345"));
        R27TestSupport.log("R27-021_PASS hash=" + s.policyHash().substring(0, 16) + " lossless=true");
    }

    @Test public void everyIdeaFamilyProducesExecutableBirths() throws Exception {
        ReplayDatasetR27 d = R27TestSupport.csv(1500); FeatureCacheR27 f = new FeatureCacheR27(d); BirthGateR27 g = new BirthGateR27(f, 1300);
        StatsR27.SplitMix r = new StatsR27.SplitMix(11); StringBuilder b = new StringBuilder();
        for (IdeaFamilyR27 fam : EvolutionLoopR27.FAMILIES) {
            int ok = 0, n = 30; Map<String, Integer> rej = new TreeMap<>();
            for (int i = 0; i < n; i++) { PolicySpecR27 s = fam.generate(r, new IdeaFamilyR27.Context()); BirthGateR27.Verdict v = g.check(s); if (v.ok) ok++; else rej.merge(v.reason.name(), 1, Integer::sum); }
            assertTrue(fam.id() + " has no executable birth", ok > 0);
            b.append(fam.id()).append(':').append(ok).append('/').append(n).append(rej.isEmpty() ? "" : rej.toString()).append(' ');
        }
        R27TestSupport.log("R27-023_PASS " + b);
    }
}
