package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Executes one bounded Research formula against the already-computed exact 1200-action surface.
 * The prediction is frozen before draw and is settled only from SettlementCoordinatorR815.  This
 * class owns no Production decision, BET or settlement authority.
 */
public final class ResearchShadowChallengerRuntimeR817 {
    public static final String OWNER = "RESEARCH_SHADOW_SIDECAR_ONLY";
    public static final boolean MAY_AUTHORIZE_REAL = false;

    private ResearchShadowChallengerRuntimeR817() {}

    /**
     * R903 evaluates a bounded cohort for the same future issue.  The previous one-candidate
     * round-robin required roughly 96 live issues before every formula received a second sample.
     * Each call still freezes an independent immutable prediction and remains Research-only.
     */
    /** R906 shared-matrix evaluator: loads a whole lightweight cohort, evaluates vector plans, then freezes in one DB transaction. */
    public static BatchResult captureR906Batch(
            final DbStore store, final V51RuntimeResult runtime, int maxCandidates, long budgetMs) {
        if (store == null || runtime == null || !runtime.success || runtime.context == null
                || runtime.actions == null || runtime.actions.length != K3ActionSpaceEngine.TOTAL_ACTIONS)
            return new BatchResult(false, "R906_INPUT_INVALID", 0, "[]");
        return captureSurface(
                store,
                runtime.context.targetIssue,
                runtime.context.baseIssue,
                runtime.context.historyHash,
                runtime.checksum,
                runtime.actions,
                maxCandidates,
                budgetMs);
    }

    /** Crash-durable replay from the canonical snapshot; no Production model is recomputed. */
    public static BatchResult captureR906Batch(
            final DbStore store,
            final DeferredEvidenceSnapshotR36.Snapshot snapshot,
            int maxCandidates,
            long budgetMs) {
        if (store == null || snapshot == null)
            return new BatchResult(false, "R914_SNAPSHOT_INPUT_INVALID", 0, "[]");
        return captureSurface(
                store,
                snapshot.targetIssue,
                snapshot.baseIssue,
                snapshot.historyHash,
                snapshot.recordHash,
                snapshot.positionActions(),
                maxCandidates,
                budgetMs);
    }

