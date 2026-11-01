package com.ethk3.novaonepro.strategyv2;
import android.database.sqlite.SQLiteDatabase;
import android.database.Cursor;
import java.sql.*;
import java.util.*;
public final class RepairTimelineR30HostTest {
    interface Work{void run()throws Exception;}
    static void need(boolean value,String reason){if(!value)throw new AssertionError(reason);}
    static void tx(SQLiteDatabase db,Work work)throws Exception{db.jdbc.setAutoCommit(false);try{work.run();db.jdbc.commit();}catch(Exception|Error failure){db.jdbc.rollback();throw failure;}finally{db.jdbc.setAutoCommit(true);}}
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){need(c.moveToFirst(),sql);return c.getLong(0);}}
    static void mark(SQLiteDatabase db,String trace,String role,IssueTimelineR30.Stage stage,long mono){IssueTimelineWriterR30.markEventAt(db,"SYNTHETIC_ISSUE",IssueTimelineR30.EventRole.valueOf(role),IssueTimelineR30.EventKind.DRAW,trace,null,null,"SYNTHETIC_EXACT_DRAW_HASH",stage,1000+mono,mono,"SYNTHETIC_BOOT_A");}
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");try(Connection connection=DriverManager.getConnection("jdbc:sqlite::memory:")){
            SQLiteDatabase db=new SQLiteDatabase(connection);db.execSQL(IssueTimelineR30.DDL);db.execSQL(IssueTimelineR30.EVENT_DDL);db.execSQL(IssueTimelineR30.EVENT_INDEX);db.execSQL(IssueTimelineR30.EVENT_ACTIVE_INDEX);
            db.execSQL(IssueTimelineR30.SQL_ENSURE,new Object[]{"SYNTHETIC_ISSUE","HISTORICAL_UNTYPED",1});
            tx(db,()->{mark(db,"RAW_REQUEST_A","LIVE",IssueTimelineR30.Stage.FIRST_OBSERVED,10);mark(db,"RAW_REQUEST_A","LIVE",IssueTimelineR30.Stage.DRAW_COMMITTED,20);mark(db,"RAW_REQUEST_A","LIVE",IssueTimelineR30.Stage.SETTLED,25);mark(db,"RAW_REQUEST_B","BACKFILL",IssueTimelineR30.Stage.FIRST_OBSERVED,1);});
            need(n(db,"SELECT count(*) FROM "+IssueTimelineR30.EVENT_TABLE)==2,"same issue distinct role/instance retained");need(n(db,"SELECT count(*) FROM r30_issue_timeline WHERE boot_id='HISTORICAL_UNTYPED' AND mono_first_observed IS NULL")==1,"legacy evidence not rewritten");
            tx(db,()->mark(db,"RAW_REQUEST_A","LIVE",IssueTimelineR30.Stage.FIRST_OBSERVED,999));need(n(db,"SELECT mono_first_observed FROM "+IssueTimelineR30.EVENT_TABLE+" WHERE event_role='LIVE'")==10,"duplicate cannot replace original clock");
            String id=IssueTimelineWriterR30.eventId("SYNTHETIC_ISSUE",IssueTimelineR30.EventKind.DRAW,"RAW_REQUEST_A","SYNTHETIC_EXACT_DRAW_HASH");List<IssueTimelineWriterR30.Committed> receipts=Collections.singletonList(new IssueTimelineWriterR30.Committed(id,IssueTimelineR30.Stage.SETTLED,1030,30,"SYNTHETIC_BOOT_A"));
            try{tx(db,()->{IssueTimelineWriterR30.flushCommittedWithin(db,receipts);throw new IllegalStateException("SYNTHETIC_ROLLBACK_POSTCOMMIT_OBSERVATION_FLUSH");});throw new AssertionError("fault missing");}catch(IllegalStateException expected){}
            need(n(db,"SELECT count(*) FROM "+IssueTimelineR30.EVENT_TABLE+" WHERE durable_mono IS NOT NULL")==0,"failed observation flush publishes nothing");tx(db,()->IssueTimelineWriterR30.flushCommittedWithin(db,receipts));need(n(db,"SELECT durable_mono FROM "+IssueTimelineR30.EVENT_TABLE+" WHERE event_role='LIVE'")==30,"exact postcommit observation after retry");
            List<IssueTimelineWriterR30.Committed> later=Collections.singletonList(new IssueTimelineWriterR30.Committed(id,IssueTimelineR30.Stage.SETTLED,9999,9999,"OTHER_BOOT"));tx(db,()->IssueTimelineWriterR30.flushCommittedWithin(db,later));need(n(db,"SELECT durable_mono FROM "+IssueTimelineR30.EVENT_TABLE+" WHERE event_role='LIVE'")==30,"first successful observation immutable");
            tx(db,()->mark(db,"RAW_REQUEST_A","REPLAY",IssueTimelineR30.Stage.VALIDATED,20));need(n(db,"SELECT count(*) FROM "+IssueTimelineR30.EVENT_TABLE+" WHERE outcome='EVENT_IDENTITY_CONFLICT'")==1,"role mismatch isolated");
            try{tx(db,()->{mark(db,"ROLLBACK_REQUEST","LIVE",IssueTimelineR30.Stage.FIRST_OBSERVED,1);throw new IllegalStateException("SYNTHETIC_CORE_ROLLBACK");});throw new AssertionError("fault missing");}catch(IllegalStateException expected){}need(n(db,"SELECT count(*) FROM "+IssueTimelineR30.EVENT_TABLE)==2,"failed owner does not leave timeline stage");
            System.out.println("REPAIR_TIMELINE_NATIVE_SQLITE_HOST_PASS exact_request_and_role=1 historical_unchanged=1 first_clock_immutable=1 postcommit_flush_rollback_retry=1 no_rewrite_on_retry=1 identity_conflict_isolated=1 core_rollback_atomic=1 scope=HOST_NOT_Android_or_phone_SLO");
        }
    }
}
