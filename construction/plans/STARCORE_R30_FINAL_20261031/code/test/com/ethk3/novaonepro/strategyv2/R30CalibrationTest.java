package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.util.Map;
import org.junit.Test;

/** R30-B06-01 宿主测试。复制到 app/src/test/java/com/ethk3/novaonepro/strategyv2/。 */
public class R30CalibrationTest {
    @Test @SuppressWarnings("unchecked") public void calibrationPerfectAndBiased() {
        int n = 20000; double[] p = new double[n]; int[] y = new int[n];
        StatsR27.SplitMix rng = new StatsR27.SplitMix(11);
        for (int i = 0; i < n; i++) { p[i] = 0.2 + 0.2 * (i % 5) / 4.0; y[i] = rng.nextDouble() < p[i] ? 1 : 0; }
        Map<String, Object> ok = CalibrationR30.evaluate(p, y, n, 10, 0.2, 0.4);
        assertTrue("ece=" + ok.get("eceEqualMass"), (Double) ok.get("eceEqualMass") < 0.02);
        assertEquals(1.0, (Double) ok.get("supportCoverage"), 1e-12);
        double[] biased = p.clone(); for (int i = 0; i < n; i++) biased[i] = Math.min(1, p[i] + 0.15);
        Map<String, Object> bad = CalibrationR30.evaluate(biased, y, n, 10, 0.2, 0.4);
        assertTrue("ece=" + bad.get("eceEqualMass"), (Double) bad.get("eceEqualMass") > 0.10);
        assertEquals(Boolean.FALSE, bad.get("q05Pass"));
        assertNull("样本<200 不判定", CalibrationR30.evaluate(p, y, 150, 10, 0.2, 0.4).get("q05Pass"));
    }
}
