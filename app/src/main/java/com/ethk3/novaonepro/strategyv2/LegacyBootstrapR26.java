package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.util.*;

/** Rebuild only program/router/gate history from cached prequential outputs; adopt exact trained members/calibrators. */
public final class LegacyBootstrapR26 {
    private LegacyBootstrapR26() {}
    public static LegacyReplayStateR26 build(String canonical,long[] ordinals,ModelReplaySU2.Library lib)throws IOException {
        if(ordinals.length!=lib.draws.length||ordinals.length==0)throw new IOException("R26_BOOTSTRAP_SOURCE_SHAPE");
        LegacyReplayStateR26 state=new LegacyReplayStateR26(Collections.singletonList(canonical),ordinals[0]);LiveStrategySU2 live=state.lives.values().iterator().next();
        Map<String,ModelGenomeSU2.Member> members=new LinkedHashMap<>();gather(canonical,members);
        Map<String,OnlineModelsSU2.Output> raw=new LinkedHashMap<>(),cal=new LinkedHashMap<>();
        for(Map.Entry<String,ModelGenomeSU2.Member> e:members.entrySet()){ExecutionBudgetR26.poll();raw.put(e.getKey(),lib.get(e.getValue()));if(state.hub.cal.containsKey(e.getKey()))cal.put(e.getKey(),lib.getCal(e.getValue()));}
        for(int t=0;t<lib.draws.length;t++){
            ExecutionBudgetR26.poll();if(t>0&&ordinals[t]!=ordinals[t-1]+1)throw new IOException("R26_BOOTSTRAP_NONCONTIGUOUS_SOURCE");
            state.hub.p.clear();state.hub.pc.clear();state.hub.st.clear();
            for(String key:members.keySet()){OnlineModelsSU2.Output out=raw.get(key);state.hub.p.put(key,out.p[t]);state.hub.st.put(key,out.state[t]);if(cal.containsKey(key))state.hub.pc.put(key,cal.get(key).p[t]);}
            state.hub.predicted=true;live.decide();live.settle(lib.draws[t]);state.hub.t++;
        }
        for(Map.Entry<String,ModelGenomeSU2.Member> e:members.entrySet()){
            ExecutionBudgetR26.poll();ModelGenomeSU2.Member member=e.getValue();OnlineModelsSU2.Model model;
            if(lib.durable!=null)model=lib.durable.trained(member,lib.draws.length);
            else {model=ModelReplaySU2.build(member.kind,member.params);float[][] p=new float[10][10];float[] f=new float[model.stateWidth()];for(int[] d:lib.draws){ExecutionBudgetR26.poll();model.predict(p,f);model.update(d);}}
            state.hub.models.put(e.getKey(),model);
            if(state.hub.cal.containsKey(e.getKey())){
                CalibrationSU2.Live fitted;if(lib.durable!=null)fitted=lib.durable.calibrator(member,lib.draws.length);else{fitted=new CalibrationSU2.Live();OnlineModelsSU2.Output out=raw.get(e.getKey());for(int t=0;t<lib.draws.length;t++){ExecutionBudgetR26.poll();fitted.predict(out.p[t]);fitted.settle(out.p[t],lib.draws[t]);}}
                state.hub.cal.put(e.getKey(),fitted);
            }
        }
        state.hub.predicted=false;state.hub.p.clear();state.hub.pc.clear();state.hub.st.clear();return LegacyReplayStateR26.imported(state.canonical,state.genesis,ordinals[ordinals.length-1],state.hub,state.lives);
    }
    private static void gather(String text,Map<String,ModelGenomeSU2.Member> out){
        if(StageStrategySU2.isStage(text)){for(String child:StageStrategySU2.definition(text).candidates)gather(child,out);out.put(StageStrategySU2.REGIME.modelKey(),StageStrategySU2.REGIME);}
        else if(CommitteeSU2.isCommittee(text)){for(ModelGenomeSU2 g:CommitteeSU2.parse(text).subs)gather(g.text(),out);}
        else for(ModelGenomeSU2.Member member:ModelGenomeSU2.parse(text).members)out.put(member.modelKey(),member);
    }
}
