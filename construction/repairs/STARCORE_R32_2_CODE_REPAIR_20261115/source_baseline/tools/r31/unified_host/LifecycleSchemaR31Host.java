package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.File;
import java.sql.*;
import java.util.*;

/** Actual migration owner/journal on native SQLite. The declarations below are
 * test storage, not synthetic app data and not evidence that the four Stores ran. */
public final class LifecycleSchemaR31Host {
    static int assertions;
    static void need(boolean value,String message){assertions++;if(!value)throw new AssertionError(message);}
    public static String[] fixture(){return new String[]{
        "CREATE TABLE IF NOT EXISTS r31_keep_owner_fixture(id INTEGER PRIMARY KEY,value TEXT NOT NULL DEFAULT 'Ready ; IF NOT EXISTS X')",
        "CREATE INDEX IF NOT EXISTS r31_keep_owner_index ON r31_keep_owner_fixture(value) WHERE value='Ready ; IF NOT EXISTS X'",
        "CREATE TRIGGER IF NOT EXISTS r31_keep_owner_guard BEFORE UPDATE ON r31_keep_owner_fixture WHEN OLD.value='Original owner' BEGIN SELECT RAISE(ABORT,'Owner Literal ; IF NOT EXISTS'); END"
    };}
    static class Db extends SQLiteDatabase {
        int schemaStatements;String blocked="";
        Db(Connection connection){super(connection);}
        @Override public void execSQL(String sql,Object[] args){
            if(sql.matches("(?is)^\\s*(CREATE|ALTER|DROP)\\b.*")){schemaStatements++;if(!blocked.isEmpty()&&sql.contains(blocked))throw new android.database.sqlite.SQLiteException("INJECTED_OBJECT_FAILURE");}
            super.execSQL(sql,args);
        }
    }
    static long scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){need(c.moveToFirst(),"scalar");return c.getLong(0);}}
    static LifecycleSchemaR31.Result unit(Db db)throws Exception{
        db.schemaStatements=0;db.beginTransaction();
        try{
            LifecycleSchemaR31.Result result=LifecycleSchemaR31.advance(db,LifecycleSchemaR31.definitions(fixture()),tx->{});
            need(db.schemaStatements<=1,"one actual DDL statement per committed unit");db.setTransactionSuccessful();return result;
        }finally{db.endTransaction();}
    }
    static void finish(Db db)throws Exception{for(int i=0;i<12;i++)if(unit(db).ready)return;throw new AssertionError("owner did not finish");}
    static Db open(File path)throws Exception{return new Db(DriverManager.getConnection("jdbc:sqlite:"+path));}
    static void setup(Db db){SchemaDomainCoordinatorR926.ensureInfrastructure(db);db.execSQL("CREATE TABLE protected_critical_fixture(id INTEGER PRIMARY KEY,value TEXT NOT NULL)");db.execSQL("INSERT INTO protected_critical_fixture VALUES(1,'Original owner')");}
    static void kill(File path,String point)throws Exception{
        Process process=new ProcessBuilder(System.getProperty("java.home")+"/bin/java","-cp",System.getProperty("java.class.path"),LifecycleSchemaR31Host.class.getName(),"halt",path.toString(),point).inheritIO().start();
        need(process.waitFor()==74,"real process died at "+point);
    }
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");File path=new File(args[1]);
        if(args[0].equals("halt")){
            try(Db db=open(path)){
                SchemaDomainCoordinatorR926.installCutPointsR31((domain,step,point)->{if(domain.equals(LifecycleSchemaR31.DOMAIN)&&point.equals(args[2]))Runtime.getRuntime().halt(74);});
                unit(db);if(args[2].equals("AFTER_COMMIT"))Runtime.getRuntime().halt(74);throw new AssertionError("kill boundary absent");
            }
        }
        int criticalCommits=0;
        try(Db db=open(path)){
            setup(db);need(!LifecycleSchemaR31.ready(db),"class presence and missing schema are not READY");
            for(int i=0;i<3;i++){
                LifecycleSchemaR31.Result result=unit(db);need(!result.ready&&result.committedUnit,"one missing object commits its unit before whole READY");
                // A separate native connection can really commit after the migration released its writer.
                try(Connection critical=DriverManager.getConnection("jdbc:sqlite:"+path);Statement s=critical.createStatement()){s.executeUpdate("INSERT INTO protected_critical_fixture VALUES("+(100+i)+",'COMMITTED_BETWEEN_UNITS')");criticalCommits++;}
                need(scalar(db,"SELECT count(*) FROM protected_critical_fixture")==2+i,"independent critical write visible while pending");
            }
            finish(db);need(LifecycleSchemaR31.ready(db),"real SQL+journal+stamp reach READY");
            db.execSQL("INSERT INTO r31_keep_owner_fixture VALUES(1,'Original owner')");
            SQLiteDatabase.trace.clear();need(LifecycleSchemaR31.ready(db),"business probe ready");
            need(SQLiteDatabase.trace.stream().noneMatch(x->x.contains("|execSQL|")),"ready probe never executes DDL or DML");
            // Simulate an old READY receipt whose historical index creation was swallowed.
            db.execSQL("DROP INDEX r31_keep_owner_index");need(!LifecycleSchemaR31.ready(db),"old READY cannot hide a missing object");
            db.blocked="r31_keep_owner_index";boolean failed=false;try{unit(db);}catch(android.database.sqlite.SQLiteException expected){failed=true;}
            need(failed,"actual CREATE failure is propagated");
            need(scalar(db,"SELECT count(*) FROM sqlite_master WHERE name='r31_keep_owner_index'")==0,"failed object was not created");
            db.blocked="";finish(db);need(LifecycleSchemaR31.ready(db),"retry resumes without journal deletion or database reset");
            need(scalar(db,"SELECT count(*) FROM r31_keep_owner_fixture WHERE value='Original owner'")==1,"existing evidence row retained");
            db.execSQL("DROP INDEX r31_keep_owner_index");db.execSQL("CREATE INDEX r31_keep_owner_index ON r31_keep_owner_fixture(value) WHERE value='ready ; IF NOT EXISTS X'");
            need(!LifecycleSchemaR31.ready(db),"literal case changes are actual SQL drift");
            boolean drift=false;try{unit(db);}catch(DatabaseCoordinator.SchemaPendingR31 expected){drift=true;}
            need(drift,"unknown definition cannot be repaired by blind stamp");
            db.execSQL("DROP INDEX r31_keep_owner_index");finish(db);
            db.execSQL("CREATE TRIGGER unknown_fixture_guard BEFORE DELETE ON r31_keep_owner_fixture BEGIN SELECT RAISE(ABORT,'foreign'); END");
            need(!LifecycleSchemaR31.ready(db),"unknown attachments block readiness");
            boolean unknown=false;try{unit(db);}catch(DatabaseCoordinator.SchemaPendingR31 expected){unknown=true;}
            need(unknown,"unknown attachment is preserved/refused rather than reauthenticated");
            db.execSQL("DROP TRIGGER unknown_fixture_guard");finish(db);
        }
        for(String point:new String[]{"STEP_STARTED","STEP_BODY_DONE","AFTER_COMMIT"}){
            File killed=new File(path.getParentFile(),"kill-"+point+".db");try(Db db=open(killed)){setup(db);}
            kill(killed,point);
            try(Db db=open(killed)){
                finish(db);need(LifecycleSchemaR31.ready(db),"fresh process resumes at "+point);
                need(scalar(db,"SELECT count(*) FROM protected_critical_fixture WHERE value='Original owner'")==1,"original row survives real process death");
                need(scalar(db,"SELECT count(*) FROM v57_schema_migration_journal_r926 WHERE domain='RESEARCH_R31_LIFECYCLE' AND status='COMPLETED'")==3,"exactly one durable receipt per object after restart");
            }
        }
        need(criticalCommits==3,"three actual independent commits");
        System.out.println("LIFECYCLE_SCHEMA_NATIVE_PASS assertions="+assertions+" maxDdlPerUnit=1 criticalCommits="+criticalCommits+" realKillBoundaries=3 readonlyProbe=true scope=OWNER_NOT_FOUR_STORES_OR_ANDROID");
    }
}
