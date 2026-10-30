package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import com.ethk3.novaonepro.r952.*;
import java.io.File;
import java.lang.reflect.Field;
import java.nio.file.*;
import java.util.*;

/** Original Root P0 triggers and real LongScale numerical commits on isolated synthetic SQLite files.
 * This HOST proof is not an Android physical-device, natural-learning or private-data acceptance. */
public final class RootNotificationFenceR29HostTest {
    static final long EPOCH=1793400000016L;
    static int checks;
    interface Body {void run()throws Exception;}
    static void check(String id,boolean yes){System.out.println("R29_ROOT_NOTIFICATION_FENCE|"+id+"|"+(yes?"OK":"FAILED"));if(!yes)throw new AssertionError(id);checks++;}
    static void tx(SQLiteDatabase db,Body body)throws Exception{db.beginTransactionNonExclusive();try{body.run();db.setTransactionSuccessful();}finally{db.endTransaction();}}
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static String s(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):null;}}
    static byte[] b(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getBlob(0):null;}}
    static String projection(SQLiteDatabase db){return ResearchCutoverR29.researchRoot(db,Long.MAX_VALUE,()->false);}
    static String raw(SQLiteDatabase db){return ResearchCutoverR29.root(db,ResearchCutoverR29.researchTables(db),Long.MAX_VALUE,()->false);}
    static String protocol()throws Exception{Field f=ResearchCutoverR29.class.getDeclaredField("FENCE_PROTOCOL");f.setAccessible(true);return(String)f.get(null);}
    static String[] registryDdl(String field)throws Exception{Field f=ResearchMigrationRegistryR952.class.getDeclaredField(field);f.setAccessible(true);return(String[])f.get(null);}
    static String signature(SQLiteDatabase db,String name){try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE name=?",new String[]{name})){if(!c.moveToFirst())throw new AssertionError("ORIGINAL_DDL_MISSING:"+name);return c.getString(0);}}
    static boolean rejected(SQLiteDatabase db){try{ResearchCutoverR29.verifyFence(db,EPOCH,Long.MAX_VALUE,()->false);return false;}catch(IllegalStateException expected){if(!String.valueOf(expected.getMessage()).contains("RECOVERY_REQUIRED"))throw expected;return true;}}
    static final class Fixture implements AutoCloseable {
        final Path dir=Files.createTempDirectory("r29-root-notification-");
        final SQLiteDatabase root=SQLiteDatabase.openOrCreateDatabase(dir.resolve("root.db").toFile(),null);
        Fixture()throws Exception{
            root.execSQL("PRAGMA journal_mode=WAL");CriticalInputProductionTriggerFixtureR29.installRoot(root);
            tx(root,()->{
                root.execSQL("CREATE TABLE IF NOT EXISTS "+DbHelper.TABLE_OOS_ACTION_LOG+"(_id INTEGER PRIMARY KEY AUTOINCREMENT,contract_decision_id TEXT NOT NULL UNIQUE)");DbHelper.ensureR811CriticalSchema(root);
                // Execute exact original production DDL, never a handwritten simplified revision trigger.
                for(String ddl:registryDdl("CP4_DDL"))if(ddl.contains("CREATE TABLE IF NOT EXISTS research_action_v2("))root.execSQL(ddl);
                for(String ddl:registryDdl("CP8_DDL"))if(ddl.contains("CREATE TABLE IF NOT EXISTS research_row_revision_v2("))root.execSQL(ddl);
                for(String ddl:registryDdl("CP8_DDL"))if(ddl.contains("CREATE TRIGGER IF NOT EXISTS cp8_revision_draws_")||ddl.contains("CREATE TRIGGER IF NOT EXISTS cp8_revision_v57_draw_canonical_r811_")||ddl.contains("CREATE TRIGGER IF NOT EXISTS cp8_revision_v57_draw_provenance_r811_")||ddl.contains("CREATE TRIGGER IF NOT EXISTS cp8_revision_research_action_v2_"))root.execSQL(ddl);
                root.execSQL("INSERT OR REPLACE INTO app_state VALUES('cp12_training_history_generation','1',1)");
                LongScaleSummaryR80.schema(root);
                for(int i=1;i<=10;i++){CanonicalInputProductionFixtureR29.insertDraw(root,i);CanonicalInputProductionFixtureR29.canonical(root,i);}
                root.execSQL("INSERT INTO research_action_v2(action_id,finding_id,action_type,operator,proposal_json,evidence_hash,seed,parent_hashes_json,protocol_hash,planned_endpoint,alpha_identity,state) VALUES('SYNTHETIC_ACTION','SYNTHETIC_FINDING','HOLD','SYNTHETIC','{}','SYNTHETIC',1,'[]','SYNTHETIC_PROTOCOL',0,'SYNTHETIC_ALPHA','PROPOSED')");
            });
            // The source checkpoint is produced by the real numerical preparation and atomic commit.
            LongScaleSummaryR80.Prepared p=LongScaleSummaryR80.prepare(root,System.nanoTime()+10_000_000_000L);tx(root,()->LongScaleSummaryR80.commit(root,p));
            root.execSQL(ResearchCutoverR29.FENCE);
        }
        void bind()throws Exception{root.execSQL("DELETE FROM r29_migration_owner_fence");root.execSQL("INSERT INTO r29_migration_owner_fence VALUES(1,?,?,?,1)",new Object[]{EPOCH,protocol(),projection(root)});}
        void future()throws Exception{tx(root,()->{CanonicalInputProductionFixtureR29.insertDraw(root,11);CanonicalInputProductionFixtureR29.canonical(root,11);});}
        void late()throws Exception{tx(root,()->root.execSQL("DELETE FROM "+DbHelper.TABLE_R811_DRAW_CANONICAL+" WHERE target_issue='202610300005'"));bind();tx(root,()->CanonicalInputProductionFixtureR29.canonical(root,5));}
        public void close()throws Exception{root.close();try(java.util.stream.Stream<Path> paths=Files.walk(dir)){paths.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(Exception e){throw new IllegalStateException(e);}});}}
    }
    static void notifications(boolean oldNegative)throws Exception{
        try(Fixture f=new Fixture()){
            check("N1_ORIGINAL_P0_TRIGGERS_AND_REAL_CHECKPOINT_PRESENT",n(f.root,"SELECT count(*) FROM sqlite_master WHERE type='trigger' AND name IN('long_source_token_insert_r80','long_canonical_insert_r81','cp8_revision_draws_update','cp8_revision_v57_draw_canonical_r811_insert','cp8_revision_research_action_v2_update')")==5&&n(f.root,"SELECT length(state_blob) FROM "+LongScaleSummaryR80.HEAD)==LongScaleCountsR80.CHECKPOINT_BYTES&&LongScaleCountsR80.restore(b(f.root,"SELECT state_blob FROM "+LongScaleSummaryR80.HEAD),s(f.root,"SELECT state_hash FROM "+LongScaleSummaryR80.HEAD)).support(1440)==10);
            f.bind();String beforeRaw=raw(f.root),beforeProjected=projection(f.root);long token=n(f.root,"SELECT generation FROM "+LongScaleSummaryR80.TOKEN);
            f.future();boolean allowed=!rejected(f.root);
            check("N2_REAL_P0_FUTURE_INPUT_ADVANCES_TOKEN_AND_AUTHORITY_REVISIONS",n(f.root,"SELECT generation FROM "+LongScaleSummaryR80.TOKEN)==token+1&&n(f.root,"SELECT count(*) FROM research_row_revision_v2 WHERE source_table='draws'")==11&&n(f.root,"SELECT count(*) FROM research_row_revision_v2 WHERE source_table='v57_draw_canonical_r811'")==11&&!beforeRaw.equals(raw(f.root)));
            if(oldNegative)System.out.println("R29_ROOT_NOTIFICATION_FENCE|OLD51_P0_ONLY_RESULT|"+(allowed?"ALLOWED":"ROOT_RESEARCH_ADVANCED_AFTER_SEAL"));
            check("N3_P0_ONLY_NOTIFICATION_MUST_NOT_BLOCK_SEALED_RESEARCH_OWNER",allowed&&beforeProjected.equals(projection(f.root)));
            if(oldNegative)return;
            tx(f.root,()->f.root.execSQL("UPDATE draws SET issue=issue WHERE issue='202610300001'"));check("N4_IDENTICAL_DRAW_UPDATE_REVISION_CHURN_IS_ALLOWED",!rejected(f.root)&&beforeProjected.equals(projection(f.root)));
            LongScaleSummaryR80.Prepared next=LongScaleSummaryR80.prepare(f.root,System.nanoTime()+10_000_000_000L);tx(f.root,()->LongScaleSummaryR80.commit(f.root,next));
            check("N5_REAL_LONGSCALE_NUMERICAL_COMMIT_REMAINS_FAIL_CLOSED",rejected(f.root)&&n(f.root,"SELECT count(*) FROM "+LongScaleSummaryR80.SNAPSHOT)==2&&s(f.root,"SELECT through_issue FROM "+LongScaleSummaryR80.HEAD).equals("202610300011"));
        }
        try(Fixture f=new Fixture()){
            byte[] state=b(f.root,"SELECT state_blob FROM "+LongScaleSummaryR80.HEAD);String stateHash=s(f.root,"SELECT state_hash FROM "+LongScaleSummaryR80.HEAD),chain=s(f.root,"SELECT source_chain FROM "+LongScaleSummaryR80.HEAD),through=s(f.root,"SELECT through_issue FROM "+LongScaleSummaryR80.HEAD);long generation=n(f.root,"SELECT generation FROM "+LongScaleSummaryR80.HEAD);
            Map<String,String> original=new LinkedHashMap<>();for(String name:new String[]{"long_canonical_insert_r81","long_source_token_insert_r80","cp8_revision_draws_update"})original.put(name,signature(f.root,name));
            f.late();check("N6_ORIGINAL_LATE_SOURCE_TRIGGER_CHANGES_ONLY_INVALIDATION_METADATA",n(f.root,"SELECT generation FROM "+LongScaleSummaryR80.HEAD)==generation+1&&"INVALID_SOURCE".equals(s(f.root,"SELECT status FROM "+LongScaleSummaryR80.HEAD))&&s(f.root,"SELECT last_error FROM "+LongScaleSummaryR80.HEAD).startsWith("CANONICAL_INSERT:")&&Arrays.equals(state,b(f.root,"SELECT state_blob FROM "+LongScaleSummaryR80.HEAD))&&stateHash.equals(s(f.root,"SELECT state_hash FROM "+LongScaleSummaryR80.HEAD))&&chain.equals(s(f.root,"SELECT source_chain FROM "+LongScaleSummaryR80.HEAD))&&through.equals(s(f.root,"SELECT through_issue FROM "+LongScaleSummaryR80.HEAD)));
            check("N7_LATE_SOURCE_NOTIFICATION_PERMITS_OWNER_WITH_CHECKPOINT_UNCHANGED",!rejected(f.root));
            String rawBefore=raw(f.root),fullBefore=ResearchCutoverR29.root(f.root,ResearchCutoverR29.tables(f.root),Long.MAX_VALUE,()->false);File sealed=f.dir.resolve("original-sealed-copy.db").toFile();
            try(SQLiteDatabase copied=SQLiteDatabase.openOrCreateDatabase(sealed,null)){
                tx(copied,()->{for(MigrationCopyR29.Table t:ResearchCutoverR29.tables(f.root)){copied.execSQL(t.createSql);Long after=null;for(;;){List<MigrationCopyR29.Row> page=ResearchCutoverR29.page(f.root,t,after);if(page.isEmpty())break;ResearchCutoverR29.restorePage(copied,t,page);after=page.get(page.size()-1).rowid;}}});
                check("N8_FULL_TYPED_COPY_PRESERVES_RAW_NOTIFICATION_REVISIONS_AND_CHECKPOINT",rawBefore.equals(raw(copied))&&fullBefore.equals(ResearchCutoverR29.root(copied,ResearchCutoverR29.tables(copied),Long.MAX_VALUE,()->false))&&n(copied,"SELECT generation FROM "+LongScaleSummaryR80.TOKEN)==n(f.root,"SELECT generation FROM "+LongScaleSummaryR80.TOKEN)&&Arrays.equals(state,b(copied,"SELECT state_blob FROM "+LongScaleSummaryR80.HEAD))&&"INVALID_SOURCE".equals(s(copied,"SELECT status FROM "+LongScaleSummaryR80.HEAD)));
            }
            String sealedSha=ResearchCutoverR29.fileHash(sealed,Long.MAX_VALUE,()->false);tx(f.root,()->f.root.execSQL("UPDATE "+LongScaleSummaryR80.HEAD+" SET source_chain=?",new Object[]{StatsR27.sha256("SYNTHETIC_TRUE_CHAIN_CHANGE")}));
            check("N9_TRUE_SOURCE_CHAIN_CHANGE_STILL_REQUIRES_RECOVERY",rejected(f.root)&&sealedSha.equals(ResearchCutoverR29.fileHash(sealed,Long.MAX_VALUE,()->false)));
            boolean unchanged=true;for(Map.Entry<String,String> e:original.entrySet())unchanged&=e.getValue().equals(signature(f.root,e.getKey()));check("N10_ALL_ORIGINAL_ROOT_TRIGGER_SQL_RETAINED",unchanged);
        }
    }
    static void strictGuards()throws Exception{
        try(Fixture f=new Fixture()){f.bind();tx(f.root,()->f.root.execSQL("UPDATE research_action_v2 SET state='CREATED' WHERE action_id='SYNTHETIC_ACTION'"));check("N11_REAL_RESEARCH_ACTION_AND_REVISION_PROGRESS_STILL_FAILS_CLOSED",rejected(f.root));}
        try(Fixture f=new Fixture()){f.bind();tx(f.root,()->f.root.execSQL("UPDATE research_row_revision_v2 SET generation=generation+10000 WHERE source_table='research_action_v2'"));check("N12_MIXED_JOURNAL_RESEARCH_ROWS_ARE_STILL_PROTECTED",rejected(f.root));}
        try(Fixture f=new Fixture()){f.bind();tx(f.root,()->f.root.execSQL("INSERT INTO research_row_revision_v2(source_table,source_rowid) VALUES('SYNTHETIC_UNKNOWN_SOURCE',1)"));check("N13_UNKNOWN_REVISION_SOURCE_IS_NOT_BLANKET_AUTHORITY_EXEMPT",rejected(f.root));}
        try(Fixture f=new Fixture()){f.bind();tx(f.root,()->f.root.execSQL("UPDATE "+LongScaleSummaryR80.HEAD+" SET through_issue='202610300009'"));check("N14_TRUE_MODEL_CURSOR_CHANGE_STILL_REQUIRES_RECOVERY",rejected(f.root));}
        try(Fixture f=new Fixture()){f.bind();tx(f.root,()->f.root.execSQL("UPDATE "+LongScaleSummaryR80.HEAD+" SET availability_max=availability_max+1"));check("N15_TRUE_MODEL_AVAILABILITY_CHANGE_STILL_REQUIRES_RECOVERY",rejected(f.root));}
        try(Fixture f=new Fixture()){f.bind();tx(f.root,()->f.root.execSQL("ALTER TABLE "+LongScaleSummaryR80.TOKEN+" ADD COLUMN synthetic_future_model TEXT"));check("N16_SCHEMA_IS_NEVER_IGNORED_BY_NOTIFICATION_PROJECTION",rejected(f.root));}
        try(Fixture f=new Fixture()){f.bind();tx(f.root,()->f.root.execSQL("UPDATE r29_migration_owner_fence SET protocol='R29_ROOT_RESEARCH_FROZEN_V1'"));check("N17_OLD_V1_PENDING_FENCE_IS_NOT_SILENTLY_REBOUND_AS_V2",rejected(f.root));}
        try(Fixture f=new Fixture()){f.bind();byte[] checkpoint=b(f.root,"SELECT state_blob FROM "+LongScaleSummaryR80.HEAD);checkpoint[40]^=1;tx(f.root,()->f.root.execSQL("UPDATE "+LongScaleSummaryR80.HEAD+" SET state_blob=?",new Object[]{checkpoint}));check("N18_CHECKPOINT_PAYLOAD_ONLY_CHANGE_STILL_REQUIRES_RECOVERY",rejected(f.root));}
        try(Fixture f=new Fixture()){f.bind();tx(f.root,()->f.root.execSQL("UPDATE "+LongScaleSummaryR80.HEAD+" SET state_hash=?",new Object[]{StatsR27.sha256("SYNTHETIC_TRUE_STATE_HASH_CHANGE")}));check("N19_CHECKPOINT_HASH_ONLY_CHANGE_STILL_REQUIRES_RECOVERY",rejected(f.root));}
        try(Fixture f=new Fixture()){f.bind();tx(f.root,()->f.root.execSQL("UPDATE "+LongScaleSummaryR80.HEAD+" SET status='INVALID_SOURCE',last_error='SYNTHETIC_UNKNOWN_NOTIFICATION'"));check("N20_UNKNOWN_NOTIFICATION_REASON_IS_NOT_NORMALIZED",rejected(f.root));}
        try(Fixture f=new Fixture()){f.bind();tx(f.root,()->f.root.execSQL("UPDATE "+LongScaleSummaryR80.HEAD+" SET status='INVALID_SOURCE',last_error='CANONICAL_INSERT:NOT_AN_ISSUE'"));check("N21_MALFORMED_SOURCE_NOTIFICATION_IS_NOT_NORMALIZED",rejected(f.root));}
    }
    public static void main(String[] args)throws Exception{
        boolean oldNegative=args.length>0&&args[0].equals("--old-notification");notifications(oldNegative);if(!oldNegative)strictGuards();System.out.println("R29_ROOT_NOTIFICATION_FENCE|ALL_PASS|checks="+checks+"|ORIGINAL_PRODUCTION_P0_DDL_AND_REAL_LONGSCALE_COMMIT_HOST_ONLY");
    }
}
