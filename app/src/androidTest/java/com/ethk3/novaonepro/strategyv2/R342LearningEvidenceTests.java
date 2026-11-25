package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;
import android.content.Context;

public final class R342LearningEvidenceTests {
    private R342LearningEvidenceTests() {}

    public static void optimizerParameterTransition(Context ignored) {
        AtomNetR27 model = new AtomNetR27(3,8721L);
        double[][] features={{.12,.5,.37},{.47,.04,.49},{.15,.7,.15},{.08,.17,.75}};
        int[] hit={1,0,1,0}, actual={1,2,3,4};
        model.trainBatch(features,hit,actual,.005d,null);
        AtomNetR27.OptimizerStep first=model.lastStep();
        assertNotNull(first);
        assertEquals(1L,first.adamStep);
        assertEquals(features.length,first.batchSize);
        assertTrue(first.gradientNorm>0 && first.parameterChange>0);
        assertTrue(first.parametersBefore.matches("[a-f0-9]{64}"));
        assertNotEquals(first.parametersBefore,first.parametersAfter);
        model.trainBatch(features,hit,actual,.005d,null);
        assertEquals(first.parametersAfter,model.lastStep().parametersBefore);
        assertNotEquals(first.parametersAfter,model.lastStep().parametersAfter);
        assertEquals(2L,model.lastStep().adamStep);
    }

    public static void sameTypedProbePastOnly(Context ignored) {
        assertNotEquals(AtomNetR27.parameterHashR342(new double[][]{{.1,.2},{.3}}),
                        AtomNetR27.parameterHashR342(new double[][]{{.1},{.2,.3}}));
        assertNotEquals(AtomNetR27.parameterHashR342(new double[][]{{.1,.2,.3}}),
                        AtomNetR27.parameterHashR342(new double[][]{{.1,.3,.2}}));
        R341LearningTestCases.sameHistoryBeforeTarget(ignored);
    }
}
