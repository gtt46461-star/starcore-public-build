package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import java.util.*;

/** Dispatcher ownership fencing and legacy outputs use the production SQL/protocol on real SQLite. */
public final class OwnerR29HostTest {
    static int checks;
    static void check(String name,boolean ok){System.out.println("R29_OWNER|"+name+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(name);checks++;}
    static long n(SQLiteDatabase db,String sql){return EventBridgeR29HostTest.count(db,sql);}
    public static void main(String[] args)throws Exception{
        try(SQLiteDatabase rt=EventBridgeR29HostTest.open(true);SQLiteDatabase rs=EventBridgeR29HostTest.open(true)){
            EventBridgeR29HostTest.tx(rt,()->LegacyCapabilityAdapterR29.initialize(rt,"session1"),false);
            LegacyCapabilityAdapterR29.requireOwner(rt,"session1",false);
            boolean rejected=false;try{EventBridgeR29HostTest.tx(rt,()->LegacyCapabilityAdapterR29.publishWithin(rt,"session1"),false);}catch(IllegalStateException expected){rejected=true;}
            check("O1_UNPUBLISHED_DATABASE_CANNOT_HAND_OFF_DISPATCH",rejected);
            rt.execSQL("INSERT INTO r29_database_route VALUES(1,'ACTIVE',?,'schema','root',42,1)",new Object[]{ResearchRouteR29.FILE_NAME});
            rejected=false;try{EventBridgeR29HostTest.tx(rt,()->LegacyCapabilityAdapterR29.publishWithin(rt,"session1"),true);}catch(IllegalStateException expected){rejected=true;}
            LegacyCapabilityAdapterR29.requireOwner(rt,"session1",false);
            check("O2_KILLED_OWNER_PUBLICATION_RETURNS_TO_LEGACY",rejected);
            EventBridgeR29HostTest.tx(rt,()->LegacyCapabilityAdapterR29.publishWithin(rt,"session1"),false);
            LegacyCapabilityAdapterR29.requireOwner(rt,"session1",true);
            rejected=false;try{LegacyCapabilityAdapterR29.requireOwner(rt,"session1",false);}catch(IllegalStateException expected){rejected=true;}
            check("O3_OLD_DISPATCHER_CANNOT_CLAIM_AFTER_HANDOFF",rejected);
            rejected=false;try{LegacyCapabilityAdapterR29.requireOwner(rt,"stale",true);}catch(IllegalStateException expected){rejected=true;}
            check("O4_STALE_SESSION_CANNOT_CLAIM_POOL_WORK",rejected);
            rt.execSQL("UPDATE r29_database_route SET epoch=43");
            rejected=false;try{LegacyCapabilityAdapterR29.requireOwner(rt,"session1",true);}catch(IllegalStateException expected){rejected=true;}
            check("O5_OLD_EPOCH_CANNOT_CLAIM_POOL_WORK",rejected);
            EventBridgeR29HostTest.tx(rt,()->LegacyCapabilityAdapterR29.initialize(rt,"session2"),false);
            rejected=false;try{LegacyCapabilityAdapterR29.requireOwner(rt,"session1",true);}catch(IllegalStateException expected){rejected=true;}
            check("O6_RESTART_FENCES_PREVIOUS_POOL_SESSION",rejected);
            rejected=false;try{LegacyResearchTransferR29.lineage(rt,"child","parent",100);}catch(IllegalStateException expected){rejected=true;}
            check("O7_LEGACY_OUTPUT_REQUIRES_OWNER_TRANSACTION",rejected);
            try{EventBridgeR29HostTest.tx(rt,()->LegacyResearchTransferR29.lineage(rt,"child","parent",100),true);}catch(IllegalStateException expected){}
            check("O8_KILLED_LEGACY_OUTPUT_CANNOT_LEAK_EVENT",n(rt,"SELECT count(*) FROM r29_event_outbox")==0);
            EventBridgeR29HostTest.tx(rt,()->{LegacyResearchTransferR29.lineage(rt,"child","parent",100);LegacyResearchTransferR29.taskError(rt,"execution1","legacy-replay","HostFault","real failure","stack",100);},false);
            List<ResearchEventBridgeR29.Event> events=ResearchEventBridgeR29.pending(rt);
            check("O9_LEGACY_OUTPUTS_DO_NOT_WRITE_RESEARCH_TABLES_ON_REALTIME",events.size()==2&&n(rt,"SELECT count(*) FROM r27_lineage")==0&&n(rt,"SELECT count(*) FROM r27_task_error")==0);
            List<ResearchEventBridgeR29.Event> duplicated=new ArrayList<>(events);duplicated.addAll(events);
            EventBridgeR29HostTest.batch(rs,ResearchEventBridgeR29.prepare(duplicated,200),false);
            EventBridgeR29HostTest.batch(rs,ResearchEventBridgeR29.prepare(events,201),false);
            check("O10_DUPLICATE_BATCH_AND_ACK_LOSS_EXACTLY_ONCE",n(rs,"SELECT count(*) FROM r27_lineage")==1&&n(rs,"SELECT count(*) FROM r27_task_error")==1&&n(rs,"SELECT count(*) FROM r29_event_inbox")==2);
            check("O11_COMPATIBILITY_DELIVERY_DOES_NOT_INVENT_LEARNING",n(rs,"SELECT count(*) FROM r29_learning_work")==0&&n(rs,"SELECT count(*) FROM r29_learning_receipt")==0);
            check("O12_SCHEDULER_AND_CHECKPOINT_IDENTITY_STAY_WITH_LEGACY_OWNER",!MigrationCopyR29.ownedResearch("r29_scheduler_owner")&&!MigrationCopyR29.ownedResearch("r27_legacy_roster_alias"));
        }
        System.out.println("R29_OWNER|ALL_PASS|checks="+checks+"|HOST_ONLY_ANDROID_CONCURRENCY_PENDING");
    }
}