    private static BatchResult captureSurface(
            final DbStore store,
            final String targetIssue,
            final String baseIssue,
            final String historyHash,
            final String surfaceHash,
            final V51Models.PositionAction[] actions,
            int maxCandidates,
            long budgetMs) {
        final String target = safe(targetIssue);
        final String base = safe(baseIssue);
        if (store == null
                || !TargetIssueResolver.exact(base, target).valid
                || safe(historyHash).length() != 64
                || safe(surfaceHash).length() != 64
                || actions == null
                || actions.length != K3ActionSpaceEngine.TOTAL_ACTIONS)
            return new BatchResult(false, "R914_SURFACE_INPUT_INVALID", 0, "[]");
        if (AppConfig.R36_REAL_BET_UNLOCK || MAY_AUTHORIZE_REAL)
            throw new IllegalStateException("R906_RESEARCH_REAL_BOUNDARY");
        if (store.getDrawByIssue(target) != null)
            return new BatchResult(false, "R914_TARGET_ALREADY_DRAWN", 0, "[]");
        final long started = System.currentTimeMillis();
        final long deadline = started + Math.max(100L, Math.min(1800L, budgetMs));
        GlobalResearchScheduleR932.prepareConsumer(store,target,surfaceHash);
        java.util.List<String> cohort = ResearchEvaluationSchedulerR906.cohortForTarget(
                store, target,
                Math.max(1, Math.min(AppConfig.R906_RESEARCH_ACTIVE_MAX, maxCandidates)));
        if (cohort.isEmpty()) return new BatchResult(true, "R906_NO_EXECUTABLE_ASSET", 0, "[]");
        java.util.ArrayList<String> dispatchedKeys=new java.util.ArrayList<String>();
        for(String assetId:cohort)dispatchedKeys.add("formula:"+assetId);
        ResearchCaptureLedgerR932.markDispatched(store,target,dispatchedKeys,"FORMULA_R906");
        final double[][] matrix = new double[actions.length][ResearchCompiledGraphRuntimeR906.MATRIX_FEATURES];
        for (int i = 0; i < actions.length; i++)
            matrix[i] = ResearchCompiledGraphRuntimeR906.canonicalVector(actions[i]);
        final java.util.ArrayList<R906Freeze> freezes = new java.util.ArrayList<R906Freeze>();
        final java.util.ArrayList<String> attempted = new java.util.ArrayList<String>();
        HistoricalStrategyInventionR908.Profile historicalProfile = null;
        for (int c = 0; c < cohort.size() && System.currentTimeMillis() < deadline; c++) {
            ResearchWorkBudgetR927.checkpoint();
            String assetId = cohort.get(c);
            boolean proxy=GlobalResearchScheduleR932.proxy(store,target,"formula:"+assetId);
            if(!proxy)attempted.add(assetId);
            ResearchCaptureLedgerR932.record(store,target,"formula:"+assetId,"EXECUTION_STARTED",proxy?"PROXY120":"FULL1200",0);
            R906PlanRow row = loadR906Plan(store, assetId);
            if (row == null) continue;
            ResearchCompiledGraphRuntimeR906.Compiled compiled = ResearchCompiledGraphRuntimeR906.compile(row.plan);
            if (!compiled.executable || !row.artifactHash.equals(compiled.artifactHash)) continue;
            CandidateExecutionAuditR932.Builder audit=proxy?null:new CandidateExecutionAuditR932.Builder();
            String programBefore=row.plan.canonical();
            ResearchMethodPlanR906 replayPlan=ResearchMethodPlanR906.parseCanonical(programBefore);
            ResearchCompiledGraphRuntimeR906.Compiled replayCompiled=ResearchCompiledGraphRuntimeR906.compile(replayPlan);
            if(!compiled.artifactHash.equals(replayCompiled.artifactHash))throw new IllegalStateException("R932_FORMULA_PROGRAM_ROUNDTRIP");
            double[] score;
            V51Models.PositionAction[] useActions=actions;double[][] useMatrix=matrix;
            if(proxy){useActions=new V51Models.PositionAction[120];useMatrix=new double[120][];for(int i=0;i<120;i++){useActions[i]=actions[i*10];useMatrix[i]=matrix[i*10];}}
            if (compiled.family == ResearchCompiledGraphRuntimeR906.Family.HISTORICAL_SYNTHESIS) {
                if (historicalProfile == null)
                    historicalProfile = GlobalResearchScheduleR932.profile(store, base);
                score = HistoricalStrategyInventionR908.evaluateAll(historicalProfile, useActions, row.plan);
            } else {
                score = ResearchCompiledGraphRuntimeR906.evaluateAll(compiled, useMatrix);
            }
            double[] replay=proxy?score:compiled.family==ResearchCompiledGraphRuntimeR906.Family.HISTORICAL_SYNTHESIS
                ?HistoricalStrategyInventionR908.evaluateAll(historicalProfile,useActions,replayPlan)
                :ResearchCompiledGraphRuntimeR906.evaluateAll(replayCompiled,useMatrix);
            int best = -1; double bestScore = -Double.MAX_VALUE;
            for (int i = 0; i < score.length; i++) {
                V51Models.PositionAction a = useActions[i];
                if(audit!=null)audit.add(K3ActionSpaceEngine.globalOrdinal(a.position,a.n1,a.n2,a.n3),score[i],replay[i],a.rank*1201+i);
                if (best < 0 || score[i] > bestScore
                        || (score[i] == bestScore && a.rank < useActions[best].rank)) {
                    best = i; bestScore = score[i];
                }
            }
            if (best < 0) continue;
            V51Models.PositionAction a = useActions[best];
            int ordinal = K3ActionSpaceEngine.globalOrdinal(a.position, a.n1, a.n2, a.n3);
            if(proxy){GlobalResearchScheduleR932.saveProxy(store,target,"formula:"+assetId,surfaceHash,ordinal,bestScore);continue;}
            long frozenAt = System.currentTimeMillis();
            String inputHash = V51ChecksumEngine.sha256("R914_SHARED_MATRIX|" + target
                    + '|' + historyHash + '|' + surfaceHash + '|' + compiled.artifactHash);
            String recordHash = V51ChecksumEngine.sha256("R906_PREDRAW|" + assetId + '|'
                    + target + '|' + ordinal + '|' + bestScore + '|' + inputHash);
            R906Freeze frozen=new R906Freeze(assetId,row.tier,compiled.artifactHash,a,ordinal,bestScore,inputHash,frozenAt,recordHash);
            frozen.audit=audit.finish(ordinal,programBefore,row.plan.canonical(),"STATELESS","STATELESS","STATELESS");
            // A completed candidate survives any later candidate's yield or failure.
            if(freezeR906One(store,target,base,surfaceHash,frozen,Math.max(0L,System.currentTimeMillis()-started)))freezes.add(frozen);
        }
        long elapsed=Math.max(0L,System.currentTimeMillis()-started);
        boolean targetDrawn=store.getDrawByIssue(target)!=null;
        String status=targetDrawn?"R914_TARGET_DRAWN_DURING_CAPTURE":
            "R943_CANDIDATES_COMMITTED="+freezes.size()+"/"+attempted.size()+"|costMs="+elapsed;
        store.setState("r906_research_batch_status",status);store.setState("r906_research_batch_target",target);
        return new BatchResult(!targetDrawn,status,freezes.size(),attempted.toString());
    }

