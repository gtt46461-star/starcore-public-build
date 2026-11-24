package com.ethk3.novaonepro.strategyv2;

import static org.junit.Assert.*;

import android.content.Context;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public final class R341TimingFixture {
    private R341TimingFixture() {}

    public static void independentLimitsAndUnknowns(Context app) {
        String start = "202611241440";
        String first = "202611250001";
        String second = "202611250002";
        String third = "202611250003";
        String fourth = "202611250004";
        List<Object> rows = new ArrayList<>();
        rows.add(draw(start, 10000L, null));
        rows.add(draw(first, 70000L, 1000L));
        rows.add(draw(second, 130000L, 1001L));
        rows.add(draw(fourth, 250000L, 999L));
        rows.add(freeze(start, first, 10000L + 4999L, 4999L, "BOOT_A"));
        rows.add(freeze(first, second, 70000L + 5001L, 5000L, "BOOT_A"));
        rows.add(freeze(second, third, 130000L + 5000L, 5001L, "BOOT_B"));
        Map<String, Object> report = IssueTimelineR30.closedOpportunityTiming(start, fourth, rows);
        assertEquals(4, report.get("opportunities"));
        assertEquals("202611250005", report.get("nextPendingTarget"));
        assertEquals(2, report.get("marginAtLeast5s"));
        assertEquals(1, report.get("marginMissing"));
        assertCounts(report.get("drawCommitToFreeze"), 1, 1, 2);
        assertCounts(report.get("receivedToSettlement"), 2, 1, 1);
        assertCounts(report.get("publicationToFreeze"), 0, 0, 4);
        assertCounts(report.get("publicationToSettlement"), 0, 0, 4);
        assertEquals("NOT_MEASURABLE_CALENDAR_BOUNDS", IssueTimelineR30.closedOpportunityTiming(null, fourth, rows).get("status"));
    }

    private static void assertCounts(Object obj, int pass, int over, int unknown) {
        Map<?, ?> row = (Map<?, ?>) obj;
        assertEquals(pass, row.get("passed"));
        assertEquals(over, row.get("overLimit"));
        assertEquals(unknown, row.get("notMeasurable"));
    }

    private static Map<String, Object> draw(String issue, long commit, Long settlement) {
        Map<String, Object> row = event(issue, "DRAW");
        row.put("draw_hash", issue);
        row.put("canonical_draw_head", true);
        row.put("draw_durable_mono", commit);
        row.put("draw_durable_boot", "BOOT_A");
        row.put("mono_first_observed", commit - 100L);
        row.put("boot_first_observed", "BOOT_A");
        if (settlement != null) {
            row.put("mono_settled", commit - 100L + settlement);
            row.put("boot_settled", "BOOT_A");
        }
        return row;
    }

    private static Map<String, Object> freeze(String base, String target, long commit, long margin, String boot) {
        Map<String, Object> row = event(target, "PREDICTION");
        long wall = 1_795_542_000_000L + commit;
        row.put("source_issue", base);
        row.put("source_draw_hash", base);
        row.put("precommit_wall_next_frozen", wall - 1L);
        row.put("wall_next_frozen", wall);
        row.put("wall_next_deadline", wall + margin);
        row.put("mono_next_frozen", commit);
        row.put("boot_next_frozen", boot);
        return row;
    }

    private static Map<String, Object> event(String issue, String kind) {
        Map<String, Object> row = new LinkedHashMap<>();
        row.put("event_id", issue + '-' + kind);
        row.put("issue", issue);
        row.put("event_kind", kind);
        row.put("event_role", "LIVE");
        row.put("outcome", "OPEN");
        return row;
    }
}
