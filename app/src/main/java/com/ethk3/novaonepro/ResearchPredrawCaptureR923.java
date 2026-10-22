package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.LinkedHashMap;

/**
 * Crash-durable, pre-draw-only Research capture from Production's immutable 100/1200 snapshot.
 *
 * <p>No {@link V51RuntimeResult} is retained on a Handler queue.  Every retry reloads the exact
 * compressed snapshot committed for the target and refuses work once that target draw exists.
 * All four lanes rotate durably so short budgets cannot permanently starve a cohort.</p>
 */
public final class ResearchPredrawCaptureR923 {
    public static final String VERSION = "R923_DURABLE_PREDRAW_CAPTURE_V1";
    public static final long MAX_TOTAL_BUDGET_MS = GlobalResearchScheduleR932.TOTAL_MS;
    private ResearchPredrawCaptureR923() {}

    public static Result captureTarget(DbStore store, String requestedTarget, long budgetMs) {
        ResearchWorkBudgetR927.Scope scopeR927=ResearchWorkBudgetR927.enter(Math.min(budgetMs,MAX_TOTAL_BUDGET_MS));
        try { return captureTargetWithinR927(store,requestedTarget,budgetMs); }
        catch (ResearchWorkBudgetR927.Yield yielded) { throw yielded; }
        finally { GlobalResearchScheduleR932.endShared(); ResearchWorkBudgetR927.leave(scopeR927); }
    }

