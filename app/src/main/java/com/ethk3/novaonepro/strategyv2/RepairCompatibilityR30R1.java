package com.ethk3.novaonepro.strategyv2;
import java.util.*;
/** Additive namespaces keep old APK cursors and parsers usable. No certificate authority is created. */
public final class RepairCompatibilityR30R1 {
 private RepairCompatibilityR30R1(){}public static final String ACTIVE="ACTIVE_REPAIR1",OBSERVE="OBSERVE_REPAIR1",OBSERVE_RETIRED="OBSERVE_REPAIR1_RETIRED";
 public static boolean active(String status){return "ACTIVE".equals(status)||ACTIVE.equals(status);}
 public static boolean requiresNewParser(PolicySpecR27 p){for(PolicyAstR27.Node n:PolicyAstR27.nodes(p.ast))if(n.op.startsWith("set_intersection"))return true;return false;}
 public static String activeStatus(PolicySpecR27 p){return requiresNewParser(p)?ACTIVE:"ACTIVE";}
 public static String observerStatus(PolicySpecR27 p){return requiresNewParser(p)?OBSERVE:"OBSERVE";}
 /** Preserve the original JSON separately at the caller before using this explicit development-boundary recovery. */
 @SuppressWarnings("unchecked") public static EvolutionLoopR27 restoreBoundary(String original,EvolutionLoopR27.Config cfg,FeatureCacheR27 cache,EvolutionLoopR27.Sink sink,EvolutionLoopR27.Clock clock){Map<String,Object> s=(Map<String,Object>)JsonR27.parse(original);if(cache.data.datasetHash.equals(s.get("datasetHash")))throw new IllegalArgumentException("BOUNDARY_RECOVERY_REQUIRES_CHANGED_INPUT");s.put("step",1L);s.put("generation",((Number)s.get("generation")).longValue()+1);s.put("pending",Collections.emptyList());s.remove("frozenConfirmation");s.put("confirmationDispatched",false);return EvolutionLoopR27.restore(JsonR27.write(s),cfg,cache,sink,clock);}
}
