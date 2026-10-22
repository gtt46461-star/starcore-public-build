package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;
/** Actual current product schema on isolated real SQLite; no runtime evidence. */
public final class SchemaR72 {
 public static void main(String[] a)throws Exception {
  try(SQLiteDatabase db=new SQLiteDatabase(a[0],a[1])) {
   DbStore store=HostProductR974.store(db);
   DbHelper helper=(DbHelper)HostProductR974.U.allocateInstance(DbHelper.class);
   helper.onConfigure(db);
   db.beginTransaction();try{helper.onCreate(db);db.setTransactionSuccessful();}finally{db.endTransaction();}
   DbHelper.ensureR811CriticalSchema(db);DbHelper.ensureR906CriticalSchema(db);RuntimeEventLedgerR926.ensureSchema(db);
   boolean ready=false;
   for(int i=0;i<256;i++){if(ResearchSchemaCoordinatorR926.ensureOneStep(store.coordinatorR36())){ready=true;break;}}
   if(!ready)throw new IllegalStateException(ResearchSchemaCoordinatorR926.summary());
   SchemaDomainCoordinatorR926.verifyObjects(db,SchemaDomainCoordinatorR926.criticalContract());
   SchemaDomainCoordinatorR926.verifyObjects(db,SchemaDomainCoordinatorR926.researchContract());
   System.out.println("R72_ACTUAL_SCHEMA_PASS "+ResearchSchemaCoordinatorR926.summary());
  }
 }
}
