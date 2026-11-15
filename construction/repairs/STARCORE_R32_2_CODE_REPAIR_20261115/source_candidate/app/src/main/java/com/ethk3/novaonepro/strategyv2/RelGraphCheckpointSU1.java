package com.ethk3.novaonepro.strategyv2;

import com.ethk3.novaonepro.r952.StrictCanonicalJson;
import java.util.*;

/** Immutable graph, complete Adam state, declared objective, data boundary and parent identity. */
public final class RelGraphCheckpointSU1 {
    public static final String FORMAT="SU1_RELGRAPH10_CHECKPOINT_V1",JOINT_FORMAT="SU1_RELGRAPH10_JOINT_CHECKPOINT_V2";
    public final RelGraphOptimizerSU1 state;
    public final TrainingObjectiveSU1 objective;
    public final int window;
    public final String format,datasetHash,throughIssue,parentHash,stateWire,stateHash,wire,hash;
    public final double learningRate,clipNorm;
    public RelGraphCheckpointSU1(RelGraphOptimizerSU1 state,int window,String dataset,String through,String parent,double rate,double clip){
        this(state,window,dataset,through,parent,rate,clip,TrainingObjectiveSU1.crossEntropy(),true);
    }
    public RelGraphCheckpointSU1(RelGraphOptimizerSU1 state,int window,String dataset,String through,String parent,double rate,double clip,TrainingObjectiveSU1 objective){
        this(state,window,dataset,through,parent,rate,clip,objective,false);
    }
    private RelGraphCheckpointSU1(RelGraphOptimizerSU1 state,int window,String dataset,String through,String parent,double rate,double clip,TrainingObjectiveSU1 objective,boolean legacy){
        if(state==null||objective==null||window<1||window>256||through==null||!through.matches("20[0-9]{10}")||!Double.isFinite(rate)||rate<=0||rate>1||!Double.isFinite(clip)||clip<=0||clip>100||!state.weights.featureSchemaHash.equals(RelationTensorAdapterSU1.SCHEMA)||state.weights.channels!=2||state.weights.roleCount!=2||state.weights.typeCount!=2)throw new IllegalArgumentException("SU1_GRAPH_CHECKPOINT_CONTRACT");
        this.state=state;this.objective=objective;this.window=window;format=legacy?FORMAT:JOINT_FORMAT;datasetHash=HashingSU1.sha(dataset);throughIssue=through;parentHash=HashingSU1.sha(parent);learningRate=rate;clipNorm=clip;stateWire=RelGraphCodecSU1.encode(state);stateHash=HashingSU1.text(stateWire);
        Map<String,Object> m=new TreeMap<>();m.put("format",format);m.put("kernel",ModelKernelRegistrySU1.RELGRAPH);m.put("protocol",RelGraphModelSU1.PROTOCOL);m.put("features",RelationTensorAdapterSU1.SCHEMA);m.put("dtype","FLOAT64");m.put("stateMode","WINDOW_RESET");m.put("normalizer","FIXED_PROBABILITY_AND_LOG_LIFT_WITH_SEPARATE_MASKS");m.put("operators","EVENT_FEATURE>ROLE_HYPEREDGE_TANH>INCOMING_SOFTMAX>MESSAGE_SUM>RECURRENT_TANH>EVENT_AND_STATE_HEAD>SOFTMAX");m.put("width",state.weights.d);m.put("channels",2);m.put("roles",2);m.put("types",2);m.put("window",window);m.put("optimizer","ADAM_BETA_09_0999_EPS_1E8_V1");m.put("steps",state.steps);m.put("stateWire",stateWire);m.put("stateHash",stateHash);m.put("dataset",datasetHash);m.put("trainedThroughIssue",through);m.put("parent",parentHash);m.put("learningRateHex",Double.toHexString(rate));m.put("clipHex",Double.toHexString(clip));
        if(!legacy){m.put("objective",Arrays.asList(Double.toHexString(objective.brier),Double.toHexString(objective.smoothness),Double.toHexString(objective.l2),Double.toHexString(objective.resource)));m.put("objectiveHash",objective.hash);}
        wire=StrictCanonicalJson.writeContext(m);hash=HashingSU1.text(wire);
    }
    public static RelGraphCheckpointSU1 decode(String wire){
        if(wire==null||wire.length()>262144)throw new IllegalArgumentException("SU1_GRAPH_CHECKPOINT_BYTES");
        Map<String,Object> m=StrictCanonicalJson.readContextObject(wire);boolean legacy=FORMAT.equals(m.get("format"));if(!legacy&&!JOINT_FORMAT.equals(m.get("format")))throw new IllegalArgumentException("SU1_GRAPH_CHECKPOINT_FORMAT");
        TrainingObjectiveSU1 objective=TrainingObjectiveSU1.crossEntropy();
        if(!legacy){if(!(m.get("objective") instanceof List)||((List<?>)m.get("objective")).size()!=4)throw new IllegalArgumentException("SU1_GRAPH_CHECKPOINT_OBJECTIVE");List<?> values=(List<?>)m.get("objective");objective=new TrainingObjectiveSU1(Double.valueOf((String)values.get(0)),Double.valueOf((String)values.get(1)),Double.valueOf((String)values.get(2)),Double.valueOf((String)values.get(3)));}
        RelGraphOptimizerSU1 state=RelGraphCodecSU1.decode((String)m.get("stateWire"),(String)m.get("stateHash"));Object window=m.get("window");if(!(window instanceof Number)||!window.toString().matches("[0-9]+"))throw new IllegalArgumentException("SU1_GRAPH_CHECKPOINT_WINDOW");
        RelGraphCheckpointSU1 result=new RelGraphCheckpointSU1(state,Integer.parseInt(window.toString()),(String)m.get("dataset"),(String)m.get("trainedThroughIssue"),(String)m.get("parent"),Double.valueOf((String)m.get("learningRateHex")),Double.valueOf((String)m.get("clipHex")),objective,legacy);
        if(!result.wire.equals(wire))throw new IllegalArgumentException("SU1_GRAPH_CHECKPOINT_UNKNOWN_OR_CHANGED_MANIFEST");return result;
    }
    public RelGraphCheckpointSU1 continueOne(RelGraphModelSU1.Input input,int[] label,String newDataset,String labelIssue,BudgetTokenSU1 budget){
        if(input.length()!=window||labelIssue==null||labelIssue.compareTo(throughIssue)<=0)throw new IllegalArgumentException("SU1_GRAPH_CONTINUATION_BOUNDARY");
        RelGraphModelSU1.Gradient gradient=RelGraphModelSU1.gradient(state.weights,input,label,budget,objective);return new RelGraphCheckpointSU1(state.apply(gradient,learningRate,clipNorm),window,newDataset,labelIssue,hash,learningRate,clipNorm,objective,FORMAT.equals(format));
    }
}
