package com.ethk3.novaonepro.strategyv2;
import android.database.sqlite.SQLiteDatabase;import java.nio.file.*;import java.sql.*;import java.util.*;
/** Real native owner growth/backpressure. Manual maintenance dispatch is not a measured service scheduler cadence. */
public final class EvolutionPublicationCapacityR31Host {
 static int checks;static void need(boolean b,String s){checks++;if(!b)throw new AssertionError(s);}
 static void publish(R31OwnedRuntimeFixture owner,ResearchWriteQueueR27.Batch source,int n)throws Exception{
  try(EvolutionPublicationStoreR31.Prepared p=EvolutionPublicationStoreR31.prepare(owner.db,RuntimeR27.LOOP,RuntimeCoordinatorR26.currentClaimR30(),"de9a30834025c18ce291c241a722abe437717539",EvolutionPublicationStoreR31.CursorState.capture(owner.db,RuntimeR27.LOOP),source,0)){
   int stage=0;for(ResearchWriteQueueR27.Batch b:p.stagingBatches())EvolutionPublicationProtocolR31Host.submit(owner,"EVOLUTION_CAPACITY_STAGE_"+n+"_"+stage++,b);p.verifyPrepared(owner.db);EvolutionPublicationProtocolR31Host.submit(owner,"EVOLUTION_STEP_CAPACITY_"+n,p.publicationBatch());
  }
 }
 public static void main(String[] args)throws Exception{
  Class.forName("org.sqlite.JDBC");ResearchWriteQueueR27.Batch source=EvolutionPublicationProtocolR31Host.originalBatch();Path path=Files.createTempFile("r31-evo-capacity-",".db");
  try(Connection c=DriverManager.getConnection("jdbc:sqlite:"+path)){
   SQLiteDatabase db=new SQLiteDatabase(c);EvolutionPublicationProtocolR31Host.schema(db);db.execSQL("PRAGMA journal_mode=WAL");try(R31OwnedRuntimeFixture owner=new R31OwnedRuntimeFixture(db,"r27-evolution")){
    EvolutionPublicationProtocolR31Host.stagingSchema(owner);EvolutionPublicationProtocolR31Host.existingEvidence(owner);int accepted=0;Map<String,Object> debt=null;long start=System.nanoTime();
    // Every packet retains the whole real128-candidate source. These backend replays never add independent quality samples.
    for(int n=0;n<40;n++){final int index=n;boolean blocked=false;try{owner.owned(()->{publish(owner,source,index);return null;});accepted++;}catch(EvolutionPublicationStoreR31.Capacity cap){FailureDispositionR31.Decision d=FailureDispositionR31.classify(cap);need(d.disposition==FailureDispositionR31.Disposition.DEPENDENCY&&!d.consumesFaultBudget(),"capacity debt does not quarantine valid research");need(cap.getMessage().contains("WAITING_PRIVATE_SCRATCH_CLEANUP"),"this actual workload reaches scratch ceiling before checkpoint debt");blocked=true;}if(blocked){debt=EvolutionPublicationStoreR31.capacitySnapshot(db);break;}}
    need(accepted>1&&accepted<40,"write-before-admission actually stops unbounded new private growth");need(debt!=null,"real debt was observed, not a fabricated resource sample");need(((Number)debt.get("scratchRowsAndReservations")).longValue()<=EvolutionPublicationStoreR31.MAX_PRIVATE_SCRATCH_ROWS,"row reservation ceiling is enforced");
    String oldPointer=EvolutionPublicationFaultR31Host.text(db,"SELECT snapshot FROM r27_evolution_cursor");int rejectedIndex=accepted;boolean retried=false;try{owner.owned(()->{publish(owner,source,rejectedIndex);return null;});}catch(EvolutionPublicationStoreR31.Capacity expected){retried=true;}need(retried&&oldPointer.equals(EvolutionPublicationFaultR31Host.text(db,"SELECT snapshot FROM r27_evolution_cursor")),"refused packet preserves committed model/cursor");
    int[] dispatches={0};boolean resumed=false;for(int n=0;n<20&&!resumed;n++){
     owner.owned(()->{ResearchWriteQueueR27.Batch cleanup=EvolutionPublicationStoreR31.cleanupSlice(db,RuntimeR27.LOOP,RuntimeCoordinatorR26.currentClaimR30());need(cleanup!=null&&cleanup.ops.size()<=64,"one actual maintenance dispatch is bounded");long before=EvolutionPublicationProtocolR31Host.number(db,"SELECT count(*) FROM r31_evolution_op");EvolutionPublicationProtocolR31Host.submit(owner,"EVOLUTION_PRIVATE_MANUAL_DISPATCH_"+dispatches[0]++,cleanup);long after=EvolutionPublicationProtocolR31Host.number(db,"SELECT count(*) FROM r31_evolution_op");need(before-after<=62,"max cleanup throughput per dispatch is62 private rows");return null;});
     try{owner.owned(()->{publish(owner,source,rejectedIndex);return null;});resumed=true;}catch(EvolutionPublicationStoreR31.Capacity notYet){need(!notYet.getMessage().isEmpty(),"capacity reason remains explicit");}
    }
    need(resumed,"fair maintenance creates real admission and the same full source resumes");need(dispatches[0]>1,"no fake all-at-once cleanup");need(EvolutionPublicationProtocolR31Host.number(db,"SELECT global_index FROM r27_eprocess WHERE policy_hash=?",EvolutionPublicationProtocolR31Host.ENGINEERING)==99,"budget index is preserved under repeated pressure");
    System.out.println("R31_EVOLUTION_CAPACITY_HOST_PASS "+JsonR27.write(Arrays.asList(checks,accepted,dispatches[0],(System.nanoTime()-start)/1000000L,debt,EvolutionPublicationStoreR31.capacitySnapshot(db),"MANUAL_DISPATCH_REAL_SQL_NOT_ACTUAL_HEALTH_SCHEDULER_CADENCE_NOT_NATURAL_SAMPLE_GROWTH")));
   }
  }finally{Files.deleteIfExists(path);Files.deleteIfExists(Path.of(path+"-wal"));Files.deleteIfExists(Path.of(path+"-shm"));}
 }
}
