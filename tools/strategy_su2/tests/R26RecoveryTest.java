package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.lang.management.ManagementFactory;
import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

/** Numerical continuation, causal gap recovery and bounded consensus against independent references. */
public final class R26RecoveryTest {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static int[] draw(int t){int[] d=new int[10];for(int p=0;p<10;p++)d[p]=(p+t)%10+1;return d;}
    static ModelGenomeSU2 program(String kind){return new ModelGenomeSU2(Collections.singletonList(new ModelGenomeSU2.Member(kind,ModelEvolutionSU2.centralParams(kind),1)),"STATIC",false,"MASS",.3,.2);}
    static void equalForecast(LegacyReplayStateR26 a,LegacyReplayStateR26 b)throws Exception {
        check(a.through()==b.through()&&a.samples()==b.samples(),"cursor restored");
        for(String key:a.lives.keySet()){
            LiveStrategySU2 left=a.lives.get(key),right=b.lives.get(key);
            ModelLiveSU2.Decision x=left.decide(),y=right.decide();
            check(x.position==y.position&&x.mask==y.mask&&x.act==y.act&&Double.doubleToLongBits(x.phat)==Double.doubleToLongBits(y.phat),"forecast changed after process recovery");
            float[][] p=left.pendingTable(a.samples()),q=right.pendingTable(b.samples());
            for(int i=0;i<10;i++)check(Arrays.equals(p[i],q[i]),"complete probability table changed");
        }
    }
    static StageStrategySU2.Definition stage(String text){
        int cols=StageLayerSU2.BASE_COLS+1;
        double[][] mu=new double[1][cols],var=new double[1][cols];Arrays.fill(var[0],1);
        double[] sd=new double[cols];Arrays.fill(sd,1);
        StageLayerSU2.Layer layer=new StageLayerSU2.Layer(new StageLayerSU2.Gmm(mu,var,new double[]{1},0),new double[cols],sd,new double[1][4],new double[]{1});
        double[] seed=new double[100];Arrays.fill(seed,.3);
        return new StageStrategySU2.Definition("F30",.3,1005,Collections.singletonList(text),layer,seed);
    }
    static void legacy()throws Exception {
        String single=program("MSDIR").text();
        String committee=new CommitteeSU2(Arrays.asList(program("MSDIR"),program("HMM")),.3).text();
        StageStrategySU2.Definition def=stage(single);StageStrategySU2.register(def);
        ModelGenomeSU2 calibrated=new ModelGenomeSU2(program("MSDIR").members,"STATIC",false,"MASS",.3,.2,true);
        LegacyReplayStateR26 a=new LegacyReplayStateR26(Arrays.asList(single,committee,def.text(),calibrated.text()),1000);
        for(int t=0;t<37;t++)a.advance(draw(t));
        LegacyReplayStateR26 b=LegacyReplayStateR26.decode(a.encode());equalForecast(a,b);
        a.advance(draw(37));b.advance(draw(37));check(Arrays.equals(a.encode(),b.encode()),"next update changed after resume");
        // A missing calendar period resets temporal/gate context, retaining fitted model counts.
        byte[] learnedBefore=modelBytes(a.hub.models.values().iterator().next());
        a.onGap();check(Arrays.equals(learnedBefore,modelBytes(a.hub.models.values().iterator().next())),"gap reseeded learned MSDIR counts");
        check(!a.hub.cal.isEmpty(),"calibrated gap fixture missing");for(CalibrationSU2.Live c:a.hub.cal.values())check(c.filled==0,"gap retained raw calibration history");
        a.advance(draw(38),1042);b=LegacyReplayStateR26.decode(a.encode());equalForecast(a,b);
        check(a.samples()==39&&a.through()==1042,"ordinal incorrectly conflated with learned labels");
        a.advance(draw(39),1043);b.advance(draw(39),1043);check(Arrays.equals(a.encode(),b.encode()),"gap context lost on restart");
        try{a.advance(draw(40),1043);throw new AssertionError("duplicate label learned");}catch(IOException good){}
        // Flip the gzip CRC, rather than an arbitrary bit that may be unused deflate padding.
        byte[] bad=a.encode();bad[bad.length-8]^=1;
        try{LegacyReplayStateR26.decode(bad);throw new AssertionError("damaged checkpoint accepted");}catch(IOException good){}
        System.out.println("LEGACY_PROGRAM_COMMITTEE_STAGE_GAP_RESUME_PASS");
    }
    static byte[] modelBytes(OnlineModelsSU2.Model model)throws Exception {ByteArrayOutputStream b=new ByteArrayOutputStream();model.write(new DataOutputStream(b));return b.toByteArray();}
    static void continuation()throws Exception {
        AtomicBoolean preempt=new AtomicBoolean(true);AtomicInteger done=new AtomicInteger(),resume=new AtomicInteger();
        try(CooperativeContinuationR26<Integer> task=new CooperativeContinuationR26<>("continuation-reference",()->{
            BudgetTokenSU1 activeBudget=new BudgetTokenSU1(100,100000,()->false);
            for(int n=0;n<8;n++){
                try(CooperativeContinuationR26.Critical critical=CooperativeContinuationR26.critical()){
                    CooperativeContinuationR26.poll();done.incrementAndGet();
                }
                activeBudget.checkpointOrYield();
            }
            return done.get();
        },preempt::get,resume::incrementAndGet,()->ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime())){
            CooperativeContinuationR26.Slice<Integer> s=task.slice(10);check(done.get()==1&&!s.done,"parked inside critical section or ignored preemption");
            long start=System.nanoTime();int quanta=1;
            while(!s.done&&quanta<12){Thread.sleep(35);s=task.slice(10);quanta++;}
            check(s.done&&s.error==null&&s.result==8,"park time consumed numerical budget: "+s.error);
            check(System.nanoTime()-start>100000000L&&resume.get()>=8,"test did not cross wall deadline");
        }
        CooperativeContinuationR26<Void> cancelled=new CooperativeContinuationR26<>("continuation-cancel",()->{for(;;)CooperativeContinuationR26.poll();},()->false,null,()->ManagementFactory.getThreadMXBean().getCurrentThreadCpuTime(),"SYNTHETIC_CANCEL_OWNER");
        cancelled.slice(5);cancelled.close();long end=System.nanoTime()+1000000000L;while(!cancelled.isDone()&&System.nanoTime()<end)Thread.yield();check(cancelled.isDone(),"cancelled helper kept computing");check(CooperativeContinuationR26.awaitOwnerStopped("SYNTHETIC_CANCEL_OWNER",1000),"owned numerical worker was released before it stopped");
        System.out.println("COOPERATIVE_PREEMPT_CRITICAL_PAUSE_CLOCK_CANCEL_PASS");
    }
    static void consensus(){
        int T=CalibrationSU2.START+CalibrationSU2.REFIT+7;int[][] draws=new int[T][];for(int t=0;t<T;t++)draws[t]=draw(t);
        ModelReplaySU2.Library lib=new ModelReplaySU2.Library(draws,4L<<20);ModelEvolutionSU2 evo=new ModelEvolutionSU2(lib,1,20,100,10);
        List<ModelGenomeSU2> programs=Arrays.asList(program("MSDIR"),program("HMM"));int M=programs.size();float[][][][] tables=new float[M][][][];double[][] cumulative=new double[M][];
        for(int m=0;m<M;m++){tables[m]=ModelReplaySU2.pooledTable(programs.get(m),lib);cumulative[m]=evo.cum(programs.get(m));}
        float[][][] expected=new float[T][10][10];for(int t=0;t<CalibrationSU2.START;t++)for(float[] row:expected[t])Arrays.fill(row,.1f);
        for(int from=CalibrationSU2.START;from<T;from+=CalibrationSU2.REFIT){double[] weights=new double[M];double max=Double.NEGATIVE_INFINITY;for(int m=0;m<M;m++){weights[m]=cumulative[m][from]-cumulative[m][Math.max(0,from-CalibrationSU2.WINDOW)];max=Math.max(max,weights[m]);}for(int m=0;m<M;m++)weights[m]=Math.exp(weights[m]-max);double z=ModelOpsSU2.pairwise(weights,0,M);for(int m=0;m<M;m++)weights[m]/=z;
            for(int t=from;t<Math.min(T,from+CalibrationSU2.REFIT);t++)for(int p=0;p<10;p++)for(int n=0;n<10;n++){double sum=0;for(int m=0;m<M;m++)sum+=weights[m]*tables[m][t][p][n];expected[t][p][n]=(float)sum;}
        }
        evo.buildConsensus(programs);for(int t=0;t<T;t++)for(int p=0;p<10;p++)check(Arrays.equals(expected[t][p],evo.Q[t][p]),"streaming consensus changed arithmetic");
        System.out.println("STREAMING_CONSENSUS_BIT_PARITY_PASS");
    }
    static void closedIdentity()throws Exception {
        ModelGenomeSU2 g=program("MSDIR").bindBirth(PolicyIdentityR26.sha256("snapshot"),1000);StageStrategySU2.Definition def=stage(g.text());
        String text=def.text(),key=def.key();byte[] original=def.bytes(),external=def.bytes();external[0]^=1;
        check(text.equals(def.text())&&key.equals(def.key())&&Arrays.equals(original,def.bytes()),"fitted policy bytes mutable");
        StageStrategySU2.Definition roundtrip=StageStrategySU2.Definition.read(original);check(roundtrip.text().equals(text)&&StageStrategySU2.currentPolicy(text),"fitted ABI identity");
        float[][] raw=new float[10][10];for(float[] row:raw){Arrays.fill(row,.05f);row[0]=.55f;}float[][] legal=LegalReadoutR26.project(raw);
        for(int i=0;i<10;i++){double row=0,col=0;for(int j=0;j<10;j++){row+=legal[i][j];col+=legal[j][i];}check(Math.abs(row-1)<1e-5&&Math.abs(col-1)<1e-5,"illegal composite probability");}
        for(int i=0;i<10;i++)check(Arrays.equals(legal[i],LegalReadoutR26.project(legal)[i]),"legal head applied twice");
        System.out.println("CLOSED_LEGAL_READOUT_FITTED_IDENTITY_PASS");
    }
    static void cachedBootstrap()throws Exception {
        int n=180;int[][] draws=new int[n][];long[] ordinals=new long[n];for(int t=0;t<n;t++){draws[t]=draw(t);ordinals[t]=1000+t;}
        ModelGenomeSU2 g=program("MSDIR").bindBirth(PolicyIdentityR26.sha256("cached-prefix"),ordinals[n-1]);
        CommitteeSU2 committee=new CommitteeSU2(Arrays.asList(g,program("HMM").bindBirth(PolicyIdentityR26.sha256("cached-prefix"),ordinals[n-1])),.3);
        StageStrategySU2.Definition stage=stage(committee.text());StageStrategySU2.register(stage);
        for(String text:Arrays.asList(g.text(),committee.text(),stage.text())){
            LegacyReplayStateR26 reference=new LegacyReplayStateR26(Collections.singletonList(text),1000);for(int t=0;t<n;t++)reference.advance(draws[t],ordinals[t]);
            LegacyReplayStateR26 cached=LegacyBootstrapR26.build(text,ordinals,new ModelReplaySU2.Library(draws,4L<<20));check(Arrays.equals(reference.encode(),cached.encode()),"cached bootstrap did not restore full original state");equalForecast(reference,cached);
            reference.advance(draw(n),ordinals[n-1]+1);cached.advance(draw(n),ordinals[n-1]+1);check(Arrays.equals(reference.encode(),cached.encode()),"cached bootstrap next-step divergence");
            PolicyRuntimeR26 runtime=PolicyRuntimeR26.decode(text,cached.encode(),LiveStateR26.hash(cached.encode()));check(runtime.forecast().mask==cached.lives.values().iterator().next().decide().mask,"private composite wire dispatch");
        }
        System.out.println("CACHED_PROGRAM_COMMITTEE_STAGE_BOOTSTRAP_NEXT_STEP_PARITY_PASS");
    }
    static void calibrationGap()throws Exception {
        CalibrationSU2.Live c=new CalibrationSU2.Live();c.t=CalibrationSU2.START;c.beta=.625;c.filled=1;c.ring[(int)c.t-1]=CalibrationSU2.uniform();c.ringY[(int)c.t-1]=new byte[10];
        c.resetTemporalContextR26();check(c.t==CalibrationSU2.START&&c.beta==.625&&c.filled==0,"calibration reset changed fitted parameters or clock");
        float[][] raw=CalibrationSU2.uniform();raw[0][0]=.55f;for(int n=1;n<10;n++)raw[0][n]=.05f;
        float[][] forecast=c.predict(raw);check(Float.isFinite(forecast[0][0])&&forecast[0][0]>.1,"empty gap ring fitted NaN or reset temperature");
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();c.write(new DataOutputStream(bytes));CalibrationSU2.Live restored=new CalibrationSU2.Live();restored.read(new DataInputStream(new ByteArrayInputStream(bytes.toByteArray())));
        check(Arrays.equals(forecast[0],restored.predict(raw)[0]),"gap calibration changed after restart");restored.settle(raw,draw(0));check(restored.filled==1&&restored.t==CalibrationSU2.START+1&&restored.beta==.625,"post-gap calibration did not resume causally");
        System.out.println("CALIBRATION_GAP_PARAMETER_CLOCK_RETENTION_EMPTY_REFIT_RESUME_PASS");
    }
    public static void main(String[] args)throws Exception {continuation();legacy();calibrationGap();consensus();closedIdentity();cachedBootstrap();System.out.println("R26_RECOVERY_PASS");}
}
