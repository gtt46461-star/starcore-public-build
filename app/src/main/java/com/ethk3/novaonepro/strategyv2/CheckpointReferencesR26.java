package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;
/** One reference census shared by checkpoint GC and fixed-snapshot full backups. */
public final class CheckpointReferencesR26 {
    private CheckpointReferencesR26() {}
    private static boolean table(SQLiteDatabase db,String name){try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{name})){return c.moveToFirst();}}
    private static void add(Set<String> hashes,String hash){if(hash==null||hash.isEmpty())return;if(!hash.matches("[0-9a-f]{64}"))throw new IllegalStateException("R26_CHECKPOINT_REFERENCE_HASH");hashes.add(hash);}
    private static void artifactColumnR31(SQLiteDatabase db,Set<String> refs,String table,String column,boolean requiredReference){
        if(!table.matches("[A-Za-z_][A-Za-z0-9_]*")||!column.matches("[A-Za-z_][A-Za-z0-9_]*"))throw new IllegalStateException("R31_CAS_ROOT_IDENTIFIER");
        Set<String> columns=new HashSet<>();try(Cursor info=db.rawQuery("PRAGMA table_info(\""+table+"\")",null)){while(info.moveToNext())columns.add(info.getString(1));}if(!columns.contains(column))throw new IllegalStateException("R31_CAS_ROOT_CENSUS_UNKNOWN:"+table+":"+column);
        // Only a bounded prefix/hash enters CursorWindow, never an old 32MiB
        // inline checkpoint just to decide whether it is a file reference.
        String blob="CAST(\""+column+"\" AS BLOB)",prefix="substr("+blob+",1,16)",reference="CAST(substr("+blob+",9,64) AS TEXT)";
        String where=requiredReference?"\""+column+"\" IS NOT NULL":"CAST("+prefix+" AS TEXT) GLOB 'R[0-9]*CAS*'";
        try(Cursor c=db.rawQuery("SELECT DISTINCT "+prefix+",length("+blob+"),"+reference+" FROM \""+table+"\" WHERE "+where+" LIMIT "+(CheckpointGcStoreR31.MAX_NODES+1),null)){int count=0;while(c.moveToNext()){
            if(++count>CheckpointGcStoreR31.MAX_NODES)throw new IllegalStateException("R31_CAS_ROOT_CENSUS_BOUND");byte[] header=c.getBlob(0);String text=header==null?"":new String(header,java.nio.charset.StandardCharsets.US_ASCII);
            if(c.getLong(1)==72&&text.startsWith("R26CAS1:")){add(refs,c.getString(2));continue;}
            if(requiredReference||text.matches("R[0-9]+CAS.*"))throw new IllegalStateException("R31_CAS_ROOT_VERSION_UNKNOWN:"+table+":"+column);
        }}
    }
    public static Set<String> hashes(SQLiteDatabase db){
        Set<String> refs=new TreeSet<>();String[] states={"r26_live_model","r26_candidate","r26_warm_initialization","r26_expert_experiment","r26_legacy_runtime","r26_member_replay","r26_prepared_policy"};
        for(String state:states){if(table(db,state))artifactColumnR31(db,refs,state,"state",false);if(table(db,"r30_keep_"+state))artifactColumnR31(db,refs,"r30_keep_"+state,"state",false);}
        if(table(db,"r26_replay_output_chunk"))artifactColumnR31(db,refs,"r26_replay_output_chunk","artifact_ref",false);
        if(table(db,"r26_warm_initialization"))for(String state:new String[]{"r26_candidate","r26_live_model"})if(table(db,state))try(Cursor c=db.rawQuery("SELECT policy_hash FROM "+state+" s WHERE length(s.state)>0 AND instr(s.canonical_text,'_warm_kernel=')>0 AND NOT EXISTS(SELECT 1 FROM r26_warm_initialization i WHERE i.policy_hash=s.policy_hash) LIMIT 1",null)){if(c.moveToFirst())throw new IllegalStateException("R26_WARM_INITIALIZER_MANIFEST_MISSING:"+c.getString(0));}
        List<String> parents=new ArrayList<>();for(String state:new String[]{"r26_live_model","r26_legacy_runtime","r26_expert_experiment"})if(table(db,state))parents.add("SELECT parent_hash FROM "+state+" WHERE parent_hash<>''"+((state.equals("r26_expert_experiment")||state.equals("r26_legacy_runtime"))?" AND cursor>0 AND length(state)>0":""));
        if(!parents.isEmpty()&&table(db,"r26_checkpoint_artifact"))try(Cursor c=db.rawQuery("SELECT artifact_hash FROM r26_checkpoint_artifact WHERE checkpoint_hash IN ("+String.join(" UNION ",parents)+")",null)){while(c.moveToNext())add(refs,c.getString(0));}
        // An unretired manifest still promises original bytes. Its existing
        // owner must retire the row before file GC may retire that artifact.
        if(table(db,"r26_checkpoint_artifact"))try(Cursor c=db.rawQuery("SELECT artifact_hash FROM r26_checkpoint_artifact",null)){while(c.moveToNext())add(refs,c.getString(0));}
        // Register every r29/r30 root before publishing the first reference. Includes protected upgrade archives
        // and PREPARED manifests, so process death cannot make their still-needed children garbage.
        Map<String,String> columns=new LinkedHashMap<>();columns.put("r29_model_checkpoint","artifact");columns.put("r29_legacy_model_checkpoint","artifact");columns.put("r30_model_artifact","weights");columns.put("r30_atomnet_dataset","payload");
        for(Map.Entry<String,String> item:columns.entrySet()){if(table(db,item.getKey()))artifactColumnR31(db,refs,item.getKey(),item.getValue(),false);String keep="r30_keep_"+item.getKey();if(table(db,keep))artifactColumnR31(db,refs,keep,item.getValue(),false);}
        if(table(db,"r30_retention_archive_index"))try(Cursor c=db.rawQuery("SELECT source_table,archive_table FROM r30_retention_archive_index",null)){while(c.moveToNext()){String column=columns.get(c.getString(0));if(column==null)continue;String archive=c.getString(1);if(archive==null||!archive.matches("r30_keep_[A-Za-z0-9_]+")||!table(db,archive))throw new IllegalStateException("R31_CAS_ARCHIVE_ROOT_CENSUS_UNKNOWN");artifactColumnR31(db,refs,archive,column,false);}}
        if(table(db,"r31_atomnet_publication"))for(String column:new String[]{"state_ref","weights_ref","dataset_ref"})artifactColumnR31(db,refs,"r31_atomnet_publication",column,true);
        if(table(db,"r31_gc_pin"))try(Cursor c=db.rawQuery("SELECT artifact_hash FROM r31_gc_pin",null)){while(c.moveToNext())add(refs,c.getString(0));}
        for(String hash:KeepMigrationStoreR31.checkpointArtifactHashes(db))add(refs,hash);
        return Collections.unmodifiableSet(refs);
    }
}
