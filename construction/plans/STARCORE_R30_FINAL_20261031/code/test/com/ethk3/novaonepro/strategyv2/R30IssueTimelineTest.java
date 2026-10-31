package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.Test;

/** R30-B02-01 宿主测试。复制到 app/src/test/java/com/ethk3/novaonepro/strategyv2/。 */
public class R30IssueTimelineTest {
    @Test public void timelineElapsedRequiresSameBoot() {
        IssueTimelineR30.Row r = new IssueTimelineR30.Row("20261031001")
                .set(IssueTimelineR30.Stage.FIRST_OBSERVED, 1000L, 100L, "boot-a")
                .set(IssueTimelineR30.Stage.SETTLED, 1600L, 700L, "boot-a")
                .set(IssueTimelineR30.Stage.NEXT_FROZEN, 9000L, 50L, "boot-b");
        assertEquals(Long.valueOf(600), r.elapsed(IssueTimelineR30.Stage.FIRST_OBSERVED, IssueTimelineR30.Stage.SETTLED));
        assertNull("跨重启不得求差", r.elapsed(IssueTimelineR30.Stage.SETTLED, IssueTimelineR30.Stage.NEXT_FROZEN));
    }

    @Test @SuppressWarnings("unchecked") public void timelineSummaryP95AndM05() {
        List<IssueTimelineR30.Row> rows = new ArrayList<>();
        for (int i = 0; i < 100; i++) {
            long settle = i < 95 ? 400 : 1500;
            rows.add(new IssueTimelineR30.Row("i" + i)
                    .set(IssueTimelineR30.Stage.FIRST_OBSERVED, 0L, 0L, "b").set(IssueTimelineR30.Stage.SETTLED, settle, settle, "b")
                    .set(IssueTimelineR30.Stage.NEXT_FROZEN, 10_000L, 10_000L, "b").set(IssueTimelineR30.Stage.NEXT_DEADLINE, i < 90 ? 20_000L : 12_000L, null, null));
        }
        Map<String, Object> s = IssueTimelineR30.summarize(rows);
        Map<String, Object> seg = (Map<String, Object>) s.get("observe_to_settled");
        assertEquals(400L, seg.get("p95"));
        assertEquals(Boolean.TRUE, seg.get("pass"));
        Map<String, Object> m05 = (Map<String, Object>) s.get("M05");
        assertEquals(0.90, (Double) m05.get("coverage"), 1e-12);
        Map<String, Object> empty = (Map<String, Object>) IssueTimelineR30.summarize(new ArrayList<>()).get("request_to_frozen");
        assertNull("无样本不能判PASS", empty.get("pass"));
    }

    @Test public void timelineMarkSqlIsFirstWriterWins() {
        String sql = IssueTimelineR30.sqlMark(IssueTimelineR30.Stage.SETTLED);
        assertTrue(sql.endsWith("WHERE issue=? AND wall_settled IS NULL"));
        assertEquals(5, IssueTimelineR30.markArgs(IssueTimelineR30.Stage.SETTLED, "x", 1, 2, "b", 3).length);
        assertEquals(3, IssueTimelineR30.markArgs(IssueTimelineR30.Stage.NEXT_DEADLINE, "x", 1, 2, "b", 3).length);
    }
}
