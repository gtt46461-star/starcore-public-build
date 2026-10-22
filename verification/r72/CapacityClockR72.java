package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.IOException;
import java.lang.reflect.*;
import java.nio.file.*;
/** Synthetic lifecycle/clock fixtures against actual product owners. No quality or device claim. */
public final class CapacityClockR72 {
 static int checks;
 static final class MeasuredDb extends SQLiteDatabase {
  int ddl;
  MeasuredDb(String bridge,String file)throws Exception{super(bridge,file);}
  @Override public synchronized void execSQL(String sql){
   if(sql.startsWith("CREATE ")||sql.startsWith("ALTER "))ddl++;
   super.execSQL(sql);
  }
  @Override public synchronized void execSQL(String sql,Object[] args){
   if(sql.startsWith("CREATE ")||sql.startsWith("ALTER "))ddl++;
   super.execSQL(sql,args);
  }
 }
 static void ok(boolean b,String name){if(!b)throw new AssertionError(name);checks++;System.out.println("PASS "+name);}
 static void set(Object o,String n,Object v)throws Exception{Field f=CountdownClock.class.getDeclaredField(n);f.setAccessible(true);f.set(o,v);}
 static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
 public static void main(String[]a)throws Exception {
  RealtimePressureGateR811.resetForTests();
  try(MeasuredDb db=new MeasuredDb(a[0],a[1])){
   for(int i=1;i<=200;i++)SettlementProgressR72.draw(db,i);
   AlgorithmGenomeR908 parent=null;
   db.beginTransaction();try {
    for(int i=0;i<64;i++){
     AlgorithmGenomeR908 g=AlgorithmGenomeR908.seed(73000+i,1,"R72_CAPACITY_FIXTURE_"+i);
     AlgorithmInventionLabR908.registerDirected(db,g,"202610120001","202610120002","R72_FIXTURE","SYNTHETIC");
     ResearchLifecycleR932.enroll(db,"algorithm:"+g.genomeId,g.structuralHash,"202610120001","C");
     if(i==0)parent=g;
    }
    String id=parent.genomeId,key="algorithm:"+id;
    ok(ResearchPopulationR966.active(db)==64,"fixture reaches actual capacity");
    SettlementProgressR72.algorithmFreeze(db,parent,201);
    ok(id.equals(ResearchPopulationR966.replacementForDevelopment(db,id)),"own zero-N parent with pending proxy can hand off capacity");
    db.execSQL("UPDATE "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" SET state_updates=1 WHERE genome_id=?",new Object[]{id});
    ok(ResearchPopulationR966.replacementForDevelopment(db,id)==null,"trained parent is protected");
    db.execSQL("UPDATE "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" SET state_updates=0,future_n=1 WHERE genome_id=?",new Object[]{id});
    ok(ResearchPopulationR966.replacementForDevelopment(db,id)==null,"Future-observed parent is protected");
    db.execSQL("UPDATE "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" SET future_n=0 WHERE genome_id=?",new Object[]{id});
    db.execSQL("INSERT INTO "+CandidateCalibrationR932.OBS+"(study_hash,target_issue,candidate_key,structure_hash,bundle_hash,selection_hash,raw_score,position,n1,n2,n3,prediction_hash,frozen_at)VALUES('test-study','202610120002',?,'s','b','p',.3,1,1,2,3,'f',1)",new Object[]{key});
    ok(ResearchPopulationR966.replacementForDevelopment(db,id)==null,"formal calibration participant is protected even before label");
    // Roll this isolated fixture transaction back; no production evidence is manufactured.
   }finally{db.endTransaction();}
   DbStore store=HostProductR974.store(db);
   store.coordinatorR36().runAtomicWriteOrThrowF01("R72_SCHEMA_TEST",d->{ResearchPlatformStoreR929.createSchema(d);return true;});
   db.ddl=0;
   store.coordinatorR36().runAtomicWriteOrThrowF01("R72_SCHEMA_TEST",d->{ResearchPlatformStoreR929.createSchema(d);return true;});
   ok(db.ddl==0,"committed platform schema performs zero repeated DDL");
   db.execSQL("DROP INDEX idx_r929_cert_lookup");db.ddl=0;
   store.coordinatorR36().runAtomicWriteOrThrowF01("R72_SCHEMA_TEST",d->{ResearchPlatformStoreR929.createSchema(d);return true;});
   ok(db.ddl>0&&n(db,"SELECT COUNT(*) FROM sqlite_master WHERE type='index' AND name='idx_r929_cert_lookup'")==1,"schema version drift reruns required DDL");
   db.beginTransaction();try{
    for(int i=0;i<64;i++){
     AlgorithmGenomeR908 g=AlgorithmGenomeR908.seed(73000+i,1,"R72_CAPACITY_FIXTURE_"+i);
     AlgorithmInventionLabR908.registerDirected(db,g,"202610120001","202610120002","R72_FIXTURE","SYNTHETIC");
     ResearchLifecycleR932.enroll(db,"algorithm:"+g.genomeId,g.structuralHash,"202610120001","C");
     if(i==0)parent=g;
    }
    SettlementProgressR72.algorithmFreeze(db,parent,201);
    String victim=ResearchPopulationR966.replacementForDevelopment(db,parent.genomeId);
    AlgorithmGenomeR908 child=AlgorithmGenomeR908.seed(74001,2,"R72_DISTINCT_CHILD_FIXTURE");
    AlgorithmInventionLabR908.registerDirected(db,child,"202610120001","202610120002","R72_FIXTURE","SYNTHETIC");
    ResearchPopulationR966.committed(db,victim,child.genomeId,"202610120002");
    ok(ResearchPopulationR966.active(db)==64,"atomic child handoff never exceeds committed capacity");
    ok(n(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW+" WHERE genome_id='"+parent.genomeId+"' AND hit_label=-1")==1,"retirement preserves pending frozen label");
    ok(n(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME)==65,"immutable parent program remains available");
    ok(n(db,"SELECT COUNT(*) FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE)==0,"capacity handoff creates no certificate");
    db.setTransactionSuccessful();
   }finally{db.endTransaction();}
  }
  android.os.SystemClock.setForTest(1000000,0);
  CountdownClock clock=new CountdownClock();clock.start();
  CountdownProjectionR971.Anchor anchor=CountdownProjectionR971.Anchor.capture("202610120002",1,TimeSourceR811.clockSessionCP12(),1060000,1000000,1000000);
  set(clock,"clockAnchorCP12",anchor);
  ok(CountdownClock.liveAnchorR72()==anchor,"resumed UI reads exact sole-clock anchor immediately");
  clock.markFetched();set(clock,"lastFetchAt",Long.MAX_VALUE);
  ok(!clock.shouldFetchNow(),"wall clock regression cannot trigger duplicate instant poll");
  android.os.SystemClock.setForTest(1100000,0);
  ok(clock.shouldFetchNow(),"wall clock regression cannot stall monotonic polling");
  clock.resetFetchTimer();ok(clock.shouldFetchNow(),"explicit reset admits immediate poll");
  clock.stop();ok(CountdownClock.liveAnchorR72()==null,"stopped service cannot expose stale anchor");
  Method classify=ReportExportServiceCP4.class.getDeclaredMethod("classify",Throwable.class);classify.setAccessible(true);
  ok("STALE_PAGE".equals(classify.invoke(null,new IOException("STALE_PAGE"))),"stale retention page keeps retry classification");
  ok("ARCHIVE_CANCELLED".equals(classify.invoke(null,new IOException("ARCHIVE_CANCELLED"))),"archive failure keeps actionable reason");
  Files.writeString(Path.of(a[2]),"{\"status\":\"PASS\",\"checks\":"+checks+",\"scope\":\"ACTUAL_PRODUCT_SYNTHETIC_HOST_NOT_DEVICE\"}\n");
 }
}
