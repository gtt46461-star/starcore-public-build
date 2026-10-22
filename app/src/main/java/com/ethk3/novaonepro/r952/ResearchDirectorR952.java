package com.ethk3.novaonepro.r952;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.util.ArrayList;

/** Online research samples are distinct from sealed certificate evaluation windows. */
public final class ResearchDirectorR952 {
    private ResearchDirectorR952() {}
    private static final String ONLINE = "ONLINE_TRAINING";

    /** Same active/current-rule/no-row domain as capturePredraw, used by durable continuation. */
    public static boolean hasPendingCapture(DbStore store,String target){
        if(store==null||!TargetIssueResolver.isValid(target))return false;
        try(Cursor c=store.readable().rawQuery("SELECT 1 FROM r952_candidate c WHERE c.ast NOT LIKE 'AST3:%' AND c.state NOT IN ('QUARANTINED','QUALITY_PRUNED','RESOURCE_RETIRED') AND EXISTS(SELECT 1 FROM "+ResearchEvaluationRunR967.RUN+" r WHERE r.learner_id=c.candidate_id AND r.first_issue<=? AND "+TrainingContinuationR973.predicate("r.rule_hash")+" AND r.first_issue=(SELECT MAX(r2.first_issue) FROM "+ResearchEvaluationRunR967.RUN+" r2 WHERE r2.learner_id=c.candidate_id AND r2.first_issue<=?)) AND NOT EXISTS(SELECT 1 FROM r952_candidate_predraw p WHERE p.candidate_id=c.candidate_id AND p.target_issue=?) LIMIT 1",new String[]{target,target,target})){
            return c.moveToFirst();
        }
    }

    public static int capturePredraw(final DbStore store, final String target,
            final String upstreamSnapshotHash, int maxCandidates) {
        if (store == null || !TargetIssueResolver.isValid(target)) return 0;
        // R971 continuity fence: a delayed maintenance/predraw task can survive past the draw.
        // Such a stale target is no longer a legal pre-draw input, but it must not abort the
        // current-period research owner.  Return no work before bootstrap/freeze owners run.
        if (drawExists(store.readable(), target)) return 0;
        ResearchModuleRegistryR952.reconcile(store);
        bootstrap(store,target);
        ResearchEvaluationRunR967.enroll(store,target);
        FrozenCandidateAdapterR958.prepare(store,target);
        final int bound=Math.max(1,Math.min(16,maxCandidates));
        final ArrayList<String[]> candidates=new ArrayList<String[]>();
        Cursor selected=store.readable().rawQuery("SELECT candidate_id,ast,structural_hash FROM r952_candidate c WHERE c.ast NOT LIKE 'AST3:%' AND state NOT IN ('QUARANTINED','QUALITY_PRUNED','RESOURCE_RETIRED') AND EXISTS(SELECT 1 FROM "+ResearchEvaluationRunR967.RUN+" r WHERE r.learner_id=c.candidate_id AND r.first_issue<=? AND "+TrainingContinuationR973.predicate("r.rule_hash")+" AND r.first_issue=(SELECT MAX(r2.first_issue) FROM "+ResearchEvaluationRunR967.RUN+" r2 WHERE r2.learner_id=c.candidate_id AND r2.first_issue<=?)) AND NOT EXISTS(SELECT 1 FROM r952_candidate_predraw p WHERE p.candidate_id=c.candidate_id AND p.target_issue=?) ORDER BY future_n ASC,created_at ASC,candidate_id ASC LIMIT ?",new String[]{target,target,target,String.valueOf(bound)});
        try { while(selected.moveToNext()) {
            String[] row={selected.getString(0),selected.getString(1),selected.getString(2)};
            CandidateAstR952 ast=CandidateAstR952.parse(row[1]);
            candidates.add(row);
        }} finally { selected.close(); }
        if(candidates.isEmpty())return 0;
        // Every batch at this target shares the same bounded history scope. Each program still
        // consumes only its own declared lookback; batch membership cannot change the snapshot.
        final PITSnapshotContractR952 pit=PITSnapshotContractR952.capture(store,target,5000);
        final String snapshot=V51ChecksumEngine.sha256("R958_SNAPSHOT|"+pit.snapshotHash+'|'+upstreamSnapshotHash);
        // R971: feature computation and sandbox execution do not own the shared DB writer.
        // Publication checks the exact captured model/AST again; a changed model is retried,
        // never silently published as if it were computed from the new weights.
        int insertedTotal=0;
        // R979: one independently replayable candidate per transaction. A later budget
        // yield cannot erase already frozen peers; the next pulse selects only missing rows.
        for(String[] candidate:candidates){
            final String[] row=candidate;
            ResearchWorkBudgetR927.checkpoint();
            CandidateAstR952 ast=CandidateAstR952.parse(row[1]);
            if(!ast.structuralHash.equals(row[2]))throw new IllegalStateException("R958_STRUCTURE_DRIFT");
            CandidateCompilerR952.Program program=CandidateCompilerR952.compile(ast);
            CandidateCompilerR952.Model model=loadModel(store.readable(),row[0],target);
            CandidateSandboxR952.Prediction prediction=CandidateSandboxR952.execute(program,pit,model);
            DatasetManifestR952 dataset=new DatasetManifestR952(pit.firstIssue,pit.lastIssue,target,
                    FeatureManifestR952.production().hash,"STRICT_POSITION_K3_HIT",program.codeHash,"EXPANDING_TIME_V1");
            final PreparedCaptureR971 prepared=new PreparedCaptureR971(row[0],ast,model,program.codeHash,dataset.hash,
                    prediction,V51ChecksumEngine.sha256("R958_INPUT|"+prediction.features+'|'+snapshot));
        Integer count=store.coordinatorForResearchR952().runAtomicWrite("R952_PREDRAW_FREEZE",
                new DatabaseCoordinator.WriteCallable<Integer>() { public Integer call(SQLiteDatabase db) {
            if(drawExists(db,target))throw new IllegalStateException("R952_POST_DRAW_FREEZE_REJECTED");
            MatrixSnapshotBusR952.publish(db,target,FeatureManifestR952.production().hash,snapshot);
            int inserted=0;
            final PreparedCaptureR971 row=prepared;
            {
                if(!captureStillCurrentR971(db,row,target))return 0;
                db.execSQL("INSERT OR IGNORE INTO r952_candidate_predraw(target_issue,candidate_id,prediction_mask,position,snapshot_hash,structure_hash,model_hash,code_hash,data_hash,captured_at,frozen_features,input_hash,evidence_role) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",
                        new Object[]{target,row.id,row.prediction.mask,row.ast.position,snapshot,row.ast.structuralHash,row.modelHash,row.codeHash,row.dataHash,System.currentTimeMillis(),row.prediction.features,row.input,ResearchEvaluationRunR967.ROLE});
                int didInsert=changed(db);
                ResearchEvaluationRunR967.bindInput(db,row.id,target,row.input,row.modelHash);
                ComputationReceiptR974.record(db,target,row.id,row.ast.structuralHash,row.modelHash,row.input,row.prediction);
                if(didInsert==1){inserted++;funnel(db,target,row.id,"FROZEN",snapshot,"model="+row.modelHash+"|updates="+row.updates);}
            }
            return inserted;
        }},null);
        if(count==null)throw new IllegalStateException("R958_PREDRAW_TRANSACTION_FAILED");
        insertedTotal+=count;
        }
        return insertedTotal;
    }

    private static final class PreparedCaptureR971 {
        final String id,modelHash,codeHash,dataHash,input;
        final CandidateAstR952 ast;
        final CandidateSandboxR952.Prediction prediction;
        final long updates;
        PreparedCaptureR971(String id,CandidateAstR952 ast,CandidateCompilerR952.Model model,
                String code,String data,CandidateSandboxR952.Prediction prediction,String input){
            this.id=id;this.ast=ast;modelHash=model.hash();updates=model.updates;
            codeHash=code;dataHash=data;this.prediction=prediction;this.input=input;
        }
    }
    private static boolean captureStillCurrentR971(SQLiteDatabase db,PreparedCaptureR971 p,String target){
        if(!db.inTransaction())throw new IllegalStateException("R971_CAPTURE_CAS_TRANSACTION");
        try(Cursor c=db.rawQuery("SELECT ast,structural_hash,state FROM r952_candidate WHERE candidate_id=?",new String[]{p.id})){
            if(!c.moveToFirst()||!p.ast.canonical.equals(c.getString(0))||!p.ast.structuralHash.equals(c.getString(1))
                    ||"QUARANTINED".equals(c.getString(2))||"QUALITY_PRUNED".equals(c.getString(2))||"RESOURCE_RETIRED".equals(c.getString(2)))return false;
        }
        return p.modelHash.equals(loadModel(db,p.id,target).hash());
    }

