package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;
import org.json.*;

/** Converts fixed terminal observations into the one existing certificate authority.
 * Audit pages and their cursor are atomic. Old score-only rows cannot be repaired after the draw.
 * No scheduling, prediction, settlement, policy unlock or REAL row is written here.
 */
public final class FixedWindowCertificatePublisherR932 {
    public static final String TABLE="research_window_publication_r932";
    public static final int PAGE=24;
    private FixedWindowCertificatePublisherR932() {}

    static void schema(SQLiteDatabase db) {
        if (TransactionSchemaCacheR941.ready(db,"FixedWindowCertificatePublisherR932")) return;

        CandidateExecutionAuditR932.schema(db);
        ResearchCaptureLedgerR932.schema(db);
        ResearchPlatformStoreR929.createSchema(db);
        db.execSQL("CREATE TABLE IF NOT EXISTS "+TABLE+"(window_id TEXT PRIMARY KEY,candidate_key TEXT NOT NULL,result_hash TEXT NOT NULL,state TEXT NOT NULL,reason TEXT NOT NULL,cursor_issue TEXT NOT NULL,verified_n INTEGER NOT NULL,proofs_hash TEXT NOT NULL,max_cost_ns INTEGER NOT NULL,certificate_hash TEXT NOT NULL,updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r932_publication_pending ON "+TABLE+"(state,window_id)");
    
        TransactionSchemaCacheR941.mark(db,"FixedWindowCertificatePublisherR932");
    }

