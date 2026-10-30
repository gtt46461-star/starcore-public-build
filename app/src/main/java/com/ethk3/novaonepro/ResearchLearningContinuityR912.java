package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONObject;

/**
 * The existing research worker owns 22 bounded maintenance lanes. Their durable review tasks
 * coalesce newer watermarks while retaining unfinished older work. A review receipt is NOT
 * evidence that a model trained or passed; only the individual business owners can prove that.
 */
public final class ResearchLearningContinuityR912 {
    public static final int CONTINUITY_LANES=22;
    private static final String NEXT_LANE="r962_research_next_lane";
    private ResearchLearningContinuityR912(){}
    public static Result pulse(DbStore store,long budgetMs) {
        return pulseOwned(store,budgetMs,false);
    }
    public static Result pulseOnResearchPool(DbStore store,long budgetMs){return pulseOwned(store,budgetMs,true);}
    private static Result pulseOwned(DbStore store,long budgetMs,boolean pool){
        String blocked=com.ethk3.novaonepro.strategyv2.LegacyCapabilityAdapterR29.enterContinuity(store,pool);
        if(blocked!=null)return Result.fail(blocked);
        ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(Math.min(1000L,Math.max(1L,budgetMs)));
        try{return pulseWithinR927(store,budgetMs);}
        catch(ResearchWorkBudgetR927.Yield y){return Result.fail("YIELD:"+y.getMessage());}
        finally{ResearchWorkBudgetR927.leave(scope);com.ethk3.novaonepro.strategyv2.LegacyCapabilityAdapterR29.leaveContinuity();}
    }
    static String latest(DbStore store) {
        try(Cursor c=store.readable().rawQuery("SELECT issue FROM "+DbHelper.TABLE_DRAWS+" WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)){return c.moveToFirst()?c.getString(0):"";}
    }
    /** One active review per lane; desired watermark may advance, immutable task inputs may not. */
    static void ensureDesired(SQLiteDatabase db,String issue,boolean unservedDev,boolean unservedOutbox) {
        if(!db.inTransaction()||!TargetIssueResolver.isValid(issue))throw new IllegalArgumentException("R967_LANE_TRANSACTION_OR_ISSUE");
        ResearchTaskQueueR967.schema(db);
        // R971: two bounded reads, not 22 repetitions of insert/select/update on every pulse.
        java.util.Map<Integer,String[]> heads=new java.util.HashMap<Integer,String[]>();
        java.util.Map<String,String> activeTasks=new java.util.HashMap<String,String>();
        try(Cursor c=db.rawQuery("SELECT lane,desired_issue,served_issue,next_sequence,active_task_id FROM "+ResearchTaskQueueR967.LANE,null)) {
            while(c.moveToNext())heads.put(c.getInt(0),new String[]{c.getString(1),c.getString(2),c.getString(3),c.getString(4)});
        }
        try(Cursor c=db.rawQuery("SELECT candidate_id,task_id FROM "+ResearchTaskQueueR967.TABLE+" WHERE kind='MAINTENANCE' AND status NOT IN ('COMPLETED','EXPIRED','FAILED_TERMINAL')",null)) {
            while(c.moveToNext())activeTasks.put(c.getString(0),c.getString(1));
        }
        // An active task already owns this work, and a newer watermark is due anyway.
        // Do not re-scan its evidence queues while holding the scheduler claim transaction.
        boolean windowDue=needsReopen(heads,activeTasks,6,issue)&&exists(db,FixedResearchWindowR932.WINDOW)&&FixedResearchWindowR932.hasRunnableWork(db);
        boolean foundationDue=needsReopen(heads,activeTasks,13,issue)&&hasUnfinishedFoundationCP12(db);
        boolean publicationDue=needsReopen(heads,activeTasks,9,issue)&&exists(db,FixedResearchWindowR932.WINDOW)&&FixedWindowCertificatePublisherR932.hasRunnableWork(db);
        boolean moduleDue=needsReopen(heads,activeTasks,7,issue)&&exists(db,DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB)&&ModuleAblationR932.hasRunnableWork(db);
        long now=System.currentTimeMillis();
        for(int lane=0;lane<CONTINUITY_LANES;lane++) {
            String[] head=heads.get(lane);
            if(head==null){
                db.execSQL("INSERT INTO "+ResearchTaskQueueR967.LANE+"(lane,desired_issue,updated_at) VALUES(?,?,?)",new Object[]{lane,issue,now});
                head=new String[]{issue,"","0",""};
            }
            String desired=head[0],served=head[1];long next=Long.parseLong(head[2]);
            if(desired.compareTo(issue)<0){desired=issue;db.execSQL("UPDATE "+ResearchTaskQueueR967.LANE+" SET desired_issue=?,updated_at=? WHERE lane=?",new Object[]{desired,now,lane});}
            String candidate="lane:"+lane,active=activeTasks.get(candidate);
            if(active!=null&&!active.isEmpty()){
                if(!active.equals(head[3]))db.execSQL("UPDATE "+ResearchTaskQueueR967.LANE+" SET active_task_id=? WHERE lane=?",new Object[]{active,lane});
                continue;
            }
            boolean due=served.compareTo(desired)<0||(lane==18&&unservedDev)||(lane==13&&unservedOutbox)
                    ||(lane==6&&windowDue)||(lane==9&&publicationDue)||(lane==7&&moduleDue)||(lane==13&&foundationDue);
            if(!due)continue;
            if(next<0||next==Long.MAX_VALUE)throw new IllegalStateException("R967_LANE_SEQUENCE_EXHAUSTED");
            String seq=Long.toString(next+1);
            String input=V51ChecksumEngine.sha256("R967_MAINTENANCE_REVIEW_ONLY|"+candidate+'|'+desired+'|'+seq);
            String id=ResearchTaskQueueR967.enqueue(db,ResearchTaskQueueR967.MAINTENANCE,candidate,desired,seq,input,"MAINTENANCE_WATERMARK:"+desired);
            db.execSQL("UPDATE "+ResearchTaskQueueR967.LANE+" SET active_task_id=?,next_sequence=?,updated_at=? WHERE lane=? AND next_sequence=?",new Object[]{id,next+1,now,lane,next});
            if(ResearchTaskQueueR967.changed(db)!=1)throw new IllegalStateException("R967_LANE_SEQUENCE_CAS");
        }
    }
    private static boolean needsReopen(java.util.Map<Integer,String[]> heads,
            java.util.Map<String,String> active,int lane,String issue) {
        String[] head=heads.get(lane);
        return head!=null&&!active.containsKey("lane:"+lane)&&head[1].compareTo(issue)>=0;
    }
    static ResearchTaskQueueR967.Lease claimNext(SQLiteDatabase db,long now) { return claimNext(db,now,false); }
    static ResearchTaskQueueR967.Lease claimNext(SQLiteDatabase db,long now,boolean prioritizeTraining) {
        int start=0,turn=0;
        try(Cursor c=db.rawQuery("SELECT state_key,state_value FROM "+DbHelper.TABLE_APP_STATE+" WHERE state_key IN (?,?)",new String[]{NEXT_LANE,"r971_maintenance_turn"})){
            while(c.moveToNext())if(NEXT_LANE.equals(c.getString(0)))start=Integer.parseInt(c.getString(1));else turn=Integer.parseInt(c.getString(1));
        }
        if(start<0||start>=CONTINUITY_LANES||turn<0||turn>=2)throw new IllegalStateException("R967_LANE_CURSOR_INVALID");
        // Half of READY turns remain the original all-lane round robin. The other
        // half rotates the actual data consumers: online training, historical training,
        // calibration labels and predraw labels. Previously only the first two had a
        // reserved slot, so producers could outrun calibration indefinitely.
        // An empty import registry has no online training/outbox rows yet. It still needs
        // the existing reserved Director turn; otherwise bootstrapping receives only the
        // slow22-lane rotation and can never make training runnable promptly.
        if((prioritizeTraining||hasUnfinishedFoundationCP12(db))&&turn==1){
            final int[] hotLanes={13,6,13,9,13,18,13,1};
            int next=0;
            try(Cursor c=db.rawQuery("SELECT state_value FROM "+DbHelper.TABLE_APP_STATE+" WHERE state_key='r980_hot_lane_cursor'",null)) {
                if(c.moveToFirst())next=Integer.parseInt(c.getString(0));
            }
            if(next<0||next>=hotLanes.length)throw new IllegalStateException("R980_HOT_CURSOR_INVALID");
            for(int offset=0;offset<hotLanes.length;offset++) {
                int index=(next+offset)%hotLanes.length;
                ResearchTaskQueueR967.Lease hot=ResearchTaskQueueR967.claimFor(db,ResearchTaskQueueR967.MAINTENANCE,"lane:"+hotLanes[index],now);
                if(hot!=null){
                    state(db,"r971_maintenance_turn","0");
                    state(db,"r980_hot_lane_cursor",Integer.toString((index+1)%hotLanes.length));
                    return hot;
                }
            }
        }
        // R971 repair: the old implementation executed up to 22 complex claimFor() SELECTs while
        // holding the coordinator transaction. On a large phone database the claim itself consumed
        // nearly the whole 1s callback, producing hundreds of CLAIM_SLICE_EXHAUSTED yields before
        // any lane owner ran. Find the next ready lane in one indexed scan, then CAS-claim once.
        String candidate=readyMaintenanceCandidate(db,start,now);
        if(candidate!=null){
            ResearchTaskQueueR967.Lease task=ResearchTaskQueueR967.claimFor(db,ResearchTaskQueueR967.MAINTENANCE,candidate,now);
            if(task!=null){
                int lane=Integer.parseInt(candidate.substring(5));
                state(db,NEXT_LANE,Integer.toString((lane+1)%CONTINUITY_LANES));
                state(db,"r971_maintenance_turn",Integer.toString((turn+1)%2));
                return task;
            }
        }
        return null;
    }

    private static boolean hasLabelLane(SQLiteDatabase db) {
        try(Cursor c=db.rawQuery("SELECT 1 FROM "+ResearchTaskQueueR967.TABLE
                +" WHERE kind='MAINTENANCE' AND candidate_id IN ('lane:1','lane:6')"
                +" AND status NOT IN ('COMPLETED','EXPIRED','FAILED_TERMINAL') LIMIT 1",null)){return c.moveToFirst();}
    }

    private static String readyMaintenanceCandidate(SQLiteDatabase db,int start,long now) {
        ResearchTaskQueueR967.reapExpiredOwnedCP4(db);
        String best=null;int bestDistance=CONTINUITY_LANES+1;
        String sql="SELECT t.candidate_id FROM "+ResearchTaskQueueR967.TABLE+" t WHERE "+ResearchTaskQueueR967.readyPredicate();
        try(Cursor c=db.rawQuery(sql,ResearchTaskQueueR967.readyArgsCP4(ResearchTaskQueueR967.MAINTENANCE,now))){
            while(c.moveToNext()){
                String candidate=c.getString(0);
                if(candidate==null||!candidate.startsWith("lane:"))continue;
                int lane;try{lane=Integer.parseInt(candidate.substring(5));}catch(Exception invalid){continue;}
                if(lane<0||lane>=CONTINUITY_LANES)continue;
                int distance=(lane-start+CONTINUITY_LANES)%CONTINUITY_LANES;
                if(distance<bestDistance){bestDistance=distance;best=candidate;if(distance==0)break;}
            }
        }
        return best;
    }

    static void reviewCommitted(SQLiteDatabase db,ResearchTaskQueueR967.Lease task,String detail) {
        int lane=Integer.parseInt(task.candidate.substring(5));
        if(!ResearchTaskQueueR967.MAINTENANCE.equals(task.kind)||lane<0||lane>=CONTINUITY_LANES)throw new IllegalArgumentException("R967_REVIEW_KIND");
        String hash=V51ChecksumEngine.sha256("R967_REVIEW_RECEIPT_NOT_TRAINING|"+task.id+'|'+detail);
        ResearchTaskQueueR967.complete(db,task,hash);
        db.execSQL("UPDATE "+ResearchTaskQueueR967.LANE+" SET served_issue=CASE WHEN served_issue<? THEN ? ELSE served_issue END,active_task_id='',last_outcome=?,updated_at=? WHERE lane=? AND active_task_id=?",new Object[]{task.issue,task.issue,detail,System.currentTimeMillis(),lane,task.id});
        if(ResearchTaskQueueR967.changed(db)!=1)throw new IllegalStateException("R967_REVIEW_CURSOR_CAS");
        state(db,"r941_continuity_lane_"+lane,"REVIEW_ONLY|"+detail+"@"+task.issue);
    }
    private static void state(SQLiteDatabase db,String key,String value) {
        db.execSQL("INSERT OR REPLACE INTO "+DbHelper.TABLE_APP_STATE+"(state_key,state_value,updated_at) VALUES(?,?,?)",new Object[]{key,value,System.currentTimeMillis()});
    }
    private static boolean exists(SQLiteDatabase db,String table) {
        try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{table})){return c.moveToFirst();}
    }
    /** Keep the existing Director lane alive through pinned imports and each first family attempt.
     * A held teacher dependency is recorded and waits for later evidence, never manufactured. */
    static boolean hasUnfinishedFoundationCP12(SQLiteDatabase db) {
        if(!exists(db,com.ethk3.novaonepro.r952.AtomCatalogImportCP12.TABLE))return false;
        if(!com.ethk3.novaonepro.r952.AtomCatalogImportCP12.completeCP12(db))return true;
        if(exists(db,"research_molecule_import_cp12")&&!com.ethk3.novaonepro.r952.ModernSeedFactoryCP12.completeCP12(db))return true;
        if(exists(db,com.ethk3.novaonepro.r952.ModernFamilyFactoryCP12.CURSOR))
            try(Cursor c=db.rawQuery("SELECT 1 FROM "+com.ethk3.novaonepro.r952.ModernFamilyFactoryCP12.CURSOR+" WHERE next_variant=0 AND last_reason='' LIMIT 1",null)){return c.moveToFirst();}
        return false;
    }
    /** Only the immutable catalog bootstrap gets the faster retry. Teacher WAIT,
     * training, resource yields and critical-work preemption keep their backoff. */
    static boolean hasIncompleteCatalogsCP12(SQLiteDatabase db) {
        if(!exists(db,com.ethk3.novaonepro.r952.AtomCatalogImportCP12.TABLE))return false;
        return !com.ethk3.novaonepro.r952.AtomCatalogImportCP12.completeCP12(db)
                ||(exists(db,"research_molecule_import_cp12")
                &&!com.ethk3.novaonepro.r952.ModernSeedFactoryCP12.completeCP12(db));
    }
    private static boolean pendingOnline(DbStore store) {return hasUnfinishedR952(store.readable());}
    /** Package-visible for the real-SQLite scheduler regression; no state mutation. */
    static boolean hasUnfinishedR952(SQLiteDatabase db) {
        if(db==null||!exists(db,"r952_research_outbox"))return false;
        // R971: "pending" means unfinished durable work, not merely READY at this millisecond.
        // The old readiness-only predicate let lane 13 commit its maintenance receipt while
        // RETRYABLE_FAILED / yielded post-train work was cooling down. That marked the lane served
        // for the draw and stranded the backlog until a later watermark happened to reopen it.
        try(Cursor c=db.rawQuery("SELECT 1 FROM r952_research_outbox WHERE state NOT IN ('COMPLETED','DEPENDENCY_RETIRED') LIMIT 1",null)){if(c.moveToFirst())return true;}
        if(!exists(db,ResearchTaskQueueR967.TABLE))return false;
        try(Cursor c=db.rawQuery("SELECT 1 FROM "+ResearchTaskQueueR967.TABLE+" WHERE kind IN('POST_TRAIN','TRAIN_CANDIDATE','FREEZE_CANDIDATE','RESEARCH_ACTION') AND status NOT IN ('COMPLETED','EXPIRED','FAILED_TERMINAL') LIMIT 1",null)){return c.moveToFirst();}
    }
    /** Do not serve lane13 after only TRAIN: the other five wheel owners must run
     * even when no legacy outbox rows exist to keep the lane awake. */
    static boolean directorTurnCompleteCP12(SQLiteDatabase db){
        try(Cursor c=db.rawQuery("SELECT state_value FROM app_state WHERE state_key='r975_director_priority'",null)){
            return c.moveToFirst()&&"TRAIN".equals(c.getString(0));
        }
    }
    static boolean terminalReviewOutcome(String detail) {
        return ResearchEvolutionHeartbeatR947.terminalReview(detail);
    }
    public static boolean needsContinuation(DbStore store) {
        if(com.ethk3.novaonepro.strategyv2.LegacyCapabilityAdapterR29.hasPoolOwner(store))return false;
        if(store==null)return false;
        // R65: pressure pauses execution, not the existing worker's wake-up. Returning
        // false here discarded the continuation precisely when an owner was preempted.
        // Keep one delayed retry; pulse/runLane still prohibit work during pressure.
        if(RealtimePressureGateR811.hasActiveCriticalWork()||SettlementExecutionGateR819.isActive())return true;
        try {
            if (!store.coordinatorR36().isR811ResearchSchemaReady()) return true;
            String issue=latest(store);if(issue.isEmpty())return false;
            SQLiteDatabase db=store.readable();
            String pending=store.getState("r48_draw_ingest_pending_issue","");
            if(TargetIssueResolver.isValid(pending)&&pending.compareTo(issue)>0)return true; // continue the existing worker until confirmed ingress is durable
            if(!exists(db,ResearchTaskQueueR967.LANE))return true;
            try(Cursor c=db.rawQuery("SELECT 1 FROM "+ResearchTaskQueueR967.LANE+" WHERE desired_issue<? OR active_task_id<>'' OR served_issue<desired_issue LIMIT 1",new String[]{issue})){if(c.moveToFirst())return true;}
            return (exists(db,FixedResearchWindowR932.WINDOW)&&FixedResearchWindowR932.hasRunnableWork(db))
                    ||(exists(db,FixedResearchWindowR932.WINDOW)&&FixedWindowCertificatePublisherR932.hasRunnableWork(db))
                    ||(exists(db,DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB)&&ModuleAblationR932.hasRunnableWork(db))
                    ||hasUnfinishedFoundationCP12(db)||pendingOnline(store)||ResearchDynamicBootstrapR922.hasUnservedTraining(store);
        // A transient queue read failure cannot prove there is no durable work.
        // Retain the same delayed callback; nextContinuationDelayMs backs off on errors.
        }catch(Exception error){return true;}
    }
    /** Stop busy-spinning on a yielded lane without cancelling its durable work. */
    public static long nextContinuationDelayMs(DbStore store) {
        if(store==null)return 1000L;
        if(RealtimePressureGateR811.hasActiveCriticalWork()||SettlementExecutionGateR819.isActive())return 1000L;
        // Bootstrap has no maintenance queue yet. Reading that absent table on every
        // successful DDL unit added one second of avoidable delay per migration step.
        // Continue on this same worker while yielding to critical work and failures.
        if (!store.coordinatorR36().isR811ResearchSchemaReady()) {
            if (RealtimePressureGateR811.hasActiveCriticalWork() || SettlementExecutionGateR819.isActive())
                return 1000L;
            return ResearchSchemaCoordinatorR926.summary().startsWith("INITIALIZING|") ? 50L : 1000L;
        }
        try {
            long now=System.currentTimeMillis();
            try(Cursor c=store.readable().rawQuery("SELECT MIN(CASE WHEN status='RUNNING' THEN lease_until ELSE available_at END) FROM "+ResearchTaskQueueR967.TABLE+" WHERE kind='MAINTENANCE' AND status IN ('PENDING','YIELDED','WAIT_INPUT','RUNNING')",null)) {
                if(c.moveToFirst()&&!c.isNull(0))return Math.max(50L,Math.min(2000L,c.getLong(0)-now));
            }
        }catch(Exception error){return 1000L;}
        return 250L;
    }
    /** R27-046: old 22-lane scheduling stays on (parallel observation, default "1") until the switch is set to "0"
     * after ≥1440 periods of side-by-side R27 operation; old tables stay readable either way. */
    public static final String R27_LEGACY_SWITCH="r27_legacy_research_enabled";
    public static Result pulseWithinR927(final DbStore store,long budgetMs) {
        if(store==null)return Result.fail("STORE_NULL");
        if("0".equals(store.getState(R27_LEGACY_SWITCH,"1")))return Result.fail("R27_LEGACY_RESEARCH_RETIRED_BY_SWITCH");
        if(criticalPressure())return Result.fail("YIELD_P0_PRESSURE");
        long started=TimeSourceR811.monoNowMs();
        try {
            final String issue=latest(store);if(issue.isEmpty())return Result.fail("WAIT_HISTORY");
            final boolean dev=ResearchDynamicBootstrapR922.hasUnservedTraining(store),online=pendingOnline(store);
            final ResearchTaskQueueR967.Lease task=store.coordinatorR36().runAtomicWrite("R967_LANE_CLAIM",new DatabaseCoordinator.WriteCallable<ResearchTaskQueueR967.Lease>() {
                public ResearchTaskQueueR967.Lease call(SQLiteDatabase db){ensureDesired(db,issue,dev,online);return claimNext(db,System.currentTimeMillis(),online||dev||hasLabelLane(db));}
            },null);
            if(task==null)return Result.fail("PERIOD_PENDING|NO_READY_LEASE|watermark="+issue);
            final int lane=Integer.parseInt(task.candidate.substring(5));
            long remaining=Math.min(Math.min(1000L,Math.max(1L,budgetMs))-Math.max(0L,TimeSourceR811.monoNowMs()-started),ResearchWorkBudgetR927.remainingMs());
            String outcome;
            if(remaining<=150)outcome="YIELD:CLAIM_SLICE_EXHAUSTED";
            else {
                long slice=Math.min(lane==0||lane==1||lane==6||lane==13||lane==14||lane==17||lane==18?850L:350L,remaining-150);
                ResearchWorkBudgetR927.Scope child=ResearchWorkBudgetR927.enter(slice);
                try{outcome=runLane(store,lane,slice);}
                catch(ResearchWorkBudgetR927.Yield y){outcome="YIELD:"+y.getMessage();}
                catch(RuntimeResourceAuditR927.BudgetExceeded y){outcome="YIELD:"+y.getMessage();}
                catch(Exception e){outcome="ERROR:"+e.getClass().getSimpleName()+":"+e.getMessage();}
                finally{ResearchWorkBudgetR927.leave(child);}
            }
            final String detail=criticalPressure()
                    ?"YIELD_P0_PRESSURE_AFTER_OWNER|"+outcome:outcome;
            final boolean terminal=terminalReviewOutcome(detail);
            // Five idle wheel slots used to each impose 250ms before the next
            // IMPORT turn. Keep all owners and the 250ms work quantum, but do not
            // spend 1.5s asleep per cold-catalog rotation. nextContinuationDelayMs
            // retains its 50ms lower bound, so this never creates a busy loop.
            final long retryDelay=detail.contains("ERROR")?1000L:
                    lane==13&&detail.startsWith("R952_PENDING|")
                    &&hasIncompleteCatalogsCP12(store.readable())?50L:250L;
            Boolean receipt=ResearchWorkBudgetR927.finishLeaseReceiptR66(() -> store.coordinatorR36().runAtomicWrite("R967_LANE_RECEIPT",new DatabaseCoordinator.WriteCallable<Boolean>() {
                public Boolean call(SQLiteDatabase db) {
                    if(terminal){reviewCommitted(db,task,detail);return Boolean.TRUE;}
                    return Boolean.valueOf(ResearchTaskQueueR967.yield(db,task,task.cursor,detail,retryDelay));
                }
            },null));
            if(!Boolean.TRUE.equals(receipt))return Result.fail("REVIEW_RECEIPT_DEFERRED|lane="+lane+"|"+detail);
            int reviewed=0;
            try(Cursor c=store.readable().rawQuery("SELECT COUNT(*) FROM "+ResearchTaskQueueR967.LANE+" WHERE served_issue>=? AND active_task_id=''",new String[]{issue})){if(c.moveToFirst())reviewed=c.getInt(0);}
            String status=(reviewed==CONTINUITY_LANES?"PERIOD_COMPLETE":"PERIOD_PENDING")+"|REVIEW_ONLY|watermark="+issue+"|taskWatermark="+task.issue+"|reviewedLanes="+reviewed+"/22|lane="+lane+"|outcome="+detail+"|numericEvidence=OWNER_RECEIPTS_ONLY|deviceTimingVerified=0";
            java.util.LinkedHashMap<String,String> states=new java.util.LinkedHashMap<String,String>();
            states.put("r912_research_continuity_status",status);
            states.put("r956_continuity_lane_summary",status);
            states.put("r912_research_continuity_at",Long.toString(System.currentTimeMillis()));
            // The lane receipt is authoritative and already committed above.
            store.trySetTelemetryStatesR77(states);
            return new Result(terminal,status,lane==4&&detail.startsWith("UPDATED")?1:0,0,0,terminal?1:0,terminal?0:1,detail.contains("ERROR")?1:0);
        }catch(ResearchWorkBudgetR927.Yield y){return Result.fail("YIELD:"+y.getMessage());}
        catch(Exception error){return Result.fail("ERROR:"+error.getClass().getSimpleName()+":"+error.getMessage());}
    }

