package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

/** Read-only inventory of every status. Resource limits never truncate the complete report. */
public final class AlgorithmLifecycleReportR927 {
    private AlgorithmLifecycleReportR927() {}

    public static String capture(DbStore store, boolean full) {
        StringBuilder out = new StringBuilder(16384);
        out.append("R932_ALGORITHM_LIFECYCLE_REPORT\n")
                .append("三个研究消费者共用状态账本；全局名额A 8、B 128、C 64；单研究线程分片续跑，达到容量时优先训练已有对象，容量让步不作质量淘汰。\n")
                .append("新120期仅L5影子；再新240期累计360后5%，后续独立窗口累计600/840/1200对应12%/20%/30%。\n")
                .append("所有权重都须通过统一证书实测门；代理样本、开发回放和旧累计分数不作晋级证据。\n")
                .append("原有定义、父代、历史指标与首次原因完整保留。旧统计在历史清单展示，新状态和证据以下述统一账本为准。\n");
        if (store == null) return out.append("STATUS=READ_ERROR|STORE_NULL\n").toString();
        try {
            SQLiteDatabase db = store.readable();
            counts(out, db, DbHelper.TABLE_R908_ALGORITHM_GENOME, "status", "创造算法");
            counts(out, db, AssetGenesisEngineR915.TABLE_ASSET_GENOME, "status", "创生资产");
            counts(out, db, DbHelper.TABLE_R906_RESEARCH_ASSET, "tier", "研究方法");
            out.append("上述为三个独立登记体系，不能相加冒充独立算法总数。\n");
            try {out.append(ResearchLifecycleR932.report(store));} catch(Exception unavailable){out.append("统一生命周期尚未建立或读取失败。\n");}
            if (full) {
                allAlgorithms(out, db);
                allResearchMethods(out, db);
                out.append("\n原始淘汰原因与当时证据：\n").append(LifecycleAuditR925.report(store));
            } else out.append("详细清单在完整检测报告的本节，包含所有状态，不限前20条。\n");
        } catch (Exception error) {
            out.append("STATUS=READ_ERROR|").append(error.getClass().getSimpleName()).append('\n');
        }
        return out.toString();
    }

    private static void counts(StringBuilder out, SQLiteDatabase db, String table,
            String statusColumn, String label) {
        Cursor c = db.rawQuery("SELECT " + statusColumn + ",COUNT(*) FROM " + table
                + " GROUP BY " + statusColumn + " ORDER BY " + statusColumn, null);
        long total = 0;
        out.append('\n').append(label).append("状态分布：\n");
        try {
            while (c.moveToNext()) {
                total += c.getLong(1);
                out.append(stateZh(c.getString(0))).append(" [").append(c.getString(0))
                        .append("]=").append(c.getLong(1)).append('\n');
            }
        } finally { c.close(); }
        out.append(label).append("登记总数=").append(total).append('\n');
    }

    static String stateZh(String status) {
        if (status == null) return "状态缺失";
        if (status.startsWith("PRUNED_RESOURCE") || "RESOURCE_RETIRED".equals(status)) return "资源名额退出，证据保留（不代表质量失败）";
        if (status.startsWith("PRUNED")) return "质量或契约淘汰，证据保留";
        if (status.startsWith("PAUSED")) return "容量暂停，等待重新评估与调度";
        if (status.startsWith("REJECTED")) return "准入拒绝，记录保留";
        if (status.startsWith("REVOKED")) return "资格已撤销，证据保留";
        if (status.startsWith("CHALLENGER")) return "挑战者，仍需生产准入检查";
        if (status.startsWith("CERTIFIED")) return "证书候选，是否生效以主链审计为准";
        if (status.startsWith("OBSERVING") || "NURSERY".equals(status)) return "观察中";
        if (status.startsWith("IDEA")) return "已登记，等待未来样本";
        if ("ARCHIVED".equals(status)) return "研究方法归档，定义和证据保留";
        if ("ACTIVE".equals(status)) return "活跃研究方法";
        return "其他状态（保留原始代码）";
    }

