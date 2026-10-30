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

/** Research runtime. After verified database publication, the drained legacy dispatcher also registers its
 * existing numerical tasks on this pool; its original VM/checkpoint storage stays retained. Tasks:
 * r27-replay-dataset, r27-evolution, r27-live, r27-atoms, r27-atomnet, r27-abi, r27-calibration, r27-chart-cache,
 * r27-legacy-export, r27-health. Every slice reports a WaitKindR27 reason through the pool. */
public final class RuntimeR27 implements AutoCloseable {
    public static final int DATASET_ROWS = 5000, REFRESH_NEW_ROWS = 360, LIVE_REBASE_ROWS = 6500;
    public static final String LOOP = "MAIN";
    static final String EVOLUTION_CHECKSUM_DDL="CREATE TABLE IF NOT EXISTS r29_evolution_checkpoint_checksum(loop TEXT PRIMARY KEY,snapshot_hash TEXT NOT NULL,dataset_hash TEXT NOT NULL,generation INTEGER NOT NULL,step INTEGER NOT NULL,rng_state INTEGER NOT NULL)";
    static final class MissingEvolutionDataset extends RuntimeException {MissingEvolutionDataset(String hash){super("WAITING_IMMUTABLE_EVOLUTION_DATASET_PIN:"+hash);}}
    /** Database work ends before the cooperative numerical context starts. Large snapshots never cross a
     * CursorWindow as one TEXT cell; metadata is checked again after bounded BLOB reads. */
    static final class EvolutionCheckpoint {
        final byte[] bytes;final String datasetHash,checksum;final int generation,step;final long rng,updated;
        EvolutionCheckpoint(byte[] bytes,String hash,String checksum,int generation,int step,long rng,long updated){this.bytes=bytes;datasetHash=hash;this.checksum=checksum;this.generation=generation;this.step=step;this.rng=rng;this.updated=updated;}
    }
    static EvolutionCheckpoint captureEvolutionCheckpoint(SQLiteDatabase db){
        String hash;int generation,step,length;long rng,updated;
        try(Cursor c=db.rawQuery("SELECT dataset_hash,generation,step,rng_state,updated_at,length(CAST(snapshot AS BLOB)) FROM r27_evolution_cursor WHERE loop=?",new String[]{LOOP})){
            if(!c.moveToFirst())return null;hash=c.getString(0);generation=c.getInt(1);step=c.getInt(2);rng=c.getLong(3);updated=c.getLong(4);length=c.getInt(5);
        }
        if(hash==null||!hash.matches("[a-f0-9]{64}")||generation<0||step<1||step>9||length<2||length>(16<<20))throw new IllegalStateException("R29_EVOLUTION_CHECKPOINT_METADATA_INVALID");
        byte[] bytes=new byte[length];String[] identity={LOOP,hash,Integer.toString(generation),Integer.toString(step),Long.toString(rng),Long.toString(updated),Integer.toString(length)};
        for(int offset=0;offset<length;offset+=128<<10){int n=Math.min(128<<10,length-offset);String[] args=new String[identity.length+2];args[0]=Integer.toString(offset+1);args[1]=Integer.toString(n);System.arraycopy(identity,0,args,2,identity.length);
            try(Cursor c=db.rawQuery("SELECT substr(CAST(snapshot AS BLOB),CAST(? AS INTEGER),CAST(? AS INTEGER)) FROM r27_evolution_cursor WHERE loop=? AND dataset_hash=? AND generation=CAST(? AS INTEGER) AND step=CAST(? AS INTEGER) AND rng_state=CAST(? AS INTEGER) AND updated_at=CAST(? AS INTEGER) AND length(CAST(snapshot AS BLOB))=CAST(? AS INTEGER)",args)){
                if(!c.moveToFirst())throw new IllegalStateException("CP3_DB_BUSY_EVOLUTION_CHECKPOINT_CHANGED");byte[] part=c.getBlob(0);if(part==null||part.length!=n)throw new IllegalStateException("R29_EVOLUTION_CHECKPOINT_TRUNCATED");System.arraycopy(part,0,bytes,offset,n);
            }
        }
        String checksum=null;if(SchemaR27.tableExists(db,"r29_evolution_checkpoint_checksum"))try(Cursor c=db.rawQuery("SELECT snapshot_hash,dataset_hash,generation,step,rng_state FROM r29_evolution_checkpoint_checksum WHERE loop=?",new String[]{LOOP})){
            if(c.moveToFirst()){checksum=c.getString(0);if(!hash.equals(c.getString(1))||generation!=c.getInt(2)||step!=c.getInt(3)||rng!=c.getLong(4)||checksum==null||!checksum.matches("[a-f0-9]{64}"))throw new IllegalStateException("R29_EVOLUTION_CHECKPOINT_BINDING_MISMATCH");}
        }
        return new EvolutionCheckpoint(bytes,hash,checksum,generation,step,rng,updated);
    }
    @SuppressWarnings("unchecked") static EvolutionLoopR27 restoreExact(EvolutionCheckpoint saved,EvolutionLoopR27.Config cfg,FeatureCacheR27 cache,EvolutionLoopR27.Clock clock){
        CooperativeContinuationR26.poll();if(saved.checksum!=null&&!saved.checksum.equals(StatsR27.sha256(saved.bytes)))throw new IllegalStateException("R29_EVOLUTION_CHECKPOINT_CHECKSUM");
        String json=new String(saved.bytes,java.nio.charset.StandardCharsets.UTF_8);Map<String,Object> header=(Map<String,Object>)JsonR27.parseCooperative(json);
        if(!saved.datasetHash.equals(cache.data.datasetHash)||!saved.datasetHash.equals(header.get("datasetHash"))||saved.generation!=(int)JsonR27.num(header.get("generation"))||saved.step!=(int)JsonR27.num(header.get("step"))||!(header.get("rng") instanceof Number)||saved.rng!=((Number)header.get("rng")).longValue())throw new IllegalStateException("R29_EVOLUTION_CHECKPOINT_PROVENANCE_MISMATCH");
        return EvolutionLoopR27.restore(json,cfg,cache,null,clock);
    }
    final DbStore store; final ResearchPoolR27 pool;
    // Evolution dataset snapshot (rebound only between generations).
    volatile ReplayDatasetR27 evoData; volatile FeatureCacheR27 evoCache; volatile ReplayDatasetR27 pendingData; volatile FeatureCacheR27 pendingCache;
    volatile EvolutionLoopR27 evo;
    // Live dataset grows by appending; family streams continue incrementally.
    ReplayDatasetR27 liveData; FeatureCacheR27 liveCache;
    // Golden set and operator ABI (semantic ABI).
    volatile ReplayDatasetR27 golden; volatile FeatureCacheR27 goldenCache; volatile Map<String, String> operatorAbi; volatile String operatorAbiHash;
    // Atom replay frame and states.
    volatile AtomReplayR27.Frame frame; volatile String frameDataset = ""; int frameCursor; double[][] frameValues; boolean[][] framePresent; int[] frameHit, framePos, frameActual; String[] frameIssue;
    final Map<Integer, String> atomStates = new HashMap<>();
    volatile AtomNetTrainerR27 atomNet; volatile String atomNetDataset = "";
    FtrlR27 atomFtrl; String atomTrainedThrough = "";
    private final MigrationTask migration = new MigrationTask();
    private final EvolutionTask evolutionTask = new EvolutionTask();
    private final EvolutionAccess evolutionAccess = new EvolutionAccess();
    private final List<CooperativeResearchTaskR29> slicedTasks=new ArrayList<>();
    private ResearchPoolR27.Task sliced(ResearchPoolR27.Task task){CooperativeResearchTaskR29 slice=new CooperativeResearchTaskR29(task,pool.session());slicedTasks.add(slice);return slice;}
    private volatile Object[][] committedControls = new Object[0][];
    /** A private evolution turn hides mutable state while parked; readers never wait for its CPU quantum. */
    static final class EvolutionAccess {
        final java.util.concurrent.locks.ReentrantReadWriteLock lock=new java.util.concurrent.locks.ReentrantReadWriteLock();
        private boolean privateTurn;
        Lease read(){java.util.concurrent.locks.Lock r=lock.readLock();if(!r.tryLock())return null;if(privateTurn){r.unlock();return null;}return new Lease(r);}
        boolean begin(){java.util.concurrent.locks.Lock w=lock.writeLock();if(!w.tryLock())return false;try{if(privateTurn)return false;privateTurn=true;return true;}finally{w.unlock();}}
        void publish(){java.util.concurrent.locks.Lock w=lock.writeLock();w.lock();try{privateTurn=false;}finally{w.unlock();}}
        static final class Lease implements AutoCloseable{final java.util.concurrent.locks.Lock lock;Lease(java.util.concurrent.locks.Lock lock){this.lock=lock;}public void close(){lock.unlock();}}
    }
    private final LegacyCapabilitySnapshotR29 legacySnapshot;