    public static int onSettlementCompleted(final DbStore store, final Models.Draw draw,
            final String settlementHash, final long completedAt) {
        if(store==null||draw==null||!draw.isValid()||draw.dirty!=0)return 0;
        final RuntimeException[] failure = new RuntimeException[1];
        Integer labels=store.coordinatorForResearchR952().runAtomicWrite("R952_STRICT_FUTURE_SETTLE",
                new DatabaseCoordinator.WriteCallable<Integer>() { public Integer call(SQLiteDatabase db) {
            try {
            ResearchMigrationRegistryR952.ensure(db);
            Cursor terminal=db.rawQuery("SELECT status,settlement_hash,completed_at FROM "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" WHERE target_issue=?",new String[]{draw.issue});
            try {
                if(!terminal.moveToFirst()||!"COMPLETED".equals(terminal.getString(0))
                        ||!settlementHash.equals(terminal.getString(1))||completedAt!=terminal.getLong(2)
                        ||completedAt<=0||!settlementHash.matches("[0-9a-f]{64}"))
                    throw new IllegalStateException("R958_AUTHORITATIVE_TERMINAL_REQUIRED");
            } finally {terminal.close();}
            Models.Draw persisted=store.getDrawByIssue(draw.issue);
            if(persisted==null||!persisted.numbersText().equals(draw.numbersText()))
                throw new IllegalStateException("R958_EXACT_DRAW_MISMATCH");
            Cursor c=db.rawQuery("SELECT p.candidate_id,p.prediction_mask,p.position,p.snapshot_hash,p.structure_hash,p.captured_at,p.input_hash,p.evidence_role,p.model_hash FROM r952_candidate_predraw p WHERE p.target_issue=? AND p.settled=0 AND p.proof_error='' ORDER BY p.candidate_id",new String[]{draw.issue});
            int n=0;
            try { while(c.moveToNext()) {
                String id=c.getString(0); int mask=c.getInt(1),position=c.getInt(2),actual=draw.getNumberAtPosition(position);
                long captured=c.getLong(5);
                if(captured<=0||captured>=completedAt||draw.openTime<=0||captured>=draw.openTime
                        ||AppConfig.countK3MaskNumbers(mask)!=3){
                    db.execSQL("UPDATE r952_candidate_predraw SET proof_error='R958_STRICT_FUTURE_PROOF_REJECTED' WHERE target_issue=? AND candidate_id=? AND settled=0",new Object[]{draw.issue,id});
                    funnel(db,draw.issue,id,"PROOF_REJECTED",c.getString(3),"captured="+captured+"|draw="+draw.openTime+"|mask="+mask);
                    continue;
                }
                int hit=(mask&(1<<(actual-1)))!=0?1:0;
                String label=V51ChecksumEngine.sha256("R958_LABEL|"+draw.issue+'|'+id+'|'+mask+'|'+position+'|'+actual+'|'+settlementHash+'|'+c.getString(3));
                db.execSQL("UPDATE r952_candidate_predraw SET settled=1,actual_number=?,hit=?,settlement_hash=?,label_hash=? WHERE target_issue=? AND candidate_id=? AND settled=0",
                        new Object[]{actual,hit,settlementHash,label,draw.issue,id});
                if(changed(db)!=1)continue;
                db.execSQL("UPDATE r952_candidate SET future_n=future_n+1 WHERE candidate_id=?",new Object[]{id});
                if(changed(db)!=1)throw new IllegalStateException("R958_CANDIDATE_MISSING");
                int future;
                Cursor fc=db.rawQuery("SELECT future_n FROM r952_candidate WHERE candidate_id=?",new String[]{id});
                try { if(!fc.moveToFirst())throw new IllegalStateException("R958_CANDIDATE_MISSING");future=fc.getInt(0); } finally {fc.close();}
                ResearchLifecycleR932.enrollR952(db,"r952:"+id,c.getString(4),draw.issue);
                String state=ResearchLifecycleR932.observeR952(db,"r952:"+id,draw.issue,label,future);
                db.execSQL("UPDATE r952_candidate SET state=? WHERE candidate_id=?",new Object[]{state,id});
                String role=c.getString(7);
                if(ResearchEvaluationRunR967.ROLE.equals(role)) {
                    ResearchEvaluationRunR967.verifyInput(db,id,draw.issue,c.getString(6),c.getString(8));
                    ResearchOutboxR952.enqueue(db,draw.issue,id,settlementHash,label,c.getString(6));
                    funnel(db,draw.issue,id,"STRICT_FUTURE",label,"TRAIN_STREAM_V2_LABEL|SEALED_FUTURE_N_UNCHANGED|hit="+hit);
                } else if(ONLINE.equals(role)) {
                    boolean legacyAllowed=ResearchEvaluationRunR967.current(db,id,draw.issue)==null;
                    try(Cursor boundary=db.rawQuery("SELECT 1 FROM r958_frozen_candidate WHERE learner_id=? AND created_issue<=? LIMIT 1",new String[]{id,draw.issue})) {
                        legacyAllowed=legacyAllowed&&!boundary.moveToFirst();
                    }
                    if(legacyAllowed)ResearchOutboxR952.enqueue(db,draw.issue,id,settlementHash,label,c.getString(6));
                    funnel(db,draw.issue,id,"STRICT_FUTURE",label,legacyAllowed?"LEGACY_TRAIN_LABEL":"LEGACY_LABEL_NO_TRAIN_BOUNDARY");
                } else {
                    funnel(db,draw.issue,id,"STRICT_FUTURE",label,"LABEL_ONLY_NO_TRAIN|role="+role);
                }
                n++;
            }} finally {c.close();}
            if(n>0)FeedbackMatrixRegistryR952.postDraw(db,draw.issue,settlementHash);
            n+=FrozenCandidateAdapterR958.settle(db,draw,settlementHash);
            n+=MultiHorizonProbeR80.settle(db,draw,settlementHash,completedAt);
            return n;
            } catch(RuntimeException bad) { failure[0]=bad; throw bad; }
        }},null);
        if(labels==null)throw new IllegalStateException("R958_LABEL_TRANSACTION_FAILED|target="+draw.issue,failure[0]);
        return labels;
    }

