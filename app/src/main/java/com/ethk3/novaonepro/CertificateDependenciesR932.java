package com.ethk3.novaonepro;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import org.json.JSONObject;
import org.json.JSONException;

/** Exact installed dependency closure; no certificate creation or separate authorization owner. */
public final class CertificateDependenciesR932 {
    private CertificateDependenciesR932(){}
    public static String component(String type) {
        String hash=ModelBundleRuntimeR932.current().artifactHashes.get(type);
        return hash==null?V51ChecksumEngine.sha256("R932_INSTALLED_SEED|"+type+'|'+ModelBundleRuntimeR932.current().hash+'|'+SourceManifestR815.SOURCE_TREE_SHA256):hash;
    }
    public static String modelHash(){return V51ChecksumEngine.sha256(component("NUMBER")+'|'+component("GATE")+'|'+component("RANKER"));}
    public static String selectionHash(){String hash=ModelBundleRuntimeR932.current().selectionHash;return hash.length()==64?hash:SelectionEvidenceContractR929.installedPolicy().hash;}
    public static String check(DbStore store,String atom,String model,String policy,String calibration,
            String trial,String structural,String family,long issuedAt) {
        if(ResearchGatePlanR967.installed().isCp3)return checkCp4(store,atom,model,policy,calibration,trial,structural,family,issuedAt);
        if(!AtomCaptureR932.graphHash(store).equals(atom))return "ATOM_GRAPH_OR_QUARANTINE_CHANGED";
        if(!modelHash().equals(model)||!component("POLICY").equals(policy)||!component("CALIBRATION").equals(calibration))return "MODEL_POLICY_CALIBRATION_CHANGED";
        try(Cursor c=store.readable().rawQuery("SELECT structural_hash,family_id,status,registered_at,finished_at FROM "+ResearchPlatformStoreR929.TABLE_TRIAL+" WHERE record_hash=? LIMIT 2",new String[]{trial})){
            if(!c.moveToFirst())return "FROZEN_TRIAL_RECORD_MISSING";
            if(!structural.equals(c.getString(0))||!family.equals(c.getString(1))||!"COMPLETED".equals(c.getString(2))||c.getLong(3)>=issuedAt||c.getLong(4)>=issuedAt)return "TRIAL_IDENTITY_OR_CHRONOLOGY_CHANGED";
            if(c.moveToNext())return "TRIAL_RECORD_AMBIGUOUS";
        }
        return "";
    }

