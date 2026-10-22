package com.ethk3.novaonepro;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;

/** Executes production Java predicates. Synthetic fixtures are never represented as live draws. */
public final class R924RuntimeRepairHarness {
    private static int assertions;
    private static final String HASH = "a".repeat(64);

    public static void main(String[] args) throws Exception {
        ReleaseIdentityR817.assertBuildConfig();
        check(ReleaseIdentityR817.VERSION_CODE == ReleaseManifestR926.VERSION_CODE, "IDENTITY");
        frequency();
        streak();
        provenance();
        chunks();
        freezeExpiry();
        trace();
        System.out.println("R923_RUNTIME_REPAIR=PASS|ASSERTIONS=" + assertions);
        System.out.println("SCOPE=PRODUCTION_JAVA_SYNTHETIC_FIXTURES|PHYSICAL_DEVICE=NOT_TESTED|WIN_RATE=NOT_PROVEN");
    }

    private static void frequency() {
        check(Math.abs(573.0 / (573 + 1323) - 0.3022151898734177) < 1e-12, "HIT_DENOMINATOR");
        check(Math.abs(573.0 / 1323 - 0.4331065759637188) < 1e-12, "WIN_LOSS_RATIO");
        ControlledFrequencyDiagnosticR923.Interval interval = ControlledFrequencyDiagnosticR923.wilson(573,1896);
        check(interval.lower > .28 && interval.lower < .29 && interval.upper > .32 && interval.upper < .33,
                "WILSON_573_OF_1896");
        ControlledFrequencyDiagnosticR923.Interval none = ControlledFrequencyDiagnosticR923.wilson(0,0);
        check(none.lower == -1 && none.upper == -1, "NO_SAMPLE_NOT_ZERO_PERCENT");
        for (int n=1;n<=500;n++) for (int k=0;k<=n;k++) {
            ControlledFrequencyDiagnosticR923.Interval x=ControlledFrequencyDiagnosticR923.wilson(k,n);
            check(x.lower>=0 && x.upper<=1 && x.lower<=x.upper, "WILSON_BOUNDS");
        }
        ControlledCandidateOpportunityAuditR922.Report empty = ControlledCandidateOpportunityAuditR922.evaluate(
                new ControlledCandidateOpportunityAuditR922.Opportunity[0],120);
        String report=new ControlledFrequencyDiagnosticR923.Report(empty,empty,empty).summary();
        check(report.contains("targetGap=N/A") && report.contains("Quota=FORBIDDEN"), "EMPTY_FREQUENCY_TRUTH");
        System.out.println("R923_FREQUENCY_ARITHMETIC=PASS|HIT_RATE=30.22%|WIN_LOSS_RATIO=43.31%|WILSON_BOUNDARY_SWEEP=125750");
    }

    private static MissStreakEscalationR923.Observation observation(int issue, int hit) {
        return new MissStreakEscalationR923.Observation("20260905"+String.format("%04d",issue),
                hit,1,1,2,3,hit==1?1:4,HASH,1000L+issue,"ELIGIBLE","exact",
                .44,.03,.82,.71,.60,.05,"EXACT_POST_DRAW","next-generation-only");
    }

