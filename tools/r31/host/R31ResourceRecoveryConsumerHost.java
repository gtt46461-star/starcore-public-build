package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import android.os.SystemClock;
import com.ethk3.novaonepro.DbStore;
import com.ethk3.novaonepro.SettlementExecutionGateR819;
import java.io.File;
import java.lang.ref.Reference;
import java.lang.ref.WeakReference;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import sun.misc.Unsafe;

/** Actual R26 consumer method and MemoryBudget producer, native SQLite/host platform only. */
public final class R31ResourceRecoveryConsumerHost {
    private static int assertions;
    private static void need(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    private static void set(Object target,String name,Object value)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);f.set(target,value);}
    private static Object get(Object target,String name)throws Exception{Field f=target.getClass().getDeclaredField(name);f.setAccessible(true);return f.get(target);}
    @SuppressWarnings("unchecked") private static Map<String,Object> observation(MemoryBudgetR31 memory){return (Map<String,Object>)memory.snapshot().get("optionalHeapRecovery");}
    private static WeakReference<byte[]> garbage(MemoryBudgetR31 memory){
        int size=(int)(Runtime.getRuntime().maxMemory()*.64);
        MemoryBudgetR31.Admission owner=memory.tryAcquire("HOST_R26_CONTROLLED_GARBAGE",MemoryBudgetR31.Kind.CHECKPOINT,size,()->false);
        need(owner.acquired(),"Pressure fixture has real write-before-allocation admission");
        byte[] array=new byte[size];array[0]=31;array[size-1]=12;owner.lease.allocatedPrimitiveArrayR31(array);memory.sample();
        WeakReference<byte[]> weak=new WeakReference<>(array);owner.lease.close();Reference.reachabilityFence(array);return weak;
    }
    @SuppressWarnings("unchecked") public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");need(Runtime.getRuntime().maxMemory()==512L*1024*1024,"Unchanged512MiB max heap");
        try(Connection connection=DriverManager.getConnection("jdbc:sqlite:"+args[0])){
            SQLiteDatabase db=new SQLiteDatabase(connection);
            DbStore store=new DbStore(null,db,new android.app.Application(new File(args[0])));
            Field unsafe=Unsafe.class.getDeclaredField("theUnsafe");unsafe.setAccessible(true);
            RuntimeCoordinatorR26 runtime=(RuntimeCoordinatorR26)((Unsafe)unsafe.get(null)).allocateInstance(RuntimeCoordinatorR26.class);
            MemoryBudgetR31 memory=MemoryBudgetR31.processBudget();
            set(runtime,"owner","HOST_REAL_R26_RESOURCE_CONTROLLER");set(runtime,"store",store);set(runtime,"memoryBudgetR31",memory);
            set(runtime,"memoryObservationR31",Collections.emptyMap());
            MemoryBudgetR31.Admission active=memory.tryAcquire("HOST_ACTIVE_UNALLOCATED_INPUT",MemoryBudgetR31.Kind.CHECKPOINT,4096,()->false);need(active.acquired(),"Unrelated active owner admitted");
            WeakReference<byte[]> weak=garbage(memory);
            MemoryBudgetR31.Admission denied=memory.tryAcquire("HOST_ACTUAL_OPTIONAL_REQUEST",MemoryBudgetR31.Kind.RESEARCH,400000,()->false);
            need(denied.reason==MemoryBudgetR31.WaitReason.DECLARED_CAPACITY,"Real heap denies optional request");
            RuntimeCoordinatorR26.MemoryControlStateR31 control=runtime.sampleMemoryControlR31(SystemClock.elapsedRealtime());
            need(!control.optionalOverTwoMinutes,"Fresh actual denial is not overdue");
            need(runtime.recoverOverdueMemoryR31(control)==control,"Actual R26 consumer takes no early collector action");
            long started=System.nanoTime();new CountDownLatch(1).await(120200,TimeUnit.MILLISECONDS);
            long elapsed=(System.nanoTime()-started)/1000000L;need(elapsed>=120000,"Real elapsed wait without clock substitution");
            control=runtime.sampleMemoryControlR31(SystemClock.elapsedRealtime());need(control.optionalOverTwoMinutes,"Actual R26 observer sees aged optional denial");need(weak.get()!=null,"Unreachable garbage has not been declared freed");
            db.beginTransaction();try{control=runtime.recoverOverdueMemoryR31(control);need(((Number)observation(memory).get("attemptCount")).longValue()==0,"Actual R26 transaction predicate prevents collector");}finally{db.endTransaction();}
            need(SettlementExecutionGateR819.tryEnter(),"Actual settlement fence acquired");
            try{control=runtime.recoverOverdueMemoryR31(control);need(((Number)observation(memory).get("attemptCount")).longValue()==0,"Actual settlement window prevents collector");}finally{SettlementExecutionGateR819.exit();}
            RuntimeCoordinatorR26.bindBackgroundClaim(new RuntimeCoordinatorR26.Claim("HOST_CURRENT_TASK","HOST_OWNER",1,0));
            try{control=runtime.recoverOverdueMemoryR31(control);need(((Number)observation(memory).get("attemptCount")).longValue()==0,"Actual task claim prevents collector");}finally{RuntimeCoordinatorR26.unbindBackgroundClaimR31();}
            set(runtime,"cancelled",true);control=runtime.recoverOverdueMemoryR31(control);need(((Number)observation(memory).get("attemptCount")).longValue()==0,"Stopped owner cannot collect");set(runtime,"cancelled",false);
            control=runtime.recoverOverdueMemoryR31(control);
            Map<String,Object> decision=(Map<String,Object>)get(runtime,"memoryRecoveryDecisionR31"),attempt=(Map<String,Object>)get(runtime,"memoryRecoveryAttemptR31");
            need(Boolean.TRUE.equals(decision.get("attempted"))&&"ADMISSION_AVAILABLE".equals(decision.get("outcome")),"Actual R26 consumer invokes producer and remeasures successful admission");
            need(((Number)observation(memory).get("attemptCount")).longValue()==1,"Exactly one actual collector request");
            need(control.optionalAdmissionRecovered&&!control.optionalLimited,"Actual R26 controller reports measured admission recovery");
            need(!Boolean.TRUE.equals(decision.get("businessProgress")),"Collector is never model progress");need(attempt.equals(decision),"Actual attempt preserved separately from decision");
            need(!active.lease.isClosed(),"Active unallocated promise was not released");need(weak.get()==null,"Previously unreachable payload actually collected");
            MemoryBudgetR31.Admission resumed=memory.tryAcquire("HOST_POST_RECOVERY_REQUEST",MemoryBudgetR31.Kind.RESEARCH,400000,()->false);need(resumed.acquired(),"Real optional admission succeeds through source API");resumed.lease.close();
            Map<String,Object> memoryObservation=(Map<String,Object>)get(runtime,"memoryObservationR31");need(memoryObservation.get("runtimeMemoryRecovery") instanceof Map,"Report observation carries actual producer-consumer receipt");
            Map<String,Object> result=new LinkedHashMap<>();result.put("scope","ACTUAL_R26_CONSUMER_AND_MEMORY_PRODUCER_NATIVE_SQLITE_HOST; NOT_ANDROID_OR_FULL_RUNTIME_LOOP");result.put("actualElapsedWaitMs",elapsed);result.put("assertions",assertions);result.put("decision",decision);result.put("runtimeMemoryControl",control.asMap());result.put("guardedWindows",java.util.Arrays.asList("CURRENT_SQLITE_TRANSACTION","SETTLEMENT_GATE","BOUND_TASK_CLAIM","CANCELLED_OWNER"));result.put("clockSubstitution",false);result.put("probeSubstitution",false);result.put("mathematicalWorkloadCollector",false);result.put("physicalDevice","NOT_RUN");result.put("naturalQuality","NOT_PROVEN");
            System.out.println("R31_RESOURCE_RECOVERY_CONSUMER_JSON|"+JsonR27.write(result));
            System.out.println("R31_RESOURCE_RECOVERY_CONSUMER_HOST_PASS assertions="+assertions);active.lease.close();
        }
    }
}
