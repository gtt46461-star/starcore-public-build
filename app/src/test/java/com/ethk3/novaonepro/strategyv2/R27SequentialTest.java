package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;
import java.util.*;
import org.junit.Test;

/** H08 (error rate: 1000 HR=30% strategies × 20000 periods, permanent α allocation) and H09 (power: HR 35%, act
 * 15%, median stopping time), R27-040. */
public class R27SequentialTest {
    static long run(SequentialQualificationR27.State s, double hr, double act, int periods, StatsR27.SplitMix r) {
        for (int t = 1; t <= periods; t++) {
            boolean a = r.chance(act), h = a && r.chance(hr);
            String st = s.observe(t, a, h);
            if (!SequentialQualificationR27.ACCUMULATING.equals(st)) return st.equals(SequentialQualificationR27.QUALIFIED) ? t : -t;
        }
        return 0;
    }

    @Test public void alphaAllocationSumsToRoot() {
        double s = 0; for (int i = 1; i <= 2_000_000; i++) s += SequentialQualificationR27.alpha(i);
        assertTrue(s <= SequentialQualificationR27.ALPHA_ROOT + 1e-9);
        R27TestSupport.log("ALPHA C=" + SequentialQualificationR27.C + " alpha1=" + SequentialQualificationR27.alpha(1) + " alpha3=" + SequentialQualificationR27.alpha(3) + " alpha1000=" + SequentialQualificationR27.alpha(1000) + " sum(1..2e6)=" + s);
    }

    @Test public void falseAuthorizationAtMostFivePercent() {
        StatsR27.SplitMix r = new StatsR27.SplitMix(2026); int up = 0, down = 0;
        for (int i = 1; i <= 1000; i++) {
            long res = run(new SequentialQualificationR27.State("P" + i, i, 0.15), 0.30, 0.15, 20000, r);
            if (res > 0) up++; else if (res < 0) down++;
        }
        double rate = up / 1000.0;
        R27TestSupport.log("H08 nullStrategies=1000 periods=20000 falseQualified=" + up + " rate=" + rate + " judgedNotBetter=" + down);
        assertTrue(rate <= 0.05); R27TestSupport.log("H08_PASS");
    }

    @Test public void powerAtHr35Act15() {
        StringBuilder b = new StringBuilder(); long medianAt3 = 0;
        for (long index : new long[] {3, 10, 1000}) {
            StatsR27.SplitMix r = new StatsR27.SplitMix(7 + index); List<Long> stops = new ArrayList<>(); int never = 0;
            for (int k = 0; k < 200; k++) { long t = run(new SequentialQualificationR27.State("Q", index, 0.15), 0.35, 0.15, 60000, r); if (t > 0) stops.add(t); else stops.add(Long.MAX_VALUE); if (t <= 0) never++; }
            Collections.sort(stops); long med = stops.get(stops.size() / 2);
            b.append("index").append(index).append(":alpha=").append(SequentialQualificationR27.alpha(index)).append(",medianStop=").append(med).append(",notQualifiedIn60000=").append(never).append(' ');
            if (index == 3) medianAt3 = med;
        }
        R27TestSupport.log("H09 " + b);
        assertTrue("median stop " + medianAt3, medianAt3 <= 10000);
        R27TestSupport.log("H09_PASS medianStopAtIndex3=" + medianAt3);
    }
}
