package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.TargetIssueResolver;
import java.util.List;
import java.util.Map;

/** Real registration handshake over two SQLite DBs, stale draw mirror, cross-period arrival and killed transactions. */
public final class RegistrationR29HostTest {
    static int checks;
    static void check(String name,boolean ok){System.out.println("R29_REGISTRATION|"+name+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(name);checks++;}
    static long n(SQLiteDatabase db,String sql){return EventBridgeR29HostTest.count(db,sql);}
    static String s(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):null;}}
    public static void main(String[]args)throws Exception{
        SQLiteDatabase realtime=EventBridgeR29HostTest.open(false),research=EventBridgeR29HostTest.open(true);
        PolicySpecR27 spec=new PolicySpecR27(PolicyAstR27.op("zscore",PolicyAstR27.leaf("freq",50,0)),1,PolicySpecR27.Rule.QUANTILE,0,.2,1,false,.01,.01,null);
        String id=spec.policyHash();
        EventBridgeR29HostTest.draw(realtime,"202610300001");
        // The research DB intentionally has no current draws at all. A local mirror cannot assign the calendar.
        EventBridgeR29HostTest.tx(research,()->StoreR27.insertShadow(research,spec,1,"HOST_CANDIDATE","ABI",id,id),false);
        check("R1_CANDIDATE_PENDING_WITHOUT_LOCAL_CALENDAR",ExperimentRegistrationR29.PENDING.equals(s(research,"SELECT status FROM r27_live_shadow"))&&n(research,"SELECT count(*) FROM r29_qual_calendar")==0&&n(research,"SELECT global_index FROM r27_eprocess")==1);
        String hash=s(research,"SELECT spec_hash FROM r29_registration_request");
        ExperimentRegistrationR29.Request request=new ExperimentRegistrationR29.Request(id,1,hash);
        // A new draw arrives between research request and realtime registration. The stale mirror's next issue is past.
        EventBridgeR29HostTest.draw(realtime,"202610300002");
        boolean killed=false;try{EventBridgeR29HostTest.tx(realtime,()->ExperimentRegistrationR29.publish(realtime,request,1000),true);}catch(IllegalStateException expected){killed=true;}
        check("R2_AUTHORITY_AND_EVENT_ROLL_BACK_TOGETHER",killed&&n(realtime,"SELECT count(*) FROM r29_registration_authority")==0&&n(realtime,"SELECT count(*) FROM r29_event_outbox")==0);
        EventBridgeR29HostTest.tx(realtime,()->ExperimentRegistrationR29.publish(realtime,request,2000),false);
        long first=TargetIssueResolver.sequenceOrdinalSU1("202610300003");
        check("R3_REALTIME_WRITER_DETERMINES_FIRST_FUTURE",n(realtime,"SELECT first_ordinal FROM r29_registration_authority")==first);
        EventBridgeR29HostTest.draw(realtime,"202610300003");
        EventBridgeR29HostTest.tx(realtime,()->ExperimentRegistrationR29.publish(realtime,request,3000),false);
        List<ResearchEventBridgeR29.Event> events=ResearchEventBridgeR29.pending(realtime);
        check("R4_RETRY_AFTER_NEXT_DRAW_DOES_NOT_SHIFT_START",events.size()==1&&n(realtime,"SELECT first_ordinal FROM r29_registration_authority")==first&&n(realtime,"SELECT registered_at FROM r29_registration_authority")==2000);
        killed=false;try{EventBridgeR29HostTest.batch(research,ResearchEventBridgeR29.prepare(events,4000),true);}catch(IllegalStateException expected){killed=true;}
        check("R5_KILLED_ACTIVATION_ROLLS_BACK_ALL_STATE",killed&&n(research,"SELECT count(*) FROM r29_qual_calendar")==0&&n(research,"SELECT count(*) FROM r29_registration_receipt")==0&&n(research,"SELECT count(*) FROM r29_event_inbox")==0&&ExperimentRegistrationR29.PENDING.equals(s(research,"SELECT status FROM r27_live_shadow")));
        EventBridgeR29HostTest.batch(research,ResearchEventBridgeR29.prepare(events,5000),false);
        check("R6_ACTIVATION_CALENDAR_AND_INBOX_COMMIT_TOGETHER","ACTIVE".equals(s(research,"SELECT status FROM r27_live_shadow"))&&n(research,"SELECT first_ordinal FROM r29_qual_calendar")==first&&n(research,"SELECT count(*) FROM r29_registration_receipt")==1&&n(research,"SELECT count(*) FROM r29_event_inbox")==1);
        check("R7_REGISTRATION_NEVER_INVENTS_FREEZE_OR_TRAINING",n(research,"SELECT count(*) FROM r27_live_frozen")==0&&n(research,"SELECT count(*) FROM r29_learning_work")==0&&n(research,"SELECT count(*) FROM r29_learning_receipt")==0);
        EventBridgeR29HostTest.batch(research,ResearchEventBridgeR29.prepare(events,6000),false);
        EventBridgeR29HostTest.tx(research,()->StoreR27.insertShadow(research,spec,1,"RETRY","ABI",id,id),false);
        check("R8_ACK_LOSS_KEEPS_PERMANENT_INDEX_AND_ONE_RECEIPT",n(research,"SELECT count(*) FROM r29_registration_receipt")==1&&n(research,"SELECT global_index FROM r27_eprocess")==1&&n(research,"SELECT n FROM r27_eprocess")==0);
        boolean rejected=false;try{EventBridgeR29HostTest.tx(realtime,()->ExperimentRegistrationR29.publish(realtime,new ExperimentRegistrationR29.Request(id,1,StatsR27.sha256("other")),7000),false);}catch(IllegalStateException expected){rejected=true;}
        check("R9_CHANGED_REQUEST_CANNOT_REUSE_AUTHORITY",rejected&&n(realtime,"SELECT count(*) FROM r29_registration_authority")==1);
        @SuppressWarnings("unchecked") Map<String,Object> p=(Map<String,Object>)JsonR27.parse(events.get(0).payload);
        p.put("specHash",StatsR27.sha256("forged-spec"));String payload=JsonR27.write(p),badHash=StatsR27.sha256(payload),issue=events.get(0).issue;
        ResearchEventBridgeR29.Event forged=new ResearchEventBridgeR29.Event(999,StatsR27.sha256(ExperimentRegistrationR29.KIND+"|"+issue+"|"+badHash),issue,payload,badHash,"REALTIME_REGISTRATION_AUTHORITY",8000,ExperimentRegistrationR29.KIND);
        rejected=false;try{EventBridgeR29HostTest.batch(research,ResearchEventBridgeR29.prepare(java.util.Collections.singletonList(forged),8000),false);}catch(IllegalStateException expected){rejected=true;}
        check("R10_WRONG_SPEC_CANNOT_CHANGE_CALENDAR_OR_CURSOR",rejected&&n(research,"SELECT first_ordinal FROM r29_qual_calendar")==first&&n(research,"SELECT count(*) FROM r29_event_inbox")==1);
        String control=StatsR27.sha256("CONTROL|"+id);
        EventBridgeR29HostTest.tx(research,()->{StoreR27.insertShadow(research,spec,1,"CONTROL","ABI",control,id);ExperimentRegistrationR29.desiredStatus(research,control,"CONTROL_I00");},false);
        check("R11_CONTROL_CANNOT_BYPASS_REGISTRATION",ExperimentRegistrationR29.PENDING.equals(s(research,"SELECT status FROM r27_live_shadow WHERE policy_hash='"+control+"'")));
        String controlHash=s(research,"SELECT spec_hash FROM r29_registration_request WHERE experiment_id='"+control+"'");
        EventBridgeR29HostTest.tx(realtime,()->ExperimentRegistrationR29.publish(realtime,new ExperimentRegistrationR29.Request(control,2,controlHash),9000),false);
        EventBridgeR29HostTest.batch(research,ResearchEventBridgeR29.prepare(ResearchEventBridgeR29.pending(realtime),10000),false);
        check("R12_CONTROL_ACTIVATES_ONLY_WITH_REALTIME_RECEIPT","CONTROL_I00".equals(s(research,"SELECT status FROM r27_live_shadow WHERE policy_hash='"+control+"'"))&&n(research,"SELECT count(*) FROM r29_registration_receipt")==2);
        boolean immutable=false;try{realtime.execSQL("UPDATE r29_registration_authority SET first_ordinal=1");}catch(RuntimeException expected){immutable=true;}
        check("R13_AUTHORITY_CANNOT_BE_REWRITTEN",immutable);
        immutable=false;try{research.execSQL("DELETE FROM r29_registration_receipt");}catch(RuntimeException expected){immutable=true;}
        check("R14_REGISTRATION_EVIDENCE_CANNOT_BE_DELETED",immutable);
        realtime.jdbc.close();research.jdbc.close();System.out.println("R29_REGISTRATION|ALL_PASS|checks="+checks+"|HOST_ONLY_NOT_NATURAL_EVIDENCE");
    }
}