    private volatile boolean closing;
    boolean isClosing(){return closing;}
    private static volatile RuntimeR27 active;
    public static RuntimeR27 active() { return active; }

    public RuntimeR27(DbStore store, String session) {
        this(store,session,null);
    }
    RuntimeR27(DbStore store,String session,RuntimeCoordinatorR26 legacy) {
        this.store = store;
        legacySnapshot=new LegacyCapabilitySnapshotR29(store,session);
        CooperativeContinuationR26.threadStartHook = () -> android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_BACKGROUND);
        pool = new ResearchPoolR27(store, session, ResearchPoolR27.defaultThreads());
        pool.register(new EventTask()); pool.register(migration); pool.register(sliced(new DatasetTask())); pool.register(sliced(new LiveTask())); pool.register(evolutionTask); pool.register(sliced(new AbiTask()));
        pool.register(sliced(new AtomsTask())); pool.register(sliced(new AtomNetTask())); pool.register(sliced(new CalibrationTask())); pool.register(sliced(new ChartTask()));
        pool.register(sliced(new LegacyExportTask())); pool.register(sliced(new HealthTask()));
        if(legacy!=null){pool.register(new LegacyCapabilityAdapterR29(legacy));pool.register(sliced(new LegacyCapabilityAdapterR29.ContinuityTask(legacy)));}
        pool.register(sliced(legacySnapshot));
        active = this;
    }
    public void start() { pool.start(); }
    public ResearchPoolR27 pool() { return pool; }
    @Override public void close() {
        closing=true;for(CooperativeResearchTaskR29 task:slicedTasks)task.cancel();legacySnapshot.cancel();AtomWorldR27.cancelPretrain(this);evolutionTask.close();
        while(!AtomWorldR27.awaitPretrainStopped(this,1000))android.util.Log.w("STARCORE_R27","draining cancelled AtomNet pretraining");
        for(CooperativeResearchTaskR29 task:slicedTasks)while(!task.awaitStopped(1000))android.util.Log.w("STARCORE_R27","draining cancelled "+task.id());
        while(!legacySnapshot.awaitStopped(1000))android.util.Log.w("STARCORE_R27","draining cancelled legacy state decode");
        pool.close();migration.close();if(active==this)active=null;
    }

    static String latestIssue(SQLiteDatabase ro) { try (Cursor c = ro.rawQuery("SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1", null)) { return c.moveToFirst() ? c.getString(0) : null; } }
    static long latestOpen(SQLiteDatabase ro) { try (Cursor c = ro.rawQuery("SELECT open_time FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1", null)) { return c.moveToFirst() ? c.getLong(0) : 0; } }
    static void submit(ResearchPoolR27.Worker w, String op, ResearchWriteQueueR27.Batch b) throws InterruptedException {
        while (!w.writes().submit(op, b, w::cancelled)) { if (w.cancelled()) throw new InterruptedException("R27_CANCELLED"); CooperativeContinuationR26.poll();Thread.sleep(50); }
    }

    // ------------------------------------------------------------------ r27-replay-dataset (R27-013/016)
    final class EventTask implements ResearchPoolR27.Task {
        long lastDrain;
        public String id() { return "r29-event-inbox"; }
        public boolean urgent(ResearchPoolR27.Worker worker,long now){return now-lastDrain>=1000;}
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker w) throws Exception {
            try{ResearchPoolR27.Outcome delivered=ResearchEventBridgeR29.deliver(w);long projected=RealtimeResearchProjectionR29.drain(w)+LegacyLiveProjectionR29.drain(w.store())+LegacyAuthorityProjectionR29.drain(w.store());lastDrain=System.currentTimeMillis();return projected>0?ResearchPoolR27.Outcome.progress(delivered.units+projected,delivered.cursor):delivered;}
            catch(IllegalStateException busy){if(!ResearchWriteQueueR27.writerBusy(busy))throw busy;return ResearchPoolR27.Outcome.idle("COOPERATIVE_WRITER_BUSY:EVENT_REDELIVERY_PENDING",0,-1);}
        }
    }

    /** Baseline copy is real independent-file work; all live owners remain on the old database until cutover. */
    final class MigrationTask implements ResearchPoolR27.Task, AutoCloseable {
        private ResearchDatabaseR29 stage; private long reported;
        public String id() { return "r29-research-migration"; }
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker w) throws Exception {
            long now=System.currentTimeMillis();
            try {
                if(ResearchRouteR29.active(w.realtimeRo())){
                    if(stage!=null){stage.close();stage=null;}
                    if(now-reported>=15_000){Map<String,Object> m=new LinkedHashMap<>();m.put("state","ACTIVE_INDEPENDENT_R27_RESEARCH");m.put("activeOwner",ResearchRouteR29.FILE_NAME);m.put("cutoverEnabled",true);m.put("routingRecoveryRequired",pool.routingRecoveryRequired());m.put("legacyR26Adapter","RESEARCH_POOL_PLUGIN_AFTER_DRAIN");m.put("legacyStateStorage","ORIGINAL_VM_AND_CHECKPOINT_OWNER_RETAINED");publish(w,m,now);}
                    return ResearchPoolR27.Outcome.idle("ACTIVE_R27_DATABASE_LEGACY_VM_RETAINED",1,1);
                }
                if(stage==null)stage=ResearchDatabaseR29.acquire(store);
                MigrationCopyR29.Result result=stage.step(store,w.realtimeRo());
                long verified=stage.verifiedTables();
                if(verified>0){String handoff=ResearchCutoverR29.run(store,w);if(handoff.startsWith("ACTIVE"))return ResearchPoolR27.Outcome.progress(1,verified);}
                if(now-reported>=15_000||result.phase.startsWith("VERIFIED"))publish(w,stage.diagnostics(w.realtimeRo()),now);
                if(result.phase.startsWith("IDLE"))return ResearchPoolR27.Outcome.idle("MIGRATION_CATCHUP_AND_READER_ROUTING_PENDING",verified,stage.tableCount());
                if(result.phase.startsWith("SOURCE_CHANGED"))return ResearchPoolR27.Outcome.idle("WAITING_STABLE_SOURCE:"+result.table+":"+result.phase,verified,stage.tableCount());
                return ResearchPoolR27.Outcome.progress(Math.max(1,result.rows),verified);
            }catch(ResearchDatabaseR29.StorageBlocked blocked){
                if(now-reported>=15_000){Map<String,Object> m=new LinkedHashMap<>();m.put("state","BLOCKED_STORAGE_BUDGET");m.put("reason",blocked.getMessage());m.put("cutoverEnabled",false);m.put("activeOwner","ORIGINAL_DATABASE");publish(w,m,now);}
                return ResearchPoolR27.Outcome.idle("RESOURCE_LOW_STORAGE:MIGRATION_ONLY:"+blocked.getMessage(),0,-1);
            }catch(Exception failed){
                if(ResearchWriteQueueR27.writerBusy(failed))return ResearchPoolR27.Outcome.idle("COOPERATIVE_WRITER_BUSY:MIGRATION_RETRY",stage==null?0:stage.verifiedTables(),-1);
                if(String.valueOf(failed.getMessage()).contains("BUDGET_YIELD"))return ResearchPoolR27.Outcome.idle("MIGRATION_YIELDED:"+failed.getMessage(),stage==null?0:stage.verifiedTables(),-1);
                boolean switched=ResearchRouteR29.active(w.realtimeRo());
                Map<String,Object> m=new LinkedHashMap<>();m.put("state",switched?"FAILED_ACTIVE_ROUTING_RECOVERY_REQUIRED":"FAILED_PREPARATION_ORIGINAL_OWNER_RETAINED");m.put("reason",failed.getClass().getSimpleName()+":"+failed.getMessage());m.put("cutoverEnabled",switched);m.put("activeOwner",switched?ResearchRouteR29.FILE_NAME:"ORIGINAL_DATABASE");m.put("repairCard","R29-004");
                try{publish(w,m,now);}catch(Exception reportFailure){failed.addSuppressed(reportFailure);}throw failed;
            }
        }
        private void publish(ResearchPoolR27.Worker w,Map<String,Object> snapshot,long now)throws Exception {
            ResearchWriteQueueR27.Batch b=new ResearchWriteQueueR27.Batch();
            b.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('r29','research-migration',?,?)",JsonR27.write(snapshot),now);
            submit(w,"MIGRATION_STATUS",b);reported=now;
        }
        public void close(){if(stage!=null){stage.close();stage=null;}}
    }

    final class DatasetTask implements ResearchPoolR27.Task {
        public String id() { return "r27-replay-dataset"; }
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker w) throws Exception {
            try(EvolutionAccess.Lease lease=evolutionAccess.read()){
            if(lease==null)return ResearchPoolR27.Outcome.idle("COOPERATIVE_PRIVATE_EVOLUTION_TURN",0,-1);
            String latest = latestIssue(w.ro); if (latest == null) return ResearchPoolR27.Outcome.idle("WAITING_DRAWS", 0, 0);
            long latestOrd = TargetIssueResolver.sequenceOrdinalSU1(latest);
            ReplayDatasetR27 cur = pendingData != null ? pendingData : evoData;
            if (cur != null && latestOrd - cur.ordinals[cur.size() - 1] < REFRESH_NEW_ROWS) return ResearchPoolR27.Outcome.idle("NO_NEW_EVENT", cur.ordinals[cur.size() - 1], latestOrd);
            long t0 = System.currentTimeMillis();
            ReplayDatasetR27 d = StoreR27.loadDataset(w.ro, DATASET_ROWS);
            if (d.size() < 3 * SplitR27.BLOCK) return ResearchPoolR27.Outcome.idle("WAITING_DRAWS:rows=" + d.size(), d.size(), 3 * SplitR27.BLOCK);
            FeatureCacheR27 c = new FeatureCacheR27(d);
            SplitR27 sp = SplitR27.of(d, evo == null ? null : evo.ledger().degradedBlocks());
            ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch();
            b.add("INSERT OR IGNORE INTO r27_replay_dataset(dataset_hash,from_issue,to_issue,rows,segments,split,built_at,build_ms) VALUES(?,?,?,?,?,?,?,?)",
                    d.datasetHash, d.issues[0], d.issues[d.size() - 1], d.size(), d.segments(), sp.describe(), System.currentTimeMillis(), System.currentTimeMillis() - t0);
            submit(w, "REPLAY_DATASET", b);
            if (evoData == null) { evoData = d; evoCache = c; } else { pendingData = d; pendingCache = c; }
            return ResearchPoolR27.Outcome.progress(1, d.ordinals[d.size() - 1]);
            }
        }
    }

    // ------------------------------------------------------------------ r27-evolution (R27-021..032)
    final class EvolutionTask implements ResearchPoolR27.Task, AutoCloseable {
        private volatile boolean stopping;
        private Turn turn;private EvolutionLoopR27 deferredObservers;
        final class Turn {
            FeatureCacheR27 cache;final FeatureCacheR27 rebind;ReplayDatasetR27 data;final EvolutionLoopR27 prior;
            final EvolutionCheckpoint saved;final EvolutionDatasetPinR29.Stored pinned;final int nextGeneration;final Map<String,Double> weights;final List<PolicyAstR27.Node> leaves;
            final ReplayDatasetR27 recoveryFutureData;final FeatureCacheR27 recoveryFutureCache;
            EvolutionLoopR27 loop;StoreR27.EvolutionSink sink;String snapshot,snapshotHash;EvolutionDatasetPinR29.Prepared pin;int stepBefore,genBefore;boolean batchPrepared,failed,recoveryRebound;
            CooperativeContinuationR26<Turn> work;
            Turn(ResearchPoolR27.Worker w){
                prior=evo;cache=evoCache;rebind=prior!=null&&prior.step==1?pendingCache:null;data=rebind==null?evoData:pendingData;
                EvolutionCheckpoint old=null;EvolutionDatasetPinR29.Stored stored=null;int next=0;
                if(prior==null){old=captureEvolutionCheckpoint(w.ro);
                    if(old!=null){stored=EvolutionDatasetPinR29.read(w.ro,old.datasetHash);if(stored==null)throw new MissingEvolutionDataset(old.datasetHash);}
                    else try(Cursor c=w.ro.rawQuery("SELECT max(generation)+1 FROM r27_generation WHERE loop=?",new String[]{LOOP})){if(c.moveToFirst()&&!c.isNull(0))next=c.getInt(0);}}
                ReplayDatasetR27 newest=pendingData==null?data:pendingData;FeatureCacheR27 newestCache=pendingCache==null?cache:pendingCache;
                saved=old;pinned=stored;recoveryFutureData=old!=null&&!old.datasetHash.equals(newest.datasetHash)?newest:null;recoveryFutureCache=recoveryFutureData==null?null:newestCache;
                nextGeneration=next;weights=leafWeights();leaves=new ArrayList<>(derived);
            }
            Turn compute(){
                loop=prior;
                if(loop==null){EvolutionLoopR27.Config cfg=new EvolutionLoopR27.Config();cfg.seed=System.currentTimeMillis();
                    if(saved!=null){if(pinned!=null){data=EvolutionDatasetPinR29.decode(pinned);cache=new FeatureCacheR27(data);}loop=restoreExact(saved,cfg,cache,System::currentTimeMillis);}
                    if(loop==null){loop=new EvolutionLoopR27(cfg,cache,null,System::currentTimeMillis);loop.generation=nextGeneration;}}
                // Restore the exact old state first. Only its genuine generation boundary may
                // consume a newer prepared dataset; a mid-generation cursor stays on its pin.
                if(saved!=null&&loop.step==1&&recoveryFutureData!=null){loop.rebind(recoveryFutureCache);data=recoveryFutureData;cache=recoveryFutureCache;recoveryRebound=true;}
                if(rebind!=null)loop.rebind(rebind);
                // A turn owns one cooperative CPU context. Helper futures would escape that quantum.
                loop.executor=null;loop.serialWhile=()->true;loop.leafWeight=weights;loop.derivedLeaves=leaves;
                stepBefore=loop.step;genBefore=loop.generation;sink=new StoreR27.EvolutionSink(LOOP,data.issues[data.size()-1]);loop.setSink(sink);
                loop.step();snapshot=loop.snapshot();snapshotHash=StatsR27.sha256(snapshot);pin=EvolutionDatasetPinR29.prepare(data);return this;
            }
        }
        public String id() { return "r27-evolution"; }
        public boolean boundedSlice(){return true;}
        private String owner(){return pool.session()+":R29_EVOLUTION";}
        /** A cancelled update may have stopped halfway through a family-model row. Keep it private until its
         * thread has exited, then rebuild the caches and restore only the last committed cursor. */
        private boolean discardFailed(Turn pending){
            if(!pending.work.isDone())return false;
            synchronized(this){if(turn!=pending)return true;evo=null;evoCache=new FeatureCacheR27(evoData);if(pendingData!=null)pendingCache=new FeatureCacheR27(pendingData);turn=null;}
            evolutionAccess.publish();return true;
        }
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker w) throws Exception {
            if(stopping||w.cancelled())return ResearchPoolR27.Outcome.idle("USER_STOP",0,-1);
            Turn failed=turn;
            if(failed!=null&&failed.failed)return ResearchPoolR27.Outcome.idle(discardFailed(failed)?"COOPERATIVE_EVOLUTION_RESTORE_COMMITTED_CURSOR":"COOPERATIVE_CANCELLED_EVOLUTION_DRAIN",0,-1);
            if(deferredObservers!=null){if(!refreshObservers(w,deferredObservers))return ResearchPoolR27.Outcome.idle("COOPERATIVE_WRITER_BUSY:OBSERVERS",0,-1);deferredObservers=null;}
            if(evoCache==null)return ResearchPoolR27.Outcome.idle("DEPENDENCY(r27-replay-dataset)",0,-1);
            synchronized(this){
                if(stopping)return ResearchPoolR27.Outcome.idle("USER_STOP",0,-1);
                if(turn==null){
                    if(!evolutionAccess.begin())return ResearchPoolR27.Outcome.idle("COOPERATIVE_WAIT_FOR_EVOLUTION_READERS",0,-1);
                    try{turn=new Turn(w);evo=null;Turn pending=turn;pending.work=new CooperativeContinuationR26<>("starcore-r29-evolution",pending::compute,()->stopping,null,()->android.os.SystemClock.currentThreadTimeMillis()*1000000L,owner());}
                    catch(MissingEvolutionDataset missing){turn=null;evolutionAccess.publish();return ResearchPoolR27.Outcome.idle(missing.getMessage(),0,-1);}
                    catch(Throwable failure){turn=null;evolutionAccess.publish();throw failure;}
                }
            }
            Turn pending=turn;CooperativeContinuationR26.Slice<Turn> slice;
            try{slice=pending.work.slice(40);}catch(Exception failure){pending.failed=true;pending.work.close();discardFailed(pending);throw failure;}
            if(!slice.done)return ResearchPoolR27.Outcome.idle("COOPERATIVE_PRIVATE_EVOLUTION_TURN",0,-1);
            if(slice.error!=null){pending.failed=true;pending.work.close();discardFailed(pending);if(slice.error instanceof Exception)throw(Exception)slice.error;throw new IllegalStateException("R29_EVOLUTION_CONTINUATION",slice.error);}
            if(stopping||w.cancelled())throw new InterruptedException("R29_EVOLUTION_CANCELLED_BEFORE_COMMIT");
            EvolutionLoopR27 loop=pending.loop;ResearchWriteQueueR27.Batch batch=pending.sink.batch;
            if(!pending.batchPrepared){
            batch.add("INSERT OR REPLACE INTO r27_evolution_cursor(loop,generation,step,dataset_hash,rng_state,snapshot,updated_at) VALUES(?,?,?,?,?,?,?)",LOOP,loop.generation,loop.step,pending.data.datasetHash,loop.rng.state,pending.snapshot,System.currentTimeMillis());
            final List<Object[]> promos=pending.sink.promotions;batch.before=db->{if(stopping||closing)throw new InterruptedException("R29_EVOLUTION_STOPPED_BEFORE_TRANSACTION");for(Object[] p:promos){PolicySpecR27 spec=(PolicySpecR27)p[0];StoreR27.insertShadow(db,spec,(Double)p[1],(String)p[3],semanticAbi(spec),spec.policyHash(),spec.policyHash());}return 0;};
            EvolutionDatasetPinR29.append(batch,pending.pin);
            batch.add(EVOLUTION_CHECKSUM_DDL);
            batch.add("INSERT OR REPLACE INTO r29_evolution_checkpoint_checksum VALUES(?,?,?,?,?,?)",LOOP,pending.snapshotHash,pending.data.datasetHash,loop.generation,loop.step,loop.rng.state);
            pending.batchPrepared=true;
            }
            if(!w.writes().submit("EVOLUTION_STEP_"+EvolutionLoopR27.STEPS[pending.stepBefore],batch,()->stopping||w.cancelled()))return ResearchPoolR27.Outcome.idle("COOPERATIVE_WRITER_BUSY:EVOLUTION_TURN",0,-1);
            evoData=pending.data;evoCache=pending.rebind==null?pending.cache:pending.rebind;
            if(pending.rebind!=null&&pendingCache==pending.rebind){pendingCache=null;pendingData=null;}
            if(pending.recoveryRebound&&pendingCache==pending.recoveryFutureCache){pendingData=null;pendingCache=null;}
            if(!pending.recoveryRebound&&pending.recoveryFutureData!=null&&(pendingData==null||pendingData.ordinals[pendingData.size()-1]<pending.recoveryFutureData.ordinals[pending.recoveryFutureData.size()-1])){pendingData=pending.recoveryFutureData;pendingCache=pending.recoveryFutureCache;}
            committedControls=new Object[][]{{"CONTROL_I00",loop.i00Champion,loop.i00Tau},{"CONTROL_NULL",loop.nullEvolution.champion,loop.nullEvolution.championTau}};
            evo=loop;synchronized(this){turn=null;}evolutionAccess.publish();
            if(pending.stepBefore==9&&!refreshObservers(w,loop))deferredObservers=loop;
            if(pending.stepBefore==9&&pending.genBefore%50==49)derivedDue=true;
            return ResearchPoolR27.Outcome.progress(1,(long)loop.generation*10+loop.step);
        }
        public void close(){Turn pending;synchronized(this){stopping=true;pending=turn;if(pending!=null&&pending.work!=null)pending.work.close();}
            // The private body owns no database connection, nevertheless drain before pool.close closes RO handles.
            while(!CooperativeContinuationR26.awaitOwnerStopped(owner(),1000))android.util.Log.w("STARCORE_R27","draining cancelled evolution computation");
        }
    }
    volatile boolean derivedDue; volatile List<PolicyAstR27.Node> derived = new ArrayList<>();

    // ------------------------------------------------------------------ R27.4 observers (every strategy runs every period)
    /** Best evolved candidates (by H_SELECT score) frozen and settled every period as observers. */
    public static final int OBSERVE_CAP = 24;
    public static final String OBSERVE = "OBSERVE", OBSERVE_RETIRED = "OBSERVE_RETIRED";
    public static String observerId(String policyHash) { return StatsR27.sha256(OBSERVE + "|" + policyHash); }

    /** After each generation: the top OBSERVE_CAP population members become observers; members that dropped out are
     * retired (their settled rows stay). Observers carry no e-process row, so settlement never feeds qualification. */
    boolean refreshObservers(ResearchPoolR27.Worker w, EvolutionLoopR27 loop) throws Exception {
        List<EvolutionLoopR27.Member> ranked = new ArrayList<>(loop.population);
        ranked.sort((a, b) -> a.u != b.u ? Double.compare(b.u, a.u) : a.hash().compareTo(b.hash()));
        final Map<String, EvolutionLoopR27.Member> keep = new LinkedHashMap<>();
        for (EvolutionLoopR27.Member m : ranked) { if (keep.size() >= OBSERVE_CAP) break; if (Double.isFinite(m.tau)) keep.put(observerId(m.hash()), m); }
        final List<String> retire = new ArrayList<>(); final Map<String, EvolutionLoopR27.Member> add = new LinkedHashMap<>(keep);
        try (Cursor c = w.ro.rawQuery("SELECT policy_hash FROM r27_live_shadow WHERE status=?", new String[] {OBSERVE})) {
            while (c.moveToNext()) { String id = c.getString(0); if (add.remove(id) == null) retire.add(id); }
        }
        if (add.isEmpty() && retire.isEmpty()) return true;
        final int generation = loop.generation - 1;
        ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch(); long now = System.currentTimeMillis();
        for (String id : retire) b.add("UPDATE r27_live_shadow SET status=?,reason=?,updated_at=? WHERE policy_hash=? AND status=?", OBSERVE_RETIRED, "LEFT_TOP_" + OBSERVE_CAP + "_G" + generation, now, id, OBSERVE);
        b.before = db -> {
            long t = System.currentTimeMillis();
            for (Map.Entry<String, EvolutionLoopR27.Member> e : add.entrySet()) {
                EvolutionLoopR27.Member m = e.getValue();
                // Re-entering observers resume their own row; global_index 0 keeps them outside the α-spending sequence.
                db.execSQL("INSERT OR IGNORE INTO r27_live_shadow(policy_hash,spec_json,temperature,bias,global_index,status,reason,promoted_at,updated_at,through_ordinal,base_policy,semantic_abi) VALUES(?,?,?,?,0,?,?,?,?,-1,?,'')",
                        new Object[] {e.getKey(), m.spec.fullJson(), m.tau, StoreR27.pack(new double[10]), OBSERVE, "TOP_" + OBSERVE_CAP + "_H_SELECT_G" + generation, t, t, m.hash()});
                db.execSQL("UPDATE r27_live_shadow SET status=?,temperature=?,reason=?,updated_at=? WHERE policy_hash=? AND status=?", new Object[] {OBSERVE, m.tau, "TOP_" + OBSERVE_CAP + "_H_SELECT_G" + generation, t, e.getKey(), OBSERVE_RETIRED});
            }
            return 0;
        };
        return w.writes().submit("OBSERVERS",b,w::cancelled);
    }

    Map<String, Double> leafWeights() { synchronized (atomStates) { return AtomScoreR27.leafWeights(new HashMap<>(atomStates)); } }

    String semanticAbi(PolicySpecR27 s) { Map<String, String> abi = operatorAbi; return abi == null ? "" : SemanticAbiR27.policyAbi(s, abi); }

    // ------------------------------------------------------------------ r27-live (R27-008/032/040/041/042)
    final class LiveTask implements ResearchPoolR27.Task {
        long lastRun;
        public String id() { return "r27-live"; }
        public boolean urgent(ResearchPoolR27.Worker worker,long now){return now-lastRun>=1000&&now<latestOpen(worker.realtimeRo())+60_000L-5000;}
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker w) throws Exception {
            String latest = latestIssue(w.ro); if (latest == null) return ResearchPoolR27.Outcome.idle("WAITING_DRAWS", 0, 0);
            try {
            long latestOrd = TargetIssueResolver.sequenceOrdinalSU1(latest), deadline = latestOpen(w.ro) + 60_000L;
            w.writes().setNextDeadline(deadline);
            long units = PreparedForecastR29.recoverUnproven(w);
            units += LiveShadowsR27.settle(RuntimeR27.this, w, latestOrd);
            units += LiveShadowsR27.qualify(RuntimeR27.this, w, latestOrd);
            ensureControls(w);
            units += LiveShadowsR27.freeze(RuntimeR27.this, w, latest, latestOrd, deadline);
            units += LiveShadowsR27.freeze(RuntimeR27.this, w, latest, latestOrd, deadline, LiveShadowsR27.OBSERVE_STATES, false);
            units += LiveShadowsR27.certify(RuntimeR27.this, w, latestOrd);
            return units > 0 ? ResearchPoolR27.Outcome.progress(units, latestOrd) : ResearchPoolR27.Outcome.idle("NO_NEW_EVENT", latestOrd, latestOrd);
            } finally { lastRun=System.currentTimeMillis(); }
        }
    }

    /** Keeps one I00 and one shuffled-label control frozen every period next to the shadows (same-period negative controls). */
    void ensureControls(ResearchPoolR27.Worker w) throws Exception {
        // References are captured only after the step and its cursor were committed.
        Object[][] controls = committedControls;
        for (Object[] c : controls) {
            final PolicySpecR27 spec = (PolicySpecR27) c[1]; if (spec == null) continue; final String status = (String) c[0];
            boolean have; try (Cursor x = w.ro.rawQuery("SELECT 1 FROM r27_live_shadow s LEFT JOIN r29_registration_request r ON r.experiment_id=s.policy_hash WHERE s.status=? OR (s.status=? AND r.desired_status=?)", new String[] {status,ExperimentRegistrationR29.PENDING,status})) { have = x.moveToFirst(); }
            if (have) continue;
            final double tau = (Double) c[2]; final String id = StatsR27.sha256(status + "|" + spec.policyHash());
            ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch();
            b.before = db -> { StoreR27.insertShadow(db, spec, tau, status + ":same-period negative control", semanticAbi(spec), id, spec.policyHash());
                ExperimentRegistrationR29.desiredStatus(db,id,status); db.execSQL("UPDATE r27_eprocess SET state='CONTROL_NOT_ELIGIBLE' WHERE policy_hash=?", new Object[] {id}); return 0; };
            submit(w, "LIVE_CONTROL", b);
        }
    }

    /** Live dataset + placeholder for the next target (family streams continue from the previous cache). */
    FeatureCacheR27 liveCacheWithPlaceholder(ResearchPoolR27.Worker w, long latestOrd) {
        ReplayDatasetR27 fresh = StoreR27.loadDataset(w.ro, DATASET_ROWS);
        if(fresh.size()==0||fresh.ordinals[fresh.size()-1]!=latestOrd)throw new IssueCommitR29.Rejected("SOURCE_CHANGED_DURING_FEATURE_PREPARATION");
        // Canonical repairs/backfills must not leave the old feature prefix in memory indefinitely.
        if(liveData!=null){
            int a=0,b=0;boolean changed=false;
            while(a<liveData.size()&&b<fresh.size()){
                if(liveData.ordinals[a]<fresh.ordinals[b]){if(liveData.ordinals[a]>=fresh.ordinals[0]){changed=true;break;}a++;continue;}
                if(liveData.ordinals[a]>fresh.ordinals[b]){if(fresh.ordinals[b]<=liveData.ordinals[liveData.size()-1]){changed=true;break;}b++;continue;}
                if(!java.util.Arrays.equals(liveData.draws[a],fresh.draws[b])){changed=true;break;}a++;b++;
            }
            if(changed){liveData=null;liveCache=null;}
        }
        if (liveData == null || liveData.size() > LIVE_REBASE_ROWS || fresh.size() == 0) { liveData = fresh; liveCache = null; }
        else {
            long last = liveData.ordinals[liveData.size() - 1];
            for (int i = 0; i < fresh.size(); i++) if (fresh.ordinals[i] > last) liveData = liveData.append(new ReplayDatasetR27.Row(fresh.issues[i], fresh.ordinals[i], fresh.draws[i]));
        }
        ReplayDatasetR27 withSlot = liveData.append(new ReplayDatasetR27.Row("PLACEHOLDER", latestOrd + 1, new int[] {1, 2, 3, 4, 5, 6, 7, 8, 9, 10}));
        FeatureCacheR27 c = new FeatureCacheR27(withSlot, 64);
        if (liveCache != null) { try { c.extendFrom(liveCache); } catch (RuntimeException rebuild) { /* streams rebuilt lazily */ } }
        liveCache = c;
        return c;
    }

    // ------------------------------------------------------------------ r27-abi (R27-010/011/012)
    final class AbiTask implements ResearchPoolR27.Task {
        public String id() { return "r27-abi"; }
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker w) throws Exception {
            ensureGolden();
            long units = LegacyContinuityR27.step(store, w.ro, golden, operatorAbiHash, w.writes());
            units += LiveShadowsR27.semanticCheck(RuntimeR27.this, w);
            return units > 0 ? ResearchPoolR27.Outcome.progress(units, units) : ResearchPoolR27.Outcome.idle("NO_NEW_EVENT", 0, 0);
        }
    }
    void ensureGolden() throws Exception {
        if (golden != null) return;
        try (InputStream in = store.contextForR26().getAssets().open(SemanticAbiR27.GOLDEN_ASSET)) { golden = SemanticAbiR27.readGolden(in, true); }
        goldenCache = new FeatureCacheR27(golden, 64);
        operatorAbi = OperatorGoldenR27.fingerprints(goldenCache);
        operatorAbiHash = SemanticAbiR27.operatorSetHash(operatorAbi);
        store.setState("r27_operator_abi", operatorAbiHash);
    }

    // ------------------------------------------------------------------ r27-atoms (R27-043/044) and r27-atomnet (R27-033/034/036)
    final class AtomsTask implements ResearchPoolR27.Task {
        public String id() { return "r27-atoms"; }
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker w) throws Exception { try(EvolutionAccess.Lease lease=evolutionAccess.read()){if(lease==null)return ResearchPoolR27.Outcome.idle("COOPERATIVE_PRIVATE_EVOLUTION_TURN",0,-1);return AtomWorldR27.atomsStep(RuntimeR27.this, w);} }
    }
    final class AtomNetTask implements ResearchPoolR27.Task {
        public String id() { return "r27-atomnet"; }
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker w) throws Exception { if(closing)return ResearchPoolR27.Outcome.idle("USER_STOP",0,-1);try(EvolutionAccess.Lease lease=evolutionAccess.read()){if(lease==null)return ResearchPoolR27.Outcome.idle("COOPERATIVE_PRIVATE_EVOLUTION_TURN",0,-1);return AtomWorldR27.atomNetStep(RuntimeR27.this, w);} }
    }

    // ------------------------------------------------------------------ r27-calibration (R27-037/038)
    final class CalibrationTask implements ResearchPoolR27.Task {
        public String id() { return "r27-calibration"; }
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker w) throws Exception { try(EvolutionAccess.Lease lease=evolutionAccess.read()){if(lease==null)return ResearchPoolR27.Outcome.idle("COOPERATIVE_PRIVATE_EVOLUTION_TURN",0,-1);return LiveShadowsR27.calibrate(RuntimeR27.this, w);} }
    }

    // ------------------------------------------------------------------ r27-chart-cache (R27-049)
    final class ChartTask implements ResearchPoolR27.Task {
        long lastBuilt;
        public String id() { return "r27-chart-cache"; }
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker w) throws Exception {
            if (System.currentTimeMillis() - lastBuilt < 30_000) return ResearchPoolR27.Outcome.idle("NO_NEW_EVENT", 0, 0);
            ResearchWriteQueueR27.Batch b = ChartCacheR27.build(w.ro, RuntimeR27.this);
            submit(w, "CHART_CACHE", b); lastBuilt = System.currentTimeMillis();
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
        long last, lastSize; boolean assetsCached; final Map<String, Long> fetchFlushed = new HashMap<>();
        public String id() { return "r27-health"; }
        public ResearchPoolR27.Outcome step(ResearchPoolR27.Worker w) throws Exception {
            if (System.currentTimeMillis() - last < 60_000) return ResearchPoolR27.Outcome.idle("NO_NEW_EVENT", 0, 0);
            last = System.currentTimeMillis(); w.writes().flush();
            ResearchWriteQueueR27.Batch b = new ResearchWriteQueueR27.Batch();
            if (!assetsCached) {
                // R27-053/054: build identity, card registry, source audit and R26→R27 merge status packaged as assets.
                for (String name : new String[] {"build-identity", "r27_cards", "source-audit", "merge-status"}) {CooperativeContinuationR26.poll();
                    String payload;
                    try (InputStream in = store.contextForR26().getAssets().open("r27/" + name + ".json")) {
                        java.io.ByteArrayOutputStream buf = new java.io.ByteArrayOutputStream(); byte[] chunk = new byte[8192]; int k; while ((k = in.read(chunk)) > 0) {CooperativeContinuationR26.poll();buf.write(chunk, 0, k);}
                        payload = buf.toString("UTF-8"); JsonR27.parse(payload);
                    } catch (Exception missing) { Map<String, Object> m = new LinkedHashMap<>(); m.put("status", "INSUFFICIENT"); m.put("reason", "asset r27/" + name + ".json unavailable: " + missing.getClass().getSimpleName()); payload = JsonR27.write(m); }
                    b.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('asset',?,?,?)", name, payload, last);
                }
                Map<String, Object> runtime = new LinkedHashMap<>(); runtime.put("sourceCommit", BuildConfig.SOURCE_COMMIT_R26); runtime.put("versionCode", (long) BuildConfig.VERSION_CODE); runtime.put("versionName", BuildConfig.VERSION_NAME);
                b.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('asset','runtime-identity',?,?)", JsonR27.write(runtime), last);
                // R29-001/002: measured baseline identity and the preserved requirement ledger (read by ReportR29).
                b.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('r29','baseline-identity',?,?)", JsonR27.write(BaselineIdentityR29.describe(store.contextForR26())), last);
                b.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('r29','legacy-preservation',?,?)", JsonR27.write(LegacyContinuityR27.preservation(store.contextForR26())), last);
                assetsCached = true;
            }
            // R27-048: fetch write-rejection causes since process start (absolute counts per process; delta-added).
            for (Map.Entry<String, long[]> e : com.ethk3.novaonepro.FetchLedgerR925.rejectCountsR27().entrySet()) {CooperativeContinuationR26.poll();
                long prev = fetchFlushed.getOrDefault(e.getKey(), 0L), delta = e.getValue()[0] - prev;
                if (delta <= 0) continue;
                b.add("INSERT OR IGNORE INTO r27_fetch_reject(reason,count,last_issue,last_at) VALUES(?,0,NULL,?)", e.getKey(), e.getValue()[1]);
                b.add("UPDATE r27_fetch_reject SET count=count+?,last_at=? WHERE reason=?", delta, e.getValue()[1], e.getKey());
                fetchFlushed.put(e.getKey(), e.getValue()[0]);
            }
            // R27-047: database size and the largest tables (measurement every hour). Archival of old-chain tables is
            // gated on the legacy switch being off after the observation period; history stays queryable.
            if (last - lastSize > 3_600_000L) { lastSize = last; b.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('retention','db_size',?,?)", JsonR27.write(dbSize(w.ro)), last); }
            // wait_since bookkeeping for alarms is maintained by receipts; here we only stamp alarms for the report.
            List<WaitKindR27.Health> h = health(w.ro, last); int stuck = 0, never = 0;
            for (WaitKindR27.Health x : h) { if (x.stuck) stuck++; if (x.neverRun) never++; }
            Map<String, Object> m = new LinkedHashMap<>(); m.put("stuck", (long) stuck); m.put("neverRun", (long) never); m.put("checkedAt", last);
            m.put("workerLimit", (long) pool.workerLimit()); m.put("thermalReason", pool.thermalReason());
            b.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES('health','summary',?,?)", JsonR27.write(m), last);
            submit(w, "HEALTH", b);
            return ResearchPoolR27.Outcome.progress(1, last / 1000);
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
