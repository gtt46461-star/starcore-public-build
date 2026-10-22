package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * R908 true algorithm-invention laboratory.
 *
 * <p>Named algorithms are baselines only. Candidate invention starts from primitive instructions
 * and may change representation, state memory, nonlinear inference and post-settlement update
 * program. Discovery data only ranks IDEA candidates. A candidate is never "proven" until it
 * accumulates new future strict OOS after its genome was frozen.</p>
 */
public final class AlgorithmInventionLabR908 {
    public static final String OWNER = "RESEARCH_ONLY_ALGORITHM_PROGRAM_SEARCH";
    public static final boolean MAY_AUTHORIZE_REAL = false;
    private static final java.util.Map<DbStore,ObservationSurface> OBSERVATIONS =
            Collections.synchronizedMap(new java.util.WeakHashMap<DbStore,ObservationSurface>());
    private static final class ObservationSurface {
        final String identity;
        final RuntimeObservation[] actions=new RuntimeObservation[1200];
        final double[][] numbers=new double[100][];
        final java.util.Map<String,RuntimeScan> scans=new java.util.HashMap<String,RuntimeScan>();
        ObservationSurface(String value){identity=value;}
    }
    // Disposable partial arithmetic, bounded by the registered cohort. It never increments N.
    private static final java.util.Map<DbStore,java.util.LinkedHashMap<String,DevelopmentScanR964>> DEVELOPMENT_R964 =
            new java.util.WeakHashMap<DbStore,java.util.LinkedHashMap<String,DevelopmentScanR964>>();
    private static final class DevelopmentScanR964 {
        final String identity; final RuntimeObservation[] surface=new RuntimeObservation[1200];
        final double[][] features=new double[100][];
        final HistoricalStrategyInventionR908.Profile profile;
        final RuntimeScan scan;
        int featureCursor,surfaceCursor;
        DevelopmentScanR964(String identity,HistoricalStrategyInventionR908.Profile profile,AlgorithmGenomeR908 genome,AlgorithmGenomeVmR908.State state){
            this.identity=identity;this.profile=profile;this.scan=new RuntimeScan(genome,state,1200);
        }
    }
    static void releasePredrawR964(DbStore store,String target){
        synchronized(OBSERVATIONS){ObservationSurface old=OBSERVATIONS.get(store);if(old!=null&&old.identity.startsWith(target+"|"))OBSERVATIONS.remove(store);}
    }
    private static final int HISTORY_MAX = 320;
    private static final int MIN_HISTORY = 96;
    private static final int POPULATION = 28;
    private static final int KEEP = 6;
    private static final int DEV_MAX = 72;
    private static final int MIN_NEW_FUTURE_ISSUES_PER_GENERATION = 24;
    /** Hard cooperative slice: invention is always subordinate to fetch/analysis/settlement. */
    private static final long INVENTION_SLICE_MS = 2600L;

    private AlgorithmInventionLabR908() {}

    public static String maintain(DbStore store) {
        return DirectedResearchCreationR932.maintain(store,"algorithm");
    }

    static void registerDirected(SQLiteDatabase db,AlgorithmGenomeR908 genome,String until,String earliest,String intent,String reason){
        Evaluation e=new Evaluation(genome);e.mutationIntent=intent;e.creationReason=reason;
        e.behaviorHash=ExecutableIdentityR932.semantic(genome);
        insertGenome(db,e,until,earliest,System.currentTimeMillis());
    }

    /** Freezes a bounded cohort of invented algorithms after Production has already returned. */
    public static int capturePredraw(
            final DbStore store, final V51RuntimeResult runtime, int maxAlgorithms, long budgetMs) {
        if (store == null || runtime == null || !runtime.success || runtime.context == null
                || runtime.actions == null || runtime.actions.length != K3ActionSpaceEngine.TOTAL_ACTIONS) return 0;
        return captureSurface(
                store,
                runtime.context.targetIssue,
                runtime.context.baseIssue,
                runtime.context.historyHash,
                runtime.checksum,
                runtime.actions,
                maxAlgorithms,
                budgetMs);
    }

    /** Crash-durable Research replay from the immutable canonical snapshot. */
    public static int capturePredraw(
            final DbStore store,
            final DeferredEvidenceSnapshotR36.Snapshot snapshot,
            int maxAlgorithms,
            long budgetMs) {
        if (store == null || snapshot == null) return 0;
        return captureSurface(
                store,
                snapshot.targetIssue,
                snapshot.baseIssue,
                snapshot.historyHash,
                snapshot.recordHash,
                snapshot.positionActions(),
                maxAlgorithms,
                budgetMs);
    }

    private static int captureSurface(
            final DbStore store,
            final String targetIssue,
            final String baseIssue,
            final String historyHash,
            final String surfaceHash,
            final V51Models.PositionAction[] actions,
            int maxAlgorithms,
            long budgetMs) {
        final String target = safe(targetIssue);
        final String base = safe(baseIssue);
        if (store == null
                || !TargetIssueResolver.exact(base, target).valid
                || safe(historyHash).length() != 64
                || safe(surfaceHash).length() != 64
                || actions == null
                || actions.length != K3ActionSpaceEngine.TOTAL_ACTIONS)
            return captureZero(store, target, "INPUT_OR_SURFACE_INVALID");
        if (AppConfig.R36_REAL_BET_UNLOCK || MAY_AUTHORIZE_REAL)
            throw new IllegalStateException("R908_ALGORITHM_REAL_BOUNDARY");
        if (store.getDrawByIssue(target) != null)
            return captureZero(store, target, "TARGET_ALREADY_DRAWN");
        long deadline = TimeSourceR811.monoNowMs() + Math.max(1L, Math.min(GlobalResearchScheduleR932.SLICE_MS, budgetMs));
        GlobalResearchScheduleR932.prepareConsumer(store,target,surfaceHash);
        List<GenomeRow> rows = runnableGenomes(store, target, Math.max(1, Math.min(96, maxAlgorithms)));
        if (rows.isEmpty()) {
            int existing = predrawCountForTarget(store, target);
            if (existing > 0) return captureExisting(store, target, existing);
            return captureZero(store, target, "NO_RUNNABLE_GENOME");
        }
        CaptureOpportunityR925.eligible(store,target);
        java.util.ArrayList<String> dispatchedKeys=new java.util.ArrayList<String>();
        for(GenomeRow row:rows)dispatchedKeys.add("algorithm:"+row.genome.genomeId);
        ResearchCaptureLedgerR932.markDispatched(store,target,dispatchedKeys,"ALGORITHM_R908");
        // The frozen roster already owns eligibility/selection. Do not rewrite every
        // pending candidate on every 650ms retry before any VM can start executing.
        HistoricalStrategyInventionR908.Profile profile =
                GlobalResearchScheduleR932.profile(store, base);
        if (profile == null || !profile.ready)
            return captureZero(store, target, "PIT_PROFILE_NOT_READY");
        RuntimeObservation[] observations = runtimeObservations(store,target,base,historyHash,surfaceHash,profile,actions,deadline);
        if (observations == null || observations.length != actions.length)
            return captureZero(store, target, "OBSERVATION_SURFACE_BUDGET_YIELD");
        int frozen = 0;
        for (GenomeRow row : rows) {
            ResearchWorkBudgetR927.checkpoint();
            if (TimeSourceR811.monoNowMs() >= deadline || RealtimePressureGateR811.hasActiveCriticalWork()) break;
            long candidateStartedR925 = System.currentTimeMillis();
            ResearchCaptureLedgerR932.record(store,target,"algorithm:"+row.genome.genomeId,"EXECUTION_STARTED","FULL1200",0);
            boolean proxy=GlobalResearchScheduleR932.proxy(store,target,"algorithm:"+row.genome.genomeId);
            RuntimeObservation[] use=observations;
            if(proxy){use=new RuntimeObservation[120];for(int i=0;i<120;i++)use[i]=observations[i*10];}
            ObservationSurface shared=OBSERVATIONS.get(store);
            String scanKey=row.genome.genomeId+'|'+use.length;
            RuntimeScan scan=shared.scans.get(scanKey);
            if(scan==null||!scan.matches(row.genome,row.state)) {
                scan=new RuntimeScan(row.genome,row.state,use.length);
                shared.scans.put(scanKey,scan);
            }
            Selection s = selectRuntime(row.genome, row.state, use, deadline,scan);
            if(proxy){if(s!=null)GlobalResearchScheduleR932.saveProxy(store,target,"algorithm:"+row.genome.genomeId,surfaceHash,s.ordinal,s.score);continue;}
            boolean capturedR925 = s != null && freezeOne(store, base, target, surfaceHash, row, s);
            if (capturedR925) {frozen++;shared.scans.remove(scanKey);}
            CaptureOpportunityR925.record(store,target,row.genome.genomeId,
                    capturedR925 ? "FROZEN" : (TimeSourceR811.monoNowMs()>=deadline || RealtimePressureGateR811.hasActiveCriticalWork() ? "SYSTEM_BUDGET_YIELD" : s!=null ? "SYSTEM_FREEZE_REJECTED" : "EXECUTION_NO_OUTPUT"),
                    Math.max(0L,System.currentTimeMillis()-candidateStartedR925),
                    s==null ? "SELECTION_UNAVAILABLE" : capturedR925 ? "" : "FREEZE_REJECTED");
        }
        int total = predrawCountForTarget(store, target);
        if (frozen == 0 && total > 0) return captureExisting(store, target, total);
        store.setState("r908_algorithm_predraw_target", target);
        store.setState("r908_algorithm_predraw_frozen", String.valueOf(total));
        store.setState("r908_algorithm_predraw_at", String.valueOf(System.currentTimeMillis()));
        store.setState("r919_algorithm_capture_status",
                frozen > 0 ? "FROZEN_NEW=" + frozen + "/TOTAL=" + total + "/RUNNABLE=" + rows.size()
                        : "ZERO:GENOME_SELECTION_OR_BUDGET_YIELD/RUNNABLE=" + rows.size());
        return total;
    }

