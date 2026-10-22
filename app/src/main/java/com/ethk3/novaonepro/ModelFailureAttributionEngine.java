package com.ethk3.novaonepro;

/** Deterministic responsibility classification for V48 model and pipeline failures. */
public final class ModelFailureAttributionEngine {
    public static final String MODULE_VERSION = "V48_MODEL_FAILURE_ATTRIBUTION_20260721_48072148";

    public static final class Result {
        public String primary = "--", owner = "--", evidence = "--", action = "--";

        public String summary() {
            return "primary="
                    + primary
                    + "|owner="
                    + owner
                    + "|evidence="
                    + evidence
                    + "|action="
                    + action;
        }
    }

    private ModelFailureAttributionEngine() {}

    public static Result diagnose(
            boolean identityOk,
            int observations,
            double rate,
            double wilson,
            double predicted,
            double r1,
            double r2,
            double r3,
            double r5,
            int completeIssues,
            String runtimeStatus,
            String championStatus) {
        Result x = new Result();
        if (!identityOk) {
            x.primary = AppConfig.V48_ROOT_IDENTITY;
            x.owner = "V48BuildManifest/BuildIdentity";
            x.evidence = "betCompatible=0";
            x.action = "统一全部活动身份并禁止旧Cohort参与实时链";
            return x;
        }
        if (completeIssues <= 0) {
            x.primary = AppConfig.V48_ROOT_OOS_WRITE;
            x.owner = "OosWriteAudit/OosActionLog";
            x.evidence = "最终40完整期=0";
            x.action = "核对inserted/duplicate/failed并保留SQLite错误";
            return x;
        }
        if (observations <= 0) {
            x.primary = AppConfig.V48_ROOT_DIAGNOSIS;
            x.owner = "DiagnosticObservation/Settlement";
            x.evidence = "Rank1诊断观察=0";
            x.action = "每期身份异常也必须写一条诊断观察";
            return x;
        }
        if (runtimeStatus != null && runtimeStatus.contains(AppConfig.V48_ROOT_RUNTIME)) {
            x.primary = AppConfig.V48_ROOT_RUNTIME;
            x.owner = "RuntimeFreshnessGuard";
            x.evidence = runtimeStatus;
            x.action = "按期号+墙钟+generation阻断陈旧授权";
            return x;
        }
        if (r5 > r1 + 0.08d || r2 > r1 + 0.08d || r3 > r1 + 0.08d) {
            x.primary = AppConfig.V48_ROOT_RANK;
            x.owner = "FinalCandidateRanking/Model";
            x.evidence = "R1=" + r1 + ",R2=" + r2 + ",R3=" + r3 + ",R5=" + r5;
            x.action = "执行阶段排名差异和特征消融，未修复前只SHADOW";
            return x;
        }
        if (predicted > rate + 0.05d) {
            x.primary = AppConfig.V48_ROOT_CALIBRATION;
            x.owner = "RankConditionalCalibration";
            x.evidence = "pred=" + predicted + ",actual=" + rate;
            x.action = "按每目标期一条Rank1样本分层收缩校准";
            return x;
        }
        if (observations >= 30 && rate < AppConfig.RANDOM_K3_BASELINE) {
            x.primary = AppConfig.V48_ROOT_MODEL;
            x.owner = "Champion/FeatureModel";
            x.evidence = "rate=" + rate + ",wilson=" + wilson;
            x.action = "Challenger权重保持0，重新训练并按真实唯一K3动作验收";
            return x;
        }
        if (!"CHAMPION".equals(championStatus)) {
            x.primary = "MODEL_NOT_PROVEN";
            x.owner = "WalkForwardChampionEngine";
            x.evidence = "status=" + championStatus + ",n=" + observations;
            x.action = "继续严格走前和现场观察，不降低门槛制造出手";
            return x;
        }
        x.primary = "MODEL_ENGINEERING_OK";
        x.owner = "V48";
        x.evidence = "rate=" + rate + ",wilson=" + wilson;
        x.action = "继续监控四时间块和覆盖率";
        return x;
    }
}
