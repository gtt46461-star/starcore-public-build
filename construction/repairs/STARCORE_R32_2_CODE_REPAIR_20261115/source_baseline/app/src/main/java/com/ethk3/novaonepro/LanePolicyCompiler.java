package com.ethk3.novaonepro;

import java.util.HashMap;

/**
 * V46 single compiled Lane contract.
 *
 * <p>EventP estimates the event. ActionLCB expresses conservatism. Thirty issues only unlock
 * controlled PROBE/FLOOR evaluation; it never creates a frequency rescue. Global champion status is
 * diagnostic only in active V51; candidate-level OOS/risk contracts still decide eligibility.
 */
public final class LanePolicyCompiler {
    public static final String MODULE_VERSION = V49BuildManifest.LANE_SCHEMA;

    public static final class Result {
        public boolean eligible;
        public String lane = "SHADOW";
        public String code = "LANE_NOT_EVALUATED";
        public String primaryReject = "";
        public String secondaryReject = "";
        public String tertiaryReject = "";
        public double gate;
        public double edge;
        public double probability;
        public double actionLcb;
        public double uncertainty;
        public double familyLcb;
        public double acceptScore;
        public double counterRisk;
        public int startupSample;
        public int supportVotes;
        public int qualityVotes;
        public double coverage;
        public double thirdQuality;
        public int evidenceGroups;
        public int positiveGroups;
        public int compileCount;
        public String contractHash = "";
        public RegimeRegistryEngine.Result regime;
        public AssociationGraphEngine.Result association;
        public CounterfactualDistillationEngine.Result distillation;
        public RiskCoverageController.State riskCoverage;
        public EvidenceGroupAggregatorEngine.Snapshot evidence;

        public String summary() {
            return "lane="
                    + lane
                    + "|eligible="
                    + (eligible ? 1 : 0)
                    + "|code="
                    + safe(code)
                    + "|EventP="
                    + round(probability)
                    + "|ActionLCB="
                    + round(actionLcb)
                    + "|U="
                    + round(uncertainty)
                    + "|gate="
                    + round(gate)
                    + "|edge="
                    + round(edge)
                    + "|familyLCB="
                    + round(familyLcb)
                    + "|accept="
                    + round(acceptScore)
                    + "|startup="
                    + startupSample
                    + "|votes="
                    + supportVotes
                    + "/q="
                    + qualityVotes
                    + "|groups="
                    + evidenceGroups
                    + "/positive="
                    + positiveGroups
                    + "|cover="
                    + round(coverage)
                    + "|third="
                    + round(thirdQuality)
                    + "|compileCount="
                    + compileCount
                    + "|contract="
                    + shortHash(contractHash)
                    + "|reject="
                    + safe(primaryReject)
                    + "/"
                    + safe(secondaryReject)
                    + "/"
                    + safe(tertiaryReject);
        }
    }

    private static final class LaneAttempt {
        boolean pass;
        String lane;
        String[] failures = new String[20];
        int failureCount;
        int passCount;
        double gate;
        double minAccept;
    }

    private static final HashMap<String, Result> RESULT_CACHE = new HashMap<String, Result>();

    private LanePolicyCompiler() {}

    /**
     * Typed V50 path. Legacy Result/cache structures are intentionally bypassed; only the exported
     * selective-risk artifact owns the V50 lane.
     */
    public static V50SelectiveRiskController.Result compileV50(
            double selectedHitP,
            V50Models.PositionAction rankOne,
            V50Models.DomainCertificate domain,
            V50Models.EffectiveConsensus consensus,
            V50Models.UncertaintyBreakdown uncertainty,
            V50FalsifierExpert.Assessment falsifier) {
        return new V50SelectiveRiskController()
                .evaluate(selectedHitP, rankOne, domain, consensus, uncertainty, falsifier);
    }

    public static Result compile(
            DbStore store, Models.ComboCandidate c, ActionUtilityEngine.Result utility) {
        return compileInternal(store, c, utility, "", false);
    }

