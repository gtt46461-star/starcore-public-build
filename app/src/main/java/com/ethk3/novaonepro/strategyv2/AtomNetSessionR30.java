package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.util.*;

/** Ten-position learner on the existing task/writer. Each unit is at most one original 64-sample batch.
 * Every phase, position cursor, shuffle, Adam, dropout RNG and replay sample survives a lossless checkpoint.
 * The R29 codec is an inner optimizer container only; this outer ABI/owner never overwrites a legacy model. */
public final class AtomNetSessionR30 {
    public static final String ABI = AtomNetR27.POSITION_INPUT_ABI, OWNER = "ATOMNET_TEN_R30";
    public interface Samples { AtomNetTrainerR27.Sample get(int sample); }
    public final String datasetHash;
    public final int trainCount, selectCount, holdoutCount, input;
    public final long observedThroughOrdinal;
    public final String observedThrough;
    public AtomNetTrainerR27 trainer;
    public FtrlR27 ftrl;
    public String phase = "TRAIN", completeIssue = "", partialIssue = "";
    public int nextPosition = 1, epoch, offset, step, bad, fillOffset;
    public long onlineObservedOrdinal, pretrainSamples, materializedSamples, materializeMs, trainingMs, units;
    public int cachePeriod = 24;
    public double best = Double.POSITIVE_INFINITY, lastLr = AtomNetTrainerR27.LR0;
    int[] order;
    byte[] bestNet;
    StatsR27.Moments loss = new StatsR27.Moments(); double brier;
    public final List<AtomNetTrainerR27.Epoch> receipts = new ArrayList<>();

    public AtomNetSessionR30(String dataset, String through, long ordinal, int in, int train, int select, int holdout) {
        if (train <= 0 || select <= 0 || holdout < 0) throw new IllegalArgumentException("R30_TRAIN_SPLITS");
        datasetHash=dataset; observedThrough=through; observedThroughOrdinal=ordinal; input=in;
        trainCount=train; selectCount=select; holdoutCount=holdout;
        trainer=new AtomNetTrainerR27(in,27); ftrl=new FtrlR27(7,.05,1,1e-4,1e-3);
        bestNet=trainer.net.encode();
    }
    private void shuffle() {
        order = new int[trainCount]; for(int i=0;i<order.length;i++) order[i]=i;
        for(int i=order.length-1;i>0;i--) {int j=trainer.rng.nextInt(i+1),v=order[i];order[i]=order[j];order[j]=v;}
    }
    /** @return actual samples consumed; bounded by the original batch budget. */
    public int work(Samples samples) {
        receipts.clear(); long start=System.nanoTime(); int n=0;
        if ("TRAIN".equals(phase)) {
            if(order==null) shuffle();
            n=Math.min(AtomNetTrainerR27.BATCH,trainCount-offset);
            double[][] x=new double[n][];int[] y=new int[n],c=new int[n];
            for(int k=0;k<n;k++){AtomNetTrainerR27.Sample s=samples.get(order[offset+k]);x[k]=s.x;y[k]=s.y;c[k]=s.cls;if(epoch==0)ftrl.update(Arrays.copyOf(s.x,7),s.y);}
            int total=AtomNetTrainerR27.MAX_EPOCHS*((trainCount+63)/64);
            lastLr=AtomNetTrainerR27.LR_MIN+.5*(AtomNetTrainerR27.LR0-AtomNetTrainerR27.LR_MIN)*(1+Math.cos(Math.PI*step/(double)total));
            trainer.net.trainBatch(x,y,c,lastLr,trainer.rng);step++;offset+=n;pretrainSamples+=n;
            if(offset==trainCount) transition("EVAL_TRAIN");
        } else if(phase.startsWith("EVAL_")) {
            int from="EVAL_TRAIN".equals(phase)?0:trainCount;
            int count="EVAL_TRAIN".equals(phase)?trainCount:selectCount;
            if("EVAL_HOLD".equals(phase)||"EVAL_ONLINE_HOLD".equals(phase)){from+=selectCount;count=holdoutCount;}
            n=Math.min(64,count-offset);
            for(int k=0;k<n;k++) {
                AtomNetTrainerR27.Sample s=samples.get(from+offset+k);double p=trainer.net.predict(s.x);
                if("EVAL_SELECT".equals(phase)&&epoch==0)ftrl.update(Arrays.copyOf(s.x,7),s.y);
                loss.add(-Math.log(Math.max(1e-12,s.y==1?p:1-p)));brier+=(p-s.y)*(p-s.y);
            }
            offset+=n;
            if(offset==count) finishEvaluation();
        } else if("FILL".equals(phase)) {
            int from=Math.max(0,trainCount+selectCount-AtomNetTrainerR27.REPLAY);
            n=Math.min(64,trainCount+selectCount-from-fillOffset);
            for(int k=0;k<n;k++)trainer.remember(samples.get(from+fillOffset+k));fillOffset+=n;
            if(from+fillOffset==trainCount+selectCount){phase="ONLINE";completeIssue=observedThrough;onlineObservedOrdinal=observedThroughOrdinal;}
        }
        trainingMs+=(System.nanoTime()-start)/1_000_000L;units++;return n;
    }
    private void finishEvaluation() {
        String split="EVAL_TRAIN".equals(phase)?SplitR27.TRAIN:"EVAL_SELECT".equals(phase)?SplitR27.SELECT:SplitR27.HOLDOUT;
        double mean=loss.n==0?Double.NaN:loss.mean,br=loss.n==0?Double.NaN:brier/loss.n;
        receipts.add(new AtomNetTrainerR27.Epoch(trainer.nextEpochReceipt++,split,mean,br));
        if("EVAL_TRAIN".equals(phase)) transition("EVAL_SELECT");
        else if("EVAL_SELECT".equals(phase)) {
            if(mean<best-1e-6){best=mean;bad=0;bestNet=trainer.net.encode();}else bad++;
            epoch++;
            if(epoch>=AtomNetTrainerR27.MAX_EPOCHS||bad>=AtomNetTrainerR27.PATIENCE){
                trainer.net=AtomNetR27.decode(bestNet);trainer.onlineLr=lastLr*.1;trainer.checkpoint=trainer.net.encode();
                transition(holdoutCount>0?"EVAL_HOLD":"FILL");
            }else{order=null;transition("TRAIN");}
        } else if("EVAL_ONLINE_HOLD".equals(phase)) {
            boolean degrade=Double.isFinite(trainer.lastCheckLogloss)&&mean>trainer.lastCheckLogloss+2*Math.max(loss.se(),trainer.lastCheckSe);
            if(degrade){trainer.net=AtomNetR27.decode(trainer.checkpoint);trainer.onlineLr*=.5;}
            else{trainer.lastCheckLogloss=mean;trainer.lastCheckSe=loss.se();trainer.checkpoint=trainer.net.encode();}
            trainer.sinceCheck=0;transition("ONLINE");
        } else {
            trainer.lastCheckLogloss=mean;trainer.lastCheckSe=loss.se();trainer.sinceCheck=0;
            transition("FILL");
        }
    }
    private void transition(String next){phase=next;offset=0;loss=new StatsR27.Moments();brier=0;}