    private static void allAlgorithms(Appendable sink, SQLiteDatabase db) throws java.io.IOException {
        // No status predicate, score selection or LIMIT: high-score archived rows and paused rows
        // are as visible as active rows. One cursor defines a consistent count/detail snapshot.
        Cursor c = null;
        long count = 0;
        StringBuilder out = new StringBuilder(8192);
        out.append("\n【全部创造算法中文清单｜全状态｜无数量截断】\n");
        try {
            c = db.rawQuery("SELECT * FROM " + DbHelper.TABLE_R908_ALGORITHM_GENOME
                    + " ORDER BY genome_id", null);
            while (c.moveToNext()) {
                sink.append(out); out.setLength(0);
                ++count;
                out.append('\n').append(count).append(". ").append(value(c, "display_name_zh"))
                        .append("｜ID=").append(value(c, "genome_id"))
                        .append("\n状态=").append(stateZh(value(c, "status")))
                        .append("｜原始状态=").append(value(c, "status"))
                        .append("\n当前原因=").append(value(c, "lifecycle_reason"))
                        .append("\n未来样本/命中=").append(value(c, "future_n")).append('/')
                        .append(value(c, "future_hit"))
                        .append("｜命中率/置信下界=").append(value(c, "future_hit_rate")).append('/')
                        .append(value(c, "future_wilson_lower"))
                        .append("\nBrier/ECE=").append(value(c, "future_brier")).append('/')
                        .append(value(c, "future_ece"))
                        .append("｜分块/场景最低=").append(value(c, "future_block_min")).append('/')
                        .append(value(c, "future_regime_min"))
                        .append("｜最长连挂=").append(value(c, "future_max_miss"))
                        .append("\n覆盖=").append(value(c, "future_coverage"))
                        .append("｜配对样本/相对增益=").append(value(c, "paired_mainline_n")).append('/')
                        .append(value(c, "paired_mainline_delta"))
                        .append("｜多重检验=").append(value(c, "future_fdr_q"))
                        .append("\n开发回放样本/命中率（不作未来证明）=").append(value(c, "dev_n")).append('/')
                        .append(value(c, "dev_hit_rate"))
                        .append("\n代数=").append(value(c, "generation"))
                        .append("｜父代=").append(value(c, "parent_ids"))
                        .append("｜结构意图=").append(value(c, "mutation_intent"))
                        .append("\n创造原因=").append(value(c, "creation_reason"))
                        .append("\n中文机制=").append(value(c, "mechanism_spec_zh"))
                        .append("\n完整算法定义=").append(value(c, "genome_spec"))
                        .append("\n训练截止/最早使用/未来证据截止=").append(value(c, "trained_until_issue"))
                        .append('/').append(value(c, "earliest_use_issue")).append('/')
                        .append(value(c, "future_last_issue"))
                        .append("\n结构/行为/未来证据SHA256=").append(value(c, "structural_hash")).append('/')
                        .append(value(c, "behavior_hash")).append('/').append(value(c, "future_evidence_hash"))
                        .append("\n生产契约=").append(value(c, "research_genome_contract"))
                        .append("\n最近更新时间=").append(value(c, "updated_at")).append('\n');
            }
            out.append("END_ALL_ALGORITHMS|rows=").append(count).append("|status=COMPLETE\n");
        } catch (java.io.IOException error) { throw error;
        } catch (Exception error) {
            out.append("END_ALL_ALGORITHMS|rowsRead=").append(count).append("|status=READ_ERROR|")
                    .append(error.getClass().getSimpleName()).append('|')
                    .append(FullResearchInventoryR923.escape(String.valueOf(error.getMessage()))).append('\n');
        } finally { if (c != null) c.close(); }
        out.append("率与分数采用0到1原值，例如0.45=45%；无样本不能解释为已证明高胜率。\n");
        sink.append(out);
    }

