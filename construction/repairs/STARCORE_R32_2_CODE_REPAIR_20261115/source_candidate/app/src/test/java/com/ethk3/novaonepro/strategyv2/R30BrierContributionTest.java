package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;

public class R30BrierContributionTest {
    static PolicySpecR27 spec(PolicyAstR27.Node ast){return new PolicySpecR27(ast,1,PolicySpecR27.Rule.ALWAYS,0,0,1,false,0,0,IdeaFamilyR27.meta("I09","TEST",0,30,"paired Brier fixture"));}
    static EvalContextR27 ctx(){ReplayDatasetR27 d=ReplayDatasetR27.synthetic(3000,30,new int[0]).planted(.6,31);SplitR27 split=SplitR27.of(d,null);int[][] labels=d.draws.clone();for(int t=split.selectTo;t<labels.length;t++)labels[t]=null;return new EvalContextR27(new FeatureCacheR27(d),split,labels);}
    @Test public void constantAtomHasZeroContributionAndNeverReadsConfirm(){
        double[][] v=new double[1000][];boolean[][] present=new boolean[1000][];int[] y=new int[1000];
        for(int t=0;t<900;t++){v[t]=new double[]{.5};present[t]=new boolean[]{true};y[t]=t%10<3?1:0;}Arrays.fill(y,900,1000,Integer.MIN_VALUE);
        AtomScoreR27.Score score=AtomScoreR27.score(0,v,present,y,600,900);AtomScoreR27.Contribution c=score.contribution();
        assertEquals(300,c.gains.n);assertEquals(0,c.mean(),0);assertEquals(0,c.lower95(),0);assertEquals(0,score.nHoldout);assertEquals(AtomScoreR27.NEUTRAL,score.state);
    }
    @Test public void zeroWeightedSubtreeIsNotGivenPositiveBrierContribution(){
        EvalContextR27 ctx=ctx();PolicyAstR27.Node sub=PolicyAstR27.op("zscore",PolicyAstR27.leaf("freq",1,32));
        PolicySpecR27 p=spec(PolicyAstR27.op("add",PolicyAstR27.op("zscore",PolicyAstR27.leaf("cross",2,1)),PolicyAstR27.op("mul",PolicyAstR27.constant(0),sub)));
        AtomScoreR27.Contribution c=DerivedAtomR27.pairedContribution(sub,Collections.singletonList(p),ctx);
        assertTrue(c.gains.n>0);assertEquals(0,c.mean(),0);assertEquals(0,c.lower95(),0);
    }
    @Test public void ablationUsesOriginalK3EventAndCountsUniquePeriodsNotPolicyCopies(){
        EvalContextR27 ctx=ctx();PolicyAstR27.Node root=PolicyAstR27.leaf("cross",2,1);PolicySpecR27 p=spec(root);
        ReplayEvaluatorR27.Trace full=ReplayEvaluatorR27.selectTraceWithProbabilities(p,ctx,1);
        ReplayEvaluatorR27.Trace normal=ctx.selectTrace(p,1);assertNull(normal.probabilityRows);assertArrayEquals(normal.pHit,full.pHit,0);assertArrayEquals(normal.hit,full.hit);
        AtomScoreR27.Contribution actual=DerivedAtomR27.pairedContribution(root,Arrays.asList(p,p),ctx);StatsR27.Moments expected=new StatsR27.Moments();
        for(int t=ctx.split.selectFrom;t<ctx.split.selectTo;t++)if(!full.skipped[t])expected.add(AtomScoreR27.pairedBrier(full.pHit[t],.1+.1+.1,full.hit[t]));
        assertEquals(expected.n,actual.gains.n);assertEquals(expected.n*2,actual.comparisons);assertEquals(expected.mean,actual.mean(),1e-12);assertEquals(expected.lcb(StatsR27.Z95_ONE_SIDED),actual.lower95(),1e-12);
        for(int t=ctx.split.selectTo;t<ctx.data.size();t++){assertTrue(full.skipped[t]);assertNull(full.probabilityRows[t]);}
        System.out.println("R30_PAIRED_BRIER|SAME_ORIGINAL_K3|PERIODS="+actual.gains.n+"|POLICY_COMPARISONS="+actual.comparisons+"|H_CONFIRM_LABELS_NULL|PASS");
    }
}
