package com.ethk3.novaonepro;
import java.lang.reflect.*;import android.database.sqlite.*;import android.database.Cursor;
public final class SchemaCaptureCP4 {
 public static void main(String[] a)throws Exception {
 SQLiteDatabase db=new SQLiteDatabase(a[0],a[1]);
 try { DbStore store=HostProductR974.store(db); DbHelper h=(DbHelper)HostProductR974.U.allocateInstance(DbHelper.class);
 h.onConfigure(db);db.beginTransaction();try{h.onCreate(db);db.setTransactionSuccessful();}finally{db.endTransaction();}db.setVersion(36);
 DbHelper.ensureR811CriticalSchema(db);DbHelper.ensureR906CriticalSchema(db);RuntimeEventLedgerR926.ensureSchema(db);
 boolean ready=false;
 // R64 deliberately commits one bounded migration unit per pulse; await verified
 // readiness instead of assuming the former eight large transactions suffice.
 for(int i=0;i<512&&!ready;i++){ready=ResearchSchemaCoordinatorR926.ensureOneStep(store.coordinatorR36());System.out.println("RESEARCH="+ResearchSchemaCoordinatorR926.summary());}
 if(!ready)throw new AssertionError("RESEARCH_NOT_READY_AFTER_512_PULSES");
 System.out.println("CRITICAL=");SchemaDomainCoordinatorR926.verifyObjects(db,SchemaDomainCoordinatorR926.criticalContract());
 System.out.println("RESEARCH=");SchemaDomainCoordinatorR926.verifyObjects(db,SchemaDomainCoordinatorR926.researchContract());
 System.out.println("LEGACY=");SchemaDomainCoordinatorR926.verifyObjects(db,SchemaDomainCoordinatorR926.legacyContract());
 try(Cursor c=db.rawQuery("SELECT type,count(*) FROM sqlite_master GROUP BY type",null)){while(c.moveToNext())System.out.println(c.getString(0)+"="+c.getLong(1));}
 }finally{db.shutdown();}
 }
}
