package com.ethk3.novaonepro;
import com.ethk3.novaonepro.r952.*;import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import java.io.File;
public final class training_ReportPrepareR75 {
 public static void main(String[] a)throws Exception{
  try(SQLiteDatabase db=new SQLiteDatabase(a[0],a[1])){
   DbStore s=HostProductR974.store(db);File root=new File(a[2]);root.mkdirs();HostProductR974.set(s,"researchAppCP12",new android.content.Context(root,new File(a[3])));HostProductR974.set(s,"researchInputRootCP11",new File(root,"research-input-v2"));
   RealtimePressureGateR811.resetForTests();
   try(Cursor c=db.rawQuery("SELECT ast,candidate_id,action_id,family FROM research_family_creation_cp12 ORDER BY family",null)){
    while(c.moveToNext()) {ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(850);long start=System.nanoTime();
     try(DevelopmentProductionCP12.PreparedProtocol p=DevelopmentProductionCP12.prepare(s,CandidateAstR952.parse(c.getString(0)),c.getString(1),c.getString(2),"202610130324")) {System.out.println(c.getString(3)+" PREPARED rows="+p.rows+" trainN="+p.trainN+" ms="+(System.nanoTime()-start)/1_000_000);}
     catch(Throwable e){System.out.println(c.getString(3)+" FAILED ms="+(System.nanoTime()-start)/1_000_000+" "+e);e.printStackTrace(System.out);}
     finally{ResearchWorkBudgetR927.leave(scope);}
    }
   }
  }
 }
}