    private static int captureExisting(DbStore store, String target, int existing) {
        if (store != null) try {
            store.setState("r919_algorithm_capture_status",
                    "ALREADY_COMPLETE_FROZEN=" + Math.max(0, existing));
            store.setState("r908_algorithm_predraw_target", safe(target));
            store.setState("r908_algorithm_predraw_frozen", String.valueOf(Math.max(0, existing)));
            store.setState("r908_algorithm_predraw_at", String.valueOf(System.currentTimeMillis()));
        } catch (Throwable ignored) {}
        return Math.max(0, existing);
    }

    private static int predrawCountForTarget(DbStore store, String target) {
        Cursor c = null;
        try {
            c = store.readable().rawQuery(
                    "SELECT COUNT(1) FROM " + DbHelper.TABLE_R908_ALGORITHM_PREDRAW
                            + " a WHERE target_issue=? AND " + AlgorithmProofRecoveryR948.eligible("a.genome_id","a.target_issue","a.record_hash"), new String[]{safe(target)});
            return c.moveToFirst() ? Math.max(0, c.getInt(0)) : 0;
        } catch (Throwable ignored) { return 0; }
        finally { if (c != null) c.close(); }
    }

    private static int captureZero(DbStore store, String target, String reason) {
        if (store != null) try {
            store.setState("r919_algorithm_capture_status", "ZERO:" + safe(reason));
            store.setState("r908_algorithm_predraw_target", safe(target));
            store.setState("r908_algorithm_predraw_frozen", "0");
            store.setState("r908_algorithm_predraw_at", String.valueOf(System.currentTimeMillis()));
        } catch (Throwable ignored) {}
        return 0;
    }

