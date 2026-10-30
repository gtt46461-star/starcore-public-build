package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.ResearchMigrationRegistryR952;
import java.sql.DriverManager;
import java.lang.reflect.*;
import java.util.*;

/** Fresh schema proof on real SQLite: original unit SQL and immutable receipts remain byte exact.
 * This deterministic SQL-count regression is not Android pool-lock or wall-clock performance evidence. */
public final class SchemaDdlProofR29HostTest {
    static int checks;
    static void check(String id,boolean ok){System.out.println("R29_SCHEMA_DDL_PROOF|"+id+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(id);checks++;}
    static final class Counted extends SQLiteDatabase {
        long queries,ddl,executes;
        Counted()throws Exception{super(DriverManager.getConnection("jdbc:sqlite::memory:"));}
        @Override public Cursor rawQuery(String sql,String[] args){queries++;return super.rawQuery(sql,args);}
        @Override public void execSQL(String sql,Object[] args){executes++;String value=sql.stripLeading().toUpperCase(Locale.ROOT);while(value.startsWith("--")){int end=value.indexOf('\n');if(end<0)break;value=value.substring(end+1).stripLeading();}if(value.startsWith("CREATE ")||value.startsWith("DROP ")||value.startsWith("ALTER "))ddl++;super.execSQL(sql,args);}
        void reset(){queries=ddl=executes=0;}
    }
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static String text(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):null;}}
    static void advance(Counted db){db.beginTransactionNonExclusive();try{StrategySchemaSU1.advance(db);db.setTransactionSuccessful();}finally{db.endTransaction();}}
    static void invokeDomain(SQLiteDatabase db,String sql)throws Exception{
        Method m=ResearchMigrationRegistryR952.class.getDeclaredMethod("executeDomainDdlR29",SQLiteDatabase.class,String.class,boolean.class);m.setAccessible(true);
        try{m.invoke(null,db,sql,false);}catch(InvocationTargetException e){Throwable t=e.getCause();if(t instanceof Exception)throw (Exception)t;throw (Error)t;}
    }
    static void strategy()throws Exception{
        try(Counted db=new Counted()){
            FullSchemaCutoverR29HostTest.schema(db); // Actual final Runtime/R27/SU1/SU2 schema, no new unit receipts.
            db.reset();advance(db);check("D1_ONE_ACTUAL_IMMUTABLE_UNIT_NOT_FULL_READY",n(db,"SELECT count(*) FROM su1_migration_unit_v2")==1&&!StrategySchemaSU1.ready(db));
            long initialDdl=db.ddl;check("D2_MISSING_JOURNAL_INSTALLS_ONLY_ORIGINAL_TABLE_AND_THREE_GUARDS",initialDdl==4);
            db.reset();advance(db);long secondDdl=db.ddl;
            check("D3_EXISTING_EXACT_JOURNAL_AND_OBJECT_DO_NOT_REEXECUTE_DDL",secondDdl==0&&n(db,"SELECT count(*) FROM su1_migration_unit_v2")==2);
            while(n(db,"SELECT count(*) FROM su1_migration_unit_v2")<SqlStatementsSU1.SQL.length)advance(db);
            check("D4_ALL_ORIGINAL_UNITS_AND_HASHES_PRESERVED",StrategySchemaSU1.ready(db)&&n(db,"SELECT count(*) FROM su1_migration_unit_v2")==SqlStatementsSU1.SQL.length);
            try(Cursor c=db.rawQuery("SELECT unit,sql_hash FROM su1_migration_unit_v2 ORDER BY unit",null)){int i=0;boolean exact=true;while(c.moveToNext()){exact&=c.getInt(0)==i&&c.getString(1).equals(HashingSU1.text(SqlStatementsSU1.SQL[i]));i++;}check("D5_ORIGINAL_SQL_RECEIPT_BYTES_IDENTICAL",exact&&i==SqlStatementsSU1.SQL.length);}
            db.reset();check("D6_READY_PROOF_FRESH_BULK_CENSUS",StrategySchemaSU1.ready(db)&&db.queries<=8&&db.ddl==0);
            System.out.println("R29_SCHEMA_DDL_PROOF|MEASURED|completeUnits="+SqlStatementsSU1.SQL.length+"|readyQueries="+db.queries+"|secondAdvanceDdl="+secondDdl);
            String good=text(db,"SELECT sql FROM sqlite_master WHERE name='su1_migration_v2_no_update'");
            db.beginTransactionNonExclusive();try{db.execSQL("DROP TRIGGER su1_migration_v2_no_update");check("D7_REMOVED_GUARD_IS_NOT_READY_IN_CURRENT_TRANSACTION",!StrategySchemaSU1.ready(db));}finally{db.endTransaction();}
            check("D8_ROLLBACK_RESTORES_REAL_READY_PROOF",StrategySchemaSU1.ready(db));
            db.execSQL("DROP TRIGGER su1_migration_v2_no_update");db.execSQL("CREATE TRIGGER su1_migration_v2_no_update BEFORE UPDATE ON su1_migration_unit_v2 BEGIN SELECT 1; END");
            boolean bad=false;try{advance(db);}catch(IllegalStateException expected){bad=expected.getMessage().contains("JOURNAL_GUARD_MISMATCH");}
            check("D9_WRONG_SAME_NAME_GUARD_FAILS_CLOSED_WITHOUT_UNIT_PROGRESS",bad&&!StrategySchemaSU1.ready(db)&&n(db,"SELECT count(*) FROM su1_migration_unit_v2")==SqlStatementsSU1.SQL.length);
            db.execSQL("DROP TRIGGER su1_migration_v2_no_update");db.execSQL(good);check("D10_REAL_GUARD_REPAIR_REVALIDATES_WITHOUT_READY_CACHE",StrategySchemaSU1.ready(db));
        }
    }
    static void revisions()throws Exception{
        try(Counted db=new Counted()){
            db.execSQL("CREATE TABLE r952_knowledge(kind TEXT,knowledge_hash TEXT)");
            Field field=ResearchMigrationRegistryR952.class.getDeclaredField("CP4_DDL");field.setAccessible(true);String[] all=(String[])field.get(null);
            String drop=null,create=null;
            for(String sql:all){if(sql.contains("r952_knowledge_admission_delete")){if(sql.toUpperCase(Locale.ROOT).contains("DROP TRIGGER"))drop=sql;else if(sql.toUpperCase(Locale.ROOT).contains("CREATE TRIGGER"))create=sql;}}
            if(drop==null||create==null)throw new AssertionError("REAL_ORIGINAL_REVISION_PAIR_REQUIRED");
            // This real old guard's body references columns lazily; fixture need not fabricate archival evidence.
            invokeDomain(db,create);db.reset();invokeDomain(db,drop);invokeDomain(db,create);
            check("D11_EXACT_INSTALLED_ORIGINAL_REVISION_PAIR_HAS_ZERO_DDL",db.ddl==0);
            db.execSQL("DROP TRIGGER r952_knowledge_admission_delete");db.execSQL("CREATE TRIGGER r952_knowledge_admission_delete BEFORE DELETE ON r952_knowledge BEGIN SELECT 1; END");
            db.reset();invokeDomain(db,drop);invokeDomain(db,create);
            check("D12_DIFFERENT_ORIGINAL_GUARD_STILL_REPLACES_AND_VERIFIES",db.ddl==2&&text(db,"SELECT sql FROM sqlite_master WHERE name='r952_knowledge_admission_delete'").contains("RAISE"));
            db.execSQL("DROP TRIGGER r952_knowledge_admission_delete");db.reset();invokeDomain(db,drop);invokeDomain(db,create);
            check("D13_ABSENT_DROP_AND_MISSING_CREATE_PERFORM_ONE_REAL_DDL",db.ddl==1);
            db.execSQL("CREATE TRIGGER r29_literal_probe BEFORE UPDATE ON r952_knowledge BEGIN SELECT RAISE(ABORT,'Case Sensitive Literal'); END");
            db.reset();invokeDomain(db,"CREATE TRIGGER IF NOT EXISTS r29_literal_probe BEFORE UPDATE ON r952_knowledge BEGIN SELECT RAISE(ABORT,'case sensitive literal'); END");
            check("D14_LITERAL_DIFFERENCES_NEVER_MASQUERADE_AS_EXACT_PROOF",db.ddl==1);
            db.beginTransactionNonExclusive();try{db.execSQL("DROP TRIGGER r952_knowledge_admission_delete");invokeDomain(db,create);check("D15_TRANSACTION_LOCAL_GUARD_INSTALL_OBSERVED",text(db,"SELECT sql FROM sqlite_master WHERE name='r952_knowledge_admission_delete'").contains("RAISE"));}finally{db.endTransaction();}
            db.reset();invokeDomain(db,drop);invokeDomain(db,create);check("D16_ROLLBACK_ABA_RECHECKS_ACTUAL_SCHEMA",db.ddl==0);
        }
    }
    public static void main(String[] args)throws Exception{if(args.length>0&&args[0].equals("--cold-ready-control")){try(Counted db=new Counted()){FullSchemaCutoverR29HostTest.schema(db);while(n(db,"SELECT count(*) FROM sqlite_master WHERE name='su1_migration_unit_v2'")==0||n(db,"SELECT count(*) FROM su1_migration_unit_v2")<SqlStatementsSU1.SQL.length)advance(db);try{Field cache=StrategySchemaSU1.class.getDeclaredField("VERIFIED");cache.setAccessible(true);((Map<?,?>)cache.get(null)).clear();}catch(NoSuchFieldException noCache){}db.reset();boolean ready=StrategySchemaSU1.ready(db);System.out.println("R29_SCHEMA_DDL_PROOF|COLD_READY_MEASURED|queries="+db.queries);check("D6_READY_PROOF_FRESH_BULK_CENSUS",ready&&db.queries<=8);}return;}strategy();revisions();System.out.println("R29_SCHEMA_DDL_PROOF|ALL_PASS|checks="+checks+"|REAL_SQLITE_FRESH_OBJECT_PROOF_NO_READY_CACHE_ANDROID_POOL_ACCEPTANCE_SEPARATE");}
}
