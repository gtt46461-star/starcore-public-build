package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;
import org.json.JSONObject;

/** Exact frozen-model selection, not a research-owned BET writer.
 * Existing certificate owner verifies independent quality. DecisionGateEngine alone authorizes;
 * canonical DbStore transaction rechecks certificate, model, deadline and target before insertion.
 * No coverage quota, global open-trade count, or unrelated production entropy cuts are used here.
 */
public final class CertifiedPredictionR974 {
    public static final String LANE="CERTIFIED_EXACT_K3_R974";
    private CertifiedPredictionR974(){}
    public static final class Proof {
        public final String candidate,structure,certificate,evidence,modelHash,base,target,bundle,history,recipe,actionHash,hash;
        public final long capturedAt,deadlineAt,deadlineMono;
        public final String calibrationHash,selectionHash,scopePolicyHash;
        public final double selectedHitP;
        private Proof(UnifiedResearchChoiceR932.Evidence e,FrozenCandidateAdapterR958.Frozen f,V51Models.PredictionContext c,
                String recipe,V51Models.PositionAction action,AnalysisSingleFlightR36.Request request,ExactCalibration calibration){
            candidate=e.id;structure=f.structure;certificate=e.certificate;evidence=e.evidence;modelHash=f.modelHash;
            base=c.baseIssue;target=c.targetIssue;bundle=c.artifactBundleHash;history=c.historyHash;
            this.recipe=recipe;actionHash=action.checksum;capturedAt=ModelBundleRuntimeR932.capturedAt();
            deadlineAt=request.deadlineAt;deadlineMono=request.deadlineMonoAt;
            calibrationHash=calibration.hash;selectionHash=calibration.selector;scopePolicyHash=calibration.scope;selectedHitP=calibration.probability;
            hash=digest();
        }
        private Proof(UnifiedResearchChoiceR932.Evidence e,com.ethk3.novaonepro.strategyv2.MainlineAdapterSU1.Prepared prepared,V51Models.PredictionContext c,AnalysisSingleFlightR36.Request request){
            com.ethk3.novaonepro.strategyv2.CertificateSU1.Authority a=prepared.authority;
            candidate=e.id;structure=a.program;certificate=a.certificate;evidence=a.evidence;modelHash=a.artifact;
            base=c.baseIssue;target=c.targetIssue;bundle=c.artifactBundleHash;history=c.historyHash;
            recipe=prepared.recipe;actionHash=prepared.rankOne.checksum;capturedAt=ModelBundleRuntimeR932.capturedAt();deadlineAt=request.deadlineAt;deadlineMono=request.deadlineMonoAt;
            calibrationHash=a.calibration;selectionHash=a.selector;scopePolicyHash=exactScopePolicyHash(candidate,structure,modelHash,selectionHash);selectedHitP=prepared.selectedHitP;hash=digest();
        }
        private Proof(UnifiedResearchChoiceR932.Evidence e,com.ethk3.novaonepro.strategyv2.MainlineAdapterSU2.Prepared prepared,V51Models.PredictionContext c,AnalysisSingleFlightR36.Request request){
            com.ethk3.novaonepro.strategyv2.CertificateSU2.Authority a=prepared.authority;
            candidate=e.id;structure=a.program;certificate=a.certificate;evidence=e.evidence;modelHash=a.program;
            base=c.baseIssue;target=c.targetIssue;bundle=c.artifactBundleHash;history=c.historyHash;
            recipe=prepared.recipe;actionHash=prepared.rankOne.checksum;capturedAt=ModelBundleRuntimeR932.capturedAt();deadlineAt=request.deadlineAt;deadlineMono=request.deadlineMonoAt;
            calibrationHash=V51ChecksumEngine.sha256("SU2_PLATT|"+Double.toHexString(a.a)+"|"+Double.toHexString(a.b));selectionHash=com.ethk3.novaonepro.strategyv2.MainlineAdapterSU2.CONTRACT;
            scopePolicyHash=exactScopePolicyHash(candidate,structure,modelHash,selectionHash);selectedHitP=prepared.selectedHitP;hash=digest();
        }
        private String digest(){return V51ChecksumEngine.sha256(LANE+'|'+candidate+'|'+structure+'|'+certificate+'|'+evidence+'|'+modelHash+'|'+base+'|'+target+'|'+bundle+'|'+history+'|'+V51ChecksumEngine.sha256(recipe)+'|'+actionHash+'|'+capturedAt+'|'+deadlineAt+'|'+deadlineMono+'|'+calibrationHash+'|'+selectionHash+'|'+scopePolicyHash+'|'+Double.toHexString(selectedHitP));}
        public boolean expired(){return deadlineAt<=0||deadlineMono<=0||System.currentTimeMillis()>=deadlineAt||TimeSourceR811.monoNowMs()>=deadlineMono;}
        boolean matches(V51Models.SelectedActionEnvelope selected){
            if(selected==null||selected.action==null||selected.context==null||!hash.equals(digest()))return false;
            V51Models.PredictionContext c=selected.context;
            return TargetIssueResolver.exact(base,target).valid&&base.equals(c.baseIssue)&&base.equals(c.historyMaxIssue)
                &&target.equals(c.targetIssue)&&bundle.equals(c.artifactBundleHash)&&history.equals(c.historyHash)
                &&V51BuildManifest.COHORT_ID.equals(c.cohortId)&&selected.action.rank==1
                &&DecisionGateEngine.b12K3(selected.action.n1,selected.action.n2,selected.action.n3)
                &&selected.action.position>=1&&selected.action.position<=10&&actionHash.equals(selected.action.checksum)
                && validSelectedProbability(selected.selectedHitP)
                && Double.compare(selectedHitP,selected.selectedHitP)==0
                && selectionHash.equals(selected.selectionPolicyHash)
                && scopePolicyHash.equals(exactScopePolicyHash(candidate,structure,modelHash,selectionHash))
                && calibrationHash.matches("[0-9a-f]{64}")&&capturedAt>0&&!expired();
        }
    }
    /** Candidate-only calibration is valid here because this lane consumes the exact immutable
     * candidate rank-one action at weight 1.0; it must not calibrate a mixed/overlaid action. */
    static boolean validSelectedProbability(double p){return Double.isFinite(p)&&p>0.0d&&p<1.0d;}
    private static final class ExactCalibration {
        final String hash,selector,scope;final double probability;
        ExactCalibration(String hash,String selector,String scope,double p){this.hash=hash;this.selector=selector;this.scope=scope;probability=p;}
    }
    private static ExactCalibration readExactCalibration(SQLiteDatabase db,String cert,String candidate,
            String structure,String target,double rawScore,long captureAt){
        // Never call captureOrPredict: analysis must not create or fit calibration observations.
        String hash,selector,bundle,modelHash;
        try(Cursor c=db.rawQuery("SELECT calibration_hash,selection_hash,bundle_hash,model_hash FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+
                " WHERE certificate_hash=? AND candidate_id=? AND structural_hash=? AND state='ACTIVE'",
                new String[]{cert,candidate,structure})){
            if(!c.moveToFirst())throw new IllegalStateException("R974_EXACT_CAL_CERTIFICATE_MISSING");
            hash=c.getString(0);selector=c.getString(1);bundle=c.getString(2);modelHash=c.getString(3);
        }
        try(Cursor c=db.rawQuery("SELECT study_hash,candidate_key,validation_until,model_json,model_hash,frozen_at FROM "+
                CandidateCalibrationR932.MODEL+" WHERE model_hash=? AND length(CAST(model_json AS BLOB))<=65536",new String[]{hash})){
            if(!c.moveToFirst())throw new IllegalStateException("R974_EXACT_CAL_MODEL_MISSING");
            String study=c.getString(0),key=c.getString(1),until=c.getString(2),json=c.getString(3);
            if(!("algorithm:"+candidate).equals(key)||!hash.equals(c.getString(4))
                    ||!hash.equals(V51ChecksumEngine.sha256(json))||!TargetIssueResolver.isValid(until)
                    ||TargetIssueResolver.compare(until,target)>=0||c.getLong(5)<=0||c.getLong(5)>=captureAt)
                throw new IllegalStateException("R974_EXACT_CAL_IDENTITY_OR_PIT");
            CandidateCalibrationR932.Qualification quality=CandidateCalibrationR932.qualificationForStudyCP12(db,study);
            if(!quality.qualified()||!until.equals(quality.validationUntil))
                throw new IllegalStateException("R974_EXACT_CAL_QUALITY:"+quality.reason);
            String scope=exactScopePolicyHash(candidate,structure,modelHash,selector);
            requireExactCalibrationScopeGate(db,cert,candidate,structure,bundle,selector,modelHash,hash,scope,study);
            final double p;
            try{p=CandidateCalibrationR932.predict(new JSONObject(json),rawScore);}
            catch(org.json.JSONException invalid){throw new IllegalStateException("R974_EXACT_CAL_JSON",invalid);}
            if(!validSelectedProbability(p)||c.moveToNext())throw new IllegalStateException("R974_EXACT_CAL_P_OR_CARDINALITY");
            return new ExactCalibration(hash,selector,scope,p);
        }
    }
    /** Before choice freeze, reject blend-only/missing CAL scope and keep scanning the same ordered pool.
     * Score zero is solely a finite-parameter sanity probe; no resulting probability is used as a decision. */
    static String certificatePreflightReasonF05(DbStore store,String certificate,String candidate,String structure,String target){
        try{
            if(!certificateContract(store.readable(),certificate))return "EXACT_CERTIFICATE_SCOPE_OR_GATES_REQUIRED";
            readExactCalibration(store.readable(),certificate,candidate,structure,target,0.0d,ModelBundleRuntimeR932.capturedAt());
            return "";
        }catch(ResearchWorkBudgetR927.Yield yielded){throw yielded;}
        catch(RuntimeException failure){return "EXACT_CALIBRATION_PREFLIGHT:"+failure.getClass().getSimpleName()+":"+String.valueOf(failure.getMessage());}
    }
    public static final String EXACT_SCOPE_POLICY_F05=CalibrationRuntimeIdentityF04.EXACT_SCOPE_POLICY_F04;
    static String exactScopePolicyHash(String candidate,String structure,String model,String selector){
        if(candidate!=null&&(candidate.startsWith(com.ethk3.novaonepro.strategyv2.CertificateSU1.PREFIX)||candidate.startsWith(com.ethk3.novaonepro.strategyv2.CertificateSU2.PREFIX)))return com.ethk3.novaonepro.strategyv2.LegacyAuthorityProjectionR29.exactScope(candidate,structure,model,selector);
        return CalibrationRuntimeIdentityF04.exactScope(candidate,structure,model,selector);
    }
    private static String requireExactCalibrationObservations(SQLiteDatabase db,String study,String candidate,
            String structure,String bundle,String selector,String scope,long capturedAt){
        int n=0;StringBuilder digest=new StringBuilder("F05_CAL_OBSERVATIONS_241|").append(study);
        try(Cursor c=db.rawQuery("SELECT o.candidate_key,o.structure_hash,o.bundle_hash,o.selection_hash,o.target_issue,o.prediction_hash,o.raw_score,o.position,o.n1,o.n2,o.n3,o.frozen_at,o.hit,e.payload,e.proof_hash,p.record_hash,p.score,p.position,p.n1,p.n2,p.n3 FROM "+CandidateCalibrationR932.OBS+
                " o LEFT JOIN "+CandidateExecutionAuditR932.TABLE+" e ON e.candidate_key=o.candidate_key AND e.target_issue=o.target_issue LEFT JOIN "+FrozenCandidateAdapterR958.PREDRAW+
                " p ON p.candidate_id=? AND p.target_issue=o.target_issue WHERE o.study_hash=? ORDER BY o.target_issue",new String[]{candidate,study})){
            while(c.moveToNext()){
                if(!("algorithm:"+candidate).equals(c.getString(0))||!structure.equals(c.getString(1))
                        ||!bundle.equals(c.getString(2))||!selector.equals(c.getString(3))
                        ||c.getLong(11)<=0||c.getLong(11)>=capturedAt||(c.getInt(12)!=0&&c.getInt(12)!=1)
                        ||c.isNull(13)||!V51ChecksumEngine.sha256(c.getString(13)).equals(c.getString(14))
                        ||!c.getString(5).equals(c.getString(15))||Double.compare(c.getDouble(6),c.getDouble(16))!=0
                        ||c.getInt(7)!=c.getInt(17)||c.getInt(8)!=c.getInt(18)||c.getInt(9)!=c.getInt(19)||c.getInt(10)!=c.getInt(20))
                    throw new IllegalStateException("F05_CAL_OBSERVATION_SELECTOR_IDENTITY");
                try{
                    JSONObject proof=new JSONObject(c.getString(13));
                    int ordinal=K3ActionSpaceEngine.globalOrdinal(c.getInt(7),c.getInt(8),c.getInt(9),c.getInt(10));
                    if(!scope.equals(proof.optString("exact_scope_policy_hash",""))
                            ||proof.getInt("action_count")!=1200||proof.getInt("selected_ordinal")!=ordinal
                            ||!modelScopeCompatible(proof,candidate,structure,bundle,selector))
                        throw new IllegalStateException("F05_CAL_EXACT_SCOPE_REQUIRED");
                }catch(org.json.JSONException invalid){throw new IllegalStateException("F05_CAL_EXACT_SCOPE_JSON",invalid);}
                digest.append('|').append(c.getString(4)).append('|').append(c.getString(5)).append('|').append(c.getString(14)).append('|').append(c.getInt(12));
                n++;
            }
        }
        if(n!=CandidateCalibrationR932.OBSERVATIONS)throw new IllegalStateException("F05_CAL_EXACT_SCOPE_DENOMINATOR:"+n);
        return V51ChecksumEngine.sha256(digest.toString());
    }
    private static String calibrationScopeBinding(String candidate,String structure,String bundle,String selector,String model,String cal,String scope,String study){
        return "F05_CAL_SCOPE_BINDING|"+candidate+'|'+structure+'|'+bundle+'|'+selector+'|'+model+'|'+cal+'|'+scope+'|'+study;
    }
    /** Heavy 241-row validation is only called during off-writer certificate preparation. */
    static CertificateBundleServiceR929.GateObservation prepareExactCalibrationScopeGateF05(SQLiteDatabase db,
            String candidate,String structure,String bundle,String selector,String model,String cal){
        if(db.inTransaction())throw new IllegalStateException("F05_CAL_SCOPE_PREPARE_REQUIRES_NO_WRITER");
        String scope=exactScopePolicyHash(candidate,structure,model,selector),study;
        try(Cursor c=db.rawQuery("SELECT study_hash,candidate_key,model_json,validation_until FROM "+CandidateCalibrationR932.MODEL+" WHERE model_hash=?",new String[]{cal})){
            if(!c.moveToFirst()||!("algorithm:"+candidate).equals(c.getString(1))
                    ||!cal.equals(V51ChecksumEngine.sha256(c.getString(2))))throw new IllegalStateException("F05_CAL_SCOPE_MODEL_IDENTITY");
            study=c.getString(0);
            if(!CandidateCalibrationR932.qualificationForStudyCP12(db,study).qualified())throw new IllegalStateException("F05_CAL_SCOPE_QUALITY");
        }
        String observationHash=requireExactCalibrationObservations(db,study,candidate,structure,bundle,selector,scope,System.currentTimeMillis());
        String binding=calibrationScopeBinding(candidate,structure,bundle,selector,model,cal,scope,study);
        return new CertificateBundleServiceR929.GateObservation("EXACT_CALIBRATION_SCOPE",1,1,true,
                "F05_CAL_OBSERVATION_SCOPE_241",V51ChecksumEngine.sha256(binding+'|'+observationHash),binding+"|OBS_HASH="+observationHash);
    }
    private static void requireExactCalibrationScopeGate(SQLiteDatabase db,String certificate,String candidate,
            String structure,String bundle,String selector,String model,String cal,String scope,String study){
        String binding=calibrationScopeBinding(candidate,structure,bundle,selector,model,cal,scope,study);
        try(Cursor c=db.rawQuery("SELECT observed,required,pass,source,source_hash,detail FROM "+ResearchPlatformStoreR929.TABLE_CERT_GATE+
                " WHERE certificate_hash=? AND gate_id='EXACT_CALIBRATION_SCOPE'",new String[]{certificate})){
            if(!c.moveToFirst()||c.getDouble(0)!=1||c.getDouble(1)!=1||c.getInt(2)!=1
                    ||!"F05_CAL_OBSERVATION_SCOPE_241".equals(c.getString(3)))throw new IllegalStateException("F05_CAL_SCOPE_GATE_REQUIRED");
            String detail=c.getString(5),prefix=binding+"|OBS_HASH=";
            if(detail==null||!detail.startsWith(prefix)||!detail.substring(prefix.length()).matches("[0-9a-f]{64}")
                    ||!V51ChecksumEngine.sha256(binding+'|'+detail.substring(prefix.length())).equals(c.getString(4)))
                throw new IllegalStateException("F05_CAL_SCOPE_BINDING_CHANGED");
        }
    }
    private static boolean modelScopeCompatible(JSONObject execution,String candidate,String structure,String bundle,String selector)throws org.json.JSONException{
        return ("algorithm:"+candidate).equals(execution.getString("candidate_key"))
                &&structure.equals(execution.getString("structural_hash"))&&bundle.equals(execution.getString("model_bundle_hash"))
                &&exactScopePolicyHash(candidate,structure,execution.getString("state_hash"),selector).equals(execution.getString("exact_scope_policy_hash"));
    }
    private static boolean exactCertificateScope(SQLiteDatabase db,String certificate,List<CertificateBundleServiceR929.GateObservation> gates){
        try(Cursor c=db.rawQuery("SELECT candidate_id,structural_hash,model_hash,selection_hash FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" WHERE certificate_hash=?",new String[]{certificate})){
            if(!c.moveToFirst())return false;
            String scope=exactScopePolicyHash(c.getString(0),c.getString(1),c.getString(2),c.getString(3));
            String lifetime=CertificateDependenciesR932.markerCp4(gates,"CP4_LIFETIME");boolean admitted=false,calibrationAdmitted=false;
            for(CertificateBundleServiceR929.GateObservation gate:gates)if("EXACT_SELECTION_SCOPE".equals(gate.gateId))
                admitted=gate.pass&&gate.observed==1d&&gate.required==1d&&"F05_EXACT_MODEL_SELECTION_POLICY".equals(gate.source)&&scope.equals(gate.sourceHash);
            for(CertificateBundleServiceR929.GateObservation gate:gates)if("EXACT_CALIBRATION_SCOPE".equals(gate.gateId))
                calibrationAdmitted=gate.pass&&gate.observed==1d&&gate.required==1d&&"F05_CAL_OBSERVATION_SCOPE_241".equals(gate.source);
            if(!admitted||!calibrationAdmitted)return false;
            try(Cursor life=db.rawQuery("SELECT identity_json FROM "+ResearchGatePlanR967.CP4_LIFETIME+" WHERE lifetime_id=?",new String[]{lifetime})){
                return life.moveToFirst()&&scope.equals(new JSONObject(life.getString(0)).optString("exact_scope_policy_hash",""));
            }
        }catch(RuntimeException unavailable){return false;}
        catch(org.json.JSONException invalid){return false;}
    }
    /** Before freezing choice, skip unsupported or broken exact models and try the next
     * quality-ranked candidate. This consumes only pre-draw input, never a draw label.
     * Prediction is repeated after selection and still bound to the request deadline.
     */
    static String executableReason(DbStore store,String id,String structure,String target){
        if(!id.startsWith(FrozenCandidateAdapterR958.PREFIX))return "NOT_EXACT_FROZEN_MODEL";
        try{
            FrozenCandidateAdapterR958.Frozen f=FrozenCandidateAdapterR958.load(store.readable(),id);
            if(f==null||!f.structure.equals(structure))return "FROZEN_IDENTITY_MISMATCH";
            if(!ResearchEvaluationRunR967.consumerCompatible(store.readable(),id,target))return "OUTSIDE_EVALUATED_DOMAIN_OR_RUN";
            com.ethk3.novaonepro.r952.PITSnapshotContractR952 pit=com.ethk3.novaonepro.r952.PITSnapshotContractR952.capture(store,target,f.ast.formatVersion==3?5000:f.ast.lookback);
            if(!TargetIssueResolver.exact(pit.lastIssue,target).valid)return "CURRENT_BASE_NOT_AVAILABLE";
            if(f.ast.formatVersion==3){
                try(com.ethk3.novaonepro.r952.FrozenGraphContextCP4 ctx=FrozenCandidateAdapterR958.prepareContextCP8(store.readable(),f,pit,System.currentTimeMillis())){
                    FrozenCandidateAdapterR958.score(f.ast,f.model,pit,ctx,false);
                }
            }else FrozenCandidateAdapterR958.score(f.ast,f.model,pit.history,false);
            return "";
        }catch(ResearchWorkBudgetR927.Yield yielded){throw yielded;}
        catch(RuntimeException failure){return "EXACT_EXECUTION:"+failure.getClass().getSimpleName()+":"+String.valueOf(failure.getMessage());}
    }
    public static final class Prepared {
        public final V51Models.PositionAction[] actions;public final V51Models.PositionAction rankOne;
        public final UnifiedResearchChoiceR932.Evidence evidence;public final Proof proof;
        private Prepared(FrozenConsumerRecipeR932.Mixed m,UnifiedResearchChoiceR932.Evidence e,Proof p){actions=m.actions;rankOne=m.rankOne;evidence=e;proof=p;}
        private Prepared(com.ethk3.novaonepro.strategyv2.MainlineAdapterSU1.Prepared m,UnifiedResearchChoiceR932.Evidence e,Proof p){actions=m.actions;rankOne=m.rankOne;evidence=e;proof=p;}
        private Prepared(com.ethk3.novaonepro.strategyv2.MainlineAdapterSU2.Prepared m,UnifiedResearchChoiceR932.Evidence e,Proof p){actions=m.actions;rankOne=m.rankOne;evidence=e;proof=p;}
    }
    /** Called after the one-consumer selection is frozen. No fallback certificate is fabricated. */
    public static Prepared prepare(DbStore store,V51Models.PredictionContext context,V51Models.PositionAction[] actions,AnalysisSingleFlightR36.Request request){
        if(store==null||context==null||request==null||!request.valid()||!context.targetIssue.equals(request.targetIssue)
                ||request.deadlineAt<=System.currentTimeMillis()||request.deadlineMonoAt<=TimeSourceR811.monoNowMs())return null;
        UnifiedResearchChoiceR932.Evidence e=UnifiedResearchChoiceR932.selected(store,context.targetIssue,CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM);
        if(e!=null&&e.id.startsWith(com.ethk3.novaonepro.strategyv2.CertificateSU1.PREFIX)){
            com.ethk3.novaonepro.strategyv2.CertificateSU1.Authority a=com.ethk3.novaonepro.strategyv2.MainlineAdapterSU1.authority(store,e.id,e.structure,context.targetIssue,e.certificate);
            if(a==null)throw new IllegalStateException("SU1_CURRENT_CERTIFICATE_REQUIRED");
            com.ethk3.novaonepro.strategyv2.MainlineAdapterSU1.Prepared m=com.ethk3.novaonepro.strategyv2.MainlineAdapterSU1.prepare(store,a,context,actions);
            Proof p=new Proof(e,m,context,request);if(p.expired())throw new IllegalStateException("SU1_EXACT_DEADLINE");return new Prepared(m,e,p);
        }
        if(e!=null&&e.id.startsWith(com.ethk3.novaonepro.strategyv2.CertificateSU2.PREFIX)){
            // R13-P30: ABSTAIN, a missing frozen table or a changed certificate means no action this period
            com.ethk3.novaonepro.strategyv2.MainlineAdapterSU2.Prepared m=com.ethk3.novaonepro.strategyv2.MainlineAdapterSU2.prepare(store,context,actions);
            if(m==null||!m.authority.certificate.equals(e.certificate)||!m.authority.candidate().equals(e.id))return null;
            Proof p=new Proof(e,m,context,request);if(p.expired())throw new IllegalStateException("SU2_EXACT_DEADLINE");return new Prepared(m,e,p);
        }
        if(e==null||!e.id.startsWith(FrozenCandidateAdapterR958.PREFIX))return null;
        try{
            if(!certificateContract(store.readable(),e.certificate))throw new IllegalStateException("R974_CERTIFICATE_GATES_INVALID");
            FrozenCandidateAdapterR958.Frozen f=FrozenCandidateAdapterR958.load(store.readable(),e.id);
            if(f==null||!f.structure.equals(e.structure)||!ResearchEvaluationRunR967.consumerCompatible(store.readable(),f.id,context.targetIssue))throw new IllegalStateException("R974_FROZEN_MODEL_INCOMPATIBLE");
            FrozenConsumerRecipeR932.Recipe exact=FrozenCandidateAdapterR958.recipeLaneCP12(store,f,e,1.0,context.baseIssue,context.targetIssue,LANE);
            FrozenConsumerRecipeR932.Mixed mixed=rank(exact,actions);
            ExactCalibration calibration=readExactCalibration(store.readable(),e.certificate,e.id,e.structure,
                    context.targetIssue,mixed.rankOne.rawActionP,ModelBundleRuntimeR932.capturedAt());
            Proof proof=new Proof(e,f,context,exact.encoded,mixed.rankOne,request,calibration);
            if(proof.expired())throw new IllegalStateException("R974_EXACT_PREDICTION_EXPIRED");
            return new Prepared(mixed,e,proof);
        }catch(ResearchWorkBudgetR927.Yield yielded){throw yielded;}
        catch(Exception failure){
            // An applicable certificate which fails exact execution must not silently fall back
            // to a different rank with the same quality evidence.
            throw new IllegalStateException("R974_CERTIFIED_PREDICTION_REJECTED:"+failure.getClass().getSimpleName()+":"+failure.getMessage(),failure);
        }
    }
    static FrozenConsumerRecipeR932.Mixed rank(FrozenConsumerRecipeR932.Recipe recipe,V51Models.PositionAction[] actions){
        if(!LANE.equals(recipe.lane)||recipe.weight!=1.0||!recipe.id.startsWith(FrozenCandidateAdapterR958.PREFIX))throw new IllegalArgumentException("R974_EXACT_RECIPE");
        com.ethk3.novaonepro.r952.CandidateAstR952 ast=com.ethk3.novaonepro.r952.CandidateAstR952.parse(recipe.spec);
        com.ethk3.novaonepro.r952.CandidateCompilerR952.Model model=com.ethk3.novaonepro.r952.CandidateCompilerR952.Model.parse(recipe.state);
        String structure=ResearchEvaluationRunR967.versioned(recipe.id)?ResearchEvaluationRunR967.evaluationHash(ast,model,recipe.evaluationRule,recipe.evaluationEpoch):FrozenCandidateAdapterR958.structure(ast,model);
        String prefix=ResearchEvaluationRunR967.versioned(recipe.id)?ResearchEvaluationRunR967.frozenPrefix(recipe.id):FrozenCandidateAdapterR958.PREFIX;
        if(!structure.equals(recipe.structure)||!recipe.id.equals(prefix+structure.substring(0,24)))throw new IllegalStateException("R974_EXACT_MODEL_IDENTITY");
        final double[] scores=FrozenCandidateAdapterR958.scoreRecipeCP8(recipe,ast,model).scores;
        K3ActionSpaceEngine.validateOrThrow(actions);Integer[] order=new Integer[1200];
        for(int i=0;i<1200;i++)order[i]=i;
        Arrays.sort(order,(a,b)->{int x=K3ActionSpaceEngine.globalOrdinal(actions[a].position,actions[a].n1,actions[a].n2,actions[a].n3),y=K3ActionSpaceEngine.globalOrdinal(actions[b].position,actions[b].n1,actions[b].n2,actions[b].n3);int d=Double.compare(scores[y],scores[x]);return d!=0?d:Integer.compare(x,y);});
        V51Models.PositionAction[] out=new V51Models.PositionAction[1200];String recipeHash=V51ChecksumEngine.sha256(recipe.encoded);
        for(int r=0;r<1200;r++){
            int i=order[r];V51Models.PositionAction a=actions[i];int ordinal=K3ActionSpaceEngine.globalOrdinal(a.position,a.n1,a.n2,a.n3);
            int next=r<1199?K3ActionSpaceEngine.globalOrdinal(actions[order[r+1]].position,actions[order[r+1]].n1,actions[order[r+1]].n2,actions[order[r+1]].n3):ordinal;
            double margin=r<1199?Math.max(0,scores[ordinal]-scores[next]):0;
            out[i]=new V51Models.PositionAction(a.position,a.n1,a.n2,a.n3,a.n4,scores[ordinal],scores[ordinal],margin,a.thirdMargin,a.entropy,a.stability,a.metaFeatures,a.fingerprint,r+1,V51ChecksumEngine.sha256(LANE+'|'+a.checksum+'|'+recipeHash+'|'+ordinal+'|'+r+'|'+scores[ordinal]));
        }
        K3ActionSpaceEngine.validateOrThrow(out);return new FrozenConsumerRecipeR932.Mixed(out,out[order[0]]);
    }
    /** Reconstruct and hash-check every frozen gate; a loose all_gates_pass flag is insufficient. */
    static boolean certificateContract(SQLiteDatabase db,String certificate){
        ArrayList<CertificateBundleServiceR929.GateObservation> gates=new ArrayList<CertificateBundleServiceR929.GateObservation>();
        try(Cursor c=db.rawQuery("SELECT gate_id,observed,required,pass,source,source_hash,detail,gate_hash FROM "+ResearchPlatformStoreR929.TABLE_CERT_GATE+" WHERE certificate_hash=? ORDER BY gate_id",new String[]{certificate})){
            while(c.moveToNext()){
                CertificateBundleServiceR929.GateObservation g=new CertificateBundleServiceR929.GateObservation(c.getString(0),c.getDouble(1),c.getDouble(2),c.getInt(3)==1,c.getString(4),c.getString(5),c.getString(6));
                if(!g.gateHash.equals(c.getString(7)))return false;gates.add(g);
            }
        }return CertificateBundleServiceR929.validatedInfluenceCap(gates)>0&&exactCertificateScope(db,certificate,gates);
    }
    static V51CandidateRealGateR915.Evaluation evaluate(DbStore store,V51Models.SelectedActionEnvelope selected,Proof proof){
        String error="";
        if(!V51CandidateRealGateR915.releaseBoundary())error="RELEASE_BOUNDARY";
        else if(proof==null||!proof.matches(selected))error="PIT_IDENTITY_OR_DEADLINE";
        else if(store!=null){
            if(!BuildIdentity.isBetCompatible(store.getState(BuildIdentity.STATE_STATUS,"")))error="BUILD_IDENTITY";
            else if(store.hasDrawIssue(proof.target))error="POST_DRAW";
            else if(store.getTradeByTargetIssue(proof.target)!=null)error="DUPLICATE";
            else error=verifyCertificate(store,proof);
        }
        return new V51CandidateRealGateR915.Evaluation(error.isEmpty(),error.isEmpty()?"ELIGIBLE_CERTIFIED_EXACT_K3":error,
                proof==null?"NO_QUALIFIED_PROOF":LANE+"|candidate="+proof.candidate+"|certificate="+proof.certificate+"|proof="+proof.hash+"|frequencyCap=NONE");
    }
    static String verifyCertificate(DbStore store,Proof proof){
        try{
            if(proof.candidate.startsWith(com.ethk3.novaonepro.strategyv2.CertificateSU2.PREFIX)){
                com.ethk3.novaonepro.strategyv2.CertificateSU2.Authority a=com.ethk3.novaonepro.strategyv2.CertificateSU2.read(store.readable(),proof.candidate.substring(com.ethk3.novaonepro.strategyv2.CertificateSU2.PREFIX.length()),System.currentTimeMillis());
                if(a==null||!a.certificate.equals(proof.certificate)||!a.program.equals(proof.structure))return "SU2_CERTIFICATE_CHANGED";
                try(Cursor choice=store.readable().rawQuery("SELECT bundle_hash,consumer,candidate_id,certificate_hash FROM "+UnifiedResearchChoiceR932.TABLE+" WHERE target_issue=?",new String[]{proof.target})){
                    if(!choice.moveToFirst()||!proof.bundle.equals(choice.getString(0))||!"INVENTED_ALGORITHM".equals(choice.getString(1))||!proof.candidate.equals(choice.getString(2))||!proof.certificate.equals(choice.getString(3)))return "SU2_FROZEN_SELECTION_CHANGED";
                }return "";
            }
            if(proof.candidate.startsWith(com.ethk3.novaonepro.strategyv2.CertificateSU1.PREFIX)){
                com.ethk3.novaonepro.strategyv2.CertificateSU1.Authority authority=com.ethk3.novaonepro.strategyv2.MainlineAdapterSU1.authority(store,proof.candidate,proof.structure,proof.target,proof.certificate);
                if(authority==null||!authority.artifact.equals(proof.modelHash)||!authority.evidence.equals(proof.evidence)||!com.ethk3.novaonepro.strategyv2.MainlineAdapterSU1.verifyFrozen(store.readable(),proof,null))return "SU1_CERTIFICATE_OR_FROZEN_ACTION_CHANGED";
                try(Cursor choice=store.readable().rawQuery("SELECT bundle_hash,consumer,candidate_id,certificate_hash FROM "+UnifiedResearchChoiceR932.TABLE+" WHERE target_issue=?",new String[]{proof.target})){
                    if(!choice.moveToFirst()||!proof.bundle.equals(choice.getString(0))||!"INVENTED_ALGORITHM".equals(choice.getString(1))||!proof.candidate.equals(choice.getString(2))||!proof.certificate.equals(choice.getString(3)))return "SU1_FROZEN_SELECTION_CHANGED";
                }return "";
            }
            ResearchPlatformStoreR929.Authorization a=ResearchPlatformStoreR929.authorizeCurrent(store,CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM,proof.candidate,proof.structure,proof.target,.05);
            if(!a.authorized||!a.certificateHash.equals(proof.certificate)||!certificateContract(store.readable(),proof.certificate))return "CERTIFICATE_INVALID_OR_CHANGED";
            FrozenCandidateAdapterR958.Frozen f=FrozenCandidateAdapterR958.load(store.readable(),proof.candidate);
            if(f==null||!f.structure.equals(proof.structure)||!f.modelHash.equals(proof.modelHash)||!ResearchEvaluationRunR967.consumerCompatible(store.readable(),f.id,proof.target))return "MODEL_INVALID_OR_CHANGED";
            try(Cursor c=store.readable().rawQuery("SELECT bundle_hash,consumer,candidate_id,certificate_hash FROM "+UnifiedResearchChoiceR932.TABLE+" WHERE target_issue=?",new String[]{proof.target})){
                if(!c.moveToFirst()||!proof.bundle.equals(c.getString(0))||!"INVENTED_ALGORITHM".equals(c.getString(1))||!proof.candidate.equals(c.getString(2))||!proof.certificate.equals(c.getString(3)))return "FROZEN_SELECTION_CHANGED";
            }
            return "";
        }catch(RuntimeException ex){return "CERTIFICATE_UNAVAILABLE:"+ex.getClass().getSimpleName();}
    }
    static void beforeInsert(DbStore store,SQLiteDatabase db,V51CompiledActionContract compiled){
        Proof proof=compiled.certifiedPrediction;if(proof==null)return;
        if(!db.inTransaction()||!proof.matches(compiled.selected))throw new IllegalStateException("R974_EXACT_COMMIT_PIT_OR_DEADLINE");
        String error=verifyCertificate(store,proof);if(!error.isEmpty())throw new IllegalStateException("R974_EXACT_COMMIT_"+error);
        if(proof.candidate.startsWith(com.ethk3.novaonepro.strategyv2.CertificateSU1.PREFIX)){
            if(!com.ethk3.novaonepro.strategyv2.MainlineAdapterSU1.verifyFrozen(db,proof,compiled.selected.action))throw new IllegalStateException("SU1_EXACT_COMMIT_ACTION_CHANGED");return;
        }
        if(proof.candidate.startsWith(com.ethk3.novaonepro.strategyv2.CertificateSU2.PREFIX)){
            if(!com.ethk3.novaonepro.strategyv2.LegacyAuthorityProjectionR29.verifySu2(db,proof,compiled.selected.action))throw new IllegalStateException("SU2_EXACT_COMMIT_ACTION_OR_AUTHORITY_CHANGED");return;
        }
        ExactCalibration current=readExactCalibration(db,proof.certificate,proof.candidate,proof.structure,
                proof.target,compiled.selected.action.rawActionP,proof.capturedAt);
        if(!proof.calibrationHash.equals(current.hash)||!proof.selectionHash.equals(current.selector)
                ||Double.compare(proof.selectedHitP,current.probability)!=0)
            throw new IllegalStateException("R974_EXACT_COMMIT_CALIBRATION_CHANGED");
    }
}
