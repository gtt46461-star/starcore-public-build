package com.ethk3.novaonepro.strategyv2;
import java.io.*;
/** Actual discounted relation counts, with atomic updates and a separate statistical witness. */
public final class FamilyF21KernelR26 implements TemporalModelR26,StatisticalTrainingDiagnosticsR26 {
    private OnlineModelsSU2.RelBayes model=fresh();private long updates;private boolean known=true;private StatisticalTrainingDiagnosticsR26.Step last;
    private static OnlineModelsSU2.RelBayes fresh(){return (OnlineModelsSU2.RelBayes)ModelReplaySU2.build("RELBAYES",ModelEvolutionSU2.centralParams("RELBAYES"));}
    public int contextRequired(){return model.maxlag;}
    public int stateWidth(){return model.stateWidth();}
    public void predict(float[][] out,float[] state){model.predict(out,state);}
    public void resetTemporalContext(){TemporalResetR26.reset(model);last=null;}
    public long completedTrainingUpdates(){return updates;}
    public StatisticalTrainingDiagnosticsR26.Step lastStatisticalTrainingStep(){return last;}
    public String optimizerProtocol(){return "DISCOUNTED_RELATION_BETA_COUNTS_V1|decay="+Double.toHexString(model.decay)+"|kappa="+Double.toHexString(model.kappa)+"|"+(known?"KNOWN_LIFETIME":"UNKNOWN_PRIOR_UPDATES_COUNTED_SINCE_IMPORT");}
    private OnlineModelsSU2.RelBayes copy(){OnlineModelsSU2.RelBayes next=fresh();System.arraycopy(model.s,0,next.s,0,model.s.length);System.arraycopy(model.n,0,next.n,0,model.n.length);next.seen=model.seen;for(int i=0;i<model.recent.length;i++)System.arraycopy(model.recent[i],0,next.recent[i],0,10);return next;}
    private double loss(OnlineModelsSU2.RelBayes parameters,int[] target){
        double result=0;for(int p=0;p<10;p++){double[] score=new double[10];
            for(int q=0;q<10;q++)for(int li=0;li<model.L;li++){int src=model.recent[(model.seen-model.lags[li])%model.maxlag][q]-1;int[] predicted={src,(src+1)%10,(src+9)%10,9-src};for(int m=0;m<4;m++){int i=((p*10+q)*model.L+li)*4+m;double rho=(parameters.s[i]+.1*model.kappa)/(parameters.n[i]+model.kappa),w=Math.log(rho/.1)/model.temp;if(model.positiveOnly)w=Math.max(w,0);score[predicted[m]]+=w;}}
            double max=Double.NEGATIVE_INFINITY;for(double x:score)max=Math.max(max,x);double mass=0;for(double x:score)mass+=Math.exp(x-max);result+=(Math.log(mass)+max-score[target[p]-1])/10;
        }return result;
    }
    private String inputHash(){StringBuilder b=new StringBuilder("F21_FIXED_PRE_LABEL_RELATIONS_V1|").append(model.seen).append('|').append(updates);for(int[] row:model.recent)for(int x:row)b.append('|').append(x);return PolicyIdentityR26.sha256(b.toString());}
    public void update(int[] draw){
        OnlineModelsSU2.requireDraw(draw);ExecutionBudgetR26.poll();if(model.seen==Integer.MAX_VALUE||updates==Long.MAX_VALUE)throw new IllegalStateException("F21_CLOCK_CAPACITY");
        OnlineModelsSU2.RelBayes next=copy();boolean train=model.seen>=model.maxlag;double before=train?loss(model,draw):0;next.update(draw);StatisticalTrainingDiagnosticsR26.Step step=null;
        if(train){double statistic=0,parameter=0;for(int i=0;i<model.s.length;i++){double ds=next.s[i]-model.s[i],dn=next.n[i]-model.n[i];statistic+=ds*ds+dn*dn;double old=(model.s[i]+.1*model.kappa)/(model.n[i]+model.kappa),value=(next.s[i]+.1*model.kappa)/(next.n[i]+model.kappa),d=value-old;parameter+=d*d;}
            step=new StatisticalTrainingDiagnosticsR26.Step(updates+1,before,loss(next,draw),Math.sqrt(statistic),Math.sqrt(parameter),inputHash(),"MEAN_MARGINAL_CATEGORICAL_NLL_FIXED_PRE_LABEL_RELATIONS;DISCOUNTED_COUNTS_NOT_SGD;PARAMETER_DELTA=BETA_POSTERIOR_MEANS","DISCOUNTED_RELATION_SUCCESS_AND_EXPOSURE_COUNTS");}
        ExecutionBudgetR26.poll();model=next;if(train)updates++;last=step;
    }
    public void write(DataOutput out)throws IOException{out.writeInt(0x26210001);model.write(out);out.writeLong(updates);out.writeBoolean(known);}
    public void read(DataInput in)throws IOException{
        OnlineModelsSU2.RelBayes restored=fresh();int first=in.readInt();boolean current=first==0x26210001;
        if(current)restored.read(in);else{if(first!=restored.s.length)throw new IOException("F21_OLD_STATE_SIZE");for(int i=0;i<restored.s.length;i++)restored.s[i]=in.readDouble();OnlineModelsSU2.readArr(in,restored.n);restored.seen=in.readInt();for(int[] row:restored.recent)for(int i=0;i<10;i++)row[i]=in.readByte();}
        if(restored.seen<0)throw new IOException("F21_CONTEXT_CLOCK");for(int i=0;i<restored.s.length;i++)if(!Double.isFinite(restored.s[i])||!Double.isFinite(restored.n[i])||restored.s[i]<0||restored.n[i]<restored.s[i])throw new IOException("F21_RELATION_COUNTS");
        for(int row=0;row<restored.recent.length;row++){if(restored.seen>=restored.maxlag||row<restored.seen){try{OnlineModelsSU2.requireDraw(restored.recent[row]);}catch(IllegalArgumentException bad){throw new IOException("F21_CONTEXT_DRAW",bad);}}else for(int x:restored.recent[row])if(x!=0)throw new IOException("F21_UNUSED_CONTEXT");}
        long counter=0;boolean complete=false;if(current){counter=in.readLong();int flag=in.readUnsignedByte();if(counter<0||flag>1)throw new IOException("F21_UPDATE_HISTORY");complete=flag==1;}model=restored;updates=counter;known=complete;last=null;
    }
}