    public static Result compileForLane(
            DbStore store,
            Models.ComboCandidate c,
            ActionUtilityEngine.Result utility,
            String requiredLane) {
        return compileInternal(store, c, utility, normalizeLane(requiredLane), true);
    }

    public static synchronized Result getCompiledResult(
            Models.ComboCandidate c, String requiredLane) {
        if (c == null) return null;
        Result r = RESULT_CACHE.get(PredictionEnvelope.candidateKey(c));
        if (r == null || !r.eligible) return null;
        String required = normalizeLane(requiredLane);
        if (required.length() > 0 && !required.equals(r.lane)) return null;
        return r;
    }

    public static synchronized void clearIssue(String targetIssue) {
        String issue = safe(targetIssue);
        java.util.ArrayList<String> remove = new java.util.ArrayList<String>();
        // The sole decision owner starts a new target here. Clearing only that NEW
        // target left every completed issue in this process-static map forever.
        // Retire closed targets too; never evict a newer in-flight target.
        for (String key : RESULT_CACHE.keySet()) {
            int separator=key.indexOf('|');
            String cachedIssue=separator<0?"":key.substring(0,separator);
            if(key.startsWith(issue+"|")||(TargetIssueResolver.isValid(issue)
                    &&TargetIssueResolver.isValid(cachedIssue)&&cachedIssue.compareTo(issue)<0))remove.add(key);
        }
        for (int i = 0; i < remove.size(); i++) RESULT_CACHE.remove(remove.get(i));
        CompiledActionContract.clearIssue(issue);
    }