    /** Shared, atomic AppState receipt writer retained for existing creation/heartbeat owners. */
    static void commitProgress(DbStore store,final java.util.LinkedHashMap<String,String> values) {
        boolean committed=store.coordinatorR36().runAtomicWrite("R967_OWNER_PROGRESS",new DatabaseCoordinator.WriteRunnable() {
            public void run(SQLiteDatabase db){for(java.util.Map.Entry<String,String> e:values.entrySet())state(db,e.getKey(),e.getValue());}
        });
        if(!committed)throw new IllegalStateException("R967_OWNER_PROGRESS_COMMIT_FAILED");
    }

    private static boolean criticalPressure(){
        return SettlementExecutionGateR819.isActive()||!com.ethk3.novaonepro.strategyv2.LegacyCapabilityAdapterR29.onPoolContinuityThread()&&RealtimePressureGateR811.hasActiveCriticalWork();
    }
    private static String runLane(DbStore store,int lane,long slice) {
        // R977: a critical owner may start after the outer scheduler's pressure check.
        // A void worker's early return must not become a successful review receipt.
        if(store==null)return "ERROR:STORE_NULL";
        if(criticalPressure())
            return "YIELD_P0_PRESSURE";
        switch(lane) {
            case 0: return capturePendingPredraw(store,slice).status;
            case 1: return ResearchPredrawSettlementPumpR908.drainBatch(store,12,slice).status;
            case 2: return "POSTCOMMIT_MIRRORED="+SettlementPostCommitSidecarR909.drain(store,8,slice);
            case 3: { ResearchOrchestratorR906.Result r=ResearchOrchestratorR906.pulse(store,slice);return r.success?r.status:"YIELD_OR_ERROR:"+r.status; }
            case 4: return "RETIRED_CP12_MODERN_TRAINING_OWNER";
            case 5: return "RETIRED_CP12_DIRECTOR_IMPORT_OWNER";
            case 6: return FixedResearchWindowR932.maintainReviewed(store);
            case 7: return ModuleAblationR932.maintainReviewed(store);
            case 8: {int n=AlgorithmProofRecoveryR948.maintain(store,2);ResearchPlatformStoreR929.revokeStaleDependencies(store);return "PROOF_HEADS="+n;}
            case 9: return FixedWindowCertificatePublisherR932.maintainReviewed(store);
            case 10: ResearchCohortR932.maintain(store);return "COHORT_REVIEWED";
            case 11: if(ResearchGatePlanR967.installed().isCp3)return PairedResearchChallengeR932.describeLatestCp4(store);PairedResearchChallengeR932.maintain(store);return "PAIRED_CHALLENGE_REVIEWED";
            case 12:
                try(Cursor c=store.readable().rawQuery("SELECT target_issue FROM "+DbHelper.TABLE_R35_SHADOW_OBSERVATION+" WHERE pre_draw_frozen=1 ORDER BY target_issue DESC LIMIT 1",null)){if(c.moveToFirst())Batch34ChallengerCertificateBridgeR930.sync(store,c.getString(0));}
                CertificateLifecycleProjectionR957.maintain(store);return "CERTIFICATE_PROJECTION_REVIEWED";
            case 13: {
                int advanced=com.ethk3.novaonepro.r952.ResearchDirectorR952.pulse(store,slice);
                // Do not mark this watermark served while numeric or post-training work remains.
                // The same durable maintenance task yields and is reclaimed until the queue drains.
                return pendingOnline(store)||hasUnfinishedFoundationCP12(store.readable())||!directorTurnCompleteCP12(store.readable())
                        ?"R952_PENDING|advanced="+advanced:"R952_DRAINED|advanced="+advanced;
            }
            case 14: {
                long end=TimeSourceR811.monoNowMs()+slice;
                String detail="WAIT_NEW_FROZEN_ATOM_LABEL";
                do {detail=AtomicToolWorldR943.maintain(store);
                    if(!detail.startsWith("LEARNED|")&&!detail.startsWith("MASKED_ALL_ATOMS"))break;
                }while(TimeSourceR811.monoNowMs()<end-35);
                boolean pending=scalar(store,"SELECT COUNT(*) FROM "+AtomicToolWorldR943.CONTEXT+" a WHERE hit=-1 AND EXISTS(SELECT 1 FROM "+DrawDependencyInputCP12.SOURCE+" d WHERE d.issue=a.target_issue AND d.dirty=0)")>0;
                return pending?"ATOMIC_PENDING|"+detail:"ATOMIC_REVIEWED|"+detail;
            }
            case 15: return "MOLECULAR|"+MolecularToolWorldR947.maintain(store);
            case 16: return "CAUSAL_DIAGNOSTICS|"+CausalResearchFeedbackR947.maintain(store);
            case 18: {
                String status=ResearchDynamicBootstrapR922.pulse(store,slice).status;
                return ResearchDynamicBootstrapR922.hasUnservedTraining(store)?"DEV_ROUND_PENDING|"+status:status;
            }
            case 17: {ResearchEvolutionHeartbeatR947.Result r=ResearchEvolutionHeartbeatR947.pulseCurrent(store,slice);return r.success&&r.stage>=4&&r.issue.equals(latest(store))?r.status:"EVOLUTION_PENDING|"+r.status;}
            case 19: {
                int advanced=SlowStateR932.replayPastInputs(store);
                return (advanced>=8?"SLOW_STATE_PENDING":"SLOW_STATE_REVIEWED")+"|advanced="+advanced
                        +"|"+store.getState("r932_d2_replay_status","NO_D2_WORK");
            }
            case 20: return ResearchSettlementSidecarR811.drainBatch(store,4,slice).status;
            case 21: return ResearchEvolutionLifecycleR900.maintain(store);
            default: throw new IllegalArgumentException("R959_UNKNOWN_RESEARCH_LANE");
        }
    }

