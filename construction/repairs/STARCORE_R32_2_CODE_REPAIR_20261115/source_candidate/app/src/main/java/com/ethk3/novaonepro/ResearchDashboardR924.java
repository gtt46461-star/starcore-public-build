package com.ethk3.novaonepro;

import android.database.Cursor;

/** Four read-only views over persisted evidence. Reading never runs learning or authorizes a trade. */
public final class ResearchDashboardR924 {
    public static final String[] TITLES = {"① 深度学习", "② 动态迭代", "③ 创造能力", "④ 原子世界・分子・因子"};
    private ResearchDashboardR924() {}

    public static String[] capture(DbStore store, boolean detail) {
        String[] out = new String[4];
        if (store != null && !SchemaDomainCoordinatorR926.matches(store.readable(),SchemaDomainCoordinatorR926.researchContract())) {
            String status = "\n研究数据库正在初始化：" + ResearchSchemaCoordinatorR926.summary()
                    + "\n请稍后刷新；已有主链数据不会被清除。";
            for (int i = 0; i < out.length; i++) out[i] = TITLES[i] + status;
            return out;
        }
        ResearchFunnelR967.Snapshot summary=ResearchFunnelR967.capture(store);
        for (int i = 0; i < out.length; i++) {
            try { out[i] = summary.render()+"\n\n【分时读取的详细视图；不作为上方统一漏斗分母】\n"+section(store, i, detail); }
            catch (Throwable failure) { out[i] = TITLES[i] + "\n读取失败：" + failure.getClass().getSimpleName(); }
        }
        return out;
    }

    public static String report(DbStore store) {
        StringBuilder b = new StringBuilder();
        for (String section : capture(store, true)) b.append(section).append("\n\n");
        return b.toString();
    }