    private static Result compileInternal(
            DbStore store,
            Models.ComboCandidate c,
            ActionUtilityEngine.Result utility,
            String requiredLane,
            boolean cacheOnly) {
        Result cached = cached(c, requiredLane);
        if (cached != null) return cached;
        if (cacheOnly) return reject(new Result(), "LANE_V46_COMPILED_CONTRACT_MISSING");
        Result out = new Result();
        if (c == null) return reject(out, "LANE_INPUT_NULL");
        if (utility == null) utility = ActionUtilityEngine.evaluate(store, c);
        if (!c.isValidK3()) return reject(out, "LANE_ILLEGAL_K3");
        if (c.contractValid != 1
                || !PredictionContractEngine.STATUS_VALID.equals(
                        PredictionContractEngine.verifyCandidate(c)))
            return reject(out, "LANE_CONTRACT_INVALID");

        if (AppConfig.ENABLE_V49_FULL_MOE && safe(c.probabilityFieldVersion).indexOf("V49") >= 0) {
            return compileV49(store, c, utility, out);
        }

        String lockStatus = PipelineInvariantGuard.ensureFinalLock(c, "V48_LANE_COMPILE");
        PredictionLifecycleTraceEngine.record(store, c, "BEFORE_V48_LANE_COMPILE", lockStatus);
        if (!PipelineInvariantGuard.STATUS_LOCKED.equals(lockStatus))
            return publish(store, c, reject(out, "LANE_V46_" + machineCode(lockStatus)), utility);

        PredictionEnvelope envelope = PredictionEnvelope.readOnly(c);
        out.probability = envelope == null ? 0.0d : envelope.canonicalProbability;
        out.actionLcb = envelope == null ? 0.0d : envelope.actionLcb;
        out.uncertainty = envelope == null ? 1.0d : envelope.uncertainty;
        out.familyLcb = conservativeFamilyLcb(c);
        out.counterRisk = max01(c.counterRisk, c.counterEvidenceScore, c.evidenceRisk);
        out.regime = RegimeRegistryEngine.evaluate(c);
        out.association = AssociationGraphEngine.evaluate(c);
        out.distillation = CounterfactualDistillationEngine.evaluate(c);
        out.riskCoverage = RiskCoverageController.evaluate(store);
        out.coverage = candidateCoverage(c);
        out.thirdQuality = candidateThird(c);
        out.evidence =
                EvidenceGroupAggregatorEngine.evaluate(
                        c, utility, out.regime, out.association, out.distillation);
        out.evidenceGroups = out.evidence.availableGroups;
        out.positiveGroups = out.evidence.positiveGroups;
        out.startupSample = StartupContextEngine.resolve(store, c);
        String v48Champion =
                store == null
                        ? "MODEL_NOT_PROVEN"
                        : store.getState(
                                WalkForwardChampionEngine.STATE_STATUS, "MODEL_NOT_PROVEN");
        boolean globalChampionProven = "CHAMPION".equals(v48Champion);
        try {
            if (store != null) {
                store.setState("b07r3_global_champion_status", v48Champion);
                store.setState(
                        "b07r3_global_champion_role", "DIAGNOSTIC_NOT_ABSOLUTE_V51_LIVE_VETO");
                store.setState(
                        "b07r3_candidate_level_authorization",
                        "ENABLED_STRICT_CONTRACT_AND_RISK_GATES");
            }
        } catch (Throwable ignore) {
        }
        if (!globalChampionProven) {
            c.reasonSummary =
                    safe(c.reasonSummary)
                            + (safe(c.reasonSummary).length() > 0 ? "；" : "")
                            + "全局Champion尚未证明：不伪造MODEL_PROVEN；V51仅允许候选级严格OOS合同继续接受后续全部质量/风险门审判";
        }
        RuntimeFreshnessGuard.Result freshness =
                RuntimeFreshnessGuard.evaluate(store, c == null ? "" : c.targetIssue);
        if (!freshness.fresh)
            return publish(store, c, reject(out, AppConfig.V47_ROOT_RUNTIME), utility);
        out.supportVotes = supportVotes(c, utility, out);
        out.qualityVotes = qualityVotes(c, utility, out);
        out.acceptScore = acceptScore(c, utility, out);

        if (c.hardBlockFlag == 1 || safe(c.hardBlockSource).length() > 0)
            return publish(store, c, reject(out, "LANE_STRUCTURED_HARD_BLOCK"), utility);
        if (c.v43HardRisk == 1 || out.evidence.hardRisk == 1)
            return publish(store, c, reject(out, "LANE_V46_HARD_RISK"), utility);
        if (out.evidenceGroups < 4)
            return publish(store, c, reject(out, "LANE_V46_EVIDENCE_GROUPS_LOW"), utility);
        if (out.regime != null
                && (out.regime.hardChaos
                        || RegimeRegistryEngine.CHAOS_REJECT.equals(out.regime.family)))
            return publish(store, c, reject(out, "LANE_CHAOS_REGIME"), utility);
        if (out.distillation != null && out.distillation.hardRisk)
            return publish(store, c, reject(out, "LANE_COUNTERFACTUAL_HARD_RISK"), utility);
        if (out.association != null
                && out.association.conflictRisk >= AppConfig.V42_ASSOCIATION_HARD_CONFLICT)
            return publish(store, c, reject(out, "LANE_ASSOCIATION_HARD_CONFLICT"), utility);

        LaneAttempt normal = evaluateNormal(c, utility, out);
        LaneAttempt floor = evaluateFloor(c, utility, out);
        LaneAttempt probe = evaluateProbe(c, utility, out);
        if (normal.pass) return publish(store, c, accept(out, normal), utility);
        if (floor.pass) return publish(store, c, accept(out, floor), utility);
        if (probe.pass) return publish(store, c, accept(out, probe), utility);
        return publish(store, c, rejectFromAttempt(out, closest(normal, floor, probe)), utility);
    }

