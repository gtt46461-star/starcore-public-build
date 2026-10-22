package com.ethk3.novaonepro;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/*
 * TeacherMatrixEngine
 *
 * 老版高胜率主链恢复满血版：
 *
 * 核心原则：
 * 1. 每期开奖后只学习刚开奖这一期，不一次性硬跑50期。
 * 2. 每期学习 10位置 × 10号码 = 100 个号码反事实矩阵格。
 * 3. 每个位置同时生成一个 K3 组合画像，用于下一期实时分析反哺。
 * 4. 每个位置补充位置关联画像，不再只是中/不中流水账。
 * 5. 学习窗口统一使用 12/24/48/96/120/240，贴合实时主链。
 * 6. 自动后台小批默认1期，最大3期，防止手机卡死。
 * 7. 实时分析优先，后台批量学习遇到实时状态立即让路。
 * 8. 不下注，不结算，不写真实盈亏，不改 trades.pnl。
 * 9. 不把 HOLD / SHADOW / 教师样本 混入真实统计。
 * 10. 不写真实校准桶 updateCalibration，教师矩阵只写画像、审计、解释和快照。
 * 11. 真实统计、真实校准继续只由 SettlementEngine + DbStore.trades + REAL_TRADE 负责。
 * 12. 恢复老版实时反哺：TeacherMatrix 可对候选做小幅概率/门槛/风险修正。
 * 13. TeacherMatrix 为空时中性，不拦截、不降权、不转 SHADOW。
 *
 * Java7 / AIDE / Android 原生兼容。
 */
public final class TeacherMatrixEngine {
    public static final String OUMAN_GALAXY_REBUILD_VERSION =
            AppConfig.OUMAN_GALAXY_REBUILD_VERSION;

    private static final int POSITION_MIN = 1;
    private static final int POSITION_MAX = 10;
    private static final int NUMBER_MIN = 1;
    private static final int NUMBER_MAX = 10;

    private static final int REALTIME_HISTORY_LIMIT = 240;
    private static final int MIN_HISTORY_FOR_STRONG_LEARN = 60;
    private static final int MIN_HISTORY_FOR_ANY_LEARN = 12;

    private static final int AUTO_BATCH_MIN = 1;
    private static final int AUTO_BATCH_DEFAULT = 1;
    private static final int AUTO_BATCH_MAX = 3;
    private static final int MANUAL_BATCH_MAX = 24;

    private static final long AUTO_BATCH_BUDGET_MS = 1600L;
    private static final long MANUAL_BATCH_BUDGET_MS = 3500L;
    private static final long REALTIME_DUPLICATE_SKIP_MS = 55L * 1000L;
    private static final long SNAPSHOT_VALID_MS = 6L * 60L * 60L * 1000L;
    private static final long REALTIME_PRIORITY_COUNTDOWN_MS = 18000L;

    private static final double BASE_NUMBER_P = 0.100d;
    private static final double BASE_K3_P = 0.300d;

    private static final double GATE_BASE = 0.378d;
    private static final double GATE_MIN = 0.365d;
    private static final double GATE_MAX = 0.438d;

    private static final double REALTIME_PROB_ADJUST_MAX = 0.018d;
    private static final double REALTIME_GATE_ADJUST_MAX = 0.012d;
    private static final double REALTIME_RISK_PENALTY_MAX = 0.026d;

    private static final int SUMMARY_LIMIT = 900;

    private static final String TASK_REALTIME = "TEACHER_MATRIX_REALTIME";
    private static final String TASK_BATCH = "TEACHER_MATRIX_BATCH";
    private static final String TASK_TICK = "TEACHER_MATRIX_TICK";
    private static final String TASK_AUTO = "TEACHER_MATRIX_AUTO";
    private static final String TASK_AUTO_BATCH_LEGACY = "TEACHER_MATRIX_AUTO_BATCH";
    private static final String TASK_MANUAL_SEGMENT_LEGACY = "TEACHER_MATRIX_MANUAL_SEGMENT";

    private static final Object NO_METHOD = new Object();

    public TeacherMatrixEngine() {}

    /* =========================
     * 实时反哺入口
     * ========================= */

    public int applyRealtimeBoost(DbStore store, List<Models.ComboCandidate> candidates) {
        return applyTeacherMatrixToCandidates(store, candidates);
    }

    public int applyToCandidates(DbStore store, List<Models.ComboCandidate> candidates) {
        return applyTeacherMatrixToCandidates(store, candidates);
    }

    public int applyTeacherMatrixToCandidates(
            DbStore store, List<Models.ComboCandidate> candidates) {
        if (store == null || candidates == null || candidates.size() == 0) {
            return 0;
        }

        int applied = 0;

        for (int i = 0; i < candidates.size(); i++) {
            Models.ComboCandidate c = candidates.get(i);

            if (c == null) {
                continue;
            }

            if (applyTeacherMatrixToCandidate(store, c) != null) {
                if (readInt(c, "teacherMatrixApplied", 0) == 1) {
                    applied++;
                }
            }
        }

        sortCandidatesByTeacherMatrix(candidates);

        writeState(store, "teacher_matrix_realtime_apply_count", String.valueOf(applied));
        writeState(
                store,
                "teacher_matrix_realtime_apply_at",
                String.valueOf(System.currentTimeMillis()));
        writeState(
                store,
                "teacher_matrix_realtime_summary",
                "TeacherMatrix实时反哺完成：候选="
                        + candidates.size()
                        + "，命中画像="
                        + applied
                        + "；STRONG/READY小幅加分降门，RISK小幅加风险；空矩阵中性不拦截。");

        return applied;
    }

    public Models.ComboCandidate applyToCandidate(DbStore store, Models.ComboCandidate candidate) {
        return applyTeacherMatrixToCandidate(store, candidate);
    }

    public Models.ComboCandidate applyTeacherMatrixToCandidate(
            DbStore store, Models.ComboCandidate candidate) {
        if (candidate == null) {
            return null;
        }

        if (!isValidPosition(candidate.position)) {
            markNeutralCandidate(candidate, "TeacherMatrix中性：候选位置非法，跳过反哺");
            return candidate;
        }

        repairCandidateMask(candidate);

        TeacherProfile p = readTeacherProfileForCandidate(store, candidate);

        if (p == null || p.sample <= 0) {
            markNeutralCandidate(candidate, "TeacherMatrix中性：暂无匹配画像或样本不足，不加分、不降门、不拦截");
            return candidate;
        }

        applyTeacherProfile(candidate, p);

        return candidate;
    }

    private void sortCandidatesByTeacherMatrix(List<Models.ComboCandidate> candidates) {
        if (candidates == null || candidates.size() <= 1) {
            return;
        }

        try {
            Collections.sort(
                    candidates,
                    new Comparator<Models.ComboCandidate>() {
                        @Override
                        public int compare(Models.ComboCandidate a, Models.ComboCandidate b) {
                            double sa = teacherRealtimeScore(a);
                            double sb = teacherRealtimeScore(b);

                            if (sa < sb) {
                                return 1;
                            }

                            if (sa > sb) {
                                return -1;
                            }

                            if (a == null && b == null) {
                                return 0;
                            }

                            if (a == null) {
                                return 1;
                            }

                            if (b == null) {
                                return -1;
                            }

                            if (a.finalP < b.finalP) {
                                return 1;
                            }

                            if (a.finalP > b.finalP) {
                                return -1;
                            }

                            return a.position - b.position;
                        }
                    });
        } catch (Throwable e) {
        }
    }

    private double teacherRealtimeScore(Models.ComboCandidate c) {
        if (c == null) {
            return -999.0d;
        }

        double score = c.finalP;
        score += c.edge * 0.60d;
        score += c.rawP * 0.18d;

        int applied = readInt(c, "teacherMatrixApplied", 0);
        int sample = readInt(c, "teacherMatrixSample", 0);
        int comboSample = readInt(c, "teacherMatrixComboSample", 0);
        double hitRate = readDouble(c, "teacherMatrixHitRate", 0.0d);
        double comboHitRate = readDouble(c, "teacherMatrixRecentHitRate", 0.0d);
        double conf = readDouble(c, "teacherMatrixConfidence", 0.0d);
        double risk = readDouble(c, "teacherMatrixRiskPenalty", 0.0d);
        String status = readString(c, "teacherMatrixStatus", "");

        if (applied == 1 || sample > 0 || comboSample > 0) {
            score += hitRate * 0.020d;
            score += comboHitRate * 0.018d;
            score += conf * 0.012d;
            score -= risk * 0.080d;

            if ("STRONG".equals(status)) {
                score += 0.025d;
            } else if ("READY".equals(status)) {
                score += 0.014d;
            } else if ("RISK".equals(status)) {
                score -= 0.035d;
            }
        }

        return score;
    }

    private TeacherProfile readTeacherProfileForCandidate(DbStore store, Models.ComboCandidate c) {
        if (store == null || c == null) {
            return null;
        }

        TeacherProfile direct = buildProfileFromCandidateFields(c);

        if (direct != null && direct.sample > 0) {
            return direct;
        }

        Object obj;

        obj =
                invokeOptional(
                        store,
                        "getTeacherMatrixProfileForCandidate",
                        new Class[] {Models.ComboCandidate.class},
                        new Object[] {c});

        TeacherProfile p = profileFromObject(obj);

        if (p != null && p.sample > 0) {
            return p;
        }

        String stateKey = readString(c, "stateKey", "");
        if (stateKey.length() == 0) {
            stateKey = readString(c, "shapeKey", "");
        }

        int k3Mask = readInt(c, "k3Mask", c.k3Mask);

        obj =
                invokeOptional(
                        store,
                        "getTeacherMatrixProfile",
                        new Class[] {String.class, Integer.TYPE, Integer.TYPE},
                        new Object[] {
                            stateKey, Integer.valueOf(c.position), Integer.valueOf(k3Mask)
                        });

        p = profileFromObject(obj);

        if (p != null && p.sample > 0) {
            return p;
        }

        obj =
                invokeOptional(
                        store,
                        "getRuntimeProfile",
                        new Class[] {String.class, Integer.TYPE, Integer.TYPE},
                        new Object[] {
                            stateKey, Integer.valueOf(c.position), Integer.valueOf(k3Mask)
                        });

        p = profileFromObject(obj);

        if (p != null && p.sample > 0) {
            return p;
        }

        obj =
                invokeOptional(
                        store,
                        "getTeacherMatrixProfileByKey",
                        new Class[] {String.class},
                        new Object[] {stateKey});

        p = profileFromObject(obj);

        if (p != null && p.sample > 0) {
            return p;
        }

        return null;
    }

    private TeacherProfile buildProfileFromCandidateFields(Models.ComboCandidate c) {
        if (c == null) {
            return null;
        }

        int sample = readInt(c, "teacherMatrixSample", 0);
        int comboSample = readInt(c, "teacherMatrixComboSample", 0);
        int applied = readInt(c, "teacherMatrixApplied", 0);

        if (sample <= 0 && comboSample <= 0 && applied <= 0) {
            return null;
        }

        TeacherProfile p = new TeacherProfile();

        p.sample = Math.max(sample, comboSample);
        p.stateSample = readInt(c, "teacherMatrixStateSample", 0);
        p.comboSample = comboSample;
        p.positive = readInt(c, "teacherMatrixPositive", 0);
        p.negative = readInt(c, "teacherMatrixNegative", 0);
        p.hitRate = readDouble(c, "teacherMatrixHitRate", 0.0d);
        p.comboHitRate = readDouble(c, "teacherMatrixRecentHitRate", 0.0d);

        if (p.comboHitRate <= 0.0d) {
            p.comboHitRate = readDouble(c, "teacherMatrixComboHitRate", 0.0d);
        }

        p.confidence = readDouble(c, "teacherMatrixConfidence", 0.0d);
        p.probAdjust = readDouble(c, "teacherMatrixProbAdjust", 0.0d);
        p.gateAdjust = readDouble(c, "teacherMatrixGateAdjust", 0.0d);
        p.riskPenalty = readDouble(c, "teacherMatrixRiskPenalty", 0.0d);
        p.status = readString(c, "teacherMatrixStatus", "");
        p.summary = readString(c, "teacherMatrixSummary", "");

        normalizeTeacherProfile(p);

        return p;
    }

    private TeacherProfile profileFromObject(Object obj) {
        if (obj == null || obj == NO_METHOD) {
            return null;
        }

        if (obj instanceof TeacherProfile) {
            TeacherProfile p = (TeacherProfile) obj;
            normalizeTeacherProfile(p);
            return p;
        }

        TeacherProfile p = new TeacherProfile();

        p.sample =
                firstInt(
                        obj,
                        new String[] {
                            "sample",
                            "samples",
                            "sampleCount",
                            "teacherMatrixSample",
                            "stateSample",
                            "matrixSample"
                        },
                        0);

        p.stateSample =
                firstInt(
                        obj,
                        new String[] {"stateSample", "teacherMatrixStateSample", "profileSample"},
                        0);

        p.comboSample =
                firstInt(
                        obj,
                        new String[] {
                            "comboSample", "teacherMatrixComboSample", "k3Sample", "runtimeSample"
                        },
                        0);

        p.positive =
                firstInt(
                        obj,
                        new String[] {
                            "positive", "positiveCount", "hitCount", "teacherMatrixPositive"
                        },
                        0);

        p.negative =
                firstInt(
                        obj,
                        new String[] {
                            "negative", "negativeCount", "missCount", "teacherMatrixNegative"
                        },
                        0);

        p.hitRate =
                firstDouble(
                        obj,
                        new String[] {
                            "hitRate", "teacherMatrixHitRate", "recentHitRate", "profileHitRate"
                        },
                        0.0d);

        p.comboHitRate =
                firstDouble(
                        obj,
                        new String[] {
                            "comboHitRate",
                            "teacherMatrixRecentHitRate",
                            "teacherMatrixComboHitRate",
                            "k3HitRate"
                        },
                        0.0d);

        p.confidence =
                firstDouble(
                        obj,
                        new String[] {
                            "confidence",
                            "teacherMatrixConfidence",
                            "profileConfidence",
                            "teacherConfidence"
                        },
                        0.0d);

        p.probAdjust =
                firstDouble(
                        obj,
                        new String[] {
                            "probAdjust", "teacherMatrixProbAdjust", "probabilityAdjust", "pAdjust"
                        },
                        0.0d);

        p.gateAdjust =
                firstDouble(
                        obj,
                        new String[] {"gateAdjust", "teacherMatrixGateAdjust", "dynamicGateAdjust"},
                        0.0d);

        p.riskPenalty =
                firstDouble(
                        obj,
                        new String[] {
                            "riskPenalty", "teacherMatrixRiskPenalty", "risk", "profileRisk"
                        },
                        0.0d);

        p.status =
                firstString(
                        obj,
                        new String[] {
                            "status", "teacherMatrixStatus", "readyStatus", "profileStatus"
                        },
                        "");

        p.summary =
                firstString(
                        obj,
                        new String[] {
                            "summary", "teacherMatrixSummary", "profileSummary", "detail"
                        },
                        "");

        normalizeTeacherProfile(p);

        if (p.sample <= 0 && p.comboSample <= 0) {
            return null;
        }

        return p;
    }

