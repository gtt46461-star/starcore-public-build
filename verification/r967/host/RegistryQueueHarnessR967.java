package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Executes production registry/queue/retention SQL, including rollback and stale-lease cases. */
public final class RegistryQueueHarnessR967 {
 static int checks;
 interface Op{void run();}
 static void check(boolean v,String name){if(!v)throw new AssertionError(name);checks++;System.out.println("PASS "+name);}
 static void reject(String token,Op action){try{action.run();throw new AssertionError("Expected "+token);}catch(RuntimeException e){check(e.getMessage()!=null&&e.getMessage().contains(token),"REJECT_"+token);}}
 static void tx(SQLiteDatabase d,Op op){d.beginTransaction();try{op.run();d.setTransactionSuccessful();}finally{d.endTransaction();}}
 static long n(SQLiteDatabase d,String sql){try(Cursor c=d.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
 static String text(SQLiteDatabase d,String sql){try(Cursor c=d.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):"";}}
 static String h(String v){return V51ChecksumEngine.sha256(v);}
 public static void main(String[] args)throws Exception {
  final SQLiteDatabase d=new SQLiteDatabase(args[0]);try{
   tx(d,()->ResearchMultiplicityR932.schema(d));
   String empty=h("empty");
   tx(d,()->{
    for(int i=0;i<200;i++)ResearchMultiplicityR932.registerCandidate(d,"genesis:C"+i,"EXPLORATORY",h("structure"+i),"202609200001");
   });
   check(n(d,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.RESEARCH)==200,"RESEARCH_ENROLLMENT_PERSISTED");
   check(n(d,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.FAMILY)==0&&n(d,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.HYPOTHESIS)==0,"EXPLORATION_CONSUMES_NO_ALPHA");
   String study=CandidateCalibrationR932.study(d,"genesis:C0",h("structure0"),h("bundle"),h("selector"));
   check(study.matches("[0-9a-f]{64}"),"CALIBRATION_STUDY_BEFORE_FORMAL_REGISTRATION");
   tx(d,()->ResearchMultiplicityR932.registerCandidate(d,"genesis:C0","EXPLORATORY",h("structure0"),"202609200002"));
   check(n(d,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.RESEARCH)==200,"ENROLLMENT_REPLAY_IDEMPOTENT");
   reject("IDENTITY_REUSED",()->tx(d,()->ResearchMultiplicityR932.registerCandidate(d,"genesis:C0","EXPLORATORY",h("changed"),"202609200001")));
   reject("FAMILY_REASSIGNMENT",()->tx(d,()->ResearchMultiplicityR932.registerCandidate(d,"other","OTHER",h("structure0"),"202609200001")));
   final double[] alpha={0};
   tx(d,()->alpha[0]=ResearchMultiplicityR932.registerLook(d,"genesis:C0","W1",1,"202609200002","202609200121"));
   check(alpha[0]>0&&alpha[0]<.05,"FORMAL_LOOK_GETS_PREEXISTING_ALLOCATION_RULE");
   check(n(d,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.HYPOTHESIS)==1&&n(d,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.LOOK)==1,"ONLY_FORMAL_LOOK_SPENDS_ALPHA");
   check(study.equals(CandidateCalibrationR932.study(d,"genesis:C0",h("structure0"),h("bundle"),h("selector"))),"CALIBRATION_IDENTITY_UNCHANGED_BY_FORMAL_ALLOCATION");
   tx(d,()->ResearchMultiplicityR932.registerLook(d,"genesis:C0","W1",1,"202609200002","202609200121"));
   check(n(d,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.LOOK)==1,"FORMAL_LOOK_REPLAY_NO_SECOND_SPENDING");
   reject("RETRY_REJECTED",()->tx(d,()->ResearchMultiplicityR932.registerLook(d,"genesis:C0","renamed",1,"202609200003","202609200122")));
   reject("DENOMINATOR_PERMANENT",()->d.execSQL("DELETE FROM "+ResearchMultiplicityR932.HYPOTHESIS));
   reject("IDENTITY_IMMUTABLE",()->d.execSQL("UPDATE "+ResearchMultiplicityR932.RESEARCH+" SET structure_hash='x'"));
   tx(d,()->ResearchMultiplicityR932.schema(d));
   check(n(d,"SELECT COUNT(*) FROM "+ResearchMultiplicityR932.HYPOTHESIS)==1,"REENTER_SCHEMA_PRESERVES_SPENT_HISTORY");
   tx(d,()->ResearchTaskQueueR967.schema(d));
   d.execSQL("CREATE TABLE retained_input(issue TEXT PRIMARY KEY,value TEXT)");
   d.execSQL("INSERT INTO retained_input VALUES('202609190001','exact-input')");
   final String[] id={""};
   tx(d,()->{id[0]=ResearchTaskQueueR967.enqueue(d,"UNIT","C1","202609190001","SEQ1",h("input"),h("authority"));ResearchTaskQueueR967.pin(d,id[0],"retained_input","202609190001");});
   check(DataRetentionGovernorR900.deletePage(d,ResearchTaskQueueR967.TABLE,"target_issue","202609200001",512)==0,"PENDING_TASK_SURVIVES_5000_ISSUE_TRIM");
   check(DataRetentionGovernorR900.deletePage(d,"retained_input","issue","202609200001",512)==0,"PENDING_TASK_INPUT_SURVIVES_TRIM");
   reject("PENDING_INPUT_PROTECTED",()->d.execSQL("DELETE FROM "+ResearchTaskQueueR967.PIN));
   reject("PENDING_TASK_PROTECTED",()->d.execSQL("DELETE FROM "+ResearchTaskQueueR967.TABLE));
   tx(d,()->check(id[0].equals(ResearchTaskQueueR967.enqueue(d,"UNIT","C1","202609190001","SEQ1",h("input"),h("authority"))),"NATURAL_KEY_IDEMPOTENT"));
   reject("INPUT_DRIFT",()->tx(d,()->ResearchTaskQueueR967.enqueue(d,"UNIT","C1","202609190001","SEQ1",h("changed"),h("authority"))));
   check(!ResearchTaskQueueR967.naturalKey("a|b","c","202609190001","d").equals(ResearchTaskQueueR967.naturalKey("a","b|c","202609190001","d")),"LENGTH_FRAMED_NATURAL_KEY_NO_DELIMITER_COLLISION");
   final ResearchTaskQueueR967.Lease[] leases=new ResearchTaskQueueR967.Lease[3];long now=System.currentTimeMillis();
   tx(d,()->leases[0]=ResearchTaskQueueR967.claim(d,"UNIT",now));
   tx(d,()->check(ResearchTaskQueueR967.claim(d,"UNIT",now)==null,"ACTIVE_LEASE_NOT_STOLEN"));
   tx(d,()->leases[1]=ResearchTaskQueueR967.claim(d,"UNIT",now+ResearchTaskQueueR967.LEASE_MS+1));
   tx(d,()->check(!ResearchTaskQueueR967.owns(d,leases[0]),"EXPIRED_GENERATION_FENCED_AFTER_RECOVERY"));
   reject("STALE_TASK_LEASE",()->tx(d,()->ResearchTaskQueueR967.complete(d,leases[0],h("stale"))));
   tx(d,()->check(!ResearchTaskQueueR967.yield(d,leases[0],"bad","stale",0),"STALE_YIELD_CANNOT_OVERWRITE_CURRENT_CURSOR"));
   tx(d,()->check(ResearchTaskQueueR967.yield(d,leases[1],"cursor=12","RESOURCE_YIELD",0),"CURSOR_AND_YIELD_COMMIT_TOGETHER"));
   tx(d,()->leases[2]=ResearchTaskQueueR967.claim(d,"UNIT",System.currentTimeMillis()));
   check("cursor=12".equals(leases[2].cursor),"CURSOR_RESUMES_AFTER_NEW_LEASE");
   d.execSQL("CREATE TRIGGER inject_complete BEFORE UPDATE ON "+ResearchTaskQueueR967.TABLE+" WHEN NEW.status='COMPLETED' BEGIN SELECT RAISE(ABORT,'INJECT_ACK'); END");
   reject("INJECT_ACK",()->tx(d,()->{d.execSQL("UPDATE retained_input SET value='owner-side-effect'");ResearchTaskQueueR967.complete(d,leases[2],h("result"));}));
   check("exact-input".equals(text(d,"SELECT value FROM retained_input")),"TASK_ACK_FAILURE_ROLLS_BACK_OWNER_WRITES");
   d.execSQL("DROP TRIGGER inject_complete");
   tx(d,()->ResearchTaskQueueR967.complete(d,leases[2],h("result")));
   check(n(d,"SELECT COUNT(*) FROM "+ResearchTaskQueueR967.PIN)==0,"INPUT_PINS_RELEASE_ONLY_AFTER_COMPLETION");
   check(DataRetentionGovernorR900.deletePage(d,"retained_input","issue","202609200001",512)==1,"COMPLETED_INPUT_CAN_BE_RECLAIMED");
   reject("TERMINAL_IMMUTABLE",()->d.execSQL("UPDATE "+ResearchTaskQueueR967.TABLE+" SET status='PENDING'"));
   check(DataRetentionGovernorR900.deletePage(d,ResearchTaskQueueR967.TABLE,"target_issue","202609200001",512)==1,"COMPLETED_INDEX_WITHOUT_AUTHORITY_CAN_BE_TRIMMED");
   // Capacity is an operational admission guard, never a quality judgment or silent deletion.
   d.execSQL("WITH RECURSIVE nums(n) AS (SELECT 1 UNION ALL SELECT n+1 FROM nums WHERE n<4096) INSERT INTO "+ResearchTaskQueueR967.TABLE+"(task_id,kind,candidate_id,target_issue,stream_seq,input_hash,authority_ref,status,created_at,updated_at) SELECT 'capacity-'||n,'LOAD','C'||n,'202609190001','SEQ','HASH','REF','PENDING',1,1 FROM nums");
   reject("BACKPRESSURE",()->tx(d,()->ResearchTaskQueueR967.enqueue(d,"LOAD","EXTRA","202609190001","SEQ","HASH","REF")));
   check(n(d,"SELECT COUNT(*) FROM "+ResearchTaskQueueR967.TABLE)==4096,"BACKPRESSURE_PRESERVES_ALL_PENDING_INPUTS");
   System.out.println("REGISTRY_QUEUE_PASS checks="+checks+"; actual_product_SQL=YES; Android_lifecycle=NOT_RUN");
  }finally{d.shutdown();}
 }
}