    public static Result captureTargetWithinR927(DbStore store, String requestedTarget, long budgetMs) {
        long started = TimeSourceR811.monoNowMs();
        String target = safe(requestedTarget);
        if (store == null) return Result.fail(target, "STORE_NULL");
        if (!TargetIssueResolver.isValid(target)) return Result.fail(target, "TARGET_INVALID");
        if (store.getDrawByIssue(target) != null){
            GlobalResearchScheduleR932.releaseCompleted(store,target);
            return publish(store, Result.fail(target, "TARGET_ALREADY_DRAWN_NO_RETROACTIVE_CAPTURE"));
        }
        if (RealtimePressureGateR811.hasActiveCriticalWork() || SettlementExecutionGateR819.isActive())
            return Result.fail(target, "YIELD_P0_BEFORE_SNAPSHOT_IO");
        SnapshotRow row = load(store, target);
        if (row == null) return publish(store, Result.fail(target, "DURABLE_SNAPSHOT_MISSING"));
        if (!row.hash.equals(DeferredEvidenceSnapshotR36.sha256(row.blob)))
            return publish(store, Result.fail(target, "DURABLE_SNAPSHOT_HASH_MISMATCH"));

        final DeferredEvidenceSnapshotR36.Snapshot snapshot;
        try {
            snapshot=DeferredEvidenceSnapshotR36.decodeCached(row.blob,row.hash);
        } catch (Throwable invalid) {
            return publish(store, Result.fail(target,
                    "DURABLE_SNAPSHOT_DECODE_" + invalid.getClass().getSimpleName()));
        }
        if (!target.equals(snapshot.targetIssue)
                || snapshot.numbers.size() != 100
                || snapshot.actions.size() != K3ActionSpaceEngine.TOTAL_ACTIONS)
            return publish(store, Result.fail(target, "DURABLE_SNAPSHOT_CONTRACT_MISMATCH"));
        if (store.getDrawByIssue(target) != null)
            return publish(store, Result.fail(target, "TARGET_DRAWN_AFTER_DECODE_NO_CAPTURE"));

        // A delayed upstream response may leave the target absent after its actual
        // scheduled draw. Do not spend a research slice freezing already-late evidence.
        final Models.Draw clockBase=store.getDrawByIssue(snapshot.baseIssue);
        if(System.currentTimeMillis()>=captureDeadlineR77(clockBase,target))
            return publish(store,Result.fail(target,"TARGET_DEADLINE_PASSED_NO_RETROACTIVE_CAPTURE"));

        // R950: legacy integrity audit is maintenance work, not a prerequisite for every fresh
        // pre-draw surface. The immutable roster admits only CLEAN/RESET_CLEAN heads or truly
        // untrained genomes; the latter enroll CLEAN atomically inside freezeOne(). This keeps
        // old proof repair from consuming the legal one-minute future window.
        store.setState("r950_algorithm_proof_recovery_predraw", "DEFERRED_TO_CONTINUITY_LANE");
        GlobalResearchScheduleR932.beginShared(store,target,row.hash);
        // Independent atoms and the frozen production champion do not depend on finishing
        // hundreds of candidate registrations. Capture them before resumable roster work.
        FixedResearchWindowR932.freezeChampion(store,snapshot);
        // The independent base bundle is real evidence; no existing research Certificate is required.
        String[] champion=null;
        try(Cursor c=store.readable().rawQuery("SELECT bundle_hash,selection_hash FROM "+FixedResearchWindowR932.CHAMPION+" WHERE target_issue=? LIMIT 2",new String[]{target})){
            if(c.moveToFirst()){champion=new String[]{c.getString(0),c.getString(1)};if(c.moveToNext())throw new IllegalStateException("CAPTURE_CHAMPION_AMBIGUOUS");}
        }
        int reservedEvaluations=0;
        if(champion==null)store.setState("cp11_capture_reservation_status","WAIT_CHAMPION_BUNDLE");
        else {
            final String[] frozenChampion=champion;
            boolean reserved=store.coordinatorR36().runAtomicWrite("CP12_RESERVE_PROSPECTIVE_CAPTURE",new DatabaseCoordinator.WriteRunnable(){
                public void run(SQLiteDatabase db){GlobalResearchScheduleR932.reserveUnscheduledFrozenCP11(db,target,frozenChampion[0],frozenChampion[1]);}
            });
            if(!reserved)throw new IllegalStateException("CAPTURE_RESERVATION_COMMIT_FAILED");
            reservedEvaluations=FrozenCandidateAdapterR958.captureReservedCP12(store,snapshot);
        }
        // Reserved consecutive observations have a one-issue deadline. Atom discovery
        // resumes on the same worker after those already-admitted obligations commit.
        GlobalResearchScheduleR932.captureAtoms(store,snapshot);
        // R979: independent trained-model evaluations must not wait behind full legacy
        // cohort preparation. Give training, frozen evaluation and general cohorts a
        // durable first slot. An interrupted callback advances priority, not evidence.
        int preflight=ResearchScheduleR924.phase(store.getState("r979_predraw_preflight","0"),3);
        store.setState("r979_predraw_preflight",Integer.toString((preflight+1)%3));
        int frozenEvaluations=0;
        try {
            if(preflight==0)com.ethk3.novaonepro.r952.ResearchDirectorR952.capturePredraw(store,target,row.hash,16);
            else if(preflight==1)frozenEvaluations=FrozenCandidateAdapterR958.capture(store,snapshot);
        } catch (ResearchWorkBudgetR927.Yield yielded) {
            throw yielded;
        } catch (RuntimeResourceAuditR927.BudgetExceeded yielded) {
            throw yielded;
        } catch (RuntimeException failure) {
            store.setState("r958_extension_predraw_error",target+"|"+boundedFailure(failure));
        }
        GlobalResearchScheduleR932.prepareExecutionFirst(store,target,snapshot.recordHash);
        long acceptedBudget = Math.max(500L, Math.min(MAX_TOTAL_BUDGET_MS, budgetMs));
        long deadline = started + acceptedBudget;
        String tiny = "NOT_ATTEMPTED";
        int algorithms = reservedEvaluations + frozenEvaluations, assets = 0, formulas = 0;
        String formulaStatus = "NOT_ATTEMPTED";
        String stop = "";

        // R924 persists the next lane after each attempt, including failure/budget yield.
        // A retry starts at the first unserved lane instead of recapturing algorithms forever.
        int first = ResearchScheduleR924.phase(store.getState("r924_predraw_next_lane", "0"), 4);
        int pendingMask = GlobalResearchScheduleR932.pendingLaneMask(store,target);
        boolean[] attemptedLane = new boolean[4];
        for (int step = 0; step < 4; step++) {
            ResearchWorkBudgetR927.checkpoint();
            if (!legal(store, target, deadline)) {
                stop = stopReason(store, target, deadline);
                break;
            }
            int lane = chooseLane(first, pendingMask, attemptedLane);
            if (lane < 0) break;
            attemptedLane[lane] = true;
            // Preparation has already consumed part of this callback. Give the next pending
            // lane a useful quantum; durable rotation serves the others on the next callback.
            long slice = Math.min(Math.max(1L,deadline-TimeSourceR811.monoNowMs()),GlobalResearchScheduleR932.SLICE_MS);
            ResearchWorkBudgetR927.Scope laneScope=ResearchWorkBudgetR927.enter(slice);
            try {
                if (lane == 0) tiny = ResearchTinyDeepLearnerR909.capturePredraw(store, snapshot);
                else if (lane == 1) {
                    algorithms += AlgorithmInventionLabR908.capturePredraw(store, snapshot, 96, slice);
                }
                else if (lane == 2) assets = AssetGenesisEngineR915.capturePredraw(store, snapshot, 96, slice);
                else {
                    ResearchShadowChallengerRuntimeR817.BatchResult batch =
                            ResearchShadowChallengerRuntimeR817.captureR906Batch(store, snapshot, 96, slice);
                    formulas = batch == null ? 0 : batch.frozen;
                    formulaStatus = batch == null ? "NULL_RESULT" : safe(batch.status);
                }
            } catch (ResearchWorkBudgetR927.Yield yielded) {
                stop="SYSTEM_BUDGET_YIELD_LANE_"+lane;
            } catch (Throwable failure) {
                String error = "LANE_" + lane + "_" + failure.getClass().getSimpleName()
                        + ":" + boundedFailure(failure);
                stop = error;
                if (lane == 0) tiny = error;
                else if (lane == 1) algorithms = -1;
                else if (lane == 2) assets = -1;
                else { formulas = -1; formulaStatus = error; }
            } finally {
                ResearchWorkBudgetR927.leave(laneScope);
                store.setState("r924_predraw_next_lane", String.valueOf(ResearchScheduleR924.next(lane, 4)));
            }
        }

        String boundStatus="NOT_RUN_MANDATORY_CAPTURE_PENDING";
        if(ResearchCaptureQueueR946.mandatoryCompleteR80(store,target)) {
            long boundRemaining=Math.min(deadline-TimeSourceR811.monoNowMs(),ResearchWorkBudgetR927.remainingMs());
            if(boundRemaining>=80L) {
                ResearchWorkBudgetR927.Scope boundScope=ResearchWorkBudgetR927.enter(boundRemaining);
                try { boundStatus="CAPTURED_"+FrozenCandidateAdapterR958.captureBoundSourcesR80(store,snapshot); }
                catch(ResearchWorkBudgetR927.Yield yielded) { boundStatus="NOT_RUN_YIELD_"+boundedFailure(yielded); }
                catch(RuntimeException unavailable) { boundStatus="NOT_RUN_ERROR_"+boundedFailure(unavailable); }
                finally { ResearchWorkBudgetR927.leave(boundScope); }
            } else boundStatus="NOT_RUN_OWNER_BUDGET";
        }
        String probeStatus="NOT_RUN_MANDATORY_CAPTURE_PENDING";
        String status = stop.length() == 0 ? (ResearchCaptureQueueR946.complete(store,target) ? "ISSUE_CAPTURE_COMPLETE" : "SLICE_COMMITTED_CONTINUATION_PENDING") : "PARTIAL_" + stop;
        if("ISSUE_CAPTURE_COMPLETE".equals(status)) {
            GlobalResearchScheduleR932.releaseCompleted(store,target);
            // Optional probe research runs only after all admitted capture obligations. It
            // cannot reserve slots, delay mandatory capture, or extend this owner deadline.
            try { probeStatus=MultiHorizonProbeR80.captureOptional(store,snapshot,deadline); }
            catch(RuntimeException unavailable) { probeStatus="NOT_RUN_ERROR_"+boundedFailure(unavailable); }
        }
        long cost = Math.max(0L, TimeSourceR811.monoNowMs() - started);
        Result result = new Result(
                true, target, status, tiny, algorithms, assets, formulas, formulaStatus, cost,probeStatus,boundStatus);
        return publish(store, result);
    }