    public static String captureSection(DbStore s,int kind) {
        if(kind<0||kind>3)throw new IllegalArgumentException("SECTION_RANGE");
        return ResearchFunnelR967.capture(s).render()+"\n\n【分时读取的详细视图】\n"+section(s,kind,true);
    }
    private static String compact(DbStore s,int kind) {
        StringBuilder b=new StringBuilder(TITLES[kind]).append('\n');
        if(kind==0){
            b.append("职责：从已结算的赛前特征学习；输出模型权重。\n");
            state(b,s,"神经网络更新","r909_tiny_deep_updates");state(b,s,"训练到期号","r909_tiny_deep_trained_until");
            state(b,s,"最近训练状态","r909_tiny_deep_status");
            state(b,s,"原子神经学习","r943_atomic_world_training");
            b.append("独立效果：待未来样本与消融证明；研究训练次数不代表胜率提升。\n");
        }else if(kind==1){
            b.append("职责：按结算证据评估、定向修复、保留或淘汰。\n");
            b.append("观察候选=").append(ResearchPipelineStatusR941.count(s,ResearchLifecycleR932.TABLE,"state IN ('NURSERY','OBSERVING','EVIDENCE_READY')",null));
            b.append("；统一挑战者=").append(ResearchPipelineStatusR941.count(s, ResearchLifecycleR932.TABLE,
                    "state IN ('CHALLENGER','SHADOW','INFLUENCE','CHAMPION')", null)).append('\n');
            b.append("系统漏捕获：继续观察；质量失败：保留首次原因。\n");
            state(b,s,"最近实质变化时间","r922_research_last_material_change_at");
        }else if(kind==2){
            b.append("职责：生成可执行程序，积累独立未来样本后申请接入。\n");
            b.append(ResearchPipelineStatusR941.compact(s));
            state(b,s,"最近生成名称","r915_genesis_last_generated_names");state(b,s,"公式消费者状态","r903_mainline_governor_applied");
            b.append("下一步：未来样本与质量门 → 独立影子窗口 → 后续主链影响窗口。\n");
        }else{
            b.append("职责：提供可计算的基础特征和组合，供算法使用。\n");
            b.append("登记=").append(GenesisSchemaRegistryR915.atomCount()).append("；生产赛前语义=").append(GenesisSchemaRegistryR915.productionScoreEligibleCount()).append('\n');
            b.append("研究代理=").append(GenesisSchemaRegistryR915.researchProxyCount()).append("；开奖后证据=").append(GenesisSchemaRegistryR915.settlementEvidenceCount()).append("；审计身份=").append(GenesisSchemaRegistryR915.auditIdentityCount()).append('\n');
            state(b,s,"原资产完整性","r915_legacy_integrity");
            b.append(AtomicToolWorldR943.report(s,false)).append('\n');
        }
        return b.append("点击查看完整机制、记录和证据；不删减详情。").toString();
    }
    private static String section(DbStore s, int kind, boolean detail) {
        if (s == null) return TITLES[kind] + "\n数据库尚未就绪";
        if(!detail)return compact(s,kind);
        StringBuilder b = new StringBuilder(TITLES[kind]).append('\n');
        if (kind == 0) {
            b.append("R925离线研究工具：D0、残差D1、双尺度D2、蒸馏D3与九专家Gate；未导入真实训练权重，不冒充手机正在使用的新冠军。\n");
            b.append("神经网络：7项输入 → 16 → 8 → 1；按已结算样本逐期训练。\n");
            state(b,s,"权重更新次数","r909_tiny_deep_updates");
            state(b,s,"训练截止期","r909_tiny_deep_trained_until");
            state(b,s,"上次训练损失（越低越好，单次值不是胜率）","r909_tiny_deep_last_loss");
            state(b,s,"最近训练状态","r909_tiny_deep_status");
            state(b,s,"线性自适应FTRL更新次数","r811_ftrl_updates");
            state(b,s,"FTRL训练截止期","r811_ftrl_trained_until");
            state(b,s,"未来预测冻结状态","r909_tiny_deep_predraw_status");
            b.append("实际效果看严格样本外验证；神经网络当前为研究观察模型，更新次数不代表命中提升。\n");
            if (detail) {
                state(b,s,"模型指纹","r909_tiny_deep_artifact_hash");
                state(b,s,"更新记录时间（毫秒）","r909_tiny_deep_at");
                state(b,s,"学习连续性","r912_research_continuity_status");
            state(b,s,"原子组神经学习","r943_atomic_world_training");
                state(b,s,"无效特征跳过数","r924_tiny_deep_invalid_rows_skipped");
                b.append(ResearchMainlineContributionAuditR922.recentDetail(s,120));
            }
        } else if (kind == 1) {
            b.append("流程：复盘失败 → 生成或变异 → 开奖前冻结 → 开奖后结算 → 证据审查 → 保留、淘汰或晋级。\n");
            state(b,s,"最近一次迭代审查","r912_algorithm_lifecycle");
            state(b,s,"本轮数量与证据变化","r922_research_last_delta");
            state(b,s,"最近实质变化","r922_research_last_material_change");
            state(b,s,"最近实质变化时间（毫秒）","r922_research_last_material_change_at");
            state(b,s,"连续未中复盘","r923_miss_streak_analysis");
            state(b,s,"本轮创造执行","r915_asset_genesis_tick");
            b.append("算法结构与权重可以随证据更新；软件安装版本需安装新APK更新。\n");
            if (detail) {
                state(b,s,"失败向量","r919_algorithm_failure_vector");
                state(b,s,"本轮结构创造原因","r919_algorithm_creation_reasons");
                state(b,s,"研究运行状态","r908_research_tick_status");
                state(b,s,"研究成本（毫秒）","r908_research_tick_cost_ms");
                state(b,s,"捕获轮转游标","r924_predraw_next_lane");
                state(b,s,"学习轮转游标（14任务）","r941_continuity_next_lane");
            }
        } else if (kind == 2) {
            b.append("持久候选总数=").append(count(s,"r952_candidate",""))
                    .append("；已完成训练=").append(count(s,"research_development_receipt_v2",""))
                    .append("；待逐期训练=").append(count(s,"r952_research_outbox"," WHERE state NOT IN ('COMPLETED','DEPENDENCY_RETIRED')"))
                    .append("\n旧算法档案总数=").append(count(s,DbHelper.TABLE_R908_ALGORITHM_GENOME,""))
                    .append("；旧算法登记挑战者=").append(count(s,DbHelper.TABLE_R908_ALGORITHM_GENOME," WHERE status='CHALLENGER_FUTURE_OOS'"))
                    .append("\n创生资产=").append(count(s,AssetGenesisEngineR915.TABLE_ASSET_GENOME,""))
                    .append("；旧创生资产证书记录=").append(count(s,AssetGenesisEngineR915.TABLE_ASSET_CERTIFICATE," WHERE status='ACTIVE'"))
                    .append("\n公式=").append(count(s,DbHelper.TABLE_R906_RESEARCH_ASSET,""))
                    .append("；可执行=").append(count(s,DbHelper.TABLE_R906_RESEARCH_ASSET," WHERE runtime_executable=1")).append('\n');
            state(b,s,"最近生成名称","r915_genesis_last_generated_names");
            state(b,s,"参与主链的实际状态","r915_asset_mixer_status");
            b.append("高命中记录需要未来样本、稳定性与校准证据共同过关。点击本框查看每项准入缺口、算法机制和逐期贡献。\n");
            if (detail) {
                b.append(ResearchPipelineStatusR941.capture(s)).append('\n');
                b.append(ResearchReadinessR924.capture(s)).append('\n');
                RuntimeTruthSnapshotR906.appendGenesisAssets(b,s.readable(),Integer.MAX_VALUE);
                RuntimeTruthSnapshotR906.appendAlgorithms(b,s.readable(),Integer.MAX_VALUE);
                RuntimeTruthSnapshotR906.appendArchivedAlgorithms(b,s.readable(),Integer.MAX_VALUE);
                b.append(ResearchMainlineContributionAuditR922.recentDetail(s,120));
                b.append("全部公式、冻结及证书原始清单见一键导出的完整检测报告。\n");
            }
        } else {
            b.append("登记原子=").append(GenesisSchemaRegistryR915.atomCount())
                    .append("；可用于生产评分=").append(GenesisSchemaRegistryR915.productionScoreEligibleCount())
                    .append("\n研究代理=").append(GenesisSchemaRegistryR915.researchProxyCount())
                    .append("；审计身份=").append(GenesisSchemaRegistryR915.auditIdentityCount())
                    .append("；开奖后证据=").append(GenesisSchemaRegistryR915.settlementEvidenceCount())
                    .append("\n新原子提案=").append(count(s,AssetGenesisEngineR915.TABLE_ATOM_PROPOSAL,""))
                    .append("；保留的固定资产=").append(count(s,AssetGenesisEngineR915.TABLE_LEGACY_REGISTRY," WHERE locked=1")).append('\n');
            b.append("原子是可计算的基础特征；分子是特征组合。研究代理及开奖后标签隔离，不能拿未来结果参与本期预测。\n");
            state(b,s,"原资产完整性","r915_legacy_integrity");
            state(b,s,"最近因子研究需求","r915_genesis_last_need");
            b.append(AtomicToolWorldR943.report(s,detail)).append("\n");
            if (detail) {
                b.append(AtomQualityLedgerR925.report(s)).append('\n');
                RuntimeTruthSnapshotR906.appendAtomRegistry(b);
                RuntimeTruthSnapshotR906.appendAtomProposals(b,s.readable(),Integer.MAX_VALUE);
            }
        }
        return b.toString();
    }
    private static void state(StringBuilder b, DbStore s, String label, String key) {
        String v=s.getState(key,"");
        b.append(label).append("：").append(v==null||v.length()==0?"尚无记录":v).append('\n');
    }
    private static long count(DbStore s,String table,String where) {
        Cursor c=s.readable().rawQuery("SELECT COUNT(1) FROM "+table+where,null);
        try { if(!c.moveToFirst()) throw new IllegalStateException("COUNT_MISSING"); return c.getLong(0); }
        finally { c.close(); }
    }

public static String[] captureSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, boolean detail) {
        String[] out = new String[4];
        if (store != null && !SchemaDomainCoordinatorR926.matchesSnapshotCP4(report, store.readable(),SchemaDomainCoordinatorR926.researchContract())) {
            String status = "\n研究数据库正在初始化：" + ResearchSchemaCoordinatorR926.summary()
                    + "\n请稍后刷新；已有主链数据不会被清除。";
            for (int i = 0; i < out.length; i++) out[i] = TITLES[i] + status;
            return out;
        }
        ResearchFunnelR967.Snapshot summary=ResearchFunnelR967.captureSnapshotCP4(report, store);
        for (int i = 0; i < out.length; i++) {
            try { out[i] = summary.render()+"\n\n【分时读取的详细视图；不作为上方统一漏斗分母】\n"+sectionSnapshotCP4(report, store, i, detail); }
            catch (Throwable failure) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",failure);}
        }
        return out;
    }

