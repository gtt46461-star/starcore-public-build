package com.ethk3.novaonepro;
import com.ethk3.novaonepro.r952.*;import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;
import java.io.*;import java.nio.file.*;
/** Actual catalog/import/family/training owners; copied report draws, isolated host DB.
 * No fixture count is applied to the phone, a Future certificate, or REAL. */
public final class FamilyProgressR72 {
 static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
 static DbStore owner(SQLiteDatabase db,File root,File assets)throws Exception{
  DbStore s=HostProductR974.store(db);android.content.Context context=new android.content.Context(root,assets);
  HostProductR974.set(s,"researchAppCP12",context);HostProductR974.set(s,"researchInputRootCP11",new File(root,"research-input-v2"));return s;
 }
 public static void main(String[]a)throws Exception {
  try(SQLiteDatabase db=new SQLiteDatabase(a[0],a[1])){
   File root=new File(a[2]),assets=new File(a[3]);root.mkdirs();DbStore s=owner(db,root,assets);RealtimePressureGateR811.resetForTests();
   for(int i=0;!ModernSeedFactoryCP12.completeCP12(db)&&i<2000;i++){
    String status=ModernSeedFactoryCP12.maintain(s,System.nanoTime()+775_000_000L);
    if(i%40==0)System.out.println("CATALOG "+i+' '+status);
   }
   if(!ModernSeedFactoryCP12.completeCP12(db))throw new AssertionError("catalog did not finish");
   System.out.println("CATALOG_COMPLETE atoms="+n(db,"SELECT COUNT(*) FROM research_atom_spec_v2")+" seeds="+n(db,"SELECT COUNT(*) FROM research_molecule_seed_v2"));
   for(int i=0;i<60&&n(db,"SELECT COUNT(*) FROM research_training_protocol_v2")==0;i++) {
    s=owner(db,root,assets);ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(850);
    try{System.out.println("FAMILY "+i+' '+ModernFamilyFactoryCP12.maintain(s,System.nanoTime()+775_000_000L));}
    catch(ResearchWorkBudgetR927.Yield yielded){System.out.println("FAMILY_YIELD "+yielded.getMessage());}
    finally{ResearchWorkBudgetR927.leave(scope);}
   }
   if(n(db,"SELECT COUNT(*) FROM research_training_protocol_v2")<1)throw new AssertionError("no registered modern training protocol");
   int turns=0;long progress=0;
   for(;turns<500&&n(db,"SELECT COUNT(*) FROM research_development_receipt_v2")==0;turns++){
    s=owner(db,root,assets);ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(850);
    try{ResearchTrainingHookRegistryR952.drainTrainingSlotCP11(s,System.nanoTime()+775_000_000L);}
    catch(ResearchWorkBudgetR927.Yield yielded){}
    finally{ResearchWorkBudgetR927.leave(scope);}
    long rows=n(db,"SELECT COUNT(*) FROM research_training_feature_row_cp11");if(turns%10==0||rows>progress){System.out.println("TRAIN "+turns+" featureRows="+rows+" receipts="+n(db,"SELECT COUNT(*) FROM research_development_receipt_v2"));progress=rows;}
    if(n(db,"SELECT COUNT(*) FROM v57_research_task_r967 WHERE kind='TRAIN_CANDIDATE' AND status='FAILED_TERMINAL'")>0)throw new AssertionError("training terminal failure");
    Thread.sleep(100);
   }
   long receipts=n(db,"SELECT COUNT(*) FROM research_development_receipt_v2");if(receipts<1)throw new AssertionError("no real development training receipt after bounded retries");
   if(n(db,"SELECT SUM(future_n+may_create_real) FROM r952_candidate")!=0||n(db,"SELECT COUNT(*) FROM research_certificate_r929")!=0||n(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_TRADES)!=0)throw new AssertionError("development contaminated Future/REAL");
   Files.writeString(Path.of(a[4]),"{\"status\":\"PASS\",\"training_turns\":"+turns+",\"feature_rows\":"+progress+",\"development_receipts\":"+receipts+",\"scope\":\"ACTUAL_PRODUCT_REPORT_HISTORY_HOST_RESTARTED_OWNER_NO_PHONE_OR_FUTURE\"}\n");
  }
 }
}
