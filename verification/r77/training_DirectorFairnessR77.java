package com.ethk3.novaonepro;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.ResearchDirectorR952;import java.lang.reflect.*;import java.util.*;
/** Actual Director admission SQL, durable restart cursor and real pressure gate. */
public final class training_DirectorFairnessR77 {
 static int checks;static void require(boolean b,String s){if(!b)throw new AssertionError(s);checks++;}
 static String value(SQLiteDatabase d,String k){try(Cursor c=d.rawQuery("SELECT state_value FROM app_state WHERE state_key=?",new String[]{k})){return c.moveToFirst()?c.getString(0):"";}}
 static String admission(DbStore s,long ms)throws Exception{Method m=ResearchDirectorR952.class.getDeclaredMethod("advanceDirectorSlotCP12",DbStore.class,long.class);m.setAccessible(true);try{return (String)m.invoke(null,s,System.nanoTime()+ms*1000000L);}catch(InvocationTargetException e){if(e.getCause() instanceof RuntimeException)throw (RuntimeException)e.getCause();throw e;}}
 public static void main(String[] a)throws Exception{
  try(SQLiteDatabase d=new SQLiteDatabase(a[0],a[1])){
   DbStore s=HostProductR974.store(d);RealtimePressureGateR811.resetForTests();
   d.execSQL("CREATE TABLE IF NOT EXISTS app_state(state_key TEXT PRIMARY KEY,state_value TEXT,updated_at INTEGER)");
   if(a[2].equals("first")||a[2].equals("low_budget")){
    d.beginTransaction();try{ResearchTaskQueueR967.schema(d);ResearchTaskQueueR967.enqueue(d,"TRAIN_CANDIDATE","FAIRNESS","202610130617","1","INPUT","AUTHORITY");d.setTransactionSuccessful();}finally{d.endTransaction();}
    String old=value(d,"r975_director_priority");require(admission(s,0)==null,"no_unusable_tail");require(value(d,"r975_director_priority").equals(old),"tail_does_not_rotate");
    ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(1000);RealtimePressureGateR811.beginAnalysis();try{try{admission(s,1000);throw new AssertionError("P0_DID_NOT_YIELD");}catch(ResearchWorkBudgetR927.Yield expected){checks++;}}finally{RealtimePressureGateR811.endAnalysis();ResearchWorkBudgetR927.leave(scope);}
    require(value(d,"r975_director_priority").equals(old),"P0_does_not_rotate");
   }
   if(a[2].equals("low_budget")){
    d.execSQL("INSERT OR REPLACE INTO app_state VALUES('r975_director_priority','POST',1)");
    d.execSQL("INSERT OR REPLACE INTO app_state VALUES('r77_director_training_turn','1',1)");
    require("POST".equals(admission(s,240)),"reserved_train_cannot_block_cheap_POST");
    require(value(d,"r77_director_training_turn").equals("1"),"short_deadline_preserves_reservation");
    ResearchWorkBudgetR927.Scope shortScope=ResearchWorkBudgetR927.enter(240);
    try{require("MATRIX".equals(admission(s,2000)),"remaining_budget_cannot_block_cheap_MATRIX");}
    finally{ResearchWorkBudgetR927.leave(shortScope);}
    require(value(d,"r77_director_training_turn").equals("1"),"short_scope_preserves_reservation");
    require(admission(s,100)==null,"below_cheap_minimum_still_yields");
    require(value(d,"r975_director_priority").equals("RESEARCH"),"unusable_tail_preserves_normal_cursor");
    System.out.println("PASS checks="+checks+" mode=low_budget cheap=POST,MATRIX reserved=1");return;
   }
   List<String> sequence=new ArrayList<>();Map<String,Integer> counts=new TreeMap<>();
   for(int i=0;i<12;i++){String slot=admission(HostProductR974.store(d),2000);require(slot!=null,"admitted");sequence.add(slot);counts.put(slot,counts.getOrDefault(slot,0)+1);}
   require(counts.getOrDefault("TRAIN",0)>=7,"training_reserved_share:"+counts);
   for(String k:new String[]{"POST","MATRIX","RESEARCH","EVOLVE","IMPORT"})require(counts.getOrDefault(k,0)==1,"normal_owner_service:"+k+counts);
   require(value(d,"r975_director_priority").equals("TRAIN"),"normal_wheel_persisted");require(value(d,"r77_director_training_turn").equals("1"),"reserve_persisted");
   if(a[2].equals("restart")){
    d.beginTransaction();try{ResearchTaskQueueR967.Lease l=ResearchTaskQueueR967.claim(d,"TRAIN_CANDIDATE",System.currentTimeMillis());ResearchTaskQueueR967.complete(d,l,"NO_MODEL_FAIRNESS_FIXTURE");d.setTransactionSuccessful();}finally{d.endTransaction();}
    sequence.clear();for(int i=0;i<6;i++)sequence.add(admission(s,2000));require(sequence.equals(Arrays.asList("TRAIN","POST","MATRIX","RESEARCH","EVOLVE","IMPORT")),"empty_queue_original_wheel:"+sequence);
    require(ResearchDirectorR952.pulse(s,1)==0,"public_entry_tiny_slice_bounded");
   }
   System.out.println("PASS checks="+checks+" mode="+a[2]+" counts="+counts+" sequence="+sequence);
  }
 }
}