    static String markerCp4(java.util.List<CertificateBundleServiceR929.GateObservation> gates,String id){
        String out="";for(CertificateBundleServiceR929.GateObservation g:gates)if(id.equals(g.gateId)){
            if(!out.isEmpty()||!g.pass||g.observed!=1d||g.required!=1d||!("CP4_POLICY".equals(id)?"CP4_CERTIFICATE_METRICS_V1":"CP4_IDENTITY".equals(id)?"CP4_FROZEN_IDENTITY":"CP4_FROZEN_LIFETIME").equals(g.source))throw new IllegalStateException("CP4_CERTIFICATE_MARKER");out=g.sourceHash;
        }if(!out.matches("[0-9a-f]{64}"))throw new IllegalStateException("CP4_CERTIFICATE_MARKER_MISSING:"+id);return out;
    }
    static String validateCp4Publication(SQLiteDatabase db,CertificateBundleServiceR929.Certificate cert){
        try{
            String life=markerCp4(cert.gates,"CP4_LIFETIME"),identity=markerCp4(cert.gates,"CP4_IDENTITY"),policy=markerCp4(cert.gates,"CP4_POLICY");
            if(!numericScopeMarkerF04(cert.gates).equals(cert.codeHash))return "F04_CERTIFICATE_NUMERIC_SCOPE_DRIFT";
            if(hasFeedbackRejectionCP12(db,life))return "CP12_FEEDBACK_INTEGRITY";
            if(!policy.equals(cert.policyHash)||!policy.equals(ResearchGatePlanR967.installed().hash))return "CP4_POLICY_DRIFT";
            try(Cursor c=db.rawQuery("SELECT identity_json,identity_hash,policy_hash,registration_json,candidate_key FROM "+ResearchGatePlanR967.CP4_LIFETIME+" WHERE lifetime_id=?",new String[]{life})){
                if(!c.moveToFirst()||!identity.equals(c.getString(1))||!policy.equals(c.getString(2)))return "CP4_LIFETIME_IDENTITY_MISSING";
                JSONObject j=new JSONObject(c.getString(0));
                if(!CalibrationRuntimeIdentityF04.REVISION.equals(j.optString("semantic_revision",""))
                        ||!CalibrationRuntimeIdentityF04.numericHash().equals(j.getString("code"))
                        ||!j.getString("build_source_hash").matches("[0-9a-f]{64}")
                        ||!j.getString("atom_admission_hash").matches("[0-9a-f]{64}"))return "F04_LEGACY_SCOPE_PROOF_REQUIRED";
                String prefix=cert.candidate.consumer==CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM?"algorithm:":cert.candidate.consumer==CertificateBundleServiceR929.Consumer.GENESIS_ASSET?"genesis:":"formula:";
                if(!(prefix+cert.candidate.candidateId).equals(c.getString(4))||!cert.candidate.structuralHash.equals(j.getString("structure"))||!cert.modelHash.equals(j.getString("state"))||!cert.calibrationHash.equals(j.getString("calibration"))||!cert.bundleHash.equals(j.getString("bundle"))||!cert.schemaHash.equals(j.getString("schema"))||!cert.codeHash.equals(j.getString("code"))||!cert.atomGraphHash.equals(j.getString("atom"))||!cert.selectionHash.equals(j.getString("selector")))return "CP4_CERTIFICATE_FROZEN_IDENTITY_DRIFT";
                MultipleComparisonRegistryR930.Registration r=ResearchGatePlanR967.registrationCp4(c.getString(3));MultipleComparisonRegistryR930.Snapshot h=MultipleComparisonRegistryR930.readCp3WithinTransaction(db,r.hypothesisId);
                if(h==null||h.state!=MultipleComparisonRegistryR930.State.ACCEPTED||!h.windowId.equals(life))return "CP4_STATISTICAL_ACCEPTANCE_MISSING";
            }
            try(Cursor t=db.rawQuery("SELECT structural_hash,family_id,status,registered_at,finished_at FROM "+ResearchPlatformStoreR929.TABLE_TRIAL+" WHERE record_hash=?",new String[]{cert.trialRegistryHash})){
                if(!t.moveToFirst()||!cert.candidate.structuralHash.equals(t.getString(0))||!cert.candidate.familyId.equals(t.getString(1))||!"COMPLETED".equals(t.getString(2))||t.getLong(3)>=cert.issuedAtMs||t.getLong(4)>=cert.issuedAtMs)return "CP4_TRIAL_IDENTITY_OR_CHRONOLOGY";
                if(t.moveToNext())return "CP4_TRIAL_AMBIGUOUS";
            }
            if(!com.ethk3.novaonepro.r952.ProspectiveDiversityCP12.fenceGatesCP12(db,cert.gates))return "CP12_CERTIFICATE_DIVERSITY_SOURCE_CHANGED";
            return com.ethk3.novaonepro.r952.AtomAdmissionCP12.currentBindingsReasonCP12(db,cert.gates,cert.validFromIssue,cert.candidate.structuralHash,cert.modelHash);
        }catch(JSONException e){return "CP4_FROZEN_IDENTITY_JSON";}
    }
    static String checkCp4(DbStore store,String atom,String model,String policy,String calibration,String trial,String structural,String family,long issuedAt){
        if(!ResearchGatePlanR967.installed().hash.equals(policy))return "CP4_LEGACY_CERTIFICATE_READ_ONLY";
        String certificate="",candidate="",consumer="";
        try(Cursor c=store.readable().rawQuery("SELECT certificate_hash,candidate_id,consumer FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" WHERE trial_hash=? AND structural_hash=? AND policy_hash=? AND model_hash=? AND calibration_hash=? AND issued_at=? AND contract_revision='CP4_REQUIRED_GATES_V1'",new String[]{trial,structural,policy,model,calibration,String.valueOf(issuedAt)})){
            if(!c.moveToFirst())return "CP4_CERTIFICATE_IDENTITY_MISSING";certificate=c.getString(0);candidate=c.getString(1);consumer=c.getString(2);if(c.moveToNext())return "CP4_CERTIFICATE_AMBIGUOUS";
        }
        if(!CertifiedPredictionR974.certificateContract(store.readable(),certificate))return "CP4_FROZEN_GATES_INVALID";
        java.util.ArrayList<CertificateBundleServiceR929.GateObservation> gates=new java.util.ArrayList<CertificateBundleServiceR929.GateObservation>();
        try(Cursor c=store.readable().rawQuery("SELECT gate_id,observed,required,pass,source,source_hash,detail FROM "+ResearchPlatformStoreR929.TABLE_CERT_GATE+" WHERE certificate_hash=?",new String[]{certificate})){
            while(c.moveToNext())gates.add(new CertificateBundleServiceR929.GateObservation(c.getString(0),c.getDouble(1),c.getDouble(2),c.getInt(3)==1,c.getString(4),c.getString(5),c.getString(6)));
        }
        String life=markerCp4(gates,"CP4_LIFETIME"),identity=markerCp4(gates,"CP4_IDENTITY");if(!policy.equals(markerCp4(gates,"CP4_POLICY")))return "CP4_POLICY_GATE_DRIFT";
        numericScopeMarkerF04(gates);
        if(hasFeedbackRejectionCP12(store.readable(),life))return "CP12_FEEDBACK_INTEGRITY";
        String target=ModelBundleRuntimeR932.currentTarget();
        String atomAdmissionReason=com.ethk3.novaonepro.r952.AtomAdmissionCP12.currentBindingsReasonCP12(store.readable(),gates,target,structural,model);if(!atomAdmissionReason.isEmpty())return atomAdmissionReason;
        try(Cursor c=store.readable().rawQuery("SELECT identity_json,identity_hash,policy_hash,registration_json FROM "+ResearchGatePlanR967.CP4_LIFETIME+" WHERE lifetime_id=?",new String[]{life})){
            if(!c.moveToFirst()||!identity.equals(c.getString(1))||!policy.equals(c.getString(2)))return "CP4_LIFETIME_DRIFT";
            try{
                JSONObject j=new JSONObject(c.getString(0));
                if(!model.equals(j.getString("state"))||!structural.equals(j.getString("structure"))||!calibration.equals(j.getString("calibration"))||!atom.equals(j.getString("atom")))return "CP4_DEPENDENCY_DRIFT";
                if(!CalibrationRuntimeIdentityF04.REVISION.equals(j.optString("semantic_revision","")))return "F04_LEGACY_SCOPE_PROOF_REQUIRED";
                String candidateKey="INVENTED_ALGORITHM".equals(consumer)?"algorithm:"+candidate:"GENESIS_ASSET".equals(consumer)?"genesis:"+candidate:"formula:"+candidate;
                if(!candidateKey.equals(j.getString("candidate"))
                        ||!CalibrationRuntimeIdentityF04.numericHash().equals(j.getString("code"))
                        ||!ModelBundleRuntimeR932.current().hash.equals(j.getString("bundle"))
                        ||!CalibrationRuntimeIdentityF04.executionScope(candidateKey,structural,j.getString("bundle")).equals(atom))return "F04_EFFECTIVE_SCOPE_MISMATCH";
                if(!j.getString("build_source_hash").matches("[0-9a-f]{64}")
                        ||!j.getString("atom_admission_hash").matches("[0-9a-f]{64}"))return "F04_BUILD_PROVENANCE_CONFLICT";
                MultipleComparisonRegistryR930.Registration r=ResearchGatePlanR967.registrationCp4(c.getString(3));MultipleComparisonRegistryR930.Snapshot h=MultipleComparisonRegistryR930.readCp3WithinTransaction(store.readable(),r.hypothesisId);if(h==null||h.state!=MultipleComparisonRegistryR930.State.ACCEPTED||!life.equals(h.windowId))return "CP4_STATISTICAL_RECEIPT_INVALID";
                // Exact REAL currently supports the frozen adapter. Other namespaces remain research-only.
                if(!"INVENTED_ALGORITHM".equals(consumer)||!candidate.startsWith(FrozenCandidateAdapterR958.PREFIX))return "CP4_EXACT_EXECUTABLE_ADAPTER_REQUIRED";
                FrozenCandidateAdapterR958.Frozen f=FrozenCandidateAdapterR958.load(store.readable(),candidate);
                if(f==null||!f.structure.equals(structural)||!f.modelHash.equals(model)||!V51ChecksumEngine.sha256(f.ast.canonical).equals(j.getString("program")))return "CP4_CURRENT_FROZEN_MODEL_DRIFT";
                if(!ResearchEvaluationRunR967.consumerCompatible(store.readable(),candidate,target))return "CP4_CURRENT_DOMAIN_OR_RUN";
            }catch(JSONException e){return "CP4_IDENTITY_JSON";}
        }
        try(Cursor t=store.readable().rawQuery("SELECT structural_hash,family_id,status,registered_at,finished_at FROM "+ResearchPlatformStoreR929.TABLE_TRIAL+" WHERE record_hash=?",new String[]{trial})){
            if(!t.moveToFirst()||!structural.equals(t.getString(0))||!family.equals(t.getString(1))||!"COMPLETED".equals(t.getString(2))||t.getLong(3)>=issuedAt||t.getLong(4)>=issuedAt)return "CP4_TRIAL_IDENTITY_OR_CHRONOLOGY";
            if(t.moveToNext())return "CP4_TRIAL_AMBIGUOUS";
        }
        return CertificateBundleServiceR929.cp4CurrentDomainReason(store,life,target);
    }

    static String numericScopeMarkerF04(java.util.List<CertificateBundleServiceR929.GateObservation> gates){
        String hash="";int n=0;
        for(CertificateBundleServiceR929.GateObservation g:gates)if("F04_NUMERIC_SCOPE".equals(g.gateId)){
            n++;if(!g.pass||g.observed!=1d||g.required!=1d||!CalibrationRuntimeIdentityF04.REVISION.equals(g.source))
                throw new IllegalStateException("F04_NUMERIC_SCOPE_GATE_INVALID");
            hash=g.sourceHash;
        }
        if(n!=1||!CalibrationRuntimeIdentityF04.numericHash().equals(hash))
            throw new IllegalStateException("F04_LEGACY_SCOPE_PROOF_REQUIRED");
        return hash;
    }

    static boolean hasFeedbackRejectionCP12(SQLiteDatabase db,String lifetime){
        try(Cursor c=db.rawQuery("SELECT 1 FROM research_feedback_rejection_cp12 WHERE lifetime_id=? LIMIT 1",new String[]{lifetime})){return c.moveToFirst();}
    }
}
