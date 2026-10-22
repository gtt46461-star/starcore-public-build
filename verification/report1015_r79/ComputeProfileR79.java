package com.ethk3.novaonepro;
import java.nio.file.*;import java.util.*;
/** Pure product computation over the report's original 5000 rows. No prediction publication. */
public final class ComputeProfileR79 {
    static long start;
    static void mark(String name,String checksum){long end=System.nanoTime();System.out.println(name+"|ms="+(end-start)/1e6+"|hash="+checksum);start=end;}
    public static void main(String[] args)throws Exception{
        List<String> lines=Files.readAllLines(Path.of(args[0]));V51Models.DrawFrame[] h=new V51Models.DrawFrame[lines.size()];
        for(int i=0;i<h.length;i++){String[] p=lines.get(i).split("\\t");int[] n=new int[10];for(int j=0;j<10;j++)n[j]=Integer.parseInt(p[j+2]);h[i]=new V51Models.DrawFrame(p[0],n,Long.parseLong(p[1]));}
        for(int repeat=0;repeat<3;repeat++)try(RecipeBudgetScopeCP12 scope=RecipeBudgetScopeCP12.open()){
            start=System.nanoTime();String base=h[h.length-1].issue,target=TargetIssueResolver.next(base).targetIssue;
            V51Models.PredictionContext context=new V51Models.PredictionContext(base,target,base,V51PermutationFeatureEngine.historyHash(h),V51BuildManifest.COHORT_ID,ModelBundleRuntimeR932.current().hash,123456,h.length);
            V51PermutationFeatureEngine engine=new V51PermutationFeatureEngine();V51Models.PermutationFeatureSnapshot f=engine.build(context,h);mark("features",f.checksum);
            V51Models.DrawFrame[] previous=Arrays.copyOf(h,h.length-1);String prior=previous[previous.length-1].issue;
            V51Models.PredictionContext pc=new V51Models.PredictionContext(prior,base,prior,V51PermutationFeatureEngine.historyHash(previous),V51BuildManifest.COHORT_ID,context.artifactBundleHash,context.createdAt,previous.length);
            V51Models.PermutationFeatureSnapshot pf=engine.build(pc,previous);mark("previous_features",pf.checksum);
            V51StateTrajectoryEngine.TrajectoryBundle states=new V51StateTrajectoryEngine().buildAudited(f,pf);mark("states",states.checksum);
            mark("window",new WindowFeatureEngine().buildB05Evidence(h,states.states).checksum);
            mark("trend",new TrendStateMachine().buildB05Evidence(h,states.states).checksum);
            mark("break",new BreakRecoveryEngine().buildB05Evidence(h,states.states).checksum);
            mark("cycle",new CycleRhythmEngine().buildB05Evidence(h).checksum);
            mark("drift",new DistributionDriftEngine().buildB05Evidence(h).checksum);
            V51NumberExpert[] experts={new V51HierarchicalPriorNumberExpert(),new V51MultiScaleTrendNumberExpert(),new V51ContextTreeNumberExpert(),new V51StateDurationNumberExpert(),new V51MotifNumberExpert(),new V51RelationGraphNumberExpert(),new V51TeacherPosteriorNumberExpert(),new V51OfflineResidualNumberExpert(),new V51OnlineResidualNumberExpert()};
            for(V51NumberExpert expert:experts)mark("expert_"+expert.id(),expert.evaluate(f,states.states,h).checksum);
        }
    }
}
