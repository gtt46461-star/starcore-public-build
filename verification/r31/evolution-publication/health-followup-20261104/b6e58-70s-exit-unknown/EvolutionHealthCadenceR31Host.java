package com.ethk3.novaonepro.strategyv2;
import android.database.sqlite.SQLiteDatabase;import com.ethk3.novaonepro.DatabaseCoordinator;import com.ethk3.novaonepro.*;import java.nio.file.*;import java.sql.*;import java.util.*;import java.lang.reflect.Field;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
/** Current real Runtime Health method + real scheduler; HOST state fixture, real monotonic clock. */
public final class EvolutionHealthCadenceR31Host {
 static void dump(ResearchPoolR27 pool, SQLiteDatabase db,String when){
  System.out.println("HEALTH_POOL_DIAG "+when+" "+JsonR27.write(pool.shutdownSnapshotR31()));
  System.out.println("HEALTH_MEMORY_DIAG "+when+" "+JsonR27.write(MemoryBudgetR31.processBudget().snapshot()));
  try(android.database.Cursor c=db.rawQuery("SELECT task_id,status,owner,fence,lease_until,wait_kind,wait_detail FROM r26_task WHERE task_id='r27-health'",null)){while(c.moveToNext()){java.util.List<Object> v=new java.util.ArrayList<>();for(int i=0;i<7;i++)v.add(c.getString(i));System.out.println("HEALTH_TASK_DIAG "+when+" "+JsonR27.write(v));}}
  for(java.util.Map.Entry<Thread,StackTraceElement[]> e:Thread.getAllStackTraces().entrySet()){Thread t=e.getKey();if(!t.getName().startsWith("starcore"))continue;System.out.println("HEALTH_THREAD_DIAG "+when+" "+t.getName()+" state="+t.getState()+" alive="+t.isAlive()+" interrupted="+t.isInterrupted());for(StackTraceElement frame:e.getValue())System.out.println(" at "+frame);}
 }
 static void need(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args)throws Exception{
  Class.forName("org.sqlite.JDBC");try{System.out.println("HOST_SQLITE_CLOSE_API "+SQLiteDatabase.class.getMethod("close"));}catch(NoSuchMethodException missing){System.out.println("HOST_SQLITE_CLOSE_API_MISSING "+missing);}ResearchWriteQueueR27.Batch source=EvolutionPublicationProtocolR31Host.originalBatch();Path path=Files.createTempFile("r31-evo-health-dispatch-",".db");
  try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+path)){
   SQLiteDatabase db=new SQLiteDatabase(c);EvolutionPublicationProtocolR31Host.schema(db);db.execSQL("PRAGMA journal_mode=WAL");
   try(R31OwnedRuntimeFixture producer=new R31OwnedRuntimeFixture(db,"r27-evolution");R31OwnedRuntimeFixture healthOwner=new R31OwnedRuntimeFixture(db,"r27-health")){
    EvolutionPublicationProtocolR31Host.stagingSchema(producer);EvolutionPublicationProtocolR31Host.existingEvidence(producer);
    for(int i=0;i<6;i++){final int packet=i;producer.owned(()->{EvolutionPublicationCapacityR31Host.publish(producer,source,packet);return null;});}
    long before=EvolutionPublicationProtocolR31Host.number(db,"SELECT count(*) FROM r31_evolution_op");
    RuntimeR27.HealthTask actual=healthOwner.runtime.new HealthTask();
    // Arrange a recently completed report marker from the real current clock. This is fixture
    // state, not a substituted clock, duration certificate, model, memory probe or scheduler.
    // Initial report state remains zero; exercise two real HEALTH writes on their original throttle.
    long faultRowsBefore=EvolutionPublicationProtocolR31Host.number(db,"SELECT count(*) FROM r31_runtime_fault");
    producer.owned(()->{producer.coordinator.runAtomicWriteOrThrowF01(DatabaseCoordinator.WriteContext.of(DatabaseCoordinator.WriteClass.RESEARCH_DATA,"R31_HOST_REAL_HOLD_FOR_HEALTH_DRAIN"),tx->{Thread.sleep(230);return 0;});return null;});
    System.out.println("HEALTH_REAL_HOLD_QUEUED "+JsonR27.write(WriterHoldFaultsR31.snapshot()));
    Field stored=R31OwnedRuntimeFixture.class.getDeclaredField("store");stored.setAccessible(true);ResearchPoolR27 pool=new ResearchPoolR27((DbStore)stored.get(healthOwner),healthOwner.owner,1);
    List<Long> dispatches=Collections.synchronizedList(new ArrayList<>());List<Long> realHealthWrites=Collections.synchronizedList(new ArrayList<>());AtomicReference<Throwable> failure=new AtomicReference<>();AtomicInteger positiveUnits=new AtomicInteger();
    pool.register(new ResearchPoolR27.Task(){public String id(){return actual.id();}public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker worker)throws Exception{dispatches.add(System.nanoTime());try{ResearchPoolR27.Outcome result=actual.step(worker);if(result.units>0){positiveUnits.incrementAndGet();realHealthWrites.add(System.nanoTime());System.out.println("HEALTH_ACTUAL_WRITE "+JsonR27.write(Arrays.asList(result.units,result.cursor,System.currentTimeMillis())));}return result;}catch(Exception|Error error){failure.compareAndSet(null,error);error.printStackTrace(System.out);throw error;}}});
    long start=System.nanoTime();try{pool.start();new CountDownLatch(1).await(70,TimeUnit.SECONDS);}finally{dump(pool,db,"BEFORE_CANCEL");pool.close();boolean stopped=pool.awaitResourcesTerminatedR31(3000);dump(pool,db,"AFTER_3S");System.out.println("HEALTH_WRAPPER_DIAG dispatches="+dispatches.size()+" failure="+failure.get()+" units="+positiveUnits.get());need(stopped,"actual scheduler/continuation bodies must exit before reading final facts");}
    long elapsed=(System.nanoTime()-start)/1000000L;need(failure.get()==null,"current Runtime Health cleanup failed:"+failure.get());long after=EvolutionPublicationProtocolR31Host.number(db,"SELECT count(*) FROM r31_evolution_op");
    need(dispatches.size()>1&&after<before,"actual Health dispatches drain completed scratch even within report throttle");need(positiveUnits.get()>=2,"at least two actual HEALTH report writes across original60s throttle");
    need(EvolutionPublicationProtocolR31Host.number(db,"SELECT count(*) FROM r31_commit_receipt WHERE operation='R27_HEALTH'")>=2,"two actual durable HEALTH receipts");
    need(EvolutionPublicationProtocolR31Host.number(db,"SELECT count(*) FROM r31_runtime_fault WHERE operation='R31_HOST_REAL_HOLD_FOR_HEALTH_DRAIN'")==1,"the real over200ms observation is actually delivered by unmodified drainOne");
    need(((Number)WriterHoldFaultsR31.snapshot().get("persistedFaultReceipts")).longValue()>=1,"actual separate diagnostic consumer delivery");
    System.out.println("HEALTH_DURABLE_RECEIPTS "+JsonR27.write(Arrays.asList(realHealthWrites,WriterHoldFaultsR31.snapshot(),faultRowsBefore)));
    List<Long> gaps=new ArrayList<>();for(int i=1;i<dispatches.size();i++)gaps.add((dispatches.get(i)-dispatches.get(i-1))/1000000L);
    need(before-after<=62L*dispatches.size(),"no dispatch expands the bounded deletion contract");
    System.out.println("R31_ACTUAL_HEALTH_DISPATCH_HOST_PASS "+JsonR27.write(Arrays.asList(elapsed,dispatches.size(),gaps,before,after,positiveUnits.get(),"ACTUAL_RUNTIME_b6e58f4c_HEALTH_AND_PRODUCTION_SCHEDULER_HOST_ONLY;70_REAL_SECONDS;NO_FAKE_CLOCK;NOT_DEVICE_CADENCE_SLO")));
   }
  }finally{Files.deleteIfExists(path);Files.deleteIfExists(Path.of(path+"-wal"));Files.deleteIfExists(Path.of(path+"-shm"));}
 }
}
