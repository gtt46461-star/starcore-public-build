package com.ethk3.novaonepro;

import android.database.Cursor;
import com.ethk3.novaonepro.strategyv2.*;
import java.nio.file.*;
import java.util.*;
import java.sql.*;
import java.lang.reflect.*;

/** Actual consumer -> immutable R974 proof -> original DecisionGate -> original
 * canonical terminal writer. All inputs and persisted rows belong to a disposable
 * synthetic HOST database, never natural quality, Android or an external trade. */
public final class TypedMainlineR31Host {
    private static int assertions,negativeCases;
    private static void need(boolean value,String name){assertions++;if(!value)throw new AssertionError(name);}
    private static long scalar(TypedFreezeRuntimeR31Fixture f,String sql){try(Cursor c=f.db.rawQuery(sql,null)){need(c.moveToFirst(),"SCALAR_PRESENT");return c.getLong(0);}}
    private static V51Models.PositionAction[] universe(){
        K3ActionSpaceEngine.ActionIdentity[] identities=K3ActionSpaceEngine.enumerateIdentityUniverse();
        V51Models.PositionAction[] out=new V51Models.PositionAction[identities.length];
        for(int i=0;i<out.length;i++){
            K3ActionSpaceEngine.ActionIdentity x=identities[i];int fourth=1;while(fourth==x.n1||fourth==x.n2||fourth==x.n3)fourth++;
            out[i]=new V51Models.PositionAction(x.position,x.n1,x.n2,x.n3,fourth,.30,.30,0,0,1,1,new double[0],x.checksum,i+1,V51ChecksumEngine.sha256("HOST_CONTROL_ACTION|"+x.canonical));
        }
        K3ActionSpaceEngine.validateOrThrow(out);return out;
    }
    private static V51CompiledActionContract compiled(V51Models.PredictionContext context,CertifiedPredictionR974.Prepared prepared,V51Models.PositionAction action){
        V51Models.SelectedActionEnvelope selected=new V51Models.SelectedActionEnvelope(context,action,prepared.proof.selectedHitP,null,null,V51Enums.Lane.CONTROLLED_PROBE,0,0,V51ChecksumEngine.sha256("HOST_SELECTED|"+prepared.proof.hash),prepared.proof.selectionHash);
        V51SelectiveRiskController.Result policy=new V51SelectiveRiskController.Result(V51Enums.Lane.CONTROLLED_PROBE,V51Enums.RejectCode.NONE,null,true,"HOST_REAL_ORIGINAL_CERTIFIED_PATH");
        String hash=V51ChecksumEngine.sha256("HOST_FIXED_COMPILED_BOUNDARY");
        return new V51CompiledActionContract(selected,policy,null,hash,hash,hash,hash,action.checksum,prepared.proof);
    }
    private static void preinsertRefused(TypedFreezeRuntimeR31Fixture f,V51CompiledActionContract compiled,String name){
        try{CertifiedPredictionR974.beforeInsert(f.store,f.db,compiled);throw new AssertionError("CANONICAL_PREINSERT_ACCEPTED:"+name);}
        catch(IllegalStateException expected){need(expected.getMessage()!=null&&(expected.getMessage().contains("R974_")||expected.getMessage().contains("R31_R27_")),"EXACT_GUARD_REFUSAL:"+name+":"+expected.getMessage());}
    }
    /** Savepoints alter only this disposable native fixture. No protected source
     * or certificate is changed by the product under test. */
    private static void invalid(TypedFreezeRuntimeR31Fixture f,V51CompiledActionContract compiled,String name,Runnable mutation){
        f.transaction(()->{
            f.db.execSQL("SAVEPOINT typed_negative");
            try{
                mutation.run();
                need(!V51CandidateRealGateR915.evaluate(f.store,compiled).eligible,"ORIGINAL_CANDIDATE_GATE_REFUSES:"+name);
                need(!CertifiedPredictionR974.verifyCertificate(f.store,compiled.certifiedPrediction).isEmpty(),"CURRENT_CERTIFICATE_REFUSES:"+name);
                if(name.equals("DEFAULT_OFF"))need(new DecisionGateEngine().decideV51B12(f.store,compiled).action!=V51Enums.DecisionAction.BET,"ORIGINAL_UNIQUE_OWNER_DEFAULT_OFF_FALLS_BACK");
                preinsertRefused(f,compiled,name);negativeCases++;
            }finally{f.db.execSQL("ROLLBACK TO typed_negative");f.db.execSQL("RELEASE typed_negative");}
        });
        need(CertifiedPredictionR974.verifyCertificate(f.store,compiled.certifiedPrediction).isEmpty(),"NEGATIVE_ROLLBACK_RESTORES_ACTUAL_PROOF:"+name);
    }
    public static void main(String[] args)throws Exception{
        Class.forName("org.sqlite.JDBC");Path root=Path.of(args[0]);
        Map<String,Object> durableExpected=new LinkedHashMap<>();
        try(TypedFreezeRuntimeR31Fixture f=new TypedFreezeRuntimeR31Fixture(root)){
            need(MainlineFreezeProofR31.read(f.db,f.targetOrdinal,System.currentTimeMillis())==null,"NO_TYPED_PROOF_BEFORE_REAL_FREEZE_OWNER");
            f.certifyAndFreeze();
            MainlineFreezeProofR31.Authority authority=MainlineFreezeProofR31.read(f.db,f.targetOrdinal,System.currentTimeMillis());
            need(authority!=null,"ACTUAL_CERTIFY_AND_FREEZE_PUBLISHED_TYPED_PROOF");
            need(authority.policy.equals(f.policy)&&authority.target.equals(f.target),"ORIGINAL_POLICY_AND_TARGET_IDENTITY");
            need(authority.candidate.startsWith(MainlineFreezeProofR31.PREFIX)&&!authority.candidate.startsWith(CertificateSU2.PREFIX),"R27_HAS_INDEPENDENT_CAPABILITY_NAMESPACE");
            long count=scalar(f,"SELECT count(*) FROM r27_live_frozen"),proofs=scalar(f,"SELECT count(*) FROM r31_mainline_freeze");
            need(proofs==1,"ONLY_THIS_ACTUAL_ISOLATED_OWNER_CALL_HAS_TYPED_PROOF");
            try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.open(null,f.target,System.currentTimeMillis())){
                need(ResearchPlatformStoreR929.schemaReadyReadOnlyR31(f.db),"ACTUAL_PLATFORM_SCHEMA_READY:"+ResearchPlatformStoreR929.schemaReadinessReportR31(f.db).get("reason"));
                if(!UnifiedResearchChoiceR932.freeze(f.store,f.target)){
                    Method prepare=UnifiedResearchChoiceR932.class.getDeclaredMethod("prepareChoiceCP12",DbStore.class,String.class,String.class);prepare.setAccessible(true);
                    try{prepare.invoke(null,f.store,f.target,ModelBundleRuntimeR932.current().hash);}
                    catch(InvocationTargetException failed){throw new IllegalStateException("ACTUAL_UNIFIED_PUBLIC_FREEZE_FAILED_ORIGINAL_PREPARATION_CAUSE",failed.getCause());}
                    throw new AssertionError("ACTUAL_UNIFIED_PUBLIC_FREEZE_FAILED:"+f.store.getState("r932_choice_freeze_status","UNKNOWN"));
                }
                need(true,"ACTUAL_UNIQUE_CONSUMER_SELECTION_COMMITTED");
                UnifiedResearchChoiceR932.Evidence choice=UnifiedResearchChoiceR932.selected(f.store,f.target,CertificateBundleServiceR929.Consumer.INVENTED_ALGORITHM);
                need(choice!=null&&authority.candidate.equals(choice.id)&&authority.certificate.equals(choice.certificate),"ACTUAL_SELECTED_READER_CONSUMES_TYPED_R27");
                need(choice.count("FUTURE_N")==500&&choice.count("ACT_N")==500&&choice.metric("COVERAGE")==1d&&choice.metric("WILSON_LOWER")>0,"REAL_CERTIFICATE_METRIC_DENOMINATORS_REACH_EXISTING_RUNTIME_CONSUMER");
                MainlineBridgeSU2.ProjectionR30 projection=MainlineBridgeSU2.projectionR30(f.db,f.targetOrdinal,System.currentTimeMillis(),authority.candidate);
                need(authority.certificate.equals(projection.certificate)&&authority.choiceId.equals(projection.sources.get("choiceId"))&&Boolean.TRUE.equals(projection.certificateActive)&&Boolean.TRUE.equals(projection.freezeComplete)&&Boolean.TRUE.equals(projection.modelAvailable)&&Boolean.TRUE.equals(projection.selectorConfirmed)&&Boolean.TRUE.equals(projection.actRuleFired),"ACTUAL_REPORT_PROJECTION_CONSUMES_AUTHENTICATED_TYPED_PROOF");
                long now=System.currentTimeMillis();String history=V51ChecksumEngine.sha256("HOST_SYNTHETIC_CAUSAL_HISTORY");
                V51Models.PredictionContext context=new V51Models.PredictionContext(f.base,f.target,f.base,history,V51BuildManifest.COHORT_ID,ModelBundleRuntimeR932.current().hash,now,500);
                AnalysisSingleFlightR36.Request request=new AnalysisSingleFlightR36.Request(f.base,f.target,1,now,f.deadline,"HOST_SYNTHETIC_MECHANICS",V51ChecksumEngine.sha256("HOST_REQUEST|"+f.target),history);
                CertifiedPredictionR974.Prepared prepared=CertifiedPredictionR974.prepare(f.store,context,universe(),request);
                need(prepared!=null&&prepared.proof.candidate.equals(authority.candidate),"ACTUAL_R974_PREPARE_REACHABLE_WITH_ORIGINAL_GATE");
                need(prepared.rankOne.position==authority.position&&prepared.rankOne.n1==authority.n1&&prepared.rankOne.n2==authority.n2&&prepared.rankOne.n3==authority.n3,"EXACT_ORIGINAL_FROZEN_K3_NEVER_RESELECTED");
                need(prepared.proof.selectedHitP==authority.selectedHitP&&prepared.proof.calibrationHash.equals(authority.calibrationHash),"ORIGINAL_RAW_SCORE_R27_CALIBRATION_BOUND");
                try(Cursor calibration=f.db.rawQuery("SELECT a,b FROM r27_calibration WHERE policy_hash=?",new String[]{f.policy})){
                    need(calibration.moveToFirst(),"ACTUAL_FITTED_R27_CALIBRATION_PRESENT");double a=calibration.getDouble(0),b=calibration.getDouble(1);double[] probabilities=authority.probabilities();double raw=probabilities[authority.n1-1]+probabilities[authority.n2-1]+probabilities[authority.n3-1];
                    double actualFormula=1/(1+Math.exp(-Math.max(-30,Math.min(30,a*raw+b))));
                    Constructor<SelectiveCalibrationSU2.Platt> constructor=SelectiveCalibrationSU2.Platt.class.getDeclaredConstructor(double.class,double.class);constructor.setAccessible(true);double foreign=constructor.newInstance(a,b).apply(raw);
                    need(Double.compare(actualFormula,prepared.proof.selectedHitP)==0,"R27_ORIGINAL_RAW_SCORE_NUMERICAL_ABI");need(Math.abs(foreign-actualFormula)>1e-8,"ACTUAL_R27_CALIBRATION_DIFFERS_FROM_SU2_LOGIT_ABI");
                }
                V51CompiledActionContract compiled=compiled(context,prepared,prepared.rankOne);
                need(prepared.proof.matches(compiled.selected),"ORIGINAL_R974_CONTEXT_AND_ACTION_BINDING");
                V51CandidateRealGateR915.Evaluation gate=V51CandidateRealGateR915.evaluate(f.store,compiled);
                need(gate.eligible,"ORIGINAL_CANDIDATE_GATE_ACCEPTS_COMPLETE_TYPED_PROOF:"+gate.code);
                V51Models.AuthorizedDecision decision=new DecisionGateEngine().decideV51B12(f.store,compiled);
                need(decision.action==V51Enums.DecisionAction.BET&&decision.rejectCode==V51Enums.RejectCode.NONE,"ORIGINAL_UNIQUE_DECISION_OWNER_RETURNS_BET");
                need(V51AuthorizationCore.verify(decision,compiled),"ORIGINAL_AUTHORIZATION_SIGNATURE_VERIFIES");
                try(Cursor observed=f.db.rawQuery("SELECT count(*) FROM r30_decision_explain WHERE issue=? AND entry_point='V51_B12' AND original_allow=1 AND explain_allow=1",new String[]{f.target})){need(observed.moveToFirst()&&observed.getLong(0)>0,"ACTUAL_DECISION_OBSERVER_PERSISTS_TYPED_REPORT_SOURCE");}
                f.transaction(()->CertifiedPredictionR974.beforeInsert(f.store,f.db,compiled));
                need(scalar(f,"SELECT count(*) FROM trades")==0,"BEFORE_TERMINAL_PERSIST_NO_TRADE_ROW");

                invalid(f,compiled,"DEFAULT_OFF",()->f.db.execSQL("UPDATE app_state SET state_value='0' WHERE state_key=?",new Object[]{RealAuthoritySU2.USER_SWITCH}));
                invalid(f,compiled,"CERTIFICATE_MISSING",()->f.db.execSQL("DELETE FROM r27_certificate WHERE policy_hash=?",new Object[]{f.policy}));
                invalid(f,compiled,"CERTIFICATE_EXPIRED",()->f.db.execSQL("UPDATE r27_certificate SET expires_at=0 WHERE policy_hash=?",new Object[]{f.policy}));
                invalid(f,compiled,"CERTIFICATE_REVOKED",()->f.db.execSQL("UPDATE r27_certificate SET status='REVOKED' WHERE policy_hash=?",new Object[]{f.policy}));
                invalid(f,compiled,"MODEL_STATE_CHANGED",()->f.db.execSQL("UPDATE r27_live_shadow SET bias=? WHERE policy_hash=?",new Object[]{StoreR27.pack(new double[]{1,0,0,0,0,0,0,0,0,0}),f.policy}));
                invalid(f,compiled,"CALIBRATION_CHANGED",()->f.db.execSQL("UPDATE r27_calibration SET a=a+.01 WHERE policy_hash=?",new Object[]{f.policy}));
                invalid(f,compiled,"SOURCE_REVISION_CHANGED",()->f.db.execSQL("UPDATE r30_live_source_revision SET generation=generation+1 WHERE singleton=1"));
                invalid(f,compiled,"ORPHAN_QUAL_RECEIPT_APPEND_WITH_UNCHANGED_EPROCESS",()->f.db.execSQL("INSERT INTO r29_qual_receipt(experiment_id,target_ordinal,target_issue,act,hit,u,digest,state_after,consumed_at) SELECT experiment_id,?,?,act,hit,u,digest,state_after,consumed_at FROM r29_qual_receipt WHERE experiment_id=? LIMIT 1",new Object[]{f.targetOrdinal-10000,TargetIssueResolver.fromSequenceOrdinalSU1(f.targetOrdinal-10000),f.policy}));
                invalid(f,compiled,"RECEIPT_GUARD_DROP_REINSTALL_SCHEMA_ABA",()->{f.db.execSQL("DROP TRIGGER r31_qual_receipt_no_update");f.db.execSQL(QualificationInboxR29.legacyReceiptGuardDdlR31()[0]);});
                invalid(f,compiled,"TYPED_CHOICE_REVOKED",()->MainlineChoiceStoreR31.revoke(f.db,authority.choiceId,"HOST_TEST_REVOCATION",authority.sourceRevision,System.currentTimeMillis()));
                invalid(f,compiled,"TYPED_PROOF_SCHEMA_MISSING",()->f.db.execSQL("DROP TABLE r31_mainline_freeze"));
                invalid(f,compiled,"TYPED_AUTHORITY_DOMAIN_NOT_READY",()->f.db.execSQL("UPDATE v57_schema_domain_r926 SET state='MIGRATING' WHERE domain=?",new Object[]{MainlineChoiceStoreR31.DOMAIN}));
                invalid(f,compiled,"QUALIFICATION_PROTOCOL_STAMP_STALE",()->f.db.execSQL("UPDATE v57_schema_domain_r926 SET fingerprint='stale' WHERE domain='RESEARCH_R31_QUALIFICATION_RECEIPT'"));
                invalid(f,compiled,"UNIQUE_SELECTION_MISSING",()->f.db.execSQL("DELETE FROM "+UnifiedResearchChoiceR932.TABLE+" WHERE target_issue=?",new Object[]{f.target}));
                invalid(f,compiled,"WRONG_SCOPE_SU2_CERTIFICATE",()->f.db.execSQL("UPDATE r27_certificate SET evidence_digest=? WHERE policy_hash=?",new Object[]{V51ChecksumEngine.sha256("FOREIGN_SU2_CERTIFICATE"),f.policy}));

                V51Models.PositionAction original=prepared.rankOne;int third=original.n1==1&&original.n2==2&&original.n3==3?4:3;
                V51Models.PositionAction changed=new V51Models.PositionAction(original.position,1,2,third,original.n4,original.rawActionP,original.rankScore,original.rankMargin,original.thirdMargin,original.entropy,original.stability,original.metaFeatures,original.fingerprint,1,original.checksum);
                V51CompiledActionContract wrong=compiled(context,prepared,changed);
                f.transaction(()->preinsertRefused(f,wrong,"LEGAL_OTHER_K3_WITH_SAME_CARRIED_CHECKSUM"));
                need(!MainlineAdapterR31.verifyFrozen(f.store,prepared.proof,changed),"CANONICAL_GUARD_READS_ORIGINAL_K3_NOT_ONLY_CALLER_HASH");
                f.transaction(()->{
                    try{f.db.execSQL("UPDATE r31_mainline_freeze SET proof_hash='changed'");throw new AssertionError("MUTABLE_TYPED_PROOF");}
                    catch(android.database.sqlite.SQLiteException expected){need(expected.getMessage().contains("IMMUTABLE"),"PERSISTED_PROOF_IMMUTABLE");}
                });
                need(scalar(f,"SELECT count(*) FROM r27_live_frozen")==count&&scalar(f,"SELECT count(*) FROM r31_mainline_freeze")==proofs,"CONSUMERS_DO_NOT_GENERATE_OR_REPAIR_HISTORICAL_FREEZES");
                V51RuntimeResult result=new V51RuntimeResult(true,context,null,null,null,null,null,prepared.actions,compiled.selected,decision,compiled,"HOST_SYNTHETIC_ENGINEERING_INPUT_NOT_NATURAL_NOT_EXTERNAL_TRANSACTION",1,compiled.checksum);
                f.transaction(()->{
                    f.db.execSQL("SAVEPOINT terminal_revoked");
                    try{
                        f.db.execSQL("UPDATE r27_certificate SET status='REVOKED' WHERE policy_hash=?",new Object[]{f.policy});
                        try{f.store.insertAuthorizedTradeWithinCanonicalR915(f.db,result,System.currentTimeMillis());throw new AssertionError("ACTUAL_TERMINAL_WRITER_ACCEPTED_REVOKED_R27");}
                        catch(IllegalStateException expected){need(expected.getMessage().contains("R974_")||expected.getMessage().contains("R31_R27_"),"ACTUAL_TERMINAL_WRITER_RECHECKS_REVOCATION");}
                        need(scalar(f,"SELECT count(*) FROM trades")==0&&scalar(f,"SELECT count(*) FROM r974_exact_real_receipt")==0&&scalar(f,"SELECT count(*) FROM research_exact_probability_f05")==0,"ACTUAL_REVOKED_TERMINAL_HAS_NO_PARTIAL_PRODUCT");
                    }finally{f.db.execSQL("ROLLBACK TO terminal_revoked");f.db.execSQL("RELEASE terminal_revoked");}
                });
                long commitAt=System.currentTimeMillis();DatabaseCoordinator.TransactionResult<Long> persisted;
                try(DatabaseCoordinator.WriterGuardCP3 writer=DatabaseCoordinator.acquireWriterGuardCP3()){
                    persisted=DatabaseCoordinator.runExistingTransaction(f.db,new DatabaseCoordinator.TransactionContext("B03_PREDICTION",f.target,V51ChecksumEngine.sha256("HOST_ISOLATED_CANONICAL_TERMINAL|"+compiled.checksum)),(db,tx)->f.store.insertAuthorizedTradeWithinCanonicalR915(db,result,commitAt),-1L);
                }
                if(!persisted.success)throw new IllegalStateException("ACTUAL_ORIGINAL_TERMINAL_COMMIT_FAILED:"+persisted.code,persisted.failureCause);
                need(persisted.value!=null&&persisted.value>0,"ACTUAL_ORIGINAL_TERMINAL_TRANSACTION_COMMITTED");
                need(scalar(f,"SELECT count(*) FROM trades")==1&&scalar(f,"SELECT count(*) FROM r974_exact_real_receipt")==1&&scalar(f,"SELECT count(*) FROM research_exact_probability_f05")==1,"ACTUAL_TERMINAL_THREE_PRODUCTS_COMMITTED");
                durableExpected.put("target",f.target);durableExpected.put("position",authority.position);durableExpected.put("n1",authority.n1);durableExpected.put("n2",authority.n2);durableExpected.put("n3",authority.n3);durableExpected.put("candidate",prepared.proof.candidate);durableExpected.put("model",prepared.proof.modelHash);durableExpected.put("certificate",prepared.proof.certificate);durableExpected.put("proof",prepared.proof.hash);durableExpected.put("action",prepared.proof.actionHash);durableExpected.put("authorization",decision.authorizationHash);durableExpected.put("calibration",prepared.proof.calibrationHash);durableExpected.put("selection",prepared.proof.selectionHash);durableExpected.put("scope",prepared.proof.scopePolicyHash);durableExpected.put("probability",prepared.proof.selectedHitP);
            }
        }
        // Fresh native connection after the actual owner and first SQLite pool
        // are closed; the terminal write above was committed, not rolled back.
        try(Connection reopened=DriverManager.getConnection("jdbc:sqlite:"+root.resolve("main.db"));Statement query=reopened.createStatement();ResultSet row=query.executeQuery("SELECT t.target_issue,t.position,t.n1,t.n2,t.n3,t.authorization_id,t.calibrated_p,t.final_p,t.source_type,t.is_realtime_real,e.candidate_id,e.model_hash,e.certificate_hash,e.proof_hash,e.action_hash,e.authorization_hash,p.calibration_hash,p.selection_hash,p.scope_policy_hash,p.selected_hit_p,p.proof_hash,p.action_hash,p.authorization_hash FROM trades t JOIN r974_exact_real_receipt e ON e.trade_id=t._id AND e.target_issue=t.target_issue JOIN research_exact_probability_f05 p ON p.trade_id=t._id AND p.target_issue=t.target_issue")){
            need(row.next(),"FRESH_NATIVE_CONNECTION_READS_DURABLE_THREE_TABLE_CLOSURE");
            need(durableExpected.get("target").equals(row.getString(1))&&((Number)durableExpected.get("position")).intValue()==row.getInt(2)&&((Number)durableExpected.get("n1")).intValue()==row.getInt(3)&&((Number)durableExpected.get("n2")).intValue()==row.getInt(4)&&((Number)durableExpected.get("n3")).intValue()==row.getInt(5),"REOPEN_ORIGINAL_TARGET_POSITION_K3_IDENTITY");
            need(durableExpected.get("authorization").equals(row.getString(6))&&Models.SOURCE_TYPE_REALTIME.equals(row.getString(9))&&row.getInt(10)==1,"REOPEN_TERMINAL_AUTHORIZATION_AND_PROVENANCE");
            for(int i=0;i<6;i++)need(durableExpected.get(new String[]{"candidate","model","certificate","proof","action","authorization"}[i]).equals(row.getString(11+i)),"REOPEN_EXACT_RECEIPT_IDENTITY_"+i);
            for(int i=0;i<3;i++)need(durableExpected.get(new String[]{"calibration","selection","scope"}[i]).equals(row.getString(17+i)),"REOPEN_CALIBRATION_SELECTION_SCOPE_"+i);
            double expected=((Number)durableExpected.get("probability")).doubleValue();need(Double.compare(expected,row.getDouble(7))==0&&Double.compare(expected,row.getDouble(8))==0&&Double.compare(expected,row.getDouble(20))==0,"REOPEN_IDENTICAL_EXACT_CALIBRATED_PROBABILITY");
            need(durableExpected.get("proof").equals(row.getString(21))&&durableExpected.get("action").equals(row.getString(22))&&durableExpected.get("authorization").equals(row.getString(23))&&!row.next(),"REOPEN_ONE_TERMINAL_CLOSURE_WITHOUT_DUPLICATE");
        }
        String closure=V51ChecksumEngine.sha256(JsonR27.write(durableExpected));Map<String,Object> terminal=new LinkedHashMap<>();terminal.put("scope","SYNTHETIC_ENGINEERING_INPUT_NOT_NATURAL_NOT_EXTERNAL_TRANSACTION");terminal.put("state","ACTUAL_ORIGINAL_TERMINAL_COMMITTED_CLOSED_REOPENED_VERIFIED");terminal.put("closureSha256",closure);terminal.put("productTables",3);terminal.put("terminalRowsPerTable",1);terminal.put("naturalQuality","NOT_PROVEN");terminal.put("Android","NOT_RUN");
        Files.writeString(root.toAbsolutePath().getParent().resolve("TERMINAL_COMMIT_READBACK.json"),JsonR27.write(terminal));
        try(java.util.stream.Stream<Path> files=Files.walk(root)){for(Path file:(Iterable<Path>)files.sorted(Comparator.reverseOrder())::iterator)Files.delete(file);}
        System.out.println("TYPED_MAINLINE_R31_HOST_PASS assertions="+assertions+" actualNegativeCases="+negativeCases+" actualTerminalCommitAndReopen=YES closureSha256="+closure+" inputScope=SYNTHETIC_ENGINEERING_INPUT_NOT_NATURAL_NOT_EXTERNAL_TRANSACTION naturalQuality=NOT_PROVEN Android=NOT_RUN isolatedDatabase=CLEANED");
    }
}
