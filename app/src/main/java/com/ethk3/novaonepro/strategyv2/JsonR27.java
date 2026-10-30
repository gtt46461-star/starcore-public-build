package com.ethk3.novaonepro.strategyv2;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

/** Minimal canonical JSON for R27 (sorted keys, lossless doubles, no whitespace). org.json is not used because
 * host unit tests run against Android stubs. Values: Map, List, String, Number, Boolean, null. */
public final class JsonR27 {
    private JsonR27() {}

    public static String write(Object v) { StringBuilder b = new StringBuilder(); write(v, b); return b.toString(); }

    /** Explicit pure-compute variant; legacy callers can serialize while holding their own locks. */
    static String writeCooperative(Object v) { StringBuilder b = new StringBuilder(); write(v, b, true); return b.toString(); }

    @SuppressWarnings("unchecked")
    static void write(Object v, StringBuilder b) {
        write(v,b,false);
    }
    @SuppressWarnings("unchecked")
    private static void write(Object v,StringBuilder b,boolean cooperative) {
        if(cooperative)CooperativeContinuationR26.poll();
        if (v == null) b.append("null");
        else if (v instanceof String) str((String) v, b,cooperative);
        else if (v instanceof Boolean) b.append(v);
        else if (v instanceof Integer || v instanceof Long) b.append(v);
        else if (v instanceof Number) { double d = ((Number) v).doubleValue(); if (!Double.isFinite(d)) throw new IllegalArgumentException("R27_JSON_NONFINITE"); if (d == Math.rint(d) && Math.abs(d) < 1e15) b.append((long) d).append(".0"); else b.append(Double.toString(d)); }
        else if (v instanceof Map) {
            TreeMap<String,Object> m;
            if(cooperative){m=new TreeMap<>();for(Map.Entry<String,Object> entry:((Map<String,Object>)v).entrySet()){CooperativeContinuationR26.poll();m.put(entry.getKey(),entry.getValue());}}
            else m=new TreeMap<>((Map<String,Object>)v);
            b.append('{'); boolean first = true;
            for (Map.Entry<String, Object> e : m.entrySet()) { if(cooperative)CooperativeContinuationR26.poll(); if (!first) b.append(','); first = false; str(e.getKey(), b,cooperative); b.append(':'); write(e.getValue(), b,cooperative); }
            b.append('}');
        } else if (v instanceof List) {
            b.append('['); boolean first = true;
            for (Object o : (List<Object>) v) { if (!first) b.append(','); first = false; write(o, b,cooperative); }
            b.append(']');
        } else if (v instanceof double[]) { List<Object> l = new ArrayList<>(); for (double d : (double[]) v) { if(cooperative)CooperativeContinuationR26.poll(); l.add(d); } write(l, b,cooperative); }
        else if (v instanceof int[]) { List<Object> l = new ArrayList<>(); for (int d : (int[]) v) { if(cooperative)CooperativeContinuationR26.poll(); l.add(d); } write(l, b,cooperative); }
        else throw new IllegalArgumentException("R27_JSON_TYPE:" + v.getClass());
    }

    static void str(String s, StringBuilder b) {
        str(s,b,false);
    }
    private static void str(String s,StringBuilder b,boolean cooperative) {
        b.append('"');
        for (int i = 0; i < s.length(); i++) {
            if(cooperative&&(i&1023)==0)CooperativeContinuationR26.poll();
            char c = s.charAt(i);
            switch (c) {
                case '"': b.append("\\\""); break; case '\\': b.append("\\\\"); break;
                case '\n': b.append("\\n"); break; case '\r': b.append("\\r"); break; case '\t': b.append("\\t"); break;
                default: if (c < 0x20) b.append(String.format(java.util.Locale.ROOT, "\\u%04x", (int) c)); else b.append(c);
            }
        }
        b.append('"');
    }

    public static Object parse(String s) { int[] at = {0}; Object v = value(s, at); ws(s, at); if (at[0] != s.length()) throw new IllegalArgumentException("R27_JSON_TRAILING"); return v; }

    static Object parseCooperative(String s) { int[] at={0};Object v=value(s,at,true);ws(s,at);if(at[0]!=s.length())throw new IllegalArgumentException("R27_JSON_TRAILING");return v; }

    private static void ws(String s, int[] at) { while (at[0] < s.length() && Character.isWhitespace(s.charAt(at[0]))) at[0]++; }
    private static Object value(String s, int[] at) {
        return value(s,at,false);
    }
    private static Object value(String s,int[] at,boolean cooperative) {
        if(cooperative)CooperativeContinuationR26.poll();
        ws(s, at);
        if (at[0] >= s.length()) throw new IllegalArgumentException("R27_JSON_EOF");
        char c = s.charAt(at[0]);
        if (c == '{') {
            at[0]++; Map<String, Object> m = new TreeMap<>(); ws(s, at);
            if (s.charAt(at[0]) == '}') { at[0]++; return m; }
            while (true) {
                ws(s, at); String k = (String) value(s, at,cooperative); ws(s, at);
                if (s.charAt(at[0]++) != ':') throw new IllegalArgumentException("R27_JSON_COLON");
                m.put(k, value(s, at,cooperative)); ws(s, at);
                char d = s.charAt(at[0]++); if (d == '}') return m; if (d != ',') throw new IllegalArgumentException("R27_JSON_OBJECT");
            }
        }
        if (c == '[') {
            at[0]++; List<Object> l = new ArrayList<>(); ws(s, at);
            if (s.charAt(at[0]) == ']') { at[0]++; return l; }
            while (true) {
                l.add(value(s, at,cooperative)); ws(s, at);
                char d = s.charAt(at[0]++); if (d == ']') return l; if (d != ',') throw new IllegalArgumentException("R27_JSON_ARRAY");
            }
        }
        if (c == '"') {
            StringBuilder b = new StringBuilder(); at[0]++;
            while (true) {
                if(cooperative&&(at[0]&1023)==0)CooperativeContinuationR26.poll();
                char d = s.charAt(at[0]++);
                if (d == '"') return b.toString();
                if (d == '\\') {
                    char e = s.charAt(at[0]++);
                    switch (e) { case 'n': b.append('\n'); break; case 'r': b.append('\r'); break; case 't': b.append('\t'); break;
                        case 'u': b.append((char) Integer.parseInt(s.substring(at[0], at[0] + 4), 16)); at[0] += 4; break; default: b.append(e); }
                } else b.append(d);
            }
        }
        if (s.startsWith("true", at[0])) { at[0] += 4; return Boolean.TRUE; }
        if (s.startsWith("false", at[0])) { at[0] += 5; return Boolean.FALSE; }
        if (s.startsWith("null", at[0])) { at[0] += 4; return null; }
        int st = at[0];
        while (at[0] < s.length() && "+-0123456789.eE".indexOf(s.charAt(at[0])) >= 0) at[0]++;
        String num = s.substring(st, at[0]);
        if (num.isEmpty()) throw new IllegalArgumentException("R27_JSON_VALUE@" + st);
        if (num.indexOf('.') < 0 && num.indexOf('e') < 0 && num.indexOf('E') < 0) return Long.parseLong(num);
        return Double.parseDouble(num);
    }

    public static double num(Object o) { return ((Number) o).doubleValue(); }
}
