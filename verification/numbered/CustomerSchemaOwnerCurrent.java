package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.ResearchMigrationRegistryR952;

/** Current registered schema Owners on a protected customer snapshot; no dump/ALTER route. */
public final class CustomerSchemaOwnerCurrent {
    private static void integrity(SQLiteDatabase db) {
        try (Cursor c=db.rawQuery("PRAGMA integrity_check",null)) {
            if (!c.moveToFirst() || !"ok".equals(c.getString(0))) throw new AssertionError("integrity");
        }
        try (Cursor c=db.rawQuery("PRAGMA foreign_key_check",null)) {
            if (c.moveToFirst()) throw new AssertionError("foreign key violation");
        }
    }
    public static void main(String[] args) throws Exception {
        SQLiteDatabase db=new SQLiteDatabase(args[0],args[1]);
        try {
            db.execSQL("PRAGMA foreign_keys=ON");
            if(db.getVersion()!=36) throw new AssertionError("unreviewed user_version");
            integrity(db);
            // Critical lifecycle changes belong to DbHelper; this research test requires it ready.
            SchemaDomainCoordinatorR926.verifyObjects(db,SchemaDomainCoordinatorR926.criticalContract());
            DbStore store=HostProductR974.store(db);
            DatabaseCoordinator owner=store.coordinatorR36();
            if(!ResearchSchemaCoordinatorR926.ensureToReady(owner,8))
                throw new AssertionError("research schema not ready: "+ResearchSchemaCoordinatorR926.summary());
            for(int repeat=0;repeat<2;repeat++) {
                if(!owner.runAtomicWrite("NUMBERED_CUSTOMER_CP12_SCHEMA_REENTRY", x -> {
                    ResearchMigrationRegistryR952.ensure(x);
                    RetentionArtifactIdentityCP12.install(x);
                })) throw new AssertionError("registered owner reentry failed");
                SchemaDomainCoordinatorR926.verifyObjects(db,SchemaDomainCoordinatorR926.researchContract());
                integrity(db);
            }
            try(Cursor c=db.rawQuery("PRAGMA foreign_key_list(research_development_receipt_v2)",null)) {
                boolean anchor=false;
                while(c.moveToNext()) if("research_artifact_identity_v2".equals(c.getString(2))) anchor=true;
                if(!anchor) throw new AssertionError("permanent artifact anchor FK absent");
            }
            System.out.println("PASS|actual registered CP12 Owner/research progression + twice reentry + required objects + integrity/FK|protected customer snapshot with host Android adapters; phone lifecycle unverified");
        } finally {db.shutdown();}
    }
}
