package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Reproduces the R957 phone's READY stamp with an absent research scheduler. */
public final class ReportedSchemaHarnessR958 {
    private static int checks;
    private static void check(boolean pass,String detail) {
        if(!pass)throw new IllegalStateException(detail);
        checks++;System.out.println("PASS "+detail);
    }
    private static long count(SQLiteDatabase db,String sql) {
        try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):-1;}
    }
    public static void main(String[] args)throws Exception {
        SQLiteDatabase db=new SQLiteDatabase(args[0]);
        try {
            DbStore store=new DbStore(db);DatabaseCoordinator owner=store.coordinatorR36();
            db.execSQL("CREATE TABLE IF NOT EXISTS draws(issue TEXT PRIMARY KEY,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER DEFAULT 0,open_time INTEGER,fetch_time INTEGER)");
            DbHelper.ensureR35Schema(db);DbHelper.ensureR36Schema(db);
            RuntimeEventLedgerR926.ensureSchema(db);DbHelper.ensureR906CriticalSchema(db);
            V51Repository.ensure(db);
            RealtimePressureGateR811.resetForTests();
            check(ResearchSchemaCoordinatorR926.ensureToReady(owner,8),"MIGRATION_REACHES_READY");
            if(args.length>1&&"baseline".equals(args[1])) {
                boolean absent=count(db,"SELECT COUNT(*) FROM sqlite_master WHERE name='research_schedule_plan_r932'")==0;
                System.out.println("BASELINE_FALSE_READY="+absent);
                try{ResearchCaptureQueueR946.recover(store);throw new AssertionError("NO_REPRODUCIBLE_FAILURE");}
                catch(IllegalStateException expected){System.out.println("BASELINE_RECOVERY_FAILURE="+expected.getMessage());}
                return;
            }
            String[] tables={GlobalResearchScheduleR932.PLAN,GlobalResearchScheduleR932.TASK,
                GlobalResearchScheduleR932.IDENTITY,GlobalResearchScheduleR932.PROXY,
                ResearchCaptureLedgerR932.TABLE,ResearchCaptureLedgerR932.EVENT,
                CandidateExecutionAuditR932.TABLE,FixedWindowCertificatePublisherR932.TABLE,
                ResearchCohortR932.IDENTITY,ResearchCohortR932.COMPARISON,ResearchCohortR932.CHAMPION,
                AtomAdmissionAuditR932.AUDIT,AtomAdmissionAuditR932.RECOVERY,AtomAdmissionAuditR932.ARCHIVE,
                ModuleAblationR932.TABLE,ModuleCalibrationR932.OBS,ModuleCalibrationR932.MODEL,
                MolecularToolWorldR947.TABLE,CausalResearchFeedbackR947.TABLE};
            for(String table:tables)check(count(db,"SELECT COUNT(*) FROM sqlite_master WHERE name='"+table+"'")==1,"RUNTIME_TABLE_PRESENT:"+table);
            check("".equals(ResearchCaptureQueueR946.recover(store)),"EMPTY_RECOVERY_QUERY_EXECUTES");
            check(!ResearchCaptureQueueR946.complete(store,"202609190698"),"EMPTY_CAPTURE_NOT_COMPLETE");
            ResearchRuntimeSqlGateR911.Report sql=ResearchRuntimeSqlGateR911.verify(store);
            check(sql.passed,"ACTUAL_RESEARCH_SQL_GATE:"+sql.detail);
            db.execSQL("INSERT INTO "+ResearchPlatformStoreR929.TABLE_EVENT+" VALUES('r958-preserve','TEST','sentinel','','','OK','preserve',1)");
            db.execSQL("DROP TABLE "+GlobalResearchScheduleR932.PLAN);
            check(!SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.researchContract()),"MISSING_SCHEDULER_INVALIDATES_READY");
            sql=ResearchRuntimeSqlGateR911.verify(store);
            check(!sql.passed&&sql.detail.contains(GlobalResearchScheduleR932.PLAN),"SQL_GATE_REJECTS_MISSING_SCHEDULER_WITH_REASON");
            check(ResearchSchemaCoordinatorR926.ensureToReady(owner,8),"SAME_VERSION_REPAIR_REACHES_READY");
            check(count(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_EVENT+" WHERE event_hash='r958-preserve'")==1,"REPAIR_PRESERVES_EXISTING_DATA");
            check("".equals(ResearchCaptureQueueR946.recover(store)),"RECOVERY_QUERY_EXECUTES_AFTER_REPAIR");
            long attempts=count(db,"SELECT SUM(attempt) FROM "+SchemaDomainCoordinatorR926.TABLE_JOURNAL);
            check(ResearchSchemaCoordinatorR926.ensureToReady(owner,8),"REPEATED_ENSURE_READY");
            check(attempts==count(db,"SELECT SUM(attempt) FROM "+SchemaDomainCoordinatorR926.TABLE_JOURNAL),"NO_REPEAT_MIGRATION_AFTER_READY");
            check(count(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE)==0,"NO_FAKE_CERTIFICATE");
            check(count(db,"SELECT COUNT(*) FROM "+ResearchLifecycleR932.TABLE)==0,"NO_FAKE_CHALLENGER");
            for(int i=0;i<9;i++)check(ResearchCaptureQueueR946.claimCapturePriority(store,"202609190"+(698+i))==(i%3!=2),"PERSISTENT_CAPTURE_BACKLOG_ADMITS_CONTINUITY:"+i);
            check(!ResearchCaptureQueueR946.claimCapturePriority(store,""),"EMPTY_CAPTURE_DOES_NOT_DEFER_LEARNING");
            check(ResearchCaptureQueueR946.claimCapturePriority(store,"202609190707"),"FRESH_CAPTURE_HAS_PRIORITY");
            db.execSQL("CREATE TRIGGER fail_priority BEFORE INSERT ON "+DbHelper.TABLE_APP_STATE+" WHEN NEW.state_key='r958_capture_priority_streak' BEGIN SELECT RAISE(ABORT,'INJECTED_PRIORITY_FAILURE'); END");
            check(!ResearchCaptureQueueR946.claimCapturePriority(store,"202609190707"),"FAILED_COUNTER_WRITE_DOES_NOT_STARVE_LEARNING");
            System.out.println("R958_REPORTED_SCHEMA_PASS|checks="+checks+"|scope=PRODUCTION_JAVA_REAL_SQLITE_NOT_PHONE");
        }finally{db.shutdown();}
    }
}