public static String sectionSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 s, int kind, boolean detail) {
        if (s == null) return TITLES[kind] + "\n数据库尚未就绪";
        if(!detail)return compactSnapshotCP4(report, s,kind);
        com.ethk3.novaonepro.ReportTextCP12 b = new com.ethk3.novaonepro.ReportTextCP12(report, TITLES[kind]).append('\n');
        if (kind == 0) {
            b.append("R925离线研究工具：D0、残差D1、双尺度D2、蒸馏D3与九专家Gate；未导入真实训练权重，不冒充手机正在使用的新冠军。\n");
            b.append("神经网络：7项输入 → 16 → 8 → 1；按已结算样本逐期训练。\n");
            stateSnapshotCP4(report, b,s,"权重更新次数","r909_tiny_deep_updates");
            stateSnapshotCP4(report, b,s,"训练截止期","r909_tiny_deep_trained_until");
            stateSnapshotCP4(report, b,s,"上次训练损失（越低越好，单次值不是胜率）","r909_tiny_deep_last_loss");
            stateSnapshotCP4(report, b,s,"最近训练状态","r909_tiny_deep_status");
            stateSnapshotCP4(report, b,s,"线性自适应FTRL更新次数","r811_ftrl_updates");
            stateSnapshotCP4(report, b,s,"FTRL训练截止期","r811_ftrl_trained_until");
            stateSnapshotCP4(report, b,s,"未来预测冻结状态","r909_tiny_deep_predraw_status");
            b.append("实际效果看严格样本外验证；神经网络当前为研究观察模型，更新次数不代表命中提升。\n");
            if (detail) {
                stateSnapshotCP4(report, b,s,"模型指纹","r909_tiny_deep_artifact_hash");
                stateSnapshotCP4(report, b,s,"更新记录时间（毫秒）","r909_tiny_deep_at");
                stateSnapshotCP4(report, b,s,"学习连续性","r912_research_continuity_status");
            stateSnapshotCP4(report, b,s,"原子组神经学习","r943_atomic_world_training");
                stateSnapshotCP4(report, b,s,"无效特征跳过数","r924_tiny_deep_invalid_rows_skipped");
                b.append(ResearchMainlineContributionAuditR922.recentDetailSnapshotCP4(report, s,120));
            }
        } else if (kind == 1) {
            b.append("流程：复盘失败 → 生成或变异 → 开奖前冻结 → 开奖后结算 → 证据审查 → 保留、淘汰或晋级。\n");
            stateSnapshotCP4(report, b,s,"最近一次迭代审查","r912_algorithm_lifecycle");
            stateSnapshotCP4(report, b,s,"本轮数量与证据变化","r922_research_last_delta");
            stateSnapshotCP4(report, b,s,"最近实质变化","r922_research_last_material_change");
            stateSnapshotCP4(report, b,s,"最近实质变化时间（毫秒）","r922_research_last_material_change_at");
            stateSnapshotCP4(report, b,s,"连续未中复盘","r923_miss_streak_analysis");
            stateSnapshotCP4(report, b,s,"本轮创造执行","r915_asset_genesis_tick");
            b.append("算法结构与权重可以随证据更新；软件安装版本需安装新APK更新。\n");
            if (detail) {
                stateSnapshotCP4(report, b,s,"失败向量","r919_algorithm_failure_vector");
                stateSnapshotCP4(report, b,s,"本轮结构创造原因","r919_algorithm_creation_reasons");
                stateSnapshotCP4(report, b,s,"研究运行状态","r908_research_tick_status");
                stateSnapshotCP4(report, b,s,"研究成本（毫秒）","r908_research_tick_cost_ms");
                stateSnapshotCP4(report, b,s,"捕获轮转游标","r924_predraw_next_lane");
                stateSnapshotCP4(report, b,s,"学习轮转游标（14任务）","r941_continuity_next_lane");
            }
        } else if (kind == 2) {
            b.append("算法总数=").append(countSnapshotCP4(report, s,DbHelper.TABLE_R908_ALGORITHM_GENOME,""))
                    .append("；旧算法登记挑战者=").append(countSnapshotCP4(report, s,DbHelper.TABLE_R908_ALGORITHM_GENOME," WHERE status='CHALLENGER_FUTURE_OOS'"))
                    .append("\n创生资产=").append(countSnapshotCP4(report, s,AssetGenesisEngineR915.TABLE_ASSET_GENOME,""))
                    .append("；旧创生资产证书记录=").append(countSnapshotCP4(report, s,AssetGenesisEngineR915.TABLE_ASSET_CERTIFICATE," WHERE status='ACTIVE'"))
                    .append("\n公式=").append(countSnapshotCP4(report, s,DbHelper.TABLE_R906_RESEARCH_ASSET,""))
                    .append("；可执行=").append(countSnapshotCP4(report, s,DbHelper.TABLE_R906_RESEARCH_ASSET," WHERE runtime_executable=1")).append('\n');
            stateSnapshotCP4(report, b,s,"最近生成名称","r915_genesis_last_generated_names");
            stateSnapshotCP4(report, b,s,"参与主链的实际状态","r915_asset_mixer_status");
            b.append("高命中记录需要未来样本、稳定性与校准证据共同过关。点击本框查看每项准入缺口、算法机制和逐期贡献。\n");
            if (detail) {
                b.append(ResearchPipelineStatusR941.captureSnapshotCP4(report, s)).append('\n');
                b.append(ResearchReadinessR924.captureSnapshotCP4(report, s)).append('\n');
                RuntimeTruthSnapshotR906.appendGenesisAssetsSnapshotCP4(report, b,s.readable(),Integer.MAX_VALUE);
                RuntimeTruthSnapshotR906.appendAlgorithmsSnapshotCP4(report, b,s.readable(),Integer.MAX_VALUE);
                RuntimeTruthSnapshotR906.appendArchivedAlgorithmsSnapshotCP4(report, b,s.readable(),Integer.MAX_VALUE);
                b.append(ResearchMainlineContributionAuditR922.recentDetailSnapshotCP4(report, s,120));
                b.append("全部公式、冻结及证书原始清单见一键导出的完整检测报告。\n");
            }
        } else {
            b.append("登记原子=").append(GenesisSchemaRegistryR915.atomCount())
                    .append("；可用于生产评分=").append(GenesisSchemaRegistryR915.productionScoreEligibleCount())
                    .append("\n研究代理=").append(GenesisSchemaRegistryR915.researchProxyCount())
                    .append("；审计身份=").append(GenesisSchemaRegistryR915.auditIdentityCount())
                    .append("；开奖后证据=").append(GenesisSchemaRegistryR915.settlementEvidenceCount())
                    .append("\n新原子提案=").append(countSnapshotCP4(report, s,AssetGenesisEngineR915.TABLE_ATOM_PROPOSAL,""))
                    .append("；保留的固定资产=").append(countSnapshotCP4(report, s,AssetGenesisEngineR915.TABLE_LEGACY_REGISTRY," WHERE locked=1")).append('\n');
            b.append("原子是可计算的基础特征；分子是特征组合。研究代理及开奖后标签隔离，不能拿未来结果参与本期预测。\n");
            stateSnapshotCP4(report, b,s,"原资产完整性","r915_legacy_integrity");
            stateSnapshotCP4(report, b,s,"最近因子研究需求","r915_genesis_last_need");
            b.append(AtomicToolWorldR943.reportSnapshotCP4(report, s,detail)).append("\n");
            if (detail) {
                b.append(AtomQualityLedgerR925.reportSnapshotCP4(report, s)).append('\n');
                RuntimeTruthSnapshotR906.appendAtomRegistrySnapshotCP4(report, b);
                RuntimeTruthSnapshotR906.appendAtomProposalsSnapshotCP4(report, b,s.readable(),Integer.MAX_VALUE);
            }
        }
        return b.toString();
    }

