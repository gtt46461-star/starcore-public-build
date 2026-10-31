package com.ethk3.novaonepro.strategyv2;

import com.ethk3.novaonepro.AtomReplayR27;
import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;

public class R30TenPositionTrainingTest {
    @Test public void everyPeriodProducesTenPositionSamplesAndAllTenArePit() {
        ReplayDatasetR27 d=ReplayDatasetR27.synthetic(100,30,new int[0]);
        AtomReplayR27.Frame all=AtomReplayR27.build(d,24,50);assertEquals(26*10,all.rows());
        for(int i=0;i<all.rows();i++){
            int p=i%10+1;assertEquals(p,all.position[i]);assertEquals(24+i/10,all.periodRow[i]);assertNotNull(all.values[i]);
            assertEquals(d.draws[all.periodRow[i]][p-1],all.actual[i]);
            double[] x=AtomNetR27.positionedInput(AtomReplayR27.features(all.values[i],all.present[i],true),p);
            assertEquals(182,x.length);for(int j=0;j<10;j++)assertEquals(j==p-1?1:0,x[172+j],0);
        }
        int[][] labels=new int[d.size()][];StatsR27.SplitMix r=new StatsR27.SplitMix(998);
        for(int i=0;i<d.size();i++)labels[i]=i<70?d.draws[i]:ReplayDatasetR27.randomPermutation(r);
        AtomReplayR27.Frame a=AtomReplayR27.build(d,70,71),b=AtomReplayR27.build(d.withDraws(labels),70,71);
        for(int p=0;p<10;p++){assertArrayEquals(a.values[p],b.values[p],0);assertArrayEquals(a.present[p],b.present[p]);assertArrayEquals(a.k3[p],b.k3[p]);}
        assertEquals(26,AtomReplayR27.buildLegacy(d,24,50).rows());
    }
    static AtomNetTrainerR27.Sample sample(int i){
        int p=i%10+1;double[] x=AtomNetR27.positionedInput(new double[]{i%7/7.,i%3/3.},p);
        return new AtomNetTrainerR27.Sample(x,i%3==0?1:0,i%10,AtomNetSessionR30.eventKey(String.format(java.util.Locale.ROOT,"%012d",100+i/10),p));
    }
    static AtomNetSessionR30 session(){return new AtomNetSessionR30("dataset","000000000999",999,12,80,40,20);}
    static AtomNetSessionR30 copy(AtomNetSessionR30 s){byte[] b=s.encode();return AtomNetSessionR30.decode(b,StatsR27.sha256(b));}
    @Test public void everyPretrainAndOnlineBoundaryResumesExactlyWithinOriginalBudget(){
        AtomNetSessionR30 a=session(),b=copy(a);byte[] initial=a.trainer.net.encode();int units=0;
        while(!a.ready()){
            assertTrue(a.work(R30TenPositionTrainingTest::sample)<=64);b.work(R30TenPositionTrainingTest::sample);b=copy(b);
            assertArrayEquals(a.trainer.net.encode(),b.trainer.net.encode());assertEquals(a.phase,b.phase);assertEquals(a.offset,b.offset);
            assertTrue(++units<1000);
        }
        assertFalse(Arrays.equals(initial,a.trainer.net.encode()));assertEquals(120,a.trainer.buffer.size());
        for(int p=1;p<=10;p++){
            AtomNetTrainerR27.Sample s=new AtomNetTrainerR27.Sample(AtomNetR27.positionedInput(new double[]{.2,.7},p),p%3==0?1:0,p-1,AtomNetSessionR30.eventKey("000000001000",p));
            a.online(s,"000000001000",1000,p);b.online(s,"000000001000",1000,p);b=copy(b);
            assertArrayEquals(a.trainer.net.encode(),b.trainer.net.encode());assertEquals(a.nextPosition,b.nextPosition);
            assertEquals("000000001000",a.trainedThrough());assertEquals(1000,a.trainedThroughOrdinal());
            if(p<10){assertEquals("000000000999",a.completeIssue);assertEquals("000000001000",a.partialIssue);}
        }
        assertEquals(10,a.trainer.onlineSteps);assertEquals("000000001000",a.completeIssue);assertEquals("",a.partialIssue);
        a.trainingMs=b.trainingMs=0;a.materializeMs=b.materializeMs=0;assertArrayEquals(a.encode(),b.encode());
        try{a.online(sample(0),"000000001000",1000,1);fail("duplicate issue accepted");}catch(IllegalStateException expected){}
        System.out.println("R30_TEN_POSITION|PRETRAIN_RESUME_UNITS="+units+"|ALL_10_ONLINE_POSITIONS_RESUME_EXACTLY|BATCH_LE_64|PASS");
    }
    @Test public void periodicDiagnosticStillRollsBackWithoutRewindingLearningCursor(){
        AtomNetSessionR30 s=session();while(!s.ready())s.work(R30TenPositionTrainingTest::sample);
        byte[] checkpoint=s.trainer.net.encode();s.trainer.checkpoint=checkpoint;s.trainer.lastCheckLogloss=0;s.trainer.lastCheckSe=0;
        s.trainer.sinceCheck=10L*AtomNetTrainerR27.CHECK_EVERY-10;double lr=s.trainer.onlineLr;
        for(int p=1;p<=10;p++)s.online(new AtomNetTrainerR27.Sample(AtomNetR27.positionedInput(new double[]{.2,.7},p),0,p-1,AtomNetSessionR30.eventKey("000000001000",p)),"000000001000",1000,p);
        assertEquals("EVAL_ONLINE_HOLD",s.phase);s=copy(s);
        while(!s.ready()){assertTrue(s.work(R30TenPositionTrainingTest::sample)<=64);s=copy(s);}
        assertArrayEquals(checkpoint,s.trainer.net.encode());assertEquals(lr*.5,s.trainer.onlineLr,0);
        assertEquals("000000001000",s.completeIssue);assertEquals(10,s.trainer.onlineSteps);assertEquals(0,s.trainer.sinceCheck);
    }
    @Test public void legacyModelIsSeparateAndChecksumAndInputShapeAreEnforced(){
        AtomNetTrainerR27 legacy=new AtomNetTrainerR27(172,3);legacy.onlineLr=.0001;byte[] old=new ModelArtifactR29(legacy,new FtrlR27(7,.05,1,1e-4,1e-3),"old","000000000001").encode();
        AtomNetSessionR30 next=session();byte[] current=next.encode();assertFalse(ModelArtifactR29.ABI.equals(AtomNetSessionR30.ABI));
        assertArrayEquals(old,ModelArtifactR29.decode(old,StatsR27.sha256(old)).encode());assertEquals(12,copy(next).trainer.net.in);
        current[current.length-1]^=1;try{AtomNetSessionR30.decode(current,StatsR27.sha256(next.encode()));fail("bad checksum");}catch(IllegalArgumentException expected){}
        try{next.trainer.net.predict(new double[11]);fail("wrong shape");}catch(IllegalArgumentException expected){}
    }
}
