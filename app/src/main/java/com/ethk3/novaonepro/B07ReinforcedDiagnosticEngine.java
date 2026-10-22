package com.ethk3.novaonepro;

import android.content.Context;
import android.content.SharedPreferences;

import java.lang.reflect.Method;
import java.util.Locale;

/** B01-B07 system-level diagnosis only. It does not implement B08 teacher/causal expansion. */
public final class B07ReinforcedDiagnosticEngine {
    private B07ReinforcedDiagnosticEngine() {}

    public static String build(Context context, DbStore store) {
        StringBuilder s = new StringBuilder(32768);
        long now = System.currentTimeMillis();
        s.append("【星核天枢 1-7批次加强版完整动态检测报告】\n");
        s.append("版本=").append(AppConfig.B07R_VERSION).append('\n');
        s.append("执行模式=")
                .append(AppConfig.B07R_EXECUTION_MODE)
                .append("；无人值守资金执行=")
                .append(AppConfig.B07R_UNATTENDED_MONEY_EXECUTION)
                .append('\n');
        s.append("生成时间ms=").append(now).append("；包=").append(AppConfig.PACKAGE_NAME).append('\n');
        s.append("原则：观测值与目标值严格分开；没有严格PIT/OOS证据时绝不把39/40/50/60%写成已达标。\n\n");

        section(s, "一、B01-B07施工完整性");
        batch(
                s,
                "B01 身份/接口/原始入口",
                present("com.ethk3.novaonepro.ApiClient")
                        && present("com.ethk3.novaonepro.ApiParser")
                        && present("com.ethk3.novaonepro.BuildIdentity"));
        batch(
                s,
                "B02 开奖完整性/目标时钟/统一模型",
                present("com.ethk3.novaonepro.CountdownClock")
                        && present("com.ethk3.novaonepro.Models"));
        batch(
                s,
                "B03 SQLite/迁移/唯一仓储",
                present("com.ethk3.novaonepro.DbStore")
                        && present("com.ethk3.novaonepro.DbHelper"));
        batch(
                s,
                "B04 PIT冻结/滚动缓存/基础特征",
                present("com.ethk3.novaonepro.PreDrawFeatureEngine")
                        || present("com.ethk3.novaonepro.WindowFeatureEngine"));
        batch(
                s,
                "B05 状态/窗口/趋势/周期/漂移",
                present("com.ethk3.novaonepro.TrendStateMachine")
                        && present("com.ethk3.novaonepro.BreakRecoveryEngine"));
        batch(
                s,
                "B06 形态/关系/记忆/相似窗口",
                present("com.ethk3.novaonepro.ShapeStateEngine")
                        && present("com.ethk3.novaonepro.SimilarShapeEngine")
                        && present("com.ethk3.novaonepro.ShapeMemoryEngine")
                        && present("com.ethk3.novaonepro.AttributionEngine"));
        batch(
                s,
                "B07 Evidence合同/注册/聚合/健康",
                present("com.ethk3.novaonepro.UnifiedStrategyEvidenceEngine")
                        && present("com.ethk3.novaonepro.StrategyModuleCatalog"));
        s.append("B08=NOT_STARTED_BY_CONTRACT（本加强版明确不进入第8批次）\n");

        section(s, "二、唯一主链与冲突审计");
        s.append("开奖入口=ApiClient/ApiParser -> DataGuard -> DbStore\n");
        s.append(
                "实时链=冻结时点数据 -> 基础特征 -> 状态/趋势/形态/关系/相似/记忆 -> Evidence聚合 -> Probability/Calibration"
                    + " -> DynamicGate -> DecisionGateEngine\n");
        s.append("动作所有者=")
                .append(AppConfig.B07R_REAL_ACTION_OWNER)
                .append("；单位置K3=")
                .append(AppConfig.FIXED_K == 3)
                .append('\n');
        s.append("禁止K4=")
                .append(!AppConfig.ALLOW_K4)
                .append("；禁止动态K=")
                .append(!AppConfig.ALLOW_DYNAMIC_K)
                .append("；禁止多位置真实动作=")
                .append(!AppConfig.ALLOW_MULTI_POSITION_BET)
                .append('\n');
        rawState(s, store, "v44_pipeline_invariant_detail");
        rawState(s, store, "v44_decision_funnel_detail");
        rawState(s, store, "module_effect_summary");
        rawState(s, store, "dynamic_freeze_summary");

        section(s, "三、后台常驻/退出界面/息屏健康");
        s.append("要求=退出UI后继续抓取、分析、预测、结算与学习；明确用户点击停止才停止。\n");
        s.append("B07R_BACKGROUND_CONTINUE_AFTER_UI_EXIT=")
                .append(AppConfig.B07R_BACKGROUND_CONTINUE_AFTER_UI_EXIT)
                .append('\n');
        appendHeartbeatSnapshot(s, context, now);
        rawState(s, store, "last_service_started_at");
        rawState(s, store, "last_service_command_at");
        rawState(s, store, "last_task_removed_at");
        rawState(s, store, "last_prediction_success_at");
        rawState(s, store, "last_prediction_issue");
        rawState(s, store, "last_analysis_success_at");
        rawState(s, store, "last_api_success_at");
        rawState(s, store, "last_settlement_at");

        section(s, "四、闪退/OOM/线程/数据库健康");
        appendCrashSnapshot(s, context, now);
        rawState(s, store, "database_health_summary");
        rawState(s, store, "db_queue_state");
        rawState(s, store, "last_error");
        rawState(s, store, "analysis_runtime_state");
        Runtime rt = Runtime.getRuntime();
        long used = rt.totalMemory() - rt.freeMemory();
        s.append("JVM内存 used/total/max=")
                .append(used)
                .append('/')
                .append(rt.totalMemory())
                .append('/')
                .append(rt.maxMemory())
                .append('\n');
        s.append("OOM策略=串行重试+")
                .append(AppConfig.B07_OOM_MAX_SERIAL_RETRY)
                .append("次上限；后台重任务不得与实时链抢占。\n");

        section(s, "五、实时出手/影子/结算统计真实性");
        s.append("LIVE_SIGNAL_ENABLED=")
                .append(AppConfig.B07R_LIVE_SIGNAL_ENABLED)
                .append("；人工确认模式，禁止未证明模型无人值守资金执行。\n");
        rawState(s, store, "real_bet_diagnosis_summary");
        rawState(s, store, "real_bet_diagnosis_detail");
        rawState(s, store, "shadow_promotion_summary");
        rawState(s, store, "shadow_promotion_detail");
        s.append("真实统计口径=REAL_TRADE/实时动作 + 已结算WIN/MISS；SHADOW/HOLD不得污染真实胜率/ECE/盈亏。\n");

        section(s, "六、胜率/频率/校准是否达标");
        rawState(s, store, "diagnosis_closed_loop_summary");
        rawState(s, store, "adaptive_judge_summary");
        rawState(s, store, "calibration_summary");
        rawState(s, store, "coverage_frontier_summary");
        s.append("证明门：至少严格时序OOS + 足够已结算样本 + Wilson95%下界 + ECE<=4% + 多时间块一致 + 无未来泄漏。\n");
        s.append("39/40/50/60判断必须读取观测证据；代码存在不等于胜率达标。\n");

        section(s, "七、为什么可能不达标——动态根因树");
        s.append("A 运行层：服务死亡/心跳假活/API失败/分析线程卡死/OOM -> 先修运行，不评价策略。\n");
        s.append("B 数据层：缺期/乱序/目标期错位/未来泄漏/脏数据 -> PIT证据失效。\n");
        s.append("C 主链层：多概率所有者/旁路BET/重复计权/SHADOW污染 -> 统计失真或策略冲突。\n");
        s.append("D 模型层：号码排序弱、第三号拖累、相似窗口过拟合、状态切换滞后、校准偏高 -> 命中率不足。\n");
        s.append("E 频率层：严格证据域过窄 -> 频率低；禁止靠降低安全门或频率债务硬凑。\n");
        s.append("F 证据层：样本太少/四块分裂/漂移高 -> 不能宣称40/50/60。\n");

        section(s, "八、策略/算法/公式的具体加强方向");
        s.append(
                "1) 模块可靠度权重：w_i=max(0,WilsonLower_i-0.30)*stability_i*freshness_i*(1-corrPenalty_i)。\n");
        s.append("2) 低相关融合：score=sum(w_i*logit(p_i))/sum(abs(w_i))，防止多个相似模块重复放大同一证据。\n");
        s.append("3) 校准：分状态/位置/证据域做PIT isotonic或分桶可靠度；同时报告Brier/ECE，不允许小样本抬概率。\n");
        s.append("4) 漂移：JS/PSI + CUSUM/BOCPD；漂移升高时缩权而不是硬冻结全部频率。\n");
        s.append("5) 消融：逐个关闭位置/号码/第三号/形态/关系/相似/记忆/校准，比较严格OOS增益与跨块稳定性；负贡献模块冻结。\n");
        s.append("6) 频率：扩大被OOS证明的状态原型和互补专家覆盖；目标先15-20%，再验证20-30%，不为频率放宽真实质量门。\n");
        s.append("7) 50/60%：只有在新信息源或显著互补特征带来可重复信息增益，并使Wilson下界在多块OOS仍过线时才可升级；仅加算法数量不能证明。\n");

        section(s, "九、建议新增/修改的具体模块（仅在证据触发时）");
        s.append("若相关性过高：OrthogonalEvidenceWeightEngine（相关矩阵+去重权重）。\n");
        s.append("若状态漂移：RegimeDriftCertificateEngine（JS/PSI/CUSUM/BOCPD域证书）。\n");
        s.append("若第三号拖累：ThirdNumberConditionalRankEngine（条件排名+第三号专属校准）。\n");
        s.append("若频率不足但高质量域稳定：CertifiedCoverageExpansionEngine（只扩已证明原型，不降低门槛）。\n");
        s.append("若概率虚高：HierarchicalCalibrationEngine（位置/状态/样本量分层收缩）。\n");
        s.append("若模型冲突：ExpertCorrelationAuditEngine + AblationLedger（跨块增益/负增益/重复证据审计）。\n");

        section(s, "十、当前结论生成规则");
        String merged =
                (state(store, "diagnosis_closed_loop_summary")
                                + ' '
                                + state(store, "real_bet_diagnosis_summary")
                                + ' '
                                + state(store, "adaptive_judge_summary"))
                        .toUpperCase(Locale.US);
        if (merged.contains("OOM") || merged.contains("CRASH") || merged.contains("RUNTIME_FAILED"))
            s.append("当前优先级=P0运行稳定性；胜率结论暂停，先消除闪退/后台中断。\n");
        else if (merged.contains("NOT_PROVEN")
                || merged.contains("NO_SAMPLE")
                || merged.length() < 20)
            s.append("当前证据=NOT_PROVEN/不足；不得把目标胜率当成实测胜率。先累计严格实时结算与影子反事实。\n");
        else s.append("当前证据存在；请以上述原始诊断字段的OOS/Wilson/ECE/频率/分块结果为准。\n");
        s.append("本报告只覆盖B01-B07返工，不声明B08已完成。\n");
        return trim(s.toString(), AppConfig.B07R_REPORT_MAX_CHARS);
    }

