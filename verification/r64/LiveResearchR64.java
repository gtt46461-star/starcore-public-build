package com.ethk3.novaonepro;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;import android.content.Context;import android.content.res.AssetManager;import com.ethk3.novaonepro.r952.*;import java.io.*;import java.nio.file.*;
/** Public production consumers on the supplied past data only; no invented Future or qualification. */
public final class LiveResearchR64 {
 static long count(SQLiteDatabase db,String table){try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+table,null)){c.moveToFirst();return c.getLong(0);}}
 public static void main(String[]a)throws Exception{
  File path=new File(a[2]);Files.copy(Path.of(a[1]),path.toPath());SQLiteDatabase db=new SQLiteDatabase(a[0],path.getPath());
  try{DbStore store=HostProductR974.store(db);HostProductR974.set(store,"researchInputRootCP11",new File(a[5]));
   HostProductR974.set(store,"researchAppCP12",new Context(new AssetManager(new File(a[3]),new File(a[4]),0,AtomCatalogPinnedCP12.BYTES)));
   int calls=0;while(!ModernSeedFactoryCP12.completeCP12(db)&&calls++<2500){String result=MolecularToolWorldR947.importSeedBatchCP4(store,System.nanoTime()+2_000_000_000L);if(calls%100==0)System.out.println("IMPORT|calls="+calls+"|"+result);}
   if(!AtomCatalogImportCP12.completeCP12(db)||!ModernSeedFactoryCP12.completeCP12(db))throw new AssertionError("PUBLIC_CATALOG_CONSUMER_STALLED");
   System.out.println("CATALOGS|atoms="+count(db,"research_atom_spec_v2")+"|molecules="+count(db,"research_molecule_seed_v2"));
   for(int i=0;i<15;i++)System.out.println("FAMILY|"+MolecularToolWorldR947.importSeedBatchCP4(store,System.nanoTime()+2_000_000_000L));
   int progressed=0;long deadline=System.nanoTime()+120_000_000_000L;
   for(int i=0;i<4000&&System.nanoTime()<deadline;i++){
    if(ResearchTrainingHookRegistryR952.drainTrainingSlotCP11(store,System.nanoTime()+2_000_000_000L))progressed++;
    if(i%100==0)System.out.println("TRAINING|calls="+i+"|progressed="+progressed);
    try(Cursor c=db.rawQuery("SELECT 1 FROM v57_research_task_r967 WHERE kind IN ('TRAIN_CANDIDATE','FREEZE_CANDIDATE') AND status NOT IN ('COMPLETED','FAILED_TERMINAL','EXPIRED') LIMIT 1",null)){if(!c.moveToFirst())break;}
   }
   for(String t:new String[]{"r952_candidate","research_training_feature_row_cp11","r967_frozen_evaluation_v2","research_certificate_r929","trades"})System.out.println("COUNT|"+t+"="+count(db,t));
   try(Cursor c=db.rawQuery("SELECT kind,status,last_error,COUNT(*) FROM v57_research_task_r967 GROUP BY kind,status,last_error",null)){while(c.moveToNext())System.out.println("TASK|"+c.getString(0)+"|"+c.getString(1)+"|"+c.getString(2)+"|"+c.getLong(3));}
   try(Cursor c=db.rawQuery("SELECT SUM(future_n),SUM(may_create_real) FROM r952_candidate",null)){c.moveToFirst();if(c.getLong(0)!=0||c.getLong(1)!=0)throw new AssertionError("FALSE_NATURAL_AUTHORITY");}
   try(Cursor c=db.rawQuery("PRAGMA integrity_check",null)){if(!c.moveToFirst()||!"ok".equals(c.getString(0)))throw new AssertionError("INTEGRITY");}
   System.out.println("R64_LIVE_PAST_CONSUMER_PASS|scope=USER_PAST_DATA_HOST_PRODUCT_CONSUMERS_NOT_PHONE_NOT_NATURAL");
  }finally{db.close();}
 }
}
