package com.ethk3.novaonepro.strategyv2;

import android.database.sqlite.SQLiteDatabase;
import java.util.*;

/** Resolve immutable parameter manifests before numerical execution; historical targets must follow training. */
public final class ModelAssetsSU1 {
    private ModelAssetsSU1() {}
    public static Map<String,ProgramTreeSU1.Fitted> load(SQLiteDatabase db,String text,EventFrameSU1 frame) {
        Map<String,ProgramTreeSU1.Fitted> out = new HashMap<>();
        for (ProgramTextSU1.Term term : ProgramTextSU1.subtrees(ProgramTextSU1.parse(text),false)) {
            if (!term.op.equals("MODEL")) continue;
            String hash = term.args.get(0).toString();
            TrainingCheckpointSU1 checkpoint = TrainingCheckpointSU1.decode(CampaignStateSU1.localBlob(db,hash));
            if (!checkpoint.hash.equals(hash) || checkpoint.window != Integer.parseInt(term.args.get(1).toString()))
                throw new IllegalArgumentException("SU1_MODEL_PROGRAM_BINDING");
            int first = 0;
            while (first < frame.size() && String.valueOf(frame.issue(first)).compareTo(checkpoint.throughIssue) <= 0) first++;
            if (String.valueOf(frame.targetIssue).compareTo(checkpoint.throughIssue) <= 0)
                throw new IllegalArgumentException("SU1_MODEL_TRAINED_ON_TARGET");
            out.put(hash,new ProgramTreeSU1.Fitted(checkpoint.weights,first));
        }
        return Collections.unmodifiableMap(out);
    }
    public static Map<String,ProgramTreeSU1.GraphFitted> loadGraph(SQLiteDatabase db,String text,EventFrameSU1 frame){
        Map<String,ProgramTreeSU1.GraphFitted> out=new HashMap<>();
        for(ProgramTextSU1.Term term:ProgramTextSU1.subtrees(ProgramTextSU1.parse(text),false))if(term.op.equals("GMODEL")){
            String hash=term.args.get(0).toString();RelGraphCheckpointSU1 checkpoint=RelGraphCheckpointSU1.decode(CampaignStateSU1.localBlob(db,hash));if(!hash.equals(checkpoint.hash)||checkpoint.window!=Integer.parseInt(term.args.get(1).toString()))throw new IllegalArgumentException("SU1_GRAPH_PROGRAM_BINDING");
            int first=0;while(first<frame.size()&&String.valueOf(frame.issue(first)).compareTo(checkpoint.throughIssue)<=0)first++;
            if(String.valueOf(frame.targetIssue).compareTo(checkpoint.throughIssue)<=0)throw new IllegalArgumentException("SU1_GRAPH_TRAINED_ON_TARGET");out.put(hash,new ProgramTreeSU1.GraphFitted(checkpoint.state.weights,first));
        }return Collections.unmodifiableMap(out);
    }
}
