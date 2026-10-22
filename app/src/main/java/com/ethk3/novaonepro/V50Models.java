package com.ethk3.novaonepro;


/** Immutable typed data carried by the V50 PRIME runtime and ledgers. */
public final class V50Models {
    private V50Models() {}

    public static final class PredictionContext {
        public final String baseIssue;
        public final String targetIssue;
        public final String historyMaxIssue;
        public final String historyHash;
        public final long createdAt;
        public final long dataAgeMs;
        public final long serviceGeneration;
        public final String featureVersion;
        public final String cohortId;

        public PredictionContext(
                String baseIssue,
                String targetIssue,
                String historyMaxIssue,
                String historyHash,
                long createdAt,
                long dataAgeMs,
                long serviceGeneration,
                String featureVersion,
                String cohortId) {
            this.baseIssue = safe(baseIssue);
            this.targetIssue = safe(targetIssue);
            this.historyMaxIssue = safe(historyMaxIssue);
            this.historyHash = safe(historyHash);
            this.createdAt = createdAt;
            this.dataAgeMs = Math.max(0L, dataAgeMs);
            this.serviceGeneration = Math.max(0L, serviceGeneration);
            this.featureVersion = safe(featureVersion);
            this.cohortId = safe(cohortId);
        }
    }

    public static final class NumberFeatureRow {
        public final int position;
        public final int number;
        public final double[] windowFrequency;

        /** Exact 24-field training/serving vector consumed by V50NumberModelArtifact. */
        public final double[] modelFeatures;

        public final double gap;
        public final double gapPercentile;
        public final double gapHazard;
        public final double releaseFailureRate;
        public final double slope;
        public final double acceleration;
        public final double volatility;
        public final double positionEntropy;
        public final double concentration;
        public final double attributeScore;
        public final double relationScore;
        public final double drift;
        public final String featureHash;

        public NumberFeatureRow(
                int position,
                int number,
                double[] windowFrequency,
                double[] modelFeatures,
                double gap,
                double gapPercentile,
                double gapHazard,
                double releaseFailureRate,
                double slope,
                double acceleration,
                double volatility,
                double positionEntropy,
                double concentration,
                double attributeScore,
                double relationScore,
                double drift,
                String featureHash) {
            this.position = position;
            this.number = number;
            this.windowFrequency = copy(windowFrequency);
            this.modelFeatures = copy(modelFeatures);
            this.gap = finite(gap);
            this.gapPercentile = finite(gapPercentile);
            this.gapHazard = finite(gapHazard);
            this.releaseFailureRate = finite(releaseFailureRate);
            this.slope = finite(slope);
            this.acceleration = finite(acceleration);
            this.volatility = finite(volatility);
            this.positionEntropy = finite(positionEntropy);
            this.concentration = finite(concentration);
            this.attributeScore = finite(attributeScore);
            this.relationScore = finite(relationScore);
            this.drift = finite(drift);
            this.featureHash = safe(featureHash);
        }
    }

    public static final class EffectiveConsensus {
        public final int position;
        public final double distributionAgreement;
        public final double top3Overlap;
        public final double rankCorrelation;
        public final double directionalAgreement;
        public final double signalStrength;
        public final double historicalReliability;
        public final double recentReliability;
        public final double stateReliability;
        public final double effectiveConsensus;
        public final String checksum;

        public EffectiveConsensus(
                int position,
                double distributionAgreement,
                double top3Overlap,
                double rankCorrelation,
                double directionalAgreement,
                double signalStrength,
                double historicalReliability,
                double recentReliability,
                double stateReliability,
                double effectiveConsensus,
                String checksum) {
            this.position = position;
            this.distributionAgreement = finite(distributionAgreement);
            this.top3Overlap = finite(top3Overlap);
            this.rankCorrelation = finite(rankCorrelation);
            this.directionalAgreement = finite(directionalAgreement);
            this.signalStrength = finite(signalStrength);
            this.historicalReliability = finite(historicalReliability);
            this.recentReliability = finite(recentReliability);
            this.stateReliability = finite(stateReliability);
            this.effectiveConsensus = finite(effectiveConsensus);
            this.checksum = safe(checksum);
        }
    }

