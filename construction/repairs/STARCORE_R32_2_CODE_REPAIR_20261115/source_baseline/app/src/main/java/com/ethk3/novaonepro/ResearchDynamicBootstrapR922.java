package com.ethk3.novaonepro;

import android.content.ContentValues;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.util.*;
import org.json.*;

/** Resumable development training on the existing research worker. Never grants REAL or Future evidence. */
public final class ResearchDynamicBootstrapR922 {
    public static final String VERSION="R979_BOUNDED_FOCUSED_DEVELOPMENT_V6";
    public static final boolean MAY_AUTHORIZE_REAL=false;
    private static final String JOB="r959_development_job_", INPUT="r959_development_input_";
    private static final int HISTORY=512, TRAIN=240;
    private ResearchDynamicBootstrapR922() {}

    /** Unfinished historical development stays runnable within the SAME draw.
     * A four-label slice is progress, not completion of a 240-label training job.
     * Imported formulas are legitimate inputs even without an old CREATED receipt.
     */
    private static String eligibleDevelopment() {
        return "g.dev_n=0 AND g.status NOT LIKE 'PRUNED%' AND g.status NOT LIKE 'QUARANTINED%'"
                +" AND g.status NOT IN ('RETIRED','ARCHIVED')"
                +" AND NOT EXISTS(SELECT 1 FROM "+DbHelper.TABLE_APP_STATE+" s WHERE s.state_key='r959_development_done_'||g.genome_id AND s.state_value='DONE')"
                +" AND NOT EXISTS(SELECT 1 FROM "+DbHelper.TABLE_APP_STATE+" s WHERE s.state_key='r978_development_retry_at_'||g.genome_id AND CAST(s.state_value AS INTEGER)>?)";
    }
    public static boolean hasUnservedTraining(DbStore store) {
        if (store == null) return false;
        try(Cursor c=store.readable().rawQuery("SELECT 1 FROM "+DbHelper.TABLE_R908_ALGORITHM_GENOME
                +" g WHERE "+eligibleDevelopment()+" LIMIT 1",new String[]{Long.toString(System.currentTimeMillis())})) {
            return c.moveToFirst();
        }
    }

    public static Result pulse(DbStore store,long budgetMs) {
        long started=TimeSourceR811.monoNowMs();
        if(store==null)return new Result("STORE_NULL",0,0,0,0,0);
        int before=count(store,DbHelper.TABLE_R908_ALGORITHM_GENOME);
        int assets=count(store,AssetGenesisEngineR915.TABLE_ASSET_GENOME);
        ResearchWorkBudgetR927.Scope scope=ResearchWorkBudgetR927.enter(Math.max(1,Math.min(1000,budgetMs)));
        String status;
        try {
            ResearchWorkBudgetR927.checkpoint();
            status=trainSlice(store,TimeSourceR811.monoNowMs()+Math.min(Math.max(1,Math.min(1000,budgetMs)),ResearchWorkBudgetR927.remainingMs()));
        } catch(ResearchWorkBudgetR927.Yield y){status="YIELD:"+y.getMessage();}
        catch(Exception e){status="ERROR:"+e.getClass().getSimpleName()+":"+e.getMessage();}
        finally{ResearchWorkBudgetR927.leave(scope);}
        Result result=new Result(status,before,count(store,DbHelper.TABLE_R908_ALGORITHM_GENOME),assets,
                count(store,AssetGenesisEngineR915.TABLE_ASSET_GENOME),TimeSourceR811.monoNowMs()-started);
        LinkedHashMap<String,String> telemetry=new LinkedHashMap<String,String>();
        telemetry.put("r922_dynamic_bootstrap_status",result.status);
        telemetry.put("r922_dynamic_bootstrap_cost_ms",String.valueOf(result.elapsedMs));
        telemetry.put("r922_dynamic_bootstrap_at",String.valueOf(System.currentTimeMillis()));
        telemetry.put("r922_dynamic_bootstrap_direct_real","0");
        String selected=store.getState("r960_development_last_parent","");
        // Missing history/capacity or a damaged input remains visible and retryable,
        // but cannot trigger an unbounded same-period busy loop.
        if(!selected.isEmpty() && (status.startsWith("WAIT_") || status.startsWith("DEV_WAIT_POPULATION_CAPACITY") || status.startsWith("ERROR:"))) {
            telemetry.put("r978_development_retry_at_"+selected,Long.toString(System.currentTimeMillis()+5000L));
            telemetry.put("r978_development_reason_"+selected,status);
        }
        store.setStates(telemetry);
        return result;
    }

