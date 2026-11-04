package com.ethk3.novaonepro.strategyv2;
import java.io.*;
import java.util.*;
/** Causal filtered HMM experts. Explicit graph variants bind their state count and transition-memory cut. */
public final class FamilyF08KernelR26 implements TemporalModelR26,StatisticalTrainingDiagnosticsR26 {
    private static final int MAGIC=0x26080001;
    private final int states;private final boolean transitionMemory,legacyCodec;
    private OnlineModelsSU2.Hmm model;private long updates;private boolean completeUpdateHistory=true;private StatisticalTrainingDiagnosticsR26.Step last;
    public FamilyF08KernelR26(){this(Integer.parseInt(ModelEvolutionSU2.centralParams("HMM").get("K")),true,true);}
    public FamilyF08KernelR26(int states,boolean memory){this(states,memory,false);}
    private FamilyF08KernelR26(int count,boolean memory,boolean legacy){
        if(count<2||count>6)throw new IllegalArgumentException("F08_STATE_GRAPH");
        states=count;transitionMemory=memory;legacyCodec=legacy;Map<String,String> p=ModelEvolutionSU2.centralParams("HMM");
        model=new OnlineModelsSU2.Hmm(count,memory?Double.parseDouble(p.get("stay")):1.0/count,Double.parseDouble(p.get("lr")),Long.parseLong(p.get("seed")),Double.parseDouble(p.get("temper")));
    }
    public void resetTemporalContext(){TemporalResetR26.reset(model);last=null;}
    public StatisticalTrainingDiagnosticsR26.Step lastStatisticalTrainingStep(){return last;}
    public long completedTrainingUpdates(){return updates;}
    public String optimizerProtocol(){return "TEMPERED_POSTERIOR_EMISSION_RELAXATION_V2|lr="+Double.toHexString(model.lr)+"|temper="+Double.toHexString(model.temper)+"|"+(completeUpdateHistory?"KNOWN_LIFETIME":"UNKNOWN_PRIOR_UPDATES_COUNTED_SINCE_IMPORT");}
    public int stateWidth(){return model.stateWidth();}
    public void predict(float[][] out,float[] state){model.predict(out,state);}
    private OnlineModelsSU2.Hmm fresh(){return new OnlineModelsSU2.Hmm(states,model.stay,model.lr,0,model.temper);}
    private static double loss(OnlineModelsSU2.Hmm parameters,double[] oldPosterior,int[] draw){
        double[] pi=new double[parameters.K];for(int j=0;j<parameters.K;j++)for(int i=0;i<parameters.K;i++)pi[j]+=oldPosterior[i]*parameters.A[i*parameters.K+j];double loss=0;
        for(int p=0;p<10;p++){double probability=0;for(int k=0;k<parameters.K;k++)probability+=pi[k]*parameters.E[k*100+p*10+draw[p]-1];if(!(probability>0)||!Double.isFinite(probability))throw new IllegalStateException("F08_NONFINITE_PREDICTIVE_PROBABILITY");loss-=Math.log(probability)/10;}return loss;
    }
    private String inputHash(){StringBuilder b=new StringBuilder("F08_FIXED_PRE_LABEL_FILTER_V2|").append(states).append('|').append(transitionMemory).append('|').append(updates).append('|').append(completeUpdateHistory);for(double x:model.a)b.append('|').append(Double.toHexString(x));return PolicyIdentityR26.sha256(b.toString());}
    public void update(int[] draw){
        OnlineModelsSU2.requireDraw(draw);ExecutionBudgetR26.poll();if(updates==Long.MAX_VALUE)throw new IllegalStateException("F08_UPDATE_CLOCK_CAPACITY");double before=loss(model,model.a,draw);OnlineModelsSU2.Hmm next=fresh();System.arraycopy(model.E,0,next.E,0,model.E.length);System.arraycopy(model.a,0,next.a,0,states);System.arraycopy(model.A,0,next.A,0,model.A.length);next.update(draw);double squared=0;for(int i=0;i<model.E.length;i++){double d=next.E[i]-model.E[i];squared+=d*d;}
        StatisticalTrainingDiagnosticsR26.Step step=new StatisticalTrainingDiagnosticsR26.Step(updates+1,before,loss(next,model.a,draw),Math.sqrt(squared),Math.sqrt(squared),inputHash(),"MEAN_MARGINAL_CATEGORICAL_PREDICTIVE_NLL_FIXED_PRE_LABEL_FILTER_POSTERIOR;TEMPERED_POSTERIOR_EMISSION_RELAXATION_NOT_SGD;PARAMETER_DELTA=EMISSION_FREQUENCY_ESTIMATES_ONLY","POSTERIOR_UPDATED_CATEGORICAL_EMISSION_FREQUENCY_ESTIMATES");ExecutionBudgetR26.poll();model=next;updates++;last=step;
    }
    public void write(DataOutput out)throws IOException{out.writeInt(0x26080002);out.writeInt(states);out.writeBoolean(transitionMemory);out.writeBoolean(legacyCodec);model.write(out);OnlineModelsSU2.writeArr(out,model.A);out.writeLong(updates);out.writeBoolean(completeUpdateHistory);}
    private static void validate(OnlineModelsSU2.Hmm restored)throws IOException{
        for(int k=0;k<restored.K;k++)for(int p=0;p<10;p++){double sum=0;for(int n=0;n<10;n++){double x=restored.E[k*100+p*10+n];if(!Double.isFinite(x)||x<0||x>1.00000001)throw new IOException("F08_EMISSION_STATE");sum+=x;}if(Math.abs(sum-1)>1e-8)throw new IOException("F08_EMISSION_MASS");}
        double sum=0;for(double x:restored.a){if(!Double.isFinite(x)||x<0||x>1.00000001)throw new IOException("F08_FILTER_STATE");sum+=x;}if(Math.abs(sum-1)>1e-8)throw new IOException("F08_FILTER_MASS");
    }
    public void read(DataInput in)throws IOException{
        int header=in.readInt();boolean current=header==0x26080002;OnlineModelsSU2.Hmm restored=fresh();
        if(current){int count=in.readInt(),memory=in.readUnsignedByte(),legacy=in.readUnsignedByte();if(count!=states||memory>1||legacy>1||(memory==1)!=transitionMemory||(legacy==1)!=legacyCodec)throw new IOException("F08_CHECKPOINT_GRAPH");restored.read(in);double[] transition=new double[states*states];OnlineModelsSU2.readArr(in,transition);for(int i=0;i<transition.length;i++)if(Double.doubleToLongBits(transition[i])!=Double.doubleToLongBits(restored.A[i]))throw new IOException("F08_FIXED_TRANSITION_BINDING");}
        else if(header==MAGIC){if(legacyCodec||in.readInt()!=states||in.readBoolean()!=transitionMemory)throw new IOException("F08_CHECKPOINT_GRAPH");restored.read(in);}
        else{if(!legacyCodec||header!=restored.E.length)throw new IOException("F08_LEGACY_CHECKPOINT_GRAPH");for(int i=0;i<restored.E.length;i++)restored.E[i]=in.readDouble();OnlineModelsSU2.readArr(in,restored.a);}
        validate(restored);long lifetime=0;boolean known=false;if(current){lifetime=in.readLong();int flag=in.readUnsignedByte();if(lifetime<0||flag>1)throw new IOException("F08_UPDATE_HISTORY");known=flag==1;}model=restored;updates=lifetime;completeUpdateHistory=known;last=null;
    }
}
