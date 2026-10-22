package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.nio.charset.StandardCharsets;
import java.util.*;

/** SQL publication only. Compression, scoring and training happen before entering the writer. */
public final class StrategyRepositorySU1 {
    private StrategyRepositorySU1() {}
    public static final String ACTION=HashingSU1.text("SU1_ACTION_HEAD_V2|ONE_POSITION|FIXED_K3|FLOAT64_MASS_ROUND_1E9|LOW_POSITION_TIE");
    public static final String UPDATE=HashingSU1.text("SU1_WINDOW_RESET_V2|DEEP_SGD_CAUSAL_BLOCK50|HEDGE_SETTLED_PREFIX|NO_PARENT_EVIDENCE_INHERITANCE");
    public static final class Member {
        public final String id,text,arm,parent;
        public final double coverage,fitness;
        public final boolean margin;
        Member(String id,String text,double coverage,boolean margin,String arm,String parent,double fitness){this.id=id;this.text=text;this.coverage=coverage;this.margin=margin;this.arm=arm;this.parent=parent;this.fitness=fitness;}
    }
    public static List<Member> active(SQLiteDatabase db){
        List<Member> out=new ArrayList<>();
        try(Cursor c=db.rawQuery("SELECT p.strategy_id,r.canonical_text,r.coverage,r.margin_gate,p.arm,r.parent_id,p.prior_fitness FROM su1_research_pool p JOIN su1_runtime_program r ON r.strategy_id=p.strategy_id WHERE p.state='ACTIVE' ORDER BY p.prior_fitness DESC,p.strategy_id LIMIT 32",null)){
            while(c.moveToNext())out.add(new Member(c.getString(0),c.getString(1),c.getDouble(2),c.getInt(3)==1,c.getString(4),c.getString(5),c.getDouble(6)));
        }
        return out;
    }
    public static String blob(SQLiteDatabase db,String payload,String codec,long now){
        String hash=HashingSU1.text(payload);
        try(Cursor c=db.rawQuery("SELECT byte_count,codec,local_locator FROM su1_blob WHERE blob_hash=?",new String[]{hash})){
            if(c.moveToFirst()){
                if(c.getLong(0)!=payload.getBytes(StandardCharsets.UTF_8).length||!codec.equals(c.getString(1))||!payload.equals(c.getString(2)))throw new IllegalStateException("SU1_BLOB_CONFLICT");
                return hash;
            }
        }
        db.execSQL("INSERT INTO su1_blob(blob_hash,byte_count,codec,storage_state,local_locator,created_at) VALUES(?,?,?,'LOCAL',?,?)",
                new Object[]{hash,payload.getBytes(StandardCharsets.UTF_8).length,codec,payload,now});
        return hash;
    }
    public static void snapshot(SQLiteDatabase db,EventFrameSU1 frame,SnapshotPayloadSU1 preparedPayload){
        long now=System.currentTimeMillis();
        for(Map.Entry<String,String> chunk:preparedPayload.chunks.entrySet())blob(db,chunk.getValue(),"SU1_PIT_CHUNK_GZIP_BASE64_V1",now);
        String payload=blob(db,preparedPayload.manifest,"SU1_PIT_CHUNKS64_MANIFEST_V1",now);
        if(exists(db,"SELECT 1 FROM su1_snapshot WHERE snapshot_hash=?",frame.contentHash))return;
        db.execSQL("INSERT INTO su1_snapshot VALUES(?,?,?,?,?,?,?,?,?,?)",new Object[]{frame.contentHash,"Binance1-PK10",frame.targetIssue,frame.targetOpenTime,frame.sourceMaxIssue,frame.sourceAvailableAt,frame.cutoff,frame.sourceEpoch,frame.sourceVersionHash,payload});
        dependency(db,"SNAPSHOT",frame.contentHash,"BLOB",payload,"PIT_MANIFEST",now);
        for(String chunk:preparedPayload.chunks.keySet())dependency(db,"BLOB",payload,"BLOB",chunk,"PIT_CHUNK",now);
    }
    public static void dependency(SQLiteDatabase db,String sourceKind,String source,String targetKind,String target,String reason,long now){
        if(!exists(db,"SELECT 1 FROM su1_dependency WHERE source_kind=? AND source_id=? AND target_kind=? AND target_id=?",sourceKind,source,targetKind,target))db.execSQL("INSERT INTO su1_dependency VALUES(?,?,?,?,?,?)",new Object[]{sourceKind,source,targetKind,target,reason,now});
    }
    public static void row(SQLiteDatabase db,String table,Object... fields){
        if(!table.matches("su1_[a-z0-9_]+")||fields.length%2!=0)throw new IllegalArgumentException("SU1_ROW");
        android.content.ContentValues values=new android.content.ContentValues();for(int i=0;i<fields.length;i+=2){String key=(String)fields[i];Object v=fields[i+1];if(v==null)values.putNull(key);else if(v instanceof String)values.put(key,(String)v);else if(v instanceof Double)values.put(key,(Double)v);else if(v instanceof Integer)values.put(key,(Integer)v);else if(v instanceof Long)values.put(key,(Long)v);else throw new IllegalArgumentException("SU1_ROW_VALUE");}db.insertOrThrow(table,null,values);
    }
    public static String beginCampaign(SQLiteDatabase db,String kind,long through,String config,long now){
        String configHash=HashingSU1.text(config),id=HashingSU1.fields("SU1_CAMPAIGN_V1",kind,String.valueOf(through),configHash);
        if(!exists(db,"SELECT 1 FROM su1_campaign WHERE campaign_id=?",id))db.execSQL("INSERT INTO su1_campaign(campaign_id,kind,data_through_sequence,config_hash,config_json,started_at,status) VALUES(?,?,?,?,?,?,'RUNNING')",new Object[]{id,kind,through,configHash,config,now});
        return id;
    }
    public static String candidate(SQLiteDatabase db,String text,double coverage,boolean margin,String arm,String parent,
            String campaign,long through,String datasetHash,double fitness,long now){
        text=ProgramTextSU1.canonical(text);if(!Double.isFinite(fitness)||coverage<=0||coverage>1)throw new IllegalArgumentException("SU1_CANDIDATE");
        String program=HashingSU1.text(ProgramTreeSU1.PROTOCOL+"|"+text);
        String ir=blob(db,text,"SU1_GRAMMAR_TEXT_V1",now),weights=blob(db,"PARAMETER_FREE","SU1_PARAMETER_FREE_V1",now);
        String report=blob(db,"STRUCTURE_SELECTION|"+datasetHash+"|"+through+"|"+Double.toHexString(fitness),"SU1_SEARCH_RECEIPT_V1",now);
        if(!exists(db,"SELECT 1 FROM su1_program WHERE program_hash=?",program))db.execSQL("INSERT INTO su1_program VALUES(?,?,?,?,?,?,?,?)",new Object[]{program,HashingSU1.text(ProgramTreeSU1.PROTOCOL),HashingSU1.text("PK10_10X10_PIT_V1"),ir,UPDATE,0,1000000,now});
        long trainLast=Math.max(1,through-1000),valFirst=trainLast+1,valLast=Math.max(valFirst,through);
        String protocolJson="{\"kind\":\"PARAMETER_FREE_STRUCTURE_SEARCH\",\"dataset\":\""+datasetHash+"\",\"trainLast\":"+trainLast+",\"validationFirst\":"+valFirst+",\"validationLast\":"+valLast+"}";
        String protocol=HashingSU1.fields("SU1_SEARCH_PROTOCOL_V1",program,protocolJson);
        if(!exists(db,"SELECT 1 FROM su1_train_protocol WHERE protocol_hash=?",protocol))db.execSQL("INSERT INTO su1_train_protocol VALUES(?,?,?,?,?,?,?,?,?,?,?)",new Object[]{protocol,program,datasetHash,HashingSU1.fields(String.valueOf(trainLast),String.valueOf(valLast)),protocolJson,Math.max(1,trainLast-3200),trainLast,valFirst,valLast,ACTION,now});
        String artifact=HashingSU1.fields("SU1_INTERPRETED_ARTIFACT_V1",protocol,program,weights,report);
        if(!exists(db,"SELECT 1 FROM su1_model_artifact WHERE artifact_hash=?",artifact))db.execSQL("INSERT INTO su1_model_artifact VALUES(?,?,?,?,?,?,?,?,?)",new Object[]{artifact,protocol,program,"GRAMMAR_INTERPRETED",HashingSU1.text("ATOM_EVALUATOR_CP4_V1"),weights,through,report,now});
        String selector=HashingSU1.fields("SU1_CAUSAL_QUANTILE_V1",Double.toHexString(coverage),String.valueOf(margin),"720|50|LINEAR");
        String id=new StrategyIdentitySU1(program,protocol,artifact,UPDATE,selector,HashingSU1.text("CALIBRATION_MISSING"),HashingSU1.fields("SU1_ATOM_CONDITION_V2_1E12|JAVA17_FLOAT32_NODES_V1",com.ethk3.novaonepro.TrainingCodeIdentityR964.SHA256)).hash;
        if(!exists(db,"SELECT 1 FROM su1_strategy_version WHERE strategy_id=?",id)){
            db.execSQL("INSERT INTO su1_strategy_version(strategy_id,codec,program_hash,artifact_hash,selector_hash,calibration_hash,calibration_state,action_contract_hash,state_initial_hash,created_sequence,created_at,strategy_role) VALUES(?,'SU1',?,?,?,NULL,'MISSING',?,?,?,?,'CANDIDATE')",new Object[]{id,program,artifact,selector,ACTION,UPDATE,through,now});
            db.execSQL("INSERT INTO su1_runtime_program VALUES(?,?,?,?,?,?)",new Object[]{id,text,coverage,margin?1:0,parent,now});
        }
        if(!exists(db,"SELECT 1 FROM su1_research_pool WHERE strategy_id=? AND state='ACTIVE'",id)){
            int n=count(db,"SELECT count(*) FROM su1_research_pool WHERE state='ACTIVE'");
            if(n>=32){db.execSQL("UPDATE su1_research_pool SET state='RETIRED',retired_sequence=?,retire_reason='POOL_RESOURCE_CAP' WHERE entry_id=(SELECT entry_id FROM su1_research_pool WHERE state='ACTIVE' ORDER BY prior_fitness,entry_id LIMIT 1)",new Object[]{through+1});}
            String entry=HashingSU1.fields("SU1_POOL_V1",id,campaign,String.valueOf(through+1));
            if(!exists(db,"SELECT 1 FROM su1_research_pool WHERE entry_id=?",entry))db.execSQL("INSERT INTO su1_research_pool(entry_id,strategy_id,campaign_id,arm,born_sequence,prior_fitness,state,created_at) VALUES(?,?,?,?,?,?,'ACTIVE',?)",new Object[]{entry,id,campaign,arm,through+1,fitness,now});
        }
        return id;
    }
    public static boolean exists(SQLiteDatabase db,String sql,String... args){try(Cursor c=db.rawQuery(sql,args)){return c.moveToFirst();}}
    public static int count(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new IllegalStateException("SU1_COUNT");return c.getInt(0);}}
    public static void health(SQLiteDatabase db,String phase,String detail,long micros){db.execSQL("INSERT INTO su1_health_event(phase,detail,elapsed_micros,created_at) VALUES(?,?,?,?)",new Object[]{phase,detail,micros,System.currentTimeMillis()});db.execSQL("DELETE FROM su1_health_event WHERE id<(SELECT max(id)-512 FROM su1_health_event)");}
}
