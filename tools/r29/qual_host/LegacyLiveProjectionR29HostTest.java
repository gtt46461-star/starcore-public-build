package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import java.sql.DriverManager;

/** Independent real SQLite writers; model bytes never enter the realtime projection. Synthetic only. */
public final class LegacyLiveProjectionR29HostTest {
    static int checks;
    static void check(String name,boolean ok){System.out.println("R29_LEGACY_LIVE_PROJECTION|"+name+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(name);checks++;}
    static long n(SQLiteDatabase db,String sql){return EventBridgeR29HostTest.count(db,sql);}
    static LegacyLiveProjectionR29.Row row(long revision,String hash){LegacyLiveProjectionR29.Row r=new LegacyLiveProjectionR29.Row();r.policy="synthetic-policy";r.checkpoint="checkpoint-"+revision;r.hash=hash;r.through=29;r.status="ACTIVE";r.revision=revision;return r;}
    public static void main(String[] args)throws Exception{
        try(SQLiteDatabase rt=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite::memory:"));SQLiteDatabase research=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite::memory:"))){
            rt.execSQL(ResearchRouteR29.ROUTE_DDL);rt.execSQL("INSERT INTO r29_database_route VALUES(1,'ACTIVE','starcore-r29-research.db','schema','root',42,1)");LegacyLiveProjectionR29.ensureRealtime(rt);
            research.execSQL("CREATE TABLE r26_live_model(policy_hash TEXT PRIMARY KEY,checkpoint_hash TEXT,state_hash TEXT,through_ordinal INTEGER,status TEXT,state BLOB)");LegacyLiveProjectionR29.installResearch(research);
            boolean killed=false;try{EventBridgeR29HostTest.tx(research,()->research.execSQL("INSERT INTO r26_live_model VALUES('synthetic-policy','checkpoint-1','state-1',29,'ACTIVE',X'010203')"),true);}catch(IllegalStateException expected){killed=true;}
            check("L1_KILLED_MODEL_AND_PROJECTION_INTENT_ROLL_BACK_TOGETHER",killed&&n(research,"SELECT count(*) FROM r26_live_model")==0&&n(research,"SELECT count(*) FROM r29_legacy_live_pending")==0);
            EventBridgeR29HostTest.tx(research,()->research.execSQL("INSERT INTO r26_live_model VALUES('synthetic-policy','checkpoint-1','state-1',29,'ACTIVE',X'010203')"),false);
            check("L2_RESEARCH_COMMIT_DOES_NOT_IMPLICITLY_PUBLISH_REALTIME",n(research,"SELECT revision FROM r29_legacy_live_pending")==1&&n(rt,"SELECT count(*) FROM r29_legacy_live_projection")==0);
            LegacyLiveProjectionR29.Row first=row(1,"state-1");killed=false;try{EventBridgeR29HostTest.tx(rt,()->LegacyLiveProjectionR29.apply(rt,first,42,100),true);}catch(IllegalStateException expected){killed=true;}
            check("L3_KILLED_REALTIME_PROJECTION_IS_NOT_HALF_VISIBLE",killed&&n(rt,"SELECT count(*) FROM r29_legacy_live_projection")==0);
            EventBridgeR29HostTest.tx(rt,()->LegacyLiveProjectionR29.apply(rt,first,42,100),false);EventBridgeR29HostTest.tx(rt,()->LegacyLiveProjectionR29.apply(rt,first,42,101),false);
            check("L4_LOST_ACK_REDELIVERY_KEEPS_ONE_IDENTICAL_REVISION",n(rt,"SELECT count(*) FROM r29_legacy_live_projection")==1&&n(rt,"SELECT projected_at FROM r29_legacy_live_projection")==100);
            check("L5_FREEZE_REQUIRES_EXACT_CHECKPOINT_HASH_CURSOR_REVISION_AND_EPOCH",LegacyLiveProjectionR29.matches(rt,first.policy,first.checkpoint,first.hash,29,1,42)&&!LegacyLiveProjectionR29.matches(rt,first.policy,first.checkpoint,"wrong",29,1,42)&&!LegacyLiveProjectionR29.matches(rt,first.policy,first.checkpoint,first.hash,30,1,42)&&!LegacyLiveProjectionR29.matches(rt,first.policy,first.checkpoint,first.hash,29,2,42)&&!LegacyLiveProjectionR29.matches(rt,first.policy,first.checkpoint,first.hash,29,1,41));
            LegacyLiveProjectionR29.Row second=row(2,"state-2");EventBridgeR29HostTest.tx(rt,()->LegacyLiveProjectionR29.apply(rt,second,42,200),false);EventBridgeR29HostTest.tx(rt,()->LegacyLiveProjectionR29.apply(rt,first,42,201),false);
            check("L6_OLDER_MESSAGE_CANNOT_ROLL_BACK_NEWER_MODEL_VERSION",n(rt,"SELECT revision FROM r29_legacy_live_projection")==2&&!LegacyLiveProjectionR29.matches(rt,first.policy,first.checkpoint,first.hash,29,1,42));
            boolean rejected=false;try{EventBridgeR29HostTest.tx(rt,()->LegacyLiveProjectionR29.apply(rt,row(2,"different-state"),42,300),false);}catch(IllegalStateException expected){rejected=expected.getMessage().contains("SAME_REVISION_CONFLICT");}
            check("L7_SAME_REVISION_DIFFERENT_PAYLOAD_IS_A_REAL_CONFLICT",rejected);
            rt.jdbc.setAutoCommit(false);try{rt.execSQL("UPDATE r29_legacy_live_projection SET projected_at=projected_at");EventBridgeR29HostTest.tx(research,()->research.execSQL("UPDATE r26_live_model SET state=X'040506',checkpoint_hash='checkpoint-2',state_hash='state-2'"),false);}finally{rt.jdbc.rollback();rt.jdbc.setAutoCommit(true);}
            check("L8_REALTIME_WRITER_DOES_NOT_OWN_MODEL_STATE_COMMIT",n(research,"SELECT revision FROM r29_legacy_live_pending")==2&&n(research,"SELECT length(state) FROM r26_live_model")==3);
            LegacyLiveProjectionR29.Row blocked=row(3,"state-2");blocked.status="BLOCKED_INPUT_OR_STATE";EventBridgeR29HostTest.tx(rt,()->LegacyLiveProjectionR29.apply(rt,blocked,42,400),false);
            check("L9_BLOCKED_MODEL_CANNOT_FREEZE_FROM_OLDER_ACTIVE_PROJECTION",!LegacyLiveProjectionR29.matches(rt,second.policy,second.checkpoint,second.hash,29,2,42)&&!LegacyLiveProjectionR29.matches(rt,blocked.policy,blocked.checkpoint,blocked.hash,29,3,42));
            rejected=false;try{EventBridgeR29HostTest.tx(rt,()->LegacyLiveProjectionR29.apply(rt,row(4,"state-4"),41,500),false);}catch(IllegalStateException expected){rejected=expected.getMessage().contains("ROUTE_EPOCH_CHANGED");}
            check("L10_UNPUBLISHED_OR_STALE_ROUTE_EPOCH_CANNOT_PUBLISH",rejected&&n(rt,"SELECT revision FROM r29_legacy_live_projection")==3);
            check("L11_REALTIME_PROJECTION_HAS_NO_MODEL_WEIGHTS_OR_OPTIMIZER",n(rt,"SELECT count(*) FROM pragma_table_info('r29_legacy_live_projection') WHERE name IN ('state','weights','optimizer','artifact')")==0);
            check("L12_PRESERVED_ROOT_PROOFS_CANNOT_BYPASS_CURRENT_PROJECTION",MainlineAdapterSU1.projectionRequired(rt)&&!MainlineAdapterSU1.verifyFrozen(rt,null,null)&&CertificateSU2.read(rt,"preserved-old-genome",System.currentTimeMillis())==null);
            rejected=false;try{EventBridgeR29HostTest.tx(rt,()->CertificateSU2.preregister(rt,"old-policy",30),false);}catch(IllegalStateException expected){rejected=expected.getMessage().contains("RESEARCH_WRITER_REQUIRED");}
            check("L13_ROOT_CANNOT_SPEND_RESEARCH_REGISTRATION_BUDGET_AFTER_CUTOVER",rejected);
            research.execSQL("UPDATE r29_legacy_live_pending SET acked_revision=revision");
            long beforeReplace=n(research,"SELECT revision FROM r29_legacy_live_pending");
            EventBridgeR29HostTest.tx(research,()->research.execSQL("INSERT OR REPLACE INTO r26_live_model VALUES('synthetic-policy','checkpoint-3','state-3',30,'ACTIVE',X'070809')"),false);
            check("L15_OUTER_REPLACE_CANNOT_RESET_PENDING_REVISION_OR_ACK",n(research,"SELECT revision FROM r29_legacy_live_pending")==beforeReplace+1&&n(research,"SELECT acked_revision FROM r29_legacy_live_pending")==beforeReplace);
            research.execSQL(ResearchRouteR29.OWNER_DDL);
            check("L14_RESEARCH_PROOF_CHECKING_IS_NOT_CONFUSED_WITH_REALTIME_AUTHORITY",!MainlineAdapterSU1.projectionRequired(research));
        }
        System.out.println("R29_LEGACY_LIVE_PROJECTION|ALL_PASS|checks="+checks+"|HOST_PROTOCOL_NOT_ANDROID_LIFECYCLE");
    }
}
