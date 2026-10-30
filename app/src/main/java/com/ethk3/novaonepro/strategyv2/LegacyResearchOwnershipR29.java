package com.ethk3.novaonepro.strategyv2;

import com.ethk3.novaonepro.SchemaDomainCoordinatorR926;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

/** Table ownership for the existing research engines. A retained Root table is a compatibility copy,
 * not a second training authority. Explicit realtime authorities take precedence over old domain stamps.
 * Research-future observations stay research evidence; only the original mainline binding/decision owner
 * can authorize REAL. Published model projections do not move that responsibility into a training task. */
public final class LegacyResearchOwnershipR29 {
    private LegacyResearchOwnershipR29(){}
    private static final Set<String> REALTIME=Collections.unmodifiableSet(new HashSet<>(Arrays.asList(
        "r26_feedback","r26_frozen","r26_incumbent_frozen",
        "su1_source_clock","su1_mainline_binding",
        "su2_live_decision","su2_mainline_choice","su2_mainline_binding"
    )));
    private static final Set<String> CRITICAL=Collections.unmodifiableSet(new HashSet<>(
        Arrays.asList(SchemaDomainCoordinatorR926.criticalContract().objects)));
    private static final Set<String> HISTORICAL=historical();
    private static Set<String> historical(){
        Set<String> out=new HashSet<>(Arrays.asList(SchemaDomainCoordinatorR926.researchContract().objects));
        out.addAll(Arrays.asList(SchemaDomainCoordinatorR926.legacyContract().objects));
        out.removeAll(CRITICAL);out.removeAll(REALTIME);return Collections.unmodifiableSet(out);
    }
    public static boolean realtime(String table){return table!=null&&(REALTIME.contains(table)||CRITICAL.contains(table));}
    public static boolean owned(String table){
        if(table==null||realtime(table))return false;
        return table.startsWith("r26_")||table.startsWith("su1_")||table.startsWith("su2_")
            ||table.startsWith("research_")||table.matches("r9(?:5[28]|6[789]|7[0-9])_[a-z0-9_]+")
            ||HISTORICAL.contains(table);
    }
    public static Set<String> explicitRealtimeAuthorities(){return REALTIME;}
}
