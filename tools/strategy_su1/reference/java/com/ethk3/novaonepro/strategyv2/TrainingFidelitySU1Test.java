package com.ethk3.novaonepro.strategyv2;
import java.util.*;
public final class TrainingFidelitySU1Test {
    private static int checks;
    private static void check(boolean b,String message){checks++;if(!b)throw new AssertionError(message);}
    private static BudgetTokenSU1 budget(){return new BudgetTokenSU1(60000,100000000,null);}
    public static void main(String[] args){
        int[][] draws=new int[3400][10];for(int t=0;t<draws.length;t++)for(int p=0;p<10;p++)draws[t][p]=(p+t)%10+1;
        RelationalStateInference.Weights full=RelationalTrainerSU1.trainCausal(draws,100,2,8,19,budget()),explicit=RelationalTrainerSU1.trainCausal(draws,100,2,8,19,budget(),TrainingFidelitySU1.FULL),scout=RelationalTrainerSU1.trainCausal(draws,100,2,8,19,budget(),TrainingFidelitySU1.SCOUT);
        check(RelationalModelCodecSU1.encode(full).equals(RelationalModelCodecSU1.encode(explicit)),"default FULL training changed");check(!RelationalModelCodecSU1.encode(full).equals(RelationalModelCodecSU1.encode(scout)),"cheap fit did not execute a distinct smaller optimization");
        RelationTensorAdapterSU1.Factory factory=new RelationTensorAdapterSU1.Factory(draws);RelGraphOptimizerSU1 graphFull=RelGraphTrainerSU1.trainCausal(factory,draws,100,2,4,19,budget(),TrainingFidelitySU1.FULL),graphScout=RelGraphTrainerSU1.trainCausal(factory,draws,100,2,4,19,budget(),TrainingFidelitySU1.SCOUT);
        check(graphFull.steps==8&&graphScout.steps==2,"cheap graph training faked its update count");
        for(String text:new String[]{"DEEP(2,8,19)","GDEEP(2,4,19)","FUSE(EXPERT(0),XT(3),60)"}){
            ScoreHistoryCacheSU1 cache=new ScoreHistoryCacheSU1(768);ProgramTreeSU1.Node program=ProgramTreeSU1.parse(text);
            program.score(new ProgramTreeSU1.Context(draws,100,null,cache,budget()).withTrainingFidelity(TrainingFidelitySU1.SCOUT));
            double[][] afterScout=program.score(new ProgramTreeSU1.Context(draws,100,null,cache,budget())),fresh=program.score(new ProgramTreeSU1.Context(draws,100,null,new ScoreHistoryCacheSU1(768),budget()));
            check(Arrays.deepEquals(afterScout,fresh),"SCOUT parameters/scores contaminated FULL cache: "+text);
        }
        for(String text:new String[]{"EXPERT(0)","HEDGE(60,1000)","DEEP(2,8,19)","FUSE(EXPERT(0),XT(3),60)"}){
            HistoricalReplayEngineSU1.Replay prior=HistoricalReplayEngineSU1.run(draws,text,1,false,1500,1520,budget());
            try(ReplayCursorSU1 cursor=new ReplayCursorSU1(draws,text,1,false,1500,1520)){
                check(cursor.cursor==1500,"full-coverage replay warmed unused coverage quantiles");cursor.advance(budget());
                for(int t=1500;t<1520;t++)check(cursor.replay.position[t]==prior.position[t]&&cursor.replay.mask[t]==prior.mask[t]&&cursor.replay.hit[t]==prior.hit[t]&&cursor.replay.act[t]==prior.act[t],"warm-up removal changed a real decision: "+text);
            }
        }
        int[][] fixed=new int[3400][10],future=new int[3400][10];for(int t=0;t<fixed.length;t++)for(int p=0;p<10;p++){fixed[t][p]=p+1;future[t][p]=p+1;}for(int t=2400;t<future.length;t++){future[t][0]=2;future[t][1]=1;}
        String dataset=HashingSU1.text("scout boundary");StrategyFoundrySU1.Genome bad=new StrategyFoundrySU1.Genome("NEG(NUMIS(0))","GRAMMAR","GRAMMAR","mutate","",-1,1,false),good=new StrategyFoundrySU1.Genome("NUMIS(0)","GRAMMAR","GRAMMAR","mutate","",-1,1,false);String firstDetail;
        try(StrategyFoundrySU1 a=new StrategyFoundrySU1(fixed,dataset,11,8,1);StrategyFoundrySU1 b=new StrategyFoundrySU1(future,dataset,11,8,1)){
            a.startRefresh(Arrays.asList(bad,good));b.startRefresh(Collections.singleton(bad));StrategyFoundrySU1.Attempt rejected=a.next(budget());firstDetail=rejected.detail;check(rejected.ranked==null&&rejected.result.equals("REJECTED")&&firstDetail.contains("SCOUT_UPPER_BELOW025"),"bad scout advanced to full validation");check(firstDetail.equals(b.next(budget()).detail),"VAL labels leaked into cheap DEV rejection");
            try{a.next(new BudgetTokenSU1(60000,30,null));}catch(BudgetTokenSU1.Yield expected){/* exact in-progress scout cursor */}
            String saved=a.checkpoint();try(StrategyFoundrySU1 restored=StrategyFoundrySU1.restore(fixed,dataset,saved)){
                check(saved.equals(restored.checkpoint()),"fidelity and cursor did not resume exactly");StrategyFoundrySU1.Attempt accepted=restored.next(budget());check(accepted.ranked!=null&&accepted.detail.contains("FINAL_FIDELITY=FULL"),"scout-only weights were published");check(accepted.ranked.devActions==900&&accepted.ranked.valActions==1000,"scout samples credited as extra full evidence");check(((Number)com.ethk3.novaonepro.r952.StrictCanonicalJson.readContextObject(restored.knowledge(rejected)).get("scoutHits")).intValue()==0,"later candidate overwrote a prior immutable scout observation");
            }
        }
        System.out.println("SU1_TRAINING_FIDELITY_PASS checks="+checks+" scope=real_smaller_joint_fit_cache_isolation_DEV_only_screen_and_full_decision_parity");
    }
}