    public static final class TeacherEvidence {
        public final int position;
        public final int number;
        public final double posteriorP;
        public final double credibleLow;
        public final int totalSample;
        public final int recentSample;
        public final int oosSample;
        public final int realSample;
        public final int shadowSample;
        public final int blockCount;
        public final double crossBlockStability;
        public final double recentValidity;
        public final double teacherLift;
        public final double misleadRisk;
        public final double transferability;
        public final V50Enums.TeacherStatus status;
        public final String evidenceLevel;
        public final String checksum;

        public TeacherEvidence(
                int position,
                int number,
                double posteriorP,
                double credibleLow,
                int totalSample,
                int recentSample,
                int oosSample,
                int realSample,
                int shadowSample,
                int blockCount,
                double crossBlockStability,
                double recentValidity,
                double teacherLift,
                double misleadRisk,
                double transferability,
                V50Enums.TeacherStatus status,
                String evidenceLevel,
                String checksum) {
            this.position = position;
            this.number = number;
            this.posteriorP = finite(posteriorP);
            this.credibleLow = finite(credibleLow);
            this.totalSample = Math.max(0, totalSample);
            this.recentSample = Math.max(0, recentSample);
            this.oosSample = Math.max(0, oosSample);
            this.realSample = Math.max(0, realSample);
            this.shadowSample = Math.max(0, shadowSample);
            this.blockCount = Math.max(0, blockCount);
            this.crossBlockStability = finite(crossBlockStability);
            this.recentValidity = finite(recentValidity);
            this.teacherLift = finite(teacherLift);
            this.misleadRisk = finite(misleadRisk);
            this.transferability = finite(transferability);
            this.status = status == null ? V50Enums.TeacherStatus.NO_SAMPLE : status;
            this.evidenceLevel = safe(evidenceLevel);
            this.checksum = safe(checksum);
        }
    }

    public static final class NumberFeatureSnapshot {
        public final PredictionContext context;
        public final NumberFeatureRow[] rows;
        public final String checksum;

        public NumberFeatureSnapshot(
                PredictionContext context, NumberFeatureRow[] rows, String checksum) {
            this.context = context;
            this.rows = rows == null ? new NumberFeatureRow[0] : (NumberFeatureRow[]) rows.clone();
            this.checksum = safe(checksum);
        }

        public boolean hasExactly100Rows() {
            if (rows.length != 100) return false;
            boolean[][] seen = new boolean[10][10];
            for (int i = 0; i < rows.length; i++) {
                NumberFeatureRow row = rows[i];
                if (row == null
                        || row.position < 1
                        || row.position > 10
                        || row.number < 1
                        || row.number > 10) return false;
                if (seen[row.position - 1][row.number - 1]) return false;
                seen[row.position - 1][row.number - 1] = true;
            }
            return true;
        }
    }

    public static final class StatePrototypeScore {
        public final String prototypeId;
        public final double score;

        public StatePrototypeScore(String prototypeId, double score) {
            this.prototypeId = safe(prototypeId);
            this.score = finite(score);
        }
    }

    public static final class StateTrajectorySnapshot {
        public final String targetIssue;
        public final int position;
        public final StatePrototypeScore[] prototypes;
        public final String dynamicDomain;
        public final V50Enums.Lifecycle lifecycle;
        public final int coarseRunLength;
        public final int mediumRunLength;
        public final int fineRunLength;
        public final double remainingLife;
        public final double hazard;
        public final double transitionRisk;
        public final double confidence;
        public final String checksum;

        public StateTrajectorySnapshot(
                String targetIssue,
                int position,
                StatePrototypeScore[] prototypes,
                String dynamicDomain,
                V50Enums.Lifecycle lifecycle,
                int coarseRunLength,
                int mediumRunLength,
                int fineRunLength,
                double remainingLife,
                double hazard,
                double transitionRisk,
                double confidence,
                String checksum) {
            this.targetIssue = safe(targetIssue);
            this.position = position;
            this.prototypes =
                    prototypes == null
                            ? new StatePrototypeScore[0]
                            : (StatePrototypeScore[]) prototypes.clone();
            this.dynamicDomain = safe(dynamicDomain);
            this.lifecycle = lifecycle == null ? V50Enums.Lifecycle.UNKNOWN : lifecycle;
            this.coarseRunLength = Math.max(0, coarseRunLength);
            this.mediumRunLength = Math.max(0, mediumRunLength);
            this.fineRunLength = Math.max(0, fineRunLength);
            this.remainingLife = finite(remainingLife);
            this.hazard = finite(hazard);
            this.transitionRisk = finite(transitionRisk);
            this.confidence = finite(confidence);
            this.checksum = safe(checksum);
        }
    }

