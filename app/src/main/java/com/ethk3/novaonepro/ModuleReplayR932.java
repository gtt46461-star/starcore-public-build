package com.ethk3.novaonepro;
import java.util.*;

/** Read-only same-input ablation through production Number/Gate/Consensus/META/Ranker kernels. */
public final class ModuleReplayR932 {
    public enum Module { BASELINE,FTRL,TINY_DEEP,TEACHER,TREND,SHAPE,RELATION,INVENTED }
    private ModuleReplayR932(){}
    static final class Result {
        final V51Models.PositionAction[] actions;
        final V51Models.PositionAction selected;
        final V51PermutationNumberModelEngine.Result number;
        final boolean disabledActive;
        final String actionHash;
        Result(V51Models.PositionAction[] actions,V51PermutationNumberModelEngine.Result number,boolean active) {
            this.actions=actions;this.number=number;selected=actions[0];disabledActive=active;
            StringBuilder hash=new StringBuilder();for(V51Models.PositionAction a:actions)hash.append(K3ActionSpaceEngine.globalOrdinal(a.position,a.n1,a.n2,a.n3)).append(':').append(Double.toHexString(a.rankScore)).append('|');actionHash=V51ChecksumEngine.sha256(hash.toString());
        }
    }
    static Result rerun(FrozenModuleInputR932.Input input,Module module) {
        if(!ModelBundleRuntimeR932.current().hash.equals(input.bundle))throw new IllegalArgumentException("R932_ABLATION_BUNDLE_NOT_PINNED");
        int off=module==Module.FTRL?8:module==Module.TEACHER?6:module==Module.TREND?1:module==Module.SHAPE?4:module==Module.RELATION?5:-1;
        boolean[] disabled=new boolean[9];V51Models.ExpertEvidence[] experts=input.experts.clone();
        if(off>=0) {
            disabled[off]=true;V51Models.ExpertEvidence e=experts[off];double[] uncertainty=new double[10];Arrays.fill(uncertainty,1);
            experts[off]=new V51Models.ExpertEvidence(e.expertId,e.artifactId,new double[10][10],new double[10],uncertainty,new double[10],new String[]{"EXPLICIT_OFF_COUNTERFACTUAL"},new String[0],V51ChecksumEngine.sha256(e.checksum+"|OFF"));
        }
        V51ExpertGateEngine.Result gate=new V51ExpertGateEngine().evaluateFrozen(experts,input.states,input.health,disabled);
        V51PermutationNumberModelEngine.Result number=new V51PermutationNumberModelEngine().predictFrozen(input.features,input.states,experts,gate,input.pastSlow);
        V51EffectiveConsensusEngine.Consensus[] consensus=new V51EffectiveConsensusEngine().evaluateFrozen(experts,input.states,input.reliability,disabled);
        V51Models.PositionAction[] actions=new V51PositionActionBuilder().build(number.matrix,input.states,consensus);
        double[][] meta=new V51ActionMetaFeatureEngine().build(actions,input.features,input.states,experts,gate.weight,consensus);
        V51ActionRankerEngine.RankResult ranked=new V51ActionRankerEngine().rankDetailed(actions,meta);
        V51Models.PositionAction[] finalActions=ranked.ranked;
        if(input.consumerApplied&&module!=Module.INVENTED)finalActions=FrozenConsumerRecipeR932.execute(input.consumer,finalActions).actions;
        finalActions=finalActions.clone();Arrays.sort(finalActions,new Comparator<V51Models.PositionAction>(){public int compare(V51Models.PositionAction a,V51Models.PositionAction b){return Integer.compare(a.rank,b.rank);}});
        Result result=new Result(finalActions,number,off>=0||(module==Module.INVENTED&&input.consumerApplied));
        if(module==Module.BASELINE) {
            for(int p=0;p<10;p++){for(int n=0;n<10;n++)requireClose(input.q[p][n],number.matrix.probability[p][n],"NUMBER");for(int e=0;e<9;e++)requireClose(input.gate[p][e],gate.weight[p][e],"GATE");}
            for(V51Models.PositionAction action:result.actions)requireClose(input.finalScores[K3ActionSpaceEngine.globalOrdinal(action.position,action.n1,action.n2,action.n3)],action.rankScore,"RANK_SCORE");
            if(K3ActionSpaceEngine.globalOrdinal(result.selected.position,result.selected.n1,result.selected.n2,result.selected.n3)!=input.selectedOrdinal)throw new IllegalStateException("R932_ABLATION_BASELINE_SELECTION_PARITY");
        }
        return result;
    }
    private static void requireClose(double expected,double actual,String stage){if(!Double.isFinite(actual)||Math.abs(expected-actual)>1e-10*Math.max(1,Math.abs(expected)))throw new IllegalStateException("R932_ABLATION_BASELINE_PARITY:"+stage);}
}
