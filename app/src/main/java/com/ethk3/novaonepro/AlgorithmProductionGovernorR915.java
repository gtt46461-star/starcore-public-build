package com.ethk3.novaonepro;

import android.database.Cursor;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;

/**
 * Production-side consumer for one evidence-qualified invented algorithm.
 *
 * <p>The invention laboratory never receives REAL authority.  This governor may only combine the
 * frozen Production 1200-action surface with one {@code CHALLENGER_FUTURE_OOS} genome whose
 * chronological evidence already passed hit-rate, Wilson, block/regime stability, Brier, ECE,
 * coverage, paired-mainline and max-miss gates.  The resulting action must still pass
 * {@link V51CandidateRealGateR915} and the sole {@link DecisionGateEngine}; this class has no Trade
 * writer and no authorization method.</p>
 */
public final class AlgorithmProductionGovernorR915 {
    public static final String VERSION = "R915_ALGORITHM_PRODUCTION_INFLUENCE_V1";
    public static final boolean MAY_AUTHORIZE_REAL = false;
    private static final int REQUIRED_FUTURE_N = ResearchGenomeContractR915.MIN_CERTIFICATE_N;
    private static final int REQUIRED_PAIRED_N = ResearchGenomeContractR915.MIN_PAIRED_N;
    private static final double MIN_FUTURE_RATE = CertificateBundleServiceR929.MIN_ENTRY_HIT_RATE;
    private static final double MIN_WILSON = 0.30d;
    private static final double MIN_BLOCK = 0.25d;
    private static final double MIN_REGIME = 0.22d;
    private static final double MAX_BRIER = CertificateBundleServiceR929.MAX_ENTRY_BRIER;
    private static final double MAX_ECE = 0.12d;
    private static final double MIN_COVERAGE = 0.80d;
    private static final double MIN_PAIRED_DELTA = 0.02d;
    private static final double MAX_FDR_Q = 0.20d;
    private static final int MAX_MISS = 8;

    public static final class Result {
        public final boolean applied;
        public final String frozenRecipe;
        public final V51Models.PositionAction[] actions;
        public final V51Models.PositionAction rankOne;
        public final String status, genomeId, nameZh, evidenceHash, certificateHash, mechanismHash;
        public final int futureN, pairedN;
        public final double futureRate, wilson, brier, ece, coverage;
        public final double pairedDelta, fdrQ, influenceWeight;

        Result(
                boolean applied,
                V51Models.PositionAction[] actions,
                V51Models.PositionAction rankOne,
                String status,
                Candidate candidate,
                double weight) {
            this(applied,actions,rankOne,status,candidate,weight,"");
        }
        Result(boolean applied,V51Models.PositionAction[] actions,V51Models.PositionAction rankOne,String status,Candidate candidate,double weight,String frozenRecipe) {
            this.frozenRecipe=frozenRecipe;
            this.applied = applied;
            this.actions = actions;
            this.rankOne = rankOne;
            this.status = safe(status);
            genomeId = candidate == null ? "" : candidate.genomeId;
            nameZh = candidate == null ? "" : candidate.nameZh;
            evidenceHash = candidate == null ? "" : candidate.evidenceHash;
            certificateHash = candidate == null ? "" : candidate.certificateHash;
            mechanismHash = candidate == null ? "" : candidate.structuralHash;
            futureN = candidate == null ? 0 : candidate.futureN;
            pairedN = candidate == null ? 0 : candidate.pairedN;
            futureRate = candidate == null ? 0.0d : candidate.futureRate;
            wilson = candidate == null ? 0.0d : candidate.wilson;
            brier = candidate == null ? 1.0d : candidate.brier;
            ece = candidate == null ? 1.0d : candidate.ece;
            coverage = candidate == null ? 0.0d : candidate.coverage;
            pairedDelta = candidate == null ? 0.0d : candidate.pairedDelta;
            fdrQ = candidate == null ? 1.0d : candidate.fdrQ;
            influenceWeight = weight;
        }