    private void normalizeTeacherProfile(TeacherProfile p) {
        if (p == null) {
            return;
        }

        if (p.sample <= 0 && p.stateSample > 0) {
            p.sample = p.stateSample;
        }

        if (p.sample <= 0 && p.comboSample > 0) {
            p.sample = p.comboSample;
        }

        if (p.hitRate <= 0.0d && p.sample > 0 && p.positive > 0) {
            p.hitRate = ((double) p.positive) / ((double) Math.max(1, p.positive + p.negative));
        }

        if (p.comboHitRate <= 0.0d) {
            p.comboHitRate = p.hitRate;
        }

        if (p.confidence <= 0.0d) {
            double sampleConf = p.sample >= 120 ? 1.0d : ((double) p.sample) / 120.0d;
            double comboConf = p.comboSample >= 30 ? 1.0d : ((double) p.comboSample) / 30.0d;
            p.confidence = clamp01(sampleConf * 0.65d + comboConf * 0.35d);
        }

        p.hitRate = clamp01(p.hitRate);
        p.comboHitRate = clamp01(p.comboHitRate);
        p.confidence = clamp01(p.confidence);
        p.riskPenalty = clamp(p.riskPenalty, 0.0d, REALTIME_RISK_PENALTY_MAX);

        if (p.status == null) {
            p.status = "";
        }

        if (p.status.length() == 0) {
            if (p.sample >= 80 && p.hitRate >= 0.395d && p.confidence >= 0.480d) {
                p.status = "STRONG";
            } else if (p.sample >= 30 && p.hitRate >= 0.365d && p.confidence >= 0.340d) {
                p.status = "READY";
            } else if (p.sample >= 80
                    && p.hitRate > 0.0d
                    && p.hitRate < 0.285d
                    && p.confidence >= 0.340d) {
                p.status = "RISK";
            } else {
                p.status = "NEUTRAL";
            }
        }

        if (p.probAdjust == 0.0d) {
            if ("STRONG".equals(p.status)) {
                p.probAdjust = 0.014d;
            } else if ("READY".equals(p.status)) {
                p.probAdjust = 0.008d;
            } else if ("RISK".equals(p.status)) {
                p.probAdjust = -0.008d;
            }
        }

        if (p.gateAdjust == 0.0d) {
            if ("STRONG".equals(p.status)) {
                p.gateAdjust = -0.010d;
            } else if ("READY".equals(p.status)) {
                p.gateAdjust = -0.006d;
            } else if ("RISK".equals(p.status)) {
                p.gateAdjust = 0.010d;
            }
        }

        p.probAdjust = clamp(p.probAdjust, -REALTIME_PROB_ADJUST_MAX, REALTIME_PROB_ADJUST_MAX);
        p.gateAdjust = clamp(p.gateAdjust, -REALTIME_GATE_ADJUST_MAX, REALTIME_GATE_ADJUST_MAX);

        if ("RISK".equals(p.status) && p.riskPenalty <= 0.0d) {
            p.riskPenalty = 0.014d;
        }

        if (p.summary == null) {
            p.summary = "";
        }

        if (p.summary.length() == 0) {
            p.summary =
                    "TeacherMatrix画像：status="
                            + p.status
                            + "，sample="
                            + p.sample
                            + "，comboSample="
                            + p.comboSample
                            + "，hitRate="
                            + percent(p.hitRate)
                            + "，confidence="
                            + percent(p.confidence);
        }
    }

    private void applyTeacherProfile(Models.ComboCandidate c, TeacherProfile p) {
        if (c == null || p == null) {
            return;
        }

        double oldFinalP = c.finalP;
        double oldGate = c.dynamicGate;

        double adjust = p.probAdjust;
        double gateAdjust = p.gateAdjust;

        if ("STRONG".equals(p.status)) {
            adjust = Math.max(adjust, 0.010d);
            gateAdjust = Math.min(gateAdjust, -0.006d);
        } else if ("READY".equals(p.status)) {
            adjust = Math.max(adjust, 0.004d);
            gateAdjust = Math.min(gateAdjust, -0.003d);
        } else if ("RISK".equals(p.status)) {
            adjust = Math.min(adjust, -0.004d);
            gateAdjust = Math.max(gateAdjust, 0.004d);
        }

        adjust = clamp(adjust, -REALTIME_PROB_ADJUST_MAX, REALTIME_PROB_ADJUST_MAX);
        gateAdjust = clamp(gateAdjust, -REALTIME_GATE_ADJUST_MAX, REALTIME_GATE_ADJUST_MAX);

        if (c.finalP <= 0.0d && c.rawP > 0.0d) {
            c.finalP = c.rawP;
        }

        if (c.dynamicGate <= 0.0d) {
            c.dynamicGate = GATE_BASE;
        }

        c.finalP = clamp(c.finalP + adjust, 0.0d, 0.999d);
        c.dynamicGate = clamp(c.dynamicGate + gateAdjust, GATE_MIN, GATE_MAX);
        c.edge = c.finalP - c.dynamicGate;

        writeInt(c, "teacherMatrixApplied", 1);
        writeInt(c, "teacherMatrixSample", p.sample);
        writeInt(c, "teacherMatrixStateSample", p.stateSample);
        writeInt(c, "teacherMatrixComboSample", p.comboSample);
        writeInt(c, "teacherMatrixPositive", p.positive);
        writeInt(c, "teacherMatrixNegative", p.negative);

        writeDouble(c, "teacherMatrixHitRate", p.hitRate);
        writeDouble(c, "teacherMatrixRecentHitRate", p.comboHitRate);
        writeDouble(c, "teacherMatrixComboHitRate", p.comboHitRate);
        writeDouble(c, "teacherMatrixConfidence", p.confidence);
        writeDouble(c, "teacherMatrixProbAdjust", adjust);
        writeDouble(c, "teacherMatrixGateAdjust", gateAdjust);
        writeDouble(c, "teacherMatrixRiskPenalty", p.riskPenalty);

        writeString(c, "teacherMatrixStatus", p.status);
        writeString(
                c,
                "teacherMatrixSummary",
                "TeacherMatrix实时反哺："
                        + p.summary
                        + "；FinalP "
                        + percent(oldFinalP)
                        + "→"
                        + percent(c.finalP)
                        + "；Gate "
                        + percent(oldGate)
                        + "→"
                        + percent(c.dynamicGate)
                        + "；规则=小幅修正，不直接下注，不污染真实统计");

        appendCandidateReason(
                c,
                "TeacherMatrix实时反哺="
                        + p.status
                        + "，样本="
                        + p.sample
                        + "，命中率="
                        + percent(p.hitRate)
                        + "，置信="
                        + percent(p.confidence)
                        + "，修正P="
                        + signedPercent(adjust)
                        + "，修正门="
                        + signedPercent(gateAdjust));
    }

    private void markNeutralCandidate(Models.ComboCandidate c, String summary) {
        if (c == null) {
            return;
        }

        writeInt(c, "teacherMatrixApplied", 0);
        writeInt(c, "teacherMatrixSample", 0);
        writeInt(c, "teacherMatrixComboSample", 0);
        writeDouble(c, "teacherMatrixHitRate", 0.0d);
        writeDouble(c, "teacherMatrixRecentHitRate", 0.0d);
        writeDouble(c, "teacherMatrixConfidence", 0.0d);
        writeDouble(c, "teacherMatrixProbAdjust", 0.0d);
        writeDouble(c, "teacherMatrixGateAdjust", 0.0d);
        writeDouble(c, "teacherMatrixRiskPenalty", 0.0d);
        writeString(c, "teacherMatrixStatus", "");
        writeString(c, "teacherMatrixSummary", summary == null ? "TeacherMatrix中性" : summary);
    }

    private void appendCandidateReason(Models.ComboCandidate c, String add) {
        if (c == null || add == null || add.length() == 0) {
            return;
        }

        String old = c.reasonSummary == null ? "" : c.reasonSummary;

        if (old.indexOf(add) >= 0) {
            return;
        }

        if (old.length() == 0) {
            c.reasonSummary = add;
        } else {
            c.reasonSummary = old + "；" + add;
        }

        if (c.reasonSummary != null && c.reasonSummary.length() > SUMMARY_LIMIT) {
            c.reasonSummary = c.reasonSummary.substring(0, SUMMARY_LIMIT);
        }
    }

    private void repairCandidateMask(Models.ComboCandidate c) {
        if (c == null) {
            return;
        }

        if (c.k3Mask <= 0 && isValidNumber(c.n1) && isValidNumber(c.n2) && isValidNumber(c.n3)) {
            c.k3Mask = buildK3Mask(c.n1, c.n2, c.n3);
        }
    }

    /* =========================
     * AutoFetchService / LearningEngine 兼容入口
     * ========================= */

    public int learnLatest(DbStore store) {
        if (store == null) {
            return 0;
        }

        Models.Draw latest = null;

        try {
            latest = store.getLatestDraw();
        } catch (Throwable e) {
            latest = null;
        }

        if (!isValidDraw(latest)) {
            writeState(store, "teacher_matrix_status", "IDLE");
            writeState(store, "teacher_matrix_summary", "教师矩阵：未找到有效最新开奖，等待开奖入库。");
            writeState(store, "teacher_snapshot_status", "WAITING");
            writeState(store, "teacher_snapshot_summary", "教师快照等待：最新开奖尚未入库。");
            return 0;
        }

        return learnFromSettledDrawFullMatrix(store, latest);
    }

    public int learnLatestDraw(DbStore store) {
        return learnLatest(store);
    }

    public int runLatest(DbStore store) {
        return learnLatest(store);
    }

    public int learnLatestDraw(DbStore store, Models.Draw actualDraw) {
        return learnFromSettledDrawFullMatrix(store, actualDraw);
    }

    public int learnAfterDraw(DbStore store, Models.Draw actualDraw) {
        return learnFromSettledDrawFullMatrix(store, actualDraw);
    }

    public int learnAfterNewDraw(DbStore store, Models.Draw actualDraw) {
        return learnFromSettledDrawFullMatrix(store, actualDraw);
    }

    public int onDrawSaved(DbStore store, Models.Draw actualDraw) {
        return learnFromSettledDrawFullMatrix(store, actualDraw);
    }

    public int runAfterDraw(DbStore store, Models.Draw actualDraw) {
        return learnFromSettledDrawFullMatrix(store, actualDraw);
    }

    public int learnOneDraw(DbStore store, Models.Draw actualDraw) {
        return learnFromSettledDrawFullMatrix(store, actualDraw);
    }

    public int learnFromDraw(DbStore store, Models.Draw actualDraw) {
        return learnFromSettledDrawFullMatrix(store, actualDraw);
    }

    public int learnFromActualDraw(DbStore store, Models.Draw actualDraw) {
        return learnFromSettledDrawFullMatrix(store, actualDraw);
    }

    public int learn(DbStore store, Models.Draw actualDraw) {
        return learnFromSettledDrawFullMatrix(store, actualDraw);
    }

    public int runTick(DbStore store) {
        return runAutoLearnBatch(store, AUTO_BATCH_DEFAULT);
    }

    public int tick(DbStore store) {
        return runAutoLearnBatch(store, AUTO_BATCH_DEFAULT);
    }

    public int runAutoLearnBatch(DbStore store) {
        return runAutoLearnBatch(store, AUTO_BATCH_DEFAULT);
    }

    public int runAutoLearnBatch(DbStore store, int maxTargetCount) {
        return learnRecentBatch(store, normalizeAutoBatch(maxTargetCount));
    }

    public int learnAutoSmallBatch(DbStore store, int maxTargetCount) {
        return learnRecentBatch(store, normalizeAutoBatch(maxTargetCount));
    }

    public int runSmallBatch(DbStore store, int maxTargetCount) {
        return learnRecentBatch(store, normalizeAutoBatch(maxTargetCount));
    }

    private int normalizeAutoBatch(int count) {
        if (count <= 0) {
            count = AUTO_BATCH_DEFAULT;
        }

        if (count < AUTO_BATCH_MIN) {
            count = AUTO_BATCH_MIN;
        }

        if (count > AUTO_BATCH_MAX) {
            count = AUTO_BATCH_MAX;
        }

        return count;
    }

    /* =========================
     * 状态查询入口
     * ========================= */

    public String getStatusText(DbStore store) {
        if (store == null) {
            return "教师矩阵：数据库未连接";
        }

        try {
            String status = store.getState("teacher_matrix_status", "IDLE");
            String ready = store.getState("teacher_matrix_ready_status", "");
            String issue = store.getState("teacher_matrix_last_issue", "");
            String samples = store.getState("teacher_matrix_sample_count", "0");
            String profiles = store.getState("teacher_matrix_profile_count", "0");
            String conf = store.getState("teacher_matrix_last_confidence_text", "");
            String summary = store.getState("teacher_matrix_summary", "");

            StringBuilder sb = new StringBuilder();

            sb.append("教师矩阵：");
            sb.append(status);

            if (ready != null && ready.length() > 0) {
                sb.append("｜");
                sb.append(ready);
            }

            if (issue != null && issue.length() > 0) {
                sb.append("｜期=");
                sb.append(issue);
            }

            sb.append("｜样本=");
            sb.append(samples);
            sb.append("｜画像=");
            sb.append(profiles);

            if (conf != null && conf.length() > 0) {
                sb.append("｜置信=");
                sb.append(conf);
            }

            if (summary != null && summary.length() > 0) {
                sb.append("\n");
                sb.append(limit(summary, SUMMARY_LIMIT));
            }

            return sb.toString();
        } catch (Throwable e) {
            return "教师矩阵：状态读取异常";
        }
    }

    public boolean isReady(DbStore store) {
        if (store == null) {
            return false;
        }

        try {
            String status = store.getState("teacher_matrix_status", "");
            String ready = store.getState("teacher_matrix_ready", "0");
            String cells = store.getState("teacher_matrix_last_cells", "0");
            int c = parseInt(cells, 0);

            return "1".equals(ready) && c >= 100 && ("OK".equals(status) || "DONE".equals(status));
        } catch (Throwable e) {
            return false;
        }
    }

    /* =========================
     * 主学习入口
     * ========================= */

    public int learnFromSettledDrawFullMatrix(DbStore store, Models.Draw actualDraw) {
        return learnInternal(store, actualDraw, TASK_REALTIME, "", -1, true, true);
    }

    public int learnFromSettledDrawFullMatrix(
            DbStore store,
            Models.Draw actualDraw,
            String taskKey,
            String segmentKey,
            int targetIndex) {

        String k = taskKey == null ? "" : taskKey;

        boolean skipDuplicate =
                TASK_REALTIME.equals(k) || TASK_TICK.equals(k) || TASK_AUTO.equals(k);

        return learnInternal(
                store, actualDraw, taskKey, segmentKey, targetIndex, true, skipDuplicate);
    }

