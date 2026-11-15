package com.ethk3.novaonepro;

import android.database.Cursor;
import java.util.ArrayList;
import java.util.List;

/** Coverage-first fair scheduler. It never silently evaluates 9/96 because of ordering bias. */
public final class ResearchEvaluationSchedulerR906 {
    private ResearchEvaluationSchedulerR906(){}
    public static List<String> cohort(DbStore store,int max){ArrayList<String>out=new ArrayList<String>();if(store==null)return out;int bound=Math.max(1,Math.min(AppConfig.R906_RESEARCH_ACTIVE_MAX,max));Cursor c=null;try{c=store.readable().rawQuery("SELECT a.asset_id FROM "+DbHelper.TABLE_R906_RESEARCH_ASSET+" a LEFT JOIN "+DbHelper.TABLE_R906_RESEARCH_EVAL_COVERAGE+" e ON e.asset_id=a.asset_id WHERE a.runtime_executable=1 AND a.tier IN ('IDEA','NURSERY','ACTIVE','CHALLENGER','CERTIFIED_SHADOW') ORDER BY COALESCE(e.frozen_n,0) ASC,COALESCE(e.eligible_n,0) ASC,a.created_at ASC LIMIT ?",new String[]{String.valueOf(bound)});while(c.moveToNext())out.add(c.getString(0));}finally{if(c!=null)c.close();}return out;}

    /** Fair cohort that has not already frozen this exact future issue. */
    public static List<String> cohortForTarget(DbStore store,String target,int max){
        ArrayList<String>out=new ArrayList<String>();
        if(store==null||!TargetIssueResolver.isValid(target))return out;
        int bound=Math.max(1,Math.min(AppConfig.R906_RESEARCH_ACTIVE_MAX,max));Cursor c=null;
        try{
            c=store.readable().rawQuery(
                    "SELECT a.asset_id FROM "+DbHelper.TABLE_R906_RESEARCH_ASSET+" a"
                            +" LEFT JOIN "+DbHelper.TABLE_R906_RESEARCH_EVAL_COVERAGE+" e ON e.asset_id=a.asset_id"
                            +" WHERE a.runtime_executable=1 AND (a.tier IN ('IDEA','NURSERY','ACTIVE','CHALLENGER','CERTIFIED_SHADOW') OR "+PairedResearchChallengeR932.referenceCondition("'formula:'||a.asset_id",target)+")"
                            +GlobalResearchScheduleR932.predicate(target,"'formula:'||a.asset_id")
                            +" AND NOT EXISTS(SELECT 1 FROM "+DbHelper.TABLE_R817_RESEARCH_PREDRAW
                            +" p WHERE p.candidate_id=a.asset_id AND p.target_issue=?)"
                            +" ORDER BY COALESCE((SELECT cap.execution_started FROM "+ResearchCaptureLedgerR932.TABLE
                            +" cap WHERE cap.target_issue=? AND cap.candidate_key=('formula:'||a.asset_id)),0) ASC,"
                            +"COALESCE(e.frozen_n,0) ASC,COALESCE(e.eligible_n,0) ASC,a.created_at ASC LIMIT ?",
                    new String[]{target,target,String.valueOf(bound)});
            while(c.moveToNext())out.add(c.getString(0));
        }finally{if(c!=null)c.close();}
        return out;
    }

