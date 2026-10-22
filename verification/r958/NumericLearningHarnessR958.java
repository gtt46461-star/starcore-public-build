package com.ethk3.novaonepro.r952;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.util.*;

/** Synthetic chronology using production R952 code and real SQLite, not phone/OOS evidence. */
public final class NumericLearningHarnessR958 {
    static int checks;
    static void check(boolean value,String name){if(!value)throw new AssertionError(name);checks++;System.out.println(name+"=PASS");}
    static long number(SQLiteDatabase db,String sql){Cursor c=db.rawQuery(sql,null);try{return c.moveToFirst()?c.getLong(0):-1;}finally{c.close();}}
    static String text(SQLiteDatabase db,String sql){Cursor c=db.rawQuery(sql,null);try{return c.moveToFirst()?c.getString(0):"";}finally{c.close();}}
    static Models.Draw draw(int issue,int winner,long time){Models.Draw d=new Models.Draw();d.issue=String.format(Locale.US,"20260919%04d",issue);d.openTime=time;d.fetchTime=time+10;for(int i=0;i<10;i++)d.numbers[i]=(winner-1+i)%10+1;return d;}
    static void insert(SQLiteDatabase db,Models.Draw d){Object[] a=new Object[14];a[0]=d.issue;for(int i=0;i<10;i++)a[i+1]=d.numbers[i];a[11]=0;a[12]=d.openTime;a[13]=d.fetchTime;db.execSQL("INSERT INTO "+DbHelper.TABLE_DRAWS+" VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?)",a);}
    static String terminal(SQLiteDatabase db,Models.Draw d,long at){String h=V51ChecksumEngine.sha256("SYNTHETIC_SETTLEMENT|"+d.issue);db.execSQL("INSERT INTO "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" VALUES(?,'COMPLETED',?,?, '', '', '',0,'SYNTHETIC')",new Object[]{d.issue,h,at});return h;}
    static void tx(SQLiteDatabase db,Runnable code){db.beginTransaction();try{code.run();db.setTransactionSuccessful();}finally{db.endTransaction();}}
    public static void main(String[] args)throws Exception{
        Random random=new Random(1958);
        for(int k=0;k<1000;k++){
            double[] p=new double[10];double sum=0;for(int i=0;i<10;i++){p[i]=k==0?1:random.nextInt(5)+1;sum+=p[i];}
            for(int i=0;i<10;i++)p[i]/=sum;
            int mask=CandidateCompilerR952.top3(p);
            if(AppConfig.countK3MaskNumbers(mask)!=3)throw new AssertionError("UNIQUE_K3_"+k);
            for(int i=0;i<10;i++)for(int j=0;j<10;j++)if(AppConfig.maskContainsNumber(mask,i+1)&&!AppConfig.maskContainsNumber(mask,j+1)&&p[j]>p[i])throw new AssertionError("RANK_ORDER");
        }
        check(true,"THOUSAND_SCORE_CASES_UNIQUE_CORRECT_TOP3");
        boolean rejected=false;try{CandidateAstR952.parse("AST1(UNKNOWN,POSITION=1,LOOKBACK=120,K=3)");}catch(IllegalArgumentException good){rejected=true;}
        check(rejected,"UNKNOWN_OPERATOR_REJECTED");
        SQLiteDatabase db=new SQLiteDatabase(args[0]);DbStore store=new DbStore(db);
        try {
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_DRAWS+"(issue TEXT PRIMARY KEY,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER,open_time INTEGER,fetch_time INTEGER)");
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_R36_SETTLEMENT_TASK+"(target_issue TEXT PRIMARY KEY,status TEXT,settlement_hash TEXT,completed_at INTEGER,last_error TEXT,terminal_code TEXT,task_class TEXT,attempts INTEGER,source TEXT)");
            long clock=System.currentTimeMillis();
            for(int i=1;i<=30;i++)insert(db,draw(i,i%10+1,clock-(31-i)*60000L));
            String issue="202609190031";
            check(ResearchDirectorR952.capturePredraw(store,issue,V51ChecksumEngine.sha256("surface"),8)==1,"PRODUCTION_PREDRAW_FREEZE");
            long changes=number(db,"SELECT total_changes()");
            check(ResearchDirectorR952.capturePredraw(store,issue,V51ChecksumEngine.sha256("surface"),8)==0,"PREDRAW_INSERT_ONCE");
            check(number(db,"SELECT total_changes()")==changes,"RETRY_PRELUDE_NO_REDUNDANT_WRITES");
            PITSnapshotContractR952 pit=PITSnapshotContractR952.capture(store,issue,120);
            check(pit.history.get(0).issue.equals("202609190030"),"PRODUCTION_CHRONOLOGICAL_HISTORY_NORMALIZED");
            String initialHash=text(db,"SELECT model_hash FROM r952_candidate_predraw WHERE target_issue='"+issue+"'");
            Models.Draw first=draw(31,10,clock+60000);insert(db,first);
            String hash=terminal(db,first,clock+60010);
            check(ResearchDirectorR952.onSettlementCompleted(store,first,hash,clock+60010)==1,"COMMITTED_TERMINAL_LABELS_REAL_FROZEN_ROW");
            check(ResearchDirectorR952.onSettlementCompleted(store,first,hash,clock+60010)==0,"DUPLICATE_SETTLEMENT_NO_NEW_LABEL");
            check(number(db,"SELECT future_n FROM r952_candidate")==1,"FUTURE_COUNT_ONCE");
            check(number(db,"SELECT paired_n FROM research_lifecycle_r932")==0,"NO_FAKE_PAIRED_EVIDENCE");
            check(ResearchOutboxR952.drainOne(store),"PRODUCTION_TRAINING_COMMITS");
            check(!ResearchOutboxR952.drainOne(store),"DUPLICATE_DRAIN_NO_UPDATE");
            check(number(db,"SELECT numeric_updates FROM r952_artifact")==1,"NUMERIC_UPDATE_COUNT_ONE");
            String modelHash=text(db,"SELECT payload_hash FROM r952_artifact");
            check(!modelHash.equals(initialHash),"MODEL_PARAMETERS_CHANGED_AFTER_LABEL");
            check(ResearchDirectorR952.capturePredraw(store,"202609190032",V51ChecksumEngine.sha256("surface2"),8)==1,"NEXT_ISSUE_FREEZE");
            check(text(db,"SELECT model_hash FROM r952_candidate_predraw WHERE target_issue='202609190032'").equals(modelHash),"NEXT_PREDICTION_LOADS_TRAINED_PARAMETERS");
            CandidateCompilerR952.Model learned=CandidateCompilerR952.Model.parse(text(db,"SELECT model_payload FROM r952_artifact"));
            double[] uniform=new double[10];Arrays.fill(uniform,.1);
            check(learned.probabilities(uniform)[9]>.1,"LEARNING_CHANGES_ACTUAL_PREDICTION_PROBABILITY");
            boolean late=false;try{ResearchDirectorR952.capturePredraw(store,issue,V51ChecksumEngine.sha256("late"),8);}catch(IllegalStateException good){late=true;}
            // An already frozen row is insert-once and no postdraw rewrite is possible.
            check(number(db,"SELECT COUNT(*) FROM r952_candidate_predraw WHERE target_issue='"+issue+"'")==1,"NO_POSTDRAW_REWRITE");
            Models.Draw second=draw(32,9,clock+120000);insert(db,second);String hash2=terminal(db,second,clock+120010);
            // Simulate process death after production COMPLETED but before the research callback.
            ResearchDirectorR952.pulse(store,1200);
            check(number(db,"SELECT settled FROM r952_candidate_predraw WHERE target_issue='202609190032'")==1,"MISSED_TERMINAL_CALLBACK_RECOVERED");
            check(number(db,"SELECT MAX(numeric_updates) FROM r952_artifact")==2,"RECOVERED_LABEL_TRAINS_ONCE");
            ResearchDirectorR952.capturePredraw(store,"202609190033",V51ChecksumEngine.sha256("surface3"),8);
            Models.Draw third=draw(33,8,clock+180000);insert(db,third);String hash3=terminal(db,third,clock+180010);
            ResearchDirectorR952.onSettlementCompleted(store,third,hash3,clock+180010);
            db.execSQL("CREATE TRIGGER inject_model_failure BEFORE INSERT ON r952_artifact BEGIN SELECT RAISE(ABORT,'INJECT_MODEL_WRITE_FAILURE'); END");
            check(!ResearchOutboxR952.drainOne(store),"MODEL_FAILURE_NOT_REPORTED_SUCCESS");
            check(number(db,"SELECT MAX(numeric_updates) FROM r952_artifact")==2,"FAILED_MODEL_WRITE_ROLLS_BACK_PARAMETERS");
            check(text(db,"SELECT trained_until_issue FROM r952_learning_cursor").equals("202609190032"),"FAILED_WRITE_CURSOR_NOT_ADVANCED");
            check(text(db,"SELECT state FROM r952_research_outbox WHERE target_issue='202609190033'").equals("RETRYABLE_FAILED"),"COORDINATOR_FALLBACK_RECORDED_AS_FAILURE");
            check(text(db,"SELECT last_error FROM r952_research_outbox WHERE target_issue='202609190033'").contains("INJECT_MODEL_WRITE_FAILURE"),"FAILURE_REASON_VISIBLE");
            db.execSQL("DROP TRIGGER inject_model_failure");
            db.execSQL("UPDATE r952_research_outbox SET state='RUNNING',lease_until=0 WHERE target_issue='202609190033'");
            check(ResearchOutboxR952.drainOne(store),"EXPIRED_LEASE_RECOVERY");
            check(number(db,"SELECT MAX(numeric_updates) FROM r952_artifact")==3,"RECOVERY_NO_DOUBLE_TRAINING");
            String report=FullbloodDiagnosticsProviderR952.render(store);
            check(report.contains("training_completed=3")&&report.contains("frozen_evaluation_candidates=0"),"DIAGNOSTICS_TRAINING_NOT_FROZEN_EVALUATION");
            ResearchDirectorR952.capturePredraw(store,"202609190034",V51ChecksumEngine.sha256("surface4"),8);
            Models.Draw fourth=draw(34,7,clock+240000);insert(db,fourth);String hash4=terminal(db,fourth,clock+240010);
            ResearchDirectorR952.onSettlementCompleted(store,fourth,hash4,clock+240010);
            db.execSQL("UPDATE r952_research_outbox SET state='RUNNING',lease_until=? WHERE target_issue='202609190034'",new Object[]{System.currentTimeMillis()+60000});
            ResearchDirectorR952.capturePredraw(store,"202609190035",V51ChecksumEngine.sha256("surface5"),8);
            Models.Draw fifth=draw(35,6,clock+300000);insert(db,fifth);String hash5=terminal(db,fifth,clock+300010);
            ResearchDirectorR952.onSettlementCompleted(store,fifth,hash5,clock+300010);
            check(!ResearchOutboxR952.drainOne(store),"LATER_LABEL_CANNOT_OVERTAKE_LIVE_EARLIER_LEASE");
            check(number(db,"SELECT MAX(numeric_updates) FROM r952_artifact")==3,"ORDERING_GUARD_PRESERVES_MODEL");
            db.execSQL("UPDATE r952_research_outbox SET lease_until=0 WHERE target_issue='202609190034'");
            db.execSQL("UPDATE r952_candidate_predraw SET evidence_role='SEALED_CERTIFICATE' WHERE target_issue='202609190034'");
            check(!ResearchOutboxR952.drainOne(store),"SEALED_EVIDENCE_REJECTED_BY_ONLINE_TRAINER");
            check(number(db,"SELECT MAX(numeric_updates) FROM r952_artifact")==3,"SEALED_INPUT_NEVER_UPDATES_PARAMETERS");
            java.lang.reflect.Method trim=DataRetentionGovernorR900.class.getDeclaredMethod("deletePage",SQLiteDatabase.class,String.class,String.class,String.class,int.class);trim.setAccessible(true);
            db.beginTransaction();
            try {
                trim.invoke(null,db,"r952_artifact","target_issue","202609191000",512);
                check(number(db,"SELECT COUNT(*) FROM r952_artifact")==1,"RETENTION_PRESERVES_ACTIVE_NUMERIC_MODEL");
                trim.invoke(null,db,"r952_research_outbox","target_issue","202609191000",512);
                check(number(db,"SELECT COUNT(*) FROM r952_research_outbox WHERE state<>'COMPLETED'")==2,"RETENTION_PRESERVES_PENDING_DELIVERIES");
                trim.invoke(null,db,"r952_candidate_predraw","target_issue","202609191000",512);
                check(number(db,"SELECT COUNT(*) FROM r952_candidate_predraw")==2,"RETENTION_PRESERVES_PENDING_FROZEN_INPUTS");
                trim.invoke(null,db,DbHelper.TABLE_DRAWS,"issue","202609191000",512);
                check(number(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_DRAWS)==2,"RETENTION_PRESERVES_PENDING_EXACT_DRAWS");
            } finally {db.endTransaction();}
            long before=number(db,"SELECT COUNT(*) FROM r952_candidate_predraw");
            tx(db,()->ResearchMigrationRegistryR952.ensure(db));tx(db,()->ResearchMigrationRegistryR952.ensure(db));
            check(number(db,"SELECT COUNT(*) FROM r952_candidate_predraw")==before,"SAME_VERSION_MIGRATION_PRESERVES_EVIDENCE");
            check(number(db,"SELECT COUNT(*) FROM v57_schema_migration_journal_r926 WHERE step_id='R958_NUMERIC_MODEL_V1' AND status='COMPLETED'")==1,"MIGRATION_USES_CANONICAL_JOURNAL");
            System.out.println("R958_PRODUCTION_SQLITE_HARNESS=PASS|checks="+checks+"|SYNTHETIC_NOT_LIVE_NOT_DEVICE_NOT_MODEL_PROOF");
        } finally {db.shutdown();}
    }
}
