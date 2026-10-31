package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;

public class R30CapabilityRegistryTest {
    @Test public void everyActualObjectHasOneRowAndDependenciesArePreserved(){
        List<CapabilityRegistryR30.Definition> rows=CapabilityRegistryR30.all();assertEquals(82,rows.size());Set<String> ids=new HashSet<>();Map<String,Integer> groups=new HashMap<>();
        for(CapabilityRegistryR30.Definition d:rows){assertTrue(ids.add(d.id));assertTrue(d.id+":"+d.implementationClass,d.implemented);groups.put(d.kind,groups.getOrDefault(d.kind,0)+1);if(d.id.equals("F23"))assertEquals(FamilyRegistryR26.EXECUTABLE.contains("F23")&&!FamilyRegistryR26.implementation("F23").isEmpty()&&ModelReplaySU2.ADAPTERS.containsKey("R26F23"),d.executable);}
        assertEquals(Integer.valueOf(24),groups.get("FAMILY"));assertEquals(Integer.valueOf(16),groups.get("EXPERT"));assertEquals(Integer.valueOf(21),groups.get("IDEA"));assertEquals(Integer.valueOf(1),groups.get("MODEL"));assertEquals(Integer.valueOf(20),groups.get("MATRIX"));
    }
    @Test public void auditExpertsAreReceiptsAndI00RemainsControlOnly(){
        Map<String,CapabilityRegistryR30.Definition> rows=new HashMap<>();for(CapabilityRegistryR30.Definition d:CapabilityRegistryR30.all())rows.put(d.id,d);
        for(String id:new String[]{"E01","E14","E15","E16"})assertEquals("AUDIT_RECEIPT",rows.get(id).outputMode);
        assertEquals("CONTROL_ONLY_POLICY",rows.get("I00").outputMode);assertEquals("TEN_POSITION_LEARNING_AND_NUMBER_SURFACE",rows.get("ATOMNET").outputMode);
    }
}
