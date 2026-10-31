package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;

public final class R30NullPipelineTest {
    @Test public void completeDrawPermutationHasARealIdentityAndLeavesConfirmUntouched() {
        ReplayDatasetR27 data=ReplayDatasetR27.synthetic(3000,123,new int[0]);SplitR27 split=SplitR27.of(data,null);
        ReplayDatasetR27 shuffled=ReplayEvaluatorR27.shuffledDataset(data,split.trainFrom,split.selectTo,29);
        assertNotEquals(data.datasetHash,shuffled.datasetHash);
        for(int t=split.selectTo;t<data.size();t++)assertArrayEquals(data.draws[t],shuffled.draws[t]);
        int[] a=new int[10],b=new int[10];
        for(int t=0;t<split.selectTo;t++){a[data.draws[t][0]-1]++;b[shuffled.draws[t][0]-1]++;}
        assertArrayEquals(a,b);assertEquals(data.ordinals[0],shuffled.ordinals[0]);
    }
    @Test public void sameFullEvolutionPipelineResumesItsControlState() {
        ReplayDatasetR27 data=ReplayDatasetR27.synthetic(3000,401,new int[0]);FeatureCacheR27 cache=new FeatureCacheR27(data);SplitR27 split=SplitR27.of(data,null);
        NullEvolutionR27 straight=new NullEvolutionR27(),resumed=new NullEvolutionR27();
        straight.seedAssets=Collections.singletonList(NullPipelineSimulationR30.neutralSeed());
        resumed.seedAssets=straight.seedAssets;
        StatsR27.SplitMix ra=new StatsR27.SplitMix(37),rb=new StatsR27.SplitMix(37);
        for(int gen=0;gen<4;gen++){
            straight.generation(cache,split,EvolutionLoopR27.FAMILIES,40,ra,gen,26);
            resumed.generation(cache,split,EvolutionLoopR27.FAMILIES,40,rb,gen,26);
            assertEquals(JsonR27.write(straight.json()),JsonR27.write(resumed.json()));
            NullEvolutionR27 restored=new NullEvolutionR27();restored.restoreFull(resumed.snapshot());restored.seedAssets=straight.seedAssets;
            restored.champion=resumed.champion;restored.championTau=resumed.championTau;restored.lastSeed=resumed.lastSeed;resumed=restored;
        }
        Map<?,?> receipts=(Map<?,?>)straight.json().get("receipts");
        assertEquals(4L,receipts.get("completedGenerations"));assertTrue(straight.births>0);assertTrue(receipts.containsKey("expertReceipts"));
        assertFalse(receipts.containsKey("scores:H_HOLDOUT"));
        R27TestSupport.log("R30_FULL_NULL_PIPELINE "+JsonR27.write(straight.json()));
    }
    @SuppressWarnings("unchecked") @Test public void powerAndFullPipelineQualificationHaveRecoverableSyntheticReceipts() {
        NullPipelineSimulationR30 straight=new NullPipelineSimulationR30(),resumed=new NullPipelineSimulationR30();int work=0;
        while(!straight.complete()){
            assertTrue(straight.step());assertTrue(resumed.step());work++;
            resumed=NullPipelineSimulationR30.restore(resumed.snapshot());
            if(work>200)fail("simulation did not finish within its finite work units");
        }
        assertTrue(resumed.complete());assertEquals(JsonR27.write(straight.summary()),JsonR27.write(resumed.summary()));
        Map<String,Object> report=straight.summary();assertEquals(false,report.get("naturalQualityEligible"));assertEquals(false,report.get("mainAlphaBudgetSpent"));
        assertTrue("full pipeline must actually feed at least one frozen candidate to qualification",((Number)report.get("eligibleNullCandidates")).longValue()>0);
        assertTrue(((Number)report.get("falseSequentialQualifications")).longValue()<=2);
        List<Object> power=(List<Object>)report.get("power");Map<String,Object> strong=(Map<String,Object>)power.get(2);
        assertEquals(20L,strong.get("experiments"));assertTrue(((Number)strong.get("detected")).longValue()>=18);
        R27TestSupport.log("R30_Q08_MEASURED workUnits="+work+" "+JsonR27.write(report));
    }
}