    private static boolean freezeR906One(final DbStore store,final String target,final String base,
            final String surfaceHash,final R906Freeze f,final long elapsed){
        return Boolean.TRUE.equals(store.coordinatorR36().runAtomicWrite("R943_RESEARCH_FORMULA_FREEZE_ONE",
            new DatabaseCoordinator.WriteCallable<Boolean>(){public Boolean call(SQLiteDatabase db){
                if(drawExists(db,target))return Boolean.FALSE;
                ContentValues v=new ContentValues();
                v.put("candidate_id",f.assetId);v.put("target_issue",target);v.put("phase","R906_"+f.tier);
                v.put("position",f.action.position);v.put("n1",f.action.n1);v.put("n2",f.action.n2);v.put("n3",f.action.n3);
                v.put("selected_ordinal",f.ordinal);v.put("score",f.score);v.put("probability",-1.0d);
                v.put("formula_probability",-1.0d);v.put("adaptive_probability",-1.0d);v.put("adaptive_artifact_hash","");
                v.put("adaptive_trained_until","");v.put("adaptive_updates",0);v.put("adaptive_weight",0.0d);
                v.put("formula_hash",f.artifactHash);v.put("input_hash",f.inputHash);v.put("frozen_at",f.frozenAt);v.put("record_hash",f.recordHash);
                long ins=db.insertWithOnConflict(DbHelper.TABLE_R817_RESEARCH_PREDRAW,null,v,SQLiteDatabase.CONFLICT_IGNORE);
                if(ins<0L&&!predrawMatchesR906(db,f.assetId,target,f.recordHash))throw new IllegalStateException("R932_FORMULA_FREEZE_CONFLICT");
                if(f.audit==null)throw new IllegalStateException("R932_FORMULA_FULL_REPLAY_REQUIRED");
                f.audit.freeze(db,"formula:"+f.assetId,f.artifactHash,surfaceHash,base,target,f.recordHash);
                FixedResearchWindowR932.freezePair(db,"formula:"+f.assetId,f.artifactHash,target,f.recordHash,f.action.position,f.action.n1,f.action.n2,f.action.n3,f.score);
                ResearchCaptureLedgerR932.record(db,target,"formula:"+f.assetId,"FROZEN","RAW_SCORE_NOT_CALIBRATED",f.recordHash,Math.max(1L,f.audit.elapsedNs/1000000L),true);
                if(ins>=0L)bumpCoverageR906(db,f.assetId,1,1,1,target,System.currentTimeMillis(),elapsed);
                return Boolean.TRUE;
            }},Boolean.FALSE));
    }

    private static R906PlanRow loadR906Plan(DbStore store,String assetId){
        Cursor c=null;try{c=store.readable().rawQuery(
                "SELECT tier,representation_family,learner_family,update_rule,objective_family,calibration_family,risk_family,falsifier_family,parent_asset_ids,generation,genetic_operator,runtime_executable,canonical_spec,artifact_hash,method_signature FROM "
                        +DbHelper.TABLE_R906_RESEARCH_ASSET+" WHERE asset_id=? LIMIT 1",new String[]{assetId});
            if(!c.moveToFirst())return null;
            String canonical=safe(c.getString(12));
            String feature=fieldR906(canonical,"feature");
            ResearchMethodPlanR906 p=new ResearchMethodPlanR906(c.getString(1),feature,c.getString(2),c.getString(3),c.getString(4),c.getString(5),c.getString(6),c.getString(7),c.getString(8),c.getInt(9),c.getString(10),c.getInt(11)==1);
            String storedSignature=safe(c.getString(14));
            if(!storedSignature.equals(ResearchMethodSignatureR906.of(p)))return null;
            ResearchCompiledGraphRuntimeR906.Compiled verify=ResearchCompiledGraphRuntimeR906.compile(p);
            String storedArtifact=safe(c.getString(13));
            if(!verify.executable||!storedArtifact.equals(verify.artifactHash))return null;
            return new R906PlanRow(p,c.getString(0),storedArtifact);
        }finally{if(c!=null)c.close();}
    }
    private static void bumpCoverageR906(SQLiteDatabase db,String assetId,int eligible,int scheduled,int frozen,String issue,long now,long latency){
        Cursor c=null;int e=0,s=0,f=0,settled=0,hit=0;String first="";try{c=db.rawQuery("SELECT eligible_n,scheduled_n,frozen_n,settled_n,hit_n,first_eligible_issue FROM "+DbHelper.TABLE_R906_RESEARCH_EVAL_COVERAGE+" WHERE asset_id=? LIMIT 1",new String[]{assetId});if(c.moveToFirst()){e=c.getInt(0);s=c.getInt(1);f=c.getInt(2);settled=c.getInt(3);hit=c.getInt(4);first=safe(c.getString(5));}}finally{if(c!=null)c.close();}
        ContentValues v=new ContentValues();v.put("asset_id",assetId);v.put("eligible_n",e+eligible);v.put("scheduled_n",s+scheduled);v.put("frozen_n",f+frozen);v.put("settled_n",settled);v.put("hit_n",hit);v.put("first_eligible_issue",first.length()==0&&eligible>0?issue:first);v.put("last_eligible_issue",eligible>0?issue:"");v.put("last_frozen_issue",frozen>0?issue:"");v.put("last_settled_issue","");v.put("max_gap_issues",Math.max(0,(e+eligible)-(f+frozen)));v.putNull("hit_rate");v.putNull("interval_lower");v.putNull("interval_upper");v.put("interval_method","");v.putNull("brier");v.putNull("ece");v.putNull("block_stability");v.putNull("drift_score");v.put("latency_p95_ms",(double)latency);v.put("ram_p95_bytes",Runtime.getRuntime().totalMemory()-Runtime.getRuntime().freeMemory());v.put("evidence_hash",V51ChecksumEngine.sha256("R906_COVERAGE|"+assetId+'|'+(e+eligible)+'|'+(s+scheduled)+'|'+(f+frozen)+'|'+settled+'|'+hit));v.put("updated_at",now);db.insertWithOnConflict(DbHelper.TABLE_R906_RESEARCH_EVAL_COVERAGE,null,v,SQLiteDatabase.CONFLICT_REPLACE);
    }
    private static boolean predrawMatchesR906(SQLiteDatabase db,String id,String target,String hash){Cursor c=null;try{c=db.rawQuery("SELECT record_hash FROM "+DbHelper.TABLE_R817_RESEARCH_PREDRAW+" WHERE candidate_id=? AND target_issue=? LIMIT 1",new String[]{id,target});return c.moveToFirst()&&hash.equals(safe(c.getString(0)));}finally{if(c!=null)c.close();}}
    private static boolean drawExists(SQLiteDatabase db,String target){Cursor c=null;try{c=db.rawQuery("SELECT 1 FROM "+DbHelper.TABLE_DRAWS+" WHERE issue=? LIMIT 1",new String[]{target});return c.moveToFirst();}finally{if(c!=null)c.close();}}
    private static String fieldR906(String canonical,String key){String prefix=key+"=";if(canonical==null)return "";String[]parts=canonical.split("\\|");for(String part:parts){String x=safe(part);if(x.startsWith(prefix))return x.substring(prefix.length());}return "";}
    private static final class R906PlanRow{final ResearchMethodPlanR906 plan;final String tier,artifactHash;R906PlanRow(ResearchMethodPlanR906 p,String t,String a){plan=p;tier=safe(t);artifactHash=safe(a);}}
    private static final class R906Freeze{CandidateExecutionAuditR932.Proof audit;final String assetId,tier,artifactHash,inputHash,recordHash;final V51Models.PositionAction action;final int ordinal;final double score;final long frozenAt;R906Freeze(String id,String t,String ah,V51Models.PositionAction a,int o,double s,String ih,long at,String rh){assetId=id;tier=t;artifactHash=ah;action=a;ordinal=o;score=s;inputHash=ih;frozenAt=at;recordHash=rh;}}