    public static void writeDetails(DbStore store, Appendable out) throws java.io.IOException {
        allAlgorithms(out, store.readable());
        allResearchMethods(out, store.readable());
    }

    private static void allResearchMethods(Appendable out, SQLiteDatabase db) throws java.io.IOException {
        out.append("\n【全部研究方法｜含归档｜无数量截断】\n");
        Cursor c = null;
        long rows = 0;
        try {
            c = db.rawQuery("SELECT a.*,e.frozen_n,e.settled_n,e.hit_rate,e.interval_lower,"
                    + "e.brier,e.ece,e.block_stability,e.drift_score FROM " + DbHelper.TABLE_R906_RESEARCH_ASSET
                    + " a LEFT JOIN " + DbHelper.TABLE_R906_RESEARCH_EVAL_COVERAGE
                    + " e ON e.asset_id=a.asset_id ORDER BY a.asset_id", null);
            while(c.moveToNext()) {
                out.append("\n名称=").append(value(c,"display_name_zh"))
                    .append("｜ID=").append(value(c,"asset_id"))
                    .append("\n状态=").append(stateZh(value(c,"tier")))
                    .append("｜原始状态=").append(value(c,"tier"))
                    .append("\n归档/晋升原因=").append(value(c,"last_transition_code"))
                    .append("\n冻结/结算样本=").append(value(c,"frozen_n")).append("/").append(value(c,"settled_n"))
                    .append("｜命中率/下界=").append(value(c,"hit_rate")).append("/").append(value(c,"interval_lower"))
                    .append("\nBrier/ECE/稳定性/漂移=").append(value(c,"brier")).append("/").append(value(c,"ece"))
                    .append("/").append(value(c,"block_stability")).append("/").append(value(c,"drift_score"))
                    .append("\n代数/父代=").append(value(c,"generation")).append("/").append(value(c,"parent_asset_ids"))
                    .append("\n完整定义=").append(value(c,"canonical_spec"))
                    .append("\n训练截止/最早使用=").append(value(c,"trained_until_issue")).append("/").append(value(c,"earliest_use_issue"))
                    .append("\n执行资格/产物状态=").append(value(c,"runtime_executable")).append("/").append(value(c,"artifact_status"))
                    .append("\n");
                rows++;
            }
            out.append("END_ALL_RESEARCH_METHODS|rows=").append(String.valueOf(rows)).append("|status=COMPLETE\n");
        } catch (java.io.IOException error) { throw error;
        } catch (Exception error) {
            out.append("END_ALL_RESEARCH_METHODS|rowsRead=").append(String.valueOf(rows))
                    .append("|status=READ_ERROR|").append(error.getClass().getSimpleName()).append('|')
                    .append(FullResearchInventoryR923.escape(String.valueOf(error.getMessage()))).append('\n');
        } finally { if (c != null) c.close(); }
    }

