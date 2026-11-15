package com.ethk3.novaonepro.strategyv2;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.util.*;
import static org.junit.Assert.*;
@RunWith(AndroidJUnit4.class)
public final class ResearchSchedulingDeviceSU1Test {
    private static BudgetTokenSU1 budget(){return new BudgetTokenSU1(60000,10000000,null);}
    @Test public void scoutFitsStayOutOfFullPredictionsAndFailedCandidatesDoNotStopSearch(){
        int[][] draws=new int[3400][10];for(int t=0;t<draws.length;t++)for(int p=0;p<10;p++)draws[t][p]=(p+t)%10+1;
        RelationTensorAdapterSU1.Factory factory=new RelationTensorAdapterSU1.Factory(draws);
        assertEquals(8,RelGraphTrainerSU1.trainCausal(factory,draws,100,2,4,19,budget(),TrainingFidelitySU1.FULL).steps);
        assertEquals(2,RelGraphTrainerSU1.trainCausal(factory,draws,100,2,4,19,budget(),TrainingFidelitySU1.SCOUT).steps);
        for(String text:new String[]{"DEEP(2,8,19)","GDEEP(2,4,19)","FUSE(EXPERT(0),XT(3),60)"}){
            ScoreHistoryCacheSU1 cache=new ScoreHistoryCacheSU1(768);ProgramTreeSU1.Node program=ProgramTreeSU1.parse(text);program.score(new ProgramTreeSU1.Context(draws,100,null,cache,budget()).withTrainingFidelity(TrainingFidelitySU1.SCOUT));
            double[][] a=program.score(new ProgramTreeSU1.Context(draws,100,null,cache,budget())),b=program.score(new ProgramTreeSU1.Context(draws,100,null,new ScoreHistoryCacheSU1(768),budget()));for(int p=0;p<10;p++)assertArrayEquals(b[p],a[p],0);
        }
        String dataset=HashingSU1.text("Android search recovery");StrategyFoundrySU1.Genome missing=new StrategyFoundrySU1.Genome("MODEL("+HashingSU1.text("not installed")+",8)","GRAMMAR","DEEP","mutate","",-1,1,false),valid=new StrategyFoundrySU1.Genome("REL(PLUS1,1,0,1)","GRAMMAR","RELATION","mutate","",-1,1,false);
        try(StrategyFoundrySU1 foundry=new StrategyFoundrySU1(draws,dataset,11,8,1)){
            foundry.startRefresh(Arrays.asList(missing,valid));StrategyFoundrySU1.Attempt failed=foundry.next(budget());assertEquals("REJECTED",failed.result);assertTrue(failed.detail.contains("DEPENDENCY"));assertEquals(0,foundry.evaluated);
            String checkpoint=foundry.checkpoint();try(StrategyFoundrySU1 restored=StrategyFoundrySU1.restore(draws,dataset,checkpoint)){restored.restoreSeen(Collections.singleton(missing.key()));StrategyFoundrySU1.Attempt actual=restored.next(budget());assertNotNull(actual.ranked);assertTrue(actual.detail.contains("FINAL_FIDELITY=FULL"));}
        }
    }
}
