package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.FetchObservationR29;
import com.ethk3.novaonepro.Models;
import com.ethk3.novaonepro.TargetIssueResolver;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.DriverManager;

/** Exact production event/observation schema and canonical append on real SQLite. No device SLO is inferred. */
public final class EventSchemaR29HostTest {
    static int checks;
    static void check(String name,boolean ok){System.out.println("R29_EVENT_SCHEMA|"+name+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(name);checks++;}
    static final class CountingDatabase extends SQLiteDatabase {
        long ddl,smallCatalogReads;
        CountingDatabase(String path)throws Exception{super(DriverManager.getConnection("jdbc:sqlite:"+path));}
        @Override public void execSQL(String sql,Object[] args){if(sql.matches("(?s)\\s*(CREATE|ALTER|DROP)\\b.*"))ddl++;super.execSQL(sql,args);}
        @Override public Cursor rawQuery(String sql,String[] args){if(sql.contains("FROM main.sqlite_master WHERE name IN (")){smallCatalogReads++;if(args==null||args.length>7)throw new AssertionError("EVENT_CATALOG_NOT_BOUNDED");}return super.rawQuery(sql,args);}
        void reset(){ddl=smallCatalogReads=0;}
    }
    interface Work{void run()throws Exception;}
    static void tx(CountingDatabase db,Work work,boolean rollback)throws Exception{
        db.jdbc.setAutoCommit(false);try{work.run();if(rollback)db.jdbc.rollback();else db.jdbc.commit();}catch(Exception failure){db.jdbc.rollback();throw failure;}finally{db.jdbc.setAutoCommit(true);}
    }
    static long n(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static String s(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):null;}}
    static void draws(CountingDatabase db){db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER NOT NULL DEFAULT 0)");}
    static void draw(CountingDatabase db,String issue){db.execSQL("INSERT OR IGNORE INTO draws VALUES(?,?,?,?,?,?,?,?,?,?,?,0)",new Object[]{issue,1,2,3,4,5,6,7,8,9,10});}
    static void ensure(CountingDatabase db)throws Exception{tx(db,()->ResearchEventBridgeR29.ensureRealtime(db),false);}
    static boolean conflict(CountingDatabase db)throws Exception{try{ensure(db);return false;}catch(IllegalStateException failure){if(!failure.getMessage().startsWith("R29_REALTIME_SCHEMA_CONFLICT:"))throw failure;return true;}}
    public static void main(String[] args)throws Exception{
        long base=TargetIssueResolver.sequenceOrdinalSU1("202610300001");
        try(CountingDatabase db=new CountingDatabase(":memory:")){
            draws(db);db.reset();ensure(db);check("Q1_FIRST_INSTALL_ORIGINAL_SEVEN_OBJECTS",db.ddl==7);
            db.reset();long start=System.nanoTime();
            tx(db,()->{for(int i=0;i<1000;i++){String issue=TargetIssueResolver.fromSequenceOrdinalSU1(base+i);draw(db,issue);ResearchEventBridgeR29.appendCanonical(db,issue,true,100);ResearchEventBridgeR29.appendCanonical(db,issue,false,200);}},false);
            long wall=(System.nanoTime()-start)/1000000L;
            System.out.println("R29_EVENT_SCHEMA|CANONICAL_BATCH_COUNTERS|rows=1000|appendCalls=2000|ddlCalls="+db.ddl+"|boundedCatalogReads="+db.smallCatalogReads);
            check("Q2_1000_ROW_REPEATED_CANONICAL_BATCH_DDL_ZERO",db.ddl==0);
            check("Q3_ONE_FRESH_SEVEN_NAME_READ_PER_ENSURE",db.smallCatalogReads==2000);
            check("Q4_DUPLICATE_APPEND_RETAINS_FIRST_EVENT",n(db,"SELECT count(*) FROM r29_event_outbox")==1000&&n(db,"SELECT count(*) FROM r29_event_outbox WHERE committed_at=100 AND origin='NEW_CANONICAL_COMMIT' AND acked_at=0")==1000);
            boolean identities=true;
            try(Cursor c=db.rawQuery("SELECT event_id,issue,payload_json,payload_hash FROM r29_event_outbox",null)){while(c.moveToNext()){String digest=StatsR27.sha256(c.getString(2));identities&=digest.equals(c.getString(3))&&StatsR27.sha256("DRAW_COMMITTED|"+c.getString(1)+"|"+digest).equals(c.getString(0));}}
            check("Q5_EVENT_IDS_AND_PAYLOAD_HASHES_UNCHANGED",identities);
            Models.Draw observed=new Models.Draw();observed.issue=TargetIssueResolver.fromSequenceOrdinalSU1(base+1100);observed.numbers=new int[]{1,2,3,4,5,6,7,8,9,10};observed.openTime=12345;
            FetchObservationR29.validated(observed);tx(db,()->{draw(db,observed.issue);ResearchEventBridgeR29.appendCanonical(db,observed.issue,true,300);},false);
            long first=n(db,"SELECT first_validated_wall FROM r29_fetch_observation");
            check("Q6_VALIDATED_FIRST_SEEN_REMAINS_REAL_AND_SOURCE_TIME_UNKNOWN",first>0&&n(db,"SELECT count(*) FROM r29_fetch_observation WHERE first_validated_mono>0 AND length(clock_session)>0 AND provider_scheduled_open=12345 AND source_available_wall IS NULL AND commit_observed_wall IS NULL")==1);
            FetchObservationR29.committed(observed);FetchObservationR29.validated(observed);db.reset();
            tx(db,()->{for(int i=0;i<1000;i++)ResearchEventBridgeR29.appendCanonical(db,observed.issue,false,400+i);},false);
            System.out.println("R29_EVENT_SCHEMA|VALIDATED_LATEST_COUNTERS|appendCalls=1000|ddlCalls="+db.ddl+"|boundedCatalogReads="+db.smallCatalogReads);
            check("Q7_1000_VALIDATED_LATEST_REPEATS_DDL_ZERO",db.ddl==0&&db.smallCatalogReads==2000);
            check("Q8_FIRST_SEEN_AND_CALLBACK_COMMIT_REMAIN_IDEMPOTENT",n(db,"SELECT count(*) FROM r29_fetch_observation")==1&&n(db,"SELECT first_validated_wall FROM r29_fetch_observation")==first&&n(db,"SELECT count(*) FROM r29_fetch_observation WHERE commit_observed_wall>=first_validated_wall AND commit_observed_mono>=first_validated_mono")==1);
            check("Q9_UNVALIDATED_HISTORY_NEVER_BECOMES_OBSERVATION",n(db,"SELECT count(*) FROM r29_event_outbox")==1001&&n(db,"SELECT count(*) FROM r29_fetch_observation")==1);
            db.execSQL("DROP TRIGGER r29_event_payload_immutable");db.reset();ensure(db);check("Q10_DROPPED_GUARD_REPAIRED_WITH_ONE_DDL",db.ddl==1);
            boolean protectedEvent=false;try{db.execSQL("UPDATE r29_event_outbox SET payload_hash='bad'");}catch(RuntimeException failure){protectedEvent=true;}check("Q11_REINSTALLED_GUARD_STILL_BLOCKS_MUTATION",protectedEvent);
            String guard=s(db,"SELECT sql FROM sqlite_master WHERE name='r29_registration_authority_no_update'");
            db.execSQL("DROP TRIGGER r29_registration_authority_no_update");db.execSQL("CREATE TRIGGER r29_registration_authority_no_update BEFORE UPDATE ON r29_registration_authority BEGIN SELECT 1; END");db.reset();
            check("Q12_SAME_NAME_INEFFECTIVE_GUARD_FAILS_BEFORE_ANY_DDL",conflict(db)&&db.ddl==0);
            db.execSQL("DROP TRIGGER r29_registration_authority_no_update");db.execSQL(guard);
            db.execSQL("DROP INDEX r29_event_outbox_pending");db.execSQL("CREATE INDEX r29_event_outbox_pending ON r29_event_outbox(seq,acked_at)");db.reset();check("Q13_SAME_NAME_WRONG_INDEX_FAILS_CLOSED",conflict(db)&&db.ddl==0);
            db.execSQL("DROP INDEX r29_event_outbox_pending");ensure(db);
            tx(db,()->{db.execSQL("DROP TRIGGER r29_event_payload_immutable");ResearchEventBridgeR29.ensureRealtime(db);},true);db.reset();ensure(db);check("Q14_DDL_REPAIR_ROLLBACK_DOES_NOT_PUBLISH_FAKE_READY",db.ddl==0);
            db.execSQL("DROP TRIGGER r29_registration_authority_no_update");db.execSQL(guard.replace("R29_REGISTRATION_AUTHORITY_IMMUTABLE","R29_REGISTRATION_AUTHORITY_IMMUTABLE "));
            check("Q15_QUOTED_GUARD_TEXT_IS_NOT_WHITESPACE_NORMALIZED_AWAY",conflict(db));
            System.out.println("R29_EVENT_SCHEMA|BATCH_MEASUREMENT|canonicalRows=1000|appendCalls=2000|wallMs="+wall+"|HOST_ONLY_NOT_ANDROID_SLO");
        }
        try(CountingDatabase db=new CountingDatabase(":memory:")){
            tx(db,()->ResearchEventBridgeR29.ensureRealtime(db),true);check("Q16_FIRST_INSTALL_ROLLBACK_LEAVES_NO_EVENT_TABLE",n(db,"SELECT count(*) FROM sqlite_master WHERE name='r29_event_outbox'")==0);
            db.reset();ensure(db);check("Q17_RETRY_AFTER_COLD_INSTALL_ROLLBACK_REINSTALLS_ALL",db.ddl==7);
            long version=n(db,"PRAGMA schema_version");db.execSQL("DROP TABLE r29_fetch_observation");db.execSQL("CREATE TABLE r29_fetch_observation(issue TEXT)");db.execSQL("PRAGMA schema_version="+version);db.reset();
            check("Q18_SAME_SCHEMA_VERSION_WRONG_TABLE_IS_NOT_CACHED_READY",conflict(db)&&db.ddl==0);
            Models.Draw observed=new Models.Draw();observed.issue="202610300002";observed.numbers=new int[]{1,2,3,4,5,6,7,8,9,10};FetchObservationR29.validated(observed);
            boolean rejected=false;try{tx(db,()->FetchObservationR29.acceptedWithin(db,observed.issue,ResearchEventBridgeR29.Event.draw(0,observed.issue,observed.numbers,"VALIDATED_LATEST",0).hash),false);}catch(IllegalStateException failure){rejected=failure.getMessage().startsWith("R29_REALTIME_SCHEMA_CONFLICT:");}
            check("Q21_WRONG_OBSERVATION_SCHEMA_CANNOT_CREATE_TIMING_SAMPLE",rejected&&n(db,"SELECT count(*) FROM r29_fetch_observation")==0);
        }
        Path file=Files.createTempFile("r29-event-schema-",".db");
        try{
            try(CountingDatabase db=new CountingDatabase(file.toString())){ensure(db);}
            try(CountingDatabase reopened=new CountingDatabase(file.toString())){reopened.reset();ensure(reopened);check("Q19_REOPEN_FRESH_VERIFICATION_DDL_ZERO",reopened.ddl==0&&reopened.smallCatalogReads==1);reopened.execSQL("DROP TRIGGER r29_registration_authority_no_delete");}
            try(CountingDatabase reopened=new CountingDatabase(file.toString())){reopened.reset();ensure(reopened);check("Q20_REOPEN_DETECTS_AND_REPAIRS_MISSING_GUARD",reopened.ddl==1);}
        }finally{Files.deleteIfExists(file);}
        System.out.println("R29_EVENT_SCHEMA|ALL_PASS|checks="+checks+"|HOST_ONLY_FULL_APP_REVALIDATION_REQUIRED");
    }
}
