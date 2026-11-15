package com.ethk3.novaonepro;

import android.content.Context;
import android.content.SharedPreferences;

import java.lang.reflect.Method;
import java.util.Locale;

/** B01-B07 R2 full-state diagnostic doctor. Read-only. */
public final class B07R2FullDiagnosticEngine {
    private B07R2FullDiagnosticEngine() {}

    public static String build(Context context, DbStore store) {
        StringBuilder s = new StringBuilder(96000);
        long now = System.currentTimeMillis();
        s.append("【星核天枢 B01-B07 最终返修R3.2 · 全状态智能检测报告】\n");
        s.append("版本=")
                .append(AppConfig.B07R32_VERSION)
                .append("；schema=")
                .append(AppConfig.B07R3_DIAG_SCHEMA)
                .append('\n');
        s.append("范围=B01-B07返工；B08=NOT_STARTED_BY_CONTRACT；报告只读，不改策略，不造胜率。\n");
        s.append("游戏=")
                .append(AppConfig.LOTTERY_CODE)
                .append("；固定K=")
                .append(AppConfig.FIXED_K)
                .append("；单位置=")
                .append(!AppConfig.ALLOW_MULTI_POSITION_BET)
                .append("；DecisionGate唯一真实动作Owner=")
                .append(AppConfig.STRICT_DECISION_GATE_ONLY_CAN_BET)
                .append('\n');
        s.append("真实动作策略=")
                .append(AppConfig.B07R2_REAL_ACTION_POLICY)
                .append("；伪造MODEL_PROVEN=")
                .append(!AppConfig.B07R2_NO_FAKE_MODEL_PROOF)
                .append('\n');
        section(s, "1. B01-B07 54文件构建期审计");
        s.append(B07R2BuildAuditManifest.render());
        s.append("说明：文件存在/行数/SHA只证明源码交付，不等于策略效果已证明；效果必须看PIT/OOS和实时结算。\n");
        section(s, "2. 唯一主链/Owner/旁路冲突");
        s.append(
                "合同主链=ApiClient/ApiParser -> DataGuard -> PIT/FeatureGraph ->"
                    + " State/Trend/Pattern/Relation/Memory -> EvidencePacket -> Evidence聚合 ->"
                    + " Probability/Calibration -> DynamicGate -> DecisionGateEngine ->"
                    + " Ledger/Settlement\n");
        s.append("ACTIVE_V51_TIANSHU=")
                .append(AppConfig.ENABLE_V51_TIANSHU)
                .append("；ENABLE_V50_PRIME=")
                .append(AppConfig.ENABLE_V50_PRIME)
                .append("；V50证明门作用域=")
                .append(AppConfig.B07R2_V50_PROOF_SCOPE)
                .append('\n');
        states(
                s,
                store,
                new String[] {
                    "v44_pipeline_invariant_detail",
                    "v44_decision_funnel_detail",
                    "strategy_module_catalog_summary",
                    "strategy_module_catalog_validation",
                    "module_effect_summary",
                    "dynamic_freeze_summary",
                    "b07r2_real_action_owner",
                    "b07r2_real_action_status",
                    "b07r2_real_action_target"
                });
        String chain =
                joinStates(
                                store,
                                new String[] {
                                    "v44_pipeline_invariant_detail",
                                    "v44_decision_funnel_detail",
                                    "strategy_module_catalog_validation",
                                    "module_effect_summary"
                                })
                        .toUpperCase(Locale.US);
        if (containsAny(
                chain,
                new String[] {
                    "DUPLICATE_OWNER", "BYPASS", "CONFLICT", "FUTURE_LEAK", "SHADOW_POLLUTION"
                })) s.append("主链判定=CONFLICT_DETECTED；先解决Owner/未来泄漏/重复计权，再谈胜率。\n");
        else s.append("主链判定=未从现有状态字段发现显式Owner冲突；仍以运行回放Hash和OOS证据为最终验收。\n");
        section(s, "3. 后台常驻/退出软件/息屏/恢复链");
        runtime(s, context, store, now);
        section(s, "4. 闪退/OOM/线程/SQLite健康");
        crash(s, context, store, now);
        states(
                s,
                store,
                new String[] {
                    "database_health_summary",
                    "db_queue_state",
                    "last_error",
                    "analysis_runtime_state",
                    "b07r2_low_memory_at",
                    "b07r2_low_memory_action",
                    "b07r2_trim_memory_level"
                });
        s.append("V51实时预算=soft ")
                .append(AppConfig.V51_REALTIME_SOFT_BUDGET_MS)
                .append("ms / hard ")
                .append(AppConfig.V51_REALTIME_HARD_REDLINE_MS)
                .append("ms；status=")
                .append(state(store, "v51_runtime_budget_status"))
                .append("；elapsedMs=")
                .append(state(store, "v51_runtime_elapsed_ms"))
                .append("；optionalEvidence=")
                .append(state(store, "v51_optional_evidence_status"))
                .append('\n');
        section(s, "5. API/开奖/目标期/数据完整性");
        states(
                s,
                store,
                new String[] {
                    "last_api_success_at",
                    "last_api_error",
                    "last_fetch_issue",
                    "draw_integrity_summary",
                    "history_integrity_summary",
                    "target_issue_status",
                    "last_prediction_issue",
                    "last_prediction_success_at",
                    "last_analysis_success_at",
                    "last_settlement_at"
                });
        section(s, "6. 真实出手/SHADOW/结算口径");
        s.append("真实动作策略=")
                .append(AppConfig.B07R31_REAL_ACTION_POLICY)
                .append("；mayAuthorizeRealBet=")
                .append(V51BuildManifest.mayAuthorizeRealBet())
                .append("；SHADOW_ONLY=")
                .append(V51BuildManifest.SHADOW_ONLY)
                .append("；REAL_BET_HARD_DISABLED=")
                .append(V51BuildManifest.REAL_BET_HARD_DISABLED)
                .append("；MODEL_PROVEN=")
                .append(V51BuildManifest.MODEL_PROVEN)
                .append('\n');
        s.append("说明：MODEL_PROVEN=false仍保持真实性；只有CONTROLLED_PROBE候选通过全部严格门后，DecisionGate才可签BET。\n");
        s.append(
                "V50 typed"
                    + " proof-lock若MODEL_PROVEN=false仍保持SHADOW；因为当前V50_PRIME=false，该锁不应支配V51天枢活动主链。\n");
        states(
                s,
                store,
                new String[] {
                    "real_bet_diagnosis_summary",
                    "real_bet_diagnosis_detail",
                    "shadow_promotion_summary",
                    "shadow_promotion_detail",
                    "decision_authorization_summary",
                    "settlement_diagnosis_summary",
                    "b07r2_real_action_status"
                });
        s.append("真实胜率口径=REAL_TRADE + BET + 已结算WIN/MISS；SHADOW只做反事实，HOLD不计真实盈亏/ECE。\n");
        appendTradeInvariant(s, store);
        section(s, "7. 胜率/频率/ECE/OOS是否达标");
        try {
            V51Repository.DiagnosticStats live = V51Repository.diagnosticStats(store);
            s.append("V51实时不可变账本=").append(live.summary()).append("\n");
            s.append("真实BET胜率=")
                    .append(fmt(live.betHitRate))
                    .append("；SHADOW反事实胜率=")
                    .append(fmt(live.shadowHitRate))
                    .append("；真实BET覆盖=")
                    .append(fmt(live.coverage))
                    .append("；全部选择Wilson下界=")
                    .append(fmt(live.wilsonLower))
                    .append("；ECE=")
                    .append(fmt(live.ece))
                    .append("\n");
        } catch (Throwable e) {
            s.append("V51实时账本统计=ERROR:").append(e.getClass().getSimpleName()).append("\n");
        }
        String evidence =
                joinStates(
                        store,
                        new String[] {
                            "diagnosis_closed_loop_summary",
                            "real_bet_diagnosis_summary",
                            "adaptive_judge_summary",
                            "calibration_summary",
                            "coverage_frontier_summary",
                            "micro_walk_forward_summary",
                            "strict_oos_summary"
                        });
        states(
                s,
                store,
                new String[] {
                    "diagnosis_closed_loop_summary",
                    "adaptive_judge_summary",
                    "calibration_summary",
                    "coverage_frontier_summary",
                    "micro_walk_forward_summary",
                    "strict_oos_summary"
                });
        Metrics m = Metrics.parse(evidence);
        renderMetrics(s, m);
        diagnoseTargets(s, m, evidence);
        section(s, "8. 为什么不达标：动态根因优先级");
        rootCause(s, context, store, now, m, evidence);
        section(s, "9. 策略方法算法公式是否满血/是否冲突");
        s.append("检查维度=信息增益、独立性、校准、漂移鲁棒、第三号贡献、窗口稳定、位置稳定、运行时成本、OOS跨块一致。\n");
        states(
                s,
                store,
                new String[] {
                    "module_effect_summary",
                    "module_score_summary",
                    "ablation_summary",
                    "rule_health_summary",
                    "dynamic_freeze_summary",
                    "teacher_matrix_summary",
                    "counterfactual_summary",
                    "third_guard_summary"
                });
        s.append("核心公式A 模块可靠度：w_i=max(0,LCB_i-0.30)*stability_i*freshness_i*(1-corrPenalty_i)。\n");
        s.append(
                "核心公式B"
                    + " 去相关融合：z=sum(w_i*logit(p_i))/max(eps,sum(abs(w_i)))；p=sigmoid(z)，同源模块相关越高权重越低。\n");
        s.append("核心公式C 校准目标：min Brier + lambda*ECE + gamma*跨时间块方差；小样本用层级收缩，不允许抬高概率。\n");
        s.append("核心公式D 漂移门：risk=max(JS,PSI,CUSUM,BOCPD_changeP)；漂移高时缩权/转SHADOW，而不是频率债务补单。\n");
        section(s, "10. 40/50/60%与频率15%+的具体施工建议");
        strategyPlan(s, m, evidence);
        section(s, "11. 本版修改后的验收结论");
        s.append("B08=未开始。B01-B07源码完整度以第1节54文件审计为准；运行完美度以第3-8节实时证据为准。\n");
        s.append(
                "本报告不会声称50/60%已达标。达到50/60只能由新的可重复信息增益 + 严格PIT/OOS + Wilson下界 +"
                    + " 多块一致性证明，不能靠多写几个算法名。\n");
        s.append("如果运行层STALE/CRASH/OOM，优先P0修运行；如果运行健康但OOS低，才进入策略/特征/Ranker/校准的模型层升级。\n");
        s.append("\n【B07R3 1-7批次加强复核】\n");
        s.append("R3版本=").append(AppConfig.B07R3_VERSION).append("\n");
        s.append("后台能力=").append(AppConfig.B07R3_BACKGROUND_SCOPE).append("\n");
        s.append(
                "退出/划掉任务：前台服务即时重挂 + 同服务foreground re-delivery + Alarm +"
                    + " Job；息屏靠PARTIAL_WAKE_LOCK。用户手动强行停止/厂商禁后台无法由普通应用绕过，必须在报告中如实显示。\n");
        s.append(
                "真实出手修复：全局WalkForward"
                    + " CHAMPION只做证据诊断，不再一票锁死活动V51；候选仍必须通过PredictionContract、RuntimeFreshness、硬风险、EventP、ActionLCB、Coverage、Third、Counter、Uncertainty、Authorization。\n");
        s.append("频率红线：活动V51不允许频率债务制造机会；频率只能通过扩大已证明状态域、改善候选排序/校准/证据覆盖来增加。\n");
        s.append("V51唯一运行链=").append(state(store, "v51_runtime_isolation")).append("\n");
        s.append("V51结算闭环=")
                .append(state(store, "v51_settlement_last"))
                .append("；success=")
                .append(state(store, "v51_settlement_success"))
                .append("\n");
        s.append("B06/B07实时桥=").append(state(store, "v51_b06_b07_bridge")).append("\n");
        s.append("Kernel最后状态=")
                .append(state(store, StarcoreEvidenceKernel.STATE_LAST))
                .append("\n");
        s.append("Kernel回放状态=")
                .append(state(store, StarcoreEvidenceKernel.STATE_REPLAY))
                .append("\n");
        s.append("Kernel插件健康：\n")
                .append(clipWithDigest(StarcoreEvidenceKernel.renderHealth(store), 12000));
        String champion = state(store, WalkForwardChampionEngine.STATE_STATUS);
        if (champion.length() == 0) champion = "MODEL_NOT_PROVEN";
        s.append("全局模型证明=")
                .append(champion)
                .append("；注意：它仍决定能否声称模型已证明，但不再等价于活动V51每个候选永远禁止真实BET。\n");
        s.append(
                "50%/60%目标分析：只有严格时间顺序OOS/真实影子样本的命中率、Wilson下界、ECE、四块稳定性和覆盖同时支持时才可称达标。若未达标，应优先做：层级校准(HierarchicalCalibration)、状态域漂移证书(RegimeDriftCertificate)、第三号条件排序(ThirdNumberConditionalRank)、反事实残差挖掘(RankerResidualMining)、专家相关性去重(ExpertCorrelationAudit)、消融账本(AblationLedger)、已证明域覆盖扩张(CertifiedCoverageExpansion)；禁止简单降低门槛凑50/60或凑频率。\n");
        s.append("Kernel职责边界：只聚合证据/健康/回放；不写FinalP、不选Lane、不创建BET，DecisionGate仍是唯一真实BET出口。\n");
        String report = s.toString();
        return report;
    }