public static void stateSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportTextCP12 b, com.ethk3.novaonepro.ReportReadContextCP4 s, String label, String key) {
        String v=s.getState(key,"");
        b.append(label).append("：").append(v==null||v.length()==0?"尚无记录":v).append('\n');
    }

public static long countSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 s,String table,String where) {
        Cursor c=report.select("SELECT COUNT(1) FROM "+table+where,null);
        try { if(!c.moveToFirst()) throw new IllegalStateException("COUNT_MISSING"); return c.getLong(0); }
        finally { c.close(); }
    }

public static String compactSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 s,int kind) {
        com.ethk3.novaonepro.ReportTextCP12 b=new com.ethk3.novaonepro.ReportTextCP12(report, TITLES[kind]).append('\n');
        if(kind==0){
            b.append("职责：从已结算的赛前特征学习；输出模型权重。\n");
            stateSnapshotCP4(report, b,s,"神经网络更新","r909_tiny_deep_updates");stateSnapshotCP4(report, b,s,"训练到期号","r909_tiny_deep_trained_until");
            stateSnapshotCP4(report, b,s,"最近训练状态","r909_tiny_deep_status");
            stateSnapshotCP4(report, b,s,"原子神经学习","r943_atomic_world_training");
            b.append("独立效果：待未来样本与消融证明；研究训练次数不代表胜率提升。\n");
        }else if(kind==1){
            b.append("职责：按结算证据评估、定向修复、保留或淘汰。\n");
            b.append("观察候选=").append(ResearchPipelineStatusR941.countSnapshotCP4(report, s,ResearchLifecycleR932.TABLE,"state IN ('NURSERY','OBSERVING','EVIDENCE_READY')",null));
            b.append("；统一挑战者=").append(ResearchPipelineStatusR941.countSnapshotCP4(report, s, ResearchLifecycleR932.TABLE,
                    "state IN ('CHALLENGER','SHADOW','INFLUENCE','CHAMPION')", null)).append('\n');
            b.append("系统漏捕获：继续观察；质量失败：保留首次原因。\n");
            stateSnapshotCP4(report, b,s,"最近实质变化时间","r922_research_last_material_change_at");
        }else if(kind==2){
            b.append("职责：生成可执行程序，积累独立未来样本后申请接入。\n");
            b.append(ResearchPipelineStatusR941.compactSnapshotCP4(report, s));
            stateSnapshotCP4(report, b,s,"最近生成名称","r915_genesis_last_generated_names");stateSnapshotCP4(report, b,s,"公式消费者状态","r903_mainline_governor_applied");
            b.append("下一步：未来样本与质量门 → 独立影子窗口 → 后续主链影响窗口。\n");
        }else{
            b.append("职责：提供可计算的基础特征和组合，供算法使用。\n");
            b.append("登记=").append(GenesisSchemaRegistryR915.atomCount()).append("；生产赛前语义=").append(GenesisSchemaRegistryR915.productionScoreEligibleCount()).append('\n');
            b.append("研究代理=").append(GenesisSchemaRegistryR915.researchProxyCount()).append("；开奖后证据=").append(GenesisSchemaRegistryR915.settlementEvidenceCount()).append("；审计身份=").append(GenesisSchemaRegistryR915.auditIdentityCount()).append('\n');
            stateSnapshotCP4(report, b,s,"原资产完整性","r915_legacy_integrity");
            b.append(AtomicToolWorldR943.reportSnapshotCP4(report, s,false)).append('\n');
        }
        return b.append("点击查看完整机制、记录和证据；不删减详情。").toString();
    }

public static String reportSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        com.ethk3.novaonepro.ReportTextCP12 b = new com.ethk3.novaonepro.ReportTextCP12(report);
        for (String section : captureSnapshotCP4(report, store, true)) b.append(section).append("\n\n");
        return b.toString();
    }
}
