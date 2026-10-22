package com.ethk3.novaonepro;
import com.ethk3.novaonepro.r952.ResearchDirectorR952;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;
import java.lang.reflect.Method;import java.nio.file.*;
/** Actual durable Director admission with a short parent scope, then a fresh scope.
 * Reflection selects the old method on an unmodified R74 build for before/after reproduction. */
public final class training_DirectorBudgetR75 {
 static int checks;static void check(boolean v,String m){if(!v)throw new AssertionError(m);checks++;System.out.println("PASS "+m);}
 static String head(SQLiteDatabase d){try(Cursor c=d.rawQuery("SELECT state_value FROM app_state WHERE state_key='r975_director_priority'",null)){return c.moveToFirst()?c.getString(0):"TRAIN";}}
 static String trainHead(SQLiteDatabase d){try(Cursor c=d.rawQuery("SELECT state_value FROM app_state WHERE state_key='cp11_training_subslot'",null)){return c.moveToFirst()?c.getString(0):"0";}}
 static String trainAdmit(DbStore s,long end)throws Exception{
  Class<?> k=com.ethk3.novaonepro.r952.ResearchTrainingHookRegistryR952.class;Method m;
  try{m=k.getDeclaredMethod("advanceTrainingSlotCP11",DbStore.class,long.class);m.setAccessible(true);return (String)m.invoke(null,s,end);}
  catch(NoSuchMethodException old){m=k.getDeclaredMethod("advanceTrainingSlotCP11",DbStore.class);m.setAccessible(true);return (String)m.invoke(null,s);}
 }
 static String admit(DbStore s,long end)throws Exception{
  Method m;try{m=ResearchDirectorR952.class.getDeclaredMethod("advanceDirectorSlotCP12",DbStore.class,long.class);m.setAccessible(true);return (String)m.invoke(null,s,end);}
  catch(NoSuchMethodException old){m=ResearchDirectorR952.class.getDeclaredMethod("advanceDirectorSlotCP12",DbStore.class);m.setAccessible(true);return (String)m.invoke(null,s);}
 }
 public static void main(String[]a)throws Exception{
  try(SQLiteDatabase d=new SQLiteDatabase(a[0],a[1])){
   RealtimePressureGateR811.resetForTests();DbStore s=HostProductR974.store(d);
   String[] slots={"TRAIN","POST","MATRIX","RESEARCH","EVOLVE","IMPORT"};
   // Every owner reaches the tail of a callback, including across reconstructed stores.
   for(int i=0;i<slots.length;i++){
    final String slot=slots[i];d.execSQL("INSERT OR REPLACE INTO app_state VALUES('r975_director_priority',?,0)",new Object[]{slot});
    for(int attempt=0;attempt<3;attempt++){
     s=HostProductR974.store(d);ResearchWorkBudgetR927.Scope parent=ResearchWorkBudgetR927.enter(90);
     try{check(admit(s,System.nanoTime()+850_000_000L)==null,"SHORT_PARENT_NOT_ADMITTED_"+slot+'_'+attempt);check(slot.equals(head(d)),"UNSTARTED_CURSOR_PRESERVED_"+slot+'_'+attempt);}
     finally{ResearchWorkBudgetR927.leave(parent);}
    }
    ResearchWorkBudgetR927.Scope full=ResearchWorkBudgetR927.enter(850);
    try{check(slot.equals(admit(s,System.nanoTime()+850_000_000L)),"FRESH_SLICE_ADMITS_"+slot);check(slots[(i+1)%slots.length].equals(head(d)),"ADMITTED_OWNER_ADVANCES_FOR_FAIRNESS_"+slot);}
    finally{ResearchWorkBudgetR927.leave(full);}
   }
   d.execSQL("INSERT OR REPLACE INTO app_state VALUES('r975_director_priority','IMPORT',0)");
   ResearchWorkBudgetR927.Scope p=ResearchWorkBudgetR927.enter(850);
   try{check(admit(s,System.nanoTime()+200_000_000L)==null,"SHORT_LOCAL_TAIL_NOT_ADMITTED_IMPORT");check("IMPORT".equals(head(d)),"IMPORT_GETS_NEXT_CALLBACK_INSTEAD_OF_REPEATED_TAIL");}
   finally{ResearchWorkBudgetR927.leave(p);}
   d.execSQL("INSERT OR REPLACE INTO app_state VALUES('r975_director_priority','RESEARCH',0)");
   ResearchWorkBudgetR927.Scope shortResearch=ResearchWorkBudgetR927.enter(200);
   try{check(admit(s,System.nanoTime()+850_000_000L)==null,"RESEARCH_200MS_PARENT_NOT_ADMITTED");check("RESEARCH".equals(head(d)),"RESEARCH_DIAGNOSTIC_OWNER_RETAINS_NEXT_FRESH_SLICE");}
   finally{ResearchWorkBudgetR927.leave(shortResearch);}
   ResearchWorkBudgetR927.Scope freshResearch=ResearchWorkBudgetR927.enter(850);
   try{check("RESEARCH".equals(admit(HostProductR974.store(d),System.nanoTime()+850_000_000L)),"RESEARCH_RESUMES_WITH_SUFFICIENT_DIAGNOSTIC_BUDGET");check("EVOLVE".equals(head(d)),"ADMITTED_RESEARCH_ROTATES_TO_EVOLVE");}
   finally{ResearchWorkBudgetR927.leave(freshResearch);}
   String[] kinds={"LEGACY_OUTBOX","TRAIN_CANDIDATE","FREEZE_CANDIDATE"};
   for(int i=0;i<3;i++){
    d.execSQL("INSERT OR REPLACE INTO app_state VALUES('cp11_training_subslot',?,0)",new Object[]{Integer.toString(i)});
    ResearchWorkBudgetR927.Scope shortScope=ResearchWorkBudgetR927.enter(90);
    try{check(trainAdmit(s,System.nanoTime()+850_000_000L)==null,"SHORT_PARENT_SUBSLOT_NOT_CONSUMED_"+kinds[i]);check(Integer.toString(i).equals(trainHead(d)),"SUBSLOT_HEAD_PRESERVED_"+kinds[i]);}
    finally{ResearchWorkBudgetR927.leave(shortScope);}
    ResearchWorkBudgetR927.Scope full=ResearchWorkBudgetR927.enter(850);
    try{check(kinds[i].equals(trainAdmit(HostProductR974.store(d),System.nanoTime()+775_000_000L)),"SUBSLOT_RESUMES_AFTER_RESTART_"+kinds[i]);check(Integer.toString((i+1)%3).equals(trainHead(d)),"ADMITTED_SUBSLOT_ROTATES_"+kinds[i]);}
    finally{ResearchWorkBudgetR927.leave(full);}
   }
   Files.writeString(Path.of(a[2]),"{\"status\":\"PASS\",\"checks\":"+checks+",\"scope\":\"REAL_DIRECTOR_SQLITE_ADMISSION_WITH_SHORT_PARENT_SCOPE_NO_DEVICE_OR_QUALITY_CLAIM\"}\n");
  }
 }
}