    public static BatchResult captureBatch(
            DbStore store, V51RuntimeResult runtime, int maxCandidates, long budgetMs) {
        int bound = Math.max(1, Math.min(32, maxCandidates));
        long deadline = System.currentTimeMillis() + Math.max(80L, Math.min(1600L, budgetMs));
        int frozen = 0;
        String lastStatus = "NO_ATTEMPT";
        StringBuilder ids = new StringBuilder();
        for (int i = 0; i < bound && System.currentTimeMillis() < deadline; i++) {
            Result one = capture(store, runtime);
            lastStatus = one.status;
            if (!one.success) break;
            frozen++;
            if (ids.length() > 0) ids.append(',');
            ids.append(one.candidateId);
        }
        if (store != null) {
            store.setState("r903_research_capture_count", String.valueOf(frozen));
            store.setState("r903_research_capture_candidates", ids.toString());
            store.setState("r903_research_capture_at", String.valueOf(System.currentTimeMillis()));
        }
        return new BatchResult(
                frozen > 0,
                "R903_BATCH_FROZEN=" + frozen + "|last=" + lastStatus,
                frozen,
                ids.toString());
    }

    public static Result capture(final DbStore store, final V51RuntimeResult runtime) {
        if (store == null || runtime == null || !runtime.success)
            return Result.skip("INPUT_INVALID");
        if (AppConfig.R36_REAL_BET_UNLOCK
                || MAY_AUTHORIZE_REAL)
            throw new IllegalStateException("R817_RESEARCH_REAL_BOUNDARY");
        if (!store.coordinatorR36().isR811ResearchSchemaReady())
            return Result.skip("SCHEMA_NOT_READY");
        if (runtime.context == null
                || !TargetIssueResolver.isValid(runtime.context.targetIssue)
                || runtime.actions == null
                || runtime.actions.length != K3ActionSpaceEngine.TOTAL_ACTIONS)
            return Result.skip("RUNTIME_SURFACE_INVALID");

        final String targetIssue = runtime.context.targetIssue;
        final Candidate candidate = nextCandidate(store, targetIssue);
        if (candidate == null) return Result.skip("NO_RUNTIME_FORMULA_CANDIDATE");
        final ResearchStrategyDslEngine.Node formula;
        try {
            formula = ResearchStrategyDslEngine.parse(candidate.formula);
        } catch (Throwable invalidFormula) {
            return Result.skip("FORMULA_INVALID");
        }
        /* Only an artifact trained on an earlier issue may affect this future prediction. */
        final AdaptiveContext adaptive = adaptiveForTarget(store, targetIssue);

        FeatureView features = new FeatureView();
        double[] adaptiveFeatures = adaptive.enabled() ? new double[7] : null;
        V51Models.PositionAction best = null;
        int bestOrdinal = -1;
        double bestScore = -Double.MAX_VALUE;
        double bestFormulaProbability = 0.0d;
        double bestAdaptiveProbability = -1.0d;
        for (int i = 0; i < runtime.actions.length; i++) {
            V51Models.PositionAction action = runtime.actions[i];
            if (action == null) return Result.skip("ACTION_NULL_" + i);
            int ordinal =
                    K3ActionSpaceEngine.globalOrdinal(
                            action.position, action.n1, action.n2, action.n3);
            if (ordinal != i) return Result.skip("ACTION_ORDINAL_" + i);
            features.bind(action);
            double formulaScore = ResearchStrategyDslEngine.evaluate(formula, features);
            double formulaProbability = probability(formulaScore);
            double adaptiveProbability = -1.0d;
            double score = formulaProbability;
            if (adaptive.enabled()) {
                adaptiveFeatures[0] = finite(action.rawActionP);
                adaptiveFeatures[1] = finite(action.rankScore);
                adaptiveFeatures[2] = finite(action.thirdMargin);
                adaptiveFeatures[3] = finite(action.entropy);
                adaptiveFeatures[4] = finite(action.stability);
                adaptiveFeatures[5] = meta(action.metaFeatures, 20);
                adaptiveFeatures[6] = formulaProbability;
                adaptiveProbability = probability(adaptive.learner.predictResearch(adaptiveFeatures));
                score = blend(formulaProbability, adaptiveProbability, adaptive.weight);
            }
            if (best == null
                    || score > bestScore
                    || (score == bestScore
                            && (action.rank < best.rank
                                    || (action.rank == best.rank && ordinal < bestOrdinal)))) {
                best = action;
                bestOrdinal = ordinal;
                bestScore = score;
                bestFormulaProbability = formulaProbability;
                bestAdaptiveProbability = adaptiveProbability;
            }
        }
        if (best == null || bestOrdinal < 0) return Result.skip("NO_SELECTION");

        final long frozenAt = System.currentTimeMillis();
        final String formulaHash = ResearchStrategyDslEngine.hash(formula);
        final String inputHash =
                V51ChecksumEngine.sha256(
                        "R817_RESEARCH_INPUT|"
                                + targetIssue
                                + '|'
                                + runtime.context.historyHash
                                + '|'
                                + runtime.checksum
                                + '|'
                                + candidate.candidateHash
                                + '|'
                                + adaptive.artifactHash
                                + '|'
                                + adaptive.trainedUntil
                                + '|'
                                + adaptive.updates
                                + '|'
                                + Double.toString(adaptive.weight));
        final double probability = probability(bestScore);
        final double formulaProbability = probability(bestFormulaProbability);
        final double adaptiveProbability =
                bestAdaptiveProbability < 0.0d ? -1.0d : probability(bestAdaptiveProbability);
        final V51Models.PositionAction selected = best;
        final int selectedOrdinal = bestOrdinal;
        final double selectedScore = finite(bestScore);
        final String recordHash =
                V51ChecksumEngine.sha256(
                        "R817_PREDRAW|"
                                + candidate.candidateId
                                + '|'
                                + targetIssue
                                + '|'
                                + selected.position
                                + '|'
                                + selected.n1
                                + '|'
                                + selected.n2
                                + '|'
                                + selected.n3
                                + '|'
                                + selectedOrdinal
                                + '|'
                                + Double.toString(selectedScore)
                                + '|'
                                + Double.toString(probability)
                                + '|'
                                + Double.toString(formulaProbability)
                                + '|'
                                + Double.toString(adaptiveProbability)
                                + '|'
                                + adaptive.artifactHash
                                + '|'
                                + adaptive.trainedUntil
                                + '|'
                                + adaptive.updates
                                + '|'
                                + Double.toString(adaptive.weight)
                                + '|'
                                + formulaHash
                                + '|'
                                + inputHash);

        Boolean inserted =
                store.coordinatorR36()
                        .runWrite(
                                "R817_RESEARCH_PREDRAW_CAPTURE",
                                new DatabaseCoordinator.WriteCallable<Boolean>() {
                                    @Override
                                    public Boolean call(SQLiteDatabase db) {
                                        ContentValues values = new ContentValues();
                                        values.put("candidate_id", candidate.candidateId);
                                        values.put("target_issue", targetIssue);
                                        values.put("phase", candidate.status);
                                        values.put("position", selected.position);
                                        values.put("n1", selected.n1);
                                        values.put("n2", selected.n2);
                                        values.put("n3", selected.n3);
                                        values.put("selected_ordinal", selectedOrdinal);
                                        values.put("score", selectedScore);
                                        values.put("probability", probability);
                                        values.put("formula_probability", formulaProbability);
                                        values.put("adaptive_probability", adaptiveProbability);
                                        values.put("adaptive_artifact_hash", adaptive.artifactHash);
                                        values.put("adaptive_trained_until", adaptive.trainedUntil);
                                        values.put("adaptive_updates", adaptive.updates);
                                        values.put("adaptive_weight", adaptive.weight);
                                        values.put("formula_hash", formulaHash);
                                        values.put("input_hash", inputHash);
                                        values.put("frozen_at", frozenAt);
                                        values.put("record_hash", recordHash);
                                        long row =
                                                db.insertWithOnConflict(
                                                        DbHelper.TABLE_R817_RESEARCH_PREDRAW,
                                                        null,
                                                        values,
                                                        SQLiteDatabase.CONFLICT_IGNORE);
                                        if (row >= 0L) return Boolean.TRUE;
                                        Cursor existing = null;
                                        try {
                                            existing =
                                                    db.rawQuery(
                                                            "SELECT record_hash FROM "
                                                                    + DbHelper.TABLE_R817_RESEARCH_PREDRAW
                                                                    + " WHERE candidate_id=? AND target_issue=? LIMIT 1",
                                                            new String[] {
                                                                candidate.candidateId, targetIssue
                                                            });
                                            return Boolean.valueOf(
                                                    existing.moveToFirst()
                                                            && recordHash.equals(
                                                                    safe(existing.getString(0))));
                                        } finally {
                                            if (existing != null) existing.close();
                                        }
                                    }
                                },
                                Boolean.FALSE);
        boolean success = inserted != null && inserted.booleanValue();
        if (success) {
            Map<String, String> state = new LinkedHashMap<String, String>();
            state.put("r817_research_predraw_status", "FROZEN_SHADOW_ONLY");
            state.put("r817_research_predraw_candidate", candidate.candidateId);
            state.put("r817_research_predraw_target", targetIssue);
            state.put("r817_research_predraw_at", String.valueOf(frozenAt));
            state.put(
                    "r817_research_adaptive_status",
                    adaptive.enabled() ? "APPLIED_PIT_FTRL" : "FORMULA_ONLY_NO_PRIOR_ARTIFACT");
            state.put("r817_research_adaptive_artifact_hash", adaptive.artifactHash);
            state.put("r817_research_adaptive_trained_until", adaptive.trainedUntil);
            state.put("r817_research_adaptive_updates", String.valueOf(adaptive.updates));
            state.put("r817_research_adaptive_weight", Double.toString(adaptive.weight));
            // The frozen prediction's writer has completed; preserve its result
            // even if the separate display batch cannot acquire the writer.
            store.trySetTelemetryStatesR77(state);
        }
        return new Result(
                success,
                success ? "FROZEN_SHADOW_ONLY" : "WRITE_FAILED",
                candidate.candidateId,
                targetIssue,
                recordHash,
                selectedOrdinal,
                probability);
    }

