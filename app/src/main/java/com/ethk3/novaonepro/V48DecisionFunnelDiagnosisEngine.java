package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import java.util.HashMap;
import java.util.Locale;

/** V48 diagnosis reads current exact identities and one Rank1 observation per target. */
public final class V48DecisionFunnelDiagnosisEngine {
    public static final String MODULE_VERSION = V48BuildManifest.DIAGNOSIS_VERSION;

    public static final class Report {
        public int observations, hit, completeIssues, candidateRows;
        public double rate, wilson, brier, avgP;
        public final int[] rankN = new int[6], rankHit = new int[6];
        public String identity = "", runtime = "", champion = "", responsibility = "", detail = "";
    }

    private V48DecisionFunnelDiagnosisEngine() {}

    public static String buildAndPersist(DbStore store) {
        Report r = build(store);
        try {
            if (store != null) {
                store.setState("v48_decision_funnel_version", MODULE_VERSION);
                store.setState("v48_decision_funnel_detail", r.detail);
                store.setState("v48_decision_funnel_rate", String.valueOf(r.rate));
                store.setState("v48_decision_funnel_observations", String.valueOf(r.observations));
            }
        } catch (Throwable ignore) {
        }
        return r.detail;
    }

    public static Report build(DbStore store) {
        Report r = new Report();
        if (store == null) {
            r.detail = "V48诊断=NO_STORE";
            return r;
        }
        r.identity = store.getState(BuildIdentity.STATE_STATUS, "");
        r.runtime = store.getState("v48_runtime_freshness", "");
        r.champion = store.getState(WalkForwardChampionEngine.STATE_STATUS, "MODEL_NOT_PROVEN");
        Cursor c = null;
        double err = 0;
        try {
            SQLiteDatabase db = store.readable();
            c =
                    db.query(
                            DbHelper.TABLE_V48_DIAGNOSTIC_OBSERVATION,
                            new String[] {"hit", "event_p"},
                            "settled=1 AND build_id=? AND cohort_id=? AND schema_version=? AND"
                                + " pipeline_version=? AND model_version=?",
                            new String[] {
                                V48BuildManifest.BUILD_ID,
                                V48BuildManifest.COHORT_ID,
                                V48BuildManifest.SCHEMA_VERSION,
                                V48BuildManifest.STRATEGY_VERSION,
                                V48BuildManifest.MODEL_VERSION
                            },
                            null,
                            null,
                            "target_issue ASC",
                            "2000");
            while (c != null && c.moveToNext()) {
                int h = c.getInt(0) == 1 ? 1 : 0;
                double p = c.getDouble(1);
                r.observations++;
                r.hit += h;
                r.avgP += p;
                double e = p - h;
                err += e * e;
            }
        } catch (Throwable e) {
            r.detail = "V48诊断观察读取错误:" + e.getClass().getSimpleName() + ":" + safe(e.getMessage());
            return r;
        } finally {
            if (c != null)
                try {
                    c.close();
                } catch (Throwable ignore) {
                }
        }
        r.rate = rate(r.hit, r.observations);
        r.wilson = wilson(r.hit, r.observations);
        if (r.observations > 0) {
            r.avgP /= r.observations;
            r.brier = err / r.observations;
        }
        HashMap<String, Integer> issueCounts = new HashMap<String, Integer>();
        c = null;
        try {
            SQLiteDatabase db = store.readable();
            c =
                    db.query(
                            DbHelper.TABLE_OOS_ACTION_LOG,
                            new String[] {"target_issue", "hit", "rank_slot"},
                            "settled=1 AND sample_source=? AND build_id=? AND cohort_id=? AND"
                                + " schema_version=? AND pipeline_version=? AND version=?",
                            new String[] {
                                OosActionLogEngine.SOURCE_LIVE_REALTIME,
                                V48BuildManifest.BUILD_ID,
                                V48BuildManifest.COHORT_ID,
                                V48BuildManifest.SCHEMA_VERSION,
                                V48BuildManifest.STRATEGY_VERSION,
                                V48BuildManifest.OOS_VERSION
                            },
                            null,
                            null,
                            "target_issue ASC,rank_slot ASC",
                            "8000");
            while (c != null && c.moveToNext()) {
                String issue = safe(c.getString(0));
                int rank = c.getInt(2), h = c.getInt(1) == 1 ? 1 : 0;
                r.candidateRows++;
                Integer current = issueCounts.get(issue);
                issueCounts.put(
                        issue, Integer.valueOf(current == null ? 1 : current.intValue() + 1));
                if (rank >= 1 && rank <= 5) {
                    r.rankN[rank]++;
                    r.rankHit[rank] += h;
                }
            }
            for (int n : issueCounts.values())
                if (n == AppConfig.V48_FORMAL_CANDIDATE_COUNT) r.completeIssues++;
        } catch (Throwable e) {
            try {
                store.setState(
                        "v48_diagnosis_oos_error",
                        e.getClass().getSimpleName() + ":" + safe(e.getMessage()));
            } catch (Throwable ignore) {
            }
        } finally {
            if (c != null)
                try {
                    c.close();
                } catch (Throwable ignore) {
                }
        }
        double r1 = rr(r, 1), r2 = rr(r, 2), r3 = rr(r, 3), r5 = rr(r, 5);
        ModelFailureAttributionEngine.Result cause =
                ModelFailureAttributionEngine.diagnose(
                        BuildIdentity.isRuntimeCompatible(r.identity),
                        r.observations,
                        r.rate,
                        r.wilson,
                        r.avgP,
                        r1,
                        r2,
                        r3,
                        r5,
                        r.completeIssues,
                        r.runtime,
                        r.champion);
        r.responsibility = cause.summary();
        r.detail = format(r, cause);
        return r;
    }

