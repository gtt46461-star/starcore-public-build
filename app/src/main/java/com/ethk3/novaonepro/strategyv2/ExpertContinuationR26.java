package com.ethk3.novaonepro.strategyv2;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.*;
import java.io.*;
import java.util.function.Supplier;
import org.json.*;

/** A new source epoch for an authenticated learned private pair. Old metrics never
 * become the new epoch's samples; no experimental pair is a publication authority. */
public final class ExpertContinuationR26 {
    public static final String WAITING="WAITING_EXPERT_CONTINUATION",SOURCE="EXPERT_CONTINUATION_SOURCE";
    private ExpertContinuationR26(){}
    public static void preserveExpired(SQLiteDatabase db){
        if(!db.inTransaction())throw new IllegalStateException("R26_EXPERT_CONTINUATION_WRITER");
        String learned="length(state)>0 AND (cursor>0 OR checkpoint_hash<>'')",expired="NOT EXISTS(SELECT 1 FROM r26_foundry f WHERE f.campaign_id=r26_expert_experiment.campaign_id AND length(f.snapshot)>0 AND f.status IN ('RUNNING','DONE','"+SOURCE+"'))";
        db.execSQL("UPDATE r26_expert_experiment SET status=? WHERE "+learned+" AND "+expired+" AND numerical_abi=? AND status IN ('RUNNING','DONE','EXPIRED_SOURCE_RETENTION')",new Object[]{WAITING,ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256});
        db.execSQL("UPDATE r26_expert_experiment SET status='BLOCKED_EXPERT_ABI_PRESERVED' WHERE "+learned+" AND numerical_abi<>? AND status NOT IN ('CONTINUED_WEIGHTS_TRANSFERRED','BLOCKED_EXPERT_ABI_PRESERVED')",new Object[]{ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256});
    }
    private static final class Parent {String id,campaign,stateHash,checkpoint,parent,proposal,abi;byte[] reference;int cursor;long trained;}
    private static Parent parent(SQLiteDatabase db,String expert){try(Cursor c=db.rawQuery("SELECT experiment_id,campaign_id,state,state_hash,checkpoint_hash,parent_hash,cursor,proposal_json,numerical_abi FROM r26_expert_experiment WHERE expert_id=? AND status=? AND numerical_abi=? ORDER BY created_at,experiment_id LIMIT 1",new String[]{expert,WAITING,ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256})){if(!c.moveToFirst())return null;Parent p=new Parent();p.id=c.getString(0);p.campaign=c.getString(1);p.reference=c.getBlob(2);p.stateHash=c.getString(3);p.checkpoint=c.getString(4);p.parent=c.getString(5);p.cursor=c.getInt(6);p.proposal=c.getString(7);p.abi=c.getString(8);return p;}}
    /** -1: no waiting pair; 0: retained/waiting/blocked; 1: one complete fenced transfer. */
    public static long step(DbStore store,Supplier<RuntimeCoordinatorR26.Claim> claims,BudgetTokenSU1 budget,String expert)throws IOException,JSONException{
        Parent p=parent(ExpertWorkshopR26.snapshotReader(store),expert);if(p==null)return -1;budget.checkpointOrYield();ExpertExperimentR26.Pair prior,child;FoundryCampaignR26.Snapshot source;byte[] snapshot,checkpoint;String sourceHash;
        try{
            if(!HashingSU1.fields("R26_EXPERT_CHECKPOINT_V1",p.id,p.parent,Integer.toString(p.cursor),p.stateHash).equals(p.checkpoint))throw new IOException("R26_EXPERT_WARM_CHECKPOINT");
            try(Cursor manifest=ExpertWorkshopR26.snapshotReader(store).rawQuery("SELECT artifact_hash,policy_hash,source_hash,parent_hash,trained_through FROM r26_checkpoint_artifact WHERE checkpoint_hash=?",new String[]{p.checkpoint})){
                if(!manifest.moveToFirst()||!CheckpointArtifactsR26.referenceHash(p.reference).equals(manifest.getString(0))||!p.id.equals(manifest.getString(1))||!p.campaign.equals(manifest.getString(2))||!p.parent.equals(manifest.getString(3)))throw new IOException("R26_EXPERT_WARM_MANIFEST");p.trained=manifest.getLong(4);
            }
            byte[] state=CheckpointArtifactsR26.resolve(store,p.reference);if(!LiveStateR26.hash(state).equals(p.stateHash))throw new IOException("R26_EXPERT_WARM_STATE_HASH");prior=ExpertExperimentR26.Pair.decode(state);if(!prior.spec.id.equals(expert)||prior.cursor!=p.cursor)throw new IOException("R26_EXPERT_WARM_OWNER_OR_CURSOR");
            source=WarmCandidateR26.retainedSuffix(FoundryCampaignR26.capture(store),Math.max(prior.exposureThrough(),p.trained));if(source==null){claims.get().reason("EXPERT_WAITING_64_RETAINED_LABELS_AFTER_ALL_EXPOSURE; BOTH_LEARNED_MODELS_PRESERVED");return 0;}
            snapshot=source.encode();sourceHash=LiveStateR26.hash(snapshot);child=ExpertExperimentR26.Pair.continueAfter(prior,p.checkpoint,p.stateHash,sourceHash,source.ordinal[0],source.through,p.trained,prior.exposureThrough());checkpoint=child.encode();
        }catch(IOException|IllegalArgumentException invalid){
            store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_EXPERT_WARM_BLOCKED",db->{RuntimeCoordinatorR26.requireFence(db,claims.get());db.execSQL("UPDATE r26_expert_experiment SET status='BLOCKED_EXPERT_STATE_PRESERVED' WHERE experiment_id=? AND checkpoint_hash=? AND status=?",new Object[]{p.id,p.checkpoint,WAITING});return 0;});claims.get().reason("EXPERT_WARM_STATE_PRESERVED:"+invalid.getClass().getSimpleName()+":"+String.valueOf(invalid.getMessage()));return 0;
        }
        String campaign=HashingSU1.fields("R26_EXPERT_WARM_SOURCE_V1",sourceHash,p.abi),id=HashingSU1.fields("R26_EXPERT_WARM_EXPERIMENT_V1",expert,p.id,p.checkpoint,sourceHash),hash=LiveStateR26.hash(checkpoint),seed=HashingSU1.fields("R26_EXPERT_CHECKPOINT_V1",id,p.checkpoint,"0",hash);
        JSONObject proposal=new JSONObject(p.proposal).put("experimentId",id).put("sourceHash",sourceHash).put("result","VERIFIED_LEARNED_PAIR_NEW_SOURCE_EPOCH").put("continuationParent",p.id).put("continuationCheckpoint",p.checkpoint).put("continuationParentStateHash",p.stateHash).put("retainedModelSamples",child.retainedModelSamples).put("parentExposureThrough",child.parentExposedThrough).put("newSourceFirst",source.ordinal[0]).put("inheritPairedMetrics",false).put("inheritQualification",false);
        budget.checkpointOrYield();try(CheckpointArtifactsR26.Prepared file=CheckpointArtifactsR26.prepareLease(store,checkpoint)){
            return store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_EXPERT_WARM_TRANSFER",db->{RuntimeCoordinatorR26.requireFence(db,claims.get());
                try(Cursor latest=db.rawQuery("SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)){if(!latest.moveToFirst()||TargetIssueResolver.sequenceOrdinalSU1(latest.getString(0))-source.ordinal[0]>=5000)throw new IllegalStateException("R26_EXPERT_WARM_SOURCE_EXPIRED");}
                try(Cursor current=db.rawQuery("SELECT 1 FROM r26_expert_experiment WHERE experiment_id=? AND checkpoint_hash=? AND state_hash=? AND cursor=? AND status=? AND numerical_abi=?",new String[]{p.id,p.checkpoint,p.stateHash,Integer.toString(p.cursor),WAITING,p.abi})){if(!current.moveToFirst())throw new IllegalStateException("R26_EXPERT_WARM_PARENT_CAS");}
                long now=System.currentTimeMillis();db.execSQL("INSERT OR IGNORE INTO r26_foundry(campaign_id,snapshot_hash,source_through,snapshot,status,created_at,numerical_abi,source_first) VALUES(?,?,?,?,?,?,?,?)",new Object[]{campaign,sourceHash,source.through,snapshot,SOURCE,now,p.abi,source.ordinal[0]});
                try(Cursor fixed=db.rawQuery("SELECT snapshot_hash,source_first,source_through,status FROM r26_foundry WHERE campaign_id=?",new String[]{campaign})){if(!fixed.moveToFirst()||!sourceHash.equals(fixed.getString(0))||fixed.getLong(1)!=source.ordinal[0]||fixed.getLong(2)!=source.through||!SOURCE.equals(fixed.getString(3)))throw new IllegalStateException("R26_EXPERT_WARM_SOURCE_CONFLICT");}
                db.execSQL("INSERT INTO r26_expert_experiment(experiment_id,expert_id,campaign_id,cursor,state,state_hash,checkpoint_hash,parent_hash,numerical_abi,proposal_json,created_at) VALUES(?,?,?,0,?,?,?,?,?,?,?)",new Object[]{id,expert,campaign,file.reference(),hash,seed,p.checkpoint,p.abi,proposal.toString(),now});CheckpointManifestR26.record(db,seed,id,campaign,p.checkpoint,p.trained,file.reference());
                db.execSQL("UPDATE r26_expert_experiment SET status='CONTINUED_WEIGHTS_TRANSFERRED',state=X'',parent_hash='' WHERE experiment_id=? AND checkpoint_hash=? AND status=?",new Object[]{p.id,p.checkpoint,WAITING});
                try(Cursor changed=db.rawQuery("SELECT changes()",null)){if(!changed.moveToFirst()||changed.getInt(0)!=1)throw new IllegalStateException("R26_EXPERT_WARM_TRANSFER_CAS");}return 1L;
            });
        }
    }
}