        public boolean controlledInfluenceEligible() {
            return applied
                    && "CERTIFIED_ALGORITHM_INFLUENCE".equals(status)
                    && evidenceHash.length() == 64
                    && certificateHash.length() == 64
                    && mechanismHash.length() == 64
                    && futureN >= REQUIRED_FUTURE_N
                    && pairedN >= REQUIRED_PAIRED_N
                    && fdrQ <= MAX_FDR_Q;
        }

        public String proof() {
            return status
                    + "|id=" + genomeId
                    + "|name=" + nameZh
                    + "|futureN=" + futureN
                    + "|rate=" + futureRate
                    + "|wilson=" + wilson
                    + "|brier=" + brier
                    + "|ece=" + ece
                    + "|coverage=" + coverage
                    + "|pairedN=" + pairedN
                    + "|delta=" + pairedDelta
                    + "|FWER_adjusted_p=" + fdrQ
                    + "|weight=" + influenceWeight
                    + "|evidence=" + evidenceHash
                    + "|certificate=" + certificateHash;
        }
    }

    private AlgorithmProductionGovernorR915() {}

    public static Result skippedForAsset(V51Models.PositionAction[] actions){
        return fallback(null,actions,"SKIP_CERTIFIED_ASSET_PRIORITY",null);
    }

    public static Result govern(
            DbStore store,
            String baseIssue,
            String targetIssue,
            V51Models.PositionAction[] productionActions) {
        if (store == null
                || !TargetIssueResolver.exact(baseIssue, targetIssue).valid
                || productionActions == null
                || productionActions.length != K3ActionSpaceEngine.TOTAL_ACTIONS) {
            return fallback(store, productionActions, "INPUT_INVALID", null);
        }
        if (MAY_AUTHORIZE_REAL || AlgorithmInventionLabR908.MAY_AUTHORIZE_REAL)
            throw new IllegalStateException("R915_ALGORITHM_GOVERNOR_AUTHORITY_VIOLATION");
        UnifiedResearchChoiceR932.Evidence frozenEvidence=UnifiedResearchChoiceR932.selected(store,targetIssue,CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM);
        if(frozenEvidence!=null&&frozenEvidence.id.startsWith(FrozenCandidateAdapterR958.PREFIX))
            return governFrozen(store,baseIssue,targetIssue,productionActions,frozenEvidence);
        Candidate candidate = loadBest(store, targetIssue);
        if (candidate == null)
            return fallback(store, productionActions, "NO_CERTIFIED_ALGORITHM", null);
        final PromotionWindowR925.Window window;
        try{window=PromotionWindowR925.authorize(store,CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM,candidate.genomeId,candidate.structuralHash,targetIssue);}
        catch(Throwable unavailable){return fallback(store,productionActions,"R925_WINDOW_READ_ERROR_"+unavailable.getClass().getSimpleName(),candidate);}
        if (window.weight<=0) return fallback(store,productionActions,window.summary(),candidate);
        HistoricalStrategyInventionR908.Profile profile =
                HistoricalStrategyInventionR908.buildProfileAt(store, baseIssue);
        if (profile == null || !profile.ready)
            return fallback(store, productionActions, "PIT_PROFILE_NOT_READY", candidate);
        try {
            final double weight=window.weight;
            FrozenConsumerRecipeR932.Recipe recipe=FrozenConsumerRecipeR932.freeze("INVENTED_ALGORITHM_CERTIFIED",candidate.genomeId,candidate.genomeSpec,
                    AlgorithmGenomeVmR908.State.decode(candidate.stateBlob).encode(),candidate.structuralHash,candidate.evidenceHash,candidate.certificateHash,weight,baseIssue,targetIssue,profile,null);
            FrozenConsumerRecipeR932.Mixed mixed=FrozenConsumerRecipeR932.execute(recipe,productionActions);
            V51Models.PositionAction[] integrated=mixed.actions;V51Models.PositionAction rankOne=mixed.rankOne;
            Result result = new Result(
                    true,
                    integrated,
                    rankOne,
                    "CERTIFIED_ALGORITHM_INFLUENCE",
                    candidate,
                    weight,recipe.encoded);
            persistTruth(store, result);
            return result;
        } catch (Throwable failure) {
            return fallback(
                    store,
                    productionActions,
                    "ALGORITHM_RUNTIME_FAIL_" + failure.getClass().getSimpleName(),
                    candidate);
        }
    }