    /** Immutable prepared update; constructed only by the read/compute stage below. */
    public static final class PreparedTraining {
        private final String delivery,expected,parent,artifact;
        private final CandidateCompilerR952.Model next;
        private PreparedTraining(String delivery,String expected,String parent,
                String artifact,CandidateCompilerR952.Model next){
            this.delivery=delivery;this.expected=expected;this.parent=parent;
            this.artifact=artifact;this.next=next;
        }
    }
    private static final class TrainingInput {
        final String fingerprint,parent,replayArtifact,replayModel;
        final double[] features;final int actual;
        final CandidateCompilerR952.Model model;
        TrainingInput(String fp,String parent,String replayArtifact,String replayModel,
                double[] features,int actual,CandidateCompilerR952.Model model){
            this.fingerprint=fp;this.parent=parent;this.replayArtifact=replayArtifact;
            this.replayModel=replayModel;this.features=features;this.actual=actual;this.model=model;
        }
    }
    /** Called outside the writer transaction. Neither reads nor numeric work writes evidence. */
    public static PreparedTraining prepareTrainingLabel(SQLiteDatabase db,ResearchOutboxR952.Event e){
        if(db==null||db.inTransaction())throw new IllegalStateException("R967_TRAIN_PREPARE_OUTSIDE_TRANSACTION");
        TrainingInput input=trainingInput(db,e);
        if(!input.replayArtifact.isEmpty())return new PreparedTraining(e.deliveryHash,input.fingerprint,
                input.parent,input.replayArtifact,null);
        ResearchWorkBudgetR927.checkpoint();
        CandidateCompilerR952.Model next=input.model.update(input.features,input.actual);
        String artifact=V51ChecksumEngine.sha256("R958_ARTIFACT|"+e.candidateId+'|'+input.parent+'|'
                +e.targetIssue+'|'+e.deliveryHash+'|'+next.hash());
        return new PreparedTraining(e.deliveryHash,input.fingerprint,input.parent,artifact,next);
    }
    /** Writer-side CAS and commit only; numeric update() is deliberately NOT called here. */
    public static void commitTrainingLabel(SQLiteDatabase db,ResearchOutboxR952.Event e,PreparedTraining prepared){
        if(db==null||!db.inTransaction())throw new IllegalStateException("R967_TRAIN_COMMIT_TRANSACTION_REQUIRED");
        if(prepared==null||!prepared.delivery.equals(e.deliveryHash))throw new IllegalArgumentException("R967_TRAIN_PREPARED_ID");
        TrainingInput current=trainingInput(db,e);
        if(!current.replayArtifact.isEmpty()){
            if(!current.replayArtifact.equals(prepared.artifact)
                    ||(prepared.next!=null&&!prepared.next.hash().equals(current.replayModel)))
                throw new IllegalStateException("R967_TRAIN_REPLAY_DRIFT");
            return;
        }
        if(prepared.next==null||!current.fingerprint.equals(prepared.expected))
            throw new IllegalStateException("R967_TRAIN_PREPARED_SNAPSHOT_CHANGED");
        CandidateCompilerR952.Model next=prepared.next;
        db.execSQL("INSERT INTO r952_artifact(artifact_hash,candidate_id,parent_hash,target_issue,delivery_hash,payload_hash,created_at,model_payload,numeric_updates) VALUES(?,?,?,?,?,?,?,?,?)",
                new Object[]{prepared.artifact,e.candidateId,prepared.parent,e.targetIssue,e.deliveryHash,
                        next.hash(),System.currentTimeMillis(),next.payload(),next.updates});
        LearningCursorR952.advance(db,"R952:"+e.candidateId,e.targetIssue,prepared.artifact,e.deliveryHash);
        funnel(db,e.targetIssue,e.candidateId,"TRAINED_NUMERIC",prepared.artifact,"updates="+next.updates+"|payload="+next.hash());
    }
    private static TrainingInput trainingInput(SQLiteDatabase db,ResearchOutboxR952.Event e){
        if(e==null||!RuntimeEventContractR952.deliveryHash(e.targetIssue,e.candidateId,e.settlementHash,e.labelHash).equals(e.deliveryHash))
            throw new IllegalArgumentException("R967_TRAIN_EVENT_IDENTITY");
        String inputHash,frozen,snapshot;int actual,mask,position;
        try(Cursor label=db.rawQuery("SELECT settled,label_hash,settlement_hash,actual_number,frozen_features,input_hash,evidence_role,snapshot_hash,prediction_mask,position,model_hash FROM r952_candidate_predraw WHERE target_issue=? AND candidate_id=?",new String[]{e.targetIssue,e.candidateId})){
            if(!label.moveToFirst()||label.getInt(0)!=1||!e.labelHash.equals(label.getString(1))||!e.settlementHash.equals(label.getString(2)))
                throw new IllegalStateException("R952_TRAIN_LABEL_NOT_STRICT");
            String role=label.getString(6);
            if(ResearchEvaluationRunR967.ROLE.equals(role)){
                ResearchEvaluationRunR967.verifyInput(db,e.candidateId,e.targetIssue,label.getString(5),label.getString(10));
            }else if(ONLINE.equals(role)){
                // Historical rows are not silently upgraded into the new disjoint stream.
                try(Cursor boundary=db.rawQuery("SELECT 1 FROM r958_frozen_candidate WHERE learner_id=? AND created_issue<=?",new String[]{e.candidateId,e.targetIssue})){
                    if(boundary.moveToFirst())throw new IllegalStateException("R958_EVALUATION_LABEL_CANNOT_TRAIN");
                }
                if(ResearchEvaluationRunR967.current(db,e.candidateId,e.targetIssue)!=null)throw new IllegalStateException("R969_LEGACY_LABEL_CANNOT_CROSS_STREAM_BOUNDARY");
            }else throw new IllegalStateException("R958_NOT_ONLINE_TRAINING_INPUT");
            frozen=label.getString(4);snapshot=label.getString(7);inputHash=label.getString(5);
            if(!V51ChecksumEngine.sha256("R958_INPUT|"+frozen+'|'+snapshot).equals(inputHash)||!inputHash.equals(e.modelInputHash))
                throw new IllegalStateException("R958_INPUT_HASH_DRIFT");
            actual=label.getInt(3);mask=label.getInt(8);position=label.getInt(9);
            if(actual<1||actual>10||position<1||position>10||AppConfig.countK3MaskNumbers(mask)!=3
                    ||!V51ChecksumEngine.sha256("R958_LABEL|"+e.targetIssue+'|'+e.candidateId+'|'+mask+'|'+position+'|'+actual+'|'+e.settlementHash+'|'+snapshot).equals(e.labelHash))
                throw new IllegalStateException("R967_TRAIN_LABEL_CONTENT_DRIFT");
        }
        double[] features=CandidateCompilerR952.decode(frozen,true);
        try(Cursor replay=db.rawQuery("SELECT artifact_hash,model_payload,payload_hash,candidate_id,target_issue FROM r952_artifact WHERE delivery_hash=?",new String[]{e.deliveryHash})){
            if(replay.moveToFirst()){
                if(!e.candidateId.equals(replay.getString(3))||!e.targetIssue.equals(replay.getString(4))||replay.getString(1).isEmpty())
                    throw new IllegalStateException("R967_TRAIN_ARTIFACT_IDENTITY");
                CandidateCompilerR952.Model m=CandidateCompilerR952.Model.parse(replay.getString(1));
                if(!m.hash().equals(replay.getString(2)))throw new IllegalStateException("R958_ARTIFACT_PAYLOAD_DRIFT");
                return new TrainingInput("","",replay.getString(0),m.hash(),features,actual,m);
            }
        }
        String parent="",cutoff="",lastDelivery="";CandidateCompilerR952.Model model=CandidateCompilerR952.Model.initial();
        // Cursor and referenced model are read by ONE SELECT, not two mutually drifting queries.
        try(Cursor c=db.rawQuery("SELECT c.trained_until_issue,c.artifact_hash,c.last_delivery_hash,a.model_payload,a.payload_hash,a.candidate_id FROM r952_learning_cursor c LEFT JOIN r952_artifact a ON a.artifact_hash=c.artifact_hash WHERE c.consumer_id=?",new String[]{"R952:"+e.candidateId})){
            if(c.moveToFirst()){
                cutoff=c.getString(0);parent=c.getString(1);lastDelivery=c.getString(2);
                if(cutoff.compareTo(e.targetIssue)>=0)throw new IllegalStateException("R958_MODEL_NOT_PIT");
                if(c.isNull(3)||!e.candidateId.equals(c.getString(5)))throw new IllegalStateException("R958_MODEL_ARTIFACT_MISSING");
                if(!c.getString(3).isEmpty()){
                    model=CandidateCompilerR952.Model.parse(c.getString(3));
                    if(!model.hash().equals(c.getString(4)))throw new IllegalStateException("R958_MODEL_HASH_DRIFT");
                }
            }
        }
        ResearchEvaluationRunR967.Run active=ResearchEvaluationRunR967.current(db,e.candidateId,e.targetIssue);
        if(active!=null&&ResearchEvaluationRunR967.SPLIT.equals(active.split)&&cutoff.compareTo(active.first)<0)
            model=CandidateCompilerR952.Model.parse(active.initial);
        String[] fields={e.deliveryHash,e.targetIssue,e.candidateId,e.settlementHash,e.labelHash,inputHash,
                Integer.toString(actual),parent,cutoff,lastDelivery,model.hash()};
        StringBuilder framed=new StringBuilder("R967_TRAIN_SNAPSHOT_V1");
        for(String value:fields){if(value==null)throw new IllegalStateException("R967_TRAIN_NULL_IDENTITY");framed.append('|').append(value.length()).append(':').append(value);}
        return new TrainingInput(V51ChecksumEngine.sha256(framed.toString()),parent,"","",features,actual,model);
    }

