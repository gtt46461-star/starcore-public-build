package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.*;
import java.sql.*;
import java.util.*;

/** Native SQLite runs the actual migration owner, strict executor and journal.
 * Only Android transport is adapted. Child JVM termination is a real OS exit. */
public final class StrictMigrationR31Host {
    static int assertions;
    static void need(boolean value,String detail){assertions++;if(!value)throw new AssertionError(detail);}
    static long scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){need(c.moveToFirst(),sql);return c.getLong(0);}}
    static class FaultDb extends SQLiteDatabase {
        String blocked="",ignoredAlter="";int tableInfoQueries,catalogQueries;
        FaultDb(Connection connection){super(connection);}
        @Override public void execSQL(String sql){if(!blocked.isEmpty()&&sql.startsWith(blocked))throw new android.database.sqlite.SQLiteException("INJECTED_"+blocked);if(!ignoredAlter.isEmpty()&&sql.startsWith(ignoredAlter))return;super.execSQL(sql);}
        @Override public Cursor rawQuery(String sql,String[] args){if(sql.startsWith("PRAGMA table_info"))tableInfoQueries++;if(sql.contains("COALESCE(sql,'')"))catalogQueries++;return super.rawQuery(sql,args);}
    }
    static LinkedHashMap<String,Runnable> units(SQLiteDatabase db,boolean halt){
        LinkedHashMap<String,Runnable> units=new LinkedHashMap<>();
        units.put("OBJECTS",()->{
            // Historical catch sites deliberately swallow the thrown value. The
            // scope must still refuse COMPLETED and stop any subsequent SQL.
            try{
                LegacyDdlR31.exec(db,"CREATE TABLE IF NOT EXISTS uf_sample(id INTEGER PRIMARY KEY,value TEXT)");
                LegacyDdlR31.exec(db,"ALTER TABLE uf_sample ADD COLUMN epoch INTEGER DEFAULT 7");
                if(halt)Runtime.getRuntime().halt(73);
                LegacyDdlR31.exec(db,"CREATE INDEX IF NOT EXISTS uf_sample_value ON uf_sample(value)");
            }catch(Throwable swallowed){ /* exercise sticky first failure */ }
        });return units;
    }
    static boolean advance(SQLiteDatabase db,boolean halt)throws Exception{return LegacySchemaCoordinatorR926.advanceUnitsR31(db,"UF_NATIVE_",units(db,halt),false);}
    static void finish(SQLiteDatabase db)throws Exception{for(int i=0;i<20;i++)if(advance(db,false))return;throw new AssertionError("migration did not finish");}
    public static void main(String[] args)throws Exception {
        Class.forName("org.sqlite.JDBC");File path=new File(args[1]);
        try(FaultDb db=new FaultDb(DriverManager.getConnection("jdbc:sqlite:"+path))) {
            if(args[0].equals("halt")) {
                LinkedHashMap<String,Runnable> crash=new LinkedHashMap<>();
                crash.put("AUTOCOMMIT",()->{LegacyDdlR31.exec(db,"CREATE TABLE IF NOT EXISTS killed_unit(id INTEGER PRIMARY KEY,value TEXT)");Runtime.getRuntime().halt(73);});
                LegacySchemaCoordinatorR926.advanceUnitsR31(db,"UF_KILL_",crash,false);throw new AssertionError("halt did not occur");
            }
            SchemaDomainCoordinatorR926.ensureInfrastructure(db);
            db.execSQL("CREATE TABLE uf_scoped_columns(id INTEGER PRIMARY KEY)");
            int initialProbes=db.tableInfoQueries;
            try(LegacyDdlR31.Scope scope=new LegacyDdlR31.Scope(db,false)){
                LegacyDdlR31.exec(db,"ALTER TABLE uf_scoped_columns ADD COLUMN first_col INTEGER");
                boolean yielded=false;try{LegacyDdlR31.exec(db,"ALTER TABLE uf_scoped_columns ADD COLUMN next_col INTEGER");}catch(LegacyDdlR31.Pending expected){yielded=true;}
                need(yielded&&scope.statements==1,"next missing column still yields after one actual DDL");
            }
            need(db.tableInfoQueries-initialProbes==1,"one fresh scoped census replaces redundant whole-table PRAGMA probes");
            try(LegacyDdlR31.Scope scope=new LegacyDdlR31.Scope(db,false)){
                LegacyDdlR31.exec(db,"ALTER TABLE uf_scoped_columns ADD COLUMN first_col INTEGER");need(scope.statements==0,"existing column is initially verified from a fresh scope");
                int beforeChange=db.tableInfoQueries;
                db.execSQL("ALTER TABLE uf_scoped_columns DROP COLUMN first_col"); // explicit fixture-only same-scope schema mutation
                LegacyDdlR31.exec(db,"ALTER TABLE uf_scoped_columns ADD COLUMN first_col INTEGER");
                need(scope.statements==1&&db.tableInfoQueries>beforeChange,"schema_version change invalidates an in-scope column census before reuse");
            }
            db.execSQL("ALTER TABLE uf_scoped_columns DROP COLUMN first_col"); // modern native fixture only; never production retirement authority
            try(LegacyDdlR31.Scope scope=new LegacyDdlR31.Scope(db,true)){
                boolean pending=false;try{LegacyDdlR31.exec(db,"ALTER TABLE uf_scoped_columns ADD COLUMN first_col INTEGER");}catch(LegacyDdlR31.Pending expected){pending=true;}
                need(pending&&scope.statements==0,"a new read-only scope starts fresh and sees external column loss");
            }
            db.execSQL("CREATE TABLE uf_shadow(id INTEGER PRIMARY KEY)");
            try(LegacyDdlR31.Scope scope=new LegacyDdlR31.Scope(db,true)){
                db.execSQL("CREATE TEMP TABLE uf_shadow(id INTEGER PRIMARY KEY,kept INTEGER)");
                LegacyDdlR31.exec(db,"ALTER TABLE uf_shadow ADD COLUMN kept INTEGER");
                db.execSQL("DROP TABLE temp.uf_shadow");
                boolean pending=false;try{LegacyDdlR31.exec(db,"ALTER TABLE uf_shadow ADD COLUMN kept INTEGER");}catch(LegacyDdlR31.Pending expected){pending=true;}
                need(pending&&scope.statements==0,"TEMP shadow removal cannot reuse another schema's column census");
            }
            try(LegacyDdlR31.Scope scope=new LegacyDdlR31.Scope(db,true)){
                db.execSQL("ATTACH DATABASE ':memory:' AS auxiliary");db.execSQL("CREATE TABLE auxiliary.uf_attached(id INTEGER PRIMARY KEY,kept INTEGER)");
                LegacyDdlR31.exec(db,"ALTER TABLE uf_attached ADD COLUMN kept INTEGER");
                db.execSQL("DETACH DATABASE auxiliary");db.execSQL("ATTACH DATABASE ':memory:' AS auxiliary");db.execSQL("CREATE TABLE auxiliary.uf_attached(id INTEGER PRIMARY KEY)");
                boolean pending=false;try{LegacyDdlR31.exec(db,"ALTER TABLE uf_attached ADD COLUMN kept INTEGER");}catch(LegacyDdlR31.Pending expected){pending=true;}
                need(pending&&scope.statements==0,"detach and same-name attachment swap always use fresh column metadata");
            }finally{db.execSQL("DETACH DATABASE auxiliary");}
            // A swallowed/no-op ALTER must fail a real SQLite compiler check.
            // Unqualified double quotes would compile as a string literal.
            try(Cursor c=db.rawQuery("SELECT \"ghost_col\" FROM uf_scoped_columns LIMIT 0",null)){need(c.getColumnCount()==1,"DQS control demonstrates why a qualified column is required");}
            db.ignoredAlter="ALTER TABLE uf_scoped_columns ADD COLUMN ghost_col";
            try(LegacyDdlR31.Scope scope=new LegacyDdlR31.Scope(db,false)){
                boolean missing=false;try{LegacyDdlR31.exec(db,"ALTER TABLE uf_scoped_columns ADD COLUMN ghost_col INTEGER");}catch(android.database.sqlite.SQLiteException expected){missing=true;}
                need(missing,"qualified SQLite parse rejects an ALTER that did not actually create the column");
                boolean sticky=false;try{scope.check();}catch(RuntimeException expected){sticky=true;}
                need(sticky,"the failed actual postcondition cannot be swallowed by a helper");
            }finally{db.ignoredAlter="";}
            try(LegacyDdlR31.Scope scope=new LegacyDdlR31.Scope(db,false)){
                LegacyDdlR31.exec(db,"ALTER TABLE uf_scoped_columns ADD COLUMN ghost_col INTEGER");need(scope.statements==1,"a later real ALTER recovers without clearing durable evidence");
            }
            db.execSQL("CREATE TABLE protected_rows(id INTEGER PRIMARY KEY,value TEXT NOT NULL)");db.execSQL("INSERT INTO protected_rows VALUES(1,'ORIGINAL_KEEP')");
            for(String sql:new String[]{"CREATE TABLE IF NOT EXISTS uf_sample","ALTER TABLE uf_sample","CREATE INDEX IF NOT EXISTS uf_sample_value"}) {
                db.blocked=sql;boolean failed=false;
                for(int i=0;i<5;i++)try{advance(db,false);}catch(RuntimeException expected){failed=true;need(expected.getMessage().contains("INJECTED_"),"first actual error preserved: "+expected);break;}
                need(failed,"injected DDL must fail: "+sql);
                need(scalar(db,"SELECT count(*) FROM v57_schema_migration_journal_r926 WHERE step_id='UF_NATIVE_OBJECTS' AND status='COMPLETED'")==0,"failed CREATE/ALTER/INDEX cannot be completed");
                // Clearing the fault needs no journal deletion and no data reset.
                db.blocked="";
                if(sql.startsWith("CREATE TABLE"))advance(db,false);
                if(sql.startsWith("ALTER"))advance(db,false);
            }
            db.blocked="";finish(db);
            db.execSQL("CREATE INDEX idx_v26_oos_one_action_target ON protected_rows(value)");
            try(LegacyDdlR31.Scope audit=new LegacyDdlR31.Scope(db,true)){
                boolean pending=false;try{LegacyDdlR31.exec(db,"DROP INDEX IF EXISTS idx_v26_oos_one_action_target");}catch(LegacyDdlR31.Pending expected){pending=true;}
                need(pending&&audit.statements==0,"read-only audit cannot perform retirement");
            }
            try(LegacyDdlR31.Scope owner=new LegacyDdlR31.Scope(db,false)){
                LegacyDdlR31.exec(db,"DROP INDEX IF EXISTS idx_v26_oos_one_action_target");need(owner.statements==1,"retirement consumes the one actual DDL unit");
                boolean pending=false;try{LegacyDdlR31.exec(db,"CREATE INDEX uf_pending_after_retirement ON protected_rows(value)");}catch(LegacyDdlR31.Pending expected){pending=true;}
                need(pending,"retirement cannot share a slice with another DDL");
            }
            need(scalar(db,"SELECT count(*) FROM sqlite_master WHERE name='idx_v26_oos_one_action_target'")==0,"explicit old index retired");
            boolean refused=false;try{LegacyDdlR31.exec(db,"DROP INDEX IF EXISTS uf_sample_value");}catch(IllegalArgumentException expected){refused=true;}
            need(refused&&scalar(db,"SELECT count(*) FROM sqlite_master WHERE name='uf_sample_value'")==1,"unregistered DROP cannot remove a current index");
            String exactPredicate="CREATE INDEX IF NOT EXISTS uf_exact_predicate ON protected_rows(value) WHERE value='Ready if not exists; X'";
            db.execSQL(exactPredicate);
            LegacyDdlR31.exec(db,exactPredicate);need(scalar(db,"SELECT count(*) FROM sqlite_master WHERE name='uf_exact_predicate'")==1,"exact literal-bearing predicate remains accepted");
            for(String changed:new String[]{"ready if not exists; X","Ready if not exists;X","Ready if not exists X","Ready ; X"}){
                boolean drift=false;try{LegacyDdlR31.exec(db,exactPredicate.replace("Ready if not exists; X",changed));}catch(IllegalStateException expected){drift=expected.getMessage().startsWith("LEGACY_DDL_DEFINITION_DRIFT:");}
                need(drift,"literal case, whitespace, semicolon and IF NOT EXISTS text retain their SQL meaning: "+changed);
            }
            SchemaDomainCoordinatorR926.Contract scopedFingerprint=new SchemaDomainCoordinatorR926.Contract("UF_SCOPED_EXACT_FINGERPRINT",1,new String[]{"protected_rows","uf_exact_predicate"});
            try(LegacyDdlR31.Scope scope=new LegacyDdlR31.Scope(db,false)){
                int originalCatalog=db.catalogQueries;
                String before=SchemaDomainCoordinatorR926.fingerprint(db,scopedFingerprint);
                need(before.equals(SchemaDomainCoordinatorR926.fingerprint(db,scopedFingerprint))&&db.catalogQueries==originalCatalog+1,"exact fingerprint is reused only inside the same scope and live schema version");
                db.execSQL("DROP INDEX uf_exact_predicate"); // explicit native fixture drift, not production retirement permission
                db.execSQL(exactPredicate.replace("Ready if not exists; X","ready if not exists; X"));
                need(!before.equals(SchemaDomainCoordinatorR926.fingerprint(db,scopedFingerprint)),"same-scope same-name literal mutation never reads an old exact fingerprint");
                boolean mismatch=false;try{LegacyDdlR31.exec(db,exactPredicate);}catch(IllegalStateException expected){mismatch=expected.getMessage().startsWith("LEGACY_DDL_DEFINITION_DRIFT:");}
                need(mismatch,"actual catalog definition is still checked after an in-scope schema mutation");
            }
            for(boolean temporary:new boolean[]{true,false}){
                try(LegacyDdlR31.Scope scope=new LegacyDdlR31.Scope(db,true)){
                    if(temporary)db.execSQL("CREATE TEMP TABLE uf_cache_guard(id INTEGER)");else db.execSQL("ATTACH DATABASE ':memory:' AS auxiliary");
                    int probes=db.catalogQueries;SchemaDomainCoordinatorR926.fingerprint(db,scopedFingerprint);SchemaDomainCoordinatorR926.fingerprint(db,scopedFingerprint);
                    need(db.catalogQueries==probes+2,"TEMP/attached connections never reuse exact catalog fingerprints: "+temporary);
                }finally{if(temporary)db.execSQL("DROP TABLE temp.uf_cache_guard");else db.execSQL("DETACH DATABASE auxiliary");}
            }
            db.execSQL("DROP INDEX uf_exact_predicate");db.execSQL(exactPredicate);
            String escaped="CREATE INDEX IF NOT EXISTS uf_escaped_predicate ON protected_rows(value) WHERE value='Don''t Normalize'";
            db.execSQL(escaped);LegacyDdlR31.exec(db,escaped);need(scalar(db,"SELECT count(*) FROM sqlite_master WHERE name='uf_escaped_predicate'")==1,"escaped quotes keep the original predicate valid");
            boolean escapedDrift=false;try{LegacyDdlR31.exec(db,escaped.replace("Normalize","normalize"));}catch(IllegalStateException expected){escapedDrift=expected.getMessage().startsWith("LEGACY_DDL_DEFINITION_DRIFT:");}
            need(escapedDrift,"escaped-quote literal case drift cannot match an existing index");
            db.execSQL("INSERT INTO uf_sample(id,value) VALUES(1,'KEEP')");
            db.execSQL("DROP INDEX uf_sample_value"); // fixture-only simulated old swallowed index failure
            db.execSQL("INSERT OR REPLACE INTO v57_schema_migration_journal_r926(domain,revision,step_id,status) VALUES('LEGACY',2,'FINAL_VERIFY','FAILED')");
            need(!advance(db,false),"old COMPLETED must reopen its missing index unit");finish(db);
            need(scalar(db,"SELECT count(*) FROM sqlite_master WHERE name='uf_sample_value'")==1,"missing index repaired automatically");
            need(scalar(db,"SELECT epoch FROM uf_sample WHERE id=1")==7,"original default/value preserved");
            // A second additive unit is interrupted after real autocommit DDL,
            // before its journal can commit. Reopen and rerun with the same ID.
            db.execSQL("UPDATE v57_schema_migration_journal_r926 SET status='REPAIR_REQUIRED' WHERE step_id='UF_NATIVE_OBJECTS'");
            Process child=new ProcessBuilder(System.getProperty("java.home")+"/bin/java","-cp",System.getProperty("java.class.path"),StrictMigrationR31Host.class.getName(),"halt",path.getPath()).inheritIO().start();
            need(child.waitFor()==73,"real child process termination after committed DDL");
            need(scalar(db,"SELECT count(*) FROM v57_schema_migration_journal_r926 WHERE step_id='UF_KILL_AUTOCOMMIT' AND status='RUNNING'")==1,"death before completion leaves RUNNING journal");
            LinkedHashMap<String,Runnable> resume=new LinkedHashMap<>();resume.put("AUTOCOMMIT",()->LegacyDdlR31.exec(db,"CREATE TABLE IF NOT EXISTS killed_unit(id INTEGER PRIMARY KEY,value TEXT)"));
            for(int i=0;i<5;i++)if(LegacySchemaCoordinatorR926.advanceUnitsR31(db,"UF_KILL_",resume,false))break;
            need(scalar(db,"SELECT count(*) FROM v57_schema_migration_journal_r926 WHERE step_id='UF_KILL_AUTOCOMMIT' AND status='COMPLETED'")==1,"fresh process DDL resumes without clearing journal");finish(db);
            need(scalar(db,"SELECT count(*) FROM protected_rows WHERE value='ORIGINAL_KEEP'")==1,"protected rows survive kill/restart");
            need(scalar(db,"SELECT count(*) FROM uf_sample WHERE value='KEEP'")==1,"business row not duplicated after restart");
            // A key write must occur while the migration is genuinely pending.
            db.execSQL("DROP INDEX uf_sample_value");need(!advance(db,false),"migration pending before critical commit");
            db.execSQL("INSERT INTO protected_rows VALUES(2,'COMMITTED_WHILE_PENDING')");
            need(scalar(db,"SELECT count(*) FROM protected_rows WHERE id=2")==1,"during>0 actual commit between migration slices");finish(db);
            System.out.println("STRICT_MIGRATION_NATIVE_PASS assertions="+assertions+" during=1 realKillRestart=true");
        }
    }
}
