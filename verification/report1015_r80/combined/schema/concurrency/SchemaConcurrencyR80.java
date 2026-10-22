package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import java.lang.reflect.Field;
import java.util.concurrent.atomic.*;
/** Deterministic existing-writer contention; no production thread/lock substitutions. */
public final class SchemaConcurrencyR80 {
 static int checks;
 static void ok(boolean v,String why){checks++;if(!v)throw new AssertionError(why+" / "+ResearchSchemaCoordinatorR926.summary());}
 static AtomicBoolean ready(DatabaseCoordinator o)throws Exception{Field f=DatabaseCoordinator.class.getDeclaredField("r811ResearchSchemaReady");f.setAccessible(true);return (AtomicBoolean)f.get(o);}
 static void contention(DatabaseCoordinator owner, boolean publicBoolean)throws Exception {
  ready(owner).set(false); // fresh process-cache state, durable schema is already exact READY
  Field f=DatabaseCoordinator.class.getDeclaredField("waitingWriters");f.setAccessible(true);AtomicInteger waiting=(AtomicInteger)f.get(owner);
  AtomicReference<Throwable> error=new AtomicReference<>();AtomicReference<ResearchSchemaCoordinatorR926.StepResult> result=new AtomicReference<>();AtomicBoolean value=new AtomicBoolean(true);
  Thread loser=new Thread(()->{try{if(publicBoolean)value.set(owner.ensureR811ResearchSchemaOnce());else{result.set(ResearchSchemaCoordinatorR926.ensureOneStepResult(owner));value.set(result.get().ready);}}catch(Throwable e){error.set(e);}},"competing-schema-caller");
  owner.runWriteOrThrowF01("TEST_EXISTING_WRITER",db->{
   loser.start();long end=System.nanoTime()+150_000_000L;while(waiting.get()<2&&System.nanoTime()<end)Thread.yield();ok(waiting.get()>=2,"competitor entered original writer acquisition");
   ok(owner.ensureR811ResearchSchemaOnce(),"reentrant real-schema winner succeeds");ok(owner.isR811ResearchSchemaReady(),"winner publishes cache under writer");
   loser.join(5000);ok(!loser.isAlive(),"no lock-order deadlock");ok(error.get()==null,"public invocation preserves nonthrowing busy behavior");ok(!value.get(),"competing invocation returns false");
   ok(owner.isR811ResearchSchemaReady(),"late false cannot demote READY cache");ok(ResearchSchemaCoordinatorR926.summary().startsWith("READY|"),"late busy cannot overwrite READY state");
   ok(SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.researchContract()),"durable schema stays READY");
   if(!publicBoolean){ok(!result.get().enteredWriter,"busy invocation accurately reports no callback");ok("WRITER_BUSY".equals(result.get().status),"busy is deferred not schema failed");ok(result.get().cause!=null&&result.get().detail.contains("CP3_DB_BUSY_DEADLINE"),"original invocation failure cause retained");}
   return true;
  });
 }
 static String journal(SQLiteDatabase db){StringBuilder b=new StringBuilder();try(Cursor c=db.rawQuery("SELECT * FROM v57_schema_migration_journal_r926 WHERE step_id<>'R952_EXTENSION' AND step_id NOT LIKE 'R64_CP4_DDL_%' ORDER BY domain,revision,step_id",null)){while(c.moveToNext()){for(int i=0;i<c.getColumnCount();i++)b.append(c.getString(i)).append('|');b.append('\n');}}return b.toString();}
 static void incompleteProgress(DatabaseCoordinator owner,SQLiteDatabase db)throws Exception{
  db.beginTransaction();try{
   db.execSQL("DELETE FROM v57_schema_migration_journal_r926 WHERE step_id='R952_EXTENSION' OR (step_id LIKE 'R64_CP4_DDL_%' AND CAST(substr(step_id,13) AS INTEGER)>=54)");
   db.execSQL("DROP TRIGGER trg_r926_runtime_event_no_delete");
   // Synthetic same-revision intermediate boundary; retains every baseline receipt.
   db.execSQL("UPDATE v57_schema_domain_r926 SET state='INITIALIZING',fingerprint='intermediate-fixture' WHERE domain='RESEARCH'");
   String prior=journal(db);ready(owner).set(false);
   Field wf=DatabaseCoordinator.class.getDeclaredField("waitingWriters");wf.setAccessible(true);AtomicInteger waiting=(AtomicInteger)wf.get(owner);
   AtomicReference<ResearchSchemaCoordinatorR926.StepResult> loserResult=new AtomicReference<>();Thread loser=new Thread(()->loserResult.set(ResearchSchemaCoordinatorR926.ensureOneStepResult(owner)));
   owner.runWriteOrThrowF01("TEST_PARTIAL_EXISTING_WRITER",x->{
    loser.start();long end=System.nanoTime()+150_000_000L;while(waiting.get()<2&&System.nanoTime()<end)Thread.yield();ok(waiting.get()>=2,"partial competitor awaiting writer");
    ResearchSchemaCoordinatorR926.StepResult first=ResearchSchemaCoordinatorR926.ensureOneStepResult(owner);ok(!first.ready&&first.enteredWriter,"one partial CP4 slice only");
    loser.join(5000);ok(!loser.isAlive()&&"WRITER_BUSY".equals(loserResult.get().status),"partial contender expires without deadlock");ok(ResearchSchemaCoordinatorR926.summary().startsWith("INITIALIZING|"),"contender cannot poison progressing state");
    ResearchSchemaCoordinatorR926.StepResult next=ResearchSchemaCoordinatorR926.ensureOneStepResult(owner);ok(!next.ready&&next.detail.isEmpty()&&next.cause==null,"subsequent partial progress clears stale failure");
    ok(prior.equals(journal(db)),"old completed journal rows never replay after contention");
    try(Cursor c=db.rawQuery("SELECT state FROM v57_schema_domain_r926 WHERE domain='RESEARCH'",null)){ok(c.moveToFirst()&&"INITIALIZING".equals(c.getString(0)),"durable intermediate state never poisoned FAILED");}
    return true;
   });
  }finally{db.endTransaction();}
  ok(owner.ensureR811ResearchSchemaOnce(),"partial rollback recovers original identity");
 }
 public static void main(String[] args)throws Exception{try(SQLiteDatabase db=new SQLiteDatabase(args[0],args[1])){
  DatabaseCoordinator owner=HostProductR974.store(db).coordinatorR36();
  contention(owner,true);contention(owner,false);
  ok(!ResearchSchemaCoordinatorR926.ensureToReady(null,1),"another call global READY cannot make null owner ready");
  db.beginTransaction();try{
   db.execSQL("DROP TRIGGER r80_probe_input_no_update");db.execSQL("CREATE TRIGGER r80_probe_input_no_update BEFORE UPDATE ON r80_probe_input BEGIN SELECT 1; END");
   ResearchSchemaCoordinatorR926.StepResult result=ResearchSchemaCoordinatorR926.ensureOneStepResult(owner);
   ok(result.enteredWriter&&!result.ready&&"FAILED".equals(result.status),"actual malformed schema is genuine entered failure");
   ok(result.cause!=null&&result.detail.contains("PROBE_SCHEMA_DRIFT"),"actual migration Throwable retained");ok(owner.startupSchemaSummary().contains("researchOwnerError=")&&owner.startupSchemaSummary().contains("PROBE_SCHEMA_DRIFT"),"actual startup diagnostic retains genuine failure after runWrite recordSuccess");ok(!owner.isR811ResearchSchemaReady(),"real invalidation clears cache under writer");
  }finally{db.endTransaction();}
  ok(owner.ensureR811ResearchSchemaOnce(),"rollback recovery verifies actual schema");
  db.beginTransaction();try{
   db.execSQL("UPDATE v57_schema_domain_r926 SET contract_hash='synthetic-drift' WHERE domain='RESEARCH'");
   ResearchSchemaCoordinatorR926.StepResult result=ResearchSchemaCoordinatorR926.ensureOneStepResult(owner);
   ok(!result.ready&&result.enteredWriter,"explicit identity revalidation does not trust old READY latch");ok(!owner.isR811ResearchSchemaReady(),"identity drift invalidates correct owner cache");
  }finally{db.endTransaction();}
  ok(owner.ensureR811ResearchSchemaOnce(),"identity rollback recovers");
  db.beginTransaction();try{
   db.execSQL("DROP TRIGGER r80_probe_input_no_update");
   ResearchSchemaCoordinatorR926.StepResult result=ResearchSchemaCoordinatorR926.ensureOneStepResult(owner);ok(result.ready&&result.detail.isEmpty()&&result.cause==null,"successful repair clears old error and cause");
  }finally{db.endTransaction();}
  ResearchWorkBudgetR927.Scope budget=ResearchWorkBudgetR927.enter(1L);boolean yielded=false;try{Thread.sleep(5L);ResearchSchemaCoordinatorR926.ensureOneStepResult(owner);}catch(ResearchWorkBudgetR927.Yield expected){yielded=true;}finally{ResearchWorkBudgetR927.leave(budget);}
  ok(yielded,"cooperative budget still propagates unchanged");ok(owner.isR811ResearchSchemaReady(),"budget deferral does not demote completed identity");
  incompleteProgress(owner,db);
  boolean mainRejected=false;android.os.Looper.setMainForTest(true);try{ResearchSchemaCoordinatorR926.ensureOneStepResult(owner);}catch(IllegalStateException e){mainRejected="CP3_DB_WRITE_ON_MAIN_THREAD".equals(e.getMessage());}finally{android.os.Looper.setMainForTest(false);}
  ok(mainRejected,"main-thread programming guard remains fail-loud");
  System.out.println("SCHEMA_CONCURRENCY_PASS checks="+checks);
 }}
}
