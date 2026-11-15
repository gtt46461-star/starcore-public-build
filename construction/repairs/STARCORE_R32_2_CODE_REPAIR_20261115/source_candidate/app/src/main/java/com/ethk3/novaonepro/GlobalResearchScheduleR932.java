package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;

/** One immutable predraw roster across all existing research lanes. No worker or REAL owner. */
public final class GlobalResearchScheduleR932 {
    public static final String PLAN="research_schedule_plan_r932",TASK="research_schedule_task_r932",
            IDENTITY="research_candidate_identity_r932",PROXY="research_proxy_capture_r932";
    public static final long TOTAL_MS=1200,SLICE_MS=650;
    // A 24-member rotating evaluation cohort cannot achieve 80% capture with 100+ candidates.
    // Keep time/preemption bounds; admit the existing cohort, throttle new open creation upstream.
    public static final int EVALUATION_CAP_R963=128;
    private static final ThreadLocal<Shared> SHARED=new ThreadLocal<Shared>();
    private static final Map<DbStore,Shared> CACHE=Collections.synchronizedMap(new WeakHashMap<DbStore,Shared>());
    private GlobalResearchScheduleR932(){}
    public static final class Candidate {
        public final String key,family,structure,tier;
        final String last;
        public Candidate(String key,String family,String structure,String tier,String last) {
            if(key==null||key.length()==0||family==null||family.length()==0||structure==null||!structure.matches("[0-9a-f]{64}")||!Arrays.asList("A","B","C").contains(tier))throw new IllegalArgumentException("R932_ROSTER_CANDIDATE");
            this.key=key;this.family=family;this.structure=structure;this.tier=tier;this.last=last==null?"":last;
        }
    }
    static void schema(SQLiteDatabase db) {
        if (TransactionSchemaCacheR941.ready(db,"GlobalResearchScheduleR932")) return;

        StagedResearchRosterR943.schema(db);
        ResearchMultiplicityR932.schema(db);
        ResearchCohortR932.schema(db);
        ResearchLifecycleR932.schema(db);
        PairedResearchChallengeR932.schema(db);
        db.execSQL("CREATE TABLE IF NOT EXISTS "+PLAN+"(target_issue TEXT PRIMARY KEY,input_hash TEXT NOT NULL,roster_hash TEXT NOT NULL,created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+TASK+"(target_issue TEXT NOT NULL,candidate_key TEXT NOT NULL,family TEXT NOT NULL,structure_hash TEXT NOT NULL,tier TEXT NOT NULL,selected INTEGER NOT NULL,queue_rank INTEGER NOT NULL,PRIMARY KEY(target_issue,candidate_key))");
        boolean mode=false;
        try(Cursor columns=db.rawQuery("PRAGMA table_info("+TASK+")",null)){while(columns.moveToNext())if("execution_mode".equals(columns.getString(1)))mode=true;}
        if(!mode)db.execSQL("ALTER TABLE "+TASK+" ADD COLUMN execution_mode TEXT NOT NULL DEFAULT 'FULL1200'");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r959_schedule_mode_immutable BEFORE UPDATE ON "+TASK+" BEGIN SELECT RAISE(ABORT,'R959_SCHEDULE_IMMUTABLE'); END");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r932_schedule_candidate ON "+TASK+"(candidate_key,selected,target_issue)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+IDENTITY+"(candidate_key TEXT PRIMARY KEY,family TEXT NOT NULL,structure_hash TEXT NOT NULL,first_issue TEXT NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+PROXY+"(target_issue TEXT NOT NULL,candidate_key TEXT NOT NULL,input_hash TEXT NOT NULL,selected_ordinal INTEGER NOT NULL,raw_score REAL NOT NULL,action_count INTEGER NOT NULL,record_hash TEXT NOT NULL,created_at INTEGER NOT NULL,PRIMARY KEY(target_issue,candidate_key))");
    
        TransactionSchemaCacheR941.mark(db,"GlobalResearchScheduleR932");
    }
    public static final String RESERVATION_CP11="research_capture_reservation_v2";
    public static final String CAPTURE_SCHEDULE_CP11="CP12_FIXED_EVALUATION_241_V1";
    public static final int CAPTURE_ACTIVE_MAX_CP11=8, CAPTURE_COST_US_CP11=50000;
    private static final String CAPTURE_SCAN_R74="r74_capture_admission_scan";
    public static final class CaptureReservationCP11 {
        public final String id,key,freeze,study,first,next,state;
        public final long generation;
        CaptureReservationCP11(Cursor c){id=c.getString(0);key=c.getString(1);freeze=c.getString(2);
            study=c.getString(3);first=c.getString(4);next=c.getString(5);state=c.getString(6);generation=c.getLong(7);}
    }
    /** Existing scheduler Owner. Every reservation references a real durable champion for this target.
     * Metadata projections deliberately exclude AST/model payloads from the single DB writer. */
    public static int reserveUnscheduledFrozenCP11(SQLiteDatabase db,String target,String championBundle,String selectorHash){
        if(db==null||!db.inTransaction())throw new IllegalStateException("CAPTURE_RESERVATION_TRANSACTION_REQUIRED");
        if(!TargetIssueResolver.isValid(target)||championBundle==null||!championBundle.matches("[0-9a-f]{64}")
                ||selectorHash==null||!selectorHash.matches("[0-9a-f]{64}"))throw new IllegalArgumentException("CAPTURE_RESERVATION_IDENTITY");
        if(drawn(db,target))throw new IllegalStateException("CAPTURE_RESERVATION_POSTDRAW");
        try(Cursor c=db.rawQuery("SELECT bundle_hash,selection_hash FROM "+FixedResearchWindowR932.CHAMPION+" WHERE target_issue=? LIMIT 2",new String[]{target})){
            if(!c.moveToFirst()||!championBundle.equals(c.getString(0))||!selectorHash.equals(c.getString(1))||c.moveToNext())
                throw new IllegalStateException("CAPTURE_CHAMPION_MISSING_OR_AMBIGUOUS");
        }
        ArrayList<CaptureReservationCP11> active=new ArrayList<>();
        try(Cursor c=db.rawQuery("SELECT reservation_id,candidate_key,freeze_hash,study_hash,first_issue,next_issue,state,generation FROM "+RESERVATION_CP11
                +" WHERE state IN('RESERVED','ACTIVE') ORDER BY next_issue,created_at,reservation_id LIMIT 9",null)){
            while(c.moveToNext())active.add(new CaptureReservationCP11(c));
        }
        if(active.size()>CAPTURE_ACTIVE_MAX_CP11)throw new IllegalStateException("CAPTURE_CAPACITY_CORRUPT");
        for(CaptureReservationCP11 r:active){
            boolean valid;
            try(Cursor c=db.rawQuery("SELECT 1 FROM "+ResearchEvaluationRunR967.EVAL+" f JOIN "+ResearchEvaluationRunR967.RUN+" run ON run.run_hash=f.run_hash "
                    +"JOIN r952_candidate parent ON parent.candidate_id=f.learner_id WHERE ('algorithm:'||f.candidate_id)=? AND f.structural_hash=? "
                    +"AND run.first_issue=(SELECT MAX(rr.first_issue)FROM "+ResearchEvaluationRunR967.RUN+" rr WHERE rr.learner_id=f.learner_id AND rr.first_issue<=?) "
                    +"AND parent.structural_hash=run.ast_hash AND parent.state NOT IN('QUALITY_PRUNED','QUARANTINED','RESOURCE_RETIRED') AND "
                    +TrainingContinuationR973.predicate("f.rule_hash")+" LIMIT 1",new String[]{r.key,r.freeze,target})){valid=c.moveToFirst();}
            if(!valid){retireReservationCP11(db,r,"CAPTURE_DEPENDENCY_RETIRED");continue;}
            if(retireInvalidBoundReservationR80(db,r,target))continue;
            String currentStudy=CandidateCalibrationR932.study(db,r.key,r.freeze,championBundle,selectorHash);
            if(!currentStudy.equals(r.study)){retireReservationCP11(db,r,"CAPTURE_CHAMPION_DEPENDENCY_CHANGED");continue;}
            if(r.next.compareTo(target)<0){
                String gapReason="MISSING_SCHEDULED_CAPTURE";
                try(Cursor reason=db.rawQuery("SELECT last_reason FROM "+RESERVATION_CP11+" WHERE reservation_id=? AND generation=?",new String[]{r.id,Long.toString(r.generation)})){
                    if(reason.moveToFirst()&&"MEMORY_PRESSURE".equals(reason.getString(0)))gapReason="MEMORY_PRESSURE";
                }
                markReservationGapCP11(db,r,target,gapReason);
                CandidateCalibrationR932.restartReservedGapCP11(db,r.key,r.freeze,championBundle,selectorHash,r.study,target);
            }
        }
        int occupied;
        try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM "+RESERVATION_CP11+" WHERE state IN('RESERVED','ACTIVE')",null)){c.moveToFirst();occupied=c.getInt(0);}
        occupied+=com.ethk3.novaonepro.r952.BoundAtomLifecycleR80.occupiedR80(db);
        // The admitted consecutive captures own this predraw slice. When every
        // slot is occupied, scanning another 32 candidates cannot admit work,
        // yet it reads/revalidates their complete study dependencies before the
        // first due capture runs. Keep the durable scan position unchanged and
        // spend the bounded callback on the already promised observations.
        if(occupied>=CAPTURE_ACTIVE_MAX_CP11){
            LinkedHashMap<String,String> state=new LinkedHashMap<>();
            state.put("cp11_capture_active",Integer.toString(occupied));
            state.put("cp11_capture_reservation_status","WAIT_CAPTURE_CAPACITY");
            DbStore.putStatesR36(db,state);
            return 0;
        }
        int admitted=0;
        // Resume a bounded circular metadata scan. Terminal studies in the oldest
        // page must not starve later candidates; active reservations remain pinned.
        ArrayList<String[]> ids=new ArrayList<>();
        String after="";
        try(Cursor c=db.rawQuery("SELECT substr(state_value,1,256) FROM "+DbHelper.TABLE_APP_STATE+" WHERE state_key=?",new String[]{CAPTURE_SCAN_R74})){
            if(c.moveToFirst()&&!c.isNull(0))after=c.getString(0);
        }
        String[] position=after.split("\\|",-1);
        boolean resume=position.length==2&&position[1].matches("R958-F-V[23]-[0-9a-f]{24}");
        long afterAt=0;
        if(resume)try{afterAt=Long.parseLong(position[0]);}catch(NumberFormatException invalid){resume=false;}
        for(int pass=0;pass<2;pass++){
        String predicate=resume?" AND (f.created_at>? OR (f.created_at=? AND f.candidate_id>?))":"";
        String[] args=resume?new String[]{target,target,target,Long.toString(afterAt),Long.toString(afterAt),position[1]}:new String[]{target,target,target};
        try(Cursor c=db.rawQuery("SELECT f.candidate_id,f.structural_hash,f.created_issue,f.created_at FROM "+ResearchEvaluationRunR967.EVAL+" f "
                +"JOIN "+ResearchEvaluationRunR967.RUN+" run ON run.run_hash=f.run_hash JOIN r952_candidate parent ON parent.candidate_id=f.learner_id "
                +"WHERE f.created_issue<=? AND f.trained_until_issue<? AND run.first_issue=(SELECT MAX(rr.first_issue)FROM "+ResearchEvaluationRunR967.RUN
                +" rr WHERE rr.learner_id=f.learner_id AND rr.first_issue<=?) AND parent.structural_hash=run.ast_hash "
                +"AND parent.state NOT IN('QUALITY_PRUNED','QUARANTINED','RESOURCE_RETIRED') AND "+TrainingContinuationR973.predicate("f.rule_hash")
                +" AND NOT EXISTS(SELECT 1 FROM "+ResearchLifecycleR932.TABLE+" life WHERE life.candidate_key=('algorithm:'||f.candidate_id) "
                +"AND life.state IN('QUALITY_PRUNED','QUARANTINED','RESOURCE_RETIRED')) "
                +"AND NOT EXISTS(SELECT 1 FROM "+RESERVATION_CP11+" r WHERE r.candidate_key=('algorithm:'||f.candidate_id) AND r.state IN('RESERVED','ACTIVE')) "
                +predicate+" ORDER BY f.created_at,f.candidate_id LIMIT 32",args)){
            while(c.moveToNext())ids.add(new String[]{c.getString(0),c.getString(1),c.getString(2),Long.toString(c.getLong(3))});
        }
        if(!ids.isEmpty()||!resume)break;
        resume=false; // Wrap once; no second nonempty page or unbounded payload read.
        }
        for(String[] row:ids){
            String key="algorithm:"+row[0];
            if(!ResearchEvaluationRunR967.eligibleEvaluation(key,target)||com.ethk3.novaonepro.r952.BoundAtomLifecycleR80.sourceOnlyR80(db,row[0],target)||!com.ethk3.novaonepro.r952.BoundAtomLifecycleR80.frozenDependencyReasonR80(db,row[1],target).isEmpty()||!com.ethk3.novaonepro.r952.LongSummaryBindingsR80.frozenDependencyReasonR81(db,row[1],target).isEmpty())continue;
            ResearchMultiplicityR932.registerCandidate(db,key,"algorithm:R958_FIXED_POSITION_SOFTMAX",row[1],row[2]);
            String study=CandidateCalibrationR932.prepareReservedStudyCP11(db,key,row[1],championBundle,selectorHash,target),first=target;
            String state="COLLECTING";int captured=0;
            try(Cursor c=db.rawQuery("SELECT state,first_issue,captured_n FROM "+CandidateCalibrationR932.STUDY+" WHERE study_hash=?",new String[]{study})){
                if(c.moveToFirst()){state=c.getString(0);first=c.getString(1);captured=c.getInt(2);}
            }
            if(!("COLLECTING".equals(state)||"READY".equals(state)||"FROZEN".equals(state)))continue;
            if(captured<0||captured>241)throw new IllegalStateException("CAPTURE_COUNTER_INVALID");
            try(Cursor c=db.rawQuery("SELECT state FROM "+RESERVATION_CP11+" WHERE study_hash=?",new String[]{study})){if(c.moveToFirst())continue;}
            String next=target;
            if(captured>0){
                int observed=0;String expected=first,last="";
                try(Cursor c=db.rawQuery("SELECT target_issue,raw_score,prediction_hash FROM "+CandidateCalibrationR932.OBS+" WHERE study_hash=? ORDER BY target_issue LIMIT 242",new String[]{study})){
                    while(c.moveToNext()){
                        if(observed>=captured||!expected.equals(c.getString(0))||!Double.isFinite(c.getDouble(1))||!c.getString(2).matches("[0-9a-f]{64}"))
                            throw new IllegalStateException("CAPTURE_EXISTING_EVIDENCE_INVALID");
                        last=c.getString(0);expected=ResearchEvaluationRunR967.nextEvaluation(key,last);observed++;
                    }
                }
                if(observed!=captured)throw new IllegalStateException("CAPTURE_COUNTER_DRIFT");next=expected;
                if(captured<241&&next.compareTo(target)<0)throw new IllegalStateException("CAPTURE_EXISTING_GAP_NOT_RESTARTED");
            }
            if(captured<241&&occupied>=CAPTURE_ACTIVE_MAX_CP11)continue;
            db.execSQL("INSERT OR IGNORE INTO "+CandidateCalibrationR932.STUDY+" VALUES(?,?,0,0,'COLLECTING',?)",new Object[]{study,key,first});
            String id=V51ChecksumEngine.sha256("CP12_CAPTURE_RESERVATION|"+key+'|'+row[1]+'|'+study+'|'+first+'|'+CAPTURE_SCHEDULE_CP11);
            db.execSQL("INSERT INTO "+RESERVATION_CP11+"(reservation_id,candidate_key,freeze_hash,study_hash,first_issue,planned_n,schedule_version,cost_budget_us,state,next_issue,generation,last_reason,created_at) "
                    +"VALUES(?,?,?,?,?,241,?,?,?,?,0,'',?)",new Object[]{id,key,row[1],study,first,CAPTURE_SCHEDULE_CP11,CAPTURE_COST_US_CP11,captured==241?"COMPLETE":captured==0?"RESERVED":"ACTIVE",next,System.currentTimeMillis()});
            if(captured<241)occupied++;admitted++;
        }
        LinkedHashMap<String,String> state=new LinkedHashMap<>();state.put("cp11_capture_active",Integer.toString(occupied));
        state.put("cp11_capture_reservation_status",occupied>=CAPTURE_ACTIVE_MAX_CP11?"WAIT_CAPTURE_CAPACITY":"CAPTURE_CAPACITY_AVAILABLE");
        String[] last=ids.isEmpty()?null:ids.get(ids.size()-1);
        state.put(CAPTURE_SCAN_R74,last==null?"":last[3]+'|'+last[0]);
        DbStore.putStatesR36(db,state);
        return admitted;
    }
    private static int changesCP11(SQLiteDatabase db){try(Cursor c=db.rawQuery("SELECT changes()",null)){if(!c.moveToFirst())throw new IllegalStateException("CAPTURE_CAS_RESULT_MISSING");return c.getInt(0);}}
    private static void retireReservationCP11(SQLiteDatabase db,CaptureReservationCP11 r,String reason){
        db.execSQL("UPDATE "+RESERVATION_CP11+" SET state='RETIRED',last_reason=?,generation=generation+1 WHERE reservation_id=? AND generation=? AND state IN('RESERVED','ACTIVE')",
                new Object[]{reason,r.id,r.generation});if(changesCP11(db)!=1)throw new IllegalStateException("CAPTURE_RESERVATION_CAS_LOST");
        db.execSQL("INSERT OR IGNORE INTO r975_calibration_failure VALUES(?,?,?,?)",new Object[]{r.study,r.key,reason,System.currentTimeMillis()});
        db.execSQL("UPDATE "+CandidateCalibrationR932.STUDY+" SET state='BLOCKED_INVALID_INPUT' WHERE study_hash=? AND state='COLLECTING'",new Object[]{r.study});
    }
    /** Existing reservation owner releases only failed bound obligations, retaining
     * all original predictions, study observations, model and qualification evidence. */
    public static boolean retireInvalidBoundReservationR80(SQLiteDatabase db,CaptureReservationCP11 r,String target){
        if(!db.inTransaction()||r==null)throw new IllegalArgumentException("BOUND_RESERVATION_WRITER");
        String reason=com.ethk3.novaonepro.r952.BoundAtomLifecycleR80.frozenDependencyReasonR80(db,r.freeze,target);
        boolean longSource=false;
        if(reason.isEmpty()){reason=com.ethk3.novaonepro.r952.LongSummaryBindingsR80.frozenDependencyReasonR81(db,r.freeze,target);longSource=!reason.isEmpty();}
        if(reason.isEmpty())return false;
        retireReservationCP11(db,r,(longSource?"CAPTURE_LONG_DEPENDENCY:":"CAPTURE_BOUND_DEPENDENCY:")+reason);
        ResearchCaptureLedgerR932.record(db,target,r.key,"EXECUTION_FAILED",reason,"",0,false);return true;
    }
    public static void releaseInvalidBoundReservationsR80(SQLiteDatabase db,String target){
        if(!db.inTransaction())throw new IllegalArgumentException("BOUND_RESERVATION_WRITER");
        ArrayList<CaptureReservationCP11> rows=new ArrayList<>();try(Cursor c=db.rawQuery("SELECT reservation_id,candidate_key,freeze_hash,study_hash,first_issue,next_issue,state,generation FROM "+RESERVATION_CP11+" WHERE state IN('RESERVED','ACTIVE') ORDER BY reservation_id LIMIT 9",null)){while(c.moveToNext())rows.add(new CaptureReservationCP11(c));}
        if(rows.size()>CAPTURE_ACTIVE_MAX_CP11)throw new IllegalStateException("CAPTURE_CAPACITY_CORRUPT");
        for(CaptureReservationCP11 r:rows)retireInvalidBoundReservationR80(db,r,target);
    }
    /** A deterministic invalid fixed-graph output cannot monopolize unrelated reservations.
     * Keep its original study/evidence, do not turn the failure into a missing-sample retry. */
    static void rejectInvalidScoreR75(SQLiteDatabase db,CaptureReservationCP11 r,String target,String reason){
        if(!db.inTransaction()||r==null||!target.equals(r.next)||!"NEGATIVE_GRAPH_SCORE".equals(reason))
            throw new IllegalArgumentException("CAPTURE_SCORE_REJECTION_IDENTITY");
        retireReservationCP11(db,r,"CAPTURE_INVALID_SCORE:"+reason);
        ResearchCaptureLedgerR932.record(db,target,r.key,"EXECUTION_FAILED",reason,"",0,false);
    }
    private static void markReservationGapCP11(SQLiteDatabase db,CaptureReservationCP11 r,String target,String reason){
        db.execSQL("INSERT OR IGNORE INTO research_capture_gap_v2 VALUES(?,?,?,?,?,?)",new Object[]{r.study,r.next,target,reason,r.id,System.currentTimeMillis()});
        db.execSQL("INSERT OR IGNORE INTO r975_calibration_failure VALUES(?,?,'R969_CAL_EVALUATION_SCHEDULE_GAP',?)",new Object[]{r.study,r.key,System.currentTimeMillis()});
        db.execSQL("UPDATE "+CandidateCalibrationR932.STUDY+" SET state='BLOCKED_INVALID_INPUT' WHERE study_hash=? AND state='COLLECTING'",new Object[]{r.study});
        db.execSQL("UPDATE "+RESERVATION_CP11+" SET state='FAILED_GAP',last_reason=?,generation=generation+1 WHERE reservation_id=? AND generation=? AND next_issue=? AND state IN('RESERVED','ACTIVE')",
                new Object[]{reason,r.id,r.generation,r.next});if(changesCP11(db)!=1)throw new IllegalStateException("CAPTURE_RESERVATION_CAS_LOST");
    }
    /** Resource HOLD preserves the scheduled issue and actual evidence N; a missed issue becomes a truthful gap. */
    static void holdReservationCP12(SQLiteDatabase db,CaptureReservationCP11 r,String target,String reason){
        if(!db.inTransaction()||r==null||!target.equals(r.next)||!"MEMORY_PRESSURE".equals(reason))
            throw new IllegalStateException("CAPTURE_HOLD_IDENTITY");
        db.execSQL("UPDATE "+RESERVATION_CP11+" SET last_reason=?,generation=generation+1 WHERE reservation_id=? AND generation=? AND next_issue=? AND state IN('RESERVED','ACTIVE')",
                new Object[]{reason,r.id,r.generation,target});
        if(changesCP11(db)!=1)throw new IllegalStateException("CAPTURE_RESERVATION_CAS_LOST");
    }
    public static List<CaptureReservationCP11> dueReservationsCP11(SQLiteDatabase db,String target){
        if(db==null||!TargetIssueResolver.isValid(target))throw new IllegalArgumentException("CAPTURE_DUE_INPUT");
        ArrayList<CaptureReservationCP11> out=new ArrayList<>();
        try(Cursor c=db.rawQuery("SELECT reservation_id,candidate_key,freeze_hash,study_hash,first_issue,next_issue,state,generation FROM "+RESERVATION_CP11
                +" WHERE state IN('RESERVED','ACTIVE') AND next_issue=? ORDER BY CASE state WHEN 'ACTIVE' THEN 0 ELSE 1 END,created_at,reservation_id LIMIT 9",new String[]{target})){
            while(c.moveToNext())out.add(new CaptureReservationCP11(c));
        }
        if(out.size()>CAPTURE_ACTIVE_MAX_CP11)throw new IllegalStateException("CAPTURE_CAPACITY_CORRUPT");
        return Collections.unmodifiableList(out);
    }
    /** Observation insert, counter increment and this fenced advance are one transaction. */
    static void completeReservationCaptureCP11(SQLiteDatabase db,CaptureReservationCP11 r,String key,String freeze,String study,String target,int captured){
        if(!db.inTransaction()||r==null||!key.equals(r.key)||!freeze.equals(r.freeze)||!study.equals(r.study)||!target.equals(r.next)||captured<1||captured>241)
            throw new IllegalStateException("CAPTURE_RESERVATION_COMMIT_IDENTITY");
        String next=ResearchEvaluationRunR967.nextEvaluation(key,target);
        db.execSQL("UPDATE "+RESERVATION_CP11+" SET next_issue=?,state=?,generation=generation+1,last_reason='' WHERE reservation_id=? AND generation=? AND next_issue=? AND state IN('RESERVED','ACTIVE')",
                new Object[]{next,captured==241?"COMPLETE":"ACTIVE",r.id,r.generation,target});
        if(changesCP11(db)!=1)throw new IllegalStateException("CAPTURE_RESERVATION_CAS_LOST");
    }
    static CaptureReservationCP11 reservationForStudyCP11(SQLiteDatabase db,String study){
        try(Cursor c=db.rawQuery("SELECT reservation_id,candidate_key,freeze_hash,study_hash,first_issue,next_issue,state,generation FROM "+RESERVATION_CP11+" WHERE study_hash=?",new String[]{study})){
            return c.moveToFirst()?new CaptureReservationCP11(c):null;
        }
    }
    public static void beginShared(DbStore store,String target,String input){
        synchronized(CACHE){Shared shared=CACHE.get(store);if(shared==null||!shared.target.equals(target)||!shared.input.equals(input)){shared=new Shared(target,input);CACHE.put(store,shared);}SHARED.set(shared);}
    }
    public static void endShared(){SHARED.remove();}
    static void releaseCompleted(DbStore store,String target){
        synchronized(CACHE){Shared old=CACHE.get(store);if(old!=null&&old.target.equals(target)){CACHE.remove(store);if(SHARED.get()==old)SHARED.remove();}}
        AlgorithmInventionLabR908.releasePredrawR964(store,target);
        GenesisScanR967.releaseTarget(store,target);
    }
    static boolean retainedForTestsR964(DbStore store,String target){synchronized(CACHE){Shared s=CACHE.get(store);return s!=null&&s.target.equals(target);}}
    private static final class Shared {
        final String target,input;HistoricalStrategyInventionR908.Profile profile;
        GenesisAssetVmR915.PredrawContext[] contexts;
        boolean atomRecorded;
        Shared(String target,String input){this.target=target;this.input=input;}
    }
    public static void captureAtoms(final DbStore store,DeferredEvidenceSnapshotR36.Snapshot snapshot) {
        Shared shared=SHARED.get();if(shared!=null&&shared.atomRecorded)return;
        HistoricalStrategyInventionR908.Profile profile=profile(store,snapshot.baseIssue);
        if(profile==null||!profile.ready)return;
        int index=Math.floorMod(snapshot.targetIssue.hashCode(),1200);
        int historyRows;
        try(Cursor c=BudgetedQueryR927.query(store.readable(),"SELECT COUNT(*) FROM "+DbHelper.TABLE_DRAWS+" WHERE dirty=0 AND issue<=?",new String[]{snapshot.baseIssue})){historyRows=c.moveToFirst()?c.getInt(0):0;}
        GenesisAssetVmR915.PredrawContext context=shared==null||shared.contexts==null?null:shared.contexts[index];
        if(context==null)context=GenesisAssetVmR915.buildContext(profile,snapshot,historyRows,snapshot.positionActions()[index]);
        if(context==null)return;
        final GenesisAssetVmR915.PredrawContext captured=context;
        final GenesisAssetVmR915.PredrawContext replay=GenesisAssetVmR915.buildContext(profile,snapshot,historyRows,snapshot.positionActions()[index]);
        boolean ok=store.coordinatorR36().runAtomicWrite("R932_INDEPENDENT_ATOM_CAPTURE",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase db){
            if(drawn(db,captured.targetIssue))throw new IllegalStateException("R932_ATOM_CONTEXT_POSTDRAW");
            if(AtomAdmissionAuditR932.record(db,captured,replay))AtomCaptureR932.freeze(db,captured);
        }});
        if(!ok)throw new IllegalStateException("R932_ATOM_CONTEXT_COMMIT_FAILED");
        if(shared!=null){if(shared.contexts==null)shared.contexts=new GenesisAssetVmR915.PredrawContext[1200];shared.contexts[index]=context;shared.atomRecorded=true;}
    }
    public static GenesisAssetVmR915.PredrawContext[] contexts(DbStore store,DeferredEvidenceSnapshotR36.Snapshot snapshot,
            HistoricalStrategyInventionR908.Profile profile,V51Models.PositionAction[] actions,int historyRows) {
        Shared shared=SHARED.get();
        if(shared!=null&&!snapshot.targetIssue.equals(shared.target))throw new IllegalArgumentException("R932_SHARED_CONTEXT_TARGET");
        GenesisAssetVmR915.PredrawContext[] out=shared==null?new GenesisAssetVmR915.PredrawContext[1200]:shared.contexts;
        if(out==null){out=new GenesisAssetVmR915.PredrawContext[1200];shared.contexts=out;}
        // Each completed action is immutable. A cooperative yield preserves completed work.
        for(int i=0;i<out.length;i++){
            ResearchWorkBudgetR927.checkpoint();
            if(out[i]==null)out[i]=GenesisAssetVmR915.buildContext(profile,snapshot,historyRows,actions[i]);
            if(out[i]==null)throw new IllegalStateException("R932_SHARED_CONTEXT_UNAVAILABLE");
        }
        return out;
    }
    public static HistoricalStrategyInventionR908.Profile profile(DbStore store,String base) {
        Shared shared=SHARED.get();
        if(shared==null)return HistoricalStrategyInventionR908.buildProfileAt(store,base);
        if(!TargetIssueResolver.exact(base,shared.target).valid)throw new IllegalArgumentException("R932_SHARED_PROFILE_PIT");
        if(shared.profile==null)shared.profile=HistoricalStrategyInventionR908.buildProfileAt(store,base);
        return shared.profile;
    }
    /** Owner-side roster progress: one bounded registration page per outer predraw retry. */
    public static void prepare(final DbStore store,final String target,final String inputHash) {
        StagedResearchRosterR943.prepareSlice(store,target,inputHash);
    }

    /**
     * R952 execution-first owner gate. A committed prefix must receive at least one execution
     * attempt per selected member before the owner spends another retry registering more names.
     * This prevents a slow phone from growing cursor=32,64,... while execution_started remains 0.
     * It does not require success: once every admitted row has started at least once, the next
     * registration page may be published and failed candidates remain eligible for retries.
     *
     * @return true when a roster page was advanced (or the final PLAN already exists), false when
     *         registration was deliberately deferred in favor of execution.
     */
    public static boolean prepareExecutionFirst(final DbStore store,final String target,final String inputHash) {
        if (hasFinalPlan(store,target,inputHash)) {
            try {
                store.setState("r952_roster_owner_decision", "COMPLETE_PLAN_ALREADY_PUBLISHED");
                store.setState("r952_roster_owner_target", target);
            } catch (Throwable ignored) {}
            return true;
        }
        if (hasAdmittedPrefix(store,target,inputHash) && hasUnstartedAdmitted(store,target)) {
            try {
                store.setState("r952_roster_owner_decision", "DEFER_REGISTRATION_UNSTARTED_ADMITTED");
                store.setState("r952_roster_owner_target", target);
            } catch (Throwable ignored) {}
            return false;
        }
        StagedResearchRosterR943.prepareSlice(store,target,inputHash);
        try {
            store.setState("r952_roster_owner_decision", "ADVANCED_OR_COMPLETE");
            store.setState("r952_roster_owner_target", target);
        } catch (Throwable ignored) {}
        return true;
    }

    private static boolean hasFinalPlan(DbStore store,String target,String inputHash) {
        if(store==null||!TargetIssueResolver.isValid(target)||inputHash==null||!inputHash.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("R952_ROSTER_OWNER_INPUT");
        try(Cursor plan=store.readable().rawQuery("SELECT input_hash FROM "+PLAN+" WHERE target_issue=?",new String[]{target})) {
            if(!plan.moveToFirst())return false;
            if(!inputHash.equals(plan.getString(0)))throw new IllegalStateException("R952_ROSTER_INPUT_DRIFT");
            return true;
        } catch(android.database.sqlite.SQLiteException notReady) { return false; }
    }

    /** Returns a bit mask of admitted, selected lanes that still lack a frozen prediction. */
    public static int pendingLaneMask(DbStore store,String target) {
        if(store==null||!TargetIssueResolver.isValid(target))return 0;
        int mask=0;
        SQLiteDatabase db=store.readable();
        String sql="SELECT sq.candidate_key FROM "+TASK+" sq JOIN "+ResearchCaptureLedgerR932.TABLE
                +" cap ON cap.target_issue=sq.target_issue AND cap.candidate_key=sq.candidate_key"
                +" WHERE sq.target_issue=? AND sq.selected=1 AND cap.frozen=0 AND "+admittedPage("sq");
        try(Cursor c=db.rawQuery(sql,new String[]{target})) {
            while(c.moveToNext()) {
                String key=c.getString(0);
                if(key==null)continue;
                if(key.startsWith("tiny:"))mask|=1;
                else if(key.startsWith("algorithm:"))mask|=2;
                else if(key.startsWith("genesis:"))mask|=4;
                else if(key.startsWith("formula:"))mask|=8;
                if(mask==15)break;
            }
        } catch(android.database.sqlite.SQLiteException notReady) { return 0; }
        return mask;
    }

    private static boolean hasUnstartedAdmitted(DbStore store,String target) {
        SQLiteDatabase db=store.readable();
        String sql="SELECT 1 FROM "+TASK+" sq JOIN "+ResearchCaptureLedgerR932.TABLE
                +" cap ON cap.target_issue=sq.target_issue AND cap.candidate_key=sq.candidate_key"
                +" WHERE sq.target_issue=? AND sq.selected=1 AND cap.status='SELECTED' AND cap.execution_started=0 AND cap.frozen=0 AND "
                +admittedPage("sq")+" LIMIT 1";
        try(Cursor c=db.rawQuery(sql,new String[]{target})) { return c.moveToFirst(); }
        catch(android.database.sqlite.SQLiteException notReady) { return false; }
    }

    /**
     * Consumer-side roster guard.  A lane may be invoked directly by a recovery/test path, so it
     * still self-primes when no admitted page exists.  Once the outer predraw owner has committed
     * any immutable prefix, consumers must only consume that prefix; they must not spend their
     * execution slice advancing another registration page.
     */
    public static void prepareConsumer(final DbStore store,final String target,final String inputHash) {
        if (hasAdmittedPrefix(store,target,inputHash)) return;
        StagedResearchRosterR943.prepareSlice(store,target,inputHash);
    }

    private static boolean hasAdmittedPrefix(DbStore store,String target,String inputHash) {
        if(store==null||!TargetIssueResolver.isValid(target)||inputHash==null||!inputHash.matches("[0-9a-f]{64}"))
            throw new IllegalArgumentException("R951_ROSTER_CONSUMER_INPUT");
        SQLiteDatabase db=store.readable();
        try(Cursor plan=db.rawQuery("SELECT input_hash FROM "+PLAN+" WHERE target_issue=?",new String[]{target})){
            if(plan.moveToFirst()){
                if(!inputHash.equals(plan.getString(0)))throw new IllegalStateException("R951_ROSTER_INPUT_DRIFT");
                return true;
            }
        }catch(android.database.sqlite.SQLiteException notReady){
            return false;
        }
        try(Cursor page=db.rawQuery(
                "SELECT 1 FROM "+StagedResearchRosterR943.TABLE+" p JOIN "+StagedResearchRosterR943.MANIFEST
                        +" m ON m.target_issue=p.target_issue AND m.input_hash=p.input_hash AND m.payload_hash=p.payload_hash"
                        +" WHERE p.target_issue=? AND p.input_hash=? AND p.cursor>0 AND p.cursor<=m.candidate_count LIMIT 1",
                new String[]{target,inputHash})){
            return page.moveToFirst();
        }catch(android.database.sqlite.SQLiteException notReady){
            return false;
        }
    }
    static void add(SQLiteDatabase db,List<Candidate> out,String target,String key,String family,String structure,String tier){
        try(Cursor c=db.rawQuery("SELECT h.family FROM "+ResearchMultiplicityR932.IDENTITY+" h WHERE h.candidate_key=?",new String[]{key})){if(c.moveToFirst())family=c.getString(0);}
        ResearchLifecycleR932.enroll(db,key,structure,"","C".equals(tier)?"C":"B");
        tier=ResearchLifecycleR932.tier(db,key,tier);if(tier.isEmpty()){if(!PairedResearchChallengeR932.requiresReference(db,key,target))return;tier="B";}
        String incumbent=ResearchCohortR932.champion(db,family);if(tier.equals("A")&&!incumbent.isEmpty()&&!incumbent.equals(key))tier="B";
        String last="";
        try(Cursor c=db.rawQuery("SELECT target_issue FROM "+TASK+" WHERE candidate_key=? AND selected=1 ORDER BY target_issue DESC LIMIT 1",new String[]{key})){if(c.moveToFirst())last=c.getString(0);}
        out.add(new Candidate(key,family,structure,tier,last));
    }
    static String tier(String state){return state.contains("CERTIFIED")?"A":state.contains("IDEA")||state.equals("NURSERY")?"C":"B";}
    static void freeze(SQLiteDatabase db,String target,String input,List<Candidate> candidates) {
        if(!db.inTransaction())throw new IllegalStateException("R932_ROSTER_TRANSACTION_REQUIRED");
        schema(db);
        if(drawn(db,target))throw new IllegalStateException("R932_ROSTER_POSTDRAW");
        try(Cursor c=db.rawQuery("SELECT input_hash FROM "+PLAN+" WHERE target_issue=?",new String[]{target})){
            if(c.moveToFirst()){if(!input.equals(c.getString(0)))throw new IllegalStateException("R932_ROSTER_INPUT_DRIFT");return;}
        }
        HashSet<String> keys=new HashSet<String>();
        for(Candidate candidate:candidates){
            if(!keys.add(candidate.key))throw new IllegalArgumentException("R932_DUPLICATE_CANDIDATE");
            try(Cursor c=db.rawQuery("SELECT family,structure_hash FROM "+IDENTITY+" WHERE candidate_key=?",new String[]{candidate.key})){
                if(c.moveToFirst()&&(!candidate.family.equals(c.getString(0))||!candidate.structure.equals(c.getString(1))))throw new IllegalStateException("R932_CANDIDATE_IDENTITY_CHANGED");
            }
            db.execSQL("INSERT OR IGNORE INTO "+IDENTITY+" VALUES(?,?,?,?)",new Object[]{candidate.key,candidate.family,candidate.structure,target});
            ResearchMultiplicityR932.registerCandidate(db,candidate.key,candidate.family,candidate.structure,target);
            ResearchLifecycleR932.enroll(db,candidate.key,candidate.structure,target,candidate.tier);
            try(Cursor identity=db.rawQuery("SELECT 1 FROM "+ResearchCohortR932.IDENTITY+" WHERE candidate_key=?",new String[]{candidate.key})){
                if(!identity.moveToFirst())ResearchCohortR932.register(db,candidate.key,candidate.structure,candidate.structure,"GLOBAL_ALL_POSITIONS",target);
            }
        }
        Map<String,Integer> chosen=choose(target,candidates);
        Collections.sort(candidates,(a,b)->a.key.compareTo(b.key));StringBuilder canonical=new StringBuilder(target).append('|').append(input);
        for(Candidate c:candidates){
            ResearchWorkBudgetR927.checkpoint();int selected=chosen.containsKey(c.key)?1:0,rank=selected==1?chosen.get(c.key):-1;
            db.execSQL("INSERT INTO "+TASK+" VALUES(?,?,?,?,?,?,?,?)",new Object[]{target,c.key,c.family,c.structure,c.tier,selected,rank,c.tier.equals("C")?"PROXY120":"FULL1200"});
            ResearchCaptureLedgerR932.record(db,target,c.key,selected==1?"SELECTED":"NOT_SELECTED","R959_BOUNDED_FAIR_QUEUE_TIER_"+c.tier,"",0,false);
            canonical.append('|').append(c.key).append(':').append(c.structure).append(':').append(c.family).append(':').append(c.tier).append(':').append(selected).append(':').append(rank);
        }
        db.execSQL("INSERT INTO "+PLAN+" VALUES(?,?,?,?)",new Object[]{target,input,V51ChecksumEngine.sha256(canonical.toString()),System.currentTimeMillis()});
    }
    /** Single deterministic admission owner: family rotation, age and structural deduplication. */
    static Map<String,Integer> choose(String target,List<Candidate> candidates) {
        Map<String,Integer> chosen=new HashMap<String,Integer>();
        Map<String,String> lastByStructure=new HashMap<String,String>();
        for(Candidate c:candidates){String last=lastByStructure.get(c.structure);if(last==null||last.compareTo(c.last)<0)lastByStructure.put(c.structure,c.last);}
        Set<String> representative=new HashSet<String>();
        ArrayList<Candidate> stable=new ArrayList<Candidate>(candidates);Collections.sort(stable,(a,b)->{int tier=a.tier.compareTo(b.tier);return tier!=0?tier:a.key.compareTo(b.key);});
        Set<String> structures=new HashSet<String>();
        for(Candidate c:stable)if(structures.add(c.structure))representative.add(c.key);
        int totalRank=0;
        for(String tier:Arrays.asList("A","B","C")){
            TreeMap<String,ArrayList<Candidate>> families=new TreeMap<String,ArrayList<Candidate>>();
            for(Candidate c:candidates)if(c.tier.equals(tier)&&representative.contains(c.key)){
                if(!families.containsKey(c.family))families.put(c.family,new ArrayList<Candidate>());families.get(c.family).add(c);
            }
            for(ArrayList<Candidate> family:families.values())Collections.sort(family,(a,b)->{int cmp=lastByStructure.get(a.structure).compareTo(lastByStructure.get(b.structure));return cmp!=0?cmp:a.key.compareTo(b.key);});
            ArrayList<String> order=new ArrayList<String>(families.keySet());
            Collections.sort(order,(a,b)->{int cmp=lastByStructure.get(families.get(a).get(0).structure).compareTo(lastByStructure.get(families.get(b).get(0).structure));return cmp!=0?cmp:V51ChecksumEngine.sha256(target+'|'+a).compareTo(V51ChecksumEngine.sha256(target+'|'+b));});
            int cap=tier.equals("A")?8:tier.equals("B")?EVALUATION_CAP_R963:64,rank=0,round=0;
            while(rank<cap){boolean added=false;for(String family:order){if(rank>=cap)break;ArrayList<Candidate> rows=families.get(family);if(round<rows.size()){chosen.put(rows.get(round).key,totalRank++);rank++;added=true;}}if(!added)break;round++;}
        }
        return chosen;
    }

    /** Task membership must be committed with a verified page of the immutable full manifest. */
    static String admittedPage(String alias) {
        return "((NOT EXISTS(SELECT 1 FROM "+StagedResearchRosterR943.MANIFEST+" absent WHERE absent.target_issue="+alias+".target_issue) AND EXISTS(SELECT 1 FROM "+PLAN+" sp WHERE sp.target_issue="+alias+".target_issue)) OR EXISTS(SELECT 1 FROM "+StagedResearchRosterR943.MANIFEST+" m948 JOIN "+StagedResearchRosterR943.TABLE+" p948 ON p948.target_issue=m948.target_issue AND p948.input_hash=m948.input_hash AND p948.payload_hash=m948.payload_hash WHERE m948.target_issue="+alias+".target_issue AND "+alias+".queue_rank>=0 AND "+alias+".queue_rank<p948.cursor AND "+alias+".queue_rank<m948.candidate_count AND EXISTS(SELECT 1 FROM "+IDENTITY+" i948 WHERE i948.candidate_key="+alias+".candidate_key AND i948.structure_hash="+alias+".structure_hash)))";
    }
    public static String predicate(String target,String keyExpression) {
        if(!TargetIssueResolver.isValid(target))throw new IllegalArgumentException("R932_ROSTER_TARGET");
        return " AND EXISTS(SELECT 1 FROM "+TASK+" sq WHERE sq.target_issue='"+target+"' AND sq.candidate_key="+keyExpression+" AND sq.selected=1 AND "+admittedPage("sq")+" AND NOT EXISTS(SELECT 1 FROM "+PROXY+" px WHERE px.target_issue=sq.target_issue AND px.candidate_key=sq.candidate_key))";
    }
    public static boolean proxy(DbStore store,String target,String key){
        try(Cursor c=store.readable().rawQuery("SELECT tier,execution_mode FROM "+TASK+" sq WHERE target_issue=? AND candidate_key=? AND selected=1 AND "+admittedPage("sq"),new String[]{target,key})){
            if(!c.moveToFirst())throw new IllegalStateException("R932_CANDIDATE_NOT_SCHEDULED");return "C".equals(c.getString(0))&&"PROXY120".equals(c.getString(1));
        }
    }
    public static boolean saveProxy(final DbStore store,final String target,final String key,final String input,final int ordinal,final double score) {
        if(!proxy(store,target,key))throw new IllegalArgumentException("R959_PROXY_NOT_ADMITTED");
        if(!Double.isFinite(score)||ordinal<0||ordinal>=1200)throw new IllegalArgumentException("R932_PROXY_OUTPUT");
        return store.coordinatorR36().runAtomicWrite("R932_NURSERY_PROXY",new DatabaseCoordinator.WriteCallable<Boolean>(){public Boolean call(SQLiteDatabase db){
            if(drawn(db,target))return Boolean.FALSE;
            String hash=V51ChecksumEngine.sha256("R932_PROXY120|"+target+'|'+key+'|'+input+'|'+ordinal+'|'+Double.toHexString(score));
            try(Cursor c=db.rawQuery("SELECT record_hash FROM "+PROXY+" WHERE target_issue=? AND candidate_key=?",new String[]{target,key})){
                if(c.moveToFirst()){if(!hash.equals(c.getString(0)))throw new IllegalStateException("R932_PROXY_FREEZE_CONFLICT");return Boolean.TRUE;}
            }
            db.execSQL("INSERT INTO "+PROXY+" VALUES(?,?,?,?,?,?,?,?)",new Object[]{target,key,input,ordinal,score,120,hash,System.currentTimeMillis()});
            ResearchCaptureLedgerR932.record(db,target,key,"FROZEN","NURSERY_PROXY120_NO_PROMOTION_OR_CALIBRATION",hash,0,false);return Boolean.TRUE;
        }},Boolean.FALSE);
    }
    private static boolean drawn(SQLiteDatabase db,String target){try(Cursor c=db.rawQuery("SELECT 1 FROM "+DbHelper.TABLE_DRAWS+" WHERE issue>=? LIMIT 1",new String[]{target})){return c.moveToFirst();}}

public static int pendingLaneMaskSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store,String target) {
        if(store==null||!TargetIssueResolver.isValid(target))return 0;
        int mask=0;
        SQLiteDatabase db=store.readable();
        String sql="SELECT sq.candidate_key FROM "+TASK+" sq JOIN "+ResearchCaptureLedgerR932.TABLE
                +" cap ON cap.target_issue=sq.target_issue AND cap.candidate_key=sq.candidate_key"
                +" WHERE sq.target_issue=? AND sq.selected=1 AND cap.frozen=0 AND "+admittedPageSnapshotCP4(report, "sq");
        try(Cursor c=report.select(sql,new String[]{target})) {
            while(c.moveToNext()) {
                String key=c.getString(0);
                if(key==null)continue;
                if(key.startsWith("tiny:"))mask|=1;
                else if(key.startsWith("algorithm:"))mask|=2;
                else if(key.startsWith("genesis:"))mask|=4;
                else if(key.startsWith("formula:"))mask|=8;
                if(mask==15)break;
            }
        } catch(android.database.sqlite.SQLiteException notReady) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",notReady);}
        return mask;
    }

static String admittedPageSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String alias) {
        return "((NOT EXISTS(SELECT 1 FROM "+StagedResearchRosterR943.MANIFEST+" absent WHERE absent.target_issue="+alias+".target_issue) AND EXISTS(SELECT 1 FROM "+PLAN+" sp WHERE sp.target_issue="+alias+".target_issue)) OR EXISTS(SELECT 1 FROM "+StagedResearchRosterR943.MANIFEST+" m948 JOIN "+StagedResearchRosterR943.TABLE+" p948 ON p948.target_issue=m948.target_issue AND p948.input_hash=m948.input_hash AND p948.payload_hash=m948.payload_hash WHERE m948.target_issue="+alias+".target_issue AND "+alias+".queue_rank>=0 AND "+alias+".queue_rank<p948.cursor AND "+alias+".queue_rank<m948.candidate_count AND EXISTS(SELECT 1 FROM "+IDENTITY+" i948 WHERE i948.candidate_key="+alias+".candidate_key AND i948.structure_hash="+alias+".structure_hash)))";
    }
}