    public int learnRecentBatch(DbStore store, int maxTargetCount) {
        if (store == null) {
            return 0;
        }

        if (maxTargetCount <= 0) {
            maxTargetCount = AUTO_BATCH_DEFAULT;
        }

        if (maxTargetCount > MANUAL_BATCH_MAX) {
            maxTargetCount = MANUAL_BATCH_MAX;
        }

        long begin = System.currentTimeMillis();
        long budget =
                maxTargetCount <= AUTO_BATCH_MAX ? AUTO_BATCH_BUDGET_MS : MANUAL_BATCH_BUDGET_MS;

        if (shouldYieldToRealtime(store)) {
            writeState(store, "teacher_matrix_status", "IDLE");
            writeState(store, "teacher_matrix_summary", "教师矩阵后台小批量让路：实时分析优先，稍后继续。");
            return 0;
        }

        List<Models.Draw> raw = null;

        try {
            raw = store.getRecentDraws(maxTargetCount + REALTIME_HISTORY_LIMIT);
        } catch (Throwable e) {
            raw = null;
        }

        ArrayList<Models.Draw> draws = normalizeDraws(raw);

        if (draws == null || draws.size() < MIN_HISTORY_FOR_ANY_LEARN + 1) {
            writeState(store, "teacher_matrix_status", "WAITING");
            writeState(
                    store,
                    "teacher_matrix_summary",
                    "教师矩阵批量学习：历史不足，至少需要 " + MIN_HISTORY_FOR_ANY_LEARN + " 期。");
            writeState(store, "teacher_snapshot_status", "WAITING");
            writeState(store, "teacher_snapshot_summary", "教师快照等待：历史不足，暂不能生成稳定快照。");
            return 0;
        }

        int start = draws.size() - maxTargetCount;

        if (start < MIN_HISTORY_FOR_ANY_LEARN) {
            start = MIN_HISTORY_FOR_ANY_LEARN;
        }

        int learnedCells = 0;
        int learnedDraws = 0;
        Models.Draw lastLearned = null;
        boolean yielded = false;

        writeState(store, "teacher_matrix_status", "RUNNING");
        writeState(store, "teacher_matrix_running", "1");
        writeState(
                store,
                "teacher_matrix_summary",
                "教师矩阵后台小批量启动：最多=" + (draws.size() - start) + "期；每次1~3期，实时出号优先。");

        for (int i = start; i < draws.size(); i++) {
            if (shouldYieldToRealtime(store)) {
                yielded = true;
                break;
            }

            if (System.currentTimeMillis() - begin > budget) {
                yielded = true;
                break;
            }

            Models.Draw d = draws.get(i);

            if (!isValidDraw(d)) {
                continue;
            }

            learnedCells +=
                    learnInternal(
                            store,
                            d,
                            TASK_BATCH,
                            "RECENT_" + start + "_" + (draws.size() - 1),
                            i,
                            false,
                            false);

            learnedDraws++;
            lastLearned = d;

            cooperativePause(1L);
        }

        long cost = System.currentTimeMillis() - begin;

        String summary;

        if (yielded) {
            summary =
                    "教师矩阵后台小批量已让路：已学="
                            + learnedDraws
                            + "期｜矩阵格="
                            + learnedCells
                            + "｜耗时="
                            + cost
                            + "ms｜实时分析优先，游标后续继续。";
            writeState(store, "teacher_matrix_status", "IDLE");
        } else {
            summary =
                    "教师矩阵批量学习完成：目标期="
                            + learnedDraws
                            + "｜矩阵格="
                            + learnedCells
                            + "｜耗时="
                            + cost
                            + "ms｜100格反事实+K3画像+位置关联画像已反哺。";
            writeState(store, "teacher_matrix_status", "OK");
        }

        writeState(store, "teacher_matrix_running", "0");
        writeState(store, "teacher_matrix_summary", summary);
        writeState(store, "auto_learn_summary", summary);
        writeState(store, "runtime_profile_summary", "教师矩阵后台小批量已执行，实时预测读取最新画像缓存。");

        if (lastLearned != null && isValidDraw(lastLearned)) {
            writeCompatibleTeacherSnapshot(
                    store,
                    lastLearned,
                    REALTIME_HISTORY_LIMIT,
                    learnedCells,
                    learnedDraws * 10,
                    learnedDraws * 10,
                    learnedDraws * 10,
                    0,
                    cost,
                    summary);
        }

        return learnedCells;
    }

    private int learnInternal(
            DbStore store,
            Models.Draw actualDraw,
            String taskKey,
            String segmentKey,
            int targetIndex,
            boolean updateUiState,
            boolean skipDuplicateRealtime) {

        if (store == null || !isValidDraw(actualDraw)) {
            return 0;
        }

        String issue = safe(actualDraw.issue);
        String normalizedTaskKey =
                taskKey == null || taskKey.length() == 0 ? TASK_REALTIME : taskKey;

        boolean realtimeTask =
                TASK_REALTIME.equals(normalizedTaskKey)
                        || TASK_TICK.equals(normalizedTaskKey)
                        || TASK_AUTO.equals(normalizedTaskKey);

        if (!updateUiState && !realtimeTask && shouldYieldToRealtime(store)) {
            return 0;
        }

        if (skipDuplicateRealtime && shouldSkipDuplicateRealtime(store, issue, normalizedTaskKey)) {
            writeState(store, "teacher_matrix_status", "OK");
            writeState(store, "teacher_matrix_running", "0");
            writeState(
                    store, "teacher_matrix_summary", "教师矩阵已学习本期开奖：" + issue + "，跳过重复计算，避免拖慢实时出号。");
            return 0;
        }

        long startMs = System.currentTimeMillis();

        invokeOptional(store, "ensureLearningTables", new Class[] {}, new Object[] {});
        invokeOptional(store, "ensureTeacherMatrixTables", new Class[] {}, new Object[] {});

        if (updateUiState) {
            writeState(store, "teacher_matrix_status", "RUNNING");
            writeState(store, "teacher_matrix_running", "1");
            writeState(store, "teacher_matrix_issue", issue);
            writeState(
                    store,
                    "teacher_matrix_summary",
                    "教师矩阵学习中：开奖=" + issue + "｜用开奖前240期重建12/24/48/96/120/240状态｜生成100号码格+K3画像+位置关联。");
        }

        List<Models.Draw> history = loadHistoryBefore(store, issue, REALTIME_HISTORY_LIMIT);

        if (history == null || history.size() < MIN_HISTORY_FOR_ANY_LEARN) {
            if (updateUiState) {
                String msg =
                        "教师矩阵：历史不足，当前可见历史="
                                + (history == null ? 0 : history.size())
                                + "｜至少需要 "
                                + MIN_HISTORY_FOR_ANY_LEARN
                                + " 期，暂不做强学习。";

                writeState(store, "teacher_matrix_status", "WAITING");
                writeState(store, "teacher_matrix_running", "0");
                writeState(store, "teacher_matrix_summary", msg);
                writeState(store, "teacher_snapshot_status", "WAITING");
                writeState(store, "teacher_snapshot_summary", "教师快照等待：历史不足，不能生成稳定教师快照。");
            }

            return 0;
        }

        int historySize = history.size();

        ArrayList<Models.LearningCase> positionCases = new ArrayList<Models.LearningCase>();

        int matrixCells = 0;
        int hitCells = 0;
        int comboHitCount = 0;
        int weakSampleCount = 0;
        int strongSampleCount = 0;
        int numberProfileWrites = 0;
        int runtimeProfileWrites = 0;
        int positionLinkWrites = 0;
        int memoryWrites = 0;
        int auditWrites = 0;

        StringBuilder topSummary = new StringBuilder();

        for (int position = POSITION_MIN; position <= POSITION_MAX; position++) {
            PositionState state = buildPositionState(history, actualDraw, position);

            if (state == null) {
                continue;
            }

            int actualNumber = getNumber(actualDraw, position);

            if (!isValidNumber(actualNumber)) {
                continue;
            }

            int[] top3 = chooseTop3Numbers(state);
            int k3Mask = buildK3Mask(top3[0], top3[1], top3[2]);
            boolean comboHit = containsNumber(k3Mask, actualNumber);

            if (comboHit) {
                comboHitCount++;
            }

            if (state.historySize >= MIN_HISTORY_FOR_STRONG_LEARN) {
                strongSampleCount++;
            } else {
                weakSampleCount++;
            }

            for (int number = NUMBER_MIN; number <= NUMBER_MAX; number++) {
                boolean hit = number == actualNumber;
                boolean inK3 = containsNumber(k3Mask, number);
                double support = calcNumberSupportDelta(state, number, hit, inK3);

                numberProfileWrites +=
                        updateStateNumberProfileCompat(
                                store,
                                state.stateKey,
                                position,
                                number,
                                hit,
                                support,
                                state.riskScore,
                                1);

                matrixCells++;

                if (hit) {
                    hitCells++;
                }
            }

            runtimeProfileWrites +=
                    updateRuntimeProfileCompat(
                            store,
                            state.stateKey,
                            position,
                            k3Mask,
                            comboHit,
                            state.finalP,
                            state.dynamicGate,
                            state.riskScore,
                            state.teacherWeight,
                            state.stateConfidence);

            positionLinkWrites +=
                    updatePositionLinkProfileCompat(
                            store,
                            state.positionLinkKey,
                            state.position,
                            state.linkPosition,
                            comboHit,
                            state.positionLinkScore,
                            state.riskScore);

            memoryWrites +=
                    updateShapeMemoryCompat(
                            store,
                            state.shapeKey,
                            position,
                            comboHit,
                            !comboHit,
                            state.falseStrongRisk > 0.58d);

            memoryWrites +=
                    updateComboMemoryCompat(
                            store,
                            state.shapeKey,
                            position,
                            k3Mask,
                            comboHit,
                            state.comboGap,
                            state.trendLife);

            auditWrites += updateTeacherCalibrationAuditCompat(store, state.finalP, comboHit);
            auditWrites += updateProbabilityAuditCompat(store, state.finalP, comboHit);

            Models.LearningCase lc =
                    buildPositionLearningCase(
                            actualDraw,
                            position,
                            actualNumber,
                            k3Mask,
                            comboHit,
                            state,
                            normalizedTaskKey,
                            segmentKey,
                            targetIndex);

            if (lc != null) {
                positionCases.add(lc);
            }

            if (topSummary.length() > 0) {
                topSummary.append("；");
            }

            topSummary.append("P");
            topSummary.append(position);
            topSummary.append("=");
            topSummary.append(two(top3[0]));
            topSummary.append("/");
            topSummary.append(two(top3[1]));
            topSummary.append("/");
            topSummary.append(two(top3[2]));

            if (comboHit) {
                topSummary.append("中");
            } else {
                topSummary.append("漏");
                topSummary.append(two(actualNumber));
            }

            if (state.linkPosition > 0) {
                topSummary.append("@L");
                topSummary.append(state.linkPosition);
            }
        }

        int caseSaved = saveLearningCasesCompat(store, positionCases);
        int matrixSaved = saveTeacherMatrixFromLearningCasesOptional(store, positionCases);

        updateTeacherMatrixSummaryStateOptional(store);

        long costMs = System.currentTimeMillis() - startMs;

        String readyStatus = resolveReadyStatus(historySize, matrixCells, comboHitCount, costMs);

        String summary =
                "教师矩阵完成："
                        + "开奖="
                        + safe(actualDraw.issue)
                        + "｜状态="
                        + readyStatus
                        + "｜历史="
                        + historySize
                        + "｜窗口=12/24/48/96/120/240"
                        + "｜矩阵="
                        + matrixCells
                        + "格"
                        + "｜正样本="
                        + hitCells
                        + "｜负样本="
                        + (matrixCells - hitCells)
                        + "｜位置K3样本="
                        + caseSaved
                        + "｜矩阵入库="
                        + matrixSaved
                        + "｜号码画像写="
                        + numberProfileWrites
                        + "｜运行画像写="
                        + runtimeProfileWrites
                        + "｜位置关联写="
                        + positionLinkWrites
                        + "｜记忆写="
                        + memoryWrites
                        + "｜教师审计写="
                        + auditWrites
                        + "｜K3命中位="
                        + comboHitCount
                        + "/10"
                        + "｜强样本位="
                        + strongSampleCount
                        + "｜弱样本位="
                        + weakSampleCount
                        + "｜耗时="
                        + costMs
                        + "ms"
                        + "｜规则=只画像反哺，不直接救援BET，不污染真实校准";

        if (topSummary.length() > 0) {
            summary += "\n反事实摘要：" + topSummary.toString();
        }

        writeCompatibleTeacherSnapshot(
                store,
                actualDraw,
                historySize,
                matrixCells,
                hitCells,
                comboHitCount,
                caseSaved,
                matrixSaved,
                costMs,
                summary);

        if (updateUiState) {
            writeState(store, "teacher_matrix_status", "OK");
            writeState(store, "teacher_matrix_running", "0");
            writeState(store, "teacher_matrix_summary", summary);
            writeState(store, "learning_summary", summary);
            writeState(store, "auto_learn_summary", summary);
            writeState(
                    store,
                    "runtime_profile_summary",
                    "教师矩阵已反哺：每期100号码格、K3画像、位置关联画像，下一期实时分析读取缓存；不污染真实校准。");
            writeState(
                    store,
                    "service_core_summary",
                    "Teacher Matrix已同步：开奖保存→结算→Teacher Matrix 100格学习→K3画像→位置关联→教师快照兼容→实时决策读取画像。");
        }

        return matrixCells;
    }

    private String resolveReadyStatus(
            int historySize, int matrixCells, int comboHitCount, long costMs) {
        if (historySize < MIN_HISTORY_FOR_ANY_LEARN || matrixCells < 100) {
            return "WARMING";
        }

        if (historySize < MIN_HISTORY_FOR_STRONG_LEARN) {
            return "WEAK";
        }

        if (comboHitCount <= 1 && historySize < 120) {
            return "RISK";
        }

        if (historySize >= 120 && matrixCells >= 100 && comboHitCount >= 3 && costMs <= 3500L) {
            return "STRONG";
        }

        return "READY";
    }

    private boolean shouldSkipDuplicateRealtime(DbStore store, String issue, String taskKey) {
        if (store == null || issue == null || issue.length() == 0) {
            return false;
        }

        if (!TASK_REALTIME.equals(taskKey)
                && !TASK_TICK.equals(taskKey)
                && !TASK_AUTO.equals(taskKey)) {
            return false;
        }

        try {
            String lastIssue = store.getState("teacher_matrix_last_issue", "");
            String status = store.getState("teacher_matrix_status", "");
            long updatedAt = parseLong(store.getState("teacher_matrix_updated_at", "0"), 0L);
            long now = System.currentTimeMillis();

            if (issue.equals(lastIssue)
                    && ("OK".equals(status) || "DONE".equals(status))
                    && updatedAt > 0L
                    && now - updatedAt < REALTIME_DUPLICATE_SKIP_MS) {
                return true;
            }

            if (issue.equals(lastIssue)
                    && ("OK".equals(status) || "DONE".equals(status))
                    && updatedAt > 0L
                    && now - updatedAt < SNAPSHOT_VALID_MS) {
                String cells = store.getState("teacher_matrix_last_cells", "0");
                int c = parseInt(cells, 0);

                if (c >= 100) {
                    return true;
                }
            }
        } catch (Throwable e) {
        }

        return false;
    }

    private boolean shouldYieldToRealtime(DbStore store) {
        if (store == null) {
            return false;
        }

        try {
            String analysisStatus = store.getState("analysis_status", "");
            if ("RUNNING".equals(analysisStatus)
                    || "START".equals(analysisStatus)
                    || "ANALYZING".equals(analysisStatus)
                    || "BUSY".equals(analysisStatus)) {
                return true;
            }

            String realtimePriorityUntil = store.getState("realtime_priority_until_at", "0");
            long until = parseLong(realtimePriorityUntil, 0L);
            long now = System.currentTimeMillis();

            if (until > now) {
                return true;
            }

            long remainMs = parseLong(store.getState("countdown_remain_ms", "999999"), 999999L);
            if (remainMs > 0L && remainMs <= REALTIME_PRIORITY_COUNTDOWN_MS) {
                return true;
            }

            String target = store.getState("analysis_target_issue", "");
            String done = store.getState("analysis_done_target_issue", "");

            if (target != null && target.length() > 0 && !target.equals(done)) {
                return true;
            }
        } catch (Throwable e) {
        }

        return false;
    }

