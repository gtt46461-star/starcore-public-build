package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;

public class R30NumberRankerTest {
    @Test public void trainerProviderAndRestoredModelUseIdenticalLearnedNumberHead(){
        AtomNetTrainerR27 trainer=new AtomNetTrainerR27(12,30);trainer.onlineLr=.001;
        double[] x=AtomNetR27.positionedInput(new double[]{.2,.7},3);double[] before=trainer.numberProbabilities(x);
        for(int i=0;i<40;i++)trainer.online(new AtomNetTrainerR27.Sample(x,1,7,"unique-"+i));
        double[] learned=trainer.numberProbabilities(x);assertFalse(Arrays.equals(before,learned));assertTrue(learned[7]>before[7]);
        assertArrayEquals(trainer.net.forward(x,null).pClass,learned,0);
        ModelSurfaceProviderR30.FrozenModelRef ref=new ModelSurfaceProviderR30.FrozenModelRef(trainer.net.encode(),99,10_000);
        ModelSurfaceProviderR30.Surface surface=ModelSurfaceProviderR30.predict(new ModelSurfaceProviderR30.Pit(100,99,3,"history",x,19_000,20_000,20_000,30_000),ref);
        assertArrayEquals(learned,surface.probabilities(),0);
        ModelArtifactR29 artifact=new ModelArtifactR29(trainer,new FtrlR27(7,.05,1,1e-4,1e-3),"d","issue");byte[] blob=artifact.encode();
        assertArrayEquals(learned,ModelArtifactR29.decode(blob,StatsR27.sha256(blob)).trainer.numberProbabilities(x),0);
        double sum=0;for(double p:learned)sum+=p;assertEquals(1,sum,PitGuardR30.PROB_SUM_TOLERANCE);
        learned[0]=99;assertArrayEquals(surface.probabilities(),trainer.numberProbabilities(x),0);
    }
    @Test public void invalidNumberSurfaceIsRejectedWithoutRepairingIt(){
        AtomNetR27.Out out=new AtomNetR27.Out();Arrays.fill(out.pClass,.1);double[] p=NumberRankerAdapterR30.probabilities(out);p[0]=1;assertEquals(.1,out.pClass[0],0);
        out.pClass[0]=.2;try{NumberRankerAdapterR30.probabilities(out);fail("sum was normalized");}catch(IllegalArgumentException expected){}
        out.pClass[0]=Double.NaN;try{NumberRankerAdapterR30.probabilities(out);fail("NaN was repaired");}catch(IllegalArgumentException expected){}
    }
}
