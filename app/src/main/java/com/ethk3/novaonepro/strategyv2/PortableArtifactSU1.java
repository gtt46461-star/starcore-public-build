package com.ethk3.novaonepro.strategyv2;

import com.ethk3.novaonepro.r952.StrictCanonicalJson;
import java.util.*;

/** Small, closed import surface. Results, hit counts, alpha and certificates are never importable. */
public final class PortableArtifactSU1 {
    public static final String FORMAT="SU1_PORTABLE_GRAMMAR_V1";
    public final String program,dataset,throughIssue,hash,payload;
    public final double coverage;
    public final boolean margin;
    private PortableArtifactSU1(String program,String dataset,String through,String hash,String payload,double coverage,boolean margin){this.program=program;this.dataset=dataset;throughIssue=through;this.hash=hash;this.payload=payload;this.coverage=coverage;this.margin=margin;}
    public static PortableArtifactSU1 decode(String wire){
        Map<String,Object> envelope=StrictCanonicalJson.readObject(wire);if(!envelope.keySet().equals(new HashSet<>(Arrays.asList("payload","sha256")))||!(envelope.get("payload") instanceof Map))throw new IllegalArgumentException("SU1_IMPORT_ENVELOPE");
        @SuppressWarnings("unchecked") Map<String,Object> m=(Map<String,Object>)envelope.get("payload");
        if(!m.keySet().equals(new HashSet<>(Arrays.asList("format","program","coverageHex","marginGate","datasetSha256","trainedThroughIssue","numericProtocol")))||!FORMAT.equals(m.get("format"))||!"SU1_ATOM_CONDITION_V2_1E12".equals(m.get("numericProtocol"))||!(m.get("marginGate") instanceof Boolean))throw new IllegalArgumentException("SU1_IMPORT_PROTOCOL");
        String payload=StrictCanonicalJson.write(m),hash=HashingSU1.text(payload);if(!hash.equals(envelope.get("sha256")))throw new IllegalArgumentException("SU1_IMPORT_HASH");
        String text=(String)m.get("program"),through=(String)m.get("trainedThroughIssue");String program=ProgramTextSU1.canonical(text);if(!program.equals(text)||ProgramTextSU1.parse(text).depth()>6)throw new IllegalArgumentException("SU1_IMPORT_CANONICAL_PROGRAM");
        String data=HashingSU1.sha((String)m.get("datasetSha256"));double coverage=Double.valueOf((String)m.get("coverageHex"));if(!Double.isFinite(coverage)||coverage<.10||coverage>1||through==null||!through.matches("20[0-9]{10}"))throw new IllegalArgumentException("SU1_IMPORT_LIMITS");
        return new PortableArtifactSU1(program,data,through,hash,payload,coverage,(Boolean)m.get("marginGate"));
    }
}
