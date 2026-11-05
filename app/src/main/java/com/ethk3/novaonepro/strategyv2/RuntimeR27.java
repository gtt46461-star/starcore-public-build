package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** R27 research runtime: the single owner of the R27 research tasks on ResearchPoolR27 (docs/02 2.4: new system is
 * the Owner; the R26 coordinator keeps the legacy R26 tasks on its own thread). Tasks:
 * r27-replay-dataset, r27-evolution, r27-live, r27-atoms, r27-atomnet, r27-abi, r27-calibration, r27-chart-cache,
 * r27-legacy-export, r27-health. Every slice reports a WaitKindR27 reason through the pool. */
public final class RuntimeR27 implements AutoCloseable {
    public static final int DATASET_ROWS = 5000, REFRESH_NEW_ROWS = 360, LIVE_REBASE_ROWS = 6500;
    public static final String LOOP = "MAIN_REPAIR1"; // MAIN stays readable by the pre-repair APK
    final DbStore store; final ResearchPoolR27 pool;
    private final HealthTask healthTaskR31;
    private boolean resourceCachesClosedR31;
    // Evolution dataset snapshot (rebound only between generations).
    volatile ReplayDatasetR27 evoData; volatile FeatureCacheR27 evoCache; volatile ReplayDatasetR27 pendingData; volatile FeatureCacheR27 pendingCache;
    volatile EvolutionLoopR27 evo;
    volatile String recoveredLegacyCursor;
    // Live dataset grows by appending; family streams continue incrementally.
    ReplayDatasetR27 liveData; FeatureCacheR27 liveCache;
    /** R30-B03-04 live feature cache key: (dataset hash, trained through, feature ABI); positions share one cache. */
    volatile String liveCacheKeyR30 = "";
    // Golden set and operator ABI (semantic ABI).
    volatile ReplayDatasetR27 golden; volatile FeatureCacheR27 goldenCache; volatile Map<String, String> operatorAbi; volatile String operatorAbiHash;
    // Atom replay frame and states.
    volatile AtomReplayR27.Frame frame; volatile String frameDataset = ""; int frameCursor; double[][] frameValues; boolean[][] framePresent; int[] frameHit, framePos, frameActual; String[] frameIssue;
    final Map<Integer, String> atomStates = new HashMap<>();
    volatile AtomNetTrainerR27 atomNet; volatile String atomNetDataset = "";
    FtrlR27 atomFtrl; String atomTrainedThrough = "";

    int calibrationIndexR30;
    static final class AtomPublicationR30 {final AtomNetSessionR30 state;final ModelSurfaceProviderR30.FrozenModelRef model;AtomPublicationR30(AtomNetSessionR30 state,ModelSurfaceProviderR30.FrozenModelRef model){this.state=state;this.model=model;}}
    volatile AtomPublicationR30 atomPublicationR30;
    void publishAtomStateR30(AtomNetSessionR30 state){ModelSurfaceProviderR30.FrozenModelRef ref=state.ready()?new ModelSurfaceProviderR30.FrozenModelRef(state.trainer.net.encode(),state.trainedThroughOrdinal(),System.currentTimeMillis()):null;atomPublicationR30=new AtomPublicationR30(state,ref);atomSessionR30=state;}
    void publishAtomStateR31(AtomNetSessionR30 state, ModelSurfaceProviderR30.FrozenModelRef persisted) {
        if (state == null || state.ready() != (persisted != null)
                || (persisted != null && (persisted.trainedThroughOrdinal != state.trainedThroughOrdinal()
                || !persisted.weightsHash.equals(StatsR27.sha256(state.trainer.net.encode())))))
            throw new IllegalArgumentException("R31_ATOM_PUBLICATION_STATE_MISMATCH");
        atomPublicationR30 = new AtomPublicationR30(state, persisted);
        atomSessionR30 = state;
    }
    volatile AtomNetSessionR30 atomSessionR30; ReplayDatasetR27 atomDataR30; long atomUnitsR30;

    private static volatile RuntimeR27 active;
    public static RuntimeR27 active() { return active; }

    public RuntimeR27(DbStore store, String session) {
        this.store = store;
        CooperativeContinuationR26.threadStartHook = () -> android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        pool = new ResearchPoolR27(store, session, ResearchPoolR27.defaultThreads());
        pool.register(new DatasetTask()); pool.register(new LiveTask()); pool.register(new EvolutionTask()); pool.register(new AbiTask());
        pool.register(new AtomsTask()); pool.register(new AtomNetTask()); pool.register(new CalibrationTask()); pool.register(new ChartTask());
        healthTaskR31=new HealthTask();
        pool.register(new ObservatoryTask()); pool.register(new ResearchStudyTask()); pool.register(new MatrixProductionTask()); pool.register(new LegacyExportTask()); pool.register(healthTaskR31);
        active = this;
    }
    public void start() { pool.start(); }
    public ResearchPoolR27 pool() { return pool; }
    @Override public void close() { pool.close(); if (active == this) active = null; }

