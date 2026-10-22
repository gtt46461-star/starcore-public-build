package com.ethk3.novaonepro;

import java.io.*;
import java.nio.*;
import java.nio.file.*;
import java.util.*;
import org.json.*;

/** Offline historical research only. No invented phone capture timestamp or production export. */
public final class HistoricalActionExportR958 {
    public static void main(String[] args)throws Exception{
        if("parity".equals(args[0])){parity(args);return;}
        List<V51Models.DrawFrame> all=new ArrayList<V51Models.DrawFrame>();
        for(String line:Files.readAllLines(Paths.get(args[0]))){String[] fields=line.split("\t");if(fields.length!=12)throw new IllegalArgumentException("HISTORY_SHAPE");int[] numbers=new int[10];for(int i=0;i<10;i++)numbers[i]=Integer.parseInt(fields[i+2]);all.add(new V51Models.DrawFrame(fields[0],numbers,Long.parseLong(fields[1])));}
        JSONObject parent=new JSONObject(new String(Files.readAllBytes(Paths.get(args[1])),"UTF-8"));
        if(!"HISTORICAL_REPLAY".equals(parent.getString("evidence_scope"))||parent.getBoolean("production_authority"))throw new IllegalStateException("PARENT_SCOPE");
        UpstreamModelMathR932.Gate gate=new UpstreamModelMathR932.Gate(parent.getJSONObject("model"));
        UpstreamModelMathR932.Number model=new UpstreamModelMathR932.Number(new JSONObject("{\"kind\":\"GATE9\"}"));
        if(!"UNTRAINED".equals(V51StateModelArtifact.DATA_SHA256)||!V51OfflineResidualArtifact.TRAINED_UNTIL_ISSUE.isEmpty())throw new IllegalStateException("PARENT_PROVENANCE_REVIEW_REQUIRED");
        V51PermutationFeatureEngine featuresOwner=new V51PermutationFeatureEngine();V51StateTrajectoryEngine statesOwner=new V51StateTrajectoryEngine();
        V51PermutationNumberModelEngine expertsOwner=new V51PermutationNumberModelEngine();V51Models.PermutationFeatureSnapshot previous=null;
        try(OutputStream meta=new BufferedOutputStream(new FileOutputStream(args[2]));DataOutputStream raw=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(args[3])))){
            raw.writeUTF("R958_HISTORICAL_FULL1200_V1");raw.writeInt(all.size()-240);raw.writeUTF(V51ActionMetaFeatureEngine.featureManifestHash());
            for(int t=240;t<all.size();t++){
                V51Models.DrawFrame[] past=all.subList(t-240,t).toArray(new V51Models.DrawFrame[0]);String target=all.get(t).issue,base=past[past.length-1].issue;
                if(!TargetIssueResolver.exact(base,target).valid||parent.getString("trained_until_issue").compareTo(past[0].issue)>=0)throw new IllegalStateException("HISTORICAL_PARENT_OR_ISSUE_NOT_PIT");
                for(V51Models.DrawFrame row:past)if(row.issue.compareTo(target)>=0)throw new IllegalStateException("LABEL_IN_FEATURES");
                V51Models.PredictionContext context=new V51Models.PredictionContext(base,target,base,V51PermutationFeatureEngine.historyHash(past),V51BuildManifest.COHORT_ID,V51ArtifactBundleManifest.BUNDLE_HASH,System.currentTimeMillis(),past.length);
                V51Models.PermutationFeatureSnapshot features=featuresOwner.build(context,past);
                V51Models.StateTrajectorySnapshot[] states=statesOwner.buildAudited(features,previous).states;
                V51Models.ExpertEvidence[] experts=expertsOwner.predict(features,states,past).experts;
                double[][] state=UpstreamModelMathR932.stateValues(states);double[][][] expert=UpstreamModelMathR932.expertValues(experts);
                double[] weights=gate.weights(state,expert);double[][] logits=model.logits(UpstreamModelMathR932.features(state,expert),null,expert,weights);
                V51SinkhornEngine.ProjectionResult projected=new V51SinkhornEngine().projectDetailed(logits,1,240,1e-10);
                if(!projected.productionValid())throw new IllegalStateException("NUMBER_PROJECTION_INVALID");
                V51EffectiveConsensusEngine.Consensus[] consensus=new V51EffectiveConsensusEngine().evaluate(experts,states,null);
                V51Models.PositionAction[] actions=new V51PositionActionBuilder().build(projected.matrix,states,consensus);
                K3ActionSpaceEngine.validateOrThrow(actions);double[][] gateRows=new double[10][];for(int p=0;p<10;p++)gateRows[p]=weights.clone();
                double[][] values=new V51ActionMetaFeatureEngine().build(actions,features,states,experts,gateRows,consensus);
                ByteBuffer buffer=ByteBuffer.allocate(1200*96*8).order(ByteOrder.LITTLE_ENDIAN);boolean[] seen=new boolean[1200];
                for(int i=0;i<actions.length;i++){
                    V51Models.PositionAction a=actions[i];int ordinal=K3ActionSpaceEngine.globalOrdinal(a.position,a.n1,a.n2,a.n3);
                    if(seen[ordinal])throw new IllegalStateException("DUPLICATE_ACTION");seen[ordinal]=true;buffer.position(ordinal*96*8);
                    for(double value:values[i]){if(!Double.isFinite(value))throw new IllegalStateException("NONFINITE_META");buffer.putDouble(value);}
                }
                meta.write(buffer.array());raw.writeUTF(target);raw.writeUTF(base);
                for(double[] row:state)for(double value:row)raw.writeDouble(value);
                for(double[][] matrix:expert)for(double[] row:matrix)for(double value:row)raw.writeDouble(value);
                for(double weight:weights)raw.writeDouble(weight);
                for(double[] row:projected.matrix.probability)for(double value:row)raw.writeDouble(value);
                // The target label is accessed only after every feature, model and action value.
                for(int value:all.get(t).permutation)raw.writeInt(value-1);
                previous=features;if((t-239)%250==0)System.out.println("FULL1200_HISTORICAL_EXPORT="+(t-239)+"/"+(all.size()-240));
            }
        }
        System.out.println("HISTORICAL_FULL1200_COMPLETE|n="+(all.size()-240)+"|future_n=0|production_authority=false");
    }
    private static void parity(String[] args)throws Exception{
        JSONObject bundle=new JSONObject(new String(Files.readAllBytes(Paths.get(args[1])),"UTF-8"));
        TrainedModelMathR932.Ranker ranker=new TrainedModelMathR932.Ranker(bundle.getJSONObject("ranker"));
        TrainedModelMathR932.Calibration calibration=new TrainedModelMathR932.Calibration(bundle.getJSONObject("calibration"));
        try(DataInputStream source=new DataInputStream(new BufferedInputStream(new FileInputStream(args[2])));DataOutputStream out=new DataOutputStream(new BufferedOutputStream(new FileOutputStream(args[3])))){
            byte[] bytes=new byte[1200*96*8];
            for(int t=0;t<100;t++){
                source.readFully(bytes);ByteBuffer values=ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN);
                double[] scores=new double[1200],raw=new double[1200];int best=0;
                for(int a=0;a<1200;a++){double[] x=new double[96];for(int f=0;f<96;f++)x[f]=values.getDouble();raw[a]=x[0];scores[a]=ranker.score(x,x[0]);out.writeDouble(scores[a]);
                    if(scores[a]>scores[best]||scores[a]==scores[best]&&raw[a]>raw[best])best=a;
                }
                out.writeInt(best);out.writeDouble(calibration.predict(scores[best]));
            }
        }
        System.out.println("ACTUAL_JAVA_RANKER_CALIBRATION_PARITY_OUTPUT|issues=100|full_actions=120000");
    }
}
