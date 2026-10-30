package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.TargetIssueResolver;
import java.util.*;

/** Production typed restore/root verification and projection receiver on independent real SQLite databases.
 * Android quiescence, connection reload and native report snapshots are separate device acceptance. */
public final class CutoverR29HostTest {
    static int checks;
    static void check(String name,boolean ok){System.out.println("R29_CUTOVER|"+name+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(name);checks++;}
    static SQLiteDatabase open()throws Exception{
        SQLiteDatabase db=EventBridgeR29HostTest.open(true);
        db.execSQL("CREATE TABLE r26_task(task_id TEXT PRIMARY KEY,lane TEXT NOT NULL,mechanism TEXT,status TEXT,cursor INTEGER DEFAULT 0)");
        db.execSQL("CREATE TABLE r26_evolution(attempt_id TEXT PRIMARY KEY,campaign_id TEXT,policy_hash TEXT,parent TEXT,operation TEXT,category TEXT,structure_hash TEXT,behavior_novel INTEGER DEFAULT 0,research_accepted INTEGER DEFAULT 0,prospective_qualified INTEGER DEFAULT 0,verified_improvement INTEGER DEFAULT 0)");
        return db;
    }
    static long n(SQLiteDatabase db,String sql){return EventBridgeR29HostTest.count(db,sql);}
    static String s(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):null;}}
    public static void main(String[]args)throws Exception{
        SQLiteDatabase source=open();
        source.execSQL("CREATE TABLE r29_transfer_fixture(id INTEGER PRIMARY KEY,t TEXT,r REAL,b BLOB,n TEXT)");
        source.execSQL("INSERT INTO r29_transfer_fixture VALUES(?,?,?, ?,NULL)",new Object[]{Long.MIN_VALUE,"汉字\n\u0000",.125,new byte[]{0,1,2,(byte)255}});
        source.execSQL("INSERT INTO r26_task(task_id,lane,status) VALUES('r27-test','R27','READY'),('legacy-test','P2','READY')");
        source.execSQL("INSERT INTO r26_evolution(attempt_id,policy_hash) VALUES('R27:MAIN:1','P'),('R26:LEGACY:1','L')");
        PolicySpecR27 spec=new PolicySpecR27(PolicyAstR27.constant(1),1,PolicySpecR27.Rule.ALWAYS,0,.2,1,false,.01,.01,null);String id=spec.policyHash();
        EventBridgeR29HostTest.tx(source,()->StoreR27.insertShadow(source,spec,1,"HOST","ABI",id,id),false);
        EventBridgeR29HostTest.draw(source,"202610300001");
        long ordinal=TargetIssueResolver.sequenceOrdinalSU1("202610300002"),now=System.currentTimeMillis();
        source.execSQL("UPDATE r27_live_shadow SET status='ACTIVE'");
        source.execSQL("UPDATE r27_eprocess SET last_ordinal=?,state='QUALIFIED'",new Object[]{ordinal-1});
        source.execSQL("INSERT INTO r27_certificate VALUES(?,'ACTIVE','HOST_SYNTHETIC_ONLY',?, ?,?)",new Object[]{id,StatsR27.sha256("fixture"),now,now+100000});
        source.execSQL("INSERT INTO r27_live_frozen(policy_hash,target_ordinal,target_issue,position,probabilities,n1,n2,n3,act,status,frozen_at,deadline_at) VALUES(?,?,'202610300002',1,?,1,2,3,1,'FROZEN',?,?)",new Object[]{id,ordinal,StoreR27.pack(new double[10]),now-10000,now+60000});
        source.execSQL("INSERT INTO r27_chart_cache VALUES('mainline','choice',?,?)",new Object[]{JsonR27.write(Collections.singletonMap("choice",id)),now});
        List<MigrationCopyR29.Table> tables=ResearchCutoverR29.tables(source);
        String before=ResearchCutoverR29.root(source,tables,Long.MAX_VALUE,()->false);
        SQLiteDatabase destination=new SQLiteDatabase(java.sql.DriverManager.getConnection("jdbc:sqlite::memory:"));
        for(MigrationCopyR29.Table table:tables)destination.execSQL(table.createSql);
        boolean killed=false;
        MigrationCopyR29.Table fixture=tables.stream().filter(t->t.name.equals("r29_transfer_fixture")).findFirst().orElseThrow();
        try{EventBridgeR29HostTest.tx(destination,()->ResearchCutoverR29.restorePage(destination,fixture,ResearchCutoverR29.page(source,fixture,null)),true);}catch(IllegalStateException expected){killed=true;}
        check("C1_RESTORE_PAGE_ROLLBACK_LEAVES_NO_HALF_ROWS",killed&&n(destination,"SELECT count(*) FROM r29_transfer_fixture")==0);
        for(MigrationCopyR29.Table table:tables){
            Long after=null;for(;;){List<MigrationCopyR29.Row> page=ResearchCutoverR29.page(source,table,after);if(page.isEmpty())break;EventBridgeR29HostTest.tx(destination,()->ResearchCutoverR29.restorePage(destination,table,page),false);after=page.get(page.size()-1).rowid;}
        }
        check("C2_TYPED_ROWS_AND_SOURCE_ROOT_MATCH",before.equals(ResearchCutoverR29.root(destination,tables,Long.MAX_VALUE,()->false)));
        check("C3_LEGACY_PARTITIONS_NOT_COPIED_AS_RESEARCH_OWNERS",n(destination,"SELECT count(*) FROM r26_task")==1&&n(destination,"SELECT count(*) FROM r26_evolution")==1);
        try(Cursor c=destination.rawQuery("SELECT id,t,r,b,n FROM r29_transfer_fixture",null)){
            check("C4_NEGATIVE_ROWID_TEXT_FLOAT_BLOB_NULL_PRESERVED",c.moveToFirst()&&c.getLong(0)==Long.MIN_VALUE&&"汉字\n\u0000".equals(c.getString(1))&&c.getDouble(2)==.125&&Arrays.equals(c.getBlob(3),new byte[]{0,1,2,(byte)255})&&c.isNull(4));
        }
        check("C5_ORIGINAL_SOURCE_UNCHANGED",before.equals(ResearchCutoverR29.root(source,tables,Long.MAX_VALUE,()->false))&&n(source,"SELECT count(*) FROM r26_task")==2);
        boolean rejected=false;try{ResearchCutoverR29.root(source,tables,Long.MAX_VALUE,()->true);}catch(IllegalStateException expected){rejected=true;}
        check("C6_CANCELLED_COPY_CANNOT_REACH_PUBLICATION",rejected);
        EventBridgeR29HostTest.tx(destination,()->RealtimeResearchProjectionR29.installResearch(destination),false);
        List<RealtimeResearchProjectionR29.Row> projected=RealtimeResearchProjectionR29.capture(destination);
        check("C7_EXISTING_CANDIDATE_PROJECTIONS_ARE_QUEUED",projected.size()>=5);
        source.execSQL("INSERT INTO r29_database_route VALUES(1,'ACTIVE',?,?,?,42,?)",new Object[]{ResearchRouteR29.FILE_NAME,StatsR27.sha256("schema"),before,now});
        check("C8_NO_HEARTBEAT_CANNOT_CREATE_INTENT",!RealtimeResearchProjectionR29.freshForIntent(source,id,ordinal,now));
        EventBridgeR29HostTest.tx(source,()->{for(RealtimeResearchProjectionR29.Row row:projected)RealtimeResearchProjectionR29.apply(source,row,42,now);},false);
        long count=n(source,"SELECT count(*) FROM r29_projection_applied");
        EventBridgeR29HostTest.tx(source,()->{for(RealtimeResearchProjectionR29.Row row:projected)RealtimeResearchProjectionR29.apply(source,row,42,now+1);},false);
        check("C9_ACK_LOSS_REDELIVERY_DOES_NOT_DOUBLE_PROJECT",count==n(source,"SELECT count(*) FROM r29_projection_applied"));
        source.execSQL("INSERT INTO r29_research_heartbeat VALUES(1,42,0,?)",new Object[]{now});
        check("C10_CURRENT_FROZEN_FRAME_AND_HEALTH_ACCEPTED_BY_TRANSPORT",RealtimeResearchProjectionR29.freshForIntent(source,id,ordinal,now));
        check("C11_EXPIRED_HEARTBEAT_FAILS_CLOSED",!RealtimeResearchProjectionR29.freshForIntent(source,id,ordinal,now+4000));
        destination.execSQL("UPDATE r27_certificate SET status='REVOKED_TEST' WHERE policy_hash=?",new Object[]{id});
        List<RealtimeResearchProjectionR29.Row> changed=RealtimeResearchProjectionR29.capture(destination);
        RealtimeResearchProjectionR29.Row revocation=changed.stream().filter(r->r.table.equals("r27_certificate")).findFirst().orElseThrow();
        EventBridgeR29HostTest.tx(source,()->RealtimeResearchProjectionR29.apply(source,revocation,42,now+2),false);
        RealtimeResearchProjectionR29.Row old=projected.stream().filter(r->r.table.equals("r27_certificate")).findFirst().orElseThrow();
        EventBridgeR29HostTest.tx(source,()->RealtimeResearchProjectionR29.apply(source,old,42,now+3),false);
        check("C12_STALE_PUBLICATION_CANNOT_UNDO_REVOCATION","REVOKED_TEST".equals(s(source,"SELECT status FROM r27_certificate WHERE policy_hash='"+id+"'")));
        destination.execSQL("UPDATE r27_live_frozen SET hit=1,settled_at=? WHERE policy_hash=?",new Object[]{now,id});
        RealtimeResearchProjectionR29.Row settlement=RealtimeResearchProjectionR29.capture(destination).stream().filter(r->r.table.equals("r27_live_frozen")).findFirst().orElseThrow();
        EventBridgeR29HostTest.tx(source,()->RealtimeResearchProjectionR29.apply(source,settlement,42,now+4),false);
        check("C13_SETTLEMENT_PROJECTS_WITHOUT_REWRITING_FREEZE",n(source,"SELECT hit FROM r27_live_frozen")==1&&n(source,"SELECT frozen_at FROM r27_live_frozen")==now-10000);
        EventBridgeR29HostTest.publish(source,"202610300002",false);
        check("C14_NEW_REALTIME_RESULT_BLOCKS_OLD_RESEARCH_FRAME",!RealtimeResearchProjectionR29.freshForIntent(source,id,ordinal,now+5));
        source.jdbc.close();destination.jdbc.close();System.out.println("R29_CUTOVER|ALL_PASS|checks="+checks+"|HOST_CORE_ONLY_ANDROID_HANDOFF_AND_REPORT_PENDING");
    }
}