    private static String trainSlice(DbStore store,long deadline)throws JSONException {
        String spec="",id="";
        // A NEW development trial is frozen at registration's latest known draw.
        // Existing jobs retain their original dataset/cutoff and cannot be relabeled.
        String birth=ResearchLearningContinuityR912.latest(store);
        if(!TargetIssueResolver.isValid(birth))return "WAIT_VALID_HISTORY_CUTOFF";
        String lastParent=store.getState("r960_development_last_parent","");
        String focus=store.getState("r979_development_focus","");
        int focusTurns=0;try{focusTurns=Integer.parseInt(store.getState("r979_development_focus_turns","0"));}catch(NumberFormatException ignored){}
        // A bounded run of 64 callbacks finishes useful work instead of cold-starting
        // all 64 parents each time. Persisted before work; expires even on repeated yields.
        if(focusTurns>=64)focus="";
        try(Cursor c=store.readable().rawQuery("SELECT g.genome_spec,g.genome_id FROM "
                +DbHelper.TABLE_R908_ALGORITHM_GENOME+" g WHERE "+eligibleDevelopment()
                +" ORDER BY CASE WHEN g.genome_id=? THEN 0 ELSE 1 END,"
                +" CASE WHEN EXISTS(SELECT 1 FROM "+DbHelper.TABLE_APP_STATE+" served WHERE served.state_key='r960_development_advanced_'||g.genome_id AND served.state_value=?) THEN 1 ELSE 0 END,"
                +" CASE WHEN g.genome_id>? THEN 0 ELSE 1 END,g.genome_id LIMIT 1",
                new String[]{Long.toString(System.currentTimeMillis()),focus,birth,lastParent})) {
            if(c.moveToFirst()){spec=c.getString(0);id=c.getString(1);}
        }
        if(id.isEmpty())return "IDLE_NO_UNTRAINED_REGISTERED_ALGORITHM";
        // Advance before computation, including WAIT/YIELD/error. The model checkpoint itself
        // still commits atomically with its cursor and never reuses another parent's state.
        LinkedHashMap<String,String> rotation=new LinkedHashMap<String,String>();
        rotation.put("r960_development_last_parent",id);
        rotation.put("r979_development_focus",id);
        rotation.put("r979_development_focus_turns",Integer.toString(id.equals(focus)?focusTurns+1:1));
        ResearchLearningContinuityR912.commitProgress(store,rotation);
        AlgorithmGenomeR908 parent=AlgorithmGenomeR908.parse(spec);
        String key=JOB+id; String before=store.getState(key,"");
        if(before.isEmpty()) {
            String registered=register(store,parent,birth,key);
            before=store.getState(key,"");
            if(before.isEmpty()||TimeSourceR811.monoNowMs()>=deadline-100)return registered;
        }
        JSONObject job=new JSONObject(before);
        if(!job.getString("parent_spec").equals(parent.canonical()))throw new IllegalStateException("R959_DEV_PARENT_CHANGED");
        if(!TrainingContinuationR973.compatible(job.optString("training_code",""))) {
            abandonChangedSource(store,job,key,id,before);
            String registered=register(store,parent,birth,key);
            before=store.getState(key,"");
            if(before.isEmpty())return registered;
            job=new JSONObject(before);
            if(TimeSourceR811.monoNowMs()>=deadline-100)return "DEV_REGISTERED|RESTART_AFTER_SOURCE_CHANGE|futureN=0";
        }
        String payload=store.getState(INPUT+id,"");
        if(!hash(payload).equals(job.getString("dataset")))throw new IllegalStateException("R959_DEV_DATASET_HASH");
        JSONArray rows=new JSONArray(payload); List<Models.Draw> history=decode(rows);
        int cursor=job.getInt("cursor"),n=job.getInt("n"),hit=job.getInt("hit"),run=job.getInt("miss_run"),max=job.getInt("max_miss");
        int initial=cursor,initialN=n; String trace=job.getString("trace");
        AlgorithmGenomeVmR908.State state=AlgorithmGenomeVmR908.State.decode(job.getString("state"));
        if(!state.encode().equals(job.getString("state"))||state.updates!=n)throw new IllegalStateException("R959_DEV_STATE_CURSOR");
        while(cursor<history.size() && cursor-initial<16 && TimeSourceR811.monoNowMs()<deadline-100) {
            // Once at least one row advanced, a cooperative yield must flow to
            // the atomic checkpoint below, not discard the completed local batch.
            try { ResearchWorkBudgetR927.checkpoint(); }
            catch(ResearchWorkBudgetR927.Yield yielded){if(cursor==initial)throw yielded;break;}
            if(rows.getJSONArray(cursor).getInt(11)==0){cursor++;continue;}
            AlgorithmInventionLabR908.DevelopmentStep step;
            try { step=AlgorithmInventionLabR908.trainDevelopmentStep(store,parent,state,
                    history.subList(Math.max(0,cursor-512),cursor),history.get(cursor),deadline-100); }
            catch(ResearchWorkBudgetR927.Yield yielded){if(cursor==initial)throw yielded;break;}
            n++;hit+=step.hit;run=step.hit==1?0:run+1;max=Math.max(max,run);
            trace=hash(trace+'|'+history.get(cursor).issue+'|'+step.ordinal+'|'+step.hit+'|'+state.encode());
            int block=Math.min(3,Math.max(0,(cursor-job.getInt("start"))*4/Math.max(1,history.size()-job.getInt("start"))));
            JSONArray bn=job.getJSONArray("block_n"),bh=job.getJSONArray("block_hit");bn.put(block,bn.getInt(block)+1);bh.put(block,bh.getInt(block)+step.hit);
            cursor++;
        }
        job.put("cursor",cursor);job.put("n",n);job.put("hit",hit);job.put("miss_run",run);job.put("max_miss",max);
        job.put("state",state.encode());job.put("trace",trace);
        job.put("last_execution_source",SourceManifestR815.SOURCE_TREE_SHA256);
        final boolean finished=cursor==history.size();
        if(finished && n<24)throw new IllegalStateException("R959_DEV_INSUFFICIENT_UNRESERVED_LABELS");
        final String expected=before,encoded=job.toString(),jobKey=key,parentId=id;
        final boolean advanced=n>initialN;
        final String servedIssue=ResearchLearningContinuityR912.latest(store);
        final JSONObject committed=job;
        final boolean[] published={false};
        Boolean saved=store.coordinatorR36().runAtomicWrite("R959_NUMERIC_DEV_CHECKPOINT",new DatabaseCoordinator.WriteCallable<Boolean>(){
            public Boolean call(SQLiteDatabase db) {
                if(!read(db,jobKey).equals(expected))throw new IllegalStateException("R959_DEV_CURSOR_CONFLICT");
                try {if(finished)published[0]=complete(db,committed);}
                catch(JSONException e){throw new IllegalStateException("R959_DEV_COMPLETE_JSON",e);}
                put(db,jobKey,committed.toString());if(published[0])put(db,"r959_development_done_"+parentId,"DONE");
                if(advanced)put(db,"r960_development_advanced_"+parentId,servedIssue);
                put(db,"r978_development_progress_"+parentId,"historicalN="+committed.optInt("n")
                        +"|cursor="+committed.optInt("cursor")+"|cutoff="+committed.optString("birth")
                        +"|stateHash="+hash(committed.optString("state"))+"|futureEvidenceAdded=0");
                return Boolean.TRUE;
            }
        },Boolean.FALSE);
        if(!Boolean.TRUE.equals(saved))throw new IllegalStateException("R959_DEV_CHECKPOINT_COMMIT_FAILED");
        return (finished?(published[0]?"DEV_COMPLETED":"DEV_WAIT_POPULATION_CAPACITY"):"DEV_RUNNING")+"|parent="+id+"|trained="+n+"|cursor="+cursor+"/"+history.size()+"|futureN=0";
    }

