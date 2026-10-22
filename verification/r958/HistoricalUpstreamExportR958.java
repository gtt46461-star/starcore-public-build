package com.ethk3.novaonepro;

import java.io.*;
import java.nio.file.*;
import java.util.*;

/** Historical research export using production feature/state/expert code. Never live predraw evidence. */
public final class HistoricalUpstreamExportR958 {
    public static void main(String[] args)throws Exception {
        List<String> lines=Files.readAllLines(Paths.get(args[0]));ArrayList<V51Models.DrawFrame> all=new ArrayList<>();
        for(String line:lines) {
            String[] p=line.split("\t");if(p.length!=12)throw new IllegalArgumentException("HISTORY_COLUMNS");
            int[] numbers=new int[10];for(int i=0;i<10;i++)numbers[i]=Integer.parseInt(p[i+2]);
            V51Models.DrawFrame draw=new V51Models.DrawFrame(p[0],numbers,Long.parseLong(p[1]));all.add(draw);
        }
        if(!"UNTRAINED".equals(V51StateModelArtifact.DATA_SHA256)||!V51OfflineResidualArtifact.TRAINED_UNTIL_ISSUE.isEmpty())
            throw new IllegalStateException("HISTORICAL_PARENT_ARTIFACT_TRAINING_PROVENANCE_REQUIRES_REVIEW");
        int lookback=240;
        V51PermutationFeatureEngine featureEngine=new V51PermutationFeatureEngine();V51StateTrajectoryEngine stateEngine=new V51StateTrajectoryEngine();
        V51PermutationNumberModelEngine numberEngine=new V51PermutationNumberModelEngine();V51Models.PermutationFeatureSnapshot previous=null;
        try(DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(args[1])))) {
            out.writeUTF("R958_HISTORICAL_UPSTREAM_V1");out.writeInt(all.size()-lookback);
            for(int t=lookback;t<all.size();t++) {
                V51Models.DrawFrame[] history=all.subList(t-lookback,t).toArray(new V51Models.DrawFrame[0]);
                String base=history[history.length-1].issue,target=all.get(t).issue;
                if(!TargetIssueResolver.exact(base,target).valid)throw new IllegalStateException("HISTORICAL_GAP_REQUIRES_SEGMENT_SELECTION");
                for(V51Models.DrawFrame d:history)if(d.issue.compareTo(target)>=0)throw new IllegalStateException("HISTORICAL_PIT_LEAK");
                V51Models.PredictionContext context=new V51Models.PredictionContext(base,target,base,V51PermutationFeatureEngine.historyHash(history),V51BuildManifest.COHORT_ID,V51ArtifactBundleManifest.BUNDLE_HASH,System.currentTimeMillis(),history.length);
                V51Models.PermutationFeatureSnapshot features=featureEngine.build(context,history);
                V51Models.StateTrajectorySnapshot[] states=stateEngine.buildAudited(features,previous).states;
                V51PermutationNumberModelEngine.Result number=numberEngine.predict(features,states,history);
                double[][] state=UpstreamModelMathR932.stateValues(states);double[][][] experts=UpstreamModelMathR932.expertValues(number.experts);
                out.writeUTF(target);out.writeUTF(base);
                for(int p=0;p<10;p++)for(int f=0;f<4;f++){if(!Double.isFinite(state[p][f]))throw new IllegalStateException("NONFINITE_STATE");out.writeDouble(state[p][f]);}
                for(int e=0;e<9;e++)for(int p=0;p<10;p++)for(int n=0;n<10;n++){if(!Double.isFinite(experts[e][p][n]))throw new IllegalStateException("NONFINITE_EXPERT");out.writeDouble(experts[e][p][n]);}
                // Labels are written only after the feature computation, and never reach an expert update.
                for(int n:all.get(t).permutation)out.writeInt(n-1);
                previous=features;
                if((t-lookback+1)%500==0)System.out.println("HISTORICAL_INPUT_PROGRESS="+(t-lookback+1)+"/"+(all.size()-lookback));
            }
        }
        System.out.println("R958_HISTORICAL_UPSTREAM_EXPORTED|n="+(all.size()-lookback)+"|scope=HISTORICAL_REPLAY_NOT_LIVE|online_expert=COLD_FROZEN_NO_LABEL_UPDATES");
    }
}
