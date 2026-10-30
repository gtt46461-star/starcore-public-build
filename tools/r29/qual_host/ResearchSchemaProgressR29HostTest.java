package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.ResearchSchemaCoordinatorR926;
import com.ethk3.novaonepro.SchemaDomainCoordinatorR926;
import java.lang.reflect.Method;
import java.sql.DriverManager;
import java.util.Arrays;

/** Real SQLite receipts: engineering schema progress excludes retries and telemetry writes. */
public final class ResearchSchemaProgressR29HostTest {
    static int checks;
    static void check(String name,boolean ok){System.out.println("R29_SCHEMA_PROGRESS|"+name+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(name);checks++;}
    static long[] census(SQLiteDatabase db)throws Exception{
        Method method=ResearchSchemaCoordinatorR926.class.getDeclaredMethod("schemaProgress",SQLiteDatabase.class);
        method.setAccessible(true);return (long[])method.invoke(null,db);
    }
    static void receipt(SQLiteDatabase db,String domain,String step,String status){
        db.execSQL("INSERT INTO "+SchemaDomainCoordinatorR926.TABLE_JOURNAL+"(domain,revision,step_id,status) VALUES(?,1,?,?)",new Object[]{domain,step,status});
    }
    public static void main(String[] args)throws Exception{
        try(SQLiteDatabase db=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite::memory:"))){
            SchemaDomainCoordinatorR926.ensureInfrastructure(db);
            db.execSQL("CREATE TABLE app_state(k TEXT PRIMARY KEY,v TEXT)");
            db.execSQL("CREATE TABLE su1_migration_unit_v2(unit INTEGER PRIMARY KEY,sql_hash TEXT,installed_at INTEGER)");
            long[] before=census(db);check("P1_READ_ONLY_REVIEW_HAS_ZERO_UNITS",Arrays.equals(before,census(db)));
            db.execSQL("CREATE TABLE research_new_schema_unit(k INTEGER)");long[] after=census(db);
            check("P2_ACTUAL_COMMITTED_DDL_CHANGES_SCHEMA_RECEIPT",after[0]!=before[0]);
            before=after;db.execSQL("INSERT INTO app_state VALUES('heartbeat','1')");
            check("P3_TELEMETRY_DOES_NOT_INVENT_SCHEMA_PROGRESS",Arrays.equals(before,census(db)));
            receipt(db,"RESEARCH","real-unit","COMPLETED");after=census(db);
            check("P4_COMPLETED_RESEARCH_UNIT_IS_OBSERVED",after[1]==before[1]+1);
            before=after;db.execSQL("UPDATE "+SchemaDomainCoordinatorR926.TABLE_JOURNAL+" SET started_at=99 WHERE step_id='real-unit'");
            check("P5_RETRY_TIMESTAMP_IS_NOT_ANOTHER_UNIT",Arrays.equals(before,census(db)));
            receipt(db,"RESEARCH","failed-unit","FAILED");
            check("P6_FAILED_ATTEMPT_IS_NOT_A_COMPLETED_UNIT",Arrays.equals(before,census(db)));
            receipt(db,"CRITICAL","retained-root-unit","COMPLETED");
            check("P7_ROOT_RECEIPT_DOES_NOT_COUNT_AS_RESEARCH_UNIT",Arrays.equals(before,census(db)));
            db.execSQL("INSERT INTO su1_migration_unit_v2 VALUES(1,'original-sql-hash',1)");after=census(db);
            check("P8_ACTUAL_SU1_UNIT_RECEIPT_IS_OBSERVED",after[2]==before[2]+1);
            before=after;db.beginTransactionNonExclusive();try{db.execSQL("CREATE TABLE research_rolled_back_unit(k INTEGER)");receipt(db,"RESEARCH","interrupted-unit","COMPLETED");}finally{db.endTransaction();}
            check("P9_ROLLBACK_CANNOT_CREATE_SCHEMA_PROGRESS",Arrays.equals(before,census(db)));
        }
        System.out.println("R29_SCHEMA_PROGRESS|ALL_PASS|checks="+checks+"|HOST_SQLITE_ENGINEERING_ONLY");
    }
}
