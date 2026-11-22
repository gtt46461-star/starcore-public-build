package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteStatement;
import java.io.File;

public final class SignedTenThousandHotWindowCheck {
    private static final long FIRST = TargetIssueResolver.sequenceOrdinalSU1("202611140001");
    private static String issue(int n) { return TargetIssueResolver.fromSequenceOrdinalSU1(FIRST + n); }
    private static void require(boolean value, String failure) {
        if (!value) throw new AssertionError(failure);
    }
    private static long number(SQLiteDatabase db, String sql) {
        try (Cursor row=db.rawQuery(sql,null)) {
            if (!row.moveToFirst()) throw new AssertionError(sql);
            return row.getLong(0);
        }
    }
    private static void insert(SQLiteStatement st, int offset) {
        st.clearBindings();
        st.bindLong(1,offset+1);
        st.bindString(2,issue(offset));
        st.bindLong(3,1780000000000L+60000L*offset);
        st.executeInsert();
    }
    private static String statement() {
        return "INSERT INTO draws VALUES(?,?,?,10,'controlled',1,2,3,4,5,6,7,8,9,10,0,'',1,1,'','{}')";
    }
    public static void main(String[] args) {
        File file=new File(args[0]); require(!file.exists(), "fresh evidence path");
        String staged=DrawDependencyInputCP12.TABLE, task=DbHelper.TABLE_R36_SETTLEMENT_TASK;
        int firstRun=5000;
        try(SQLiteDatabase db=SQLiteDatabase.openOrCreateDatabase(file,null)) {
            db.execSQL("CREATE TABLE draws(_id INTEGER PRIMARY KEY,issue TEXT UNIQUE,open_time INTEGER,fetch_time INTEGER,source_line TEXT,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER,dirty_reason TEXT,first_seen_at INTEGER,draw_hash_version INTEGER,draw_hash_v2 TEXT,raw_json TEXT)");
            db.execSQL("CREATE TABLE app_state(state_key TEXT PRIMARY KEY,state_value TEXT,updated_at INTEGER)");
            db.execSQL("CREATE TABLE "+task+"(target_issue TEXT PRIMARY KEY,status TEXT)");
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+"(target_issue TEXT,pre_draw_frozen INTEGER,settled INTEGER)");
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_TRADES+"(target_issue TEXT,settled INTEGER)");
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+"(target_issue TEXT,status TEXT)");
            DrawDependencyInputCP12.schemaWithinMigration(db);
            db.beginTransaction();
            try(SQLiteStatement st=db.compileStatement(statement())) {
                for(int n=0;n<firstRun;n++) insert(st,n);
                db.execSQL("INSERT INTO "+task+" VALUES(?,'PENDING')",new Object[]{issue(0)});
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
            db.beginTransaction();
            try {
                require(DataRetentionGovernorR900.reserveTailSlotWithinWriteCP12(db,issue(firstRun)),"reserve aborting insert");
                require(number(db,"SELECT count(*) FROM "+staged)==1,"abort journal has pinned original");
            } finally { db.endTransaction(); }
            require(number(db,"SELECT count(*) FROM "+staged)==0 && number(db,"SELECT count(*) FROM draws")==5000,"abort restored source and pin");
            db.beginTransaction();
            try(SQLiteStatement st=db.compileStatement(statement())) {
                require(!DataRetentionGovernorR900.reserveTailSlotWithinWriteCP12(db,issue(0)),"reject older duplicate");
                require(DataRetentionGovernorR900.reserveTailSlotWithinWriteCP12(db,issue(firstRun)),"reserve actual insert");
                insert(st,firstRun);
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
        }
        try(SQLiteDatabase db=SQLiteDatabase.openDatabase(file.getAbsolutePath(),null,SQLiteDatabase.OPEN_READWRITE)) {
            require(number(db,"SELECT count(*) FROM draws")==5000 && number(db,"SELECT count(*) FROM "+staged)==1,"cold original survives reopen");
            require(DrawDependencyInputCP12.verifiedOpenTimeR77(db,issue(0))==1780000000000L,"exact input hash and clock");
            require(number(db,"SELECT count(*) FROM "+DrawDependencyInputCP12.SOURCE)==5001,"dependent consumer can read displaced row");
            db.beginTransaction();
            try(SQLiteStatement st=db.compileStatement(statement())) {
                db.execSQL("UPDATE "+task+" SET status='COMPLETED' WHERE target_issue=?",new Object[]{issue(0)});
                for(int n=firstRun+1;n<10000;n++) {
                    require(DataRetentionGovernorR900.reserveTailSlotWithinWriteCP12(db,issue(n)),"rotate "+n);
                    insert(st,n);
                }
                db.setTransactionSuccessful();
            } finally { db.endTransaction(); }
        }
        try(SQLiteDatabase db=SQLiteDatabase.openDatabase(file.getAbsolutePath(),null,SQLiteDatabase.OPEN_READONLY)) {
            require(number(db,"SELECT count(*) FROM draws")==5000 && number(db,"SELECT count(*) FROM "+staged)==0,"consumed pin retired");
            try(Cursor c=db.rawQuery("SELECT min(issue),max(issue) FROM draws",null)) {
                require(c.moveToFirst() && c.getString(0).equals(issue(5000)) && c.getString(1).equals(issue(9999)),"hot tail identity");
            }
            require(HotHistoryWindowR335.read(db,issue(9999)).missingPeriods==0,"actual 5000-issue calendar");
            require(db.isDatabaseIntegrityOk(),"SQLite integrity after reopen");
        }
        System.out.println("HOT_WINDOW_SIGNED inserted=10000 hot=5000 retired=5000 pinnedBeforeConsumption=1 pinnedAfterConsumption=0 rollback=true reopens=2 calendarMissing=0 integrity=ok");
    }
}
