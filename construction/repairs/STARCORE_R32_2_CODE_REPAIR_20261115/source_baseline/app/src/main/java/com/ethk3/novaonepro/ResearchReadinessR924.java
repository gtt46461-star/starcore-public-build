package com.ethk3.novaonepro;

import android.database.Cursor;

/** Read-only explanation of the actual existing admission thresholds, never an authorization. */
public final class ResearchReadinessR924 {
    private ResearchReadinessR924() {}

    public static String missing(int n, double rate, double wilson, double brier, double ece,
            double coverage, double block, double regime, int miss, int paired, double delta, double q) {
        StringBuilder b = new StringBuilder();
        need(b, n >= CertificateBundleServiceR929.MIN_INFLUENCE_FUTURE_N,
                "未来样本", n, CertificateBundleServiceR929.MIN_INFLUENCE_FUTURE_N);
        need(b, rate >= CertificateBundleServiceR929.MIN_ENTRY_HIT_RATE,
                "命中率（含边界）", rate, CertificateBundleServiceR929.MIN_ENTRY_HIT_RATE);
        need(b, wilson >= CertificateBundleServiceR929.MIN_WILSON_LOWER,
                "Wilson下界（含边界）", wilson, CertificateBundleServiceR929.MIN_WILSON_LOWER);
        need(b, brier >= 0 && brier <= CertificateBundleServiceR929.MAX_ENTRY_BRIER,
                "Brier上限", brier, CertificateBundleServiceR929.MAX_ENTRY_BRIER);
        need(b, ece >= 0 && ece <= CertificateBundleServiceR929.MAX_ENTRY_ECE,
                "ECE上限", ece, CertificateBundleServiceR929.MAX_ENTRY_ECE);
        need(b, coverage >= CertificateBundleServiceR929.MIN_EVIDENCE_COVERAGE,
                "有效评估覆盖率（不是出手限额）", coverage, CertificateBundleServiceR929.MIN_EVIDENCE_COVERAGE);
        need(b, paired >= CertificateBundleServiceR929.MIN_INFLUENCE_PAIRED_N,
                "独立配对样本", paired, CertificateBundleServiceR929.MIN_INFLUENCE_PAIRED_N);
        need(b, (!Double.isNaN(delta) && !Double.isInfinite(delta)) && delta > 0, "配对提升需大于", delta, 0);
        // Legacy block/regime/miss/q columns are not interchangeable with the current formal
        // endpoint, independent-window and adjusted-p ledger. Do not manufacture those gates.
        return (b.length() == 0 ? "所列汇总数值达标（不是证书）" : b.toString())
                + "；仍需至少2个独立窗口、当前FWER调整p≤0.05、有效域/资源及PIT程序身份校验"
                + "；旧future_fdr_q不能直接冒充FWER证据";
    }

    private static void need(StringBuilder out, boolean pass, String name, double actual, double threshold) {
        if (pass) return;
        if (out.length() > 0) out.append('；');
        out.append(name).append('=').append(actual).append(" / 要求=").append(threshold);
    }

    public static String capture(DbStore store) {
        if (store == null) return "READ_ERROR:STORE_NULL";
        Cursor c = null;
        StringBuilder out = new StringBuilder("R925活跃算法接入缺口（完整资产另见全量清单）\n");
        try {
            c = store.readable().rawQuery("SELECT display_name_zh,future_n,future_hit_rate,"
                    + "future_wilson_lower,future_brier,future_ece,future_coverage,future_block_min,"
                    + "future_regime_min,future_max_miss,paired_mainline_n,paired_mainline_delta,"
                    + "future_fdr_q,status,lifecycle_reason,genome_id FROM " + DbHelper.TABLE_R908_ALGORITHM_GENOME
                    + " WHERE status IN ('IDEA_WAIT_FUTURE_OOS','OBSERVING_FUTURE_OOS','CHALLENGER_FUTURE_OOS')"
                    + " ORDER BY future_wilson_lower DESC,genome_id ASC", null);
            while (c.moveToNext()) {
                out.append(c.getString(0)).append("｜状态=").append(c.getString(13))
                        .append("｜生命周期=").append(c.getString(14)).append("｜缺口=")
                        .append(missing(c.getInt(1), value(c,2), value(c,3), value(c,4), value(c,5),
                                value(c,6), value(c,7), value(c,8), c.getInt(9), c.getInt(10), value(c,11), value(c,12)))
                        .append('\n');
            }
            out.append("有效评估覆盖率用于判断证据，不是每期出手配额；本说明不能授予REAL权限。\n")
                    .append("轮转游标：捕获=").append(store.getState("r924_predraw_next_lane", "0"))
                    .append("；学习=").append(store.getState("r924_continuity_next_lane", "0"));
        } catch (Throwable failure) { out.append("READ_ERROR:").append(failure.getClass().getSimpleName()); }
        finally { if (c != null) c.close(); }
        return out.toString();
    }

