package com.ethk3.novaonepro;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;

/** Diagnostics over already paired, source-fenced Future observations. No admission authority. */
public final class PairedDiversityMetricsR81 {
    public static final int MIN_N = 20;
    private PairedDiversityMetricsR81() { }
    public static final class Pair {
        public final double childProbability,parentProbability;
        public final int childHit,parentHit,childMask,parentMask;
        public final String regime;
        public final boolean regimeVerified;
        public Pair(double cp,double pp,int ch,int ph,int cm,int pm,String r,boolean verified) {
            childProbability=cp;parentProbability=pp;childHit=ch;parentHit=ph;
            childMask=cm;parentMask=pm;regime=r==null?"":r;regimeVerified=verified;
        }
    }
    public static final class Result {
        public final int n;
        public final double predictionCorrelation,errorCorrelation,residualCorrelation,jointMiss,k3Jaccard,featureOverlap,regimeComplement;
        public final String status,regimeStatus;
        Result(int n,double pc,double ec,double rc,double jm,double kj,double fo,double rg,String status,String rs) {
            this.n=n;predictionCorrelation=pc;errorCorrelation=ec;residualCorrelation=rc;
            jointMiss=jm;k3Jaccard=kj;featureOverlap=fo;regimeComplement=rg;this.status=status;regimeStatus=rs;
        }
    }
    public static Result compute(List<Pair> input,Set<String> childFeatures,Set<String> parentFeatures) {
        List<Pair> pairs=new ArrayList<Pair>();
        if(input!=null)for(Pair p:input)if(valid(p))pairs.add(p);
        int n=pairs.size();
        double pc=Double.NaN,ec=Double.NaN,rc=Double.NaN,jm=Double.NaN,kj=Double.NaN;
        if(n>=MIN_N) {
            double[] cp=new double[n],pp=new double[n],ce=new double[n],pe=new double[n],cr=new double[n],pr=new double[n];
            int misses=0;double sumJ=0;
            for(int i=0;i<n;i++) { Pair p=pairs.get(i);cp[i]=p.childProbability;pp[i]=p.parentProbability;
                ce[i]=1-p.childHit;pe[i]=1-p.parentHit;cr[i]=p.childHit-p.childProbability;pr[i]=p.parentHit-p.parentProbability;
                if(p.childHit==0&&p.parentHit==0)misses++;
                sumJ+=(double)Integer.bitCount(p.childMask&p.parentMask)/Integer.bitCount(p.childMask|p.parentMask);
            }
            pc=correlation(cp,pp);ec=correlation(ce,pe);rc=correlation(cr,pr);jm=(double)misses/n;kj=sumJ/n;
        }
        Map<String,int[]> strata=new TreeMap<String,int[]>();boolean allVerified=n>0;
        for(Pair p:pairs) { if(!p.regimeVerified||p.regime.length()==0){allVerified=false;continue;}
            int[] a=strata.get(p.regime);if(a==null){a=new int[3];strata.put(p.regime,a);}a[0]++;a[1]+=p.childHit;a[2]+=p.parentHit;
        }
        // Complementarity requires two independently supported regimes with opposite advantage.
        double positive=0,negative=0;int supported=0;
        if(allVerified)for(int[] a:strata.values())if(a[0]>=MIN_N){double d=(double)(a[1]-a[2])/a[0];positive=Math.max(positive,d);negative=Math.max(negative,-d);supported++;}
        double complement=allVerified&&supported>=2?Math.min(positive,negative):Double.NaN;
        String rs=!allVerified?"UNKNOWN_FROZEN_REGIME_PROOF":supported<2?"UNKNOWN_REGIME_N":"KNOWN";
        return new Result(n,pc,ec,rc,jm,kj,jaccard(childFeatures,parentFeatures),complement,n<MIN_N?"UNKNOWN_PAIRED_N":"KNOWN",rs);
    }
    public static double correlation(double[] a,double[] b) {
        if(a==null||b==null||a.length!=b.length||a.length<MIN_N)return Double.NaN;
        double ma=0,mb=0;for(int i=0;i<a.length;i++){if(!finite(a[i])||!finite(b[i]))return Double.NaN;ma+=a[i];mb+=b[i];}ma/=a.length;mb/=a.length;
        double aa=0,bb=0,ab=0;for(int i=0;i<a.length;i++){double x=a[i]-ma,y=b[i]-mb;aa+=x*x;bb+=y*y;ab+=x*y;}
        if(aa<=1e-24||bb<=1e-24)return Double.NaN;return Math.max(-1,Math.min(1,ab/Math.sqrt(aa*bb)));
    }
    private static boolean valid(Pair p) { return p!=null&&unit(p.childProbability)&&unit(p.parentProbability)&&(p.childHit==0||p.childHit==1)&&(p.parentHit==0||p.parentHit==1)&&p.childMask>0&&p.parentMask>0&&(p.childMask&~1023)==0&&(p.parentMask&~1023)==0&&Integer.bitCount(p.childMask)==3&&Integer.bitCount(p.parentMask)==3; }
    private static double jaccard(Set<String> a,Set<String> b) { if(a==null||b==null||a.isEmpty()||b.isEmpty())return Double.NaN;int intersection=0;for(String s:a)if(b.contains(s))intersection++;return (double)intersection/(a.size()+b.size()-intersection); }
    private static boolean unit(double x){return finite(x)&&x>=0&&x<=1;}
    private static boolean finite(double x){return !Double.isNaN(x)&&!Double.isInfinite(x);}
}
