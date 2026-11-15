package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONObject;

/** Durable, predraw registered paired windows. Canonical/REAL records are never written here. */
public final class FixedResearchWindowR932 {
    public static final String WINDOW="research_fixed_window_r932",CHAMPION="research_fixed_champion_r932",PAIR="research_fixed_pair_r932",CONTRACT="research_fixed_window_contract_r932";
    private FixedResearchWindowR932() {}
    public static void schema(SQLiteDatabase db) {
        if (TransactionSchemaCacheR941.readyCommitted(db,"FixedResearchWindowR932")) return;

        ObservationGenerationR932.schema(db);
        db.execSQL("CREATE TABLE IF NOT EXISTS "+CONTRACT+"(window_id TEXT PRIMARY KEY,contract_hash TEXT NOT NULL)");
        ResearchMultiplicityR932.schema(db);
        CandidateCalibrationR932.schema(db);
        PairedResearchChallengeR932.schema(db);
        FixedWindowStatisticsR932.schema(db);
        db.execSQL("CREATE TABLE IF NOT EXISTS "+WINDOW+"(window_id TEXT PRIMARY KEY,candidate_key TEXT NOT NULL,structural_hash TEXT NOT NULL,champion_hash TEXT NOT NULL,selection_hash TEXT NOT NULL,first_issue TEXT NOT NULL,last_issue TEXT NOT NULL,planned_n INTEGER NOT NULL,window_no INTEGER NOT NULL,registered_at INTEGER NOT NULL,state TEXT NOT NULL,reason TEXT NOT NULL,result_hash TEXT NOT NULL DEFAULT '',UNIQUE(candidate_key,window_no))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r932_window_endpoint ON "+WINDOW+"(state,last_issue)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+CHAMPION+"(target_issue TEXT NOT NULL,bundle_hash TEXT NOT NULL,input_hash TEXT NOT NULL,selection_hash TEXT NOT NULL,position INTEGER NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,prediction_hash TEXT NOT NULL,frozen_at INTEGER NOT NULL,PRIMARY KEY(target_issue,bundle_hash))");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+PAIR+"(window_id TEXT NOT NULL,target_issue TEXT NOT NULL,candidate_hash TEXT NOT NULL,champion_prediction_hash TEXT NOT NULL,position INTEGER NOT NULL,n1 INTEGER NOT NULL,n2 INTEGER NOT NULL,n3 INTEGER NOT NULL,selected_hit_p REAL NOT NULL,calibration_hash TEXT NOT NULL,frozen_at INTEGER NOT NULL,candidate_hit INTEGER NOT NULL DEFAULT -1,champion_hit INTEGER NOT NULL DEFAULT -1,settled_at INTEGER NOT NULL DEFAULT 0,PRIMARY KEY(window_id,target_issue))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r932_pair_pending ON "+PAIR+"(candidate_hit,target_issue)");
    
        ResearchGatePlanR967.schema(db);
        TransactionSchemaCacheR941.markCommitted(db,"FixedResearchWindowR932");
    }
    /** Re-rank the immutable full group under its captured base bundle, without research consumers. */
    public static void freezeChampion(final DbStore store,final DeferredEvidenceSnapshotR36.Snapshot snapshot) {
        String model=null;
        try {for(String proof:snapshot.proofs)if(proof.startsWith(RawTrainingInputR932.PREFIX))model=new JSONObject(proof.substring(RawTrainingInputR932.PREFIX.length())).getString("model_bundle_hash");}
        catch(Exception e){throw new IllegalArgumentException("R932_CHAMPION_INPUT_PROOF",e);}
        if(model==null)return; // Historical snapshots cannot be retroactively repaired into new evidence.
        final String bundle=model;
        try(Cursor c=store.readable().rawQuery("SELECT input_hash,selection_hash FROM "+CHAMPION+" WHERE target_issue=? AND bundle_hash=?",new String[]{snapshot.targetIssue,bundle})) {
            if(c.moveToFirst()){
                if(!snapshot.recordHash.equals(c.getString(0))||!SelectionEvidenceContractR929.installedPolicy().hash.equals(c.getString(1)))throw new IllegalStateException("R932_CHAMPION_REPLAY_DRIFT");
                return;
            }
        }catch(android.database.sqlite.SQLiteException firstSchema){ /* create schema below */ }
        try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.forEvidence(bundle,snapshot.targetIssue,System.currentTimeMillis())) {
            V51Models.PositionAction[] actions=snapshot.positionActions();double[][] meta=new double[actions.length][];
            for(int i=0;i<actions.length;i++)meta[i]=actions[i].metaFeatures;
            final V51Models.PositionAction selected=V51RuntimeOrchestrator.rankK3DiagnosticActionsF06(actions,meta).rankOne;
            final String selector=SelectionEvidenceContractR929.installedPolicy().hash;
            final String hash=V51ChecksumEngine.sha256("R932_FIXED_CHAMPION|"+snapshot.targetIssue+'|'+bundle+'|'+snapshot.recordHash+'|'+selector+'|'+selected.checksum);
            boolean ok=store.coordinatorR36().runAtomicWrite("R932_FREEZE_INDEPENDENT_CHAMPION",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase db){
                schema(db);if(drawExists(db,snapshot.targetIssue))throw new IllegalStateException("R932_CHAMPION_DRAW_ALREADY_EXISTS");
                db.execSQL("INSERT OR IGNORE INTO "+CHAMPION+" VALUES(?,?,?,?,?,?,?,?,?,?)",new Object[]{snapshot.targetIssue,bundle,snapshot.recordHash,selector,selected.position,selected.n1,selected.n2,selected.n3,hash,System.currentTimeMillis()});
                try(Cursor c=db.rawQuery("SELECT prediction_hash FROM "+CHAMPION+" WHERE target_issue=? AND bundle_hash=?",new String[]{snapshot.targetIssue,bundle})) {if(!c.moveToFirst()||!hash.equals(c.getString(0)))throw new IllegalStateException("R932_CHAMPION_REPLAY_DRIFT");}
                ChampionProbabilityEvidenceR82.freeze(db,snapshot,selected,bundle,selector,hash);
            }});
            if(!ok)throw new IllegalStateException("R932_CHAMPION_FREEZE_COMMIT_FAILED");
        }
    }
    /** Invoke only inside the candidate prediction transaction; unknown probability stays -1. */
    public static void freezePair(SQLiteDatabase db,String key,String structural,String target,String candidateHash,int position,int n1,int n2,int n3) {
        freezePair(db,key,structural,target,candidateHash,position,n1,n2,n3,Double.NaN);
    }
    public static void freezePair(SQLiteDatabase db,String key,String structural,String target,String candidateHash,int position,int n1,int n2,int n3,double rawScore) {
        if(!db.inTransaction())throw new IllegalStateException("R932_PAIR_REQUIRES_TRANSACTION");
        schema(db);if(!ResearchEvaluationRunR967.eligibleEvaluation(key,target))throw new IllegalStateException("R969_TRAIN_ISSUE_NOT_SEALED_EVALUATION");if(drawExists(db,target))throw new IllegalStateException("R932_PAIR_POSTDRAW_REJECTED");
        if(!Double.isFinite(rawScore))return; // Unknown score must not spend formal alpha or bypass calibration.
        String bundle,selector,champion;
        try(Cursor c=db.rawQuery("SELECT bundle_hash,selection_hash,prediction_hash FROM "+CHAMPION+" WHERE target_issue=? LIMIT 2",new String[]{target})) {
            if(!c.moveToFirst())return;bundle=c.getString(0);selector=c.getString(1);champion=c.getString(2);if(c.moveToNext())throw new IllegalStateException("R932_CHAMPION_AMBIGUOUS");
        }
        if(!ResearchGatePlanR967.installed().isCp3)PairedResearchChallengeR932.freeze(db,key,structural,target,candidateHash,rawScore,position,n1,n2,n3);
        String window=null;int number=1;boolean recovery=false;
        try(Cursor c=db.rawQuery("SELECT window_id,structural_hash,champion_hash,selection_hash,first_issue,last_issue,state,window_no FROM "+WINDOW+" WHERE candidate_key=? ORDER BY window_no DESC LIMIT 1",new String[]{key})) {
            if(c.moveToFirst()) {
                String state=c.getString(6);number=c.getInt(7);window=c.getString(0);
                if(!"OPEN".equals(state)&&!"PASS".equals(state)&&!ObservationGenerationR932.isSystemPause(state))return;
                String frozen="";try(Cursor dependency=db.rawQuery("SELECT contract_hash FROM "+CONTRACT+" WHERE window_id=?",new String[]{window})){if(dependency.moveToFirst())frozen=dependency.getString(0);}
                boolean changed=!frozen.equals(contract(db))||!structural.equals(c.getString(1))||!bundle.equals(c.getString(2))||!selector.equals(c.getString(3));
                if("OPEN".equals(state)&&changed){
                    db.execSQL("UPDATE "+WINDOW+" SET state='PAUSED',reason='ATOM_SCHEMA_SOURCE_DEPENDENCY_CHANGED' WHERE window_id=? AND state='OPEN'",new Object[]{window});return;
                }
                if(ObservationGenerationR932.isSystemPause(state)||"PASS".equals(state)&&changed){
                    if(target.compareTo(c.getString(5))<=0)return;
                    // Never reset old denominators. Recovery starts a separately-paid observation generation.
                    recovery=true;number++;window=null;
                }else if("PASS".equals(state)){
                    if(ObservationGenerationR932.of(db,key,window,number).stage>=5||target.compareTo(c.getString(5))<=0)return;
                    number++;window=null;
                }else if(target.compareTo(c.getString(4))<0||target.compareTo(c.getString(5))>0)return;
            }
        }
        CandidateCalibrationR932.Probability calibrated=null;
        if(Double.isFinite(rawScore)) {
            calibrated=CandidateCalibrationR932.captureOrPredict(db,key,structural,bundle,selector,target,candidateHash,rawScore,position,n1,n2,n3);
            if(calibrated==null)return;
        }
        if(window==null){
            if(recovery){
                if(calibrated==null)return;
                window=ObservationGenerationR932.recover(db,key,structural,bundle,selector,target,calibrated.hash);
                if(window==null)return;
            }else{
                ObservationGenerationR932.Chain next=ObservationGenerationR932.next(db,key,number,target);
                window=register(db,key,structural,bundle,selector,target,new int[]{120,240,240,240,360}[next.stage-1],number);
            }
        }
        if(ResearchGatePlanR967.installed().isCp3)ResearchGatePlanR967.observeCp4(db,window,key,target,candidateHash);
        db.execSQL("INSERT OR IGNORE INTO "+PAIR+"(window_id,target_issue,candidate_hash,champion_prediction_hash,position,n1,n2,n3,selected_hit_p,calibration_hash,frozen_at) VALUES(?,?,?,?,?,?,?,?,?,?,?)",new Object[]{window,target,candidateHash,champion,position,n1,n2,n3,calibrated==null?-1.0:calibrated.value,calibrated==null?"":calibrated.hash,System.currentTimeMillis()});
        try(Cursor c=db.rawQuery("SELECT candidate_hash,champion_prediction_hash FROM "+PAIR+" WHERE window_id=? AND target_issue=?",new String[]{window,target})) {
            if(!c.moveToFirst()||!candidateHash.equals(c.getString(0))||!champion.equals(c.getString(1)))throw new IllegalStateException("R932_PAIR_REPLAY_DRIFT");
        }
    }
    static String register(SQLiteDatabase db,String key,String structural,String bundle,String selector,String first,int planned,int number) {
        ObservationGenerationR932.Chain chain=ObservationGenerationR932.next(db,key,number,first);
        return persistWindow(db,key,structural,bundle,selector,first,planned,number,chain);
    }
    static String persistWindow(SQLiteDatabase db,String key,String structural,String bundle,String selector,String first,int planned,int number,ObservationGenerationR932.Chain chain){
        if(!db.inTransaction())throw new IllegalStateException("R932_WINDOW_REGISTRATION_TRANSACTION");
        int[] sizes={120,240,240,240,360};
        if(chain.stage<1||chain.stage>5||planned!=sizes[chain.stage-1]||number!=chain.start+chain.stage-1||!TargetIssueResolver.isValid(first))throw new IllegalArgumentException("R932_FIXED_WINDOW_SIZE");
        if(drawExists(db,first))throw new IllegalStateException("R932_WINDOW_REGISTRATION_POSTDRAW");
        String last=ResearchEvaluationRunR967.endpoint(key,first,planned);
        String id=V51ChecksumEngine.sha256("R932_WINDOW|"+key+'|'+structural+'|'+bundle+'|'+selector+'|'+first+'|'+last+'|'+planned+'|'+number);
        boolean exists=false;try(Cursor prior=db.rawQuery("SELECT window_id FROM "+WINDOW+" WHERE candidate_key=? AND window_no=?",new String[]{key,String.valueOf(number)})){
            if(prior.moveToFirst()){exists=true;if(!id.equals(prior.getString(0)))throw new IllegalStateException("R932_WINDOW_REGISTRATION_CONFLICT");}
        }
        if(!exists)db.execSQL("INSERT INTO "+WINDOW+"(window_id,candidate_key,structural_hash,champion_hash,selection_hash,first_issue,last_issue,planned_n,window_no,registered_at,state,reason,formal_plan_revision) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{id,key,structural,bundle,selector,first,last,planned,number,System.currentTimeMillis(),"OPEN",ResearchGatePlanR967.REASON,ResearchGatePlanR967.REVISION});
        try(Cursor c=db.rawQuery("SELECT window_id FROM "+WINDOW+" WHERE candidate_key=? AND window_no=?",new String[]{key,String.valueOf(number)})){if(!c.moveToFirst()||!id.equals(c.getString(0)))throw new IllegalStateException("R932_WINDOW_REGISTRATION_CONFLICT");}
        db.execSQL("INSERT OR IGNORE INTO "+CONTRACT+" VALUES(?,?)",new Object[]{id,contract(db)});
        ObservationGenerationR932.bind(db,id,key,chain);
        ResearchGatePlanR967.registerFormalExperiment(db,id,key,structural,bundle,selector,first,last,planned,number,chain,!exists);
        return id;
    }
    public static void maintain(final DbStore store) { maintainReviewed(store); }
    static String maintainReviewed(final DbStore store) {
        if(store==null)return "ERROR:STORE_NULL";
        if(RealtimePressureGateR811.hasActiveCriticalWork()||SettlementExecutionGateR819.isActive())return "YIELD_P0_PRESSURE";
        boolean ready=store.coordinatorR36().runAtomicWrite("R932_FIXED_WINDOW_SCHEMA",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase db){schema(db);}});
        if(!ready)throw new IllegalStateException("R932_FIXED_WINDOW_SCHEMA_COMMIT_FAILED");
        // Three independent owners rotate FIRST place. A fit/schema/close that consumes its
        // budget must not starve label settlement indefinitely (R978 report: every V3 study N=0).
        int phase=ResearchScheduleR924.phase(store.getState("r979_window_phase","0"),3);
        store.setState("r979_window_phase",Integer.toString((phase+1)%3));
        if(phase==1)CandidateCalibrationR932.fitOne(store);
        if(phase==2)closeOne(store);
        // Refill the oldest eligible head after each independent small page. The old
        // fixed two-issue limit wasted remaining budget while labels accumulated.
        // Bounded by BOTH 16 pages and the enclosing cooperative deadline; no new worker.
        for(int page=0;page<16;page++) {
            ResearchWorkBudgetR927.checkpoint();
            if(page>0&&ResearchWorkBudgetR927.remainingMs()<=80L)break;
            String issue;
            try(Cursor c=BudgetedQueryR927.query(store.readable(),pendingIssueSql()+" ORDER BY target_issue LIMIT 1",null)) {
                if(!c.moveToFirst())break;issue=c.getString(0);
            }
            ResearchWorkBudgetR927.checkpoint();final Models.Draw draw=store.getDrawByIssue(issue);if(draw==null||!draw.isValid())break;
            // Each family commits independently, so a later cooperative yield cannot erase
            // calibration progress already made for this draw.
            if(hasPending(store.readable(),CandidateCalibrationR932.OBS,"hit",issue)
                    && !store.coordinatorR36().runAtomicWrite("R948_CAL_SETTLE_PAGE",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase db){CandidateCalibrationR932.settle(db,draw);}}))throw new IllegalStateException("R948_CAL_SETTLE_COMMIT_FAILED");
            ResearchWorkBudgetR927.checkpoint();
            if(!ResearchGatePlanR967.installed().isCp3 && hasPending(store.readable(),PairedResearchChallengeR932.PREDICTION,"hit",issue)
                    && !store.coordinatorR36().runAtomicWrite("R948_CHALLENGE_SETTLE_PAGE",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase db){PairedResearchChallengeR932.settle(db,draw);}}))throw new IllegalStateException("R948_CHALLENGE_SETTLE_COMMIT_FAILED");
            boolean ok=!hasPending(store.readable(),PAIR,"candidate_hit",issue)
                    || store.coordinatorR36().runAtomicWrite("R932_FIXED_PAIR_SETTLE",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase db){
                try(Cursor c=db.rawQuery("SELECT p.window_id,p.position,p.n1,p.n2,p.n3,c.position,c.n1,c.n2,c.n3 FROM "+PAIR+" p JOIN "+WINDOW+" w ON w.window_id=p.window_id JOIN "+CHAMPION+" c ON c.target_issue=p.target_issue AND c.bundle_hash=w.champion_hash AND c.prediction_hash=p.champion_prediction_hash WHERE p.target_issue=? AND p.candidate_hit=-1 ORDER BY p.window_id LIMIT 16",new String[]{draw.issue})) {
                    int completed=0;
                    while(c.moveToNext()) {
                        try{ResearchWorkBudgetR927.checkpoint();}catch(ResearchWorkBudgetR927.Yield y){if(completed==0)throw y;break;}
                        int hit=draw.hitStrictPositionK3(c.getInt(1),c.getInt(2),c.getInt(3),c.getInt(4))?1:0;
                        int base=draw.hitStrictPositionK3(c.getInt(5),c.getInt(6),c.getInt(7),c.getInt(8))?1:0;
                        db.execSQL("UPDATE "+PAIR+" SET candidate_hit=?,champion_hit=?,settled_at=? WHERE window_id=? AND target_issue=? AND candidate_hit=-1",new Object[]{hit,base,System.currentTimeMillis(),c.getString(0),draw.issue});completed++;
                    }
                }
            }});if(!ok)throw new IllegalStateException("R932_PAIR_SETTLE_COMMIT_FAILED");
        }
        if(ResearchWorkBudgetR927.remainingMs()>80){
            if(phase!=1)CandidateCalibrationR932.fitOne(store);
            if(ResearchWorkBudgetR927.remainingMs()>60&&phase!=2)closeOne(store);
        }
        return hasRunnableWork(store.readable())?"FIXED_WINDOW_PENDING":"FIXED_WINDOWS_REVIEWED";
    }
    /** Readiness is not authority: it only keeps the existing bounded owner scheduled. */
    static boolean hasRunnableWork(SQLiteDatabase db) {
        try(Cursor c=db.rawQuery("SELECT 1 FROM "+CandidateCalibrationR932.STUDY+" WHERE state='READY' LIMIT 1",null)){
            if(c.moveToFirst())return true;
        }
        try(Cursor c=db.rawQuery(pendingIssueSql()+" LIMIT 1",null)){if(c.moveToFirst())return true;}
        String endpoint="";
        try(Cursor c=db.rawQuery("SELECT issue FROM "+DbHelper.TABLE_DRAWS+" WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)){
            if(c.moveToFirst())endpoint=c.getString(0);
        }
        if(!TargetIssueResolver.isValid(endpoint))return false;
        // Legacy unregistered rows receive an explicit non-authoritative review status.
        try(Cursor c=db.rawQuery("SELECT 1 FROM "+WINDOW+" w WHERE w.state='OPEN' AND NOT EXISTS(SELECT 1 FROM "+ResearchMultiplicityR932.LOOK+" l WHERE l.window_id=w.window_id) LIMIT 1",null)){
            if(c.moveToFirst())return true;
        }
        // Avoid rebuilding a dependency graph on the common empty/no-ended-window path.
        try(Cursor c=db.rawQuery("SELECT 1 FROM "+WINDOW+" WHERE state='OPEN' AND last_issue<=? LIMIT 1",new String[]{endpoint})){
            if(!c.moveToFirst())return false;
        }
        try(Cursor c=db.rawQuery("SELECT 1 FROM "+WINDOW+" w WHERE "+closablePredicate()+" LIMIT 1",new String[]{endpoint,contract(db)})){
            return c.moveToFirst();
        }
    }
    private static boolean hasPending(SQLiteDatabase db,String table,String label,String issue) {
        // Only private call sites supply constant schema names.
        try(Cursor c=db.rawQuery("SELECT 1 FROM "+table+" WHERE target_issue=? AND "+label+"=-1 LIMIT 1",new String[]{issue})){return c.moveToFirst();}
    }
    private static String pendingIssueSql(){
        // At most one indexed head per independent evidence family enters the UNION.
        // The prior UNION materialized the entire historical backlog even for LIMIT 1.
        // A JOIN to the wide UNION source causes SQLite to scan/materialize every
        // hot draw (including raw_json) before serving this one indexed head. Probe
        // the two primary keys instead. Preserve the source view's precedence:
        // a dirty hot row must not silently fall back to an older retained copy.
        String draw=" AND (EXISTS(SELECT 1 FROM "+DbHelper.TABLE_DRAWS+" d WHERE d.issue=p.target_issue AND d.dirty=0)"
                +" OR (NOT EXISTS(SELECT 1 FROM "+DbHelper.TABLE_DRAWS+" d WHERE d.issue=p.target_issue)"
                +" AND EXISTS(SELECT 1 FROM "+DrawDependencyInputCP12.TABLE+" d WHERE d.issue=p.target_issue AND d.dirty=0)))";
        return "SELECT target_issue FROM (SELECT target_issue FROM (SELECT p.target_issue FROM "+PAIR+" p JOIN "+WINDOW
                +" w ON w.window_id=p.window_id JOIN "+CHAMPION
                +" c ON c.target_issue=p.target_issue AND c.bundle_hash=w.champion_hash AND c.prediction_hash=p.champion_prediction_hash"
                +" WHERE p.candidate_hit=-1"+draw+" ORDER BY p.target_issue LIMIT 1)"
                +" UNION SELECT target_issue FROM (SELECT p.target_issue FROM "+CandidateCalibrationR932.OBS+" p"
                +" WHERE p.hit=-1"+draw+" ORDER BY p.target_issue LIMIT 1)"
                +(ResearchGatePlanR967.installed().isCp3?"":" UNION SELECT target_issue FROM (SELECT p.target_issue FROM "+PairedResearchChallengeR932.PREDICTION+" p WHERE p.hit=-1"+draw+" ORDER BY p.target_issue LIMIT 1)")+ ") pending";
    }
    private static String closablePredicate(){
        // Keep unknown/pending labels intact. Skip a waiting head, but still invalidate
        // its obsolete dependency contract just as the original closer did.
        return "w.state='OPEN' AND w.last_issue<=? AND (NOT EXISTS(SELECT 1 FROM "+PAIR
                +" p WHERE p.window_id=w.window_id AND (p.candidate_hit<0 OR p.champion_hit<0))"
                +" OR NOT EXISTS(SELECT 1 FROM "+CONTRACT+" k WHERE k.window_id=w.window_id AND k.contract_hash=?))";
    }
    /** One fixed-window close, independently schedulable of the settlement backlog. */
    static void closeOne(final DbStore store) {
        if(ResearchGatePlanR967.installed().isCp3){closeOneCp4(store);return;}
        ResearchWorkBudgetR927.checkpoint();
        String latest="";try(Cursor c=store.readable().rawQuery("SELECT issue FROM "+DbHelper.TABLE_DRAWS+" WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)){if(c.moveToFirst())latest=c.getString(0);}
        final String endpoint=latest;
        // Empty queues must not rebuild the atom/source dependency contract every pulse.
        // Legacy rows still receive an explicit review; no real OPEN window is skipped.
        try(Cursor c=store.readable().rawQuery("SELECT 1 FROM "+WINDOW+" w WHERE w.state='OPEN' AND (w.last_issue<=? OR NOT EXISTS(SELECT 1 FROM "+ResearchMultiplicityR932.LOOK+" l WHERE l.window_id=w.window_id)) LIMIT 1",new String[]{endpoint})) {
            if(!c.moveToFirst())return;
        }
        boolean sealed=store.coordinatorR36().runAtomicWrite("R932_FIXED_WINDOW_CLOSE",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase db){
            db.execSQL("UPDATE "+WINDOW+" SET state='LEGACY_REVIEW',reason='NO_PREDRAW_GLOBAL_ALPHA_REGISTRATION' WHERE state='OPEN' AND NOT EXISTS(SELECT 1 FROM "+ResearchMultiplicityR932.LOOK+" l WHERE l.window_id="+WINDOW+".window_id)");
            try(Cursor c=db.rawQuery("SELECT w.window_id,w.planned_n,w.last_issue FROM "+WINDOW+" w WHERE "+closablePredicate()+" ORDER BY w.last_issue,w.window_id LIMIT 1",new String[]{endpoint,contract(db)})) {
                while(c.moveToNext()) {
                    ResearchWorkBudgetR927.checkpoint();String id=c.getString(0);int planned=c.getInt(1);
                    String frozen="";try(Cursor dependency=db.rawQuery("SELECT contract_hash FROM "+CONTRACT+" WHERE window_id=?",new String[]{id})){if(dependency.moveToFirst())frozen=dependency.getString(0);}
                    if(!frozen.equals(contract(db))){db.execSQL("UPDATE "+WINDOW+" SET state='PAUSED',reason='ATOM_SCHEMA_SOURCE_DEPENDENCY_CHANGED' WHERE window_id=? AND state='OPEN'",new Object[]{id});continue;}
                    FixedWindowStatisticsR932.Result result=FixedWindowStatisticsR932.evaluate(db,id,planned,endpoint);
                    if(result==null)continue;
                    FixedWindowStatisticsR932.persist(db,id,result);
                    ResearchMultiplicityR932.close(db,id,endpoint,result.state,result.hash,result.rawP);
                    db.execSQL("UPDATE "+WINDOW+" SET state=?,reason=?,result_hash=? WHERE window_id=? AND state='OPEN'",new Object[]{result.state,result.json,result.hash,id});
                }
            }
        }});if(!sealed)throw new IllegalStateException("R932_FIXED_WINDOW_CLOSE_COMMIT_FAILED");
    }
    public static String report(DbStore store) {
        StringBuilder out=new StringBuilder("\n独立校准与固定配对窗口\n");
        try(Cursor exists=store.readable().rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{WINDOW})){if(!exists.moveToFirst())return out.append("尚无新格式窗口；旧分数不能作为校准概率。\n").toString();}
        try(Cursor c=store.readable().rawQuery("SELECT state,COUNT(*) FROM "+CandidateCalibrationR932.STUDY+" GROUP BY state",null)){while(c.moveToNext())out.append("候选校准 ").append(c.getString(0)).append(": ").append(c.getInt(1)).append('\n');}
        try(Cursor c=store.readable().rawQuery("SELECT state,COUNT(*) FROM "+WINDOW+" GROUP BY state",null)){while(c.moveToNext())out.append("窗口 ").append(c.getString(0)).append(": ").append(c.getInt(1)).append('\n');}
        try(Cursor c=store.readable().rawQuery("SELECT COUNT(*),COALESCE(SUM(alpha),0) FROM "+ResearchMultiplicityR932.FAMILY,null)){if(c.moveToFirst())out.append("已注册统计族: ").append(c.getInt(0)).append("；已分配 alpha: ").append(c.getDouble(1)).append(" / 0.05\n");}
        try(Cursor c=store.readable().rawQuery("SELECT w.candidate_key,w.window_no,w.state,s.metrics_json FROM "+WINDOW+" w LEFT JOIN "+FixedWindowStatisticsR932.TABLE+" s ON s.window_id=w.window_id ORDER BY w.registered_at DESC LIMIT 3",null)) {
            while(c.moveToNext()) {
                out.append(c.getString(0)).append(" #").append(c.getInt(1)).append(' ').append(c.getString(2));
                if(!c.isNull(3))try{out.append(windowMetricsR81(new JSONObject(c.getString(3))));}catch(org.json.JSONException e){out.append("；指标损坏，需核查");}
                out.append('\n');
            }
        }
        return out.append("候选专用校准不等于最终混合选择校准；窗口通过不直接授予 REAL。\n").append(FixedWindowCertificatePublisherR932.report(store)).toString();
    }
    static String contract(SQLiteDatabase db){return CalibrationRuntimeIdentityF04.formalContract();}
    private static boolean drawExists(SQLiteDatabase db,String issue) {
        try(Cursor c=db.rawQuery("SELECT 1 FROM "+DbHelper.TABLE_DRAWS+" WHERE issue>=? AND dirty=0 LIMIT 1",new String[]{issue})){return c.moveToFirst();}
    }

    static void closeOneCp4(final DbStore store){
        String issue="";try(Cursor c=store.readable().rawQuery("SELECT issue FROM "+DbHelper.TABLE_DRAWS+" WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)){if(c.moveToFirst())issue=c.getString(0);}
        if(issue.isEmpty())return;final String endpoint=issue;
        boolean ok=store.coordinatorR36().runAtomicWrite("CP4_FORMAL_WINDOW_CLOSE",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase db){
            try(Cursor c=db.rawQuery("SELECT w.window_id,w.planned_n FROM "+WINDOW+" w JOIN "+ResearchGatePlanR967.CP4_BINDING+" b ON b.window_id=w.window_id WHERE w.state='OPEN' AND w.last_issue<=? ORDER BY w.last_issue,w.window_id LIMIT 1",new String[]{endpoint})){
                if(!c.moveToFirst())return;String id=c.getString(0);FixedWindowStatisticsR932.Result r=FixedWindowStatisticsR932.evaluate(db,id,c.getInt(1),endpoint);if(r==null)return;
                FixedWindowStatisticsR932.persist(db,id,r);
                db.execSQL("UPDATE "+WINDOW+" SET state=?,reason=?,result_hash=? WHERE window_id=? AND state='OPEN'",new Object[]{r.state,r.json,r.hash,id});
            }
        }});if(!ok)throw new IllegalStateException("CP4_FORMAL_CLOSE_COMMIT");
    }


    /** CP4 endpoint receipts do not contain the legacy HR or adjusted-p fields. */
    static String windowMetricsR81(JSONObject metrics) throws org.json.JSONException {
        int paired = metrics.getInt("paired_n"), planned = metrics.getInt("planned_n");
        if (paired < 0 || planned <= 0 || paired > planned)
            throw new org.json.JSONException("INVALID_WINDOW_DENOMINATOR");
        String prefix = "；配对 " + paired + '/' + planned;
        if ("CP4_ENDPOINT_INTEGRITY_V1".equals(metrics.optString("revision", ""))) {
            String ece = metrics.isNull("ece") ? "UNKNOWN" : String.valueOf(metricR81(metrics, "ece"));
            return prefix + "；HR MISSING（端点收据未记录）；ECE " + ece
                    + "；校正 p N/A（CP4端点完整性收据，不授予资格）";
        }
        return prefix + "；HR " + metricR81(metrics, "hit_rate") + "；ECE "
                + metricR81(metrics, "ece") + "；校正 p " + metricR81(metrics, "fwer_adjusted_p");
    }

    private static double metricR81(JSONObject metrics, String key) throws org.json.JSONException {
        double value = metrics.getDouble(key);
        if (Double.isNaN(value) || Double.isInfinite(value) || value < 0d || value > 1d)
            throw new org.json.JSONException("INVALID_WINDOW_METRIC:" + key);
        return value;
    }