    private static void appendTradeInvariant(StringBuilder s, DbStore store) {
        if (store == null) {
            s.append("真实BET幂等审计=NO_DB\n");
            return;
        }
        android.database.Cursor c = null;
        try {
            c =
                    store.readable()
                            .rawQuery(
                                    "SELECT COUNT(*),COALESCE(MAX(c),0) FROM (SELECT"
                                        + " target_issue,COUNT(*) c FROM "
                                            + DbHelper.TABLE_TRADES
                                            + " WHERE action=? AND stat_scope=? GROUP BY"
                                            + " target_issue HAVING COUNT(*)>1)",
                                    new String[] {
                                        AppConfig.ACTION_BET, AppConfig.STAT_SCOPE_REAL_TRADE
                                    });
            int duplicateIssues = 0, maxPerIssue = 0;
            if (c.moveToFirst()) {
                duplicateIssues = c.getInt(0);
                maxPerIssue = c.getInt(1);
            }
            s.append("每期真实BET幂等审计=duplicateIssues=")
                    .append(duplicateIssues)
                    .append(";maxDuplicateCount=")
                    .append(maxPerIssue)
                    .append(";contractMax=")
                    .append(AppConfig.V50.REAL_TRADE_MAX_PER_ISSUE)
                    .append("；")
                    .append(duplicateIssues == 0 ? "PASS" : "HARD_FAIL_DUPLICATE_REAL_TRADE")
                    .append('\n');
        } catch (Throwable e) {
            s.append("每期真实BET幂等审计=ERROR:").append(e.getClass().getSimpleName()).append('\n');
        } finally {
            if (c != null) c.close();
        }
    }