    private static String format(Report r, ModelFailureAttributionEngine.Result cause) {
        StringBuilder b = new StringBuilder();
        b.append("【V48单动作Champion·胜率与责任诊断】\n");
        b.append("身份=")
                .append(r.identity)
                .append("｜Champion=")
                .append(r.champion)
                .append("｜Runtime=")
                .append(r.runtime)
                .append('\n');
        b.append("闭环：Rank1诊断观察=")
                .append(r.observations)
                .append("，最终40候选行=")
                .append(r.candidateRows)
                .append("，完整期=")
                .append(r.completeIssues)
                .append('\n');
        b.append("真实模型Rank1反事实：")
                .append(r.hit)
                .append('/')
                .append(r.observations)
                .append('/')
                .append(pct(r.rate))
                .append("，Wilson下界=")
                .append(pct(r.wilson))
                .append("，均P=")
                .append(pct(r.avgP))
                .append("，Brier=")
                .append(String.format(Locale.US, "%.4f", r.brier))
                .append('\n');
        b.append("排名：R1=")
                .append(rank(r, 1))
                .append("｜R2=")
                .append(rank(r, 2))
                .append("｜R3=")
                .append(rank(r, 3))
                .append("｜R4=")
                .append(rank(r, 4))
                .append("｜R5=")
                .append(rank(r, 5))
                .append('\n');
        b.append("明确责任=").append(cause.primary).append("｜Owner=").append(cause.owner).append('\n');
        b.append("证据=").append(cause.evidence).append('\n');
        b.append("处理=").append(cause.action).append('\n');
        b.append("确定性口径：身份异常不再切断诊断观察；未晋级Challenger实时权重=0；真实BET只由V48单Lane签名授权。");
        return b.toString();
    }

    private static String rank(Report r, int x) {
        return r.rankHit[x] + "/" + r.rankN[x] + "/" + pct(rr(r, x));
    }

    private static double rr(Report r, int x) {
        return rate(r.rankHit[x], r.rankN[x]);
    }

    private static double rate(int h, int n) {
        return n <= 0 ? 0.0d : (double) h / n;
    }

    private static double wilson(int h, int n) {
        if (n <= 0) return 0;
        double z = 1.6448536269514722,
                p = (double) h / n,
                d = 1 + z * z / n,
                c = p + z * z / (2 * n),
                m = z * Math.sqrt((p * (1 - p) + z * z / (4 * n)) / n);
        return Math.max(0, (c - m) / d);
    }

    private static String pct(double v) {
        return String.format(Locale.US, "%.1f%%", 100 * v);
    }

    private static String safe(String v) {
        return v == null ? "" : v;
    }
}