    /** Called only by the settlement coordinator after its sole Production owner is terminal. */
    public static int settle(final DbStore store, final Models.Draw draw) {
        if (store == null || draw == null || !draw.isValid()) return 0;
        if (!store.coordinatorR36().isR811ResearchSchemaReady()) return 0;
        Integer changed =
                store.coordinatorR36()
                        .runWrite(
                                "R817_RESEARCH_PREDRAW_SETTLE",
                                new DatabaseCoordinator.WriteCallable<Integer>() {
                                    @Override
                                    public Integer call(SQLiteDatabase db) {
                                        Cursor cursor = null;
                                        int count = 0;
                                        try {
                                            cursor =
                                                    db.rawQuery(
                                                            "SELECT candidate_id,position,n1,n2,n3,selected_ordinal,score,probability,record_hash"
                                                                    + " FROM "
                                                                    + DbHelper.TABLE_R817_RESEARCH_PREDRAW
                                                                    + " WHERE target_issue=? AND hit_label=-1"
                                                                    + " ORDER BY candidate_id ASC",
                                                            new String[] {draw.issue});
                                            while (cursor.moveToNext()) {
                                                ResearchWorkBudgetR927.checkpoint();
                                                boolean owns=!db.inTransaction();if(owns)db.beginTransaction();
                                                try(Cursor row=db.rawQuery("SELECT candidate_id,position,n1,n2,n3,selected_ordinal,score,probability,record_hash FROM "+DbHelper.TABLE_R817_RESEARCH_PREDRAW+" WHERE candidate_id=? AND target_issue=? AND hit_label=-1",new String[]{cursor.getString(0),draw.issue})) {
                                                if(!row.moveToFirst())continue;
                                                String candidateId = safe(row.getString(0));
                                                boolean historicalOnlyR79=candidateId.startsWith("R908-ALG-")
                                                        &&AlgorithmProofRecoveryR948.historicalOutcomeOnlyR79(db,candidateId,draw.issue);
                                                LegacySettlementProofR79.Original original=null;
                                                String proofReason="VALID";
                                                if(candidateId.startsWith("R908-ALG-")) {
                                                    original=LegacySettlementProofR79.original(db,"algorithm:"+candidateId,draw.issue);
                                                    proofReason=LegacySettlementProofR79.verifyAlgorithmSettlement(db,candidateId,draw,original);
                                                }
                                                if (candidateId.startsWith("R908-ALG-") && ((!LegacySettlementProofR79.currentAlgorithmEligible(db,candidateId,draw.issue)&&!historicalOnlyR79)
                                                        || !"VALID".equals(proofReason))) {
                                                    LegacySettlementProofR79.record(db,original,draw,"MIRROR_REJECTED",
                                                            "VALID".equals(proofReason)?"CURRENT_HEAD_NOT_QUALIFIED":proofReason);
                                                    AlgorithmProofRecoveryR948.quarantine(db,candidateId,draw.issue,"MIRROR_SETTLEMENT_REJECTED_UNQUALIFIED_CAPTURE");
                                                    if(owns)db.setTransactionSuccessful();
                                                    continue;
                                                }
                                                int position = row.getInt(1);
                                                int n1 = row.getInt(2);
                                                int n2 = row.getInt(3);
                                                int n3 = row.getInt(4);
                                                int ordinal = row.getInt(5);
                                                double score = finite(row.getDouble(6));
                                                double storedP=row.getDouble(7);
                                                double p=Double.isFinite(storedP)&&storedP>=0&&storedP<=1?storedP:-1d;
                                                String predrawHash = safe(row.getString(8));
                                                if (!AppConfig.isValidPosition(position)
                                                        || !DecisionGateEngine.b12K3(n1, n2, n3))
                                                    continue;
                                                int actual = draw.getNumberAtPosition(position);
                                                int hit =
                                                        actual == n1 || actual == n2 || actual == n3
                                                                ? 1
                                                                : 0;
                                                long settledAt = System.currentTimeMillis();
                                                String outcomeHash =
                                                        V51ChecksumEngine.sha256(
                                                                "R817_OUTCOME|"
                                                                        + predrawHash
                                                                        + '|'
                                                                        + actual
                                                                        + '|'
                                                                        + hit);
                                                ContentValues update = new ContentValues();
                                                update.put("actual_number", actual);
                                                update.put("hit_label", hit);
                                                update.put("settled_at", settledAt);
                                                update.put("outcome_hash", outcomeHash);
                                                int n =
                                                        db.update(
                                                                DbHelper.TABLE_R817_RESEARCH_PREDRAW,
                                                                update,
                                                                "candidate_id=? AND target_issue=? AND hit_label=-1",
                                                                new String[] {
                                                                    candidateId, draw.issue
                                                                });
                                                if (n != 1) continue;
                                                if(original!=null)LegacySettlementProofR79.record(db,original,draw,
                                                        historicalOnlyR79?"MIRROR_HISTORICAL_OUTCOME_ONLY":"MIRROR_CURRENT_HEAD","VALID");
                                                if(historicalOnlyR79) {
                                                    // The mirrored historical label cannot enter
                                                    // the current candidate's training outcome stream.
                                                    if(owns)db.setTransactionSuccessful();count++;continue;
                                                }
                                                ContentValues outcome = new ContentValues();
                                                outcome.put("candidate_id", candidateId);
                                                outcome.put("target_issue", draw.issue);
                                                outcome.put("selected_ordinal", ordinal);
                                                outcome.put("score", score);
                                                outcome.put("probability", p);
                                                outcome.put("hit_label", hit);
                                                outcome.put("regime_id", "FUTURE_LIVE_R817");
                                                outcome.put("ablated_feature", "NONE");
                                                outcome.put("ablated_ordinal", ordinal);
                                                outcome.put("ablated_score", score);
                                                outcome.put("ablated_probability", p);
                                                outcome.put("ablated_hit_label", hit);
                                                outcome.put("ablated_regime_id", "FUTURE_LIVE_R817");
                                                outcome.put("latency_ms", 0.0d);
                                                outcome.put("ram_peak_bytes", 0L);
                                                outcome.put("prediction_hash", predrawHash);
                                                outcome.put("created_at", settledAt);
                                                db.insertWithOnConflict(
                                                        "v57_research_candidate_outcome_r811",
                                                        null,
                                                        outcome,
                                                        SQLiteDatabase.CONFLICT_IGNORE);
                                                if(owns)db.setTransactionSuccessful();count++;
                                                } finally {if(owns)db.endTransaction();}
                                            }
                                        } finally {
                                            if (cursor != null) cursor.close();
                                        }
                                        return Integer.valueOf(count);
                                    }
                                },
                                Integer.valueOf(0));
        int count = changed == null ? 0 : changed.intValue();
        if (count > 0) {
            store.setState("r817_research_settlement_issue", draw.issue);
            store.setState("r817_research_settlement_rows", String.valueOf(count));
            store.setState(
                    "r817_research_settlement_at", String.valueOf(System.currentTimeMillis()));
        }
        return count;
    }