    private static void runtime(StringBuilder s, Context c, DbStore d, long now) {
        try {
            ServiceHeartbeatStore h = new ServiceHeartbeatStore(c.getApplicationContext());
            long age = h.getHeartbeatAgeMs();
            s.append("heartbeatAgeMs=")
                    .append(age)
                    .append(";expired=")
                    .append(h.isHeartbeatExpired())
                    .append(";businessStale=")
                    .append(h.isBusinessStale())
                    .append(";runtimeLeaseFresh=")
                    .append(h.isRuntimeLeaseFresh())
                    .append(";userEnabled=")
                    .append(h.isUserEnabled())
                    .append('\n');
            boolean healthy =
                    !h.isHeartbeatExpired() && h.isRuntimeLeaseFresh() && !h.isBusinessStale();
            s.append("后台判定=")
                    .append(healthy ? "HEALTHY" : "STALE_OR_RECOVERING")
                    .append("；UI退出后服务应继续；只有显式STOP才允许永久停止。\n");
        } catch (Throwable e) {
            s.append("runtimeSnapshot=ERROR:").append(e.getClass().getSimpleName()).append('\n');
        }
        states(
                s,
                d,
                new String[] {
                    "last_service_started_at",
                    "last_service_command_at",
                    "last_task_removed_at",
                    "b07r2_last_task_removed_rearm_at",
                    "service_foreground_main_loop",
                    "last_prediction_success_at",
                    "last_analysis_success_at",
                    "last_api_success_at"
                });
    }

