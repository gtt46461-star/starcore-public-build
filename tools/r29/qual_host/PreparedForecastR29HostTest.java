package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.TimeSourceR811;
import java.util.*;

/** Actual two-database freeze/receipt SQL with source/clock races and killed transactions. Synthetic only. */
public final class PreparedForecastR29HostTest {
    static int checks;
    static void check(String name,boolean ok){System.out.println("R29_PREPARED|"+name+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(name);checks++;}
    static long n(SQLiteDatabase db,String sql){return EventBridgeR29HostTest.count(db,sql);}
    static SQLiteDatabase database()throws Exception{
        SQLiteDatabase db=EventBridgeR29HostTest.open(true);db.execSQL("ALTER TABLE draws ADD COLUMN open_time INTEGER NOT NULL DEFAULT 0");
        for(String[] c:SchemaR27.COLUMNS)if(c[0].equals("r27_live_frozen"))db.execSQL("ALTER TABLE "+c[0]+" ADD COLUMN "+c[1]+" "+c[2]);
        db.execSQL(PreparedForecastR29.FROZEN_IMMUTABLE);return db;
    }
    static void draw(SQLiteDatabase db,String issue,long open)throws Exception{
        EventBridgeR29HostTest.tx(db,()->{db.execSQL("INSERT INTO draws(issue,n1,n2,n3,n4,n5,n6,n7,n8,n9,n10,dirty,open_time) VALUES(?,1,2,3,4,5,6,7,8,9,10,0,?)",new Object[]{issue,open});ResearchEventBridgeR29.appendCanonical(db,issue,true,System.currentTimeMillis());},false);
    }
    static PreparedForecastR29.Frame frame(SQLiteDatabase realtime)throws Exception{
        long now=System.currentTimeMillis();draw(realtime,"202610300001",now);
        IssueCommitR29.Fence fence=IssueCommitR29.capture(realtime,"202610300001",com.ethk3.novaonepro.TargetIssueResolver.sequenceOrdinalSU1("202610300001"),now+60_000,"SYNTHETIC_SESSION");
        PreparedForecastR29.Frame frame=new PreparedForecastR29.Frame(fence,StatsR27.sha256("actual synthetic fixture input"),true);
        for(int i=0;i<3;i++){PreparedForecastR29.Forecast f=new PreparedForecastR29.Forecast();f.policy="synthetic-"+i;f.position=1;double[] p=new double[10];Arrays.fill(p,.1);f.probabilities=StoreR27.pack(p);f.n1=1;f.n2=2;f.n3=3;f.act=1;f.status="FROZEN";f.frozenAt=System.currentTimeMillis();frame.forecasts.add(f);}return frame;
    }
    static void apply(SQLiteDatabase db,ResearchWriteQueueR27.Batch batch,boolean kill)throws Exception{EventBridgeR29HostTest.batch(db,batch,kill);}
    public static void main(String[]args)throws Exception{
        try(SQLiteDatabase rt=database();SQLiteDatabase rs=database()){
            PreparedForecastR29.Frame f=frame(rt);boolean rejected=false;
            try{apply(rs,PreparedForecastR29.pending(f,rt),true);}catch(IllegalStateException expected){rejected=true;}
            check("P1_KILLED_FRAME_ROLLS_BACK_EVERY_MEMBER_AND_MANIFEST",rejected&&n(rs,"SELECT count(*) FROM r27_live_frozen")==0&&n(rs,"SELECT count(*) FROM r29_freeze_manifest")==0);
            apply(rs,PreparedForecastR29.pending(f,rt),false);
            check("P2_DURABLE_PREPARED_ROWS_ARE_NOT_FUTURE",n(rs,"SELECT count(*) FROM r27_live_frozen WHERE status='PREPARED'")==3&&n(rs,"SELECT count(*) FROM r27_live_frozen WHERE status='FROZEN'")==0&&n(rs,"SELECT count(*) FROM r29_learning_receipt")==0);
            rs.execSQL("CREATE TABLE r26_evolution(attempt_id TEXT PRIMARY KEY)");EventBridgeR29HostTest.tx(rs,()->RealtimeResearchProjectionR29.installResearch(rs),false);
            check("P3_UNRECEIPTED_FRAME_NEVER_PROJECTS_TO_REALTIME",RealtimeResearchProjectionR29.capture(rs).isEmpty());
            try{apply(rs,PreparedForecastR29.receipt(f,System.currentTimeMillis(),TimeSourceR811.monoNowMs(),true,"",rt),true);}catch(IllegalStateException expected){}
            check("P4_KILLED_RECEIPT_CANNOT_HALF_PROMOTE_A_FRAME",n(rs,"SELECT count(*) FROM r27_live_frozen WHERE status='FROZEN'")==0&&n(rs,"SELECT count(*) FROM r29_freeze_manifest WHERE phase='PREPARED_PENDING_COMMIT_RECEIPT'")==1);
            apply(rs,PreparedForecastR29.receipt(f,System.currentTimeMillis(),TimeSourceR811.monoNowMs(),true,"",rt),false);
            check("P5_RECEIPT_PROMOTES_ALL_MEMBERS_AND_OPPORTUNITY_AT_ONCE",n(rs,"SELECT count(*) FROM r27_live_frozen WHERE status='FROZEN' AND commit_verified_at>0")==3&&n(rs,"SELECT frozen FROM r27_opportunity")==3&&n(rs,"SELECT count(*) FROM r29_freeze_manifest WHERE phase='USABLE'")==1);
            rejected=false;try{rs.execSQL("UPDATE r29_freeze_manifest SET first_committed_wall=0");}catch(RuntimeException expected){rejected=true;}
            check("P6_USABLE_MANIFEST_CANNOT_BE_REWRITTEN",rejected);
            rejected=false;try{rs.execSQL("UPDATE r27_live_frozen SET probabilities=X'00'");}catch(RuntimeException expected){rejected=true;}
            check("P7_PUBLISHED_PREDICTION_PAYLOAD_IS_IMMUTABLE",rejected);
            rejected=false;try{apply(rs,PreparedForecastR29.pending(f,rt),false);}catch(RuntimeException expected){rejected=true;}
            check("P8_RETRY_CANNOT_DUPLICATE_FREEZE_OR_OPPORTUNITY",rejected&&n(rs,"SELECT count(*) FROM r27_live_frozen")==3&&n(rs,"SELECT count(*) FROM r27_opportunity")==1);
        }
        try(SQLiteDatabase rt=database();SQLiteDatabase rs=database()){
            PreparedForecastR29.Frame f=frame(rt);draw(rt,"202610300002",System.currentTimeMillis());boolean rejected=false;try{apply(rs,PreparedForecastR29.pending(f,rt),false);}catch(RuntimeException expected){rejected=IssueCommitR29.rejected(expected)!=null;}
            check("P9_RESULT_ARRIVAL_DURING_PREPARATION_BLOCKS_FUTURE",rejected&&n(rs,"SELECT count(*) FROM r27_live_frozen")==0);
        }
        try(SQLiteDatabase rt=database();SQLiteDatabase rs=database()){
            PreparedForecastR29.Frame f=frame(rt);apply(rs,PreparedForecastR29.pending(f,rt),false);boolean rejected=false;
            try{apply(rs,PreparedForecastR29.receipt(f,f.fence.deadlineWall-4000,f.fence.deadlineMono-4000,true,"",rt),false);}catch(RuntimeException expected){rejected=IssueCommitR29.rejected(expected)!=null;}
            apply(rs,PreparedForecastR29.receipt(f,f.fence.deadlineWall-4000,f.fence.deadlineMono-4000,false,"SIMULATED_SLOW_COMMIT",rt),false);
            check("P10_COMMIT_RETURNING_AFTER_MARGIN_CANNOT_BECOME_FUTURE",rejected&&n(rs,"SELECT count(*) FROM r27_live_frozen WHERE status='FROZEN'")==0&&n(rs,"SELECT count(*) FROM r27_live_frozen WHERE status='COMMIT_UNPROVEN'")==3);
        }
        try(SQLiteDatabase rt=database();SQLiteDatabase rs=database()){
            PreparedForecastR29.Frame f=frame(rt);apply(rs,PreparedForecastR29.pending(f,rt),false);apply(rs,PreparedForecastR29.recover(Collections.singletonList(f.hash())),false);
            check("P11_RESTART_WITHOUT_RECEIPT_PRESERVES_BUT_NEVER_BACKFILLS_FUTURE",n(rs,"SELECT count(*) FROM r27_live_frozen WHERE status='RECOVERY_UNPROVEN'")==3&&n(rs,"SELECT count(*) FROM r29_freeze_member")==3&&n(rs,"SELECT count(*) FROM r29_learning_receipt")==0);
        }
        try(SQLiteDatabase rt=database();SQLiteDatabase rs=database()){
            PreparedForecastR29.Frame f=frame(rt);apply(rs,PreparedForecastR29.pending(f,rt),false);
            // Only this disposable synthetic DB: simulate logical file corruption below the original SQL guard.
            rs.execSQL("DROP TRIGGER r27_live_frozen_immutable");rs.execSQL("UPDATE r27_live_frozen SET n1=10 WHERE policy_hash='synthetic-0'");boolean rejected=false;
            try{apply(rs,PreparedForecastR29.receipt(f,System.currentTimeMillis(),TimeSourceR811.monoNowMs(),true,"",rt),false);}catch(IllegalStateException expected){rejected=expected.getMessage().contains("PAYLOAD_CONFLICT");}
            check("P12_CHANGED_STORED_MEMBER_CANNOT_RECEIVE_VALID_PROOF",rejected&&n(rs,"SELECT count(*) FROM r27_live_frozen WHERE status='FROZEN'")==0);
            rejected=false;try{f.fence.clock(f.fence.preparedWall-10_000,f.fence.preparedMono+10);}catch(IssueCommitR29.Rejected expected){rejected=true;}
            check("P13_CLOCK_ROLLBACK_CANNOT_EXTEND_THE_DEADLINE",rejected);
        }
        System.out.println("R29_PREPARED|ALL_PASS|checks="+checks+"|HOST_PROTOCOL_ONLY_ANDROID_INTEGRATION_PENDING");
    }
}
