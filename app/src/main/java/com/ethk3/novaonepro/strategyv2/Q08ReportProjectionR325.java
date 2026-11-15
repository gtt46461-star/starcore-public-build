package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/** Read-only producer-receipt bridge. A count of 120 or a cached PASS cannot
 * satisfy Q08. Original registry, fixed experiments and REAL gates are unchanged. */
public final class Q08ReportProjectionR325 {
    private Q08ReportProjectionR325() {}
    private static final String REGISTRY="R31_RISK_REGISTRY_1";
    private static final Set<String> ARMS=new LinkedHashSet<>(Arrays.asList("NULL_UNIFORM","NULL_TIME_SHUFFLED","SIGNAL","SIGNAL_DISABLED","DRIFT","GAPS"));
    private static Map<?,?> map(Object x){return x instanceof Map?(Map<?,?>)x:Collections.emptyMap();}
    private static List<?> list(Object x){return x instanceof List?(List<?>)x:Collections.emptyList();}
    private static boolean hash(Object x){return x instanceof String&&((String)x).matches("[0-9a-f]{64}");}
    private static Long count(Object x){if(!(x instanceof Number))return null;double d=((Number)x).doubleValue();return Double.isFinite(d)&&d>=0&&d==Math.rint(d)&&d<Long.MAX_VALUE?((Number)x).longValue():null;}
    private static Double number(Object x){return x instanceof Number&&Double.isFinite(((Number)x).doubleValue())?((Number)x).doubleValue():null;}
    private static boolean n(Object x,long wanted){Long v=count(x);return v!=null&&v==wanted;}
    private static boolean close(Object a,double b){Double x=number(a);return x!=null&&Math.abs(x-b)<=1e-12;}
    private static String digest(Object x){return StatsR27.sha256(JsonR27.write(x));}
    private static Map<String,Object> copy(Map<?,?> x){Map<String,Object> out=new LinkedHashMap<>();for(Map.Entry<?,?> e:x.entrySet())out.put(String.valueOf(e.getKey()),e.getValue());return out;}
    private static boolean source(Map<?,?> view,Map<?,?> payload,String lane,Object source){return source instanceof String&&!((String)source).isEmpty()&&lane.equals(view.get("source"))&&source.equals(payload.get("sourceCommit"));}
    private static void result(Map<String,Object> row,String status,String reason){row.put("status",status);row.put("missingReason",reason);}

