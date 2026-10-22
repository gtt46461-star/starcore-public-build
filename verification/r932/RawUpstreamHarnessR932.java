package com.ethk3.novaonepro;
import java.io.*;
import java.util.*;
import org.json.*;

/** Synthetic chronological draws through production feature/expert/state capture. */
public final class RawUpstreamHarnessR932 {
    public static void main(String[] args)throws Exception{
        try(Writer writer=new OutputStreamWriter(new FileOutputStream(args[0]),"UTF-8")){
            int issueCount=args.length>2?Integer.parseInt(args[2]):120;
            if(issueCount<100)throw new IllegalArgumentException("PARITY_REQUIRES_AT_LEAST_100");
            for(int t=0;t<issueCount;t++){
                V51Models.DrawFrame[] history=new V51Models.DrawFrame[24];
                for(int i=0;i<history.length;i++){
                    int[] draw=new int[10];for(int n=0;n<10;n++)draw[n]=(n+i+t)%10+1;
                    history[i]=new V51Models.DrawFrame(String.format(Locale.US,"20260910%04d",i+t+1),draw,1700000000000L+(i+t)*60000L);
                }
                String base=history[23].issue,target=String.format(Locale.US,"20260910%04d",t+25);
                V51Models.PredictionContext context=new V51Models.PredictionContext(base,target,base,V51PermutationFeatureEngine.historyHash(history),
                    V51BuildManifest.COHORT_ID,V51ArtifactBundleManifest.BUNDLE_HASH,history[23].openTime+1000,history.length);
                V51Models.PermutationFeatureSnapshot features=new V51PermutationFeatureEngine().build(context,history);
                V51Models.StateTrajectorySnapshot[] states=new V51StateTrajectoryEngine().buildAudited(features,null).states;
                V51PermutationNumberModelEngine.Result number=new V51PermutationNumberModelEngine().predict(features,states,history);
                V51RuntimeResult result=new V51RuntimeResult(true,context,features,states,number,null,null,null,null,null,null,"SYNTHETIC_CAPTURE_TEST",0,V51ChecksumEngine.sha256("fixture"));
                List<String> proofs=new ArrayList<>();RawTrainingInputR932.append(proofs,result);
                if(!RawTrainingInputR932.ready(proofs))throw new AssertionError("RAW_NOT_READY:"+proofs);
                JSONObject payload=new JSONObject(proofs.get(0).substring(RawTrainingInputR932.PREFIX.length()));
                for(int p=0;p<10;p++)for(int n=0;n<10;n++)for(int f=0;f<18;f++)
                    if(Double.doubleToLongBits(payload.getJSONArray("number_features").getJSONArray(p).getJSONArray(n).getDouble(f))!=Double.doubleToLongBits(features.numberFeatures[p][n][f]))throw new AssertionError("RAW_FEATURE_PARITY");
                for(int e=0;e<9;e++)for(int p=0;p<10;p++)for(int n=0;n<10;n++)
                    if(Double.doubleToLongBits(payload.getJSONArray("expert_logits").getJSONArray(e).getJSONArray(p).getDouble(n))!=Double.doubleToLongBits(number.experts[e].rawScore[p][n]))throw new AssertionError("EXPERT_LOGIT_PARITY");
                JSONObject row=new JSONObject();row.put("target_issue",target);row.put("max_source_issue",base);row.put("upstream_raw_available",true);row.put("proofs",new JSONArray(proofs));
                if(args.length>1&&args[1].equals("full-export")) {
                    V51EffectiveConsensusEngine.Consensus[] consensus=new V51EffectiveConsensusEngine().evaluate(number.experts,states,new V51ExpertRegretEngine());
                    V51Models.PositionAction[] actions=new V51PositionActionBuilder().build(number.matrix,states,consensus);
                    double[][] meta=new V51ActionMetaFeatureEngine().build(actions,features,states,number.experts,number.gateWeight,consensus);
                    JSONArray exported=new JSONArray();for(int a=0;a<actions.length;a++){V51Models.PositionAction action=actions[a];JSONArray entry=new JSONArray();entry.put(action.position);entry.put(action.n1);entry.put(action.n2);entry.put(action.n3);entry.put(new JSONArray(meta[a]));exported.put(entry);}
                    JSONArray actual=new JSONArray();for(int n=0;n<10;n++)actual.put((n+t+1)%10+1);
                    row.put("integrity_version",2);row.put("schema","R929_CANONICAL_1200_META96");row.put("game",V51Repository.GAME_CODE);row.put("input_stage","ACTION_META_1200");
                    row.put("feature_manifest_sha256",V51ActionMetaFeatureEngine.featureManifestHash());row.put("actions",exported);row.put("actual",actual);
                    row.put("frozen_at",context.createdAt);row.put("draw_at",context.createdAt+59000L);
                    for(String field:new String[]{"snapshot_hash","parent_bundle_hash","selection_contract_hash","record_hash"})row.put(field,V51ChecksumEngine.sha256("SYNTHETIC_EXPORT|"+field+"|"+target));
                    String json=row.toString();writer.write(json.substring(0,json.length()-1)+",\"payload_sha256\":\""+V51ChecksumEngine.sha256(json)+"\"}");
                } else writer.write(row.toString());writer.write('\n');
            }
        }
        System.out.println("R932_RAW_UPSTREAM_PASS|issues="+(args.length>2?args[2]:"120")+"|fields_per_issue=2700|scope=PRODUCTION_CAPTURE_SYNTHETIC_HISTORY");
    }
}
