package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.ResearchMigrationRegistryR952;
import java.sql.Connection;
import java.sql.DriverManager;

/** Executes the exact shared Android fixture and original migration bytecode on real SQLite. */
public final class OriginalReceiptSchemaR30HostTest {
    interface Work { void run(); }
    static void require(boolean condition, String code) { if (!condition) throw new AssertionError(code); }
    static long n(SQLiteDatabase db, String sql) { try (Cursor c=db.rawQuery(sql,null)) { require(c.moveToFirst(),"SCALAR_MISSING"); return c.getLong(0); } }
    static void rejected(Work work,String code) {
        try { work.run(); throw new AssertionError("ORIGINAL_CONSTRAINT_NOT_ENFORCED:"+code); }
        catch (android.database.sqlite.SQLiteException expected) { require(expected.getMessage().contains(code),expected.getMessage()); }
    }
    static String cause(Throwable error) { StringBuilder s=new StringBuilder();for(Throwable t=error;t!=null;t=t.getCause()) { s.append(t.getMessage()).append('|');if(t.getCause()==t)break; }return s.toString(); }
    static void rows(SQLiteDatabase db,int id,String target) {
        String h="0123456789abcdef0123456789abcdef0123456789abcdef0123456789abcdef";
        db.execSQL("INSERT INTO trades VALUES(?,?)",new Object[]{id,target});
        db.execSQL("INSERT INTO r974_exact_real_receipt VALUES(?,?,?,?,?,?,?,?,?,?)",new Object[]{target,id,"SU2-F-"+h,h,h,h,h,h,h,11});
        db.execSQL("INSERT INTO research_exact_probability_f05 VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{"SYNTHETIC_NOT_QUALITY",target,id,"SU2-F-"+h,h,h,h,h,h,.4,h,h,h,10,11});
    }
    public static void main(String[] args)throws Exception {
        Class.forName("org.sqlite.JDBC");
        try(Connection conn=DriverManager.getConnection("jdbc:sqlite::memory:")) {
            SQLiteDatabase db=new SQLiteDatabase(conn);db.execSQL("PRAGMA foreign_keys=ON");db.execSQL("CREATE TABLE trades(_id INTEGER PRIMARY KEY,target_issue TEXT)");
            conn.setAutoCommit(false);
            try { ResearchMigrationRegistryR952.ensure(db);throw new AssertionError("OLD_FIXTURE_MISSING_DEPENDENCY_NOT_REPRODUCED"); }
            catch(IllegalStateException expected) { require(cause(expected).contains("research_candidate_cal_model_r932"),cause(expected)); }
            conn.rollback();
            R30OriginalReceiptFixture.schema(db);
            require(!SchemaDomainCoordinatorR926.stepComplete(db,SchemaDomainCoordinatorR926.researchContract(),"R64_CP4_DDL_0"),"UNRELATED_FULL_RESEARCH_DOMAIN_WAS_MIGRATED");
            long units=n(db,"SELECT count(*) FROM v57_schema_migration_journal_r926 WHERE status='COMPLETED'");
            R30OriginalReceiptFixture.schema(db);require(units==n(db,"SELECT count(*) FROM v57_schema_migration_journal_r926 WHERE status='COMPLETED'"),"FIXTURE_REPLAY_CHANGED_JOURNAL");
            rows(db,1,"202610310241");conn.commit();
            require(n(db,"SELECT count(*) FROM trades t JOIN r974_exact_real_receipt e ON e.trade_id=t._id JOIN research_exact_probability_f05 q ON q.trade_id=t._id AND q.proof_hash=e.proof_hash AND q.authorization_hash=e.authorization_hash WHERE q.selected_hit_p=.4")==1,"ORIGINAL_RECEIPTS_NOT_ATOMICALLY_BOUND");
            rejected(()->db.execSQL("UPDATE r974_exact_real_receipt SET committed_at=12"),"R974_REAL_RECEIPT_IMMUTABLE");
            rejected(()->db.execSQL("UPDATE research_exact_probability_f05 SET selected_hit_p=.5"),"F05_EXACT_PROBABILITY_IMMUTABLE");
            rejected(()->db.execSQL("INSERT INTO research_exact_probability_f05 SELECT * FROM research_exact_probability_f05"),"F05_EXACT_PROBABILITY_FIRST_WRITE");
            rows(db,2,"202610310242");conn.rollback();
            require(n(db,"SELECT count(*) FROM trades")==1&&n(db,"SELECT count(*) FROM r974_exact_real_receipt")==1&&n(db,"SELECT count(*) FROM research_exact_probability_f05")==1,"ROLLBACK_LEFT_HALF_TRADE_OR_RECEIPT");
        }
        System.out.println("R30_ORIGINAL_RECEIPT_SCHEMA|ACTUAL_SHARED_ANDROID_FIXTURE|ORIGINAL_MIGRATION_BYTECODE|OLD_DEPENDENCY_FAILURE_REPRODUCED|ORIGINAL_CONSTRAINTS_AND_ROLLBACK|NO_FULL_DOMAIN_READY_CLAIM|HOST_NOT_ANDROID_OR_NATURAL|PASS");
    }
}