    private static Candidate nextCandidate(DbStore store, String targetIssue) {
        Cursor cursor = null;
        try {
            cursor =
                    store.readable()
                            .rawQuery(
                                    "SELECT c.candidate_id,c.artifact_hash,c.canonical_spec,c.status FROM"
                                            + " v57_research_candidate_r811 c LEFT JOIN "
                                            + DbHelper.TABLE_R817_RESEARCH_PREDRAW
                                            + " p ON p.candidate_id=c.candidate_id WHERE c.candidate_type='FORMULA'"
                                            + " AND c.runtime_executable=1 AND c.status IN"
                                            + " ('EXPLORATORY_COLLECT_ONLY','RESEARCH_READY','SHADOW_CHALLENGER','ACTIVE_OBSERVE_R900','KEEP_ACTIVE_R900')"
                                            + " AND NOT EXISTS(SELECT 1 FROM "
                                            + DbHelper.TABLE_R817_RESEARCH_PREDRAW
                                            + " x WHERE x.candidate_id=c.candidate_id AND x.target_issue=?)"
                                            + " GROUP BY c.candidate_id,c.artifact_hash,c.canonical_spec,c.status"
                                            + " ORDER BY CASE c.status WHEN 'SHADOW_CHALLENGER' THEN 0"
                                            + " WHEN 'KEEP_ACTIVE_R900' THEN 1 WHEN 'RESEARCH_READY' THEN 2"
                                            + " WHEN 'ACTIVE_OBSERVE_R900' THEN 3 ELSE 4 END,"
                                            + " COUNT(p.target_issue) ASC,c.updated_at ASC,c.candidate_id ASC LIMIT 1",
                                    new String[] {safe(targetIssue)});
            return cursor.moveToFirst()
                    ? new Candidate(
                            cursor.getString(0),
                            cursor.getString(1),
                            cursor.getString(2),
                            cursor.getString(3))
                    : null;
        } catch (Throwable ignored) {
            return null;
        } finally {
            if (cursor != null) cursor.close();
        }
    }

