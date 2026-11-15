package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** Descriptive, paired outcome diagnostics. Does not grant eligibility or monetary profit. */
public final class ResearchResultMetricsR81 {
    private ResearchResultMetricsR81() { }
    public static final class Sample {
        public final long ordinal;
        public final int childHit,parentHit;
        public final double childProbability,parentProbability;
        public final String regime;
        public final boolean regimeVerified;
        public Sample(long ordinal,int childHit,int parentHit,double childProbability,double parentProbability,String regime,boolean verified) {
            this.ordinal=ordinal;this.childHit=childHit;this.parentHit=parentHit;this.childProbability=childProbability;this.parentProbability=parentProbability;this.regime=regime==null?"":regime;this.regimeVerified=verified;
        }
    }
    public static final class Side {
        public final int n,hits,maxConsecutiveMiss;
        public final double hitRate,wilsonLower,wilsonUpper,brier,ece,baselineExcessMaxDrawdown;
        Side(int n,int hits,int streak,double hr,double lo,double hi,double brier,double ece,double dd){this.n=n;this.hits=hits;maxConsecutiveMiss=streak;hitRate=hr;wilsonLower=lo;wilsonUpper=hi;this.brier=brier;this.ece=ece;baselineExcessMaxDrawdown=dd;}
    }
    public static final class Result {
        public final int commonFutureN;
        public final Side child,parent;
        public final Map<Integer,Double> pairedWindowDelta;
        public final Map<String,Double> frozenRegimeDelta;
        public final double labelDrift;
        public final String regimeStatus;
        Result(int n,Side c,Side p,Map<Integer,Double>w,Map<String,Double>r,double drift,String status){commonFutureN=n;child=c;parent=p;pairedWindowDelta=Collections.unmodifiableMap(w);frozenRegimeDelta=Collections.unmodifiableMap(r);labelDrift=drift;regimeStatus=status;}
    }
    public static Result compute(List<Sample> input) {
        List<Sample> sorted=new ArrayList<Sample>();if(input!=null)for(Sample s:input)if(s!=null&&s.ordinal>=0&&(s.childHit==0||s.childHit==1)&&(s.parentHit==0||s.parentHit==1))sorted.add(s);
        Collections.sort(sorted,new Comparator<Sample>(){public int compare(Sample a,Sample b){return a.ordinal<b.ordinal?-1:a.ordinal==b.ordinal?0:1;}});
        List<Sample> rows=new ArrayList<Sample>();for(Sample s:sorted){if(!rows.isEmpty()&&rows.get(rows.size()-1).ordinal==s.ordinal)throw new IllegalArgumentException("duplicate Future issue");rows.add(s);}
        Map<Integer,Double>w=new LinkedHashMap<Integer,Double>();for(int size:new int[]{20,50,100,300}){double delta=Double.NaN;if(rows.size()>=size){int sum=0;for(int i=rows.size()-size;i<rows.size();i++)sum+=rows.get(i).childHit-rows.get(i).parentHit;delta=(double)sum/size;}w.put(size,delta);}
        Map<String,int[]>reg=new java.util.TreeMap<String,int[]>();boolean verified=!rows.isEmpty();
        for(Sample s:rows){if(!s.regimeVerified||s.regime.length()==0){verified=false;continue;}int[]v=reg.get(s.regime);if(v==null){v=new int[2];reg.put(s.regime,v);}v[0]++;v[1]+=s.childHit-s.parentHit;}
        Map<String,Double>rd=new LinkedHashMap<String,Double>();if(verified)for(Map.Entry<String,int[]>e:reg.entrySet())rd.put(e.getKey(),e.getValue()[0]>=20?(double)e.getValue()[1]/e.getValue()[0]:Double.NaN);
        double drift=Double.NaN;int n=rows.size();if(n>=40){int half=n/2,a=0,b=0;for(int i=0;i<half;i++)a+=rows.get(i).childHit;for(int i=n-half;i<n;i++)b+=rows.get(i).childHit;drift=(double)b/half-(double)a/half;}
        return new Result(n,side(rows,true),side(rows,false),w,rd,drift,verified?"KNOWN_FROZEN_REGIME":"UNKNOWN_FROZEN_REGIME_PROOF");
    }
    private static Side side(List<Sample> rows,boolean child){int n=rows.size(),hits=0,streak=0,maxStreak=0;long previous=-2;double bs=0,excess=0,peak=0,dd=0;boolean knownProbability=n>0;int[]bn=new int[10],bh=new int[10];double[]bp=new double[10];
        for(Sample s:rows){int hit=child?s.childHit:s.parentHit;double p=child?s.childProbability:s.parentProbability;hits+=hit;if(s.ordinal!=previous+1)streak=0;streak=hit==0?streak+1:0;maxStreak=Math.max(maxStreak,streak);previous=s.ordinal;
            excess+=hit-.3;peak=Math.max(peak,excess);dd=Math.max(dd,peak-excess);
            if(!unit(p)){knownProbability=false;continue;}double e=hit-p;bs+=e*e;int bin=Math.min(9,(int)(p*10));bn[bin]++;bh[bin]+=hit;bp[bin]+=p;
        }
        double ece=Double.NaN;if(knownProbability){ece=0;for(int i=0;i<10;i++)if(bn[i]>0)ece+=Math.abs((double)bh[i]/bn[i]-bp[i]/bn[i])*bn[i]/n;}
        double[]ci=wilson(hits,n);return new Side(n,hits,maxStreak,n==0?Double.NaN:(double)hits/n,ci[0],ci[1],knownProbability?bs/n:Double.NaN,ece,n==0?Double.NaN:dd);
    }
    public static double[] wilson(int hits,int n){if(n<=0||hits<0||hits>n)return new double[]{Double.NaN,Double.NaN};double z=1.959963984540054,z2=z*z,p=(double)hits/n,d=1+z2/n,c=(p+z2/(2*n))/d,r=z*Math.sqrt(p*(1-p)/n+z2/(4.0*n*n))/d;return new double[]{Math.max(0,c-r),Math.min(1,c+r)};}
    private static boolean unit(double p){return !Double.isNaN(p)&&!Double.isInfinite(p)&&p>=0&&p<=1;}
}
