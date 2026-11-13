package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;

/** Production R31 design registry. The existing fenced HEALTH writer commits
 * registration before the first new factory step. Independent, non-DISPLAY
 * panels are durable protocol records, not rendering caches. No DDL runs here.
 * V1 certificates/exposures and DecisionGate authority are never rewritten. */
public final class EvidenceContractStoreR31 {
    private EvidenceContractStoreR31() {}
    public static final String VERSION="R31_RISK_REGISTRY_1";
    static final String REG="r31-risk-contract",STATE="r31-risk-contract-state",TRIAL="r31-risk-contract-trial",EVENT="r31-risk-contract-event";
    static final int CELL_BYTES=16384,TRIALS=120;
    public static EvidenceContractR31.Contract productionContract() {
        return EvidenceContractR31.register("R31_FULL_FACTORY_RISK_V1","PAIRED_UTILITY_PER_ELIGIBLE_OPPORTUNITY",
                "CALIBRATION_WORST_BLOCK_AND_CONTINUOUS_LOSS_RISK_WITH_UNCHANGED_V1_GATES",
                "6000_FUTURE_PERIODS_AFTER_3000_WARM_PERIODS;3_GENERATIONS;6_ARMS_X_20_SEEDS",
                "EVERY_REGISTERED_POLICY_TARGET_OPPORTUNITY_INCLUDING_ABSTAIN;MISSED_AND_UNKNOWN_REPORTED_SEPARATELY",
                "NO_REAL_BEFORE_ORIGINAL_V1_CERTIFICATE;REVOKE_ON_SOURCE_ABI_RISK_OR_DESIGN_VALIDATION_FAILURE",
                "ORIGINAL_V1_ECE_LE_0.04;WORST720_GE_0.26;MAX_LOSS_STREAK_LE_6",CertificateGateR27.MIN_ACTS,
                .15,.70,.15,.05,"REGISTERED_CURRENT_SOURCE_AND_SEMANTIC_ABI;DEVICE_ACCEPTANCE_SEPARATE");
    }
    public static final class Registration {
        public final String source,binding,protocolHash,record;public final EvidenceContractR31.Contract contract;public final long registeredAt;
        Registration(String source,long at){this.source=source;registeredAt=at;contract=productionContract();protocolHash=FactoryCapabilityExperimentR31.protocolHash();binding=StatsR27.sha256(VERSION+"|"+source+"|"+contract.hash+"|"+protocolHash);
            Map<String,Object> m=new LinkedHashMap<>();m.put("version",VERSION);m.put("sourceCommit",source);m.put("binding",binding);m.put("contract",contract.json());m.put("protocolHash",protocolHash);m.put("registeredAt",at);
            m.put("registrationBoundary","COMMITTED_BEFORE_FIRST_FACTORY_STEP");m.put("realPermission",false);record=JsonR27.write(m);}
        public String factoryKey(){return source+":"+protocolHash+":"+binding;}
    }
    static String read(SQLiteDatabase db,String panel,String key){
        try(Cursor c=db.rawQuery("SELECT CASE WHEN length(CAST(payload_json AS BLOB))<=CAST(? AS INTEGER) THEN payload_json ELSE NULL END,length(CAST(payload_json AS BLOB)) FROM r27_chart_cache WHERE panel_id=? AND key=?",new String[]{Integer.toString(CELL_BYTES),panel,key})){
            if(!c.moveToFirst())return null;if(c.isNull(0)||c.getLong(1)>CELL_BYTES)throw new IllegalStateException("R31_CONTRACT_RECORD_CAPACITY_OR_CORRUPTION");return c.getString(0);}
    }
    @SuppressWarnings("unchecked") static Map<String,Object> map(String text){Object value=JsonR27.parse(text);if(!(value instanceof Map))throw new IllegalArgumentException("R31_CONTRACT_RECORD_OBJECT");return (Map<String,Object>)value;}
    static void sameBefore(SQLiteDatabase read,ResearchWriteQueueR27.Batch batch,String panel,String key,String expected){batch.guardBefore(db->Objects.equals(expected,read(db,panel,key))?0:-1);}
    static void append(SQLiteDatabase read,ResearchWriteQueueR27.Batch batch,String panel,String key,String text,long at){
        if(text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>CELL_BYTES)throw new IllegalStateException("R31_CONTRACT_RECORD_CAPACITY");String prior=read(read,panel,key);
        if(prior!=null&&!prior.equals(text))throw new IllegalStateException("R31_CONTRACT_IMMUTABLE_RECORD_CONFLICT");sameBefore(read,batch,panel,key,prior);
        if(prior==null)batch.add("INSERT INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES(?,?,?,?)",panel,key,text,at);
    }
    static String recordHash(Map<String,Object> value){Map<String,Object> copy=new LinkedHashMap<>(value);copy.remove("recordSha256");return StatsR27.sha256(JsonR27.write(copy));}
    static void requireRecordHash(Map<String,Object> value){if(!recordHash(value).equals(value.get("recordSha256")))throw new IllegalArgumentException("R31_CONTRACT_RECORD_HASH");}
    static void replace(SQLiteDatabase read,ResearchWriteQueueR27.Batch batch,String panel,String key,Map<String,Object> value,long at){String old=read(read,panel,key);sameBefore(read,batch,panel,key,old);value.put("recordSha256",recordHash(value));batch.add("INSERT OR REPLACE INTO r27_chart_cache(panel_id,key,payload_json,updated_at) VALUES(?,?,?,?)",panel,key,JsonR27.write(value),at);}
    /** null means registration is only prepared, not committed. The factory must
     * return without executing a step, then read the committed record next slice. */
    public static Registration beforeFactory(SQLiteDatabase read,ResearchWriteQueueR27.Batch batch,String source,long at){
        if(source==null||source.isEmpty()||source.length()>160||at<1)throw new IllegalArgumentException("R31_CONTRACT_SOURCE_OR_TIME");
        Registration proposed=new Registration(source,at);String existing=read(read,REG,proposed.binding);
        if(existing==null){
            String pointer=read(read,STATE,"CURRENT");
            if(pointer!=null){Map<String,Object> previous=map(pointer);Object binding=previous.get("binding");if(binding instanceof String&&!proposed.binding.equals(binding))revoke(read,batch,(String)binding,"SOURCE_OR_PROTOCOL_REPLACED",at);}
            append(read,batch,REG,proposed.binding,proposed.record,at);
            Map<String,Object> state=status(proposed,"OBSERVE_ONLY","PREREGISTERED_WAIT_FIRST_FACTORY_STEP",at);replace(read,batch,STATE,proposed.binding,state,at);replace(read,batch,STATE,"CURRENT",state,at);return null;
        }
        Map<String,Object> saved=map(existing);Object time=saved.get("registeredAt");if(!(time instanceof Number))throw new IllegalStateException("R31_CONTRACT_REGISTRATION_TIME");
        Registration registration=new Registration(source,((Number)time).longValue());if(!existing.equals(registration.record))throw new IllegalStateException("R31_CONTRACT_REGISTRATION_IDENTITY");
        sameBefore(read,batch,REG,registration.binding,existing);return registration;
    }
    static Map<String,Object> status(Registration r,String state,String reason,long at){Map<String,Object> m=new LinkedHashMap<>();m.put("version",VERSION);m.put("binding",r.binding);m.put("sourceCommit",r.source);m.put("contractHash",r.contract.hash);m.put("contractId",r.contract.id);m.put("protocolHash",r.protocolHash);m.put("registeredAt",r.registeredAt);m.put("state",state);m.put("reason",reason);m.put("updatedAt",at);m.put("realPermission",false);m.put("naturalQuality","NOT_PROVEN_SYNTHETIC_ONLY");m.put("currentRealAuthority",CertificateGateR27.GATE_VERSION);return m;}
    static String trialKey(Registration r,String arm,long seed){return r.binding+":"+arm+":"+seed;}
    static long integer(Object value,String name){if(!(value instanceof Number)||((Number)value).doubleValue()!=((Number)value).longValue())throw new IllegalArgumentException("R31_CONTRACT_TRIAL_"+name);return ((Number)value).longValue();}
    static String digest(Object value){if(!(value instanceof String)||!((String)value).matches("[0-9a-f]{64}"))throw new IllegalArgumentException("R31_CONTRACT_TRIAL_HASH");return (String)value;}
    /** Original full search ledger hash, source, protocol, namespace, arm and
     * seed are authenticated before the compact validation record is appended. */
    static Map<String,Object> trial(Registration r,Map<String,Object> artifact,String encoded){
        if(!r.source.equals(artifact.get("sourceCommit"))||!r.protocolHash.equals(artifact.get("protocolHash"))||!FactoryCapabilityExperimentR31.NAMESPACE.equals(artifact.get("namespace"))||!Boolean.FALSE.equals(artifact.get("realPermission"))||!Boolean.FALSE.equals(artifact.get("naturalQualityEligible")))throw new IllegalArgumentException("R31_CONTRACT_TRIAL_IDENTITY");
        String arm=String.valueOf(artifact.get("arm"));ResearchCapabilityExperimentR31.Arm.valueOf(arm);long seed=integer(artifact.get("seed"),"SEED");if(seed<710000||seed>=710020)throw new IllegalArgumentException("R31_CONTRACT_TRIAL_SEED");
        String outcome=String.valueOf(artifact.get("status"));if(!Arrays.asList("DETECTED","NOT_DETECTED","FAILED").contains(outcome))throw new IllegalArgumentException("R31_CONTRACT_TRIAL_STATUS");
        String ledgerHash=digest(artifact.get("candidateLedgerSha256"));if(!ledgerHash.equals(StatsR27.sha256(JsonR27.write(artifact.get("candidateLedger")))))throw new IllegalArgumentException("R31_CONTRACT_TRIAL_LEDGER_HASH");
        Map<String,Object> m=new LinkedHashMap<>();m.put("binding",r.binding);m.put("sourceCommit",r.source);m.put("protocolHash",r.protocolHash);m.put("arm",arm);m.put("seed",seed);m.put("status",outcome);m.put("candidateLedgerSha256",ledgerHash);m.put("artifactCellSha256",StatsR27.sha256(encoded));m.put("realPermission",false);m.put("recordSha256",recordHash(m));return m;
    }
    static LinkedHashMap<String,String> trials(SQLiteDatabase db,Registration r){LinkedHashMap<String,String> rows=new LinkedHashMap<>();int bytes=0;
        try(Cursor c=db.rawQuery("SELECT key,CASE WHEN length(CAST(payload_json AS BLOB))<=CAST(? AS INTEGER) THEN payload_json ELSE NULL END FROM r27_chart_cache WHERE panel_id=? AND key>=? AND key<? ORDER BY key LIMIT 121",new String[]{Integer.toString(CELL_BYTES),TRIAL,r.binding+":",r.binding+";"})){
            while(c.moveToNext()){String text=c.getString(1);if(text==null||(bytes+=text.getBytes(java.nio.charset.StandardCharsets.UTF_8).length)>262144||rows.size()>=TRIALS)throw new IllegalStateException("R31_CONTRACT_TRIAL_RECORD_BOUND");rows.put(c.getString(0),text);}}
        return rows;
    }
    /** Invoked for the actual production factory snapshot. All status changes,
     * trial receipts and the factory checkpoint commit together or not at all. */
    public static Map<String,Object> afterFactory(SQLiteDatabase read,ResearchWriteQueueR27.Batch batch,Registration registration,Map<String,Object> summary,Map<String,Object> artifact,String encoded,long at){
        if(!registration.source.equals(summary.get("sourceCommit"))||!registration.protocolHash.equals(summary.get("protocolHash")))throw new IllegalArgumentException("R31_CONTRACT_SUMMARY_IDENTITY");
        LinkedHashMap<String,String> original=trials(read,registration),all=new LinkedHashMap<>(original);
        if(artifact!=null){Map<String,Object> row=trial(registration,artifact,encoded);String key=trialKey(registration,(String)row.get("arm"),integer(row.get("seed"),"SEED")),text=JsonR27.write(row);append(read,batch,TRIAL,key,text,at);all.put(key,text);}
        // A compact original set (at most 120 rows / 256 KiB), never inflate a
        // factory or search ledger inside the writer transaction.
        batch.guardBefore(db->original.equals(trials(db,registration))?0:-1);
        List<String> reasons=new ArrayList<>();int nullN=0,falseAlarms=0,signalN=0,detected=0,failed=0;Set<String> ids=new HashSet<>();
        for(Map.Entry<String,String> entry:all.entrySet()){Map<String,Object> row=map(entry.getValue());requireRecordHash(row);String arm=String.valueOf(row.get("arm"));ResearchCapabilityExperimentR31.Arm.valueOf(arm);long seed=integer(row.get("seed"),"SEED");
            if(!registration.binding.equals(row.get("binding"))||!registration.source.equals(row.get("sourceCommit"))||!registration.protocolHash.equals(row.get("protocolHash"))||seed<710000||seed>=710020||!entry.getKey().equals(trialKey(registration,arm,seed))||!ids.add(arm+":"+seed))throw new IllegalArgumentException("R31_CONTRACT_FOREIGN_OR_DUPLICATE_TRIAL");
            digest(row.get("candidateLedgerSha256"));digest(row.get("artifactCellSha256"));String outcome=String.valueOf(row.get("status"));if(!Arrays.asList("DETECTED","NOT_DETECTED","FAILED").contains(outcome))throw new IllegalArgumentException("R31_CONTRACT_TRIAL_STATUS");
            if("FAILED".equals(outcome))failed++;if(Arrays.asList("NULL_UNIFORM","NULL_TIME_SHUFFLED","SIGNAL_DISABLED").contains(arm)){nullN++;if("DETECTED".equals(outcome))falseAlarms++;}if("SIGNAL".equals(arm)){signalN++;if("DETECTED".equals(outcome))detected++;}
        }
        if(all.size()!=TRIALS)reasons.add("INCOMPLETE_FULL_FACTORY_TRIAL_SET");if(integer(summary.get("completedTrials"),"COMPLETED")!=all.size())reasons.add("SUMMARY_AND_ORIGINAL_TRIAL_COUNT_DIFFER");if(failed!=0||integer(summary.get("failedTrials"),"FAILED")!=0)reasons.add("FACTORY_EXECUTION_FAILURES");
        if(!"COMPLETE_MEASURED_REQUIRES_REGISTERED_POWER_ASSESSMENT".equals(summary.get("status")))reasons.add("FACTORY_NOT_COMPLETE");
        double far=nullN==0?1:ResearchCapabilityExperimentR31.clopperPearsonUpper(falseAlarms,nullN,.05),power=signalN==0?0:ResearchCapabilityExperimentR31.clopperPearsonLower(detected,signalN,.05);
        if(far>registration.contract.errorBudget)reasons.add("FALSE_ALARM_UPPER95_ABOVE_REGISTERED_BUDGET");if(power<registration.contract.powerTarget)reasons.add("SIGNAL_POWER_LOWER95_BELOW_REGISTERED_TARGET");
        EvidenceContractR31.Validation validation=new EvidenceContractR31.Validation(registration.contract.hash,registration.protocolHash,reasons.isEmpty(),reasons,far,power);
        String revoked=read(read,EVENT,registration.binding+":REVOKED");EvidenceContractR31.Active active=EvidenceContractR31.activate(registration.contract,revoked==null?validation:null);
        Map<String,Object> current=status(registration,revoked==null?active.state.name():"REVOKED",revoked==null?(validation.passed?"COMPLETE_REGISTERED_FULL_FACTORY_VALIDATION":"WAIT_REGISTERED_FULL_FACTORY_VALIDATION"):"DURABLE_REVOCATION",at);
        current.put("completedOriginalTrials",all.size());current.put("plannedTrials",TRIALS);current.put("failedTrials",failed);current.put("nullTrials",nullN);current.put("falseAlarms",falseAlarms);current.put("signalTrials",signalN);current.put("signalDetections",detected);current.put("falseAlarmUpper95",nullN==0?null:far);current.put("signalPowerLower95",signalN==0?null:power);current.put("validationPassed",validation.passed);current.put("validationReasons",reasons);current.put("validationOriginalsSha256",StatsR27.sha256(JsonR27.write(new TreeMap<>(all))));
        current.put("issuingScope","NEW_EXPERIMENTS_REGISTERED_UNDER_THIS_EXACT_CONTRACT_ONLY;NO_V1_OR_REAL_OVERRIDE");
        sameBefore(read,batch,EVENT,registration.binding+":REVOKED",revoked);replace(read,batch,STATE,registration.binding,current,at);replace(read,batch,STATE,"CURRENT",current,at);return current;
    }
    /** Called by the source/protocol replacement path and available to the
     * existing fenced risk owner. Revocation is append-only and survives reboot. */
    public static void revoke(SQLiteDatabase read,ResearchWriteQueueR27.Batch batch,String binding,String reason,long at){
        if(binding==null||!binding.matches("[0-9a-f]{64}")||!Arrays.asList("SOURCE_OR_PROTOCOL_REPLACED","SOURCE_REVISION_CHANGED","ABI_CHANGED","RISK_BREACH","DESIGN_VALIDATION_REVOKED").contains(reason))throw new IllegalArgumentException("R31_CONTRACT_REVOCATION_REASON");
        String prior=read(read,EVENT,binding+":REVOKED");if(prior!=null)return;Map<String,Object> event=new LinkedHashMap<>();event.put("binding",binding);event.put("state","REVOKED");event.put("reason",reason);event.put("at",at);append(read,batch,EVENT,binding+":REVOKED",JsonR27.write(event),at);
        String old=read(read,STATE,binding);if(old!=null){Map<String,Object> state=map(old);state.put("state","REVOKED");state.put("reason",reason);state.put("updatedAt",at);state.put("realPermission",false);replace(read,batch,STATE,binding,state,at);}
    }
    /** Bounded read-only report consumer. Missing/corrupt records are UNKNOWN,
     * never zero success or implicit permission. Registry originals stay intact. */
    public static Map<String,Object> report(SQLiteDatabase db){Map<String,Object> out=new LinkedHashMap<>();out.put("version",VERSION);out.put("currentRealAuthority",CertificateGateR27.GATE_VERSION);out.put("realPermission",false);out.put("naturalQuality","NOT_PROVEN");
        try{String current=read(db,STATE,"CURRENT");if(current==null){out.put("state","NOT_REGISTERED");return out;}Map<String,Object> state=map(current);requireRecordHash(state);String binding=digest(state.get("binding")),original=read(db,REG,binding);if(original==null)throw new IllegalStateException("R31_CONTRACT_REGISTRATION_MISSING");Map<String,Object> registered=map(original);Registration r=new Registration((String)registered.get("sourceCommit"),integer(registered.get("registeredAt"),"REGISTERED_AT"));if(!original.equals(r.record)||!binding.equals(r.binding))throw new IllegalStateException("R31_CONTRACT_REGISTRATION_IDENTITY");
            String revocation=read(db,EVENT,binding+":REVOKED");out.putAll(state);out.put("registration",registered);out.put("registrationSha256",StatsR27.sha256(original));out.put("revoked",revocation!=null);if(revocation!=null){out.put("state","REVOKED");out.put("revocation",map(revocation));}out.put("opportunities",opportunities(db,r.contract));
        }catch(RuntimeException failure){FailureDispositionR31.Disposition category=FailureDispositionR31.classify(failure).disposition;if(category==FailureDispositionR31.Disposition.CANCELLED)throw failure;out.put("state","UNKNOWN_REGISTRY_UNAVAILABLE_OR_CORRUPT");out.put("validationPassed",null);out.put("reasonCode","REGISTRY_READ_FAILED");}
        return out;
    }
    private static Map<String,Object> opportunities(SQLiteDatabase db,EvidenceContractR31.Contract contract){Map<String,Object> out=new LinkedHashMap<>();out.put("scope","BOUNDED_CURRENT_V1_EXPERIMENT_OPPORTUNITIES;NOT_R31_FORMAL_EVIDENCE");out.put("maximumRows",2048);
        try{String experiment;try(Cursor c=db.rawQuery("SELECT policy_hash FROM r27_eprocess WHERE global_index>0 ORDER BY global_index DESC,policy_hash LIMIT 1",null)){if(!c.moveToFirst()){out.put("state","NO_REGISTERED_EXPERIMENT");return out;}experiment=c.getString(0);}long eligible=0,act=0,abstain=0,missedInput=0,missedDeadline=0,unknown=0,rows=0,from=-1,to=-1;
            try(Cursor c=db.rawQuery("SELECT target_ordinal,status,act FROM r27_live_frozen WHERE policy_hash=? ORDER BY target_ordinal DESC LIMIT 2048",new String[]{experiment})){while(c.moveToNext()){rows++;long ordinal=c.getLong(0);from=ordinal;if(to<0)to=ordinal;String status=c.getString(1);int action=c.getInt(2);if(("FROZEN".equals(status)||"ABSTAIN_SET_SUPPORT".equals(status))&&(action==0||action==1)){eligible++;if(action==1)act++;else abstain++;}else if("SKIPPED_GAP".equals(status))missedInput++;else if("DEADLINE_MISSED".equals(status))missedDeadline++;else unknown++;}}
            long missing=rows==0?0:Math.max(0,Math.subtractExact(Math.addExact(to-from,1),rows));unknown=Math.addExact(unknown,missing);
            out.putAll(EvidenceContractR31.frequencyReport(contract,new EvidenceContractR31.Opportunities(eligible,act,abstain,missedInput,missedDeadline,unknown),false,"OBSERVED_FROZEN_SELECTOR_SUPPORT_ONLY"));out.put("experiment",experiment);out.put("state",rows==0?"NO_OBSERVED_OPPORTUNITIES":"READ_OK_BOUNDED_TAIL");out.put("fromOrdinal",rows==0?null:from);out.put("toOrdinal",rows==0?null:to);out.put("unrecordedOrdinalsWithinObservedTail",missing);out.put("historyComplete",false);out.put("boundedTailAtLimit",rows==2048);out.put("scope","BOUNDED_CURRENT_V1_EXPERIMENT_OPPORTUNITIES;NOT_R31_FORMAL_EVIDENCE");
        }catch(RuntimeException missing){if(FailureDispositionR31.classify(missing).disposition==FailureDispositionR31.Disposition.CANCELLED)throw missing;out.put("state","UNKNOWN_OPPORTUNITY_SOURCE");}
        return out;
    }
}
