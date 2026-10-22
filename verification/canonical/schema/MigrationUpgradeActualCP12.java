package com.ethk3.novaonepro;
import android.database.sqlite.*;import android.database.Cursor;
public final class MigrationUpgradeActualCP12 {
 public static void main(String[] a)throws Exception {
 SQLiteDatabase db=new SQLiteDatabase(a[0],a[1]);
 try { DbStore store=HostProductR974.store(db); DbHelper h=(DbHelper)HostProductR974.U.allocateInstance(DbHelper.class);
 h.onConfigure(db);if(a.length>2&&a[2].equals("upgrade35")){db.beginTransaction();try{h.onUpgrade(db,35,36);db.setVersion(36);db.setTransactionSuccessful();}finally{db.endTransaction();}}
 DbHelper.ensureR811CriticalSchema(db);DbHelper.ensureR906CriticalSchema(db);RuntimeEventLedgerR926.ensureSchema(db);
 for(int i=0;i<8;i++)if(ResearchSchemaCoordinatorR926.ensureOneStep(store.coordinatorR36()))break;
 SchemaDomainCoordinatorR926.verifyObjects(db,SchemaDomainCoordinatorR926.criticalContract());SchemaDomainCoordinatorR926.verifyObjects(db,SchemaDomainCoordinatorR926.researchContract());SchemaDomainCoordinatorR926.verifyObjects(db,SchemaDomainCoordinatorR926.legacyContract());
 System.out.println("PASS "+ResearchSchemaCoordinatorR926.summary());
 }finally{db.shutdown();}
 }
}