    /** Reconciles immutable R817 predraw/outcome rows into the R906 coverage ledger after exact settlement. */
    public static int syncSettledCoverage(final DbStore store) {
        if (store == null) return 0;
        final int[] changed={0};
        store.coordinatorR36().runWrite("R906_COVERAGE_SYNC",new DatabaseCoordinator.WriteRunnable(){
            public void run(android.database.sqlite.SQLiteDatabase db){
                Cursor assets=null;try{
                    String after=store.getState("r943_coverage_cursor","");
                    assets=db.rawQuery("SELECT asset_id FROM "+DbHelper.TABLE_R906_RESEARCH_ASSET+" WHERE runtime_executable=1 ORDER BY CASE WHEN asset_id>? THEN 0 ELSE 1 END,asset_id LIMIT 4",new String[]{after});
                    while(assets.moveToNext()){
                        ResearchWorkBudgetR927.checkpoint();
                        String id=assets.getString(0);store.setState("r943_coverage_cursor",id);Metrics m=metrics(db,id);if(m.n<=0)continue;
                        Cursor old=null;int eligible=0,scheduled=0,frozen=0;String first="",lastEligible="",lastFrozen="";long latency=0L,ram=0L;
                        try{old=db.rawQuery("SELECT eligible_n,scheduled_n,frozen_n,first_eligible_issue,last_eligible_issue,last_frozen_issue,COALESCE(latency_p95_ms,0),COALESCE(ram_p95_bytes,0) FROM "+DbHelper.TABLE_R906_RESEARCH_EVAL_COVERAGE+" WHERE asset_id=? LIMIT 1",new String[]{id});if(old.moveToFirst()){eligible=old.getInt(0);scheduled=old.getInt(1);frozen=old.getInt(2);first=old.getString(3);lastEligible=old.getString(4);lastFrozen=old.getString(5);latency=(long)old.getDouble(6);ram=old.getLong(7);}}finally{if(old!=null)old.close();}
                        // The unified ledger counts missed opportunities too; successful writes
                        // alone would produce a misleading 100% capture coverage.
                        try(Cursor capture=db.rawQuery("SELECT COUNT(*),COALESCE(SUM(selected),0),COALESCE(SUM(frozen),0) FROM "+ResearchCaptureLedgerR932.TABLE+" WHERE candidate_key=? AND eligible=1",new String[]{"formula:"+id})){
                            if(capture.moveToFirst()&&capture.getInt(0)>0){eligible=capture.getInt(0);scheduled=capture.getInt(1);frozen=capture.getInt(2);}
                        }
                        android.content.ContentValues v=new android.content.ContentValues();v.put("asset_id",id);v.put("eligible_n",eligible);v.put("scheduled_n",scheduled);v.put("frozen_n",frozen);v.put("settled_n",m.n);v.put("hit_n",m.hit);v.put("first_eligible_issue",safe(first));v.put("last_eligible_issue",safe(lastEligible));v.put("last_frozen_issue",safe(lastFrozen));v.put("last_settled_issue",m.lastIssue);v.put("max_gap_issues",Math.max(0,eligible-frozen));v.put("hit_rate",m.rate);v.put("interval_lower",m.low);v.put("interval_upper",m.high);v.put("interval_method","WILSON95");v.put("brier",m.brier);v.put("ece",m.ece);v.put("block_stability",m.blockStability);v.put("drift_score",m.drift);v.put("latency_p95_ms",(double)latency);v.put("ram_p95_bytes",ram);v.put("evidence_hash",m.hash);v.put("updated_at",System.currentTimeMillis());db.insertWithOnConflict(DbHelper.TABLE_R906_RESEARCH_EVAL_COVERAGE,null,v,android.database.sqlite.SQLiteDatabase.CONFLICT_REPLACE);changed[0]++;
                    }
                }finally{if(assets!=null)assets.close();}
            }});
        return changed[0];
    }
    private static Metrics metrics(android.database.sqlite.SQLiteDatabase db,String id){
        Cursor c=null;int n=0,hit=0,probabilityN=0;double brier=0d;int[]bc=new int[10];double[]bp=new double[10],by=new double[10];java.util.ArrayList<Integer>ys=new java.util.ArrayList<Integer>();String last="";
        try{c=db.rawQuery("SELECT probability,hit_label,target_issue FROM "+DbHelper.TABLE_R817_RESEARCH_PREDRAW+" WHERE candidate_id=? AND hit_label IN (0,1) ORDER BY target_issue ASC",new String[]{id});while(c.moveToNext()){double p=c.getDouble(0);int y=c.getInt(1);n++;hit+=y;if(Double.isFinite(p)&&p>=0&&p<=1){probabilityN++;brier+=(p-y)*(p-y);int bin=Math.min(9,(int)Math.floor(p*10d));bc[bin]++;bp[bin]+=p;by[bin]+=y;}ys.add(Integer.valueOf(y));last=safe(c.getString(2));}}finally{if(c!=null)c.close();}
        if(n<=0)return new Metrics();double ece=0d;for(int i=0;i<10;i++)if(bc[i]>0)ece+=((double)bc[i]/n)*Math.abs(bp[i]/bc[i]-by[i]/bc[i]);double[]w=wilson(hit,n);double first=rateRange(ys,0,Math.min(n,60)),recent=rateRange(ys,Math.max(0,n-60),n);double drift=n>=24?Math.abs(recent-first):1d;double stability=n>=24?Math.max(0d,1d-drift):0d;String h=V51ChecksumEngine.sha256("R906_EVAL|"+id+'|'+n+'|'+hit+'|'+brier+'|'+ece+'|'+last);return new Metrics(n,hit,(double)hit/n,w[0],w[1],probabilityN==n?brier/n:-1d,probabilityN==n?ece:-1d,stability,drift,last,h);
    }
    private static double rateRange(java.util.List<Integer>x,int from,int to){if(to<=from)return 0d;int h=0;for(int i=from;i<to;i++)h+=x.get(i).intValue();return (double)h/(to-from);}
    private static double[] wilson(int hit,int n){if(n<=0)return new double[]{0d,1d};double z=1.959963984540054d,p=(double)hit/n,z2=z*z,d=1d+z2/n;double center=(p+z2/(2.0d*n))/d;double half=z*Math.sqrt((p*(1.0d-p)+z2/(4.0d*n))/n)/d;return new double[]{Math.max(0d,center-half),Math.min(1d,center+half)};}
    private static final class Metrics{int n,hit;double rate,low,high,brier,ece,blockStability,drift;String lastIssue="",hash="";Metrics(){}Metrics(int n,int h,double r,double l,double u,double b,double e,double s,double d,String last,String hash){this.n=n;hit=h;rate=r;low=l;high=u;brier=b;ece=e;blockStability=s;drift=d;lastIssue=last;this.hash=hash;}}

    public static String coverageGap(DbStore store){if(store==null)return "STORE_NULL";Cursor c=null;try{c=store.readable().rawQuery("SELECT COUNT(*),COALESCE(MIN(frozen_n),0),COALESCE(MAX(frozen_n),0) FROM "+DbHelper.TABLE_R906_RESEARCH_EVAL_COVERAGE,null);return c.moveToFirst()?"assets="+c.getInt(0)+"|minFrozen="+c.getInt(1)+"|maxFrozen="+c.getInt(2):"assets=0";}finally{if(c!=null)c.close();}}
    private static String safe(String x){return x==null?"":x.trim();}
}
