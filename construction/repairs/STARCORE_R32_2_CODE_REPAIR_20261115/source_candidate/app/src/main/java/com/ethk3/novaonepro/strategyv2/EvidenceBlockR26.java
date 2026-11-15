package com.ethk3.novaonepro.strategyv2;

import java.io.*;

/** Bounded sufficient statistics of a predeclared calendar. No draws or per-period actions are retained. */
public final class EvidenceBlockR26 {
    public static final int BINS=256,MAX_CALENDAR=1000000;
    public int calendar,acts,hits,missing,pending,benchmarkMissing;
    public long pairedTenths;
    public final int[] count=new int[BINS],won=new int[BINS];
    public static int bin(double probability){if(!Double.isFinite(probability)||probability<0||probability>1)throw new IllegalArgumentException("R26_EVIDENCE_PROBABILITY");return Math.min(BINS-1,(int)(probability*BINS));}
    public static double representative(int bin){return (bin+.5)/BINS;}
    public static double quantized(double probability){return representative(bin(probability));}
    /** state: 0=MISSING, 1=PENDING, 2=settled ABSTAIN, 3=settled ACT. */
    public void add(int state,boolean hit,double probability,int benchmarkState,boolean benchmarkHit){
        if(state<0||state>3||benchmarkState<0||benchmarkState>3||calendar>=MAX_CALENDAR)throw new IllegalArgumentException("R26_EVIDENCE_CALENDAR");
        int probabilityBin=state==3?bin(probability):0;
        calendar++;if(state==0)missing++;else if(state==1)pending++;
        if(benchmarkState<2)benchmarkMissing++;
        if(state==3){acts++;count[probabilityBin]++;if(hit){hits++;won[probabilityBin]++;}}
        pairedTenths+=(state==3?(hit?7:-3):0)-(benchmarkState==3?(benchmarkHit?7:-3):0);
    }
    public void merge(EvidenceBlockR26 other){
        if(other.calendar>MAX_CALENDAR-calendar)throw new IllegalArgumentException("R26_EVIDENCE_CAPACITY");
        calendar+=other.calendar;acts+=other.acts;hits+=other.hits;missing+=other.missing;pending+=other.pending;benchmarkMissing+=other.benchmarkMissing;pairedTenths+=other.pairedTenths;
        for(int i=0;i<BINS;i++){count[i]+=other.count[i];won[i]+=other.won[i];}
    }
    public byte[] bytes()throws IOException {
        ByteArrayOutputStream bytes=new ByteArrayOutputStream();try(DataOutputStream out=new DataOutputStream(bytes)){out.writeInt(0x26454201);out.writeInt(calendar);out.writeInt(acts);out.writeInt(hits);out.writeInt(missing);out.writeInt(pending);out.writeInt(benchmarkMissing);out.writeLong(pairedTenths);for(int i=0;i<BINS;i++){out.writeInt(count[i]);out.writeInt(won[i]);}}return bytes.toByteArray();
    }
    public static EvidenceBlockR26 read(byte[] bytes)throws IOException {
        if(bytes==null||bytes.length!=2084)throw new IOException("R26_EVIDENCE_BLOCK_SIZE");
        try(DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes))){if(in.readInt()!=0x26454201)throw new IOException("R26_EVIDENCE_BLOCK_ABI");EvidenceBlockR26 b=new EvidenceBlockR26();b.calendar=in.readInt();b.acts=in.readInt();b.hits=in.readInt();b.missing=in.readInt();b.pending=in.readInt();b.benchmarkMissing=in.readInt();b.pairedTenths=in.readLong();int total=0,wins=0;
            for(int i=0;i<BINS;i++){b.count[i]=in.readInt();b.won[i]=in.readInt();if(b.count[i]<0||b.count[i]>MAX_CALENDAR||b.won[i]<0||b.won[i]>b.count[i])throw new IOException("R26_EVIDENCE_BIN");total+=b.count[i];wins+=b.won[i];}
            if(b.calendar<0||b.calendar>MAX_CALENDAR||b.acts!=total||b.hits!=wins||b.acts>b.calendar||b.missing<0||b.pending<0||b.missing+b.pending+b.acts>b.calendar||b.benchmarkMissing<0||b.benchmarkMissing>b.calendar||Math.abs(b.pairedTenths)>10L*b.calendar)throw new IOException("R26_EVIDENCE_BLOCK_COUNTS");return b;
        }
    }
    public static SelectiveCalibrationSU2.Platt fit(EvidenceBlockR26 fit){
        double a=1,b=0,ridge=10;
        for(int iteration=0;iteration<25;iteration++){
            double ga=ridge*(a-1),gb=ridge*b,haa=ridge,hab=0,hbb=ridge+1e-9;
            for(int i=0;i<BINS;i++)if(fit.count[i]>0){double x=SelectiveCalibrationSU2.logit(representative(i)),p=1/(1+Math.exp(-(a*x+b))),e=p*fit.count[i]-fit.won[i],w=p*(1-p)*fit.count[i];ga+=e*x;gb+=e;haa+=w*x*x;hab+=w*x;hbb+=w;}
            double det=haa*hbb-hab*hab;if(Math.abs(det)<1e-12)break;double da=(hbb*ga-hab*gb)/det,db=(haa*gb-hab*ga)/det;a-=da;b-=db;if(Math.abs(da)+Math.abs(db)<1e-9)break;
        }
        return new SelectiveCalibrationSU2.Platt(a,b);
    }
    public double ece(SelectiveCalibrationSU2.Platt cal){
        if(acts==0)return Double.POSITIVE_INFINITY;double[] sum=new double[10];int[] n=new int[10],k=new int[10];
        for(int i=0;i<BINS;i++)if(count[i]>0){double p=cal.apply(representative(i));int bin=Math.min(9,(int)(p*10));sum[bin]+=p*count[i];n[bin]+=count[i];k[bin]+=won[i];}
        double e=0;for(int i=0;i<10;i++)e+=Math.abs(sum[i]-k[i])/acts;return e;
    }
    public static SelectiveEvidenceSU2.Verdict evaluate(EvidenceBlockR26[] quarters,int look,double alpha,boolean compare){
        if(quarters.length!=4)throw new IllegalArgumentException("R26_EVIDENCE_FOUR_BLOCKS");EvidenceBlockR26 all=new EvidenceBlockR26();double worst=1;
        for(EvidenceBlockR26 b:quarters){all.merge(b);worst=Math.min(worst,b.acts==0?0:b.hits/(double)b.acts);}
        double rate=all.acts==0?0:all.hits/(double)all.acts,coverage=all.acts/(double)Math.max(1,all.calendar),spent=SelectiveEvidenceSU2.alphaForLook(alpha,look);
        double lower=SelectiveEvidenceSU2.hoeffdingLower(all.hits,all.acts,spent),mean=all.pairedTenths/(10.0*Math.max(1,all.calendar)),radius=Math.sqrt(2*Math.log(1/spent)/Math.max(1,all.calendar));
        String reason=all.pending>0?"PENDING_LABELS":all.missing>0?"CALENDAR_INCOMPLETE":compare&&all.benchmarkMissing>0?"BENCHMARK_CALENDAR_INCOMPLETE":all.acts<600?"ACTS_LT_600":coverage<.15?"COVERAGE_LT_0.15":rate<.35?"OBSERVED_HIT_RATE_LT_035":!(lower>.30)?"LOWER_BOUND_NOT_ABOVE_NULL_30":worst<.28?"BLOCK_BELOW_FLOOR":compare&&mean-radius<-.01?"PAIRED_LCB_BELOW_PREREGISTERED_MINUS_001":"PASS";
        return new SelectiveEvidenceSU2.Verdict(reason.equals("PASS"),reason,all.acts,all.hits,all.missing,rate,lower,coverage,worst,compare?mean+radius:Double.NaN,compare?mean-radius:Double.NaN);
    }
}