    /** The coordinator calls this only after its own worker/probe and this entire pool have exited.
     * Closing a session requests cancellation; it must not recycle shared cache storage while used. */
    synchronized boolean releaseTerminatedCachesR31() {
        if(resourceCachesClosedR31)return true;
        if(!pool.resourcesTerminatedR31())return false;
        EvolutionLoopR27 loop=evo;
        if(loop!=null){loop.close();if(!loop.resourcesClosedR31())return false;}
        if(healthTaskR31.q08!=null){healthTaskR31.q08.close();healthTaskR31.q08=null;}
        java.util.Set<FeatureCacheR27> caches=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<FeatureCacheR27,Boolean>());
        for(FeatureCacheR27 cache:new FeatureCacheR27[]{evoCache,pendingCache,liveCache,goldenCache})
            if(cache!=null&&caches.add(cache))cache.close();
        evo=null;evoCache=null;pendingCache=null;liveCache=null;goldenCache=null;
        evoData=null;pendingData=null;liveData=null;golden=null;liveCacheKeyR30="";
        // This drops owned references. Heap/array reclamation requires an actual later observation.
        resourceCachesClosedR31=true;
        return true;
    }

    private FeatureCacheR27 featureCacheR31(ReplayDatasetR27 data,int capacity) {
        FeatureCacheR27 cache=new FeatureCacheR27(data,capacity);
        String abi=operatorAbiHash;
        if(abi!=null)cache.bindSharedFeaturesR31(abi);
        return cache;
    }

    /** Called by the pool only after the revoked task's continuation has actually terminated.
     * Discard computation drafts, retaining committed evidence and the last published immutable model. */
    public synchronized void invalidateTaskDraftR31(String task) {
        switch(task) {
            case "r27-evolution":
                // DatasetTask reloads the exact pinned dataset referenced by the durable evolution cursor.
                evo=null;evoData=null;evoCache=null;pendingData=null;pendingCache=null;recoveredLegacyCursor=null;
                break;
            case "r27-live":
                liveData=null;liveCache=null;liveCacheKeyR30="";
                break;
            case "r27-atoms":
                // frameCursor can advance before score publication. Rebuild this derived input on cancellation.
                frame=null;frameDataset="";frameCursor=0;frameValues=null;framePresent=null;
                frameHit=null;framePos=null;frameActual=null;frameIssue=null;
                // atomStates/derived are committed publications, and remain available to their consumers.
                break;
            case "r27-atomnet":
                atomSessionR30=null;atomDataR30=null;atomUnitsR30=0;
                atomNet=null;atomFtrl=null;atomNetDataset="";atomTrainedThrough="";
                // atomPublicationR30 remains the last immutable committed prediction reference.
                break;
            case "r27-calibration":calibrationIndexR30=0;break;
            case "r27-replay-dataset": // immutable dataset references are published only after commit
            case "r27-abi": // ensureGolden publishes a complete immutable bundle
            case "r27-chart-cache": // lastBuilt changes only after successful publication
            case "r27-health": // attempt counters are observations, not model/checkpoint state
            case "r27-legacy-export":
            case "r27-study-r1":
            case "r27-matrices-r1":
            case "r27-observatory-r1": // each step reads its durable cursor/result
                break;
            default:throw new IllegalArgumentException("R31_UNKNOWN_TASK_DRAFT:"+task);
        }
    }

    static String latestIssue(SQLiteDatabase ro) { try (Cursor c = ro.rawQuery("SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1", null)) { return c.moveToFirst() ? c.getString(0) : null; } }
    static long latestOpen(SQLiteDatabase ro) { try (Cursor c = ro.rawQuery("SELECT open_time FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1", null)) { return c.moveToFirst() ? c.getLong(0) : 0; } }
    static void submit(ResearchPoolR27.Worker w, String op, ResearchWriteQueueR27.Batch b) throws InterruptedException {
        submit(w,op,b,false);
    }
    static void submit(ResearchPoolR27.Worker w,String op,ResearchWriteQueueR27.Batch b,boolean freezeWrite)throws InterruptedException {
        submit(w,op,b,freezeWrite,null);
    }
    /** The immutable batch survives writer backoff. Session/fence are audit facts, not a new experiment key. */
    static void submit(ResearchPoolR27.Worker w,String op,ResearchWriteQueueR27.Batch b,
                       boolean freezeWrite,String ownerInputSnapshot)throws InterruptedException {
        RuntimeCoordinatorR26.Claim claim=RuntimeCoordinatorR26.currentClaimR30();
        if(claim==null)throw new IllegalStateException("R31_DEPENDENCY_TASK_CLAIM_REQUIRED");
        final long sequence=claim.nextCommitChunkR31();
        String latest=latestIssue(w.ro);
        Map<String,Object> input=new LinkedHashMap<>();input.put("task",claim.task);input.put("committedWorkCursor",claim.cursor);
        input.put("sourceRevision",SchemaR27.liveSourceRevision(w.ro));input.put("latestVisibleIssue",latest);
        input.put("ownerInputSnapshot",ownerInputSnapshot);
        // Input scope is explicit: owner snapshots bind the actual frozen dataset/checkpoint when supplied.
        input.put("scope",ownerInputSnapshot==null?"TASK_CHECKPOINT_AND_SOURCE_ADMISSION_SNAPSHOT":"OWNER_IMMUTABLE_INPUT_SNAPSHOT");
        final String inputHash=HashingSU1.text(JsonR27.write(input));
        final String operatorVersion="R31_RESEARCH_WRITE_V1|"+BuildConfig.SOURCE_WORKTREE_SHA256_R31;
        final String commitKey=HashingSU1.text(claim.task+"|"+op+"|"+inputHash+"|"+sequence+"|"+operatorVersion);
        final ResearchWriteQueueR27.ImmutableBatch prepared=b.prepareR31(op,commitKey,claim.task,
                claim.owner,claim.fence,inputHash,sequence,operatorVersion);
        final byte[] preparedIdentity=prepared.payloadHash.getBytes(java.nio.charset.StandardCharsets.UTF_8);
        CommitEnvelopeR30R1 envelope=new CommitEnvelopeR30R1(commitKey,claim.fence,0,preparedIdentity);
        for(;;){
            if(w.cancelled()||Thread.currentThread().isInterrupted())throw new InterruptedException("R27_CANCELLED");
            CommitEnvelopeR30R1.Outcome outcome=envelope.attempt((id,fence,revision,state)->
                    w.writes().submit(op,prepared,w::cancelled,freezeWrite)
                            ?CommitEnvelopeR30R1.Outcome.COMMITTED:CommitEnvelopeR30R1.Outcome.RETRY_LATER);
            if(outcome==CommitEnvelopeR30R1.Outcome.COMMITTED){envelope.publishedState();return;}
            CooperativeContinuationR26.yieldPreparedWrite();
        }
    }

    // ------------------------------------------------------------------ r27-replay-dataset (R27-013/016)
    final class DatasetTask implements ResearchPoolR27.Task {
        public String id() { return "r27-replay-dataset"; }
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker w) throws Exception {
            String latest = latestIssue(w.ro); if (latest == null) return ResearchPoolR27.Outcome.idle("WAITING_DRAWS", 0, 0);
            long latestOrd = TargetIssueResolver.sequenceOrdinalSU1(latest);
            ReplayDatasetR27 cur = pendingData != null ? pendingData : evoData;
            if(cur==null){
                String savedHash=null;
                try(Cursor x=w.ro.rawQuery("SELECT dataset_hash FROM r27_evolution_cursor WHERE loop IN (?,?) ORDER BY CASE loop WHEN ? THEN 0 ELSE 1 END LIMIT 1",new String[]{LOOP,"MAIN",LOOP})){if(x.moveToFirst())savedHash=x.getString(0);}
                if(savedHash!=null){ResearchInputPinR30R1.Pin pin=ResearchInputPinR30R1.loadCurrent(w.ro,savedHash);if(pin!=null){evoData=pin.data;evoCache=featureCacheR31(pin.data,48);ModelSurfaceStoreR30.bindHistorical(w.ro,evoCache);return ResearchPoolR27.Outcome.progress(1,pin.data.ordinals[pin.data.size()-1]);}}
            }

            if (cur != null && !ResearchInputPinR30R1.invalid(w.ro,cur.datasetHash) && latestOrd - cur.ordinals[cur.size() - 1] < REFRESH_NEW_ROWS) return ResearchPoolR27.Outcome.idle("NO_NEW_EVENT", cur.ordinals[cur.size() - 1], latestOrd);
            long t0 = System.currentTimeMillis();
            final long capturedRevision=SchemaR27.liveSourceRevision(w.ro);
            ReplayDatasetR27 d = StoreR27.loadDataset(w.ro, DATASET_ROWS);
            if(capturedRevision<0||capturedRevision!=SchemaR27.liveSourceRevision(w.ro))return ResearchPoolR27.Outcome.idle("DEPENDENCY(DATASET_READ_SOURCE_CHANGED)",0,-1);
            if (d.size() < 3 * SplitR27.BLOCK) return ResearchPoolR27.Outcome.idle("WAITING_DRAWS:rows=" + d.size(), d.size(), 3 * SplitR27.BLOCK);
            FeatureCacheR27 c = featureCacheR31(d,48);boolean published=false;
            try{
                ModelSurfaceStoreR30.bindHistorical(w.ro,c);
                SplitR27 sp = SplitR27.of(d, evo == null ? null : evo.ledger().degradedBlocks());
                // A process may die after the canonical input transaction, before publishing its cache.
                // Restore that committed input without manufacturing new timestamps or another receipt.
                ResearchInputPinR30R1.Pin pinned=ResearchInputPinR30R1.loadCurrent(w.ro,d.datasetHash);
                if(pinned!=null){
                    SplitR27 original=SplitR27.of(d,null);
                    String sealed=original.describe(),degraded=SplitR27.of(d,java.util.Collections.singleton(original.holdoutBlock)).describe();
                    try(Cursor saved=w.ro.rawQuery("SELECT from_issue,to_issue,rows,segments,split FROM r27_replay_dataset WHERE dataset_hash=?",new String[]{d.datasetHash})){
                        if(!saved.moveToFirst()||!d.issues[0].equals(saved.getString(0))||!d.issues[d.size()-1].equals(saved.getString(1))||saved.getInt(2)!=d.size()||saved.getInt(3)!=d.segments()||!(sealed.equals(saved.getString(4))||degraded.equals(saved.getString(4))))
                            throw new DatasetPublicationIntegrityR31("PERSISTED_INPUT_HEADER_MISMATCH");
                    }
                    if(w.cancelled()||Thread.currentThread().isInterrupted())throw new InterruptedException("R27_CANCELLED");
                    RuntimeCoordinatorR26.Claim current=RuntimeCoordinatorR26.currentClaimR30();
                    if(current==null)throw new IllegalStateException("R31_DEPENDENCY_TASK_CLAIM_REQUIRED");
                    RuntimeCoordinatorR26.requireFence(w.ro,current);
                    if(capturedRevision!=SchemaR27.liveSourceRevision(w.ro)||ResearchInputPinR30R1.invalid(w.ro,d.datasetHash))throw new ResearchStoreR30R1.StaleResearch();
                    if(evoData==null){evoData=d;evoCache=c;}else{pendingData=d;pendingCache=c;}
                    published=true;
                    return ResearchPoolR27.Outcome.progress(1,d.ordinals[d.size()-1]);
                }
                ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch();
                b.add("INSERT OR IGNORE INTO r27_replay_dataset(dataset_hash,from_issue,to_issue,rows,segments,split,built_at,build_ms) VALUES(?,?,?,?,?,?,?,?)",
                        d.datasetHash, d.issues[0], d.issues[d.size() - 1], d.size(), d.segments(), sp.describe(), System.currentTimeMillis(), System.currentTimeMillis() - t0);
                try(PinPublicationR31.Scope pin=ResearchInputPinR30R1.appendAdmitted(b,w.ro,store.researchInputRootCP11().getParentFile(),d,capturedRevision,System.currentTimeMillis())){
                    submit(w, "REPLAY_DATASET", b,false,d.datasetHash+"|sourceRevision="+capturedRevision);
                }
                if (evoData == null) { evoData = d; evoCache = c; } else { pendingData = d; pendingCache = c; }
                published=true;
                return ResearchPoolR27.Outcome.progress(1, d.ordinals[d.size() - 1]);
            }finally{if(!published)c.close();}
        }
    }

    private static final class DatasetPublicationIntegrityR31 extends IllegalStateException implements FailureDispositionR31.ClassifiedFailure{
        DatasetPublicationIntegrityR31(String reason){super("R31_DATASET_INPUT_INTEGRITY:"+reason);}
        public String failureCategory(){return "INTEGRITY";}public String operation(){return "REPLAY_DATASET_RESTORE";}
        public String phase(){return "VERIFY_PERSISTED_INPUT";}public String commitOutcomeName(){return "NOT_SUBMITTED";}
    }

    // ------------------------------------------------------------------ r27-evolution (R27-021..032)
    final class EvolutionTask implements ResearchPoolR27.Task {
        public String id() { return "r27-evolution"; }
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker w) throws Exception {
            if (evoCache == null) return ResearchPoolR27.Outcome.idle("DEPENDENCY(r27-replay-dataset)", 0, -1);
            final EvolutionPublicationStoreR31.CursorState expectedEvolutionCursor=EvolutionPublicationStoreR31.CursorState.capture(w.ro,LOOP);
            EvolutionLoopR27 committed=evo;
            final boolean restoredObserverBoundaryR31=committed==null;
            if(committed==null){committed=restoreOrCreate(w);ConfirmReadStoreR30.restore(w.ro,committed.ledger());}
            // Recover observer consumption after a committed generation; never rerun the committed step9.
            // A current in-memory loop already acknowledged its refresh and needs no duplicate ACK.
            if(restoredObserverBoundaryR31&&committed.step==1&&committed.generation>0&&expectedEvolutionCursor.exists
                    &&expectedEvolutionCursor.snapshot.startsWith(EvolutionPublicationStoreR31.SNAPSHOT_PREFIX)
                    &&!ResearchInputPinR30R1.invalid(w.ro,committed.context().data.datasetHash)){
                boolean refreshed=false;
                try{refreshObservers(w,committed);refreshed=true;}
                finally{if(!refreshed&&evo!=committed)committed.close();}
            }
            EvolutionLoopR27 loop=committed.preparedForkR30();
            if(ResearchInputPinR30R1.invalid(w.ro,loop.context().data.datasetHash)){
                if(pendingCache==null||ResearchInputPinR30R1.invalid(w.ro,pendingCache.data.datasetHash))return ResearchPoolR27.Outcome.idle("DEPENDENCY(CORRECTED_REPLAY_DATASET)",0,-1);
                // Save the original cursor below before abandoning this invalid development generation. Exposure ledger/RNG/population remain.
                loop.restartInvalidDevelopmentInput();
            }

            // Never expose a learner draft or a newly rebound dataset before its artifact transaction succeeds.
            final FeatureCacheR27 selectedCache=loop.step==1&&pendingCache!=null?pendingCache:evoCache;
            final ReplayDatasetR27 selectedData=selectedCache.data;
            if(loop.context().cache!=selectedCache)loop.rebind(selectedCache);
            ConfirmReadStoreR30.restore(w.ro,loop.ledger());
            loop.executor=null;loop.serialWhile=()->pool.narrow(System.currentTimeMillis());loop.leafWeight=leafWeights();loop.derivedLeaves=derived;
            String birthIssue = selectedData.issues[selectedData.size() - 1];
            StoreR27.EvolutionSink sink = new StoreR27.EvolutionSink(LOOP, birthIssue);
            loop.setSink(sink);
            loop.sourceCommit = BuildConfig.SOURCE_COMMIT_R26;
            ConfirmReadStoreR30.bind(w, loop.ledger());
            int stepBefore = loop.step, genBefore = loop.generation;
            // A separate phase on this same worker: confirmation cannot run inside EvolutionLoop.step().
            FrozenConfirmationR30.Task queued = FrozenConfirmationStoreR30.nextPending(w.ro);
            if (queued != null) confirmFrozen(w,loop,queued); // at most one durable packet per worker slice, including orphaned cursors
            FrozenConfirmationR30.Task confirmTask = null;
            FrozenConfirmationR30.Result confirmResult = null;
            if (stepBefore == 9 && loop.confirmationDispatched && loop.pendingConfirmation != null) {
                confirmTask = FrozenConfirmationStoreR30.task(w.ro,loop.pendingConfirmation.key);
                confirmResult = FrozenConfirmationStoreR30.result(w.ro,confirmTask.key);
            }
            if(stepBefore==2){loop.directedResearch=ResearchStoreR30R1.ready(w.ro,loop.context(),loop.generation,4);loop.researchContext=ResearchContextR30R1.read(w.ro,loop.context());}
            if(stepBefore==3)loop.revalidateResearchAdmissions(w.ro);
            loop.step();
            ResearchWriteQueueR27.Batch b = sink.batch;
            final String preservedLegacy=recoveredLegacyCursor;
            if(preservedLegacy!=null){String id=StatsR27.sha256("LEGACY_CURSOR_RECOVERY|"+preservedLegacy);Map<String,Object> original=new LinkedHashMap<>();original.put("originalSnapshot",preservedLegacy);original.put("reason","Original input snapshot unavailable after tail movement; preserve models/RNG/exposure and restart development at boundary; do not reconstruct old formal outcomes");b.add("INSERT OR IGNORE INTO r30r1_analysis_run VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",id,selectedData.datasetHash,StatsR27.sha256("LEGACY_CURSOR_RECOVERY"),"SELECT",BuildConfig.SOURCE_COMMIT_R26,0,0,0,StatsR27.sha256(preservedLegacy),"LEGACY_CURSOR_BOUNDARY_RECOVERY",JsonR27.write(original),System.currentTimeMillis());}
            final long inputRevision=ResearchInputPinR30R1.revision(w.ro,selectedData.datasetHash);
            b.guardBefore(db->{ResearchInputPinR30R1.require(db,selectedData.datasetHash,inputRevision);return 0;});
            if(ResearchInputPinR30R1.invalid(w.ro,committed.context().data.datasetHash)){
                String old=committed.snapshot(),saved=StatsR27.sha256("INVALID_INPUT_CURSOR|"+old);
                b.add("INSERT OR IGNORE INTO r30r1_analysis_run VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",saved,committed.context().data.datasetHash,StatsR27.sha256("RECOVERY_CURSOR"),"SELECT",BuildConfig.SOURCE_COMMIT_R26,0,committed.context().data.ordinals[0],committed.context().data.ordinals[committed.context().data.size()-1],StatsR27.sha256(old),"INVALID_INPUT_CURSOR_PRESERVED",old,System.currentTimeMillis());
            }

            if(stepBefore==2)loop.stageResearchHypotheses(b,System.currentTimeMillis());
            if(stepBefore==3)loop.commitResearchAdmissions(b,System.currentTimeMillis());
            if (stepBefore == 8 && loop.confirmationDispatched) FrozenConfirmationStoreR30.freeze(b,loop.pendingConfirmation,loop.context().data);
            if (confirmResult != null) FrozenConfirmationStoreR30.generationReport(b,w.ro,genBefore,confirmTask,confirmResult);
            b.add("INSERT OR REPLACE INTO r27_evolution_cursor(loop,generation,step,dataset_hash,rng_state,snapshot,updated_at) VALUES(?,?,?,?,?,?,?)", LOOP, loop.generation, loop.step, selectedData.datasetHash, loop.rng.state, loop.snapshot(), System.currentTimeMillis());
            if (!sink.promotions.isEmpty()) {
                final List<Object[]> promos = sink.promotions;
                b.guardBefore(db -> { for (Object[] p : promos) { PolicySpecR27 s = (PolicySpecR27) p[0]; StoreR27.insertShadow(db, s, (Double) p[1], (String) p[3], semanticAbi(s), s.policyHash(), s.policyHash()); } return 0; });
            }
            try(EvolutionPublicationStoreR31.Prepared publication=sink.preparePublicationR31(w.ro,RuntimeCoordinatorR26.currentClaimR30(),BuildConfig.SOURCE_COMMIT_R26,expectedEvolutionCursor)){
                int publicationSlice=0;
                for(ResearchWriteQueueR27.Batch stage:publication.stagingBatches()){
                    submit(w,"EVOLUTION_STAGING_"+stepBefore+"_"+publicationSlice,stage,false,publication.id+"|slice="+publicationSlice);
                    publicationSlice++;
                }
                publication.verifyPrepared(w.ro); // Hash/codec verification stays outside the writer.
                submit(w,"EVOLUTION_STEP_"+EvolutionLoopR27.STEPS[stepBefore],publication.publicationBatch(),false,publication.id);
            }catch(RuntimeException stale){boolean benign=false;for(Throwable cause=stale;cause!=null&&cause.getCause()!=cause;cause=cause.getCause())if(cause instanceof ResearchStoreR30R1.StaleResearch)benign=true;if(benign)return ResearchPoolR27.Outcome.idle("NO_NEW_EVENT:RESEARCH_SOURCE_RETRY",(long)committed.generation*10+committed.step,-1);throw stale;}
            if(preservedLegacy!=null&&preservedLegacy.equals(recoveredLegacyCursor))recoveredLegacyCursor=null;
            synchronized(RuntimeR27.this){evoData=selectedData;evoCache=selectedCache;evo=loop;if(pendingCache==selectedCache){pendingCache=null;pendingData=null;}}
            if (stepBefore == 9) refreshObservers(w, loop);
            if (stepBefore == 9) registerConfirmed(w,loop.cfg.liveCap);
            if (stepBefore == 9 && genBefore % 50 == 49) derivedDue = true;
            return ResearchPoolR27.Outcome.progress(1, (long) loop.generation * 10 + loop.step);
        }
    }
    FrozenConfirmationR30.Result confirmFrozen(ResearchPoolR27.Worker w, EvolutionLoopR27 loop, FrozenConfirmationR30.Task task) throws Exception {
        FrozenConfirmationR30.Result saved = FrozenConfirmationStoreR30.result(w.ro,task.key);
        if (saved != null) return saved;
        FrozenConfirmationStoreR30.Writer writer = (op,batch) -> {
            try { submit(w,op,batch); }
            catch (InterruptedException e) { Thread.currentThread().interrupt(); throw new IllegalStateException("R30_CONFIRM_WRITE_INTERRUPTED",e); }
        };
        FrozenConfirmationR30.Result result;
        try {
            EvalContextR27 context = task.datasetHash.equals(loop.context().data.datasetHash) ? loop.context() : FrozenConfirmationStoreR30.context(w.ro,task);
            result = FrozenConfirmationR30.evaluate(task,context,loop.ledger(),FrozenConfirmationStoreR30.evidence(w.ro,writer));
        }
        catch (IllegalStateException failure) {
            String reason = String.valueOf(failure.getMessage());
            boolean consumed = false;
            for (Throwable t = failure; t != null && t.getCause() != t; t = t.getCause())
                if (String.valueOf(t.getMessage()).contains(HoldoutLedgerR27.CONFIRM_REUSED) || "R30_CONFIRM_BUDGET_EXHAUSTED".equals(t.getMessage()) ||
                        String.valueOf(t.getMessage()).startsWith("R30_CONFIRM_DATASET_") || "R30_CONFIRM_TRACE_CORRUPT".equals(t.getMessage()) || "R30_CONFIRM_INVALID_CACHED_EVIDENCE".equals(t.getMessage())) consumed = true;
            if (!consumed) throw failure;
            // Process death after a reservation but before durable outcomes is missing evidence, never permission
            // to look again. The independently frozen FUTURE experiment remains separate from this history test.
            result = FrozenConfirmationR30.unavailable(task,reason);
        }
        ResearchWriteQueueR27.Batch batch = new ResearchWriteQueueR27.Batch();
        FrozenConfirmationStoreR30.complete(batch,task,result);
        FrozenConfirmationStoreR30.generationReport(batch,w.ro,task.generation,task,result);
        submit(w,"CONFIRM_RESULT",batch); return result;
    }
    void registerConfirmed(ResearchPoolR27.Worker w, int cap) throws Exception {
        FrozenConfirmationR30.Task task = FrozenConfirmationStoreR30.nextRegistration(w.ro);
        if (task == null) return;
        if (!FrozenConfirmationR30.currentSemantics().equals(task.researchSemanticHash)) {
            ResearchWriteQueueR27.Batch blocked = new ResearchWriteQueueR27.Batch();
            blocked.add("UPDATE r30_frozen_confirm SET registration_state='BLOCKED_SOURCE_CHANGED' WHERE task_key=? AND registered_at=0",task.key);
            submit(w,"CONFIRM_REGISTRATION_SOURCE_CHANGED",blocked); return;
        }
        if (operatorAbi == null) return;
        String abi = semanticAbi(task.champion.spec()); // prepare before entering the single database writer
        try { submit(w,"CONFIRMED_FUTURE_REGISTRATION",FrozenConfirmationStoreR30.register(task,abi,cap)); }
        catch (IllegalStateException failure) {
            boolean full = false;
            for (Throwable t = failure; t != null && t.getCause() != t; t = t.getCause()) if (String.valueOf(t.getMessage()).contains("R30_CONFIRM_WAIT_SLOT")) full = true;
            if (!full) throw failure; // a full live pool leaves the immutable queued candidate intact
        }
    }
    volatile boolean derivedDue; volatile List<PolicyAstR27.Node> derived = new ArrayList<>();

    // ------------------------------------------------------------------ R27.4 observers (every strategy runs every period)
    /** Best evolved candidates (by H_SELECT score) frozen and settled every period as observers. */
    public static final int OBSERVE_CAP = 24;
    public static final String OBSERVE = "OBSERVE_REPAIR1", OBSERVE_RETIRED = "OBSERVE_REPAIR1_RETIRED";
    public static String observerId(String policyHash) { return StatsR27.sha256(OBSERVE + "|" + policyHash); }

    /** Rank only the exploration segment. The immutable FUTURE queue owns its slots until a terminal
     * qualification result, including while a draw is missing or a conflict is being resolved. */
    static Map<String,EvolutionLoopR27.Member> explorationR30(List<EvolutionLoopR27.Member> ranked, Map<String,String> confirmation) {
        Map<String,EvolutionLoopR27.Member> keep = new LinkedHashMap<>();
        int remaining = Math.max(0, OBSERVE_CAP - confirmation.size());
        java.util.Set<String> protectedBases = new java.util.HashSet<>(confirmation.values());
        for (EvolutionLoopR27.Member m : ranked) {
            if (keep.size() >= remaining) break;
            String id = observerId(m.hash());
            if (Double.isFinite(m.tau) && !confirmation.containsKey(id) && !protectedBases.contains(m.hash())) keep.put(id,m);
        }
        return keep;
    }

    /** Freeze rank/formulas outside the writer. Only source validation and bounded SQL publish inside it. */
    void refreshObservers(ResearchPoolR27.Worker w, EvolutionLoopR27 loop) throws Exception {
        RuntimeCoordinatorR26.Claim claim=RuntimeCoordinatorR26.currentClaimR30();
        if(claim==null)throw new ObserverPublicationStoreR31.Dependency("R31_OBSERVER_TASK_CLAIM_REQUIRED");
        try(ObserverPublicationStoreR31.FrozenInput input=ObserverPublicationStoreR31.freeze(loop,claim,w::cancelled)){
            for(;;){
                if(w.cancelled())throw new InterruptedException("R27_CANCELLED");
                try(ObserverPublicationStoreR31.Prepared prepared=ObserverPublicationStoreR31.prepare(w.ro,input,RuntimeCoordinatorR26.currentClaimR30(),w::cancelled)){
                    try{submit(w,"OBSERVERS",prepared.batch,false,prepared.inputSnapshot);return;}
                    catch(RuntimeException changed){
                        FailureDispositionR31.Decision disposition=FailureDispositionR31.classify(changed);
                        boolean rosterChanged=false;
                        java.util.Set<Throwable> seen=java.util.Collections.newSetFromMap(new java.util.IdentityHashMap<Throwable,Boolean>());
                        for(Throwable cause=changed;cause!=null&&seen.size()<16&&seen.add(cause);cause=cause.getCause())if(cause instanceof ObserverPublicationStoreR31.RosterChanged)rosterChanged=true;
                        if(!rosterChanged||disposition.requiresCommitResolution||disposition.alreadyCommitted)throw changed;
                    }
                }
                // A new registration/retirement is a new readonly roster prepare, not a fault or new experiment.
                if(!CooperativeContinuationR26.installed())throw new ObserverPublicationStoreR31.Dependency("R31_OBSERVER_ROSTER_RETRY_REQUIRES_CONTINUATION");
                CooperativeContinuationR26.yieldPreparedWrite();
            }
        }
    }

    EvolutionLoopR27 restoreOrCreate(ResearchPoolR27.Worker w) {
        EvolutionLoopR27.Config cfg = new EvolutionLoopR27.Config(); cfg.seed = System.currentTimeMillis();
        try (Cursor c = w.ro.rawQuery("SELECT snapshot,dataset_hash FROM r27_evolution_cursor WHERE loop IN (?,?) ORDER BY CASE loop WHEN ? THEN 0 ELSE 1 END LIMIT 1", new String[] {LOOP,"MAIN",LOOP})) {
            if (c.moveToFirst()) {
                final String originalSnapshot=EvolutionPublicationStoreR31.resolveSnapshot(w.ro,c.getString(0));
                try { return EvolutionLoopR27.restore(originalSnapshot, cfg, evoCache, null, System::currentTimeMillis); }
                catch (RuntimeException mismatch) {
                    if(!evoCache.data.datasetHash.equals(c.getString(1))){
                        EvolutionLoopR27 recovered=RepairCompatibilityR30R1.restoreBoundary(originalSnapshot,cfg,evoCache,null,System::currentTimeMillis);
                        recoveredLegacyCursor=originalSnapshot;return recovered;
                    }
                    throw mismatch; // A damaged cursor must not silently discard the exposure ledger or models.
                }
            }
        }
        EvolutionLoopR27 l = new EvolutionLoopR27(cfg, evoCache, null, System::currentTimeMillis);
        try (Cursor c = w.ro.rawQuery("SELECT max(generation)+1 FROM r27_generation WHERE loop=?", new String[] {LOOP})) { if (c.moveToFirst() && !c.isNull(0)) l.generation = c.getInt(0); }
        return l;
    }

    Map<String, Double> leafWeights() { synchronized (atomStates) { return AtomScoreR27.leafWeights(new HashMap<>(atomStates)); } }

    String semanticAbi(PolicySpecR27 s) { Map<String, String> abi = operatorAbi; return abi == null ? "" : SemanticAbiR27.policyAbi(s, abi); }

    // ------------------------------------------------------------------ r27-live (R27-008/032/040/041/042)
    final class LiveTask implements ResearchPoolR27.Task {
        public String id() { return "r27-live"; }
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker w) throws Exception {
            String latest = latestIssue(w.ro); if (latest == null) return ResearchPoolR27.Outcome.idle("WAITING_DRAWS", 0, 0);
            long latestOrd = TargetIssueResolver.sequenceOrdinalSU1(latest), deadline = latestOpen(w.ro) + 60_000L;
            w.writes().setNextDeadline(deadline);
            long units = LiveShadowsR27.settle(RuntimeR27.this, w, latestOrd);
            units += LiveShadowsR27.qualify(RuntimeR27.this, w, latestOrd);
            try{units+=QualificationGapRepairR30R1.step(w.ro,(operation,batch)->{submit(w,operation,batch);return true;});}catch(RuntimeException sourceFailure){QualificationGapRepairR30R1.observeFailure(sourceFailure);}
            ensureControls(w);
            units += LiveShadowsR27.freeze(RuntimeR27.this, w, latest, latestOrd, deadline);
            units += LiveShadowsR27.freeze(RuntimeR27.this, w, latest, latestOrd, deadline, LiveShadowsR27.OBSERVE_STATES, false);
            units += LiveShadowsR27.certify(RuntimeR27.this, w, latestOrd);
            return units > 0 ? ResearchPoolR27.Outcome.progress(units, latestOrd) : ResearchPoolR27.Outcome.idle("NO_NEW_EVENT", latestOrd, latestOrd);
        }
    }

    /** Keeps one I00 and one shuffled-label control frozen every period next to the shadows (same-period negative controls). */
    void ensureControls(ResearchPoolR27.Worker w) throws Exception {
        EvolutionLoopR27 l = evo; if (l == null) return;
        Object[][] controls = {{"CONTROL_I00", l.i00Champion, l.i00Tau}, {"CONTROL_NULL", l.nullEvolution.champion, l.nullEvolution.championTau}};
        for (Object[] c : controls) {
            final PolicySpecR27 spec = (PolicySpecR27) c[1]; if (spec == null) continue; final String status = (String) c[0];
            boolean have; try (Cursor x = w.ro.rawQuery("SELECT 1 FROM r27_live_shadow WHERE status=?", new String[] {status})) { have = x.moveToFirst(); }
            if (have) continue;
            final double tau = (Double) c[2];String previousId=StatsR27.sha256(status+"|"+spec.policyHash());String previousStatus=QualificationGapRepairR30R1.text(w.ro,"SELECT status FROM r27_live_shadow WHERE policy_hash=?",previousId);
            final String id=QualificationGapRepairR30R1.SEALED.equals(previousStatus)?StatsR27.sha256(status+"|"+spec.policyHash()+"|R30_REPAIR_NEW_FUTURE_SEGMENT|"+latestIssue(w.ro)):previousId;
            ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch();
            b.guardBefore(db -> { StoreR27.insertShadow(db, spec, tau, status + ":same-period negative control", semanticAbi(spec), id, spec.policyHash());
                db.execSQL("UPDATE r27_live_shadow SET status=? WHERE policy_hash=?", new Object[] {status, id}); db.execSQL("UPDATE r27_eprocess SET state='CONTROL_NOT_ELIGIBLE' WHERE policy_hash=?", new Object[] {id}); return 0; });
            submit(w, "LIVE_CONTROL", b);
        }
    }

    /** Live dataset + placeholder for the next target (family streams continue from the previous cache). */
    FeatureCacheR27 liveCacheWithPlaceholder(ResearchPoolR27.Worker w, long latestOrd) {
        // Read the bounded source snapshot before reusing a stream: a backfill or correction can change
        // an older row without adding a new head. Incremental numerical state is reused only for an exact prefix.
        ReplayDatasetR27 fresh = StoreR27.loadDataset(w.ro, DATASET_ROWS);
        if (fresh.size() == 0 || fresh.ordinals[fresh.size() - 1] != latestOrd) return null;
        final ReplayDatasetR27 previousData;final FeatureCacheR27 previousCache;
        synchronized(this){previousData=liveData;previousCache=liveCache;}
        boolean prefix = previousData != null && previousData.size() <= fresh.size();
        if (prefix) for (int i = 0; i < previousData.size(); i++) {
            if (previousData.ordinals[i] != fresh.ordinals[i]
                    || !previousData.issues[i].equals(fresh.issues[i])
                    || !java.util.Arrays.equals(previousData.draws[i], fresh.draws[i])) { prefix = false; break; }
        }
        if (previousData != null && !prefix) {
            FeatureCacheR27.REBUILDS_R30.incrementAndGet();
            if (FeatureCacheR27.REBUILD_EVENTS_R30.size() < 256)
                FeatureCacheR27.REBUILD_EVENTS_R30.add(new Object[] {-1, latestOrd, "LIVE_SOURCE_PREFIX_CHANGED", System.currentTimeMillis()});
        }
        final String preparedKey=fresh.datasetHash + "|" + latestOrd + "|" + operatorAbiHash;
        ReplayDatasetR27 withSlot = fresh.append(new ReplayDatasetR27.Row("PLACEHOLDER", latestOrd + 1, new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}));
        FeatureCacheR27 c = featureCacheR31(withSlot,64);
        try{
            if (prefix && previousCache != null) c.extendFromForLiveR31(previousCache);
            synchronized(this){
                if(liveData!=previousData||liveCache!=previousCache)throw new java.util.concurrent.CancellationException("R31_STALE_LIVE_CACHE_PREPARATION");
                liveData=fresh;liveCacheKeyR30=preparedKey;liveCache=c;
            }
        }catch(RuntimeException|Error failed){c.close();throw failed;}
        // r27-live's synchronous prior freeze is finished before it asks for this next cache.
        if(previousCache!=null)previousCache.close();
        return c;
    }

    // ------------------------------------------------------------------ r27-abi (R27-010/011/012)
    final class AbiTask implements ResearchPoolR27.Task {
        public String id() { return "r27-abi"; }
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker w) throws Exception {
            ensureGolden();
            long units = LegacyContinuityR27.step(store, w.ro, golden, operatorAbiHash, w.writes());
            units += recordCacheRebuildsR30(w);
            units += LiveShadowsR27.semanticCheck(RuntimeR27.this, w);
            return units > 0 ? ResearchPoolR27.Outcome.progress(units, units) : ResearchPoolR27.Outcome.idle("NO_NEW_EVENT", 0, 0);
        }
    }
    // B02-C02/C03: an event remains in the producer queue until a durable receipt is observed.
    private static final Object CACHE_REBUILD_DELIVERY_R31=new Object();
    static final int CACHE_REBUILD_ROWS_R31=16,CACHE_REBUILD_BIND_BYTES_R31=8192;
    static final long CACHE_REBUILD_WORKING_BYTES_R31=65536;
    private static PreparedCacheRebuildsR31 pendingCacheRebuildsR31;
    private static boolean cacheRebuildInFlightR31;
    private static long cacheRebuildCommittedR31,cacheRebuildRetriesR31;
    private static final class CacheRebuildDependencyR31 extends IllegalStateException implements FailureDispositionR31.ClassifiedFailure {
        CacheRebuildDependencyR31(String reason){super(reason);}
        public String failureCategory(){return "DEPENDENCY";}public String operation(){return "R27_CACHE_REBUILD";}
        public String phase(){return "PREPARE";}public String commitOutcomeName(){return "NOT_SUBMITTED";}
    }
    private static final class PreparedCacheRebuildsR31 {
        final List<Object[]> originals,rows;
        final MemoryBudgetR31.Lease lease;
        final String inputHash,commitKey;
        PreparedCacheRebuildsR31(List<Object[]> originals,List<Object[]> rows,MemoryBudgetR31.Lease lease){
            this.originals=java.util.Collections.unmodifiableList(originals);this.rows=java.util.Collections.unmodifiableList(rows);this.lease=lease;
            // A nonce distinguishes two genuine events with identical values. Owner/fence are audit only.
            inputHash=HashingSU1.text("CACHE_REBUILD_INPUT_V1|"+java.util.UUID.randomUUID()+"|"+cacheRebuildHashR31(rows));
            commitKey=HashingSU1.text("r27-abi|CACHE_REBUILD|"+inputHash+"|R31_CACHE_REBUILD_EVENTS_V1");
        }
        ResearchWriteQueueR27.ImmutableBatch batch(RuntimeCoordinatorR26.Claim claim){
            ResearchWriteQueueR27.Batch b=new ResearchWriteQueueR27.Batch();
            b.guardBefore(db->{requireCacheRebuildSchemaR31(db);RuntimeCoordinatorR26.requireFence(db,claim);return 0;});
            for(Object[] row:rows)b.add("INSERT INTO r30_cache_rebuild(family_index,through_ordinal,reason,recorded_at) VALUES(?,?,?,?)",row);
            return b.prepareR31("CACHE_REBUILD",commitKey,"r27-abi",claim.owner,claim.fence,inputHash,0,"R31_CACHE_REBUILD_EVENTS_V1");
        }
    }
    private static void requireCacheRebuildSchemaR31(SQLiteDatabase db){
        DatabaseCoordinator.requireSchemaObjectsR31(db,"R27_CACHE_REBUILD",new String[][]{
            {"r30_cache_rebuild","id,family_index,through_ordinal,reason,recorded_at"},
            {"r31_commit_receipt","commit_key,payload_hash,operation,task_id,owner_epoch,fence,input_snapshot,chunk_seq,operator_version,committed_at"}},new String[0][]);
    }
    private static long cacheRebuildUtf8BytesR31(String value){
        long n=0;for(int i=0;i<value.length();i++){char c=value.charAt(i);if(c<128)n++;else if(c<2048)n+=2;else if(Character.isHighSurrogate(c)&&i+1<value.length()&&Character.isLowSurrogate(value.charAt(i+1))){n+=4;i++;}else n+=3;if(n>CACHE_REBUILD_BIND_BYTES_R31)return n;}return n;
    }
    private static String cacheRebuildHashR31(List<Object[]> rows){
        try{java.security.MessageDigest digest=java.security.MessageDigest.getInstance("SHA-256");
            java.io.DataOutputStream out=new java.io.DataOutputStream(new java.security.DigestOutputStream(new java.io.OutputStream(){public void write(int b){}},digest));
            out.writeInt(rows.size());for(Object[] row:rows){out.writeInt((Integer)row[0]);out.writeLong((Long)row[1]);byte[] text=((String)row[2]).getBytes(java.nio.charset.StandardCharsets.UTF_8);out.writeInt(text.length);out.write(text);out.writeLong((Long)row[3]);}out.close();
            StringBuilder hex=new StringBuilder(64);for(byte b:digest.digest())hex.append(String.format(java.util.Locale.ROOT,"%02x",b&255));return hex.toString();
        }catch(java.io.IOException|java.security.NoSuchAlgorithmException unavailable){throw new IllegalStateException("R31_CACHE_REBUILD_IDENTITY",unavailable);}
    }
    private static PreparedCacheRebuildsR31 prepareCacheRebuildsR31(){
        if(FeatureCacheR27.REBUILD_EVENTS_R30.peek()==null)return null;
        MemoryBudgetR31.Admission admission=MemoryBudgetR31.processBudget().tryAcquire("r31-cache-rebuild-prepared",MemoryBudgetR31.Kind.QUEUED,CACHE_REBUILD_WORKING_BYTES_R31,()->false);
        if(!admission.acquired())throw new MemoryBudgetR31.CapacityWait(admission.reason);
        boolean retained=false;
        try{
            List<Object[]> originals=new ArrayList<>(CACHE_REBUILD_ROWS_R31),rows=new ArrayList<>(CACHE_REBUILD_ROWS_R31);long bytes=0;
            for(Object[] original:FeatureCacheR27.REBUILD_EVENTS_R30){
                if(rows.size()==CACHE_REBUILD_ROWS_R31)break;
                if(original==null||original.length!=4||!(original[0] instanceof Integer)||!(original[1] instanceof Long)||!(original[2] instanceof String)||!(original[3] instanceof Long))throw new IllegalStateException("R31_CACHE_REBUILD_EVENT_INTEGRITY");
                long next=24+cacheRebuildUtf8BytesR31((String)original[2]);
                if(next>CACHE_REBUILD_BIND_BYTES_R31){if(rows.isEmpty())throw new CacheRebuildDependencyR31("R31_DEPENDENCY_CACHE_REBUILD_EVENT_EXCEEDS_BYTE_BUDGET");break;}
                if(bytes+next>CACHE_REBUILD_BIND_BYTES_R31)break;
                originals.add(original);rows.add(new Object[]{original[0],original[1],original[2],original[3]});bytes+=next;
            }
            if(rows.isEmpty())return null;
            PreparedCacheRebuildsR31 prepared=new PreparedCacheRebuildsR31(originals,rows,admission.lease);retained=true;return prepared;
        }finally{if(!retained)admission.lease.close();}
    }
    /** One finite attempt per ABI slice. Cancellation, rollback and UNKNOWN retain the same input/key.
     * Uncommitted diagnostic events are process-local; this is not a crash-durable training outbox. */
    static long recordCacheRebuildsR30(ResearchPoolR27.Worker w) throws Exception {
        RuntimeCoordinatorR26.Claim claim=RuntimeCoordinatorR26.currentClaimR30();
        if(claim==null||!"r27-abi".equals(claim.task))throw new CacheRebuildDependencyR31("R31_DEPENDENCY_CACHE_REBUILD_CLAIM_REQUIRED");
        synchronized(CACHE_REBUILD_DELIVERY_R31){if(pendingCacheRebuildsR31==null&&FeatureCacheR27.REBUILD_EVENTS_R30.peek()==null)return 0;}
        if(w.cancelled())throw new InterruptedException("R27_CANCELLED");
        requireCacheRebuildSchemaR31(w.ro);RuntimeCoordinatorR26.requireFence(w.ro,claim);
        PreparedCacheRebuildsR31 prepared;
        synchronized(CACHE_REBUILD_DELIVERY_R31){
            if(cacheRebuildInFlightR31)return 0;
            if(pendingCacheRebuildsR31==null)pendingCacheRebuildsR31=prepareCacheRebuildsR31();
            prepared=pendingCacheRebuildsR31;if(prepared==null)return 0;cacheRebuildInFlightR31=true;
        }
        boolean acknowledged=false;
        try{
            if(w.cancelled())throw new InterruptedException("R27_CANCELLED");
            if(!w.writes().submit("CACHE_REBUILD",prepared.batch(claim),w::cancelled))return 0;
            synchronized(CACHE_REBUILD_DELIVERY_R31){
                if(pendingCacheRebuildsR31!=prepared)throw new IllegalStateException("R31_CACHE_REBUILD_ACK_IDENTITY");
                for(Object[] original:prepared.originals)FeatureCacheR27.REBUILD_EVENTS_R30.remove(original);
                pendingCacheRebuildsR31=null;cacheRebuildCommittedR31+=prepared.rows.size();acknowledged=true;
            }
            return prepared.rows.size();
        }finally{
            try{if(acknowledged)prepared.lease.close();}
            finally{synchronized(CACHE_REBUILD_DELIVERY_R31){cacheRebuildInFlightR31=false;if(!acknowledged)cacheRebuildRetriesR31++;}}
        }
    }
    static Map<String,Object> cacheRebuildDeliverySnapshotR31(){
        synchronized(CACHE_REBUILD_DELIVERY_R31){Map<String,Object> out=new LinkedHashMap<>();PreparedCacheRebuildsR31 p=pendingCacheRebuildsR31;
            out.put("queued",FeatureCacheR27.REBUILD_EVENTS_R30.size());out.put("preparedEvents",p==null?0:p.rows.size());out.put("preparedCommitKey",p==null?null:p.commitKey);out.put("inFlight",cacheRebuildInFlightR31);out.put("committedEvents",cacheRebuildCommittedR31);out.put("retainedAttempts",cacheRebuildRetriesR31);out.put("maxEventsPerChunk",CACHE_REBUILD_ROWS_R31);out.put("maxBindBytes",CACHE_REBUILD_BIND_BYTES_R31);out.put("declaredPreparedBytes",p==null?0:p.lease.bytes());out.put("workingEstimate","64KiB_BOUNDED_ROWS_COPY_UTF8_HASH_AND_IMMUTABLE_BATCH_JAVA_ESTIMATE_NOT_MEASURED_HEAP_OR_SQLITE_NATIVE");out.put("durability","PROCESS_LOCAL_UNTIL_SQLITE_RECEIPT; UNCOMMITTED_EVENTS_NOT_PRESERVED_AFTER_PROCESS_DEATH");return java.util.Collections.unmodifiableMap(out);}
    }
    void ensureGolden() throws Exception {
        if (golden != null && operatorAbi != null && operatorAbiHash != null) return;
        final ReplayDatasetR27 prepared;
        try (InputStream in = store.contextForR26().getAssets().open(SemanticAbiR27.GOLDEN_ASSET)) { prepared = SemanticAbiR27.readGolden(in, true); }
        FeatureCacheR27 cache = new FeatureCacheR27(prepared, 64);
        Map<String,String> abi = OperatorGoldenR27.fingerprints(cache);
        String hash = SemanticAbiR27.operatorSetHash(abi);
        store.setState("r27_operator_abi", hash);
        synchronized(this) { goldenCache=cache;operatorAbi=abi;operatorAbiHash=hash;golden=prepared; }
        // Fingerprints above were computed in instance isolation. Only the resulting real operator
        // identity may authorize sharing with other dataset caches.
        for(FeatureCacheR27 existing:new FeatureCacheR27[]{cache,evoCache,pendingCache,liveCache})
            if(existing!=null)existing.bindSharedFeaturesR31(hash);
    }

    // ------------------------------------------------------------------ r27-atoms (R27-043/044) and r27-atomnet (R27-033/034/036)
    final class ObservatoryTask implements ResearchPoolR27.Task {public String id(){return "r27-observatory-r1";}public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker w)throws Exception{return ResearchObservatoryR30R1.step(RuntimeR27.this,w);}}
    final class ResearchStudyTask implements ResearchPoolR27.Task {
        public String id(){return "r27-study-r1";}
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker w)throws Exception{return ResearchStudyTaskR30R1.step(RuntimeR27.this,w);}
    }
    final class MatrixProductionTask implements ResearchPoolR27.Task {
        public String id(){return "r27-matrices-r1";}
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker w)throws Exception{return MatrixResearchR30R1.step(RuntimeR27.this,w);}
    }

    final class AtomsTask implements ResearchPoolR27.Task {
        public String id() { return "r27-atoms"; }
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker w) throws Exception { return AtomWorldR27.atomsStep(RuntimeR27.this, w); }
    }
    final class AtomNetTask implements ResearchPoolR27.Task {
        public String id() { return "r27-atomnet"; }
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker w) throws Exception { return AtomWorldR27.atomNetStep(RuntimeR27.this, w); }
    }

    // ------------------------------------------------------------------ r27-calibration (R27-037/038)
    final class CalibrationTask implements ResearchPoolR27.Task {
        public String id() { return "r27-calibration"; }
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker w) throws Exception { return LiveShadowsR27.calibrate(RuntimeR27.this, w); }
    }

    // ------------------------------------------------------------------ r27-chart-cache (R27-049)
    final class ChartTask implements ResearchPoolR27.Task {
        long lastBuilt;
        public String id() { return "r27-chart-cache"; }
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker w) throws Exception {
            if (System.currentTimeMillis() - lastBuilt < 30_000) return ResearchPoolR27.Outcome.idle("NO_NEW_EVENT", 0, 0);
            ResearchWriteQueueR27.Batch b = ChartCacheR27.build(w.ro, RuntimeR27.this);
            long preparedAt=System.currentTimeMillis();ResearchStoreR30R1.appendOwnedProgress(b,ResearchStoreR30R1.calendar(w.ro),new ResearchStoreR30R1.Facts(preparedAt/1000,preparedAt/1000,0L,null,null,HashingSU1.text("CHART_CACHE|"+preparedAt)));
            submit(w, "CHART_CACHE", b, false, "CHART_OBSERVATION|" + preparedAt
                    + "|" + android.os.SystemClock.elapsedRealtime()); lastBuilt = preparedAt;
            return ResearchPoolR27.Outcome.progress(b.ops.size(), lastBuilt / 1000);
        }
    }

    // ------------------------------------------------------------------ r27-legacy-export (R27-019/020)
    final class LegacyExportTask implements ResearchPoolR27.Task {
        public String id() { return "r27-legacy-export"; }
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker w) throws Exception { return LegacyPolicyExportR27.step(RuntimeR27.this, w); }
    }

    // ------------------------------------------------------------------ r27-health (R27-006/048)
    final class HealthTask implements ResearchPoolR27.Task {
        long last, lastSize, lastElapsedR31; boolean assetsCached; final Map<String, Long> fetchFlushed = new HashMap<>();
        NullPipelineSimulationR30 q08;
        public String id() { return "r27-health"; }
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker w) throws Exception {
            final long tickElapsed=android.os.SystemClock.elapsedRealtime();
            // Deliver observations before optional maintenance can encounter backlog backpressure.
            WriterHoldFaultsR31.drainOne(store);
            // A bounded slice of new private scratch, independent of the business publication ACK.
            ResearchWriteQueueR27.Batch evolutionCleanup = EvolutionPublicationStoreR31.cleanupSlice(
                    w.ro, LOOP, RuntimeCoordinatorR26.currentClaimR30());
            if (evolutionCleanup != null) {
                String publication = String.valueOf(evolutionCleanup.ops.get(0).args[0]);
                String sliceHash = evolutionCleanup.prepareR31("EVOLUTION_PRIVATE_SCRATCH_CLEANUP",
                        "FINGERPRINT_ONLY", "r27-health", "FINGERPRINT_ONLY", 0, publication,
                        0, "R31_PRIVATE_SCRATCH_SLICE_V1").payloadHash;
                submit(w, "EVOLUTION_PRIVATE_SCRATCH_CLEANUP", evolutionCleanup, false,
                        "NEW_PRIVATE_SCRATCH_ONLY|" + publication + "|slice=" + sliceHash);
            }
            if (lastElapsedR31>0 && tickElapsed>=lastElapsedR31 && tickElapsed-lastElapsedR31<60_000)
                return ResearchPoolR27.Outcome.idle("NO_NEW_EVENT", 0, 0);
            // A low-priority owner samples disk outside every SQLite write transaction.
            // Wall-clock correction must not stop observations for the duration of the jump.
            Map<String,Object> storageObservation=new LinkedHashMap<>();
            try {
                storageObservation.putAll(StorageLedgerR31.sampleIfDue(store.contextForR26().getFilesDir(),
                        "CONTINUOUS_RESEARCH_HEALTH",id(),false));
            } catch(java.io.IOException unavailable) {
                storageObservation.put("status","UNKNOWN_STORAGE_SAMPLE_FAILED");
                storageObservation.put("reason",unavailable.getClass().getSimpleName()+":"+unavailable.getMessage());
                storageObservation.put("regularFileBytes",null);
                storageObservation.put("allocatedBlocksBytes",null);
                storageObservation.put("physicalMeasurementComplete",false);
            }
            MemoryBudgetR31.processBudget().sample();
            final Map<String,Object> memoryObservation=MemoryBudgetR31.processBudget().snapshot();
            final long preparedAt=System.currentTimeMillis();w.writes().flush();
            final Map<String,Long> nextFetchFlushed=new HashMap<>(fetchFlushed);
            final boolean nextAssetsCached=true;long nextSize=lastSize;
            NullPipelineSimulationR30 nextQ08=q08==null?NullPipelineSimulationStoreR30.load(w.ro,BuildConfig.SOURCE_COMMIT_R26):NullPipelineSimulationR30.restore(q08.snapshot());
            ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch();
            long q08Deadline=System.nanoTime()+100_000_000L;
            for(int units=0;units<64&&!nextQ08.complete()&&System.nanoTime()<q08Deadline;units++){
                if(!nextQ08.cheapWorkPending()&&pool.narrow(System.currentTimeMillis()))break;
                nextQ08.step(); // one checkpointable unit, no database writer is held during computation
            }
            NullPipelineSimulationStoreR30.checkpoint(b,nextQ08,preparedAt);
            if (!assetsCached) {
                // R27-053/054: build identity, card registry, source audit and R26→R27 merge status packaged as assets.
                for (String name : new String[] {"build-identity", "r27_cards", "source-audit", "merge-status"}) {
                    String payload;
                    try (InputStream in = store.contextForR26().getAssets().open("r27/" + name + ".json")) {
                        java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream(); byte[] chunk = new byte[8192]; int k; while ((k = in.read(chunk)) > 0) buf.write(chunk, 0, k);
                        payload = buf.toString("UTF-8"); JsonR27.parse(payload);
                    } catch (Exception missing) { Map<String, Object> m = new LinkedHashMap<>(); m.put("status", "INSUFFICIENT"); m.put("reason", "asset r27/" + name + ".json unavailable: " + missing.getClass().getSimpleName()); payload = JsonR27.write(m); }
                    b.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('asset',?,?,?)", name, payload, preparedAt);
                }
                Map<String, Object> runtime = new LinkedHashMap<>(); runtime.put("sourceCommit", BuildConfig.SOURCE_COMMIT_R26); runtime.put("versionCode", (long) BuildConfig.VERSION_CODE); runtime.put("versionName", BuildConfig.VERSION_NAME);
                b.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('asset','runtime-identity',?,?)", JsonR27.write(runtime), preparedAt);
                // R29-001/002: measured baseline identity and the preserved requirement ledger (read by ReportR29).
                b.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('r29','baseline-identity',?,?)", JsonR27.write(BaselineIdentityR29.describe(store.contextForR26())), preparedAt);
                b.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('r29','legacy-preservation',?,?)", JsonR27.write(LegacyContinuityR27.preservation(store.contextForR26())), preparedAt);
                
            }
            // R27-048: fetch write-rejection causes since process start (absolute counts per process; delta-added).
            for (Map.Entry<String, long[]> e : com.ethk3.novaonepro.FetchLedgerR925.rejectCountsR27().entrySet()) {
                long prev = nextFetchFlushed.getOrDefault(e.getKey(), 0L), delta = e.getValue()[0] - prev;
                if (delta <= 0) continue;
                b.add("INSERT OR IGNORE INTO r27_fetch_reject(reason,count,last_issue,last_at) VALUES(?,0,NULL,?)", e.getKey(), e.getValue()[1]);
                b.add("UPDATE r27_fetch_reject SET count=count+?,last_at=? WHERE reason=?", delta, e.getValue()[1], e.getKey());
                nextFetchFlushed.put(e.getKey(), e.getValue()[0]);
            }
            // R27-047: database size and the largest tables (measurement every hour). Archival of old-chain tables is
            // gated on the legacy switch being off after the observation period; history stays queryable.
            if (preparedAt - lastSize > 3_600_000L) { nextSize = preparedAt; b.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('retention','db_size',?,?)", JsonR27.write(dbSize(w.ro)), preparedAt); }
            // wait_since bookkeeping for alarms is maintained by receipts; here we only stamp alarms for the report.
            List<WaitKindR27.Health> h = health(w.ro, preparedAt); int stuck = 0, never = 0;
            for (WaitKindR27.Health x : h) { if (x.stuck) stuck++; if (x.neverRun) never++; }
            Map<String, Object> m = new LinkedHashMap<>(); m.put("stuck", (long) stuck); m.put("neverRun", (long) never); m.put("checkedAt", preparedAt);
            m.put("workerLimit", (long) pool.workerLimit()); m.put("thermalReason", pool.thermalReason());
            m.put("storageObservationR31",storageObservation);
            m.put("memoryObservationR31",memoryObservation);
            Map<String,Object> caches=new LinkedHashMap<>();
            if(evoCache!=null)caches.put("evolution",evoCache.memorySnapshotR31());
            if(pendingCache!=null)caches.put("pending",pendingCache.memorySnapshotR31());
            if(liveCache!=null)caches.put("live",liveCache.memorySnapshotR31());
            m.put("featureCacheObservationR31",caches);
            m.put("observationScopeR31","PER_PID_MEMORY_AND_INTERVAL_DISK; NOT_MODEL_PROGRESS_OR_NATURAL_QUALITY");
            b.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('health','summary',?,?)", JsonR27.write(m), preparedAt);
            ResearchStoreR30R1.appendOwnedProgress(b,ResearchStoreR30R1.calendar(w.ro),new ResearchStoreR30R1.Facts(preparedAt/1000,preparedAt/1000,0L,null,null,HashingSU1.text("HEALTH|"+preparedAt)));
            submit(w, "HEALTH", b, false, "HEALTH_OBSERVATION|" + IssueTimelineWriterR30.bootId()
                    + "|" + tickElapsed + "|" + preparedAt);
            last=preparedAt;lastElapsedR31=tickElapsed;lastSize=nextSize;assetsCached=nextAssetsCached;q08=nextQ08;fetchFlushed.clear();fetchFlushed.putAll(nextFetchFlushed);
            return ResearchPoolR27.Outcome.progress(1, preparedAt / 1000);
        }
    }

    public static List<WaitKindR27.Health> health(SQLiteDatabase db, long now) {
        List<WaitKindR27.Health> out = new ArrayList<>();
        try (Cursor c = db.rawQuery("SELECT task_id,wait_kind,wait_detail,wait_since,registered_at,cursor,last_progress FROM r26_task", null)) {
            while (c.moveToNext()) out.add(WaitKindR27.health(c.getString(0), c.getString(1), c.getString(2), c.getLong(3), c.getLong(4), c.getLong(5), c.getLong(6), now));
        } catch (RuntimeException schemaPending) { }
        return out;
    }

    static Map<String, Object> dbSize(SQLiteDatabase db) {
        Map<String, Object> m = new LinkedHashMap<>(); long pages = 0, size = 0;
        try (Cursor c = db.rawQuery("PRAGMA page_count", null)) { if (c.moveToFirst()) pages = c.getLong(0); }
        try (Cursor c = db.rawQuery("PRAGMA page_size", null)) { if (c.moveToFirst()) size = c.getLong(0); }
        m.put("bytes", pages * size); m.put("targetBytes", 1L << 30);
        String legacy = "1"; try (Cursor c = db.rawQuery("SELECT state_value FROM app_state WHERE state_key=?", new String[] {com.ethk3.novaonepro.ResearchLearningContinuityR912.R27_LEGACY_SWITCH})) { if (c.moveToFirst()) legacy = c.getString(0); } catch (RuntimeException e) { }
        m.put("legacyResearchEnabled", legacy); m.put("archival", "1".equals(legacy) ? "PENDING_LEGACY_RETIREMENT(R27-046: parallel observation ≥1440 periods first)" : "ELIGIBLE");
        Map<String, Object> top = new LinkedHashMap<>();
        for (String t : new String[] {"research_evolution_event_v41", "r974_model_lineage", "r967_numeric_training_run", "atomic_tool_context_r943", "research_atom_context_r932", "r26_training_step", "r26_progress", "r27_policy", "r27_live_frozen", "r27_replay_score"})
            try (Cursor c = db.rawQuery("SELECT count(*) FROM " + t, null)) { if (c.moveToFirst()) top.put(t, c.getLong(0)); } catch (RuntimeException absent) { }
        m.put("rows", top); return m;
    }
}
