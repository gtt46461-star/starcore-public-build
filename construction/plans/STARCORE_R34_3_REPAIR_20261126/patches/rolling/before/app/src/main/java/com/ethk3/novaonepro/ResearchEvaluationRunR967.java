package com.ethk3.novaonepro;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.r952.CandidateAstR952;
import com.ethk3.novaonepro.r952.CandidateCompilerR952;
import com.ethk3.novaonepro.r952.FeatureManifestR952;
import java.util.ArrayList;

/** Prospective, disjoint training/evaluation streams and immutable multi-version models.
 * V3 learners train on every new issue. Immutable evaluation models remain separate objects;
 * no evaluation result is copied into a learner. V2 parity schedules remain immutable.
 * Prior observations/metrics are not reassigned and no REAL authority is owned here.
 */
public final class ResearchEvaluationRunR967 {
    public static final String RUN="r967_numeric_training_run", INPUT="r967_numeric_training_input";
    public static final String EVAL="r967_frozen_evaluation_v2", ALL="r967_frozen_candidate_all";
    public static final String ROLE="TRAIN_STREAM_V2", SPLIT="EVERY_ISSUE_SEPARATE_FROZEN_EVALUATION_V3";
    public static final String PREFIX="R958-F-V3-", LEGACY_PREFIX="R958-F-V2-", LEGACY_SPLIT="ODD_TRAIN_EVEN_EVALUATION_V1";
    public static final int FREEZE_EVERY=120, MAX_ACTIVE_PER_LEARNER=2, MAX_ACTIVE_GLOBAL=32;
    private ResearchEvaluationRunR967(){}
    public static boolean trainingIssue(String issue){
        TargetIssueResolver.ParsedIssue parsed=TargetIssueResolver.parse(issue);
        if(parsed==null)throw new IllegalArgumentException("R969_STREAM_ISSUE");
        return (parsed.sequence&1)==1; // 1440 is even: the role stays stable across a UTC day.
    }
    public static boolean versioned(String key){return key!=null&&(key.startsWith(PREFIX)||key.startsWith("algorithm:"+PREFIX)||key.startsWith(LEGACY_PREFIX)||key.startsWith("algorithm:"+LEGACY_PREFIX));}
    public static boolean legacyVersion(String key){return key!=null&&(key.startsWith(LEGACY_PREFIX)||key.startsWith("algorithm:"+LEGACY_PREFIX));}
    public static String frozenPrefix(String key){return legacyVersion(key)?LEGACY_PREFIX:PREFIX;}
    public static boolean eligibleEvaluation(String key,String target){
        if(!TargetIssueResolver.isValid(target))return false;
        return !legacyVersion(key)||!trainingIssue(target);
    }
    public static boolean trainable(Run run,String target){return run!=null&&TargetIssueResolver.isValid(target)&&(SPLIT.equals(run.split)||LEGACY_SPLIT.equals(run.split)&&trainingIssue(target));}
    public static String nextEvaluation(String key,String issue){
        String next=TargetIssueResolver.next(issue).targetIssue;
        if(legacyVersion(key)&&trainingIssue(next))next=TargetIssueResolver.next(next).targetIssue;
        if(!TargetIssueResolver.isValid(next))throw new IllegalArgumentException("R969_EVAL_ENDPOINT");
        return next;
    }
    public static String endpoint(String key,String first,int samples){
        if(samples<1||samples>10000||!eligibleEvaluation(key,first))throw new IllegalArgumentException("R969_EVAL_SCHEDULE");
        String last=first;for(int i=1;i<samples;i++)last=nextEvaluation(key,last);return last;
    }
    public static void schema(SQLiteDatabase db){
        if(TransactionSchemaCacheR941.ready(db,"R969_STREAM_V2"))return;
        db.execSQL("CREATE TABLE IF NOT EXISTS "+RUN+"(run_hash TEXT PRIMARY KEY,learner_id TEXT NOT NULL,first_issue TEXT NOT NULL,ast_hash TEXT NOT NULL,rule_hash TEXT NOT NULL,initial_payload TEXT NOT NULL,initial_updates INTEGER NOT NULL,source_artifact TEXT NOT NULL,split_rule TEXT NOT NULL,integrity_epoch TEXT NOT NULL,created_at INTEGER NOT NULL,UNIQUE(learner_id,first_issue))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r969_run_learner ON "+RUN+"(learner_id,first_issue)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+INPUT+"(learner_id TEXT NOT NULL,target_issue TEXT NOT NULL,run_hash TEXT NOT NULL,input_hash TEXT NOT NULL,model_hash TEXT NOT NULL,record_hash TEXT NOT NULL,PRIMARY KEY(learner_id,target_issue))");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+EVAL+"(candidate_id TEXT PRIMARY KEY,learner_id TEXT NOT NULL,ast TEXT NOT NULL,model_payload TEXT NOT NULL,structural_hash TEXT NOT NULL UNIQUE,model_hash TEXT NOT NULL,trained_until_issue TEXT NOT NULL,created_issue TEXT NOT NULL,source_artifact TEXT NOT NULL,created_at INTEGER NOT NULL,run_hash TEXT NOT NULL,rule_hash TEXT NOT NULL,integrity_epoch TEXT NOT NULL,update_endpoint INTEGER NOT NULL,UNIQUE(run_hash,update_endpoint))");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r969_eval_learner ON "+EVAL+"(learner_id,created_issue)");
        String cols="candidate_id,learner_id,ast,model_payload,structural_hash,model_hash,trained_until_issue,created_issue,source_artifact,created_at";
        db.execSQL("CREATE VIEW IF NOT EXISTS "+ALL+" AS SELECT "+cols+",'' AS run_hash,'' AS rule_hash,'' AS integrity_epoch,0 AS update_endpoint FROM r958_frozen_candidate UNION ALL SELECT "+cols+",run_hash,rule_hash,integrity_epoch,update_endpoint FROM "+EVAL);
        for(String table:new String[]{RUN,INPUT,EVAL}){
            db.execSQL("CREATE TRIGGER IF NOT EXISTS "+table+"_no_update BEFORE UPDATE ON "+table+" BEGIN SELECT RAISE(ABORT,'R969_STREAM_RECORD_IMMUTABLE'); END");
            db.execSQL("CREATE TRIGGER IF NOT EXISTS "+table+"_no_delete BEFORE DELETE ON "+table+" BEGIN SELECT RAISE(ABORT,'R969_STREAM_PROVENANCE_PERMANENT'); END");
        }
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r969_run_no_replace BEFORE INSERT ON "+RUN+" WHEN EXISTS(SELECT 1 FROM "+RUN+" WHERE run_hash=NEW.run_hash OR (learner_id=NEW.learner_id AND first_issue=NEW.first_issue)) BEGIN SELECT RAISE(ABORT,'R969_RUN_DUPLICATE'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r969_input_no_replace BEFORE INSERT ON "+INPUT+" WHEN EXISTS(SELECT 1 FROM "+INPUT+" WHERE learner_id=NEW.learner_id AND target_issue=NEW.target_issue) BEGIN SELECT RAISE(ABORT,'R969_INPUT_DUPLICATE'); END");
        db.execSQL("CREATE TRIGGER IF NOT EXISTS r969_eval_no_replace BEFORE INSERT ON "+EVAL+" WHEN EXISTS(SELECT 1 FROM "+EVAL+" WHERE candidate_id=NEW.candidate_id OR structural_hash=NEW.structural_hash OR (run_hash=NEW.run_hash AND update_endpoint=NEW.update_endpoint)) BEGIN SELECT RAISE(ABORT,'R969_EVAL_DUPLICATE'); END");
        TransactionSchemaCacheR941.mark(db,"R969_STREAM_V2");
    }
    public static final class Run {
        public final String hash,learner,first,ast,rule,initial,artifact,split,epoch;
        public final long initialUpdates;
        Run(Cursor c){
            hash=c.getString(0);learner=c.getString(1);first=c.getString(2);ast=c.getString(3);rule=c.getString(4);initial=c.getString(5);initialUpdates=c.getLong(6);artifact=c.getString(7);split=c.getString(8);epoch=c.getString(9);
            if(!epoch.equals(originHash(learner,first,ast,rule,initial,artifact))||(!SPLIT.equals(split)&&!LEGACY_SPLIT.equals(split))||!TargetIssueResolver.isValid(first)||CandidateCompilerR952.Model.parse(initial).updates!=initialUpdates||!hash.equals(runHash(learner,first,ast,rule,initial,artifact,split,epoch)))throw new IllegalStateException("R969_RUN_IDENTITY_DRIFT");
        }
    }
    private static String originHash(String learner,String first,String ast,String rule,String initial,String artifact){
        // R952 owns no R948 algorithm audit row. Bind this origin to actual immutable
        // warm-start provenance, not AlgorithmProofRecovery.epoch("r952:"), which is NOT_ALGORITHM.
        return new ResearchSemanticsR967.Builder("R952_NUMERIC_RUN_ORIGIN_V1").text("learner",learner).text("first",first).hash("ast",ast).hash("rule",rule).hash("initial",V51ChecksumEngine.sha256(initial)).text("artifact",artifact).digest();
    }
    private static String runHash(String learner,String first,String ast,String rule,String initial,String artifact,String split,String epoch){
        return new ResearchSemanticsR967.Builder("R967_NUMERIC_RUN_V2").text("learner",learner).text("first",first).hash("ast",ast).hash("rule",rule).hash("initial",V51ChecksumEngine.sha256(initial)).text("artifact",artifact).text("split",split).hash("epoch",epoch).digest();
    }
    public static Run current(SQLiteDatabase db,String learner,String target){
        try(Cursor c=db.rawQuery("SELECT run_hash,learner_id,first_issue,ast_hash,rule_hash,initial_payload,initial_updates,source_artifact,split_rule,integrity_epoch FROM "+RUN+" WHERE learner_id=? AND first_issue<=? ORDER BY first_issue DESC LIMIT 1",new String[]{learner,target})){return c.moveToFirst()?new Run(c):null;}
    }
    /** Enrollment is explicit and prospective; no pending old event is re-labelled. */
    public static int enroll(final DbStore store,final String target){
        if(store==null||!TargetIssueResolver.isValid(target))throw new IllegalArgumentException("R969_RUN_TARGET");
        final ArrayList<String> ids=new ArrayList<String>();
        try(Cursor c=store.readable().rawQuery("SELECT c.candidate_id FROM r952_candidate c WHERE c.ast NOT LIKE 'AST3:%' AND c.state NOT IN ('QUARANTINED','QUALITY_PRUNED','RESOURCE_RETIRED') AND NOT EXISTS(SELECT 1 FROM "+RUN+" r WHERE r.learner_id=c.candidate_id AND "+TrainingContinuationR973.predicate("r.rule_hash")+" AND r.first_issue=(SELECT MAX(r2.first_issue) FROM "+RUN+" r2 WHERE r2.learner_id=c.candidate_id AND r2.first_issue<=?)) AND (EXISTS(SELECT 1 FROM "+RUN+" old WHERE old.learner_id=c.candidate_id AND old.first_issue=(SELECT MAX(last.first_issue) FROM "+RUN+" last WHERE last.learner_id=c.candidate_id AND last.first_issue<=?) AND NOT "+TrainingContinuationR973.predicate("old.rule_hash")+") OR (NOT EXISTS(SELECT 1 FROM r952_research_outbox o WHERE o.candidate_id=c.candidate_id AND o.state NOT IN ('COMPLETED','DEPENDENCY_RETIRED')) AND NOT EXISTS(SELECT 1 FROM r952_candidate_predraw p WHERE p.candidate_id=c.candidate_id AND p.settled=0 AND p.proof_error=''))) ORDER BY c.created_at,c.candidate_id LIMIT 16",new String[]{target,target})){while(c.moveToNext())ids.add(c.getString(0));}
        if(ids.isEmpty())return 0;
        Integer n=store.coordinatorR36().runAtomicWrite("R969_PREREGISTER_DISJOINT_TRAIN_RUN",new DatabaseCoordinator.WriteCallable<Integer>(){public Integer call(SQLiteDatabase db){
            if(drawn(db,target))throw new IllegalStateException("R969_RUN_POSTDRAW");int count=0;
            for(String id:ids){
                Run prior=current(db,id,target);if(prior!=null&&TrainingContinuationR973.compatible(prior.rule))continue;
                if(prior==null){try(Cursor history=db.rawQuery("SELECT 1 FROM "+INPUT+" WHERE learner_id=? UNION ALL SELECT 1 FROM "+EVAL+" WHERE learner_id=? UNION ALL SELECT 1 FROM r952_candidate_predraw WHERE candidate_id=? AND evidence_role=? LIMIT 1",new String[]{id,id,id,ROLE})){if(history.moveToFirst())throw new IllegalStateException("R969_RUN_HISTORY_MISSING_NO_AUTOREPAIR");}}
                else if(prior.first.equals(target))throw new IllegalStateException("R969_CANNOT_REPLACE_REGISTERED_RUN");
                if(prior!=null)retireUnconsumedPriorRule(db,prior);
                try(Cursor pending=db.rawQuery("SELECT 1 FROM r952_research_outbox WHERE candidate_id=? AND state NOT IN ('COMPLETED','DEPENDENCY_RETIRED') UNION ALL SELECT 1 FROM r952_candidate_predraw WHERE candidate_id=? AND settled=0 AND proof_error='' LIMIT 1",new String[]{id,id})){if(pending.moveToFirst())continue;}
                String ast="";try(Cursor c=db.rawQuery("SELECT ast,structural_hash FROM r952_candidate WHERE candidate_id=? AND state NOT IN ('QUARANTINED','QUALITY_PRUNED','RESOURCE_RETIRED')",new String[]{id})){if(!c.moveToFirst())continue;CandidateAstR952 a=CandidateAstR952.parse(c.getString(0));if(!a.structuralHash.equals(c.getString(1)))throw new IllegalStateException("R969_RUN_AST_DRIFT");ast=a.structuralHash;}
                CandidateCompilerR952.Model model=CandidateCompilerR952.Model.initial();String artifact="";
                try(Cursor c=db.rawQuery("SELECT l.trained_until_issue,a.model_payload,a.payload_hash,a.artifact_hash FROM r952_learning_cursor l JOIN r952_artifact a ON a.artifact_hash=l.artifact_hash AND a.candidate_id=? WHERE l.consumer_id=?",new String[]{id,"R952:"+id})){if(c.moveToFirst()){if(c.getString(0).compareTo(target)>=0)throw new IllegalStateException("R969_RUN_MODEL_NOT_PIT");model=CandidateCompilerR952.Model.parse(c.getString(1));if(!model.hash().equals(c.getString(2)))throw new IllegalStateException("R969_RUN_MODEL_HASH");artifact=c.getString(3);}}
                model=model.upgradeForTraining();
                String epoch=originHash(id,target,ast,TrainingCodeIdentityR964.SHA256,model.payload(),artifact);
                String hash=runHash(id,target,ast,TrainingCodeIdentityR964.SHA256,model.payload(),artifact,SPLIT,epoch);
                db.execSQL("INSERT INTO "+RUN+" VALUES(?,?,?,?,?,?,?,?,?,?,?)",new Object[]{hash,id,target,ast,TrainingCodeIdentityR964.SHA256,model.payload(),model.updates,artifact,SPLIT,epoch,System.currentTimeMillis()});
                // Any unfinished legacy window keeps its original denominator and paid alpha.
                db.execSQL("UPDATE "+FixedResearchWindowR932.WINDOW+" SET state='PAUSED_DEPENDENCY',reason='R969_PRIOR_RUN_RETIRED_AT_PROSPECTIVE_STREAM_MIGRATION' WHERE state='OPEN' AND candidate_key IN (SELECT 'algorithm:'||candidate_id FROM "+ALL+" WHERE learner_id=?)",new Object[]{id});count++;
            }return count;
        }},null);
        if(n==null)throw new IllegalStateException("R969_RUN_REGISTRATION_FAILED");return n;
    }
    private static void retireUnconsumedPriorRule(SQLiteDatabase db,Run old){
        if(TrainingContinuationR973.compatible(old.rule))throw new IllegalStateException("R969_CANNOT_RETIRE_CURRENT_RULE");
        // Only rows tied to a verified old run are retired. Missing provenance remains blocked.
        ArrayList<String[]> pending=new ArrayList<String[]>();
        try(Cursor c=db.rawQuery("SELECT o.delivery_hash,o.attempt_count,o.target_issue,i.input_hash,p.model_hash FROM r952_research_outbox o JOIN "+INPUT+" i ON i.learner_id=o.candidate_id AND i.target_issue=o.target_issue JOIN r952_candidate_predraw p ON p.candidate_id=o.candidate_id AND p.target_issue=o.target_issue WHERE i.run_hash=? AND p.evidence_role=? AND o.state NOT IN ('COMPLETED','DEPENDENCY_RETIRED') ORDER BY o.target_issue,o.delivery_hash LIMIT 32",new String[]{old.hash,ROLE})){
            while(c.moveToNext())pending.add(new String[]{c.getString(0),c.getString(1),c.getString(2),c.getString(3),c.getString(4)});
        }
        // Close the Android cursor before modifying its indexed predicate; retain bounded work.
        for(String[] row:pending){
            ResearchWorkBudgetR927.checkpoint();
            verifyRecordedInput(db,old.learner,row[2],row[3],row[4]);
            String reason="R969_PRIOR_NUMERIC_RULE_RETIRED|"+old.hash;
            db.execSQL("UPDATE r952_research_outbox SET state='DEPENDENCY_RETIRED',lease_until=0,last_error=?,updated_at=? WHERE delivery_hash=? AND state NOT IN ('COMPLETED','DEPENDENCY_RETIRED')",new Object[]{reason,System.currentTimeMillis(),row[0]});
            com.ethk3.novaonepro.r952.RuntimeRecoveryJournalR952.record(db,row[0],Integer.parseInt(row[1]),"DEPENDENCY_RETIRED",reason);
        }
        db.execSQL("UPDATE r952_candidate_predraw SET proof_error='R969_PRIOR_RULE_CAPTURE_RETIRED_WITHOUT_TRAINING' WHERE candidate_id=? AND settled=0 AND proof_error='' AND evidence_role=? AND target_issue IN (SELECT target_issue FROM "+INPUT+" WHERE learner_id=? AND run_hash=?)",new Object[]{old.learner,ROLE,old.learner,old.hash});
    }
    public static Run requireTrain(SQLiteDatabase db,String learner,String target){
        Run run=current(db,learner,target);
        if(!trainable(run,target))throw new IllegalStateException("R969_SEALED_EVALUATION_LABEL_CANNOT_TRAIN");
        if(run==null||!TrainingContinuationR973.compatible(run.rule)||!parentCompatible(db,learner,run.ast))throw new IllegalStateException("R969_TRAIN_RUN_INCOMPATIBLE");
        return run;
    }
    public static void bindInput(SQLiteDatabase db,String learner,String target,String input,String model){
        if(!db.inTransaction())throw new IllegalStateException("R969_INPUT_TRANSACTION");Run r=requireTrain(db,learner,target);
        String hash=inputHash(r.hash,learner,target,input,model);
        try(Cursor c=db.rawQuery("SELECT record_hash FROM "+INPUT+" WHERE learner_id=? AND target_issue=?",new String[]{learner,target})){if(c.moveToFirst()){if(!hash.equals(c.getString(0)))throw new IllegalStateException("R969_INPUT_REPLAY_DRIFT");return;}}
        db.execSQL("INSERT INTO "+INPUT+" VALUES(?,?,?,?,?,?)",new Object[]{learner,target,r.hash,input,model,hash});
    }
    public static void verifyInput(SQLiteDatabase db,String learner,String target,String input,String model){
        requireTrain(db,learner,target);
        verifyRecordedInput(db,learner,target,input,model);
    }
    /** Historical completed feedback uses its immutable original run, never retrains old labels. */
    public static void verifyRecordedInput(SQLiteDatabase db,String learner,String target,String input,String model){
        Run r=current(db,learner,target);
        if(r!=null&&!trainable(r,target))throw new IllegalStateException("R969_SEALED_EVALUATION_LABEL_CANNOT_FEEDBACK");
        if(r==null)throw new IllegalStateException("R969_RECORDED_RUN_MISSING");
        try(Cursor c=db.rawQuery("SELECT run_hash,input_hash,model_hash,record_hash FROM "+INPUT+" WHERE learner_id=? AND target_issue=?",new String[]{learner,target})){
            if(!c.moveToFirst()||!r.hash.equals(c.getString(0))||!input.equals(c.getString(1))||!model.equals(c.getString(2))||!inputHash(r.hash,learner,target,input,c.getString(2)).equals(c.getString(3)))throw new IllegalStateException("R969_TRAIN_INPUT_SIDECAR_MISSING_OR_CHANGED");
        }
    }
    private static String inputHash(String run,String learner,String target,String input,String model){return new ResearchSemanticsR967.Builder("R967_TRAIN_INPUT_V2").hash("run",run).text("learner",learner).text("target",target).hash("input",input).hash("model",model).digest();}
    public static String evaluationHash(CandidateAstR952 ast,CandidateCompilerR952.Model model,String rule,String epoch){
        return ResearchSemanticsR967.frozenEvaluationHash(ast.structuralHash,model.hash(),FeatureManifestR952.forAst(ast).hash,rule,V51ChecksumEngine.sha256("FIXED_AST_POSITION_FULL1200_SCORE_DESC_ORDINAL_ASC_V1"),epoch);
    }
    /** Only the FREEZE task owner calls this method, inside its existing writer transaction. */
    static String registerDevelopmentCP8(SQLiteDatabase db,FrozenCandidateAdapterR958.PreparedFreezeCP8 p){
        p.requireOpen();
        if(!db.inTransaction()||p.ast.formatVersion!=3||p.model.isLegacy()||drawn(db,p.target))
            throw new IllegalStateException("FREEZE_REGISTRATION_GUARD");
        try(Cursor c=db.rawQuery("SELECT candidate_id,model_hash,ast,trained_until_issue FROM "+EVAL
                +" WHERE learner_id=? AND source_artifact=?",new String[]{p.candidateId,p.artifactHash})){
            if(c.moveToFirst()){
                if(!p.modelHash.equals(c.getString(1))||!p.ast.canonical.equals(c.getString(2))||!p.validationLast.equals(c.getString(3)))
                    throw new IllegalStateException("FREEZE_ARTIFACT_REPLAY_CONFLICT");
                String existing=c.getString(0);
                if(c.moveToNext())throw new IllegalStateException("FREEZE_DUPLICATE_ARTIFACT");
                return existing;
            }
        }
        String epoch=originHash(p.candidateId,p.target,p.astHash,p.ruleHash,p.modelPayload,p.artifactHash);
        String run=runHash(p.candidateId,p.target,p.astHash,p.ruleHash,p.modelPayload,p.artifactHash,SPLIT,epoch);
        try(Cursor c=db.rawQuery("SELECT run_hash FROM "+RUN+" WHERE learner_id=? AND first_issue=?",new String[]{p.candidateId,p.target})){
            if(c.moveToFirst())throw new IllegalStateException("FREEZE_RUN_ALREADY_PRESENT_WITHOUT_EVAL");
        }
        long now=System.currentTimeMillis();
        db.execSQL("INSERT INTO "+RUN+"(run_hash,learner_id,first_issue,ast_hash,rule_hash,initial_payload,initial_updates,source_artifact,split_rule,integrity_epoch,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?)",
            new Object[]{run,p.candidateId,p.target,p.astHash,p.ruleHash,p.modelPayload,p.trainN,p.artifactHash,SPLIT,epoch,now});
        String structure=evaluationHash(p.ast,p.model,p.ruleHash,epoch);
        String id=PREFIX+structure.substring(0,24);
        db.execSQL("INSERT INTO "+EVAL+"(candidate_id,learner_id,ast,model_payload,structural_hash,model_hash,trained_until_issue,created_issue,source_artifact,created_at,run_hash,rule_hash,integrity_epoch,update_endpoint) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?,0)",
            new Object[]{id,p.candidateId,p.ast.canonical,p.modelPayload,structure,p.modelHash,p.validationLast,p.target,p.artifactHash,now,run,p.ruleHash,epoch});
        ResearchLifecycleR932.enroll(db,"algorithm:"+id,structure,p.target,"C");
        ResearchLifecycleR932.transition(db,"algorithm:"+id,ResearchLifecycleR932.State.NURSERY,
            "FROZEN_WAIT_CAPTURE_CAPACITY",p.target,p.artifactHash,0,0);
        return id;
    }

    public static boolean mayCapture(SQLiteDatabase db,FrozenCandidateAdapterR958.Frozen f,String target){
        if(f==null||!com.ethk3.novaonepro.r952.BoundAtomLifecycleR80.frozenDependencyReasonR80(db,f.structure,target).isEmpty())return false;
        if(!com.ethk3.novaonepro.r952.LongSummaryBindingsR80.frozenDependencyReasonR81(db,f.structure,target).isEmpty())return false;
        if(versioned(f.id)){
            if(!eligibleEvaluation(f.id,target))return false;
            Run run=current(db,f.learner,target);
            return run!=null&&f.run.equals(run.hash)&&TrainingContinuationR973.compatible(f.rule)&&f.epoch.equals(run.epoch)&&run.ast.equals(f.ast.structuralHash)&&parentCompatible(db,f.learner,run.ast);
        }
        return current(db,f.learner,target)==null;
    }
    private static boolean parentCompatible(SQLiteDatabase db,String learner,String ast){
        try(Cursor c=db.rawQuery("SELECT structural_hash,state FROM r952_candidate WHERE candidate_id=?",new String[]{learner})){
            return c.moveToFirst()&&ast.equals(c.getString(0))&&!"QUARANTINED".equals(c.getString(1))&&!"QUALITY_PRUNED".equals(c.getString(1))&&!"RESOURCE_RETIRED".equals(c.getString(1));
        }
    }
    /** A static certificate is usable only in the predeclared domain it evaluated. */
    public static boolean consumerCompatible(SQLiteDatabase db,String key,String target){
        if(!versioned(key))return true;
        String id=key.startsWith("algorithm:")?key.substring(10):key;
        FrozenCandidateAdapterR958.Frozen f=FrozenCandidateAdapterR958.load(db,id);
        return f!=null&&mayCapture(db,f,target);
    }
    public static boolean drawn(SQLiteDatabase db,String target){try(Cursor c=db.rawQuery("SELECT 1 FROM "+DbHelper.TABLE_DRAWS+" WHERE issue>=? LIMIT 1",new String[]{target})){return c.moveToFirst();}}
}