    private static Result compileV49(
            DbStore store,
            Models.ComboCandidate c,
            ActionUtilityEngine.Result utility,
            Result out) {
        out.probability = clamp01(c.v46EventProbability > 0.0d ? c.v46EventProbability : c.finalP);
        out.actionLcb = clamp01(c.v46ActionLcb > 0.0d ? c.v46ActionLcb : out.probability - 0.04d);
        out.uncertainty =
                clamp01(
                        c.v46ProbabilityUncertainty > 0.0d
                                ? c.v46ProbabilityUncertainty
                                : c.k3CoverageRisk);
        out.familyLcb = out.actionLcb;
        out.counterRisk = clamp01(Math.max(c.counterRisk, c.counterEvidenceScore));
        out.coverage = candidateCoverage(c);
        out.thirdQuality = candidateThird(c);
        out.startupSample = startupSample(c);
        out.acceptScore = clamp01(c.v43StrategySupport);
        out.supportVotes = 0;
        if (out.probability >= AppConfig.BREAK_EVEN_PROBABILITY) out.supportVotes++;
        if (out.coverage >= 0.45d) out.supportVotes++;
        if (out.thirdQuality >= 0.25d) out.supportVotes++;
        if (c.v43StrategyMaturity >= 0.45d) out.supportVotes++;
        if (c.driftRisk <= AppConfig.V49_MAX_SWITCH_RISK) out.supportVotes++;
        out.qualityVotes = out.supportVotes;
        out.evidenceGroups = 5;
        out.positiveGroups = out.supportVotes;

        String family = safe(c.stateKey);
        String lifecycle = "";
        int bar = family.indexOf('|');
        if (bar >= 0) {
            int next = family.indexOf('|', bar + 1);
            lifecycle = next < 0 ? family.substring(bar + 1) : family.substring(bar + 1, next);
            family = family.substring(0, bar);
        }
        V49LanePolicyEngine.Result v49 =
                new V49LanePolicyEngine()
                        .compile(
                                store,
                                out.probability,
                                out.acceptScore,
                                out.coverage,
                                out.uncertainty,
                                c.driftRisk,
                                c.v43StrategyMaturity,
                                family,
                                lifecycle);
        String semanticLane = v49 == null ? V49LanePolicyEngine.SHADOW : v49.lane;
        c.v46Lane = semanticLane;
        if (v49 == null || !v49.eligible) {
            out.gate = v49 == null ? AppConfig.V49_DYNAMIC_MIN_ACTION_P : v49.gate;
            out.edge = out.probability - out.gate;
            return publish(
                    store, c, reject(out, v49 == null ? "V49_LANE_NULL" : v49.code), utility);
        }
        out.eligible = true;
        if (V49LanePolicyEngine.CORE.equals(semanticLane)) out.lane = AppConfig.BET_DOMAIN_NORMAL;
        else if (V49LanePolicyEngine.DYNAMIC.equals(semanticLane))
            out.lane = AppConfig.BET_DOMAIN_QUALIFIED_FLOOR;
        else out.lane = AppConfig.BET_DOMAIN_REAL_BET_PROBE;
        out.code = v49.code;
        out.gate = v49.gate;
        out.edge = out.probability - out.gate;
        out.primaryReject = "";
        out.secondaryReject = "";
        out.tertiaryReject = "";
        return publish(store, c, out, utility);
    }