    /** R952: pending admitted work wins over empty lanes; rotation remains the tie-breaker. */
    static int chooseLane(int first,int pendingMask,boolean[] attempted) {
        int start=Math.floorMod(first,4);
        if(attempted==null||attempted.length<4)throw new IllegalArgumentException("R952_LANE_ATTEMPT_STATE");
        for(int pass=0;pass<2;pass++) {
            for(int offset=0;offset<4;offset++) {
                int lane=(start+offset)%4;
                if(attempted[lane])continue;
                if(pass==0 && pendingMask!=0 && (pendingMask&(1<<lane))==0)continue;
                return lane;
            }
        }
        return -1;
    }

    static long captureDeadlineR77(Models.Draw base,String target){
        if(base==null||!TargetIssueResolver.exact(base.issue,target).valid)
            throw new IllegalStateException("R77_CAPTURE_CLOCK_BASE_MISMATCH");
        return TargetIssueResolver.predrawDeadlineWallR20261008(base);
    }

    private static boolean legal(DbStore store, String target, long deadline) {
        return TimeSourceR811.monoNowMs() < deadline
                && !RealtimePressureGateR811.hasActiveCriticalWork()
                && !SettlementExecutionGateR819.isActive()
                && store.getDrawByIssue(target) == null;
    }