    private static String register(DbStore store,AlgorithmGenomeR908 parent,String birth,String key)throws JSONException {
        List<Models.Draw> history=store.getDrawsBeforeIssue(TargetIssueResolver.next(birth).targetIssue,HISTORY);
        Collections.sort(history,(a,b)->a.issue.compareTo(b.issue));
        if(history.size()<96)return "WAIT_96_HISTORY_FOR_DEVELOPMENT";
        ArrayList<String[]> reserved=new ArrayList<String[]>();
        try(Cursor c=store.readable().rawQuery("SELECT first_issue,last_issue FROM "+FixedResearchWindowR932.WINDOW,null)) {
            while(c.moveToNext())reserved.add(new String[]{c.getString(0),c.getString(1)});
        }
        JSONArray data=new JSONArray();String previous="";int start=Math.max(48,history.size()-TRAIN),usable=0;
        for(int i=0;i<history.size();i++) {
            Models.Draw d=history.get(i);
            if(!d.isValid()||d.dirty!=0||d.issue.compareTo(birth)>0||d.issue.compareTo(previous)<=0)
                throw new IllegalStateException("R959_DEV_INPUT_ORDER_OR_CUTOFF");previous=d.issue;
            boolean allowed=i>=start;
            for(String[] window:reserved)if(d.issue.compareTo(window[0])>=0&&(window[1].isEmpty()||d.issue.compareTo(window[1])<=0))allowed=false;
            JSONArray row=new JSONArray();row.put(d.issue);for(int x:d.numbers)row.put(x);row.put(allowed?1:0);data.put(row);if(allowed)usable++;
        }
        if(usable<24)return "WAIT_INDEPENDENT_DEV_LABELS_RESERVED_WINDOWS_EXCLUDED";
        final JSONObject job=new JSONObject();String payload=data.toString();
        job.put("parent_spec",parent.canonical());job.put("birth",birth);job.put("source",SourceManifestR815.SOURCE_TREE_SHA256);
        job.put("training_code",TrainingCodeIdentityR964.SHA256);job.put("last_execution_source",SourceManifestR815.SOURCE_TREE_SHA256);
        job.put("dataset",hash(payload));job.put("start",start);job.put("cursor",start);job.put("n",0);job.put("hit",0);
        job.put("state",AlgorithmGenomeVmR908.initialState(parent).encode());job.put("trace",hash(VERSION+parent.structuralHash));
        job.put("miss_run",0);job.put("max_miss",0);job.put("block_n",new JSONArray("[0,0,0,0]"));job.put("block_hit",new JSONArray("[0,0,0,0]"));
        job.put("registered_at",System.currentTimeMillis());
        job.put("hyper",hash(VERSION+"|trainingCode="+TrainingCodeIdentityR964.SHA256+"|history="+HISTORY+"|train="+TRAIN+"|profile="+HistoricalStrategyInventionR908.HISTORY_MAX));
        job.put("split",hash("DEV_ONLY|cutoff="+birth+"|NO_RESERVED_WINDOW_LABELS"));
        ExperimentProtocolR929.TrialIdentity identity=identity(job);
        final ExperimentProtocolR929.TrialRecord trial=new ExperimentProtocolR929.TrialRecord(identity,ExperimentProtocolR929.TrialStatus.RUNNING,
                job.getLong("registered_at"),job.getLong("registered_at"),0,"","",false,"");
        final String inputKey=INPUT+parent.genomeId;
        Boolean ok=store.coordinatorR36().runAtomicWrite("R959_DEV_PREREGISTER",new DatabaseCoordinator.WriteCallable<Boolean>(){
            public Boolean call(SQLiteDatabase db){
                if(!read(db,key).isEmpty())return Boolean.FALSE;
                ResearchPlatformStoreR929.createSchema(db);
                if(db.insertWithOnConflict(ResearchPlatformStoreR929.TABLE_TRIAL,null,ResearchPlatformStoreR929.trialValues(trial),SQLiteDatabase.CONFLICT_ABORT)<0)
                    throw new IllegalStateException("R959_DEV_TRIAL_INSERT");
                ResearchMultiplicityR932.registerCandidate(db,"development_trial:"+trial.identity.trialId,
                        DirectedResearchCreationR932.algorithmFamily(parent),hash("DEV|"+trial.identity.trialId),TargetIssueResolver.next(birth).targetIssue);
                put(db,inputKey,payload);put(db,key,job.toString());return Boolean.TRUE;
            }
        },Boolean.FALSE);
        if(!Boolean.TRUE.equals(ok))throw new IllegalStateException("R959_DEV_PREREGISTER_COMMIT_FAILED");
        return "DEV_REGISTERED|labels="+usable+"|trial="+identity.trialId+"|futureN=0";
    }

