package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.sql.DriverManager;

/** Exercise the production owner with separate real SQLite connections. */
public final class RetentionMigrationHost {
    public static void main(String[] args) throws Exception {
        String url="jdbc:sqlite:"+args[0];
        try(SQLiteDatabase write=new SQLiteDatabase(DriverManager.getConnection(url));
            SQLiteDatabase read=new SQLiteDatabase(DriverManager.getConnection(url))) {
            try(Cursor mode=write.rawQuery("PRAGMA journal_mode=WAL",null)) { mode.moveToFirst(); }
            RetentionMigrationR34.Writer owner=(context,task)->{
                write.beginTransaction();
                try {
                    task.run(write);
                    write.setTransactionSuccessful();
                } catch(RuntimeException failure) {
                    throw failure;
                } catch(Exception failure) {
                    throw new IllegalStateException(failure);
                } finally {
                    write.endTransaction();
                }
            };
            RetentionMigrationR34.Session first=new RetentionMigrationR34.Session(read,owner,"process-one");
            for(int i=0;i<14;i++)if(first.step())throw new AssertionError("completed before all units");
            if(number(read,"SELECT count(*) FROM r34_schema_unit_receipt")!=14)throw new AssertionError("receipt count after first process");
            RetentionMigrationR34.Session resumed=new RetentionMigrationR34.Session(read,owner,"process-two");
            if(resumed.step())throw new AssertionError("completed before verification");
            try {
                first.step();
                throw new AssertionError("old owner wrote after takeover");
            } catch(IllegalStateException expected) {
                if(!"R34_STALE_MIGRATION_OWNER".equals(expected.getMessage()))throw expected;
            }
            int turns=0;
            while(!resumed.step())if(++turns>120)throw new AssertionError("stalled migration at "+number(read,"SELECT max(next_index) FROM r34_schema_unit_run"));
            if(number(read,"SELECT count(*) FROM r34_schema_unit_receipt")!=116)throw new AssertionError("must have 58 install and 58 verify receipts");
            if(number(read,"SELECT count(*) FROM r34_schema_unit_run WHERE phase='DONE' AND owner_fence=2")!=1)throw new AssertionError("missing durable completion");
            if(!resumed.step() || number(read,"SELECT count(*) FROM r34_schema_unit_receipt")!=116)throw new AssertionError("completion replay wrote a new unit");
            write.execSQL("CREATE TABLE r34_host_schema_change(id INTEGER)");
            if(resumed.step())throw new AssertionError("reused verification after schema changed");
            for(int i=0;i<58;i++) {
                boolean done=resumed.step();
                if(done!=(i==57))throw new AssertionError("verification resumed at the wrong cursor");
            }
            if(number(read,"SELECT count(*) FROM r34_schema_unit_receipt")!=174)throw new AssertionError("fresh verification missing");
        }
        System.out.println("R34_PRODUCTION_RETENTION_SQLITE_PASS 116 install/verify receipts, owner takeover, durable readback, schema-cookie revalidation");
    }

    private static long number(SQLiteDatabase db,String sql) {
        try(Cursor c=db.rawQuery(sql,null)) {
            if(!c.moveToFirst())throw new AssertionError(sql);
            return c.getLong(0);
        }
    }
}
