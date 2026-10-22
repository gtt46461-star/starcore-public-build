package com.ethk3.novaonepro.r952;
import android.database.Cursor;import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
/** Fresh JVM phases exercise actual owner startup, old READY/cache, rollback and idempotence. */
public final class MatrixReadyMigrationR79 {
 static int checks;static void ok(boolean x,String why){checks++;if(!x)throw new AssertionError(why);}
 static long count(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){if(!c.moveToFirst())throw new AssertionError("count");return c.getLong(0);}}
 static long index(SQLiteDatabase db){return count(db,"SELECT COUNT(*) FROM sqlite_master WHERE name='idx_r79_matrix_ready_partition_order'");}
 static long receipt(SQLiteDatabase db){return count(db,"SELECT COUNT(*) FROM v57_schema_migration_journal_r926 WHERE step_id='R79_MATRIX_READY_ORDER_V1' AND status='COMPLETED'");}
 static String oldJournal(SQLiteDatabase db){StringBuilder s=new StringBuilder();try(Cursor c=db.rawQuery("SELECT * FROM v57_schema_migration_journal_r926 WHERE step_id<>'R79_MATRIX_READY_ORDER_V1' ORDER BY domain,revision,step_id",null)){while(c.moveToNext()){for(int i=0;i<c.getColumnCount();i++)s.append(c.getString(i)).append('|');s.append('\n');}}return s.toString();}
 static DatabaseCoordinator owner(SQLiteDatabase db)throws Exception{DbStore s=HostProductR974.store(db);java.lang.reflect.Field f=DbStore.class.getDeclaredField("coordinator");f.setAccessible(true);return (DatabaseCoordinator)f.get(s);}
 public static void main(String[] args)throws Exception{try(SQLiteDatabase db=new SQLiteDatabase(args[0],args[1])){
  String mode=args[2];boolean outer=mode.startsWith("outer");SchemaDomainCoordinatorR926.Contract contract=SchemaDomainCoordinatorR926.researchContract();
  if(mode.endsWith("rollback")){
   ok(index(db)==0&&receipt(db)==0,"prior-release additive step missing");ok(count(db,"SELECT COUNT(*) FROM v57_schema_migration_journal_r926 WHERE step_id='CP12_MOLECULE_SEEK_DDL_V1' AND status='COMPLETED'")==1,"old final step ready");
   if(outer){SchemaDomainCoordinatorR926.verifyAndStamp(db,contract);ok(SchemaDomainCoordinatorR926.matches(db,contract),"actual valid R78 READY stamp");ok(SchemaDomainCoordinatorR926.stepComplete(db,contract,"R952_EXTENSION"),"old outer extension completed");}
   String old=oldJournal(db);db.beginTransaction();TransactionSchemaCacheR941.Scope scope=TransactionSchemaCacheR941.enter(db);try{
    TransactionSchemaCacheR941.markCommitted(db,"R64_INCREMENTAL_RESEARCH_SCHEMA");ok(TransactionSchemaCacheR941.readyCommitted(db,"R64_INCREMENTAL_RESEARCH_SCHEMA"),"simulate old ready cache");
    if(outer)ok(ResearchSchemaCoordinatorR926.ensureOneStep(owner(db)),"actual owner reaches verified READY in one additive callback");
    else ok(!ResearchMigrationRegistryR952.advanceR64(db),"new step executes despite old ready cache");
    ok(index(db)==1&&receipt(db)==1,"DDL and receipt share transaction");ok(old.equals(oldJournal(db)),"all old journal rows preserved byte-for-byte");
    if(outer)ok(SchemaDomainCoordinatorR926.matches(db,contract),"new actual fingerprint stamped");
   }finally{db.endTransaction();TransactionSchemaCacheR941.leave(scope);}
   ok(index(db)==0&&receipt(db)==0,"rollback undoes DDL and receipt");ok(old.equals(oldJournal(db)),"rollback preserves old journal");
   if(outer)ok(SchemaDomainCoordinatorR926.matches(db,contract),"rollback restores valid old domain fingerprint");
  }else{
   ok(index(db)==0&&receipt(db)==0,"fresh JVM observes prior rollback");String old=oldJournal(db);long start=System.nanoTime();
   if(outer){ok(SchemaDomainCoordinatorR926.matches(db,contract),"fresh JVM sees valid R78 READY");ok(ResearchSchemaCoordinatorR926.ensureOneStep(owner(db)),"actual owner upgrade in one callback");}
   else ResearchMigrationRegistryR952.ensure(db);
   System.out.println("additive_upgrade_ms="+(System.nanoTime()-start)/1000000.0);
   ok(index(db)==1&&receipt(db)==1,"fresh JVM installs additive step");ok(old.equals(oldJournal(db)),"no old journal replay");
   if(outer)ok(ResearchSchemaCoordinatorR926.ensureOneStep(owner(db)),"repeated actual owner READY");else ResearchMigrationRegistryR952.ensure(db);
   ok(index(db)==1&&receipt(db)==1,"repeated ensure idempotent");ok(ResearchMigrationRegistryR952.readyOrderR79(db),"actual index contract verified");
  }
  System.out.println("R79_MATRIX_READY_MIGRATION_PASS mode="+mode+" assertions="+checks);
 }}
}
