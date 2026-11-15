package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import org.junit.Test;

/** R30-B05-02 宿主测试。复制到 app/src/test/java/com/ethk3/novaonepro/strategyv2/。 */
public class R30SequentialNullSimTest {
    /** 2000 个零优势策略（命中率=0.30，每期出手），各 3000 期：全局 α 预算下误授证总数应极少。 */
    @Test public void nullStrategiesRarelyQualify() {
        StatsR27.SplitMix rng = new StatsR27.SplitMix(20261031L);
        int falseQualified = 0;
        for (long idx = 1; idx <= 2000; idx++) {
            SequentialQualificationR27.State s = new SequentialQualificationR27.State("null-" + idx, idx, 1.0);
            for (long t = 1; t <= 3000 && SequentialQualificationR27.ACCUMULATING.equals(s.state); t++) s.observe(t, true, rng.nextDouble() < StatsR27.BASE);
            if (SequentialQualificationR27.QUALIFIED.equals(s.state)) falseQualified++;
        }
        assertTrue("误授证=" + falseQualified, falseQualified <= 2);
    }

    /** 真优势 0.40（每期出手）在第1个全局序号下，5000期内应能检出（记录用期数）。 */
    @Test public void realEdgeIsDetectable() {
        StatsR27.SplitMix rng = new StatsR27.SplitMix(7L);
        int detected = 0;
        for (int rep = 0; rep < 20; rep++) {
            SequentialQualificationR27.State s = new SequentialQualificationR27.State("edge-" + rep, 1, 1.0);
            for (long t = 1; t <= 5000 && SequentialQualificationR27.ACCUMULATING.equals(s.state); t++) s.observe(t, true, rng.nextDouble() < 0.40);
            if (SequentialQualificationR27.QUALIFIED.equals(s.state)) detected++;
        }
        assertTrue("检出=" + detected + "/20", detected >= 18);
    }
}