    /** Updates invented algorithm memory only after exact draw, from the feature vector frozen pre-draw. */
    public static int settleDraw(final DbStore store, final Models.Draw draw) {
        if (store == null || draw == null || !draw.isValid()) return 0;
        final int[] headUpdatesR79={0};
        Integer changed = store.coordinatorR36().runWrite(
                "R908_ALGORITHM_SETTLE_UPDATE",
                new DatabaseCoordinator.WriteCallable<Integer>() {
                    public Integer call(SQLiteDatabase db) {
                        Cursor c = null; int n = 0;
                        try {
                            c = db.rawQuery(
                                    "SELECT p.genome_id,p.position,p.n1,p.n2,p.n3,p.feature_blob,p.record_hash,g.genome_spec,g.state_blob,g.state_updates,g.future_n,g.future_hit,p.feature_hash,g.trained_until_issue,e.code_hash "
                                            + "FROM " + DbHelper.TABLE_R908_ALGORITHM_PREDRAW + " p JOIN "
                                            + DbHelper.TABLE_R908_ALGORITHM_GENOME + " g ON g.genome_id=p.genome_id LEFT JOIN "
                                            + CandidateExecutionAuditR932.TABLE + " e ON e.candidate_key=('algorithm:'||p.genome_id)"
                                            + " AND e.target_issue=p.target_issue AND e.prediction_hash=p.record_hash "
                                            + "WHERE p.target_issue=? AND p.hit_label=-1 ORDER BY p.genome_id",
                                    new String[]{draw.issue});
                            while (c.moveToNext()) {
                                ResearchWorkBudgetR927.checkpoint();
                                boolean owns=!db.inTransaction();if(owns)db.beginTransaction();
                                try (Cursor row=db.rawQuery("SELECT p.genome_id,p.position,p.n1,p.n2,p.n3,p.feature_blob,p.record_hash,g.genome_spec,g.state_blob,g.state_updates,g.future_n,g.future_hit,p.feature_hash,g.trained_until_issue,e.code_hash FROM "+DbHelper.TABLE_R908_ALGORITHM_PREDRAW+" p JOIN "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" g ON g.genome_id=p.genome_id LEFT JOIN "+CandidateExecutionAuditR932.TABLE+" e ON e.candidate_key=('algorithm:'||p.genome_id) AND e.target_issue=p.target_issue AND e.prediction_hash=p.record_hash WHERE p.genome_id=? AND p.target_issue=? AND p.hit_label=-1",new String[]{c.getString(0),draw.issue})) {
                                if(!row.moveToFirst())continue;
                                String id = row.getString(0); int pos = row.getInt(1);
                                boolean historicalOnlyR79=AlgorithmProofRecoveryR948.historicalOutcomeOnlyR79(db,id,draw.issue);
                                LegacySettlementProofR79.Original original=LegacySettlementProofR79.original(db,"algorithm:"+id,draw.issue);
                                String proofReason = LegacySettlementProofR79.verifyAlgorithmSettlement(db,id,draw,original);
                                if ((!LegacySettlementProofR79.currentAlgorithmEligible(db,id,draw.issue)&&!historicalOnlyR79) || !"VALID".equals(proofReason)) {
                                    LegacySettlementProofR79.record(db,original,draw,"REJECTED",
                                            "VALID".equals(proofReason)?"CURRENT_HEAD_NOT_QUALIFIED":proofReason);
                                    AlgorithmProofRecoveryR948.quarantine(db,id,draw.issue,"SETTLEMENT_REJECTED:"+proofReason);
                                    if(owns)db.setTransactionSuccessful();
                                    continue;
                                }
                                int n1 = row.getInt(2), n2 = row.getInt(3), n3 = row.getInt(4);
                                if(!V51ChecksumEngine.sha256("R908_ALG_FEATURE|"+row.getString(5)).equals(row.getString(12))) {
                                    AlgorithmProofRecoveryR948.quarantine(db,id,draw.issue,"SETTLEMENT_FROZEN_FEATURE_HASH_MISMATCH");
                                    if(owns)db.setTransactionSuccessful();
                                    continue;
                                }
                                if(historicalOnlyR79) {
                                    int actual=draw.getNumberAtPosition(pos),hit=actual==n1||actual==n2||actual==n3?1:0;
                                    String outcome=V51ChecksumEngine.sha256("R908_ALG_OUTCOME|"+row.getString(6)+'|'+actual+'|'+hit);
                                    ContentValues label=new ContentValues();label.put("actual_number",actual);label.put("hit_label",hit);
                                    label.put("settled_at",System.currentTimeMillis());label.put("outcome_hash",outcome);
                                    int labeled=db.update(DbHelper.TABLE_R908_ALGORITHM_PREDRAW,label,
                                            "genome_id=? AND target_issue=? AND hit_label=-1",new String[]{id,draw.issue});
                                    // Outcome belongs to the original prediction, never the new
                                    // head's weights, Future count, pruning stream or certificate.
                                    if(labeled==1)LegacySettlementProofR79.record(db,original,draw,"HISTORICAL_OUTCOME_ONLY","VALID");
                                    if(owns)db.setTransactionSuccessful();n+=labeled;continue;
                                }
                                // A previously frozen prediction still has an exact outcome when
                                // a later label has already updated the mutable head. Settle it
                                // once, without replaying an older update into that newer state.
                                boolean advancesHead=safe(row.getString(13)).compareTo(draw.issue)<0;
                                double[] x = decodeFeatures(row.getString(5)); String predrawHash = row.getString(6);
                                AlgorithmGenomeR908 g; try { g = AlgorithmGenomeR908.parse(row.getString(7)); }
                                catch (Throwable invalid) { continue; }
                                AlgorithmGenomeVmR908.State state = AlgorithmGenomeVmR908.State.decode(row.getString(8));
                                int actual = draw.getNumberAtPosition(pos);
                                int hit = actual == n1 || actual == n2 || actual == n3 ? 1 : 0;
                                if (x.length < AlgorithmGenomeR908.INPUTS) continue;
                                x[9] = hit;
                                x[12] = (actual - 1) / 9.0d;
                                x[13] = hit - value(x, 11);
                                if(advancesHead)AlgorithmGenomeVmR908.update(g, state, pos, x);
                                long at = System.currentTimeMillis();
                                String outcome = V51ChecksumEngine.sha256("R908_ALG_OUTCOME|" + predrawHash + '|' + actual + '|' + hit);
                                ContentValues pv = new ContentValues(); pv.put("actual_number", actual); pv.put("hit_label", hit);
                                pv.put("settled_at", at); pv.put("outcome_hash", outcome);
                                int u = db.update(DbHelper.TABLE_R908_ALGORITHM_PREDRAW, pv,
                                        "genome_id=? AND target_issue=? AND hit_label=-1", new String[]{id, draw.issue});
                                if (u != 1) continue;
                                int futureN = row.getInt(10) + 1;
                                int futureHit = row.getInt(11) + hit;
                                double futureRate = futureN <= 0 ? 0.0d : ((double) futureHit / (double) futureN);
                                double futureWilson = wilsonLower(futureHit, futureN);
                                ContentValues gv = new ContentValues();
                                if(advancesHead) {
                                    gv.put("state_blob", state.encode());gv.put("state_updates", state.updates);
                                    gv.put("trained_until_issue", draw.issue);gv.put("earliest_use_issue", nextIssue(draw.issue));
                                }
                                gv.put("updated_at", at);
                                gv.put("future_n", futureN); gv.put("future_hit", futureHit);
                                gv.put("future_hit_rate", futureRate); gv.put("future_wilson_lower", futureWilson);
                                // MAX also preserves the watermark when repairing an older label.
                                gv.put("future_evidence_hash", V51ChecksumEngine.sha256(
                                        "R908_FUTURE|" + id + '|' + futureN + '|' + futureHit + '|' + draw.issue + '|' + outcome));
                                if(db.update(DbHelper.TABLE_R908_ALGORITHM_GENOME, gv, "genome_id=?", new String[]{id})!=1)throw new IllegalStateException("R943_ALGORITHM_STATE_UPDATE_MISSING");
                                db.execSQL("UPDATE "+DbHelper.TABLE_R908_ALGORITHM_GENOME
                                        +" SET future_last_issue=MAX(COALESCE(future_last_issue,''),?) WHERE genome_id=?",new Object[]{draw.issue,id});
                                if(!advancesHead)db.execSQL("INSERT OR REPLACE INTO "+DbHelper.TABLE_APP_STATE
                                        +"(state_key,state_value,updated_at) VALUES('r72_algorithm_late_label',?,?)",
                                        new Object[]{id+'|'+draw.issue+"|OUTCOME_ONLY_HEAD_PRESERVED",at});
                                ResearchPruningR965.observe(db,id,draw.issue,hit,predrawHash,row.getString(14));
                                LegacySettlementProofR79.record(db,original,draw,advancesHead?"UPDATED_HEAD":"LATE_OUTCOME_ONLY","VALID");
                                if(owns)db.setTransactionSuccessful();n++;headUpdatesR79[0]++;
                                } finally {if(owns)db.endTransaction();}
                            }
                        } finally { if (c != null) c.close(); }
                        return Integer.valueOf(n);
                    }
                }, Integer.valueOf(0));
        int n = changed == null ? 0 : changed.intValue();
        if (n > 0) {
            store.setState("r908_algorithm_settlement_issue", draw.issue);
            store.setState("r908_algorithm_settlement_updates", String.valueOf(n));
            if(headUpdatesR79[0]>0)try { AlgorithmEvolutionLifecycleR912.maintain(store); } catch (Throwable ignoreLifecycle) {}
            store.setState("r908_algorithm_future_summary", futureSummary(store, 6));
            store.setState("r908_algorithm_future_summary_at", String.valueOf(System.currentTimeMillis()));
        }
        return n;
    }

