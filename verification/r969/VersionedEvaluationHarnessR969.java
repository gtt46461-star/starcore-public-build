package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.*;
import java.util.*;
import org.json.*;

/** Controlled synthetic stream, actual numeric training, full1200 execution and certificate owners.
 * No prediction, calibration, window, gate, certificate or authorization rows are fabricated.
 * Artificial outcomes prove integration only, never prediction performance or live Future.
 */
public final class VersionedEvaluationHarnessR969 {
    private static int checks;private static boolean rejectNextLabel;
    static void check(boolean pass,String name){if(!pass)throw new AssertionError(name);checks++;System.out.println("PASS "+name);}
    static String text(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):"";}}
    static long count(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getLong(0):0;}}
    static void draw(SQLiteDatabase db,String issue,long at) {
        db.execSQL("INSERT INTO draws VALUES(?,1,2,3,4,5,6,7,8,9,10,0,?,?)",new Object[]{issue,at,at+1});
    }
    static void settle(DbStore store,SQLiteDatabase db,String issue) {
        long at=System.currentTimeMillis()+1;draw(db,issue,rejectNextLabel?at-60000:at);rejectNextLabel=false;
        String hash=V51ChecksumEngine.sha256("SYNTHETIC_TERMINAL|"+issue);
        db.execSQL("INSERT INTO "+DbHelper.TABLE_R36_SETTLEMENT_TASK+"(target_issue,draw_issue,status,created_at,updated_at,completed_at,draw_hash,settlement_hash) VALUES(?,?,'COMPLETED',?,?,?,?,?)",new Object[]{issue,issue,at+1,at+1,at+1,hash,hash});
        ResearchDirectorR952.onSettlementCompleted(store,store.getDrawByIssue(issue),hash,at+1);
    }
    static DeferredEvidenceSnapshotR36.Snapshot snapshot(String base,String target)throws Exception {
        DeferredEvidenceSnapshotR36.Snapshot old=ReportedCaptureHarnessR945.snapshot(base,target);
        String bundle=ModelBundleRuntimeR932.current().hash;
        ArrayList<DeferredEvidenceSnapshotR36.ActionRow> actions=new ArrayList<>();
        StringBuilder vector=new StringBuilder();for(int i=0;i<96;i++){if(i>0)vector.append(',');vector.append('0');}
        StringBuilder body=new StringBuilder();
        for(K3ActionSpaceEngine.ActionIdentity a:K3ActionSpaceEngine.enumerateIdentityUniverse()) {
            int fourth=1;while(a.contains(fourth))fourth++;
            double raw=a.globalOrdinal==119?.99:.1;
            String hash=V51ChecksumEngine.sha256("SYNTHETIC_ACTION|"+target+'|'+a.canonical);
            DeferredEvidenceSnapshotR36.ActionRow r=new DeferredEvidenceSnapshotR36.ActionRow(a.position,a.n1,a.n2,a.n3,fourth,raw,raw,raw,a.globalOrdinal+1,hash,vector.toString(),hash,hash);
            actions.add(r);body.append(K3ActionSpaceEngine.canonicalPayloadRow(r.position,r.n1,r.n2,r.n3,r.n4,r.rawActionP,r.rankScore,r.rank,r.fingerprint,r.actionHash));
        }
        String actionHash=K3ActionSpaceEngine.actionSpaceHashFromPayloadBody(body.toString(),1200);
        String record=V51ChecksumEngine.sha256("R36_SNAPSHOT|"+target+'|'+old.historyHash+'|'+bundle+'|'+old.contractHash+'|'+actionHash);
        String proof=RawTrainingInputR932.PREFIX+new JSONObject().put("model_bundle_hash",bundle).put("fixture_scope","SYNTHETIC_CHAMPION_IDENTITY_ONLY_NOT_RAW_TRAINING_EXPORT").toString();
        return new DeferredEvidenceSnapshotR36.Snapshot(target,base,old.historyHash,bundle,old.contractHash,actionHash,record,"",old.permutationHash,0,0,1,true,false,"",old.numbers,actions,Collections.singletonList(proof),old.spans);
    }
    public static void main(String[] args)throws Exception {
        SQLiteDatabase db=new SQLiteDatabase(args[0]);
        try {
            DbStore store=new DbStore(db);DbHelper.ensureR35Schema(db);DbHelper.ensureR36Schema(db);
            RuntimeEventLedgerR926.ensureSchema(db);DbHelper.ensureR906CriticalSchema(db);V51Repository.ensure(db);
            db.execSQL("CREATE TABLE draws(issue TEXT PRIMARY KEY,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER,open_time INTEGER,fetch_time INTEGER)");
            RealtimePressureGateR811.resetForTests();check(ResearchSchemaCoordinatorR926.ensureToReady(store.coordinatorR36(),8),"FROZEN_RUNTIME_SCHEMA_READY");
            // The installed-bundle registry is normally committed by the primary runtime.
            db.beginTransaction();try{ResearchPlatformStoreR929.activateRuntimeBundle(db,ModelBundleRuntimeR932.current(),"202609190001",System.currentTimeMillis());db.setTransactionSuccessful();}finally{db.endTransaction();}
            String base="202609190001";long start=System.currentTimeMillis()-10000000;
            db.beginTransaction();try{for(int i=0;i<120;i++){draw(db,base,start+i*60000);if(i<119)base=TargetIssueResolver.next(base).targetIssue;}db.setTransactionSuccessful();}finally{db.endTransaction();}
            String invalid=TargetIssueResolver.next(base).targetIssue;
            check(ResearchDirectorR952.capturePredraw(store,invalid,V51ChecksumEngine.sha256("REJECTED_PROOF_FIXTURE"),8)==1,"ACTUAL_CAPTURE_BEFORE_REJECTED_LABEL");
            rejectNextLabel=true;settle(store,db,invalid);base=invalid;
            check(count(db,"SELECT COUNT(*) FROM r952_candidate_predraw WHERE settled=0 AND proof_error<>''")==1,"REJECTED_PAST_PROOF_PRESERVED_UNSETTLED");
            check(count(db,"SELECT COUNT(*) FROM r952_research_outbox")==0,"REJECTED_LABEL_DOES_NOT_TRAIN");
            for(int i=0;i<120;) {
                String target=TargetIssueResolver.next(base).targetIssue;
                if(!ResearchEvaluationRunR967.trainingIssue(target)){if(ResearchDirectorR952.capturePredraw(store,target,V51ChecksumEngine.sha256("EVAL_ROLE|"+target),8)!=0)throw new AssertionError("SEALED_ROLE_CAPTURED_FOR_TRAIN");settle(store,db,target);base=target;continue;}
                if(ResearchDirectorR952.capturePredraw(store,target,V51ChecksumEngine.sha256("TRAIN|"+target),8)!=1)throw new AssertionError("TRAIN_FREEZE_"+i);
                settle(store,db,target);if(!ResearchOutboxR952.drainOne(store))throw new AssertionError("TRAIN_COMMIT_"+i);if(!PostSettlementResearchLoopR952.drainOne(store))throw new AssertionError("TRAIN_FEEDBACK_"+i);base=target;i++;
            }
            check(count(db,"SELECT MAX(numeric_updates) FROM r952_artifact")==120,"ACTUAL_120_NUMERIC_UPDATES_NOT_COUNTER_INSERTS");
            check(count(db,"SELECT COUNT(*) FROM "+ResearchEvaluationRunR967.EVAL)==0,"NO_EVALUATION_SUPPORT_INHERITED_FROM_TRAINING");
            String first=TargetIssueResolver.next(base).targetIssue;
            int online=ResearchDirectorR952.capturePredraw(store,first,V51ChecksumEngine.sha256("NEW_EVAL"),8);
            System.out.println("BOUNDARY online="+online+" frozen="+count(db,"SELECT COUNT(*) FROM "+ResearchEvaluationRunR967.EVAL)+" pending="+count(db,"SELECT COUNT(*) FROM r952_research_outbox WHERE state<>'COMPLETED'")+" state="+text(db,"SELECT state FROM r952_candidate")+" updates="+text(db,"SELECT a.numeric_updates FROM r952_learning_cursor l JOIN r952_artifact a ON a.artifact_hash=l.artifact_hash")+" latest="+text(db,"SELECT trained_until_issue FROM r952_learning_cursor"));
            check(online==0,"PREDECLARED_EVAL_ROLE_NOT_ONLINE_TRAINING");
            check(count(db,"SELECT COUNT(*) FROM "+ResearchEvaluationRunR967.EVAL)==1,"TRAINED_PARAMETERS_FROZEN_AUTOMATICALLY");
            check(count(db,"SELECT COUNT(*) FROM r952_candidate_predraw WHERE settled=0 AND proof_error<>''")==1,"REJECTED_ROW_RETAINED_WITHOUT_BLOCKING_FREEZE");
            check(com.ethk3.novaonepro.r952.FullbloodDiagnosticsProviderR952.render(store).contains("unsettledPredraw=0|rejectedPredraw=1|reason=FROZEN_VERSIONS_SEPARATE_FROM_CONTINUOUS_TRAINING"),"DIAGNOSTICS_DISTINGUISH_PENDING_FROM_REJECTED");
            String id=text(db,"SELECT candidate_id FROM "+ResearchEvaluationRunR967.EVAL),key="algorithm:"+id;
            FrozenCandidateAdapterR958.Frozen frozen=FrozenCandidateAdapterR958.load(db,id);
            String state=frozen.model.payload();
            boolean rejected=false;try{db.execSQL("UPDATE "+ResearchEvaluationRunR967.EVAL+" SET model_payload='corrupt'");}catch(RuntimeException good){rejected=true;}
            check(rejected,"FROZEN_MODEL_UPDATE_REJECTED");
            rejected=false;try{db.execSQL("DELETE FROM "+ResearchEvaluationRunR967.EVAL);}catch(RuntimeException good){rejected=true;}
            check(rejected,"FROZEN_MODEL_AND_IDENTITY_DELETE_REJECTED");
            check(FrozenCandidateAdapterR958.prepare(store,first)==0,"REPEATED_MODEL_FREEZE_DOES_NOT_RESET_EVIDENCE");
            // Every candidate/champion prediction and every CAL/window/proof row comes from owners.
            int continuedUpdates=0;
            for(int i=0;i<601;i++) {
                String target=TargetIssueResolver.next(base).targetIssue;
                if(ResearchEvaluationRunR967.trainingIssue(target)){
                    if(ResearchDirectorR952.capturePredraw(store,target,V51ChecksumEngine.sha256("CONTINUED_TRAIN|"+target),8)!=1)throw new AssertionError("ONGOING_NUMERIC_CAPTURE_"+i);
                    if(FrozenCandidateAdapterR958.capture(store,snapshot(base,target))!=0)throw new AssertionError("TRAIN_ROLE_BECAME_EVALUATION");
                    settle(store,db,target);if(!ResearchOutboxR952.drainOne(store))throw new AssertionError("CONTINUED_TRAIN_COMMIT_"+i);if(!PostSettlementResearchLoopR952.drainOne(store))throw new AssertionError("CONTINUED_FEEDBACK_"+i);continuedUpdates++;base=target;target=TargetIssueResolver.next(base).targetIssue;
                }
                if(ResearchDirectorR952.capturePredraw(store,target,V51ChecksumEngine.sha256("EVAL|"+target),8)!=0)throw new AssertionError("EVAL_ROLE_TRAINED_"+i);

                DeferredEvidenceSnapshotR36.Snapshot snapshot=snapshot(base,target);
                FixedResearchWindowR932.freezeChampion(store,snapshot);
                StagedResearchRosterR943.prepare(store,target,snapshot.recordHash);
                int expectedVersions=(int)count(db,"SELECT COUNT(*) FROM "+ResearchEvaluationRunR967.EVAL);
                if(FrozenCandidateAdapterR958.capture(store,snapshot)!=expectedVersions)throw new AssertionError("EVAL_CAPTURE_"+i+"|expected="+expectedVersions);
                if(i==0) {
                    check(count(db,"SELECT future_n FROM "+ResearchLifecycleR932.TABLE+" WHERE candidate_key='"+key+"'")==0,"FROZEN_CHILD_LIFECYCLE_STARTS_N_ZERO");
                    check(FrozenCandidateAdapterR958.capture(store,snapshot)==0,"EVALUATION_FREEZE_INSERT_ONCE");
                    try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.forEvidence(ModelBundleRuntimeR932.current().hash,target,System.currentTimeMillis()+1)) {
                        check(UnifiedResearchChoiceR932.freeze(store,target),"UNCERTIFIED_CHOICE_FREEZES_EMPTY");
                        check(!AlgorithmProductionGovernorR915.govern(store,base,target,snapshot.positionActions()).applied,"NO_CERTIFICATE_ZERO_MAINLINE_INFLUENCE");
                    }
                }
                settle(store,db,target);FixedResearchWindowR932.maintain(store);
                if(i==240) {
                    check(count(db,"SELECT COUNT(*) FROM "+CandidateCalibrationR932.MODEL)==1,"INDEPENDENT_120_FIT_1_EMBARGO_120_VALIDATION_FROZEN");
                    check(count(db,"SELECT COUNT(*) FROM "+FixedResearchWindowR932.WINDOW)==0,"CALIBRATION_ROWS_NOT_CERTIFICATE_WINDOW_SUPPORT");
                }
                if(i==360||i==600) {
                    for(int retry=0;retry<16;retry++)FixedWindowCertificatePublisherR932.maintain(store);
                    check(count(db,"SELECT COUNT(*) FROM "+FixedWindowCertificatePublisherR932.TABLE+" p JOIN "+FixedResearchWindowR932.WINDOW+" w ON w.window_id=p.window_id WHERE p.state='PUBLISHED' AND w.candidate_key='"+key+"'")==(i==360?1:2),"ACTUAL_WINDOW_PROOF_AUDIT_AND_PUBLICATION:"+i);
                }
                if(i%120==0)System.out.println("EVALUATION_PROGRESS "+(i+1)+"/601");
                base=target;
            }
            check(count(db,"SELECT COUNT(*) FROM "+CandidateExecutionAuditR932.TABLE+" WHERE candidate_key='"+key+"' AND action_count=1200")==601,"601_COMPLETE_ACTION_EXECUTION_REPLAYS");
            check(count(db,"SELECT COUNT(*) FROM "+FrozenCandidateAdapterR958.PREDRAW+" WHERE settled=1 AND candidate_id='"+id+"'")==601,"FROZEN_FUTURE_LABELS_COMMITTED_ONCE");
            check(count(db,"SELECT MAX(numeric_updates) FROM r952_artifact")==120+continuedUpdates&&state.equals(FrozenCandidateAdapterR958.load(db,id).model.payload()),"TRAIN_MODEL_CONTINUES_WHILE_FROZEN_VERSION_NEVER_MUTATES");
            check(count(db,"SELECT COUNT(*) FROM r952_research_outbox")==120+continuedUpdates,"EVALUATION_LABELS_NOT_ENQUEUED_FOR_TRAINING");
            check(count(db,"SELECT COUNT(*) FROM "+FixedResearchWindowR932.PAIR+" WHERE window_id IN (SELECT window_id FROM "+FixedResearchWindowR932.WINDOW+" WHERE candidate_key='"+key+"')")==360,"TWO_NEW_WINDOWS_HAVE_360_PAIRS_SEPARATE_FROM_CAL");
            ResearchLifecycleR932.maintain(store);
            check(text(db,"SELECT state FROM "+ResearchLifecycleR932.TABLE+" WHERE candidate_key='"+key+"'").equals("INFLUENCE"),"UNIFIED_LIFECYCLE_USES_SHARED_CERTIFICATE");
            check(count(db,"SELECT COUNT(*) FROM "+ResearchTaskQueueR967.TABLE+" WHERE kind='POST_TRAIN' AND status='COMPLETED'")==720,"ALL_720_COMMITTED_UPDATES_HAVE_REAL_POST_TRAIN_FEEDBACK");
            String trainOnly=TargetIssueResolver.next(base).targetIssue;
            try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.forEvidence(ModelBundleRuntimeR932.current().hash,trainOnly,System.currentTimeMillis()+1)){
                check(UnifiedResearchChoiceR932.freeze(store,trainOnly),"TRAIN_DOMAIN_CHOICE_EMPTY_SAFE");
                check(!AlgorithmProductionGovernorR915.govern(store,base,trainOnly,snapshot(base,trainOnly).positionActions()).applied,"STATIC_CERT_NOT_APPLIED_OUTSIDE_EVALUATED_DOMAIN");
            }
            // Advance a synthetic raw draw only; it is not a fabricated training receipt.
            settle(store,db,trainOnly);base=trainOnly;
            String next=TargetIssueResolver.next(base).targetIssue;
            DeferredEvidenceSnapshotR36.Snapshot nextSnapshot=snapshot(base,next);
            try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.forEvidence(ModelBundleRuntimeR932.current().hash,next,System.currentTimeMillis()+1)) {
                check(UnifiedResearchChoiceR932.freeze(store,next),"SHARED_SINGLE_CONSUMER_SELECTED_FROM_REAL_PUBLICATION");
                AlgorithmProductionGovernorR915.Result applied=AlgorithmProductionGovernorR915.govern(store,base,next,nextSnapshot.positionActions());
                check(applied.applied&&applied.influenceWeight>0&&applied.influenceWeight<=.05,"L6_FROZEN_MODEL_ACTUALLY_CONTRIBUTES_WITHIN_FIVE_PERCENT");
                FrozenConsumerRecipeR932.Recipe recipe=FrozenConsumerRecipeR932.decode(applied.frozenRecipe,next,"INVENTED_ALGORITHM_CERTIFIED",new JSONObject(applied.frozenRecipe).getString("id"),applied.influenceWeight);
                FrozenConsumerRecipeR932.Mixed replay=FrozenConsumerRecipeR932.execute(recipe,nextSnapshot.positionActions());
                check(replay.rankOne.checksum.equals(applied.rankOne.checksum),"FROZEN_CONSUMER_REPLAY_IDENTICAL_TO_ACTUAL_GOVERNOR");
                check(!AlgorithmProductionGovernorR915.MAY_AUTHORIZE_REAL&&!CertificateEvaluatorR952.MAY_CREATE_REAL,"RESEARCH_CONTRIBUTION_HAS_NO_REAL_AUTHORITY");
                check(ResearchPlatformStoreR929.revoke(store,applied.certificateHash,"SYNTHETIC_REVOCATION_TEST"),"REVOKE_VIA_SHARED_CERTIFICATE_OWNER");
                check(!AlgorithmProductionGovernorR915.govern(store,base,next,nextSnapshot.positionActions()).applied,"REVOKED_CERTIFICATE_IMMEDIATELY_REMOVES_INFLUENCE");
                JSONObject corrupt=new JSONObject(applied.frozenRecipe);corrupt.put("state",CandidateCompilerR952.Model.initial().payload());
                rejected=false;try{FrozenConsumerRecipeR932.execute(new FrozenConsumerRecipeR932.Recipe(corrupt),nextSnapshot.positionActions());}catch(RuntimeException good){rejected=true;}
                check(rejected,"ALTERED_NUMERIC_MODEL_REJECTED_IN_REPLAY");
            }
            check(continuedUpdates==600,"CONTINUOUS_600_REAL_NUMERIC_UPDATES_DURING_601_EVALUATIONS");
            check(count(db,"SELECT COUNT(*) FROM "+ResearchEvaluationRunR967.EVAL)==2,"TWO_IMMUTABLE_MODEL_VERSIONS_SAME_LEARNER");
            check(count(db,"SELECT COUNT(*) FROM "+ResearchEvaluationRunR967.INPUT)==721,"TRAIN_INPUTS_HAVE_IMMUTABLE_RUN_BINDING_INCLUDING_REJECTED_PROOF");
            check(count(db,"SELECT COUNT(*) FROM r952_candidate_predraw WHERE evidence_role='TRAIN_STREAM_V2' AND CAST(substr(target_issue,9) AS INTEGER)%2=0")==0,"NO_EVAL_TARGET_LABEL_IN_TRAIN_STREAM");
            check(count(db,"SELECT COUNT(*) FROM "+FrozenCandidateAdapterR958.PREDRAW+" WHERE CAST(substr(target_issue,9) AS INTEGER)%2=1")==0,"NO_TRAIN_TARGET_IN_FROZEN_EVALUATION");
            check(count(db,"SELECT COUNT(*) FROM r958_frozen_candidate")==0,"LEGACY_TABLE_NOT_REPURPOSED_FOR_NEW_VERSIONS");
            check(ResearchEvaluationRunR967.nextEvaluation(key,"202609191440").equals("202609200002"),"DISJOINT_SCHEDULE_CROSSES_DAY_WITHOUT_OVERLAP");
            System.out.println("R969_VERSIONED_EVALUATION_PASS|checks="+checks+"|scope=ACTUAL_PRODUCTION_OWNERS_SYNTHETIC_720_NUMERIC_TRAIN_601_FIRST_VERSION_EVAL_TWO_VERSIONS_NOT_LIVE_NOT_DEVICE_NOT_EFFECT");
        }finally{db.shutdown();}
    }
}