    private static void streak() {
        MissStreakEscalationR923.Result zero=MissStreakEscalationR923.evaluate(null);
        check(zero.status.equals("NO_PROVEN_REAL_SAMPLE"), "STREAK_NO_SAMPLE");
        MissStreakEscalationR923.Observation miss=observation(3,0), hit=observation(1,1);
        check(!MissStreakEscalationR923.evaluate(new MissStreakEscalationR923.Observation[]{miss,hit}).escalated(),
                "ONE_MISS_NOT_ESCALATED");
        MissStreakEscalationR923.Result two=MissStreakEscalationR923.evaluate(
                new MissStreakEscalationR923.Observation[]{miss,observation(2,0),hit});
        check(two.escalated() && two.streak==2 && two.failureVector.contains("avgStability=0.820000"),
                "TWO_MISS_VECTOR");
        check(two.detail.contains("不暂停") && two.detail.contains("不能保证下一期命中"), "NO_PAUSE_OR_GUARANTEE");
        check(MissStreakEscalationR923.evaluate(new MissStreakEscalationR923.Observation[]{hit,miss,miss}).streak==0,
                "HIT_RESETS_STREAK");
        check(MissStreakEscalationR923.evaluate(new MissStreakEscalationR923.Observation[]{miss,null,miss}).streak==1,
                "INVALID_BREAKS_NOT_JOINED");
        check(MissStreakEscalationR923.evaluate(new MissStreakEscalationR923.Observation[]{null,miss,miss})
                .status.equals("UNVERIFIED_LATEST_REAL_BOUNDARY"), "INVALID_LATEST_EXPLICIT");
        MissStreakEscalationR923.Observation corrupt=new MissStreakEscalationR923.Observation(
                "202609050003",0,1,1,2,3,1,HASH,1003,"","",.4,.1,.8,.7,.5,.1,"","");
        check(!corrupt.valid(), "ACTUAL_HIT_CONTRADICTION_REJECTED");
        for(int n=2;n<=24;n++) {
            MissStreakEscalationR923.Observation[] observations=new MissStreakEscalationR923.Observation[n];
            for(int i=0;i<n;i++) observations[i]=observation(n-i,0);
            MissStreakEscalationR923.Result r=MissStreakEscalationR923.evaluate(observations);
            check(r.escalated() && r.streak==n && r.failureVector.contains("vectorHash="), "CONTINUOUS_ESCALATION");
        }
        System.out.println("R923_TWO_MISS_FEEDBACK=PASS|THRESHOLD=2|MAX_OBSERVED_WINDOW=24|INVALID_BOUNDARY=BREAK|NO_BET_AUTHORITY=1");
    }

    private static Object fixture() throws Exception {
        Class<?> type=Class.forName("com.ethk3.novaonepro.RealtimeProvenanceRepairR923$Proof");
        Constructor<?> ctor=type.getDeclaredConstructor();ctor.setAccessible(true);Object p=ctor.newInstance();
        for(Field f:type.getDeclaredFields()) {f.setAccessible(true);if(f.getType()==String.class)f.set(p,"");}
        put(p,"id",1L);put(p,"target","202609050001");
        for(String key: new String[]{"tradeAt","betAt","authAt","commitAt","finishedAt"})put(p,key,2000L);
        for(String key: new String[]{"requestAt","canonicalRequestAt","opportunityAt"})put(p,key,1000L);
        put(p,"firstSeenAt",3000L);
        for(String key:new String[]{"authorizationId","selectedAuth","authHash","canonicalAuth"})put(p,key,HASH);
        for(String key:new String[]{"selectedContract","authContract","canonicalContract"})put(p,key,"b".repeat(64));
        for(String key:new String[]{"tradeAction","authorizedMode","selectedAction","canonicalAction","opportunityAction"})put(p,key,"BET");
        for(String key:new String[]{"tradeLane","selectedLane","canonicalLane"})put(p,key,V51Enums.Lane.CONTROLLED_PROBE.name());
        for(String prefix:new String[]{"selected","canonical","opportunity"}) {
            put(p,prefix+"Position",1);put(p,prefix+"N1",1);put(p,prefix+"N2",2);put(p,prefix+"N3",3);
        }
        for(String key:new String[]{"position","n1","preDrawFrozen","canonicalSettled","gateEligible","authorizationValid","contractValid"})put(p,key,1);
        put(p,"n2",2);put(p,"n3",3);put(p,"drawActual",4);put(p,"canonicalActual",4);
        put(p,"statScope",AppConfig.STAT_SCOPE_REAL_TRADE);
        put(p,"contractStatus",ControlledRealContractAuditR922.EXACT_CONTRACT_STATUS);
        put(p,"requestStatus",AnalysisRequestStoreR36.CANONICAL_COMMITTED);
        put(p,"tradeResult",AppConfig.RESULT_MISS);put(p,"rawP",.42);
        put(p,"sourceType",Models.SOURCE_TYPE_UNKNOWN);
        return p;
    }