    public static final class ExpertEvidence {
        public final String expertId;
        public final double[][] numberProbability;
        public final double[] trust;
        public final double[] uncertainty;
        public final double[] signalStrength;
        public final String[] evidence;
        public final String[] counterEvidence;
        public final String artifactId;
        public final String checksum;

        public ExpertEvidence(
                String expertId,
                double[][] numberProbability,
                double[] trust,
                double[] uncertainty,
                double[] signalStrength,
                String[] evidence,
                String[] counterEvidence,
                String artifactId,
                String checksum) {
            this.expertId = safe(expertId);
            this.numberProbability = copy(numberProbability);
            this.trust = copy(trust);
            this.uncertainty = copy(uncertainty);
            this.signalStrength = copy(signalStrength);
            this.evidence = copy(evidence);
            this.counterEvidence = copy(counterEvidence);
            this.artifactId = safe(artifactId);
            this.checksum = safe(checksum);
        }
    }

    public static final class PositionAction {
        public final String targetIssue;
        public final int position;
        public final int[] top3;
        public final double rawActionP;
        public final double thirdP;
        public final double thirdMargin;
        public final double entropy;
        public final double effectiveConsensus;
        public final String dynamicDomain;
        public final V50Enums.Lifecycle lifecycle;
        public final double[] rankFeatures;
        public final double rankScore;
        public final double rankAdjustedP;
        public final int rank;
        public final String fingerprint;
        public final String checksum;

        public PositionAction(
                String targetIssue,
                int position,
                int[] top3,
                double rawActionP,
                double thirdP,
                double thirdMargin,
                double entropy,
                double effectiveConsensus,
                String dynamicDomain,
                V50Enums.Lifecycle lifecycle,
                double[] rankFeatures,
                double rankScore,
                double rankAdjustedP,
                int rank,
                String fingerprint,
                String checksum) {
            this.targetIssue = safe(targetIssue);
            this.position = position;
            this.top3 = top3 == null ? new int[0] : (int[]) top3.clone();
            this.rawActionP = finite(rawActionP);
            this.thirdP = finite(thirdP);
            this.thirdMargin = finite(thirdMargin);
            this.entropy = finite(entropy);
            this.effectiveConsensus = finite(effectiveConsensus);
            this.dynamicDomain = safe(dynamicDomain);
            this.lifecycle = lifecycle == null ? V50Enums.Lifecycle.UNKNOWN : lifecycle;
            this.rankFeatures = copy(rankFeatures);
            this.rankScore = finite(rankScore);
            this.rankAdjustedP = finite(rankAdjustedP);
            this.rank = rank;
            this.fingerprint = safe(fingerprint);
            this.checksum = safe(checksum);
        }

        public boolean validFixedK3() {
            if (position < 1 || position > 10 || top3.length != 3) return false;
            boolean[] seen = new boolean[11];
            for (int i = 0; i < top3.length; i++) {
                int n = top3[i];
                if (n < 1 || n > 10 || seen[n]) return false;
                seen[n] = true;
            }
            return true;
        }
    }

    public static final class SelectedActionEnvelope {
        public final PredictionContext context;
        public final PositionAction action;
        public final double selectedHitP;
        public final double uncertainty;
        public final V50Enums.DomainStatus domainStatus;
        public final V50Enums.Lane lane;
        public final String primaryCause;
        public final String secondaryCause;
        public final String thirdCause;
        public final String counterEvidence;
        public final String necessaryCondition;
        public final String falsifierCondition;
        public final String contractHash;

