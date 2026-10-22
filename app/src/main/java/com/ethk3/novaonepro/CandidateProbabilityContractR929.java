package com.ethk3.novaonepro;

import java.util.Locale;

/** R929/C03 probability truth contract for research candidates and selected actions. */
public final class CandidateProbabilityContractR929 {
    public static final String REVISION="R929_CANDIDATE_PROBABILITY_V1";
    private CandidateProbabilityContractR929(){}

    public enum Kind { RAW_SCORE, RAW_ACTION_P, CALIBRATED_SELECTED_HIT_P, UNSUPPORTED }
    public static final class Value {
        public final Kind kind; public final double value; public final int supportN;
        public final double brier,ece,nll; public final String sourceHash,selectionPolicyHash;
        public final boolean probability;
        public Value(Kind kind,double value,int supportN,double brier,double ece,double nll,
                     String sourceHash,String selectionPolicyHash){
            this.kind=kind==null?Kind.UNSUPPORTED:kind;this.value=value;this.supportN=Math.max(0,supportN);
            this.brier=brier;this.ece=ece;this.nll=nll;this.sourceHash=optionalHash(sourceHash);
            this.selectionPolicyHash=optionalHash(selectionPolicyHash);
            this.probability=this.kind==Kind.RAW_ACTION_P||this.kind==Kind.CALIBRATED_SELECTED_HIT_P;
            if(probability&&(!finite(value)||value<0||value>1))throw new IllegalArgumentException("R929_PROB_RANGE");
            if(this.kind==Kind.CALIBRATED_SELECTED_HIT_P){
                if(this.supportN<=0||this.selectionPolicyHash.length()!=64)throw new IllegalArgumentException("R929_CAL_PROB_SUPPORT");
                if(!finite(brier)||brier<0||!finite(ece)||ece<0||ece>1||!finite(nll)||nll<0)throw new IllegalArgumentException("R929_CAL_METRICS");
            }
        }
        public double asProbability(){if(!probability)throw new IllegalStateException("R929_SCORE_IS_NOT_PROBABILITY:"+kind);return value;}
        public String canonical(){return REVISION+'|'+kind+'|'+fmt(value)+"|n="+supportN+"|brier="+fmt(brier)+"|ece="+fmt(ece)+"|nll="+fmt(nll)+"|source="+sourceHash+"|selection="+selectionPolicyHash;}
    }

    public static final class JointQuality {
        public final int eligibleN,selectedN,hits; public final double coverage,hitRate,ece,brier;
        public final boolean pointTargetMet; public final String status;
        public JointQuality(int eligibleN,int selectedN,int hits,double ece,double brier){
            if(eligibleN<0||selectedN<0||selectedN>eligibleN||hits<0||hits>selectedN)throw new IllegalArgumentException("R929_JOINT_COUNTS");
            this.eligibleN=eligibleN;this.selectedN=selectedN;this.hits=hits;
            this.coverage=eligibleN==0?0.0d:(double)selectedN/eligibleN;
            this.hitRate=selectedN==0?0.0d:(double)hits/selectedN;
            this.ece=ece;this.brier=brier;
            this.pointTargetMet=eligibleN>0&&selectedN>0&&coverage>=0.15d&&hitRate>=0.40d&&finite(ece)&&ece<=0.04d;
            this.status=pointTargetMet?"POINT_TARGET_MET_NEEDS_INDEPENDENT_CONFIDENCE":"FAIL_EFFECT_OR_SUPPORT";
        }
    }

    public static double brier(double[] probability,int[] label){
        requirePairs(probability,label);double s=0;for(int i=0;i<probability.length;i++){double p=probability[i];if(!finite(p)||p<0||p>1||(label[i]!=0&&label[i]!=1))throw new IllegalArgumentException("R929_BRIER_INPUT");double d=p-label[i];s+=d*d;}return s/probability.length;
    }
    public static double nll(double[] probability,int[] label){
        requirePairs(probability,label);double s=0;for(int i=0;i<probability.length;i++){double p=Math.max(1e-9,Math.min(1-1e-9,probability[i]));if(label[i]!=0&&label[i]!=1)throw new IllegalArgumentException("R929_NLL_LABEL");s-=label[i]*Math.log(p)+(1-label[i])*Math.log(1-p);}return s/probability.length;
    }
    public static double ece10(double[] probability,int[] label){
        requirePairs(probability,label);double e=0;for(int bin=0;bin<10;bin++){int n=0,h=0;double pSum=0;double lo=bin/10.0d,hi=(bin+1)/10.0d;for(int i=0;i<probability.length;i++){double p=probability[i];boolean in=p>=lo&&(bin==9?p<=1:p<hi);if(in){n++;h+=label[i];pSum+=p;}}if(n>0)e+=(double)n/probability.length*Math.abs(pSum/n-(double)h/n);}return e;
    }
    private static void requirePairs(double[] p,int[] y){if(p==null||y==null||p.length==0||p.length!=y.length)throw new IllegalArgumentException("R929_PROB_PAIR");}
    private static String optionalHash(String x){String v=x==null?"":x.trim().toLowerCase(Locale.US);if(v.length()==0)return "";if(!v.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("R929_PROB_HASH");return v;}
    private static boolean finite(double x){return !Double.isNaN(x)&&!Double.isInfinite(x);} private static String fmt(double x){return finite(x)?String.format(Locale.US,"%.8f",x):"N/A";}
}
