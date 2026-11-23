package com.ethk3.novaonepro.strategyv2;

import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import com.ethk3.novaonepro.DbStore;
import com.ethk3.novaonepro.ResearchSemanticsGeneratedR967;
import com.ethk3.novaonepro.TargetIssueResolver;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Set;
import java.util.function.Supplier;
import org.json.JSONObject;

/** R34 B06: consumes old-ABI preserved research rows one at a time.
 * Original old-ABI parents are unchanged; weights/metrics/certificates are never inherited.
 * Expired zero-trained children may change lifecycle status only after a fresh successor commits.
 * New candidates are trained by FoundryCampaignR26; new pairs by ExpertWorkshopR26.
 * No DDL, model construction or serialization occurs in the writer transaction. */
public final class ColdRebirthR34 {
    public static final String TASK = "cold-rebirth-r34";
    private ColdRebirthR34() {}
    private static final class Parent {
        String kind,campaign,id,text,expert,stateHash,checkpoint,abi,status,key;
        long cursor,exposure; boolean expiredUntrained;
    }
    private static final class Prepared {
        Parent parent; String targetAbi,campaign,child,snapshotHash,sourceStatus,proposal,initialHash;
        byte[] snapshot,initial; long generation,first,through,now;
        PolicyIdentityLedgerR26.Prepared identity;
        String text,structure;
    }
    private static long scalar(SQLiteDatabase db,String sql,String... args) {
        try (Cursor c=db.rawQuery(sql,args.length==0?null:args)) { return c.moveToFirst()&&!c.isNull(0)?c.getLong(0):0; }
    }
    private static boolean done(SQLiteDatabase db,Parent p,String abi) {
        return scalar(db,"SELECT count(*) FROM r34_cold_rebirth WHERE source_kind=? AND source_campaign=? AND source_id=? AND target_abi=?",p.kind,p.campaign,p.id,abi)>0;
    }
    private static String birthColumn(String kind,String id,String column) {
        return "(SELECT born."+column+" FROM r34_cold_rebirth born WHERE born.source_kind='"+kind+"' AND born.child_campaign=p.campaign_id AND born.child_id="+id+" LIMIT 1)";
    }
    private static String ownerAbi(String kind,String id) {
        return kind.equals("CANDIDATE")?"coalesce(f.numerical_abi,"+birthColumn(kind,id,"target_abi")+")":"p.numerical_abi";
    }
    private static String recoverZero(String kind) {
        String zero=kind.equals("CANDIDATE")?ColdRebirthContractR34.protectZeroCandidateSql("p"):ColdRebirthContractR34.protectZeroExpertSql("p");
        return "("+zero+" AND p.status<>'COLD_UNTRAINED_SUPERSEDED' AND (f.campaign_id IS NULL OR length(f.snapshot)=0 OR f.status IN ('EXPIRED_RETENTION','ABANDONED_NUMERICAL_ABI') OR "+ownerAbi(kind,kind.equals("CANDIDATE")?"p.policy_hash":"p.experiment_id")+"<>?))";
    }
    private static Parent pick(SQLiteDatabase db,String kind,String abi,long now) {
        String table=kind.equals("CANDIDATE")?"r26_candidate":"r26_expert_experiment";
        String id=kind.equals("CANDIDATE")?"p.policy_hash":"p.experiment_id";
        String wanted=kind.equals("CANDIDATE")?"BLOCKED_WARM_NUMERICAL_ABI_PRESERVED":"BLOCKED_EXPERT_ABI_PRESERVED";
        String zero=kind.equals("CANDIDATE")?ColdRebirthContractR34.protectZeroCandidateSql("p"):ColdRebirthContractR34.protectZeroExpertSql("p");
        String columns=kind.equals("CANDIDATE")?"p.campaign_id,p.policy_hash,p.canonical_text,'',":"p.campaign_id,p.experiment_id,'',p.expert_id,";
        columns+="p.state_hash,p.checkpoint_hash,p.cursor,"+ownerAbi(kind,id)+",coalesce(f.source_through,"+birthColumn(kind,id,"source_through")+",-1),p.status,"+zero;
        String sql="SELECT "+columns+" FROM "+table+" p LEFT JOIN r26_foundry f ON f.campaign_id=p.campaign_id WHERE ((p.status=? AND "+ownerAbi(kind,id)+"<>?) OR "+recoverZero(kind)+")"
            +" AND NOT EXISTS(SELECT 1 FROM r34_cold_rebirth b WHERE b.source_kind=? AND b.source_campaign=p.campaign_id AND b.source_id="+id+" AND b.target_abi=?)"
            +" AND NOT EXISTS(SELECT 1 FROM r34_cold_rebirth_retry b WHERE b.source_kind=? AND b.source_campaign=p.campaign_id AND b.source_id="+id+" AND b.target_abi=? AND b.next_at>?)"
            +" ORDER BY p.campaign_id,"+id+" LIMIT 1";
        try(Cursor c=db.rawQuery(sql,new String[]{wanted,abi,abi,kind,abi,kind,abi,Long.toString(now)})) {
            if(!c.moveToFirst())return null;Parent p=new Parent();p.kind=kind;p.campaign=c.getString(0);p.id=c.getString(1);
            p.text=c.getString(2);p.expert=c.getString(3);p.stateHash=c.getString(4);p.checkpoint=c.getString(5);
            p.cursor=c.getLong(6);p.abi=c.getString(7);p.exposure=c.getLong(8);p.status=c.getString(9);p.expiredUntrained=c.getInt(10)==1;
            p.key=ColdRebirthContractR34.sourceKey(kind,p.campaign,p.id);return p;
        }
    }
    /** Current primary-row queue, not heartbeat progress. Null means schema unavailable. */
    public static Long pendingParentsForHealth(SQLiteDatabase db) {
        if(scalar(db,"SELECT count(*) FROM sqlite_master WHERE type='table' AND name IN ('r34_cold_rebirth','r26_candidate','r26_expert_experiment','r26_foundry')")!=4)return null;
        String abi=ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256;long total=0;
        for(String kind:new String[]{"CANDIDATE","EXPERT"}) {
            String table=kind.equals("CANDIDATE")?"r26_candidate":"r26_expert_experiment",id=kind.equals("CANDIDATE")?"p.policy_hash":"p.experiment_id";
            String status=kind.equals("CANDIDATE")?"BLOCKED_WARM_NUMERICAL_ABI_PRESERVED":"BLOCKED_EXPERT_ABI_PRESERVED";
            total+=scalar(db,"SELECT count(*) FROM "+table+" p LEFT JOIN r26_foundry f ON f.campaign_id=p.campaign_id WHERE ((p.status=? AND "+ownerAbi(kind,id)+"<>?) OR "+recoverZero(kind)+") AND NOT EXISTS(SELECT 1 FROM r34_cold_rebirth b WHERE b.source_kind=? AND b.source_campaign=p.campaign_id AND b.source_id="+id+" AND b.target_abi=?)",status,abi,abi,kind,abi);
        }
        return total;
    }
    private static long pending(SQLiteDatabase db) {
        String abi=ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256;
        // An expired zero child needs a successor; counting it as runnable would
        // deadlock recovery when all 16 admitted snapshots expire together.
        return scalar(db,"SELECT count(*) FROM r34_cold_rebirth b JOIN r26_foundry f ON f.campaign_id=b.child_campaign WHERE f.numerical_abi=? AND length(f.snapshot)>0 AND (EXISTS(SELECT 1 FROM r26_candidate c WHERE b.source_kind='CANDIDATE' AND c.campaign_id=b.child_campaign AND c.policy_hash=b.child_id AND c.status='READY' AND f.status='RUNNING') OR EXISTS(SELECT 1 FROM r26_expert_experiment e WHERE b.source_kind='EXPERT' AND e.experiment_id=b.child_id AND e.status='RUNNING' AND e.numerical_abi=? AND f.status IN ('RUNNING','DONE','EXPERT_CONTINUATION_SOURCE')))",abi,abi);
    }
    /** Negative claim: old originals are preserved, not decoded, restored, or verified by this operation. */
    private static ModelGenomeSU2 coldGenome(Parent p,String abi,String hash,long through) {
        ModelGenomeSU2 old=ModelGenomeSU2.parse(p.text);List<ModelGenomeSU2.Member> members=new ArrayList<>();
        for(ModelGenomeSU2.Member member:old.members) {
            java.util.SortedMap<String,String> parameters=ColdRebirthContractR34.coldParameters(member.params,p.key,abi);
            if(member.kind.equals("R26F23")&&!ColdRebirthContractR34.stripColdParameters(parameters).equals(FamilyRegistryR26.legacyBinding()))
                throw new IllegalArgumentException("F23_ORIGINAL_TEACHER_BINDING_NOT_AVAILABLE");
            members.add(new ModelGenomeSU2.Member(member.kind,parameters,member.weight));
        }
        ModelGenomeSU2 next=new ModelGenomeSU2(members,old.pool,old.sink,old.gate,old.coverage,old.metaC,old.calibrated).bindBirth(hash,through);
        if(WarmModelSeedR26.has(next)||next.key().equals(p.id))throw new IllegalArgumentException("R34_REBIRTH_NOT_FRESH");
        return next;
    }
    private static void closePair(ExpertExperimentR26.Pair pair) {
        if(pair==null)return;Set<OnlineModelsSU2.Model> seen=Collections.newSetFromMap(new IdentityHashMap<>());
        seen.addAll(pair.left.hub.models.values());seen.addAll(pair.right.hub.models.values());
        for(OnlineModelsSU2.Model model:seen)FamilyRegistryR26.closeR31(model);
    }
    private static Prepared prepare(DbStore store,Parent parent,String abi,BudgetTokenSU1 budget)throws Exception {
        Prepared p=new Prepared();p.parent=parent;p.targetAbi=abi;p.now=System.currentTimeMillis();
        if(parent.exposure<0)throw new IllegalArgumentException("R34_COLD_PARENT_SOURCE_EXPOSURE_MISSING");
        p.generation=EventFrameBuilderSU1.generation(store.readable());budget.checkpointOrYield();
        FoundryCampaignR26.Snapshot source=FoundryCampaignR26.capture(store);if(source==null)return null;
        p.snapshot=source.encode();p.first=source.ordinal[0];p.through=source.through;
        ColdRebirthContractR34.requireSnapshot(source.draws.length,p.snapshot.length,p.first,p.through,parent.exposure);
        p.snapshotHash=LiveStateR26.hash(p.snapshot);p.campaign=ColdRebirthContractR34.campaign(parent.key,abi,p.snapshotHash);
        if(parent.kind.equals("CANDIDATE")) {
            ModelGenomeSU2 next=coldGenome(parent,abi,p.snapshotHash,p.through);p.child=next.key();p.text=next.text();
            p.identity=PolicyIdentityLedgerR26.prepare(p.text,p.child);p.structure=StructuralProposalR26.structure(next);p.sourceStatus="RUNNING";
        } else {
            ExpertExperimentR26.Spec spec=ExpertExperimentR26.spec(parent.expert);ExpertExperimentR26.Pair pair=null;
            // Validate both structural dependencies before allocating either learner.
            ExpertExperimentR26.genome(spec.left);ExpertExperimentR26.genome(spec.right);
            try {
                pair=new ExpertExperimentR26.Pair(spec,p.snapshotHash,p.through);
                ColdRebirthContractR34.requireFreshCounts(pair.cursor,pair.left.samples()+pair.right.samples(),0,0,0);
                p.initial=pair.encode();p.initialHash=LiveStateR26.hash(p.initial);
            } finally {closePair(pair);}
            p.child=HashingSU1.fields("R26_EXPERT_EXPERIMENT_V1",parent.expert,p.campaign);p.sourceStatus=ExpertContinuationR26.SOURCE;
            p.proposal=new JSONObject().put("failureCluster",spec.cluster).put("hypothesis",spec.hypothesis)
                .put("typedDiff",new JSONObject(spec.typedDiff())).put("falsifier",spec.falsifier).put("ablation",spec.ablation)
                .put("experimentId",p.child).put("sourceHash",p.snapshotHash).put("result",ColdRebirthContractR34.PROTOCOL)
                .put("coldParent",parent.id).put("oldAbi",parent.abi).put("newAbi",abi)
                .put("restartKind",parent.expiredUntrained?"EXPIRED_UNTRAINED_NEW_SOURCE":"OLD_ABI_FRESH_STATE")
                .put("inheritedWeights",false).put("inheritedEvidence",false).put("automaticPromotion",false)
                .put("nextTrigger","EXISTING_EXPERT_WORKSHOP_ACTUAL_PAIRED_TRAINING").toString();
        }
        budget.checkpointOrYield();
        if(EventFrameBuilderSU1.generation(store.readable())!=p.generation)
            throw new ModelPublicationR31.PublicationDependencyR31("R34_COLD_SOURCE_CHANGED_DURING_PREPARE");
        return p;
    }
    private static void requireParent(SQLiteDatabase db,Parent p) {
        String table=p.kind.equals("CANDIDATE")?"r26_candidate":"r26_expert_experiment",id=p.kind.equals("CANDIDATE")?"p.policy_hash":"p.experiment_id";
        String zero=p.expiredUntrained?" AND "+(p.kind.equals("CANDIDATE")?ColdRebirthContractR34.protectZeroCandidateSql("p"):ColdRebirthContractR34.protectZeroExpertSql("p")):"";
        String sql="SELECT 1 FROM "+table+" p LEFT JOIN r26_foundry f ON f.campaign_id=p.campaign_id WHERE p.campaign_id=? AND "+id+"=? AND p.state_hash=? AND p.checkpoint_hash=? AND p.cursor=? AND p.status=? AND "+ownerAbi(p.kind,id)+"=? AND "+(p.kind.equals("CANDIDATE")?"p.canonical_text":"p.expert_id")+"=? AND coalesce(f.source_through,"+birthColumn(p.kind,id,"source_through")+",-1)=?"+zero;
        try(Cursor c=db.rawQuery(sql,new String[]{p.campaign,p.id,p.stateHash,p.checkpoint,Long.toString(p.cursor),p.status,p.abi,p.kind.equals("CANDIDATE")?p.text:p.expert,Long.toString(p.exposure)})) {
            if(!c.moveToFirst())throw new ModelPublicationR31.PublicationDependencyR31("R34_COLD_PARENT_CHANGED");
        }
    }
    private static long publish(SQLiteDatabase db,Prepared p,byte[] initialReference,RuntimeCoordinatorR26.Claim claim) {
        if(!db.inTransaction())throw new IllegalStateException("R34_COLD_WRITER_REQUIRED");
        if(done(db,p.parent,p.targetAbi))return 0;
        if(!p.targetAbi.equals(ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256))throw new IllegalStateException("R34_COLD_ABI_CHANGED");
        requireParent(db,p.parent);
        if(pending(db)>=ColdRebirthContractR34.MAX_PENDING)throw new ModelPublicationR31.PublicationDependencyR31("R34_COLD_PENDING_CAPACITY");
        if(EventFrameBuilderSU1.generation(db)!=p.generation)throw new ModelPublicationR31.PublicationDependencyR31("R34_COLD_SOURCE_CHANGED_BEFORE_COMMIT");
        try(Cursor c=db.rawQuery("SELECT issue FROM draws WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)) {
            if(!c.moveToFirst()||TargetIssueResolver.sequenceOrdinalSU1(c.getString(0))-p.first>=5000)
                throw new ModelPublicationR31.PublicationDependencyR31("R34_COLD_SOURCE_EXPIRED_BEFORE_COMMIT");
        }
        db.execSQL(ColdRebirthContractR34.INSERT_SOURCE,new Object[]{p.campaign,p.snapshotHash,p.through,p.snapshot,p.sourceStatus,p.now,p.targetAbi,p.first});
        if(p.parent.kind.equals("CANDIDATE")) {
            if(!PolicyIdentityLedgerR26.recordPrepared(db,p.identity))throw new IllegalStateException("R34_COLD_IDENTITY_NOT_RECORDED");
            db.execSQL(ColdRebirthContractR34.INSERT_CANDIDATE,new Object[]{p.campaign,p.child,p.text,p.parent.id,ColdRebirthContractR34.PROTOCOL});
            db.execSQL("INSERT INTO r26_evolution(attempt_id,campaign_id,policy_hash,parent,operation,category,structure_hash) VALUES(?,?,?,?,?,?,?)",
                new Object[]{p.campaign+":COLD",p.campaign,p.child,p.parent.id,ColdRebirthContractR34.PROTOCOL,p.parent.expiredUntrained?"UNTRAINED_SOURCE_RESTART_NOT_QUALITY_IMPROVEMENT":"ABI_MIGRATION_NOT_QUALITY_IMPROVEMENT",p.structure});
            try(Cursor c=db.rawQuery("SELECT cursor,dev_n,dev_k,val_n,val_k,coalesce(length(state),0),length(checkpoint_hash) FROM r26_candidate WHERE campaign_id=? AND policy_hash=?",new String[]{p.campaign,p.child})) {
                if(!c.moveToFirst())throw new IllegalStateException("R34_COLD_CHILD_MISSING");
                for(int i=0;i<7;i++)if(c.getLong(i)!=0)throw new IllegalStateException("R34_COLD_CANDIDATE_NOT_ZERO");
            }
        } else {
            if(initialReference==null)throw new IllegalStateException("R34_COLD_EXPERT_ARTIFACT_MISSING");
            db.execSQL(ColdRebirthContractR34.INSERT_EXPERT,new Object[]{p.child,p.parent.expert,p.campaign,initialReference,p.initialHash,p.targetAbi,p.proposal,p.now});
        }
        Parent old=p.parent;
        db.execSQL(ColdRebirthContractR34.INSERT_RECEIPT,new Object[]{old.kind,old.campaign,old.id,old.key,old.stateHash,old.checkpoint,
            old.cursor,old.abi,p.targetAbi,p.campaign,p.child,p.snapshotHash,p.first,p.through,ColdRebirthContractR34.PROTOCOL,p.now});
        db.execSQL("DELETE FROM r34_cold_rebirth_retry WHERE source_kind=? AND source_campaign=? AND source_id=? AND target_abi=?",
            new Object[]{old.kind,old.campaign,old.id,p.targetAbi});
        if(old.expiredUntrained) {
            String table=old.kind.equals("CANDIDATE")?"r26_candidate":"r26_expert_experiment",id=old.kind.equals("CANDIDATE")?"policy_hash":"experiment_id";
            db.execSQL("UPDATE "+table+" SET status='COLD_UNTRAINED_SUPERSEDED' WHERE campaign_id=? AND "+id+"=? AND cursor=0 AND checkpoint_hash='' AND status=?",new Object[]{old.campaign,old.id,old.status});
            if(scalar(db,"SELECT changes()")!=1)throw new IllegalStateException("R34_COLD_SUPERSEDE_CAS");
        }
        long births=scalar(db,"SELECT count(*) FROM r34_cold_rebirth");Long waiting=pendingParentsForHealth(db);
        if(waiting==null)throw new IllegalStateException("R34_COLD_PENDING_SCHEMA");
        ResearchStoreR30R1.commitProgress(db,TASK,claim.owner,claim.fence,ResearchStoreR30R1.calendar(db),
            new ResearchStoreR30R1.Facts(births,births+waiting,waiting,null,null,
                ColdRebirthContractR34.hash(ColdRebirthContractR34.PROTOCOL,old.key,p.targetAbi,p.campaign,p.child,p.snapshotHash)));
        return 1;
    }
    private static void retry(DbStore store,Supplier<RuntimeCoordinatorR26.Claim> claims,Parent p,String abi,Exception failure) {
        String detail=failure.getClass().getSimpleName()+":"+String.valueOf(failure.getMessage());
        final String reason=detail.substring(0,Math.min(512,detail.length()));
        store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_R34_COLD_REBIRTH_RETRY",db->{
            RuntimeCoordinatorR26.requireFence(db,claims.get());if(done(db,p,abi))return 0;requireParent(db,p);
            long count=scalar(db,"SELECT attempts FROM r34_cold_rebirth_retry WHERE source_kind=? AND source_campaign=? AND source_id=? AND target_abi=?",p.kind,p.campaign,p.id,abi);
            int attempts=(int)Math.min(1000000,count+1);long now=System.currentTimeMillis();
            db.execSQL("INSERT OR REPLACE INTO r34_cold_rebirth_retry VALUES(?,?,?,?,?,?,?,?)",
                new Object[]{p.kind,p.campaign,p.id,abi,attempts,now+ColdRebirthContractR34.retryDelay(attempts),reason,now});
            RuntimeCoordinatorR26.requireFence(db,claims.get());return 0;
        });
        claims.get().reason("R34_COLD_ONE_PARENT_WAIT:"+p.key+":"+reason);
    }
    public static long step(DbStore store,Supplier<RuntimeCoordinatorR26.Claim> claims,BudgetTokenSU1 budget)throws Exception {
        // Use the original data-write admission, before any snapshot/model IO.
        // A waiting birth is never relabelled as CONTROL to bypass backpressure.
        if(WriterHoldFaultsR31.deferResearch() && !com.ethk3.novaonepro.DatabaseCoordinator.researchDataDueR335())
            throw new ModelPublicationR31.PublicationDependencyR31("R33_WAIT_WRITER_HOLD_FAULT_BACKLOG");
        SQLiteDatabase db=store.readable();ColdRebirthSchemaR34.require(db);
        if(!ResearchStoreR30R1.ready(db))throw new com.ethk3.novaonepro.DatabaseCoordinator.SchemaPendingR31("R34_COLD_REBIRTH","TYPED_PROGRESS_OWNER");
        if(pending(db)>=ColdRebirthContractR34.MAX_PENDING){claims.get().reason("R34_COLD_WAIT_ACTUAL_TRAINING_CAPACITY");return 0;}
        String abi=ResearchSemanticsGeneratedR967.TRAINING_RULE_SHA256,first="CANDIDATE";
        try(Cursor c=db.rawQuery("SELECT source_kind FROM r34_cold_rebirth ORDER BY created_at DESC,source_key DESC LIMIT 1",null)) {
            if(c.moveToFirst()&&"CANDIDATE".equals(c.getString(0)))first="EXPERT";
        }
        long now=System.currentTimeMillis();Parent parent=pick(db,first,abi,now);
        if(parent==null)parent=pick(db,first.equals("CANDIDATE")?"EXPERT":"CANDIDATE",abi,now);
        if(parent==null){claims.get().reason("R34_COLD_NO_DUE_PRESERVED_PARENT");return 0;}
        // Expert encode can hold the bounded 8 MiB result and two temporary copies.
        long workspaceBytes=parent.kind.equals("EXPERT")?32L<<20:2L<<20;
        MemoryBudgetR31.Admission admission=MemoryBudgetR31.processBudget().tryAcquire(TASK,MemoryBudgetR31.Kind.CHECKPOINT,workspaceBytes,()->Thread.currentThread().isInterrupted());
        if(!admission.acquired())throw new MemoryBudgetR31.CapacityWait(admission.reason);
        try {
            Prepared p;
            try {p=prepare(store,parent,abi,budget);}
            catch(IOException|IllegalArgumentException failure){retry(store,claims,parent,abi,failure);return 0;}
            if(p==null){claims.get().reason("R34_COLD_WAIT_64_RETAINED_LABELS");return 0;}
            long changed;
            if(p.initial==null)changed=store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_R34_COLD_REBIRTH",tx->{RuntimeCoordinatorR26.requireFence(tx,claims.get());long n=publish(tx,p,null,claims.get());RuntimeCoordinatorR26.requireFence(tx,claims.get());return n;});
            else try(CheckpointArtifactsR26.Prepared file=CheckpointArtifactsR26.prepareLease(store,p.initial)) {
                changed=store.coordinatorForResearchR952().runAtomicWriteOrThrowF01("R26_R34_COLD_REBIRTH",tx->{RuntimeCoordinatorR26.requireFence(tx,claims.get());long n=publish(tx,p,file.reference(),claims.get());RuntimeCoordinatorR26.requireFence(tx,claims.get());return n;});
            }
            claims.get().reason(changed>0?"R34_COLD_CHILD_REGISTERED_ACTUAL_TRAINING_PENDING":"R34_COLD_ALREADY_COMMITTED");return changed;
        } finally {admission.lease.close();}
    }
}