        public SelectedActionEnvelope(
                PredictionContext context,
                PositionAction action,
                double selectedHitP,
                double uncertainty,
                V50Enums.DomainStatus domainStatus,
                V50Enums.Lane lane,
                String primaryCause,
                String secondaryCause,
                String thirdCause,
                String counterEvidence,
                String necessaryCondition,
                String falsifierCondition,
                String contractHash) {
            this.context = context;
            this.action = action;
            this.selectedHitP = finite(selectedHitP);
            this.uncertainty = finite(uncertainty);
            this.domainStatus = domainStatus == null ? V50Enums.DomainStatus.WATCH : domainStatus;
            this.lane = lane == null ? V50Enums.Lane.SHADOW : lane;
            this.primaryCause = safe(primaryCause);
            this.secondaryCause = safe(secondaryCause);
            this.thirdCause = safe(thirdCause);
            this.counterEvidence = safe(counterEvidence);
            this.necessaryCondition = safe(necessaryCondition);
            this.falsifierCondition = safe(falsifierCondition);
            this.contractHash = safe(contractHash);
        }
    }

    public static final class DomainCertificate {
        public final String domainId;
        public final String parentPrototype;
        public final V50Enums.Lifecycle lifecycle;
        public final int sample;
        public final int recentSample;
        public final int blockCount;
        public final double hitRate;
        public final double credibleLow;
        public final double crossBlockStability;
        public final double calibrationError;
        public final int maxLoseStreak;
        public final double reasonValidity;
        public final double falsifierFailure;
        public final double transitionPerformance;
        public final V50Enums.DomainStatus status;
        public final String checksum;

        public DomainCertificate(
                String domainId,
                String parentPrototype,
                V50Enums.Lifecycle lifecycle,
                int sample,
                int recentSample,
                int blockCount,
                double hitRate,
                double credibleLow,
                double crossBlockStability,
                double calibrationError,
                int maxLoseStreak,
                double reasonValidity,
                double falsifierFailure,
                double transitionPerformance,
                V50Enums.DomainStatus status,
                String checksum) {
            this.domainId = safe(domainId);
            this.parentPrototype = safe(parentPrototype);
            this.lifecycle = lifecycle == null ? V50Enums.Lifecycle.UNKNOWN : lifecycle;
            this.sample = Math.max(0, sample);
            this.recentSample = Math.max(0, recentSample);
            this.blockCount = Math.max(0, blockCount);
            this.hitRate = finite(hitRate);
            this.credibleLow = finite(credibleLow);
            this.crossBlockStability = finite(crossBlockStability);
            this.calibrationError = finite(calibrationError);
            this.maxLoseStreak = Math.max(0, maxLoseStreak);
            this.reasonValidity = finite(reasonValidity);
            this.falsifierFailure = finite(falsifierFailure);
            this.transitionPerformance = finite(transitionPerformance);
            this.status = status == null ? V50Enums.DomainStatus.WATCH : status;
            this.checksum = safe(checksum);
        }
    }

    public static final class UncertaintyBreakdown {
        public final double aleatoric;
        public final double expertDisagreement;
        public final double state;
        public final double calibration;
        public final double domain;
        public final double total;

        public UncertaintyBreakdown(
                double aleatoric,
                double expertDisagreement,
                double state,
                double calibration,
                double domain,
                double total) {
            this.aleatoric = finite(aleatoric);
            this.expertDisagreement = finite(expertDisagreement);
            this.state = finite(state);
            this.calibration = finite(calibration);
            this.domain = finite(domain);
            this.total = finite(total);
        }
    }

    public static final class RuntimeResult {
        public final boolean success;
        public final PredictionContext context;
        public final NumberFeatureSnapshot features;
        public final StateTrajectorySnapshot[] states;
        public final ExpertEvidence[] experts;
        public final double[][] numberProbability;
        public final EffectiveConsensus[] consensus;
        public final PositionAction[] actions;
        public final SelectedActionEnvelope selected;
        public final AuthorizedDecision authorized;
        public final DomainCertificate domainCertificate;
        public final UncertaintyBreakdown uncertainty;
        public final String compiledContractChecksum;
        public final String status;
        public final long totalCostMs;
        public final String checksum;

