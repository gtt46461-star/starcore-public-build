package com.ethk3.novaonepro;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import java.lang.reflect.*;import java.util.concurrent.atomic.*;
public final class SchemaDurableRaceR80 {
 static final class PausedDb extends SQLiteDatabase{
  volatile boolean pause; PausedDb(String bridge,String path)throws Exception{super(bridge,path);}
  @Override public Cursor rawQuery(String sql,String[] args){if(pause&&Thread.currentThread().getName().equals("main")&&sql.startsWith("SELECT status FROM v57_schema_migration_journal_r926")){pause=false;try{Thread.sleep(400);}catch(Exception e){throw new RuntimeException(e);}}return super.rawQuery(sql,args);}
 }
 static String rows(SQLiteDatabase db,String sql){StringBuilder b=new StringBuilder();try(Cursor c=db.rawQuery(sql,null)){while(c.moveToNext()){for(int i=0;i<c.getColumnCount();i++)b.append(c.getString(i)).append('|');b.append('\n');}}return b.toString();}
 public static void main(String[] a)throws Exception{try(PausedDb db=new PausedDb(a[0],a[1])){
  DatabaseCoordinator owner=HostProductR974.store(db).coordinatorR36();
  db.execSQL("DELETE FROM v57_schema_migration_journal_r926 WHERE step_id='R952_EXTENSION' OR (step_id LIKE 'R64_CP4_DDL_%' AND CAST(substr(step_id,13) AS INTEGER)>=54)");
  db.execSQL("DROP TRIGGER trg_r926_runtime_event_no_delete");db.execSQL("DROP TRIGGER r969_run_no_replace");
  db.execSQL("UPDATE v57_schema_domain_r926 SET state='INITIALIZING',fingerprint='intermediate' WHERE domain='RESEARCH'");
  Field rf=DatabaseCoordinator.class.getDeclaredField("r811ResearchSchemaReady");rf.setAccessible(true);((AtomicBoolean)rf.get(owner)).set(false);
  Field f=DatabaseCoordinator.class.getDeclaredField("waitingWriters");f.setAccessible(true);AtomicInteger waiting=(AtomicInteger)f.get(owner);
  Thread loser=new Thread(()->{System.out.println("LOSER="+owner.ensureR811ResearchSchemaOnce());});
  owner.runWriteOrThrowF01("TEST_HOLD",x->{loser.start();long waitUntil=System.nanoTime()+150_000_000L;while(waiting.get()<2&&System.nanoTime()<waitUntil)Thread.yield();if(waiting.get()<2)throw new AssertionError("no waiting competitor");db.pause=true;System.out.println("WINNER="+owner.ensureR811ResearchSchemaOnce());loser.join();String durable=rows(db,"SELECT state,cursor FROM v57_schema_domain_r926 WHERE domain='RESEARCH'");System.out.println("DURABLE_AFTER="+durable);boolean original=a.length>2&&"original".equals(a[2]);if(!durable.startsWith(original?"FAILED|":"INITIALIZING|"))throw new AssertionError("unexpected durable state");return true;});
  owner.ensureR811ResearchSchemaOnce();String baseline=rows(db,"SELECT step_id,status,attempt FROM v57_schema_migration_journal_r926 WHERE step_id IN('R811_RESEARCH','R815_RESEARCH')");System.out.println("BASELINE_AFTER="+baseline);boolean original=a.length>2&&"original".equals(a[2]);if(original){if(!baseline.contains("R811_RESEARCH|COMPLETED|2|")||!baseline.contains("R815_RESEARCH|REPAIR_REQUIRED|1|"))throw new AssertionError("original replay not reproduced");}else if(!baseline.contains("R811_RESEARCH|COMPLETED|1|")||!baseline.contains("R815_RESEARCH|COMPLETED|1|"))throw new AssertionError("unexpected baseline replay");System.out.println("DURABLE_RACE_PASS expected="+(original?"original-defect":"fixed"));
 }}
}