    private static LaneAttempt evaluateNormal(
            Models.ComboCandidate c, ActionUtilityEngine.Result u, Result r) {
        LaneAttempt a =
                attempt(
                        AppConfig.BET_DOMAIN_NORMAL,
                        AppConfig.V46_NORMAL_GATE,
                        r.riskCoverage == null
                                ? AppConfig.V42_NORMAL_ACCEPT_MIN
                                : r.riskCoverage.normalAcceptMin);
        require(a, r.startupSample >= AppConfig.V46_NORMAL_START_MIN_ISSUES, "NORMAL_STARTUP_LT30");
        require(a, r.riskCoverage == null || r.riskCoverage.allowNormal, "NORMAL_RC_DISABLED");
        require(a, r.probability >= AppConfig.V46_NORMAL_MIN_EVENT_P, "NORMAL_EVENT_P_LOW");
        require(a, r.actionLcb >= AppConfig.V46_NORMAL_MIN_ACTION_LCB, "NORMAL_ACTION_LCB_LOW");
        require(
                a,
                r.probability - a.gate >= -AppConfig.CONTRACT_PROBABILITY_EPSILON,
                "NORMAL_EDGE_LOW");
        require(a, r.positiveGroups >= 3, "NORMAL_GROUP_QUORUM_LOW");
        require(a, r.coverage >= AppConfig.V46_MIN_COVERAGE, "NORMAL_COVERAGE_LOW");
        require(a, r.thirdQuality >= AppConfig.V46_MIN_THIRD_QUALITY, "NORMAL_THIRD_LOW");
        require(a, r.counterRisk <= AppConfig.V42_NORMAL_MAX_COUNTER, "NORMAL_COUNTER_HIGH");
        require(
                a,
                r.uncertainty <= AppConfig.V42_NORMAL_MAX_UNCERTAINTY,
                "NORMAL_UNCERTAINTY_HIGH");
        require(a, r.acceptScore >= a.minAccept, "NORMAL_ACCEPT_SCORE_LOW");
        a.pass = a.failureCount == 0;
        return a;
    }

    private static LaneAttempt evaluateFloor(
            Models.ComboCandidate c, ActionUtilityEngine.Result u, Result r) {
        LaneAttempt a =
                attempt(
                        AppConfig.BET_DOMAIN_QUALIFIED_FLOOR,
                        AppConfig.V46_FLOOR_GATE,
                        Math.min(
                                r.riskCoverage == null
                                        ? AppConfig.V42_FLOOR_ACCEPT_MIN
                                        : r.riskCoverage.floorAcceptMin,
                                0.440d));
        require(
                a,
                r.startupSample >= AppConfig.V46_CONTROLLED_START_MIN_ISSUES,
                "FLOOR_STARTUP_LT30");
        require(a, r.riskCoverage == null || r.riskCoverage.allowFloor, "FLOOR_RC_DISABLED");
        require(a, r.probability >= AppConfig.V46_FLOOR_MIN_EVENT_P, "FLOOR_EVENT_P_LOW");
        require(a, r.actionLcb >= AppConfig.V46_FLOOR_MIN_ACTION_LCB, "FLOOR_ACTION_LCB_LOW");
        require(a, modelProbability(c) >= AppConfig.V46_FLOOR_MIN_EVENT_P, "FLOOR_MODEL_P_LOW");
        require(a, r.probability - a.gate >= -0.005d, "FLOOR_EDGE_LOW");
        require(a, r.positiveGroups >= 3, "FLOOR_GROUP_QUORUM_LOW");
        require(a, r.coverage >= AppConfig.V46_MIN_COVERAGE, "FLOOR_COVERAGE_LOW");
        require(a, r.thirdQuality >= AppConfig.V46_MIN_THIRD_QUALITY, "FLOOR_THIRD_LOW");
        require(a, r.uncertainty <= AppConfig.V46_MAX_UNCERTAINTY_FLOOR, "FLOOR_UNCERTAINTY_HIGH");
        require(a, r.counterRisk <= AppConfig.V46_MAX_COUNTER_RISK, "FLOOR_COUNTER_HIGH");
        require(a, r.qualityVotes >= AppConfig.V46_MIN_QUALITY_VOTES, "FLOOR_QUALITY_QUORUM_LOW");
        require(a, r.acceptScore >= a.minAccept, "FLOOR_ACCEPT_SCORE_LOW");
        a.pass = a.failureCount == 0;
        return a;
    }