    /** Same full1200 inference and update program used by live capture; DEV never mints Future. */
    static DevelopmentStep trainDevelopmentStep(AlgorithmGenomeR908 genome,
            AlgorithmGenomeVmR908.State state, List<Models.Draw> past, Models.Draw label, long deadline) {
        return trainDevelopmentStep(null,genome,state,past,label,deadline);
    }
    static DevelopmentStep trainDevelopmentStep(DbStore owner,AlgorithmGenomeR908 genome,
            AlgorithmGenomeVmR908.State state,List<Models.Draw> past,Models.Draw label,long deadline) {
        if (label == null || !label.isValid() || past == null || past.size() < 48)
            throw new IllegalArgumentException("R959_DEV_HISTORY_REQUIRED");
        String previous = "";
        StringBuilder identityInput=new StringBuilder(genome.canonical()).append('|').append(state.encode()).append('|').append(label.issue);
        for (Models.Draw draw : past) {
            if (!draw.isValid() || draw.issue.compareTo(previous) <= 0 || draw.issue.compareTo(label.issue) >= 0)
                throw new IllegalArgumentException("R959_DEV_PIT_OR_DUPLICATE");
            previous = draw.issue;
            identityInput.append('|').append(draw.issue);for(int value:draw.numbers)identityInput.append(':').append(value);
        }
        String identity=V51ChecksumEngine.sha256(identityInput.toString());
        java.util.LinkedHashMap<String,DevelopmentScanR964> cache=null;DevelopmentScanR964 pending=null;
        if(owner!=null)synchronized(DEVELOPMENT_R964){
            cache=DEVELOPMENT_R964.get(owner);
            if(cache==null){cache=new java.util.LinkedHashMap<String,DevelopmentScanR964>(32,.75f,true);DEVELOPMENT_R964.put(owner,cache);}
            pending=cache.get(genome.genomeId);
            if(pending!=null&&!identity.equals(pending.identity)){cache.remove(genome.genomeId);pending=null;}
        }
        if(pending==null){
        HistoricalStrategyInventionR908.Profile profile = HistoricalStrategyInventionR908.Profile.from(past.subList(Math.max(0,past.size()-HistoricalStrategyInventionR908.HISTORY_MAX),past.size()));
        if (!profile.ready) throw new IllegalStateException("R959_DEV_PROFILE_NOT_READY");
        // Publish disposable arithmetic BEFORE the first feature checkpoint. Previously
        // a yield in the 100-feature loop discarded every feature and restarted forever.
        pending=new DevelopmentScanR964(identity,profile,genome,state);
        if(cache!=null)synchronized(DEVELOPMENT_R964){
            cache.put(genome.genomeId,pending);
            while(cache.size()>64)cache.remove(cache.keySet().iterator().next());
        }
        }
        for(int i=pending.featureCursor;i<100;i++) {
            ResearchWorkBudgetR927.checkpoint();
            if(TimeSourceR811.monoNowMs()>=deadline)throw new ResearchWorkBudgetR927.Yield("DEV_FEATURES_PENDING|features="+pending.featureCursor+"/100");
            pending.features[i]=inputs(pending.profile,i/10+1,i%10+1);
            pending.featureCursor=i+1;
        }
        int ordinal=0;
        for(int pos=1;pos<=10;pos++) for(int a=1;a<=8;a++) for(int b=a+1;b<=9;b++) for(int c=b+1;c<=10;c++,ordinal++) {
            if(ordinal<pending.surfaceCursor)continue;
            if((ordinal&31)==0){ResearchWorkBudgetR927.checkpoint();if(TimeSourceR811.monoNowMs()>=deadline)throw new ResearchWorkBudgetR927.Yield("DEV_SURFACE_PENDING|actions="+pending.surfaceCursor+"/1200");}
            double[] x=pending.features[(pos-1)*10+a-1],y=pending.features[(pos-1)*10+b-1],z=pending.features[(pos-1)*10+c-1];
            V51Models.PositionAction action=new V51Models.PositionAction(pos,a,b,c,0,0,0,0,0,0,0,null,"",0,"");
            pending.surface[ordinal]=new RuntimeObservation(action,average(x,y,z),Math.max(Math.max(x[2],y[2]),z[2])-Math.min(Math.min(x[2],y[2]),z[2]),ordinal);
            pending.surfaceCursor=ordinal+1;
        }
        Selection selected;
        try{selected=selectRuntime(genome,state,pending.surface,deadline,pending.scan);}
        catch(ResearchWorkBudgetR927.Yield yield){throw new ResearchWorkBudgetR927.Yield("DEV_FULL1200_PENDING|actions="+pending.scan.cursor+"/1200|"+yield.getMessage());}
        if(selected==null) throw new ResearchWorkBudgetR927.Yield("DEV_FULL1200_PENDING|actions="+pending.scan.cursor+"/1200");
        if(cache!=null)synchronized(DEVELOPMENT_R964){cache.remove(genome.genomeId);}
        int actual=label.getNumberAtPosition(selected.position);
        int hit=actual==selected.n1||actual==selected.n2||actual==selected.n3?1:0;
        double[] update=selected.feature.clone(); update[9]=hit; update[12]=(actual-1)/9.0d; update[13]=hit-selected.score;
        AlgorithmGenomeVmR908.update(genome,state,selected.position,update);
        return new DevelopmentStep(hit,selected.ordinal,selected.score);
    }
    static final class DevelopmentStep {
        final int hit, ordinal; final double score;
        DevelopmentStep(int h,int o,double s){hit=h;ordinal=o;score=s;}
    }
    static void registerTrained(SQLiteDatabase db, AlgorithmGenomeR908 genome, String until,
            String earliest, int n, int hits, int maxMiss, double blockMin, String trialHash) {
        Evaluation e=new Evaluation(genome); e.n=n; e.hit=hits; e.hitRate=n==0?0:(double)hits/n;
        e.wilsonLower=wilsonLower(hits,n); e.blockMin=blockMin; e.maxMiss=maxMiss;
        e.mutationIntent="PARAMETER_VARIANT"; e.creationReason="CHRONOLOGICAL_DEV_TRAINED|trial="+trialHash+"|futureN=0";
        e.behaviorHash=ExecutableIdentityR932.semantic(genome);
        insertGenome(db,e,until,earliest,System.currentTimeMillis());
    }

    private static Selection selectRuntime(
            AlgorithmGenomeR908 g,
            AlgorithmGenomeVmR908.State state,
            RuntimeObservation[] observations,
            long deadline) {
        return selectRuntime(g,state,observations,deadline,new RuntimeScan(g,state,observations.length));
    }

    /** Partial arithmetic is disposable, never evidence. It is bound to this exact surface,
     * program and learner state; only the completed full-group proof may be committed. */
    private static final class RuntimeScan {
        final String programBefore,stateBefore;
        final AlgorithmGenomeR908 replayGenome;
        final AlgorithmGenomeVmR908.State replayState;
        final CandidateExecutionAuditR932.Builder audit;
        final AlgorithmGenomeVmR908.Scorer scorer,replayScorer;
        int cursor;Selection best;
        RuntimeScan(AlgorithmGenomeR908 g,AlgorithmGenomeVmR908.State s,int count){
            programBefore=g.canonical();stateBefore=s.encode();
            replayGenome=AlgorithmGenomeR908.parse(programBefore);replayState=AlgorithmGenomeVmR908.State.decode(stateBefore);
            scorer=new AlgorithmGenomeVmR908.Scorer(g,s);replayScorer=new AlgorithmGenomeVmR908.Scorer(replayGenome,replayState);
            audit=count==1200?new CandidateExecutionAuditR932.Builder():null;
        }
        boolean matches(AlgorithmGenomeR908 g,AlgorithmGenomeVmR908.State s){return programBefore.equals(g.canonical())&&stateBefore.equals(s.encode());}
    }
    private static Selection selectRuntime(AlgorithmGenomeR908 g,AlgorithmGenomeVmR908.State state,
            RuntimeObservation[] observations,long deadline,RuntimeScan scan) {
        for (int i = scan.cursor; i < observations.length; i++) {
            ResearchWorkBudgetR927.checkpoint();
            if ((i & 127) == 0 && (TimeSourceR811.monoNowMs() >= deadline
                    || RealtimePressureGateR811.hasActiveCriticalWork())) return null;
            RuntimeObservation observation = observations[i];
            V51Models.PositionAction a = observation.action;
            double[] f = observation.feature;
            double score = scan.scorer.score(a.position, f);
            double margin = score - 0.10d * observation.localSpread;
            if(scan.audit!=null) scan.audit.add(observation.ordinal,margin,
                    scan.replayScorer.score(a.position,f)-0.10d*observation.localSpread,
                    observation.ordinal);
            int ordinal = observation.ordinal;
            if(scan.best==null||margin>scan.best.margin||(margin==scan.best.margin&&ordinal<scan.best.ordinal)){
                double[] selected=f.clone();selected[10]=margin;selected[11]=score;
                scan.best=new Selection(a.position,a.n1,a.n2,a.n3,ordinal,score,margin,selected);
            }
            scan.cursor=i+1;
        }
        if(scan.best!=null&&scan.audit!=null&&scan.best.audit==null)scan.best.audit=scan.audit.finish(scan.best.ordinal,
                scan.programBefore,g.canonical(),scan.stateBefore,state.encode(),scan.replayState.encode());
        return scan.best;
    }