    private static void abandonChangedSource(DbStore store,JSONObject job,final String key,final String parentId,final String expected)throws JSONException {
        final ExperimentProtocolR929.TrialRecord failed=new ExperimentProtocolR929.TrialRecord(identity(job),ExperimentProtocolR929.TrialStatus.FAILED,
                job.getLong("registered_at"),job.getLong("registered_at"),System.currentTimeMillis(),"","SOURCE_DEPENDENCY_CHANGED",false,"");
        boolean ok=store.coordinatorR36().runAtomicWrite("R959_DEV_DEPENDENCY_RESTART",new DatabaseCoordinator.WriteRunnable(){
            public void run(SQLiteDatabase db){
                if(!expected.equals(read(db,key)))throw new IllegalStateException("R959_DEV_DEPENDENCY_CURSOR_CONFLICT");
                if(db.update(ResearchPlatformStoreR929.TABLE_TRIAL,ResearchPlatformStoreR929.trialValues(failed),"trial_id=? AND status='RUNNING'",new String[]{failed.identity.trialId})!=1)
                    throw new IllegalStateException("R959_DEV_DEPENDENCY_TRIAL_CONFLICT");
                put(db,"r959_development_archive_"+failed.identity.trialId,expected);
                put(db,"r965_development_archive_input_"+failed.identity.trialId,read(db,INPUT+parentId));
                db.delete(DbHelper.TABLE_APP_STATE,"state_key IN (?,?)",new String[]{key,INPUT+parentId});
            }
        });
        if(!ok)throw new IllegalStateException("R959_DEV_DEPENDENCY_RESTART_COMMIT_FAILED");
    }

