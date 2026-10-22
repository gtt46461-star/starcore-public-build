package com.ethk3.novaonepro;
import com.ethk3.novaonepro.r952.*;import android.database.*;import android.database.sqlite.*;import java.io.*;
/** Recompute a phone-reported partial prefix without importing unavailable feature blobs. */
public final class training_PrefixParityR77 {
 public static void main(String[]a)throws Exception{try(SQLiteDatabase d=new SQLiteDatabase(a[0],a[1])){
  DbStore s=HostProductR974.store(d);HostProductR974.set(s,"researchAppCP12",new android.content.Context(new File(a[2]),new File(a[3])));HostProductR974.set(s,"researchInputRootCP11",new File(a[2],"research-input-v2"));RealtimePressureGateR811.resetForTests();
  String candidate;try(Cursor c=d.rawQuery("SELECT candidate_id FROM v57_research_task_r967 WHERE task_id=?",new String[]{a[4]})){if(!c.moveToFirst())throw new AssertionError("TASK_MISSING");candidate=c.getString(0);}
  ResearchTaskQueueR967.Lease lease=s.coordinatorForResearchR952().runAtomicWrite("TEST_PREFIX_CLAIM",db->ResearchTaskQueueR967.claimFor(db,"TRAIN_CANDIDATE",candidate,System.currentTimeMillis()),null);if(lease==null)throw new AssertionError("NOT_CLAIMED");
  DevelopmentProductionCP12.prepareBatch(s,lease,System.nanoTime()+3_000_000_000L);
  String hash=V51ChecksumEngine.sha256("CP12_FEATURE_PREFIX|"+lease.id+'|'+lease.sequence+'|'+lease.inputHash);int count=0;
  try(Cursor c=d.rawQuery("SELECT feature_hash FROM research_training_feature_row_cp11 WHERE task_id=? AND row_index<? ORDER BY row_index",new String[]{lease.id,a[5]})){while(c.moveToNext()){hash=V51ChecksumEngine.sha256(hash+'|'+c.getString(0));count++;}}
  if(count!=Integer.parseInt(a[5]))throw new AssertionError("PREFIX_TOO_SHORT:"+count);
  System.out.println("PREFIX task="+lease.id+" rows="+count+" hash="+hash+" trainingRule="+TrainingCodeIdentityR964.SHA256);
 }}
}