    static boolean hasRunnableWork(SQLiteDatabase db) {
        if(ResearchGatePlanR967.installed().isCp3)return hasRunnableCp4(db);
        boolean installed;
        try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{TABLE})){installed=c.moveToFirst();}
        String sql=installed
                ?"SELECT 1 FROM "+FixedResearchWindowR932.WINDOW+" w LEFT JOIN "+TABLE+" p ON w.window_id=p.window_id WHERE w.state<>'OPEN' AND (p.window_id IS NULL OR p.state='AUDITING') LIMIT 1"
                :"SELECT 1 FROM "+FixedResearchWindowR932.WINDOW+" WHERE state<>'OPEN' LIMIT 1";
        // Without an actual draw there is no valid-from issue for a publication.
        try(Cursor c=db.rawQuery("SELECT issue FROM "+DbHelper.TABLE_DRAWS+" WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)){
            if(!c.moveToFirst()||!TargetIssueResolver.isValid(c.getString(0)))return false;
        }
        try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst();}
    }
    public static void maintain(final DbStore store) { maintainReviewed(store); }
    static String maintainReviewed(final DbStore store) {
        if(store==null)return "ERROR:STORE_NULL";
        if(RealtimePressureGateR811.hasActiveCriticalWork()||SettlementExecutionGateR819.isActive())return "YIELD_P0_PRESSURE";
        String latest="";
        try(Cursor c=BudgetedQueryR927.query(store.readable(),"SELECT issue FROM "+DbHelper.TABLE_DRAWS+" WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)){if(c.moveToFirst())latest=c.getString(0);}
        if(!TargetIssueResolver.isValid(latest))return "WAIT_HISTORY";
        final String next=TargetIssueResolver.next(latest).targetIssue;
        final PreparedCertificateCP12 prepared=ResearchGatePlanR967.installed().isCp3?prepareCertificateCP12(store.readable(),prepareMatrixPublicationCP12(store.readable())):null;
        String active=store.getState(ModelBundleManagerR929.STATE_ACTIVE,"");
        if(active.length()!=64)active=V51ArtifactBundleManifest.BUNDLE_HASH;
        try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.forEvidence(active,next,System.currentTimeMillis())) {
            boolean ok=store.coordinatorR36().runAtomicWrite("R932_WINDOW_AUDIT_PUBLICATION",new DatabaseCoordinator.WriteRunnable(){
                public void run(SQLiteDatabase db){advance(db,next,prepared);}
            });
            if(!ok)throw new IllegalStateException("R932_WINDOW_PUBLICATION_COMMIT_FAILED");
        }
        return hasRunnableWork(store.readable())?"CERTIFICATE_PENDING":"CERTIFICATE_WINDOWS_REVIEWED";
    }

    static void advance(SQLiteDatabase db,String validFrom) {advance(db,validFrom,null);}
    static void advance(SQLiteDatabase db,String validFrom,PreparedCertificateCP12 prepared) {
        if(ResearchGatePlanR967.installed().isCp3){advanceCp4(db,prepared);return;}
        if(!db.inTransaction())throw new IllegalStateException("R932_WINDOW_PUBLICATION_TRANSACTION");
        FixedResearchWindowR932.schema(db);
        schema(db);
        String id=null,key=null,structure=null,bundle=null,selector=null,first=null,last=null,status=null,resultHash=null;
        int planned=0,number=0;long registered=0;
        try(Cursor c=db.rawQuery("SELECT w.window_id,w.candidate_key,w.structural_hash,w.champion_hash,w.selection_hash,w.first_issue,w.last_issue,w.planned_n,w.window_no,w.registered_at,w.state,w.result_hash FROM "+FixedResearchWindowR932.WINDOW+" w LEFT JOIN "+TABLE+" p ON w.window_id=p.window_id WHERE w.state<>'OPEN' AND (p.window_id IS NULL OR p.state='AUDITING') ORDER BY w.last_issue,w.window_id LIMIT 1",null)){
            if(!c.moveToFirst())return;
            id=c.getString(0);key=c.getString(1);structure=c.getString(2);bundle=c.getString(3);selector=c.getString(4);first=c.getString(5);last=c.getString(6);planned=c.getInt(7);number=c.getInt(8);registered=c.getLong(9);status=c.getString(10);resultHash=c.getString(11);
        }
        db.execSQL("INSERT OR IGNORE INTO "+TABLE+" VALUES(?,?,?,'AUDITING','VERIFYING_FROZEN_EXECUTION','',0,'',0,'',?)",new Object[]{id,key,resultHash,System.currentTimeMillis()});
        if(!"PASS".equals(status)){block(db,id,"WINDOW_"+status);return;}
        if(!ModelBundleRuntimeR932.current().hash.equals(bundle)||!SelectionEvidenceContractR929.installedPolicy().hash.equals(selector)){
            block(db,id,"FROZEN_BUNDLE_OR_CHAMPION_SELECTOR_CHANGED");return;
        }
        String contract="";try(Cursor c=db.rawQuery("SELECT contract_hash FROM "+FixedResearchWindowR932.CONTRACT+" WHERE window_id=?",new String[]{id})){if(c.moveToFirst())contract=c.getString(0);}
        if(!contract.equals(FixedResearchWindowR932.contract(db))){block(db,id,"ATOM_SCHEMA_SOURCE_DEPENDENCY_CHANGED");return;}
        JSONObject metrics;long finished;
        try(Cursor c=db.rawQuery("SELECT s.metrics_json,s.result_hash,s.created_at,l.state,l.result_hash,l.first_issue,l.last_issue FROM "+FixedWindowStatisticsR932.TABLE+" s JOIN "+ResearchMultiplicityR932.LOOK+" l ON l.window_id=s.window_id WHERE s.window_id=?",new String[]{id})) {
            if(!c.moveToFirst()||!resultHash.equals(c.getString(1))||!resultHash.equals(V51ChecksumEngine.sha256(c.getString(0)))
                    ||!"PASS".equals(c.getString(3))||!resultHash.equals(c.getString(4))||!first.equals(c.getString(5))||!last.equals(c.getString(6))) {
                block(db,id,"FIXED_TERMINAL_EVIDENCE_HASH_OR_LOOK_MISMATCH");return;
            }
            try{metrics=new JSONObject(c.getString(0));}catch(JSONException e){throw new IllegalStateException("R932_WINDOW_METRICS_JSON",e);}
            finished=c.getLong(2);
        }
        ResearchGatePlanR967.Plan frozenPlan=ResearchGatePlanR967.forWindow(db,id,false);
        if(frozenPlan!=null&&(!frozenPlan.hash.equals(metrics.optString("plan_hash"))||!frozenPlan.policy.hash.equals(metrics.optString("policy_hash")))){
            block(db,id,"IMMUTABLE_EXPERIMENT_PLAN_MISMATCH");return;
        }
        if(frozenPlan==null&&metrics.optString("plan_hash","").matches("[0-9a-f]{64}")){
            block(db,id,"IMMUTABLE_EXPERIMENT_PLAN_MISSING");return;
        }
        if(!id.equals(metrics.optString("window_id"))||metrics.optInt("paired_n")!=planned||!"PASS".equals(metrics.optString("state"))){block(db,id,"TERMINAL_METRICS_IDENTITY");return;}
        String cursor="",chain="";int n=0;long max=0;
        try(Cursor c=db.rawQuery("SELECT cursor_issue,verified_n,proofs_hash,max_cost_ns,result_hash FROM "+TABLE+" WHERE window_id=?",new String[]{id})){
            if(!c.moveToFirst()||!resultHash.equals(c.getString(4)))throw new IllegalStateException("R932_PUBLICATION_IDENTITY_DRIFT");
            cursor=c.getString(0);n=c.getInt(1);chain=c.getString(2);max=c.getLong(3);
        }
        String atom=CalibrationRuntimeIdentityF04.executionScope(key,structure,bundle),code=CalibrationRuntimeIdentityF04.numericHash(),schema=V51ActionMetaFeatureEngine.featureManifestHash();
        int read=0;
        try(Cursor c=db.rawQuery("SELECT p.target_issue,p.candidate_hash,p.position,p.n1,p.n2,p.n3,e.structural_hash,e.base_issue,e.prediction_hash,e.code_hash,e.schema_hash,e.atom_graph_hash,e.action_count,e.max_error,e.elapsed_ns,e.payload,e.proof_hash,o.frozen,o.full1200,o.record_hash FROM "+FixedResearchWindowR932.PAIR+" p LEFT JOIN "+CandidateExecutionAuditR932.TABLE+" e ON e.candidate_key=? AND e.target_issue=p.target_issue LEFT JOIN "+ResearchCaptureLedgerR932.TABLE+" o ON o.candidate_key=? AND o.target_issue=p.target_issue WHERE p.window_id=? AND p.target_issue>? ORDER BY p.target_issue LIMIT "+PAGE,new String[]{key,key,id,cursor})) {
            while(c.moveToNext()) {
                ResearchWorkBudgetR927.checkpoint();read++;
                String issue=c.getString(0),expected=cursor.length()==0?first:ResearchEvaluationRunR967.nextEvaluation(key,cursor);
                if(!expected.equals(issue)||c.isNull(6)||!structure.equals(c.getString(6))||!TargetIssueResolver.exact(c.getString(7),issue).valid
                        ||!c.getString(1).equals(c.getString(8))||!schema.equals(c.getString(10))
                        ||c.getInt(12)!=1200||!Double.isFinite(c.getDouble(13))||c.getDouble(13)<0||c.getLong(14)<=0
                        ||c.isNull(17)||c.getInt(17)!=1||c.getInt(18)!=1||!c.getString(1).equals(c.getString(19))) {
                    block(db,id,"MISSING_OR_INCOMPATIBLE_PREDRAW_FULL1200_EXECUTION_PROOF");return;
                }
                String payload=c.getString(15),proof=c.getString(16);
                if(!V51ChecksumEngine.sha256(payload).equals(proof)){block(db,id,"EXECUTION_PROOF_HASH_MISMATCH");return;}
                try {
                    JSONObject p=new JSONObject(payload);
                    if(!CalibrationRuntimeIdentityF04.REVISION.equals(p.optString("semantic_revision",""))){block(db,id,"F04_LEGACY_SCOPE_PROOF_REQUIRED");return;}
                    int ordinal=K3ActionSpaceEngine.globalOrdinal(c.getInt(2),c.getInt(3),c.getInt(4),c.getInt(5));
                    if(!CandidateExecutionAuditR932.REVISION.equals(p.getString("revision"))||!key.equals(p.getString("candidate_key"))||!issue.equals(p.getString("target_issue"))
                            ||!structure.equals(p.getString("structural_hash"))||!c.getString(1).equals(p.getString("prediction_hash"))
                            ||ordinal!=p.getInt("selected_ordinal")||p.getInt("action_count")!=1200||p.getBoolean("may_authorize_real")
                            ||!bundle.equals(p.getString("model_bundle_hash"))
                            ||!c.getString(11).equals(p.getString("atom_graph_hash"))||!c.getString(9).equals(p.getString("code_hash"))
                            ||!p.getString("code_hash").equals(p.getString("build_source_hash"))
                            ||!atom.equals(p.getString("execution_scope_hash"))||!code.equals(p.getString("effective_numeric_hash"))
                            ||!schema.equals(p.getString("schema_hash"))) {
                        block(db,id,"EXECUTION_PAYLOAD_IDENTITY_MISMATCH");return;
                    }
                }catch(JSONException error){throw new IllegalStateException("R932_EXECUTION_PROOF_JSON",error);}
                max=Math.max(max,c.getLong(14));n++;cursor=issue;chain=V51ChecksumEngine.sha256(chain+'|'+issue+'|'+proof);
            }
        }
        db.execSQL("UPDATE "+TABLE+" SET cursor_issue=?,verified_n=?,proofs_hash=?,max_cost_ns=?,updated_at=? WHERE window_id=? AND state='AUDITING'",new Object[]{cursor,n,chain,max,System.currentTimeMillis(),id});
        if(n<planned&&read==PAGE)return;
        if(n!=planned||!last.equals(cursor)){block(db,id,"FULL1200_CAPTURE_DENOMINATOR_INCOMPLETE");return;}
        if(max>GlobalResearchScheduleR932.SLICE_MS*1000000L){block(db,id,"CANDIDATE_REPLAY_RESOURCE_LIMIT_EXCEEDED");return;}
        // Publication is deliberately later than the immutable terminal evaluation.
        if(System.currentTimeMillis()<=finished)return;
        publish(db,id,key,structure,bundle,selector,first,last,number,registered,finished,metrics,validFrom);
    }

    private static void publish(SQLiteDatabase db,String id,String key,String structure,String bundle,String selector,
            String first,String last,int number,long registered,long finished,JSONObject latest,String validFrom) {
        int n=0,windows=0,hits=0,baseHits=0,planned=0;double brier=0,ece=0,adjusted=0;StringBuilder sources=new StringBuilder();
        String previousLast="";
        ObservationGenerationR932.Chain generation=ObservationGenerationR932.of(db,key,id,number);
        try(Cursor c=db.rawQuery("SELECT w.window_id,w.window_no,w.first_issue,w.last_issue,w.planned_n,w.structural_hash,w.champion_hash,w.selection_hash,w.state,p.verified_n,p.proofs_hash,p.state,s.metrics_json,s.result_hash FROM "+FixedResearchWindowR932.WINDOW+" w JOIN "+TABLE+" p ON p.window_id=w.window_id JOIN "+FixedWindowStatisticsR932.TABLE+" s ON s.window_id=w.window_id WHERE w.candidate_key=? AND w.window_no<=? AND w.window_no>=? ORDER BY w.window_no LIMIT 5",new String[]{key,String.valueOf(number),String.valueOf(generation.start)})) {
            while(c.moveToNext()) {
                ResearchWorkBudgetR927.checkpoint();
                if(c.getInt(1)!=generation.start+windows||!structure.equals(c.getString(5))||!bundle.equals(c.getString(6))||!selector.equals(c.getString(7))
                        ||!"PASS".equals(c.getString(8))||c.getInt(4)!=c.getInt(9)||(!id.equals(c.getString(0))&&!"PUBLISHED".equals(c.getString(11)))
                        ||previousLast.length()>0&&TargetIssueResolver.compare(c.getString(2),previousLast)<=0) {
                    block(db,id,"PREVIOUS_INDEPENDENT_WINDOW_NOT_AUDITED_OR_PUBLISHED");return;
                }
                try {
                    if(!V51ChecksumEngine.sha256(c.getString(12)).equals(c.getString(13)))throw new IllegalStateException("R932_PRIOR_WINDOW_HASH");
                    JSONObject m=new JSONObject(c.getString(12));int count=m.getInt("paired_n");
                    if(count!=c.getInt(4)||!"PASS".equals(m.getString("state")))throw new IllegalStateException("R932_PRIOR_WINDOW_COUNT");
                    n+=count;planned+=c.getInt(4);hits+=m.getInt("candidate_hits");baseHits+=m.getInt("champion_hits");
                    brier+=count*m.getDouble("brier");ece+=count*m.getDouble("ece");adjusted=Math.max(adjusted,m.getDouble("fwer_adjusted_p"));
                }catch(JSONException e){throw new IllegalStateException("R932_PRIOR_WINDOW_METRICS",e);}
                ObservationGenerationR932.Chain member=ObservationGenerationR932.of(db,key,c.getString(0),c.getInt(1));
                if(!generation.generation.equals(member.generation)||!generation.claim.equals(member.claim)||member.stage!=windows+1){block(db,id,"OBSERVATION_GENERATION_MIXED");return;}
                sources.append('|').append(c.getString(0)).append(':').append(c.getString(10)).append(':').append(c.getString(13));
                previousLast=c.getString(3);windows++;
            }
        }
        if(windows!=generation.stage){block(db,id,"INDEPENDENT_WINDOW_CHAIN_INCOMPLETE");return;}
        String family="",hypothesis="";
        try(Cursor c=db.rawQuery("SELECT h.family,h.hypothesis_hash,h.structure_hash FROM "+ResearchMultiplicityR932.IDENTITY+" h WHERE h.candidate_key=?",new String[]{key})){
            if(!c.moveToFirst()||!structure.equals(c.getString(2))){block(db,id,"PREREGISTERED_HYPOTHESIS_MISSING");return;}
            family=c.getString(0);hypothesis=c.getString(1);
        }
        String calibration=latest.optString("calibration_hash");
        try(Cursor c=db.rawQuery("SELECT model_json,model_hash,validation_until FROM "+CandidateCalibrationR932.MODEL+" WHERE candidate_key=? AND model_hash=?",new String[]{key,calibration})){
            if(!c.moveToFirst()||!calibration.equals(V51ChecksumEngine.sha256(c.getString(0)))||TargetIssueResolver.compare(c.getString(2),first)>=0){block(db,id,"EARLIER_VALIDATED_CANDIDATE_CALIBRATION_MISSING");return;}
        }
        String evidence=V51ChecksumEngine.sha256("R932_PUBLISHED_FIXED_WINDOWS|"+sources),schema=V51ActionMetaFeatureEngine.featureManifestHash(),code=SourceManifestR815.SOURCE_TREE_SHA256;
        ExperimentProtocolR929.TrialIdentity identity=new ExperimentProtocolR929.TrialIdentity(V51Repository.GAME_CODE,schema,
            V51ChecksumEngine.sha256("PREREGISTERED_FUTURE_ISSUES|"+first+'|'+last),code,hypothesis,0L,family,structure,ExperimentProtocolR929.Stage.EVALUATE,id,Collections.<ExperimentProtocolR929.ArtifactDependency>emptyList());
        ExperimentProtocolR929.TrialRecord trial=new ExperimentProtocolR929.TrialRecord(identity,ExperimentProtocolR929.TrialStatus.COMPLETED,registered,registered,finished,evidence,"",false,"");
        ContentValues values=ResearchPlatformStoreR929.trialValues(trial);
        db.insertWithOnConflict(ResearchPlatformStoreR929.TABLE_TRIAL,null,values,SQLiteDatabase.CONFLICT_IGNORE);
        try(Cursor c=db.rawQuery("SELECT record_hash FROM "+ResearchPlatformStoreR929.TABLE_TRIAL+" WHERE trial_id=?",new String[]{identity.trialId})){
            if(!c.moveToFirst()||!trial.recordHash.equals(c.getString(0)))throw new IllegalStateException("R932_WINDOW_TRIAL_PROJECTION_CONFLICT");
        }
        double rate=hits/(double)n,z=1.959963984540054;
        double lower=(rate+z*z/(2*n)-z*Math.sqrt(rate*(1-rate)/n+z*z/(4d*n*n)))/(1+z*z/n);
        CertificateBundleServiceR929.EvidenceInput input=new CertificateBundleServiceR929.EvidenceInput(true,true,true,n,n,windows,rate,lower,n/(double)planned,brier/n,ece/n,(hits-baseHits)/(double)n,adjusted,true,true,evidence);
        CertificateBundleServiceR929.Evaluation evaluation=CertificateBundleServiceR929.evaluate(input);
        ArrayList<CertificateBundleServiceR929.GateObservation> gates=new ArrayList<CertificateBundleServiceR929.GateObservation>();
        for(CertificateBundleServiceR929.GateObservation gate:evaluation.gates) {
            String detail=gate.gateId.equals("PARITY")?"1200_ACTION_SERIALIZATION_RUNTIME_PARITY; DEVICE_PARITY_SEPARATE":
                gate.gateId.equals("DOMAIN_SUPPORT")?"GLOBAL_CANDIDATE_CAL_VALIDATION_BEFORE_ALL_FUTURE_WINDOWS; NOT_A_DOMAIN_SPECIFIC_ADVANTAGE_CLAIM":
                gate.gateId.equals("COVERAGE")?"FULL_PREDRAW_CAPTURE / ALL_PREREGISTERED_WINDOW_ISSUES; NOT_REAL_BET_FREQUENCY":
                gate.gateId.equals("ECE")?"WEIGHTED_WINDOW_ECE_UPPER_BOUND; EACH_WINDOW_INDEPENDENTLY_PASSED":gate.detail;
            gates.add(new CertificateBundleServiceR929.GateObservation(gate.gateId,gate.observed,gate.required,gate.pass,"R932_FIXED_WINDOW_EXECUTION_LEDGER",evidence,detail));
        }
        CertificateBundleServiceR929.CandidateKey candidate=candidateKey(key,family,structure);
        CertificateBundleServiceR929.Certificate certificate=new CertificateBundleServiceR929.Certificate(candidate,evaluation.level,CertificateBundleServiceR929.CertificateState.ACTIVE,
            bundle,schema,AtomCaptureR932.graphHash(db),CertificateDependenciesR932.modelHash(),CertificateDependenciesR932.component("POLICY"),CertificateDependenciesR932.component("CALIBRATION"),
            DomainRouterR929.installedSchemaHash(),trial.recordHash,CertificateDependenciesR932.selectionHash(),code,validFrom,"",System.currentTimeMillis(),evaluation.weightCap,gates,
            "FIXED_WINDOWS_"+windows+"_FUTURE_N_"+n+"; FINAL_MIXED_ACTION_CALIBRATION_REMAINS_REQUIRED; NO_REAL_AUTHORITY");
        if(!ResearchPlatformStoreR929.putCertificate(db,certificate,evidence))throw new IllegalStateException("R932_SHARED_CERTIFICATE_PUBLICATION_REJECTED");
        db.execSQL("UPDATE "+TABLE+" SET state='PUBLISHED',reason=?,certificate_hash=?,updated_at=? WHERE window_id=? AND state='AUDITING'",new Object[]{evaluation.reason,certificate.certificateHash,System.currentTimeMillis(),id});
    }

    static CertificateBundleServiceR929.CandidateKey candidateKey(String key,String family,String structure) {
        int colon=key.indexOf(':');if(colon<1||colon==key.length()-1)throw new IllegalArgumentException("R932_PUBLICATION_CANDIDATE_KEY");
        String namespace=key.substring(0,colon);CertificateBundleServiceR929.Consumer consumer;
        if(namespace.equals("algorithm"))consumer=CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM;
        else if(namespace.equals("genesis"))consumer=CertificateBundleServiceR929.Consumer.GENESIS_ASSET;
        else if(namespace.equals("formula"))consumer=CertificateBundleServiceR929.Consumer.FORMULA_GRAPH;
        else throw new IllegalArgumentException("R932_PUBLICATION_CONSUMER");
        return new CertificateBundleServiceR929.CandidateKey(consumer,"R932_LIVE_"+namespace,family,key.substring(colon+1),structure);
    }
    private static void block(SQLiteDatabase db,String id,String reason) {
        db.execSQL("UPDATE "+TABLE+" SET state='BLOCKED',reason=?,updated_at=? WHERE window_id=? AND state='AUDITING'",new Object[]{reason,System.currentTimeMillis(),id});
    }
    public static String report(DbStore store) {
        StringBuilder out=new StringBuilder("\n固定窗口统一证书发布\n");
        try(Cursor exists=store.readable().rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{TABLE})) {if(!exists.moveToFirst())return out.append("尚未运行窗口发布审计。\n").toString();}
        try(Cursor c=store.readable().rawQuery("SELECT candidate_key,state,reason,verified_n,max_cost_ns FROM "+TABLE+" ORDER BY updated_at DESC LIMIT 8",null)){
            while(c.moveToNext())out.append(c.getString(0)).append("：").append(c.getString(1)).append("；已核对 ").append(c.getInt(3)).append(" 期；最大重放 ").append(c.getLong(4)/1000000d).append("ms；").append(c.getString(2)).append('\n');
        }
        return out.toString();
    }

    static boolean hasRunnableCp4(SQLiteDatabase db){
        try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{ResearchGatePlanR967.CP4_BINDING})){if(!c.moveToFirst())return false;}
        try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{TABLE})){if(!c.moveToFirst())return true;}
        try(Cursor c=db.rawQuery("SELECT 1 FROM "+ResearchGatePlanR967.CP4_BINDING+" b JOIN "+FixedResearchWindowR932.WINDOW+" w ON w.window_id=b.window_id LEFT JOIN "+TABLE+" p ON p.window_id=b.window_id WHERE "+runnableCp4()+" LIMIT 1",new String[]{String.valueOf(System.currentTimeMillis()-60000L)})){return c.moveToFirst();}
    }
    static void advanceCp4(SQLiteDatabase db){advanceCp4(db,(PreparedCertificateCP12)null);}
    static void advanceCp4(SQLiteDatabase db,PreparedCertificateCP12 prepared){
        if(!db.inTransaction())throw new IllegalStateException("CP4_PUBLICATION_TX_REQUIRED");if(prepared==null)return;
        PreparedMatrixPublicationCP12 p=prepared.selection;
        try(Cursor c=db.rawQuery("SELECT b.lifetime_id,b.stage,l.identity_hash,w.state,w.result_hash,w.structural_hash FROM "+ResearchGatePlanR967.CP4_BINDING+" b JOIN "+ResearchGatePlanR967.CP4_LIFETIME+" l ON l.lifetime_id=b.lifetime_id JOIN "+FixedResearchWindowR932.WINDOW+" w ON w.window_id=b.window_id WHERE b.window_id=?",new String[]{p.window})){
            if(!c.moveToFirst()||!p.lifetime.equals(c.getString(0))||p.stage!=c.getInt(1)||!p.identityHash.equals(c.getString(2))||!p.windowState.equals(c.getString(3))||!p.resultHash.equals(c.getString(4))||!p.structure.equals(c.getString(5)))throw new IllegalStateException("CP12_CERTIFICATE_SELECTION_CHANGED");
        }
        if(!prepared.sourceFence.equals(certificateSourceFenceCP12(db)))throw new IllegalStateException("CP12_CERTIFICATE_SOURCE_CHANGED");
        if(CertificateDependenciesR932.hasFeedbackRejectionCP12(db,p.lifetime))throw new IllegalStateException("CP12_FEEDBACK_INTEGRITY");
        if(prepared.selection.reason.isEmpty()&&!com.ethk3.novaonepro.r952.MatrixPolicyViewsCP12.fenceCP12(db,p.structure,p.endpoint,p.views))throw new IllegalStateException("CP12_MATRIX_FENCE_CHANGED");
        db.execSQL("INSERT OR IGNORE INTO "+TABLE+" VALUES(?,?,?,'AUDITING','CP12_PREPARED_PUBLICATION','',0,'',0,'',?)",new Object[]{p.window,prepared.key,p.resultHash,System.currentTimeMillis()});
        {MultipleComparisonRegistryR930.Snapshot current=MultipleComparisonRegistryR930.readCp3WithinTransaction(db,prepared.registration.hypothesisId);if(current==null||current.state!=prepared.hypothesisState||Double.compare(current.alpha,prepared.alpha)!=0)throw new IllegalStateException("CP12_MULTIPLICITY_FENCE_CHANGED");if(prepared.decide)MultipleComparisonRegistryR930.decideCp3WithinTransaction(db,prepared.registration,prepared.p,prepared.evidence);}
        if(prepared.certificate!=null){db.insertWithOnConflict(ResearchPlatformStoreR929.TABLE_TRIAL,null,ResearchPlatformStoreR929.trialValues(prepared.trial),SQLiteDatabase.CONFLICT_IGNORE);if(!ResearchPlatformStoreR929.putCertificate(db,prepared.certificate,prepared.evidence))throw new IllegalStateException("CP4_CERTIFICATE_STORE_REJECTED");}
        publicationCp4(db,p.window,prepared.state,prepared.reason,prepared.n,prepared.metrics);
        if(prepared.certificate!=null){
            db.execSQL("UPDATE "+TABLE+" SET certificate_hash=? WHERE window_id=? AND state='PUBLISHED'",new Object[]{prepared.certificate.certificateHash,p.window});
            try(Cursor verify=db.rawQuery("SELECT state,certificate_hash FROM "+TABLE+" WHERE window_id=?",new String[]{p.window})){
                if(!verify.moveToFirst()||!"PUBLISHED".equals(verify.getString(0))
                        ||!prepared.certificate.certificateHash.equals(verify.getString(1)))
                    throw new IllegalStateException("F05_CERTIFICATE_PUBLICATION_RECEIPT_MISMATCH");
            }
        }
    }
    static final class PreparedCertificateCP12 {
        final PreparedMatrixPublicationCP12 selection;final String sourceFence,state,reason,metrics,key,evidence;final int n;final MultipleComparisonRegistryR930.Registration registration;final MultipleComparisonRegistryR930.State hypothesisState;final double alpha,p;final boolean decide;
        final ExperimentProtocolR929.TrialRecord trial;final CertificateBundleServiceR929.Certificate certificate;
        PreparedCertificateCP12(PreparedMatrixPublicationCP12 selection,String fence,String state,String reason,int n,String metrics,String key,MultipleComparisonRegistryR930.Registration registration,MultipleComparisonRegistryR930.Snapshot hypothesis,double p,String evidence,boolean decide){this.selection=selection;sourceFence=fence;this.state=state;this.reason=reason;this.n=n;this.metrics=metrics;this.key=key;this.registration=registration;hypothesisState=hypothesis==null?null:hypothesis.state;alpha=hypothesis==null?0:hypothesis.alpha;this.p=p;this.evidence=evidence;this.decide=decide;trial=null;certificate=null;}
        private PreparedCertificateCP12(PreparedCertificateCP12 base,ExperimentProtocolR929.TrialRecord trial,CertificateBundleServiceR929.Certificate certificate){selection=base.selection;sourceFence=base.sourceFence;state=base.state;reason=base.reason;metrics=base.metrics;key=base.key;evidence=base.evidence;n=base.n;registration=base.registration;hypothesisState=base.hypothesisState;alpha=base.alpha;p=base.p;decide=base.decide;this.trial=trial;this.certificate=certificate;}
    }
    static PreparedCertificateCP12 finishCertificatePrepareCP12(SQLiteDatabase db,PreparedMatrixPublicationCP12 selection,String sourceFence,String state,String reason,int n,String metrics){
        if(!sourceFence.equals(certificateSourceFenceCP12(db)))throw new IllegalStateException("CP12_CERTIFICATE_SOURCE_CHANGED_DURING_PREPARE");String key="";try(Cursor c=db.rawQuery("SELECT candidate_key FROM "+ResearchGatePlanR967.CP4_LIFETIME+" WHERE lifetime_id=?",new String[]{selection.lifetime})){if(c.moveToFirst())key=c.getString(0);}MultipleComparisonRegistryR930.Registration r=ResearchGatePlanR967.registrationForWindowCp4(db,selection.window);MultipleComparisonRegistryR930.Snapshot h=MultipleComparisonRegistryR930.readCp3WithinTransaction(db,r.hypothesisId);double p=0;String evidence="";boolean decide=false;
        if(!metrics.isEmpty())try{JSONObject m=new JSONObject(metrics);p=m.getDouble("anytime_p");evidence=m.getString("evidence_hash");decide=h!=null&&h.state==MultipleComparisonRegistryR930.State.OBSERVING&&(state.equals("PUBLISHED")||selection.stage==5&&state.equals("WAIT_EXTERNAL_EVIDENCE"));}catch(JSONException e){throw new IllegalStateException("CP12_PREPARED_METRICS",e);}
        return new PreparedCertificateCP12(selection,sourceFence,state,reason,n,metrics,key,r,h,p,evidence,decide);
    }
    /** One aggregate of monotonic revision IDs/counts. No execution JSON or model math in the writer. */
    private static String certificateSourceFenceCP12(SQLiteDatabase db){String tables="'research_fixed_window_r932','research_fixed_pair_r932','research_cp4_window_binding','research_cp4_frozen_lifetime','research_fixed_window_statistics_r932','research_candidate_execution_r932','research_capture_opportunity_r932','research_cp4_candidate_resource','draws','v57_settlement_task_r36','"+Batch34SchemaR930.TABLE_HYPOTHESIS+"','"+ResearchGatePlanR967.POLICY+"'";try(Cursor c=db.rawQuery("SELECT COUNT(*),COALESCE(MAX(generation),0) FROM research_row_revision_v2 WHERE source_table IN("+tables+")",null)){if(!c.moveToFirst())throw new IllegalStateException("CP12_CERTIFICATE_REVISION_FENCE");return V51ChecksumEngine.sha256("CP12_CERT_SOURCE|"+c.getLong(0)+'|'+c.getLong(1));}}
    static PreparedCertificateCP12 prepareCertificateCP12(SQLiteDatabase db,PreparedMatrixPublicationCP12 prepared){
        if(db.inTransaction())throw new IllegalStateException("CP12_CERTIFICATE_PREPARE_IN_WRITER");if(prepared==null)return null;
        String sourceFence=certificateSourceFenceCP12(db);
        String window="",lifetime="",identityJson="",identityHash="",key="",windowState="",endpoint="",resultHash="";int stage=0;long registered=0;
        try(Cursor c=db.rawQuery("SELECT b.window_id,b.lifetime_id,b.stage,b.last_issue,l.identity_json,l.identity_hash,l.candidate_key,l.created_at,w.state,w.result_hash FROM "+ResearchGatePlanR967.CP4_BINDING+" b JOIN "+ResearchGatePlanR967.CP4_LIFETIME+" l ON l.lifetime_id=b.lifetime_id JOIN "+FixedResearchWindowR932.WINDOW+" w ON w.window_id=b.window_id LEFT JOIN "+TABLE+" p ON p.window_id=b.window_id WHERE "+runnableCp4()+" ORDER BY b.last_issue,b.window_id LIMIT 1",new String[]{String.valueOf(System.currentTimeMillis()-60000L)})){
            if(!c.moveToFirst())return null;window=c.getString(0);lifetime=c.getString(1);stage=c.getInt(2);endpoint=c.getString(3);identityJson=c.getString(4);identityHash=c.getString(5);key=c.getString(6);registered=c.getLong(7);windowState=c.getString(8);resultHash=c.getString(9);
        }
        if(!"PASS".equals(windowState)&&!"OPEN".equals(windowState)){return finishCertificatePrepareCP12(db,prepared,sourceFence,"BLOCKED","WINDOW_"+windowState,0,"");}
        if(CertificateDependenciesR932.hasFeedbackRejectionCP12(db,lifetime)){return finishCertificatePrepareCP12(db,prepared,sourceFence,"BLOCKED","CP12_FEEDBACK_INTEGRITY",0,"");}
        ResearchGatePlanR967.Plan plan=ResearchGatePlanR967.forWindow(db,window,true);
        MultipleComparisonRegistryR930.Registration registration=ResearchGatePlanR967.registrationForWindowCp4(db,window);
        MultipleComparisonRegistryR930.Snapshot hypothesis=MultipleComparisonRegistryR930.readCp3WithinTransaction(db,registration.hypothesisId);
        if(hypothesis==null)throw new IllegalStateException("CP4_HYPOTHESIS_MISSING");
        final boolean renewal=hypothesis.state==MultipleComparisonRegistryR930.State.ACCEPTED;
        if("OPEN".equals(windowState)){
            if(!renewal)throw new IllegalStateException("CP4_OPEN_PREFIX_REQUIRES_PRIOR_ACCEPTANCE");
            try(Cursor prefix=db.rawQuery("SELECT MAX(x.target_issue) FROM "+ResearchGatePlanR967.CP4_BINDING+" b JOIN "+FixedResearchWindowR932.PAIR+" x ON x.window_id=b.window_id WHERE b.lifetime_id=? AND b.stage<=? AND x.candidate_hit IN (0,1) AND x.champion_hit IN (0,1)",new String[]{lifetime,String.valueOf(stage)})){if(!prefix.moveToFirst()||prefix.isNull(0))return null;endpoint=prefix.getString(0);}
        }
        if(prepared==null||!prepared.window.equals(window)||!prepared.lifetime.equals(lifetime)||prepared.stage!=stage||!prepared.endpoint.equals(endpoint)||!prepared.identityHash.equals(identityHash)||!prepared.resultHash.equals(resultHash)||!prepared.windowState.equals(windowState)) {return finishCertificatePrepareCP12(db,prepared,sourceFence,"WAIT_EXTERNAL_EVIDENCE","WAIT_MATRIX_PUBLICATION_PREPARE",0,"");}
        if(!prepared.reason.isEmpty()){return finishCertificatePrepareCP12(db,prepared,sourceFence,"WAIT_EXTERNAL_EVIDENCE",prepared.reason,0,"");}
        try{
            JSONObject identity=new JSONObject(identityJson);
            ArrayList<Integer> hits=new ArrayList<Integer>();ArrayList<Double> probs=new ArrayList<Double>();
            StringBuilder proof=new StringBuilder(identityHash);String expected=registration.windowStartIssue,last="";
            int completedWindows=0,prefixWindows=0,planned=0;double minWindowRate=1;boolean openPrefix=false;
            try(Cursor c=db.rawQuery("SELECT b.window_id,b.stage,b.planned_n,w.state,s.metrics_json,s.result_hash,w.result_hash FROM "+ResearchGatePlanR967.CP4_BINDING+" b JOIN "+FixedResearchWindowR932.WINDOW+" w ON w.window_id=b.window_id LEFT JOIN "+FixedWindowStatisticsR932.TABLE+" s ON s.window_id=b.window_id WHERE b.lifetime_id=? AND b.stage<=? ORDER BY b.stage",new String[]{lifetime,String.valueOf(stage)})){
                while(c.moveToNext()){ResearchWorkBudgetR927.checkpoint();
                    if(c.getInt(1)!=++prefixWindows)throw new IllegalStateException("CP4_PREFIX_STAGE_ORDER");
                    if("OPEN".equals(c.getString(3))&&renewal&&prefixWindows==stage){openPrefix=true;continue;}
                    completedWindows++;
                    if(!"PASS".equals(c.getString(3))||c.isNull(4)||!V51ChecksumEngine.sha256(c.getString(4)).equals(c.getString(5))||!c.getString(5).equals(c.getString(6)))throw new IllegalStateException("CP4_PREFIX_ENDPOINT_INVALID");
                    JSONObject m=new JSONObject(c.getString(4));if(m.getInt("paired_n")!=c.getInt(2)||!m.getString("policy_hash").equals(plan.policy.hash))throw new IllegalStateException("CP4_PREFIX_COUNT_OR_POLICY");
                    minWindowRate=Math.min(minWindowRate,m.getInt("candidate_hits")/(double)c.getInt(2));planned+=c.getInt(2);proof.append('|').append(c.getString(5));
                }
            }
            if(prefixWindows!=stage)throw new IllegalStateException("CP4_PREFIX_STAGE_GAP");
            try(Cursor c=db.rawQuery("SELECT p.target_issue,p.candidate_hit,p.selected_hit_p,p.calibration_hash,p.candidate_hash,e.payload,e.proof_hash,e.base_issue,e.action_count,o.frozen,o.full1200,o.record_hash FROM "+ResearchGatePlanR967.CP4_BINDING+" b JOIN "+FixedResearchWindowR932.PAIR+" p ON p.window_id=b.window_id LEFT JOIN "+CandidateExecutionAuditR932.TABLE+" e ON e.candidate_key=? AND e.target_issue=p.target_issue LEFT JOIN "+ResearchCaptureLedgerR932.TABLE+" o ON o.candidate_key=? AND o.target_issue=p.target_issue WHERE b.lifetime_id=? AND b.stage<=? AND p.target_issue<=? ORDER BY p.target_issue",new String[]{key,key,lifetime,String.valueOf(stage),endpoint})){
                while(c.moveToNext()){ResearchWorkBudgetR927.checkpoint();
                    String issue=c.getString(0);int y=c.getInt(1);double prob=c.getDouble(2);
                    if(!issue.equals(expected)||!plan.calibrationHash.equals(c.getString(3))||(y!=0&&y!=1)||!Double.isFinite(prob)||prob<0||prob>1||c.isNull(5)||!V51ChecksumEngine.sha256(c.getString(5)).equals(c.getString(6))||!TargetIssueResolver.exact(c.getString(7),issue).valid||c.getInt(8)!=1200||c.getInt(9)!=1||c.getInt(10)!=1||!c.getString(4).equals(c.getString(11)))throw new IllegalStateException("CP4_CAPTURE_INTEGRITY");
                    JSONObject execution=new JSONObject(c.getString(5));
                    ResearchGatePlanR967.FrozenIdentityCp4 actual=new ResearchGatePlanR967.FrozenIdentityCp4(key,identity.getString("structure"),identity.getString("bundle"),identity.getString("selector"),identity.getString("calibration"),identity.getString("family"),execution);
                    if(!actual.hash.equals(identityHash)||!c.getString(4).equals(execution.getString("prediction_hash")))throw new IllegalStateException("CP4_MODEL_IDENTITY_DRIFT");
                    if(key.startsWith("algorithm:"+FrozenCandidateAdapterR958.PREFIX)
                            &&!identity.getString("exact_scope_policy_hash").equals(execution.optString("exact_scope_policy_hash","")))
                        throw new IllegalStateException("F05_FORMAL_EXACT_SCOPE_MISMATCH");
                    hits.add(y);probs.add(prob);last=issue;expected=ResearchEvaluationRunR967.nextEvaluation(key,issue);proof.append('|').append(issue).append('|').append(c.getString(6)).append('|').append(y).append('|').append(Double.toHexString(prob));
                }
            }
            int n=hits.size();if((!openPrefix&&n!=planned)||!last.equals(endpoint))throw new IllegalStateException("CP4_LIFETIME_DENOMINATOR");
            if(openPrefix){
                int minimum=0;for(int i=1;i<stage;i++)minimum+=plan.policy.planned(i);
                if(n<minimum+1||n>minimum+plan.policy.planned(stage))throw new IllegalStateException("CP4_RENEWAL_PREFIX_RANGE");
            }
            JSONObject metrics=metricsCp4(lifetime,identityHash,key,endpoint,stage,plan.policy.hash,hypothesis.alpha,hits,probs,minWindowRate,V51ChecksumEngine.sha256(proof.toString()));
            metrics.put("independent_windows",completedWindows);metrics.put("renewal",renewal);
            JSONObject matrixProofs=new JSONObject();StringBuilder matrixEvidence=new StringBuilder(metrics.getString("evidence_hash")).append('|').append(prepared.proofHash);
            for(Map.Entry<String,com.ethk3.novaonepro.r952.MatrixPolicyViewsCP12.View> entry:prepared.views.entrySet()){matrixProofs.put(entry.getKey(),entry.getValue().viewHash);matrixEvidence.append('|').append(entry.getKey()).append(':').append(entry.getValue().viewHash);}
            metrics.put("matrix_policy_hash",com.ethk3.novaonepro.r952.MatrixProjectionCP12.POLICY_VIEWS_HASH);metrics.put("matrix_view_hashes",matrixProofs);metrics.put("matrix_publication_proof",prepared.proofHash);
            for(int width:new int[]{20,50,100,300})if(n>=width){com.ethk3.novaonepro.r952.MatrixPolicyViewsCP12.View view=prepared.views.get("CandidateWindowMatrix/"+width);double rate=view.scalar("hitRate");if(Math.abs(rate-metrics.getDouble("rolling_"+width+"_rate"))>1e-12||view.scalar("coverage")!=1d||prepared.views.get("CandidateErrorMatrix/"+width).scalar("missingCapture")!=0d)throw new IllegalStateException("CP12_MATRIX_FORMAL_DERIVATION_DRIFT/"+width);metrics.put("rolling_"+width+"_rate",rate);metrics.put("rolling_"+width+"_brier",prepared.views.get("ConfidenceMatrix/"+width).scalar("brier"));metrics.put("rolling_"+width+"_ece",prepared.views.get("ConfidenceMatrix/"+width).scalar("ece"));metrics.put("rolling_"+width+"_residual",prepared.views.get("ResidualMatrix/"+width).scalar("mean"));}
            metrics.put("evidence_hash",V51ChecksumEngine.sha256(matrixEvidence.toString()));
            if(n<360){return finishCertificatePrepareCP12(db,prepared,sourceFence,"AUDITED_NO_CERTIFICATE","CP4_MINIMUM_360_NOT_REACHED",n,metrics.toString());}
            List<CertificateBundleServiceR929.GateObservation> external=CertificateBundleServiceR929.cp4ExternalGates(db,lifetime,metrics.getString("evidence_hash"),endpoint);
            external.add(new CertificateBundleServiceR929.GateObservation("F04_NUMERIC_SCOPE",1,1,true,CalibrationRuntimeIdentityF04.REVISION,identity.getString("code"),"IMMUTABLE_CP4_IDENTITY_NUMERIC_SCOPE"));
            if(key.startsWith("algorithm:"+FrozenCandidateAdapterR958.PREFIX)){
                String scope=CertifiedPredictionR974.exactScopePolicyHash(key.substring("algorithm:".length()),identity.getString("structure"),identity.getString("state"),identity.getString("selector"));
                if(!scope.equals(identity.getString("exact_scope_policy_hash")))throw new IllegalStateException("F05_EXACT_SCOPE_PUBLICATION_IDENTITY");
                external.add(CertifiedPredictionR974.prepareExactCalibrationScopeGateF05(db,key.substring("algorithm:".length()),identity.getString("structure"),identity.getString("bundle"),identity.getString("selector"),identity.getString("state"),identity.getString("calibration")));
                external.add(new CertificateBundleServiceR929.GateObservation("EXACT_SELECTION_SCOPE",1,1,true,
                        "F05_EXACT_MODEL_SELECTION_POLICY",scope,"PREREGISTERED_FULL1200_ARGMAX_NOT_BLEND_WEIGHT"));
            }
            CertificateBundleServiceR929.Evaluation evaluation=CertificateBundleServiceR929.evaluateCp4(metrics,external);
            boolean all=evaluation.weightCap>0;for(CertificateBundleServiceR929.GateObservation gate:evaluation.gates)all&=gate.pass;
            if(!all){
                
                return finishCertificatePrepareCP12(db,prepared,sourceFence,"WAIT_EXTERNAL_EVIDENCE",evaluation.reason,n,metrics.toString());
            }
            
            if(hypothesis.state!=MultipleComparisonRegistryR930.State.ACCEPTED&&!(hypothesis.state==MultipleComparisonRegistryR930.State.OBSERVING&&metrics.getDouble("anytime_p")<=hypothesis.alpha))throw new IllegalStateException("CP4_STATISTICAL_ACCEPTANCE_REQUIRED");
            String first=registration.windowStartIssue,structure=identity.getString("structure"),evidence=metrics.getString("evidence_hash");
            long finished=0;try(Cursor finish=db.rawQuery("SELECT MAX(s.created_at) FROM "+ResearchGatePlanR967.CP4_BINDING+" b JOIN "+FixedWindowStatisticsR932.TABLE+" s ON s.window_id=b.window_id WHERE b.lifetime_id=? AND b.stage<=?",new String[]{lifetime,String.valueOf(stage)})){if(finish.moveToFirst())finished=finish.getLong(0);}
            if(renewal){try(Cursor settledAt=db.rawQuery("SELECT MAX(p.settled_at) FROM "+ResearchGatePlanR967.CP4_BINDING+" b JOIN "+FixedResearchWindowR932.PAIR+" p ON p.window_id=b.window_id WHERE b.lifetime_id=? AND b.stage<=? AND p.target_issue<=?",new String[]{lifetime,String.valueOf(stage),endpoint})){if(settledAt.moveToFirst())finished=Math.max(finished,settledAt.getLong(0));}}
            long issued=System.currentTimeMillis();if(finished<=registered||issued<=finished){return finishCertificatePrepareCP12(db,prepared,sourceFence,"WAIT_EXTERNAL_EVIDENCE","CP4_PUBLICATION_CHRONOLOGY",n,metrics.toString());}
            ExperimentProtocolR929.TrialIdentity trialIdentity=new ExperimentProtocolR929.TrialIdentity(V51Repository.GAME_CODE,identity.getString("schema"),V51ChecksumEngine.sha256("CP4_FUTURE_ISSUES|"+first+'|'+endpoint),identity.getString("code"),registration.hypothesisId,0L,identity.getString("family"),structure,ExperimentProtocolR929.Stage.EVALUATE,V51ChecksumEngine.sha256(window+"|CP4_PREFIX_"+n),Collections.<ExperimentProtocolR929.ArtifactDependency>emptyList());
            ExperimentProtocolR929.TrialRecord trial=new ExperimentProtocolR929.TrialRecord(trialIdentity,ExperimentProtocolR929.TrialStatus.COMPLETED,registered,registered,finished,evidence,"",false,"");
            String from=TargetIssueResolver.next(endpoint).targetIssue,until=from;for(int i=1;i<120;i++)until=TargetIssueResolver.next(until).targetIssue;
            CertificateBundleServiceR929.Certificate cert=new CertificateBundleServiceR929.Certificate(candidateKey(key,identity.getString("family"),structure),evaluation.level,CertificateBundleServiceR929.CertificateState.ACTIVE,
                identity.getString("bundle"),identity.getString("schema"),identity.getString("atom"),identity.getString("state"),plan.policy.hash,identity.getString("calibration"),DomainRouterR929.installedSchemaHash(),trial.recordHash,identity.getString("selector"),identity.getString("code"),from,until,issued,evaluation.weightCap,evaluation.gates,"CP4_LIFETIME="+lifetime+"|FUTURE_N="+n+"|NO_REAL_AUTHORITY");
            PreparedCertificateCP12 result=finishCertificatePrepareCP12(db,prepared,sourceFence,"PUBLISHED",evaluation.reason,n,metrics.toString());return new PreparedCertificateCP12(result,trial,cert);
        }catch(JSONException e){throw new IllegalStateException("CP4_PUBLICATION_JSON",e);}
    }
    static final class PreparedMatrixPublicationCP12 {
        final String window,lifetime,endpoint,identityHash,resultHash,windowState,structure,reason,proofHash;final int stage;
        final Map<String,com.ethk3.novaonepro.r952.MatrixPolicyViewsCP12.View> views;
        PreparedMatrixPublicationCP12(String window,String lifetime,String endpoint,String identity,String result,String state,String structure,int stage,String reason,Map<String,com.ethk3.novaonepro.r952.MatrixPolicyViewsCP12.View> views){this.window=window;this.lifetime=lifetime;this.endpoint=endpoint;identityHash=identity;resultHash=result;windowState=state;this.structure=structure;this.stage=stage;this.reason=reason;this.views=views;proofHash=V51ChecksumEngine.sha256("CP12_MATRIX_PUBLICATION|"+window+'|'+lifetime+'|'+stage+'|'+endpoint+'|'+identity+'|'+result+'|'+state+'|'+structure+'|'+reason);}
    }
    static PreparedMatrixPublicationCP12 prepareMatrixPublicationCP12(SQLiteDatabase db){
        if(db.inTransaction())throw new IllegalStateException("CP12_MATRIX_PUBLICATION_PREPARE_IN_TX");String window,lifetime,endpoint,identity,result,state,structure;int stage;
        try(Cursor c=db.rawQuery("SELECT b.window_id,b.lifetime_id,b.stage,b.last_issue,l.identity_hash,w.state,w.result_hash,w.structural_hash FROM "+ResearchGatePlanR967.CP4_BINDING+" b JOIN "+ResearchGatePlanR967.CP4_LIFETIME+" l ON l.lifetime_id=b.lifetime_id JOIN "+FixedResearchWindowR932.WINDOW+" w ON w.window_id=b.window_id LEFT JOIN "+TABLE+" p ON p.window_id=b.window_id WHERE "+runnableCp4()+" ORDER BY b.last_issue,b.window_id LIMIT 1",new String[]{String.valueOf(System.currentTimeMillis()-60000L)})){if(!c.moveToFirst())return null;window=c.getString(0);lifetime=c.getString(1);stage=c.getInt(2);endpoint=c.getString(3);identity=c.getString(4);state=c.getString(5);result=c.getString(6);structure=c.getString(7);}
        if("OPEN".equals(state)){try(Cursor c=db.rawQuery("SELECT MAX(x.target_issue) FROM "+ResearchGatePlanR967.CP4_BINDING+" b JOIN "+FixedResearchWindowR932.PAIR+" x ON x.window_id=b.window_id WHERE b.lifetime_id=? AND b.stage<=? AND x.candidate_hit IN(0,1) AND x.champion_hit IN(0,1)",new String[]{lifetime,String.valueOf(stage)})){if(!c.moveToFirst()||c.isNull(0))return new PreparedMatrixPublicationCP12(window,lifetime,endpoint,identity,result,state,structure,stage,"WAIT_MATRIX_FORMAL_ENDPOINT",Collections.<String,com.ethk3.novaonepro.r952.MatrixPolicyViewsCP12.View>emptyMap());endpoint=c.getString(0);}}
        try{return new PreparedMatrixPublicationCP12(window,lifetime,endpoint,identity,result,state,structure,stage,"",com.ethk3.novaonepro.r952.MatrixPolicyViewsCP12.readAtCP12(db,structure,endpoint));}
        catch(IllegalStateException unavailable){String reason=unavailable.getMessage();if(reason==null||!(reason.startsWith("WAIT_MATRIX_CHECKPOINT")||reason.startsWith("MATRIX_METRIC_UNAVAILABLE")))throw unavailable;return new PreparedMatrixPublicationCP12(window,lifetime,endpoint,identity,result,state,structure,stage,reason,Collections.<String,com.ethk3.novaonepro.r952.MatrixPolicyViewsCP12.View>emptyMap());}
    }
    private static void publicationCp4(SQLiteDatabase db,String window,String state,String reason,int n,String metrics){
        db.execSQL("UPDATE "+TABLE+" SET state=?,reason=?,verified_n=?,proofs_hash=?,updated_at=? WHERE window_id=?",new Object[]{state,reason+(metrics.isEmpty()?"":"|METRICS="+metrics),n,metrics.isEmpty()?"":V51ChecksumEngine.sha256(metrics),System.currentTimeMillis(),window});
    }
    private static JSONObject metricsCp4(String life,String identity,String key,String endpoint,int stage,String policy,double alpha,List<Integer> ys,List<Double> ps,double minWindowRate,String evidence)throws JSONException{
        int n=ys.size(),hits=0,run=0,maxRun=0;double brier=0,baseline=0,ece=0,wealth=0,peak=0,dd=0,maxLogE=0,logE=0;double[] binsP=new double[10],binsY=new double[10],q={.31,.32,.34,.36,.40,.45,.50,.60,.70,.80,.90},log=new double[11];
        for(int i=0;i<n;i++){int y=ys.get(i);double p=ps.get(i);hits+=y;run=y==0?run+1:0;maxRun=Math.max(maxRun,run);brier+=(p-y)*(p-y);baseline+=(.3-y)*(.3-y);int bin=Math.min(9,(int)(p*10));binsP[bin]+=p;binsY[bin]+=y;wealth+=y-.3;peak=Math.max(peak,wealth);dd=Math.max(dd,peak-wealth);double max=Double.NEGATIVE_INFINITY;for(int j=0;j<q.length;j++){log[j]+=y==1?Math.log(q[j]/.3):Math.log((1-q[j])/.7);max=Math.max(max,log[j]);}double sum=0;for(double v:log)sum+=Math.exp(v-max);logE=max+Math.log(sum)-Math.log(11d);maxLogE=Math.max(maxLogE,logE);}
        for(int i=0;i<10;i++)ece+=Math.abs(binsP[i]-binsY[i]);double rate=hits/(double)n,z=1.959963984540054,z2=z*z,lower=(rate+z2/(2*n)-z*Math.sqrt(rate*(1-rate)/n+z2/(4d*n*n)))/(1+z2/n);
        JSONObject m=new JSONObject();m.put("revision","CP4_CERTIFICATE_METRICS_V1");m.put("lifetime_id",life);m.put("identity_hash",identity);m.put("candidate_key",key);m.put("endpoint",endpoint);m.put("stage",stage);m.put("policy_hash",policy);m.put("evidence_hash",evidence);m.put("future_n",n);m.put("paired_n",n);m.put("independent_windows",stage);m.put("hit_rate",rate);m.put("wilson_lower",lower);m.put("coverage",1d);m.put("brier",brier/n);m.put("baseline_brier",baseline/n);m.put("ece",ece/n);m.put("max_loss_streak",maxRun);m.put("residual_drawdown",dd);m.put("min_fixed_window_rate",minWindowRate);m.put("log_e",logE);m.put("max_log_e",maxLogE);m.put("alpha",alpha);m.put("anytime_p",Math.exp(-maxLogE));m.put("integrity",true);
        for(int w:new int[]{20,50,100,300}){if(n<w){m.put("rolling_"+w+"_rate",JSONObject.NULL);continue;}int h=0;for(int i=n-w;i<n;i++)h+=ys.get(i);m.put("rolling_"+w+"_rate",h/(double)w);}return m;
    }


    private static String runnableCp4(){
        String original="(w.state<>'OPEN' AND (p.window_id IS NULL OR p.state='AUDITING' OR (p.state='WAIT_EXTERNAL_EVIDENCE' AND p.updated_at<?)))";
        String accepted="EXISTS(SELECT 1 FROM "+ResearchGatePlanR967.CP4_LIFETIME+" life JOIN "+Batch34SchemaR930.TABLE_HYPOTHESIS+" h ON h.hypothesis_id=life.hypothesis_id WHERE life.lifetime_id=b.lifetime_id AND h.state='ACCEPTED')";
        String settled="(SELECT COUNT(*) FROM "+ResearchGatePlanR967.CP4_BINDING+" prefix JOIN "+FixedResearchWindowR932.PAIR+" x ON x.window_id=prefix.window_id WHERE prefix.lifetime_id=b.lifetime_id AND prefix.stage<=b.stage AND x.candidate_hit IN (0,1) AND x.champion_hit IN (0,1))";
        String published="COALESCE((SELECT MAX(z.verified_n) FROM "+ResearchGatePlanR967.CP4_BINDING+" prior JOIN "+TABLE+" z ON z.window_id=prior.window_id WHERE prior.lifetime_id=b.lifetime_id),0)";
        return "("+original+" OR (w.state IN ('OPEN','PASS') AND "+accepted+" AND "+settled+">="+published+"+120))";
    }


public static String reportSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store) {
        com.ethk3.novaonepro.ReportTextCP12 out=new com.ethk3.novaonepro.ReportTextCP12(report, "\n固定窗口统一证书发布\n");
        try(Cursor exists=report.select("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{TABLE})) {if(!exists.moveToFirst())return out.append("尚未运行窗口发布审计。\n").toString();}
        try(Cursor c=report.select("SELECT candidate_key,state,reason,verified_n,max_cost_ns FROM "+TABLE+" ORDER BY updated_at DESC LIMIT 8",null)){
            while(c.moveToNext())out.append(c.getString(0)).append("：").append(c.getString(1)).append("；已核对 ").append(c.getInt(3)).append(" 期；最大重放 ").append(c.getLong(4)/1000000d).append("ms；").append(c.getString(2)).append('\n');
        }
        return out.toString();
    }
}
