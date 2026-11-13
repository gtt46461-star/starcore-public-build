package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.Locale;
import java.util.HashSet;
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
        // At most one actual table census, owned by this callback only. A new
        // callback starts empty; business/readiness calls cannot inherit it.
        String columnTable;
        HashSet<String> columnNames;
        long columnSchemaVersion;
        String fingerprintKey,fingerprintValue;
        long fingerprintSchemaVersion;
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
    private static long schemaVersion(SQLiteDatabase db){try(Cursor c=db.rawQuery("PRAGMA schema_version",null)){if(!c.moveToFirst())throw new IllegalStateException("LEGACY_SCHEMA_VERSION_MISSING");return c.getLong(0);}}
    // Unqualified legacy DDL/PRAGMA names may resolve TEMP or attached objects.
    // Such connections retain the original fresh algorithm; never let a main
    // schema cookie describe another schema's object or a previous attachment.
    private static long cacheSchemaVersion(SQLiteDatabase db,Scope scope){
        boolean mainOnly=true;
        try(Cursor c=db.rawQuery("PRAGMA database_list",null)){
            while(c.moveToNext()){String name=c.getString(1);if(!"main".equals(name)&&!"temp".equals(name))mainOnly=false;}
        }
        if(mainOnly)try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_temp_master LIMIT 1",null)){mainOnly=!c.moveToFirst();}
        if(mainOnly)return schemaVersion(db);
        scope.columnTable=null;scope.columnNames=null;scope.fingerprintKey=null;scope.fingerprintValue=null;
        return -1;
    }
    /** Exact schema hashes may be reused only within this callback, on a main
     * only connection and the same live schema_version. Unknown DDL is fresh. */
    static String scopedFingerprint(SQLiteDatabase db,String key,java.util.function.Supplier<String> actual){
        Scope scope=CURRENT.get();if(scope==null||scope.db!=db)return actual.get();
        long before=cacheSchemaVersion(db,scope);
        if(before>=0&&key.equals(scope.fingerprintKey)&&scope.fingerprintValue!=null&&before==scope.fingerprintSchemaVersion)return scope.fingerprintValue;
        String value=actual.get();
        if(before>=0&&cacheSchemaVersion(db,scope)==before){scope.fingerprintKey=key;scope.fingerprintValue=value;scope.fingerprintSchemaVersion=before;}
        else{scope.fingerprintKey=null;scope.fingerprintValue=null;}
        return value;
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
    private interface VerifiedDdlR31 { void verify(); }
    /** An original Legacy unit may add columns/indexes to a Critical table. Keep
     * that one verified statement and the already-authenticated stamp atomic.
     * A pre-existing mismatch is never re-stamped by this path. */
    private static void mutate(SQLiteDatabase db,String sql,VerifiedDdlR31 verified) {
        Scope scope=CURRENT.get();
        if(scope==null){db.execSQL(sql);verified.verify();return;}
        Matcher create=CREATE.matcher(sql),alter=ALTER.matcher(sql);
        String object,table;
        if(alter.find()){object=table=alter.group(1);}
        else if(create.find()){
            object=create.group(2);table=object;
            if(!"TABLE".equalsIgnoreCase(create.group(1))){
                Matcher on=Pattern.compile("(?is)\\bON\\s+[\\\"`\\[]?([a-zA-Z0-9_]+)").matcher(sql);
                if(!on.find())throw new IllegalArgumentException("LEGACY_DDL_TABLE_UNDECLARED");table=on.group(1);
            }
        }else if(RETIRE_INDEX.matcher(sql).matches()){
            object="idx_v26_oos_one_action_target";table="";
            try(Cursor owner=db.rawQuery("SELECT tbl_name FROM sqlite_master WHERE type='index' AND name=?",new String[]{object})){if(owner.moveToFirst())table=owner.getString(0);}
        }else throw new IllegalArgumentException("LEGACY_DDL_UNSUPPORTED:"+sql);
        SchemaDomainCoordinatorR926.migrateLegacyDdlR31(db,sql,object,table,x->{db.execSQL(sql);scope.statements++;verified.verify();});
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
                mutate(db,sql,()->{if(retiredIndexPresent(db))throw new IllegalStateException("LEGACY_DDL_POSTCONDITION:RETIRE_V26_INDEX");});
                return;
            }
            Matcher create=CREATE.matcher(sql),alter=ALTER.matcher(sql);
            boolean isCreate=create.find(),isAlter=alter.find();
            if(!isCreate&&!isAlter)throw new IllegalArgumentException("LEGACY_DDL_UNSUPPORTED:"+sql);
            if(present(db,sql,isCreate?create:null,isAlter?alter:null))return;
            if(s!=null && (s.readOnly||s.statements>=1))throw new Pending("LEGACY_DDL_PENDING:"+sql);
            mutate(db,sql,()->{
            if(isAlter&&s!=null) {
                // Qualified names cannot use SQLite's double-quoted string
                // fallback. Preparation of this zero-row read proves the new
                // column really exists without materializing the whole schema.
                String table=alter.group(1),column=alter.group(2);
                try(Cursor verified=db.rawQuery("SELECT r31_verify.\""+column+"\" FROM \""+table+"\" AS r31_verify LIMIT 0",null)) {
                    if(verified.getColumnCount()!=1||verified.moveToFirst())throw new IllegalStateException("LEGACY_DDL_COLUMN_POSTCONDITION:"+column);
                }
                if(table.equalsIgnoreCase(s.columnTable)&&s.columnNames!=null){
                    long version=cacheSchemaVersion(db,s);
                    if(version>=0&&version==s.columnSchemaVersion+1){s.columnNames.add(column.toLowerCase(Locale.ROOT));s.columnSchemaVersion=version;}
                    else{s.columnTable=null;s.columnNames=null;}
                }
            } else if(!present(db,sql,isCreate?create:null,isAlter?alter:null))
                throw new IllegalStateException("LEGACY_DDL_POSTCONDITION:"+sql);
            });
        } catch(RuntimeException e) { failed(e); throw e; }
    }
    private static boolean retiredIndexPresent(SQLiteDatabase db) {
        try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='index' AND name='idx_v26_oos_one_action_target'",null)){return c.moveToFirst();}
    }
    private static boolean present(SQLiteDatabase db,String sql,Matcher create,Matcher alter) {
        if(alter!=null) {
            String table=alter.group(1);Scope scope=CURRENT.get();
            long before=scope!=null&&scope.db==db?cacheSchemaVersion(db,scope):-1;
            if(before>=0&&scope!=null&&scope.db==db&&table.equalsIgnoreCase(scope.columnTable)&&scope.columnNames!=null
                    &&scope.columnSchemaVersion==before)
                return scope.columnNames.contains(alter.group(2).toLowerCase(Locale.ROOT));
            HashSet<String> names=new HashSet<>();
            try(Cursor c=db.rawQuery("PRAGMA table_info(\""+table+"\")",null)) {
                while(c.moveToNext())names.add(c.getString(1).toLowerCase(Locale.ROOT));
            }
            if(before>=0&&scope!=null&&scope.db==db){
                long after=cacheSchemaVersion(db,scope);if(after!=before)throw new Pending("LEGACY_SCHEMA_CHANGED_DURING_COLUMN_CENSUS");
                scope.columnTable=table;scope.columnNames=names;scope.columnSchemaVersion=after;
            }
            return names.contains(alter.group(2).toLowerCase(Locale.ROOT));
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
        // Only layout and unquoted token case are insignificant. Quoted tokens
        // are part of the canonical owner definition. Preserve them byte for
        // byte, including SQLite's legacy double-quoted string fallback.
        StringBuilder out=new StringBuilder();char quote=0;
        for(int i=0;i<sql.length();i++) {
            char value=sql.charAt(i);
            if(quote!=0) {
                out.append(value);
                if(value==quote) {
                    if(i+1<sql.length()&&sql.charAt(i+1)==quote)out.append(sql.charAt(++i));
                    else quote=0;
                }
            } else if(value=='\''||value=='"'||value=='`'||value=='[') {quote=value=='['?']':value;out.append(value);}
            else if(!Character.isWhitespace(value)&&value!=';')out.append(Character.toLowerCase(value));
        }
        return out.toString().replaceFirst("^(create(?:unique)?(?:index|trigger))ifnotexists","$1");
    }
}