    /* =========================
     * 兼容旧教师快照 / 运行状态
     * ========================= */

    private void writeCompatibleTeacherSnapshot(
            DbStore store,
            Models.Draw actualDraw,
            int historySize,
            int matrixCells,
            int hitCells,
            int comboHitCount,
            int caseSaved,
            int matrixSaved,
            long costMs,
            String summary) {

        if (store == null || actualDraw == null) {
            return;
        }

        long now = System.currentTimeMillis();
        String issue = safe(actualDraw.issue);
        String snapshotId = "TM_" + issue + "_" + now;
        double confidence =
                buildSnapshotConfidence(historySize, matrixCells, comboHitCount, costMs);
        String readyStatus = resolveReadyStatus(historySize, matrixCells, comboHitCount, costMs);
        String shortSummary = limit(summary, SUMMARY_LIMIT);

        int totalSamples = optionalInt(store, "countTeacherMatrixSamples", 0);
        int totalProfiles = optionalInt(store, "countTeacherMatrixProfiles", 0);
        int strongProfiles = optionalInt(store, "countTeacherMatrixStrongProfiles", 0);
        int riskProfiles = optionalInt(store, "countTeacherMatrixRiskProfiles", 0);

        if (totalSamples <= 0) {
            totalSamples = matrixCells;
        }

        if (totalProfiles <= 0) {
            totalProfiles = caseSaved;
        }

        writeState(store, "teacher_matrix_status", "OK");
        writeState(store, "teacher_matrix_running", "0");
        writeState(store, "teacher_matrix_summary", shortSummary);
        writeState(store, "teacher_matrix_issue", issue);
        writeState(store, "teacher_matrix_last_issue", issue);
        writeState(store, "teacher_matrix_updated_at", String.valueOf(now));
        writeState(store, "teacher_matrix_sample_count", String.valueOf(totalSamples));
        writeState(store, "teacher_matrix_profile_count", String.valueOf(totalProfiles));
        writeState(store, "teacher_matrix_strong_profile_count", String.valueOf(strongProfiles));
        writeState(store, "teacher_matrix_risk_profile_count", String.valueOf(riskProfiles));
        writeState(store, "teacher_matrix_last_cells", String.valueOf(matrixCells));
        writeState(store, "teacher_matrix_last_positive", String.valueOf(hitCells));
        writeState(store, "teacher_matrix_last_negative", String.valueOf(matrixCells - hitCells));
        writeState(store, "teacher_matrix_last_combo_hit", String.valueOf(comboHitCount));
        writeState(store, "teacher_matrix_last_case_saved", String.valueOf(caseSaved));
        writeState(store, "teacher_matrix_last_matrix_saved", String.valueOf(matrixSaved));
        writeState(store, "teacher_matrix_last_cost_ms", String.valueOf(costMs));
        writeState(store, "teacher_matrix_last_confidence", String.valueOf(confidence));
        writeState(store, "teacher_matrix_last_confidence_text", percent(confidence));
        writeState(store, "teacher_matrix_ready_status", readyStatus);
        writeState(store, "teacher_matrix_ready", matrixCells >= 100 ? "1" : "0");

        writeState(store, "teacher_snapshot_status", "OK");
        writeState(store, "teacher_snapshot_running", "0");
        writeState(store, "teacher_snapshot_id", snapshotId);
        writeState(store, "teacher_snapshot_issue", issue);
        writeState(
                store,
                "teacher_snapshot_summary",
                "教师快照已生成：来源=TeacherMatrix｜状态="
                        + readyStatus
                        + "｜开奖="
                        + issue
                        + "｜矩阵="
                        + matrixCells
                        + "格｜置信="
                        + percent(confidence)
                        + "｜"
                        + shortSummary);
        writeState(store, "teacher_snapshot_updated_at", String.valueOf(now));
        writeState(store, "teacher_snapshot_created_at", String.valueOf(now));
        writeState(store, "teacher_snapshot_valid", "1");
        writeState(store, "teacher_snapshot_valid_until", String.valueOf(now + SNAPSHOT_VALID_MS));

        writeState(store, "teacher_confidence", String.valueOf(confidence));
        writeState(store, "teacher_snapshot_confidence", String.valueOf(confidence));
        writeState(store, "teacher_weight", String.valueOf(confidence));

        writeState(store, "last_snapshot_id", snapshotId);
        writeState(store, "lastSnapshotId", snapshotId);
        writeState(store, "runtime_learning_snapshot_id", snapshotId);
        writeState(store, "runtime_snapshot_id", snapshotId);
        writeState(store, "deep_snapshot_id", snapshotId);

        writeState(store, "learning_status", "OK");
        writeState(store, "learning_summary", shortSummary);
        writeState(store, "auto_learn_status", "IDLE");
        writeState(store, "auto_learn_summary", shortSummary);
        writeState(
                store,
                "runtime_profile_summary",
                "教师矩阵快照有效：snapshot="
                        + snapshotId
                        + "｜状态="
                        + readyStatus
                        + "｜样本="
                        + totalSamples
                        + "｜画像="
                        + totalProfiles
                        + "｜规则=只画像反哺不直接救援BET");
        writeState(
                store,
                "service_core_summary",
                "核心闭环正常：开奖保存→结算→Teacher Matrix 100格学习→K3画像→位置关联→教师快照兼容→实时决策读取画像。");

        saveRuntimeLearningSnapshotOptional(
                store,
                actualDraw,
                snapshotId,
                confidence,
                matrixCells,
                hitCells,
                comboHitCount,
                caseSaved,
                matrixSaved,
                costMs,
                shortSummary);
    }

    private double buildSnapshotConfidence(
            int historySize, int matrixCells, int comboHitCount, long costMs) {
        double sample = historySize >= 240 ? 1.0d : ((double) historySize) / 240.0d;
        double cells = matrixCells >= 100 ? 1.0d : ((double) matrixCells) / 100.0d;
        double combo = ((double) comboHitCount) / 10.0d;
        double speed;

        if (costMs <= 900L) {
            speed = 1.0d;
        } else if (costMs >= 5000L) {
            speed = 0.40d;
        } else {
            speed = 1.0d - (((double) costMs - 900.0d) / 7000.0d);
        }

        return clamp01((sample * 0.42d) + (cells * 0.28d) + (combo * 0.16d) + (speed * 0.14d));
    }

    private void saveRuntimeLearningSnapshotOptional(
            DbStore store,
            Models.Draw actualDraw,
            String snapshotId,
            double confidence,
            int matrixCells,
            int hitCells,
            int comboHitCount,
            int caseSaved,
            int matrixSaved,
            long costMs,
            String summary) {

        if (store == null) {
            return;
        }

        try {
            Class cls = Class.forName("com.ethk3.novaonepro.Models$RuntimeLearningSnapshot");
            Object snap = cls.newInstance();

            setField(snap, "snapshotId", snapshotId);
            setField(snap, "id", snapshotId);
            setField(snap, "baseIssue", safe(actualDraw.issue));
            setField(snap, "targetIssue", safe(actualDraw.issue));
            setField(snap, "createdAt", Long.valueOf(System.currentTimeMillis()));
            setField(snap, "updatedAt", Long.valueOf(System.currentTimeMillis()));
            setField(snap, "summary", summary);
            setField(snap, "detail", summary);
            setField(snap, "learningSummary", summary);
            setField(snap, "teacherMatrixSummary", summary);
            setField(snap, "teacherSnapshotSummary", summary);
            setField(snap, "status", "OK");
            setField(snap, "source", "TeacherMatrixEngine");
            setField(snap, "sampleCount", Integer.valueOf(matrixCells));
            setField(snap, "matrixCells", Integer.valueOf(matrixCells));
            setField(snap, "positiveCount", Integer.valueOf(hitCells));
            setField(snap, "negativeCount", Integer.valueOf(matrixCells - hitCells));
            setField(snap, "comboHitCount", Integer.valueOf(comboHitCount));
            setField(snap, "caseSaved", Integer.valueOf(caseSaved));
            setField(snap, "matrixSaved", Integer.valueOf(matrixSaved));
            setField(snap, "confidence", Double.valueOf(confidence));
            setField(snap, "teacherConfidence", Double.valueOf(confidence));
            setField(snap, "teacherWeight", Double.valueOf(confidence));
            setField(snap, "costMs", Long.valueOf(costMs));

            invokeOptional(
                    store, "saveRuntimeLearningSnapshot", new Class[] {cls}, new Object[] {snap});
            invokeOptional(store, "saveTeacherSnapshot", new Class[] {cls}, new Object[] {snap});
            invokeOptional(store, "saveLearningSnapshot", new Class[] {cls}, new Object[] {snap});
            invokeOptional(
                    store, "updateRuntimeLearningSnapshot", new Class[] {cls}, new Object[] {snap});
        } catch (Throwable e) {
        }
    }