    private static RuntimeObservation[] runtimeObservations(DbStore store,String target,String base,String historyHash,String surfaceHash,
            HistoricalStrategyInventionR908.Profile profile,V51Models.PositionAction[] actions,long deadline) {
        String identity=target+'|'+base+'|'+historyHash+'|'+surfaceHash;
        ObservationSurface cached;
        synchronized(OBSERVATIONS){cached=OBSERVATIONS.get(store);if(cached==null||!cached.identity.equals(identity)){cached=new ObservationSurface(identity);OBSERVATIONS.put(store,cached);}}
        // Only 100 distinct position/number feature vectors feed 1200 K3 combinations.
        // A yielded slice retains completed immutable inputs, never a partial prediction.
        for(int i=0;i<100;i++) {
            ResearchWorkBudgetR927.checkpoint();
            if(cached.numbers[i]==null)cached.numbers[i]=inputs(profile,i/10+1,i%10+1);
        }
        for (int i=0;i<actions.length;i++) {
            ResearchWorkBudgetR927.checkpoint();
            if ((i&63)==0&&(TimeSourceR811.monoNowMs()>=deadline||RealtimePressureGateR811.hasActiveCriticalWork()))return null;
            if(cached.actions[i]!=null)continue;
            V51Models.PositionAction a=actions[i];if(a==null)return null;
            double[] x1=cached.numbers[(a.position-1)*10+a.n1-1],x2=cached.numbers[(a.position-1)*10+a.n2-1],x3=cached.numbers[(a.position-1)*10+a.n3-1];
            double spread=Math.max(Math.max(x1[2],x2[2]),x3[2])-Math.min(Math.min(x1[2],x2[2]),x3[2]);
            cached.actions[i]=new RuntimeObservation(a,average(x1,x2,x3),spread,K3ActionSpaceEngine.globalOrdinal(a.position,a.n1,a.n2,a.n3));
        }
        return cached.actions;
    }

    private static double[] inputs(HistoricalStrategyInventionR908.Profile p, int position, int number) {
        double[] x = new double[AlgorithmGenomeR908.INPUTS];
        x[0] = p.atom("position_transition_lift", position, number);
        x[1] = p.atom("cross_position_migration", position, number);
        x[2] = p.atom("short_long_residual", position, number);
        x[3] = p.atom("motif_similarity", position, number);
        x[4] = p.atom("gap_pressure", position, number);
        x[5] = p.atom("frequency_slope", position, number);
        x[6] = p.regime[Math.max(0, Math.min(9, position - 1))];
        x[7] = (position - 1) / 9.0d;
        x[8] = (number - 1) / 9.0d;
        return x;
    }

    private static boolean freezeOne(
            final DbStore store, final String base, final String target, final String surfaceHash,
            final GenomeRow row, final Selection s) {
        final long at = System.currentTimeMillis();
        final String feature = encodeFeatures(s.feature); final String featureHash = V51ChecksumEngine.sha256("R908_ALG_FEATURE|" + feature);
        final String record = V51ChecksumEngine.sha256("R908_ALG_PREDRAW|" + row.genome.genomeId + '|' + target + '|'
            + s.position + '|' + s.n1 + '|' + s.n2 + '|' + s.n3 + '|' + s.ordinal + '|' + s.score + '|' + featureHash
                + '|' + row.genome.structuralHash + '|' + row.state.encode() + '|' + surfaceHash);
        Boolean ok = store.coordinatorR36().runAtomicWrite(
                "R908_ALGORITHM_PREDRAW_FREEZE", new DatabaseCoordinator.WriteCallable<Boolean>() {
                    public Boolean call(SQLiteDatabase db) {
                        if (drawExists(db, target)) return Boolean.FALSE;
                        AlgorithmProofRecoveryR948.requireCleanForFreeze(db, row.genome.genomeId);
                        ResearchPruningR965.register(db,row.genome.genomeId,target);
                        // The selection was computed outside the writer lock. Refuse a stale
                        // mutable HEAD instead of freezing a score against newer learner state.
                        try (Cursor head = db.rawQuery("SELECT structural_hash,state_blob,trained_until_issue FROM "
                                + DbHelper.TABLE_R908_ALGORITHM_GENOME + " WHERE genome_id=?",
                                new String[]{row.genome.genomeId})) {
                            if (!head.moveToFirst()
                                    || !row.genome.structuralHash.equals(head.getString(0))
                                    || !row.state.encode().equals(AlgorithmGenomeVmR908.State.decode(head.getString(1)).encode())
                                    || !safe(row.trainedUntil).equals(safe(head.getString(2))))
                                throw new IllegalStateException("R948_ALGORITHM_HEAD_CHANGED_BEFORE_FREEZE");
                        }
                        ContentValues v = new ContentValues(); v.put("genome_id", row.genome.genomeId); v.put("target_issue", target);
                        v.put("position", s.position); v.put("n1", s.n1); v.put("n2", s.n2); v.put("n3", s.n3);
                        v.put("selected_ordinal", s.ordinal); v.put("score", s.score); v.put("feature_blob", feature);
                        v.put("feature_hash", featureHash); v.put("frozen_at", at); v.put("record_hash", record);
                        db.insertWithOnConflict(DbHelper.TABLE_R908_ALGORITHM_PREDRAW, null, v, SQLiteDatabase.CONFLICT_IGNORE);
                        ContentValues r = new ContentValues(); r.put("candidate_id", row.genome.genomeId); r.put("target_issue", target);
                        r.put("phase", "R908_INVENTED_ALGORITHM_FUTURE_OOS"); r.put("position", s.position); r.put("n1", s.n1);
                        r.put("n2", s.n2); r.put("n3", s.n3); r.put("selected_ordinal", s.ordinal); r.put("score", s.score);
                        r.put("probability", -1.0d); r.put("formula_probability", -1.0d); r.put("adaptive_probability", -1.0d);
                        r.put("adaptive_artifact_hash", ""); r.put("adaptive_trained_until", row.trainedUntil); r.put("adaptive_updates", row.state.updates);
                        r.put("adaptive_weight", 0.0d); r.put("formula_hash", row.genome.structuralHash); r.put("input_hash", featureHash);
                        r.put("frozen_at", at); r.put("record_hash", record);
                        db.insertWithOnConflict(DbHelper.TABLE_R817_RESEARCH_PREDRAW, null, r, SQLiteDatabase.CONFLICT_IGNORE);
                        // INSERT OR IGNORE alone is not an idempotency check. A conflicting
                        // mirror must roll back the complete prediction/proof/event group.
                        try (Cursor mirror = db.rawQuery("SELECT record_hash,formula_hash,input_hash,selected_ordinal FROM "
                                + DbHelper.TABLE_R817_RESEARCH_PREDRAW + " WHERE candidate_id=? AND target_issue=?",
                                new String[]{row.genome.genomeId, target})) {
                            if (!mirror.moveToFirst() || !record.equals(mirror.getString(0))
                                    || !row.genome.structuralHash.equals(mirror.getString(1))
                                    || !featureHash.equals(mirror.getString(2)) || s.ordinal != mirror.getInt(3))
                                throw new IllegalStateException("R948_ALGORITHM_MIRROR_CONFLICT");
                        }
                        Cursor c = null; try { c = db.rawQuery("SELECT record_hash FROM " + DbHelper.TABLE_R908_ALGORITHM_PREDRAW
                                + " WHERE genome_id=? AND target_issue=?", new String[]{row.genome.genomeId, target});
                            boolean matches=c.moveToFirst() && record.equals(c.getString(0));
                            if(!matches)throw new IllegalStateException("R932_ALGORITHM_FREEZE_CONFLICT");
                            if(s.audit==null)throw new IllegalStateException("R932_ALGORITHM_FULL_REPLAY_REQUIRED");
                            s.audit.freeze(db,"algorithm:"+row.genome.genomeId,row.genome.structuralHash,surfaceHash,base,target,record);
                            FixedResearchWindowR932.freezePair(db,"algorithm:"+row.genome.genomeId,row.genome.structuralHash,target,record,s.position,s.n1,s.n2,s.n3,s.score);
                            ResearchCaptureLedgerR932.record(db,target,"algorithm:"+row.genome.genomeId,"FROZEN","RAW_SCORE_NOT_CALIBRATED",record,Math.max(1L,s.audit.elapsedNs/1000000L),true);
                            db.execSQL("UPDATE v57_capture_opportunity_r925 SET status='FROZEN',reason='RAW_SCORE_NOT_CALIBRATED' WHERE target_issue=? AND genome_id=?",new Object[]{target,row.genome.genomeId});
                            return Boolean.TRUE;
                        } finally { if (c != null) c.close(); }
                    }
                }, Boolean.FALSE);
        return ok != null && ok.booleanValue();
    }