    private static String value(Cursor c, String column) {
        int i = c.getColumnIndex(column);
        return i < 0 ? "字段未提供:" + column
                : c.isNull(i) ? "未知" : FullResearchInventoryR923.escape(c.getString(i));
    }

public static void allResearchMethodsSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, Appendable out, SQLiteDatabase db) throws java.io.IOException {
        out.append("\n【全部研究方法｜含归档｜无数量截断】\n");
        Cursor c = null;
        long rows = 0;
        try {
            c = report.select("SELECT "+ReportBoundedSqlCP12.projection(report,DbHelper.TABLE_R906_RESEARCH_ASSET,"a")+",e.frozen_n,e.settled_n,e.hit_rate,e.interval_lower,"
                    + "e.brier,e.ece,e.block_stability,e.drift_score FROM " + DbHelper.TABLE_R906_RESEARCH_ASSET
                    + " a LEFT JOIN " + DbHelper.TABLE_R906_RESEARCH_EVAL_COVERAGE
                    + " e ON e.asset_id=a.asset_id ORDER BY a.asset_id", null);
            while(c.moveToNext()) {
                out.append("\n名称=").append(valueSnapshotCP4(report, c,"display_name_zh"))
                    .append("｜ID=").append(valueSnapshotCP4(report, c,"asset_id"))
                    .append("\n状态=").append(stateZhSnapshotCP4(report, valueSnapshotCP4(report, c,"tier")))
                    .append("｜原始状态=").append(valueSnapshotCP4(report, c,"tier"))
                    .append("\n归档/晋升原因=").append(valueSnapshotCP4(report, c,"last_transition_code"))
                    .append("\n冻结/结算样本=").append(valueSnapshotCP4(report, c,"frozen_n")).append("/").append(valueSnapshotCP4(report, c,"settled_n"))
                    .append("｜命中率/下界=").append(valueSnapshotCP4(report, c,"hit_rate")).append("/").append(valueSnapshotCP4(report, c,"interval_lower"))
                    .append("\nBrier/ECE/稳定性/漂移=").append(valueSnapshotCP4(report, c,"brier")).append("/").append(valueSnapshotCP4(report, c,"ece"))
                    .append("/").append(valueSnapshotCP4(report, c,"block_stability")).append("/").append(valueSnapshotCP4(report, c,"drift_score"))
                    .append("\n代数/父代=").append(valueSnapshotCP4(report, c,"generation")).append("/").append(valueSnapshotCP4(report, c,"parent_asset_ids"))
                    .append("\n完整定义=").append(valueSnapshotCP4(report, c,"canonical_spec"))
                    .append("\n训练截止/最早使用=").append(valueSnapshotCP4(report, c,"trained_until_issue")).append("/").append(valueSnapshotCP4(report, c,"earliest_use_issue"))
                    .append("\n执行资格/产物状态=").append(valueSnapshotCP4(report, c,"runtime_executable")).append("/").append(valueSnapshotCP4(report, c,"artifact_status"))
                    .append("\n");
                rows++;
            }
            out.append("END_ALL_RESEARCH_METHODS|rows=").append(String.valueOf(rows)).append("|status=COMPLETE\n");
        } catch (java.io.IOException error) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",error);} catch (Exception error) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",error);} finally { if (c != null) c.close(); }
    }

public static String valueSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, Cursor c, String column) {
        int i = c.getColumnIndex(column);
        return i < 0 ? "字段未提供:" + column
                : c.isNull(i) ? "未知" : FullResearchInventoryR923.escape(c.getString(i));
    }

static String stateZhSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, String status) {
        if (status == null) return "状态缺失";
        if (status.startsWith("PRUNED_RESOURCE") || "RESOURCE_RETIRED".equals(status)) return "资源名额退出，证据保留（不代表质量失败）";
        if (status.startsWith("PRUNED")) return "质量或契约淘汰，证据保留";
        if (status.startsWith("PAUSED")) return "容量暂停，等待重新评估与调度";
        if (status.startsWith("REJECTED")) return "准入拒绝，记录保留";
        if (status.startsWith("REVOKED")) return "资格已撤销，证据保留";
        if (status.startsWith("CHALLENGER")) return "挑战者，仍需生产准入检查";
        if (status.startsWith("CERTIFIED")) return "证书候选，是否生效以主链审计为准";
        if (status.startsWith("OBSERVING") || "NURSERY".equals(status)) return "观察中";
        if (status.startsWith("IDEA")) return "已登记，等待未来样本";
        if ("ARCHIVED".equals(status)) return "研究方法归档，定义和证据保留";
        if ("ACTIVE".equals(status)) return "活跃研究方法";
        return "其他状态（保留原始代码）";
    }