    private static double value(Cursor c, int column) {
        return c.isNull(column) ? Double.NaN : c.getDouble(column);
    }

public static String captureSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        if (store == null) return "READ_ERROR:STORE_NULL";
        Cursor c = null;
        com.ethk3.novaonepro.ReportTextCP12 out = new com.ethk3.novaonepro.ReportTextCP12(report, "R925活跃算法接入缺口（完整资产另见全量清单）\n");
        try {
            c = report.select("SELECT display_name_zh,future_n,future_hit_rate,"
                    + "future_wilson_lower,future_brier,future_ece,future_coverage,future_block_min,"
                    + "future_regime_min,future_max_miss,paired_mainline_n,paired_mainline_delta,"
                    + "future_fdr_q,status,lifecycle_reason,genome_id FROM " + DbHelper.TABLE_R908_ALGORITHM_GENOME
                    + " WHERE status IN ('IDEA_WAIT_FUTURE_OOS','OBSERVING_FUTURE_OOS','CHALLENGER_FUTURE_OOS')"
                    + " ORDER BY future_wilson_lower DESC,genome_id ASC", null);
            while (c.moveToNext()) {
                out.append(c.getString(0)).append("｜状态=").append(c.getString(13))
                        .append("｜生命周期=").append(c.getString(14)).append("｜缺口=")
                        .append(missingSnapshotCP4(report, c.getInt(1), valueSnapshotCP4(report, c,2), valueSnapshotCP4(report, c,3), valueSnapshotCP4(report, c,4), valueSnapshotCP4(report, c,5),
                                valueSnapshotCP4(report, c,6), valueSnapshotCP4(report, c,7), valueSnapshotCP4(report, c,8), c.getInt(9), c.getInt(10), valueSnapshotCP4(report, c,11), valueSnapshotCP4(report, c,12)))
                        .append('\n');
            }
            out.append("有效评估覆盖率用于判断证据，不是每期出手配额；本说明不能授予REAL权限。\n")
                    .append("轮转游标：捕获=").append(store.getState("r924_predraw_next_lane", "0"))
                    .append("；学习=").append(store.getState("r924_continuity_next_lane", "0"));
        } catch (Throwable failure) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",failure);}
        finally { if (c != null) c.close(); }
        return out.toString();
    }

public static double valueSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, Cursor c, int column) {
        return c.isNull(column) ? Double.NaN : c.getDouble(column);
    }

public static String missingSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, int n, double rate, double wilson, double brier, double ece,
            double coverage, double block, double regime, int miss, int paired, double delta, double q) {
        com.ethk3.novaonepro.ReportTextCP12 b = new com.ethk3.novaonepro.ReportTextCP12(report);
        needSnapshotCP4(report, b, n >= CertificateBundleServiceR929.MIN_INFLUENCE_FUTURE_N,
                "未来样本", n, CertificateBundleServiceR929.MIN_INFLUENCE_FUTURE_N);
        needSnapshotCP4(report, b, rate >= CertificateBundleServiceR929.MIN_ENTRY_HIT_RATE,
                "命中率（含边界）", rate, CertificateBundleServiceR929.MIN_ENTRY_HIT_RATE);
        needSnapshotCP4(report, b, wilson >= CertificateBundleServiceR929.MIN_WILSON_LOWER,
                "Wilson下界（含边界）", wilson, CertificateBundleServiceR929.MIN_WILSON_LOWER);
        needSnapshotCP4(report, b, brier >= 0 && brier <= CertificateBundleServiceR929.MAX_ENTRY_BRIER,
                "Brier上限", brier, CertificateBundleServiceR929.MAX_ENTRY_BRIER);
        needSnapshotCP4(report, b, ece >= 0 && ece <= CertificateBundleServiceR929.MAX_ENTRY_ECE,
                "ECE上限", ece, CertificateBundleServiceR929.MAX_ENTRY_ECE);
        needSnapshotCP4(report, b, coverage >= CertificateBundleServiceR929.MIN_EVIDENCE_COVERAGE,
                "有效评估覆盖率（不是出手限额）", coverage, CertificateBundleServiceR929.MIN_EVIDENCE_COVERAGE);
        needSnapshotCP4(report, b, paired >= CertificateBundleServiceR929.MIN_INFLUENCE_PAIRED_N,
                "独立配对样本", paired, CertificateBundleServiceR929.MIN_INFLUENCE_PAIRED_N);
        needSnapshotCP4(report, b, (!Double.isNaN(delta) && !Double.isInfinite(delta)) && delta > 0, "配对提升需大于", delta, 0);
        // Legacy block/regime/miss/q columns are not interchangeable with the current formal
        // endpoint, independent-window and adjusted-p ledger. Do not manufacture those gates.
        return (b.length() == 0 ? "所列汇总数值达标（不是证书）" : b.toString())
                + "；仍需至少2个独立窗口、当前FWER调整p≤0.05、有效域/资源及PIT程序身份校验"
                + "；旧future_fdr_q不能直接冒充FWER证据";
    }

public static void needSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportTextCP12 out, boolean pass, String name, double actual, double threshold) {
        if (pass) return;
        if (out.length() > 0) out.append('；');
        out.append(name).append('=').append(actual).append(" / 要求=").append(threshold);
    }
}