    private static String stopReason(DbStore store, String target, long deadline) {
        if (store.getDrawByIssue(target) != null) return "TARGET_DRAWN_NO_RETROACTIVE_CAPTURE";
        if (RealtimePressureGateR811.hasActiveCriticalWork()) return "YIELD_P0_PRESSURE";
        if (SettlementExecutionGateR819.isActive()) return "YIELD_SETTLEMENT_P0";
        if (TimeSourceR811.monoNowMs() >= deadline) return "BUDGET_EXHAUSTED";
        return "LEGAL_WINDOW_CLOSED";
    }

    private static long stageBudget(long deadline, long preferred) {
        return Math.max(100L, Math.min(preferred, Math.max(100L, deadline - TimeSourceR811.monoNowMs())));
    }

    private static SnapshotRow load(DbStore store, String target) {
        Cursor c = null;
        try {
            c = store.readable().rawQuery(
                    "SELECT snapshot_blob,snapshot_hash FROM "
                            + DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB
                            + " WHERE target_issue=? AND snapshot_blob IS NOT NULL LIMIT 1",
                    new String[] {target});
            if (!c.moveToFirst()) return null;
            byte[] blob = c.getBlob(0);
            String hash = safe(c.getString(1));
            return blob == null || blob.length == 0 || hash.length() != 64
                    ? null : new SnapshotRow(blob, hash);
        } catch (Throwable unavailable) {
            return null;
        } finally {
            if (c != null) c.close();
        }
    }

    private static Result publish(DbStore store, Result result) {
        if (store == null || result == null) return result;
        try {
            LinkedHashMap<String, String> state = new LinkedHashMap<String, String>();
            state.put("r923_research_predraw_version", VERSION);
            state.put("r923_research_predraw_target", result.target);
            state.put("r923_research_predraw_status", result.canonical());
            state.put("r923_research_predraw_tiny_deep", result.tinyDeep);
            state.put("r923_research_predraw_algorithms", String.valueOf(result.algorithms));
            state.put("r923_research_predraw_genesis_assets", String.valueOf(result.genesisAssets));
            state.put("r923_research_predraw_formulas", String.valueOf(result.formulas));
            state.put("r923_research_predraw_at", String.valueOf(System.currentTimeMillis()));
            state.put("r80_probe_last_status",result.target+"|"+result.probeStatus);
            state.put("r80_bound_source_last_status",result.target+"|"+result.boundSourceStatus);
            store.setStates(state);
        } catch (Throwable ignored) {}
        return result;
    }

    static String boundedFailure(Throwable failure) {
        String message = safe(failure == null ? "" : failure.getMessage()).replace('\n', ' ').replace('\r', ' ');
        return message.length() > 240 ? message.substring(0, 240) : message;
    }

    private static String safe(String value) {
        return value == null ? "" : value.replace('|', '_').trim();
    }

    private static final class SnapshotRow {
        final byte[] blob;
        final String hash;
        SnapshotRow(byte[] value, String digest) { blob = value; hash = digest; }
    }

    public static final class Result {
        public final boolean success;
        public final String target, status, tinyDeep, formulaStatus, probeStatus, boundSourceStatus;
        public final int algorithms, genesisAssets, formulas;
        public final long costMs;

        Result(boolean ok, String targetValue, String statusValue, String tinyValue,
                int algorithmCount, int assetCount, int formulaCount, String formulaState,
                long elapsed) {
            this(ok,targetValue,statusValue,tinyValue,algorithmCount,assetCount,formulaCount,formulaState,elapsed,"NOT_RUN_OWNER_NOT_REACHED","NOT_RUN_OWNER_NOT_REACHED");
        }
        Result(boolean ok,String targetValue,String statusValue,String tinyValue,int algorithmCount,
                int assetCount,int formulaCount,String formulaState,long elapsed,String probe,String bound) {
            probeStatus=safe(probe);boundSourceStatus=safe(bound);
            success = ok;
            target = safe(targetValue);
            status = safe(statusValue);
            tinyDeep = safe(tinyValue);
            algorithms = algorithmCount;
            genesisAssets = assetCount;
            formulas = formulaCount;
            formulaStatus = safe(formulaState);
            costMs = Math.max(0L, elapsed);
        }

        static Result fail(String target, String status) {
            return new Result(false, target, status, "NOT_ATTEMPTED", 0, 0, 0,
                    "NOT_ATTEMPTED", 0L);
        }

        public String canonical() {
            return VERSION + "|target=" + target + "|status=" + status
                    + "|tinyDeep=" + tinyDeep + "|algorithms=" + algorithms
                    + "|genesisAssets=" + genesisAssets + "|formulas=" + formulas
                    + "|formulaStatus=" + formulaStatus + "|costMs=" + costMs + "|probe="+probeStatus+"|boundSource="+boundSourceStatus;
        }
    }
}
