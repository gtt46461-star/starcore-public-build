package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.nio.file.*;
import java.sql.*;
import java.util.*;

/** Actual production keep adapter, native SQLite and actual child-process kill
 * points. Synthetic fixtures only; no Android/service/device claim. */
public final class KeepStoreR31Host {
    static void require(boolean ok,String code){if(!ok)throw new AssertionError(code);}
    static long n(SQLiteDatabase db,String sql,String...args){return KeepMigrationStoreR31.scalar(db,sql,args);}
    static KeepMigrationStoreR31.Writer writer(SQLiteDatabase db,String pause){
        return (operation,work)->{
            try{db.jdbc.setAutoCommit(false);work.run(db);
                if(pause.equals("before")){System.out.println("KEEP_NATIVE_BEFORE_COMMIT");System.out.flush();Thread.sleep(60000);}
                db.jdbc.commit();db.jdbc.setAutoCommit(true);
                if(pause.equals("after")){System.out.println("KEEP_NATIVE_AFTER_COMMIT");System.out.flush();Thread.sleep(60000);}
            }catch(Exception e){try{db.jdbc.rollback();db.jdbc.setAutoCommit(true);}catch(Exception ignored){}throw new IllegalStateException(e);}
        };
    }
    static KeepMigrationStoreR31 adapter(SQLiteDatabase db,File directory,String pause)throws Exception{return new KeepMigrationStoreR31(db,directory,writer(db,pause));}
    public static void main(String[] args)throws Exception {
        File data=new File(args[0]);File databases=new File(data,"databases"),files=new File(data,"files/r31-keep");databases.mkdirs();files.mkdirs();
        try(SQLiteDatabase db=SQLiteDatabase.openDatabase(new File(databases,"keep.db").getPath(),null,0)){
            String mode=args[1];String archive="r30_keep_r27_policy";
            if(mode.equals("generic-init")){
                db.execSQL("CREATE TABLE r27_chart_cache(panel_id TEXT,key TEXT,payload_json TEXT,updated_at INTEGER,PRIMARY KEY(panel_id,key))");db.execSQL("CREATE TABLE critical_probe(id INTEGER PRIMARY KEY AUTOINCREMENT,value TEXT NOT NULL)");
                db.jdbc.setAutoCommit(false);
                for(String table:new String[]{"r26_candidate","r26_gap_receipt"})for(String sql:RuntimeSchemaR26.DDL)if(sql.startsWith("CREATE TABLE IF NOT EXISTS "+table+"("))db.execSQL(sql);
                for(String sql:SchemaR27.DDL)if(sql.startsWith("CREATE TABLE IF NOT EXISTS r27_live_frozen("))db.execSQL(sql);
                java.lang.reflect.Method addColumn=RuntimeSchemaR26.class.getDeclaredMethod("column",SQLiteDatabase.class,String.class,String.class,String.class);addColumn.setAccessible(true);addColumn.invoke(null,db,"r26_candidate","service_cpu_ms","INTEGER NOT NULL DEFAULT 0");addColumn.invoke(null,db,"r26_candidate","last_served","INTEGER NOT NULL DEFAULT 0");
                java.lang.reflect.Method install=ResearchRetentionStoreR30.class.getDeclaredMethod("ensureProtectedObjectsR31",SQLiteDatabase.class);install.setAccessible(true);install.invoke(null,db);
                for(String sql:KeepMigrationStoreR31.DDL)db.execSQL(sql);for(String sql:KeepMismatchReferenceR31.DDL)db.execSQL(sql);for(String sql:KeepArchiveRetirementR31.DDL)db.execSQL(sql);for(String sql:KeepArchiveRetirementR31.guardDefinitionsR31(db))db.execSQL(sql);
                byte[] blob=new byte[160001];for(int i=0;i<blob.length;i++)blob[i]=(byte)i;db.execSQL("INSERT INTO r26_candidate(campaign_id,policy_hash,canonical_text,state,operation) VALUES('campaign','policy','original program',?,'original operation')",new Object[]{blob});db.execSQL("DELETE FROM r26_candidate");
                db.execSQL("INSERT INTO r26_gap_receipt VALUES('policy',1,3,1,'original gap receipt',1)");db.execSQL("DELETE FROM r26_gap_receipt");
                byte[] probabilities=StoreR27.pack(new double[]{.1,.1,.1,.1,.1,.1,.1,.1,.1,.1});long ordinal=com.ethk3.novaonepro.TargetIssueResolver.sequenceOrdinalSU1("202001010001");
                db.execSQL("INSERT INTO r27_live_frozen VALUES('policy',?,'202001010001',1,?,1,2,3,1,'FROZEN',1000,200000,-1,0)",new Object[]{ordinal,probabilities});db.execSQL("DELETE FROM r27_live_frozen");
                com.ethk3.novaonepro.SchemaDomainCoordinatorR926.migrateR31MainlineChoice(db);MainlineChoiceStoreR31.appendChoice(db,"","",CertificateGateR27.GATE_VERSION,"","",1,1,1,1);db.execSQL("INSERT INTO r27_chart_cache VALUES('mainline','choice',?,1)",new Object[]{JsonR27.write(Collections.singletonMap("choice",MainlineChoiceStoreR31.FALLBACK))});MainlineChoiceStoreR31.switchToTypedRead(db,1);
                db.jdbc.commit();db.jdbc.setAutoCommit(true);System.out.println("KEEP_NATIVE_GENERIC_ORIGINAL_SOURCE_DDL_INITIALIZED");return;
            }
            if(mode.equals("generic-one")){String target=args[2];KeepMigrationR31.step(adapter(db,files,""),"r30_keep_"+target);try(Connection critical=DriverManager.getConnection("jdbc:sqlite:"+new File(databases,"keep.db").getPath())){critical.createStatement().execute("INSERT INTO critical_probe(value) VALUES('actual generic between-unit commit')");}return;}
            if(mode.equals("generic-cutover")){writer(db,args.length>2?args[2]:"").write("NATIVE_DECLARED_GENERIC_GUARD_DELTA",connection->{require(KeepArchiveRetirementR31.advanceWithinMigrationR31(connection),"ACTUAL_GENERIC_CUTOVER_UNIT");});return;}
            if(mode.equals("generic-retire")){require(KeepArchiveRetirementR31.retireOne(db,files,writer(db,args.length>2?args[2]:"")),"ACTUAL_GENERIC_RETIRE_UNIT");return;}
            if(mode.equals("generic-append")){db.execSQL("INSERT INTO r26_gap_receipt VALUES('new-policy',4,6,1,'new actual legacy copy is not called reference-only',2)");db.execSQL("DELETE FROM r26_gap_receipt");require(n(db,"SELECT count(*) FROM r30_keep_r26_gap_receipt")==1,"SOURCE_COPY_PROTOCOL_ACCURATELY_RETAINED");return;}
            if(mode.equals("generic-verify")){
                adapter(db,files,"");for(String target:new String[]{"r26_candidate","r26_gap_receipt","r27_live_frozen"}){require(KeepArchiveRetirementR31.MODE.equals(KeepArchiveRetirementR31.mode(db,target)),"DECLARED_DUPLICATE_RETIREMENT_MODE");require(n(db,"SELECT count(*) FROM r30_keep_"+target)==0,"ACTUAL_GENERIC_SQLITE_DUPLICATE_SHRINK");ResearchRetentionStoreR30.requireKeepMigrationSourceR31(db,"r30_keep_"+target);}
                require(n(db,"SELECT count(*) FROM r31_keep_row_retirement")==4,"ALL_OLD_AND_NEW_APPENDED_ROWS_RETIRED_EXACTLY_ONCE");require(n(db,"SELECT sum(physical_bytes_released) FROM r31_keep_row_retirement")==0,"NO_PHYSICAL_FILE_RELEASE_FABRICATED");require(n(db,"SELECT count(*) FROM critical_probe")>12,"CRITICAL_SQLITE_COMMITS_BETWEEN_GENERIC_UNITS");
                long before=KeepMigrationStoreR31.maximumOriginalId(db,"r30_keep_r26_candidate");require(before==1,"WARM_AFTER_CAS_RETIREMENT_HIGH_WATER");
                WarmPublicationR31.requireCandidateArchiveR31(db);
                byte[] bytes=(byte[])KeepMigrationStoreR31.readOriginal(db,"r30_keep_r26_candidate",1,"state")[0];require(bytes.length==160001,"COMPLETE_WARM_PARENT_ORIGINAL_RETAINED");for(int i=0;i<bytes.length;i++)require(bytes[i]==(byte)i,"COMPLETE_WARM_PARENT_BYTES");
                String[] columns=("__r30_id,"+QualificationGapRepairR30R1.COLUMNS+",__r30_reason").split(",");Object[] predraw=KeepMigrationStoreR31.readOriginal(db,"r30_keep_r27_live_frozen",1,columns);require(QualificationGapRepairR30R1.legalOriginal(predraw),"ACTUAL_QUALIFICATION_ORIGINAL_CONSUMER_ACCEPTS_CAS");require(QualificationGapRepairR30R1.predrawHash(predraw).matches("[0-9a-f]{64}"),"ORIGINAL_PREDRAW_HASH_PRESERVED");
                for(String sql:SchemaR27.DDL)if(sql.startsWith("CREATE TABLE IF NOT EXISTS r27_live_shadow(")||sql.startsWith("CREATE TABLE IF NOT EXISTS r27_eprocess("))db.execSQL(sql);
                java.lang.reflect.Field qualificationDdl=QualificationInboxR29.class.getDeclaredField("DDL");qualificationDdl.setAccessible(true);for(String sql:(String[])qualificationDdl.get(null))if(sql.startsWith("CREATE TABLE IF NOT EXISTS r29_qual_cursor("))db.execSQL(sql);
                long originalOrdinal=((Number)predraw[2]).longValue();db.execSQL("INSERT INTO r27_live_shadow(policy_hash,spec_json,temperature,bias,global_index,status,reason,promoted_at,updated_at) VALUES('policy','{}',1,X'',1,'ACTIVE','native original gap',1,1)");
                db.execSQL("INSERT INTO r27_eprocess(policy_hash,global_index,alpha,log_e_up,log_e_down,n,sum_u,sum_u2,last_ordinal,state,updated_at) VALUES('policy',1,0.01,0,0,0,0,0,?,'ACCUMULATING',1)",new Object[]{originalOrdinal-1});
                db.execSQL("INSERT INTO r29_qual_cursor(experiment_id,blocked_ordinal,blocked_reason,updated_at) VALUES('policy',?,'MISSING_OPPORTUNITY',1)",new Object[]{originalOrdinal});
                long repaired=QualificationGapRepairR30R1.step(db,(operation,batch)->{writer(db,"").write(operation,connection->ResearchWriteQueueR27.exec(connection,batch));return true;});require(repaired==1,"ACTUAL_QUALIFICATION_REPAIR_PRODUCTION_ENTRY_RAN");require(n(db,"SELECT count(*) FROM r27_live_frozen WHERE policy_hash='policy' AND target_ordinal=?",Long.toString(originalOrdinal))==1,"ORIGINAL_CAS_PREDRAW_RESTORED_THROUGH_ACTUAL_CONSUMER");require(n(db,"SELECT n FROM r27_eprocess WHERE policy_hash='policy'")==0,"FORMAL_ALPHA_OR_EVIDENCE_NOT_RESET_OR_FABRICATED");

                byte[] next=new byte[]{1,2,3};db.execSQL("INSERT INTO r26_candidate(campaign_id,policy_hash,canonical_text,state,operation) VALUES('next campaign','next policy','next program',?,'new operation')",new Object[]{next});db.execSQL("DELETE FROM r26_candidate");List<KeepMigrationStoreR31.OriginalRow> appended=KeepMigrationStoreR31.originalRows(db,"r30_keep_r26_candidate",before,KeepMigrationStoreR31.maximumOriginalId(db,"r30_keep_r26_candidate"),2,WarmPublicationR31.COLUMNS.split(","));require(appended.size()==1,"ACTUAL_WARM_NEW_PARENT_VERIFICATION_EXCLUDES_OLDER_CAS_ROWS");try{KeepMigrationStoreR31.readOriginal(db,"r30_keep_r26_candidate",appended.get(0).id,"not_a_column");throw new AssertionError("SQLITE_DQS_FAKE_COLUMN_ACCEPTED");}catch(IllegalArgumentException expected){require(expected.getMessage().contains("UNKNOWN_PROJECTION_COLUMN"),"SQLITE_DQS_UNKNOWN_PROJECTION_BLOCKED");}
                db.jdbc.setAutoCommit(false);try{java.lang.reflect.Method install=ResearchRetentionStoreR30.class.getDeclaredMethod("ensureProtectedObjectsR31",SQLiteDatabase.class);install.setAccessible(true);install.invoke(null,db);db.jdbc.commit();}finally{db.jdbc.setAutoCommit(true);}require(n(db,"SELECT count(*) FROM sqlite_master WHERE name='r30_keep_r26_candidate_immutable_delete'")==0,"OLD_OWNER_DOES_NOT_RECREATE_RETIRED_GENERIC_GUARD");
                require(!StartupCheckpointIdentityR31.ready(db)&&!KeepArchiveRetirementR31.MODE.equals(KeepArchiveRetirementR31.mode(db,"r29_model_checkpoint")),"STARTUP_READER_DEPENDENCY_REQUIRED_BEFORE_R29_RETIREMENT");
                System.out.println("KEEP_NATIVE_GENERIC_ACTUAL_CAS_READERS_DUPLICATE_RETIREMENT_PASS_NO_REFERENCE_ONLY_CLAIM");return;
            }
            if(mode.equals("gap-init")){
                db.jdbc.setAutoCommit(false);for(String sql:GapReferenceR31.DDL)db.execSQL(sql);for(String sql:GapReferenceR31.guardDefinitionsR31(db))db.execSQL(sql);require(GapReferenceR31.advanceCutoverWithinMigrationR31(db),"ACTUAL_GAP_REFERENCE_PROTOCOL_SWITCH");
                for(int i=0;i<2304;i++)db.execSQL("INSERT INTO r26_gap_receipt VALUES(?,?,?,1,'UNKNOWN_LABELS; PARAMETERS_RETAINED; TEMPORAL_CONTEXT_REBUILT',?)",new Object[]{"gap-policy-"+i,i,i+2,i});
                Map<String,Object> checkpoint=new LinkedHashMap<>();checkpoint.put("version",RetentionR26.PHASE_VERSION);checkpoint.put("phase",20);checkpoint.put("cycleSequence",50000);checkpoint.put("lastCompletedSequence",49999);checkpoint.put("workCursor",0);db.execSQL("INSERT INTO r27_chart_cache VALUES(?,?,?,?)",new Object[]{RetentionR26.PHASE_PANEL,RetentionR26.PHASE_KEY,JsonR27.write(checkpoint),1});db.jdbc.commit();db.jdbc.setAutoCommit(true);
                require(n(db,"SELECT count(*) FROM r30_keep_r26_gap_receipt")==0,"NO_NEW_GAP_COPY_AT_REFERENCE_SWITCH");System.out.println("KEEP_NATIVE_GAP_REFERENCE_REAL_SOURCE_TAIL_AND_BATCH_INITIALIZED");return;
            }
            if(mode.equals("gap-step")){
                RetentionR26.Checkpoint checkpoint=RetentionR26.checkpoint(db);long work=GapReferenceR31.advancePrepared(db,files,writer(db,args.length>2?args[2]:""),()->{},ignored->{},checkpoint,50000);
                try(Connection critical=DriverManager.getConnection("jdbc:sqlite:"+new File(databases,"keep.db").getPath())){critical.createStatement().execute("INSERT INTO critical_probe(value) VALUES('actual critical commit between gap producer chunks')");}
                System.out.println("KEEP_NATIVE_GAP_REFERENCE_COMMITTED_WORK="+work);return;
            }
            if(mode.equals("gap-verify")){
                adapter(db,files,"");require(RetentionR26.checkpoint(db).phase==21,"SOURCE_EFFECT_AND_ORIGINAL_RETENTION_PHASE_ADVANCE_COMMIT_TOGETHER");require(n(db,"SELECT count(*) FROM r26_gap_receipt")==2048,"ORIGINAL_2048_RETENTION_THRESHOLD_UNCHANGED");require(n(db,"SELECT min(recorded_at) FROM r26_gap_receipt")==256,"EXACT_ORIGINAL_256_SELECTED_OLD_ROWS_REMOVED");require(n(db,"SELECT count(*) FROM r31_gap_reference_original")==256,"ALL_ORIGINAL_GAP_ROWS_REFERENCE_ONLY");require(n(db,"SELECT count(*) FROM r30_keep_r26_gap_receipt")==0,"REFERENCE_PRODUCER_DID_NOT_INSERT_OLD_KEEP");require(n(db,"SELECT count(*) FROM r31_gap_reference_permit")==0,"ALL_SOURCE_PERMITS_RESOLVED");require(n(db,"SELECT count(*) FROM r31_gap_reference_item WHERE state='APPLIED' AND row_json=''")==256,"DURABLE_BATCH_COPIES_RETIRED_AFTER_REFERENCE_COMMIT");
                try(android.database.Cursor c=db.rawQuery("SELECT archive_id,policy_hash,after_ordinal,before_ordinal FROM r31_gap_reference_original ORDER BY after_ordinal",null)){int seen=0;while(c.moveToNext()){Object[] row=KeepMigrationStoreR31.readOriginal(db,GapReferenceR31.ARCHIVE,c.getLong(0),GapReferenceR31.COLUMNS.split(","));require(c.getString(1).equals(row[0])&&c.getLong(2)==((Number)row[1]).longValue()&&c.getLong(3)==((Number)row[2]).longValue(),"EVERY_GAP_CAS_ORIGINAL_COMPLETE_AND_LINKED");seen++;}require(seen==256,"EVERY_SOURCE_ROW_READ_BACK");}
                try{db.execSQL("DELETE FROM r26_gap_receipt");throw new AssertionError("UNPREPARED_GAP_DELETE");}catch(android.database.sqlite.SQLiteException expected){require(expected.getMessage().contains("PERMIT_REQUIRED"),"SOURCE_DELETE_CANNOT_BYPASS_PREPARATION");}
                db.jdbc.setAutoCommit(false);try{RetentionR26.pruneStep(db,50000,18);throw new AssertionError("LEGACY_WRITER_STARTED_FILE_WORK_OR_UNPREPARED_DELETE");}catch(com.ethk3.novaonepro.DatabaseCoordinator.SchemaPendingR31 expected){require(expected.getMessage().contains("OUTSIDE_WRITER"),"NO_WRITER_SIDE_CAS_PREPARATION");}finally{db.jdbc.rollback();db.jdbc.setAutoCommit(true);}
                System.out.println("KEEP_NATIVE_GAP_REFERENCE_2048_TAIL_256_BATCH_CHUNKS_ORIGINALS_AND_SOURCE_PERMIT_PASS");return;
            }
            if(mode.equals("mismatch-init")){
                db.execSQL("CREATE TABLE r27_chart_cache(panel_id TEXT,key TEXT,payload_json TEXT,updated_at INTEGER,PRIMARY KEY(panel_id,key))");
                db.execSQL("CREATE TABLE critical_probe(id INTEGER PRIMARY KEY AUTOINCREMENT,value TEXT NOT NULL)");
                db.jdbc.setAutoCommit(false);for(String sql:DecisionExplanationStoreR30.DDL)db.execSQL(sql);
                java.lang.reflect.Method install=ResearchRetentionStoreR30.class.getDeclaredMethod("ensureProtectedObjectsR31",SQLiteDatabase.class);install.setAccessible(true);install.invoke(null,db);
                for(String sql:KeepMigrationStoreR31.DDL)db.execSQL(sql);for(String sql:KeepMismatchReferenceR31.DDL)db.execSQL(sql);for(String sql:KeepMismatchReferenceR31.cutoverDefinitionsR31(db))db.execSQL(sql);
                db.execSQL("INSERT INTO r30_decision_mismatch VALUES('202001010001','original-old',1,0,'ORIGINAL_MORE_PERMISSIVE','original reason',NULL,1)");db.execSQL("DELETE FROM r30_decision_mismatch");
                db.execSQL("INSERT INTO r30_decision_mismatch VALUES('202001010002','new-producer',0,1,'ORIGINAL_STRICTER','new reason',NULL,2)");
                com.ethk3.novaonepro.SchemaDomainCoordinatorR926.migrateR31MainlineChoice(db);
                MainlineChoiceStoreR31.appendChoice(db,"","",CertificateGateR27.GATE_VERSION,"","",1,1,1,1);
                db.execSQL("INSERT INTO r27_chart_cache VALUES('mainline','choice',?,1)",new Object[]{JsonR27.write(Collections.singletonMap("choice",MainlineChoiceStoreR31.FALLBACK))});MainlineChoiceStoreR31.switchToTypedRead(db,1);
                db.jdbc.commit();db.jdbc.setAutoCommit(true);require(KeepMismatchReferenceR31.authorityProof(db)!=null,"LEGAL_FALLBACK_P05_AUTHORITY_PROOF");System.out.println("KEEP_NATIVE_MISMATCH_INITIALIZED_TYPED_FALLBACK_NO_REAL_GRANT");return;
            }
            if(mode.equals("mismatch-one")){KeepMigrationR31.step(adapter(db,files,""),KeepMismatchReferenceR31.ARCHIVE);return;}
            if(mode.equals("mismatch-cutover")){writer(db,args.length>2?args[2]:"").write("FIXTURE_REAL_CUTOVER",connection->{require(KeepMismatchReferenceR31.advanceCutoverWithinMigrationR31(connection),"ACTUAL_CUTOVER_UNIT");});return;}
            if(mode.equals("mismatch-retire")){require(KeepMismatchReferenceR31.retireOne(db,files,writer(db,args.length>2?args[2]:"")),"ACTUAL_RETIRE_UNIT");return;}
            if(mode.equals("mismatch-produce")){require(KeepMismatchReferenceR31.archiveOne(db,files,writer(db,args.length>2?args[2]:""),com.ethk3.novaonepro.TargetIssueResolver.sequenceOrdinalSU1("202611130100")-5000),"ACTUAL_REFERENCE_PRODUCER_UNIT");return;}
            if(mode.equals("mismatch-verify")){
                require(KeepMismatchReferenceR31.mode(db).equals("REFERENCE_ONLY"),"ACTUAL_REFERENCE_ONLY_SWITCH");
                require(n(db,"SELECT count(*) FROM r30_keep_r30_decision_mismatch")==0,"LEGACY_KEEP_ACTUALLY_SHRANK");require(n(db,"SELECT count(*) FROM r30_decision_mismatch")==0,"PRODUCER_SOURCE_ROW_ARCHIVED");
                require(n(db,"SELECT min(archived_at) FROM r31_keep_mismatch_original")>2,"ARCHIVE_RECEIPT_TIME_IS_ACTUAL_COMMIT_NOT_OLD_OBSERVATION_TIME");
                require(n(db,"SELECT count(*) FROM r31_keep_mismatch_permit")==0&&n(db,"SELECT count(*) FROM r31_keep_retire_permit")==0,"NO_LOST_PENDING_PERMITS");
                require(n(db,KeepMismatchReferenceR31.countSql(db,null))==2,"FULL_HISTORICAL_DENOMINATOR_PRESERVED");require(n(db,KeepMismatchReferenceR31.countSql(db,"ORIGINAL_MORE_PERMISSIVE"))==1,"UNSAFE_HISTORY_NOT_ERASED");require(((Number)DecisionExplanationStoreR30.report(db).get("mismatches")).longValue()==2,"ACTUAL_DECISION_REPORT_CONSUMER");
                adapter(db,files,"");try(android.database.Cursor c=db.rawQuery("SELECT archive_id,issue,decision_key FROM r31_keep_mismatch_original ORDER BY archive_id",null)){int seen=0;while(c.moveToNext()){Object[] values=KeepMigrationStoreR31.readOriginal(db,KeepMismatchReferenceR31.ARCHIVE,c.getLong(0),"issue","decision_key");require(c.getString(1).equals(values[0])&&c.getString(2).equals(values[1]),"POST_RETIRE_ACTUAL_COMPLETE_ORIGINAL_CONSUMER");seen++;}require(seen==2,"BOTH_ORIGINALS_RETAINED");}
                db.jdbc.setAutoCommit(false);try{java.lang.reflect.Method install=ResearchRetentionStoreR30.class.getDeclaredMethod("ensureProtectedObjectsR31",SQLiteDatabase.class);install.setAccessible(true);install.invoke(null,db);db.jdbc.commit();}finally{db.jdbc.setAutoCommit(true);}
                require(n(db,"SELECT count(*) FROM sqlite_master WHERE name='r30_keep_r30_decision_mismatch_immutable_delete'")==0,"OWNER_DID_NOT_RECREATE_RETIRED_GUARD");ResearchRetentionStoreR30.requireKeepMigrationSourceR31(db,KeepMismatchReferenceR31.ARCHIVE);
                db.execSQL("INSERT INTO r30_decision_mismatch VALUES('202611130099','current-tail',1,0,'ORIGINAL_MORE_PERMISSIVE','tail',NULL,3)");
                require(!KeepMismatchReferenceR31.archiveOne(db,files,writer(db,""),com.ethk3.novaonepro.TargetIssueResolver.sequenceOrdinalSU1("202611130100")-5000),"FIVE_THOUSAND_PERIOD_TAIL_KEPT");
                try{db.execSQL("DELETE FROM r30_decision_mismatch");throw new AssertionError("UNPERMITTED_SOURCE_DELETE");}catch(android.database.sqlite.SQLiteException expected){require(expected.getMessage().contains("PERMIT_REQUIRED"),"PRODUCER_PERMIT_ENFORCED");}
                db.execSQL("PRAGMA recursive_triggers=OFF");
                try{db.execSQL("INSERT OR REPLACE INTO r30_decision_mismatch VALUES('202611130099','current-tail',0,0,'ORIGINAL_STRICTER','conflicting replace',NULL,3)");throw new AssertionError("REPLACE_BYPASSED_ARCHIVE");}catch(android.database.sqlite.SQLiteException expected){require(expected.getMessage().contains("INSERT_CONFLICT"),"REPLACE_BYPASS_BLOCKED");}
                require(n(db,KeepMismatchReferenceR31.countSql(db,"ORIGINAL_MORE_PERMISSIVE"))==2,"CONFLICT_LEFT_ORIGINAL_UNCHANGED");
                db.execSQL("ALTER TABLE r30_decision_mismatch ADD COLUMN unregistered_extra BLOB");
                try{ResearchRetentionStoreR30.requireKeepMigrationSourceR31(db,KeepMismatchReferenceR31.ARCHIVE);throw new AssertionError("UNDECLARED_SOURCE_COLUMN_ACCEPTED");}catch(com.ethk3.novaonepro.DatabaseCoordinator.SchemaPendingR31 expected){require((expected.getMessage().contains("COLUMNS_OR_TYPES_CHANGED")||expected.getMessage().contains("OWNER_INDEX_CHANGED")),"SOURCE_CONTRACT_DRIFT_BLOCKED");}
                System.out.println("KEEP_NATIVE_REFERENCE_CUTOVER_PERMIT_NEW_WRITES_AND_ORIGINAL_CONSUMERS_PASS");return;
            }
            if(mode.equals("init")){
                db.execSQL("CREATE TABLE r27_policy(policy_hash TEXT PRIMARY KEY,payload BLOB,txt TEXT,integer_value INTEGER,real_value REAL,empty_value BLOB,null_value BLOB)");
                db.execSQL("CREATE TABLE critical_probe(id INTEGER PRIMARY KEY AUTOINCREMENT,value TEXT NOT NULL)");
                db.jdbc.setAutoCommit(false);
                java.lang.reflect.Method install=ResearchRetentionStoreR30.class.getDeclaredMethod("ensureProtectedObjectsR31",SQLiteDatabase.class);install.setAccessible(true);install.invoke(null,db);
                for(String sql:KeepMigrationStoreR31.DDL)db.execSQL(sql);
                for(int i=1;i<=4;i++){byte[] bytes=new byte[160001+i];for(int j=0;j<bytes.length;j++)bytes[j]=(byte)(j*31+i);db.execSQL("INSERT INTO r27_policy VALUES(?,?,?,?,?,?,NULL)",new Object[]{"policy-"+i,bytes,"原件' IF NOT EXISTS ; \u0000 "+i,Long.MIN_VALUE+i,0.12345678901234567,new byte[0]});}
                db.execSQL("DELETE FROM r27_policy");db.jdbc.commit();db.jdbc.setAutoCommit(true);
                require(n(db,"SELECT count(*) FROM "+archive)==4,"ACTUAL_LEGACY_PRODUCER_COPY");System.out.println("KEEP_NATIVE_INITIALIZED");return;
            }
            if(mode.equals("append")){
                byte[] bytes=new byte[160006];for(int j=0;j<bytes.length;j++)bytes[j]=(byte)(j*31+5);db.execSQL("INSERT INTO r27_policy VALUES(?,?,?,?,?,?,NULL)",new Object[]{"policy-5",bytes,"原件' IF NOT EXISTS ; \u0000 "+5,Long.MIN_VALUE+5,0.12345678901234567,new byte[0]});db.execSQL("DELETE FROM r27_policy");return;
            }
            if(mode.equals("one")){
                KeepMigrationStoreR31 store=adapter(db,files,args.length>2?args[2]:"");KeepMigrationR31.Stage stage=KeepMigrationR31.step(store,archive);
                try(Connection critical=DriverManager.getConnection("jdbc:sqlite:"+new File(databases,"keep.db").getPath())){critical.createStatement().execute("INSERT INTO critical_probe(value) VALUES('actual independent commit between units')");}
                System.out.println("KEEP_NATIVE_STAGE="+stage);return;
            }
            if(mode.equals("verify")){
                Map<String,Object> cp=adapter(db,files,"").checkpoint(archive);require("SWITCH".equals(cp.get("stage")),"NO_FAKE_DONE");
                require("WAIT_SCHEMA_OWNED_READER_PRODUCER_AND_P05_CUTOVER".equals(cp.get("blockedReason")),"CUTOVER_DEFAULT_DENY");
                require(n(db,"SELECT count(*) FROM "+archive)==5,"ORIGINAL_ROWS_PRESERVED");require(n(db,"SELECT count(*) FROM r31_keep_original WHERE state='MIGRATED_ORIGINAL_RETAINED'")==5,"EVERY_ORIGINAL_REFERENCE_COMMITTED");
                for(long id=1;id<=5;id++){
                    Object[] original=KeepMigrationStoreR31.readOriginal(db,archive,id,"policy_hash","payload","txt","integer_value","real_value","empty_value","null_value");
                    require(original.length==7&&original[0].equals("policy-"+id),"ACTUAL_CAS_CONSUMER_ID");byte[] bytes=(byte[])original[1];require(bytes.length==160001+id,"FULL_BLOB_LENGTH");for(int j=0;j<bytes.length;j++)require(bytes[j]==(byte)(j*31+id),"FULL_BLOB_BYTE_IDENTITY");
                    require(original[2].equals("原件' IF NOT EXISTS ; \u0000 "+id),"TEXT_LITERAL_BYTES");require(((Long)original[3])==Long.MIN_VALUE+id,"INTEGER_STORAGE_CLASS");require(Double.doubleToRawLongBits((Double)original[4])==Double.doubleToRawLongBits(0.12345678901234567),"REAL_IEEE754_IDENTITY");require(((byte[])original[5]).length==0&&original[6]==null,"EMPTY_IS_NOT_NULL");
                }
                long before=n(db,"SELECT total_changes()");Map<String,Object> report=KeepMigrationStoreR31.report(db);require(n(db,"SELECT total_changes()")==before,"REPORT_READ_ONLY");require(Boolean.TRUE.equals(report.get("readOnly")),"REPORT_ACTUAL_STATUS");
                require(n(db,"SELECT count(*) FROM critical_probe")>8,"ACTUAL_BETWEEN_UNIT_COMMITS");
                List<KeepMigrationStoreR31.ArtifactRef> first=KeepMigrationStoreR31.snapshotArtifactPage(db,"",2);require(first.size()==2,"ACTUAL_SNAPSHOT_FIRST_KEYSET_PAGE");List<KeepMigrationStoreR31.ArtifactRef> second=KeepMigrationStoreR31.snapshotArtifactPage(db,first.get(1).hash,2);require(second.size()==2&&first.get(1).hash.compareTo(second.get(0).hash)<0,"ACTUAL_SNAPSHOT_SECOND_KEYSET_PAGE");List<KeepMigrationStoreR31.ArtifactRef> last=KeepMigrationStoreR31.snapshotArtifactPage(db,second.get(1).hash,2);require(last.size()==1,"APPENDED_ORIGINAL_INCLUDED_IN_SNAPSHOT_CLOSURE");require(((Number)cp.get("lowerRowId")).longValue()==4&&((Number)cp.get("upperRowId")).longValue()==5,"NEW_WRITES_RESUME_NEXT_BOUNDED_RANGE");
                db.jdbc.setAutoCommit(false);try{KeepMigrationStoreR31.readOriginal(db,archive,1,"payload");throw new AssertionError("CAS_READ_INSIDE_WRITER");}catch(IllegalStateException expected){require(expected.getMessage().contains("PREPARATION_OUTSIDE_WRITER"),"NO_CAS_FILE_READ_WHILE_WRITER_HELD");}finally{db.jdbc.rollback();db.jdbc.setAutoCommit(true);}
                try{adapter(db,files,"").rowIds(archive);throw new AssertionError("FULL_TABLE_ENUMERATION");}catch(IllegalStateException expected){require(expected.getMessage().contains("FORBIDDEN"),"KEYSET_ONLY");}
                try(SQLiteDatabase snapshot=SQLiteDatabase.openDatabase(new File(databases,"snapshot.db").getPath(),null,0)){
                    try(KeepMigrationStoreR31.ReaderRootLease lease=KeepMigrationStoreR31.openReaderRoot(snapshot,files)){}
                    try{java.lang.reflect.Method method=KeepMigrationStoreR31.class.getDeclaredMethod("originalsRoot",SQLiteDatabase.class);method.setAccessible(true);method.invoke(null,snapshot);throw new AssertionError("CLOSED_SNAPSHOT_LEASE_REUSED");}catch(java.lang.reflect.InvocationTargetException expected){require(expected.getCause() instanceof IOException,"SNAPSHOT_ROOT_LEASE_CLOSED");}
                }
                String hash;try(android.database.Cursor c=db.rawQuery("SELECT row_hash FROM r31_keep_original ORDER BY row_id LIMIT 1",null)){c.moveToFirst();hash=c.getString(0);}
                File artifact=new File(files,hash+".row");try(RandomAccessFile f=new RandomAccessFile(artifact,"rw")){f.seek(20);f.write(99);}
                try{KeepMigrationStoreR31.readOriginal(db,archive,1,"policy_hash");throw new AssertionError("CORRUPT_CAS_ACCEPTED");}catch(IllegalStateException expected){require(expected.getMessage().contains("READER_INTEGRITY"),"CORRUPT_CAS_BLOCKED");}
                db.execSQL("DROP TRIGGER r30_keep_r27_policy_immutable_update");db.execSQL("CREATE TRIGGER r30_keep_r27_policy_immutable_update BEFORE UPDATE ON r30_keep_r27_policy BEGIN SELECT 1; END");
                try{ResearchRetentionStoreR30.requireKeepMigrationSourceR31(db,archive);throw new AssertionError("UNKNOWN_GUARD_ACCEPTED");}catch(com.ethk3.novaonepro.DatabaseCoordinator.SchemaPendingR31 expected){require(expected.getMessage().contains("EXACT_GUARD_MISMATCH"),"UNKNOWN_GUARD_BLOCKED");}
                System.out.println("KEEP_NATIVE_STORE_CONSUMERS_BYTE_IDENTITY_NEGATIVES_PASS");return;
            }
            throw new IllegalArgumentException("UNKNOWN_HOST_MODE");
        }
    }
}