    static CandidateCompilerR952.Model loadModel(SQLiteDatabase db,String id,String target) {
        Cursor c=db.rawQuery("SELECT c.trained_until_issue,c.artifact_hash,a.model_payload,a.payload_hash,a.candidate_id FROM r952_learning_cursor c LEFT JOIN r952_artifact a ON a.artifact_hash=c.artifact_hash WHERE c.consumer_id=?",new String[]{"R952:"+id});
        try {
            if(!c.moveToFirst()){ResearchEvaluationRunR967.Run run=ResearchEvaluationRunR967.current(db,id,target);
                return run==null?CandidateCompilerR952.Model.initial():CandidateCompilerR952.Model.parse(run.initial);}
            if(c.getString(0).compareTo(target)>=0)throw new IllegalStateException("R958_MODEL_NOT_PIT");
            if(c.isNull(2)||!id.equals(c.getString(4)))throw new IllegalStateException("R958_MODEL_ARTIFACT_MISSING");
            // Preserve old receipts but start real numeric training at zero, explicitly.
            if(c.getString(2).isEmpty())return CandidateCompilerR952.Model.initial();
            CandidateCompilerR952.Model model=CandidateCompilerR952.Model.parse(c.getString(2));
            if(!model.hash().equals(c.getString(3)))throw new IllegalStateException("R958_MODEL_HASH_DRIFT");
            ResearchEvaluationRunR967.Run run=ResearchEvaluationRunR967.current(db,id,target);
            if(run!=null&&ResearchEvaluationRunR967.SPLIT.equals(run.split)&&c.getString(0).compareTo(run.first)<0)
                return CandidateCompilerR952.Model.parse(run.initial);
            return model;
        } finally {c.close();}
    }

    /** Preserve the durable six-slot order; skip bounded empty turns in the same wake-up. */
    public static int pulse(DbStore store,long budgetMs) {
        if(store==null)return 0;
        long available=Math.min(Math.max(1L,Math.min(850L,budgetMs)),ResearchWorkBudgetR927.remainingMs());
        long overallEnd=System.nanoTime()+available*1000000L;
        int total=0;
        // Keep the durable wheel order, but use the remaining slice after a short
        // successful commit. IO-heavy TRAIN/RESEARCH/EVOLVE/IMPORT use the remaining bounded
        // lane slice; a fixed 250ms ceiling starved preparation on slower storage.
        for(int turn=0;turn<6;turn++) {
            ResearchWorkBudgetR927.checkpoint();
            if(RealtimePressureGateR811.hasActiveCriticalWork()||SettlementExecutionGateR819.isActive()
                    ||System.nanoTime()+75_000_000L>=overallEnd)return total;
            String slot=advanceDirectorSlotCP12(store,overallEnd);if(slot==null)return total;
            ResearchWorkBudgetR927.checkpoint();
            long end=slot.equals("TRAIN")||slot.equals("RESEARCH")||slot.equals("EVOLVE")||slot.equals("IMPORT")
                    ?overallEnd-75_000_000L:Math.min(overallEnd-75_000_000L,System.nanoTime()+250_000_000L);
            total+=runDirectorSlotR65(store,slot,end);
        }
        return total;
    }
    private static int runDirectorSlotR65(DbStore store,String slot,long end) {
        switch(slot){
            case "TRAIN":return ResearchTrainingHookRegistryR952.drainTrainingSlotCP11(store,end)?1:0;
            case "POST":{
                // Use this owner's existing quantum for small durable projections.
                // A single receipt per six-slot turn let completed numeric updates
                // outrun feedback. Each unit still claims/commits independently.
                int completed=0;
                long remaining=Math.min(ResearchWorkBudgetR927.remainingMs(),Math.max(1L,(end-System.nanoTime())/1_000_000L));
                ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(remaining);
                try{
                    while(completed<16&&System.nanoTime()+50_000_000L<end){
                        ResearchWorkBudgetR927.checkpoint();
                        if(!drainPostSlotCP12(store,end))break;
                        completed++;
                    }
                }finally{ResearchWorkBudgetR927.leave(scope);}
                return completed;
            }
            case "MATRIX":return MatrixSnapshotBusR952.drainOneCP4(store,end)?1:0;
            case "RESEARCH":
                // The subturn is committed by the existing six-slot claim below.
                // Feedback and summaries cannot starve each other on a large backlog.
                if("0".equals(store.getState("r81_long_research_turn","1"))){
                    if(LongScaleSummaryR80.drainOne(store,end))return 1;
                    String result=LongCandidateLifecycleR81.maintain(store,end);
                    if(!result.equals(store.getState("r81_long_candidate_status","")))store.setState("r81_long_candidate_status",result);
                    return result.startsWith("LONG_WAIT:")||result.equals("LONG_YIELD")?0:1;
                }
                return ResearchFeedbackCP12.drainOne(store,end)?1:0;
            case "EVOLVE":return EvolutionControllerR952.drainActionCP12(store,end)?1:0;
            case "IMPORT":return drainImportSliceCP12(store,end);
            default:throw new IllegalStateException("DIRECTOR_SLOT");
        }
    }

    /** Use the assigned import slice for bounded pages, each committed by its existing owner.
     * The six-slot wheel and the archive/seed subslot still alternate. Previously a fast
     * 32-atom page discarded nearly all of this slice and waited another entire wheel.
     * Never hold one writer transaction across pages or continue into critical work.
     */
    private static int drainImportSliceCP12(DbStore store,long end) {
        int advanced=0,empty=0;
        if(ModernSeedFactoryCP12.completeCP12(store.readable())&&System.nanoTime()+75000000L<end){
            String bound=BoundAtomLifecycleR80.maintainR80(store,end);
            if(bound.equals("BOUND_COHORT_PREREGISTERED")||bound.equals("BOUND_FRESH_ACTION_PROPOSED")||bound.startsWith("R967-"))advanced++;
        }
        for(int page=0;page<8&&System.nanoTime()+50000000L<end;page++) {
            ResearchWorkBudgetR927.checkpoint();
            if(RealtimePressureGateR811.hasActiveCriticalWork()||SettlementExecutionGateR819.isActive())break;
            if(HistoricalGenomeImportR967.maintainCP12(store,end)) {advanced++;empty=0;}
            else if(++empty>=2)break;
        }
        return advanced;
    }