    private static LaneAttempt evaluateProbe(
            Models.ComboCandidate c, ActionUtilityEngine.Result u, Result r) {
        LaneAttempt a =
                attempt(
                        AppConfig.BET_DOMAIN_REAL_BET_PROBE,
                        AppConfig.V46_PROBE_GATE,
                        Math.min(
                                r.riskCoverage == null
                                        ? AppConfig.V42_PROBE_ACCEPT_MIN
                                        : r.riskCoverage.probeAcceptMin,
                                0.405d));
        LiveActionBudgetEngine.State budget = LiveActionBudgetEngine.evaluate(r.riskCoverage);
        require(
                a,
                r.startupSample >= AppConfig.V46_CONTROLLED_START_MIN_ISSUES,
                "PROBE_STARTUP_LT30");
        require(a, r.riskCoverage == null || r.riskCoverage.allowProbe, "PROBE_RC_DISABLED");
        require(a, budget.allowed, safe(budget.code));
        require(a, r.probability >= AppConfig.V46_PROBE_MIN_EVENT_P, "PROBE_EVENT_P_LOW");
        require(a, r.actionLcb >= AppConfig.V46_PROBE_MIN_ACTION_LCB, "PROBE_ACTION_LCB_LOW");
        require(a, modelProbability(c) >= AppConfig.V46_PROBE_MIN_MODEL_P, "PROBE_MODEL_P_LOW");
        require(a, RankingAuditEngine.expectedValue(r.probability) > 0.0d, "PROBE_EV_NOT_POSITIVE");
        require(a, r.positiveGroups >= 3, "PROBE_GROUP_QUORUM_LOW");
        require(a, r.coverage >= AppConfig.V46_MIN_COVERAGE, "PROBE_COVERAGE_LOW");
        require(a, r.thirdQuality >= AppConfig.V46_MIN_THIRD_QUALITY, "PROBE_THIRD_LOW");
        require(a, r.uncertainty <= AppConfig.V46_MAX_UNCERTAINTY_PROBE, "PROBE_UNCERTAINTY_HIGH");
        require(a, r.counterRisk <= AppConfig.V46_MAX_COUNTER_RISK, "PROBE_COUNTER_HIGH");
        require(a, r.qualityVotes >= AppConfig.V46_MIN_QUALITY_VOTES, "PROBE_QUALITY_QUORUM_LOW");
        require(a, r.acceptScore >= a.minAccept, "PROBE_ACCEPT_SCORE_LOW");
        a.pass = a.failureCount == 0;
        return a;
    }

    private static LaneAttempt attempt(String lane, double gate, double minAccept) {
        LaneAttempt a = new LaneAttempt();
        a.lane = lane;
        a.gate = gate;
        a.minAccept = minAccept;
        return a;
    }

    private static int supportVotes(
            Models.ComboCandidate c, ActionUtilityEngine.Result u, Result r) {
        int v = r.evidence == null ? 0 : r.evidence.positiveGroups;
        if (r.probability >= AppConfig.V46_PROBE_MIN_EVENT_P) v++;
        if (r.actionLcb >= AppConfig.V46_PROBE_MIN_ACTION_LCB) v++;
        if (RankingAuditEngine.expectedValue(r.probability) > 0.0d) v++;
        if (r.familyLcb >= AppConfig.V42_PROBE_MIN_FAMILY_LCB) v++;
        if (u.structureScore >= AppConfig.V42_PROBE_MIN_STRUCTURE) v++;
        return v;
    }

    private static int qualityVotes(
            Models.ComboCandidate c, ActionUtilityEngine.Result u, Result r) {
        int v = r.evidence == null ? 0 : r.evidence.positiveGroups;
        if (r.coverage >= AppConfig.V46_MIN_COVERAGE) v++;
        if (r.thirdQuality >= AppConfig.V46_MIN_THIRD_QUALITY) v++;
        if (r.uncertainty <= AppConfig.V46_MAX_UNCERTAINTY_FLOOR) v++;
        if (r.counterRisk <= AppConfig.V46_MAX_COUNTER_RISK) v++;
        return v;
    }