    /** Exactly one new position plus the original 63 replay samples. Base issue and position cursor commit together. */
    public void online(AtomNetTrainerR27.Sample sample,String issue,long ordinal,int position) {
        if(!"ONLINE".equals(phase)||position!=nextPosition||position<1||position>10
                ||(!partialIssue.isEmpty()&&!partialIssue.equals(issue))||issue.compareTo(completeIssue)<=0
                ||!sample.issue.equals(eventKey(issue,position)))throw new IllegalStateException("R30_POSITION_CURSOR");
        trainer.online(sample);ftrl.update(Arrays.copyOf(sample.x,7),sample.y);
        partialIssue=issue;onlineObservedOrdinal=ordinal;nextPosition++;
        if(nextPosition==11){completeIssue=issue;partialIssue="";nextPosition=1;
            if(holdoutCount>0&&trainer.sinceCheck>=10L*AtomNetTrainerR27.CHECK_EVERY)transition("EVAL_ONLINE_HOLD");
        }
    }
    public static String eventKey(String issue,int position){return issue+"@P"+(position<10?"0":"")+position;}
    public String trainedThrough(){return partialIssue.isEmpty()?completeIssue:partialIssue;}
    public long trainedThroughOrdinal(){return Math.max(observedThroughOrdinal,onlineObservedOrdinal);}
    public boolean ready(){return "ONLINE".equals(phase);}
    public Map<String,Object> report(){
        Map<String,Object> r=new LinkedHashMap<>();r.put("abi",ABI);r.put("owner",OWNER);r.put("datasetHash",datasetHash);r.put("phase",phase);
        r.put("samplesPerEligiblePeriod",10L);r.put("maxTrainingSamplesPerSlice",64L);r.put("cachePeriod",(long)cachePeriod);
        r.put("materializedSamples",materializedSamples);r.put("materializeMs",materializeMs);
        r.put("materializeMsPerPeriod",materializedSamples>0?materializeMs/(materializedSamples/10.0):null);
        r.put("samplesPerSecond",materializeMs>0?materializedSamples*1000.0/materializeMs:null);
        r.put("trainingSamplesProcessed",pretrainSamples);r.put("trainingMs",trainingMs);r.put("epoch",(long)epoch);r.put("phaseOffset",(long)offset);
        r.put("onlineSamples",trainer.onlineSteps);r.put("completeIssue",completeIssue);r.put("partialIssue",partialIssue);r.put("nextPosition",(long)nextPosition);
        r.put("trainedThrough",completeIssue.isEmpty()?observedThrough:trainedThrough());r.put("trainedThroughOrdinal",trainedThroughOrdinal());
        r.put("status",materializedSamples>0?"MEASURED":"INSUFFICIENT");r.put("quality","NO_NATURAL_QUALITY_CLAIM");return r;
    }
    public byte[] encode(){
        try{
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream o=new DataOutputStream(bytes);
            o.writeInt(0x30A70001);o.writeUTF(ABI);o.writeUTF(datasetHash);o.writeUTF(observedThrough);o.writeLong(observedThroughOrdinal);
            for(int v:new int[]{input,trainCount,selectCount,holdoutCount,cachePeriod,epoch,offset,step,bad,fillOffset,nextPosition})o.writeInt(v);
            o.writeUTF(phase);o.writeUTF(completeIssue);o.writeUTF(partialIssue);
            for(long v:new long[]{onlineObservedOrdinal,pretrainSamples,materializedSamples,materializeMs,trainingMs,units,loss.n})o.writeLong(v);
            for(double v:new double[]{best,lastLr,loss.mean,loss.m2,brier})o.writeDouble(v);
            o.writeInt(order==null?0:order.length);if(order!=null)for(int v:order)o.writeInt(v);
            ModelArtifactR29.blob(o,bestNet);ModelArtifactR29.blob(o,new ModelArtifactR29(trainer,ftrl,datasetHash,completeIssue.isEmpty()?observedThrough:trainedThrough()).encode());
            o.close();return bytes.toByteArray();
        }catch(IOException e){throw new IllegalStateException("R30_MODEL_ENCODE",e);}
    }
    public static AtomNetSessionR30 decode(byte[] bytes,String hash){
        if(bytes==null||bytes.length>64<<20||!StatsR27.sha256(bytes).equals(hash))throw new IllegalArgumentException("R30_MODEL_CHECKSUM");
        try{
            DataInputStream d=new DataInputStream(new ByteArrayInputStream(bytes));if(d.readInt()!=0x30A70001||!ABI.equals(d.readUTF()))throw new IOException("ABI");
            String ds=d.readUTF(),through=d.readUTF();long ordinal=d.readLong();int[] v=new int[11];for(int i=0;i<v.length;i++)v[i]=d.readInt();
            if(v[0]<11||v[0]>4096||v[1]<=0||v[1]>1_000_000||v[2]<=0||v[2]>1_000_000||v[3]<0||v[3]>1_000_000)throw new IOException("shape");
            AtomNetSessionR30 s=new AtomNetSessionR30(ds,through,ordinal,v[0],v[1],v[2],v[3]);
            s.cachePeriod=v[4];s.epoch=v[5];s.offset=v[6];s.step=v[7];s.bad=v[8];s.fillOffset=v[9];s.nextPosition=v[10];
            s.phase=d.readUTF();s.completeIssue=d.readUTF();s.partialIssue=d.readUTF();
            s.onlineObservedOrdinal=d.readLong();s.pretrainSamples=d.readLong();s.materializedSamples=d.readLong();s.materializeMs=d.readLong();s.trainingMs=d.readLong();s.units=d.readLong();s.loss.n=d.readLong();
            s.best=d.readDouble();s.lastLr=d.readDouble();s.loss.mean=d.readDouble();s.loss.m2=d.readDouble();s.brier=d.readDouble();
            int n=d.readInt();if(n!=0&&n!=s.trainCount)throw new IOException("shuffle");if(n>0){s.order=new int[n];boolean[] seen=new boolean[n];for(int i=0;i<n;i++){int x=d.readInt();if(x<0||x>=n||seen[x])throw new IOException("shuffle index");seen[x]=true;s.order[i]=x;}}
            s.bestNet=ModelArtifactR29.blob(d);byte[] inner=ModelArtifactR29.blob(d);ModelArtifactR29 a=ModelArtifactR29.decode(inner,StatsR27.sha256(inner));s.trainer=a.trainer;s.ftrl=a.ftrl;
            if(d.available()!=0||s.trainer.net.in!=s.input||s.nextPosition<1||s.nextPosition>10||s.offset<0||s.epoch<0||s.epoch>20||!Arrays.asList("TRAIN","EVAL_TRAIN","EVAL_SELECT","EVAL_HOLD","EVAL_ONLINE_HOLD","FILL","ONLINE").contains(s.phase))throw new IOException("cursor");
            return s;
        }catch(IOException|RuntimeException e){throw new IllegalArgumentException("R30_MODEL_DECODE",e);}
    }
}