public static void allAlgorithmsSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, Appendable sink, SQLiteDatabase db) throws java.io.IOException {
        // No status predicate, score selection or LIMIT: high-score archived rows and paused rows
        // are as visible as active rows. One cursor defines a consistent count/detail snapshot.
        Cursor c = null;
        long count = 0;
        com.ethk3.novaonepro.ReportTextCP12 out = new com.ethk3.novaonepro.ReportTextCP12(report, 8192);
        out.append("\n【全部创造算法中文清单｜全状态｜无数量截断】\n");
        try {
            c = report.select("SELECT "+ReportBoundedSqlCP12.projection(report,DbHelper.TABLE_R908_ALGORITHM_GENOME,"a")+" FROM " + DbHelper.TABLE_R908_ALGORITHM_GENOME
                    + " a ORDER BY genome_id", null);
            while (c.moveToNext()) {
                sink.append(out.toString()); out = new com.ethk3.novaonepro.ReportTextCP12(report,8192);
                ++count;
                out.append('\n').append(count).append(". ").append(valueSnapshotCP4(report, c, "display_name_zh"))
                        .append("｜ID=").append(valueSnapshotCP4(report, c, "genome_id"))
                        .append("\n状态=").append(stateZhSnapshotCP4(report, valueSnapshotCP4(report, c, "status")))
                        .append("｜原始状态=").append(valueSnapshotCP4(report, c, "status"))
                        .append("\n当前原因=").append(valueSnapshotCP4(report, c, "lifecycle_reason"))
                        .append("\n未来样本/命中=").append(valueSnapshotCP4(report, c, "future_n")).append('/')
                        .append(valueSnapshotCP4(report, c, "future_hit"))
                        .append("｜命中率/置信下界=").append(valueSnapshotCP4(report, c, "future_hit_rate")).append('/')
                        .append(valueSnapshotCP4(report, c, "future_wilson_lower"))
                        .append("\nBrier/ECE=").append(valueSnapshotCP4(report, c, "future_brier")).append('/')
                        .append(valueSnapshotCP4(report, c, "future_ece"))
                        .append("｜分块/场景最低=").append(valueSnapshotCP4(report, c, "future_block_min")).append('/')
                        .append(valueSnapshotCP4(report, c, "future_regime_min"))
                        .append("｜最长连挂=").append(valueSnapshotCP4(report, c, "future_max_miss"))
                        .append("\n覆盖=").append(valueSnapshotCP4(report, c, "future_coverage"))
                        .append("｜配对样本/相对增益=").append(valueSnapshotCP4(report, c, "paired_mainline_n")).append('/')
                        .append(valueSnapshotCP4(report, c, "paired_mainline_delta"))
                        .append("｜多重检验=").append(valueSnapshotCP4(report, c, "future_fdr_q"))
                        .append("\n开发回放样本/命中率（不作未来证明）=").append(valueSnapshotCP4(report, c, "dev_n")).append('/')
                        .append(valueSnapshotCP4(report, c, "dev_hit_rate"))
                        .append("\n代数=").append(valueSnapshotCP4(report, c, "generation"))
                        .append("｜父代=").append(valueSnapshotCP4(report, c, "parent_ids"))
                        .append("｜结构意图=").append(valueSnapshotCP4(report, c, "mutation_intent"))
                        .append("\n创造原因=").append(valueSnapshotCP4(report, c, "creation_reason"))
                        .append("\n中文机制=").append(valueSnapshotCP4(report, c, "mechanism_spec_zh"))
                        .append("\n完整算法定义=").append(valueSnapshotCP4(report, c, "genome_spec"))
                        .append("\n训练截止/最早使用/未来证据截止=").append(valueSnapshotCP4(report, c, "trained_until_issue"))
                        .append('/').append(valueSnapshotCP4(report, c, "earliest_use_issue")).append('/')
                        .append(valueSnapshotCP4(report, c, "future_last_issue"))
                        .append("\n结构/行为/未来证据SHA256=").append(valueSnapshotCP4(report, c, "structural_hash")).append('/')
                        .append(valueSnapshotCP4(report, c, "behavior_hash")).append('/').append(valueSnapshotCP4(report, c, "future_evidence_hash"))
                        .append("\n生产契约=").append(valueSnapshotCP4(report, c, "research_genome_contract"))
                        .append("\n最近更新时间=").append(valueSnapshotCP4(report, c, "updated_at")).append('\n');
            }
            out.append("END_ALL_ALGORITHMS|rows=").append(count).append("|status=COMPLETE\n");
        } catch (java.io.IOException error) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",error);} catch (Exception error) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",error);} finally { if (c != null) c.close(); }
        out.append("率与分数采用0到1原值，例如0.45=45%；无样本不能解释为已证明高胜率。\n");
        sink.append(out.toString());
    }