    private static void crash(StringBuilder s, Context c, DbStore d, long now) {
        try {
            SharedPreferences p =
                    c.getApplicationContext()
                            .getSharedPreferences("v20_crash_snapshot", Context.MODE_PRIVATE);
            long at = p.getLong("at", 0L);
            s.append("crashCount=")
                    .append(p.getInt("crash_count", 0))
                    .append(";window10m=")
                    .append(p.getInt("crash_window_count", 0))
                    .append(";oomCount=")
                    .append(p.getInt("oom_count", 0))
                    .append(";soeCount=")
                    .append(p.getInt("soe_count", 0))
                    .append('\n');
            s.append("lastCrashAgeMs=")
                    .append(at <= 0 ? -1 : Math.max(0L, now - at))
                    .append(";type=")
                    .append(p.getString("type", ""))
                    .append(";stage=")
                    .append(p.getString("stage", ""))
                    .append(";fingerprint=")
                    .append(p.getString("fingerprint", ""))
                    .append('\n');
            s.append("memory free/total/max=")
                    .append(Runtime.getRuntime().freeMemory())
                    .append('/')
                    .append(Runtime.getRuntime().totalMemory())
                    .append('/')
                    .append(Runtime.getRuntime().maxMemory())
                    .append('\n');
        } catch (Throwable e) {
            s.append("crashSnapshot=ERROR:").append(e.getClass().getSimpleName()).append('\n');
        }
    }

