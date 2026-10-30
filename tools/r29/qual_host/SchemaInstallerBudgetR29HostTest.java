package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.nio.file.*;
import java.sql.DriverManager;
import java.util.*;

/** Actual Root/RP installers and fresh per-invocation query accounting on SQLite. No timing/device claim. */
public final class SchemaInstallerBudgetR29HostTest {
    static int checks;
    static void check(String name,boolean ok){System.out.println("R29_INSTALLER_BUDGET|"+name+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(name);checks++;}
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static long units(SQLiteDatabase db){return n(db,"SELECT count(*) FROM sqlite_master WHERE name='su1_migration_unit_v2'")==0?0:n(db,"SELECT count(*) FROM su1_migration_unit_v2");}
    static boolean unit(SQLiteDatabase db){db.beginTransactionNonExclusive();try{boolean ready=StrategySchemaSU1.advance(db);db.setTransactionSuccessful();return ready;}finally{db.endTransaction();}}
    static void exact(SQLiteDatabase db,String label){
        int count=0;boolean exact=true;try(Cursor c=db.rawQuery("SELECT unit,sql_hash FROM su1_migration_unit_v2 ORDER BY unit",null)){
            while(c.moveToNext()){exact&=count<SqlStatementsSU1.SQL.length&&c.getInt(0)==count&&c.getString(1).equals(HashingSU1.text(SqlStatementsSU1.SQL[count]));count++;}
        }
        check(label+"_ALL_ORIGINAL_265_ORDERED_SQL_HASHES_RETAINED",exact&&count==265&&count==SqlStatementsSU1.SQL.length);
    }
    static long readyQueries(SQLiteDatabase db,String label){
        long[] count={0},roles={0};SQLiteDatabase.hostBeforeQuery=(handle,sql)->{if(handle==db){count[0]++;if(sql.equals("SELECT 1 FROM main.sqlite_master WHERE type='table' AND name='r29_research_owner'"))roles[0]++;}};
        boolean ready;try{ready=StrategySchemaSU1.ready(db);}finally{SQLiteDatabase.hostBeforeQuery=null;}
        System.out.println("R29_INSTALLER_BUDGET|MEASURED|"+label+"|readyQueries="+count[0]+"|roleQueries="+roles[0]);
        check(label+"_FRESH_COMPLETE_READY_PROOF_AT_MOST_TEN_QUERIES",ready&&count[0]<=10&&roles[0]<=1);return count[0];
    }
    static void finish(SQLiteDatabase db,String label){
        while(units(db)<265){long before=units(db);unit(db);checkUnit(db,label,before);}
        exact(db,label);check(label+"_REAL_FINAL_STORAGE_READY",StrategySchemaSU1.ready(db));
    }
    static void checkUnit(SQLiteDatabase db,String label,long before){if(units(db)!=before+1)throw new AssertionError(label+"_ONE_REAL_JOURNALED_UNIT_PER_SHORT_TRANSACTION");}
    public static void main(String[]args)throws Exception{
        String mode=args.length==0?"":args[0];Path dir=Files.createTempDirectory("r29-schema-installer-budget-");
        try(SQLiteDatabase root=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite:"+dir.resolve("root.db")));SQLiteDatabase rp=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite:"+dir.resolve("rp.db")))){
            ResearchPhaseStampR29HostTest.bootstrap(root,rp);int entered=0;
            while(units(rp)<1&&entered++<1000){ResearchPhaseStampR29HostTest.advance(rp);if(ResearchSchemaCoordinatorR926.summary().startsWith("FAILED"))throw new AssertionError(ResearchSchemaCoordinatorR926.summary());}
            check("B1_REAL_RP_PENDING_INSTALLER_REACHES_FIRST_UNIT",units(rp)==1);
            if(!mode.equals("--old-rp-ready-control")){
                long[] queries={0};SQLiteDatabase.hostBeforeQuery=(db,sql)->{if(db==rp)queries[0]++;};
                try{for(int i=0;i<8;i++){boolean ready=ResearchPhaseStampR29HostTest.advance(rp);if(ready)throw new AssertionError("PENDING_UNIT_MUST_NOT_BECOME_READY");}}finally{SQLiteDatabase.hostBeforeQuery=null;}
                System.out.println("R29_INSTALLER_BUDGET|MEASURED|RP_PENDING_EIGHT|queries="+queries[0]+"|old84Queries=472|budget=354");
                check("B2_EIGHT_REAL_PENDING_UNITS_REDUCE_OLD_QUERY_COUNT_BY_AT_LEAST_ONE_QUARTER",queries[0]<=354&&units(rp)==9);
                if(!mode.isEmpty())return;
            }
            finish(rp,"RP");readyQueries(rp,"RP");if(!mode.isEmpty())return;
            finish(root,"ROOT");readyQueries(root,"ROOT");
            ResearchSchemaOwnerR29HostTest.noRootAuthorities(rp);
            long[] fresh={0};SQLiteDatabase.hostBeforeQuery=(db,sql)->{if(db==rp&&sql.startsWith("SELECT name,type,sql FROM main.sqlite_master"))fresh[0]++;};
            try{check("B3_REPEATED_READY_READS_CURRENT_OBJECT_CENSUS",StrategySchemaSU1.ready(rp)&&StrategySchemaSU1.ready(rp)&&fresh[0]==2);}finally{SQLiteDatabase.hostBeforeQuery=null;}
            rp.beginTransactionNonExclusive();try{rp.execSQL("DROP TRIGGER su1_migration_v2_no_update");check("B4_TRANSACTION_LOCAL_MISSING_GUARD_FAILS_READY",!StrategySchemaSU1.ready(rp));}finally{rp.endTransaction();}
            check("B5_ROLLBACK_REVALIDATES_GUARD_WITHOUT_READY_CACHE",StrategySchemaSU1.ready(rp));
        }finally{SQLiteDatabase.hostBeforeQuery=null;try(java.util.stream.Stream<Path> paths=Files.walk(dir)){paths.sorted(Comparator.reverseOrder()).forEach(p->{try{Files.deleteIfExists(p);}catch(Exception ignored){}});}}
        System.out.println("R29_INSTALLER_BUDGET|ALL_PASS|checks="+checks+"|ACTUAL_ROOT_RP_INSTALLER_SQL_COUNTS_HOST_ONLY");
    }
}
