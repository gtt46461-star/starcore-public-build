package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Strict, resumable DDL for the existing Legacy migration owner.
 * A callback changes at most one schema object. Even a historical catch(Throwable)
 * cannot hide a failed statement or consume the rest of the slice's budget. */
final class LegacyDdlR31 {
    private LegacyDdlR31() {}
    private static final ThreadLocal<Scope> CURRENT = new ThreadLocal<>();
    private static final Pattern CREATE = Pattern.compile(
            "(?is)^\\s*CREATE\\s+(?:UNIQUE\\s+)?(TABLE|INDEX|TRIGGER)\\s+(?:IF\\s+NOT\\s+EXISTS\\s+)?[\\\"`\\[]?([a-zA-Z0-9_]+)");
    private static final Pattern ALTER = Pattern.compile(
            "(?is)^\\s*ALTER\\s+TABLE\\s+[\\\"`\\[]?([a-zA-Z0-9_]+)[\\\"`\\]]?\\s+ADD\\s+(?:COLUMN\\s+)?[\\\"`\\[]?([a-zA-Z0-9_]+)");
    private static final Pattern RETIRE_INDEX = Pattern.compile(
            "(?is)^\\s*DROP\\s+INDEX\\s+IF\\s+EXISTS\\s+idx_v26_oos_one_action_target\\s*;?\\s*$");

    static final class Pending extends RuntimeException {
        private static final long serialVersionUID = 1L;
        Pending(String message) { super(message); }
    }
    static final class Scope implements AutoCloseable {
        final SQLiteDatabase db;
        final boolean readOnly;
        final long started = System.nanoTime();
        int statements;
        RuntimeException failure;
        Scope(SQLiteDatabase db, boolean readOnly) {
            if (CURRENT.get() != null) throw new IllegalStateException("LEGACY_DDL_NESTED_OWNER");
            this.db = db; this.readOnly = readOnly; CURRENT.set(this);
        }
        void check() { if (failure != null) throw failure; }
        String evidence() {
            return "ddl=" + statements + "|elapsed_us=" + (System.nanoTime()-started)/1000L
                    + "|cancelled=" + Thread.currentThread().isInterrupted();
        }
        @Override public void close() { CURRENT.remove(); }
    }
    static boolean owned(SQLiteDatabase db) { Scope s=CURRENT.get(); return s!=null && s.db==db; }
    static boolean suppressDefaults(SQLiteDatabase db) {
        Scope s=CURRENT.get(); return s!=null && s.db==db && (s.readOnly || s.failure!=null);
    }
    static void check() { Scope s=CURRENT.get(); if(s!=null)s.check(); }
    static void failed(Throwable error) {
        Scope s=CURRENT.get(); if(s==null)return;
        Error fatal=com.ethk3.novaonepro.strategyv2.FailureDispositionR31.processFatalCause(error);
        if(fatal!=null)throw fatal;
        if(s.failure==null)s.failure=error instanceof RuntimeException?(RuntimeException)error:
                new IllegalStateException("LEGACY_DDL_FAILED",error);
    }
    static void exec(SQLiteDatabase db,String sql) {
        Scope s=CURRENT.get();
        try {
            if(s!=null) {
                if(s.db!=db)throw new IllegalStateException("LEGACY_DDL_WRONG_HANDLE");
                s.check();
                if(Thread.currentThread().isInterrupted())throw new java.util.concurrent.CancellationException("LEGACY_DDL_CANCELLED");
            }
            // This exact retirement belongs to the original V28 migration.
            // No general DROP TABLE/INDEX authority is introduced.
            if(RETIRE_INDEX.matcher(sql).matches()) {
                if(!retiredIndexPresent(db))return;
                if(s!=null&&(s.readOnly||s.statements>=1))throw new Pending("LEGACY_DDL_PENDING:RETIRE_V26_INDEX");
                db.execSQL(sql);if(s!=null)s.statements++;
                if(retiredIndexPresent(db))throw new IllegalStateException("LEGACY_DDL_POSTCONDITION:RETIRE_V26_INDEX");
                return;
            }
            Matcher create=CREATE.matcher(sql),alter=ALTER.matcher(sql);
            boolean isCreate=create.find(),isAlter=alter.find();
            if(!isCreate&&!isAlter)throw new IllegalArgumentException("LEGACY_DDL_UNSUPPORTED:"+sql);
            if(present(db,sql,isCreate?create:null,isAlter?alter:null))return;
            if(s!=null && (s.readOnly||s.statements>=1))throw new Pending("LEGACY_DDL_PENDING:"+sql);
            db.execSQL(sql);
            if(s!=null)s.statements++;
            if(!present(db,sql,isCreate?create:null,isAlter?alter:null))
                throw new IllegalStateException("LEGACY_DDL_POSTCONDITION:"+sql);
        } catch(RuntimeException e) { failed(e); throw e; }
    }
    private static boolean retiredIndexPresent(SQLiteDatabase db) {
        try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='index' AND name='idx_v26_oos_one_action_target'",null)){return c.moveToFirst();}
    }
    private static boolean present(SQLiteDatabase db,String sql,Matcher create,Matcher alter) {
        if(alter!=null) {
            try(Cursor c=db.rawQuery("PRAGMA table_info(\""+alter.group(1)+"\")",null)) {
                while(c.moveToNext())if(alter.group(2).equalsIgnoreCase(c.getString(1)))return true;
            }
            return false;
        }
        String kind=create.group(1).toLowerCase(Locale.ROOT);
        try(Cursor c=db.rawQuery("SELECT sql FROM sqlite_master WHERE type=? AND name=?",new String[]{kind,create.group(2)})) {
            if(!c.moveToFirst())return false;
            // Tables are additive: later column units validate their full column contract.
            // Index and trigger definitions must match, not merely share an object name.
            if(!"table".equals(kind)&&!normalize(sql).equals(normalize(c.getString(0))))
                throw new IllegalStateException("LEGACY_DDL_DEFINITION_DRIFT:"+create.group(2));
            return true;
        }
    }
    private static String normalize(String sql) {
        return sql.toLowerCase(Locale.ROOT).replaceAll("(?i)if\\s+not\\s+exists","")
                .replaceAll("[\\s\\\"`\\[\\];]","");
    }
}