    /** Runs on the detached current report input, never on live mutable state. */
    static void apply(ReportR27.R30Input in,Map<String,Object> row){
        row.put("role","SYNTHETIC");
        Map<?,?> view=map(in.data.get("factoryCapabilityExperiment")),f=map(view.get("payload"));
        Map<?,?> nullView=map(in.data.get("syntheticControl")),nulls=map(nullView.get("payload"));
        Map<?,?> life=map(map(in.data.get("lifecycleEvidenceR31")).get("evidenceContractR31")),r=map(life.get("value"));
        Map<?,?> originals=map(in.data.get("q08FactoryOriginalsR325"));
        Map<String,Object> observations=new LinkedHashMap<>();observations.put("factoryCapabilityExperiment",view);observations.put("syntheticControl",nullView);observations.put("registeredFactoryValidation",life);observations.put("originalFactoryTrialSet",originals);row.put("observations",observations);
        row.put("projectionVersion","R32_5_Q08_PRODUCER_RECEIPT_BRIDGE_1");
        row.put("measurementValueScope","COMPLETED_ORIGINAL_FACTORY_TRIALS_DIVIDED_BY_120; COMPLETENESS_IS_NOT_STATISTICAL_PASS");
        row.put("realPermission",false);row.put("naturalQualityEligible",false);
        row.put("receiptAuthority","REPORT_ONLY; ORIGINAL_REGISTERED_PRODUCERS_AND_V1_CERTIFICATE_GATES_UNCHANGED");
        Object src=in.watermark.get("sourceCommit");
        String state=String.valueOf(f.get("status"));
        if(source(view,f,"r30/q08-factory-r31",src)&&("FAULTED".equals(state)||"FACTORY_TRANSPORT_FAILED".equals(state))){result(row,"FAIL","当前源码完整工厂已有执行/传输失败；保留failedTrials/transportFailure，其他缺证不得遮盖。");row.put("producerFailure",state);return;}
        if(!"READ_OK_NOT_A_HEALTH_VERDICT".equals(life.get("status"))||!Boolean.TRUE.equals(life.get("readOnly"))||r.isEmpty()){result(row,"UNKNOWN_REGISTERED_VALIDATION_SOURCE","缺少同源只读风险注册表投影；120条缓存记录不能替代原统计验证。");return;}
        if(!(src instanceof String)||!src.equals(r.get("sourceCommit"))){result(row,"UNKNOWN_STALE_SYNTHETIC_SOURCE","原注册回执不是当前源码；旧源实验不能签当前Q08。");return;}
        String problem=registryProblem(life,r,src);
        if(problem!=null){result(row,"DATA_CONFLICT","Q08原注册回执不一致："+problem);row.put("producerReceiptConflict",problem);return;}
        if(Boolean.TRUE.equals(r.get("revoked"))||"REVOKED".equals(r.get("state"))){result(row,"FAIL","原注册设计验证已撤销；保留撤销原因，不授予真实资格。");return;}
        Long completed=count(r.get("completedOriginalTrials"));
        if(completed==null){result(row,"INSUFFICIENT","原注册已建立但尚无首个工厂原件；等待原生产者提交，不能由展示缓存补造。");return;}
        if(completed>120||!n(r.get("plannedTrials"),120)||!"NOT_PROVEN_SYNTHETIC_ONLY".equals(r.get("naturalQuality"))){result(row,"DATA_CONFLICT","原注册验证的计划数、完成数或权限范围不一致。");return;}
        row.put("value",completed/120.0);row.put("numerator",completed);row.put("denominator",120L);row.put("sampleCount",completed);row.put("population","REGISTERED_FACTORY_ARM_SEED_ORIGINAL_TRIALS_6_X_20");
        if(!"READ_OK_NOT_A_HEALTH_VERDICT".equals(originals.get("status"))||!Boolean.TRUE.equals(originals.get("readOnly"))){result(row,"UNKNOWN_FACTORY_ORIGINALS_MISSING","独立原始trial记录缺失、不可读或超过原有120条/256KiB边界；不从计数推断完成。");return;}
        if(!"r27_chart_cache".equals(originals.get("sourceTable"))||!"r31-risk-contract-trial".equals(originals.get("panel"))||!n(originals.get("maximumRows"),120)||!n(originals.get("maximumBytes"),262144)||!readScope(originals)){result(row,"DATA_CONFLICT","工厂原件读取的来源、容量合同或时间范围缺失；展示列表不能充当原trial账。");return;}
        if(!Objects.equals(r.get("recordSha256"),originals.get("registryRecordSha256"))||!Objects.equals(src,originals.get("sourceCommit"))||!Objects.equals(r.get("binding"),originals.get("binding"))||!Objects.equals(r.get("protocolHash"),originals.get("protocolHash"))){result(row,"UNKNOWN_SYNTHETIC_READ_SCOPE_MISMATCH","原始trial读取与注册回执不是同一绑定；保留读取范围并重采，禁止拼接PASS。");return;}
        Map<?,?> originalRows=map(originals.get("rows"));
        if(originalRows.size()!=completed||!n(originals.get("rowCount"),completed)||!hash(r.get("validationOriginalsSha256"))||!r.get("validationOriginalsSha256").equals(digest(originalRows))||!Objects.equals(r.get("validationOriginalsSha256"),originals.get("originalsSha256"))){result(row,"UNKNOWN_SYNTHETIC_READ_SCOPE_MISMATCH","trial原件集合与原注册验证摘要不一致，可能是分次读取期间的新提交；必须重采一致集合。");return;}
        problem=factoryCellsProblem(originalRows,r,observations);
        if(problem!=null){result(row,"DATA_CONFLICT","完整工厂逐臂原始记录冲突："+problem);return;}
        Long failed=count(r.get("failedTrials"));
        if(failed==null){result(row,"DATA_CONFLICT","原注册验证未提供工厂失败数。");return;}
        if(failed>0){result(row,"FAIL","完整工厂包含失败试验，禁止把完整性计数当统计通过。");return;}
        if(completed<120){result(row,"INSUFFICIENT","原6臂×20种子尚未完成；真实进度、CI/功效和自然质量分别报告。");return;}
        Object reasons=r.get("validationReasons");
        if(!(reasons instanceof List)||!(r.get("validationPassed") instanceof Boolean)){result(row,"DATA_CONFLICT","原设计验证状态或拒绝原因缺失。");return;}
        if(!Boolean.TRUE.equals(r.get("validationPassed"))){result(row,"FAIL","原完整设计统计验证未通过；保留falseAlarmUpper95、signalPowerLower95与全部validationReasons。");return;}
        Map<?,?> contract=map(map(r.get("registration")).get("contract"));
        Double far=number(r.get("falseAlarmUpper95")),power=number(r.get("signalPowerLower95"));
        if(!"ACTIVE".equals(r.get("state"))||!((List<?>)reasons).isEmpty()||!hash(r.get("validationOriginalsSha256"))||!n(r.get("nullTrials"),60)||!n(r.get("signalTrials"),20)
                ||far==null||power==null||far<0||far>1||power<0||power>1
                ||!close(contract.get("errorBudget"),.15)||!close(contract.get("powerTarget"),.70)||far>.15||power<.70){result(row,"DATA_CONFLICT","生产者通过声明与注册CI/功效、原件集合或权限状态冲突。");return;}
        if(!source(nullView,nulls,"r30/q08",src)){result(row,"RECORDED_UNVALIDATED","完整工厂原设计验证通过；仍缺当前源码34/37/40%功效与空信号独立实验原结果。");return;}
        if(!"READ_OK_NOT_A_HEALTH_VERDICT".equals(nullView.get("status"))||!Boolean.TRUE.equals(nullView.get("readOnly"))||!readScope(nullView)||!digest(nulls).equals(nullView.get("originalSha256"))){result(row,"RECORDED_UNVALIDATED","原功效/空信号结果未由固定主键只读完整采集，或其原始JSON摘要不一致。");return;}
        problem=nullSuiteProblem(nulls);
        if(problem!=null){row.put("pendingSyntheticEvidence",problem);result(row,"RECORDED_UNVALIDATED","完整工厂原验证通过；原功效/空信号报告尚未完整一致："+problem);return;}
        Map<String,Object> uncertainty=new LinkedHashMap<>();
        uncertainty.put("factoryNullFalseAlarmUpper95",r.get("falseAlarmUpper95"));uncertainty.put("factorySignalPowerLower95",r.get("signalPowerLower95"));
        uncertainty.put("factoryDecisionMethod","ORIGINAL_REGISTERED_ONE_SIDED_95_CLOPPER_PEARSON; FAR_UPPER_LE_0.15; SIGNAL_POWER_LOWER_GE_0.70");
        uncertainty.put("perArmDescriptiveIntervals",observations.get("perArmDescriptiveIntervals"));uncertainty.put("legacyPowerByOriginalEdge",nulls.get("power"));
        uncertainty.put("legacyFalseQualificationPerCompletedPipelineInterval95",nulls.get("falseQualificationPerCompletedPipelineInterval95"));uncertainty.put("legacyFalseQualificationGivenEligibleInterval95",nulls.get("falseQualificationGivenEligibleInterval95"));
        row.put("uncertainty",uncertainty);
        row.put("validatedProducerReceiptSha256",r.get("recordSha256"));
        result(row,"PASS","当前源码原6×20工厂验证通过，原34/37/40%功效与空信号结果完整一致；仅报告原合成证据，不代表自然优势或REAL许可。");
    }
    private static boolean readScope(Map<?,?> view){
        Long started=count(view.get("readStartedAt")),finished=count(view.get("readFinishedAt"));
        return started!=null&&finished!=null&&finished>=started&&Arrays.asList("PINNED_READ_ONLY_TRANSACTION","SEPARATE_LIFECYCLE_READS_AFTER_FACT_WATERMARK").contains(view.get("sourceScope"));
    }
    private static String registryProblem(Map<?,?> life,Map<?,?> value,Object source){
        if(!source.equals(value.get("sourceCommit"))||!REGISTRY.equals(value.get("version")))return "SOURCE_OR_REGISTRY_VERSION";
        if(!hash(life.get("originalProjectionSha256"))||!digest(value).equals(life.get("originalProjectionSha256")))return "PROJECTION_HASH";
        Map<?,?> registration=map(value.get("registration")),contract=map(registration.get("contract"));
        if(!hash(value.get("registrationSha256"))||!digest(registration).equals(value.get("registrationSha256")))return "REGISTRATION_HASH";
        for(String field:new String[]{"sourceCommit","binding","protocolHash","registeredAt"})if(!Objects.equals(value.get(field),registration.get(field)))return "REGISTRATION_"+field;
        if(!REGISTRY.equals(registration.get("version"))||!"COMMITTED_BEFORE_FIRST_FACTORY_STEP".equals(registration.get("registrationBoundary"))
                ||!Boolean.FALSE.equals(registration.get("realPermission"))||!Boolean.FALSE.equals(value.get("realPermission"))||!"R27_GATE_V1".equals(value.get("currentRealAuthority"))
                ||!hash(value.get("contractHash"))||!value.get("contractHash").equals(contract.get("hash"))||!hash(value.get("protocolHash"))||!"R31_FULL_FACTORY_RISK_V1".equals(contract.get("id")))return "REGISTRATION_CONTRACT";
        String binding=StatsR27.sha256(REGISTRY+"|"+source+"|"+value.get("contractHash")+"|"+value.get("protocolHash"));if(!binding.equals(value.get("binding")))return "REGISTRATION_BINDING";
        Long at=count(value.get("registeredAt")),updated=count(value.get("updatedAt")),started=count(life.get("readStartedAt")),finished=count(life.get("readFinishedAt"));
        if(at==null||at==0||updated==null||updated<at||started==null||finished==null||finished<started||updated>finished)return "READ_TIMES";
        if(!Arrays.asList("PINNED_READ_ONLY_TRANSACTION","SEPARATE_LIFECYCLE_READS_AFTER_FACT_WATERMARK").contains(life.get("sourceScope")))return "READ_SCOPE";
        // report() overlays a durable revocation on the CURRENT pointer without
        // rewriting that pointer's original hash. This is a negative-only path:
        // authenticate the bound event and return FAIL, never a new PASS.
        if(Boolean.TRUE.equals(value.get("revoked"))){
            Map<?,?> event=map(value.get("revocation"));Long revokedAt=count(event.get("at"));
            return Objects.equals(value.get("binding"),event.get("binding"))&&"REVOKED".equals(event.get("state"))&&"REVOKED".equals(value.get("state"))&&revokedAt!=null&&revokedAt>=at&&revokedAt<=finished?null:"REVOCATION_EVENT";
        }
        Map<String,Object> original=copy(value);for(String field:new String[]{"recordSha256","registration","registrationSha256","revoked","revocation","opportunities"})original.remove(field);
        return !hash(value.get("recordSha256"))||!digest(original).equals(value.get("recordSha256"))?"ORIGINAL_STATE_HASH":null;
    }
    private static String factoryCellsProblem(Map<?,?> rows,Map<?,?> registry,Map<String,Object> observations){
        Set<String> cells=new HashSet<>();Map<String,Integer> detected=new HashMap<>(),counts=new HashMap<>();int failures=0;
        for(Map.Entry<?,?> entry:rows.entrySet()){Map<?,?> r;
            try{if(!(entry.getValue() instanceof String))return "ORIGINAL_RECORD_NOT_TEXT";r=map(JsonR27.parse((String)entry.getValue()));}catch(RuntimeException invalid){return "MALFORMED_ORIGINAL";}
            String arm=String.valueOf(r.get("arm"));Long seed=count(r.get("seed"));
            if(!ARMS.contains(arm)||seed==null||seed<710000||seed>=710020||!cells.add(arm+":"+seed))return "FOREIGN_OR_DUPLICATE_ARM_SEED";
            if(!Objects.equals(registry.get("sourceCommit"),r.get("sourceCommit"))||!Objects.equals(registry.get("protocolHash"),r.get("protocolHash"))||!Objects.equals(registry.get("binding"),r.get("binding"))
                    ||!Objects.equals(entry.getKey(),registry.get("binding")+":"+arm+":"+seed)||!Boolean.FALSE.equals(r.get("realPermission"))||!hash(r.get("candidateLedgerSha256"))||!hash(r.get("artifactCellSha256")))return "TRIAL_IDENTITY_OR_ORIGINAL_REFERENCE";
            Map<String,Object> original=copy(r);original.remove("recordSha256");if(!digest(original).equals(r.get("recordSha256")))return "TRIAL_RECORD_HASH";
            if(!Arrays.asList("DETECTED","NOT_DETECTED","FAILED").contains(r.get("status")))return "TRIAL_STATUS";
            counts.put(arm,counts.getOrDefault(arm,0)+1);if("DETECTED".equals(r.get("status")))detected.put(arm,detected.getOrDefault(arm,0)+1);if("FAILED".equals(r.get("status")))failures++;
        }
        int nulls=0,alarms=0;for(String arm:Arrays.asList("NULL_UNIFORM","NULL_TIME_SHUFFLED","SIGNAL_DISABLED")){nulls+=counts.getOrDefault(arm,0);alarms+=detected.getOrDefault(arm,0);}
        if(!n(registry.get("nullTrials"),nulls)||!n(registry.get("falseAlarms"),alarms)||!n(registry.get("signalTrials"),counts.getOrDefault("SIGNAL",0))||!n(registry.get("signalDetections"),detected.getOrDefault("SIGNAL",0))||!n(registry.get("failedTrials"),failures))return "REGISTRY_ORIGINAL_OUTCOME_COUNTS";
        List<Object> groups=new ArrayList<>();for(String arm:ARMS){Map<String,Object> group=new LinkedHashMap<>();int hits=detected.getOrDefault(arm,0),total=counts.getOrDefault(arm,0);group.put("arm",arm);group.put("events",hits);group.put("independentReplicates",total);double[] ci=total==0?null:StatsR27.wilson(hits,total,StatsR27.Z95);group.put("lower95",ci==null?null:ci[0]);group.put("upper95",ci==null?null:ci[1]);group.put("method","WILSON_TWO_SIDED_95_DESCRIPTIVE");group.put("scope","RECOMPUTED_DESCRIPTION_OF_ORIGINAL_TRIAL_OUTCOMES; ORIGINAL_REGISTERED_CP_DECISION_UNCHANGED");groups.add(group);}
        observations.put("perArmDescriptiveIntervals",groups);return null;
    }
    private static String nullSuiteProblem(Map<?,?> s){
        if(!"MEASURED".equals(s.get("status"))||!"R30_REPAIR_Q08_INDEPENDENT_INDEX1_V2".equals(s.get("protocol")))return "CURRENT_PROTOCOL_NOT_COMPLETE";
        if(!n(s.get("plannedNullTrials"),20)||!n(s.get("productionTrialBudget"),20)||!n(s.get("completedNullTrials"),20)||!Boolean.FALSE.equals(s.get("naturalQualityEligible"))
                ||!Boolean.FALSE.equals(s.get("mainAlphaBudgetSpent"))||!Boolean.FALSE.equals(s.get("originalAlphaReset"))||!"FIXED_REGISTERED_TRIAL_COUNT_NO_STOP_AFTER_FAVOURABLE_RESULT".equals(s.get("earlyStoppingRule")))return "NULL_TRIAL_COUNT_OR_ROLE";
        Set<Double> edges=new HashSet<>();for(Object raw:list(s.get("power"))){Map<?,?> r=map(raw);Double edge=number(r.get("trueHitProbability"));Long detected=count(r.get("detected")),notBetter=count(r.get("notBetter")),censored=count(r.get("censoredAt5000"));
            if(edge==null||!Arrays.asList(.34,.37,.40).contains(edge)||!edges.add(edge)||!n(r.get("experiments"),20)||detected==null||notBetter==null||censored==null||detected>20||notBetter>20||censored>20||detected+notBetter+censored!=20)return "POWER_SUPPORT_OR_EDGE";
            if(list(r.get("detectionPeriods")).size()!=detected||!interval(map(r.get("detectionProbabilityInterval95")),detected,20))return "POWER_ORIGINALS_OR_INTERVAL";
            for(Object t:list(r.get("detectionPeriods"))){Long p=count(t);if(p==null||p<1||p>5000)return "POWER_PERIODS";}
        }if(edges.size()!=3)return "POWER_GRID_INCOMPLETE";
        Set<Long> ids=new HashSet<>();int eligible=0,falseAlarms=0;
        for(Object raw:list(s.get("fullPipelineTrials"))){Map<?,?> r=map(raw);Long trial=count(r.get("trial"));
            if(trial==null||trial>=20||!ids.add(trial)||!"SYNTHETIC_RANDOM_PERMUTATION_ONLY".equals(r.get("mode"))||!Boolean.FALSE.equals(r.get("naturalQualityEligible"))||!(r.get("falseSequentialQualification") instanceof Boolean))return "NULL_TRIAL_IDENTITY";
            if("MEASURED".equals(r.get("status"))){eligible++;Long p=count(r.get("futurePeriods"));if(p==null||p<1||p>6000||!hash(r.get("policyHash")))return "NULL_TRIAL_SUPPORT";if(Boolean.TRUE.equals(r.get("falseSequentialQualification")))falseAlarms++;}
            else if(!"NO_ELIGIBLE_CANDIDATE".equals(r.get("status"))||!n(r.get("futurePeriods"),0)||!Boolean.FALSE.equals(r.get("falseSequentialQualification")))return "NULL_TRIAL_STATE";
        }
        if(ids.size()!=20||!n(s.get("eligibleNullCandidates"),eligible)||!n(s.get("noEligibleCandidates"),20-eligible)||(eligible==0?s.get("falseSequentialQualifications")!=null:!n(s.get("falseSequentialQualifications"),falseAlarms)))return "NULL_AGGREGATE";
        return interval(map(s.get("falseQualificationPerCompletedPipelineInterval95")),falseAlarms,20)&&interval(map(s.get("falseQualificationGivenEligibleInterval95")),falseAlarms,eligible)?null:"NULL_INTERVAL";
    }
    private static boolean interval(Map<?,?> r,long hits,long total){
        if(!n(r.get("events"),hits)||!n(r.get("independentReplicates"),total))return false;
        if(total==0)return "INSUFFICIENT".equals(r.get("status"))&&r.get("rate")==null&&r.get("lower95")==null&&r.get("upper95")==null;
        if(hits<0||hits>total)return false;double[] ci=StatsR27.wilson(hits,total,StatsR27.Z95);
        return "WILSON_TWO_SIDED_95_DESCRIPTIVE".equals(r.get("method"))&&"DESCRIPTIVE_SYNTHETIC_ONLY".equals(r.get("status"))&&close(r.get("rate"),hits/(double)total)&&close(r.get("lower95"),ci[0])&&close(r.get("upper95"),ci[1]);
    }
}