    private static void renderMetrics(StringBuilder s, Metrics m) {
        s.append("解析指标 hit=")
                .append(fmt(m.hit))
                .append(";coverage=")
                .append(fmt(m.coverage))
                .append(";ece=")
                .append(fmt(m.ece))
                .append(";settled=")
                .append(m.settled)
                .append(";wilsonLower=")
                .append(fmt(m.wilsonLower))
                .append('\n');
        s.append("40%目标=")
                .append(rateVerdict(m, 0.40d))
                .append("；50%目标=")
                .append(rateVerdict(m, 0.50d))
                .append("；60%目标=")
                .append(rateVerdict(m, 0.60d))
                .append("；频率15%=")
                .append(
                        m.coverage < 0
                                ? "NO_EVIDENCE"
                                : (m.coverage >= 0.15d ? "OBSERVED_PASS" : "OBSERVED_LOW"))
                .append('\n');
    }

    private static void diagnoseTargets(StringBuilder s, Metrics m, String raw) {
        String u = safe(raw).toUpperCase(Locale.US);
        if (u.contains("NOT_PROVEN")
                || u.contains("NO_SAMPLE")
                || m.settled >= 0 && m.settled < AppConfig.B07R2_MIN_SETTLED_FOR_RATE_CLAIM) {
            s.append("正式结论=NOT_PROVEN：样本/证明门不足，不得宣称40/50/60。\n");
            return;
        }
        if (m.ece >= 0 && m.ece > 0.04d) s.append("校准=FAIL(ECE>4%)；先降虚高概率再扩大频率。\n");
        if (m.hit >= 0 && m.hit < 0.40d)
            s.append("命中=低于40%工程目标；需要找负贡献模块/第三号拖累/状态漂移/Ranker弱项，不允许靠降门槛硬出手。\n");
        if (m.coverage >= 0 && m.coverage < 0.15d)
            s.append("频率=低于15%；只能扩大已证明状态原型/互补专家覆盖，不能用频率债务补单。\n");
    }

