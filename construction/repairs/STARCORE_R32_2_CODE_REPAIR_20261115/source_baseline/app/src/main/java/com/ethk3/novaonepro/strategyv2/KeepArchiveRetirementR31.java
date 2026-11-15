package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.util.*;

/** Retire duplicate SQLite payloads only after the complete original has moved
 * to the verified CAS and every current application keep reader has an adapter.
 * Source copy triggers are deliberately unchanged and remain labelled LEGACY
 * COPY until a separate producer reference protocol is implemented. Removing a
 * SQLite row releases no physical database-file byte; no such claim is made. */
public final class KeepArchiveRetirementR31 {
    private KeepArchiveRetirementR31(){}
    public static final String MODE="CAS_DUPLICATE_RETIREMENT";
    public static final String[] DDL={
        "CREATE TABLE IF NOT EXISTS r31_keep_row_retirement(archive_table TEXT NOT NULL,row_id INTEGER NOT NULL,row_hash TEXT NOT NULL,retired_at INTEGER NOT NULL,authority_hash TEXT NOT NULL,reader_contract_hash TEXT NOT NULL,sqlite_rows_removed INTEGER NOT NULL CHECK(sqlite_rows_removed=1),physical_bytes_released INTEGER NOT NULL CHECK(physical_bytes_released=0),PRIMARY KEY(archive_table,row_id))",
        "CREATE INDEX IF NOT EXISTS idx_r31_keep_retirement_hash ON r31_keep_row_retirement(row_hash)"
    };
    public static boolean ready(SQLiteDatabase db){return ResearchRetentionStoreR30.exists(db,"r31_keep_row_retirement")&&KeepMismatchReferenceR31.ready(db)&&KeepMigrationStoreR31.ready(db);}
    /** Source-specific consumers must additionally prove their actual read-side dependencies before cutover. */
    public static boolean eligible(String source){return ResearchRetentionR30.PROTECTED_TABLES.contains(source)&&!source.equals(KeepMismatchReferenceR31.SOURCE);}
    private static String source(String archive){return KeepMigrationStoreR31.source(archive);}
    private static String q(String name){return ResearchRetentionStoreR30.q(name);}
    private static String lit(String name){return ResearchRetentionStoreR30.lit(name);}
    private static boolean has(SQLiteDatabase db,String object){try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE name=?",new String[]{object})){return c.moveToFirst();}}
    public static String mode(SQLiteDatabase db,String source){if(!KeepMismatchReferenceR31.ready(db))return "LEGACY_COPY";try(Cursor c=db.rawQuery("SELECT mode FROM r31_keep_cutover WHERE source_table=?",new String[]{source})){return c.moveToFirst()?c.getString(0):"LEGACY_COPY";}}
    public static String readerContract(String source){
        if(!ResearchRetentionR30.PROTECTED_TABLES.contains(source))throw new IllegalArgumentException("R31_KEEP_READER_UNREGISTERED_SOURCE");
        String shared="KeepMigrationStoreR31.complete-original-reader+snapshotArtifactPage;ReportAttachmentClosureCP12.full-CAS-attachment;ReportSnapshotR30.retention-archive-index-counts;CheckpointReferencesR26.checkpointArtifactHashes";
        if(source.equals("r26_candidate"))return shared+";WarmPublicationR31.max-original-id-and-complete-new-row-verification";
        if(source.equals("r27_live_frozen"))return shared+";QualificationGapRepairR30R1.CAS-original-predraw-scan-and-restore";
        if(source.equals("r29_model_checkpoint"))return shared+";StartupCheckpointIdentityR31.full-typed-CAS-equality-and-source-generation;ResearchRetentionStoreR30.preserveStartupCheckpoint.prepared-consumer-receipt";
        return shared+";NO_OTHER_KEEP_ROW_READER_IN_EXECUTABLE_SOURCE_INVENTORY_V1";
    }
    public static String guardName(String source){if(!eligible(source))throw new IllegalArgumentException("R31_KEEP_RETIRE_SOURCE_NOT_DECLARED");return "r31_keep_retire_"+source;}
    public static String guardDdl(String source){String archive="r30_keep_"+source,name=guardName(source);
        return "CREATE TRIGGER IF NOT EXISTS "+q(name)+" BEFORE DELETE ON "+q(archive)+" WHEN NOT (EXISTS(SELECT 1 FROM r31_keep_cutover WHERE source_table="+lit(source)+" AND mode IN ('GUARDED_PREPARED','"+MODE+"','REFERENCE_ONLY_GAP')) AND EXISTS(SELECT 1 FROM r31_keep_retire_permit p JOIN r31_keep_original o ON o.archive_table=p.archive_table AND o.row_id=p.row_id AND o.row_hash=p.row_hash WHERE p.archive_table="+lit(archive)+" AND p.row_id=OLD.__r30_id AND o.state='MIGRATED_ORIGINAL_RETAINED')) BEGIN SELECT RAISE(ABORT,'R31_KEEP_ARCHIVE_RETIRE_REQUIRES_VERIFIED_ORIGINAL_PERMIT'); END";
    }
    public static String[] guardDefinitionsR31(SQLiteDatabase db){List<String> definitions=new ArrayList<>();for(String source:ResearchRetentionR30.PROTECTED_TABLES)if(eligible(source)&&ResearchRetentionStoreR30.exists(db,source)&&ResearchRetentionStoreR30.exists(db,"r30_keep_"+source))definitions.add(guardDdl(source));return definitions.toArray(new String[0]);}
    static boolean knownGuard(SQLiteDatabase db,String archive,String type,String name,String sql){
        String source=source(archive);if(!eligible(source)||!"trigger".equals(type)||!guardName(source).equals(name))return false;
        String expected=guardDdl(source),header="CREATE TRIGGER IF NOT EXISTS ";expected="CREATE TRIGGER "+expected.substring(header.length());String actual=sql==null?"":sql.trim();if(actual.startsWith(header))actual="CREATE TRIGGER "+actual.substring(header.length());return expected.equals(actual);
    }
    static LinkedHashMap<String,String> expectedGuards(SQLiteDatabase db,String source,LinkedHashMap<String,String> old){
        if(!eligible(source)||!has(db,guardName(source)))return old;LinkedHashMap<String,String> result=new LinkedHashMap<>(old);result.put(guardName(source),guardDdl(source));
        if((mode(db,source).equals("GUARDED_PREPARED")||(mode(db,source).equals(MODE)||mode(db,source).equals(GapReferenceR31.MODE)))&&!has(db,"r30_keep_"+source+"_immutable_delete"))result.remove("r30_keep_"+source+"_immutable_delete");return result;
    }
    /** One declared, exact old guard delta at a time. This does not change any
     * source copy trigger or certificate/REAL authority. */
    @SuppressWarnings("unchecked") public static boolean advanceWithinMigrationR31(SQLiteDatabase db){
        if(!db.inTransaction())throw new IllegalStateException("R31_KEEP_RETIRE_MIGRATION_WRITER_REQUIRED");if(!ready(db))return false;String authority=KeepMismatchReferenceR31.authorityProof(db);if(authority==null)return false;
        for(String source:ResearchRetentionR30.PROTECTED_TABLES){if(source.equals(StartupCheckpointIdentityR31.SOURCE)&&!StartupCheckpointIdentityR31.ready(db))continue;if(!eligible(source)||!ResearchRetentionStoreR30.exists(db,source))continue;String archive="r30_keep_"+source,name=guardName(source);if(!has(db,archive)||!has(db,name)||(mode(db,source).equals(MODE)||mode(db,source).equals(GapReferenceR31.MODE)))continue;
            ResearchRetentionStoreR30.requireGuardDefinitionsR31(db,new String[][]{{name,guardDdl(source)}},true);ResearchRetentionStoreR30.requireKeepMigrationSourceR31(db,archive);
            long revision;Map<String,Object> state;String raw;
            try(Cursor c=db.rawQuery("SELECT revision,state_json FROM r31_keep_checkpoint WHERE archive_table=?",new String[]{archive})){if(!c.moveToFirst())continue;revision=c.getLong(0);raw=c.getString(1);state=(Map<String,Object>)JsonR27.parse(raw);}
            if(!"SWITCH".equals(state.get("stage"))||((Number)state.get("upperRowId")).longValue()<KeepMigrationStoreR31.maximumOriginalId(db,archive))continue;
            if(KeepMigrationStoreR31.scalar(db,"SELECT count(*) FROM "+q(archive)+" k WHERE NOT EXISTS(SELECT 1 FROM r31_keep_original o WHERE o.archive_table=? AND o.row_id=k.__r30_id AND o.state='MIGRATED_ORIGINAL_RETAINED')",archive)!=0)continue;
            String schema;try(Cursor c=db.rawQuery("SELECT schema_hash FROM r30_retention_archive_index WHERE source_table=?",new String[]{source})){if(!c.moveToFirst())continue;schema=c.getString(0);}
            String old=archive+"_immutable_delete";
            if(has(db,old)){ResearchRetentionStoreR30.requireGuardDefinitionsR31(db,new String[][]{{old,"CREATE TRIGGER IF NOT EXISTS "+q(old)+" BEFORE DELETE ON "+q(archive)+" BEGIN SELECT RAISE(ABORT,'R30_PROTECTED_ARCHIVE_IMMUTABLE'); END"}},true);
                db.execSQL("INSERT OR REPLACE INTO r31_keep_cutover VALUES(?,'GUARDED_PREPARED',?,?,?,?,?,?)",new Object[]{source,schema,revision,authority,StatsR27.sha256(readerContract(source)),StatsR27.sha256("UNCHANGED_LEGACY_COPY;BOUNDED_DUPLICATE_RETIREMENT;NOT_REFERENCE_ONLY"),System.currentTimeMillis()});db.execSQL("DROP TRIGGER "+q(old));return true;
            }
            if(!mode(db,source).equals("GUARDED_PREPARED"))throw new IllegalStateException("R31_KEEP_RETIRE_UNDECLARED_OLD_GUARD_DELTA");
            db.execSQL("UPDATE r31_keep_cutover SET mode=?,checkpoint_revision=?,authority_hash=?,updated_at=? WHERE source_table=? AND mode='GUARDED_PREPARED'",new Object[]{MODE,revision,authority,System.currentTimeMillis(),source});
            state.put("blockedReason",null);state.put("duplicateRetirement",MODE);state.put("newWriteDisposition","UNCHANGED_LEGACY_COPY_NOT_REFERENCE_ONLY");state.put("readerContractHash",StatsR27.sha256(readerContract(source)));
            db.execSQL("UPDATE r31_keep_checkpoint SET revision=revision+1,state_json=?,updated_at=? WHERE archive_table=? AND revision=? AND state_json=?",new Object[]{JsonR27.write(state),System.currentTimeMillis(),archive,revision,raw});if(KeepMigrationStoreR31.scalar(db,"SELECT changes()")!=1)throw new IllegalStateException("R31_KEEP_RETIRE_CHECKPOINT_CAS");return true;
        }return false;
    }
    public static boolean retireOne(SQLiteDatabase read,File root,KeepMigrationStoreR31.Writer writer)throws IOException{
        if(!ready(read))return false;String archive=null,hash=null;long id=0;
        try(Cursor c=read.rawQuery("SELECT o.archive_table,o.row_id,o.row_hash FROM r31_keep_original o JOIN r31_keep_cutover k ON o.archive_table='r30_keep_'||k.source_table WHERE k.mode=? AND o.state='MIGRATED_ORIGINAL_RETAINED' AND NOT EXISTS(SELECT 1 FROM r31_keep_row_retirement r WHERE r.archive_table=o.archive_table AND r.row_id=o.row_id) ORDER BY o.captured_at,o.archive_table,o.row_id LIMIT 64",new String[]{MODE})){while(c.moveToNext()){String candidate=c.getString(0);if(source(candidate).equals(StartupCheckpointIdentityR31.SOURCE)&&!StartupCheckpointIdentityR31.ready(read))continue;if(!eligible(source(candidate)))continue;if(KeepMigrationStoreR31.scalar(read,"SELECT count(*) FROM "+q(candidate)+" WHERE __r30_id=?",Long.toString(c.getLong(1)))==0)continue;archive=candidate;id=c.getLong(1);hash=c.getString(2);break;}}
        if(archive==null)return false;final String target=archive,key=hash,source=source(archive);final long rowId=id;
        KeepMigrationStoreR31.readArtifact(root,target,rowId,key,"__r30_id");String authority=KeepMismatchReferenceR31.authorityProof(read);if(authority==null)return false;
        writer.write("R31_KEEP_RETIRE_DUPLICATE_ORIGINAL",db->{if(!mode(db,source).equals(MODE)||KeepMismatchReferenceR31.authorityProof(db)==null)throw new IllegalStateException("R31_KEEP_RETIRE_AUTHORITY_CHANGED");ResearchRetentionStoreR30.requireKeepMigrationSourceR31(db,target);
            db.execSQL("INSERT INTO r31_keep_retire_permit VALUES(?,?,?)",new Object[]{target,rowId,key});db.execSQL("DELETE FROM "+q(target)+" WHERE __r30_id=?",new Object[]{rowId});if(KeepMigrationStoreR31.scalar(db,"SELECT changes()")!=1)throw new IllegalStateException("R31_KEEP_RETIRE_DUPLICATE_CAS");
            db.execSQL("INSERT INTO r31_keep_row_retirement VALUES(?,?,?,?,?,?,1,0)",new Object[]{target,rowId,key,System.currentTimeMillis(),authority,StatsR27.sha256(readerContract(source))});db.execSQL("DELETE FROM r31_keep_retire_permit WHERE archive_table=? AND row_id=?",new Object[]{target,rowId});
        });return true;
    }
    static long step(DbStore store,RuntimeCoordinatorR26.Claim claim,BudgetTokenSU1 budget)throws IOException{
        SQLiteDatabase read=store.readable();if(!ready(read))return -1;budget.checkpointOrYield();KeepMigrationStoreR31.Writer writer=(operation,work)->store.coordinatorForResearchR952().runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.RETENTION,operation),db->{RuntimeCoordinatorR26.requireFence(db,claim);work.run(db);RuntimeCoordinatorR26.requireFence(db,claim);return 0;});
        if(retireOne(read,new File(store.contextForR26().getFilesDir(),"r31-keep"),writer)){claim.reason("R31_KEEP_SQLITE_DUPLICATE_ROW_RETIRED;ORIGINAL_CAS_RETAINED;PHYSICAL_RELEASE_NOT_CLAIMED");return 1;}return -1;
    }
}
