package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;
import java.util.function.Consumer;

/** One validated root census for GC and fixed-snapshot backups.
 * R32.4 streams compact references; cumulative publications have no Java node cap.
 * Unknown codecs, columns or archive declarations still preserve all originals. */
public final class CheckpointReferencesR26 {
    private CheckpointReferencesR26() {}
    private static boolean table(SQLiteDatabase db,String name) {
        try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{name})) { return c.moveToFirst(); }
    }
    private static String identifier(String name) {
        if(name==null||!name.matches("[A-Za-z_][A-Za-z0-9_]*")) throw new IllegalStateException("R31_CAS_ROOT_IDENTIFIER");
        return "\""+name+"\"";
    }
    private static void column(SQLiteDatabase db,String table,String name) {
        try(Cursor c=db.rawQuery("PRAGMA table_info("+identifier(table)+")",null)) {
            while(c.moveToNext()) if(name.equals(c.getString(1))) return;
        }
        throw new IllegalStateException("R31_CAS_ROOT_CENSUS_UNKNOWN:"+table+":"+name);
    }
    private static void emit(Consumer<String> sink,String hash) {
        if(hash==null||hash.isEmpty()) return;
        if(!hash.matches("[0-9a-f]{64}")) throw new IllegalStateException("R26_CHECKPOINT_REFERENCE_HASH");
        sink.accept(hash);
    }
    private static void artifact(SQLiteDatabase db,List<String> queries,String table,String name,boolean required) {
        identifier(table);identifier(name);column(db,table,name);
        String value="CAST("+identifier(name)+" AS BLOB)", text="CAST(substr("+value+",1,16) AS TEXT)";
        String considered=required?identifier(name)+" IS NOT NULL":text+" GLOB 'R[0-9]*CAS*'";
        String ref="CAST(substr("+value+",9,64) AS TEXT)";
        String valid="length("+value+")=72 AND CAST(substr("+value+",1,8) AS TEXT)='R26CAS1:' AND length("+ref+")=64 AND "+ref+" NOT GLOB '*[^0-9a-f]*'";
        try(Cursor c=db.rawQuery("SELECT 1 FROM "+identifier(table)+" WHERE ("+considered+") AND NOT ("+valid+") LIMIT 1",null)) {
            if(c.moveToFirst()) throw new IllegalStateException("R31_CAS_ROOT_VERSION_UNKNOWN:"+table+":"+name);
        }
        queries.add("SELECT "+ref+" AS hash FROM "+identifier(table)+" WHERE "+considered);
    }
    private static void direct(SQLiteDatabase db,List<String> queries,String table,String name) {
        column(db,table,name);String value=identifier(name);
        try(Cursor c=db.rawQuery("SELECT 1 FROM "+identifier(table)+" WHERE "+value+" IS NOT NULL AND "+value+"<>'' AND (length("+value+")<>64 OR "+value+" GLOB '*[^0-9a-f]*') LIMIT 1",null)) {
            if(c.moveToFirst()) throw new IllegalStateException("R26_CHECKPOINT_REFERENCE_HASH");
        }
        queries.add("SELECT "+value+" AS hash FROM "+identifier(table)+" WHERE "+value+" IS NOT NULL AND "+value+"<>''");
    }
    /** Projection SQL contains identifiers from the validated finite source registry only.
     * Each row is a 64-byte hash; it never reads an inline model into CursorWindow. */
    static List<String> referenceQueriesR324(SQLiteDatabase db) {
        List<String> queries=new ArrayList<>();
        String[] states={"r26_live_model","r26_candidate","r26_warm_initialization","r26_expert_experiment","r26_legacy_runtime","r26_member_replay","r26_prepared_policy"};
        for(String state:states) {
            if(table(db,state)) artifact(db,queries,state,"state",false);
            if(table(db,"r30_keep_"+state)) artifact(db,queries,"r30_keep_"+state,"state",false);
        }
        if(table(db,"r26_replay_output_chunk")) artifact(db,queries,"r26_replay_output_chunk","artifact_ref",false);
        if(table(db,"r26_warm_initialization")) for(String state:new String[]{"r26_candidate","r26_live_model"}) if(table(db,state)) {
            try(Cursor c=db.rawQuery("SELECT policy_hash FROM "+state+" s WHERE length(s.state)>0 AND instr(s.canonical_text,'_warm_kernel=')>0 AND NOT EXISTS(SELECT 1 FROM r26_warm_initialization i WHERE i.policy_hash=s.policy_hash) LIMIT 1",null)) {
                if(c.moveToFirst()) throw new IllegalStateException("R26_WARM_INITIALIZER_MANIFEST_MISSING:"+c.getString(0));
            }
        }
        // All unretired checkpoint manifests include the parents previously selected by UNION.
        if(table(db,"r26_checkpoint_artifact")) direct(db,queries,"r26_checkpoint_artifact","artifact_hash");
        Map<String,String> columns=new LinkedHashMap<>();
        columns.put("r29_model_checkpoint","artifact");columns.put("r29_legacy_model_checkpoint","artifact");
        columns.put("r30_model_artifact","weights");columns.put("r30_atomnet_dataset","payload");
        Set<String> declared=new HashSet<>();
        for(Map.Entry<String,String> item:columns.entrySet()) {
            if(table(db,item.getKey())) artifact(db,queries,item.getKey(),item.getValue(),false);
            String keep="r30_keep_"+item.getKey();
            if(table(db,keep)) { artifact(db,queries,keep,item.getValue(),false);declared.add(keep); }
        }
        if(table(db,"r30_retention_archive_index")) try(Cursor c=db.rawQuery("SELECT source_table,archive_table FROM r30_retention_archive_index",null)) {
            while(c.moveToNext()) {
                String col=columns.get(c.getString(0));if(col==null) continue;
                String archive=c.getString(1);
                if(archive==null||!archive.matches("r30_keep_[A-Za-z0-9_]+")||!table(db,archive)) throw new IllegalStateException("R31_CAS_ARCHIVE_ROOT_CENSUS_UNKNOWN");
                if(declared.add(archive)) artifact(db,queries,archive,col,false);
            }
        }
        if(table(db,"r31_atomnet_publication")) for(String name:new String[]{"state_ref","weights_ref","dataset_ref"}) artifact(db,queries,"r31_atomnet_publication",name,true);
        if(table(db,"r324_atomnet_sample_archive")) artifact(db,queries,"r324_atomnet_sample_archive","archive_ref",true);
        if(table(db,"r31_gc_pin")) direct(db,queries,"r31_gc_pin","artifact_hash");
        if(table(db,"r335_checkpoint_recovery")) direct(db,queries,"r335_checkpoint_recovery","expected_hash");
        return queries;
    }
    static void visitKeepR324(SQLiteDatabase db,Consumer<String> sink) {
        if(!table(db,"r31_keep_original")) return;
        column(db,"r31_keep_original","checkpoint_refs_json");
        try(Cursor c=db.rawQuery("SELECT checkpoint_refs_json FROM r31_keep_original",null)) {
            while(c.moveToNext()) {
                ExecutionBudgetR26.poll();String json=c.getString(0);
                if(json==null||json.length()>65536) throw new IllegalStateException("R31_KEEP_ROOT_CODEC_BOUND");
                Object parsed=JsonR27.parse(json);
                if(!(parsed instanceof List)) throw new IllegalStateException("R31_KEEP_ROOT_CODEC");
                for(Object hash:(List<?>)parsed) {
                    if(!(hash instanceof String)||!((String)hash).matches("[0-9a-f]{64}")) throw new IllegalStateException("R31_KEEP_ROOT_HASH");
                    sink.accept((String)hash);
                }
            }
        }
    }
    public static void forEachHashR324(SQLiteDatabase db,Consumer<String> sink) {
        Objects.requireNonNull(sink,"reference consumer");
        for(String query:referenceQueriesR324(db)) try(Cursor c=db.rawQuery(query,null)) {
            while(c.moveToNext()) { ExecutionBudgetR26.poll();emit(sink,c.getString(0)); }
        }
        visitKeepR324(db,sink);
    }
    /** Compatibility API for existing snapshot owners. Live GC uses streaming/query APIs. */
    public static Set<String> hashes(SQLiteDatabase db) {
        Set<String> refs=new TreeSet<>();forEachHashR324(db,refs::add);return Collections.unmodifiableSet(refs);
    }
    static boolean directlyReferencedR324(SQLiteDatabase db,String hash) {
        if(hash==null||!hash.matches("[0-9a-f]{64}")) throw new IllegalArgumentException("R31_GC_ARTIFACT_HASH");
        List<String> queries=referenceQueriesR324(db);
        if(!queries.isEmpty()) try(Cursor c=db.rawQuery("SELECT 1 FROM ("+String.join(" UNION ALL ",queries)+") WHERE hash=? LIMIT 1",new String[]{hash})) {
            if(c.moveToFirst()) return true;
        }
        // JSON1 is not available on every supported Android SQLite. The guarded
        // codec contains only canonical hash strings, so this token match is exact.
        if(table(db,"r31_keep_original")) {
            final boolean[] found={false};visitKeepR324(db,value->{if(hash.equals(value)) found[0]=true;});
            if(found[0]) return true;
        }
        return false;
    }
    /** Reject incomplete graph metadata before a collection slice. No partial
     * census, unknown reference or lost child can authorize deletion. */
    static void requireRegisteredR324(SQLiteDatabase db) {
        List<String> queries=referenceQueriesR324(db);
        if(!queries.isEmpty()) try(Cursor c=db.rawQuery("SELECT r.hash FROM ("+String.join(" UNION ALL ",queries)+") r LEFT JOIN r31_gc_artifact a ON a.artifact_hash=r.hash WHERE a.artifact_hash IS NULL OR a.state<>'LIVE' OR a.verified_at<=0 LIMIT 1",null)) {
            if(c.moveToFirst()) throw new IllegalStateException("R324_GC_ROOT_METADATA_INCOMPLETE:"+c.getString(0));
        }
        visitKeepR324(db,hash->{try(Cursor c=db.rawQuery("SELECT 1 FROM r31_gc_artifact WHERE artifact_hash=? AND state='LIVE' AND verified_at>0",new String[]{hash})) {
            if(!c.moveToFirst()) throw new IllegalStateException("R324_GC_KEEP_ROOT_METADATA_INCOMPLETE:"+hash);
        }});
        try(Cursor c=db.rawQuery("SELECT e.to_hash FROM r31_gc_edge e JOIN r31_gc_artifact p ON p.artifact_hash=e.from_hash LEFT JOIN r31_gc_artifact a ON a.artifact_hash=e.to_hash WHERE p.state='LIVE' AND (a.artifact_hash IS NULL OR a.state<>'LIVE' OR a.verified_at<=0) LIMIT 1",null)) {
            if(c.moveToFirst()) throw new IllegalStateException("R324_GC_CHILD_METADATA_INCOMPLETE:"+c.getString(0));
        }
    }
}
