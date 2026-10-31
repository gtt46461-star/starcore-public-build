package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;

public final class R30FailureRepairTest {
    static PolicySpecR27 policy(PolicyAstR27.Node ast, int pos) {
        return new PolicySpecR27(ast,pos,PolicySpecR27.Rule.MARGIN,0,0,1,false,0,0,
                IdeaFamilyR27.meta("I09","BIRTH",0,307,"failure repair fixture"));
    }
    static PolicyAstR27.Node sum(int from, int to) {
        if (to - from == 1) return PolicyAstR27.op("zscore",PolicyAstR27.leaf("freq",from,8));
        int middle = (from + to) / 2;
        return PolicyAstR27.op("add",sum(from,middle),sum(middle,to));
    }
    static PolicySpecR27 rich() { return policy(PolicyAstR27.op("add",sum(1,11),PolicyAstR27.constant(0.2)),1); }

    /** Synthetic counterexample: position 7 always misses the unchanged parent's K3 on H_SELECT. All ten
     * numbers remain a permutation, and feature data / H_TRAIN labels are unchanged. */
    static EvalContextR27 failedPositionContext(PolicySpecR27 parent) {
        ReplayDatasetR27 data = ReplayDatasetR27.synthetic(1800,307,new int[0]);
        FeatureCacheR27 cache = new FeatureCacheR27(data); SplitR27 split = SplitR27.of(data,null);
        EvalContextR27 original = new EvalContextR27(cache,split,null);
        ReplayEvaluatorR27.Trace trace = original.selectTrace(parent,1);
        int[][] labels = new int[data.size()][];
        for (int t = 0; t < data.size(); t++) {
            labels[t] = data.draws[t].clone();
            if (t < split.selectFrom || t >= split.selectTo || trace.skipped[t]) continue;
            int miss = 1;
            while (miss - 1 == trace.k3[t*3] || miss - 1 == trace.k3[t*3+1] || miss - 1 == trace.k3[t*3+2]) miss++;
            int other = 0; while (labels[t][other] != miss) other++;
            int old = labels[t][6]; labels[t][6] = miss; labels[t][other] = old;
        }
        return new EvalContextR27(cache,split,labels);
    }

    @Test public void measuredFailureDrivesActualWindowMutationAndFamilyRepair() {
        PolicySpecR27 parent = rich(); EvalContextR27 evaluation = failedPositionContext(parent);
        FailureProfileR30 p = evaluation.failureProfile(parent);
        assertTrue(p.ready()); assertEquals(7,p.position); assertEquals(512,p.window);
        assertEquals(512,p.nAct); assertEquals(0,p.hits);
        IdeaFamilyR27.Context ctx = new IdeaFamilyR27.Context(); ctx.evaluation = evaluation;
        StatsR27.SplitMix rng = new StatsR27.SplitMix(7); int changed = 0, familyChildren = 0;
        List<PolicyAstR27.Node> original = PolicyAstR27.nodes(parent.ast);
        for (int i = 0; i < 100; i++) {
            PolicySpecR27 child = MutationOpsR27.mutate(parent,"M_WINDOW",rng,ctx,null);
            assertNotNull(child); assertTrue(child.meta.reason.contains("\"position\":7"));
            List<PolicyAstR27.Node> nodes = PolicyAstR27.nodes(child.ast);
            for (int j = 0; j < original.size(); j++) if (original.get(j).leaf()) {
                PolicyAstR27.Node leaf = original.get(j);
                if ("freq".equals(leaf.op) && leaf.ints[0] == 7) {
                    if (!leaf.equals(nodes.get(j))) changed++;
                } else assertEquals("changed an input outside the failed position",leaf,nodes.get(j));
            }
            PolicySpecR27 family = IdeaFamilyR27.Grow.swapLeaf(parent,new String[]{"freq"},"I09",1,ctx,rng);
            if (family != null) { familyChildren++; assertTrue(family.meta.reason.contains("failureProfile=")); }
        }
        assertTrue("window repair was only a no-op",changed >= 20); assertTrue(familyChildren >= 90);
        R27TestSupport.log("R30_FAILURE_TARGET " + p.json() + " actualWindowChanges=" + changed + " familyChildren=" + familyChildren);
    }