    private static double meta(double[] values, int index) {
        return values != null && index >= 0 && index < values.length
                ? finite(values[index])
                : 0.0d;
    }

    private static AdaptiveContext adaptiveForTarget(DbStore store, String targetIssue) {
        try {
            ResearchArtifactCodecR811.Artifact artifact =
                    ResearchAdaptiveLearnerStoreR811.loadForTarget(store, targetIssue);
            if (artifact == null) return AdaptiveContext.none();
            if (!"FTRL_SHADOW".equals(artifact.type)
                    || !TargetIssueResolver.isValid(artifact.trainedUntilIssue)
                    || TargetIssueResolver.compare(artifact.trainedUntilIssue, targetIssue) >= 0)
                return AdaptiveContext.none();
            double[] metadata = artifact.fields.get("meta");
            long updates =
                    metadata == null || metadata.length == 0
                            ? 0L
                            : Math.max(0L, (long) finite(metadata[0]));
            AdaptiveShadowLearnerEngine learner = AdaptiveShadowLearnerEngine.restore(artifact);
            return new AdaptiveContext(
                    learner,
                    ResearchArtifactCodecR811.hash(artifact),
                    artifact.trainedUntilIssue,
                    updates,
                    adaptiveWeight(updates));
        } catch (Throwable invalidArtifact) {
            if (store != null)
                store.setState(
                        "r817_research_adaptive_status",
                        "ARTIFACT_REJECTED_" + invalidArtifact.getClass().getSimpleName());
            return AdaptiveContext.none();
        }
    }