    private static void bootstrap(final DbStore store,final String issue) {
        final CandidateAstR952 ast=new CandidateAstR952(CandidateAstR952.Operator.RECENCY_BLEND,1,120);
        final String id="R952-C-"+ast.structuralHash.substring(0,20);
        // Registration belongs to the immutable identity, not each issue or each retry.
        boolean lifecycleReady;
        try(Cursor table=store.readable().rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{ResearchLifecycleR932.TABLE})) {lifecycleReady=table.moveToFirst();}
        if(lifecycleReady) {
            try(Cursor c=store.readable().rawQuery("SELECT c.ast,c.structural_hash,c.may_create_real,l.structural_hash FROM r952_candidate c JOIN "+ResearchLifecycleR932.TABLE+" l ON l.candidate_key=('r952:'||c.candidate_id) WHERE c.candidate_id=?",new String[]{id})) {
                if(c.moveToFirst()) {
                    if(!ast.canonical.equals(c.getString(0))||!ast.structuralHash.equals(c.getString(1))
                            ||c.getInt(2)!=0||!ast.structuralHash.equals(c.getString(3)))throw new IllegalStateException("R958_BOOTSTRAP_IDENTITY_DRIFT");
                    return;
                }
            }
        }
        boolean ok=store.coordinatorForResearchR952().runAtomicWrite("R952_BOOTSTRAP_CANDIDATE",new DatabaseCoordinator.WriteRunnable(){public void run(SQLiteDatabase db){
            ResearchMigrationRegistryR952.ensure(db);
            if(drawExists(db,issue))throw new IllegalStateException("R958_BOOTSTRAP_POSTDRAW");
            String intent=ResearchIntentLedgerR952.put(db,"FUTURE_DEFICIT","","N=0","STRICT_K3_HIT,BRIER","POSITION_FREQUENCY,RECENCY_BLEND",1,issue);
            db.execSQL("INSERT OR IGNORE INTO r952_candidate(candidate_id,intent_id,ast,structural_hash,parent_id,state,future_n,created_issue,may_create_real,created_at) VALUES(?,?,?,?,?,'NURSERY',0,?,0,?)",new Object[]{id,intent,ast.canonical,ast.structuralHash,"",issue,System.currentTimeMillis()});
            ResearchLifecycleR932.enrollR952(db,"r952:"+id,ast.structuralHash,issue);
            funnel(db,issue,id,"REGISTERED",ast.structuralHash,"");
        }});
        if(!ok)throw new IllegalStateException("R958_BOOTSTRAP_COMMIT_FAILED");
    }
    private static boolean drawExists(SQLiteDatabase db,String target){Cursor c=db.rawQuery("SELECT 1 FROM "+DrawDependencyInputCP12.SOURCE+" WHERE issue=? LIMIT 1",new String[]{target});try{return c.moveToFirst();}finally{c.close();}}
    private static int changed(SQLiteDatabase db){Cursor c=db.rawQuery("SELECT changes()",null);try{return c.moveToFirst()?c.getInt(0):0;}finally{c.close();}}
    private static void funnel(SQLiteDatabase db,String target,String id,String stage,String hash,String reason){db.execSQL("INSERT OR IGNORE INTO r952_candidate_funnel(target_issue,candidate_id,stage,stage_hash,reason,created_at) VALUES(?,?,?,?,?,?)",new Object[]{target,id,stage,hash,reason,System.currentTimeMillis()});RuntimeEventContractR952.record(db,stage,target,id,hash);}

    /** Sole writer of development artifact/checkpoint, followed atomically by FREEZE enqueue. */
    public static String commitCandidateBatchCP11(SQLiteDatabase db,ResearchTaskQueueR967.Lease lease,
            ResearchTrainingHookRegistryR952.DevelopmentBatchCP11 batch) {
        if(db==null||!db.inTransaction()||lease==null||batch==null||!"TRAIN_CANDIDATE".equals(lease.kind)
                ||!lease.id.equals(batch.taskId)||!lease.sequence.equals(batch.protocolHash)
                ||!lease.inputHash.equals(batch.inputHash)||!ResearchTaskQueueR967.owns(db,lease))
            throw new IllegalStateException("TRAIN_COMMIT_IDENTITY_OR_LEASE");
        LongSummaryBindingsR80.validateTaskSources(db,lease);
        try(Cursor c=db.rawQuery("SELECT p.ast_hash,p.feature_manifest_hash,p.train_n,p.validation_n,p.validation_first,p.validation_last,p.model_kind,c.ast,c.structural_hash,c.state FROM research_training_protocol_v2 p JOIN r952_candidate c ON c.candidate_id=p.candidate_id WHERE p.protocol_hash=? AND p.candidate_id=? AND p.input_hash=?",new String[]{lease.sequence,lease.candidate,lease.inputHash})){
            if(!c.moveToFirst()||!batch.astHash.equals(c.getString(0))||!batch.manifestHash.equals(c.getString(1))
                    ||batch.trainN!=c.getLong(2)||c.getInt(3)!=120||!batch.validationFirst.equals(c.getString(4))
                    ||!batch.validationLast.equals(c.getString(5))||!batch.astHash.equals(c.getString(8))
                    ||"QUARANTINED".equals(c.getString(9))||"QUALITY_PRUNED".equals(c.getString(9))
                    ||"RESOURCE_RETIRED".equals(c.getString(9)))throw new IllegalStateException("TRAIN_PROTOCOL_CAS");
            CandidateAstR952 ast=CandidateAstR952.parse(c.getString(7));
            if(ast.formatVersion!=3||!ast.structuralHash.equals(batch.astHash)
                    ||!ast.graphSpec.modelKind.name().equals(c.getString(6)))throw new IllegalStateException("TRAIN_AST_CAS");
        }
        String checkpointHash="";long checkpointGeneration=0;
        try(Cursor c=db.rawQuery("SELECT payload_hash,lease_generation,input_hash FROM research_task_checkpoint_v2 WHERE task_id=?",new String[]{lease.id})){
            if(c.moveToFirst()){checkpointHash=c.getString(0);checkpointGeneration=c.getLong(1);
                if(!lease.inputHash.equals(c.getString(2)))throw new IllegalStateException("CHECKPOINT_INPUT_CAS");}
        }
        if(!checkpointHash.equals(batch.oldCheckpointHash)||checkpointGeneration!=batch.oldCheckpointGeneration)
            throw new IllegalStateException("CHECKPOINT_PREDECESSOR_CAS");
        long now=System.currentTimeMillis();
        if(!batch.complete){
            if(checkpointHash.isEmpty())db.execSQL("INSERT INTO research_task_checkpoint_v2(task_id,input_hash,model_hash,phase,cursor,partial_payload,payload_hash,lease_generation,updated_at) VALUES(?,?,?,?,?,?,?,?,?)",new Object[]{lease.id,lease.inputHash,batch.modelHash,batch.phase,batch.rowCursor,batch.checkpoint(),batch.newCheckpointHash,lease.generation,now});
            else {
                db.execSQL("UPDATE research_task_checkpoint_v2 SET model_hash=?,phase=?,cursor=?,partial_payload=?,payload_hash=?,lease_generation=?,updated_at=? WHERE task_id=? AND input_hash=? AND payload_hash=? AND lease_generation=?",new Object[]{batch.modelHash,batch.phase,batch.rowCursor,batch.checkpoint(),batch.newCheckpointHash,lease.generation,now,lease.id,lease.inputHash,checkpointHash,checkpointGeneration});
                if(changed(db)!=1)throw new IllegalStateException("CHECKPOINT_CAS");
            }
            if(!ResearchTaskQueueR967.yield(db,lease,batch.queueCursor,"TRAIN_SLICE",250))throw new IllegalStateException("CHECKPOINT_QUEUE_CAS");
            return "YIELDED";
        }
        String delivery=V51ChecksumEngine.sha256("CP8_DEV_DELIVERY|"+batch.protocolHash);
        String artifact=V51ChecksumEngine.sha256("CP8_DEV_ARTIFACT|"+lease.candidate+'|'+batch.protocolHash+'|'+batch.modelHash+'|'+batch.receiptHash);
        // This identity was computed/verified outside the writer. No numerical work is done here.
        db.execSQL("INSERT INTO r952_artifact(artifact_hash,candidate_id,parent_hash,target_issue,delivery_hash,payload_hash,created_at,model_payload,numeric_updates,training_kind,development_receipt_hash,development_sample_n,validation_sample_n,validation_pass,validation_hash) VALUES(?,?,'',?,?,?,?,?,?,'DEVELOPMENT_AST3',?,?,120,1,?)",new Object[]{artifact,lease.candidate,batch.validationLast,delivery,batch.modelHash,now,batch.modelPayload,batch.trainN,batch.receiptHash,batch.trainN,batch.validationHash});
        RetentionArtifactIdentityCP12.register(db,artifact);
        db.execSQL("INSERT INTO research_development_receipt_v2(receipt_hash,artifact_hash,protocol_hash,validation_hash,receipt_json,validation_json,created_at) VALUES(?,?,?,?,?,?,?)",new Object[]{batch.receiptHash,artifact,batch.protocolHash,batch.validationHash,batch.receiptJson,batch.validationJson,now});
        LearningCursorR952.advance(db,"R952:"+lease.candidate,batch.validationLast,artifact,delivery);
        ResearchTaskQueueR967.complete(db,lease,artifact);
        ResearchTaskQueueR967.enqueue(db,"FREEZE_CANDIDATE",lease.candidate,lease.issue,lease.sequence,lease.inputHash,artifact);
        db.execSQL("UPDATE r952_candidate SET state='VALIDATED' WHERE candidate_id=? AND structural_hash=? AND may_create_real=0",new Object[]{lease.candidate,batch.astHash});
        if(changed(db)!=1)throw new IllegalStateException("TRAIN_CANDIDATE_STATE_CAS");
        funnel(db,lease.issue,lease.candidate,"VALIDATED",batch.validationHash,"DEVELOPMENT_ONLY|FUTURE_N_UNCHANGED");
        return artifact;
    }
    /** Immutable parent/action expectations captured before any file preparation. */
    public static final class PreparedEvolution {
        public final String candidateId,actionId,parentId,intentId,expectedParentHash,expectedActionEvidenceHash;
        public final String secondaryParentId,expectedSecondaryParentHash;
        public PreparedEvolution(String candidateId,String actionId,String parentId,String intentId,String expectedParentHash,String expectedActionEvidenceHash) {
            this(candidateId,actionId,parentId,intentId,expectedParentHash,expectedActionEvidenceHash,"","");
        }
        public PreparedEvolution(String candidateId,String actionId,String parentId,String intentId,String expectedParentHash,String expectedActionEvidenceHash,String secondaryParentId,String expectedSecondaryParentHash) {
            if(candidateId==null||candidateId.isEmpty()||actionId==null||actionId.isEmpty()||parentId==null||intentId==null||intentId.isEmpty()
                    ||expectedParentHash==null||expectedActionEvidenceHash==null||!expectedActionEvidenceHash.matches("[0-9a-f]{64}")
                    ||secondaryParentId==null||expectedSecondaryParentHash==null||(!parentId.isEmpty()&&!expectedParentHash.matches("[0-9a-f]{64}"))
                    ||(!secondaryParentId.isEmpty()&&(!expectedSecondaryParentHash.matches("[0-9a-f]{64}")||parentId.equals(secondaryParentId))))
                throw new IllegalArgumentException("REGISTER_EVOLUTION_EXPECTATION");
            this.candidateId=candidateId;this.actionId=actionId;this.parentId=parentId;this.intentId=intentId;
            this.expectedParentHash=expectedParentHash;this.expectedActionEvidenceHash=expectedActionEvidenceHash;
            this.secondaryParentId=secondaryParentId;this.expectedSecondaryParentHash=expectedSecondaryParentHash;
        }
    }
    public static DevelopmentProductionCP12.PreparedProtocol prepareTrainingProtocolCP5(DbStore store,CandidateAstR952 ast,String candidateId,String actionId,String registrationIssue) {
        return DevelopmentProductionCP12.prepare(store,ast,candidateId,actionId,registrationIssue);
    }
    /** Sole modern child writer. Caller also completes its owned RESEARCH_ACTION lease in this transaction. */
    public static String commitPreparedCandidateCP5(SQLiteDatabase db,DevelopmentProductionCP12.PreparedProtocol p,PreparedEvolution child) {
        if(db==null||!db.inTransaction()||p==null||child==null)throw new IllegalStateException("REGISTER_TX_REQUIRED");p.requireOpen();
        if(!p.candidateId.equals(child.candidateId)||!p.actionId.equals(child.actionId)||p.ast.formatVersion!=3
                ||drawExists(db,p.registeredIssue))throw new IllegalStateException("REGISTER_PREPARED_IDENTITY");
        LongSummaryBindingsR80.validateProtocolSources(db,StrictCanonicalJson.readContextObject(p.protocolJson));
        if(DevelopmentProductionCP12.historyGeneration(db)!=p.historyGeneration)throw new DevelopmentProductionCP12.Wait("TRAIN_HISTORY_CHANGED");
        String latest="";try(Cursor c=db.rawQuery("SELECT issue FROM "+DbHelper.TABLE_DRAWS+" WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)){if(c.moveToFirst())latest=c.getString(0);}
        if(!TargetIssueResolver.isValid(latest)||!TargetIssueResolver.next(latest).targetIssue.equals(p.registeredIssue))throw new DevelopmentProductionCP12.Wait("TARGET_SUPERSEDED");
        if(!p.lastIssue.equals(CalibrationRepairR75.developmentCutoff(db,child.actionId,p.selectionSourceIssue)))throw new IllegalStateException("REGISTER_DEVELOPMENT_CUTOFF_CAS");
        String parentAst=readParentCP12(db,child.parentId,child.expectedParentHash,child.actionId),secondAst=readParentCP12(db,child.secondaryParentId,child.expectedSecondaryParentHash,child.actionId);
        java.util.TreeSet<String> parentHashes=new java.util.TreeSet<>();if(!child.parentId.isEmpty())parentHashes.add(child.expectedParentHash);if(!child.secondaryParentId.isEmpty())parentHashes.add(child.expectedSecondaryParentHash);
        String parents=StrictCanonicalJson.writeContext(new java.util.ArrayList<>(parentHashes));
        try(Cursor c=db.rawQuery("SELECT source_issue,selection_boundary,evidence_hash,boundary_hash FROM research_action_input_boundary_cp12 WHERE action_id=?",new String[]{child.actionId})){
            if(!c.moveToFirst()||!p.selectionSourceIssue.equals(c.getString(0))||!TargetIssueResolver.next(p.selectionSourceIssue).targetIssue.equals(c.getString(1))||!child.expectedActionEvidenceHash.equals(c.getString(2))
                    ||!V51ChecksumEngine.sha256("CP12_ACTION_BOUNDARY|"+child.actionId+'|'+c.getString(0)+'|'+c.getString(1)+'|'+c.getString(2)).equals(c.getString(3)))throw new IllegalStateException("REGISTER_SELECTION_BOUNDARY_CAS");
        }
        try(Cursor c=db.rawQuery("SELECT state,evidence_hash,parent_hashes_json,action_type FROM research_action_v2 WHERE action_id=?",new String[]{child.actionId})){
            if(!c.moveToFirst()||!"PROPOSED".equals(c.getString(0))||!child.expectedActionEvidenceHash.equals(c.getString(1))
                    ||!parents.equals(c.getString(2))||"HOLD".equals(c.getString(3)))throw new IllegalStateException("REGISTER_ACTION_CAS");
        }
        try(Cursor c=db.rawQuery("SELECT candidate_id,ast,structural_hash FROM r952_candidate WHERE structural_hash=?",new String[]{p.ast.structuralHash})){
            if(c.moveToFirst()){
                if(!p.ast.canonical.equals(c.getString(1)))throw new IllegalStateException("REGISTER_HASH_COLLISION");
                db.execSQL("UPDATE research_action_v2 SET state='NO_OP',child_id=?,last_error='EXISTING_STRUCTURE_NO_NEW_GENERATION' WHERE action_id=? AND state='PROPOSED' AND evidence_hash=?",new Object[]{c.getString(0),child.actionId,child.expectedActionEvidenceHash});
                if(changed(db)!=1)throw new IllegalStateException("REGISTER_NO_OP_CAS");return "NO_OP:"+c.getString(0);
            }
        }
        // Ordered creation admission: exact genome above, then canonical semantic program.
        // Prediction/error similarity require prospective common Future and are certificate-stage gates.
        try(Cursor duplicate=db.rawQuery("SELECT candidate_id FROM r952_candidate WHERE semantic_hash=?",new String[]{p.ast.semanticHashCP12})){
            if(duplicate.moveToFirst()){
                db.execSQL("UPDATE research_action_v2 SET state='NO_OP',child_id=?,last_error='EXISTING_SEMANTIC_PROGRAM_NO_NEW_GENERATION' WHERE action_id=? AND state='PROPOSED' AND evidence_hash=?",new Object[]{duplicate.getString(0),child.actionId,child.expectedActionEvidenceHash});
                if(changed(db)!=1)throw new IllegalStateException("REGISTER_SEMANTIC_NO_OP_CAS");return "NO_OP:"+duplicate.getString(0);
            }
        }
        if(!trainingQueueHasCapacityR65(db))throw new DevelopmentProductionCP12.Wait("TRAIN_INPUT_CAPACITY");
        long activeBytes=0;java.util.HashSet<String> activeHashes=new java.util.HashSet<>();
        String active="SELECT p.protocol_hash,p.input_hash FROM research_training_protocol_v2 p WHERE EXISTS(SELECT 1 FROM v57_research_task_r967 q WHERE q.candidate_id=p.candidate_id AND q.kind IN('TRAIN_CANDIDATE','FREEZE_CANDIDATE') AND q.status NOT IN('COMPLETED','EXPIRED','FAILED_TERMINAL'))";
        try(Cursor c=db.rawQuery("SELECT i.input_hash,i.byte_count FROM research_training_input_v2 i WHERE i.input_hash IN(SELECT input_hash FROM ("+active+"))",null)){while(c.moveToNext()){activeHashes.add(c.getString(0));activeBytes+=c.getLong(1);}}
        try(Cursor c=db.rawQuery("SELECT a.attachment_hash,a.byte_count FROM research_training_attachment_cp12 a WHERE EXISTS(SELECT 1 FROM research_training_protocol_attachment_cp12 r WHERE r.attachment_hash=a.attachment_hash AND r.protocol_hash IN(SELECT protocol_hash FROM ("+active+")))",null)){while(c.moveToNext())if(activeHashes.add(c.getString(0)))activeBytes+=c.getLong(1);}
        if(activeHashes.add(p.inputHash))activeBytes+=p.bytes;for(TrainingCasCP12.LockedFile f:p.boundFiles)if(activeHashes.add(f.hash))activeBytes+=f.bytes;
        if(activeBytes>(64L<<20))throw new DevelopmentProductionCP12.Wait("TRAIN_INPUT_CAPACITY");
        db.execSQL("INSERT INTO r952_candidate(candidate_id,intent_id,ast,structural_hash,parent_id,state,future_n,created_issue,may_create_real,created_at,semantic_hash) VALUES(?,?,?,?,?,'NURSERY',0,?,0,?,?)",new Object[]{child.candidateId,child.intentId,p.ast.canonical,p.ast.structuralHash,child.parentId,p.registeredIssue,System.currentTimeMillis(),p.ast.semanticHashCP12});
        MatrixBindingsCP12.registerSpecsCP12(db,p.ast);
        insertTrainingInputCP12(db,p);insertTrainingProtocolCP12(db,p);
        String task=ResearchTaskQueueR967.enqueue(db,"TRAIN_CANDIDATE",child.candidateId,p.registeredIssue,p.protocolHash,p.inputHash,child.actionId);
        ResearchLifecycleR932.enrollR952(db,"r952:"+child.candidateId,p.ast.structuralHash,p.registeredIssue);
        if(!child.parentId.isEmpty())ComputationReceiptR974.lineage(db,child.candidateId,child.parentId,"PRIMARY",parentAst,child.expectedActionEvidenceHash,p.registeredIssue);
        if(!child.secondaryParentId.isEmpty())ComputationReceiptR974.lineage(db,child.candidateId,child.secondaryParentId,"FUSION_SECONDARY",secondAst,child.expectedActionEvidenceHash,p.registeredIssue);
        db.execSQL("UPDATE research_action_v2 SET state='WAIT_FUTURE',child_id=?,last_error='' WHERE action_id=? AND state='PROPOSED' AND evidence_hash=?",new Object[]{child.candidateId,child.actionId,child.expectedActionEvidenceHash});
        if(changed(db)!=1)throw new IllegalStateException("REGISTER_ACTION_STATE_CAS");
        funnel(db,p.registeredIssue,child.candidateId,"REGISTERED",p.protocolHash,"TRAIN_TASK="+task+"|FUTURE_N=0");return task;
    }
    private static String readParentCP12(SQLiteDatabase db,String id,String hash,String action) {
        if(id.isEmpty()){if(!hash.isEmpty())throw new IllegalArgumentException("REGISTER_EMPTY_PARENT_HASH");return "";}
        try(Cursor c=db.rawQuery("SELECT ast,structural_hash,state FROM r952_candidate WHERE candidate_id=?",new String[]{id})){
            if(!c.moveToFirst()||!hash.equals(c.getString(1))||"QUALITY_PRUNED".equals(c.getString(2))||"RESOURCE_RETIRED".equals(c.getString(2)))throw new IllegalStateException("REGISTER_PARENT_CAS");
            if("QUARANTINED".equals(c.getString(2))&&!DevelopmentFeatureRepairR78.permitsQuarantinedParent(db,action,id,hash))throw new IllegalStateException("REGISTER_PARENT_CAS");return c.getString(0);}
    }
    private static void insertTrainingInputCP12(SQLiteDatabase db,DevelopmentProductionCP12.PreparedProtocol p) {
        String path=p.inputHash+TrainingCasCP12.INPUT_SUFFIX;
        try(Cursor c=db.rawQuery("SELECT relative_path,byte_count,row_count,first_issue,last_issue,schema_hash,source_manifest_hash FROM research_training_input_v2 WHERE input_hash=?",new String[]{p.inputHash})){
            if(c.moveToFirst()){if(!path.equals(c.getString(0))||p.bytes!=c.getLong(1)||p.rows!=c.getInt(2)||!p.firstIssue.equals(c.getString(3))||!p.lastIssue.equals(c.getString(4))||!V51ChecksumEngine.sha256(p.headerJson).equals(c.getString(5))||!p.sourceManifestHash.equals(c.getString(6)))throw new IllegalStateException("REGISTER_INPUT_DRIFT");return;}
        }
        db.execSQL("INSERT INTO research_training_input_v2(input_hash,relative_path,byte_count,row_count,first_issue,last_issue,schema_hash,source_manifest_hash,created_at) VALUES(?,?,?,?,?,?,?,?,?)",new Object[]{p.inputHash,path,p.bytes,p.rows,p.firstIssue,p.lastIssue,V51ChecksumEngine.sha256(p.headerJson),p.sourceManifestHash,System.currentTimeMillis()});
    }
    private static void insertTrainingProtocolCP12(SQLiteDatabase db,DevelopmentProductionCP12.PreparedProtocol p) {
        java.util.Map<String,Object> json=StrictCanonicalJson.readContextObject(p.protocolJson);
        db.execSQL("INSERT INTO research_training_protocol_v2(protocol_hash,candidate_id,input_hash,ast_hash,feature_manifest_hash,model_kind,train_first,train_last,embargo_issue,validation_first,validation_last,train_n,validation_n,seed,protocol_json,registered_issue,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,120,?,?,?,?)",new Object[]{p.protocolHash,p.candidateId,p.inputHash,p.ast.structuralHash,p.ast.graphSpec.manifest.hash,p.ast.graphSpec.modelKind.name(),json.get("trainFirst"),json.get("trainLast"),json.get("embargoIssue"),json.get("validationFirst"),json.get("validationLast"),p.trainN,p.seed,p.protocolJson,p.registeredIssue,System.currentTimeMillis()});
        for(TrainingCasCP12.LockedFile attachment:p.boundFiles){
            try(Cursor existing=db.rawQuery("SELECT byte_count FROM research_training_attachment_cp12 WHERE attachment_hash=?",new String[]{attachment.hash})){
                if(existing.moveToFirst()){if(existing.getLong(0)!=attachment.bytes)throw new IllegalStateException("REGISTER_ATTACHMENT_DRIFT");}
                else db.execSQL("INSERT INTO research_training_attachment_cp12(attachment_hash,relative_path,byte_count,created_at) VALUES(?,?,?,?)",new Object[]{attachment.hash,attachment.hash+TrainingCasCP12.BOUND_SUFFIX,attachment.bytes,System.currentTimeMillis()});
            }
            db.execSQL("INSERT INTO research_training_protocol_attachment_cp12(protocol_hash,attachment_hash) VALUES(?,?)",new Object[]{p.protocolHash,attachment.hash});
        }
    }

    /** Same admission bound for the cheap producer preflight and authoritative commit. */
    public static boolean trainingQueueHasCapacityR65(SQLiteDatabase db){
        try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM v57_research_task_r967 WHERE kind IN('TRAIN_CANDIDATE','FREEZE_CANDIDATE') AND status NOT IN('COMPLETED','EXPIRED','FAILED_TERMINAL')",null)){
            if(!c.moveToFirst())throw new IllegalStateException("TRAIN_CAPACITY_READ_FAILED");
            return c.getLong(0)<16;
        }
    }
    private static String advanceDirectorSlotCP12(DbStore store,long overallEndNs) {
        final String[] slots={"TRAIN","POST","MATRIX","RESEARCH","EVOLVE","IMPORT"};
        return store.coordinatorForResearchR952().runAtomicWrite("CP12_DIRECTOR_SLOT",db->{
            String value="TRAIN",reserved="1";
            try(Cursor c=db.rawQuery("SELECT state_key,state_value FROM app_state WHERE state_key IN ('r975_director_priority','r77_director_training_turn')",null)){
                while(c.moveToNext())if("r975_director_priority".equals(c.getString(0)))value=c.getString(1);else reserved=c.getString(1);
            }
            int selected=0;for(int i=0;i<slots.length;i++)if(slots[i].equals(value))selected=i;
            // R77: a durable training queue must not receive only one turn inside
            // three nested wheels. Alternate a READY modern-training turn with the
            // original six-owner wheel. The normal cursor does not move on a reserve
            // turn, so POST/MATRIX/RESEARCH/EVOLVE/IMPORT retain bounded service.
            // Only existing tasks qualify: an empty queue cannot consume a reserve.
            boolean trainingReady=ResearchTaskQueueR967.hasReady(db,"TRAIN_CANDIDATE",System.currentTimeMillis())
                    ||ResearchTaskQueueR967.hasReady(db,"FREEZE_CANDIDATE",System.currentTimeMillis());
            // A reservation is an opportunity, not a barrier to a cheaper owner.
            // Preserve it for the next usable slice when POST/MATRIX still fit now.
            boolean trainingTurn="1".equals(reserved)&&trainingReady
                    &&overallEndNs-System.nanoTime()>=350_000_000L
                    &&ResearchWorkBudgetR927.remainingMs()>=350L;
            String slot=trainingTurn?"TRAIN":slots[selected];
            long minimumNs=("TRAIN".equals(slot)||"EVOLVE".equals(slot)
                    ||"RESEARCH".equals(slot)||"IMPORT".equals(slot))?350_000_000L:125_000_000L;
            if(overallEndNs-System.nanoTime()<minimumNs
                    ||ResearchWorkBudgetR927.remainingMs()<minimumNs/1_000_000L)return null;
            ResearchWorkBudgetR927.checkpoint();
            long now=System.currentTimeMillis();
            if(!trainingTurn)db.execSQL("INSERT OR REPLACE INTO app_state(state_key,state_value,updated_at) VALUES('r975_director_priority',?,?)",new Object[]{slots[(selected+1)%slots.length],now});
            if("RESEARCH".equals(slot)){
                String previous="1";try(Cursor c=db.rawQuery("SELECT state_value FROM app_state WHERE state_key='r81_long_research_turn'",null)){if(c.moveToFirst())previous=c.getString(0);}
                if(!"0".equals(previous)&&!"1".equals(previous))throw new IllegalStateException("LONG_RESEARCH_TURN_INVALID");
                db.execSQL("INSERT OR REPLACE INTO app_state(state_key,state_value,updated_at) VALUES('r81_long_research_turn',?,?)",new Object[]{"1".equals(previous)?"0":"1",now});
            }
            db.execSQL("INSERT OR REPLACE INTO app_state(state_key,state_value,updated_at) VALUES('r77_director_training_turn',?,?)",new Object[]{trainingTurn?"0":"1",now});
            db.execSQL("INSERT OR REPLACE INTO app_state(state_key,state_value,updated_at) VALUES('r65_director_last_slot',?,?)",new Object[]{slot,now});
            db.execSQL("INSERT OR REPLACE INTO app_state(state_key,state_value,updated_at) VALUES('r65_director_last_slot_at',?,?)",new Object[]{Long.toString(now),now});
            return slot;
        },null);
    }
    /** A bounded POST safe unit: repair one terminal projection, otherwise consume one POST_TRAIN. */
    private static boolean drainPostSlotCP12(DbStore store,long end) {
        if(System.nanoTime()>=end)return false;String[] row=null;
        try(Cursor c=store.readable().rawQuery("SELECT DISTINCT p.target_issue,t.settlement_hash,t.completed_at FROM (SELECT target_issue,settled,proof_error FROM r952_candidate_predraw UNION ALL SELECT target_issue,settled,proof_error FROM r958_frozen_predraw UNION ALL SELECT target_issue,settled,proof_error FROM "+BoundAtomLifecycleR80.SOURCE+" UNION ALL SELECT target_issue,settled,proof_error FROM "+MultiHorizonProbeR80.SOURCE+") p JOIN "+DbHelper.TABLE_R36_SETTLEMENT_TASK+" t ON t.target_issue=p.target_issue JOIN "+DrawDependencyInputCP12.SOURCE+" d ON d.issue=p.target_issue AND d.dirty=0 AND d.open_time>0 WHERE p.settled=0 AND p.proof_error='' AND t.status='COMPLETED' AND t.completed_at>0 ORDER BY p.target_issue LIMIT 1",null)){if(c.moveToFirst())row=new String[]{c.getString(0),c.getString(1),c.getString(2)};}
        if(row!=null){Models.Draw draw=store.getDrawByIssue(row[0]);if(draw!=null&&draw.isValid()&&draw.dirty==0){onSettlementCompleted(store,draw,row[1],Long.parseLong(row[2]));return true;}}
        return PostSettlementResearchLoopR952.drainOne(store);
    }

    public static boolean failCandidateTaskCP12(SQLiteDatabase db,ResearchTaskQueueR967.Lease lease,String reason) {
        if(db==null||!db.inTransaction()||!ResearchTaskQueueR967.owns(db,lease))return false;
        String evidence=V51ChecksumEngine.sha256("CP12_TRAIN_FAILURE|"+lease.id+'|'+lease.inputHash+'|'+reason);
        String finding=V51ChecksumEngine.sha256("CP12_TASK_FINDING|"+lease.id);
        java.util.Map<String,Object> details=new java.util.TreeMap<>();details.put("taskId",lease.id);details.put("inputHash",lease.inputHash);details.put("protocolHash",lease.sequence);details.put("cursor",lease.cursor);details.put("reason",reason);
        db.execSQL("INSERT OR IGNORE INTO research_finding_v2(finding_id,game_code,candidate_version,source_issue,episode_start_issue,problem_type,severity,evidence_hash,evidence_json,hypothesis,falsification_test,state,created_at,updated_at) VALUES(?,'PK10',?,?,?,'TRAIN_INPUT_INTEGRITY','P0',?,?,?,'REPLAY_IMMUTABLE_INPUT_AND_CHECKPOINT','ERROR',?,?)",new Object[]{finding,lease.candidate,lease.issue,lease.issue,evidence,StrictCanonicalJson.writeContext(details),"Input/protocol/checkpoint identity failed; no numerical result was published",System.currentTimeMillis(),System.currentTimeMillis()});
        if(!ResearchTaskQueueR967.failTerminalCP12(db,lease,reason))throw new IllegalStateException("TRAIN_TERMINAL_LEASE_CHANGED");
        // A dead child cannot leave its originating experiment permanently WAIT_FUTURE.
        db.execSQL("UPDATE research_finding_v2 SET state='ERROR',updated_at=? WHERE finding_id IN(SELECT finding_id FROM research_action_v2 WHERE child_id=? AND state IN('WAIT_FUTURE','CREATED'))",new Object[]{System.currentTimeMillis(),lease.candidate});
        db.execSQL("UPDATE research_action_v2 SET state='REJECTED',last_error=? WHERE child_id=? AND state IN('WAIT_FUTURE','CREATED')",new Object[]{reason,lease.candidate});
        db.execSQL("UPDATE r952_candidate SET state='QUARANTINED' WHERE candidate_id=?",new Object[]{lease.candidate});return true;
    }
}
