package com.ethk3.novaonepro.strategyv2;

import java.util.*;

/** Production consumer boundary tests. Trial outcomes are explicitly test
 * fixtures, NOT executions of 120 research experiments or quality evidence. */
public final class Q08ReportBoundaryProbe {
    static int checks,failures;
    static final String SRC="a".repeat(40);
    static Map<String,Object> m(Object... kv){Map<String,Object> r=new LinkedHashMap<>();for(int i=0;i<kv.length;i+=2)r.put((String)kv[i],kv[i+1]);return r;}
    @SuppressWarnings("unchecked") static Map<String,Object> map(Object x){return x instanceof Map?(Map<String,Object>)x:new LinkedHashMap<>();}
    @SuppressWarnings("unchecked") static List<Object> list(Object x){return (List<Object>)x;}
    static String hash(Object x){return StatsR27.sha256(JsonR27.write(x));}
    static void check(boolean ok,String name){checks++;if(!ok){failures++;System.out.println("FAIL "+name);}}
    static Map<String,Object> definition(String id){for(Map<String,Object> r:MetricDefinitionsR29.r31Definitions())if(id.equals(r.get("id")))return r;throw new AssertionError(id);}
    static final class Fixture {
        final EvidenceContractStoreR31.Registration registration=new EvidenceContractStoreR31.Registration(SRC,1000L);
        final SortedMap<String,String> rows=new TreeMap<>();
        final Map<String,Object> registry=EvidenceContractStoreR31.status(registration,"ACTIVE","COMPLETE_REGISTERED_FULL_FACTORY_VALIDATION",2000L);
        final Map<String,Object> life=m("readOnly",true,"sourceScope","PINNED_READ_ONLY_TRANSACTION","readStartedAt",10000L,"readFinishedAt",10001L,"status","READ_OK_NOT_A_HEALTH_VERDICT","value",registry);
        final Map<String,Object> originals=m("readOnly",true,"sourceScope","PINNED_READ_ONLY_TRANSACTION","readStartedAt",10002L,"readFinishedAt",10003L,"status","READ_OK_NOT_A_HEALTH_VERDICT","sourceTable","r27_chart_cache","panel","r31-risk-contract-trial","maximumRows",120L,"maximumBytes",262144L,"rows",rows);
        final Map<String,Object> factory=m("sourceCommit",SRC,"status","COMPLETE_MEASURED_REQUIRES_REGISTERED_POWER_ASSESSMENT");
        final Map<String,Object> nulls=nullSummary();
        final Map<String,Object> nullView=m("source","r30/q08","readOnly",true,"status","READ_OK_NOT_A_HEALTH_VERDICT","sourceScope","PINNED_READ_ONLY_TRANSACTION","readStartedAt",10004L,"readFinishedAt",10005L,"payload",nulls);
        final Map<String,Object> data=m("factoryCapabilityExperiment",m("source","r30/q08-factory-r31","payload",factory),"syntheticControl",nullView,"lifecycleEvidenceR31",m("evidenceContractR31",life),"q08FactoryOriginalsR325",originals);
        Fixture(){
            for(ResearchCapabilityExperimentR31.Arm arm:ResearchCapabilityExperimentR31.Arm.values())for(long seed=710000;seed<710020;seed++){
                Map<String,Object> ledger=m("fixtureOnly",true,"arm",arm.name(),"seed",seed);
                Map<String,Object> artifact=m("sourceCommit",SRC,"protocolHash",registration.protocolHash,"namespace",FactoryCapabilityExperimentR31.NAMESPACE,"arm",arm.name(),"seed",seed,"realPermission",false,"naturalQualityEligible",false,
                        "status",arm==ResearchCapabilityExperimentR31.Arm.SIGNAL?"DETECTED":"NOT_DETECTED","candidateLedger",ledger,"candidateLedgerSha256",hash(ledger));
                Map<String,Object> trial=EvidenceContractStoreR31.trial(registration,artifact,JsonR27.write(artifact));
                rows.put(EvidenceContractStoreR31.trialKey(registration,arm.name(),seed),JsonR27.write(trial));
            }
            registry.putAll(m("completedOriginalTrials",120L,"plannedTrials",120L,"failedTrials",0L,"nullTrials",60L,"falseAlarms",0L,"signalTrials",20L,"signalDetections",20L,
                "falseAlarmUpper95",ResearchCapabilityExperimentR31.clopperPearsonUpper(0,60,.05),"signalPowerLower95",ResearchCapabilityExperimentR31.clopperPearsonLower(20,20,.05),
                "validationPassed",true,"validationReasons",new ArrayList<>(),"issuingScope","NEW_EXPERIMENTS_REGISTERED_UNDER_THIS_EXACT_CONTRACT_ONLY;NO_V1_OR_REAL_OVERRIDE"));
            seal();
        }
        void seal(){
            registry.put("validationOriginalsSha256",hash(rows));
            Map<String,Object> state=new LinkedHashMap<>(registry);for(String field:new String[]{"recordSha256","registration","registrationSha256","revoked","revocation","opportunities"})state.remove(field);
            registry.put("recordSha256",hash(state));registry.put("registration",JsonR27.parse(registration.record));registry.put("registrationSha256",StatsR27.sha256(registration.record));registry.put("revoked",false);registry.put("opportunities",m("fixtureOnly",true));
            life.put("originalProjectionSha256",hash(registry));life.put("originalProjectionBytes",JsonR27.write(registry).getBytes(java.nio.charset.StandardCharsets.UTF_8).length);
            originals.putAll(m("sourceCommit",SRC,"protocolHash",registration.protocolHash,"binding",registration.binding,"registryRecordSha256",registry.get("recordSha256"),"originalsSha256",hash(rows),"rowCount",rows.size()));
            nullView.put("originalSha256",hash(nulls));
        }
        ReportR27.R30Input input(){return new ReportR27.R30Input(m("activeCertificates",1L,"qualifiedExperiments",1L),m("sourceCommit",SRC,"factsReadAt",10000L),data,Collections.emptyList());}
        Map<String,Object> row(){for(Map<String,Object> row:input().metricResults)if("Q08".equals(row.get("metricId")))return row;throw new AssertionError("Q08 missing");}
    }
    static Map<String,Object> nullSummary(){
        List<Object> power=new ArrayList<>(),trials=new ArrayList<>();
        for(double edge:new double[]{.34,.37,.40}){List<Long> periods=new ArrayList<>();for(int i=0;i<20;i++)periods.add(1000L+i);
            power.add(m("trueHitProbability",edge,"experiments",20L,"detected",20L,"notBetter",0L,"censoredAt5000",0L,"detectionPeriods",periods,"detectionProbabilityInterval95",NullPipelineSimulationR30.binomial(20,20)));}
        for(long i=0;i<20;i++)trials.add(m("trial",i,"mode","SYNTHETIC_RANDOM_PERMUTATION_ONLY","naturalQualityEligible",false,"falseSequentialQualification",false,"status","MEASURED","futurePeriods",6000L,"policyHash",StatsR27.sha256("fixture-policy-"+i)));
        return m("metric","Q08","sourceCommit",SRC,"status","MEASURED","protocol","R30_REPAIR_Q08_INDEPENDENT_INDEX1_V2","plannedNullTrials",20L,"completedNullTrials",20L,"productionTrialBudget",20L,
                "earlyStoppingRule","FIXED_REGISTERED_TRIAL_COUNT_NO_STOP_AFTER_FAVOURABLE_RESULT","naturalQualityEligible",false,"mainAlphaBudgetSpent",false,"originalAlphaReset",false,"power",power,"fullPipelineTrials",trials,
                "eligibleNullCandidates",20L,"noEligibleCandidates",0L,"falseSequentialQualifications",0L,"falseQualificationGivenEligibleInterval95",NullPipelineSimulationR30.binomial(0,20),"falseQualificationPerCompletedPipelineInterval95",NullPipelineSimulationR30.binomial(0,20));
    }
    static String status(Fixture f){return String.valueOf(f.row().get("status"));}
    static Map<String,Object> archive(long faults,long claimed){
        Map<String,Object> maintenance=m("task","r27-atomnet-archive","learnerPrerequisite",false,"statusCounts",Arrays.asList(m("status","FAULT_PRESERVED","objects",faults),m("status","CLAIMED","objects",claimed)),"boundedFailureExamples",Collections.emptyList());
        Map<String,Object> capacity=m("status","READ_OK_NOT_A_HEALTH_VERDICT","readOnly",true,"sourceScope","PINNED_READ_ONLY_TRANSACTION","value",m("archiveMaintenance",maintenance));
        Map<String,Object> chapter=m("sectionId","R30_11","retention",m("lifecycleEvidenceR31",m("atomNetPublicationCapacityR33",capacity)));
        return RepairDiagnosisR32.operatingSummaryR32(Collections.singletonList(chapter));
    }
    static boolean actualLocation(Object file,Object method){
        if(!(file instanceof String)||!(method instanceof String)||((String)method).isEmpty())return false;
        String path=(String)file;if(!path.startsWith("app/src/main/java/")||!path.endsWith(".java"))return false;
        String name=path.substring("app/src/main/java/".length(),path.length()-5).replace('/','.');
        try{
            Class<?> type=Class.forName(name,false,Q08ReportBoundaryProbe.class.getClassLoader());
            if("<init>".equals(method))return type.getDeclaredConstructors().length>0;
            for(Class<?> c=type;c!=null;c=c.getSuperclass())for(java.lang.reflect.Method m:c.getDeclaredMethods())if(method.equals(m.getName()))return true;
            return false;
        }catch(ReflectiveOperationException|LinkageError missing){return false;}
    }
    public static void main(String[] args){
        Fixture f=new Fixture();Map<String,Object> row=f.row();
        check("PASS".equals(row.get("status")),"complete source-bound original producer validation reaches Q08 consumer");
        check("PASS".equals(map(row.get("r31Evaluation")).get("status")),"current versioned evaluator consumes actual Q08 row");
        check("SYNTHETIC".equals(row.get("role"))&&Boolean.FALSE.equals(row.get("naturalQualityEligible"))&&Boolean.FALSE.equals(row.get("realPermission")),"Q08 remains synthetic and grants no REAL");
        check(row.get("sampleCount") instanceof Number&&((Number)row.get("sampleCount")).longValue()==120&&row.get("value") instanceof Number&&((Number)row.get("value")).doubleValue()==1,"completeness uses original trial records");
        f=new Fixture();f.data.remove("lifecycleEvidenceR31");check(!"PASS".equals(status(f)),"120 manually counted rows without producer registry cannot pass");
        f=new Fixture();f.registry.put("sourceCommit","b".repeat(40));check("UNKNOWN_STALE_SYNTHETIC_SOURCE".equals(status(f)),"foreign source receipt not current proof");
        f=new Fixture();f.life.put("originalProjectionSha256","0".repeat(64));check("DATA_CONFLICT".equals(status(f)),"registry projection hash mismatch visible");
        f=new Fixture();map(f.registry.get("registration")).put("registeredAt",999L);f.life.put("originalProjectionSha256",hash(f.registry));check("DATA_CONFLICT".equals(status(f)),"tampered registration rejected");
        f=new Fixture();f.registry.put("falseAlarms",1L);f.life.put("originalProjectionSha256",hash(f.registry));check("DATA_CONFLICT".equals(status(f)),"state hash tamper rejected");
        f=new Fixture();f.data.remove("q08FactoryOriginalsR325");check("UNKNOWN_FACTORY_ORIGINALS_MISSING".equals(status(f)),"cached PASS cannot replace missing compact originals");
        f=new Fixture();f.originals.put("registryRecordSha256","0".repeat(64));check("UNKNOWN_SYNTHETIC_READ_SCOPE_MISMATCH".equals(status(f)),"separate reads cannot merge different producer receipts");
        f=new Fixture();f.rows.remove(f.rows.firstKey());check("UNKNOWN_SYNTHETIC_READ_SCOPE_MISMATCH".equals(status(f)),"original set hash catches omitted row");
        f=new Fixture();String k=f.rows.firstKey();Map<String,Object> trial=map(JsonR27.parse(f.rows.get(k)));trial.put("recordSha256","0".repeat(64));f.rows.put(k,JsonR27.write(trial));f.seal();check("DATA_CONFLICT".equals(status(f)),"individual original record hash checked");
        f=new Fixture();k=f.rows.firstKey();trial=map(JsonR27.parse(f.rows.get(k)));trial.put("seed",710099L);trial.remove("recordSha256");trial.put("recordSha256",hash(trial));f.rows.put(k,JsonR27.write(trial));f.seal();check("DATA_CONFLICT".equals(status(f)),"foreign arm seed rejected even with repaired outer digest");
        f=new Fixture();f.registry.put("falseAlarms",1L);f.seal();check("DATA_CONFLICT".equals(status(f)),"original outcome counts must equal validated registry counts");
        f=new Fixture();f.registry.put("falseAlarmUpper95",.16);f.seal();check("DATA_CONFLICT".equals(status(f)),"registered FAR upper bound cannot be relaxed");
        f=new Fixture();f.registry.put("signalPowerLower95",.69);f.seal();check("DATA_CONFLICT".equals(status(f)),"registered power lower bound cannot be relaxed");
        f=new Fixture();f.registry.put("validationPassed",false);f.registry.put("state","OBSERVE_ONLY");f.registry.put("validationReasons",Arrays.asList("SIGNAL_POWER_LOWER95_BELOW_REGISTERED_TARGET"));f.seal();check("FAIL".equals(status(f)),"original full design refusal remains FAIL with 120 records");
        f=new Fixture();f.factory.put("status","FAULTED");check("FAIL".equals(status(f)),"known current factory execution failure reaches Q08");
        f=new Fixture();f.factory.put("status","FACTORY_TRANSPORT_FAILED");f.data.remove("lifecycleEvidenceR31");check("FAIL".equals(status(f)),"known transport failure is not hidden by another missing source");
        f=new Fixture();f.factory.put("sourceCommit","old");f.factory.put("status","FAULTED");check("PASS".equals(status(f)),"stale display cache cannot override current original validation");
        f=new Fixture();f.registry.put("revoked",true);f.registry.put("state","REVOKED");f.registry.put("revocation",m("binding",f.registration.binding,"state","REVOKED","reason","RISK_BREACH","at",3000L));f.life.put("originalProjectionSha256",hash(f.registry));check("FAIL".equals(status(f)),"durable revocation overlay is negative without rewriting CURRENT hash");
        f=new Fixture();f.data.remove("syntheticControl");check("RECORDED_UNVALIDATED".equals(status(f)),"factory validation alone lacks legacy power and null suite");
        f=new Fixture();list(f.nulls.get("power")).remove(0);f.seal();check("RECORDED_UNVALIDATED".equals(status(f)),"all three original power edges are required");
        f=new Fixture();list(f.nulls.get("fullPipelineTrials")).remove(0);f.seal();check("RECORDED_UNVALIDATED".equals(status(f)),"all twenty original null repetitions are required");
        f=new Fixture();map(map(list(f.nulls.get("power")).get(0)).get("detectionProbabilityInterval95")).put("upper95",.1);f.seal();check("RECORDED_UNVALIDATED".equals(status(f)),"inconsistent original descriptive interval rejected");
        f=new Fixture();map(list(f.nulls.get("power")).get(0)).put("experiments",19L);f.seal();check("RECORDED_UNVALIDATED".equals(status(f)),"power support cannot be padded by factory count");
        f=new Fixture();f.nulls.put("sourceCommit","old");f.seal();check("RECORDED_UNVALIDATED".equals(status(f)),"legacy power source must match current source");
        f=new Fixture();f.factory.clear();map(f.data.get("factoryCapabilityExperiment")).put("status","REFERENCE_ONLY_ORIGINAL_BYTE_BOUND");check("PASS".equals(status(f)),"16KiB factory display bound cannot hide complete authoritative compact originals");
        f=new Fixture();String remove=null;for(String key:f.rows.keySet())if(key.contains(":GAPS:")){remove=key;break;}f.rows.remove(remove);f.registry.put("completedOriginalTrials",119L);f.registry.put("state","OBSERVE_ONLY");f.registry.put("validationPassed",false);f.registry.put("validationReasons",Arrays.asList("INCOMPLETE_FULL_FACTORY_TRIAL_SET"));f.seal();check("INSUFFICIENT".equals(status(f)),"honest partial trial progress is insufficient");
        check(((Number)definition("Q05").get("minimumSample")).longValue()==200,"Q05 report support unit is 200 calibration observations");
        check(((Number)definition("Q07").get("minimumSample")).longValue()==1,"Q07 report support unit is one inspected asset record");
        boolean natural=true;for(int i=1;i<=7;i++)natural&="NATURAL_FUTURE_ONLY".equals(definition(String.format(Locale.ROOT,"Q%02d",i)).get("role"));check(natural,"all seven natural roles unchanged");
        f=new Fixture();Map<String,Object> q07=null;for(Map<String,Object> r:f.input().metricResults)if("Q07".equals(r.get("metricId")))q07=r;check(q07!=null&&"RECORDED_UNVALIDATED".equals(q07.get("status")),"one active certificate count alone never qualifies Q07");
        check(CertificateGateR27.MIN_ACTS==250,"per-asset original V1 minimum ACT count is unchanged");
        Map<String,Object> a=archive(1,1);check("DEGRADED_ARCHIVE_ONLY".equals(map(a.get("atomNetArchiveMaintenance")).get("status")),"archival poison is visible as archival-only degradation");
        a=archive(0,1);check("OBSERVED_NO_ISOLATED_ARCHIVE_FAILURE".equals(map(a.get("atomNetArchiveMaintenance")).get("status")),"claimed but not failed archival object is not a failure");
        check(Boolean.FALSE.equals(map(a.get("atomNetArchiveMaintenance")).get("learnerPrerequisite")),"archive display does not become a learner prerequisite");
        check(map(a.get("tasks")).containsKey("r27-atomnet")&&map(a.get("tasks")).containsKey("r27-atomnet-archive"),"training and archive task receipts displayed separately");
        Map<String,Object> detail=RepairDiagnosisR32.build(ReportObjectIndexR30.buildProductionR31(m(),m("sourceCommit",SRC),m()));
        List<Object> objectRows=list(detail.get("rows"));boolean located=objectRows.size()==165,legacyLocated=true;int legacyCount=0;
        for(Object object:objectRows){Map<String,Object> objectRow=map(object);located&=actualLocation(objectRow.get("sourceFile"),objectRow.get("sourceMethod"));
            if("legacyCharts".equals(objectRow.get("group"))){legacyCount++;legacyLocated&=actualLocation(objectRow.get("consumerFile"),objectRow.get("consumerMethod"))&&String.valueOf(objectRow.get("locationScope")).contains("NOT_EXECUTION");}}
        check(located,"all 165 object producer files and methods resolve to actual compiled production classes");
        check(legacyCount==29&&legacyLocated,"all 29 legacy chart consumer methods exist and location is not execution evidence");
        String[] exported=RepairDiagnosisR32.attach(new String[]{"original diagnostic",JsonR27.write(m("findings",Collections.emptyList()))},detail);
        List<Object> exportedRows=list(map(map(JsonR27.parse(exported[1])).get("capabilityRepairR32")).get("rows"));boolean exportLocated=exportedRows.size()==165;
        for(Object object:exportedRows){Map<String,Object> objectRow=map(object);exportLocated&=actualLocation(objectRow.get("sourceFile"),objectRow.get("sourceMethod"))&&exported[0].contains(String.valueOf(objectRow.get("sourceFile")));}
        check(exportLocated,"standalone UPGRADE JSON and Markdown carry all 165 actual code locations");
        System.out.println("RESULT checks="+checks+" failures="+failures);if(failures>0)System.exit(1);
    }
}
