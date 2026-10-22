package com.ethk3.novaonepro;
import java.util.*;
import org.json.*;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

public final class ModuleAblationHarnessR932 {
    static void check(boolean ok,String label){if(!ok)throw new AssertionError(label);System.out.println(label+"=PASS");}
    static String scalar(SQLiteDatabase db,String sql){try(Cursor c=db.rawQuery(sql,null)){return c.moveToFirst()?c.getString(0):"";}}
    static void consumerChecks(V51Models.DrawFrame[] history,V51RuntimeResult original,ModuleReplayR932.Result plain)throws Exception {
        List<Models.Draw> draws=new ArrayList<>();for(V51Models.DrawFrame frame:history){Models.Draw draw=new Models.Draw();draw.issue=frame.issue;draw.openTime=frame.openTime;draw.fetchTime=frame.openTime+100;draw.numbers=frame.permutation.clone();draws.add(draw);}
        HistoricalStrategyInventionR908.Profile profile=HistoricalStrategyInventionR908.Profile.from(draws);
        AlgorithmGenomeR908 algorithm=AlgorithmGenomeR908.seed(932,2,"SYNTHETIC_REPLAY_ONLY");AlgorithmGenomeVmR908.State memory=new AlgorithmGenomeVmR908.State();
        for(int i=0;i<memory.global.length;i++)memory.global[i]=i*.01;memory.updates=19;
        GenesisAssetGenomeR915 genesis=new GenesisAssetGenomeR915("AF33",2,"","SYNTHETIC_REPLAY_ONLY",history[0].issue,GenesisAssetGenomeR915.OutputType.RELATION_FIELD,new int[]{2,22,40,60,80,100,120,140,160},algorithm);
        ResearchMethodPlanR906 linear=new ResearchMethodPlanR906("RESIDUAL_TRANSPORT","raw_action_p+rank_score+third_margin+entropy_risk+stability+window_consensus+assignment_support","FTRL_LINEAR_GRAPH","ONLINE_FTRL_NEXT_ISSUE_ONLY","PAIRED_LOGLOSS_PLUS_HIT","SELECTION_CONDITIONED_ISOTONIC","ENTROPY_DRIFT_BLOCK_GUARD","SEALED_FUTURE_BLOCK_AND_PERMUTATION_TEST","",1,"SYNTHETIC_REPLAY_ONLY",true);
        ResearchMethodPlanR906 historical=new ResearchMethodPlanR906("HISTORICAL_SYNTHESIS_INTERACTION","0.5*position_transition_lift+0.5*frequency_slope","EMPIRICAL_SYMBOLIC_PROGRAM","OOS_EVOLVE_KEEP_MUTATE_PRUNE","HIT_STABILITY_COVERAGE_PARETO","STRICT_OOS_ISOTONIC_IF_ELIGIBLE","BLOCK_REGIME_AND_MAX_STREAK","WALK_FORWARD_ABLATION_COUNTERFACTUAL","",1,"SYNTHETIC_REPLAY_ONLY",true);
        String hash=V51ChecksumEngine.sha256("SYNTHETIC_NO_CERTIFICATE");String base=original.context.baseIssue,target=original.context.targetIssue;
        FrozenConsumerRecipeR932.Recipe[] recipes={
            FrozenConsumerRecipeR932.freeze("INVENTED_ALGORITHM_CERTIFIED",algorithm.genomeId,algorithm.canonical(),memory.encode(),algorithm.structuralHash,hash,hash,.30,base,target,profile,null),
            FrozenConsumerRecipeR932.freeze("GENESIS_NEW_CERTIFIED",genesis.assetId,genesis.canonical(),new GenesisAssetVmR915.State().encode(),genesis.structuralHash,hash,hash,.30,base,target,profile,original),
            FrozenConsumerRecipeR932.freeze("FORMULA_DSL_ACTIVE_SHADOW","SYNTHETIC_LINEAR",linear.canonical(),"",ResearchCompiledGraphRuntimeR906.compile(linear).artifactHash,hash,hash,.30,base,target,null,null),
            FrozenConsumerRecipeR932.freeze("FORMULA_DSL_ACTIVE_SHADOW","SYNTHETIC_HISTORY",historical.canonical(),"",ResearchCompiledGraphRuntimeR906.compile(historical).artifactHash,hash,hash,.30,base,target,profile,null)};
        int changed=0;
        for(FrozenConsumerRecipeR932.Recipe recipe:recipes){
            FrozenConsumerRecipeR932.Mixed mixed=FrozenConsumerRecipeR932.execute(recipe,original.actions);String frozen=recipe.encoded;
            ResearchMainlineContributionAuditR922.Contribution contribution=new ResearchMainlineContributionAuditR922.Contribution(target,recipe.lane,recipe.id,"","SYNTHETIC_REPLAY_ONLY",true,recipe.weight,0,0,0,-1,-1,0,0,0,-1,hash,mixed.rankOne.checksum,false,original.context.createdAt);
            V51RuntimeResult result=new V51RuntimeResult(true,original.context,original.features,original.states,original.numberModel,original.consensus,null,mixed.actions,null,null,null,"SYNTHETIC_REPLAY_ONLY",0,hash,null,contribution,recipe.encoded);
            List<String> proofs=new ArrayList<>();RawTrainingInputR932.append(proofs,result);FrozenModuleInputR932.Input input=FrozenModuleInputR932.decode(proofs);
            ModuleReplayR932.Result rerun=ModuleReplayR932.rerun(input,ModuleReplayR932.Module.BASELINE);
            check(rerun.selected.rank==1&&input.selectedOrdinal==K3ActionSpaceEngine.globalOrdinal(mixed.rankOne.position,mixed.rankOne.n1,mixed.rankOne.n2,mixed.rankOne.n3),"ACTIVE_CONSUMER_REPRODUCES_ALL_1200_SCORES_"+recipe.id);
            ModuleReplayR932.Result without=ModuleReplayR932.rerun(input,ModuleReplayR932.Module.INVENTED);
            check(without.disabledActive&&without.actionHash.equals(plain.actionHash),"INVENTED_OFF_REMOVES_REAL_VM_CONSUMER_"+recipe.id);
            ModuleReplayR932.Result trendOff=ModuleReplayR932.rerun(input,ModuleReplayR932.Module.TREND);
            check(trendOff.actions.length==1200&&!trendOff.actionHash.equals(rerun.actionHash),"UPSTREAM_OFF_REEXECUTES_FROZEN_CONSUMER_"+recipe.id);
            check(frozen.equals(recipe.encoded)&&memory.updates==19,"FROZEN_PROGRAM_AND_VM_MEMORY_REMAIN_UNCHANGED_"+recipe.id);
            if(!rerun.actionHash.equals(plain.actionHash))changed++;
        }
        check(changed==recipes.length,"ALL_FOUR_ACTIVE_VM_FAMILIES_CHANGE_ACTUAL_SCORE_SURFACE");
        JSONObject broken=new JSONObject(recipes[0].encoded);broken.getJSONArray("history").getJSONObject(0).put("issue",target);
        boolean rejected=false;try{new FrozenConsumerRecipeR932.Recipe(broken);}catch(IllegalArgumentException expected){rejected=true;}
        check(rejected,"CONSUMER_HISTORY_CANNOT_INCLUDE_TARGET_DRAW");
        rejected=false;try{FrozenConsumerRecipeR932.decode(recipes[0].encoded,target,recipes[0].lane,"DIFFERENT_ID",.30);}catch(IllegalArgumentException expected){rejected=true;}
        check(rejected,"FROZEN_RECIPE_CANNOT_SUBSTITUTE_ANOTHER_SELECTED_CANDIDATE");
        V51Models.PositionAction[] shifted=original.actions.clone();Arrays.sort(shifted,(a,b)->Integer.compare(b.rank,a.rank));V51Models.PositionAction first=new V51ActionRankerEngine().rankOne(original.actions);
        V51RuntimeResult shuffled=new V51RuntimeResult(true,original.context,original.features,original.states,original.numberModel,original.consensus,null,shifted,null,null,null,"SYNTHETIC_REPLAY_ONLY",0,hash,null,original.contributionAuditR922);
        check(FrozenModuleInputR932.capture(shuffled).getInt("selected_ordinal")==K3ActionSpaceEngine.globalOrdinal(first.position,first.n1,first.n2,first.n3),"CAPTURE_USES_ACTUAL_RANK_ONE_NOT_ARRAY_FIRST");
    }
    public static void main(String[] args)throws Exception {
        V51Models.DrawFrame[] history=new V51Models.DrawFrame[128];
        for(int i=0;i<128;i++){int[] permutation=new int[10];for(int n=0;n<10;n++)permutation[n]=(n+i*3)%10+1;history[i]=new V51Models.DrawFrame(String.format(Locale.US,"20260910%04d",i+1),permutation,1789000000000L+i*60000);}
        String base=history[127].issue,target=TargetIssueResolver.next(base).targetIssue;
        V51Models.PredictionContext context=new V51Models.PredictionContext(base,target,base,V51PermutationFeatureEngine.historyHash(history),V51BuildManifest.COHORT_ID,V51ArtifactBundleManifest.BUNDLE_HASH,history[127].openTime+1000,128);
        V51Models.PermutationFeatureSnapshot features=new V51PermutationFeatureEngine().build(context,history);
        V51Models.StateTrajectorySnapshot[] states=new V51StateTrajectoryEngine().buildAudited(features,null).states;
        V51PermutationNumberModelEngine.Result number=new V51PermutationNumberModelEngine().predict(features,states,history);
        V51EffectiveConsensusEngine.Consensus[] consensus=new V51EffectiveConsensusEngine().evaluate(number.experts,states,new V51ExpertRegretEngine());
        V51Models.PositionAction[] actions=new V51PositionActionBuilder().build(number.matrix,states,consensus);
        double[][] meta=new V51ActionMetaFeatureEngine().build(actions,features,states,number.experts,number.gateWeight,consensus);
        V51ActionRankerEngine.RankResult rank=new V51ActionRankerEngine().rankDetailed(actions,meta);
        ResearchMainlineContributionAuditR922.Contribution contribution=new ResearchMainlineContributionAuditR922.Contribution(target,"PRODUCTION_FALLBACK","","","SYNTHETIC_NO_CONSUMER",false,0,0,0,0,-1,-1,0,0,0,-1,"",rank.rankOne.checksum,false,context.createdAt);
        V51RuntimeResult result=new V51RuntimeResult(true,context,features,states,number,consensus,null,rank.ranked,null,null,null,"SYNTHETIC_MODULE_REPLAY_FIXTURE",0,V51ChecksumEngine.sha256("SYNTHETIC_MODULE_REPLAY"),null,contribution);
        List<String> proofs=new ArrayList<String>();RawTrainingInputR932.append(proofs,result);
        FrozenModuleInputR932.Input input=FrozenModuleInputR932.decode(proofs);
        for(String field:new String[]{"source_sha256","number_schema_sha256","meta_schema_sha256","domain_schema_sha256"}) {
            JSONObject broken=new JSONObject(input.raw.toString());
            if(field.equals("source_sha256"))broken.getJSONObject("module_replay").put(field,V51ChecksumEngine.sha256("wrong-source"));
            else broken.put(field,V51ChecksumEngine.sha256("wrong-schema"));
            String reason="";try{new FrozenModuleInputR932.Input(broken);}catch(IllegalArgumentException expected){reason=expected.getMessage();}
            String code=field.equals("source_sha256")?"SOURCE":field.equals("number_schema_sha256")?"NUMBER_SCHEMA":field.equals("meta_schema_sha256")?"META_SCHEMA":"DOMAIN_SCHEMA";
            check(reason.startsWith("R932_MODULE_REPLAY_"+code+"|"),"REPLAY_NAMES_EXACT_REJECTED_DEPENDENCY:"+field);
        }
        ModuleReplayR932.Result baseline=ModuleReplayR932.rerun(input,ModuleReplayR932.Module.BASELINE);
        check(baseline.actions.length==1200,"FROZEN_ORIGINAL_REPRODUCES_NUMBER_GATE_AND_ALL_1200_RANK_SCORES");
        int changed=0;String before=proofs.get(0);
        for(ModuleReplayR932.Module module:ModuleReplayR932.Module.values()) {
            ModuleReplayR932.Result off=ModuleReplayR932.rerun(input,module);
            check(off.actions.length==1200,"FULL_1200_REEXECUTION_"+module.name());
            if(!off.actionHash.equals(baseline.actionHash))changed++;
            int channel=module==ModuleReplayR932.Module.FTRL?8:module==ModuleReplayR932.Module.TEACHER?6:module==ModuleReplayR932.Module.TREND?1:module==ModuleReplayR932.Module.SHAPE?4:module==ModuleReplayR932.Module.RELATION?5:-1;
            if(channel>=0)for(int p=0;p<10;p++)if(off.number.gateWeight[p][channel]!=0)throw new AssertionError("DISABLED_CHANNEL_STILL_ROUTED");
            if(module==ModuleReplayR932.Module.TINY_DEEP||module==ModuleReplayR932.Module.INVENTED)if(off.disabledActive||!off.actionHash.equals(baseline.actionHash))throw new AssertionError("INACTIVE_MODULE_NEGATIVE_CONTROL");
        }
        check(changed>0,"ACTIVE_MODULE_REMOVAL_CHANGES_ACTUAL_RANKING");
        check(before.equals(proofs.get(0)),"ABLATION_DOES_NOT_MUTATE_FROZEN_ORIGINAL_INPUT");
        JSONObject corrupt=new JSONObject(before.substring(RawTrainingInputR932.PREFIX.length()));corrupt.getJSONArray("expert_logits").getJSONArray(0).getJSONArray(0).put(0,100.0);
        List<String> altered=Arrays.asList(RawTrainingInputR932.PREFIX+corrupt.toString(),RawTrainingInputR932.HASH_PREFIX+V51ChecksumEngine.sha256(corrupt.toString()));
        boolean rejected=false;try{ModuleReplayR932.rerun(FrozenModuleInputR932.decode(altered),ModuleReplayR932.Module.BASELINE);}catch(IllegalStateException expected){rejected=true;}
        check(rejected,"BASELINE_PARITY_FAILURE_BLOCKS_FALSE_ATTRIBUTION");
        consumerChecks(history,result,baseline);
        DeferredEvidenceSnapshotR36.EncodedSnapshot snapshot=DeferredEvidenceSnapshotR36.encodeRuntimeResult(result,null,K3ActionSpaceEngine.actionSpaceHash(rank.ranked));
        SQLiteDatabase db=new SQLiteDatabase(args[0]);try {
            DbStore store=new DbStore(db);ModuleAblationR932.schema(db);
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_DRAWS+"(issue TEXT PRIMARY KEY,n1 INTEGER,n2 INTEGER,n3 INTEGER,n4 INTEGER,n5 INTEGER,n6 INTEGER,n7 INTEGER,n8 INTEGER,n9 INTEGER,n10 INTEGER,dirty INTEGER,open_time INTEGER,fetch_time INTEGER)");
            db.execSQL("INSERT INTO "+DbHelper.TABLE_DRAWS+" VALUES(?,1,2,3,4,5,6,7,8,9,10,0,?,?)",new Object[]{target,context.createdAt+60000,context.createdAt+60100});
            db.execSQL("CREATE TABLE "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+"(target_issue TEXT PRIMARY KEY,snapshot_blob BLOB,snapshot_hash TEXT)");
            db.execSQL("INSERT INTO "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB+" VALUES(?,?,?)",new Object[]{target,snapshot.bytes,snapshot.sha256});
            db.execSQL("CREATE TRIGGER fixture_ablation_abort BEFORE INSERT ON "+ModuleAblationR932.TABLE+" BEGIN SELECT RAISE(ABORT,'INJECTED'); END");
            rejected=false;try{ModuleAblationR932.maintain(store);}catch(IllegalStateException expected){rejected=true;}
            check(rejected&&store.getState("r932_ablation_current","").equals("")&&scalar(db,"SELECT COUNT(*) FROM "+ModuleAblationR932.TABLE).equals("0"),"ABLATION_RESULT_AND_CURSOR_ROLL_BACK_TOGETHER");
            db.execSQL("DROP TRIGGER fixture_ablation_abort");for(int i=0;i<9;i++)ModuleAblationR932.maintain(store);
            if(!scalar(db,"SELECT COUNT(*) FROM "+ModuleAblationR932.TABLE).equals("8"))System.out.println("WORKER_DEBUG="+scalar(db,"SELECT payload FROM "+ModuleAblationR932.TABLE+" ORDER BY created_at DESC LIMIT 1"));
            check(scalar(db,"SELECT COUNT(*) FROM "+ModuleAblationR932.TABLE).equals("8")&&store.getState("r932_ablation_cursor","").equals(target),"EXISTING_WORKER_COMPLETES_BASELINE_AND_SEVEN_MODULES_ONCE");
            check(scalar(db,"SELECT COUNT(*) FROM "+ModuleAblationR932.TABLE+" WHERE state='REEXECUTED'").equals("5")&&scalar(db,"SELECT COUNT(*) FROM "+ModuleAblationR932.TABLE+" WHERE state='INACTIVE_MODULE_NEGATIVE_CONTROL'").equals("2"),"ACTUAL_VS_INACTIVE_MODULES_ARE_SEPARATE_EVIDENCE");
            check(!ModuleAblationR932.summary(store,ModuleReplayR932.Module.FTRL).sufficient,"FEWER_THAN_THIRTY_FAILURES_CANNOT_CLAIM_ATTRIBUTION");
            check(scalar(db,"SELECT COUNT(*) FROM "+DbHelper.TABLE_DRAWS).equals("1")&&snapshot.sha256.equals(scalar(db,"SELECT snapshot_hash FROM "+DbHelper.TABLE_R35_DEFERRED_EVIDENCE_JOB)),"CANONICAL_DRAW_AND_PREDICTION_SNAPSHOT_UNCHANGED");
            System.out.println("R932_MODULE_ABLATION_PASS|scope=SYNTHETIC_SAME_INPUT_PRODUCTION_KERNELS_REAL_SQLITE_NOT_DEVICE_NOT_FUTURE");
        }finally{db.shutdown();}
    }
}
