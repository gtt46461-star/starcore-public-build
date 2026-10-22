package com.ethk3.novaonepro;

import java.lang.reflect.Field;
import java.security.MessageDigest;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/**
 * V43 canonical evidence contract.
 *
 * <p>Every legacy strategy is allowed to describe evidence, but no legacy strategy owns the final
 * event probability, lane or BET authorization. This class converts the many historical field names
 * into one auditable vector without deleting their source fields. Missing evidence is explicit and
 * is never silently replaced by a strong score.
 */
public final class StrategyEvidenceContract {
    public static final String VERSION = AppConfig.V43_STRATEGY_EVIDENCE_VERSION;

    public static final String GROUP_SHAPE = "SHAPE_TREND";
    public static final String GROUP_SEQUENCE = "SEQUENCE_TRANSITION";
    public static final String GROUP_CAUSAL = "CAUSAL_POSITION";
    public static final String GROUP_TEACHER = "TEACHER_LEARNING";
    public static final String GROUP_COUNTERFACTUAL = "COUNTERFACTUAL";
    public static final String GROUP_DYNAMIC = "DYNAMIC_CLUSTER";
    public static final String GROUP_RISK = "RISK_HEALTH";
    public static final String GROUP_CALIBRATION = "OOS_CALIBRATION";

    private static final Map<Class<?>, Map<String, Field>> FIELD_CACHE =
            new HashMap<Class<?>, Map<String, Field>>();

    private StrategyEvidenceContract() {}

    public static final class Snapshot {
        public double shape;
        public double sequence;
        public double causal;
        public double teacher;
        public double counterfactualSupport;
        public double dynamic;
        public double risk;
        public double calibration;
        public double support;
        public double confidence;
        public double diversity;
        public double maturity;
        public double fragility;
        public int availableGroups;
        public int hardRisk;
        public String dominantGroup = "NONE";
        public String missingGroups = "";
        public String fingerprint = "";
        public String summary = "";
    }

    public static Snapshot capture(Object candidate) {
        Snapshot s = new Snapshot();
        if (candidate == null) {
            s.missingGroups = "ALL";
            s.summary = "V43证据合同：候选为空";
            return s;
        }

        ArrayList<String> missing = new ArrayList<String>();
        s.shape =
                groupScore(
                        candidate,
                        new String[] {
                            "shapeScore",
                            "shapeConfidence",
                            "patternScore",
                            "patternConfidence",
                            "trendScore",
                            "trendConfidence",
                            "trendLifeScore",
                            "stateConfidence",
                            "windowConsistency",
                            "windowConsensusScore",
                            "breakRecoveryScore",
                            "recoveryScore",
                            "cycleScore",
                            "rhythmScore",
                            "positionStrength",
                            "deepFeatureScore",
                            "deepFeatureConfidence",
                            "deepStateScore",
                            "profileScore",
                            "runtimeProfileScore",
                            "stateNumberProfileScore",
                            "biasScore",
                            "biasConfidence",
                            "shapeP"
                        },
                        missing,
                        GROUP_SHAPE);

        s.sequence =
                groupScore(
                        candidate,
                        new String[] {
                            "sequenceProbability",
                            "sequenceDistributionP",
                            "sequenceScore",
                            "markovProbability",
                            "markovScore",
                            "transitionScore",
                            "transitionConfidence",
                            "numberMoveScore",
                            "positionMoveScore",
                            "historicalPriorProbability",
                            "historicalPriorConfidence",
                            "evidenceForestProbability",
                            "evidenceForestP",
                            "evidenceForestConfidence",
                            "deepTransitionScore",
                            "transitionRelationScore",
                            "numberBehaviorScore",
                            "sequenceConfidence"
                        },
                        missing,
                        GROUP_SEQUENCE);

        s.causal =
                groupScore(
                        candidate,
                        new String[] {
                            "causalScore",
                            "causalConfidence",
                            "causeScore",
                            "causeConfidence",
                            "positionCauseScore",
                            "positionHealthScore",
                            "dynamicCauseScore",
                            "dynamicCauseConfidence",
                            "needSupplyScore",
                            "needSupplyMatchScore",
                            "positionNeedScore",
                            "numberSupplyScore",
                            "causeChainScore",
                            "reasonCausalScore",
                            "associationScore",
                            "reasonScore",
                            "causalLift",
                            "causeAblationScore",
                            "evidenceScore",
                            "aiJudgeScore",
                            "aiJudgeConfidence"
                        },
                        missing,
                        GROUP_CAUSAL);

        s.teacher = teacherScore(candidate, missing);

        double counterRisk =
                maxNormalized(
                        candidate,
                        new String[] {
                            "counterRisk", "counterEvidenceScore", "counterfactualRisk",
                            "counterfactualWhyNotRisk", "counterfactualRiskScore", "whyNotRisk",
                            "ablationRisk", "falseCauseRisk"
                        });
        double counterSupport =
                maxNormalized(
                        candidate,
                        new String[] {
                            "counterfactualSupport", "counterSupport", "counterfactualConfidence",
                            "counterEvidenceSupport", "ablationSupport", "contrastiveSupport",
                            "counterfactualRankScore", "reasonWilsonScore"
                        });
        if (!groupActive(
                candidate,
                GROUP_COUNTERFACTUAL,
                new String[] {
                    "counterRisk",
                    "counterfactualRisk",
                    "counterfactualSupport",
                    "counterEvidenceScore",
                    "ablationRisk",
                    "contrastiveSupport",
                    "counterfactualRankScore"
                })) {
            missing.add(GROUP_COUNTERFACTUAL);
        }
        s.counterfactualSupport = clamp01(Math.max(counterSupport, 1.0d - counterRisk));

        s.dynamic =
                groupScore(
                        candidate,
                        new String[] {
                            "dynamicWindowScore",
                            "dynamicClusterScore",
                            "clusterConfidence",
                            "dynamicStateConfidence",
                            "regimeQuality",
                            "promotionScore",
                            "shadowPromotionScore",
                            "adaptiveScore",
                            "adaptiveConfidence",
                            "domainConfidence",
                            "familyQuality",
                            "dynamicClusterHitRate",
                            "dynamicClusterRecentHitRate",
                            "dynamicClusterConfidence",
                            "adaptiveClusterConfidence",
                            "adaptiveCauseScore",
                            "adaptivePromoteScore",
                            "stateConfidence",
                            "promotionScore",
                            "aPlusScore",
                            "auditBoost"
                        },
                        missing,
                        GROUP_DYNAMIC);

        s.risk = aggregateRisk(candidate, missing);

        s.calibration =
                groupScore(
                        candidate,
                        new String[] {
                            "calibrationConfidence", "posteriorConfidence", "posteriorWilsonLower",
                            "familyWilsonLower", "microWalkForwardConfidence", "oosReliability",
                            "oosConfidence", "calibrationReady", "posteriorMean"
                        },
                        missing,
                        GROUP_CALIBRATION);

        double[] values =
                new double[] {
                    s.shape,
                    s.sequence,
                    s.causal,
                    s.teacher,
                    s.counterfactualSupport,
                    s.dynamic,
                    s.calibration
                };
        String[] names =
                new String[] {
                    GROUP_SHAPE,
                    GROUP_SEQUENCE,
                    GROUP_CAUSAL,
                    GROUP_TEACHER,
                    GROUP_COUNTERFACTUAL,
                    GROUP_DYNAMIC,
                    GROUP_CALIBRATION
                };
        double weighted = 0.0d;
        double weightSum = 0.0d;
        double max = -1.0d;
        double min = 1.0d;
        String dominant = "NONE";
        for (int i = 0; i < values.length; i++) {
            double v = clamp01(values[i]);
            if (!contains(missing, names[i])) {
                double w = evidenceWeight(names[i]);
                weighted += w * v;
                weightSum += w;
                s.availableGroups++;
                if (v > max) {
                    max = v;
                    dominant = names[i];
                }
                if (v < min) min = v;
            }
        }
        s.support = weightSum <= 0.0d ? 0.0d : clamp01(weighted / weightSum);
        s.diversity = clamp01(s.availableGroups / 7.0d);
        double agreement = s.availableGroups <= 1 ? 0.0d : clamp01(1.0d - (max - min));
        s.maturity = maturity(candidate, s.availableGroups);
        s.confidence =
                clamp01(
                        0.34d * s.diversity
                                + 0.26d * agreement
                                + 0.25d * s.maturity
                                + 0.15d * s.calibration);
        s.fragility =
                clamp01((1.0d - s.diversity) * 0.45d + (1.0d - agreement) * 0.35d + s.risk * 0.20d);
        s.hardRisk = hardRisk(candidate, s.risk);
        s.dominantGroup = dominant;
        s.missingGroups = join(missing, ",");
        s.fingerprint = fingerprint(candidate, s);
        ContractAudit r3Audit = auditCandidateR3(candidate);
        s.summary =
                "R3Contract="
                        + r3Audit.summary()
                        + "|V43统一证据：support="
                        + f(s.support)
                        + "|risk="
                        + f(s.risk)
                        + "|confidence="
                        + f(s.confidence)
                        + "|diversity="
                        + f(s.diversity)
                        + "|maturity="
                        + f(s.maturity)
                        + "|fragility="
                        + f(s.fragility)
                        + "|groups="
                        + s.availableGroups
                        + "/7"
                        + "|dominant="
                        + s.dominantGroup
                        + "|missing="
                        + (s.missingGroups.length() == 0 ? "NONE" : s.missingGroups)
                        + "|hardRisk="
                        + s.hardRisk;
        return s;
    }

