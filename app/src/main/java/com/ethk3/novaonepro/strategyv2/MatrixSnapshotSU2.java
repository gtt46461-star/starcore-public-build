package com.ethk3.novaonepro.strategyv2;

import java.io.*;
import java.util.*;

/** P46: learned factors and teachers frozen at a checkpoint; reason grades use later live decisions only. */
public final class MatrixSnapshotSU2 {
    private MatrixSnapshotSU2() {}
    public static final int MAX_BYTES = 262144;
    public static final class Factor {
        final int kind, a, b; final String name, family; final double[] weight;
        Factor(int kind, int a, int b, String name, double[] weight) {
            this.kind=kind;this.a=a;this.b=b;this.name=name;this.weight=weight.clone();
            int end=name.length();for(char ch:new char[]{'(','×','['}){int at=name.indexOf(ch);if(at>=0)end=Math.min(end,at);}family=name.substring(0,end);
        }
        double support(AtomStreamSU2 stream,int position,int mask) {
            float[][] values;
            if(kind==0)values=stream.atom(a);
            else if(kind==1)values=AtomStreamSU2.pair(stream.atom(a),stream.atom(b));
            else if(kind==2){values=stream.atom(a);float[] state=stream.state(b);float[][] copy=new float[10][10];for(int p=0;p<10;p++)for(int n=0;n<10;n++)copy[p][n]=values[p][n]*state[p];values=copy;}
            else{float[][][] group=stream.group();values=new float[10][10];for(int p=0;p<10;p++)for(int n=0;n<10;n++)values[p][n]=group[p][n][a];}
            double total=0;for(int n=0;n<10;n++)if((mask&(1<<n))!=0)total+=values[position-1][n];
            return total*weight[position-1]/3;
        }
    }
    public static final class Data {
        public final String json;public final byte[] factors,stream;
        Data(String json,List<Factor> factors,AtomStreamSU2 state){this.json=json;this.factors=encode(factors);this.stream=streamBytes(state);if(json.getBytes(java.nio.charset.StandardCharsets.UTF_8).length>MAX_BYTES)throw new IllegalArgumentException("SU2_MATRIX_JSON_BUDGET");}
    }
    static double[] weights(AtomNetSU2 net,int f){double[] out=new double[10];for(int p=0;p<10;p++)out[p]=net.u[f]+net.v[p*net.Fd+f];return out;}
    static List<Factor> factors(AtomNetSU2 net){
        List<Factor> out=new ArrayList<>();int f=0;
        for(int a:net.atoms)out.add(new Factor(0,a,0,AtomStreamSU2.CATALOG[a].name(),weights(net,f++)));
        for(int[] p:net.pairs)out.add(new Factor(1,p[0],p[1],AtomStreamSU2.CATALOG[p[0]].name()+"×"+AtomStreamSU2.CATALOG[p[1]].name(),weights(net,f++)));
        for(int[] p:net.mols)out.add(new Factor(2,p[0],p[1],AtomStreamSU2.CATALOG[p[0]].name()+"×"+AtomStreamSU2.STATES[p[1]].name(),weights(net,f++)));
        if(net.group)for(int g=0;g<AtomStreamSU2.N_GROUP;g++)out.add(new Factor(3,g,0,"GROUP["+g+"]",weights(net,f++)));
        return out;
    }
    public static Data capture(CommitteeSU2 committee,ModelReplaySU2.Library lib,long through){
        Map<String,Object> root=new LinkedHashMap<>();root.put("schema","SU2_MATRICES_V1");root.put("matrix_registry_id",MatrixRegistryR30.SU2_ID);root.put("genome_key",committee.key());root.put("checkpoint_sequence",through);root.put("formula",committee.formula());
        List<Factor> all=new ArrayList<>();List<Object> visible=new ArrayList<>(),teachers=new ArrayList<>();Set<String> seen=new HashSet<>();int[][] draws=lib.draws();
        Set<Integer> atoms=new LinkedHashSet<>();for(int a:ModelEvolutionSU2.defaultAtoms())atoms.add(a);
        for(ModelGenomeSU2 sub:committee.subs){
            for(ModelGenomeSU2.Member member:sub.members)if(member.kind.equals("ATOMNET")&&seen.add(member.modelKey())){
                AtomNetSU2 net=AtomNetSU2.of(member.params);float[][] q=new float[10][10];float[] st=new float[3];
                for(int[] d:draws){net.predict(q,st);net.update(d);}
                List<Factor> fs=factors(net);all.addAll(fs);for(int a:net.atoms)atoms.add(a);
                fs.sort((a,b)->Double.compare(importance(b.weight),importance(a.weight)));
                for(Factor f:fs.subList(0,Math.min(6,fs.size()))){Map<String,Object> row=new LinkedHashMap<>();row.put("model_key",member.modelKey());row.put("factor",f.name);row.put("mean_abs_weight",importance(f.weight));row.put("by_position",f.weight);visible.add(row);}
            }
            float[][][] P=ModelReplaySU2.pooledTable(sub,lib);Map<String,Object> teacher=new LinkedHashMap<>(),states=new LinkedHashMap<>();teacher.put("formula",sub.formula());
            for(int j:teacherStates()){
                float[][] S=lib.states(j);double[] sums=new double[2];long[] counts=new long[2];
                for(int t=1500;t<draws.length;t++)for(int p=0;p<10;p++)if(S[t][p]!=0){int sign=S[t][p]>0?0:1;sums[sign]+=Math.log(Math.max(1e-9,P[t][p][draws[t][p]-1]))-Math.log(.1);counts[sign]++;}
                Map<String,Object> row=new LinkedHashMap<>();row.put("high_mnat",counts[0]>0?1000*sums[0]/counts[0]:null);row.put("low_mnat",counts[1]>0?1000*sums[1]/counts[1]:null);row.put("high_n",counts[0]);row.put("low_n",counts[1]);states.put(AtomStreamSU2.STATES[j].name(),row);
            }
            teacher.put("gain_by_state",states);teachers.add(teacher);
        }
        root.put("factor_matrix",visible);root.put("teacher_matrix",teachers);root.put("reason_grades",Collections.emptyMap());root.put("reason_scope","WAIT_NATURAL_FROZEN_FORWARD; fixed checkpoint factors, observed associations");
        List<Integer> candidates=new ArrayList<>(atoms);if(candidates.size()>48)candidates=candidates.subList(0,48);
        ModelEvolutionSU2.Distilled explanation=ModelEvolutionSU2.distill(CommitteeSU2.table(committee,lib),draws,candidates,draws.length,8);
        Map<String,Object> exp=new LinkedHashMap<>();exp.put("r2",explanation.r2);List<Object> members=new ArrayList<>();for(int i=0;i<explanation.atoms.length;i++){Map<String,Object> row=new LinkedHashMap<>();row.put("atom",AtomStreamSU2.CATALOG[explanation.atoms[i]].name());row.put("weight",explanation.weights[i]);members.add(row);}exp.put("atoms",members);root.put("atomic_explanation",exp);
        AtomStreamSU2 state=new AtomStreamSU2();for(int[] d:draws)state.update(d);
        return new Data(json(root),all,state);
    }
    public static byte[] streamBytes(AtomStreamSU2 state){try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();state.write(new DataOutputStream(bytes));if(bytes.size()>MAX_BYTES)throw new IllegalArgumentException("SU2_MATRIX_STREAM_BUDGET");return bytes.toByteArray();}catch(IOException impossible){throw new IllegalStateException(impossible);}}
    static int[] teacherStates(){List<Integer> out=new ArrayList<>();String[] wanted={"ENTROPY(window=50)","DRIFT_JS(recent=20,reference=120)","REGIME_SCORE(regime=LOW_ENTROPY,window=120)","ROLLING_VARIANCE_DIGIT(window=30)","DIRECTION(lag=1)"};for(String name:wanted)for(int j=0;j<AtomStreamSU2.states();j++)if(AtomStreamSU2.STATES[j].name().equals(name)){out.add(j);break;}int[] ids=new int[out.size()];for(int i=0;i<ids.length;i++)ids[i]=out.get(i);return ids;}
    static double importance(double[] w){double sum=0;for(double v:w)sum+=Math.abs(v);return sum/w.length;}
    static byte[] encode(List<Factor> factors){try{ByteArrayOutputStream bytes=new ByteArrayOutputStream();DataOutputStream o=new DataOutputStream(bytes);o.writeInt(1);o.writeInt(factors.size());for(Factor f:factors){o.writeInt(f.kind);o.writeInt(f.a);o.writeInt(f.b);o.writeUTF(f.name);for(double w:f.weight)o.writeDouble(w);}o.flush();if(bytes.size()>MAX_BYTES)throw new IllegalArgumentException("SU2_MATRIX_FACTOR_BUDGET");return bytes.toByteArray();}catch(IOException impossible){throw new IllegalStateException(impossible);}}
    public static List<Factor> decode(byte[] bytes)throws IOException{if(bytes.length>MAX_BYTES)throw new IOException("SU2_MATRIX_FACTOR_BUDGET");DataInputStream in=new DataInputStream(new ByteArrayInputStream(bytes));if(in.readInt()!=1)throw new IOException("SU2_MATRIX_VERSION");int n=in.readInt();if(n<0||n>1200)throw new IOException("SU2_MATRIX_FACTOR_COUNT");List<Factor> out=new ArrayList<>();for(int i=0;i<n;i++){int k=in.readInt(),a=in.readInt(),b=in.readInt();String name=in.readUTF();double[] w=new double[10];for(int p=0;p<10;p++)w[p]=in.readDouble();out.add(new Factor(k,a,b,name,w));}if(in.available()!=0)throw new IOException("SU2_MATRIX_TRAILING_BYTES");return out;}
    public static Map<String,Double> support(List<Factor> factors,AtomStreamSU2 stream,int position,int mask){Map<String,Double> out=new TreeMap<>();for(Factor f:factors)out.put(f.family,out.getOrDefault(f.family,0.0)+f.support(stream,position,mask));return out;}
    public static String grade(long supported,long supportedHits,long other,long otherHits){double lift=supported>0&&other>0?supportedHits/(double)supported-otherHits/(double)other:0;double lower=wilson(supportedHits,supported);return lift>0&&lower>.30&&supported>=100?"A_VALID":lift>.01?"B_USEFUL":lift<-.02?"E_FALSE":"C_EXPLAIN_ONLY";}
    static double wilson(long hits,long n){if(n==0)return 0;double z=1.6449,p=hits/(double)n;return(p+z*z/(2*n)-z*Math.sqrt(p*(1-p)/n+z*z/(4.0*n*n)))/(1+z*z/n);}
    static String quote(String s){StringBuilder out=new StringBuilder("\"");for(int i=0;i<s.length();i++){char c=s.charAt(i);if(c=='"'||c=='\\')out.append('\\').append(c);else if(c<' ')out.append(String.format(Locale.ROOT,"\\u%04x",(int)c));else out.append(c);}return out.append('"').toString();}
    static String json(Object value){if(value==null)return"null";if(value instanceof String)return quote((String)value);if(value instanceof Number){double d=((Number)value).doubleValue();return Double.isNaN(d)||Double.isInfinite(d)?"null":value.toString();}if(value instanceof double[]){List<Double> a=new ArrayList<>();for(double x:(double[])value)a.add(x);return json(a);}if(value instanceof Map){StringBuilder out=new StringBuilder("{");for(Map.Entry<?,?> e:((Map<?,?>)value).entrySet()){if(out.length()>1)out.append(',');out.append(quote(String.valueOf(e.getKey()))).append(':').append(json(e.getValue()));}return out.append('}').toString();}if(value instanceof Iterable){StringBuilder out=new StringBuilder("[");for(Object x:(Iterable<?>)value){if(out.length()>1)out.append(',');out.append(json(x));}return out.append(']').toString();}throw new IllegalArgumentException("SU2_MATRIX_JSON_TYPE");}
}