    /**
     * Recovers Research pre-draw capture from the durable canonical snapshot. The snapshot was
     * frozen by Production before the draw; this method only replays its 1200 actions and refuses
     * to insert after an exact draw exists.
     */
    public static PredrawCapture capturePendingPredraw(DbStore store,long budgetMs) {
        if(store==null)return PredrawCapture.none("STORE_NULL");
        if(RealtimePressureGateR811.hasActiveCriticalWork()||SettlementExecutionGateR819.isActive())
            return PredrawCapture.none("YIELD_P0_PRESSURE");
        PendingSnapshot pending=loadPendingSnapshot(store);
        if(pending==null)return PredrawCapture.none("NO_PENDING_PREDRAW_SURFACE");
        ResearchPredrawCaptureR923.Result capture=
                ResearchPredrawCaptureR923.captureTarget(store,pending.target,budgetMs);
        String status=capture.canonical();
        try{
            store.setState("r914_research_predraw_recovery_status",status);
            store.setState("r914_research_predraw_recovery_target",pending.target);
            store.setState("r914_research_predraw_recovery_at",String.valueOf(System.currentTimeMillis()));
        }catch(Throwable ignore){}
        return new PredrawCapture(capture.success,status,pending.target,capture.formulas,
                capture.algorithms,capture.genesisAssets,capture.tinyDeep);
    }

