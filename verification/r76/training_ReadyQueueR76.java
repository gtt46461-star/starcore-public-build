package com.ethk3.novaonepro;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import java.nio.file.*;
/** Exports actual product SQL; also exercises actual claim and generation CAS. */
public final class training_ReadyQueueR76 {
 public static void main(String[]a)throws Exception{
  if(a[0].equals("sql")){Files.writeString(Path.of(a[1]),ResearchTaskQueueR967.readyPredicate());Files.writeString(Path.of(a[1]+"-orphan"),ResearchTaskQueueR967.localOrphanQueryR76());return;}
  try(SQLiteDatabase d=new SQLiteDatabase(a[0],a[1])){
   DbStore s=HostProductR974.store(d);RealtimePressureGateR811.resetForTests();
   for(String kind:new String[]{"TRAIN_CANDIDATE","POST_TRAIN","MAINTENANCE"}){
    ResearchTaskQueueR967.Lease lease=s.coordinatorR36().runAtomicWriteOrThrowF01("R76_READY_OWNER",db->ResearchTaskQueueR967.claim(db,kind,System.currentTimeMillis()));
    if(lease==null)throw new AssertionError("EXPECTED_READY_"+kind);
    if(kind.equals("TRAIN_CANDIDATE")&&lease.candidate.startsWith("blocked-")&&lease.issue.equals("202609010002"))throw new AssertionError("TERMINAL_PREDECESSOR_IGNORED");
    boolean yielded=s.coordinatorR36().runAtomicWriteOrThrowF01("R76_READY_YIELD",db->ResearchTaskQueueR967.yield(db,lease,"","R76_TEST_YIELD",0));
    if(!yielded)throw new AssertionError("LEASE_RECEIPT_FAILED");
    System.out.println("CLAIM "+kind+" "+lease.candidate+" generation="+lease.generation);
   }
   s.coordinatorR36().runAtomicWriteOrThrowF01("R76_ORPHAN_INPUT",db->ResearchTaskQueueR967.enqueue(db,"TRAIN_CANDIDATE","r76-local-orphan","202610130483","one","input","authority"));
   ResearchTaskQueueR967.Lease first=s.coordinatorR36().runAtomicWriteOrThrowF01("R76_ORPHAN_CLAIM",db->ResearchTaskQueueR967.claimFor(db,"TRAIN_CANDIDATE","r76-local-orphan",System.currentTimeMillis()));
   if(first==null)throw new AssertionError("ORPHAN_FIRST_CLAIM");
   ResearchTaskQueueR967.Lease stolen=s.coordinatorR36().runAtomicWrite("R76_LIVE_PROTECTION",db->ResearchTaskQueueR967.claimFor(db,"TRAIN_CANDIDATE","r76-local-orphan",System.currentTimeMillis()),null);
   if(stolen!=null)throw new AssertionError("LIVE_LOCAL_LEASE_STOLEN");
   java.lang.reflect.Field live=ResearchTaskQueueR967.class.getDeclaredField("ACTIVE_CP4");live.setAccessible(true);
   ((java.util.Map<?,?>)live.get(null)).remove(first.id); // simulate worker token loss, not a wall-clock expiry
   ResearchTaskQueueR967.Lease recovered=s.coordinatorR36().runAtomicWriteOrThrowF01("R76_ORPHAN_RECOVERY",db->ResearchTaskQueueR967.claimFor(db,"TRAIN_CANDIDATE","r76-local-orphan",System.currentTimeMillis()));
   if(recovered==null||!recovered.id.equals(first.id)||recovered.generation!=first.generation+1)throw new AssertionError("ORPHAN_NOT_RECOVERED");
   Boolean valid=s.coordinatorR36().runAtomicWriteOrThrowF01("R76_ORPHAN_GENERATION_CAS",db->{
    if(ResearchTaskQueueR967.owns(db,first)||!ResearchTaskQueueR967.owns(db,recovered))return false;
    if(ResearchTaskQueueR967.yield(db,first,"","STALE_TEST",0))return false;
    return ResearchTaskQueueR967.yield(db,recovered,"","RECOVERED_TEST",0);
   });
   if(!Boolean.TRUE.equals(valid))throw new AssertionError("ORPHAN_STALE_TOKEN_REVIVED");
   System.out.println("LOCAL_ORPHAN live_protected=1 recovered_generation="+recovered.generation+" stale_token_rejected=1");
  }
 }
}