    private static void put(Object target,String field,Object value) throws Exception {
        Field f=target.getClass().getDeclaredField(field);f.setAccessible(true);f.set(target,value);
    }
    private static boolean proofValid(Object proof) throws Exception {
        Method m=proof.getClass().getDeclaredMethod("valid");m.setAccessible(true);return (Boolean)m.invoke(proof);
    }
    private static void provenance() throws Exception {
        check(proofValid(fixture()),"LEGACY_MISSING_FLAGS_EXACT_PROOF");
        Object[][] bad={{"authorizationId","f".repeat(64)},{"selectedContract","c".repeat(64)},
                {"canonicalN3",4},{"position",0},{"position",11},{"preDrawFrozen",0},
                {"gateEligible",0},{"authorizationValid",0},{"contractValid",0},
                {"firstSeenAt",2000L},{"finishedAt",4000L},{"opportunityAt",2100L},
                {"tradeAt",1900L},{"canonicalRequestAt",1100L},{"requestStatus","FAILED"},
                {"canonicalActual",1},{"tradeHit",1},{"calibratedP",.5},
                {"sourceType","SHADOW"},{"selectedLane","SHADOW"},{"n2",1},
                {"rawP",Double.NaN},{"tradeResult",AppConfig.RESULT_WIN},{"mask",1}};
        for(Object[] defect:bad) {Object p=fixture();put(p,(String)defect[0],defect[1]);check(!proofValid(p),"PROOF_REJECT_"+defect[0]);}
        Object already=fixture();put(already,"sourceType",Models.SOURCE_TYPE_REALTIME);put(already,"realtimeFlag",1);
        check(!proofValid(already), "REPAIR_IDEMPOTENT_ALREADY_CORRECT");
        System.out.println("R923_PROVENANCE_PROOF=PASS|VALID=1|FAULTS_REJECTED="+bad.length+"|IDEMPOTENT=1|NEW_BET=FORBIDDEN");
    }

    private static void chunks() {
        int size=DiagnosticTextExportR923.SAFE_CLIPBOARD_CHARS;
        String full="中".repeat(size-1)+"😀"+"文".repeat(size*6)+"END";
        String[] chunks=DiagnosticTextExportR923.chunks(full);
        check(String.join("",chunks).equals(full), "FULL_TEXT_ROUNDTRIP");
        for(String part:chunks) {
            check(part.length()<=size, "CHUNK_BOUND");
            check(!Character.isHighSurrogate(part.charAt(part.length()-1)), "NO_SPLIT_HIGH_SURROGATE");
            check(!Character.isLowSurrogate(part.charAt(0)), "NO_SPLIT_LOW_SURROGATE");
        }
        check(DiagnosticTextExportR923.chunks(null).length==1,"EMPTY_CHUNK");
        System.out.println("R923_FULL_TEXT_EXPORT=PASS|CHARACTERS="+full.length()+"|CHUNKS="+chunks.length+"|LOSS=0|SURROGATE_SAFE=1");
    }

    private static void freezeExpiry() throws Exception {
        Class<?> ruleType=Class.forName("com.ethk3.novaonepro.DynamicFreezeRealtimeSnapshotR923$Rule");
        Constructor<?> ctor=ruleType.getDeclaredConstructor();ctor.setAccessible(true);Object rule=ctor.newInstance();
        put(rule,"untilIssue","202609050002");put(rule,"untilTime",2000L);
        Method expiry=DynamicFreezeRealtimeSnapshotR923.class.getDeclaredMethod("expired",ruleType,String.class,long.class);
        expiry.setAccessible(true);
        check(!(Boolean)expiry.invoke(null,rule,"202609050002",2000L),"EXPIRY_INCLUSIVE");
        check((Boolean)expiry.invoke(null,rule,"202609050003",1900L),"ISSUE_EXPIRES");
        check((Boolean)expiry.invoke(null,rule,"202609050001",2001L),"TIME_EXPIRES");
        System.out.println("R923_FREEZE_EXPIRY=PASS|TIME_AND_ISSUE_BOUNDARIES=3");
    }
    private static void trace() {
        RuntimeTraceR36 trace=new RuntimeTraceR36("attempt","202609050001",1000L);
        RuntimeTraceR36.Mark mark=trace.begin("FEATURES_SUB");trace.end(mark,true,"");
        check(trace.compactStagesR923().contains("INCLUSIVE_NOT_ADDITIVE")
                && trace.compactStagesR923().contains("FEATURES_SUB="),"STAGE_TRACE_NO_FAKE_SUM");
    }
    private static void check(boolean ok,String code) {assertions++;if(!ok)throw new AssertionError(code);}
}