    private static PendingSnapshot loadPendingSnapshot(DbStore store){
        // R950: one durable definition of “capture still pending”. The old lane independently
        // searched every globally active asset and could repeatedly reopen a target that the
        // immutable staged roster had already completed (or that was never admitted). Reuse the
        // canonical queue recovery predicate so watchdog and continuity cannot disagree.
        try {
            String target=ResearchCaptureQueueR946.recover(store);
            return TargetIssueResolver.isValid(target) ? new PendingSnapshot(target,null,"") : null;
        } catch (Throwable unavailable) {
            return null;
        }
    }

    private static long remaining(long deadline){return Math.max(100L,deadline-TimeSourceR811.monoNowMs());}
    private static int strictCount(DbStore s){return scalar(s,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R35_OOS_LABEL+" WHERE strict_oos=1");}
    private static int compactCount(DbStore s){return scalar(s,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R906_RESEARCH_COMPACT_LABEL+" WHERE asset_id='PRODUCTION_SELECTED'");}
    private static int algorithmPending(DbStore s){return scalar(s,"SELECT COUNT(1) FROM "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW+" a WHERE hit_label=-1 AND "+AlgorithmProofRecoveryR948.eligible("a.genome_id","a.target_issue","a.record_hash")+" AND EXISTS(SELECT 1 FROM "+DrawDependencyInputCP12.SOURCE+" d WHERE d.issue=a.target_issue AND d.dirty=0)");}
    private static int scalar(DbStore s,String sql){Cursor c=null;try{c=BudgetedQueryR927.query(s.readable(),sql,null);return c.moveToFirst()?c.getInt(0):0;}finally{if(c!=null)c.close();}}
    private static String safe(String x){return x==null?"":x.replace('|','_').trim();}
    private static final class PendingSnapshot{final String target,hash;final byte[]blob;PendingSnapshot(String t,byte[]b,String h){target=t;blob=b;hash=h;}}
    public static final class PredrawCapture{public final boolean success;public final String status,target,tinyDeep;public final int formulas,algorithms,genesisAssets;PredrawCapture(boolean ok,String s,String t,int f,int a,int g,String d){success=ok;status=s;target=t;formulas=f;algorithms=a;genesisAssets=g;tinyDeep=d;}static PredrawCapture none(String s){return new PredrawCapture(false,s,"",0,0,0,"");}}
    public static final class Result{
        public final boolean success;
        public final String status;
        public final int deepUpdates,compactLag,algorithmPending,completedLanes,yieldedLanes,errorLanes;
        Result(boolean ok,String s,int d,int c,int a,int completed,int yielded,int errors){
            success=ok;status=s;deepUpdates=d;compactLag=c;algorithmPending=a;
            completedLanes=Math.max(0,completed);yieldedLanes=Math.max(0,yielded);errorLanes=Math.max(0,errors);
        }
        static Result fail(String s){return new Result(false,s,0,0,0,0,0,0);}
    }
}
