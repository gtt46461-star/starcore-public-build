package com.ethk3.novaonepro.strategyv2;
import java.util.*;

/** Raw-calendar reference parity, held-later calibration and a preregistered statistical power probe. */
public final class R26EvidenceTest {
    static void check(boolean ok,String why){if(!ok)throw new AssertionError(why);}
    static void near(double a,double b,String why){check(Math.abs(a-b)<1e-10,why+":"+a+"/"+b);}
    static void parity()throws Exception {
        int n=4000;boolean[] act=new boolean[n],hit=new boolean[n],incAct=new boolean[n],incHit=new boolean[n];Random rng=new Random(2601);
        EvidenceBlockR26[] quarters={new EvidenceBlockR26(),new EvidenceBlockR26(),new EvidenceBlockR26(),new EvidenceBlockR26()};
        for(int t=0;t<n;t++){act[t]=rng.nextDouble()<.3;hit[t]=rng.nextDouble()<.4;incAct[t]=rng.nextDouble()<.2;incHit[t]=rng.nextDouble()<.35;quarters[t/(n/4)].add(act[t]?3:2,hit[t],.4,incAct[t]?3:2,incHit[t]);}
        SelectiveEvidenceSU2.Verdict raw=SelectiveEvidenceSU2.evaluate(act,hit,null,incAct,incHit,0,n,3,.003,600,.28,.15),folded=EvidenceBlockR26.evaluate(quarters,3,.003,true);
        check(raw.acts==folded.acts&&raw.hits==folded.hits&&raw.pass==folded.pass&&raw.reason.equals(folded.reason),"aggregate changed qualification");near(raw.lower,folded.lower,"Bernoulli lower");near(raw.pairedLower,folded.pairedLower,"paired lower");near(raw.worstBlock,folded.worstBlock,"block stability");
        for(EvidenceBlockR26 b:quarters)check(Arrays.equals(b.bytes(),EvidenceBlockR26.read(b.bytes()).bytes()),"sufficient statistics not resumable");
        quarters[0].add(1,false,Double.NaN,2,false);check(EvidenceBlockR26.evaluate(quarters,3,.003,true).reason.equals("PENDING_LABELS"),"unknown label became a loss or a pass");
        byte[] corrupt=quarters[1].bytes();corrupt[8]=(byte)0xff;try{EvidenceBlockR26.read(corrupt);throw new AssertionError("corrupt counts accepted");}catch(java.io.IOException expected){}
        int size=quarters[0].calendar;try{quarters[0].add(3,false,Double.NaN,2,false);throw new AssertionError("nonfinite probability accepted");}catch(IllegalArgumentException good){}check(quarters[0].calendar==size,"invalid sample mutated calendar");
        EvidenceBlockR26 fit=new EvidenceBlockR26(),held=new EvidenceBlockR26();double[] probabilities=new double[2400];boolean[] outcomes=new boolean[2400];
        for(int i=0;i<2400;i++){double q=.2+(i%5)*.12;boolean h=i%10<4;probabilities[i]=EvidenceBlockR26.quantized(q);outcomes[i]=h;fit.add(3,h,q,2,false);}
        SelectiveCalibrationSU2.Platt weighted=EvidenceBlockR26.fit(fit),expanded=SelectiveCalibrationSU2.fit(probabilities,outcomes,2400,10);near(weighted.a,expanded.a,"weighted Platt slope");near(weighted.b,expanded.b,"weighted Platt intercept");
        double[] calibrated=new double[800];boolean[] heldOut=new boolean[800];for(int i=0;i<800;i++){double q=.3+(i%4)*.1;heldOut[i]=i%5<2;held.add(3,heldOut[i],q,2,false);calibrated[i]=weighted.apply(EvidenceBlockR26.quantized(q));}near(held.ece(weighted),SelectiveCalibrationSU2.ece(calibrated,heldOut,800),"held-later ECE");
        System.out.println("CALENDAR_AGGREGATE_PARITY_CODEC_PENDING_CALIBRATION_PASS");
    }
    static SelectiveEvidenceSU2.Verdict simulate(int n,double probability,long seed){
        Random rng=new Random(seed);EvidenceBlockR26[] quarters={new EvidenceBlockR26(),new EvidenceBlockR26(),new EvidenceBlockR26(),new EvidenceBlockR26()};
        for(int t=0;t<n;t++){
            // Both predeclared K3 actions refer to the same legal draw label, with disjoint number sets.
            double u=rng.nextDouble();boolean mine=u<probability,parent=u>=probability&&u<probability+.30,act=t%20<3;
            quarters[Math.min(3,t/(n/4))].add(act?3:2,mine,probability,act?3:2,parent);
        }
        return EvidenceBlockR26.evaluate(quarters,1,.05/(25*26),true);
    }
    static void power(){
        int trials=300,positive=0,old=0,uniform=0;for(int i=0;i<trials;i++){if(simulate(QualificationWindowR26.LENGTH,.40,260000+i).pass)positive++;if(simulate(4000,.40,260000+i).pass)old++;}
        for(int i=0;i<1000;i++)if(simulate(QualificationWindowR26.LENGTH,.30,270000+i).pass)uniform++;
        check(old==0,"old underpowered counterexample no longer reproduced");check(positive>=285,"registered 40%/15% power below95%: "+positive);check(uniform==0,"uniform negative control granted qualification: "+uniform);
        System.out.println("EVIDENCE_POWER|calendar="+QualificationWindowR26.LENGTH+"|coverage=0.15|familyOrdinal=25|signal40="+positive+"/300|old4000="+old+"/300|null30="+uniform+"/1000|scope=SYNTHETIC_ONLY");
    }
    public static void main(String[] args)throws Exception {parity();power();System.out.println("R26_EVIDENCE_PASS");}
}
