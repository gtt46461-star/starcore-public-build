package com.ethk3.novaonepro;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/** Registry of PIT-safe compact primitives. No post-draw label can enter a pre-draw feature matrix. */
public final class ResearchFeatureGrammarR906 {
    private static final Set<String> SAFE=new HashSet<String>(Arrays.asList(
            "raw_action_p","rank_score","third_margin","entropy_risk","stability","window_consensus","assignment_support",
            "selected_hit_p_prior","short_long_residual","cross_position_migration","position_transition_lift","motif_similarity",
            "regime_change_risk","expert_disagreement","calibration_residual_prior","rank_regret_prior","frequency_slope","gap_pressure"));
    private ResearchFeatureGrammarR906(){}
    public static boolean allowed(String primitive){return SAFE.contains(norm(primitive));}
    public static void requireAllowed(String expression){String x=norm(expression);String[] parts=x.split("[+*(),:/\\-]");for(String p:parts){String t=p.trim();if(t.length()==0||isNumber(t)||isOperatorWord(t))continue;if(!allowed(t))throw new IllegalArgumentException("R906_FEATURE_NOT_REGISTERED:"+t);}}
    public static String schemaHash(){java.util.ArrayList<String>x=new java.util.ArrayList<String>(SAFE);java.util.Collections.sort(x);return V51ChecksumEngine.sha256("R906_FEATURE_GRAMMAR|"+x.toString());}
    private static boolean isNumber(String x){try{Double.parseDouble(x);return true;}catch(Throwable e){return false;}}
    private static boolean isOperatorWord(String x){return "min".equals(x)||"max".equals(x)||"abs".equals(x)||"clip".equals(x)||"sigmoid".equals(x);}
    private static String norm(String x){return x==null?"":x.trim().toLowerCase(java.util.Locale.US);}
}