    private static void rootCause(
            StringBuilder s, Context c, DbStore d, long now, Metrics m, String raw) {
        boolean runtimeBad = false;
        try {
            ServiceHeartbeatStore h = new ServiceHeartbeatStore(c.getApplicationContext());
            runtimeBad = h.isHeartbeatExpired() || !h.isRuntimeLeaseFresh() || h.isBusinessStale();
        } catch (Throwable e) {
            runtimeBad = true;
        }
        String all =
                (safe(raw)
                                + ' '
                                + joinStates(
                                        d,
                                        new String[] {
                                            "last_error",
                                            "module_effect_summary",
                                            "dynamic_freeze_summary",
                                            "rule_health_summary"
                                        }))
                        .toUpperCase(Locale.US);
        if (runtimeBad || all.contains("OOM") || all.contains("CRASH"))
            s.append("P0=运行层：后台中断/假活/OOM/崩溃会直接造成不抓取、不分析、不预测；先修运行，策略胜率暂不评价。\n");
        if (all.contains("FUTURE")
                || all.contains("LEAK")
                || all.contains("OUT_OF_ORDER")
                || all.contains("DATA_ERROR")) s.append("P0=数据/PIT：未来泄漏、乱序或目标期错位会让回测虚高或实时失真。\n");
        if (all.contains("DUPLICATE") || all.contains("CONFLICT") || all.contains("BYPASS"))
            s.append("P0=Owner冲突：重复概率Owner/旁路BET/重复Evidence会使主链不可信。\n");
        if (m.hit >= 0 && m.hit < 0.40d)
            s.append("P1=模型效果：严格OOS命中不足；优先做逐模块消融、第三号条件排名、状态分层校准、低相关融合。\n");
        if (m.coverage >= 0 && m.coverage < 0.15d) s.append("P1=覆盖不足：扩高质量状态域和互补专家，不降低真实动作门。\n");
        if (m.ece >= 0 && m.ece > 0.04d) s.append("P1=校准：概率虚高/虚低；使用层级校准并限制小样本。\n");
        if (!runtimeBad && m.hit < 0 && !all.contains("NOT_PROVEN"))
            s.append("P2=证据解析不足：状态字段没有稳定的结构化指标；先补结构化诊断账本。\n");
    }

    private static void strategyPlan(StringBuilder s, Metrics m, String raw) {
        s.append("A. ExpertCorrelationAuditEngine：EvidenceGroup相关矩阵/互信息/重复证据审计，高相关模块降权。\n");
        s.append(
                "B. AblationLedgerEngine：逐模块严格时间块开启/关闭，对比Hit/Coverage/Brier/ECE/WilsonLCB，负贡献自动WATCH/FREEZE。\n");
        s.append(
                "C. ThirdNumberConditionalRankEngine：建模第三号边际贡献P(hit|n1,n2,state)-P(hit|n1,n2)，负贡献则重排。\n");
        s.append(
                "D. HierarchicalCalibrationEngine：position×regime×evidence-domain分层Beta/分桶收缩，解决小样本概率虚高。\n");
        s.append("E. RegimeDriftCertificateEngine：JS/PSI/CUSUM/BOCPD联合漂移证书，只缩权失效专家。\n");
        s.append(
                "F. CertifiedCoverageExpansionEngine：从SHADOW中寻找跨4时间块稳定、WilsonLCB过线的状态原型逐级晋级，Coverage先15-20%，再20-30%。\n");
        s.append(
                "G. RankerResidualMiningEngine：统计真实号码落在rank4-6/7-10的条件残差，训练pairwise/listwise残差修正。\n");
        s.append(
                "40%路线=先把运行/PIT/Owner清零，再让OOS"
                    + " Hit>=40%、Coverage>=15%、ECE<=4%、多块一致；50/60路线=必须发现额外、互补、可重复的信息源，否则算法复杂度本身不会创造预测信息。\n");
        s.append("频率路线=扩大已证明域，不动硬风险门；稳定性路线=相关惩罚+漂移缩权+层级校准+消融冻结。\n");
    }

    private static String rateVerdict(Metrics m, double target) {
        if (m.hit < 0) return "NO_EVIDENCE";
        if (m.settled >= 0 && m.settled < AppConfig.B07R2_MIN_SETTLED_FOR_RATE_CLAIM)
            return "SAMPLE_TOO_SMALL";
        if (m.wilsonLower >= 0)
            return m.wilsonLower >= target ? "PROVEN_BY_PARSED_LCB" : "NOT_PROVEN_LCB";
        return m.hit >= target ? "OBSERVED_ONLY_NOT_PROVEN" : "OBSERVED_LOW";
    }