public static void writeDetailsSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, Appendable out) throws java.io.IOException {
        allAlgorithmsSnapshotCP4(report, out, store.readable());
        allResearchMethodsSnapshotCP4(report, out, store.readable());
    }

public static String captureSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, boolean full) {
        com.ethk3.novaonepro.ReportTextCP12 out = new com.ethk3.novaonepro.ReportTextCP12(report, 16384);
        out.append("R932_ALGORITHM_LIFECYCLE_REPORT\n")
                .append("三个研究消费者共用状态账本；全局名额A 8、B 128、C 64；单研究线程分片续跑，达到容量时优先训练已有对象，容量让步不作质量淘汰。\n")
                .append("新120期仅L5影子；再新240期累计360后5%，后续独立窗口累计600/840/1200对应12%/20%/30%。\n")
                .append("所有权重都须通过统一证书实测门；代理样本、开发回放和旧累计分数不作晋级证据。\n")
                .append("原有定义、父代、历史指标与首次原因完整保留。旧统计在历史清单展示，新状态和证据以下述统一账本为准。\n");
        if (store == null) return out.append("STATUS=READ_ERROR|STORE_NULL\n").toString();
        try {
            SQLiteDatabase db = store.readable();
            countsSnapshotCP4(report, out, db, DbHelper.TABLE_R908_ALGORITHM_GENOME, "status", "创造算法");
            countsSnapshotCP4(report, out, db, AssetGenesisEngineR915.TABLE_ASSET_GENOME, "status", "创生资产");
            countsSnapshotCP4(report, out, db, DbHelper.TABLE_R906_RESEARCH_ASSET, "tier", "研究方法");
            out.append("上述为三个独立登记体系，不能相加冒充独立算法总数。\n");
            try {out.append(ResearchLifecycleR932.reportSnapshotCP4(report, store));} catch(Exception unavailable){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",unavailable);}
            if (full) {
                allAlgorithmsSnapshotCP4(report, out, db);
                allResearchMethodsSnapshotCP4(report, out, db);
                out.append("\n原始淘汰原因与当时证据：\n").append(LifecycleAuditR925.reportSnapshotCP4(report, store));
            } else out.append("详细清单在完整检测报告的本节，包含所有状态，不限前20条。\n");
        } catch (Exception error) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",error);}
        return out.toString();
    }

public static void countsSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportTextCP12 out, SQLiteDatabase db, String table,
            String statusColumn, String label) {
        Cursor c = report.select("SELECT " + statusColumn + ",COUNT(*) FROM " + table
                + " GROUP BY " + statusColumn + " ORDER BY " + statusColumn, null);
        long total = 0;
        out.append('\n').append(label).append("状态分布：\n");
        try {
            while (c.moveToNext()) {
                total += c.getLong(1);
                out.append(stateZhSnapshotCP4(report, c.getString(0))).append(" [").append(c.getString(0))
                        .append("]=").append(c.getLong(1)).append('\n');
            }
        } finally { c.close(); }
        out.append(label).append("登记总数=").append(total).append('\n');
    }
}
