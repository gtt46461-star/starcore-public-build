package com.ethk3.novaonepro.strategyv2;
import org.junit.Test;import static org.junit.Assert.*;import java.util.*;
public class RepairFusionStudyR30Test {
 @Test public void samePredictionHasSameResidualAndNoPairedImprovement(){EvalContextR27 c=RepairObservatoryR30Test.ctx();PolicySpecR27 p=RepairObservatoryR30Test.parent();FusionStudyR30R1.Pair pair=FusionStudyR30R1.pair(p,p,c);assertEquals(1,pair.residualCorrelation,1e-12);assertFalse(pair.complementary());ResearchGainR30R1.Estimate e=FusionStudyR30R1.gain(p,p,c).estimate();assertEquals(0,e.meanBlockGain,0);assertFalse(FusionStudyR30R1.improvesEveryParent(Collections.singletonList(p),p,c));}
 @Test(expected=IllegalArgumentException.class) public void crossTargetParentsCannotBeCompared(){PolicySpecR27 p=RepairObservatoryR30Test.parent();FusionStudyR30R1.pair(p,p.withSelector(2,p.rule,p.threshold,p.rate,p.meta),RepairObservatoryR30Test.ctx());}
}
