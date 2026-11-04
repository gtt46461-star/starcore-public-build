package com.ethk3.novaonepro.strategyv2;
import android.database.sqlite.SQLiteDatabase;import com.ethk3.novaonepro.*;import java.nio.file.*;import java.sql.*;import java.util.*;import java.lang.reflect.Field;import java.util.concurrent.*;import java.util.concurrent.atomic.*;
/** Current real Runtime Health method + real scheduler; HOST state fixture, real monotonic clock. */
public final class EvolutionHealthCadenceR31Host {
 static void need(boolean b,String s){if(!b)throw new AssertionError(s);}
 public static void main(String[] args)throws Exception{
  Class.forName("org.sqlite.JDBC");ResearchWriteQueueR27.Batch source=EvolutionPublicationProtocolR31Host.originalBatch();Path path=Files.createTempFile("r31-evo-health-dispatch-",".db");
  try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+path)){
   SQLiteDatabase db=new SQLiteDatabase(c);EvolutionPublicationProtocolR31Host.schema(db);db.execSQL("PRAGMA journal_mode=WAL");
   try(R31OwnedRuntimeFixture producer=new R31OwnedRuntimeFixture(db,"r27-evolution");R31OwnedRuntimeFixture healthOwner=new R31OwnedRuntimeFixture(db,"r27-health")){
    EvolutionPublicationProtocolR31Host.stagingSchema(producer);EvolutionPublicationProtocolR31Host.existingEvidence(producer);
    for(int i=0;i<6;i++){final int packet=i;producer.owned(()->{EvolutionPublicationCapacityR31Host.publish(producer,source,packet);return null;});}
    long before=EvolutionPublicationProtocolR31Host.number(db,"SELECT count(*) FROM r31_evolution_op");
    RuntimeR27.HealthTask actual=healthOwner.runtime.new HealthTask();
    // Arrange a recently completed report marker from the real current clock. This is fixture
    // state, not a substituted clock, duration certificate, model, memory probe or scheduler.
    actual.lastElapsedR31=android.os.SystemClock.elapsedRealtime();
    Field stored=R31OwnedRuntimeFixture.class.getDeclaredField("store");stored.setAccessible(true);ResearchPoolR27 pool=new ResearchPoolR27((DbStore)stored.get(healthOwner),healthOwner.owner,1);
    List<Long> dispatches=Collections.synchronizedList(new ArrayList<>());AtomicReference<Throwable> failure=new AtomicReference<>();AtomicInteger positiveUnits=new AtomicInteger();
    pool.register(new ResearchPoolR27.Task(){public String id(){return actual.id();}public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker worker)throws Exception{dispatches.add(System.nanoTime());try{ResearchPoolR27.Outcome result=actual.step(worker);if(result.units>0)positiveUnits.incrementAndGet();return result;}catch(Exception|Error error){failure.compareAndSet(null,error);throw error;}}});
    long start=System.nanoTime();try{pool.start();new CountDownLatch(1).await(12,TimeUnit.SECONDS);}finally{pool.close();need(pool.awaitResourcesTerminatedR31(3000),"actual scheduler/continuation bodies must exit before reading final facts");}
    long elapsed=(System.nanoTime()-start)/1000000L;need(failure.get()==null,"current Runtime Health cleanup failed:"+failure.get());long after=EvolutionPublicationProtocolR31Host.number(db,"SELECT count(*) FROM r31_evolution_op");
    need(dispatches.size()>1&&after<before,"actual Health dispatches drain completed scratch even within report throttle");need(positiveUnits.get()==0,"private maintenance must not claim business research progress");
    List<Long> gaps=new ArrayList<>();for(int i=1;i<dispatches.size();i++)gaps.add((dispatches.get(i)-dispatches.get(i-1))/1000000L);
    need(before-after<=62L*dispatches.size(),"no dispatch expands the bounded deletion contract");
    System.out.println("R31_ACTUAL_HEALTH_DISPATCH_HOST_PASS "+JsonR27.write(Arrays.asList(elapsed,dispatches.size(),gaps,before,after,positiveUnits.get(),"ACTUAL_RUNTIME_62c46_HEALTH_AND_PRODUCTION_SCHEDULER_HOST_ONLY;RECENT_REPORT_MARKER_IS_FIXTURE_STATE;NO_FAKE_CLOCK;NOT_DEVICE_CADENCE_SLO")));
   }
  }finally{Files.deleteIfExists(path);Files.deleteIfExists(Path.of(path+"-wal"));Files.deleteIfExists(Path.of(path+"-shm"));}
 }
}
