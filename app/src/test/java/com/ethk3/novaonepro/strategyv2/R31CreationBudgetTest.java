package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.lang.reflect.*;
import java.util.*;
import org.junit.Test;

/** R31-B07-C03#A1/#A2/#A3a/#A4 on the actual evolution loop: every family is explored inside the round budget or
 * carries a reason; injected duplicates raise only the duplicate count and earn no repeated operator credit; a small
 * cost budget is never exceeded while research continues; allocation, seed and credit survive a restart; retired
 * non-champions release their memoized scores. */
public final class R31CreationBudgetTest {
    private int r31LeasesBeforeTest;
    @org.junit.Before public void r31OutstandingLeaseGuardBefore(){FeatureCacheR27.trimRegisteredCachesR31();r31LeasesBeforeTest=MemoryBudgetR31.processBudget().explicitOwnerLeaseCountR31();}
    @org.junit.After public void r31OutstandingLeaseGuardAfter(){
        FeatureCacheR27.trimRegisteredCachesR31();MemoryBudgetR31 memory=MemoryBudgetR31.processBudget();int after=memory.explicitOwnerLeaseCountR31();
        org.junit.Assert.assertTrue("R31_OUTSTANDING_LEASES before="+r31LeasesBeforeTest+" after="+after+" owners="+memory.explicitOwnerLeasesR31(64),after<=r31LeasesBeforeTest);
    }
    static EvolutionLoopR27.Config config(long seed) { EvolutionLoopR27.Config c = new EvolutionLoopR27.Config(); c.births = 48; c.seed = seed; c.i00PerGeneration = 2; c.nullControl = false; return c; }
    @SuppressWarnings("unchecked") static List<Object> pending(EvolutionLoopR27 loop) throws Exception { Field f = EvolutionLoopR27.class.getDeclaredField("pending"); f.setAccessible(true); return (List<Object>) f.get(loop); }
    static Object get(Object pending, String name) throws Exception { Field f = pending.getClass().getDeclaredField(name); f.setAccessible(true); return f.get(pending); }
    static void set(Object pending, String name, Object value) throws Exception { Field f = pending.getClass().getDeclaredField(name); f.setAccessible(true); f.set(pending, value); }
    static Object copyOf(Object pending) throws Exception {
        Constructor<?> c = pending.getClass().getDeclaredConstructor(); c.setAccessible(true); Object copy = c.newInstance();
        for (String name : new String[]{"spec", "state", "reason", "structural"}) set(copy, name, get(pending, name));
        return copy;
    }
    static void stepTo(EvolutionLoopR27 loop, int step) { while (loop.step != step) loop.step(); }

    @Test public void everyFamilyIsExploredEveryRoundOrNamesItsDependency() {
        ReplayDatasetR27 d = ReplayDatasetR27.synthetic(2600, 5, new int[0]);
        for (double pressure : new double[]{0, 0.6}) {
            EvolutionLoopR27.Config cfg = config(31); cfg.creationPressure = pressure;
            try (FeatureCacheR27 cache = new FeatureCacheR27(d); EvolutionLoopR27 loop = new EvolutionLoopR27(cfg, cache, new R27BirthGateTest.Recorder(), () -> 0L)) {
                for (int g = 0; g < 2; g++) loop.runGeneration();
                for (int f = 1; f < EvolutionLoopR27.FAMILIES.length; f++) {
                    String id = EvolutionLoopR27.FAMILIES[f].id();
                    assertEquals(id + " explored in every round", 2, loop.creation.count(id, "rounds"));
                    assertTrue(id + " attempts under pressure " + pressure, loop.creation.count(id, "attempts") >= 2L * Math.max(1, (int) Math.floor(cfg.minPerFamily * (1 - pressure))));
                }
                assertTrue("pressure shrinks the slice", pressure == 0 || loop.creation.total("attempts") < 2L * cfg.births);
                for (String stage : CreationBudgetR31.STAGES) assertTrue(stage, loop.creation.json().toString().contains(stage));
                assertTrue("funnel narrows", loop.creation.total("attempts") >= loop.creation.total("contractValid") && loop.creation.total("contractValid") >= loop.creation.total("unique") && loop.creation.total("unique") >= loop.creation.total("devSurvivor"));
            }
        }
        CreationBudgetR31 b = new CreationBudgetR31(1, 1000, 100);
        Map<String, Integer> a = b.allocate(0, Arrays.asList("I01", "I12", "I13"), Collections.singletonMap("I12", "DEPENDENCY_NOT_READY:NO_LEGACY_ASSET"), Collections.<String, Integer>emptyMap(), 9, 2, 0, true);
        assertEquals(Integer.valueOf(0), a.get("I12")); assertEquals("DEPENDENCY_NOT_READY:NO_LEGACY_ASSET", b.dependencyReason("I12"));
        assertTrue(a.get("I01") >= 2 && a.get("I13") >= 2); assertEquals(9, a.get("I01") + a.get("I13"));
    }