    private void setField(Object obj, String fieldName, Object value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            f.set(obj, value);
        } catch (Throwable e) {
        }
    }

    /* =========================
     * LearningCase 构建
     * ========================= */

    private Models.LearningCase buildPositionLearningCase(
            Models.Draw actualDraw,
            int position,
            int actualNumber,
            int k3Mask,
            boolean comboHit,
            PositionState state,
            String taskKey,
            String segmentKey,
            int targetIndex) {

        if (actualDraw == null || state == null) {
            return null;
        }

        Models.LearningCase x = new Models.LearningCase();

        x.caseKey = buildCaseKey(taskKey, segmentKey, actualDraw.issue, position);
        x.taskKey = safe(taskKey);
        x.segmentKey = safe(segmentKey);
        x.baseIssue = state.baseIssue;
        x.targetIssue = actualDraw.issue == null ? "" : actualDraw.issue;
        x.createdAt = System.currentTimeMillis();
        x.targetIndex = targetIndex;

        x.position = position;
        x.actualNumber = actualNumber;
        x.predictedK3 = k3Mask;
        x.k3Mask = k3Mask;
        x.hit = comboHit ? 1 : 0;

        x.stateKey = state.stateKey;
        x.stateMode = state.stateMode;
        x.trendKey = state.trendKey;
        x.shapeKey = state.shapeKey;
        x.driftKey = state.driftKey;
        x.breakKey = state.breakKey;
        x.recoveryKey = state.recoveryKey;
        x.transitionKey = state.transitionKey;

        x.expectedShape = state.expectedShape;
        x.actualShape = "P" + position + "_ACTUAL_" + two(actualNumber);

        x.whyExpectedHit = buildWhyExpectedHit(state, k3Mask);
        x.whyActualHit = buildWhyActualHit(state, actualNumber, comboHit);
        x.whyMiss = comboHit ? "" : buildWhyMiss(state, actualNumber, k3Mask);
        x.nextAdvice = buildNextAdvice(state, comboHit);

        x.mainReasonCode = comboHit ? "TEACHER_MATRIX_HIT" : "TEACHER_MATRIX_MISS";
        x.affectedModule = "TeacherMatrixEngine";

        x.finalP = state.finalP;
        x.dynamicGate = state.dynamicGate;
        x.edge = state.finalP - state.dynamicGate;

        x.trendScore = state.trendScore;
        x.shapeScore = state.shapeScore;
        x.driftRisk = state.driftRisk;
        x.breakRisk = state.breakRisk;
        x.recoveryScore = state.recoveryScore;
        x.profileScore = state.profileScore;
        x.numberBias = state.numberBias;
        x.comboBias = state.comboBias;
        x.riskScore = state.riskScore;
        x.teacherWeight = state.teacherWeight;
        x.stateConfidence = state.stateConfidence;
        x.stateSample = state.historySize;
        x.cacheSample = state.historySize;
        x.recentSample = Math.min(state.historySize, 60);
        x.recentHitRate = comboHit ? 1.0d : 0.0d;

        setField(x, "positionLinkKey", state.positionLinkKey);
        setField(x, "positionLinkSummary", state.positionLinkSummary);
        setField(x, "linkedPosition", Integer.valueOf(state.linkPosition));
        setField(x, "positionLinkScore", Double.valueOf(state.positionLinkScore));
        setField(x, "numberShapeKey", state.numberShapeKey);
        setField(x, "comboShapeKey", state.comboShapeKey);
        setField(
                x,
                "teacherMatrixReadyStatus",
                resolveReadyStatus(state.historySize, 100, comboHit ? 1 : 0, 0L));
        setField(x, "whyHit", x.whyActualHit);
        setField(x, "why_hit", x.whyActualHit);
        setField(x, "why_miss", x.whyMiss);
        setField(x, "next_advice", x.nextAdvice);

        return x;
    }

    /* =========================
     * 历史读取
     * ========================= */

    private List<Models.Draw> loadHistoryBefore(DbStore store, String issue, int limit) {
        ArrayList<Models.Draw> out = new ArrayList<Models.Draw>();

        if (store == null) {
            return out;
        }

        if (limit <= 0) {
            limit = REALTIME_HISTORY_LIMIT;
        }

        Object result =
                invokeOptional(
                        store,
                        "getDrawsBeforeIssue",
                        new Class[] {String.class, Integer.TYPE},
                        new Object[] {issue, Integer.valueOf(limit)});

        if (result instanceof List) {
            try {
                List list = (List) result;

                for (int i = 0; i < list.size(); i++) {
                    Object obj = list.get(i);

                    if (obj instanceof Models.Draw) {
                        Models.Draw d = (Models.Draw) obj;

                        if (!isValidDraw(d)) {
                            continue;
                        }

                        if (issue != null && issue.length() > 0 && safe(d.issue).equals(issue)) {
                            continue;
                        }

                        if (issue != null
                                && issue.length() > 0
                                && compareIssue(d.issue, issue) >= 0) {
                            continue;
                        }

                        out.add(d);
                    }
                }
            } catch (Throwable e) {
                out.clear();
            }
        }

        if (out.size() < MIN_HISTORY_FOR_ANY_LEARN) {
            try {
                List<Models.Draw> recent = store.getRecentDraws(limit + 20);

                out.clear();

                if (recent != null) {
                    for (int i = 0; i < recent.size(); i++) {
                        Models.Draw d = recent.get(i);

                        if (!isValidDraw(d)) {
                            continue;
                        }

                        if (issue != null && issue.length() > 0 && safe(d.issue).equals(issue)) {
                            continue;
                        }

                        if (issue != null
                                && issue.length() > 0
                                && compareIssue(d.issue, issue) >= 0) {
                            continue;
                        }

                        out.add(d);
                    }
                }
            } catch (Throwable e) {
            }
        }

        out = normalizeDraws(out);

        if (out.size() > limit) {
            ArrayList<Models.Draw> trimmed = new ArrayList<Models.Draw>();
            int start = out.size() - limit;

            for (int i = start; i < out.size(); i++) {
                trimmed.add(out.get(i));
            }

            return trimmed;
        }

        return out;
    }

    private ArrayList<Models.Draw> normalizeDraws(List<Models.Draw> source) {
        ArrayList<Models.Draw> out = new ArrayList<Models.Draw>();

        if (source == null || source.size() == 0) {
            return out;
        }

        for (int i = 0; i < source.size(); i++) {
            Models.Draw d = source.get(i);

            if (!isValidDraw(d)) {
                continue;
            }

            boolean exists = false;

            for (int j = 0; j < out.size(); j++) {
                Models.Draw old = out.get(j);

                if (old != null && safe(old.issue).equals(safe(d.issue))) {
                    exists = true;

                    if (d.fetchTime > old.fetchTime) {
                        out.set(j, d);
                    }

                    break;
                }
            }

            if (!exists) {
                out.add(d);
            }
        }

        Collections.sort(
                out,
                new Comparator<Models.Draw>() {
                    @Override
                    public int compare(Models.Draw a, Models.Draw b) {
                        return compareIssue(a == null ? "" : a.issue, b == null ? "" : b.issue);
                    }
                });

        return out;
    }

    /* =========================
     * 位置状态建模
     * ========================= */

    private PositionState buildPositionState(
            List<Models.Draw> history, Models.Draw actualDraw, int position) {
        if (history == null || history.size() == 0 || !isValidPosition(position)) {
            return null;
        }

        PositionState s = new PositionState();

        s.position = position;
        s.historySize = history.size();

        Models.Draw baseDraw = history.get(history.size() - 1);
        s.baseIssue = baseDraw == null || baseDraw.issue == null ? "" : baseDraw.issue;
        s.targetIssue = actualDraw == null || actualDraw.issue == null ? "" : actualDraw.issue;

        int[] count12 = countWindow(history, position, 12);
        int[] count24 = countWindow(history, position, 24);
        int[] count48 = countWindow(history, position, 48);
        int[] count96 = countWindow(history, position, 96);
        int[] count120 = countWindow(history, position, 120);
        int[] count240 = countWindow(history, position, 240);

        double[] p12 = toProbability(count12);
        double[] p24 = toProbability(count24);
        double[] p48 = toProbability(count48);
        double[] p96 = toProbability(count96);
        double[] p120 = toProbability(count120);
        double[] p240 = toProbability(count240);

        s.entropy12 = entropy01(p12);
        s.entropy24 = entropy01(p24);
        s.entropy48 = entropy01(p48);
        s.entropy96 = entropy01(p96);
        s.entropy120 = entropy01(p120);
        s.entropy240 = entropy01(p240);

        s.top3Mass12 = topMass(p12, 3);
        s.top3Mass24 = topMass(p24, 3);
        s.top3Mass48 = topMass(p48, 3);
        s.top3Mass96 = topMass(p96, 3);
        s.top3Mass120 = topMass(p120, 3);
        s.top3Mass240 = topMass(p240, 3);

        s.driftShort = avgAbsDiff(p12, p48);
        s.driftMid = avgAbsDiff(p24, p96);
        s.driftLong = avgAbsDiff(p96, p240);
        s.driftRisk =
                clamp01((s.driftShort * 2.40d) + (s.driftMid * 1.20d) + (s.driftLong * 0.70d));

        s.lastNumber = getNumber(history.get(history.size() - 1), position);
        s.prevNumber =
                history.size() >= 2 ? getNumber(history.get(history.size() - 2), position) : 0;
        s.actualNumber = actualDraw == null ? 0 : getNumber(actualDraw, position);

        buildMissCounts(history, position, s.miss);
        buildTransitionProb(history, position, s.lastNumber, s.transitionProb);
        buildNumberScores(s, p12, p24, p48, p96, p120, p240);
        buildPositionLinkState(history, s);

        s.recoveryScore = calcRecoveryScore(s);
        s.breakRisk = calcBreakRisk(s);
        s.hotReverseRisk = calcHotReverseRisk(s);
        s.falseStrongRisk = calcFalseStrongRisk(s);
        s.trendLife = calcTrendLife(s);

        s.riskScore =
                clamp01(
                        (s.driftRisk * 0.34d)
                                + (s.breakRisk * 0.22d)
                                + (s.hotReverseRisk * 0.18d)
                                + (s.falseStrongRisk * 0.18d)
                                + ((1.0d - s.entropy48) * 0.08d));

        s.stateConfidence = calcStateConfidence(s.historySize, s.driftRisk, s.entropy96);
        s.teacherWeight = calcTeacherWeight(s.historySize, s.riskScore);

        int[] top3 = chooseTop3Numbers(s);

        s.k3Mask = buildK3Mask(top3[0], top3[1], top3[2]);
        s.comboGap = calcComboGap(s, top3);
        s.numberBias = calcTopNumberBias(s, top3);
        s.comboBias = calcComboBias(s, top3);

        s.rawK3P = calcRawK3Probability(s, top3);
        s.finalP = calcFinalK3Probability(s, top3);
        s.dynamicGate = calcDynamicGate(s);
        s.trendScore = calcTrendScore(s, top3);
        s.shapeScore = calcShapeScore(s);
        s.profileScore =
                clamp01(
                        (s.finalP * 0.46d)
                                + (s.comboBias * 0.22d)
                                + (s.stateConfidence * 0.16d)
                                - (s.riskScore * 0.20d));

        s.stateMode = resolveStateMode(s);
        s.trendKey = buildTrendKey(s);
        s.driftKey = bucketText("DRIFT", s.driftRisk);
        s.breakKey = bucketText("BREAK", s.breakRisk);
        s.recoveryKey = bucketText("RECOVER", s.recoveryScore);
        s.transitionKey = buildTransitionKey(s);
        s.numberShapeKey = buildNumberShapeKey(s, top3);
        s.comboShapeKey = buildComboShapeKey(s, top3);
        s.shapeKey = buildShapeKey(s);
        s.stateKey = buildStateKey(s);
        s.expectedShape = buildExpectedShape(s, top3);

        return s;
    }

    private void buildNumberScores(
            PositionState s,
            double[] p12,
            double[] p24,
            double[] p48,
            double[] p96,
            double[] p120,
            double[] p240) {

        double[] raw = new double[NUMBER_MAX + 1];
        double sum = 0.0d;

        for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
            double missRecovery = missRecoveryScore(s.miss[n]);
            double transition = s.transitionProb[n];
            double repeatSupport = n == s.lastNumber ? 0.030d : 0.000d;
            double prevBounce = n == s.prevNumber ? 0.012d : 0.000d;

            double stableFreq = (p96[n] * 0.26d) + (p120[n] * 0.18d) + (p240[n] * 0.08d);
            double recentFreq = (p12[n] * 0.18d) + (p24[n] * 0.15d) + (p48[n] * 0.10d);

            double driftBoost = 0.0d;

            if (p12[n] > p48[n]) {
                driftBoost += (p12[n] - p48[n]) * 0.24d;
            }

            if (p24[n] > p96[n]) {
                driftBoost += (p24[n] - p96[n]) * 0.10d;
            }

            double coldPenalty = 0.0d;

            if (s.miss[n] >= 18 && p12[n] <= 0.0001d) {
                coldPenalty += 0.020d;
            }

            double hotReversePenalty = 0.0d;

            if (p12[n] >= 0.220d && p48[n] < 0.120d) {
                hotReversePenalty += 0.018d;
            }

            double linkSupport = 0.0d;

            if (s.linkLastNumber == n && s.positionLinkScore > 0.58d) {
                linkSupport += 0.010d;
            }

            double score =
                    0.035d
                            + stableFreq
                            + recentFreq
                            + (transition * 0.26d)
                            + (missRecovery * 0.080d)
                            + repeatSupport
                            + prevBounce
                            + driftBoost
                            + linkSupport
                            - coldPenalty
                            - hotReversePenalty;

            if (score < 0.004d) {
                score = 0.004d;
            }

            raw[n] = score;
            sum += score;
        }

        if (sum <= 0.0d) {
            for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
                s.numberP[n] = BASE_NUMBER_P;
                s.numberScore[n] = BASE_NUMBER_P;
            }

            return;
        }

        for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
            double p = raw[n] / sum;

            if (p < 0.025d) {
                p = 0.025d;
            }

            if (p > 0.230d) {
                p = 0.230d;
            }

            s.numberP[n] = p;
            s.numberScore[n] = raw[n];
        }

        renormalizeNumberP(s.numberP);
    }

    private void buildPositionLinkState(List<Models.Draw> history, PositionState s) {
        if (history == null || s == null || !isValidPosition(s.position)) {
            return;
        }

        int window = Math.min(96, history.size());

        if (window < 12) {
            return;
        }

        int start = history.size() - window;

        if (start < 0) {
            start = 0;
        }

        int bestPos = 0;
        double bestScore = 0.0d;
        int bestLast = 0;

        for (int p = POSITION_MIN; p <= POSITION_MAX; p++) {
            if (p == s.position) {
                continue;
            }

            int sameZone = 0;
            int closeGap = 0;
            int sameOddEven = 0;
            int total = 0;

            for (int i = start; i < history.size(); i++) {
                Models.Draw d = history.get(i);
                int a = getNumber(d, s.position);
                int b = getNumber(d, p);

                if (!isValidNumber(a) || !isValidNumber(b)) {
                    continue;
                }

                if (zoneOf(a) == zoneOf(b)) {
                    sameZone++;
                }

                if (Math.abs(a - b) <= 2) {
                    closeGap++;
                }

                if ((a % 2) == (b % 2)) {
                    sameOddEven++;
                }

                total++;
            }

            if (total <= 0) {
                continue;
            }

            double score =
                    (((double) sameZone) / ((double) total)) * 0.42d
                            + (((double) closeGap) / ((double) total)) * 0.34d
                            + (((double) sameOddEven) / ((double) total)) * 0.24d;

            if (score > bestScore) {
                bestScore = score;
                bestPos = p;
                bestLast = getNumber(history.get(history.size() - 1), p);
            }
        }

        s.linkPosition = bestPos;
        s.positionLinkScore = clamp01(bestScore);
        s.linkLastNumber = bestLast;

        if (bestPos > 0) {
            s.positionLinkKey =
                    cleanKey(
                            "P"
                                    + s.position
                                    + "_LINK_P"
                                    + bestPos
                                    + "_S_"
                                    + bucketText("", bestScore)
                                    + "_LN_"
                                    + two(bestLast));
            s.positionLinkSummary =
                    "P"
                            + s.position
                            + " 与 P"
                            + bestPos
                            + " 近"
                            + window
                            + "期关联分="
                            + percent(bestScore)
                            + "，联动末号="
                            + two(bestLast);
        } else {
            s.positionLinkKey = "P" + s.position + "_LINK_NONE";
            s.positionLinkSummary = "P" + s.position + " 暂无稳定位置关联";
        }
    }

    private void renormalizeNumberP(double[] p) {
        if (p == null || p.length <= NUMBER_MAX) {
            return;
        }

        double sum = 0.0d;

        for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
            if (p[n] < 0.0d) {
                p[n] = 0.0d;
            }

            sum += p[n];
        }

        if (sum <= 0.0d) {
            for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
                p[n] = BASE_NUMBER_P;
            }

            return;
        }

        for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
            p[n] = p[n] / sum;
        }
    }

    private int[] chooseTop3Numbers(PositionState s) {
        int[] top = new int[] {1, 2, 3};
        double[] score = new double[NUMBER_MAX + 1];

        if (s == null) {
            return top;
        }

        for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
            double v = s.numberP[n];

            v += s.transitionProb[n] * 0.030d;
            v += missRecoveryScore(s.miss[n]) * 0.010d;

            if (n == s.lastNumber && s.driftRisk > 0.48d) {
                v -= 0.006d;
            }

            if (s.linkLastNumber == n && s.positionLinkScore >= 0.58d) {
                v += 0.006d;
            }

            score[n] = v;
        }

        for (int slot = 0; slot < 3; slot++) {
            int best = 0;
            double bestScore = -999.0d;

            for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
                if (contains(top, slot, n)) {
                    continue;
                }

                if (score[n] > bestScore) {
                    bestScore = score[n];
                    best = n;
                }
            }

            if (best <= 0) {
                best = slot + 1;
            }

            top[slot] = best;
        }

        sort3(top);

        return top;
    }

    private double calcNumberSupportDelta(PositionState s, int number, boolean hit, boolean inK3) {
        if (s == null || !isValidNumber(number)) {
            return 0.0d;
        }

        double p = s.numberP[number];
        double transition = s.transitionProb[number];
        double recovery = missRecoveryScore(s.miss[number]);
        double support = 0.0d;

        if (hit) {
            support += 0.160d;
            support += p * 0.220d;
            support += transition * 0.120d;
            support += recovery * 0.050d;

            if (inK3) {
                support += 0.080d;
            }

            if (s.linkLastNumber == number && s.positionLinkScore > 0.58d) {
                support += 0.020d;
            }
        } else {
            support -= 0.030d;

            if (inK3) {
                support -= 0.055d;
            }

            if (p > 0.145d) {
                support -= 0.025d;
            }

            if (transition > 0.160d) {
                support -= 0.015d;
            }
        }

        support -= s.riskScore * 0.035d;

        return clamp(support, -0.350d, 0.350d);
    }

    private double calcRawK3Probability(PositionState s, int[] top3) {
        if (s == null || top3 == null || top3.length < 3) {
            return BASE_K3_P;
        }

        double p = 0.0d;

        for (int i = 0; i < 3; i++) {
            int n = top3[i];

            if (isValidNumber(n)) {
                p += s.numberP[n];
            }
        }

        return clamp(p, 0.220d, 0.520d);
    }

    private double calcFinalK3Probability(PositionState s, int[] top3) {
        if (s == null) {
            return BASE_K3_P;
        }

        double raw = calcRawK3Probability(s, top3);
        double adjusted = raw;

        adjusted += s.stateConfidence * 0.018d;
        adjusted += s.comboBias * 0.030d;
        adjusted += s.recoveryScore * 0.012d;
        adjusted += s.positionLinkScore * 0.006d;
        adjusted -= s.riskScore * 0.035d;
        adjusted -= s.falseStrongRisk * 0.018d;
        adjusted -= s.hotReverseRisk * 0.012d;

        if (s.historySize < MIN_HISTORY_FOR_STRONG_LEARN) {
            adjusted = (adjusted * 0.68d) + (BASE_K3_P * 0.32d);
        }

        return clamp(adjusted, 0.250d, 0.480d);
    }

    private double calcDynamicGate(PositionState s) {
        if (s == null) {
            return GATE_BASE;
        }

        double gate = GATE_BASE;

        if (s.historySize < MIN_HISTORY_FOR_STRONG_LEARN) {
            gate += 0.018d;
        }

        gate += s.riskScore * 0.038d;
        gate += s.falseStrongRisk * 0.016d;
        gate += s.hotReverseRisk * 0.010d;

        if (s.comboGap < 0.018d) {
            gate += 0.010d;
        }

        if (s.stateConfidence > 0.72d && s.riskScore < 0.30d) {
            gate -= 0.006d;
        }

        if (s.positionLinkScore > 0.68d && s.riskScore < 0.42d) {
            gate -= 0.002d;
        }

        return clamp(gate, GATE_MIN, GATE_MAX);
    }

    private double calcStateConfidence(int historySize, double driftRisk, double entropy) {
        double sample = historySize >= 120 ? 1.0d : ((double) historySize) / 120.0d;
        double stability = 1.0d - clamp01(driftRisk);
        double entropyBalance = 1.0d - Math.abs(entropy - 0.82d);

        return clamp01((sample * 0.55d) + (stability * 0.30d) + (entropyBalance * 0.15d));
    }

    private double calcTeacherWeight(int historySize, double risk) {
        double w;

        if (historySize >= 180) {
            w = 1.00d;
        } else if (historySize >= 120) {
            w = 0.82d;
        } else if (historySize >= 60) {
            w = 0.62d;
        } else {
            w = 0.38d;
        }

        w -= clamp01(risk) * 0.18d;

        return clamp(w, 0.20d, 1.00d);
    }

    private double calcRecoveryScore(PositionState s) {
        if (s == null) {
            return 0.0d;
        }

        double total = 0.0d;
        int count = 0;

        for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
            total += missRecoveryScore(s.miss[n]);
            count++;
        }

        if (count <= 0) {
            return 0.0d;
        }

        return clamp01(total / count);
    }

    private double calcBreakRisk(PositionState s) {
        if (s == null) {
            return 0.0d;
        }

        double risk = 0.0d;

        if (s.entropy12 > 0.92d && s.top3Mass12 < 0.350d) {
            risk += 0.28d;
        }

        if (s.driftRisk > 0.50d) {
            risk += 0.25d;
        }

        if (s.top3Mass12 < s.top3Mass48 - 0.090d) {
            risk += 0.18d;
        }

        if (s.lastNumber > 0 && s.miss[s.lastNumber] == 0 && s.transitionMax() < 0.115d) {
            risk += 0.10d;
        }

        return clamp01(risk);
    }

    private double calcHotReverseRisk(PositionState s) {
        if (s == null) {
            return 0.0d;
        }

        double risk = 0.0d;

        for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
            if (s.numberP[n] > 0.155d && s.miss[n] == 0) {
                risk += 0.045d;
            }

            if (s.numberP[n] > 0.175d && s.transitionProb[n] < 0.040d) {
                risk += 0.035d;
            }
        }

        return clamp01(risk);
    }

    private double calcFalseStrongRisk(PositionState s) {
        if (s == null) {
            return 0.0d;
        }

        int[] top3 = chooseTop3Numbers(s);
        double risk = 0.0d;

        for (int i = 0; i < 3; i++) {
            int n = top3[i];

            if (!isValidNumber(n)) {
                continue;
            }

            if (s.numberP[n] > 0.155d && s.miss[n] >= 12) {
                risk += 0.065d;
            }

            if (s.transitionProb[n] <= 0.001d && s.lastNumber > 0) {
                risk += 0.040d;
            }
        }

        if (s.comboGap < 0.012d) {
            risk += 0.090d;
        }

        if (s.driftRisk > 0.55d) {
            risk += 0.100d;
        }

        return clamp01(risk);
    }

    private double calcTrendLife(PositionState s) {
        if (s == null) {
            return 0.0d;
        }

        double life = 0.0d;

        if (s.top3Mass12 >= s.top3Mass24) {
            life += 0.18d;
        }

        if (s.top3Mass24 >= s.top3Mass48 - 0.020d) {
            life += 0.18d;
        }

        if (s.top3Mass48 >= s.top3Mass96 - 0.020d) {
            life += 0.16d;
        }

        if (s.driftRisk < 0.35d) {
            life += 0.22d;
        }

        if (s.entropy96 >= 0.65d && s.entropy96 <= 0.92d) {
            life += 0.14d;
        }

        if (s.transitionMax() >= 0.120d) {
            life += 0.12d;
        }

        return clamp01(life);
    }

    private double calcComboGap(PositionState s, int[] top3) {
        if (s == null || top3 == null || top3.length < 3) {
            return 0.0d;
        }

        double third = 999.0d;
        double fourth = -999.0d;

        for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
            boolean in = false;

            for (int i = 0; i < 3; i++) {
                if (top3[i] == n) {
                    in = true;
                    break;
                }
            }

            if (in) {
                if (s.numberP[n] < third) {
                    third = s.numberP[n];
                }
            } else {
                if (s.numberP[n] > fourth) {
                    fourth = s.numberP[n];
                }
            }
        }

        if (third == 999.0d || fourth == -999.0d) {
            return 0.0d;
        }

        return third - fourth;
    }

    private double calcTopNumberBias(PositionState s, int[] top3) {
        if (s == null || top3 == null) {
            return 0.0d;
        }

        double max = 0.0d;

        for (int i = 0; i < top3.length; i++) {
            int n = top3[i];

            if (isValidNumber(n) && s.numberP[n] > max) {
                max = s.numberP[n];
            }
        }

        return clamp01((max - BASE_NUMBER_P) * 3.0d);
    }

    private double calcComboBias(PositionState s, int[] top3) {
        if (s == null || top3 == null) {
            return 0.0d;
        }

        double p = calcRawK3Probability(s, top3);
        return clamp01((p - BASE_K3_P) * 3.2d);
    }

    private double calcTrendScore(PositionState s, int[] top3) {
        if (s == null || top3 == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += s.trendLife * 0.36d;
        score += (1.0d - s.driftRisk) * 0.20d;
        score += s.comboBias * 0.22d;
        score += Math.max(0.0d, s.comboGap) * 3.0d;
        score += s.positionLinkScore * 0.04d;

        return clamp01(score);
    }

    private double calcShapeScore(PositionState s) {
        if (s == null) {
            return 0.0d;
        }

        double score = 0.0d;

        score += (1.0d - Math.abs(s.entropy96 - 0.78d)) * 0.28d;
        score += s.top3Mass96 * 0.32d;
        score += (1.0d - s.driftRisk) * 0.24d;
        score += s.recoveryScore * 0.16d;

        return clamp01(score);
    }

    private String resolveStateMode(PositionState s) {
        if (s == null) {
            return "UNKNOWN";
        }

        if (s.historySize < MIN_HISTORY_FOR_STRONG_LEARN) {
            return "WARMING";
        }

        if (s.entropy96 > 0.920d && s.top3Mass96 < 0.360d) {
            return "CHAOS";
        }

        if (s.driftRisk >= 0.560d) {
            return "DRIFT";
        }

        if (s.breakRisk >= 0.520d) {
            return "BROKEN";
        }

        if (s.recoveryScore >= 0.560d && s.driftRisk < 0.430d) {
            return "RECOVERING";
        }

        if (s.top3Mass96 >= 0.420d && s.driftRisk < 0.340d) {
            return "STABLE";
        }

        if (s.hotReverseRisk >= 0.420d || s.falseStrongRisk >= 0.420d) {
            return "REVERSING";
        }

        return "MIXED";
    }

    private String buildTrendKey(PositionState s) {
        StringBuilder sb = new StringBuilder();

        sb.append("TL=");
        sb.append(bucketText("", s.trendLife));
        sb.append("|T3=");
        sb.append(bucketText("", s.top3Mass96));
        sb.append("|E=");
        sb.append(bucketText("", s.entropy96));

        return sb.toString();
    }

    private String buildTransitionKey(PositionState s) {
        StringBuilder sb = new StringBuilder();

        sb.append("FROM_");
        sb.append(two(s.lastNumber));
        sb.append("_MAX_");
        sb.append(bucketText("", s.transitionMax()));

        return sb.toString();
    }

    private String buildNumberShapeKey(PositionState s, int[] top3) {
        if (s == null || top3 == null || top3.length < 3) {
            return "";
        }

        int low = 0;
        int mid = 0;
        int high = 0;
        int odd = 0;
        int even = 0;

        for (int i = 0; i < top3.length; i++) {
            int n = top3[i];

            if (n <= 3) {
                low++;
            } else if (n <= 7) {
                mid++;
            } else {
                high++;
            }

            if (n % 2 == 0) {
                even++;
            } else {
                odd++;
            }
        }

        return cleanKey("LMH_" + low + "_" + mid + "_" + high + "_OE_" + odd + "_" + even);
    }

    private String buildComboShapeKey(PositionState s, int[] top3) {
        if (s == null || top3 == null || top3.length < 3) {
            return "";
        }

        int span = Math.abs(top3[2] - top3[0]);

        return cleanKey(
                "P"
                        + s.position
                        + "_K3_"
                        + maskText(buildK3Mask(top3[0], top3[1], top3[2]))
                        + "_SPAN_"
                        + span
                        + "_GAP_"
                        + bucketText("", s.comboGap)
                        + "_RISK_"
                        + bucketText("", s.riskScore));
    }

    private String buildShapeKey(PositionState s) {
        StringBuilder sb = new StringBuilder();

        sb.append("P");
        sb.append(s.position);
        sb.append("|");
        sb.append(s.stateMode);
        sb.append("|E96=");
        sb.append(bucketText("", s.entropy96));
        sb.append("|T3=");
        sb.append(bucketText("", s.top3Mass96));
        sb.append("|D=");
        sb.append(bucketText("", s.driftRisk));
        sb.append("|R=");
        sb.append(bucketText("", s.recoveryScore));
        sb.append("|L=");
        sb.append(bucketText("", s.trendLife));
        sb.append("|LINK=");
        sb.append(s.positionLinkKey);

        return cleanKey(sb.toString());
    }

    private String buildStateKey(PositionState s) {
        StringBuilder sb = new StringBuilder();

        sb.append("P");
        sb.append(s.position);
        sb.append("|S=");
        sb.append(s.stateMode);
        sb.append("|");
        sb.append(s.shapeKey);

        return cleanKey(sb.toString());
    }

    private String buildExpectedShape(PositionState s, int[] top3) {
        StringBuilder sb = new StringBuilder();

        sb.append("状态=");
        sb.append(s.stateMode);
        sb.append("｜位置=");
        sb.append(s.position);
        sb.append("｜候选=");

        for (int i = 0; i < top3.length; i++) {
            if (i > 0) {
                sb.append("/");
            }

            sb.append(two(top3[i]));
        }

        sb.append("｜P=");
        sb.append(percent(s.finalP));
        sb.append("｜Gate=");
        sb.append(percent(s.dynamicGate));
        sb.append("｜Risk=");
        sb.append(percent(s.riskScore));
        sb.append("｜Link=");
        sb.append(s.positionLinkSummary);

        return sb.toString();
    }

    private String buildWhyExpectedHit(PositionState s, int k3Mask) {
        StringBuilder sb = new StringBuilder();

        sb.append("教师判断该位置K3可选：");
        sb.append("状态=");
        sb.append(s.stateMode);
        sb.append("，窗口=12/24/48/96/120/240");
        sb.append("，top3集中度=");
        sb.append(percent(s.top3Mass96));
        sb.append("，趋势寿命=");
        sb.append(percent(s.trendLife));
        sb.append("，漂移风险=");
        sb.append(percent(s.driftRisk));
        sb.append("，K3=");
        sb.append(maskText(k3Mask));
        sb.append("，位置关联=");
        sb.append(s.positionLinkSummary);
        sb.append("。");

        return sb.toString();
    }

    private String buildWhyActualHit(PositionState s, int actualNumber, boolean comboHit) {
        StringBuilder sb = new StringBuilder();

        sb.append("实际号码=");
        sb.append(two(actualNumber));

        if (comboHit) {
            sb.append("，在教师K3范围内，说明该状态下号码画像、转移链、集中度或位置关联判断有效。");
        } else {
            sb.append("，不在教师K3范围内，说明本期出现反事实漏判，需要降低假强号码权重，并提高实际号码在相似状态下的关注。");
        }

        sb.append("号码P=");
        sb.append(percent(isValidNumber(actualNumber) ? s.numberP[actualNumber] : 0.0d));
        sb.append("，miss=");
        sb.append(isValidNumber(actualNumber) ? s.miss[actualNumber] : 0);
        sb.append("，link=");
        sb.append(s.positionLinkSummary);
        sb.append("。");

        return sb.toString();
    }

    private String buildWhyMiss(PositionState s, int actualNumber, int k3Mask) {
        StringBuilder sb = new StringBuilder();

        sb.append("漏判原因：实际 ");
        sb.append(two(actualNumber));
        sb.append(" 未进入K3 ");
        sb.append(maskText(k3Mask));
        sb.append("；");

        if (s.falseStrongRisk >= 0.40d) {
            sb.append("假强风险偏高；");
        }

        if (s.driftRisk >= 0.45d) {
            sb.append("短窗漂移偏高；");
        }

        if (s.hotReverseRisk >= 0.35d) {
            sb.append("热号反转风险存在；");
        }

        if (s.comboGap < 0.018d) {
            sb.append("第三名与第四名差距小；");
        }

        if (isValidNumber(actualNumber) && s.transitionProb[actualNumber] > 0.0d) {
            sb.append("实际号码转移支持=");
            sb.append(percent(s.transitionProb[actualNumber]));
            sb.append("；");
        }

        if (s.linkPosition > 0) {
            sb.append("位置关联=");
            sb.append(s.positionLinkSummary);
            sb.append("；");
        }

        sb.append("下次相似状态应提高实际号码权重，降低未中K3号码权重，并检查该位置是否从延续转为反转。");

        return sb.toString();
    }

    private String buildNextAdvice(PositionState s, boolean comboHit) {
        StringBuilder sb = new StringBuilder();

        if (comboHit) {
            sb.append("奖励：保留该状态画像，适度增强命中K3组合、状态号码画像、位置关联和转移链权重。");
        } else {
            sb.append("惩罚：降低该状态下当前K3组合权重，增加实际号码在相似状态下的候选权重。");
        }

        if (s.riskScore >= 0.50d) {
            sb.append(" 风险偏高，下一期必须提高动态门槛。");
        }

        if (s.comboGap < 0.018d) {
            sb.append(" 第3名与第4名差距小，下一期同类形态不宜轻易BET。");
        }

        if (s.positionLinkScore >= 0.62d) {
            sb.append(" 位置关联较强，下一期该位置要同步参考P");
            sb.append(s.linkPosition);
            sb.append("的状态。");
        }

        return sb.toString();
    }

    /* =========================
     * DbStore 兼容写入
     * ========================= */

    private int saveLearningCasesCompat(DbStore store, ArrayList<Models.LearningCase> cases) {
        if (store == null || cases == null || cases.size() == 0) {
            return 0;
        }

        try {
            return store.saveLearningCases(cases);
        } catch (Throwable e) {
        }

        int saved = 0;

        for (int i = 0; i < cases.size(); i++) {
            Models.LearningCase c = cases.get(i);

            if (c == null) {
                continue;
            }

            Object r =
                    invokeOptional(
                            store,
                            "saveLearningCase",
                            new Class[] {Models.LearningCase.class},
                            new Object[] {c});

            if (r != NO_METHOD) {
                saved++;
            }
        }

        return saved;
    }

    private int updateStateNumberProfileCompat(
            DbStore store,
            String stateKey,
            int position,
            int number,
            boolean hit,
            double support,
            double risk,
            int sample) {

        if (store == null || stateKey == null || stateKey.length() == 0) {
            return 0;
        }

        if (!isValidPosition(position) || !isValidNumber(number)) {
            return 0;
        }

        Object[] args1 =
                new Object[] {
                    stateKey,
                    Integer.valueOf(position),
                    Integer.valueOf(number),
                    Boolean.valueOf(hit),
                    Double.valueOf(support),
                    Double.valueOf(risk),
                    Integer.valueOf(sample)
                };

        Class[] types1 =
                new Class[] {
                    String.class,
                    Integer.TYPE,
                    Integer.TYPE,
                    Boolean.TYPE,
                    Double.TYPE,
                    Double.TYPE,
                    Integer.TYPE
                };

        if (invokeAnyStoreMethod(
                        store,
                        new String[] {
                            "updateStateNumberProfile",
                            "updateStateNumberProfileCache",
                            "learnStateNumberProfile",
                            "upsertStateNumberProfile"
                        },
                        types1,
                        args1)
                != NO_METHOD) {
            return 1;
        }

        Object[] args2 =
                new Object[] {
                    stateKey,
                    Integer.valueOf(position),
                    Integer.valueOf(number),
                    Integer.valueOf(hit ? 1 : 0),
                    Double.valueOf(support),
                    Double.valueOf(risk),
                    Integer.valueOf(sample)
                };

        Class[] types2 =
                new Class[] {
                    String.class,
                    Integer.TYPE,
                    Integer.TYPE,
                    Integer.TYPE,
                    Double.TYPE,
                    Double.TYPE,
                    Integer.TYPE
                };

        if (invokeAnyStoreMethod(
                        store,
                        new String[] {
                            "updateStateNumberProfile",
                            "updateStateNumberProfileCache",
                            "learnStateNumberProfile",
                            "upsertStateNumberProfile"
                        },
                        types2,
                        args2)
                != NO_METHOD) {
            return 1;
        }

        return 0;
    }

    private int updateRuntimeProfileCompat(
            DbStore store,
            String stateKey,
            int position,
            int k3Mask,
            boolean hit,
            double finalP,
            double gate,
            double risk,
            double teacherWeight,
            double confidence) {

        if (store == null || stateKey == null || stateKey.length() == 0) {
            return 0;
        }

        Object[] args1 =
                new Object[] {
                    stateKey,
                    Integer.valueOf(position),
                    Integer.valueOf(k3Mask),
                    Boolean.valueOf(hit),
                    Double.valueOf(finalP),
                    Double.valueOf(gate),
                    Double.valueOf(risk),
                    Double.valueOf(teacherWeight),
                    Double.valueOf(confidence)
                };

        Class[] types1 =
                new Class[] {
                    String.class,
                    Integer.TYPE,
                    Integer.TYPE,
                    Boolean.TYPE,
                    Double.TYPE,
                    Double.TYPE,
                    Double.TYPE,
                    Double.TYPE,
                    Double.TYPE
                };

        if (invokeAnyStoreMethod(
                        store,
                        new String[] {
                            "updateRuntimeProfile",
                            "updateRuntimeProfileCache",
                            "learnRuntimeProfile",
                            "upsertRuntimeProfile"
                        },
                        types1,
                        args1)
                != NO_METHOD) {
            return 1;
        }

        Object[] args2 =
                new Object[] {
                    stateKey,
                    Boolean.valueOf(hit),
                    Double.valueOf(finalP),
                    Double.valueOf(risk),
                    Integer.valueOf(1)
                };

        Class[] types2 =
                new Class[] {String.class, Boolean.TYPE, Double.TYPE, Double.TYPE, Integer.TYPE};

        if (invokeAnyStoreMethod(
                        store,
                        new String[] {
                            "updateRuntimeProfileCache",
                            "updateRuntimeProfile",
                            "learnRuntimeProfile",
                            "upsertRuntimeProfile"
                        },
                        types2,
                        args2)
                != NO_METHOD) {
            return 1;
        }

        Object[] args3 =
                new Object[] {
                    stateKey,
                    Integer.valueOf(hit ? 1 : 0),
                    Double.valueOf(finalP),
                    Double.valueOf(risk),
                    Integer.valueOf(1)
                };

        Class[] types3 =
                new Class[] {String.class, Integer.TYPE, Double.TYPE, Double.TYPE, Integer.TYPE};

        if (invokeAnyStoreMethod(
                        store,
                        new String[] {
                            "updateRuntimeProfileCache",
                            "updateRuntimeProfile",
                            "learnRuntimeProfile",
                            "upsertRuntimeProfile"
                        },
                        types3,
                        args3)
                != NO_METHOD) {
            return 1;
        }

        return 0;
    }

    private int updatePositionLinkProfileCompat(
            DbStore store,
            String linkKey,
            int position,
            int linkPosition,
            boolean hit,
            double linkScore,
            double risk) {

        if (store == null || linkKey == null || linkKey.length() == 0) {
            return 0;
        }

        if (!isValidPosition(position) || !isValidPosition(linkPosition)) {
            return 0;
        }

        Object[] args1 =
                new Object[] {
                    linkKey,
                    Integer.valueOf(position),
                    Integer.valueOf(linkPosition),
                    Boolean.valueOf(hit),
                    Double.valueOf(linkScore),
                    Double.valueOf(risk),
                    Integer.valueOf(1)
                };

        Class[] types1 =
                new Class[] {
                    String.class,
                    Integer.TYPE,
                    Integer.TYPE,
                    Boolean.TYPE,
                    Double.TYPE,
                    Double.TYPE,
                    Integer.TYPE
                };

        if (invokeAnyStoreMethod(
                        store,
                        new String[] {
                            "updatePositionLinkProfile",
                            "updatePositionLinkProfileCache",
                            "learnPositionLinkProfile",
                            "upsertPositionLinkProfile"
                        },
                        types1,
                        args1)
                != NO_METHOD) {
            return 1;
        }

        Object[] args2 =
                new Object[] {
                    linkKey,
                    Integer.valueOf(position),
                    Integer.valueOf(linkPosition),
                    Integer.valueOf(hit ? 1 : 0),
                    Double.valueOf(linkScore),
                    Double.valueOf(risk),
                    Integer.valueOf(1)
                };

        Class[] types2 =
                new Class[] {
                    String.class,
                    Integer.TYPE,
                    Integer.TYPE,
                    Integer.TYPE,
                    Double.TYPE,
                    Double.TYPE,
                    Integer.TYPE
                };

        if (invokeAnyStoreMethod(
                        store,
                        new String[] {
                            "updatePositionLinkProfile",
                            "updatePositionLinkProfileCache",
                            "learnPositionLinkProfile",
                            "upsertPositionLinkProfile"
                        },
                        types2,
                        args2)
                != NO_METHOD) {
            return 1;
        }

        return 0;
    }

    private int updateShapeMemoryCompat(
            DbStore store,
            String shapeKey,
            int position,
            boolean hit,
            boolean fail,
            boolean reverse) {

        if (store == null || shapeKey == null || shapeKey.length() == 0) {
            return 0;
        }

        Object result =
                invokeOptional(
                        store,
                        "updateShapeMemory",
                        new Class[] {
                            String.class, Integer.TYPE, Boolean.TYPE, Boolean.TYPE, Boolean.TYPE
                        },
                        new Object[] {
                            shapeKey,
                            Integer.valueOf(position),
                            Boolean.valueOf(hit),
                            Boolean.valueOf(fail),
                            Boolean.valueOf(reverse)
                        });

        return result == NO_METHOD ? 0 : 1;
    }

    private int updateComboMemoryCompat(
            DbStore store,
            String shapeKey,
            int position,
            int k3Mask,
            boolean hit,
            double comboGap,
            double trendLife) {

        if (store == null || shapeKey == null || shapeKey.length() == 0) {
            return 0;
        }

        Object result =
                invokeOptional(
                        store,
                        "updateComboMemory",
                        new Class[] {
                            String.class,
                            Integer.TYPE,
                            Integer.TYPE,
                            Boolean.TYPE,
                            Double.TYPE,
                            Double.TYPE
                        },
                        new Object[] {
                            shapeKey,
                            Integer.valueOf(position),
                            Integer.valueOf(k3Mask),
                            Boolean.valueOf(hit),
                            Double.valueOf(comboGap),
                            Double.valueOf(trendLife)
                        });

        return result == NO_METHOD ? 0 : 1;
    }

    private int updateTeacherCalibrationAuditCompat(DbStore store, double p, boolean hit) {
        if (store == null || p <= 0.0d) {
            return 0;
        }

        Object result =
                invokeOptional(
                        store,
                        "updateTeacherMatrixCalibrationAudit",
                        new Class[] {Double.TYPE, Boolean.TYPE},
                        new Object[] {Double.valueOf(p), Boolean.valueOf(hit)});

        if (result != NO_METHOD) {
            return 1;
        }

        result =
                invokeOptional(
                        store,
                        "updateTeacherProbabilityAudit",
                        new Class[] {Double.TYPE, Boolean.TYPE},
                        new Object[] {Double.valueOf(p), Boolean.valueOf(hit)});

        if (result != NO_METHOD) {
            return 1;
        }

        result =
                invokeOptional(
                        store,
                        "updateTeacherMatrixProbabilityAudit",
                        new Class[] {Double.TYPE, Boolean.TYPE},
                        new Object[] {Double.valueOf(p), Boolean.valueOf(hit)});

        return result == NO_METHOD ? 0 : 1;
    }

    private int updateProbabilityAuditCompat(DbStore store, double p, boolean hit) {
        if (store == null || p <= 0.0d) {
            return 0;
        }

        Object result =
                invokeOptional(
                        store,
                        "updateProbabilityAudit",
                        new Class[] {Double.TYPE, Boolean.TYPE},
                        new Object[] {Double.valueOf(p), Boolean.valueOf(hit)});

        if (result != NO_METHOD) {
            return 1;
        }

        result =
                invokeOptional(
                        store,
                        "updateProbabilityAuditCache",
                        new Class[] {Double.TYPE, Boolean.TYPE},
                        new Object[] {Double.valueOf(p), Boolean.valueOf(hit)});

        return result == NO_METHOD ? 0 : 1;
    }

    private int saveTeacherMatrixFromLearningCasesOptional(
            DbStore store, List<Models.LearningCase> cases) {
        if (store == null || cases == null || cases.size() == 0) {
            return 0;
        }

        Object result =
                invokeOptional(
                        store,
                        "saveTeacherMatrixFromLearningCases",
                        new Class[] {List.class},
                        new Object[] {cases});

        return objectToInt(result, 0);
    }

    private void updateTeacherMatrixSummaryStateOptional(DbStore store) {
        if (store == null) {
            return;
        }

        invokeOptional(store, "updateTeacherMatrixSummaryState", new Class[] {}, new Object[] {});
    }

    private int optionalInt(DbStore store, String methodName, int def) {
        if (store == null || methodName == null || methodName.length() == 0) {
            return def;
        }

        Object result = invokeOptional(store, methodName, new Class[] {}, new Object[] {});
        return objectToInt(result, def);
    }

    private Object invokeAnyStoreMethod(
            DbStore store, String[] names, Class[] types, Object[] args) {
        if (store == null || names == null) {
            return NO_METHOD;
        }

        for (int i = 0; i < names.length; i++) {
            Object r = invokeOptional(store, names[i], types, args);

            if (r != NO_METHOD) {
                return r;
            }
        }

        return NO_METHOD;
    }

    private Object invokeOptional(Object target, String methodName, Class[] types, Object[] args) {
        if (target == null || methodName == null || methodName.length() == 0) {
            return NO_METHOD;
        }

        try {
            Method m = target.getClass().getMethod(methodName, types);
            return m.invoke(target, args);
        } catch (Throwable e) {
            return NO_METHOD;
        }
    }

    private int objectToInt(Object value, int def) {
        if (value == null || value == NO_METHOD) {
            return def;
        }

        if (value instanceof Integer) {
            return ((Integer) value).intValue();
        }

        if (value instanceof Long) {
            return (int) ((Long) value).longValue();
        }

        if (value instanceof Number) {
            return ((Number) value).intValue();
        }

        try {
            return Integer.parseInt(String.valueOf(value));
        } catch (Throwable e) {
            return def;
        }
    }

    /* =========================
     * 基础统计工具
     * ========================= */

    private int[] countWindow(List<Models.Draw> history, int position, int window) {
        int[] count = new int[NUMBER_MAX + 1];

        if (history == null || history.size() == 0 || window <= 0) {
            return count;
        }

        int start = history.size() - window;

        if (start < 0) {
            start = 0;
        }

        for (int i = start; i < history.size(); i++) {
            Models.Draw d = history.get(i);
            int n = getNumber(d, position);

            if (isValidNumber(n)) {
                count[n]++;
            }
        }

        return count;
    }

    private double[] toProbability(int[] count) {
        double[] p = new double[NUMBER_MAX + 1];

        if (count == null || count.length <= NUMBER_MAX) {
            for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
                p[n] = BASE_NUMBER_P;
            }

            return p;
        }

        int total = 0;

        for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
            total += count[n];
        }

        if (total <= 0) {
            for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
                p[n] = BASE_NUMBER_P;
            }

            return p;
        }

        for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
            p[n] = ((double) count[n]) / ((double) total);
        }

        return p;
    }

    private void buildMissCounts(List<Models.Draw> history, int position, int[] miss) {
        if (miss == null || miss.length <= NUMBER_MAX) {
            return;
        }

        for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
            miss[n] = history == null ? 0 : history.size();
        }

        if (history == null) {
            return;
        }

        for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
            int gap = 0;

            for (int i = history.size() - 1; i >= 0; i--) {
                int v = getNumber(history.get(i), position);

                if (v == n) {
                    break;
                }

                gap++;
            }

            miss[n] = gap;
        }
    }

    private void buildTransitionProb(
            List<Models.Draw> history, int position, int fromNumber, double[] out) {
        if (out == null || out.length <= NUMBER_MAX) {
            return;
        }

        for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
            out[n] = BASE_NUMBER_P;
        }

        if (history == null || history.size() < 2 || !isValidNumber(fromNumber)) {
            return;
        }

        int[] count = new int[NUMBER_MAX + 1];
        int total = 0;

        for (int i = 1; i < history.size(); i++) {
            int prev = getNumber(history.get(i - 1), position);

            if (prev != fromNumber) {
                continue;
            }

            int cur = getNumber(history.get(i), position);

            if (!isValidNumber(cur)) {
                continue;
            }

            count[cur]++;
            total++;
        }

        if (total <= 0) {
            return;
        }

        for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
            out[n] = ((double) count[n]) / ((double) total);
        }
    }

    private double entropy01(double[] p) {
        if (p == null || p.length <= NUMBER_MAX) {
            return 1.0d;
        }

        double e = 0.0d;

        for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
            double v = p[n];

            if (v <= 0.0d) {
                continue;
            }

            e -= v * Math.log(v);
        }

        double max = Math.log(10.0d);

        if (max <= 0.0d) {
            return 1.0d;
        }

        return clamp01(e / max);
    }

    private double topMass(double[] p, int topN) {
        if (p == null || p.length <= NUMBER_MAX || topN <= 0) {
            return 0.0d;
        }

        boolean[] used = new boolean[NUMBER_MAX + 1];
        double mass = 0.0d;

        for (int i = 0; i < topN; i++) {
            int best = 0;
            double bestV = -1.0d;

            for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
                if (used[n]) {
                    continue;
                }

                if (p[n] > bestV) {
                    bestV = p[n];
                    best = n;
                }
            }

            if (best > 0) {
                used[best] = true;
                mass += p[best];
            }
        }

        return clamp01(mass);
    }

    private double avgAbsDiff(double[] a, double[] b) {
        if (a == null || b == null || a.length <= NUMBER_MAX || b.length <= NUMBER_MAX) {
            return 0.0d;
        }

        double total = 0.0d;

        for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
            total += Math.abs(a[n] - b[n]);
        }

        return total / 10.0d;
    }

    private double missRecoveryScore(int miss) {
        if (miss <= 0) {
            return 0.02d;
        }

        if (miss >= 30) {
            return 1.0d;
        }

        return ((double) miss) / 30.0d;
    }

    /* =========================
     * 反射读写工具
     * ========================= */

    private int firstInt(Object obj, String[] fields, int def) {
        if (fields == null) {
            return def;
        }

        for (int i = 0; i < fields.length; i++) {
            int v = readInt(obj, fields[i], Integer.MIN_VALUE);

            if (v != Integer.MIN_VALUE) {
                return v;
            }
        }

        return def;
    }

    private double firstDouble(Object obj, String[] fields, double def) {
        if (fields == null) {
            return def;
        }

        for (int i = 0; i < fields.length; i++) {
            double v = readDouble(obj, fields[i], Double.NaN);

            if (!Double.isNaN(v)) {
                return v;
            }
        }

        return def;
    }

    private String firstString(Object obj, String[] fields, String def) {
        if (fields == null) {
            return def;
        }

        for (int i = 0; i < fields.length; i++) {
            String v = readString(obj, fields[i], null);

            if (v != null && v.length() > 0) {
                return v;
            }
        }

        return def;
    }

    private int readInt(Object obj, String fieldName, int def) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return def;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            Object v = f.get(obj);

            if (v instanceof Number) {
                return ((Number) v).intValue();
            }

            if (v instanceof String) {
                return Integer.parseInt((String) v);
            }
        } catch (Throwable e) {
        }

        return def;
    }

    private double readDouble(Object obj, String fieldName, double def) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return def;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            Object v = f.get(obj);

            if (v instanceof Number) {
                double d = ((Number) v).doubleValue();

                if (Double.isNaN(d) || Double.isInfinite(d)) {
                    return def;
                }

                return d;
            }

            if (v instanceof String) {
                double d = Double.parseDouble((String) v);

                if (Double.isNaN(d) || Double.isInfinite(d)) {
                    return def;
                }

                return d;
            }
        } catch (Throwable e) {
        }

        return def;
    }

    private String readString(Object obj, String fieldName, String def) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return def;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            Object v = f.get(obj);

            if (v == null) {
                return def;
            }

            return String.valueOf(v);
        } catch (Throwable e) {
        }

        return def;
    }

    private void writeInt(Object obj, String fieldName, int value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            f.set(obj, Integer.valueOf(value));
        } catch (Throwable e) {
        }
    }

    private void writeDouble(Object obj, String fieldName, double value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            f.set(obj, Double.valueOf(value));
        } catch (Throwable e) {
        }
    }

    private void writeString(Object obj, String fieldName, String value) {
        if (obj == null || fieldName == null || fieldName.length() == 0) {
            return;
        }

        try {
            Field f = obj.getClass().getField(fieldName);
            f.set(obj, value == null ? "" : value);
        } catch (Throwable e) {
        }
    }

    /* =========================
     * 基础安全工具
     * ========================= */

    private int getNumber(Models.Draw d, int position) {
        if (d == null || d.numbers == null || position < POSITION_MIN || position > POSITION_MAX) {
            return 0;
        }

        int index = position - 1;

        if (index < 0 || index >= d.numbers.length) {
            return 0;
        }

        return d.numbers[index];
    }

    private boolean isValidDraw(Models.Draw d) {
        if (d == null) {
            return false;
        }

        if (d.issue == null || d.issue.length() == 0) {
            return false;
        }

        if (d.numbers == null || d.numbers.length < 10) {
            return false;
        }

        for (int i = 0; i < 10; i++) {
            if (!isValidNumber(d.numbers[i])) {
                return false;
            }
        }

        return true;
    }

    private boolean isValidPosition(int position) {
        return position >= POSITION_MIN && position <= POSITION_MAX;
    }

    private boolean isValidNumber(int number) {
        return number >= NUMBER_MIN && number <= NUMBER_MAX;
    }

    private int buildK3Mask(int a, int b, int c) {
        int mask = 0;

        if (isValidNumber(a)) {
            mask |= (1 << a);
        }

        if (isValidNumber(b)) {
            mask |= (1 << b);
        }

        if (isValidNumber(c)) {
            mask |= (1 << c);
        }

        return mask;
    }

    private boolean containsNumber(int mask, int number) {
        if (!isValidNumber(number)) {
            return false;
        }

        return (mask & (1 << number)) != 0;
    }

    private boolean contains(int[] arr, int length, int value) {
        if (arr == null || length <= 0) {
            return false;
        }

        for (int i = 0; i < length && i < arr.length; i++) {
            if (arr[i] == value) {
                return true;
            }
        }

        return false;
    }

    private void sort3(int[] a) {
        if (a == null || a.length < 3) {
            return;
        }

        for (int i = 0; i < 3; i++) {
            for (int j = i + 1; j < 3; j++) {
                if (a[j] < a[i]) {
                    int t = a[i];
                    a[i] = a[j];
                    a[j] = t;
                }
            }
        }
    }

    private int zoneOf(int n) {
        if (n <= 3) {
            return 1;
        }

        if (n <= 7) {
            return 2;
        }

        return 3;
    }

    private String maskText(int mask) {
        StringBuilder sb = new StringBuilder();

        for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
            if (containsNumber(mask, n)) {
                if (sb.length() > 0) {
                    sb.append("/");
                }

                sb.append(two(n));
            }
        }

        if (sb.length() == 0) {
            return "--";
        }

        return sb.toString();
    }

    private String buildCaseKey(String taskKey, String segmentKey, String issue, int position) {
        StringBuilder sb = new StringBuilder();

        sb.append(safe(taskKey));
        sb.append("_");
        sb.append(safe(segmentKey));
        sb.append("_");
        sb.append(safe(issue));
        sb.append("_P");
        sb.append(position);

        return cleanKey(sb.toString());
    }

    private int compareIssue(String a, String b) {
        String aa = normalizeIssue(a);
        String bb = normalizeIssue(b);

        if (aa.length() == 0 && bb.length() == 0) {
            return 0;
        }

        if (aa.length() == 0) {
            return -1;
        }

        if (bb.length() == 0) {
            return 1;
        }

        if (aa.length() > bb.length()) {
            return 1;
        }

        if (aa.length() < bb.length()) {
            return -1;
        }

        return aa.compareTo(bb);
    }

    private String normalizeIssue(String issue) {
        if (issue == null) {
            return "";
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < issue.length(); i++) {
            char c = issue.charAt(i);

            if (c >= '0' && c <= '9') {
                sb.append(c);
            }
        }

        String s = sb.toString();

        while (s.length() > 1 && s.startsWith("0")) {
            s = s.substring(1);
        }

        return s;
    }

    private String bucketText(String prefix, double v) {
        String p = prefix == null ? "" : prefix;

        if (v <= 0.0001d) {
            return p + "0";
        }

        if (v < 0.200d) {
            return p + "LOW";
        }

        if (v < 0.450d) {
            return p + "MID";
        }

        if (v < 0.700d) {
            return p + "HIGH";
        }

        return p + "EXT";
    }

    private String cleanKey(String s) {
        if (s == null) {
            return "";
        }

        String out = s.replace('|', '_');
        out = out.replace(' ', '_');
        out = out.replace('\n', '_');
        out = out.replace('\r', '_');
        out = out.replace('\t', '_');

        if (out.length() > 180) {
            out = out.substring(0, 180);
        }

        return out;
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }

    private String two(int n) {
        if (n <= 0) {
            return "00";
        }

        if (n < 10) {
            return "0" + n;
        }

        return String.valueOf(n);
    }

    private String percent(double p) {
        int v = (int) Math.round(clamp01(p) * 100.0d);
        return String.valueOf(v) + "%";
    }

    private String signedPercent(double p) {
        int v = (int) Math.round(p * 100.0d);

        if (v > 0) {
            return "+" + v + "%";
        }

        return String.valueOf(v) + "%";
    }

    private String limit(String text, int max) {
        if (text == null) {
            return "";
        }

        if (max <= 0 || text.length() <= max) {
            return text;
        }

        return text.substring(0, max) + "...";
    }

    private double clamp01(double v) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return 0.0d;
        }

        if (v < 0.0d) {
            return 0.0d;
        }

        if (v > 1.0d) {
            return 1.0d;
        }

        return v;
    }

    private double clamp(double v, double min, double max) {
        if (Double.isNaN(v) || Double.isInfinite(v)) {
            return min;
        }

        if (v < min) {
            return min;
        }

        if (v > max) {
            return max;
        }

        return v;
    }

    private int parseInt(String text, int fallback) {
        if (text == null || text.length() == 0) {
            return fallback;
        }

        try {
            return Integer.parseInt(text);
        } catch (Throwable e) {
            return fallback;
        }
    }

    private long parseLong(String text, long fallback) {
        if (text == null || text.length() == 0) {
            return fallback;
        }

        try {
            return Long.parseLong(text);
        } catch (Throwable e) {
            return fallback;
        }
    }

    private void cooperativePause(long sleepMs) {
        try {
            Thread.yield();

            if (sleepMs > 0L) {
                Thread.sleep(sleepMs);
            }
        } catch (Throwable e) {
        }
    }

    private void writeState(DbStore store, String key, String value) {
        if (store == null || key == null || key.length() == 0) {
            return;
        }

        try {
            store.setState(key, value == null ? "" : value);
        } catch (Throwable e) {
        }
    }

    private static final class TeacherProfile {
        int sample;
        int stateSample;
        int comboSample;
        int positive;
        int negative;

        double hitRate;
        double comboHitRate;
        double confidence;
        double probAdjust;
        double gateAdjust;
        double riskPenalty;

        String status;
        String summary;

        TeacherProfile() {
            sample = 0;
            stateSample = 0;
            comboSample = 0;
            positive = 0;
            negative = 0;

            hitRate = 0.0d;
            comboHitRate = 0.0d;
            confidence = 0.0d;
            probAdjust = 0.0d;
            gateAdjust = 0.0d;
            riskPenalty = 0.0d;

            status = "";
            summary = "";
        }
    }

    private static final class PositionState {
        int position;
        int historySize;
        String baseIssue;
        String targetIssue;

        int lastNumber;
        int prevNumber;
        int actualNumber;
        int k3Mask;

        int linkPosition;
        int linkLastNumber;
        double positionLinkScore;
        String positionLinkKey;
        String positionLinkSummary;

        int[] miss = new int[NUMBER_MAX + 1];
        double[] numberP = new double[NUMBER_MAX + 1];
        double[] numberScore = new double[NUMBER_MAX + 1];
        double[] transitionProb = new double[NUMBER_MAX + 1];

        double entropy12;
        double entropy24;
        double entropy48;
        double entropy96;
        double entropy120;
        double entropy240;

        double top3Mass12;
        double top3Mass24;
        double top3Mass48;
        double top3Mass96;
        double top3Mass120;
        double top3Mass240;

        double driftShort;
        double driftMid;
        double driftLong;
        double driftRisk;

        double breakRisk;
        double recoveryScore;
        double hotReverseRisk;
        double falseStrongRisk;
        double riskScore;
        double trendLife;

        double rawK3P;
        double finalP;
        double dynamicGate;
        double comboGap;
        double numberBias;
        double comboBias;

        double trendScore;
        double shapeScore;
        double profileScore;
        double stateConfidence;
        double teacherWeight;

        String stateMode;
        String stateKey;
        String shapeKey;
        String trendKey;
        String driftKey;
        String breakKey;
        String recoveryKey;
        String transitionKey;
        String expectedShape;
        String numberShapeKey;
        String comboShapeKey;

        PositionState() {
            baseIssue = "";
            targetIssue = "";

            stateMode = "";
            stateKey = "";
            shapeKey = "";
            trendKey = "";
            driftKey = "";
            breakKey = "";
            recoveryKey = "";
            transitionKey = "";
            expectedShape = "";
            numberShapeKey = "";
            comboShapeKey = "";

            linkPosition = 0;
            linkLastNumber = 0;
            positionLinkScore = 0.0d;
            positionLinkKey = "";
            positionLinkSummary = "";
        }

        double transitionMax() {
            double m = 0.0d;

            for (int n = NUMBER_MIN; n <= NUMBER_MAX; n++) {
                if (transitionProb[n] > m) {
                    m = transitionProb[n];
                }
            }

            return m;
        }
    }

    // V57_B08_EVIDENCE_ONLY_FULL_IMPLEMENTATION

    public static final String V57_B08_OWNER = "EVIDENCE_ONLY:TeacherMatrixEngine";

    public static final class V57TeacherEvidenceRow {
        public String key = "";
        public double support;
        public double counter;
        public double risk;
        public double confidence;
        public int sample;
        public double freshness;
        public long newestSourceTime;
        public long decisionCutoffTime;
        public String status = "NO_SAMPLE";
        public String hash = "";

        public boolean pitValid() {
            return newestSourceTime <= decisionCutoffTime;
        }

        public String canonical() {
            return key
                    + '|'
                    + support
                    + '|'
                    + counter
                    + '|'
                    + risk
                    + '|'
                    + confidence
                    + '|'
                    + sample
                    + '|'
                    + freshness
                    + '|'
                    + newestSourceTime
                    + '|'
                    + decisionCutoffTime
                    + '|'
                    + status;
        }
    }

    public static V57TeacherEvidenceRow v57b08EvidenceOnly(
            String key,
            int positive,
            int negative,
            int sample,
            long newest,
            long cutoff,
            long now,
            long horizonMs) {
        V57TeacherEvidenceRow r = new V57TeacherEvidenceRow();
        r.key = key == null ? "" : key;
        r.sample = Math.max(0, sample);
        r.newestSourceTime = newest;
        r.decisionCutoffTime = cutoff;
        r.support =
                r.sample == 0
                        ? 0.0d
                        : Math.max(0.0d, Math.min(1.0d, (double) Math.max(0, positive) / r.sample));
        r.counter =
                r.sample == 0
                        ? 0.0d
                        : Math.max(0.0d, Math.min(1.0d, (double) Math.max(0, negative) / r.sample));
        r.freshness =
                (newest <= 0 || now < newest)
                        ? 0.0d
                        : Math.max(
                                0.0d,
                                Math.min(
                                        1.0d,
                                        1.0d - (double) (now - newest) / Math.max(1L, horizonMs)));
        r.risk =
                Math.max(
                        0.0d,
                        Math.min(
                                1.0d,
                                (1.0d - r.freshness) + Math.max(0.0d, r.counter - r.support)));
        double rel = r.sample <= 0 ? 0.0d : 1.0d - Math.exp(-r.sample / 80.0d);
        r.confidence = Math.max(0.0d, Math.min(1.0d, rel * r.freshness * (1.0d - 0.5d * r.risk)));
        if (!r.pitValid()) r.status = "PIT_REJECT";
        else if (r.sample < 30) r.status = "WATCH";
        else if (r.risk >= 0.75d) r.status = "HARD_FREEZE";
        else if (r.confidence >= 0.65d && r.support > r.counter) r.status = "STRONG";
        else if (r.confidence >= 0.45d) r.status = "READY";
        else r.status = "SOFT_RISK";
        r.hash = v57b08Hash(r.canonical());
        return r;
    }

    public static boolean v57b08CanWriteProductionDecision() {
        return false;
    }

    private static String v57b08Hash(String text) {
        try {
            java.security.MessageDigest md = java.security.MessageDigest.getInstance("SHA-256");
            byte[] b = md.digest((text == null ? "" : text).getBytes("UTF-8"));
            StringBuilder sb = new StringBuilder(64);
            for (int i = 0; i < b.length; i++) {
                int v = b[i] & 255;
                if (v < 16) sb.append('0');
                sb.append(Integer.toHexString(v));
            }
            return sb.toString();
        } catch (Throwable t) {
            return "HASH_ERROR";
        }
    }
}
