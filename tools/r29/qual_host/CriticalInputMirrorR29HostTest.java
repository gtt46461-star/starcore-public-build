package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DbHelper;
import com.ethk3.novaonepro.DrawDependencyInputCP12;
import java.sql.DriverManager;
import java.util.*;

/** Real SQLite triggers and independent transactions; no invented forecast or learning receipts. */
public final class CriticalInputMirrorR29HostTest {
    static int checks;
    static String SHADOW=DbHelper.TABLE_R35_SHADOW_OBSERVATION,OOS=DbHelper.TABLE_R35_OOS_LABEL,DEP=DrawDependencyInputCP12.TABLE;
    static void check(String name,boolean ok){System.out.println("R29_CRITICAL_MIRROR|"+name+"|"+(ok?"OK":"FAILED"));if(!ok)throw new AssertionError(name);checks++;}
    static long n(SQLiteDatabase db,String sql){return CriticalInputMirrorR29.scalar(db,sql);}
    static SQLiteDatabase db()throws Exception{SQLiteDatabase db=new SQLiteDatabase(DriverManager.getConnection("jdbc:sqlite::memory:"));db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY)");return db;}
    static void setup(SQLiteDatabase root)throws Exception{
        root.execSQL("CREATE TABLE "+SHADOW+"(target_issue TEXT PRIMARY KEY,pre_draw_frozen INTEGER NOT NULL,settled INTEGER NOT NULL,hit_label INTEGER NOT NULL,settled_at INTEGER NOT NULL)");
        root.execSQL("CREATE TABLE "+OOS+"(target_issue TEXT PRIMARY KEY,hit_label INTEGER NOT NULL)");root.execSQL("CREATE TABLE "+DEP+"(issue TEXT PRIMARY KEY,input_hash TEXT NOT NULL)");
        root.execSQL("CREATE TABLE r26_frozen(policy_hash TEXT NOT NULL,target_ordinal INTEGER NOT NULL,payload BLOB,PRIMARY KEY(policy_hash,target_ordinal))");
        root.execSQL("CREATE TABLE r26_incumbent_frozen(target_ordinal INTEGER PRIMARY KEY,payload BLOB)");
        root.execSQL("CREATE TABLE su2_live_decision(genome_key TEXT NOT NULL,sequence_ordinal INTEGER NOT NULL,hit INTEGER,PRIMARY KEY(genome_key,sequence_ordinal)) WITHOUT ROWID");
        root.execSQL("CREATE VIEW "+DrawDependencyInputCP12.SOURCE+" AS SELECT issue,'ACTUAL_ROOT_VIEW_SQL' AS origin FROM draws UNION ALL SELECT issue,'ACTUAL_ROOT_VIEW_SQL' FROM "+DEP+" d WHERE NOT EXISTS(SELECT 1 FROM draws x WHERE x.issue=d.issue)");
        EventBridgeR29HostTest.tx(root,()->{for(int i=0;i<130;i++)root.execSQL("INSERT INTO "+SHADOW+" VALUES(?,1,0,-1,0)",new Object[]{String.format(Locale.ROOT,"20261030%04d",i+1)});root.execSQL("INSERT INTO su2_live_decision VALUES('legacy',9,NULL)");CriticalInputMirrorR29.ensureRealtime(root);},false);
    }
    static boolean sync(SQLiteDatabase root,SQLiteDatabase target)throws Exception{CriticalInputMirrorR29.Step step=CriticalInputMirrorR29.capture(root,target);boolean[] ready={false};EventBridgeR29HostTest.tx(target,()->ready[0]=CriticalInputMirrorR29.apply(target,step),false);return ready[0];}
    static void bootstrap(SQLiteDatabase root,SQLiteDatabase target)throws Exception{int turns=0;while(!sync(root,target))if(++turns>100)throw new AssertionError("bootstrap did not finish");}
    static void tests()throws Exception{
        try(SQLiteDatabase root=db();SQLiteDatabase target=db()){
            setup(root);CriticalInputMirrorR29.Step first=CriticalInputMirrorR29.capture(root,target);boolean[] ready={true};
            EventBridgeR29HostTest.tx(target,()->ready[0]=CriticalInputMirrorR29.apply(target,first),false);
            check("I1_FIRST_PAGE_HAS_DURABLE_CURSOR_AND_IS_NOT_READY",!ready[0]&&"BOOTSTRAP".equals(CriticalInputMirrorR29.state(target).phase)&&n(root,"SELECT count(*) FROM "+SHADOW)==130);
            int maxPage=0,turns=0;while(!ready[0]){CriticalInputMirrorR29.Step step=CriticalInputMirrorR29.capture(root,target);maxPage=Math.max(maxPage,step.rows.size());EventBridgeR29HostTest.tx(target,()->ready[0]=CriticalInputMirrorR29.apply(target,step),false);if(++turns>100)throw new AssertionError("bootstrap failed");}
            check("I2_BOOTSTRAP_IS_BOUNDED_RESUMABLE_AND_COMPLETE",maxPage==128&&n(target,"SELECT count(*) FROM "+SHADOW)==130&&"READY".equals(CriticalInputMirrorR29.state(target).phase));
            check("I3_ORIGINAL_ROOT_SOURCE_VIEW_IS_REBUILT_IN_REPLICA",CriticalInputMirrorR29.sourceViewSql(root).equals(CriticalInputMirrorR29.sourceViewSql(target))&&n(target,"SELECT count(*) FROM su2_live_decision WHERE genome_key='legacy' AND sequence_ordinal=9")==1);
            boolean rejected=false;try{target.execSQL("UPDATE "+SHADOW+" SET hit_label=1");}catch(RuntimeException expected){rejected=expected.getMessage().contains("ROOT_AUTHORITY");}
            check("I4_LEGACY_WRITER_CANNOT_AUTHOR_A_ROOT_INPUT_REPLICA",rejected&&n(target,"SELECT count(*) FROM "+SHADOW+" WHERE hit_label=1")==0);
            long drawRows=n(root,"SELECT count(*) FROM draws");EventBridgeR29HostTest.tx(root,()->{root.execSQL("UPDATE "+SHADOW+" SET settled=1,hit_label=1,settled_at=123 WHERE target_issue='202610300001'");root.execSQL("INSERT INTO "+OOS+" VALUES('202610300001',1)");},false);
            bootstrap(root,target);
            check("I5_LATE_SETTLEMENT_WITHOUT_ANY_NEW_DRAW_REACHES_RESEARCH",drawRows==n(root,"SELECT count(*) FROM draws")&&n(target,"SELECT hit_label FROM "+SHADOW+" WHERE target_issue='202610300001'")==1&&n(target,"SELECT count(*) FROM "+OOS)==1);
            long applied=CriticalInputMirrorR29.state(target).seq;EventBridgeR29HostTest.tx(root,()->{CriticalInputMirrorR29.acknowledge(root,applied);CriticalInputMirrorR29.acknowledge(root,applied);},false);
            check("I6_DUPLICATE_ACK_IS_IDEMPOTENT_AND_COUNTS_STAY_EXACT",n(root,"SELECT count(*) FROM "+CriticalInputMirrorR29.CHANGE)==0&&n(root,"SELECT entries FROM "+CriticalInputMirrorR29.CLOCK)==0);
            EventBridgeR29HostTest.tx(root,()->{root.execSQL("INSERT INTO "+DEP+" VALUES('202609010001','original-input-hash')");},false);bootstrap(root,target);
            check("I7_EVICTED_INPUT_IS_VISIBLE_THROUGH_ORIGINAL_SOURCE_VIEW",n(target,"SELECT count(*) FROM "+DrawDependencyInputCP12.SOURCE+" WHERE issue='202609010001' AND origin='ACTUAL_ROOT_VIEW_SQL'")==1);
            EventBridgeR29HostTest.tx(root,()->root.execSQL("DELETE FROM "+DEP+" WHERE issue='202609010001'"),false);bootstrap(root,target);
            check("I8_SOURCE_DELETE_REMOVES_ONLY_THE_REPLICA_INPUT",n(target,"SELECT count(*) FROM "+DEP)==0&&n(root,"SELECT count(*) FROM "+SHADOW)==130);
            EventBridgeR29HostTest.tx(root,()->root.execSQL("UPDATE "+SHADOW+" SET settled=1,hit_label=0,settled_at=124 WHERE target_issue='202610300002'"),false);
            CriticalInputMirrorR29.Step interrupted=CriticalInputMirrorR29.capture(root,target);long prior=CriticalInputMirrorR29.state(target).seq;
            try{EventBridgeR29HostTest.tx(target,()->CriticalInputMirrorR29.apply(target,interrupted),true);}catch(IllegalStateException expected){}
            check("I9_KILLED_APPLY_ROLLS_BACK_ROWS_CURSOR_AND_WRITE_GUARD",CriticalInputMirrorR29.state(target).seq==prior&&n(target,"SELECT hit_label FROM "+SHADOW+" WHERE target_issue='202610300002'")==-1&&n(target,"SELECT active FROM "+CriticalInputMirrorR29.GUARD)==0);
            EventBridgeR29HostTest.tx(target,()->CriticalInputMirrorR29.apply(target,interrupted),false);
            check("I10_RETRY_AFTER_KILLED_APPLY_MIRRORS_ONCE_WITHOUT_NEW_EVIDENCE",n(target,"SELECT hit_label FROM "+SHADOW+" WHERE target_issue='202610300002'")==0&&n(target,"SELECT count(*) FROM "+SHADOW)==130);
            long sourceSeq=CriticalInputMirrorR29.sourceSequence(root);try{EventBridgeR29HostTest.tx(root,()->root.execSQL("INSERT INTO "+OOS+" VALUES('rollback',1)"),true);}catch(IllegalStateException expected){}
            check("I11_ROOT_TRANSACTION_ROLLBACK_ALSO_ROLLS_BACK_CHANGE_INDEX",CriticalInputMirrorR29.sourceSequence(root)==sourceSeq&&n(root,"SELECT count(*) FROM "+OOS+" WHERE target_issue='rollback'")==0);
            EventBridgeR29HostTest.tx(root,()->root.execSQL("UPDATE su2_live_decision SET genome_key='replacement',hit=1 WHERE genome_key='legacy'"),false);bootstrap(root,target);
            check("I12_WITHOUT_ROWID_PRIMARY_KEY_MOVE_REMOVES_OLD_AND_COPIES_NEW",n(target,"SELECT count(*) FROM su2_live_decision WHERE genome_key='legacy'")==0&&n(target,"SELECT hit FROM su2_live_decision WHERE genome_key='replacement'")==1);
            EventBridgeR29HostTest.tx(root,()->root.execSQL("INSERT INTO "+DEP+" VALUES('recycled-A','first')"),false);bootstrap(root,target);
            EventBridgeR29HostTest.tx(root,()->{root.execSQL("DELETE FROM "+DEP+" WHERE issue='recycled-A'");root.execSQL("INSERT INTO "+DEP+" VALUES('recycled-B','second')");for(int i=0;i<63;i++)root.execSQL("INSERT INTO "+DEP+" VALUES(?,'filler')",new Object[]{"filler-"+i});root.execSQL("INSERT INTO "+DEP+" VALUES('recycled-A','reinserted')");},false);bootstrap(root,target);
            check("I15_RECYCLED_SOURCE_ROWID_CANNOT_COLLIDE_ACROSS_CHANGE_PAGES",n(target,"SELECT count(*) FROM "+DEP)==65&&n(target,"SELECT count(*) FROM "+DEP+" WHERE issue='recycled-A' AND input_hash='reinserted'")==1);
            EventBridgeR29HostTest.tx(root,()->{for(int i=0;i<50;i++)root.execSQL("INSERT INTO r26_frozen VALUES(?,1,?)",new Object[]{"large-"+i,new byte[50000]});},false);
            boolean largeReady=sync(root,target);long firstLargePage=n(target,"SELECT count(*) FROM r26_frozen");bootstrap(root,target);
            check("I16_BYTE_BUDGET_SPLITS_LARGE_DELTA_WITHOUT_DROPPING_KEYS",!largeReady&&firstLargePage>0&&firstLargePage<50&&n(target,"SELECT count(*) FROM r26_frozen")==50);
            long oldCursor=CriticalInputMirrorR29.state(target).seq;EventBridgeR29HostTest.tx(root,()->{for(int i=0;i<CriticalInputMirrorR29.MAX_CHANGES+130;i++)root.execSQL("INSERT INTO "+OOS+" VALUES(?,0)",new Object[]{"overflow-"+i});},false);
            check("I13_CHANGE_INDEX_IS_BOUNDED_WITH_EXPLICIT_LOST_PREFIX_FENCE",n(root,"SELECT count(*) FROM "+CriticalInputMirrorR29.CHANGE)<=CriticalInputMirrorR29.MAX_CHANGES&&CriticalInputMirrorR29.floor(root)>oldCursor&&n(root,"SELECT entries FROM "+CriticalInputMirrorR29.CLOCK)==n(root,"SELECT count(*) FROM "+CriticalInputMirrorR29.CHANGE));
            CriticalInputMirrorR29.Step reset=CriticalInputMirrorR29.capture(root,target);boolean[] usable={true};EventBridgeR29HostTest.tx(target,()->usable[0]=CriticalInputMirrorR29.apply(target,reset),false);
            check("I14_OVERFLOW_FORCES_PAGED_REBUILD_AND_NEVER_DELETES_ROOT_RESULTS",reset.reset&&!usable[0]&&"BOOTSTRAP".equals(CriticalInputMirrorR29.state(target).phase)&&n(root,"SELECT count(*) FROM "+OOS)>CriticalInputMirrorR29.MAX_CHANGES);
        }
        try(SQLiteDatabase root=db();SQLiteDatabase target=db()){
            setup(root);EventBridgeR29HostTest.tx(root,()->root.execSQL("INSERT INTO "+OOS+" VALUES('',0)"),false);bootstrap(root,target);
            check("I17_EMPTY_TEXT_PK_IS_DISTINCT_FROM_UNSTARTED_BOOTSTRAP_CURSOR",n(target,"SELECT count(*) FROM "+OOS+" WHERE target_issue=''")==1&&"READY".equals(CriticalInputMirrorR29.state(target).phase));
            root.execSQL("PRAGMA query_only=1");boolean verifiedWithoutWrite=CriticalInputMirrorR29.installedRealtime(root);List<RuntimeException> deferred=new ArrayList<>();
            CriticalInputMirrorR29.cleanupAck(()->{root.beginTransactionNonExclusive();try{CriticalInputMirrorR29.acknowledge(root,CriticalInputMirrorR29.state(target).seq);root.setTransactionSuccessful();}finally{root.endTransaction();}},deferred::add);root.execSQL("PRAGMA query_only=0");
            check("I18_COLD_SCHEMA_VALIDATION_IS_READ_ONLY_AND_ACK_FAILURE_CANNOT_UNDO_RP_COMMIT",verifiedWithoutWrite&&deferred.size()==1&&"READY".equals(CriticalInputMirrorR29.state(target).phase));
            String trigger="r29_critical_track_"+SHADOW+"_UPDATE";root.execSQL("DROP TRIGGER "+MigrationCopyR29.id(trigger));boolean missingRejected=!CriticalInputMirrorR29.installedRealtime(root);EventBridgeR29HostTest.tx(root,()->CriticalInputMirrorR29.ensureRealtime(root),false);boolean recreated=CriticalInputMirrorR29.installedRealtime(root);
            root.execSQL("DROP TRIGGER "+MigrationCopyR29.id(trigger));root.execSQL("CREATE TRIGGER "+MigrationCopyR29.id(trigger)+" AFTER UPDATE ON "+SHADOW+" BEGIN SELECT 1; END");boolean bodyRejected=!CriticalInputMirrorR29.installedRealtime(root);EventBridgeR29HostTest.tx(root,()->CriticalInputMirrorR29.ensureRealtime(root),false);
            root.execSQL("ALTER TABLE "+SHADOW+" ADD COLUMN test_source_revision INTEGER DEFAULT 0");boolean schemaRejected=!CriticalInputMirrorR29.installedRealtime(root);EventBridgeR29HostTest.tx(root,()->CriticalInputMirrorR29.ensureRealtime(root),false);
            root.execSQL("DROP VIEW "+DrawDependencyInputCP12.SOURCE);root.execSQL("CREATE VIEW "+DrawDependencyInputCP12.SOURCE+" AS SELECT issue,'CHANGED_ROOT_VIEW' AS origin FROM draws");boolean viewRejected=!CriticalInputMirrorR29.installedRealtime(root);EventBridgeR29HostTest.tx(root,()->CriticalInputMirrorR29.ensureRealtime(root),false);
            check("I19_MISSING_OR_CHANGED_TRACKERS_SOURCE_SCHEMA_AND_VIEW_REQUIRE_REAL_REINSTALL",missingRejected&&recreated&&bodyRejected&&schemaRejected&&viewRejected&&CriticalInputMirrorR29.installedRealtime(root));
        }
    }
    static void catchupAck()throws Exception{
        try(SQLiteDatabase root=db();SQLiteDatabase target=db()){
            setup(root);bootstrap(root,target);long epoch=CriticalInputMirrorR29.sourceEpoch(root),schema=n(root,"PRAGMA schema_version");
            EventBridgeR29HostTest.tx(root,()->{for(int i=0;i<193;i++)root.execSQL("INSERT INTO "+OOS+" VALUES(?,1)",new Object[]{"multi-ack-"+i});},false);
            CriticalInputMirrorR29.Step first=CriticalInputMirrorR29.capture(root,target);EventBridgeR29HostTest.tx(target,()->CriticalInputMirrorR29.apply(target,first),false);EventBridgeR29HostTest.tx(root,()->CriticalInputMirrorR29.acknowledge(root,first.next.seq),false);
            CriticalInputMirrorR29.Step second=CriticalInputMirrorR29.capture(root,target);
            check("I20_COMMITTED_CATCHUP_ACK_CANNOT_RESET_BOOTSTRAP_TO_OLD_BASE",!second.reset&&"CATCHUP".equals(first.next.phase)&&first.next.seq>first.next.base&&CriticalInputMirrorR29.floor(root)==first.next.seq&&second.changes.size()==64);
            int pages=1;boolean complete=false;while(!complete){CriticalInputMirrorR29.Step step=CriticalInputMirrorR29.capture(root,target);long before=CriticalInputMirrorR29.state(target).seq;boolean[] done={false};EventBridgeR29HostTest.tx(target,()->done[0]=CriticalInputMirrorR29.apply(target,step),false);EventBridgeR29HostTest.tx(root,()->CriticalInputMirrorR29.acknowledge(root,step.next.seq),false);if(step.reset||step.next.seq<=before||++pages>5)throw new AssertionError("catchup did not durably advance after ACK");complete=done[0];}
            EventBridgeR29HostTest.tx(root,()->CriticalInputMirrorR29.ensureRealtime(root),false);
            check("I21_MULTIPAGE_CATCHUP_DURABLY_ADVANCES_WITH_REAL_ACK_AND_STABLE_SOURCE_EPOCH",pages==4&&n(target,"SELECT count(*) FROM "+OOS)==193&&CriticalInputMirrorR29.state(target).seq==CriticalInputMirrorR29.sourceSequence(root)&&"READY".equals(CriticalInputMirrorR29.state(target).phase)&&CriticalInputMirrorR29.sourceEpoch(root)==epoch&&n(root,"PRAGMA schema_version")==schema&&n(root,"SELECT count(*) FROM "+CriticalInputMirrorR29.CHANGE)==0);
        }
    }
    public static void main(String[]args)throws Exception{tests();catchupAck();System.out.println("R29_CRITICAL_MIRROR|ALL_PASS|checks="+checks+"|REAL_SQLITE_HOST_ONLY_ANDROID_COMBINED_ACCEPTANCE_PENDING");}
}