    private static void insertGenome(SQLiteDatabase db, Evaluation e, String until, String earliest, long now) {
        ContentValues v = new ContentValues(); AlgorithmGenomeR908 g = e.genome;
        ResearchGenomeContractR915 contract =
                ResearchGenomeContractR915.freshChild(g, until, earliest);
        v.put("genome_id", g.genomeId); v.put("display_name_zh", g.chineseName()); v.put("genome_spec", g.canonical());
        v.put("mechanism_spec_zh", g.mechanismDescriptionZh());
        v.put("structural_hash", g.structuralHash); v.put("behavior_hash", e.behaviorHash); v.put("generation", g.generation);
        v.put("parent_ids", g.parentIds); v.put("trained_until_issue", until); v.put("earliest_use_issue", earliest);
        v.put("mutation_intent", safe(e.mutationIntent));
        v.put("creation_reason", safe(e.creationReason));
        v.put("dev_n", e.n); v.put("dev_hit", e.hit); v.put("dev_hit_rate", e.hitRate); v.put("dev_wilson_lower", e.wilsonLower);
        v.put("dev_block_min", e.blockMin); v.put("dev_max_miss", e.maxMiss); v.put("novelty", e.novelty);
        v.put("complexity", g.complexity()); v.put("state_blob", AlgorithmGenomeVmR908.initialState(g).encode());
        v.put("state_updates", 0); v.put("status", "IDEA_WAIT_FUTURE_OOS"); v.put("may_authorize_real", 0);
        v.put("research_genome_contract", contract.canonical());
        v.put("future_fdr_q", 1.0d);
        v.put("created_at", now); v.put("updated_at", now);
        db.insertWithOnConflict(DbHelper.TABLE_R908_ALGORITHM_GENOME, null, v, SQLiteDatabase.CONFLICT_IGNORE);
        ResearchFamilyRegistryR925.register(db,g,"PREREGISTERED_BEFORE_FUTURE_OOS");
    }

    private static List<GenomeRow> runnableGenomes(DbStore store, String target, int limit) {
        ArrayList<GenomeRow> out = new ArrayList<GenomeRow>(); Cursor c = null;
        try {
            c = store.readable().rawQuery("SELECT genome_spec,state_blob,trained_until_issue,earliest_use_issue FROM "
                            + DbHelper.TABLE_R908_ALGORITHM_GENOME + " g WHERE (status IN ('IDEA_WAIT_FUTURE_OOS','OBSERVING_FUTURE_OOS','CHALLENGER_FUTURE_OOS') OR "+PairedResearchChallengeR932.referenceCondition("'algorithm:'||g.genome_id",target)+")"
                            + " AND (earliest_use_issue='' OR earliest_use_issue<=?)"
                            + " AND " + AlgorithmProofRecoveryR948.freezeReadySql("g.genome_id",target)
                            + GlobalResearchScheduleR932.predicate(target,"'algorithm:'||g.genome_id")
                            + " AND NOT EXISTS(SELECT 1 FROM " + DbHelper.TABLE_R908_ALGORITHM_PREDRAW
                            + " p WHERE p.genome_id=g.genome_id AND p.target_issue=?)"
                            + " ORDER BY COALESCE((SELECT cap.execution_started FROM " + ResearchCaptureLedgerR932.TABLE
                            + " cap WHERE cap.target_issue=? AND cap.candidate_key=('algorithm:'||g.genome_id)),0) ASC,"
                            + "COALESCE((SELECT MAX(cp.target_issue) FROM v57_algorithm_predraw_r908 cp WHERE cp.genome_id=g.genome_id),'') ASC,"
                            + "CASE status WHEN 'CHALLENGER_FUTURE_OOS' THEN 0 WHEN 'OBSERVING_FUTURE_OOS' THEN 1 ELSE 2 END,"
                            + "dev_wilson_lower DESC,generation DESC,updated_at ASC LIMIT ?",
                    new String[]{target, target, target, String.valueOf(limit)});
            while (c.moveToNext()) {
            ResearchWorkBudgetR927.checkpoint();
                try { out.add(new GenomeRow(AlgorithmGenomeR908.parse(c.getString(0)), AlgorithmGenomeVmR908.State.decode(c.getString(1)), c.getString(2))); }
                catch (Throwable ignore) {}
            }
        } finally { if (c != null) c.close(); }
        return out;
    }

    private static List<ParentSeed> loadParents(DbStore store, int limit) {
        ArrayList<ParentSeed> out = new ArrayList<ParentSeed>(); Cursor c = null;
        try {
            c = store.readable().rawQuery("SELECT genome_spec,future_n,future_wilson_lower,future_ece,"
                    + "future_max_miss,future_block_min,future_regime_min,future_coverage,"
                    + "paired_mainline_delta,complexity FROM " + DbHelper.TABLE_R908_ALGORITHM_GENOME
                    + " WHERE status NOT LIKE 'PRUNED%' AND status<>'PAUSED_CAPACITY_R912'"
                    + " ORDER BY CASE WHEN status='CHALLENGER_FUTURE_OOS' THEN 1 ELSE 0 END DESC,future_wilson_lower DESC,future_hit_rate DESC,dev_wilson_lower DESC,dev_block_min DESC,novelty DESC,updated_at DESC LIMIT ?",
                    new String[]{String.valueOf(limit)});
            while (c.moveToNext()) try {
                int futureN = c.getInt(1);
                double wilson = finiteOr(c.getDouble(2), 0.0d);
                double ece = finiteOr(c.getDouble(3), 1.0d);
                int maxMiss = c.getInt(4);
                double block = finiteOr(c.getDouble(5), 0.0d);
                double regime = finiteOr(c.getDouble(6), 0.0d);
                double coverage = finiteOr(c.getDouble(7), 0.0d);
                double paired = finiteOr(c.getDouble(8), 0.0d);
                double complexity = finiteOr(c.getDouble(9), 0.0d);
                AlgorithmGenomeR908.MutationIntent intent = failureIntent(
                        futureN, ece, maxMiss, block, regime, coverage, paired, complexity);
                String reason = "N=" + futureN + "|W=" + fmt(wilson) + "|ECE=" + fmt(ece)
                        + "|MaxMiss=" + maxMiss + "|Block=" + fmt(block)
                        + "|Regime=" + fmt(regime) + "|Coverage=" + fmt(coverage)
                        + "|Paired=" + fmt(paired) + "|Complexity=" + fmt(complexity)
                        + "|Repair=" + intent.name();
                out.add(new ParentSeed(AlgorithmGenomeR908.parse(c.getString(0)), intent, reason));
            } catch (Throwable ignore) {}
        } finally { if (c != null) c.close(); }
        return out;
    }

