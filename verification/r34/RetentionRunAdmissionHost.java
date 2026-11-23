package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DatabaseCoordinator;
import java.sql.Connection;
import java.sql.DriverManager;
import java.util.concurrent.atomic.AtomicInteger;

/** An unrelated SQLite writer holds the database during admission of the durable run. */
public final class RetentionRunAdmissionHost {
    private static final class Read extends SQLiteDatabase {
        int planReads;
        private final String last=SchemaUnitPlanR34.PROTECTED_TABLES_095.get(57);
        Read(Connection connection) { super(connection); }
        @Override public Cursor rawQuery(String sql,String[] args) {
            if (args!=null && ((args.length==1 && args[0].equals(last) && sql.startsWith("SELECT sql FROM main.sqlite_master"))
                    || (args.length>58 && sql.contains("FROM main.sqlite_master WHERE name IN (")))) planReads++;
            return super.rawQuery(sql,args);
        }
    }
    public static void main(String[] args) throws Exception {
        String jdbc="jdbc:sqlite:"+args[0];
        try(Read read=new Read(DriverManager.getConnection(jdbc));
            SQLiteDatabase write=new SQLiteDatabase(DriverManager.getConnection(jdbc));
            SQLiteDatabase blocking=new SQLiteDatabase(DriverManager.getConnection(jdbc))) {
            try(Cursor c=write.rawQuery("pragma busy_timeout=5",null)){c.moveToFirst();}
            AtomicInteger admission=new AtomicInteger();
            RetentionMigrationR34.Writer owner=(ctx,task)->{
                boolean busy=ctx.operation.equals("R34_RETENTION_RUN") && admission.getAndIncrement()<2;
                if(busy){blocking.beginTransaction();blocking.execSQL("INSERT INTO host_unrelated_fetch VALUES(123)");}
                write.beginTransaction();
                try{task.run(write);write.setTransactionSuccessful();}
                catch(RuntimeException error){
                    if(!busy)throw error;
                    Throwable cause=error;boolean confirmed=false;
                    while(cause!=null){String m=String.valueOf(cause.getMessage()).toLowerCase(java.util.Locale.ROOT);
                        confirmed|=m.contains("database is locked")||m.contains("sqlite_busy");cause=cause.getCause();}
                    if(!confirmed)throw error;
                    throw new DatabaseCoordinator.WriteRejectedR31("LOCK_BUSY",ctx.operation,"WAIT",DatabaseCoordinator.CommitOutcome.ROLLED_BACK,error);
                }catch(Exception checked){throw new IllegalStateException(checked);}
                finally{write.endTransaction();if(busy){blocking.setTransactionSuccessful();blocking.endTransaction();}}
            };
            RetentionMigrationR34.Session session=new RetentionMigrationR34.Session(read,owner,"reentry-proof-owner");
            boolean changeSchema=args.length>1 && args[1].equals("ddl");
            for(int i=0;i<2;i++){
                try{session.step();throw new AssertionError("ORIGINAL_LOCK_WAS_NOT_OBSERVED");}
                catch(DatabaseCoordinator.WriteRejectedR31 e){if(!ResearchPoolR27.retryRolledBackLock(e))throw e;}
                if(i==0&&changeSchema)blocking.execSQL("CREATE TABLE h114_unrelated_schema_change(id INTEGER)");
            }
            session.step();
            try(Cursor c=read.rawQuery("SELECT count(*) FROM r34_schema_unit_run WHERE owner_epoch='reentry-proof-owner'",null)) {
                if(!c.moveToFirst()||c.getInt(0)!=1)throw new AssertionError("RUN_NOT_ACQUIRED");
            }
            System.out.println("seedAttempts="+admission+" fullPlansRead="+read.planReads);
            if(read.planReads!=(changeSchema?2:1))throw new AssertionError("LOCK_RETRIES_REBUILD_ALL_58_PROTECTED_DEFINITIONS");
        }
    }
}
