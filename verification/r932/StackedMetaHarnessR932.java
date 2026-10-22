package com.ethk3.novaonepro;
import java.io.*;
import java.util.Locale;

/** Regenerate actual production history/features/actions/META against changed upstream Q/Gate. */
public final class StackedMetaHarnessR932 {
    public static void main(String[] args)throws Exception {
        double error=0;long values=0;
        try(DataInputStream in=new DataInputStream(new BufferedInputStream(new FileInputStream(args[0])))){
            int count=in.readInt();
            for(int t=0;t<count;t++){
                V51Models.DrawFrame[] history=new V51Models.DrawFrame[24];
                for(int i=0;i<24;i++){int[] draw=new int[10];for(int n=0;n<10;n++)draw[n]=(n+i+t)%10+1;history[i]=new V51Models.DrawFrame(String.format(Locale.US,"20260910%04d",i+t+1),draw,1700000000000L+(i+t)*60000L);}
                String base=history[23].issue,target=String.format(Locale.US,"20260910%04d",t+25);
                String hash=V51PermutationFeatureEngine.historyHash(history);
                V51Models.PredictionContext context=new V51Models.PredictionContext(base,target,base,hash,"FIXTURE",ModelBundleRuntimeR932.current().hash,1700000000001L+t*60000,24);
                V51Models.PermutationFeatureSnapshot features=new V51PermutationFeatureEngine().build(context,history);
                V51Models.StateTrajectorySnapshot[] states=new V51StateTrajectoryEngine().buildAudited(features,null).states;
                V51PermutationNumberModelEngine.Result original=new V51PermutationNumberModelEngine().predict(features,states,history);
                V51EffectiveConsensusEngine.Consensus[] consensus=new V51EffectiveConsensusEngine().evaluate(original.experts,states,new V51ExpertRegretEngine());
                double[][] q=new double[10][10],gate=new double[10][9];
                for(int p=0;p<10;p++)for(int n=0;n<10;n++)q[p][n]=in.readDouble();
                for(int e=0;e<9;e++){double weight=in.readDouble();for(int p=0;p<10;p++)gate[p][e]=weight;}
                V51Models.NumberProbabilityMatrix matrix=new V51Models.NumberProbabilityMatrix(q,0,0,60,true,false,new int[10],"SYNTHETIC_CHANGED_NUMBER_Q");
                V51Models.PositionAction[] actions=new V51PositionActionBuilder().build(matrix,states,consensus);
                double[][] actual=new V51ActionMetaFeatureEngine().build(actions,features,states,original.experts,gate,consensus);
                for(int a=0;a<1200;a++)for(int f=0;f<96;f++){double expected=in.readDouble();error=Math.max(error,Math.abs(actual[a][f]-expected));values++;}
            }
            if(in.read()!=-1)throw new AssertionError("EXTRA_FIXTURE_BYTES");
        }
        if(error>1e-10)throw new AssertionError("ACTUAL_META_REBUILD_DRIFT:"+error);
        System.out.println("R932_STACKED_META_PASS|values="+values+"|maxError="+error+"|scope=ACTUAL_JAVA_BUILDER_SYNTHETIC_CHANGED_Q_GATE");
    }
}
