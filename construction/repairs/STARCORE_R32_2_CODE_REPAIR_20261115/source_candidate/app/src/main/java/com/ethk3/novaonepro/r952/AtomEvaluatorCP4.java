package com.ethk3.novaonepro.r952;
import java.util.*;
/** One deterministic method for 29 accepted family labels; no database, clock or global cache. */
public final class AtomEvaluatorCP4 {
 private AtomEvaluatorCP4(){}
 public static final class Spec {
  public final String family,regime;public final int w,lag,digit,from,to,recent,reference,period,order,minContext;
  public final double alpha;
  public Spec(String family,int w,int lag,int digit,int from,int to,int recent,int reference,int period,double alpha,String regime){this(family,w,lag,digit,from,to,recent,reference,period,alpha,regime,1,0);}
  public Spec(String family,int w,int lag,int digit,int from,int to,int recent,int reference,int period,double alpha,String regime,int order,int minContext){
   this.family=Objects.requireNonNull(family);this.w=w;this.lag=lag;this.digit=digit;this.from=from;this.to=to;this.recent=recent;this.reference=reference;this.period=period;this.alpha=alpha;this.regime=regime;this.order=order;this.minContext=minContext;
   if(digit<0||digit>9||from<0||from>9||to<0||to>9||!Double.isFinite(alpha)||alpha<0)throw new IllegalArgumentException("ATOM_SPEC");
   if(order!=1&&order!=2||order==2&&(!family.equals("MARKOV_SCORE")||minContext!=8||alpha<=0)||order==1&&minContext!=0)throw new IllegalArgumentException("MARKOV_ORDER_OR_CONTEXT");
  }
 }
 public static final class Binding {
  // Mutable array references never escape. Oldest to newest, all settled issues strictly before target.
  private final int[] hits,masks,hitPrefix;private final double[] probability,residualPrefix;private final int trailingMiss;public final long maxSourceOrdinal;
  public final String freezeHash,payloadHash;
  public Binding(int[] hits,double[] probability,int[] masks,long maxSourceOrdinal,String freezeHash,String payloadHash){
   if(hits.length!=probability.length||hits.length>5000||masks.length>32)throw new IllegalArgumentException("BINDING_SIZE");
   this.hits=hits.clone();this.probability=probability.clone();this.masks=masks.clone();this.maxSourceOrdinal=maxSourceOrdinal;this.freezeHash=Objects.requireNonNull(freezeHash);this.payloadHash=Objects.requireNonNull(payloadHash);
   for(int i=0;i<hits.length;i++)if((hits[i]!=0&&hits[i]!=1)||!Double.isFinite(probability[i])||probability[i]<0||probability[i]>1)throw new IllegalArgumentException("BINDING_VALUE");
   for(int mask:masks)if(Integer.bitCount(mask)!=3||(mask&~1023)!=0)throw new IllegalArgumentException("FIXED_K3");
   hitPrefix=new int[hits.length+1];residualPrefix=new double[hits.length+1];int streak=0;for(int i=0;i<hits.length;i++){hitPrefix[i+1]=hitPrefix[i]+hits[i];residualPrefix[i+1]=residualPrefix[i]+hits[i]-probability[i];streak=hits[i]==0?streak+1:0;}trailingMiss=streak;
  }
 }
 public static final class Result {
  public final double value;public final boolean present;public final String reason;
  private Result(double value,boolean present,String reason){this.value=value;this.present=present;this.reason=reason;}
 }
 public static Result evaluate(Spec s,AtomWindowCP4 data,Binding bound,long targetOrdinal){
  if(data.end()>=targetOrdinal)throw new IllegalArgumentException("PIT_INVALID");
  try{double answer=calculate(s,data,bound,targetOrdinal);if(!Double.isFinite(answer))throw new AtomWindowCP4.Missing("NONFINITE");return new Result(answer,true,"");}
  catch(AtomWindowCP4.Missing missing){return new Result(0,false,missing.getMessage());}
 }
 private static double calculate(Spec s,AtomWindowCP4 d,Binding b,long target){int w=s.w,k=s.digit;
  switch(s.family){
   case "LAG_DIGIT_EQ":return d.lag(s.lag)==k?1:0;
   case "DELTA_NUMERIC":return d.lag(1)-d.lag(s.lag+1);
   case "DIRECTION":return Integer.signum(d.lag(1)-d.lag(s.lag+1));
   case "FREQUENCY":case "ROLLING_MEAN_INDICATOR":return d.window(w,0).frequency(k);
   case "ROLLING_VARIANCE_INDICATOR":double p=d.window(w,0).frequency(k);return p*(1-p);
   case "FREQUENCY_SLOPE":return d.window(w,0).slope(k);
   case "OMISSION":return d.omission(k);
   case "OMISSION_RATE":if(w<1||d.size()<w)throw new AtomWindowCP4.Missing("INSUFFICIENT_HISTORY");return Math.min(d.omission(k),w)/w;
   case "GAP_LAST_GAP":return d.window(w,0).gap(k,"LAST");
   case "GAP_MEAN_GAP":return d.window(w,0).gap(k,"MEAN");
   case "GAP_VAR_GAP":return d.window(w,0).gap(k,"VAR");
   case "GAP_GAP_Z":return d.window(w,0).gap(k,"Z");
   case "TRANSITION_P1":return d.window(w,1).transition(s.from,s.to,s.alpha);
   case "MARKOV_SCORE":return s.order==2?d.markov2(w).probability(d.lag(2),d.lag(1),k,s.alpha,s.minContext):d.window(w,1).transition(d.lag(1),k,s.alpha);
   case "PERIODICITY":return d.window(w,s.period).periodicity(k);
   case "ENTROPY":return AtomWindowCP4.entropy(d.window(w,0).counts());
   case "CONDITIONAL_ENTROPY":return d.window(w,s.lag).conditionalEntropy();
   case "MUTUAL_INFORMATION":return d.window(w,s.lag).indicatorMI(k);
   case "ROLLING_MEAN_DIGIT":return d.window(w,0).numericMean();
   case "ROLLING_VARIANCE_DIGIT":return d.window(w,0).numericVariance();
   case "HOT_COLD_Z":if(s.recent>=s.reference)throw new IllegalArgumentException("WINDOW_ORDER");double a=d.window(s.recent,0).frequency(k),z=d.window(s.reference,0).frequency(k);double variance=z*(1-z)*(1.0/s.recent-1.0/s.reference);if(variance<=1e-12)throw new AtomWindowCP4.Missing("HOT_COLD_VARIANCE_ZERO");return (a-z)/Math.sqrt(variance);
   case "DRIFT_JS":case "DRIFT_PSI":double[] recent=d.window(s.recent,0).counts(),older=d.window(s.recent+s.reference,0).counts();for(int i=0;i<10;i++)older[i]-=recent[i];double[] r=AtomWindowCP4.probabilities(recent,.5),q=AtomWindowCP4.probabilities(older,.5);if(s.family.equals("DRIFT_JS"))return AtomWindowCP4.js(r,q);double psi=0;for(int i=0;i<10;i++)psi+=(r[i]-q[i])*Math.log(r[i]/q[i]);return psi;
   case "REGIME_SCORE":double[] c=d.window(w,0).counts(),old=d.window(2*w,0).counts();for(int i=0;i<10;i++)old[i]-=c[i];double h=AtomWindowCP4.entropy(c)/Math.log(10),j=AtomWindowCP4.js(AtomWindowCP4.probabilities(c,.5),AtomWindowCP4.probabilities(old,.5)),corr;
    try{corr=d.window(w-1,1).digitCorrelation();}catch(AtomWindowCP4.Missing m){if(!m.getMessage().equals("ZERO_VARIANCE"))throw m;corr=0;}
    double[] logits={2*(1-h),2*h,2*Math.max(0,corr),2*Math.max(0,-corr),4*j};String[]labels={"LOW_ENTROPY","HIGH_ENTROPY","TRENDING","MEAN_REVERTING","DRIFTING"};double sum=0;int idx=-1;for(int i=0;i<5;i++){logits[i]=Math.exp(logits[i]);sum+=logits[i];if(labels[i].equals(s.regime))idx=i;}if(idx<0)throw new IllegalArgumentException("REGIME");return logits[idx]/sum;
   case "RESIDUAL_MEAN_TEMPLATE":requireBinding(b,target,w);return (b.residualPrefix[b.hits.length]-b.residualPrefix[b.hits.length-w])/w;
   case "LOSS_STREAK_TEMPLATE":requireBinding(b,target,1);return Math.min(w,b.trailingMiss);
   case "CONFIDENCE_TEMPLATE":requireBinding(b,target,w);int hits=b.hitPrefix[b.hits.length]-b.hitPrefix[b.hits.length-w];double n=w,ph=hits/n,zv=1.959963984540054;return (ph+zv*zv/(2*n)-zv*Math.sqrt(ph*(1-ph)/n+zv*zv/(4*n*n)))/(1+zv*zv/n);
   case "STRATEGY_DISAGREEMENT_TEMPLATE":if(b==null||b.masks.length<2)throw new AtomWindowCP4.Missing("WAIT_BINDING");if(b.maxSourceOrdinal>=target)throw new IllegalArgumentException("PIT_INVALID");double total=0;int pairs=0;for(int i=0;i<b.masks.length;i++)for(int jj=i+1;jj<b.masks.length;jj++){total+=1-Integer.bitCount(b.masks[i]&b.masks[jj])/(double)Integer.bitCount(b.masks[i]|b.masks[jj]);pairs++;}return total/pairs;
   default:throw new IllegalArgumentException("UNKNOWN_ATOM_FAMILY:"+s.family);
  }
 }
 private static void requireBinding(Binding b,long target,int min){if(b==null||b.freezeHash.isEmpty()||b.payloadHash.isEmpty())throw new AtomWindowCP4.Missing("WAIT_BINDING");if(b.maxSourceOrdinal>=target)throw new IllegalArgumentException("PIT_INVALID");if(b.hits.length<min)throw new AtomWindowCP4.Missing("INSUFFICIENT_FUTURE_HISTORY");}
}