    @Test public void duplicateCandidatesRaiseDedupOnlyAndCreditOnce() throws Exception {
        ReplayDatasetR27 d = ReplayDatasetR27.synthetic(2600, 8, new int[0]);
        long[] unique = new long[2], accepted = new long[2], duplicate = new long[2];
        for (int run = 0; run < 2; run++) {
            try (FeatureCacheR27 cache = new FeatureCacheR27(d); EvolutionLoopR27 loop = new EvolutionLoopR27(config(44), cache, new R27BirthGateTest.Recorder(), () -> 0L)) {
                stepTo(loop, 3);
                if (run == 1) { List<Object> p = pending(loop); Object first = null; for (Object x : p) if (get(x, "spec") != null) { first = x; break; } for (int i = 0; i < 12; i++) p.add(copyOf(first)); }
                loop.step(); // gate
                unique[run] = loop.counter("uniquePolicies"); duplicate[run] = loop.creation.total("duplicate");
                stepTo(loop, 1); accepted[run] = loop.counter("researchAccepted");
            }
        }
        assertEquals("injected copies are not new unique policies", unique[0], unique[1]);
        assertEquals("injected copies are not promoted", accepted[0], accepted[1]);
        assertTrue("dedup count rises by the injected copies", duplicate[1] >= duplicate[0] + 12);
        CreationBudgetR31 b = new CreationBudgetR31(1, 1000, 100);
        assertTrue(b.creditOperator("M_LEAF", "p", "c", "w", .02, .001));
        for (int i = 0; i < 5; i++) assertFalse("a repeated receipt is not credited again", b.creditOperator("M_LEAF", "p", "c", "w", .02, .001));
        assertEquals(1, b.operatorSupport("M_LEAF")); assertEquals(5, b.duplicateCreditRefusals());
        assertEquals("sparse support stays at the exploration prior", 0, b.operatorCredit("M_LEAF"), 0);
        b.creditOperator("M_LEAF", "p", "c2", "w", .02, .001); b.creditOperator("M_LEAF", "p", "c3", "w", .02, .001);
        assertEquals("supported credit is gain minus cost", .019, b.operatorCredit("M_LEAF"), 1e-12);
        double[] prior = MutationOpsR27.defaultWeights(), weighted = b.weighted(prior, MutationOpsR27.OPS);
        int leaf = Arrays.asList(MutationOpsR27.OPS).indexOf("M_LEAF"); double sum = 0; for (double w : weighted) { assertTrue(w > 0); sum += w; }
        assertEquals(1, sum, 1e-9); assertTrue("positive supported credit raises its operator", weighted[leaf] / weighted[0] > prior[leaf] / prior[0]);
    }

    @Test public void researchContextCreditsOneExperimentOnce() throws Exception {
        Constructor<ResearchContextR30R1> c = ResearchContextR30R1.class.getDeclaredConstructor(String.class, long.class); c.setAccessible(true);
        double[][] result = new double[2][];
        for (int copies = 1; copies <= 2; copies++) {
            ResearchContextR30R1 ctx = c.newInstance("dataset", 1L);
            Field frames = ResearchContextR30R1.class.getDeclaredField("frames"); frames.setAccessible(true);
            List<Object> points = new ArrayList<>();
            for (int i = 0; i < copies * 3; i++) { Map<String, Object> p = new LinkedHashMap<>(); p.put("operator", "M_LEAF"); p.put("parentHash", "parent"); p.put("childHash", "child"); p.put("lower", .03); p.put("upper", .05); points.add(p); }
            Map<String, Object> frame = new LinkedHashMap<>(); frame.put("points", points);
            @SuppressWarnings("unchecked") Map<String, Map<String, Object>> f = (Map<String, Map<String, Object>>) frames.get(ctx); f.put("R10", frame);
            result[copies - 1] = ctx.weights(MutationOpsR27.defaultWeights());
        }
        assertArrayEquals("repeated receipts of one experiment do not multiply its credit", result[0], result[1], 0);
    }