public static String reportSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        com.ethk3.novaonepro.ReportTextCP12 out=new com.ethk3.novaonepro.ReportTextCP12(report, "\n独立校准与固定配对窗口\n");
        try(Cursor exists=report.select("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{WINDOW})){if(!exists.moveToFirst())return out.append("尚无新格式窗口；旧分数不能作为校准概率。\n").toString();}
        try(Cursor c=report.select("SELECT state,COUNT(*) FROM "+CandidateCalibrationR932.STUDY+" GROUP BY state",null)){while(c.moveToNext())out.append("候选校准 ").append(c.getString(0)).append(": ").append(c.getInt(1)).append('\n');}
        try(Cursor c=report.select("SELECT state,COUNT(*) FROM "+WINDOW+" GROUP BY state",null)){while(c.moveToNext())out.append("窗口 ").append(c.getString(0)).append(": ").append(c.getInt(1)).append('\n');}
        try(Cursor c=report.select("SELECT COUNT(*),COALESCE(SUM(alpha),0) FROM "+ResearchMultiplicityR932.FAMILY,null)){if(c.moveToFirst())out.append("已注册统计族: ").append(c.getInt(0)).append("；已分配 alpha: ").append(c.getDouble(1)).append(" / 0.05\n");}
        try(Cursor c=report.select("SELECT w.candidate_key,w.window_no,w.state,s.metrics_json FROM "+WINDOW+" w LEFT JOIN "+FixedWindowStatisticsR932.TABLE+" s ON s.window_id=w.window_id ORDER BY w.registered_at DESC LIMIT 3",null)) {
            while(c.moveToNext()) {
                out.append(c.getString(0)).append(" #").append(c.getInt(1)).append(' ').append(c.getString(2));
                if(!c.isNull(3))try{out.append(windowMetricsR81(new JSONObject(c.getString(3))));}catch(org.json.JSONException e){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",e);}
                out.append('\n');
            }
        }
        return out.append("候选专用校准不等于最终混合选择校准；窗口通过不直接授予 REAL。\n").append(FixedWindowCertificatePublisherR932.reportSnapshotCP4(report, store)).toString();
    }
}
