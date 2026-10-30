package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;
/** One reference census shared by checkpoint GC and fixed-snapshot full backups. */
public final class CheckpointReferencesR26 {
    private CheckpointReferencesR26() {}
    private static boolean table(SQLiteDatabase db,String schema,String name){try(Cursor c=db.rawQuery("SELECT 1 FROM "+schema+".sqlite_master WHERE type='table' AND name=?",new String[]{name})){return c.moveToFirst();}}
    private static void add(Set<String> hashes,String hash){if(hash==null||hash.isEmpty())return;if(!hash.matches("[0-9a-f]{64}"))throw new IllegalStateException("R26_CHECKPOINT_REFERENCE_HASH");hashes.add(hash);}
    public static Set<String> hashes(SQLiteDatabase db){
        Set<String> refs=new TreeSet<>();collect(db,refs);
        if(ResearchRouteR29.isResearchDatabase(db)){
            SQLiteDatabase realtime=ResearchRouteR29.realtimeForWriter(db);
            if(realtime!=db)collect(realtime,refs);
            else try(Cursor c=db.rawQuery("PRAGMA database_list",null)){while(c.moveToNext())if("realtime".equals(c.getString(1)))collectAttachedRealtime(db,refs);}
        }else if(ResearchRouteR29.active(db))collect(ResearchRouteR29.reader(db),refs);
        return Collections.unmodifiableSet(refs);
    }
    private static void collectAttachedRealtime(SQLiteDatabase db,Set<String> refs){collect(db,refs,"realtime");}
    private static void collect(SQLiteDatabase db,Set<String> refs){collect(db,refs,"main");}
    private static void collect(SQLiteDatabase db,Set<String> refs,String schema){
        // Only fixed schema identifiers are accepted; neither input nor filenames are interpolated as SQL.
        if(!schema.equals("main")&&!schema.equals("realtime"))throw new IllegalArgumentException("R29_REFERENCE_SCHEMA");String prefix=schema+".";
        String[] states={"r26_live_model","r26_candidate","r26_warm_initialization","r26_expert_experiment","r26_legacy_runtime","r26_member_replay","r26_prepared_policy"};
        for(String state:states)if(table(db,schema,state))try(Cursor c=db.rawQuery("SELECT state FROM "+prefix+state+" WHERE length(state)=72",null)){while(c.moveToNext())add(refs,CheckpointArtifactsR26.referenceHash(c.getBlob(0)));}
        if(table(db,schema,"r29_legacy_checkpoint_pin"))try(Cursor c=db.rawQuery("SELECT state_ref FROM "+prefix+"r29_legacy_checkpoint_pin WHERE length(state_ref)=72",null)){while(c.moveToNext())add(refs,CheckpointArtifactsR26.referenceHash(c.getBlob(0)));}
        if(table(db,schema,"r26_replay_output_chunk"))try(Cursor c=db.rawQuery("SELECT artifact_ref FROM "+prefix+"r26_replay_output_chunk",null)){while(c.moveToNext())add(refs,CheckpointArtifactsR26.referenceHash(c.getBlob(0)));}
        if(table(db,schema,"r26_warm_initialization"))for(String state:new String[]{"r26_candidate","r26_live_model"})if(table(db,schema,state))try(Cursor c=db.rawQuery("SELECT policy_hash FROM "+prefix+state+" s WHERE length(s.state)>0 AND instr(s.canonical_text,'_warm_kernel=')>0 AND NOT EXISTS(SELECT 1 FROM "+prefix+"r26_warm_initialization i WHERE i.policy_hash=s.policy_hash) LIMIT 1",null)){if(c.moveToFirst())throw new IllegalStateException("R26_WARM_INITIALIZER_MANIFEST_MISSING:"+c.getString(0));}
        List<String> parents=new ArrayList<>();for(String state:new String[]{"r26_live_model","r26_legacy_runtime","r26_expert_experiment"})if(table(db,schema,state))parents.add("SELECT parent_hash FROM "+prefix+state+" WHERE parent_hash<>''"+((state.equals("r26_expert_experiment")||state.equals("r26_legacy_runtime"))?" AND cursor>0 AND length(state)>0":""));
        if(!parents.isEmpty()&&table(db,schema,"r26_checkpoint_artifact"))try(Cursor c=db.rawQuery("SELECT artifact_hash FROM "+prefix+"r26_checkpoint_artifact WHERE checkpoint_hash IN ("+String.join(" UNION ",parents)+")",null)){while(c.moveToNext())add(refs,c.getString(0));}
    }
}
