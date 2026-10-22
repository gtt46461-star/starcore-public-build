package com.ethk3.novaonepro;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;
import android.content.Context;import android.content.res.AssetManager;
import com.ethk3.novaonepro.r952.*;
import java.io.*;import java.nio.file.*;import java.util.*;import java.lang.reflect.*;
/** Current two-phase public importer; real asset/SQLite, host file transport only. */
public final class ModernSeedImportContractCP12 {
 static int n;static void ok(boolean b){n++;if(!b)throw new AssertionError("seed import "+n);}
 static long count(SQLiteDatabase db,String t){try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+t,null)){c.moveToFirst();return c.getLong(0);}}
 static Object invoke(Method m,Object...a)throws Exception{try{return m.invoke(null,a);}catch(InvocationTargetException e){if(e.getCause() instanceof Exception)throw (Exception)e.getCause();throw e;}}
 static String progress(DbStore store)throws Exception{store.setState("cp12_import_subslot","1");boolean advanced=HistoricalGenomeImportR967.maintainCP12(store,System.nanoTime()+30_000_000_000L);String result=store.getState("cp12_import_last_result","");ok(advanced);return result;}
 public static void main(String[] a)throws Exception {
  SQLiteDatabase db=new SQLiteDatabase(a[0],a[1]);try{
   DbStore store=HostProductR974.store(db);Field app=DbStore.class.getDeclaredField("researchAppCP12");app.setAccessible(true);app.set(store,new Context(new AssetManager(new File("app/src/main/assets/cp4_atom_registry.jsonl"),new File(a[2]),0,AtomCatalogPinnedCP12.BYTES)));
   ok(AtomCatalogImportCP12.completeCP12(db)&&count(db,"research_atom_spec_v2")==25350);ok(count(db,"research_molecule_seed_v2")==0);ok(!ModernSeedFactoryCP12.completeCP12(db));
   ok(progress(store).equals("SEEDS_CHECKPOINT_INITIALIZED"));
   Method read=ModernSeedFactoryCP12.class.getDeclaredMethod("readCheckpoint",SQLiteDatabase.class);read.setAccessible(true);Object before=invoke(read,db);
   Field scan=before.getClass().getDeclaredField("scan");scan.setAccessible(true);MoleculeSeedPageCP12.Cursor zero=(MoleculeSeedPageCP12.Cursor)scan.get(before);
   MoleculeSeedPageCP12.Page first;try(InputStream in=new FileInputStream(a[2])){first=MoleculeSeedPageCP12.prepare(in,zero,4200,Long.MAX_VALUE,System::nanoTime);}ok(first.rawRows.size()==25);
   Method commit=ModernSeedFactoryCP12.class.getDeclaredMethod("scanCommit",SQLiteDatabase.class,before.getClass(),MoleculeSeedPageCP12.Page.class);commit.setAccessible(true);
   boolean rejected=false;try{invoke(commit,db,before,first);}catch(IllegalArgumentException expected){rejected=true;}ok(rejected);
   db.beginTransaction();try{ok(((String)invoke(commit,db,before,first)).startsWith("SEEDS_RAW_SCAN:"));}finally{db.endTransaction();}
   try(Cursor c=db.rawQuery("SELECT scan_row FROM research_molecule_import_checkpoint_cp12",null)){ok(c.moveToFirst()&&c.getInt(0)==0);}ok(count(db,"research_molecule_seed_v2")==0);
   db.beginTransaction();try{invoke(commit,db,before,first);db.setTransactionSuccessful();}finally{db.endTransaction();}
   db.beginTransaction();try{ok(((String)invoke(commit,db,before,first)).startsWith("SEEDS_SCAN_REPLAY:"));db.setTransactionSuccessful();}finally{db.endTransaction();}
   ok(progress(store).startsWith("SEEDS_RAW_SCAN:"));
   rejected=false;db.beginTransaction();try{try{invoke(commit,db,before,first);}catch(IllegalArgumentException expected){rejected=expected.getMessage().equals("SEED_SCAN_CAS_STALE");}}finally{db.endTransaction();}ok(rejected);
   int calls=0;while(!ModernSeedFactoryCP12.completeCP12(db)){
    String result=progress(store);ok(result.startsWith("SEEDS_RAW_SCAN:")||result.startsWith("SEEDS_TYPED_MATERIALIZED:"));ok(++calls<1000);
    try(Cursor c=db.rawQuery("SELECT state,scan_row,scan_prefix,material_row FROM research_molecule_import_checkpoint_cp12",null)){ok(c.moveToFirst());if(c.getString(0).equals("SCANNING"))ok(count(db,"research_molecule_seed_v2")==0);else ok(c.getInt(1)==4200&&c.getString(2).equals(ModernSeedFactoryCP12.ASSET_SHA));}
   }
   ok(count(db,"research_molecule_seed_v2")==4200);ok(count(db,"r952_candidate")==0&&count(db,DbHelper.TABLE_TRADES)==0);
   try(Cursor c=db.rawQuery("SELECT scan_row,scan_offset,scan_prefix,material_row,material_offset,material_prefix,state FROM research_molecule_import_checkpoint_cp12",null)){ok(c.moveToFirst()&&c.getInt(0)==4200&&c.getLong(1)==MoleculeSeedPageCP12.BYTES&&c.getString(2).equals(ModernSeedFactoryCP12.ASSET_SHA)&&c.getInt(3)==4200&&c.getLong(4)==MoleculeSeedPageCP12.BYTES&&c.getString(5).equals(ModernSeedFactoryCP12.ASSET_SHA)&&c.getString(6).equals("COMPLETE"));}
   try(Cursor c=db.rawQuery("PRAGMA foreign_key_check",null)){ok(!c.moveToFirst());}
   rejected=false;try{MoleculeSeedPageCP12.prepare(new ByteArrayInputStream("tampered\n".getBytes()),MoleculeSeedPageCP12.Cursor.zero(),4200,Long.MAX_VALUE,System::nanoTime);}catch(IOException|IllegalStateException expected){rejected=true;}ok(rejected);
   Files.writeString(Path.of(a[3]),"{\"status\":\"PASS\",\"assertions\":"+n+",\"compiled_feature_seeds\":4200,\"public_import_calls\":"+calls+",\"scope\":\"Current public Molecular IMPORT via HistoricalGenome fixed subslot; complete raw SHA before any typed writes, complete typed SHA, all4200 strict AST/compiler constructors, real SQLite rollback/replay/staleCAS, progress acknowledgement; host asset transport, no Android Director dispatch or natural performance.\"}\n");
  }finally{db.shutdown();}
 }
}
