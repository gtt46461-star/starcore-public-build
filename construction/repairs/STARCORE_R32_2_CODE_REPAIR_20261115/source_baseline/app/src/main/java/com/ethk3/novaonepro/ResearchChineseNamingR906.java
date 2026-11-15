package com.ethk3.novaonepro;
/** Deterministic Chinese semantic naming; a different machine id never disguises a duplicate method signature. */
public final class ResearchChineseNamingR906 {
    private static final String[] SERIES={"星织","天枢","玄衡","流光","曜矩","辰链"};
    private ResearchChineseNamingR906(){}
    public static String name(ResearchMethodPlanR906 p){
        String sig=ResearchMethodSignatureR906.of(p);int k=sig.hashCode();
        k=k==Integer.MIN_VALUE?0:Math.abs(k);
        String rep=word(p.representation,"跨窗残差"), learner=word(p.learner,"稳健排序");
        String semantic=semanticFromFeature(p.feature);
        String core = semantic.length()>0 ? semantic+"·"+structure(p.representation) : rep+"-"+learner;
        String name=SERIES[k%SERIES.length]+"·"+core+"·第"+two(p.generation)+"代";
        if(name.length()>40)name=name.substring(0,36)+"·"+Integer.toHexString(k).substring(0,Math.min(3,Integer.toHexString(k).length()));
        ResearchAssetContractR906.requireChineseName(name);return name;
    }
    private static String semanticFromFeature(String f){String s=f==null?"":f.toLowerCase(java.util.Locale.US);StringBuilder b=new StringBuilder();if(s.contains("position_transition_lift"))b.append("位转");if(s.contains("cross_position_migration"))b.append("迁移");if(s.contains("motif_similarity"))b.append("母题");if(s.contains("short_long_residual"))b.append("残差");if(s.contains("gap_pressure"))b.append("遗漏");if(s.contains("frequency_slope"))b.append("斜率");return b.toString();}
    private static String structure(String x){String s=x==null?"":x.toUpperCase(java.util.Locale.US);if(s.contains("REGIME_SWITCH"))return "变局分流";if(s.contains("INTERACTION"))return "交互合成";if(s.contains("BOTTLENECK"))return "短板共识";if(s.contains("CONSENSUS"))return "稳态共识";if(s.contains("CONTRADICTION"))return "反证抑制";if(s.contains("CASCADE"))return "级联筛选";if(s.contains("MAX_SUPPORT"))return "强证据择优";if(s.contains("WEIGHTED"))return "走前加权";return "结构合成";}
    public static String normalized(String x){return x==null?"":x.replace(" ","").replace("　","").trim();}
    private static String word(String x,String fallback){String s=x==null?"":x.toUpperCase(java.util.Locale.US);if(s.contains("BAYES")||s.contains("MARKOV"))return "贝叶斯转移";if(s.contains("RETRIEVAL")||s.contains("KNN"))return "度量检索";if(s.contains("COMPACT_FOREST")||s.contains("OBLIVIOUS_FOREST"))return "紧凑森林";if(s.contains("PERMUTATION")||s.contains("GRAPH"))return "排列迁移图";if(s.contains("RESIDUAL")||s.contains("TRANSPORT"))return "跨窗残差输运";if(s.contains("EVIDENCE")||s.contains("WEAVE"))return "低熵证据编织";if(s.contains("REGIME")||s.contains("CHANGE"))return "贝叶斯变点回退";if(s.contains("MOTIF"))return "序列母题迁移";if(s.contains("FOREST"))return "在线森林";if(s.contains("NEURAL"))return "时序神经";if(s.contains("MOE"))return "多专家分治";if(s.contains("FTRL"))return "自适应线性";return fallback;}
    private static String two(int x){return x<10?"0"+x:String.valueOf(x);}
}