    public static void write(Object target, Snapshot s, String stage) {
        if (target == null || s == null) return;
        writeDouble(target, "v43StrategySupport", s.support);
        writeDouble(target, "v43StrategyRisk", s.risk);
        writeDouble(target, "v43StrategyConfidence", s.confidence);
        writeDouble(target, "v43StrategyDiversity", s.diversity);
        writeDouble(target, "v43StrategyMaturity", s.maturity);
        writeDouble(target, "v43AblationFragility", s.fragility);
        writeInt(target, "v43EvidenceGroupCount", s.availableGroups);
        writeInt(target, "v43HardRisk", s.hardRisk);
        writeString(target, "v43DominantEvidence", s.dominantGroup);
        writeString(target, "v43MissingEvidence", s.missingGroups);
        writeString(target, "v43EvidenceFingerprint", s.fingerprint);
        writeString(target, "v43PipelineStage", safe(stage));
        writeString(target, "v43StrategyVersion", VERSION);
        appendString(target, "reasonSummary", s.summary);
    }

    private static double teacherScore(Object c, List<String> missing) {
        boolean has =
                groupActive(
                        c,
                        GROUP_TEACHER,
                        new String[] {
                            "teacherScore",
                            "teacherConfidence",
                            "teacherMatrixScore",
                            "teacherNumberScore",
                            "teacherHitRate",
                            "teacherRecentRate",
                            "teacherMatrixConfidence",
                            "teacherEvidenceContribution",
                            "teacherMatrixEvidenceContribution"
                        });
        if (!has) missing.add(GROUP_TEACHER);
        double score =
                maxNormalized(
                        c,
                        new String[] {
                            "teacherScore",
                            "teacherMatrixScore",
                            "teacherNumberScore",
                            "teacherHitRate",
                            "teacherRecentRate"
                        });
        double conf =
                maxNormalized(
                        c,
                        new String[] {
                            "teacherConfidence",
                            "teacherMatrixConfidence",
                            "teacherNumberConfidence"
                        });
        double sample =
                maxRaw(
                        c,
                        new String[] {
                            "teacherSample",
                            "teacherMatrixSample",
                            "teacherNumberSample",
                            "teacherEffectiveSample"
                        });
        double maturity = clamp01(sample / 60.0d);
        return clamp01(0.55d * score + 0.25d * conf + 0.20d * maturity);
    }