    private static AlgorithmGenomeR908.MutationIntent failureIntent(
            int futureN, double ece, int maxMiss, double block, double regime,
            double coverage, double paired, double complexity) {
        if (futureN < 24) return AlgorithmGenomeR908.MutationIntent.OPEN_EXPLORATION;
        if (maxMiss >= 12) return AlgorithmGenomeR908.MutationIntent.LONG_MISS_MEMORY;
        if (ece > 0.18d) return AlgorithmGenomeR908.MutationIntent.CALIBRATION_SHAPE;
        if (regime < 0.18d) return AlgorithmGenomeR908.MutationIntent.REGIME_ROUTER;
        if (block < 0.18d || coverage < 0.70d)
            return AlgorithmGenomeR908.MutationIntent.BLOCK_STABILITY;
        if (paired <= 0.0d) return AlgorithmGenomeR908.MutationIntent.MAINLINE_COMPLEMENT;
        if (complexity > 72.0d) return AlgorithmGenomeR908.MutationIntent.RESOURCE_SIMPLIFY;
        return AlgorithmGenomeR908.MutationIntent.OPEN_EXPLORATION;
    }

    private static String parentFailureSummary(List<ParentSeed> parents) {
        if (parents == null || parents.isEmpty()) return "NO_PARENT|PRIMITIVE_OPEN_SEARCH";
        StringBuilder out = new StringBuilder();
        for (ParentSeed parent : parents) {
            ResearchWorkBudgetR927.checkpoint();
            if (out.length() > 0) out.append(';');
            out.append(parent.genome.genomeId).append("=>").append(parent.failureReason);
        }
        return out.toString();
    }

    private static String creationReasonSummary(List<Evaluation> chosen) {
        StringBuilder out = new StringBuilder();
        if (chosen != null) for (Evaluation e : chosen) {
            ResearchWorkBudgetR927.checkpoint();
            if (out.length() > 0) out.append(';');
            out.append(e.genome.genomeId).append("=>").append(safe(e.mutationIntent))
                    .append('|').append(safe(e.creationReason));
        }
        return out.length() == 0 ? "NO_SURVIVOR" : out.toString();
    }

    private static Set<String> loadBehaviorHashes(DbStore store) {
        HashSet<String> out = new HashSet<String>(); Cursor c = null;
        try { c = store.readable().rawQuery("SELECT behavior_hash FROM " + DbHelper.TABLE_R908_ALGORITHM_GENOME + " WHERE behavior_hash<>''", null);
            while (c.moveToNext()) out.add(c.getString(0));
        } catch (Throwable ignore) {} finally { if (c != null) c.close(); }
        return out;
    }

    private static String futureSummary(DbStore store, int limit) {
        Cursor c = null; StringBuilder out = new StringBuilder();
        try {
            c = store.readable().rawQuery(
                    "SELECT display_name_zh,future_n,future_hit,future_hit_rate,future_wilson_lower,status,future_last_issue,"
                            + "future_brier,future_ece,future_coverage,future_block_min,future_regime_min,"
                            + "paired_mainline_n,paired_mainline_delta,lifecycle_reason "
                            + "FROM " + DbHelper.TABLE_R908_ALGORITHM_GENOME
                            + " WHERE future_n>0 ORDER BY future_wilson_lower DESC,future_n DESC,updated_at DESC LIMIT ?",
                    new String[]{String.valueOf(Math.max(1, Math.min(12, limit)))});
            while (c.moveToNext()) {
            ResearchWorkBudgetR927.checkpoint();
                if (out.length() > 0) out.append(';');
                out.append(c.getString(0)).append("/N=").append(c.getInt(1))
                        .append("/H=").append(c.getInt(2))
                        .append("/HR=").append(fmt(c.getDouble(3)))
                        .append("/W=").append(fmt(c.getDouble(4)))
                        .append('/').append(c.getString(5))
                        .append("/until=").append(c.getString(6))
                        .append("/B=").append(fmt(c.getDouble(7)))
                        .append("/ECE=").append(fmt(c.getDouble(8)))
                        .append("/COV=").append(fmt(c.getDouble(9)))
                        .append("/BLK=").append(fmt(c.getDouble(10)))
                        .append("/REG=").append(fmt(c.getDouble(11)))
                        .append("/PAIR=").append(c.getInt(12)).append(':').append(fmt(c.getDouble(13)))
                        .append("/WHY=").append(c.getString(14));
            }
        } catch (Throwable e) { return "READ_ERROR:" + e.getClass().getSimpleName(); }
        finally { if (c != null) c.close(); }
        return out.length() == 0 ? "NO_FUTURE_OOS_YET" : out.toString();
    }