    @Test public void confirmAndFutureLabelsCannotChangeTheProfile() {
        ReplayDatasetR27 data = ReplayDatasetR27.synthetic(3000,307,new int[0]); SplitR27 split = SplitR27.of(data,null);
        assertTrue(split.holdoutAvailable); int[][] poisoned = new int[data.size()][];
        for (int t = 0; t < data.size(); t++) poisoned[t] = t >= split.selectTo ? null : data.draws[t].clone();
        // null sealed/open rows throw on any attempted label read, including a hidden temperature fit.
        PolicySpecR27 parent = rich(); FeatureCacheR27 cache = new FeatureCacheR27(data);
        FailureProfileR30 a = new EvalContextR27(cache,split,null).failureProfile(parent);
        FailureProfileR30 b = new EvalContextR27(cache,split,poisoned).failureProfile(parent);
        assertEquals(a.json(),b.json()); assertTrue(a.ready()); assertTrue(a.from >= split.selectFrom); assertEquals(split.selectTo,a.to);
        PolicySpecR27 silent = parent.withSelector(1,PolicySpecR27.Rule.MARGIN,0.70,0,parent.meta);
        FailureProfileR30 missing = new EvalContextR27(cache,split,poisoned).failureProfile(silent);
        assertEquals("INSUFFICIENT",missing.status); assertEquals(0,missing.nAct); assertTrue(Double.isNaN(missing.hitRate()));
        assertEquals("NOT_RUN",FailureProfileR30.forParent(parent,new IdeaFamilyR27.Context()).status);
    }

    @Test public void allOperatorProductsAreCompleteAndCarryRealProfiles() {
        PolicySpecR27 parent = rich(); EvalContextR27 evaluation = failedPositionContext(parent);
        IdeaFamilyR27.Context ctx = new IdeaFamilyR27.Context(); ctx.evaluation = evaluation;
        List<PolicySpecR27> parents = Arrays.asList(parent,policy(PolicyAstR27.op("zscore",PolicyAstR27.leaf("freq",2,128)),2),
                policy(PolicyAstR27.op("zscore",PolicyAstR27.leaf("gap",3)),3),policy(PolicyAstR27.op("zscore",PolicyAstR27.leaf("cross",4,1)),4));
        StatsR27.SplitMix rng = new StatsR27.SplitMix(30407); Map<String,Integer> proposed = new TreeMap<>();
        int produced = 0, valid = 0, attempted = 0;
        for (String op : MutationOpsR27.OPS) {
            int n = 0;
            for (PolicySpecR27 p : parents) for (int i = 0; i < 100; i++) {
                attempted++; PolicySpecR27 child = MutationOpsR27.mutate(p,op,rng,ctx,null);
                if (child == null) continue; n++; produced++;
                assertTrue(child.meta.reason.contains("failureProfile=")); assertTrue(child.meta.reason.contains("H_SELECT"));
                if (PolicyContractR30.violations(child).isEmpty()) valid++;
            }
            assertTrue(op + " produced no meaningful sample",n >= 20); proposed.put(op,n);
        }
        for (String op : new String[]{"X_SUBTREE","X_INPUT","F_STACK"}) {
            int n = 0;
            for (int i = 0; i < 200; i++) {
                PolicySpecR27 a = parents.get(i % parents.size()), b = parents.get((i+1) % parents.size()); attempted++;
                PolicySpecR27 child = "F_STACK".equals(op) ? FusionOpsR27.fuse(parents,evaluation,rng,1) :
                        "X_INPUT".equals(op) ? CrossoverOpsR27.input(a,b,rng,1,evaluation) : CrossoverOpsR27.subtree(a,b,rng,1,evaluation);
                if (child == null) continue; n++; produced++;
                assertTrue(child.meta.reason,child.meta.reason.contains("failureProfile="));
                if (PolicyContractR30.violations(child).isEmpty()) valid++;
            }
            assertTrue(op + " produced no meaningful sample",n >= 20); proposed.put(op,n);
        }
        assertTrue(valid / (double)produced >= 0.99);
        R27TestSupport.log("R30_REPAIR_CONTRACT attempts=" + attempted + " proposed=" + produced + " valid=" + valid + " notProposed=" + (attempted-produced) + " perOperator=" + proposed);
    }
}
