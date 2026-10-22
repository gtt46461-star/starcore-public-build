package com.ethk3.novaonepro.strategyv2;

import com.ethk3.novaonepro.r952.StrictCanonicalJson;
import java.util.*;

/** Closed imports carry executable research assets. Results, hits, alpha and certificates are not fields. */
public final class PortableArtifactSU1 {
    public static final String FORMAT="SU1_PORTABLE_GRAMMAR_V1";
    public static final String MODEL_FORMAT="SU1_PORTABLE_FITTED_MODEL_V2";
    public static final String GRAPH_FORMAT="SU1_PORTABLE_FITTED_RELGRAPH_V3";
    public final String program,dataset,throughIssue,hash,payload;
    public final double coverage;
    public final boolean margin;
    public final TrainingCheckpointSU1 fitted;
    public final RelGraphCheckpointSU1 graph;
    private PortableArtifactSU1(String program,String dataset,String through,String hash,String payload,double coverage,boolean margin,TrainingCheckpointSU1 fitted,RelGraphCheckpointSU1 graph){this.program=program;this.dataset=dataset;throughIssue=through;this.hash=hash;this.payload=payload;this.coverage=coverage;this.margin=margin;this.fitted=fitted;this.graph=graph;}
    public static PortableArtifactSU1 decode(String wire){
        Map<String,Object> envelope=StrictCanonicalJson.readContextObject(wire);
        if(!envelope.keySet().equals(new HashSet<>(Arrays.asList("payload","sha256")))||!(envelope.get("payload") instanceof Map))throw new IllegalArgumentException("SU1_IMPORT_ENVELOPE");
        @SuppressWarnings("unchecked") Map<String,Object> m=(Map<String,Object>)envelope.get("payload");
        boolean model=MODEL_FORMAT.equals(m.get("format")),graphModel=GRAPH_FORMAT.equals(m.get("format"));
        Set<String> fields=new HashSet<>(Arrays.asList("format","program","coverageHex","marginGate","datasetSha256","trainedThroughIssue","numericProtocol"));if(model)fields.add("fittedModel");if(graphModel)fields.add("graphModel");
        if(!m.keySet().equals(fields)||!model&&!graphModel&&!FORMAT.equals(m.get("format"))||!"SU1_ATOM_CONDITION_V2_1E12".equals(m.get("numericProtocol"))||!(m.get("marginGate") instanceof Boolean))throw new IllegalArgumentException("SU1_IMPORT_PROTOCOL");
        String payload=StrictCanonicalJson.writeContext(m),hash=HashingSU1.text(payload);if(!hash.equals(envelope.get("sha256")))throw new IllegalArgumentException("SU1_IMPORT_HASH");
        String text=(String)m.get("program"),through=(String)m.get("trainedThroughIssue");String program=ProgramTextSU1.canonical(text);if(!program.equals(text)||ProgramTextSU1.parse(text).depth()>6)throw new IllegalArgumentException("SU1_IMPORT_CANONICAL_PROGRAM");
        String data=HashingSU1.sha((String)m.get("datasetSha256"));double coverage=Double.valueOf((String)m.get("coverageHex"));if(!Double.isFinite(coverage)||coverage<.10||coverage>1||through==null||!through.matches("20[0-9]{10}"))throw new IllegalArgumentException("SU1_IMPORT_LIMITS");
        TrainingCheckpointSU1 checkpoint=model?TrainingCheckpointSU1.decode((String)m.get("fittedModel")):null;
        RelGraphCheckpointSU1 graph=graphModel?RelGraphCheckpointSU1.decode((String)m.get("graphModel")):null;
        if(model&&(!data.equals(checkpoint.datasetHash)||!through.equals(checkpoint.throughIssue)||!program.equals("MODEL("+checkpoint.hash+","+checkpoint.window+")")))throw new IllegalArgumentException("SU1_IMPORT_MODEL_IDENTITY");
        if(graphModel&&(!data.equals(graph.datasetHash)||!through.equals(graph.throughIssue)||!program.equals("GMODEL("+graph.hash+","+graph.window+")")))throw new IllegalArgumentException("SU1_IMPORT_GRAPH_IDENTITY");
        if(!model&&!graphModel){if(wire.length()>32768)throw new IllegalArgumentException("SU1_GRAMMAR_IMPORT_BYTES");for(ProgramTextSU1.Term t:ProgramTextSU1.subtrees(ProgramTextSU1.parse(program),false))if(t.op.equals("MODEL")||t.op.equals("GMODEL"))throw new IllegalArgumentException("SU1_IMPORT_MODEL_MANIFEST_REQUIRED");}
        return new PortableArtifactSU1(program,data,through,hash,payload,coverage,(Boolean)m.get("marginGate"),checkpoint,graph);
    }
}