    static double adaptiveWeight(long updates) {
        if (updates <= 0L) return 0.0d;
        return 0.35d * (Math.min(120L, updates) / 120.0d);
    }

    static double blend(double formulaProbability, double adaptiveProbability, double weight) {
        double base = probability(formulaProbability);
        double learned = probability(adaptiveProbability);
        double boundedWeight = Math.max(0.0d, Math.min(0.35d, finite(weight)));
        return probability((1.0d - boundedWeight) * base + boundedWeight * learned);
    }

    private static double probability(double score) {
        double value = finite(score);
        if (value >= 0.0d && value <= 1.0d) return value;
        if (value >= 0.0d)
            return 1.0d / (1.0d + Math.exp(-Math.min(60.0d, value)));
        double e = Math.exp(Math.max(-60.0d, value));
        return e / (1.0d + e);
    }

    private static double finite(double value) {
        return Double.isNaN(value) || Double.isInfinite(value) ? 0.0d : value;
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class Candidate {
        final String candidateId, candidateHash, formula, status;

        Candidate(String id, String hash, String formula, String status) {
            candidateId = safe(id);
            candidateHash = safe(hash);
            this.formula = safe(formula);
            this.status = safe(status);
        }
    }

    private static final class AdaptiveContext {
        final AdaptiveShadowLearnerEngine learner;
        final String artifactHash, trainedUntil;
        final long updates;
        final double weight;

        AdaptiveContext(
                AdaptiveShadowLearnerEngine learner,
                String artifactHash,
                String trainedUntil,
                long updates,
                double weight) {
            this.learner = learner;
            this.artifactHash = safe(artifactHash);
            this.trainedUntil = safe(trainedUntil);
            this.updates = Math.max(0L, updates);
            this.weight = Math.max(0.0d, Math.min(0.35d, finite(weight)));
        }

        static AdaptiveContext none() {
            return new AdaptiveContext(null, "", "", 0L, 0.0d);
        }

        boolean enabled() {
            return learner != null && artifactHash.length() == 64 && weight > 0.0d;
        }
    }

    private static final class FeatureView implements ResearchStrategyDslEngine.FeatureProvider {
        V51Models.PositionAction action;

        void bind(V51Models.PositionAction value) {
            action = value;
        }

        @Override
        public double value(String name) {
            if (action == null) return 0.0d;
            if ("rank_score".equals(name)) return action.rankScore;
            if ("raw_action_p".equals(name)) return action.rawActionP;
            if ("third_margin".equals(name)) return action.thirdMargin;
            if ("entropy".equals(name)) return action.entropy;
            if ("stability".equals(name)) return action.stability;
            if ("assignment_support".equals(name)) return meta(action.metaFeatures, 15);
            if ("window_consensus".equals(name)) return meta(action.metaFeatures, 20);
            return 0.0d;
        }
    }

    public static final class Result {
        public final boolean success;
        public final String status, candidateId, targetIssue, recordHash;
        public final int selectedOrdinal;
        public final double probability;

        Result(
                boolean success,
                String status,
                String candidateId,
                String targetIssue,
                String recordHash,
                int selectedOrdinal,
                double probability) {
            this.success = success;
            this.status = safe(status);
            this.candidateId = safe(candidateId);
            this.targetIssue = safe(targetIssue);
            this.recordHash = safe(recordHash);
            this.selectedOrdinal = selectedOrdinal;
            this.probability = probability;
        }

        static Result skip(String status) {
            return new Result(false, status, "", "", "", -1, 0.0d);
        }
    }

    public static final class BatchResult {
        public final boolean success;
        public final String status, candidateIds;
        public final int frozen;

        BatchResult(boolean success, String status, int frozen, String candidateIds) {
            this.success = success;
            this.status = safe(status);
            this.frozen = Math.max(0, frozen);
            this.candidateIds = safe(candidateIds);
        }
    }
}
