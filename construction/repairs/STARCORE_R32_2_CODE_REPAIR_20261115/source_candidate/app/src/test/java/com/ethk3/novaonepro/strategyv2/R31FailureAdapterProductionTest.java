package com.ethk3.novaonepro.strategyv2;
import java.util.*;
import org.junit.Test;
import static org.junit.Assert.*;

/** The reason-specific adapter called by the real failure selector produces an
 * executable new model, or an explicit unsupported result. No quality claim. */
public final class R31FailureAdapterProductionTest {
    static ResearchHypothesisR31.Hypothesis hypothesis(ModelGenomeSU2 parent,ResearchHypothesisR31.Mechanism mechanism){return ResearchHypothesisR31.fromFailure(new ResearchHypothesisR31.Failure(parent.key(),StatsR27.sha256("failed-look"),mechanism,mechanism.name(),"","REVEALED_FAILED_LOOK",1000,999,500,.2));}
    @Test public void calibrationAndCoverageChangeTheirRealConsumerBeforeBirth(){
        ModelGenomeSU2 parent=FamilyRegistryR26.genome("F01");
        ModelGenomeSU2 calibration=FailureDirectedResearchR26.routeR31(parent,hypothesis(parent,ResearchHypothesisR31.Mechanism.CALIBRATION)).get(0).genome;
        assertTrue(calibration.calibrated);assertFalse(parent.calibrated);assertNotEquals(parent.key(),calibration.key());assertEquals(parent.members.get(0).modelKey(),calibration.members.get(0).modelKey());
        ModelGenomeSU2 coverage=FailureDirectedResearchR26.routeR31(parent,hypothesis(parent,ResearchHypothesisR31.Mechanism.COVERAGE)).get(0).genome;
        assertTrue(coverage.coverage>parent.coverage);assertEquals(parent.members.get(0).modelKey(),coverage.members.get(0).modelKey());
    }
    @Test public void measuredRegimeWindowIsConsumedByTheActualBocpdModel(){
        ModelGenomeSU2 parent=FamilyRegistryR26.genome("F01");Map<String,Object> regime=new LinkedHashMap<>();regime.put("route","REGIME");regime.put("selectedWindow",64);
        List<StructuralProposalR26.Proposal> proposals=FailureDirectedResearchR26.routeR31(parent,hypothesis(parent,ResearchHypothesisR31.Mechanism.SHORT_WINDOW_DRIFT),regime);assertEquals(1,proposals.size());ModelGenomeSU2 child=proposals.get(0).genome;assertEquals("GATED",child.pool);assertEquals(parent.members.size()+1,child.members.size());
        ModelGenomeSU2.Member added=child.members.get(child.members.size()-1);assertEquals("BOCPD",added.kind);assertEquals("64",added.params.get("R"));OnlineModelsSU2.Model model=ModelReplaySU2.build(added.kind,added.params);
        float[][] p=new float[10][10];model.predict(p,new float[model.stateWidth()]);int[] draw={1,2,3,4,5,6,7,8,9,10};model.update(draw);model.predict(p,new float[model.stateWidth()]);for(float[] row:p){double sum=0;for(float v:row){assertTrue(Float.isFinite(v));sum+=v;}assertEquals(1,sum,1e-6);}
        regime.put("route","BASE");assertTrue(FailureDirectedResearchR26.routeR31(parent,hypothesis(parent,ResearchHypothesisR31.Mechanism.SHORT_WINDOW_DRIFT),regime).isEmpty());
    }
    @Test public void unrelatedRiskHeadUnknownComponentAndResourceFaultDoNotBecomeChildren(){
        ModelGenomeSU2 parent=FamilyRegistryR26.genome("F01");for(ResearchHypothesisR31.Mechanism mechanism:new ResearchHypothesisR31.Mechanism[]{ResearchHypothesisR31.Mechanism.CROSS_POSITION_SUPPORT,ResearchHypothesisR31.Mechanism.COMPONENT_NEGATIVE,ResearchHypothesisR31.Mechanism.RESOURCE_FAILURE})assertTrue(mechanism.name(),FailureDirectedResearchR26.routeR31(parent,hypothesis(parent,mechanism)).isEmpty());
        assertTrue("original F15 risk family remains executable",FamilyRegistryR26.build("F15") instanceof SetFamilyR26);
    }
}