    private static Result governFrozen(DbStore store,String base,String target,V51Models.PositionAction[] actions,UnifiedResearchChoiceR932.Evidence e) {
        try {
            FrozenCandidateAdapterR958.Frozen f=FrozenCandidateAdapterR958.load(store.readable(),e.id);
            if(f==null||!ResearchEvaluationRunR967.mayCapture(store.readable(),f,target)||!f.structure.equals(e.structure)||f.trainedUntil.compareTo(target)>=0||f.createdIssue.compareTo(target)>0)
                return fallback(store,actions,"FROZEN_MODEL_IDENTITY_OR_PIT_REJECTED",null);
            PromotionWindowR925.Window window=PromotionWindowR925.authorize(store,CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM,f.id,f.structure,target);
            if(window.weight<=0)return fallback(store,actions,window.summary(),null);
            Candidate candidate=new Candidate(f.id,"冻结频率残差研究候选",f.ast.canonical,f.model.payload(),f.structure,f.structure,e.evidence,e.certificate,f.trainedUntil,f.createdIssue,
                e.count("FUTURE_N"),e.metric("HIT_RATE"),e.metric("WILSON_LOWER"),Double.NaN,Double.NaN,e.metric("BRIER"),e.metric("ECE"),e.metric("COVERAGE"),0,e.count("PAIRED_N"),e.metric("PAIRED_DELTA"),e.metric("FWER_ADJUSTED_P"),"R958_FIXED_POSITION_FULL1200",0);
            FrozenConsumerRecipeR932.Recipe recipe=FrozenCandidateAdapterR958.recipe(store,f,e,window.weight,base,target);
            FrozenConsumerRecipeR932.Mixed mixed=FrozenConsumerRecipeR932.execute(recipe,actions);
            Result result=new Result(true,mixed.actions,mixed.rankOne,"CERTIFIED_ALGORITHM_INFLUENCE",candidate,window.weight,recipe.encoded);
            persistTruth(store,result);return result;
        } catch(ResearchWorkBudgetR927.Yield yielded) {throw yielded;}
          catch(Exception invalid) {return fallback(store,actions,"FROZEN_NUMERIC_CONSUMER_REJECTED_"+invalid.getClass().getSimpleName(),null);}
    }

    private static Candidate loadBest(DbStore store, String target) {
        UnifiedResearchChoiceR932.Evidence e=UnifiedResearchChoiceR932.selected(store,target,CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM);
        if(e==null)return null;
        try(Cursor c=store.readable().rawQuery("SELECT genome_id,display_name_zh,genome_spec,state_blob,structural_hash,behavior_hash,trained_until_issue,earliest_use_issue,future_block_min,future_regime_min,future_max_miss,research_genome_contract,may_authorize_real FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME+" WHERE genome_id=? AND structural_hash=?",new String[]{e.id,e.structure})){
            if(!c.moveToFirst())return null;
            Candidate candidate=new Candidate(c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4),c.getString(5),e.evidence,e.certificate,c.getString(6),c.getString(7),
                e.count("FUTURE_N"),e.metric("HIT_RATE"),e.metric("WILSON_LOWER"),c.getDouble(8),c.getDouble(9),e.metric("BRIER"),e.metric("ECE"),e.metric("COVERAGE"),c.getInt(10),e.count("PAIRED_N"),e.metric("PAIRED_DELTA"),e.metric("FWER_ADJUSTED_P"),c.getString(11),c.getInt(12));
            return candidate.validFor(target)?candidate:null;
        }catch(RuntimeException error){if(error instanceof ResearchWorkBudgetR927.Yield)throw error;return null;}
    }

