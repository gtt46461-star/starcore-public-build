package com.ethk3.novaonepro.r952;

import java.nio.charset.StandardCharsets;
import java.util.*;

/** Bounded, duplicate-rejecting JSON. AST floating point values are hex strings.
 * Atom catalog decimals use the separate readAtomObject/writeAtom entry points.
 * This class has no Android, database, clock or reflection dependencies. */
public final class StrictCanonicalJson {
    private StrictCanonicalJson() {}
    private static final int MAX_BYTES = 32768, MAX_DEPTH = 32, MAX_ITEMS = 8192;
    private static final Comparator<String> CODE_POINT_ORDER = (a,b) -> {
        int i=0,j=0;
        while(i<a.length() && j<b.length()) {
            int x=a.codePointAt(i),y=b.codePointAt(j);
            if(x!=y)return Integer.compare(x,y);
            i+=Character.charCount(x);j+=Character.charCount(y);
        }
        return Integer.compare(a.length()-i,b.length()-j);
    };
    /** Only the two exact decimal tokens used by the versioned Atom catalog. */
    public static final class AtomDecimal {
        private final String token;
        private AtomDecimal(String token) { this.token=token; }
        public double doubleValue() { return Double.parseDouble(token); }
        @Override public String toString() { return token; }
    }
    public static Map<String,Object> readObject(String text) { return read(text,false,MAX_BYTES); }
    public static Map<String,Object> readAtomObject(String text) { return read(text,true,MAX_BYTES); }
    public static Map<String,Object> readContextObject(String text) { return read(text,false,262144); }
    public static String writeContext(Object value) { return write(value,false,262144); }
    @SuppressWarnings("unchecked")
    private static Map<String,Object> read(String text,boolean atoms,int byteLimit) {
        checkString(text,byteLimit);
        if(text.getBytes(StandardCharsets.UTF_8).length>byteLimit)throw bad("JSON_SIZE");
        Parser p=new Parser(text,atoms,byteLimit);Object o=p.value(0);p.space();
        if(p.i!=text.length() || !(o instanceof Map))throw bad("JSON_OBJECT_OR_TAIL");
        return (Map<String,Object>)o;
    }
    public static String write(Object value) { return write(value,false,MAX_BYTES); }
    public static String writeAtom(Object value) { return write(value,true,MAX_BYTES); }
    private static String write(Object value,boolean atoms,int byteLimit) {
        StringBuilder out=new StringBuilder();int[] items={0};emit(value,out,0,items,atoms,byteLimit);
        String text=out.toString();
        if(text.getBytes(StandardCharsets.UTF_8).length>byteLimit)throw bad("JSON_SIZE");
        return text;
    }
    private static IllegalArgumentException bad(String code) { return new IllegalArgumentException(code); }
    private static void checkString(String s,int byteLimit) {
        if(s==null)throw bad("JSON_NULL_STRING");
        if(s.length()>byteLimit)throw bad("JSON_SIZE");
        for(int i=0;i<s.length();i++) {
            char c=s.charAt(i);
            if(Character.isHighSurrogate(c)) {
                if(++i>=s.length() || !Character.isLowSurrogate(s.charAt(i)))throw bad("JSON_SURROGATE");
            } else if(Character.isLowSurrogate(c))throw bad("JSON_SURROGATE");
        }
    }
    private static void quoted(String s,StringBuilder out,int byteLimit) {
        checkString(s,byteLimit);out.append('"');
        for(int i=0;i<s.length();i++) {
            char c=s.charAt(i);
            switch(c) {
                case '"':out.append("\\\"");break;
                case '\\':out.append("\\\\");break;
                case '\b':out.append("\\b");break;
                case '\f':out.append("\\f");break;
                case '\n':out.append("\\n");break;
                case '\r':out.append("\\r");break;
                case '\t':out.append("\\t");break;
                default:
                    if(c<32) {out.append("\\u00");out.append("0123456789abcdef".charAt(c>>>4));out.append("0123456789abcdef".charAt(c&15));}
                    else out.append(c);
            }
            if(out.length()>byteLimit)throw bad("JSON_SIZE");
        }
        out.append('"');
    }
    private static void emit(Object o,StringBuilder out,int depth,int[] items,boolean atoms,int byteLimit) {
        if(depth>MAX_DEPTH || ++items[0]>MAX_ITEMS)throw bad("JSON_COMPLEXITY");
        if(o==null)out.append("null");
        else if(o instanceof String)quoted((String)o,out,byteLimit);
        else if(o instanceof Boolean)out.append(o);
        else if(o instanceof Byte || o instanceof Short || o instanceof Integer || o instanceof Long)out.append(o);
        else if(atoms && o instanceof AtomDecimal)out.append(o);
        else if(o instanceof List) {
            out.append('[');boolean first=true;
            for(Object v:(List<?>)o){if(!first)out.append(',');first=false;emit(v,out,depth+1,items,atoms,byteLimit);}out.append(']');
        } else if(o instanceof Map) {
            Map<?,?> map=(Map<?,?>)o;ArrayList<String> keys=new ArrayList<>();
            for(Object key:map.keySet()){if(!(key instanceof String))throw bad("JSON_KEY");checkString((String)key,byteLimit);keys.add((String)key);}
            keys.sort(CODE_POINT_ORDER);out.append('{');boolean first=true;
            for(String key:keys){if(!first)out.append(',');first=false;quoted(key,out,byteLimit);out.append(':');emit(map.get(key),out,depth+1,items,atoms,byteLimit);}out.append('}');
        } else throw bad("JSON_TYPE");
        if(out.length()>byteLimit)throw bad("JSON_SIZE");
    }
    private static final class Parser {
        final String s;final boolean atoms;final int byteLimit;int i,items;
        Parser(String s,boolean atoms,int byteLimit){this.s=s;this.atoms=atoms;this.byteLimit=byteLimit;}
        void space(){while(i<s.length() && (s.charAt(i)==' '||s.charAt(i)=='\n'||s.charAt(i)=='\r'||s.charAt(i)=='\t'))i++;}
        boolean take(char c){space();if(i<s.length()&&s.charAt(i)==c){i++;return true;}return false;}
        void need(char c){if(!take(c))throw bad("JSON_TOKEN");}
        Object value(int depth) {
            if(depth>MAX_DEPTH || ++items>MAX_ITEMS)throw bad("JSON_COMPLEXITY");space();
            if(i==s.length())throw bad("JSON_EOF");char c=s.charAt(i);
            if(c=='{') {
                i++;Map<String,Object> map=new TreeMap<>(CODE_POINT_ORDER);
                if(!take('}'))do {space();if(i==s.length()||s.charAt(i)!='"')throw bad("JSON_KEY");
                    String key=string();if(map.containsKey(key))throw bad("JSON_DUPLICATE_KEY");need(':');map.put(key,value(depth+1));
                    if(take('}'))return Collections.unmodifiableMap(map);need(',');
                }while(true);
                return Collections.unmodifiableMap(map);
            }
            if(c=='[') {
                i++;ArrayList<Object> list=new ArrayList<>();
                if(!take(']'))do {list.add(value(depth+1));if(take(']'))return Collections.unmodifiableList(list);need(',');}while(true);
                return Collections.unmodifiableList(list);
            }
            if(c=='"')return string();
            if(s.startsWith("true",i)){i+=4;return Boolean.TRUE;}
            if(s.startsWith("false",i)){i+=5;return Boolean.FALSE;}
            if(s.startsWith("null",i)){i+=4;return null;}
            int begin=i;if(c=='-')i++;
            if(i==s.length()||s.charAt(i)<'0'||s.charAt(i)>'9')throw bad("JSON_NUMBER");
            if(s.charAt(i)=='0')i++;else while(i<s.length()&&s.charAt(i)>='0'&&s.charAt(i)<='9')i++;
            if(atoms && i<s.length()&&s.charAt(i)=='.') {
                i++;while(i<s.length()&&s.charAt(i)>='0'&&s.charAt(i)<='9')i++;
                String number=s.substring(begin,i);
                if(!number.equals("0.5")&&!number.equals("1.0"))throw bad("ATOM_DECIMAL");
                return new AtomDecimal(number);
            }
            String number=s.substring(begin,i);if(number.equals("-0"))throw bad("JSON_NEGATIVE_ZERO");
            try{return Long.valueOf(number);}catch(NumberFormatException e){throw bad("JSON_INTEGER_RANGE");}
        }
        String string() {
            need('"');StringBuilder b=new StringBuilder();boolean done=false;
            while(i<s.length()) {
                char c=s.charAt(i++);if(c=='"'){done=true;break;}if(c<32)throw bad("JSON_CONTROL");
                if(c!='\\'){b.append(c);continue;}
                if(i==s.length())throw bad("JSON_ESCAPE");c=s.charAt(i++);
                switch(c) {
                    case '"':case '\\':case '/':b.append(c);break;
                    case 'b':b.append('\b');break;case 'f':b.append('\f');break;case 'n':b.append('\n');break;
                    case 'r':b.append('\r');break;case 't':b.append('\t');break;
                    case 'u':
                        if(i+4>s.length())throw bad("JSON_ESCAPE");int n=0;
                        for(int k=0;k<4;k++){int v=Character.digit(s.charAt(i++),16);if(v<0)throw bad("JSON_ESCAPE");n=(n<<4)|v;}b.append((char)n);break;
                    default:throw bad("JSON_ESCAPE");
                }
            }
            if(!done)throw bad("JSON_STRING_EOF");String text=b.toString();checkString(text,byteLimit);return text;
        }
    }
}
