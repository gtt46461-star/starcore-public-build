package com.ethk3.novaonepro.strategyv2;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;
/** Bounded per-PID memo of actually decoded immutable original-input proofs, never of source validity. */
final class PinSnapshotProofR31 {
 private static final int DATABASES=8,ENTRIES=128;private static final long BYTES=512L<<10;
 private static final WeakHashMap<SQLiteDatabase,LinkedHashMap<String,Boolean>> CACHE=new WeakHashMap<>();
 private static MemoryBudgetR31.Lease lease;
 private static final Runnable TRIM=()->{synchronized(CACHE){CACHE.clear();if(lease!=null){lease.close();lease=null;}}};
 private PinSnapshotProofR31(){}
 private static String key(SQLiteDatabase db,String run,String contract){long version=SchemaR27.liveSourceSchemaVersion(db);return version<0?null:run+'|'+contract+'|'+version;}
 static boolean known(SQLiteDatabase db,String run,String contract){String key=key(db,run,contract);synchronized(CACHE){LinkedHashMap<String,Boolean> proofs=CACHE.get(db);return key!=null&&proofs!=null&&proofs.containsKey(key);}}
 static void prepare(){MemoryBudgetR31 memory=MemoryBudgetR31.processBudget();memory.registerPressureReclaimerR31("PIN_SNAPSHOT_PROOF_METADATA",TRIM);synchronized(CACHE){if(lease!=null)return;}MemoryBudgetR31.Admission admission=memory.tryAcquire("pin-snapshot-verified-metadata",MemoryBudgetR31.Kind.RESEARCH,BYTES,()->Thread.currentThread().isInterrupted());if(!admission.acquired())return;synchronized(CACHE){if(lease==null)lease=admission.lease;else admission.lease.close();}}
 static void verified(SQLiteDatabase db,String run,String contract){if(!db.inTransaction())prepare();String key=key(db,run,contract);if(key==null)return;
  synchronized(CACHE){if(lease==null)return;LinkedHashMap<String,Boolean> proofs=CACHE.get(db);if(proofs==null){if(CACHE.size()>=DATABASES){Iterator<SQLiteDatabase> first=CACHE.keySet().iterator();first.next();first.remove();}proofs=new LinkedHashMap<>(16,.75f,true);CACHE.put(db,proofs);}proofs.put(key,Boolean.TRUE);while(proofs.size()>ENTRIES){Iterator<String> old=proofs.keySet().iterator();old.next();old.remove();}}
 }
}
