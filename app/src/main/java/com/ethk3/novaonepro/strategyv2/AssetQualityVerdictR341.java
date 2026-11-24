package com.ethk3.novaonepro.strategyv2;

import java.util.LinkedHashMap;
import java.util.Map;

/** Read-only projection. Neither a missing metric nor this projection changes V1 authority. */
public final class AssetQualityVerdictR341 {
    private AssetQualityVerdictR341() {}
    public static final String VERSION="R341_PER_ASSET_QUALITY_1";

    static Map<String,Object> evaluate(CertificateGateR27.Verdict v1,GateV2R29.Evidence evidence,
            SequentialQualificationR27.State sequence,boolean prefixConflict,boolean issuanceEligible){
        Map<String,Object> value=new LinkedHashMap<>(),checks=new LinkedHashMap<>();
        boolean calendarKnown=evidence.registeredOrdinal>=0&&evidence.registrationOrigin!=null;
        boolean supportAligned=sequence.acts==evidence.settledActs&&sequence.hits==evidence.hits
                &&evidence.p.length==evidence.settledActs&&evidence.y.length==evidence.settledActs;
        boolean complete=calendarKnown&&!prefixConflict&&evidence.opportunities>0
                &&evidence.frozen==evidence.opportunities&&evidence.missingFrozen==0&&evidence.lateFrozen==0
                &&evidence.pendingActs==0&&sequence.n==evidence.opportunities&&supportAligned;
        Map<String,Object> future=CertificateGateR27.futureSummary(evidence.opportunities,evidence.settledActs,evidence.hits);
        value.put("version",VERSION);value.put("experiment",sequence.policyHash);value.put("originalPrefixSha256",evidence.inputHash);
        value.put("registrationOrigin",evidence.registrationOrigin);value.put("registeredFirst",calendarKnown?evidence.registeredOrdinal:null);
        value.put("auditedThrough",calendarKnown?evidence.latestOrdinal:null);value.put("completeOriginalCalendar",complete);
        value.put("frozen",evidence.frozen);value.put("late",evidence.lateFrozen);value.put("missing",evidence.missingFrozen);
        value.put("pendingActs",evidence.pendingActs);value.put("validActDecisions",evidence.actDecisions);value.put("future",future);
        value.put("v1MathematicalGatesPassed",v1.active);value.put("v1ActualIssuancePreconditionsMet",issuanceEligible);
        value.put("marketUseAudit","REQUIRES_ORIGINAL_DEPLOYMENT_OBSERVATION");
        value.put("modelConfidenceQualityTarget",.37);
        value.put("iidStreakGateSensitivity",CertificateGateR27.streakGateSensitivityR31(250,new double[]{.34,.37,.40}));
        value.put("checks",checks);
        checks.put("Q01",measurement(evidence.settledActs<CertificateGateR27.MIN_ACTS||!supportAligned?"INSUFFICIENT"
            :evidence.hits/(double)evidence.settledActs>=CertificateGateR27.MIN_HR?"PASS":"FAIL",future.get("hitRate"),"SETTLED_INDEPENDENT_ACT"));
        checks.put("Q02",measurement("UNKNOWN",null,"DEPLOYED_REAL_ACT_REQUIRES_SEPARATE_ORIGINAL_LEDGER"));
        checks.put("Q03",measurement(!complete?"INSUFFICIENT":state(v1,"sequentialTest"),sequence.logUp,"ORIGINAL_E_PROCESS_AND_PERMANENT_ALPHA"));
        checks.put("Q04",measurement(!complete?"INSUFFICIENT":combine(state(v1,"worst720Block>=0.26"),state(v1,"losingStreak<=6")),
            v1.gates.get("worst720Block>=0.26"),"ORIGINAL_V1_BLOCK_AND_LOSING_STREAK"));
        GateV2R29.Calibration calibration=GateV2R29.calibration(evidence.p,evidence.y,evidence.p.length);
        Map<String,Object> calibrationResult=calibration.json();
        long valid=0;double forecastBrier=0,referenceBrier=0;
        if(evidence.p.length==evidence.y.length)for(int i=0;i<evidence.p.length;i++){
            double probability=evidence.p[i];int hit=evidence.y[i];
            if(!Double.isFinite(probability)||probability<0||probability>1||(hit!=0&&hit!=1))continue;
            forecastBrier+=(probability-hit)*(probability-hit);referenceBrier+=(.3-hit)*(.3-hit);valid++;
        }
        boolean calibrationComplete=valid>0&&valid==evidence.settledActs&&evidence.settledActs==evidence.p.length;
        calibrationResult.put("brier",calibrationComplete?forecastBrier/valid:null);
        calibrationResult.put("bss",calibrationComplete&&referenceBrier>0?1-forecastBrier/referenceBrier:null);
        checks.put("Q05",measurement(calibration.status,calibrationResult,"ACT_EVENT_SPECIFIC_FROZEN_PREDICTIONS"));
        checks.put("Q06",measurement("UNKNOWN",null,"INDEPENDENT_PARENT_CHILD_ATTESTATION_REQUIRED"));
        checks.put("Q07",measurement("RECORDED_UNVALIDATED",issuanceEligible,"READ_ACTUAL_CERTIFICATE_STATUS_FROM_THE_AUTHORITY_TABLE"));
        checks.put("Q08",measurement("SEPARATE_PROTOCOL",null,"REGISTERED_FULL_FACTORY_SYNTHETIC_VERIFICATION"));
        value.put("naturalAssessment",!complete?"INCOMPLETE_CALENDAR_OR_SUPPORT":!v1.active||!issuanceEligible
            ?"GATES_OR_ISSUANCE_PRECONDITIONS_NOT_MET":"PER_ASSET_EXTERNAL_EVIDENCE_REVIEW_REQUIRED");
        return value;
    }

    private static String state(CertificateGateR27.Verdict verdict,String key){
        Object gate=verdict.gates.get(key);
        return gate instanceof Map?String.valueOf(((Map<?,?>)gate).get("state")):"UNKNOWN";
    }
    private static String combine(String first,String second){
        if("FAIL".equals(first)||"FAIL".equals(second))return "FAIL";
        return "PASS".equals(first)&&"PASS".equals(second)?"PASS":"UNKNOWN";
    }
    private static Map<String,Object> measurement(String state,Object original,String scope){
        Map<String,Object> out=new LinkedHashMap<>();out.put("state",state);out.put("value",original);out.put("scope",scope);return out;
    }
}
