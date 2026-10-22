package com.ethk3.novaonepro;
import android.database.sqlite.SQLiteDatabase;
public class R925SqlHarness {
    static void check(boolean v,String n){if(!v)throw new AssertionError(n);System.out.println(n+"=PASS");}
    public static void main(String[] args)throws Exception{
        SQLiteDatabase db=new SQLiteDatabase(args[0]);DbStore store=new DbStore(db);
        try{
            SchemaRevisionR925.ensureTables(db);SchemaRevisionR925.ensureTables(db);
            String criticalBefore=SchemaDomainCoordinatorR926.fingerprint(db,SchemaDomainCoordinatorR926.criticalContract());
            db.execSQL("CREATE TABLE injected(x INTEGER)");
            check(criticalBefore.equals(SchemaDomainCoordinatorR926.fingerprint(db,SchemaDomainCoordinatorR926.criticalContract())),"DOMAIN_FINGERPRINT_IGNORES_UNOWNED_SCHEMA");
            db.execSQL("DROP TABLE injected");
            check(criticalBefore.equals(SchemaDomainCoordinatorR926.fingerprint(db,SchemaDomainCoordinatorR926.criticalContract())),"DOMAIN_FINGERPRINT_DETERMINISTIC");
            LifecycleAuditR925.record(db,"a","PRUNED_EVIDENCE_R912","BRIER_UNUSABLE","old-metrics","202609060001",200,.9);
            LifecycleAuditR925.record(db,"a","PRUNED_EVIDENCE_R912","SECOND_REASON","new-metrics","202609060002",201,.9);
            check(LifecycleAuditR925.report(store).contains("first_prune_reason=BRIER_UNUSABLE"),"FIRST_PRUNE_REASON_IMMUTABLE");
            db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,fetch_time INTEGER,open_time INTEGER,source_line TEXT,dirty INTEGER,dirty_reason TEXT)");
            db.execSQL("CREATE TABLE v57_shadow_observation_r35(target_issue TEXT PRIMARY KEY,position INTEGER,n1 INTEGER,n2 INTEGER,n3 INTEGER,decision_action TEXT,pre_draw_frozen INTEGER)");
            db.execSQL("CREATE TABLE v57_oos_label_r35(target_issue TEXT PRIMARY KEY,hit_label INTEGER,strict_oos INTEGER)");
            db.execSQL("CREATE TABLE v57_analysis_request_r36(target_issue TEXT PRIMARY KEY,status TEXT,request_at INTEGER,started_at INTEGER,finished_at INTEGER,error_code TEXT)");
            db.execSQL("CREATE TABLE v57_issue_closure_r906(target_issue TEXT PRIMARY KEY,core_status TEXT,hit_label INTEGER)");
            db.execSQL("CREATE TABLE v57_deferred_evidence_job(target_issue TEXT PRIMARY KEY,status TEXT,core_materialized INTEGER,next_action_ordinal INTEGER,created_at INTEGER,completed_at INTEGER)");
            db.execSQL("INSERT INTO draws VALUES('202609060001',150,160,'fixture',0,'')");
            db.execSQL("INSERT INTO v57_shadow_observation_r35 VALUES('202609060001',1,1,2,3,'HOLD',1)");
            db.execSQL("INSERT INTO v57_oos_label_r35 VALUES('202609060001',1,1)");
            db.execSQL("INSERT INTO v57_shadow_observation_r35 VALUES('202609060002',1,1,2,3,'HOLD',1)");
            RuntimeEventLedgerR926.ensureSchema(db);
            db.execSQL("INSERT INTO v57_issue_projection_r926(game_code,target_issue,current_state,state_rank,epoch_id,attempt_id,last_event_id,event_watermark,request_seen,accepted_seen,rejected_seen,queued_seen,started_seen,frozen_seen,canonical_seen,settled_seen,full_seen,learned_seen,recovery_seen,last_error_code,as_of) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?,?)",
                    new Object[]{ReleaseManifestR926.GAME_CODE,"202609060002","CANONICAL_COMMITTED",60,"e","a","event",1,1,1,0,0,1,1,1,0,0,0,0,"",200});
            db.execSQL("INSERT INTO v57_runtime_epoch_r925 VALUES(?,?,?,?,?)",new Object[]{"release-"+BuildConfig.VERSION_CODE,"RELEASE",100,"202609050999",BuildConfig.VERSION_CODE});
            check(RuntimeObservabilityR925.pipeline(store,20,true).contains("202609060002"),"PIPELINE_READS_R926_SOLE_PROJECTION");
            check(RuntimeObservabilityR925.shadow(store).contains("100.00%"),"SHADOW_EXACT_SETTLEMENT_AGGREGATION");
            check(RuntimeObservabilityR925.epochs(store).contains("1/1"),"RELEASE_EPOCH_ACTUAL_DRAW_DENOMINATOR");
            RuntimeObservabilityR925.timing(db,"202609060001",100,120,200,50,30,"t1",100,120,200);RuntimeObservabilityR925.timing(db,"202609060002",300,310,400,50,40,"t2",300,310,400);
            check(RuntimeObservabilityR925.performance(store).contains("WARM_MONOTONIC N=1"),"COLD_WARM_SEPARATED");
            db.execSQL("CREATE TABLE v57_algorithm_predraw_r908(genome_id TEXT,target_issue TEXT,hit_label INTEGER,score REAL)");
            PromotionWindowR925.register(db,"g","202609060001",240,"h");
            check(PromotionWindowR925.evaluate(store,"g").weight==0,"NO_NEW_SAMPLES_NO_INFLUENCE");
            PromotionWindowR925.revoke(db,"g");
            check(RuntimeObservabilityR925.query(store,"SELECT COUNT(*) n FROM v57_promotion_history_r925",false).contains("1"),"REVOKED_WINDOW_HISTORY_PRESERVED");
            String request=FetchLedgerR925.begin(store,"GET","https://example.com/data?key=SECRET_TEST_ONLY");
            ApiClient.RawHttpResult raw=new ApiClient.RawHttpResult();raw.ok=false;raw.httpCode=503;raw.body="bad";
            FetchLedgerR925.complete(store,request,20,raw);
            String ledger=FetchLedgerR925.report(store,10);
            check(ledger.contains("HTTP_FAILED")&&!ledger.contains("SECRET_TEST_ONLY"),"FETCH_FAILURE_DURABLE_WITHOUT_QUERY_SECRET");
            raw.ok=true;raw.httpCode=200;FetchLedgerR925.complete(store,request,25,raw);FetchLedgerR925.payload(store,request,"backup","202609060002",false,"STALE");
            check(FetchLedgerR925.report(store,10).contains("PAYLOAD_REJECTED"),"HTTP_200_NOT_BUSINESS_ACCEPTANCE");
            CaptureOpportunityR925.record(store,"202609060001","g","SYSTEM_BUDGET_YIELD",5,"budget");
            check(CaptureOpportunityR925.executionCoverage(store,"g")[0]==0,"SYSTEM_BUDGET_NOT_EXECUTION_FAILURE");
            CaptureOpportunityR925.record(store,"202609060001","g","FROZEN",5,"");CaptureOpportunityR925.record(store,"202609060001","g","EXECUTION_NO_OUTPUT",5,"retry");
            check(CaptureOpportunityR925.executionCoverage(store,"g")[1]==1,"FROZEN_OPPORTUNITY_NOT_DOWNGRADED");
            db.execSQL("CREATE TABLE v57_algorithm_genome_r908(genome_id TEXT,structural_hash TEXT)");
            db.execSQL("INSERT INTO v57_hypothesis_r925 VALUES('g','family','hash','PREREGISTERED',1)");
            double original=ResearchFamilyRegistryR925.adjustedQ(store,"g",180,240);
            db.execSQL("INSERT INTO v57_hypothesis_r925 VALUES('archived','family','hash2','PREREGISTERED',1)");
            check(ResearchFamilyRegistryR925.adjustedQ(store,"g",180,240)>=original,"ARCHIVED_HYPOTHESIS_REMAINS_MULTIPLICITY");
            PromotionWindowR925.register(db,"window","202609060002",240,"h");
            for(int i=0;i<360;i++){String issue=String.format(java.util.Locale.US,"20260906%04d",i+3);db.execSQL("INSERT INTO v57_algorithm_predraw_r908 VALUES (?,?,?,?)",new Object[]{"window",issue,1,1});db.execSQL("INSERT INTO v57_oos_label_r35 VALUES (?,?,?)",new Object[]{issue,0,1});}
            check(PromotionWindowR925.evaluate(store,"window").weight==0 && Double.isNaN(PromotionWindowR925.evaluate(store,"window").brier),"LEGACY_ROLLING_RAW_SCORE_CANNOT_BUY_INFLUENCE");
            db.execSQL("UPDATE v57_algorithm_predraw_r908 SET hit_label=0 WHERE genome_id='window' AND target_issue<'202609060123'");
            check(PromotionWindowR925.evaluate(store,"window").weight==0,"FAILED_FIRST_WINDOW_CANNOT_BE_HIDDEN_BY_LATER_WINS");
            db.execSQL("CREATE TABLE research_new_asset_freeze_v41(asset_id TEXT,target_issue TEXT,hit_label INTEGER,score REAL)");
            PromotionWindowR925.register(db,"genesis:g","202609060002",240,"h");
            check(PromotionWindowR925.evaluateGenesis(store,"g").weight==0,"GENESIS_OLD_CERTIFICATE_NO_NEW_WINDOW_NO_INFLUENCE");
            check(BacklogConvergenceR925.report(store).contains("预计清空=0"),"EMPTY_BACKLOG_ETA_ZERO");
            System.out.println("R925_SQLITE_PRODUCTION_SQL=PASS|ANDROID_DEVICE=NOT_TESTED");
        }finally{db.shutdown();}
    }
}