    private static double aggregateRisk(Object c, List<String> missing) {
        String[] fields =
                new String[] {
                    "riskScore",
                    "dynamicRisk",
                    "driftRisk",
                    "counterRisk",
                    "falseStrongRisk",
                    "falseSeedRisk",
                    "teacherRisk",
                    "teacherMatrixRisk",
                    "thirdRisk",
                    "thirdNumberRisk",
                    "ruleHealthRisk",
                    "shadowRisk",
                    "freezeRisk",
                    "comboConflict",
                    "conflictLevel",
                    "deepFeatureRisk",
                    "evidenceRisk",
                    "posteriorDrift",
                    "calibrationError",
                    "associationConflictRisk"
                };
        if (!groupActive(c, GROUP_RISK, fields)) missing.add(GROUP_RISK);
        double max = maxNormalized(c, fields);
        double hard = hardRisk(c, max) == 1 ? 1.0d : 0.0d;
        return clamp01(0.82d * max + 0.18d * hard);
    }

    private static int hardRisk(Object c, double risk) {
        if (readInt(c, "hardBlockFlag", 0) == 1) return 1;
        if (readInt(c, "teacherRiskFlag", 0) == 1) return 1;
        if (readInt(c, "falseStrongFreezeFlag", 0) == 1) return 1;
        if (readInt(c, "dynamicFreezeFlag", 0) == 1) return 1;
        if (readInt(c, "ruleHealthHardRiskFlag", 0) == 1) return 1;
        String source = readString(c, "hardBlockSource", "").toUpperCase(Locale.US);
        if (source.length() > 0 && source.indexOf("WATCH") < 0 && source.indexOf("SOFT") < 0)
            return 1;
        return risk >= AppConfig.V43_HARD_RISK_LINE ? 1 : 0;
    }

    private static double maturity(Object c, int groups) {
        double teacher =
                maxRaw(
                        c,
                        new String[] {
                            "teacherSample", "teacherMatrixSample", "teacherNumberSample"
                        });
        double posterior =
                maxRaw(c, new String[] {"posteriorSample", "familySample", "domainSample"});
        double cal = maxRaw(c, new String[] {"calibrationSample", "calibrationTotalSample"});
        double history =
                maxRaw(
                        c,
                        new String[] {
                            "evidenceForestSample",
                            "historicalPriorSample",
                            "sequenceHistorySize",
                            "historySample"
                        });
        double sample =
                0.24d * clamp01(teacher / 60.0d)
                        + 0.26d * clamp01(posterior / 80.0d)
                        + 0.22d * clamp01(cal / 40.0d)
                        + 0.28d * clamp01(history / 240.0d);
        return clamp01(0.75d * sample + 0.25d * clamp01(groups / 7.0d));
    }

    private static double groupScore(Object c, String[] fields, List<String> missing, String name) {
        if (!groupActive(c, name, fields)) {
            missing.add(name);
            return 0.0d;
        }
        double sum = 0.0d;
        int count = 0;
        for (int i = 0; i < fields.length; i++) {
            Double value = readDoubleNullable(c, fields[i]);
            if (value == null) continue;
            double v = normalize(value.doubleValue());
            if (v <= 0.0d) continue;
            sum += v;
            count++;
        }
        if (count == 0) return 0.0d;
        double mean = sum / count;
        double strongest = maxNormalized(c, fields);
        return clamp01(0.55d * mean + 0.45d * strongest);
    }

    private static double evidenceWeight(String name) {
        if (GROUP_CAUSAL.equals(name)) return 1.18d;
        if (GROUP_COUNTERFACTUAL.equals(name)) return 1.12d;
        if (GROUP_SEQUENCE.equals(name)) return 1.08d;
        if (GROUP_SHAPE.equals(name)) return 1.00d;
        if (GROUP_TEACHER.equals(name)) return 0.95d;
        if (GROUP_DYNAMIC.equals(name)) return 0.90d;
        if (GROUP_CALIBRATION.equals(name)) return 1.10d;
        return 1.0d;
    }

