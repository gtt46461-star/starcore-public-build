package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.TargetIssueResolver;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.Collections;
import sun.misc.Unsafe;

/** Inject a committed next draw after preparation but before the actual writer transaction. */
public final class FreezeSourceR30HostTest {
    static String issue(int n) { return String.format(java.util.Locale.ROOT,"20261030%04d",n); }
    static void add(Connection c,int n) throws Exception {
        try(PreparedStatement p=c.prepareStatement("INSERT INTO draws(issue,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,dirty,open_time) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)")) {
            p.setString(1,issue(n));for(int k=1;k<=10;k++)p.setInt(k+1,k);p.setInt(12,0);p.setLong(13,System.currentTimeMillis());p.executeUpdate();
        }
    }
    public static void main(String[] args) throws Exception {
        Class.forName("org.sqlite.JDBC");
        try(Connection c=DriverManager.getConnection("jdbc:sqlite::memory:")) {
            try(Statement s=c.createStatement()) {
                s.execute("CREATE TABLE draws(issue TEXT PRIMARY KEY,n1 INT,n2 INT,n3 INT,n4 INT,n5 INT,n6 INT,n7 INT,n8 INT,n9 INT,n10 INT,dirty INT,open_time INT,fetch_time INT DEFAULT 0)");
                s.execute(HistoryImportR27.DDL);
                s.execute("CREATE TABLE r27_live_shadow(policy_hash TEXT,spec_json TEXT,temperature REAL,status TEXT,global_index INT)");
                s.execute("CREATE TABLE r27_live_frozen(policy_hash TEXT,target_ordinal INT,target_issue TEXT,position INT,probabilities BLOB,n1 INT,n2 INT,n3 INT,act INT,status TEXT,frozen_at INT,deadline_at INT,PRIMARY KEY(policy_hash,target_ordinal))");
                s.execute("CREATE TABLE r27_opportunity(target_ordinal INT PRIMARY KEY,target_issue TEXT,scheduled INT,frozen INT,acted INT,settled INT,deadline_missed INT,recorded_at INT)");
            }
            for(int n=1;n<=100;n++)add(c,n);
            if(args.length>0 && args[0].equals("schema")) {
                SQLiteDatabase schemaDb=new SQLiteDatabase(c);c.setAutoCommit(false);
                SchemaR27.ensure(schemaDb);long first=SchemaR27.liveSourceRevision(schemaDb);
                SchemaR27.ensure(schemaDb);long second=SchemaR27.liveSourceRevision(schemaDb);
                if(first!=0 || second!=0 || !SchemaR27.liveSourceTriggersValid(schemaDb))throw new AssertionError("SCHEMA_REOPEN_CHANGED_SOURCE");
                try(Statement st=c.createStatement();ResultSet r=st.executeQuery("SELECT COUNT(*) FROM draws")) {r.next();if(r.getInt(1)!=100)throw new AssertionError("SCHEMA_LOST_DRAWS");}
                c.commit();c.setAutoCommit(true);System.out.println("SCHEMA_ENSURE_TWICE_PRESERVES_EXISTING_DRAWS_AND_REVISION");return;
            }
            PolicySpecR27 spec=new PolicySpecR27(PolicyAstR27.leaf("freq",1,32),1,PolicySpecR27.Rule.ALWAYS,0,0,1,false,0.01,0.01,new PolicySpecR27.Meta("I01","TEST",Collections.emptyList(),0,1,"race"));
            try(PreparedStatement p=c.prepareStatement("INSERT INTO r27_live_shadow VALUES(?,?,1,'ACTIVE',1)")){p.setString(1,"fixture");p.setString(2,spec.fullJson());p.executeUpdate();}
            long open;try(Statement st=c.createStatement();ResultSet r=st.executeQuery("SELECT open_time FROM draws WHERE issue='"+issue(100)+"'")){r.next();open=r.getLong(1);}
            final boolean race=args.length==0, correction=args.length>0 && args[0].equals("correction");
            final boolean imported=args.length>0 && args[0].equals("import"), missing=args.length>0 && args[0].equals("missing");
            final boolean metadata=args.length>0 && args[0].equals("metadata");
            final boolean deleted=args.length>0 && args[0].equals("delete"), tampered=args.length>0 && args[0].equals("tamper");
            SQLiteDatabase db=new SQLiteDatabase(c);
            if(!missing) { c.setAutoCommit(false);SchemaR27.ensureLiveSourceRevision(db);c.commit();c.setAutoCommit(true); }
            Field f=Unsafe.class.getDeclaredField("theUnsafe");f.setAccessible(true);Unsafe u=(Unsafe)f.get(null);
            RuntimeR27 rt=(RuntimeR27)u.allocateInstance(RuntimeR27.class);
            ResearchPoolR27 pool=(ResearchPoolR27)u.allocateInstance(ResearchPoolR27.class);
            ResearchWriteQueueR27 writes=new ResearchWriteQueueR27((op,batch)->{
                try {
                    if(race && op.equals("LIVE_FREEZE"))add(c,101); // another producer committed before this transaction
                    if(correction && op.equals("LIVE_FREEZE"))try(Statement st=c.createStatement()){st.executeUpdate("UPDATE draws SET n1=2,n2=1 WHERE issue='"+issue(10)+"'");}
                    if(imported && op.equals("LIVE_FREEZE"))try(Statement st=c.createStatement()){st.executeUpdate("INSERT INTO r27_history_import VALUES('202610290001',1,'1,2,3,4,5,6,7,8,9,10','IMPORTED',NULL,'TEST',1)");}
                    if(metadata && op.equals("LIVE_FREEZE"))try(Statement st=c.createStatement()){st.executeUpdate("UPDATE draws SET fetch_time=100 WHERE issue='"+issue(10)+"'");}
                    if(deleted && op.equals("LIVE_FREEZE"))try(Statement st=c.createStatement()){st.executeUpdate("DELETE FROM draws WHERE issue='"+issue(10)+"'");}
                    if(tampered && op.equals("LIVE_FREEZE"))try(Statement st=c.createStatement()){st.execute("DROP TRIGGER r30_live_revision_draws_update");}
                    c.setAutoCommit(false);ResearchWriteQueueR27.execute(db,batch,op);c.commit();c.setAutoCommit(true);
                } catch(Exception e) {try{c.rollback();c.setAutoCommit(true);}catch(Exception ignored){}throw new IllegalStateException(e);}
            },()->false);
            u.putObject(pool,u.objectFieldOffset(ResearchPoolR27.class.getDeclaredField("writes")),writes);
            ResearchPoolR27.Worker worker=pool.new Worker(db,"race");
            long start=System.currentTimeMillis();
            LiveShadowsR27.freeze(rt,worker,issue(100),TargetIssueResolver.sequenceOrdinalSU1(issue(100)),open+60_000L,"('ACTIVE')",true);
            long end=System.currentTimeMillis();
            try(Statement s=c.createStatement();ResultSet r=s.executeQuery("SELECT COUNT(*) FROM r27_live_frozen WHERE status='FROZEN' AND target_issue='"+issue(101)+"'")) {
                r.next();int illegal=r.getInt(1);System.out.println("ALREADY_KNOWN_TARGET_FROZEN="+illegal);
                if((race||correction||imported||missing||deleted||tampered) && illegal!=0)throw new AssertionError("FREEZE_AFTER_SOURCE_CHANGED");
                if(!race && !correction && !imported && !missing && !deleted && !tampered && illegal!=1)throw new AssertionError("VALID_SOURCE_FREEZE_MISSING");
            }
            if(race||correction||imported||missing||deleted||tampered)try(Statement st=c.createStatement();ResultSet r=st.executeQuery("SELECT code,source FROM r30_pit_violation")) {
                if(!r.next() || !r.getString(1).startsWith("SOURCE_") || !"LIVE_WAIT".equals(r.getString(2)))throw new AssertionError("SOURCE_GAP_REASON_MISSING");
                System.out.println("RETRYABLE_SOURCE_GAP_RECORDED="+r.getString(1));
            }
            if(!race && !correction && !imported && !missing && !deleted && !tampered)try(Statement st=c.createStatement();ResultSet r=st.executeQuery("SELECT frozen_at,deadline_at FROM r27_live_frozen WHERE status='FROZEN'")) {
                if(!r.next())throw new AssertionError("VALID_FREEZE_MISSING");long at=r.getLong(1),deadline=r.getLong(2);
                if(at<start-10 || at>end+10 || deadline-at<5000)throw new AssertionError("FROZEN_CLOCK_NOT_REAL_WRITER_TIME");
                System.out.println("VALID_FROZEN_AT_REAL_WRITER_TIME_AND_ORIGINAL_MARGIN");
            }
        }
    }
}