        public RuntimeResult(
                boolean success,
                PredictionContext context,
                NumberFeatureSnapshot features,
                StateTrajectorySnapshot[] states,
                ExpertEvidence[] experts,
                double[][] numberProbability,
                EffectiveConsensus[] consensus,
                PositionAction[] actions,
                SelectedActionEnvelope selected,
                AuthorizedDecision authorized,
                DomainCertificate domainCertificate,
                UncertaintyBreakdown uncertainty,
                String compiledContractChecksum,
                String status,
                long totalCostMs,
                String checksum) {
            this.success = success;
            this.context = context;
            this.features = features;
            this.states =
                    states == null
                            ? new StateTrajectorySnapshot[0]
                            : (StateTrajectorySnapshot[]) states.clone();
            this.experts =
                    experts == null ? new ExpertEvidence[0] : (ExpertEvidence[]) experts.clone();
            this.numberProbability = copy(numberProbability);
            this.consensus =
                    consensus == null
                            ? new EffectiveConsensus[0]
                            : (EffectiveConsensus[]) consensus.clone();
            this.actions =
                    actions == null ? new PositionAction[0] : (PositionAction[]) actions.clone();
            this.selected = selected;
            this.authorized = authorized;
            this.domainCertificate = domainCertificate;
            this.uncertainty = uncertainty;
            this.compiledContractChecksum = safe(compiledContractChecksum);
            this.status = safe(status);
            this.totalCostMs = Math.max(0L, totalCostMs);
            this.checksum = safe(checksum);
        }
    }

    public static final class AuthorizedDecision {
        public final String decisionId;
        public final SelectedActionEnvelope envelope;
        public final V50Enums.DecisionAction action;
        public final String authorizationSignature;
        public final String authorizationOwner;
        public final long authorizationTime;
        public final V50Enums.RejectCode rejectCode;

        public AuthorizedDecision(
                String decisionId,
                SelectedActionEnvelope envelope,
                V50Enums.DecisionAction action,
                String authorizationSignature,
                String authorizationOwner,
                long authorizationTime,
                V50Enums.RejectCode rejectCode) {
            this.decisionId = safe(decisionId);
            this.envelope = envelope;
            this.action = action == null ? V50Enums.DecisionAction.HOLD : action;
            this.authorizationSignature = safe(authorizationSignature);
            this.authorizationOwner = safe(authorizationOwner);
            this.authorizationTime = authorizationTime;
            this.rejectCode = rejectCode == null ? V50Enums.RejectCode.NONE : rejectCode;
        }
    }

    public static final class LearningReport {
        public final String targetIssue;
        public final boolean success;
        public final int issueRows;
        public final int numberEventRows;
        public final int positionActionRows;
        public final int selectedDecisionRows;
        public final int actualPositionHits;
        public final int selectedHit;
        public final V50Enums.FailureCode failureCode;
        public final String detail;
        public final String checksum;

        public LearningReport(
                String targetIssue,
                boolean success,
                int issueRows,
                int numberEventRows,
                int positionActionRows,
                int selectedDecisionRows,
                int actualPositionHits,
                int selectedHit,
                V50Enums.FailureCode failureCode,
                String detail,
                String checksum) {
            this.targetIssue = safe(targetIssue);
            this.success = success;
            this.issueRows = issueRows;
            this.numberEventRows = numberEventRows;
            this.positionActionRows = positionActionRows;
            this.selectedDecisionRows = selectedDecisionRows;
            this.actualPositionHits = actualPositionHits;
            this.selectedHit = selectedHit;
            this.failureCode = failureCode == null ? V50Enums.FailureCode.UNKNOWN : failureCode;
            this.detail = safe(detail);
            this.checksum = safe(checksum);
        }
    }

    private static String safe(String value) {
        return value == null ? "" : value;
    }

    private static double finite(double value) {
        return Double.isNaN(value) || Double.isInfinite(value) ? 0.0d : value;
    }

    private static double[] copy(double[] source) {
        return source == null ? new double[0] : (double[]) source.clone();
    }

    private static String[] copy(String[] source) {
        return source == null ? new String[0] : (String[]) source.clone();
    }

    private static double[][] copy(double[][] source) {
        if (source == null) return new double[0][];
        double[][] out = new double[source.length][];
        for (int i = 0; i < source.length; i++) out[i] = copy(source[i]);
        return out;
    }
}
