package com.ethk3.novaonepro;
import android.database.Cursor;import com.ethk3.novaonepro.r952.*;import java.io.*;import java.util.*;
/** Five real failed publications retain at most four recomputable pages. */
public final class training_CacheBoundR77 {
 public static void main(String[]a)throws Exception{
  try(training_PreparedPageR77.FailingDb d=new training_PreparedPageR77.FailingDb(a[0],a[1])){
   DbStore s=HostProductR974.store(d);HostProductR974.set(s,"researchAppCP12",new android.content.Context(new File(a[2]),new File(a[3])));HostProductR974.set(s,"researchInputRootCP11",new File(a[2],"research-input-v2"));RealtimePressureGateR811.resetForTests();
   List<String> candidates=new ArrayList<>();try(Cursor c=d.rawQuery("SELECT candidate_id FROM v57_research_task_r967 WHERE kind='TRAIN_CANDIDATE' ORDER BY created_at LIMIT 5",null)){while(c.moveToNext())candidates.add(c.getString(0));}
   if(candidates.size()!=5)throw new AssertionError("FIVE_TASK_FIXTURE_REQUIRED");long before=RuntimeMemoryGuardR811.algorithmAtomMatrixBudget().used();
   for(int i=0;i<candidates.size();i++){
    ResearchTaskQueueR967.Lease l=training_PreparedPageR77.claim(s,candidates.get(i));if(l==null)throw new AssertionError("TASK_NOT_CLAIMED");d.fail=true;
    try{DevelopmentProductionCP12.prepareBatch(s,l,System.nanoTime()+3_000_000_000L);throw new AssertionError("WRITE_NOT_FAILED");}catch(DevelopmentProductionCP12.Wait expected){if(!expected.reason.equals("TRAIN_FEATURE_COMMIT_FAILED"))throw expected;}
    long used=RuntimeMemoryGuardR811.algorithmAtomMatrixBudget().used()-before;
    if(used!=Math.min(4,i+1)*128*1024L)throw new AssertionError("RETAINED_MEMORY_BOUND:"+used);
    if(training_PreparedPageR77.count(d)!=0)throw new AssertionError("ROLLED_BACK_ROWS_PUBLISHED");
   }
   System.out.println("PASS real_failed_pages=5 retained_pages=4 retained_bytes=524288 rollback_rows=0");
  }
 }
}
