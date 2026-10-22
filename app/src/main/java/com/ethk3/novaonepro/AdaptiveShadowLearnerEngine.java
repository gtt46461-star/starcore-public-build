package com.ethk3.novaonepro;

import java.util.*;

/** Bounded post-settlement research FTRL. Replay buffers and weights share one artifact checkpoint. */
public final class AdaptiveShadowLearnerEngine {
    public static final double ALPHA=.05,BETA=1,LAMBDA_1=.001,LAMBDA_2=.01;
    static final int RECENT=32,LONG_TERM=64,MAX_STEPS=5;
    static final double GRADIENT_NORM=1,FEATURE_BOUND=16,ROLLBACK_LOSS=.05;
    static final long REPLAY_SEED=932017;
    static final double[] REPLAY_CONTRACT={1,RECENT,LONG_TERM,MAX_STEPS,GRADIENT_NORM,FEATURE_BOUND,ROLLBACK_LOSS,REPLAY_SEED};
    public static final String UPDATE_RULE="R932_FTRL_DUAL_MEMORY_V1_FIXED_32_64_FIVE_STEPS";
    private double[] z=new double[0],n=new double[0];
    private long updates,optimizerSteps,rollbackCount;
    private String trainedUntilIssue="";
    private double calibrationBias,driftEma;
    private boolean versioned;
    private final ArrayList<OosRow> recent=new ArrayList<>(),longTerm=new ArrayList<>();

