package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;

public class R30ModelSurfaceTest {
    static ModelSurfaceProviderR30.Pit pit(long through,long available){return new ModelSurfaceProviderR30.Pit(100,through,1,"history",AtomNetR27.positionedInput(new double[]{.2,.8},1),available,20_000,20_000,30_000);}
    static void rejected(Runnable r,String reason){try{r.run();fail("accepted "+reason);}catch(ModelSurfaceProviderR30.Blocked expected){assertTrue(expected.getMessage(),expected.getMessage().contains(reason));}}
    @Test public void actualNumberHeadAndBothInputsAndOutputsAreImmutable(){
        AtomNetR27 n=new AtomNetR27(12,7);byte[] weights=n.encode();ModelSurfaceProviderR30.FrozenModelRef ref=new ModelSurfaceProviderR30.FrozenModelRef(weights,99,10_000);
        weights[weights.length-1]^=1;
        ModelSurfaceProviderR30.Surface s=ModelSurfaceProviderR30.predict(pit(99,19_000),ref);double[] expected=n.forward(AtomNetR27.positionedInput(new double[]{.2,.8},1),null).pClass;
        assertArrayEquals(expected,s.probabilities(),0);double[] changed=s.probabilities();changed[0]=99;assertArrayEquals(expected,s.probabilities(),0);
        byte[] exposed=ref.weights();exposed[0]=0;assertArrayEquals(n.encode(),ref.weights());double sum=0;for(double p:s.probabilities())sum+=p;assertEquals(1,sum,1e-12);
    }
    @Test public void targetTrainingLateAvailabilityAndBadArchivedProbabilitiesAreBlocked(){
        byte[] weights=new AtomNetR27(12,7).encode();
        rejected(()->ModelSurfaceProviderR30.predict(pit(99,19_000),new ModelSurfaceProviderR30.FrozenModelRef(weights,100,10_000)),"MODEL_SEES_TARGET");
        rejected(()->ModelSurfaceProviderR30.predict(pit(100,19_000),new ModelSurfaceProviderR30.FrozenModelRef(weights,99,10_000)),"FEATURE_SEES_TARGET");
        rejected(()->ModelSurfaceProviderR30.predict(pit(99,19_000),new ModelSurfaceProviderR30.FrozenModelRef(weights,99,20_001)),"MODEL_AFTER_FREEZE");
        rejected(()->ModelSurfaceProviderR30.predict(pit(99,19_000),null),"NO_FROZEN_MODEL");
        rejected(()->ModelSurfaceProviderR30.archived(StatsR27.sha256(weights),"h",new double[10],100,99,10_000,99,19_000,1,20_000,20_000,30_000),"PROB_SUM");
    }
    @Test public void typedAstConsumesActualSurfaceAndMissingRowsCannotCarryThroughEma(){
        ReplayDatasetR27 d=ReplayDatasetR27.synthetic(50,14,new int[0]);FeatureCacheR27 f=new FeatureCacheR27(d);
        int row=30;ModelSurfaceProviderR30.FrozenModelRef ref=new ModelSurfaceProviderR30.FrozenModelRef(new AtomNetR27(12,7).encode(),d.ordinals[row]-1,10_000);
        ModelSurfaceProviderR30.Surface s=ModelSurfaceProviderR30.predict(new ModelSurfaceProviderR30.Pit(d.ordinals[row],d.ordinals[row]-1,1,ModelSurfaceProviderR30.historyHash(d,row),AtomNetR27.positionedInput(new double[]{.2,.8},1),19_000,20_000,20_000,30_000),ref);
        Map<Long,ModelSurfaceProviderR30.Surface> rows=new HashMap<>();rows.put(ModelSurfaceProviderR30.key(row,1),s);f.bindModelSurfaces(ModelSurfaceProviderR30.source(rows,Collections.emptyMap()));
        PolicyAstR27.Node leaf=PolicyAstR27.parse("(model_surface 1)");assertEquals(PolicyAstR27.Type.PROB,PolicyAstR27.type(leaf));assertNull(PolicyAstR27.rangeAndCausality(leaf));
        double[] p=PolicyAstR27.eval(leaf,f,1);assertArrayEquals(s.probabilities(),Arrays.copyOfRange(p,row*10,row*10+10),0);
        double[] ema=PolicyAstR27.eval(PolicyAstR27.op("ema",.5,leaf),f,1);for(int n=0;n<10;n++)assertTrue(Double.isNaN(ema[(row+1)*10+n]));
        assertEquals("RANGE:model_surface",PolicyAstR27.rangeAndCausality(PolicyAstR27.leaf("model_surface",11)));
        f.bindModelSurfaces(null);for(double x:PolicyAstR27.eval(leaf,f,1))assertTrue(Double.isNaN(x));
    }
    @Test public void i08ActuallyProposesModelLeavesAndRetainsFamilyCapability(){
        IdeaI08NeuralRepresentationR27 family=new IdeaI08NeuralRepresentationR27();StatsR27.SplitMix r=new StatsR27.SplitMix(30);int models=0,legacy=0;
        for(int i=0;i<1000;i++){PolicySpecR27 p=family.generate(r,new IdeaFamilyR27.Context());assertTrue(PolicyContractR30.violations(p).toString(),PolicyContractR30.violations(p).isEmpty());if(p.ast.modelPositions!=0)models++;else legacy++;}
        assertTrue(models>=400);assertTrue(legacy>=400);assertEquals(ModelSurfaceProviderR30.goldenHash(),ModelSurfaceProviderR30.goldenHash());
        System.out.println("R30_I08|MODEL_PROPOSALS="+models+"|FAMILY_PROPOSALS="+legacy+"|NO_UNAVAILABLE_MODEL_BIRTH_CLAIM");
    }
}