    private static int algorithmSettledCount(DbStore store) {
        Cursor c = null;
        try {
            c = store.readable().rawQuery("SELECT COUNT(1) FROM " + DbHelper.TABLE_R908_ALGORITHM_PREDRAW + " p JOIN "
                    + CandidateExecutionAuditR932.TABLE + " e ON e.candidate_key=('algorithm:'||p.genome_id)"
                    + " AND e.target_issue=p.target_issue AND e.prediction_hash=p.record_hash WHERE p.hit_label IN (0,1)", null);
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally { if (c != null) c.close(); }
    }

    private static int algorithmSettledIssueCount(DbStore store) {
        Cursor c = null;
        try {
            c = store.readable().rawQuery("SELECT COUNT(DISTINCT p.target_issue) FROM "
                    + DbHelper.TABLE_R908_ALGORITHM_PREDRAW + " p JOIN "
                    + CandidateExecutionAuditR932.TABLE + " e ON e.candidate_key=('algorithm:'||p.genome_id)"
                    + " AND e.target_issue=p.target_issue AND e.prediction_hash=p.record_hash WHERE p.hit_label IN (0,1)", null);
            return c.moveToFirst() ? c.getInt(0) : 0;
        } finally { if (c != null) c.close(); }
    }

    private static int nextGeneration(DbStore store) {
        Cursor c = null; try { c = store.readable().rawQuery("SELECT COALESCE(MAX(generation),0) FROM " + DbHelper.TABLE_R908_ALGORITHM_GENOME, null);
            return c.moveToFirst() ? Math.max(1, c.getInt(0) + 1) : 1;
        } finally { if (c != null) c.close(); }
    }
    private static int activeGenomeCount(DbStore store) { Cursor c=null; try { c=store.readable().rawQuery("SELECT COUNT(1) FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE status NOT LIKE 'PRUNED%' AND status<>'PAUSED_CAPACITY_R912'",null);return c.moveToFirst()?c.getInt(0):0;} finally{if(c!=null)c.close();} }
    private static int strictOosCount(DbStore store) { Cursor c=null; try { c=store.readable().rawQuery("SELECT COUNT(1) FROM "+DbHelper.TABLE_R35_OOS_LABEL+" WHERE strict_oos=1",null);return c.moveToFirst()?c.getInt(0):0;} finally{if(c!=null)c.close();} }
    private static boolean drawExists(SQLiteDatabase db,String target){Cursor c=null;try{c=db.rawQuery("SELECT 1 FROM "+DbHelper.TABLE_DRAWS+" WHERE issue=? LIMIT 1",new String[]{target});return c.moveToFirst();}finally{if(c!=null)c.close();}}

    private static List<Models.Draw> history(DbStore store, int max) {
        ArrayList<Models.Draw> out = new ArrayList<Models.Draw>(); List<Models.Draw> src = store.getRecentDraws(max);
        if (src != null) for (Models.Draw d : src) if (d != null && d.isValid() && TargetIssueResolver.isValid(d.issue)) out.add(d);
        Collections.sort(out, new Comparator<Models.Draw>() { public int compare(Models.Draw a, Models.Draw b) { return a.issue.compareTo(b.issue); } });
        ArrayList<Models.Draw> dedup = new ArrayList<Models.Draw>(); String last = "";
        for (Models.Draw d : out) if (!d.issue.equals(last)) {
            ResearchWorkBudgetR927.checkpoint(); dedup.add(d); last = d.issue; }
        return dedup;
    }

    private static double[] average(double[] a, double[] b, double[] c) {
        double[] out = new double[AlgorithmGenomeR908.INPUTS];
        for (int i = 0; i < out.length; i++) out[i] = (value(a,i)+value(b,i)+value(c,i))/3.0d;
        return out;
    }
    private static int[] top3(double[] x) { int[] idx={1,2,3,4,5,6,7,8,9,10}; for(int i=0;i<3;i++){
            ResearchWorkBudgetR927.checkpoint();int best=i;for(int j=i+1;j<10;j++)if(x[idx[j]-1]>x[idx[best]-1])best=j;int t=idx[i];idx[i]=idx[best];idx[best]=t;}return new int[]{idx[0],idx[1],idx[2]}; }
    private static void sort3(int[] x){for(int i=0;i<2;i++)for(int j=i+1;j<3;j++)if(x[j]<x[i]){
            ResearchWorkBudgetR927.checkpoint();int t=x[i];x[i]=x[j];x[j]=t;}}
    private static double value(double[] x,int i){return x!=null&&i>=0&&i<x.length?x[i]:0.0d;}
    private static double wilsonLower(int hit,int n){if(n<=0)return 0;double z=1.959963984540054d,p=(double)hit/n,z2=z*z;double den=1+z2/n;double center=p+z2/(2*n);double rad=z*Math.sqrt((p*(1-p)+z2/(4*n))/n);return Math.max(0,(center-rad)/den);}
    private static double novelty(String hash, Set<String> known){return known.contains(hash)?0.0d:1.0d;}
    private static boolean shouldYield(long deadline){return TimeSourceR811.monoNowMs()>=deadline||RealtimePressureGateR811.hasActiveCriticalWork()||SettlementExecutionGateR819.isActive();}
    private static String inventionYield(DbStore store,String stage,int generation){String status="YIELD_P0_OR_BUDGET|stage="+safe(stage)+"|generation="+generation+"|sliceMs="+INVENTION_SLICE_MS;try{store.setState("r914_algorithm_invention_yield",status);store.setState("r914_algorithm_invention_yield_at",String.valueOf(System.currentTimeMillis()));}catch(Throwable ignore){}return status;}
    private static long seed(String x){long h=1469598103934665603L;String s=x==null?"":x;for(int i=0;i<s.length();i++){
            ResearchWorkBudgetR927.checkpoint();h^=s.charAt(i);h*=1099511628211L;}return h;}
    private static int parseInt(String x,int d){try{return Integer.parseInt(x);}catch(Throwable e){return d;}}
    private static String nextIssue(String issue){if(!TargetIssueResolver.isValid(issue))return "";TargetIssueResolver.Resolution r=TargetIssueResolver.next(issue);return r.valid?r.targetIssue:"";}
    private static double clamp01(double x){return Math.max(0,Math.min(1,Double.isFinite(x)?x:0));}
    private static double finiteOr(double x,double fallback){return Double.isFinite(x)?x:fallback;}
    private static String fmt(double x){return String.format(Locale.US,"%.4f",x);}
    private static String safe(String x){return x==null?"":x.trim();}
    private static String bounded(String x,int max){String s=safe(x);return s.length()<=max?s:s.substring(0,Math.max(0,max))+"…";}
    private static String survivorSummary(List<Evaluation> x){StringBuilder b=new StringBuilder();for(Evaluation e:x){
            ResearchWorkBudgetR927.checkpoint();if(b.length()>0)b.append(';');b.append(e.genome.genomeId).append(':').append(e.genome.chineseName()).append("/intent=").append(safe(e.mutationIntent)).append("/cell=").append(e.genome.mechanismCell()).append("/devN=").append(e.n).append("/HR=").append(fmt(e.hitRate)).append("/W=").append(fmt(e.wilsonLower)).append("/nov=").append(fmt(e.novelty));}return b.toString();}
    private static String encodeFeatures(double[] x){StringBuilder b=new StringBuilder("R908F1");for(int i=0;i<AlgorithmGenomeR908.INPUTS;i++)b.append('|').append(Long.toHexString(Double.doubleToLongBits(value(x,i))));return b.toString();}
    private static double[] decodeFeatures(String s){double[] out=new double[AlgorithmGenomeR908.INPUTS];String[]p=(s==null?"":s).split("\\|");if(p.length!=1+out.length||!"R908F1".equals(p[0]))return new double[0];for(int i=0;i<out.length;i++)try{out[i]=Double.longBitsToDouble(Long.parseUnsignedLong(p[i+1],16));}catch(Throwable e){return new double[0];}return out;}

    private static final class Evaluation { final AlgorithmGenomeR908 genome; int n,hit,maxMiss;double hitRate,wilsonLower,blockMin,novelty,searchScore;String behaviorHash="",mutationIntent="",creationReason="";Evaluation(AlgorithmGenomeR908 g){genome=g;} }
    private static final class CandidateGenome { final AlgorithmGenomeR908 genome;final String mutationIntent,creationReason;CandidateGenome(AlgorithmGenomeR908 g,String i,String r){genome=g;mutationIntent=safe(i);creationReason=safe(r);} }
    private static final class ParentSeed { final AlgorithmGenomeR908 genome;final AlgorithmGenomeR908.MutationIntent intent;final String failureReason;ParentSeed(AlgorithmGenomeR908 g,AlgorithmGenomeR908.MutationIntent i,String r){genome=g;intent=i==null?AlgorithmGenomeR908.MutationIntent.OPEN_EXPLORATION:i;failureReason=safe(r);} }
    private static final class GenomeRow { final AlgorithmGenomeR908 genome;final AlgorithmGenomeVmR908.State state;final String trainedUntil;GenomeRow(AlgorithmGenomeR908 g,AlgorithmGenomeVmR908.State s,String t){genome=g;state=s;trainedUntil=t==null?"":t;} }
    private static final class RuntimeObservation { final V51Models.PositionAction action;final double[]feature;final double localSpread;final int ordinal;
        RuntimeObservation(V51Models.PositionAction a,double[]f,double spread,int o){action=a;feature=f;localSpread=spread;ordinal=o;} }
    private static final class Selection { CandidateExecutionAuditR932.Proof audit; final int position,n1,n2,n3,ordinal;final double score,margin;final double[]feature;Selection(int p,int a,int b,int c,int o,double s,double m,double[]f){position=p;n1=a;n2=b;n3=c;ordinal=o;score=s;margin=m;feature=f;} }
}