    public synchronized Update updateAfterSettlement(OosRow row){
        if(row==null||!row.valid())return new Update(false,"INVALID_OOS",trainedUntilIssue,updates);
        if(RealtimePressureGateR811.hasActiveCriticalWork())return new Update(false,"YIELD_REALTIME",trainedUntilIssue,updates);
        if(TargetIssueResolver.isValid(trainedUntilIssue)&&row.targetIssue.compareTo(trainedUntilIssue)<=0)return new Update(false,"REPLAY_IGNORED",trainedUntilIssue,updates);
        // Compute separately: yield, shape rejection or numerical error leaves this checkpoint intact.
        AdaptiveShadowLearnerEngine next=copy();next.ensure(row.features.length);next.versioned=true;
        ArrayList<OosRow> replay=next.replayRows(row.targetIssue);
        double before=next.loss(replay),error=next.predict(row.features)-row.label;
        next.step(row);for(OosRow old:replay){ResearchWorkBudgetR927.checkpoint();next.step(old);}
        boolean rollback=!replay.isEmpty()&&next.loss(replay)>before+ROLLBACK_LOSS;
        if(rollback){next.z=z.length==0?new double[row.features.length]:z.clone();next.n=n.length==0?new double[row.features.length]:n.clone();next.calibrationBias=calibrationBias;next.rollbackCount++;}
        next.driftEma=.98*driftEma+.02*Math.abs(error);next.trainedUntilIssue=row.targetIssue;next.updates++;
        next.recent.add(row.copy());if(next.recent.size()>RECENT)next.recent.remove(0);
        next.longTerm.add(row.copy());Collections.sort(next.longTerm,(a,b)->priority(a).compareTo(priority(b)));
        if(next.longTerm.size()>LONG_TERM)next.longTerm.remove(next.longTerm.size()-1);
        adopt(next);return new Update(true,rollback?"RESEARCH_REPLAY_REGRESSION_ROLLBACK":"RESEARCH_DUAL_MEMORY_UPDATED",trainedUntilIssue,updates);
    }
    private ArrayList<OosRow> replayRows(String target){
        ArrayList<OosRow> out=new ArrayList<>();HashSet<String> seen=new HashSet<>();
        for(List<OosRow> memory:Arrays.asList(recent,longTerm)){
            int added=0;for(int offset=0;offset<memory.size()&&added<2;offset++){
                OosRow row=memory.get((int)Math.floorMod(updates*7+offset,memory.size()));
                if(row.targetIssue.compareTo(target)>=0)throw new IllegalStateException("R932_REPLAY_FUTURE_ROW");
                if(seen.add(row.targetIssue)){out.add(row);added++;}
            }
        }
        if(out.size()+1>MAX_STEPS)throw new IllegalStateException("R932_REPLAY_STEP_BUDGET");return out;
    }
    private void step(OosRow row){
        double g=predict(row.features)-row.label,norm=0;double[] gradient=new double[z.length];
        for(int i=0;i<z.length;i++){gradient[i]=g*clip(row.features[i],-FEATURE_BOUND,FEATURE_BOUND);norm+=gradient[i]*gradient[i];}
        double scale=Math.max(1,Math.sqrt(norm)/GRADIENT_NORM);
        for(int i=0;i<z.length;i++){
            double gi=gradient[i]/scale,w=weight(i),sigma=(Math.sqrt(n[i]+gi*gi)-Math.sqrt(n[i]))/ALPHA;
            z[i]+=gi-sigma*w;n[i]+=gi*gi;
            if(!Double.isFinite(z[i])||!Double.isFinite(n[i])||Math.abs(z[i])>1e6||n[i]>1e12)throw new IllegalStateException("R932_FTRL_NUMERICAL_BOUND");
        }
        calibrationBias=clip(.99*calibrationBias-.01*g,-1,1);optimizerSteps++;
    }
    private double loss(List<OosRow> rows){double sum=0;for(OosRow row:rows){double e=predict(row.features)-row.label;sum+=e*e;}return rows.isEmpty()?0:sum/rows.size();}
    private static String priority(OosRow row){return V51ChecksumEngine.sha256(REPLAY_SEED+"|"+row.targetIssue);}
    private AdaptiveShadowLearnerEngine copy(){AdaptiveShadowLearnerEngine c=new AdaptiveShadowLearnerEngine();c.adopt(this);return c;}
    private void adopt(AdaptiveShadowLearnerEngine c){z=c.z.clone();n=c.n.clone();updates=c.updates;optimizerSteps=c.optimizerSteps;rollbackCount=c.rollbackCount;trainedUntilIssue=c.trainedUntilIssue;calibrationBias=c.calibrationBias;driftEma=c.driftEma;versioned=c.versioned;recent.clear();for(OosRow r:c.recent)recent.add(r.copy());longTerm.clear();for(OosRow r:c.longTerm)longTerm.add(r.copy());}
    public static AdaptiveShadowLearnerEngine restore(ResearchArtifactCodecR811.Artifact artifact){
        AdaptiveShadowLearnerEngine out=new AdaptiveShadowLearnerEngine();if(artifact==null)return out;
        artifact.validate();if(!"FTRL_SHADOW".equals(artifact.type))throw new IllegalArgumentException("R811_FTRL_ARTIFACT_TYPE");
        double[] z=artifact.fields.get("z"),n=artifact.fields.get("n"),meta=artifact.fields.get("meta"),params=artifact.fields.get("params");
        if(z==null||n==null||z.length==0||z.length>64||z.length!=n.length||meta==null||meta.length!=3)throw new IllegalArgumentException("R811_FTRL_ARTIFACT_SHAPE");
        if(!Arrays.equals(params,new double[]{ALPHA,BETA,LAMBDA_1,LAMBDA_2}))throw new IllegalArgumentException("R811_FTRL_ARTIFACT_PARAMS");
        if(meta[0]<0||meta[0]!=Math.floor(meta[0])||meta[0]>1e9||Math.abs(meta[1])>1||meta[2]<0||meta[2]>1||!TargetIssueResolver.isValid(artifact.trainedUntilIssue))throw new IllegalArgumentException("R932_FTRL_CHECKPOINT_METADATA");
        for(int i=0;i<z.length;i++)if(Math.abs(z[i])>1e6||n[i]<0||n[i]>1e12)throw new IllegalArgumentException("R932_FTRL_CHECKPOINT_WEIGHTS");
        out.z=z.clone();out.n=n.clone();out.updates=(long)meta[0];out.calibrationBias=meta[1];out.driftEma=meta[2];out.trainedUntilIssue=artifact.trainedUntilIssue;
        if(artifact.fields.containsKey("replay_contract")){
            if(!Arrays.equals(artifact.fields.get("replay_contract"),REPLAY_CONTRACT))throw new IllegalArgumentException("R932_REPLAY_RULE_CHANGED");out.versioned=true;
            double[] counters=artifact.fields.get("replay_counters");if(counters==null||counters.length!=2||counters[0]<0||counters[0]>out.updates*MAX_STEPS||counters[1]<0||counters[1]>out.updates||counters[0]!=Math.floor(counters[0])||counters[1]!=Math.floor(counters[1]))throw new IllegalArgumentException("R932_REPLAY_COUNTERS");
            out.optimizerSteps=(long)counters[0];out.rollbackCount=(long)counters[1];
            out.decodeRows(artifact.fields.get("replay_recent"),out.recent,RECENT,true);out.decodeRows(artifact.fields.get("replay_long"),out.longTerm,LONG_TERM,false);
        }else if(artifact.fields.containsKey("replay_recent")||artifact.fields.containsKey("replay_long")||artifact.fields.containsKey("replay_counters"))throw new IllegalArgumentException("R932_REPLAY_UNVERSIONED_FIELDS");
        return out;
    }
    private void decodeRows(double[] values,List<OosRow> into,int limit,boolean ordered){
        int width=z.length+4;if(values==null||values.length%width!=0||values.length/width>limit)throw new IllegalArgumentException("R932_REPLAY_BUFFER_SHAPE");
        HashSet<String> issues=new HashSet<>();String previous="";
        for(int i=0;i<values.length;i+=width){
            String issue=Long.toString((long)values[i]);double[] x=Arrays.copyOfRange(values,i+4,i+width);
            if(values[i]!=(long)values[i]||values[i+1]!=(int)values[i+1]||values[i+2]!=(long)values[i+2]||values[i+3]!=(long)values[i+3])throw new IllegalArgumentException("R932_REPLAY_INTEGER_METADATA");
            OosRow r=new OosRow(issue,(long)values[i+2],(long)values[i+3],true,(int)values[i+1],x);
            if(!r.valid()||issue.compareTo(trainedUntilIssue)>0||!issues.add(issue))throw new IllegalArgumentException("R932_REPLAY_PIT_OR_DUPLICATE");
            String order=ordered?issue:priority(r);if(!previous.isEmpty()&&previous.compareTo(order)>=0)throw new IllegalArgumentException("R932_REPLAY_BUFFER_ORDER");previous=order;into.add(r);
        }
    }
    private double[] encodeRows(List<OosRow> rows){int width=z.length+4;double[] out=new double[rows.size()*width];int i=0;for(OosRow row:rows){out[i++]=Long.parseLong(row.targetIssue);out[i++]=row.label;out[i++]=row.frozenAt;out[i++]=row.drawOpenTime;for(double x:row.features)out[i++]=x;}return out;}
    public synchronized ResearchArtifactCodecR811.Artifact snapshotArtifact(){
        LinkedHashMap<String,double[]> fields=new LinkedHashMap<>();fields.put("z",z.clone());fields.put("n",n.clone());fields.put("meta",new double[]{updates,calibrationBias,driftEma});fields.put("params",new double[]{ALPHA,BETA,LAMBDA_1,LAMBDA_2});
        if(versioned){fields.put("replay_contract",REPLAY_CONTRACT.clone());fields.put("replay_counters",new double[]{optimizerSteps,rollbackCount});fields.put("replay_recent",encodeRows(recent));fields.put("replay_long",encodeRows(longTerm));}
        return new ResearchArtifactCodecR811.Artifact("FTRL_SHADOW",trainedUntilIssue,fields);
    }
    public synchronized ResearchArtifactCodecR811.Artifact snapshotResearchArtifact(){return snapshotArtifact();}
    public synchronized double predictResearch(double[] features){if(features==null||features.length==0||z.length!=features.length)throw new IllegalArgumentException("R811_FTRL_PREDICT_SHAPE");for(double x:features)if(!Double.isFinite(x))throw new IllegalArgumentException("R932_FTRL_MISSING_INPUT");return predict(features);}
    public synchronized double driftUpdate(double absoluteError){if(!Double.isFinite(absoluteError)||absoluteError<0||absoluteError>1)throw new IllegalArgumentException("R932_DRIFT_INPUT");driftEma=.98*driftEma+.02*absoluteError;return driftEma;}
    private double predict(double[] x){double v=calibrationBias;for(int i=0;i<x.length;i++)v+=weight(i)*(versioned?clip(x[i],-FEATURE_BOUND,FEATURE_BOUND):x[i]);return v>=0?1/(1+Math.exp(-Math.min(60,v))):Math.exp(Math.max(-60,v))/(1+Math.exp(Math.max(-60,v)));}
    private double weight(int i){if(Math.abs(z[i])<=LAMBDA_1)return 0;double w=-(z[i]-(z[i]<0?-1:1)*LAMBDA_1)/((BETA+Math.sqrt(n[i]))/ALPHA+LAMBDA_2);return versioned?clip(w,-16,16):w;}
    private void ensure(int size){if(size<1||size>64)throw new IllegalArgumentException("R932_FTRL_FEATURE_LIMIT");if(z.length==size)return;if(updates!=0)throw new IllegalStateException("R811_LEARNER_FEATURE_DRIFT");z=new double[size];n=new double[size];}
    private static double clip(double x,double lo,double hi){return Math.max(lo,Math.min(hi,x));}
    public static final class OosRow {
        public final String targetIssue;public final long frozenAt,drawOpenTime;public final boolean strictOos;public final int label;public final double[] features;
        public OosRow(String issue,long frozen,long open,boolean strict,int label,double[] x){targetIssue=issue==null?"":issue;frozenAt=frozen;drawOpenTime=open;strictOos=strict;this.label=label;features=x==null?new double[0]:x.clone();}
        OosRow copy(){return new OosRow(targetIssue,frozenAt,drawOpenTime,strictOos,label,features);}
        boolean valid(){if(!TargetIssueResolver.isValid(targetIssue)||!strictOos||frozenAt<=0||drawOpenTime<=frozenAt||(label!=0&&label!=1)||features.length==0||features.length>64)return false;for(double x:features)if(!Double.isFinite(x))return false;return true;}
    }
    public static final class Update {public final boolean updated;public final String status,trainedUntilIssue;public final long updates;Update(boolean updated,String status,String issue,long n){this.updated=updated;this.status=status;trainedUntilIssue=issue;updates=n;}}
}
