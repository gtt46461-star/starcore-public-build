package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;
import org.json.*;

/** One state projection for all three research consumers; the certificate store owns influence. */
public final class ResearchLifecycleR932 {
    public static final String TABLE="research_lifecycle_r932",EVENT="research_lifecycle_event_r932",ELITE="research_elite_r932",CHILD="research_repair_child_r932";
    /** One pulse can advance the complete current algorithm cohort without an unbounded scan. */
    public static final int MAX_REVIEWS_PER_PULSE_R961=24;
    private static final String REVIEWED_PERIOD="r962_lifecycle_reviewed_";
    public enum State {IDEA,NURSERY,OBSERVING,EVIDENCE_READY,CHALLENGER,SHADOW,INFLUENCE,CHAMPION,PAUSED,QUARANTINED,QUALITY_PRUNED,RESOURCE_RETIRED}
    private ResearchLifecycleR932(){}
    static void schema(SQLiteDatabase db){
        if (TransactionSchemaCacheR941.readyCommitted(db,"ResearchLifecycleR932")) return;

        db.execSQL("CREATE TABLE IF NOT EXISTS "+TABLE+"(candidate_key TEXT PRIMARY KEY,structural_hash TEXT NOT NULL,state TEXT NOT NULL,reason TEXT NOT NULL,first_prune_reason TEXT NOT NULL,future_n INTEGER NOT NULL,paired_n INTEGER NOT NULL,evidence_hash TEXT NOT NULL,last_issue TEXT NOT NULL,reviewed_at INTEGER NOT NULL)");
        db.execSQL("CREATE INDEX IF NOT EXISTS idx_r932_lifecycle_review ON "+TABLE+"(reviewed_at,candidate_key)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+EVENT+"(event_hash TEXT PRIMARY KEY,candidate_key TEXT NOT NULL,from_state TEXT NOT NULL,to_state TEXT NOT NULL,reason TEXT NOT NULL,evidence_hash TEXT NOT NULL,at_issue TEXT NOT NULL,created_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+ELITE+"(candidate_key TEXT PRIMARY KEY,structural_hash TEXT NOT NULL,future_n INTEGER NOT NULL,hit_rate REAL NOT NULL,brier REAL NOT NULL,ece REAL NOT NULL,paired_delta REAL NOT NULL,missing_gates TEXT NOT NULL,evidence_hash TEXT NOT NULL,updated_at INTEGER NOT NULL)");
        db.execSQL("CREATE TABLE IF NOT EXISTS "+CHILD+"(child_key TEXT PRIMARY KEY,parent_key TEXT NOT NULL,parent_structure TEXT NOT NULL,child_structure TEXT NOT NULL,repair_target TEXT NOT NULL,changed_modules TEXT NOT NULL,changed_atoms TEXT NOT NULL,expected_effect TEXT NOT NULL,falsification TEXT NOT NULL,kind TEXT NOT NULL,created_issue TEXT NOT NULL,plan_hash TEXT NOT NULL,challenged_id TEXT NOT NULL DEFAULT '',outcome TEXT NOT NULL DEFAULT 'PENDING',created_at INTEGER NOT NULL)");
    
        TransactionSchemaCacheR941.markCommitted(db,"ResearchLifecycleR932");
    }
    static void enroll(SQLiteDatabase db,String key,String structure,String issue,String tier){
        schema(db);
        db.execSQL("INSERT OR IGNORE INTO "+TABLE+" VALUES(?,?,?,'FRESH_OBSERVATION_NO_INHERITED_SUPPORT','',0,0,?,?,0)",new Object[]{key,structure,tier.equals("C")?State.NURSERY.name():State.OBSERVING.name(),V51ChecksumEngine.sha256("R932_ENROLL|"+key+'|'+structure),issue});
        try(Cursor c=db.rawQuery("SELECT structural_hash FROM "+TABLE+" WHERE candidate_key=?",new String[]{key})){if(!c.moveToFirst()||!structure.equals(c.getString(0)))throw new IllegalStateException("R932_LIFECYCLE_STRUCTURE_DRIFT");}
    }
    /** R952 is an extension producer; R932 remains the single lifecycle state owner. */
    public static void enrollR952(SQLiteDatabase db,String key,String structure,String issue){
        if(key==null||!key.startsWith("r952:")||structure==null||structure.isEmpty())throw new IllegalArgumentException("R952_LIFECYCLE_ENROLL_CONTRACT");
        enroll(db,key,structure,issue,"C");
    }
    public static void repairR952(SQLiteDatabase db,String child,String parent,String parentStructure,String childStructure,String target,String modules,String atoms,String expected,String falsification,String issue){
        repairR952(db,child,parent,parentStructure,childStructure,target,modules,atoms,expected,falsification,"PARAMETER_VARIANT",issue);
    }
    public static void repairR952(SQLiteDatabase db,String child,String parent,String parentStructure,String childStructure,String target,String modules,String atoms,String expected,String falsification,String kind,String issue){
        if(!child.startsWith("r952:")||!parent.startsWith("r952:"))throw new IllegalArgumentException("R958_REPAIR_NAMESPACE");
        freshChild(db,child,parent,parentStructure,childStructure,target,modules,atoms,expected,falsification,kind,issue);
    }
    /** Future count alone may leave nursery, but can never mint evidence/certification. */
    public static String observeR952(SQLiteDatabase db,String key,String issue,String evidence,int future){
        if(key==null||!key.startsWith("r952:")||future<0)throw new IllegalArgumentException("R952_LIFECYCLE_OBSERVE_CONTRACT");
        // An online training label cannot overwrite a stronger certificate owner's
        // state, its paired N or its evidence. Only the lifecycle review may demote it.
        try(Cursor c=db.rawQuery("SELECT state FROM "+TABLE+" WHERE candidate_key=?",new String[]{key})) {
            if(c.moveToFirst()) {
                State current=State.valueOf(c.getString(0));
                if(current!=State.IDEA&&current!=State.NURSERY&&current!=State.OBSERVING)
                    return current.name();
            }
        }
        State next=future<3?State.NURSERY:State.OBSERVING;
        String reason=future<3?"WAIT_THREE_DISTINCT_FROZEN_PREDRAW_CHECKS":"WAIT_FIXED_WINDOW_AND_SHARED_CERTIFICATE";
        transition(db,key,next,reason,issue,evidence,future,0);
        return next.name();
    }
    static void freshChild(SQLiteDatabase db,String child,String parent,String parentStructure,String childStructure,String target,String modules,String atoms,String expected,String falsification,String kind,String issue){
        if(!db.inTransaction()||parent==null||parent.isEmpty()||parent.equals(child)||parentStructure.equals(childStructure)||target.isEmpty()||modules.isEmpty()||atoms.isEmpty()||expected.isEmpty()||falsification.isEmpty()||!Arrays.asList("PARAMETER_VARIANT","STRUCTURAL_CHANGE","NEW_FAMILY").contains(kind))throw new IllegalArgumentException("R932_REPAIR_CHILD_CONTRACT");
        schema(db);String plan=V51ChecksumEngine.sha256(child+'|'+parent+'|'+parentStructure+'|'+childStructure+'|'+target+'|'+modules+'|'+atoms+'|'+expected+'|'+falsification+'|'+kind+'|'+issue);
        db.execSQL("INSERT OR IGNORE INTO "+CHILD+"(child_key,parent_key,parent_structure,child_structure,repair_target,changed_modules,changed_atoms,expected_effect,falsification,kind,created_issue,plan_hash,created_at) VALUES(?,?,?,?,?,?,?,?,?,?,?,?,?)",new Object[]{child,parent,parentStructure,childStructure,target,modules,atoms,expected,falsification,kind,issue,plan,System.currentTimeMillis()});
        try(Cursor c=db.rawQuery("SELECT plan_hash FROM "+CHILD+" WHERE child_key=?",new String[]{child})){if(!c.moveToFirst()||!plan.equals(c.getString(0)))throw new IllegalStateException("R932_REPAIR_LINEAGE_CONFLICT");}
        enroll(db,child,childStructure,issue,"C");
    }
    static boolean managed(SQLiteDatabase db,String key){
        try(Cursor t=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{TABLE})){if(!t.moveToFirst())return false;}
        try(Cursor c=db.rawQuery("SELECT 1 FROM "+TABLE+" WHERE candidate_key=?",new String[]{key})){return c.moveToFirst();}
    }
    static String tier(SQLiteDatabase db,String key,String fallback){
        try(Cursor c=db.rawQuery("SELECT state FROM "+TABLE+" WHERE candidate_key=?",new String[]{key})){
            if(!c.moveToFirst())return fallback;State state=State.valueOf(c.getString(0));
            switch(state){case IDEA:case NURSERY:return "C";case INFLUENCE:case CHAMPION:return "A";case QUARANTINED:case QUALITY_PRUNED:case RESOURCE_RETIRED:return "";default:return "B";}
        }
    }
    static void transition(SQLiteDatabase db,String key,State to,String reason,String issue,String evidence,int future,int paired){
        if(!db.inTransaction())throw new IllegalStateException("R932_LIFECYCLE_TRANSACTION_REQUIRED");
        if(to==null||reason==null||reason.isEmpty()||evidence==null||!evidence.matches("[0-9a-f]{64}")||future<0||paired<0||paired>future)throw new IllegalArgumentException("R932_LIFECYCLE_TRANSITION_CONTRACT");
        String old,first;
        try(Cursor c=db.rawQuery("SELECT state,first_prune_reason FROM "+TABLE+" WHERE candidate_key=?",new String[]{key})){if(!c.moveToFirst())throw new IllegalStateException("R932_LIFECYCLE_UNREGISTERED");old=c.getString(0);first=c.getString(1);}
        if((old.equals("QUALITY_PRUNED")||old.equals("QUARANTINED")||old.equals("RESOURCE_RETIRED"))&&!old.equals(to.name()))throw new IllegalStateException("R932_LIFECYCLE_EXPLICIT_AUDIT_RECOVERY_REQUIRED");
        if((to==State.QUALITY_PRUNED||to==State.QUARANTINED||to==State.RESOURCE_RETIRED)&&first.isEmpty())first=reason;
        if(!old.equals(to.name())){
            String event=V51ChecksumEngine.sha256(key+'|'+old+'|'+to+'|'+reason+'|'+issue+'|'+evidence);
            db.execSQL("INSERT OR IGNORE INTO "+EVENT+" VALUES(?,?,?,?,?,?,?,?)",new Object[]{event,key,old,to.name(),reason,evidence,issue,System.currentTimeMillis()});
        }
        db.execSQL("UPDATE "+TABLE+" SET state=?,reason=?,first_prune_reason=?,future_n=?,paired_n=?,evidence_hash=?,last_issue=?,reviewed_at=? WHERE candidate_key=? AND state=?",new Object[]{to.name(),reason,first,future,paired,evidence,issue,System.currentTimeMillis(),key,old});
        try(Cursor changed=db.rawQuery("SELECT changes()",null)){if(!changed.moveToFirst()||changed.getInt(0)!=1)throw new IllegalStateException("R932_LIFECYCLE_STATE_CAS_FAILED");}
        if(to==State.QUARANTINED||to==State.RESOURCE_RETIRED)db.execSQL("DELETE FROM "+ELITE+" WHERE candidate_key=?",new Object[]{key});
        project(db,key,to,reason,future);
    }
    /**
     * Bounded, independently committed reviews replace the former four-candidate group
     * transaction.  A slow/corrupt candidate can no longer roll back already completed reviews,
     * and a normal 20-member algorithm cohort can leave nursery immediately after its third
     * legal predraw proxy rather than waiting several additional draws for the lifecycle cursor.
     */
    public static int maintain(final DbStore store){
        if(RealtimePressureGateR811.hasActiveCriticalWork()||SettlementExecutionGateR819.isActive())return 0;
        final ReviewBatch batch=store.coordinatorR36().runAtomicWrite("R961_LIFECYCLE_SELECT",new DatabaseCoordinator.WriteCallable<ReviewBatch>(){public ReviewBatch call(SQLiteDatabase db){
            schema(db);GlobalResearchScheduleR932.schema(db);FixedWindowCertificatePublisherR932.schema(db);FixedResearchWindowR932.schema(db);
            String latest="";try(Cursor c=db.rawQuery("SELECT issue FROM "+DbHelper.TABLE_DRAWS+" WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)){if(c.moveToFirst())latest=c.getString(0);}
            if(latest.isEmpty())return new ReviewBatch("",Collections.<String>emptyList());
                // R954: certificate publication is a state-change event, not ordinary round-robin
                // housekeeping.  With hundreds of enrolled candidates the former oldest-four-only
                // queue could leave a freshly issued L4/L5 certificate invisible for many pulses,
                // making challenger/shadow appear stuck at zero even though the shared certificate
                // owner had already committed the stage.  Prioritize only certificate rows newer
                // than the candidate's last lifecycle review; after one review reviewed_at catches
                // up, so a stale/invalid ACTIVE row cannot monopolize the queue.  Actual review()
                // still re-validates PIT time, dependency hashes, validity range and revocation.
                LinkedHashSet<String> selected=new LinkedHashSet<String>();
                String prioritySql="SELECT l.candidate_key FROM "+TABLE+" l JOIN "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" c ON "
                        +"c.candidate_id=substr(l.candidate_key,instr(l.candidate_key,':')+1) AND c.structural_hash=l.structural_hash AND c.consumer=CASE "
                        +"WHEN l.candidate_key LIKE 'algorithm:%' THEN 'INVENTED_ALGORITHM' WHEN l.candidate_key LIKE 'genesis:%' THEN 'GENESIS_ASSET' "
                        +"WHEN l.candidate_key LIKE 'formula:%' THEN 'FORMULA_GRAPH' ELSE '' END "
                        +"WHERE c.state='ACTIVE' AND c.updated_at>l.reviewed_at AND l.state NOT IN ('QUALITY_PRUNED','QUARANTINED','RESOURCE_RETIRED') "
                        +"ORDER BY c.updated_at DESC,l.reviewed_at,l.candidate_key LIMIT "+MAX_REVIEWS_PER_PULSE_R961;
                boolean certificateTable=false;
                try(Cursor t=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{ResearchPlatformStoreR929.TABLE_CERTIFICATE})){certificateTable=t.moveToFirst();}
                if(certificateTable){
                    try(Cursor c=db.rawQuery(prioritySql,null)){while(c.moveToNext()&&selected.size()<MAX_REVIEWS_PER_PULSE_R961)selected.add(c.getString(0));}
                }
                // A nursery that has completed its three legal proxy captures must not wait
                // behind every old formula's routine review before it can collect real labels.
                if(selected.size()<MAX_REVIEWS_PER_PULSE_R961){
                    try(Cursor c=db.rawQuery("SELECT l.candidate_key FROM "+TABLE+" l WHERE l.state IN ('IDEA','NURSERY') AND "+notReviewed("l")+" AND (SELECT COUNT(*) FROM (SELECT p.target_issue FROM "+GlobalResearchScheduleR932.PROXY+" p WHERE p.candidate_key=l.candidate_key LIMIT 3))>=3 ORDER BY l.reviewed_at,l.candidate_key LIMIT "+MAX_REVIEWS_PER_PULSE_R961,new String[]{latest})){
                        while(c.moveToNext()&&selected.size()<MAX_REVIEWS_PER_PULSE_R961)selected.add(c.getString(0));
                    }
                }
                if(selected.size()<MAX_REVIEWS_PER_PULSE_R961){
                    try(Cursor c=db.rawQuery("SELECT l.candidate_key FROM "+TABLE+" l WHERE "+notReviewed("l")+" ORDER BY l.reviewed_at,l.candidate_key LIMIT "+(MAX_REVIEWS_PER_PULSE_R961*2),new String[]{latest})){
                        while(c.moveToNext()&&selected.size()<MAX_REVIEWS_PER_PULSE_R961)selected.add(c.getString(0));
                    }
                }
                return new ReviewBatch(latest,new ArrayList<String>(selected));
        }},null);
        if(batch==null)throw new IllegalStateException("R961_LIFECYCLE_SELECTION_COMMIT_FAILED");
        int committed=0,failed=0;
        for(final String key:batch.keys){
            try{
                ResearchWorkBudgetR927.checkpoint();
                Boolean ok=store.coordinatorR36().runAtomicWrite("R961_LIFECYCLE_REVIEW_ONE",new DatabaseCoordinator.WriteCallable<Boolean>(){public Boolean call(SQLiteDatabase db){
                    String active=store.getState(ModelBundleManagerR929.STATE_ACTIVE,"");if(active.isEmpty())active=ModelBundleRuntimeR932.current().hash;
                    try(ModelBundleRuntimeR932.Scope scope=ModelBundleRuntimeR932.forEvidence(active,TargetIssueResolver.next(batch.latest).targetIssue,System.currentTimeMillis())){
                        review(store,db,key,batch.latest);
                        ContentValues receipt=new ContentValues();receipt.put("state_key",REVIEWED_PERIOD+key);
                        receipt.put("state_value",batch.latest);receipt.put("updated_at",System.currentTimeMillis());
                        if(db.insertWithOnConflict(DbHelper.TABLE_APP_STATE,null,receipt,SQLiteDatabase.CONFLICT_REPLACE)<0)
                            throw new IllegalStateException("R962_LIFECYCLE_REVIEW_RECEIPT_FAILED");
                        return Boolean.TRUE;
                    }
                }},Boolean.FALSE);
                if(Boolean.TRUE.equals(ok))committed++;else failed++;
            }catch(ResearchWorkBudgetR927.Yield yielded){break;}
        }
        try{store.setState("r961_lifecycle_progress","selected="+batch.keys.size()+"|committed="+committed+"|failed="+failed+"|per_candidate_atomic=1");}catch(Throwable ignored){}
        return committed;
    }
    private static String notReviewed(String alias){
        return alias+".state NOT IN ('QUALITY_PRUNED','QUARANTINED','RESOURCE_RETIRED') AND NOT EXISTS(SELECT 1 FROM "+DbHelper.TABLE_APP_STATE+" done WHERE done.state_key='"
                +REVIEWED_PERIOD+"'||"+alias+".candidate_key AND done.state_value=?)";
    }
    public static boolean hasPendingPeriodReview(DbStore store){
        String latest=ResearchLearningContinuityR912.latest(store);if(latest.isEmpty())return false;
        try(Cursor c=BudgetedQueryR927.query(store.readable(),"SELECT 1 FROM "+TABLE+" l WHERE "
                +notReviewed("l")+" LIMIT 1",new String[]{latest})){return c.moveToFirst();}
    }
    private static final class ReviewBatch{
        final String latest;final List<String> keys;
        ReviewBatch(String issue,List<String> selected){latest=issue;keys=selected;}
    }
    /** A database-only review cannot claim production influence without the shared authorizer. */
    static void review(SQLiteDatabase db,String key,String latest){review(null,db,key,latest);}
    static void review(DbStore store,SQLiteDatabase db,String key,String latest){
        // The online R952 stream is not an independent, sealed certificate window.
        // Do not erase its real observations by querying the unrelated formula window store.
        if(key.startsWith("r952:")){
            db.execSQL("UPDATE "+TABLE+" SET reviewed_at=?,reason='WAIT_TRAINED_FROZEN_CHILD_IN_SHARED_CERTIFICATE_WINDOWS' WHERE candidate_key=?",
                    new Object[]{System.currentTimeMillis(),key});
            return;
        }
        String old,structure;try(Cursor c=db.rawQuery("SELECT state,structural_hash FROM "+TABLE+" WHERE candidate_key=?",new String[]{key})){if(!c.moveToFirst())return;old=c.getString(0);structure=c.getString(1);}
        if(old.equals("QUARANTINED")||old.equals("QUALITY_PRUNED")||old.equals("RESOURCE_RETIRED")){db.execSQL("UPDATE "+TABLE+" SET reviewed_at=? WHERE candidate_key=?",new Object[]{System.currentTimeMillis(),key});return;}
        int n=0,hits=0,base=0;double brier=0,ece=0;int[] bins=new int[10];double[] ps=new double[10],hs=new double[10];StringBuilder hashes=new StringBuilder(key);
        try(Cursor c=db.rawQuery("SELECT p.target_issue,p.candidate_hit,p.champion_hit,p.selected_hit_p,p.candidate_hash FROM "+FixedResearchWindowR932.WINDOW+" w JOIN "+FixedResearchWindowR932.PAIR+" p ON p.window_id=w.window_id WHERE w.candidate_key=? AND w.window_no>=? AND w.structural_hash=? AND w.champion_hash=? AND w.selection_hash=? AND EXISTS(SELECT 1 FROM "+FixedResearchWindowR932.CONTRACT+" wc WHERE wc.window_id=w.window_id AND wc.contract_hash=?) AND p.candidate_hit IN (0,1) AND p.champion_hit IN (0,1) AND p.selected_hit_p>=0 AND p.selected_hit_p<=1 AND p.target_issue<=? AND p.target_issue>=w.first_issue AND p.target_issue<=w.last_issue ORDER BY p.target_issue DESC LIMIT 1200",new String[]{key,String.valueOf(ObservationGenerationR932.firstSerial(db,key)),structure,ModelBundleRuntimeR932.current().hash,SelectionEvidenceContractR929.installedPolicy().hash,FixedResearchWindowR932.contract(db),latest})){
            while(c.moveToNext()){ResearchWorkBudgetR927.checkpoint();n++;int h=c.getInt(1);hits+=h;base+=c.getInt(2);double p=c.getDouble(3);brier+=(h-p)*(h-p);int bin=Math.min(9,(int)(p*10));bins[bin]++;ps[bin]+=p;hs[bin]+=h;hashes.append('|').append(c.getString(0)).append(':').append(c.getString(4));}
        }
        for(int i=0;i<10;i++)ece+=Math.abs(ps[i]-hs[i])/Math.max(1,n);brier/=Math.max(1,n);
        State next=State.OBSERVING;String reason="WAIT_INDEPENDENT_FUTURE_AND_SHARED_CERTIFICATE";
        int proxy=0;try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM (SELECT target_issue FROM "+GlobalResearchScheduleR932.PROXY+" WHERE candidate_key=? LIMIT 3)",new String[]{key})){if(c.moveToFirst())proxy=c.getInt(0);}
        if(proxy<3)try(Cursor c=db.rawQuery("SELECT COUNT(*) FROM (SELECT target_issue FROM "+ResearchCaptureLedgerR932.TABLE+" WHERE candidate_key=? AND frozen=1 AND full1200=1 LIMIT 3)",new String[]{key})){if(c.moveToFirst())proxy=Math.max(proxy,c.getInt(0));}
        if((old.equals("IDEA")||old.equals("NURSERY"))&&proxy<3){next=State.NURSERY;reason="WAIT_THREE_DISTINCT_FROZEN_PREDRAW_CHECKS";}
        try(Cursor c=db.rawQuery("SELECT state,reason,result_hash FROM "+FixedResearchWindowR932.WINDOW+" WHERE candidate_key=? AND structural_hash=? ORDER BY first_issue DESC,window_no DESC,window_id DESC LIMIT 1",new String[]{key,structure})){
            if(c.moveToFirst()){
                String status=c.getString(0);hashes.append('|').append(c.getString(2));
                if(status.equals("FAIL_EFFECT")){next=State.QUALITY_PRUNED;reason="FIXED_ENDPOINT_QUALITY_FAILURE|"+c.getString(2);}
                else if(status.startsWith("PAUSED")){next=State.PAUSED;reason="SYSTEM_OR_DEPENDENCY_PAUSE|"+c.getString(1);}
                else if(status.startsWith("FAIL_")){next=State.QUARANTINED;reason="FROZEN_CONTRACT_FAILURE|"+c.getString(1);}
                else if(status.equals("PASS")){next=State.EVIDENCE_READY;reason="FIXED_WINDOW_PASS_WAIT_EXECUTION_AUDIT_PUBLICATION";}
            }
        }
        // PAUSED is sticky under review. Only ObservationGenerationR932.recover() may reopen it
        // after a new paid generation is durably registered and old influence is revoked.
        if(old.equals("PAUSED")&&next!=State.QUALITY_PRUNED&&next!=State.QUARANTINED){
            next=State.PAUSED;
            reason="WAIT_EXPLICIT_NEW_OBSERVATION_RECOVERY";
        }
        // R953: L4/L5 are real unified certificate stages even though they intentionally have zero
        // influence weight. The old projection called authorizeCurrent() only; because that method
        // correctly denies SHADOW_ONLY_LEVEL, lifecycle could remain EVIDENCE_READY forever and the
        // visible challenger/shadow count stayed zero despite an ACTIVE certificate. Project the
        // current dependency-valid certificate stage first; only L6/L7 may proceed to weight auth.
        if(store!=null&&next!=State.PAUSED&&next!=State.QUALITY_PRUNED&&next!=State.QUARANTINED){
            String id=key.substring(key.indexOf(':')+1),consumer=key.startsWith("algorithm:")?"INVENTED_ALGORITHM":key.startsWith("genesis:")?"GENESIS_ASSET":"FORMULA_GRAPH";
            CertificateBundleServiceR929.Consumer kind=CertificateBundleServiceR929.Consumer.valueOf(consumer);
            String target=TargetIssueResolver.next(latest).targetIssue;
            ResearchPlatformStoreR929.CertificateStage stage=ResearchPlatformStoreR929.currentStage(store,kind,id,structure,target);
            if(stage.valid){
                hashes.append('|').append(stage.certificateHash);
                if(stage.is("L4_CHALLENGER")){next=State.CHALLENGER;reason="SHARED_CURRENT_CHALLENGER_CERTIFICATE|"+stage.certificateHash;}
                else if(stage.is("L5_SHADOW")){next=State.SHADOW;reason="SHARED_CURRENT_SHADOW_CERTIFICATE|"+stage.certificateHash;}
                else if(stage.is("L6_CONTROLLED_INFLUENCE")||stage.is("L7_CHAMPION")){
                    ResearchPlatformStoreR929.Authorization auth=ResearchPlatformStoreR929.authorizeCurrent(store,kind,id,structure,target,.30);
                    if(auth.authorized&&auth.grantedWeight>0&&!auth.mayCreateReal){
                        next=State.INFLUENCE;reason="SHARED_CURRENT_CERTIFICATE|"+auth.certificateHash;
                        try(Cursor c=db.rawQuery("SELECT family_id FROM "+ResearchPlatformStoreR929.TABLE_CERTIFICATE+" WHERE certificate_hash=?",new String[]{auth.certificateHash})){
                            if(c.moveToFirst()&&key.equals(ResearchCohortR932.champion(db,c.getString(0)))){next=State.CHAMPION;reason="SHARED_CURRENT_CERTIFICATE_AND_FAMILY_INCUMBENT|"+auth.certificateHash;}
                        }
                    }
                }
            }
        }
        String evidence=V51ChecksumEngine.sha256(hashes.toString());
        // Elite is research parent selection only. Gates remain explicit, and no training score
        // can make a parent or child an influence certificate holder.
        if(n>=30&&hits/(double)n>=CertificateBundleServiceR929.MIN_ENTRY_HIT_RATE&&brier<=.24&&ece<=.08&&next!=State.QUARANTINED&&next!=State.QUALITY_PRUNED&&next!=State.PAUSED){
            String missing=(n<360?"FUTURE_N_360;":"")+(hits/(double)n<CertificateBundleServiceR929.MIN_ENTRY_HIT_RATE?"HR_"+CertificateBundleServiceR929.MIN_ENTRY_HIT_RATE+";":"")+(brier>.24?"BRIER_0.24;":"")+(ece>.04?"ECE_0.04;":"")+(next!=State.INFLUENCE&&next!=State.CHAMPION?"INDEPENDENT_WINDOWS_AND_CERTIFICATE;":"");
            db.execSQL("INSERT OR REPLACE INTO "+ELITE+" VALUES(?,?,?,?,?,?,?,?,?,?)",new Object[]{key,structure,n,hits/(double)n,brier,ece,(hits-base)/(double)n,missing,evidence,System.currentTimeMillis()});
        }else db.execSQL("DELETE FROM "+ELITE+" WHERE candidate_key=?",new Object[]{key});
        transition(db,key,next,reason,latest,evidence,n,n);
    }
    static void project(SQLiteDatabase db,String key,State state,String reason,int n){
        String id=key.substring(key.indexOf(':')+1),table,column,statusColumn="status",reasonColumn="lifecycle_reason",status;
        boolean initial=state==State.IDEA||state==State.NURSERY,stop=state==State.QUARANTINED||state==State.QUALITY_PRUNED;
        if(key.startsWith("algorithm:")){table=DbHelper.TABLE_R908_ALGORITHM_GENOME;column="genome_id";status=stop?(state==State.QUALITY_PRUNED?"PRUNED_QUALITY_R932":"QUARANTINED_R932"):initial?"IDEA_WAIT_FUTURE_OOS":"OBSERVING_FUTURE_OOS";}
        else if(key.startsWith("genesis:")){table=AssetGenesisEngineR915.TABLE_ASSET_GENOME;column="asset_id";status=stop?(state==State.QUALITY_PRUNED?"PRUNED_QUALITY_R932":"QUARANTINED_R932"):initial?"IDEA_WAIT_FUTURE_OOS":"OBSERVING_FUTURE_OOS";}
        else if(key.startsWith("formula:")){table=DbHelper.TABLE_R906_RESEARCH_ASSET;column="asset_id";statusColumn="tier";status=stop?"ARCHIVED":initial?"NURSERY":"ACTIVE";reasonColumn="";}
        else return;
        try(Cursor c=db.rawQuery("SELECT 1 FROM sqlite_master WHERE type='table' AND name=?",new String[]{table})){if(!c.moveToFirst())return;}
        if(state==State.RESOURCE_RETIRED)status="PRUNED_RESOURCE_R966";
        ContentValues values=new ContentValues();values.put(statusColumn,status);if(!reasonColumn.isEmpty())values.put(reasonColumn,reason);
        db.update(table,values,column+"=?",new String[]{id});
    }
    public static String report(DbStore store){
        StringBuilder b=new StringBuilder("\n统一研究生命周期\n");
        try(Cursor c=store.readable().rawQuery("SELECT state,COUNT(*) FROM "+TABLE+" GROUP BY state",null)){while(c.moveToNext())b.append(c.getString(0)).append(' ').append(c.getInt(1)).append('\n');}
        try(Cursor c=store.readable().rawQuery("SELECT COUNT(*),SUM(CASE WHEN outcome='PASSED' THEN 1 ELSE 0 END),SUM(CASE WHEN outcome='FAILED' THEN 1 ELSE 0 END) FROM "+CHILD,null)){if(c.moveToFirst()){int attempts=c.getInt(0),passed=c.isNull(1)?0:c.getInt(1),failed=c.isNull(2)?0:c.getInt(2);b.append("修复 attempted=").append(attempts).append("，matured=").append(passed+failed).append("，passed=").append(passed).append("，pending=").append(attempts-passed-failed).append('\n');}}
        return b.append("新子代 Future N 从0开始；系统缺口暂停；修复通过只取独立未来配对端点，不取创建数量。\n").toString();
    }

public static String reportSnapshotCP4(com.ethk3.novaonepro.ReportReadContextCP4 report, com.ethk3.novaonepro.ReportReadContextCP4 store){
        com.ethk3.novaonepro.ReportTextCP12 b=new com.ethk3.novaonepro.ReportTextCP12(report, "\n统一研究生命周期\n");
        try(Cursor c=report.select("SELECT state,COUNT(*) FROM "+TABLE+" GROUP BY state",null)){while(c.moveToNext())b.append(c.getString(0)).append(' ').append(c.getInt(1)).append('\n');}
        try(Cursor c=report.select("SELECT COUNT(*),SUM(CASE WHEN outcome='PASSED' THEN 1 ELSE 0 END),SUM(CASE WHEN outcome='FAILED' THEN 1 ELSE 0 END) FROM "+CHILD,null)){if(c.moveToFirst()){int attempts=c.getInt(0),passed=c.isNull(1)?0:c.getInt(1),failed=c.isNull(2)?0:c.getInt(2);b.append("修复 attempted=").append(attempts).append("，matured=").append(passed+failed).append("，passed=").append(passed).append("，pending=").append(attempts-passed-failed).append('\n');}}
        return b.append("新子代 Future N 从0开始；系统缺口暂停；修复通过只取独立未来配对端点，不取创建数量。\n").toString();
    }
}
