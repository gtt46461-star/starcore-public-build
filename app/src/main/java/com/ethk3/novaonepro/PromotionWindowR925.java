package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** A challenger starts new evidence windows. Old accumulated samples cannot buy immediate influence. */
public final class PromotionWindowR925 {
    private PromotionWindowR925() {}
    public static void register(SQLiteDatabase db,String id,String issue,int sample,String evidence){
        db.execSQL("INSERT OR IGNORE INTO v57_promotion_window_r925 VALUES (?,?,?,?,?)",
            new Object[]{id,issue,sample,evidence,System.currentTimeMillis()});
    }
    public static Window evaluate(DbStore store,String id){
        Cursor c=store.readable().rawQuery("SELECT p.hit_label,o.hit_label,p.score FROM v57_promotion_window_r925 w JOIN v57_algorithm_predraw_r908 p ON p.genome_id=w.genome_id AND p.target_issue>w.start_issue JOIN "+CandidateExecutionAuditR932.TABLE+" e ON e.candidate_key=('algorithm:'||p.genome_id) AND e.target_issue=p.target_issue AND e.prediction_hash=p.record_hash JOIN v57_oos_label_r35 o ON o.target_issue=p.target_issue AND o.strict_oos=1 WHERE w.genome_id=? AND p.hit_label IN(0,1) AND o.hit_label IN(0,1) ORDER BY p.target_issue",new String[]{id});
        return fromCursor(c);
    }
    public static Window evaluateAsset(DbStore store,String id){
        Cursor c=store.readable().rawQuery("SELECT p.hit_label,o.hit_label,p.probability FROM v57_promotion_window_r925 w JOIN "+DbHelper.TABLE_R817_RESEARCH_PREDRAW+" p ON ('asset:'||p.candidate_id)=w.genome_id AND p.target_issue>w.start_issue JOIN v57_oos_label_r35 o ON o.target_issue=p.target_issue AND o.strict_oos=1 WHERE w.genome_id=? AND p.hit_label IN(0,1) AND o.hit_label IN(0,1) ORDER BY p.target_issue",new String[]{"asset:"+id});
        return fromCursor(c);
    }
    public static Window evaluateGenesis(DbStore store,String id){
        Cursor c=store.readable().rawQuery("SELECT p.hit_label,o.hit_label,p.score FROM v57_promotion_window_r925 w JOIN research_new_asset_freeze_v41 p ON ('genesis:'||p.asset_id)=w.genome_id AND p.target_issue>w.start_issue JOIN v57_oos_label_r35 o ON o.target_issue=p.target_issue AND o.strict_oos=1 WHERE w.genome_id=? AND p.hit_label IN(0,1) AND o.hit_label IN(0,1) ORDER BY p.target_issue",new String[]{"genesis:"+id});
        return fromCursor(c);
    }
    public static Window authorize(DbStore store,CertificateBundleServiceR929.Consumer consumer,
            String id,String structuralHash,String targetIssue){
        ResearchPlatformStoreR929.Authorization a=ResearchPlatformStoreR929.authorizeCurrent(store,consumer,id,structuralHash,targetIssue,.30d);
        return new Window(0,Double.NaN,Double.NaN,Double.NaN,a.reason,a.grantedWeight);
    }
    static Window fromCursor(Cursor c){
        java.util.ArrayList<Double> deltas=new java.util.ArrayList<Double>();
        java.util.ArrayList<Double> errors=new java.util.ArrayList<Double>();
        try{while(c.moveToNext()){
            deltas.add((double)(c.getInt(0)-c.getInt(1)));
            errors.add(Double.NaN);
        }}finally{c.close();}
        int n=deltas.size();
        double[] shadow=stats(deltas,errors,0,Math.min(120,n));
        double[] production=stats(deltas,errors,Math.min(120,n),n);
        double[] recent=stats(deltas,errors,Math.max(0,n-120),n);
        // These retained rows contain rank scores and rolling legacy baselines, not a frozen
        // paired trial with independently calibrated probabilities. Preserve diagnostics only.
        return new Window(n,n>120?production[0]:shadow[0],n>120?production[1]:shadow[1],Double.NaN,
                "LEGACY_REVIEW_RAW_SCORE_NOT_CALIBRATED",0);
    }
    static double[] stats(java.util.List<Double> d,java.util.List<Double> e,int from,int to){
        int n=to-from;double sum=0,bs=0;java.util.ArrayList<Double> blocks=new java.util.ArrayList<Double>();
        double part=0;int pn=0;
        for(int i=from;i<to;i++){sum+=d.get(i);bs+=e.get(i);part+=d.get(i);pn++;if(pn==20){blocks.add(part/20);part=0;pn=0;}}
        double lower=-1;
        if(blocks.size()>=6){double mean=0,var=0;for(double v:blocks)mean+=v;mean/=blocks.size();for(double v:blocks)var+=(v-mean)*(v-mean);
            lower=mean-2.571*Math.sqrt(var/((blocks.size()-1)*blocks.size()));}
        return new double[]{n==0?0:sum/n,lower,n==0?1:bs/n};
    }
    public static void revoke(SQLiteDatabase db,String id){
        db.execSQL("INSERT OR IGNORE INTO v57_promotion_history_r925 SELECT * FROM v57_promotion_window_r925 WHERE genome_id=?",new Object[]{id});
        db.execSQL("DELETE FROM v57_promotion_window_r925 WHERE genome_id=?",new Object[]{id});
    }
    public static final class Window{
        public final int n;public final double delta,lower,brier,weight;public final String stage;
        Window(int n,double d,double l,double b,String s,double w){this.n=n;delta=d;lower=l;brier=b;stage=s;weight=w;}
        public String summary(){return stage+"|newPairedN="+n+"|delta="+delta+"|blockLower="+lower+"|brier="+brier+"|weight="+weight;}
    }
}