    private static String fingerprint(Object c, Snapshot s) {
        String raw =
                readString(c, "targetIssue", "")
                        + "|"
                        + readInt(c, "position", 0)
                        + "|"
                        + readInt(c, "n1", 0)
                        + "|"
                        + readInt(c, "n2", 0)
                        + "|"
                        + readInt(c, "n3", 0)
                        + "|"
                        + f(s.shape)
                        + "|"
                        + f(s.sequence)
                        + "|"
                        + f(s.causal)
                        + "|"
                        + f(s.teacher)
                        + "|"
                        + f(s.counterfactualSupport)
                        + "|"
                        + f(s.dynamic)
                        + "|"
                        + f(s.risk);
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] d = md.digest(raw.getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < d.length && i < 12; i++) {
                String h = Integer.toHexString(d[i] & 0xff);
                if (h.length() == 1) sb.append('0');
                sb.append(h);
            }
            return sb.toString();
        } catch (Throwable e) {
            return Integer.toHexString(raw.hashCode());
        }
    }

    private static boolean contains(List<String> list, String value) {
        if (list == null) return false;
        for (int i = 0; i < list.size(); i++) if (value.equals(list.get(i))) return true;
        return false;
    }

    private static String join(List<String> list, String sep) {
        StringBuilder sb = new StringBuilder();
        if (list != null)
            for (int i = 0; i < list.size(); i++) {
                if (i > 0) sb.append(sep);
                sb.append(list.get(i));
            }
        return sb.toString();
    }

    private static double maxNormalized(Object c, String[] fields) {
        double max = 0.0d;
        for (int i = 0; i < fields.length; i++) {
            Double v = readDoubleNullable(c, fields[i]);
            if (v != null) max = Math.max(max, normalize(v.doubleValue()));
        }
        return clamp01(max);
    }

    private static double maxRaw(Object c, String[] fields) {
        double max = 0.0d;
        for (int i = 0; i < fields.length; i++) {
            Double v = readDoubleNullable(c, fields[i]);
            if (v != null) max = Math.max(max, v.doubleValue());
        }
        return max;
    }

    private static boolean hasAny(Object c, String[] fields) {
        for (int i = 0; i < fields.length; i++) if (findField(c, fields[i]) != null) return true;
        return false;
    }

    /**
     * A declared Java field is not evidence. A group is active only when a module produced a
     * non-default value, a usable sample, or an explicit runtime status.
     */
    private static boolean groupActive(Object c, String group, String[] fields) {
        if (c == null) return false;
        if (hasMeaningfulValue(c, fields)) return true;
        if (GROUP_SHAPE.equals(group))
            return anyStatus(
                    c, new String[] {"shapeKey", "trendKey", "stateMode", "deepFeatureStatus"});
        if (GROUP_SEQUENCE.equals(group))
            return positiveSample(
                            c,
                            new String[] {
                                "historicalPriorSample",
                                "evidenceForestSample",
                                "sequenceHistorySize"
                            })
                    || anyStatus(c, new String[] {"historicalPriorStatus", "evidenceForestStatus"});
        if (GROUP_CAUSAL.equals(group))
            return positiveSample(c, new String[] {"similarWindowSample", "forwardReasonSample"})
                    || anyStatus(
                            c,
                            new String[] {
                                "v20Status",
                                "dynamicCauseStatus",
                                "positionNeedKey",
                                "needSupplyMatchKey",
                                "causeAblationKey",
                                "dynamicCausalSummary"
                            });
        if (GROUP_TEACHER.equals(group))
            return positiveSample(
                            c,
                            new String[] {
                                "teacherNumberSample",
                                "teacherMatrixSample",
                                "teacherMatrixStateSample",
                                "teacherMatrixComboSample"
                            })
                    || readInt(c, "teacherMatrixApplied", 0) == 1
                    || anyStatus(
                            c,
                            new String[] {
                                "teacherNumberStatus",
                                "teacherStatus",
                                "teacherMatrixStatus",
                                "teacherFieldVersion"
                            });
        if (GROUP_COUNTERFACTUAL.equals(group))
            return anyStatus(
                    c,
                    new String[] {
                        "counterfactualReasonKey",
                        "counterfactualSummary",
                        "causeAblationKey",
                        "causeAblationSummary"
                    });
        if (GROUP_DYNAMIC.equals(group))
            return positiveSample(
                            c,
                            new String[] {
                                "dynamicClusterSample",
                                "adaptiveClusterSample",
                                "promotionClusterSample"
                            })
                    || anyStatus(
                            c,
                            new String[] {
                                "dynamicClusterStatus",
                                "adaptiveJudgeStatus",
                                "shadowPromotionStatus",
                                "promotionClusterStatus",
                                "dynamicWindowStateKey"
                            });
        if (GROUP_CALIBRATION.equals(group))
            return positiveSample(
                            c,
                            new String[] {
                                "calibrationSample",
                                "calibrationTotalSample",
                                "posteriorSample",
                                "microWalkForwardSample"
                            })
                    || readInt(c, "calibrationReady", 0) == 1
                    || anyStatus(
                            c,
                            new String[] {
                                "calibrationStatus",
                                "posteriorMatchedLevel",
                                "microWalkForwardStatus",
                                "oosStatus"
                            });
        if (GROUP_RISK.equals(group))
            return anyStatus(
                    c,
                    new String[] {
                        "ruleHealthStatus",
                        "shadowPromotionStatus",
                        "dynamicGateQualityStatus",
                        "hardBlockSource",
                        "freezeReason"
                    });
        return false;
    }

    private static boolean hasMeaningfulValue(Object c, String[] fields) {
        if (fields == null) return false;
        for (int i = 0; i < fields.length; i++) {
            Field f = findField(c, fields[i]);
            if (f == null) continue;
            try {
                Object v = f.get(c);
                if (v instanceof Number && Math.abs(((Number) v).doubleValue()) > 0.000000001d)
                    return true;
                if (v instanceof Boolean && ((Boolean) v).booleanValue()) return true;
                if (v instanceof String && meaningfulStatus((String) v)) return true;
            } catch (Throwable ignore) {
            }
        }
        return false;
    }

    private static boolean positiveSample(Object c, String[] fields) {
        if (fields == null) return false;
        for (int i = 0; i < fields.length; i++) if (readInt(c, fields[i], 0) > 0) return true;
        return false;
    }

    private static boolean anyStatus(Object c, String[] fields) {
        if (fields == null) return false;
        for (int i = 0; i < fields.length; i++)
            if (meaningfulStatus(readString(c, fields[i], ""))) return true;
        return false;
    }

    private static boolean meaningfulStatus(String raw) {
        String v = safe(raw).trim().toUpperCase(Locale.US);
        if (v.length() == 0) return false;
        if ("NONE".equals(v)
                || "UNKNOWN".equals(v)
                || "NOT_RUN".equals(v)
                || "NO_SAMPLE".equals(v)
                || "UNAVAILABLE".equals(v)
                || "PENDING".equals(v)
                || "N/A".equals(v)
                || "NULL".equals(v)) return false;
        return true;
    }

    private static double normalize(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) return 0.0d;
        if (v > 1.0d) v /= 100.0d;
        if (v < 0.0d) return 0.0d;
        return clamp01(v);
    }

    private static Double readDoubleNullable(Object c, String field) {
        try {
            Field f = findField(c, field);
            if (f == null) return null;
            Object v = f.get(c);
            if (v instanceof Number) return Double.valueOf(((Number) v).doubleValue());
            if (v != null) return Double.valueOf(Double.parseDouble(String.valueOf(v)));
        } catch (Throwable ignore) {
        }
        return null;
    }

    private static int readInt(Object c, String field, int def) {
        try {
            Field f = findField(c, field);
            if (f == null) return def;
            Object v = f.get(c);
            if (v instanceof Number) return ((Number) v).intValue();
            return Integer.parseInt(String.valueOf(v));
        } catch (Throwable e) {
            return def;
        }
    }

    private static String readString(Object c, String field, String def) {
        try {
            Field f = findField(c, field);
            if (f == null) return def;
            Object v = f.get(c);
            return v == null ? def : String.valueOf(v);
        } catch (Throwable e) {
            return def;
        }
    }

    private static void writeDouble(Object c, String field, double value) {
        try {
            Field f = findField(c, field);
            if (f != null) f.setDouble(c, value);
        } catch (Throwable ignore) {
        }
    }

    private static void writeInt(Object c, String field, int value) {
        try {
            Field f = findField(c, field);
            if (f != null) f.setInt(c, value);
        } catch (Throwable ignore) {
        }
    }

    private static void writeString(Object c, String field, String value) {
        try {
            Field f = findField(c, field);
            if (f != null) f.set(c, safe(value));
        } catch (Throwable ignore) {
        }
    }

    private static void appendString(Object c, String field, String value) {
        if (value == null || value.length() == 0) return;
        String old = readString(c, field, "");
        if (old.indexOf(value) >= 0) return;
        writeString(c, field, old.length() == 0 ? value : old + "；" + value);
    }

    private static Field findField(Object c, String name) {
        if (c == null || name == null) return null;
        Class<?> type = c instanceof Class ? (Class<?>) c : c.getClass();
        synchronized (FIELD_CACHE) {
            Map<String, Field> byName = FIELD_CACHE.get(type);
            if (byName == null) {
                byName = new HashMap<String, Field>();
                try {
                    Field[] all = type.getFields();
                    for (int i = 0; i < all.length; i++) {
                        try {
                            all[i].setAccessible(true);
                        } catch (Throwable ignore) {
                        }
                        byName.put(all[i].getName(), all[i]);
                    }
                } catch (Throwable ignore) {
                }
                FIELD_CACHE.put(type, byName);
            }
            return byName.get(name);
        }
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static double clamp01(double v) {
        return v < 0.0d ? 0.0d : (v > 1.0d ? 1.0d : v);
    }

    private static String f(double v) {
        return String.valueOf(Math.round(v * 1000.0d) / 1000.0d);
    }

    public static final String B07R3_CONTRACT_FULLBLOOD_MARKER = "B07R3_EVIDENCE_FIELD_CONTRACT";

    public static final class FieldRule {
        public final String field;
        public final String group;
        public final String semantic;
        public final double min;
        public final double max;
        public final boolean maturity;

        FieldRule(String f, String g, String s, double lo, double hi, boolean m) {
            field = f;
            group = g;
            semantic = s;
            min = lo;
            max = hi;
            maturity = m;
        }

        public String text() {
            return field
                    + "@"
                    + group
                    + "#"
                    + semantic
                    + "["
                    + min
                    + ","
                    + max
                    + "]#maturity="
                    + maturity;
        }
    }

    public static final class ContractAudit {
        public int fieldsSeen, fieldsUsable, outOfRange, missingGroups;
        public double supportMean, riskMean, confidence;
        public String dominantGroup = "NONE", fingerprint = "";

        public String summary() {
            return "fields="
                    + fieldsUsable
                    + "/"
                    + fieldsSeen
                    + "|range="
                    + outOfRange
                    + "|missingGroups="
                    + missingGroups
                    + "|support="
                    + fmt4R3(supportMean)
                    + "|risk="
                    + fmt4R3(riskMean)
                    + "|confidence="
                    + fmt4R3(confidence)
                    + "|dominant="
                    + dominantGroup
                    + "|hash="
                    + fingerprint;
        }
    }

    public static List<FieldRule> b07R3FieldRules() {
        ArrayList<FieldRule> x = new ArrayList<FieldRule>();
        x.add(new FieldRule("shapeScore", "SHAPE_TREND", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("shapeConfidence", "SHAPE_TREND", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("patternScore", "SHAPE_TREND", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("patternConfidence", "SHAPE_TREND", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("trendScore", "SHAPE_TREND", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("trendConfidence", "SHAPE_TREND", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("trendLifeScore", "SHAPE_TREND", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("stateConfidence", "SHAPE_TREND", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("windowConsistency", "SHAPE_TREND", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("windowConsensusScore", "SHAPE_TREND", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("breakRecoveryScore", "SHAPE_TREND", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("recoveryScore", "SHAPE_TREND", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("cycleScore", "SHAPE_TREND", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("rhythmScore", "SHAPE_TREND", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("positionStrength", "SHAPE_TREND", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("deepFeatureScore", "SHAPE_TREND", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("deepFeatureConfidence", "SHAPE_TREND", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("deepStateScore", "SHAPE_TREND", "SUPPORT", 0.0d, 1.0d, false));
        x.add(
                new FieldRule(
                        "sequenceProbability",
                        "SEQUENCE_TRANSITION",
                        "SUPPORT",
                        0.0d,
                        1.0d,
                        false));
        x.add(
                new FieldRule(
                        "sequenceDistributionP",
                        "SEQUENCE_TRANSITION",
                        "SUPPORT",
                        0.0d,
                        1.0d,
                        false));
        x.add(new FieldRule("sequenceScore", "SEQUENCE_TRANSITION", "SUPPORT", 0.0d, 1.0d, false));
        x.add(
                new FieldRule(
                        "markovProbability", "SEQUENCE_TRANSITION", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("markovScore", "SEQUENCE_TRANSITION", "SUPPORT", 0.0d, 1.0d, false));
        x.add(
                new FieldRule(
                        "transitionScore", "SEQUENCE_TRANSITION", "SUPPORT", 0.0d, 1.0d, false));
        x.add(
                new FieldRule(
                        "transitionConfidence",
                        "SEQUENCE_TRANSITION",
                        "SUPPORT",
                        0.0d,
                        1.0d,
                        false));
        x.add(
                new FieldRule(
                        "numberMoveScore", "SEQUENCE_TRANSITION", "SUPPORT", 0.0d, 1.0d, false));
        x.add(
                new FieldRule(
                        "positionMoveScore", "SEQUENCE_TRANSITION", "SUPPORT", 0.0d, 1.0d, false));
        x.add(
                new FieldRule(
                        "historicalPriorProbability",
                        "SEQUENCE_TRANSITION",
                        "SUPPORT",
                        0.0d,
                        1.0d,
                        false));
        x.add(
                new FieldRule(
                        "historicalPriorConfidence",
                        "SEQUENCE_TRANSITION",
                        "SUPPORT",
                        0.0d,
                        1.0d,
                        false));
        x.add(
                new FieldRule(
                        "evidenceForestProbability",
                        "SEQUENCE_TRANSITION",
                        "SUPPORT",
                        0.0d,
                        1.0d,
                        false));
        x.add(
                new FieldRule(
                        "evidenceForestP", "SEQUENCE_TRANSITION", "SUPPORT", 0.0d, 1.0d, false));
        x.add(
                new FieldRule(
                        "evidenceForestConfidence",
                        "SEQUENCE_TRANSITION",
                        "SUPPORT",
                        0.0d,
                        1.0d,
                        false));
        x.add(
                new FieldRule(
                        "transitionRelationScore",
                        "SEQUENCE_TRANSITION",
                        "SUPPORT",
                        0.0d,
                        1.0d,
                        false));
        x.add(
                new FieldRule(
                        "numberBehaviorScore",
                        "SEQUENCE_TRANSITION",
                        "SUPPORT",
                        0.0d,
                        1.0d,
                        false));
        x.add(new FieldRule("causalScore", "CAUSAL_POSITION", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("causalConfidence", "CAUSAL_POSITION", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("causeScore", "CAUSAL_POSITION", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("causeConfidence", "CAUSAL_POSITION", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("positionCauseScore", "CAUSAL_POSITION", "SUPPORT", 0.0d, 1.0d, false));
        x.add(
                new FieldRule(
                        "positionHealthScore", "CAUSAL_POSITION", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("dynamicCauseScore", "CAUSAL_POSITION", "SUPPORT", 0.0d, 1.0d, false));
        x.add(
                new FieldRule(
                        "dynamicCauseConfidence", "CAUSAL_POSITION", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("needSupplyScore", "CAUSAL_POSITION", "SUPPORT", 0.0d, 1.0d, false));
        x.add(
                new FieldRule(
                        "needSupplyMatchScore", "CAUSAL_POSITION", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("positionNeedScore", "CAUSAL_POSITION", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("numberSupplyScore", "CAUSAL_POSITION", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("causeChainScore", "CAUSAL_POSITION", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("reasonCausalScore", "CAUSAL_POSITION", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("associationScore", "CAUSAL_POSITION", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("causalLift", "CAUSAL_POSITION", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("causeAblationScore", "CAUSAL_POSITION", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("teacherScore", "TEACHER_LEARNING", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("teacherConfidence", "TEACHER_LEARNING", "SUPPORT", 0.0d, 1.0d, false));
        x.add(
                new FieldRule(
                        "teacherMatrixScore", "TEACHER_LEARNING", "SUPPORT", 0.0d, 1.0d, false));
        x.add(
                new FieldRule(
                        "teacherNumberScore", "TEACHER_LEARNING", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("teacherHitRate", "TEACHER_LEARNING", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("teacherRecentRate", "TEACHER_LEARNING", "SUPPORT", 0.0d, 1.0d, false));
        x.add(
                new FieldRule(
                        "teacherMatrixConfidence",
                        "TEACHER_LEARNING",
                        "SUPPORT",
                        0.0d,
                        1.0d,
                        false));
        x.add(
                new FieldRule(
                        "teacherEvidenceContribution",
                        "TEACHER_LEARNING",
                        "SUPPORT",
                        0.0d,
                        1.0d,
                        false));
        x.add(
                new FieldRule(
                        "teacherMatrixEvidenceContribution",
                        "TEACHER_LEARNING",
                        "SUPPORT",
                        0.0d,
                        1.0d,
                        false));
        x.add(
                new FieldRule(
                        "teacherNumberConfidence",
                        "TEACHER_LEARNING",
                        "SUPPORT",
                        0.0d,
                        1.0d,
                        false));
        x.add(new FieldRule("teacherSample", "TEACHER_LEARNING", "SAMPLE", 0.0d, 10000.0d, true));
        x.add(
                new FieldRule(
                        "teacherMatrixSample", "TEACHER_LEARNING", "SAMPLE", 0.0d, 10000.0d, true));
        x.add(
                new FieldRule(
                        "teacherNumberSample", "TEACHER_LEARNING", "SAMPLE", 0.0d, 10000.0d, true));
        x.add(
                new FieldRule(
                        "teacherEffectiveSample",
                        "TEACHER_LEARNING",
                        "SAMPLE",
                        0.0d,
                        10000.0d,
                        true));
        x.add(new FieldRule("counterRisk", "COUNTERFACTUAL", "RISK", 0.0d, 1.0d, false));
        x.add(new FieldRule("counterEvidenceScore", "COUNTERFACTUAL", "RISK", 0.0d, 1.0d, false));
        x.add(new FieldRule("counterfactualRisk", "COUNTERFACTUAL", "RISK", 0.0d, 1.0d, false));
        x.add(
                new FieldRule(
                        "counterfactualWhyNotRisk", "COUNTERFACTUAL", "RISK", 0.0d, 1.0d, false));
        x.add(
                new FieldRule(
                        "counterfactualRiskScore", "COUNTERFACTUAL", "RISK", 0.0d, 1.0d, false));
        x.add(new FieldRule("whyNotRisk", "COUNTERFACTUAL", "RISK", 0.0d, 1.0d, false));
        x.add(new FieldRule("ablationRisk", "COUNTERFACTUAL", "RISK", 0.0d, 1.0d, false));
        x.add(new FieldRule("falseCauseRisk", "COUNTERFACTUAL", "RISK", 0.0d, 1.0d, false));
        x.add(
                new FieldRule(
                        "counterfactualSupport", "COUNTERFACTUAL", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("counterSupport", "COUNTERFACTUAL", "SUPPORT", 0.0d, 1.0d, false));
        x.add(
                new FieldRule(
                        "counterfactualConfidence",
                        "COUNTERFACTUAL",
                        "SUPPORT",
                        0.0d,
                        1.0d,
                        false));
        x.add(
                new FieldRule(
                        "counterEvidenceSupport", "COUNTERFACTUAL", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("ablationSupport", "COUNTERFACTUAL", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("contrastiveSupport", "COUNTERFACTUAL", "SUPPORT", 0.0d, 1.0d, false));
        x.add(
                new FieldRule(
                        "counterfactualRankScore", "COUNTERFACTUAL", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("reasonWilsonScore", "COUNTERFACTUAL", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("dynamicWindowScore", "DYNAMIC_CLUSTER", "SUPPORT", 0.0d, 1.0d, false));
        x.add(
                new FieldRule(
                        "dynamicClusterScore", "DYNAMIC_CLUSTER", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("clusterConfidence", "DYNAMIC_CLUSTER", "SUPPORT", 0.0d, 1.0d, false));
        x.add(
                new FieldRule(
                        "dynamicStateConfidence", "DYNAMIC_CLUSTER", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("regimeQuality", "DYNAMIC_CLUSTER", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("promotionScore", "DYNAMIC_CLUSTER", "SUPPORT", 0.0d, 1.0d, false));
        x.add(
                new FieldRule(
                        "shadowPromotionScore", "DYNAMIC_CLUSTER", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("adaptiveScore", "DYNAMIC_CLUSTER", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("adaptiveConfidence", "DYNAMIC_CLUSTER", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("domainConfidence", "DYNAMIC_CLUSTER", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("familyQuality", "DYNAMIC_CLUSTER", "SUPPORT", 0.0d, 1.0d, false));
        x.add(
                new FieldRule(
                        "dynamicClusterHitRate", "DYNAMIC_CLUSTER", "SUPPORT", 0.0d, 1.0d, false));
        x.add(
                new FieldRule(
                        "dynamicClusterRecentHitRate",
                        "DYNAMIC_CLUSTER",
                        "SUPPORT",
                        0.0d,
                        1.0d,
                        false));
        x.add(
                new FieldRule(
                        "dynamicClusterConfidence",
                        "DYNAMIC_CLUSTER",
                        "SUPPORT",
                        0.0d,
                        1.0d,
                        false));
        x.add(
                new FieldRule(
                        "adaptiveClusterConfidence",
                        "DYNAMIC_CLUSTER",
                        "SUPPORT",
                        0.0d,
                        1.0d,
                        false));
        x.add(new FieldRule("adaptiveCauseScore", "DYNAMIC_CLUSTER", "SUPPORT", 0.0d, 1.0d, false));
        x.add(
                new FieldRule(
                        "adaptivePromoteScore", "DYNAMIC_CLUSTER", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("riskScore", "RISK_HEALTH", "RISK", 0.0d, 1.0d, false));
        x.add(new FieldRule("dynamicRisk", "RISK_HEALTH", "RISK", 0.0d, 1.0d, false));
        x.add(new FieldRule("driftRisk", "RISK_HEALTH", "RISK", 0.0d, 1.0d, false));
        x.add(new FieldRule("falseStrongRisk", "RISK_HEALTH", "RISK", 0.0d, 1.0d, false));
        x.add(new FieldRule("falseSeedRisk", "RISK_HEALTH", "RISK", 0.0d, 1.0d, false));
        x.add(new FieldRule("teacherRisk", "RISK_HEALTH", "RISK", 0.0d, 1.0d, false));
        x.add(new FieldRule("teacherMatrixRisk", "RISK_HEALTH", "RISK", 0.0d, 1.0d, false));
        x.add(new FieldRule("thirdRisk", "RISK_HEALTH", "RISK", 0.0d, 1.0d, false));
        x.add(new FieldRule("thirdNumberRisk", "RISK_HEALTH", "RISK", 0.0d, 1.0d, false));
        x.add(new FieldRule("ruleHealthRisk", "RISK_HEALTH", "RISK", 0.0d, 1.0d, false));
        x.add(new FieldRule("shadowRisk", "RISK_HEALTH", "RISK", 0.0d, 1.0d, false));
        x.add(new FieldRule("freezeRisk", "RISK_HEALTH", "RISK", 0.0d, 1.0d, false));
        x.add(new FieldRule("comboConflict", "RISK_HEALTH", "RISK", 0.0d, 1.0d, false));
        x.add(new FieldRule("conflictLevel", "RISK_HEALTH", "RISK", 0.0d, 1.0d, false));
        x.add(new FieldRule("deepFeatureRisk", "RISK_HEALTH", "RISK", 0.0d, 1.0d, false));
        x.add(new FieldRule("evidenceRisk", "RISK_HEALTH", "RISK", 0.0d, 1.0d, false));
        x.add(new FieldRule("posteriorDrift", "RISK_HEALTH", "RISK", 0.0d, 1.0d, false));
        x.add(new FieldRule("calibrationError", "RISK_HEALTH", "RISK", 0.0d, 1.0d, false));
        x.add(new FieldRule("associationConflictRisk", "RISK_HEALTH", "RISK", 0.0d, 1.0d, false));
        x.add(
                new FieldRule(
                        "calibrationConfidence", "OOS_CALIBRATION", "SUPPORT", 0.0d, 1.0d, false));
        x.add(
                new FieldRule(
                        "posteriorConfidence", "OOS_CALIBRATION", "SUPPORT", 0.0d, 1.0d, false));
        x.add(
                new FieldRule(
                        "posteriorWilsonLower", "OOS_CALIBRATION", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("familyWilsonLower", "OOS_CALIBRATION", "SUPPORT", 0.0d, 1.0d, false));
        x.add(
                new FieldRule(
                        "microWalkForwardConfidence",
                        "OOS_CALIBRATION",
                        "SUPPORT",
                        0.0d,
                        1.0d,
                        false));
        x.add(new FieldRule("oosReliability", "OOS_CALIBRATION", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("oosConfidence", "OOS_CALIBRATION", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("calibrationReady", "OOS_CALIBRATION", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("posteriorMean", "OOS_CALIBRATION", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("calibrationBrier", "OOS_CALIBRATION", "SUPPORT", 0.0d, 1.0d, false));
        x.add(new FieldRule("calibrationEce", "OOS_CALIBRATION", "SUPPORT", 0.0d, 1.0d, false));
        x.add(
                new FieldRule(
                        "calibrationSample", "OOS_CALIBRATION", "SAMPLE", 0.0d, 10000.0d, true));
        x.add(new FieldRule("posteriorSample", "OOS_CALIBRATION", "SAMPLE", 0.0d, 10000.0d, true));
        x.add(new FieldRule("familySample", "OOS_CALIBRATION", "SAMPLE", 0.0d, 10000.0d, true));
        x.add(new FieldRule("domainSample", "OOS_CALIBRATION", "SAMPLE", 0.0d, 10000.0d, true));
        return x;
    }

    public static ContractAudit auditCandidateR3(Object candidate) {
        ContractAudit out = new ContractAudit();
        java.util.LinkedHashMap<String, double[]> group =
                new java.util.LinkedHashMap<String, double[]>();
        List<FieldRule> rules = b07R3FieldRules();
        StringBuilder raw = new StringBuilder(12000);
        for (int i = 0; i < rules.size(); i++) {
            FieldRule r = rules.get(i);
            out.fieldsSeen++;
            Double value = readNumberR3(candidate, r.field);
            if (value == null) continue;
            double v = value.doubleValue();
            if (Double.isNaN(v) || Double.isInfinite(v) || v < r.min || v > r.max) {
                out.outOfRange++;
                continue;
            }
            out.fieldsUsable++;
            double n = "SAMPLE".equals(r.semantic) ? Math.min(1.0d, v / 160.0d) : normalizeR3(v);
            double[] a = group.get(r.group);
            if (a == null) {
                a = new double[4];
                group.put(r.group, a);
            }
            if ("RISK".equals(r.semantic)) {
                a[1] += n;
                a[3]++;
            } else {
                a[0] += n;
                a[2]++;
            }
            raw.append(r.field).append('=').append(fmt4R3(n)).append('|');
        }
        String[] canonical =
                new String[] {
                    "SHAPE_TREND",
                    "SEQUENCE_TRANSITION",
                    "CAUSAL_POSITION",
                    "TEACHER_LEARNING",
                    "COUNTERFACTUAL",
                    "DYNAMIC_CLUSTER",
                    "RISK_HEALTH",
                    "OOS_CALIBRATION"
                };
        double support = 0, risk = 0, weight = 0, best = -2;
        for (int i = 0; i < canonical.length; i++) {
            double[] a = group.get(canonical[i]);
            if (a == null) {
                out.missingGroups++;
                continue;
            }
            double sp = a[2] <= 0 ? 0 : a[0] / a[2],
                    rp = a[3] <= 0 ? 0 : a[1] / a[3],
                    w =
                            ("CAUSAL_POSITION".equals(canonical[i])
                                            || "OOS_CALIBRATION".equals(canonical[i]))
                                    ? 1.15d
                                    : 1.0d;
            support += sp * w;
            risk += rp * w;
            weight += w;
            if (sp - rp > best) {
                best = sp - rp;
                out.dominantGroup = canonical[i];
            }
        }
        out.supportMean = weight <= 0 ? 0 : clamp01(support / weight);
        out.riskMean = weight <= 0 ? 1 : clamp01(risk / weight);
        out.confidence =
                clamp01(
                        0.55d * ((double) out.fieldsUsable / Math.max(1, out.fieldsSeen))
                                + 0.25d * (1.0d - out.riskMean)
                                + 0.20d * (1.0d - out.missingGroups / 8.0d));
        out.fingerprint = shaR3(raw.toString() + "|" + out.dominantGroup);
        return out;
    }

    public static String b07R3ContractManifest() {
        StringBuilder s = new StringBuilder(24000);
        List<FieldRule> x = b07R3FieldRules();
        for (int i = 0; i < x.size(); i++) s.append(x.get(i).text()).append('\n');
        return s.toString();
    }

    public static String b07R3ContractHash() {
        return shaR3(b07R3ContractManifest());
    }

    private static Double readNumberR3(Object o, String name) {
        if (o == null) return null;
        try {
            Field f = o.getClass().getField(name);
            Object v = f.get(o);
            return v instanceof Number ? Double.valueOf(((Number) v).doubleValue()) : null;
        } catch (Throwable e) {
            return null;
        }
    }

    private static double normalizeR3(double v) {
        if (v > 1.5d && v <= 100.0d) v /= 100.0d;
        return clamp01(v);
    }

    private static String shaR3(String raw) {
        try {
            MessageDigest md = MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest(safe(raw).getBytes("UTF-8"));
            StringBuilder s = new StringBuilder(64);
            for (int i = 0; i < b.length; i++) {
                String h = Integer.toHexString(b[i] & 255);
                if (h.length() == 1) s.append('0');
                s.append(h);
            }
            return s.toString();
        } catch (Throwable e) {
            return Integer.toHexString(safe(raw).hashCode());
        }
    }

    private static String fmt4R3(double v) {
        return String.format(Locale.US, "%.4f", v);
    }
}
