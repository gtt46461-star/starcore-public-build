package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import java.util.*;

/** Production terminal-outbox/mirror SQL, actual SQLite transactions; no natural-quality claim. */
public final class TerminalFeedbackR29HostTest {
    static int checks;
    static void check(String name,boolean ok){System.out.println("R29_TERMINAL|"+name+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(name);checks++;}
    static long n(SQLiteDatabase db,String sql){return EventBridgeR29HostTest.count(db,sql);}
    static void terminal(SQLiteDatabase db,String issue,String source,long at){
        String id=PolicyIdentityR26.sha256("R26_SETTLED|"+issue),payload=issue+",1,2,3,4,5,6,7,8,9,10";
        db.execSQL("INSERT INTO r26_feedback VALUES(?,?,?,?,?)",new Object[]{id,com.ethk3.novaonepro.TargetIssueResolver.sequenceOrdinalSU1(issue),source,payload,at});
        ResearchEventBridgeR29.appendTerminalFeedback(db,id);
    }
    public static void main(String[]args)throws Exception{
        String issue="202610300001",source=StatsR27.sha256("actual terminal fixture");long at=123456789;
        try(SQLiteDatabase rt=EventBridgeR29HostTest.open(true);SQLiteDatabase rs=EventBridgeR29HostTest.open(true)){
            rt.execSQL(RuntimeSchemaR26.FEEDBACK);rs.execSQL(RuntimeSchemaR26.FEEDBACK);
            try{EventBridgeR29HostTest.tx(rt,()->terminal(rt,issue,source,at),true);}catch(IllegalStateException expected){}
            check("T1_ROLLED_BACK_TERMINAL_CANNOT_LEAK_FEEDBACK_OR_OUTBOX",n(rt,"SELECT count(*) FROM r26_feedback")==0&&n(rt,"SELECT count(*) FROM r29_event_outbox")==0);
            EventBridgeR29HostTest.tx(rt,()->terminal(rt,issue,source,at),false);
            List<ResearchEventBridgeR29.Event> events=ResearchEventBridgeR29.pending(rt);
            check("T2_TERMINAL_AND_DELIVERY_INTENT_SHARE_REALTIME_COMMIT",events.size()==1&&events.get(0).kind.equals(ResearchEventBridgeR29.TERMINAL_FEEDBACK)&&n(rt,"SELECT count(*) FROM r26_feedback")==1);
            try{EventBridgeR29HostTest.batch(rs,ResearchEventBridgeR29.prepare(events,999),true);}catch(IllegalStateException expected){}
            check("T3_KILLED_MIRROR_TRANSACTION_ROLLS_BACK_INBOX_AND_STATE",n(rs,"SELECT count(*) FROM r26_feedback")==0&&n(rs,"SELECT count(*) FROM r29_event_inbox")==0);
            List<ResearchEventBridgeR29.Event> twice=new ArrayList<>(events);twice.addAll(events);
            EventBridgeR29HostTest.batch(rs,ResearchEventBridgeR29.prepare(twice,1000),false);EventBridgeR29HostTest.batch(rs,ResearchEventBridgeR29.prepare(events,1001),false);
            check("T4_ACK_LOSS_AND_DUPLICATE_DELIVERY_DO_NOT_DUPLICATE_TERMINAL",n(rs,"SELECT count(*) FROM r26_feedback")==1&&n(rs,"SELECT count(*) FROM r29_event_inbox")==1&&n(rt,"SELECT count(*) FROM r29_event_outbox WHERE acked_at=0")==1);
            check("T5_DELIVERED_TERMINAL_IS_NOT_INVENTED_LEARNING",n(rs,"SELECT count(*) FROM r29_learning_receipt")==0&&n(rs,"SELECT count(*) FROM r29_learning_work")==0);
            check("T6_ORIGINAL_SOURCE_HASH_AND_TERMINAL_CLOCK_ARE_PRESERVED",n(rs,"SELECT count(*) FROM r26_feedback WHERE source_hash='"+source+"' AND ready_at="+at)==1);
            ResearchEventBridgeR29.Event conflict=ResearchEventBridgeR29.terminalFeedback(PolicyIdentityR26.sha256("R26_SETTLED|"+issue),com.ethk3.novaonepro.TargetIssueResolver.sequenceOrdinalSU1(issue),StatsR27.sha256("changed terminal"),issue+",1,2,3,4,5,6,7,8,9,10",at);
            boolean rejected=false;try{EventBridgeR29HostTest.batch(rs,ResearchEventBridgeR29.prepare(Collections.singletonList(conflict),1002),false);}catch(IllegalStateException expected){rejected=expected.getMessage().contains("TERMINAL_FEEDBACK_CONFLICT");}
            check("T7_REVISED_TERMINAL_FAILS_WITHOUT_REWRITING_OR_ACKING",rejected&&n(rs,"SELECT count(*) FROM r29_event_inbox")==1&&n(rs,"SELECT count(*) FROM r26_feedback WHERE source_hash='"+source+"'")==1);
            EventBridgeR29HostTest.tx(rt,()->ResearchEventBridgeR29.acknowledge(rt,events,1003),false);
            check("T8_ACK_FOLLOWS_THE_COMPLETE_MIRROR_COMMIT",n(rt,"SELECT count(*) FROM r29_event_outbox WHERE acked_at>0")==1);
            rejected=false;try{ResearchEventBridgeR29.terminalFeedback("wrong",com.ethk3.novaonepro.TargetIssueResolver.sequenceOrdinalSU1(issue),source,issue+",1,2,3,4,5,6,7,8,9,10",at);}catch(IllegalArgumentException expected){rejected=true;}
            check("T9_TERMINAL_SOURCE_IDENTITY_CANNOT_BE_RENAMED",rejected);
        }
        System.out.println("R29_TERMINAL|ALL_PASS|checks="+checks+"|HOST_ONLY_ANDROID_MIGRATION_PENDING");
    }
}
