package com.ethk3.novaonepro.strategyv2;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

public final class R31FullFactoryProductionTest {
    @Test public void realFactoryCheckpointRestoresTheSearchAndNeverCompletesAnUnrunExperiment(){
        FeatureCacheR27.trimRegisteredCachesR31();int before=MemoryBudgetR31.processBudget().explicitOwnerLeaseCountR31();String saved;
        try(FactoryCapabilityExperimentR31 runner=new FactoryCapabilityExperimentR31("HOST_FACTORY_SOURCE")){
            runner.step();runner.step();saved=runner.snapshot();Map<?,?> state=(Map<?,?>)JsonR27.parse(saved);
            Map<?,?> engine=(Map<?,?>)JsonR27.parse((String)state.get("loopState"));assertFalse(engine.isEmpty());
            assertTrue("actual factory snapshot retains pending births",saved.contains("pending"));assertTrue(saved.contains("R27_POLICY"));
            assertEquals(0,runner.summary().get("completedTrials"));assertEquals("RUNNING_INCOMPLETE",runner.summary().get("status"));assertEquals(false,runner.summary().get("meetsRegisteredExperiment"));assertEquals(false,runner.summary().get("realPermission"));
        }
        try(FactoryCapabilityExperimentR31 restored=FactoryCapabilityExperimentR31.restore(saved)){
            assertEquals("HOST_FACTORY_SOURCE",restored.source());assertEquals(FactoryCapabilityExperimentR31.protocolHash(),((Map<?,?>)JsonR27.parse(restored.snapshot())).get("protocolHash"));
            assertEquals(120,restored.summary().get("plannedTrials"));assertEquals(0,restored.summary().get("completedTrials"));assertFalse(restored.complete());
        }
        FeatureCacheR27.trimRegisteredCachesR31();assertTrue(MemoryBudgetR31.processBudget().explicitOwnerLeaseCountR31()<=before);
    }
    @Test public void registeredProtocolKeepsAllOriginalFamilyAndSearchBudgets(){
        Map<String,Object> p=FactoryCapabilityExperimentR31.protocol();EvolutionLoopR27.Config original=new EvolutionLoopR27.Config();
        assertEquals(original.births,p.get("births"));assertEquals(original.minPerFamily,p.get("familyMinimum"));assertEquals(original.nullControl,p.get("recursiveNullControl"));assertEquals(EvolutionLoopR27.FAMILIES.length,((List<?>)p.get("factoryFamilies")).size());assertEquals(6,((List<?>)p.get("arms")).size());
        try{p.put("births",4);fail("registered protocol mutated");}catch(UnsupportedOperationException expected){}
    }
}