    private static boolean complete(SQLiteDatabase db,JSONObject job)throws JSONException {
        AlgorithmGenomeR908 parent=AlgorithmGenomeR908.parse(job.getString("parent_spec"));
        AlgorithmGenomeVmR908.State learned=AlgorithmGenomeVmR908.State.decode(job.getString("state"));
        // Model initialization is learned; its live update counter starts at zero, separately from DEV N.
        learned.updates=0;
        AlgorithmGenomeR908 child=new AlgorithmGenomeR908(parent.generation+1,parent.genomeId,
                parent.scoreProgram,parent.updateProgram,parent.scoreOutputReg,learned.encode());
        String latest="";try(Cursor c=db.rawQuery("SELECT issue FROM "+DbHelper.TABLE_DRAWS+" WHERE dirty=0 ORDER BY issue DESC LIMIT 1",null)){if(c.moveToFirst())latest=c.getString(0);}
        if(!TargetIssueResolver.isValid(latest))throw new IllegalStateException("R959_DEV_NO_CURRENT_BOUNDARY");
        if(!ResearchPopulationR966.normalize(db,latest))return false;
        String victim=ResearchPopulationR966.replacementForDevelopment(db,parent.genomeId);
        if(victim==null)return false; // Keep finished numeric state durable; retry publication later.
        String first=TargetIssueResolver.next(latest).targetIssue;
        double blockMin=1;JSONArray bn=job.getJSONArray("block_n"),bh=job.getJSONArray("block_hit");
        for(int i=0;i<4;i++)blockMin=Math.min(blockMin,bn.getInt(i)==0?0:(double)bh.getInt(i)/bn.getInt(i));
        ExperimentProtocolR929.TrialIdentity identity=identity(job);
        AlgorithmInventionLabR908.registerTrained(db,child,job.getString("birth"),first,
                job.getInt("n"),job.getInt("hit"),job.getInt("max_miss"),blockMin,identity.trialId);
        String childKey="algorithm:"+child.genomeId;
        ResearchMultiplicityR932.registerCandidate(db,childKey,DirectedResearchCreationR932.algorithmFamily(parent),child.structuralHash,first);
        ResearchLifecycleR932.enroll(db,childKey,child.structuralHash,first,"C");
        ResearchLifecycleR932.freshChild(db,childKey,"algorithm:"+parent.genomeId,parent.structuralHash,child.structuralHash,
                "INITIAL_VM_PARAMETERS","CHRONOLOGICAL_DEV", "UNCHANGED_PRIMITIVE_INPUTS",
                "INDEPENDENT_FUTURE_IMPROVEMENT","FAIL_FIXED_WINDOW_WITHOUT_GAIN","PARAMETER_VARIANT",first);
        AlgorithmProofRecoveryR948.requireCleanForFreeze(db,child.genomeId);
        ExperimentProtocolR929.TrialRecord done=new ExperimentProtocolR929.TrialRecord(identity,ExperimentProtocolR929.TrialStatus.COMPLETED,
                job.getLong("registered_at"),job.getLong("registered_at"),System.currentTimeMillis(),hash(child.canonical()),"",false,"");
        if(db.update(ResearchPlatformStoreR929.TABLE_TRIAL,ResearchPlatformStoreR929.trialValues(done),"trial_id=? AND status='RUNNING'",new String[]{identity.trialId})!=1)
            throw new IllegalStateException("R959_DEV_TRIAL_COMPLETION_CONFLICT");
        job.put("output_child",child.genomeId);job.put("output_hash",hash(child.canonical()));
        ResearchPopulationR966.committed(db,victim,childKey,latest);
        return true;
    }
    private static ExperimentProtocolR929.TrialIdentity identity(JSONObject j)throws JSONException {
        AlgorithmGenomeR908 g=AlgorithmGenomeR908.parse(j.getString("parent_spec"));
        return new ExperimentProtocolR929.TrialIdentity(V51Repository.GAME_CODE,hash("R908_INPUT14_FULL1200_V1"),j.getString("dataset"),
                j.getString("source"),j.getString("hyper"),0,DirectedResearchCreationR932.algorithmFamily(g),
                g.structuralHash,ExperimentProtocolR929.Stage.NUMBER,j.getString("split"),null);
    }
    private static List<Models.Draw> decode(JSONArray rows)throws JSONException {
        List<Models.Draw> out=new ArrayList<Models.Draw>();
        for(int i=0;i<rows.length();i++){JSONArray r=rows.getJSONArray(i);Models.Draw d=new Models.Draw();d.issue=r.getString(0);for(int k=0;k<10;k++)d.numbers[k]=r.getInt(k+1);out.add(d);}return out;
    }
    private static int count(DbStore store,String table){try(Cursor c=store.readable().rawQuery("SELECT COUNT(*) FROM "+table,null)){return c.moveToFirst()?c.getInt(0):0;}}
    private static String read(SQLiteDatabase db,String key){try(Cursor c=db.rawQuery("SELECT state_value FROM "+DbHelper.TABLE_APP_STATE+" WHERE state_key=?",new String[]{key})){return c.moveToFirst()?c.getString(0):"";}}
    private static void put(SQLiteDatabase db,String key,String value){ContentValues v=new ContentValues();v.put("state_key",key);v.put("state_value",value);v.put("updated_at",System.currentTimeMillis());if(db.insertWithOnConflict(DbHelper.TABLE_APP_STATE,null,v,SQLiteDatabase.CONFLICT_REPLACE)<0)throw new IllegalStateException("R959_DEV_STATE_WRITE");}
    private static String hash(String value){return V51ChecksumEngine.sha256(value);}

    public static final class Result {
        public final String status;
        public final int algorithmsBefore, algorithmsAfter, assetsBefore, assetsAfter;
        public final long elapsedMs;

        Result(String status, int algorithmsBefore, int algorithmsAfter,
                int assetsBefore, int assetsAfter, long elapsedMs) {
            this.status = status == null ? "" : status;
            this.algorithmsBefore = Math.max(0, algorithmsBefore);
            this.algorithmsAfter = Math.max(0, algorithmsAfter);
            this.assetsBefore = Math.max(0, assetsBefore);
            this.assetsAfter = Math.max(0, assetsAfter);
            this.elapsedMs = Math.max(0L, elapsedMs);
        }

        public boolean changed() {
            return algorithmsAfter > algorithmsBefore || assetsAfter > assetsBefore;
        }

        public String canonical() {
            return VERSION + '|' + status + "|algorithm=" + algorithmsBefore + "->"
                    + algorithmsAfter + "|asset=" + assetsBefore + "->" + assetsAfter
                    + "|elapsedMs=" + elapsedMs + "|directREAL=0";
        }
    }
}
