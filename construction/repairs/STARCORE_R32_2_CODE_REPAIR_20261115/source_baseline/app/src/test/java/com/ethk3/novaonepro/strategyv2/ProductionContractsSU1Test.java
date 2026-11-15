package com.ethk3.novaonepro.strategyv2;

import org.junit.Test;
import static org.junit.Assert.*;
import java.io.*;
import java.util.*;

public final class ProductionContractsSU1Test {
    private static int[][] history(int count){int[][] d=new int[count][10];Random r=new Random(19);for(int[] row:d){for(int i=0;i<10;i++)row[i]=i+1;for(int i=9;i>0;i--){int j=r.nextInt(i+1),v=row[i];row[i]=row[j];row[j]=v;}}return d;}
    private static BudgetTokenSU1 budget(){return new BudgetTokenSU1(60000,100000000,null);}
    @Test public void relationAndStateGradientsMatchFiniteDifferences(){
        RelationalStateInference.Weights w=RelationalTrainerSU1.initial(3,11);int[][] d=history(5);int[][] x=Arrays.copyOf(d,4);int[] y=d[4];
        RelationalTrainerSU1.Gradient g=RelationalTrainerSU1.gradient(w,x,y,budget());
        double[][][] parameters={w.e,w.u,w.r,w.wx,w.wm,w.wh,w.wo};double[][][] gradients={g.e,g.u,g.r,g.wx,g.wm,g.wh,g.wo};
        for(int i=0;i<parameters.length;i++){double old=parameters[i][0][0],epsilon=1e-5;parameters[i][0][0]=old+epsilon;double plus=RelationalTrainerSU1.gradient(w,x,y,budget()).loss;parameters[i][0][0]=old-epsilon;double minus=RelationalTrainerSU1.gradient(w,x,y,budget()).loss;parameters[i][0][0]=old;assertEquals("parameter group "+i,(plus-minus)/(2*epsilon),gradients[i][0][0],2e-7);}
        double before=g.loss;RelationalStateInference.Weights next=RelationalTrainerSU1.update(w,g,.05,1);assertTrue(RelationalTrainerSU1.gradient(next,x,y,budget()).loss<before);assertNotEquals(w.r[0][0],next.r[0][0],0);assertNotEquals(w.wh[0][0],next.wh[0][0],0);
    }
    @Test public void modelTransportRejectsTamperingAndPreservesProbabilities(){
        RelationalStateInference.Weights w=RelationalTrainerSU1.initial(4,4);String payload=RelationalModelCodecSU1.encode(w);RelationalStateInference.Weights copy=RelationalModelCodecSU1.decode(payload,HashingSU1.text(payload));
        double[][] a=RelationalStateInference.predict(w,history(12)),b=RelationalStateInference.predict(copy,history(12));for(int p=0;p<10;p++)assertArrayEquals(a[p],b[p],0);
        try{RelationalModelCodecSU1.decode(payload+"X",HashingSU1.text(payload));fail("corrupt model accepted");}catch(IllegalArgumentException expected){assertNotNull(expected.getMessage());}
    }
    @Test public void periodCheckpointReproducesUninterruptedReplay()throws Exception{
        int[][] d=history(1050);String text="ADD(EXPERT(2),XT(3))";ReplayCursorSU1 saved;
        try(ReplayCursorSU1 cursor=new ReplayCursorSU1(d,text,.25,false,800,1000)){
            try{cursor.advance(new BudgetTokenSU1(60000,60,null));fail("budget did not yield");}catch(BudgetTokenSU1.Yield expected){assertTrue(cursor.cursor<1000);}
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();cursor.write(new DataOutputStream(bytes));saved=ReplayCursorSU1.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())),d);
        }
        try(ReplayCursorSU1 resumed=saved;ReplayCursorSU1 all=new ReplayCursorSU1(d,text,.25,false,800,1000)){
            resumed.advance(budget());all.advance(budget());assertArrayEquals(all.replay.act,resumed.replay.act);assertArrayEquals(all.replay.mask,resumed.replay.mask);assertArrayEquals(all.replay.position,resumed.replay.position);
        }
    }
    @Test public void futureRowsDoNotChangeLearnedScores(){
        int[][] a=history(120),b=history(120);for(int i=100;i<120;i++){int v=b[i][0];b[i][0]=b[i][1];b[i][1]=v;}
        ProgramTreeSU1.Node program=ProgramTreeSU1.parse("DEEP(3,8,7)");double[][] first=program.score(new ProgramTreeSU1.Context(a,100)),second=program.score(new ProgramTreeSU1.Context(b,100));for(int p=0;p<10;p++)assertArrayEquals(first[p],second[p],0);
    }
    @Test public void portableImportRejectsTamperingAndAuthorityFields(){
        Map<String,Object> payload=new TreeMap<>();payload.put("format",PortableArtifactSU1.FORMAT);payload.put("program","XT(0)");payload.put("coverageHex","0x1.0p0");payload.put("marginGate",false);payload.put("datasetSha256",HashingSU1.text("data"));payload.put("trainedThroughIssue","202610210001");payload.put("numericProtocol","SU1_ATOM_CONDITION_V2_1E12");
        Map<String,Object> envelope=new TreeMap<>();envelope.put("payload",payload);envelope.put("sha256",HashingSU1.text(com.ethk3.novaonepro.r952.StrictCanonicalJson.write(payload)));
        String wire=com.ethk3.novaonepro.r952.StrictCanonicalJson.write(envelope);assertEquals("XT(0)",PortableArtifactSU1.decode(wire).program);
        try{PortableArtifactSU1.decode(wire.replace("XT(0)","XT(1)"));fail("tampered program accepted");}catch(IllegalArgumentException expected){assertNotNull(expected.getMessage());}
        payload.put("certificate","PASS");envelope.put("sha256",HashingSU1.text(com.ethk3.novaonepro.r952.StrictCanonicalJson.write(payload)));
        try{PortableArtifactSU1.decode(com.ethk3.novaonepro.r952.StrictCanonicalJson.write(envelope));fail("imported authority accepted");}catch(IllegalArgumentException expected){assertNotNull(expected.getMessage());}
    }
    @Test public void foundryCheckpointRestoresAnInterruptedAtomicScan(){
        int[][] draws=history(3400);String hash=HashingSU1.text("immutable dataset");String wire;
        try(StrategyFoundrySU1 f=new StrategyFoundrySU1(draws,hash,19,8,1)){
            try{f.next(new BudgetTokenSU1(60000,100,null));fail("expected a budget yield");}catch(BudgetTokenSU1.Yield expected){assertEquals(0,f.evaluated);}wire=f.checkpoint();
        }
        try(StrategyFoundrySU1 resumed=StrategyFoundrySU1.restore(draws,hash,wire)){assertEquals(wire,resumed.checkpoint());assertEquals(0,resumed.evaluated);}
        try{StrategyFoundrySU1.restore(draws,HashingSU1.text("other data"),wire);fail("wrong dataset resumed");}catch(IllegalArgumentException expected){assertNotNull(expected.getMessage());}
    }
    @Test public void canonicalSequenceOwnerHandlesDayAndEpochBoundaries(){
        assertEquals(1,com.ethk3.novaonepro.TargetIssueResolver.sequenceOrdinalSU1("202001010001"));
        long last=com.ethk3.novaonepro.TargetIssueResolver.sequenceOrdinalSU1("202610211440");assertEquals("202610220001",com.ethk3.novaonepro.TargetIssueResolver.fromSequenceOrdinalSU1(last+1));
        assertEquals(2,com.ethk3.novaonepro.TargetIssueResolver.firstUnseenOrdinalSU1(1577836800000L));
        com.ethk3.novaonepro.Models.Draw draw=new com.ethk3.novaonepro.Models.Draw();draw.issue="202610211440";draw.openTime=1577836800000L;for(int i=0;i<10;i++)draw.numbers[i]=i+1;
        assertEquals(last+2,com.ethk3.novaonepro.TargetIssueResolver.firstFutureOrdinalSU1(draw,draw.openTime+1000));
        assertEquals(last+5,com.ethk3.novaonepro.TargetIssueResolver.firstFutureOrdinalSU1(draw,draw.openTime+181000));
    }
}