    private static double acceptScore(
            Models.ComboCandidate c, ActionUtilityEngine.Result u, Result r) {
        double groupSupport = r.evidence == null ? 0.0d : r.evidence.supportMean;
        double groupConfidence = r.evidence == null ? 0.0d : r.evidence.confidenceMean;
        double groupRisk = r.evidence == null ? 1.0d : r.evidence.riskMean;
        double regimeQuality = r.regime == null ? 0.0d : r.regime.quality;
        double association = r.association == null ? 0.0d : r.association.score;
        double distillSupport = r.distillation == null ? 0.0d : r.distillation.support;
        return clamp01(
                0.25d * groupSupport
                        + 0.17d * groupConfidence
                        + 0.15d * u.structureScore
                        + 0.10d * u.discriminationScore
                        + 0.10d * regimeQuality
                        + 0.06d * association
                        + 0.05d * distillSupport
                        + 0.06d * r.coverage
                        + 0.06d * r.thirdQuality
                        - 0.12d * r.uncertainty
                        - 0.12d * groupRisk
                        - 0.08d * r.counterRisk);
    }

    private static synchronized Result publish(
            DbStore store,
            Models.ComboCandidate c,
            Result out,
            ActionUtilityEngine.Result utility) {
        String key = PredictionEnvelope.candidateKey(c);
        Result old = RESULT_CACHE.get(key);
        if (old != null) return old;
        int compileCount = CompiledActionContract.nextCompileCount(c);
        out.compileCount = compileCount;
        CompiledActionContract contract =
                new CompiledActionContract(
                        c,
                        out.lane,
                        out.code,
                        out.eligible,
                        out.probability,
                        out.gate,
                        out.familyLcb,
                        out.acceptScore,
                        out.coverage,
                        out.thirdQuality,
                        out.supportVotes,
                        out.qualityVotes,
                        out.primaryReject,
                        out.secondaryReject,
                        out.tertiaryReject,
                        compileCount);
        contract = CompiledActionContract.publish(contract);
        out.contractHash = contract == null ? "" : contract.contractHash;
        RESULT_CACHE.put(key, out);
        c.v43PipelineStage = "V46_COMPILED_ACTION_CONTRACT";
        c.dynamicGate = out.gate;
        c.riskAdjustedGate = out.gate;
        c.gate = out.gate;
        c.edge = out.probability - out.gate;
        c.laneCandidate = out.eligible ? out.lane : "SHADOW";
        c.v46ContractHash = out.contractHash;
        PredictionLifecycleTraceEngine.recordContract(store, c, contract);
        try {
            if (store != null) {
                store.setState("v46_lane_version", MODULE_VERSION);
                store.setState("v46_lane_last", out.summary());
                store.setState("v46_lane_compile_count", String.valueOf(compileCount));
            }
        } catch (Throwable ignore) {
        }
        return out;
    }

    private static synchronized Result cached(Models.ComboCandidate c, String requiredLane) {
        if (c == null) return null;
        Result r = RESULT_CACHE.get(PredictionEnvelope.candidateKey(c));
        if (r == null) return null;
        String required = normalizeLane(requiredLane);
        if (required.length() > 0 && (!r.eligible || !required.equals(r.lane))) return null;
        return r;
    }

    private static Result accept(Result out, LaneAttempt a) {
        out.eligible = true;
        out.lane = a.lane;
        out.code = "LANE_" + a.lane + "_READY";
        out.gate = a.gate;
        out.edge = out.probability - out.gate;
        return out;
    }

    private static Result rejectFromAttempt(Result out, LaneAttempt a) {
        if (a == null) return reject(out, "LANE_NO_ATTEMPT");
        out.lane = "SHADOW";
        out.gate = a.gate;
        out.edge = out.probability - out.gate;
        out.primaryReject = failure(a, 0);
        out.secondaryReject = failure(a, 1);
        out.tertiaryReject = failure(a, 2);
        out.code = safe(out.primaryReject).length() == 0 ? "LANE_NOT_READY" : out.primaryReject;
        return out;
    }

    private static Result reject(Result out, String code) {
        out.eligible = false;
        out.lane = "SHADOW";
        out.code = safe(code);
        out.primaryReject = safe(code);
        return out;
    }