    private static Result fallback(
            DbStore store,
            V51Models.PositionAction[] actions,
            String status,
            Candidate candidate) {
        V51Models.PositionAction rankOne = null;
        if (actions != null) {
            for (V51Models.PositionAction action : actions) {
                if (action != null && action.rank == 1) {
                    rankOne = action;
                    break;
                }
            }
        }
        Result result = new Result(false, actions, rankOne, status, candidate, 0.0d);
        if (store != null) persistTruth(store, result);
        return result;
    }

    private static void persistTruth(DbStore store, Result result) {
        try {
            LinkedHashMap<String, String> state = new LinkedHashMap<String, String>();
            state.put("r915_algorithm_production_status", result.status);
            state.put("r915_algorithm_production_id", result.genomeId);
            state.put("r915_algorithm_production_name_zh", result.nameZh);
            state.put("r915_algorithm_production_evidence", result.evidenceHash);
            state.put("r915_algorithm_production_certificate", result.certificateHash);
            state.put("r915_algorithm_production_proof", result.proof());
            state.put("r915_algorithm_production_at", String.valueOf(System.currentTimeMillis()));
            store.setStates(state);
        } catch (Throwable ignored) {}
    }

    private static String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private static final class Candidate {
        final String genomeId, nameZh, genomeSpec, stateBlob, structuralHash, behaviorHash,
                evidenceHash, certificateHash, trainedUntil, earliestUse, researchContract;
        final int futureN, maxMiss, pairedN, mayAuthorizeReal;
        final double futureRate, wilson, blockMin, regimeMin, brier, ece, coverage, pairedDelta,
                fdrQ;

        Candidate(
                String id, String name, String spec, String state, String structural,
                String behavior, String evidence, String certificate, String trained, String earliest, int n,
                double rate, double lower, double block, double regime, double brier,
                double ece, double coverage, int maxMiss, int pairedN, double delta,
                double fdrQ, String researchContract, int mayAuthorizeReal) {
            genomeId = id; nameZh = name; genomeSpec = spec; stateBlob = state;
            structuralHash = structural; behaviorHash = behavior; evidenceHash = evidence;
            certificateHash = safe(certificate); trainedUntil = trained; earliestUse = earliest; futureN = n;
            futureRate = rate; wilson = lower; blockMin = block; regimeMin = regime;
            this.brier = brier; this.ece = ece; this.coverage = coverage;
            this.maxMiss = maxMiss; this.pairedN = pairedN; pairedDelta = delta;
            this.fdrQ = fdrQ; this.researchContract = safe(researchContract);
            this.mayAuthorizeReal = mayAuthorizeReal;
        }

        boolean validFor(String target) {
            if(genomeId.length()==0||structuralHash.length()!=64||behaviorHash.length()!=64||evidenceHash.length()!=64||certificateHash.length()!=64||mayAuthorizeReal!=0
                    ||futureRate<MIN_FUTURE_RATE||maxMiss>MAX_MISS||blockMin<MIN_BLOCK||regimeMin<MIN_REGIME
                    ||!TargetIssueResolver.isValid(trainedUntil)||TargetIssueResolver.compare(trainedUntil,target)>=0
                    ||(earliestUse.length()>0&&(!TargetIssueResolver.isValid(earliestUse)||TargetIssueResolver.compare(earliestUse,target)>0)))return false;
            try{AlgorithmGenomeR908 genome=AlgorithmGenomeR908.parse(genomeSpec);return genome.algorithmGrade()&&genome.genomeId.equals(genomeId)&&genome.structuralHash.equals(structuralHash)&&genome.chineseName().equals(nameZh);}
            catch(RuntimeException invalid){return false;}
        }

        private static boolean close(double left, double right) {
            return Math.abs(left - right) <= 1.0e-9d;
        }

        private static String twoDigitGeneration(String spec) {
            try {
                AlgorithmGenomeR908 genome = AlgorithmGenomeR908.parse(spec);
                return String.format(java.util.Locale.US, "%02d", genome.generation);
            } catch (Throwable failure) {
                return "--";
            }
        }
    }
}
