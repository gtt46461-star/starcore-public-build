package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;

/** Append-only observations of the sole decision owner. Never authorizes or edits an action. */
public final class DecisionExplanationStoreR30 {
    private DecisionExplanationStoreR30() {}
    public static final String[] DDL={
        "CREATE TABLE IF NOT EXISTS r30_decision_explain(issue TEXT NOT NULL,decision_key TEXT NOT NULL,entry_point TEXT NOT NULL,original_allow INTEGER NOT NULL,explain_allow INTEGER NOT NULL,original_reason TEXT NOT NULL,certificate_active INTEGER,first_blocker TEXT,items_json TEXT NOT NULL,source_json TEXT NOT NULL,recorded_at INTEGER NOT NULL,PRIMARY KEY(issue,decision_key))",
        "CREATE TABLE IF NOT EXISTS r30_decision_mismatch(issue TEXT NOT NULL,decision_key TEXT NOT NULL,original_allow INTEGER NOT NULL,explain_allow INTEGER NOT NULL,direction TEXT NOT NULL,original_reason TEXT NOT NULL,first_blocker TEXT,recorded_at INTEGER NOT NULL,PRIMARY KEY(issue,decision_key))",
        "CREATE INDEX IF NOT EXISTS r30_decision_explain_time ON r30_decision_explain(recorded_at,issue)"
    };
    public static final class Observation {
        public final String issue,key,entry,originalReason,firstBlocker,items,sources,direction;
        public final boolean originalAllow,explainAllow; public final Integer certificateActive; public final long at;
        Observation(String issue,String entry,String identity,boolean originalAllow,String reason,DecisionChainR30.Facts facts,Map<String,Object> sources,long at) {
            DecisionChainR30.Verdict v=DecisionChainR30.explain(facts);
            this.issue=issue;this.entry=entry;this.originalAllow=originalAllow;originalReason=reason==null?"":reason;
            explainAllow=v.allowReal;firstBlocker=v.firstBlocker;items=JsonR27.write(v.items);this.sources=JsonR27.write(sources);this.at=at;
            certificateActive=facts.certificateActive==null?null:facts.certificateActive?1:0;
            direction=originalAllow==explainAllow?null:originalAllow?"ORIGINAL_MORE_PERMISSIVE":"ORIGINAL_STRICTER";
            key=StatsR27.sha256(entry+"|"+identity+"|"+originalAllow+"|"+originalReason+"|"+items+"|"+this.sources);
        }
    }
    public static Observation observe(String issue,String entry,String identity,boolean originalAllow,String originalReason,DecisionChainR30.Facts facts,Map<String,Object> sources,long at) {
        if(issue==null||issue.isEmpty())throw new IllegalArgumentException("R30_EXPLAIN_TARGET_REQUIRED");
        return new Observation(issue,entry,identity,originalAllow,originalReason,facts,sources,at);
    }
    /** Only already prepared strings/values enter the existing single writer. */
    public static ResearchWriteQueueR27.Batch batch(Observation o) {
        ResearchWriteQueueR27.Batch b=new ResearchWriteQueueR27.Batch();
        b.add("INSERT OR IGNORE INTO r30_decision_explain(issue,decision_key,entry_point,original_allow,explain_allow,original_reason,certificate_active,first_blocker,items_json,source_json,recorded_at) VALUES(?,?,?,?,?,?,?,?,?,?,?)",o.issue,o.key,o.entry,o.originalAllow?1:0,o.explainAllow?1:0,o.originalReason,o.certificateActive,o.firstBlocker,o.items,o.sources,o.at);
        if(o.direction!=null)b.add("INSERT OR IGNORE INTO r30_decision_mismatch(issue,decision_key,original_allow,explain_allow,direction,original_reason,first_blocker,recorded_at) VALUES(?,?,?,?,?,?,?,?)",o.issue,o.key,o.originalAllow?1:0,o.explainAllow?1:0,o.direction,o.originalReason,o.firstBlocker,o.at);
        return b;
    }
    public static void persistWithin(SQLiteDatabase db,Observation observation) throws Exception {
        ResearchWriteQueueR27.execute(db,batch(observation),"R30_DECISION_EXPLAIN");
    }
    public static Map<String,Object> report(SQLiteDatabase db) {
        Map<String,Object> out=new LinkedHashMap<>();
        if(!table(db,"r30_decision_explain")){out.put("status","UNKNOWN_SOURCE_MISSING");out.put("decisions",null);out.put("mismatches",null);return out;}
        long n=count(db,"SELECT count(*) FROM r30_decision_explain");out.put("status",n==0?"NOT_RUN":"RECORDED");out.put("decisions",n);
        out.put("mismatches",count(db,"SELECT count(*) FROM r30_decision_mismatch"));
        out.put("originalMorePermissive",count(db,"SELECT count(*) FROM r30_decision_mismatch WHERE direction='ORIGINAL_MORE_PERMISSIVE'"));
        out.put("originalStricter",count(db,"SELECT count(*) FROM r30_decision_mismatch WHERE direction='ORIGINAL_STRICTER'"));
        Map<String,Object> blockers=new LinkedHashMap<>();
        try(Cursor c=db.rawQuery("SELECT coalesce(first_blocker,'NONE'),count(*) FROM r30_decision_explain WHERE certificate_active=1 AND original_allow=0 GROUP BY first_blocker ORDER BY count(*) DESC,first_blocker",null)){while(c.moveToNext())blockers.put(c.getString(0),c.getLong(1));}
        out.put("certificatePresentButNoReal",blockers);List<Object> recent=new ArrayList<>();
        try(Cursor c=db.rawQuery("SELECT issue,entry_point,original_allow,explain_allow,original_reason,first_blocker,items_json,source_json,recorded_at FROM r30_decision_explain ORDER BY recorded_at DESC,issue DESC LIMIT 200",null)){
            while(c.moveToNext()){Map<String,Object> row=new LinkedHashMap<>();row.put("issue",c.getString(0));row.put("entryPoint",c.getString(1));row.put("originalAllow",c.getInt(2)==1);row.put("explainAllow",c.getInt(3)==1);row.put("originalReason",c.getString(4));row.put("firstBlocker",c.getString(5));row.put("items",JsonR27.parse(c.getString(6)));row.put("sources",JsonR27.parse(c.getString(7)));row.put("recordedAt",c.getLong(8));recent.add(row);}
        }
        out.put("recent",recent);out.put("recentLimit",200);out.put("authority","ORIGINAL_DECISION_UNCHANGED");return out;
    }
    static boolean table(SQLiteDatabase db,String name){try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{name})){return c.moveToFirst();}}
    static long count(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
}