    @Test public void smallCostBudgetIsNeverExceededAndResearchContinues() throws Exception {
        ReplayDatasetR27 d = ReplayDatasetR27.synthetic(2600, 13, new int[0]);
        long slice, cap; int rows;
        try (FeatureCacheR27 cache = new FeatureCacheR27(d); EvolutionLoopR27 probe = new EvolutionLoopR27(config(52), cache, new R27BirthGateTest.Recorder(), () -> 0L)) {
            stepTo(probe, 4); rows = probe.context().split.selectTo - probe.context().split.selectFrom;
            List<Long> costs = new ArrayList<>(); for (Object p : pending(probe)) if ("BORN".equals(get(p, "state"))) costs.add(CreationBudgetR31.costUnits((PolicySpecR27) get(p, "spec"), rows));
            Collections.sort(costs); assertTrue("fixture has candidates", costs.size() >= 6);
            slice = costs.get(costs.size() / 2); cap = slice * 3;
        }
        EvolutionLoopR27.Config cfg = config(52); cfg.creationCostUnits = cap; cfg.creationSliceUnits = slice;
        try (FeatureCacheR27 cache = new FeatureCacheR27(d); EvolutionLoopR27 loop = new EvolutionLoopR27(cfg, cache, new R27BirthGateTest.Recorder(), () -> 0L)) {
            for (int g = 0; g < 3; g++) {
                stepTo(loop, 4);
                // the most expensive born candidate is over the slice (fixture median) and must be deferred
                Object largest = null; long most = -1;
                for (Object p : pending(loop)) if ("BORN".equals(get(p, "state"))) { long u = CreationBudgetR31.costUnits((PolicySpecR27) get(p, "spec"), rows); if (u > most) { most = u; largest = p; } }
                loop.step(); // replay with admission
                long spent = 0;
                for (Object p : pending(loop)) {
                    if (get(p, "spec") == null) continue;
                    long u = CreationBudgetR31.costUnits((PolicySpecR27) get(p, "spec"), rows);
                    if ("BORN".equals(get(p, "state"))) { assertTrue("no admitted slice exceeds the slice budget", u <= slice); spent += u; }
                    if ("DEFERRED_CREATION_BUDGET".equals(get(p, "state"))) assertTrue(String.valueOf(get(p, "reason")).startsWith("R31_CREATION_BUDGET:units=" + u));
                }
                assertTrue("generation budget never exceeded: " + spent + "/" + cap, spent <= cap);
                assertEquals(spent, loop.creation.spentThisGeneration());
                if (most > slice) assertEquals("DEFERRED_CREATION_BUDGET", get(largest, "state"));
                stepTo(loop, 1);
                assertEquals("research continues to the next generation", g + 1, loop.generation);
            }
            assertTrue("expensive candidates were deferred, not silently replayed", loop.creation.deferredTotal() > 0);
            assertTrue("research still produced replayed candidates", loop.creation.total("replayed") > 0);
        }
    }

    @Test public void restartKeepsAllocationSeedAndCreditAndRetirementReleasesMemory() throws Exception {
        ReplayDatasetR27 d = ReplayDatasetR27.synthetic(2600, 21, new int[0]);
        EvolutionLoopR27.Config cfg = config(63);
        try (FeatureCacheR27 cache = new FeatureCacheR27(d); FeatureCacheR27 cache2 = new FeatureCacheR27(d);
             EvolutionLoopR27 straight = new EvolutionLoopR27(cfg, cache, new R27BirthGateTest.Recorder(), () -> 0L)) {
            straight.runGeneration(); stepTo(straight, 3);
            String snap = straight.snapshot();
            try (EvolutionLoopR27 resumed = EvolutionLoopR27.restore(snap, cfg, cache2, new R27BirthGateTest.Recorder(), () -> 0L)) {
                assertEquals("allocation, seed, funnel and credit survive the restart", straight.creation.json(), resumed.creation.json());
                assertEquals(straight.creation.lastAllocation(), resumed.creation.lastAllocation());
                stepTo(straight, 1); stepTo(resumed, 1);
                assertEquals("restored loop continues identically", straight.creation.json(), resumed.creation.json());
                assertEquals(straight.counter("uniquePolicies"), resumed.counter("uniquePolicies"));
            }
            // retirement: a retired non-champion with no archive cell and no sibling sharing its backing releases its
            // memoized scores; the champion, archive elites and shared backings are kept.
            int rows = straight.context().split.selectTo - straight.context().split.selectFrom;
            PolicySpecR27 retiredSpec = new PolicySpecR27(PolicyAstR27.op("zscore", PolicyAstR27.leaf("freq", 1, 77)), 6, PolicySpecR27.Rule.ALWAYS, 0, 0, 1, false, .01, .001, null);
            straight.context().scores(retiredSpec); assertTrue(straight.context().holdsScoresR31(retiredSpec));
            EvolutionLoopR27.Member retired = new EvolutionLoopR27.Member(); retired.spec = retiredSpec;
            straight.retiredReleasedBytesR31 = 0; straight.releaseRetiredR31(retired);
            assertFalse("retired non-champion released its score backing", straight.context().holdsScoresR31(retiredSpec));
            assertTrue("released bytes are real: " + straight.retiredReleasedBytesR31, straight.retiredReleasedBytesR31 >= 8L * rows);
            assertNotNull("a generation produced a champion", straight.champion);
            EvolutionLoopR27.Member championMember = new EvolutionLoopR27.Member(); championMember.spec = straight.champion;
            straight.context().scores(straight.champion); straight.releaseRetiredR31(championMember);
            assertTrue("the champion is never released", straight.context().holdsScoresR31(straight.champion));
            if (!straight.population.isEmpty()) {
                EvolutionLoopR27.Member live = straight.population.get(straight.population.size() - 1);
                EvolutionLoopR27.Member twin = new EvolutionLoopR27.Member(); twin.spec = live.spec.withSelector(live.spec.position, live.spec.rule, live.spec.threshold + .01, live.spec.rate, null);
                straight.context().scores(live.spec); straight.releaseRetiredR31(twin);
                assertTrue("a backing shared with a live member is kept", straight.context().holdsScoresR31(live.spec));
            }
            assertEquals(CreationBudgetR31.retirable(Arrays.asList("a", "b", "c"), "b", Collections.singleton("c")), Collections.singletonList("a"));
        }
    }
}