    private static String fmt(double x) {
        return x < 0 ? "NA" : String.format(Locale.US, "%.2f%%", x * 100.0d);
    }

    private static void section(StringBuilder s, String x) {
        s.append("\n【").append(x).append("】\n");
    }

    private static void states(StringBuilder s, DbStore d, String[] keys) {
        for (int i = 0; i < keys.length; i++) {
            String v = state(d, keys[i]);
            s.append(keys[i])
                    .append('=')
                    .append(
                            v.length() == 0
                                    ? "NO_DATA"
                                    : clip(v, AppConfig.B07R2_REPORT_RAW_STATE_CAP))
                    .append('\n');
        }
    }

    private static String joinStates(DbStore d, String[] keys) {
        StringBuilder s = new StringBuilder();
        for (int i = 0; i < keys.length; i++) s.append(state(d, keys[i])).append(' ');
        return s.toString();
    }

    private static String state(DbStore d, String key) {
        if (d == null) return "";
        try {
            Method m = d.getClass().getMethod("getState", String.class);
            Object v = m.invoke(d, key);
            return v == null ? "" : String.valueOf(v);
        } catch (Throwable e) {
            return "";
        }
    }

    private static String clip(String x, int n) {
        return clipWithDigest(x, n);
    }

    private static String clipWithDigest(String x, int n) {
        if (x == null) return "";
        if (x.length() <= n) return x;
        int head = Math.max(256, (n - 180) * 3 / 4), tail = Math.max(128, n - head - 180);
        if (head + tail > x.length()) return x;
        return x.substring(0, head)
                + "...[VALUE_BOUNDED len="
                + x.length()
                + " sha256="
                + V51ChecksumEngine.sha256(x)
                + "]..."
                + x.substring(x.length() - tail);
    }

    private static String safe(String x) {
        return x == null ? "" : x;
    }

    private static boolean containsAny(String x, String[] a) {
        for (int i = 0; i < a.length; i++) if (x.contains(a[i])) return true;
        return false;
    }

    static final class Metrics {
        double hit = -1, coverage = -1, ece = -1, wilsonLower = -1;
        int settled = -1;

        static Metrics parse(String raw) {
            Metrics m = new Metrics();
            String x = safe(raw);
            m.hit = percentAny(x, new String[] {"Hit=", "命中率=", "hitRate=", "realHitRate="});
            m.coverage = percentAny(x, new String[] {"Coverage=", "覆盖率=", "coverage="});
            m.ece = percentAny(x, new String[] {"ECE=", "ece="});
            m.wilsonLower = percentAny(x, new String[] {"WilsonLower=", "Wilson95%下界=", "LCB="});
            m.settled = intAny(x, new String[] {"settled=", "已结算=", "结算="});
            return m;
        }

        static double percentAny(String x, String[] keys) {
            for (int i = 0; i < keys.length; i++) {
                int p = x.indexOf(keys[i]);
                if (p >= 0) {
                    p += keys[i].length();
                    StringBuilder n = new StringBuilder();
                    while (p < x.length()) {
                        char c = x.charAt(p++);
                        if ((c >= '0' && c <= '9') || c == '.' || c == '-') n.append(c);
                        else if (n.length() > 0) break;
                    }
                    try {
                        double v = Double.parseDouble(n.toString());
                        return v > 1.0d ? v / 100.0d : v;
                    } catch (Throwable e) {
                    }
                }
            }
            return -1;
        }

        static int intAny(String x, String[] keys) {
            for (int i = 0; i < keys.length; i++) {
                int p = x.indexOf(keys[i]);
                if (p >= 0) {
                    p += keys[i].length();
                    StringBuilder n = new StringBuilder();
                    while (p < x.length()) {
                        char c = x.charAt(p++);
                        if (c >= '0' && c <= '9') n.append(c);
                        else if (n.length() > 0) break;
                    }
                    try {
                        return Integer.parseInt(n.toString());
                    } catch (Throwable e) {
                    }
                }
            }
            return -1;
        }
    }
}
