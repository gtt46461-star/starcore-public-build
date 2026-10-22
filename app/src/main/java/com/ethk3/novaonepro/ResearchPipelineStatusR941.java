package com.ethk3.novaonepro;
import android.database.Cursor;

/** Read only projection of the unified lifecycle, certificates and committed production evidence. */
public final class ResearchPipelineStatusR941 {
    private ResearchPipelineStatusR941() {}
    static String count(DbStore store, String table, String predicate, String[] args) {
        try (Cursor c = store.readable().rawQuery("SELECT COUNT(*) FROM " + table + " WHERE " + predicate, args)) {
            if (!c.moveToFirst()) return "N/A:COUNT_MISSING";
            return String.valueOf(c.getLong(0));
        } catch (RuntimeException error) { return "N/A:" + error.getClass().getSimpleName(); }
    }
    static String certificateProjectionPending(DbStore store) {
        if (store == null) return "N/A:STORE_NULL";
        String sql="SELECT COUNT(*) FROM "+ResearchLifecycleR932.TABLE+" l JOIN "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" c ON "
                +"c.candidate_id=substr(l.candidate_key,instr(l.candidate_key,':')+1) AND c.structural_hash=l.structural_hash AND c.consumer=CASE "
                +"WHEN l.candidate_key LIKE 'algorithm:%' THEN 'INVENTED_ALGORITHM' WHEN l.candidate_key LIKE 'genesis:%' THEN 'GENESIS_ASSET' "
                +"WHEN l.candidate_key LIKE 'formula:%' THEN 'FORMULA_GRAPH' ELSE '' END "
                +"WHERE c.state='ACTIVE' AND c.updated_at>l.reviewed_at AND l.state NOT IN ('QUALITY_PRUNED','RESOURCE_RETIRED','QUARANTINED')";
        try(Cursor c=store.readable().rawQuery(sql,null)){return c.moveToFirst()?String.valueOf(c.getLong(0)):"N/A:COUNT_MISSING";}
        catch(RuntimeException error){return "N/A:"+error.getClass().getSimpleName();}
    }
    public static String compact(DbStore store) {
        if (store == null) return "统一研究链路：数据库未就绪";
        return "统一登记=" + count(store, ResearchLifecycleR932.TABLE, "1=1", null)
                + "；挑战者=" + count(store, ResearchLifecycleR932.TABLE, "state IN ('CHALLENGER','SHADOW','INFLUENCE','CHAMPION')", null)
                + "\n统一ACTIVE证书记录=" + count(store, ResearchPlatformStoreR929.TABLE_CERTIFICATE, "state='ACTIVE'", null)
                + "（L4=" + count(store, ResearchPlatformStoreR929.TABLE_CERTIFICATE, "state='ACTIVE' AND level='L4_CHALLENGER'", null)
                + "；L5=" + count(store, ResearchPlatformStoreR929.TABLE_CERTIFICATE, "state='ACTIVE' AND level='L5_SHADOW'", null)
                + "；L6=" + count(store, ResearchPlatformStoreR929.TABLE_CERTIFICATE, "state='ACTIVE' AND level='L6_CONTROLLED_INFLUENCE'", null)
                + "；L7=" + count(store, ResearchPlatformStoreR929.TABLE_CERTIFICATE, "state='ACTIVE' AND level='L7_CHAMPION'", null) + "）"
                + "；待生命周期投影=" + certificateProjectionPending(store)
                + "；当前版本已应用期数=" + count(store, ResearchMainlineContributionAuditR922.TABLE,
                        "runtime_version=? AND applied=1 AND influence_weight>0", new String[]{AppConfig.R36_RUNTIME_REPAIR_VERSION})
                + "\n校准模型=" + count(store, CandidateCalibrationR932.MODEL, "1=1", null)
                + "；固定窗口=" + count(store, FixedResearchWindowR932.WINDOW, "1=1", null)
                + "\n证书还需逐期校验有效期和依赖；N/A表示读取失败，不等于0。\n";
    }
    public static String capture(DbStore store) {
        StringBuilder out = new StringBuilder("【挑战者、证书与主链实际应用｜统一账本】\n").append(compact(store));
        if (store == null) return out.toString();
        try(Cursor c=store.readable().rawQuery("SELECT p.target_issue,p.cursor,COALESCE(m.candidate_count,-1) FROM "+StagedResearchRosterR943.TABLE+" p LEFT JOIN "+StagedResearchRosterR943.MANIFEST+" m ON m.target_issue=p.target_issue ORDER BY p.target_issue DESC LIMIT 1",null)){
            if(c.moveToFirst()){
                String target=c.getString(0);int total=c.getInt(2);
                out.append("最近研究期=").append(target).append("｜名单准备=").append(c.getInt(1)).append('/').append(total).append('\n');
                try(Cursor stage=store.readable().rawQuery("SELECT COUNT(*),COALESCE(SUM(execution_started),0),COALESCE(SUM(frozen),0) FROM "+ResearchCaptureLedgerR932.TABLE+" WHERE target_issue=?",new String[]{target})){
                    if(stage.moveToFirst())out.append("已登记/开始计算/冻结预测=").append(stage.getInt(0)).append('/').append(stage.getInt(1)).append('/').append(stage.getInt(2)).append("；未来样本需这些预测在开奖后结算。\n");
                }
                out.append("R952执行优先：owner=").append(store.getState("r952_roster_owner_decision","NOT_RUN"))
                        .append("；pendingLaneMask=").append(GlobalResearchScheduleR932.pendingLaneMask(store,target))
                        .append("（1=神经 2=算法 4=创生 8=公式）；名单未执行完时不继续扩页。\n");
            }
        }catch(Exception failure){out.append("研究阶段读取失败：").append(failure.getClass().getSimpleName()).append('\n');}
        out.append("算法原证据审计：清洁HEAD=").append(count(store,AlgorithmProofRecoveryR948.HEAD,"status='CLEAN'",null))
                .append("；已隔离旧权重并重建=").append(count(store,AlgorithmProofRecoveryR948.HEAD,"status='RESET_CLEAN'",null))
                .append("；审计未完成=").append(count(store,AlgorithmProofRecoveryR948.HEAD,"status='AUDITING'",null))
                .append("；不可用于正式训练的旧记录=").append(count(store,AlgorithmProofRecoveryR948.QUARANTINE,"1=1",null)).append('\n');
        out.append("捕获续跑：").append(store.getState("r907_research_async_target","N/A")).append(" | ")
                .append(store.getState("r907_research_async_status","NOT_RUN")).append('\n');
        out.append("旧权重重建不补写未来样本；证书为0与没有执行是两种状态。\n");
        out.append("R954证书阶段：L4=挑战者、L5=影子（均零主链权重）；证书更新优先进入生命周期复核，待生命周期投影="+certificateProjectionPending(store)+"；L6/L7仍必须通过统一授权才可影响主链。\n");
        out.append("维护下一任务=").append(store.getState("r941_continuity_next_lane", "NOT_RUN")).append("/14\n");
        out.append("0捕获 1研究结算 2结算侧车 3压缩学习 4神经学习 5生命周期 6固定窗口 7消融 8撤销旧依赖 9发布证书 10家族 11配对 12旧算法观察 13原子世界学习\n");
        try (Cursor c = store.readable().rawQuery("SELECT state_key,state_value FROM " + DbHelper.TABLE_APP_STATE
                + " WHERE state_key LIKE 'r941_continuity_lane_%' ORDER BY state_key", null)) {
            while (c.moveToNext()) out.append(c.getString(0)).append('=').append(c.getString(1)).append('\n');
        } catch (RuntimeException failure) { out.append("维护读取失败：").append(failure.getClass().getSimpleName()).append('\n'); }
        try (Cursor c = store.readable().rawQuery("SELECT candidate_key,state,reason,future_n,paired_n FROM "
                + ResearchLifecycleR932.TABLE + " ORDER BY reviewed_at DESC,candidate_key LIMIT 8", null)) {
            while (c.moveToNext()) out.append("候选 ").append(c.getString(0)).append(" | ").append(c.getString(1))
                    .append(" | 未来/配对=").append(c.getInt(3)).append('/').append(c.getInt(4))
                    .append(" | 当前原因=").append(c.getString(2)).append('\n');
        } catch (RuntimeException failure) { out.append("候选读取失败：").append(failure.getClass().getSimpleName()).append('\n'); }
        try (Cursor c = store.readable().rawQuery("SELECT candidate_key,state,captured_n,settled_n FROM "
                + CandidateCalibrationR932.STUDY + " ORDER BY first_issue DESC LIMIT 6", null)) {
            while (c.moveToNext()) out.append("独立校准 ").append(c.getString(0)).append(" | ").append(c.getString(1))
                    .append(" | 捕获/结算=").append(c.getInt(2)).append('/').append(c.getInt(3)).append(" | 需要241期（120拟合+1隔离+120验证）\n");
        } catch (RuntimeException failure) { out.append("校准读取失败：").append(failure.getClass().getSimpleName()).append('\n'); }
        try (Cursor c = store.readable().rawQuery("SELECT target_issue,candidate_name,status,applied,influence_weight FROM "
                + ResearchMainlineContributionAuditR922.TABLE + " ORDER BY target_issue DESC LIMIT 5", null)) {
            while (c.moveToNext()) out.append("主链 ").append(c.getString(0)).append(" | ").append(c.getString(1)).append(" | ")
                    .append(c.getString(2)).append(" | applied=").append(c.getInt(3)).append(" | weight=").append(c.getDouble(4)).append('\n');
        } catch (RuntimeException failure) { out.append("主链读取失败：").append(failure.getClass().getSimpleName()).append('\n'); }
        return out.append("运行时间只增加观察机会；质量和证据必须过门，不能保证等待后就获得证书。\n").toString();
    }

public static String captureSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        com.ethk3.novaonepro.ReportTextCP12 out = new com.ethk3.novaonepro.ReportTextCP12(report, "【挑战者、证书与主链实际应用｜统一账本】\n").append(compactSnapshotCP4(report, store));
        if (store == null) return out.toString();
        try(Cursor c=report.select("SELECT p.target_issue,p.cursor,COALESCE(m.candidate_count,-1) FROM "+StagedResearchRosterR943.TABLE+" p LEFT JOIN "+StagedResearchRosterR943.MANIFEST+" m ON m.target_issue=p.target_issue ORDER BY p.target_issue DESC LIMIT 1",null)){
            if(c.moveToFirst()){
                String target=c.getString(0);int total=c.getInt(2);
                out.append("最近研究期=").append(target).append("｜名单准备=").append(c.getInt(1)).append('/').append(total).append('\n');
                try(Cursor stage=report.select("SELECT COUNT(*),COALESCE(SUM(execution_started),0),COALESCE(SUM(frozen),0) FROM "+ResearchCaptureLedgerR932.TABLE+" WHERE target_issue=?",new String[]{target})){
                    if(stage.moveToFirst())out.append("已登记/开始计算/冻结预测=").append(stage.getInt(0)).append('/').append(stage.getInt(1)).append('/').append(stage.getInt(2)).append("；未来样本需这些预测在开奖后结算。\n");
                }
                out.append("R952执行优先：owner=").append(store.getState("r952_roster_owner_decision","NOT_RUN"))
                        .append("；pendingLaneMask=").append(GlobalResearchScheduleR932.pendingLaneMaskSnapshotCP4(report, store,target))
                        .append("（1=神经 2=算法 4=创生 8=公式）；名单未执行完时不继续扩页。\n");
            }
        }catch(Exception failure){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",failure);}
        out.append("算法原证据审计：清洁HEAD=").append(countSnapshotCP4(report, store,AlgorithmProofRecoveryR948.HEAD,"status='CLEAN'",null))
                .append("；已隔离旧权重并重建=").append(countSnapshotCP4(report, store,AlgorithmProofRecoveryR948.HEAD,"status='RESET_CLEAN'",null))
                .append("；审计未完成=").append(countSnapshotCP4(report, store,AlgorithmProofRecoveryR948.HEAD,"status='AUDITING'",null))
                .append("；不可用于正式训练的旧记录=").append(countSnapshotCP4(report, store,AlgorithmProofRecoveryR948.QUARANTINE,"1=1",null)).append('\n');
        out.append("捕获续跑：").append(store.getState("r907_research_async_target","N/A")).append(" | ")
                .append(store.getState("r907_research_async_status","NOT_RUN")).append('\n');
        out.append("旧权重重建不补写未来样本；证书为0与没有执行是两种状态。\n");
        out.append("R954证书阶段：L4=挑战者、L5=影子（均零主链权重）；证书更新优先进入生命周期复核，待生命周期投影="+certificateProjectionPendingSnapshotCP4(report, store)+"；L6/L7仍必须通过统一授权才可影响主链。\n");
        out.append("维护下一任务=").append(store.getState("r941_continuity_next_lane", "NOT_RUN")).append("/14\n");
        out.append("0捕获 1研究结算 2结算侧车 3压缩学习 4神经学习 5生命周期 6固定窗口 7消融 8撤销旧依赖 9发布证书 10家族 11配对 12旧算法观察 13原子世界学习\n");
        try (Cursor c = report.select("SELECT state_key,state_value FROM " + DbHelper.TABLE_APP_STATE
                + " WHERE state_key LIKE 'r941_continuity_lane_%' ORDER BY state_key", null)) {
            while (c.moveToNext()) out.append(c.getString(0)).append('=').append(c.getString(1)).append('\n');
        } catch (RuntimeException failure) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",failure);}
        try (Cursor c = report.select("SELECT candidate_key,state,reason,future_n,paired_n FROM "
                + ResearchLifecycleR932.TABLE + " ORDER BY reviewed_at DESC,candidate_key LIMIT 8", null)) {
            while (c.moveToNext()) out.append("候选 ").append(c.getString(0)).append(" | ").append(c.getString(1))
                    .append(" | 未来/配对=").append(c.getInt(3)).append('/').append(c.getInt(4))
                    .append(" | 当前原因=").append(c.getString(2)).append('\n');
        } catch (RuntimeException failure) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",failure);}
        try (Cursor c = report.select("SELECT candidate_key,state,captured_n,settled_n FROM "
                + CandidateCalibrationR932.STUDY + " ORDER BY first_issue DESC LIMIT 6", null)) {
            while (c.moveToNext()) out.append("独立校准 ").append(c.getString(0)).append(" | ").append(c.getString(1))
                    .append(" | 捕获/结算=").append(c.getInt(2)).append('/').append(c.getInt(3)).append(" | 需要241期（120拟合+1隔离+120验证）\n");
        } catch (RuntimeException failure) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",failure);}
        try (Cursor c = report.select("SELECT target_issue,candidate_name,status,applied,influence_weight FROM "
                + ResearchMainlineContributionAuditR922.TABLE + " ORDER BY target_issue DESC LIMIT 5", null)) {
            while (c.moveToNext()) out.append("主链 ").append(c.getString(0)).append(" | ").append(c.getString(1)).append(" | ")
                    .append(c.getString(2)).append(" | applied=").append(c.getInt(3)).append(" | weight=").append(c.getDouble(4)).append('\n');
        } catch (RuntimeException failure) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",failure);}
        return out.append("运行时间只增加观察机会；质量和证据必须过门，不能保证等待后就获得证书。\n").toString();
    }

static String certificateProjectionPendingSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        if (store == null) return "N/A:STORE_NULL";
        String sql="SELECT COUNT(*) FROM "+ResearchLifecycleR932.TABLE+" l JOIN "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" c ON "
                +"c.candidate_id=substr(l.candidate_key,instr(l.candidate_key,':')+1) AND c.structural_hash=l.structural_hash AND c.consumer=CASE "
                +"WHEN l.candidate_key LIKE 'algorithm:%' THEN 'INVENTED_ALGORITHM' WHEN l.candidate_key LIKE 'genesis:%' THEN 'GENESIS_ASSET' "
                +"WHEN l.candidate_key LIKE 'formula:%' THEN 'FORMULA_GRAPH' ELSE '' END "
                +"WHERE c.state='ACTIVE' AND c.updated_at>l.reviewed_at AND l.state NOT IN ('QUALITY_PRUNED','RESOURCE_RETIRED','QUARANTINED')";
        try(Cursor c=report.select(sql,null)){return c.moveToFirst()?String.valueOf(c.getLong(0)):"N/A:COUNT_MISSING";}
        catch(RuntimeException error){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",error);}
    }

static String countSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store, String table, String predicate, String[] args) {
        try (Cursor c = report.select("SELECT COUNT(*) FROM " + table + " WHERE " + predicate, args)) {
            if (!c.moveToFirst()) return "N/A:COUNT_MISSING";
            return String.valueOf(c.getLong(0));
        } catch (RuntimeException error) {throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",error);}
    }

public static String compactSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        if (store == null) return "统一研究链路：数据库未就绪";
        return "统一登记=" + countSnapshotCP4(report, store, ResearchLifecycleR932.TABLE, "1=1", null)
                + "；挑战者=" + countSnapshotCP4(report, store, ResearchLifecycleR932.TABLE, "state IN ('CHALLENGER','SHADOW','INFLUENCE','CHAMPION')", null)
                + "\n统一ACTIVE证书记录=" + countSnapshotCP4(report, store, ResearchPlatformStoreR929.TABLE_CERTIFICATE, "state='ACTIVE'", null)
                + "（L4=" + countSnapshotCP4(report, store, ResearchPlatformStoreR929.TABLE_CERTIFICATE, "state='ACTIVE' AND level='L4_CHALLENGER'", null)
                + "；L5=" + countSnapshotCP4(report, store, ResearchPlatformStoreR929.TABLE_CERTIFICATE, "state='ACTIVE' AND level='L5_SHADOW'", null)
                + "；L6=" + countSnapshotCP4(report, store, ResearchPlatformStoreR929.TABLE_CERTIFICATE, "state='ACTIVE' AND level='L6_CONTROLLED_INFLUENCE'", null)
                + "；L7=" + countSnapshotCP4(report, store, ResearchPlatformStoreR929.TABLE_CERTIFICATE, "state='ACTIVE' AND level='L7_CHAMPION'", null) + "）"
                + "；待生命周期投影=" + certificateProjectionPendingSnapshotCP4(report, store)
                + "；当前版本已应用期数=" + countSnapshotCP4(report, store, ResearchMainlineContributionAuditR922.TABLE,
                        "runtime_version=? AND applied=1 AND influence_weight>0", new String[]{AppConfig.R36_RUNTIME_REPAIR_VERSION})
                + "\n校准模型=" + countSnapshotCP4(report, store, CandidateCalibrationR932.MODEL, "1=1", null)
                + "；固定窗口=" + countSnapshotCP4(report, store, FixedResearchWindowR932.WINDOW, "1=1", null)
                + "\n证书还需逐期校验有效期和依赖；N/A表示读取失败，不等于0。\n";
    }
}