    private static void section(StringBuilder s, String x) {
        s.append("\n【").append(x).append("】\n");
    }

    private static void batch(StringBuilder s, String n, boolean ok) {
        s.append(n)
                .append('=')
                .append(ok ? "IMPLEMENTED_CLASS_SET_PRESENT" : "PARTIAL_OR_MISSING")
                .append('\n');
    }

    private static boolean present(String n) {
        try {
            Class.forName(n);
            return true;
        } catch (Throwable e) {
            return false;
        }
    }

    private static String state(DbStore store, String key) {
        if (store == null) return "";
        try {
            Method m = store.getClass().getMethod("getState", String.class);
            Object v = m.invoke(store, key);
            return v == null ? "" : String.valueOf(v);
        } catch (Throwable e) {
            return "";
        }
    }

    private static void rawState(StringBuilder s, DbStore d, String k) {
        String v = state(d, k);
        s.append(k).append('=').append(v.length() == 0 ? "NO_DATA" : trim(v, 12000)).append('\n');
    }

    private static String trim(String x, int n) {
        if (x == null) return "";
        return x.length() <= n ? x : x.substring(0, n) + "\n[TRUNCATED_RAW_PAYLOAD]";
    }

    private static void appendHeartbeatSnapshot(StringBuilder s, Context c, long now) {
        try {
            ServiceHeartbeatStore h = new ServiceHeartbeatStore(c.getApplicationContext());
            long age = h.getHeartbeatAgeMs();
            s.append("serviceHeartbeatAgeMs=")
                    .append(age)
                    .append(';')
                    .append("expired=")
                    .append(h.isHeartbeatExpired())
                    .append(';')
                    .append("businessStale=")
                    .append(h.isBusinessStale())
                    .append(';')
                    .append("userEnabled=")
                    .append(h.isUserEnabled())
                    .append('\n');
            s.append("后台判定=")
                    .append(
                            age <= AppConfig.B07R_BUSINESS_STALE_MS && !h.isBusinessStale()
                                    ? "ALIVE"
                                    : "STALE_OR_BROKEN")
                    .append('\n');
        } catch (Throwable e) {
            s.append("heartbeatSnapshot=ERROR:").append(e.getClass().getSimpleName()).append('\n');
        }
    }

