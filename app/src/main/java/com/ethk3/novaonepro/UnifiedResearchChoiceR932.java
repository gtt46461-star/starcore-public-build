package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;

/** Freeze one earlier certificate across all three consumers before any overlay executes. */
public final class UnifiedResearchChoiceR932 {
    public static final String TABLE="research_consumer_choice_r932";
    private UnifiedResearchChoiceR932(){}
    /**
     * Freeze one certified Research overlay. Research owns no REAL authority, therefore an
     * unavailable Research ledger must degrade to zero overlay instead of deleting the Production
     * prediction. Scope violations still fail hard because they indicate an internal PIT bug.
     */
    public static boolean freeze(final DbStore store,final String target) {
        if(store==null||!target.equals(ModelBundleRuntimeR932.currentTarget()))
            throw new IllegalStateException("R932_CHOICE_REQUEST_SCOPE");
        try {
            ResearchPlatformStoreR929.ensureSchema(store);
            final String bundle=ModelBundleRuntimeR932.current().hash;
            try(Cursor previous=store.readable().rawQuery("SELECT bundle_hash FROM "+TABLE+" WHERE target_issue=?",new String[]{target})){
                if(previous.moveToFirst()){
                    if(!bundle.equals(previous.getString(0)))throw new IllegalStateException("R932_CHOICE_BUNDLE_DRIFT");
                    publishFreezeStatus(store,target,currentChoiceReasonCP12(store,target));return true;
                }
            }
            // Modern graph preparation/inference forbids a writer transaction. Prepare
            // executable candidates before acquiring the existing canonical write owner.
            final PreparedChoiceCP12 prepared=prepareChoiceCP12(store,target,bundle);
            final String[] commitFailure={"RESEARCH_OVERLAY_DISABLED_WRITE_FAILED"};
            Boolean ok=store.coordinatorR36().runAtomicWrite(
                    "R932_FREEZE_ONE_RESEARCH_CONSUMER",
                    new DatabaseCoordinator.WriteCallable<Boolean>(){
                        public Boolean call(SQLiteDatabase db){
                            if(!bundle.equals(ModelBundleRuntimeR932.current().hash))throw new IllegalStateException("R932_CHOICE_BUNDLE_DRIFT");
                            try(Cursor c=db.rawQuery("SELECT bundle_hash FROM "+TABLE+" WHERE target_issue=?",new String[]{target})){
                                if(c.moveToFirst()){
                                    if(!bundle.equals(c.getString(0)))throw new IllegalStateException("R932_CHOICE_BUNDLE_DRIFT");
                                    return Boolean.TRUE;
                                }
                            }
                            try(Cursor draw=db.rawQuery("SELECT 1 FROM "+DbHelper.TABLE_DRAWS+" WHERE issue=? LIMIT 1",new String[]{target})){
                                if(draw.moveToFirst()){
                                    commitFailure[0]="R932_CHOICE_POSTDRAW_REJECTED";
                                    throw new IllegalStateException(commitFailure[0]);
                                }
                            }
                            if(!prepared.consumer.isEmpty()){
                                ResearchPlatformStoreR929.Authorization current=ResearchPlatformStoreR929.authorizeCurrent(
                                        store,CertificateBundleServiceR929.Consumer.valueOf(prepared.consumer),prepared.candidate,prepared.structure,target,.30);
                                if(!current.authorized||!prepared.certificate.equals(current.certificateHash)){
                                    commitFailure[0]="R932_CHOICE_PREPARED_CERTIFICATE_CHANGED:"+current.reason;
                                    throw new IllegalStateException(commitFailure[0]);
                                }
                            }
                            for(String[] rejected:prepared.rejections)db.execSQL("INSERT OR IGNORE INTO r974_prediction_rejection VALUES(?,?,?,?,?)",
                                    new Object[]{target,rejected[0],rejected[1],rejected[2],System.currentTimeMillis()});
                            db.execSQL("INSERT INTO "+TABLE+" VALUES(?,?,?,?,?,?,?)",
                                    new Object[]{target,bundle,prepared.consumer,prepared.candidate,prepared.certificate,
                                            prepared.rosterHash,System.currentTimeMillis()});
                            return Boolean.TRUE;
                        }
                    },Boolean.FALSE);
            if(Boolean.TRUE.equals(ok)){
                publishFreezeStatus(store,target,currentChoiceReasonCP12(store,target));
                return true;
            }
            publishFreezeStatus(store,target,commitFailure[0]);
            return false;
        } catch(ResearchWorkBudgetR927.Yield yielded){
            publishFreezeStatus(store,target,"RESEARCH_OVERLAY_YIELD_P0");
            return false;
        } catch(RuntimeResourceAuditR927.BudgetExceeded budget){
            publishFreezeStatus(store,target,"RESEARCH_OVERLAY_YIELD_DEADLINE");
            return false;
        } catch(RuntimeException unavailable){
            publishFreezeStatus(store,target,"RESEARCH_OVERLAY_UNAVAILABLE_"+unavailable.getClass().getSimpleName()+":"+String.valueOf(unavailable.getMessage()));
            return false;
        }
    }
    private static final class PreparedChoiceCP12 {
        final String consumer,candidate,structure,certificate,rosterHash;
        final List<String[]> rejections;
        PreparedChoiceCP12(String consumer,String candidate,String structure,String certificate,String rosterHash,List<String[]> rejections){
            this.consumer=consumer;this.candidate=candidate;this.structure=structure;this.certificate=certificate;this.rosterHash=rosterHash;
            ArrayList<String[]> copy=new ArrayList<String[]>();for(String[] r:rejections)copy.add(r.clone());this.rejections=Collections.unmodifiableList(copy);
        }
    }
    /** Pure bounded preflight. Failure receipts are committed later with the choice. */
    private static PreparedChoiceCP12 prepareChoiceCP12(DbStore store,String target,String bundle){
        if(store.readable().inTransaction())throw new IllegalStateException("R932_CHOICE_PREFLIGHT_REQUIRES_NO_WRITER");
        String consumer="",candidate="",structure="",certificate="";
        ArrayList<String[]> rejections=new ArrayList<String[]>();
        StringBuilder roster=new StringBuilder("CP12_LCB_BRIER_LATENCY_ID|").append(target).append('|').append(bundle);
        try(Cursor c=store.readable().rawQuery(
                "SELECT c.consumer,c.candidate_id,c.structural_hash,c.certificate_hash FROM "
                        +ResearchPlatformStoreR929.TABLE_CERTIFICATE
                        +" c WHERE c.state='ACTIVE' AND c.bundle_hash=? AND c.issued_at<? AND c.maximum_weight>0 AND c.all_gates_pass=1"
                        +(ResearchGatePlanR967.installed().isCp3?"":" AND NOT EXISTS(SELECT 1 FROM "+ResearchCohortR932.CHAMPION+" f WHERE f.family=c.family_id AND f.candidate_key<>(CASE c.consumer WHEN 'INVENTED_ALGORITHM' THEN 'algorithm:' WHEN 'GENESIS_ASSET' THEN 'genesis:' ELSE 'formula:' END)||c.candidate_id)")
                        +" AND c.consumer='INVENTED_ALGORITHM' AND c.candidate_id LIKE 'R958-F-%' ORDER BY COALESCE((SELECT observed FROM "+ResearchPlatformStoreR929.TABLE_CERT_GATE+" WHERE certificate_hash=c.certificate_hash AND gate_id='WILSON_LOWER'),-1) DESC,"
                        +" COALESCE((SELECT observed FROM "+ResearchPlatformStoreR929.TABLE_CERT_GATE+" WHERE certificate_hash=c.certificate_hash AND gate_id='BRIER'),1) ASC,"
                        +" COALESCE((SELECT observed FROM "+ResearchPlatformStoreR929.TABLE_CERT_GATE+" WHERE certificate_hash=c.certificate_hash AND gate_id='RESOURCE_LATENCY_P95_MS'),1e99) ASC,"
                        +" c.candidate_id ASC,c.certificate_hash ASC",
                new String[]{bundle,String.valueOf(ModelBundleRuntimeR932.capturedAt())})){
            while(c.moveToNext()){
                ResearchWorkBudgetR927.checkpoint();
                String scopeReason=CertifiedPredictionR974.certificatePreflightReasonF05(store,c.getString(3),c.getString(1),c.getString(2),target);
                if(!scopeReason.isEmpty()){
                    roster.append('|').append(c.getString(3)).append(':').append(scopeReason);
                    rejections.add(new String[]{c.getString(1),c.getString(3),scopeReason});continue;
                }
                if("INVENTED_ALGORITHM".equals(c.getString(0))&&c.getString(1).startsWith(FrozenCandidateAdapterR958.PREFIX)){
                    String unavailable=CertifiedPredictionR974.executableReason(store,c.getString(1),c.getString(2),target);
                    if(!unavailable.isEmpty()){
                        roster.append('|').append(c.getString(1)).append(':').append(unavailable);
                        rejections.add(new String[]{c.getString(1),c.getString(3),unavailable});continue;
                    }
                }
                if(!ResearchEvaluationRunR967.consumerCompatible(store.readable(),c.getString(1),target)){roster.append('|').append(c.getString(3)).append(":OUTSIDE_STATIC_DOMAIN_OR_RUN");rejections.add(new String[]{c.getString(1),c.getString(3),"OUTSIDE_STATIC_DOMAIN_OR_RUN"});continue;}
                CertificateBundleServiceR929.Consumer type;
                try{type=CertificateBundleServiceR929.Consumer.valueOf(c.getString(0));}
                catch(IllegalArgumentException invalid){roster.append('|').append(c.getString(3)).append(":INVALID_CONSUMER");rejections.add(new String[]{c.getString(1),c.getString(3),"INVALID_CONSUMER"});continue;}
                ResearchPlatformStoreR929.Authorization auth=ResearchPlatformStoreR929.authorizeCurrent(store,type,c.getString(1),c.getString(2),target,.30);
                roster.append('|').append(c.getString(3)).append(':').append(auth.authorized).append(':').append(auth.grantedWeight);
                if(auth.authorized){consumer=type.name();candidate=c.getString(1);structure=c.getString(2);certificate=c.getString(3);
                    roster.append("|POOL_LIMIT=NONE|REMAINDER=NOT_PREFLIGHTED_AFTER_HIGHER_PRIORITY_SELECTION");break;}
                rejections.add(new String[]{c.getString(1),c.getString(3),"AUTHORIZE_CURRENT_REJECTED:"+auth.reason});
                roster.append('|').append(auth.reason);
            }
        }
        if(consumer.isEmpty()){
            com.ethk3.novaonepro.strategyv2.CertificateSU1.Authority su1=com.ethk3.novaonepro.strategyv2.MainlineAdapterSU1.first(store,target);
            if(su1!=null){consumer="INVENTED_ALGORITHM";candidate=su1.candidate();structure=su1.program;certificate=su1.certificate;roster.append("|SU1_EXACT:").append(su1.certificate);}
        }
        if(consumer.isEmpty()&&rejections.isEmpty()){
            String latest="NO_CERTIFICATE_RECORD";
            try(Cursor last=store.readable().rawQuery("SELECT state,reason FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE
                    +" WHERE consumer='INVENTED_ALGORITHM' AND candidate_id LIKE 'R958-F-%' ORDER BY issued_at DESC,certificate_hash DESC LIMIT 1",null)){
                if(last.moveToFirst())latest="latest_state="+last.getString(0)+";original_reason="+last.getString(1);
            }
            String reason="NO_CERTIFICATE_MATCHED_CURRENT_PREFLIGHT|"+latest
                    +"|missing=ACTIVE,current_bundle,issued_before_capture,positive_weight,required_gates,supported_exact_consumer"
                    +"|recovery=run_independent_fixed_Future_and_calibration_then_actual_certificate_publisher";
            rejections.add(new String[]{"NO_CANDIDATE","",reason});roster.append('|').append(reason);
        }
        return new PreparedChoiceCP12(consumer,candidate,structure,certificate,V51ChecksumEngine.sha256(roster.toString()),rejections);
    }
    /** Reads the frozen choice and durable rejection facts; never grants authority or reruns a model. */
    public static String currentChoiceReasonCP12(DbStore store,String target){
        String consumer,candidate,certificate;
        try(Cursor choice=store.readable().rawQuery("SELECT consumer,candidate_id,certificate_hash FROM "+TABLE+" WHERE target_issue=?",new String[]{target})){
            if(!choice.moveToFirst())return "CHOICE_NOT_FROZEN|missing=durable_choice|recovery=retry_existing_freeze_owner";
            consumer=choice.getString(0);candidate=choice.getString(1);certificate=choice.getString(2);
        }
        if(consumer!=null&&!consumer.isEmpty()){
            try(Cursor cert=store.readable().rawQuery("SELECT state,reason FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" WHERE certificate_hash=?",new String[]{certificate})){
                if(cert.moveToFirst()&&"ACTIVE".equals(cert.getString(0)))return "FROZEN_CERTIFIED_CHOICE|candidate="+candidate+"|certificate="+certificate
                        +"|selection=LCB_DESC_BRIER_ASC_LATENCY_ASC_ID_HASH_ASC|pool_limit=NONE|remainder=NOT_PREFLIGHTED_AFTER_HIGHER_PRIORITY_SELECTION";
                return "NO_VALID_CERTIFICATE|raw="+(cert.getCount()==0?"FROZEN_CERTIFICATE_MISSING":cert.getString(0)+":"+cert.getString(1))
                        +"|missing=ACTIVE_exact_frozen_certificate|recovery=actual_publisher_with_independent_evidence";
            }
        }
        StringBuilder reason=new StringBuilder();int count=0;
        try(Cursor rejected=store.readable().rawQuery("SELECT candidate_id,certificate_hash,reason FROM r974_prediction_rejection WHERE target_issue=? ORDER BY recorded_at DESC,candidate_id,certificate_hash LIMIT 8",new String[]{target})){
            while(rejected.moveToNext()){if(count++>0)reason.append("; ");reason.append(rejected.getString(0)).append(':').append(rejected.getString(2));}
        }
        if(count==0)reason.append("HISTORICAL_EMPTY_CHOICE_NO_PREFLIGHT_RECEIPT");
        return "NO_VALID_CERTIFICATE|preflight="+(count==0?"NOT_RUN":"EXECUTED")+"|raw="+reason.toString()+"|missing=authorized_executable_exact_model|recovery=repair_recorded_dependency_then_freeze_next_target";
    }
    private static void publishFreezeStatus(DbStore store,String target,String status){
        try{
            store.setState("r932_choice_freeze_status",status);
            store.setState("r932_choice_freeze_target",target);
            store.setState("r932_choice_freeze_at",String.valueOf(System.currentTimeMillis()));
        }catch(Throwable ignored){}
    }
    public static final class Evidence {
        public final String id,structure,certificate,evidence;
        private final Map<String,Double> metrics;
        Evidence(String id,String structure,String certificate,String evidence,Map<String,Double> metrics){this.id=id;this.structure=structure;this.certificate=certificate;this.evidence=evidence;this.metrics=Collections.unmodifiableMap(metrics);}
        public double metric(String key){Double value=metrics.get(key);return value==null?Double.NaN:value;}
        public int count(String key){double n=metric(key);return Double.isFinite(n)?(int)n:0;}
    }
    public static Evidence selected(DbStore store,String target,CertificateBundleServiceR929.Consumer consumer) {
        if(consumer==CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM&&com.ethk3.novaonepro.strategyv2.StrategySchemaSU1.ready(store.readable())){
            try(Cursor choice=store.readable().rawQuery("SELECT candidate_id,certificate_hash FROM "+TABLE+" WHERE target_issue=? AND consumer=? AND candidate_id LIKE 'SU1-F-%'",new String[]{target,consumer.name()})){
                if(choice.moveToFirst()){
                    com.ethk3.novaonepro.strategyv2.CertificateSU1.Authority a=com.ethk3.novaonepro.strategyv2.CertificateSU1.read(store.readable(),choice.getString(1),System.currentTimeMillis(),true);
                    if(a==null||com.ethk3.novaonepro.strategyv2.MainlineAdapterSU1.authority(store,choice.getString(0),a.program,target,a.certificate)==null)return null;
                    Map<String,Double> metrics=new HashMap<String,Double>();
                    try(Cursor look=store.readable().rawQuery("SELECT planned_opportunities,paired_n,child_hits,coverage,paired_delta,metrics_json FROM su1_evidence_look WHERE evidence_hash=?",new String[]{a.evidence})){
                        if(!look.moveToFirst())return null;int n=look.getInt(1),k=look.getInt(2);metrics.put("FUTURE_N",(double)look.getInt(0));metrics.put("PAIRED_N",(double)n);metrics.put("HIT_RATE",n==0?Double.NaN:k/(double)n);metrics.put("WILSON_LOWER",n==0?Double.NaN:com.ethk3.novaonepro.strategyv2.FoldStabilityFitnessSU1.wilsonLower(k,n));metrics.put("COVERAGE",look.getDouble(3));metrics.put("PAIRED_DELTA",look.isNull(4)?Double.NaN:look.getDouble(4));
                        Map<String,Object> quality=com.ethk3.novaonepro.r952.StrictCanonicalJson.readObject(look.getString(5));metrics.put("BRIER",Double.valueOf((String)quality.get("brier")));metrics.put("ECE",Double.valueOf((String)quality.get("ece")));
                    }
                    return new Evidence(a.candidate(),a.program,a.certificate,a.evidence,metrics);
                }
            }
        }

        try(Cursor c=store.readable().rawQuery("SELECT c.candidate_id,c.structural_hash,c.certificate_hash,c.evidence_hash FROM "+TABLE+" s JOIN "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" c ON c.certificate_hash=s.certificate_hash WHERE s.target_issue=? AND s.consumer=? AND c.state='ACTIVE'",new String[]{target,consumer.name()})){
            if(!c.moveToFirst())return null;
            String id=c.getString(0),structure=c.getString(1),certificate=c.getString(2),evidence=c.getString(3);
            if(!ResearchEvaluationRunR967.consumerCompatible(store.readable(),id,target))return null;
            if(!ResearchPlatformStoreR929.authorizeCurrent(store,consumer,id,structure,target,.30).authorized)return null;
            Map<String,Double> values=new HashMap<String,Double>();
            try(Cursor gates=store.readable().rawQuery("SELECT gate_id,observed FROM "+ResearchPlatformStoreR929.TABLE_CERT_GATE+" WHERE certificate_hash=?",new String[]{certificate})){while(gates.moveToNext())values.put(gates.getString(0),gates.getDouble(1));}
            return new Evidence(id,structure,certificate,evidence,values);
        }catch(RuntimeException failure){if(failure instanceof ResearchWorkBudgetR927.Yield)throw failure;return null;}
    }
    public static String report(DbStore store) {
        StringBuilder text=new StringBuilder("\nR932 统一证书与唯一研究消费者\n当前CP4：正式Future至少360期且2窗口，研究排序影响上限5%；校准为120/1/120共241期；旧梯度仅历史协议。排序影响比例不是每期REAL频率帽。\n");
        try(Cursor rows=store.readable().rawQuery("SELECT candidate_id,consumer,state,reason,certificate_hash FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" ORDER BY issued_at DESC LIMIT 5",null)){
            int count=0;while(rows.moveToNext()){
                count++;text.append(rows.getString(0)).append(" | ").append(rows.getString(1)).append(" | ").append(rows.getString(2)).append(" | ").append(rows.getString(3)).append('\n');
                try(Cursor gates=store.readable().rawQuery("SELECT gate_id,observed,required,pass,source,source_hash FROM "+ResearchPlatformStoreR929.TABLE_CERT_GATE+" WHERE certificate_hash=? ORDER BY gate_id",new String[]{rows.getString(4)})){
                    int n=0;while(gates.moveToNext()){n++;text.append("  ").append(gates.getString(0)).append(" observed=").append(gates.getDouble(1)).append(" required=").append(gates.getDouble(2)).append(" pass=").append(gates.getInt(3)).append(" source=").append(gates.getString(4)).append('/').append(gates.getString(5)).append('\n');}
                    if(n==0)text.append("  无逐门原始证据：仅旧记录，不授予权重。\n");
                }
            }
            if(count==0)text.append("统一证书为0：尚无完整固定窗口、独立校准、试验注册和依赖审计证据；旧证书不自动带权。\n");
        }catch(RuntimeException missing){text.append("证书账本尚不可读：").append(missing.getClass().getSimpleName()).append('\n');}
        return text.toString();
    }

public static String reportSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        com.ethk3.novaonepro.ReportTextCP12 text=new com.ethk3.novaonepro.ReportTextCP12(report, "\nR932 统一证书与唯一研究消费者\n当前CP4：正式Future至少360期且2窗口，研究排序影响上限5%；校准为120/1/120共241期；旧梯度仅历史协议。排序影响比例不是每期REAL频率帽。\n");
        try(Cursor rows=report.select("SELECT candidate_id,consumer,state,reason,certificate_hash FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" ORDER BY issued_at DESC LIMIT 5",null)){
            int count=0;while(rows.moveToNext()){
                count++;text.append(rows.getString(0)).append(" | ").append(rows.getString(1)).append(" | ").append(rows.getString(2)).append(" | ").append(rows.getString(3)).append('\n');
                try(Cursor gates=report.select("SELECT gate_id,observed,required,pass,source,source_hash FROM "+ResearchPlatformStoreR929.TABLE_CERT_GATE+" WHERE certificate_hash=? ORDER BY gate_id",new String[]{rows.getString(4)})){
                    int n=0;while(gates.moveToNext()){n++;text.append("  ").append(gates.getString(0)).append(" observed=").append(gates.getDouble(1)).append(" required=").append(gates.getDouble(2)).append(" pass=").append(gates.getInt(3)).append(" source=").append(gates.getString(4)).append('/').append(gates.getString(5)).append('\n');}
                    if(n==0)text.append("  无逐门原始证据：仅旧记录，不授予权重。\n");
                }
            }
            if(count==0)text.append("统一证书为0：尚无完整固定窗口、独立校准、试验注册和依赖审计证据；旧证书不自动带权。\n");
        }catch(RuntimeException missing){throw new com.ethk3.novaonepro.ReportReadContextCP4.ReportReadFailureCP4("REPORT_QUERY_OR_FORMAT_FAILED",missing);}
        return text.toString();
    }
}
