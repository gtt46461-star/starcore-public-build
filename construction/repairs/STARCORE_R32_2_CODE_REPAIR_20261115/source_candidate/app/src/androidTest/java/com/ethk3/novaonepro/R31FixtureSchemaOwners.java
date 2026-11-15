package com.ethk3.novaonepro;

import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.strategyv2.SqlStatementsSU1;
import com.ethk3.novaonepro.strategyv2.StrategySchemaSU1;

/** Test setup uses the same authenticated, journaled SU1 owner as production.
 * No stamp reset, missing-object waiver, assertion change or fixture-data change. */
public final class R31FixtureSchemaOwners {
    private R31FixtureSchemaOwners() {}
    public static void installSu1WithinWriterR31(SQLiteDatabase db) {
        if(!db.inTransaction())throw new IllegalStateException("FIXTURE_SCHEMA_WRITER_REQUIRED");
        boolean trusted=SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.criticalContract());
        for(int step=0;step<SqlStatementsSU1.SQL.length+64;step++) {
            if(StrategySchemaSU1.advance(db)) {
                if(!StrategySchemaSU1.ready(db))throw new AssertionError("Actual SU1 journal/object postconditions are not ready");
                if(trusted&&!SchemaDomainCoordinatorR926.matches(db,SchemaDomainCoordinatorR926.criticalContract()))throw new AssertionError("Authenticated SU1 owner lost the trusted Critical contract");
                return;
            }
        }
        throw new AssertionError("Actual SU1 owner did not finish its bounded fixture setup");
    }
}
