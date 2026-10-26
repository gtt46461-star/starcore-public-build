package com.ethk3.novaonepro.strategyv2;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** HOST-compiled reference. No Android integration or REAL authorization here. */
public final class LearningPolicyR25 {
    private LearningPolicyR25() {}
    public static final Set<String> FIELDS = Collections.unmodifiableSet(new TreeSet<>(Arrays.asList(
        "feature", "architecture", "optimizer", "update_schedule", "initial_state", "rng",
        "gate", "router", "calibration", "missing_policy", "resource_schedule", "evidence_protocol")));
    public static String identity(Map<String,String> spec) {
        if (!spec.keySet().equals(FIELDS)) throw new IllegalArgumentException("POLICY_RULES_INCOMPLETE");
        // Length-prefixed UTF-8 fields; encoding ID is part of the contract.
        // This Java codec is explicitly different from the Python JSON reference.
        try {
            MessageDigest d=MessageDigest.getInstance("SHA-256");
            d.update("R25_JAVA_POLICY_LENGTH_PREFIX_V1".getBytes(StandardCharsets.UTF_8));
            for(String key:new TreeSet<>(spec.keySet())) {
                String value=spec.get(key);
                if(value==null||value.isEmpty())throw new IllegalArgumentException("EMPTY_RULE");
                for(String s:Arrays.asList(key,value)) {
                    byte[] b=s.getBytes(StandardCharsets.UTF_8);
                    d.update(new byte[]{(byte)(b.length>>>24),(byte)(b.length>>>16),(byte)(b.length>>>8),(byte)b.length});d.update(b);
                }
            }
            StringBuilder out=new StringBuilder();for(byte b:d.digest())out.append(String.format(Locale.ROOT,"%02x",b&255));return out.toString();
        }catch(java.security.NoSuchAlgorithmException e){throw new IllegalStateException(e);}
    }
    public static final class Transition {
        public final String policy,parent,next;
        public final long trainedThrough,target,knownThrough,sequence;
        public Transition(String p,String a,String b,long t,long target,long known,long seq) {
            this.policy=p;this.parent=a;this.next=b;this.trainedThrough=t;this.target=target;this.knownThrough=known;this.sequence=seq;
        }
    }
    public static void validate(Transition x,String activePolicy,String activeHash,long activeSequence,long previousThrough) {
        if(!Objects.equals(x.policy,activePolicy))throw new IllegalArgumentException("NEW_POLICY_REQUIRED");
        if(!Objects.equals(x.parent,activeHash)||x.sequence!=activeSequence+1)throw new IllegalArgumentException("STALE_CAS");
        if(x.next==null||x.next.isEmpty())throw new IllegalArgumentException("ARTIFACT_MISSING");
        if(x.trainedThrough>=x.target||x.trainedThrough>x.knownThrough)throw new IllegalArgumentException("FUTURE_LABEL");
        if(x.trainedThrough<=previousThrough)throw new IllegalArgumentException("CURSOR_NOT_ADVANCED");
    }
    public static double mass(double[] p,int[] n) {
        if(p.length!=10||n.length!=3)throw new IllegalArgumentException("K3_SHAPE");
        double total=0;for(double v:p){if(!Double.isFinite(v)||v<0)throw new IllegalArgumentException("PROB_INVALID");total+=v;}
        if(Math.abs(total-1)>1e-6)throw new IllegalArgumentException("PROB_SUM");
        Set<Integer> seen=new HashSet<>();double q=0;
        for(int k:n){if(k<1||k>10||!seen.add(k))throw new IllegalArgumentException("FIXED_K3");q+=p[k-1];}
        return q;
    }
}