    private static LaneAttempt closest(LaneAttempt a, LaneAttempt b, LaneAttempt c) {
        LaneAttempt x = a;
        if (b != null
                && (x == null
                        || b.passCount > x.passCount
                        || (b.passCount == x.passCount && b.failureCount < x.failureCount))) x = b;
        if (c != null
                && (x == null
                        || c.passCount > x.passCount
                        || (c.passCount == x.passCount && c.failureCount < x.failureCount))) x = c;
        return x;
    }

    private static void require(LaneAttempt a, boolean ok, String code) {
        if (ok) {
            a.passCount++;
            return;
        }
        if (a.failureCount < a.failures.length) a.failures[a.failureCount] = safe(code);
        a.failureCount++;
    }

    private static String failure(LaneAttempt a, int index) {
        return a == null || index < 0 || index >= a.failureCount || index >= a.failures.length
                ? ""
                : safe(a.failures[index]);
    }

    private static int startupSample(Models.ComboCandidate c) {
        if (c == null) return 0;
        int x = Math.max(c.evidenceForestSample, c.historicalPriorSample);
        x = Math.max(x, c.calibrationTotalSample);
        x = Math.max(x, c.posteriorSample);
        x = Math.max(x, c.microWalkForwardSample);
        return x;
    }

    private static double conservativeFamilyLcb(Models.ComboCandidate c) {
        if (c == null) return 0.0d;
        double x = Math.max(c.familyWilsonLower, c.posteriorWilsonLower);
        if (x <= 0.0d && c.posteriorSample < AppConfig.V42_PROBE_FAMILY_COLD_SAMPLE)
            x = AppConfig.V42_FAMILY_COLD_PRIOR_LCB;
        return clamp01(x);
    }

    private static double candidateCoverage(Models.ComboCandidate c) {
        return c == null
                ? 0.0d
                : max01(
                        c.coverageQuality,
                        c.coverageQualityScore,
                        c.dynamicGateCoverageQualityScore);
    }

    private static double candidateThird(Models.ComboCandidate c) {
        return c == null
                ? 0.0d
                : max01(c.thirdQuality, c.thirdNumberQualityScore, c.dynamicGateThirdNumberQuality);
    }

    private static double modelProbability(Models.ComboCandidate c) {
        if (c == null) return 0.0d;
        return c.modelP > 0.0d ? c.modelP : c.rawP;
    }

    public static String normalizeLane(String lane) {
        String s = safe(lane).trim();
        if (s.equals(AppConfig.BET_DOMAIN_NORMAL) || s.equals("NORMAL"))
            return AppConfig.BET_DOMAIN_NORMAL;
        if (s.equals(AppConfig.BET_DOMAIN_QUALIFIED_FLOOR)
                || s.equals("QUALIFIED_FLOOR")
                || s.equals("FLOOR")) return AppConfig.BET_DOMAIN_QUALIFIED_FLOOR;
        if (s.equals(AppConfig.BET_DOMAIN_REAL_BET_PROBE)
                || s.equals("REAL_PROBE")
                || s.equals("PROBE")) return AppConfig.BET_DOMAIN_REAL_BET_PROBE;
        return "";
    }

    private static String machineCode(String s) {
        String x = safe(s).toUpperCase(java.util.Locale.US);
        StringBuilder b = new StringBuilder();
        for (int i = 0; i < x.length(); i++) {
            char ch = x.charAt(i);
            b.append(((ch >= 'A' && ch <= 'Z') || (ch >= '0' && ch <= '9')) ? ch : '_');
        }
        return b.toString();
    }

    private static double max01(double a, double b, double c) {
        return clamp01(Math.max(a, Math.max(b, c)));
    }

    private static double clamp01(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return 0.0d;
        if (v > 1.5d) v /= 100.0d;
        return v < 0 ? 0 : (v > 1 ? 1 : v);
    }

    private static String round(double v) {
        return String.valueOf(Math.round(v * 1000.0d) / 1000.0d);
    }

    private static String shortHash(String s) {
        return safe(s).length() <= 12 ? safe(s) : s.substring(0, 12);
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }
}
