package com.ethk3.novaonepro.strategyv2;
import java.util.*;
import java.io.*;

/** Dataset/lineage-owned macros. Independent control experiments construct independent instances. */
public final class MacroLibrarySU1 {
    public static final class Macro {public final String id,text;public int support;public double fitness;Macro(String text,int support,double fitness){this.text=text;this.support=support;this.fitness=fitness;id="M"+HashingSU1.text("SU1_MACRO_V1|"+text).substring(0,10);}}
    final LinkedHashMap<String,Macro> macros=new LinkedHashMap<>();
    public List<Macro> all(){return Collections.unmodifiableList(new ArrayList<>(macros.values()));}
    public void learn(List<StrategyFoundrySU1.Ranked> ranked){
        Map<String,Integer> support=new TreeMap<>();Map<String,Double> fit=new HashMap<>();
        for(int i=0;i<Math.min(12,ranked.size());i++){StrategyFoundrySU1.Ranked r=ranked.get(i);Set<String> seen=new HashSet<>();for(ProgramTextSU1.Term t:ProgramTextSU1.subtrees(ProgramTextSU1.parse(r.genome.text),false))if(t.size()>=2&&t.size()<=7&&!ProgramTextSU1.constant(t))seen.add(t.toString());for(String text:seen){support.put(text,support.getOrDefault(text,0)+1);fit.put(text,Math.max(fit.getOrDefault(text,-Double.MAX_VALUE),r.fitness));}}
        for(Map.Entry<String,Integer> e:support.entrySet())if(e.getValue()>=2){Macro m=macros.get(e.getKey());if(m==null)macros.put(e.getKey(),new Macro(e.getKey(),e.getValue(),fit.get(e.getKey())));else{m.support=Math.max(m.support,e.getValue());m.fitness=Math.max(m.fitness,fit.get(e.getKey()));}}
        List<Macro> all=new ArrayList<>(macros.values());all.sort(Comparator.comparingInt((Macro m)->m.support).reversed().thenComparing(Comparator.comparingDouble((Macro m)->m.fitness).reversed()).thenComparing(m->m.id));macros.clear();for(int i=0;i<Math.min(40,all.size());i++)macros.put(all.get(i).text,all.get(i));
    }
    public String textForArm(String arm){for(Macro m:macros.values())if(("EMERGENT:"+m.id).equals(arm))return m.text;return null;}
    public void write(DataOutput out)throws IOException{out.writeInt(macros.size());for(Macro m:macros.values()){out.writeUTF(m.text);out.writeInt(m.support);out.writeDouble(m.fitness);}}
    public static MacroLibrarySU1 read(DataInput in)throws IOException{MacroLibrarySU1 lib=new MacroLibrarySU1();int n=in.readInt();if(n<0||n>40)throw new IOException("SU1_MACRO_CAP");for(int i=0;i<n;i++){String text=ProgramTextSU1.canonical(in.readUTF());int support=in.readInt();double f=in.readDouble();if(support<2||!Double.isFinite(f))throw new IOException("SU1_MACRO_STATE");lib.macros.put(text,new Macro(text,support,f));}return lib;}
}
