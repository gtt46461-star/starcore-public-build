package com.ethk3.novaonepro;

import java.util.*;
import org.json.*;

public final class DualMemoryHarnessR932 {
    static void check(boolean b,String name){if(!b)throw new AssertionError(name);System.out.println(name+"=PASS");}
    static String hash(AdaptiveShadowLearnerEngine learner){return ResearchArtifactCodecR811.hash(learner.snapshotArtifact());}
    static AdaptiveShadowLearnerEngine.OosRow row(int i){double x=i%2==0?1:-1;int y=(i>160&&i<=320)?(x<0?1:0):(x>0?1:0);return new AdaptiveShadowLearnerEngine.OosRow(String.format(Locale.US,"20260910%04d",i),i*1000L,i*1000L+1,true,y,new double[]{x});}
    static double loss(AdaptiveShadowLearnerEngine learner,boolean reversed){double a=learner.predictResearch(new double[]{1})-(reversed?0:1),b=learner.predictResearch(new double[]{-1})-(reversed?1:0);return (a*a+b*b)/2;}
    static ResearchArtifactCodecR811.Artifact changed(ResearchArtifactCodecR811.Artifact a,String key,double[] value){Map<String,double[]> fields=new LinkedHashMap<>(a.fields);fields.put(key,value);return new ResearchArtifactCodecR811.Artifact(a.type,a.trainedUntilIssue,fields);}
    static boolean rejects(ResearchArtifactCodecR811.Artifact a){try{AdaptiveShadowLearnerEngine.restore(a);return false;}catch(RuntimeException expected){return true;}}
    public static void main(String[] args)throws Exception{
        AdaptiveShadowLearnerEngine uninterrupted=new AdaptiveShadowLearnerEngine(),restarted=new AdaptiveShadowLearnerEngine(),staticModel=null;
        JSONArray phases=new JSONArray();int restarts=0;double forgetting=0;
        for(int i=1;i<=480;i++){
            AdaptiveShadowLearnerEngine.OosRow row=row(i);String before=hash(uninterrupted);
            if(i>1){uninterrupted.predictResearch(row.features);checkOnce(before.equals(hash(uninterrupted)),"READ_ONLY_PREDICTION");}
            if(!uninterrupted.updateAfterSettlement(row).updated||!restarted.updateAfterSettlement(row).updated)throw new AssertionError("UPDATE");
            if(i%48==0){ResearchArtifactCodecR811.Artifact decoded=ResearchArtifactCodecR811.decode(ResearchArtifactCodecR811.encode(restarted.snapshotArtifact()));restarted=AdaptiveShadowLearnerEngine.restore(decoded);restarts++;}
            if(!hash(uninterrupted).equals(hash(restarted)))throw new AssertionError("RESTART_HASH_DRIFT_"+i);
            if(i==160)staticModel=AdaptiveShadowLearnerEngine.restore(uninterrupted.snapshotArtifact());
            if(i%160==0){JSONObject phase=new JSONObject();phase.put("phase",i==320?"B":"A");phase.put("until",i);phase.put("adaptive_A_brier",loss(uninterrupted,false));phase.put("adaptive_B_brier",loss(uninterrupted,true));phase.put("static_A_brier",loss(staticModel,false));phase.put("static_B_brier",loss(staticModel,true));phase.put("checkpoint_hash",hash(uninterrupted));phases.put(phase);if(i==320)forgetting=loss(uninterrupted,false)-loss(staticModel,false);}
        }
        check(restarts==10&&hash(uninterrupted).equals(hash(restarted)),"TEN_RESTARTS_MATCH_UNINTERRUPTED_WEIGHTS_AND_BOTH_BUFFERS");
        ResearchArtifactCodecR811.Artifact artifact=uninterrupted.snapshotArtifact();
        check(artifact.fields.get("replay_recent").length==32*5&&artifact.fields.get("replay_long").length==64*5,"RECENT_32_LONG_64_ACTUAL_REPLAY_ROWS_ARE_BOUNDED");
        double[] counters=artifact.fields.get("replay_counters");check(counters[0]>480&&counters[0]<=480*5,"ACTUAL_OPTIMIZER_REPLAYS_OLDER_ROWS_WITH_AT_MOST_FIVE_STEPS");
        double[] longer=artifact.fields.get("replay_long");boolean oldA=false;for(int i=0;i<longer.length;i+=5)oldA|=longer[i]<=202609100160d;check(oldA,"LONG_MEMORY_RETAINS_A_AFTER_ABA_TRANSITIONS");
        String before=hash(uninterrupted);check(!uninterrupted.updateAfterSettlement(row(480)).updated&&before.equals(hash(uninterrupted)),"DUPLICATE_SETTLEMENT_CANNOT_REPLAY_GRADIENTS");
        check(!uninterrupted.updateAfterSettlement(new AdaptiveShadowLearnerEngine.OosRow("202609100481",1,2,true,1,new double[]{Double.NaN})).updated&&before.equals(hash(uninterrupted)),"MISSING_INPUT_IS_REJECTED_WITHOUT_CHECKPOINT_MUTATION");
        check(!uninterrupted.updateAfterSettlement(new AdaptiveShadowLearnerEngine.OosRow("202609100481",2,1,true,1,new double[]{1})).updated,"CURRENT_POSTDRAW_FEATURES_CANNOT_ENTER_REPLAY");
        boolean rejected=false;try{uninterrupted.updateAfterSettlement(new AdaptiveShadowLearnerEngine.OosRow("202609100481",1,2,true,1,new double[]{1,2}));}catch(RuntimeException e){rejected=true;}check(rejected&&before.equals(hash(uninterrupted)),"FEATURE_SCHEMA_CHANGE_ROLLS_BACK_WHOLE_UPDATE");
        check(rejects(changed(artifact,"n",new double[]{-1})),"BAD_ACCUMULATOR_CHECKPOINT_REJECTED");
        double[] contract=artifact.fields.get("replay_contract").clone();contract[3]++;check(rejects(changed(artifact,"replay_contract",contract)),"REPLAY_RULE_CHANGE_CANNOT_REUSE_OLD_CHECKPOINT");
        double[] future=artifact.fields.get("replay_recent").clone();future[future.length-5]=202609100481d;check(rejects(changed(artifact,"replay_recent",future)),"FUTURE_LABEL_IN_REPLAY_CHECKPOINT_REJECTED");
        double[] params=artifact.fields.get("params").clone();params[0]*=2;check(rejects(changed(artifact,"params",params)),"CHANGED_LEARNING_RATE_REJECTED");
        byte[] encoded=ResearchArtifactCodecR811.encode(artifact);String checksum=ResearchArtifactCodecR811.hash(artifact);encoded[encoded.length-1]^=1;check(!ResearchArtifactCodecR811.verify(encoded,checksum),"TAMPERED_WEIGHT_OR_MEMORY_BYTES_FAIL_CHECKSUM");
        Map<String,double[]> legacy=new LinkedHashMap<>();for(String name:new String[]{"z","n","meta","params"})legacy.put(name,artifact.fields.get(name));ResearchArtifactCodecR811.Artifact old=new ResearchArtifactCodecR811.Artifact("FTRL_SHADOW",artifact.trainedUntilIssue,legacy);
        AdaptiveShadowLearnerEngine migrated=AdaptiveShadowLearnerEngine.restore(old);check(ResearchArtifactCodecR811.hash(old).equals(hash(migrated)),"LEGACY_CHECKPOINT_REMAINS_BYTE_IDENTICAL_BEFORE_NEXT_UPDATE");
        migrated.updateAfterSettlement(row(481));check(migrated.snapshotArtifact().fields.containsKey("replay_contract")&&!ResearchArtifactCodecR811.hash(old).equals(hash(migrated)),"NEXT_SETTLEMENT_MIGRATES_WITH_NEW_RULE_AND_NEW_HASH");
        JSONObject report=new JSONObject();report.put("scope","SYNTHETIC_ABA_ACTUAL_PRODUCTION_LEARNER_NOT_REAL_FUTURE_EFFECT");report.put("phases",phases);report.put("forgetting_A_after_B",forgetting);report.put("final_A_recovery",loss(uninterrupted,false)-loss(staticModel,false));report.put("optimizer_steps",counters[0]);report.put("regression_rollbacks",counters[1]);report.put("fixed_rule",AdaptiveShadowLearnerEngine.UPDATE_RULE);report.put("may_authorize_real",false);System.out.println(report.toString());
        System.out.println("R932_DUAL_MEMORY_PASS|scope=ACTUAL_FTRL_BOUNDED_REPLAY_SYNTHETIC_ABA");
    }
    static void checkOnce(boolean b,String s){if(!b)throw new AssertionError(s);}
}