    private static void appendCrashSnapshot(StringBuilder s, Context c, long now) {
        try {
            SharedPreferences p =
                    c.getApplicationContext()
                            .getSharedPreferences("v20_crash_snapshot", Context.MODE_PRIVATE);
            long at = p.getLong("at", 0L);
            s.append("crashCount=")
                    .append(p.getInt("crash_count", 0))
                    .append("; oomCount=")
                    .append(p.getInt("oom_count", 0))
                    .append("; soeCount=")
                    .append(p.getInt("soe_count", 0))
                    .append('\n');
            s.append("lastCrashAgeMs=")
                    .append(at <= 0 ? -1 : Math.max(0, now - at))
                    .append("; type=")
                    .append(p.getString("type", ""))
                    .append("; stage=")
                    .append(p.getString("stage", ""))
                    .append("; issue=")
                    .append(p.getString("target_issue", ""))
                    .append('\n');
            s.append("lastCrashMessage=")
                    .append(trim(p.getString("message", ""), 1000))
                    .append('\n');
            s.append("lastCrashStack=").append(trim(p.getString("stack", ""), 8000)).append('\n');
        } catch (Throwable e) {
            s.append("crashSnapshot=ERROR:").append(e.getClass().getSimpleName()).append('\n');
        }
    }
}
