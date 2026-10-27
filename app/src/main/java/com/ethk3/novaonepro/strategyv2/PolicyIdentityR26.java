package com.ethk3.novaonepro.strategyv2;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.*;

/** R26 typed byte protocol. The returned bytes, never a caller's map, own identity. */
public final class PolicyIdentityR26 {
    private PolicyIdentityR26() {}
    public static final Set<String> RULES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "graph","features","updater","update_schedule","initial_state","rng","gate","router",
        "calibration","missing_policy","resource_schedule","evidence_protocol","runtime_abi")));
    public static final class Policy {
        public final String canonical, hash;
        public Policy(Map<String,?> spec) {
            if (!spec.keySet().equals(RULES)) throw new IllegalArgumentException("R26_INCOMPLETE_POLICY");
            Map<String,Object> body=new HashMap<>();body.put("domain","STARCORE_POLICY_R26_V1");body.put("spec",spec);
            canonical=canonical(body);hash=sha256(canonical);
        }
    }
    public static String sha256(String text) {
        try { byte[] bytes=MessageDigest.getInstance("SHA-256").digest(text.getBytes(StandardCharsets.UTF_8));
            StringBuilder b=new StringBuilder();for(byte x:bytes)b.append(String.format(Locale.ROOT,"%02x",x&255));return b.toString();
        } catch(java.security.NoSuchAlgorithmException e){throw new AssertionError(e);}
    }
    public static String canonical(Object value) {StringBuilder b=new StringBuilder();typed(b,value);return b.toString();}
    private static void typed(StringBuilder b,Object v) {
        if(v==null){b.append("[\"null\"]");return;}
        if(v instanceof Boolean){b.append("[\"bool\",").append(v).append(']');return;}
        if(v instanceof Byte||v instanceof Short||v instanceof Integer||v instanceof Long||v instanceof java.math.BigInteger){b.append("[\"int\",");quote(b,v.toString());b.append(']');return;}
        if(v instanceof Double||v instanceof Float){double d=((Number)v).doubleValue();if(!Double.isFinite(d))throw new IllegalArgumentException("R26_NON_FINITE");
            b.append("[\"f64\",\"").append(String.format(Locale.ROOT,"%016x",Double.doubleToRawLongBits(d))).append("\"]");return;}
        if(v instanceof String){b.append("[\"str\",");quote(b,(String)v);b.append(']');return;}
        if(v instanceof Map){Map<?,?> m=(Map<?,?>)v;List<String> keys=new ArrayList<>();for(Object k:m.keySet()){if(!(k instanceof String))throw new IllegalArgumentException("R26_NON_STRING_KEY");validate((String)k);keys.add((String)k);}
            keys.sort((a,c)->{byte[] x=a.getBytes(StandardCharsets.UTF_8),y=c.getBytes(StandardCharsets.UTF_8);for(int i=0;i<Math.min(x.length,y.length);i++){int d=(x[i]&255)-(y[i]&255);if(d!=0)return d;}return x.length-y.length;});
            b.append("[\"map\",[");boolean first=true;for(String k:keys){if(!first)b.append(',');first=false;b.append('[');quote(b,k);b.append(',');typed(b,m.get(k));b.append(']');}b.append("]]");return;}
        if(v instanceof Iterable){b.append("[\"seq\",[");boolean first=true;for(Object x:(Iterable<?>)v){if(!first)b.append(',');first=false;typed(b,x);}b.append("]]");return;}
        throw new IllegalArgumentException("R26_UNSUPPORTED_TYPE:"+v.getClass().getName());
    }
    private static void validate(String s){for(int i=0;i<s.length();i++){char c=s.charAt(i);if(Character.isHighSurrogate(c)){if(++i>=s.length()||!Character.isLowSurrogate(s.charAt(i)))throw new IllegalArgumentException("R26_SURROGATE");}else if(Character.isLowSurrogate(c))throw new IllegalArgumentException("R26_SURROGATE");}}
    private static void quote(StringBuilder b,String s){validate(s);b.append('"');for(int i=0;i<s.length();i++){char c=s.charAt(i);switch(c){case '"':b.append("\\\"");break;case '\\':b.append("\\\\");break;case '\b':b.append("\\b");break;case '\f':b.append("\\f");break;case '\n':b.append("\\n");break;case '\r':b.append("\\r");break;case '\t':b.append("\\t");break;default:if(c<32)b.append(String.format(Locale.ROOT,"\\u%04x",(int)c));else b.append(c);}}b.append('"');}
}
