package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;

/** Strict issue-paired candidate-vs-default future evidence. No candidate selection is performed here. */
public final class ResearchPairedEvidenceR906 {
    private ResearchPairedEvidenceR906() {}

    public static Evidence compute(SQLiteDatabase db, String assetId) {
        if (db == null || safe(assetId).length() == 0) return Evidence.empty("INPUT_INVALID");
        String trained = scalar(db, "SELECT trained_until_issue FROM " + DbHelper.TABLE_R906_RESEARCH_ASSET + " WHERE asset_id=?", new String[]{assetId});
        if (!TargetIssueResolver.isValid(trained)) return Evidence.empty("TRAINED_UNTIL_MISSING");
        Cursor c = null; ArrayList<Double> deltas = new ArrayList<Double>();
        int n=0,cHit=0,bHit=0; double brier=0.0d; int[] bc=new int[10];double[] bp=new double[10],by=new double[10];
        String first="",last="";StringBuilder proof=new StringBuilder("R906_PAIRED|").append(assetId).append('|').append(trained);
        try {
            c=db.rawQuery(
                    "SELECT p.target_issue,p.probability,p.hit_label,p.outcome_hash,o.hit_label,o.provenance_hash"
                            + " FROM " + DbHelper.TABLE_R817_RESEARCH_PREDRAW + " p JOIN " + DbHelper.TABLE_R35_OOS_LABEL
                            + " o ON o.target_issue=p.target_issue WHERE p.candidate_id=? AND p.hit_label IN (0,1)"
                            + " AND o.strict_oos=1 AND p.target_issue>? ORDER BY p.target_issue ASC LIMIT 240",
                    new String[]{assetId,trained});
            while(c.moveToNext()){
                String issue=safe(c.getString(0));double p=clamp(c.getDouble(1),0,1);int cy=c.getInt(2),byy=c.getInt(4);
                if(!TargetIssueResolver.isValid(issue)||cy<0||cy>1||byy<0||byy>1)continue;
                if(first.length()==0)first=issue;last=issue;n++;cHit+=cy;bHit+=byy;double d=cy-byy;deltas.add(Double.valueOf(d));
                brier+=(p-cy)*(p-cy);int bin=Math.min(9,(int)Math.floor(p*10.0d));bc[bin]++;bp[bin]+=p;by[bin]+=cy;
                proof.append('|').append(issue).append(':').append(safe(c.getString(3))).append(':').append(safe(c.getString(5)));
            }
        } finally { if(c!=null)c.close(); }
        if(n==0)return Evidence.empty("NO_FUTURE_PAIRED_EVIDENCE");
        double delta=(double)(cHit-bHit)/n;double mean=delta,ss=0.0d;for(Double z:deltas){double q=z.doubleValue()-mean;ss+=q*q;}
        double variance=n>1?ss/(n-1):1.0d;double se=Math.sqrt(variance/n);
        // Fixed first-240 horizon + z=3.5 is a conservative family-wise correction for a pool <=96.
        // It prevents optional stopping and is stricter than an unadjusted Wilson/display interval.
        double adjustedLower=delta-3.5d*se;
        double ece=0.0d;for(int i=0;i<10;i++)if(bc[i]>0)ece+=((double)bc[i]/n)*Math.abs(bp[i]/bc[i]-by[i]/bc[i]);
        double blockStability=blockStability(deltas,60);double drift=drift(deltas,60);
        return new Evidence("AVAILABLE",n,cHit,bHit,(double)cHit/n,(double)bHit/n,delta,adjustedLower,brier/n,ece,blockStability,drift,first,last,V51ChecksumEngine.sha256(proof.toString()));
    }

    private static double blockStability(ArrayList<Double>x,int block){if(x.isEmpty())return 0;double min=Double.MAX_VALUE,max=-Double.MAX_VALUE;for(int i=0;i<x.size();i+=block){int end=Math.min(x.size(),i+block);double s=0;for(int j=i;j<end;j++)s+=x.get(j);double r=s/(end-i);min=Math.min(min,r);max=Math.max(max,r);}return clamp(1.0d-(max-min),0,1);}
    private static double drift(ArrayList<Double>x,int block){if(x.size()<Math.min(24,block))return 1.0d;int n=x.size(),aEnd=Math.min(block,n),bStart=Math.max(0,n-block);double a=0,b=0;for(int i=0;i<aEnd;i++)a+=x.get(i);for(int i=bStart;i<n;i++)b+=x.get(i);return Math.abs(a/aEnd-b/(n-bStart));}
    private static String scalar(SQLiteDatabase db,String sql,String[]args){Cursor c=null;try{c=db.rawQuery(sql,args);return c.moveToFirst()?safe(c.getString(0)):"";}finally{if(c!=null)c.close();}}
    private static double clamp(double x,double lo,double hi){return Math.max(lo,Math.min(hi,Double.isFinite(x)?x:0));}
    private static String safe(String x){return x==null?"":x.trim();}

    public static final class Evidence{
        public final String status;public final int n,candidateHit,baselineHit;public final double candidateRate,baselineRate,pairedDelta,adjustedLower,brier,ece,blockStability,drift;public final String firstIssue,lastIssue,evidenceHash;
        Evidence(String s,int n,int ch,int bh,double cr,double br,double d,double l,double b,double e,double st,double drift,String first,String last,String hash){status=s;this.n=n;candidateHit=ch;baselineHit=bh;candidateRate=cr;baselineRate=br;pairedDelta=d;adjustedLower=l;brier=b;ece=e;blockStability=st;this.drift=drift;firstIssue=first;lastIssue=last;evidenceHash=hash;}
        static Evidence empty(String s){return new Evidence(s,0,0,0,0,0,0,-1,1,1,0,1,"","","");}
        public boolean available(){return n>0&&"AVAILABLE".equals(status);}
    }
}
