package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.*;

/** Preregistered research resource retirement. Never grants a certificate or claims child quality. */
final class ResearchPruningR965 {
    private static final int ENDPOINT=120;
    private ResearchPruningR965(){}
    private static String key(String id){return "r965_research_pruning_"+SourceManifestR815.SOURCE_TREE_SHA256+'_'+id;}
    static void register(SQLiteDatabase db,String id,String first) {
        if(!db.inTransaction())throw new IllegalStateException("R965_PRUNING_TRANSACTION");
        if(!read(db,key(id)).isEmpty())return;
        try {
            JSONObject p=new JSONObject();p.put("source",SourceManifestR815.SOURCE_TREE_SHA256);p.put("first_issue",first);
            p.put("endpoint",ENDPOINT);p.put("target_rate",CertificateBundleServiceR929.MIN_ENTRY_HIT_RATE);p.put("rule","WILSON_95_UPPER_BELOW_TARGET_AT_FIRST_120_VALID_LABELS");
            p.put("n",0);p.put("hits",0);p.put("last_issue","");p.put("state","COLLECTING");p.put("evidence",V51ChecksumEngine.sha256("R965_FIXED_RESEARCH_RETIREMENT|"+id+'|'+first+'|'+SourceManifestR815.SOURCE_TREE_SHA256));
            put(db,key(id),p.toString());
        }catch(JSONException e){throw new IllegalStateException(e);}
    }
    /** Called only after the original execution proof and exact outcome have been validated. */
    static void observe(SQLiteDatabase db,String id,String issue,int hit,String record,String source) {
        if(!db.inTransaction()||(hit!=0&&hit!=1))throw new IllegalArgumentException("R965_PRUNING_OUTCOME");
        if(!SourceManifestR815.SOURCE_TREE_SHA256.equals(source))return;
        String encoded=read(db,key(id));if(encoded.isEmpty())return; // Never preregister after observing the label.
        try {
            JSONObject p=new JSONObject(encoded);
            if(!p.getString("state").equals("COLLECTING")||issue.compareTo(p.getString("first_issue"))<0||issue.compareTo(p.getString("last_issue"))<=0)return;
            int n=p.getInt("n")+1,hits=p.getInt("hits")+hit;
            String evidence=V51ChecksumEngine.sha256(p.getString("evidence")+'|'+issue+'|'+record+'|'+hit);
            p.put("n",n);p.put("hits",hits);p.put("last_issue",issue);p.put("evidence",evidence);
            if(n==ENDPOINT) {
                double z=1.96,rate=hits/(double)n,upper=(rate+z*z/(2*n)+z*Math.sqrt(rate*(1-rate)/n+z*z/(4*n*n)))/(1+z*z/n);
                String state="";try(Cursor c=db.rawQuery("SELECT state FROM "+ResearchLifecycleR932.TABLE+" WHERE candidate_key=?",new String[]{"algorithm:"+id})){if(c.moveToFirst())state=c.getString(0);}
                boolean retire=upper<p.getDouble("target_rate")&&(state.equals("NURSERY")||state.equals("OBSERVING"));
                p.put("upper95",upper);p.put("state",retire?"RESEARCH_QUALITY_RETIRED":"ENDPOINT_RETAINED_NO_QUALITY_PROMOTION");
                if(retire)ResearchLifecycleR932.transition(db,"algorithm:"+id,ResearchLifecycleR932.State.QUALITY_PRUNED,
                        "R965_PREREGISTERED_RAW_RESEARCH_120|hits="+hits+"|upper95="+upper+"|target="+p.getDouble("target_rate")+"|NOT_CERTIFICATE_TEST",issue,evidence,n,0);
            }
            put(db,key(id),p.toString());
        }catch(JSONException error){throw new IllegalStateException("R965_PRUNING_CONTRACT",error);}
    }
    private static String read(SQLiteDatabase db,String key){try(Cursor c=db.rawQuery("SELECT state_value FROM "+DbHelper.TABLE_APP_STATE+" WHERE state_key=?",new String[]{key})){return c.moveToFirst()?c.getString(0):"";}}
    private static void put(SQLiteDatabase db,String key,String value){db.execSQL("INSERT OR REPLACE INTO "+DbHelper.TABLE_APP_STATE+"(state_key,state_value,updated_at) VALUES(?,?,?)",new Object[]{key,value,System.currentTimeMillis()});}
}
